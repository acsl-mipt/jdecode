package ru.mipt.acsl.decode.parser


import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.lang.{Language => _, _}
import com.intellij.mock.{MockApplicationEx, MockProjectEx}
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileTypes.{FileTypeManager, FileTypeRegistry}
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.ProgressManagerImpl
import com.intellij.openapi.util.{Disposer, Getter}
import com.intellij.openapi.vfs.encoding.{EncodingManager, EncodingManagerImpl}
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.resolve.reference.{ReferenceProvidersRegistry, ReferenceProvidersRegistryImpl}
import com.typesafe.scalalogging.LazyLogging
import org.picocontainer.PicoContainer
import org.picocontainer.defaults.AbstractComponentAdapter
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.component.messages.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.domain.component.{Command, Component, ComponentRef}
import ru.mipt.acsl.decode.model.domain.expr.{FloatLiteral, IntLiteral}
import ru.mipt.acsl.decode.model.domain.impl._
import ru.mipt.acsl.decode.model.domain.impl.component.message.{EventMessage, MessageParameter, StatusMessage}
import ru.mipt.acsl.decode.model.domain.impl.component.{Command, Component, ComponentRef}
import ru.mipt.acsl.decode.model.domain.impl.naming.{ElementName, Fqn}
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.registry.{DecodeUnit, Language, Registry}
import ru.mipt.acsl.decode.model.domain.impl.types._
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy._
import ru.mipt.acsl.decode.model.domain.registry.{DecodeUnit, Language, Registry}
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, EnumType, TypeKind, TypeUnit}
import ru.mipt.acsl.decode.parser.psi.{DecodeUnit => PsiDecodeUnit, _}

import scala.collection.immutable.Seq
import scala.collection.{immutable, mutable}
import scala.io.Source
import scala.reflect.ClassTag

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

case class DecodeSourceProviderConfiguration(resourcePath: String)

class DecodeAstTransformer {

  import scala.collection.JavaConversions._

  private val imports = mutable.HashMap.empty[String, MaybeProxy[Referenceable]]
  private var ns: Namespace = _
  private var defaultLanguage: Option[Language] = None

  def processFile(node: ASTNode): Namespace = {
    val children = node.getChildren(null)
    assert(children.nonEmpty)
    val nsDecl = children.head.getPsi(classOf[DecodeNamespaceDecl])
    ns = DecodeUtils.newNamespaceForFqn(fqn(nsDecl.getElementId),
      info = elementInfo(Option(nsDecl.getInfoString)))
    children.drop(1).foreach(_.getPsi match {
      case p: DecodeTypeDecl =>
        val body = p.getTypeDeclBody
        val name = elementName(p.getElementNameRule)
        val _optionInfo = elementInfo(Option(p.getInfoString))
        ns.types = ns.types :+ Option(body.getEnumTypeDecl).map { e =>
          EnumType(name, ns, Option(e.getElementNameRule)
            .map(n => Left(proxyForFqn[EnumType](Fqn(Seq(elementName(n))))))
            .getOrElse(Right(typeApplication(Option(e.getTypeApplication)).get)), _optionInfo,
            e.getEnumTypeValues.getEnumTypeValueList.map { v =>
              val literal: DecodeLiteral = v.getLiteral
              EnumConstant(elementName(v.getElementNameRule),
              Option(literal.getFloatLiteral).map(l => FloatLiteral(l.getText.toFloat))
                .getOrElse(IntLiteral(literal.getNonNegativeNumber.getText.toInt)),
                elementInfo(Option(v.getInfoString)))
            }.to[immutable.Set], e.getFinalEnum != null)
        }.orElse {
          Option(body.getNativeTypeDecl).map(n =>
            Option(p.getGenericArgs).map(args => GenericType(name, ns, _optionInfo,
              args.getGenericArgList.map(arg => Option(arg.getElementNameRule).map(elementName))))
              .getOrElse(NativeType(name, ns, _optionInfo)))
        }.orElse {
          Option(body.getTypeApplication).map(s => SubType(name, ns, _optionInfo, typeApplication(Some(s)).get))
        }.getOrElse {
          StructType(name, ns, _optionInfo, body.getStructTypeDecl.getCommandArgs.getCommandArgList.map(cmdArg =>
            StructField(elementName(cmdArg.getElementNameRule), typeUnit(cmdArg.getTypeUnitApplication),
              elementInfo(Option(cmdArg.getInfoString)))))
        }
      case i: DecodeImportStmt =>
        val els = i.getImportElementList
        val iFqn: Fqn = fqn(i.getElementId)
        if (els.isEmpty) {
          imports.put(iFqn.last.asMangledString, MaybeProxy.proxy(iFqn.copyDropLast, TypeName(iFqn.last)))
        } else if (i.getImportElementStar != null) {
          sys.error("not implemented")
        } else {
          els.map { el =>
            val name = elementName(el.getElementNameRule)
            if (el.getImportElementAs != null)
              ImportPartNameAlias(name, elementName(el.getImportElementAs.getElementNameRule))
            else
              ImportPartName(name)
          }.foreach(p =>
            assert(imports.put(
              ElementName.newFromSourceName(p.alias).asMangledString,
              MaybeProxy.proxy(iFqn, TypeName(p.originalName))).isEmpty))
        }
      case u: DecodeUnitDecl =>
        ns.units = ns.units :+ DecodeUnit(elementName(u.getElementNameRule), ns,
          Option(u.getStringLiteral).map(string), elementInfo(Option(u.getInfoString)))
      case a: DecodeAliasDecl =>
        ns.types = ns.types :+ AliasType(elementName(a.getElementNameRule), ns,
          typeApplication(Some(a.getTypeApplication)).get, elementInfo(Option(a.getInfoString)))
      case c: DecodeComponentDecl =>
        val params = Option(c.getComponentParametersDecl).map { params =>
          val struct = StructType(makeNewSystemName(elementName(c.getElementNameRule)), ns,
            elementInfo(Option(params.getInfoString)),
            params.getCommandArgs.getCommandArgList.map { arg =>
              StructField(elementName(arg.getElementNameRule), typeUnit(arg.getTypeUnitApplication),
                elementInfo(Option(arg.getInfoString)))
            })
          ns.types = ns.types :+ struct
          MaybeProxy.obj(struct)
        }
        val component: Component = Component(elementName(c.getElementNameRule), ns,
          id(Option(c.getEntityId)), params, elementInfo(Option(c.getInfoString)),
          c.getSubcomponentDeclList.map(sc => componentRef(Fqn(Seq(elementName(sc.getElementNameRule)))))
            .to[Seq],
          c.getCommandDeclList.map(c => command(c)).to[Seq])
        ns.components = ns.components :+ component
        component.eventMessages ++= c.getMessageDeclList
          .flatMap(m => Option(m.getEventMessage).map(em => eventMessage(em, component)))
        component.statusMessages ++= c.getMessageDeclList
          .flatMap(m => Option(m.getStatusMessage).map(sm => statusMessage(sm, component)))
      case l: DecodeDefaultLanguageDecl =>
        defaultLanguage = Some(Language(elementName(l.getElementNameRule).asMangledString))
      case p: PsiWhiteSpace =>
      case p =>
        sys.error(s"not implemented for ${p.getClass}")
    })
    ns.rootNamespace
  }

  private def id(entityId: Option[DecodeEntityId]): Option[Int] =
    entityId.map(_.getNonNegativeNumber.getText.toInt)

  private def statusMessage(sm: DecodeStatusMessage, c: Component): StatusMessage =
    StatusMessage(c, elementName(sm.getElementNameRule), id(Option(sm.getEntityId)),
      elementInfo(Option(sm.getInfoString)),
      sm.getStatusMessageParametersDecl.getParameterDeclList.map(p =>
        MessageParameter(p.getParameterElement.getText, elementInfo(Option(p.getInfoString)))))

  private def eventMessage(em: DecodeEventMessage, c: Component): EventMessage =
    EventMessage(c, elementName(em.getElementNameRule), id(Option(em.getEntityId)),
      elementInfo(Option(em.getInfoString)),
      em.getEventMessageParametersDecl.getEventParameterDeclList.map { p =>
        Option(p.getParameterElement)
          .map(mp => Left(MessageParameter(mp.getText, elementInfo(Option(p.getInfoString)))))
          .getOrElse {
            val _var = p.getVarParameterElement
            Right(Parameter(elementName(_var.getElementNameRule), elementInfo(Option(p.getInfoString)),
              typeApplication(Some(_var.getTypeUnitApplication.getTypeApplication)).get,
              unit(Option(_var.getTypeUnitApplication.getUnit))))
          }
      }, typeApplication(Some(em.getTypeApplication)).get)

  private def command(c: DecodeCommandDecl): Command =
    Command(elementName(c.getElementNameRule), id(Option(c.getEntityId)),
      elementInfo(Option(c.getInfoString)), Option(c.getCommandArgs).map(_.getCommandArgList.toSeq).getOrElse(Seq.empty)
        .map { cmdArg =>
          Parameter(elementName(cmdArg.getElementNameRule), elementInfo(Option(cmdArg.getInfoString)),
            typeApplication(Some(cmdArg.getTypeUnitApplication.getTypeApplication)).get,
            unit(Option(cmdArg.getTypeUnitApplication.getUnit)))
        }.to[immutable.Seq], typeApplication(Option(c.getTypeUnitApplication).map(_.getTypeApplication)))

  private def componentRef(fqn: Fqn): ComponentRef = {
    val alias = fqn.asMangledString
    if (fqn.size == 1 && imports.contains(alias)) {
      val _import = imports.get(alias).get
      ComponentRef(_import.asInstanceOf[MaybeProxy[Component]],
        if (_import.proxy.path.element.mangledName.asMangledString.equals(alias))
          None
        else
          Some(alias))
    } else {
      ComponentRef(MaybeProxy.proxyDefaultNamespace(fqn, ns), None)
    }
  }

  private def elementName(nameRule: DecodeElementNameRule): ElementName =
    elementName(Option(nameRule.getElementNameToken).map(_.getText)
      .getOrElse(nameRule.getEscapedName.getText.substring(1)))

  private def elementName(str: String): ElementName = ElementName.newFromSourceName(str)

  private def makeNewSystemName(name: ElementName): ElementName =
    ElementName.newFromSourceName(name.asMangledString + '$')

  private def fqn(elementId: DecodeElementId): Fqn =
    Fqn(elementId.getElementNameRuleList.map(elementName))

  private def elementInfo(infoStr: Option[DecodeInfoString]): ElementInfo = infoStr match {
    case Some(i) => i.getStringValueList.map(info).toMap
    case _ => ElementInfo.empty
  }

  private def info(s: DecodeStringValue): (Language, String) = {
    Option(s.getElementNameRule) match {
      case Some(l) => (Language(elementName(l).asMangledString),
        string(s.getStringLiteral))
      case _ => (defaultLanguage.getOrElse(sys.error("default language not set")),
        string(s.getStringLiteral))
    }
  }

  private def string(s: DecodeStringLiteral): String = {
    val t = s.getText
    t.substring(1, t.length - 1)
  }

  private def unit(u: Option[PsiDecodeUnit]): Option[MaybeProxy[DecodeUnit]] =
    u.map(unit => proxyForFqn(fqn(unit.getElementId)))

  private def typeUnit(tu: DecodeTypeUnitApplication): TypeUnit =
    TypeUnit(typeApplication(Some(tu.getTypeApplication)).get, unit(Option(tu.getUnit)))

  private def proxyForFqn[T <: Referenceable : ClassTag](fqn: Fqn): MaybeProxy[T] =
    if (fqn.size == 1 && imports.contains(fqn.last.asMangledString))
    // todo: refactor out asInstanceOf
      imports.get(fqn.last.asMangledString).get.asInstanceOf[MaybeProxy[T]]
    else
      MaybeProxy.proxyDefaultNamespace[T](fqn, ns)

  private def typeApplication(ota: Option[DecodeTypeApplication]): Option[MaybeProxy[DecodeType]] =
    ota match {
      case Some(ta) =>
        val result = Option(ta.getArrayTypeApplication).map { ata =>
          val path = typeApplication(Some(ata.getTypeApplication)).get.proxy.path
          val fromTo = (Option(ata.getLengthFrom).map(_.getText), Option(ata.getLengthTo).map(_.getText))
          MaybeProxy.proxy[DecodeType](ProxyPath(path.ns, ArrayTypePath(path,
            ArraySize(fromTo._1.map(_.toLong).getOrElse(0l),
              fromTo._2.map(_.toLong).getOrElse(0l)))))
        }.orElse(Option(ta.getPrimitiveTypeApplication).map { pta =>
          MaybeProxy.proxyForSystem[DecodeType](PrimitiveTypeName(
            TypeKind.typeKindByName(pta.getPrimitiveTypeKind.getText).get, pta.getNonNegativeNumber.getText.toInt))
        }).getOrElse {
          val nta = ta.getNativeTypeApplication
          val proxy = proxyForFqn[DecodeType](fqn(nta.getElementId))
          Option(nta.getGenericParameters).map { params =>
            val path = proxy.proxy.path
            // todo: remove asInstanceOf
            MaybeProxy.proxy[DecodeType](ProxyPath(path.ns,
              GenericTypeName(path.element.asInstanceOf[TypeName].typeName,
                params.getTypeUnitApplicationList.map(p => typeApplication(Some(p.getTypeApplication))
                  .map(_.proxy.path)).to[immutable.Seq])))
          }.getOrElse(proxy)
        }
        Some(if (ta.getOptional != null) {
          MaybeProxy.proxyForSystem(GenericTypeName(ElementName.newFromMangledName("optional"),
            immutable.Seq(Some(result.proxy.path))))
        } else {
          result
        })
      case _ => None
    }
}

class DecodeSourceProvider extends LazyLogging {

  private object ParserBoilerplate {

    Extensions.registerAreaClass("IDEA_PROJECT", null)

    private val disposable = new Disposable {
      override def dispose(): Unit = sys.error("must not dispose?")
    }

    private def application: MockApplicationEx = ApplicationManager.getApplication.asInstanceOf[MockApplicationEx]

    private def initApplication(): Unit = {
      ApplicationManager.setApplication(new MockApplicationEx(disposable),
        new Getter[FileTypeRegistry]() {
          override def get: FileTypeRegistry = FileTypeManager.getInstance
        }, disposable)
      application.registerService(classOf[EncodingManager], classOf[EncodingManagerImpl])
    }

    private val project: MockProjectEx = new MockProjectEx(disposable)

    private def registerApplicationService[T](cls: Class[T], obj: T): Unit = {
      application.registerService(cls, obj)
      Disposer.register(project, new Disposable() {
        override def dispose(): Unit = application.getPicoContainer.unregisterComponent(cls.getName)
      })
    }

    def init(): Unit = {
      initApplication()
      application.getPicoContainer.registerComponent(new AbstractComponentAdapter(classOf[ProgressManager].getName, classOf[Object]) {
        override def getComponentInstance(container: PicoContainer): ProgressManager = new ProgressManagerImpl()

        override def verify(container: PicoContainer): Unit = {}
      })
      registerApplicationService(classOf[PsiBuilderFactory], new PsiBuilderFactoryImpl())
      registerApplicationService(classOf[DefaultASTFactory], new DefaultASTFactoryImpl())
      registerApplicationService(classOf[ReferenceProvidersRegistry], new ReferenceProvidersRegistryImpl())
      LanguageParserDefinitions.INSTANCE.addExplicitExtension(DecodeLanguage.INSTANCE, new DecodeParserDefinition)
    }
  }

  ParserBoilerplate.init()

  def provide(config: DecodeSourceProviderConfiguration): Registry = {
    val resourcePath = config.resourcePath
    val registry = Registry()
    val resourcesAsStream = getClass.getResourceAsStream(resourcePath)
    require(resourcesAsStream != null, resourcePath)
    registry.rootNamespaces ++= DecodeUtils.mergeRootNamespaces(Source.fromInputStream(resourcesAsStream).getLines().
      filter(_.endsWith(".decode")).map { name =>
      val resource = resourcePath + "/" + name
      logger.debug(s"Parsing $resource...")
      val parserDefinition = new DecodeParserDefinition()
      new DecodeAstTransformer().processFile(new DecodeParser().parse(DecodeParserDefinition.file, new PsiBuilderFactoryImpl().createBuilder(parserDefinition,
        parserDefinition.createLexer(null),
        Source.fromInputStream(getClass.getResourceAsStream(resource)).mkString)))
    }.toTraversable)
    registry
  }
}