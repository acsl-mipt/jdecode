package ru.mipt.acsl.decode.model.domain.impl.proxy;

import com.google.common.base.Preconditions;
import ru.mipt.acsl.decode.ScalaUtil;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.impl.type.ArraySizeImpl;
import ru.mipt.acsl.decode.model.domain.impl.type.ArrayTypeImpl;
import org.jetbrains.annotations.NotNull;
import scala.Option;

import java.net.URI;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static ru.mipt.acsl.JavaToScala.asOption;
import static ru.mipt.acsl.ScalaToJava.asOptional;
import static ru.mipt.acsl.decode.ScalaUtil.append;
import static scala.collection.JavaConversions.asJavaCollection;
import static scala.collection.JavaConversions.asScalaBuffer;

/**
 * @author Artem Shein
 */
public class FindExistingDecodeProxyResolver implements DecodeProxyResolver
{
    public static final String INVALID_MANGLED_ARRAY_NAME = "invalid mangled array name";

    @NotNull
    @Override
    public <T extends Referenceable> ResolvingResult<T> resolve(@NotNull Registry registry,
                                                                @NotNull URI uri, @NotNull Class<T> cls)
    {
        scala.collection.Iterator<String> iter = DecodeUtils.getUriParts(uri).iterator();
        ResolvingResult<Referenceable> current = ResolvingResult.empty();
        while (iter.hasNext())
        {
            String part = iter.next();
            Option<Referenceable> resolvedObject = current.resolvedObject();
            if (resolvedObject.isDefined())
            {
                current = Preconditions.checkNotNull(resolvedObject.get().accept(
                        new ResolveDecodeReferenceableVisitor(registry,
                        DecodeNameImpl.newFromMangledName(part))));
            }
            else
            {
                current = ResolvingResult.apply(Option.apply(
                        asJavaCollection(registry.rootNamespaces()).stream()
                        .filter(n -> n.name().asMangledString().equals(part)).findAny()
                        .map(Referenceable.class::cast).orElse(null)), ScalaUtil.newSeq());
            }
            if (!current.resolvedObject().isDefined())
            {
                return ResolvingResult.apply(Option.empty(), ScalaUtil.newSeq());
            }
        }
        return ResolvingResult
                .apply(asOption(asOptional(current.resolvedObject()).filter(cls::isInstance).map(cls::cast)),
                        ScalaUtil.newSeq());
    }



    private static class ResolveDecodeReferenceableVisitor
            implements DecodeReferenceableVisitor<ResolvingResult<Referenceable>>
    {
        @NotNull
        private final Registry registry;
        @NotNull
        private final DecodeNameImpl part;

        public ResolveDecodeReferenceableVisitor(@NotNull Registry registry, @NotNull DecodeNameImpl part)
        {
            this.registry = registry;
            this.part = part;
        }

        @Override
        @NotNull
        public ResolvingResult<Referenceable> visit(@NotNull Namespace namespace)
        {
            Predicate<Referenceable> pred = (Referenceable nameAware) -> {
                Option<DecodeName> nameOption = nameAware.optionName();
                return nameOption.isDefined() && nameOption.get().equals(part);
            };
            Option<Referenceable> resolvedObject = asOption(Stream.concat(asJavaCollection(namespace.subNamespaces()).stream(),
                    Stream.concat(asJavaCollection(namespace.units()).stream(),
                            Stream.concat(asJavaCollection(namespace.types()).stream(),
                                asJavaCollection(namespace.components()).stream()))).filter(pred).findAny());
            if (resolvedObject.isDefined())
            {
                return ResolvingResult.apply(resolvedObject, ScalaUtil.newSeq());
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
                ArrayType newArrayType = asJavaCollection(namespace.types()).stream().filter(t -> {
                    Option<DecodeName> nameOption = t.optionName();
                    return nameOption.isDefined() && nameOption.get().equals(part) && (t instanceof ArrayType);
                }).findAny().map(ArrayType.class::cast).orElseGet(() -> new ArrayTypeImpl(
                                Option.apply(part),
                                namespace,
                                Option.empty(),
                        MaybeProxy$.MODULE$.proxy(namespace.fqn(),
                                        DecodeNameImpl.newFromSourceName(
                                                innerPart.substring(0, index == -1 ? innerPart.length() : index))),
                                new ArraySizeImpl(finalMinLength, finalMaxLength)));
                ResolvingResult<DecodeType> resolvedBaseType = newArrayType.baseType()
                        .resolve(registry, DecodeType.class);
                if (resolvedBaseType.resolvedObject().isDefined())
                {
                    namespace.types_$eq(append(namespace.types(), newArrayType));
                    return ResolvingResult.apply(Option.apply(newArrayType), ScalaUtil.newSeq());
                }
            }
            return ResolvingResult.empty();
        }

        @Override
        @NotNull
        public ResolvingResult<Referenceable> visit(@NotNull DecodeType type)
        {
            return ResolvingResult.empty();
        }

        @Override
        @NotNull
        public ResolvingResult<Referenceable> visit(@NotNull Component component)
        {
            return ResolvingResult.empty();
        }

        @Override
        @NotNull
        public ResolvingResult<Referenceable> visit(@NotNull Measure measure)
        {
            return ResolvingResult.empty();
        }

        @Override
        public ResolvingResult<Referenceable> visit(@NotNull Language language)
        {
            return ResolvingResult.empty();
        }
    }
}
