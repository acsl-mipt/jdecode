package ru.mipt.acsl.decode.c.generator;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Created by metadeus on 08.06.16.
 */
public class FileGeneratorConfiguration {

    private final boolean isActive;
    @Nullable
    private final String path;
    @Nullable
    private final String contents;

    public static FileGeneratorConfiguration newInstance() {
        return newInstance(false);
    }

    public static FileGeneratorConfiguration newInstance(boolean isActive) {
        return newInstance(isActive, null);
    }

    public static FileGeneratorConfiguration newInstance(boolean isActive, @Nullable String path) {
        return newInstance(isActive, path, null);
    }

    public static FileGeneratorConfiguration newInstance(boolean isActive, @Nullable String path, @Nullable String contents) {
        return new FileGeneratorConfiguration(isActive, path, contents);
    }

    public boolean isActive() {
        return isActive;
    }

    public Optional<String> getPath() {
        return Optional.ofNullable(path);
    }

    public Optional<String> getContents() {
        return Optional.ofNullable(contents);
    }

    private FileGeneratorConfiguration(boolean isActive, @Nullable String path, @Nullable String contents) {
        this.isActive = isActive;
        this.path = path;
        this.contents = contents;
    }
}
