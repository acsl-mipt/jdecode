package ru.mipt.acsl.decode.idea.plugin;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.Registry;
import ru.mipt.acsl.decode.modeling.ErrorLevel$;
import ru.mipt.acsl.decode.modeling.ModelingMessage;
import ru.mipt.acsl.decode.modeling.TransformationResult;
import scala.Option;
import scala.Some;
import scala.collection.JavaConversions;
import scala.collection.immutable.Seq;
import scala.collection.immutable.Seq$;

/**
 * @author Artem Shein
 */
class DecodeTransformationResult implements TransformationResult<Registry>
{
    final Option<Registry> result;
    final Seq<ModelingMessage> messages;

    public DecodeTransformationResult(Option<Registry> registry, Seq<ModelingMessage> messages)
    {
        result = registry;
        this.messages = messages;
    }

    @NotNull
    @Override
    public Option<Registry> result()
    {
        return result;
    }

    @NotNull
    @Override
    public Seq<ModelingMessage> messages()
    {
        return messages;
    }

    @Override
    public boolean hasError()
    {
        return JavaConversions.asJavaCollection(messages).stream()
                .filter(msg -> msg.level().equals(ErrorLevel$.MODULE$)).findAny().isPresent();
    }
}
