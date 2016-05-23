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

public class DecodeComponentDeclImpl extends ASTWrapperPsiElement implements DecodeComponentDecl {

  public DecodeComponentDeclImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull DecodeVisitor visitor) {
    visitor.visitComponentDecl(this);
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
  @NotNull
  public List<DecodeCommandDecl> getCommandDeclList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, DecodeCommandDecl.class);
  }

  @Override
  @Nullable
  public DecodeComponentParametersDecl getComponentParametersDecl() {
    return findChildByClass(DecodeComponentParametersDecl.class);
  }

  @Override
  @NotNull
  public List<DecodeComponentRef> getComponentRefList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, DecodeComponentRef.class);
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
  public List<DecodeMessageDecl> getMessageDeclList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, DecodeMessageDecl.class);
  }

}
