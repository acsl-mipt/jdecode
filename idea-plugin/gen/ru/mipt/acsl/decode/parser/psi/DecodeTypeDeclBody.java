// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeTypeDeclBody extends PsiElement {

  @Nullable
  DecodeEnumTypeDecl getEnumTypeDecl();

  @Nullable
  DecodeInfoString getInfoString();

  @Nullable
  DecodeStructTypeDecl getStructTypeDecl();

  @Nullable
  DecodeTypeApplication getTypeApplication();

}
