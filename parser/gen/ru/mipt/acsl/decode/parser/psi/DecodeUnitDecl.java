// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeUnitDecl extends PsiElement {

  @Nullable
  DecodeElementInfo getElementInfo();

  @NotNull
  DecodeElementNameRule getElementNameRule();

  @NotNull
  List<DecodeStringValue> getStringValueList();

}
