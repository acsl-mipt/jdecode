package ru.mipt.acsl.decode.generator.json;

import ru.mipt.acsl.decode.model.registry.Registry;

import java.io.OutputStream;
import java.util.List;

/**
 * Created by metadeus on 08.06.16.
 */
public class DecodeJsonGeneratorConfig {

    private final Registry registry;
    private final OutputStream output;
    private final List<String> componentsFqn;
    private final boolean prettyPrint;

    public static DecodeJsonGeneratorConfig newInstance(Registry registry, OutputStream output, List<String> componentsFqn) {
        return newInstance(registry, output, componentsFqn, false);
    }

    public static DecodeJsonGeneratorConfig newInstance(Registry registry, OutputStream output, List<String> componentsFqn,
                                                        boolean prettyPrint) {
        return new DecodeJsonGeneratorConfig(registry, output, componentsFqn, prettyPrint);
    }

    public Registry getRegistry() {
        return registry;
    }

    public OutputStream getOutput() {
        return output;
    }

    public List<String> getComponentsFqn() {
        return componentsFqn;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    private DecodeJsonGeneratorConfig(Registry registry, OutputStream output, List<String> componentsFqn,
                                      boolean prettyPrint) {
        this.registry = registry;
        this.output = output;
        this.componentsFqn = componentsFqn;
        this.prettyPrint = prettyPrint;
    }


}
