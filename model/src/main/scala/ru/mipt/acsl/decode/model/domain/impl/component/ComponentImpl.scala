package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.aliases.{ElementInfo, ValidatingResult}
import ru.mipt.acsl.decode.model.domain.component.messages.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.domain.component.{Command, Component, ComponentRef}
import ru.mipt.acsl.decode.model.domain.impl.naming.Fqn
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractNameNamespaceOptionalInfoAware
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.registry.Registry
import ru.mipt.acsl.decode.model.domain.types.StructType

import scala.collection.{immutable, mutable}

/**
  * @author Artem Shein
  */
private class ComponentImpl(name: ElementName, namespace: Namespace, var id: Option[Int],
                            var baseType: Option[MaybeProxy[StructType]], info: ElementInfo,
                            var subComponents: immutable.Seq[ComponentRef],
                            var commands: immutable.Seq[Command] = immutable.Seq.empty,
                            var eventMessages: immutable.Seq[EventMessage] = immutable.Seq.empty,
                            var statusMessages: immutable.Seq[StatusMessage] = immutable.Seq.empty)
  extends AbstractNameNamespaceOptionalInfoAware(name, namespace, info) with Component {
  override def fqn: Fqn = Fqn.newFromFqn(namespace.fqn, name)
  override def resolve(registry: Registry): ResolvingResult = {
    val result = mutable.Buffer.empty[ResolvingResult]
    baseType.map { t =>
      result += t.resolve(registry)
      if (t.isResolved)
        result += t.obj.resolve(registry)
    }
    commands.foreach { cmd =>
      cmd.returnType.foreach(rt => result += rt.resolve(registry))
      cmd.parameters.foreach { arg =>
        result += arg.paramType.resolve(registry)
        arg.unit.map(u => result += u.resolve(registry))
      }
    }
    eventMessages.foreach { e =>
      result += e.baseType.resolve(registry)
      e.fields.foreach {
        case Right(p) => result += p.paramType.resolve(registry)
        case _ =>
      }
    }
    subComponents.foreach { scr =>
      val sc = scr.component
      result += sc.resolve(registry)
      if (sc.isResolved)
        result += sc.obj.resolve(registry)
    }
    result.flatten
  }

  override def validate(registry: Registry): ValidatingResult = {
    val result = mutable.Buffer.empty[ResolvingResult]
    baseType.map { t =>
        result += t.obj.validate(registry)
    }
    commands.foreach { cmd =>
      cmd.returnType.foreach(rt => result += rt.obj.validate(registry))
      cmd.parameters.foreach { arg =>
        result += arg.paramType.obj.validate(registry)
        arg.unit.map(u => result += u.obj.validate(registry))
      }
    }
    eventMessages.foreach { e =>
      result += e.baseType.obj.validate(registry)
      e.fields.foreach {
        case Right(p) => result += p.paramType.obj.validate(registry)
        case _ =>
      }
    }
    subComponents.foreach { scr =>
      val sc = scr.component
      result += sc.obj.validate(registry)
    }
    result.flatten
  }
}
