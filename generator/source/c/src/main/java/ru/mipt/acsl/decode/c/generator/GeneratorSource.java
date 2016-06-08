package ru.mipt.acsl.decode.c.generator;

/**
 * Created by metadeus on 08.06.16.
 */
public class GeneratorSource {

    private final String name;
    private final String contents;

    public static GeneratorSource newInstance(String name, String contents) {
        return new GeneratorSource(name, contents);
    }

    public String getName() {
        return name;
    }

    public String getContents() {
        return contents;
    }

    private GeneratorSource(String name, String contents) {
        this.name = name;
        this.contents = contents;
    }
}
