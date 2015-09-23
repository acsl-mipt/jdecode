package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public abstract class AbstractDecodeType extends AbstractDecodeOptionalNameAndOptionalInfoAware implements DecodeType
{
    @NotNull
    protected DecodeNamespace namespace;

    public AbstractDecodeType(
            @NotNull Optional<DecodeName> name,
            @NotNull DecodeNamespace namespace,
            @NotNull Optional<String> info)
    {
        super(name, info);
        this.namespace = namespace;
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
