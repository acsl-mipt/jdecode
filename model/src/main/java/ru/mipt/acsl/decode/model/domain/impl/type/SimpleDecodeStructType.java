package ru.mipt.acsl.decode.model.domain.impl.type;

import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.type.DecodeStructField;
import ru.mipt.acsl.decode.model.domain.type.DecodeStructType;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class SimpleDecodeStructType extends AbstractDecodeType implements DecodeStructType
{
    @NotNull
    private final List<DecodeStructField> fields;

    public static DecodeStructType newInstance(@NotNull Optional<DecodeName> name,
                                              @NotNull DecodeNamespace namespace, @NotNull Optional<String> info,
                                              @NotNull List<DecodeStructField> fields)
    {
        return new SimpleDecodeStructType(name, namespace, info, fields);
    }

    private SimpleDecodeStructType(@NotNull Optional<DecodeName> name, @NotNull DecodeNamespace namespace,
                                   @NotNull Optional<String> info,
                                   @NotNull List<DecodeStructField> fields)
    {
        super(name, namespace, info);
        this.fields = ImmutableList.copyOf(fields);
    }

    @NotNull
    @Override
    public List<DecodeStructField> getFields()
    {
        return fields;
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, namespace=%s, info=%s, fields=%s}", SimpleDecodeStructType.class.getName(),
                name, namespace.getFqn(), info, fields);
    }
}
