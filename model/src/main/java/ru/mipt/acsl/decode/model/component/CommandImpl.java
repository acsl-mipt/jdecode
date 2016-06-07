package ru.mipt.acsl.decode.model.component;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.types.Alias;
import ru.mipt.acsl.decode.model.types.TypeMeasure;

import java.util.List;
import java.util.Optional;

/**
 * Created by metadeus on 06.06.16.
 */
class CommandImpl implements Command {

    @Nullable
    private final Integer id;
    private final Alias.ComponentCommand alias;
    private final TypeMeasure returnTypeUnit;
    private List<Referenceable> objects;

    CommandImpl(Alias.ComponentCommand alias, @Nullable Integer id, List<Referenceable> objects,
                TypeMeasure returnTypeUnit) {
        this.alias = alias;
        this.id = id;
        this.objects = objects;
        this.returnTypeUnit = returnTypeUnit;
    }

    @Override
    @Nullable
    public Integer id() {
        return id;
    }

    @Override
    public List<Referenceable> objects() {
        return objects;
    }

    @Override
    public void objects(List<Referenceable> objects) {
        this.objects = objects;
    }

    @Override
    public Alias.ComponentCommand alias() {
        return alias;
    }

    @Override
    public TypeMeasure returnTypeUnit() {
        return returnTypeUnit;
    }
}

