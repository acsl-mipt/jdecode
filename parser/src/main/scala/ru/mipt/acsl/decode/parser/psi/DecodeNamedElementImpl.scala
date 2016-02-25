package ru.mipt.acsl.decode.parser.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

/**
 * @author Artem Shein
 */
abstract class DecodeNamedElementImpl(node: ASTNode) extends ASTWrapperPsiElement(node) with DecodeNamedElement
