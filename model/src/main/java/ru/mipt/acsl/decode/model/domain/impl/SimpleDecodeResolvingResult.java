package ru.mipt.acsl.decode.model.domain.impl;

import com.google.common.collect.Lists;
import ru.mipt.acsl.decode.model.domain.DecodeReferenceable;
import ru.mipt.acsl.decode.modeling.ModelingMessage;
import ru.mipt.acsl.decode.modeling.ResolvingMessage;
import ru.mipt.acsl.decode.modeling.impl.SimpleMessage;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public final class SimpleDecodeResolvingResult<T extends DecodeReferenceable> implements DecodeResolvingResult<T>
{
    private static final DecodeResolvingResult<DecodeReferenceable> EMPTY = newInstance(Optional.<DecodeReferenceable>empty());
    @NotNull
    private final List<ResolvingMessage> messages;
    @NotNull
    private final Optional<T> resolvedObject;

    @NotNull
    public static <T extends DecodeReferenceable> DecodeResolvingResult<T> newInstance(
            @NotNull Optional<T> resolvedObject)
    {
        return new SimpleDecodeResolvingResult<>(resolvedObject, new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    public static <T extends DecodeReferenceable> DecodeResolvingResult<T> immutableEmpty()
    {
        return (DecodeResolvingResult<T>) EMPTY;
    }

    public static <T extends DecodeReferenceable> DecodeResolvingResult<T> error(@NotNull String msg,
                                                                               @NotNull Object... args)
    {
        return new SimpleDecodeResolvingResult<>(Optional.empty(), Lists.newArrayList(SimpleMessage.error(msg, args)));
    }

    public static <T extends DecodeReferenceable> DecodeResolvingResult<T> merge(
            @NotNull DecodeResolvingResult<T> resolvingResult1,
            @NotNull DecodeResolvingResult<T> resolvingResult2)
    {
        resolvingResult1.getMessages().addAll(resolvingResult2.getMessages());
        return resolvingResult1;
    }

    @NotNull
    @Override
    public Optional<T> getResolvedObject()
    {
        return resolvedObject;
    }

    private SimpleDecodeResolvingResult(@NotNull Optional<T> resolvedObject,
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
