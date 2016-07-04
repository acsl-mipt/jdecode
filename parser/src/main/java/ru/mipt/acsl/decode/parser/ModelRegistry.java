package ru.mipt.acsl.decode.parser;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import ru.mipt.acsl.decode.model.proxy.ResolvingMessages;
import ru.mipt.acsl.decode.model.registry.Registry;
import ru.mipt.acsl.decode.model.registry.RegistryUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class ModelRegistry {

    public static final DecodeSourceProvider Provider = new DecodeSourceProvider();
    public static final DecodeSourceProviderConfiguration Config = new DecodeSourceProviderConfiguration("mcc/decode");

    public static class Sources {

        public static final SourceFileName RUNTIME = SourceFileName.newInstance("runtime");
        public static final List<SourceFileName> ALL = Lists.newArrayList(RUNTIME);

        // prevent from instantiation
        private Sources() {

        }
    }

    public static String sourceName(SourceFileName nameWithoutExt) {
        return nameWithoutExt.fileNameWithoutExt() + ".decode";
    }

    public static String sourceResourcePath(SourceFileName nameWithoutExt) {
        return "mcc/" + sourceName(nameWithoutExt);
    }

    public static String sourceContents(SourceFileName nameWithoutExt) {
        try {
            return Resources.toString(Resources.getResource(sourceResourcePath(nameWithoutExt)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Registry registryForSourceNames(List<SourceFileName> sourceNames) {
        return registry(sourceNames.stream().map(ModelRegistry::sourceContents).collect(Collectors.toList()));
    }

    public static Registry registry(List<String> sources) {
        Registry registry = Provider.provide(Config, sources);
        ResolvingMessages resolvingResult = RegistryUtils.resolve(registry);
        if (resolvingResult.hasError())
            resolvingResult.messages().forEach(msg -> System.out.println(msg.text()));
        return registry;
    }

    // prevent from instantiation
    private ModelRegistry() {

    }

}
