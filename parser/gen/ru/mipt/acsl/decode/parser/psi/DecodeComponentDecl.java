// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DecodeComponentDecl extends PsiElement {

  @NotNull
  List<DecodeCommandDecl> getCommandDeclList();

  @Nullable
  DecodeComponentParametersDecl getComponentParametersDecl();

  @NotNull
  DecodeElementNameRule getElementNameRule();

  @Nullable
  DecodeEntityId getEntityId();

  @Nullable
  DecodeInfoString getInfoString();

  @NotNull
  List<DecodeMessageDecl> getMessageDeclList();

  @NotNull
  List<DecodeSubcomponentDecl> getSubcomponentDeclList();

}
