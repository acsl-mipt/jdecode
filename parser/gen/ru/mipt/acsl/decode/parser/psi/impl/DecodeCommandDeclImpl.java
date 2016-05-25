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

public class DecodeCommandDeclImpl extends ASTWrapperPsiElement implements DecodeCommandDecl {

  public DecodeCommandDeclImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull DecodeVisitor visitor) {
    visitor.visitCommandDecl(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) accept((DecodeVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<DecodeAnnotationDecl> getAnnotationDeclList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, DecodeAnnotationDecl.class);
  }

  @Override
  @Nullable
  public DecodeCommandArgs getCommandArgs() {
    return findChildByClass(DecodeCommandArgs.class);
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
  @NotNull
  public List<DecodeExpr> getExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, DecodeExpr.class);
  }

  @Override
  @NotNull
  public DecodeTypeUnitApplication getTypeUnitApplication() {
    return findNotNullChildByClass(DecodeTypeUnitApplication.class);
  }

}
