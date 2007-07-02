/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
/*
 *
 * (C) Copyright IBM Corp. 1998, All Rights Reserved
 */

package com.ibm.richtext.textpanel;

/*
For any incoming character C
if you can map C using the following FakeArabicTable, return
FakeArabicTable(C)
else if C is from A through Z, return FakeArabicTable(lowercase(C))
else just return C


FakeArabicTable is defined by the following mapping

0   0660;ARABIC-INDIC DIGIT ZERO;Nd;0;AN;;0;0;0;N;;;;;
1   0661;ARABIC-INDIC DIGIT ONE;Nd;0;AN;;1;1;1;N;;;;;
2   0662;ARABIC-INDIC DIGIT TWO;Nd;0;AN;;2;2;2;N;;;;;
3   0663;ARABIC-INDIC DIGIT THREE;Nd;0;AN;;3;3;3;N;;;;;
4   0664;ARABIC-INDIC DIGIT FOUR;Nd;0;AN;;4;4;4;N;;;;;
5   0665;ARABIC-INDIC DIGIT FIVE;Nd;0;AN;;5;5;5;N;;;;;
6   0666;ARABIC-INDIC DIGIT SIX;Nd;0;AN;;6;6;6;N;;;;;
7   0667;ARABIC-INDIC DIGIT SEVEN;Nd;0;AN;;7;7;7;N;;;;;
8   0668;ARABIC-INDIC DIGIT EIGHT;Nd;0;AN;;8;8;8;N;;;;;
9   0669;ARABIC-INDIC DIGIT NINE;Nd;0;AN;;9;9;9;N;;;;;

%   066A;ARABIC PERCENT SIGN;Po;0;ET;;;;;N;;;;;
.   066B;ARABIC DECIMAL SEPARATOR;Po;0;AN;;;;;N;;;;;
,   060C;ARABIC COMMA;Po;0;R;;;;;N;;;;;
-   0640;ARABIC TATWEEL;Lm;0;R;;;;;N;;;;;
'   0652;ARABIC SUKUN;Mn;34;R;;;;;N;;;;;
"   0651;ARABIC SHADDA;Mn;33;R;;;;;N;ARABIC SHADDAH;;;;
;   061B;ARABIC SEMICOLON;Po;0;R;;;;;N;;;;;
?   061F;ARABIC QUESTION MARK;Po;0;R;;;;;N;;;;;

a   0627;ARABIC LETTER ALEF;Lo;0;R;;;;;N;;;;;
A   0639;ARABIC LETTER AIN;Lo;0;R;;;;;N;;;;;
b   0628;ARABIC LETTER BEH;Lo;0;R;;;;;N;ARABIC LETTER BAA;;;;
c   0635;ARABIC LETTER SAD;Lo;0;R;;;;;N;;;;;
d   062F;ARABIC LETTER DAL;Lo;0;R;;;;;N;;;;;
D   0630;ARABIC LETTER THAL;Lo;0;R;;;;;N;;;;;
E   064B;ARABIC FATHATAN;Mn;27;R;;;;;N;;;;;
e   064E;ARABIC FATHA;Mn;30;R;;;;;N;ARABIC FATHAH;;;;
f   0641;ARABIC LETTER FEH;Lo;0;R;;;;;N;ARABIC LETTER FA;;;;
g   063A;ARABIC LETTER GHAIN;Lo;0;R;;;;;N;;;;;
h   062D;ARABIC LETTER HAH;Lo;0;R;;;;;N;ARABIC LETTER HAA;;;;
H   0647;ARABIC LETTER HEH;Lo;0;R;;;;;N;ARABIC LETTER HA;;;;
I   064D;ARABIC KASRATAN;Mn;29;R;;;;;N;;;;;
i   0650;ARABIC KASRA;Mn;32;R;;;;;N;ARABIC KASRAH;;;;
j   062C;ARABIC LETTER JEEM;Lo;0;R;;;;;N;;;;;
K   062E;ARABIC LETTER KHAH;Lo;0;R;;;;;N;ARABIC LETTER KHAA;;;;
k   0643;ARABIC LETTER KAF;Lo;0;R;;;;;N;ARABIC LETTER CAF;;;;
l   0644;ARABIC LETTER LAM;Lo;0;R;;;;;N;;;;;
m   0645;ARABIC LETTER MEEM;Lo;0;R;;;;;N;;;;;
n   0646;ARABIC LETTER NOON;Lo;0;R;;;;;N;;;;;
o   064F;ARABIC DAMMA;Mn;31;R;;;;;N;ARABIC DAMMAH;;;;
p   0628;ARABIC LETTER BEH;Lo;0;R;;;;;N;ARABIC LETTER BAA;;;;
q   0642;ARABIC LETTER QAF;Lo;0;R;;;;;N;;;;;
r   0631;ARABIC LETTER REH;Lo;0;R;;;;;N;ARABIC LETTER RA;;;;
s   0633;ARABIC LETTER SEEN;Lo;0;R;;;;;N;;;;;
S   0634;ARABIC LETTER SHEEN;Lo;0;R;;;;;N;;;;;
t   062A;ARABIC LETTER TEH;Lo;0;R;;;;;N;ARABIC LETTER TAA;;;;
T   062B;ARABIC LETTER THEH;Lo;0;R;;;;;N;ARABIC LETTER THAA;;;;
U   064C;ARABIC DAMMATAN;Mn;28;R;;;;;N;;;;;
u   064F;ARABIC DAMMA;Mn;31;R;;;;;N;ARABIC DAMMAH;;;;
v   0641;ARABIC LETTER FEH;Lo;0;R;;;;;N;ARABIC LETTER FA;;;;
w   0648;ARABIC LETTER WAW;Lo;0;R;;;;;N;;;;;
x   0633;ARABIC LETTER SEEN;Lo;0;R;;;;;N;;;;;
y   064A;ARABIC LETTER YEH;Lo;0;R;;;;;N;ARABIC LETTER YA;;;;
z   0632;ARABIC LETTER ZAIN;Lo;0;R;;;;;N;;;;;
Z   0638;ARABIC LETTER ZAH;Lo;0;R;;;;;N;ARABIC LETTER DHAH;;;;

*/

/**
 * This class implements KeyRemap to produce transliterated Arabic
 * characters from Latin-1 characters.
 */

final class ArabicTransliteration extends KeyRemap {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public char remap(char c) {

        switch (c) {
            case '0': return '\u0660'; // ARABIC-INDIC DIGIT ZERO
            case '1': return '\u0661'; // ARABIC-INDIC DIGIT ONE
            case '2': return '\u0662'; // ARABIC-INDIC DIGIT TWO
            case '3': return '\u0663'; // ARABIC-INDIC DIGIT THREE
            case '4': return '\u0664'; // ARABIC-INDIC DIGIT FOUR
            case '5': return '\u0665'; // ARABIC-INDIC DIGIT FIVE
            case '6': return '\u0666'; // ARABIC-INDIC DIGIT SIX
            case '7': return '\u0667'; // ARABIC-INDIC DIGIT SEVEN
            case '8': return '\u0668'; // ARABIC-INDIC DIGIT EIGHT
            case '9': return '\u0669'; // ARABIC-INDIC DIGIT NINE

            case '%': return '\u066A'; // ARABIC PERCENT SIGN
            // the Traditional Arabic font does not contain this character
            // case '.': return '\u066B'; // ARABIC DECIMAL SEPARATOR
            case ',': return '\u060C'; // ARABIC COMMA
            case '-': return '\u0640'; // ARABIC TATWEEL
            case '\'': return '\u0652'; // ARABIC SUKUN
            case '"': return '\u0651'; // ARABIC SHADDA
            case ';': return '\u061B'; // ARABIC SEMICOLON
            case '?': return '\u061F'; // ARABIC QUESTION MARK

            case 'a': return '\u0627'; // ARABIC LETTER ALEF
            case 'A': return '\u0639'; // ARABIC LETTER AIN
            case 'b': return '\u0628'; // ARABIC LETTER BEH
            case 'B': return '\u0628'; // ARABIC LETTER BEH
            case 'c': return '\u0635'; // ARABIC LETTER SAD
            case 'C': return '\u0635'; // ARABIC LETTER SAD
            case 'd': return '\u062F'; // ARABIC LETTER DAL
            case 'D': return '\u0630'; // ARABIC LETTER THAL
            case 'e': return '\u064E'; // ARABIC FATHA
            case 'E': return '\u064B'; // ARABIC FATHATAN
            case 'f': return '\u0641'; // ARABIC LETTER FEH
            case 'F': return '\u0641'; // ARABIC LETTER FEH
            case 'g': return '\u063A'; // ARABIC LETTER GHAIN
            case 'G': return '\u063A'; // ARABIC LETTER GHAIN
            case 'h': return '\u062D'; // ARABIC LETTER HAH
            case 'H': return '\u0647'; // ARABIC LETTER HEH
            case 'i': return '\u0650'; // ARABIC KASRA
            case 'I': return '\u064D'; // ARABIC KASRATAN
            case 'j': return '\u062C'; // ARABIC LETTER JEEM
            case 'J': return '\u062C'; // ARABIC LETTER JEEM
            case 'k': return '\u0643'; // ARABIC LETTER KAF
            case 'K': return '\u062E'; // ARABIC LETTER KHAH
            case 'l': return '\u0644'; // ARABIC LETTER LAM
            case 'L': return '\u0644'; // ARABIC LETTER LAM
            case 'm': return '\u0645'; // ARABIC LETTER MEEM
            case 'M': return '\u0645'; // ARABIC LETTER MEEM
            case 'n': return '\u0646'; // ARABIC LETTER NOON
            case 'N': return '\u0646'; // ARABIC LETTER NOON
            case 'o': return '\u064F'; // ARABIC DAMMA
            case 'O': return '\u064F'; // ARABIC DAMMA
            case 'p': return '\u0628'; // ARABIC LETTER BEH
            case 'P': return '\u0628'; // ARABIC LETTER BEH
            case 'q': return '\u0642'; // ARABIC LETTER QAF
            case 'Q': return '\u0642'; // ARABIC LETTER QAF
            case 'r': return '\u0631'; // ARABIC LETTER REH
            case 'R': return '\u0631'; // ARABIC LETTER REH
            case 's': return '\u0633'; // ARABIC LETTER SEEN
            case 'S': return '\u0634'; // ARABIC LETTER SHEEN
            case 't': return '\u062A'; // ARABIC LETTER TEH
            case 'T': return '\u062B'; // ARABIC LETTER THEH
            case 'U': return '\u064C'; // ARABIC DAMMATAN
            case 'u': return '\u064F'; // ARABIC DAMMA
            case 'v': return '\u0641'; // ARABIC LETTER FEH
            case 'V': return '\u0641'; // ARABIC LETTER FEH
            case 'w': return '\u0648'; // ARABIC LETTER WAW
            case 'W': return '\u0648'; // ARABIC LETTER WAW
            case 'x': return '\u0633'; // ARABIC LETTER SEEN
            case 'X': return '\u0633'; // ARABIC LETTER SEEN
            case 'y': return '\u064A'; // ARABIC LETTER YEH
            case 'Y': return '\u064A'; // ARABIC LETTER YEH
            case 'z': return '\u0632'; // ARABIC LETTER ZAIN
            case 'Z': return '\u0638'; // ARABIC LETTER ZAH
        }

        return c;
    }
}
