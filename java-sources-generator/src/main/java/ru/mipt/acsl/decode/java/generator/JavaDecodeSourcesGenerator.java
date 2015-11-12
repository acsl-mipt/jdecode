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
import scala.Option;
import scala.collection.JavaConversions;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.mipt.acsl.decode.java.generator.JavaDecodeTypeVisitor.*;
import static scala.collection.JavaConversions.asJavaCollection;
import static scala.collection.JavaConversions.bufferAsJavaList;

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
        generateNamespaces(bufferAsJavaList(config.getRegistry().rootNamespaces()));
    }

    private void generateNamespaces(@NotNull List<DecodeNamespace> namespaces)
    {
        namespaces.stream().forEach(this::generateNamespace);
    }

    private void generateNamespace(@NotNull DecodeNamespace namespace)
    {
        generateNamespaces(bufferAsJavaList(namespace.subNamespaces()));
        bufferAsJavaList(namespace.components()).stream().forEach(this::generateComponent);
        bufferAsJavaList(namespace.units()).stream().forEach(this::generateUnit);
        bufferAsJavaList(namespace.types()).stream().forEach(this::generateType);
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
                        DecodeType baseType = subType.baseType().obj();
                        JavaClass javaClass = JavaClass.newBuilder(subType.namespace().fqn().asString(),
                                // FIXME
                                classNameFromTypeName(subType.optionalName().get().asString()))
                                .extendsClass(getJavaTypeForDecodeType(baseType, false))
                                .build();
                        return Optional.of(javaClass);
                    }

                    @Override
                    @NotNull
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodeEnumType enumType) throws RuntimeException
                    {
                        JavaEnum javaEnum = JavaEnum.newBuilder(enumType.namespace().fqn().asString(),
                                // FIXME
                                classNameFromEnumName(enumType.optionalName().get().asString())).build();
                        return Optional.of(javaEnum);
                    }

                    @Override
                    @NotNull
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodeArrayType arrayType) throws RuntimeException
                    {
                        JavaClass javaClass = JavaClass.newBuilder(arrayType.namespace().fqn().asString(),
                                classNameFromArrayType(arrayType))
                                .genericArgument("T")
                                .extendsClass("decode.Array", new JavaTypeApplication("T"))
                                .constuctor(Collections.emptyList(),
                                        new JavaSuperCallStatement(
                                                new JavaLongExpr(arrayType.size().minLength()),
                                                new JavaLongExpr(arrayType.size().maxLength())))
                                .build();
                        return Optional.of(javaClass);
                    }

                    @Override
                    @NotNull
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodeStructType structType) throws RuntimeException
                    {
                        JavaClass javaClass = JavaClass.newBuilder(structType.namespace().fqn().asString(),
                                classNameFromTypeName(getOrMakeUniqueName(structType)))
                                .constuctor(
                                        asJavaCollection(structType.fields()).stream().map(f ->
                                                new JavaMethodArgument(
                                                        getJavaTypeForDecodeType(f.fieldType().obj(), false),
                                                        f.name().asString())).collect(Collectors.toList()),
                                        asJavaCollection(structType.fields()).stream().map(f ->
                                                new JavaAssignStatement(
                                                        new JavaVarExpr("this." + f.name().asString()),
                                                        new JavaVarExpr(f.name().asString())))
                                                .collect(Collectors.toList()))
                                .build();
                        final List<JavaField> fields = javaClass.getFields();
                        final List<JavaClassMethod> methods = javaClass.getMethods();
                        asJavaCollection(structType.fields()).stream().forEach((f) ->
                        {
                            JavaType javaType = getJavaTypeForDecodeType(f.fieldType().obj(), false);
                            String fieldName = f.name().asString();
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

                    @Override
                    public Optional<AbstractJavaBaseClass> visit(@NotNull DecodeGenericType genericType)
                    {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<AbstractJavaBaseClass> visit(
                            @NotNull DecodeGenericTypeSpecialized genericTypeSpecialized)
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
        Option<DecodeName> optionalName = optionalNameAware.optionalName();
        return optionalName.isDefined() ? optionalName.get().asString() : makeUniqueName();
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

    private void generateUnit(@NotNull DecodeUnit unit)
    {
        JavaClass javaClass =
                JavaClass.newBuilder(fqnToJavaPackage(unit.namespace().fqn().asString()), unitNameToClassName(
                        unit.name().asString())).build();
        javaClass.getFields().add(new JavaField(JavaVisibility.PUBLIC, true, true,
                new JavaTypeApplication(Optional.class, new JavaTypeApplication(String.class)), "DISPLAY",
                Optional.of(unit.display().isDefined() ?
                        new JavaClassMethodCallExpr(new JavaTypeApplication(Optional.class), "of",
                                new JavaStringExpr(unit.display().get())) :
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
                javaClass.generate(state);
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
                component.name().asString());
        JavaClass.Builder componentClassBuilder = JavaClass.newBuilder(component.namespace().fqn().asString(),
                componentClassName).visibilityPublic();
        componentClassBuilder.publicStaticFinalField(String.class, "FQN", new JavaStringExpr(component.namespace().fqn().asString() + "." + component.name().asString()));
        asJavaCollection(component.messages()).stream().forEach(m ->
        {
            JavaClass.Builder messageClassBuilder = JavaClass.newBuilder("", classNameFromMessageName(
                    m.name().asString()));
            messageClassBuilder.visibilityPublic();
            messageClassBuilder.staticClass();
            List<JavaMethodArgument> ctorArgs = new ArrayList<>();
            List<JavaStatement> ctorStatements = new ArrayList<>();
            for (DecodeMessageParameter param : asJavaCollection(m.parameters()))
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
                    new JavaStringExpr("." + m.name().asString())));
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
        return parameter.value().replaceAll("[\\.\\[\\]]", "_");
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
