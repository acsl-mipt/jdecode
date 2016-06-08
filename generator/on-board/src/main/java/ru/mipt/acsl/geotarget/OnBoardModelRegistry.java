package ru.mipt.acsl.geotarget;

import com.google.common.collect.Lists;
import ru.mipt.acsl.decode.model.registry.Registry;
import ru.mipt.acsl.decode.parser.ModelRegistry;
import ru.mipt.acsl.decode.parser.SourceFileName;

import java.util.List;

/**
 * Created by metadeus on 08.06.16.
 */
public class OnBoardModelRegistry {

    public static class Sources {

        public static final SourceFileName FOUNDATION = SourceFileName.newInstance("foundation");
        public static final SourceFileName FS = SourceFileName.newInstance("fs");
        public static final SourceFileName IDENTIFICATION = SourceFileName.newInstance("identification");
        public static final SourceFileName MCC = SourceFileName.newInstance("mcc");
        public static final SourceFileName PHOTON = SourceFileName.newInstance("photon");
        public static final SourceFileName SCRIPTING = SourceFileName.newInstance("scripting");
        public static final SourceFileName SEGMENTATION = SourceFileName.newInstance("segmentation");
        public static final SourceFileName TM = SourceFileName.newInstance("tm");
        public static final SourceFileName ROUTING = SourceFileName.newInstance("routing");

        public static final List<SourceFileName> ALL = Lists.newArrayList(FOUNDATION, FS,
                IDENTIFICATION, MCC, PHOTON, SCRIPTING, SEGMENTATION, TM, ROUTING);

        static {
            ALL.addAll(ModelRegistry.Sources.ALL);
        }

        // prevent from instantiation
        private Sources() {

        }

    }

    public static Registry registry() {
        return ModelRegistry.registryForSourceNames(Sources.ALL);
    }

    // prevent from instantiation
    private OnBoardModelRegistry() {

    }

}
