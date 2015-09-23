package ru.mipt.acsl.decode.idea.plugin;

import ru.mipt.acsl.decode.model.domain.DecodeRegistry;
import ru.mipt.acsl.decode.modeling.ModelingMessage;
import ru.mipt.acsl.decode.modeling.TransformationMessage;
import ru.mipt.acsl.decode.modeling.TransformationResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
class DecodeTransformationResult implements TransformationResult<DecodeRegistry>
{
    @NotNull
    Optional<DecodeRegistry> result = Optional.empty();
    @NotNull
    List<TransformationMessage> messages = new ArrayList<>();

    public DecodeTransformationResult(
            @NotNull DecodeRegistry registry)
    {
        result = Optional.of(registry);
    }

    @NotNull
    @Override
    public Optional<DecodeRegistry> getResult()
    {
        return result;
    }

    @NotNull
    @Override
    public List<TransformationMessage> getMessages()
    {
        return messages;
    }

    @Override
    public boolean hasError()
    {
        return messages.stream().filter(msg -> msg.getLevel().equals(ModelingMessage.Level.ERROR)).findAny()
                .isPresent();
    }
}
