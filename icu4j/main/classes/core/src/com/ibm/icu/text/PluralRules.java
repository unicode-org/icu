/*
 *******************************************************************************
 * Copyright (C) 2007-2013, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.impl.PatternProps;
import com.ibm.icu.impl.PluralRulesLoader;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.PluralRules.NumberInfo;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * <p>
 * Defines rules for mapping non-negative numeric values onto a small set of keywords.
 * </p>
 * <p>
 * Rules are constructed from a text description, consisting of a series of keywords and conditions. The {@link #select}
 * method examines each condition in order and returns the keyword for the first condition that matches the number. If
 * none match, {@link #KEYWORD_OTHER} is returned.
 * </p>
 * <p>
 * A PluralRules object is immutable. It contains caches for sample values, but those are synchronized.
 * <p>
 * PluralRules is Serializable so that it can be used in formatters, which are serializable.
 * </p>
 * <p>
 * For more information, details, and tips for writing rules, see the <a
 * href="http://www.unicode.org/draft/reports/tr35/tr35.html#Language_Plural_Rules">LDML spec, C.11 Language Plural
 * Rules</a>
 * </p>
 * <p>
 * Examples:
 * </p>
 * 
 * <pre>
 * &quot;one: n is 1; few: n in 2..4&quot;
 * </pre>
 * <p>
 * This defines two rules, for 'one' and 'few'. The condition for 'one' is "n is 1" which means that the number must be
 * equal to 1 for this condition to pass. The condition for 'few' is "n in 2..4" which means that the number must be
 * between 2 and 4 inclusive - and be an integer - for this condition to pass. All other numbers are assigned the
 * keyword "other" by the default rule.
 * </p>
 * 
 * <pre>
 * &quot;zero: n is 0; one: n is 1; zero: n mod 100 in 1..19&quot;
 * </pre>
 * <p>
 * This illustrates that the same keyword can be defined multiple times. Each rule is examined in order, and the first
 * keyword whose condition passes is the one returned. Also notes that a modulus is applied to n in the last rule. Thus
 * its condition holds for 119, 219, 319...
 * </p>
 * 
 * <pre>
 * &quot;one: n is 1; few: n mod 10 in 2..4 and n mod 100 not in 12..14&quot;
 * </pre>
 * <p>
 * This illustrates conjunction and negation. The condition for 'few' has two parts, both of which must be met:
 * "n mod 10 in 2..4" and "n mod 100 not in 12..14". The first part applies a modulus to n before the test as in the
 * previous example. The second part applies a different modulus and also uses negation, thus it matches all numbers
 * _not_ in 12, 13, 14, 112, 113, 114, 212, 213, 214...
 * </p>
 * <p>
 * Syntax:
 * </p>
 * <pre>
 * rules         = rule (';' rule)*
 * rule          = keyword ':' condition
 * keyword       = &lt;identifier&gt;
 * condition     = and_condition ('or' and_condition)*
 * and_condition = relation ('and' relation)*
 * relation      = is_relation | in_relation | within_relation | 'n' <EOL>
 * is_relation   = expr 'is' ('not')? value
 * in_relation   = expr ('not')? 'in' range_list
 * within_relation = expr ('not')? 'within' range_list
 * expr          = ('n' | 'i' | 'f' | 'v') ('mod' value)?
 * range_list    = (range | value) (',' range_list)*
 * value         = digit+ ('.' digit+)?
 * digit         = 0|1|2|3|4|5|6|7|8|9
 * range         = value'..'value
 * </pre>
 * 
 * <p>
 * The i, f, and v values are defined as follows:
 * </p>
 * <ul>
 * <li>i to be the integer digits.</li>
 * <li>f to be the visible fractional digits, as an integer.</li>
 * <li>v to be the number of visible fraction digits.</li>
 * <li>j is defined to only match integers. That is j is 3 fails if v != 0 (eg for 3.1 or 3.0).</li>
 * </ul>
 * <p>
 * Examples are in the following table:
 * </p>
 * <table border='1' style="border-collapse:collapse">
 * <tbody>
 * <tr>
 * <th>n</th>
 * <th>i</th>
 * <th>f</th>
 * <th>v</th>
 * </tr>
 * <tr>
 * <td>1.0</td>
 * <td>1</td>
 * <td align="right">0</td>
 * <td>1</td>
 * </tr>
 * <tr>
 * <td>1.00</td>
 * <td>1</td>
 * <td align="right">0</td>
 * <td>2</td>
 * </tr>
 * <tr>
 * <td>1.3</td>
 * <td>1</td>
 * <td align="right">3</td>
 * <td>1</td>
 * </tr>
 * <tr>
 * <td>1.03</td>
 * <td>1</td>
 * <td align="right">3</td>
 * <td>2</td>
 * </tr>
 * <tr>
 * <td>1.23</td>
 * <td>1</td>
 * <td align="right">23</td>
 * <td>2</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * An "identifier" is a sequence of characters that do not have the Unicode Pattern_Syntax or Pattern_White_Space
 * properties.
 * <p>
 * The difference between 'in' and 'within' is that 'in' only includes integers in the specified range, while 'within'
 * includes all values. Using 'within' with a range_list consisting entirely of values is the same as using 'in' (it's
 * not an error).
 * </p>
 * 
 * @stable ICU 3.8
 */
public class PluralRules implements Serializable {
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final String CATEGORY_SEPARATOR = ";  ";
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final String KEYWORD_RULE_SEPARATOR = ": ";

    private static final long serialVersionUID = 1;

    private final RuleList rules;
    private final Set<String> keywords;
    private int repeatLimit; // for equality test
    private transient int hashCode;
    private transient Map<String, List<Double>> _keySamplesMap;
    private transient Map<String, Boolean> _keyLimitedMap;
    private transient Map<String, Set<NumberInfo>> _keyFractionSamplesMap;
    private transient Set<NumberInfo> _fractionSamples;

    /**
     * Provides a factory for returning plural rules
     * 
     * @deprecated This API is ICU internal only.
     * @internal
     */
    public static abstract class Factory {
        /**
         * Provides access to the predefined <code>PluralRules</code> for a given locale and the plural type.
         * 
         * <p>
         * ICU defines plural rules for many locales based on CLDR <i>Language Plural Rules</i>. For these predefined
         * rules, see CLDR page at http://unicode.org/repos/cldr-tmp/trunk/diff/supplemental/language_plural_rules.html
         * 
         * @param locale
         *            The locale for which a <code>PluralRules</code> object is returned.
         * @param type
         *            The plural type (e.g., cardinal or ordinal).
         * @return The predefined <code>PluralRules</code> object for this locale. If there's no predefined rules for
         *         this locale, the rules for the closest parent in the locale hierarchy that has one will be returned.
         *         The final fallback always returns the default rules.
         * @deprecated This API is ICU internal only.
         * @internal
         */
        public abstract PluralRules forLocale(ULocale locale, PluralType type);

        /**
         * Utility for getting CARDINAL rules.
         * @param locale
         * @return plural rules.
         * @deprecated This API is ICU internal only.
         * @internal
         */
        public final PluralRules forLocale(ULocale locale) {
            return forLocale(locale, PluralType.CARDINAL);
        }
        
        /**
         * Returns the locales for which there is plurals data.
         * 
         * @deprecated This API is ICU internal only.
         * @internal
         */
        public abstract ULocale[] getAvailableULocales();

        /**
         * Returns the 'functionally equivalent' locale with respect to plural rules. Calling PluralRules.forLocale with
         * the functionally equivalent locale, and with the provided locale, returns rules that behave the same. <br/>
         * All locales with the same functionally equivalent locale have plural rules that behave the same. This is not
         * exaustive; there may be other locales whose plural rules behave the same that do not have the same equivalent
         * locale.
         * 
         * @param locale
         *            the locale to check
         * @param isAvailable
         *            if not null and of length > 0, this will hold 'true' at index 0 if locale is directly defined
         *            (without fallback) as having plural rules
         * @return the functionally-equivalent locale
         * @deprecated This API is ICU internal only.
         * @internal
         */
        public abstract ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable);
        
        /**
         * Returns the default factory.
         * @deprecated This API is ICU internal only.
         * @internal
         */
        public static PluralRulesLoader getDefaultFactory() {
            return PluralRulesLoader.loader;
        }
    }
    // Standard keywords.

    /**
     * Common name for the 'zero' plural form.
     * @stable ICU 3.8
     */
    public static final String KEYWORD_ZERO = "zero";

    /**
     * Common name for the 'singular' plural form.
     * @stable ICU 3.8
     */
    public static final String KEYWORD_ONE = "one";

    /**
     * Common name for the 'dual' plural form.
     * @stable ICU 3.8
     */
    public static final String KEYWORD_TWO = "two";

    /**
     * Common name for the 'paucal' or other special plural form.
     * @stable ICU 3.8
     */
    public static final String KEYWORD_FEW = "few";

    /**
     * Common name for the arabic (11 to 99) plural form.
     * @stable ICU 3.8
     */
    public static final String KEYWORD_MANY = "many";

    /**
     * Common name for the default plural form.  This name is returned
     * for values to which no other form in the rule applies.  It
     * can additionally be assigned rules of its own.
     * @stable ICU 3.8
     */
    public static final String KEYWORD_OTHER = "other";

    /**
     * Value returned by {@link #getUniqueKeywordValue} when there is no
     * unique value to return.
     * @stable ICU 4.8
     */
    public static final double NO_UNIQUE_VALUE = -0.00123456777;

    /**
     * Type of plurals and PluralRules.
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    public enum PluralType {
        /**
         * Plural rules for cardinal numbers: 1 file vs. 2 files.
         * @draft ICU 50
         * @provisional This API might change or be removed in a future release.
         */
        CARDINAL,
        /**
         * Plural rules for ordinal numbers: 1st file, 2nd file, 3rd file, 4th file, etc.
         * @draft ICU 50
         * @provisional This API might change or be removed in a future release.
         */
        ORDINAL
    };

    /*
     * The default constraint that is always satisfied.
     */
    private static final Constraint NO_CONSTRAINT = new Constraint() {
        private static final long serialVersionUID = 9163464945387899416L;

        public boolean isFulfilled(NumberInfo n) {
            return true;
        }

        public boolean isLimited() {
            return false;
        }

        public String toString() {
            return "n is any";
        }

        public int updateRepeatLimit(int limit) {
            return limit;
        }

        public void getMentionedValues(Set<NumberInfo> toAddTo) {
            toAddTo.add(new NumberInfo(0));
            toAddTo.add(new NumberInfo(9999.9999));
        }
    };

    /*
     * The default rule that always returns "other".
     */
    private static final Rule DEFAULT_RULE = new Rule() {
        private static final long serialVersionUID = -5677499073940822149L;

        public String getKeyword() {
            return KEYWORD_OTHER;
        }

        public boolean appliesTo(NumberInfo n) {
            return true;
        }

        public boolean isLimited() {
            return false;
        }

        public String toString() {
            return null;
        }

        public int updateRepeatLimit(int limit) {
            return limit;
        }

        public void getMentionedValues(Set<NumberInfo> toAddTo) {
        }

        public String getConstraint() {
            return null;
        }
    };

    /**
     * The default rules that accept any number and return
     * {@link #KEYWORD_OTHER}.
     * @stable ICU 3.8
     */
    public static final PluralRules DEFAULT =
            new PluralRules(new RuleList().addRule(DEFAULT_RULE));

    /**
     * Parses a plural rules description and returns a PluralRules.
     * @param description the rule description.
     * @throws ParseException if the description cannot be parsed.
     *    The exception index is typically not set, it will be -1.
     * @stable ICU 3.8
     */
    public static PluralRules parseDescription(String description)
            throws ParseException {

        description = description.trim();
        if (description.length() == 0) {
            return DEFAULT;
        }

        return new PluralRules(parseRuleChain(description));
    }

    /**
     * Creates a PluralRules from a description if it is parsable,
     * otherwise returns null.
     * @param description the rule description.
     * @return the PluralRules
     * @stable ICU 3.8
     */
    public static PluralRules createRules(String description) {
        try {
            return parseDescription(description);
        } catch(ParseException e) {
            return null;
        }
    }

    private enum Operand {
        n,
        i,
        f,
        v,
        j;
    }

    /**
     * @deprecated This API is ICU internal only.
     * @internal
     */
    public static class NumberInfo implements Comparable<NumberInfo> {
        public final double source;
        public final int visibleFractionDigitCount;
        public final long fractionalDigits;
        public final long intValue;
        public final boolean hasIntegerValue;
        public final boolean isNegative;

        public NumberInfo(double n, int v, long f) {
            isNegative = n < 0;
            source = isNegative ? -n : n;
            visibleFractionDigitCount = v;
            fractionalDigits = f;
            intValue = (long)n;
            hasIntegerValue = source == intValue;
            // check values. TODO make into unit test.
            //            
            //            long visiblePower = (int) Math.pow(10, v);
            //            if (fractionalDigits > visiblePower) {
            //                throw new IllegalArgumentException();
            //            }
            //            double fraction = intValue + (fractionalDigits / (double) visiblePower);
            //            if (fraction != source) {
            //                double diff = Math.abs(fraction - source)/(Math.abs(fraction) + Math.abs(source));
            //                if (diff > 0.00000001d) {
            //                    throw new IllegalArgumentException();
            //                }
            //            }
        }

        // Ugly, but for samples we don't care.
        public NumberInfo(double n, int v) {
            this(n,v,getFractionalDigits(n, v));
        }

        private static int getFractionalDigits(double n, int v) {
            if (v == 0) {
                return 0;
            } else {
                int base = (int) Math.pow(10, v);
                long scaled = Math.round(n * base);
                return (int) (scaled % base);
            }
        }

        public NumberInfo(double n) {
            this(n, decimals(n));
        }

        // Ugly, but for samples we don't care.
        public static int decimals(double n) {
            String temp = String.valueOf(n);
            return temp.endsWith(".0") ? 0 : temp.length() - temp.indexOf('.') - 1;
        }

        public NumberInfo(long n) {
            this(n,0);
        }

        // Ugly, but for samples we don't care.
        public NumberInfo (String n) {
            this(Double.parseDouble(n), getVisibleFractionCount(n));
        }

        private static int getVisibleFractionCount(String value) {
            value = value.trim();
            int decimalPos = value.indexOf('.') + 1;
            if (decimalPos == 0) {
                return 0;
            } else {
                return value.length() - decimalPos;
            }
        }

        public double get(Operand operand) {
            switch(operand) {
            default: return source;
            case i: return intValue;
            case f: return fractionalDigits;
            case v: return visibleFractionDigitCount;
            }
        }

        public static Operand getOperand(String t) {
            return Operand.valueOf(t);
        }

        /**
         * We're not going to care about NaN.
         */
        public int compareTo(NumberInfo other) {
            if (intValue != other.intValue) {
                return intValue < other.intValue ? -1 : 1;
            }
            if (source != other.source) {
                return source < other.source ? -1 : 1;
            }
            if (visibleFractionDigitCount != other.visibleFractionDigitCount) {
                return visibleFractionDigitCount < other.visibleFractionDigitCount ? -1 : 1;
            }
            long diff = fractionalDigits - other.fractionalDigits;
            if (diff != 0) {
                return diff < 0 ? -1 : 1;
            }
            return 0;
        }
        @Override
        public boolean equals(Object arg0) {
            if (arg0 == null) {
                return false;
            }
            if (arg0 == this) {
                return true;
            }
            if (!(arg0 instanceof NumberInfo)) {
                return false;
            }
            NumberInfo other = (NumberInfo)arg0;
            return source == other.source && visibleFractionDigitCount == other.visibleFractionDigitCount && fractionalDigits == other.fractionalDigits;
        }
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return (int)(fractionalDigits + 37 * (visibleFractionDigitCount + (int)(37 * source)));
        }
        @Override
        public String toString() {
            return String.format("%." + visibleFractionDigitCount + "f", source);
        }

        public boolean hasIntegerValue() {
            return hasIntegerValue;
        }
    }


    /*
     * A constraint on a number.
     */
    private interface Constraint extends Serializable {
        /*
         * Returns true if the number fulfills the constraint.
         * @param n the number to test, >= 0.
         */
        boolean isFulfilled(NumberInfo n);

        /*
         * Returns false if an unlimited number of values fulfills the
         * constraint.
         */
        boolean isLimited();

        /*
         * Returns the larger of limit or the limit of this constraint.
         * If the constraint is a simple range test, this is the higher
         * end of the range; if it is a modulo test, this is the modulus.
         *
         * @param limit the target limit
         * @return the new limit
         */
        int updateRepeatLimit(int limit);

        /**
         * Gets samples of significant numbers
         */
        void getMentionedValues(Set<NumberInfo> toAddTo);

    }

    /*
     * A pluralization rule.
     */
    private interface Rule extends Serializable {
        /* Returns the keyword that names this rule. */
        String getKeyword();

        /* Returns true if the rule applies to the number. */
        boolean appliesTo(NumberInfo n);

        /* Returns false if an unlimited number of values generate this rule. */
        boolean isLimited();

        /* Returns the larger of limit and this rule's limit. */
        int updateRepeatLimit(int limit);

        /**
         * Gets samples of significant numbers
         */
        void getMentionedValues(Set<NumberInfo> toAddTo);

        public String getConstraint();
    }

    //    /*
    //     * A list of rules to apply in order.
    //     */
    //    private class RuleList extends Serializable {
    //        /* Returns the keyword of the first rule that applies to the number. */
    //        String select(NumberInfo n);
    //
    //        /* Returns the set of defined keywords. */
    //        Set<String> getKeywords();
    //
    //        /* Return the value at which this rulelist starts repeating. */
    //        int getRepeatLimit();
    //
    //        /* Return true if the values for this keyword are limited. */
    //        boolean isLimited(String keyword);
    //
    //        /**
    //         * Get mentioned samples
    //         */
    //        Set<NumberInfo> getMentionedValues(Set<NumberInfo> toAddTo);
    //
    //        /**
    //         * keyword: rules mapping
    //         */
    //        String getRules(String keyword);
    //    }

    /*
     * syntax:
     * condition :     or_condition
     *                 and_condition
     * or_condition :  and_condition 'or' condition
     * and_condition : relation
     *                 relation 'and' relation
     * relation :      is_relation
     *                 in_relation
     *                 within_relation
     *                 'n' EOL
     * is_relation :   expr 'is' value
     *                 expr 'is' 'not' value
     * in_relation :   expr 'in' range
     *                 expr 'not' 'in' range
     * within_relation : expr 'within' range
     *                   expr 'not' 'within' range
     * expr :          'n'
     *                 'n' 'mod' value
     * value :         digit+
     * digit :         0|1|2|3|4|5|6|7|8|9
     * range :         value'..'value
     */
    private static Constraint parseConstraint(String description)
            throws ParseException {

        description = description.trim().toLowerCase(Locale.ENGLISH);

        Constraint result = null;
        String[] or_together = Utility.splitString(description, "or");
        for (int i = 0; i < or_together.length; ++i) {
            Constraint andConstraint = null;
            String[] and_together = Utility.splitString(or_together[i], "and");
            for (int j = 0; j < and_together.length; ++j) {
                Constraint newConstraint = NO_CONSTRAINT;

                String condition = and_together[j].trim();
                String[] tokens = Utility.splitWhitespace(condition);

                int mod = 0;
                boolean inRange = true;
                boolean integersOnly = true;
                double lowBound = Long.MAX_VALUE;
                double highBound = Long.MIN_VALUE;
                double[] vals = null;

                boolean isRange = false;

                int x = 0;
                String t = tokens[x++];
                Operand operand;
                try {
                    operand = NumberInfo.getOperand(t);
                } catch (Exception e) {
                    throw unexpected(t, condition);
                }
                if (x < tokens.length) {
                    t = tokens[x++];
                    if ("mod".equals(t)) {
                        mod = Integer.parseInt(tokens[x++]);
                        t = nextToken(tokens, x++, condition);
                    }
                    if ("is".equals(t)) {
                        t = nextToken(tokens, x++, condition);
                        if ("not".equals(t)) {
                            inRange = false;
                            t = nextToken(tokens, x++, condition);
                        }
                    } else {
                        isRange = true;
                        if ("not".equals(t)) {
                            inRange = false;
                            t = nextToken(tokens, x++, condition);
                        }
                        if ("in".equals(t)) {
                            t = nextToken(tokens, x++, condition);
                        } else if ("within".equals(t)) {
                            integersOnly = false;
                            t = nextToken(tokens, x++, condition);
                        } else {
                            throw unexpected(t, condition);
                        }
                    }

                    if (isRange) {
                        String[] range_list = Utility.splitString(t, ",");
                        vals = new double[range_list.length * 2];
                        for (int k1 = 0, k2 = 0; k1 < range_list.length; ++k1, k2 += 2) {
                            String range = range_list[k1];
                            String[] pair = Utility.splitString(range, "..");
                            double low, high;
                            if (pair.length == 2) {
                                low = Double.parseDouble(pair[0]);
                                high = Double.parseDouble(pair[1]);
                                if (low > high) {
                                    throw unexpected(range, condition);
                                }
                            } else if (pair.length == 1) {
                                low = high = Double.parseDouble(pair[0]);
                            } else {
                                throw unexpected(range, condition);
                            }
                            vals[k2] = low;
                            vals[k2+1] = high;
                            lowBound = Math.min(lowBound, low);
                            highBound = Math.max(highBound, high);
                        }
                        if (vals.length == 2) {
                            vals = null;
                        }
                    } else {
                        lowBound = highBound = Double.parseDouble(t);
                    }

                    if (x != tokens.length) {
                        throw unexpected(tokens[x], condition);
                    }

                    newConstraint =
                            new RangeConstraint(mod, inRange, operand, integersOnly, lowBound, highBound, vals);
                }

                if (andConstraint == null) {
                    andConstraint = newConstraint;
                } else {
                    andConstraint = new AndConstraint(andConstraint,
                            newConstraint);
                }
            }

            if (result == null) {
                result = andConstraint;
            } else {
                result = new OrConstraint(result, andConstraint);
            }
        }

        return result;
    }

    /* Returns a parse exception wrapping the token and context strings. */
    private static ParseException unexpected(String token, String context) {
        return new ParseException("unexpected token '" + token +
                "' in '" + context + "'", -1);
    }

    /*
     * Returns the token at x if available, else throws a parse exception.
     */
    private static String nextToken(String[] tokens, int x, String context)
            throws ParseException {
        if (x < tokens.length) {
            return tokens[x];
        }
        throw new ParseException("missing token at end of '" + context + "'", -1);
    }

    /*
     * Syntax:
     * rule : keyword ':' condition
     * keyword: <identifier>
     */
    private static Rule parseRule(String description) throws ParseException {
        int x = description.indexOf(':');
        if (x == -1) {
            throw new ParseException("missing ':' in rule description '" +
                    description + "'", 0);
        }

        String keyword = description.substring(0, x).trim();
        if (!isValidKeyword(keyword)) {
            throw new ParseException("keyword '" + keyword +
                    " is not valid", 0);
        }

        description = description.substring(x+1).trim();
        if (description.length() == 0) {
            throw new ParseException("missing constraint in '" +
                    description + "'", x+1);
        }
        Constraint constraint = parseConstraint(description);
        Rule rule = new ConstrainedRule(keyword, constraint);
        return rule;
    }

    /*
     * Syntax:
     * rules : rule
     *         rule ';' rules
     */
    private static RuleList parseRuleChain(String description)
            throws ParseException {
        RuleList result = new RuleList();
        // remove trailing ;
        if (description.endsWith(";")) { 
            description = description.substring(0,description.length()-1);
        }
        String[] rules = Utility.split(description, ';');
        for (int i = 0; i < rules.length; ++i) {
            result.addRule(parseRule(rules[i].trim()));
        }
        return result;
    }

    /*
     * An implementation of Constraint representing a modulus,
     * a range of values, and include/exclude. Provides lots of
     * convenience factory methods.
     */
    private static class RangeConstraint implements Constraint, Serializable {
        private static final long serialVersionUID = 1;

        private final int mod;
        private final boolean inRange;
        private final boolean integersOnly;
        private final double lowerBound;
        private final double upperBound;
        private final double[] range_list;
        private final Operand operand;

        RangeConstraint(int mod, boolean inRange, Operand operand, boolean integersOnly,
                double lowBound, double highBound, double[] vals) {
            this.mod = mod;
            this.inRange = inRange;
            this.integersOnly = integersOnly;
            this.lowerBound = lowBound;
            this.upperBound = highBound;
            this.range_list = vals;
            this.operand = operand;
        }

        public void getMentionedValues(Set<NumberInfo> toAddTo) {
            addRanges(toAddTo, mod);
            if (mod != 0) {
                //addRanges(toAddTo, mod*2);
                addRanges(toAddTo, mod*3);
            }
        }

        private void addRanges(Set<NumberInfo> toAddTo, int offset) {
            toAddTo.add(new NumberInfo(lowerBound + offset));
            if (upperBound != lowerBound) {
                toAddTo.add(new NumberInfo(upperBound + offset));
            }
            //            if (range_list != null) {
            //                // add from each range
            //                for (int i = 0; i < range_list.length; i += 2) {
            //                    double lower = range_list[i];
            //                    double upper = range_list[i+1];
            //                    if (lower != lowerBound) {
            //                        toAddTo.add(new NumberInfo(lower + offset)); 
            //                    }
            //                    if (upper != upperBound) {
            //                        toAddTo.add(new NumberInfo(upper + offset)); 
            //                    }
            //                }
            //            }
            if (!integersOnly) {
                double average = (lowerBound + upperBound) / 2.0d;
                toAddTo.add(new NumberInfo(average + offset));
                //                if (range_list != null) {
                //                    // we will just add one value from the middle
                //                    for (int i = 0; i < range_list.length; i += 2) {
                //                        double lower = range_list[i];
                //                        double upper = range_list[i+1];
                //                        toAddTo.add(new NumberInfo((lower + upper) / 2.0d + offset)); 
                //                    }
                //                }
            }
        }

        public boolean isFulfilled(NumberInfo number) {
            double n = number.get(operand);
            if ((integersOnly && (n - (long)n) != 0.0
                    || operand == Operand.j && number.visibleFractionDigitCount != 0)) {
                return !inRange;
            }
            if (mod != 0) {
                n = n % mod;    // java % handles double numerator the way we want
            }
            boolean test = n >= lowerBound && n <= upperBound;
            if (test && range_list != null) {
                test = false;
                for (int i = 0; !test && i < range_list.length; i += 2) {
                    test = n >= range_list[i] && n <= range_list[i+1];
                }
            }
            return inRange == test;
        }

        public boolean isLimited() {
            return integersOnly && inRange && mod == 0;
        }

        public int updateRepeatLimit(int limit) {
            int mylimit = mod == 0 ? (int)upperBound : mod;
            return Math.max(mylimit, limit);
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(operand);
            if (mod != 0) {
                result.append(" mod ").append(mod);
            }
            boolean isList = lowerBound != upperBound;
            result.append(
                    !isList ? (inRange ? " is " : " is not ")
                            : integersOnly ? (inRange ? " in " : " not in ")
                                    : (inRange ? " within " : " not within ") 
                    );
            if (range_list != null) {
                for (int i = 0; i < range_list.length; i += 2) {
                    addRange(result, range_list[i], range_list[i+1], i != 0);
                }
            } else {
                addRange(result, lowerBound, upperBound, false);
            }
            return result.toString();
        }
    }

    private static void addRange(StringBuilder result, double lb, double ub, boolean addSeparator) {
        if (addSeparator) {
            result.append(",");
        }
        if (lb == ub) {
            result.append(format(lb));
        } else {
            result.append(format(lb) + ".." + format(ub));
        }
    }

    private static String format(double lb) {
        long lbi = (long) lb;
        return lb == lbi ? String.valueOf(lbi) : String.valueOf(lb);
    }

    /* Convenience base class for and/or constraints. */
    private static abstract class BinaryConstraint implements Constraint,
    Serializable {
        private static final long serialVersionUID = 1;
        protected final Constraint a;
        protected final Constraint b;

        protected BinaryConstraint(Constraint a, Constraint b) {
            this.a = a;
            this.b = b;
        }

        public int updateRepeatLimit(int limit) {
            return a.updateRepeatLimit(b.updateRepeatLimit(limit));
        }
    }

    /* A constraint representing the logical and of two constraints. */
    private static class AndConstraint extends BinaryConstraint {
        private static final long serialVersionUID = 7766999779862263523L;

        AndConstraint(Constraint a, Constraint b) {
            super(a, b);
        }

        public boolean isFulfilled(NumberInfo n) {
            return a.isFulfilled(n) && b.isFulfilled(n);
        }

        public boolean isLimited() {
            // we ignore the case where both a and b are unlimited but no values
            // satisfy both-- we still consider this 'unlimited'
            return a.isLimited() || b.isLimited();
        }

        public void getMentionedValues(Set<NumberInfo> toAddTo) {
            a.getMentionedValues(toAddTo);
            b.getMentionedValues(toAddTo);
        }

        public String toString() {
            return a.toString() + " and " + b.toString();
        }
    }

    /* A constraint representing the logical or of two constraints. */
    private static class OrConstraint extends BinaryConstraint {
        private static final long serialVersionUID = 1405488568664762222L;

        OrConstraint(Constraint a, Constraint b) {
            super(a, b);
        }

        public boolean isFulfilled(NumberInfo n) {
            return a.isFulfilled(n) || b.isFulfilled(n);
        }

        public boolean isLimited() {
            return a.isLimited() && b.isLimited();
        }

        public void getMentionedValues(Set<NumberInfo> toAddTo) {
            a.getMentionedValues(toAddTo);
            b.getMentionedValues(toAddTo);
        }
        public String toString() {
            return a.toString() + " or " + b.toString();
        }
    }

    /*
     * Implementation of Rule that uses a constraint.
     * Provides 'and' and 'or' to combine constraints.  Immutable.
     */
    private static class ConstrainedRule implements Rule, Serializable {
        private static final long serialVersionUID = 1;
        private final String keyword;
        private final Constraint constraint;

        public ConstrainedRule(String keyword, Constraint constraint) {
            this.keyword = keyword;
            this.constraint = constraint;
        }

        @SuppressWarnings("unused")
        public Rule and(Constraint c) {
            return new ConstrainedRule(keyword, new AndConstraint(constraint, c));
        }

        @SuppressWarnings("unused")
        public Rule or(Constraint c) {
            return new ConstrainedRule(keyword, new OrConstraint(constraint, c));
        }

        public String getKeyword() {
            return keyword;
        }

        public boolean appliesTo(NumberInfo n) {
            return constraint.isFulfilled(n);
        }

        public int updateRepeatLimit(int limit) {
            return constraint.updateRepeatLimit(limit);
        }

        public boolean isLimited() {
            return constraint.isLimited();
        }

        public String toString() {
            return keyword + ": " + constraint.toString();
        }

        public String getConstraint() {
            return constraint.toString();
        }

        /**
         * Gets samples of significant numbers
         */
        public void getMentionedValues(Set<NumberInfo> toAddTo) {
            constraint.getMentionedValues(toAddTo);
        }
    }

    /*
     * Implementation of RuleList that is itself a node in a linked list.
     * Immutable, but supports chaining with 'addRule'.
     */
    private static class RuleList implements Serializable {
        private static final long serialVersionUID = 1;
        private final List<Rule> rules = new ArrayList<Rule>();

        public RuleList addRule(Rule nextRule) {
            rules.add(nextRule);
            return this;
        }

        private Rule selectRule(NumberInfo n) {
            for (Rule rule : rules) {
                if (rule.appliesTo(n)) {
                    return rule;
                }
            }
            return null;
        }

        public String select(NumberInfo n) {
            Rule r = selectRule(n);
            if (r == null) {
                return KEYWORD_OTHER;
            }
            return r.getKeyword();
        }

        public Set<String> getKeywords() {
            Set<String> result = new HashSet<String>();
            for (Rule rule : rules) {
                result.add(rule.getKeyword());
            }
            result.add(KEYWORD_OTHER);
            return result;
        }

        public boolean isLimited(String keyword) {
            // if all rules with this keyword are limited, it's limited,
            // and if there's no rule with this keyword, it's unlimited
            boolean result = false;
            for (Rule rule : rules) {
                if (keyword.equals(rule.getKeyword())) {
                    if (!rule.isLimited()) {
                        return false;
                    }
                    result = true;
                }
            }
            return result;
        }

        public int getRepeatLimit() {
            int result = 0;
            for (Rule rule : rules) {
                result = rule.updateRepeatLimit(result);
            }
            return result;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            Map<String, String> ordered = new TreeMap<String, String>(KEYWORD_COMPARATOR);
            for (Rule rule : rules) {
                String keyword = rule.getKeyword();
                if (keyword.equals("other")) {
                    continue;
                }
                String constraint = rule.getConstraint();
                ordered.put(keyword, constraint);
            }
            for (Entry<String, String> entry : ordered.entrySet()) {
                if (builder.length() != 0) {
                    builder.append(CATEGORY_SEPARATOR);
                }
                builder.append(entry.getKey()).append(KEYWORD_RULE_SEPARATOR).append(entry.getValue());
            }
            return builder.toString();
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.text.PluralRules.RuleList#getMentionedSamples(java.util.Set)
         */
        public Set<NumberInfo> getMentionedValues(Set<NumberInfo> toAddTo) {
            for (Rule rule : rules) {
                rule.getMentionedValues(toAddTo);
            }
            return toAddTo;
        }


        public String getRules(String keyword) {
            for (Rule rule : rules) {
                if (rule.getKeyword().equals(keyword)) {
                    return rule.getConstraint();
                }
            }
            return null;
        }
    }

    enum StandardPluralCategories {
        zero,
        one,
        two,
        few,
        many,
        other;
        static StandardPluralCategories forString(String s) {
            StandardPluralCategories a;
            try {
                a = valueOf(s);
            } catch (Exception e) {
                return null;
            }
            return a;
        }
    }

    private static final int[] TENS = {1, 10, 100, 1000, 10000, 100000, 1000000};

    private static final int LIMIT_FRACTION_SAMPLES = 3;

    private Set<NumberInfo> fractions(Set<NumberInfo> original) {
        Set<NumberInfo> toAddTo = new HashSet<NumberInfo>();

        Set<Integer> result = new HashSet<Integer>();
        for (NumberInfo base1 : original) {
            result.add((int)base1.intValue);
        }
        List<Integer> ints = new ArrayList<Integer>(result);
        Set<String> keywords = new HashSet<String>();

        for (int j = 0; j < ints.size(); ++j) {
            Integer base = ints.get(j);
            String keyword = select(base);
            if (keywords.contains(keyword)) {
                continue;
            }
            keywords.add(keyword);
            toAddTo.add(new NumberInfo(base,1)); // add .0
            toAddTo.add(new NumberInfo(base,2)); // add .00
            Integer fract = getDifferentCategory(ints, keyword);
            if (fract >= TENS[LIMIT_FRACTION_SAMPLES-1]) { // make sure that we always get the value
                toAddTo.add(new NumberInfo(base + "." + fract));
            } else {
                for (int visibleFractions = 1; visibleFractions < LIMIT_FRACTION_SAMPLES; ++visibleFractions) {
                    for (int i = 1; i <= visibleFractions; ++i) {
                        // with visible fractions = 3, and fract = 1, then we should get x.10, 0.01
                        // with visible fractions = 3, and fract = 15, then we should get x.15, x.15
                        if (fract >= TENS[i]) {
                            continue;
                        }
                        toAddTo.add(new NumberInfo(base + fract/(double)TENS[i], visibleFractions));
                    }
                }
            }
        }
        return toAddTo;
    }

    /**
     * @param ints
     * @param base
     * @return
     */
    private Integer getDifferentCategory(List<Integer> ints, String keyword) {
        for (int i = ints.size() - 1; i >= 0; --i) {
            Integer other = ints.get(i);
            String keywordOther = select(other);
            if (!keywordOther.equals(keyword)) {
                return other;
            }
        }
        return 37;
    }

    private boolean addConditional(Set<NumberInfo> toAddTo, Set<NumberInfo> others, double trial) {
        boolean added;
        NumberInfo toAdd = new NumberInfo(trial);
        if (!toAddTo.contains(toAdd) && !others.contains(toAdd)) {
            others.add(toAdd);
            added = true;
        } else {
            added = false;
        }
        return added;
    }


    /**
     * @deprecated This API is ICU internal only.
     * @internal
     */
    public static final Comparator<String> KEYWORD_COMPARATOR = new Comparator<String> () {
        public int compare(String arg0, String arg1) {
            StandardPluralCategories a = StandardPluralCategories.forString(arg0);
            StandardPluralCategories b = StandardPluralCategories.forString(arg1);
            return a == null 
                    ? (b == null ? arg0.compareTo(arg1) : -1)
                            : (b == null ? 1 : a.compareTo(b));
        }
    };


    // -------------------------------------------------------------------------
    // Static class methods.
    // -------------------------------------------------------------------------

    /**
     * Provides access to the predefined cardinal-number <code>PluralRules</code> for a given
     * locale.
     * Same as forLocale(locale, PluralType.CARDINAL).
     *
     * <p>ICU defines plural rules for many locales based on CLDR <i>Language Plural Rules</i>.
     * For these predefined rules, see CLDR page at
     * http://unicode.org/repos/cldr-tmp/trunk/diff/supplemental/language_plural_rules.html
     *
     * @param locale The locale for which a <code>PluralRules</code> object is
     *   returned.
     * @return The predefined <code>PluralRules</code> object for this locale.
     *   If there's no predefined rules for this locale, the rules
     *   for the closest parent in the locale hierarchy that has one will
     *   be returned.  The final fallback always returns the default
     *   rules.
     * @stable ICU 3.8
     */
    public static PluralRules forLocale(ULocale locale) {
        return Factory.getDefaultFactory().forLocale(locale, PluralType.CARDINAL);
    }

    /**
     * Provides access to the predefined <code>PluralRules</code> for a given
     * locale and the plural type.
     *
     * <p>ICU defines plural rules for many locales based on CLDR <i>Language Plural Rules</i>.
     * For these predefined rules, see CLDR page at
     * http://unicode.org/repos/cldr-tmp/trunk/diff/supplemental/language_plural_rules.html
     *
     * @param locale The locale for which a <code>PluralRules</code> object is
     *   returned.
     * @param type The plural type (e.g., cardinal or ordinal).
     * @return The predefined <code>PluralRules</code> object for this locale.
     *   If there's no predefined rules for this locale, the rules
     *   for the closest parent in the locale hierarchy that has one will
     *   be returned.  The final fallback always returns the default
     *   rules.
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    public static PluralRules forLocale(ULocale locale, PluralType type) {
        return Factory.getDefaultFactory().forLocale(locale, type);
    }

    /*
     * Checks whether a token is a valid keyword.
     *
     * @param token the token to be checked
     * @return true if the token is a valid keyword.
     */
    private static boolean isValidKeyword(String token) {
        return PatternProps.isIdentifier(token);
    }

    /*
     * Creates a new <code>PluralRules</code> object.  Immutable.
     */
    private PluralRules(RuleList rules) {
        this.rules = rules;
        TreeSet<String> temp = new TreeSet<String>(KEYWORD_COMPARATOR);
        temp.addAll(rules.getKeywords());
        this.keywords = Collections.unmodifiableSet(new LinkedHashSet<String>(temp));
    }

    /**
     * Given a number, returns the keyword of the first rule that applies to
     * the number.
     *
     * @param number The number for which the rule has to be determined.
     * @return The keyword of the selected rule.
     * @stable ICU 4.0
     */
    public String select(double number) {
        return rules.select(new NumberInfo(number));
    }

    /**
     * Given a number, returns the keyword of the first rule that applies to
     * the number.
     *
     * @param number The number for which the rule has to be determined.
     * @return The keyword of the selected rule.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public String select(double number, int countVisibleFractionDigits, long fractionaldigits) {
        return rules.select(new NumberInfo(number, countVisibleFractionDigits, fractionaldigits));
    }

    
    /**
     * Given a number, returns the keyword of the first rule that applies to
     * the number.
     *
     * @param number The number for which the rule has to be determined.
     * @return The keyword of the selected rule.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public String select(NumberInfo sample) {
        return rules.select(sample);
    }

    /**
     * Returns a set of all rule keywords used in this <code>PluralRules</code>
     * object.  The rule "other" is always present by default.
     *
     * @return The set of keywords.
     * @stable ICU 3.8
     */
    public Set<String> getKeywords() {
        return keywords;
    }

    /**
     * Returns the unique value that this keyword matches, or {@link #NO_UNIQUE_VALUE}
     * if the keyword matches multiple values or is not defined for this PluralRules.
     *
     * @param keyword the keyword to check for a unique value
     * @return The unique value for the keyword, or NO_UNIQUE_VALUE.
     * @stable ICU 4.8
     */
    public double getUniqueKeywordValue(String keyword) {
        Collection<Double> values = getAllKeywordValues(keyword);
        if (values != null && values.size() == 1) {
            return values.iterator().next();
        }
        return NO_UNIQUE_VALUE;
    }

    /**
     * Returns all the values that trigger this keyword, or null if the number of such
     * values is unlimited.
     *
     * @param keyword the keyword
     * @return the values that trigger this keyword, or null.  The returned collection
     * is immutable. It will be empty if the keyword is not defined.
     * @stable ICU 4.8
     */
    public Collection<Double> getAllKeywordValues(String keyword) {
        // HACK for now
        if (!keywords.contains(keyword)) {
            return Collections.<Double>emptyList();
        }
        Collection<Double> result = getKeySamplesMap().get(keyword);

        // We depend on MAX_SAMPLES here.  It's possible for a conjunction
        // of unlimited rules that 'looks' unlimited to return a limited
        // number of values.  There's no bounds to this limited number, in
        // general, because you can construct arbitrarily complex rules.  Since
        // we always generate 3 samples if a rule is really unlimited, that's
        // where we put the cutoff.
        if (result.size() > 2 && !getKeyLimitedMap().get(keyword)) {
            return null;
        }
        return result;
    }


    /**
     * Returns a list of values for which select() would return that keyword,
     * or null if the keyword is not defined. The returned collection is unmodifiable.
     * The returned list is not complete, and there might be additional values that
     * would return the keyword.
     *
     * @param keyword the keyword to test
     * @return a list of values matching the keyword.
     * @stable ICU 4.8
     */
    public Collection<Double> getSamples(String keyword) {
        if (!keywords.contains(keyword)) {
            return null;
        }
        return getKeySamplesMap().get(keyword);
    }

    /**
     * Returns a list of values for which select() would return that keyword,
     * or null if the keyword is not defined. The returned collection is unmodifiable.
     * The returned list is not complete, and there might be additional values that
     * would return the keyword.
     *
     * @param keyword the keyword to test
     * @return a list of values matching the keyword.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Collection<NumberInfo> getFractionSamples(String keyword) {
        if (!keywords.contains(keyword)) {
            return null;
        }
        initKeyMaps();
        return _keyFractionSamplesMap.get(keyword);
    }

    /**
     * Returns a list of values that includes at least one value for each keyword.
     *
     * @return a list of values
     * @internal
     */
    public Collection<NumberInfo> getFractionSamples() {
        initKeyMaps();
        return _fractionSamples;
    }

    private Map<String, Boolean> getKeyLimitedMap() {
        initKeyMaps();
        return _keyLimitedMap;
    }

    private Map<String, List<Double>> getKeySamplesMap() {
        initKeyMaps();
        return _keySamplesMap;
    }

    private synchronized void initKeyMaps() {
        // ensure both _keySamplesMap and _keyLimitedMap are initialized.
        if (_keySamplesMap == null) {
            // If this were allowed to vary on a per-call basis, we'd have to recheck and
            // possibly rebuild the samples cache.  Doesn't seem worth it.
            // This 'max samples' value only applies to keywords that are unlimited, for
            // other keywords all the matching values are returned.  This might be a lot.
            final int MAX_SAMPLES = 3;

            Map<String, Boolean> temp = new HashMap<String, Boolean>();
            for (String k : keywords) {
                temp.put(k, rules.isLimited(k));
            }
            _keyLimitedMap = temp;

            Map<String, List<Double>> sampleMap = new HashMap<String, List<Double>>();
            int keywordsRemaining = keywords.size();

            int limit = Math.max(5, getRepeatLimit() * MAX_SAMPLES) * 2;

            for (int i = 0; keywordsRemaining > 0 && i < limit; ++i) {
                double val = i / 2.0;
                String keyword = select(val);
                boolean keyIsLimited = _keyLimitedMap.get(keyword);

                List<Double> list = sampleMap.get(keyword);
                if (list == null) {
                    list = new ArrayList<Double>(MAX_SAMPLES);
                    sampleMap.put(keyword, list);
                } else if (!keyIsLimited && list.size() == MAX_SAMPLES) {
                    continue;
                }
                list.add(Double.valueOf(val));

                if (!keyIsLimited && list.size() == MAX_SAMPLES) {
                    --keywordsRemaining;
                }
            }

            // collect explicit samples
            Map<String, Set<NumberInfo>> sampleFractionMap = new HashMap<String, Set<NumberInfo>>();
            Set<NumberInfo> mentioned = rules.getMentionedValues(new TreeSet<NumberInfo>());
            // make sure that there is at least one 'other' value
            Map<String, Set<NumberInfo>> foundKeywords = new HashMap<String, Set<NumberInfo>>();
            for (NumberInfo s : mentioned) {
                String keyword = this.select(s);
                addRelation(foundKeywords, keyword, s);
            }
            main:
                if (foundKeywords.size() != keywords.size()) {
                    for (int i = 1; i < 1000; ++i) {
                        boolean done = addIfNotPresent(i, mentioned, foundKeywords);
                        if (done) break main;
                    }
                    // if we are not done, try tenths
                    for (int i = 10; i < 1000; ++i) {
                        boolean done = addIfNotPresent(i/10d, mentioned, foundKeywords);
                        if (done) break main;
                    }
                    System.out.println("Failed to find sample for each keyword: " + foundKeywords + "\n\t" + rules + "\n\t" + mentioned);
                }
            mentioned.add(new NumberInfo(0)); // always there
            mentioned.add(new NumberInfo(1)); // always there
            mentioned.add(new NumberInfo(2)); // always there
            mentioned.add(new NumberInfo(0.1,1)); // always there
            mentioned.add(new NumberInfo(1.99,2)); // always there
            mentioned.addAll(fractions(mentioned));
            //            Set<NumberInfo> toAddTo = mentioned;
            //            {
            //                // once done, manufacture values for the OTHER case
            //                int otherCount = 2;
            //                for (int i = 0; i < 1000; ++i) {
            //                }
            //                NumberInfo last = null;
            //                Set<NumberInfo> others = new LinkedHashSet<NumberInfo>();
            //                for (NumberInfo s : toAddTo) {
            //                    double trial;
            //                    if (last == null) {
            //                        trial = s.source-0.5;
            //                    } else {
            //                        double diff = s.source - last.source;
            //                        if (diff > 1.0d) {
            //                            trial = Math.floor(s.source);
            //                            if (trial == s.source) {
            //                                --trial;
            //                            }
            //                        } else {
            //                            trial = (s.source + last.source) / 2;
            //                        }                     
            //                    }
            //                    if (trial >= 0) {
            //                        addConditional(toAddTo, others, trial);
            //                    }
            //                    last = s;
            //                }
            //                double trial = last == null ? 0 : last.source;
            //                double fraction = 0;
            //                while (otherCount > 0) {
            //                    if (addConditional(toAddTo, others, trial = trial * 2 + 1 + fraction)) {
            //                        --otherCount;
            //                    }
            //                    fraction += 0.125;
            //                }
            //                toAddTo.addAll(others);
            //                others.clear();
            //                toAddTo.addAll(fractions(toAddTo, others));
            //
            //            }
            for (NumberInfo s : mentioned) {
                String keyword = select(s);
                Set<NumberInfo> list = sampleFractionMap.get(keyword);
                if (list == null) {
                    list = new LinkedHashSet<NumberInfo>(); // will be sorted because the iteration is
                    sampleFractionMap.put(keyword, list);
                }
                list.add(s);
            }

            if (keywordsRemaining > 0) {
                for (String k : keywords) {
                    if (!sampleMap.containsKey(k)) {
                        sampleMap.put(k, Collections.<Double>emptyList());
                    }
                    if (!sampleFractionMap.containsKey(k)) {
                        sampleFractionMap.put(k, Collections.<NumberInfo>emptySet());
                    }
                }
            }

            // Make lists immutable so we can return them directly
            for (Entry<String, List<Double>> entry : sampleMap.entrySet()) {
                sampleMap.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
            }
            for (Entry<String, Set<NumberInfo>> entry : sampleFractionMap.entrySet()) {
                sampleFractionMap.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
            }
            _keySamplesMap = sampleMap;
            _keyFractionSamplesMap = sampleFractionMap;
            _fractionSamples = Collections.unmodifiableSet(mentioned);
        }
    }

    private void addRelation(Map<String, Set<NumberInfo>> foundKeywords, String keyword, NumberInfo s) {
        Set<NumberInfo> set = foundKeywords.get(keyword);
        if (set == null) {
            foundKeywords.put(keyword, set = new HashSet<NumberInfo>());
        }
        set.add(s);
    }

    private boolean addIfNotPresent(double d, Set<NumberInfo> mentioned, Map<String, Set<NumberInfo>> foundKeywords) {
        NumberInfo numberInfo = new NumberInfo(d);
        String keyword = this.select(numberInfo);
        if (!foundKeywords.containsKey(keyword) || keyword.equals("other")) {
            addRelation(foundKeywords, keyword, numberInfo);
            mentioned.add(numberInfo);
            if (keyword.equals("other")) {
                if (foundKeywords.get("other").size() > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the set of locales for which PluralRules are known.
     * @return the set of locales for which PluralRules are known, as a list
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static ULocale[] getAvailableULocales() {
        return Factory.getDefaultFactory().getAvailableULocales();
    }

    /**
     * Returns the 'functionally equivalent' locale with respect to
     * plural rules.  Calling PluralRules.forLocale with the functionally equivalent
     * locale, and with the provided locale, returns rules that behave the same.
     * <br/>
     * All locales with the same functionally equivalent locale have
     * plural rules that behave the same.  This is not exaustive;
     * there may be other locales whose plural rules behave the same
     * that do not have the same equivalent locale.
     *
     * @param locale the locale to check
     * @param isAvailable if not null and of length > 0, this will hold 'true' at
     * index 0 if locale is directly defined (without fallback) as having plural rules
     * @return the functionally-equivalent locale
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
        return Factory.getDefaultFactory().getFunctionalEquivalent(locale, isAvailable);
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    public String toString() {
        return rules.toString();
    }


    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    public int hashCode() {
        if (hashCode == 0) {
            // cache it
            int newHashCode = keywords.hashCode();
            for (int i = 0; i < 12; ++i) {
                newHashCode = newHashCode * 31 + select(i).hashCode();
            }
            if (newHashCode == 0) {
                newHashCode = 1;
            }
            hashCode = newHashCode;
        }
        return hashCode;
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    public boolean equals(Object rhs) {
        return rhs instanceof PluralRules && equals((PluralRules)rhs);
    }

    /**
     * Returns true if rhs is equal to this.
     * @param rhs the PluralRules to compare to.
     * @return true if this and rhs are equal.
     * @stable ICU 3.8
     */
    public boolean equals(PluralRules rhs) {
        if (rhs == null) {
            return false;
        }
        if (rhs == this) {
            return true;
        }

        if (hashCode() != rhs.hashCode()) {
            return false;
        }

        if (!rhs.getKeywords().equals(keywords)) {
            return false;
        }

        for (String keyword : rhs.getKeywords()) {
            String rules2 = getRules(keyword);
            String rules3 = rhs.getRules(keyword);
            if (rules2 != rules3) {
                if (rules2 == null || !rules2.equals(rules3)) {
                    return false;
                }
            }
        }
        //        int limit = Math.max(getRepeatLimit(), rhs.getRepeatLimit());
        //        for (int i = 0; i < limit * 2; ++i) {
        //            if (!select(i).equals(rhs.select(i))) {
        //                return false;
        //            }
        //        }
        return true;
    }

    private int getRepeatLimit() {
        if (repeatLimit == 0) {
            repeatLimit = rules.getRepeatLimit() + 1;
        }
        return repeatLimit;
    }

    /**
     * Status of the keyword for the rules, given a set of explicit values.
     * 
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    public enum KeywordStatus {
        /**
         * The keyword is not valid for the rules.
         * 
         * @draft ICU 50
         * @provisional This API might change or be removed in a future release.
         */
        INVALID,
        /**
         * The keyword is valid, but unused (it is covered by the explicit values).
         * 
         * @draft ICU 50
         * @provisional This API might change or be removed in a future release.
         */
        SUPPRESSED,
        /**
         * The keyword is valid, used, and has a single possible value (before considering explicit values).
         * 
         * @draft ICU 50
         * @provisional This API might change or be removed in a future release.
         */
        UNIQUE,
        /**
         * The keyword is valid, used, not unique, and has a finite set of values.
         * 
         * @draft ICU 50
         * @provisional This API might change or be removed in a future release.
         */
        BOUNDED,
        /**
         * The keyword is valid but not bounded; there indefinitely many matching values.
         * 
         * @draft ICU 50
         * @provisional This API might change or be removed in a future release.
         */
        UNBOUNDED
    }

    /**
     * Find the status for the keyword, given a certain set of explicit values.
     * 
     * @param keyword
     *            the particular keyword (call rules.getKeywords() to get the valid ones)
     * @param offset
     *            the offset used, or 0.0d if not. Internally, the offset is subtracted from each explicit value before
     *            checking against the keyword values.
     * @param explicits
     *            a set of Doubles that are used explicitly (eg [=0], "[=1]"). May be empty or null.
     * @param uniqueValue
     *            If non null, set to the unique value.
     * @return the KeywordStatus
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    public KeywordStatus getKeywordStatus(String keyword, int offset, Set<Double> explicits,
            Output<Double> uniqueValue) {

        if (uniqueValue != null) {
            uniqueValue.value = null;
        }

        if (!rules.getKeywords().contains(keyword)) {
            return KeywordStatus.INVALID;
        }
        Collection<Double> values = getAllKeywordValues(keyword);
        if (values == null) {
            return KeywordStatus.UNBOUNDED;
        }
        int originalSize = values.size();

        if (explicits == null) {
            explicits = Collections.emptySet();
        }

        // Quick check on whether there are multiple elements

        if (originalSize > explicits.size()) {
            if (originalSize == 1) {
                if (uniqueValue != null) {
                    uniqueValue.value = values.iterator().next();
                }
                return KeywordStatus.UNIQUE;
            }
            return KeywordStatus.BOUNDED;
        }

        // Compute if the quick test is insufficient.

        HashSet<Double> subtractedSet = new HashSet<Double>(values);
        for (Double explicit : explicits) {
            subtractedSet.remove(explicit - offset);
        }
        if (subtractedSet.size() == 0) {
            return KeywordStatus.SUPPRESSED;
        }

        if (uniqueValue != null && subtractedSet.size() == 1) {
            uniqueValue.value = subtractedSet.iterator().next();
        }

        return originalSize == 1 ? KeywordStatus.UNIQUE : KeywordStatus.BOUNDED;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public String getRules(String keyword) {
        return rules.getRules(keyword);
    }

    private void writeObject(
            ObjectOutputStream out)
                    throws IOException {
        throw new NotSerializableException();
    }
    private void readObject(ObjectInputStream in
            ) throws IOException, ClassNotFoundException {
        throw new NotSerializableException();
    }
    private void readObjectNoData( 
            ) throws ObjectStreamException {
        throw new NotSerializableException();
    }
    private Object writeReplace() throws ObjectStreamException {
        return new PluralRulesSerialProxy(toString());
    }
}
