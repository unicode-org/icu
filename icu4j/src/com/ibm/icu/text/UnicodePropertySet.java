/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Attic/UnicodePropertySet.java,v $
* $Date: 2002/06/06 16:27:56 $
* $Revision: 1.11 $
**********************************************************************
*/
package com.ibm.icu.text;

import java.text.*;
import java.util.*;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.*;

/**
 * INTERNAL CLASS implementing the UnicodeSet properties as outlined
 * at:
 *
 * http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/unicodeset_properties.html
 *
 * Recognized syntax:
 *
 * [:foo:] [:^foo:] - white space not allowed within "[:" or ":]"
 * \p{foo} \P{foo}  - white space not allowed within "\p" or "\P"
 *
 * Other than the above restrictions, white space is ignored.  Case
 * is ignored except in "\p" and "\P".
 *
 * This class cannot be instantiated.  It has a public static method,
 * createPropertySet(), with takes a pattern to be parsed and returns
 * a new UnicodeSet.  Another public static method,
 * resemblesPattern(), returns true if a given pattern string appears
 * to be a property set pattern, and therefore should be passed in to
 * createPropertySet().
 *
 * NOTE: Current implementation is incomplete.  The following list
 * indicates which properties are supported.
 *
 *    + GeneralCategory
 *    + CombiningClass
 *    + BidiClass
 *      DecompositionType
 *    + NumericValue
 *      NumericType
 *      EastAsianWidth
 *      LineBreak
 *      JoiningType
 *    + Script
 *      Binary properties
 *
 * '+' indicates a supported property.
 *
 * @author Alan Liu
 * @version $RCSfile: UnicodePropertySet.java,v $ $Revision: 1.11 $ $Date: 2002/06/06 16:27:56 $
 */
class UnicodePropertySet {

    private static Hashtable NAME_MAP = null;

    private static Hashtable CATEGORY_MAP = null;

    private static Hashtable COMBINING_CLASS_MAP = null;
    
    private static Hashtable BIDI_CLASS_MAP = null;
    
    private static Hashtable BINARY_PROPERTY_MAP = null;
    
    /**
     * A cache mapping character category integers, as returned by
     * UCharacter.getType(), to sets.  Entries are initially
     * null and are created on demand.
     */
    private static UnicodeSet[] CATEGORY_CACHE = null;

    /**
     * A cache mapping script integers, as defined by
     * UScript, to sets.  Entries are initially
     * null and are created on demand.
     */
    private static UnicodeSet[] SCRIPT_CACHE = null;

    // Special value codes
    private static final int ANY = -1; // general category: all code points

    // >From UnicodeData:
    // 3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
    // 4DB5;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
    // 4E00;<CJK Ideograph, First>;Lo;0;L;;;;;N;;;;;
    // 9FA5;<CJK Ideograph, Last>;Lo;0;L;;;;;N;;;;;
    // AC00;<Hangul Syllable, First>;Lo;0;L;;;;;N;;;;;
    // D7A3;<Hangul Syllable, Last>;Lo;0;L;;;;;N;;;;;
    // D800;<Non Private Use High Surrogate, First>;Cs;0;L;;;;;N;;;;;
    // DB7F;<Non Private Use High Surrogate, Last>;Cs;0;L;;;;;N;;;;;
    // DB80;<Private Use High Surrogate, First>;Cs;0;L;;;;;N;;;;;
    // DBFF;<Private Use High Surrogate, Last>;Cs;0;L;;;;;N;;;;;
    // DC00;<Low Surrogate, First>;Cs;0;L;;;;;N;;;;;
    // DFFF;<Low Surrogate, Last>;Cs;0;L;;;;;N;;;;;
    // E000;<Private Use, First>;Co;0;L;;;;;N;;;;;
    // F8FF;<Private Use, Last>;Co;0;L;;;;;N;;;;;
    // 20000;<CJK Ideograph Extension B, First>;Lo;0;L;;;;;N;;;;;
    // 2A6D6;<CJK Ideograph Extension B, Last>;Lo;0;L;;;;;N;;;;;
    // F0000;<Plane 15 Private Use, First>;Co;0;L;;;;;N;;;;;
    // FFFFD;<Plane 15 Private Use, Last>;Co;0;L;;;;;N;;;;;
    // 100000;<Plane 16 Private Use, First>;Co;0;L;;;;;N;;;;;
    // 10FFFD;<Plane 16 Private Use, Last>;Co;0;L;;;;;N;;;;;
    // 
    // >Large Blocks of Unassigned: (from DerivedGeneralCategory)
    // 1044E..1CFFF  ; Cn # [52146]
    // 1D800..1FFFF  ; Cn # [10240]
    // 2A6D7..2F7FF  ; Cn # [20777]
    // 2FA1E..E0000  ; Cn # [722403]
    // E0080..EFFFF  ; Cn # [65408]

    // TODO: The Inclusion List should be generated from the UCD for each
    // version, and thus should be accessed from the properties data file
    // (Even better: move the logic into UCharacter for building these
    // properties, since that is where it belongs!)

    /**
     * A set of all characters _except_ the second through last characters of
     * certain ranges.  These ranges are ranges of characters whose
     * properties are all exactly alike, e.g. CJK Ideographs from
     * U+4E00 to U+9FA5.
     */
    private static final UnicodeSet INCLUSIONS =
        new UnicodeSet("[^\\u3401-\\u4DB5 \\u4E01-\\u9FA5 \\uAC01-\\uD7A3 \\uD801-\\uDB7F \\uDB81-\\uDBFF \\uDC01-\\uDFFF \\uE001-\\uF8FF \\U0001044F-\\U0001CFFF \\U0001D801-\\U0001FFFF \\U00020001-\\U0002A6D6 \\U0002A6D8-\\U0002F7FF \\U0002FA1F-\\U000E0000 \\U000E0081-\\U000EFFFF \\U000F0001-\\U000FFFFD \\U00100001-\\U0010FFFD]");

    //----------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------

    /**
     * Return true if the given position, in the given pattern, appears
     * to be the start of a property set pattern [:foo:], \p{foo}, or
     * \P{foo}.
     */
    public static boolean resemblesPattern(String pattern, int pos) {
        // Patterns are at least 5 characters long
        if ((pos+5) > pattern.length()) {
            return false;
        }

        // Look for an opening [:, [:^, \p, or \P
        return pattern.regionMatches(pos, "[:", 0, 2) ||
            pattern.regionMatches(true, pos, "\\p", 0, 2);
    }

    /**
     * Create a UnicodeSet by parsing the given pattern at the given
     * parse position.
     *
     * @param pattern the pattern string
     * @param ppos on entry, the position at which to begin parsing.
     * This shold be one of the locations marked '^':
     *
     *   [:blah:]     \p{blah}     \P{blah}
     *   ^       %    ^       %    ^       %
     *
     * On return, the position after the last character parsed, that is,
     * the locations marked '%'.  If the parse fails, ppos is returned
     * unchanged.
     * @return a newly-constructed UnicodeSet object, or null upon
     * failure.
     */
    public static UnicodeSet createFromPattern(String pattern, ParsePosition ppos) {
        init();

        UnicodeSet set = null;

        int pos = ppos.getIndex();

        // On entry, ppos should point to one of the following locations:

        // Minimum length is 5 characters, e.g. \p{L}
        if ((pos+5) > pattern.length()) {
            return null;
        }

        boolean posix = false; // true for [:pat:], false for \p{pat} \P{pat}
        boolean invert = false;

        // Look for an opening [:, [:^, \p, or \P
        if (pattern.regionMatches(pos, "[:", 0, 2)) {
            posix = true;
            pos = Utility.skipWhitespace(pattern, pos+2);
            if (pos < pattern.length() && pattern.charAt(pos) == '^') {
                ++pos;
                invert = true;
            }
        } else if (pattern.regionMatches(true, pos, "\\p", 0, 2)) {
            invert = (pattern.charAt(pos+1) == 'P');
            pos = Utility.skipWhitespace(pattern, pos+2);
            if (pos == pattern.length() || pattern.charAt(pos++) != '{') {
                // Syntax error; "\p" or "\P" not followed by "{"
                return null;
            }
        } else {
            // Open delimiter not seen
            return null;
        }

        // Look for the matching close delimiter, either :] or }
        int close = pattern.indexOf(posix ? ":]" : "}", pos);
        if (close < 0) {
            // Syntax error; close delimiter missing
            return null;
        }

        // Look for an '=' sign.  If this is present, we will parse a
        // medium \p{gc=Cf} or long \p{GeneralCategory=Format}
        // pattern.
        int equals = pattern.indexOf('=', pos);
        if (equals >= 0 && equals < close) {
            // Equals seen; parse medium/long pattern
            String typeName = munge(pattern, pos, equals);
            String valueName = munge(pattern, equals+1, close);
            SetFactory factory;
            factory = (SetFactory) NAME_MAP.get(typeName);
            if (factory == null) {
                // Syntax error; type name not recognized
                return null;
            }
            set = factory.create(valueName);
        } else {
            // No equals seen; parse short format \p{Cf}
            String shortName = munge(pattern, pos, close);

            // First try general category
            set = createCategorySet(shortName);

            // If this fails, try script
            if (set == null) {
                set = createScriptSet(shortName);
            }

            /* TODO Enable later
            // If this fails, try binary property
            if (set == null) {
                set = createBinaryPropertySet(shortName);
            }
            */
        }

        if (invert) {
            set.complement();
        }

        // Move to the limit position after the close delimiter
        ppos.setIndex(close + (posix ? 2 : 1));

        return set;
    }

    //----------------------------------------------------------------
    // Property set factory classes
    // NOTE: This will change/go away when we implement UCharacter
    // based property retrieval.
    //----------------------------------------------------------------

    static interface SetFactory {

        UnicodeSet create(String valueName);
    }

    static class NumericValueFactory implements SetFactory {
        NumericValueFactory() {}
        public UnicodeSet create(String valueName) {
            double value = Double.parseDouble(valueName);
            final int ivalue = (int) value;
            if (ivalue != value || ivalue < 0) {
                // UCharacter doesn't support negative or non-integral
                // values, so just return an empty set
                return new UnicodeSet();
            }
            return createSetFromFilter(new Filter() {
                public boolean contains(int cp) {
                    return UCharacter.getUnicodeNumericValue(cp) == ivalue;
                }
            });
        }
    }

    static class CombiningClassFactory implements SetFactory {
        CombiningClassFactory() {}
        public UnicodeSet create(String valueName) {
            int ivalue = -1;
            try {
                ivalue = Integer.parseInt(valueName);
            } catch (NumberFormatException e) {}
            if (ivalue < 0) {
                // We have a negative value, or non-numeric text.
                // Try to lookup a symbolic combining class name
                Integer obj = (Integer) COMBINING_CLASS_MAP.get(valueName);
                if (obj != null) {
                    ivalue = obj.intValue();
                }
            }
            final int fivalue = ivalue;
            return createSetFromFilter(new Filter() {
                public boolean contains(int cp) {
                    return UCharacter.getCombiningClass(cp) == fivalue;
                }
            });
        }
    }

    static class BidiClassFactory implements SetFactory {
        BidiClassFactory() {}
        public UnicodeSet create(String valueName) {
            Integer obj = (Integer) BIDI_CLASS_MAP.get(valueName);
            if (obj == null) {
                return null;
            }
            final int ivalue = obj.intValue();
            return createSetFromFilter(new Filter() {
                public boolean contains(int cp) {
                    return UCharacter.getDirection(cp) == ivalue;
                }
            });
        }
    }

    //----------------------------------------------------------------
    // Property set factory static methods
    // NOTE: This will change/go away when we implement UCharacter
    // based property retrieval.
    //----------------------------------------------------------------

    /**
     * Given a general category value name, create a corresponding
     * set and return it, or return null if the name is invalid.
     * @param valueName a pre-munged general category value name
     */
    private static UnicodeSet createCategorySet(String valueName) {
        Integer valueObj;
        valueObj = (Integer) CATEGORY_MAP.get(valueName);
        if (valueObj == null) {
            return null;
        }
        int valueCode = valueObj.intValue();

        UnicodeSet set = new UnicodeSet();
        if (valueCode == ANY) {
            set.complement();
            return set;
        }
        for (int cat=0; cat<UCharacterCategory.CHAR_CATEGORY_COUNT; ++cat) {
            if ((valueCode & (1 << cat)) != 0) {
                set.addAll(UnicodePropertySet.getCategorySet(cat));
            }
        }
        return set;
    }

    /**
     * Given a script value name, create a corresponding set and
     * return it, or return null if the name is invalid.
     * @param valueName a pre-munged script value name
     */
    private static UnicodeSet createScriptSet(String valueName) {
        int[] script = UScript.getCode(valueName);
        if (script == null) {
            // Syntax error; unknown short name
            return null;
        }
        return new UnicodeSet(getScriptSet(script[0]));
    }

// TODO Enable when API support exists for binary props
//
//  static UBool _binaryPropertyFilter(UChar32 c, void* context) {
//      int32_t code = * (int32_t*) context;
//      return u_hasBinaryProperty(c, (UProperty) code);
//  }
//  
//  /**
//   * Given a binary property name, create a corresponding
//   * set and return it, or return null if the name is invalid.
//   * @param valueName a pre-munged binary property name
//   */
//  UnicodeSet* UnicodePropertySet::createBinaryPropertySet(const UnicodeString& name) {
//      int32_t code = BINARY_PROPERTY_MAP->geti(name) - MAPVAL;
//      if (code < 0) {
//          return NULL;
//      }
//      
//      UnicodeSet* set = new UnicodeSet(); 
//      initSetFromFilter(*set, _binaryPropertyFilter, &code);
//      return set;
//  }

    //----------------------------------------------------------------
    // Utility methods
    //----------------------------------------------------------------

    /**
     * Returns a UnicodeSet for the given category.  This set is
     * cached and returned again if this method is called again with
     * the same parameter.
     *
     * Callers MUST NOT MODIFY the returned set.
     */
    private static UnicodeSet getCategorySet(final int cat) {
        if (CATEGORY_CACHE[cat] == null) {
            CATEGORY_CACHE[cat] =
                createSetFromFilter(new Filter() {
                    public boolean contains(int cp) {
                        return UCharacter.getType(cp) == cat;
                    }
                });
        }
        return CATEGORY_CACHE[cat];
    }

    /**
     * Returns a UnicodeSet for the given script.  This set is
     * cached and returned again if this method is called again with
     * the same parameter.
     *
     * Callers MUST NOT MODIFY the returned set.
     */
    private static UnicodeSet getScriptSet(final int script) {
        if (SCRIPT_CACHE[script] == null) {
            SCRIPT_CACHE[script] =
                createSetFromFilter(new Filter() {
                    public boolean contains(int cp) {
                        return UScript.getScript(cp) == script;
                    }
                });
        }
        return SCRIPT_CACHE[script];
    }

    /**
     * Given a string, munge it to lose the whitespace, underscores,
     * and hyphens.  So "General Category " or "General_Category" or "
     * General-Category" become "GeneralCategory". We munge all type
     * and value strings, and store all type and value keys
     * pre-munged.
     */
    private static String munge(String str, int start, int limit) {
        StringBuffer buf = new StringBuffer();
        for (int i=start; i<limit; ) {
            int c = UTF16.charAt(str, i);
            i += UTF16.getCharCount(c);
            if (c != '_' && c != '-' && !UCharacter.isWhitespace(c)) {
                UTF16.append(buf, UCharacter.toUpperCase(c));
            }
        }
        return buf.toString();
    }

    //----------------------------------------------------------------
    // Generic filter-based scanning code
    //
    // NOTE: In general, we don't want to do this!  This is a temporary
    // implementation until we have time for something that examines
    // the underlying UCharacter data structures in an intelligent
    // way.  Iterating over all code points is dumb.  What we want to
    // do, for instance, is iterate over internally-stored ranges
    // of characters that have a given property.
    //----------------------------------------------------------------

    static interface Filter {
        boolean contains(int codePoint);
    }

    static UnicodeSet createSetFromFilter(Filter filter) {
        // Walk through all Unicode characters, noting the start
        // and end of each range for which filter.contain(c) is
        // true.  Add each range to a set.
        //
        // To improve performance, use the INCLUSIONS set, which
        // encodes information about character ranges that are known
        // to have identical properties, such as the CJK Ideographs
        // from U+4E00 to U+9FA5.  INCLUSIONS contains all characters
        // except the first characters of such ranges.
        //
        // TODO Where possible, instead of scanning over code points,
        // use internal property data to initialize UnicodeSets for
        // those properties.  Scanning code points is slow.

        UnicodeSet set = new UnicodeSet();
        int startHasProperty = -1;
        int limitRange = INCLUSIONS.getRangeCount();

        for (int j=0; j<limitRange; ++j) {
            // get current range
            int start = INCLUSIONS.getRangeStart(j);
            int end = INCLUSIONS.getRangeEnd(j);

            // for all the code points in the range, process
            for (int ch = start; ch <= end; ++ch) {
                // only add to the unicodeset on inflection points --
                // where the hasProperty value changes to false
                if (filter.contains(ch)) {
                    if (startHasProperty < 0) {
                        startHasProperty = ch;
                    }
                } else if (startHasProperty >= 0) {
                    set.add(startHasProperty, ch-1);
                    startHasProperty = -1;
                }
            }
        }
        if (startHasProperty >= 0) {
            set.add(startHasProperty, 0x10FFFF);
        }

        return set;
    }

    //----------------------------------------------------------------
    // Type and value name maps
    //----------------------------------------------------------------

    /**
     * Add a type mapping to the name map.
     */
    private static void addType(String shortName, String longName,
                                SetFactory factory) {
        // DEBUGGING CODE: DISABLE FOR PRODUCTION BUILD
        if (false) {
            if (NAME_MAP.get(shortName) != null) {
                throw new InternalError("Duplicate name " + shortName);
            }
            if (NAME_MAP.get(longName) != null) {
                throw new InternalError("Duplicate name " + longName);
            }
        }

        NAME_MAP.put(shortName, factory);
        NAME_MAP.put(longName, factory);
    }

    /**
     * Add a value mapping to the name map.
     */
    private static void addValue(Hashtable map,
                                 String shortName, String longName,
                                 int value) {
        // DEBUGGING CODE: DISABLE FOR PRODUCTION BUILD
        if (false) {
            if (map.get(shortName) != null) {
                throw new InternalError("Duplicate name " + shortName);
            }
            if (longName != null && map.get(longName) != null) {
                throw new InternalError("Duplicate name " + longName);
            }
        }

        Integer valueObj = new Integer(value);
        map.put(shortName, valueObj);
        if (longName != null) {
            map.put(longName, valueObj);
        }
    }

    static void init() {
        if (NAME_MAP != null) {
            return;
        }
        
        NAME_MAP = new Hashtable();
        CATEGORY_MAP = new Hashtable();
        CATEGORY_CACHE = new UnicodeSet[UCharacterCategory.CHAR_CATEGORY_COUNT];
        COMBINING_CLASS_MAP = new Hashtable();
        BIDI_CLASS_MAP = new Hashtable();
        BINARY_PROPERTY_MAP = new Hashtable();
        SCRIPT_CACHE = new UnicodeSet[UScript.CODE_LIMIT];

        // NOTE:  We munge all search keys to have no whitespace
        // and upper case.  As such, all stored keys should have
        // this format.

        //------------------------------------------------------------
        // MAIN KEY MAP

        addType("GC", "GENERALCATEGORY", new SetFactory() {
            public UnicodeSet create(String valueName) {
                return createCategorySet(valueName);
            }
        });

        addType("CC", "COMBININGCLASS", new CombiningClassFactory());

        addType("BC", "BIDICLASS", new BidiClassFactory());

        //addType("DT", "DECOMPOSITIONTYPE", DECOMPOSITION_TYPE);

        addType("NV", "NUMERICVALUE", new NumericValueFactory());

        //addType("NT", "NUMERICTYPE", NUMERIC_TYPE);
        //addType("EA", "EASTASIANWIDTH", EAST_ASIAN_WIDTH);
        //addType("LB", "LINEBREAK", LINE_BREAK);
        //addType("JT", "JOININGTYPE", JOINING_TYPE);

        addType("SC", "SCRIPT", new SetFactory() {
            public UnicodeSet create(String valueName) {
                return createScriptSet(valueName);
            }
        });

        //------------------------------------------------------------
        // General Category MAP

        addValue(CATEGORY_MAP, "ANY", null, ANY); // special case

        addValue(CATEGORY_MAP, "C", "OTHER",
                 (1 << UCharacterCategory.CONTROL) |
                 (1 << UCharacterCategory.FORMAT) |
                 (1 << UCharacterCategory.GENERAL_OTHER_TYPES) |
                 (1 << UCharacterCategory.PRIVATE_USE) |
                 (1 << UCharacterCategory.SURROGATE));

        addValue(CATEGORY_MAP, "CC", "CONTROL",
                 1 << UCharacterCategory.CONTROL);
        addValue(CATEGORY_MAP, "CF", "FORMAT",
                 1 << UCharacterCategory.FORMAT);
        addValue(CATEGORY_MAP, "CN", "UNASSIGNED",
                 1 << UCharacterCategory.GENERAL_OTHER_TYPES);
        addValue(CATEGORY_MAP, "CO", "PRIVATEUSE",
                 1 << UCharacterCategory.PRIVATE_USE);
        addValue(CATEGORY_MAP, "CS", "SURROGATE",
                 1 << UCharacterCategory.SURROGATE);

        addValue(CATEGORY_MAP, "L", "LETTER",
                 (1 << UCharacterCategory.LOWERCASE_LETTER) |
                 (1 << UCharacterCategory.MODIFIER_LETTER) |
                 (1 << UCharacterCategory.OTHER_LETTER) |
                 (1 << UCharacterCategory.TITLECASE_LETTER) |
                 (1 << UCharacterCategory.UPPERCASE_LETTER));

        addValue(CATEGORY_MAP, "LL", "LOWERCASELETTER",
                 1 << UCharacterCategory.LOWERCASE_LETTER);
        addValue(CATEGORY_MAP, "LM", "MODIFIERLETTER",
                 1 << UCharacterCategory.MODIFIER_LETTER);
        addValue(CATEGORY_MAP, "LO", "OTHERLETTER",
                 1 << UCharacterCategory.OTHER_LETTER);
        addValue(CATEGORY_MAP, "LT", "TITLECASELETTER",
                 1 << UCharacterCategory.TITLECASE_LETTER);
        addValue(CATEGORY_MAP, "LU", "UPPERCASELETTER",
                 1 << UCharacterCategory.UPPERCASE_LETTER);

        addValue(CATEGORY_MAP, "M", "MARK",
                 (1 << UCharacterCategory.NON_SPACING_MARK) |
                 (1 << UCharacterCategory.COMBINING_SPACING_MARK) |
                 (1 << UCharacterCategory.ENCLOSING_MARK));

        addValue(CATEGORY_MAP, "MN", "NONSPACINGMARK",
                 1 << UCharacterCategory.NON_SPACING_MARK);
        addValue(CATEGORY_MAP, "MC", "SPACINGMARK",
                 1 << UCharacterCategory.COMBINING_SPACING_MARK);
        addValue(CATEGORY_MAP, "ME", "ENCLOSINGMARK",
                 1 << UCharacterCategory.ENCLOSING_MARK);

        addValue(CATEGORY_MAP, "N", "NUMBER",
                 (1 << UCharacterCategory.DECIMAL_DIGIT_NUMBER) |
                 (1 << UCharacterCategory.LETTER_NUMBER) |
                 (1 << UCharacterCategory.OTHER_NUMBER));

        addValue(CATEGORY_MAP, "ND", "DECIMALNUMBER",
                 1 << UCharacterCategory.DECIMAL_DIGIT_NUMBER);
        addValue(CATEGORY_MAP, "NL", "LETTERNUMBER",
                 1 << UCharacterCategory.LETTER_NUMBER);
        addValue(CATEGORY_MAP, "NO", "OTHERNUMBER",
                 1 << UCharacterCategory.OTHER_NUMBER);

        addValue(CATEGORY_MAP, "P", "PUNCTUATION",
                 (1 << UCharacterCategory.CONNECTOR_PUNCTUATION) |
                 (1 << UCharacterCategory.DASH_PUNCTUATION) |
                 (1 << UCharacterCategory.END_PUNCTUATION) |
                 (1 << UCharacterCategory.FINAL_PUNCTUATION) |
                 (1 << UCharacterCategory.INITIAL_PUNCTUATION) |
                 (1 << UCharacterCategory.OTHER_PUNCTUATION) |
                 (1 << UCharacterCategory.START_PUNCTUATION));

        addValue(CATEGORY_MAP, "PC", "CONNECTORPUNCTUATION",
                 1 << UCharacterCategory.CONNECTOR_PUNCTUATION);
        addValue(CATEGORY_MAP, "PD", "DASHPUNCTUATION",
                 1 << UCharacterCategory.DASH_PUNCTUATION);
        addValue(CATEGORY_MAP, "PE", "ENDPUNCTUATION",
                 1 << UCharacterCategory.END_PUNCTUATION);
        addValue(CATEGORY_MAP, "PF", "FINALPUNCTUATION",
                 1 << UCharacterCategory.FINAL_PUNCTUATION);
        addValue(CATEGORY_MAP, "PI", "INITIALPUNCTUATION",
                 1 << UCharacterCategory.INITIAL_PUNCTUATION);
        addValue(CATEGORY_MAP, "PO", "OTHERPUNCTUATION",
                 1 << UCharacterCategory.OTHER_PUNCTUATION);
        addValue(CATEGORY_MAP, "PS", "STARTPUNCTUATION",
                 1 << UCharacterCategory.START_PUNCTUATION);

        addValue(CATEGORY_MAP, "S", "SYMBOL",
                 (1 << UCharacterCategory.CURRENCY_SYMBOL) |
                 (1 << UCharacterCategory.MODIFIER_SYMBOL) |
                 (1 << UCharacterCategory.MATH_SYMBOL) |
                 (1 << UCharacterCategory.OTHER_SYMBOL));

        addValue(CATEGORY_MAP, "SC", "CURRENCYSYMBOL",
                 1 << UCharacterCategory.CURRENCY_SYMBOL);
        addValue(CATEGORY_MAP, "SK", "MODIFIERSYMBOL",
                 1 << UCharacterCategory.MODIFIER_SYMBOL);
        addValue(CATEGORY_MAP, "SM", "MATHSYMBOL",
                 1 << UCharacterCategory.MATH_SYMBOL);
        addValue(CATEGORY_MAP, "SO", "OTHERSYMBOL",
                 1 << UCharacterCategory.OTHER_SYMBOL);

        addValue(CATEGORY_MAP, "Z", "SEPARATOR",
                 (1 << UCharacterCategory.LINE_SEPARATOR) |
                 (1 << UCharacterCategory.PARAGRAPH_SEPARATOR) |
                 (1 << UCharacterCategory.SPACE_SEPARATOR));

        addValue(CATEGORY_MAP, "ZL", "LINESEPARATOR",
                 1 << UCharacterCategory.LINE_SEPARATOR);
        addValue(CATEGORY_MAP, "ZP", "PARAGRAPHSEPARATOR",
                 1 << UCharacterCategory.PARAGRAPH_SEPARATOR);
        addValue(CATEGORY_MAP, "ZS", "SPACESEPARATOR",
                 1 << UCharacterCategory.SPACE_SEPARATOR);

        //------------------------------------------------------------
        // Combining Class MAP

        addValue(COMBINING_CLASS_MAP, "NR", "NOTREORDERED", 0);
        addValue(COMBINING_CLASS_MAP, "OV", "OVERLAYSINTERIORS", 1);
        addValue(COMBINING_CLASS_MAP, "NU", "NUKTAS", 7);
        addValue(COMBINING_CLASS_MAP, "KV", "KANAVOICINGMARKS", 8);
        addValue(COMBINING_CLASS_MAP, "V", "VIRAMAS", 9);
        addValue(COMBINING_CLASS_MAP, "BLA", "BELOWLEFTATTACHED", 200);
        addValue(COMBINING_CLASS_MAP, "BA", "BELOWATTACHED", 202);
        addValue(COMBINING_CLASS_MAP, "BRA", "BELOWRIGHTATTACHED", 204);
        addValue(COMBINING_CLASS_MAP, "LAR", "LEFTATTACHEDREORDRANT", 208);
        addValue(COMBINING_CLASS_MAP, "RA", "RIGHTATTACHED", 210);
        addValue(COMBINING_CLASS_MAP, "ALA", "ABOVELEFTATTACHED", 212);
        addValue(COMBINING_CLASS_MAP, "AA", "ABOVEATTACHED", 214);
        addValue(COMBINING_CLASS_MAP, "ARA", "ABOVERIGHTATTACHED", 216);
        addValue(COMBINING_CLASS_MAP, "BL", "BELOWLEFT", 218);
        addValue(COMBINING_CLASS_MAP, "B", "BELOW", 220);
        addValue(COMBINING_CLASS_MAP, "BR", "BELOWRIGHT", 222);
        addValue(COMBINING_CLASS_MAP, "L", "LEFT", 224);
        addValue(COMBINING_CLASS_MAP, "R", "RIGHT", 226);
        addValue(COMBINING_CLASS_MAP, "AL", "ABOVELEFT", 228);
        addValue(COMBINING_CLASS_MAP, "A", "ABOVE", 230);
        addValue(COMBINING_CLASS_MAP, "AR", "ABOVERIGHT", 232);
        addValue(COMBINING_CLASS_MAP, "DB", "DOUBLEBELOW", 232);
        addValue(COMBINING_CLASS_MAP, "DA", "DOUBLEABOVE", 234);
        addValue(COMBINING_CLASS_MAP, "IS", "IOTASUBSCRIPT", 240);

        //------------------------------------------------------------
        // Bidi Class MAP

        addValue(BIDI_CLASS_MAP, "L", "LEFTTORIGHT", UCharacterDirection.LEFT_TO_RIGHT); 
        addValue(BIDI_CLASS_MAP, "R", "RIGHTTOLEFT", UCharacterDirection.RIGHT_TO_LEFT); 
        addValue(BIDI_CLASS_MAP, "EN", "EUROPEANNUMBER", UCharacterDirection.EUROPEAN_NUMBER);
        addValue(BIDI_CLASS_MAP, "ES", "EUROPEANNUMBERSEPARATOR", UCharacterDirection.EUROPEAN_NUMBER_SEPARATOR);
        addValue(BIDI_CLASS_MAP, "ET", "EUROPEANNUMBERTERMINATOR", UCharacterDirection.EUROPEAN_NUMBER_TERMINATOR);
        addValue(BIDI_CLASS_MAP, "AN", "ARABICNUMBER", UCharacterDirection.ARABIC_NUMBER);
        addValue(BIDI_CLASS_MAP, "CS", "COMMONNUMBERSEPARATOR", UCharacterDirection.COMMON_NUMBER_SEPARATOR);
        addValue(BIDI_CLASS_MAP, "B", "BLOCKSEPARATOR", UCharacterDirection.BLOCK_SEPARATOR);
        addValue(BIDI_CLASS_MAP, "S", "SEGMENTSEPARATOR", UCharacterDirection.SEGMENT_SEPARATOR);
        addValue(BIDI_CLASS_MAP, "WS", "WHITESPACENEUTRAL", UCharacterDirection.WHITE_SPACE_NEUTRAL); 
        addValue(BIDI_CLASS_MAP, "ON", "OTHERNEUTRAL", UCharacterDirection.OTHER_NEUTRAL); 
        addValue(BIDI_CLASS_MAP, "LRE", "LEFTTORIGHTEMBEDDING", UCharacterDirection.LEFT_TO_RIGHT_EMBEDDING);
        addValue(BIDI_CLASS_MAP, "LRO", "LEFTTORIGHTOVERRIDE", UCharacterDirection.LEFT_TO_RIGHT_OVERRIDE);
        addValue(BIDI_CLASS_MAP, "AL", "RIGHTTOLEFTARABIC", UCharacterDirection.RIGHT_TO_LEFT_ARABIC);
        addValue(BIDI_CLASS_MAP, "RLE", "RIGHTTOLEFTEMBEDDING", UCharacterDirection.RIGHT_TO_LEFT_EMBEDDING);
        addValue(BIDI_CLASS_MAP, "RLO", "RIGHTTOLEFTOVERRIDE", UCharacterDirection.RIGHT_TO_LEFT_OVERRIDE);
        addValue(BIDI_CLASS_MAP, "PDF", "POPDIRECTIONALFORMAT", UCharacterDirection.POP_DIRECTIONAL_FORMAT);
        addValue(BIDI_CLASS_MAP, "NSM", "DIRNONSPACINGMARK", UCharacterDirection.DIR_NON_SPACING_MARK);
        addValue(BIDI_CLASS_MAP, "BN", "BOUNDARYNEUTRAL", UCharacterDirection.BOUNDARY_NEUTRAL);

        //------------------------------------------------------------
        // Binary Properties MAP

        /* TODO Enable when this API exists in icu4j, possible ICU 2.4

        addValue(BINARY_PROPERTY_MAP, "ALPH", "ALPHABETIC", UCHAR_ALPHABETIC);
        addValue(BINARY_PROPERTY_MAP, "AHEXD", "ASCII_HEXDIGIT", UCHAR_ASCII_HEX_DIGIT);
        addValue(BINARY_PROPERTY_MAP, "BDCON", "BIDICONTROL", UCHAR_BIDI_CONTROL);
        addValue(BINARY_PROPERTY_MAP, "BDMIR", "BIDIMIRRORED", UCHAR_BIDI_MIRRORED);
        addValue(BINARY_PROPERTY_MAP, "DASH", "", UCHAR_DASH);
        addValue(BINARY_PROPERTY_MAP, "DICP", "DEFAULTIGNORABLECODEPOINT", UCHAR_DEFAULT_IGNORABLE_CODE_POINT);
        addValue(BINARY_PROPERTY_MAP, "DEPR", "DEPRECATED", UCHAR_DEPRECATED);
        addValue(BINARY_PROPERTY_MAP, "DIAC", "DIACRITIC", UCHAR_DIACRITIC);
        addValue(BINARY_PROPERTY_MAP, "EXT", "EXTENDER", UCHAR_EXTENDER);
        addValue(BINARY_PROPERTY_MAP, "FCE", "FULLCOMPOSITIONEXCLUSION", UCHAR_FULL_COMPOSITION_EXCLUSION);
        addValue(BINARY_PROPERTY_MAP, "GBASE", "GRAPHEMEBASE", UCHAR_GRAPHEME_BASE);
        addValue(BINARY_PROPERTY_MAP, "GEXT", "GRAPHEMEEXTEND", UCHAR_GRAPHEME_EXTEND);
        addValue(BINARY_PROPERTY_MAP, "GLINK", "GRAPHEMELINK", UCHAR_GRAPHEME_LINK);
        addValue(BINARY_PROPERTY_MAP, "HEXD", "HEXDIGIT", UCHAR_HEX_DIGIT);
        addValue(BINARY_PROPERTY_MAP, "HYPH", "HYPHEN", UCHAR_HYPHEN);
        addValue(BINARY_PROPERTY_MAP, "IDC", "IDCONTINUE", UCHAR_ID_CONTINUE);
        addValue(BINARY_PROPERTY_MAP, "IDS", "IDSTART", UCHAR_ID_START);
        addValue(BINARY_PROPERTY_MAP, "IDEO", "IDEOGRAPHIC", UCHAR_IDEOGRAPHIC);
        addValue(BINARY_PROPERTY_MAP, "IDSB", "IDSBINARYOPERATOR", UCHAR_IDS_BINARY_OPERATOR);
        addValue(BINARY_PROPERTY_MAP, "IDST", "IDSTRINARYOPERATOR", UCHAR_IDS_TRINARY_OPERATOR);
        addValue(BINARY_PROPERTY_MAP, "JCON", "JOINCONTROL", UCHAR_JOIN_CONTROL);
        addValue(BINARY_PROPERTY_MAP, "LOE", "LOGICALORDEREXCEPTION", UCHAR_LOGICAL_ORDER_EXCEPTION);
        addValue(BINARY_PROPERTY_MAP, "LC", "LOWERCASE", UCHAR_LOWERCASE);
        addValue(BINARY_PROPERTY_MAP, "MATH", "", UCHAR_MATH);
        addValue(BINARY_PROPERTY_MAP, "NCHAR", "NONCHARACTERCODEPOINT", UCHAR_NONCHARACTER_CODE_POINT);
        addValue(BINARY_PROPERTY_MAP, "QUOT", "QUOTATIONMARK", UCHAR_QUOTATION_MARK);
        addValue(BINARY_PROPERTY_MAP, "RAD", "RADICAL", UCHAR_RADICAL);
        addValue(BINARY_PROPERTY_MAP, "SOFT", "SOFTDOTTED", UCHAR_SOFT_DOTTED);
        addValue(BINARY_PROPERTY_MAP, "TERM", "TERMINALPUNCTUATION", UCHAR_TERMINAL_PUNCTUATION);
        addValue(BINARY_PROPERTY_MAP, "UID", "UNIFIEDIDEOGRAPH", UCHAR_UNIFIED_IDEOGRAPH);
        addValue(BINARY_PROPERTY_MAP, "UC", "UPPERCASE", UCHAR_UPPERCASE);
        addValue(BINARY_PROPERTY_MAP, "SPAC", "WHITESPACE", UCHAR_WHITE_SPACE);
        addValue(BINARY_PROPERTY_MAP, "XIDC", "XIDCONTINUE", UCHAR_XID_CONTINUE);
        addValue(BINARY_PROPERTY_MAP, "XIDS", "XIDSTART", UCHAR_XID_START);
        */
    }
}
