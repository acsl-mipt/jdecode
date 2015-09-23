package ru.mipt.acsl.decode.model.exporter;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public enum TableName
{
    SUB_NAMESPACE("sub_namespace"), NAMESPACE("namespace"), UNIT("unit"), TYPE("type"), PRIMITIVE_TYPE("primitive_type"),
    ENUM_TYPE("enum_type"), ENUM_TYPE_CONSTANT("enum_type_constant"), DYNAMIC_ARRAY_TYPE("dynamic_array_type"),
    ARRAY_TYPE("array_type"), ARRAY_TYPE_SIZE("array_type_size"), STRUCT_TYPE("struct_type"),
    STRUCT_TYPE_FIELD("struct_type_field"), COMPONENT("component"), SUB_COMPONENT("sub_component"), COMMAND("command"),
    COMMAND_ARGUMENT("command_argument"), MESSAGE("message"), EVENT_MESSAGE("event_message"), STATUS_MESSAGE("status_message"),
    DYNAMIC_STATUS_MESSAGE("dynamic_status_message"), MESSAGE_PARAMETER("message_parameter"), SUB_TYPE("sub_type"),
    ALIAS_TYPE("alias_type"), NATIVE_TYPE("native_type");

    @NotNull
    private final String name;

    TableName(@NotNull String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
