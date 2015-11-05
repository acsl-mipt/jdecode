package ru.mipt.acsl.decode.model.provider;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.impl.type.*;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessage;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeEnumConstant;
import ru.mipt.acsl.decode.model.domain.type.DecodeStructField;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import ru.mipt.acsl.decode.model.exporter.TableName;
import ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy;
import ru.mipt.acsl.decode.model.exporter.ModelExportingException;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class DecodeSqlProvider
{
    private Connection connection;
    @NotNull
    private final Map<Long, DecodeType> typeById = new HashMap<>();
    @NotNull
    private final Map<Long, DecodeNamespace> namespaceById = new HashMap<>();
    @NotNull
    private final Map<Long, DecodeUnit> unitById = new HashMap<>();
    @NotNull
    private final Map<Long, DecodeComponent> componentById = new HashMap<>();

    public DecodeRegistry provide(@NotNull DecodeSqlProviderConfiguration config)
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e)
        {
            throw new ModelExportingException(e);
        }
        try(Connection connection = DriverManager
                .getConnection(config.getConnectionUrl()))
        {
            this.connection = connection;
            DecodeRegistry registry = SimpleDecodeRegistry.newInstance();
            readNamespaces(registry);
            return registry;
        }
        catch (SQLException e)
        {
            throw new ModelImportingException(e);
        }
    }

    private void readNamespaces(@NotNull DecodeRegistry registry) throws SQLException
    {
        try (ResultSet namespacesSelectRs = connection.prepareStatement(String.format("SELECT id, name FROM %s",
                TableName.NAMESPACE)).executeQuery())
        {
            while (namespacesSelectRs.next())
            {
                namespaceById.put(namespacesSelectRs.getLong("id"), SimpleDecodeNamespace.newInstance(
                        ImmutableDecodeName.newInstanceFromMangledName(namespacesSelectRs.getString("name")),
                        Optional.<DecodeNamespace>empty()));
            }
        }
        try (ResultSet subNamespacesRs = connection.prepareStatement(String.format(
                "SELECT namespace_id, sub_namespace_id FROM %s", TableName.SUB_NAMESPACE)).executeQuery())
        {
            while (subNamespacesRs.next())
            {
                DecodeNamespace parent = namespaceById.get(subNamespacesRs.getLong("namespace_id")),
                        child = namespaceById.get(subNamespacesRs.getLong("sub_namespace_id"));
                child.setParent(parent);
                parent.getSubNamespaces().add(child);
            }
            List<DecodeNamespace> rootNamespaces = registry.getRootNamespaces();
            rootNamespaces.addAll(namespaceById.values().stream().filter(n -> !n.getParent().isPresent())
                    .collect(Collectors.toList()));
        }

        try (ResultSet selectUnitsRs = connection.prepareStatement(
                String.format("SELECT id, namespace_id, name, display, info FROM %s", TableName.UNIT)).executeQuery())
        {
            while (selectUnitsRs.next())
            {
                DecodeNamespace namespace = namespaceById.get(selectUnitsRs.getLong("namespace_id"));
                DecodeUnit unit = SimpleDecodeUnit.newInstance(
                        ImmutableDecodeName.newInstanceFromMangledName(selectUnitsRs.getString("name")),
                        namespace, selectUnitsRs.getString("display"), selectUnitsRs.getString("info"));
                unitById.put(selectUnitsRs.getLong("id"), unit);
                namespace.getUnits().add(unit);
            }
        }

        try (ResultSet selectTypesRs = connection.prepareStatement(String.format("SELECT id FROM %s", TableName.TYPE))
                .executeQuery())
        {
            while (selectTypesRs.next())
            {
                ensureTypeLoaded(selectTypesRs.getLong("id"));
            }
        }

        try (ResultSet selectComponentsRs = connection.prepareStatement(String.format("SELECT id FROM %s",
                TableName.COMPONENT)).executeQuery())
        {
            while (selectComponentsRs.next())
            {
                ensureComponentLoaded(selectComponentsRs.getLong("id"));
            }
        }
    }

    private DecodeComponent ensureComponentLoaded(long componentId) throws SQLException
    {
        DecodeComponent component = componentById.get(componentId);
        if (component != null)
        {
            return component;
        }

        try(PreparedStatement componentSelect = connection.prepareStatement(
                String.format("SELECT namespace_id, component_id, name, base_type_id, info FROM %s WHERE id = ?",
                        TableName.COMPONENT)))
        {
            componentSelect.setLong(1, componentId);
            try (ResultSet componentRs = componentSelect.executeQuery())
            {
                Preconditions.checkState(componentRs.next(), "component not found");

                DecodeNamespace namespace = Preconditions.checkNotNull(namespaceById.get(componentRs.getLong("namespace_id")),
                        "namespace not found");
                long baseTypeId = componentRs.getLong("base_type_id");
                Optional<DecodeMaybeProxy<DecodeType>> baseType =
                        componentRs.wasNull() ? Optional.<DecodeMaybeProxy<DecodeType>>empty()
                                : Optional.of(SimpleDecodeMaybeProxy.object(ensureTypeLoaded(baseTypeId)));

                List<DecodeComponentRef> subComponentRefs = new ArrayList<>();
                try(PreparedStatement subComponentsSelect = connection.prepareStatement(
                        String.format(
                                "SELECT sub_component_id FROM %s WHERE component_id = ? ORDER BY sub_component_id ASC",
                                TableName.SUB_COMPONENT)))
                {
                    subComponentsSelect.setLong(1, componentId);
                    try(ResultSet subComponentsRs = subComponentsSelect.executeQuery())
                    {
                        while (subComponentsRs.next())
                        {
                            subComponentRefs
                                    .add(ImmutableDecodeComponentRef.newInstance(SimpleDecodeMaybeProxy
                                            .object(ensureComponentLoaded(subComponentsRs.getLong("sub_component_id")))));
                        }
                    }
                }

                List<DecodeCommand> commands = new ArrayList<>();
                try (PreparedStatement commandsSelect = connection.prepareStatement(
                        String.format("SELECT id, name, command_id, info FROM %s WHERE component_id = ?",
                                TableName.COMMAND)))
                {
                    commandsSelect.setLong(1, componentId);
                    try (ResultSet commandsSelectRs = commandsSelect.executeQuery();
                        PreparedStatement argumentsSelect = connection.prepareStatement(String.format(
                                "SELECT name, type_id, unit_id, info FROM %s WHERE command_id = ? ORDER BY argument_index ASC",
                                TableName.COMMAND_ARGUMENT)))
                    {
                        while (commandsSelectRs.next())
                        {
                            List<DecodeCommandArgument> arguments = new ArrayList<>();
                            long commandId = commandsSelectRs.getLong("id");
                            argumentsSelect.setLong(1, commandId);
                            try (ResultSet commandArgumentsRs = argumentsSelect.executeQuery())
                            {
                                while (commandArgumentsRs.next())
                                {
                                    long unitId = commandArgumentsRs.getLong("unit_id");
                                    Optional<DecodeMaybeProxy<DecodeUnit>> unit = commandArgumentsRs.wasNull()
                                            ? Optional.<DecodeMaybeProxy<DecodeUnit>>empty()
                                            : Optional.of(SimpleDecodeMaybeProxy.object(
                                            Preconditions.checkNotNull(unitById.get(unitId), "unit not found")));

                                    arguments.add(ImmutableDecodeCommandArgument.newInstance(
                                            ImmutableDecodeName.newInstanceFromMangledName(
                                                    commandArgumentsRs.getString("name")),
                                            SimpleDecodeMaybeProxy.object(ensureTypeLoaded(
                                                    commandArgumentsRs.getLong("type_id"))),
                                            unit, Optional.ofNullable(commandArgumentsRs.getString("info"))));
                                }
                            }
                            Optional<Long> returnTypeIdOptional = getOptionalLong(commandsSelectRs, "return_type_id");
                            commands.add(ImmutableDecodeCommand.newInstance(
                                    ImmutableDecodeName.newInstanceFromMangledName(commandsSelectRs.getString("name")),
                                    getOptionalInt(commandsSelectRs, "command_id"),
                                    Optional.ofNullable(commandsSelectRs.getString("info")), arguments,
                                    returnTypeIdOptional.map(rti -> SimpleDecodeMaybeProxy.object(
                                            Preconditions.checkNotNull(typeById.get(rti), "type not found")))));
                        }

                    }
                }

                List<DecodeMessage> messages = new ArrayList<>();
                int componentForcedId = componentRs.getInt("component_id");
                boolean isComponentForcedIdProvided = !componentRs.wasNull();
                component = SimpleDecodeComponent.newInstance(
                        ImmutableDecodeName.newInstanceFromMangledName(componentRs.getString("name")), namespace,
                        isComponentForcedIdProvided ? Optional.of(componentForcedId) : Optional.empty(), baseType,
                        Optional.ofNullable(componentRs.getString("info")), subComponentRefs, commands, messages);
                try (PreparedStatement messagesSelect = connection.prepareStatement(String.format(
                        "SELECT m.id AS id, m.name AS name, m.message_id AS message_id, m.info AS info,"
                                + " s.message_id AS s_message_id, e.message_id AS e_message_id, e.event_type_id AS e_event_type_id,"
                                + " FROM %s AS m LEFT JOIN %s AS s ON s.message_id = m.id"
                                + " LEFT JOIN %s AS e ON e.message_id = m.id"
                                + " WHERE component_id = ?", TableName.MESSAGE, TableName.STATUS_MESSAGE,
                        TableName.EVENT_MESSAGE)))
                {
                    messagesSelect.setLong(1, componentId);
                    try (ResultSet messagesSelectRs = messagesSelect.executeQuery();
                         PreparedStatement messageParametersSelect = connection.prepareStatement(String.format(
                                 "SELECT name FROM %s WHERE message_id = ? ORDER BY parameter_index ASC",
                                 TableName.MESSAGE_PARAMETER)))
                    {
                        while (messagesSelectRs.next())
                        {
                            messageParametersSelect.setLong(1, messagesSelectRs.getLong("id"));
                            List<DecodeMessageParameter> parameters = new ArrayList<>();
                            try (ResultSet messageParametersRs = messageParametersSelect.executeQuery())
                            {
                                while (messageParametersRs.next())
                                {
                                    parameters.add(ImmutableDecodeMessageParameter.newInstance(
                                            messageParametersRs.getString("name")));
                                }
                            }

                            DecodeName messageName = ImmutableDecodeName
                                    .newInstanceFromMangledName(messagesSelectRs.getString("name"));
                            Optional<Integer> messageId = getOptionalInt(messagesSelectRs, "message_id");
                            Optional<String> messageInfo = Optional.ofNullable(messagesSelectRs.getString("info"));

                            DecodeMessage message = null;

                            messagesSelectRs.getLong("s_message_id");
                            if (!messagesSelectRs.wasNull())
                            {
                                message = ImmutableDecodeStatusMessage
                                        .newInstance(component, messageName, messageId, messageInfo,
                                                parameters);
                            }

                            messagesSelectRs.getLong("e_message_id");
                            if (!messagesSelectRs.wasNull())
                            {
                                Preconditions.checkState(message == null, "invalid message");
                                message = ImmutableDecodeEventMessage
                                        .newInstance(component, messageName, messageId, messageInfo,
                                                parameters, SimpleDecodeMaybeProxy.object(Preconditions.checkNotNull(typeById.get(messagesSelectRs.getLong("e_event_type_id")))));
                            }

                            messages.add(Preconditions.checkNotNull(message, "invalid message"));
                        }
                    }
                }

                componentById.put(componentId, component);
                namespace.getComponents().add(component);
                return component;
            }
        }
    }

    @NotNull
    private Optional<Long> getOptionalLong(@NotNull ResultSet rs, @NotNull String fieldName) throws SQLException
    {
        long value = rs.getLong(fieldName);
        return rs.wasNull() ? Optional.<Long>empty() : Optional.of(value);
    }

    @NotNull
    private Optional<Integer> getOptionalInt(@NotNull ResultSet rs, @NotNull String fieldName) throws SQLException
    {
        int value = rs.getInt(fieldName);
        return rs.wasNull() ?  Optional.<Integer>empty() : Optional.of(value);
    }

    private DecodeType ensureTypeLoaded(long typeId) throws SQLException
    {
        DecodeType type = typeById.get(typeId);
        if (type != null)
        {
            return type;
        }

        try (PreparedStatement typeSelect = connection.prepareStatement(String.format(
                "SELECT t.namespace_id AS namespace_id, t.name AS name, t.info AS info, p.kind AS kind,"
                        + " p.bit_length AS bit_length, a.base_type_id AS a_base_type_id,"
                        + " s.base_type_id AS s_base_type_id, e.base_type_id AS e_base_type_id,"
                        + " ar.base_type_id AS ar_base_type_id, ar.min_length AS min_length, ar.max_length AS max_length,"
                        + " str.id AS str_type_id FROM %s AS t LEFT JOIN %s AS p ON p.type_id = t.id"
                        + " LEFT JOIN %s AS a ON a.type_id = t.id LEFT JOIN %s AS s ON s.type_id = t.id"
                        + " LEFT JOIN %s AS e ON e.type_id = t.id LEFT JOIN %s AS ar ON ar.type_id = t.id"
                        + " LEFT JOIN %s AS str ON str.type_id = t.id WHERE t.id = ?",
                TableName.TYPE, TableName.PRIMITIVE_TYPE, TableName.ALIAS_TYPE, TableName.SUB_TYPE,
                TableName.ENUM_TYPE, TableName.ARRAY_TYPE, TableName.STRUCT_TYPE)))
        {
            typeSelect.setLong(1, typeId);
            try (ResultSet typeRs = typeSelect.executeQuery())
            {
                Preconditions.checkState(typeRs.next(), "type not found");
                DecodeNamespace namespace = Preconditions.checkNotNull(namespaceById.get(typeRs.getLong("namespace_id")),
                        "namespace not found for type");
                Optional<DecodeName> name = Optional.ofNullable(typeRs.getString("name")).map(
                        ImmutableDecodeName::newInstanceFromMangledName);
                Optional<String> info = Optional.ofNullable(typeRs.getString("info"));
                String primitiveKind = typeRs.getString("kind");

                if (primitiveKind != null)
                {
                    type = SimpleDecodePrimitiveType
                            .newInstance(name, namespace, DecodeType.TypeKind.forName(primitiveKind).orElseThrow(
                                            AssertionError::new),
                                    typeRs.getLong("bit_length"), info);
                }

                long aliasBaseTypeId = typeRs.getLong("a_base_type_id");
                if (!typeRs.wasNull())
                {
                    Preconditions.checkState(type == null, "invalid type");
                    type = SimpleDecodeAliasType.newInstance(name.get(), namespace,
                            SimpleDecodeMaybeProxy.object(ensureTypeLoaded(aliasBaseTypeId)), info);
                }

                long subTypeBaseTypeId = typeRs.getLong("s_base_type_id");
                if (!typeRs.wasNull())
                {
                    Preconditions.checkState(type == null, "invalid type");
                    type = SimpleDecodeSubType.newInstance(name, namespace,
                            SimpleDecodeMaybeProxy.object(ensureTypeLoaded(subTypeBaseTypeId)), info);
                }

                long enumBaseTypeId = typeRs.getLong("e_base_type_id");
                if (!typeRs.wasNull())
                {
                    Preconditions.checkState(type == null, "invalid type");
                    PreparedStatement enumConstantsStmt = connection.prepareStatement(
                            String.format("SELECT name, info, value FROM %s WHERE enum_type_id = ?",
                                    TableName.ENUM_TYPE_CONSTANT));
                    enumConstantsStmt.setLong(1, enumBaseTypeId);
                    ResultSet constantsRs = enumConstantsStmt.executeQuery();
                    Set<DecodeEnumConstant> constants = new HashSet<>();
                    while (constantsRs.next())
                    {
                        constants.add(ImmutableDecodeEnumConstant
                                .newInstance(ImmutableDecodeName.newInstanceFromMangledName(
                                                Preconditions.checkNotNull(constantsRs.getString(0), "constant name")),
                                        constantsRs.getString(2), Optional.ofNullable(constantsRs.getString(1))));
                    }
                    type = SimpleDecodeEnumType.newInstance(name, namespace,
                            SimpleDecodeMaybeProxy.object(ensureTypeLoaded(enumBaseTypeId)), info, constants);
                }

                long arrayBaseTypeId = typeRs.getLong("ar_base_type_id");
                if (!typeRs.wasNull())
                {
                    Preconditions.checkState(type == null, "invalid type");
                    type = SimpleDecodeArrayType.newInstance(name, namespace,
                            SimpleDecodeMaybeProxy.object(ensureTypeLoaded(arrayBaseTypeId)), info,
                            ImmutableArraySize.newInstance(typeRs.getLong("min_length"),
                                    typeRs.getLong("max_length")));
                }

                long structTypeId = typeRs.getLong("str_type_id");
                if (!typeRs.wasNull())
                {
                    Preconditions.checkState(type == null, "invalid type");
                    PreparedStatement structFieldsStmt = connection.prepareStatement(String.format(
                            "SELECT name, type_id, unit_id, info FROM %s WHERE struct_type_id = ? ORDER BY field_index ASC",
                            TableName.STRUCT_TYPE_FIELD));
                    structFieldsStmt.setLong(1, structTypeId);
                    ResultSet structFieldsRs = structFieldsStmt.executeQuery();
                    List<DecodeStructField> fields = new ArrayList<>();
                    while (structFieldsRs.next())
                    {
                        long unitId = structFieldsRs.getLong(3);
                        Optional<DecodeUnit> unit = structFieldsRs.wasNull() ? Optional.<DecodeUnit>empty() : Optional.of(
                                unitById.get(unitId));
                        fields.add(ImmutableDecodeStructField.newInstance(
                                ImmutableDecodeName.newInstanceFromMangledName(
                                        structFieldsRs.getString(1)),
                                SimpleDecodeMaybeProxy.object(ensureTypeLoaded(structFieldsRs.getLong(
                                        2))), unit.map(SimpleDecodeMaybeProxy::object), info));
                    }
                    Preconditions.checkState(!fields.isEmpty(), "struct must not be empty");
                    type = SimpleDecodeStructType.newInstance(name, namespace, info, fields);
                }

                namespace.getTypes().add(Preconditions.checkNotNull(type, "invalid type"));
            }
        }

        typeById.put(typeId, type);
        return type;
    }
}
