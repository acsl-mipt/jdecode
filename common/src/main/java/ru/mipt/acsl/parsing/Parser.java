package ru.mipt.acsl.parsing;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * @author Artem Shein
 */
public interface Parser<T>
{
    /**
     * @throws ParsingException if parsing fails
     * @param is input stream to parse
     * @return parsing result
     */
    @NotNull
    T parse(@NotNull InputStream is);

    @NotNull
    default T parse(@NotNull File inputFile)
    {
        try (InputStream is = FileUtils.openInputStream(inputFile))
        {
            return parse(is);
        }
        catch (IOException e)
        {
            throw new ParsingException(e);
        }
    }

    @NotNull
    default T parse(@NotNull String str)
    {
        try (StringReader reader = new StringReader(str))
        {
            try (InputStream is = new ReaderInputStream(reader))
            {
                return parse(is);
            }
            catch (IOException e)
            {
                throw new ParsingException(e);
            }
        }
    }
}
