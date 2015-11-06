package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeOptionalNameAware extends DecodeElement
{
    @NotNull
    Optional<IDecodeName> getOptionalName();
}
