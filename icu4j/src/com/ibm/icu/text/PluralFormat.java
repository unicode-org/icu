/*
 *******************************************************************************
 * Copyright (C) 2007-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.util.ULocale;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * <code>PluralFormat</code> supports the creation of internationalized
 * messages with plural inflection. It is based on <i>plural
 * selection</i>, i.e. the caller specifies messages for each 
 * plural case that can appear in the users language and the
 * <code>PluralFormat</code> selects the appropriate message based on
 * the number.
 * </p>
 * <h4>The Problem of Plural Forms in Internationalized Messages</h4>
 * <p>
 * Different languages have different ways to inflect
 * plurals. Creating internationalized messages that include plural
 * forms is only feasible when the framework is able to handle plural
 * forms of <i>all</i> languages correctly. <code>ChoiceFormat</code>
 * doesn't handle this well, because it attaches a number interval to
 * each message and selects the message whose interval contains a
 * given number. This can only handle a finite number of
 * intervals. But in some languages, like Polish, one plural case
 * applies to infinitely many intervals (e.g., paucal applies to
 * numbers ending with 2, 3, or 4 except those ending with 12, 13, or
 * 14). Thus <code>ChoiceFormat</code> is not adequate.
 * </p><p>
 * <code>PluralFormat</code> deals with this by breaking the problem
 * into two parts:
 * <ul>
 * <li>It uses <code>PluralRules</code> that can define more complex
 *     conditions for a plural case than just a single interval. These plural
 *     rules define both what plural cases exist in a language, and to
 *     which numbers these cases apply.
 * <li>It provides predefined plural rules for many locales. Thus, the programmer
 *     need not worry about the plural cases of a language. On the flip side,
 *     the localizer does not have to specify the plural cases; he can simply
 *     use the predefined keywords. The whole plural formatting of messages can
 *     be done using localized patterns from resource bundles.
 * </ul>
 * </p>
 * <h4>Usage of <code>PluralFormat</code></h4>
 * <p>
 * This discussion assumes that you use <code>PluralFormat</code> with
 * a predefined set of plural rules. You can create one using one of
 * the constructors that takes a <code>ULocale</code> object. To
 * specify the message pattern, you can either pass it to the
 * constructor or set it explicitly using the
 * <code>applyPattern()</code> method. The <code>format()</code>
 * method takes a number object and selects the message of the
 * matching plural case. This message will be returned.
 * </p>
 * <h5>Patterns and Their Interpretation</h5>
 * <p>
 * The pattern text defines the message output for each plural case of the
 * used locale. The pattern is a sequence of 
 * <code><i>caseKeyword</i>{<i>message</i>}</code> clauses, separated by white
 * space characters. Each clause assigns the message <code><i>message</i></code>
 * to the plural case identified by <code><i>caseKeyword</i></code>.
 * </p><p>
 * You always have to define a message text for the default plural case
 * "<code>other</code>" which is contained in every rule set. If the plural
 * rules of the <code>PluralFormat</code> object do not contain a plural case
 * identified by <code><i>caseKeyword</i></code>, an
 * <code>IllegalArgumentException</code> is thrown.
 * If you do not specify a message text for a particular plural case, the
 * message text of the plural case "<code>other</code>" gets assigned to this
 * plural case. If you specify more than one message for the same plural case,
 * an <code>IllegalArgumentException</code> is thrown.
 * <br/>
 * Spaces between <code><i>caseKeyword</i></code> and
 * <code><i>message</i></code>  will be ignored; spaces within 
 * <code><i>message</i></code> will be preserved.
 * </p><p> 
 * The message text for a particular plural case may contain other message
 * format patterns. <code>PluralFormat</code> preserves these so that you
 * can use the strings produced by <code>PluralFormat</code> with other
 * formatters. If you are using <code>PluralFormat</code> inside a
 * <code>MessageFormat</code> pattern, <code>MessageFormat</code> will
 * automatically evaluate the resulting format pattern.<br/>
 * Thus, curly braces (<code>{</code>, <code>}</code>) are <i>only</i> allowed
 * in message texts to define a nested format pattern.<br/>
 * The pound sign (<code>#</code>) will be interpreted as the number placeholder
 * in the message text, if it is not contained in curly braces (to preserve
 * <code>NumberFormat</code> patterns). <code>PluralFormat</code> will
 * replace each of those pound signs by the number passed to the
 * <code>format()</code> method. It will be formatted using a
 * <code>NumberFormat</code> for the <code>PluralFormat</code>'s locale. If you
 * need special number formatting, you have to explicitly specify a
 * <code>NumberFormat</code> for the <code>PluralFormat</code> to use.
 * </p>
 * Example
 * <pre>
 * MessageFormat msgFmt = new MessageFormat("{0, plural, " +
 *     "one{{0, number, C''''est #,##0.0#  fichier}} " +
 *     "other {Ce sont # fichiers}} dans la liste.",
 *     new ULocale("fr"));
 * Object args[] = {new Long(0)};
 * System.out.println(msgFmt.format(args));
 * args = {new Long(3)};
 * System.out.println(msgFmt.format(args));
 * </pre>
 * Produces the output:<br />
 * <code>C'est 0,0 fichier dans la liste.</code><br />
 * <code>Ce sont 3 fichiers dans la liste."</code>
 * <p>
 * <strong>Note:</strong><br />
 *   Currently <code>PluralFormat</code>
 *   does not make use of quotes like <code>MessageFormat</code>.
 *   If you use plural format strings with <code>MessageFormat</code> and want
 *   to use a quote sign "<code>'</code>", you have to write "<code>''</code>".
 *   <code>MessageFormat</code> unquotes this pattern and  passes the unquoted
 *   pattern to <code>PluralFormat</code>. It's a bit trickier if you use
 *   nested formats that do quoting. In the example above, we wanted to insert
 *   "<code>'</code>" in the number format pattern. Since
 *   <code>NumberFormat</code> supports quotes, we had to insert
 *   "<code>''</code>". But since <code>MessageFormat</code> unquotes the
 *   pattern before it gets passed to <code>PluralFormat</code>, we have to
 *   double these quotes, i.e. write "<code>''''</code>".
 * </p>
 * <h4>Defining Custom Plural Rules</h4>
 * <p>If you need to use <code>PluralFormat</code> with custom rules, you can
 * create a <code>PluralRules</code> object and pass it to
 * <code>PluralFormat</code>'s constructor. If you also specify a locale in this
 * constructor, this locale will be used to format the number in the message
 * texts.
 * </p><p>
 * For more information about <code>PluralRules</code>, see
 * {@link PluralRules}.
 * </p>
 * 
 * @author tschumann (Tim Schumann)
 * @draft ICU 3.8
 * @provisional This API might change or be removed in a future release.
 */
public class PluralFormat extends UFormat {
    private static final long serialVersionUID = 1L;

    /**
     * The locale used for standard number formatting and getting the predefined
     * plural rules (if they were not defined explicitely).
     */
    private ULocale ulocale = null;

    /**
     * The plural rules used for plural selection.
     */
    private PluralRules pluralRules = null;

    /**
     * The applied pattern string.
     */
    private String pattern = null;

    /**
     * The format messages for each plural case. It is a mapping:
     *  <code>String</code>(plural case keyword) --&gt; <code>String</code> 
     *  (message for this plural case).  
     */
    private Map parsedValues = null;

    /**
     * This <code>NumberFormat</code> is used for the standard formatting of
     * the number inserted into the message.
     */
    private NumberFormat numberFormat = null;

    /**
     * Creates a new <code>PluralFormat</code> for the default locale.
     * This locale will be used to get the set of plural rules and for standard
     * number formatting.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public PluralFormat() {
        init(null, ULocale.getDefault());
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given locale.
     * @param ulocale the <code>PluralFormat</code> will be configured with
     *        rules for this locale. This locale will also be used for standard
     *        number formatting.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public PluralFormat(ULocale ulocale) {
        init(null, ulocale);
    }
    
    /**
     * Creates a new <code>PluralFormat</code> for a given set of rules.
     * The standard number formatting will be done using the default locale. 
     * @param rules defines the behavior of the <code>PluralFormat</code>
     *        object.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public PluralFormat(PluralRules rules) {
        init(rules, ULocale.getDefault());
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given set of rules.
     * The standard number formatting will be done using the given locale.
     * @param ulocale the default number formatting will be done using this
     *        locale.
     * @param rules defines the behavior of the <code>PluralFormat</code>
     *        object.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public PluralFormat(ULocale ulocale, PluralRules rules) {
        init(rules, ulocale);
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given pattern string.
     * The default locale will be used to get the set of plural rules and for
     * standard number formatting.
     * @param  pattern the pattern for this <code>PluralFormat</code>.
     * @throws IllegalArgumentException if the pattern is invalid.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public PluralFormat(String pattern) {
        init(null, ULocale.getDefault());
        applyPattern(pattern);
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given pattern string and 
     * locale.
     * The locale will be used to get the set of plural rules and for
     * standard number formatting.
     * @param ulocale the <code>PluralFormat</code> will be configured with
     *        rules for this locale. This locale will also be used for standard
     *        number formatting.
     * @param  pattern the pattern for this <code>PluralFormat</code>.
     * @throws IllegalArgumentException if the pattern is invalid.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public PluralFormat(ULocale ulocale, String pattern) {
        init(null, ulocale);
        applyPattern(pattern);
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given set of rules and a 
     * pattern.
     * The standard number formatting will be done using the default locale. 
     * @param rules defines the behavior of the <code>PluralFormat</code>
     *        object.
     * @param  pattern the pattern for this <code>PluralFormat</code>.
     * @throws IllegalArgumentException if the pattern is invalid.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */    
    public PluralFormat(PluralRules rules, String pattern) {
        init(rules, ULocale.getDefault());
        applyPattern(pattern);
    }
    
    /**
     * Creates a new <code>PluralFormat</code> for a given set of rules, a 
     * pattern and a locale.
     * @param ulocale the <code>PluralFormat</code> will be configured with
     *        rules for this locale. This locale will also be used for standard
     *        number formatting.
     * @param rules defines the behavior of the <code>PluralFormat</code>
     *        object.
     * @param  pattern the pattern for this <code>PluralFormat</code>.
     * @throws IllegalArgumentException if the pattern is invalid.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public PluralFormat(ULocale ulocale, PluralRules rules, String pattern) {
        init(rules, ulocale);
        applyPattern(pattern);
    }

    /**
     * Initializes the <code>PluralRules</code> object.
     * Postcondition:<br/>
     *   <code>ulocale</code>    :  is <code>locale</code><br/>
     *   <code>pluralRules</code>:  if <code>rules</code> != <code>null</code> 
     *                              it's set to rules, otherwise it is the   
     *                              predefined plural rule set for the locale
     *                              <code>ulocale</code>.<br/> 
     *   <code>parsedValues</code>: is <code>null</code><br/>
     *   <code>pattern</code>:      is <code>null</code><br/>
     *   <code>numberFormat</code>: a <code>NumberFormat</code> for the locale
     *                              <code>ulocale</code>.
     */
    private void init(PluralRules rules, ULocale locale) {
        ulocale = locale;
        pluralRules = (rules == null) ? PluralRules.forLocale(ulocale)
                                      : rules;
        parsedValues = null;
        pattern = null;
        numberFormat = NumberFormat.getInstance(ulocale);
    }

    /**
     * Sets the pattern used by this plural format.
     * The method parses the pattern and creates a map of format strings
     * for the plural rules.
     * Patterns and their interpretation are specified in the class description.
     * 
     * @param pttrn the pattern for this plural format.
     * @throws IllegalArgumentException if the pattern is invalid.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public void applyPattern(String pttrn) {
        this.pattern = pttrn;
        int braceStack = 0;
        Set ruleNames = pluralRules.getKeywords();
        parsedValues = new HashMap();

        // Format string has to include keywords.
        // states:
        // 0: Reading keyword.
        // 1: Reading value for preceding keyword.
        int state = 0;
        StringBuffer token = new StringBuffer();
        String currentKeyword = null;
        boolean readSpaceAfterKeyword = false;
        for (int i = 0; i < pttrn.length(); ++i) {
            char ch = pttrn.charAt(i);
            switch (state) {
            case 0: // Reading value.
                if (token.length() == 0) {
                    readSpaceAfterKeyword = false;
                }
                if (UCharacterProperty.isRuleWhiteSpace(ch)) {
                    if (token.length() > 0) {
                        readSpaceAfterKeyword = true;
                    }
                    // Skip leading and trailing whitespaces.
                    break;
                }
                if (ch == '{') { // End of keyword definition reached.
                    currentKeyword = token.toString().toLowerCase(
                            Locale.ENGLISH);
                    if (!ruleNames.contains(currentKeyword)) {
                        parsingFailure("Malformed formatting expression. "
                                + "Unknown keyword \"" + currentKeyword
                                + "\" at position " + i + ".");
                    }
                    if (parsedValues.get(currentKeyword) != null) {
                        parsingFailure("Malformed formatting expression. "
                                + "Text for case \"" + currentKeyword
                                + "\" at position " + i + " already defined!");
                    }
                    token.delete(0, token.length());
                    braceStack++;
                    state = 1;
                    break;
                }
                if (readSpaceAfterKeyword) {
                    parsingFailure("Malformed formatting expression. " +
                            "Invalid keyword definition. Character \"" + ch +
                            "\" at position " + i + " not expected!");
                }
                token.append(ch);
                break;
            case 1: // Reading value.
                switch (ch) {
                case '{':
                    braceStack++;
                    token.append(ch);
                    break;
                case '}':
                    braceStack--;
                    if (braceStack == 0) { // End of value reached.
                        parsedValues.put(currentKeyword, token.toString());
                        token.delete(0, token.length());
                        state = 0;
                    } else if (braceStack < 0) {
                        parsingFailure("Malformed formatting expression. "
                                + "Braces do not match.");
                    } else { // braceStack > 0
                        token.append(ch);
                    }
                    break;
                default:
                    token.append(ch);
                }
                break;
            } // switch state
        } // for loop.
        if (braceStack != 0) {
            parsingFailure(
                    "Malformed formatting expression. Braces do not match.");
        }
        checkSufficientDefinition();
    }

    /**
     * Formats a plural message for a given number.
     * 
     * @param number a number for which the plural message should be formatted.
     *        If no pattern has been applied to this
     *        <code>PluralFormat</code> object yet, the formatted number will
     *        be returned.
     * @return the string containing the formatted plural message.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public final String format(double number) {
        // If no pattern was applied, return the formatted number.
        if (parsedValues == null) {
            return numberFormat.format(number);
        }

        // Get appropriate format pattern.
        String selectedRule = pluralRules.select(number);
        String selectedPattern = (String) parsedValues.get(selectedRule);
        if (selectedPattern == null) { // Fallback to others.
            selectedPattern = 
                (String) parsedValues.get(PluralRules.KEYWORD_OTHER);
        }
        // Get formatted number and insert it into String.
        // Will replace all '#' which are not inside curly braces by the
        // formatted number.
        return insertFormattedNumber(number, selectedPattern);
        
    }

    /**
     * Formats a plural message for a given number and appends the formatted
     * message to the given <code>StringBuffer</code>.
     * @param number a number object (instance of <code>Number</code> for which
     *        the plural message should be formatted. If no pattern has been
     *        applied to this <code>PluralFormat</code> object yet, the
     *        formatted number will be returned.
     *        Note: If this object is not an instance of <code>Number</code>,
     *              the <code>toAppendTo</code> will not be modified. 
     * @param toAppendTo the formatted message will be appended to this
     *        <code>StringBuffer</code>.
     * @param pos will be ignored by this method. 
     * @return the string buffer passed in as toAppendTo, with formatted text
     *         appended.
     * @throws IllegalArgumentException if number is not an instance of Number
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public StringBuffer format(Object number, StringBuffer toAppendTo,
            FieldPosition pos) {
        if (number instanceof Number) {
            toAppendTo.append(format(((Number) number).doubleValue()));
            return toAppendTo;
        }
        throw new IllegalArgumentException("'" + number + 
                                           "' is not a Number");
    }

    /**
     * This method is not yet supported by <code>PluralFormat</code>.
     * @param text the string to be parsed.
     * @param parsePosition defines the position where parsing is to begin,
     * and upon return, the position where parsing left off.  If the position
     * has not changed upon return, then parsing failed.
     * @return nothing because this method is not yet implemented.
     * @throws UnsupportedOperationException
     *     will always be thrown by this method.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public Number parse(String text, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * This method is not yet supported by <code>PluralFormat</code>.
     * @param source the string to be parsed.
     * @param pos defines the position where parsing is to begin,
     * and upon return, the position where parsing left off.  If the position
     * has not changed upon return, then parsing failed.
     * @return nothing because this method is not yet implemented.
     * @throws UnsupportedOperationException
     *     will always be thrown by this method.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the locale used by this <code>PluraFormat</code> object.
     * Note: Calling this method resets this <code>PluraFormat</code> object,
     *     i.e., a pattern that was applied previously will be removed,
     *     and the NumberFormat is set to the default number format for
     *     the locale.  The resulting format behaves the same as one
     *     constructed from {@link #PluralFormat(ULocale)}.
     * @param ulocale the <code>ULocale</code> used to configure the
     *     formatter. If <code>ulocale</code> is <code>null</code>, the
     *     default locale will be used.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public void setLocale(ULocale ulocale) {
        if (ulocale == null) {
            ulocale = ULocale.getDefault();
        }
        init(null, ulocale);
    }

    /**
     * Sets the number format used by this formatter.  You only need to
     * call this if you want a different number format than the default
     * formatter for the locale.
     * @param format the number format to use.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public void setNumberFormat(NumberFormat format) {
        numberFormat = format;
    }

    /**
     * Checks if the applied pattern provided enough information,
     * i.e., if the attribute <code>parsedValues</code> stores enough
     * information for plural formatting.
     * Will be called at the end of pattern parsing.
     * @throws IllegalArgumentException if there's not sufficient information
     *     provided.
     */
    private void checkSufficientDefinition() {
        // Check that at least the default rule is defined.
        if (parsedValues.get(PluralRules.KEYWORD_OTHER) == null) {
            parsingFailure("Malformed formatting expression.\n"
                    + "Value for case \"" + PluralRules.KEYWORD_OTHER
                    + "\" was not defined.");
        }
    }

    /**
     * Helper method that resets the <code>PluralFormat</code> object and throws
     * an <code>IllegalArgumentException</code> with a given error text.
     * @param errorText the error text of the exception message.
     * @throws IllegalArgumentException will always be thrown by this method.
     */
    private void parsingFailure(String errorText) {
        // Set PluralFormat to a valid state.
        init(null, ULocale.getDefault());
        throw new IllegalArgumentException(errorText);
    }
    
    /**
     * Helper method that is called during formatting.
     * It replaces the character '#' by the number used for plural selection in
     * a message text. Only '#' are replaced, that are not written inside curly
     * braces. This allows the use of nested number formats.
     * The number will be formatted using the attribute
     * <code>numberformat</code>.
     * @param number the number used for plural selection.
     * @param message is the text in which '#' will be replaced.
     * @return the text with inserted numbers.
     */
    private String insertFormattedNumber(double number, String message) {
        if (message == null) {
            return "";
        }
        String formattedNumber = numberFormat.format(number);
        StringBuffer result = new StringBuffer();
        int braceStack = 0;
        int startIndex = 0;
        for (int i = 0; i < message.length(); ++i) {
            switch (message.charAt(i)) {
            case '{': 
                ++braceStack;
                break;
            case '}': 
                --braceStack;
                break;
            case '#':
                if (braceStack == 0) {
                    result.append(message.substring(startIndex,i));
                    startIndex = i + 1;
                    result.append(formattedNumber);
                }
                break;
            }
        }
        if (startIndex < message.length()) {
            result.append(message.substring(startIndex, message.length()));
        }
        return result.toString();
    }

    /**
     * {@inheritDoc}
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public boolean equals(Object rhs) {
        return rhs instanceof PluralFormat && equals((PluralFormat) rhs);
    }

    /**
     * Returns true if this equals the provided PluralFormat.
     * @param rhs the PluralFormat to compare against
     * @return true if this equals rhs
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public boolean equals(PluralFormat rhs) {
      return pluralRules.equals(rhs.pluralRules) &&
          parsedValues.equals(rhs.parsedValues) &&
          numberFormat.equals(rhs.numberFormat);
    }
        
    /**
     * {@inheritDoc}
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public int hashCode() {
        return pluralRules.hashCode() ^ parsedValues.hashCode();
    }

    /**
     * For debugging purposes only
     * @return a text representation of the format data.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("locale=" + ulocale);
        buf.append(", rules='" + pluralRules + "'");
        buf.append(", pattern='" + pattern + "'");
        buf.append(", parsedValues='" + parsedValues + "'");
        buf.append(", format='" + numberFormat + "'");
        return buf.toString();
    }
}
