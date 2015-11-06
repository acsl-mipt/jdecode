package ru.mipt.acsl.decode.model.domain.impl.proxy;

import ru.mipt.acsl.common.Either;
import ru.mipt.acsl.decode.model.domain.impl.DecodeUtils;
import ru.mipt.acsl.decode.model.domain.impl.SimpleDecodeResolvingResult;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.*;

import java.net.URI;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class SimpleDecodeMaybeProxy<T extends DecodeReferenceable> extends Either<T, DecodeProxy<T>> implements
        DecodeMaybeProxy<T>
{
    @NotNull
    private Optional<T> resolvedObject;
    @NotNull
    private Optional<DecodeProxy<T>> proxy;

    @NotNull
    private static <K extends DecodeReferenceable> DecodeMaybeProxy<K> proxy(@NotNull DecodeProxy<K> proxy)
    {
        return new SimpleDecodeMaybeProxy<>(proxy);
    }

    @NotNull
    public static <K extends DecodeReferenceable> DecodeMaybeProxy<K> proxy(@NotNull URI uri)
    {
        return proxy(SimpleDecodeProxy.newInstance(uri));
    }

    @NotNull
    public static <K extends DecodeReferenceable> DecodeMaybeProxy<K> proxy(@NotNull DecodeFqn namespaceFqn,
                                                                          @NotNull DecodeName name)
    {
        return proxy(DecodeUtils.getUriForNamespaceAndName(namespaceFqn, name));
    }

    @NotNull
    public static <K extends DecodeReferenceable> DecodeMaybeProxy<K> proxyForTypeString(@NotNull String string,
                                                                                         @NotNull DecodeFqn defaultNamespaceFqn)
    {
        return proxy(DecodeUtils.getUriForSourceTypeFqnString(string, defaultNamespaceFqn));
    }

    @NotNull
    public static <K extends DecodeReferenceable> DecodeMaybeProxy<K> proxyForSystemTypeString(@NotNull String typeString)
    {
        typeString = DecodeUtils.normalizeSourceTypeString(typeString);
        return proxyForTypeString(typeString, DecodeConstants.SYSTEM_NAMESPACE_FQN);
    }

    @NotNull
    public static <K extends DecodeReferenceable> DecodeMaybeProxy<K> proxyForSystem(@NotNull DecodeName decodeName)
    {
        return proxy(DecodeConstants.SYSTEM_NAMESPACE_FQN, decodeName);
    }

    @NotNull
    public static <K extends DecodeReferenceable> DecodeMaybeProxy<K> proxyDefaultNamespace(@NotNull
                                                                                            DecodeFqn elementFqn, @NotNull
                                                                                            DecodeNamespace defaultNamespace)
    {
        return elementFqn.size() > 1? proxy(elementFqn.copyDropLast(), elementFqn.getLast()) : proxy(defaultNamespace.getFqn(), elementFqn.getLast());
    }

    @NotNull
    public static <K extends DecodeReferenceable> DecodeMaybeProxy<K> object(@NotNull K object)
    {
        return new SimpleDecodeMaybeProxy<>(object);
    }

    @NotNull
    public static <T extends DecodeReferenceable> DecodeMaybeProxy<T> proxyForTypeUriString(@NotNull String typeUriString,
                                                                                            @NotNull DecodeFqn defaultNsFqn)
    {
        return new SimpleDecodeMaybeProxy<T>(SimpleDecodeProxy.newInstanceFromTypeUriString(typeUriString, defaultNsFqn));
    }

    @Override
    public DecodeResolvingResult<T> resolve(@NotNull DecodeRegistry registry, @NotNull Class<T> cls)
    {
        if (!isProxy())
        {
            return SimpleDecodeResolvingResult.newInstance(resolvedObject);
        }
        DecodeResolvingResult<T> resolvingResult = proxy.get().resolve(registry, cls);
        if (resolvingResult.getResolvedObject().isPresent())
        {
            resolvedObject = resolvingResult.getResolvedObject();
            proxy = Optional.empty();
            return resolvingResult;
        }
        return SimpleDecodeResolvingResult.error("Can't resolve proxy '%s'", this);
    }

    @Override
    public boolean isProxy()
    {
        return isRight();
    }

    @NotNull
    @Override
    public T getObject()
    {
        return getLeft();
    }

    @NotNull
    @Override
    public DecodeProxy<T> getProxy()
    {
        return proxy.get();
    }

    @Override
    public boolean isLeft()
    {
        return resolvedObject.isPresent();
    }

    @Override
    public boolean isRight()
    {
        return !resolvedObject.isPresent();
    }

    @Override
    @NotNull
    public T getLeft()
    {
        return resolvedObject.get();
    }

    @Override
    @NotNull
    public DecodeProxy<T> getRight()
    {
        return proxy.get();
    }

    private SimpleDecodeMaybeProxy(@NotNull DecodeProxy<T> right)
    {
        resolvedObject = Optional.empty();
        proxy = Optional.of(right);
    }

    private SimpleDecodeMaybeProxy(@NotNull T object)
    {
        resolvedObject = Optional.of(object);
        proxy = Optional.empty();
    }

    @Override
    public String toString()
    {
        return String.format("%s{%s=%s}", SimpleDecodeMaybeProxy.class.getName(), isProxy()? "proxy" : "object",
                isProxy()? proxy.get() : resolvedObject.get());
    }
}
