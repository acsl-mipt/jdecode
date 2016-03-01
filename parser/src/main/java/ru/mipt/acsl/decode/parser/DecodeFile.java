package ru.mipt.acsl.decode.parser;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Created by metadeus on 02.03.16.
 */
public class DecodeFile extends PsiFileBase {
    public DecodeFile(FileViewProvider viewProvider) {
        super(viewProvider, DecodeLanguage.INSTANCE);
    }

    @Override
    @NotNull
    public FileType getFileType() {
        return DecodeFileType$.MODULE$;
    }

    @Override
    public String toString() {
        return "Device components definition file";
    }
}