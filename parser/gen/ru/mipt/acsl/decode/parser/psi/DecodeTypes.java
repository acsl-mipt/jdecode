// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import ru.mipt.acsl.decode.parser.psi.impl.*;

public interface DecodeTypes {

  IElementType ALIAS_DECL = new DecodeElementType("ALIAS_DECL");
  IElementType ARRAY_TYPE_APPLICATION = new DecodeElementType("ARRAY_TYPE_APPLICATION");
  IElementType BOOL_LITERAL = new DecodeElementType("BOOL_LITERAL");
  IElementType COMMAND_ARG = new DecodeElementType("COMMAND_ARG");
  IElementType COMMAND_ARGS = new DecodeElementType("COMMAND_ARGS");
  IElementType COMMAND_CALL = new DecodeElementType("COMMAND_CALL");
  IElementType COMMAND_DECL = new DecodeElementType("COMMAND_DECL");
  IElementType COMPONENT_DECL = new DecodeElementType("COMPONENT_DECL");
  IElementType COMPONENT_PARAMETERS_DECL = new DecodeElementType("COMPONENT_PARAMETERS_DECL");
  IElementType COMPONENT_REF = new DecodeElementType("COMPONENT_REF");
  IElementType DEFAULT_LANGUAGE_DECL = new DecodeElementType("DEFAULT_LANGUAGE_DECL");
  IElementType ELEMENT_ID = new DecodeElementType("ELEMENT_ID");
  IElementType ELEMENT_INFO = new DecodeElementType("ELEMENT_INFO");
  IElementType ELEMENT_NAME_RULE = new DecodeElementType("ELEMENT_NAME_RULE");
  IElementType ENTITY_ID = new DecodeElementType("ENTITY_ID");
  IElementType ENUM_TYPE_DECL = new DecodeElementType("ENUM_TYPE_DECL");
  IElementType ENUM_TYPE_VALUE = new DecodeElementType("ENUM_TYPE_VALUE");
  IElementType ENUM_TYPE_VALUES = new DecodeElementType("ENUM_TYPE_VALUES");
  IElementType EVENT_MESSAGE = new DecodeElementType("EVENT_MESSAGE");
  IElementType EVENT_MESSAGE_PARAMETERS_DECL = new DecodeElementType("EVENT_MESSAGE_PARAMETERS_DECL");
  IElementType EVENT_PARAMETER_DECL = new DecodeElementType("EVENT_PARAMETER_DECL");
  IElementType EXPR = new DecodeElementType("EXPR");
  IElementType FINAL_ENUM = new DecodeElementType("FINAL_ENUM");
  IElementType FLOAT_LITERAL = new DecodeElementType("FLOAT_LITERAL");
  IElementType GENERIC_ARG = new DecodeElementType("GENERIC_ARG");
  IElementType GENERIC_ARGS = new DecodeElementType("GENERIC_ARGS");
  IElementType GENERIC_PARAMETERS = new DecodeElementType("GENERIC_PARAMETERS");
  IElementType IMPORT_ELEMENT = new DecodeElementType("IMPORT_ELEMENT");
  IElementType IMPORT_ELEMENT_AS = new DecodeElementType("IMPORT_ELEMENT_AS");
  IElementType IMPORT_ELEMENT_STAR = new DecodeElementType("IMPORT_ELEMENT_STAR");
  IElementType IMPORT_STMT = new DecodeElementType("IMPORT_STMT");
  IElementType LENGTH_FROM = new DecodeElementType("LENGTH_FROM");
  IElementType LENGTH_TO = new DecodeElementType("LENGTH_TO");
  IElementType LITERAL = new DecodeElementType("LITERAL");
  IElementType MESSAGE_DECL = new DecodeElementType("MESSAGE_DECL");
  IElementType NAMESPACE_DECL = new DecodeElementType("NAMESPACE_DECL");
  IElementType NATIVE_TYPE_DECL = new DecodeElementType("NATIVE_TYPE_DECL");
  IElementType NUMERIC_LITERAL = new DecodeElementType("NUMERIC_LITERAL");
  IElementType OPTIONAL = new DecodeElementType("OPTIONAL");
  IElementType PARAMETER_DECL = new DecodeElementType("PARAMETER_DECL");
  IElementType PARAMETER_ELEMENT = new DecodeElementType("PARAMETER_ELEMENT");
  IElementType RANGE_DECL = new DecodeElementType("RANGE_DECL");
  IElementType SCRIPT_DECL = new DecodeElementType("SCRIPT_DECL");
  IElementType SIMPLE_OR_GENERIC_TYPE_APPLICATION = new DecodeElementType("SIMPLE_OR_GENERIC_TYPE_APPLICATION");
  IElementType STATUS_MESSAGE = new DecodeElementType("STATUS_MESSAGE");
  IElementType STATUS_MESSAGE_PARAMETERS_DECL = new DecodeElementType("STATUS_MESSAGE_PARAMETERS_DECL");
  IElementType STRING_LITERAL = new DecodeElementType("STRING_LITERAL");
  IElementType STRING_VALUE = new DecodeElementType("STRING_VALUE");
  IElementType STRUCT_TYPE_DECL = new DecodeElementType("STRUCT_TYPE_DECL");
  IElementType TYPE_APPLICATION = new DecodeElementType("TYPE_APPLICATION");
  IElementType TYPE_DECL = new DecodeElementType("TYPE_DECL");
  IElementType TYPE_DECL_BODY = new DecodeElementType("TYPE_DECL_BODY");
  IElementType TYPE_UNIT_APPLICATION = new DecodeElementType("TYPE_UNIT_APPLICATION");
  IElementType UNIT = new DecodeElementType("UNIT");
  IElementType UNIT_DECL = new DecodeElementType("UNIT_DECL");
  IElementType VAR_EXPR = new DecodeElementType("VAR_EXPR");
  IElementType VAR_PARAMETER_ELEMENT = new DecodeElementType("VAR_PARAMETER_ELEMENT");

  IElementType AFTER = new DecodeTokenType("after");
  IElementType ALIAS = new DecodeTokenType("alias");
  IElementType ARRAY = new DecodeTokenType("array");
  IElementType ARROW = new DecodeTokenType("->");
  IElementType AS = new DecodeTokenType("as");
  IElementType BEFORE = new DecodeTokenType("before");
  IElementType COLON = new DecodeTokenType(":");
  IElementType COMMA = new DecodeTokenType(",");
  IElementType COMMAND = new DecodeTokenType("command");
  IElementType COMMENT = new DecodeTokenType("COMMENT");
  IElementType COMPONENT = new DecodeTokenType("component");
  IElementType DEFAULT = new DecodeTokenType("default");
  IElementType DISPLAY = new DecodeTokenType("display");
  IElementType DOT = new DecodeTokenType(".");
  IElementType DOTS = new DecodeTokenType("..");
  IElementType DYNAMIC = new DecodeTokenType("dynamic");
  IElementType ELEMENT_NAME_TOKEN = new DecodeTokenType("ELEMENT_NAME_TOKEN");
  IElementType ENUM = new DecodeTokenType("enum");
  IElementType EQ_SIGN = new DecodeTokenType("=");
  IElementType ESCAPED_NAME = new DecodeTokenType("ESCAPED_NAME");
  IElementType EVENT = new DecodeTokenType("event");
  IElementType EXTENDS = new DecodeTokenType("extends");
  IElementType FALSE = new DecodeTokenType("false");
  IElementType FINAL = new DecodeTokenType("final");
  IElementType FOR = new DecodeTokenType("for");
  IElementType GT = new DecodeTokenType(">");
  IElementType ID = new DecodeTokenType("id");
  IElementType IMPORT = new DecodeTokenType("import");
  IElementType LANGUAGE = new DecodeTokenType("language");
  IElementType LEFT_BRACE = new DecodeTokenType("{");
  IElementType LEFT_BRACKET = new DecodeTokenType("[");
  IElementType LEFT_PAREN = new DecodeTokenType("(");
  IElementType LT = new DecodeTokenType("<");
  IElementType MESSAGE = new DecodeTokenType("message");
  IElementType MINUS = new DecodeTokenType("-");
  IElementType MULTILINE_COMMENT = new DecodeTokenType("MULTILINE_COMMENT");
  IElementType NAMESPACE = new DecodeTokenType("namespace");
  IElementType NATIVE = new DecodeTokenType("native");
  IElementType NON_NEGATIVE_NUMBER = new DecodeTokenType("NON_NEGATIVE_NUMBER");
  IElementType PARAMETER = new DecodeTokenType("parameter");
  IElementType PARAMETERS = new DecodeTokenType("parameters");
  IElementType PLACEMENT = new DecodeTokenType("placement");
  IElementType PLUS = new DecodeTokenType("+");
  IElementType PRIORITY = new DecodeTokenType("priority");
  IElementType QUESTION = new DecodeTokenType("?");
  IElementType RANGE = new DecodeTokenType("range");
  IElementType RIGHT_BRACE = new DecodeTokenType("}");
  IElementType RIGHT_BRACKET = new DecodeTokenType("]");
  IElementType RIGHT_PAREN = new DecodeTokenType(")");
  IElementType SCRIPT = new DecodeTokenType("script");
  IElementType SLASH = new DecodeTokenType("/");
  IElementType STAR = new DecodeTokenType("*");
  IElementType STATUS = new DecodeTokenType("status");
  IElementType STRING = new DecodeTokenType("STRING");
  IElementType STRING_UNARY_QUOTES = new DecodeTokenType("STRING_UNARY_QUOTES");
  IElementType STRUCT = new DecodeTokenType("struct");
  IElementType SUBCOMPONENT = new DecodeTokenType("subcomponent");
  IElementType TRUE = new DecodeTokenType("true");
  IElementType TYPE_KEYWORD = new DecodeTokenType("type");
  IElementType UNIT_TOKEN = new DecodeTokenType("unit");
  IElementType VAR = new DecodeTokenType("var");
  IElementType WITH = new DecodeTokenType("with");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == ALIAS_DECL) {
        return new DecodeAliasDeclImpl(node);
      }
      else if (type == ARRAY_TYPE_APPLICATION) {
        return new DecodeArrayTypeApplicationImpl(node);
      }
      else if (type == BOOL_LITERAL) {
        return new DecodeBoolLiteralImpl(node);
      }
      else if (type == COMMAND_ARG) {
        return new DecodeCommandArgImpl(node);
      }
      else if (type == COMMAND_ARGS) {
        return new DecodeCommandArgsImpl(node);
      }
      else if (type == COMMAND_CALL) {
        return new DecodeCommandCallImpl(node);
      }
      else if (type == COMMAND_DECL) {
        return new DecodeCommandDeclImpl(node);
      }
      else if (type == COMPONENT_DECL) {
        return new DecodeComponentDeclImpl(node);
      }
      else if (type == COMPONENT_PARAMETERS_DECL) {
        return new DecodeComponentParametersDeclImpl(node);
      }
      else if (type == COMPONENT_REF) {
        return new DecodeComponentRefImpl(node);
      }
      else if (type == DEFAULT_LANGUAGE_DECL) {
        return new DecodeDefaultLanguageDeclImpl(node);
      }
      else if (type == ELEMENT_ID) {
        return new DecodeElementIdImpl(node);
      }
      else if (type == ELEMENT_INFO) {
        return new DecodeElementInfoImpl(node);
      }
      else if (type == ELEMENT_NAME_RULE) {
        return new DecodeElementNameRuleImpl(node);
      }
      else if (type == ENTITY_ID) {
        return new DecodeEntityIdImpl(node);
      }
      else if (type == ENUM_TYPE_DECL) {
        return new DecodeEnumTypeDeclImpl(node);
      }
      else if (type == ENUM_TYPE_VALUE) {
        return new DecodeEnumTypeValueImpl(node);
      }
      else if (type == ENUM_TYPE_VALUES) {
        return new DecodeEnumTypeValuesImpl(node);
      }
      else if (type == EVENT_MESSAGE) {
        return new DecodeEventMessageImpl(node);
      }
      else if (type == EVENT_MESSAGE_PARAMETERS_DECL) {
        return new DecodeEventMessageParametersDeclImpl(node);
      }
      else if (type == EVENT_PARAMETER_DECL) {
        return new DecodeEventParameterDeclImpl(node);
      }
      else if (type == EXPR) {
        return new DecodeExprImpl(node);
      }
      else if (type == FINAL_ENUM) {
        return new DecodeFinalEnumImpl(node);
      }
      else if (type == FLOAT_LITERAL) {
        return new DecodeFloatLiteralImpl(node);
      }
      else if (type == GENERIC_ARG) {
        return new DecodeGenericArgImpl(node);
      }
      else if (type == GENERIC_ARGS) {
        return new DecodeGenericArgsImpl(node);
      }
      else if (type == GENERIC_PARAMETERS) {
        return new DecodeGenericParametersImpl(node);
      }
      else if (type == IMPORT_ELEMENT) {
        return new DecodeImportElementImpl(node);
      }
      else if (type == IMPORT_ELEMENT_AS) {
        return new DecodeImportElementAsImpl(node);
      }
      else if (type == IMPORT_ELEMENT_STAR) {
        return new DecodeImportElementStarImpl(node);
      }
      else if (type == IMPORT_STMT) {
        return new DecodeImportStmtImpl(node);
      }
      else if (type == LENGTH_FROM) {
        return new DecodeLengthFromImpl(node);
      }
      else if (type == LENGTH_TO) {
        return new DecodeLengthToImpl(node);
      }
      else if (type == LITERAL) {
        return new DecodeLiteralImpl(node);
      }
      else if (type == MESSAGE_DECL) {
        return new DecodeMessageDeclImpl(node);
      }
      else if (type == NAMESPACE_DECL) {
        return new DecodeNamespaceDeclImpl(node);
      }
      else if (type == NATIVE_TYPE_DECL) {
        return new DecodeNativeTypeDeclImpl(node);
      }
      else if (type == NUMERIC_LITERAL) {
        return new DecodeNumericLiteralImpl(node);
      }
      else if (type == OPTIONAL) {
        return new DecodeOptionalImpl(node);
      }
      else if (type == PARAMETER_DECL) {
        return new DecodeParameterDeclImpl(node);
      }
      else if (type == PARAMETER_ELEMENT) {
        return new DecodeParameterElementImpl(node);
      }
      else if (type == RANGE_DECL) {
        return new DecodeRangeDeclImpl(node);
      }
      else if (type == SCRIPT_DECL) {
        return new DecodeScriptDeclImpl(node);
      }
      else if (type == SIMPLE_OR_GENERIC_TYPE_APPLICATION) {
        return new DecodeSimpleOrGenericTypeApplicationImpl(node);
      }
      else if (type == STATUS_MESSAGE) {
        return new DecodeStatusMessageImpl(node);
      }
      else if (type == STATUS_MESSAGE_PARAMETERS_DECL) {
        return new DecodeStatusMessageParametersDeclImpl(node);
      }
      else if (type == STRING_LITERAL) {
        return new DecodeStringLiteralImpl(node);
      }
      else if (type == STRING_VALUE) {
        return new DecodeStringValueImpl(node);
      }
      else if (type == STRUCT_TYPE_DECL) {
        return new DecodeStructTypeDeclImpl(node);
      }
      else if (type == TYPE_APPLICATION) {
        return new DecodeTypeApplicationImpl(node);
      }
      else if (type == TYPE_DECL) {
        return new DecodeTypeDeclImpl(node);
      }
      else if (type == TYPE_DECL_BODY) {
        return new DecodeTypeDeclBodyImpl(node);
      }
      else if (type == TYPE_UNIT_APPLICATION) {
        return new DecodeTypeUnitApplicationImpl(node);
      }
      else if (type == UNIT) {
        return new DecodeUnitImpl(node);
      }
      else if (type == UNIT_DECL) {
        return new DecodeUnitDeclImpl(node);
      }
      else if (type == VAR_EXPR) {
        return new DecodeVarExprImpl(node);
      }
      else if (type == VAR_PARAMETER_ELEMENT) {
        return new DecodeVarParameterElementImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}