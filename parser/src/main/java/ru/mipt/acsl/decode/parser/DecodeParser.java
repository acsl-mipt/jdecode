package ru.mipt.acsl.decode.parser;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;
import ru.mipt.acsl.decode.model.domain.DecodeElement;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.parsing.Parser;
import ru.mipt.acsl.parsing.ParsingException;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class DecodeParser implements Parser<DecodeNamespace>
{
    @NotNull
    public DecodeNamespace parse(@NotNull InputStream is)
    {
        DecodeParboiledParser parser = Parboiled.createParser(DecodeParboiledParser.class);
        try
        {
            ParsingResult<DecodeNamespace> result = new RecoveringParseRunner<DecodeNamespace>(parser.File()).run(
                    IOUtils.toString(is, Charsets.UTF_8));
            if (!result.matched || result.hasErrors())
            {
                throw new ParsingException(result.parseErrors.stream().map(ParboiledErrorToStringWrapper::new).collect(
                        Collectors.toList()).toString());
            }
            return Preconditions.checkNotNull(result.resultValue);
        }
        catch (IOException e)
        {
            throw new ParsingException(e);
        }
    }
}
