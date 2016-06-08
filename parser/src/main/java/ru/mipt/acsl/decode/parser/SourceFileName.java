package ru.mipt.acsl.decode.parser;

/**
 * Created by metadeus on 08.06.16.
 */
public class SourceFileName {

    private final String name;

    public static SourceFileName newInstance(String name) {
        return new SourceFileName(name);
    }

    private SourceFileName(String name) {
        this.name = name;
    }

    public String fileNameWithoutExt() {
        return name;
    }

    @Override
    public String toString() {
        return fileNameWithoutExt();
    }

}
