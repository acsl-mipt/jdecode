package ru.mipt.acsl.decode.model.registry;

import com.google.common.collect.Lists;
import ru.mipt.acsl.decode.model.Message;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.ReferenceableVisitor;
import ru.mipt.acsl.decode.model.component.Component;
import ru.mipt.acsl.decode.model.component.message.EventMessage;
import ru.mipt.acsl.decode.model.component.message.StatusMessage;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.*;
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface Registry extends Referenceable {

    static Registry newInstance() {
        return new RegistryImpl(ElementName.newInstanceFromMangledName("GlobalRegistry"), Namespace.newInstanceRoot(),
                Lists.newArrayList(ExistingElementsProxyResolver.newInstance(),
                        NativeLiteralGenericTypesProxyResolver.newInstance()));
    }

    Namespace rootNamespace();

    void setRootNamespace(Namespace ns);

    List<DecodeProxyResolver> proxyResolvers();

    default ResolvingResult<Referenceable> resolveElement(ProxyPath path) {
        for (DecodeProxyResolver resolver : proxyResolvers()) {
            ResolvingResult<Referenceable> resultAndMessages = resolver.resolveElement(this, path);
            if (resultAndMessages.result().isPresent())
                return resultAndMessages;
        }
        return ResolvingResult.newInstance(
                Message.newInstance(Level.ERROR, String.format("path %s can not be resolved", path)));
    }

    default void accept(ReferenceableVisitor visitor) {
        visitor.visit(this);
    }

    // TODO: refactoring
    default Optional<Namespace> findNamespace(Fqn namespaceFqn) {
        List<Namespace> namespaces = rootNamespace().subNamespaces();
        Namespace namespace = null;
        for (ElementName nsName : namespaceFqn.getParts()) {
            namespace = namespaces.stream().filter(ns -> ns.name().equals(nsName)).findAny().orElse(null);
            if (namespace == null)
                return Optional.empty();
            else
                namespaces = namespace.subNamespaces();
        }
        return Optional.ofNullable(namespace);
    }

    default Optional<Component> component(Fqn fqn) {
        Optional<Namespace> namespaceOptional = findNamespace(fqn.copyDropLast());
        if (!namespaceOptional.isPresent())
            return Optional.empty();
        ElementName componentName = fqn.last();
        return namespaceOptional.get().components().stream().filter(c -> c.name().equals(componentName)).findAny();
    }

    default Optional<EventMessage> eventMessage(Fqn fqn) {
        ElementName messageName = fqn.last();
        return component(fqn.copyDropLast()).flatMap(c -> c.eventMessage(messageName));
    }

    default Optional<StatusMessage> statusMessage(Fqn fqn) {
        ElementName messageName = fqn.last();
        return component(fqn.copyDropLast()).flatMap(c -> c.statusMessage(messageName));
    }

    default StatusMessage statusMessageOrFail(String fqn) {
        return statusMessage(Fqn.newInstance(fqn))
                .orElseThrow(() -> new AssertionError(String.format("status message '%s' not found", fqn)));
    }

    default EventMessage eventMessageOrFail(String fqn) {
        return eventMessage(Fqn.newInstance(fqn))
                .orElseThrow(() -> new AssertionError(String.format("event message '%s' not found", fqn)));
    }

    default List<Component> allComponents() {
        return rootNamespace().allComponents();
    }

    default List<Namespace> allNamespaces() {
        return rootNamespace().allNamespaces();
    }

}
