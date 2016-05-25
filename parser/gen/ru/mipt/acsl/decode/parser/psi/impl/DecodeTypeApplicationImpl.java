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

public class DecodeTypeApplicationImpl extends ASTWrapperPsiElement implements DecodeTypeApplication {

  public DecodeTypeApplicationImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull DecodeVisitor visitor) {
    visitor.visitTypeApplication(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) accept((DecodeVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public DecodeElementId getElementId() {
    return findChildByClass(DecodeElementId.class);
  }

  @Override
  @Nullable
  public DecodeGenericArguments getGenericArguments() {
    return findChildByClass(DecodeGenericArguments.class);
  }

  @Override
  @Nullable
  public DecodeRangeDecl getRangeDecl() {
    return findChildByClass(DecodeRangeDecl.class);
  }

}
