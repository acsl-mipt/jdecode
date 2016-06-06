package ru.mipt.acsl.decode.parser

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.component.message._
import ru.mipt.acsl.decode.model.component.{Command, Component}
import ru.mipt.acsl.decode.model.expr.{BigDecimalLiteral, BigIntLiteral}
import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.proxy.path.{GenericTypeName, ProxyPath, TypeName}
import ru.mipt.acsl.decode.model.proxy.{MaybeProxy, Proxy}
import ru.mipt.acsl.decode.model.registry.{Language, Measure}
import ru.mipt.acsl.decode.model.types.{Alias, EnumConstant, EnumType, _}
import ru.mipt.acsl.decode.parser.psi.{DecodeUnit => PsiDecodeUnit, _}

import scala.collection.mutable

/**
  * @author Artem Shein
  */
class DecodeAstTransformer {

  import scala.collection.JavaConversions._

  private val imports = mutable.HashMap.empty[String, MaybeProxy.Referenceable]
  private var ns: Namespace = _
  private var defaultLanguage: Option[Language] = None

  def processFile(node: ASTNode): Namespace = {
    var children = node.getChildren(null)
    assert(children.nonEmpty)
    while (children.head.getPsi.isInstanceOf[PsiWhiteSpace])
      children = children.tail
    val nsDecl = children.head.getPsi(classOf[DecodeNamespaceDecl])
    val rootNs = Namespace.newRoot
    ns = Namespace(fqn(nsDecl.getElementId), rootNs, elementInfo(Option(nsDecl.getElementInfo)))
    children.drop(1).foreach(_.getPsi match {
      case s: DecodeSubTypeDecl =>
        ns.objects ++= newType(s)
      case e: DecodeEnumTypeDecl =>
        ns.objects ++= newType(e)
      case s: DecodeStructTypeDecl =>
        ns.objects ++= newType(s)
      case n: DecodeNativeTypeDecl =>
        ns.objects ++= newType(n)
      case a: DecodeAliasDecl =>
        ns.objects :+= newType(a)
      case i: DecodeImportStmt =>
        val els = i.getImportElementList
        val iFqn: Fqn = fqn(i.getElementId)
        if (els.isEmpty) {
          imports.put(iFqn.last.asMangledString, MaybeProxy.Referenceable(Left(Proxy(ProxyPath(iFqn.copyDropLast, TypeName(iFqn.last))))))
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
              MaybeProxy.Referenceable(Left(Proxy(ProxyPath(iFqn, TypeName(p.originalName)))))).isEmpty))
        }
      case m: DecodeMeasureDecl =>
        val alias = Alias.NsMeasure(elementName(m.getElementNameRule), elementInfo(Option(m.getElementInfo)))(ns, null)
        alias.obj = Measure(alias, localizedStrings(m.getStringValueList))
        ns.objects ++= Seq(alias, alias.obj)
      case c: DecodeComponentDecl =>
        val params = Option(c.getComponentParametersDecl).map { params =>
          val struct = StructType(None, ns, Seq.empty, Seq.empty)
          struct.objects ++= params.getCommandArgs.getCommandArgList.flatMap { arg =>
            val alias = Alias.StructField(elementName(arg.getElementNameRule),
              elementInfo(Option(arg.getElementInfo)))(struct, null)
            alias.obj = StructField(alias, typeMeasure(arg.getTypeUnitApplication))
            Seq(alias, alias.obj)
          }
          ns.objects :+= struct
          MaybeProxy.Struct(Right(struct))
        }
        val alias = Alias.NsComponent(elementName(c.getElementNameRule), elementInfo(Option(c.getElementInfo)))(ns, null)
        val component = Component(alias, ns, id(c.getAnnotationDeclList), params, Seq.empty)
        alias.obj = component
        alias.obj.objects ++= c.getComponentRefList.map(sc =>
          componentRef(component, Fqn(Seq(elementName(sc.getElementNameRule))))) ++
          c.getCommandDeclList.flatMap(c => command(c, component))
        ns.objects ++= Seq(alias, alias.obj)
        component.objects ++= c.getMessageDeclList
          .flatMap(m => Option(m.getEventMessage).seq.flatMap(eventMessage(_, component))) ++
          c.getMessageDeclList
            .flatMap(m => Option(m.getStatusMessage).seq.flatMap(statusMessage(_, component)))
      case l: DecodeLanguageDecl =>
        defaultLanguage = Some(Language(elementName(l.getElementNameRule).asMangledString))
      case c: DecodeConstDecl =>
        val alias = Alias.NsConst(elementName(c.getElementNameRule), elementInfo(Option(c.getElementInfo)))(ns, null)
        alias.obj = Const(Some(alias), ns, c.getLiteral.getText)
        ns.objects ++= Seq(alias, alias.obj)
      case s: DecodeScriptDecl =>
        //sys.error("not implemented")
      case p: PsiWhiteSpace =>
      case p =>
        sys.error(s"not implemented for ${p.getText} (${p.getClass} $p) at ${p.getTextOffset}")
    })
    ns.rootNamespace
  }

  private def newType(a: DecodeAliasDecl): Alias.NsTypeMeasure =
    Alias.NsTypeMeasure(elementName(a.getElementNameRule), elementInfo(Option(a.getElementInfo)))(ns, typeMeasure(a.getTypeUnitApplication))

  private def newType(e: DecodeEnumTypeDecl): Seq[Referenceable] = {
    val alias = Alias.NsType(elementName(e.getEnumName.getElementNameRule), elementInfo(Option(e.getElementInfo)))(ns, null)
    alias.obj = newEnumType(e, alias, typeParameters(Option(e.getGenericParameters)))
    Seq(alias, alias.obj)
  }

  private def newType(n: DecodeNativeTypeDecl): Seq[Referenceable] = {
    val alias = Alias.NsType(elementName(n.getElementNameRule), elementInfo(Option(n.getElementInfo)))(ns, null)
    alias.obj = NativeType(alias, ns, typeParameters(Option(n.getGenericParameters)))
    Seq(alias, alias.obj)
  }

  private def newType(s: DecodeStructTypeDecl): Seq[Referenceable] = {
    val alias = Alias.NsType(elementName(s.getElementNameRule), elementInfo(Option(s.getElementInfo)))(ns, null)
    alias.obj = newStructType(alias, s.getCommandArgs, typeParameters(Option(s.getGenericParameters)))
    Seq(alias, alias.obj)
  }

  private def newType(s: DecodeSubTypeDecl): Seq[Referenceable] = {
    val alias = Alias.NsType(elementName(s.getElementNameRule), elementInfo(Option(s.getElementInfo)))(ns, null)
    val t = SubType(Some(alias), ns, typeMeasure(s.getTypeUnitApplication),
      typeParameters(Option(s.getGenericParameters)))
    alias.obj = t
    Seq(alias, t)
  }

  private def typeParameters(ga: Option[DecodeGenericParameters]): Seq[ElementName] =
    ga.toSeq.flatMap(_.getGenericParameterList.map(arg => elementName(arg.getElementNameRule)))

  private def newEnumType(e: DecodeEnumTypeDecl, alias: Alias.NsType,
                          typeParameters: Seq[ElementName]): EnumType = {
    val enum = EnumType(Some(alias), ns, Option(e.getElementNameRule)
      .map(n => Left(MaybeProxy.Enum(Left(proxyForFqn(Fqn(Seq(elementName(n)))).proxy))))
      .getOrElse(Right(typeMeasure(e.getTypeUnitApplication))),
        Seq.empty, e.getFinalEnum != null,
      typeParameters)
    alias.obj = enum
    enum.objects ++= e.getEnumTypeValues.getEnumTypeValueList.flatMap(v => newEnumConstant(v, enum))
    enum
  }

  private def newStructType(alias: Alias.NsType, args: DecodeCommandArgs, typeParameters: Seq[ElementName]): StructType = {
    val t = StructType(Some(alias), ns, Seq.empty, typeParameters)
    t.objects = args.getCommandArgList.flatMap { cmdArg =>
      val alias = Alias.StructField(elementName(cmdArg.getElementNameRule),
        elementInfo(Option(cmdArg.getElementInfo)))(t, null)
      alias.obj = StructField(alias, typeMeasure(cmdArg.getTypeUnitApplication))
      Seq(alias, alias.obj)
    }
    t
  }

  private def newEnumConstant(v: DecodeEnumTypeValue, enum: EnumType): Seq[Referenceable] = {
    val literal = v.getLiteral
    val numericLiteral = literal.getNumericLiteral
    val alias = Alias.EnumConstant(elementName(v.getElementNameRule), elementInfo(Option(v.getElementInfo)))(enum, null)
    alias.obj = EnumConstant(alias, Option(numericLiteral.getFloatLiteral).map(l => BigDecimalLiteral(l.getText))
        .getOrElse(BigIntLiteral(numericLiteral.getIntegerLiteral.getText)))
    Seq(alias, alias.obj)
  }

  private def id(annotations: Seq[DecodeAnnotationDecl]): Option[Int] =
    annotations.find(a => string(a.getElementNameRule) == "id")
      .map(_.getAnnotationParameterList.headOption.map(_.getText.toInt))
      .getOrElse(None)

  private def statusMessage(sm: DecodeStatusMessage, c: Component): Seq[Referenceable] = {
    val alias = Alias.ComponentStatusMessage(elementName(sm.getElementNameRule),
      elementInfo(Option(sm.getElementInfo)))(c, null)
    alias.obj = StatusMessage(alias, c, id(sm.getAnnotationDeclList),
      sm.getStatusMessageParametersDecl.getParameterDeclList.map(p =>
        StatusParameter(parameterPath(p.getParameterElement), elementInfo(Option(p.getElementInfo)))))
    Seq(alias, alias.obj)
  }

  private def parameterPath(el: DecodeParameterElement): MessageParameterPath =
    Left(elementName(el.getElementNameRule)) +:
      el.getParameterPathElementList.map(parameterPathElement)

  private def parameterPathElement(el: DecodeParameterPathElement): MessageParameterPathElement = {
    val rangeDecl = el.getDependentRangeDecl
    Option(el.getElementNameRule).map(e => Left(elementName(e)))
      .getOrElse(Right(ArrayRange(
        Option(rangeDecl.getRangeFromDecl).map(from => BigInt(from.getText)).getOrElse(0),
        Option(rangeDecl.getRangeToDecl).map(to => BigInt(to.getText)))))
  }

  private def eventMessage(em: DecodeEventMessage, c: Component): Seq[Referenceable] = {
    val alias = Alias.ComponentEventMessage(elementName(em.getElementNameRule),
      elementInfo(Option(em.getElementInfo)))(c, null)
    alias.obj = EventMessage(alias, id(em.getAnnotationDeclList), Seq.empty, typeApplication(em.getTypeApplication))
    alias.obj.objects ++= em.getEventMessageParametersDecl.getEventParameterDeclList.flatMap { p =>
      Option(p.getParameterElement)
        .map(mp => Seq(StatusParameter(parameterPath(mp), elementInfo(Option(p.getElementInfo)))))
        .getOrElse {
          val _var = p.getVarParameterElement
          val paramAlias = Alias.MessageOrCommandParameter(elementName(_var.getElementNameRule),
            elementInfo(Option(_var.getElementInfo)))(CommandOrTmMessage(Right(alias.obj)), null)
          paramAlias.obj = Parameter(paramAlias, typeMeasure(_var.getTypeUnitApplication))
          Seq(paramAlias, paramAlias.obj)
        }
    }
    Seq(alias, alias.obj)
  }

  private def command(c: DecodeCommandDecl, component: Component): Seq[Referenceable] = {
    val alias = Alias.ComponentCommand(elementName(c.getElementNameRule),
      elementInfo(Option(c.getElementInfo)))(component, null)
    alias.obj = Command(alias, id(c.getAnnotationDeclList), Seq.empty, typeMeasure(c.getTypeUnitApplication))
    alias.obj.objects ++= Option(c.getCommandArgs).map(_.getCommandArgList.toSeq).getOrElse(Seq.empty)
      .flatMap { cmdArg =>
        val paramAlias = Alias.MessageOrCommandParameter(elementName(cmdArg.getElementNameRule),
          elementInfo(Option(cmdArg.getElementInfo)))(CommandOrTmMessage(Left(alias.obj)), null)
        paramAlias.obj = Parameter(paramAlias, typeMeasure(cmdArg.getTypeUnitApplication))
        Seq(paramAlias, paramAlias.obj)
      }
    Seq(alias, alias.obj)
  }

  private def componentRef(component: Component, fqn: Fqn): Alias.ComponentComponent = {
    val alias = fqn.asMangledString
    if (fqn.size == 1 && imports.contains(alias)) {
      val _import = imports.get(alias).get
      Alias.ComponentComponent(fqn.last, LocalizedString.empty)(component, MaybeProxy.Component(Left(Proxy(_import.proxy.path))))
    } else {
      Alias.ComponentComponent(fqn.last, LocalizedString.empty)(component, MaybeProxy.Component(Left(Proxy(ProxyPath(fqn, ns)))))
    }
  }

  private def elementName(nameRule: DecodeElementNameRule): ElementName =
    elementName(Option(nameRule.getElementNameToken).map(_.getText)
      .getOrElse(nameRule.getEscapedName.getText.substring(1)))

  private def elementName(str: String): ElementName = ElementName.newFromSourceName(str)

  private def fqn(elementId: DecodeElementId): Fqn =
    Fqn(elementId.getElementNameRuleList.map(elementName))

  private def elementInfo(infoStr: Option[DecodeElementInfo]): LocalizedString = infoStr match {
    case Some(i) => localizedStrings(i.getStringValueList)
    case _ => LocalizedString.empty
  }

  private def localizedStrings(values: Seq[DecodeStringValue]): LocalizedString = values.map(info).toMap

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

  private def string(en: DecodeElementNameRule): String =
    elementName(en).asMangledString

  private def measure(u: Option[PsiDecodeUnit]): Option[MaybeProxy.Measure] =
    u.map(unit => MaybeProxy.Measure(Left(proxyForFqn(fqn(unit.getElementId)).proxy)))

  private def typeMeasure(tu: DecodeTypeUnitApplication): TypeMeasure = {
    val result = TypeMeasure(typeApplication(tu.getTypeApplication), measure(Option(tu.getUnit)))
    if (tu.getOptional != null) {
      TypeMeasure(MaybeProxy.Type(Left(Proxy(ProxyPath(GenericTypeName(ElementName.newFromMangledName("option"),
        Seq(result.typeProxy.proxy.path)))))), None) // fixme: Not None
    } else {
      result
    }
  }

  private def proxyForFqn(fqn: Fqn): MaybeProxy.Referenceable =
    if (fqn.size == 1 && imports.contains(fqn.last.asMangledString))
      imports.get(fqn.last.asMangledString).get
    else
      MaybeProxy.Referenceable(Left(Proxy(ProxyPath(fqn, ns))))

  private def typeApplication(ta: DecodeTypeApplication): MaybeProxy.TypeProxy = {
    Option(ta.getElementId).map { elId =>
      val maybeProxy = MaybeProxy.Type(Left(proxyForFqn(fqn(elId)).proxy))
      Option(ta.getGenericArguments).map { params =>
        maybeProxy.proxy.path match {
          case e: ProxyPath.FqnElement =>
            // todo: remove asInstanceOf
            MaybeProxy.Type(Left(Proxy(ProxyPath(e.ns,
              GenericTypeName(e.element.mangledName,
                params.getTypeUnitApplicationList.map(p => typeMeasure(p).typeProxy.proxy.path))))))
          case l: ProxyPath.Literal =>
            sys.error("literal can't be parametrized")
        }
      }.getOrElse(maybeProxy)
    }.getOrElse {
      MaybeProxy.Type(Left(Proxy(ProxyPath.fromLiteral(ta.getLiteral.getText))))
    }
  }
}
