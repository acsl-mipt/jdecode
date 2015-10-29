package ru.mipt.acsl.decode.model.domain.impl.proxy;

import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.impl.SimpleDecodeResolvingResult;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.type.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Artem Shein
 */
public final class SimpleDecodeDomainModelResolver implements DecodeDomainModelResolver
{
    public static DecodeDomainModelResolver newInstance()
    {
        return new SimpleDecodeDomainModelResolver();
    }

    private SimpleDecodeDomainModelResolver()
    {

    }

    @Override
    public DecodeResolvingResult<DecodeReferenceable> resolve(@NotNull DecodeRegistry registry)
    {
        return registry.getRootNamespaces().stream().flatMap(namespace -> resolve(namespace, registry))
                .reduce(SimpleDecodeResolvingResult.newInstance(Optional.<DecodeReferenceable>empty()),
                        SimpleDecodeResolvingResult::merge);
    }

    @NotNull
    private static Stream<DecodeResolvingResult<DecodeReferenceable>> resolve(
            @NotNull DecodeNamespace namespace,
            @NotNull DecodeRegistry registry)
    {
        List<DecodeResolvingResult<DecodeReferenceable>> resultList = new ArrayList<>();
        resultList.addAll(namespace.getTypes().stream().map(type -> resolve(type, registry))
                .collect(Collectors.toList()));
        resultList.addAll(namespace.getSubNamespaces().stream().flatMap(
                subNamespace -> resolve(subNamespace, registry))
                .collect(Collectors.toList()));
        resultList.addAll(namespace.getComponents().stream().flatMap(component -> resolve(component, registry)).collect(
                Collectors.toList()));
        return resultList.stream();
    }

    @NotNull
    private static Stream<DecodeResolvingResult<DecodeReferenceable>> resolve(
            @NotNull DecodeComponent component,
            @NotNull DecodeRegistry registry)
    {
        List<DecodeResolvingResult<DecodeReferenceable>> resultList = new ArrayList<>();
        if (component.getBaseType().isPresent())
        {
            resultList.add(resolveWithTypeCheck(component.getBaseType().get(), registry, DecodeType.class));
            if (component.getBaseType().get().isResolved())
            {
                resultList.add(resolve(component.getBaseType().get().getObject(), registry));
            }
        }
        for (DecodeCommand command : component.getCommands())
        {
            for (DecodeCommandArgument argument : command.getArguments())
            {
                resultList.add(resolveWithTypeCheck(argument.getType(), registry, DecodeType.class));
                if (argument.getUnit().isPresent())
                {
                    resultList.add(resolveWithTypeCheck(argument.getUnit().get(), registry, DecodeUnit.class));
                }
            }
        }
        for (DecodeComponentRef subComponentRef : component.getSubComponents())
        {
            DecodeMaybeProxy<DecodeComponent> subComponent = subComponentRef.getComponent();
            resultList.add(resolveWithTypeCheck(subComponent, registry, DecodeComponent.class));
            if (subComponent.isResolved())
            {
                resultList.addAll(resolve(subComponent.getObject(), registry).collect(Collectors.toList()));
            }
        }
        return resultList.stream();
    }

    @SuppressWarnings("unchecked")
    private static <T extends DecodeReferenceable> DecodeResolvingResult<DecodeReferenceable>
    resolveWithTypeCheck(
            @NotNull DecodeMaybeProxy<T> maybeProxy, @NotNull DecodeRegistry registry,
            @NotNull Class<T> cls)
    {
        // it's Java...
        return (DecodeResolvingResult<DecodeReferenceable>)maybeProxy.resolve(registry, cls);
    }

    @NotNull
    private static DecodeResolvingResult<DecodeReferenceable> resolve(
            @NotNull DecodeType type,
            @NotNull DecodeRegistry registry)
    {
        List<DecodeResolvingResult<DecodeReferenceable>> resolvingResultList = new ArrayList<>();
        type.accept(new DecodeTypeResolveVisitor(registry, resolvingResultList));
        return resolvingResultList.stream().reduce(SimpleDecodeResolvingResult.newInstance(
                        Optional.<DecodeReferenceable>empty()),
                SimpleDecodeResolvingResult::merge);
    }

    private static class DecodeTypeResolveVisitor implements DecodeTypeVisitor<Void>
    {
        @NotNull
        private final DecodeRegistry registry;
        @NotNull
        private final List<DecodeResolvingResult<DecodeReferenceable>> resolvingResultList;

        public DecodeTypeResolveVisitor(@NotNull DecodeRegistry registry,
                                        @NotNull List<DecodeResolvingResult<DecodeReferenceable>> resolvingResultList)
        {
            this.registry = registry;
            this.resolvingResultList = resolvingResultList;
        }

        @Override
        public Void visit(@NotNull DecodePrimitiveType baseType)
        {
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeNativeType nativeType)
        {
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeSubType subType)
        {
            resolvingResultList.add(resolveWithTypeCheck(subType.getBaseType(), registry, DecodeType.class));
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeEnumType enumType)
        {
            resolvingResultList.add(resolveWithTypeCheck(enumType.getBaseType(), registry, DecodeType.class));
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeArrayType arrayType)
        {
            resolvingResultList.add(resolveWithTypeCheck(arrayType.getBaseType(), registry, DecodeType.class));
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeStructType structType)
        {
            for (DecodeStructField field : structType.getFields())
            {
                resolvingResultList.add(resolveWithTypeCheck(field.getType(), registry, DecodeType.class));
                if (!field.getType().isProxy())
                {
                    resolvingResultList.add(resolve(field.getType().getObject(), registry));
                }
                if (field.getUnit().isPresent())
                {
                    resolvingResultList.add(resolveWithTypeCheck(field.getUnit().get(), registry, DecodeUnit.class));
                }
            }
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeAliasType typeAlias)
        {
            resolvingResultList.add(resolveWithTypeCheck(typeAlias.getType(), registry, DecodeType.class));
            return null;
        }
    }
}
