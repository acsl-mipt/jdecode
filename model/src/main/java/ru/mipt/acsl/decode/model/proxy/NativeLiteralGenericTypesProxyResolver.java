package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.path.GenericTypeName;
import ru.mipt.acsl.decode.model.proxy.path.ProxyElementName;
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath;
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath$;
import ru.mipt.acsl.decode.model.registry.Registry;
import ru.mipt.acsl.decode.model.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class NativeLiteralGenericTypesProxyResolver implements DecodeProxyResolver {

    public static NativeLiteralGenericTypesProxyResolver newInstance() {
        return new NativeLiteralGenericTypesProxyResolver();
    }

    @Override
    public ResolvingResult<Referenceable> resolveElement(Registry registry, ProxyPath path) {
        if (path instanceof ProxyPath.FqnElement)
            return resolveFqnElement(registry, (ProxyPath.FqnElement) path);
        else if (path instanceof ProxyPath.Literal)
            return resolveLiteralElement(registry, (ProxyPath.Literal) path);
        else
            throw new RuntimeException("not implemented");
    }

    private ResolvingResult<Referenceable> resolveLiteralElement(Registry registry, ProxyPath.Literal literal) {
        Namespace ns = registry.findNamespace(Fqn.DECODE_NAMESPACE)
                .orElseThrow(() -> new RuntimeException("decode namespace not found"));
        Alias.NsConst alias = new Alias.NsConst(literal.mangledName(), new HashMap<>(), ns, null);
        alias.obj(Const.newInstance(alias, ns, literal.value(), new ArrayList<>()));
        ns.objects().add(alias);
        ns.objects().add(alias.obj());
        return ResolvingResult.newInstance(alias.obj());
    }

    private ResolvingResult<Referenceable> resolveFqnElement(Registry registry, ProxyPath.FqnElement fqnElement) {

        if(Fqn.DECODE_NAMESPACE.size() != 1)
            throw new AssertionError("not implemented");

        Fqn nsFqn = fqnElement.ns();
        if (!nsFqn.equals(Fqn.DECODE_NAMESPACE))
            return ResolvingResult.newInstance();

        Namespace systemNamespace = registry.findNamespace(Fqn.DECODE_NAMESPACE)
                .orElseThrow(() -> new AssertionError("system namespace not found"));
        ProxyElementName element = fqnElement.element();
        if (!(element instanceof GenericTypeName))
            return ResolvingResult.newInstance();
        GenericTypeName e = (GenericTypeName) element;
        MaybeTypeProxyType maybeProxy = MaybeTypeProxyType.newInstance(Proxy$.MODULE$.apply(
                ProxyPath$.MODULE$.apply(nsFqn, e.typeName())));
        ResolvingMessages result = maybeProxy.resolve(registry);
        if (result.hasError())
            return ResolvingResult.newInstance(null, result);
        DecodeType genericType = maybeProxy.obj();
        ElementName name = element.mangledName();
        GenericTypeSpecialized specializedType = systemNamespace.alias(name).flatMap(a -> a.obj() instanceof GenericTypeSpecialized ?
                Optional.of((GenericTypeSpecialized) a.obj()) : Optional.empty()).orElseGet(() -> {
            Alias.NsType alias = new Alias.NsType(name, new HashMap<>(), genericType.namespace(), null);
            GenericTypeSpecialized newSpecializedType =
                    GenericTypeSpecialized$.MODULE$.apply(alias, genericType.namespace(),
                            MaybeTypeProxyType.newInstance(genericType),
                            e.genericArgumentPaths().stream()
                                    .map(arg -> MaybeTypeProxyType.newInstance(Proxy$.MODULE$.apply(arg)))
                                    .collect(Collectors.toList()),
                            new ArrayList<>());
            alias.obj(newSpecializedType);
            List<Referenceable> objects = systemNamespace.objects();
            objects.add(alias);
            objects.add(newSpecializedType);
            return newSpecializedType;
        });
        ResolvingMessages argsResult = specializedType.genericTypeArgumentsProxy().stream().map(p -> p.resolve(registry))
                .reduce(ResolvingMessages.newInstance(), (l, r) -> { l.addAll(r); return l; });
        return argsResult.hasError()
                ? ResolvingResult.newInstance(null, argsResult)
                : ResolvingResult.newInstance(specializedType);
    }

    private NativeLiteralGenericTypesProxyResolver() {

    }
}

