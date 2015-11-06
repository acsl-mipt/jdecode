package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.DecodeReferenceableVisitor;
import ru.mipt.acsl.decode.model.domain.DecodeUnit;
import ru.mipt.acsl.decode.model.domain.DecodeName;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class SimpleDecodeUnit extends AbstractDecodeOptionalInfoAware implements DecodeUnit
{
    @NotNull
    private final Optional<String> display;
    @NotNull
    private final DecodeNameImpl name;
    @NotNull
    private DecodeNamespace namespace;

    @NotNull
    public static DecodeUnit newInstance(@NotNull DecodeNameImpl name, @NotNull DecodeNamespace namespace,
                                         @NotNull Optional<String> display,
                                         @NotNull Optional<String> info)
    {
        return new SimpleDecodeUnit(name, namespace, display, info);
    }

    @NotNull
    public static DecodeUnit newInstance(@NotNull DecodeNameImpl name, @NotNull DecodeNamespace namespace,
                                         @Nullable String display, @Nullable String info)
    {
        return newInstance(name, namespace, Optional.ofNullable(display), Optional.ofNullable(info));
    }

    private SimpleDecodeUnit(@NotNull DecodeNameImpl name, @NotNull DecodeNamespace namespace,
                             @NotNull Optional<String> display,
                             @NotNull Optional<String> info)
    {
        super(info);
        this.name = name;
        this.namespace = namespace;
        this.display = display;
    }

    @NotNull
    @Override
    public Optional<String> getDisplay()
    {
        return display;
    }

    @Override
    public <T> T accept(DecodeReferenceableVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    @NotNull
    @Override
    public DecodeNameImpl getName()
    {
        return name;
    }

    @NotNull
    @Override
    public Optional<DecodeName> getOptionalName()
    {
        return Optional.of(name);
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("Unit{name=%s, display=%s, info=%s}", name, display, info);
    }

    @NotNull
    @Override
    public DecodeNamespace getNamespace()
    {
        return namespace;
    }

    @Override
    public void setNamespace(@NotNull DecodeNamespace namespace)
    {
        this.namespace = namespace;
    }
}
