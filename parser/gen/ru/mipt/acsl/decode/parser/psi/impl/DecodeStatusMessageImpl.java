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

public class DecodeStatusMessageImpl extends ASTWrapperPsiElement implements DecodeStatusMessage {

  public DecodeStatusMessageImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) ((DecodeVisitor)visitor).visitStatusMessage(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public DecodeElementNameRule getElementNameRule() {
    return findNotNullChildByClass(DecodeElementNameRule.class);
  }

  @Override
  @Nullable
  public DecodeEntityId getEntityId() {
    return findChildByClass(DecodeEntityId.class);
  }

  @Override
  @Nullable
  public DecodeInfoString getInfoString() {
    return findChildByClass(DecodeInfoString.class);
  }

  @Override
  @NotNull
  public DecodeStatusMessageParametersDecl getStatusMessageParametersDecl() {
    return findNotNullChildByClass(DecodeStatusMessageParametersDecl.class);
  }

  @Override
  @Nullable
  public PsiElement getNonNegativeNumber() {
    return findChildByType(NON_NEGATIVE_NUMBER);
  }

}
