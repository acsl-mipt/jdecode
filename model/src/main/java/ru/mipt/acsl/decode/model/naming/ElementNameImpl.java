package ru.mipt.acsl.decode.model.naming;

import java.util.regex.Pattern;

/**
 * Created by metadeus on 06.06.16.
 */
public class ElementNameImpl implements ElementName {

    private final String mangledName;

    @Override
    public String mangledNameString() {
        return mangledName;
    }

    ElementNameImpl(String mangledName) {
        this.mangledName = mangledName;
    }
}
