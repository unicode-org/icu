/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/UCD.java,v $
* $Date: 2001/08/31 00:30:17 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.HashMap;
import java.util.BitSet;
import java.util.Map;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import com.ibm.text.utility.*;


public final class UCD implements UCD_Types {
    /**
     * Used for the default version.
     */
    public static final String latestVersion = "3.1.1";

    /**
     * Create singleton instance for default (latest) version
     */
    public static UCD make() {
        return make("");
    }

    /**
     * Create singleton instance for the specific version
     */
    public static UCD make(String version) {
        if (version == null || version.length() == 0) version = latestVersion;
        UCD result = (UCD)versionCache.get(version);
        if (result == null) {
            result = new UCD();
            result.fillFromFile(version);
            versionCache.put(version, result);
        }
        return result;
    }

    /**
     * Get the version of the UCD
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the date that the data was parsed
     */
    public long getDate() {
        return date;
    }

    /**
     * Is the code point allocated?
     */
    public boolean isAllocated(int codePoint) {
        if (getCategory(codePoint) != Cn) return true;
        if ((codePoint & 0xFFFE) == 0xFFFE) {
            if (major < 2 && codePoint > 0xFFFF) return false;
            return true;         // Noncharacter
        }
        if (codePoint >= 0xFDD0 && codePoint <= 0xFDEF && major >= 3 && minor >= 1) return true;
        return false;
    }

    /**
     * Is the code point assigned to a character (or surrogate)
     */
    public boolean isAssigned(int codePoint) {
        return getCategory(codePoint) != Cn;
    }

    /**
     * Is the code point a PUA character (fast check)
     */
    public boolean isPUA(int codePoint) {
        return (codePoint >= 0xE000 && codePoint < 0xF900
             || codePoint >= 0xF0000 && codePoint < 0xFFFFE
             || codePoint >= 0x100000 && codePoint < 0x10FFFE);
    }

    /**
     * Many ranges are elided in the UCD. All but the first are not actually
     * represented in the data internally. This detects such cases.
     */
    public boolean isRepresented(int codePoint) {
        return getRaw(codePoint) != null;
    }

    /**
     * Return XML version of the data associated with the code point.
     */
    public String toString(int codePoint) {
        return get(codePoint, true).toString(FULL);
    }

    /**
     * Get the character name.
     */
    public String getName(int codePoint) {
        return get(codePoint, true).name;
    }

    /**
     * Get the character names for the code points in a string, separated by ", "
     */
    public String getName(String s) {
        if (s.length() == 1) return get(s.charAt(0), true).name;
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(", ");
            result.append(getName(cp));
        }
        return result.toString();
    }

    /**
     * Get the code in U+ notation
     */
    public static String getCode(int codePoint) {
        return "U+" + Utility.hex(codePoint);
    }

    /**
     * Get the code in U+ notation
     */
    public static String getCode(String s) {
        if (s.length() == 1) return getCode(s.charAt(0)); // fast path
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(", ");
            result.append(getCode(cp));
        }
        return result.toString();
    }

    /**
     * Get the name and number (U+xxxx NAME) for a code point
     */
    public String getCodeAndName(int codePoint) {
        return getCode(codePoint) + " " + getName(codePoint);
    }

    /**
     * Get the name and number (U+xxxx NAME) for the code points in a string,
     * separated by ", "
     */
    public String getCodeAndName(String s) {
        if (s == null || s.length() == 0) return "NULL";
        if (s.length() == 1) return getCodeAndName(s.charAt(0)); // fast path
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(", ");
            result.append(getCodeAndName(cp));
        }
        return result.toString();
    }

    /**
     * Get the general category
     */
    public byte getCategory(int codePoint) {
        return get(codePoint, false).generalCategory;
    }

    /**
     * Get the main category, as a mask
     */
    public static int mainCategoryMask(byte cat) {
        switch (cat) {
          case Lu: case Ll: case Lt: case Lm: case Lo: return LETTER_MASK;
          case Mn: case Me: case Mc: return MARK_MASK;
          case Nd: case Nl: case No: return NUMBER_MASK;
          case Zs: case Zl: case Zp: return SEPARATOR_MASK;
          case Cc: case Cf: case Cs: case Co: return CONTROL_MASK;
          case Pc: case Pd: case Ps: case Pe: case Po: case Pi: case Pf: return PUNCTUATION_MASK;
          case Sm: case Sc: case Sk: case So: return SYMBOL_MASK;
          case Cn: return UNASSIGNED_MASK;
        }
        throw new IllegalArgumentException ("Illegal General Category " + cat);
    }

    /**
     * Get the combining class, a number between zero and 255. Returned
     * as a short to avoid the signed-byte problem in Java
     */
    public short getCombiningClass(int codePoint) {
        return (short)(get(codePoint, false).combiningClass & 0xFF);
    }

    /**
     * Does this combining class actually occur in this version of the data.
     */
    public boolean isCombiningClassUsed(byte value) {
        return combiningClassSet.get(0xFF & value);
    }

    /**
     * Get the bidi class
     */
    public byte getBidiClass(int codePoint) {
        return get(codePoint, false).bidiClass;
    }

    /**
     * Get the RAW decomposition mapping. Must be used recursively for the full mapping!
     */
    public String getDecompositionMapping(int codePoint) {
        return get(codePoint, true).decompositionMapping;
    }

    /**
     * Get BIDI mirroring character, if there is one.
     */
    public String getBidiMirror(int codePoint) {
        return get(codePoint, true).bidiMirror;
    }

    /**
     * Get the RAW decomposition type: the <...> field in the UCD data.
     */
    public byte getDecompositionType(int codePoint) {
        return get(codePoint, false).decompositionType;
    }

    public float getNumericValue(int codePoint) {
        return get(codePoint, false).numericValue;
    }

    public byte getNumericType(int codePoint) {
        return get(codePoint, false).numericType;
    }

    public String getCase(int codePoint, byte simpleVsFull, byte caseType) {
        return getCase(codePoint, simpleVsFull, caseType, "");
    }

    public String getCase(String s, byte simpleVsFull, byte caseType) {
        return getCase(s, simpleVsFull, caseType, "");
    }

    public String getCase(int codePoint, byte simpleVsFull, byte caseType, String condition) {
        UData udata = get(codePoint, true);
        if (caseType < LOWER || caseType > FOLD
          || (simpleVsFull != SIMPLE && simpleVsFull != FULL)) {
            throw new IllegalArgumentException("simpleVsFull or caseType out of bounds");
        }
        if (caseType < FOLD) {
            if (simpleVsFull == FULL && udata.specialCasing.length() != 0) {
                if (condition.length() == 0
                || udata.specialCasing.indexOf(condition) < 0) {
                    simpleVsFull = SIMPLE;
                }
            }
        } else {
            // special case. For these characters alone, use "I" as option meaning collapse to "i"
            //if (codePoint == 0x0131 || codePoint == 0x0130) { // special case turkish i
            if (getBinaryProperty(codePoint, CaseFoldTurkishI)) {
                if (!udata.specialCasing.equals("I")) simpleVsFull = SIMPLE;
                else simpleVsFull = FULL;
            }
        }

        switch (caseType + simpleVsFull) {
            case SIMPLE + UPPER: return udata.simpleUppercase;
            case SIMPLE + LOWER: return udata.simpleLowercase;
            case SIMPLE + TITLE: return udata.simpleTitlecase;
            case SIMPLE + FOLD: return udata.simpleCaseFolding;
            case FULL + UPPER: return udata.fullUppercase;
            case FULL + LOWER: return udata.fullLowercase;
            case FULL + TITLE: return udata.fullTitlecase;
            case FULL + FOLD: return udata.fullCaseFolding;
        }
        throw new IllegalArgumentException("getCase: " + caseType + ", " + simpleVsFull);
    }

    public String getCase(String s, byte simpleVsFull, byte caseType, String condition) {
        if (UTF32.length32(s) == 1) return getCase(UTF32.char32At(s, 0), simpleVsFull, caseType);
        StringBuffer result = new StringBuffer();
        int cp;
        byte currentCaseType = caseType;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            String mappedVersion = getCase(cp, simpleVsFull, currentCaseType, condition);
            result.append(mappedVersion);
            if (caseType == TITLE) {
                // if letter is cased, change to lowercase, otherwise change to TITLE
                byte cat = getCategory(cp);
                if (cat == Mn || cat == Me || cat == Mc) {
                    // ignore!
                } else if (cat == Lu || cat == Ll || cat == Lt
                  || getBinaryProperty(cp, Other_Lowercase)
                  || getBinaryProperty(cp, Other_Uppercase)) {
                    currentCaseType = LOWER;
                } else {
                    currentCaseType = TITLE;
                }
            }
        }
        return result.toString();
    }

    /*
    public String getSimpleLowercase(int codePoint) {
        return get(codePoint, true).simpleLowercase;
    }

    public String getSimpleUppercase(int codePoint) {
        return get(codePoint, true).simpleUppercase;
    }

    public String getSimpleTitlecase(int codePoint) {
        return get(codePoint, true).simpleTitlecase;
    }

    public String getSimpleCaseFolding(int codePoint) {
        return get(codePoint, true).simpleCaseFolding;
    }

    public String getFullLowercase(int codePoint) {
        return get(codePoint, true).fullLowercase;
    }

    public String getFullUppercase(int codePoint) {
        return get(codePoint, true).fullUppercase;
    }

    public String getFullTitlecase(int codePoint) {
        return get(codePoint, true).fullTitlecase;
    }

    public String getFullCaseFolding(int codePoint) {
        return get(codePoint, true).simpleCaseFolding;
    }

    public String getLowercase(int codePoint, boolean full) {
        if (full) return getFullLowercase(codePoint);
        return getSimpleLowercase(codePoint);
    }

    public String getUppercase(int codePoint, boolean full) {
        if (full) return getFullUppercase(codePoint);
        return getSimpleLowercase(codePoint);
    }

    public String getTitlecase(int codePoint, boolean full) {
        if (full) return getFullTitlecase(codePoint);
        return getSimpleTitlecase(codePoint);
    }

    public String getCaseFolding(int codePoint, boolean full) {
        if (full) return getFullCaseFolding(codePoint);
        return getSimpleCaseFolding(codePoint);
    }

    public String getLowercase(String s, boolean full) {
        if (s.length() == 1) return getLowercase(s.charAt(0), true);
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(", ");
            result.append(getLowercase(cp, true));
        }
        return result.toString();
    }

    public String getUppercase(String s, boolean full) {
        if (s.length() == 1) return getUppercase(s.charAt(0), true);
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(", ");
            result.append(getUppercase(cp, true));
        }
        return result.toString();
    }

    public String getTitlecase(String s, boolean full) {
        if (s.length() == 1) return getTitlecase(s.charAt(0), true);
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(", ");
            result.append(getTitlecase(cp, true));
        }
        return result.toString();
    }

    public String getCaseFolding(String s, boolean full) {
        if (s.length() == 1) return getCaseFolding(s.charAt(0), true);
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(", ");
            result.append(getCaseFolding(cp, true));
        }
        return result.toString();
    }
    */

    public String getSpecialCase(int codePoint) {
        return get(codePoint, true).specialCasing;
    }

    public byte getEastAsianWidth(int codePoint) {
        return get(codePoint, false).eastAsianWidth;
    }

    public byte getLineBreak(int codePoint) {
        return get(codePoint, false).lineBreak;
    }

    public byte getScript(int codePoint) {
        return get(codePoint, false).script;
    }

    public byte getAge(int codePoint) {
        return get(codePoint, false).age;
    }

    public byte getJoiningType(int codePoint) {
        return get(codePoint, false).joiningType;
    }

    public byte getJoiningGroup(int codePoint) {
        return get(codePoint, false).joiningGroup;
    }

    public int getBinaryProperties(int codePoint) {
        return get(codePoint, false).binaryProperties;
    }

    public boolean getBinaryProperty(int codePoint, int bit) {
        return (get(codePoint, false).binaryProperties & (1<<bit)) != 0;
    }

    // ENUM Mask Utilties

    public int getCategoryMask(int codePoint) {
        return 1<<get(codePoint, false).generalCategory;
    }

    public int getBidiClassMask(int codePoint) {
        return 1<<get(codePoint, false).bidiClass;
    }

    public int getNumericTypeMask(int codePoint) {
        return 1<<get(codePoint, false).numericType;
    }

    public int getDecompositionTypeMask(int codePoint) {
        return 1<<get(codePoint, false).decompositionType;
    }

    public int getEastAsianWidthMask(int codePoint) {
        return 1<<get(codePoint, false).eastAsianWidth;
    }

    public int getLineBreakMask(int codePoint) {
        return 1<<get(codePoint, false).lineBreak;
    }

    public int getScriptMask(int codePoint) {
        return 1<<get(codePoint, false).script;
    }

    public int getAgeMask(int codePoint) {
        return 1<<get(codePoint, false).age;
    }

    public int getJoiningTypeMask(int codePoint) {
        return 1<<get(codePoint, false).joiningType;
    }

    public int getJoiningGroupMask(int codePoint) {
        return 1<<get(codePoint, false).joiningGroup;
    }


    // VERSIONS WITH NAMES

    public String getCategoryID(int codePoint) {
        return getCategoryID_fromIndex(getCategory(codePoint));
    }

    public static String getCategoryID_fromIndex(byte prop) {
        return UCD_Names.GC[prop];
    }

    public String getBidiClassID(int codePoint) {
        return getBidiClassID_fromIndex(getBidiClass(codePoint));
    }

    public static String getBidiClassID_fromIndex(byte prop) {
        return UCD_Names.BC[prop];
    }

    public String getCombiningClassID(int codePoint) {
        return getCombiningClassID_fromIndex(getCombiningClass(codePoint));
    }

    public static String getCombiningClassID_fromIndex(short cc) {
        return cc + "";
    }

    public String getDecompositionTypeID(int codePoint) {
        return getDecompositionTypeID_fromIndex(getDecompositionType(codePoint));
    }

    public static String getDecompositionTypeID_fromIndex(byte prop) {
        return UCD_Names.DT[prop];
    }

    public String getNumericTypeID(int codePoint) {
        return getNumericTypeID_fromIndex(getNumericType(codePoint));
    }

    public static String getNumericTypeID_fromIndex(byte prop) {
        return UCD_Names.NT[prop];
    }

    public String getEastAsianWidthID(int codePoint) {
        return getEastAsianWidthID_fromIndex(getEastAsianWidth(codePoint));
    }

    public static String getEastAsianWidthID_fromIndex(byte prop) {
        return UCD_Names.EA[prop];
    }

    public String getLineBreakID(int codePoint) {
        return getLineBreakID_fromIndex(getLineBreak(codePoint));
    }

    public static String getLineBreakID_fromIndex(byte prop) {
        return UCD_Names.LB[prop];
    }

    public String getJoiningTypeID(int codePoint) {
        return getJoiningTypeID_fromIndex(getJoiningType(codePoint));
    }

    public static String getJoiningTypeID_fromIndex(byte prop) {
        return UCD_Names.JOINING_TYPE[prop];
    }

    public String getJoiningGroupID(int codePoint) {
        return getJoiningGroupID_fromIndex(getJoiningGroup(codePoint));
    }

    public static String getJoiningGroupID_fromIndex(byte prop) {
        return UCD_Names.JOINING_GROUP[prop];
    }

    public String getScriptID(int codePoint) {
        return getScriptID_fromIndex(getScript(codePoint));
    }

    public static String getScriptID_fromIndex(byte prop) {
        return UCD_Names.SCRIPT[prop];
    }

    public String getAgeID(int codePoint) {
        return getAgeID_fromIndex(getAge(codePoint));
    }

    public static String getAgeID_fromIndex(byte prop) {
        return UCD_Names.AGE[prop];
    }

    public String getBinaryPropertiesID(int codePoint, byte bit) {
        return (getBinaryProperties(codePoint) & (1<<bit)) != 0 ? "Y" : "N";
    }

    public static String getBinaryPropertiesID_fromIndex(byte bit) {
        return UCD_Names.BP[bit];
    }

    public static int mapToRepresentative(int ch, boolean old) {
        if (ch <= 0xFFFD) {
            //if (ch <= 0x2800) return ch;
            //if (ch <= 0x28FF) return 0x2800;    // braille
            if (ch <= 0x3400) return ch;         // CJK Ideograph Extension A
            if (ch <= 0x4DB5) return 0x3400;
            if (ch <= 0x4E00) return ch;         // CJK Ideograph
            if (ch <= 0x9FA5) return 0x4E00;
            if (ch <= 0xAC00) return ch;         // Hangul Syllable
            if (ch <= 0xD7A3) return 0xAC00;
            if (ch <= 0xD800) return ch;         // Non Private Use High Surrogate
            if (ch <= 0xDB7F) return 0xD800;
            if (ch <= 0xDB80) return ch;         // Private Use High Surrogate
            if (ch <= 0xDBFF) return 0xDB80;
            if (ch <= 0xDC00) return ch;         // Low Surrogate
            if (ch <= 0xDFFF) return 0xDC00;
            if (ch <= 0xE000) return ch;         // Private Use
            if (ch <= 0xF8FF) return 0xE000;
            if (old) {
                if (ch <= 0xF900) return ch;         // CJK Compatibility Ideograp
                if (ch <= 0xFA2D) return 0xF900;
            }
            if (ch <  0xFDD0) return ch;         // Noncharacter
            if (ch <= 0xFDEF) return 0xFFFF;
        } else {
            if ((ch & 0xFFFE) == 0xFFFE) return 0xFFFF;         // Noncharacter
            if (ch <= 0x20000) return ch;         // Extension B
            if (ch <= 0x2A6D6) return 0x20000;
            //if (ch <= 0x2F800) return ch;
            //if (ch <= 0x2FA1D) return 0x2F800;      // compat ideographs
            if (ch <= 0xF0000) return ch;       // Plane 15 Private Use
            if (ch <= 0xFFFFD) return 0xF0000;       // Plane 16 Private Use
            if (ch <= 0x100000) return ch;       // Plane 15 Private Use
            if (ch <= 0x10FFFD) return 0x100000;       // Plane 16 Private Use
        }
        return ch;
    }

    public boolean isIdentifierStart(int cp, boolean extended) {
        if (extended) {
            if (cp == 0x0E33 || cp == 0x0EB3 || cp == 0xFF9E || cp == 0xFF9F) return false;
            if (cp == 0x037A || cp >= 0xFC5E && cp <= 0xFC63 || cp == 0xFDFA || cp == 0xFDFB) return false;
            if (cp >= 0xFE70 && cp <= 0xFE7E && (cp & 1) == 0) return false;
        }
        byte cat = getCategory(cp);
        if (cat == Lu || cat == Ll || cat == Lt || cat == Lm || cat == Lo || cat == Nl) return true;
        return false;
    }

    public boolean isIdentifierContinue_NO_Cf(int cp, boolean extended) {
        if (isIdentifierStart(cp, extended)) return true;
        if (extended) {
            if (cp == 0x00B7) return true;
            if (cp == 0x0E33 || cp == 0x0EB3 || cp == 0xFF9E || cp == 0xFF9F) return true;
        }
        byte cat = getCategory(cp);
        if (cat == Mn || cat == Mc || cat == Nd || cat == Pc) return true;
        return false;
    }

    public boolean isIdentifier(String s, boolean extended) {
        if (s.length() == 0) return false; // at least one!
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i == 0) {
                if (!isIdentifierStart(cp, extended)) return false;
            } else {
                if (!isIdentifierContinue_NO_Cf(cp, extended)) return false;
            }
        }
        return true;
    }
    /*
Middle Dot. Because most Catalan legacy data will be encoded in Latin-1, U+00B7 MIDDLE DOT needs to be
allowed in <identifier_extend>.

In particular, the following four characters should be in <identifier_extend> and not <identifier_start>:
0E33 THAI CHARACTER SARA AM
0EB3 LAO VOWEL SIGN AM
FF9E HALFWIDTH KATAKANA VOICED SOUND MARK
FF9F HALFWIDTH KATAKANA SEMI-VOICED SOUND MARK
Irregularly decomposing characters. U+037A GREEK YPOGEGRAMMENI and certain Arabic presentation
forms have irregular compatibility decompositions, and need to be excluded from both <identifier_start>
and <identifier_extend>. It is recommended that all Arabic presentation forms be excluded from identifiers
in any event, although only a few of them are required to be excluded for normalization
to guarantee identifier closure.
*/

    // *******************
    // PRIVATES
    // *******************

        // cache of singletons
    private static Map versionCache = new HashMap();

    private static final int LIMIT_CODE_POINT = 0x110000;
    private static final UData[] ALL_NULLS = new UData[1024];

    // main data
    private UData[][] data = new UData[LIMIT_CODE_POINT>>10][];

    // extras
    private BitSet combiningClassSet = new BitSet(256);
    private String version;
    private String file;
    private long date = -1;
    private byte format = -1;
    private byte major = -1;
    private byte minor = -1;
    private byte update = -1;
    private int size = -1;

    // cache last UData
    private int lastCode = Integer.MIN_VALUE;
    private UData lastResult = UData.UNASSIGNED;
    private boolean lastCodeFixed = false;

    // hide constructor
    private UCD() {
        for (int i = 0; i < data.length; ++i) {
            data[i] = ALL_NULLS;
        }
    }

    private void add(UData uData) {
        int high = uData.codePoint>>10;
        if (data[high] == ALL_NULLS) {
            UData[] temp = new UData[1024];
            data[high] = temp;
        }
        data[high][uData.codePoint & 0x3FF] = uData;
    }

    public boolean hasComputableName(int codePoint) {
        if (codePoint >= 0xF900 && codePoint <= 0xFA2D) return true;
        int rangeStart = mapToRepresentative(codePoint, major < 2);
        switch (rangeStart) {
          default:
            return getRaw(codePoint) == null;
          case 0x2800: // braille
          case 0xF900: // compat ideos
          case 0x2F800: // compat ideos
          case 0x3400: // CJK Ideograph Extension A
          case 0x4E00: // CJK Ideograph
          case 0x20000: // Extension B
          case 0xAC00: // Hangul Syllable
          case 0xE000: // Private Use
          case 0xF0000: // Private Use
          case 0x100000: // Private Use
          case 0xD800: // Surrogate
          case 0xDB80: // Private Use
          case 0xDC00: // Private Use
          case 0xFFFF: // Noncharacter
            return true;
        }
    }

    private UData getRaw(int codePoint) {
        return data[codePoint>>10][codePoint & 0x3FF];
    }

    // access data for codepoint
    UData get(int codePoint, boolean fixStrings) {
        //if (codePoint == lastCode && fixStrings <= lastCodeFixed) return lastResult;
        /*
        // we play some funny tricks for performance
        // if cp is not represented, it is either in a elided block or missing.
        // elided blocks are either CONTINUE or FFFF

        byte cat;
        if (!ucdData.isRepresented(cp)) {
            int rep = UCD.mapToRepresentative(cp);
            if (rep == 0xFFFF) cat = Cn;
            else if (rep != cp) return CONTINUE;
            else if (!ucdData.isRepresented(rep)) cat = Cn;
            else cat = ucdData.getCategory(rep);
        } else {
            cat = ucdData.getCategory(cp);
        }
        */

        UData result = null;

        // do range stuff
        String constructedName = null;
        int rangeStart = mapToRepresentative(codePoint, major < 2);
        boolean isHangul = false;
        switch (rangeStart) {
          case 0xF900:
            if (major < 2) {
                if (fixStrings) constructedName = "CJK COMPATIBILITY IDEOGRAPH-" + Utility.hex(codePoint, 4);
                break;
            }
            // FALL THROUGH!!!!
          default:
            result = getRaw(codePoint);
            if (result == null) {
                result = UData.UNASSIGNED;
                if (fixStrings) result.name = "<unassigned-" + Utility.hex(codePoint, 4) + ">";
            }
            return result;
          case 0x3400: // CJK Ideograph Extension A
          case 0x4E00: // CJK Ideograph
          case 0x20000: // Extension B
            if (fixStrings) constructedName = "CJK UNIFIED IDEOGRAPH-" + Utility.hex(codePoint, 4);
            break;
          case 0xAC00: // Hangul Syllable
            isHangul = true;
            if (fixStrings) {
                constructedName = "HANGUL SYLLABLE " + getHangulName(codePoint);
            }
            break;
          case   0xE000: // Private Use
          case  0xF0000: // Private Use
          case 0x100000: // Private Use
            if (fixStrings) constructedName = "<private use-" + Utility.hex(codePoint, 4) + ">";
            break;
          case 0xD800: // Surrogate
          case 0xDB80: // Private Use
          case 0xDC00: // Private Use
            if (fixStrings) constructedName = "<surrogate-" + Utility.hex(codePoint, 4) + ">";
            break;
          case 0xFFFF: // Noncharacter
            if (fixStrings) constructedName = "<noncharacter-" + Utility.hex(codePoint, 4) + ">";
            break;
        }
        result = getRaw(rangeStart);
        if (result == null) {
            result = UData.UNASSIGNED;
            if (fixStrings) result.name = "<unassigned-" + Utility.hex(codePoint, 4) + ">";
            return result;
        }

        result.codePoint = codePoint;
        if (fixStrings) {
            result.name = constructedName;
            result.decompositionMapping = result.bidiMirror
            = result.simpleLowercase = result.simpleUppercase = result.simpleTitlecase = result.simpleCaseFolding
            = result.fullLowercase = result.fullUppercase = result.fullTitlecase = result.fullCaseFolding
            = UTF32.valueOf32(codePoint);
        }
        if (isHangul) {
            if (fixStrings) result.decompositionMapping = getHangulDecompositionPair(codePoint);
            result.decompositionType = CANONICAL;
        }
        return result;
    }

    // Hangul constants

    static final int
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount,   // 11172
        LLimit = LBase + LCount,    // 1113
        VLimit = VBase + VCount,    // 1176
        TLimit = TBase + TCount,    // 11C3
        SLimit = SBase + SCount;    // D7A4

    private static String getHangulName(int s) {
        int SIndex = s - SBase;
        if (0 > SIndex || SIndex >= SCount) {
            throw new IllegalArgumentException("Not a Hangul Syllable: " + s);
        }
        int LIndex = SIndex / NCount;
        int VIndex = (SIndex % NCount) / TCount;
        int TIndex = SIndex % TCount;
        // if (true) return "?";
        return UCD_Names.JAMO_L_TABLE[LIndex] + UCD_Names.JAMO_V_TABLE[VIndex] + UCD_Names.JAMO_T_TABLE[TIndex];
    }

    private static final char[] pair = new char[2];

    static String getHangulDecompositionPair(int ch) {
        int SIndex = ch - SBase;
        if (0 > SIndex || SIndex >= SCount) {
            return "";
        }
        int TIndex = SIndex % TCount;
        if (TIndex != 0) { // triple
            pair[0] = (char)(SBase + SIndex - TIndex);
            pair[1] = (char)(TBase + TIndex);
        } else {
            pair[0] = (char)(LBase + SIndex / NCount);
            pair[1] = (char)(VBase + (SIndex % NCount) / TCount);
        }
        return String.valueOf(pair);
    }

    static int composeHangul(int char1, int char2) {
        if (LBase <= char1 && char1 < LLimit && VBase <= char2 && char2 < VLimit) {
            return (SBase + ((char1 - LBase) * VCount + (char2 - VBase)) * TCount);
        }
        if (SBase <= char1 && char1 < SLimit && TBase <= char2 && char2 < TLimit
                && ((char1 - SBase) % TCount) == 0) {
            return char1 + (char2 - TBase);
        }
        return 0xFFFF; // no composition
    }

    static boolean isTrailingJamo(int cp) {
        return (VBase <= cp && cp < VLimit) || (TBase <= cp && cp < TLimit);
    }

    private void fillFromFile(String version) {
        DataInputStream dataIn = null;
        String fileName = BIN_DIR + "UCD_Data" + version + ".bin";
        int uDataFileCount = 0;
        try {
            dataIn = new DataInputStream(
                new BufferedInputStream(
                    new FileInputStream(fileName),
                    128*1024));
            // header
            format = dataIn.readByte();
            major = dataIn.readByte();
            minor = dataIn.readByte();
            update = dataIn.readByte();
            String foundVersion = major + "." + minor + "." + update;
            if (format != BINARY_FORMAT || !version.equals(foundVersion)) {
                throw new ChainException("Illegal data file format for {0}: {1}, {2}",
                    new Object[]{version, new Byte(format), foundVersion});
            }
            date = dataIn.readLong();
            size = uDataFileCount = dataIn.readInt();

            boolean didJoiningHack = false;


            // records
            for (int i = 0; i < uDataFileCount; ++i) {
                UData uData = new UData();
                uData.readBytes(dataIn);

                if (uData.codePoint == 0x2801) {
                    System.out.println("SPOT-CHECK: " + uData);
                }

                //T = Mc + (Cf - ZWNJ - ZWJ)
                int cp = uData.codePoint;
                    byte old = uData.joiningType;
                    byte cat = uData.generalCategory;
                //if (cp == 0x200D) {
                  //  uData.joiningType = JT_C;
                //} else
                if (cp != 0x200D && cp != 0x200C && (cat == Mn || cat == Cf)) {
                    uData.joiningType = JT_T;
                }
                if (!didJoiningHack && uData.joiningType != old) {
                    System.out.println("HACK: Setting "
                        + UCD_Names.LONG_JOINING_TYPE[uData.joiningType]
                        + ": " + Utility.hex(cp) + " " + uData.name);
                    didJoiningHack = true;
                }

                combiningClassSet.set(uData.combiningClass & 0xFF);
                add(uData);
            }
            /*
            if (update == -1) {
                throw new ChainException("Data File truncated for ",
                    new Object[]{version}, e);
            }
            if (size != fileSize) {
                throw new ChainException("Counts do not match: file {0}, records {1}",
                    new Object[]{new Integer(fileSize), new Integer(size)});
            }
            */
            // everything is ok!
            this.version = version;
            this.file = fileName;
            //+ " " + new File(fileName).lastModified();
        } catch (IOException e) {
            throw new ChainException("Can't read data file for {0}", new Object[]{version}, e);
        } finally {
            if (dataIn != null) {
                try {
                    dataIn.close();
                } catch (IOException e) {}
            }
        }
    }
}