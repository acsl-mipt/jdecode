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
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.cli.*;
import java.nio.charset.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
/**
 * @author Artem Shein
 */
public class GenerateMccStuff {

    public static final Fqn MAVLINK_COMPONENT_FQN = Fqn.newInstance("mavlink.Common");

    public static Options constructPosixOptions()
    {
        final Options posixOptions = new Options();
        posixOptions.addOption("i", "input", true, "common.xml path")
                    .addOption("o", "output", true, "mcc directory path");

        return posixOptions;
    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static List<String> getIncludes(String xmlPath)
    {
        List<String> includes = new ArrayList<String>();
        try {
            File fXmlFile = new File(xmlPath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            String includeNode = doc.getDocumentElement().getElementsByTagName("include").item(0).getTextContent();

            String sourceDir = fXmlFile.getParentFile().getAbsolutePath();
            File f2 = new File(sourceDir, includeNode);

            includes.add(f2.getAbsolutePath());
            return includes;
        }
        catch(Exception e)
        {
            return new ArrayList<String>();
        }
    }

    public static void main(String[] args) {
        final CommandLineParser cmdLinePosixParser = new DefaultParser();
        final Options posixOptions = constructPosixOptions();
        CommandLine commandLine;

        String MAVLINK_SOURCE_RESOURCE = "mavlink/common.xml";

        String MCC_DECODE_SOURCES_PATH = "src/mcc/core/db/db/sources/";
        String MAVLINK_FILE_PATH = MCC_DECODE_SOURCES_PATH + "mavlink.decode";
        String MODEL_FILE_PATH = "src/mcc/core/db/db/model.json";

        try
        {
            commandLine = cmdLinePosixParser.parse(posixOptions, args);
            MAVLINK_SOURCE_RESOURCE = commandLine.getParsedOptionValue("input").toString();
            String mccPath = commandLine.getParsedOptionValue("output").toString();
            MCC_DECODE_SOURCES_PATH = mccPath + "/src/mcc/core/db/db/sources/";
            MAVLINK_FILE_PATH = MCC_DECODE_SOURCES_PATH + "mavlink.decode";
            MODEL_FILE_PATH = mccPath + "/src/mcc/core/db/db/model.json";
        }
        catch (ParseException parseException)  // checked exception
        {
            System.err.println(
                    "Encountered exception while parsing using PosixParser:\n"
                            + parseException.getMessage() );
        }
        try {
            String finalMAVLINK_SOURCE_RESOURCE = MAVLINK_SOURCE_RESOURCE;
            String finalMCC_DECODE_SOURCES_PATH = MCC_DECODE_SOURCES_PATH;
            String finalMAVLINK_FILE_PATH = MAVLINK_FILE_PATH;

            List<String> MAVLINK_INCLUDES = getIncludes(finalMAVLINK_SOURCE_RESOURCE);
            new FileOutputStream(new File(MODEL_FILE_PATH)) {{

                ByteArrayOutputStream mavlinkOutput = new ByteArrayOutputStream();

                Map<String, String> filesContents = new HashMap<>();
                for(String s : MAVLINK_INCLUDES)
                {
                    filesContents.put(new File(s).getName(), readFile(s, StandardCharsets.UTF_8));
                }

                // Generate Decode sources for Mavlink
                MavlinkSourceGenerator.apply(MavlinkSourceGeneratorInternalConfig.newInstance(
                        readFile(finalMAVLINK_SOURCE_RESOURCE, StandardCharsets.UTF_8),
                        MAVLINK_COMPONENT_FQN.copyDropLast().mangledNameString(),
                        MAVLINK_COMPONENT_FQN.last().mangledNameString(),
                        mavlinkOutput, filesContents)).generate();

                // Copy Decode sources to MCC
                OnBoardModelRegistry.Sources.ALL.forEach(sourceFileName -> {
                    try
                    {
                        new FileOutputStream(
                                new File(finalMCC_DECODE_SOURCES_PATH + ModelRegistry.sourceName(sourceFileName)))
                        {{
                            write(ModelRegistry.sourceContents(sourceFileName).getBytes());
                            close();
                        }};
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                });

                // Read Decode sources
                List<String> contents = OnBoardModelRegistry.Sources.ALL.stream()
                        .map(ModelRegistry::sourceContents)
                        .collect(Collectors.toList());
                byte[] mavlinkSourceContents = mavlinkOutput.toByteArray();
                contents.add(new String(mavlinkSourceContents, StandardCharsets.UTF_8));

                // Copy Mavlink sources to MCC
                File mavlinkSource = new File(finalMAVLINK_FILE_PATH);
                mavlinkSource.getParentFile().mkdirs();
                new FileOutputStream(mavlinkSource) {{
                    write(mavlinkSourceContents);
                    close();
                }};

                // Generate JSON
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
