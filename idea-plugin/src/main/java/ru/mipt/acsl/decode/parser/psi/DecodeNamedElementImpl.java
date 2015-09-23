package ru.mipt.acsl.decode.parser.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public abstract class DecodeNamedElementImpl extends ASTWrapperPsiElement implements DecodeNamedElement
{
    public DecodeNamedElementImpl(@NotNull ASTNode node)
    {
        super(node);
    }
}
