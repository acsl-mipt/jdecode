package ru.mipt.acsl.decode.model.naming;

/**
 * Created by metadeus on 06.06.16.
 */
public interface ElementName {

    static String mangleName(String sourceName) {
        String result = sourceName;
        if (result.startsWith("^")) {
            result = result.substring(1);
        }
        result = result.replaceAll("[ \\\\\\^]", "");
        if (result.isEmpty()) {
            throw new IllegalArgumentException("invalid name");
        }
        return result;
    }

    static ElementName newInstanceFromMangledName(String mangledName) {
        return new ElementNameImpl(mangledName);
    }

    static ElementName newInstanceFromSourceName(String sourceName) {
        return new ElementNameImpl(mangleName(sourceName));
    }

    String mangledNameString();

}
