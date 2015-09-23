package ru.mipt.acsl.generator.java.ast;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Artem Shein
 */
public class JavaTypeApplication implements JavaType
{
    @NotNull
    private final String type;
    @NotNull
    private final List<JavaType> genericParameters;

    public JavaTypeApplication(@NotNull Class<?> cls)
    {
        this(cls.getCanonicalName(), new ArrayList<>());
    }

    public JavaTypeApplication(@NotNull Class<?> cls, @NotNull JavaType... genericParameters)
    {
        this(cls.getCanonicalName(), Lists.newArrayList(genericParameters));
    }

    public JavaTypeApplication(@NotNull String type, @NotNull List<JavaType> genericParameters)
    {
        this.type = type;
        this.genericParameters = genericParameters;
    }

    public JavaTypeApplication(@NotNull String fqn)
    {
        this(fqn, new ArrayList<>());
    }

    public JavaTypeApplication(@NotNull String fqn, @NotNull JavaType... genericParameters)
    {
        this(fqn, Lists.newArrayList(genericParameters));
    }

    @Override
    @NotNull
    public List<JavaType> getGenericParameters()
    {
        return genericParameters;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state, @NotNull Appendable appendable) throws IOException
    {
        appendable.append(type);
        if (!genericParameters.isEmpty())
        {
            appendable.append("<");
            boolean isFirst = true;
            for (JavaType parameter: genericParameters)
            {
                if (isFirst)
                {
                    isFirst = false;
                }
                else
                {
                    appendable.append(", ");
                }
                parameter.generate(state, appendable);
            }
            appendable.append(">");
        }
    }

    @NotNull
    @Override
    public String getFqn()
    {
        return type;
    }
}
