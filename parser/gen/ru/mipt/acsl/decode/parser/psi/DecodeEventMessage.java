// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeEventMessage extends PsiElement {

  @NotNull
  DecodeElementNameRule getElementNameRule();

  @Nullable
  DecodeEntityId getEntityId();

  @NotNull
  DecodeEventMessageParametersDecl getEventMessageParametersDecl();

  @Nullable
  DecodeInfoString getInfoString();

  @NotNull
  DecodeTypeApplication getTypeApplication();

}
