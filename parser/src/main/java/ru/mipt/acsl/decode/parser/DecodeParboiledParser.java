package ru.mipt.acsl.decode.parser;

import com.google.common.base.Charsets;
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
import ru.mipt.acsl.decode.model.domain.impl.type.*;
import ru.mipt.acsl.parsing.ParsingException;
import scala.Int;
import scala.Option;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Buffer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
    private Map<String, DecodeMaybeProxy<DecodeReferenceable>> imports;

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
        return Sequence(ACTION(resetImports()), OptInfoEw(infoVar), NamespaceAsNamespace(infoVar),
                namespaceVar.set((DecodeNamespace) pop()),
                ZeroOrMore(EW(), Import()),
                ZeroOrMore(EW(),
                        infoVar.set(null), OptInfoEw(infoVar),
                        FirstOf(
                                Sequence(ComponentAsComponent(namespaceVar, infoVar),
                                        namespaceVar.get().components().$plus$eq((DecodeComponent) pop())),
                                Sequence(UnitDeclAsUnit(namespaceVar, infoVar),
                                        namespaceVar.get().units().$plus$eq((DecodeUnit) pop())),
                                Sequence(TypeDeclAsType(namespaceVar, infoVar),
                                        namespaceVar.get().types().$plus$eq((DecodeType) pop())),
                                Sequence(AliasAsAlias(namespaceVar, infoVar),
                                        namespaceVar.get().types().$plus$eq((DecodeType) pop())),
                                Sequence(LanguageAsLanguage(namespaceVar, infoVar),
                                        namespaceVar.get().languages().$plus$eq((DecodeLanguage) pop())))),
                OptEW(), EOI, push(namespaceVar.get().rootNamespace()));
    }

    boolean resetImports()
    {
        imports = new HashMap<>();
        return true;
    }

    Rule Import()
    {
        Var<DecodeFqn> namespaceFqnVar = new Var<>();
        return Sequence("import", EW(), ElementIdAsFqn(), namespaceFqnVar.set((DecodeFqn) pop()),
                FirstOf(Sequence('.', OptEW(), '{', OptEW(), ImportPartAsImportPart(),
                                addImport(namespaceFqnVar, (ImportPart) pop()),
                                ZeroOrMore(OptEW(), ',', OptEW(), ImportPartAsImportPart(),
                                        addImport(namespaceFqnVar, (ImportPart) pop())),
                                Optional(OptEW(), ','), OptEW(), '}'),
                        addImport(namespaceFqnVar.get())));
    }

    boolean addImport(@NotNull Var<DecodeFqn> namespaceFqnVar, @NotNull ImportPart importPart)
    {
        Preconditions.checkState(!imports.containsKey(importPart.getAlias()));
        imports.put(importPart.getAlias(), SimpleDecodeMaybeProxy.proxy(namespaceFqnVar.get(), importPart.getOriginalName()));
        return true;
    }

    boolean addImport(@NotNull DecodeFqn fqn)
    {
        Preconditions.checkState(!imports.containsKey(fqn.last().asString()));
        imports.put(fqn.last().asString(), SimpleDecodeMaybeProxy.proxy(fqn.copyDropLast(), fqn.last()));
        return true;
    }

    Rule ImportPartAsImportPart()
    {
        return Sequence(ElementNameAsName(), FirstOf(Sequence(OptEW(), "as", OptEW(), ElementNameAsName(), push(new ImportPartNameAlias((DecodeNameImpl) pop(1), (DecodeNameImpl) pop()))),
                push(new ImportPartName((DecodeNameImpl) pop()))));
    }

    Rule LanguageAsLanguage(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        Var<Boolean> defaultVar = new Var<>(false);
        return Sequence("language", EW(), ElementNameAsName(),
                Optional(EW(), "default", defaultVar.set(true)),
                push(SimpleDecodeLanguage.newInstance((DecodeNameImpl) pop(), namespaceVar.get(), defaultVar.get(),
                        Option.apply(infoVar.get()))));
    }

    Rule AliasAsAlias(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        return Sequence("alias", EW(), ElementNameAsName(), EW(), push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                push(new DecodeAliasTypeImpl((DecodeNameImpl) pop(1), namespaceVar.get(),
                        getOrThrow((Optional<DecodeMaybeProxy<DecodeType>>) pop()), Option.apply(infoVar.get()))));
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
                result = DecodeNamespaceImpl.apply(fqn.last(), Option.apply(parentNamespace));
        if (parentNamespace != null)
        {
            parentNamespace.subNamespaces().$plus$eq(result);
        }
        return result;
    }

    Rule ElementNameAsName()
    {
        return Sequence(
                Sequence(Optional('^'), FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), '_'), ZeroOrMore(
                        FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), CharRange('0', '9'), '_'))),
                push(DecodeNameImpl.newFromSourceName(match())));
    }

    Rule ComponentAsComponent(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        Var<DecodeStructType> typeVar = new Var<>();
        Var<Buffer<DecodeComponentRef>> subComponentsVar = new Var<>(new ArrayBuffer<>());
        Var<Buffer<DecodeCommand>> commandsVar = new Var<>(new ArrayBuffer<>());
        Var<Buffer<DecodeMessage>> messagesVar = new Var<>(new ArrayBuffer<>());
        Var<DecodeName> baseTypeNameVar = new Var<>();
        Var<DecodeComponent> componentVar = new Var<>();
        Var<Int> idVar = new Var<>();
        return Sequence("component", EW(), ElementNameAsName(), baseTypeNameVar.set((DecodeNameImpl) peek()),
                Optional(OptEW(), ':', NonNegativeNumberAsInteger(), idVar.set((Int) pop())),
                Optional(EW(), "with", OptEW(), Subcomponent(namespaceVar, subComponentsVar),
                        ZeroOrMore(OptEW(), ',', OptEW(), Subcomponent(namespaceVar, subComponentsVar))),
                OptEW(), '{',
                Optional(OptEW(), ComponentParametersAsType(namespaceVar, baseTypeNameVar),
                        typeVar.set((DecodeStructType) pop()), namespaceVar.get().types().$plus$eq(typeVar.get())),
                componentVar.set(new DecodeComponentImpl((DecodeNameImpl) pop(), namespaceVar.get(), Option.apply(idVar.get()),
                                typeVar.isSet()
                                        ? Option.apply(SimpleDecodeMaybeProxy.object(typeVar.get()))
                                        : Option.<DecodeMaybeProxy<DecodeStructType>>empty(), Option.apply(infoVar.get()),
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
                StructTypeFields(namespaceVar, fieldsVar), push(new DecodeStructTypeImpl(makeNewSystemName(nameVar.get()), namespaceVar.get(),
                                Option.apply(infoVar.get()), fieldsVar.get())));
    }

    @NotNull
    static Optional<DecodeName> makeNewSystemName(@Nullable DecodeName name)
    {
        return Optional.ofNullable(name).map(DecodeName::asString).map(n -> DecodeNameImpl
                .newFromSourceName(n + "$"));
    }

    Rule CommandAsCommand(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<List<DecodeCommandArgument>> argsVar = new Var<>(new ArrayList<>());
        Var<String> infoVar = new Var<>();
        Var<Integer> idVar = new Var<>();
        Var<Optional<DecodeMaybeProxy<DecodeType>>> returnTypeVar = new Var<>(Optional.empty());
        return Sequence(OptInfoEw(infoVar), "command", EW(), ElementNameAsName(), OptEW(),
                Optional(':', OptEW(), NonNegativeNumberAsInteger(), idVar.set(((ImmutableDecodeElementWrapper<Integer>) pop()).getValue()), OptEW()),
                '(', Optional(OptEW(), CommandArgs(namespaceVar, argsVar)), OptEW(), ')',
                Optional(OptEW(), "->", OptEW(), push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                        returnTypeVar.set((Optional<DecodeMaybeProxy<DecodeType>>) pop())),
                push(ImmutableDecodeCommand.newInstance((DecodeNameImpl) pop(),
                        Option.apply(idVar.get()), Option.apply(infoVar.get()),
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
                push(new DecodeCommandArgumentImpl((DecodeNameImpl) pop(), getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop()),
                                Option.apply(unitVar.get()),
                                Option.apply(infoVar.get()))));
    }

    Rule StructFieldAsStructField(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<DecodeMaybeProxy<DecodeUnit>> unitVar = new Var<>();
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar), TypeUnitApplicationAsProxyType(namespaceVar,
                        unitVar), EW(), ElementNameAsName(),
                push(ImmutableDecodeStructField
                        .newInstance((DecodeNameImpl) pop(), getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop()),
                                Option.apply(unitVar.get()),
                                Option.apply(infoVar.get()))));
    }

    Rule TypeUnitApplicationAsProxyType(@NotNull Var<DecodeNamespace> namespaceVar,
                                        @NotNull Var<DecodeMaybeProxy<DecodeUnit>> unitVar)
    {
        return Sequence(push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(), Optional(
                OptEW(), UnitAsFqn(), unitVar.set(unitForFqn((DecodeFqn) pop(), namespaceVar.get()))));
    }

    @NotNull
    DecodeMaybeProxy<DecodeUnit> unitForFqn(@NotNull DecodeFqn unitFqn, @NotNull DecodeNamespace namespace)
    {
        if (unitFqn.size() == 1 && imports.containsKey(unitFqn.asString()))
        {
            return (DecodeMaybeProxy<DecodeUnit>) (DecodeMaybeProxy<?>) imports.get(unitFqn.asString());
        }
        return proxyDefaultNamespace(unitFqn, namespace);
    }

    Rule TypeApplicationAsOptionalProxyType()
    {
        Var<List<Optional<DecodeMaybeProxy<DecodeReferenceable>>>> genericTypesVar = new Var<>(new ArrayList<>());
        Var<DecodeNamespace> namespaceVar = new Var<>();
        return Sequence(
                // FIXME: Parboiled bug
                namespaceVar.set((DecodeNamespace) pop()),
                FirstOf(
                        PrimitiveTypeApplicationAsOptionalProxyType(),
                        NativeTypeApplicationAsOptionalProxyType(),
                        // FIXME: bug with Var in parboiled
                        Sequence(push(namespaceVar.get()),
                                ArrayTypeApplicationAsOptionalProxyType()),
                        Sequence(ElementIdAsFqn(),
                                push(Optional.of(proxyForTypeFqn(namespaceVar.get(), (DecodeFqn) pop()))))),
                Optional(OptEW(), '<', push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                        genericTypesVar.get().add((Optional<DecodeMaybeProxy<DecodeReferenceable>>) pop()),
                        ZeroOrMore(OptEW(), ',', OptEW(), push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                                genericTypesVar.get().add((Optional<DecodeMaybeProxy<DecodeReferenceable>>) pop())),
                        Optional(OptEW(), ','), OptEW(), '>', push(Optional.of(SimpleDecodeMaybeProxy.proxyForTypeUriString(
                                getOrThrow((Optional<DecodeMaybeProxy<DecodeReferenceable>>) pop()).proxy().uri() + "%3C" +
                                        genericTypesListToTypesUriString(genericTypesVar.get()) + "%3E", namespaceVar.get().fqn())))),
                Optional(OptEW(), '?',
                        push(Optional.of(proxyForSystem(DecodeNameImpl.newFromSourceName("optional<"
                                + getOrThrow((Optional<DecodeMaybeProxy<DecodeReferenceable>>) pop()).proxy().uri().toString()
                                + ">"))))));
    }

    DecodeMaybeProxy<DecodeReferenceable> proxyForTypeFqn(@NotNull DecodeNamespace namespace,
                                                          @NotNull DecodeFqn typeFqn)
    {
        if (typeFqn.size() == 1 && imports.containsKey(typeFqn.last().asString()))
        {
            return imports.get(typeFqn.last().asString());
        }
        return proxyDefaultNamespace(typeFqn, namespace);
    }

    @NotNull
    static String genericTypesListToTypesUriString(@NotNull List<Optional<DecodeMaybeProxy<DecodeReferenceable>>> genericTypesVar)
    {
        String result = genericTypesVar.stream().map(op -> op.map(DecodeMaybeProxy::proxy)
                .map(DecodeProxy::uri).map(Object::toString).map(s -> {
                    try
                    {
                        return URLEncoder.encode(s, Charsets.UTF_8.name());
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        throw new ParsingException(e);
                    }
                }).orElse("void")).collect(
                Collectors.joining(","));
        Preconditions.checkState(!result.contains("/") && !result.contains("<"), "invalid types string");
        return result;
    }

    Rule UnitAsFqn()
    {
        return Sequence('/', ElementIdAsFqn(), '/');
    }

    Rule NativeTypeApplicationAsOptionalProxyType()
    {
        return FirstOf(Sequence("void", push(Optional.empty())),
                Sequence("ber",
                        push(Optional.of(proxyForSystem(DecodeNameImpl.newFromSourceName(match()))))));
    }

    Rule PrimitiveTypeApplicationAsOptionalProxyType()
    {
        return Sequence(Sequence(PrimitiveTypeKind(), ':', NonNegativeNumber()),
                push(Optional.of(proxyForSystem(DecodeNameImpl.newFromSourceName(match())))));
    }

    // FIXME: uses DecodeNamespace ref from stack, should use Var, parboiled bug
    Rule ArrayTypeApplicationAsOptionalProxyType()
    {
        Var<DecodeNamespace> namespaceVar = new Var<>();
        Var<DecodeMaybeProxy<DecodeType>> typeApplicationVar = new Var<>();
        return Sequence(
                Sequence(namespaceVar.set((DecodeNamespace) pop()), '[', OptEW(),
                        push(namespaceVar.get()),
                        TypeApplicationAsOptionalProxyType(), typeApplicationVar.set(getOrThrow((Optional<DecodeMaybeProxy<DecodeType>>) pop())),
                        Optional(OptEW(), ',', OptEW(),
                                Sequence(LengthFrom(), Optional(OptEW(), "..", OptEW(), LengthTo()))),
                        OptEW(), ']'),
                push(Optional.of(proxy(DecodeUtils.getNamespaceFqnFromUri(typeApplicationVar.get().proxy().uri()), DecodeNameImpl
                        .newFromSourceName(match())))));
    }

    static <T> T getOrThrow(@NotNull Option<T> optional)
    {
        return optional.orElse(() -> { throw new ParsingException("void type is not allowed here"); });
    }

    Rule StructTypeFields(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<List<DecodeStructField>> fieldsVar)
    {
        return Sequence('(', OptEW(),
                StructFieldAsStructField(namespaceVar), fieldsVar.get().add((DecodeStructField) pop()),
                ZeroOrMore(OptEW(), ',', OptEW(), StructFieldAsStructField(namespaceVar),
                        fieldsVar.get().add((DecodeStructField) pop())),
                Optional(OptEW(), ','), OptEW(), ')');
    }

    Rule StructTypeDeclAsStructType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeNameImpl> nameVar)
    {
        Var<String> infoVar = new Var<>();
        Var<List<DecodeStructField>> fieldsVar = new Var<>(new ArrayList<>());
        return Sequence("struct", OptEW(), StructTypeFields(namespaceVar, fieldsVar),
                push(new DecodeStructTypeImpl(Option.apply(nameVar.get()), namespaceVar.get(),
                                Option.apply(infoVar.get()), fieldsVar.get())));
    }

    Rule ElementIdAsFqn()
    {
        Var<Buffer<DecodeName>> names = new Var<>();
        return Sequence(ElementNameAsName(), names.set(new ArrayBuffer<>()), names.get().$plus$eq((DecodeName) pop()),
                ZeroOrMore('.', ElementNameAsName(), names.get().$plus$eq((DecodeName) pop())),
                push(DecodeFqnImpl.apply(names.get())));
    }

    Rule MessageAsMessage(@NotNull Var<DecodeComponent> componentVar)
    {
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar),
                FirstOf(StatusMessageAsMessage(componentVar, infoVar),
                        EventMessageAsMessage(componentVar, infoVar)));
    }

    Rule MessageNameId(@NotNull Var<DecodeNameImpl> nameVar, @NotNull Var<Integer> idVar)
    {
        return Sequence(ElementNameAsName(), nameVar.set((DecodeNameImpl) pop()),
            Optional(OptEW(), ':', OptEW(),
                    NonNegativeNumberAsInteger(), idVar.set(((ImmutableDecodeElementWrapper<Integer>) pop()).getValue())));
    }

    Rule StatusMessageAsMessage(@NotNull Var<DecodeComponent> componentVar, @NotNull Var<String> infoVar)
    {
        Var<List<DecodeMessageParameter>> parametersVar = new Var<>(new ArrayList<>());
        Var<DecodeNameImpl> nameVar = new Var<>();
        Var<Integer> idVar = new Var<>();
        return Sequence("status", EW(), MessageNameId(nameVar, idVar), OptEW(), MessageParameters(parametersVar),
                push(new DecodeStatusMessageImpl(componentVar.get(), nameVar.get(), Option.apply(idVar.get()),
                        Option.apply(infoVar.get()), parametersVar.get())));
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
        @NotNull Var<DecodeNameImpl> nameVar = new Var<>();
        @NotNull Var<Integer> idVar = new Var<>();
        Var<List<DecodeMessageParameter>> parametersVar = new Var<>(new ArrayList<>());
        return Sequence("event", EW(), MessageNameId(nameVar, idVar), EW(), push(componentVar.get().namespace()), TypeApplicationAsOptionalProxyType(), OptEW(), MessageParameters(parametersVar),
                push(new DecodeEventMessageImpl(componentVar.get(), nameVar.get(), Option.apply(idVar.get()),
                        Option.apply(infoVar.get()), parametersVar.get(), getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop()))));
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
                      @NotNull Var<Buffer<DecodeComponentRef>> subComponentsVar)
    {
        return Sequence(ElementIdAsFqn(), addSubcomponent(subComponentsVar.get(), (DecodeFqn) pop(), namespaceVar.get()));
    }

    boolean addSubcomponent(@NotNull Buffer<DecodeComponentRef> componentRefs, @NotNull DecodeFqn fqn,
                            @NotNull DecodeNamespace namespace)
    {
        String alias = fqn.asString();
        if (fqn.size() == 1 && imports.containsKey(alias))
        {
            componentRefs.$plus$eq(new DecodeComponentRefImpl(alias,
                    (DecodeMaybeProxy<DecodeComponent>) (DecodeMaybeProxy<?>) Preconditions.checkNotNull(imports.get(
                    alias))));
        }
        else
        {
            componentRefs.$plus$eq(new DecodeComponentRef(proxyDefaultNamespace(fqn, namespace)));
        }
        return true;
    }

    Rule UnitDeclAsUnit(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        Var<String> displayVar = new Var<>();
        return Sequence("unit", EW(), ElementNameAsName(), Optional(EW(), "display", EW(), StringValueAsString(),
                        displayVar.set(((ImmutableDecodeElementWrapper<String>) pop()).getValue())),
                Optional(EW(), "placement", EW(), FirstOf("before", "after")),
                push(new DecodeUnitImpl((DecodeName) pop(), namespaceVar.get(),
                        displayVar.get(), infoVar.get())));
    }

    Rule TypeDeclAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        Var<DecodeNameImpl> nameVar = new Var<>();
        return Sequence("type", EW(), ElementNameAsName(), nameVar.set((DecodeNameImpl) pop()), EW(),
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

    Rule TypeDeclBodyAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeNameImpl> nameVar, @NotNull Var<String> infoVar)
    {
        return FirstOf(
                EnumTypeDeclAsType(namespaceVar, nameVar),
                StructTypeDeclAsStructType(namespaceVar, nameVar),
                Sequence(push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                        push(new DecodeSubTypeImpl(Option.apply(nameVar.get()), namespaceVar.get(),
                                        getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop()), Option.apply(
                                                infoVar.get())))));
    }

    Rule EnumTypeDeclAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeNameImpl> nameVar)
    {
        Var<String> infoVar = new Var<>();
        Var<Set<DecodeEnumConstant>> enumConstantsVar = new Var<>(new HashSet<>());
        return Sequence("enum", EW(), push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(), OptEW(), '(', OptEW(),
                EnumTypeValues(enumConstantsVar), OptEW(), ')',
                push(new DecodeEnumTypeImpl(Option.apply(nameVar.get()), namespaceVar.get(),
                        getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop()),
                        Option.apply(infoVar.get()), enumConstantsVar.get())));
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
                enumConstantsVar.get().add(ImmutableDecodeEnumConstant.newInstanceWrapper((DecodeNameImpl) pop(1),
                        (ImmutableDecodeElementWrapper<String>) pop(), Option.apply(infoVar.get()))));
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
        DecodeNameImpl getOriginalName();
    }

    static class ImportPartName implements ImportPart
    {
        @NotNull
        private final DecodeNameImpl name;

        public ImportPartName(@NotNull DecodeNameImpl name)
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
        public DecodeNameImpl getOriginalName()
        {
            return name;
        }
    }

    static class ImportPartNameAlias implements ImportPart
    {
        @NotNull
        private final DecodeNameImpl name;
        @NotNull
        private final DecodeNameImpl alias;

        public ImportPartNameAlias(@NotNull DecodeNameImpl name, @NotNull DecodeNameImpl alias)
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
        public DecodeNameImpl getOriginalName()
        {
            return name;
        }
    }
}
