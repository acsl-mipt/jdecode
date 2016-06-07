package ru.mipt.acsl.decode.model.naming;

import java.util.List;

/**
 * Created by metadeus on 06.06.16.
 */
public class FqnImpl implements Fqn {

    private final List<ElementName> parts;

    @Override
    public List<ElementName> getParts() {
        return parts;
    }

    public Fqn copyDropLast() {
        return new FqnImpl(parts.subList(0, parts.size() - 1));
    }

    FqnImpl(List<ElementName> parts) {
        this.parts = parts;
    }

}
