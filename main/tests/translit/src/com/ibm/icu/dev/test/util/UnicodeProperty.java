/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.CollectionUtilities.InverseMatcher;
import com.ibm.icu.dev.test.util.CollectionUtilities.ObjectMatcher;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.SymbolTable;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeMatcher;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public abstract class UnicodeProperty extends UnicodeLabel {

    public static boolean DEBUG = false;

    public static String CHECK_NAME = "FC_NFKC_Closure";

    public static int CHECK_VALUE = 0x037A;

    private String name;

    private String firstNameAlias = null;

    private int type;

    private Map valueToFirstValueAlias = null;

    /*
     * Name: Unicode_1_Name Name: ISO_Comment Name: Name Name: Unicode_1_Name
     * 
     */

    public static final int UNKNOWN = 0, BINARY = 2, EXTENDED_BINARY = 3,
            ENUMERATED = 4, EXTENDED_ENUMERATED = 5, CATALOG = 6,
            EXTENDED_CATALOG = 7, MISC = 8, EXTENDED_MISC = 9, STRING = 10,
            EXTENDED_STRING = 11, NUMERIC = 12, EXTENDED_NUMERIC = 13,
            START_TYPE = 2, LIMIT_TYPE = 14, EXTENDED_MASK = 1,
            CORE_MASK = ~EXTENDED_MASK, BINARY_MASK = (1 << BINARY)
                    | (1 << EXTENDED_BINARY), STRING_MASK = (1 << STRING)
                    | (1 << EXTENDED_STRING),
            STRING_OR_MISC_MASK = (1 << STRING) | (1 << EXTENDED_STRING)
                    | (1 << MISC) | (1 << EXTENDED_MISC),
            ENUMERATED_OR_CATALOG_MASK = (1 << ENUMERATED)
                    | (1 << EXTENDED_ENUMERATED) | (1 << CATALOG)
                    | (1 << EXTENDED_CATALOG);

    private static final String[] TYPE_NAMES = { "Unknown", "Unknown",
            "Binary", "Extended Binary", "Enumerated", "Extended Enumerated",
            "Catalog", "Extended Catalog", "Miscellaneous",
            "Extended Miscellaneous", "String", "Extended String", "Numeric",
            "Extended Numeric", };

    public static String getTypeName(int propType) {
        return TYPE_NAMES[propType];
    }

    public final String getName() {
        return name;
    }

    public final int getType() {
        return type;
    }

    public final boolean isType(int mask) {
        return ((1 << type) & mask) != 0;
    }

    protected final void setName(String string) {
        if (string == null)
            throw new IllegalArgumentException("Name must not be null");
        name = string;
    }

    protected final void setType(int i) {
        type = i;
    }

    public String getVersion() {
        return _getVersion();
    }

    public String getValue(int codepoint) {
        if (DEBUG && CHECK_VALUE == codepoint && CHECK_NAME.equals(getName())) {
            String value = _getValue(codepoint);
            System.out.println(getName() + "(" + Utility.hex(codepoint) + "):"
                    + (getType() == STRING ? Utility.hex(value) : value));
            return value;
        }
        return _getValue(codepoint);
    }

    // public String getValue(int codepoint, boolean isShort) {
    // return getValue(codepoint);
    // }

    public List getNameAliases(List result) {
        if (result == null)
            result = new ArrayList(1);
        return _getNameAliases(result);
    }

    public List getValueAliases(String valueAlias, List result) {
        if (result == null)
            result = new ArrayList(1);
        result = _getValueAliases(valueAlias, result);
        if (!result.contains(valueAlias)) { // FIX && type < NUMERIC
            result = _getValueAliases(valueAlias, result); // for debugging
            throw new IllegalArgumentException("Internal error: " + getName()
                    + " doesn't contain " + valueAlias + ": "
                    + new BagFormatter().join(result));
        }
        return result;
    }

    public List getAvailableValues(List result) {
        if (result == null)
            result = new ArrayList(1);
        return _getAvailableValues(result);
    }

    protected abstract String _getVersion();

    protected abstract String _getValue(int codepoint);

    protected abstract List _getNameAliases(List result);

    protected abstract List _getValueAliases(String valueAlias, List result);

    protected abstract List _getAvailableValues(List result);

    // conveniences
    public final List getNameAliases() {
        return getNameAliases(null);
    }

    public final List getValueAliases(String valueAlias) {
        return getValueAliases(valueAlias, null);
    }

    public final List getAvailableValues() {
        return getAvailableValues(null);
    }

    public final String getValue(int codepoint, boolean getShortest) {
        String result = getValue(codepoint);
        if (type >= MISC || result == null || !getShortest)
            return result;
        return getFirstValueAlias(result);
    }

    public final String getFirstNameAlias() {
        if (firstNameAlias == null) {
            firstNameAlias = (String) getNameAliases().get(0);
        }
        return firstNameAlias;
    }

    public final String getFirstValueAlias(String value) {
        if (valueToFirstValueAlias == null)
            _getFirstValueAliasCache();
        return (String) valueToFirstValueAlias.get(value);
    }

    private void _getFirstValueAliasCache() {
        maxValueWidth = 0;
        maxFirstValueAliasWidth = 0;
        valueToFirstValueAlias = new HashMap(1);
        Iterator it = getAvailableValues().iterator();
        while (it.hasNext()) {
            String value = (String) it.next();
            String first = (String) getValueAliases(value).get(0);
            if (first == null) { // internal error
                throw new IllegalArgumentException(
                        "Value not in value aliases: " + value);
            }
            if (DEBUG && CHECK_NAME.equals(getName())) {
                System.out.println("First Alias: " + getName() + ": " + value
                        + " => " + first
                        + new BagFormatter().join(getValueAliases(value)));
            }
            valueToFirstValueAlias.put(value, first);
            if (value.length() > maxValueWidth) {
                maxValueWidth = value.length();
            }
            if (first.length() > maxFirstValueAliasWidth) {
                maxFirstValueAliasWidth = first.length();
            }
        }
    }

    private int maxValueWidth = -1;

    private int maxFirstValueAliasWidth = -1;

    public int getMaxWidth(boolean getShortest) {
        if (maxValueWidth < 0)
            _getFirstValueAliasCache();
        if (getShortest)
            return maxFirstValueAliasWidth;
        return maxValueWidth;
    }

    public final UnicodeSet getSet(String propertyValue) {
        return getSet(propertyValue, null);
    }

    public final UnicodeSet getSet(PatternMatcher matcher) {
        return getSet(matcher, null);
    }

    public final UnicodeSet getSet(String propertyValue, UnicodeSet result) {
        return getSet(new SimpleMatcher(propertyValue,
                isType(STRING_OR_MISC_MASK) ? null : PROPERTY_COMPARATOR),
                result);
    }

    private UnicodeMap unicodeMap = null;

    public static final String UNUSED = "??";

    public final UnicodeSet getSet(PatternMatcher matcher, UnicodeSet result) {
        if (result == null)
            result = new UnicodeSet();
        if (isType(STRING_OR_MISC_MASK)) {
            for (int i = 0; i <= 0x10FFFF; ++i) {
                String value = getValue(i);
                if (value != null && matcher.matches(value)) {
                    result.add(i);
                }
            }
            return result;
        }
        List temp = new ArrayList(1); // to avoid reallocating...
        UnicodeMap um = getUnicodeMap_internal();
        Iterator it = um.getAvailableValues(null).iterator();
        main: while (it.hasNext()) {
            String value = (String) it.next();
            temp.clear();
            Iterator it2 = getValueAliases(value, temp).iterator();
            while (it2.hasNext()) {
                String value2 = (String) it2.next();
                // System.out.println("Values:" + value2);
                if (matcher.matches(value2)
                        || matcher.matches(toSkeleton(value2))) {
                    um.keySet(value, result);
                    continue main;
                }
            }
        }
        return result;
    }

    /*
     * public UnicodeSet getMatchSet(UnicodeSet result) { if (result == null)
     * result = new UnicodeSet(); addAll(matchIterator, result); return result; }
     * 
     * public void setMatchSet(UnicodeSet set) { matchIterator = new
     * UnicodeSetIterator(set); }
     */

    /**
     * Utility for debugging
     */
    public static String getStack() {
        Exception e = new Exception();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return "Showing Stack with fake " + sw.getBuffer().toString();
    }

    // TODO use this instead of plain strings
    public static class Name implements Comparable {
        private String skeleton;

        private String pretty;

        public final int RAW = 0, TITLE = 1, NORMAL = 2;

        public Name(String name, int style) {
            if (name == null)
                name = "";
            if (style == RAW) {
                skeleton = pretty = name;
            } else {
                pretty = regularize(name, style == TITLE);
                skeleton = toSkeleton(pretty);
            }
        }

        public int compareTo(Object o) {
            return skeleton.compareTo(((Name) o).skeleton);
        }

        public boolean equals(Object o) {
            return skeleton.equals(((Name) o).skeleton);
        }

        public int hashCode() {
            return skeleton.hashCode();
        }

        public String toString() {
            return pretty;
        }
    }

    /**
     * @return the unicode map
     */
    public UnicodeMap getUnicodeMap() {
        return getUnicodeMap(false);
    }

    /**
     * @return the unicode map
     */
    public UnicodeMap getUnicodeMap(boolean getShortest) {
        if (!getShortest)
            return (UnicodeMap) getUnicodeMap_internal().cloneAsThawed();
        UnicodeMap result = new UnicodeMap();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            // if (DEBUG && i == 0x41) System.out.println(i + "\t" +
            // getValue(i));
            String value = getValue(i, true);
            result.put(i, value);
        }
        return result;
    }

    /**
     * @return the unicode map
     */
    protected UnicodeMap getUnicodeMap_internal() {
        if (unicodeMap == null)
            unicodeMap = _getUnicodeMap();
        return unicodeMap;
    }

    protected UnicodeMap _getUnicodeMap() {
        UnicodeMap result = new UnicodeMap();
        HashMap myIntern = new HashMap();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            // if (DEBUG && i == 0x41) System.out.println(i + "\t" +
            // getValue(i));
            String value = getValue(i);
            String iValue = (String) myIntern.get(value);
            if (iValue == null)
                myIntern.put(value, iValue = value);
            result.put(i, iValue);
        }
        if (DEBUG) {
            for (int i = 0; i <= 0x10FFFF; ++i) {
                // if (DEBUG && i == 0x41) System.out.println(i + "\t" +
                // getValue(i));
                String value = getValue(i);
                String resultValue = (String) result.getValue(i);
                if (!value.equals(resultValue)) {
                    throw new RuntimeException("Value failure at: "
                            + Utility.hex(i));
                }
            }
        }
        if (DEBUG && CHECK_NAME.equals(getName())) {
            System.out.println(getName() + ":\t" + getClass().getName() + "\t"
                    + getVersion());
            System.out.println(getStack());
            System.out.println(result);
        }
        return result;
    }

    /**
     * Really ought to create a Collection UniqueList, that forces uniqueness.
     * But for now...
     */
    public static Collection addUnique(Object obj, Collection result) {
        if (obj != null && !result.contains(obj))
            result.add(obj);
        return result;
    }

    /**
     * Utility for managing property & non-string value aliases
     */
    public static final Comparator PROPERTY_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return compareNames((String) o1, (String) o2);
        }
    };

    /**
     * Utility for managing property & non-string value aliases
     * 
     */
    // TODO optimize
    public static boolean equalNames(String a, String b) {
        if (a == b)
            return true;
        if (a == null)
            return false;
        return toSkeleton(a).equals(toSkeleton(b));
    }

    /**
     * Utility for managing property & non-string value aliases
     */
    // TODO optimize
    public static int compareNames(String a, String b) {
        if (a == b)
            return 0;
        if (a == null)
            return -1;
        if (b == null)
            return 1;
        return toSkeleton(a).compareTo(toSkeleton(b));
    }

    /**
     * Utility for managing property & non-string value aliases
     */
    // TODO account for special names, tibetan, hangul
    public static String toSkeleton(String source) {
        if (source == null)
            return null;
        StringBuffer skeletonBuffer = new StringBuffer();
        boolean gotOne = false;
        // remove spaces, '_', '-'
        // we can do this with char, since no surrogates are involved
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (i > 0 && (ch == '_' || ch == ' ' || ch == '-')) {
                gotOne = true;
            } else {
                char ch2 = Character.toLowerCase(ch);
                if (ch2 != ch) {
                    gotOne = true;
                    skeletonBuffer.append(ch2);
                } else {
                    skeletonBuffer.append(ch);
                }
            }
        }
        if (!gotOne)
            return source; // avoid string creation
        return skeletonBuffer.toString();
    }

    // get the name skeleton
    public static String toNameSkeleton(String source) {
        if (source == null)
            return null;
        StringBuffer result = new StringBuffer();
        // remove spaces, medial '-'
        // we can do this with char, since no surrogates are involved
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (('0' <= ch && ch <= '9') || ('A' <= ch && ch <= 'Z')
                    || ch == '<' || ch == '>') {
                result.append(ch);
            } else if (ch == ' ') {
                // don't copy ever
            } else if (ch == '-') {
                // only copy non-medials AND trailing O-E
                if (0 == i
                        || i == source.length() - 1
                        || source.charAt(i - 1) == ' '
                        || source.charAt(i + 1) == ' '
                        || (i == source.length() - 2
                                && source.charAt(i - 1) == 'O' && source
                                .charAt(i + 1) == 'E')) {
                    System.out.println("****** EXCEPTION " + source);
                    result.append(ch);
                }
                // otherwise don't copy
            } else {
                throw new IllegalArgumentException("Illegal Name Char: U+"
                        + Utility.hex(ch) + ", " + ch);
            }
        }
        return result.toString();
    }

    /**
     * These routines use the Java functions, because they only need to act on
     * ASCII Changes space, - into _, inserts _ between lower and UPPER.
     */
    public static String regularize(String source, boolean titlecaseStart) {
        if (source == null)
            return source;
        /*
         * if (source.equals("noBreak")) { // HACK if (titlecaseStart) return
         * "NoBreak"; return source; }
         */
        StringBuffer result = new StringBuffer();
        int lastCat = -1;
        boolean haveFirstCased = true;
        for (int i = 0; i < source.length(); ++i) {
            char c = source.charAt(i);
            if (c == ' ' || c == '-' || c == '_') {
                c = '_';
                haveFirstCased = true;
            }
            if (c == '=')
                haveFirstCased = true;
            int cat = Character.getType(c);
            if (lastCat == Character.LOWERCASE_LETTER
                    && cat == Character.UPPERCASE_LETTER) {
                result.append('_');
            }
            if (haveFirstCased
                    && (cat == Character.LOWERCASE_LETTER
                            || cat == Character.TITLECASE_LETTER || cat == Character.UPPERCASE_LETTER)) {
                if (titlecaseStart) {
                    c = Character.toUpperCase(c);
                }
                haveFirstCased = false;
            }
            result.append(c);
            lastCat = cat;
        }
        return result.toString();
    }

    /**
     * Utility function for comparing codepoint to string without generating new
     * string.
     * 
     * @param codepoint
     * @param other
     * @return true if the codepoint equals the string
     */
    public static final boolean equals(int codepoint, String other) {
        if (other.length() == 1) {
            return codepoint == other.charAt(0);
        }
        if (other.length() == 2) {
            return other.equals(UTF16.valueOf(codepoint));
        }
        return false;
    }

    /**
     * Utility that should be on UnicodeSet
     * 
     * @param source
     * @param result
     */
    static public void addAll(UnicodeSetIterator source, UnicodeSet result) {
        while (source.nextRange()) {
            if (source.codepoint == UnicodeSetIterator.IS_STRING) {
                result.add(source.string);
            } else {
                result.add(source.codepoint, source.codepointEnd);
            }
        }
    }

    /**
     * Really ought to create a Collection UniqueList, that forces uniqueness.
     * But for now...
     */
    public static Collection addAllUnique(Collection source, Collection result) {
        for (Iterator it = source.iterator(); it.hasNext();) {
            addUnique(it.next(), result);
        }
        return result;
    }

    /**
     * Really ought to create a Collection UniqueList, that forces uniqueness.
     * But for now...
     */
    public static Collection addAllUnique(Object[] source, Collection result) {
        for (int i = 0; i < source.length; ++i) {
            addUnique(source[i], result);
        }
        return result;
    }

    static public class Factory {
        static boolean DEBUG = false;

        Map canonicalNames = new TreeMap();

        Map skeletonNames = new TreeMap();

        Map propertyCache = new HashMap(1);

        public final Factory add(UnicodeProperty sp) {
            canonicalNames.put(sp.getName(), sp);
            List c = sp.getNameAliases(new ArrayList(1));
            Iterator it = c.iterator();
            while (it.hasNext()) {
                skeletonNames.put(toSkeleton((String) it.next()), sp);
            }
            return this;
        }

        public final UnicodeProperty getProperty(String propertyAlias) {
            return (UnicodeProperty) skeletonNames
                    .get(toSkeleton(propertyAlias));
        }

        public final List getAvailableNames() {
            return getAvailableNames(null);
        }

        public final List getAvailableNames(List result) {
            if (result == null)
                result = new ArrayList(1);
            Iterator it = canonicalNames.keySet().iterator();
            while (it.hasNext()) {
                addUnique(it.next(), result);
            }
            return result;
        }

        public final List getAvailableNames(int propertyTypeMask) {
            return getAvailableNames(propertyTypeMask, null);
        }

        public final List getAvailableNames(int propertyTypeMask, List result) {
            if (result == null)
                result = new ArrayList(1);
            Iterator it = canonicalNames.keySet().iterator();
            while (it.hasNext()) {
                String item = (String) it.next();
                UnicodeProperty property = getProperty(item);
                if (DEBUG)
                    System.out.println("Properties: " + item + ","
                            + property.getType());
                if (!property.isType(propertyTypeMask)) {
                    // System.out.println("Masking: " + property.getType() + ","
                    // + propertyTypeMask);
                    continue;
                }
                addUnique(property.getName(), result);
            }
            return result;
        }

        InversePatternMatcher inverseMatcher = new InversePatternMatcher();

        /**
         * Format is: propname ('=' | '!=') propvalue ( '|' propValue )*
         */
        public final UnicodeSet getSet(String propAndValue,
                PatternMatcher matcher, UnicodeSet result) {
            int equalPos = propAndValue.indexOf('=');
            String prop = propAndValue.substring(0, equalPos);
            String value = propAndValue.substring(equalPos + 1);
            boolean negative = false;
            if (prop.endsWith("!")) {
                prop = prop.substring(0, prop.length() - 1);
                negative = true;
            }
            prop = prop.trim();
            UnicodeProperty up = getProperty(prop);
            if (matcher == null) {
                matcher = new SimpleMatcher(value, up
                        .isType(STRING_OR_MISC_MASK) ? null
                        : PROPERTY_COMPARATOR);
            }
            if (negative) {
                inverseMatcher.set(matcher);
                matcher = inverseMatcher;
            }
            return up.getSet(matcher.set(value), result);
        }

        public final UnicodeSet getSet(String propAndValue,
                PatternMatcher matcher) {
            return getSet(propAndValue, matcher, null);
        }

        public final UnicodeSet getSet(String propAndValue) {
            return getSet(propAndValue, null, null);
        }

        public final SymbolTable getSymbolTable(String prefix) {
            return new PropertySymbolTable(prefix);
        }

        private class MyXSymbolTable extends UnicodeSet.XSymbolTable {
            public boolean applyPropertyAlias(String propertyName,
                    String propertyValue, UnicodeSet result) {
                if (false)
                    System.out.println(propertyName + "=" + propertyValue);
                UnicodeProperty prop = getProperty(propertyName);
                if (prop == null)
                    return false;
                result.clear();
                UnicodeSet x = prop.getSet(propertyValue, result);
                return x.size() != 0;
            }
        }

        public final UnicodeSet.XSymbolTable getXSymbolTable() {
            return new MyXSymbolTable();
        }

        private class PropertySymbolTable implements SymbolTable {
            static final boolean DEBUG = false;

            private String prefix;

            RegexMatcher regexMatcher = new RegexMatcher();

            PropertySymbolTable(String prefix) {
                this.prefix = prefix;
            }

            public char[] lookup(String s) {
                if (DEBUG)
                    System.out.println("\t(" + prefix + ")Looking up " + s);
                // ensure, again, that prefix matches
                int start = prefix.length();
                if (!s.regionMatches(true, 0, prefix, 0, start))
                    return null;

                int pos = s.indexOf(':', start);
                if (pos < 0) { // should never happen
                    throw new IllegalArgumentException(
                            "Internal Error: missing =: " + s + "\r\n");
                }
                UnicodeProperty prop = getProperty(s.substring(start, pos));
                if (prop == null) {
                    throw new IllegalArgumentException("Invalid Property in: "
                            + s + "\r\nUse " + showSet(getAvailableNames()));
                }
                String value = s.substring(pos + 1);
                UnicodeSet set;
                if (value.startsWith("\u00AB")) { // regex!
                    set = prop.getSet(regexMatcher.set(value.substring(1, value
                            .length() - 1)));
                } else {
                    set = prop.getSet(value);
                }
                if (set.size() == 0) {
                    throw new IllegalArgumentException(
                            "Empty Property-Value in: " + s + "\r\nUse "
                                    + showSet(prop.getAvailableValues()));
                }
                if (DEBUG)
                    System.out.println("\t(" + prefix + ")Returning "
                            + set.toPattern(true));
                return set.toPattern(true).toCharArray(); // really ugly
            }

            private String showSet(List list) {
                StringBuffer result = new StringBuffer("[");
                boolean first = true;
                for (Iterator it = list.iterator(); it.hasNext();) {
                    if (!first)
                        result.append(", ");
                    else
                        first = false;
                    result.append(it.next().toString());
                }
                result.append("]");
                return result.toString();
            }

            public UnicodeMatcher lookupMatcher(int ch) {
                return null;
            }

            public String parseReference(String text, ParsePosition pos,
                    int limit) {
                if (DEBUG)
                    System.out.println("\t(" + prefix + ")Parsing <"
                            + text.substring(pos.getIndex(), limit) + ">");
                int start = pos.getIndex();
                // ensure that it starts with 'prefix'
                if (!text
                        .regionMatches(true, start, prefix, 0, prefix.length()))
                    return null;
                start += prefix.length();
                // now see if it is of the form identifier:identifier
                int i = getIdentifier(text, start, limit);
                if (i == start)
                    return null;
                String prop = text.substring(start, i);
                String value = "true";
                if (i < limit) {
                    if (text.charAt(i) == ':') {
                        int j;
                        if (text.charAt(i + 1) == '\u00AB') { // regular
                                                                // expression
                            j = text.indexOf('\u00BB', i + 2) + 1; // include
                                                                    // last
                                                                    // character
                            if (j <= 0)
                                return null;
                        } else {
                            j = getIdentifier(text, i + 1, limit);
                        }
                        value = text.substring(i + 1, j);
                        i = j;
                    }
                }
                pos.setIndex(i);
                if (DEBUG)
                    System.out.println("\t(" + prefix + ")Parsed <" + prop
                            + ">=<" + value + ">");
                return prefix + prop + ":" + value;
            }

            private int getIdentifier(String text, int start, int limit) {
                if (DEBUG)
                    System.out.println("\tGetID <"
                            + text.substring(start, limit) + ">");
                int cp = 0;
                int i;
                for (i = start; i < limit; i += UTF16.getCharCount(cp)) {
                    cp = UTF16.charAt(text, i);
                    if (!com.ibm.icu.lang.UCharacter
                            .isUnicodeIdentifierPart(cp)
                            && cp != '.') {
                        break;
                    }
                }
                if (DEBUG)
                    System.out.println("\tGotID <" + text.substring(start, i)
                            + ">");
                return i;
            }
        }
    }

    public static class FilteredProperty extends UnicodeProperty {
        private UnicodeProperty property;

        protected StringFilter filter;

        protected UnicodeSetIterator matchIterator = new UnicodeSetIterator(
                new UnicodeSet(0, 0x10FFFF));

        protected HashMap backmap;

        boolean allowValueAliasCollisions = false;

        public FilteredProperty(UnicodeProperty property, StringFilter filter) {
            this.property = property;
            this.filter = filter;
        }

        public StringFilter getFilter() {
            return filter;
        }

        public UnicodeProperty setFilter(StringFilter filter) {
            this.filter = filter;
            return this;
        }

        List temp = new ArrayList(1);

        public List _getAvailableValues(List result) {
            temp.clear();
            return filter.addUnique(property.getAvailableValues(temp), result);
        }

        public List _getNameAliases(List result) {
            temp.clear();
            return filter.addUnique(property.getNameAliases(temp), result);
        }

        public String _getValue(int codepoint) {
            return filter.remap(property.getValue(codepoint));
        }

        public List _getValueAliases(String valueAlias, List result) {
            if (backmap == null) {
                backmap = new HashMap(1);
                temp.clear();
                Iterator it = property.getAvailableValues(temp).iterator();
                while (it.hasNext()) {
                    String item = (String) it.next();
                    String mappedItem = filter.remap(item);
                    if (backmap.get(mappedItem) != null
                            && !allowValueAliasCollisions) {
                        throw new IllegalArgumentException(
                                "Filter makes values collide! " + item + ", "
                                        + mappedItem);
                    }
                    backmap.put(mappedItem, item);
                }
            }
            valueAlias = (String) backmap.get(valueAlias);
            temp.clear();
            return filter.addUnique(property.getValueAliases(valueAlias, temp),
                    result);
        }

        public String _getVersion() {
            return property.getVersion();
        }

        public boolean isAllowValueAliasCollisions() {
            return allowValueAliasCollisions;
        }

        public FilteredProperty setAllowValueAliasCollisions(boolean b) {
            allowValueAliasCollisions = b;
            return this;
        }

    }

    public static abstract class StringFilter implements Cloneable {
        public abstract String remap(String original);

        public final List addUnique(Collection source, List result) {
            if (result == null)
                result = new ArrayList(1);
            Iterator it = source.iterator();
            while (it.hasNext()) {
                UnicodeProperty.addUnique(remap((String) it.next()), result);
            }
            return result;
        }
        /*
         * public Object clone() { try { return super.clone(); } catch
         * (CloneNotSupportedException e) { throw new
         * IllegalStateException("Should never happen."); } }
         */
    }

    public static class MapFilter extends StringFilter {
        private Map valueMap;

        public MapFilter(Map valueMap) {
            this.valueMap = valueMap;
        }

        public String remap(String original) {
            Object changed = valueMap.get(original);
            return changed == null ? original : (String) changed;
        }

        public Map getMap() {
            return valueMap;
        }
    }

    public interface PatternMatcher extends ObjectMatcher {
        public PatternMatcher set(String pattern);
    }

    public static class InversePatternMatcher extends InverseMatcher implements
            PatternMatcher {
        PatternMatcher other;

        public PatternMatcher set(PatternMatcher toInverse) {
            other = toInverse;
            return this;
        }

        public boolean matches(Object value) {
            return !other.matches(value);
        }

        public PatternMatcher set(String pattern) {
            other.set(pattern);
            return this;
        }
    }

    public static class SimpleMatcher implements PatternMatcher {
        Comparator comparator;

        String pattern;

        public SimpleMatcher(String pattern, Comparator comparator) {
            this.comparator = comparator;
            this.pattern = pattern;
        }

        public boolean matches(Object value) {
            if (comparator == null)
                return pattern.equals(value);
            return comparator.compare(pattern, value) == 0;
        }

        public PatternMatcher set(String pattern) {
            this.pattern = pattern;
            return this;
        }
    }

    public static class RegexMatcher implements UnicodeProperty.PatternMatcher {
        private java.util.regex.Matcher matcher;

        public UnicodeProperty.PatternMatcher set(String pattern) {
            matcher = Pattern.compile(pattern).matcher("");
            return this;
        }

        public boolean matches(Object value) {
            matcher.reset(value.toString());
            return matcher.matches();
        }
    }

    public static abstract class BaseProperty extends UnicodeProperty {
        protected List propertyAliases = new ArrayList(1);

        protected Map toValueAliases;

        protected String version;

        public BaseProperty setMain(String alias, String shortAlias,
                int propertyType, String version) {
            setName(alias);
            setType(propertyType);
            propertyAliases.add(shortAlias);
            propertyAliases.add(alias);
            this.version = version;
            return this;
        }

        public String _getVersion() {
            return version;
        }

        public List _getNameAliases(List result) {
            addAllUnique(propertyAliases, result);
            return result;
        }

        public BaseProperty addValueAliases(String[][] valueAndAlternates,
                boolean errorIfCant) {
            if (toValueAliases == null)
                _fixValueAliases();
            for (int i = 0; i < valueAndAlternates.length; ++i) {
                for (int j = 1; j < valueAndAlternates[0].length; ++j) {
                    addValueAlias(valueAndAlternates[i][0],
                            valueAndAlternates[i][j], errorIfCant);
                }
            }
            return this;
        }

        public void addValueAlias(String value, String valueAlias,
                boolean errorIfCant) {
            List result = (List) toValueAliases.get(value);
            if (result == null && !errorIfCant)
                return;
            addUnique(value, result);
            addUnique(valueAlias, result);
        }

        protected List _getValueAliases(String valueAlias, List result) {
            if (toValueAliases == null)
                _fixValueAliases();
            List a = (List) toValueAliases.get(valueAlias);
            if (a != null)
                addAllUnique(a, result);
            return result;
        }

        protected void _fixValueAliases() {
            if (toValueAliases == null)
                toValueAliases = new HashMap(1);
            for (Iterator it = getAvailableValues().iterator(); it.hasNext();) {
                Object value = it.next();
                _ensureValueInAliases(value);
            }
        }

        protected void _ensureValueInAliases(Object value) {
            List result = (List) toValueAliases.get(value);
            if (result == null)
                toValueAliases.put(value, result = new ArrayList(1));
            addUnique(value, result);
        }

        public BaseProperty swapFirst2ValueAliases() {
            for (Iterator it = toValueAliases.keySet().iterator(); it.hasNext();) {
                List list = (List) toValueAliases.get(it.next());
                if (list.size() < 2)
                    continue;
                Object first = list.get(0);
                list.set(0, list.get(1));
                list.set(1, first);
            }
            return this;
        }

        /**
         * @param string
         * @return
         */
        public UnicodeProperty addName(String string) {
            throw new UnsupportedOperationException();
        }

    }

    public static abstract class SimpleProperty extends BaseProperty {
        List values;

        public UnicodeProperty addName(String alias) {
            propertyAliases.add(alias);
            return this;
        }

        public SimpleProperty setValues(String valueAlias) {
            _addToValues(valueAlias, null);
            return this;
        }

        public SimpleProperty setValues(String[] valueAliases,
                String[] alternateValueAliases) {
            for (int i = 0; i < valueAliases.length; ++i) {
                if (valueAliases[i].equals(UNUSED))
                    continue;
                _addToValues(
                        valueAliases[i],
                        alternateValueAliases != null ? alternateValueAliases[i]
                                : null);
            }
            return this;
        }

        public SimpleProperty setValues(List valueAliases) {
            this.values = new ArrayList(valueAliases);
            for (Iterator it = this.values.iterator(); it.hasNext();) {
                _addToValues((String) it.next(), null);
            }
            return this;
        }

        public List _getAvailableValues(List result) {
            if (values == null)
                _fillValues();
            result.addAll(values);
            return result;
        }

        protected void _fillValues() {
            List newvalues = (List) getUnicodeMap_internal()
                    .getAvailableValues(new ArrayList());
            for (Iterator it = newvalues.iterator(); it.hasNext();) {
                _addToValues((String) it.next(), null);
            }
        }

        private void _addToValues(String item, String alias) {
            if (values == null)
                values = new ArrayList(1);
            if (toValueAliases == null)
                _fixValueAliases();
            addUnique(item, values);
            _ensureValueInAliases(item);
            addValueAlias(item, alias, true);
        }
        /*        public String _getVersion() {
         return version;
         }
         */
    }

    public static class UnicodeMapProperty extends BaseProperty {
        /*
         * Example of usage:
         * new UnicodeProperty.UnicodeMapProperty() {
         {
         unicodeMap = new UnicodeMap();
         unicodeMap.setErrorOnReset(true);
         unicodeMap.put(0xD, "CR");
         unicodeMap.put(0xA, "LF");
         UnicodeProperty cat = getProperty("General_Category");
         UnicodeSet temp = cat.getSet("Line_Separator")
         .addAll(cat.getSet("Paragraph_Separator"))
         .addAll(cat.getSet("Control"))
         .addAll(cat.getSet("Format"))
         .remove(0xD).remove(0xA).remove(0x200C).remove(0x200D);
         unicodeMap.putAll(temp, "Control");
         UnicodeSet graphemeExtend = getProperty("Grapheme_Extend").getSet("true");
         unicodeMap.putAll(graphemeExtend,"Extend");
         UnicodeProperty hangul = getProperty("Hangul_Syllable_Type");
         unicodeMap.putAll(hangul.getSet("L"),"L");
         unicodeMap.putAll(hangul.getSet("V"),"V");
         unicodeMap.putAll(hangul.getSet("T"),"T");
         unicodeMap.putAll(hangul.getSet("LV"),"LV");
         unicodeMap.putAll(hangul.getSet("LVT"),"LVT");
         unicodeMap.setMissing("Other");
         }
         }.setMain("Grapheme_Cluster_Break", "GCB", UnicodeProperty.ENUMERATED, version)
         */
        protected UnicodeMap unicodeMap;

        public UnicodeMapProperty set(UnicodeMap map) {
            unicodeMap = map;
            return this;
        }

        protected String _getValue(int codepoint) {
            return (String) unicodeMap.getValue(codepoint);
        }

        /* protected List _getValueAliases(String valueAlias, List result) {
         if (!unicodeMap.getAvailableValues().contains(valueAlias)) return result;
         result.add(valueAlias);
         return result; // no other aliases
         }
         */protected List _getAvailableValues(List result) {
            return (List) unicodeMap.getAvailableValues(result);
        }
    }
}

