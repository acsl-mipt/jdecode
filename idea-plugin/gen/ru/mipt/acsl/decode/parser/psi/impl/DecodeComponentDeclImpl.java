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

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) ((DecodeVisitor)visitor).visitComponentDecl(this);
    else super.accept(visitor);
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
  public List<DecodeMessageDecl> getMessageDeclList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, DecodeMessageDecl.class);
  }

  @Override
  @NotNull
  public List<DecodeSubcomponentDecl> getSubcomponentDeclList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, DecodeSubcomponentDecl.class);
  }

}
