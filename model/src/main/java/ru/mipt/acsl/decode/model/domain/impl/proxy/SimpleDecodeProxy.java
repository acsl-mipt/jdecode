package ru.mipt.acsl.decode.model.domain.impl.proxy;

import com.google.common.base.Charsets;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class SimpleDecodeProxy<T extends DecodeReferenceable> implements DecodeProxy<T>
{
    @NotNull
    private final URI uri;

    public static <T extends DecodeReferenceable> SimpleDecodeProxy<T> newInstance(@NotNull URI uri)
    {
        return new SimpleDecodeProxy<>(uri);
    }

    @NotNull
    public static <T extends DecodeReferenceable> DecodeProxy<T> newInstanceFromTypeUriString(@NotNull String typeUriString, @NotNull DecodeFqn defaultNsFqn)
    {
        return newInstance(URI.create((typeUriString.startsWith("/")? typeUriString : "/" + defaultNsFqn.getParts().stream().map(
                IDecodeName::asString).collect(Collectors.joining("/")) + typeUriString)));
    }

    private SimpleDecodeProxy(@NotNull URI uri)
    {
        this.uri = uri;
    }

    @Override
    public DecodeResolvingResult<T> resolve(@NotNull DecodeRegistry registry, @NotNull Class<T> cls)
    {
        return registry.resolve(uri, cls);
    }

    @NotNull
    @Override
    public URI getUri()
    {
        return uri;
    }

    @Override
    public String toString()
    {
        try
        {
            return String.format("%s{uri=%s}", SimpleDecodeProxy.class.getName(),
                    URLDecoder.decode(uri.toString(), Charsets.UTF_8.name()));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError(e);
        }
    }
}
