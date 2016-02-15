// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeStatusMessage extends PsiElement {

  @NotNull
  DecodeElementNameRule getElementNameRule();

  @Nullable
  DecodeEntityId getEntityId();

  @Nullable
  DecodeInfoString getInfoString();

  @NotNull
  DecodeStatusMessageParametersDecl getStatusMessageParametersDecl();

  @Nullable
  PsiElement getNonNegativeNumber();

}
