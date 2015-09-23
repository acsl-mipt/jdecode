package ru.mipt.acsl.decode.idea.plugin;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class DecodeSyntaxHighlighterFactory extends SyntaxHighlighterFactory
{
    @NotNull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile)
    {
        return new DecodeSyntaxHighlighter();
    }
}
