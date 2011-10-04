/*
 *******************************************************************************
 * Copyright (C) 2007-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.icu.impl.PatternProps;
import com.ibm.icu.impl.PluralRulesLoader;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.ULocale;

/**
 * <p>Defines rules for mapping non-negative numeric values onto a small set of
 * keywords. Serializable so can be used in formatters, which are
 * serializable. Rules are constructed from a text description, consisting
 * of a series of keywords and conditions.  The {@link #select} method
 * examines each condition in order and returns the keyword for the
 * first condition that matches the number.  If none match,
 * {@link #KEYWORD_OTHER} is returned.</p>
 *
 * <p>For more information, details, and tips for writing rules, see the
 * <a href="http://www.unicode.org/draft/reports/tr35/tr35.html#Language_Plural_Rules">LDML spec,
 * C.11 Language Plural Rules</a></p>
 *
 * <p>
 * Examples:<pre>
 *   "one: n is 1; few: n in 2..4"</pre></p>
 * <p>
 * This defines two rules, for 'one' and 'few'.  The condition for
 * 'one' is "n is 1" which means that the number must be equal to
 * 1 for this condition to pass.  The condition for 'few' is
 * "n in 2..4" which means that the number must be between 2 and
 * 4 inclusive - and be an integer - for this condition to pass. All other
 * numbers are assigned the keyword "other" by the default rule.</p>
 * <p><pre>
 *   "zero: n is 0; one: n is 1; zero: n mod 100 in 1..19"</pre>
 * This illustrates that the same keyword can be defined multiple times.
 * Each rule is examined in order, and the first keyword whose condition
 * passes is the one returned.  Also notes that a modulus is applied
 * to n in the last rule.  Thus its condition holds for 119, 219, 319...</p>
 * <p><pre>
 *   "one: n is 1; few: n mod 10 in 2..4 and n mod 100 not in 12..14"</pre></p>
 * <p>
 * This illustrates conjunction and negation.  The condition for 'few'
 * has two parts, both of which must be met: "n mod 10 in 2..4" and
 * "n mod 100 not in 12..14".  The first part applies a modulus to n
 * before the test as in the previous example.  The second part applies
 * a different modulus and also uses negation, thus it matches all
 * numbers _not_ in 12, 13, 14, 112, 113, 114, 212, 213, 214...</p>
 * <p>
 * Syntax:<pre>
 * rules         = rule (';' rule)*
 * rule          = keyword ':' condition
 * keyword       = &lt;identifier&gt;
 * condition     = and_condition ('or' and_condition)*
 * and_condition = relation ('and' relation)*
 * relation      = is_relation | in_relation | within_relation | 'n' <EOL>
 * is_relation   = expr 'is' ('not')? value
 * in_relation   = expr ('not')? 'in' range_list
 * within_relation = expr ('not')? 'within' range_list
 * expr          = 'n' ('mod' value)?
 * range_list    = (range | value) (',' range_list)*
 * value         = digit+
 * digit         = 0|1|2|3|4|5|6|7|8|9
 * range         = value'..'value
 * </pre></p>
 * <p>
 * An "identifier" is a sequence of characters that do not have the
 * Unicode Pattern_Syntax or Pattern_White_Space properties.
 * <p>
 * The difference between 'in' and 'within' is that 'in' only includes
 * integers in the specified range, while 'within' includes all values.
 * Using 'within' with a range_list consisting entirely of values
 * is the same as using 'in' (it's not an error).</p>
 * @stable ICU 3.8
 */
public class PluralRules implements Serializable {
    private static final long serialVersionUID = 1;

    private final RuleList rules;
    private final Set<String> keywords;
    private int repeatLimit; // for equality test
    private transient int hashCode;
    private transient Map<String, List<Double>> _keySamplesMap;
    private transient Map<String, Boolean> _keyLimitedMap;

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
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release.
     */
    public static final double NO_UNIQUE_VALUE = -0.00123456777;

    /*
     * The default constraint that is always satisfied.
     */
    private static final Constraint NO_CONSTRAINT = new Constraint() {
        private static final long serialVersionUID = 9163464945387899416L;

        public boolean isFulfilled(double n) {
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
    };

    /*
     * The default rule that always returns "other".
     */
    private static final Rule DEFAULT_RULE = new Rule() {
        private static final long serialVersionUID = -5677499073940822149L;

        public String getKeyword() {
            return KEYWORD_OTHER;
        }

        public boolean appliesTo(double n) {
            return true;
        }

        public boolean isLimited() {
            return false;
        }

        public String toString() {
            return "(" + KEYWORD_OTHER + ")";
        }

        public int updateRepeatLimit(int limit) {
            return limit;
        }
    };

    /**
     * The default rules that accept any number and return
     * {@link #KEYWORD_OTHER}.
     * @stable ICU 3.8
     */
    public static final PluralRules DEFAULT =
        new PluralRules(new RuleChain(DEFAULT_RULE));

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

    /*
     * A constraint on a number.
     */
    private interface Constraint extends Serializable {
        /*
         * Returns true if the number fulfills the constraint.
         * @param n the number to test, >= 0.
         */
        boolean isFulfilled(double n);

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
    }

    /*
     * A pluralization rule.
     */
    private interface Rule extends Serializable {
        /* Returns the keyword that names this rule. */
        String getKeyword();

        /* Returns true if the rule applies to the number. */
        boolean appliesTo(double n);

        /* Returns false if an unlimited number of values generate this rule. */
        boolean isLimited();

        /* Returns the larger of limit and this rule's limit. */
        int updateRepeatLimit(int limit);
    }

    /*
     * A list of rules to apply in order.
     */
    private interface RuleList extends Serializable {
        /* Returns the keyword of the first rule that applies to the number. */
        String select(double n);

        /* Returns the set of defined keywords. */
        Set<String> getKeywords();

        /* Return the value at which this rulelist starts repeating. */
        int getRepeatLimit();

        /* Return true if the values for this keyword are limited. */
        boolean isLimited(String keyword);
    }

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
                long lowBound = Long.MAX_VALUE;
                long highBound = Long.MIN_VALUE;
                long[] vals = null;

                boolean isRange = false;

                int x = 0;
                String t = tokens[x++];
                if (!"n".equals(t)) {
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
                        vals = new long[range_list.length * 2];
                        for (int k1 = 0, k2 = 0; k1 < range_list.length; ++k1, k2 += 2) {
                            String range = range_list[k1];
                            String[] pair = Utility.splitString(range, "..");
                            long low, high;
                            if (pair.length == 2) {
                                low = Long.parseLong(pair[0]);
                                high = Long.parseLong(pair[1]);
                                if (low > high) {
                                    throw unexpected(range, condition);
                                }
                            } else if (pair.length == 1) {
                                low = high = Long.parseLong(pair[0]);
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
                        lowBound = highBound = Long.parseLong(t);
                    }

                    if (x != tokens.length) {
                        throw unexpected(tokens[x], condition);
                    }

                    newConstraint =
                        new RangeConstraint(mod, inRange, integersOnly, lowBound, highBound, vals);
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
    private static RuleChain parseRuleChain(String description)
        throws ParseException {

        RuleChain rc = null;
        String[] rules = Utility.split(description, ';');
        for (int i = 0; i < rules.length; ++i) {
            Rule r = parseRule(rules[i].trim());
            if (rc == null) {
                rc = new RuleChain(r);
            } else {
                rc = rc.addRule(r);
            }
        }
        return rc;
    }

    /*
     * An implementation of Constraint representing a modulus,
     * a range of values, and include/exclude. Provides lots of
     * convenience factory methods.
     */
    private static class RangeConstraint implements Constraint, Serializable {
        private static final long serialVersionUID = 1;

        private int mod;
        private boolean inRange;
        private boolean integersOnly;
        private long lowerBound;
        private long upperBound;
        private long[] range_list;

        RangeConstraint(int mod, boolean inRange, boolean integersOnly,
                        long lowerBound, long upperBound, long[] range_list) {
            this.mod = mod;
            this.inRange = inRange;
            this.integersOnly = integersOnly;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.range_list = range_list;
        }

        public boolean isFulfilled(double n) {
            if (integersOnly && (n - (long)n) != 0.0) {
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
            class ListBuilder {
                StringBuilder sb = new StringBuilder("[");
                ListBuilder add(String s) {
                    return add(s, null);
                }
                ListBuilder add(String s, Object o) {
                    if (sb.length() > 1) {
                        sb.append(", ");
                    }
                    sb.append(s);
                    if (o != null) {
                        sb.append(": ").append(o.toString());
                    }
                    return this;
                }
                public String toString() {
                    String s = sb.append(']').toString();
                    sb = null;
                    return s;
                }
            }
            ListBuilder lb = new ListBuilder();
            if (mod > 1) {
                lb.add("mod", mod);
            }
            if (inRange) {
                lb.add("in");
            } else {
                lb.add("except");
            }
            if (integersOnly) {
                lb.add("ints");
            }
            if (lowerBound == upperBound) {
                lb.add(String.valueOf(lowerBound));
            } else {
                lb.add(String.valueOf(lowerBound) + "-" + String.valueOf(upperBound));
            }
            if (range_list != null) {
                lb.add(Arrays.toString(range_list));
            }
            return lb.toString();
        }
    }

    /* Convenience base class for and/or constraints. */
    private static abstract class BinaryConstraint implements Constraint,
                                                   Serializable {
        private static final long serialVersionUID = 1;
        protected final Constraint a;
        protected final Constraint b;
        private final String conjunction;

        protected BinaryConstraint(Constraint a, Constraint b, String c) {
            this.a = a;
            this.b = b;
            this.conjunction = c;
        }

        public int updateRepeatLimit(int limit) {
            return a.updateRepeatLimit(b.updateRepeatLimit(limit));
        }

        public String toString() {
            return a.toString() + conjunction + b.toString();
        }
    }

    /* A constraint representing the logical and of two constraints. */
    private static class AndConstraint extends BinaryConstraint {
        private static final long serialVersionUID = 7766999779862263523L;

        AndConstraint(Constraint a, Constraint b) {
            super(a, b, " && ");
        }

        public boolean isFulfilled(double n) {
            return a.isFulfilled(n) && b.isFulfilled(n);
        }

        public boolean isLimited() {
            // we ignore the case where both a and b are unlimited but no values
            // satisfy both-- we still consider this 'unlimited'
            return a.isLimited() || b.isLimited();
        }
    }

    /* A constraint representing the logical or of two constraints. */
    private static class OrConstraint extends BinaryConstraint {
        private static final long serialVersionUID = 1405488568664762222L;

        OrConstraint(Constraint a, Constraint b) {
            super(a, b, " || ");
        }

        public boolean isFulfilled(double n) {
            return a.isFulfilled(n) || b.isFulfilled(n);
        }

        public boolean isLimited() {
            return a.isLimited() && b.isLimited();
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

        public boolean appliesTo(double n) {
            return constraint.isFulfilled(n);
        }

        public int updateRepeatLimit(int limit) {
            return constraint.updateRepeatLimit(limit);
        }

        public boolean isLimited() {
            return constraint.isLimited();
        }

        public String toString() {
            return keyword + ": " + constraint;
        }
    }

    /*
     * Implementation of RuleList that is itself a node in a linked list.
     * Immutable, but supports chaining with 'addRule'.
     */
    private static class RuleChain implements RuleList, Serializable {
        private static final long serialVersionUID = 1;
        private final Rule rule;
        private final RuleChain next;

        /** Creates a rule chain with the single rule. */
        public RuleChain(Rule rule) {
            this(rule, null);
        }

        private RuleChain(Rule rule, RuleChain next) {
            this.rule = rule;
            this.next = next;
        }

        public RuleChain addRule(Rule nextRule) {
            return new RuleChain(nextRule, this);
        }

        private Rule selectRule(double n) {
            Rule r = null;
            if (next != null) {
                r = next.selectRule(n);
            }
            if (r == null && rule.appliesTo(n)) {
                r = rule;
            }
            return r;
        }

        public String select(double n) {
            Rule r = selectRule(n);
            if (r == null) {
                return KEYWORD_OTHER;
            }
            return r.getKeyword();
        }

        public Set<String> getKeywords() {
            Set<String> result = new HashSet<String>();
            result.add(KEYWORD_OTHER);
            RuleChain rc = this;
            while (rc != null) {
                result.add(rc.rule.getKeyword());
                rc = rc.next;
            }
            return result;
        }

        public boolean isLimited(String keyword) {
            // if all rules with this keyword are limited, it's limited,
            // and if there's no rule with this keyword, it's unlimited
            RuleChain rc = this;
            boolean result = false;
            while (rc != null) {
                if (keyword.equals(rc.rule.getKeyword())) {
                    if (!rc.rule.isLimited()) {
                        return false;
                    }
                    result = true;
                }
                rc = rc.next;
            }
            return result;
        }

        public int getRepeatLimit() {
          int result = 0;
          RuleChain rc = this;
          while (rc != null) {
            result = rc.rule.updateRepeatLimit(result);
            rc = rc.next;
          }
          return result;
        }

        public String toString() {
            String s = rule.toString();
            if (next != null) {
                s = next.toString() + "; " + s;
            }
            return s;
        }
    }

    // -------------------------------------------------------------------------
    // Static class methods.
    // -------------------------------------------------------------------------

    /**
     * Provides access to the predefined <code>PluralRules</code> for a given
     * locale.
     * ICU defines plural rules for many locales based on CLDR <i>Language Plural Rules</i>.
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
      return PluralRulesLoader.loader.forLocale(locale);
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
        this.keywords = Collections.unmodifiableSet(rules.getKeywords());
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
        return rules.select(number);
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
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release.
     */
    public Collection<Double> getAllKeywordValues(String keyword) {
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
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release.
     */
    public Collection<Double> getSamples(String keyword) {
        if (!keywords.contains(keyword)) {
            return null;
        }
        return getKeySamplesMap().get(keyword);
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

            if (keywordsRemaining > 0) {
                for (String k : keywords) {
                    if (!sampleMap.containsKey(k)) {
                        sampleMap.put(k, Collections.<Double>emptyList());
                        if (--keywordsRemaining == 0) {
                            break;
                        }
                    }
                }
            }

            // Make lists immutable so we can return them directly
            for (Entry<String, List<Double>> entry : sampleMap.entrySet()) {
                sampleMap.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
            }
            _keySamplesMap = sampleMap;
        }
    }

    /**
     * Returns the set of locales for which PluralRules are known.
     * @return the set of locales for which PluralRules are known, as a list
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static ULocale[] getAvailableULocales() {
      return PluralRulesLoader.loader.getAvailableULocales();
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
        return PluralRulesLoader.loader.getFunctionalEquivalent(locale, isAvailable);
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    public String toString() {
      return "keywords: " + keywords +
          " limit: " + getRepeatLimit() +
          " rules: " + rules.toString();
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
     * Return tif rhs is equal to this.
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

      int limit = Math.max(getRepeatLimit(), rhs.getRepeatLimit());
      for (int i = 0; i < limit * 2; ++i) {
        if (!select(i).equals(rhs.select(i))) {
          return false;
        }
      }
      return true;
    }

    private int getRepeatLimit() {
      if (repeatLimit == 0) {
        repeatLimit = rules.getRepeatLimit() + 1;
      }
      return repeatLimit;
    }
 }
