package ru.mipt.acsl.decode.model.domain.type;

import ru.mipt.acsl.decode.model.domain.DecodeOptionalInfoAware;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.IDecodeName;

/**
 * @author Artem Shein
 */
public interface DecodeEnumConstant extends DecodeOptionalInfoAware
{
    @NotNull
    IDecodeName getName();
    @NotNull
    String getValue();
}
