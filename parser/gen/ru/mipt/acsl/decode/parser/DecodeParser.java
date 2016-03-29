// This is a generated file. Not intended for manual editing.
package ru.mipt.acsl.decode.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static ru.mipt.acsl.decode.parser.psi.DecodeTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class DecodeParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == ALIAS_DECL) {
      r = alias_decl(b, 0);
    }
    else if (t == ARRAY_TYPE_APPLICATION) {
      r = array_type_application(b, 0);
    }
    else if (t == BOOL_LITERAL) {
      r = bool_literal(b, 0);
    }
    else if (t == COMMAND_ARG) {
      r = command_arg(b, 0);
    }
    else if (t == COMMAND_ARGS) {
      r = command_args(b, 0);
    }
    else if (t == COMMAND_CALL) {
      r = command_call(b, 0);
    }
    else if (t == COMMAND_DECL) {
      r = command_decl(b, 0);
    }
    else if (t == COMPONENT_DECL) {
      r = component_decl(b, 0);
    }
    else if (t == COMPONENT_PARAMETERS_DECL) {
      r = component_parameters_decl(b, 0);
    }
    else if (t == COMPONENT_REF) {
      r = component_ref(b, 0);
    }
    else if (t == DEFAULT_LANGUAGE_DECL) {
      r = default_language_decl(b, 0);
    }
    else if (t == ELEMENT_ID) {
      r = element_id(b, 0);
    }
    else if (t == ELEMENT_INFO) {
      r = element_info(b, 0);
    }
    else if (t == ELEMENT_NAME_RULE) {
      r = element_name_rule(b, 0);
    }
    else if (t == ENTITY_ID) {
      r = entity_id(b, 0);
    }
    else if (t == ENUM_TYPE_DECL) {
      r = enum_type_decl(b, 0);
    }
    else if (t == ENUM_TYPE_VALUE) {
      r = enum_type_value(b, 0);
    }
    else if (t == ENUM_TYPE_VALUES) {
      r = enum_type_values(b, 0);
    }
    else if (t == EVENT_MESSAGE) {
      r = event_message(b, 0);
    }
    else if (t == EVENT_MESSAGE_PARAMETERS_DECL) {
      r = event_message_parameters_decl(b, 0);
    }
    else if (t == EVENT_PARAMETER_DECL) {
      r = event_parameter_decl(b, 0);
    }
    else if (t == EXPR) {
      r = expr(b, 0);
    }
    else if (t == FINAL_ENUM) {
      r = final_enum(b, 0);
    }
    else if (t == FLOAT_LITERAL) {
      r = float_literal(b, 0);
    }
    else if (t == GENERIC_ARG) {
      r = generic_arg(b, 0);
    }
    else if (t == GENERIC_ARGS) {
      r = generic_args(b, 0);
    }
    else if (t == GENERIC_PARAMETERS) {
      r = generic_parameters(b, 0);
    }
    else if (t == IMPORT_ELEMENT) {
      r = import_element(b, 0);
    }
    else if (t == IMPORT_ELEMENT_AS) {
      r = import_element_as(b, 0);
    }
    else if (t == IMPORT_ELEMENT_STAR) {
      r = import_element_star(b, 0);
    }
    else if (t == IMPORT_STMT) {
      r = import_stmt(b, 0);
    }
    else if (t == LENGTH_FROM) {
      r = length_from(b, 0);
    }
    else if (t == LENGTH_TO) {
      r = length_to(b, 0);
    }
    else if (t == LITERAL) {
      r = literal(b, 0);
    }
    else if (t == MESSAGE_DECL) {
      r = message_decl(b, 0);
    }
    else if (t == NAMESPACE_DECL) {
      r = namespace_decl(b, 0);
    }
    else if (t == NATIVE_TYPE_DECL) {
      r = native_type_decl(b, 0);
    }
    else if (t == NON_NEGATIVE_INTEGER_LITERAL) {
      r = non_negative_integer_literal(b, 0);
    }
    else if (t == NUMERIC_LITERAL) {
      r = numeric_literal(b, 0);
    }
    else if (t == NUMERIC_RANGE_DECL) {
      r = numeric_range_decl(b, 0);
    }
    else if (t == OPTIONAL) {
      r = optional(b, 0);
    }
    else if (t == PARAMETER_DECL) {
      r = parameter_decl(b, 0);
    }
    else if (t == PARAMETER_ELEMENT) {
      r = parameter_element(b, 0);
    }
    else if (t == RANGE_DECL) {
      r = range_decl(b, 0);
    }
    else if (t == RANGE_UPPER_BOUND_DECL) {
      r = range_upper_bound_decl(b, 0);
    }
    else if (t == SCRIPT_DECL) {
      r = script_decl(b, 0);
    }
    else if (t == SIMPLE_OR_GENERIC_TYPE_APPLICATION) {
      r = simple_or_generic_type_application(b, 0);
    }
    else if (t == STATUS_MESSAGE) {
      r = status_message(b, 0);
    }
    else if (t == STATUS_MESSAGE_PARAMETERS_DECL) {
      r = status_message_parameters_decl(b, 0);
    }
    else if (t == STRING_LITERAL) {
      r = string_literal(b, 0);
    }
    else if (t == STRING_VALUE) {
      r = string_value(b, 0);
    }
    else if (t == STRUCT_TYPE_DECL) {
      r = struct_type_decl(b, 0);
    }
    else if (t == TYPE_APPLICATION) {
      r = type_application(b, 0);
    }
    else if (t == TYPE_DECL) {
      r = type_decl(b, 0);
    }
    else if (t == TYPE_DECL_BODY) {
      r = type_decl_body(b, 0);
    }
    else if (t == TYPE_UNIT_APPLICATION) {
      r = type_unit_application(b, 0);
    }
    else if (t == UNIT) {
      r = unit(b, 0);
    }
    else if (t == UNIT_DECL) {
      r = unit_decl(b, 0);
    }
    else if (t == VAR_EXPR) {
      r = var_expr(b, 0);
    }
    else if (t == VAR_PARAMETER_ELEMENT) {
      r = var_parameter_element(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return decodeFile(b, l + 1);
  }

  /* ********************************************************** */
  // element_info? ALIAS element_name_rule type_application
  public static boolean alias_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "alias_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ALIAS_DECL, "<alias decl>");
    r = alias_decl_0(b, l + 1);
    r = r && consumeToken(b, ALIAS);
    r = r && element_name_rule(b, l + 1);
    r = r && type_application(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean alias_decl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "alias_decl_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // LEFT_BRACKET type_application (COMMA length_from (DOTS (length_to | STAR))?)? RIGHT_BRACKET
  public static boolean array_type_application(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_type_application")) return false;
    if (!nextTokenIs(b, LEFT_BRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_BRACKET);
    r = r && type_application(b, l + 1);
    r = r && array_type_application_2(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACKET);
    exit_section_(b, m, ARRAY_TYPE_APPLICATION, r);
    return r;
  }

  // (COMMA length_from (DOTS (length_to | STAR))?)?
  private static boolean array_type_application_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_type_application_2")) return false;
    array_type_application_2_0(b, l + 1);
    return true;
  }

  // COMMA length_from (DOTS (length_to | STAR))?
  private static boolean array_type_application_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_type_application_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && length_from(b, l + 1);
    r = r && array_type_application_2_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (DOTS (length_to | STAR))?
  private static boolean array_type_application_2_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_type_application_2_0_2")) return false;
    array_type_application_2_0_2_0(b, l + 1);
    return true;
  }

  // DOTS (length_to | STAR)
  private static boolean array_type_application_2_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_type_application_2_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOTS);
    r = r && array_type_application_2_0_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // length_to | STAR
  private static boolean array_type_application_2_0_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_type_application_2_0_2_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = length_to(b, l + 1);
    if (!r) r = consumeToken(b, STAR);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // TRUE | FALSE
  public static boolean bool_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bool_literal")) return false;
    if (!nextTokenIs(b, "<bool literal>", FALSE, TRUE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BOOL_LITERAL, "<bool literal>");
    r = consumeToken(b, TRUE);
    if (!r) r = consumeToken(b, FALSE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // element_info? element_name_rule COLON type_unit_application
  public static boolean command_arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_arg")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMMAND_ARG, "<command arg>");
    r = command_arg_0(b, l + 1);
    r = r && element_name_rule(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && type_unit_application(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean command_arg_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_arg_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // command_arg (COMMA command_arg)* COMMA?
  public static boolean command_args(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_args")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMMAND_ARGS, "<command args>");
    r = command_arg(b, l + 1);
    r = r && command_args_1(b, l + 1);
    r = r && command_args_2(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (COMMA command_arg)*
  private static boolean command_args_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_args_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!command_args_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "command_args_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // COMMA command_arg
  private static boolean command_args_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_args_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && command_arg(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean command_args_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_args_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // element_id LEFT_PAREN (expr (COMMA expr)*)? RIGHT_PAREN
  public static boolean command_call(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_call")) return false;
    if (!nextTokenIs(b, "<command call>", ELEMENT_NAME_TOKEN, ESCAPED_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMMAND_CALL, "<command call>");
    r = element_id(b, l + 1);
    r = r && consumeToken(b, LEFT_PAREN);
    r = r && command_call_2(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (expr (COMMA expr)*)?
  private static boolean command_call_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_call_2")) return false;
    command_call_2_0(b, l + 1);
    return true;
  }

  // expr (COMMA expr)*
  private static boolean command_call_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_call_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr(b, l + 1);
    r = r && command_call_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA expr)*
  private static boolean command_call_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_call_2_0_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!command_call_2_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "command_call_2_0_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // COMMA expr
  private static boolean command_call_2_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_call_2_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // element_info? COMMAND element_name_rule entity_id? LEFT_PAREN command_args? RIGHT_PAREN
  //     (COLON type_unit_application)? (EQ_SIGN LEFT_BRACE expr* RIGHT_BRACE)?
  public static boolean command_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMMAND_DECL, "<command decl>");
    r = command_decl_0(b, l + 1);
    r = r && consumeToken(b, COMMAND);
    r = r && element_name_rule(b, l + 1);
    r = r && command_decl_3(b, l + 1);
    r = r && consumeToken(b, LEFT_PAREN);
    r = r && command_decl_5(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    r = r && command_decl_7(b, l + 1);
    r = r && command_decl_8(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean command_decl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_decl_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  // entity_id?
  private static boolean command_decl_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_decl_3")) return false;
    entity_id(b, l + 1);
    return true;
  }

  // command_args?
  private static boolean command_decl_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_decl_5")) return false;
    command_args(b, l + 1);
    return true;
  }

  // (COLON type_unit_application)?
  private static boolean command_decl_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_decl_7")) return false;
    command_decl_7_0(b, l + 1);
    return true;
  }

  // COLON type_unit_application
  private static boolean command_decl_7_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_decl_7_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && type_unit_application(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (EQ_SIGN LEFT_BRACE expr* RIGHT_BRACE)?
  private static boolean command_decl_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_decl_8")) return false;
    command_decl_8_0(b, l + 1);
    return true;
  }

  // EQ_SIGN LEFT_BRACE expr* RIGHT_BRACE
  private static boolean command_decl_8_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_decl_8_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, EQ_SIGN, LEFT_BRACE);
    r = r && command_decl_8_0_2(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr*
  private static boolean command_decl_8_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_decl_8_0_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!expr(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "command_decl_8_0_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // element_info? COMPONENT element_name_rule entity_id? (WITH component_ref (COMMA component_ref)*)?
  //     LEFT_BRACE component_parameters_decl? (command_decl|message_decl)* RIGHT_BRACE
  public static boolean component_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMPONENT_DECL, "<component decl>");
    r = component_decl_0(b, l + 1);
    r = r && consumeToken(b, COMPONENT);
    r = r && element_name_rule(b, l + 1);
    r = r && component_decl_3(b, l + 1);
    r = r && component_decl_4(b, l + 1);
    r = r && consumeToken(b, LEFT_BRACE);
    r = r && component_decl_6(b, l + 1);
    r = r && component_decl_7(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean component_decl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_decl_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  // entity_id?
  private static boolean component_decl_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_decl_3")) return false;
    entity_id(b, l + 1);
    return true;
  }

  // (WITH component_ref (COMMA component_ref)*)?
  private static boolean component_decl_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_decl_4")) return false;
    component_decl_4_0(b, l + 1);
    return true;
  }

  // WITH component_ref (COMMA component_ref)*
  private static boolean component_decl_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_decl_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, WITH);
    r = r && component_ref(b, l + 1);
    r = r && component_decl_4_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA component_ref)*
  private static boolean component_decl_4_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_decl_4_0_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!component_decl_4_0_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "component_decl_4_0_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // COMMA component_ref
  private static boolean component_decl_4_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_decl_4_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && component_ref(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // component_parameters_decl?
  private static boolean component_decl_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_decl_6")) return false;
    component_parameters_decl(b, l + 1);
    return true;
  }

  // (command_decl|message_decl)*
  private static boolean component_decl_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_decl_7")) return false;
    int c = current_position_(b);
    while (true) {
      if (!component_decl_7_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "component_decl_7", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // command_decl|message_decl
  private static boolean component_decl_7_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_decl_7_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = command_decl(b, l + 1);
    if (!r) r = message_decl(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // element_info? PARAMETERS LEFT_PAREN command_args RIGHT_PAREN
  public static boolean component_parameters_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_parameters_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMPONENT_PARAMETERS_DECL, "<component parameters decl>");
    r = component_parameters_decl_0(b, l + 1);
    r = r && consumeTokens(b, 0, PARAMETERS, LEFT_PAREN);
    r = r && command_args(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean component_parameters_decl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_parameters_decl_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // element_name_rule
  public static boolean component_ref(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "component_ref")) return false;
    if (!nextTokenIs(b, "<component ref>", ELEMENT_NAME_TOKEN, ESCAPED_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMPONENT_REF, "<component ref>");
    r = element_name_rule(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // namespace_decl import_stmt* (component_decl | unit_decl | type_decl | alias_decl
  //     | default_language_decl | script_decl)*
  static boolean decodeFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "decodeFile")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = namespace_decl(b, l + 1);
    r = r && decodeFile_1(b, l + 1);
    r = r && decodeFile_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // import_stmt*
  private static boolean decodeFile_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "decodeFile_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!import_stmt(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "decodeFile_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // (component_decl | unit_decl | type_decl | alias_decl
  //     | default_language_decl | script_decl)*
  private static boolean decodeFile_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "decodeFile_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!decodeFile_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "decodeFile_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // component_decl | unit_decl | type_decl | alias_decl
  //     | default_language_decl | script_decl
  private static boolean decodeFile_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "decodeFile_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = component_decl(b, l + 1);
    if (!r) r = unit_decl(b, l + 1);
    if (!r) r = type_decl(b, l + 1);
    if (!r) r = alias_decl(b, l + 1);
    if (!r) r = default_language_decl(b, l + 1);
    if (!r) r = script_decl(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LANGUAGE element_name_rule
  public static boolean default_language_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "default_language_decl")) return false;
    if (!nextTokenIs(b, LANGUAGE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LANGUAGE);
    r = r && element_name_rule(b, l + 1);
    exit_section_(b, m, DEFAULT_LANGUAGE_DECL, r);
    return r;
  }

  /* ********************************************************** */
  // element_name_rule (DOT element_name_rule)*
  public static boolean element_id(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "element_id")) return false;
    if (!nextTokenIs(b, "<element id>", ELEMENT_NAME_TOKEN, ESCAPED_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ELEMENT_ID, "<element id>");
    r = element_name_rule(b, l + 1);
    r = r && element_id_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (DOT element_name_rule)*
  private static boolean element_id_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "element_id_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!element_id_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "element_id_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // DOT element_name_rule
  private static boolean element_id_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "element_id_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOT);
    r = r && element_name_rule(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // string_value+
  public static boolean element_info(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "element_info")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ELEMENT_INFO, "<element info>");
    r = string_value(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!string_value(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "element_info", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ELEMENT_NAME_TOKEN | ESCAPED_NAME
  public static boolean element_name_rule(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "element_name_rule")) return false;
    if (!nextTokenIs(b, "<element name rule>", ELEMENT_NAME_TOKEN, ESCAPED_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ELEMENT_NAME_RULE, "<element name rule>");
    r = consumeToken(b, ELEMENT_NAME_TOKEN);
    if (!r) r = consumeToken(b, ESCAPED_NAME);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ID EQ_SIGN NON_NEGATIVE_NUMBER
  public static boolean entity_id(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entity_id")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, ID, EQ_SIGN, NON_NEGATIVE_NUMBER);
    exit_section_(b, m, ENTITY_ID, r);
    return r;
  }

  /* ********************************************************** */
  // final_enum? ENUM (EXTENDS element_name_rule | type_application) LEFT_PAREN enum_type_values RIGHT_PAREN
  public static boolean enum_type_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_type_decl")) return false;
    if (!nextTokenIs(b, "<enum type decl>", ENUM, FINAL)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ENUM_TYPE_DECL, "<enum type decl>");
    r = enum_type_decl_0(b, l + 1);
    r = r && consumeToken(b, ENUM);
    r = r && enum_type_decl_2(b, l + 1);
    r = r && consumeToken(b, LEFT_PAREN);
    r = r && enum_type_values(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // final_enum?
  private static boolean enum_type_decl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_type_decl_0")) return false;
    final_enum(b, l + 1);
    return true;
  }

  // EXTENDS element_name_rule | type_application
  private static boolean enum_type_decl_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_type_decl_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = enum_type_decl_2_0(b, l + 1);
    if (!r) r = type_application(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // EXTENDS element_name_rule
  private static boolean enum_type_decl_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_type_decl_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EXTENDS);
    r = r && element_name_rule(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // element_info? element_name_rule EQ_SIGN literal
  public static boolean enum_type_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_type_value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ENUM_TYPE_VALUE, "<enum type value>");
    r = enum_type_value_0(b, l + 1);
    r = r && element_name_rule(b, l + 1);
    r = r && consumeToken(b, EQ_SIGN);
    r = r && literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean enum_type_value_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_type_value_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // enum_type_value (COMMA enum_type_value)* COMMA?
  public static boolean enum_type_values(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_type_values")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ENUM_TYPE_VALUES, "<enum type values>");
    r = enum_type_value(b, l + 1);
    r = r && enum_type_values_1(b, l + 1);
    r = r && enum_type_values_2(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (COMMA enum_type_value)*
  private static boolean enum_type_values_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_type_values_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!enum_type_values_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "enum_type_values_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // COMMA enum_type_value
  private static boolean enum_type_values_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_type_values_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && enum_type_value(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean enum_type_values_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_type_values_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // element_info? EVENT element_name_rule entity_id? type_application event_message_parameters_decl
  public static boolean event_message(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "event_message")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EVENT_MESSAGE, "<event message>");
    r = event_message_0(b, l + 1);
    r = r && consumeToken(b, EVENT);
    r = r && element_name_rule(b, l + 1);
    r = r && event_message_3(b, l + 1);
    r = r && type_application(b, l + 1);
    r = r && event_message_parameters_decl(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean event_message_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "event_message_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  // entity_id?
  private static boolean event_message_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "event_message_3")) return false;
    entity_id(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // LEFT_PAREN event_parameter_decl (COMMA event_parameter_decl)* COMMA? RIGHT_PAREN
  public static boolean event_message_parameters_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "event_message_parameters_decl")) return false;
    if (!nextTokenIs(b, LEFT_PAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_PAREN);
    r = r && event_parameter_decl(b, l + 1);
    r = r && event_message_parameters_decl_2(b, l + 1);
    r = r && event_message_parameters_decl_3(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    exit_section_(b, m, EVENT_MESSAGE_PARAMETERS_DECL, r);
    return r;
  }

  // (COMMA event_parameter_decl)*
  private static boolean event_message_parameters_decl_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "event_message_parameters_decl_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!event_message_parameters_decl_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "event_message_parameters_decl_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // COMMA event_parameter_decl
  private static boolean event_message_parameters_decl_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "event_message_parameters_decl_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && event_parameter_decl(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean event_message_parameters_decl_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "event_message_parameters_decl_3")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // element_info? (parameter_element | var_parameter_element)
  public static boolean event_parameter_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "event_parameter_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EVENT_PARAMETER_DECL, "<event parameter decl>");
    r = event_parameter_decl_0(b, l + 1);
    r = r && event_parameter_decl_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean event_parameter_decl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "event_parameter_decl_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  // parameter_element | var_parameter_element
  private static boolean event_parameter_decl_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "event_parameter_decl_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parameter_element(b, l + 1);
    if (!r) r = var_parameter_element(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // command_call | literal | var_expr
  public static boolean expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR, "<expr>");
    r = command_call(b, l + 1);
    if (!r) r = literal(b, l + 1);
    if (!r) r = var_expr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // FINAL
  public static boolean final_enum(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "final_enum")) return false;
    if (!nextTokenIs(b, FINAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FINAL);
    exit_section_(b, m, FINAL_ENUM, r);
    return r;
  }

  /* ********************************************************** */
  // (PLUS | MINUS)? NON_NEGATIVE_NUMBER DOT NON_NEGATIVE_NUMBER (("e" | "E") (PLUS | MINUS)? NON_NEGATIVE_NUMBER)?
  public static boolean float_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "float_literal")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FLOAT_LITERAL, "<float literal>");
    r = float_literal_0(b, l + 1);
    r = r && consumeTokens(b, 0, NON_NEGATIVE_NUMBER, DOT, NON_NEGATIVE_NUMBER);
    r = r && float_literal_4(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (PLUS | MINUS)?
  private static boolean float_literal_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "float_literal_0")) return false;
    float_literal_0_0(b, l + 1);
    return true;
  }

  // PLUS | MINUS
  private static boolean float_literal_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "float_literal_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, MINUS);
    exit_section_(b, m, null, r);
    return r;
  }

  // (("e" | "E") (PLUS | MINUS)? NON_NEGATIVE_NUMBER)?
  private static boolean float_literal_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "float_literal_4")) return false;
    float_literal_4_0(b, l + 1);
    return true;
  }

  // ("e" | "E") (PLUS | MINUS)? NON_NEGATIVE_NUMBER
  private static boolean float_literal_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "float_literal_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = float_literal_4_0_0(b, l + 1);
    r = r && float_literal_4_0_1(b, l + 1);
    r = r && consumeToken(b, NON_NEGATIVE_NUMBER);
    exit_section_(b, m, null, r);
    return r;
  }

  // "e" | "E"
  private static boolean float_literal_4_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "float_literal_4_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "e");
    if (!r) r = consumeToken(b, "E");
    exit_section_(b, m, null, r);
    return r;
  }

  // (PLUS | MINUS)?
  private static boolean float_literal_4_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "float_literal_4_0_1")) return false;
    float_literal_4_0_1_0(b, l + 1);
    return true;
  }

  // PLUS | MINUS
  private static boolean float_literal_4_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "float_literal_4_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, MINUS);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // element_name_rule?
  public static boolean generic_arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_arg")) return false;
    Marker m = enter_section_(b, l, _NONE_, GENERIC_ARG, "<generic arg>");
    element_name_rule(b, l + 1);
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // LT generic_arg (COMMA generic_arg)* GT
  public static boolean generic_args(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_args")) return false;
    if (!nextTokenIs(b, LT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LT);
    r = r && generic_arg(b, l + 1);
    r = r && generic_args_2(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, GENERIC_ARGS, r);
    return r;
  }

  // (COMMA generic_arg)*
  private static boolean generic_args_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_args_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!generic_args_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "generic_args_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // COMMA generic_arg
  private static boolean generic_args_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_args_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && generic_arg(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LT type_unit_application (COMMA type_unit_application?)* COMMA? GT
  public static boolean generic_parameters(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_parameters")) return false;
    if (!nextTokenIs(b, LT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LT);
    r = r && type_unit_application(b, l + 1);
    r = r && generic_parameters_2(b, l + 1);
    r = r && generic_parameters_3(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, GENERIC_PARAMETERS, r);
    return r;
  }

  // (COMMA type_unit_application?)*
  private static boolean generic_parameters_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_parameters_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!generic_parameters_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "generic_parameters_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // COMMA type_unit_application?
  private static boolean generic_parameters_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_parameters_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && generic_parameters_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // type_unit_application?
  private static boolean generic_parameters_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_parameters_2_0_1")) return false;
    type_unit_application(b, l + 1);
    return true;
  }

  // COMMA?
  private static boolean generic_parameters_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_parameters_3")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // element_name_rule import_element_as?
  public static boolean import_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_element")) return false;
    if (!nextTokenIs(b, "<import element>", ELEMENT_NAME_TOKEN, ESCAPED_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IMPORT_ELEMENT, "<import element>");
    r = element_name_rule(b, l + 1);
    r = r && import_element_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // import_element_as?
  private static boolean import_element_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_element_1")) return false;
    import_element_as(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // AS element_name_rule
  public static boolean import_element_as(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_element_as")) return false;
    if (!nextTokenIs(b, AS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, AS);
    r = r && element_name_rule(b, l + 1);
    exit_section_(b, m, IMPORT_ELEMENT_AS, r);
    return r;
  }

  /* ********************************************************** */
  // STAR
  public static boolean import_element_star(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_element_star")) return false;
    if (!nextTokenIs(b, STAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, STAR);
    exit_section_(b, m, IMPORT_ELEMENT_STAR, r);
    return r;
  }

  /* ********************************************************** */
  // IMPORT element_id (DOT (LEFT_BRACE import_element (COMMA import_element)* COMMA? RIGHT_BRACE | import_element_star))?
  public static boolean import_stmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_stmt")) return false;
    if (!nextTokenIs(b, IMPORT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IMPORT);
    r = r && element_id(b, l + 1);
    r = r && import_stmt_2(b, l + 1);
    exit_section_(b, m, IMPORT_STMT, r);
    return r;
  }

  // (DOT (LEFT_BRACE import_element (COMMA import_element)* COMMA? RIGHT_BRACE | import_element_star))?
  private static boolean import_stmt_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_stmt_2")) return false;
    import_stmt_2_0(b, l + 1);
    return true;
  }

  // DOT (LEFT_BRACE import_element (COMMA import_element)* COMMA? RIGHT_BRACE | import_element_star)
  private static boolean import_stmt_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_stmt_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOT);
    r = r && import_stmt_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // LEFT_BRACE import_element (COMMA import_element)* COMMA? RIGHT_BRACE | import_element_star
  private static boolean import_stmt_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_stmt_2_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = import_stmt_2_0_1_0(b, l + 1);
    if (!r) r = import_element_star(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // LEFT_BRACE import_element (COMMA import_element)* COMMA? RIGHT_BRACE
  private static boolean import_stmt_2_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_stmt_2_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_BRACE);
    r = r && import_element(b, l + 1);
    r = r && import_stmt_2_0_1_0_2(b, l + 1);
    r = r && import_stmt_2_0_1_0_3(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA import_element)*
  private static boolean import_stmt_2_0_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_stmt_2_0_1_0_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!import_stmt_2_0_1_0_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "import_stmt_2_0_1_0_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // COMMA import_element
  private static boolean import_stmt_2_0_1_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_stmt_2_0_1_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && import_element(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean import_stmt_2_0_1_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_stmt_2_0_1_0_3")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // NON_NEGATIVE_NUMBER
  public static boolean length_from(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "length_from")) return false;
    if (!nextTokenIs(b, NON_NEGATIVE_NUMBER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NON_NEGATIVE_NUMBER);
    exit_section_(b, m, LENGTH_FROM, r);
    return r;
  }

  /* ********************************************************** */
  // NON_NEGATIVE_NUMBER
  public static boolean length_to(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "length_to")) return false;
    if (!nextTokenIs(b, NON_NEGATIVE_NUMBER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NON_NEGATIVE_NUMBER);
    exit_section_(b, m, LENGTH_TO, r);
    return r;
  }

  /* ********************************************************** */
  // numeric_literal | bool_literal
  public static boolean literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "literal")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, LITERAL, "<literal>");
    r = numeric_literal(b, l + 1);
    if (!r) r = bool_literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // status_message | event_message
  public static boolean message_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "message_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MESSAGE_DECL, "<message decl>");
    r = status_message(b, l + 1);
    if (!r) r = event_message(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // element_info? NAMESPACE element_id
  public static boolean namespace_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NAMESPACE_DECL, "<namespace decl>");
    r = namespace_decl_0(b, l + 1);
    r = r && consumeToken(b, NAMESPACE);
    r = r && element_id(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean namespace_decl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_decl_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // NATIVE
  public static boolean native_type_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "native_type_decl")) return false;
    if (!nextTokenIs(b, NATIVE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NATIVE);
    exit_section_(b, m, NATIVE_TYPE_DECL, r);
    return r;
  }

  /* ********************************************************** */
  // NON_NEGATIVE_NUMBER
  public static boolean non_negative_integer_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "non_negative_integer_literal")) return false;
    if (!nextTokenIs(b, NON_NEGATIVE_NUMBER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NON_NEGATIVE_NUMBER);
    exit_section_(b, m, NON_NEGATIVE_INTEGER_LITERAL, r);
    return r;
  }

  /* ********************************************************** */
  // float_literal | non_negative_integer_literal
  public static boolean numeric_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "numeric_literal")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NUMERIC_LITERAL, "<numeric literal>");
    r = float_literal(b, l + 1);
    if (!r) r = non_negative_integer_literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // numeric_literal DOTS numeric_literal
  public static boolean numeric_range_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "numeric_range_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NUMERIC_RANGE_DECL, "<numeric range decl>");
    r = numeric_literal(b, l + 1);
    r = r && consumeToken(b, DOTS);
    r = r && numeric_literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // QUESTION
  public static boolean optional(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "optional")) return false;
    if (!nextTokenIs(b, QUESTION)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, QUESTION);
    exit_section_(b, m, OPTIONAL, r);
    return r;
  }

  /* ********************************************************** */
  // element_info? parameter_element
  public static boolean parameter_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER_DECL, "<parameter decl>");
    r = parameter_decl_0(b, l + 1);
    r = r && parameter_element(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean parameter_decl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_decl_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // element_name_rule (range_decl | DOT element_name_rule)*
  public static boolean parameter_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_element")) return false;
    if (!nextTokenIs(b, "<parameter element>", ELEMENT_NAME_TOKEN, ESCAPED_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER_ELEMENT, "<parameter element>");
    r = element_name_rule(b, l + 1);
    r = r && parameter_element_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (range_decl | DOT element_name_rule)*
  private static boolean parameter_element_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_element_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!parameter_element_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parameter_element_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // range_decl | DOT element_name_rule
  private static boolean parameter_element_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_element_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = range_decl(b, l + 1);
    if (!r) r = parameter_element_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // DOT element_name_rule
  private static boolean parameter_element_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_element_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOT);
    r = r && element_name_rule(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LEFT_BRACKET (non_negative_integer_literal (DOTS range_upper_bound_decl)? | STAR) RIGHT_BRACKET
  public static boolean range_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_decl")) return false;
    if (!nextTokenIs(b, LEFT_BRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_BRACKET);
    r = r && range_decl_1(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACKET);
    exit_section_(b, m, RANGE_DECL, r);
    return r;
  }

  // non_negative_integer_literal (DOTS range_upper_bound_decl)? | STAR
  private static boolean range_decl_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_decl_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = range_decl_1_0(b, l + 1);
    if (!r) r = consumeToken(b, STAR);
    exit_section_(b, m, null, r);
    return r;
  }

  // non_negative_integer_literal (DOTS range_upper_bound_decl)?
  private static boolean range_decl_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_decl_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = non_negative_integer_literal(b, l + 1);
    r = r && range_decl_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (DOTS range_upper_bound_decl)?
  private static boolean range_decl_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_decl_1_0_1")) return false;
    range_decl_1_0_1_0(b, l + 1);
    return true;
  }

  // DOTS range_upper_bound_decl
  private static boolean range_decl_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_decl_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOTS);
    r = r && range_upper_bound_decl(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // non_negative_integer_literal | STAR
  public static boolean range_upper_bound_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_upper_bound_decl")) return false;
    if (!nextTokenIs(b, "<range upper bound decl>", NON_NEGATIVE_NUMBER, STAR)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, RANGE_UPPER_BOUND_DECL, "<range upper bound decl>");
    r = non_negative_integer_literal(b, l + 1);
    if (!r) r = consumeToken(b, STAR);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // SCRIPT element_name_rule FOR component_ref LEFT_PAREN command_args? RIGHT_PAREN
  //     (COLON type_unit_application)? EQ_SIGN LEFT_BRACE expr* RIGHT_BRACE
  public static boolean script_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "script_decl")) return false;
    if (!nextTokenIs(b, SCRIPT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SCRIPT);
    r = r && element_name_rule(b, l + 1);
    r = r && consumeToken(b, FOR);
    r = r && component_ref(b, l + 1);
    r = r && consumeToken(b, LEFT_PAREN);
    r = r && script_decl_5(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    r = r && script_decl_7(b, l + 1);
    r = r && consumeTokens(b, 0, EQ_SIGN, LEFT_BRACE);
    r = r && script_decl_10(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACE);
    exit_section_(b, m, SCRIPT_DECL, r);
    return r;
  }

  // command_args?
  private static boolean script_decl_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "script_decl_5")) return false;
    command_args(b, l + 1);
    return true;
  }

  // (COLON type_unit_application)?
  private static boolean script_decl_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "script_decl_7")) return false;
    script_decl_7_0(b, l + 1);
    return true;
  }

  // COLON type_unit_application
  private static boolean script_decl_7_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "script_decl_7_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && type_unit_application(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr*
  private static boolean script_decl_10(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "script_decl_10")) return false;
    int c = current_position_(b);
    while (true) {
      if (!expr(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "script_decl_10", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // element_id generic_parameters?
  public static boolean simple_or_generic_type_application(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_or_generic_type_application")) return false;
    if (!nextTokenIs(b, "<simple or generic type application>", ELEMENT_NAME_TOKEN, ESCAPED_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SIMPLE_OR_GENERIC_TYPE_APPLICATION, "<simple or generic type application>");
    r = element_id(b, l + 1);
    r = r && simple_or_generic_type_application_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // generic_parameters?
  private static boolean simple_or_generic_type_application_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_or_generic_type_application_1")) return false;
    generic_parameters(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // element_info? STATUS element_name_rule entity_id? (PRIORITY EQ_SIGN NON_NEGATIVE_NUMBER)? status_message_parameters_decl
  public static boolean status_message(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "status_message")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STATUS_MESSAGE, "<status message>");
    r = status_message_0(b, l + 1);
    r = r && consumeToken(b, STATUS);
    r = r && element_name_rule(b, l + 1);
    r = r && status_message_3(b, l + 1);
    r = r && status_message_4(b, l + 1);
    r = r && status_message_parameters_decl(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean status_message_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "status_message_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  // entity_id?
  private static boolean status_message_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "status_message_3")) return false;
    entity_id(b, l + 1);
    return true;
  }

  // (PRIORITY EQ_SIGN NON_NEGATIVE_NUMBER)?
  private static boolean status_message_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "status_message_4")) return false;
    status_message_4_0(b, l + 1);
    return true;
  }

  // PRIORITY EQ_SIGN NON_NEGATIVE_NUMBER
  private static boolean status_message_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "status_message_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, PRIORITY, EQ_SIGN, NON_NEGATIVE_NUMBER);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LEFT_PAREN parameter_decl (COMMA parameter_decl)* COMMA? RIGHT_PAREN
  public static boolean status_message_parameters_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "status_message_parameters_decl")) return false;
    if (!nextTokenIs(b, LEFT_PAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_PAREN);
    r = r && parameter_decl(b, l + 1);
    r = r && status_message_parameters_decl_2(b, l + 1);
    r = r && status_message_parameters_decl_3(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    exit_section_(b, m, STATUS_MESSAGE_PARAMETERS_DECL, r);
    return r;
  }

  // (COMMA parameter_decl)*
  private static boolean status_message_parameters_decl_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "status_message_parameters_decl_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!status_message_parameters_decl_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "status_message_parameters_decl_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // COMMA parameter_decl
  private static boolean status_message_parameters_decl_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "status_message_parameters_decl_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && parameter_decl(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean status_message_parameters_decl_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "status_message_parameters_decl_3")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // STRING | STRING_UNARY_QUOTES
  public static boolean string_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_literal")) return false;
    if (!nextTokenIs(b, "<string literal>", STRING, STRING_UNARY_QUOTES)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STRING_LITERAL, "<string literal>");
    r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, STRING_UNARY_QUOTES);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // element_name_rule? string_literal
  public static boolean string_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STRING_VALUE, "<string value>");
    r = string_value_0(b, l + 1);
    r = r && string_literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_name_rule?
  private static boolean string_value_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_value_0")) return false;
    element_name_rule(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // STRUCT LEFT_PAREN command_args RIGHT_PAREN
  public static boolean struct_type_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_type_decl")) return false;
    if (!nextTokenIs(b, STRUCT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, STRUCT, LEFT_PAREN);
    r = r && command_args(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    exit_section_(b, m, STRUCT_TYPE_DECL, r);
    return r;
  }

  /* ********************************************************** */
  // (simple_or_generic_type_application | array_type_application) optional?
  public static boolean type_application(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_application")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_APPLICATION, "<type application>");
    r = type_application_0(b, l + 1);
    r = r && type_application_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // simple_or_generic_type_application | array_type_application
  private static boolean type_application_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_application_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = simple_or_generic_type_application(b, l + 1);
    if (!r) r = array_type_application(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // optional?
  private static boolean type_application_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_application_1")) return false;
    optional(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // element_info? TYPE_KEYWORD element_name_rule generic_args? type_decl_body
  public static boolean type_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_DECL, "<type decl>");
    r = type_decl_0(b, l + 1);
    r = r && consumeToken(b, TYPE_KEYWORD);
    r = r && element_name_rule(b, l + 1);
    r = r && type_decl_3(b, l + 1);
    r = r && type_decl_body(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean type_decl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_decl_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  // generic_args?
  private static boolean type_decl_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_decl_3")) return false;
    generic_args(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // type_application (RANGE numeric_range_decl)? | enum_type_decl | struct_type_decl | native_type_decl
  public static boolean type_decl_body(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_decl_body")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_DECL_BODY, "<type decl body>");
    r = type_decl_body_0(b, l + 1);
    if (!r) r = enum_type_decl(b, l + 1);
    if (!r) r = struct_type_decl(b, l + 1);
    if (!r) r = native_type_decl(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // type_application (RANGE numeric_range_decl)?
  private static boolean type_decl_body_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_decl_body_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = type_application(b, l + 1);
    r = r && type_decl_body_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (RANGE numeric_range_decl)?
  private static boolean type_decl_body_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_decl_body_0_1")) return false;
    type_decl_body_0_1_0(b, l + 1);
    return true;
  }

  // RANGE numeric_range_decl
  private static boolean type_decl_body_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_decl_body_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, RANGE);
    r = r && numeric_range_decl(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // type_application unit?
  public static boolean type_unit_application(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_unit_application")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_UNIT_APPLICATION, "<type unit application>");
    r = type_application(b, l + 1);
    r = r && type_unit_application_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // unit?
  private static boolean type_unit_application_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_unit_application_1")) return false;
    unit(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // SLASH element_id SLASH
  public static boolean unit(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unit")) return false;
    if (!nextTokenIs(b, SLASH)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SLASH);
    r = r && element_id(b, l + 1);
    r = r && consumeToken(b, SLASH);
    exit_section_(b, m, UNIT, r);
    return r;
  }

  /* ********************************************************** */
  // element_info? UNIT_TOKEN element_name_rule (DISPLAY string_value)* (PLACEMENT (BEFORE | AFTER))?
  public static boolean unit_decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unit_decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, UNIT_DECL, "<unit decl>");
    r = unit_decl_0(b, l + 1);
    r = r && consumeToken(b, UNIT_TOKEN);
    r = r && element_name_rule(b, l + 1);
    r = r && unit_decl_3(b, l + 1);
    r = r && unit_decl_4(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // element_info?
  private static boolean unit_decl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unit_decl_0")) return false;
    element_info(b, l + 1);
    return true;
  }

  // (DISPLAY string_value)*
  private static boolean unit_decl_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unit_decl_3")) return false;
    int c = current_position_(b);
    while (true) {
      if (!unit_decl_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "unit_decl_3", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // DISPLAY string_value
  private static boolean unit_decl_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unit_decl_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DISPLAY);
    r = r && string_value(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (PLACEMENT (BEFORE | AFTER))?
  private static boolean unit_decl_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unit_decl_4")) return false;
    unit_decl_4_0(b, l + 1);
    return true;
  }

  // PLACEMENT (BEFORE | AFTER)
  private static boolean unit_decl_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unit_decl_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLACEMENT);
    r = r && unit_decl_4_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // BEFORE | AFTER
  private static boolean unit_decl_4_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unit_decl_4_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, BEFORE);
    if (!r) r = consumeToken(b, AFTER);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // element_name_rule
  public static boolean var_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_expr")) return false;
    if (!nextTokenIs(b, "<var expr>", ELEMENT_NAME_TOKEN, ESCAPED_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VAR_EXPR, "<var expr>");
    r = element_name_rule(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // VAR element_name_rule COLON type_unit_application
  public static boolean var_parameter_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_parameter_element")) return false;
    if (!nextTokenIs(b, VAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VAR);
    r = r && element_name_rule(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && type_unit_application(b, l + 1);
    exit_section_(b, m, VAR_PARAMETER_ELEMENT, r);
    return r;
  }

}
