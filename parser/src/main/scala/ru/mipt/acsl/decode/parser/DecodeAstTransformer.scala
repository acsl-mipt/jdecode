package ru.mipt.acsl.decode.parser

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import ru.mipt.acsl.decode.model.domain.{Language, LocalizedString, Referenceable}
import ru.mipt.acsl.decode.model.domain.component.message.{ArrayRange, EventMessage, MessageParameter, StatusMessage}
import ru.mipt.acsl.decode.model.domain.component.{Command, Component, ComponentRef}
import ru.mipt.acsl.decode.model.domain.impl.expr.{FloatLiteral, IntLiteral}
import ru.mipt.acsl.decode.model.domain.impl.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.registry.Language
import ru.mipt.acsl.decode.model.domain.impl.types._
import ru.mipt.acsl.decode.model.domain.component.message.{MessageParameterPath, MessageParameterPathElement, StatusMessage}
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn}
import ru.mipt.acsl.decode.model.domain.types.{EnumConstant, EnumType => _, _}
import ru.mipt.acsl.decode.parser.psi.{DecodeUnit => PsiDecodeUnit, _}

import scala.collection.{immutable, mutable}
import scala.reflect.ClassTag
import ru.mipt.acsl.decode.model.domain.impl.naming._
import ru.mipt.acsl.decode.model.domain.proxy.path.{ArrayTypePath, GenericTypeName, ProxyPath, TypeName}
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit

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
      case p: DecodeTypeDecl =>
        ns.types = ns.types :+ newType(p)
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
      case u: DecodeUnitDecl =>
        ns.units = ns.units :+ DecodeUnit(elementName(u.getElementNameRule), ns,
          localizedStrings(u.getStringValueList), elementInfo(Option(u.getElementInfo)))
      case a: DecodeAliasDecl =>
        ns.types = ns.types :+ AliasType(elementName(a.getElementNameRule), ns,
          typeApplication(Some(a.getTypeApplication)).get, elementInfo(Option(a.getElementInfo)))
      case c: DecodeComponentDecl =>
        val params = Option(c.getComponentParametersDecl).map { params =>
          val struct = StructType(makeNewSystemName(elementName(c.getElementNameRule)), ns,
            elementInfo(Option(params.getElementInfo)),
            params.getCommandArgs.getCommandArgList.map { arg =>
              StructField(elementName(arg.getElementNameRule), typeUnit(arg.getTypeUnitApplication),
                elementInfo(Option(arg.getElementInfo)))
            }.to[immutable.Seq])
          ns.types = ns.types :+ struct
          MaybeProxy(struct)
        }
        val component: Component = Component(elementName(c.getElementNameRule), ns,
          id(Option(c.getEntityId)), params, elementInfo(Option(c.getElementInfo)),
          c.getComponentRefList.map(sc => componentRef(Fqn(Seq(elementName(sc.getElementNameRule))))).to[immutable.Seq],
          c.getCommandDeclList.map(c => command(c)).to[immutable.Seq])
        ns.components = ns.components :+ component
        component.eventMessages ++= c.getMessageDeclList
          .flatMap(m => Option(m.getEventMessage).map(eventMessage(_, component)))
        component.statusMessages ++= c.getMessageDeclList
          .flatMap(m => Option(m.getStatusMessage).map(statusMessage(_, component)))
      case l: DecodeDefaultLanguageDecl =>
        defaultLanguage = Some(Language(elementName(l.getElementNameRule).asMangledString))
      case s: DecodeScriptDecl =>
        //sys.error("not implemented")
      case p: PsiWhiteSpace =>
      case p =>
        sys.error(s"not implemented for ${p.getClass}")
    })
    ns.rootNamespace
  }

  private def newType(p: DecodeTypeDecl): DecodeType = {
    val body = p.getTypeDeclBody
    val name = elementName(p.getElementNameRule)
    val _info = elementInfo(Option(p.getElementInfo))
    Option(body.getEnumTypeDecl).map(e => newEnumType(e, name, _info)).orElse {
      Option(body.getNativeTypeDecl).map(n =>
        Option(p.getGenericArgs).map(args => GenericType(name, ns, _info,
          args.getGenericArgList.map(arg => Option(arg.getElementNameRule).map(elementName))))
          .getOrElse(NativeType(name, ns, _info)))
    }.orElse {
      Option(body.getTypeApplication).map(s => SubType(name, ns, _info, typeApplication(Some(s)).get))
    }.getOrElse(newStructType(name, _info, body))
  }

  private def newEnumType(e: DecodeEnumTypeDecl, name: ElementName, _info: LocalizedString): EnumType =
    EnumType(name, ns, Option(e.getElementNameRule)
      .map(n => Left(proxyForFqn[EnumType](Fqn(Seq(elementName(n))))))
      .getOrElse(Right(typeApplication(Option(e.getTypeApplication)).get)), _info,
      e.getEnumTypeValues.getEnumTypeValueList.map(v => newEnumConstant(v)).to[immutable.Set], e.getFinalEnum != null)

  private def newStructType(name: ElementName, _info: LocalizedString, body: DecodeTypeDeclBody): StructType =
    StructType(name, ns, _info, body.getStructTypeDecl.getCommandArgs.getCommandArgList.map(cmdArg =>
      StructField(elementName(cmdArg.getElementNameRule), typeUnit(cmdArg.getTypeUnitApplication),
        elementInfo(Option(cmdArg.getElementInfo)))).to[immutable.Seq])

  private def newEnumConstant(v: DecodeEnumTypeValue): EnumConstant = {
    val literal = v.getLiteral
    val numericLiteral = literal.getNumericLiteral
    EnumConstant(elementName(v.getElementNameRule),
      Option(numericLiteral.getFloatLiteral).map(l => FloatLiteral(l.getText.toFloat))
        .getOrElse(IntLiteral(numericLiteral.getNonNegativeIntegerLiteral.getText.toInt)),
      elementInfo(Option(v.getElementInfo)))
  }

  private def id(entityId: Option[DecodeEntityId]): Option[Int] =
    entityId.map(_.getNonNegativeNumber.getText.toInt)

  private def statusMessage(sm: DecodeStatusMessage, c: Component): StatusMessage =
    StatusMessage(c, elementName(sm.getElementNameRule), id(Option(sm.getEntityId)),
      elementInfo(Option(sm.getElementInfo)),
      sm.getStatusMessageParametersDecl.getParameterDeclList.map(p =>
        MessageParameter(parameterPath(p.getParameterElement), elementInfo(Option(p.getElementInfo)))))

  private def parameterPath(el: DecodeParameterElement): MessageParameterPath =
    Left(elementName(el.getElementNameRule)) +:
      el.getParameterPathElementList.map(parameterPathElement).to[immutable.Seq]

  private def parameterPathElement(el: DecodeParameterPathElement): MessageParameterPathElement = {
    val rangeDecl = el.getRangeDecl
    Option(el.getElementNameRule).map(e => Left(elementName(e)))
      .getOrElse(Right(ArrayRange(
        Option(rangeDecl.getNonNegativeIntegerLiteral).map(_.getText.toLong).getOrElse(0),
        Option(rangeDecl.getRangeUpperBoundDecl).map(_.getText.toLong))))
  }

  private def eventMessage(em: DecodeEventMessage, c: Component): EventMessage =
    EventMessage(c, elementName(em.getElementNameRule), id(Option(em.getEntityId)),
      elementInfo(Option(em.getElementInfo)),
      em.getEventMessageParametersDecl.getEventParameterDeclList.map { p =>
        Option(p.getParameterElement)
          .map(mp => Left(MessageParameter(parameterPath(mp), elementInfo(Option(p.getElementInfo)))))
          .getOrElse {
            val _var = p.getVarParameterElement
            Right(Parameter(elementName(_var.getElementNameRule), elementInfo(Option(p.getElementInfo)),
              typeApplication(Some(_var.getTypeUnitApplication.getTypeApplication)).get,
              unit(Option(_var.getTypeUnitApplication.getUnit))))
          }
      }, typeApplication(Some(em.getTypeApplication)).get)

  private def command(c: DecodeCommandDecl): Command =
    Command(elementName(c.getElementNameRule), id(Option(c.getEntityId)),
      elementInfo(Option(c.getElementInfo)), Option(c.getCommandArgs).map(_.getCommandArgList.toSeq).getOrElse(Seq.empty)
        .map { cmdArg =>
          Parameter(elementName(cmdArg.getElementNameRule), elementInfo(Option(cmdArg.getElementInfo)),
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
          val min = fromTo._1.map(_.toLong).getOrElse(0l)
          val max = fromTo._2.map(_.toLong).getOrElse(min)
          MaybeProxy[DecodeType](ProxyPath(path.ns, ArrayTypePath(path,
            ArraySize(min, max))))
        }.getOrElse {
          val nta = ta.getSimpleOrGenericTypeApplication
          val proxy = proxyForFqn[DecodeType](fqn(nta.getElementId))
          Option(nta.getGenericParameters).map { params =>
            val path = proxy.proxy.path
            // todo: remove asInstanceOf
            MaybeProxy[DecodeType](ProxyPath(path.ns,
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
