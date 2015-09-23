package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeNamespaceAware
{
    @NotNull
    DecodeNamespace getNamespace();

    void setNamespace(@NotNull DecodeNamespace namespace);
}
