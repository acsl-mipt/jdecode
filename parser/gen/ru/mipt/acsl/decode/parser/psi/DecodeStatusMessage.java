// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeStatusMessage extends PsiElement {

  @Nullable
  DecodeElementInfo getElementInfo();

  @NotNull
  DecodeElementNameRule getElementNameRule();

  @Nullable
  DecodeEntityId getEntityId();

  @NotNull
  DecodeStatusMessageParametersDecl getStatusMessageParametersDecl();

  @Nullable
  PsiElement getNonNegativeNumber();

}
