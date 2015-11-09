package ru.mipt.acsl.decode.model.domain;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.common.Either;
import ru.mipt.acsl.decode.model.domain.impl.TokenWalker;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class DecodeComponentWalker
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

    public void walk(scala.util.Either<String, Integer> token)
    {
        if (type.isPresent())
        {
            // must not return null
            type = Optional.of(type.get().accept(new TokenWalker(token)));
        }
        else
        {
            Preconditions.checkArgument(token.isLeft());
            scala.util.Either.LeftProjection stringToken = token.left();
            Optional<DecodeComponentRef> subComponent = component.subComponents().stream().filter(cr -> {
                Optional<String> alias = cr.getAlias();
                return (alias.isPresent() && alias.get().equals(stringToken))
                        || cr.getComponent().getObject().name().asString().equals(stringToken);
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
