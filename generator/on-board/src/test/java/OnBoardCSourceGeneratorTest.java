import com.google.common.io.Resources;
import ru.mipt.acsl.decode.c.generator.CGeneratorConfiguration;
import ru.mipt.acsl.decode.c.generator.CSourceGenerator;
import ru.mipt.acsl.decode.c.generator.FileGeneratorConfiguration;
import ru.mipt.acsl.decode.c.generator.GeneratorSource;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.parser.ModelRegistry;
import ru.mipt.acsl.geotarget.OnBoardModelRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @author Artem Shein
 */
class OnBoardCSourceGeneratorTest {

    static Fqn fqn(String str) {
        return Fqn.newInstance(str);
    }

    public void testGenerator() {
        String prologuePath = "photon/prologue.h";

        try {
            CGeneratorConfiguration config = CGeneratorConfiguration.newInstance(new File("target/gen/"),
                    OnBoardModelRegistry.registry(),
                    "test.TestComp",
                    new HashMap<Fqn, Fqn>() {{
                        put(fqn("decode"), fqn("photon.decode"));
                        put(fqn("ru.mipt.acsl.photon"), fqn("photon"));
                        put(fqn("ru.mipt.acsl.foundation"), fqn("photon.foundation"));
                        put(fqn("ru.mipt.acsl.fs"), fqn("photon.fs"));
                        put(fqn("ru.mipt.acsl.identification"), fqn("photon.identification"));
                        put(fqn("ru.mipt.acsl.mcc"), fqn("photon"));
                        put(fqn("ru.mipt.acsl.routing"), fqn("photon.routing"));
                        put(fqn("ru.mipt.acsl.scripting"), fqn("photon.scripting"));
                        put(fqn("ru.mipt.acsl.segmentation"), fqn("photon.segmentation"));
                        put(fqn("ru.mipt.acsl.tm"), fqn("photon.tm"));
                    }},
                    OnBoardModelRegistry.Sources.ALL.stream().map(source ->
                            GeneratorSource.newInstance(ModelRegistry.sourceName(source),
                                    ModelRegistry.sourceContents(source))).collect(Collectors.toList()),
                    FileGeneratorConfiguration.newInstance(true, prologuePath,
                            Resources.toString(Resources.getResource(prologuePath), StandardCharsets.UTF_8)),
                    FileGeneratorConfiguration.newInstance(),
                    true);
            new CSourceGenerator(config).generate();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
