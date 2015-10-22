package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class SimpleDecodeNamespace implements DecodeNamespace
{
    @NotNull
    private final DecodeName name;
    @NotNull
    private List<DecodeType> types = new ArrayList<>();
    @NotNull
    private final List<DecodeUnit> units = new ArrayList<>();
    @NotNull
    private final List<DecodeNamespace> subNamespaces = new ArrayList<>();
    @NotNull
    private Optional<DecodeNamespace> parent;
    @NotNull
    private List<DecodeComponent> components = new ArrayList<>();
    @NotNull
    private List<DecodeLanguage> languages = new ArrayList<>();

    public static DecodeNamespace newInstance(@NotNull DecodeName name, @NotNull Optional<DecodeNamespace> parent)
    {
        return new SimpleDecodeNamespace(name, parent);
    }

    private SimpleDecodeNamespace(@NotNull DecodeName name, @NotNull Optional<DecodeNamespace> parent)
    {
        this.name = name;
        this.parent = parent;
    }

    @NotNull
    @Override
    public String asString()
    {
        return name.asString();
    }

    @NotNull
    @Override
    public List<DecodeUnit> getUnits()
    {
        return units;
    }

    @NotNull
    @Override
    public List<DecodeType> getTypes()
    {
        return types;
    }

    @Override
    public void setTypes(@NotNull List<DecodeType> types)
    {
        this.types = types;
    }

    @NotNull
    @Override
    public List<DecodeNamespace> getSubNamespaces()
    {
        return subNamespaces;
    }

    @NotNull
    @Override
    public Optional<DecodeNamespace> getParent()
    {
        return parent;
    }

    @NotNull
    @Override
    public List<DecodeComponent> getComponents()
    {
        return components;
    }

    @NotNull
    @Override
    public List<DecodeLanguage> getLanguages()
    {
        return languages;
    }

    @Override
    public void setParent(@Nullable DecodeNamespace parent)
    {
        this.parent = Optional.ofNullable(parent);
    }

    @NotNull
    @Override
    public Optional<DecodeName> getOptionalName()
    {
        return Optional.of(name);
    }

    @Override
    @NotNull
    public DecodeName getName()
    {
        return name;
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("Namespace{name=%s, %d subnamespaces, %d units, %d types, %d components}", name.asString(),
                subNamespaces.size(), units.size(), types.size(), components.size());
    }

}
