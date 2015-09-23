package ru.mipt.acsl.decode.model.domain.message;

import ru.mipt.acsl.decode.model.domain.DecodeComponent;
import ru.mipt.acsl.decode.model.domain.DecodeNameAware;
import ru.mipt.acsl.decode.model.domain.DecodeOptionalInfoAware;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Artem Shein
 */
public interface DecodeMessage extends DecodeOptionalInfoAware, DecodeNameAware
{
    <T, E extends Throwable> T accept(DecodeMessageVisitor<T, E> visitor) throws E;

    @NotNull
    List<DecodeMessageParameter> getParameters();

    @NotNull
    DecodeComponent getComponent();

    int getId();
}
