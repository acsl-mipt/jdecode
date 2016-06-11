package ru.mipt.acsl.decode.generator.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.mipt.acsl.decode.model.*;
import ru.mipt.acsl.decode.model.component.Command;
import ru.mipt.acsl.decode.model.component.Component;
import ru.mipt.acsl.decode.model.component.HasComponent;
import ru.mipt.acsl.decode.model.component.message.TmMessage;
import ru.mipt.acsl.decode.model.expr.ConstExpr;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxy;
import ru.mipt.acsl.decode.model.registry.Measure;
import ru.mipt.acsl.decode.model.registry.Registry;
import ru.mipt.acsl.decode.model.types.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DecodeJsonGenerator {

    private final DecodeJsonGeneratorConfig config;

    public static DecodeJsonGenerator newInstance(DecodeJsonGeneratorConfig config) {
        return new DecodeJsonGenerator(config);
    }

    private DecodeJsonGenerator(DecodeJsonGeneratorConfig config) {
        this.config = config;
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

        private ObjectNode nodeKind(String kind) {
            return nodeFactory.objectNode().put("kind", kind);
        }

        private void objects(ObjectNode node, Container container) {
            node.putArray("objects").addAll(
                    container.objects().stream()
                            .map(r -> nodeFactory.numberNode(generate(r)))
                            .collect(Collectors.toList()));
        }

        private <T extends Alias> void alias(ObjectNode node, T alias) {
            node.put("alias", generate(alias));
        }

        private void namespace(ObjectNode node, HasNamespace namespaced) {
            node.put("namespace", generate(namespaced.namespace()));
        }

        private void component(ObjectNode node, HasComponent componented) {
            node.put("component", generate(componented.component()));
        }

        private void id(ObjectNode node, MayHaveId ided) {
            ided.id().ifPresent(id -> node.put("id", id));
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
            return referenceable.accept(new ReferenceableVisitor<Integer>() {
                @Override
                public Integer visit(DecodeType t) {
                    return null;
                }

                @Override
                public Integer visit(Alias a) {
                    return null;
                }

                @Override
                public Integer visit(TmParameter p) {
                    return null;
                }

                @Override
                public Integer visit(Container c) {
                    return findOrCreate(c, c.accept(new ContainerVisitor<Supplier<ObjectNode>>() {
                        @Override
                        public Supplier<ObjectNode> visit(Namespace namespace) {
                            return () -> {
                                ObjectNode namespaceNode = nodeKind("namespace");
                                alias(namespaceNode, namespace.alias());
                                namespace.parent().ifPresent(parent -> namespaceNode.put("parent", generate(parent)));
                                objects(namespaceNode, namespace);
                                return namespaceNode;
                            };
                        }

                        @Override
                        public Supplier<ObjectNode> visit(Command command) {
                            return () -> {
                                ObjectNode commandNode = nodeKind("command");
                                alias(commandNode, command.alias());
                                commandNode.put("returnType", generate(command.returnType()));
                                objects(commandNode, command);
                                return commandNode;
                            };
                        }

                        @Override
                        public Supplier<ObjectNode> visit(EnumType enumType) {
                            return () -> {
                                ObjectNode enumNode = nodeKind("enumType");
                                alias(enumNode, enumType.alias());
                                namespace(enumNode, enumType);
                                enumType.baseTypeOption().ifPresent(baseType ->
                                        enumNode.put("baseType", generate(baseType)));
                                enumType.extendsTypeOption().ifPresent(extendsType ->
                                        enumNode.put("extendsType", generate(extendsType)));
                                enumNode.put("isFinal", enumType.isFinal());
                                objects(enumNode, enumType);
                                return enumNode;
                            };
                        }

                        @Override
                        public Supplier<ObjectNode> visit(StructType structType) {
                            return () -> {
                                ObjectNode structNode = nodeKind("structType");
                                alias(structNode, structType.alias());
                                namespace(structNode, structType);
                                objects(structNode, structType);
                                return structNode;
                            };
                        }

                        @Override
                        public Supplier<ObjectNode> visit(TmMessage tmMessage) {
                            return () -> {
                                ObjectNode tmMessageNode = nodeKind("tmMessage");
                                alias(tmMessageNode, tmMessage.alias());
                                component(tmMessageNode, tmMessage);
                                id(tmMessageNode, tmMessage);
                                objects(tmMessageNode, tmMessage);
                                return tmMessageNode;
                            };
                        }

                        @Override
                        public Supplier<ObjectNode> visit(Component component) {
                            return () -> {
                                ObjectNode componentNode = nodeKind("component");
                                alias(componentNode, component.alias());
                                namespace(componentNode, component);
                                component.baseType().ifPresent(t -> componentNode.put("baseType", generate(t)));
                                id(componentNode, component);
                                objects(componentNode, component);
                                return componentNode;
                            };
                        }
                    }));
                }

                @Override
                public Integer visit(EnumConstant c) {
                    return null;
                }

                @Override
                public Integer visit(EnumType e) {
                    return null;
                }

                @Override
                public Integer visit(Measure m) {
                    return null;
                }

                @Override
                public Integer visit(MaybeProxy p) {
                    return null;
                }

                @Override
                public Integer visit(Registry r) {
                    return null;
                }

                @Override
                public Integer visit(ConstExpr e) {
                    return null;
                }

                @Override
                public Integer visit(StructField f) {
                    return null;
                }

                @Override
                public Integer visit(StructType s) {
                    return null;
                }
            });
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
