package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.HasName;
import ru.mipt.acsl.decode.model.proxy.MaybeProxy;
import ru.mipt.acsl.decode.model.registry.Language;
import ru.mipt.acsl.decode.model.registry.Measure;
import ru.mipt.acsl.decode.model.types.Alias;
import ru.mipt.acsl.decode.model.types.DecodeType;
import ru.mipt.acsl.decode.model.types.TypeMeasure;

import java.util.Map;
import java.util.Optional;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Parameter extends TmParameter, HasName {

    static Parameter newInstance(Alias.MessageOrCommandParameter alias, TypeMeasure typeMeasure) {
        return new ParameterImpl(alias, typeMeasure);
    }

    Alias.MessageOrCommandParameter alias();

    TypeMeasure typeMeasure();

    default MaybeProxy.TypeProxy typeProxy() {
        return typeMeasure().typeProxy();
    }

    default DecodeType parameterType() {
        return typeProxy().obj();
    }

    default Optional<MaybeProxy.Measure> measureProxy() {
        return typeMeasure().measureProxy();
    }

    default Optional<Measure> measure() {
        return measureProxy().map(MaybeProxy.Measure::obj);
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
