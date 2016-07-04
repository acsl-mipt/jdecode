package ru.mipt.acsl.geotarget;

import com.google.common.io.Resources;
import ru.mipt.acsl.decode.c.generator.CGeneratorConfiguration;
import ru.mipt.acsl.decode.c.generator.CSourceGenerator;
import ru.mipt.acsl.decode.c.generator.FileGeneratorConfiguration;
import ru.mipt.acsl.decode.c.generator.GeneratorSource;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.parser.ModelRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
public class OnBoardCSourceGenerator {

    public static final String ROOT_COMPONENT_FQN_STRING = "mcc.photon.Main";
    public static final String PROLOGUE_PATH = "photon/prologue.h";

    public static Fqn fqn(String str) {
        return Fqn.newInstance(str);
    }

    public static void main(String[] args) {
        try {
            CGeneratorConfiguration config = CGeneratorConfiguration.newInstance(new File("gen/"),
                    OnBoardModelRegistry.registry(),
                    ROOT_COMPONENT_FQN_STRING,
                    new HashMap<Fqn, Fqn>() {{
                        put(fqn("decode"), fqn("photon.decode"));
                        put(fqn("mcc.photon"), fqn("photon"));
                        put(fqn("mcc.foundation"), fqn("photon.foundation"));
                        put(fqn("mcc.fs"), fqn("photon.fs"));
                        put(fqn("mcc.identification"), fqn("photon.identification"));
                        put(fqn("mcc.mcc"), fqn("photon"));
                        put(fqn("mcc.routing"), fqn("photon.routing"));
                        put(fqn("mcc.scripting"), fqn("photon.scripting"));
                        put(fqn("mcc.segmentation"), fqn("photon.segmentation"));
                        put(fqn("mcc.tm"), fqn("photon.tm"));
                    }},
                    OnBoardModelRegistry.Sources.ALL.stream()
                            .map(source -> GeneratorSource.newInstance(ModelRegistry.sourceName(source),
                                    ModelRegistry.sourceContents(source))).collect(Collectors.toList()),
                    FileGeneratorConfiguration.newInstance(true, PROLOGUE_PATH,
                            Resources.toString(Resources.getResource(PROLOGUE_PATH), StandardCharsets.UTF_8)),
                    FileGeneratorConfiguration.newInstance(),
                    true, true);
            System.out.println(String.format("Generating on-board sources to %s...",
                    config.outputDir().getAbsolutePath()));
            new CSourceGenerator(config).generate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}