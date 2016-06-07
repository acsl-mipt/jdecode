package ru.mipt.acsl.decode.model.types;

import ru.mipt.acsl.decode.model.CommandOrTmMessage;
import ru.mipt.acsl.decode.model.Parameter;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.component.Command;
import ru.mipt.acsl.decode.model.component.Component;
import ru.mipt.acsl.decode.model.component.message.EventMessage;
import ru.mipt.acsl.decode.model.component.message.StatusMessage;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.HasName;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxy;
import ru.mipt.acsl.decode.model.registry.Language;
import ru.mipt.acsl.decode.model.registry.Measure;

import java.util.Map;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Alias<P extends Container, O extends Referenceable> extends Referenceable, HasName {

    ElementName name();

    Container parent();

    void parent(P parent);

    Map<Language, String> info();

    O obj();

    void obj(O obj);

    class NsType extends AbstractTypeAlias<Namespace, DecodeType> {

        public NsType(ElementName name, Map<Language, String> info, Namespace parent, DecodeType obj) {
            super(name, info, parent, obj);
        }

    }

    class ComponentCommand extends AbstractAlias<Component, Command> {

        public ComponentCommand(ElementName name, Map<Language, String> info, Component parent, Command obj) {
            super(name, info, parent, obj);
        }

    }

    class NsComponent extends AbstractAlias<Namespace, Component> {

        public NsComponent(ElementName name, Map<Language, String> info, Namespace parent, Component obj) {
            super(name, info, parent, obj);
        }

    }

    class NsMeasure extends AbstractAlias<Namespace, Measure> {

        public NsMeasure(ElementName name, Map<Language, String> info, Namespace parent, Measure obj) {
            super(name, info, parent, obj);
        }

    }

    class NsConst extends AbstractTypeAlias<Namespace, Const> {

        public NsConst(ElementName name, Map<Language, String> info, Namespace parent, Const obj) {
            super(name, info, parent, obj);
        }

    }

    class NsNs extends AbstractAlias<Namespace, Namespace> {

        public NsNs(ElementName name, Map<Language, String> info, Namespace parent, Namespace obj) {
            super(name, info, parent, obj);
        }

    }

    class NsReferenceable extends AbstractAlias<Namespace, Referenceable> {

        public NsReferenceable(ElementName name, Map<Language, String> info, Namespace parent, Referenceable obj) {
            super(name, info, parent, obj);
        }

    }

    class NsTypeMeasure extends AbstractAlias<Namespace, TypeMeasure> {

        public NsTypeMeasure(ElementName name, Map<Language, String> info, Namespace parent, TypeMeasure obj) {
            super(name, info, parent, obj);
        }

    }

    class ComponentComponent extends AbstractAlias<Component, MaybeProxy.Component> {

        public ComponentComponent(ElementName name, Map<Language, String> info, Component parent, MaybeProxy.Component component) {
            super(name, info, parent, component);
        }

    }

    class ComponentParameter extends AbstractAlias<Component, Parameter> {

        public ComponentParameter(ElementName name, Map<Language, String> info, Component parent, Parameter parameter) {
            super(name, info, parent, parameter);
        }

    }

    class ComponentEventMessage extends AbstractAlias<Component, EventMessage> {

        public ComponentEventMessage(ElementName name, Map<Language, String> info, Component parent, EventMessage eventMessage) {
            super(name, info, parent, eventMessage);
        }

    }

    class ComponentStatusMessage extends AbstractAlias<Component, StatusMessage> {

        public ComponentStatusMessage(ElementName name, Map<Language, String> info, Component parent, StatusMessage statusMessage) {
            super(name, info, parent, statusMessage);
        }

    }

    class EnumConstant extends AbstractAlias<EnumType, ru.mipt.acsl.decode.model.types.EnumConstant> {

        public EnumConstant(ElementName name, Map<Language, String> info, EnumType parent,
                            ru.mipt.acsl.decode.model.types.EnumConstant enumConstant) {
            super(name, info, parent, enumConstant);
        }

    }

    class StructField extends AbstractAlias<StructType, ru.mipt.acsl.decode.model.types.StructField> {

        public StructField(ElementName name, Map<Language, String> info, StructType parent,
                            ru.mipt.acsl.decode.model.types.StructField structField) {
            super(name, info, parent, structField);
        }

    }

    class MessageOrCommandParameter extends AbstractAlias<CommandOrTmMessage, Parameter> {

        public MessageOrCommandParameter(ElementName name, Map<Language, String> info, CommandOrTmMessage parent,
                                         Parameter parameter) {
            super(name, info, parent, parameter);
        }

    }


}
