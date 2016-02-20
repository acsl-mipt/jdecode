package ru.mipt.acsl.decode.parser

import java.io.InputStream
import java.net.URLEncoder

import com.google.common.base.Charsets
import com.typesafe.scalalogging.LazyLogging
import org.parboiled2.ParserInput.StringBasedParserInput
import org.parboiled2._
import ru.mipt.acsl.decode.model.domain.impl._
import ru.mipt.acsl.decode.model.domain.impl.types._
import ru.mipt.acsl.decode.model.domain._

import scala.collection.{immutable, mutable}
import scala.io.Source
import scala.util.{Try, Success, Failure}

class DecodeParboiledParser(val input: ParserInput) extends Parser with LazyLogging {

  private val alpha = CharPredicate.Alpha ++ '_'
  private val alphaNum = alpha ++ CharPredicate.Digit
  private val imports = mutable.HashMap.empty[String, MaybeProxy[Referenceable]]

  def Ew = rule { anyOf(" \t\r\n").+ }

  def quotedString(quoteChar: Char): Rule1[String] = rule {
    atomic(ch(quoteChar) ~ capture(zeroOrMore(!anyOf(quoteChar + "\\") ~ ANY | '\\' ~ ANY)) ~ ch(quoteChar))
      .named("stringLiteral")
  }

  def StringValue: Rule1[String] = rule { quotedString('"') | quotedString('\'') }

  def InfoEw: Rule1[String] = rule { StringValue ~ Ew.? }

  def ElementNameLiteral: Rule0 = rule { '^'.? ~ alpha ~ alphaNum.* }

  def ElementName: Rule1[DecodeName] = rule { capture(ElementNameLiteral) ~> DecodeNameImpl.newFromSourceName _ }

  def ElementIdLiteral: Rule0 = rule { ElementNameLiteral.+('.') }

  def ElementId: Rule1[Fqn] = rule { ElementName.+('.') ~> FqnImpl.apply _ }

  def Namespace: Rule1[Namespace] = rule {
    InfoEw.? ~ atomic("namespace") ~ Ew ~ ElementId ~>
      ((info: Option[String], fqn: Fqn) => newNamespace(info, fqn))
  }

  private def ImportPart: Rule1[ImportPart] = rule {
    ElementName ~ (Ew ~ atomic("as") ~ Ew ~ ElementName
      ~> ((as: DecodeName, name: DecodeName) => ImportPartNameAlias(name, as))
      | MATCH ~> (ImportPartName(_: DecodeName)))
  }

  def addImport(fqn: Fqn, importPart: ImportPart) {
    require(!imports.contains(importPart.alias), importPart.alias)
    imports.put(importPart.alias, MaybeProxy.proxy(fqn, TypeName(importPart.originalName)))
  }

  def addImport(fqn: Fqn) {
    require(!imports.contains(fqn.last.asMangledString), fqn.last.asMangledString)
    imports.put(fqn.last.asMangledString, MaybeProxy.proxy(fqn.copyDropLast(), TypeName(fqn.last)))
  }

  def Import: Rule0 = rule {
    atomic("import") ~ Ew ~ ElementId ~
      ('.' ~ Ew.? ~ '{' ~ Ew.? ~ ImportPart.+(Ew.? ~ ',' ~ Ew.?)
        ~ Ew.? ~ ','.? ~ Ew.? ~ '}'
        ~> ((fqn: Fqn, parts: Seq[ImportPart]) => parts.foreach(addImport(fqn, _)))
        | MATCH ~> (addImport(_: Fqn)))
  }

  def NonNegativeIntegerLiteral: Rule0 = rule { CharPredicate.Digit.+ }

  def NonNegativeIntegerAsInt: Rule1[Int] = rule {
    capture(NonNegativeIntegerLiteral) ~> (Integer.parseInt(_: String, 10))
  }

  private var ns: Option[Namespace] = None

  private def makeNewSystemName(name: DecodeName): DecodeName =
    DecodeNameImpl.newFromSourceName(name.asMangledString + '$')

  private def unitForFqn(unitFqn: Fqn): MaybeProxy[Measure] = {
    if (unitFqn.size == 1 && imports.contains(unitFqn.asMangledString))
      // todo: remove asInstanceOf
      imports.getOrElse(unitFqn.asMangledString, sys.error("not found")).asInstanceOf[MaybeProxy[Measure]]
    else
      MaybeProxy.proxyDefaultNamespace(unitFqn, ns.get)
  }

  def UnitApplication: Rule1[Fqn] = rule { '/' ~ ElementId ~ '/' }

  private def newTypeUnitApplication(t: Option[MaybeProxy[DecodeType]],
                                     unit: Option[MaybeProxy[Measure]]) =
    new DecodeTypeUnitApplicationImpl(t.get, unit)

  def TypeUnitApplication: Rule1[DecodeTypeUnitApplication] = rule {
    TypeApplication ~ (Ew.? ~ UnitApplication ~> (unitForFqn(_: Fqn))).? ~> newTypeUnitApplication _
  }

  private def newStructField(info: Option[String], typeUnit: DecodeTypeUnitApplication, name: DecodeName) =
    new DecodeStructFieldImpl(name, typeUnit, info)

  def StructField: Rule1[DecodeStructField] = rule {
    InfoEw.? ~ TypeUnitApplication ~ Ew ~ ElementName ~> newStructField _
  }

  def StructTypeFields: Rule1[Seq[DecodeStructField]] = rule {
    '(' ~ Ew.? ~ StructField.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  var componentName: Option[DecodeName] = None

  def ComponentParameters: Rule1[StructType] = rule {
    InfoEw.? ~ atomic("parameters") ~> (() => makeNewSystemName(componentName.get)) ~ Ew ~ StructTypeFields ~>
      newStructType _
  }

  private def newCommandArg(info: Option[String], typeUnit: DecodeTypeUnitApplication, name: DecodeName) =
    new ParameterImpl(name, info, typeUnit.t, typeUnit.unit)

  def CommandArg: Rule1[Parameter] = rule {
    InfoEw.? ~ TypeUnitApplication ~ Ew ~ ElementName ~> newCommandArg _
  }

  def CommandArgs: Rule1[immutable.Seq[Parameter]] = rule {
    '(' ~ Ew.? ~ CommandArg.*(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  private def newCommand(info: Option[String], name: DecodeName, id: Option[Int],
                         args: immutable.Seq[Parameter],
                         returnType: Option[MaybeProxy[DecodeType]]) =
    new DecodeCommandImpl(name, id, info, args, returnType)

  def Command: Rule1[DecodeCommand] = rule {
    definition("command") ~ Id.? ~ Ew.? ~ CommandArgs ~
      (Ew.? ~ "->" ~ Ew.? ~ TypeApplication ~> (_.get)).? ~> newCommand _
  }

  def MessageParameterElement: Rule1[String] = rule {
    capture(ElementIdLiteral ~ ('[' ~ Ew.? ~ NonNegativeIntegerLiteral ~ Ew.? ~
      (atomic("..") ~ Ew.? ~ NonNegativeIntegerLiteral ~ Ew.?).? ~ ']'
      | '.' ~ ElementIdLiteral).*)
  }

  private def newMessageParameter(info: Option[String], value: String) = new MessageParameterImpl(value, info)

  def MessageParameter: Rule1[MessageParameter] = rule {
    InfoEw.? ~ MessageParameterElement ~> newMessageParameter _
  }

  // TODO: parameter case
  def EventMessageParameter: Rule1[Either[MessageParameter, Parameter]] = rule {
    InfoEw.? ~ ((atomic("var") ~ Ew ~ TypeUnitApplication ~ Ew ~ ElementName ~> newCommandArg _
      ~> Right[MessageParameter, Parameter] _) |
      (MessageParameterElement ~> newMessageParameter _ ~> Left[MessageParameter, Parameter] _))
  }

  def EventMessageParameters: Rule1[Seq[Either[MessageParameter, Parameter]]] = rule {
    '(' ~ Ew.? ~ EventMessageParameter.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  def MessageParameters: Rule1[Seq[MessageParameter]] = rule {
    '(' ~ Ew.? ~ MessageParameter.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  def MessageNameId: Rule2[DecodeName, Option[Int]] = rule { ElementName ~ Id.? }

  var component: Option[Component] = None

  private def newEventMessage(info: Option[String], name: DecodeName, id: Option[Int],
                              baseType: Option[MaybeProxy[DecodeType]],
                              parameters: Seq[Either[MessageParameter, Parameter]]) =
    new EventMessageImpl(component.get, name, id, info, parameters, baseType.get)

  def EventMessage: Rule1[EventMessage] = rule {
    InfoEw.? ~ atomic("event") ~ Ew ~ MessageNameId ~ Ew ~ TypeApplication ~ Ew.? ~ EventMessageParameters ~>
      newEventMessage _
  }

  private def newStatusMessage(info: Option[String], name: DecodeName, id: Option[Int], priority: Option[Int],
                               parameters: Seq[MessageParameter]) =
    new StatusMessageImpl(component.get, name, id, info, parameters, priority)

  def StatusMessage: Rule1[StatusMessage] = rule {
    InfoEw.? ~ atomic("status") ~ Ew ~ MessageNameId ~
      (Ew ~ "priority" ~ Ew.? ~ ':' ~ Ew.? ~ NonNegativeIntegerAsInt).? ~ Ew.? ~ MessageParameters ~> newStatusMessage _
  }

  private def newComponent(info: Option[String], id: Option[Int], subComponents: Option[immutable.Seq[Fqn]],
                           baseType: Option[StructType]): Component = {
    component = Some(new ComponentImpl(componentName.get, ns.get, id,
      baseType.map(MaybeProxy.obj), info, subComponents.map(_.map{ fqn =>
      val alias = fqn.asMangledString
      if (fqn.size == 1 && imports.contains(alias))
        new DecodeComponentRefImpl(imports.get(alias).get.asInstanceOf[MaybeProxy[Component]], Some(alias))
      else
        new DecodeComponentRefImpl(MaybeProxy.proxyDefaultNamespace(fqn, ns.get), None)
      }).getOrElse(immutable.Seq.empty)))
    component.get
  }

  def Id: Rule1[Int] = rule {
    Ew ~ "id" ~ Ew.? ~ ':' ~ Ew.? ~ NonNegativeIntegerAsInt
  }

  def Component: Rule1[Component] = rule {
    definition("component") ~> { name => componentName = Some(name) } ~ Id.? ~
      (Ew ~ atomic("with") ~ Ew ~ ElementId.+(Ew.? ~ ',' ~ Ew.?)).? ~
      Ew.? ~ '{' ~ (Ew.? ~ ComponentParameters).? ~> newComponent _ ~
      (Ew.? ~ (Command ~> { command => component.get.commands = component.get.commands :+ command }
      | (StatusMessage ~> { statusMessage => component.get.statusMessages = component.get.statusMessages :+ statusMessage }
        | EventMessage ~> { eventMessage => component.get.eventMessages = component.get.eventMessages :+ eventMessage }))).* ~
      Ew.? ~ '}'
  }

  private def newUnit(display: Option[String], name: DecodeName, info: Option[String]) =
    new MeasureImpl(name, ns.get, display, info)

  def Unit_ : Rule1[Measure] = rule {
    definition("unit") ~ (Ew ~ atomic("display") ~ Ew ~ StringValue).? ~
      (Ew ~ atomic("placement") ~ Ew ~ atomic("before" | "after")).? ~> newUnit _
  }

  def FloatLiteral: Rule0 = rule {
    anyOf("+-").? ~ (NonNegativeIntegerLiteral ~ '.' ~ NonNegativeIntegerLiteral.? | '.' ~ NonNegativeIntegerLiteral) ~
      (anyOf("eE") ~ anyOf("+-").? ~ NonNegativeIntegerLiteral)
  }

  def Literal: Rule0 = rule {
    FloatLiteral | NonNegativeIntegerLiteral | atomic("true") | atomic("false")
  }

  private def newEnumConstant(info: Option[String], name: DecodeName, value: String) =
    new DecodeEnumConstantImpl(name, value, info)

  def EnumTypeValue: Rule1[DecodeEnumConstant] = rule {
    InfoEw.? ~ ElementName ~ Ew.? ~ '=' ~ Ew.? ~ capture(Literal) ~> newEnumConstant _
  }

  def EnumTypeValues: Rule1[Seq[DecodeEnumConstant]] = rule { EnumTypeValue.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? }

  private def newEnumType(name: DecodeName, info: Option[String], t: Option[MaybeProxy[DecodeType]],
                          constants: Seq[DecodeEnumConstant]): EnumType =
    new EnumTypeImpl(Some(name), ns.get, t.get, info, constants.to[immutable.Set])

  def EnumType = rule {
    InfoEw.? ~ atomic("enum") ~ Ew ~ TypeApplication ~ Ew.? ~ '(' ~ Ew.? ~ EnumTypeValues ~ Ew.? ~ ')' ~>
      newEnumType _
  }

  private def newStructType(info: Option[String], name: DecodeName, fields: Seq[DecodeStructField]): StructType =
    new StructTypeImpl(Some(name), ns.get, info, fields)

  def StructType = rule { atomic("struct") ~ Ew.? ~ StructTypeFields ~> newStructType _ }

  private def newSubType(info: Option[String], name: DecodeName,
                         baseType: Option[MaybeProxy[DecodeType]]): SubType =
    new SubTypeImpl(Some(name), ns.get, info, baseType.get)

  def Type: Rule1[DecodeType] = rule {
    definition("type") ~ Ew ~ (
        EnumType
      | StructType
      | TypeApplication ~> newSubType _)
  }

  def definition(keyword: String) = rule { InfoEw.? ~ atomic(keyword) ~ Ew ~ ElementName }

  def proxyForTypeFqn[T <: Referenceable](namespace: Namespace, typeFqn: Fqn): MaybeProxy[T] =
    if (typeFqn.size == 1 && imports.contains(typeFqn.last.asMangledString))
      imports.get(typeFqn.last.asMangledString).get.asInstanceOf[MaybeProxy[T]]
    else
      MaybeProxy.proxyDefaultNamespace(typeFqn, namespace)

  def NativeTypeApplication: Rule1[Option[MaybeProxy[DecodeType]]] = rule {
    atomic("void") ~> (() => None) |
      capture(atomic("ber")) ~> { t: String =>
        Some(MaybeProxy.proxyForSystem[DecodeType](TypeName(DecodeNameImpl.newFromSourceName(t))))
      }
  }

  def PrimitiveTypeKind: Rule1[String] = rule { capture("uint" | "int" | "float" | "bool").named("primitiveTypeKind") }

  def PrimitiveTypeApplication: Rule1[MaybeProxy[DecodeType]] = rule {
    PrimitiveTypeKind ~ ':' ~ NonNegativeIntegerAsInt ~> { (typeKindName: String, bitSize: Int) =>
      MaybeProxy.proxyForSystem[DecodeType](PrimitiveTypeName(TypeKind.typeKindByName(typeKindName).get, bitSize))
    }
  }

  def LengthTo: Rule1[Int] = rule { NonNegativeIntegerAsInt | ch('*') ~> (() => 0) }

  private def newTypeProxy(typeApplication: Option[MaybeProxy[DecodeType]],
                           fromTo: Option[(Int, Option[Int])]): Option[MaybeProxy[DecodeType]] = {
    val path = typeApplication.get.proxy.path
    Some(MaybeProxy.proxy(ProxyPath(path.ns, ArrayTypePath(path,
        new ArraySizeImpl(fromTo.map(_._1.toLong).getOrElse(0l),
          fromTo.map(_._2.map(_.toLong).getOrElse(0l)).getOrElse(0l))))))
  }

  def ArrayTypeApplication: Rule1[Option[MaybeProxy[DecodeType]]] = rule {
    '[' ~ Ew.? ~ TypeApplication ~ (Ew.? ~ ',' ~ Ew.? ~ NonNegativeIntegerAsInt
      ~ (Ew.? ~ ".." ~ Ew.? ~ LengthTo).? ~> ((_, _))).? ~ Ew.? ~ ']' ~> newTypeProxy _
  }

  private def newGenericTypeProxy(b: Option[MaybeProxy[DecodeType]],
                                  g: Seq[Option[MaybeProxy[DecodeType]]]): Option[MaybeProxy[DecodeType]] = {
    val path = b.get.proxy.path
    Some(MaybeProxy.proxy(ProxyPath(path.ns, GenericTypeName(path.element.asInstanceOf[TypeName].typeName,
      g.map(_.map(_.proxy.path)).to[immutable.Seq]))))
  }

  private def newOptionalTypeProxy(t: Option[MaybeProxy[DecodeType]]): Option[MaybeProxy[DecodeType]] =
    Some(MaybeProxy.proxyForSystem(GenericTypeName(OptionalType.MANGLED_NAME, immutable.Seq(Some(t.get.proxy.path)))))

  def TypeApplication: Rule1[Option[MaybeProxy[DecodeType]]] = rule {
    (PrimitiveTypeApplication ~> (Some(_)) | NativeTypeApplication | ArrayTypeApplication
      | ElementId ~> { fqn => Some(proxyForTypeFqn[DecodeType](ns.get, fqn))}) ~
    (Ew.? ~ '<' ~ TypeApplication.+(Ew.? ~ ',' ~ Ew.?) ~ ','.? ~ Ew.? ~ '>' ~> newGenericTypeProxy _).? ~
    (Ew.? ~ '?' ~> newOptionalTypeProxy _).?
  }

  private def newAliasType(info: Option[String], name: DecodeName,
                           typeAppOption: Option[MaybeProxy[DecodeType]]) =
    new AliasTypeImpl(name, ns.get, typeAppOption.get, info)

  def Alias: Rule1[AliasType] = rule { definition("alias") ~ Ew ~ TypeApplication ~> newAliasType _ }

  private def newLanguage(info: Option[String], name: DecodeName, default: Boolean) =
    new LanguageImpl(name, ns.get, default, info)

  def Language: Rule1[Language] = rule {
    definition("language") ~ (Ew ~ atomic("default") ~> (() => true) | MATCH ~> (() => false)) ~> newLanguage _
  }

  // todo: refactoring
  def File: Rule1[Namespace] = rule {
    run(imports.clear()) ~ Namespace ~>
      { _ns => ns = Some(_ns); _ns } ~
      (Ew ~ Import).* ~
      (Ew.? ~ (Component ~> { c => ns.get.components = ns.get.components :+ c }
        | Unit_ ~> { u => ns.get.units = ns.get.units :+ u }
        | Type ~> { t => ns.get.types = ns.get.types :+ t }
        | Alias ~> { a => ns.get.types = ns.get.types :+ a }
        | Language ~> { l => ns.get.languages = ns.get.languages :+ l })).* ~
      Ew.? ~ EOI
  }

  def newNamespace(info: Option[String], fqn: Fqn): Namespace = {
    var parentNamespace: Option[Namespace] = None
    if (fqn.size > 1)
      parentNamespace = Some(DecodeUtils.newNamespaceForFqn(fqn.copyDropLast()))
    val result: Namespace = NamespaceImpl(fqn.last, parentNamespace)
    parentNamespace.foreach(ns => ns.subNamespaces = ns.subNamespaces :+ result)
    result
  }
}

private sealed trait ImportPart {
  def alias: String
  def originalName: DecodeName
}

private case class ImportPartName(originalName: DecodeName) extends ImportPart {
  def alias: String = originalName.asMangledString
}

private case class ImportPartNameAlias(originalName: DecodeName, _alias: DecodeName) extends ImportPart {
  def alias: String = _alias.asMangledString
}

object DecodeParser {
  def parse(input: ParserInput): Try[Namespace] = {
    val parser: DecodeParboiledParser = new DecodeParboiledParser(input)
    parser.File.run()
  }
}

case class DecodeSourceProviderConfiguration(resourcePath: String)

class DecodeSourceProvider extends LazyLogging {
  def provide(config: DecodeSourceProviderConfiguration): Registry = {
    val resourcePath = config.resourcePath
    val registry = new RegistryImpl()
    val resourcesAsStream = getClass.getResourceAsStream(resourcePath)
    require(resourcesAsStream != null, resourcePath)
    registry.rootNamespaces ++= DecodeUtils.mergeRootNamespaces(Source.fromInputStream(resourcesAsStream).getLines().
      filter(_.endsWith(".decode")).map { name =>
      val resource: String = resourcePath + "/" + name
      logger.debug(s"Parsing $resource...")
      val input: StringBasedParserInput = Source.fromInputStream(getClass.getResourceAsStream(resource)).mkString
      DecodeParser.parse(input) match {
        case Success(v) => v.rootNamespace
        case Failure(e) => e match {
          case p: ParseError =>
            val formatter: ErrorFormatter = new ErrorFormatter(showTraces = true)
            logger.error(formatter.format(p, input))
        }
        throw e
      }
    }.toTraversable)
    registry
  }
}