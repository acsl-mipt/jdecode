package ru.mipt.acsl.decode.model.domain.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;

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

        List<DecodeNamespace> namespaces = registry.getRootNamespaces();
        DecodeNamespace namespace = null;

        for (String namespaceName : namespaceFqn.split(Pattern.quote(".")))
        {
            final DecodeNamespace parentNamespace = namespace;
            namespace = namespaces.stream().filter(ns -> ns.getName().asString().equals(namespaceName)).findAny().orElseGet(() -> {
                DecodeNamespace newNamespace = SimpleDecodeNamespace.newInstance(
                        ImmutableDecodeName.newInstanceFromMangledName(namespaceName),
                        Optional.ofNullable(parentNamespace));
                if (parentNamespace != null)
                {
                    parentNamespace.getSubNamespaces().add(newNamespace);
                }
                else
                {
                    namespaces.add(newNamespace);
                }
                return newNamespace;
            });
        }
        //noinspection ConstantConditions
        return namespace;
    }

    @NotNull
    public static Optional<DecodeNamespace> getNamespaceByFqn(@NotNull DecodeRegistry registry,
                                                             @NotNull DecodeFqn namespaceFqn)
    {
        List<DecodeNamespace> namespaces = registry.getRootNamespaces();
        Optional<DecodeNamespace> namespace = Optional.empty();
        for (DecodeName namespaceName : namespaceFqn.getParts())
        {
            namespace = namespaces.stream().filter(ns -> ns.getName().equals(namespaceName)).findAny();
            if (!namespace.isPresent())
            {
                return namespace;
            }
            else
            {
                namespaces = namespace.get().getSubNamespaces();
            }
        }
        return  namespace;
    }

    @NotNull
    public static URI getUriForNamespaceAndName(@NotNull DecodeFqn namespaceFqn, @NotNull DecodeName name)
    {
        List<DecodeName> namespaceNameParts = new ArrayList<>(namespaceFqn.getParts());
        namespaceNameParts.add(name);
        try
        {
            return URI.create("/" + URLEncoder
                    .encode(String.join("/", namespaceNameParts.stream().map(DecodeName::asString).collect(
                            Collectors.toList())), Charsets.UTF_8.name()));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError(e);
        }
    }

    public static List<String> getUriParts(@NotNull URI uri)
    {
        try
        {
            return Lists.newArrayList(
                    URLDecoder.decode(uri.getPath(), Charsets.UTF_8.name()).substring(1).split(Pattern.quote("/")));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError(e);
        }
    }

    @NotNull
    public static List<DecodeNamespace> mergeRootNamespaces(@NotNull List<DecodeNamespace> namespaces)
    {
        List<DecodeNamespace> result = new ArrayList<>();
        namespaces.stream().forEach(n -> mergeNamespaceToNamespacesList(result, n));
        return result;
    }

    private static void mergeNamespaceToNamespacesList(@NotNull List<DecodeNamespace> list,
                                                       @NotNull DecodeNamespace namespace)
    {
        Optional<DecodeNamespace> targetNamespace = list.stream().filter(n -> n.getName().equals(namespace.getName())).findAny();
        if (targetNamespace.isPresent())
        {
            mergeNamespaceTo(targetNamespace.get(), namespace);
        }
        else
        {
            list.add(namespace);
        }
    }

    private static void mergeNamespaceTo(@NotNull DecodeNamespace targetNamespace, @NotNull DecodeNamespace namespace)
    {
        List<DecodeNamespace> subNamespaces = targetNamespace.getSubNamespaces();
        namespace.getSubNamespaces().stream().forEach(n -> {
            mergeNamespaceToNamespacesList(subNamespaces, n);
            n.setParent(targetNamespace);
        });

        List<DecodeUnit> units = targetNamespace.getUnits();
        namespace.getUnits().stream().forEach(u ->
        {
            DecodeName name = u.getName();
            Preconditions.checkState(units.stream().noneMatch(u2 -> u2.getName().equals(name)),
                    "unit name collision '%s'", name);
            u.setNamespace(targetNamespace);
        });
        units.addAll(namespace.getUnits());

        List<DecodeType> types = targetNamespace.getTypes();
        namespace.getTypes().stream().forEach(t ->
        {
            Optional<DecodeName> name = t.getOptionalName();
            Preconditions.checkState(types.stream().noneMatch(t2 -> t2.getOptionalName().equals(name)),
                    "type name collision '%s'", name);
            t.setNamespace(targetNamespace);
        });
        types.addAll(namespace.getTypes());

        List<DecodeComponent> components = targetNamespace.getComponents();
        namespace.getComponents().stream().forEach(c ->
        {
            DecodeName name = c.getName();
            Preconditions.checkState(components.stream().noneMatch(c2 -> c2.getName().equals(name)),
                    "component name collision '%s'", name);
            c.setNamespace(targetNamespace);
        });
        components.addAll(namespace.getComponents());
    }

    @NotNull
    public static DecodeNamespace newRootDecodeNamespaceForFqn(@NotNull DecodeFqn namespaceFqn)
    {
        DecodeNamespace namespace = SimpleDecodeNamespace.newInstance(namespaceFqn.getLast(),
                Optional.<DecodeNamespace>empty());
        List<DecodeName> parts = namespaceFqn.getParts();
        for (int i = parts.size() - 2; i >= 0; i--)
        {
            DecodeNamespace parentNamespace = SimpleDecodeNamespace.newInstance(parts.get(i),
                    Optional.<DecodeNamespace>empty());
            namespace.setParent(parentNamespace);
            parentNamespace.getSubNamespaces().add(namespace);
            namespace = parentNamespace;
        }
        return namespace;
    }

    @NotNull
    public static DecodeNamespace newNamespaceForFqn(@NotNull DecodeFqn fqn)
    {
        DecodeNamespace currentNamespace = null;
        for (DecodeName name : fqn.getParts())
        {
            currentNamespace = SimpleDecodeNamespace.newInstance(name, Optional.ofNullable(currentNamespace));
            if (currentNamespace.getParent().isPresent())
            {
                currentNamespace.getParent().get().getSubNamespaces().add(currentNamespace);
            }
        }
        return Preconditions.checkNotNull(currentNamespace);
    }

    @NotNull
    public static DecodeFqn getNamespaceFqnFromUri(@NotNull URI uri)
    {
        List<String> uriParts = getUriParts(uri);
        return ImmutableDecodeFqn.newInstance(uriParts.stream().limit(uriParts.size() - 1)
                .map(ImmutableDecodeName::newInstanceFromMangledName).collect(Collectors.toList()));
    }

    @NotNull
    public static URI getUriForSourceTypeString(@NotNull String typeString)
    {
        typeString = normalizeSourceTypeString(typeString);
        typeString = processQuestionMarks(typeString);
        int genericStartIndex = typeString.indexOf('<');
        if (genericStartIndex != -1)
        {
            DecodeFqn namespaceFqn = ImmutableDecodeFqn.newInstanceFromSource(typeString.substring(0, genericStartIndex));
            return getUriForTypeNamespaceNameGenericArguments(namespaceFqn.copyDropLast(), namespaceFqn.getLast(),
                    typeString.substring(genericStartIndex));
        }
        DecodeFqn namespaceFqn = ImmutableDecodeFqn.newInstanceFromSource(typeString);
        return getUriForNamespaceAndName(namespaceFqn.copyDropLast(), namespaceFqn.getLast());
    }

    @NotNull
    private static URI getUriForTypeNamespaceNameGenericArguments(@NotNull DecodeFqn namespaceFqn,
                                                                  @NotNull DecodeName typeName,
                                                                  @NotNull String typeGenericArguments)
    {
        List<DecodeName> namespaceNameParts = new ArrayList<>(namespaceFqn.getParts());
        namespaceNameParts.add(typeName);
        try
        {
            return URI.create("/" + URLEncoder
                    .encode(String.join("/", namespaceNameParts.stream().map(DecodeName::asString).collect(
                            Collectors.toList()) + typeGenericArguments), Charsets.UTF_8.name()));
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
            typeString = DecodeConstants.SYSTEM_NAMESPACE_NAME.asString() + ".optional<"
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
}
