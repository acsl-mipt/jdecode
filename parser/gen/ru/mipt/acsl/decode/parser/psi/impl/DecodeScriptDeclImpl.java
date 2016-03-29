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

public class DecodeScriptDeclImpl extends ASTWrapperPsiElement implements DecodeScriptDecl {

  public DecodeScriptDeclImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull DecodeVisitor visitor) {
    visitor.visitScriptDecl(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) accept((DecodeVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public DecodeCommandArgs getCommandArgs() {
    return findChildByClass(DecodeCommandArgs.class);
  }

  @Override
  @NotNull
  public DecodeComponentRef getComponentRef() {
    return findNotNullChildByClass(DecodeComponentRef.class);
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
  @Nullable
  public DecodeTypeUnitApplication getTypeUnitApplication() {
    return findChildByClass(DecodeTypeUnitApplication.class);
  }

}
