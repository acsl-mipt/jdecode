package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Created by metadeus on 07.06.16.
 */
public class EnumTypeOrTypeMeasure {

    @Nullable
    private final EnumType enumType;
    @Nullable
    private final TypeMeasure typeMeasure;

    public EnumTypeOrTypeMeasure(@NotNull EnumType enumType) {
        this.enumType = enumType;
        this.typeMeasure = null;
    }

    public EnumTypeOrTypeMeasure(@NotNull TypeMeasure typeMeasure) {
        this.typeMeasure = typeMeasure;
        this.enumType = null;
    }

    public Optional<EnumType> enumType() {
        return Optional.ofNullable(enumType);
    }

    public Optional<TypeMeasure> typeMeasure() {
        return Optional.ofNullable(typeMeasure);
    }

}
