/*
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/internal/Attic/UInfo.java,v $ 
 * $Date: 2001/08/23 00:57:21 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.internal;

import java.io.*;
import java.util.*;
import com.ibm.util.Utility;

public final class UInfo {
    static final boolean DEBUG = false;
    static final int UINFO_VERSION = 5;

    // these values are aligned with the java.lang.Character constants

    public static final byte
    UNASSIGNED      = 0,
    UPPERCASE_LETTER    = 1,
    LOWERCASE_LETTER    = 2,
    TITLECASE_LETTER    = 3,
    MODIFIER_LETTER     = 4,
    OTHER_LETTER        = 5,
    NON_SPACING_MARK    = 6,
    ENCLOSING_MARK      = 7,
    COMBINING_SPACING_MARK  = 8,
    DECIMAL_DIGIT_NUMBER    = 9,
    LETTER_NUMBER       = 10,
    OTHER_NUMBER        = 11,
    SPACE_SEPARATOR     = 12,
    LINE_SEPARATOR      = 13,
    PARAGRAPH_SEPARATOR = 14,
    CONTROL         = 15,
    FORMAT          = 16,
    PRIVATE_USE     = 18,
    SURROGATE       = 19,
    DASH_PUNCTUATION    = 20,
    START_PUNCTUATION   = 21,
    END_PUNCTUATION     = 22,
    CONNECTOR_PUNCTUATION   = 23,
    OTHER_PUNCTUATION   = 24,
    MATH_SYMBOL     = 25,
    CURRENCY_SYMBOL     = 26,
    MODIFIER_SYMBOL     = 27,
    OTHER_SYMBOL        = 28;

    public String getName(char ch) {return getInfo(ch).name;}
    public String getDecomposition(char ch) {return getInfo(ch).decomposition;}
    public String getName10(char ch) {return getInfo(ch).name10;}
    public String getComment(char ch) {return getInfo(ch).comment;}

    public float getNumeric(char ch) {return getInfo(ch).numeric;}

    public short getCanonicalClass(char ch) {return getInfo(ch).canonical;}
    public short getDecimal(char ch) {return getInfo(ch).decimal;}
    public short getDigit(char ch) {return getInfo(ch).digit;}

    public char getUppercase(char ch) {return getInfo(ch).uppercase;}
    public char getLowercase(char ch) {return getInfo(ch).lowercase;}
    public char getTitlecase(char ch) {return getInfo(ch).titlecase;}

    public byte getCategory(char ch) {return getInfo(ch).category;}
    public byte getBidiClass(char ch) {return getInfo(ch).bidi;}
    public boolean getMirrored(char ch) {return getInfo(ch).mirrored;}

    public boolean isDisparaged(char ch) { return getDecomposition(ch).length() == 4; }

    public boolean isLetter(char ch) {
        return (0 != ((1<<getCategory(ch)) &
          ((1<<UPPERCASE_LETTER)
          | (1<<LOWERCASE_LETTER)
          | (1<<TITLECASE_LETTER)
          | (1<<MODIFIER_LETTER)
          | (1<<MODIFIER_LETTER))));
    }

    public boolean isMark(char ch) {
        return (0 != ((1<<getCategory(ch)) &
          ((1<<NON_SPACING_MARK)
          | (1<<ENCLOSING_MARK)
          | (1<<COMBINING_SPACING_MARK))));
    }

    public boolean isNumber(char ch) {
        return (0 != ((1<<getCategory(ch)) &
          ((1<<DECIMAL_DIGIT_NUMBER)
          | (1<<LETTER_NUMBER)
          | (1<<OTHER_NUMBER))));
    }

    public boolean isSeparator(char ch) {
        return (0 != ((1<<getCategory(ch)) &
          ((1<<SPACE_SEPARATOR)
          | (1<<LINE_SEPARATOR)
          | (1<<PARAGRAPH_SEPARATOR))));
    }

    public boolean isFormat(char ch) {
        return (0 != ((1<<getCategory(ch)) &
          ((1<<CONTROL)
          | (1<<FORMAT))));
    }

    public boolean isPunctuation(char ch) {
        return (0 != ((1<<getCategory(ch)) &
          ((1<<DASH_PUNCTUATION)
          | (1<<START_PUNCTUATION)
          | (1<<END_PUNCTUATION)
          | (1<<CONNECTOR_PUNCTUATION)
          | (1<<START_PUNCTUATION)
          | (1<<END_PUNCTUATION)
          | (1<<OTHER_PUNCTUATION))));
    }

    public boolean isSymbol(char ch) {
        return (0 != ((1<<getCategory(ch)) &
          ((1<<MATH_SYMBOL)
          | (1<<CURRENCY_SYMBOL)
          | (1<<MODIFIER_SYMBOL)
          | (1<<OTHER_SYMBOL))));
    }

    //
    // Characters excluded from composition.  This is read from the Unicode
    // file CompositionExclusions.txt.
    //
    String composeExclude = "";

    /**
     * Is this character excluded from the composition algorithm by virtue
     * of being listed in the composition exclusion table in Tech Report #15?
     */
    public boolean isExcludedComposition(char ch) {
        return isDisparaged(ch)
            || composeExclude.indexOf(ch) >= 0
            || getCanonicalClass(getDecompositionChars(ch).charAt(0)) != 0;
    }



    public String getName(String s) {
        return getName(s,true);
    }

    public String getName(String s, boolean shortVersion) {
        StringBuffer temp = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            if (i != 0) temp.append(", ");
            temp.append(getName(s.charAt(i), shortVersion));
        }
        return temp.toString();
    }

    public String getName(char ch, boolean shortVersion) {
        String result = getName(ch);
        if (!shortVersion) return result;
        result = replace(result,"LETTER ","");
        result = replace(result,"CHARACTER ","");
        result = replace(result,"SIGN ","");
        result = replace(result,"CAPITAL ","UC ");
        if (getCategory(ch) == LOWERCASE_LETTER)
          result = replace(result,"SMALL ","LC ");
        result = replace(result,"COMBINING ","-");
        result = replace(result,"WITH ","");
        result = replace(result,"AND ","");
        result = replace(result,"VARIA","GRAVE");
        result = replace(result,"OXIA","ACUTE");
        result = replace(result,"VRACHY","BREVE");
        result = replace(result,"VERTICAL LINE ABOVE","TONOS");
        result = replace(result,"PSILI","SMOOTH");
        result = replace(result,"DASIA","ROUGH");
        result = replace(result,"COMMA ABOVE","SMOOTH");
        result = replace(result,"REVERSED COMMA ABOVE","ROUGH");
        result = replace(result,"YPOGEGRAMMENI","IOTA-SUB");
        result = replace(result,"PROSGEGRAMMENI","IOTA-AD");
        result = replace(result,"DIALYTIKA","DIAERESIS");
        result = replace(result,"PERISPOMENI","CIRCUMFLEX");
        result = replace(result,"VOICED SOUND MARK","VOICED SIGN");
        result = replace(result,"PROLONGED SOUND MARK","VOICED SIGN");
        result = replace(result,"KATAKANA-HIRAGANA","KANA");
        result = replace(result,"COMPATIBILITY IDEOGRAPH-","");
        result = replace(result,"CHOSEONG","INITIAL");
        result = replace(result,"JUNGSEONG","MEDIAL");
        result = replace(result,"JONGSEONG","FINAL");

        return result.substring(0,1)
          + result.substring(1,result.length()).toLowerCase();
    }

    public String replace(String source,
      String replacee, String replacer) {
        int p = source.indexOf(replacee);
        if (p == -1) return source;
        return source.substring(0,p)
          + replacer
          + source.substring(p+replacee.length(),source.length());
    }

    public boolean isCCS(String s) {
        if (s.length() < 2) return false;
        if (isMark(s.charAt(0))) return false;
        for (int i = 1; i < s.length(); ++i) {
            if (!isMark(s.charAt(i))) return false;
        }
        return true;
    }

    // combining base sequence := <cat_zero>+ <cat_pos>*
    public boolean isCBS(String s) {
        if (s.length() == 0) return false;
        if (getCanonicalClass(s.charAt(0)) != 0) return false;
        boolean gotGreater = false;
        for (int i = 1; i < s.length(); ++i) {
            if (getCanonicalClass(s.charAt(i)) == 0) {
                if (gotGreater) return false;
            } else {
                gotGreater = true;
            }
        }
        return true;
    }

    public boolean hasCanonicalDecomposition(char ch) {
        String decomp = getDecomposition(ch);
        return (decomp.length() != 0 && decomp.indexOf('<') == -1);
    }

    public boolean hasCompatibilityDecomposition(char ch) {
        String decomp = getDecomposition(ch);
        return (decomp.length() != 0 && decomp.indexOf('<') != -1);
    }

    public boolean isEquivalent(
      String a, String b, boolean canonical) {
        return getFullDecomposition(a, canonical).equals(
          getFullDecomposition(b, canonical));
    }

    // use very dumb algorithm. Don't need lower order one.

    public String getFullDecomposition(
      String s, boolean canonical) {
        StringBuffer output = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            getFullDecomp2(s.charAt(i),canonical,output);
        }
        return fixCanonical(output).toString();
    }

    public StringBuffer getFullDecomposition(
      char ch, boolean canonical, StringBuffer output) {

        StringBuffer result = getFullDecomp2(ch,canonical,output);
        return fixCanonical(result);
    }

    public String getFullDecomposition(
      char ch, boolean canonical) {
        return getFullDecomposition(ch, canonical, new StringBuffer()).toString();
    }

    /**
     * Given a decomposed string of characters, put it in canonical
     * order by finding and processing all exchangeable pairs.
     */
    public StringBuffer fixCanonical(StringBuffer target) {
        for (int i = 1; i < target.length(); ++i) {
            char ch = target.charAt(i);
            short canClass = getCanonicalClass(ch);
            char chPrev = target.charAt(i-1);
            short canClassPrev = getCanonicalClass(chPrev);
            if (canClass != 0 && canClass < canClassPrev) {
                target.setCharAt(i-1, ch);
                target.setCharAt(i, chPrev);
                if (i > 1) i -= 2; // backup (-1 to compensate for loop)
            }
        }
        return target;
    }

    public String fixCanonical(String source) {
        return fixCanonical(new StringBuffer(source)).toString();
    }


    // ============================================
    //                  PRIVATES
    // ============================================

    static class CharData {
        public CharData() {
        };

        String name = "";
        String decomposition = "";
        String name10 = "";
        String comment = "";

        float numeric = Float.MIN_VALUE;

        short canonical = 0;
        short decimal = Short.MIN_VALUE;
        short digit = Short.MIN_VALUE;

        char uppercase;
        char lowercase;
        char titlecase;

        byte category;
        byte bidi = 0;

        boolean mirrored;
    };

    private static final CharData UNASSIGNED_INFO = new CharData();
    private static char cachedChar = 0xFFFF;

    private CharData getInfo(char ch) {
        if (ch == cachedChar) return UNASSIGNED_INFO;
        // remap special ranges
        if (ch >= 0x4E00 && ch < 0xF900) {
            if (ch <= 0x9FA5) ch = 0x4E00;
            else if (ch >= 0xAC00 && ch <= 0xD7A3) ch = 0xAC00;
            else if (ch >= 0xD800 && ch <= 0xDFFF) ch = 0xD800;
            else if (ch >= 0xE000) ch = 0xE000;
        }
        Object value = cache[ch];
        CharData result;
        if (value == null) {
            result = UNASSIGNED_INFO;
        } else if (value instanceof String) {
            result = updateCache((String)value);
        } else {
            result = (CharData)value;
        }
        return result;
    }

    private StringBuffer getFullDecomp2(
      char ch, boolean canonical, StringBuffer output) {

        String decomp = getDecomposition(ch);
        if (decomp.length() == 0
          || (canonical && decomp.indexOf('<') != -1)) {
            output.append(ch);
            return output;
        }
        boolean inBrackets = false;
        for (int i = 0; i < decomp.length(); ++i) {
            char c = decomp.charAt(i);
            if (c == '<') inBrackets = true;
            else if (c == '>') inBrackets = false;
            else if (inBrackets) ; // skip
            else if (c == ' ') ; // skip
            else {
                String tempString = decomp.substring(i,i+4);
                char temp = (char)Integer.parseInt(tempString,16);
                getFullDecomposition(temp,canonical,output);
                i+= 3;
            }
        }
        return output;
    }

    public String getDecompositionChars(char ch) {
        StringBuffer output = new StringBuffer();
        String decomp = getDecomposition(ch);
        if (decomp.length() == 0) {
            output.append(ch);
            return output.toString();
        }
        boolean inBrackets = false;
        for (int i = 0; i < decomp.length(); ++i) {
            char c = decomp.charAt(i);
            if (c == '<') inBrackets = true;
            else if (c == '>') inBrackets = false;
            else if (inBrackets) ; // skip
            else if (c == ' ') ; // skip
            else {
                String tempString = decomp.substring(i,i+4);
                char temp = (char)Integer.parseInt(tempString,16);
                output.append(temp);
                i+= 3;
            }
        }
        return output.toString();
    }

    public UInfo(String fileName, String composeExcludeFileName) {
        long startTime,endTime;

        BufferedReader input = null;
        String line = null;
        try {
            input = new BufferedReader(new FileReader(fileName),64*1024);
            for (int count = 0;;++count) {
                line = input.readLine();
                if (line == null) break;
                if (line.length() == 0) continue;
                char ch = charFrom(line.substring(0,4));
                if (DEBUG) if ((count % 100) == 0)
                    System.out.println("[" + count + "," + Utility.hex(ch) + ']');
                cache[ch] = line;
            }

            // Read composition exlusions
            input = new BufferedReader(new FileReader(composeExcludeFileName),64*1024);
            StringBuffer ce = new StringBuffer();
            for (;;) {
                line = input.readLine();
                if (line == null) break;
                if (line.length() == 0 ||
                    Character.digit(line.charAt(0), 16) < 0) continue;
                ce.append(charFrom(line.substring(0,4)));
            }
            composeExclude = ce.toString();
        } catch (Exception ex) {
            try {
                input.close();
            } catch (Exception ex2) {}
            ex.printStackTrace();
            throw new IllegalArgumentException("Couldn't read file "
              + ex.getClass().getName() + " " + ex.getMessage()
              + " line = " + line
              );
        }
    }

    public UInfo() {
        // FIX
        // This is bad...this path must be correct relative to the
        // user's current directory.  I have changed it so that it's
        // relative to the root icu4j directory, so it works as long
        // as code is run from that directory, e.g., "java -classpath
        // classes...".  A better way to do this might be to get it
        // from a system property that is defined on the command line,
        // e.g., "java -Dicu4j=D:/icu4j..." - liu
        this("src/data/unicode/UnicodeData-3.0.0.txt",
             "src/data/unicode/CompositionExclusions-1.txt");
    }

    /*
  0 Code value in 4-digit hexadecimal format.
  1 Unicode 2.1 Character Name. These names match exactly the
  2 General Category. This is a useful breakdown into various "character
  3 Canonical Combining Classes. The classes used for the
  4 Bidirectional Category. See the list below for an explanation of the
  5 Character Decomposition. In the Unicode Standard, not all of
  6 Decimal digit value. This is a numeric field. If the character
  7 Digit value. This is a numeric field. If the character represents a
  8 Numeric value. This is a numeric field. If the character has the
  9 If the characters has been identified as a "mirrored" character in
 10 Unicode 1.0 Name. This is the old name as published in Unicode 1.0.
 11 10646 Comment field. This field is informative.
 12 Upper case equivalent mapping. If a character is part of an
 13 Lower case equivalent mapping. Similar to 12. This field is informative.
 14 Title case equivalent mapping. Similar to 12. This field is informative.
    */

    private CharData updateCache(String line) {
        try {
            String[] parts = new String[30];
            Utility.split(line,';',parts);
            CharData info = new CharData();
            char ch = charFrom(parts[0]);
            info.name = parts[1];
            info.category = (byte)Utility.lookup(parts[2], CATEGORY_TABLE);
            info.canonical = shortFrom(parts[3]);
            info.bidi = (byte)Utility.lookup(parts[4], BIDI_TABLE);
            info.decomposition = parts[5];
            info.decimal = shortFrom(parts[6]);
            info.digit = shortFrom(parts[7]);
            info.numeric = floatFrom(parts[8]);
            info.mirrored = charFrom(parts[9]) == 'Y';
            info.name10 = parts[10];
            info.comment = parts[11];
            info.uppercase = charFrom(parts[12]);
            if (info.uppercase == 0) info.uppercase = ch;
            info.lowercase = charFrom(parts[13]);
            if (info.lowercase == 0) info.lowercase = ch;
            info.titlecase = charFrom(parts[14]);
            if (info.titlecase == 0) info.titlecase = info.uppercase;
            String trial = Utility.hex(ch) + ";" + info;
            if (DEBUG) if (!trial.equals(line)) {
                System.out.println("Difference between:");
                System.out.println(line);
                System.out.println(trial);
            }
            cache[ch] = info;
            return info;
        }
        catch (NumberFormatException e) {
            System.out.println("updateCache: error parsing '" + line + "'");
            throw e;
        }
    }

    private static CharData typeInfo = new CharData();

    private boolean latin1(char c) {
        return ((c >= 20 && c <= 0x7F) || c > 0xA0);
    }

    private static final String[] YN_TABLE = {"N", "Y"};

    private static final String[] CATEGORY_TABLE = {
        "Cn", // = Other, Not Assigned

        "Lu", // = Letter, Uppercase
        "Ll", // = Letter, Lowercase
        "Lt", // = Letter, Titlecase
        "Lm", // = Letter, Modifier
        "Lo", // = Letter, Other

        "Mn", // = Mark, Non-Spacing
        "Me", // = Mark, Enclosing
        "Mc", // = Mark, Spacing Combining

        "Nd", // = Number, Decimal Digit
        "Nl", // = Number, Letter
        "No", // = Number, Other

        "Zs", // = Separator, Space
        "Zl", // = Separator, Line
        "Zp", // = Separator, Paragraph

        "Cc", // = Other, Control
        "Cf", // = Other, Format
        "",   // unused
        "Co", // = Other, Private Use
        "Cs", // = Other, Surrogate


        "Pd", // = Punctuation, Dash
        "Ps", // = Punctuation, Open
        "Pe", // = Punctuation, Close
        "Pc", // = Punctuation, Connector
        "Po", // = Punctuation, Other

        "Sm", // = Symbol, Math
        "Sc", // = Symbol, Currency
        "Sk", // = Symbol, Modifier
        "So", // = Symbol, Other

        "Pi", // = Punctuation, Initial quote (may behave like Ps or Pe depending on usage)
        "Pf", // = Punctuation, Final quote (may behave like Ps or Pe dependingon usage)
    };

    private static String[] BIDI_TABLE = {
        "L", // Left-Right; Most alphabetic, syllabic, and logographic characters (e.g., CJK ideographs)
        "R", // Right-Left; Arabic, Hebrew, and punctuation specific to those scripts
        "EN", //    European Number
        "ES", //    European Number Separator
        "ET", //    European Number Terminator
        "AN", //    Arabic Number
        "CS", //    Common Number Separator
        "B", // Block Separator
        "S", // Segment Separator
        "WS", //    Whitespace
        "ON" // Other Neutrals ; All other characters: punctuation, symbols
    };

    private static short shortFrom(String p) {
        if (p.length() == 0) return Short.MIN_VALUE;
        return Short.parseShort(p);
    }

    private static float floatFrom(String p) {
        try {
            if (p.length() == 0) return Float.MIN_VALUE;
            int fract = p.indexOf('/');
            if (fract == -1) return Float.valueOf(p).floatValue();
            String q = p.substring(0,fract);
            float num = 0;
            if (q.length() != 0) num = Integer.parseInt(q);
            p = p.substring(fract+1,p.length());
            float den = 0;
            if (p.length() != 0) den = Integer.parseInt(p);
            return num/den;
        }
        catch (NumberFormatException e) {
            System.out.println("floatFrom: error parsing '" + p + "'");
            throw e;
        }
    }

    private static char charFrom(String p) {
        if (p.length() == 0) return '\u0000';
        else if (p.length() == 1) return p.charAt(0);
        int temp = Integer.parseInt(p, 16);
        if (temp < 0 || temp > 0xFFFF)
            throw new NumberFormatException(
                "Hex char out of range: " + p);
        return (char)temp;
    }


    private Object[] cache = new Object[65536];
}
