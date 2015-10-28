package ru.mipt.acsl.decode.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.support.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.*;
import ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessage;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeEnumConstant;
import ru.mipt.acsl.decode.model.domain.type.DecodeStructField;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;

import java.util.*;
import java.util.stream.Collectors;

import static ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy.*;

/**
 * @author Artem Shein
 */
@BuildParseTree
public class DecodeParboiledParser extends BaseParser<Object>
{
    private static final Logger LOG = LoggerFactory.getLogger(DecodeParboiledParser.class);

    Rule W()
    {
        return OneOrMore(AnyOf(" \t"));
    }

    Rule OptW()
    {
        return Optional(W());
    }

    Rule EW()
    {
        return OneOrMore(AnyOf(" \t\r\n"));
    }

    Rule OptEW()
    {
        return Optional(EW());
    }

    Rule File()
    {
        Var<DecodeNamespace> namespaceVar = new Var<>();
        Var<String> infoVar = new Var<>();
        Var<Map<String, DecodeMaybeProxy<DecodeReferenceable>>> importsVar = new Var<>(new HashMap<>());
        return Sequence(OptInfoEw(infoVar), NamespaceAsNamespace(infoVar), namespaceVar.set((DecodeNamespace) pop()),
                ZeroOrMore(EW(), Import(importsVar)),
                ZeroOrMore(EW(),
                        infoVar.set(null), OptInfoEw(infoVar),
                        FirstOf(
                                Sequence(ComponentAsComponent(namespaceVar, infoVar),
                                        namespaceVar.get().getComponents().add((DecodeComponent) pop())),
                                Sequence(UnitDeclAsUnit(namespaceVar, infoVar),
                                        namespaceVar.get().getUnits().add((DecodeUnit) pop())),
                                Sequence(TypeDeclAsType(namespaceVar, infoVar),
                                        namespaceVar.get().getTypes().add((DecodeType) pop())),
                                Sequence(AliasAsAlias(namespaceVar, infoVar),
                                        namespaceVar.get().getTypes().add((DecodeType) pop())),
                                Sequence(LanguageAsLanguage(namespaceVar, infoVar),
                                        namespaceVar.get().getLanguages().add((DecodeLanguage) pop())))),
                OptEW(), EOI, push(namespaceVar.get().getRootNamespace()));
    }

    Rule Import(@NotNull Var<Map<String, DecodeMaybeProxy<DecodeReferenceable>>> importsVar)
    {
        Var<DecodeFqn> namespaceFqnVar = new Var<>();
        return Sequence("import", EW(), ElementIdAsFqn(), namespaceFqnVar.set((DecodeFqn) pop()),
                FirstOf(Sequence('.', OptEW(), '{', OptEW(), ImportPartAsImportPart(), addImport(importsVar.get(), namespaceFqnVar, (ImportPart) pop()),
                                ZeroOrMore(OptEW(), ',', OptEW(), ImportPartAsImportPart(), addImport(importsVar.get(), namespaceFqnVar, (ImportPart) pop())), Optional(OptEW(), ','), OptEW(), '}'),
                        addImport(importsVar.get(), (DecodeFqn) pop())));
    }

    static boolean addImport(@NotNull Map<String, DecodeMaybeProxy<DecodeReferenceable>> importMap,
                             @NotNull Var<DecodeFqn> namespaceFqnVar,
                             @NotNull ImportPart importPart)
    {
        Preconditions.checkState(!importMap.containsKey(importPart.getAlias()));
        importMap.put(importPart.getAlias(), SimpleDecodeMaybeProxy.proxy(namespaceFqnVar.get(), importPart.getOriginalName()));
        return true;
    }

    static boolean addImport(@NotNull Map<String, DecodeMaybeProxy<DecodeReferenceable>> importMap,
                             @NotNull DecodeFqn fqn)
    {
        Preconditions.checkState(!importMap.containsKey(fqn.getLast().asString()));
        importMap.put(fqn.getLast().asString(), SimpleDecodeMaybeProxy.proxy(fqn.copyDropLast(), fqn.getLast()));
        return true;
    }

    Rule ImportPartAsImportPart()
    {
        return Sequence(ElementNameAsName(), FirstOf(Sequence(OptEW(), "as", OptEW(), ElementNameAsName(), push(new ImportPartNameAlias((DecodeName) pop(1), (DecodeName) pop()))),
                push(new ImportPartName((DecodeName) pop()))));
    }

    Rule LanguageAsLanguage(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        Var<Boolean> defaultVar = new Var<>(false);
        return Sequence("language", EW(), ElementNameAsName(),
                Optional(EW(), "default", defaultVar.set(true)),
                push(SimpleDecodeLanguage.newInstance((DecodeName) pop(), namespaceVar.get(), defaultVar.get(),
                        Optional.ofNullable(infoVar.get()))));
    }

    Rule AliasAsAlias(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        return Sequence("alias", EW(), ElementNameAsName(), EW(), push(namespaceVar.get()), TypeApplicationAsProxyType(),
                push(SimpleDecodeAliasType.newInstance((DecodeName) pop(1), namespaceVar.get(),
                        (DecodeMaybeProxy<DecodeType>) pop(), Optional.ofNullable(infoVar.get()))));
    }

    Rule NamespaceAsNamespace(@NotNull Var<String> infoVar)
    {
        return Sequence("namespace", EW(), ElementIdAsFqn(), push(newNamespace((DecodeFqn) pop())));
    }

    @NotNull
    static DecodeNamespace newNamespace(@NotNull DecodeFqn fqn)
    {
        DecodeNamespace parentNamespace = null;
        if (fqn.size() > 1)
        {
            parentNamespace = DecodeUtils.newNamespaceForFqn(fqn.copyDropLast());
        }
        DecodeNamespace
                result = SimpleDecodeNamespace.newInstance(fqn.getLast(), Optional.ofNullable(parentNamespace));
        if (parentNamespace != null)
        {
            parentNamespace.getSubNamespaces().add(result);
        }
        return result;
    }

    Rule ElementNameAsName()
    {
        return Sequence(
                Sequence(Optional('^'), FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), '_'), ZeroOrMore(
                        FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), CharRange('0', '9'), '_'))),
                push(ImmutableDecodeName.newInstanceFromSourceName(match())));
    }

    Rule ComponentAsComponent(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        Var<DecodeType> typeVar = new Var<>();
        Var<Set<DecodeMaybeProxy<DecodeComponent>>> subComponentsVar = new Var<>(new HashSet<>());
        Var<List<DecodeCommand>> commandsVar = new Var<>(new ArrayList<>());
        Var<List<DecodeMessage>> messagesVar = new Var<>(new ArrayList<>());
        Var<DecodeName> baseTypeNameVar = new Var<>();
        Var<DecodeComponent> componentVar = new Var<>();
        Var<Integer> idVar = new Var<>();
        return Sequence("component", EW(), ElementNameAsName(),
                Optional(OptEW(), ':', NonNegativeNumberAsInteger(), idVar.set(((ImmutableDecodeElementWrapper<Integer>) pop()).getValue())),
                Optional(EW(), "with", OptEW(), Subcomponent(namespaceVar, subComponentsVar),
                        ZeroOrMore(OptEW(), ',', OptEW(), Subcomponent(namespaceVar, subComponentsVar))),
                OptEW(), '{',
                Optional(OptEW(), ComponentParametersAsType(namespaceVar,
                                baseTypeNameVar),
                        typeVar.set((DecodeType) pop()), namespaceVar.get().getTypes().add(typeVar.get())),
                componentVar.set(SimpleDecodeComponent
                        .newInstance((DecodeName) pop(), namespaceVar.get(), Optional.ofNullable(idVar.get()),
                                typeVar.isSet()? Optional.of(SimpleDecodeMaybeProxy.object(typeVar.get())) : Optional.<DecodeMaybeProxy<DecodeType>>empty(), Optional.ofNullable(infoVar.get()),
                                subComponentsVar.get(), commandsVar.get(), messagesVar.get())),
                ZeroOrMore(OptEW(),
                        FirstOf(Sequence(CommandAsCommand(namespaceVar), commandsVar.get().add((DecodeCommand) pop())),
                                Sequence(MessageAsMessage(componentVar), messagesVar.get().add((DecodeMessage) pop())))),
                OptEW(), '}',
                push(componentVar.get()));
    }

    Rule ComponentParametersAsType(@NotNull Var<DecodeNamespace> namespaceVar,
                                   @NotNull Var<DecodeName> nameVar)
    {
        Var<List<DecodeStructField>> fieldsVar = new Var<>(new ArrayList<>());
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar), "parameters", OptEW(),
                StructTypeFields(namespaceVar, fieldsVar), push(SimpleDecodeStructType
                        .newInstance(makeNewSystemName(nameVar.get()), namespaceVar.get(),
                                Optional.ofNullable(infoVar.get()), fieldsVar.get())));
    }

    @NotNull
    static Optional<DecodeName> makeNewSystemName(@Nullable DecodeName name)
    {
        return Optional.ofNullable(name).map(DecodeName::asString).map(n -> ImmutableDecodeName
                .newInstanceFromSourceName(n + "$"));
    }

    Rule CommandAsCommand(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<List<DecodeCommandArgument>> argsVar = new Var<>(new ArrayList<>());
        Var<String> infoVar = new Var<>();
        Var<Integer> idVar = new Var<>();
        Var<DecodeMaybeProxy<DecodeReferenceable>> returnTypeVar = new Var<>(SimpleDecodeMaybeProxy.proxyForSystem(ImmutableDecodeName.newInstanceFromMangledName("void")));
        return Sequence(OptInfoEw(infoVar), "command", EW(), ElementNameAsName(), OptEW(),
                Optional(':', OptEW(), NonNegativeNumberAsInteger(), idVar.set(((ImmutableDecodeElementWrapper<Integer>) pop()).getValue()), OptEW()),
                '(', Optional(OptEW(), CommandArgs(namespaceVar, argsVar)), OptEW(), ')',
                Optional(OptEW(), "->", OptEW(), push(namespaceVar.get()), TypeApplicationAsProxyType(), returnTypeVar.set(
                        (DecodeMaybeProxy<DecodeReferenceable>) pop())),
                push(ImmutableDecodeCommand.newInstance((DecodeName) pop(),
                        Optional.ofNullable(idVar.get()), Optional.ofNullable(infoVar.get()),
                        argsVar.get(), returnTypeVar.get())));
    }

    Rule CommandArgs(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<List<DecodeCommandArgument>> argsVar)
    {
        return Sequence(CommandArgAsCommandArg(namespaceVar), argsVar.get().add((DecodeCommandArgument) pop()),
                ZeroOrMore(OptEW(), ',', OptEW(), CommandArgAsCommandArg(namespaceVar), argsVar.get().add(
                        (DecodeCommandArgument) pop())), Optional(OptEW(), ','));
    }

    Rule CommandArgAsCommandArg(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<DecodeMaybeProxy<DecodeUnit>> unitVar = new Var<>();
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar), TypeUnitApplicationAsProxyType(namespaceVar, unitVar), EW(), ElementNameAsName(),
                push(ImmutableDecodeCommandArgument
                        .newInstance((DecodeName) pop(), (DecodeMaybeProxy<DecodeType>) pop(),
                                Optional.ofNullable(unitVar.get()),
                                Optional.ofNullable(infoVar.get()))));
    }

    Rule StructFieldAsStructField(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<DecodeMaybeProxy<DecodeUnit>> unitVar = new Var<>();
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar), TypeUnitApplicationAsProxyType(namespaceVar,
                        unitVar), EW(), ElementNameAsName(),
                push(ImmutableDecodeStructField
                        .newInstance((DecodeName) pop(), (DecodeMaybeProxy<DecodeType>) pop(),
                                Optional.ofNullable(unitVar.get()),
                                Optional.ofNullable(infoVar.get()))));
    }

    Rule TypeUnitApplicationAsProxyType(@NotNull Var<DecodeNamespace> namespaceVar,
                                        @NotNull Var<DecodeMaybeProxy<DecodeUnit>> unitVar)
    {
        return Sequence(push(namespaceVar.get()), TypeApplicationAsProxyType(), Optional(
                OptEW(), UnitAsFqn(), unitVar.set(
                        proxyDefaultNamespace((DecodeFqn) pop(), namespaceVar.get()))));
    }

    Rule TypeApplicationAsProxyType()
    {
        Var<List<DecodeMaybeProxy<DecodeReferenceable>>> genericTypesVar = new Var<>(new ArrayList<>());
        Var<DecodeNamespace> namespaceVar = new Var<>();
        return Sequence(
                // FIXME: Parboiled bug
                namespaceVar.set((DecodeNamespace) pop()),
                FirstOf(
                        PrimitiveTypeApplicationAsProxyType(),
                        NativeTypeApplicationAsProxyType(),
                        // FIXME: bug with Var in parboiled
                        Sequence(push(namespaceVar.get()),
                                ArrayTypeApplicationAsProxyType()),
                        Sequence(ElementIdAsFqn(), push(
                                proxyDefaultNamespace((DecodeFqn) pop(),
                                        namespaceVar.get())))),
                Optional(OptEW(), '<', push(namespaceVar.get()), TypeApplicationAsProxyType(), genericTypesVar.get().add((DecodeMaybeProxy<DecodeReferenceable>) pop()),
                        ZeroOrMore(OptEW(), ',', OptEW(), push(namespaceVar.get()), TypeApplicationAsProxyType(), genericTypesVar.get().add((DecodeMaybeProxy<DecodeReferenceable>) pop())),
                        Optional(OptEW(), ','), OptEW(), '>', push(proxyForSystemTypeString(
                                ((DecodeMaybeProxy<DecodeReferenceable>) pop()).getProxy().getUri().toString() + "<" +
                                        genericTypesListToString(genericTypesVar.get()) + ">"))),
                Optional(OptEW(), '?',
                        push(proxyForSystem(ImmutableDecodeName.newInstanceFromSourceName("optional<"
                                + ((DecodeMaybeProxy<DecodeReferenceable>) pop()).getProxy().getUri().toString()
                                + ">")))));
    }

    @NotNull
    static String genericTypesListToString(@NotNull List<DecodeMaybeProxy<DecodeReferenceable>> genericTypesVar)
    {
        return genericTypesVar.stream().map(DecodeMaybeProxy::getProxy)
                .map(DecodeProxy::getUri).map(Object::toString).collect(
                Collectors.joining(","));
    }

    Rule UnitAsFqn()
    {
        return Sequence('/', ElementIdAsFqn(), '/');
    }

    Rule NativeTypeApplicationAsProxyType()
    {
        return Sequence("ber",
                push(proxyForSystem(ImmutableDecodeName.newInstanceFromSourceName(match()))));
    }

    Rule PrimitiveTypeApplicationAsProxyType()
    {
        return Sequence(Sequence(PrimitiveTypeKind(), ':', NonNegativeNumber()),
                push(proxyForSystem(ImmutableDecodeName.newInstanceFromSourceName(match()))));
    }

    // FIXME: uses DecodeNamespace ref from stack, should use Var, parboiled bug
    Rule ArrayTypeApplicationAsProxyType()
    {
        Var<DecodeNamespace> namespaceVar = new Var<>();
        Var<DecodeMaybeProxy<DecodeType>> typeApplicationVar = new Var<>();
        return Sequence(
                Sequence(namespaceVar.set((DecodeNamespace) pop()), '[', OptEW(),
                        push(namespaceVar.get()),
                        TypeApplicationAsProxyType(), typeApplicationVar.set((DecodeMaybeProxy<DecodeType>) pop()),
                        Optional(OptEW(), ',', OptEW(),
                                Sequence(LengthFrom(), Optional(OptEW(), "..", OptEW(), LengthTo()))),
                        OptEW(), ']'),
                push(proxy(DecodeUtils.getNamespaceFqnFromUri(typeApplicationVar.get().getProxy().getUri()), ImmutableDecodeName
                        .newInstanceFromSourceName(match()))));
    }

    Rule StructTypeFields(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<List<DecodeStructField>> fieldsVar)
    {
        return Sequence('(', OptEW(),
                StructFieldAsStructField(namespaceVar), fieldsVar.get().add((DecodeStructField) pop()),
                ZeroOrMore(OptEW(), ',', OptEW(), StructFieldAsStructField(namespaceVar),
                        fieldsVar.get().add((DecodeStructField) pop())),
                Optional(OptEW(), ','), OptEW(), ')');
    }

    Rule StructTypeDeclAsStructType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeName> nameVar)
    {
        Var<String> infoVar = new Var<>();
        Var<List<DecodeStructField>> fieldsVar = new Var<>(new ArrayList<>());
        return Sequence("struct", OptEW(), StructTypeFields(namespaceVar, fieldsVar),
                push(SimpleDecodeStructType
                        .newInstance(Optional.ofNullable(nameVar.get()), namespaceVar.get(),
                                Optional.ofNullable(infoVar.get()), fieldsVar.get())));
    }

    Rule ElementIdAsFqn()
    {
        Var<List<DecodeName>> names = new Var<>();
        return Sequence(ElementNameAsName(), names.set(Lists.newArrayList((DecodeName) pop())),
                ZeroOrMore('.', ElementNameAsName(), names.get().add((DecodeName) pop())),
                push(ImmutableDecodeFqn.newInstance(names.get())));
    }

    Rule MessageAsMessage(@NotNull Var<DecodeComponent> componentVar)
    {
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar),
                FirstOf(StatusMessageAsMessage(componentVar, infoVar),
                        EventMessageAsMessage(componentVar, infoVar)));
    }

    Rule MessageNameId(@NotNull Var<DecodeName> nameVar, @NotNull Var<Integer> idVar)
    {
        return Sequence(ElementNameAsName(), nameVar.set((DecodeName) pop()),
            Optional(OptEW(), ':', OptEW(),
                    NonNegativeNumberAsInteger(), idVar.set(((ImmutableDecodeElementWrapper<Integer>) pop()).getValue())));
    }

    Rule StatusMessageAsMessage(@NotNull Var<DecodeComponent> componentVar, @NotNull Var<String> infoVar)
    {
        Var<List<DecodeMessageParameter>> parametersVar = new Var<>(new ArrayList<>());
        Var<DecodeName> nameVar = new Var<>();
        Var<Integer> idVar = new Var<>();
        return Sequence("status", EW(), MessageNameId(nameVar, idVar), OptEW(), MessageParameters(parametersVar),
                push(ImmutableDecodeStatusMessage.newInstance(componentVar.get(), nameVar.get(), Optional.ofNullable(idVar.get()),
                        Optional.ofNullable(infoVar.get()), parametersVar.get())));
    }

    Rule MessageParameters(@NotNull Var<List<DecodeMessageParameter>> parametersVar)
    {
        return Sequence('(', OptEW(), ParameterAsParameter(), parametersVar.get().add((DecodeMessageParameter) pop()),
                        ZeroOrMore(OptEW(), ',', OptEW(), ParameterAsParameter(), parametersVar.get().add(
                                (DecodeMessageParameter) pop())),
                        Optional(OptEW(), ','), OptEW(), ')');
    }

    Rule EventMessageAsMessage(@NotNull Var<DecodeComponent> componentVar, @NotNull Var<String> infoVar)
    {
        @NotNull Var<DecodeName> nameVar = new Var<>();
        @NotNull Var<Integer> idVar = new Var<>();
        Var<List<DecodeMessageParameter>> parametersVar = new Var<>(new ArrayList<>());
        return Sequence("event", EW(), MessageNameId(nameVar, idVar), EW(), push(componentVar.get().getNamespace()), TypeApplicationAsProxyType(), OptEW(), MessageParameters(parametersVar),
                push(ImmutableDecodeEventMessage.newInstance(componentVar.get(), nameVar.get(), Optional.ofNullable(idVar.get()),
                        Optional.ofNullable(infoVar.get()), parametersVar.get(), (DecodeMaybeProxy<DecodeType>) pop())));
    }

    Rule ParameterAsParameter()
    {
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar), ParameterElement(), push(ImmutableDecodeElementWrapper.newInstance(match())),
                        push(ImmutableDecodeMessageParameter.newInstance(
                                ((ImmutableDecodeElementWrapper<String>) pop()).getValue())));
    }

    Rule ParameterElement()
    {
        return Sequence(ElementIdAsFqn(), drop(),
                Optional(OneOrMore('[', OptEW(), NonNegativeNumber(), OptEW(),
                                Optional("..", OptEW(), NonNegativeNumber(), OptEW()), ']'),
                        ZeroOrMore('.', ElementIdAsFqn(), drop(),
                                Optional('[', OptEW(), NonNegativeNumber(), OptEW(),
                                        Optional("..", OptEW(), NonNegativeNumber(), OptEW()), ']'))));
    }

    Rule OptInfoEw(@NotNull Var<String> infoVar)
    {
        return Optional(StringValueAsString(),
                infoVar.set(((ImmutableDecodeElementWrapper<String>) pop()).getValue()), EW());
    }

    Rule StringValueAsString()
    {
        return FirstOf(Sequence('"', ZeroOrMore(FirstOf(NoneOf("\"\\"), Sequence('\\', ANY))),
                        push(ImmutableDecodeElementWrapper.newInstance(match())),
                        FirstOf('"', Sequence(drop(), NOTHING))),
                Sequence('\'', ZeroOrMore(FirstOf(NoneOf("\'\\"), Sequence('\\', ANY))),
                        push(ImmutableDecodeElementWrapper.newInstance(match())),
                        FirstOf('\'', Sequence(drop(), NOTHING))));
    }

    Rule Subcomponent(@NotNull Var<DecodeNamespace> namespaceVar,
                      Var<Set<DecodeMaybeProxy<DecodeComponent>>> subComponentsVar)
    {
        return Sequence(ElementIdAsFqn(), subComponentsVar.get().add(proxyDefaultNamespace(
                (DecodeFqn) pop(), namespaceVar.get())));
    }

    Rule UnitDeclAsUnit(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        Var<String> displayVar = new Var<>();
        return Sequence("unit", EW(), ElementNameAsName(), Optional(EW(), "display", EW(), StringValueAsString(),
                        displayVar.set(((ImmutableDecodeElementWrapper<String>) pop()).getValue())),
                Optional(EW(), "placement", EW(), FirstOf("before", "after")),
                push(SimpleDecodeUnit.newInstance((DecodeName) pop(), namespaceVar.get(),
                        displayVar.get(), infoVar.get())));
    }

    Rule TypeDeclAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        Var<DecodeName> nameVar = new Var<>();
        return Sequence("type", EW(), ElementNameAsName(), nameVar.set((DecodeName) pop()), EW(),
                TypeDeclBodyAsType(namespaceVar, nameVar, infoVar));
    }

    Rule PrimitiveTypeKind()
    {
        return FirstOf("uint", "int", "float", "bool");
    }

    Rule LengthFrom()
    {
        return NonNegativeNumber();
    }

    Rule LengthTo()
    {
        return FirstOf(NonNegativeNumber(), '*');
    }

    Rule TypeDeclBodyAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeName> nameVar, @NotNull Var<String> infoVar)
    {
        return FirstOf(
                EnumTypeDeclAsType(namespaceVar, nameVar),
                StructTypeDeclAsStructType(namespaceVar, nameVar),
                Sequence(push(namespaceVar.get()), TypeApplicationAsProxyType(),
                        push(SimpleDecodeSubType
                                .newInstance(Optional.ofNullable(nameVar.get()), namespaceVar.get(),
                                        (DecodeMaybeProxy<DecodeType>) pop(), Optional.ofNullable(
                                                infoVar.get())))));
    }

    Rule EnumTypeDeclAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeName> nameVar)
    {
        Var<String> infoVar = new Var<>();
        Var<Set<DecodeEnumConstant>> enumConstantsVar = new Var<>(new HashSet<>());
        return Sequence("enum", EW(), ElementIdAsFqn(), OptEW(), '(', OptEW(),
                EnumTypeValues(enumConstantsVar), OptEW(), ')',
                push(SimpleDecodeEnumType.newInstance(Optional.ofNullable(nameVar.get()), namespaceVar.get(),
                        proxyDefaultNamespace((DecodeFqn) pop(), namespaceVar.get()),
                        Optional.ofNullable(infoVar.get()), enumConstantsVar.get())));
    }

    Rule EnumTypeValues(@NotNull Var<Set<DecodeEnumConstant>> enumConstantsVar)
    {
        return Sequence(EnumTypeValue(enumConstantsVar),
                ZeroOrMore(OptEW(), ',', OptEW(), EnumTypeValue(enumConstantsVar)), Optional(OptEW(), ','));
    }

    Rule EnumTypeValue(@NotNull Var<Set<DecodeEnumConstant>> enumConstantsVar)
    {
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar), ElementNameAsName(), OptEW(), '=', OptEW(), LiteralAsString(),
                enumConstantsVar.get().add(ImmutableDecodeEnumConstant.newInstanceWrapper((DecodeName) pop(1),
                        (ImmutableDecodeElementWrapper<String>) pop(), Optional.ofNullable(infoVar.get()))));
    }

    Rule LiteralAsString()
    {
        return Sequence(FirstOf(FloatLiteral(), NonNegativeNumber(), "true", "false"),
                push(ImmutableDecodeElementWrapper.newInstance(match())));
    }

    Rule FloatLiteral()
    {
        return Sequence(Optional(FirstOf('+', '-')),
                FirstOf(Sequence(NonNegativeNumber(), '.', Optional(NonNegativeNumber())),
                        Sequence('.', NonNegativeNumber())),
                Optional(AnyOf("eE"), Optional(AnyOf("+-")), NonNegativeNumber()));
    }

    Rule NonNegativeNumberAsInteger()
    {
        return Sequence(NonNegativeNumber(), push(ImmutableDecodeElementWrapper.newInstance(Integer.parseInt(match()))));
    }

    Rule NonNegativeNumber()
    {
        return OneOrMore(CharRange('0', '9'));
    }

    private interface ImportPart
    {
        @NotNull
        String getAlias();
        @NotNull
        DecodeName getOriginalName();
    }

    static class ImportPartName implements ImportPart
    {
        @NotNull
        private final DecodeName name;

        public ImportPartName(@NotNull DecodeName name)
        {
            this.name = name;
        }

        @NotNull
        @Override
        public String getAlias()
        {
            return name.asString();
        }

        @NotNull
        @Override
        public DecodeName getOriginalName()
        {
            return name;
        }
    }

    static class ImportPartNameAlias implements ImportPart
    {
        @NotNull
        private final DecodeName name;
        @NotNull
        private final DecodeName alias;

        public ImportPartNameAlias(@NotNull DecodeName name, @NotNull DecodeName alias)
        {
            this.name = name;
            this.alias = alias;
        }

        @NotNull
        @Override
        public String getAlias()
        {
            return alias.asString();
        }

        @NotNull
        @Override
        public DecodeName getOriginalName()
        {
            return name;
        }
    }
}
