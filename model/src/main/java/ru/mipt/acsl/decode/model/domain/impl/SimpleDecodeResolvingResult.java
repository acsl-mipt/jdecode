package ru.mipt.acsl.decode.model.domain.impl;

import com.google.common.collect.Lists;
import ru.mipt.acsl.JavaToScala;
import ru.mipt.acsl.ScalaToJava;
import ru.mipt.acsl.decode.model.domain.DecodeReferenceable;
import ru.mipt.acsl.decode.model.domain.DecodeResolvingResult;
import ru.mipt.acsl.decode.modeling.ModelingMessage;
import ru.mipt.acsl.decode.modeling.ResolvingMessage;
import ru.mipt.acsl.decode.modeling.impl.SimpleMessage;
import org.jetbrains.annotations.NotNull;
import scala.Option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public final class SimpleDecodeResolvingResult<T extends DecodeReferenceable> implements DecodeResolvingResult<T>
{
    private static final DecodeResolvingResult<DecodeReferenceable> EMPTY = newInstance(Option.<DecodeReferenceable>empty());
    @NotNull
    private final List<ResolvingMessage> messages;
    @NotNull
    private final Option<T> resolvedObject;

    @NotNull
    public static <T extends DecodeReferenceable> DecodeResolvingResult<T> newInstance(
            @NotNull Option<T> resolvedObject)
    {
        return new SimpleDecodeResolvingResult<>(resolvedObject, new ArrayList<>());
    }

    @NotNull
    public static <T extends DecodeReferenceable> DecodeResolvingResult<T> newInstance(
            @NotNull Option<T> resolvedObject, @NotNull List<ResolvingMessage> messages)
    {
        return new SimpleDecodeResolvingResult<>(resolvedObject, messages);
    }

    @SuppressWarnings("unchecked")
    public static <T extends DecodeReferenceable> DecodeResolvingResult<T> immutableEmpty()
    {
        return (DecodeResolvingResult<T>) EMPTY;
    }

    public static <T extends DecodeReferenceable> DecodeResolvingResult<T> error(@NotNull String msg,
                                                                               @NotNull Object... args)
    {
        return new SimpleDecodeResolvingResult<>(Option.empty(), Lists.newArrayList(SimpleMessage.error(msg, args)));
    }

    public static <T extends DecodeReferenceable> DecodeResolvingResult<T> merge(
            @NotNull DecodeResolvingResult<T> resolvingResult1,
            @NotNull DecodeResolvingResult<T> resolvingResult2)
    {
        List<ResolvingMessage> msgs = new ArrayList<>(resolvingResult1.getMessages().size() + resolvingResult2.getMessages().size());
        msgs.addAll(resolvingResult1.getMessages());
        msgs.addAll(resolvingResult2.getMessages());
        Option<T> obj = resolvingResult1.resolvedObject();
        if (obj.isEmpty())
            obj = resolvingResult2.resolvedObject();
        return new SimpleDecodeResolvingResult<>(obj, msgs);
    }

    @NotNull
    @Override
    public Option<T> resolvedObject()
    {
        return resolvedObject;
    }

    private SimpleDecodeResolvingResult(@NotNull Option<T> resolvedObject,
                                        @NotNull List<ResolvingMessage> messages)
    {
        this.resolvedObject = resolvedObject;
        this.messages = messages;
    }

    @Override
    public boolean hasError()
    {
        return messages.stream().filter(msg -> msg.getLevel().equals(ModelingMessage.Level.ERROR)).findAny().isPresent();
    }

    @NotNull
    @Override
    public List<ResolvingMessage> getMessages()
    {
        return messages;
    }
}
