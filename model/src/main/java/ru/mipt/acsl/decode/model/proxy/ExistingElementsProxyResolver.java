package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.Message;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.path.ProxyElementName;
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath;
import ru.mipt.acsl.decode.model.proxy.path.TypeName;
import ru.mipt.acsl.decode.model.registry.Registry;
import ru.mipt.acsl.decode.model.registry.RegistryUtils;
import ru.mipt.acsl.decode.model.types.Alias;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class ExistingElementsProxyResolver implements DecodeProxyResolver {

    public static ExistingElementsProxyResolver newInstance() {
        return new ExistingElementsProxyResolver();
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
                .orElseThrow(() -> new RuntimeException("system namespace must exist"));
        List<Alias> elements = ns.aliases().stream().filter(a -> a.name().equals(literal.mangledName())).collect(
                Collectors.toList());
        if (elements.size() == 1)
        {
            Alias alias = elements.get(0);
            RegistryUtils.resolve(registry, alias);
            Referenceable obj = alias.referenceable();
            return ResolvingResult.newInstance(obj, RegistryUtils.resolve(registry, obj));
        }
        else if (elements.size() > 1)
            return ResolvingResult.newInstance(
                    Message.newError(String.format("must be exactly one element for %s, found: %d", literal, elements.size())));
        else
            return ResolvingResult.newInstance();
    }

    private ResolvingResult<Referenceable> resolveFqnElement(Registry registry, ProxyPath.FqnElement fqnElement) {
        Optional<Namespace> nsOption = registry.findNamespace(fqnElement.ns());
        if (!nsOption.isPresent())
            return ResolvingResult.newInstance(
                    Message.newError(String.format("namespace not found %s", fqnElement.ns().mangledNameString())));
        Namespace ns = nsOption.get();
        ProxyElementName element = fqnElement.element();
        if (element instanceof TypeName)
        {
            ElementName name = ((TypeName) element).typeName();
            List<Alias> elements =
                    ns.aliases().stream().filter(a -> a.name().equals(name)).collect(Collectors.toList());
            if (elements.size() == 1)
            {
                Alias alias = elements.get(0);
                RegistryUtils.resolve(registry, alias);
                Referenceable obj = alias.referenceable();
                return ResolvingResult.newInstance(obj, RegistryUtils.resolve(registry, obj));
            }
            else
                return ResolvingResult.newInstance(
                        Message.newError(String.format("must be exactly one element for %s, found: %d", fqnElement,
                                elements.size())));
        }
        else
            return ResolvingResult.newInstance();
    }

    private ExistingElementsProxyResolver() {

    }
}
