package ru.mipt.acsl.decode.model.proxy.path;

import ru.mipt.acsl.decode.model.naming.ElementName;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class GenericTypeName extends ProxyElementName {

    private final ElementName typeName;
    private final List<ProxyPath> genericArgumentPaths;

    public static GenericTypeName newInstance(ElementName typeName, List<ProxyPath> genericArgumentPaths) {
        return new GenericTypeName(typeName, genericArgumentPaths);
    }

    public ElementName typeName() {
        return typeName;
    }

    public List<ProxyPath> genericArgumentPaths() {
        return genericArgumentPaths;
    }

    private GenericTypeName(ElementName typeName, List<ProxyPath> genericArgumentPaths) {
        this.typeName = typeName;
        this.genericArgumentPaths = genericArgumentPaths;
    }

    @Override
    public ElementName mangledName() {
        return ElementName.newInstanceFromMangledName(typeName.mangledNameString() + "[" +
                genericArgumentPaths.stream()
                        .map(p -> p.mangledName().mangledNameString()).collect(Collectors.joining(",")) + "]");
    }

}
