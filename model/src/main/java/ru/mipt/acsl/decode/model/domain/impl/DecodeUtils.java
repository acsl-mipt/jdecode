package ru.mipt.acsl.decode.model.domain.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.JavaToScala;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.proxy.SimpleDecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.impl.type.DecodeFqnImpl;
import ru.mipt.acsl.decode.model.domain.impl.type.DecodeNamespaceImpl;
import ru.mipt.acsl.decode.model.exporter.ModelExportingException;
import ru.mipt.acsl.decode.model.provider.ModelImportingException;
import scala.Function1;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Buffer;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.mipt.acsl.JavaToScala.asOption;
import static scala.collection.JavaConversions.asJavaCollection;

/**
 * @author Artem Shein
 */
public final class DecodeUtils
{
    @NotNull
    public static DecodeNamespace getOrCreateNamespaceByFqn(@NotNull DecodeRegistry registry,
                                                           @NotNull String namespaceFqn)
    {
        Preconditions.checkArgument(!namespaceFqn.isEmpty());

        Collection<DecodeNamespace>
                namespaces = asJavaCollection(registry.rootNamespaces());
        DecodeNamespace namespace = null;

        for (String namespaceName : namespaceFqn.split(Pattern.quote(".")))
        {
            final DecodeNamespace parentNamespace = namespace;
            namespace = namespaces.stream().filter(ns -> ns.name().asString().equals(namespaceName)).findAny().orElseGet(() -> {
                DecodeNamespace newNamespace = DecodeNamespaceImpl.apply(
                        DecodeNameImpl.newFromMangledName(namespaceName),
                        Option.apply(parentNamespace));
                if (parentNamespace != null)
                {
                    parentNamespace.subNamespaces().$plus$eq(newNamespace);
                }
                else
                {
                    registry.rootNamespaces().$plus$eq(newNamespace);
                }
                return newNamespace;
            });
        }
        //noinspection ConstantConditions
        return namespace;
    }

    @NotNull
    public static Option<DecodeNamespace> getNamespaceByFqn(@NotNull DecodeRegistry registry,
                                                             @NotNull DecodeFqn namespaceFqn)
    {
        Collection<DecodeNamespace> namespaces = asJavaCollection(registry.rootNamespaces());
        Option<DecodeNamespace> namespace = Option.empty();
        Iterator<DecodeName> iterator = namespaceFqn.parts().iterator();
        while (iterator.hasNext())
        {
            DecodeName namespaceName = iterator.next();
            namespace = asOption(namespaces.stream().filter(ns -> ns.name().equals(namespaceName)).findAny());
            if (!namespace.isDefined())
            {
                return namespace;
            }
            else
            {
                namespaces = asJavaCollection(namespace.get().subNamespaces());
            }
        }
        return  namespace;
    }

    @NotNull
    public static URI getUriForNamespaceAndName(@NotNull DecodeFqn namespaceFqn, @NotNull DecodeName name)
    {
        List<DecodeName> namespaceNameParts = new ArrayList<>(asJavaCollection(namespaceFqn.parts()));
        namespaceNameParts.add(name);
            return URI.create("/" + String.join("/",
                    namespaceNameParts.stream().map(DecodeName::asString).map(s -> {
                        try
                        {
                            return URLEncoder.encode(s, Charsets.UTF_8.name());
                        }
                        catch (UnsupportedEncodingException e)
                        {
                            throw new ModelImportingException(e);
                        }
                    }).collect(
                            Collectors.toList())));
    }

    public static List<String> getUriParts(@NotNull URI uri)
    {
        String s = uri.toString();
        return Stream.of(s.substring(1).split(Pattern.quote("/"))).map(part -> {
            try
            {
                return URLDecoder.decode(part, Charsets.UTF_8.name());
            }
            catch (UnsupportedEncodingException e)
            {
                throw new ModelExportingException(e);
            }
        }).collect(Collectors.toList());
    }







    @NotNull
    public static DecodeNamespace newRootDecodeNamespaceForFqn(@NotNull DecodeFqn namespaceFqn)
    {
        DecodeNamespace namespace = DecodeNamespaceImpl.apply(namespaceFqn.last(),
                Option.<DecodeNamespace>empty());
        Seq<DecodeName> parts = namespaceFqn.parts();
        for (int i = parts.size() - 2; i >= 0; i--)
        {
            DecodeNamespace parentNamespace = DecodeNamespaceImpl.apply(parts.apply(i),
                    Option.<DecodeNamespace>empty());
            namespace.parent_$eq(Option.apply(parentNamespace));
            parentNamespace.subNamespaces().$plus$eq(namespace);
            namespace = parentNamespace;
        }
        return namespace;
    }

    @NotNull
    public static DecodeNamespace newNamespaceForFqn(@NotNull DecodeFqn fqn)
    {
        DecodeNamespace currentNamespace = null;
        Iterator<DecodeName> iterator = fqn.parts().iterator();
        while (iterator.hasNext())
        {
            currentNamespace = DecodeNamespaceImpl.apply(iterator.next(), Option.apply(currentNamespace));
            if (currentNamespace.parent().isDefined())
            {
                currentNamespace.parent().get().subNamespaces().$plus$eq(currentNamespace);
            }
        }
        return Preconditions.checkNotNull(currentNamespace);
    }

    @NotNull
    public static DecodeFqn getNamespaceFqnFromUri(@NotNull URI uri)
    {
        List<String> uriParts = getUriParts(uri);
        return DecodeFqnImpl.apply(JavaConversions.asScalaBuffer(uriParts.stream().limit(uriParts.size() - 1)
                .map(DecodeNameImpl::newFromMangledName).collect(Collectors.toList())));
    }

    @NotNull
    public static URI getUriForSourceTypeFqnString(@NotNull String typeFqnString, @NotNull DecodeFqn defaultNamespaceFqn)
    {
        Preconditions.checkArgument(!typeFqnString.contains("/"), "illegal type fqn '%s'", typeFqnString);
        typeFqnString = normalizeSourceTypeString(typeFqnString);
        typeFqnString = processQuestionMarks(typeFqnString);
        int genericStartIndex = typeFqnString.indexOf('<');
        if (genericStartIndex != -1)
        {
            DecodeFqn namespaceFqn = DecodeFqnImpl.newFromSource(typeFqnString.substring(0, genericStartIndex));
            return getUriForTypeNamespaceNameGenericArguments(namespaceFqn.size() > 1
                            ? namespaceFqn.copyDropLast()
                            : defaultNamespaceFqn, namespaceFqn.last(),
                    typeFqnString.substring(genericStartIndex));
        }
        DecodeFqn namespaceFqn = DecodeFqnImpl.newFromSource(typeFqnString);
        return getUriForNamespaceAndName(namespaceFqn.copyDropLast(), namespaceFqn.last());
    }

    @NotNull
    private static URI getUriForTypeNamespaceNameGenericArguments(@NotNull DecodeFqn namespaceFqn,
                                                                  @NotNull DecodeName typeName,
                                                                  @NotNull String typeGenericArguments)
    {
        List<DecodeName> namespaceNameParts = new ArrayList<>(asJavaCollection(namespaceFqn.parts()));
        namespaceNameParts.add(typeName);
        try
        {
            return URI.create("/" + URLEncoder
                    .encode(String.join("/", namespaceNameParts.stream().map(DecodeName::asString)
                            .map(s -> {
                                try
                                {
                                    return URLEncoder.encode(s, Charsets.UTF_8.name());
                                }
                                catch (UnsupportedEncodingException e)
                                {
                                    throw new ModelImportingException(e);
                                }
                            }).collect(
                            Collectors.toList())) + typeGenericArguments, Charsets.UTF_8.name()));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError(e);
        }
    }

    @NotNull
    private static String processQuestionMarks(@NotNull String typeString)
    {
        if (typeString.endsWith("?"))
        {
            String genericTypes = typeString.substring(0, typeString.length() - 1);
            typeString = DecodeConstants.SYSTEM_NAMESPACE_FQN.asString() + ".optional<"
                    + Stream.of(genericTypes.split(Pattern.quote(",")))
                        .map(DecodeUtils::processQuestionMarks).collect(Collectors.joining(","))
                    + ">";
        }
        return typeString;
    }

    @NotNull
    public static String normalizeSourceTypeString(@NotNull String typeString)
    {
        return typeString.replaceAll(Pattern.quote(" "), "");
    }

    public static String typeUriToTypeName(@NotNull URI uri)
    {
        return typeFqnStringFromUriString(uri.toString());
    }

    @NotNull
    public static Optional<DecodeMaybeProxy<DecodeType>> uriToOptionalMaybeProxyType(@NotNull String typeUriString)
    {
        return "".equals(typeUriString)
                ? Optional.empty()
                : Optional.of(SimpleDecodeMaybeProxy.proxy(URI.create(typeUriString)));
    }

    @NotNull
    public static String typeFqnStringFromUriString(@NotNull String typeUriString)
    {
        try
        {
            return URLDecoder.decode(typeUriString, Charsets.UTF_8.name()).substring(1).replace("/", ".");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new ModelExportingException(e);
        }
    }
}
