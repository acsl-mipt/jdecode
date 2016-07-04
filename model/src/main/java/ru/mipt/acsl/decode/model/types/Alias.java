package ru.mipt.acsl.decode.model.types;

import ru.mipt.acsl.decode.model.MessageOrCommand;
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
import ru.mipt.acsl.decode.model.proxy.*;
import ru.mipt.acsl.decode.model.registry.Language;

import java.util.Map;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Alias extends Referenceable, HasName {

    ElementName name();

    Container parent();

    void parent(Container parent);

    Map<Language, String> info();

    Object obj();

    void obj(Object obj);

    Referenceable referenceable();

    default <T> T accept(ReferenceableVisitor<T> visitor) {
        return visitor.visit(this);
    }

    interface Type extends Alias {

        @Override
        MaybeTypeProxy obj();

    }

    interface FromNamespace extends Alias
    {
        @Override
        Namespace parent();

        void setParent(Namespace namespace);
    }

    class NsType extends AbstractAlias<Namespace, MaybeProxyType> implements FromNamespace
    {

        public NsType(ElementName name, Map<Language, String> info, Namespace parent, MaybeProxyType obj)
        {
            super(name, info, parent, obj);
        }

        @Override
        public Namespace parent() {
            return super.parent();
        }

        @Override
        public void setParent(Namespace namespace)
        {
            this.parent = namespace;
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public MaybeProxyType obj() {
            return super.obj();
        }

        @Override
        public void obj(Object obj) {
            this.obj = (MaybeProxyType) obj;
        }

        @Override
        public Referenceable referenceable() {
            return obj.obj();
        }

    }

    class ComponentCommand extends AbstractAlias<Component, Command>
    {

        public ComponentCommand(ElementName name, Map<Language, String> info, Component parent, Command obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Component) parent;
        }

        @Override
        public void obj(Object obj) {
            this.obj = (Command) obj;
        }

        @Override
        public Referenceable referenceable() {
            return obj;
        }
    }

    class NsComponent extends AbstractAlias<Namespace, MaybeProxyComponent> implements FromNamespace
    {

        public NsComponent(ElementName name, Map<Language, String> info, Namespace parent, MaybeProxyComponent obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Object obj) {
            this.obj = (MaybeProxyComponent) obj;
        }

        @Override
        public Referenceable referenceable() {
            return obj.obj();
        }

        @Override
        public void setParent(Namespace namespace)
        {
            this.parent = namespace;
        }
    }

    class NsMeasure extends AbstractAlias<Namespace, MaybeProxyMeasure> implements FromNamespace
    {

        public NsMeasure(ElementName name, Map<Language, String> info, Namespace parent, MaybeProxyMeasure obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Object obj) {
            this.obj = (MaybeProxyMeasure) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj.obj();
        }

        @Override
        public void setParent(Namespace namespace)
        {
            this.parent = namespace;
        }
    }

    class NsConst extends AbstractAlias<Namespace, MaybeProxyConst> implements Type, FromNamespace
    {

        public NsConst(ElementName name, Map<Language, String> info, Namespace parent, MaybeProxyConst obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Object obj) {
            this.obj = (MaybeProxyConst) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj.obj();
        }

        @Override
        public void setParent(Namespace namespace)
        {
            this.parent = namespace;
        }
    }

    class NsNs extends AbstractAlias<Namespace, MaybeProxyNamespace> implements FromNamespace
    {

        public NsNs(ElementName name, Map<Language, String> info, Namespace parent, MaybeProxyNamespace obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Object obj) {
            this.obj = (MaybeProxyNamespace) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj.obj();
        }

        @Override
        public void setParent(Namespace namespace)
        {
            this.parent = namespace;
        }
    }

    class NsReferenceable extends AbstractAlias<Namespace, MaybeProxyReferenceable> implements FromNamespace
    {

        public NsReferenceable(ElementName name, Map<Language, String> info, Namespace parent, MaybeProxyReferenceable obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Object obj) {
            this.obj = (MaybeProxyReferenceable) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj.obj();
        }

        @Override
        public void setParent(Namespace namespace)
        {
            this.parent = namespace;
        }
    }

    class NsTypeMeasure extends AbstractAlias<Namespace, TypeMeasure> implements FromNamespace
    {

        public NsTypeMeasure(ElementName name, Map<Language, String> info, Namespace parent, TypeMeasure obj) {
            super(name, info, parent, obj);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Namespace) parent;
        }

        @Override
        public void obj(Object obj) {
            this.obj = (TypeMeasure) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj;
        }

        @Override
        public void setParent(Namespace namespace)
        {
            this.parent = namespace;
        }
    }

    class ComponentComponent extends AbstractAlias<Component, MaybeProxyComponent> {

        public ComponentComponent(ElementName name, Map<Language, String> info, Component parent, MaybeProxyComponent component) {
            super(name, info, parent, component);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (Component) parent;
        }

        @Override
        public void obj(Object obj) {
            this.obj = (MaybeProxyComponent) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj.obj();
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
        public void obj(Object obj) {
            this.obj = (Parameter) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj;
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
        public void obj(Object obj) {
            this.obj = (EventMessage) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj;
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
        public void obj(Object obj) {
            this.obj = (StatusMessage) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj;
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
        public void obj(Object obj) {
            this.obj = (ru.mipt.acsl.decode.model.types.EnumConstant) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj;
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
        public void obj(Object obj) {
            this.obj = (ru.mipt.acsl.decode.model.types.StructField) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj;
        }
    }

    class MessageOrCommandParameter extends AbstractAlias<MessageOrCommand, Parameter> {

        public MessageOrCommandParameter(ElementName name, Map<Language, String> info, MessageOrCommand parent,
                                         Parameter parameter) {
            super(name, info, parent, parameter);
        }

        @Override
        public void parent(Container parent) {
            this.parent = (MessageOrCommand) parent;
        }

        @Override
        public void obj(Object obj) {
            this.obj = (Parameter) obj;
        }

        @Override
        public Referenceable referenceable()
        {
            return obj;
        }
    }


}
