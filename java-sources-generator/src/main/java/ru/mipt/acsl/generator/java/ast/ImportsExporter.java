package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImportsExporter
{
    @NotNull
    private final Map<String, String> imports;
    @NotNull
    private final String basePackage;

    public ImportsExporter(@NotNull Map<String, String> imports, @NotNull String basePackage)
    {
        this.imports = imports;
        this.basePackage = basePackage;
    }

    public void export(@NotNull AbstractJavaBaseClass javaClass)
    {
        Optional<JavaType> extendsClass = javaClass.getExtendsClass();
        if (extendsClass.isPresent())
        {
            JavaType newExtendClass = export(extendsClass.get());
            if (newExtendClass != null)
            {
                javaClass.setExtendsClass(newExtendClass);
            }
        }
        javaClass.getFields().stream().forEach(this::export);
        javaClass.getConstructors().stream().forEach(this::export);
        javaClass.getMethods().stream().forEach(this::export);
        javaClass.getInnerClasses().stream().forEach(this::export);
    }

    @Nullable
    private JavaType export(@NotNull JavaType type)
    {
        String a = exportTypeFqn(type.getFqn());
        List<JavaType> genericParameters = type.getGenericParameters();
        int paramsCount = genericParameters.size();
        for (int i = 0; i < paramsCount; i++)
        {
            JavaType newParam = export(genericParameters.get(i));
            if (newParam != null)
            {
                genericParameters.set(i, newParam);
            }
        }
        if (a != null)
        {
            return new JavaTypeApplication(a, genericParameters);
        }
        return null;
    }

    private void export(@NotNull JavaClassMethod method)
    {
        JavaType newReturnType = export(method.getReturnType());
        if (newReturnType != null)
        {
            method.setReturnType(newReturnType);
        }
        List<JavaMethodArgument> arguments = method.getArguments();
        for (JavaMethodArgument argument : arguments)
        {
            JavaType newType = export(argument.getType());
            if (newType != null)
            {
                argument.setType(newType);
            }
        }
    }

    public void export(@NotNull JavaField field)
    {
        JavaType newType = export(field.getType());
        if (newType != null)
        {
            field.setType(newType);
        }
    }

    @Nullable
    private String exportTypeFqn(@NotNull String typeFqn)
    {
        int index = typeFqn.lastIndexOf('.');
        if (index != -1)
        {
            String packageFqn = typeFqn.substring(0, index);
            String typeName = typeFqn.substring(index + 1);
            if (basePackage.equals(packageFqn) || "java.lang".equals(packageFqn))
            {
                return typeName;
            }
            String importedPackageFqn = imports.get(typeName);
            if (importedPackageFqn == null)
            {
                imports.put(typeName, packageFqn);
                return typeName;
            }
            else if (importedPackageFqn.equals(packageFqn))
            {
                return typeName;
            }
        }
        return null;
    }
}
