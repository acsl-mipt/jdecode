package ru.mipt.acsl.decode.model.domain.impl.proxy;

import com.google.common.base.Preconditions;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.impl.type.SimpleDecodeArrayType;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeProxyResolver;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import ru.mipt.acsl.decode.model.domain.type.DecodeArrayType;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.stream.Stream.concat;
import static ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy.proxy;

/**
 * @author Artem Shein
 */
public class FindExistingDecodeProxyResolver implements DecodeProxyResolver
{
    public static final String INVALID_MANGLED_ARRAY_NAME = "invalid mangled array name";

    @NotNull
    @Override
    public <T extends DecodeReferenceable> DecodeResolvingResult<T> resolve(@NotNull DecodeRegistry registry,
                                                                            @NotNull URI uri, @NotNull Class<T> cls)
    {
        Iterator<String> iter = DecodeUtils.getUriParts(uri).iterator();
        DecodeResolvingResult<DecodeReferenceable> current = SimpleDecodeResolvingResult.immutableEmpty();
        while (iter.hasNext())
        {
            String part = iter.next();
            Optional<DecodeReferenceable> resolvedObject = current.getResolvedObject();
            if (resolvedObject.isPresent())
            {
                current = Preconditions.checkNotNull(resolvedObject.get().accept(
                        new ResolveDecodeReferenceableVisitor(registry,
                        DecodeNameImpl.newFromMangledName(part))));
            }
            else
            {
                current = SimpleDecodeResolvingResult.newInstance(registry.getRootNamespaces().stream()
                        .filter(n -> n.getName().asString().equals(part)).findAny()
                        .map(v -> (DecodeReferenceable) v));
            }
            if (!current.getResolvedObject().isPresent())
            {
                return SimpleDecodeResolvingResult.newInstance(Optional.empty());
            }
        }
        return SimpleDecodeResolvingResult
                .newInstance(current.getResolvedObject().map(o -> cls.isInstance(o) ? cls.cast(o) : null));
    }

    private static class ResolveDecodeReferenceableVisitor
            implements DecodeReferenceableVisitor<DecodeResolvingResult<DecodeReferenceable>>
    {
        @NotNull
        private final DecodeRegistry registry;
        @NotNull
        private final DecodeNameImpl part;

        public ResolveDecodeReferenceableVisitor(@NotNull DecodeRegistry registry, @NotNull DecodeNameImpl part)
        {
            this.registry = registry;
            this.part = part;
        }

        @Override
        @NotNull
        public DecodeResolvingResult<DecodeReferenceable> visit(@NotNull DecodeNamespace namespace)
        {
            Optional<DecodeReferenceable> resolvedObject = concat(
                    concat(
                            concat(namespace.getSubNamespaces().stream(), namespace.getUnits().stream()),
                            namespace.getTypes().stream()),
                    namespace.getComponents().stream())
                    .filter(n -> n.getOptionalName().map(name -> name.equals(part)).orElse(false)).findAny();
            if (resolvedObject.isPresent())
            {
                return SimpleDecodeResolvingResult.newInstance(resolvedObject);
            }
            String partString = part.asString();
            if (partString.startsWith("["))
            {
                Preconditions.checkState(partString.endsWith("]"), INVALID_MANGLED_ARRAY_NAME);
                String innerPart = partString.substring(1, partString.length() - 1);
                int index = innerPart.lastIndexOf(",");
                long minLength = 0, maxLength = 0;
                if (index != -1)
                {
                    String sizePart = innerPart.substring(index + 1);
                    if (sizePart.contains(".."))
                    {
                        String[] parts = sizePart.split(Pattern.quote(".."));
                        minLength = Long.parseLong(parts[0]);
                        maxLength = "*".equals(parts[1]) ? 0 : Long.parseLong(parts[1]);
                    }
                    else
                    {
                        minLength = maxLength = Long.parseLong(sizePart);
                    }
                }
                final long finalMinLength = minLength;
                final long finalMaxLength = maxLength;
                DecodeArrayType newArrayType = namespace.getTypes().stream().filter(t -> t.getOptionalName().map(on -> on.equals(part)).orElse(false))
                        .filter(DecodeArrayType.class::isInstance).map(DecodeArrayType.class::cast).findAny()
                        .orElseGet(() -> SimpleDecodeArrayType.newInstance(
                                Optional.of(part),
                                namespace,
                                proxy(namespace.getFqn(),
                                        DecodeNameImpl.newFromSourceName(
                                                innerPart.substring(0, index == -1 ? innerPart.length() : index))),
                                Optional.<String>empty(),
                                ImmutableArraySize.newInstance(finalMinLength, finalMaxLength)));
                DecodeResolvingResult<DecodeType> resolvedBaseType = newArrayType.getBaseType()
                        .resolve(registry, DecodeType.class);
                if (resolvedBaseType.getResolvedObject().isPresent())
                {
                    List<DecodeType> types = new ArrayList<>(namespace.getTypes());
                    types.add(newArrayType);
                    namespace.setTypes(types);
                    return SimpleDecodeResolvingResult.newInstance(Optional.of(newArrayType));
                }
            }
            return SimpleDecodeResolvingResult.immutableEmpty();
        }

        @Override
        @NotNull
        public DecodeResolvingResult<DecodeReferenceable> visit(@NotNull DecodeType type)
        {
            return SimpleDecodeResolvingResult.immutableEmpty();
        }

        @Override
        @NotNull
        public DecodeResolvingResult<DecodeReferenceable> visit(@NotNull DecodeComponent component)
        {
            return SimpleDecodeResolvingResult.immutableEmpty();
        }

        @Override
        @NotNull
        public DecodeResolvingResult<DecodeReferenceable> visit(@NotNull DecodeUnit unit)
        {
            return SimpleDecodeResolvingResult.immutableEmpty();
        }

        @Override
        public DecodeResolvingResult<DecodeReferenceable> visit(@NotNull DecodeLanguage language)
        {
            return SimpleDecodeResolvingResult.immutableEmpty();
        }
    }
}
