package ru.mipt.acsl.decode.model.types;

import ru.mipt.acsl.decode.model.CommandOrTmMessage;
import ru.mipt.acsl.decode.model.Parameter;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.ReferenceableVisitor;
import ru.mipt.acsl.decode.model.component.Command;
import ru.mipt.acsl.decode.model.component.Component;
import ru.mipt.acsl.decode.model.component.message.EventMessage;
import ru.mipt.acsl.decode.model.component.message.StatusMessage;
import ru.mipt.acsl.decode.model.component.message.TmMessage;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.HasName;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyCompanion;
import ru.mipt.acsl.decode.model.registry.Language;
import ru.mipt.acsl.decode.model.registry.Measure;

import java.util.Map;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Alias extends Referenceable, HasName {

    ElementName name();

    Container parent();

    void parent(Container parent);

    Map<Language, String> info();

    Referenceable obj();

    void obj(Referenceable obj);

    default <T> T accept(ReferenceableVisitor<T> visitor) {
        return visitor.visit(this);
    }

    interface Type extends Alias {

        @Override
        DecodeType obj();

    }

    class NsType extends AbstractAlias<Namespace, DecodeType> implements Type {

        public NsType(ElementName name, Map<Language, String> info, Namespace parent, DecodeType obj) {
            super(name, info, parent, obj);
        }

        @Override
        public Namespace parent() {
            return (Namespace) super.parent();
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public DecodeType obj() {
            return (DecodeType) super.obj();
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (DecodeType) obj;
        }
    }

    class ComponentCommand extends AbstractAlias<Component, Command> {

        public ComponentCommand(ElementName name, Map<Language, String> info, Component parent, Command obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Component) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (Command) obj;
        }
    }

    class NsComponent extends AbstractAlias<Namespace, Component> {

        public NsComponent(ElementName name, Map<Language, String> info, Namespace parent, Component obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (Component) obj;
        }
    }

    class NsMeasure extends AbstractAlias<Namespace, Measure> {

        public NsMeasure(ElementName name, Map<Language, String> info, Namespace parent, Measure obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (Measure) obj;
        }
    }

    class NsConst extends AbstractAlias<Namespace, Const> implements Type {

        public NsConst(ElementName name, Map<Language, String> info, Namespace parent, Const obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (Const) obj;
        }
    }

    class NsNs extends AbstractAlias<Namespace, Namespace> {

        public NsNs(ElementName name, Map<Language, String> info, Namespace parent, Namespace obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (Namespace) obj;
        }
    }

    class NsReferenceable extends AbstractAlias<Namespace, Referenceable> {

        public NsReferenceable(ElementName name, Map<Language, String> info, Namespace parent, Referenceable obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = obj;
        }
    }

    class NsTypeMeasure extends AbstractAlias<Namespace, TypeMeasure> implements Type {

        public NsTypeMeasure(ElementName name, Map<Language, String> info, Namespace parent, TypeMeasure obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (TypeMeasure) obj;
        }
    }

    class ComponentComponent extends AbstractAlias<Component, MaybeProxyCompanion.Component> {

        public ComponentComponent(ElementName name, Map<Language, String> info, Component parent, MaybeProxyCompanion.Component component) {
            super(name, info, parent, component);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Component) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (MaybeProxyCompanion.Component) obj;
        }
    }

    class ComponentParameter extends AbstractAlias<Component, Parameter> {

        public ComponentParameter(ElementName name, Map<Language, String> info, Component parent, Parameter parameter) {
            super(name, info, parent, parameter);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Component) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (Parameter) obj;
        }
    }

    abstract class ComponentTmMessage<T extends TmMessage> extends AbstractAlias<Component, T> {

        public ComponentTmMessage(ElementName name, Map<Language, String> info, Component parent, T tmMessage) {
            super(name, info, parent, tmMessage);
        }

    }

    class ComponentEventMessage extends ComponentTmMessage<EventMessage> {

        public ComponentEventMessage(ElementName name, Map<Language, String> info, Component parent, EventMessage eventMessage) {
            super(name, info, parent, eventMessage);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Component) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (EventMessage) obj;
        }
    }

    class ComponentStatusMessage extends ComponentTmMessage<StatusMessage> {

        public ComponentStatusMessage(ElementName name, Map<Language, String> info, Component parent, StatusMessage statusMessage) {
            super(name, info, parent, statusMessage);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Component) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (StatusMessage) obj;
        }
    }

    class EnumConstant extends AbstractAlias<EnumType, ru.mipt.acsl.decode.model.types.EnumConstant> {

        public EnumConstant(ElementName name, Map<Language, String> info, EnumType parent,
                            ru.mipt.acsl.decode.model.types.EnumConstant enumConstant) {
            super(name, info, parent, enumConstant);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (EnumType) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (ru.mipt.acsl.decode.model.types.EnumConstant) obj;
        }
    }

    class StructField extends AbstractAlias<StructType, ru.mipt.acsl.decode.model.types.StructField> {

        public StructField(ElementName name, Map<Language, String> info, StructType parent,
                            ru.mipt.acsl.decode.model.types.StructField structField) {
            super(name, info, parent, structField);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (StructType) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (ru.mipt.acsl.decode.model.types.StructField) obj;
        }
    }

    class MessageOrCommandParameter extends AbstractAlias<CommandOrTmMessage, Parameter> {

        public MessageOrCommandParameter(ElementName name, Map<Language, String> info, CommandOrTmMessage parent,
                                         Parameter parameter) {
            super(name, info, parent, parameter);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (CommandOrTmMessage) parent;
        }

        @Override
        public void obj(Referenceable obj) {
            this.obj = (Parameter) obj;
        }
    }


}
