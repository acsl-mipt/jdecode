package ru.mipt.acsl.decode.generator.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.mipt.acsl.decode.model.*;
import ru.mipt.acsl.decode.model.component.Command;
import ru.mipt.acsl.decode.model.component.Component;
import ru.mipt.acsl.decode.model.component.HasComponent;
import ru.mipt.acsl.decode.model.component.StatusParameter;
import ru.mipt.acsl.decode.model.component.message.EventMessage;
import ru.mipt.acsl.decode.model.component.message.StatusMessage;
import ru.mipt.acsl.decode.model.component.message.TmMessage;
import ru.mipt.acsl.decode.model.expr.ConstExpr;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.*;
import ru.mipt.acsl.decode.model.proxy.path.GenericTypeName;
import ru.mipt.acsl.decode.model.proxy.path.ProxyElementName;
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath;
import ru.mipt.acsl.decode.model.proxy.path.TypeName;
import ru.mipt.acsl.decode.model.registry.Language;
import ru.mipt.acsl.decode.model.registry.Measure;
import ru.mipt.acsl.decode.model.registry.Registry;
import ru.mipt.acsl.decode.model.types.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DecodeJsonGenerator {

    private final DecodeJsonGeneratorConfig config;

    public static DecodeJsonGenerator newInstance(DecodeJsonGeneratorConfig config) {
        return new DecodeJsonGenerator(config);
    }

    public void generate() {

        new OutputStreamWriter(config.getOutput()) {{

            try {
                List<Component> rootComponents = config.getComponentsFqn().stream()
                        .map(f -> config.getRegistry().component(Fqn.newInstance(f))
                                .orElseGet(() -> { throw new RuntimeException(String.format("component not found %s", f)); }))
                        .collect(Collectors.toList());

                JsonFactory j = new JsonFactory();
                JsonGenerator generator = j.createGenerator(this);
                JsonNodeFactory nodeFactory = new JsonNodeFactory(false);
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode rootNode = nodeFactory.objectNode();
                rootNode.put(Keys.GENERATED.getKey(), LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

                rootNode.set(Keys.OBJECTS.getKey(), new StatefulDecodeJsonGenerator(nodeFactory, rootComponents).generate());

                objectMapper.writeTree(generator, rootNode);
                generator.close();
                close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }};
    }

    public enum Keys {

        KIND("k"), NAME("n"), ALIAS("a"), OBJ("o"), INFO("i"), DISPLAY("d"), NAMESPACE("ns"), VALUE("v"), OBJECTS("os"),
        COMPONENT("c"), PARENT("p"), BASE_TYPE("b"), NATIVE_TYPE("n"), SUB_TYPE("sub"), TYPE_MEASURE("tm"),
        MEASURE("m"), CONST("const"), GENERIC_TYPE_SPECIALIZED("gts"),
        TYPE_ARGUMENTS("as"), STATUS_PARAMETER("sp"), ELEMENTS("e"), MIN("min"), MAX("max"), PARAMETER("p"),
        COMMAND("cmd"), STATUS_MESSAGE("sm"), EVENT_MESSAGE("em"), ENUM_CONSTANT("ec"),
        ENUM_TYPE("e"), EXTENDS_TYPE("et"), IS_FINAL("f"), PROXY("p"),
        CONST_EXPR("ex"), STRUCT_FIELD("f"), STRUCT_TYPE("st"), GENERATED("gen"), PRIORITY("pr"), BASE_TYPE_PROXY("bp"),
        TYPE_PARAMETERS("tp"), GENERIC_TYPE_PROXY("gtp"), TYPE_NAME("tn"), GENERIC_ARGUMENT_PATHS("gap"),
        MAYBE_TYPE_PROXY_TYPE("t"), MAYBE_PROXY_ENUM("e"), MAYBE_PROXY_CONST("ct"), MAYBE_PROXY_STRUCT("s"),
        MAYBE_PROXY_COMPONENT("c"), MAYBE_PROXY_NAMESPACE("ns"), TYPE_PROXY_PATH("tpp"),
        MAYBE_PROXY_MEASURE("m"), MAYBE_PROXY_REFERENCEABLE("r"), TYPE("t"), ID("id"), RETURN_TYPE_MEASURE("rtm"),
        BASE_TYPE_MEASURE("btm"), INDEX("i");

        public String getKey() {
            return key;
        }

        private final String key;

        Keys(String key) {
            this.key = key;
        }

    }

    private DecodeJsonGenerator(DecodeJsonGeneratorConfig config) {
        this.config = config;
    }

    private static class StatefulDecodeJsonGenerator {

        private final List<Component> rootComponents;
        private final JsonNodeFactory nodeFactory;

        private final Map<Referenceable, Integer> map = new HashMap<>();
        private final List<ObjectNode> list = new ArrayList<>();

        StatefulDecodeJsonGenerator(JsonNodeFactory nodeFactory, List<Component> rootComponents) {
            this.nodeFactory = nodeFactory;
            this.rootComponents = rootComponents;
        }

        public ArrayNode generate() {
            rootComponents.forEach(this::generate);
            return nodeFactory.arrayNode().addAll(list);
        }

        private ObjectNode extendedNodeKind(String kind, Referenceable referenceable) {
            ObjectNode node = nodeKind(kind);
            boolean isNotTypeMeasure = !(referenceable instanceof TypeMeasure);
            if (referenceable instanceof HasAlias)
                alias(node, (HasAlias) referenceable);
            else if (referenceable instanceof MayHaveAlias && isNotTypeMeasure) {
                MayHaveAlias aliased = (MayHaveAlias) referenceable;
                aliased.alias().ifPresent(a -> alias(node, a));
            }
            if (referenceable instanceof HasNamespace && isNotTypeMeasure)
                namespace(node, (HasNamespace) referenceable);
            if (referenceable instanceof HasComponent)
                component(node, (HasComponent) referenceable);
            if (referenceable instanceof MayHaveId)
                id(node, (MayHaveId) referenceable);
            if (referenceable instanceof Container)
                objects(node, (Container) referenceable);
            return node;
        }

        private ObjectNode nodeKind(String kind) {
            return nodeFactory.objectNode().put(Keys.KIND.getKey(), kind);
        }

        private void objects(ObjectNode node, Container container) {
            node.putArray(Keys.OBJECTS.getKey()).addAll(
                    container.objects().stream()
                            .map(r -> nodeFactory.numberNode(generate(r)))
                            .collect(Collectors.toList()));
        }

        private void alias(ObjectNode node, HasAlias aliased) {
            alias(node, aliased.alias());
        }

        private void alias(ObjectNode node, Alias alias) {
            node.put(Keys.ALIAS.getKey(), generate(alias));
        }

        private void namespace(ObjectNode node, HasNamespace namespaced) {
            node.put(Keys.PARENT.getKey(), generate(namespaced.namespace()));
        }

        private void component(ObjectNode node, HasComponent componented) {
            node.put(Keys.COMPONENT.getKey(), generate(componented.component()));
        }

        private void id(ObjectNode node, MayHaveId ided) {
            ided.id().ifPresent(id -> node.put(Keys.ID.getKey(), id));
        }

        private ObjectNode display(Map<Language, String> display) {
            ObjectNode node = nodeFactory.objectNode();
            display.forEach((l, s) -> node.put(l.code(), s));
            return node;
        }

        private void pathElement(ObjectNode proxy, ProxyPath path) {
            if (path instanceof ProxyPath.FqnElement) {
                ProxyPath.FqnElement el = (ProxyPath.FqnElement) path;
                proxy.put(Keys.NAMESPACE.getKey(), el.ns().mangledNameString());
                ProxyElementName element = el.element();
                if (element instanceof GenericTypeName) {
                    GenericTypeName gtn = (GenericTypeName) element;
                    proxy.put(Keys.TYPE_NAME.getKey(), gtn.typeName().mangledNameString());
                    ArrayNode gaps = proxy.putArray(Keys.GENERIC_ARGUMENT_PATHS.getKey());
                    gtn.genericArgumentPaths().forEach(p -> pathElement(gaps.addObject(), p));
                } else {
                    proxy.put(Keys.TYPE_NAME.getKey(), ((TypeName) element).typeName().mangledNameString());
                }
            } else {
                ProxyPath.Literal literal = (ProxyPath.Literal) path;
                proxy.put(Keys.VALUE.getKey(), literal.value());
            }
        }

        private Integer findOrCreate(Referenceable referenceable, Supplier<ObjectNode> supplier) {
            if (supplier == null)
                return null;
            Integer index = map.get(referenceable);
            if (index == null) {
                index = list.size();
                list.add(null);
                map.put(referenceable, index);
                list.set(index, supplier.get());
            }
            return index;
        }

        private Integer generate(Referenceable referenceable) {
            return findOrCreate(referenceable, referenceable.accept(new ReferenceableVisitor<Supplier<ObjectNode>>() {
                @Override
                public Supplier<ObjectNode> visit(DecodeType t) {
                    return () -> {
                        if (t instanceof NativeType)
                            return extendedNodeKind(Keys.NATIVE_TYPE.getKey(), t);
                        else if (t instanceof SubType) {
                            SubType subType = (SubType) t;
                            ObjectNode node = extendedNodeKind(Keys.SUB_TYPE.getKey(), t)
                                    .put(Keys.PARENT.getKey(), generate(subType.namespace()));
                            typeMeasure(node.putObject(Keys.TYPE_MEASURE.getKey()), subType.typeMeasure());
                            return node;
                        } else if (t instanceof Const) {
                            Const aConst = (Const) t;
                            ObjectNode node = extendedNodeKind(Keys.CONST.getKey(), t)
                                    .put(Keys.VALUE.getKey(), aConst.value())
                                    .put(Keys.PARENT.getKey(), generate(aConst.namespace()));
                            if (!aConst.typeParameters().isEmpty()) {
                                ArrayNode tp = node.putArray(Keys.TYPE_PARAMETERS.getKey());
                                aConst.typeParameters().forEach(p -> tp.add(p.mangledNameString()));
                            }
                            return node;
                        } else if (t instanceof GenericTypeSpecialized) {
                            GenericTypeSpecialized genericTypeSpecialized = (GenericTypeSpecialized) t;
                            ObjectNode node = extendedNodeKind(Keys.GENERIC_TYPE_SPECIALIZED.getKey(), t)
                                    .put(Keys.PARENT.getKey(), generate(genericTypeSpecialized.namespace()));
                            typeProxy(node.putObject(Keys.GENERIC_TYPE_PROXY.getKey()), genericTypeSpecialized.genericTypeProxy());
                            ArrayNode typeArguments = node.putArray(Keys.TYPE_ARGUMENTS.getKey());
                            genericTypeSpecialized.genericTypeArgumentsProxy().forEach(a -> typeArguments.add(generate(a.obj())));
                            return node;
                        } else
                            throw new AssertionError();
                    };
                }

                @Override
                public Supplier<ObjectNode> visit(Alias a) {
                    return () -> {
                        ObjectNode node = extendedNodeKind(Keys.ALIAS.getKey(), a)
                            .put(Keys.NAME.getKey(), a.name().mangledNameString())
                            .put(Keys.PARENT.getKey(), generate(a.parent()))
                            .put(Keys.OBJ.getKey(), generate(a.referenceable()));
                        if (!a.info().isEmpty())
                            node.set(Keys.INFO.getKey(), display(a.info()));
                        return node;
                    };
                }

                @Override
                public Supplier<ObjectNode> visit(TmParameter p) {
                    return () -> {
                        if (p instanceof StatusParameter) {
                            ObjectNode node = extendedNodeKind(Keys.STATUS_PARAMETER.getKey(), p);
                            ArrayNode elementsNode = node.putArray(Keys.ELEMENTS.getKey());
                            ((StatusParameter) p).path().elements().forEach(el -> elementsNode.add(el.arrayRange().<JsonNode>map(a -> {
                                ObjectNode arrayRangeNode = nodeFactory.objectNode();
                                arrayRangeNode.put(Keys.MIN.getKey(), a.min().longValue());
                                a.max().ifPresent(m -> arrayRangeNode.put(Keys.MAX.getKey(), m.longValue()));
                                return arrayRangeNode;
                            }).orElseGet(() -> nodeFactory.textNode(el.elementName().get().mangledNameString()))));
                            return node;
                        } else {
                            ObjectNode node = extendedNodeKind(Keys.PARAMETER.getKey(), p);
                            typeMeasure(node.putObject(Keys.TYPE_MEASURE.getKey()), ((Parameter) p).typeMeasure());
                            return node;
                        }
                    };
                }

                @Override
                public Supplier<ObjectNode> visit(Container c) {
                    return c.accept(new ContainerVisitor<Supplier<ObjectNode>>() {
                        @Override
                        public Supplier<ObjectNode> visit(Namespace namespace) {
                            return () -> {
                                ObjectNode namespaceNode = extendedNodeKind(Keys.NAMESPACE.getKey(), namespace);
                                namespace.parent().ifPresent(parent -> namespaceNode.put(Keys.PARENT.getKey(), generate(parent)));
                                return namespaceNode;
                            };
                        }

                        @Override
                        public Supplier<ObjectNode> visit(Command command) {
                            return () -> {
                                ObjectNode node = extendedNodeKind(Keys.COMMAND.getKey(), command)
                                        .put(Keys.PARENT.getKey(), generate(command.component()));

                                typeMeasure(node.putObject(Keys.RETURN_TYPE_MEASURE.getKey()), command.returnTypeMeasure());
                                return node;
                            };
                        }

                        @Override
                        public Supplier<ObjectNode> visit(EnumType enumType) {
                            throw new AssertionError("must be unreachable");
                        }

                        @Override
                        public Supplier<ObjectNode> visit(StructType structType) {
                            throw new AssertionError("must be unreachable");
                        }

                        @Override
                        public Supplier<ObjectNode> visit(TmMessage tmMessage) {
                            return () -> {
                                if (tmMessage instanceof StatusMessage) {
                                    ObjectNode node = extendedNodeKind(Keys.STATUS_MESSAGE.getKey(), tmMessage)
                                            .put(Keys.PARENT.getKey(), generate(tmMessage.component()));
                                    ((StatusMessage) tmMessage).priority().ifPresent(p -> node.put(Keys.PRIORITY.getKey(), p));
                                    return node;
                                } else {
                                    ObjectNode node = extendedNodeKind(Keys.EVENT_MESSAGE.getKey(), tmMessage)
                                            .put(Keys.PARENT.getKey(), generate(tmMessage.component()));
                                    typeProxy(node.putObject(Keys.BASE_TYPE_PROXY.getKey()),
                                            ((EventMessage) tmMessage).baseTypeProxy());
                                    return node;
                                }
                            };
                        }

                        @Override
                        public Supplier<ObjectNode> visit(Component component) {
                            return () -> {
                                ObjectNode componentNode = extendedNodeKind(Keys.COMPONENT.getKey(), component);
                                component.baseType().ifPresent(t -> componentNode.put(Keys.BASE_TYPE.getKey(), generate(t)));
                                return componentNode;
                            };
                        }
                    });
                }

                @Override
                public Supplier<ObjectNode> visit(EnumConstant c) {
                    return () -> extendedNodeKind(Keys.ENUM_CONSTANT.getKey(), c).put(Keys.VALUE.getKey(), generate(c.value()));
                }

                @Override
                public Supplier<ObjectNode> visit(EnumType e) {
                    return () -> {
                        ObjectNode enumNode = extendedNodeKind(Keys.ENUM_TYPE.getKey(), e);
                        e.baseTypeMeasureOptional().ifPresent(typeMeasure ->
                                typeMeasure(enumNode.putObject(Keys.BASE_TYPE_MEASURE.getKey()), typeMeasure));
                        e.extendsTypeOption().ifPresent(extendsType ->
                                enumNode.put(Keys.EXTENDS_TYPE.getKey(), generate(extendsType)));
                        enumNode.put(Keys.IS_FINAL.getKey(), e.isFinal());
                        return enumNode;
                    };
                }

                @Override
                public Supplier<ObjectNode> visit(Measure m) {
                    // ObjectNode#set returns ObjectNode
                    return () -> (ObjectNode) extendedNodeKind(Keys.MEASURE.getKey(), m)
                            .put(Keys.PARENT.getKey(), generate(m.namespace()))
                            .set(Keys.DISPLAY.getKey(), display(m.display()));
                }

                @Override
                public Supplier<ObjectNode> visit(MaybeProxy p) {
                    throw new AssertionError("must be unreachable");
                }

                @Override
                public Supplier<ObjectNode> visit(Registry r) {
                    throw new AssertionError("must be unreachable");
                }

                @Override
                public Supplier<ObjectNode> visit(ConstExpr e) {
                    return () -> extendedNodeKind(Keys.CONST_EXPR.getKey(), e).put(Keys.VALUE.getKey(), e.exprStringRepr());
                }

                @Override
                public Supplier<ObjectNode> visit(StructField f) {
                    return () -> {
                        ObjectNode node = extendedNodeKind(Keys.STRUCT_FIELD.getKey(), f)
                                .put(Keys.INDEX.getKey(), f.index());
                        typeMeasure(node.putObject(Keys.TYPE_MEASURE.getKey()), f.typeMeasure());
                        return node;
                    };
                }

                @Override
                public Supplier<ObjectNode> visit(StructType s) {
                    return () -> extendedNodeKind(Keys.STRUCT_TYPE.getKey(), s);
                }

                @Override
                public Supplier<ObjectNode> visit(TypeMeasure tm) {
                    throw new AssertionError("must be unreachable");
                }
            }));
        }

        private void typeMeasure(ObjectNode node, TypeMeasure typeMeasure)
        {
            MaybeTypeProxy maybeTypeProxy = typeMeasure.typeProxy();
            if (maybeTypeProxy.isResolved())
                node.put(Keys.TYPE.getKey(), generate(maybeTypeProxy.obj()));
            else
                pathElement(node.putObject(Keys.TYPE_PROXY_PATH.getKey()), maybeTypeProxy.proxy().path());
            typeMeasure.measureProxy().ifPresent(mp -> node.put(Keys.MEASURE.getKey(), generate(mp.obj())));
        }

        private void typeProxy(ObjectNode node, MaybeTypeProxy maybeTypeProxy)
        {
            node.put(Keys.TYPE.getKey(), maybeTypeProxy.accept(new MaybeProxyVisitor<Keys>()
            {
                @Override
                public Keys visit(MaybeProxyNamespace n)
                {
                    return Keys.MAYBE_PROXY_NAMESPACE;
                }

                @Override
                public Keys visit(MaybeProxyType tt) {
                    return Keys.MAYBE_TYPE_PROXY_TYPE;
                }

                @Override
                public Keys visit(MaybeProxyConst c)
                {
                    return Keys.MAYBE_PROXY_CONST;
                }

                @Override
                public Keys visit(MaybeProxyEnumType e) {
                    return Keys.MAYBE_PROXY_ENUM;
                }

                @Override
                public Keys visit(MaybeProxyStructType s) {
                    return Keys.MAYBE_PROXY_STRUCT;
                }

                @Override
                public Keys visit(MaybeProxyComponent c) {
                    return Keys.MAYBE_PROXY_COMPONENT;
                }

                @Override
                public Keys visit(MaybeProxyMeasure m) {
                    return Keys.MAYBE_PROXY_MEASURE;
                }

                @Override
                public Keys visit(MaybeProxyReferenceable r) {
                    return Keys.MAYBE_PROXY_REFERENCEABLE;
                }
            }).getKey());
            if (maybeTypeProxy.isResolved())
                node.put(Keys.OBJ.getKey(), generate(maybeTypeProxy.obj()));
            else {
                ProxyPath path = maybeTypeProxy.proxy().path();
                ObjectNode proxy = node.putObject(Keys.PROXY.getKey());
                pathElement(proxy, path);
            }
        }

    }

}
