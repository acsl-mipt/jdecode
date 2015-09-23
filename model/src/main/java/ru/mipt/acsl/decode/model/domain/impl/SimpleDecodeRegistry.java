package ru.mipt.acsl.decode.model.domain.impl;

import com.google.common.collect.Lists;
import ru.mipt.acsl.decode.model.domain.impl.proxy.ProvidePrimitivesAndNativeTypesDecodeProxyResolver;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeProxyResolver;
import ru.mipt.acsl.decode.model.domain.impl.proxy.FindExistingDecodeProxyResolver;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeConstants;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.DecodeReferenceable;
import ru.mipt.acsl.decode.model.domain.DecodeRegistry;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class SimpleDecodeRegistry implements DecodeRegistry
{
    @NotNull
    private final List<DecodeNamespace> rootNamespaces = new ArrayList<>();
    @NotNull
    private final List<DecodeProxyResolver> proxyResolvers = new ArrayList<>();

    public static DecodeRegistry newInstance()
    {
        return new SimpleDecodeRegistry(new FindExistingDecodeProxyResolver(), new ProvidePrimitivesAndNativeTypesDecodeProxyResolver());
    }

    private SimpleDecodeRegistry(@NotNull DecodeProxyResolver... resolvers)
    {
        proxyResolvers.addAll(Lists.newArrayList(resolvers));
        rootNamespaces.add(SimpleDecodeNamespace.newInstance(DecodeConstants.SYSTEM_NAMESPACE_NAME,
                Optional.<DecodeNamespace>empty()));

    }

    @NotNull
    @Override
    public List<DecodeNamespace> getRootNamespaces()
    {
        return rootNamespaces;
    }

    @NotNull
    @Override
    public <T extends DecodeReferenceable> DecodeResolvingResult<T> resolve(@NotNull URI uri, @NotNull Class<T> cls)
    {
        for (DecodeProxyResolver resolver : proxyResolvers)
        {
            DecodeResolvingResult<T> result = resolver.resolve(this, uri, cls);
            if (result.getResolvedObject().isPresent())
            {
                return result;
            }
        }
        return SimpleDecodeResolvingResult.immutableEmpty();
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{rootNamespaces=%s, proxyResolvers=%s}", SimpleDecodeRegistry.class.getName(),
                rootNamespaces, proxyResolvers);
    }

}
