package ru.mipt.acsl.decode.parser

import org.parboiled2._
import ru.mipt.acsl.decode.model.domain.impl.DecodeNameImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeFqnImpl
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeNamespaceImpl
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy

import scala.collection.mutable

class DecodeParboiled2Parser(val input: ParserInput) extends Parser {

  private val alpha = CharPredicate.Alpha ++ '_'
  private val alphaNum = alpha ++ CharPredicate.Digit
  private val imports = mutable.HashMap[String, DecodeMaybeProxy[DecodeReferenceable]]()

  val Ew = rule { anyOf(" \t\r\n").+ }

  def quotedString(quoteChar: Char): Rule1[String] = rule {
    atomic(ch(quoteChar) ~ capture(zeroOrMore(!anyOf(quoteChar + "\\") ~ ANY | '\\' ~ ANY)) ~ ch(quoteChar)).named("stringLiteral")
  }

  val StringValue: Rule1[String] = rule { quotedString('"') | quotedString('\'') }

  /*
  Rule ImportPartAsImportPart()
    {
        return Sequence(ElementNameAsName(), FirstOf(Sequence(OptEW(), "as", OptEW(), ElementNameAsName(), push(new ImportPartNameAlias((DecodeName) pop(1), (DecodeName) pop()))),
                push(new ImportPartName((DecodeName) pop()))));
    }
  */

  val OptInfoWs: Rule1[Option[String]] = rule { (StringValue ~ Ew).? }

  /*
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
  */

  val ElementName: Rule1[DecodeName] = rule { '^'.? ~ capture(alpha ~ alphaNum.*) ~> DecodeNameImpl.newFromSourceName _ }

  def ElementId: Rule1[DecodeFqn] = rule { ElementName.+('.') ~> DecodeFqnImpl.apply _ }

  val Namespace: Rule1[DecodeNamespace] = rule { OptInfoWs ~ atomic("namespace") ~ Ew ~ ElementId ~> ((info: Option[String], fqn: DecodeFqn) => newNamespace(info, fqn)) }

  private val ImportPart: Rule1[ImportPart] = rule {
    ElementName ~ (Ew ~ atomic("as") ~ Ew ~ ElementName ~> ((as: DecodeName, name: DecodeName) => ImportPartNameAlias(name, as))
      | MATCH ~> (ImportPartName(_: DecodeName)))
  }

  /*

   boolean addImport(@NotNull Var<DecodeFqn> namespaceFqnVar, @NotNull ImportPart importPart)
    {
        Preconditions.checkState(!imports.contains(importPart.getAlias()));
        imports.put(importPart.getAlias(), SimpleDecodeMaybeProxy.proxy(namespaceFqnVar.get(), importPart.getOriginalName()));
        return true;
    }
  */

  def addImport(fqn: DecodeFqn, importPart: ImportPart) {
    require(!imports.contains(importPart.alias))
    imports.put(importPart.alias, SimpleDecodeMaybeProxy.proxy(fqn, importPart.originalName))
  }

  def addImport(fqn: DecodeFqn) {
    require(!imports.contains(fqn.last.asString()))
    imports.put(fqn.last.asString(), SimpleDecodeMaybeProxy.proxy(fqn.copyDropLast(), fqn.last))
  }

  private var fqn: Option[DecodeFqn] = None
  val Import: Rule0 = rule {
    atomic("import") ~ Ew ~ ElementId ~> ((_fqn: DecodeFqn) => { fqn = Some(_fqn) }: Unit) ~
      ( '.' ~ Ew.? ~ '{' ~!~ Ew.? ~ ImportPart
        ~ (Ew.? ~ ',' ~ Ew.? ~ ImportPart ~> (addImport(fqn.get, _))).*
        ~> (addImport(fqn.get, _)) ~ Ew.? ~ ','.? ~ Ew.? ~ '}'
        | MATCH ~> (() => addImport(fqn.get)))
  }

  val File: Rule1[DecodeNamespace] = rule {
    run(() => imports.clear()) ~ Namespace ~ (Ew ~ Import).*
  }

  def newNamespace(info: Option[String], fqn: DecodeFqn) = new DecodeNamespaceImpl(new DecodeNameImpl("not implemented"))
  /*

    Rule W()
    {
        return OneOrMore(AnyOf(" \t"));
    }

    Rule OptW()
    {
        return Optional(W());
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


   */
}

private trait ImportPart {
  def alias: String
  def originalName: DecodeName
}

private case class ImportPartName(originalName: DecodeName) extends ImportPart {
  def alias: String = originalName.asString()
}

private case class ImportPartNameAlias(originalName: DecodeName, _alias: DecodeName) extends ImportPart {
  def alias: String = _alias.asString()
}