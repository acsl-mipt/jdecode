package ru.mipt.acsl.decode.model.proxy

import java.util
import java.util.Optional

import ru.mipt.acsl.decode.model.Referenceable
import ru.mipt.acsl.decode.model.naming.Fqn
import ru.mipt.acsl.decode.model.proxy.path.{GenericTypeName, ProxyPath}
import ru.mipt.acsl.decode.model.registry.Registry
import ru.mipt.acsl.decode.model.types.{Alias, Const, GenericTypeSpecialized}

import scala.collection.JavaConversions._

/**
  * Created by metadeus on 20.02.16.
  */
case object NativeLiteralGenericTypesProxyResolver extends DecodeProxyResolver {

  override def resolveElement(registry: Registry, path: ProxyPath): ResolvingResult[Referenceable] = {
    path match {
      case fqnElement: ProxyPath.FqnElement =>
        resolveFqnElement(registry, fqnElement)
      case l: ProxyPath.Literal =>
        resolveLiteralElement(registry, l)
    }
  }

  private def resolveLiteralElement(registry: Registry, literal: ProxyPath.Literal): ResolvingResult[Referenceable] = {
    val ns = registry.findNamespace(Fqn.DECODE_NAMESPACE).getOrElse(sys.error("decode namespace not found"))
    val alias = new Alias.NsConst(literal.mangledName, util.Collections.emptyMap(), ns, null)
    alias.obj(Const.newInstance(alias, ns, literal.value, util.Collections.emptyList()))
    ns.objects().add(alias)
    ns.objects().add(alias.obj)
    ResolvingResult(Some(alias.obj))
  }

  private def resolveFqnElement(registry: Registry, fqnElement: ProxyPath.FqnElement): ResolvingResult[Referenceable] = {

    assert(Fqn.DECODE_NAMESPACE.size == 1, "not implemented")

    val nsFqn = fqnElement.ns
    if (nsFqn != Fqn.DECODE_NAMESPACE)
      return ResolvingResult(None)

    val systemNamespace = registry.findNamespace(Fqn.DECODE_NAMESPACE).getOrElse(sys.error("system namespace not found"))
    fqnElement.element match {
      // Generic type
      case e: GenericTypeName =>
        val maybeProxy = MaybeProxy.Type(Left(Proxy(ProxyPath(nsFqn, e.typeName))))
        val result = maybeProxy.resolve(registry)
        if (result.hasError)
          return ResolvingResult(None, result)
        val genericType = maybeProxy.obj
        val name = fqnElement.element.mangledName
        val specializedType = Option(systemNamespace.alias(name).orElse(null)).flatMap(_.obj match {
          case g: GenericTypeSpecialized => Some(g)
        }).getOrElse {
          val alias = new Alias.NsType(name, util.Collections.emptyMap(), genericType.namespace, null)
          val specializedType = GenericTypeSpecialized(alias, genericType.namespace,
            MaybeProxy.Type(Right(genericType)),
            e.genericArgumentPaths.map(arg => MaybeProxy.Type(Left(Proxy(arg)))), util.Collections.emptyList())
          alias.obj(specializedType)
          systemNamespace.objects ++= Seq(alias, specializedType)
          specializedType
        }
        val argsResult = specializedType.genericTypeArgumentsProxy.map(_.resolve(registry))
          .foldLeft(Result.empty)(_ ++ _)
        if (argsResult.hasError)
          ResolvingResult(None, argsResult)
        else
          ResolvingResult(Some(specializedType), Result.empty)
      case _ =>
        ResolvingResult(None)
    }
  }
}
