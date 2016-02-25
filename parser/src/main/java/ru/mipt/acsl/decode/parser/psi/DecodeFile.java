package ru.mipt.acsl.decode.parser.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.parser.DecodeFileType;
import ru.mipt.acsl.decode.parser.DecodeLanguage;

/**
 * @author Artem Shein
 */
public class DecodeFile extends PsiFileBase
{
    public DecodeFile(FileViewProvider viewProvider) {
        super(viewProvider, DecodeLanguage.instance());
    }

    @Override
    @NotNull
    public  FileType getFileType() { return DecodeFileType.instance(); }

    @Override
    public String toString() { return "Device components definition file"; }
}
