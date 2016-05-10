package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain.ValidatingResult
import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.impl.component.message.EventMessage
import ru.mipt.acsl.decode.model.domain.impl.proxy.{ResolvingResult, Result}
import ru.mipt.acsl.decode.model.domain.impl.types.{DecodeType, EnumType, GenericTypeSpecialized, HasBaseType, StructType}
import ru.mipt.acsl.decode.model.domain.pure.component.message.StatusMessage
import ru.mipt.acsl.decode.model.domain.pure.naming.Fqn
import ru.mipt.acsl.decode.model.domain.pure.types.NativeType

import scala.collection.mutable

package object registry {

  import naming._

  implicit class RegistryHelper(val r: Registry) {

    def allComponents: Seq[Component] = r.rootNamespaces.flatMap(_.allComponents)

    def allNamespaces: Seq[Namespace] = r.rootNamespaces.flatMap(_.allNamespaces)

    def component(fqn: String): Option[Component] = {
      val dotPos = fqn.lastIndexOf('.')
      val namespaceOptional = namespace(fqn.substring(0, dotPos))
      if (namespaceOptional.isEmpty)
      {
        return None
      }
      val componentName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
      namespaceOptional.get.components.find(_.name == componentName)
    }

    def namespace(fqn: String): Option[Namespace] = {
      var currentNamespaces: Option[Seq[Namespace]] = Some(r.rootNamespaces)
      var currentNamespace: Option[Namespace] = None
      "\\.".r.split(fqn).foreach(nsName => {
        if (currentNamespaces.isEmpty)
        {
          return None
        }
        val decodeName = ElementName.newFromMangledName(nsName)
        currentNamespace = currentNamespaces.get.find(_.name == decodeName)
        if (currentNamespace.isDefined)
        {
          currentNamespaces = Some(currentNamespace.get.subNamespaces)
        }
        else
        {
          currentNamespaces = None
        }
      })
      currentNamespace
    }

    def eventMessage(fqn: String): Option[EventMessage] = {
      val dotPos = fqn.lastIndexOf('.')
      val decodeName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
      component(fqn.substring(0, dotPos)).map(_.eventMessages.find(_.name == decodeName).orNull)
    }

    def statusMessage(fqn: String): Option[StatusMessage] = {
      val dotPos = fqn.lastIndexOf('.')
      val decodeName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
      component(fqn.substring(0, dotPos)).map(_.statusMessages.find(_.name == decodeName).orNull)
    }

    def statusMessageOrFail(fqn: String): StatusMessage =
      statusMessage(fqn).getOrElse(sys.error("assertion error"))

    def eventMessageOrFail(fqn: String): EventMessage =
      eventMessage(fqn).getOrElse(sys.error("assertion error"))

    def resolve(): ResolvingResult =
      r.rootNamespaces.flatMap(resolve(_))

    def validate(): ValidatingResult =
      r.rootNamespaces.flatMap(validate(_))

    def resolve(ns: Namespace): ResolvingResult = {
      val result = new mutable.ArrayBuffer[ResolvingResult]()
      result ++= ns.types.map(resolve(_))
      result ++= ns.subNamespaces.map(resolve(_))
      result ++= ns.components.map(resolve(_))
      result.flatten
    }

    def validate(ns: Namespace): ValidatingResult = {
      val result = new mutable.ArrayBuffer[ValidatingResult]()
      result ++= ns.types.map(validate(_))
      result ++= ns.subNamespaces.map(validate(_))
      result ++= ns.components.map(validate(_))
      result.flatten
    }

    def resolve(c: Component): ResolvingResult = {
      val result = mutable.Buffer.empty[ResolvingResult]
      c.baseTypeProxy.map { t =>
        result += r.resolve(t.obj)
        if (t.isResolved)
          result += r.resolve(t.obj)
      }

      c.commands.foreach { cmd =>
        cmd.returnTypeProxy.foreach(rt => result += rt.resolve(r))
        cmd.parameters.foreach { arg =>
          result += arg.paramTypeProxy.resolve(r)
          arg.unitProxy.map(u => result += u.resolve(r))
        }
      }

      c.eventMessages.foreach { e =>
        result += e.baseTypeProxy.resolve(r)
        e.fields.foreach {
          case Right(p) => result += p.paramTypeProxy.resolve(r)
          case _ =>
        }
      }

      c.subComponents.foreach { scr =>
        val sc = scr.componentProxy
        result += sc.resolve(r)
        if (sc.isResolved)
          result += resolve(sc.obj)
      }
      result.flatten
    }

    def validate(c: Component): ValidatingResult = {
      val result = mutable.Buffer.empty[ResolvingResult]

      c.baseTypeProxy.map { t =>
        result += validate(t.obj)
      }

      c.commands.foreach { cmd =>
        cmd.returnType.foreach(rt => result += validate(rt))
        cmd.parameters.foreach { arg =>
          result += validate(arg.paramType)
          arg.unit.map(u => result += validate(u))
        }
      }

      c.eventMessages.foreach { e =>
        result += validate(e.baseType)
        e.fields.foreach {
          case Right(p) => result += validate(p.paramType)
          case _ =>
        }
      }

      c.subComponents.foreach { scr =>
        val sc = scr.component
        result += validate(sc)
      }
      result.flatten
    }

    def resolve(t: DecodeType): ResolvingResult = {
      val resolvingResultList = mutable.Buffer.empty[ResolvingResult]
      t match {
        case t: EnumType =>
          resolvingResultList += (t.extendsOrBaseTypeProxy match {
            case Left(extendsTypeProxy) => extendsTypeProxy.resolve(r)
            case Right(baseTypeProxy) => baseTypeProxy.resolve(r)
          })
        case t: HasBaseType =>
          resolvingResultList += t.baseTypeProxy.resolve(r)
        case t: StructType =>
          t.fields.foreach { f =>
            val typeUnit = f.typeUnit
            resolvingResultList += typeUnit.typeProxy.resolve(r)
            if (typeUnit.typeProxy.isResolved)
              resolvingResultList += resolve(typeUnit.t)
            for (unit <- typeUnit.unitProxy)
              resolvingResultList += unit.resolve(r)
          }
        case t: GenericTypeSpecialized =>
          resolvingResultList += t.genericTypeProxy.resolve(r)
          t.genericTypeArgumentsProxy.foreach(_.foreach(gta =>
            resolvingResultList += gta.resolve(r)))
        case _ =>
      }
      resolvingResultList.flatten
    }

    def validate(t: DecodeType): ValidatingResult = {
      val result = mutable.Buffer.empty[ValidatingResult]
      t match {
        case t: EnumType =>
          t.extendsOrBaseType match {
            case Left(extendsType) => extendsType match {
              case e: EnumType =>
              case e =>
                result += Result.error(s"enum type can extend an enum, not a ${e.getClass}")
            }
            case Right(baseType) => baseType match {
              case _: NativeType =>
              case b =>
                result += Result.error(s"enum base type must be an instance of PrimitiveType or NativeType, not a ${b.getClass}")
            }
          }
        case _ =>
      }
      result.flatten
    }

    def validate(u: DecodeUnit): ValidatingResult = ValidatingResult.empty

    def getOrCreateNamespaceByFqn(namespaceFqn: String): Namespace = {
      require(!namespaceFqn.isEmpty)

      val namespaces = r.rootNamespaces
      var namespace: Option[Namespace] = None

      "\\.".r.split(namespaceFqn).foreach { nsName =>
        val parentNamespace = namespace
        namespace = Some(namespaces.find(_.name.asMangledString == nsName).getOrElse {
          val newNamespace = Namespace(ElementName.newFromMangledName(nsName), parent = parentNamespace)
          parentNamespace match {
            case Some(parentNs) => parentNs.subNamespaces :+= newNamespace
            case _ => r.rootNamespaces :+= newNamespace
          }
          newNamespace
        })
      }
      namespace.getOrElse(sys.error("assertion error"))
    }

    // TODO: refactoring
    def findNamespace(namespaceFqn: Fqn): Option[Namespace] = {
      var namespaces = r.rootNamespaces
      var namespace: Option[Namespace] = None
      for (nsName <- namespaceFqn.parts) {
        namespace = namespaces.find(_.name == nsName)
        namespace match {
          case Some(ns) =>
            namespaces = ns.subNamespaces
          case _ => return namespace
        }
      }
      namespace
    }
  }

}