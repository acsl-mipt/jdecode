package ru.mipt.acsl.generator.java.ast;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class JavaClass extends AbstractJavaBaseClass
{
    public JavaClass(@NotNull JavaVisibility visibility, boolean isStatic, @NotNull String _package,
                     @NotNull String name,
                     @NotNull List<String> genericArguments,
                     @NotNull Optional<JavaType> extendsClass,
                     @NotNull List<JavaConstructor> constructors, List<JavaField> fields,
                     List<JavaClassMethod> methods, @NotNull List<AbstractJavaBaseClass> innerClasses)
    {
        super(visibility, isStatic, _package, name, genericArguments, extendsClass, fields, methods, innerClasses, constructors);
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state)
    {
        generateVisibility(state);
        generateStatic(state);
        state.append("class ").append(name);
        generateGenericArguments(state);

        if (extendsClass.isPresent())
        {
            state.append(" extends ");
            extendsClass.get().generate(state);
        }

        state.startBlock();

        for (JavaField field : fields)
        {
            state.indent();
            field.generate(state);
            state.eol();
        }

        for (JavaConstructor constructor : constructors)
        {
            state.indent();
            constructor.generate(state);
            state.eol();
        }

        for (JavaClassMethod method : methods)
        {
            state.indent();
            method.generate(state);
            state.eol();
        }

        generateInnerClasses(state);

        state.finishBlock();
    }

    public static Builder newBuilder(@NotNull String packageFqn, @NotNull String name)
    {
        return new Builder(packageFqn, name);
    }

    public static class Builder
    {
        @NotNull
        private final String packageFqn;
        @NotNull
        private final String name;
        @NotNull
        private Optional<JavaType> extendsClass = Optional.empty();
        @NotNull
        private List<JavaConstructor> constructors = new ArrayList<>();
        @NotNull
        private List<String> genericArguments = new ArrayList<>();
        @NotNull
        private List<AbstractJavaBaseClass> innerClasses = new ArrayList<>();
        @NotNull
        private List<JavaField> fields = new ArrayList<>();
        @NotNull
        private List<JavaClassMethod> methods = new ArrayList<>();
        @NotNull
        private JavaVisibility visibility = JavaVisibility.PACKAGE_PRIVATE;
        private boolean isStatic = false;

        public Builder(@NotNull String packageFqn, @NotNull String name)
        {
            this.packageFqn = packageFqn;
            this.name = name;
        }


        public JavaClass build()
        {
            return new JavaClass(visibility, isStatic, packageFqn, name, genericArguments, extendsClass, constructors, fields, methods, innerClasses);
        }

        @NotNull
        public Builder extendsClass(@NotNull String packageFqn, @NotNull String name,
                                                                    @NotNull JavaType... genericParameters)
        {
            return extendsClass(packageFqn + "." + name, genericParameters);
        }

        public Builder extendsClass(@NotNull String fqn, @NotNull JavaType... genericParameters)
        {
            Preconditions.checkState(!extendsClass.isPresent());
            extendsClass = Optional.of(new JavaTypeApplication(fqn, genericParameters));
            return this;
        }

        public Builder constuctor(@NotNull List<JavaMethodArgument> arguments,
                                                                  @NotNull JavaStatement... statements)
        {
            return constuctor(arguments, Lists.newArrayList(statements));
        }

        public Builder constuctor(@NotNull List<JavaMethodArgument> arguments,
                                  @NotNull List<JavaStatement> statements)
        {
            constructors.add(new JavaConstructor(JavaVisibility.PUBLIC, name, arguments, Lists.newArrayList(statements)));
            return this;
        }

        public Builder genericArgument(@NotNull String name)
        {
            genericArguments.add(name);
            return this;
        }

        public Builder extendsClass(@NotNull JavaType extendsType)
        {
            this.extendsClass = Optional.of(extendsType);
            return this;
        }

        public Builder innerClass(@NotNull JavaClass innerClass)
        {
            this.innerClasses.add(innerClass);
            return this;
        }

        public Builder privateField(@NotNull JavaType type, @NotNull String fieldNameForParameter)
        {
            this.fields.add(new JavaField(JavaVisibility.PRIVATE, false, false, type, fieldNameForParameter));
            return this;
        }

        public Builder publicMethod(@NotNull JavaType type, @NotNull String name, @NotNull List<JavaMethodArgument> arguments,
                                    @NotNull List<JavaStatement> statements)
        {
            this.methods.add(new JavaClassMethod(JavaVisibility.PUBLIC, false, type, name, arguments, statements));
            return this;
        }

        public Builder visibilityPublic()
        {
            this.visibility = JavaVisibility.PUBLIC;
            return this;
        }

        public Builder staticClass()
        {
            this.isStatic = true;
            return this;
        }

        public Builder publicStaticFinalField(@NotNull Class<?> cls, @NotNull String name,
                                              @NotNull JavaExpr initialValue)
        {
            this.fields.add(new JavaField(JavaVisibility.PUBLIC, true, true,
                    new JavaTypeApplication(cls.getCanonicalName()), name, Optional.of(initialValue)));
            return this;
        }
    }
}
