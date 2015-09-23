package ru.mipt.acsl.decode.parser;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
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

import static ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy.proxy;
import static ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy.proxyDefaultNamespace;
import static ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy.proxyForSystem;

/**
 * @author Artem Shein
 */
@BuildParseTree
public class DecodeParboiledParser extends BaseParser<DecodeElement>
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
        return Sequence(NamespaceAsNamespace(), namespaceVar.set((DecodeNamespace) pop()), ZeroOrMore(EW(),
                        FirstOf(
                                Sequence(ComponentAsComponent(namespaceVar),
                                        namespaceVar.get().getComponents().add((DecodeComponent) pop())),
                                Sequence(UnitDeclAsUnit(namespaceVar),
                                        namespaceVar.get().getUnits().add((DecodeUnit) pop())),
                                Sequence(TypeDeclAsType(namespaceVar),
                                        namespaceVar.get().getTypes().add((DecodeType) pop())),
                                Sequence(AliasAsAlias(namespaceVar),
                                        namespaceVar.get().getTypes().add((DecodeType) pop())))),
                EOI, push(namespaceVar.get().getRootNamespace()));
    }

    Rule AliasAsAlias(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<String> infoVar = new Var<>();
        return Sequence("alias", EW(), ElementNameAsName(), EW(), TypeApplicationAsProxyType(namespaceVar),
                OptEwInfoString(infoVar),
                push(SimpleDecodeAliasType.newInstance((DecodeName) pop(1), namespaceVar.get(),
                        (DecodeMaybeProxy<DecodeType>) pop(), Optional.ofNullable(infoVar.get()))));
    }

    Rule NamespaceAsNamespace()
    {
        Var<DecodeFqn> fqn = new Var<>();
        return Sequence("namespace", EW(), ElementIdAsFqn(), fqn.set((DecodeFqn) pop()), push(newNamespace(fqn)));
    }

    @NotNull
    DecodeNamespace newNamespace(@NotNull Var<DecodeFqn> fqnVar)
    {
        DecodeNamespace parentNamespace = null;
        if (fqnVar.get().size() > 1)
        {
            parentNamespace = DecodeUtils.newNamespaceForFqn(fqnVar.get().copyDropLast());
        }
        DecodeNamespace
                result = SimpleDecodeNamespace.newInstance(fqnVar.get().getLast(), Optional.ofNullable(parentNamespace));
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

    Rule ComponentAsComponent(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<DecodeMaybeProxy<DecodeType>> typeVar = new Var<>();
        Var<String> infoVar = new Var<>();
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
                OptEwInfoString(infoVar), OptEW(), '{',
                Optional(OptEW(), ComponentBaseTypeAsType(namespaceVar,
                                baseTypeNameVar),
                        typeVar.set(SimpleDecodeMaybeProxy.object((DecodeType) pop()))),
                componentVar.set(SimpleDecodeComponent
                        .newInstance((DecodeName) pop(), namespaceVar.get(), Optional.ofNullable(idVar.get()),
                                Optional.ofNullable(typeVar.get()), Optional.ofNullable(infoVar.get()),
                                subComponentsVar.get(), commandsVar.get(), messagesVar.get())),
                ZeroOrMore(OptEW(),
                        FirstOf(Sequence(CommandAsCommand(namespaceVar), commandsVar.get().add((DecodeCommand) pop())),
                                Sequence(MessageAsMessage(componentVar), messagesVar.get().add((DecodeMessage) pop())))),
                OptEW(), '}',
                push(componentVar.get()));
    }

    Rule ComponentBaseTypeAsType(@NotNull Var<DecodeNamespace> namespaceVar,
                                 @NotNull Var<DecodeName> nameVar)
    {
        return Sequence(TestNot(Sequence(FirstOf("command", "message"), EW())),
                TypeDeclBodyAsType(namespaceVar, nameVar));
    }

    Rule CommandAsCommand(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<List<DecodeCommandArgument>> argsVar = new Var<>(new ArrayList<>());
        Var<String> infoVar = new Var<>();
        return Sequence("command", EW(), ElementNameAsName(), OptEW(), ':', OptEW(),
                NonNegativeNumberAsInteger(), OptEwInfoString(infoVar),
                OptEW(), '(', Optional(OptEW(), CommandArgs(namespaceVar, argsVar)), OptEW(), ')',
                push(ImmutableDecodeCommand.newInstance((DecodeName) pop(1),
                        ((ImmutableDecodeElementWrapper<Integer>) pop()).getValue(), Optional.ofNullable(infoVar.get()),
                        argsVar.get())));
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
        return Sequence(TypeUnitApplicationAsProxyType(namespaceVar, unitVar), EW(), ElementNameAsName(),
                OptEwInfoString(
                        infoVar),
                push(ImmutableDecodeCommandArgument
                        .newInstance((DecodeName) pop(), (DecodeMaybeProxy<DecodeType>) pop(),
                                Optional.ofNullable(unitVar.get()),
                                Optional.ofNullable(infoVar.get()))));
    }

    Rule StructFieldAsStructField(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<DecodeMaybeProxy<DecodeUnit>> unitVar = new Var<>();
        Var<String> infoVar = new Var<>();
        return Sequence(TypeUnitApplicationAsProxyType(namespaceVar,
                        unitVar), EW(), ElementNameAsName(), OptEwInfoString(
                        infoVar),
                push(ImmutableDecodeStructField
                        .newInstance((DecodeName) pop(), (DecodeMaybeProxy<DecodeType>) pop(),
                                Optional.ofNullable(unitVar.get()),
                                Optional.ofNullable(infoVar.get()))));
    }

    Rule TypeUnitApplicationAsProxyType(@NotNull Var<DecodeNamespace> namespaceVar,
                                        @NotNull Var<DecodeMaybeProxy<DecodeUnit>> unitVar)
    {
        return Sequence(TypeApplicationAsProxyType(namespaceVar), Optional(
                OptEW(), UnitAsFqn(), unitVar.set(
                        proxyDefaultNamespace((DecodeFqn) pop(), namespaceVar.get()))));
    }

    Rule TypeApplicationAsProxyType(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        return
                FirstOf(PrimitiveTypeApplicationAsProxyType(),
                        NativeTypeApplicationAsProxyType(),
                        // FIXME: bug with Var in parboiled
                        Sequence(push(namespaceVar.get()),
                                ArrayTypeApplicationAsProxyType()),
                        Sequence(ElementIdAsFqn(), push(
                                proxyDefaultNamespace((DecodeFqn) pop(),
                                        namespaceVar.get()))));
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
                        TypeApplicationAsProxyType(namespaceVar), typeApplicationVar.set((DecodeMaybeProxy<DecodeType>) pop()),
                        Optional(OptEW(), ',', OptEW(),
                                Sequence(LengthFrom(), Optional(OptEW(), "..", OptEW(), LengthTo()))),
                        OptEW(), ']'),
                push(proxy(DecodeUtils.getNamespaceFqnFromUri(typeApplicationVar.get().getProxy().getUri()), ImmutableDecodeName
                        .newInstanceFromSourceName(match()))));
    }

    Rule StructTypeDeclAsStructType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeName> nameVar)
    {
        Var<String> infoVar = new Var<>();
        Var<List<DecodeStructField>> fieldsVar = new Var<>(new ArrayList<>());
        return Sequence("struct", OptEwInfoString(infoVar), OptEW(), '(', OptEW(),
                StructFieldAsStructField(namespaceVar), fieldsVar.get().add((DecodeStructField) pop()),
                ZeroOrMore(OptEW(), ',', OptEW(), StructFieldAsStructField(namespaceVar),
                        fieldsVar.get().add((DecodeStructField) pop())),
                Optional(OptEW(), ','), OptEW(), ')',
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
        Var<DecodeName> nameVar = new Var<>();
        Var<Integer> idVar = new Var<>();
        return Sequence("message", EW(), ElementNameAsName(), nameVar.set((DecodeName) pop()), OptEW(), ':', OptEW(),
                NonNegativeNumberAsInteger(), idVar.set(((ImmutableDecodeElementWrapper<Integer>) pop()).getValue()),
                OptEwInfoString(infoVar), OptEW(),
                FirstOf(StatusMessageAsMessage(componentVar, nameVar, idVar, infoVar),
                        EventMessageAsMessage(componentVar, nameVar, idVar, infoVar),
                        DynamicStatusMessageAsMessage(componentVar, nameVar, idVar, infoVar)));
    }

    Rule StatusMessageAsMessage(@NotNull Var<DecodeComponent> componentVar, @NotNull Var<DecodeName> nameVar,
                                @NotNull Var<Integer> idVar, @NotNull Var<String> infoVar)
    {
        Var<List<DecodeMessageParameter>> parametersVar = new Var<>(new ArrayList<>());
        return Sequence("status", OptEW(), MessageParameters(parametersVar),
                push(ImmutableDecodeStatusMessage.newInstance(componentVar.get(), nameVar.get(), idVar.get(),
                        Optional.ofNullable(infoVar.get()), parametersVar.get())));
    }

    Rule MessageParameters(@NotNull Var<List<DecodeMessageParameter>> parametersVar)
    {
        return //FirstOf(
                //Sequence(DeepAllParameters(), parametersVar.get().add(ImmutableDecodeDeepAllParameters.INSTANCE)),
                //Sequence(AllParameters(), parametersVar.get().add(ImmutableDecodeAllParameters.INSTANCE)),
                Sequence('(', OptEW(), ParameterAsParameter(), parametersVar.get().add((DecodeMessageParameter) pop()),
                        ZeroOrMore(OptEW(), ',', OptEW(), ParameterAsParameter(), parametersVar.get().add(
                                (DecodeMessageParameter) pop())),
                        Optional(OptEW(), ','), OptEW(), ')');//);
    }

    /*Rule DeepAllParameters()
    {
        return String("*.*");
    }*/

    /*Rule AllParameters()
    {
        return Ch('*');
    }*/

    Rule EventMessageAsMessage(@NotNull Var<DecodeComponent> componentVar, @NotNull Var<DecodeName> nameVar,
                               @NotNull Var<Integer> idVar, @NotNull Var<String> infoVar)
    {
        Var<List<DecodeMessageParameter>> parametersVar = new Var<>(new ArrayList<>());
        return Sequence("event", EW(), MessageParameters(parametersVar),
                push(ImmutableDecodeEventMessage.newInstance(componentVar.get(), nameVar.get(), idVar.get(),
                        Optional.ofNullable(infoVar.get()), parametersVar.get())));
    }

    Rule DynamicStatusMessageAsMessage(@NotNull Var<DecodeComponent> componentVar, @NotNull Var<DecodeName> nameVar,
                                       @NotNull Var<Integer> idVar, @NotNull Var<String> infoVar)
    {
        Var<List<DecodeMessageParameter>> parametersVar = new Var<>(new ArrayList<>());
        return Sequence("dynamic", EW(), "status", EW(), MessageParameters(parametersVar),
                push(ImmutableDecodeDynamicStatusMessage.newInstance(componentVar.get(), nameVar.get(), idVar.get(),
                        Optional.ofNullable(infoVar.get()), parametersVar.get())));
    }

    Rule ParameterAsParameter()
    {
        Var<String> infoVar = new Var<>();
        return //FirstOf(Sequence(AllParameters(), push(ImmutableDecodeAllParameters.INSTANCE)),
                Sequence(ParameterElement(), push(ImmutableDecodeElementWrapper.newInstance(match())),
                        OptEwInfoString(infoVar), push(ImmutableDecodeMessageParameter.newInstance(
                                ((ImmutableDecodeElementWrapper<String>) pop()).getValue())));//);
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

    Rule InfoStringAsString()
    {
        return Sequence("info", EW(), StringValueAsString());
    }

    Rule OptEwInfoString(@NotNull Var<String> infoVar)
    {
        return Optional(EW(), InfoStringAsString(),
                infoVar.set(((ImmutableDecodeElementWrapper<String>) pop()).getValue()));
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

    Rule UnitDeclAsUnit(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<String> infoVar = new Var<>();
        Var<String> displayVar = new Var<>();
        return Sequence("unit", EW(), ElementNameAsName(), Optional(EW(), "display", EW(), StringValueAsString(),
                        displayVar.set(((ImmutableDecodeElementWrapper<String>) pop()).getValue())),
                Optional(EW(), "placement", EW(), FirstOf("before", "after")), OptEwInfoString(infoVar),
                push(SimpleDecodeUnit.newInstance((DecodeName) pop(), namespaceVar.get(),
                        displayVar.get(), infoVar.get())));
    }

    Rule TypeDeclAsType(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<DecodeName> nameVar = new Var<>();
        return Sequence("type", EW(), ElementNameAsName(), nameVar.set((DecodeName) pop()), EW(),
                TypeDeclBodyAsType(namespaceVar, nameVar));
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

    Rule TypeDeclBodyAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeName> nameVar)
    {
        Var<String> infoVar = new Var<>();
        return FirstOf(
                EnumTypeDeclAsType(namespaceVar, nameVar),
                StructTypeDeclAsStructType(namespaceVar, nameVar),
                Sequence(TypeApplicationAsProxyType(namespaceVar), OptEwInfoString(infoVar),

                        push(SimpleDecodeSubType
                                .newInstance(Optional.ofNullable(nameVar.get()), namespaceVar.get(),
                                        (DecodeMaybeProxy<DecodeType>) pop(), Optional.ofNullable(
                                                infoVar.get())))));
    }

    Rule EnumTypeDeclAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeName> nameVar)
    {
        Var<String> infoVar = new Var<>();
        Var<Set<DecodeEnumConstant>> enumConstantsVar = new Var<>(new HashSet<>());
        return Sequence("enum", EW(), ElementIdAsFqn(), OptEwInfoString(infoVar), OptEW(), '(', OptEW(),
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
        return Sequence(ElementNameAsName(), OptEW(), '=', OptEW(), LiteralAsString(), OptEwInfoString(infoVar),
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
}
