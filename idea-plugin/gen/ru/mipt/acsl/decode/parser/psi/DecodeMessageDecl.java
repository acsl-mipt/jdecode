// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeMessageDecl extends PsiElement {

  @Nullable
  DecodeDynamicStatusMessage getDynamicStatusMessage();

  @NotNull
  DecodeElementNameRule getElementNameRule();

  @Nullable
  DecodeEventMessage getEventMessage();

  @Nullable
  DecodeInfoString getInfoString();

  @Nullable
  DecodeStatusMessage getStatusMessage();

  @NotNull
  PsiElement getNonNegativeNumber();

}
