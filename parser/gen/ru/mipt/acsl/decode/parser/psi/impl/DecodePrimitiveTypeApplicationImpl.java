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

public class DecodePrimitiveTypeApplicationImpl extends ASTWrapperPsiElement implements DecodePrimitiveTypeApplication {

  public DecodePrimitiveTypeApplicationImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DecodeVisitor) ((DecodeVisitor)visitor).visitPrimitiveTypeApplication(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public DecodeNativeTypeKind getNativeTypeKind() {
    return findChildByClass(DecodeNativeTypeKind.class);
  }

  @Override
  @Nullable
  public DecodePrimitiveTypeKind getPrimitiveTypeKind() {
    return findChildByClass(DecodePrimitiveTypeKind.class);
  }

  @Override
  @Nullable
  public PsiElement getNonNegativeNumber() {
    return findChildByType(NON_NEGATIVE_NUMBER);
  }

}
