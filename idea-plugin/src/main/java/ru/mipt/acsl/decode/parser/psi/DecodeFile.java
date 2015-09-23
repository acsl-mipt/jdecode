package ru.mipt.acsl.decode.parser.psi;

import ru.mipt.acsl.decode.parser.DecodeFileType;
import ru.mipt.acsl.decode.parser.DecodeLanguage;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class DecodeFile extends PsiFileBase
{
    public DecodeFile(@NotNull FileViewProvider viewProvider)
    {
        super(viewProvider, DecodeLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType()
    {
        return DecodeFileType.INSTANCE;
    }

    @Override
    public String toString()
    {
        return "Device interface definition file";
    }

}
