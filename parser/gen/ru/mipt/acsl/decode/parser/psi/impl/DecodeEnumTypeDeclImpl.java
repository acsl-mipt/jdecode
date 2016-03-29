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

public class DecodeEnumTypeDeclImpl extends ASTWrapperPsiElement implements DecodeEnumTypeDecl {

  public DecodeEnumTypeDeclImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull DecodeVisitor visitor) {
    visitor.visitEnumTypeDecl(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) accept((DecodeVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public DecodeElementNameRule getElementNameRule() {
    return findChildByClass(DecodeElementNameRule.class);
  }

  @Override
  @NotNull
  public DecodeEnumTypeValues getEnumTypeValues() {
    return findNotNullChildByClass(DecodeEnumTypeValues.class);
  }

  @Override
  @Nullable
  public DecodeFinalEnum getFinalEnum() {
    return findChildByClass(DecodeFinalEnum.class);
  }

  @Override
  @Nullable
  public DecodeTypeApplication getTypeApplication() {
    return findChildByClass(DecodeTypeApplication.class);
  }

}
