package ru.mipt.acsl.decode.model.domain.impl;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.proxy.FindExistingDecodeProxyResolver;
import ru.mipt.acsl.decode.model.domain.impl.proxy.ProvidePrimitivesAndNativeTypesDecodeProxyResolver;
import ru.mipt.acsl.decode.model.domain.impl.type.DecodeNamespaceImpl;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Buffer;

import java.net.URI;
import java.util.Arrays;

/**
 * @author Artem Shein
 */
public class SimpleDecodeRegistry implements DecodeRegistry
{
    @NotNull
    private final Buffer<DecodeNamespace> rootNamespaces = new ArrayBuffer<>();
    @NotNull
    private final Buffer<DecodeProxyResolver> proxyResolvers = new ArrayBuffer<>();

    public static DecodeRegistry newInstance()
    {
        return new SimpleDecodeRegistry(new FindExistingDecodeProxyResolver(), new ProvidePrimitivesAndNativeTypesDecodeProxyResolver());
    }

    private SimpleDecodeRegistry(@NotNull DecodeProxyResolver... resolvers)
    {
        proxyResolvers.append(JavaConversions.asScalaBuffer(Arrays.asList(resolvers)));
        Preconditions.checkState(DecodeConstants.SYSTEM_NAMESPACE_FQN.size() == 1, "not implemented");
        rootNamespaces.$plus$eq(DecodeNamespaceImpl.apply(DecodeConstants.SYSTEM_NAMESPACE_FQN.last(),
                Option.<DecodeNamespace>empty()));

    }

    @NotNull
    @Override
    public Buffer<DecodeNamespace> rootNamespaces()
    {
        return rootNamespaces;
    }

    @NotNull
    @Override
    public <T extends DecodeReferenceable> DecodeResolvingResult<T> resolve(@NotNull URI uri, @NotNull Class<T> cls)
    {
        Iterator<DecodeProxyResolver> iterator = proxyResolvers.iterator();
        while (iterator.hasNext())
        {
            DecodeResolvingResult<T> result = iterator.next().resolve(this, uri, cls);
            if (result.resolvedObject().isDefined())
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
