package ru.mipt.acsl.decode.model.component;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.*;
import ru.mipt.acsl.decode.model.component.message.EventMessage;
import ru.mipt.acsl.decode.model.component.message.StatusMessage;
import ru.mipt.acsl.decode.model.naming.*;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyCompanion;
import ru.mipt.acsl.decode.model.registry.Language;
import ru.mipt.acsl.decode.model.types.Alias;
import ru.mipt.acsl.decode.model.types.StructType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by metadeus on 11.06.16.
 */
public interface Component extends Container, HasName, HasNamespace, MayHaveId, HasAlias {

    static Component newInstance(Alias.NsComponent alias, Namespace namespace, @Nullable Integer id,
                                 @Nullable  MaybeProxyCompanion.Struct baseTypeProxy,
                                 List<Referenceable> objects) {
        return new ComponentImpl(alias, namespace, id, baseTypeProxy, objects);
    }

    Optional<MaybeProxyCompanion.Struct> baseTypeProxy();

    default Optional<StructType> baseType() {
        return baseTypeProxy().map(MaybeProxyCompanion.Struct::obj);
    }

    void setNamespace(Namespace ns);

    @Override
    Alias.NsComponent alias();

    @Override
    default <T> T accept(ContainerVisitor<T> visitor) {
        return visitor.visit(this);
    }

    default ElementName name() {
        return alias().name();
    }

    default Map<Language, String> info() {
        return alias().info();
    }

    default List<StatusMessage> statusMessages() {
        return filterByClass(StatusMessage.class);
    }

    default List<EventMessage> eventMessages() {
        return filterByClass(EventMessage.class);
    }

    default List<Command> commands() {
        return filterByClass(Command.class);
    }

    default List<Alias.ComponentComponent> subComponents() {
        return filterByClass(Alias.ComponentComponent.class);
    }

    default Fqn fqn() {
        return Fqn.newInstance(namespace().fqn(), name());
    }

    default Optional<EventMessage> eventMessage(ElementName name) {
        return filterByClassAndNameStream(Alias.ComponentEventMessage.class, name).findAny()
                .map(Alias.ComponentEventMessage::obj);
    }

    default Optional<StatusMessage> statusMessage(ElementName name) {
        return filterByClassAndNameStream(Alias.ComponentStatusMessage.class, name).findAny()
                .map(Alias.ComponentStatusMessage::obj);
    }

}

