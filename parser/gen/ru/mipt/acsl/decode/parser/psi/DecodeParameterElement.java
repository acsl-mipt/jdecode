// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeParameterElement extends PsiElement {

  @NotNull
  List<DecodeElementNameRule> getElementNameRuleList();

  @NotNull
  List<DecodeRangeDecl> getRangeDeclList();

}
