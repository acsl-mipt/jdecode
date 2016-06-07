package ru.mipt.acsl.decode.model

import ru.mipt.acsl.decode.model.component.message.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.component.{Command, Component}
import ru.mipt.acsl.decode.model.naming.{Container, ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.proxy.{ResolvingMessages, Result}
import ru.mipt.acsl.decode.model.types.{Alias, DecodeType, EnumType, NativeType, StructField, SubType, TypeMeasure}

import scala.collection.JavaConversions._
import scala.collection.mutable

package object registry {

  implicit class RegistryHelper(val r: Registry) {

    def allComponents: Seq[Component] = r.rootNamespace.allComponents

    def allNamespaces: Seq[Namespace] = r.rootNamespace.allNamespaces

    def component(fqn: String): Option[Component] = {
      val dotPos = fqn.lastIndexOf('.')
      val namespaceOptional = namespace(fqn.substring(0, dotPos))
      if (namespaceOptional.isEmpty)
        return None
      val componentName = ElementName.newInstanceFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
      namespaceOptional.get.components.find(_.name == componentName)
    }

    // FIXME: duplicate with findNamespace
    def namespace(fqn: String): Option[Namespace] = {
      var currentNamespaces: Option[Seq[Namespace]] = Some(r.rootNamespace.subNamespaces)
      var currentNamespace: Option[Namespace] = None
      "\\.".r.split(fqn).foreach { nsName =>
        if (currentNamespaces.isEmpty)
          return None
        val decodeName = ElementName.newInstanceFromMangledName(nsName)
        currentNamespace = currentNamespaces.get.find(_.name == decodeName)
        currentNamespaces = currentNamespace.map(_.subNamespaces)
      }
      currentNamespace
    }

    def eventMessage(fqn: String): Option[EventMessage] = {
      val dotPos = fqn.lastIndexOf('.')
      val decodeName = ElementName.newInstanceFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
      component(fqn.substring(0, dotPos)).flatMap(_.eventMessage(decodeName))
    }

    def statusMessage(fqn: String): Option[StatusMessage] = {
      val dotPos = fqn.lastIndexOf('.')
      val decodeName = ElementName.newInstanceFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
      component(fqn.substring(0, dotPos)).flatMap(_.statusMessage(decodeName))
    }

    def statusMessageOrFail(fqn: String): StatusMessage =
      statusMessage(fqn).getOrElse(sys.error("assertion error"))

    def eventMessageOrFail(fqn: String): EventMessage =
      eventMessage(fqn).getOrElse(sys.error("assertion error"))

    def resolve(): ResolvingMessages = resolve(r.rootNamespace)

    def validate(): ValidatingResult = validate(r.rootNamespace)

    def resolve(obj: Referenceable): ResolvingMessages = obj match {
      case container: Container =>
        container.objects.flatMap(resolve(_)) ++
          (container match {
            case c: Command =>
              c.returnTypeProxy.resolve(r)
            case e: EnumType =>
              val proxy = e.extendsOrBaseTypeProxy()
              val enum = proxy.maybeProxyEnum()
              if (enum.isPresent)
                enum.get().resolve(r)
              else
                proxy.typeMeasure().get().typeProxy.resolve(r)
            case e: EventMessage =>
              e.baseTypeProxy.resolve(r)
            case _ =>
              ResolvingMessages.empty
          })
      case cc: Alias.ComponentComponent =>
        cc.obj().resolve(r)
      case s: SubType if !s.isGeneric =>
        s.typeMeasure.typeProxy.resolve(r)
      case tm: TypeMeasure =>
        tm.typeProxy.resolve(r) ++ Option(tm.measureProxy.orElse(null)).seq.flatMap(_.resolve(r))
      case p: Parameter =>
        p.typeProxy.resolve(r) ++ Option(p.measureProxy.orElse(null)).seq.flatMap(_.resolve(r))
      case sf: StructField =>
        sf.typeMeasure.typeProxy.resolve(r) ++ Option(sf.typeMeasure.measureProxy.orElse(null)).seq.flatMap(_.resolve(r))
      case _ => ResolvingMessages.empty
    }

    def validate(obj: Referenceable): ValidatingResult = obj match {
      case container: Container =>
        container.objects.flatMap(validate(_))
    }

    /*def resolve(c: Component): ResolvingMessages = {
      val result = mutable.Buffer.empty[ResolvingMessages]
      c.baseTypeProxy.map { t =>
        result += r.resolve(t.obj)
        if (t.isResolved)
          result += r.resolve(t.obj)
      }

      c.commands.foreach { cmd =>
        result += cmd.returnTypeProxy.resolve(r)
        cmd.parameters.foreach { arg =>
          result += arg.typeProxy.resolve(r)
          arg.unitProxy.map(u => result += u.resolve(r))
        }
      }

      c.eventMessages.foreach { e =>
        result += e.baseTypeProxy.resolve(r)
        e.parameters.foreach {
          case Parameter(_, typeUnit) => result += typeUnit.typeProxy.resolve(r)
          case _ =>
        }
      }

      c.subComponents.foreach { scr =>
        val sc = scr.component
        result += sc.resolve(r)
        if (sc.isResolved)
          result += resolve(sc.obj)
      }
      result.flatten
    }*/

    def validate(c: Component): ValidatingResult = {
      val result = mutable.Buffer.empty[ResolvingMessages]

      c.baseTypeProxy.map { t =>
        result += validate(t.obj)
      }

      c.commands.foreach { cmd =>
        result += validate(cmd.returnType)
        cmd.parameters.foreach { arg =>
          result += validate(arg.parameterType)
          Option(arg.measure.orElse(null)).map(u => result += validate(u))
        }
      }

      c.eventMessages.foreach { e =>
        result += validate(e.baseType)
        e.parameters.foreach {
          case p: Parameter => result += validate(p.typeMeasure().t)
          case _ =>
        }
      }

      c.subComponents.foreach { scr =>
        val sc = scr.obj
        result += validate(sc)
      }
      result.flatten
    }

    /*def resolve(t: DecodeType): ResolvingMessages = {
      val resolvingResultList = mutable.Buffer.empty[ResolvingMessages]
      t match {
        case t: EnumType =>
          resolvingResultList += (t.extendsOrBaseTypeProxy match {
            case Left(extendsTypeProxy) => extendsTypeProxy.resolve(r)
            case Right(baseTypeProxy) => baseTypeProxy.typeProxy.resolve(r)
          })
        case t: StructType =>
          t.fields.foreach { f =>
            val typeUnit = f.typeMeasure
            resolvingResultList += typeUnit.typeProxy.resolve(r)
            if (typeUnit.typeProxy.isResolved)
              resolvingResultList += resolve(typeUnit.t)
            for (unit <- typeUnit.unitProxy)
              resolvingResultList += unit.resolve(r)
          }
        case t: GenericTypeSpecialized =>
          resolvingResultList += t.genericTypeProxy.resolve(r)
          t.genericTypeArgumentsProxy.foreach(gta =>
            resolvingResultList += gta.resolve(r))
        case _ =>
      }
      resolvingResultList.flatten
    }*/

    def validate(t: DecodeType): ValidatingResult = {
      val result = mutable.Buffer.empty[ValidatingResult]
      t match {
        case t: EnumType =>
          val extendsOrBaseType = t.eitherExtendsOrBaseType
          val enumType = extendsOrBaseType.enumType()
          enumType.isPresent match {
            case true => enumType.get() match {
              case e: EnumType =>
              case e =>
                result += Result.error(s"enum type can extend an enum, not a ${e.getClass}")
            }
            case false => extendsOrBaseType.typeMeasure().get() match {
              case _: NativeType =>
              case b =>
                result += Result.error(s"enum base type must be an instance of PrimitiveType or NativeType, not a ${b.getClass}")
            }
          }
        case _ =>
      }
      result.flatten
    }

    def validate(u: Measure): ValidatingResult = ValidatingResult.empty

    /*def getOrCreateNamespaceByFqn(namespaceFqn: String): Namespace = {
      require(!namespaceFqn.isEmpty)

      val namespaces = r.rootNamespaces
      var namespace: Option[Namespace] = None

      "\\.".r.split(namespaceFqn).foreach { nsName =>
        val parentNamespace = namespace
        namespace = Some(namespaces.find(_.name.asMangledString == nsName).getOrElse {
          val alias = Alias[Namespace, Namespace](ElementName.newFromMangledName(nsName), parentNamespace, LocalizedString.empty, null)
          val newNamespace = Namespace(alias, parentNamespace)
          parentNamespace match {
            case Some(parentNs) => parentNs.subNamespaces :+= newNamespace
            case _ => r.rootNamespaces :+= newNamespace
          }
          newNamespace
        })
      }
      namespace.getOrElse(sys.error("assertion error"))
    }*/

    // TODO: refactoring
    def findNamespace(namespaceFqn: Fqn): Option[Namespace] = {
      var namespaces = r.rootNamespace.subNamespaces
      var namespace: Option[Namespace] = None
      for (nsName <- namespaceFqn.getParts) {
        namespace = namespaces.find(_.name == nsName)
        namespace match {
          case Some(ns) =>
            namespaces = ns.subNamespaces
          case _ =>
            return namespace
        }
      }
      namespace
    }
  }

}