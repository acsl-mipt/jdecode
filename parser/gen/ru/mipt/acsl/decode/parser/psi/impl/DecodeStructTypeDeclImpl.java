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

public class DecodeStructTypeDeclImpl extends ASTWrapperPsiElement implements DecodeStructTypeDecl {

  public DecodeStructTypeDeclImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull DecodeVisitor visitor) {
    visitor.visitStructTypeDecl(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) accept((DecodeVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public DecodeCommandArgs getCommandArgs() {
    return findNotNullChildByClass(DecodeCommandArgs.class);
  }

  @Override
  @Nullable
  public DecodeElementInfo getElementInfo() {
    return findChildByClass(DecodeElementInfo.class);
  }

  @Override
  @NotNull
  public DecodeElementNameRule getElementNameRule() {
    return findNotNullChildByClass(DecodeElementNameRule.class);
  }

  @Override
  @Nullable
  public DecodeGenericParameters getGenericParameters() {
    return findChildByClass(DecodeGenericParameters.class);
  }

}
