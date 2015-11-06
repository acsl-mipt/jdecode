package ru.mipt.acsl.decode.model.domain.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeFqn;
import ru.mipt.acsl.decode.model.domain.DecodeName;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeFqn implements DecodeFqn
{
    @NotNull
    private final List<DecodeName> parts;

    public static DecodeFqn newInstance(@NotNull List<DecodeName> parts)
    {
        return new ImmutableDecodeFqn(parts);
    }

    @NotNull
    public static DecodeFqn newInstanceFromSource(@NotNull String sourceText)
    {
        return new ImmutableDecodeFqn(Stream.of(sourceText.split(Pattern.quote(".")))
                .map(DecodeNameImpl::newFromSourceName).collect(Collectors.toList()));
    }

    @NotNull
    @Override
    public List<DecodeName> getParts()
    {
        return parts;
    }

    @NotNull
    @Override
    public String asString()
    {
        return String.join(".", parts.stream().map(DecodeName::asString).collect(Collectors.<String>toList()));
    }

    @NotNull
    @Override
    public DecodeFqn copyDropLast()
    {
        return newInstance(parts.stream().limit(parts.size() - 1).collect(Collectors.toList()));
    }

    @Override
    public int size()
    {
        return parts.size();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || !(o instanceof DecodeFqn))
        {
            return false;
        }
        DecodeFqn fqn = (DecodeFqn)o;
        return this == fqn || Objects.equals(parts, fqn.getParts());
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(parts);
    }

    private ImmutableDecodeFqn(@NotNull List<DecodeName> parts)
    {
        Preconditions.checkArgument(!parts.isEmpty(), "FQN must not be empty");
        this.parts = ImmutableList.copyOf(parts);
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{parts=%s}", ImmutableDecodeFqn.class.getName(),
                String.join(".", parts.stream().map(DecodeName::asString).collect(Collectors.toList())));
    }
}
