package ru.mipt.acsl.decode.mavlink.generator;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import ru.mipt.acsl.generation.Generatable;
import ru.mipt.acsl.generation.GenerationException;
import ru.mipt.acsl.generation.Generator;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class MavlinkDecodeSourceGenerator implements Generator<MavlinkDecodeSourceGeneratorConfiguration>
{
    public static final String COMMAND_LONG = "COMMAND_LONG";
    public static final String MAV_CMD = "MAV_CMD";
    @NotNull
    private final MavlinkDecodeSourceGeneratorConfiguration config;
    @NotNull
    private final Set<String> types = new HashSet<>();
    @NotNull
    private final List<Message> messages = new ArrayList<>();
    @NotNull
    private final Map<String, MavEnum> enums = new HashMap<>();
    @NotNull
    private final Multimap<String, String> concreteEnums = MultimapBuilder.hashKeys().hashSetValues().build();

    public static void main(String[] args)
    {
        MavlinkDecodeSourceGeneratorConfiguration config = new MavlinkDecodeSourceGeneratorConfiguration();
        CmdLineParser parser = new CmdLineParser(config);
        try
        {
            parser.parseArgument(Arrays.asList(args));
            new MavlinkDecodeSourceGenerator(config).generate();
        }
        catch (CmdLineException e)
        {
            System.out.println(ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }
    }

    public MavlinkDecodeSourceGenerator(@NotNull MavlinkDecodeSourceGeneratorConfiguration config)
    {
        this.config = config;
    }

    public void generate()
    {
        try
        {
            File outputFile = config.getOutputFile();
            if (outputFile.exists())
            {
                Preconditions.checkState(outputFile.delete());
            }
            try (Writer writer = new FileWriter(outputFile))
            {
                writer.append(String.format("namespace %s", config.getNamespace()));
                eol(writer);
                File inputFile = config.getInputFile();
                processFile(inputFile);
                eol(writer);
                writer.append("alias ^float float:32");
                eol(writer);
                writer.append("alias char uint:8");
                eol(writer);
                types.stream().forEach(t -> generateType(t, writer));
                messages.stream().forEach(s -> s.generate(writer));
                concreteEnums.entries().stream().forEach(e -> generateConcreteEnum(e.getKey(), e.getValue(), writer));

                // Component
                eol(writer);
                writer.append("component ").append(makeComponentName(inputFile.getName().split(Pattern.quote("."))[0]));
                writer.append(" {");
                eol(writer);
                writer.append("\tstruct(");
                eol(writer);
                for (Message msg : messages)
                {
                    String typeName = msg.getTypeName();
                    writer.append("\t\t").append(typeName)
                            .append(StringUtils.repeat(" ", Math.max(1, 40 - typeName.length())))
                            .append("_").append(Integer.toString(msg.getId())).append(",");
                    eol(writer);
                }
                writer.append("\t)");
                eol(writer);
                Optional<Message>
                        commandMessage = messages.stream().filter(m -> m.getName().equals(COMMAND_LONG)).findAny();
                if (commandMessage.isPresent())
                {
                    MavEnum cmdEnum = Preconditions.checkNotNull(enums.get(MAV_CMD));
                    for (MavEnumConstant constant : cmdEnum.getConstants())
                    {
                        eol(writer);
                        String cmdNameOriginal = constant.getName();
                        if (cmdNameOriginal.startsWith(MAV_CMD))
                        {
                            cmdNameOriginal = cmdNameOriginal.substring(MAV_CMD.length() + 1);
                        }
                        writer.append("\tcommand ")
                                .append(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, cmdNameOriginal))
                                .append(":").append(constant.getValue().get().toString()).append("(");
                        eol(writer);
                        for (StructField field : commandMessage.get().getFields())
                        {
                            String fieldName = field.getName();
                            String typeName = field.getTypeName();
                            writer.append("\t\t").append(typeName)
                                    .append(StringUtils.repeat(" ", Math.max(1, 20 - typeName.length())))
                                    .append(fieldName);
                            if (fieldName.startsWith("param"))
                            {
                                int paramNum = Integer.parseInt(fieldName.substring("param".length()));
                                Optional<MavEnumCostantParam>
                                        parameter = constant.getParams().stream().filter(p -> p.getIndex() == paramNum).findAny();
                                if (parameter.isPresent() || field.getDescription().isPresent())
                                {
                                    writer.append(" info '").append(escapeUnaryQuotesString(parameter.isPresent() ?
                                            parameter.get().getInfo() : field.getDescription().get())).append("'");
                                }
                            }
                            writer.append(",");
                            eol(writer);
                        }
                        writer.append("\t)");
                        eol(writer);
                    }
                }
                eol(writer);
                for (Message msg : messages)
                {
                    String originalName = msg.getName();
                    if (originalName.equals(COMMAND_LONG))
                    {
                        continue;
                    }
                    String idString = Integer.toString(msg.getId());
                    String name = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, originalName);
                    writer.append("\tmessage ").append(name).append(":").append(idString)
                            .append(StringUtils.repeat(" ",
                                    Math.max(1, 40 - name.length() - idString.length())))
                            .append("status (").append("_").append(idString).append(")");
                    eol(writer);
                }
                writer.append("}");
                eol(writer);

            }
        }
        catch (JDOMException | IOException e)
        {
            throw new GenerationException(e);
        }
    }

    @NotNull
    @Override
    public MavlinkDecodeSourceGeneratorConfiguration getConfiguration()
    {
        return config;
    }

    @NotNull
    private static String makeComponentName(@NotNull String name)
    {
        return StringUtils.capitalize(name);
    }

    private void generateConcreteEnum(@NotNull String typeName, @NotNull String enumName,
                                      @NotNull Appendable appendable)
    {
        try
        {
            eol(appendable);
            appendable.append("type ").append(makeTypeNameForEnum(typeName, enumName))
                    .append(" enum ").append(typeName);
            MavEnum anEnum = enums.get(enumName);
            if (anEnum.getInfo().isPresent())
            {
                appendable.append(" info '").append(escapeUnaryQuotesString(anEnum.getInfo().get())).append("'");
            }
            appendable.append(" (");
            eol(appendable);
            anEnum.getConstants().forEach(c -> {
                try
                {
                    appendable.append("\t").append(c.getName());
                    int width = c.getName().length();
                    if (c.getValue().isPresent())
                    {
                        String literal = c.getValue().get().toString();
                        appendable.append(" = ").append(literal);
                        width += literal.length() + 3;
                    }
                    if (c.getInfo().isPresent())
                    {
                        appendable.append(StringUtils.repeat(" ", Math.max(1, 40 - width)));
                        appendable.append(" info '").append(escapeUnaryQuotesString(c.getInfo().get())).append("'");
                    }
                    appendable.append(",");
                    eol(appendable);
                }
                catch (IOException e)
                {
                    throw new GenerationException(e);
                }
            });
            appendable.append(")");
            eol(appendable);
        }
        catch (IOException e)
        {
            throw new GenerationException(e);
        }
    }

    private void processFile(@NotNull File inputFile) throws IOException, JDOMException
    {
        Document document = new SAXBuilder().build(inputFile);
        for (Element element : document.getRootElement().getChildren())
        {
            switch (element.getName())
            {
                case "include":
                    processFile(new File(inputFile.getParent(), element.getText()));
                    break;
                case "version":
                    break;
                case "enums":
                    element.getChildren("enum").stream().map(this::processEnum).forEach(e -> {
                        String name = e.getName();
                        if (enums.containsKey(name))
                        {
                            enums.get(name).merge(e);
                        }
                        else
                        {
                            enums.put(name, e);
                        }
                    });
                    break;
                case "messages":
                    messages.addAll(element.getChildren("message").stream().<Message>map(this::processMessage)
                            .collect(Collectors.toList()));

                    break;
                default:
                    throw new GenerationException(String.format("Unexpected tag '%s'", element.getName()));
            }
        }
    }

    private MavEnum processEnum(@NotNull Element element)
    {
        return new MavEnum(element.getAttribute("name").getValue(),
                Optional.ofNullable(element.getChild("description")).map(Element::getText),
                element.getChildren("entry").stream()
                        .map(e -> new MavEnumConstant(e.getAttribute("name").getValue(),
                                Optional.ofNullable(e.getAttribute("value")).map(Attribute::getValue)
                                        .map(Integer::parseInt),
                                Optional.ofNullable(e.getChild("description")).map(Element::getText),
                                e.getChildren("param").stream().map(p -> new MavEnumCostantParam(
                                        Integer.parseInt(p.getAttribute("index").getValue()), p.getText()))
                                        .collect(Collectors.toList()))).collect(
                        Collectors.toList()));
    }

    private void generateType(@NotNull String typeName, @NotNull Appendable appendable)
    {
        Matcher matcher = Pattern.compile("([u]?int)([1-9][0-9]?)_t[a-zA-Z_0-9]*").matcher(typeName);
        try
        {
            if (matcher.matches())
            {
                appendable.append("alias ").append(typeName).append(" ").append(matcher.group(1)).append(":")
                        .append(matcher.group(2));
                eol(appendable);
            }
        }
        catch (IOException e)
        {
            throw new GenerationException(e);
        }
    }

    @NotNull
    private Message processMessage(@NotNull Element message)
    {
        try
        {
            int id = message.getAttribute("id").getIntValue();

            String name = message.getAttributeValue("name");
            Element description = message.getChild("description");

            List<StructField> fields = new ArrayList<>();
            for (Element field : message.getChildren("field"))
            {
                String type = field.getAttributeValue("type");
                String fieldName = field.getAttributeValue("name");
                String fieldDescription = field.getText();
                String fieldNameFormatted =
                        escapeIfKeyword(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, fieldName));
                if (type.contains("["))
                {
                    int index = type.indexOf('[');
                    String baseType = escapeIfKeyword(type.substring(0, index));
                    type = "[" + baseType + ", " + type.substring(index + 1);
                    types.add(baseType);
                }
                else
                {
                    type = escapeIfKeyword(type);
                    types.add(type);
                }
                Optional<String> anEnum = Optional.ofNullable(field.getAttribute("enum")).map(Attribute::getValue);
                if (anEnum.isPresent())
                {
                    concreteEnums.put(type, anEnum.get());
                }
                fields.add(new StructField(type, fieldNameFormatted, Optional.ofNullable(fieldDescription).map(
                        MavlinkDecodeSourceGenerator::escapeUnaryQuotesString),
                        anEnum));
            }

            return new Message(name, escapeIfKeyword(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, name)),
                    id, Optional.ofNullable(description).map(d -> escapeUnaryQuotesString(d.getText())), fields);
        }
        catch (DataConversionException e)
        {
            throw new GenerationException(e);
        }
    }

    private static String escapeUnaryQuotesString(@NotNull String text)
    {
        return text.replaceAll(Pattern.quote("'"), "\\\\'");
    }

    @NotNull
    private static String escapeIfKeyword(@NotNull String name)
    {
        switch (name)
        {
            case "type":
            case "float":
            case "command":
                return "^" + name;
            default:
                return name;
        }
    }

    @NotNull
    private static String makeTypeNameForEnum(@NotNull String typeName, @NotNull String enumName)
    {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, enumName) + "_" + typeName;
    }

    private static void eol(@NotNull Appendable appendable) throws IOException
    {
        appendable.append("\n");
    }

    private static class Message implements Generatable<Appendable>
    {
        @NotNull
        private final String typeName;
        @NotNull
        private final Optional<String> description;

        @NotNull
        public List<StructField> getFields()
        {
            return fields;
        }

        @NotNull
        private final List<StructField> fields;
        private final int id;
        @NotNull
        private String name;

        public Message(@NotNull String name, @NotNull String typeName, int id, @NotNull Optional<String> description,
                       @NotNull List<StructField> fields)
        {
            this.name = name;
            this.typeName = typeName;
            this.id = id;
            this.description = description;
            this.fields = reorderFields(fields);
        }

        private List<StructField> reorderFields(@NotNull List<StructField> fields)
        {
            return fields.stream().sorted(new Comparator<StructField>()
            {
                private final List<List<String>> priorityLists = Lists.newArrayList(
                        Lists.newArrayList("int64_t", "double"),
                        Lists.newArrayList("int32_t", "float"),
                        Lists.newArrayList("int16_t"));
                @Override
                public int compare(StructField f1, StructField f2)
                {
                    return priority(f1) - priority(f2);
                }

                private int priority(StructField field)
                {
                    int currentPriority = 0;
                    String type = field.getType();
                    for (List<String> priorityList : priorityLists)
                    {
                        if (priorityList.stream().anyMatch(type::contains))
                        {
                            break;
                        }
                        currentPriority++;
                    }
                    return currentPriority;
                }
            }).collect(Collectors.toList());
        }

        public int getId()
        {
            return id;
        }

        @Override
        public void generate(@NotNull Appendable appendable)
        {
            try
            {
                eol(appendable);
                appendable.append("type ").append(typeName).append(" struct");
                if (description.isPresent())
                {
                    appendable.append(" info '").append(description.get()).append("'");
                }
                appendable.append("(");
                eol(appendable);

                for (StructField field : fields)
                {
                    field.generate(appendable);
                }

                appendable.append(")");
                eol(appendable);
            }
            catch (IOException e)
            {
                throw new GenerationException(e);
            }
        }

        @NotNull
        public String getTypeName()
        {
            return typeName;
        }

        public String getName()
        {
            return name;
        }
    }

    private static class StructField implements Generatable<Appendable>
    {
        @NotNull
        public Optional<String> getDescription()
        {
            return description;
        }

        @NotNull
        public String getType()
        {
            return type;

        }

        @NotNull
        private final String type;
        @NotNull
        private final String name;
        @NotNull
        private final Optional<String> description;
        @NotNull
        private final Optional<String> anEnum;

        public StructField(@NotNull String type, @NotNull String name, @NotNull Optional<String> description,
                           @NotNull Optional<String> anEnum)
        {
            this.type = type;
            this.name = name;
            this.description = description;
            this.anEnum = anEnum;
        }

        @Override
        public void generate(@NotNull Appendable appendable)
        {
            try
            {
                String typeName = getTypeName();
                appendable.append("  ").append(typeName)
                        .append(StringUtils.repeat(" ", Math.max(20 - typeName.length(), 1)))
                        .append(name)
                        .append(StringUtils.repeat(" ", Math.max(20 - name.length(), 1)));
                if (description.isPresent())
                {
                    appendable.append("info '").append(description.get()).append("'");
                }
                appendable.append(",");
                eol(appendable);
            }
            catch (IOException e)
            {
                throw new GenerationException(e);
            }
        }

        @NotNull
        public String getTypeName()
        {
            return anEnum.isPresent() ? makeTypeNameForEnum(type, anEnum.get()) : type;
        }

        @NotNull
        public String getName()
        {
            return name;
        }
    }

    private static class MavEnum
    {
        @NotNull
        private final Optional<String> info;
        @NotNull
        private final List<MavEnumConstant> constants;
        @NotNull
        private String name;

        @NotNull
        public Optional<String> getInfo()
        {
            return info;
        }

        @NotNull
        public List<MavEnumConstant> getConstants()
        {
            return constants;
        }

        public MavEnum(@NotNull String name, @NotNull Optional<String> description,
                       @NotNull List<MavEnumConstant> constants)
        {
            this.name = name;
            this.info = description;
            this.constants = constants;
        }

        @NotNull
        public String getName()
        {
            return name;
        }

        public void merge(@NotNull MavEnum e)
        {
            constants.addAll(e.getConstants());
        }
    }

    private static class MavEnumConstant
    {
        @NotNull
        private final List<MavEnumCostantParam> params;
        @NotNull
        private final Optional<String> info;
        @NotNull
        private final Optional<Integer> value;
        @NotNull
        private final String name;

        @NotNull
        public List<MavEnumCostantParam> getParams()
        {
            return params;
        }

        @NotNull
        public Optional<String> getInfo()
        {
            return info;
        }

        @NotNull
        public Optional<Integer> getValue()
        {
            return value;
        }

        @NotNull
        public String getName()
        {
            return name;
        }

        public MavEnumConstant(@NotNull String name, @NotNull Optional<Integer> value,
                               @NotNull Optional<String> description,
                               @NotNull List<MavEnumCostantParam> params)
        {
            this.name = name;
            this.value = value;
            this.info = description;
            this.params = params;
        }
    }

    private class MavEnumCostantParam
    {
        private final int index;
        @NotNull
        private final String info;

        public int getIndex()
        {
            return index;
        }

        @NotNull
        public String getInfo()
        {
            return info;
        }

        public MavEnumCostantParam(int index, @NotNull String info)
        {
            this.index = index;
            this.info = info;
        }
    }

    private class MavComponent
    {
        @NotNull
        private final String name;
        @NotNull
        private final Set<MavComponent> subComponents = new HashSet<>();

        public MavComponent(@NotNull String name)
        {
            this.name = name;
        }

        public void addSubcomponent(@NotNull MavComponent subComponent)
        {
            subComponents.add(subComponent);
        }

        @NotNull
        public String getName()
        {
            return name;
        }

        @NotNull
        public Set<MavComponent> getSubComponents()
        {
            return subComponents;
        }
    }
}
