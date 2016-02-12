package ru.mipt.acsl.decode.model.exporter;

import com.google.common.base.Preconditions;
import ru.mipt.acsl.decode.model.domain.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Int;
import scala.Option;
import scala.collection.*;
import scala.collection.Iterator;

import java.sql.*;
import java.util.*;
import java.util.Map;

/**
 * @author Artem Shein
 */
public class DecodeSqlite3Exporter
{
    @NotNull
    private final Map<DecodeType, Long> typeKeyByType = new HashMap<>();
    @NotNull
    private final Map<DecodeNamespace, Long> namespaceKeyByNamespaceMap = new HashMap<>();
    @NotNull
    private final Map<DecodeUnit, Long> unitKeyByUnit = new HashMap<>();
    @NotNull
    private final Map<DecodeComponent, Long> componentKeyByComponentMap = new HashMap<>();
    @NotNull
    private final DecodeSqlite3ExporterConfiguration config;
    @NotNull
    private Connection connection;
    @NotNull
    private InsertTypeVisitor insertTypeVisitor = new InsertTypeVisitor();

    public DecodeSqlite3Exporter(@NotNull DecodeSqlite3ExporterConfiguration config)
    {
        this.config = config;
    }

    public void export(@NotNull DecodeRegistry registry)
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
                .getConnection("jdbc:sqlite:" + config.getOutputFile().getAbsolutePath()))
        {
            this.connection = connection;
            connection.setAutoCommit(false);
            Collection<DecodeNamespace> namespacesStream = JavaConversions.asJavaCollection(registry.rootNamespaces());
            namespacesStream.forEach(this::insertNamespaceAndSubNamespaces);
            namespacesStream.forEach(this::insertUnits);
            namespacesStream.forEach(this::insertTypes);
            namespacesStream.forEach(this::insertComponents);
            connection.commit();
        }
        catch (SQLException e)
        {
            throw new ModelExportingException(e);
        }
    }

    private Void insertNamespaceAndSubNamespaces(@NotNull DecodeNamespace namespace)
    {
        try
        {
            insertNamespace(namespace);
            try (PreparedStatement linkNamespaces = connection.prepareStatement(
                    String.format("INSERT INTO %s (namespace_id, sub_namespace_id) VALUES (?, ?)",
                            TableName.SUB_NAMESPACE)))
            {
                scala.collection.Iterator<DecodeNamespace> subNsIt = namespace.subNamespaces().iterator();
                while (subNsIt.hasNext())
                {
                    DecodeNamespace subNamespace = subNsIt.next();
                    insertNamespaceAndSubNamespaces(subNamespace);
                    linkNamespaces.setLong(1, namespaceKeyByNamespaceMap.get(namespace));
                    linkNamespaces.setLong(2, namespaceKeyByNamespaceMap.get(subNamespace));
                    linkNamespaces.execute();
                }
            }
        }
        catch (SQLException e)
        {
            throw new ModelExportingException(e);
        }
        return null;
    }

    private Void insertComponents(@NotNull DecodeNamespace namespace)
    {
        JavaConversions.asJavaCollection(namespace.components()).forEach(this::insertComponent);
        JavaConversions.asJavaCollection(namespace.subNamespaces()).forEach(this::insertComponents);
        return null;
    }

    private void insertComponent(@NotNull DecodeComponent component)
    {
        if (componentKeyByComponentMap.containsKey(component))
        {
            return;
        }
        try(PreparedStatement insertComponent = connection.prepareStatement(
                String.format("INSERT INTO %s (namespace_id, name, base_type_id, info) VALUES (?, ?, ?, ?)", TableName.COMPONENT)))
        {
            insertComponent.setLong(1, namespaceKeyByNamespaceMap.get(component.namespace()));
            insertComponent.setString(2, component.name().asMangledString());
            if (component.baseType().isDefined())
            {
                insertType(component.baseType().get().obj());
                insertComponent.setLong(3, typeKeyByType.get(component.baseType().get().obj()));
            }
            else
            {
                insertComponent.setNull(3, Types.BIGINT);
            }
            setStringOrNull(insertComponent, 4, component.info());
            insertComponent.execute();

            long componentKey = getGeneratedKey(insertComponent);
            componentKeyByComponentMap.put(component, componentKey);
            int subComponentIndex = 0;
            Iterator<DecodeComponentRef> subCompIt = component.subComponents().iterator();
            while (subCompIt.hasNext())
            {
                DecodeComponentRef subComp = subCompIt.next();
                DecodeMaybeProxy<DecodeComponent> subComponent = subComp.component();
                insertComponent(subComponent.obj());
                try (PreparedStatement linkComponents = connection.prepareStatement(
                        String.format("INSERT INTO %s (component_id, sub_component_index, sub_component_alias, sub_component_id) VALUES (?, ?, ?, ?)",
                                TableName.SUB_COMPONENT)))
                {
                    linkComponents.setLong(1, componentKey);
                    linkComponents.setLong(2, subComponentIndex++);
                    setStringOrNull(linkComponents, 3, subComp.alias());
                    linkComponents.setLong(4, componentKeyByComponentMap.get(subComponent.obj()));
                    linkComponents.execute();
                }
            }
            Iterator<DecodeCommand> cmdsIt = component.commands().iterator();
            while (cmdsIt.hasNext())
            {
                DecodeCommand command = cmdsIt.next();
                long commandKey;
                try (PreparedStatement insertCommand = connection.prepareStatement(
                        String.format("INSERT INTO %s (component_id, name, command_id, info) VALUES (?, ?, ?, ?)",
                                TableName.COMMAND)))
                {
                    insertCommand.setLong(1, componentKey);
                    insertCommand.setString(2, command.name().asMangledString());
                    setLongOrNull(insertCommand, 3, Option.apply(command.id().isDefined() ? (long) command.id().get() : null));
                    setStringOrNull(insertCommand, 4, command.info());
                    insertCommand.execute();
                    commandKey = getGeneratedKey(insertCommand);
                }
                long index = 0;
                Iterator<DecodeCommandParameter> argsIt = command.parameters().iterator();
                while (argsIt.hasNext())
                {
                    DecodeCommandParameter argument = argsIt.next();
                    try (PreparedStatement insertArgument = connection.prepareStatement(
                            String.format(
                                    "INSERT INTO %s (command_id, argument_index, name, type_id, unit_id, info) VALUES (?, ?, ?, ?, ?, ?)",
                                    TableName.COMMAND_ARGUMENT)))
                    {
                        insertArgument.setLong(1, commandKey);
                        insertArgument.setLong(2, index++);
                        insertArgument.setString(3, argument.name().asMangledString());
                        insertArgument.setLong(4, typeKeyByType.get(argument.paramType().obj()));
                        setUnit(insertArgument, 5, argument.unit());
                        setStringOrNull(insertArgument, 6, argument.info());
                        insertArgument.execute();
                    }
                }
            }
            Iterator<DecodeMessage> msgsIt = component.messages().iterator();
            while (msgsIt.hasNext())
            {
                DecodeMessage message = msgsIt.next();
                long messageKey;
                try (PreparedStatement insertMessage = connection.prepareStatement(
                        String.format("INSERT INTO %s (message_id, component_id, name, info) VALUES (?, ?, ?, ?)",
                                TableName.MESSAGE)))
                {
                    setIntOrNull(insertMessage, 1, Option.apply(message.id().isDefined()? (Int) message.id().get() : null));
                    insertMessage.setLong(2, componentKey);
                    insertMessage.setString(3, message.name().asMangledString());
                    setStringOrNull(insertMessage, 4, message.info());
                    insertMessage.execute();
                    messageKey = getGeneratedKey(insertMessage);
                }
                message.accept(new DecodeInsertMessageVisitor(messageKey, connection));
                long index = 0;
                Iterator<DecodeMessageParameter> parIt = message.parameters().iterator();
                while (parIt.hasNext())
                {
                    DecodeMessageParameter parameter = parIt.next();
                    try (PreparedStatement insertParameter = connection.prepareStatement(
                            String.format("INSERT INTO %s (message_id, parameter_index, name) VALUES (?, ?, ?)",
                                    TableName.MESSAGE_PARAMETER)))
                    {
                        insertParameter.setLong(1, messageKey);
                        insertParameter.setLong(2, index++);
                        insertParameter.setString(3, parameter.value());
                        insertParameter.execute();
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new ModelExportingException(e);
        }
    }

    private void setLongOrNull(@NotNull PreparedStatement statement, int index, @NotNull Option<Long> longOptional) throws
            SQLException
    {
        if (longOptional.isDefined())
        {
            statement.setLong(index, longOptional.get());
        }
        else
        {
            statement.setNull(index, Types.BIGINT);
        }
    }

    private void setIntOrNull(@NotNull PreparedStatement statement, int index, @NotNull Option<Int> integerOptional) throws
            SQLException
    {
        if (integerOptional.isDefined())
        {
            statement.setInt(index, Int.unbox(integerOptional.get()));
        }
        else
        {
            statement.setNull(index, Types.BIGINT);
        }
    }

    private void setUnit(@NotNull PreparedStatement statement, int index,
                                @NotNull Option<DecodeMaybeProxy<DecodeUnit>> unit) throws SQLException
    {
        if (unit.isDefined())
        {
            statement.setLong(index, unitKeyByUnit.get(unit.get().obj()));
        }
        else
        {
            statement.setNull(index, Types.BIGINT);
        }
    }

    private Void insertTypes(@NotNull DecodeNamespace namespace)
    {
        JavaConversions.asJavaCollection(namespace.types()).forEach(this::insertType);
        JavaConversions.asJavaCollection(namespace.subNamespaces()).forEach(this::insertTypes);
        return null;
    }

    private Void insertType(@NotNull DecodeType type)
    {
        if (typeKeyByType.containsKey(type))
        {
            return null;
        }
        System.out.println("--");
        try(PreparedStatement insertType = connection.prepareStatement(
                String.format("INSERT INTO %s (namespace_id, name, info) VALUES (?, ?, ?)", TableName.TYPE)))
        {
            insertType.setLong(1, namespaceKeyByNamespaceMap.get(type.namespace()));
            setStringOrNull(insertType, 2, Option.apply(type.optionName().isDefined()? type.optionName().get().asMangledString() : null));
            setStringOrNull(insertType, 3, type.info());
            insertType.execute();
            typeKeyByType.put(type, getGeneratedKey(insertType));
            type.accept(insertTypeVisitor);
        }
        catch (SQLException e)
        {
            throw new ModelExportingException(e, "Can't insert type: %s", type);
        }
        return null;
    }

    private void setStringOrNull(@NotNull PreparedStatement statement, int index, @NotNull Option<String> stringOptional) throws
            SQLException
    {
        if (stringOptional.isDefined())
        {
            statement.setString(index, stringOptional.get());
        }
        else
        {
            statement.setNull(index, Types.VARCHAR);
        }
    }

    private Void insertUnits(@NotNull DecodeNamespace namespace)
    {
        JavaConversions.asJavaCollection(namespace.units()).forEach(this::insertUnit);
        JavaConversions.asJavaCollection(namespace.subNamespaces()).forEach(this::insertUnits);
        return null;
    }

    private Void insertUnit(@NotNull DecodeUnit unit)
    {
        try(PreparedStatement insertUnit = connection.prepareStatement(
                String.format("INSERT INTO %s (namespace_id, name, display) VALUES (?, ?, ?)", TableName.UNIT)))
        {
            insertUnit.setLong(1, namespaceKeyByNamespaceMap.get(unit.namespace()));
            insertUnit.setString(2, unit.name().asMangledString());
            if (unit.display().isDefined())
            {
                insertUnit.setString(3, unit.display().get());
            }
            else
            {
                insertUnit.setNull(3, Types.VARCHAR);
            }
            insertUnit.execute();
            unitKeyByUnit.put(unit, getGeneratedKey(insertUnit));
        }
        catch (SQLException e)
        {
            throw new ModelExportingException(e);
        }
        return null;
    }

    private void insertNamespace(@NotNull DecodeNamespace namespace)
    {
        try(PreparedStatement insertNamespace = connection.prepareStatement(
                String.format("INSERT INTO %s (name) VALUES (?)", TableName.NAMESPACE)))
        {
            insertNamespace.setString(1, namespace.name().asMangledString());
            insertNamespace.execute();
            long generatedKey = getGeneratedKey(insertNamespace);
            namespaceKeyByNamespaceMap.put(namespace, generatedKey);
        }
        catch (SQLException e)
        {
            throw new ModelExportingException(e);
        }
    }

    private static long getGeneratedKey(PreparedStatement statement) throws SQLException
    {
        return statement.getGeneratedKeys().getLong(1);
    }

    private class InsertTypeVisitor implements DecodeTypeVisitor<Void>
    {
        public InsertTypeVisitor()
        {
        }

        @Nullable
        @Override
        public Void visit(@NotNull DecodePrimitiveType primitiveType)
        {
            try(PreparedStatement insertPrimitiveType = connection.prepareStatement(
                    String.format("INSERT INTO %s (type_id, kind, bit_length) VALUES (?, ?, ?)", TableName.PRIMITIVE_TYPE)))
            {
                insertPrimitiveType.setLong(1, typeKeyByType.get(primitiveType));
                insertPrimitiveType.setString(2, TypeKind.nameForTypeKind(primitiveType.kind()));
                insertPrimitiveType.setLong(3, primitiveType.bitLength());
                insertPrimitiveType.execute();
            }
            catch (SQLException e)
            {
                throw new ModelExportingException(e);
            }
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeNativeType nativeType)
        {
            try(PreparedStatement insertPrimitiveType = connection.prepareStatement(
                    String.format("INSERT INTO %s (type_id) VALUES (?)", TableName.NATIVE_TYPE)))
            {
                insertPrimitiveType.setLong(1, typeKeyByType.get(nativeType));
                insertPrimitiveType.execute();
            }
            catch (SQLException e)
            {
                throw new ModelExportingException(e);
            }
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeSubType subType)
        {
            insertType(subType.baseType().obj());
            try(PreparedStatement insertSubType = connection.prepareStatement(
                    String.format("INSERT INTO %s (type_id, base_type_id) VALUES " +
                                    "(?, ?)",
                            TableName.SUB_TYPE)))
            {
                insertSubType.setLong(1, typeKeyByType.get(subType));
                insertSubType.setLong(2, typeKeyByType.get(subType.baseType().obj()));
                insertSubType.execute();
            }
            catch (SQLException e)
            {
                throw new ModelExportingException(e);
            }
            return null;
        }

        @Nullable
        @Override
        public Void visit(@NotNull DecodeEnumType enumType)
        {
            insertType(enumType.baseType().obj());
            try(PreparedStatement insertEnumType = connection.prepareStatement(
                    String.format("INSERT INTO %s (type_id, base_type_id) VALUES (?, ?)",
                            TableName.ENUM_TYPE)))
            {
                insertEnumType.setLong(1, typeKeyByType.get(enumType));
                insertEnumType.setLong(2, typeKeyByType.get(enumType.baseType().obj()));
                insertEnumType.execute();
                long enumKey = getGeneratedKey(insertEnumType);
                try(PreparedStatement insertEnumConstant = connection.prepareStatement(
                        String.format("INSERT INTO %s (enum_type_id, name, value, info) VALUES (?, ?, ?, ?)",
                                TableName.ENUM_TYPE_CONSTANT)))
                {
                    Iterator<DecodeEnumConstant> constIt = enumType.constants().iterator();
                    while (constIt.hasNext())
                    {
                        DecodeEnumConstant constant = constIt.next();
                        insertEnumConstant.setLong(1, enumKey);
                        insertEnumConstant.setString(2, constant.name().asMangledString());
                        insertEnumConstant.setString(3, constant.value());
                        setStringOrNull(insertEnumConstant, 4, constant.info());
                        insertEnumConstant.execute();
                    }
                }
            }
            catch (SQLException e)
            {
                throw new ModelExportingException(e);
            }
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeArrayType arrayType)
        {
            insertType(arrayType.baseType().obj());
            try(PreparedStatement insertArrayType = connection.prepareStatement(
                    String.format("INSERT INTO %s (type_id, base_type_id, min_length, max_length) VALUES (?, ?, ?, ?)",
                            TableName.ARRAY_TYPE)))
            {
                insertArrayType.setLong(1, typeKeyByType.get(arrayType));
                insertArrayType.setLong(2, typeKeyByType.get(arrayType.baseType().obj()));
                insertArrayType.setLong(3, arrayType.size().min());
                insertArrayType.setLong(4, arrayType.size().max());
                insertArrayType.execute();
            }
            catch (SQLException e)
            {
                throw new ModelExportingException(e);
            }
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeStructType structType)
        {
            try(PreparedStatement insertStructType = connection.prepareStatement(
                    String.format("INSERT INTO %s (type_id) VALUES (?)", TableName.STRUCT_TYPE)))
            {
                insertStructType.setLong(1, typeKeyByType.get(structType));
                insertStructType.execute();
                long structKey = getGeneratedKey(insertStructType);
                try(PreparedStatement insertField = connection.prepareStatement(
                        String.format("INSERT INTO %s (struct_type_id, field_index, name, type_id, unit_id, info) VALUES (?, ?, ?, ?, (SELECT id FROM %s WHERE namespace_id = ? AND name = ?), ?)",
                                TableName.STRUCT_TYPE_FIELD, TableName.UNIT)))
                {
                    long index = 0;
                    Preconditions.checkState(!structType.fields().isEmpty(), "struct must not be empty");
                    Iterator<DecodeStructField> fIt = structType.fields().iterator();
                    while (fIt.hasNext())
                    {
                        DecodeStructField field = fIt.next();
                        DecodeTypeUnitApplication typeUnit = field.typeUnit();
                        insertField.setLong(1, structKey);
                        insertField.setLong(2, index++);
                        insertField.setString(3, field.name().asMangledString());
                        insertType(typeUnit.t().obj());
                        insertField.setLong(4, typeKeyByType.get(typeUnit.t().obj()));
                        insertField.setLong(5, namespaceKeyByNamespaceMap.get(structType.namespace()));
                        setStringOrNull(insertField, 6, Option.apply(typeUnit.unit().isDefined()
                                ? typeUnit.unit().get().obj().name().asMangledString() : null));
                        setStringOrNull(insertField, 7, field.info());
                        insertField.execute();
                    }
                }
            }
            catch (SQLException e)
            {
                throw new ModelExportingException(e);
            }
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeAliasType typeAlias)
        {
            insertType(typeAlias.baseType().obj());
            try(PreparedStatement insertArrayType = connection.prepareStatement(
                    String.format("INSERT INTO %s (type_id, base_type_id) VALUES (?, ?)",
                            TableName.ALIAS_TYPE)))
            {
                insertArrayType.setLong(1, typeKeyByType.get(typeAlias));
                insertArrayType.setLong(2, typeKeyByType.get(typeAlias.baseType().obj()));
                insertArrayType.execute();
            }
            catch (SQLException e)
            {
                throw new ModelExportingException(e);
            }
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeGenericType genericType)
        {
            throw new AssertionError("not implemented");
        }

        @Override
        public Void visit(@NotNull DecodeGenericTypeSpecialized genericTypeSpecialized)
        {
            throw new AssertionError("not implemented");
        }
    }

    private static class DecodeInsertMessageVisitor implements DecodeMessageVisitor<Void>
    {
        private final long messageKey;
        @NotNull
        private final Connection connection;

        public DecodeInsertMessageVisitor(
                long messageKey, @NotNull Connection connection)
        {
            this.messageKey = messageKey;
            this.connection = connection;
        }

        @Override
        public Void visit(@NotNull DecodeEventMessage eventMessage)
        {
            try(PreparedStatement insertEventMessage = connection.prepareStatement(
                    String.format("INSERT INTO %s (message_id) VALUES (?)", TableName.EVENT_MESSAGE)))
            {
                insertEventMessage.setLong(1, messageKey);
                insertEventMessage.execute();
            }
            catch (SQLException e)
            {
                throw new ModelExportingException(e);
            }
            return null;
        }

        @Override
        public Void visit(@NotNull DecodeStatusMessage statusMessage)
        {
            try(PreparedStatement insertEventMessage = connection.prepareStatement(
                    String.format("INSERT INTO %s (message_id) VALUES (?)", TableName.STATUS_MESSAGE)))
            {
                insertEventMessage.setLong(1, messageKey);
                insertEventMessage.execute();
            }
            catch (SQLException e)
            {
                throw new ModelExportingException(e);
            }
            return null;
        }
    }
}
