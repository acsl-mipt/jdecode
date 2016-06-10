package ru.mipt.acsl.decode.model.registry;

import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.DecodeProxyResolver;

import java.util.List;

/**
 * @author Artem Shein
 */
class RegistryImpl implements Registry {

    private final ElementName name;
    private Namespace rootNamespace;
    private final List<DecodeProxyResolver> proxyResolvers;

    RegistryImpl(ElementName name, Namespace rootNamespace, List<DecodeProxyResolver> proxyResolvers) {
        this.name = name;
        this.rootNamespace = rootNamespace;
        this.proxyResolvers = proxyResolvers;

        if (Fqn.DECODE_NAMESPACE.size() != 1)
            throw new RuntimeException("not implemented");
    }

    @Override
    public Namespace rootNamespace() {
        return rootNamespace;
    }

    @Override
    public void setRootNamespace(Namespace ns) {
        rootNamespace = ns;
    }

    @Override
    public List<DecodeProxyResolver> proxyResolvers() {
        return proxyResolvers;
    }
}
