package ru.mipt.acsl.decode.generator.mcc;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import ru.mipt.acsl.decode.generator.json.DecodeJsonGenerator;
import ru.mipt.acsl.decode.generator.json.DecodeJsonGeneratorConfig;
import ru.mipt.acsl.decode.mavlink.generator.MavlinkSourceGenerator;
import ru.mipt.acsl.decode.mavlink.generator.MavlinkSourceGeneratorInternalConfig;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.parser.ModelRegistry;
import ru.mipt.acsl.geotarget.OnBoardCSourceGenerator;
import ru.mipt.acsl.geotarget.OnBoardModelRegistry;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class GenerateMccStuff {

    public static final Fqn MAVLINK_COMPONENT_FQN = Fqn.newInstance("mavlink.Common");

    public static final String MAVLINK_SOURCE_RESOURCE = "mavlink/common.xml";
    public static final List<String> MAVLINK_INCLUDES = new ArrayList<>();

    public static final String MAVLINK_FILE_PATH = "src/mcc/core/db/db/sources/mavlink.decode";
    public static final String MODEL_FILE_PATH = "src/mcc/core/db/db/model.json";

    public static void main(String[] args) {

        try {
            new FileOutputStream(new File(MODEL_FILE_PATH)) {{

                ByteArrayOutputStream pixhawkOutput = new ByteArrayOutputStream();

                Map<String, String> filesContents = new HashMap<>();
                MAVLINK_INCLUDES.forEach(i -> filesContents.put(new File(i).getName(), resourceContents(i)));

                MavlinkSourceGenerator.apply(MavlinkSourceGeneratorInternalConfig.newInstance(
                        resourceContents(MAVLINK_SOURCE_RESOURCE),
                        MAVLINK_COMPONENT_FQN.copyDropLast().mangledNameString(),
                        MAVLINK_COMPONENT_FQN.last().mangledNameString(),
                        pixhawkOutput, filesContents)).generate();

                List<String> contents = OnBoardModelRegistry.Sources.ALL.stream()
                        .map(ModelRegistry::sourceContents)
                        .collect(Collectors.toList());
                byte[] pixhwakSourceContents = pixhawkOutput.toByteArray();
                contents.add(new String(pixhwakSourceContents, StandardCharsets.UTF_8));

                File pixhawkSource = new File(MAVLINK_FILE_PATH);
                pixhawkSource.getParentFile().mkdirs();
                new FileOutputStream(pixhawkSource) {{
                    write(pixhwakSourceContents);
                    close();
                }};

                DecodeJsonGenerator.newInstance(DecodeJsonGeneratorConfig.newInstance(
                        ModelRegistry.registry(contents), this,
                        Lists.newArrayList(OnBoardCSourceGenerator.ROOT_COMPONENT_FQN_STRING,
                                MAVLINK_COMPONENT_FQN.mangledNameString()),
                        true))
                        .generate();

                close();
            }};

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String resourceContents(String path) {
        try {
            return Resources.toString(Resources.getResource(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
