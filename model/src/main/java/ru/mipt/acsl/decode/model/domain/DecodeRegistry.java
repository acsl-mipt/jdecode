package ru.mipt.acsl.decode.model.domain;

import ru.mipt.acsl.decode.model.domain.impl.DecodeName;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessage;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author Artem Shein
 */
public interface DecodeRegistry
{
    @NotNull
    List<DecodeNamespace> getRootNamespaces();

    @NotNull
    <T extends DecodeReferenceable> DecodeResolvingResult<T> resolve(@NotNull URI uri, @NotNull Class<T> cls);

    @NotNull
    default Optional<DecodeComponent> getComponent(@NotNull String fqn)
    {
        int dotPos = fqn.lastIndexOf('.');
        Optional<DecodeNamespace> namespaceOptional = getNamespace(fqn.substring(0, dotPos));
        if (!namespaceOptional.isPresent())
        {
            return Optional.empty();
        }
        IDecodeName componentName = DecodeName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()));
        return namespaceOptional.get().getComponents().stream().filter((c) -> c.getName().equals(componentName)).findAny();
    }

    @NotNull
    default Optional<DecodeNamespace> getNamespace(@NotNull String fqn)
    {
        List<DecodeNamespace> currentNamespaces = getRootNamespaces();
        Optional<DecodeNamespace> currentNamespace = Optional.empty();
        for (String namespaceName : fqn.split(Pattern.quote(".")))
        {
            if (currentNamespaces == null)
            {
                return Optional.empty();
            }
            IDecodeName decodeName = DecodeName.newFromMangledName(namespaceName);
            currentNamespace = currentNamespaces.stream().filter((n) -> n.getName().equals(decodeName)).findAny();
            currentNamespaces = currentNamespace.isPresent() ? currentNamespace.get().getSubNamespaces() : null;
        }
        return currentNamespace;
    }

    @NotNull
    default Optional<DecodeMessage> getMessage(@NotNull String fqn)
    {
        int dotPos = fqn.lastIndexOf('.');
        IDecodeName decodeName = DecodeName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()));
        return getComponent(fqn.substring(0, dotPos)).map((c) -> c.getMessages().stream().filter((m) -> m.getName().equals(decodeName)).findAny().orElse(null));
    }

    @NotNull
    default DecodeMessage getMessageOrThrow(@NotNull String fqn)
    {
        return getMessage(fqn).get();
    }

}
