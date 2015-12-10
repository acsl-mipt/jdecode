package ru.mipt.acsl.decode.parser

import java.io.InputStream

import org.parboiled2._
import ru.mipt.acsl.decode.model.domain.impl._
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeAliasTypeImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeCommandArgumentImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeComponentImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeEnumConstantImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeEnumTypeImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeStructFieldImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeStructTypeImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeSubTypeImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeTypeUnitApplicationImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeUnitImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeFqnImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeNamespaceImpl
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy

import scala.collection.mutable
import scala.io.Source
import scala.util.{Try, Success, Failure}

class DecodeParboiledParser(val input: ParserInput) extends Parser {

  private val alpha = CharPredicate.Alpha ++ '_'
  private val alphaNum = alpha ++ CharPredicate.Digit
  private val imports = mutable.HashMap[String, DecodeMaybeProxy[DecodeReferenceable]]()

  val Ew = rule { anyOf(" \t\r\n").+ }

  def quotedString(quoteChar: Char): Rule1[String] = rule {
    atomic(ch(quoteChar) ~ capture(zeroOrMore(!anyOf(quoteChar + "\\") ~ ANY | '\\' ~ ANY)) ~ ch(quoteChar)).named("stringLiteral")
  }

  val StringValue: Rule1[String] = rule { quotedString('"') | quotedString('\'') }

  val OptInfoEw: Rule1[Option[String]] = rule { (StringValue ~ Ew).? }

  val ElementNameLiteral: Rule0 = rule { '^'.? ~ alpha ~ alphaNum.* }

  val ElementName: Rule1[DecodeName] = rule { capture(ElementNameLiteral) ~> DecodeNameImpl.newFromSourceName _ }

  val ElementIdLiteral: Rule0 = rule { ElementNameLiteral.+('.') }

  val ElementId: Rule1[DecodeFqn] = rule { ElementName.+('.') ~> DecodeFqnImpl.apply _ }

  val Namespace: Rule1[DecodeNamespace] = rule {
    OptInfoEw ~ atomic("namespace") ~ Ew ~ ElementId ~>
      ((info: Option[String], fqn: DecodeFqn) => newNamespace(info, fqn))
  }

  private val ImportPart: Rule1[ImportPart] = rule {
    ElementName ~ (Ew ~ atomic("as") ~ Ew ~ ElementName ~> ((as: DecodeName, name: DecodeName) => ImportPartNameAlias(name, as))
      | MATCH ~> (ImportPartName(_: DecodeName)))
  }

  def addImport(fqn: DecodeFqn, importPart: ImportPart) {
    require(!imports.contains(importPart.alias))
    imports.put(importPart.alias, SimpleDecodeMaybeProxy.proxy(fqn, importPart.originalName))
  }

  def addImport(fqn: DecodeFqn) {
    require(!imports.contains(fqn.last.asMangledString))
    imports.put(fqn.last.asMangledString, SimpleDecodeMaybeProxy.proxy(fqn.copyDropLast(), fqn.last))
  }

  private var fqn: Option[DecodeFqn] = None
  val Import: Rule0 = rule {
    atomic("import") ~ Ew ~ ElementId ~> ((_fqn: DecodeFqn) => { fqn = Some(_fqn) }: Unit) ~
      ('.' ~ Ew.? ~ '{' ~ Ew.? ~ ImportPart.+(Ew.? ~ ',' ~ Ew.?)
        ~> ((parts: Seq[ImportPart]) => parts.foreach(addImport(fqn.get, _)))
        ~ Ew.? ~ ','.? ~ Ew.? ~ '}'
        | MATCH ~> (() => addImport(fqn.get)))
  }

  val NonNegativeIntegerLiteral : Rule0 = rule { CharPredicate.Digit.+ }

  val NonNegativeIntegerAsInt: Rule1[Int] = rule { capture(NonNegativeIntegerLiteral) ~> (Integer.parseInt(_: String, 10)) }

  private var ns: Option[DecodeNamespace] = None

  def makeNewSystemName(name: DecodeName): DecodeName = {
    DecodeNameImpl.newFromSourceName(name.asMangledString + '$')
  }

  def unitForFqn(unitFqn: DecodeFqn): DecodeMaybeProxy[DecodeUnit] = {
    if (unitFqn.size == 1 && imports.contains(unitFqn.asMangledString()))
      imports.get(unitFqn.asMangledString()).get.asInstanceOf[DecodeMaybeProxy[DecodeUnit]]
    else
      SimpleDecodeMaybeProxy.proxyDefaultNamespace(unitFqn, ns.get)
  }

  val UnitApplication: Rule1[DecodeFqn] = rule { '/' ~ ElementId ~ '/' }

  val TypeUnitApplication: Rule1[DecodeTypeUnitApplication] = rule {
    TypeApplication ~ (Ew.? ~ UnitApplication ~> (unitForFqn(_: DecodeFqn))).? ~>
      ((t: Option[DecodeMaybeProxy[DecodeType]], unit: Option[DecodeMaybeProxy[DecodeUnit]])
        => new DecodeTypeUnitApplicationImpl(t.get, unit))
  }

  val StructField: Rule1[DecodeStructField] = rule {
    OptInfoEw ~ TypeUnitApplication ~ Ew ~ ElementName ~>
      ((info: Option[String], typeUnit: DecodeTypeUnitApplication, name: DecodeName)
        => new DecodeStructFieldImpl(name, typeUnit, info))
  }

  val StructTypeFields: Rule1[Seq[DecodeStructField]] = rule {
    '(' ~ Ew.? ~ StructField.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  var componentName: Option[DecodeName] = None
  val ComponentParameters: Rule1[DecodeStructType] = rule {
    OptInfoEw ~ atomic("parameters") ~ Ew ~ StructTypeFields ~>
    ((info: Option[String], fields: Seq[DecodeStructField])
      => new DecodeStructTypeImpl(Some(makeNewSystemName(componentName.get)), ns.get, info, fields))
  }

  val CommandArg: Rule1[DecodeCommandArgument] = rule {
    OptInfoEw ~ TypeUnitApplication ~ Ew ~ ElementName ~>
      ((info: Option[String], typeUnit: DecodeTypeUnitApplication, name: DecodeName)
      => new DecodeCommandArgumentImpl(name, info, typeUnit.t, typeUnit.unit))
  }

  val CommandArgs: Rule1[Seq[DecodeCommandArgument]] = rule {
    '(' ~ CommandArg.*(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  val Command: Rule1[DecodeCommand] = rule {
    definition("command") ~ Ew.? ~ (':' ~ Ew.? ~ NonNegativeIntegerAsInt).? ~ Ew.? ~ CommandArgs ~
      (Ew.? ~ "->" ~ Ew.? ~ TypeApplication ~> (_.get)).? ~>
      ((info: Option[String], name: DecodeName, id: Option[Int], args: Seq[DecodeCommandArgument], returnType: Option[DecodeMaybeProxy[DecodeType]])
        => new DecodeCommandImpl(name, id, info, args, returnType))
  }

  val MessageParameterElement: Rule1[String] = rule {
    capture(ElementIdLiteral ~ ('[' ~ Ew.? ~ NonNegativeIntegerLiteral ~ Ew.? ~
      (atomic("..") ~ Ew.? ~ NonNegativeIntegerLiteral ~ Ew.?).? ~ ']'
      | '.' ~ ElementIdLiteral).*)
  }

  val MessageParameter: Rule1[DecodeMessageParameter] = rule {
    OptInfoEw ~ MessageParameterElement ~>
      ((info: Option[String], value: String) => new DecodeMessageParameterImpl(value, info))
  }

  val MessageParameters: Rule1[Seq[DecodeMessageParameter]] = rule {
    '(' ~ Ew.? ~ MessageParameter.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  val MessageNameId: Rule2[DecodeName, Option[Int]] = rule {
    ElementName ~ (Ew.? ~ ':' ~ Ew.? ~ NonNegativeIntegerAsInt).?
  }

  var component: Option[DecodeComponent] = None

  val EventMessage: Rule1[DecodeEventMessage] = rule {
    OptInfoEw ~ atomic("event") ~ Ew ~ MessageNameId ~ Ew ~ TypeApplication ~ Ew.? ~ MessageParameters ~>
      ((info: Option[String], name: DecodeName, id: Option[Int], baseType: Option[DecodeMaybeProxy[DecodeType]], parameters: Seq[DecodeMessageParameter])
        => new DecodeEventMessageImpl(component.get, name, id, info, parameters, baseType.get))
  }

  val StatusMessage: Rule1[DecodeStatusMessage] = rule {
    OptInfoEw ~ atomic("status") ~ Ew ~ MessageNameId ~ Ew.? ~ MessageParameters ~>
      ((info: Option[String], name: DecodeName, id: Option[Int], parameters: Seq[DecodeMessageParameter])
        => new DecodeStatusMessageImpl(component.get, name, id, info, parameters))
  }

  val Message: Rule1[DecodeMessage] = rule { StatusMessage | EventMessage }

  /*
  boolean addSubcomponent(@NotNull Buffer<DecodeComponentRef> componentRefs, @NotNull DecodeFqn fqn,
                            @NotNull DecodeNamespace namespace)
    {
        String alias = fqn.asString();
        if (fqn.size() == 1 && imports.contains(alias))
        {
            appendToBuffer(componentRefs, new DecodeComponentRefImpl((DecodeMaybeProxy<DecodeComponent>) (DecodeMaybeProxy<?>) Preconditions.checkNotNull(imports.get(
                    alias)), Some.apply(alias)));
        }
        else
        {
            appendToBuffer(componentRefs, new DecodeComponentRefImpl(proxyDefaultNamespace(fqn, namespace), Option.<String>empty()));
        }
        return true;
    }
   */

  val Component: Rule1[DecodeComponent] = rule {
    definition("component") ~> ((name: DecodeName) => { componentName = Some(name) }: Unit) ~ (Ew.? ~ ':' ~ NonNegativeIntegerAsInt).? ~
      (Ew ~ atomic("with") ~ Ew ~ ElementId.+(Ew.? ~ ',' ~ Ew.?)).? ~
      Ew.? ~ '{' ~ (Ew.? ~ ComponentParameters).? ~>
      ((info: Option[String], id: Option[Int], subComponents: Option[Seq[DecodeFqn]], baseType: Option[DecodeStructType])
        => new DecodeComponentImpl(componentName.get, ns.get, id, baseType.map(SimpleDecodeMaybeProxy.obj(_)), info,
          subComponents.map(_.toBuffer).getOrElse(mutable.Buffer()).map((fqn: DecodeFqn) => {
            val alias = fqn.asMangledString()
            if (fqn.size == 1 && imports.contains(alias))
              new DecodeComponentRefImpl(imports.get(alias).get.asInstanceOf[DecodeMaybeProxy[DecodeComponent]], Some(alias))
            else
              new DecodeComponentRefImpl(SimpleDecodeMaybeProxy.proxyDefaultNamespace(fqn, ns.get), None)
          }))) ~
      (Ew.? ~ (Command ~> ((command: DecodeCommand) => { component.get.commands += command }: Unit)
        | Message ~> ((message: DecodeMessage) => { component.get.messages += message }: Unit))).* ~
      Ew.? ~ '}'
  }

  val Unit_ : Rule1[DecodeUnit] = rule {
    definition("unit") ~ (Ew ~ atomic("display") ~ Ew ~ StringValue).? ~
      (Ew ~ atomic("placement") ~ Ew ~ atomic("before" | "after")).? ~>
      ((display: Option[String], name: DecodeName, info: Option[String]) => new DecodeUnitImpl(name, ns.get, display, info))
  }

  val FloatLiteral: Rule0 = rule {
    anyOf("+-").? ~ (NonNegativeIntegerLiteral ~ '.' ~ (NonNegativeIntegerLiteral).? | '.' ~ NonNegativeIntegerLiteral) ~
      (anyOf("eE") ~ anyOf("+-").? ~ NonNegativeIntegerLiteral)
  }

  val Literal: Rule0 = rule {
    FloatLiteral | NonNegativeIntegerLiteral | atomic("true") | atomic("false")
  }

  val EnumTypeValue: Rule1[DecodeEnumConstant] = rule {
    OptInfoEw ~ ElementName ~ Ew.? ~ '=' ~ Ew.? ~ capture(Literal) ~>
      ((info: Option[String], name: DecodeName, value: String)
        => new DecodeEnumConstantImpl(name, value, info))
  }

  val EnumTypeValues: Rule1[Seq[DecodeEnumConstant]] = rule { EnumTypeValue.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? }

  val EnumType = rule {
    OptInfoEw ~ atomic("enum") ~ Ew ~ TypeApplication ~ Ew.? ~ '(' ~ Ew.? ~ EnumTypeValues ~ Ew.? ~ ')' ~>
      ((name: DecodeName, info: Option[String], t: Option[DecodeMaybeProxy[DecodeType]], constants: Seq[DecodeEnumConstant])
        => new DecodeEnumTypeImpl(Some(name), ns.get, t.get, info, constants.to[mutable.Set]))
  }

  val StructType = rule {
    atomic("struct") ~ Ew.? ~ StructTypeFields ~>
      ((info: Option[String], name: DecodeName, fields: Seq[DecodeStructField])
        => new DecodeStructTypeImpl(Some(name), ns.get, info, fields))
  }

  val Type: Rule1[DecodeType] = rule {
    definition("type") ~ Ew ~ (
        EnumType
      | StructType
      | TypeApplication ~> ((info: Option[String], name: DecodeName, baseType: Option[DecodeMaybeProxy[DecodeType]])
        => new DecodeSubTypeImpl(Some(name), ns.get, info, baseType.get)))
  }

  def definition(keyword: String) = rule { OptInfoEw ~ atomic(keyword) ~ Ew ~ ElementName }

  /*
  Rule TypeApplicationAsOptionalProxyType()
    {
        Var<Buffer<Option<DecodeMaybeProxy<DecodeReferenceable>>>> genericTypesVar = new Var<>(new ArrayBuffer<>());
        Var<DecodeNamespace> namespaceVar = new Var<>();
        return Sequence(
                // FIXME: Parboiled bug
                namespaceVar.set((DecodeNamespace) pop()),
                FirstOf(
                        PrimitiveTypeApplicationAsOptionalProxyType(),
                        NativeTypeApplicationAsOptionalProxyType(),
                        // FIXME: bug with Var in parboiled
                        Sequence(push(namespaceVar.get()),
                                ArrayTypeApplicationAsOptionalProxyType()),
                        Sequence(ElementIdAsFqn(),
                                push(Option.apply(proxyForTypeFqn(namespaceVar.get(), (DecodeFqn) pop()))))),
                Optional(OptEW(), '<', push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                        append(genericTypesVar.get(), (Option<DecodeMaybeProxy<DecodeReferenceable>>) pop()),
                        ZeroOrMore(OptEW(), ',', OptEW(), push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                                append(genericTypesVar.get(), (Option<DecodeMaybeProxy<DecodeReferenceable>>) pop())),
                        Optional(OptEW(), ','), OptEW(), '>', push(Option.apply(SimpleDecodeMaybeProxy.proxyForTypeUriString(
                                getOrThrow((Option<DecodeMaybeProxy<DecodeReferenceable>>) pop()).proxy().uri() + "%3C" +
                                        genericTypesListToTypesUriString(genericTypesVar.get()) + "%3E", namespaceVar.get().fqn())))),
                Optional(OptEW(), '?',
                        push(Option.apply(proxyForSystem(DecodeNameImpl.newFromSourceName("optional<"
                                + getOrThrow((Option<DecodeMaybeProxy<DecodeReferenceable>>) pop()).proxy().uri().toString()
                                + ">"))))));
    }
   */

  def proxyForTypeFqn[T <: DecodeReferenceable](namespace: DecodeNamespace, typeFqn: DecodeFqn): DecodeMaybeProxy[T] = {
    if (typeFqn.size == 1 && imports.contains(typeFqn.last.asMangledString))
      imports.get(typeFqn.last.asMangledString).get.asInstanceOf[DecodeMaybeProxy[T]]
    else
      SimpleDecodeMaybeProxy.proxyDefaultNamespace(typeFqn, namespace)
  }

  val NativeTypeApplication: Rule1[Option[DecodeMaybeProxy[DecodeType]]] = rule {
    atomic("void") ~> (() => None) |
      capture(atomic("ber")) ~> ((t: String) => Some(SimpleDecodeMaybeProxy.proxyForSystem[DecodeType](DecodeNameImpl.newFromSourceName(t))))
  }

  val PrimitiveTypeKind: Rule0 = rule { atomic("uint" | "int" | "float" | "bool").named("primitiveTypeKind") }

  val PrimitiveTypeApplication: Rule1[DecodeMaybeProxy[DecodeType]] = rule {
    capture(PrimitiveTypeKind ~ ':' ~ NonNegativeIntegerLiteral) ~>
      ((t: String) => SimpleDecodeMaybeProxy.proxyForSystem[DecodeType](DecodeNameImpl.newFromSourceName(t)))
  }

  val LengthTo: Rule1[Int] = rule { NonNegativeIntegerAsInt | ch('*') ~> (() => -1) }

  val ArrayTypeApplication: Rule1[Option[DecodeMaybeProxy[DecodeType]]] = rule {
    capture('[' ~ Ew.? ~ TypeApplication ~ (Ew.? ~ ',' ~ Ew.? ~ NonNegativeIntegerAsInt
      ~ (Ew.? ~ ".." ~ Ew.? ~ LengthTo).? ~> ((_, _))).? ~ Ew.? ~ ']') ~>
      ((typeApplication: Option[DecodeMaybeProxy[DecodeType]], fromTo: Option[(Int, Option[Int])], capture: String)
        => Some(SimpleDecodeMaybeProxy.proxy[DecodeType](DecodeUtils.getNamespaceFqnFromUri(typeApplication.get.proxy.uri),
          DecodeNameImpl.newFromSourceName(capture))))
  }

  def TypeApplication: Rule1[Option[DecodeMaybeProxy[DecodeType]]] = rule {
    PrimitiveTypeApplication ~> (Some(_)) | NativeTypeApplication | ArrayTypeApplication | ElementId ~>
      ((fqn: DecodeFqn) => Some(proxyForTypeFqn[DecodeType](ns.get, fqn)))
  }

  val Alias: Rule1[DecodeAliasType] = rule {
    definition("alias") ~ Ew ~ TypeApplication ~>
      ((info: Option[String], name: DecodeName, typeAppOption: Option[DecodeMaybeProxy[DecodeType]]) =>
        new DecodeAliasTypeImpl(name, ns.get, typeAppOption.get, info))
  }

  val Language: Rule1[DecodeLanguage] = rule {
    definition("language") ~ (Ew ~ atomic("default") ~> (() => true) | MATCH ~> (() => false)) ~>
      ((info: Option[String], name: DecodeName, default: Boolean) => new DecodeLanguageImpl(name, ns.get, default, info))
  }

  val File: Rule1[DecodeNamespace] = rule {
    run(() => imports.clear()) ~ Namespace ~> ((_ns: DecodeNamespace) => { ns = Some(_ns) }: Unit) ~ (Ew ~ Import).* ~
      (Component ~> ((c: DecodeComponent) => { ns.get.components += c }: Unit)
        | Unit_ ~> ((u: DecodeUnit) => { ns.get.units += u }: Unit)
        | Type ~> ((t: DecodeType) => { ns.get.types += t }: Unit)
        | Alias ~> ((a: DecodeAliasType) => { ns.get.types += a }: Unit)
        | Language ~> ((l: DecodeLanguage) => { ns.get.languages += l }: Unit)) ~
      Ew.? ~ EOI ~> (() => ns.get)
  }

  def newNamespace(info: Option[String], fqn: DecodeFqn) = {
    var parentNamespace: Option[DecodeNamespace] = None
    if (fqn.size > 1)
      parentNamespace = Some(DecodeUtils.newNamespaceForFqn(fqn.copyDropLast()))
    var result: DecodeNamespace = DecodeNamespaceImpl(fqn.last, parentNamespace)
    parentNamespace.foreach(_.subNamespaces += result)
    result
  }
}

private trait ImportPart {
  def alias: String
  def originalName: DecodeName
}

private case class ImportPartName(originalName: DecodeName) extends ImportPart {
  def alias: String = originalName.asMangledString
}

private case class ImportPartNameAlias(originalName: DecodeName, _alias: DecodeName) extends ImportPart {
  def alias: String = _alias.asMangledString
}

class DecodeParser extends ru.mipt.acsl.parsing.Parser[Try[DecodeNamespace]] {
  override def parse(is: InputStream): Try[DecodeNamespace] = {
    new DecodeParboiledParser(Source.fromInputStream(is).mkString).File.run()
  }
}

case class DecodeSourceProviderConfiguration(resourcePath: String)

class DecodeSourceProvider {
  def provide(config: DecodeSourceProviderConfiguration): DecodeRegistry = {
    val resourcePath = config.resourcePath
    val parser = new DecodeParser()
    val registry = new DecodeRegistryImpl()
    registry.rootNamespaces ++= DecodeUtil.mergeRootNamespaces(Source.fromInputStream(getClass.getResourceAsStream(resourcePath)).getLines().
      filter(_.endsWith(".decode")).map(name => {
      parser.parse(getClass.getResourceAsStream(resourcePath + "/" + name)) match {
        case Success(v) => v
        case Failure(e) => throw e;
      }
    }).toTraversable)
    registry
  }
}