// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static ru.mipt.acsl.decode.parser.psi.DecodeTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import ru.mipt.acsl.decode.parser.psi.*;

public class DecodeCommandArgsImpl extends ASTWrapperPsiElement implements DecodeCommandArgs {

  public DecodeCommandArgsImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) ((DecodeVisitor)visitor).visitCommandArgs(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<DecodeCommandArg> getCommandArgList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, DecodeCommandArg.class);
  }

}
