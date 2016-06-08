package ru.mipt.acsl.decode.mavlink.generator;

/**
 * Created by metadeus on 08.06.16.
 */
public interface MavlinkSourceGeneratorConfig {

    String getIncludeContents(String fileName);

    String getInputContents();

    void writeOutput(String contents);

    String getNamespaceFqn();

    String getComponentName();

}
