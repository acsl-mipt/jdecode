package ru.mipt.acsl.decode.model.domain;

import com.google.common.base.Preconditions;
import ru.mipt.acsl.common.Either;
import ru.mipt.acsl.decode.model.domain.impl.DecodeParameterWalker;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessage;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.type.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public interface DecodeComponent extends DecodeOptionalInfoAware, DecodeReferenceable, DecodeNamespaceAware
{
    @NotNull
    Optional<DecodeMaybeProxy<DecodeType>> getBaseType();

    @NotNull
    Set<DecodeMaybeProxy<DecodeComponent>> getSubComponents();

    @NotNull
    List<DecodeCommand> getCommands();

    @NotNull
    List<DecodeMessage> getMessages();

    @Override
    default <T> T accept(@NotNull DecodeReferenceableVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    @NotNull
    default SortedSet<DecodeComponent> getAllSubComponentsOrdered()
    {
        SortedSet<DecodeComponent> components = new TreeSet<>();
        components.addAll(getSubComponents().stream().map(DecodeMaybeProxy::getObject).collect(Collectors.toSet()));
        components.addAll(
                components.stream().flatMap(c -> c.getAllSubComponentsOrdered().stream()).collect(Collectors.toSet()));
        return components;
    }

    @NotNull
    default DecodeType getTypeForParameter(@NotNull DecodeMessageParameter parameter)
    {
        Optional<DecodeMaybeProxy<DecodeType>> baseType = getBaseType();
        Preconditions.checkState(baseType.isPresent());
        DecodeType result = baseType.get().getObject();
        DecodeParameterWalker walker = new DecodeParameterWalker(parameter);
        while (walker.hasNext())
        {
            Either<String, Integer> token = walker.next();
            result = Preconditions.checkNotNull(result)
                    .accept(new TokenWalker(token));
        }
        return Preconditions.checkNotNull(result);
    }

    class TokenWalker implements DecodeTypeVisitor<DecodeType>
    {
        @NotNull
        private final Either<String, Integer> token;

        public TokenWalker(@NotNull Either<String, Integer> token)
        {
            this.token = token;
        }

        @Override
        public DecodeType visit(@NotNull DecodePrimitiveType primitiveType) throws RuntimeException
        {
            return null;
        }

        @Override
        public DecodeType visit(@NotNull DecodeNativeType nativeType) throws RuntimeException
        {
            return null;
        }

        @Override
        public DecodeType visit(@NotNull DecodeSubType subType) throws RuntimeException
        {
            return subType.getBaseType().getObject().accept(this);
        }

        @Override
        public DecodeType visit(@NotNull DecodeEnumType enumType) throws RuntimeException
        {
            return null;
        }

        @Override
        public DecodeType visit(@NotNull DecodeArrayType arrayType) throws RuntimeException
        {
            Preconditions.checkState(token.isRight());
            return arrayType.getBaseType().getObject();
        }

        @Override
        public DecodeType visit(@NotNull DecodeStructType structType) throws RuntimeException
        {
            Preconditions.checkState(token.isLeft());
            String name = token.getLeft();
            return structType.getFields().stream().filter(f -> f.getName().asString().equals(name))
                    .findAny().orElseThrow(() -> new AssertionError(String.format("Field '%s' not found in struct '%s'", name, structType))).getType().getObject();
        }

        @Override
        public DecodeType visit(@NotNull DecodeAliasType typeAlias) throws RuntimeException
        {
            return typeAlias.getType().getObject().accept(this);
        }
    }
}
