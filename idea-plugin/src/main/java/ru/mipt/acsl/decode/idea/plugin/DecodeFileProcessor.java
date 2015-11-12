package ru.mipt.acsl.decode.idea.plugin;

import com.google.common.collect.Lists;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.apache.commons.lang.NotImplementedException;
import ru.mipt.acsl.JavaToScala;
import ru.mipt.acsl.ScalaToJava;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.impl.type.*;
import ru.mipt.acsl.decode.modeling.ModelingMessage;
import ru.mipt.acsl.decode.modeling.TransformationMessage;
import ru.mipt.acsl.decode.modeling.TransformationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.parser.psi.*;
import ru.mipt.acsl.decode.parser.psi.DecodeUnit;
import scala.Option;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Buffer;

import java.util.*;
import java.util.stream.Collectors;

import static ru.mipt.acsl.JavaToScala.asOption;
import static ru.mipt.acsl.ScalaToJava.asOptional;
import static ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy.object;
import static ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy.proxy;
import static ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy.proxyForSystem;

/**
 * @author Artem Shein
 */
public class DecodeFileProcessor
{
    @NotNull
    private final DecodeRegistry registry;
    @NotNull
    private final TransformationResult<DecodeRegistry> result;

    public static void notifyUser(@NotNull ModelingMessage msg)
    {
        Notifications.Bus.notify(new Notification(DecodeGenerateSqliteForDecodeSourcesAction.GROUP_DISPLAY_ID,
                msg.getLevel().name(), msg.getText(), getNotificationTypeForLevel(msg.getLevel())));
    }

    public DecodeFileProcessor(@NotNull DecodeRegistry registry, @NotNull TransformationResult<DecodeRegistry> result)
    {
        this.registry = registry;
        this.result = result;
    }

    public void process(@NotNull DecodeFile file)
    {
        List<PsiElement> elements = Lists.newArrayList(file.getNode().getPsi().getChildren());
        Iterator<PsiElement> it = elements.iterator();
        if (!it.hasNext())
        {
            return;
        }
        PsiElement namespaceDecl = it.next();
        if (!(namespaceDecl instanceof DecodeNamespaceDecl))
        {
            error("Expected namespace declaration, found '%s'", namespaceDecl);
            return;
        }
        String namespaceString = ((DecodeNamespaceDecl) namespaceDecl).getElementId().getText();
        DecodeNamespace namespace = DecodeUtils.getOrCreateNamespaceByFqn(registry, namespaceString);
        while (it.hasNext())
        {
            PsiElement element = it.next();

            if (element instanceof DecodeUnitDecl)
            {
                namespace.units().$plus$eq(
                        new DecodeUnitImpl(
                                DecodeNameImpl.newFromSourceName(
                                        ((DecodeUnitDecl) element).getElementNameRule().getText()),
                                namespace,
                                getText(((DecodeUnitDecl) element).getStringValue()),
                                getText(((DecodeUnitDecl) element).getInfoString())));
            }
            else if (element instanceof DecodeTypeDecl)
            {
                namespace.types().$plus$eq(newType((DecodeTypeDecl) element, namespace));
            }
            else if (element instanceof DecodeComponentDecl)
            {
                processComponentDefinition((DecodeComponentDecl) element, namespace);
            }
            else if (element instanceof DecodeAliasDecl)
            {
                namespace.types()
                        .$plus$eq(new DecodeAliasTypeImpl(DecodeNameImpl.newFromSourceName(
                                        ((DecodeAliasDecl) element).getElementId().getText()),
                                namespace,
                                makeProxyForTypeApplication(((DecodeAliasDecl) element).getTypeApplication(), namespace),
                                getText(((DecodeAliasDecl) element).getInfoString())));
            }
            else if (element instanceof PsiWhiteSpace)
            {
                // skip
            }
            else
            {
                error("Unexpected '%s'", element);
            }
        }
    }

    @NotNull
    private static NotificationType getNotificationTypeForLevel(@NotNull TransformationMessage.Level level)
    {
        switch (level)
        {
            case ERROR:
                return NotificationType.ERROR;
            case WARN:
                return NotificationType.WARNING;
            case NOTICE:
                return NotificationType.INFORMATION;
        }
        throw new AssertionError();
    }

    private void processComponentDefinition(@NotNull DecodeComponentDecl componentDecl,
                                            @NotNull DecodeNamespace namespace)
    {
        throw new NotImplementedException();
        /*
        Optional<DecodeTypeDeclBody> typeDeclBodyOptional = Optional.ofNullable(
                componentDecl.getComponentParametersDecl()).map(
                DecodeComponentParametersDecl::getCommandArgs).map(DecodeCommandArgs::getCommandArgList).map(
                DecodeComponentBaseTypeDecl::getTypeDeclBody);
        final String name = componentDecl.getElementNameRule().getText();
        Optional<DecodeMaybeProxy<DecodeType>> baseType = Optional.empty();
        if (typeDeclBodyOptional.isPresent())
        {
            baseType = Optional.of(findExistingOrCreateType(Optional.<DecodeName>empty(),
                    typeDeclBodyOptional.get(),
                    namespace));
        }

        List<DecodeCommand> commands = componentDecl.getCommandDeclList().stream().map(commandDecl -> {
            Optional<DecodeCommandArgs> commandArgs = Optional.ofNullable(commandDecl.getCommandArgs());
            List<DecodeCommandArgument> commandArguments = new ArrayList<>();
            if (commandArgs.isPresent())
            {
                commandArguments = commandArgs.get().getCommandArgList().stream().map(arg -> {
                    DecodeTypeUnitApplication typeUnit = arg.getTypeUnitApplication();
                    DecodeUnit unit = typeUnit.getUnit();
                    return ImmutableDecodeCommandArgument.newInstance(ImmutableDecodeName.newInstanceFromSourceName(
                                    arg.getElementNameRule().getText()),
                            makeProxyForTypeApplication(typeUnit.getTypeApplication(), namespace),
                            Optional.ofNullable(unit).map(u -> proxy(namespace.getFqn(),
                                    ImmutableDecodeName.newInstanceFromSourceName(u.getElementId().getText()))),
                            getText(arg.getInfoString()));
                }).collect(Collectors.toList());
            }
            return ImmutableDecodeCommand.newInstance(ImmutableDecodeName.newInstanceFromSourceName(
                    commandDecl.getElementNameRule().getText()),
                    Integer.parseInt(commandDecl.getNonNegativeNumber().getText()),
                    getText(commandDecl.getInfoString()), commandArguments);
        }).collect(Collectors.toList());

        DecodeComponent component = SimpleDecodeComponent.newInstance(
                ImmutableDecodeName.newInstanceFromSourceName(name),
                namespace, Optional.ofNullable(componentDecl.getNonNegativeNumber()).map(PsiElement::getText)
                        .map(Integer::parseInt), baseType, getText(componentDecl.getInfoString()),
                componentDecl.getSubcomponentDeclList().stream()
                        .map(subcomponentDecl -> SimpleDecodeMaybeProxy.<DecodeComponent>proxy(namespace.getFqn(),
                                ImmutableDecodeName
                                        .newInstanceFromSourceName(
                                                subcomponentDecl.getElementNameRule().getText())))
                        .collect(Collectors.toSet()), commands, new ArrayList<>());
        component.getMessages().addAll(componentDecl.getMessageDeclList().stream().map(messageDecl -> {
            DecodeStatusMessage statusMessage = messageDecl.getStatusMessage();
            DecodeEventMessage eventMessage = messageDecl.getEventMessage();
            DecodeDynamicStatusMessage dynamicStatusMessage = messageDecl.getDynamicStatusMessage();
            String messageSourceName = messageDecl.getElementNameRule().getText();
            Optional<String> infoOptional = getText(messageDecl.getInfoString());
            DecodeName messageName = ImmutableDecodeName.newInstanceFromSourceName(messageSourceName);
            int id = Integer.parseInt(messageDecl.getNonNegativeNumber().getText());
            if (statusMessage != null)
            {
                return ImmutableDecodeStatusMessage.newInstance(component, messageName, id, infoOptional,
                        getMessageParameters(statusMessage.getMessageParametersDecl()));
            }
            if (eventMessage != null)
            {
                return ImmutableDecodeEventMessage.newInstance(component, messageName, id, infoOptional,
                        getMessageParameters(eventMessage.getMessageParametersDecl()));
            }
            if (dynamicStatusMessage != null)
            {
                return ImmutableDecodeDynamicStatusMessage.newInstance(component, messageName, id, infoOptional,
                        getMessageParameters(dynamicStatusMessage.getMessageParametersDecl()));
            }
            throw new AssertionError();
        }).collect(Collectors.toList()));

        namespace.getComponents().add(component);*/
    }

    @NotNull
    private DecodeMaybeProxy<DecodeType> makeProxyForTypeApplication(
            @NotNull DecodeTypeApplication typeApplication,
            @NotNull DecodeNamespace namespace)
    {
        DecodeArrayTypeApplication arrayType = typeApplication.getArrayTypeApplication();
        DecodePrimitiveTypeApplication primitiveType = typeApplication.getPrimitiveTypeApplication();
        DecodeGenericTypeApplication genericTypeApplication = typeApplication.getGenericTypeApplication();
        throw new AssertionError("not implemented: question mark & generic type");
        /*if (genericTypeApplication != null)
        {
            return proxy(namespace.getFqn(), ImmutableDecodeName.newInstanceFromSourceName(genericTypeApplication.getText()));
        }
        if (primitiveType != null)
        {
            return getProxyFor(primitiveType);
        }
        if (arrayType != null)
        {
            return getProxyFor(arrayType, namespace);
        }
        throw new AssertionError();*/
    }

    @NotNull
    private DecodeMaybeProxy<DecodeType> getProxyFor(@NotNull DecodeArrayTypeApplication arrayType,
                                                   @NotNull DecodeNamespace namespace)
    {
        return proxy(getNamespaceFqnFor(arrayType, namespace),
                DecodeNameImpl.newFromSourceName(arrayType.getText()));
    }

    @NotNull
    private DecodeMaybeProxy<DecodeType> getProxyFor(@NotNull DecodeTypeApplication typeApplication,
                                                   @NotNull DecodeNamespace namespace)
    {
        DecodeArrayTypeApplication arrayTypeApplication = typeApplication.getArrayTypeApplication();
        DecodePrimitiveTypeApplication primitiveTypeApplication = typeApplication.getPrimitiveTypeApplication();
        DecodeGenericTypeApplication genericTypeApplication = typeApplication.getGenericTypeApplication();
        throw new AssertionError("not implemented: question mark & generic type");
        /*if (arrayTypeApplication != null)
        {
            return getProxyFor(arrayTypeApplication, namespace);
        }
        if (primitiveTypeApplication != null)
        {
            return getProxyFor(primitiveTypeApplication);
        }
        if (genericTypeApplication != null)
        {
            return proxy(namespace.getFqn(), ImmutableDecodeName.newInstanceFromSourceName(genericTypeApplication.getText()));
        }
        throw new AssertionError();*/
    }

    @NotNull
    private DecodeFqn getNamespaceFqnFor(@NotNull DecodeArrayTypeApplication arrayType, @NotNull DecodeNamespace namespace)
    {
        return getNamespaceFqnFor(arrayType.getTypeApplication(), namespace);
    }

    private DecodeFqn getNamespaceFqnFor(@NotNull DecodeTypeApplication typeApplication,
                                        @NotNull DecodeNamespace namespace)
    {
        DecodeArrayTypeApplication arrayType = typeApplication.getArrayTypeApplication();
        DecodePrimitiveTypeApplication primitiveType = typeApplication.getPrimitiveTypeApplication();
        DecodeGenericTypeApplication genericTypeApplication = typeApplication.getGenericTypeApplication();
        if (arrayType != null)
        {
            return getNamespaceFqnFor(arrayType, namespace);
        }
        if (primitiveType != null)
        {
            return DecodeConstants.SYSTEM_NAMESPACE_FQN;
        }
        if (genericTypeApplication != null)
        {
            String name = genericTypeApplication.getElementId().getText();
            if (name.contains("."))
            {
                return DecodeFqnImpl.newFromSource(name.substring(0, name.lastIndexOf('.') - 1));
            }
            return namespace.fqn();
        }
        throw new AssertionError();
    }

    @NotNull
    private DecodeMaybeProxy<DecodeType> getProxyFor(@NotNull DecodePrimitiveTypeApplication primitiveType)
    {
        return proxyForSystem(DecodeNameImpl.newFromSourceName(primitiveType.getText()));
    }

    @NotNull
    private static List<DecodeMessageParameter> getMessageParameters(
            @NotNull DecodeMessageParametersDecl messageParametersDecl)
    {
        /*if (messageParametersDecl.getDeepAllParameters() != null)
        {
            return ImmutableList.of(ImmutableDecodeDeepAllParameters.INSTANCE);
        }
        if (messageParametersDecl.getAllParameters() != null)
        {
            return ImmutableList.of(ImmutableDecodeAllParameters.INSTANCE);
        }*/
        return messageParametersDecl.getParameterDeclList().stream()
                .map(DecodeParameterDecl::getParameterElement)
                        .map(e -> ImmutableDecodeMessageParameter.newInstance(e.getText()))
                        .collect(Collectors.toList());
    }

    @NotNull
    private DecodeMaybeProxy<DecodeType> findExistingOrCreateType(@NotNull Option<DecodeName> name, @NotNull DecodeTypeDeclBody typeDeclBody,
                                                                  @NotNull DecodeNamespace namespace)
    {
        DecodeTypeApplication typeApplication = typeDeclBody.getTypeApplication();
        DecodeEnumTypeDecl enumType = typeDeclBody.getEnumTypeDecl();
        DecodeStructTypeDecl structType = typeDeclBody.getStructTypeDecl();
        if (typeApplication != null)
        {
            return getProxyFor(typeApplication, namespace);
        }
        if (enumType != null)
        {
            return object(newEnumType(name, enumType, namespace));
        }
        if (structType != null)
        {
            return object(newStructType(name, structType, namespace));
        }
        throw new AssertionError();
    }

    @NotNull
    private DecodeType newType(@NotNull Option<DecodeName> name, @NotNull DecodeTypeDeclBody typeDeclBody,
                               @NotNull DecodeNamespace namespace)
    {
        DecodeTypeApplication typeApplication = typeDeclBody.getTypeApplication();
        DecodeEnumTypeDecl enumType = typeDeclBody.getEnumTypeDecl();
        DecodeStructTypeDecl structType = typeDeclBody.getStructTypeDecl();

        if (typeApplication != null)
        {
            return newSubTypeForTypeApplication(name, getText(typeDeclBody.getInfoString()), typeApplication, namespace);
        }
        if (enumType != null)
        {
            return newEnumType(name, enumType, namespace);
        }
        if (structType != null)
        {
            return newStructType(name, structType, namespace);
        }
        throw new AssertionError();
    }

    @NotNull
    private DecodeType newType(@NotNull DecodeTypeDecl typeDecl, @NotNull DecodeNamespace namespace)
    {
        return newType(
                Option.apply(DecodeNameImpl.newFromSourceName(typeDecl.getElementNameRule().getText())),
                typeDecl.getTypeDeclBody(), namespace);
    }

    @NotNull
    private DecodeStructType newStructType(
            @NotNull Option<DecodeName> name,
            @NotNull DecodeStructTypeDecl structTypeDecl,
            @NotNull DecodeNamespace namespace)
    {
        Buffer<DecodeStructField> fields = new ArrayBuffer<>();
        for (DecodeCommandArg fieldElement : structTypeDecl.getCommandArgs().getCommandArgList())
        {
            DecodeTypeUnitApplication typeUnitApplication = fieldElement.getTypeUnitApplication();
            DecodeUnit unit = typeUnitApplication.getUnit();
            fields.$plus$eq(ImmutableDecodeStructField.newInstance(DecodeNameImpl.newFromSourceName(
                            fieldElement.getElementNameRule().getText()),
                    makeProxyForTypeApplication(typeUnitApplication.getTypeApplication(), namespace),
                    asOption(Optional.ofNullable(unit).map(u -> proxy(namespace.fqn(),
                            DecodeNameImpl.newFromSourceName(u.getElementId().getText())))),
                    getText(fieldElement.getInfoString())));
        }
        return new DecodeStructTypeImpl(name, namespace, getText(structTypeDecl.getInfoString()), fields);
    }

    @NotNull
    private DecodeSubType newSubTypeForTypeApplication(@NotNull Option<DecodeName> name,
                                                      @NotNull Option<String> info,
                                                      @NotNull DecodeTypeApplication newTypeDeclBody,
                                                      @NotNull DecodeNamespace namespace)
    {
        DecodeArrayTypeApplication arrayTypeApplication = newTypeDeclBody.getArrayTypeApplication();
        DecodePrimitiveTypeApplication primitiveTypeApplication = newTypeDeclBody.getPrimitiveTypeApplication();
        DecodeGenericTypeApplication genericTypeApplication = newTypeDeclBody.getGenericTypeApplication();
        throw new AssertionError("not implemented: question mark & generic type");
        /*
        if (arrayTypeApplication != null)
        {
//            long minLength = Long.parseLong(arrayTypeApplication.getLengthFrom().getNonNegativeNumber().getText());
//            DecodeLengthTo lengthTo = arrayTypeApplication.getLengthTo();
//            long maxLength = lengthTo == null ? minLength : Long.parseLong(lengthTo.getNonNegativeNumber().getText());
//            ArraySize size = new ImmutableDecodeArrayType.ImmutableArraySize(minLength, maxLength);
            return SimpleDecodeSubType.newInstance(name, namespace, getProxyFor(arrayTypeApplication, namespace), info);
        }
        if (primitiveTypeApplication != null)
        {
            return SimpleDecodeSubType.newInstance(name, namespace,
                    makeProxyForPrimitiveType(primitiveTypeApplication), info);
        }
        if (genericTypeApplication != null)
        {
            return SimpleDecodeSubType.newInstance(name, namespace,
                    proxy(namespace.getFqn(), ImmutableDecodeName.newInstanceFromSourceName(
                            Preconditions.checkNotNull(newTypeDeclBody.getElementId()).getText())), info);
        }
        throw new AssertionError();*/
    }

    @NotNull
    private static DecodeMaybeProxy<DecodeType> makeProxyForPrimitiveType(
            @NotNull DecodePrimitiveTypeApplication primitiveTypeApplication)
    {
        throw new AssertionError();
    }

    @NotNull
    private static DecodeNamespace resolveNamespaceForTypeApplication(@NotNull DecodeTypeApplication typeApplication,
                                                                     @NotNull DecodeNamespace namespace)
    {
        DecodeArrayTypeApplication arrayTypeApplication = typeApplication.getArrayTypeApplication();
        DecodePrimitiveTypeApplication primitiveTypeApplication = typeApplication.getPrimitiveTypeApplication();
        DecodeGenericTypeApplication genericTypeApplication = typeApplication.getGenericTypeApplication();
        throw new AssertionError("not implemented: question mark & generic type");
        /*
        if (arrayTypeApplication != null)
        {
            return resolveNamespaceForTypeApplication(arrayTypeApplication.getTypeApplication(), namespace);
        }
        if (primitiveTypeApplication != null)
        {
            // TODO: Resolve namespace for primitive type
            return namespace;
        }
        if (genericTypeApplication != null)
        {
            // TODO: Resolve namespace for base type
            return namespace;
        }
        throw new AssertionError();*/
    }

    @NotNull
    private static DecodeType newEnumType(@NotNull Option<DecodeName> name, @NotNull DecodeEnumTypeDecl enumTypeDecl,
                                          @NotNull DecodeNamespace namespace)
    {
        Set<DecodeEnumConstant> values = enumTypeDecl.getEnumTypeValues().getEnumTypeValueList().stream()
                .map(child -> ImmutableDecodeEnumConstant.newInstance(
                        DecodeNameImpl.newFromSourceName(child.getElementNameRule().getText()),
                        child.getLiteral().getText(), getText(child.getInfoString()))).collect(Collectors.toSet());
        throw new AssertionError("not implemented");
        /*return SimpleDecodeEnumType.newInstance(name, namespace, proxy(namespace.getFqn(),
                        ImmutableDecodeName.newInstanceFromSourceName(enumTypeDecl.getElementId().getText())),
                getText(enumTypeDecl.getInfoString()), values);*/
    }

    @NotNull
    private Optional<DecodeType> newPrimitiveType(@NotNull Optional<DecodeName> name,
                                                 @NotNull DecodeNamespace namespace,
                                                 @NotNull DecodePrimitiveTypeApplication primitiveTypeApplication)
    {
        throw new AssertionError("not implemented: native type");
        /*
        String typeKindString = primitiveTypeApplication.getPrimitiveTypeKind().getText();
        DecodeType.TypeKind typeKind;
        switch (typeKindString)
        {
            case "uint":
                typeKind = DecodeType.TypeKind.UINT;
                break;
            case "int":
                typeKind = DecodeType.TypeKind.INT;
                break;
            case "float":
                typeKind = DecodeType.TypeKind.FLOAT;
                break;
            case "bool":
                typeKind = DecodeType.TypeKind.BOOL;
                break;
            default:
                error("Unsupported type kind '%s'", typeKindString);
                return Optional.empty();
        }
        return Optional.of(SimpleDecodePrimitiveType.newInstance(name, namespace,
                typeKind, Long.parseLong(primitiveTypeApplication.getNonNegativeNumber().getText()),
                Optional.<String>empty()));*/
    }

    @NotNull
    private static Option<String> getText(@Nullable DecodeInfoString infoString)
    {
        return getText(asOption(Optional.ofNullable(infoString)
                .map(DecodeInfoString::getStringValue)));
    }

    @NotNull
    private static Option<String> getText(@Nullable DecodeStringValue stringValue)
    {
        return getText(Option.apply(stringValue));
    }

    @NotNull
    private static Option<String> getText(@NotNull Option<DecodeStringValue> stringValue)
    {
        return asOption(asOptional(stringValue).map(str -> Optional.ofNullable(str.getString()).orElse(str.getStringUnaryQuotes())).map(
                PsiElement::getText).map(text -> text.substring(1, text.length() - 1)));
    }

    private void error(@NotNull String msg, Object... params)
    {
        result.getMessages().add(new DecodeTransformationMessage(ModelingMessage.Level.ERROR, msg, params));
    }

}
