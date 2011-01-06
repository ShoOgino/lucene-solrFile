/* The following code was generated by JFlex 1.5.0-SNAPSHOT on 1/6/11 12:09 AM */

package org.apache.lucene.analysis.standard;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * This class implements Word Break rules from the Unicode Text Segmentation 
 * algorithm, as specified in 
 * <a href="http://unicode.org/reports/tr29/">Unicode Standard Annex #29</a> 
 * <p/>
 * Tokens produced are of the following types:
 * <ul>
 *   <li>&lt;ALPHANUM&gt;: A sequence of alphabetic and numeric characters</li>
 *   <li>&lt;NUM&gt;: A number</li>
 *   <li>&lt;SOUTHEAST_ASIAN&gt;: A sequence of characters from South and Southeast
 *       Asian languages, including Thai, Lao, Myanmar, and Khmer</li>
 *   <li>&lt;IDEOGRAPHIC&gt;: A single CJKV ideographic character</li>
 *   <li>&lt;HIRAGANA&gt;: A single hiragana character</li>
 * </ul>
 */

public final class StandardTokenizerImpl implements StandardTokenizerInterface {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

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
    "\47\0\1\140\4\0\1\137\1\0\1\140\1\0\12\134\1\136\1\137"+
    "\5\0\32\132\4\0\1\141\1\0\32\132\57\0\1\132\2\0\1\133"+
    "\7\0\1\132\1\0\1\136\2\0\1\132\5\0\27\132\1\0\37\132"+
    "\1\0\u01ca\132\4\0\14\132\16\0\5\132\7\0\1\132\1\0\1\132"+
    "\21\0\160\133\5\132\1\0\2\132\2\0\4\132\1\137\7\0\1\132"+
    "\1\136\3\132\1\0\1\132\1\0\24\132\1\0\123\132\1\0\213\132"+
    "\1\0\7\133\236\132\11\0\46\132\2\0\1\132\7\0\47\132\1\0"+
    "\1\137\7\0\55\133\1\0\1\133\1\0\2\133\1\0\2\133\1\0"+
    "\1\133\10\0\33\132\5\0\4\132\1\136\13\0\4\133\10\0\2\137"+
    "\2\0\13\133\5\0\53\132\25\133\12\134\1\0\1\134\1\137\1\0"+
    "\2\132\1\133\143\132\1\0\1\132\7\133\1\133\1\0\6\133\2\132"+
    "\2\133\1\0\4\133\2\132\12\134\3\132\2\0\1\132\17\0\1\133"+
    "\1\132\1\133\36\132\33\133\2\0\131\132\13\133\1\132\16\0\12\134"+
    "\41\132\11\133\2\132\2\0\1\137\1\0\1\132\5\0\26\132\4\133"+
    "\1\132\11\133\1\132\3\133\1\132\5\133\22\0\31\132\3\133\244\0"+
    "\4\133\66\132\3\133\1\132\22\133\1\132\7\133\12\132\2\133\2\0"+
    "\12\134\1\0\7\132\1\0\7\132\1\0\3\133\1\0\10\132\2\0"+
    "\2\132\2\0\26\132\1\0\7\132\1\0\1\132\3\0\4\132\2\0"+
    "\1\133\1\132\7\133\2\0\2\133\2\0\3\133\1\132\10\0\1\133"+
    "\4\0\2\132\1\0\3\132\2\133\2\0\12\134\2\132\17\0\3\133"+
    "\1\0\6\132\4\0\2\132\2\0\26\132\1\0\7\132\1\0\2\132"+
    "\1\0\2\132\1\0\2\132\2\0\1\133\1\0\5\133\4\0\2\133"+
    "\2\0\3\133\3\0\1\133\7\0\4\132\1\0\1\132\7\0\12\134"+
    "\2\133\3\132\1\133\13\0\3\133\1\0\11\132\1\0\3\132\1\0"+
    "\26\132\1\0\7\132\1\0\2\132\1\0\5\132\2\0\1\133\1\132"+
    "\10\133\1\0\3\133\1\0\3\133\2\0\1\132\17\0\2\132\2\133"+
    "\2\0\12\134\21\0\3\133\1\0\10\132\2\0\2\132\2\0\26\132"+
    "\1\0\7\132\1\0\2\132\1\0\5\132\2\0\1\133\1\132\7\133"+
    "\2\0\2\133\2\0\3\133\10\0\2\133\4\0\2\132\1\0\3\132"+
    "\2\133\2\0\12\134\1\0\1\132\20\0\1\133\1\132\1\0\6\132"+
    "\3\0\3\132\1\0\4\132\3\0\2\132\1\0\1\132\1\0\2\132"+
    "\3\0\2\132\3\0\3\132\3\0\14\132\4\0\5\133\3\0\3\133"+
    "\1\0\4\133\2\0\1\132\6\0\1\133\16\0\12\134\21\0\3\133"+
    "\1\0\10\132\1\0\3\132\1\0\27\132\1\0\12\132\1\0\5\132"+
    "\3\0\1\132\7\133\1\0\3\133\1\0\4\133\7\0\2\133\1\0"+
    "\2\132\6\0\2\132\2\133\2\0\12\134\22\0\2\133\1\0\10\132"+
    "\1\0\3\132\1\0\27\132\1\0\12\132\1\0\5\132\2\0\1\133"+
    "\1\132\7\133\1\0\3\133\1\0\4\133\7\0\2\133\7\0\1\132"+
    "\1\0\2\132\2\133\2\0\12\134\1\0\2\132\17\0\2\133\1\0"+
    "\10\132\1\0\3\132\1\0\51\132\2\0\1\132\7\133\1\0\3\133"+
    "\1\0\4\133\1\132\10\0\1\133\10\0\2\132\2\133\2\0\12\134"+
    "\12\0\6\132\2\0\2\133\1\0\22\132\3\0\30\132\1\0\11\132"+
    "\1\0\1\132\2\0\7\132\3\0\1\133\4\0\6\133\1\0\1\133"+
    "\1\0\10\133\22\0\2\133\15\0\60\142\1\143\2\142\7\143\5\0"+
    "\7\142\10\143\1\0\12\134\47\0\2\142\1\0\1\142\2\0\2\142"+
    "\1\0\1\142\2\0\1\142\6\0\4\142\1\0\7\142\1\0\3\142"+
    "\1\0\1\142\1\0\1\142\2\0\2\142\1\0\4\142\1\143\2\142"+
    "\6\143\1\0\2\143\1\142\2\0\5\142\1\0\1\142\1\0\6\143"+
    "\2\0\12\134\2\0\2\142\42\0\1\132\27\0\2\133\6\0\12\134"+
    "\13\0\1\133\1\0\1\133\1\0\1\133\4\0\2\133\10\132\1\0"+
    "\44\132\4\0\24\133\1\0\2\133\5\132\13\133\1\0\44\133\11\0"+
    "\1\133\71\0\53\142\24\143\1\142\12\134\6\0\6\142\4\143\4\142"+
    "\3\143\1\142\3\143\2\142\7\143\3\142\4\143\15\142\14\143\1\142"+
    "\1\143\12\134\4\143\2\142\46\132\12\0\53\132\1\0\1\132\3\0"+
    "\u0149\132\1\0\4\132\2\0\7\132\1\0\1\132\1\0\4\132\2\0"+
    "\51\132\1\0\4\132\2\0\41\132\1\0\4\132\2\0\7\132\1\0"+
    "\1\132\1\0\4\132\2\0\17\132\1\0\71\132\1\0\4\132\2\0"+
    "\103\132\2\0\3\133\40\0\20\132\20\0\125\132\14\0\u026c\132\2\0"+
    "\21\132\1\0\32\132\5\0\113\132\3\0\3\132\17\0\15\132\1\0"+
    "\4\132\3\133\13\0\22\132\3\133\13\0\22\132\2\133\14\0\15\132"+
    "\1\0\3\132\1\0\2\133\14\0\64\142\2\143\36\143\3\0\1\142"+
    "\4\0\1\142\1\143\2\0\12\134\41\0\3\133\2\0\12\134\6\0"+
    "\130\132\10\0\51\132\1\133\1\132\5\0\106\132\12\0\35\132\3\0"+
    "\14\133\4\0\14\133\12\0\12\134\36\142\2\0\5\142\13\0\54\142"+
    "\4\0\21\143\7\142\2\143\6\0\12\134\1\142\3\0\2\142\40\0"+
    "\27\132\5\133\4\0\65\142\12\143\1\0\35\143\2\0\1\133\12\134"+
    "\6\0\12\134\6\0\16\142\122\0\5\133\57\132\21\133\7\132\4\0"+
    "\12\134\21\0\11\133\14\0\3\133\36\132\12\133\3\0\2\132\12\134"+
    "\6\0\46\132\16\133\14\0\44\132\24\133\10\0\12\134\3\0\3\132"+
    "\12\134\44\132\122\0\3\133\1\0\25\133\4\132\1\133\4\132\1\133"+
    "\15\0\300\132\47\133\25\0\4\133\u0116\132\2\0\6\132\2\0\46\132"+
    "\2\0\6\132\2\0\10\132\1\0\1\132\1\0\1\132\1\0\1\132"+
    "\1\0\37\132\2\0\65\132\1\0\7\132\1\0\1\132\3\0\3\132"+
    "\1\0\7\132\3\0\4\132\2\0\6\132\4\0\15\132\5\0\3\132"+
    "\1\0\7\132\17\0\2\133\2\133\10\0\2\140\12\0\1\140\2\0"+
    "\1\136\2\0\5\133\20\0\2\141\3\0\1\137\17\0\1\141\13\0"+
    "\5\133\5\0\6\133\1\0\1\132\15\0\1\132\20\0\15\132\63\0"+
    "\41\133\21\0\1\132\4\0\1\132\2\0\12\132\1\0\1\132\3\0"+
    "\5\132\6\0\1\132\1\0\1\132\1\0\1\132\1\0\4\132\1\0"+
    "\13\132\2\0\4\132\5\0\5\132\4\0\1\132\21\0\51\132\u032d\0"+
    "\64\132\u0716\0\57\132\1\0\57\132\1\0\205\132\6\0\4\132\3\133"+
    "\16\0\46\132\12\0\66\132\11\0\1\132\17\0\1\133\27\132\11\0"+
    "\7\132\1\0\7\132\1\0\7\132\1\0\7\132\1\0\7\132\1\0"+
    "\7\132\1\0\7\132\1\0\7\132\1\0\40\133\57\0\1\132\120\0"+
    "\32\144\1\0\131\144\14\0\326\144\57\0\1\132\1\0\1\144\31\0"+
    "\11\144\6\133\1\0\5\135\2\0\3\144\1\132\1\132\4\0\126\145"+
    "\2\0\2\133\2\135\3\145\133\135\1\0\4\135\5\0\51\132\3\0"+
    "\136\132\21\0\33\132\65\0\20\135\320\0\57\135\1\0\130\135\250\0"+
    "\u19b6\144\112\0\u51cc\144\64\0\u048d\132\103\0\56\132\2\0\u010d\132\3\0"+
    "\20\132\12\134\2\132\24\0\57\132\4\133\11\0\2\133\1\0\31\132"+
    "\10\0\120\132\2\133\45\0\11\132\2\0\147\132\2\0\4\132\1\0"+
    "\2\132\16\0\12\132\120\0\10\132\1\133\3\132\1\133\4\132\1\133"+
    "\27\132\5\133\30\0\64\132\14\0\2\133\62\132\21\133\13\0\12\134"+
    "\6\0\22\133\6\132\3\0\1\132\4\0\12\134\34\132\10\133\2\0"+
    "\27\132\15\133\14\0\35\132\3\0\4\133\57\132\16\133\16\0\1\132"+
    "\12\134\46\0\51\132\16\133\11\0\3\132\1\133\10\132\2\133\2\0"+
    "\12\134\6\0\33\142\1\143\4\0\60\142\1\143\1\142\3\143\2\142"+
    "\2\143\5\142\2\143\1\142\1\143\1\142\30\0\5\142\41\0\6\132"+
    "\2\0\6\132\2\0\6\132\11\0\7\132\1\0\7\132\221\0\43\132"+
    "\10\133\1\0\2\133\2\0\12\134\6\0\u2ba4\132\14\0\27\132\4\0"+
    "\61\132\4\0\1\31\1\25\1\46\1\43\1\13\3\0\1\7\1\5"+
    "\2\0\1\3\1\1\14\0\1\11\21\0\1\112\7\0\1\65\1\17"+
    "\6\0\1\130\3\0\1\120\1\120\1\120\1\120\1\120\1\120\1\120"+
    "\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120"+
    "\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120"+
    "\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120"+
    "\1\120\1\120\1\120\1\120\1\121\1\120\1\120\1\120\1\125\1\123"+
    "\17\0\1\114\u02c1\0\1\70\277\0\1\113\1\71\1\2\3\124\2\35"+
    "\1\124\1\35\2\124\1\14\21\124\2\60\7\73\1\72\7\73\7\52"+
    "\1\15\1\52\1\75\2\45\1\44\1\75\1\45\1\44\10\75\2\63"+
    "\5\61\2\54\5\61\1\6\10\37\5\21\3\27\12\106\20\27\3\42"+
    "\32\30\1\26\2\24\2\110\1\111\2\110\2\111\2\110\1\111\3\24"+
    "\1\16\2\24\12\64\1\74\1\41\1\34\1\64\6\41\1\34\66\41"+
    "\5\115\6\103\1\51\4\103\2\51\10\103\1\51\7\100\1\12\2\100"+
    "\32\103\1\12\4\100\1\12\5\102\1\101\1\102\3\101\7\102\1\101"+
    "\23\102\5\67\3\102\6\67\2\67\6\66\10\66\2\100\7\66\36\100"+
    "\4\66\102\100\15\115\1\77\2\115\1\131\3\117\1\115\2\117\5\115"+
    "\4\117\4\116\1\115\3\116\1\115\5\116\26\56\4\23\1\105\2\104"+
    "\4\122\1\104\2\122\3\76\33\122\35\55\3\122\35\126\3\122\6\126"+
    "\2\33\31\126\1\33\17\126\6\122\4\22\1\10\37\22\1\10\4\22"+
    "\25\62\1\127\11\62\21\55\5\62\1\57\12\40\13\62\4\55\1\50"+
    "\6\55\12\122\17\55\1\47\3\53\15\20\11\36\1\32\24\36\2\20"+
    "\11\36\1\32\31\36\1\32\4\20\4\36\2\32\2\107\1\4\5\107"+
    "\52\4\u1900\0\u012e\144\2\0\76\144\2\0\152\144\46\0\7\132\14\0"+
    "\5\132\5\0\1\132\1\133\12\132\1\0\15\132\1\0\5\132\1\0"+
    "\1\132\1\0\2\132\1\0\2\132\1\0\154\132\41\0\u016b\132\22\0"+
    "\100\132\2\0\66\132\50\0\14\132\4\0\20\133\1\137\2\0\1\136"+
    "\1\137\13\0\7\133\14\0\2\141\30\0\3\141\1\137\1\0\1\140"+
    "\1\0\1\137\1\136\32\0\5\132\1\0\207\132\2\0\1\133\7\0"+
    "\1\140\4\0\1\137\1\0\1\140\1\0\12\134\1\136\1\137\5\0"+
    "\32\132\4\0\1\141\1\0\32\132\13\0\70\135\2\133\37\132\3\0"+
    "\6\132\2\0\6\132\2\0\6\132\2\0\3\132\34\0\3\133\4\0";

  /** 
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\1\0\23\1\1\2\1\3\1\2\1\1\1\4\1\5"+
    "\1\6\15\0\1\2\1\0\1\2\10\0\1\3\61\0";

  private static int [] zzUnpackAction() {
    int [] result = new int[101];
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
    "\0\0\0\146\0\314\0\u0132\0\u0198\0\u01fe\0\u0264\0\u02ca"+
    "\0\u0330\0\u0396\0\u03fc\0\u0462\0\u04c8\0\u052e\0\u0594\0\u05fa"+
    "\0\u0660\0\u06c6\0\u072c\0\u0792\0\u07f8\0\u085e\0\u08c4\0\u092a"+
    "\0\u0990\0\146\0\146\0\314\0\u0132\0\u0198\0\u01fe\0\u0264"+
    "\0\u09f6\0\u0a5c\0\u0ac2\0\u0b28\0\u0462\0\u0b8e\0\u0bf4\0\u0c5a"+
    "\0\u0cc0\0\u0d26\0\u0d8c\0\u0df2\0\u0330\0\u0396\0\u0e58\0\u0ebe"+
    "\0\u0f24\0\u0f8a\0\u0ff0\0\u1056\0\u10bc\0\u1122\0\u1188\0\u11ee"+
    "\0\u1254\0\u12ba\0\u1320\0\u1386\0\u13ec\0\u1452\0\u14b8\0\u092a"+
    "\0\u151e\0\u1584\0\u15ea\0\u1650\0\u16b6\0\u171c\0\u1782\0\u17e8"+
    "\0\u184e\0\u18b4\0\u191a\0\u1980\0\u19e6\0\u1a4c\0\u1ab2\0\u1b18"+
    "\0\u1b7e\0\u1be4\0\u1c4a\0\u1cb0\0\u1d16\0\u1d7c\0\u1de2\0\u1e48"+
    "\0\u1eae\0\u1f14\0\u1f7a\0\u1fe0\0\u2046\0\u20ac\0\u2112\0\u2178"+
    "\0\u21de\0\u2244\0\u22aa\0\u2310\0\u2376";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[101];
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
    "\1\2\1\3\1\2\1\4\1\2\1\5\1\2\1\6"+
    "\1\2\1\7\1\2\1\10\3\2\1\11\5\2\1\12"+
    "\3\2\1\13\11\2\1\14\2\2\1\15\43\2\1\16"+
    "\1\2\1\17\3\2\1\20\1\21\1\2\1\22\1\2"+
    "\1\23\2\2\1\24\1\2\1\25\1\2\1\26\1\27"+
    "\3\2\1\30\2\31\1\32\1\33\150\0\1\25\11\0"+
    "\1\25\20\0\1\25\22\0\1\25\10\0\3\25\17\0"+
    "\1\25\10\0\1\25\23\0\1\25\1\0\1\25\1\0"+
    "\1\25\1\0\1\25\1\0\1\25\1\0\3\25\1\0"+
    "\5\25\1\0\3\25\1\0\11\25\1\0\2\25\1\0"+
    "\16\25\1\0\2\25\1\0\21\25\1\0\1\25\1\0"+
    "\3\25\2\0\1\25\1\0\1\25\1\0\2\25\1\0"+
    "\1\25\16\0\1\25\3\0\1\25\5\0\2\25\3\0"+
    "\1\25\13\0\1\25\1\0\1\25\4\0\2\25\4\0"+
    "\1\25\1\0\1\25\3\0\2\25\1\0\1\25\5\0"+
    "\3\25\1\0\1\25\15\0\1\25\10\0\1\25\23\0"+
    "\1\25\3\0\1\25\1\0\1\25\1\0\1\25\1\0"+
    "\3\25\2\0\4\25\1\0\3\25\2\0\3\25\1\0"+
    "\4\25\1\0\2\25\2\0\3\25\1\0\11\25\1\0"+
    "\2\25\1\0\16\25\1\0\2\25\1\0\1\25\1\0"+
    "\3\25\2\0\1\25\1\0\1\25\1\0\2\25\1\0"+
    "\1\25\16\0\1\25\3\0\1\25\3\0\1\25\1\0"+
    "\3\25\2\0\1\25\1\0\2\25\1\0\3\25\3\0"+
    "\2\25\1\0\1\25\1\0\2\25\1\0\2\25\3\0"+
    "\2\25\1\0\1\25\1\0\1\25\1\0\2\25\1\0"+
    "\2\25\1\0\2\25\1\0\5\25\1\0\5\25\1\0"+
    "\2\25\1\0\2\25\1\0\1\25\1\0\3\25\4\0"+
    "\1\25\4\0\1\25\30\0\3\25\5\0\1\25\1\0"+
    "\1\25\1\0\1\25\4\0\1\25\14\0\1\25\5\0"+
    "\1\25\11\0\2\25\12\0\1\26\1\0\2\25\12\0"+
    "\1\25\23\0\1\25\1\0\1\26\7\0\2\25\2\0"+
    "\5\25\2\0\2\25\4\0\6\25\1\0\2\25\4\0"+
    "\5\25\1\0\5\25\1\0\2\25\1\0\3\25\1\0"+
    "\4\25\1\0\5\25\1\26\1\0\1\25\1\0\1\25"+
    "\1\0\3\25\2\0\1\25\1\0\1\25\1\0\1\25"+
    "\2\0\1\25\16\0\1\25\3\0\1\25\5\0\2\25"+
    "\3\0\1\25\4\0\3\25\4\0\1\25\1\0\1\25"+
    "\2\0\1\25\1\0\2\25\4\0\1\25\1\0\1\25"+
    "\3\0\2\25\1\0\1\25\5\0\3\25\1\0\1\25"+
    "\10\0\1\25\1\0\2\26\1\0\1\25\10\0\1\25"+
    "\23\0\1\25\3\0\1\25\6\0\2\25\5\0\1\25"+
    "\1\0\1\25\1\0\1\25\1\0\11\25\2\0\1\25"+
    "\4\0\1\25\4\0\6\25\2\0\1\25\1\0\1\25"+
    "\1\0\3\25\3\0\2\25\4\0\3\25\1\0\1\25"+
    "\10\0\1\25\1\0\2\25\20\0\1\25\11\0\2\25"+
    "\17\0\1\25\6\0\2\25\4\0\1\25\5\0\1\25"+
    "\2\0\1\25\5\0\3\25\1\0\1\25\15\0\1\25"+
    "\10\0\1\25\23\0\1\25\3\0\1\25\5\0\1\25"+
    "\32\0\15\25\5\0\3\25\1\0\1\25\5\0\1\25"+
    "\7\0\1\25\2\0\1\25\5\0\1\25\2\0\1\25"+
    "\1\0\1\25\105\0\1\33\21\0\1\27\34\0\1\32"+
    "\3\0\1\32\3\0\1\32\1\0\3\32\2\0\1\32"+
    "\2\0\1\32\1\0\3\32\3\0\2\32\1\0\1\32"+
    "\1\0\2\32\1\0\2\32\3\0\2\32\1\0\1\32"+
    "\3\0\2\32\1\0\2\32\1\0\2\32\1\0\5\32"+
    "\1\0\5\32\2\0\1\32\1\0\2\32\1\0\1\32"+
    "\1\0\3\32\4\0\1\32\4\0\1\32\16\0\1\32"+
    "\1\0\1\32\1\0\1\32\1\0\1\32\1\0\1\32"+
    "\1\0\3\32\1\0\5\32\1\0\3\32\1\0\11\32"+
    "\1\0\2\32\1\0\16\32\1\0\2\32\1\0\21\32"+
    "\1\0\1\32\1\0\3\32\2\0\1\32\1\0\1\32"+
    "\1\0\2\32\1\0\1\32\16\0\1\32\1\0\1\32"+
    "\1\0\1\32\3\0\1\32\1\0\3\32\1\0\2\32"+
    "\1\0\2\32\1\0\3\32\1\0\11\32\1\0\2\32"+
    "\1\0\16\32\1\0\2\32\1\0\21\32\1\0\1\32"+
    "\1\0\3\32\2\0\1\32\1\0\1\32\1\0\2\32"+
    "\1\0\1\32\16\0\1\32\11\0\1\32\20\0\1\32"+
    "\33\0\1\32\21\0\1\32\10\0\1\32\23\0\1\32"+
    "\1\0\1\32\1\0\1\32\1\0\1\32\1\0\1\32"+
    "\1\0\3\32\1\0\5\32\1\0\3\32\1\0\6\32"+
    "\1\0\2\32\1\0\2\32\1\0\10\32\1\0\5\32"+
    "\1\0\2\32\1\0\21\32\1\0\1\32\1\0\3\32"+
    "\2\0\1\32\1\0\1\32\1\0\2\32\1\0\1\32"+
    "\145\0\1\33\15\0\1\34\1\0\1\35\1\0\1\36"+
    "\1\0\1\37\1\0\1\40\1\0\1\41\3\0\1\42"+
    "\5\0\1\43\3\0\1\44\11\0\1\45\2\0\1\46"+
    "\16\0\1\47\2\0\1\50\41\0\2\25\1\51\1\0"+
    "\1\52\1\0\1\52\1\53\1\0\1\25\3\0\1\34"+
    "\1\0\1\35\1\0\1\36\1\0\1\37\1\0\1\40"+
    "\1\0\1\54\3\0\1\55\5\0\1\56\3\0\1\57"+
    "\11\0\1\45\2\0\1\60\16\0\1\61\2\0\1\62"+
    "\41\0\1\25\2\26\2\0\2\63\1\64\1\0\1\26"+
    "\15\0\1\65\15\0\1\66\14\0\1\67\16\0\1\70"+
    "\2\0\1\71\21\0\1\72\20\0\1\27\1\0\1\27"+
    "\3\0\1\53\1\0\1\27\3\0\1\34\1\0\1\35"+
    "\1\0\1\36\1\0\1\37\1\0\1\40\1\0\1\73"+
    "\3\0\1\55\5\0\1\56\3\0\1\74\11\0\1\45"+
    "\2\0\1\75\16\0\1\76\2\0\1\77\21\0\1\72"+
    "\17\0\1\25\1\100\1\26\1\27\3\0\1\100\1\0"+
    "\1\100\144\0\2\31\4\0\1\25\11\0\3\25\5\0"+
    "\1\25\1\0\1\25\1\0\1\25\4\0\1\25\4\0"+
    "\1\25\1\0\2\25\4\0\1\25\5\0\1\25\3\0"+
    "\1\25\4\0\5\25\10\0\1\51\1\0\2\25\1\0"+
    "\1\25\10\0\1\25\23\0\1\25\1\0\1\51\7\0"+
    "\2\25\2\0\5\25\2\0\2\25\4\0\6\25\1\0"+
    "\2\25\4\0\5\25\1\0\5\25\1\0\2\25\1\0"+
    "\3\25\1\0\4\25\1\0\5\25\1\51\1\0\1\25"+
    "\1\0\1\25\1\0\3\25\2\0\1\25\1\0\1\25"+
    "\1\0\1\25\2\0\1\25\16\0\1\25\3\0\1\25"+
    "\5\0\2\25\3\0\1\25\4\0\3\25\4\0\1\25"+
    "\1\0\1\25\2\0\1\25\1\0\2\25\4\0\1\25"+
    "\1\0\1\25\3\0\2\25\1\0\1\25\5\0\3\25"+
    "\1\0\1\25\10\0\1\25\1\0\2\51\1\0\1\25"+
    "\10\0\1\25\23\0\1\25\3\0\1\25\6\0\2\25"+
    "\5\0\1\25\1\0\1\25\1\0\1\25\1\0\11\25"+
    "\2\0\1\25\4\0\1\25\4\0\6\25\2\0\1\25"+
    "\1\0\1\25\1\0\3\25\1\0\1\25\1\0\2\25"+
    "\4\0\3\25\1\0\1\25\10\0\1\25\1\0\2\25"+
    "\20\0\1\25\3\0\1\25\5\0\1\25\32\0\15\25"+
    "\5\0\3\25\1\0\1\25\5\0\3\25\5\0\1\25"+
    "\2\0\2\25\4\0\1\25\2\0\1\25\1\0\1\25"+
    "\102\0\2\25\6\0\1\25\55\0\1\25\3\0\1\25"+
    "\2\0\1\25\3\0\1\25\5\0\1\25\7\0\1\25"+
    "\4\0\2\25\3\0\2\25\1\0\1\25\4\0\1\25"+
    "\1\0\1\25\2\0\2\25\1\0\3\25\1\0\1\25"+
    "\2\0\4\25\2\0\1\25\40\0\1\34\1\0\1\35"+
    "\1\0\1\36\1\0\1\37\1\0\1\40\1\0\1\101"+
    "\3\0\1\42\5\0\1\43\3\0\1\102\11\0\1\45"+
    "\2\0\1\103\16\0\1\104\2\0\1\105\41\0\1\25"+
    "\2\51\2\0\2\106\1\53\1\0\1\51\3\0\1\34"+
    "\1\0\1\35\1\0\1\36\1\0\1\37\1\0\1\40"+
    "\1\0\1\107\3\0\1\110\5\0\1\111\3\0\1\112"+
    "\11\0\1\45\2\0\1\113\16\0\1\114\2\0\1\115"+
    "\41\0\1\25\1\52\7\0\1\52\3\0\1\34\1\0"+
    "\1\35\1\0\1\36\1\0\1\37\1\0\1\40\1\0"+
    "\1\116\3\0\1\42\5\0\1\43\3\0\1\117\11\0"+
    "\1\45\2\0\1\120\16\0\1\121\2\0\1\122\21\0"+
    "\1\72\17\0\1\25\1\53\1\51\1\27\3\0\1\53"+
    "\1\0\1\53\4\0\1\26\11\0\3\25\5\0\1\25"+
    "\1\0\1\25\1\0\1\25\4\0\1\25\4\0\1\26"+
    "\1\0\2\26\4\0\1\25\5\0\1\25\3\0\1\26"+
    "\4\0\1\26\2\25\2\26\10\0\1\26\1\0\2\25"+
    "\1\0\1\26\10\0\1\25\23\0\1\25\3\0\1\25"+
    "\6\0\2\25\5\0\1\25\1\0\1\25\1\0\1\25"+
    "\1\0\11\25\2\0\1\25\4\0\1\25\4\0\6\25"+
    "\2\0\1\25\1\0\1\25\1\0\3\25\1\0\1\26"+
    "\1\0\2\25\4\0\3\25\1\0\1\25\10\0\1\25"+
    "\1\0\2\25\20\0\1\25\3\0\1\25\5\0\1\25"+
    "\32\0\15\25\5\0\3\25\1\0\1\25\5\0\1\25"+
    "\2\26\5\0\1\25\2\0\1\25\1\26\4\0\1\25"+
    "\2\0\1\25\1\0\1\25\102\0\2\26\6\0\1\26"+
    "\55\0\1\26\3\0\1\26\2\0\1\26\3\0\1\26"+
    "\5\0\1\26\7\0\1\26\4\0\2\26\3\0\2\26"+
    "\1\0\1\26\4\0\1\26\1\0\1\26\2\0\2\26"+
    "\1\0\3\26\1\0\1\26\2\0\4\26\2\0\1\26"+
    "\52\0\1\123\3\0\1\124\5\0\1\125\3\0\1\126"+
    "\14\0\1\127\16\0\1\130\2\0\1\131\42\0\1\63"+
    "\1\26\6\0\1\63\3\0\1\34\1\0\1\35\1\0"+
    "\1\36\1\0\1\37\1\0\1\40\1\0\1\132\3\0"+
    "\1\55\5\0\1\56\3\0\1\133\11\0\1\45\2\0"+
    "\1\134\16\0\1\135\2\0\1\136\21\0\1\72\17\0"+
    "\1\25\1\64\1\26\1\27\3\0\1\64\1\0\1\64"+
    "\4\0\1\27\37\0\1\27\1\0\2\27\16\0\1\27"+
    "\4\0\1\27\2\0\2\27\15\0\1\27\131\0\1\27"+
    "\152\0\2\27\11\0\1\27\114\0\2\27\6\0\1\27"+
    "\55\0\1\27\3\0\1\27\2\0\1\27\3\0\1\27"+
    "\5\0\1\27\7\0\1\27\4\0\2\27\3\0\2\27"+
    "\1\0\1\27\4\0\1\27\1\0\1\27\2\0\2\27"+
    "\1\0\3\27\1\0\1\27\2\0\4\27\2\0\1\27"+
    "\152\0\1\27\34\0\1\100\11\0\3\25\5\0\1\25"+
    "\1\0\1\25\1\0\1\25\4\0\1\25\4\0\1\100"+
    "\1\0\2\100\4\0\1\25\5\0\1\25\3\0\1\100"+
    "\4\0\1\100\2\25\2\100\10\0\1\26\1\0\2\25"+
    "\1\0\1\100\10\0\1\25\23\0\1\25\3\0\1\25"+
    "\6\0\2\25\5\0\1\25\1\0\1\25\1\0\1\25"+
    "\1\0\11\25\2\0\1\25\4\0\1\25\4\0\6\25"+
    "\2\0\1\25\1\0\1\25\1\0\3\25\1\0\1\100"+
    "\1\0\2\25\4\0\3\25\1\0\1\25\10\0\1\25"+
    "\1\0\2\25\20\0\1\25\3\0\1\25\5\0\1\25"+
    "\32\0\15\25\5\0\3\25\1\0\1\25\5\0\1\25"+
    "\2\100\5\0\1\25\2\0\1\25\1\100\4\0\1\25"+
    "\2\0\1\25\1\0\1\25\102\0\2\100\6\0\1\100"+
    "\55\0\1\100\3\0\1\100\2\0\1\100\3\0\1\100"+
    "\5\0\1\100\7\0\1\100\4\0\2\100\3\0\2\100"+
    "\1\0\1\100\4\0\1\100\1\0\1\100\2\0\2\100"+
    "\1\0\3\100\1\0\1\100\2\0\4\100\2\0\1\100"+
    "\41\0\1\51\11\0\3\25\5\0\1\25\1\0\1\25"+
    "\1\0\1\25\4\0\1\25\4\0\1\51\1\0\2\51"+
    "\4\0\1\25\5\0\1\25\3\0\1\51\4\0\1\51"+
    "\2\25\2\51\10\0\1\51\1\0\2\25\1\0\1\51"+
    "\10\0\1\25\23\0\1\25\3\0\1\25\6\0\2\25"+
    "\5\0\1\25\1\0\1\25\1\0\1\25\1\0\11\25"+
    "\2\0\1\25\4\0\1\25\4\0\6\25\2\0\1\25"+
    "\1\0\1\25\1\0\3\25\1\0\1\51\1\0\2\25"+
    "\4\0\3\25\1\0\1\25\10\0\1\25\1\0\2\25"+
    "\20\0\1\25\3\0\1\25\5\0\1\25\32\0\15\25"+
    "\5\0\3\25\1\0\1\25\5\0\1\25\2\51\5\0"+
    "\1\25\2\0\1\25\1\51\4\0\1\25\2\0\1\25"+
    "\1\0\1\25\102\0\2\51\6\0\1\51\55\0\1\51"+
    "\3\0\1\51\2\0\1\51\3\0\1\51\5\0\1\51"+
    "\7\0\1\51\4\0\2\51\3\0\2\51\1\0\1\51"+
    "\4\0\1\51\1\0\1\51\2\0\2\51\1\0\3\51"+
    "\1\0\1\51\2\0\4\51\2\0\1\51\52\0\1\137"+
    "\3\0\1\140\5\0\1\141\3\0\1\142\14\0\1\143"+
    "\16\0\1\144\2\0\1\145\42\0\1\106\1\51\6\0"+
    "\1\106\4\0\1\52\11\0\3\25\5\0\1\25\1\0"+
    "\1\25\1\0\1\25\4\0\1\25\4\0\1\52\1\0"+
    "\2\52\4\0\1\25\5\0\1\25\3\0\1\52\4\0"+
    "\1\52\2\25\2\52\12\0\2\25\1\0\1\52\10\0"+
    "\1\25\23\0\1\25\11\0\2\25\2\0\5\25\2\0"+
    "\2\25\4\0\6\25\1\0\2\25\4\0\5\25\1\0"+
    "\5\25\1\0\2\25\1\0\3\25\1\0\4\25\1\0"+
    "\5\25\2\0\1\25\1\0\1\25\1\0\3\25\2\0"+
    "\1\25\1\0\1\25\1\0\1\25\2\0\1\25\16\0"+
    "\1\25\3\0\1\25\5\0\2\25\3\0\1\25\4\0"+
    "\3\25\4\0\1\25\1\0\1\25\2\0\1\25\1\0"+
    "\2\25\4\0\1\25\1\0\1\25\3\0\2\25\1\0"+
    "\1\25\5\0\3\25\1\0\1\25\10\0\1\25\4\0"+
    "\1\25\10\0\1\25\23\0\1\25\3\0\1\25\6\0"+
    "\2\25\5\0\1\25\1\0\1\25\1\0\1\25\1\0"+
    "\11\25\2\0\1\25\4\0\1\25\4\0\6\25\2\0"+
    "\1\25\1\0\1\25\1\0\3\25\1\0\1\52\1\0"+
    "\2\25\4\0\3\25\1\0\1\25\10\0\1\25\1\0"+
    "\2\25\20\0\1\25\3\0\1\25\5\0\1\25\32\0"+
    "\15\25\5\0\3\25\1\0\1\25\5\0\1\25\2\52"+
    "\5\0\1\25\2\0\1\25\1\52\4\0\1\25\2\0"+
    "\1\25\1\0\1\25\102\0\2\52\6\0\1\52\55\0"+
    "\1\52\3\0\1\52\2\0\1\52\3\0\1\52\5\0"+
    "\1\52\7\0\1\52\4\0\2\52\3\0\2\52\1\0"+
    "\1\52\4\0\1\52\1\0\1\52\2\0\2\52\1\0"+
    "\3\52\1\0\1\52\2\0\4\52\2\0\1\52\41\0"+
    "\1\53\11\0\3\25\5\0\1\25\1\0\1\25\1\0"+
    "\1\25\4\0\1\25\4\0\1\53\1\0\2\53\4\0"+
    "\1\25\5\0\1\25\3\0\1\53\4\0\1\53\2\25"+
    "\2\53\10\0\1\51\1\0\2\25\1\0\1\53\10\0"+
    "\1\25\23\0\1\25\3\0\1\25\6\0\2\25\5\0"+
    "\1\25\1\0\1\25\1\0\1\25\1\0\11\25\2\0"+
    "\1\25\4\0\1\25\4\0\6\25\2\0\1\25\1\0"+
    "\1\25\1\0\3\25\1\0\1\53\1\0\2\25\4\0"+
    "\3\25\1\0\1\25\10\0\1\25\1\0\2\25\20\0"+
    "\1\25\3\0\1\25\5\0\1\25\32\0\15\25\5\0"+
    "\3\25\1\0\1\25\5\0\1\25\2\53\5\0\1\25"+
    "\2\0\1\25\1\53\4\0\1\25\2\0\1\25\1\0"+
    "\1\25\102\0\2\53\6\0\1\53\55\0\1\53\3\0"+
    "\1\53\2\0\1\53\3\0\1\53\5\0\1\53\7\0"+
    "\1\53\4\0\2\53\3\0\2\53\1\0\1\53\4\0"+
    "\1\53\1\0\1\53\2\0\2\53\1\0\3\53\1\0"+
    "\1\53\2\0\4\53\2\0\1\53\41\0\1\63\37\0"+
    "\1\63\1\0\2\63\16\0\1\63\4\0\1\63\2\0"+
    "\2\63\10\0\1\26\4\0\1\63\36\0\1\26\102\0"+
    "\1\26\146\0\2\26\133\0\1\63\152\0\2\63\11\0"+
    "\1\63\114\0\2\63\6\0\1\63\55\0\1\63\3\0"+
    "\1\63\2\0\1\63\3\0\1\63\5\0\1\63\7\0"+
    "\1\63\4\0\2\63\3\0\2\63\1\0\1\63\4\0"+
    "\1\63\1\0\1\63\2\0\2\63\1\0\3\63\1\0"+
    "\1\63\2\0\4\63\2\0\1\63\41\0\1\64\11\0"+
    "\3\25\5\0\1\25\1\0\1\25\1\0\1\25\4\0"+
    "\1\25\4\0\1\64\1\0\2\64\4\0\1\25\5\0"+
    "\1\25\3\0\1\64\4\0\1\64\2\25\2\64\10\0"+
    "\1\26\1\0\2\25\1\0\1\64\10\0\1\25\23\0"+
    "\1\25\3\0\1\25\6\0\2\25\5\0\1\25\1\0"+
    "\1\25\1\0\1\25\1\0\11\25\2\0\1\25\4\0"+
    "\1\25\4\0\6\25\2\0\1\25\1\0\1\25\1\0"+
    "\3\25\1\0\1\64\1\0\2\25\4\0\3\25\1\0"+
    "\1\25\10\0\1\25\1\0\2\25\20\0\1\25\3\0"+
    "\1\25\5\0\1\25\32\0\15\25\5\0\3\25\1\0"+
    "\1\25\5\0\1\25\2\64\5\0\1\25\2\0\1\25"+
    "\1\64\4\0\1\25\2\0\1\25\1\0\1\25\102\0"+
    "\2\64\6\0\1\64\55\0\1\64\3\0\1\64\2\0"+
    "\1\64\3\0\1\64\5\0\1\64\7\0\1\64\4\0"+
    "\2\64\3\0\2\64\1\0\1\64\4\0\1\64\1\0"+
    "\1\64\2\0\2\64\1\0\3\64\1\0\1\64\2\0"+
    "\4\64\2\0\1\64\41\0\1\106\37\0\1\106\1\0"+
    "\2\106\16\0\1\106\4\0\1\106\2\0\2\106\10\0"+
    "\1\51\4\0\1\106\36\0\1\51\102\0\1\51\146\0"+
    "\2\51\133\0\1\106\152\0\2\106\11\0\1\106\114\0"+
    "\2\106\6\0\1\106\55\0\1\106\3\0\1\106\2\0"+
    "\1\106\3\0\1\106\5\0\1\106\7\0\1\106\4\0"+
    "\2\106\3\0\2\106\1\0\1\106\4\0\1\106\1\0"+
    "\1\106\2\0\2\106\1\0\3\106\1\0\1\106\2\0"+
    "\4\106\2\0\1\106\37\0";

  private static int [] zzUnpackTrans() {
    int [] result = new int[9180];
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
    "\1\0\1\11\27\1\2\11\15\0\1\1\1\0\1\1"+
    "\10\0\1\1\61\0";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[101];
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

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** number of newlines encountered up to the start of the matched text */
  private int yyline;

  /** the number of characters up to the start of the matched text */
  private int yychar;

  /**
   * the number of characters from the last newline up to the start of the 
   * matched text
   */
  private int yycolumn;

  /** 
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;

  /* user code: */
  /** Alphanumeric sequences */
  public static final int WORD_TYPE = StandardTokenizer.ALPHANUM;
  
  /** Numbers */
  public static final int NUMERIC_TYPE = StandardTokenizer.NUM;
  
  /**
   * Chars in class \p{Line_Break = Complex_Context} are from South East Asian
   * scripts (Thai, Lao, Myanmar, Khmer, etc.).  Sequences of these are kept 
   * together as as a single token rather than broken up, because the logic
   * required to break them at word boundaries is too complex for UAX#29.
   * <p>
   * See Unicode Line Breaking Algorithm: http://www.unicode.org/reports/tr14/#SA
   */
  public static final int SOUTH_EAST_ASIAN_TYPE = StandardTokenizer.SOUTHEAST_ASIAN;
  
  public static final int IDEOGRAPHIC_TYPE = StandardTokenizer.IDEOGRAPHIC;
  
  public static final int HIRAGANA_TYPE = StandardTokenizer.HIRAGANA;

  public final int yychar()
  {
    return yychar;
  }

  /**
   * Fills CharTermAttribute with the current token text.
   */
  public final void getText(CharTermAttribute t) {
    t.copyBuffer(zzBuffer, zzStartRead, zzMarkedPos-zzStartRead);
  }


  /**
   * Creates a new scanner
   * There is also a java.io.InputStream version of this constructor.
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public StandardTokenizerImpl(java.io.Reader in) {
    this.zzReader = in;
  }

  /**
   * Creates a new scanner.
   * There is also java.io.Reader version of this constructor.
   *
   * @param   in  the java.io.Inputstream to read input from.
   */
  public StandardTokenizerImpl(java.io.InputStream in) {
    this(new java.io.InputStreamReader(in));
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
    while (i < 2640) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }


  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   * 
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {

    /* first: make room (if you can) */
    if (zzStartRead > 0) {
      System.arraycopy(zzBuffer, zzStartRead,
                       zzBuffer, 0,
                       zzEndRead-zzStartRead);

      /* translate stored positions */
      zzEndRead-= zzStartRead;
      zzCurrentPos-= zzStartRead;
      zzMarkedPos-= zzStartRead;
      zzStartRead = 0;
    }

    /* is the buffer big enough? */
    if (zzCurrentPos >= zzBuffer.length) {
      /* if not: blow it up */
      char newBuffer[] = new char[zzCurrentPos*2];
      System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
      zzBuffer = newBuffer;
    }

    /* finally: fill the buffer with new input */
    int numRead = zzReader.read(zzBuffer, zzEndRead,
                                            zzBuffer.length-zzEndRead);

    if (numRead > 0) {
      zzEndRead+= numRead;
      return false;
    }
    // unlikely but not impossible: read 0 characters, but not at end of stream    
    if (numRead == 0) {
      int c = zzReader.read();
      if (c == -1) {
        return true;
      } else {
        zzBuffer[zzEndRead++] = (char) c;
        return false;
      }     
    }

	// numRead < 0
    return true;
  }

    
  /**
   * Closes the input stream.
   */
  public final void yyclose() throws java.io.IOException {
    zzAtEOF = true;            /* indicate end of file */
    zzEndRead = zzStartRead;  /* invalidate buffer    */

    if (zzReader != null)
      zzReader.close();
  }


  /**
   * Resets the scanner to read from a new input stream.
   * Does not close the old reader.
   *
   * All internal variables are reset, the old input stream 
   * <b>cannot</b> be reused (internal buffer is discarded and lost).
   * Lexical state is set to <tt>ZZ_INITIAL</tt>.
   *
   * Internal scan buffer is resized down to its initial length, if it has grown.
   *
   * @param reader   the new input stream 
   */
  public final void yyreset(java.io.Reader reader) {
    zzReader = reader;
    zzAtBOL  = true;
    zzAtEOF  = false;
    zzEOFDone = false;
    zzEndRead = zzStartRead = 0;
    zzCurrentPos = zzMarkedPos = 0;
    yyline = yychar = yycolumn = 0;
    zzLexicalState = YYINITIAL;
    if (zzBuffer.length > ZZ_BUFFERSIZE)
      zzBuffer = new char[ZZ_BUFFERSIZE];
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
  public final String yytext() {
    return new String( zzBuffer, zzStartRead, zzMarkedPos-zzStartRead );
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
    return zzBuffer[zzStartRead+pos];
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
  public int getNextToken() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    char [] zzBufferL = zzBuffer;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      yychar+= zzMarkedPosL-zzStartRead;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
  
      zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {
    
          if (zzCurrentPosL < zzEndReadL)
            zzInput = zzBufferL[zzCurrentPosL++];
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
              zzInput = zzBufferL[zzCurrentPosL++];
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
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
        case 2: 
          { return WORD_TYPE;
          }
        case 7: break;
        case 4: 
          { return SOUTH_EAST_ASIAN_TYPE;
          }
        case 8: break;
        case 5: 
          { return IDEOGRAPHIC_TYPE;
          }
        case 9: break;
        case 1: 
          { /* Not numeric, word, ideographic, hiragana, or SE Asian -- ignore it. */
          }
        case 10: break;
        case 3: 
          { return NUMERIC_TYPE;
          }
        case 11: break;
        case 6: 
          { return HIRAGANA_TYPE;
          }
        case 12: break;
        default: 
          if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
            zzAtEOF = true;
              {
                return StandardTokenizerInterface.YYEOF;
              }
          } 
          else {
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
