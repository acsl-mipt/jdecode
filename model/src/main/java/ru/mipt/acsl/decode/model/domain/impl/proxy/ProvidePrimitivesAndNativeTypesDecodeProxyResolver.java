package ru.mipt.acsl.decode.model.domain.impl.proxy;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.DecodeNameImpl;
import ru.mipt.acsl.decode.model.domain.impl.DecodeUtils;
import ru.mipt.acsl.decode.model.domain.impl.SimpleDecodeResolvingResult;
import ru.mipt.acsl.decode.model.domain.impl.type.*;
import scala.Enumeration;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Seq;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.mipt.acsl.JavaToScala.asOption;
import static ru.mipt.acsl.decode.ScalaUtil.append;

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
        Preconditions.checkState(DecodeConstants.SYSTEM_NAMESPACE_FQN().size() == 1, "not implemented");
        if (parts.size() == 2 && parts.get(0).equals(DecodeConstants.SYSTEM_NAMESPACE_FQN().asMangledString()))
        {
            Option<DecodeNamespace> systemNamespaceOptional = DecodeUtils.getNamespaceByFqn(registry,
                    DecodeConstants.SYSTEM_NAMESPACE_FQN());
            Preconditions.checkState(systemNamespaceOptional.isDefined(), "system namespace not found");
            String typeName = parts.get(1);
            // Primitive type
            if (typeName.contains(":"))
            {
                int index = typeName.indexOf(":");
                String typeKind = typeName.substring(0, index);
                long bitLength = Long.parseLong(typeName.substring(index + 1));
                Option<Enumeration.Value> typeKindOptional = TypeKind.typeKindByName(typeKind);
                if (typeKindOptional.isDefined())
                {
                    DecodeName name = DecodeNameImpl.newFromMangledName(typeName);
                    DecodeNamespace namespace = systemNamespaceOptional.get();
                    DecodeType nativeOrPrimitiveType = knownTypeByNameMap.computeIfAbsent(name,
                            (nameKey) -> new DecodePrimitiveTypeImpl(Option.apply(nameKey), namespace, Option.empty(), typeKindOptional.get(),
                                            bitLength));
                    Preconditions.checkState(nativeOrPrimitiveType instanceof DecodePrimitiveType);
                    DecodePrimitiveType primitiveType = (DecodePrimitiveType) nativeOrPrimitiveType;
                    Option<DecodeName> primitiveTypeOptionalName = primitiveType.optionName();
                    if (primitiveTypeOptionalName.isDefined()
                            && !findTypeForName(namespace.types(), primitiveTypeOptionalName.get()).isPresent())
                    {
                        namespace.types_$eq(append(namespace.types(), primitiveType));
                    }
                    return SimpleDecodeResolvingResult.newInstance(asOption(Optional.of(primitiveType)
                            .filter(cls::isInstance).map(cls::cast)));
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
                        (Optional<DecodeMaybeProxy<DecodeGenericType>>) (Optional<?>) DecodeUtils.uriToOptionalMaybeProxyType("/" + DecodeConstants.SYSTEM_NAMESPACE_FQN().asMangledString() + "/" + genericTypeName);
                Preconditions.checkState(genericTypeOptional.isPresent(), "invalid generic type");
                DecodeResolvingResult<?> result = genericTypeOptional.get().resolve(registry, DecodeGenericType.class);
                if (result.hasError())
                    return (DecodeResolvingResult<T>) result;
                Optional<DecodeGenericTypeSpecialized> resObj = Optional.of(genericTypeSpecializedByTypeNameMap.computeIfAbsent(typeName,
                        tn -> {
                            DecodeGenericTypeSpecializedImpl t = new DecodeGenericTypeSpecializedImpl(Option.empty(),
                                genericTypeOptional.get().obj().namespace(), Option.empty(),
                                genericTypeOptional.get(), JavaConversions
                                .asScalaBuffer(Stream.of(genericArgumentsString.split(Pattern.quote(",")))
                                        .map(DecodeUtils::uriToOptionalMaybeProxyType)
                                        .map(o -> Option.apply(o.orElse(null)))
                                        .collect(Collectors.toList())));
                            DecodeNamespace systemNamespace = systemNamespaceOptional.get();
                            systemNamespace.types_$eq(append(systemNamespace.types(), t));
                            return t;
                        }));
                Optional<DecodeResolvingResult<DecodeType>> argsResolvingResult = resObj.map((DecodeGenericTypeSpecialized obj) -> {
                    Iterator<Option<DecodeMaybeProxy<DecodeType>>> it = obj.genericTypeArguments().iterator();
                    DecodeResolvingResult<DecodeType> resolvingResult = SimpleDecodeResolvingResult.immutableEmpty();
                    while (it.hasNext()) {
                        Option<DecodeMaybeProxy<DecodeType>> itIs = it.next();
                        if (itIs.isDefined())
                            resolvingResult = SimpleDecodeResolvingResult.merge(resolvingResult,
                                    itIs.get().resolve(registry, DecodeType.class));
                    }
                    return resolvingResult;
                });
                if (argsResolvingResult.isPresent() && argsResolvingResult.get().hasError())
                    return SimpleDecodeResolvingResult.newInstance(Option.empty(), argsResolvingResult.get().getMessages());
                return SimpleDecodeResolvingResult.newInstance(asOption(resObj
                        .filter(cls::isInstance).map(cls::cast)));
            }
            // Native type
            else if (DecodeNativeType$.MODULE$.MANGLED_TYPE_NAMES().contains(typeName))
            {
                DecodeNameImpl name = DecodeNameImpl.newFromMangledName(typeName);
                DecodeNamespace namespace = systemNamespaceOptional.get();
                DecodeType knownType = knownTypeByNameMap.computeIfAbsent(name,
                        (nameKey) -> {
                            if (nameKey.equals(DecodeBerType.MANGLED_NAME()))
                            {
                                return DecodeBerType.apply(namespace, Option.empty());
                            }
                            else if (nameKey.equals(DecodeOptionalType.MANGLED_NAME()))
                            {
                                return new DecodeOptionalType(Option.apply(nameKey), namespace, Option.empty());
                            }
                            else if (nameKey.equals(DecodeOrType.MANGLED_NAME()))
                            {
                                return new DecodeOrType(Option.apply(nameKey), namespace, Option.empty());
                            }
                            throw new AssertionError();
                        });
                Option<DecodeName> nativeTypeOptionalName = knownType.optionName();
                if (nativeTypeOptionalName.isDefined()
                        && !findTypeForName(namespace.types(), nativeTypeOptionalName.get()).isPresent())
                    namespace.types_$eq(append(namespace.types(), knownType));
                return SimpleDecodeResolvingResult.newInstance(asOption(Optional.of(knownType)
                        .filter(cls::isInstance).map(cls::cast)));
            }
        }
        return SimpleDecodeResolvingResult.immutableEmpty();
    }

    @NotNull
    private Optional<DecodeType> findTypeForName(Seq<DecodeType> types, DecodeName name)
    {
        Iterator<DecodeType> it = types.iterator();
        while (it.hasNext())
        {
            DecodeType type = it.next();
            if (name.equals(type.optionName()))
                return Optional.of(type);
        }
        return Optional.empty();
    }
}
