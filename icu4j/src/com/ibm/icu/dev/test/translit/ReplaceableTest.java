package com.ibm.icu.dev.test.translit;
import com.ibm.icu.dev.test.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import com.ibm.icu.impl.Utility;
import java.io.*;
import java.util.BitSet;
import java.util.HashMap;
import java.text.ParseException;

/**
 * @test
 * @summary Round trip test of Transliterator
 */
public class ReplaceableTest extends TestFmwk {
	
	public static final boolean LATER_THAN_2_1 = false;
	
    public static void main(String[] args) throws Exception {
        new ReplaceableTest().run(args);
    }
  
    public void Test() throws IOException, ParseException {
        check("Lower", "ABCD", "1234");
        check("Upper", "abcd\u00DF", "123455"); // must map 00DF to SS
        check("Title", "aBCD", "1234");
        check("NFC", "A\u0300E\u0300", "13");
        check("NFD", "\u00C0\u00C8", "1122");
        if (!LATER_THAN_2_1) return;
        check("*(x) > A $1 B", "wxy", "11223");
        check("*(x)(y) > A $2 B $1 C $2 D", "wxyz", "113322334");
        check("*(x)(y)(z) > A $3 B $2 C $1 D", "wxyzu", "114433225");
    }
    
    void check(String transliteratorName, String test, String shouldProduceStyles) {
        TestReplaceable tr = new TestReplaceable(test, null);
        String original = tr.toString();
        
        Transliterator t;
        if (transliteratorName.startsWith("*")) {
        	transliteratorName = transliteratorName.substring(1);
        	t = new RuleBasedTransliterator("test", transliteratorName);
        } else {
        	t = Transliterator.getInstance(transliteratorName);
        }
        t.transliterate(tr);
        String newStyles = tr.getStyles();
        if (!newStyles.equals(shouldProduceStyles)) {
            errln("FAIL Styles: " + transliteratorName + " ( "
                + original + " ) => " + tr.toString() + "; should be {" + shouldProduceStyles + "}!");
        } else {
            logln("OK: " + transliteratorName + " ( " + original + " ) => " + tr.toString());
        }
    }
    

    /**
     * This is a test class that simulates styled text.
     * It associates a style number (0..65536) with each character,
     * and maintains that style in the normal fashion:
     * When setting text from raw string or characters,<br>
     * Set the styles to the style of the first character replaced.<br>
     * If no characters are replaced, use the style of the previous character.<br>
     * If at start, use the following character<br>
     * Otherwise use defaultStyle.
     */
    static class TestReplaceable implements Replaceable {
        ReplaceableString chars;
        ReplaceableString styles;
        
        char defaultStyle = '_';
        
        TestReplaceable (String text, String styles) {
            chars = new ReplaceableString(text);
            StringBuffer s = new StringBuffer();
            for (int i = 0; i < text.length(); ++i) {
                if (styles != null && i < styles.length()) {
                    s.append(styles.charAt(i));
                } else {
                    s.append((char) (i + '1'));
                }
            }
            this.styles = new ReplaceableString(s.toString());
        }
        
        public String getStyles() {
            return styles.toString();
        }
        
        public String toString() {
            return chars.toString() + "{" + styles.toString() + "}";
        }

        public String substring(int start, int limit) {
            return chars.substring(start, limit);
        }

        public int length() {
            return chars.length();
        }

        public char charAt(int offset) {
            return chars.charAt(offset);
        }

        public int char32At(int offset) {
            return chars.char32At(offset);
        }

        public void getChars(int srcStart, int srcLimit, char dst[], int dstStart) {
            chars.getChars(srcStart, srcLimit, dst, dstStart);
        }

        public void replace(int start, int limit, String text) {
            if (substring(start,limit).equals(text)) return; // NO ACTION!
            chars.replace(start, limit, text);
            fixStyles(start, limit, text.length());
        }
        
        public void replace(int start, int limit, char[] chars,
                            int charsStart, int charsLen) {
            if (substring(start,limit).equals(new String(chars, charsStart, charsLen-charsStart))) return; // NO ACTION!
            this.chars.replace(start, limit, chars, charsStart, charsLen);
            fixStyles(start, limit, charsLen-charsStart);
        }

        void fixStyles(int start, int limit, int newLen) {
            char newStyle = defaultStyle;
            if (start != limit) {
                newStyle = styles.charAt(start);
            } else if (start > 0) {
                newStyle = styles.charAt(start-1);
            } else if (limit < styles.length() - 1) {
                newStyle = styles.charAt(limit+1);
            }
            // dumb implementation for now.
            StringBuffer s = new StringBuffer();
            for (int i = 0; i < newLen; ++i) {
                s.append(newStyle);
            }
            styles.replace(start, limit, s.toString());
        }

        public void copy(int start, int limit, int dest) {
            chars.copy(start, limit, dest);
            styles.copy(start, limit, dest);
        }
        
        public boolean hasMetaData() {
        	return true;
        }
    }
}
