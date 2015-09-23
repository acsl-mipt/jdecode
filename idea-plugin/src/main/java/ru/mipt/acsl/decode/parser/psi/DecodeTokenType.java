package ru.mipt.acsl.decode.parser.psi;

import ru.mipt.acsl.decode.parser.DecodeLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class DecodeTokenType extends IElementType
{
    public DecodeTokenType(@NotNull String debugName)
    {
        super(debugName, DecodeLanguage.INSTANCE);
    }

    @Override
    public String toString()
    {
        return "DecodeTokenType." + super.toString();
    }
}
