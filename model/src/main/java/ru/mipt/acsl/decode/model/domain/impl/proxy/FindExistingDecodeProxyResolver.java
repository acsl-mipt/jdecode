package ru.mipt.acsl.decode.model.domain.impl.proxy;

import com.google.common.base.Preconditions;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.impl.type.ArraySizeImpl;
import ru.mipt.acsl.decode.model.domain.impl.type.DecodeArrayTypeImpl;
import org.jetbrains.annotations.NotNull;
import scala.Function1;
import scala.Option;
import scala.collection.Seq;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Buffer;

import java.net.URI;
import java.util.Iterator;
import java.util.Optional;
import java.util.regex.Pattern;

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
            Option<DecodeReferenceable> resolvedObject = current.resolvedObject();
            if (resolvedObject.isDefined())
            {
                current = Preconditions.checkNotNull(resolvedObject.get().accept(
                        new ResolveDecodeReferenceableVisitor(registry,
                        DecodeNameImpl.newFromMangledName(part))));
            }
            else
            {
                current = SimpleDecodeResolvingResult.newInstance(registry.rootNamespaces()
                        .find(n -> n.name().asString().equals(part))
                        .map(DecodeReferenceable.class::cast));
            }
            if (!current.resolvedObject().isDefined())
            {
                return SimpleDecodeResolvingResult.newInstance(Option.empty());
            }
        }
        return SimpleDecodeResolvingResult
                .newInstance(current.resolvedObject().map(o -> cls.isInstance(o) ? cls.cast(o) : null));
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
            Function1<DecodeReferenceable, Object> pred = (DecodeReferenceable nameAware) -> {
                Option<DecodeName> nameOption = nameAware.optionalName();
                return nameOption.isDefined() && nameOption.get().equals(part);
            };
            Option<DecodeReferenceable> resolvedObject = ((Seq<DecodeReferenceable>) (Seq<?>) namespace.subNamespaces()).find(pred);
            resolvedObject = resolvedObject.orElse(() -> ((Seq<DecodeReferenceable>) (Seq<?>) namespace.units()).find(pred));
            resolvedObject = resolvedObject.orElse(() -> ((Seq<DecodeReferenceable>) (Seq<?>) namespace.types()).find(pred));
            resolvedObject = resolvedObject.orElse(() -> ((Seq<DecodeReferenceable>) (Seq<?>) namespace.components()).find(pred));
            if (resolvedObject.isDefined())
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
                DecodeArrayType newArrayType = namespace.types().find(t -> {
                    Option<DecodeName> nameOption = t.optionalName();
                    return nameOption.isDefined() && nameOption.get().equals(part) && (t instanceof DecodeArrayType);
                }).getOrElse(() -> new DecodeArrayTypeImpl(
                                Option.apply(part),
                                namespace,
                                Option.empty(),
                                proxy(namespace.fqn(),
                                        DecodeNameImpl.newFromSourceName(
                                                innerPart.substring(0, index == -1 ? innerPart.length() : index))),
                                new ArraySizeImpl(finalMinLength, finalMaxLength)));
                DecodeResolvingResult<DecodeType> resolvedBaseType = newArrayType.baseType()
                        .resolve(registry, DecodeType.class);
                if (resolvedBaseType.resolvedObject().isDefined())
                {
                    Buffer<DecodeType> types = new ArrayBuffer<>();
                    types.append(namespace.types());
                    types.$plus$eq(newArrayType);
                    namespace.types_$eq(types);
                    return SimpleDecodeResolvingResult.newInstance(Option.apply(newArrayType));
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
