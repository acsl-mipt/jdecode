package ru.mipt.acsl.decode.model.domain.type;

import ru.mipt.acsl.decode.model.domain.DecodeNamespaceAware;
import ru.mipt.acsl.decode.model.domain.DecodeOptionalNameAndOptionalInfoAware;
import ru.mipt.acsl.decode.model.domain.DecodeReferenceableVisitor;
import ru.mipt.acsl.decode.model.domain.DecodeReferenceable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeType extends DecodeReferenceable, DecodeOptionalNameAndOptionalInfoAware, DecodeNamespaceAware
{
    enum TypeKind
    {
        INT("int"), UINT("uint"), FLOAT("float"), BOOL("bool");

        @NotNull
        private static final Map<String, TypeKind> typeKindByName = new HashMap<String, TypeKind>(){{
            put(INT.getName(), INT);
            put(UINT.getName(), UINT);
            put(FLOAT.getName(), FLOAT);
            put(BOOL.getName(), BOOL);
        }};
        @NotNull
        private final String name;

        @NotNull
        public static Optional<TypeKind> forName(@NotNull String name)
        {
            return Optional.ofNullable(typeKindByName.get(name));
        }

        TypeKind(@NotNull String name)
        {
            this.name = name;
        }

        @NotNull
        public String getName()
        {
            return name;
        }
    }

    <T> T accept(@NotNull DecodeTypeVisitor<T> visitor);

    @Nullable
    default <T> T accept(@NotNull DecodeReferenceableVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
