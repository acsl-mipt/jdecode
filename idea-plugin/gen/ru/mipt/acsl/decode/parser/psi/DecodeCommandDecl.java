// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeCommandDecl extends PsiElement {

  @Nullable
  DecodeCommandArgs getCommandArgs();

  @NotNull
  DecodeElementNameRule getElementNameRule();

  @Nullable
  DecodeInfoString getInfoString();

  @Nullable
  DecodeTypeUnitApplication getTypeUnitApplication();

  @Nullable
  PsiElement getNonNegativeNumber();

}
