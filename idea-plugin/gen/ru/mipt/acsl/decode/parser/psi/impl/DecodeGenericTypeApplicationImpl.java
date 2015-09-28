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

public class DecodeGenericTypeApplicationImpl extends ASTWrapperPsiElement implements DecodeGenericTypeApplication {

  public DecodeGenericTypeApplicationImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) ((DecodeVisitor)visitor).visitGenericTypeApplication(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public DecodeElementId getElementId() {
    return findNotNullChildByClass(DecodeElementId.class);
  }

  @Override
  @NotNull
  public List<DecodeTypeUnitApplication> getTypeUnitApplicationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, DecodeTypeUnitApplication.class);
  }

}
