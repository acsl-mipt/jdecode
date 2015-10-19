// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeEnumTypeDecl extends PsiElement {

  @NotNull
  DecodeEnumTypeValues getEnumTypeValues();

  @Nullable
  DecodeInfoString getInfoString();

  @Nullable
  DecodeNativeTypeKind getNativeTypeKind();

  @Nullable
  DecodePrimitiveTypeKind getPrimitiveTypeKind();

}
