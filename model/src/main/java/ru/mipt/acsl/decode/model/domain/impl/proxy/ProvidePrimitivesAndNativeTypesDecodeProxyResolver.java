package ru.mipt.acsl.decode.model.domain.impl.proxy;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeProxyResolver;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import ru.mipt.acsl.decode.model.domain.type.DecodeNativeType;
import ru.mipt.acsl.decode.model.domain.type.DecodePrimitiveType;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;

/**
 * @author Artem Shein
 */
public class ProvidePrimitivesAndNativeTypesDecodeProxyResolver implements DecodeProxyResolver
{
    @NotNull
    private final Map<DecodeName, DecodeType> primitiveOrNativeTypeByNameMap = new HashMap<>();
    @NotNull
    private final Map<DecodeName, DecodeNativeType> nativeTypeByNameMap = new HashMap<>();
    @NotNull
    @Override
    public <T extends DecodeReferenceable> DecodeResolvingResult<T> resolve(@NotNull DecodeRegistry registry,
                                                                          @NotNull URI uri,
                                                                          @NotNull Class<T> cls)
    {
        List<String> parts = DecodeUtils.getUriParts(uri);
        if (parts.size() == 2 && parts.get(0).equals(DecodeConstants.SYSTEM_NAMESPACE_NAME.asString()))
        {
            Optional<DecodeNamespace> namespaceOptional = DecodeUtils.getNamespaceByFqn(registry,
                    ImmutableDecodeFqn.newInstance(Lists.newArrayList(DecodeConstants.SYSTEM_NAMESPACE_NAME)));
            Preconditions.checkState(namespaceOptional.isPresent(), "system namespace not found");
            String typeName = parts.get(1);
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
                    if (!namespace.getTypes().stream().filter(t -> t.getName().equals(primitiveType.getName()))
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
                if (!namespace.getTypes().stream().filter(t -> t.getName().equals(nativeType.getName()))
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
