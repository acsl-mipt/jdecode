package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.DecodeNamespaceAware;
import ru.mipt.acsl.decode.model.domain.DecodeName;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class AbstractDecodeNameNamespaceOptionalInfoAware extends AbstractDecodeNameAndOptionalInfoAware implements
        DecodeNamespaceAware
{
    @NotNull
    protected DecodeNamespace namespace;

    protected AbstractDecodeNameNamespaceOptionalInfoAware(@NotNull DecodeName name,
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
