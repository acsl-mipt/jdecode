package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.proxy.DecodeTypeResolveVisitor
import ru.mipt.acsl.decode.modeling.aliases.ResolvingMessage

import scala.collection.mutable

/**
  * @author Artem Shein
  */
class ProxyImpl[T <: Referenceable](val path: ProxyPath) extends Proxy[T] {
  override def resolve(registry: Registry, cls: Class[T]): ResolvingResult[T] = registry.resolve(path, cls)
  override def toString: String = path.toString
}

object ProxyImpl {
  def apply[T <: Referenceable](path: ProxyPath): Proxy[T] = new ProxyImpl[T](path)
}

object DecodeModelResolver {
  def resolve(namespace: Namespace, registry: Registry): mutable.Buffer[ResolvingResult[Referenceable]] = {
    val result = new mutable.ArrayBuffer[ResolvingResult[Referenceable]]()
    result ++= namespace.types.map(resolve(_, registry))
    result ++= namespace.subNamespaces.flatMap(resolve(_, registry))
    result ++= namespace.components.flatMap(resolve(_, registry))
    result
  }

  def resolveWithTypeCheck[T <: Referenceable](maybeProxy: MaybeProxy[T], registry: Registry, cls: Class[T]): ResolvingResult[Referenceable] = {
    // TODO: this is dumb copy-paste from Java, Scala can do better i believe
    maybeProxy.resolve(registry, cls) match {
      case a: ResolvingResult[T] => a
      case _ => sys.error("wtf")
    }
  }

  def resolve(t: DecodeType, registry: Registry):  ResolvingResult[Referenceable] = {
    val resolvingResultList = mutable.Buffer.empty[ResolvingResult[Referenceable]]
    t.accept(new DecodeTypeResolveVisitor(registry, resolvingResultList))
    resolvingResultList.foldLeft(ResolvingResult.empty[Referenceable])(ResolvingResult.merge)
  }

  def resolve(registry: Registry): Iterable[ResolvingMessage] = {
    registry.rootNamespaces.flatMap(resolve(_, registry)).foldLeft(
      ResolvingResult.empty[Referenceable])(ResolvingResult.merge).messages
  }

  def resolve(component: Component, registry: Registry): Seq[ResolvingResult[Referenceable]] = {
    val resultList = mutable.Buffer.empty[ResolvingResult[Referenceable]]
    component.baseType.map { t =>
      resultList += resolveWithTypeCheck(t, registry, classOf[StructType])
      if (t.isResolved)
        resultList += DecodeModelResolver.resolve(t.obj, registry)
    }
    component.commands.foreach { cmd =>
      cmd.returnType.foreach(rt => resultList += resolveWithTypeCheck(rt, registry, classOf[DecodeType]))
      cmd.parameters.foreach { arg =>
        resultList += resolveWithTypeCheck(arg.paramType, registry, classOf[DecodeType])
        arg.unit.map(u => resultList += resolveWithTypeCheck(u, registry, classOf[Measure]))
      }
    }

    component.eventMessages.foreach { e =>
        resultList += resolveWithTypeCheck(e.baseType, registry, classOf[DecodeType])
        e.fields.foreach {
          case Right(p) => resultList += resolveWithTypeCheck(p.paramType, registry, classOf[DecodeType])
          case _ =>
        }
    }

    component.subComponents.foreach { scr =>
      val sc = scr.component
      resultList += resolveWithTypeCheck(sc, registry, classOf[Component])
      if (sc.isResolved)
        resultList ++= resolve(sc.obj, registry)
    }
    resultList
  }
}