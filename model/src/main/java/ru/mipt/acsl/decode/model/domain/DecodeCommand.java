package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Artem Shein
 */
public interface DecodeCommand extends DecodeOptionalInfoAware, DecodeNameAware
{
    int getId();
    @NotNull
    List<DecodeCommandArgument> getArguments();
}
