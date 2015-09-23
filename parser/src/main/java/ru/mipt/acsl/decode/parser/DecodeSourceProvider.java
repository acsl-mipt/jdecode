package ru.mipt.acsl.decode.parser;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mipt.acsl.decode.model.domain.DecodeElement;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.DecodeRegistry;
import ru.mipt.acsl.decode.model.domain.impl.DecodeUtils;
import ru.mipt.acsl.decode.model.domain.impl.SimpleDecodeRegistry;
import ru.mipt.acsl.parsing.ParsingException;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class DecodeSourceProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(DecodeSourceProvider.class);

    @NotNull
    public DecodeRegistry provide(@NotNull DecodeSourceProviderConfiguration config)
    {
        String resourcePath = config.getResourcePath();
        final DecodeParser parser = new DecodeParser();
        try
        {
            DecodeRegistry registry = SimpleDecodeRegistry.newInstance();
            registry
                    .getRootNamespaces().addAll(
                    DecodeUtils.mergeRootNamespaces(
                            Resources.readLines(Resources.getResource(resourcePath), Charsets.UTF_8)
                                    .stream().filter((name) -> name.endsWith(".decode")).map(
                                    (name) -> {
                                        try
                                        {
                                            LOG.debug("Parsing resource '{}'", resourcePath + "/" + name);
                                            DecodeElement element = parser.parse(Resources
                                                    .toString(Resources.getResource(resourcePath + "/" + name),
                                                            Charsets.UTF_8));
                                            Preconditions.checkState(element instanceof DecodeNamespace,
                                                    "must be an instance of DecodeNamespace");
                                            return (DecodeNamespace) element;
                                        }
                                        catch (IOException e)
                                        {
                                            throw new ParsingException(e);
                                        }
                                    }).collect(Collectors.toList())));
            return registry;
        }
        catch (IOException e)
        {
            throw new ParsingException(e);
        }
    }
}
