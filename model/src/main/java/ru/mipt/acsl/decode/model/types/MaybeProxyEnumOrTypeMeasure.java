package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyEnumType;

import java.util.Optional;

/**
 * Created by metadeus on 07.06.16.
 */
public class MaybeProxyEnumOrTypeMeasure {

    @Nullable
    private final MaybeProxyEnumType maybeProxyEnum;
    @Nullable
    private final TypeMeasure typeMeasure;

    public MaybeProxyEnumOrTypeMeasure(@NotNull MaybeProxyEnumType maybeProxyEnum) {
        this.maybeProxyEnum = maybeProxyEnum;
        this.typeMeasure = null;
    }

    public MaybeProxyEnumOrTypeMeasure(@NotNull TypeMeasure typeMeasure) {
        this.typeMeasure = typeMeasure;
        this.maybeProxyEnum = null;
    }

    public Optional<MaybeProxyEnumType> maybeProxyEnum() {
        return Optional.ofNullable(maybeProxyEnum);
    }

    public Optional<TypeMeasure> typeMeasure() {
        return Optional.ofNullable(typeMeasure);
    }

}
