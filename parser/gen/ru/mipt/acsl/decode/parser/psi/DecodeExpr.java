// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeExpr extends PsiElement {

  @Nullable
  DecodeCommandCall getCommandCall();

  @Nullable
  DecodeLiteral getLiteral();

  @Nullable
  DecodeVarExpr getVarExpr();

}
