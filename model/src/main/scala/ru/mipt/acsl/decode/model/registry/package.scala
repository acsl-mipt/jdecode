package ru.mipt.acsl.decode.model

import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.naming.Container
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.types.{DecodeType, EnumType, NativeType}

import scala.collection.JavaConversions._

package object registry {

  implicit class RegistryHelper(val r: Registry) {

    def validate(): ValidatingResult = validate(r.rootNamespace)

    def validate(maybeProxy: MaybeProxy): ValidatingResult = ValidatingResult.newInstance()

    def validate(obj: Referenceable): ValidatingResult = obj match {
      case container: Container =>
        container.objects.foldLeft(ValidatingResult.newInstance()){(r, ref) => r.addAll(validate(ref)); r}
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
      val result = ValidatingResult.newInstance()

      if (c.baseTypeProxy.isPresent)
        result.addAll(validate(c.baseTypeProxy().get().obj))

      c.commands.foreach { cmd =>
        result.addAll(validate(cmd.returnType))
        cmd.parameters.foreach { arg =>
          result.addAll(validate(arg.parameterType))
          Option(arg.measure.orElse(null)).foreach(u => result.addAll(validate(u)))
        }
      }

      c.eventMessages.foreach { e =>
        result.addAll(validate(e.baseType))
        e.parameters.foreach {
          case p: Parameter => result.addAll(validate(p.typeMeasure().t))
          case _ =>
        }
      }

      c.subComponents.foreach { scr =>
        val sc = scr.obj()
        result.addAll(validate(sc))
      }
      result
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
      val result = ValidatingResult.newInstance()
      t match {
        case t: EnumType =>
          val extendsOrBaseType = t.eitherExtendsOrBaseType
          val enumType = extendsOrBaseType.enumType()
          enumType.isPresent match {
            case true => enumType.get() match {
              case e: EnumType =>
              case e =>
                result.add(Message.newError(s"enum type can extend an enum, not a ${e.getClass}"))
            }
            case false => extendsOrBaseType.typeMeasure().get() match {
              case _: NativeType =>
              case b =>
                result.add(Message.newError(s"enum base type must be an instance of PrimitiveType or NativeType, not a ${b.getClass}"))
            }
          }
        case _ =>
      }
      result
    }

    def validate(u: Measure): ValidatingResult = ValidatingResult.newInstance()

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


  }

}