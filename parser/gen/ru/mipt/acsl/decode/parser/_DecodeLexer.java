/* The following code was generated by JFlex 1.4.3 on 5/24/16 1:15 AM */

package ru.mipt.acsl.decode.parser;
import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static ru.mipt.acsl.decode.parser.psi.DecodeTypes.*;


/**
 * This class is a scanner generated by 
 * <a href="http://www.jflex.de/">JFlex</a> 1.4.3
 * on 5/24/16 1:15 AM from the specification file
 * <tt>/home/metadeus/projects/ACSL/geo-target/decode/parser/_DecodeLexer.flex</tt>
 */
public class _DecodeLexer implements FlexLexer {
  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = { 
     0, 0
  };

  /** 
   * Translates characters to character classes
   */
  private static final String ZZ_CMAP_PACKED = 
    "\11\0\1\3\1\2\1\0\1\3\1\1\22\0\1\3\1\0\1\12"+
    "\1\7\3\0\1\14\1\31\1\32\1\11\1\51\1\35\1\52\1\47"+
    "\1\10\12\6\1\53\2\0\1\36\1\54\1\57\1\61\32\5\1\33"+
    "\1\13\1\34\1\4\1\5\1\0\1\16\1\45\1\23\1\26\1\20"+
    "\1\50\1\40\1\56\1\43\2\5\1\46\1\17\1\15\1\24\1\22"+
    "\1\5\1\44\1\21\1\25\1\37\1\41\1\55\1\60\1\42\1\5"+
    "\1\27\1\0\1\30\uff82\0";

  /** 
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\1\0\1\1\1\2\1\1\1\3\1\4\1\5\1\6"+
    "\1\7\2\1\11\3\1\10\1\11\1\12\1\13\1\14"+
    "\1\15\1\16\1\17\6\3\1\20\1\3\1\21\1\22"+
    "\1\23\1\3\1\24\1\25\1\26\2\0\1\27\3\0"+
    "\1\30\1\3\1\31\31\3\1\32\3\3\1\33\1\3"+
    "\1\0\27\3\1\34\5\3\1\35\2\3\1\36\6\3"+
    "\1\37\13\3\1\40\1\41\3\3\1\42\6\3\1\43"+
    "\2\3\1\44\1\45\1\46\1\3\1\47\16\3\1\50"+
    "\2\3\1\51\1\52\1\3\1\53\2\3\1\54\1\55"+
    "\1\56\11\3\1\57\1\60\2\3\1\61\1\62\4\3"+
    "\1\63\1\3\1\64\1\65\1\66\4\3\1\67\2\3"+
    "\1\70\1\71\1\3\1\72\1\73\1\74\1\3\1\75"+
    "\1\3\1\76";

  private static int [] zzUnpackAction() {
    int [] result = new int[222];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /** 
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\62\0\144\0\226\0\310\0\372\0\u012c\0\u015e"+
    "\0\62\0\u0190\0\u01c2\0\u01f4\0\u0226\0\u0258\0\u028a\0\u02bc"+
    "\0\u02ee\0\u0320\0\u0352\0\u0384\0\62\0\62\0\62\0\62"+
    "\0\62\0\62\0\62\0\62\0\u03b6\0\u03e8\0\u041a\0\u044c"+
    "\0\u047e\0\u04b0\0\u04e2\0\u0514\0\62\0\u0546\0\62\0\u0578"+
    "\0\62\0\62\0\u05aa\0\u05dc\0\u0190\0\62\0\u060e\0\u01c2"+
    "\0\u0640\0\62\0\u0672\0\310\0\u06a4\0\u06d6\0\u0708\0\u073a"+
    "\0\u076c\0\u079e\0\u07d0\0\u0802\0\u0834\0\u0866\0\u0898\0\u08ca"+
    "\0\u08fc\0\u092e\0\u0960\0\u0992\0\u09c4\0\u09f6\0\u0a28\0\u0a5a"+
    "\0\u0a8c\0\u0abe\0\u0af0\0\u0b22\0\u0b54\0\62\0\u0b86\0\u0bb8"+
    "\0\u0bea\0\62\0\u0c1c\0\u0c4e\0\u0c80\0\u0cb2\0\u0ce4\0\u0d16"+
    "\0\u0d48\0\u0d7a\0\u0dac\0\u0dde\0\u0e10\0\u0e42\0\u0e74\0\u0ea6"+
    "\0\u0ed8\0\u0f0a\0\u0f3c\0\u0f6e\0\u0fa0\0\u0fd2\0\u1004\0\u1036"+
    "\0\u1068\0\u109a\0\u10cc\0\310\0\u10fe\0\u1130\0\u1162\0\u1194"+
    "\0\u11c6\0\310\0\u11f8\0\u122a\0\62\0\u125c\0\u128e\0\u12c0"+
    "\0\u12f2\0\u1324\0\u1356\0\310\0\u1388\0\u13ba\0\u13ec\0\u141e"+
    "\0\u1450\0\u1482\0\u14b4\0\u14e6\0\u1518\0\u154a\0\u157c\0\310"+
    "\0\310\0\u15ae\0\u15e0\0\u1612\0\310\0\u1644\0\u1676\0\u16a8"+
    "\0\u16da\0\u170c\0\u173e\0\310\0\u1770\0\u17a2\0\310\0\310"+
    "\0\310\0\u17d4\0\310\0\u1806\0\u1838\0\u186a\0\u189c\0\u18ce"+
    "\0\u1900\0\u1932\0\u1964\0\u1996\0\u19c8\0\u19fa\0\u1a2c\0\u1a5e"+
    "\0\u1a90\0\310\0\u1ac2\0\u1af4\0\310\0\310\0\u1b26\0\310"+
    "\0\u1b58\0\u1b8a\0\310\0\310\0\310\0\u1bbc\0\u1bee\0\u1c20"+
    "\0\u1c52\0\u1c84\0\u1cb6\0\u1ce8\0\u1d1a\0\u1d4c\0\310\0\310"+
    "\0\u1d7e\0\u1db0\0\310\0\310\0\u1de2\0\u1e14\0\u1e46\0\u1e78"+
    "\0\310\0\u1eaa\0\310\0\310\0\310\0\u1edc\0\u1f0e\0\u1f40"+
    "\0\u1f72\0\310\0\u1fa4\0\u1fd6\0\310\0\310\0\u2008\0\u203a"+
    "\0\310\0\310\0\u206c\0\310\0\u209e\0\310";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[222];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /** 
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\2\3\3\1\4\1\5\1\6\1\7\1\10\1\11"+
    "\1\12\1\2\1\13\1\14\1\15\1\16\1\17\1\20"+
    "\1\21\1\22\1\5\1\23\1\24\1\25\1\26\1\27"+
    "\1\30\1\31\1\32\1\33\1\34\1\35\1\5\1\36"+
    "\1\5\1\37\1\40\1\41\1\42\1\43\1\44\1\45"+
    "\1\46\1\47\1\2\1\50\1\5\1\51\1\5\1\52"+
    "\63\0\3\3\63\0\1\53\7\0\12\53\10\0\10\53"+
    "\1\0\1\53\4\0\2\53\1\0\1\53\6\0\2\5"+
    "\6\0\12\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\7\0\1\6\53\0\1\7\2\0\57\7"+
    "\11\0\1\54\50\0\12\55\1\56\1\57\46\55\13\60"+
    "\1\61\1\62\45\60\5\0\2\5\6\0\1\5\1\63"+
    "\10\5\10\0\10\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\4\5\1\64\5\5\10\0"+
    "\5\5\1\65\1\5\1\66\1\0\1\67\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\3\5\1\70\6\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\1\71\11\5\10\0\2\5\1\72"+
    "\5\5\1\0\1\5\4\0\2\5\1\0\1\73\6\0"+
    "\2\5\6\0\6\5\1\74\1\5\1\75\1\5\10\0"+
    "\1\76\7\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\1\5\1\77\10\5\10\0\5\5"+
    "\1\100\1\5\1\101\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\7\5\1\102\2\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\12\5\10\0\3\5\1\103\1\5\1\104"+
    "\2\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\3\5\1\105\6\5\10\0\3\5\1\106"+
    "\1\107\3\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\1\110\11\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\1\5\1\111\10\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\2\5\1\112"+
    "\7\5\10\0\10\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\1\5\1\113\10\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\3\5\1\114\6\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\1\5\1\115\10\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\50\0\1\116\17\0\2\5\6\0"+
    "\1\5\1\117\5\5\1\120\2\5\10\0\4\5\1\121"+
    "\3\5\1\0\1\5\4\0\2\5\1\0\1\5\55\0"+
    "\1\122\12\0\2\5\6\0\12\5\10\0\4\5\1\123"+
    "\3\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\53\6\0\12\53\10\0\10\53\1\0\1\53\4\0"+
    "\2\53\1\0\1\53\1\0\11\54\1\124\50\54\2\55"+
    "\1\0\57\55\2\60\1\0\57\60\5\0\2\5\6\0"+
    "\2\5\1\125\5\5\1\126\1\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\12\5\10\0\5\5\1\127\2\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\12\5\10\0"+
    "\4\5\1\130\3\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\10\5\1\131\1\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\4\5\1\132\5\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\12\5\10\0\1\133\7\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\3\5\1\134\6\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\10\5\1\135\1\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\12\5\10\0\5\5\1\136\2\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\1\5"+
    "\1\137\10\5\10\0\5\5\1\140\2\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\12\5"+
    "\10\0\6\5\1\141\1\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\12\5\10\0\5\5"+
    "\1\142\2\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\12\5\10\0\4\5\1\143\3\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\1\5\1\144\10\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\2\5"+
    "\1\145\7\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\5\5\1\146\4\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\12\5\10\0\1\147\7\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\12\5\10\0\10\5\1\0\1\150\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\1\151\11\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\4\5\1\152\5\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\12\5"+
    "\10\0\4\5\1\153\3\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\12\5\10\0\5\5"+
    "\1\154\2\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\5\5\1\155\4\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\1\156\11\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\12\5\10\0"+
    "\10\5\1\0\1\157\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\1\160\11\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\12\5"+
    "\10\0\7\5\1\161\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\12\5\10\0\5\5\1\162"+
    "\2\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\1\163\11\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\10\5"+
    "\1\164\1\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\11\0\1\165\56\0\2\5\6\0\3\5"+
    "\1\166\6\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\12\5\10\0\4\5"+
    "\1\167\3\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\1\5\1\170\10\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\1\5\1\171\10\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\3\5"+
    "\1\172\6\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\4\5\1\173\5\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\2\5\1\174\7\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\1\175\11\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\3\5\1\176"+
    "\6\5\10\0\10\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\12\5\10\0\4\5\1\177"+
    "\3\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\10\5\1\200\1\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\12\5\10\0\1\201\7\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\6\5\1\202\3\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\1\5\1\203\10\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\7\5\1\204\2\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\6\5"+
    "\1\205\3\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\2\5\1\206\2\5"+
    "\1\207\4\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\3\5\1\210\6\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\3\5\1\211\6\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\1\5\1\212\10\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\1\5"+
    "\1\213\10\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\5\5\1\214\4\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\10\5\1\215\1\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\7\5\1\216\2\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\12\5"+
    "\10\0\1\5\1\217\6\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\7\5\1\220\2\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\12\5\10\0\1\5\1\221\6\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\4\5\1\222\5\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\1\5"+
    "\1\223\10\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\12\5\10\0\10\5"+
    "\1\0\1\5\4\0\1\5\1\224\1\0\1\5\6\0"+
    "\2\5\6\0\4\5\1\225\5\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\12\5\10\0\2\5\1\226\5\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\12\5\10\0"+
    "\3\5\1\227\4\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\4\5\1\230\5\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\12\5\10\0\5\5\1\231\2\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\1\5\1\232\10\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\10\5\1\233"+
    "\1\5\10\0\10\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\1\234\11\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\5\5\1\235\4\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\12\5"+
    "\10\0\1\236\7\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\6\5\1\237\3\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\7\5\1\240\2\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\2\5\1\241\7\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\12\5\10\0"+
    "\5\5\1\242\2\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\3\5\1\243\6\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\1\5\1\244\10\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\7\5\1\245\2\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\12\5\10\0"+
    "\1\246\7\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\2\5\1\247\7\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\12\5\10\0\7\5\1\250\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\12\5\10\0"+
    "\5\5\1\251\2\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\3\5\1\252\6\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\12\5\10\0\5\5\1\253\2\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\12\5\10\0\1\254\7\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\3\5\1\255\6\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\12\5\10\0\7\5\1\256\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\5\5\1\257\4\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\3\5\1\260"+
    "\6\5\10\0\10\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\12\5\10\0\1\5\1\261"+
    "\6\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\11\5\1\262\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\10\5"+
    "\1\263\1\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\4\5\1\264\5\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\10\5\1\265\1\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\2\5\1\266\7\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\3\5"+
    "\1\267\6\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\12\5\10\0\4\5"+
    "\1\270\3\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\2\5\1\271\7\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\1\272\11\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\1\273\11\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\12\5\10\0\7\5\1\274\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\12\5\10\0\4\5\1\275\3\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\1\5\1\276"+
    "\10\5\10\0\10\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\10\5\1\277\1\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\3\5\1\300\6\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\1\5\1\301\10\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\1\5\1\302"+
    "\10\5\10\0\10\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\3\5\1\303\6\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\4\5\1\304\5\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\5\5\1\305\4\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\10\5\1\306"+
    "\1\5\10\0\10\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\10\5\1\307\1\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\3\5\1\310\6\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\11\5\1\311\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\3\5\1\312\6\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\10\5\1\313\1\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\6\5\1\314\3\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\12\5"+
    "\10\0\3\5\1\315\4\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\12\5\10\0\1\5"+
    "\1\316\6\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\6\5\1\317\3\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\7\5\1\320\2\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\6\0\2\5\6\0\3\5"+
    "\1\321\6\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\12\5\10\0\3\5"+
    "\1\322\4\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\1\323\11\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\1\324\11\5\10\0\10\5\1\0\1\5\4\0\2\5"+
    "\1\0\1\5\6\0\2\5\6\0\3\5\1\325\6\5"+
    "\10\0\10\5\1\0\1\5\4\0\2\5\1\0\1\5"+
    "\6\0\2\5\6\0\3\5\1\326\6\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\1\327\11\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\12\5\10\0"+
    "\5\5\1\330\2\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\10\5\1\331\1\5\10\0"+
    "\10\5\1\0\1\5\4\0\2\5\1\0\1\5\6\0"+
    "\2\5\6\0\10\5\1\332\1\5\10\0\10\5\1\0"+
    "\1\5\4\0\2\5\1\0\1\5\6\0\2\5\6\0"+
    "\3\5\1\333\6\5\10\0\10\5\1\0\1\5\4\0"+
    "\2\5\1\0\1\5\6\0\2\5\6\0\4\5\1\334"+
    "\5\5\10\0\10\5\1\0\1\5\4\0\2\5\1\0"+
    "\1\5\6\0\2\5\6\0\1\335\11\5\10\0\10\5"+
    "\1\0\1\5\4\0\2\5\1\0\1\5\6\0\2\5"+
    "\6\0\10\5\1\336\1\5\10\0\10\5\1\0\1\5"+
    "\4\0\2\5\1\0\1\5\1\0";

  private static int [] zzUnpackTrans() {
    int [] result = new int[8400];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;
  private static final char[] EMPTY_BUFFER = new char[0];
  private static final int YYEOF = -1;
  private static java.io.Reader zzReader = null; // Fake

  /* error messages for the codes above */
  private static final String ZZ_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\1\0\1\11\6\1\1\11\13\1\10\11\10\1\1\11"+
    "\1\1\1\11\1\1\2\11\1\1\2\0\1\11\3\0"+
    "\1\11\33\1\1\11\3\1\1\11\1\1\1\0\40\1"+
    "\1\11\151\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[222];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private CharSequence zzBuffer = "";

  /** this buffer may contains the current text array to be matched when it is cheap to acquire it */
  private char[] zzBufferArray;

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the textposition at the last state to be included in yytext */
  private int zzPushbackPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /**
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /* user code: */
  public _DecodeLexer() {
    this((java.io.Reader)null);
  }


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public _DecodeLexer(java.io.Reader in) {
    this.zzReader = in;
  }


  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    char [] map = new char[0x10000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 128) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }

  public final int getTokenStart(){
    return zzStartRead;
  }

  public final int getTokenEnd(){
    return getTokenStart() + yylength();
  }

  public void reset(CharSequence buffer, int start, int end,int initialState){
    zzBuffer = buffer;
    zzBufferArray = com.intellij.util.text.CharArrayUtil.fromSequenceWithoutCopying(buffer);
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzPushbackPos = 0;
    zzAtEOF  = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }

  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
  }


  /**
   * Returns the character at position <tt>pos</tt> from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBufferArray != null ? zzBufferArray[zzStartRead+pos]:zzBuffer.charAt(zzStartRead+pos);
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public IElementType advance() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;
    char[] zzBufferArrayL = zzBufferArray;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

      zzState = ZZ_LEXSTATE[zzLexicalState];


      zzForAction: {
        while (true) {

          if (zzCurrentPosL < zzEndReadL)
            zzInput = (zzBufferArrayL != null ? zzBufferArrayL[zzCurrentPosL++] : zzBufferL.charAt(zzCurrentPosL++));
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = (zzBufferArrayL != null ? zzBufferArrayL[zzCurrentPosL++] : zzBufferL.charAt(zzCurrentPosL++));
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          int zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
        case 37: 
          { return ALIAS;
          }
        case 63: break;
        case 7: 
          { return STAR;
          }
        case 64: break;
        case 38: 
          { return AFTER;
          }
        case 65: break;
        case 28: 
          { return VAR;
          }
        case 66: break;
        case 12: 
          { return LEFT_BRACKET;
          }
        case 67: break;
        case 62: 
          { return SUBCOMPONENT;
          }
        case 68: break;
        case 36: 
          { return ARRAY;
          }
        case 69: break;
        case 27: 
          { return ARROW;
          }
        case 70: break;
        case 30: 
          { return MULTILINE_COMMENT;
          }
        case 71: break;
        case 57: 
          { return NAMESPACE;
          }
        case 72: break;
        case 43: 
          { return NATIVE;
          }
        case 73: break;
        case 58: 
          { return PARAMETER;
          }
        case 74: break;
        case 45: 
          { return STATUS;
          }
        case 75: break;
        case 41: 
          { return FALSE;
          }
        case 76: break;
        case 10: 
          { return LEFT_PAREN;
          }
        case 77: break;
        case 14: 
          { return COMMA;
          }
        case 78: break;
        case 16: 
          { return DOT;
          }
        case 79: break;
        case 21: 
          { return AT;
          }
        case 80: break;
        case 5: 
          { return COMMENT;
          }
        case 81: break;
        case 59: 
          { return PLACEMENT;
          }
        case 82: break;
        case 56: 
          { return LANGUAGE;
          }
        case 83: break;
        case 24: 
          { return STRING_UNARY_QUOTES;
          }
        case 84: break;
        case 61: 
          { return PARAMETERS;
          }
        case 85: break;
        case 54: 
          { return DISPLAY;
          }
        case 86: break;
        case 35: 
          { return WITH;
          }
        case 87: break;
        case 4: 
          { return NON_NEGATIVE_NUMBER;
          }
        case 88: break;
        case 19: 
          { return COLON;
          }
        case 89: break;
        case 47: 
          { return IMPORT;
          }
        case 90: break;
        case 50: 
          { return EXTENDS;
          }
        case 91: break;
        case 6: 
          { return SLASH;
          }
        case 92: break;
        case 26: 
          { return DOTS;
          }
        case 93: break;
        case 15: 
          { return EQ_SIGN;
          }
        case 94: break;
        case 44: 
          { return SCRIPT;
          }
        case 95: break;
        case 22: 
          { return ESCAPED_NAME;
          }
        case 96: break;
        case 55: 
          { return PRIORITY;
          }
        case 97: break;
        case 8: 
          { return LEFT_BRACE;
          }
        case 98: break;
        case 49: 
          { return MESSAGE;
          }
        case 99: break;
        case 32: 
          { return TYPE_KEYWORD;
          }
        case 100: break;
        case 11: 
          { return RIGHT_PAREN;
          }
        case 101: break;
        case 53: 
          { return DYNAMIC;
          }
        case 102: break;
        case 31: 
          { return ENUM;
          }
        case 103: break;
        case 25: 
          { return AS;
          }
        case 104: break;
        case 40: 
          { return RANGE;
          }
        case 105: break;
        case 42: 
          { return FINAL;
          }
        case 106: break;
        case 34: 
          { return UNIT_TOKEN;
          }
        case 107: break;
        case 18: 
          { return MINUS;
          }
        case 108: break;
        case 13: 
          { return RIGHT_BRACKET;
          }
        case 109: break;
        case 46: 
          { return STRUCT;
          }
        case 110: break;
        case 60: 
          { return COMPONENT;
          }
        case 111: break;
        case 23: 
          { return STRING;
          }
        case 112: break;
        case 2: 
          { return com.intellij.psi.TokenType.WHITE_SPACE;
          }
        case 113: break;
        case 33: 
          { return TRUE;
          }
        case 114: break;
        case 29: 
          { return FOR;
          }
        case 115: break;
        case 39: 
          { return EVENT;
          }
        case 116: break;
        case 52: 
          { return DEFAULT;
          }
        case 117: break;
        case 17: 
          { return PLUS;
          }
        case 118: break;
        case 51: 
          { return COMMAND;
          }
        case 119: break;
        case 20: 
          { return QUESTION;
          }
        case 120: break;
        case 3: 
          { return ELEMENT_NAME_TOKEN;
          }
        case 121: break;
        case 9: 
          { return RIGHT_BRACE;
          }
        case 122: break;
        case 1: 
          { return com.intellij.psi.TokenType.BAD_CHARACTER;
          }
        case 123: break;
        case 48: 
          { return BEFORE;
          }
        case 124: break;
        default:
          if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
            zzAtEOF = true;
            return null;
          }
          else {
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
