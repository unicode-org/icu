/**
*******************************************************************************
* Copyright (C) 1996-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.lang;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.impl.UCharacterUtility;
import com.ibm.icu.impl.UCharacterName;
import com.ibm.icu.impl.UCharacterNameChoice;
import com.ibm.icu.impl.UPropertyAliases;
import com.ibm.icu.lang.UCharacterEnums.*;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.util.RangeValueIterator;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ValueIterator;
import com.ibm.icu.util.VersionInfo;

/**
 * <p>
 * The UCharacter class provides extensions to the 
 * <a href=http://java.sun.com/j2se/1.3/docs/api/java/lang/Character.html>
 * java.lang.Character</a> class. These extensions provide support for 
 * Unicode 3.2 properties and together with the <a href=../text/UTF16.html>UTF16</a> 
 * class, provide support for supplementary characters (those with code 
 * points above U+FFFF).
 * </p>
 * <p>
 * Code points are represented in these API using ints. While it would be 
 * more convenient in Java to have a separate primitive datatype for them, 
 * ints suffice in the meantime.
 * </p>
 * <p>
 * To use this class please add the jar file name icu4j.jar to the 
 * class path, since it contains data files which supply the information used 
 * by this file.<br>
 * E.g. In Windows <br>
 * <code>set CLASSPATH=%CLASSPATH%;$JAR_FILE_PATH/ucharacter.jar</code>.<br>
 * Otherwise, another method would be to copy the files uprops.dat and 
 * unames.icu from the icu4j source subdirectory
 * <i>$ICU4J_SRC/src/com.ibm.icu.impl.data</i> to your class directory 
 * <i>$ICU4J_CLASS/com.ibm.icu.impl.data</i>.
 * </p>
 * <p>
 * Aside from the additions for UTF-16 support, and the updated Unicode 3.1
 * properties, the main differences between UCharacter and Character are:
 * <ul>
 * <li> UCharacter is not designed to be a char wrapper and does not have 
 *      APIs to which involves management of that single char.<br>
 *      These include: 
 *      <ul>
 *        <li> char charValue(), 
 *        <li> int compareTo(java.lang.Character, java.lang.Character), etc.
 *      </ul>
 * <li> UCharacter does not include Character APIs that are deprecated, not 
 *      does it include the Java-specific character information, such as 
 *      boolean isJavaIdentifierPart(char ch).
 * <li> Character maps characters 'A' - 'Z' and 'a' - 'z' to the numeric 
 *      values '10' - '35'. UCharacter also does this in digit and
 *      getNumericValue, to adhere to the java semantics of these
 *      methods.  New methods unicodeDigit, and
 *      getUnicodeNumericValue do not treat the above code points 
 *      as having numeric values.  This is a semantic change from ICU4J 1.3.1.
 * </ul>
 * <p>
 * Further detail differences can be determined from the program 
 *        <a href = http://oss.software.ibm.com/developerworks/opensource/cvs/icu4j/~checkout~/icu4j/src/com/ibm/icu/dev/test/lang/UCharacterCompare.java>
 *        com.ibm.icu.dev.test.lang.UCharacterCompare</a>
 * </p>
 * <p>
 * This class is not subclassable
 * </p>
 * @author Syn Wee Quek
 * @stable ICU 2.1
 * @see com.ibm.icu.lang.UCharacterEnums
 */

public final class UCharacter implements ECharacterCategory, ECharacterDirection
{ 
    // public inner classes ----------------------------------------------
      
    /**
     * A family of character subsets representing the character blocks in the 
     * Unicode specification, generated from Unicode Data file Blocks.txt. 
     * Character blocks generally define characters used for a specific script 
     * or purpose. A character is contained by at most one Unicode block. 
     * @stable ICU 2.4
     */
    public static final class UnicodeBlock extends Character.Subset 
    {
        // blocks objects ---------------------------------------------------
        
        /** 
         * @stable ICU 2.6
         */
        public static final UnicodeBlock NO_BLOCK 
            = new UnicodeBlock("NO_BLOCK", 0);

        /** 
         * @stable ICU 2.4
         */
        public static final UnicodeBlock BASIC_LATIN 
            = new UnicodeBlock("BASIC_LATIN", 1);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock LATIN_1_SUPPLEMENT 
            = new UnicodeBlock("LATIN_1_SUPPLEMENT", 2);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock LATIN_EXTENDED_A
            = new UnicodeBlock("LATIN_EXTENDED_A", 3);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock LATIN_EXTENDED_B 
            = new UnicodeBlock("LATIN_EXTENDED_B", 4);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock IPA_EXTENSIONS 
            = new UnicodeBlock("IPA_EXTENSIONS", 5);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SPACING_MODIFIER_LETTERS 
            = new UnicodeBlock("SPACING_MODIFIER_LETTERS", 6);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock COMBINING_DIACRITICAL_MARKS 
            = new UnicodeBlock("COMBINING_DIACRITICAL_MARKS", 7);
        /**
         * Unicode 3.2 renames this block to "Greek and Coptic".
         * @stable ICU 2.4
         */
        public static final UnicodeBlock GREEK
            = new UnicodeBlock("GREEK", 8);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CYRILLIC 
            = new UnicodeBlock("CYRILLIC", 9);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock ARMENIAN 
            = new UnicodeBlock("ARMENIAN", 10);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock HEBREW 
            = new UnicodeBlock("HEBREW", 11);  
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock ARABIC
            = new UnicodeBlock("ARABIC", 12);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SYRIAC 
            = new UnicodeBlock("SYRIAC", 13);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock THAANA 
            = new UnicodeBlock("THAANA", 14);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock DEVANAGARI 
            = new UnicodeBlock("DEVANAGARI", 15);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock BENGALI 
            = new UnicodeBlock("BENGALI", 16);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock GURMUKHI 
            = new UnicodeBlock("GURMUKHI", 17);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock GUJARATI 
            = new UnicodeBlock("GUJARATI", 18);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock ORIYA 
            = new UnicodeBlock("ORIYA", 19);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock TAMIL 
            = new UnicodeBlock("TAMIL", 20);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock TELUGU 
            = new UnicodeBlock("TELUGU", 21);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock KANNADA 
            = new UnicodeBlock("KANNADA", 22);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock MALAYALAM 
            = new UnicodeBlock("MALAYALAM", 23);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SINHALA 
            = new UnicodeBlock("SINHALA", 24);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock THAI 
            = new UnicodeBlock("THAI", 25);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock LAO 
            = new UnicodeBlock("LAO", 26);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock TIBETAN 
            = new UnicodeBlock("TIBETAN", 27);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock MYANMAR 
            = new UnicodeBlock("MYANMAR", 28);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock GEORGIAN 
            = new UnicodeBlock("GEORGIAN", 29);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock HANGUL_JAMO 
            = new UnicodeBlock("HANGUL_JAMO", 30);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock ETHIOPIC 
            = new UnicodeBlock("ETHIOPIC", 31);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CHEROKEE 
            = new UnicodeBlock("CHEROKEE", 32);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS 
            = new UnicodeBlock("UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS", 33);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock OGHAM 
            = new UnicodeBlock("OGHAM", 34);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock RUNIC 
            = new UnicodeBlock("RUNIC", 35);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock KHMER 
            = new UnicodeBlock("KHMER", 36);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock MONGOLIAN 
            = new UnicodeBlock("MONGOLIAN", 37);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock LATIN_EXTENDED_ADDITIONAL 
            = new UnicodeBlock("LATIN_EXTENDED_ADDITIONAL", 38);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock GREEK_EXTENDED 
            = new UnicodeBlock("GREEK_EXTENDED", 39);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock GENERAL_PUNCTUATION 
            = new UnicodeBlock("GENERAL_PUNCTUATION", 40);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SUPERSCRIPTS_AND_SUBSCRIPTS 
            = new UnicodeBlock("SUPERSCRIPTS_AND_SUBSCRIPTS", 41);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CURRENCY_SYMBOLS 
            = new UnicodeBlock("CURRENCY_SYMBOLS", 42);
        /**
         * Unicode 3.2 renames this block to "Combining Diacritical Marks for 
         * Symbols".
         * @stable ICU 2.4
         */
        public static final UnicodeBlock COMBINING_MARKS_FOR_SYMBOLS 
            = new UnicodeBlock("COMBINING_MARKS_FOR_SYMBOLS", 43);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock LETTERLIKE_SYMBOLS 
            = new UnicodeBlock("LETTERLIKE_SYMBOLS", 44);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock NUMBER_FORMS 
            = new UnicodeBlock("NUMBER_FORMS", 45);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock ARROWS 
            = new UnicodeBlock("ARROWS", 46);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock MATHEMATICAL_OPERATORS 
            = new UnicodeBlock("MATHEMATICAL_OPERATORS", 47);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock MISCELLANEOUS_TECHNICAL 
            = new UnicodeBlock("MISCELLANEOUS_TECHNICAL", 48);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CONTROL_PICTURES 
            = new UnicodeBlock("CONTROL_PICTURES", 49);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock OPTICAL_CHARACTER_RECOGNITION 
            = new UnicodeBlock("OPTICAL_CHARACTER_RECOGNITION", 50);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock ENCLOSED_ALPHANUMERICS 
            = new UnicodeBlock("ENCLOSED_ALPHANUMERICS", 51);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock BOX_DRAWING 
            = new UnicodeBlock("BOX_DRAWING", 52);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock BLOCK_ELEMENTS 
            = new UnicodeBlock("BLOCK_ELEMENTS", 53);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock GEOMETRIC_SHAPES 
            = new UnicodeBlock("GEOMETRIC_SHAPES", 54);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock MISCELLANEOUS_SYMBOLS 
            = new UnicodeBlock("MISCELLANEOUS_SYMBOLS", 55);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock DINGBATS 
            = new UnicodeBlock("DINGBATS", 56);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock BRAILLE_PATTERNS 
            = new UnicodeBlock("BRAILLE_PATTERNS", 57);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CJK_RADICALS_SUPPLEMENT 
            = new UnicodeBlock("CJK_RADICALS_SUPPLEMENT", 58);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock KANGXI_RADICALS 
            = new UnicodeBlock("KANGXI_RADICALS", 59);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock IDEOGRAPHIC_DESCRIPTION_CHARACTERS 
            = new UnicodeBlock("IDEOGRAPHIC_DESCRIPTION_CHARACTERS", 60);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CJK_SYMBOLS_AND_PUNCTUATION 
            = new UnicodeBlock("CJK_SYMBOLS_AND_PUNCTUATION", 61);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock HIRAGANA 
            = new UnicodeBlock("HIRAGANA", 62);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock KATAKANA 
            = new UnicodeBlock("KATAKANA", 63);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock BOPOMOFO 
            = new UnicodeBlock("BOPOMOFO", 64);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock HANGUL_COMPATIBILITY_JAMO 
            = new UnicodeBlock("HANGUL_COMPATIBILITY_JAMO", 65);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock KANBUN 
            = new UnicodeBlock("KANBUN", 66);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock BOPOMOFO_EXTENDED 
            = new UnicodeBlock("BOPOMOFO_EXTENDED", 67);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock ENCLOSED_CJK_LETTERS_AND_MONTHS 
            = new UnicodeBlock("ENCLOSED_CJK_LETTERS_AND_MONTHS", 68);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CJK_COMPATIBILITY 
            = new UnicodeBlock("CJK_COMPATIBILITY", 69);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A 
            = new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A", 70);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS 
            = new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS", 71);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock YI_SYLLABLES 
            = new UnicodeBlock("YI_SYLLABLES", 72);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock YI_RADICALS 
            = new UnicodeBlock("YI_RADICALS", 73);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock HANGUL_SYLLABLES 
            = new UnicodeBlock("HANGUL_SYLLABLES", 74);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock HIGH_SURROGATES 
            = new UnicodeBlock("HIGH_SURROGATES", 75);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock HIGH_PRIVATE_USE_SURROGATES 
            = new UnicodeBlock("HIGH_PRIVATE_USE_SURROGATES", 76);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock LOW_SURROGATES 
            = new UnicodeBlock("LOW_SURROGATES", 77);
        /**
         * Same as public static final int PRIVATE_USE.
         * Until Unicode 3.1.1; the corresponding block name was "Private Use";
         * and multiple code point ranges had this block.
         * Unicode 3.2 renames the block for the BMP PUA to "Private Use Area" 
         * and adds separate blocks for the supplementary PUAs.
         * @stable ICU 2.4
         */
        public static final UnicodeBlock PRIVATE_USE_AREA 
            = new UnicodeBlock("PRIVATE_USE_AREA",  78);
        /**
         * Same as public static final int PRIVATE_USE_AREA.
         * Until Unicode 3.1.1; the corresponding block name was "Private Use";
         * and multiple code point ranges had this block.
         * Unicode 3.2 renames the block for the BMP PUA to "Private Use Area" 
         * and adds separate blocks for the supplementary PUAs.
         * @stable ICU 2.4
         */
        public static final UnicodeBlock PRIVATE_USE 
            = PRIVATE_USE_AREA;
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS 
            = new UnicodeBlock("CJK_COMPATIBILITY_IDEOGRAPHS", 79);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock ALPHABETIC_PRESENTATION_FORMS 
            = new UnicodeBlock("ALPHABETIC_PRESENTATION_FORMS", 80);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock ARABIC_PRESENTATION_FORMS_A 
            = new UnicodeBlock("ARABIC_PRESENTATION_FORMS_A", 81);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock COMBINING_HALF_MARKS 
            = new UnicodeBlock("COMBINING_HALF_MARKS", 82);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CJK_COMPATIBILITY_FORMS 
            = new UnicodeBlock("CJK_COMPATIBILITY_FORMS", 83);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SMALL_FORM_VARIANTS 
            = new UnicodeBlock("SMALL_FORM_VARIANTS", 84);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock ARABIC_PRESENTATION_FORMS_B 
            = new UnicodeBlock("ARABIC_PRESENTATION_FORMS_B", 85);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SPECIALS 
            = new UnicodeBlock("SPECIALS", 86);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock HALFWIDTH_AND_FULLWIDTH_FORMS 
            = new UnicodeBlock("HALFWIDTH_AND_FULLWIDTH_FORMS", 87);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock OLD_ITALIC 
            = new UnicodeBlock("OLD_ITALIC", 88);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock GOTHIC 
            = new UnicodeBlock("GOTHIC", 89);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock DESERET 
            = new UnicodeBlock("DESERET", 90);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock BYZANTINE_MUSICAL_SYMBOLS 
            = new UnicodeBlock("BYZANTINE_MUSICAL_SYMBOLS", 91);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock MUSICAL_SYMBOLS 
            = new UnicodeBlock("MUSICAL_SYMBOLS", 92);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock MATHEMATICAL_ALPHANUMERIC_SYMBOLS 
            = new UnicodeBlock("MATHEMATICAL_ALPHANUMERIC_SYMBOLS", 93);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B  
            = new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B", 94);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock 
            CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT 
            = new UnicodeBlock("CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT", 95);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock TAGS 
            = new UnicodeBlock("TAGS", 96);
    
        // New blocks in Unicode 3.2
    
        /** 
         * Unicode 4.0.1 renames the "Cyrillic Supplementary" block to "Cyrillic Supplement".
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock CYRILLIC_SUPPLEMENTARY 
            = new UnicodeBlock("CYRILLIC_SUPPLEMENTARY", 97);
        /** 
         * Unicode 4.0.1 renames the "Cyrillic Supplementary" block to "Cyrillic Supplement".
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        public static final UnicodeBlock CYRILLIC_SUPPLEMENT 
            = new UnicodeBlock("CYRILLIC_SUPPLEMENT", 97);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock TAGALOG 
            = new UnicodeBlock("TAGALOG", 98);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock HANUNOO 
            = new UnicodeBlock("HANUNOO", 99);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock BUHID 
            = new UnicodeBlock("BUHID", 100);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock TAGBANWA 
            = new UnicodeBlock("TAGBANWA", 101);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A 
            = new UnicodeBlock("MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A", 102);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SUPPLEMENTAL_ARROWS_A 
            = new UnicodeBlock("SUPPLEMENTAL_ARROWS_A", 103);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SUPPLEMENTAL_ARROWS_B 
            = new UnicodeBlock("SUPPLEMENTAL_ARROWS_B", 104);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B 
            = new UnicodeBlock("MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B", 105);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SUPPLEMENTAL_MATHEMATICAL_OPERATORS 
            = new UnicodeBlock("SUPPLEMENTAL_MATHEMATICAL_OPERATORS", 106);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock KATAKANA_PHONETIC_EXTENSIONS 
            = new UnicodeBlock("KATAKANA_PHONETIC_EXTENSIONS", 107);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock VARIATION_SELECTORS 
            = new UnicodeBlock("VARIATION_SELECTORS", 108);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_A 
            = new UnicodeBlock("SUPPLEMENTARY_PRIVATE_USE_AREA_A", 109);
        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_B 
            = new UnicodeBlock("SUPPLEMENTARY_PRIVATE_USE_AREA_B", 110);
   
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock LIMBU 
            = new UnicodeBlock("LIMBU", 111);
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock TAI_LE 
            = new UnicodeBlock("TAI LE", 112);
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock KHMER_SYMBOLS 
            = new UnicodeBlock("KHMER SYMBOLS", 113);

        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock PHONETIC_EXTENSIONS 
            = new UnicodeBlock("PHONETIC EXTENSIONS", 114);

        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock MISCELLANEOUS_SYMBOLS_AND_ARROWS 
            = new UnicodeBlock("MISCELLANEOUS_SYMBOLS_AND_ARROWS", 115);
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock YIJING_HEXAGRAM_SYMBOLS 
            = new UnicodeBlock("YIJING_HEXAGRAM_SYMBOLS", 116);
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock LINEAR_B_SYLLABARY 
            = new UnicodeBlock("LINEAR_B_SYLLABARY", 117);
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock LINEAR_B_IDEOGRAMS 
            = new UnicodeBlock("LINEAR_B_IDEOGRAMS", 118);
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock AEGEAN_NUMBERS 
            = new UnicodeBlock("AEGEAN_NUMBERS", 119);                                               
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock UGARITIC 
            = new UnicodeBlock("UGARITIC", 120);
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock SHAVIAN 
            = new UnicodeBlock("SHAVIAN", 121);
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock OSMANYA 
            = new UnicodeBlock("OSMANYA", 122);
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock CYPRIOT_SYLLABARY 
            = new UnicodeBlock("CYPRIOT_SYLLABARY", 123);
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock TAI_XUAN_JING_SYMBOLS 
            = new UnicodeBlock("TAI_XUAN_JING_SYMBOLS", 124);
        
        /** 
         * @stable ICU 2.6 
         */
        public static final UnicodeBlock VARIATION_SELECTORS_SUPPLEMENT 
            = new UnicodeBlock("VARIATION_SELECTORS_SUPPLEMENT", 125);                                      

        /** 
         * @stable ICU 2.4 
         */
        public static final UnicodeBlock INVALID_CODE 
            = new UnicodeBlock("INVALID_CODE", -1);
           
        // block id corresponding to icu4c -----------------------------------
           
        /** 
         * @stable ICU 2.4 
         */
        public static final int INVALID_CODE_ID = -1;                          
        /** 
         * @stable ICU 2.4
         */
        public static final int BASIC_LATIN_ID = 1;
        /** 
         * @stable ICU 2.4 
         */
        public static final int LATIN_1_SUPPLEMENT_ID = 2;
        /** 
         * @stable ICU 2.4 
         */
        public static final int LATIN_EXTENDED_A_ID = 3;
        /** 
         * @stable ICU 2.4 
         */
        public static final int LATIN_EXTENDED_B_ID = 4;
        /** 
         * @stable ICU 2.4 
         */
        public static final int IPA_EXTENSIONS_ID = 5;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SPACING_MODIFIER_LETTERS_ID = 6;
        /** 
         * @stable ICU 2.4 
         */
        public static final int COMBINING_DIACRITICAL_MARKS_ID = 7;
        /**
         * Unicode 3.2 renames this block to "Greek and Coptic".
         * @stable ICU 2.4
         */
        public static final int GREEK_ID = 8;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CYRILLIC_ID = 9;
        /** 
         * @stable ICU 2.4 
         */
        public static final int ARMENIAN_ID = 10;
        /** 
         * @stable ICU 2.4 
         */
        public static final int HEBREW_ID = 11;  
        /** 
         * @stable ICU 2.4 
         */
        public static final int ARABIC_ID = 12;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SYRIAC_ID = 13;
        /** 
         * @stable ICU 2.4 
         */
        public static final int THAANA_ID = 14;
        /** 
         * @stable ICU 2.4 
         */
        public static final int DEVANAGARI_ID = 15;
        /** 
         * @stable ICU 2.4 
         */
        public static final int BENGALI_ID = 16;
        /** 
         * @stable ICU 2.4 
         */
        public static final int GURMUKHI_ID = 17;
        /** 
         * @stable ICU 2.4 
         */
        public static final int GUJARATI_ID = 18;
        /** 
         * @stable ICU 2.4 
         */
        public static final int ORIYA_ID = 19;
        /** 
         * @stable ICU 2.4 
         */
        public static final int TAMIL_ID = 20;
        /** 
         * @stable ICU 2.4 
         */
        public static final int TELUGU_ID = 21;
        /** 
         * @stable ICU 2.4 
         */
        public static final int KANNADA_ID = 22;
        /** 
         * @stable ICU 2.4 
         */
        public static final int MALAYALAM_ID = 23;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SINHALA_ID = 24;
        /** 
         * @stable ICU 2.4 
         */
        public static final int THAI_ID = 25;
        /** 
         * @stable ICU 2.4 
         */
        public static final int LAO_ID = 26;
        /** 
         * @stable ICU 2.4 
         */
        public static final int TIBETAN_ID = 27;
        /** 
         * @stable ICU 2.4 
         */
        public static final int MYANMAR_ID = 28;
        /** 
         * @stable ICU 2.4 
         */
        public static final int GEORGIAN_ID = 29;
        /** 
         * @stable ICU 2.4 
         */
        public static final int HANGUL_JAMO_ID = 30;
        /** 
         * @stable ICU 2.4 
         */
        public static final int ETHIOPIC_ID = 31;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CHEROKEE_ID = 32;
        /** 
         * @stable ICU 2.4 
         */
        public static final int UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_ID = 33;
        /** 
         * @stable ICU 2.4 
         */
        public static final int OGHAM_ID = 34;
        /** 
         * @stable ICU 2.4 
         */
        public static final int RUNIC_ID = 35;
        /** 
         * @stable ICU 2.4 
         */
        public static final int KHMER_ID = 36;
        /** 
         * @stable ICU 2.4 
         */
        public static final int MONGOLIAN_ID = 37;
        /** 
         * @stable ICU 2.4 
         */
        public static final int LATIN_EXTENDED_ADDITIONAL_ID = 38;
        /** 
         * @stable ICU 2.4 
         */
        public static final int GREEK_EXTENDED_ID = 39;
        /** 
         * @stable ICU 2.4 
         */
        public static final int GENERAL_PUNCTUATION_ID = 40;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SUPERSCRIPTS_AND_SUBSCRIPTS_ID = 41;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CURRENCY_SYMBOLS_ID = 42;
        /**
         * Unicode 3.2 renames this block to "Combining Diacritical Marks for 
         * Symbols".
         * @stable ICU 2.4
         */
        public static final int COMBINING_MARKS_FOR_SYMBOLS_ID = 43;
        /** 
         * @stable ICU 2.4 
         */
        public static final int LETTERLIKE_SYMBOLS_ID = 44;
        /** 
         * @stable ICU 2.4 
         */
        public static final int NUMBER_FORMS_ID = 45;
        /** 
         * @stable ICU 2.4 
         */
        public static final int ARROWS_ID = 46;
        /** 
         * @stable ICU 2.4 
         */
        public static final int MATHEMATICAL_OPERATORS_ID = 47;
        /** 
         * @stable ICU 2.4 
         */
        public static final int MISCELLANEOUS_TECHNICAL_ID = 48;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CONTROL_PICTURES_ID = 49;
        /** 
         * @stable ICU 2.4 
         */
        public static final int OPTICAL_CHARACTER_RECOGNITION_ID = 50;
        /** 
         * @stable ICU 2.4 
         */
        public static final int ENCLOSED_ALPHANUMERICS_ID = 51;
        /** 
         * @stable ICU 2.4 
         */
        public static final int BOX_DRAWING_ID = 52;
        /** 
         * @stable ICU 2.4 
         */
        public static final int BLOCK_ELEMENTS_ID = 53;
        /** 
         * @stable ICU 2.4 
         */
        public static final int GEOMETRIC_SHAPES_ID = 54;
        /** 
         * @stable ICU 2.4 
         */
        public static final int MISCELLANEOUS_SYMBOLS_ID = 55;
        /** 
         * @stable ICU 2.4 
         */
        public static final int DINGBATS_ID = 56;
        /** 
         * @stable ICU 2.4 
         */
        public static final int BRAILLE_PATTERNS_ID = 57;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CJK_RADICALS_SUPPLEMENT_ID = 58;
        /** 
         * @stable ICU 2.4 
         */
        public static final int KANGXI_RADICALS_ID = 59;
        /** 
         * @stable ICU 2.4 
         */
        public static final int IDEOGRAPHIC_DESCRIPTION_CHARACTERS_ID = 60;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CJK_SYMBOLS_AND_PUNCTUATION_ID = 61;
        /** 
         * @stable ICU 2.4 
         */
        public static final int HIRAGANA_ID = 62;
        /** 
         * @stable ICU 2.4 
         */
        public static final int KATAKANA_ID = 63;
        /** 
         * @stable ICU 2.4 
         */
        public static final int BOPOMOFO_ID = 64;
        /** 
         * @stable ICU 2.4 
         */
        public static final int HANGUL_COMPATIBILITY_JAMO_ID = 65;
        /** 
         * @stable ICU 2.4 
         */
        public static final int KANBUN_ID = 66;
        /** 
         * @stable ICU 2.4 
         */
        public static final int BOPOMOFO_EXTENDED_ID = 67;
        /** 
         * @stable ICU 2.4 
         */
        public static final int ENCLOSED_CJK_LETTERS_AND_MONTHS_ID = 68;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CJK_COMPATIBILITY_ID = 69;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A_ID = 70;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CJK_UNIFIED_IDEOGRAPHS_ID = 71;
        /** 
         * @stable ICU 2.4 
         */
        public static final int YI_SYLLABLES_ID = 72;
        /** 
         * @stable ICU 2.4 
         */
        public static final int YI_RADICALS_ID = 73;
        /** 
         * @stable ICU 2.4 
         */
        public static final int HANGUL_SYLLABLES_ID = 74;
        /** 
         * @stable ICU 2.4 
         */
        public static final int HIGH_SURROGATES_ID = 75;
        /** 
         * @stable ICU 2.4 
         */
        public static final int HIGH_PRIVATE_USE_SURROGATES_ID = 76;
        /** 
         * @stable ICU 2.4 
         */
        public static final int LOW_SURROGATES_ID = 77;
        /**
         * Same as public static final int PRIVATE_USE.
         * Until Unicode 3.1.1; the corresponding block name was "Private Use";
         * and multiple code point ranges had this block.
         * Unicode 3.2 renames the block for the BMP PUA to "Private Use Area" 
         * and adds separate blocks for the supplementary PUAs.
         * @stable ICU 2.4
         */
        public static final int PRIVATE_USE_AREA_ID = 78;
        /**
         * Same as public static final int PRIVATE_USE_AREA.
         * Until Unicode 3.1.1; the corresponding block name was "Private Use";
         * and multiple code point ranges had this block.
         * Unicode 3.2 renames the block for the BMP PUA to "Private Use Area" 
         * and adds separate blocks for the supplementary PUAs.
         * @stable ICU 2.4
         */
        public static final int PRIVATE_USE_ID = PRIVATE_USE_AREA_ID;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CJK_COMPATIBILITY_IDEOGRAPHS_ID = 79;
        /** 
         * @stable ICU 2.4 
         */
        public static final int ALPHABETIC_PRESENTATION_FORMS_ID = 80;
        /** 
         * @stable ICU 2.4 
         */
        public static final int ARABIC_PRESENTATION_FORMS_A_ID = 81;
        /** 
         * @stable ICU 2.4 
         */
        public static final int COMBINING_HALF_MARKS_ID = 82;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CJK_COMPATIBILITY_FORMS_ID = 83;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SMALL_FORM_VARIANTS_ID = 84;
        /** 
         * @stable ICU 2.4 
         */
        public static final int ARABIC_PRESENTATION_FORMS_B_ID = 85;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SPECIALS_ID = 86;
        /** 
         * @stable ICU 2.4 
         */
        public static final int HALFWIDTH_AND_FULLWIDTH_FORMS_ID = 87;
        /** 
         * @stable ICU 2.4 
         */
        public static final int OLD_ITALIC_ID = 88;
        /** 
         * @stable ICU 2.4 
         */
        public static final int GOTHIC_ID = 89;
        /** 
         * @stable ICU 2.4 
         */
        public static final int DESERET_ID = 90;
        /** 
         * @stable ICU 2.4 
         */
        public static final int BYZANTINE_MUSICAL_SYMBOLS_ID = 91;
        /** 
         * @stable ICU 2.4 
         */
        public static final int MUSICAL_SYMBOLS_ID = 92;
        /** 
         * @stable ICU 2.4 
         */
        public static final int MATHEMATICAL_ALPHANUMERIC_SYMBOLS_ID = 93;
        /** 
         * @stable ICU 2.4 
         */
        public static final int CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B_ID = 94;
        /** 
         * @stable ICU 2.4 
         */
        public static final int 
            CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT_ID = 95;
        /** 
         * @stable ICU 2.4 
         */
        public static final int TAGS_ID = 96;
    
        // New blocks in Unicode 3.2
    
        /** 
         * Unicode 4.0.1 renames the "Cyrillic Supplementary" block to "Cyrillic Supplement".
         * @stable ICU 2.4 
         */
        public static final int CYRILLIC_SUPPLEMENTARY_ID = 97;
        /** 
         * Unicode 4.0.1 renames the "Cyrillic Supplementary" block to "Cyrillic Supplement".
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */

        public static final int CYRILLIC_SUPPLEMENT_ID = 97;
        /** 
         * @stable ICU 2.4 
         */
        public static final int TAGALOG_ID = 98;
        /** 
         * @stable ICU 2.4 
         */
        public static final int HANUNOO_ID = 99;
        /** 
         * @stable ICU 2.4 
         */
        public static final int BUHID_ID = 100;
        /** 
         * @stable ICU 2.4 
         */
        public static final int TAGBANWA_ID = 101;
        /** 
         * @stable ICU 2.4 
         */
        public static final int MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A_ID = 102;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SUPPLEMENTAL_ARROWS_A_ID = 103;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SUPPLEMENTAL_ARROWS_B_ID = 104;
        /** 
         * @stable ICU 2.4 
         */
        public static final int MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B_ID = 105;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SUPPLEMENTAL_MATHEMATICAL_OPERATORS_ID = 106;
        /** 
         * @stable ICU 2.4 
         */
        public static final int KATAKANA_PHONETIC_EXTENSIONS_ID = 107;
        /** 
         * @stable ICU 2.4 
         */
        public static final int VARIATION_SELECTORS_ID = 108;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SUPPLEMENTARY_PRIVATE_USE_AREA_A_ID = 109;
        /** 
         * @stable ICU 2.4 
         */
        public static final int SUPPLEMENTARY_PRIVATE_USE_AREA_B_ID = 110;
        
        /** 
         * @stable ICU 2.6 
         */
        public static final int LIMBU_ID = 111; /*[1900]*/
        /**
         * @stable ICU 2.6 
         */
        public static final int TAI_LE_ID = 112; /*[1950]*/
        /** 
         * @stable ICU 2.6 
         */
        public static final int KHMER_SYMBOLS_ID = 113; /*[19E0]*/
        /** 
         * @stable ICU 2.6
         */
        public static final int PHONETIC_EXTENSIONS_ID = 114; /*[1D00]*/
        /** 
         * @stable ICU 2.6 
         */
        public static final int MISCELLANEOUS_SYMBOLS_AND_ARROWS_ID = 115; /*[2B00]*/
        /**
         * @stable ICU 2.6 
         */
        public static final int YIJING_HEXAGRAM_SYMBOLS_ID = 116; /*[4DC0]*/
        /** 
         * @stable ICU 2.6 
         */
        public static final int LINEAR_B_SYLLABARY_ID = 117; /*[10000]*/
        /**
         * @stable ICU 2.6 
         */
        public static final int LINEAR_B_IDEOGRAMS_ID = 118; /*[10080]*/
        /** 
         * @stable ICU 2.6
         */
        public static final int AEGEAN_NUMBERS_ID = 119; /*[10100]*/
        /**
         * @stable ICU 2.6
         */
        public static final int UGARITIC_ID = 120; /*[10380]*/
        /**
         * @stable ICU 2.6
         */
        public static final int SHAVIAN_ID = 121; /*[10450]*/
        /**
         * @stable ICU 2.6
         */
        public static final int OSMANYA_ID = 122; /*[10480]*/
        /**
         * @stable ICU 2.6
         */
        public static final int CYPRIOT_SYLLABARY_ID = 123; /*[10800]*/
        /**
         * @stable ICU 2.6
         */
        public static final int TAI_XUAN_JING_SYMBOLS_ID = 124; /*[1D300]*/
        /**
         * @stable ICU 2.6
         */
        public static final int VARIATION_SELECTORS_SUPPLEMENT_ID = 125; /*[E0100]*/
        /** 
         * @stable ICU 2.4 
         */
        public static final int COUNT = 126;
        
        // public methods --------------------------------------------------
        
        /** 
         * Gets the only instance of the UnicodeBlock with the argument ID.
         * If no such ID exists, a INVALID_CODE UnicodeBlock will be returned.
         * @param id UnicodeBlock ID
         * @return the only instance of the UnicodeBlock with the argument ID
         *         if it exists, otherwise a INVALID_CODE UnicodeBlock will be 
         *         returned.
         * @stable ICU 2.4
         */
        public static UnicodeBlock getInstance(int id)
        {
            if (id >= 0 && id < BLOCKS_.length) {
                return BLOCKS_[id];
            }
            return INVALID_CODE;
        }
        
        /**
         * Returns the Unicode allocation block that contains the code point,
         * or null if the code point is not a member of a defined block.
         * @param ch code point to be tested
         * @return the Unicode allocation block that contains the code point
         * @stable ICU 2.4
         */
        public static UnicodeBlock of(int ch)
        {
            if (ch > MAX_VALUE) {
                return INVALID_CODE;
            }

            return UnicodeBlock.getInstance((PROPERTY_.getAdditional(ch, 0)
                         & BLOCK_MASK_) >> BLOCK_SHIFT_);
        }

        /**
         * Internal function returning of(ch).getID().
         *
         * @param ch
         * @return numeric block value
         * @internal
         */
        static int idOf(int ch) {
            if (ch < 0 || ch > MAX_VALUE) {
                return -1;
            }

            return (PROPERTY_.getAdditional(ch, 0) & BLOCK_MASK_) >> BLOCK_SHIFT_;
        }

        /**
         * Cover the JDK 1.5 API.  Return the Unicode block with the
         * given name.  <br/><b>Note</b>: Unlike JDK 1.5, this only matches
         * against the official UCD name and the Java block name
         * (ignoring case).
         * @param blockName the name of the block to match
         * @return the UnicodeBlock with that name
         * @throws IllegalArgumentException if the blockName could not be matched
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        public static final UnicodeBlock forName(String blockName) {
            Map m = null;
            if (mref != null) {
                m = (Map)mref.get();
            }
            if (m == null) {
                m = new HashMap(BLOCKS_.length);
                for (int i = 0; i < BLOCKS_.length; ++i) {
                    UnicodeBlock b = BLOCKS_[i];
                    String name = getPropertyValueName(UProperty.BLOCK, b.getID(), UProperty.NameChoice.LONG);
                    m.put(name.toUpperCase(), b);
            m.put(name.replace('_',' ').toUpperCase(), b);
                    m.put(b.toString().toUpperCase(), b);
                }
                mref = new SoftReference(m);
            }
            UnicodeBlock b = (UnicodeBlock)m.get(blockName.toUpperCase());
            if (b == null) {
                throw new IllegalArgumentException();
            }
            return b;
        }
        private static SoftReference mref;

        /**
         * Returns the type ID of this Unicode block
         * @return integer type ID of this Unicode block
         * @stable ICU 2.4
         */
        public int getID()
        {
            return m_id_;
        }
        
        // private data members ---------------------------------------------
        
        /**
         * Array of UnicodeBlocks, for easy access in getInstance(int)
         */
        private final static UnicodeBlock BLOCKS_[] = {
            NO_BLOCK, BASIC_LATIN, 
            LATIN_1_SUPPLEMENT, LATIN_EXTENDED_A, 
            LATIN_EXTENDED_B, IPA_EXTENSIONS, 
            SPACING_MODIFIER_LETTERS, COMBINING_DIACRITICAL_MARKS,
            GREEK, CYRILLIC,
            ARMENIAN, HEBREW,
            ARABIC, SYRIAC, 
            THAANA, DEVANAGARI, 
            BENGALI, GURMUKHI, 
            GUJARATI, ORIYA, 
            TAMIL, TELUGU, 
            KANNADA, MALAYALAM, 
            SINHALA, THAI, 
            LAO, TIBETAN, 
            MYANMAR, GEORGIAN, 
            HANGUL_JAMO, ETHIOPIC, 
            CHEROKEE, UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS,
            OGHAM, RUNIC, 
            KHMER, MONGOLIAN, 
            LATIN_EXTENDED_ADDITIONAL, GREEK_EXTENDED, 
            GENERAL_PUNCTUATION, SUPERSCRIPTS_AND_SUBSCRIPTS,
            CURRENCY_SYMBOLS, COMBINING_MARKS_FOR_SYMBOLS, 
            LETTERLIKE_SYMBOLS, NUMBER_FORMS, 
            ARROWS, MATHEMATICAL_OPERATORS, 
            MISCELLANEOUS_TECHNICAL, CONTROL_PICTURES,
            OPTICAL_CHARACTER_RECOGNITION, ENCLOSED_ALPHANUMERICS,
            BOX_DRAWING, BLOCK_ELEMENTS,
            GEOMETRIC_SHAPES, MISCELLANEOUS_SYMBOLS,
            DINGBATS, BRAILLE_PATTERNS,
            CJK_RADICALS_SUPPLEMENT, KANGXI_RADICALS,
            IDEOGRAPHIC_DESCRIPTION_CHARACTERS, CJK_SYMBOLS_AND_PUNCTUATION,
            HIRAGANA, KATAKANA, 
            BOPOMOFO, HANGUL_COMPATIBILITY_JAMO,
            KANBUN, BOPOMOFO_EXTENDED, 
            ENCLOSED_CJK_LETTERS_AND_MONTHS, CJK_COMPATIBILITY,
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A, CJK_UNIFIED_IDEOGRAPHS,
            YI_SYLLABLES, YI_RADICALS, 
            HANGUL_SYLLABLES, HIGH_SURROGATES,
            HIGH_PRIVATE_USE_SURROGATES, LOW_SURROGATES,
            PRIVATE_USE_AREA, CJK_COMPATIBILITY_IDEOGRAPHS,
            ALPHABETIC_PRESENTATION_FORMS, ARABIC_PRESENTATION_FORMS_A,
            COMBINING_HALF_MARKS, CJK_COMPATIBILITY_FORMS,
            SMALL_FORM_VARIANTS, ARABIC_PRESENTATION_FORMS_B,
            SPECIALS, HALFWIDTH_AND_FULLWIDTH_FORMS,
            OLD_ITALIC, GOTHIC, 
            DESERET, BYZANTINE_MUSICAL_SYMBOLS,
            MUSICAL_SYMBOLS, MATHEMATICAL_ALPHANUMERIC_SYMBOLS,
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B, 
            CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT, 
            TAGS, CYRILLIC_SUPPLEMENT,
            TAGALOG, HANUNOO, 
            BUHID, TAGBANWA, 
            MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A, SUPPLEMENTAL_ARROWS_A,
            SUPPLEMENTAL_ARROWS_B, MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B,
            SUPPLEMENTAL_MATHEMATICAL_OPERATORS, 
            KATAKANA_PHONETIC_EXTENSIONS,
            VARIATION_SELECTORS, SUPPLEMENTARY_PRIVATE_USE_AREA_A,
            SUPPLEMENTARY_PRIVATE_USE_AREA_B,
            LIMBU, TAI_LE, KHMER_SYMBOLS, PHONETIC_EXTENSIONS,
            MISCELLANEOUS_SYMBOLS_AND_ARROWS, YIJING_HEXAGRAM_SYMBOLS,
            LINEAR_B_SYLLABARY, LINEAR_B_IDEOGRAMS, AEGEAN_NUMBERS,
            UGARITIC, SHAVIAN, OSMANYA, CYPRIOT_SYLLABARY,
            TAI_XUAN_JING_SYMBOLS, VARIATION_SELECTORS_SUPPLEMENT                                      
        };

        static {
            if(COUNT!=BLOCKS_.length) {
                throw new java.lang.InternalError("UnicodeBlock fields are inconsistent!");
            }
        }
        /**
         * Identification code for this UnicodeBlock
         */
        private int m_id_;
        
        // private constructor ----------------------------------------------
        
        /**
         * UnicodeBlock constructor
         * @param name name of this UnicodeBlock
         * @param id unique id of this UnicodeBlock
         * @exception NullPointerException if name is <code>null</code>
         */
        private UnicodeBlock(String name, int id)
        {
            super(name);
            m_id_ = id;
        }
    }
    
    /**
     * East Asian Width constants.
     * @see UProperty#EAST_ASIAN_WIDTH
     * @see UCharacter#getIntPropertyValue
     * @stable ICU 2.4
     */
    public static interface EastAsianWidth 
    {
        /**
         * @stable ICU 2.4
         */
        public static final int NEUTRAL = 0;
        /**
         * @stable ICU 2.4
         */
        public static final int AMBIGUOUS = 1;
        /**
         * @stable ICU 2.4
         */
        public static final int HALFWIDTH = 2;
        /**
         * @stable ICU 2.4
         */
        public static final int FULLWIDTH = 3;
        /**
         * @stable ICU 2.4
         */
        public static final int NARROW = 4;
        /**
         * @stable ICU 2.4
         */
        public static final int WIDE = 5;
        /**
         * @stable ICU 2.4
         */
        public static final int COUNT = 6;
    }

    /**
     * Decomposition Type constants.
     * @see UProperty#DECOMPOSITION_TYPE
     * @stable ICU 2.4
     */
    public static interface DecompositionType 
    {
        /**
         * @stable ICU 2.4
         */
        public static final int NONE = 0;
        /**
         * @stable ICU 2.4
         */
        public static final int CANONICAL = 1;
        /**
         * @stable ICU 2.4
         */
        public static final int COMPAT = 2;
        /**
         * @stable ICU 2.4
         */
        public static final int CIRCLE = 3;
        /**
         * @stable ICU 2.4
         */
        public static final int FINAL = 4;
        /**
         * @stable ICU 2.4
         */
        public static final int FONT = 5;
        /**
         * @stable ICU 2.4
         */
        public static final int FRACTION = 6;
        /**
         * @stable ICU 2.4
         */
        public static final int INITIAL = 7;
        /**
         * @stable ICU 2.4
         */
        public static final int ISOLATED = 8;
        /**
         * @stable ICU 2.4
         */
        public static final int MEDIAL = 9;
        /**
         * @stable ICU 2.4
         */
        public static final int NARROW = 10;
        /**
         * @stable ICU 2.4
         */
        public static final int NOBREAK = 11;
        /**
         * @stable ICU 2.4
         */
        public static final int SMALL = 12;
        /**
         * @stable ICU 2.4
         */
        public static final int SQUARE = 13;
        /**
         * @stable ICU 2.4
         */
        public static final int SUB = 14;
        /**
         * @stable ICU 2.4
         */
        public static final int SUPER = 15;
        /**
         * @stable ICU 2.4
         */
        public static final int VERTICAL = 16;
        /**
         * @stable ICU 2.4
         */
        public static final int WIDE = 17;
        /**
         * @stable ICU 2.4
         */
        public static final int COUNT = 18;
    }
    
    /**
     * Joining Type constants.
     * @see UProperty#JOINING_TYPE
     * @stable ICU 2.4
     */
    public static interface JoiningType 
    {
        /**
         * @stable ICU 2.4
         */
        public static final int NON_JOINING = 0;
        /**
         * @stable ICU 2.4
         */
        public static final int JOIN_CAUSING = 1;
        /**
         * @stable ICU 2.4
         */
        public static final int DUAL_JOINING = 2;
        /**
         * @stable ICU 2.4
         */
        public static final int LEFT_JOINING = 3;
        /**
         * @stable ICU 2.4
         */
        public static final int RIGHT_JOINING = 4;
        /**
         * @stable ICU 2.4
         */
        public static final int TRANSPARENT = 5;
        /**
         * @stable ICU 2.4
         */
        public static final int COUNT = 6;
    }
    
    /**
     * Joining Group constants.
     * @see UProperty#JOINING_GROUP
     * @stable ICU 2.4
     */
    public static interface JoiningGroup 
    {
        /**
         * @stable ICU 2.4
         */
        public static final int NO_JOINING_GROUP = 0;
        /**
         * @stable ICU 2.4
         */
        public static final int AIN = 1;
        /**
         * @stable ICU 2.4
         */
        public static final int ALAPH = 2;
        /**
         * @stable ICU 2.4
         */
        public static final int ALEF = 3;
        /**
         * @stable ICU 2.4
         */
        public static final int BEH = 4;
        /**
         * @stable ICU 2.4
         */
        public static final int BETH = 5;
        /**
         * @stable ICU 2.4
         */
        public static final int DAL = 6;
        /**
         * @stable ICU 2.4
         */
        public static final int DALATH_RISH = 7;
        /**
         * @stable ICU 2.4
         */
        public static final int E = 8;
        /**
         * @stable ICU 2.4
         */
        public static final int FEH = 9;
        /**
         * @stable ICU 2.4
         */
        public static final int FINAL_SEMKATH = 10;
        /**
         * @stable ICU 2.4
         */
        public static final int GAF = 11;
        /**
         * @stable ICU 2.4
         */
        public static final int GAMAL = 12;
        /** 
         * @stable ICU 2.4
         */
        public static final int HAH = 13;
        /**
         * @stable ICU 2.4
         */
        public static final int HAMZA_ON_HEH_GOAL = 14;
        /**
         * @stable ICU 2.4
         */
        public static final int HE = 15;
        /**
         * @stable ICU 2.4
         */
        public static final int HEH = 16;
        /**
         * @stable ICU 2.4
         */
        public static final int HEH_GOAL = 17;
        /**
         * @stable ICU 2.4
         */
        public static final int HETH = 18;
        /**
         * @stable ICU 2.4
         */
        public static final int KAF = 19;
        /**
         * @stable ICU 2.4
         */
        public static final int KAPH = 20;
        /**
         * @stable ICU 2.4
         */
        public static final int KNOTTED_HEH = 21;
        /**
         * @stable ICU 2.4
         */
        public static final int LAM = 22;
        /**
         * @stable ICU 2.4
         */
        public static final int LAMADH = 23;
        /**
         * @stable ICU 2.4
         */
        public static final int MEEM = 24;
        /**
         * @stable ICU 2.4
         */
        public static final int MIM = 25;
        /**
         * @stable ICU 2.4
         */
        public static final int NOON = 26;
        /**
         * @stable ICU 2.4
         */
        public static final int NUN = 27;
        /**
         * @stable ICU 2.4
         */
        public static final int PE = 28;
        /**
         * @stable ICU 2.4
         */
        public static final int QAF = 29;
        /**
         * @stable ICU 2.4
         */
        public static final int QAPH = 30;
        /**
         * @stable ICU 2.4
         */
        public static final int REH = 31;
        /**
         * @stable ICU 2.4
         */
        public static final int REVERSED_PE = 32;
        /**
         * @stable ICU 2.4
         */
        public static final int SAD = 33;
        /**
         * @stable ICU 2.4
         */
        public static final int SADHE = 34;
        /**
         * @stable ICU 2.4
         */
        public static final int SEEN = 35;
        /**
         * @stable ICU 2.4
         */
        public static final int SEMKATH = 36;
        /**
         * @stable ICU 2.4
         */
        public static final int SHIN = 37;
        /**
         * @stable ICU 2.4
         */
        public static final int SWASH_KAF = 38;
        /**
         * @stable ICU 2.4
         */
        public static final int SYRIAC_WAW = 39;
        /**
         * @stable ICU 2.4
         */
        public static final int TAH = 40;
        /**
         * @stable ICU 2.4
         */
        public static final int TAW = 41;
        /**
         * @stable ICU 2.4
         */
        public static final int TEH_MARBUTA = 42;
        /**
         * @stable ICU 2.4
         */
        public static final int TETH = 43;
        /**
         * @stable ICU 2.4
         */
        public static final int WAW = 44;
        /**
         * @stable ICU 2.4
         */
        public static final int YEH = 45;
        /**
         * @stable ICU 2.4
         */
        public static final int YEH_BARREE = 46;
        /**
         * @stable ICU 2.4
         */
        public static final int YEH_WITH_TAIL = 47;
        /**
         * @stable ICU 2.4
         */
        public static final int YUDH = 48;
        /**
         * @stable ICU 2.4
         */
        public static final int YUDH_HE = 49;
        /**
         * @stable ICU 2.4
         */
        public static final int ZAIN = 50;
        /** 
         * @stable ICU 2.6 
         */
        public static final int FE = 51;        
        /** 
         * @stable ICU 2.6 
         */
        public static final int KHAPH = 52;
        /**
         * @stable ICU 2.6 
         */
        public static final int ZHAIN =53;   
        /**
         * @stable ICU 2.4
         */
        public static final int COUNT = 54;
    }
    
    /**
     * Line Break constants.
     * @see UProperty#LINE_BREAK
     * @stable ICU 2.4
     */
    public static interface LineBreak 
    {
        /**
         * @stable ICU 2.4
         */
        public static final int UNKNOWN = 0;
        /**
         * @stable ICU 2.4
         */
        public static final int AMBIGUOUS = 1;
        /**
         * @stable ICU 2.4
         */
        public static final int ALPHABETIC = 2;
        /**
         * @stable ICU 2.4
         */
        public static final int BREAK_BOTH = 3;
        /**
         * @stable ICU 2.4
         */
        public static final int BREAK_AFTER = 4;
        /**
         * @stable ICU 2.4
         */
        public static final int BREAK_BEFORE = 5;
        /**
         * @stable ICU 2.4
         */
        public static final int MANDATORY_BREAK = 6;
        /**
         * @stable ICU 2.4
         */
        public static final int CONTINGENT_BREAK = 7;
        /**
         * @stable ICU 2.4
         */
        public static final int CLOSE_PUNCTUATION = 8;
        /**
         * @stable ICU 2.4
         */
        public static final int COMBINING_MARK = 9;
        /**
         * @stable ICU 2.4
         */
        public static final int CARRIAGE_RETURN = 10;
        /**
         * @stable ICU 2.4
         */
        public static final int EXCLAMATION = 11;
        /**
         * @stable ICU 2.4
         */
        public static final int GLUE = 12;
        /**
         * @stable ICU 2.4
         */
        public static final int HYPHEN = 13;
        /**
         * @stable ICU 2.4
         */
        public static final int IDEOGRAPHIC = 14;
        /**
         * @see #INSEPARABLE
         * @stable ICU 2.4
         */
        public static final int INSEPERABLE = 15;
        /**
         * Renamed from the misspelled "inseperable" in Unicode 4.0.1.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        public static final int INSEPARABLE = 15;
        /**
         * @stable ICU 2.4
         */
        public static final int INFIX_NUMERIC = 16;
        /**
         * @stable ICU 2.4
         */
        public static final int LINE_FEED = 17;
        /**
         * @stable ICU 2.4
         */
        public static final int NONSTARTER = 18;
        /**
         * @stable ICU 2.4
         */
        public static final int NUMERIC = 19;
        /**
         * @stable ICU 2.4
         */
        public static final int OPEN_PUNCTUATION = 20;
        /**
         * @stable ICU 2.4
         */
        public static final int POSTFIX_NUMERIC = 21;
        /**
         * @stable ICU 2.4
         */
        public static final int PREFIX_NUMERIC = 22;
        /**
         * @stable ICU 2.4
         */
        public static final int QUOTATION = 23;
        /**
         * @stable ICU 2.4
         */
        public static final int COMPLEX_CONTEXT = 24;
        /**
         * @stable ICU 2.4
         */
        public static final int SURROGATE = 25;
        /**
         * @stable ICU 2.4
         */
        public static final int SPACE = 26;
        /**
         * @stable ICU 2.4
         */
        public static final int BREAK_SYMBOLS = 27;
        /**
         * @stable ICU 2.4
         */
        public static final int ZWSPACE = 28;
        
        /**
         * @stable ICU 2.6
         */
        public static final int NEXT_LINE = 29;       /*[NL]*/ /* from here on: new in Unicode 4/ICU 2.6 */
        
        /**
         * @stable ICU 2.6
         */
        public static final int  WORD_JOINER = 30;      /*[WJ]*/
        
        /**
         * @stable ICU 2.4
         */
        public static final int COUNT = 31;
    }
    
    /**
     * Numeric Type constants.
     * @see UProperty#NUMERIC_TYPE
     * @stable ICU 2.4
     */
    public static interface NumericType 
    {
        /**
         * @stable ICU 2.4
         */
        public static final int NONE = 0;
        /**
         * @stable ICU 2.4
         */
        public static final int DECIMAL = 1;
        /**
         * @stable ICU 2.4
         */
        public static final int DIGIT = 2;
        /**
         * @stable ICU 2.4
         */
        public static final int NUMERIC = 3;
        /**
         * @stable ICU 2.4
         */
        public static final int COUNT = 4;
    }
    
    /**
     * Hangul Syllable Type constants.
     *
     * @see UProperty#HANGUL_SYLLABLE_TYPE
     * @stable ICU 2.6
     */
    public static interface HangulSyllableType 
    {
        /**
         * @stable ICU 2.6
         */
        public static final int NOT_APPLICABLE      = 0;   /*[NA]*/ /*See note !!*/
        /**
         * @stable ICU 2.6
         */
        public static final int LEADING_JAMO        = 1;   /*[L]*/
        /**
         * @stable ICU 2.6
         */
        public static final int VOWEL_JAMO          = 2;   /*[V]*/
        /**
         * @stable ICU 2.6
         */
        public static final int TRAILING_JAMO       = 3;   /*[T]*/
        /**
         * @stable ICU 2.6
         */
        public static final int LV_SYLLABLE         = 4;   /*[LV]*/
        /**
         * @stable ICU 2.6
         */
        public static final int LVT_SYLLABLE        = 5;   /*[LVT]*/
        /**
         * @stable ICU 2.6
         */
        public static final int COUNT               = 6;
    }

    // public data members -----------------------------------------------
  
    /** 
     * The lowest Unicode code point value.
     * @stable ICU 2.1
     */
    public static final int MIN_VALUE = UTF16.CODEPOINT_MIN_VALUE;

    /**
     * The highest Unicode code point value (scalar value) according to the 
     * Unicode Standard. 
     * This is a 21-bit value (21 bits, rounded up).<br>
     * Up-to-date Unicode implementation of java.lang.Character.MIN_VALUE
     * @stable ICU 2.1
     */
    public static final int MAX_VALUE = UTF16.CODEPOINT_MAX_VALUE; 
      
    /**
     * The minimum value for Supplementary code points
     * @stable ICU 2.1
     */
    public static final int SUPPLEMENTARY_MIN_VALUE = 
        UTF16.SUPPLEMENTARY_MIN_VALUE;
      
    /**
     * Unicode value used when translating into Unicode encoding form and there 
     * is no existing character.
     * @stable ICU 2.1
     */
    public static final int REPLACEMENT_CHAR = '\uFFFD';
        
    /**
     * Special value that is returned by getUnicodeNumericValue(int) when no 
     * numeric value is defined for a code point.
     * @stable ICU 2.4
     * @see #getUnicodeNumericValue
     */
    public static final double NO_NUMERIC_VALUE = -123456789;
    
    // public methods ----------------------------------------------------
      
    /**
     * Retrieves the numeric value of a decimal digit code point.
     * <br>This method observes the semantics of
     * <code>java.lang.Character.digit()</code>.  Note that this
     * will return positive values for code points for which isDigit
     * returns false, just like java.lang.Character.
     * <br><em>Semantic Change:</em> In release 1.3.1 and
     * prior, this did not treat the European letters as having a
     * digit value, and also treated numeric letters and other numbers as 
     * digits.  
     * This has been changed to conform to the java semantics.
     * <br>A code point is a valid digit if and only if:
     * <ul>
     *   <li>ch is a decimal digit or one of the european letters, and
     *   <li>the value of ch is less than the specified radix.
     * </ul>
     * @param ch the code point to query
     * @param radix the radix
     * @return the numeric value represented by the code point in the
     * specified radix, or -1 if the code point is not a decimal digit
     * or if its value is too large for the radix
     * @stable ICU 2.1
     */
    public static int digit(int ch, int radix)
    {
        // when ch is out of bounds getProperty == 0
        int props = getProperty(ch);        
        if (getNumericType(props) != NumericType.DECIMAL) {
            return (radix <= 10) ? -1 : getEuropeanDigit(ch);
        }
        // if props == 0, it will just fall through and return -1
        if (isNotExceptionIndicator(props)) {
        // not contained in exception data
            // getSignedValue is just shifting so we can check for the sign
            // first
            // Optimization
            // int result = UCharacterProperty.getSignedValue(props);
            // if (result >= 0) {
            //    return result;
            // }
            if (props >= 0) {
                return UCharacterProperty.getSignedValue(props);
            }
        }
        else {
            int index = UCharacterProperty.getExceptionIndex(props);
        if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_NUMERIC_VALUE_)) {
                int result = PROPERTY_.getException(index, 
                            UCharacterProperty.EXC_NUMERIC_VALUE_);
                if (result >= 0) {
                    return result;
                }  
            }
        }
       
        if (radix > 10) {
            int result = getEuropeanDigit(ch);
            if (result >= 0 && result < radix) {
                return result;
            }
        }
        return -1;
    }
    
    /**
     * Retrieves the numeric value of a decimal digit code point.
     * <br>This is a convenience overload of <code>digit(int, int)</code> 
     * that provides a decimal radix.
     * <br><em>Semantic Change:</em> In release 1.3.1 and prior, this
     * treated numeric letters and other numbers as digits.  This has
     * been changed to conform to the java semantics.
     * @param ch the code point to query
     * @return the numeric value represented by the code point,
     * or -1 if the code point is not a decimal digit or if its
     * value is too large for a decimal radix 
     * @stable ICU 2.1
     */
    public static int digit(int ch)
    {
        return digit(ch, DECIMAL_RADIX_);
    }

    /** 
     * Returns the numeric value of the code point as a nonnegative 
     * integer.
     * <br>If the code point does not have a numeric value, then -1 is returned. 
     * <br>
     * If the code point has a numeric value that cannot be represented as a 
     * nonnegative integer (for example, a fractional value), then -2 is 
     * returned.
     * @param ch the code point to query
     * @return the numeric value of the code point, or -1 if it has no numeric 
     * value, or -2 if it has a numeric value that cannot be represented as a 
     * nonnegative integer
     * @stable ICU 2.1
     */
    public static int getNumericValue(int ch)
    {
        int props = getProperty(ch);   
        if ((props & NUMERIC_TYPE_MASK_) == NumericType.NONE) {
            return getEuropeanDigit(ch);
        }
        
        // if props == 0, it will just fall through and return -1
        if (isNotExceptionIndicator(props)) {
            // not contained in exception data
            return props >>> 20;
        }
            
        int index = UCharacterProperty.getExceptionIndex(props);
        if (!PROPERTY_.hasExceptionValue(index, 
                     UCharacterProperty.EXC_DENOMINATOR_VALUE_) && 
            PROPERTY_.hasExceptionValue(index, 
                    UCharacterProperty.EXC_NUMERIC_VALUE_)) {
            return PROPERTY_.getException(index, 
                      UCharacterProperty.EXC_NUMERIC_VALUE_);
        }
        
        int europeannumeric = getEuropeanDigit(ch);
        if (europeannumeric >= 0) {
            return europeannumeric;
        }
        return -2;
    }
    
    /**
     * <p>Get the numeric value for a Unicode code point as defined in the 
     * Unicode Character Database.</p>
     * <p>A "double" return type is necessary because some numeric values are 
     * fractions, negative, or too large for int.</p>
     * <p>For characters without any numeric values in the Unicode Character 
     * Database, this function will return NO_NUMERIC_VALUE.</p>
     * <p><em>API Change:</em> In release 2.2 and prior, this API has a
     * return type int and returns -1 when the argument ch does not have a 
     * corresponding numeric value. This has been changed to synch with ICU4C
     * </p>
     * This corresponds to the ICU4C function u_getNumericValue.
     * @param ch Code point to get the numeric value for.
     * @return numeric value of ch, or NO_NUMERIC_VALUE if none is defined.
     * @stable ICU 2.4
     */
    public static double getUnicodeNumericValue(int ch)
    {
        // equivalent to c version double u_getNumericValue(UChar32 c)
        int props = PROPERTY_.getProperty(ch);
        int numericType = getNumericType(props);
        if (numericType > NumericType.NONE && numericType < NumericType.COUNT) {
            if (isNotExceptionIndicator(props)) {
                return UCharacterProperty.getSignedValue(props);
            } 
            else {
                int index = UCharacterProperty.getExceptionIndex(props);
                boolean nex = false;
                boolean dex = false;
                double numerator = 0;
                if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_NUMERIC_VALUE_)) {
                    int num = PROPERTY_.getException(index, 
                             UCharacterProperty.EXC_NUMERIC_VALUE_);
                    // There are special values for huge numbers that are 
                    // powers of ten. genprops/store.c documents:
                    // if numericValue = 0x7fffff00 + x then 
                    // numericValue = 10 ^ x
                    if (num >= NUMERATOR_POWER_LIMIT_) {
                        num &= 0xff;
                        // 10^x without math.h
                        numerator = Math.pow(10, num);
                    } 
                    else {
                        numerator = num;
                    }
                    nex = true;
                }
                double denominator = 0;
                if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_DENOMINATOR_VALUE_)) {
                    denominator = PROPERTY_.getException(index, 
                             UCharacterProperty.EXC_DENOMINATOR_VALUE_);
                    // faster path not in c
                    if (numerator != 0) {
                        return numerator / denominator;
                    }
                    dex = true;
                } 
        
                if (nex) {
                    if (dex) {
                        return numerator / denominator;
                    } 
                    return numerator;
                }
                if (dex) {
                    return 1 / denominator;
                }
            }
        }
        return NO_NUMERIC_VALUE;
    }
  
    /**
     * Returns a value indicating a code point's Unicode category.
     * Up-to-date Unicode implementation of java.lang.Character.getType() 
     * except for the above mentioned code points that had their category 
     * changed.<br>
     * Return results are constants from the interface 
     * <a href=UCharacterCategory.html>UCharacterCategory</a><br>
     * <em>NOTE:</em> the UCharacterCategory values are <em>not</em> compatible with
     * those returned by java.lang.Character.getType.  UCharacterCategory values
     * match the ones used in ICU4C, while java.lang.Character type
     * values, though similar, skip the value 17.</p>
     * @param ch code point whose type is to be determined
     * @return category which is a value of UCharacterCategory
     * @stable ICU 2.1
     */
    public static int getType(int ch)
    {
        return getProperty(ch) & UCharacterProperty.TYPE_MASK;
    }
       
    /**
     * Determines if a code point has a defined meaning in the up-to-date 
     * Unicode standard.
     * E.g. supplementary code points though allocated space are not defined in 
     * Unicode yet.<br>
     * Up-to-date Unicode implementation of java.lang.Character.isDefined()
     * @param ch code point to be determined if it is defined in the most 
     *        current version of Unicode
     * @return true if this code point is defined in unicode
     * @stable ICU 2.1
     */
    public static boolean isDefined(int ch)
    {
        return getType(ch) != 0;
    }
                                    
    /**
     * Determines if a code point is a Java digit.
     * <br>This method observes the semantics of
     * <code>java.lang.Character.isDigit()</code>. It returns true for decimal 
     * digits only.
     * <br><em>Semantic Change:</em> In release 1.3.1 and prior, this treated 
     * numeric letters and other numbers as digits. 
     * This has been changed to conform to the java semantics.
     * @param ch code point to query
     * @return true if this code point is a digit 
     * @stable ICU 2.1
     */
    public static boolean isDigit(int ch)
    {
        return getType(ch) == UCharacterCategory.DECIMAL_DIGIT_NUMBER;
    }

    /**
     * Determines if the specified code point is an ISO control character.
     * A code point is considered to be an ISO control character if it is in 
     * the range &#92u0000 through &#92u001F or in the range &#92u007F through 
     * &#92u009F.<br>
     * Up-to-date Unicode implementation of java.lang.Character.isISOControl()
     * @param ch code point to determine if it is an ISO control character
     * @return true if code point is a ISO control character
     * @stable ICU 2.1
     */
    public static boolean isISOControl(int ch)
    {
        return ch >= 0 && ch <= APPLICATION_PROGRAM_COMMAND_ && 
            ((ch <= UNIT_SEPARATOR_) || (ch >= DELETE_));
    }
                                    
    /**
     * Determines if the specified code point is a letter.
     * Up-to-date Unicode implementation of java.lang.Character.isLetter()
     * @param ch code point to determine if it is a letter
     * @return true if code point is a letter
     * @stable ICU 2.1
     */
    public static boolean isLetter(int ch)
    {
        // if props == 0, it will just fall through and return false
        return ((1 << getType(ch)) 
        & ((1 << UCharacterCategory.UPPERCASE_LETTER) 
           | (1 << UCharacterCategory.LOWERCASE_LETTER)
           | (1 << UCharacterCategory.TITLECASE_LETTER)
           | (1 << UCharacterCategory.MODIFIER_LETTER)
           | (1 << UCharacterCategory.OTHER_LETTER))) != 0;
    }
                
    /**
     * Determines if the specified code point is a letter or digit.
     * Note this method, unlike java.lang.Character does not regard the ascii 
     * characters 'A' - 'Z' and 'a' - 'z' as digits.
     * @param ch code point to determine if it is a letter or a digit
     * @return true if code point is a letter or a digit
     * @stable ICU 2.1
     */
    public static boolean isLetterOrDigit(int ch)
    {
        return ((1 << getType(ch)) 
        & ((1 << UCharacterCategory.UPPERCASE_LETTER) 
           | (1 << UCharacterCategory.LOWERCASE_LETTER)
           | (1 << UCharacterCategory.TITLECASE_LETTER)
           | (1 << UCharacterCategory.MODIFIER_LETTER)
           | (1 << UCharacterCategory.OTHER_LETTER)
           | (1 << UCharacterCategory.DECIMAL_DIGIT_NUMBER))) != 0;
    }
        
    /**
     * Determines if the specified code point is a lowercase character.
     * UnicodeData only contains case mappings for code points where they are 
     * one-to-one mappings; it also omits information about context-sensitive 
     * case mappings.<br> For more information about Unicode case mapping 
     * please refer to the 
     * <a href=http://www.unicode.org/unicode/reports/tr21/>Technical report 
     * #21</a>.<br>
     * Up-to-date Unicode implementation of java.lang.Character.isLowerCase()
     * @param ch code point to determine if it is in lowercase
     * @return true if code point is a lowercase character
     * @stable ICU 2.1
     */
    public static boolean isLowerCase(int ch)
    {
        // if props == 0, it will just fall through and return false
        return getType(ch) == UCharacterCategory.LOWERCASE_LETTER;
    }
       
    /**
     * Determines if the specified code point is a white space character.
     * A code point is considered to be an whitespace character if and only
     * if it satisfies one of the following criteria:
     * <ul>
     * <li> It is a Unicode space separator (category "Zs"), but is not
     *      a no-break space (&#92u00A0 or &#92u202F or &#92uFEFF).
     * <li> It is a Unicode line separator (category "Zl").
     * <li> It is a Unicode paragraph separator (category "Zp").
     * <li> It is &#92u0009, HORIZONTAL TABULATION. 
     * <li> It is &#92u000A, LINE FEED. 
     * <li> It is &#92u000B, VERTICAL TABULATION. 
     * <li> It is &#92u000C, FORM FEED. 
     * <li> It is &#92u000D, CARRIAGE RETURN. 
     * <li> It is &#92u001C, FILE SEPARATOR. 
     * <li> It is &#92u001D, GROUP SEPARATOR. 
     * <li> It is &#92u001E, RECORD SEPARATOR. 
     * <li> It is &#92u001F, UNIT SEPARATOR.  
     * </ul>
     *
     * This API tries to synch to the semantics of the Java API,
     * java.lang.Character.isWhitespace(). 
     * @param ch code point to determine if it is a white space
     * @return true if the specified code point is a white space character
     * @stable ICU 2.1
     */
    public static boolean isWhitespace(int ch)
    {
        // exclude no-break spaces
        // if props == 0, it will just fall through and return false
        return ((1 << getType(ch)) & 
                ((1 << UCharacterCategory.SPACE_SEPARATOR)
                 | (1 << UCharacterCategory.LINE_SEPARATOR)
                 | (1 << UCharacterCategory.PARAGRAPH_SEPARATOR))) != 0 
        && (ch != NO_BREAK_SPACE_) && (ch != NARROW_NO_BREAK_SPACE_) 
        && (ch != ZERO_WIDTH_NO_BREAK_SPACE_)
        // TAB VT LF FF CR FS GS RS US NL are all control characters
        // that are white spaces.
        || (ch >= 0x9 && ch <= 0xd) || (ch >= 0x1c && ch <= 0x1f);
    }
       
    /**
     * Determines if the specified code point is a Unicode specified space 
     * character, i.e. if code point is in the category Zs, Zl and Zp.
     * Up-to-date Unicode implementation of java.lang.Character.isSpaceChar().
     * @param ch code point to determine if it is a space
     * @return true if the specified code point is a space character
     * @stable ICU 2.1
     */
    public static boolean isSpaceChar(int ch)
    {
        // if props == 0, it will just fall through and return false
        return ((1 << getType(ch)) & ((1 << UCharacterCategory.SPACE_SEPARATOR) 
                      | (1 << UCharacterCategory.LINE_SEPARATOR)
                      | (1 << UCharacterCategory.PARAGRAPH_SEPARATOR)))
        != 0;
    }
                                    
    /**
     * Determines if the specified code point is a titlecase character.
     * UnicodeData only contains case mappings for code points where they are 
     * one-to-one mappings; it also omits information about context-sensitive 
     * case mappings.<br>
     * For more information about Unicode case mapping please refer to the 
     * <a href=http://www.unicode.org/unicode/reports/tr21/>
     * Technical report #21</a>.<br>
     * Up-to-date Unicode implementation of java.lang.Character.isTitleCase().
     * @param ch code point to determine if it is in title case
     * @return true if the specified code point is a titlecase character
     * @stable ICU 2.1
     */
    public static boolean isTitleCase(int ch)
    {
        // if props == 0, it will just fall through and return false
        return getType(ch) == UCharacterCategory.TITLECASE_LETTER;
    }
       
    /**
     * Determines if the specified code point may be any part of a Unicode 
     * identifier other than the starting character.
     * A code point may be part of a Unicode identifier if and only if it is 
     * one of the following: 
     * <ul>
     * <li> Lu Uppercase letter
     * <li> Ll Lowercase letter
     * <li> Lt Titlecase letter
     * <li> Lm Modifier letter
     * <li> Lo Other letter
     * <li> Nl Letter number
     * <li> Pc Connecting punctuation character 
     * <li> Nd decimal number
     * <li> Mc Spacing combining mark 
     * <li> Mn Non-spacing mark 
     * <li> Cf formatting code
     * </ul>
     * Up-to-date Unicode implementation of 
     * java.lang.Character.isUnicodeIdentifierPart().<br>
     * See <a href=http://www.unicode.org/unicode/reports/tr8/>UTR #8</a>.
     * @param ch code point to determine if is can be part of a Unicode 
     *        identifier
     * @return true if code point is any character belonging a unicode 
     *         identifier suffix after the first character
     * @stable ICU 2.1
     */
    public static boolean isUnicodeIdentifierPart(int ch)
    {
        // if props == 0, it will just fall through and return false
        // cat == format
        return ((1 << getType(ch)) 
        & ((1 << UCharacterCategory.UPPERCASE_LETTER) 
           | (1 << UCharacterCategory.LOWERCASE_LETTER)
           | (1 << UCharacterCategory.TITLECASE_LETTER)
           | (1 << UCharacterCategory.MODIFIER_LETTER)
           | (1 << UCharacterCategory.OTHER_LETTER)
           | (1 << UCharacterCategory.LETTER_NUMBER) 
           | (1 << UCharacterCategory.CONNECTOR_PUNCTUATION)
           | (1 << UCharacterCategory.DECIMAL_DIGIT_NUMBER)
           | (1 << UCharacterCategory.COMBINING_SPACING_MARK)
           | (1 << UCharacterCategory.NON_SPACING_MARK))) != 0
        || isIdentifierIgnorable(ch);
    }
                       
    /**
     * Determines if the specified code point is permissible as the first 
     * character in a Unicode identifier.
     * A code point may start a Unicode identifier if it is of type either 
     * <ul> 
     * <li> Lu Uppercase letter
     * <li> Ll Lowercase letter
     * <li> Lt Titlecase letter
     * <li> Lm Modifier letter
     * <li> Lo Other letter
     * <li> Nl Letter number
     * </ul>
     * Up-to-date Unicode implementation of 
     * java.lang.Character.isUnicodeIdentifierStart().<br>
     * See <a href=http://www.unicode.org/unicode/reports/tr8/>UTR #8</a>.
     * @param ch code point to determine if it can start a Unicode identifier
     * @return true if code point is the first character belonging a unicode 
     *              identifier
     * @stable ICU 2.1
     */
    public static boolean isUnicodeIdentifierStart(int ch)
    {
        /*int cat = getType(ch);*/
        // if props == 0, it will just fall through and return false
        return ((1 << getType(ch)) 
        & ((1 << UCharacterCategory.UPPERCASE_LETTER) 
           | (1 << UCharacterCategory.LOWERCASE_LETTER)
           | (1 << UCharacterCategory.TITLECASE_LETTER)
           | (1 << UCharacterCategory.MODIFIER_LETTER)
           | (1 << UCharacterCategory.OTHER_LETTER)
           | (1 << UCharacterCategory.LETTER_NUMBER))) != 0;
    }

    /**
     * Determines if the specified code point should be regarded as an 
     * ignorable character in a Unicode identifier.
     * A character is ignorable in the Unicode standard if it is of the type 
     * Cf, Formatting code.<br>
     * Up-to-date Unicode implementation of 
     * java.lang.Character.isIdentifierIgnorable().<br>
     * See <a href=http://www.unicode.org/unicode/reports/tr8/>UTR #8</a>.
     * @param ch code point to be determined if it can be ignored in a Unicode 
     *        identifier.
     * @return true if the code point is ignorable
     * @stable ICU 2.1
     */
    public static boolean isIdentifierIgnorable(int ch)
    {
        // see java.lang.Character.isIdentifierIgnorable() on range of 
        // ignorable characters.
        if (ch <= 0x9f) {
        return isISOControl(ch) 
        && !((ch >= 0x9 && ch <= 0xd) 
             || (ch >= 0x1c && ch <= 0x1f));
        } 
        return getType(ch) == UCharacterCategory.FORMAT;
    }
                      
    /**
     * Determines if the specified code point is an uppercase character.
     * UnicodeData only contains case mappings for code point where they are 
     * one-to-one mappings; it also omits information about context-sensitive 
     * case mappings.<br> 
     * For language specific case conversion behavior, use 
     * toUpperCase(locale, str). <br>
     * For example, the case conversion for dot-less i and dotted I in Turkish,
     * or for final sigma in Greek.
     * For more information about Unicode case mapping please refer to the 
     * <a href=http://www.unicode.org/unicode/reports/tr21/>
     * Technical report #21</a>.<br>
     * Up-to-date Unicode implementation of java.lang.Character.isUpperCase().
     * @param ch code point to determine if it is in uppercase
     * @return true if the code point is an uppercase character
     * @stable ICU 2.1
     */
    public static boolean isUpperCase(int ch)
    {
        // if props == 0, it will just fall through and return false
        return getType(ch) == UCharacterCategory.UPPERCASE_LETTER;
    }
                       
    /**
     * The given code point is mapped to its lowercase equivalent; if the code 
     * point has no lowercase equivalent, the code point itself is returned.
     * UnicodeData only contains case mappings for code point where they are 
     * one-to-one mappings; it also omits information about context-sensitive 
     * case mappings.<br> 
     * For language specific case conversion behavior, use 
     * toLowerCase(locale, str). <br>
     * For example, the case conversion for dot-less i and dotted I in Turkish,
     * or for final sigma in Greek.
     * For more information about Unicode case mapping please refer to the 
     * <a href=http://www.unicode.org/unicode/reports/tr21/>
     * Technical report #21</a>.<br>
     * Up-to-date Unicode implementation of java.lang.Character.toLowerCase()
     * @param ch code point whose lowercase equivalent is to be retrieved
     * @return the lowercase equivalent code point
     * @stable ICU 2.1
     */
    public static int toLowerCase(int ch)
    {
        // when ch is out of bounds getProperty == 0   
        int props = PROPERTY_.getProperty(ch);
        // if props == 0, it will just fall through and return itself
        if(isNotExceptionIndicator(props)) {
            int cat = UCharacterProperty.TYPE_MASK & props;
            if (cat == UCharacterCategory.UPPERCASE_LETTER || 
                cat == UCharacterCategory.TITLECASE_LETTER) {
                return ch + UCharacterProperty.getSignedValue(props);
            }
        } 
        else 
        {
        int index = UCharacterProperty.getExceptionIndex(props);
        if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_LOWERCASE_)) {
            return PROPERTY_.getException(index, 
                          UCharacterProperty.EXC_LOWERCASE_); 
        }
        }
        return ch;
    }

    /**
     * Converts argument code point and returns a String object representing 
     * the code point's value in UTF16 format.
     * The result is a string whose length is 1 for non-supplementary code 
     * points, 2 otherwise.<br>
     * com.ibm.ibm.icu.UTF16 can be used to parse Strings generated by this 
     * function.<br>
     * Up-to-date Unicode implementation of java.lang.Character.toString()
     * @param ch code point
     * @return string representation of the code point, null if code point is not
     *         defined in unicode
     * @stable ICU 2.1
     */
    public static String toString(int ch)
    {
        if (ch < MIN_VALUE || ch > MAX_VALUE) {
            return null;
        }
        
        if (ch < SUPPLEMENTARY_MIN_VALUE) {
            return String.valueOf((char)ch);
        }
        
        StringBuffer result = new StringBuffer();
        result.append(UTF16.getLeadSurrogate(ch));
        result.append(UTF16.getTrailSurrogate(ch));
        return result.toString();
    }
                                    
    /**
     * Converts the code point argument to titlecase.
     * UnicodeData only contains case mappings for code points where they are 
     * one-to-one mappings; it also omits information about context-sensitive 
     * case mappings.<br> 
     * There are only four Unicode characters that are truly titlecase forms
     * that are distinct from uppercase forms.
     * For more information about Unicode case mapping please refer
     * to the <a href=http://www.unicode.org/unicode/reports/tr21/>
     * Technical report #21</a>.<br>
     * If no titlecase is available, the uppercase is returned. If no uppercase 
     * is available, the code point itself is returned.<br>
     * Up-to-date Unicode implementation of java.lang.Character.toTitleCase()
     * @param ch code point  whose title case is to be retrieved
     * @return titlecase code point
     * @stable ICU 2.1
     */
    public static int toTitleCase(int ch)
    {
        // when ch is out of bounds getProperty == 0
        int props = PROPERTY_.getProperty(ch);
        // if props == 0, it will just fall through and return itself
        if (isNotExceptionIndicator(props)) {
            if ((UCharacterProperty.TYPE_MASK & props) == 
                UCharacterCategory.LOWERCASE_LETTER) {
                // here, titlecase is same as uppercase
                return ch - UCharacterProperty.getSignedValue(props);
            }
        } 
        else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_TITLECASE_)) {
                return PROPERTY_.getException(index,
                          UCharacterProperty.EXC_TITLECASE_);
            }
            else {
                // here, titlecase is same as uppercase
                if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_UPPERCASE_)) {
                    return PROPERTY_.getException(index, 
                          UCharacterProperty.EXC_UPPERCASE_); 
                }
            }
        }
        return ch; // no mapping - return c itself
    }
       
    /**
     * Converts the character argument to uppercase.
     * UnicodeData only contains case mappings for characters where they are 
     * one-to-one mappings; it also omits information about context-sensitive 
     * case mappings.<br> 
     * For more information about Unicode case mapping please refer
     * to the <a href=http://www.unicode.org/unicode/reports/tr21/>
     * Technical report #21</a>.<br>
     * If no uppercase is available, the character itself is returned.<br>
     * Up-to-date Unicode implementation of java.lang.Character.toUpperCase()
     * @param ch code point whose uppercase is to be retrieved
     * @return uppercase code point
     * @stable ICU 2.1
     */
    public static int toUpperCase(int ch)
    {
        // when ch is out of bounds getProperty == 0
        int props = PROPERTY_.getProperty(ch);
        // if props == 0, it will just fall through and return itself
        if (isNotExceptionIndicator(props)) {
            if ((UCharacterProperty.TYPE_MASK & props) == 
                UCharacterCategory.LOWERCASE_LETTER) {
                // here, titlecase is same as uppercase */
                return ch - UCharacterProperty.getSignedValue(props);
            }
        }
        else 
        {
        int index = UCharacterProperty.getExceptionIndex(props);
        if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_UPPERCASE_)) {
            return PROPERTY_.getException(index, 
                          UCharacterProperty.EXC_UPPERCASE_); 
        }
        }
        return ch; // no mapping - return c itself
    }
       
    // extra methods not in java.lang.Character --------------------------
       
    /**
     * Determines if the code point is a supplementary character.
     * A code point is a supplementary character if and only if it is greater 
     * than <a href=#SUPPLEMENTARY_MIN_VALUE>SUPPLEMENTARY_MIN_VALUE</a>
     * @param ch code point to be determined if it is in the supplementary 
     *        plane
     * @return true if code point is a supplementary character
     * @stable ICU 2.1
     */
    public static boolean isSupplementary(int ch)
    {
        return ch >= UCharacter.SUPPLEMENTARY_MIN_VALUE && 
            ch <= UCharacter.MAX_VALUE;
    }
      
    /**
     * Determines if the code point is in the BMP plane.
     * @param ch code point to be determined if it is not a supplementary 
     *        character
     * @return true if code point is not a supplementary character
     * @stable ICU 2.1
     */
    public static boolean isBMP(int ch) 
    {
        return (ch >= 0 && ch <= LAST_CHAR_MASK_);
    }

    /**
     * Determines whether the specified code point is a printable character 
     * according to the Unicode standard.
     * @param ch code point to be determined if it is printable
     * @return true if the code point is a printable character
     * @stable ICU 2.1
     */
    public static boolean isPrintable(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return (cat != UCharacterCategory.UNASSIGNED && 
        cat != UCharacterCategory.CONTROL && 
        cat != UCharacterCategory.FORMAT &&
        cat != UCharacterCategory.PRIVATE_USE &&
        cat != UCharacterCategory.SURROGATE &&
        cat != UCharacterCategory.GENERAL_OTHER_TYPES);
    }

    /**
     * Determines whether the specified code point is of base form.
     * A code point of base form does not graphically combine with preceding 
     * characters, and is neither a control nor a format character.
     * @param ch code point to be determined if it is of base form
     * @return true if the code point is of base form
     * @stable ICU 2.1
     */
    public static boolean isBaseForm(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.DECIMAL_DIGIT_NUMBER || 
            cat == UCharacterCategory.OTHER_NUMBER || 
            cat == UCharacterCategory.LETTER_NUMBER || 
            cat == UCharacterCategory.UPPERCASE_LETTER || 
            cat == UCharacterCategory.LOWERCASE_LETTER || 
            cat == UCharacterCategory.TITLECASE_LETTER ||
            cat == UCharacterCategory.MODIFIER_LETTER || 
            cat == UCharacterCategory.OTHER_LETTER || 
            cat == UCharacterCategory.NON_SPACING_MARK || 
            cat == UCharacterCategory.ENCLOSING_MARK ||
            cat == UCharacterCategory.COMBINING_SPACING_MARK;
    }

    /**
     * Returns the Bidirection property of a code point.
     * For example, 0x0041 (letter A) has the LEFT_TO_RIGHT directional 
     * property.<br>
     * Result returned belongs to the interface 
     * <a href=UCharacterDirection.html>UCharacterDirection</a>
     * @param ch the code point to be determined its direction
     * @return direction constant from UCharacterDirection.
     * @stable ICU 2.1
     */
    public static int getDirection(int ch)
    {
        // when ch is out of bounds getProperty == 0
        return (getProperty(ch) >> BIDI_SHIFT_) & BIDI_MASK_AFTER_SHIFT_;
    }

    /**
     * Determines whether the code point has the "mirrored" property.
     * This property is set for characters that are commonly used in
     * Right-To-Left contexts and need to be displayed with a "mirrored"
     * glyph.
     * @param ch code point whose mirror is to be determined
     * @return true if the code point has the "mirrored" property
     * @stable ICU 2.1
     */
    public static boolean isMirrored(int ch)
    {
        // when ch is out of bounds getProperty == 0
        return (PROPERTY_.getProperty(ch) & UCharacterProperty.MIRROR_MASK) 
        != 0;
    }

    /**
     * Maps the specified code point to a "mirror-image" code point.
     * For code points with the "mirrored" property, implementations sometimes 
     * need a "poor man's" mapping to another code point such that the default 
     * glyph may serve as the mirror-image of the default glyph of the 
     * specified code point.<br> 
     * This is useful for text conversion to and from codepages with visual 
     * order, and for displays without glyph selection capabilities.
     * @param ch code point whose mirror is to be retrieved
     * @return another code point that may serve as a mirror-image substitute, 
     *         or ch itself if there is no such mapping or ch does not have the 
     *         "mirrored" property
     * @stable ICU 2.1
     */
    public static int getMirror(int ch)
    {
        // when ch is out of bounds getProperty == 0
        int props = PROPERTY_.getProperty(ch);
        // mirrored - the value is a mirror offset
        // if props == 0, it will just fall through and return false
        if ((props & UCharacterProperty.MIRROR_MASK) != 0) {
            if(isNotExceptionIndicator(props)) {
                return ch + UCharacterProperty.getSignedValue(props);
            }
            else 
        {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY_.hasExceptionValue(index, 
                            UCharacterProperty.EXC_MIRROR_MAPPING_)) 
            return PROPERTY_.getException(index, 
                              UCharacterProperty.EXC_MIRROR_MAPPING_);   
        }
        }
        return ch;
    }
      
    /**
     * Gets the combining class of the argument codepoint
     * @param ch code point whose combining is to be retrieved
     * @return the combining class of the codepoint
     * @stable ICU 2.1
     */
    public static int getCombiningClass(int ch)
    {
        if (ch < MIN_VALUE || ch > MAX_VALUE) {
        throw new IllegalArgumentException("Codepoint out of bounds");
        }
        return NormalizerImpl.getCombiningClass(ch);
    }
      
    /**
     * A code point is illegal if and only if
     * <ul>
     * <li> Out of bounds, less than 0 or greater than UCharacter.MAX_VALUE
     * <li> A surrogate value, 0xD800 to 0xDFFF
     * <li> Not-a-character, having the form 0x xxFFFF or 0x xxFFFE
     * </ul>
     * Note: legal does not mean that it is assigned in this version of Unicode.
     * @param ch code point to determine if it is a legal code point by itself
     * @return true if and only if legal. 
     * @stable ICU 2.1
     */
    public static boolean isLegal(int ch) 
    {
        if (ch < MIN_VALUE) {
            return false;
        }
        if (ch < UTF16.SURROGATE_MIN_VALUE) {
            return true;
        }
        if (ch <= UTF16.SURROGATE_MAX_VALUE) {
            return false;
        }
        if (UCharacterUtility.isNonCharacter(ch)) {
            return false;
        }
        return (ch <= MAX_VALUE);
    }
      
    /**
     * A string is legal iff all its code points are legal.
     * A code point is illegal if and only if
     * <ul>
     * <li> Out of bounds, less than 0 or greater than UCharacter.MAX_VALUE
     * <li> A surrogate value, 0xD800 to 0xDFFF
     * <li> Not-a-character, having the form 0x xxFFFF or 0x xxFFFE
     * </ul>
     * Note: legal does not mean that it is assigned in this version of Unicode.
     * @param str containing code points to examin
     * @return true if and only if legal. 
     * @stable ICU 2.1
     */
    public static boolean isLegal(String str) 
    {
        int size = str.length();
        int codepoint;
        for (int i = 0; i < size; i ++)
        {
        codepoint = UTF16.charAt(str, i);
        if (!isLegal(codepoint)) {
            return false;
        }
        if (isSupplementary(codepoint)) {
            i ++;
        }
        }
        return true;
    }

    /**
     * Gets the version of Unicode data used. 
     * @return the unicode version number used
     * @stable ICU 2.1
     */
    public static VersionInfo getUnicodeVersion()
    {
        return PROPERTY_.m_unicodeVersion_;
    }
      
    /**
     * Retrieve the most current Unicode name of the argument code point, or 
     * null if the character is unassigned or outside the range 
     * UCharacter.MIN_VALUE and UCharacter.MAX_VALUE or does not have a name.
     * <br>
     * Note calling any methods related to code point names, e.g. get*Name*() 
     * incurs a one-time initialisation cost to construct the name tables.
     * @param ch the code point for which to get the name
     * @return most current Unicode name
     * @stable ICU 2.1
     */
    public static String getName(int ch)
    {
        if(NAME_==null){
            throw new RuntimeException("Could not load unames.icu");
        }
        return NAME_.getName(ch, UCharacterNameChoice.UNICODE_CHAR_NAME);
    }
    
    /**
     * Gets the names for each of the characters in a string
     * @param s string to format
     * @param separator string to go between names
     * @return string of names
     * @internal
     */
    public static String getName(String s, String separator) {
        if (s.length() == 1) { // handle common case
            return getName(s.charAt(0));
        }
        int cp;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s,i);
            if (i != 0) sb.append(separator);
            sb.append(UCharacter.getName(cp));
        }
        return sb.toString();
    }
      
    /**
     * Retrieve the earlier version 1.0 Unicode name of the argument code 
     * point, or null if the character is unassigned or outside the range 
     * UCharacter.MIN_VALUE and UCharacter.MAX_VALUE or does not have a name.
     * <br>
     * Note calling any methods related to code point names, e.g. get*Name*() 
     * incurs a one-time initialisation cost to construct the name tables.
     * @param ch the code point for which to get the name
     * @return version 1.0 Unicode name
     * @stable ICU 2.1
     */
    public static String getName1_0(int ch)
    {
        if(NAME_==null){
            throw new RuntimeException("Could not load unames.icu");
        }
        return NAME_.getName(ch, 
                             UCharacterNameChoice.UNICODE_10_CHAR_NAME);
    }
    
    /**
     * <p>Retrieves a name for a valid codepoint. Unlike, getName(int) and
     * getName1_0(int), this method will return a name even for codepoints that
     * are not assigned a name in UnicodeData.txt.
     * </p>
     * The names are returned in the following order.
     * <ul>
     * <li> Most current Unicode name if there is any
     * <li> Unicode 1.0 name if there is any
     * <li> Extended name in the form of 
     *      "<codepoint_type-codepoint_hex_digits>". E.g. <noncharacter-fffe>
     * </ul>
     * Note calling any methods related to code point names, e.g. get*Name*() 
     * incurs a one-time initialisation cost to construct the name tables.
     * @param ch the code point for which to get the name
     * @return a name for the argument codepoint
     * @stable ICU 2.6
     */
    public static String getExtendedName(int ch) 
    {
        if(NAME_==null){
            throw new RuntimeException("Could not load unames.icu");
        }
        return NAME_.getName(ch, UCharacterNameChoice.EXTENDED_CHAR_NAME);
    }
    
    /**
     * Get the ISO 10646 comment for a character.
     * The ISO 10646 comment is an informative field in the Unicode Character
     * Database (UnicodeData.txt field 11) and is from the ISO 10646 names list.
     * @param ch The code point for which to get the ISO comment.
     *           It must be <code>0<=c<=0x10ffff</code>.
     * @return The ISO comment, or null if there is no comment for this 
     *         character.
     * @stable ICU 2.4
     */
    public static String getISOComment(int ch)
    {
        if (ch < UCharacter.MIN_VALUE || ch > UCharacter.MAX_VALUE) {
            return null;
        }
        if(NAME_==null){
            throw new RuntimeException("Could not load unames.icu");
        }    
        String result = NAME_.getGroupName(ch, 
                                           UCharacterNameChoice.ISO_COMMENT_);
        return result;
    }
      
    /**
     * <p>Find a Unicode code point by its most current Unicode name and 
     * return its code point value. All Unicode names are in uppercase.</p>
     * Note calling any methods related to code point names, e.g. get*Name*() 
     * incurs a one-time initialisation cost to construct the name tables.
     * @param name most current Unicode character name whose code point is to 
     *        be returned
     * @return code point or -1 if name is not found
     * @stable ICU 2.1
     */
    public static int getCharFromName(String name)
    {     
        if(NAME_==null){
            throw new RuntimeException("Could not load unames.icu");
        }
        return NAME_.getCharFromName(
                     UCharacterNameChoice.UNICODE_CHAR_NAME, name);
    }
      
    /**
     * <p>Find a Unicode character by its version 1.0 Unicode name and return 
     * its code point value. All Unicode names are in uppercase.</p>
     * Note calling any methods related to code point names, e.g. get*Name*() 
     * incurs a one-time initialisation cost to construct the name tables.
     * @param name Unicode 1.0 code point name whose code point is to 
     *             returned
     * @return code point or -1 if name is not found
     * @stable ICU 2.1
     */
    public static int getCharFromName1_0(String name)
    {
        if(NAME_==null){
            throw new RuntimeException("Could not load unames.icu");
        }
        return NAME_.getCharFromName(
                     UCharacterNameChoice.UNICODE_10_CHAR_NAME, name);
    }
    
    /**
     * <p>Find a Unicode character by either its name and return its code 
     * point value. All Unicode names are in uppercase. 
     * Extended names are all lowercase except for numbers and are contained
     * within angle brackets.</p>
     * The names are searched in the following order
     * <ul>
     * <li> Most current Unicode name if there is any
     * <li> Unicode 1.0 name if there is any
     * <li> Extended name in the form of 
     *      "<codepoint_type-codepoint_hex_digits>". E.g. <noncharacter-FFFE>
     * </ul>
     * Note calling any methods related to code point names, e.g. get*Name*() 
     * incurs a one-time initialisation cost to construct the name tables.
     * @param name codepoint name
     * @return code point associated with the name or -1 if the name is not
     *         found.
     * @stable ICU 2.6
     */
    public static int getCharFromExtendedName(String name)
    {
        if(NAME_==null){
            throw new RuntimeException("Could not load unames.icu");
        }
        return NAME_.getCharFromName(
                     UCharacterNameChoice.EXTENDED_CHAR_NAME, name);
    }

    /**
     * Return the Unicode name for a given property, as given in the
     * Unicode database file PropertyAliases.txt.  Most properties
     * have more than one name.  The nameChoice determines which one
     * is returned.
     *
     * In addition, this function maps the property
     * UProperty.GENERAL_CATEGORY_MASK to the synthetic names "gcm" /
     * "General_Category_Mask".  These names are not in
     * PropertyAliases.txt.
     * 
     * @param property UProperty selector.
     *
     * @param nameChoice UProperty.NameChoice selector for which name
     * to get.  All properties have a long name.  Most have a short
     * name, but some do not.  Unicode allows for additional names; if
     * present these will be returned by UProperty.NameChoice.LONG + i,
     * where i=1, 2,...
     *
     * @return a name, or null if Unicode explicitly defines no name
     * ("n/a") for a given property/nameChoice.  If a given nameChoice
     * throws an exception, then all larger values of nameChoice will
     * throw an exception.  If null is returned for a given
     * nameChoice, then other nameChoice values may return non-null
     * results.
     *
     * @exception IllegalArgumentException thrown if property or
     * nameChoice are invalid.
     *
     * @see UProperty
     * @see UProperty.NameChoice
     * @stable ICU 2.4
     */
    public static String getPropertyName(int property,
                                         int nameChoice) {
        return PNAMES_.getPropertyName(property, nameChoice);
    }

    /**
     * Return the UProperty selector for a given property name, as
     * specified in the Unicode database file PropertyAliases.txt.
     * Short, long, and any other variants are recognized.
     *
     * In addition, this function maps the synthetic names "gcm" /
     * "General_Category_Mask" to the property
     * UProperty.GENERAL_CATEGORY_MASK.  These names are not in
     * PropertyAliases.txt.
     *
     * @param propertyAlias the property name to be matched.  The name
     * is compared using "loose matching" as described in
     * PropertyAliases.txt.
     *
     * @return a UProperty enum.
     *
     * @exception IllegalArgumentException thrown if propertyAlias
     * is not recognized.
     *
     * @see UProperty
     * @stable ICU 2.4
     */
    public static int getPropertyEnum(String propertyAlias) {
        return PNAMES_.getPropertyEnum(propertyAlias);
    }

    /**
     * Return the Unicode name for a given property value, as given in
     * the Unicode database file PropertyValueAliases.txt.  Most
     * values have more than one name.  The nameChoice determines
     * which one is returned.
     *
     * Note: Some of the names in PropertyValueAliases.txt can only be
     * retrieved using UProperty.GENERAL_CATEGORY_MASK, not
     * UProperty.GENERAL_CATEGORY.  These include: "C" / "Other", "L" /
     * "Letter", "LC" / "Cased_Letter", "M" / "Mark", "N" / "Number", "P"
     * / "Punctuation", "S" / "Symbol", and "Z" / "Separator".
     *
     * @param property UProperty selector constant.
     * UProperty.INT_START &lt;= property &lt; UProperty.INT_LIMIT or
     * UProperty.BINARY_START &lt;= property &lt; UProperty.BINARY_LIMIT or
     * UProperty.MASK_START &lt; = property &lt; UProperty.MASK_LIMIT.
     * If out of range, null is returned.
     *
     * @param value selector for a value for the given property.  In
     * general, valid values range from 0 up to some maximum.  There
     * are a few exceptions: (1.) UProperty.BLOCK values begin at the
     * non-zero value BASIC_LATIN.getID().  (2.)
     * UProperty.CANONICAL_COMBINING_CLASS values are not contiguous
     * and range from 0..240.  (3.)  UProperty.GENERAL_CATEGORY_MASK values
     * are mask values produced by left-shifting 1 by
     * UCharacter.getType().  This allows grouped categories such as
     * [:L:] to be represented.  Mask values are non-contiguous.
     *
     * @param nameChoice UProperty.NameChoice selector for which name
     * to get.  All values have a long name.  Most have a short name,
     * but some do not.  Unicode allows for additional names; if
     * present these will be returned by UProperty.NameChoice.LONG + i,
     * where i=1, 2,...
     *
     * @return a name, or null if Unicode explicitly defines no name
     * ("n/a") for a given property/value/nameChoice.  If a given
     * nameChoice throws an exception, then all larger values of
     * nameChoice will throw an exception.  If null is returned for a
     * given nameChoice, then other nameChoice values may return
     * non-null results.
     *
     * @exception IllegalArgumentException thrown if property, value,
     * or nameChoice are invalid.
     *
     * @see UProperty
     * @see UProperty.NameChoice
     * @stable ICU 2.4
     */
    public static String getPropertyValueName(int property,
                                              int value,
                                              int nameChoice) 
    {
        if (property == UProperty.CANONICAL_COMBINING_CLASS 
            && value >= UCharacter.getIntPropertyMinValue(
                              UProperty.CANONICAL_COMBINING_CLASS)
            && value <= UCharacter.getIntPropertyMaxValue(
                              UProperty.CANONICAL_COMBINING_CLASS)
            && nameChoice >= 0 && nameChoice < UProperty.NameChoice.COUNT) {
            // this is hard coded for the valid cc
            // because PropertyValueAliases.txt does not contain all of them
            try {
                return PNAMES_.getPropertyValueName(property, value, 
                                                    nameChoice);
            }
            catch (IllegalArgumentException e) {
                return null;
            }
        }
        return PNAMES_.getPropertyValueName(property, value, nameChoice);
    }

    /**
     * Return the property value integer for a given value name, as
     * specified in the Unicode database file PropertyValueAliases.txt.
     * Short, long, and any other variants are recognized.
     *
     * Note: Some of the names in PropertyValueAliases.txt will only be
     * recognized with UProperty.GENERAL_CATEGORY_MASK, not
     * UProperty.GENERAL_CATEGORY.  These include: "C" / "Other", "L" /
     * "Letter", "LC" / "Cased_Letter", "M" / "Mark", "N" / "Number", "P"
     * / "Punctuation", "S" / "Symbol", and "Z" / "Separator".
     *
     * @param property UProperty selector constant.
     * UProperty.INT_START &lt;= property &lt; UProperty.INT_LIMIT or
     * UProperty.BINARY_START &lt;= property &lt; UProperty.BINARY_LIMIT or
     * UProperty.MASK_START &lt; = property &lt; UProperty.MASK_LIMIT.
     * Only these properties can be enumerated.
     *
     * @param valueAlias the value name to be matched.  The name is
     * compared using "loose matching" as described in
     * PropertyValueAliases.txt.
     *
     * @return a value integer.  Note: UProperty.GENERAL_CATEGORY
     * values are mask values produced by left-shifting 1 by
     * UCharacter.getType().  This allows grouped categories such as
     * [:L:] to be represented.
     *
     * @see UProperty
     * @throws IllegalArgumentException if property is not a valid UProperty
     *         selector
     * @stable ICU 2.4
     */
    public static int getPropertyValueEnum(int property,
                                           String valueAlias) {
        return PNAMES_.getPropertyValueEnum(property, valueAlias);
    }
      
    /**
     * Returns a code point corresponding to the two UTF16 characters.
     * @param lead the lead char
     * @param trail the trail char
     * @return code point if surrogate characters are valid.
     * @exception IllegalArgumentException thrown when argument characters do
     *            not form a valid codepoint
     * @stable ICU 2.1
     */
    public static int getCodePoint(char lead, char trail) 
    {
        if (lead >= UTF16.LEAD_SURROGATE_MIN_VALUE && 
        lead <= UTF16.LEAD_SURROGATE_MAX_VALUE &&
            trail >= UTF16.TRAIL_SURROGATE_MIN_VALUE && 
        trail <= UTF16.TRAIL_SURROGATE_MAX_VALUE) {
            return UCharacterProperty.getRawSupplementary(lead, trail);
        }
        throw new IllegalArgumentException("Illegal surrogate characters");
    }
      
    /**
     * Returns the code point corresponding to the UTF16 character.
     * @param char16 the UTF16 character
     * @return code point if argument is a valid character.
     * @exception IllegalArgumentException thrown when char16 is not a valid
     *            codepoint
     * @stable ICU 2.1
     */
    public static int getCodePoint(char char16) 
    {
        if (UCharacter.isLegal(char16)) {
            return char16;
        }
        throw new IllegalArgumentException("Illegal codepoint");
    }
      
    /**
     * Gets uppercase version of the argument string. 
     * Casing is dependent on the default locale and context-sensitive.
     * @param str source string to be performed on
     * @return uppercase version of the argument string
     * @stable ICU 2.1
     */
    public static String toUpperCase(String str)
    {
        return toUpperCase(ULocale.getDefault(), str);
    }
      
    /**
     * Gets lowercase version of the argument string. 
     * Casing is dependent on the default locale and context-sensitive
     * @param str source string to be performed on
     * @return lowercase version of the argument string
     * @stable ICU 2.1
     */
    public static String toLowerCase(String str)
    {
        return toLowerCase(ULocale.getDefault(), str);
    }
    
    /**
     * <p>Gets the titlecase version of the argument string.</p>
     * <p>Position for titlecasing is determined by the argument break 
     * iterator, hence the user can customized his break iterator for 
     * a specialized titlecasing. In this case only the forward iteration 
     * needs to be implemented.
     * If the break iterator passed in is null, the default Unicode algorithm
     * will be used to determine the titlecase positions.
     * </p>
     * <p>Only positions returned by the break iterator will be title cased,
     * character in between the positions will all be in lower case.</p>
     * <p>Casing is dependent on the default locale and context-sensitive</p>
     * @param str source string to be performed on
     * @param breakiter break iterator to determine the positions in which
     *        the character should be title cased.
     * @return lowercase version of the argument string
     * @stable ICU 2.6
     */
    public static String toTitleCase(String str, BreakIterator breakiter)
    {
        return toTitleCase(ULocale.getDefault(), str, breakiter);
    }
      
    /**
     * Gets uppercase version of the argument string. 
     * Casing is dependent on the argument locale and context-sensitive.
     * @param locale which string is to be converted in
     * @param str source string to be performed on
     * @return uppercase version of the argument string
     * @stable ICU 2.1
     */
    public static String toUpperCase(Locale locale, String str)
    {
        return toUpperCase(ULocale.forLocale(locale), str);
    }

    /**
     * Gets uppercase version of the argument string. 
     * Casing is dependent on the argument locale and context-sensitive.
     * @param locale which string is to be converted in
     * @param str source string to be performed on
     * @return uppercase version of the argument string
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String toUpperCase(ULocale locale, String str)
    {
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        return PROPERTY_.toUpperCase(locale, str, 0, str.length());
    }
      
    /**
     * Gets lowercase version of the argument string. 
     * Casing is dependent on the argument locale and context-sensitive
     * @param locale which string is to be converted in
     * @param str source string to be performed on
     * @return lowercase version of the argument string
     * @stable ICU 2.1
     */
    public static String toLowerCase(Locale locale, String str)
    {
        return toLowerCase(ULocale.forLocale(locale), str);
    }

    /**
     * Gets lowercase version of the argument string. 
     * Casing is dependent on the argument locale and context-sensitive
     * @param locale which string is to be converted in
     * @param str source string to be performed on
     * @return lowercase version of the argument string
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String toLowerCase(ULocale locale, String str)
    {
        int length = str.length();
        StringBuffer result = new StringBuffer(length);
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        PROPERTY_.toLowerCase(locale, str, 0, length, result);
        return result.toString();
    }
    
    /**
     * <p>Gets the titlecase version of the argument string.</p>
     * <p>Position for titlecasing is determined by the argument break 
     * iterator, hence the user can customized his break iterator for 
     * a specialized titlecasing. In this case only the forward iteration 
     * needs to be implemented.
     * If the break iterator passed in is null, the default Unicode algorithm
     * will be used to determine the titlecase positions.
     * </p>
     * <p>Only positions returned by the break iterator will be title cased,
     * character in between the positions will all be in lower case.</p>
     * <p>Casing is dependent on the argument locale and context-sensitive</p>
     * @param locale which string is to be converted in
     * @param str source string to be performed on
     * @param breakiter break iterator to determine the positions in which
     *        the character should be title cased.
     * @return lowercase version of the argument string
     * @stable ICU 2.6
     */
    public static String toTitleCase(Locale locale, String str, 
                                     BreakIterator breakiter)
    {
        return toTitleCase(ULocale.forLocale(locale), str, breakiter);
    }

    /**
     * <p>Gets the titlecase version of the argument string.</p>
     * <p>Position for titlecasing is determined by the argument break 
     * iterator, hence the user can customized his break iterator for 
     * a specialized titlecasing. In this case only the forward iteration 
     * needs to be implemented.
     * If the break iterator passed in is null, the default Unicode algorithm
     * will be used to determine the titlecase positions.
     * </p>
     * <p>Only positions returned by the break iterator will be title cased,
     * character in between the positions will all be in lower case.</p>
     * <p>Casing is dependent on the argument locale and context-sensitive</p>
     * @param locale which string is to be converted in
     * @param str source string to be performed on
     * @param breakiter break iterator to determine the positions in which
     *        the character should be title cased.
     * @return lowercase version of the argument string
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String toTitleCase(ULocale locale, String str, 
                                     BreakIterator breakiter)
    {
        if (breakiter == null) {
            if (locale == null) {
                locale = ULocale.getDefault();
            }
            breakiter = BreakIterator.getWordInstance(locale);
        }
        return PROPERTY_.toTitleCase(locale, str, breakiter);
    }
    
    /**
     * The given character is mapped to its case folding equivalent according 
     * to UnicodeData.txt and CaseFolding.txt; if the character has no case 
     * folding equivalent, the character itself is returned.
     * Only "simple", single-code point case folding mappings are used.
     * For "full", multiple-code point mappings use the API 
     * foldCase(String str, boolean defaultmapping).
     * @param ch             the character to be converted
     * @param defaultmapping Indicates if all mappings defined in 
     *                       CaseFolding.txt is to be used, otherwise the 
     *                       mappings for dotted I  and dotless i marked with 
     *                       'I' in CaseFolding.txt will be skipped.
     * @return               the case folding equivalent of the character, if 
     *                       any; otherwise the character itself.
     * @see                  #foldCase(String, boolean)
     * @stable ICU 2.1
     */
    public static int foldCase(int ch, boolean defaultmapping)
    {
        // Some special cases are hardcoded because their conditions cannot be
        // parsed and processed from CaseFolding.txt.
        // Unicode 3.2 CaseFolding.txt specifies for its status field:
        // # C: common case folding, common mappings shared by both simple and 
        // full mappings.
        // # F: full case folding, mappings that cause strings to grow in 
        // length. Multiple characters are separated by spaces.
        // # S: simple case folding, mappings to single characters where 
        // different from F.
        // # T: special case for uppercase I and dotted uppercase I
        // #    - For non-Turkic languages, this mapping is normally not used.
        // #    - For Turkic languages (tr, az), this mapping can be used 
        // instead of the normal mapping for these characters.
        // # Usage:
        // #  A. To do a simple case folding, use the mappings with status 
        // C + S.
        // #  B. To do a full case folding, use the mappings with status C + F.
        // #    The mappings with status T can be used or omitted depending on 
        // the desired case-folding behavior. 
        // (The default option is to exclude them.)
        // Unicode 3.2 has 'T' mappings as follows:
        // 0049; T; 0131; # LATIN CAPITAL LETTER I
        // 0130; T; 0069; # LATIN CAPITAL LETTER I WITH DOT ABOVE
        // while the default mappings for these code points are:
        // 0049; C; 0069; # LATIN CAPITAL LETTER I
        // 0130; F; 0069 0307; # LATIN CAPITAL LETTER I WITH DOT ABOVE
        // U+0130 is otherwise lowercased to U+0069 (UnicodeData.txt).
        // In case this code is used with CaseFolding.txt from an older version 
        // of Unicode where CaseFolding.txt contains mappings with a status of 
        // 'I' that have the opposite polarity ('I' mappings are included by 
        // default but excluded for Turkic), we must also hardcode the Unicode 
        // 3.2 mappings for the code points with 'I' mappings. 
        // Unicode 3.1.1 has 'I' mappings for U+0130 and U+0131.
        // Unicode 3.2 has a 'T' mapping for U+0130, and lowercases U+0131 to 
        // itself (see UnicodeData.txt).
        // when ch is out of bounds getProperty == 0
        int props = PROPERTY_.getProperty(ch);
        if (isNotExceptionIndicator(props)) {
            int type = UCharacterProperty.TYPE_MASK & props;
            if (type == UCharacterCategory.UPPERCASE_LETTER ||
                type == UCharacterCategory.TITLECASE_LETTER) {
                return ch + UCharacterProperty.getSignedValue(props);
            }
        } 
        else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_CASE_FOLDING_)) {
                int exception = PROPERTY_.getException(index, 
                               UCharacterProperty.EXC_CASE_FOLDING_);
                if (exception != 0) {
                    int foldedcasech = 
            PROPERTY_.getFoldCase(exception & LAST_CHAR_MASK_);
                    if (foldedcasech != 0){
                        return foldedcasech;
                    }
                }
                else {
                    // special case folding mappings, hardcoded
                    if (defaultmapping) { 
                        // default mappings
                        if (ch == 0x49 || ch == 0x130) { 
                            // 0049; C; 0069; # LATIN CAPITAL LETTER I */
                            // no simple default mapping for U+0130, 
                            // use UnicodeData.txt
                            return UCharacterProperty.LATIN_SMALL_LETTER_I_;
                        } 
                    } 
                    else {
                        // Turkic mappings 
                        if (ch == 0x49) {
                            // 0049; T; 0131; # LATIN CAPITAL LETTER I
                            return 0x131;
                        } 
                        else if (ch == 0x130) {
                            // 0130; T; 0069; 
                            // # LATIN CAPITAL LETTER I WITH DOT ABOVE
                            return 0x69;
                        }
                    }
                    // return ch itself because it is excluded from case folding
                    return ch;
                }                                  
            }
            if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_LOWERCASE_)) {  
                // not else! - allow to fall through from above
                return PROPERTY_.getException(index, 
                          UCharacterProperty.EXC_LOWERCASE_);
            }
        }
            
        return ch; // no mapping - return the character itself
    }

    /**
     * The given string is mapped to its case folding equivalent according to
     * UnicodeData.txt and CaseFolding.txt; if any character has no case 
     * folding equivalent, the character itself is returned.
     * "Full", multiple-code point case folding mappings are returned here.
     * For "simple" single-code point mappings use the API 
     * foldCase(int ch, boolean defaultmapping).
     * @param str            the String to be converted
     * @param defaultmapping Indicates if all mappings defined in 
     *                       CaseFolding.txt is to be used, otherwise the 
     *                       mappings for dotted I and dotless i marked with 
     *                       'I' in CaseFolding.txt will be skipped.
     * @return               the case folding equivalent of the character, if 
     *                       any; otherwise the character itself.
     * @see                  #foldCase(int, boolean)
     * @stable ICU 2.1
     */
    public static String foldCase(String str, boolean defaultmapping)
    {
        int          size   = str.length();
        StringBuffer result = new StringBuffer(size);
        int          offset  = 0;
        int          ch;

        // case mapping loop
        while (offset < size) {
            ch = UTF16.charAt(str, offset);
            offset += UTF16.getCharCount(ch);
            int props = PROPERTY_.getProperty(ch);
            if (isNotExceptionIndicator(props)) {
                int type = UCharacterProperty.TYPE_MASK & props;
                if (type == UCharacterCategory.UPPERCASE_LETTER ||
                    type == UCharacterCategory.TITLECASE_LETTER) {
                    ch += UCharacterProperty.getSignedValue(props);
                }
            }  
            else {
                int index = UCharacterProperty.getExceptionIndex(props);
                if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_CASE_FOLDING_)) {
                    int exception = PROPERTY_.getException(index, 
                               UCharacterProperty.EXC_CASE_FOLDING_);                             
                    if (exception != 0) {
                        PROPERTY_.getFoldCase(exception & LAST_CHAR_MASK_, 
                          exception >> SHIFT_24_, result);
                    } 
                    else {
                        // special case folding mappings, hardcoded
                        if (ch != 0x49 && ch != 0x130) {
                            // return ch itself because there is no special 
                            // mapping for it
                            UTF16.append(result, ch);
                            continue;
                        }
                        if (defaultmapping) {
                            // default mappings
                            if (ch == 0x49) {
                                // 0049; C; 0069; # LATIN CAPITAL LETTER I
                                result.append(
                          UCharacterProperty.LATIN_SMALL_LETTER_I_);
                            }
                            else if (ch == 0x130) {
                                // 0130; F; 0069 0307; 
                                // # LATIN CAPITAL LETTER I WITH DOT ABOVE
                                result.append(
                          UCharacterProperty.LATIN_SMALL_LETTER_I_);
                                result.append((char)0x307);
                            }
                        }
                        else {
                            // Turkic mappings
                            if (ch == 0x49) {
                                // 0049; T; 0131; # LATIN CAPITAL LETTER I
                                result.append((char)0x131);
                            } 
                            else if (ch == 0x130) {
                                // 0130; T; 0069; 
                                // # LATIN CAPITAL LETTER I WITH DOT ABOVE
                                result.append(
                          UCharacterProperty.LATIN_SMALL_LETTER_I_);
                            }
                        }
                    }
                    // do not fall through to the output of c
                    continue;
                } 
                else {
                    if (PROPERTY_.hasExceptionValue(index, 
                            UCharacterProperty.EXC_LOWERCASE_)) {
                        ch = PROPERTY_.getException(index, 
                            UCharacterProperty.EXC_LOWERCASE_);
                    }
                }
                
            }

            // handle 1:1 code point mappings from UnicodeData.txt
            UTF16.append(result, ch);
        }
        
        return result.toString();
    }
    
    /**
     * Bit mask for getting just the options from a string compare options word
     * that are relevant for case folding (of a single string or code point).
     * @internal
     */
    private static final int FOLD_CASE_OPTIONS_MASK = 0xff;
    
    /**
     * Option value for case folding: use default mappings defined in CaseFolding.txt.
     * @stable ICU 2.6
     */
    public static final int FOLD_CASE_DEFAULT    =      0x0000;
    /** 
     * Option value for case folding: exclude the mappings for dotted I 
     * and dotless i marked with 'I' in CaseFolding.txt. 
     * @stable ICU 2.6
     */
    public static final int FOLD_CASE_EXCLUDE_SPECIAL_I = 0x0001;
    
    /**
     * The given character is mapped to its case folding equivalent according 
     * to UnicodeData.txt and CaseFolding.txt; if the character has no case 
     * folding equivalent, the character itself is returned.
     * Only "simple", single-code point case folding mappings are used.
     * For "full", multiple-code point mappings use the API 
     * foldCase(String str, boolean defaultmapping).
     * @param ch             the character to be converted
     * @param options        A bit set for special processing. Currently the recognised options are
     *                        FOLD_CASE_EXCLUDE_SPECIAL_I and FOLD_CASE_DEFAULT 
     * @return               the case folding equivalent of the character, if 
     *                       any; otherwise the character itself.
     * @see #foldCase(String, boolean)
     * @stable ICU 2.6
     */

    /*
     * Issue for canonical caseless match (UAX #21):
     * Turkic casefolding (using "T" mappings in CaseFolding.txt) does not preserve
     * canonical equivalence, unlike default-option casefolding.
     * For example, I-grave and I + grave fold to strings that are not canonically
     * equivalent.
     * For more details, see the comment in Normalizer.compare() 
     * and the intermediate prototype changes for Jitterbug 2021.
     * (For example, revision 1.104 of uchar.c and 1.4 of CaseFolding.txt.)
     *
     * This did not get fixed because it appears that it is not possible to fix
     * it for uppercase and lowercase characters (I-grave vs. i-grave)
     * together in a way that they still fold to common result strings.
     */
    public static int foldCase(int ch, int options)
    {
        int props = PROPERTY_.getProperty(ch);
        if (isNotExceptionIndicator(props)) {
            int type = UCharacterProperty.TYPE_MASK & props;
            if (type == UCharacterCategory.UPPERCASE_LETTER ||
                type == UCharacterCategory.TITLECASE_LETTER) {
                return ch + UCharacterProperty.getSignedValue(props);
            }
        } 
        else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_CASE_FOLDING_)) {
                int exception = PROPERTY_.getException(index, 
                               UCharacterProperty.EXC_CASE_FOLDING_);
                if (exception != 0) {
                    int foldedcasech = 
            PROPERTY_.getFoldCase(exception & LAST_CHAR_MASK_);
                    if (foldedcasech != 0){
                        return foldedcasech;
                    }
                }
                else {
                    // special case folding mappings, hardcoded
                    if ((options&FOLD_CASE_OPTIONS_MASK)== Normalizer.FOLD_CASE_DEFAULT) { 
                        // default mappings
                        if (ch == 0x49 || ch == 0x130) { 
                            // 0049; C; 0069; # LATIN CAPITAL LETTER I */
                            // no simple default mapping for U+0130, 
                            // use UnicodeData.txt
                            return UCharacterProperty.LATIN_SMALL_LETTER_I_;
                        } 
                    } 
                    else {
                        // Turkic mappings 
                        if (ch == 0x49) {
                            // 0049; T; 0131; # LATIN CAPITAL LETTER I
                            return 0x131;
                        } 
                        else if (ch == 0x130) {
                            // 0130; T; 0069; 
                            // # LATIN CAPITAL LETTER I WITH DOT ABOVE
                            return 0x69;
                        }
                    }
                    // return ch itself because it is excluded from case folding
                    return ch;
                }                                  
            }
            if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_LOWERCASE_)) {  
                // not else! - allow to fall through from above
                return PROPERTY_.getException(index, 
                          UCharacterProperty.EXC_LOWERCASE_);
            }
        }
            
        return ch; // no mapping - return the character itself
    }
    
    /**
     * The given string is mapped to its case folding equivalent according to
     * UnicodeData.txt and CaseFolding.txt; if any character has no case 
     * folding equivalent, the character itself is returned.
     * "Full", multiple-code point case folding mappings are returned here.
     * For "simple" single-code point mappings use the API 
     * foldCase(int ch, boolean defaultmapping).
     * @param str            the String to be converted
     * @param options        A bit set for special processing. Currently the recognised options are
     *                        FOLD_CASE_EXCLUDE_SPECIAL_I and FOLD_CASE_DEFAULT 
     * @return               the case folding equivalent of the character, if 
     *                       any; otherwise the character itself.
     * @see #foldCase(int, boolean)
     * @stable ICU 2.6
     */
    public static final String foldCase(String str, int options){
        int          size   = str.length();
        StringBuffer result = new StringBuffer(size);
        int          offset  = 0;
        int          ch;

        // case mapping loop
        while (offset < size) {
            ch = UTF16.charAt(str, offset);
            offset += UTF16.getCharCount(ch);
            int props = getProperty(ch);
            if ((props & UCharacterProperty.EXCEPTION_MASK) == 0) {
                int type = UCharacterProperty.TYPE_MASK & props;
                if (type == UCharacterCategory.UPPERCASE_LETTER ||
                    type == UCharacterCategory.TITLECASE_LETTER) {
                    ch += UCharacterProperty.getSignedValue(props);
                }
            }  
            else {
                int index = UCharacterProperty.getExceptionIndex(props);
                if (PROPERTY_.hasExceptionValue(index, 
                        UCharacterProperty.EXC_CASE_FOLDING_)) {
                    int exception = PROPERTY_.getException(index, 
                               UCharacterProperty.EXC_CASE_FOLDING_);                             
                    if (exception != 0) {
                        PROPERTY_.getFoldCase(exception & LAST_CHAR_MASK_, 
                          exception >> SHIFT_24_, result);
                    } 
                    else {
                        // special case folding mappings, hardcoded
                        if (ch != 0x49 && ch != 0x130) {
                            // return ch itself because there is no special 
                            // mapping for it
                            UTF16.append(result, ch);
                            continue;
                        }
                        if ((options&FOLD_CASE_OPTIONS_MASK)== Normalizer.FOLD_CASE_DEFAULT) {
                            // default mappings
                            if (ch == 0x49) {
                                // 0049; C; 0069; # LATIN CAPITAL LETTER I
                                result.append(
                          UCharacterProperty.LATIN_SMALL_LETTER_I_);
                            }
                            else if (ch == 0x130) {
                                // 0130; F; 0069 0307; 
                                // # LATIN CAPITAL LETTER I WITH DOT ABOVE
                                result.append(
                          UCharacterProperty.LATIN_SMALL_LETTER_I_);
                                result.append((char)0x307);
                            }
                        }
                        else {
                            // Turkic mappings
                            if (ch == 0x49) {
                                // 0049; T; 0131; # LATIN CAPITAL LETTER I
                                result.append((char)0x131);
                            } 
                            else if (ch == 0x130) {
                                // 0130; T; 0069; 
                                // # LATIN CAPITAL LETTER I WITH DOT ABOVE
                                result.append(
                          UCharacterProperty.LATIN_SMALL_LETTER_I_);
                            }
                        }
                    }
                    // do not fall through to the output of c
                    continue;
                } 
                else {
                    if (PROPERTY_.hasExceptionValue(index, 
                            UCharacterProperty.EXC_LOWERCASE_)) {
                        ch = PROPERTY_.getException(index, 
                            UCharacterProperty.EXC_LOWERCASE_);
                    }
                }
                
            }

            // handle 1:1 code point mappings from UnicodeData.txt
            UTF16.append(result, ch);
        }
        
        return result.toString();
    }
    /**
     * Return numeric value of Han code points.
     * <br> This returns the value of Han 'numeric' code points,
     * including those for zero, ten, hundred, thousand, ten thousand,
     * and hundred million.  Unicode does not consider these to be
     * numeric. This includes both the standard and 'checkwriting'
     * characters, the 'big circle' zero character, and the standard
     * zero character.
     * @param ch code point to query
     * @return value if it is a Han 'numeric character,' otherwise return -1.  
     * @stable ICU 2.4
     */
    public static int getHanNumericValue(int ch)
    {
        switch(ch)
        {
        case IDEOGRAPHIC_NUMBER_ZERO_ :
        case CJK_IDEOGRAPH_COMPLEX_ZERO_ :
        return 0; // Han Zero
        case CJK_IDEOGRAPH_FIRST_ :
        case CJK_IDEOGRAPH_COMPLEX_ONE_ :
        return 1; // Han One
        case CJK_IDEOGRAPH_SECOND_ :
        case CJK_IDEOGRAPH_COMPLEX_TWO_ :
        return 2; // Han Two
        case CJK_IDEOGRAPH_THIRD_ :
        case CJK_IDEOGRAPH_COMPLEX_THREE_ :
        return 3; // Han Three
        case CJK_IDEOGRAPH_FOURTH_ :
        case CJK_IDEOGRAPH_COMPLEX_FOUR_ :
        return 4; // Han Four
        case CJK_IDEOGRAPH_FIFTH_ :
        case CJK_IDEOGRAPH_COMPLEX_FIVE_ :
        return 5; // Han Five
        case CJK_IDEOGRAPH_SIXTH_ :
        case CJK_IDEOGRAPH_COMPLEX_SIX_ :
        return 6; // Han Six
        case CJK_IDEOGRAPH_SEVENTH_ :
        case CJK_IDEOGRAPH_COMPLEX_SEVEN_ :
        return 7; // Han Seven
        case CJK_IDEOGRAPH_EIGHTH_ : 
        case CJK_IDEOGRAPH_COMPLEX_EIGHT_ :
        return 8; // Han Eight
        case CJK_IDEOGRAPH_NINETH_ :
        case CJK_IDEOGRAPH_COMPLEX_NINE_ :
        return 9; // Han Nine
        case CJK_IDEOGRAPH_TEN_ :
        case CJK_IDEOGRAPH_COMPLEX_TEN_ :
        return 10;
        case CJK_IDEOGRAPH_HUNDRED_ :
        case CJK_IDEOGRAPH_COMPLEX_HUNDRED_ :
        return 100;
        case CJK_IDEOGRAPH_THOUSAND_ :
        case CJK_IDEOGRAPH_COMPLEX_THOUSAND_ :
        return 1000;
        case CJK_IDEOGRAPH_TEN_THOUSAND_ :
        return 10000;
        case CJK_IDEOGRAPH_HUNDRED_MILLION_ :
        return 100000000;
        }
        return -1; // no value
    }
    
    /**
     * <p>Gets an iterator for character types, iterating over codepoints.</p>
     * Example of use:<br>
     * <pre>
     * RangeValueIterator iterator = UCharacter.getTypeIterator();
     * RangeValueIterator.Element element = new RangeValueIterator.Element();
     * while (iterator.next(element)) {
     *     System.out.println("Codepoint \\u" + 
     *                        Integer.toHexString(element.start) + 
     *                        " to codepoint \\u" +
     *                        Integer.toHexString(element.limit - 1) + 
     *                        " has the character type " + 
     *                        element.value);
     * }
     * </pre>
     * @return an iterator 
     * @stable ICU 2.6
     */
    public static RangeValueIterator getTypeIterator()
    {
        return new UCharacterTypeIterator(PROPERTY_);
    }

    /**
     * <p>Gets an iterator for character names, iterating over codepoints.</p>
     * <p>This API only gets the iterator for the modern, most up-to-date 
     * Unicode names. For older 1.0 Unicode names use get1_0NameIterator() or
     * for extended names use getExtendedNameIterator().</p>
     * Example of use:<br>
     * <pre>
     * ValueIterator iterator = UCharacter.getNameIterator();
     * ValueIterator.Element element = new ValueIterator.Element();
     * while (iterator.next(element)) {
     *     System.out.println("Codepoint \\u" + 
     *                        Integer.toHexString(element.codepoint) +
     *                        " has the name " + (String)element.value);
     * }
     * </pre>
     * <p>The maximal range which the name iterator iterates is from 
     * UCharacter.MIN_VALUE to UCharacter.MAX_VALUE.</p>
     * @return an iterator 
     * @stable ICU 2.6
     */
    public static ValueIterator getNameIterator()
    {
        if(NAME_==null){
            throw new RuntimeException("Could not load unames.icu");
        }
        return new UCharacterNameIterator(NAME_,
                      UCharacterNameChoice.UNICODE_CHAR_NAME);
    }
    
    /**
     * <p>Gets an iterator for character names, iterating over codepoints.</p>
     * <p>This API only gets the iterator for the older 1.0 Unicode names. 
     * For modern, most up-to-date Unicode names use getNameIterator() or
     * for extended names use getExtendedNameIterator().</p>
     * Example of use:<br>
     * <pre>
     * ValueIterator iterator = UCharacter.get1_0NameIterator();
     * ValueIterator.Element element = new ValueIterator.Element();
     * while (iterator.next(element)) {
     *     System.out.println("Codepoint \\u" + 
     *                        Integer.toHexString(element.codepoint) +
     *                        " has the name " + (String)element.value);
     * }
     * </pre>
     * <p>The maximal range which the name iterator iterates is from 
     * @return an iterator 
     * @stable ICU 2.6
     */
    public static ValueIterator getName1_0Iterator()
    {
        if(NAME_==null){
            throw new RuntimeException("Could not load unames.icu");
        }
        return new UCharacterNameIterator(NAME_,
                      UCharacterNameChoice.UNICODE_10_CHAR_NAME);
    }
    
    /**
     * <p>Gets an iterator for character names, iterating over codepoints.</p>
     * <p>This API only gets the iterator for the extended names. 
     * For modern, most up-to-date Unicode names use getNameIterator() or
     * for older 1.0 Unicode names use get1_0NameIterator().</p>
     * Example of use:<br>
     * <pre>
     * ValueIterator iterator = UCharacter.getExtendedNameIterator();
     * ValueIterator.Element element = new ValueIterator.Element();
     * while (iterator.next(element)) {
     *     System.out.println("Codepoint \\u" + 
     *                        Integer.toHexString(element.codepoint) +
     *                        " has the name " + (String)element.value);
     * }
     * </pre>
     * <p>The maximal range which the name iterator iterates is from 
     * @return an iterator 
     * @stable ICU 2.6
     */
    public static ValueIterator getExtendedNameIterator()
    {
        if(NAME_==null){
            throw new RuntimeException("Could not load unames.icu");
        }
        return new UCharacterNameIterator(NAME_,
                      UCharacterNameChoice.EXTENDED_CHAR_NAME);
    }
    
    /**
     * <p>Get the "age" of the code point.</p>
     * <p>The "age" is the Unicode version when the code point was first
     * designated (as a non-character or for Private Use) or assigned a 
     * character.
     * <p>This can be useful to avoid emitting code points to receiving 
     * processes that do not accept newer characters.</p>
     * <p>The data is from the UCD file DerivedAge.txt.</p>
     * @param ch The code point.
     * @return the Unicode version number
     * @stable ICU 2.6
     */
    public static VersionInfo getAge(int ch) 
    {
        if (ch < MIN_VALUE || ch > MAX_VALUE) {
        throw new IllegalArgumentException("Codepoint out of bounds");
        }
        return PROPERTY_.getAge(ch);
    }
    
    /**
     * <p>Check a binary Unicode property for a code point.</p> 
     * <p>Unicode, especially in version 3.2, defines many more properties 
     * than the original set in UnicodeData.txt.</p>
     * <p>This API is intended to reflect Unicode properties as defined in 
     * the Unicode Character Database (UCD) and Unicode Technical Reports 
     * (UTR).</p>
     * <p>For details about the properties see 
     * <a href=http://www.unicode.org/>http://www.unicode.org/</a>.</p>
     * <p>For names of Unicode properties see the UCD file 
     * PropertyAliases.txt.</p>
     * <p>This API does not check the validity of the codepoint.</p>
     * <p>Important: If ICU is built with UCD files from Unicode versions 
     * below 3.2, then properties marked with "new" are not or 
     * not fully available.</p>
     * @param ch code point to test.
     * @param property selector constant from com.ibm.icu.lang.UProperty, 
     *        identifies which binary property to check.
     * @return true or false according to the binary Unicode property value 
     *         for ch. Also false if property is out of bounds or if the 
     *         Unicode version does not have data for the property at all, or 
     *         not for this code point.
     * @see com.ibm.icu.lang.UProperty
     * @stable ICU 2.6
     */
    public static boolean hasBinaryProperty(int ch, int property) 
    {
    if (ch < MIN_VALUE || ch > MAX_VALUE) {
        throw new IllegalArgumentException("Codepoint out of bounds");
        }
        return PROPERTY_.hasBinaryProperty(ch, property);
    }
        
    /**
     * <p>Check if a code point has the Alphabetic Unicode property.</p> 
     * <p>Same as UCharacter.hasBinaryProperty(ch, UProperty.ALPHABETIC).</p>
     * <p>Different from UCharacter.isLetter(ch)!</p> 
     * @stable ICU 2.6
     * @param ch codepoint to be tested
     */
    public static boolean isUAlphabetic(int ch)
    {
    return hasBinaryProperty(ch, UProperty.ALPHABETIC);
    }

    /**
     * <p>Check if a code point has the Lowercase Unicode property.</p>
     * <p>Same as UCharacter.hasBinaryProperty(ch, UProperty.LOWERCASE).</p>
     * <p>This is different from UCharacter.isLowerCase(ch)!</p>
     * @param ch codepoint to be tested
     * @stable ICU 2.6
     */
    public static boolean isULowercase(int ch) 
    {
    return hasBinaryProperty(ch, UProperty.LOWERCASE);
    }

    /**
     * <p>Check if a code point has the Uppercase Unicode property.</p>
     * <p>Same as UCharacter.hasBinaryProperty(ch, UProperty.UPPERCASE).</p>
     * <p>This is different from UCharacter.isUpperCase(ch)!</p>
     * @param ch codepoint to be tested
     * @stable ICU 2.6
     */
    public static boolean isUUppercase(int ch) 
    {
    return hasBinaryProperty(ch, UProperty.UPPERCASE);
    }

    /**
     * <p>Check if a code point has the White_Space Unicode property.</p>
     * <p>Same as UCharacter.hasBinaryProperty(ch, UProperty.WHITE_SPACE).</p>
     * <p>This is different from both UCharacter.isSpace(ch) and 
     * UCharacter.isWhitespace(ch)!</p>
     * @param ch codepoint to be tested
     * @stable ICU 2.6
     */
    public static boolean isUWhiteSpace(int ch) 
    {
    return hasBinaryProperty(ch, UProperty.WHITE_SPACE);
    }


    /**
     * <p>Gets the property value for an Unicode property type of a code point. 
     * Also returns binary and mask property values.</p>
     * <p>Unicode, especially in version 3.2, defines many more properties than 
     * the original set in UnicodeData.txt.</p>
     * <p>The properties APIs are intended to reflect Unicode properties as 
     * defined in the Unicode Character Database (UCD) and Unicode Technical 
     * Reports (UTR). For details about the properties see 
     * http://www.unicode.org/.</p>
     * <p>For names of Unicode properties see the UCD file PropertyAliases.txt.
     * </p>
     * <pre>
     * Sample usage:
     * int ea = UCharacter.getIntPropertyValue(c, UProperty.EAST_ASIAN_WIDTH);
     * int ideo = UCharacter.getIntPropertyValue(c, UProperty.IDEOGRAPHIC);
     * boolean b = (ideo == 1) ? true : false; 
     * </pre>
     * @param ch code point to test.
     * @param type UProperty selector constant, identifies which binary 
     *        property to check. Must be 
     *        UProperty.BINARY_START &lt;= type &lt; UProperty.BINARY_LIMIT or 
     *        UProperty.INT_START &lt;= type &lt; UProperty.INT_LIMIT or 
     *        UProperty.MASK_START &lt;= type &lt; UProperty.MASK_LIMIT.
     * @return numeric value that is directly the property value or,
     *         for enumerated properties, corresponds to the numeric value of 
     *         the enumerated constant of the respective property value 
     *         enumeration type (cast to enum type if necessary).
     *         Returns 0 or 1 (for false / true) for binary Unicode properties.
     *         Returns a bit-mask for mask properties.
     *         Returns 0 if 'type' is out of bounds or if the Unicode version
     *         does not have data for the property at all, or not for this code 
     *         point.
     * @see UProperty
     * @see #hasBinaryProperty
     * @see #getIntPropertyMinValue
     * @see #getIntPropertyMaxValue
     * @see #getUnicodeVersion
     * @stable ICU 2.4
     */
    public static int getIntPropertyValue(int ch, int type)
    {
        if (type < UProperty.BINARY_START) {
            return 0; // undefined
        } 
        else if (type < UProperty.BINARY_LIMIT) {
            return hasBinaryProperty(ch, type) ? 1 : 0;
        } 
        else if (type < UProperty.INT_START) {
            return 0; // undefined
        } 
        else if (type < UProperty.INT_LIMIT) {
            //int result = 0;
            switch (type) {
            case UProperty.BIDI_CLASS:
                return getDirection(ch);
            case UProperty.BLOCK:
                return UnicodeBlock.idOf(ch);
            case UProperty.CANONICAL_COMBINING_CLASS:
                return getCombiningClass(ch);
            case UProperty.DECOMPOSITION_TYPE:
                return PROPERTY_.getAdditional(ch, 2) 
            & DECOMPOSITION_TYPE_MASK_;
            case UProperty.EAST_ASIAN_WIDTH:
                return (PROPERTY_.getAdditional(ch, 0)
            & EAST_ASIAN_MASK_) >> EAST_ASIAN_SHIFT_;
            case UProperty.GENERAL_CATEGORY:
                return getType(ch);
            case UProperty.JOINING_GROUP:
                return (PROPERTY_.getAdditional(ch, 2) 
            & JOINING_GROUP_MASK_) >> JOINING_GROUP_SHIFT_;
            case UProperty.JOINING_TYPE:
                return (int)(PROPERTY_.getAdditional(ch, 2)& JOINING_TYPE_MASK_)>> JOINING_TYPE_SHIFT_;
                // ArabicShaping.txt:
                // Note: Characters of joining type T and most characters of 
                // joining type U are not explicitly listed in this file.
                // Characters of joining type T can [be] derived by the following formula:
                //   T = Mn + Cf - ZWNJ - ZWJ
                /*
          result = (PROPERTY_.getAdditional(ch, 2) 
          & JOINING_TYPE_MASK_) >> JOINING_TYPE_SHIFT_;
          if (result == 0 && ch != ZERO_WIDTH_NON_JOINER_ 
          && ch != ZERO_WIDTH_JOINER_) {
          int t = getType(ch);
          if (t == UCharacterCategory.NON_SPACING_MARK
          || t == UCharacterCategory.FORMAT) {
          result = JoiningType.TRANSPARENT;
          }
          }
          return result;
                */
            case UProperty.LINE_BREAK:
                return (int)(PROPERTY_.getAdditional(ch, 0)& LINE_BREAK_MASK_)>>LINE_BREAK_SHIFT_;
                /*
                 * LineBreak.txt:
                 *  - Assigned characters that are not listed explicitly are given the value
                 *    "AL".
                 *  - Unassigned characters are given the value "XX".
                 * ...
                 * E000..F8FF;XX # <Private Use, First>..<Private Use, Last>
                 * F0000..FFFFD;XX # <Plane 15 Private Use, First>..<Plane 15 Private Use, Last>
                 * 100000..10FFFD;XX # <Plane 16 Private Use, First>..<Plane 16 Private Use, Last>
                 */
        /*
          result = (PROPERTY_.getAdditional(ch, 0) 
          & LINE_BREAK_MASK_) >> LINE_BREAK_SHIFT_;
          if (result == 0) {
          int t = getType(ch);
          if (t != UCharacterCategory.UNASSIGNED 
          && t != UCharacterCategory.PRIVATE_USE) {
          result = LineBreak.ALPHABETIC;
          }
          }
          return result;
                */
            case UProperty.NUMERIC_TYPE:
                return getNumericType(PROPERTY_.getProperty(ch));
            case UProperty.SCRIPT:
                return UScript.getScript(ch);
            case UProperty.HANGUL_SYLLABLE_TYPE:
        /* purely algorithmic; hardcode known characters, check for assigned new ones */ 
        if(ch<NormalizerImpl.JAMO_L_BASE) { 
            /* NA */ 
        } else if(ch<=0x11ff) { 
            /* Jamo range */ 
            if(ch<=0x115f) { 
            /* Jamo L range, HANGUL CHOSEONG ... */ 
            if(ch==0x115f || ch<=0x1159 || getType(ch)==UCharacterCategory.OTHER_LETTER) { 
                return HangulSyllableType.LEADING_JAMO; 
            } 
            } else if(ch<=0x11a7) { 
            /* Jamo V range, HANGUL JUNGSEONG ... */ 
            if(ch<=0x11a2 || getType(ch)==UCharacterCategory.OTHER_LETTER) { 
                return HangulSyllableType.VOWEL_JAMO; 
            } 
            } else { 
            /* Jamo T range */ 
            if(ch<=0x11f9 || getType(ch)==UCharacterCategory.OTHER_LETTER) { 
                return HangulSyllableType.TRAILING_JAMO; 
            } 
            } 
        } else if((ch-=NormalizerImpl.HANGUL_BASE)<0) { 
            /* NA */ 
        } else if(ch<NormalizerImpl.HANGUL_COUNT) { 
            /* Hangul syllable */ 
            return ch%NormalizerImpl.JAMO_T_COUNT==0 ? HangulSyllableType.LV_SYLLABLE : HangulSyllableType.LVT_SYLLABLE; 
        } 
        return 0; /* NA */ 

            case UProperty.NFD_QUICK_CHECK:
            case UProperty.NFKD_QUICK_CHECK:
            case UProperty.NFC_QUICK_CHECK:
            case UProperty.NFKC_QUICK_CHECK:
                return NormalizerImpl.quickCheck(ch, (type-UProperty.NFD_QUICK_CHECK)+2); // 2=UNORM_NFD
            case UProperty.LEAD_CANONICAL_COMBINING_CLASS:
                return NormalizerImpl.getFCD16(ch)>>8;
            case UProperty.TRAIL_CANONICAL_COMBINING_CLASS:
                return NormalizerImpl.getFCD16(ch)&0xff;
            default:
               
        return 0; /* undefined */
            }
        } else if (type == UProperty.GENERAL_CATEGORY_MASK) {
            return UCharacterProperty.getMask(getType(ch));
        }
        return 0; // undefined
    }
    /**
     * Returns a string version of the property value.
     * @param propertyEnum
     * @param codepoint
     * @param nameChoice
     * @return value as string
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static String getStringPropertyValue(int propertyEnum, int codepoint, int nameChoice) {
        // TODO some of these are less efficient, since a string is forced!
        if ((propertyEnum >= UProperty.BINARY_START && propertyEnum < UProperty.BINARY_LIMIT) ||
                (propertyEnum >= UProperty.INT_START && propertyEnum < UProperty.INT_LIMIT)) {
            return getPropertyValueName(propertyEnum, getIntPropertyValue(codepoint, propertyEnum), nameChoice);
        }
        if (propertyEnum == UProperty.NUMERIC_VALUE) {
                return String.valueOf(getUnicodeNumericValue(codepoint));
        }
        // otherwise must be string property
        switch (propertyEnum) {
        case UProperty.AGE: return getAge(codepoint).toString();
        case UProperty.ISO_COMMENT: return getISOComment(codepoint);
        case UProperty.BIDI_MIRRORING_GLYPH: return UTF16.valueOf(getMirror(codepoint));
        case UProperty.CASE_FOLDING: return foldCase(UTF16.valueOf(codepoint), true);
        case UProperty.LOWERCASE_MAPPING: return toLowerCase(UTF16.valueOf(codepoint));
        case UProperty.NAME: return getName(codepoint);
        case UProperty.SIMPLE_CASE_FOLDING: return UTF16.valueOf(foldCase(codepoint,true));
        case UProperty.SIMPLE_LOWERCASE_MAPPING: return UTF16.valueOf(toLowerCase(codepoint));
        case UProperty.SIMPLE_TITLECASE_MAPPING: return UTF16.valueOf(toTitleCase(codepoint));
        case UProperty.SIMPLE_UPPERCASE_MAPPING: return UTF16.valueOf(toUpperCase(codepoint));
        case UProperty.TITLECASE_MAPPING: return toTitleCase(UTF16.valueOf(codepoint),null);
        case UProperty.UNICODE_1_NAME: return getName1_0(codepoint);
        case UProperty.UPPERCASE_MAPPING: return toUpperCase(UTF16.valueOf(codepoint));        
        }
        throw new IllegalArgumentException("Illegal Property Enum");
    }
    
    /**
     * Get the minimum value for an integer/binary Unicode property type.
     * Can be used together with UCharacter.getIntPropertyMaxValue(int)
     * to allocate arrays of com.ibm.icu.text.UnicodeSet or similar.
     * @param type UProperty selector constant, identifies which binary 
     *        property to check. Must be 
     *        UProperty.BINARY_START &lt;= type &lt; UProperty.BINARY_LIMIT or 
     *        UProperty.INT_START &lt;= type &lt; UProperty.INT_LIMIT.
     * @return Minimum value returned by UCharacter.getIntPropertyValue(int) 
     *         for a Unicode property. 0 if the property 
     *         selector 'type' is out of range.
     * @see UProperty
     * @see #hasBinaryProperty
     * @see #getUnicodeVersion
     * @see #getIntPropertyMaxValue
     * @see #getIntPropertyValue
     * @stable ICU 2.4
     */
    public static int getIntPropertyMinValue(int type)
    {

        return 0; // undefined; and: all other properties have a minimum value 
    // of 0
    }

    
    /**
     * Get the maximum value for an integer/binary Unicode property.
     * Can be used together with UCharacter.getIntPropertyMinValue(int)
     * to allocate arrays of com.ibm.icu.text.UnicodeSet or similar.
     * Examples for min/max values (for Unicode 3.2):
     * <ul>
     * <li> UProperty.BIDI_CLASS:    0/18 (UCharacterDirection.LEFT_TO_RIGHT/UCharacterDirection.BOUNDARY_NEUTRAL)
     * <li> UProperty.SCRIPT:        0/45 (UScript.COMMON/UScript.TAGBANWA)
     * <li> UProperty.IDEOGRAPHIC:   0/1  (false/true)
     * </ul>
     * For undefined UProperty constant values, min/max values will be 0/-1.
     * @param type UProperty selector constant, identifies which binary 
     *        property to check. Must be 
     *        UProperty.BINARY_START &lt;= type &lt; UProperty.BINARY_LIMIT or 
     *        UProperty.INT_START &lt;= type &lt; UProperty.INT_LIMIT.
     * @return Maximum value returned by u_getIntPropertyValue for a Unicode 
     *         property. &lt;= 0 if the property selector 'type' is out of range.
     * @see UProperty
     * @see #hasBinaryProperty
     * @see #getUnicodeVersion
     * @see #getIntPropertyMaxValue
     * @see #getIntPropertyValue
     * @stable ICU 2.4
     */
    public static int getIntPropertyMaxValue(int type)
    { 
        if (type < UProperty.BINARY_START) {
            return -1; // undefined
        } 
        else if (type < UProperty.BINARY_LIMIT) {
            return 1; // maximum TRUE for all binary properties
        } 
        else if (type < UProperty.INT_START) {
            return -1; // undefined
        } 
        else if (type < UProperty.INT_LIMIT) {
            int max = 0;
            switch (type) {
            case UProperty.BIDI_CLASS:
                return UCharacterDirection.CHAR_DIRECTION_COUNT - 1;
            case UProperty.BLOCK:
                max = (PROPERTY_.getMaxValues(0)
               & BLOCK_MASK_) >> BLOCK_SHIFT_;
                return (max!=0) ? max : UnicodeBlock.COUNT - 1;
            case UProperty.CANONICAL_COMBINING_CLASS:
            case UProperty.LEAD_CANONICAL_COMBINING_CLASS:
            case UProperty.TRAIL_CANONICAL_COMBINING_CLASS:
                return 0xff; // TODO do we need to be more precise, 
        // getting the actual maximum?
            case UProperty.DECOMPOSITION_TYPE:
                max=PROPERTY_.getMaxValues(2) & DECOMPOSITION_TYPE_MASK_;
                return (max!=0) ? max : DecompositionType.COUNT - 1;
            case UProperty.EAST_ASIAN_WIDTH:
                max=(PROPERTY_.getMaxValues(0) & EAST_ASIAN_MASK_) >> EAST_ASIAN_SHIFT_;
                return (max!=0) ? max : EastAsianWidth.COUNT - 1;
            case UProperty.GENERAL_CATEGORY:
                return UCharacterCategory.CHAR_CATEGORY_COUNT - 1;
            case UProperty.JOINING_GROUP:
        max=(PROPERTY_.getMaxValues(2) & JOINING_GROUP_MASK_) >> JOINING_GROUP_SHIFT_;
        return (max!=0) ? max : JoiningGroup.COUNT - 1;
            case UProperty.JOINING_TYPE:
                max=(PROPERTY_.getMaxValues(2) & JOINING_TYPE_MASK_) >> JOINING_TYPE_SHIFT_;
                return (max!=0) ? max :  JoiningType.COUNT - 1;
            case UProperty.LINE_BREAK:
                max=(PROPERTY_.getMaxValues(0) & LINE_BREAK_MASK_) >> LINE_BREAK_SHIFT_;
                return (max!=0) ? max :  LineBreak.COUNT - 1;
            case UProperty.NUMERIC_TYPE:
                return NumericType.COUNT - 1;
            case UProperty.SCRIPT:
                max = PROPERTY_.getMaxValues(0) & SCRIPT_MASK_;   
                return (max!= 0)? max :  UScript.CODE_LIMIT - 1;
            case UProperty.HANGUL_SYLLABLE_TYPE:
        return HangulSyllableType.COUNT-1;
            case UProperty.NFD_QUICK_CHECK:
            case UProperty.NFKD_QUICK_CHECK:
                return 1; // YES -- these are never "maybe", only "no" or "yes"
            case UProperty.NFC_QUICK_CHECK:
            case UProperty.NFKC_QUICK_CHECK:
                return 2; // MAYBE
            }

        }
        return -1; // undefined
    }

    /**
     * Provide the java.lang.Character forDigit API, for convenience.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static char forDigit(int digit, int radix) {
        return java.lang.Character.forDigit(digit, radix);
    }

    // JDK 1.5 API coverage

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @see UTF16#LEAD_SURROGATE_MIN_VALUE
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final char MIN_HIGH_SURROGATE = UTF16.LEAD_SURROGATE_MIN_VALUE;

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @see UTF16#LEAD_SURROGATE_MAX_VALUE
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final char MAX_HIGH_SURROGATE = UTF16.LEAD_SURROGATE_MAX_VALUE;

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @see UTF16#TRAIL_SURROGATE_MIN_VALUE
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final char MIN_LOW_SURROGATE = UTF16.TRAIL_SURROGATE_MIN_VALUE;

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @see UTF16#TRAIL_SURROGATE_MAX_VALUE
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final char MAX_LOW_SURROGATE = UTF16.TRAIL_SURROGATE_MAX_VALUE;

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @see UTF16#SURROGATE_MIN_VALUE
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final char MIN_SURROGATE = UTF16.SURROGATE_MIN_VALUE;

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @see UTF16#SURROGATE_MAX_VALUE
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final char MAX_SURROGATE = UTF16.SURROGATE_MAX_VALUE;

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @see UTF16#SUPPLEMENTARY_MIN_VALUE
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int  MIN_SUPPLEMENTARY_CODE_POINT = UTF16.SUPPLEMENTARY_MIN_VALUE;

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @see UTF16#CODEPOINT_MAX_VALUE
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int  MAX_CODE_POINT = UTF16.CODEPOINT_MAX_VALUE;

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @see UTF16#CODEPOINT_MIN_VALUE
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int  MIN_CODE_POINT = UTF16.CODEPOINT_MIN_VALUE;

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @param cp the code point to check
     * @return true if cp is a valid code point
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final boolean isValidCodePoint(int cp) {
        return cp >= 0 && cp <= MAX_CODE_POINT;
    }

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @param cp the code point to check
     * @return true if cp is a supplementary code point
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final boolean isSupplementaryCodePoint(int cp) {
        return cp >= UTF16.SUPPLEMENTARY_MIN_VALUE
            && cp <= UTF16.CODEPOINT_MAX_VALUE;
    }

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @param ch the char to check
     * @return true if ch is a high (lead) surrogate
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static boolean isHighSurrogate(char ch) {
        return ch >= MIN_HIGH_SURROGATE && ch <= MAX_HIGH_SURROGATE;
    }

    /**
     * Cover the JDK 1.5 API, for convenience.
     * @param ch the char to check
     * @return true if ch is a low (trail) surrogate
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static boolean isLowSurrogate(char ch) {
        return ch >= MIN_LOW_SURROGATE && ch <= MAX_LOW_SURROGATE;
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Return true if the chars
     * form a valid surrogate pair.
     * @param high the high (lead) char
     * @param low the low (trail) char
     * @return true if high, low form a surrogate pair
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final boolean isSurrogatePair(char high, char low) {
        return isHighSurrogate(high) && isLowSurrogate(low);
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Return the number of chars needed
     * to represent the code point.  This does not check the
     * code point for validity.
     * @param cp the code point to check
     * @return the number of chars needed to represent the code point
     * @see UTF16#getCharCount
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static int charCount(int cp) {
        return UTF16.getCharCount(cp);
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Return the code point represented by
     * the characters.  This does not check the surrogate pair for validity.
     * @param high the high (lead) surrogate
     * @param low the low (trail) surrogate
     * @return the code point formed by the surrogate pair
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int toCodePoint(char high, char low) {
        return UCharacterProperty.getRawSupplementary(high, low);
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Return the code point at index.
     * <br/><b>Note</b>: the semantics of this API is different from the related UTF16
     * API.  This examines only the characters at index and index+1.
     * @param seq the characters to check
     * @param index the index of the first or only char forming the code point
     * @return the code point at the index
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int codePointAt(CharSequence seq, int index) {
        char c1 = seq.charAt(index++);
        if (isHighSurrogate(c1)) {
            if (index < seq.length()) {
                char c2 = seq.charAt(index);
                if (isLowSurrogate(c2)) {
                    return toCodePoint(c1, c2);
                }
            }
        }
        return c1;
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Return the code point at index.
     * <br/><b>Note</b>: the semantics of this API is different from the related UTF16
     * API.  This examines only the characters at index and index+1.
     * @param text the characters to check
     * @param index the index of the first or only char forming the code point
     * @return the code point at the index
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int codePointAt(char[] text, int index) {
        char c1 = text[index++];
        if (isHighSurrogate(c1)) {
            if (index < text.length) {
                char c2 = text[index];
                if (isLowSurrogate(c2)) {
                    return toCodePoint(c1, c2);
                }
            }
        }
        return c1;
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Return the code point at index.
     * <br/><b>Note</b>: the semantics of this API is different from the related UTF16
     * API.  This examines only the characters at index and index+1.
     * @param text the characters to check
     * @param index the index of the first or only char forming the code point
     * @param limit the limit of the valid text
     * @return the code point at the index
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int codePointAt(char[] text, int index, int limit) {
    if (index >= limit || limit > text.length) {
        throw new IndexOutOfBoundsException();
    }
        char c1 = text[index++];
        if (isHighSurrogate(c1)) {
            if (index < limit) {
                char c2 = text[index];
                if (isLowSurrogate(c2)) {
                    return toCodePoint(c1, c2);
                }
            }
        }
        return c1;
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Return the code point before index.
     * <br/><b>Note</b>: the semantics of this API is different from the related UTF16
     * API.  This examines only the characters at index-1 and index-2.
     * @param seq the characters to check
     * @param index the index after the last or only char forming the code point
     * @return the code point before the index
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int codePointBefore(CharSequence seq, int index) {
        char c2 = seq.charAt(--index);
        if (isLowSurrogate(c2)) {
            if (index > 0) {
                char c1 = seq.charAt(--index);
                if (isHighSurrogate(c1)) {
                    return toCodePoint(c1, c2);
                }
            }
        }
        return c2;
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Return the code point before index.
     * <br/><b>Note</b>: the semantics of this API is different from the related UTF16
     * API.  This examines only the characters at index-1 and index-2.
     * @param text the characters to check
     * @param index the index after the last or only char forming the code point
     * @return the code point before the index
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int codePointBefore(char[] text, int index) {
        char c2 = text[--index];
        if (isLowSurrogate(c2)) {
            if (index > 0) {
                char c1 = text[--index];
                if (isHighSurrogate(c1)) {
                    return toCodePoint(c1, c2);
                }
            }
        }
        return c2;
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Return the code point before index.
     * <br/><b>Note</b>: the semantics of this API is different from the related UTF16
     * API.  This examines only the characters at index-1 and index-2.
     * @param text the characters to check
     * @param index the index after the last or only char forming the code point
     * @param limit the start of the valid text
     * @return the code point before the index
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int codePointBefore(char[] text, int index, int limit) {
    if (index <= limit || limit < 0) {
        throw new IndexOutOfBoundsException();
    }
        char c2 = text[--index];
        if (isLowSurrogate(c2)) {
            if (index > limit) {
                char c1 = text[--index];
                if (isHighSurrogate(c1)) {
                    return toCodePoint(c1, c2);
                }
            }
        }
        return c2;
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Writes the chars representing the
     * code point into the destination at the given index.
     * @param cp the code point to convert
     * @param dst the destination array into which to put the char(s) representing the code point
     * @param dstIndex the index at which to put the first (or only) char
     * @return the count of the number of chars written (1 or 2)
     * @throws IllegalArgumentException if cp is not a valid code point
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int toChars(int cp, char[] dst, int dstIndex) {
        if (cp >= 0) {
            if (cp < MIN_SUPPLEMENTARY_CODE_POINT) {
                dst[dstIndex] = (char)cp;
                return 1;
            }
            if (cp <= MAX_CODE_POINT) {
                dst[dstIndex] = UTF16.getLeadSurrogate(cp);
                dst[dstIndex+1] = UTF16.getTrailSurrogate(cp);
                return 2;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Cover the JDK 1.5 API, for convenience.  Returns a char array
     * representing the code point.
     * @param cp the code point to convert
     * @return an array containing the char(s) representing the code point
     * @throws IllegalArgumentException if cp is not a valid code point
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final char[] toChars(int cp) {
        if (cp >= 0) {
            if (cp < MIN_SUPPLEMENTARY_CODE_POINT) {
                return new char[] { (char)cp };
            }
            if (cp <= MAX_CODE_POINT) {
                return new char[] {
                    UTF16.getLeadSurrogate(cp),
                    UTF16.getTrailSurrogate(cp)
                };
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Cover the JDK API, for convenience.  Return a byte representing the directionality of
     * the character.
     * <br/><b>Note</b>: Unlike the JDK, this returns DIRECTIONALITY_LEFT_TO_RIGHT for undefined or
     * out-of-bounds characters.  <br/><b>Note</b>: The return value must be
     * tested using the constants defined in {@link UCharacterEnums.ECharacterDirection}
     * since the values are different from the ones defined by <code>java.lang.Character</code>.
     * @param cp the code point to check
     * @return the directionality of the code point
     * @see #getDirection
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static byte getDirectionality(int cp)
    {
        // when ch is out of bounds getProperty == 0
        return (byte)((getProperty(cp) >> BIDI_SHIFT_) & BIDI_MASK_AFTER_SHIFT_);
    }

    /**
     * Cover the JDK API, for convenience.  Count the number of code points in the range of text.
     * @param text the characters to check
     * @param start the start of the range
     * @param limit the limit of the range
     * @return the number of code points in the range
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static int codePointCount(CharSequence text, int start, int limit) {
    if (start < 0 || limit < start || limit > text.length()) {
        throw new IndexOutOfBoundsException("start (" + start +
                        ") or limit (" + limit +
                        ") invalid or out of range 0, " + text.length());
    }

    int len = limit - start;
    while (limit > start) {
        char ch = text.charAt(--limit);
        while (ch >= MIN_LOW_SURROGATE && ch <= MAX_LOW_SURROGATE && limit > start) {
        ch = text.charAt(--limit);
        if (ch >= MIN_HIGH_SURROGATE && ch <= MAX_HIGH_SURROGATE) {
            --len;
            break;
        }
        }
    }
    return len;
    }

    /**
     * Cover the JDK API, for convenience.  Count the number of code points in the range of text.
     * @param text the characters to check
     * @param start the start of the range
     * @param limit the limit of the range
     * @return the number of code points in the range
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static int codePointCount(char[] text, int start, int limit) {
    if (start < 0 || limit < start || limit > text.length) {
        throw new IndexOutOfBoundsException("start (" + start +
                        ") or limit (" + limit +
                        ") invalid or out of range 0, " + text.length);
    }

    int len = limit - start;
    while (limit > start) {
        char ch = text[--limit];
        while (ch >= MIN_LOW_SURROGATE && ch <= MAX_LOW_SURROGATE && limit > start) {
        ch = text[--limit];
        if (ch >= MIN_HIGH_SURROGATE && ch <= MAX_HIGH_SURROGATE) {
            --len;
            break;
        }
        }
    }
    return len;
    }

    /**
     * Cover the JDK API, for convenience.  Adjust the char index by a code point offset.
     * @param text the characters to check
     * @param index the index to adjust
     * @param codePointOffset the number of code points by which to offset the index
     * @return the adjusted index
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static int offsetByCodePoints(CharSequence text, int index, int codePointOffset) {
    if (index < 0 || index > text.length()) {
        throw new IndexOutOfBoundsException("index ( " + index +
                        ") out of range 0, " + text.length());
    }

    if (codePointOffset < 0) {
        while (++codePointOffset <= 0) {
        char ch = text.charAt(--index);
        while (ch >= MIN_LOW_SURROGATE && ch <= MAX_LOW_SURROGATE && index > 0) {
            ch = text.charAt(--index);
            if (ch < MIN_HIGH_SURROGATE || ch > MAX_HIGH_SURROGATE) {
            if (++codePointOffset > 0) {
                return index+1;
            }
            }
        }
        }
    } else {
        int limit = text.length();
        while (--codePointOffset >= 0) {
        char ch = text.charAt(index++);
        while (ch >= MIN_HIGH_SURROGATE && ch <= MAX_HIGH_SURROGATE && index < limit) {
            ch = text.charAt(index++);
            if (ch < MIN_LOW_SURROGATE || ch > MAX_LOW_SURROGATE) {
            if (--codePointOffset < 0) {
                return index-1;
            }
            }
        }
        }
    }

    return index;
    }

    /**
     * Cover the JDK API, for convenience.  Adjust the char index by a code point offset.
     * @param text the characters to check
     * @param start the start of the range to check
     * @param count the length of the range to check
     * @param index the index to adjust
     * @param codePointOffset the number of code points by which to offset the index
     * @return the adjusted index
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static int offsetByCodePoints(char[] text, int start, int count, int index, int codePointOffset) {
    int limit = start + count;
    if (start < 0 || limit < start || limit > text.length || index < start || index > limit) {
        throw new IndexOutOfBoundsException("index ( " + index +
                        ") out of range " + start +
                        ", " + limit +
                        " in array 0, " + text.length);
    }

    if (codePointOffset < 0) {
        while (++codePointOffset <= 0) {
        char ch = text[--index];
        if (index < start) {
            throw new IndexOutOfBoundsException("index ( " + index +
                            ") < start (" + start +
                            ")");
        }
        while (ch >= MIN_LOW_SURROGATE && ch <= MAX_LOW_SURROGATE && index > start) {
            ch = text[--index];
            if (ch < MIN_HIGH_SURROGATE || ch > MAX_HIGH_SURROGATE) {
            if (++codePointOffset > 0) {
                return index+1;
            }
            }
        }
        }
    } else {
        while (--codePointOffset >= 0) {
        char ch = text[index++];
        if (index > limit) {
            throw new IndexOutOfBoundsException("index ( " + index +
                            ") > limit (" + limit +
                            ")");
        }
        while (ch >= MIN_HIGH_SURROGATE && ch <= MAX_HIGH_SURROGATE && index < limit) {
            ch = text[index++];
            if (ch < MIN_LOW_SURROGATE || ch > MAX_LOW_SURROGATE) {
            if (--codePointOffset < 0) {
                return index-1;
            }
            }
        }
        }
    }

    return index;
    }

    // protected data members --------------------------------------------
    
    /**
     * Database storing the sets of character name
     */
    static UCharacterName NAME_ = null;

    /**
     * Singleton object encapsulating the imported pnames.icu property aliases
     */
    static UPropertyAliases PNAMES_ = null;
      
    // block to initialise name database and unicode 1.0 data 
    static
    {
        try
        {
        PNAMES_ = new UPropertyAliases();
        NAME_ = UCharacterName.getInstance();
        }
        catch (Exception e)
        {
        e.printStackTrace();
        //throw new RuntimeException(e.getMessage());
        // DONOT throw an exception
        // we might be building ICU modularly wothout names.icu and pnames.icu
        }
    }
        
    // private variables -------------------------------------------------
    
    /**
     * Database storing the sets of character property
     */
    private static final UCharacterProperty PROPERTY_;
    /**
     * For optimization
     */
    private static final char[] PROPERTY_TRIE_INDEX_;
    private static final char[] PROPERTY_TRIE_DATA_;
    private static final int[] PROPERTY_DATA_;
    private static final int PROPERTY_INITIAL_VALUE_;

    // block to initialise character property database
    static
    {
        try
        {
        PROPERTY_ = UCharacterProperty.getInstance();
        PROPERTY_TRIE_INDEX_ = PROPERTY_.m_trieIndex_;
        PROPERTY_TRIE_DATA_ = PROPERTY_.m_trieData_;
        PROPERTY_DATA_ = PROPERTY_.m_property_;
        PROPERTY_INITIAL_VALUE_ 
            = PROPERTY_DATA_[PROPERTY_.m_trieInitialValue_];
        }
        catch (Exception e)
        {
        throw new RuntimeException(e.getMessage());
        }
    }
    
    /**
     * To get the last character out from a data type
     */
    private static final int LAST_CHAR_MASK_ = 0xFFFF;
      
    /**
     * To get the last byte out from a data type
     */
    private static final int LAST_BYTE_MASK_ = 0xFF;
      
    /**
     * Shift 16 bits
     */
    private static final int SHIFT_16_ = 16;
      
    /**
     * Shift 24 bits
     */
    private static final int SHIFT_24_ = 24;  
    
    /**
     * Decimal radix
     */
    private static final int DECIMAL_RADIX_ = 10;
      
    /**
     * No break space code point
     */
    private static final int NO_BREAK_SPACE_ = 0xA0;
      
    /**
     * Narrow no break space code point
     */
    private static final int NARROW_NO_BREAK_SPACE_ = 0x202F;
      
    /**
     * Zero width no break space code point
     */
    private static final int ZERO_WIDTH_NO_BREAK_SPACE_ = 0xFEFF;
      
    /**
     * Ideographic number zero code point
     */
    private static final int IDEOGRAPHIC_NUMBER_ZERO_ = 0x3007;
            
    /**
     * CJK Ideograph, First code point
     */
    private static final int CJK_IDEOGRAPH_FIRST_ = 0x4e00;
      
    /**
     * CJK Ideograph, Second code point
     */
    private static final int CJK_IDEOGRAPH_SECOND_ = 0x4e8c;
            
    /**
     * CJK Ideograph, Third code point
     */
    private static final int CJK_IDEOGRAPH_THIRD_ = 0x4e09;
      
    /**
     * CJK Ideograph, Fourth code point
     */
    private static final int CJK_IDEOGRAPH_FOURTH_ = 0x56d8;
      
    /**
     * CJK Ideograph, FIFTH code point
     */
    private static final int CJK_IDEOGRAPH_FIFTH_ = 0x4e94;
      
    /**
     * CJK Ideograph, Sixth code point
     */
    private static final int CJK_IDEOGRAPH_SIXTH_ = 0x516d;
            
    /**
     * CJK Ideograph, Seventh code point
     */
    private static final int CJK_IDEOGRAPH_SEVENTH_ = 0x4e03;
      
    /**
     * CJK Ideograph, Eighth code point
     */
    private static final int CJK_IDEOGRAPH_EIGHTH_ = 0x516b;
      
    /**
     * CJK Ideograph, Nineth code point
     */
    private static final int CJK_IDEOGRAPH_NINETH_ = 0x4e5d;
      
    /**
     * Application Program command code point
     */
    private static final int APPLICATION_PROGRAM_COMMAND_ = 0x009F;
      
    /**
     * Unit separator code point
     */
    private static final int UNIT_SEPARATOR_ = 0x001F;
      
    /**
     * Delete code point
     */
    private static final int DELETE_ = 0x007F;
    /**
     * ISO control character first range upper limit 0x0 - 0x1F
     */
    private static final int ISO_CONTROL_FIRST_RANGE_MAX_ = 0x1F;
    /**
     * Shift to get numeric type
     */
    private static final int NUMERIC_TYPE_SHIFT_ = 12;
    /**
     * Mask to get numeric type
     */
    private static final int NUMERIC_TYPE_MASK_ = 0x7 << NUMERIC_TYPE_SHIFT_;
    /**
     * Shift to get bidi bits
     */
    private static final int BIDI_SHIFT_ = 6;
      
    /**
     * Mask to be applied after shifting to get bidi bits
     */
    private static final int BIDI_MASK_AFTER_SHIFT_ = 0x1F;
    /**
     * Han digit characters
     */
    private static final int CJK_IDEOGRAPH_COMPLEX_ZERO_     = 0x96f6;    
    private static final int CJK_IDEOGRAPH_COMPLEX_ONE_      = 0x58f9;    
    private static final int CJK_IDEOGRAPH_COMPLEX_TWO_      = 0x8cb3;    
    private static final int CJK_IDEOGRAPH_COMPLEX_THREE_    = 0x53c3;    
    private static final int CJK_IDEOGRAPH_COMPLEX_FOUR_     = 0x8086;    
    private static final int CJK_IDEOGRAPH_COMPLEX_FIVE_     = 0x4f0d;    
    private static final int CJK_IDEOGRAPH_COMPLEX_SIX_      = 0x9678;    
    private static final int CJK_IDEOGRAPH_COMPLEX_SEVEN_    = 0x67d2;    
    private static final int CJK_IDEOGRAPH_COMPLEX_EIGHT_    = 0x634c;    
    private static final int CJK_IDEOGRAPH_COMPLEX_NINE_     = 0x7396;    
    private static final int CJK_IDEOGRAPH_TEN_              = 0x5341;    
    private static final int CJK_IDEOGRAPH_COMPLEX_TEN_      = 0x62fe;    
    private static final int CJK_IDEOGRAPH_HUNDRED_          = 0x767e;    
    private static final int CJK_IDEOGRAPH_COMPLEX_HUNDRED_  = 0x4f70;    
    private static final int CJK_IDEOGRAPH_THOUSAND_         = 0x5343;    
    private static final int CJK_IDEOGRAPH_COMPLEX_THOUSAND_ = 0x4edf;    
    private static final int CJK_IDEOGRAPH_TEN_THOUSAND_     = 0x824c;    
    private static final int CJK_IDEOGRAPH_HUNDRED_MILLION_  = 0x5104;
    /**
     * <p>Numerator power limit.
     * There are special values for huge numbers that are powers of ten.</p>
     * <p>c version genprops/store.c documents:
     * if numericValue = 0x7fffff00 + x then numericValue = 10 ^ x</p>
     */
    private static final int NUMERATOR_POWER_LIMIT_ = 0x7fffff00;
    /**
     * Integer properties mask and shift values for joining type.
     * Equivalent to icu4c UPROPS_JT_MASK. 
     */    
    private static final int JOINING_TYPE_MASK_ = 0x00003800;
    /**
     * Integer properties mask and shift values for joining type.
     * Equivalent to icu4c UPROPS_JT_SHIFT. 
     */    
    private static final int JOINING_TYPE_SHIFT_ = 11;
    /**
     * Integer properties mask and shift values for joining group.
     * Equivalent to icu4c UPROPS_JG_MASK. 
     */    
    private static final int JOINING_GROUP_MASK_ = 0x000007e0;
    /**
     * Integer properties mask and shift values for joining group.
     * Equivalent to icu4c UPROPS_JG_SHIFT. 
     */    
    private static final int JOINING_GROUP_SHIFT_ = 5;
    /**
     * Integer properties mask for decomposition type.
     * Equivalent to icu4c UPROPS_DT_MASK. 
     */    
    private static final int DECOMPOSITION_TYPE_MASK_ = 0x0000001f;
    /**
     * Integer properties mask and shift values for East Asian cell width.
     * Equivalent to icu4c UPROPS_EA_MASK 
     */    
    private static final int EAST_ASIAN_MASK_ = 0x00038000;
    /**
     * Integer properties mask and shift values for East Asian cell width.
     * Equivalent to icu4c UPROPS_EA_SHIFT 
     */    
    private static final int EAST_ASIAN_SHIFT_ = 15;
    /**
     * Zero Width Non Joiner.
     * Equivalent to icu4c ZWNJ.
     */
    private static final int ZERO_WIDTH_NON_JOINER_ = 0x200c;
    /**
     * Zero Width Joiner
     * Equivalent to icu4c ZWJ. 
     */
    private static final int ZERO_WIDTH_JOINER_ = 0x200d;
    /**
     * Integer properties mask and shift values for line breaks.
     * Equivalent to icu4c UPROPS_LB_MASK 
     */    
    private static final int LINE_BREAK_MASK_ = 0x007C0000;
    /**
     * Integer properties mask and shift values for line breaks.
     * Equivalent to icu4c UPROPS_LB_SHIFT 
     */    
    private static final int LINE_BREAK_SHIFT_ = 18;
    /**
     * Integer properties mask and shift values for blocks.
     * Equivalent to icu4c UPROPS_BLOCK_MASK 
     */    
    private static final int BLOCK_MASK_ = 0x00007f80;
    /**
     * Integer properties mask and shift values for blocks.
     * Equivalent to icu4c UPROPS_BLOCK_SHIFT 
     */    
    private static final int BLOCK_SHIFT_ = 7;
    /**
     * Integer properties mask and shift values for scripts.
     * Equivalent to icu4c UPROPS_SHIFT_MASK
     */    
    private static final int SCRIPT_MASK_ = 0x0000007f;
                           
    // private constructor -----------------------------------------------
    ///CLOVER:OFF  
    /**
     * Private constructor to prevent instantiation
     */
    private UCharacter()
    {
    }
    ///CLOVER:ON 
    // private methods ---------------------------------------------------
    
    /**
     * Getting the digit values of characters like 'A' - 'Z', normal, 
     * half-width and full-width. This method assumes that the other digit 
     * characters are checked by the calling method.
     * @param ch character to test
     * @return -1 if ch is not a character of the form 'A' - 'Z', otherwise
     *         its corresponding digit will be returned.
     */
    private static int getEuropeanDigit(int ch) {
        if ((ch > 0x7a && ch < 0xff21)  
            || ch < 0x41 || (ch > 0x5a && ch < 0x61)
            || ch > 0xff5a || (ch > 0xff31 && ch < 0xff41)) {
            return -1;
        } 
        if (ch <= 0x7a) {
            // ch >= 0x41 or ch < 0x61 
            return ch + 10 - ((ch <= 0x5a) ? 0x41 : 0x61);
        }
        // ch >= 0xff21
        if (ch <= 0xff3a) {
            return ch + 10 - 0xff21;
        } 
        // ch >= 0xff41 && ch <= 0xff5a
        return ch + 10 - 0xff41;
    }
    
    /**
     * Gets the numeric type of the property argument
     * @param props 32 bit property
     * @return the numeric type
     */
    private static int getNumericType(int props)
    {
        return (props & NUMERIC_TYPE_MASK_) >> NUMERIC_TYPE_SHIFT_;
    }
    
    /**
     * Checks if the property value has a exception indicator
     * @param props 32 bit property value
     * @return true if property does not have a exception indicator, false
     *          otherwise
     */     
    private static boolean isNotExceptionIndicator(int props)
    {
    return (props & UCharacterProperty.EXCEPTION_MASK) == 0;
    }
         
    /**
     * Gets the property value at the index.
     * This is optimized.
     * Note this is alittle different from CharTrie the index m_trieData_
     * is never negative.
     * This is a duplicate of UCharacterProperty.getProperty. For optimization
     * purposes, this method calls the trie data directly instead of through 
     * UCharacterProperty.getProperty.
     * @param ch code point whose property value is to be retrieved
     * @return property value of code point
     * @stable ICU 2.6
     */
    private static int getProperty(int ch)
    {
        if (ch < UTF16.LEAD_SURROGATE_MIN_VALUE 
            || (ch > UTF16.LEAD_SURROGATE_MAX_VALUE 
                && ch < UTF16.SUPPLEMENTARY_MIN_VALUE)) {
            // BMP codepoint
            try { // using try for < 0 ch is faster than using an if statement
                return PROPERTY_DATA_[
                      PROPERTY_TRIE_DATA_[
                              (PROPERTY_TRIE_INDEX_[ch >> 5] << 2) 
                              + (ch & 0x1f)]];
            } catch (ArrayIndexOutOfBoundsException e) {
                return PROPERTY_INITIAL_VALUE_;
            }
        }
        if (ch <= UTF16.LEAD_SURROGATE_MAX_VALUE) {
            // surrogate 
            return PROPERTY_DATA_[
                  PROPERTY_TRIE_DATA_[
                              (PROPERTY_TRIE_INDEX_[(0x2800 >> 5) + (ch >> 5)] << 2) 
                              + (ch & 0x1f)]];
        }
        // for optimization
        if (ch <= UTF16.CODEPOINT_MAX_VALUE) {
            // look at the construction of supplementary characters
            // trail forms the ends of it.
            return PROPERTY_DATA_[PROPERTY_.m_trie_.getSurrogateValue(
                                      UTF16.getLeadSurrogate(ch), 
                                      (char)(ch & 0x3ff))];
        }
        // return m_dataOffset_ if there is an error, in this case we return 
        // the default value: m_initialValue_
        // we cannot assume that m_initialValue_ is at offset 0
        // this is for optimization.
        return PROPERTY_INITIAL_VALUE_;
    }
}

