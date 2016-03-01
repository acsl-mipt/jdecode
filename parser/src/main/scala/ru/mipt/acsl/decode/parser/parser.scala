package ru.mipt.acsl.decode.parser

import java.io.InputStream
import java.net.URLEncoder

import com.google.common.base.Charsets
import com.intellij.lang.{ASTNode, DefaultASTFactory, DefaultASTFactoryImpl, PsiBuilderFactory}
import com.intellij.lang.impl.{PsiBuilderFactoryImpl, PsiBuilderImpl}
import com.intellij.mock.{MockApplicationEx, MockProject, MockProjectEx}
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileTypes.{FileTypeManager, FileTypeRegistry}
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.ProgressManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.{Disposer, Getter}
import com.intellij.openapi.vfs.encoding.{EncodingManager, EncodingManagerImpl}
import com.intellij.psi.impl.source.resolve.reference.{ReferenceProvidersRegistry, ReferenceProvidersRegistryImpl}
import com.intellij.psi.impl.source.tree.FileElement
import com.typesafe.scalalogging.LazyLogging
import org.parboiled2.ParserInput.StringBasedParserInput
import org.parboiled2._
import org.picocontainer.PicoContainer
import org.picocontainer.defaults.AbstractComponentAdapter
import ru.mipt.acsl.decode.model.domain.impl._
import ru.mipt.acsl.decode.model.domain.impl.types._
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.proxy._
import ru.mipt.acsl.decode.parser.psi.DecodeTypes

import scala.collection.{immutable, mutable}
import scala.io.Source
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

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

  def ElementName_ : Rule1[ElementName] = rule { capture(ElementNameLiteral) ~> ElementName.newFromSourceName _ }

  def ElementIdLiteral: Rule0 = rule { ElementNameLiteral.+('.') }

  def ElementId: Rule1[Fqn] = rule { ElementName_.+('.') ~> Fqn.apply _ }

  def Namespace_ : Rule1[Namespace] = rule {
    InfoEw.? ~ atomic("namespace") ~ Ew ~ ElementId ~>
      ((info: Option[String], fqn: Fqn) => newNamespace(info, fqn))
  }

  private def ImportPart: Rule1[ImportPart] = rule {
    ElementName_ ~ (Ew ~ atomic("as") ~ Ew ~ ElementName_
      ~> ((as: ElementName, name: ElementName) => ImportPartNameAlias(name, as))
      | MATCH ~> (ImportPartName(_: ElementName)))
  }

  def addImport(fqn: Fqn, importPart: ImportPart) {
    require(!imports.contains(importPart.alias), importPart.alias)
    imports.put(importPart.alias, MaybeProxy.proxy(fqn, TypeName(importPart.originalName)))
  }

  def addImport(fqn: Fqn) {
    require(!imports.contains(fqn.last.asMangledString), fqn.last.asMangledString)
    imports.put(fqn.last.asMangledString, MaybeProxy.proxy(fqn.copyDropLast, TypeName(fqn.last)))
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

  private def makeNewSystemName(name: ElementName): ElementName =
    ElementName.newFromSourceName(name.asMangledString + '$')

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
    new TypeUnitImpl(t.get, unit)

  def TypeUnitApplication: Rule1[TypeUnit] = rule {
    TypeApplication ~ (Ew.? ~ UnitApplication ~> (unitForFqn(_: Fqn))).? ~> newTypeUnitApplication _
  }

  private def newStructField(info: Option[String], typeUnit: TypeUnit, name: ElementName) =
    new StructFieldImpl(name, typeUnit, info)

  def StructField: Rule1[StructField] = rule {
    InfoEw.? ~ TypeUnitApplication ~ Ew ~ ElementName_ ~> newStructField _
  }

  def StructTypeFields: Rule1[Seq[StructField]] = rule {
    '(' ~ Ew.? ~ StructField.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  var componentName: Option[ElementName] = None

  def ComponentParameters: Rule1[StructType] = rule {
    InfoEw.? ~ atomic("parameters") ~> (() => makeNewSystemName(componentName.get)) ~ Ew ~ StructTypeFields ~>
      newStructType _
  }

  private def newCommandArg(info: Option[String], typeUnit: TypeUnit, name: ElementName) =
    new ParameterImpl(name, info, typeUnit.t, typeUnit.unit)

  def CommandArg: Rule1[Parameter] = rule {
    InfoEw.? ~ TypeUnitApplication ~ Ew ~ ElementName_ ~> newCommandArg _
  }

  def CommandArgs: Rule1[immutable.Seq[Parameter]] = rule {
    '(' ~ Ew.? ~ CommandArg.*(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  private def newCommand(info: Option[String], name: ElementName, id: Option[Int],
                         args: immutable.Seq[Parameter],
                         returnType: Option[MaybeProxy[DecodeType]]) =
    new CommandImpl(name, id, info, args, returnType)

  def Command: Rule1[Command] = rule {
    definition("command") ~ Id.? ~ Ew.? ~ CommandArgs ~
      (Ew.? ~ "->" ~ Ew.? ~ TypeApplication ~> (_.get)).? ~> newCommand _
  }

  def MessageParameterElement: Rule1[String] = rule {
    capture(ElementIdLiteral ~ ('[' ~ Ew.? ~ NonNegativeIntegerLiteral ~ Ew.? ~
      (atomic("src/main") ~ Ew.? ~ NonNegativeIntegerLiteral ~ Ew.?).? ~ ']'
      | '.' ~ ElementIdLiteral).*)
  }

  private def newMessageParameter(info: Option[String], value: String) = new MessageParameterImpl(value, info)

  def MessageParameter: Rule1[MessageParameter] = rule {
    InfoEw.? ~ MessageParameterElement ~> newMessageParameter _
  }

  def EventMessageParameter: Rule1[Either[MessageParameter, Parameter]] = rule {
    InfoEw.? ~ ((atomic("var") ~ Ew ~ TypeUnitApplication ~ Ew ~ ElementName_ ~> newCommandArg _
      ~> Right[MessageParameter, Parameter] _)
      | (MessageParameterElement ~> newMessageParameter _ ~> Left[MessageParameter, Parameter] _))
  }

  def EventMessageParameters: Rule1[Seq[Either[MessageParameter, Parameter]]] = rule {
    '(' ~ Ew.? ~ EventMessageParameter.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  def MessageParameters: Rule1[Seq[MessageParameter]] = rule {
    '(' ~ Ew.? ~ MessageParameter.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? ~ Ew.? ~ ')'
  }

  def MessageNameId: Rule2[ElementName, Option[Int]] = rule { ElementName_ ~ Id.? }

  var component: Option[Component] = None

  private def newEventMessage(info: Option[String], name: ElementName, id: Option[Int],
                              baseType: Option[MaybeProxy[DecodeType]],
                              parameters: Seq[Either[MessageParameter, Parameter]]) =
    new EventMessageImpl(component.get, name, id, info, parameters, baseType.get)

  def EventMessage: Rule1[EventMessage] = rule {
    InfoEw.? ~ atomic("event") ~ Ew ~ MessageNameId ~ Ew ~ TypeApplication ~ Ew.? ~ EventMessageParameters ~>
      newEventMessage _
  }

  private def newStatusMessage(info: Option[String], name: ElementName, id: Option[Int], priority: Option[Int],
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
      if (fqn.size == 1 && imports.contains(alias)) {
        val _import = imports.get(alias).get
        new ComponentRefImpl(_import.asInstanceOf[MaybeProxy[Component]],
          if (_import.proxy.path.element.mangledName.asMangledString.equals(alias)) None else Some(alias))
      } else
        new ComponentRefImpl(MaybeProxy.proxyDefaultNamespace(fqn, ns.get), None)
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

  private def newMeasure(display: Option[String], name: ElementName, info: Option[String]) =
    new MeasureImpl(name, ns.get, display, info)

  def Unit_ : Rule1[Measure] = rule {
    definition("unit") ~ (Ew ~ atomic("display") ~ Ew ~ StringValue).? ~
      (Ew ~ atomic("placement") ~ Ew ~ atomic("before" | "after")).? ~> newMeasure _
  }

  def FloatLiteral: Rule0 = rule {
    anyOf("+-").? ~ (NonNegativeIntegerLiteral ~ '.' ~ NonNegativeIntegerLiteral.? | '.' ~ NonNegativeIntegerLiteral) ~
      (anyOf("eE") ~ anyOf("+-").? ~ NonNegativeIntegerLiteral)
  }

  def Literal: Rule0 = rule {
    FloatLiteral | NonNegativeIntegerLiteral | atomic("true") | atomic("false")
  }

  private def newEnumConstant(info: Option[String], name: ElementName, value: String) =
    new EnumConstantImpl(name, value, info)

  def EnumTypeValue: Rule1[EnumConstant] = rule {
    InfoEw.? ~ ElementName_ ~ Ew.? ~ '=' ~ Ew.? ~ capture(Literal) ~> newEnumConstant _
  }

  def EnumTypeValues: Rule1[Seq[EnumConstant]] = rule { EnumTypeValue.+(Ew.? ~ ',' ~ Ew.?) ~ (Ew.? ~ ',').? }

  private def newEnumType(info: Option[String], name: ElementName, genericParameters: Option[Seq[ElementName]],
                          isFinal: Boolean,
                          t: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
                          constants: Seq[EnumConstant]): EnumType = {
    require(genericParameters.isEmpty, "not implemented")
    new EnumTypeImpl(name, ns.get, t, info, constants.to[immutable.Set], isFinal)
  }

  def ExtendsEnumType: Rule1[Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]]] = rule {
    ElementId ~> { (fqn: Fqn) => Left(proxyForTypeFqn[EnumType](ns.get, fqn)) }
  }

  def EnumType = rule {
    (atomic("final") ~ Ew ~> (() => true) | MATCH ~> (() => false)) ~ atomic("enum") ~ Ew ~
      (atomic("extends") ~ Ew ~ ExtendsEnumType | TypeApplication ~> { (ta: Option[MaybeProxy[DecodeType]]) =>
        Right[MaybeProxy[EnumType], MaybeProxy[DecodeType]](ta.get)
      }) ~ Ew.? ~ '(' ~ Ew.? ~ EnumTypeValues ~ Ew.? ~ ')' ~> newEnumType _
  }

  private def newStructType(info: Option[String], name: ElementName, fields: Seq[StructField]): StructType =
    newStructTypeWithParameters(info, name, None, fields)

  private def newStructTypeWithParameters(info: Option[String], name: ElementName, genericParameters: Option[Seq[Option[ElementName]]],
                            fields: Seq[StructField]): StructType = {
    require(genericParameters.isEmpty, "not implemented")
    StructType(name, ns.get, info, fields)
  }

  def StructType_ = rule { atomic("struct") ~ Ew.? ~ StructTypeFields ~> newStructTypeWithParameters _ }

  private def newSubType(info: Option[String], name: ElementName, genericParameters: Option[Seq[ElementName]],
                         baseType: Option[MaybeProxy[DecodeType]]): SubType = {
    require(genericParameters.isEmpty, "not implemented")
    SubType(name, ns.get, info, baseType.get)
  }

  private def newNativeType(info: Option[String], name: ElementName,
                            typeParameters: Option[Seq[Option[ElementName]]]): DecodeType =
    typeParameters.map(gp => GenericType(name, ns.get, info, gp))
      .getOrElse(NativeType(name, ns.get, info))

  def NativeType_ = rule { atomic("native") ~> newNativeType _ }

  def TypeParameters: Rule1[Seq[Option[ElementName]]] = rule {
    '<' ~ Ew.? ~ ElementName_.?.*(Ew.? ~ ',' ~ Ew.?) ~ Ew.? ~ '>'
  }

  def Type: Rule1[DecodeType] = rule {
    definition("type") ~ (Ew.? ~ TypeParameters).? ~ Ew ~ (
        EnumType
      | StructType_
      | NativeType_
      | TypeApplication ~> newSubType _)
  }

  def definition(keyword: String) = rule { InfoEw.? ~ atomic(keyword) ~ Ew ~ ElementName_ }

  def proxyForTypeFqn[T <: Referenceable : ClassTag](namespace: Namespace, typeFqn: Fqn): MaybeProxy[T] =
    if (typeFqn.size == 1 && imports.contains(typeFqn.last.asMangledString))
      imports.get(typeFqn.last.asMangledString).get.asInstanceOf[MaybeProxy[T]]
    else
      MaybeProxy.proxyDefaultNamespace[T](typeFqn, namespace)

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
        ArraySize(fromTo.map(_._1.toLong).getOrElse(0l),
          fromTo.map(_._2.map(_.toLong).getOrElse(0l)).getOrElse(0l))))))
  }

  def ArrayTypeApplication: Rule1[Option[MaybeProxy[DecodeType]]] = rule {
    '[' ~ Ew.? ~ TypeApplication ~ (Ew.? ~ ',' ~ Ew.? ~ NonNegativeIntegerAsInt
      ~ (Ew.? ~ "src/main" ~ Ew.? ~ LengthTo).? ~> ((_, _))).? ~ Ew.? ~ ']' ~> newTypeProxy _
  }

  private def newGenericTypeProxy(b: Option[MaybeProxy[DecodeType]],
                                  g: Seq[Option[MaybeProxy[DecodeType]]]): Option[MaybeProxy[DecodeType]] = {
    val path = b.get.proxy.path
    // TODO: remove asInstanceOf
    Some(MaybeProxy.proxy(ProxyPath(path.ns, GenericTypeName(path.element.asInstanceOf[TypeName].typeName,
      g.map(_.map(_.proxy.path)).to[immutable.Seq]))))
  }

  private def newOptionalTypeProxy(t: Option[MaybeProxy[DecodeType]]): Option[MaybeProxy[DecodeType]] =
    Some(MaybeProxy.proxyForSystem(GenericTypeName(ElementName.newFromMangledName("optional"),
      immutable.Seq(Some(t.get.proxy.path)))))

  def TypeApplication: Rule1[Option[MaybeProxy[DecodeType]]] = rule {
    (PrimitiveTypeApplication ~> (Some(_)) | ArrayTypeApplication
      | ElementId ~> { fqn => Some(proxyForTypeFqn[DecodeType](ns.get, fqn))}) ~
    (Ew.? ~ '<' ~ TypeApplication.+(Ew.? ~ ',' ~ Ew.?) ~ ','.? ~ Ew.? ~ '>' ~> newGenericTypeProxy _).? ~
    (Ew.? ~ '?' ~> newOptionalTypeProxy _).?
  }

  private def newAliasType(info: Option[String], name: ElementName,
                           typeAppOption: Option[MaybeProxy[DecodeType]]) =
    new AliasTypeImpl(name, ns.get, typeAppOption.get, info)

  def Alias: Rule1[AliasType] = rule { definition("alias") ~ Ew ~ TypeApplication ~> newAliasType _ }

  private def newLanguage(info: Option[String], name: ElementName, default: Boolean) =
    new LanguageImpl(name, ns.get, default, info)

  def Language: Rule1[Language] = rule {
    definition("language") ~ (Ew ~ atomic("default") ~> (() => true) | MATCH ~> (() => false)) ~> newLanguage _
  }

  // todo: refactoring
  def File: Rule1[Namespace] = rule {
    run(imports.clear()) ~ Namespace_ ~>
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
      parentNamespace = Some(DecodeUtils.newNamespaceForFqn(fqn.copyDropLast))
    val result = Namespace(fqn.last, parent = parentNamespace)
    parentNamespace.foreach(ns => ns.subNamespaces = ns.subNamespaces :+ result)
    result
  }
}

private sealed trait ImportPart {
  def alias: String
  def originalName: ElementName
}

private case class ImportPartName(originalName: ElementName) extends ImportPart {
  def alias: String = originalName.asMangledString
}

private case class ImportPartNameAlias(originalName: ElementName, _alias: ElementName) extends ImportPart {
  def alias: String = _alias.asMangledString
}

object OldDecodeParser {
  def parse(input: ParserInput): Try[Namespace] = {
    val parser: DecodeParboiledParser = new DecodeParboiledParser(input)
    parser.File.run()
  }
}

case class DecodeSourceProviderConfiguration(resourcePath: String)

class DecodeSourceProvider extends LazyLogging {

  def processNode(node: ASTNode): Namespace = {
    Namespace(ElementName.newFromMangledName("t"))
  }

  val disposable = new Disposable {
    override def dispose(): Unit = sys.error("must not dispose?")
  }

  def getApplication: MockApplicationEx = ApplicationManager.getApplication.asInstanceOf[MockApplicationEx]

  def initApplication(): Unit = {
    //if (ApplicationManager.getApplication() instanceof MockApplicationEx) return;
    val instance = new MockApplicationEx(disposable)
    ApplicationManager.setApplication(instance,
      new Getter[FileTypeRegistry]() {
        override def get: FileTypeRegistry = FileTypeManager.getInstance
      }, disposable)
    getApplication.registerService(classOf[EncodingManager], classOf[EncodingManagerImpl])
  }

  Extensions.registerAreaClass("IDEA_PROJECT", null);
  private val project: MockProjectEx = new MockProjectEx(disposable)

  protected def registerApplicationService[T](aClass: Class[T] , obj: T): Unit = {
    getApplication.registerService(aClass, obj)
    Disposer.register(project, new Disposable() {
      override def dispose(): Unit = getApplication.getPicoContainer.unregisterComponent(aClass.getName)
    })
  }

  def provideNew(config: DecodeSourceProviderConfiguration): Registry = {
    val resourcePath = config.resourcePath
    val registry = Registry()
    val resourcesAsStream = getClass.getResourceAsStream(resourcePath)
    require(resourcesAsStream != null, resourcePath)
    initApplication()
    getApplication.getPicoContainer.registerComponent(new AbstractComponentAdapter(classOf[ProgressManager].getName, classOf[Object]) {
      override def getComponentInstance(container: PicoContainer): ProgressManager = new ProgressManagerImpl()

      override def verify(container: PicoContainer): Unit = { }
    })
    registerApplicationService(classOf[PsiBuilderFactory], new PsiBuilderFactoryImpl())
    registerApplicationService(classOf[DefaultASTFactory], new DefaultASTFactoryImpl())
    registerApplicationService(classOf[ReferenceProvidersRegistry], new ReferenceProvidersRegistryImpl())
    registry.rootNamespaces ++= DecodeUtils.mergeRootNamespaces(Source.fromInputStream(resourcesAsStream).getLines().
      filter(_.endsWith(".decode")).map { name =>
      val resource: String = resourcePath + "/" + name
      logger.debug(s"Parsing $resource...")
      val parserDefinition = new DecodeParserDefinition()
      processNode(new DecodeParser().parse(DecodeParserDefinition.file, new PsiBuilderFactoryImpl().createBuilder(parserDefinition,
        parserDefinition.createLexer(null),
        Source.fromInputStream(getClass.getResourceAsStream(resource)).mkString)))
    }.toTraversable)
    registry
  }

  def provide(config: DecodeSourceProviderConfiguration): Registry = {
    val resourcePath = config.resourcePath
    val registry = Registry()
    val resourcesAsStream = getClass.getResourceAsStream(resourcePath)
    require(resourcesAsStream != null, resourcePath)
    initApplication()
    getApplication.getPicoContainer.registerComponent(new AbstractComponentAdapter(classOf[ProgressManager].getName, classOf[Object]) {
      override def getComponentInstance(container: PicoContainer): ProgressManager = new ProgressManagerImpl()

      override def verify(container: PicoContainer): Unit = { }
    })
    registerApplicationService(classOf[PsiBuilderFactory], new PsiBuilderFactoryImpl())
    registerApplicationService(classOf[DefaultASTFactory], new DefaultASTFactoryImpl())
    registerApplicationService(classOf[ReferenceProvidersRegistry], new ReferenceProvidersRegistryImpl())
    registry.rootNamespaces ++= DecodeUtils.mergeRootNamespaces(Source.fromInputStream(resourcesAsStream).getLines().
      filter(_.endsWith(".decode")).map { name =>
      val resource: String = resourcePath + "/" + name
      logger.debug(s"Parsing $resource...")
      val input: StringBasedParserInput = Source.fromInputStream(getClass.getResourceAsStream(resource)).mkString
      OldDecodeParser.parse(input) match {
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