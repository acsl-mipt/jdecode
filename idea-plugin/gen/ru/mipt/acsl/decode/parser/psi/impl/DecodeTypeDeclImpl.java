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

public class DecodeTypeDeclImpl extends ASTWrapperPsiElement implements DecodeTypeDecl {

  public DecodeTypeDeclImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) ((DecodeVisitor)visitor).visitTypeDecl(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public DecodeElementNameRule getElementNameRule() {
    return findNotNullChildByClass(DecodeElementNameRule.class);
  }

  @Override
  @Nullable
  public DecodeInfoString getInfoString() {
    return findChildByClass(DecodeInfoString.class);
  }

  @Override
  @NotNull
  public DecodeTypeDeclBody getTypeDeclBody() {
    return findNotNullChildByClass(DecodeTypeDeclBody.class);
  }

}
