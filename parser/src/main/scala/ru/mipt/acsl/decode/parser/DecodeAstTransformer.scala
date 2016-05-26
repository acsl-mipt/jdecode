package ru.mipt.acsl.decode.parser

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import ru.mipt.acsl.decode.model.component.message._
import ru.mipt.acsl.decode.model.component.{Command, Component, ComponentRef}
import ru.mipt.acsl.decode.model.expr.{BigDecimalLiteral, BigDecimalLiteral$, BigIntLiteral, BigIntLiteral$}
import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn, Namespace, _}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.proxy.path.{GenericTypeName, ProxyPath, TypeName}
import ru.mipt.acsl.decode.model.registry.{Language, Measure}
import ru.mipt.acsl.decode.model.types.{EnumConstant, EnumType, _}
import ru.mipt.acsl.decode.model.{LocalizedString, Referenceable}
import ru.mipt.acsl.decode.parser.psi.{DecodeUnit => PsiDecodeUnit, _}

import scala.collection.{immutable, mutable}
import scala.reflect.ClassTag

/**
  * @author Artem Shein
  */
class DecodeAstTransformer {

  import scala.collection.JavaConversions._

  private val imports = mutable.HashMap.empty[String, MaybeProxy[Referenceable]]
  private var ns: Namespace = _
  private var defaultLanguage: Option[Language] = None

  def processFile(node: ASTNode): Namespace = {
    var children = node.getChildren(null)
    assert(children.nonEmpty)
    while (children.head.getPsi.isInstanceOf[PsiWhiteSpace])
      children = children.tail
    val nsDecl = children.head.getPsi(classOf[DecodeNamespaceDecl])
    ns = fqn(nsDecl.getElementId).newNamespaceForFqn(elementInfo(Option(nsDecl.getElementInfo)))
    children.drop(1).foreach(_.getPsi match {
      case s: DecodeSubTypeDecl =>
        ns.types = ns.types :+ newType(s)
      case e: DecodeEnumTypeDecl =>
        ns.types = ns.types :+ newType(e)
      case s: DecodeStructTypeDecl =>
        ns.types = ns.types :+ newType(s)
      case n: DecodeNativeTypeDecl =>
        ns.types = ns.types :+ newType(n)
      case a: DecodeAliasDecl =>
        ns.types = ns.types :+ newType(a)
      case i: DecodeImportStmt =>
        val els = i.getImportElementList
        val iFqn: Fqn = fqn(i.getElementId)
        if (els.isEmpty) {
          imports.put(iFqn.last.asMangledString, MaybeProxy(iFqn.copyDropLast, TypeName(iFqn.last)))
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
              MaybeProxy(iFqn, TypeName(p.originalName))).isEmpty))
        }
      case m: DecodeMeasureDecl =>
        ns.measures = ns.measures :+ Measure(elementName(m.getElementNameRule), ns,
          localizedStrings(m.getStringValueList), elementInfo(Option(m.getElementInfo)))
      case c: DecodeComponentDecl =>
        val params = Option(c.getComponentParametersDecl).map { params =>
          val struct = StructType(makeNewSystemName(elementName(c.getElementNameRule)), ns,
            elementInfo(Option(params.getElementInfo)),
            params.getCommandArgs.getCommandArgList.map { arg =>
              StructField(elementName(arg.getElementNameRule), typeUnit(arg.getTypeUnitApplication),
                elementInfo(Option(arg.getElementInfo)))
            }.to[immutable.Seq], Seq.empty)
          ns.types = ns.types :+ struct
          MaybeProxy(struct)
        }
        val component: Component = Component(elementName(c.getElementNameRule), ns,
          id(c.getAnnotationDeclList), params, elementInfo(Option(c.getElementInfo)),
          c.getComponentRefList.map(sc => componentRef(Fqn(Seq(elementName(sc.getElementNameRule))))).to[immutable.Seq],
          c.getCommandDeclList.map(c => command(c)).to[immutable.Seq])
        ns.components = ns.components :+ component
        component.eventMessages ++= c.getMessageDeclList
          .flatMap(m => Option(m.getEventMessage).map(eventMessage(_, component)))
        component.statusMessages ++= c.getMessageDeclList
          .flatMap(m => Option(m.getStatusMessage).map(statusMessage(_, component)))
      case l: DecodeLanguageDecl =>
        defaultLanguage = Some(Language(elementName(l.getElementNameRule).asMangledString))
      case s: DecodeScriptDecl =>
        //sys.error("not implemented")
      case p: PsiWhiteSpace =>
      case p =>
        sys.error(s"not implemented for ${p.getText} (${p.getClass} $p) at ${p.getTextOffset}")
    })
    ns.rootNamespace
  }

  private def newType(a: DecodeAliasDecl): AliasType =
    AliasType(elementName(a.getElementNameRule), ns,
      typeUnit(a.getTypeUnitApplication), elementInfo(Option(a.getElementInfo)))

  private def newType(e: DecodeEnumTypeDecl): EnumType =
    newEnumType(e, elementName(e.getEnumName.getElementNameRule), elementInfo(Option(e.getElementInfo)),
      typeParameters(Option(e.getGenericParameters)))

  private def newType(n: DecodeNativeTypeDecl): NativeType =
      NativeType(elementName(n.getElementNameRule), ns, elementInfo(Option(n.getElementInfo)),
        typeParameters(Option(n.getGenericParameters)))

  private def newType(s: DecodeStructTypeDecl): StructType =
    newStructType(elementName(s.getElementNameRule), elementInfo(Option(s.getElementInfo)), s.getCommandArgs,
      typeParameters(Option(s.getGenericParameters)))

  private def newType(s: DecodeSubTypeDecl): SubType =
    SubType(elementName(s.getElementNameRule), ns, elementInfo(Option(s.getElementInfo)),
      typeUnit(s.getTypeUnitApplication), typeParameters(Option(s.getGenericParameters)))

  private def typeParameters(ga: Option[DecodeGenericParameters]): Seq[ElementName] =
    ga.toSeq.flatMap(_.getGenericParameterList.map(arg => elementName(arg.getElementNameRule)))

  private def newEnumType(e: DecodeEnumTypeDecl, name: ElementName, _info: LocalizedString,
                          typeParameters: Seq[ElementName]): EnumType =
    EnumType(name, ns, Option(e.getElementNameRule)
      .map(n => Left(proxyForFqn[EnumType](Fqn(Seq(elementName(n))))))
      .getOrElse(Right(typeUnit(e.getTypeUnitApplication))), _info,
      e.getEnumTypeValues.getEnumTypeValueList.map(v => newEnumConstant(v)).to[immutable.Set], e.getFinalEnum != null,
      typeParameters)

  private def newStructType(name: ElementName, _info: LocalizedString, args: DecodeCommandArgs,
                            typeParameters: Seq[ElementName]): StructType =
    StructType(name, ns, _info, args.getCommandArgList.map(cmdArg =>
      StructField(elementName(cmdArg.getElementNameRule), typeUnit(cmdArg.getTypeUnitApplication),
        elementInfo(Option(cmdArg.getElementInfo)))).to[immutable.Seq], typeParameters)

  private def newEnumConstant(v: DecodeEnumTypeValue): EnumConstant = {
    val literal = v.getLiteral
    val numericLiteral = literal.getNumericLiteral
    EnumConstant(elementName(v.getElementNameRule),
      Option(numericLiteral.getFloatLiteral).map(l => BigDecimalLiteral(l.getText))
        .getOrElse(BigIntLiteral(numericLiteral.getIntegerLiteral.getText)),
      elementInfo(Option(v.getElementInfo)))
  }

  private def id(annotations: Seq[DecodeAnnotationDecl]): Option[Int] =
    annotations.find(a => string(a.getElementNameRule) == "id")
      .map(_.getAnnotationParameterList.headOption.map(_.getText.toInt))
      .getOrElse(None)

  private def statusMessage(sm: DecodeStatusMessage, c: Component): StatusMessage =
    StatusMessage(c, elementName(sm.getElementNameRule), id(sm.getAnnotationDeclList),
      elementInfo(Option(sm.getElementInfo)),
      sm.getStatusMessageParametersDecl.getParameterDeclList.map(p =>
        MessageParameter(parameterPath(p.getParameterElement), elementInfo(Option(p.getElementInfo)))))

  private def parameterPath(el: DecodeParameterElement): MessageParameterPath =
    Left(elementName(el.getElementNameRule)) +:
      el.getParameterPathElementList.map(parameterPathElement).to[immutable.Seq]

  private def parameterPathElement(el: DecodeParameterPathElement): MessageParameterPathElement = {
    val rangeDecl = el.getDependentRangeDecl
    Option(el.getElementNameRule).map(e => Left(elementName(e)))
      .getOrElse(Right(ArrayRange(
        Option(BigInt(rangeDecl.getRangeFromDecl.getText)).getOrElse(0),
        Option(BigInt(rangeDecl.getRangeToDecl.getText)))))
  }

  private def eventMessage(em: DecodeEventMessage, c: Component): EventMessage =
    EventMessage(c, elementName(em.getElementNameRule), id(em.getAnnotationDeclList),
      elementInfo(Option(em.getElementInfo)),
      em.getEventMessageParametersDecl.getEventParameterDeclList.map { p =>
        Option(p.getParameterElement)
          .map(mp => Left(MessageParameter(parameterPath(mp), elementInfo(Option(p.getElementInfo)))))
          .getOrElse {
            val _var = p.getVarParameterElement
            Right(Parameter(elementName(_var.getElementNameRule), elementInfo(Option(p.getElementInfo)),
              typeUnit(_var.getTypeUnitApplication)))
          }
      }, typeApplication(em.getTypeApplication))

  private def command(c: DecodeCommandDecl): Command =
    Command(elementName(c.getElementNameRule), id(c.getAnnotationDeclList),
      elementInfo(Option(c.getElementInfo)), Option(c.getCommandArgs).map(_.getCommandArgList.toSeq).getOrElse(Seq.empty)
        .map { cmdArg =>
          Parameter(elementName(cmdArg.getElementNameRule), elementInfo(Option(cmdArg.getElementInfo)),
            typeUnit(cmdArg.getTypeUnitApplication))
        }.to[immutable.Seq], typeUnit(c.getTypeUnitApplication))

  private def componentRef(fqn: Fqn): ComponentRef = {
    val alias = fqn.asMangledString
    if (fqn.size == 1 && imports.contains(alias)) {
      val _import = imports.get(alias).get
      ComponentRef(_import.asInstanceOf[MaybeProxy[Component]],
        if (_import.proxy.path.asInstanceOf[ProxyPath.FqnElement].element.mangledName.asMangledString.equals(alias))
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

  private def measure(u: Option[PsiDecodeUnit]): Option[MaybeProxy[Measure]] =
    u.map(unit => proxyForFqn(fqn(unit.getElementId)))

  private def typeUnit(tu: DecodeTypeUnitApplication): TypeMeasure = {
    val result = TypeMeasure(typeApplication(tu.getTypeApplication), measure(Option(tu.getUnit)))
    if (tu.getOptional != null) {
      TypeMeasure(MaybeProxy.proxyForSystem(GenericTypeName(ElementName.newFromMangledName("option"),
        immutable.Seq(result.typeProxy.proxy.path))), None) // fixme: Not None
    } else {
      result
    }
  }

  private def proxyForFqn[T <: Referenceable : ClassTag](fqn: Fqn): MaybeProxy[T] =
    if (fqn.size == 1 && imports.contains(fqn.last.asMangledString))
    // todo: refactor out asInstanceOf
      imports.get(fqn.last.asMangledString).get.asInstanceOf[MaybeProxy[T]]
    else
      MaybeProxy.proxyDefaultNamespace[T](fqn, ns)

  private def typeApplication(ta: DecodeTypeApplication): MaybeProxy[DecodeType] = {
    val proxy = proxyForFqn[DecodeType](fqn(ta.getElementId))
    Option(ta.getGenericArguments).map { params =>
      proxy.proxy.path match {
        case e: ProxyPath.FqnElement =>
          // todo: remove asInstanceOf
          MaybeProxy[DecodeType](ProxyPath(e.ns,
            GenericTypeName(e.element.asInstanceOf[TypeName].typeName,
              params.getTypeUnitApplicationList.map(p => typeUnit(p).typeProxy.proxy.path)
                .to[immutable.Seq])))
        case l: ProxyPath.Literal =>
          sys.error("literal can't be parametrized")
      }
    }.getOrElse(proxy)
  }
}
