package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.types.Alias;
import ru.mipt.acsl.decode.model.types.TypeMeasure;

/**
 * Created by metadeus on 06.06.16.
 */
public class ParameterImpl implements Parameter {

    private final Alias.MessageOrCommandParameter alias;
    private final TypeMeasure typeMeasure;

    ParameterImpl(Alias.MessageOrCommandParameter alias, TypeMeasure typeMeasure) {
        this.alias = alias;
        this.typeMeasure = typeMeasure;
    }

    @Override
    public Alias.MessageOrCommandParameter alias() {
        return alias;
    }

    @Override
    public TypeMeasure typeMeasure() {
        return typeMeasure;
    }
}
