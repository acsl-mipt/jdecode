package ru.mipt.acsl.decode.model.domain.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        Buffer<DecodeNamespace> namespaces = registry.rootNamespaces();
        DecodeNamespace namespace = null;

        for (String namespaceName : namespaceFqn.split(Pattern.quote(".")))
        {
            final DecodeNamespace parentNamespace = namespace;
            namespace = namespaces.find(ns -> ns.name().asString().equals(namespaceName)).getOrElse(() -> {
                DecodeNamespace newNamespace = DecodeNamespaceImpl.apply(
                        DecodeNameImpl.newFromMangledName(namespaceName),
                        Option.apply(parentNamespace));
                if (parentNamespace != null)
                {
                    parentNamespace.subNamespaces().$plus$eq(newNamespace);
                }
                else
                {
                    namespaces.$plus$eq(newNamespace);
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
        Buffer<DecodeNamespace> namespaces = registry.rootNamespaces();
        Option<DecodeNamespace> namespace = Option.empty();
        Iterator<DecodeName> iterator = namespaceFqn.parts().iterator();
        while (iterator.hasNext())
        {
            DecodeName namespaceName = iterator.next();
            namespace = namespaces.find(ns -> ns.name().equals(namespaceName));
            if (!namespace.isDefined())
            {
                return namespace;
            }
            else
            {
                namespaces = namespace.get().subNamespaces();
            }
        }
        return  namespace;
    }

    @NotNull
    public static URI getUriForNamespaceAndName(@NotNull DecodeFqn namespaceFqn, @NotNull DecodeName name)
    {
        List<DecodeName> namespaceNameParts = new ArrayList<>(JavaConversions.asJavaCollection(namespaceFqn.parts()));
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
    public static Buffer<DecodeNamespace> mergeRootNamespaces(@NotNull Buffer<DecodeNamespace> namespaces)
    {
        Buffer<DecodeNamespace> result = new ArrayBuffer<>();
        namespaces.foreach(n -> { mergeNamespaceToNamespacesList(result, n); return null; });
        return result;
    }

    private static void mergeNamespaceToNamespacesList(@NotNull Buffer<DecodeNamespace> list,
                                                       @NotNull DecodeNamespace namespace)
    {
        Option<DecodeNamespace> targetNamespace = list.find(n -> n.name().equals(namespace.name()));
        if (targetNamespace.isDefined())
        {
            mergeNamespaceTo(targetNamespace.get(), namespace);
        }
        else
        {
            list.$plus$eq(namespace);
        }
    }

    private static void mergeNamespaceTo(@NotNull DecodeNamespace targetNamespace, @NotNull DecodeNamespace namespace)
    {
        Buffer<DecodeNamespace> subNamespaces = targetNamespace.subNamespaces();
        namespace.subNamespaces().foreach(v1 -> {
            mergeNamespaceToNamespacesList(subNamespaces, v1);
            v1.parent_$eq(Option.apply(targetNamespace));
            return null;
        });

        Buffer<DecodeUnit> units = targetNamespace.units();
        namespace.units().foreach(u ->
        {
            DecodeName name = u.name();
            Preconditions.checkState(units.find(u2 -> u2.name().equals(name)).isEmpty(),
                    "unit name collision '%s'", name);
            u.namespace_$eq(targetNamespace);
            return null;
        });
        units.$plus$plus$eq(namespace.units());

        Buffer<DecodeType> types = targetNamespace.types();
        namespace.types().foreach(t ->
        {
            Option<DecodeName> name = t.optionalName();
            Preconditions.checkState(types.find(t2 -> t2.optionalName().equals(name)).isEmpty(),
                    "type name collision '%s'", name);
            t.namespace_$eq(targetNamespace);
            return null;
        });
        types.$plus$plus$eq(namespace.types());

        Buffer<DecodeComponent> components = targetNamespace.components();
        namespace.components().foreach(c ->
        {
            DecodeName name = c.name();
            Preconditions.checkState(components.find(c2 -> c2.name().equals(name)).isEmpty(),
                    "component name collision '%s'", name);
            c.namespace_$eq(targetNamespace);
            return null;
        });
        components.$plus$plus$eq(namespace.components());
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
        List<DecodeName> namespaceNameParts = new ArrayList<>(JavaConversions.asJavaCollection(namespaceFqn.parts()));
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
