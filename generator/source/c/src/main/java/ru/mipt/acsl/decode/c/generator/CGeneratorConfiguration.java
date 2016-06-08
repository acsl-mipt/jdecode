package ru.mipt.acsl.decode.c.generator;

import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.model.registry.Registry;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by metadeus on 08.06.16.
 */
public class CGeneratorConfiguration {

    private final File outputDir;
    private final Registry registry;
    private final String rootComponentFqn;
    private final Map<Fqn, Fqn> namespaceAliases;
    private final List<GeneratorSource> sources;
    private final FileGeneratorConfiguration prologue;
    private final FileGeneratorConfiguration epilogue;
    private final boolean isSingleton;
    private final boolean includeModelInfo;

    public static CGeneratorConfiguration newInstance(File outputDir, Registry registry, String rootComponentFqn,
                                                      Map<Fqn, Fqn> namespaceAliases,
                                                      List<GeneratorSource> sources) {
        return newInstance(outputDir, registry, rootComponentFqn, namespaceAliases, sources,
                FileGeneratorConfiguration.newInstance());
    }

    public static CGeneratorConfiguration newInstance(File outputDir, Registry registry, String rootComponentFqn,
                                                      Map<Fqn, Fqn> namespaceAliases,
                                                      List<GeneratorSource> sources,
                                                      FileGeneratorConfiguration prologue) {
        return newInstance(outputDir, registry, rootComponentFqn, namespaceAliases, sources, prologue,
                FileGeneratorConfiguration.newInstance());
    }

    public static CGeneratorConfiguration newInstance(File outputDir, Registry registry, String rootComponentFqn,
                                                      Map<Fqn, Fqn> namespaceAliases,
                                                      List<GeneratorSource> sources,
                                                      FileGeneratorConfiguration prologue,
                                                      FileGeneratorConfiguration epilogue) {
        return newInstance(outputDir, registry, rootComponentFqn, namespaceAliases, sources, prologue, epilogue, false);
    }

    public static CGeneratorConfiguration newInstance(File outputDir, Registry registry, String rootComponentFqn,
                                                      Map<Fqn, Fqn> namespaceAliases,
                                                      List<GeneratorSource> sources,
                                                      FileGeneratorConfiguration prologue,
                                                      FileGeneratorConfiguration epilogue,
                                                      boolean isSingleton) {
        return newInstance(outputDir, registry, rootComponentFqn, namespaceAliases, sources, prologue, epilogue,
                isSingleton, false);
    }

    public static CGeneratorConfiguration newInstance(File outputDir, Registry registry, String rootComponentFqn,
                                                      Map<Fqn, Fqn> namespaceAliases,
                                                      List<GeneratorSource> sources,
                                                      FileGeneratorConfiguration prologue,
                                                      FileGeneratorConfiguration epilogue,
                                                      boolean isSingleton,
                                                      boolean includeModelInfo) {
        return new CGeneratorConfiguration(outputDir, registry, rootComponentFqn, namespaceAliases, sources, prologue,
                epilogue, isSingleton, includeModelInfo);
    }

    public File outputDir() {
        return outputDir;
    }

    public Registry getRegistry() {
        return registry;
    }

    public String getRootComponentFqn() {
        return rootComponentFqn;
    }

    public Map<Fqn, Fqn> getNamespaceAliases() {
        return namespaceAliases;
    }

    public List<GeneratorSource> getSources() {
        return sources;
    }

    public FileGeneratorConfiguration getPrologue() {
        return prologue;
    }

    public FileGeneratorConfiguration getEpilogue() {
        return epilogue;
    }

    public boolean isSingleton() {
        return isSingleton;
    }

    public boolean isIncludeModelInfo() {
        return includeModelInfo;
    }

    private CGeneratorConfiguration(File outputDir, Registry registry, String rootComponentFqn,
                                    Map<Fqn, Fqn> namespaceAliases,
                                    List<GeneratorSource> sources,
                                    FileGeneratorConfiguration prologue,
                                    FileGeneratorConfiguration epilogue,
                                    boolean isSingleton,
                                    boolean includeModelInfo) {
        this.outputDir = outputDir;
        this.registry = registry;
        this.rootComponentFqn = rootComponentFqn;
        this.namespaceAliases = namespaceAliases;
        this.sources = sources;
        this.prologue = prologue;
        this.epilogue = epilogue;
        this.isSingleton = isSingleton;
        this.includeModelInfo = includeModelInfo;
    }
}
