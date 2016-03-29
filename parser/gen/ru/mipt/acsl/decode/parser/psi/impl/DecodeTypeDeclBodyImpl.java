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

  public void accept(@NotNull DecodeVisitor visitor) {
    visitor.visitTypeDeclBody(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) accept((DecodeVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public DecodeEnumTypeDecl getEnumTypeDecl() {
    return findChildByClass(DecodeEnumTypeDecl.class);
  }

  @Override
  @Nullable
  public DecodeNativeTypeDecl getNativeTypeDecl() {
    return findChildByClass(DecodeNativeTypeDecl.class);
  }

  @Override
  @Nullable
  public DecodeNumericRangeDecl getNumericRangeDecl() {
    return findChildByClass(DecodeNumericRangeDecl.class);
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
