package ru.mipt.acsl.decode.idea.plugin;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.parser.DecodeLanguage;
import ru.mipt.acsl.decode.parser.psi.DecodeTypes;

/**
 * @author Artem Shein
 */
public class DecodeCompletionContributor extends CompletionContributor
{
    public DecodeCompletionContributor()
    {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(DecodeTypes.ELEMENT_NAME_RULE).withLanguage(DecodeLanguage.INSTANCE),
                new CompletionProvider<CompletionParameters>()
                {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                                  @NotNull CompletionResultSet result)
                    {
//                        PsiActionSupportFactory.PsiElementSelector
                        System.out.println(parameters.getPosition().getParent().getNode());
                        //result.addElement(LookupElementBuilder.create("balalaika"));
                    }
                });
    }
}
