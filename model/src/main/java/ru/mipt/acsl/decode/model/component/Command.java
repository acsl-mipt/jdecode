package ru.mipt.acsl.decode.model.component;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.*;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.HasName;
import ru.mipt.acsl.decode.model.proxy.MaybeProxy;
import ru.mipt.acsl.decode.model.registry.Language;
import ru.mipt.acsl.decode.model.types.Alias;
import ru.mipt.acsl.decode.model.types.DecodeType;
import ru.mipt.acsl.decode.model.types.TypeMeasure;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Command extends Container, HasName, MayHaveId, HasInfo {

    static Command newInstance(Alias.ComponentCommand alias, @Nullable Integer id, List<Referenceable> objects,
                               TypeMeasure returnTypeUnit) {
        return new CommandImpl(alias, id, objects, returnTypeUnit);
    }

    Alias.ComponentCommand alias();

    TypeMeasure returnTypeUnit();

    default MaybeProxy.TypeProxy returnTypeProxy() {
        return returnTypeUnit().typeProxy();
    }

    default DecodeType returnType() {
        return returnTypeProxy().obj();
    }

    default List<Parameter> parameters() {
        return objects().stream().flatMap(p -> p instanceof Parameter ? Stream.of((Parameter) p) : Stream.empty())
                .collect(Collectors.toList());
    }

    @Override
    default ElementName name() {
        return alias().name();
    }

    @Override
    default Map<Language, String> info() {
        return alias().info();
    }

}