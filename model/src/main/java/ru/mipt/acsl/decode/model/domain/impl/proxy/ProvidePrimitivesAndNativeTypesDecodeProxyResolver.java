package ru.mipt.acsl.decode.model.domain.impl.proxy;

import com.google.common.base.Preconditions;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.impl.type.*;
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
    private final Map<DecodeName, DecodeType> knownTypeByNameMap = new HashMap<>();
    @NotNull
    private final Map<String, DecodeGenericTypeSpecialized> genericTypeSpecializedByTypeNameMap = new HashMap<>();

    @NotNull
    @Override
    public <T extends DecodeReferenceable> DecodeResolvingResult<T> resolve(@NotNull DecodeRegistry registry,
                                                                            @NotNull URI uri, @NotNull Class<T> cls)
    {
        List<String> parts = DecodeUtils.getUriParts(uri);
        Preconditions.checkState(DecodeConstants.SYSTEM_NAMESPACE_FQN.size() == 1, "not implemented");
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
                    DecodeName name = DecodeName.newFromMangledName(typeName);
                    DecodeNamespace namespace = namespaceOptional.get();
                    DecodeType nativeOrPrimitiveType = knownTypeByNameMap.computeIfAbsent(name,
                            (nameKey) -> SimpleDecodePrimitiveType
                                    .newInstance(Optional.of(nameKey), namespace, typeKindOptional.get(),
                                            bitLength,
                                            Optional.<String>empty()));
                    Preconditions.checkState(nativeOrPrimitiveType instanceof DecodePrimitiveType);
                    DecodePrimitiveType primitiveType = (DecodePrimitiveType) nativeOrPrimitiveType;
                    Optional<IDecodeName> primitiveTypeOptionalName = primitiveType.getOptionalName();
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
                        (Optional<DecodeMaybeProxy<DecodeGenericType>>) (Optional<?>) DecodeUtils.uriToOptionalMaybeProxyType("/" + DecodeConstants.SYSTEM_NAMESPACE_FQN.asString() + "/" + genericTypeName);
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
                                        genericTypeOptional.get(), Stream.of(genericArgumentsString.split(Pattern.quote(",")))
                                                .map(DecodeUtils::uriToOptionalMaybeProxyType)
                                                .collect(Collectors.toList())))).filter(cls::isInstance)
                                .map(cls::cast));
            }
            // Native type
            else if (DecodeNativeType.MANGLED_TYPE_NAMES.contains(typeName))
            {
                DecodeName name = DecodeName.newFromMangledName(typeName);
                DecodeNamespace namespace = namespaceOptional.get();
                DecodeType knownType = knownTypeByNameMap.computeIfAbsent(name,
                        (nameKey) -> {
                            if (nameKey.equals(ImmutableDecodeBerType.MANGLED_NAME))
                            {
                                return ImmutableDecodeBerType
                                        .newInstance(Optional.of(nameKey), namespace, Optional.<String>empty());
                            }
                            else if (nameKey.equals(DecodeOptionalType.MANGLED_NAME()))
                            {
                                return new DecodeOptionalType(Optional.of(nameKey), namespace, Optional.empty());
                            }
                            else if (nameKey.equals(ImmutableDecodeOrType.MANGLED_NAME))
                            {
                                return ImmutableDecodeOrType.newInstance(Optional.of(nameKey), namespace, Optional.empty());
                            }
                            throw new AssertionError();
                        });
                Optional<IDecodeName> nativeTypeOptionalName = knownType.getOptionalName();
                if (nativeTypeOptionalName.isPresent()
                        && !namespace.getTypes().stream().filter(t -> t.getOptionalName().equals(
                        nativeTypeOptionalName))
                        .findAny().isPresent())
                {
                    List<DecodeType> types = new ArrayList<>();
                    types.add(knownType);
                    namespace.setTypes(types);
                }
                return SimpleDecodeResolvingResult.newInstance(Optional.of(knownType)
                        .filter(cls::isInstance).map(cls::cast));
            }
        }
        return SimpleDecodeResolvingResult.immutableEmpty();
    }
}
