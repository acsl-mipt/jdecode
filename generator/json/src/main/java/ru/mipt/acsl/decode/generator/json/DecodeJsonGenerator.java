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
import ru.mipt.acsl.decode.model.component.message.TmMessage;
import ru.mipt.acsl.decode.model.expr.ConstExpr;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxy;
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath;
import ru.mipt.acsl.decode.model.registry.Language;
import ru.mipt.acsl.decode.model.registry.Measure;
import ru.mipt.acsl.decode.model.registry.Registry;
import ru.mipt.acsl.decode.model.types.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
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
                rootNode.put("generated", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

                rootNode.set("objects", new StatefulDecodeJsonGenerator(nodeFactory, rootComponents).generate());

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
        TYPE_PROXY("p"), MEASURE("m"), CONST("const"), GENERIC_TYPE_SPECIALIZED("gts"), GENERIC_TYPE("gt"),
        TYPE_ARGUMENTS("as"), STATUS_PARAMETER("sp"), ELEMENTS("e"), MIN("min"), MAX("max"), PARAMETER("p"),
        COMMAND("cmd"), RETURN_TYPE("rt"), TM_MESSAGE("tm"), ENUM_CONSTANT("ec"), ENUM_TYPE("e"), EXTENDS_TYPE("et"),
        IS_FINAL("f"), MAYBE_PROXY("mp"), ELEMENT_NAME("e"), PROXY("p"), CONST_EXPR("ex"), STRUCT_FIELD("f"),
        STRUCT_TYPE("st");

        public String getKey() {
            return key;
        }

        private final String key;

        private Keys(String key) {
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
            for (Component component : rootComponents)
                generate(component);
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
            node.put(Keys.NAMESPACE.getKey(), generate(namespaced.namespace()));
        }

        private void component(ObjectNode node, HasComponent componented) {
            node.put(Keys.COMPONENT.getKey(), generate(componented.component()));
        }

        private void id(ObjectNode node, MayHaveId ided) {
            ided.id().ifPresent(id -> node.put("id", id));
        }

        private ObjectNode display(Map<Language, String> display) {
            ObjectNode node = nodeFactory.objectNode();
            display.forEach((l, s) -> node.put(l.code(), s));
            return node;
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
                        else if (t instanceof SubType)
                            return extendedNodeKind(Keys.SUB_TYPE.getKey(), t)
                                    .put(Keys.TYPE_MEASURE.getKey(), generate(((SubType)t).typeMeasure()));
                        else if (t instanceof TypeMeasure) {
                            TypeMeasure typeMeasure = (TypeMeasure) t;
                            ObjectNode node = extendedNodeKind(Keys.TYPE_MEASURE.getKey(), t);
                            node.put(Keys.TYPE_PROXY.getKey(), generate(typeMeasure.typeProxy()));
                            typeMeasure.measure().ifPresent(m -> node.put(Keys.MEASURE.getKey(), generate(m)));
                            return node;
                        } else if (t instanceof Const)
                            return extendedNodeKind(Keys.CONST.getKey(), t).put(Keys.VALUE.getKey(), ((Const)t).value());
                        else if (t instanceof GenericTypeSpecialized) {
                            GenericTypeSpecialized genericTypeSpecialized = (GenericTypeSpecialized) t;
                            ObjectNode node = extendedNodeKind(Keys.GENERIC_TYPE_SPECIALIZED.getKey(), t)
                                    .put(Keys.GENERIC_TYPE.getKey(), generate(genericTypeSpecialized.genericType()));
                            ArrayNode typeArguments = node.putArray(Keys.TYPE_ARGUMENTS.getKey());
                            genericTypeSpecialized.genericTypeArguments().forEach(a -> typeArguments.add(generate(a)));
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
                            .put(Keys.OBJ.getKey(), generate(a.obj()));
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
                            return extendedNodeKind(Keys.PARAMETER.getKey(), p)
                                    .put(Keys.TYPE_MEASURE.getKey(), generate(((Parameter) p).typeMeasure()));
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
                            return () ->
                                extendedNodeKind(Keys.COMMAND.getKey(), command)
                                        .put(Keys.RETURN_TYPE.getKey(), generate(command.returnType()));
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
                            return () -> extendedNodeKind(Keys.TM_MESSAGE.getKey(), tmMessage);
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
                        e.baseTypeOption().ifPresent(baseType ->
                                enumNode.put(Keys.BASE_TYPE.getKey(), generate(baseType)));
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
                            .set(Keys.DISPLAY.getKey(), display(m.display()));
                }

                @Override
                public Supplier<ObjectNode> visit(MaybeProxy p) {
                    return () -> {
                        ObjectNode node = nodeKind(Keys.MAYBE_PROXY.getKey());
                        if (p.isResolved())
                            node.put(Keys.OBJ.getKey(), generate(p.obj()));
                        else {
                            ProxyPath path = p.proxy().path();
                            ObjectNode proxy = nodeFactory.objectNode();
                            if (path instanceof ProxyPath.FqnElement) {
                                ProxyPath.FqnElement el = (ProxyPath.FqnElement) path;
                                proxy.put(Keys.NAMESPACE.getKey(), el.ns().mangledNameString());
                                proxy.put(Keys.ELEMENT_NAME.getKey(), el.mangledName().mangledNameString());
                            } else {
                                ProxyPath.Literal literal = (ProxyPath.Literal) path;
                                proxy.put(Keys.VALUE.getKey(), literal.value());
                            }
                            node.put(Keys.PROXY.getKey(), proxy);
                        }
                        return node;
                    };
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
                    return () -> extendedNodeKind(Keys.STRUCT_FIELD.getKey(), f)
                            .put(Keys.TYPE_MEASURE.getKey(), generate(f.typeMeasure()));
                }

                @Override
                public Supplier<ObjectNode> visit(StructType s) {
                    return () -> extendedNodeKind(Keys.STRUCT_TYPE.getKey(), s);
                }
            }));
        }

    }
/*
  private class StatefulDecodeJsonGenerator {

    case class MapBuffer[T, JT](map: mutable.Map[T, Int] = mutable.Map[T, Int](), buffer: mutable.Buffer[JT] = mutable.Buffer[JT]())

    val objects = MapBuffer[Referenceable, Json.Referenceable]()

    def generateRootComponents(cs: Seq[Component]): Json.Root = {
      cs.foreach(generate)
      Json.Root(, objects.buffer)
    }

    private def generate(c: Component): Int =
      generate(c,
        Json.Component(generate(c.alias), generate(c.namespace), c.baseType.map(generate), c.objects.map(generate)))

    private def constExpr(e: ConstExpr): Json.ConstExpr = e match {
      case f: BigDecimalLiteral => Json.NumberLiteral(f.value.toString)
      case i: BigIntLiteral => Json.NumberLiteral(i.value.toString)
      case _ => sys.error("not implemented")
    }

    private def name(name: ElementName): String = name.mangledNameString()

    private def name(named: HasName): String = named.name.mangledNameString()

    private def info(info: util.Map[Language, String]): Json.LocalizedString = localizedString(info)

    private def info(obj: HasInfo): Json.LocalizedString = localizedString(obj.info)

    private def pathElement(p: MessageParameterPathElement): Json.ParameterPathElement = p.isElementName match {
      case true => Json.ElementName(p.elementName().get().mangledNameString())
      case _ =>
        val arrayRange = p.arrayRange().get()
        Json.ArrayRange(arrayRange.min.toString, arrayRange.max.map(_.toString))
    }

    private def localizedString(info: util.Map[Language, String]): Json.LocalizedString =
      Json.LocalizedString(info.map{ i => i._1.code() -> i._2}.toMap)

    private def typeMeasure(tm: TypeMeasure): Json.TypeMeasure =
      Json.TypeMeasure(generate(tm.t), Option(tm.measure.orElse(null)).map(generate))

    private def structField(f: StructField): Json.StructField =
      Json.StructField(generate(f.alias), generate(f.typeMeasure))

    private def enumConst(c: EnumConstant): Json.EnumConst =
      Json.EnumConst(generate(c.alias), constExpr(c.value))

    private def generate(obj: Referenceable, makeJObj: => Json.Referenceable): Int = {
      for (id <- objects.map.get(obj))
        return id
      val idx = objects.buffer.size
      objects.buffer += null
      objects.map(obj) = idx
      objects.buffer(idx) = makeJObj
      idx
    }

    private def generate(r: Referenceable): Int =
      generate(r, {
        r match {
          case ns: Namespace => Json.Namespace(generate(ns.alias), Option(ns.parent.orElse(null)).map(generate))
          case m: Measure => Json.Measure(generate(m.alias), localizedString(m.display))
          case s: SubType => Json.SubType(Option(s.alias).map(generate), generate(s.namespace), generate(s.baseType))
          case n: NativeType => Json.NativeType(Option(n.alias).map(generate).getOrElse(sys.error("must have an alias")), generate(n.namespace))
          case s: StructType => Json.StructType(Option(s.alias).map(generate), generate(s.namespace), s.objects.map(generate))
          case s: GenericTypeSpecialized => Json.GenericTypeSpecialized(Option(s.alias).map(generate), generate(s.namespace),
            generate(s.genericType), s.genericTypeArguments.map(generate))
          case e: EnumType => new Json.EnumType(Option(e.alias).map(generate), generate(e.namespace),
            Option(e.extendsTypeOption.orElse(null)).map(generate), Option(e.baseTypeOption.orElse(null)).map(generate), e.isFinal,
            e.objects.map(generate))
          case c: Const => new Json.Const(Option(c.alias).map(generate).getOrElse(sys.error("must have an alias")), generate(c.namespace), c.value)
          case tm: TypeMeasure => new Json.TypeMeasure(generate(tm.t), Option(tm.measure.orElse(null)).map(generate))
          case a: Alias[_, _] => Json.Alias(name(a.name), info(a.info), generate(a.parent), 0)
          case ec: EnumConstant => Json.EnumConst(generate(ec.alias), constExpr(ec.value))
          case c: Command => Json.Command(generate(c.alias), c.objects.map(generate), generate(c.returnType))
          case com: CommandOrTmMessage => com.isCommand match {
            case true => val c = com.command().get(); Json.Command(generate(c.alias()), c.objects().map(generate), generate(c.returnType))
            case _ => com.tmMessage().get() match {
              case e: EventMessage => eventMessage(e)
              case s: StatusMessage => statusMessage(s)
            }
          }
          case e: EventMessage => eventMessage(e)
          case s: StatusMessage => statusMessage(s)
          case s: StatusParameter => Json.StatusParameter(info(s.info), s.path.elements().map(pathElement))
          case p: Parameter => Json.Parameter(generate(p.alias), generate(p.typeMeasure))
          case f: StructField => Json.StructField(generate(f.alias), generate(f.typeMeasure))
          case _ => sys.error(s"not implemented for $r")
        }
      })

    def eventMessage(e: EventMessage): Json.EventMessage =
      Json.EventMessage(generate(e.alias), generate(e.baseType), Option(e.id.toInt), e.objects.map(generate))

    def statusMessage(s: StatusMessage): Json.StatusMessage =
      Json.StatusMessage(generate(s.alias), Option(s.id).map(_.toInt), Option(s.priority).map(_.toInt),
        s.objects.map(generate))

  }*/

}
