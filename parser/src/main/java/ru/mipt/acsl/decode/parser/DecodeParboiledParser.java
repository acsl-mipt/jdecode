package ru.mipt.acsl.decode.parser;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
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
import ru.mipt.acsl.decode.model.domain.impl.type.*;
import ru.mipt.acsl.parsing.ParsingException;
import scala.Int;
import scala.Option;
import scala.Some;
import scala.collection.JavaConversions;
import scala.collection.mutable.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.stream.Collectors;

import static ru.mipt.acsl.ScalaToJava.asOptional;
import static ru.mipt.acsl.decode.ScalaUtil.appendToBuffer;
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
                                        append(namespaceVar.get().components(), (DecodeComponent) pop())),
                                Sequence(UnitDeclAsUnit(namespaceVar, infoVar),
                                        append(namespaceVar.get().units(), (DecodeUnit) pop())),
                                Sequence(TypeDeclAsType(namespaceVar, infoVar),
                                        append(namespaceVar.get().types(), (DecodeType) pop())),
                                Sequence(AliasAsAlias(namespaceVar, infoVar),
                                        append(namespaceVar.get().types(), (DecodeType) pop())),
                                Sequence(LanguageAsLanguage(namespaceVar, infoVar),
                                        append(namespaceVar.get().languages(), (DecodeLanguage) pop())))),
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
        Preconditions.checkState(!imports.contains(importPart.getAlias()));
        imports.put(importPart.getAlias(), SimpleDecodeMaybeProxy.proxy(namespaceFqnVar.get(), importPart.getOriginalName()));
        return true;
    }

    boolean addImport(@NotNull DecodeFqn fqn)
    {
        Preconditions.checkState(!imports.contains(fqn.last().asString()));
        imports.put(fqn.last().asString(), SimpleDecodeMaybeProxy.proxy(fqn.copyDropLast(), fqn.last()));
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
                push(new DecodeLanguageImpl((DecodeName) pop(), namespaceVar.get(), defaultVar.get(),
                        Option.apply(infoVar.get()))));
    }

    Rule AliasAsAlias(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        return Sequence("alias", EW(), ElementNameAsName(), EW(), push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                push(new DecodeAliasTypeImpl((DecodeNameImpl) pop(1), namespaceVar.get(),
                        getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop()), Option.apply(infoVar.get()))));
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
            appendToBuffer(parentNamespace.subNamespaces(), result);
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
        Var<Integer> idVar = new Var<>();
        return Sequence("component", EW(), ElementNameAsName(), baseTypeNameVar.set((DecodeNameImpl) peek()),
                Optional(OptEW(), ':', NonNegativeNumberAsInteger(), idVar.set((Integer) pop())),
                Optional(EW(), "with", OptEW(), Subcomponent(namespaceVar, subComponentsVar),
                        ZeroOrMore(OptEW(), ',', OptEW(), Subcomponent(namespaceVar, subComponentsVar))),
                OptEW(), '{',
                Optional(OptEW(), ComponentParametersAsType(namespaceVar, baseTypeNameVar),
                        typeVar.set((DecodeStructType) pop()), append(namespaceVar.get().types(), typeVar.get())),
                componentVar.set(new DecodeComponentImpl((DecodeNameImpl) pop(), namespaceVar.get(), Option.apply(idVar.get()),
                                typeVar.isSet()
                                        ? Option.apply(SimpleDecodeMaybeProxy.object(typeVar.get()))
                                        : Option.<DecodeMaybeProxy<DecodeStructType>>empty(), Option.apply(infoVar.get()),
                                subComponentsVar.get(), commandsVar.get(), messagesVar.get())),
                ZeroOrMore(OptEW(),
                        FirstOf(Sequence(CommandAsCommand(namespaceVar), append(commandsVar.get(), (DecodeCommand) pop())),
                                Sequence(MessageAsMessage(componentVar), append(messagesVar.get(), (DecodeMessage) pop())))),
                OptEW(), '}',
                push(componentVar.get()));
    }

    Rule ComponentParametersAsType(@NotNull Var<DecodeNamespace> namespaceVar,
                                   @NotNull Var<DecodeName> nameVar)
    {
        Var<Buffer<DecodeStructField>> fieldsVar = new Var<>(new ArrayBuffer<>());
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar), "parameters", OptEW(),
                StructTypeFields(namespaceVar, fieldsVar), push(new DecodeStructTypeImpl(makeNewSystemName(Option.apply(nameVar.get())), namespaceVar.get(),
                                Option.apply(infoVar.get()), fieldsVar.get().toSeq())));
    }

    @NotNull
    static Option<DecodeName> makeNewSystemName(@NotNull Option<DecodeName> name)
    {
        return Option.apply(name.isDefined() ? DecodeNameImpl.newFromSourceName(name.get().asString() + "$") : null);
    }

    Rule CommandAsCommand(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<Buffer<DecodeCommandArgument>> argsVar = new Var<>(new ArrayBuffer<>());
        Var<String> infoVar = new Var<>();
        Var<Integer> idVar = new Var<>();
        Var<Option<DecodeMaybeProxy<DecodeType>>> returnTypeVar = new Var<>(Option.empty());
        return Sequence(OptInfoEw(infoVar), "command", EW(), ElementNameAsName(), OptEW(),
                Optional(':', OptEW(), NonNegativeNumberAsInteger(), idVar.set((Integer) pop()), OptEW()),
                '(', Optional(OptEW(), CommandArgs(namespaceVar, argsVar)), OptEW(), ')',
                Optional(OptEW(), "->", OptEW(), push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                        returnTypeVar.set((Option<DecodeMaybeProxy<DecodeType>>) pop())),
                push(new DecodeCommandImpl((DecodeNameImpl) pop(),
                        Option.apply(idVar.get()), Option.apply(infoVar.get()),
                        argsVar.get().toSeq(), returnTypeVar.get())));
    }

    Rule CommandArgs(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<Buffer<DecodeCommandArgument>> argsVar)
    {
        return Sequence(CommandArgAsCommandArg(namespaceVar), append(argsVar.get(), (DecodeCommandArgument) pop()),
                ZeroOrMore(OptEW(), ',', OptEW(), CommandArgAsCommandArg(namespaceVar), append(argsVar.get(),
                        (DecodeCommandArgument) pop())), Optional(OptEW(), ','));
    }

    Rule CommandArgAsCommandArg(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<DecodeMaybeProxy<DecodeUnit>> unitVar = new Var<>();
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar), TypeUnitApplicationAsProxyType(namespaceVar, unitVar), EW(), ElementNameAsName(),
                push(new DecodeCommandArgumentImpl((DecodeName) pop(), Option.apply(infoVar.get()), getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop()),
                                Option.apply(unitVar.get()))));
    }

    Rule StructFieldAsStructField(@NotNull Var<DecodeNamespace> namespaceVar)
    {
        Var<DecodeMaybeProxy<DecodeUnit>> unitVar = new Var<>();
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar), TypeUnitApplicationAsProxyType(namespaceVar,
                        unitVar), EW(), ElementNameAsName(),
                push(new DecodeStructFieldImpl((DecodeNameImpl) pop(),
                        getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop()),
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
        if (unitFqn.size() == 1 && imports.contains(unitFqn.asString()))
        {
            return (DecodeMaybeProxy<DecodeUnit>) (DecodeMaybeProxy<?>) imports.get(unitFqn.asString()).get();
        }
        return proxyDefaultNamespace(unitFqn, namespace);
    }

    Rule TypeApplicationAsOptionalProxyType()
    {
        Var<Buffer<Option<DecodeMaybeProxy<DecodeReferenceable>>>> genericTypesVar = new Var<>(new ArrayBuffer<>());
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
                                push(Option.apply(proxyForTypeFqn(namespaceVar.get(), (DecodeFqn) pop()))))),
                Optional(OptEW(), '<', push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                        append(genericTypesVar.get(), (Option<DecodeMaybeProxy<DecodeReferenceable>>) pop()),
                        ZeroOrMore(OptEW(), ',', OptEW(), push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                                append(genericTypesVar.get(), (Option<DecodeMaybeProxy<DecodeReferenceable>>) pop())),
                        Optional(OptEW(), ','), OptEW(), '>', push(Option.apply(SimpleDecodeMaybeProxy.proxyForTypeUriString(
                                getOrThrow((Option<DecodeMaybeProxy<DecodeReferenceable>>) pop()).proxy().uri() + "%3C" +
                                        genericTypesListToTypesUriString(genericTypesVar.get()) + "%3E", namespaceVar.get().fqn())))),
                Optional(OptEW(), '?',
                        push(Option.apply(proxyForSystem(DecodeNameImpl.newFromSourceName("optional<"
                                + getOrThrow((Option<DecodeMaybeProxy<DecodeReferenceable>>) pop()).proxy().uri().toString()
                                + ">"))))));
    }

    DecodeMaybeProxy<DecodeReferenceable> proxyForTypeFqn(@NotNull DecodeNamespace namespace,
                                                          @NotNull DecodeFqn typeFqn)
    {
        if (typeFqn.size() == 1 && imports.contains(typeFqn.last().asString()))
        {
            return imports.get(typeFqn.last().asString()).get();
        }
        return proxyDefaultNamespace(typeFqn, namespace);
    }

    @NotNull
    static String genericTypesListToTypesUriString(@NotNull Buffer<Option<DecodeMaybeProxy<DecodeReferenceable>>> genericTypesVar)
    {
        String result = JavaConversions.asJavaCollection(genericTypesVar).stream().map(op -> asOptional(op).map(DecodeMaybeProxy::proxy)
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
        return FirstOf(Sequence("void", push(Option.empty())),
                Sequence("ber",
                        push(Some.apply(proxyForSystem(DecodeNameImpl.newFromSourceName(match()))))));
    }

    Rule PrimitiveTypeApplicationAsOptionalProxyType()
    {
        return Sequence(Sequence(PrimitiveTypeKind(), ':', NonNegativeNumber()),
                push(Some.apply(proxyForSystem(DecodeNameImpl.newFromSourceName(match())))));
    }

    // FIXME: uses DecodeNamespace ref from stack, should use Var, parboiled bug
    Rule ArrayTypeApplicationAsOptionalProxyType()
    {
        Var<DecodeNamespace> namespaceVar = new Var<>();
        Var<DecodeMaybeProxy<DecodeType>> typeApplicationVar = new Var<>();
        return Sequence(
                Sequence(namespaceVar.set((DecodeNamespace) pop()), '[', OptEW(),
                        push(namespaceVar.get()),
                        TypeApplicationAsOptionalProxyType(), typeApplicationVar.set(getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop())),
                        Optional(OptEW(), ',', OptEW(),
                                Sequence(LengthFrom(), Optional(OptEW(), "..", OptEW(), LengthTo()))),
                        OptEW(), ']'),
                push(Option.apply(proxy(DecodeUtils.getNamespaceFqnFromUri(typeApplicationVar.get().proxy().uri()), DecodeNameImpl
                        .newFromSourceName(match())))));
    }

    @NotNull
    static <T> T getOrThrow(@NotNull Option<T> option)
    {
        return asOptional(option).orElseThrow(() -> new ParsingException("void type is not allowed here"));
    }

    Rule StructTypeFields(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<Buffer<DecodeStructField>> fieldsVar)
    {
        return Sequence('(', OptEW(),
                StructFieldAsStructField(namespaceVar), append(fieldsVar.get(), (DecodeStructField) pop()),
                ZeroOrMore(OptEW(), ',', OptEW(), StructFieldAsStructField(namespaceVar),
                        append(fieldsVar.get(), (DecodeStructField) pop())),
                Optional(OptEW(), ','), OptEW(), ')');
    }

    Rule StructTypeDeclAsStructType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeName> nameVar)
    {
        Var<String> infoVar = new Var<>();
        Var<Buffer<DecodeStructField>> fieldsVar = new Var<>(new ArrayBuffer<>());
        return Sequence("struct", OptEW(), StructTypeFields(namespaceVar, fieldsVar),
                push(new DecodeStructTypeImpl(Option.apply(nameVar.get()), namespaceVar.get(),
                                Option.apply(infoVar.get()), fieldsVar.get().toSeq())));
    }

    Rule ElementIdAsFqn()
    {
        Var<Buffer<DecodeName>> names = new Var<>();
        return Sequence(ElementNameAsName(), names.set(new ArrayBuffer<>()), append(names.get(), (DecodeName) pop()),
                ZeroOrMore('.', ElementNameAsName(), append(names.get(), (DecodeName) pop())),
                push(DecodeFqnImpl.apply(names.get().toSeq())));
    }

    public static <T> boolean append(@NotNull Buffer<T> buffer, T value)
    {
        appendToBuffer(buffer, value);
        return true;
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
                    NonNegativeNumberAsInteger(), idVar.set((Integer) pop())));
    }

    Rule StatusMessageAsMessage(@NotNull Var<DecodeComponent> componentVar, @NotNull Var<String> infoVar)
    {
        Var<Buffer<DecodeMessageParameter>> parametersVar = new Var<>(new ArrayBuffer<>());
        Var<DecodeName> nameVar = new Var<>();
        Var<Integer> idVar = new Var<>();
        return Sequence("status", EW(), MessageNameId(nameVar, idVar), OptEW(), MessageParameters(parametersVar),
                push(new DecodeStatusMessageImpl(componentVar.get(), nameVar.get(), Option.apply(idVar.get()),
                        Option.apply(infoVar.get()), parametersVar.get().toSeq())));
    }

    Rule MessageParameters(@NotNull Var<Buffer<DecodeMessageParameter>> parametersVar)
    {
        return Sequence('(', OptEW(), ParameterAsParameter(), append(parametersVar.get(), (DecodeMessageParameter) pop()),
                        ZeroOrMore(OptEW(), ',', OptEW(), ParameterAsParameter(), append(parametersVar.get(),
                                (DecodeMessageParameter) pop())),
                        Optional(OptEW(), ','), OptEW(), ')');
    }

    Rule EventMessageAsMessage(@NotNull Var<DecodeComponent> componentVar, @NotNull Var<String> infoVar)
    {
        @NotNull Var<DecodeName> nameVar = new Var<>();
        @NotNull Var<Integer> idVar = new Var<>();
        Var<Buffer<DecodeMessageParameter>> parametersVar = new Var<>(new ArrayBuffer<>());
        return Sequence("event", EW(), MessageNameId(nameVar, idVar), EW(), push(componentVar.get().namespace()), TypeApplicationAsOptionalProxyType(), OptEW(), MessageParameters(parametersVar),
                push(new DecodeEventMessageImpl(componentVar.get(), nameVar.get(), Option.apply(idVar.get()),
                        Option.apply(infoVar.get()), parametersVar.get(), getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop()))));
    }

    Rule ParameterAsParameter()
    {
        Var<String> infoVar = new Var<>();
        return Sequence(OptInfoEw(infoVar), ParameterElement(), push(match()),
                        push(new DecodeMessageParameterImpl((String) pop())));
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
                infoVar.set((String) pop()), EW());
    }

    Rule StringValueAsString()
    {
        return FirstOf(Sequence('"', ZeroOrMore(FirstOf(NoneOf("\"\\"), Sequence('\\', ANY))),
                        push(match()),
                        FirstOf('"', Sequence(drop(), NOTHING))),
                Sequence('\'', ZeroOrMore(FirstOf(NoneOf("\'\\"), Sequence('\\', ANY))),
                        push(match()),
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
        if (fqn.size() == 1 && imports.contains(alias))
        {
            appendToBuffer(componentRefs, new DecodeComponentRefImpl((DecodeMaybeProxy<DecodeComponent>) (DecodeMaybeProxy<?>) Preconditions.checkNotNull(imports.get(
                    alias)), Some.apply(alias)));
        }
        else
        {
            appendToBuffer(componentRefs, new DecodeComponentRefImpl(proxyDefaultNamespace(fqn, namespace), Option.<String>empty()));
        }
        return true;
    }

    Rule UnitDeclAsUnit(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        Var<String> displayVar = new Var<>();
        return Sequence("unit", EW(), ElementNameAsName(), Optional(EW(), "display", EW(), StringValueAsString(),
                        displayVar.set((String) pop())),
                Optional(EW(), "placement", EW(), FirstOf("before", "after")),
                push(new DecodeUnitImpl((DecodeName) pop(), namespaceVar.get(),
                        Option.apply(displayVar.get()), Option.apply(infoVar.get()))));
    }

    Rule TypeDeclAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<String> infoVar)
    {
        Var<DecodeName> nameVar = new Var<>();
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

    Rule TypeDeclBodyAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeName> nameVar, @NotNull Var<String> infoVar)
    {
        return FirstOf(
                EnumTypeDeclAsType(namespaceVar, nameVar),
                StructTypeDeclAsStructType(namespaceVar, nameVar),
                Sequence(push(namespaceVar.get()), TypeApplicationAsOptionalProxyType(),
                        push(new DecodeSubTypeImpl(Option.apply(nameVar.get()), namespaceVar.get(), Option.apply(
                                infoVar.get()), getOrThrow((Option<DecodeMaybeProxy<DecodeType>>) pop())))));
    }

    Rule EnumTypeDeclAsType(@NotNull Var<DecodeNamespace> namespaceVar, @NotNull Var<DecodeName> nameVar)
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
                enumConstantsVar.get().add(new DecodeEnumConstantImpl((DecodeNameImpl) pop(1),
                        (String) pop(), Option.apply(infoVar.get()))));
    }

    Rule LiteralAsString()
    {
        return Sequence(FirstOf(FloatLiteral(), NonNegativeNumber(), "true", "false"),
                push(match()));
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
        return Sequence(NonNegativeNumber(), push(Int.box(Integer.parseInt(match()))));
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
