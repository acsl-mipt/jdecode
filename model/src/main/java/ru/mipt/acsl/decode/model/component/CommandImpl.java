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
    private final Component component;
    private final TypeMeasure returnTypeUnit;
    private List<Referenceable> objects;

    CommandImpl(Alias.ComponentCommand alias, Component component, @Nullable Integer id, List<Referenceable> objects,
                TypeMeasure returnTypeUnit) {
        this.alias = alias;
        this.component = component;
        this.id = id;
        this.objects = objects;
        this.returnTypeUnit = returnTypeUnit;
    }

    @Override
    public Optional<Integer> id() {
        return Optional.ofNullable(id);
    }

    @Override
    public List<Referenceable> objects() {
        return objects;
    }

    @Override
    public void setObjects(List<Referenceable> objects) {
        this.objects = objects;
    }

    @Override
    public Component component() {
        return component;
    }

    @Override
    public Alias.ComponentCommand alias() {
        return alias;
    }

    @Override
    public TypeMeasure returnTypeMeasure() {
        return returnTypeUnit;
    }
}

