// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeCommandArg extends PsiElement {

  @NotNull
  DecodeElementNameRule getElementNameRule();

  @Nullable
  DecodeInfoString getInfoString();

  @NotNull
  DecodeTypeUnitApplication getTypeUnitApplication();

}
