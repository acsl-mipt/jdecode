// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeScriptDecl extends PsiElement {

  @Nullable
  DecodeCommandArgs getCommandArgs();

  @NotNull
  DecodeComponentRef getComponentRef();

  @NotNull
  DecodeElementNameRule getElementNameRule();

  @NotNull
  List<DecodeExpr> getExprList();

  @NotNull
  DecodeTypeUnitApplication getTypeUnitApplication();

}
