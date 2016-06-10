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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class GenerateMccStuff {

    public static final Fqn PixhawkComponentFqn = Fqn.newInstance("mavlink.Pixhawk");

    public static final String PixhawkSourceResource = "pixhawk/pixhawk.xml";
    public static final List<String> PixhawkIncludes = Lists.newArrayList("pixhawk/common.xml");

    public static final String ModelFilePath = "src/mcc/core/db/db/model.json";

    public static void main(String[] args) {

        try {
            new FileOutputStream(new File(ModelFilePath)) {{

                ByteArrayOutputStream pixhawkOutput = new ByteArrayOutputStream();

                Map<String, String> filesContents = new HashMap<>();
                PixhawkIncludes.forEach(i -> filesContents.put(new File(i).getName(), resourceContents(i)));

                MavlinkSourceGenerator.apply(MavlinkSourceGeneratorInternalConfig.newInstance(
                        resourceContents(PixhawkSourceResource),
                        PixhawkComponentFqn.copyDropLast().mangledNameString(),
                        PixhawkComponentFqn.last().mangledNameString(),
                        pixhawkOutput, filesContents)).generate();

                List<String> contents = OnBoardModelRegistry.Sources.ALL.stream()
                        .map(ModelRegistry::sourceContents)
                        .collect(Collectors.toList());
                contents.add(new String(pixhawkOutput.toByteArray(), StandardCharsets.UTF_8));

                DecodeJsonGenerator.newInstance(DecodeJsonGeneratorConfig.newInstance(
                        ModelRegistry.registry(contents), this,
                        Lists.newArrayList(OnBoardCSourceGenerator.ROOT_COMPONENT_FQN_STRING,
                                PixhawkComponentFqn.mangledNameString()),
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
