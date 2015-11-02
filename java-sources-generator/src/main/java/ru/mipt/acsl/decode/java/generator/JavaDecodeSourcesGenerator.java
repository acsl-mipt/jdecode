package ru.mipt.acsl.decode.java.generator;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.generation.GenerationException;
import ru.mipt.acsl.generation.Generator;
import ru.mipt.acsl.generator.java.ast.*;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import ru.mipt.acsl.decode.model.domain.type.*;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Artem Shein
 */
public class JavaDecodeSourcesGenerator implements Generator<JavaDecodeSourcesGeneratorConfiguration>
{
    @NotNull
    private static final Logger LOG = LoggerFactory.getLogger(JavaDecodeSourcesGenerator.class);
    @NotNull
    private final JavaDecodeSourcesGeneratorConfiguration config;
    private static int uniqueId = 0;

    public JavaDecodeSourcesGenerator(@NotNull JavaDecodeSourcesGeneratorConfiguration config)
    {
        this.config = config;
    }

    @Override
    public void generate()
    {
        LOG.info("Generating Java sources to '{}'", config.getOutputDir().getAbsolutePath());
        generateNamespaces(config.getRegistry().getRootNamespaces());
    }

    private void generateNamespaces(@NotNull List<DecodeNamespace> namespaces)
    {
        namespaces.stream().forEach(this::generateNamespace);
    }

    private void generateNamespace(@NotNull DecodeNamespace namespace)
    {
        generateNamespaces(namespace.getSubNamespaces());
        namespace.getComponents().stream().forEach(this::generateComponent);
        namespace.getUnits().stream().forEach(this::generateUnit);
        namespace.getTypes().stream().forEach(this::generateType);
    }

    private void generateType(@NotNull DecodeType type)
    {
        Optional<AbstractJavaBaseClass> javaClassOptional =
                type.accept(new DecodeTypeVisitor<Optional<AbstractJavaBaseClass>>()
                {
                    @Override
                    @NotNull
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodePrimitiveType primitiveType) throws RuntimeException
                    {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodeNativeType nativeType) throws
                            RuntimeException
                    {
                        return Optional.empty();
                    }

                    @Override
                    @NotNull
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodeSubType subType) throws RuntimeException
                    {
                        DecodeType baseType = subType.getBaseType().getObject();
                        JavaClass javaClass = JavaClass.newBuilder(subType.getNamespace().getFqn().asString(),
                                // FIXME
                                classNameFromTypeName(subType.getOptionalName().get().asString()))
                                .extendsClass(getJavaTypeForDecodeType(baseType, false))
                                .build();
                        return Optional.of(javaClass);
                    }

                    @Override
                    @NotNull
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodeEnumType enumType) throws RuntimeException
                    {
                        JavaEnum javaEnum = JavaEnum.newBuilder(enumType.getNamespace().getFqn().asString(),
                                // FIXME
                                classNameFromEnumName(enumType.getOptionalName().get().asString())).build();
                        return Optional.of(javaEnum);
                    }

                    @Override
                    @NotNull
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodeArrayType arrayType) throws RuntimeException
                    {
                        JavaClass javaClass = JavaClass.newBuilder(arrayType.getNamespace().getFqn().asString(),
                                classNameFromArrayType(arrayType))
                                .genericArgument("T")
                                .extendsClass("decode.Array", new JavaTypeApplication("T"))
                                .constuctor(Collections.emptyList(),
                                        new JavaSuperCallStatement(
                                                new JavaLongExpr(arrayType.getSize().getMinLength()),
                                                new JavaLongExpr(arrayType.getSize().getMaxLength())))
                                .build();
                        return Optional.of(javaClass);
                    }

                    @Override
                    @NotNull
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodeStructType structType) throws RuntimeException
                    {
                        JavaClass javaClass = JavaClass.newBuilder(structType.getNamespace().getFqn().asString(),
                                classNameFromTypeName(getOrMakeUniqueName(structType)))
                                .constuctor(
                                        structType.getFields().stream().map(f ->
                                                new JavaMethodArgument(
                                                        getJavaTypeForDecodeType(f.getType().getObject(), false),
                                                        f.getName().asString())).collect(Collectors.toList()),
                                        structType.getFields().stream().map(f ->
                                                new JavaAssignStatement(
                                                        new JavaVarExpr("this." + f.getName().asString()),
                                                        new JavaVarExpr(f.getName().asString())))
                                                .collect(Collectors.toList()))
                                .build();
                        final List<JavaField> fields = javaClass.getFields();
                        final List<JavaClassMethod> methods = javaClass.getMethods();
                        structType.getFields().stream().forEach((f) ->
                        {
                            JavaType javaType = getJavaTypeForDecodeType(f.getType().getObject(), false);
                            String fieldName = f.getName().asString();
                            JavaField field = new JavaField(JavaVisibility.PRIVATE, false, false, javaType,
                                    fieldName);
                            methods.add(new JavaClassMethod(JavaVisibility.PUBLIC, false, javaType, "get" +
                                    StringUtils.capitalize(fieldName), Collections.emptyList(),
                                    Lists.newArrayList(new JavaReturnStatement(new JavaVarExpr(fieldName)))));
                            fields.add(field);
                        });
                        return Optional.of(javaClass);
                    }

                    @Override
                    @NotNull
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodeAliasType typeAlias) throws RuntimeException
                    {
                        return Optional.empty();
                    }
                });
        if (javaClassOptional.isPresent())
        {
            generateJavaClass(javaClassOptional.get());
        }
    }

    @NotNull
    private String getOrMakeUniqueName(DecodeOptionalNameAware optionalNameAware)
    {
        Optional<DecodeName> optionalName = optionalNameAware.getOptionalName();
        return optionalName.isPresent() ? optionalName.get().asString() : makeUniqueName();
    }

    @NotNull
    private String makeUniqueName()
    {
        return "name" + uniqueId++;
    }

    @NotNull
    private String classNameFromEnumName(@NotNull String enumName)
    {
        return classNameFromTypeName(enumName);
    }

    @NotNull
    private String classNameFromTypeName(@NotNull String typeName)
    {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, typeName);
    }

    @NotNull
    private JavaType getJavaTypeForDecodeType(@NotNull DecodeType type, boolean genericUse)
    {
        return type.accept(new DecodeTypeVisitor<JavaType>()
        {
            @Override
            public JavaType visit(@NotNull DecodePrimitiveType primitiveType)
            {
                switch (primitiveType.getKind())
                {
                    case INT:
                        switch ((byte) primitiveType.getBitLength())
                        {
                            case 8:
                                return genericUse ? JavaType.Std.BYTE : JavaType.Primitive.BYTE;
                            case 16:
                                return genericUse ? JavaType.Std.SHORT : JavaType.Primitive.SHORT;
                            case 32:
                                return genericUse ? JavaType.Std.INTEGER : JavaType.Primitive.INT;
                            case 64:
                                return genericUse ? JavaType.Std.LONG : JavaType.Primitive.LONG;
                            default:
                                throw new AssertionError();
                        }
                    case UINT:
                        switch ((byte) primitiveType.getBitLength())
                        {
                            case 8:
                                return genericUse ? JavaType.Std.SHORT : JavaType.Primitive.SHORT;
                            case 16:
                                return genericUse ? JavaType.Std.INTEGER : JavaType.Primitive.INT;
                            case 32:
                                return genericUse ? JavaType.Std.LONG : JavaType.Primitive.LONG;
                            case 64:
                                return JavaType.Std.BIG_INTEGER;
                            default:
                                throw new AssertionError();
                        }
                    case FLOAT:
                        switch ((byte) primitiveType.getBitLength())
                        {
                            case 32:
                                return genericUse ? JavaType.Std.FLOAT : JavaType.Primitive.FLOAT;
                            case 64:
                                return genericUse ? JavaType.Std.DOUBLE : JavaType.Primitive.DOUBLE;
                            default:
                                throw new AssertionError();
                        }
                    case BOOL:
                        return genericUse ? JavaType.Std.BOOLEAN : JavaType.Primitive.BOOLEAN;
                }
                throw new AssertionError();
            }

            @Override
            public JavaType visit(@NotNull DecodeNativeType nativeType)
            {
                return new JavaTypeApplication("decode.Ber");
            }

            @Override
            public JavaType visit(@NotNull DecodeSubType subType)
            {
                return new JavaTypeApplication(subType.getNamespace().getFqn().asString() + "." + classNameFromTypeName(
                        // FIXME
                        subType.getOptionalName().get().asString()));
            }

            @Override
            public JavaType visit(@NotNull DecodeEnumType enumType)
            {
                return new JavaTypeApplication(enumType.getNamespace().getFqn().asString() + "." +
                        // FIXME
                        classNameFromTypeName(enumType.getOptionalName().get().asString()));
            }

            @Override
            public JavaType visit(@NotNull DecodeArrayType arrayType)
            {
                return new JavaTypeApplication(
                        arrayType.getNamespace().getFqn().asString() + "." + classNameFromArrayType(
                                arrayType), getJavaTypeForDecodeType(arrayType.getBaseType().getObject(), true));
            }

            @Override
            public JavaType visit(@NotNull DecodeStructType structType)
            {
                return new JavaTypeApplication(structType.getNamespace().getFqn().asString() + "." +
                        // FIXME
                        classNameFromTypeName(structType.getOptionalName().get().asString()));
            }

            @Override
            public JavaType visit(@NotNull DecodeAliasType typeAlias)
            {
                return getJavaTypeForDecodeType(typeAlias.getType().getObject(), genericUse);
            }
        });
    }

    @NotNull
    private String classNameFromArrayType(@NotNull DecodeArrayType arrayType)
    {
        return "Array" + (arrayType.isFixedSize()
                ? arrayType.getSize().getMinLength()
                : (arrayType.getSize().getMaxLength() == 0 ? "" : arrayType.getSize().getMinLength() + "_" + arrayType.getSize().getMaxLength()));
    }

    private void generateUnit(@NotNull DecodeUnit unit)
    {
        JavaClass javaClass =
                JavaClass.newBuilder(fqnToJavaPackage(unit.getNamespace().getFqn().asString()), unitNameToClassName(
                        unit.getName().asString())).build();
        javaClass.getFields().add(new JavaField(JavaVisibility.PUBLIC, true, true,
                new JavaTypeApplication(Optional.class, new JavaTypeApplication(String.class)), "DISPLAY",
                Optional.of(unit.getDisplay().isPresent() ?
                        new JavaClassMethodCallExpr(new JavaTypeApplication(Optional.class), "of",
                                new JavaStringExpr(unit.getDisplay().get())) :
                        new JavaClassMethodCallExpr(Optional.class, "empty"))));
        generateJavaClass(javaClass);
    }

    @NotNull
    private String fqnToJavaPackage(@NotNull String fqn)
    {
        return fqn.toLowerCase();
    }

    @NotNull
    private String unitNameToClassName(@NotNull String unitName)
    {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, unitName) + "Unit";
    }

    private void generateJavaClass(@NotNull AbstractJavaBaseClass javaClass)
    {
        File dir = createPathIfNotExistsForPackage(javaClass.getPackage());
        File javaFile = new File(dir, javaClass.getName() + ".java");
        try (OutputStream os = new FileOutputStream(javaFile))
        {
            try (OutputStreamWriter writer = new OutputStreamWriter(os))
            {
                JavaGeneratorState state = new JavaGeneratorState(writer);
                writer.append("package ").append(javaClass.getPackage()).append(";");
                state.eol();
                Map<String, String> imports = new HashMap<>();
                new ImportsExporter(imports, javaClass.getPackage()).export(javaClass);
                if (!imports.isEmpty())
                {
                    for (Map.Entry<String, String> _import : imports.entrySet())
                    {
                        writer.append("import ").append(_import.getValue()).append('.').append(_import.getKey()).append(';');
                        state.eol();
                    }
                }
                state.eol();
                javaClass.generate(state, writer);
            }
        }
        catch (Exception e)
        {
            throw new GenerationException(e);
        }
    }

    @NotNull
    private File createPathIfNotExistsForPackage(@NotNull String _package)
    {
        File dir = new File(config.getOutputDir(), _package.replaceAll(Pattern.quote("."), "/"));
        if (!dir.exists())
        {
            Preconditions.checkState(dir.mkdirs());
        }
        return dir;
    }

    private void generateComponent(@NotNull DecodeComponent component)
    {
        String componentClassName = classNameFromComponentName(
                component.getName().asString());
        JavaClass.Builder componentClassBuilder = JavaClass.newBuilder(component.getNamespace().getFqn().asString(),
                componentClassName).visibilityPublic();
        componentClassBuilder.publicStaticFinalField(String.class, "FQN", new JavaStringExpr(component.getNamespace().getFqn().asString() + "." + component.getName().asString()));
        component.getMessages().stream().forEach(m ->
        {
            JavaClass.Builder messageClassBuilder = JavaClass.newBuilder("", classNameFromMessageName(
                    m.getName().asString()));
            messageClassBuilder.visibilityPublic();
            messageClassBuilder.staticClass();
            List<JavaMethodArgument> ctorArgs = new ArrayList<>();
            List<JavaStatement> ctorStatements = new ArrayList<>();
            for (DecodeMessageParameter param : m.getParameters())
            {
                JavaType type = getJavaTypeForDecodeType(component.getTypeForParameter(param), false);
                String name = getFieldNameForParameter(param);
                messageClassBuilder.privateField(type, name);
                ctorArgs.add(new JavaMethodArgument(type, name));
                ctorStatements.add(new JavaAssignStatement(new JavaVarExpr("this." + name), new JavaVarExpr(name)));
                messageClassBuilder.publicMethod(type, "get" + StringUtils.capitalize(name), Collections.emptyList(),
                        Lists.newArrayList(new JavaReturnStatement(new JavaVarExpr(name))));
            }
            messageClassBuilder.publicStaticFinalField(String.class, "FQN", new JavaAddExpr(new JavaClassFieldExpr(
                    new JavaTypeApplication(componentClassName), "FQN"),
                    new JavaStringExpr("." + m.getName().asString())));
            messageClassBuilder.constuctor(ctorArgs, ctorStatements);
            componentClassBuilder.innerClass(messageClassBuilder.build());
        });
        generateJavaClass(componentClassBuilder.build());
    }

    @NotNull
    private static String classNameFromComponentName(@NotNull String componentName)
    {
        return componentName + "Component";
    }

    @NotNull
    private static String getFieldNameForParameter(@NotNull DecodeMessageParameter parameter)
    {
        return parameter.getValue().replaceAll("[\\.\\[\\]]", "_");
    }

    private Stream<DecodeMessageParameter> getParametersStreamForParameter(@NotNull DecodeMessageParameter parameter, @NotNull
    DecodeComponent component)
    {
        /*TODO: implement * and *.* parameters
        if (parameter.equals(ImmutableDecodeDeepAllParameters.INSTANCE))
        {

        }
        else if (parameter.equals(ImmutableDecodeAllParameters.INSTANCE))
        {
            Optional<DecodeMaybeProxy<DecodeType>> baseTypeOptional = component.getBaseType();
            if (baseTypeOptional.isPresent())
            {
                baseTypeOptional.get().getObject().
            }
        }*/
        return Stream.of(parameter);
    }

    @NotNull
    private String classNameFromMessageName(@NotNull String name)
    {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name) + "Message";
    }

    @NotNull
    @Override
    public JavaDecodeSourcesGeneratorConfiguration getConfiguration()
    {
        return config;
    }
}
