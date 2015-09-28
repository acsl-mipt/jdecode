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

public class DecodeComponentBaseTypeDeclImpl extends ASTWrapperPsiElement implements DecodeComponentBaseTypeDecl {

  public DecodeComponentBaseTypeDeclImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) ((DecodeVisitor)visitor).visitComponentBaseTypeDecl(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public DecodeTypeDeclBody getTypeDeclBody() {
    return findNotNullChildByClass(DecodeTypeDeclBody.class);
  }

}
