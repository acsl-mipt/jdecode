package ru.mipt.acsl.decode.mavlink.generator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Created by metadeus on 08.06.16.
 */
public class MavlinkSourceGeneratorInternalConfig implements MavlinkSourceGeneratorConfig {

    private final String inputContents;
    private final String nsFqn;
    private final String componentName;
    private final ByteArrayOutputStream outputStream;
    private final Map<String, String> includes;

    @Override
    public String getInputContents() {
        return inputContents;
    }

    @Override
    public String getNamespaceFqn() {
        return nsFqn;
    }

    @Override
    public String getComponentName() {
        return componentName;
    }

    public static MavlinkSourceGeneratorInternalConfig newInstance(String inputContents, String nsFqn,
                                                                   String componentName,
                                                                   ByteArrayOutputStream outputStream,
                                                                   Map<String, String> includes) {
        return new MavlinkSourceGeneratorInternalConfig(inputContents, nsFqn, componentName, outputStream, includes);
    }

    private MavlinkSourceGeneratorInternalConfig(String inputContents, String nsFqn, String componentName,
                                                 ByteArrayOutputStream outputStream, Map<String, String> includes) {
        this.inputContents = inputContents;
        this.nsFqn = nsFqn;
        this.componentName = componentName;
        this.outputStream = outputStream;
        this.includes = includes;
    }

    @Override
    public String getIncludeContents(String fileName) {
        return includes.get(fileName);
    }

    @Override
    public void writeOutput(String contents) {
        try {
            outputStream.write(contents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
