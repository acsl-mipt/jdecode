package ru.mipt.acsl.decode.model.domain.impl.proxy;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.ScalaUtil;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.DecodeNameImpl;
import ru.mipt.acsl.decode.model.domain.impl.DecodeUtils;
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
    private final Map<String, GenericTypeSpecialized> genericTypeSpecializedByTypeNameMap = new HashMap<>();

    @NotNull
    @Override
    public <T extends Referenceable> ResolvingResult<T> resolve(@NotNull Registry registry,
                                                                @NotNull URI uri, @NotNull Class<T> cls)
    {
        Seq<String> parts = DecodeUtils.getUriParts(uri);
        Preconditions.checkState(DecodeConstants.SYSTEM_NAMESPACE_FQN().size() == 1, "not implemented");
        if (parts.size() == 2 && parts.apply(0).equals(DecodeConstants.SYSTEM_NAMESPACE_FQN().asMangledString()))
        {
            Option<Namespace> systemNamespaceOptional = DecodeUtils.getNamespaceByFqn(registry,
                    DecodeConstants.SYSTEM_NAMESPACE_FQN());
            Preconditions.checkState(systemNamespaceOptional.isDefined(), "system namespace not found");
            String typeName = parts.apply(1);
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
                    Namespace namespace = systemNamespaceOptional.get();
                    DecodeType nativeOrPrimitiveType = knownTypeByNameMap.computeIfAbsent(name,
                            (nameKey) -> new PrimitiveTypeImpl(Option.apply(nameKey), namespace, Option.empty(), typeKindOptional.get(),
                                            bitLength));
                    Preconditions.checkState(nativeOrPrimitiveType instanceof PrimitiveType);
                    PrimitiveType primitiveType = (PrimitiveType) nativeOrPrimitiveType;
                    Option<DecodeName> primitiveTypeOptionalName = primitiveType.optionName();
                    if (primitiveTypeOptionalName.isDefined()
                            && !findTypeForName(namespace.types(), primitiveTypeOptionalName.get()).isPresent())
                    {
                        namespace.types_$eq(append(namespace.types(), primitiveType));
                    }
                    return ResolvingResult.apply(asOption(Optional.of(primitiveType)
                            .filter(cls::isInstance).map(cls::cast)), ScalaUtil.newSeq());
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
                Option<MaybeProxy<GenericType>> genericTypeOptional =
                        (Option<MaybeProxy<GenericType>>) (Option<?>) DecodeUtils.uriToOptionalMaybeProxyType("/" + DecodeConstants.SYSTEM_NAMESPACE_FQN().asMangledString() + "/" + genericTypeName);
                Preconditions.checkState(genericTypeOptional.isDefined(), "invalid generic type");
                ResolvingResult<?> result = genericTypeOptional.get().resolve(registry, GenericType.class);
                if (result.hasError())
                    return (ResolvingResult<T>) result;
                Optional<GenericTypeSpecialized> resObj = Optional.of(genericTypeSpecializedByTypeNameMap.computeIfAbsent(typeName,
                        tn -> {
                            GenericTypeSpecializedImpl t = new GenericTypeSpecializedImpl(Option.empty(),
                                genericTypeOptional.get().obj().namespace(), Option.empty(),
                                genericTypeOptional.get(), JavaConversions
                                .asScalaBuffer(Stream.of(genericArgumentsString.split(Pattern.quote(",")))
                                        .map(DecodeUtils::uriToOptionalMaybeProxyType)
                                        .map(o -> Option.<MaybeProxy<DecodeType>>apply(o.getOrElse(null)))
                                        .collect(Collectors.toList())));
                            Namespace systemNamespace = systemNamespaceOptional.get();
                            systemNamespace.types_$eq(append(systemNamespace.types(), t));
                            return t;
                        }));
                Optional<ResolvingResult<DecodeType>> argsResolvingResult = resObj.map((GenericTypeSpecialized obj) -> {
                    Iterator<Option<MaybeProxy<DecodeType>>> it = obj.genericTypeArguments().iterator();
                    ResolvingResult<DecodeType> resolvingResult = ResolvingResult.empty();
                    while (it.hasNext()) {
                        Option<MaybeProxy<DecodeType>> itIs = it.next();
                        if (itIs.isDefined())
                            resolvingResult = ResolvingResult.merge(resolvingResult,
                                    itIs.get().resolve(registry, DecodeType.class));
                    }
                    return resolvingResult;
                });
                if (argsResolvingResult.isPresent() && argsResolvingResult.get().hasError())
                    return ResolvingResult.apply(Option.empty(), argsResolvingResult.get().messages());
                return ResolvingResult.apply(asOption(resObj
                        .filter(cls::isInstance).map(cls::cast)), ScalaUtil.newSeq());
            }
            // Native type
            else if (NativeType$.MODULE$.MANGLED_TYPE_NAMES().contains(typeName))
            {
                DecodeNameImpl name = DecodeNameImpl.newFromMangledName(typeName);
                Namespace namespace = systemNamespaceOptional.get();
                DecodeType knownType = knownTypeByNameMap.computeIfAbsent(name,
                        (nameKey) -> {
                            if (nameKey.equals(BerType.MANGLED_NAME()))
                            {
                                return BerType.apply(namespace, Option.empty());
                            }
                            else if (nameKey.equals(OptionalType.MANGLED_NAME()))
                            {
                                return new OptionalType(Option.apply(nameKey), namespace, Option.empty());
                            }
                            else if (nameKey.equals(OrType.MANGLED_NAME()))
                            {
                                return new OrType(Option.apply(nameKey), namespace, Option.empty());
                            }
                            throw new AssertionError();
                        });
                Option<DecodeName> nativeTypeOptionalName = knownType.optionName();
                if (nativeTypeOptionalName.isDefined()
                        && !findTypeForName(namespace.types(), nativeTypeOptionalName.get()).isPresent())
                    namespace.types_$eq(append(namespace.types(), knownType));
                return ResolvingResult.apply(asOption(Optional.of(knownType)
                        .filter(cls::isInstance).map(cls::cast)), ScalaUtil.newSeq());
            }
        }
        return ResolvingResult.empty();
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
