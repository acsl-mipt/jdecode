package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.impl.DecodeNameImpl;
import scala.Option;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Buffer;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * @author Artem Shein
 */
public interface DecodeRegistry
{
    @NotNull
    Buffer<DecodeNamespace> rootNamespaces();

    @NotNull
    <T extends DecodeReferenceable> DecodeResolvingResult<T> resolve(@NotNull URI uri, @NotNull Class<T> cls);

    @NotNull
    default Option<DecodeComponent> getComponent(@NotNull String fqn)
    {
        int dotPos = fqn.lastIndexOf('.');
        Option<DecodeNamespace> namespaceOptional = getNamespace(fqn.substring(0, dotPos));
        if (!namespaceOptional.isDefined())
        {
            return Option.empty();
        }
        DecodeName componentName = DecodeNameImpl.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()));
        return namespaceOptional.get().components().find((c) -> c.name().equals(componentName));
    }

    @NotNull
    default Option<DecodeNamespace> getNamespace(@NotNull String fqn)
    {
        Buffer<DecodeNamespace> currentNamespaces = new ArrayBuffer<>();
        currentNamespaces.append(rootNamespaces());
        Option<DecodeNamespace> currentNamespace = Option.empty();
        for (String namespaceName : fqn.split(Pattern.quote(".")))
        {
            if (currentNamespaces == null)
            {
                return Option.empty();
            }
            DecodeName decodeName = DecodeNameImpl.newFromMangledName(namespaceName);
            currentNamespace = currentNamespaces.find((n) -> n.name().equals(decodeName));
            if (currentNamespace.isDefined())
            {
                currentNamespaces.clear();
                currentNamespaces.append(currentNamespace.get().subNamespaces());
            }
            else
            {
                currentNamespaces = null;
            }
        }
        return currentNamespace;
    }

    @NotNull
    default Option<DecodeMessage> getMessage(@NotNull String fqn)
    {
        int dotPos = fqn.lastIndexOf('.');
        DecodeName decodeName = DecodeNameImpl.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()));
        return getComponent(fqn.substring(0, dotPos)).map((c) -> c.messages().find((m) -> m.name().equals(decodeName)).getOrElse(() -> null));
    }

    @NotNull
    default DecodeMessage getMessageOrThrow(@NotNull String fqn)
    {
        return getMessage(fqn).get();
    }

}
