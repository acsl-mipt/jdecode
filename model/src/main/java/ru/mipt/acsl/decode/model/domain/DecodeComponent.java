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
public interface DecodeComponent extends DecodeOptionalInfoAware, DecodeNameAware, DecodeReferenceable, DecodeNamespaceAware
{
    @NotNull
    Optional<DecodeMaybeProxy<DecodeType>> getBaseType();

    @NotNull
    List<DecodeComponentRef> getSubComponents();

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
    default DecodeType getTypeForParameter(@NotNull DecodeMessageParameter parameter)
    {
        DecodeParameterWalker walker = new DecodeParameterWalker(parameter);
        DecodeComponentWalker componentWalker = new DecodeComponentWalker(this);
        while (walker.hasNext())
        {
            Either<String, Integer> token = walker.next();
            componentWalker.walk(token);
        }
        return Preconditions.checkNotNull(componentWalker.getType()).get();
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

        @Override
        public DecodeType visit(@NotNull DecodeGenericType genericType)
        {
            return null;
        }

        @Override
        public DecodeType visit(@NotNull DecodeGenericTypeSpecialized genericTypeSpecialized)
        {
            return null;
        }
    }

    class DecodeComponentWalker
    {
        @NotNull
        private DecodeComponent component;
        @NotNull
        private Optional<DecodeType> type = Optional.empty();

        public DecodeComponentWalker(@NotNull DecodeComponent component)
        {
            this.component = component;
        }

        @NotNull
        public Optional<DecodeType> getType()
        {
            return type;
        }

        public void walk(@NotNull Either<String, Integer> token)
        {
            if (type.isPresent())
            {
                // must not return null
                type = Optional.of(type.get().accept(new TokenWalker(token)));
            }
            else
            {
                Preconditions.checkArgument(token.isLeft());
                String stringToken = token.getLeft();
                Optional<DecodeComponentRef> subComponent = component.getSubComponents().stream().filter(cr -> {
                    Optional<String> alias = cr.getAlias();
                    return (alias.isPresent() && alias.get().equals(stringToken))
                            || cr.getComponent().getObject().getName().asString().equals(stringToken);
                }).findAny();
                if (subComponent.isPresent())
                {
                    component = subComponent.get().getComponent().getObject();
                }
                else
                {
                    Preconditions.checkState(component.getBaseType().isPresent());
                    type = Optional.of(component.getBaseType().get().getObject());
                    walk(token);
                }
            }
        }
    }
}
