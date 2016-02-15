package ru.mipt.acsl.decode.parser;
import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static ru.mipt.acsl.decode.parser.psi.DecodeTypes.*;

%%

%{
  public _DecodeLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _DecodeLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL="\r"|"\n"|"\r\n"
LINE_WS=[\ \t\f]
WHITE_SPACE=({LINE_WS}|{EOL})+

ESCAPED_NAME=\^[a-zA-Z_][a-zA-Z0-9_]*
ELEMENT_NAME_TOKEN=[a-zA-Z_][a-zA-Z0-9_]*
COMMENT=#[^\r\n]*
MULTILINE_COMMENT="/"\*([^\*][^/\*]?)*\*"/"
STRING=\"([^\"\\]|\\.)*\"
STRING_UNARY_QUOTES='([^'\\]|\\.)*'
NON_NEGATIVE_NUMBER=[0-9]+

%%
<YYINITIAL> {
  {WHITE_SPACE}              { return com.intellij.psi.TokenType.WHITE_SPACE; }

  "namespace"                { return NAMESPACE; }
  "component"                { return COMPONENT; }
  "command"                  { return COMMAND; }
  "{"                        { return LEFT_BRACE; }
  "}"                        { return RIGHT_BRACE; }
  "("                        { return LEFT_PAREN; }
  ")"                        { return RIGHT_PAREN; }
  "["                        { return LEFT_BRACKET; }
  "]"                        { return RIGHT_BRACKET; }
  ","                        { return COMMA; }
  "="                        { return EQ_SIGN; }
  "status"                   { return STATUS; }
  "message"                  { return MESSAGE; }
  "event"                    { return EVENT; }
  "dynamic"                  { return DYNAMIC; }
  "parameter"                { return PARAMETER; }
  "info"                     { return INFO; }
  "subcomponent"             { return SUBCOMPONENT; }
  "parameters"               { return PARAMETERS; }
  "unit"                     { return UNIT_TOKEN; }
  "type"                     { return TYPE_KEYWORD; }
  "alias"                    { return ALIAS; }
  ".."                       { return DOTS; }
  "."                        { return DOT; }
  "uint"                     { return UINT; }
  "int"                      { return INT; }
  "float"                    { return FLOAT; }
  "bool"                     { return BOOL; }
  "array"                    { return ARRAY; }
  "ber"                      { return BER; }
  "enum"                     { return ENUM; }
  "placement"                { return PLACEMENT; }
  "before"                   { return BEFORE; }
  "after"                    { return AFTER; }
  "base_type"                { return BASE_TYPE; }
  "struct"                   { return STRUCT; }
  "/"                        { return SLASH; }
  "*"                        { return STAR; }
  "+"                        { return PLUS; }
  "-"                        { return MINUS; }
  ":"                        { return COLON; }
  "->"                       { return ARROW; }
  "<"                        { return LT; }
  ">"                        { return GT; }
  "display"                  { return DISPLAY; }
  "true"                     { return TRUE; }
  "false"                    { return FALSE; }
  "with"                     { return WITH; }
  "?"                        { return QUESTION; }
  "import"                   { return IMPORT; }
  "as"                       { return AS; }
  "id"                       { return ID; }
  "language"                 { return LANGUAGE; }
  "default"                  { return DEFAULT; }
  "priority"                 { return PRIORITY; }
  "var"                      { return VAR; }

  {ESCAPED_NAME}             { return ESCAPED_NAME; }
  {ELEMENT_NAME_TOKEN}       { return ELEMENT_NAME_TOKEN; }
  {COMMENT}                  { return COMMENT; }
  {MULTILINE_COMMENT}        { return MULTILINE_COMMENT; }
  {STRING}                   { return STRING; }
  {STRING_UNARY_QUOTES}      { return STRING_UNARY_QUOTES; }
  {NON_NEGATIVE_NUMBER}      { return NON_NEGATIVE_NUMBER; }

  [^] { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}
