package ru.mipt.acsl.decode.model.domain.impl.proxy;

import com.google.common.base.Preconditions;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.impl.type.ImmutableDecodeBerType;
import ru.mipt.acsl.decode.model.domain.impl.type.ImmutableDecodeGenericTypeSpecialized;
import ru.mipt.acsl.decode.model.domain.impl.type.SimpleDecodePrimitiveType;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeProxyResolver;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import ru.mipt.acsl.decode.model.domain.type.*;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Artem Shein
 */
public class ProvidePrimitivesAndNativeTypesDecodeProxyResolver implements DecodeProxyResolver
{
    @NotNull
    private final Map<DecodeName, DecodeType> primitiveOrNativeTypeByNameMap = new HashMap<>();
    @NotNull
    private final Map<String, DecodeGenericTypeSpecialized> genericTypeSpecializedByTypeNameMap = new HashMap<>();

    @NotNull
    @Override
    public <T extends DecodeReferenceable> DecodeResolvingResult<T> resolve(@NotNull DecodeRegistry registry,
                                                                            @NotNull URI uri, @NotNull Class<T> cls)
    {
        List<String> parts = DecodeUtils.getUriParts(uri);
        if (parts.size() == 2 && parts.get(0).equals(DecodeConstants.SYSTEM_NAMESPACE_FQN.asString()))
        {
            Optional<DecodeNamespace> namespaceOptional = DecodeUtils.getNamespaceByFqn(registry,
                    DecodeConstants.SYSTEM_NAMESPACE_FQN);
            Preconditions.checkState(namespaceOptional.isPresent(), "system namespace not found");
            String typeName = parts.get(1);
            // Primitive type
            if (typeName.contains(":"))
            {
                int index = typeName.indexOf(":");
                String typeKind = typeName.substring(0, index);
                long bitLength = Long.parseLong(typeName.substring(index + 1));
                Optional<DecodeType.TypeKind> typeKindOptional = DecodeType.TypeKind.forName(typeKind);
                if (typeKindOptional.isPresent())
                {
                    DecodeName name = ImmutableDecodeName.newInstanceFromMangledName(typeName);
                    DecodeNamespace namespace = namespaceOptional.get();
                    DecodeType nativeOrPrimitiveType = primitiveOrNativeTypeByNameMap.computeIfAbsent(name,
                            (nameKey) -> SimpleDecodePrimitiveType
                                    .newInstance(Optional.of(nameKey), namespace, typeKindOptional.get(),
                                            bitLength,
                                            Optional.<String>empty()));
                    Preconditions.checkState(nativeOrPrimitiveType instanceof DecodePrimitiveType);
                    DecodePrimitiveType primitiveType = (DecodePrimitiveType) nativeOrPrimitiveType;
                    Optional<DecodeName> primitiveTypeOptionalName = primitiveType.getOptionalName();
                    if (primitiveTypeOptionalName.isPresent()
                            && !namespace.getTypes().stream()
                            .filter(t -> t.getOptionalName().equals(primitiveTypeOptionalName))
                            .findAny().isPresent())
                    {
                        List<DecodeType> types = new ArrayList<>();
                        types.add(primitiveType);
                        namespace.setTypes(types);
                    }
                    return SimpleDecodeResolvingResult.newInstance(Optional.of(primitiveType)
                            .filter(cls::isInstance).map(cls::cast));
                }
            }
            // Generic type
            else if (typeName.contains("<"))
            {
                int genericStartIndex = typeName.indexOf('<');
                int genericEndIndex = typeName.lastIndexOf('>');
                Preconditions.checkState(genericEndIndex != -1, "invalid generic type");
                String genericTypeName = typeName.substring(0, genericStartIndex);
                String genericArgumentsString = typeName.substring(genericStartIndex + 1, genericEndIndex);
                Preconditions.checkState(
                        !genericArgumentsString.contains("<")
                                && !genericArgumentsString.contains(">")
                                && !genericArgumentsString.contains("[")
                                && !genericArgumentsString.contains("]"), "not implemented");
                Optional<DecodeMaybeProxy<DecodeGenericType>> genericTypeOptional =
                        (Optional<DecodeMaybeProxy<DecodeGenericType>>) (Optional<?>) DecodeUtils.uriToOptionalMaybeProxyType(genericTypeName);
                Preconditions.checkState(genericTypeOptional.isPresent(), "invalid generic type");
                DecodeResolvingResult<?> result = genericTypeOptional.get().resolve(registry, DecodeGenericType.class);
                if (result.hasError())
                {
                    return (DecodeResolvingResult<T>) result;
                }
                return SimpleDecodeResolvingResult
                        .newInstance(Optional.of(genericTypeSpecializedByTypeNameMap.computeIfAbsent(typeName,
                                tn -> ImmutableDecodeGenericTypeSpecialized.newInstance(Optional.empty(),
                                        genericTypeOptional.get().getObject().getNamespace(), Optional.empty(),
                                        genericTypeOptional.get(), Stream.of(typeName.split(Pattern.quote(",")))
                                                .map(DecodeUtils::uriToOptionalMaybeProxyType)
                                                .collect(Collectors.toList())))).filter(cls::isInstance)
                                .map(cls::cast));
            }
            // Native type
            else if (DecodeNativeType.MANGLED_TYPE_NAMES.contains(typeName))
            {
                DecodeName name = ImmutableDecodeName.newInstanceFromMangledName(typeName);
                DecodeNamespace namespace = namespaceOptional.get();
                DecodeType nativeOrPrimitiveType = primitiveOrNativeTypeByNameMap.computeIfAbsent(name,
                        (nameKey) -> {
                            if (nameKey.equals(ImmutableDecodeBerType.MANGLED_NAME))
                            {
                                return ImmutableDecodeBerType
                                        .newInstance(Optional.of(nameKey), namespace, Optional.<String>empty());
                            }
                            throw new AssertionError();
                        });
                Preconditions.checkState(nativeOrPrimitiveType instanceof DecodeNativeType);
                DecodeNativeType nativeType = (DecodeNativeType) nativeOrPrimitiveType;
                Optional<DecodeName> nativeTypeOptionalName = nativeType.getOptionalName();
                if (nativeTypeOptionalName.isPresent()
                        && !namespace.getTypes().stream().filter(t -> t.getOptionalName().equals(
                        nativeTypeOptionalName))
                        .findAny().isPresent())
                {
                    List<DecodeType> types = new ArrayList<>();
                    types.add(nativeType);
                    namespace.setTypes(types);
                }
                return SimpleDecodeResolvingResult.newInstance(Optional.of(nativeType)
                        .filter(cls::isInstance).map(cls::cast));
            }
        }
        return SimpleDecodeResolvingResult.immutableEmpty();
    }
}
