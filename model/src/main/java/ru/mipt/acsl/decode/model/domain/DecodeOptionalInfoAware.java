package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeOptionalInfoAware extends DecodeElement
{
    @NotNull
    Optional<String> getInfo();
}
