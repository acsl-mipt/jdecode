package ru.mipt.acsl.decode.model.provider;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.impl.type.*;
import ru.mipt.acsl.decode.model.exporter.TableName;
import ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy;
import ru.mipt.acsl.decode.model.exporter.ModelExportingException;
import scala.Int;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.mutable.*;
import scala.collection.mutable.HashSet;
import scala.collection.mutable.Set;

import java.sql.*;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
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
            DecodeRegistry registry = DecodeRegistryImpl.apply();
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
                namespaceById.put(namespacesSelectRs.getLong("id"), DecodeNamespaceImpl.apply(
                        DecodeNameImpl.newFromMangledName(namespacesSelectRs.getString("name")),
                        Option.<DecodeNamespace>empty()));
            }
        }
        try (ResultSet subNamespacesRs = connection.prepareStatement(String.format(
                "SELECT namespace_id, sub_namespace_id FROM %s", TableName.SUB_NAMESPACE)).executeQuery())
        {
            while (subNamespacesRs.next())
            {
                DecodeNamespace parent = namespaceById.get(subNamespacesRs.getLong("namespace_id")),
                        child = namespaceById.get(subNamespacesRs.getLong("sub_namespace_id"));
                child.parent_$eq(Option.apply(parent));
                parent.subNamespaces().$plus$eq(child);
            }
            Buffer<DecodeNamespace> rootNamespaces = registry.rootNamespaces();
            rootNamespaces.$plus$plus$eq(JavaConversions.asScalaBuffer(namespaceById.values().stream().filter(n -> !n.parent().isDefined())
                    .collect(Collectors.toList())));
        }

        try (ResultSet selectUnitsRs = connection.prepareStatement(
                String.format("SELECT id, namespace_id, name, display, info FROM %s", TableName.UNIT)).executeQuery())
        {
            while (selectUnitsRs.next())
            {
                DecodeNamespace namespace = namespaceById.get(selectUnitsRs.getLong("namespace_id"));
                DecodeUnit unit = new DecodeUnitImpl(
                        DecodeNameImpl.newFromMangledName(selectUnitsRs.getString("name")),
                        namespace, Option.apply(selectUnitsRs.getString("display")),
                        Option.apply(selectUnitsRs.getString("info")));
                unitById.put(selectUnitsRs.getLong("id"), unit);
                namespace.units().$plus$eq(unit);
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
                Option<DecodeMaybeProxy<DecodeStructType>> baseType =
                        componentRs.wasNull() ? Option.<DecodeMaybeProxy<DecodeStructType>>empty()
                                : Option.apply(SimpleDecodeMaybeProxy.object((DecodeStructType) ensureTypeLoaded(baseTypeId)));

                Buffer<DecodeComponentRef> subComponentRefs = new ArrayBuffer<>();
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
                                    .$plus$eq(new DecodeComponentRefImpl(SimpleDecodeMaybeProxy
                                            .object(ensureComponentLoaded(subComponentsRs.getLong("sub_component_id"))),
                                            Option.empty()));
                        }
                    }
                }

                Buffer<DecodeCommand> commands = new ArrayBuffer<>();
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
                            Buffer<DecodeCommandArgument> arguments = new ArrayBuffer<>();
                            long commandId = commandsSelectRs.getLong("id");
                            argumentsSelect.setLong(1, commandId);
                            try (ResultSet commandArgumentsRs = argumentsSelect.executeQuery())
                            {
                                while (commandArgumentsRs.next())
                                {
                                    long unitId = commandArgumentsRs.getLong("unit_id");
                                    Option<DecodeMaybeProxy<DecodeUnit>> unit = commandArgumentsRs.wasNull()
                                            ? Option.<DecodeMaybeProxy<DecodeUnit>>empty()
                                            : Option.apply(SimpleDecodeMaybeProxy.object(
                                            Preconditions.checkNotNull(unitById.get(unitId), "unit not found")));

                                    arguments.$plus$eq(new DecodeCommandArgumentImpl(
                                            DecodeNameImpl.newFromMangledName(
                                                    commandArgumentsRs.getString("name")),
                                            Option.apply(commandArgumentsRs.getString("info")),
                                            SimpleDecodeMaybeProxy.object(ensureTypeLoaded(
                                                    commandArgumentsRs.getLong("type_id"))),
                                            unit));
                                }
                            }
                            Option<Long> returnTypeIdOptional = getOptionalLong(commandsSelectRs, "return_type_id");
                            commands.$plus$eq(ImmutableDecodeCommand.newInstance(
                                    DecodeNameImpl.newFromMangledName(commandsSelectRs.getString("name")),
                                    getOptionalInt(commandsSelectRs, "command_id"),
                                    Option.apply(commandsSelectRs.getString("info")), arguments,
                                    Option.apply(returnTypeIdOptional.isDefined()
                                            ? SimpleDecodeMaybeProxy.object(
                                            Preconditions.checkNotNull(typeById.get(returnTypeIdOptional.get()), "type not found"))
                                            : null)));
                        }

                    }
                }

                Buffer<DecodeMessage> messages = new ArrayBuffer<>();
                int componentForcedId = componentRs.getInt("component_id");
                boolean isComponentForcedIdProvided = !componentRs.wasNull();
                component = new DecodeComponentImpl(
                        DecodeNameImpl.newFromMangledName(componentRs.getString("name")), namespace,
                        isComponentForcedIdProvided ? Option.apply(componentForcedId) : Option.empty(), baseType,
                        Option.apply(componentRs.getString("info")), subComponentRefs, commands, messages);
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
                            Buffer<DecodeMessageParameter> parameters = new ArrayBuffer<>();
                            try (ResultSet messageParametersRs = messageParametersSelect.executeQuery())
                            {
                                while (messageParametersRs.next())
                                {
                                    parameters.$plus$eq(ImmutableDecodeMessageParameter.newInstance(
                                            messageParametersRs.getString("name")));
                                }
                            }

                            DecodeNameImpl messageName = DecodeNameImpl
                                    .newFromMangledName(messagesSelectRs.getString("name"));
                            Option<Object> messageId = getOptionalInt(messagesSelectRs, "message_id");
                            Option<String> messageInfo = Option.apply(messagesSelectRs.getString("info"));

                            DecodeMessage message = null;

                            messagesSelectRs.getLong("s_message_id");
                            if (!messagesSelectRs.wasNull())
                            {
                                message = new DecodeStatusMessageImpl(component, messageName, messageId, messageInfo,
                                                parameters);
                            }

                            messagesSelectRs.getLong("e_message_id");
                            if (!messagesSelectRs.wasNull())
                            {
                                Preconditions.checkState(message == null, "invalid message");
                                message = new DecodeEventMessageImpl(component, messageName, messageId, messageInfo,
                                                parameters, SimpleDecodeMaybeProxy.object(Preconditions.checkNotNull(typeById.get(messagesSelectRs.getLong("e_event_type_id")))));
                            }

                            messages.$plus$eq(Preconditions.checkNotNull(message, "invalid message"));
                        }
                    }
                }

                componentById.put(componentId, component);
                namespace.components().$plus$eq(component);
                return component;
            }
        }
    }

    @NotNull
    private Option<Long> getOptionalLong(@NotNull ResultSet rs, @NotNull String fieldName) throws SQLException
    {
        long value = rs.getLong(fieldName);
        return rs.wasNull() ? Option.<Long>empty() : Option.apply(value);
    }

    @NotNull
    private Option<Object> getOptionalInt(@NotNull ResultSet rs, @NotNull String fieldName) throws SQLException
    {
        int value = rs.getInt(fieldName);
        return rs.wasNull() ?  Option.empty() : Option.apply(Int.box(value));
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
                Option<String> nameStr = Option.apply(typeRs.getString("name"));
                Option<DecodeName> name = Option.apply(nameStr.isDefined()? DecodeNameImpl.newFromMangledName(nameStr.get()) : null);
                Option<String> info = Option.apply(typeRs.getString("info"));
                String primitiveKind = typeRs.getString("kind");

                if (primitiveKind != null)
                {
                    type = new DecodePrimitiveTypeImpl(name, namespace, info, TypeKind.typeKindByName(primitiveKind).get(),
                                    typeRs.getLong("bit_length"));
                }

                long aliasBaseTypeId = typeRs.getLong("a_base_type_id");
                if (!typeRs.wasNull())
                {
                    Preconditions.checkState(type == null, "invalid type");
                    type = new DecodeAliasTypeImpl(name.get(), namespace,
                            SimpleDecodeMaybeProxy.object(ensureTypeLoaded(aliasBaseTypeId)), info);
                }

                long subTypeBaseTypeId = typeRs.getLong("s_base_type_id");
                if (!typeRs.wasNull())
                {
                    Preconditions.checkState(type == null, "invalid type");
                    type = new DecodeSubTypeImpl(name, namespace, info,
                            SimpleDecodeMaybeProxy.object(ensureTypeLoaded(subTypeBaseTypeId)));
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
                                .newInstance(DecodeNameImpl.newFromMangledName(
                                                Preconditions.checkNotNull(constantsRs.getString(0), "constant name")),
                                        constantsRs.getString(2), Option.apply(constantsRs.getString(1))));
                    }
                    type = new DecodeEnumTypeImpl(name, namespace,
                            SimpleDecodeMaybeProxy.object(ensureTypeLoaded(enumBaseTypeId)), info, constants);
                }

                long arrayBaseTypeId = typeRs.getLong("ar_base_type_id");
                if (!typeRs.wasNull())
                {
                    Preconditions.checkState(type == null, "invalid type");
                    type = new DecodeArrayTypeImpl(name, namespace, info,
                            SimpleDecodeMaybeProxy.object(ensureTypeLoaded(arrayBaseTypeId)),
                            new ArraySizeImpl(typeRs.getLong("min_length"),
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
                    Buffer<DecodeStructField> fields = new ArrayBuffer<>();
                    while (structFieldsRs.next())
                    {
                        long unitId = structFieldsRs.getLong(3);
                        Option<DecodeUnit> unit = structFieldsRs.wasNull() ? Option.<DecodeUnit>empty() : Option.apply(
                                unitById.get(unitId));
                        fields.$plus$eq(ImmutableDecodeStructField.newInstance(
                                DecodeNameImpl.newFromMangledName(
                                        structFieldsRs.getString(1)),
                                SimpleDecodeMaybeProxy.object(ensureTypeLoaded(structFieldsRs.getLong(
                                        2))), Option.apply(unit.isDefined() ? SimpleDecodeMaybeProxy.object(unit.get()) : null), info));
                    }
                    Preconditions.checkState(!fields.isEmpty(), "struct must not be empty");
                    type = new DecodeStructTypeImpl(name, namespace, info, fields);
                }

                namespace.types().$plus$eq(Preconditions.checkNotNull(type, "invalid type"));
            }
        }

        typeById.put(typeId, type);
        return type;
    }
}
