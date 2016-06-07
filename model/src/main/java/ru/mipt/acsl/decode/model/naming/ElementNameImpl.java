package ru.mipt.acsl.decode.model.naming;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ElementName))
            return false;
        ElementName en = (ElementName) o;
        return mangledName.equals(en.mangledNameString());
    }

    @Override
    public int hashCode() {
        return mangledName.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s{%s}", getClass().getSimpleName(), mangledNameString());
    }

}
