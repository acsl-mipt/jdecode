// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeEnumTypeDecl extends PsiElement {

  @Nullable
  DecodeElementNameRule getElementNameRule();

  @NotNull
  DecodeEnumTypeValues getEnumTypeValues();

  @Nullable
  DecodeFinalEnum getFinalEnum();

  @Nullable
  DecodeTypeApplication getTypeApplication();

}
