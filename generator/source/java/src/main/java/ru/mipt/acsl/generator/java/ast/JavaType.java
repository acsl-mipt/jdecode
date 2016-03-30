package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Artem Shein
 */
public interface JavaType extends JavaAstElement
{
    @NotNull
    List<JavaType> getGenericParameters();

    @NotNull
    String getFqn();

    enum Primitive implements JavaType
    {
        BOOLEAN("boolean"), FLOAT("float"), DOUBLE("double"), BYTE("byte"), SHORT("short"), INT("int"), LONG("long");

        @NotNull
        private final String name;

        Primitive(@NotNull String name)
        {
            this.name = name;
        }

        @Override
        @NotNull
        public String getFqn()
        {
            return name;
        }

        @NotNull
        @Override
        public List<JavaType> getGenericParameters()
        {
            return Collections.emptyList();
        }

        @Override
        public void generate(@NotNull JavaGeneratorState state)
        {
            state.append(name);
        }
    }

    enum Std implements JavaType
    {
        BYTE("java.lang.Byte"),
        BIG_INTEGER("java.math.BigInteger"), SHORT("java.lang.Short"), INTEGER("java.lang.Integer"), LONG("java.lang.Long"),
        FLOAT("java.lang.Float"), DOUBLE("java.lang.Double"), BOOLEAN("java.lang.Boolean");

        @NotNull
        private final String typeFqn;

        Std(@NotNull String typeFqn)
        {
            this.typeFqn = typeFqn;
        }

        @Override
        @NotNull
        public String getFqn()
        {
            return typeFqn;
        }

        @NotNull
        @Override
        public List<JavaType> getGenericParameters()
        {
            return Collections.emptyList();
        }

        @Override
        public void generate(@NotNull JavaGeneratorState state)
        {
            state.append(typeFqn);
        }
    }
}
