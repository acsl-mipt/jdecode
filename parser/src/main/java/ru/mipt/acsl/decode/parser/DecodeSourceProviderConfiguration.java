package ru.mipt.acsl.decode.parser;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class DecodeSourceProviderConfiguration
{
    @NotNull
    private String resourcePath;

    @NotNull
    public String getResourcePath()
    {
        return "ru/mipt/acsl/decode";
    }

    public void setResourcePath(@NotNull String resourcePath)
    {
        this.resourcePath = resourcePath;
    }
}
