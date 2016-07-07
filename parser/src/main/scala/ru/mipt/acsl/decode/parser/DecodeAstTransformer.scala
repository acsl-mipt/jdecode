package ru.mipt.acsl.decode.parser

import java.util
import java.util.{Collections, Optional}

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.annotations.Nullable
import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.component.message._
import ru.mipt.acsl.decode.model.component.{Command, Component, MessageParameterPath, MessageParameterPathElement}
import ru.mipt.acsl.decode.model.expr.{BigDecimalLiteral, BigIntLiteral}
import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.proxy.path.{GenericTypeName, ProxyPath, TypeName}
import ru.mipt.acsl.decode.model.proxy._
import ru.mipt.acsl.decode.model.registry.{Language, Measure}
import ru.mipt.acsl.decode.model.types.{Alias, EnumConstant, EnumType, _}
import ru.mipt.acsl.decode.parser.psi.{DecodeUnit => PsiDecodeUnit, _}

import scala.collection.mutable

/**
  * @author Artem Shein
  */
class DecodeAstTransformer {

  import scala.collection.JavaConversions._

  private val imports = mutable.HashMap.empty[String, MaybeProxyReferenceable]
  private var ns: Namespace = _
  private var defaultLanguage: Option[Language] = None

  def processFile(node: ASTNode): Namespace = {
    var children = node.getChildren(null)
    assert(children.nonEmpty)
    while (children.head.getPsi.isInstanceOf[PsiWhiteSpace])
      children = children.tail
    val nsDecl = children.head.getPsi(classOf[DecodeNamespaceDecl])
    val rootNs = Namespace.newInstanceRoot()
    ns = Namespace.newInstance(fqn(nsDecl.getElementId), rootNs, elementInfo(Option(nsDecl.getElementInfo)))
    children.drop(1).foreach(_.getPsi match {
      case s: DecodeSubTypeDecl =>
        ns.objects.addAll(newType(s))
      case e: DecodeEnumTypeDecl =>
        ns.objects.addAll(newType(e))
      case s: DecodeStructTypeDecl =>
        ns.objects.addAll(newType(s))
      case n: DecodeNativeTypeDecl =>
        ns.objects.addAll(newType(n))
      case a: DecodeAliasDecl if a.getTypeUnitApplication.getUnit != null =>
        ns.objects.add(newTypeMeasure(a))
      case a: DecodeAliasDecl =>
        ns.objects.add(newType(a))
      case i: DecodeImportStmt =>
        val els = i.getImportElementList
        val iFqn: Fqn = fqn(i.getElementId)
        if (els.isEmpty) {
          imports.put(iFqn.last.mangledNameString, MaybeProxyReferenceable.newInstance(Proxy(ProxyPath(iFqn.copyDropLast, TypeName(iFqn.last)))))
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
              ElementName.newInstanceFromSourceName(p.alias).mangledNameString(),
              MaybeProxyReferenceable.newInstance(Proxy(ProxyPath(iFqn, TypeName(p.originalName))))).isEmpty))
        }
      case m: DecodeMeasureDecl =>
        val alias = new Alias.NsMeasure(elementName(m.getElementNameRule), elementInfo(Option(m.getElementInfo)), ns, null)
        alias.obj(MaybeProxyMeasure.newInstance(Measure(alias, localizedStrings(m.getStringValueList))))
        ns.objects.addAll(Seq(alias, alias.obj().obj()))
      case c: DecodeComponentDecl =>
        val params = Option(c.getComponentParametersDecl).map { params =>
          val struct = StructType(null, ns, new util.ArrayList[Referenceable](), new util.ArrayList[ElementName]())
          struct.objects.addAll(params.getCommandArgs.getCommandArgList.zipWithIndex.flatMap { case (arg, idx) =>
            val alias = new Alias.StructField(elementName(arg.getElementNameRule),
              elementInfo(Option(arg.getElementInfo)), struct, null)
            alias.obj(StructField(alias, typeMeasure(arg.getTypeUnitApplication), idx))
            Seq(alias, alias.obj())
          })
          ns.objects.add(struct)
          MaybeProxyStructType.newInstance(struct)
        }
        val alias = new Alias.NsComponent(elementName(c.getElementNameRule), elementInfo(Option(c.getElementInfo)), ns, null)
        val component = Component.newInstance(alias, ns, id(c.getAnnotationDeclList), params.orNull, new util.ArrayList[Referenceable]())
        alias.obj(MaybeProxyComponent.newInstance(component))
        alias.obj().obj().objects.addAll(c.getComponentRefList.map(sc =>
          componentRef(component, Fqn.newInstance(Seq(elementName(sc.getElementNameRule))))) ++
          c.getCommandDeclList.flatMap(c => command(c, component)))
        ns.objects.addAll(Seq(alias, alias.obj().obj()))
        component.objects.addAll(c.getMessageDeclList
          .flatMap(m => Option(m.getEventMessage).seq.flatMap(eventMessage(_, component))) ++
          c.getMessageDeclList
            .flatMap(m => Option(m.getStatusMessage).seq.flatMap(statusMessage(_, component))))
      case l: DecodeLanguageDecl =>
        defaultLanguage = Some(Language.newInstance(elementName(l.getElementNameRule).mangledNameString()))
      case c: DecodeConstDecl =>
        val alias = new Alias.NsConst(elementName(c.getElementNameRule), elementInfo(Option(c.getElementInfo)), ns, null)
        alias.obj(MaybeProxyConst.newInstance(Const.newInstance(alias, ns, c.getLiteral.getText, util.Collections.emptyList())))
        ns.objects.addAll(Seq(alias, alias.obj().obj()))
      case s: DecodeScriptDecl =>
        //sys.error("not implemented")
      case p: PsiWhiteSpace =>
      case p =>
        sys.error(s"not implemented for ${p.getText} (${p.getClass} $p) at ${p.getTextOffset}")
    })
    ns.rootNamespace()
  }

  private def newType(a: DecodeAliasDecl): Alias.NsType = {
    val alias = new Alias.NsType(elementName(a.getElementNameRule), elementInfo(Option(a.getElementInfo)), ns, null)
    alias.obj(typeApplication(a.getTypeUnitApplication.getTypeApplication))
    alias
  }

  private def newTypeMeasure(a: DecodeAliasDecl): Alias.NsTypeMeasure =
    new Alias.NsTypeMeasure(elementName(a.getElementNameRule), elementInfo(Option(a.getElementInfo)), ns, typeMeasure(a.getTypeUnitApplication))

  private def newType(e: DecodeEnumTypeDecl): Seq[Referenceable] = {
    val alias = new Alias.NsType(elementName(e.getEnumName.getElementNameRule), elementInfo(Option(e.getElementInfo)), ns, null)
    alias.obj(MaybeProxyType.newInstance(newEnumType(e, alias, typeParameters(Option(e.getGenericParameters)))))
    Seq(alias, alias.obj.obj())
  }

  private def newType(n: DecodeNativeTypeDecl): Seq[Referenceable] = {
    val alias = new Alias.NsType(elementName(n.getElementNameRule), elementInfo(Option(n.getElementInfo)), ns, null)
    alias.obj(MaybeProxyType.newInstance(NativeType(alias, ns, typeParameters(Option(n.getGenericParameters)))))
    Seq(alias, alias.obj.obj())
  }

  private def newType(s: DecodeStructTypeDecl): Seq[Referenceable] = {
    val alias = new Alias.NsType(elementName(s.getElementNameRule), elementInfo(Option(s.getElementInfo)), ns, null)
    alias.obj(MaybeProxyType.newInstance(newStructType(alias, s.getCommandArgs, typeParameters(Option(s.getGenericParameters)))))
    Seq(alias, alias.obj.obj())
  }

  private def newType(s: DecodeSubTypeDecl): Seq[Referenceable] = {
    val alias = new Alias.NsType(elementName(s.getElementNameRule), elementInfo(Option(s.getElementInfo)), ns, null)
    alias.obj(MaybeProxyType.newInstance(SubType(alias, ns, typeMeasure(s.getTypeUnitApplication),
      typeParameters(Option(s.getGenericParameters)))))
    Seq(alias, alias.obj.obj())
  }

  private def typeParameters(ga: Option[DecodeGenericParameters]): Seq[ElementName] =
    ga.toSeq.flatMap(_.getGenericParameterList.map(arg => elementName(arg.getElementNameRule)))

  private def newEnumType(e: DecodeEnumTypeDecl, alias: Alias.NsType,
                          typeParameters: Seq[ElementName]): EnumType = {
    val enum = EnumType.newInstance(alias, ns, Option(e.getElementNameRule)
      .map(n => new MaybeProxyEnumOrTypeMeasure(MaybeProxyEnumType.newInstance(proxyForFqn(Fqn.newInstance(Seq(elementName(n)))).proxy)))
      .getOrElse(new MaybeProxyEnumOrTypeMeasure(typeMeasure(e.getTypeUnitApplication))),
      new util.ArrayList[Referenceable](), e.getFinalEnum != null,
      typeParameters)
    alias.obj(MaybeProxyType.newInstance(enum))
    enum.objects.addAll(e.getEnumTypeValues.getEnumTypeValueList.flatMap(v => newEnumConstant(v, enum)))
    enum
  }

  private def newStructType(alias: Alias.NsType, args: DecodeCommandArgs, typeParameters: Seq[ElementName]): StructType = {
    val t = StructType(alias, ns, Collections.emptyList(), typeParameters)
    t.setObjects(args.getCommandArgList.zipWithIndex.flatMap { case (cmdArg, idx) =>
      val alias = new Alias.StructField(elementName(cmdArg.getElementNameRule),
        elementInfo(Option(cmdArg.getElementInfo)), t, null)
      alias.obj(StructField(alias, typeMeasure(cmdArg.getTypeUnitApplication), idx))
      Seq(alias, alias.obj())
    })
    t
  }

  private def newEnumConstant(v: DecodeEnumTypeValue, enum: EnumType): Seq[Referenceable] = {
    val literal = v.getLiteral
    val numericLiteral = literal.getNumericLiteral
    val alias = new Alias.EnumConstant(elementName(v.getElementNameRule), elementInfo(Option(v.getElementInfo)), enum, null)
    alias.obj(EnumConstant(alias, Option(numericLiteral.getFloatLiteral).map(l => BigDecimalLiteral(l.getText))
        .getOrElse(BigIntLiteral(numericLiteral.getIntegerLiteral.getText))))
    Seq(alias, alias.obj)
  }

  @Nullable
  private def id(annotations: Seq[DecodeAnnotationDecl]): Integer =
    annotations.find(a => string(a.getElementNameRule) == "id") match {
      case Some(annotation) => annotation.getAnnotationParameterList.headOption.map(p => Integer.valueOf(p.getText)).get
      case None => null
    }

  private def statusMessage(sm: DecodeStatusMessage, c: Component): Seq[Referenceable] = {
    val alias = new Alias.ComponentStatusMessage(elementName(sm.getElementNameRule),
      elementInfo(Option(sm.getElementInfo)), c, null)
    alias.obj(StatusMessage(alias, c, id(sm.getAnnotationDeclList),
      sm.getStatusMessageParametersDecl.getParameterDeclList.map(p =>
        StatusParameter(parameterPath(p.getParameterElement), elementInfo(Option(p.getElementInfo)))), null))
    Seq(alias, alias.obj)
  }

  private def parameterPath(el: DecodeParameterElement): MessageParameterPath =
    MessageParameterPath.newInstance(MessageParameterPathElement.newInstance(elementName(el.getElementNameRule)) +:
      el.getParameterPathElementList.map(parameterPathElement))

  private def parameterPathElement(el: DecodeParameterPathElement): MessageParameterPathElement = {
    val rangeDecl = el.getDependentRangeDecl
    Option(el.getElementNameRule).map(e => MessageParameterPathElement.newInstance(elementName(e)))
      .getOrElse(MessageParameterPathElement.newInstance(ArrayRange(
        Option(rangeDecl.getRangeFromDecl).map(from => BigInt(from.getText)).getOrElse(0),
        if (rangeDecl.getRangeToDecl == null) null else BigInt(rangeDecl.getRangeToDecl.getText))))
  }

  private def eventMessage(em: DecodeEventMessage, c: Component): Seq[Referenceable] = {
    val alias = new Alias.ComponentEventMessage(elementName(em.getElementNameRule),
      elementInfo(Option(em.getElementInfo)), c, null)
    alias.obj(EventMessage(alias, id(em.getAnnotationDeclList), new util.ArrayList[Referenceable](), typeApplication(em.getTypeApplication)))
    alias.obj().objects.addAll(em.getEventMessageParametersDecl.getEventParameterDeclList.flatMap { p =>
      Option(p.getParameterElement)
        .map(mp => Seq(StatusParameter(parameterPath(mp), elementInfo(Option(p.getElementInfo)))))
        .getOrElse {
          val _var = p.getVarParameterElement
          val paramAlias = new Alias.MessageOrCommandParameter(elementName(_var.getElementNameRule),
            elementInfo(Option(_var.getElementInfo)), MessageOrCommand.newInstance(alias.obj()), null)
          paramAlias.obj(Parameter.newInstance(paramAlias, typeMeasure(_var.getTypeUnitApplication)))
          Seq(paramAlias, paramAlias.obj())
        }
    })
    Seq(alias, alias.obj)
  }

  private def command(c: DecodeCommandDecl, component: Component): Seq[Referenceable] = {
    val alias = new Alias.ComponentCommand(elementName(c.getElementNameRule),
      elementInfo(Option(c.getElementInfo)), component, null)
    alias.obj(Command.newInstance(alias, component, id(c.getAnnotationDeclList), new util.ArrayList[Referenceable](),
      typeMeasure(c.getTypeUnitApplication)))
    alias.obj().objects.addAll(Option(c.getCommandArgs).map(_.getCommandArgList.toSeq).getOrElse(Seq.empty)
      .flatMap { cmdArg =>
        val paramAlias = new Alias.MessageOrCommandParameter(elementName(cmdArg.getElementNameRule),
          elementInfo(Option(cmdArg.getElementInfo)), MessageOrCommand.newInstance(alias.obj()), null)
        paramAlias.obj(Parameter.newInstance(paramAlias, typeMeasure(cmdArg.getTypeUnitApplication)))
        Seq(paramAlias, paramAlias.obj())
      })
    Seq(alias, alias.obj)
  }

  private def componentRef(component: Component, fqn: Fqn): Alias.ComponentComponent = {
    val alias = fqn.mangledNameString()
    if (fqn.size == 1 && imports.contains(alias)) {
      val _import = imports.get(alias).get
      new Alias.ComponentComponent(fqn.last, util.Collections.emptyMap(), component, MaybeProxyComponent.newInstance(Proxy(_import.proxy.path)))
    } else {
      new Alias.ComponentComponent(fqn.last, util.Collections.emptyMap(), component, MaybeProxyComponent.newInstance(Proxy(ProxyPath(fqn, ns))))
    }
  }

  private def elementName(nameRule: DecodeElementNameRule): ElementName =
    elementName(Option(nameRule.getElementNameToken).map(_.getText)
      .getOrElse(nameRule.getEscapedName.getText.substring(1)))

  private def elementName(str: String): ElementName = ElementName.newInstanceFromSourceName(str)

  private def fqn(elementId: DecodeElementId): Fqn =
    Fqn.newInstance(elementId.getElementNameRuleList.map(elementName))

  private def elementInfo(infoStr: Option[DecodeElementInfo]): util.Map[Language, String] = infoStr match {
    case Some(i) => localizedStrings(i.getStringValueList)
    case _ => util.Collections.emptyMap()
  }

  private def localizedStrings(values: Seq[DecodeStringValue]): util.Map[Language, String] = values.map(info).toMap[Language, String]

  private def info(s: DecodeStringValue): (Language, String) = {
    Option(s.getElementNameRule) match {
      case Some(l) => (Language.newInstance(elementName(l).mangledNameString()),
        string(s.getStringLiteral))
      case _ => (defaultLanguage.getOrElse(sys.error("default language not set")),
        string(s.getStringLiteral))
    }
  }

  private def string(s: DecodeStringLiteral): String = {
    val t = s.getText
    t.substring(1, t.length - 1)
  }

  private def string(en: DecodeElementNameRule): String =
    elementName(en).mangledNameString()

  @Nullable
  private def measure(u: Option[PsiDecodeUnit]): MaybeProxyMeasure =
    u.map(unit => MaybeProxyMeasure.newInstance(proxyForFqn(fqn(unit.getElementId)).proxy)).orNull

  private def typeMeasure(tu: DecodeTypeUnitApplication): TypeMeasure = {
    val result = TypeMeasure.newInstance(typeApplication(tu.getTypeApplication), measure(Option(tu.getUnit)))
    if (tu.getOptional != null) {
      TypeMeasure.newInstance(MaybeProxyType.newInstance(Proxy(ProxyPath(GenericTypeName.newInstance(Fqn.OPTION.last(),
        Seq(result.typeProxy().proxy.path))))), null) // fixme: Not None
    } else {
      result
    }
  }

  private def proxyForFqn(fqn: Fqn): MaybeProxyReferenceable =
    if (fqn.size == 1 && imports.contains(fqn.last.mangledNameString()))
      imports.get(fqn.last.mangledNameString()).get
    else
      MaybeProxyReferenceable.newInstance(Proxy(ProxyPath(fqn, ns)))

  private def typeApplication(ta: DecodeTypeApplication): MaybeProxyType = {
    Option(ta.getElementId).map { elId =>
      val maybeProxy = MaybeProxyType.newInstance(proxyForFqn(fqn(elId)).proxy)
      Option(ta.getGenericArguments).map { params =>
        maybeProxy.proxy.path match {
          case e: ProxyPath.FqnElement =>
            // todo: remove asInstanceOf
            MaybeProxyType.newInstance(Proxy(ProxyPath(e.ns,
              GenericTypeName.newInstance(e.element.mangledName,
                params.getTypeUnitApplicationList.map(p => typeMeasure(p).typeProxy.proxy.path)))))
          case l: ProxyPath.Literal =>
            sys.error("literal can't be parametrized")
        }
      }.getOrElse(maybeProxy)
    }.getOrElse {
      MaybeProxyType.newInstance(Proxy(ProxyPath.fromLiteral(ta.getLiteral.getText)))
    }
  }
}
