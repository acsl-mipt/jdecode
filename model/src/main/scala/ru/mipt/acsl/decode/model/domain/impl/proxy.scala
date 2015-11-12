package ru.mipt.acsl.decode.model.domain.impl

import java.net.URI

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.proxy.DecodeTypeResolveVisitor

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * @author Artem Shein
  */
class DecodeProxyImpl[T <: DecodeReferenceable](val uri: URI) extends DecodeProxy[T] {
  override def resolve(registry: DecodeRegistry, cls: Class[T]): DecodeResolvingResult[T] = registry.resolve(uri, cls)
  override def toString: String = uri.toString
}

object DecodeProxyImpl {
  def newInstanceFromTypeUriString[T <: DecodeReferenceable](typeUriString: String, defaultNsFqn: DecodeFqn):  DecodeProxy[T] =
    apply(URI.create(if (typeUriString.startsWith("/")) typeUriString else "/" + defaultNsFqn.parts.map(_.asString()).mkString("/") + typeUriString))


  def apply[T <: DecodeReferenceable](uri: URI): DecodeProxy[T] = new DecodeProxyImpl[T](uri)
}

object DecodeModelResolver {
  def resolve(namespace: DecodeNamespace, registry: DecodeRegistry): mutable.Buffer[DecodeResolvingResult[DecodeReferenceable]] = {
    val result = new ArrayBuffer[DecodeResolvingResult[DecodeReferenceable]]()
    result ++= namespace.types.map(resolve(_, registry))
    result ++= namespace.subNamespaces.flatMap(resolve(_, registry))
    result ++= namespace.components.flatMap(resolve(_, registry))
    result
  }

  def resolveWithTypeCheck[T <: DecodeReferenceable](maybeProxy: DecodeMaybeProxy[T], registry: DecodeRegistry, cls: Class[T]): DecodeResolvingResult[DecodeReferenceable] = {
    // TODO: this is dumb copypaste from Java, Scala can do better, i believe
    maybeProxy.resolve(registry, cls) match {
      case a: DecodeResolvingResult[DecodeReferenceable] => a
      case _ => sys.error("wtf")
    }
  }

  def resolve(t: DecodeType, registry: DecodeRegistry):  DecodeResolvingResult[DecodeReferenceable] = {
    val resolvingResultList = new ArrayBuffer[DecodeResolvingResult[DecodeReferenceable]]()
    t.accept(new DecodeTypeResolveVisitor(registry, resolvingResultList))
    resolvingResultList.reduce[DecodeResolvingResult[DecodeReferenceable]](SimpleDecodeResolvingResult.merge)
  }

  def resolve(registry: DecodeRegistry): DecodeResolvingResult[DecodeReferenceable] = {
    registry.rootNamespaces.flatMap(resolve(_, registry)).reduce[DecodeResolvingResult[DecodeReferenceable]](SimpleDecodeResolvingResult.merge)
  }

  def resolve(component: DecodeComponent, registry: DecodeRegistry): Seq[DecodeResolvingResult[DecodeReferenceable]] = {
    val resultList = mutable.Buffer[DecodeResolvingResult[DecodeReferenceable]]()
    component.baseType.map(t => {
      resultList += resolveWithTypeCheck(t, registry, classOf[DecodeStructType])
      if (t.isResolved)
      {
        resultList += DecodeModelResolver.resolve(t.obj, registry)
      }
    })
    component.commands.foreach(cmd => {
      cmd.returnType.foreach(rt => {
        resultList += resolveWithTypeCheck(rt, registry, classOf[DecodeType])
      })
      cmd.arguments.foreach(arg => {
        resultList += resolveWithTypeCheck(arg.argType, registry, classOf[DecodeType])
        arg.unit.map(u => {
          resultList += resolveWithTypeCheck(u, registry, classOf[DecodeUnit])
        })
      })
    })
    component.subComponents.foreach(scr => {
      val sc = scr.component
      resultList += resolveWithTypeCheck(sc, registry, classOf[DecodeComponent])
      if (sc.isResolved)
      {
        resultList ++= resolve(sc.obj, registry)
      }
    })
    resultList
  }
}