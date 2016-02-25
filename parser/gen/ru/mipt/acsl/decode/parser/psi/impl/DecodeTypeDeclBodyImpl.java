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

public class DecodeTypeDeclBodyImpl extends ASTWrapperPsiElement implements DecodeTypeDeclBody {

  public DecodeTypeDeclBodyImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) ((DecodeVisitor)visitor).visitTypeDeclBody(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public DecodeEnumTypeDecl getEnumTypeDecl() {
    return findChildByClass(DecodeEnumTypeDecl.class);
  }

  @Override
  @Nullable
  public DecodeInfoString getInfoString() {
    return findChildByClass(DecodeInfoString.class);
  }

  @Override
  @Nullable
  public DecodeStructTypeDecl getStructTypeDecl() {
    return findChildByClass(DecodeStructTypeDecl.class);
  }

  @Override
  @Nullable
  public DecodeTypeApplication getTypeApplication() {
    return findChildByClass(DecodeTypeApplication.class);
  }

}
