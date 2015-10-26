package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeCommand extends DecodeOptionalInfoAware, DecodeNameAware
{
    @NotNull
    Optional<Integer> getId();
    @NotNull
    List<DecodeCommandArgument> getArguments();
}
