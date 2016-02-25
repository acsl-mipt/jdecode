// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeTypeApplication extends PsiElement {

  @Nullable
  DecodeArrayTypeApplication getArrayTypeApplication();

  @Nullable
  DecodeGenericTypeApplication getGenericTypeApplication();

  @Nullable
  DecodePrimitiveTypeApplication getPrimitiveTypeApplication();

}
