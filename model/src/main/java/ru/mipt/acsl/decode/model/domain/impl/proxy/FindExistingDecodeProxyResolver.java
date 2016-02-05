package ru.mipt.acsl.decode.model.domain.impl.proxy;

import com.google.common.base.Preconditions;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.impl.type.ArraySizeImpl;
import ru.mipt.acsl.decode.model.domain.impl.type.DecodeArrayTypeImpl;
import org.jetbrains.annotations.NotNull;
import scala.Option;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Buffer;

import java.net.URI;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static ru.mipt.acsl.JavaToScala.asOption;
import static ru.mipt.acsl.ScalaToJava.asOptional;
import static ru.mipt.acsl.decode.ScalaUtil.append;
import static ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy.proxy;
import static scala.collection.JavaConversions.asJavaCollection;

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
                current = SimpleDecodeResolvingResult.newInstance(Option.apply(
                        asJavaCollection(registry.rootNamespaces()).stream()
                        .filter(n -> n.name().asMangledString().equals(part)).findAny()
                        .map(DecodeReferenceable.class::cast).orElse(null)));
            }
            if (!current.resolvedObject().isDefined())
            {
                return SimpleDecodeResolvingResult.newInstance(Option.empty());
            }
        }
        return SimpleDecodeResolvingResult
                .newInstance(asOption(asOptional(current.resolvedObject()).filter(cls::isInstance).map(cls::cast)));
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
            Predicate<DecodeReferenceable> pred = (DecodeReferenceable nameAware) -> {
                Option<DecodeName> nameOption = nameAware.optionName();
                return nameOption.isDefined() && nameOption.get().equals(part);
            };
            Option<DecodeReferenceable> resolvedObject = asOption(Stream.concat(asJavaCollection(namespace.subNamespaces()).stream(),
                    Stream.concat(asJavaCollection(namespace.units()).stream(),
                            Stream.concat(asJavaCollection(namespace.types()).stream(),
                                asJavaCollection(namespace.components()).stream()))).filter(pred).findAny());
            if (resolvedObject.isDefined())
            {
                return SimpleDecodeResolvingResult.newInstance(resolvedObject);
            }
            String partString = part.asMangledString();
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
                DecodeArrayType newArrayType = asJavaCollection(namespace.types()).stream().filter(t -> {
                    Option<DecodeName> nameOption = t.optionName();
                    return nameOption.isDefined() && nameOption.get().equals(part) && (t instanceof DecodeArrayType);
                }).findAny().map(DecodeArrayType.class::cast).orElseGet(() -> new DecodeArrayTypeImpl(
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
                    namespace.types_$eq(append(namespace.types(), newArrayType));
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
