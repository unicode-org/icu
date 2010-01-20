/*
**********************************************************************
* Copyright (c) 2004-2010, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 6, 2004
* Since: ICU 3.0
**********************************************************************
*/
package com.ibm.icu.text;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;

/**
 * MessageFormat produces concatenated messages in a language-neutral
 * way. Use this whenever concatenating strings that are displayed to
 * end users.
 *
 * <p>A MessageFormat contains an array of <em>subformats</em> arranged
 * within a <em>template string</em>.  Together, the subformats and
 * template string determine how the MessageFormat will operate during
 * formatting and parsing.
 * 
 * <p>Typically, both the subformats and the template string are
 * specified at once in a <em>pattern</em>.  By using different
 * patterns for different locales, messages may be localized.
 * 
 * <p>When formatting, MessageFormat takes a collection of arguments
 * and produces a user-readable string.  The arguments may be passed
 * as an array or as a Map.  Each argument is matched up with its
 * corresponding subformat, which then formats it into a string.  The
 * resulting strings are then assembled within the string template of
 * the MessageFormat to produce the final output string.
 *
 * <p><strong>Note:</strong>
 * <code>MessageFormat</code> differs from the other <code>Format</code>
 * classes in that you create a <code>MessageFormat</code> object with one
 * of its constructors (not with a <code>getInstance</code> style factory
 * method). The factory methods aren't necessary because <code>MessageFormat</code>
 * itself doesn't implement locale-specific behavior. Any locale-specific
 * behavior is defined by the pattern that you provide and the
 * subformats used for inserted arguments.
 *
 * <p><strong>Note:</strong>
 * In ICU 3.8 MessageFormat supports named arguments.  If a named argument
 * is used, all arguments must be named.  Names start with a character in
 * <code>:ID_START:</code> and continue with characters in <code>:ID_CONTINUE:</code>,
 * in particular they do not start with a digit.  If named arguments
 * are used, {@link #usesNamedArguments()} will return true.
 * 
 * <p>The other new methods supporting named arguments are
 * {@link #setFormatsByArgumentName(Map)},
 * {@link #setFormatByArgumentName(String, Format)},
 * {@link #format(Map, StringBuffer, FieldPosition)},
 * {@link #format(String, Map)}, {@link #parseToMap(String, ParsePosition)},
 * and {@link #parseToMap(String)}.  These methods are all compatible
 * with patterns that do not used named arguments-- in these cases
 * the keys in the input or output <code>Map</code>s use
 * <code>String</code>s that name the argument indices, e.g. "0",
 * "1", "2"... etc.
 * 
 * <p>When named arguments are used, certain methods on MessageFormat that take or
 * return arrays will throw an exception, since it is not possible to
 * identify positions in an array using a name.  These methods are 
 * {@link #setFormatsByArgumentIndex(Format[])}, 
 * {@link #setFormatByArgumentIndex(int, Format)},
 * {@link #getFormatsByArgumentIndex()},
 * {@link #getFormats()},
 * {@link #format(Object[], StringBuffer, FieldPosition)},
 * {@link #format(String, Object[])},
 * {@link #parse(String, ParsePosition)}, and
 * {@link #parse(String)}.
 * These APIs all have corresponding new versions as listed above.
 * 
 * <p>The API {@link #format(Object, StringBuffer, FieldPosition)} has
 * been modified so that the <code>Object</code> argument can be
 * either an <code>Object</code> array or a <code>Map</code>.  If this
 * format uses named arguments, this argument must not be an
 * <code>Object</code> array otherwise an exception will be thrown.
 * If the argument is a <code>Map</code> it can be used with Strings that
 * represent indices as described above.
 *
 * <h4><a name="patterns">Patterns and Their Interpretation</a></h4>
 *
 * <code>MessageFormat</code> uses patterns of the following form:
 * <blockquote><pre>
 * <i>MessageFormatPattern:</i>
 *         <i>String</i>
 *         <i>MessageFormatPattern</i> <i>FormatElement</i> <i>String</i>
 *
 * <i>FormatElement:</i>
 *         { <i>ArgumentIndexOrName</i> }
 *         { <i>ArgumentIndexOrName</i> , <i>FormatType</i> }
 *         { <i>ArgumentIndexOrName</i> , <i>FormatType</i> , <i>FormatStyle</i> }
 *
 * <i>ArgumentIndexOrName: one of </i>
 *         ['0'-'9']+
 *         [:ID_START:][:ID_CONTINUE:]*
 *
 * <i>FormatType: one of </i>
 *         number date time choice spellout ordinal duration plural
 *
 * <i>FormatStyle:</i>
 *         short
 *         medium
 *         long
 *         full
 *         integer
 *         currency
 *         percent
 *         <i>SubformatPattern</i>
 *         <i>RulesetName</i>
 *
 * <i>String:</i>
 *         <i>StringPart<sub>opt</sub></i>
 *         <i>String</i> <i>StringPart</i>
 *
 * <i>StringPart:</i>
 *         ''
 *         ' <i>QuotedString</i> '
 *         <i>UnquotedString</i>
 *
 * <i>SubformatPattern:</i>
 *         <i>SubformatPatternPart<sub>opt</sub></i>
 *         <i>SubformatPattern</i> <i>SubformatPatternPart</i>
 *
 * <i>SubFormatPatternPart:</i>
 *         ' <i>QuotedPattern</i> '
 *         <i>UnquotedPattern</i>
 * </pre></blockquote>
 *
 * <i>RulesetName:</i>
 *         <i>UnquotedString</i>
 * <p>
 * Within a <i>String</i>, <code>"''"</code> represents a single
 * quote. A <i>QuotedString</i> can contain arbitrary characters
 * except single quotes; the surrounding single quotes are removed.
 * An <i>UnquotedString</i> can contain arbitrary characters
 * except single quotes and left curly brackets. Thus, a string that
 * should result in the formatted message "'{0}'" can be written as
 * <code>"'''{'0}''"</code> or <code>"'''{0}'''"</code>.
 * <p>
 * Within a <i>SubformatPattern</i>, different rules apply.
 * A <i>QuotedPattern</i> can contain arbitrary characters
 * except single quotes; but the surrounding single quotes are
 * <strong>not</strong> removed, so they may be interpreted by the
 * subformat. For example, <code>"{1,number,$'#',##}"</code> will
 * produce a number format with the pound-sign quoted, with a result
 * such as: "$#31,45".
 * An <i>UnquotedPattern</i> can contain arbitrary characters
 * except single quotes, but curly braces within it must be balanced.
 * For example, <code>"ab {0} de"</code> and <code>"ab '}' de"</code>
 * are valid subformat patterns, but <code>"ab {0'}' de"</code> and
 * <code>"ab } de"</code> are not.
 * <p>
 * <dl><dt><b>Warning:</b><dd>The rules for using quotes within message
 * format patterns unfortunately have shown to be somewhat confusing.
 * In particular, it isn't always obvious to localizers whether single
 * quotes need to be doubled or not. Make sure to inform localizers about
 * the rules, and tell them (for example, by using comments in resource
 * bundle source files) which strings will be processed by MessageFormat.
 * Note that localizers may need to use single quotes in translated
 * strings where the original version doesn't have them.
 * <br>Note also that the simplest way to avoid the problem is to
 * use the real apostrophe (single quote) character \u2019 (') for
 * human-readable text, and to use the ASCII apostrophe (\u0027 ' )
 * only in program syntax, like quoting in MessageFormat.
 * See the annotations for U+0027 Apostrophe in The Unicode Standard.</p>
 * </dl>
 * <p>
 * The <i>ArgumentIndex</i> value is a non-negative integer written
 * using the digits '0' through '9', and represents an index into the
 * <code>arguments</code> array passed to the <code>format</code> methods
 * or the result array returned by the <code>parse</code> methods.
 * <p>
 * The <i>FormatType</i> and <i>FormatStyle</i> values are used to create
 * a <code>Format</code> instance for the format element. The following
 * table shows how the values map to Format instances. Combinations not
 * shown in the table are illegal. A <i>SubformatPattern</i> must
 * be a valid pattern string for the Format subclass used.
 * <p>
 * <table border=1>
 *    <tr>
 *       <th>Format Type
 *       <th>Format Style
 *       <th>Subformat Created
 *    <tr>
 *       <td colspan=2><i>(none)</i>
 *       <td><code>null</code>
 *    <tr>
 *       <td rowspan=5><code>number</code>
 *       <td><i>(none)</i>
 *       <td><code>NumberFormat.getInstance(getLocale())</code>
 *    <tr>
 *       <td><code>integer</code>
 *       <td><code>NumberFormat.getIntegerInstance(getLocale())</code>
 *    <tr>
 *       <td><code>currency</code>
 *       <td><code>NumberFormat.getCurrencyInstance(getLocale())</code>
 *    <tr>
 *       <td><code>percent</code>
 *       <td><code>NumberFormat.getPercentInstance(getLocale())</code>
 *    <tr>
 *       <td><i>SubformatPattern</i>
 *       <td><code>new DecimalFormat(subformatPattern, new DecimalFormatSymbols(getLocale()))</code>
 *    <tr>
 *       <td rowspan=6><code>date</code>
 *       <td><i>(none)</i>
 *       <td><code>DateFormat.getDateInstance(DateFormat.DEFAULT, getLocale())</code>
 *    <tr>
 *       <td><code>short</code>
 *       <td><code>DateFormat.getDateInstance(DateFormat.SHORT, getLocale())</code>
 *    <tr>
 *       <td><code>medium</code>
 *       <td><code>DateFormat.getDateInstance(DateFormat.DEFAULT, getLocale())</code>
 *    <tr>
 *       <td><code>long</code>
 *       <td><code>DateFormat.getDateInstance(DateFormat.LONG, getLocale())</code>
 *    <tr>
 *       <td><code>full</code>
 *       <td><code>DateFormat.getDateInstance(DateFormat.FULL, getLocale())</code>
 *    <tr>
 *       <td><i>SubformatPattern</i>
 *       <td><code>new SimpleDateFormat(subformatPattern, getLocale())
 *    <tr>
 *       <td rowspan=6><code>time</code>
 *       <td><i>(none)</i>
 *       <td><code>DateFormat.getTimeInstance(DateFormat.DEFAULT, getLocale())</code>
 *    <tr>
 *       <td><code>short</code>
 *       <td><code>DateFormat.getTimeInstance(DateFormat.SHORT, getLocale())</code>
 *    <tr>
 *       <td><code>medium</code>
 *       <td><code>DateFormat.getTimeInstance(DateFormat.DEFAULT, getLocale())</code>
 *    <tr>
 *       <td><code>long</code>
 *       <td><code>DateFormat.getTimeInstance(DateFormat.LONG, getLocale())</code>
 *    <tr>
 *       <td><code>full</code>
 *       <td><code>DateFormat.getTimeInstance(DateFormat.FULL, getLocale())</code>
 *    <tr>
 *       <td><i>SubformatPattern</i>
 *       <td><code>new SimpleDateFormat(subformatPattern, getLocale())
 *    <tr>
 *       <td><code>choice</code>
 *       <td><i>SubformatPattern</i>
 *       <td><code>new ChoiceFormat(subformatPattern)</code>
 *    <tr>
 *       <td><code>spellout</code>
 *       <td><i>RulesetName (optional)</i>
 *       <td><code>new RuleBasedNumberFormat(getLocale(), RuleBasedNumberFormat.SPELLOUT)
 *           <br/>&nbsp;&nbsp;&nbsp;&nbsp;.setDefaultRuleset(ruleset);</code>
 *    <tr>
 *       <td><code>ordinal</code>
 *       <td><i>RulesetName (optional)</i>
 *       <td><code>new RuleBasedNumberFormat(getLocale(), RuleBasedNumberFormat.ORDINAL)
 *           <br/>&nbsp;&nbsp;&nbsp;&nbsp;.setDefaultRuleset(ruleset);</code>
 *    <tr>
 *       <td><code>duration</code>
 *       <td><i>RulesetName (optional)</i>
 *       <td><code>new RuleBasedNumberFormat(getLocale(), RuleBasedNumberFormat.DURATION)
 *           <br/>&nbsp;&nbsp;&nbsp;&nbsp;.setDefaultRuleset(ruleset);</code>
 *    <tr>
 *       <td><code>plural</code>
 *       <td><i>SubformatPattern</i>
 *       <td><code>new PluralFormat(subformatPattern)</code>
 *    <tr>
 *       <td><code>select</code>
 *       <td><i>SubformatPattern</i>
 *       <td><code>new SelectFormat(subformatPattern)</code>
 * </table>
 * <p>
 *
 * <h4>Usage Information</h4>
 *
 * <p>
 * Here are some examples of usage:
 * <blockquote>
 * <pre>
 * Object[] arguments = {
 *     new Integer(7),
 *     new Date(System.currentTimeMillis()),
 *     "a disturbance in the Force"
 * };
 *
 * String result = MessageFormat.format(
 *     "At {1,time} on {1,date}, there was {2} on planet {0,number,integer}.",
 *     arguments);
 *
 * <em>output</em>: At 12:30 PM on Jul 3, 2053, there was a disturbance
 *           in the Force on planet 7.
 *
 * </pre>
 * </blockquote>
 * Typically, the message format will come from resources, and the
 * arguments will be dynamically set at runtime.
 *
 * <p>
 * Example 2:
 * <blockquote>
 * <pre>
 * Object[] testArgs = {new Long(3), "MyDisk"};
 *
 * MessageFormat form = new MessageFormat(
 *     "The disk \"{1}\" contains {0} file(s).");
 *
 * System.out.println(form.format(testArgs));
 *
 * // output, with different testArgs
 * <em>output</em>: The disk "MyDisk" contains 0 file(s).
 * <em>output</em>: The disk "MyDisk" contains 1 file(s).
 * <em>output</em>: The disk "MyDisk" contains 1,273 file(s).
 * </pre>
 * </blockquote>
 *
 * <p>
 * For more sophisticated patterns, you can use a <code>ChoiceFormat</code> to get
 * output such as:
 * <blockquote>
 * <pre>
 * MessageFormat form = new MessageFormat("The disk \"{1}\" contains {0}.");
 * double[] filelimits = {0,1,2};
 * String[] filepart = {"no files","one file","{0,number} files"};
 * ChoiceFormat fileform = new ChoiceFormat(filelimits, filepart);
 * form.setFormatByArgumentIndex(0, fileform);
 *
 * Object[] testArgs = {new Long(12373), "MyDisk"};
 *
 * System.out.println(form.format(testArgs));
 *
 * // output, with different testArgs
 * output: The disk "MyDisk" contains no files.
 * output: The disk "MyDisk" contains one file.
 * output: The disk "MyDisk" contains 1,273 files.
 * </pre>
 * </blockquote>
 * You can either do this programmatically, as in the above example,
 * or by using a pattern (see
 * {@link ChoiceFormat}
 * for more information) as in:
 * <blockquote>
 * <pre>
 * form.applyPattern(
 *    "There {0,choice,0#are no files|1#is one file|1&lt;are {0,number,integer} files}.");
 * </pre>
 * </blockquote>
 * <p>
 * <strong>Note:</strong> As we see above, the string produced
 * by a <code>ChoiceFormat</code> in <code>MessageFormat</code> is treated specially;
 * occurances of '{' are used to indicated subformats, and cause recursion.
 * If you create both a <code>MessageFormat</code> and <code>ChoiceFormat</code>
 * programmatically (instead of using the string patterns), then be careful not to
 * produce a format that recurses on itself, which will cause an infinite loop.
 * <p>
 * When a single argument is parsed more than once in the string, the last match
 * will be the final result of the parsing.  For example,
 * <pre>
 * MessageFormat mf = new MessageFormat("{0,number,#.##}, {0,number,#.#}");
 * Object[] objs = {new Double(3.1415)};
 * String result = mf.format( objs );
 * // result now equals "3.14, 3.1"
 * objs = null;
 * objs = mf.parse(result, new ParsePosition(0));
 * // objs now equals {new Double(3.1)}
 * </pre>
 * <p>
 * Likewise, parsing with a MessageFormat object using patterns containing
 * multiple occurances of the same argument would return the last match.  For
 * example,
 * <pre>
 * MessageFormat mf = new MessageFormat("{0}, {0}, {0}");
 * String forParsing = "x, y, z";
 * Object[] objs = mf.parse(forParsing, new ParsePosition(0));
 * // result now equals {new String("z")}
 * </pre>
 *
 * <h4><a name="synchronization">Synchronization</a></h4>
 *
 * <p>
 * Message formats are not synchronized.
 * It is recommended to create separate format instances for each thread.
 * If multiple threads access a format concurrently, it must be synchronized
 * externally.
 *
 * @see          java.util.Locale
 * @see          Format
 * @see          NumberFormat
 * @see          DecimalFormat
 * @see          ChoiceFormat
 * @see          PluralFormat
 * @see          SelectFormat
 * @author       Mark Davis
 * @stable ICU 3.0
 */
public class MessageFormat extends UFormat {

    // Generated by serialver from JDK 1.4.1_01
    static final long serialVersionUID = 7136212545847378651L;

    /**
     * Constructs a MessageFormat for the default locale and the
     * specified pattern.
     * The constructor first sets the locale, then parses the pattern and
     * creates a list of subformats for the format elements contained in it.
     * Patterns and their interpretation are specified in the
     * <a href="#patterns">class description</a>.
     *
     * @param pattern the pattern for this message format
     * @exception IllegalArgumentException if the pattern is invalid
     * @stable ICU 3.0
     */
    public MessageFormat(String pattern) {
        this.ulocale = ULocale.getDefault();
        applyPattern(pattern);
    }

    /**
     * Constructs a MessageFormat for the specified locale and
     * pattern.
     * The constructor first sets the locale, then parses the pattern and
     * creates a list of subformats for the format elements contained in it.
     * Patterns and their interpretation are specified in the
     * <a href="#patterns">class description</a>.
     *
     * @param pattern the pattern for this message format
     * @param locale the locale for this message format
     * @exception IllegalArgumentException if the pattern is invalid
     * @stable ICU 3.0
     */
    public MessageFormat(String pattern, Locale locale) {
        this(pattern, ULocale.forLocale(locale));
    }

    /**
     * Constructs a MessageFormat for the specified locale and
     * pattern.
     * The constructor first sets the locale, then parses the pattern and
     * creates a list of subformats for the format elements contained in it.
     * Patterns and their interpretation are specified in the
     * <a href="#patterns">class description</a>.
     *
     * @param pattern the pattern for this message format
     * @param locale the locale for this message format
     * @exception IllegalArgumentException if the pattern is invalid
     * @stable ICU 3.2
     */
    public MessageFormat(String pattern, ULocale locale) {
        this.ulocale = locale;
        applyPattern(pattern);
    }

    /**
     * Sets the locale to be used when creating or comparing subformats.
     * This affects subsequent calls to the {@link #applyPattern applyPattern}
     * and {@link #toPattern toPattern} methods as well as to the
     * <code>format</code> and
     * {@link #formatToCharacterIterator formatToCharacterIterator} methods.
     *
     * @param locale the locale to be used when creating or comparing subformats
     * @stable ICU 3.0
     */
    public void setLocale(Locale locale) {
        setLocale(ULocale.forLocale(locale));
    }

    /**
     * Sets the locale to be used when creating or comparing subformats.
     * This affects subsequent calls to the {@link #applyPattern applyPattern}
     * and {@link #toPattern toPattern} methods as well as to the
     * <code>format</code> and
     * {@link #formatToCharacterIterator formatToCharacterIterator} methods.
     *
     * @param locale the locale to be used when creating or comparing subformats
     * @stable ICU 3.2
     */
    public void setLocale(ULocale locale) {
        /* Save the pattern, and then reapply so that */
        /* we pick up any changes in locale specific */
        /* elements */
        String existingPattern = toPattern();                       /*ibm.3550*/
        this.ulocale = locale;
        applyPattern(existingPattern);                              /*ibm.3550*/
    }

    /**
     * Gets the locale that's used when creating or comparing subformats.
     *
     * @return the locale used when creating or comparing subformats
     * @stable ICU 3.0
     */
    public Locale getLocale() {
        return ulocale.toLocale();
    }

    /**
     * Gets the locale that's used when creating or comparing subformats.
     *
     * @return the locale used when creating or comparing subformats
     * @stable ICU 3.2
     */
    public ULocale getULocale() {
        return ulocale;
    }

    /**
     * Sets the pattern used by this message format.
     * The method parses the pattern and creates a list of subformats
     * for the format elements contained in it.
     * Patterns and their interpretation are specified in the
     * <a href="#patterns">class description</a>.
     * <p>
     * The pattern must contain only named or only numeric arguments,
     * mixing them is not allowed.
     *
     * @param pttrn the pattern for this message format
     * @throws IllegalArgumentException if the pattern is invalid
     * @stable ICU 3.0
     */
    @SuppressWarnings("fallthrough")
    public void applyPattern(String pttrn) {
        StringBuffer[] segments = new StringBuffer[4];
        for (int i = 0; i < segments.length; ++i) {
            segments[i] = new StringBuffer();
        }
        int part = 0;
        int formatNumber = 0;
        boolean inQuote = false;
        int braceStack = 0;
        maxOffset = -1;
        for (int i = 0; i < pttrn.length(); ++i) {
            char ch = pttrn.charAt(i);
            if (part == 0) {
                if (ch == '\'') {
                    if (i + 1 < pttrn.length()
                        && pttrn.charAt(i+1) == '\'') {
                        segments[part].append(ch);  // handle doubles
                        ++i;
                    } else {
                        inQuote = !inQuote;
                    }
                } else if (ch == '{' && !inQuote) {
                    part = 1;
                } else {
                    segments[part].append(ch);
                }
            } else  if (inQuote) {  // just copy quotes in parts
                segments[part].append(ch);
                if (ch == '\'') {
                    inQuote = false;
                }
            } else {
                switch (ch) {
                case ',':
                    if (part < 3)
                        part += 1;
                    else
                        segments[part].append(ch);
                    break;
                case '{':
                    ++braceStack;
                    segments[part].append(ch);
                    break;
                case '}':
                    if (braceStack == 0) {
                        part = 0;
                        makeFormat(i, formatNumber, segments);
                        formatNumber++;
                    } else {
                        --braceStack;
                        segments[part].append(ch);
                    }
                    break;
                case '\'':
                    inQuote = true;
                    // fall through, so we keep quotes in other parts
                default:
                    segments[part].append(ch);
                    break;
                }
            }
        }
        if (braceStack == 0 && part != 0) {
            maxOffset = -1;
            throw new IllegalArgumentException("Unmatched braces in the pattern.");
        }
        this.pattern = segments[0].toString();
    }

    /**
     * Returns a pattern representing the current state of the message format.
     * The string is constructed from internal information and therefore
     * does not necessarily equal the previously applied pattern.
     *
     * @return a pattern representing the current state of the message format
     * @stable ICU 3.0
     */
    public String toPattern() {
        // later, make this more extensible
        int lastOffset = 0;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i <= maxOffset; ++i) {
            copyAndFixQuotes(pattern, lastOffset, offsets[i],result);
            lastOffset = offsets[i];
            result.append('{');
            result.append(argumentNames[i]);
            if (formats[i] == null) {
                // do nothing, string format
            } else if (formats[i] instanceof DecimalFormat) {
                if (formats[i].equals(NumberFormat.getInstance(ulocale))) {
                    result.append(",number");
                } else if (formats[i].equals(NumberFormat.getCurrencyInstance(ulocale))) {
                    result.append(",number,currency");
                } else if (formats[i].equals(NumberFormat.getPercentInstance(ulocale))) {
                    result.append(",number,percent");
                } else if (formats[i].equals(NumberFormat.getIntegerInstance(ulocale))) {
                    result.append(",number,integer");
                } else {
                    result.append(",number," +
                                  ((DecimalFormat)formats[i]).toPattern());
                }
            } else if (formats[i] instanceof SimpleDateFormat) {
                if (formats[i].equals(DateFormat.getDateInstance(DateFormat.DEFAULT,ulocale))) {
                    result.append(",date");
                } else if (formats[i].equals(DateFormat.getDateInstance(DateFormat.SHORT,ulocale))) {
                    result.append(",date,short");
// This code will never be executed [alan]
//                } else if (formats[i].equals(DateFormat.getDateInstance(DateFormat.DEFAULT,ulocale))) {
//                    result.append(",date,medium");
                } else if (formats[i].equals(DateFormat.getDateInstance(DateFormat.LONG,ulocale))) {
                    result.append(",date,long");
                } else if (formats[i].equals(DateFormat.getDateInstance(DateFormat.FULL,ulocale))) {
                    result.append(",date,full");
                } else if (formats[i].equals(DateFormat.getTimeInstance(DateFormat.DEFAULT,ulocale))) {
                    result.append(",time");
                } else if (formats[i].equals(DateFormat.getTimeInstance(DateFormat.SHORT,ulocale))) {
                    result.append(",time,short");
// This code will never be executed [alan]
//                } else if (formats[i].equals(DateFormat.getTimeInstance(DateFormat.DEFAULT,ulocale))) {
//                    result.append(",time,medium");
                } else if (formats[i].equals(DateFormat.getTimeInstance(DateFormat.LONG,ulocale))) {
                    result.append(",time,long");
                } else if (formats[i].equals(DateFormat.getTimeInstance(DateFormat.FULL,ulocale))) {
                    result.append(",time,full");
                } else {
                    result.append(",date," + ((SimpleDateFormat)formats[i]).toPattern());
                }
            } else if (formats[i] instanceof ChoiceFormat) {
                result.append(",choice,"
                        + ((ChoiceFormat) formats[i]).toPattern());
            } else if (formats[i] instanceof PluralFormat ){
                  String pat = ((PluralFormat)formats[i]).toPattern();
                  // TODO: PluralFormat does not do the single quote thing, just reapply
                  pat = duplicateSingleQuotes(pat);
                  result.append(",plural," + pat);
            } else if (formats[i] instanceof SelectFormat) {
                  String pat = ((SelectFormat)formats[i]).toPattern();
                  //TODO: SelectFormat does not do the single quote thing, just reapply
                  pat = duplicateSingleQuotes(pat);
                  result.append(",select," + pat);
            } else {
                //result.append(", unknown");
            }
            result.append('}');
        }
        copyAndFixQuotes(pattern, lastOffset, pattern.length(), result);
        return result.toString();
    }

    /**
      * Double every single quote
      */
    private String duplicateSingleQuotes(String pat){
      String result = pat;
      if (pat.indexOf('\'') != 0) {
        StringBuffer buf = new StringBuffer();
        for (int j = 0; j < pat.length(); ++j) {
          char ch = pat.charAt(j);
          if (ch == '\'') {
            buf.append(ch); // double it
          }
          buf.append(ch);
        }
        result = buf.toString();
      }
      return result;
    }

    /**
     * Sets the formats to use for the values passed into
     * <code>format</code> methods or returned from <code>parse</code>
     * methods. The indices of elements in <code>newFormats</code>
     * correspond to the argument indices used in the previously set
     * pattern string.
     * The order of formats in <code>newFormats</code> thus corresponds to
     * the order of elements in the <code>arguments</code> array passed
     * to the <code>format</code> methods or the result array returned
     * by the <code>parse</code> methods.
     * <p>
     * If an argument index is used for more than one format element
     * in the pattern string, then the corresponding new format is used
     * for all such format elements. If an argument index is not used
     * for any format element in the pattern string, then the
     * corresponding new format is ignored. If fewer formats are provided
     * than needed, then only the formats for argument indices less
     * than <code>newFormats.length</code> are replaced.
     *
     * This method is only supported if the format does not use
     * named arguments, otherwise an IllegalArgumentException is thrown.
     *
     * @param newFormats the new formats to use
     * @throws NullPointerException if <code>newFormats</code> is null
     * @throws IllegalArgumentException if this formatter uses named arguments
     * @stable ICU 3.0
     */
    public void setFormatsByArgumentIndex(Format[] newFormats) {
        if (!argumentNamesAreNumeric) {
            throw new IllegalArgumentException(
                    "This method is not available in MessageFormat objects " +
                    "that use alphanumeric argument names.");
        }
        for (int i = 0; i <= maxOffset; i++) {
            int j = Integer.parseInt(argumentNames[i]);
            if (j < newFormats.length) {
                formats[i] = newFormats[j];
            }
        }
    }

    /**
     * Sets the formats to use for the values passed into
     * <code>format</code> methods or returned from <code>parse</code>
     * methods. The keys in <code>newFormats</code> are the argument
     * names in the previously set pattern string, and the values
     * are the formats.
     * <p>
     * Only argument names from the pattern string are considered.
     * Extra keys in <code>newFormats</code> that do not correspond
     * to an argument name are ignored.  Similarly, if there is no
     * format in newFormats for an argument name, the formatter
     * for that argument remains unchanged.
     * <p>
     * This may be called on formats that do not use named arguments.
     * In this case the map will be queried for key Strings that
     * represent argument indices, e.g. "0", "1", "2" etc.
     *
     * @param newFormats a map from String to Format providing new
     *        formats for named arguments.
     * @stable ICU 3.8
     */
    public void setFormatsByArgumentName(Map<String, Format> newFormats) {
        for (int i = 0; i <= maxOffset; i++) {
            if (newFormats.containsKey(argumentNames[i])) {
                Format f = newFormats.get(argumentNames[i]);
                formats[i] = f;
            }
        }
    }

    /**
     * Sets the formats to use for the format elements in the
     * previously set pattern string.
     * The order of formats in <code>newFormats</code> corresponds to
     * the order of format elements in the pattern string.
     * <p>
     * If more formats are provided than needed by the pattern string,
     * the remaining ones are ignored. If fewer formats are provided
     * than needed, then only the first <code>newFormats.length</code>
     * formats are replaced.
     * <p>
     * Since the order of format elements in a pattern string often
     * changes during localization, it is generally better to use the
     * {@link #setFormatsByArgumentIndex setFormatsByArgumentIndex}
     * method, which assumes an order of formats corresponding to the
     * order of elements in the <code>arguments</code> array passed to
     * the <code>format</code> methods or the result array returned by
     * the <code>parse</code> methods.
     *
     * @param newFormats the new formats to use
     * @exception NullPointerException if <code>newFormats</code> is null
     * @stable ICU 3.0
     */
    public void setFormats(Format[] newFormats) {
        int runsToCopy = newFormats.length;
        if (runsToCopy > maxOffset + 1) {
            runsToCopy = maxOffset + 1;
        }
        for (int i = 0; i < runsToCopy; i++) {
            formats[i] = newFormats[i];
        }
    }

    /**
     * Sets the format to use for the format elements within the
     * previously set pattern string that use the given argument
     * index.
     * The argument index is part of the format element definition and
     * represents an index into the <code>arguments</code> array passed
     * to the <code>format</code> methods or the result array returned
     * by the <code>parse</code> methods.
     * <p>
     * If the argument index is used for more than one format element
     * in the pattern string, then the new format is used for all such
     * format elements. If the argument index is not used for any format
     * element in the pattern string, then the new format is ignored.
     *
     * This method is only supported when exclusively numbers are used for
     * argument names. Otherwise an IllegalArgumentException is thrown.
     *
     * @param argumentIndex the argument index for which to use the new format
     * @param newFormat the new format to use
     * @throws IllegalArgumentException if this format uses named arguments
     * @stable ICU 3.0
     */
    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        if (!argumentNamesAreNumeric) {
            throw new IllegalArgumentException(
                    "This method is not available in MessageFormat objects " +
                    "that use alphanumeric argument names.");
        }
        for (int j = 0; j <= maxOffset; j++) {
            if (Integer.parseInt(argumentNames[j]) == argumentIndex) {
                formats[j] = newFormat;
            }
        }
    }

    /**
     * Sets the format to use for the format elements within the
     * previously set pattern string that use the given argument
     * name.
     * <p>
     * If the argument name is used for more than one format element
     * in the pattern string, then the new format is used for all such
     * format elements. If the argument name is not used for any format
     * element in the pattern string, then the new format is ignored.
     * <p>
     * This API may be used on formats that do not use named arguments.
     * In this case <code>argumentName</code> should be a String that names
     * an argument index, e.g. "0", "1", "2"... etc.  If it does not name
     * a valid index, the format will be ignored.  No error is thrown.
     *
     * @param argumentName the name of the argument to change
     * @param newFormat the new format to use
     * @stable ICU 3.8
     */
    public void setFormatByArgumentName(String argumentName, Format newFormat) {
        for (int i = 0; i <= maxOffset; ++i) {
            if (argumentName.equals(argumentNames[i])) {
                formats[i] = newFormat;
            }
        }
    }

    /**
     * Sets the format to use for the format element with the given
     * format element index within the previously set pattern string.
     * The format element index is the zero-based number of the format
     * element counting from the start of the pattern string.
     * <p>
     * Since the order of format elements in a pattern string often
     * changes during localization, it is generally better to use the
     * {@link #setFormatByArgumentIndex setFormatByArgumentIndex}
     * method, which accesses format elements based on the argument
     * index they specify.
     *
     * @param formatElementIndex the index of a format element within the pattern
     * @param newFormat the format to use for the specified format element
     * @exception ArrayIndexOutOfBoundsException if formatElementIndex is equal to or
     *            larger than the number of format elements in the pattern string
     * @stable ICU 3.0
     */
    public void setFormat(int formatElementIndex, Format newFormat) {
        formats[formatElementIndex] = newFormat;
    }

    /**
     * Gets the formats used for the values passed into
     * <code>format</code> methods or returned from <code>parse</code>
     * methods. The indices of elements in the returned array
     * correspond to the argument indices used in the previously set
     * pattern string.
     * The order of formats in the returned array thus corresponds to
     * the order of elements in the <code>arguments</code> array passed
     * to the <code>format</code> methods or the result array returned
     * by the <code>parse</code> methods.
     * <p>
     * If an argument index is used for more than one format element
     * in the pattern string, then the format used for the last such
     * format element is returned in the array. If an argument index
     * is not used for any format element in the pattern string, then
     * null is returned in the array.
     *
     * This method is only supported when exclusively numbers are used for
     * argument names. Otherwise an IllegalArgumentException is thrown.
     *
     * @return the formats used for the arguments within the pattern
     * @throws IllegalArgumentException if this format uses named arguments
     * @stable ICU 3.0
     */
    public Format[] getFormatsByArgumentIndex() {
        if (!argumentNamesAreNumeric) {
            throw new IllegalArgumentException(
                    "This method is not available in MessageFormat objects " +
                    "that use alphanumeric argument names.");
        }
        int maximumArgumentNumber = -1;
        for (int i = 0; i <= maxOffset; i++) {
            int argumentNumber = Integer.parseInt(argumentNames[i]);
            if (argumentNumber > maximumArgumentNumber) {
                maximumArgumentNumber = argumentNumber;
            }
        }
        Format[] resultArray = new Format[maximumArgumentNumber + 1];
        for (int i = 0; i <= maxOffset; i++) {
            resultArray[Integer.parseInt(argumentNames[i])] = formats[i];
        }
        return resultArray;
    }
    // TODO: provide method public Map getFormatsByArgumentName().
    // Where Map is: String argumentName --> Format format.

    /**
     * Gets the formats used for the format elements in the
     * previously set pattern string.
     * The order of formats in the returned array corresponds to
     * the order of format elements in the pattern string.
     * <p>
     * Since the order of format elements in a pattern string often
     * changes during localization, it's generally better to use the
     * {@link #getFormatsByArgumentIndex()}
     * method, which assumes an order of formats corresponding to the
     * order of elements in the <code>arguments</code> array passed to
     * the <code>format</code> methods or the result array returned by
     * the <code>parse</code> methods.
     *
     * This method is only supported when exclusively numbers are used for
     * argument names. Otherwise an IllegalArgumentException is thrown.
     *
     * @return the formats used for the format elements in the pattern
     * @throws IllegalArgumentException if this format uses named arguments
     * @stable ICU 3.0
     */
    public Format[] getFormats() {
        Format[] resultArray = new Format[maxOffset + 1];
        System.arraycopy(formats, 0, resultArray, 0, maxOffset + 1);
        return resultArray;
    }
    
    /**
     * Get the format argument names. For more details, see 
     * {@link #setFormatByArgumentName(String, Format)}.
     * @return List of names
     * @deprecated This API is ICU internal only.
     * @internal
     */
    public Set<String> getFormatArgumentNames() {
        Set<String> result = new HashSet<String>();
        for (int i = 0; i <= maxOffset; ++i) {
            result.add(argumentNames[i]);
        }
        return result;
    }
    
    /**
     * Get the formats according to their argument names. For more details, see 
     * {@link #setFormatByArgumentName(String, Format)}.
     * @return format associated with the name, or null if there isn't one.
     * @deprecated This API is ICU internal only.
     * @internal
     */
    public Format getFormatByArgumentName(String argumentName) {
        for (int i = 0; i <= maxOffset; ++i) {
            if (argumentName.equals(argumentNames[i])) {
                return formats[i];
            }
        }
        return null;
    }

    /**
     * Formats an array of objects and appends the <code>MessageFormat</code>'s
     * pattern, with format elements replaced by the formatted objects, to the
     * provided <code>StringBuffer</code>.
     * <p>
     * The text substituted for the individual format elements is derived from
     * the current subformat of the format element and the
     * <code>arguments</code> element at the format element's argument index
     * as indicated by the first matching line of the following table. An
     * argument is <i>unavailable</i> if <code>arguments</code> is
     * <code>null</code> or has fewer than argumentIndex+1 elements.  When
     * an argument is unavailable no substitution is performed.
     * <p>
     * <table border=1>
     *    <tr>
     *       <th>Subformat
     *       <th>Argument
     *       <th>Formatted Text
     *    <tr>
     *       <td><i>any</i>
     *       <td><i>unavailable</i>
     *       <td><code>"{" + argumentIndex + "}"</code>
     *    <tr>
     *       <td><i>any</i>
     *       <td><code>null</code>
     *       <td><code>"null"</code>
     *    <tr>
     *       <td><code>instanceof ChoiceFormat</code>
     *       <td><i>any</i>
     *       <td><code>subformat.format(argument).indexOf('{') >= 0 ?<br>
     *           (new MessageFormat(subformat.format(argument), getLocale())).format(argument) :
     *           subformat.format(argument)</code>
     *    <tr>
     *       <td><code>!= null</code>
     *       <td><i>any</i>
     *       <td><code>subformat.format(argument)</code>
     *    <tr>
     *       <td><code>null</code>
     *       <td><code>instanceof Number</code>
     *       <td><code>NumberFormat.getInstance(getLocale()).format(argument)</code>
     *    <tr>
     *       <td><code>null</code>
     *       <td><code>instanceof Date</code>
     *       <td><code>DateFormat.getDateTimeInstance(DateFormat.SHORT, 
     *           DateFormat.SHORT, getLocale()).format(argument)</code>
     *    <tr>
     *       <td><code>null</code>
     *       <td><code>instanceof String</code>
     *       <td><code>argument</code>
     *    <tr>
     *       <td><code>null</code>
     *       <td><i>any</i>
     *       <td><code>argument.toString()</code>
     * </table>
     * <p>
     * If <code>pos</code> is non-null, and refers to
     * <code>Field.ARGUMENT</code>, the location of the first formatted
     * string will be returned.
     *
     * This method is only supported when the format does not use named
     * arguments, otherwise an IllegalArgumentException is thrown.
     *
     * @param arguments an array of objects to be formatted and substituted.
     * @param result where text is appended.
     * @param pos On input: an alignment field, if desired.
     *            On output: the offsets of the alignment field.
     * @throws IllegalArgumentException if an argument in the
     *            <code>arguments</code> array is not of the type
     *            expected by the format element(s) that use it.
     * @throws IllegalArgumentException if this format uses named arguments
     * @stable ICU 3.0
     */
    public final StringBuffer format(Object[] arguments, StringBuffer result,
                                     FieldPosition pos)
    {
        if (!argumentNamesAreNumeric) {
            throw new IllegalArgumentException(
                  "This method is not available in MessageFormat objects " +
                  "that use alphanumeric argument names.");
        }
        return subformat(arguments, result, pos, null);
    }

    /**
     * Formats a map of objects and appends the <code>MessageFormat</code>'s
     * pattern, with format elements replaced by the formatted objects, to the
     * provided <code>StringBuffer</code>.
     * <p>
     * The text substituted for the individual format elements is derived from
     * the current subformat of the format element and the
     * <code>arguments</code> value corresopnding to the format element's
     * argument name.
     * <p>
     * This API may be called on formats that do not use named arguments.
     * In this case the the keys in <code>arguments</code> must be numeric
     * strings (e.g. "0", "1", "2"...).
     * <p>
     * An argument is <i>unavailable</i> if <code>arguments</code> is
     * <code>null</code> or does not have a value corresponding to an argument
     * name in the pattern.  When an argument is unavailable no substitution
     * is performed.
     *
     * @param arguments a map of objects to be formatted and substituted.
     * @param result where text is appended.
     * @param pos On input: an alignment field, if desired.
     *            On output: the offsets of the alignment field.
     * @throws IllegalArgumentException if an argument in the
     *         <code>arguments</code> array is not of the type
     *         expected by the format element(s) that use it.
     * @return the passed-in StringBuffer
     * @stable ICU 3.8
     */
    public final StringBuffer format(Map<String, Object> arguments, StringBuffer result,
                                     FieldPosition pos) {
        return subformat(arguments, result, pos, null);
    }

    /**
     * Creates a MessageFormat with the given pattern and uses it
     * to format the given arguments. This is equivalent to
     * <blockquote>
     *     <code>(new {@link #MessageFormat(String) MessageFormat}(pattern)).{@link 
     *     #format(java.lang.Object[], java.lang.StringBuffer, java.text.FieldPosition) 
     *     format}(arguments, new StringBuffer(), null).toString()</code>
     * </blockquote>
     *
     * @throws IllegalArgumentException if the pattern is invalid,
     *            or if an argument in the <code>arguments</code> array
     *            is not of the type expected by the format element(s)
     *            that use it.
     * @throws IllegalArgumentException if this format uses named arguments
     * @stable ICU 3.0
     */
    public static String format(String pattern, Object[] arguments) {
        MessageFormat temp = new MessageFormat(pattern);
        return temp.format(arguments);
    }

    /**
     * Creates a MessageFormat with the given pattern and uses it to
     * format the given arguments.  The pattern must identifyarguments
     * by name instead of by number.
     * <p>
     * @throws IllegalArgumentException if the pattern is invalid,
     *         or if an argument in the <code>arguments</code> map
     *         is not of the type expected by the format element(s)
     *         that use it.
     * @see #format(Map, StringBuffer, FieldPosition)
     * @see #format(String, Object[])
     * @stable ICU 3.8
     */
    public static String format(String pattern, Map<String, Object> arguments) {
        MessageFormat temp = new MessageFormat(pattern);
        return temp.format(arguments);
    }

    /**
     * Returns true if this MessageFormat uses named arguments,
     * and false otherwise.  See class description.
     *
     * @return true if named arguments are used.
     * @stable ICU 3.8
     */
    public boolean usesNamedArguments() {
        return !argumentNamesAreNumeric;
    }

    // Overrides
    /**
     * Formats a map or array of objects and appends the <code>MessageFormat</code>'s
     * pattern, with format elements replaced by the formatted objects, to the
     * provided <code>StringBuffer</code>.
     * This is equivalent to either of
     * <blockquote>
     *     <code>{@link #format(java.lang.Object[], java.lang.StringBuffer, 
     *     java.text.FieldPosition) format}((Object[]) arguments, result, pos)</code>
     *     <code>{@link #format(java.util.Map, java.lang.StringBuffer, 
     *     java.text.FieldPosition) format}((Map) arguments, result, pos)</code>
     * </blockquote>
     * A map must be provided if this format uses named arguments, otherwise
     * an IllegalArgumentException will be thrown.
     * @param arguments a map or array of objects to be formatted
     * @param result where text is appended
     * @param pos On input: an alignment field, if desired
     *            On output: the offsets of the alignment field
     * @throws IllegalArgumentException if an argument in
     *         <code>arguments</code> is not of the type
     *         expected by the format element(s) that use it
     * @throws IllegalArgumentException if <code>arguments<code> is
     *         an array of Object and this format uses named arguments
     * @stable ICU 3.0
     */
    @SuppressWarnings("unchecked")
    public final StringBuffer format(Object arguments, StringBuffer result,
                                     FieldPosition pos)
    {
        if ((arguments == null || arguments instanceof Map)) {
            return subformat((Map<String, Object>)arguments, result, pos, null);
        } else {
            if (!argumentNamesAreNumeric) {
                throw new IllegalArgumentException(
                        "This method is not available in MessageFormat objects " +
                        "that use alphanumeric argument names.");
            }
            return subformat((Object[]) arguments, result, pos, null);
        }
    }

    /**
     * Formats an array of objects and inserts them into the
     * <code>MessageFormat</code>'s pattern, producing an
     * <code>AttributedCharacterIterator</code>.
     * You can use the returned <code>AttributedCharacterIterator</code>
     * to build the resulting String, as well as to determine information
     * about the resulting String.
     * <p>
     * The text of the returned <code>AttributedCharacterIterator</code> is
     * the same that would be returned by
     * <blockquote>
     *     <code>{@link #format(java.lang.Object[], java.lang.StringBuffer, 
     *     java.text.FieldPosition) format}(arguments, new StringBuffer(), null).toString()</code>
     * </blockquote>
     * <p>
     * In addition, the <code>AttributedCharacterIterator</code> contains at
     * least attributes indicating where text was generated from an
     * argument in the <code>arguments</code> array. The keys of these attributes are of
     * type <code>MessageFormat.Field</code>, their values are
     * <code>Integer</code> objects indicating the index in the <code>arguments</code>
     * array of the argument from which the text was generated.
     * <p>
     * The attributes/value from the underlying <code>Format</code>
     * instances that <code>MessageFormat</code> uses will also be
     * placed in the resulting <code>AttributedCharacterIterator</code>.
     * This allows you to not only find where an argument is placed in the
     * resulting String, but also which fields it contains in turn.
     *
     * @param arguments an array of objects to be formatted and substituted.
     * @return AttributedCharacterIterator describing the formatted value.
     * @exception NullPointerException if <code>arguments</code> is null.
     * @exception IllegalArgumentException if an argument in the
     *            <code>arguments</code> array is not of the type
     *            expected by the format element(s) that use it.
     * @stable ICU 3.8
     */
    @SuppressWarnings("unchecked")
    public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
        StringBuffer result = new StringBuffer();
        ArrayList<AttributedCharacterIterator> iterators = 
            new ArrayList<AttributedCharacterIterator>();

        if (arguments == null) {
            throw new NullPointerException(
                   "formatToCharacterIterator must be passed non-null object");
        }
        if (arguments instanceof Map) {
            subformat((Map<String, Object>)arguments, result, null, iterators);
        } else {
            subformat((Object[]) arguments, result, null, iterators);
        }
        if (iterators.size() == 0) {
            return _createAttributedCharacterIterator("");
        }
        return _createAttributedCharacterIterator(
                     iterators.toArray(new AttributedCharacterIterator[iterators.size()]));
    }

    /**
     * Parses the string.
     *
     * <p>Caveats: The parse may fail in a number of circumstances.
     * For example:
     * <ul>
     * <li>If one of the arguments does not occur in the pattern.
     * <li>If the format of an argument loses information, such as
     *     with a choice format where a large number formats to "many".
     * <li>Does not yet handle recursion (where
     *     the substituted strings contain {n} references.)
     * <li>Will not always find a match (or the correct match)
     *     if some part of the parse is ambiguous.
     *     For example, if the pattern "{1},{2}" is used with the
     *     string arguments {"a,b", "c"}, it will format as "a,b,c".
     *     When the result is parsed, it will return {"a", "b,c"}.
     * <li>If a single argument is parsed more than once in the string,
     *     then the later parse wins.
     * </ul>
     * When the parse fails, use ParsePosition.getErrorIndex() to find out
     * where in the string did the parsing failed. The returned error
     * index is the starting offset of the sub-patterns that the string
     * is comparing with. For example, if the parsing string "AAA {0} BBB"
     * is comparing against the pattern "AAD {0} BBB", the error index is
     * 0. When an error occurs, the call to this method will return null.
     * If the source is null, return an empty array.
     * <p>
     * This method is only supported with numbered arguments.  If
     * the format pattern used named argument an
     * IllegalArgumentException is thrown.
     *
     * @throws IllegalArgumentException if this format uses named arguments
     * @stable ICU 3.0
     */
    public Object[] parse(String source, ParsePosition pos) {
        if (!argumentNamesAreNumeric) {
            throw new IllegalArgumentException(
                    "This method is not available in MessageFormat objects " +
                    "that use named argument.");
        }
        Map<String, Object> objectMap = parseToMap(source, pos);
        int maximumArgumentNumber = -1;
        for (int i = 0; i <= maxOffset; i++) {
            int argumentNumber = Integer.parseInt(argumentNames[i]);
            if (argumentNumber > maximumArgumentNumber) {
                maximumArgumentNumber = argumentNumber;
            }
        }

        if (objectMap == null) {
            return null;
        }

        Object[] resultArray = new Object[maximumArgumentNumber + 1];
        for (String key : objectMap.keySet()) {
            resultArray[Integer.parseInt(key)] = objectMap.get(key);
        }

        return resultArray;
    }

    /**
     * Parses the string, returning the results in a Map.
     * This is similar to the version that returns an array
     * of Object.  This supports both named and numbered
     * arguments-- if numbered, the keys in the map are the
     * corresponding Strings (e.g. "0", "1", "2"...).
     *
     * @param source the text to parse
     * @param pos the position at which to start parsing.  on return,
     *        contains the result of the parse.
     * @return a Map containing key/value pairs for each parsed argument.
     * @stable ICU 3.8
     */
    public Map<String, Object> parseToMap(String source, ParsePosition pos) {
        if (source == null) {
            Map<String, Object> empty = new HashMap<String, Object>();
            return empty;
        }

//        int maximumArgumentNumber = -1;
//        for (int i = 0; i <= maxOffset; i++) {
//           int argumentNumber = Integer.parseInt(argumentNames[i]);
//           if (argumentNumber > maximumArgumentNumber) {
//               maximumArgumentNumber = argumentNumber;
//           }
//         }
//        Object[] resultArray = new Object[maximumArgumentNumber + 1];

        Map<String, Object> resultMap = new HashMap<String, Object>();

        int patternOffset = 0;
        int sourceOffset = pos.getIndex();
        ParsePosition tempStatus = new ParsePosition(0);
        for (int i = 0; i <= maxOffset; ++i) {
            // match up to format
            int len = offsets[i] - patternOffset;
            if (len == 0 || pattern.regionMatches(patternOffset,
                                                  source, sourceOffset, len)) {
                sourceOffset += len;
                patternOffset += len;
            } else {
                pos.setErrorIndex(sourceOffset);
                return null; // leave index as is to signal error
            }

            // now use format
            if (formats[i] == null) {   // string format
                // if at end, use longest possible match
                // otherwise uses first match to intervening string
                // does NOT recursively try all possibilities
                int tempLength = (i != maxOffset) ? offsets[i+1] : pattern.length();

                int next;
                if (patternOffset >= tempLength) {
                    next = source.length();
                }else{
                    next = source.indexOf( pattern.substring(patternOffset,tempLength), sourceOffset);
                }

                if (next < 0) {
                    pos.setErrorIndex(sourceOffset);
                    return null; // leave index as is to signal error
                } else {
                    String strValue = source.substring(sourceOffset, next);
                    if (!strValue.equals("{" + argumentNames[i] + "}"))
                        resultMap.put(argumentNames[i], source.substring(sourceOffset, next));
//                        resultArray[Integer.parseInt(argumentNames[i])] =
//                            source.substring(sourceOffset, next);
                    sourceOffset = next;
                }
            } else {
                tempStatus.setIndex(sourceOffset);
                resultMap.put(argumentNames[i], formats[i].parseObject(source, tempStatus));
//                resultArray[Integer.parseInt(argumentNames[i])] =
//                    formats[i].parseObject(source, tempStatus);
                if (tempStatus.getIndex() == sourceOffset) {
                    pos.setErrorIndex(sourceOffset);
                    return null; // leave index as is to signal error
                }
                sourceOffset = tempStatus.getIndex(); // update
            }
        }
        int len = pattern.length() - patternOffset;
        if (len == 0 || pattern.regionMatches(patternOffset,
                                              source, sourceOffset, len)) {
            pos.setIndex(sourceOffset + len);
        } else {
            pos.setErrorIndex(sourceOffset);
            return null; // leave index as is to signal error
        }
        return resultMap;
    }

    /**
     * Parses text from the beginning of the given string to produce an object
     * array.
     * The method may not use the entire text of the given string.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on message parsing.
     *
     * @param source A <code>String</code> whose beginning should be parsed.
     * @return An <code>Object</code> array parsed from the string.
     * @exception ParseException if the beginning of the specified string cannot be parsed.
     * @exception IllegalArgumentException if this format uses named arguments
     * @stable ICU 3.0
     */
    public Object[] parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object[] result = parse(source, pos);
        if (pos.getIndex() == 0) // unchanged, returned object is null
            throw new ParseException("MessageFormat parse error!",
                                     pos.getErrorIndex());

        return result;
    }

    /**
     * Parses text from the beginning of the given string to produce a map from
     * argument to values. The method may not use the entire text of the given string.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on message parsing.
     *
     * @param source A <code>String</code> whose beginning should be parsed.
     * @return A <code>Map</code> parsed from the string.
     * @throws ParseException if the beginning of the specified string cannot
     *         be parsed.
     * @see #parseToMap(String, ParsePosition)
     * @stable ICU 3.8
     */
    public Map<String, Object> parseToMap(String source) throws ParseException {

        ParsePosition pos = new ParsePosition(0);
        Map<String, Object> result = parseToMap(source, pos);
        if (pos.getIndex() == 0) // unchanged, returned object is null
            throw new ParseException("MessageFormat parse error!",
                                     pos.getErrorIndex());

        return result;
    }

    /**
     * Parses text from a string to produce an object array or Map.
     * <p>
     * The method attempts to parse text starting at the index given by
     * <code>pos</code>.
     * If parsing succeeds, then the index of <code>pos</code> is updated
     * to the index after the last character used (parsing does not necessarily
     * use all characters up to the end of the string), and the parsed
     * object array is returned. The updated <code>pos</code> can be used to
     * indicate the starting point for the next call to this method.
     * If an error occurs, then the index of <code>pos</code> is not
     * changed, the error index of <code>pos</code> is set to the index of
     * the character where the error occurred, and null is returned.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on message parsing.
     *
     * @param source A <code>String</code>, part of which should be parsed.
     * @param pos A <code>ParsePosition</code> object with index and error
     *            index information as described above.
     * @return An <code>Object</code> parsed from the string, either an
     *         array of Object, or a Map, depending on whether named
     *         arguments are used.  This can be queried using <code>usesNamedArguments</code>.
     *         In case of error, returns null.
     * @throws NullPointerException if <code>pos</code> is null.
     * @stable ICU 3.0
     */
    public Object parseObject(String source, ParsePosition pos) {
        if (argumentNamesAreNumeric) {
            return parse(source, pos);
        } else {
            return parseToMap(source, pos);
        }
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a clone of this instance.
     * @stable ICU 3.0
     */
    public Object clone() {
        MessageFormat other = (MessageFormat) super.clone();

        // clone arrays. Can't do with utility because of bug in Cloneable
        other.formats = formats.clone(); // shallow clone
        for (int i = 0; i < formats.length; ++i) {
            if (formats[i] != null)
                other.formats[i] = (Format) formats[i].clone();
        }
        // for primitives or immutables, shallow clone is enough
        other.offsets = offsets.clone();
        other.argumentNames = argumentNames.clone();
        other.argumentNamesAreNumeric = argumentNamesAreNumeric;

        return other;
    }

    /**
     * Equality comparison between two message format objects
     * @stable ICU 3.0
     */
    public boolean equals(Object obj) {
        if (this == obj)                      // quick check
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MessageFormat other = (MessageFormat) obj;
        return (maxOffset == other.maxOffset
                && pattern.equals(other.pattern)
                && Utility.objectEquals(ulocale, other.ulocale) // does null check
                && Utility.arrayEquals(offsets, other.offsets)
                && Utility.arrayEquals(argumentNames, other.argumentNames)
                && Utility.arrayEquals(formats, other.formats)
                && (argumentNamesAreNumeric == other.argumentNamesAreNumeric));
    }

    /**
     * Generates a hash code for the message format object.
     * @stable ICU 3.0
     */
    public int hashCode() {
        return pattern.hashCode(); // enough for reasonable distribution
    }

    /**
     * Defines constants that are used as attribute keys in the
     * <code>AttributedCharacterIterator</code> returned
     * from <code>MessageFormat.formatToCharacterIterator</code>.
     *
     * @stable ICU 3.8
     */
    public static class Field extends Format.Field {

        private static final long serialVersionUID = 7510380454602616157L;

        /**
         * Create a <code>Field</code> with the specified name.
         *
         * @param name The name of the attribute
         *
         * @stable ICU 3.8
         */
        protected Field(String name) {
            super(name);
        }

        /**
         * Resolves instances being deserialized to the predefined constants.
         *
         * @return resolved MessageFormat.Field constant
         * @throws InvalidObjectException if the constant could not be resolved.
         *
         * @stable ICU 3.8
         */
        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != MessageFormat.Field.class) {
                throw new InvalidObjectException(
                    "A subclass of MessageFormat.Field must implement readResolve.");
            }
            if (this.getName().equals(ARGUMENT.getName())) {
                return ARGUMENT;
            } else {
                throw new InvalidObjectException("Unknown attribute name.");
            }
        }

        /**
         * Constant identifying a portion of a message that was generated
         * from an argument passed into <code>formatToCharacterIterator</code>.
         * The value associated with the key will be an <code>Integer</code>
         * indicating the index in the <code>arguments</code> array of the
         * argument from which the text was generated.
         *
         * @stable ICU 3.8
         */
        public static final Field ARGUMENT = new Field("message argument field");

    }

    // ===========================privates============================

    /**
     * The locale to use for formatting numbers and dates.
     * This is no longer used, and here only for serialization compatibility.
     * @serial
     */
    private Locale locale;

    /**
     * The locale to use for formatting numbers and dates.
     * @serial
     */
    private ULocale ulocale;

    /**
     * The string that the formatted values are to be plugged into.  In other words, this
     * is the pattern supplied on construction with all of the {} expressions taken out.
     * @serial
     */
    private String pattern = "";

    /** The initially expected number of subformats in the format */
    private static final int INITIAL_FORMATS = 10;

    /**
     * An array of formatters, which are used to format the arguments.
     * @serial
     */
    private Format[] formats = new Format[INITIAL_FORMATS];

    /**
     * The positions where the results of formatting each argument are to be
     * inserted into the pattern.
     *
     * @serial
     */
    private int[] offsets = new int[INITIAL_FORMATS];

    /**
     * The argument numbers corresponding to each formatter.  (The formatters are stored
     * in the order they occur in the pattern, not in the order in which the arguments
     * are specified.)
     * @serial
     */
    // retained for backwards compatibility
    private int[] argumentNumbers = new int[INITIAL_FORMATS];

    /**
     * The argument names corresponding to each formatter. (The formatters are
     * stored in the order they occur in the pattern, not in the order in which
     * the arguments are specified.)
     *
     * @serial
     */
    private String[] argumentNames = new String[INITIAL_FORMATS];

    /**
     * Is true iff all argument names are non-negative numbers.
     *
     * @serial
     */
    private boolean argumentNamesAreNumeric = true;

    /**
     * One less than the number of entries in <code>offsets</code>.  Can also be thought of
     * as the index of the highest-numbered element in <code>offsets</code> that is being used.
     * All of these arrays should have the same number of elements being used as <code>offsets</code>
     * does, and so this variable suffices to tell us how many entries are in all of them.
     * @serial
     */
    private int maxOffset = -1;

    /**
     * Internal routine used by format. If <code>characterIterators</code> is
     * non-null, AttributedCharacterIterator will be created from the
     * subformats as necessary. If <code>characterIterators</code> is null
     * and <code>fp</code> is non-null and identifies
     * <code>Field.MESSAGE_ARGUMENT</code>, the location of
     * the first replaced argument will be set in it.
     *
     * @exception IllegalArgumentException if an argument in the
     *            <code>arguments</code> array is not of the type
     *            expected by the format element(s) that use it.
     */
    private StringBuffer subformat(Object[] arguments, StringBuffer result,
        FieldPosition fp,  List<AttributedCharacterIterator> characterIterators) {
        return subformat(arrayToMap(arguments), result, fp, characterIterators);
    }

    /**
     * Internal routine used by format.
     *
     * @throws IllegalArgumentException if an argument in the
     *         <code>arguments</code> map is not of the type
     *         expected by the format element(s) that use it.
     */
    private StringBuffer subformat(Map<String, Object> arguments, StringBuffer result,
        FieldPosition fp, List<AttributedCharacterIterator> characterIterators) {
        // note: this implementation assumes a fast substring & index.
        // if this is not true, would be better to append chars one by one.
        int lastOffset = 0;
        int last = result.length();

        for (int i = 0; i <= maxOffset; ++i) {
            result.append(pattern.substring(lastOffset, offsets[i]));
            lastOffset = offsets[i];
            String argumentName = argumentNames[i];
            if (arguments == null || !arguments.containsKey(argumentName)) {
                result.append("{" + argumentName + "}");
                continue;
            }
            // int argRecursion = ((recursionProtection >> (argumentNumber*2)) & 0x3);
            //Eclipse stated the following is "dead code"
            /*if (false) { // if (argRecursion == 3){
                // prevent loop!!!
                result.append('\uFFFD');
            } else*/ {
                Object obj = arguments.get(argumentName);
                String arg = null;
                Format subFormatter = null;
                if (obj == null) {
                    arg = "null";
                } else if (formats[i] != null) {
                    subFormatter = formats[i];
                    if (subFormatter instanceof ChoiceFormat
                        || subFormatter instanceof PluralFormat
                        || subFormatter instanceof SelectFormat) {
                        arg = formats[i].format(obj);
                        // TODO: This should be made more robust.
                        //       Does this work with '{' in quotes?
                        if (arg.indexOf('{') >= 0) {
                            subFormatter = new MessageFormat(arg, ulocale);
                            obj = arguments;
                            arg = null;
                        }
                    }
                } else if (obj instanceof Number) {
                    // format number if can
                    subFormatter = NumberFormat.getInstance(ulocale);
                } else if (obj instanceof Date) {
                    // format a Date if can
                    subFormatter = DateFormat.getDateTimeInstance(
                             DateFormat.SHORT, DateFormat.SHORT, ulocale);//fix
                } else if (obj instanceof String) {
                    arg = (String) obj;

                } else {
                    arg = obj.toString();
                    if (arg == null) arg = "null";
                }

                // At this point we are in two states, either subFormatter
                // is non-null indicating we should format obj using it,
                // or arg is non-null and we should use it as the value.

                if (characterIterators != null) {
                    // If characterIterators is non-null, it indicates we need
                    // to get the CharacterIterator from the child formatter.
                    if (last != result.length()) {
                        characterIterators.add(
                            _createAttributedCharacterIterator(result.substring
                                                              (last)));
                        last = result.length();
                    }
                    if (subFormatter != null) {
                        AttributedCharacterIterator subIterator =
                                   subFormatter.formatToCharacterIterator(obj);

                        append(result, subIterator);
                        if (last != result.length()) {
                            characterIterators.add(
                                _createAttributedCharacterIterator(
                                subIterator, Field.ARGUMENT,
                                argumentNamesAreNumeric ? 
                                    (Object)new Integer(argumentName) : 
                                    (Object)argumentName));
                            last = result.length();
                        }
                        arg = null;
                    }
                    if (arg != null && arg.length() > 0) {
                        result.append(arg);
                        characterIterators.add(
                            _createAttributedCharacterIterator(
                                arg, Field.ARGUMENT,
                                argumentNamesAreNumeric ? 
                                    (Object)new Integer(argumentName) : 
                                    (Object)argumentName));
                        last = result.length();
                    }
                } else {
                    if (subFormatter != null) {
                        arg = subFormatter.format(obj);
                    }
                    last = result.length();
                    result.append(arg);
                    if (i == 0 && fp != null && Field.ARGUMENT.equals(
                                  fp.getFieldAttribute())) {
                        fp.setBeginIndex(last);
                        fp.setEndIndex(result.length());
                    }
                    last = result.length();
                }
            }
        }
        result.append(pattern.substring(lastOffset, pattern.length()));
        if (characterIterators != null && last != result.length()) {
            characterIterators.add(_createAttributedCharacterIterator(
                                   result.substring(last)));
        }
        return result;
    }

    /**
     * Convenience method to append all the characters in
     * <code>iterator</code> to the StringBuffer <code>result</code>.
     */
    private void append(StringBuffer result, CharacterIterator iterator) {
        if (iterator.first() != CharacterIterator.DONE) {
            char aChar;

            result.append(iterator.first());
            while ((aChar = iterator.next()) != CharacterIterator.DONE) {
                result.append(aChar);
            }
        }
    }

    private static final String[] typeList =
        {"", "number", "date", "time", "choice", "spellout", "ordinal",
         "duration", "plural", "select" };
    private static final int
        TYPE_EMPTY = 0,
        TYPE_NUMBER = 1,
        TYPE_DATE = 2,
        TYPE_TIME = 3,
        TYPE_CHOICE = 4,
        TYPE_SPELLOUT = 5,
        TYPE_ORDINAL = 6,
        TYPE_DURATION = 7,
        TYPE_PLURAL = 8, 
        TYPE_SELECT = 9;

    private static final String[] modifierList =
        {"", "currency", "percent", "integer"};

    private static final int
        MODIFIER_EMPTY = 0,
        MODIFIER_CURRENCY = 1,
        MODIFIER_PERCENT = 2,
        MODIFIER_INTEGER = 3;

    private static final String[] dateModifierList =
        {"", "short", "medium", "long", "full"};

    private static final int
        DATE_MODIFIER_EMPTY = 0,
        DATE_MODIFIER_SHORT = 1,
        DATE_MODIFIER_MEDIUM = 2,
        DATE_MODIFIER_LONG = 3,
        DATE_MODIFIER_FULL = 4;

    private void makeFormat(int position, int offsetNumber,
                            StringBuffer[] segments)
    {
        // get the argument number
        // int argumentNumber;
        // try {
        //     argumentNumber = Integer.parseInt(segments[1].toString()); // always unlocalized!
        // } catch (NumberFormatException e) {
        //    throw new IllegalArgumentException("can't parse argument number "
        //            + segments[1]);
        //}
        // if (argumentNumber < 0) {
        //    throw new IllegalArgumentException("negative argument number "
        //            + argumentNumber);
        //}

        // resize format information arrays if necessary
        if (offsetNumber >= formats.length) {
            int newLength = formats.length * 2;
            Format[] newFormats = new Format[newLength];
            int[] newOffsets = new int[newLength];
            String[] newArgumentNames = new String[newLength];
            System.arraycopy(formats, 0, newFormats, 0, maxOffset + 1);
            System.arraycopy(offsets, 0, newOffsets, 0, maxOffset + 1);
            System.arraycopy(argumentNames, 0, newArgumentNames, 0,
                    maxOffset + 1);
            formats = newFormats;
            offsets = newOffsets;
            argumentNames = newArgumentNames;
        }
        int oldMaxOffset = maxOffset;
        maxOffset = offsetNumber;
        offsets[offsetNumber] = segments[0].length();
        argumentNames[offsetNumber] = segments[1].toString();
        // All argument names numeric ?
        int argumentNumber;
        try {
            // always unlocalized!
             argumentNumber = Integer.parseInt(segments[1].toString());
         } catch (NumberFormatException e) {
             argumentNumber = -1;
         }
         if (offsetNumber == 0) {
             // First argument determines whether all argument identifiers have
             // to be numbers or (IDStartChars IDContChars*) strings.
             argumentNamesAreNumeric = argumentNumber >= 0;
         }

         if (argumentNamesAreNumeric && argumentNumber < 0 ||
             !argumentNamesAreNumeric &&
             !isAlphaIdentifier(argumentNames[offsetNumber])) {
             throw new IllegalArgumentException(
                     "All argument identifiers have to be either non-negative " +
                     "numbers or strings following the pattern " +
                     "([:ID_Start:] [:ID_Continue:]*).\n" +
                     "For more details on these unicode sets, visit " +
                     "http://demo.icu-project.org/icu-bin/ubrowse");
         }

        // now get the format
        Format newFormat = null;
        int subformatType  = findKeyword(segments[2].toString(), typeList);
        switch (subformatType){
        case TYPE_EMPTY:
            break;
        case TYPE_NUMBER:
            switch (findKeyword(segments[3].toString(), modifierList)) {
            case MODIFIER_EMPTY:
                newFormat = NumberFormat.getInstance(ulocale);
                break;
            case MODIFIER_CURRENCY:
                newFormat = NumberFormat.getCurrencyInstance(ulocale);
                break;
            case MODIFIER_PERCENT:
                newFormat = NumberFormat.getPercentInstance(ulocale);
                break;
            case MODIFIER_INTEGER:
                newFormat = NumberFormat.getIntegerInstance(ulocale);
                break;
            default: // pattern
                newFormat = new DecimalFormat(segments[3].toString(), 
                        new DecimalFormatSymbols(ulocale));
                break;
            }
            break;
        case TYPE_DATE:
            switch (findKeyword(segments[3].toString(), dateModifierList)) {
            case DATE_MODIFIER_EMPTY:
                newFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, ulocale);
                break;
            case DATE_MODIFIER_SHORT:
                newFormat = DateFormat.getDateInstance(DateFormat.SHORT, ulocale);
                break;
            case DATE_MODIFIER_MEDIUM:
                newFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, ulocale);
                break;
            case DATE_MODIFIER_LONG:
                newFormat = DateFormat.getDateInstance(DateFormat.LONG, ulocale);
                break;
            case DATE_MODIFIER_FULL:
                newFormat = DateFormat.getDateInstance(DateFormat.FULL, ulocale);
                break;
            default:
                newFormat = new SimpleDateFormat(segments[3].toString(), ulocale);
                break;
            }
            break;
        case TYPE_TIME:
            switch (findKeyword(segments[3].toString(), dateModifierList)) {
            case DATE_MODIFIER_EMPTY:
                newFormat = DateFormat.getTimeInstance(DateFormat.DEFAULT, ulocale);
                break;
            case DATE_MODIFIER_SHORT:
                newFormat = DateFormat.getTimeInstance(DateFormat.SHORT, ulocale);
                break;
            case DATE_MODIFIER_MEDIUM:
                newFormat = DateFormat.getTimeInstance(DateFormat.DEFAULT, ulocale);
                break;
            case DATE_MODIFIER_LONG:
                newFormat = DateFormat.getTimeInstance(DateFormat.LONG, ulocale);
                break;
            case DATE_MODIFIER_FULL:
                newFormat = DateFormat.getTimeInstance(DateFormat.FULL, ulocale);
                break;
            default:
                newFormat = new SimpleDateFormat(segments[3].toString(), ulocale);
                break;
            }
            break;
        case TYPE_CHOICE:
            try {
                newFormat = new ChoiceFormat(segments[3].toString());
            } catch (Exception e) {
                maxOffset = oldMaxOffset;
                throw new IllegalArgumentException("Choice Pattern incorrect");
            }
            break;
        case TYPE_SPELLOUT:
            {
                RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ulocale, 
                        RuleBasedNumberFormat.SPELLOUT);
                String ruleset = segments[3].toString().trim();
                if (ruleset.length() != 0) {
                    try {
                        rbnf.setDefaultRuleSet(ruleset);
                    }
                    catch (Exception e) {
                        // warn invalid ruleset
                    }
                }
                newFormat = rbnf;
            }
            break;
        case TYPE_ORDINAL:
            {
                RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ulocale, 
                        RuleBasedNumberFormat.ORDINAL);
                String ruleset = segments[3].toString().trim();
                if (ruleset.length() != 0) {
                    try {
                        rbnf.setDefaultRuleSet(ruleset);
                    }
                    catch (Exception e) {
                        // warn invalid ruleset
                    }
                }
                newFormat = rbnf;
            }
            break;
        case TYPE_DURATION:
            {
                RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ulocale, 
                        RuleBasedNumberFormat.DURATION);
                String ruleset = segments[3].toString().trim();
                if (ruleset.length() != 0) {
                    try {
                        rbnf.setDefaultRuleSet(ruleset);
                    }
                    catch (Exception e) {
                        // warn invalid ruleset
                    }
                }
                newFormat = rbnf;
            }
            break;
        case TYPE_PLURAL:
        case TYPE_SELECT:
            {
                // PluralFormat and SelectFormat does not handle quotes.
                // Remove quotes.
                // TODO: Should PluralFormat and SelectFormat handle quotes?
                StringBuffer unquotedPattern = new StringBuffer();
                String quotedPattern = segments[3].toString();
                boolean inQuote = false;
                for (int i = 0; i < quotedPattern.length(); ++i) {
                    char ch = quotedPattern.charAt(i);
                    if (ch == '\'') {
                        if (i+1 < quotedPattern.length() &&
                            quotedPattern.charAt(i+1) == '\'') {
                                unquotedPattern.append(ch);
                                ++i;
                            } else {
                                inQuote = !inQuote;
                            }
                    } else {
                        unquotedPattern.append(ch);
                    }
                }

                if( subformatType == TYPE_PLURAL){
                    PluralFormat pls = new PluralFormat(ulocale,
                                                    unquotedPattern.toString());
                    newFormat = pls;
                }else{
                    newFormat = new SelectFormat( unquotedPattern.toString());
                }
            }
            break;
        default:
            maxOffset = oldMaxOffset;
            throw new IllegalArgumentException("unknown format type at ");
        }
        formats[offsetNumber] = newFormat;
        segments[1].setLength(0);   // throw away other segments
        segments[2].setLength(0);
        segments[3].setLength(0);
    }

    private static final int findKeyword(String s, String[] list) {
        s = s.trim().toLowerCase();
        for (int i = 0; i < list.length; ++i) {
            if (s.equals(list[i]))
                return i;
        }
        return -1;
    }

    private static final void copyAndFixQuotes(String source, int start, int end, 
            StringBuffer target) {
        // added 'gotLB' logic from ICU4C - questionable [alan]
        boolean gotLB = false;
        for (int i = start; i < end; ++i) {
            char ch = source.charAt(i);
            if (ch == '{') {
                target.append("'{'");
                gotLB = true;
            } else if (ch == '}') {
                if (gotLB) {
                    target.append(ch);
                    gotLB = false;
                } else {
                    target.append("'}'");
                }
            } else if (ch == '\'') {
                target.append("''");
            } else {
                target.append(ch);
            }
        }
    }

    /**
     * After reading an object from the input stream, do a simple verification
     * to maintain class invariants.
     * @throws InvalidObjectException if the objects read from the stream is invalid.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (argumentNames == null) { // name mod, rev
          argumentNamesAreNumeric = true;
          argumentNames = new String[argumentNumbers.length];
          for (int i = 0; i < argumentNumbers.length; ++i) {
            argumentNames[i] = String.valueOf(argumentNumbers[i]);
          }
        }
        boolean isValid = maxOffset >= -1
                && formats.length > maxOffset
                && offsets.length > maxOffset
                && argumentNames.length > maxOffset;
        if (isValid) {
            int lastOffset = pattern.length() + 1;
            for (int i = maxOffset; i >= 0; --i) {
                if ((offsets[i] < 0) || (offsets[i] > lastOffset)) {
                    isValid = false;
                    break;
                } else {
                    lastOffset = offsets[i];
                }
            }
        }
        if (!isValid) {
            throw new InvalidObjectException(
                "Could not reconstruct MessageFormat from corrupt stream.");
        }
        if (ulocale == null) {
            ulocale = ULocale.forLocale(locale);
        }
    }

    /**
     * This is a helper method for converting an object array into a map. The
     * key set of the map is [0, ..., array.length]. The value associated with
     * each key is the ith entry of the passed object array.
     *
     * @throws InvalidObjectException
     *             if the objects read from the stream is invalid.
     */
    private Map<String, Object> arrayToMap(Object[] array) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (array != null) {
            for (int i = 0; i < array.length; ++i) {
                map.put(Integer.toString(i), array[i]);
            }
        }
        return map;
    }

    private boolean isAlphaIdentifier(String argument) {
        if (argument.length() == 0) {
            return false;
        }
        for (int i = 0; i < argument.length(); ++i ) {
            if (i == 0 && !UCharacter.isUnicodeIdentifierStart(argument.charAt(i)) ||
                    i > 0 &&  !UCharacter.isUnicodeIdentifierPart(argument.charAt(i))){
                    return false;
                }
        }
        return true;
    }

    private static final char SINGLE_QUOTE = '\'';
    private static final char CURLY_BRACE_LEFT = '{';
    private static final char CURLY_BRACE_RIGHT = '}';

    private static final int STATE_INITIAL = 0;
    private static final int STATE_SINGLE_QUOTE = 1;
    private static final int STATE_IN_QUOTE = 2;
    private static final int STATE_MSG_ELEMENT = 3;

    /**
     * Convert an 'apostrophe-friendly' pattern into a standard
     * pattern.  Standard patterns treat all apostrophes as
     * quotes, which is problematic in some languages, e.g.
     * French, where apostrophe is commonly used.  This utility
     * assumes that only an unpaired apostrophe immediately before
     * a brace is a true quote.  Other unpaired apostrophes are paired,
     * and the resulting standard pattern string is returned.
     *
     * <p><b>Note</b> it is not guaranteed that the returned pattern
     * is indeed a valid pattern.  The only effect is to convert
     * between patterns having different quoting semantics.
     *
     * @param pattern the 'apostrophe-friendly' patttern to convert
     * @return the standard equivalent of the original pattern
     * @stable ICU 3.4
     */
    public static String autoQuoteApostrophe(String pattern) {
        StringBuffer buf = new StringBuffer(pattern.length() * 2);
        int state = STATE_INITIAL;
        int braceCount = 0;
        for (int i = 0, j = pattern.length(); i < j; ++i) {
            char c = pattern.charAt(i);
            switch (state) {
            case STATE_INITIAL:
                switch (c) {
                case SINGLE_QUOTE:
                    state = STATE_SINGLE_QUOTE;
                    break;
                case CURLY_BRACE_LEFT:
                    state = STATE_MSG_ELEMENT;
                    ++braceCount;
                    break;
                }
                break;
            case STATE_SINGLE_QUOTE:
                switch (c) {
                case SINGLE_QUOTE:
                    state = STATE_INITIAL;
                    break;
                case CURLY_BRACE_LEFT:
                case CURLY_BRACE_RIGHT:
                    state = STATE_IN_QUOTE;
                    break;
                default:
                    buf.append(SINGLE_QUOTE);
                    state = STATE_INITIAL;
                    break;
                }
                break;
            case STATE_IN_QUOTE:
                switch (c) {
                case SINGLE_QUOTE:
                    state = STATE_INITIAL;
                    break;
                }
                break;
            case STATE_MSG_ELEMENT:
                switch (c) {
                case CURLY_BRACE_LEFT:
                    ++braceCount;
                    break;
                case CURLY_BRACE_RIGHT:
                    if (--braceCount == 0) {
                        state = STATE_INITIAL;
                    }
                    break;
                }
                break;
            ///CLOVER:OFF
            default: // Never happens.
                break;
            ///CLOVER:ON
            }
            buf.append(c);
        }
        // End of scan
        if (state == STATE_SINGLE_QUOTE || state == STATE_IN_QUOTE) {
            buf.append(SINGLE_QUOTE);
        }
        return new String(buf);
    }

    //
    // private methods for AttributedCharacterIterator support
    //
    // Note: The equivalent methods are defined as package local methods in
    //       java.text.Format.  ICU cannot access these methods, so we have
    //       these methods locally, with "_" prefix for avoiding name collision.
    //       (The collision itself is not a problem, but Eclipse displays warnings
    //       by the default warning level.)  We may move these utility methods
    //       up to com.ibm.icu.text.UFormat later.  Yoshito

    private static AttributedCharacterIterator _createAttributedCharacterIterator(String text) {
        AttributedString as = new AttributedString(text);
        return as.getIterator();
    }

    private static AttributedCharacterIterator _createAttributedCharacterIterator(
            AttributedCharacterIterator[] iterators) {
        if (iterators == null || iterators.length == 0) {
            return _createAttributedCharacterIterator("");
        }
        // Create a single AttributedString
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < iterators.length; i++) {
            int index = iterators[i].getBeginIndex();
            int end = iterators[i].getEndIndex();
            while (index < end) {
                sb.append(iterators[i].setIndex(index++));
            }
        }
        AttributedString as = new AttributedString(sb.toString());

        // Set attributes
        int offset = 0;
        for (int i = 0; i < iterators.length; i++) {
            iterators[i].first();
            int start = iterators[i].getBeginIndex();
            while (true) {
                Map<Attribute, Object> map = iterators[i].getAttributes();
                int len = iterators[i].getRunLimit() - start; // run length
                if (map.size() > 0) {
                    for (Map.Entry<Attribute, Object> entry : map.entrySet()) {
                        as.addAttribute(entry.getKey(), entry.getValue(),
                                offset, offset + len);
                    }
                }
                offset += len;
                start += len;
                iterators[i].setIndex(start);
                if (iterators[i].current() == CharacterIterator.DONE) {
                    break;
                }
            }
        }

        return as.getIterator();
    }

    private static AttributedCharacterIterator _createAttributedCharacterIterator(
            AttributedCharacterIterator iterator,
            AttributedCharacterIterator.Attribute key, Object value) {
        AttributedString as = new AttributedString(iterator);
        as.addAttribute(key, value);
        return as.getIterator();
    }

    private static AttributedCharacterIterator _createAttributedCharacterIterator(String text,
            AttributedCharacterIterator.Attribute key, Object value) {
        AttributedString as = new AttributedString(text);
        as.addAttribute(key, value);
        return as.getIterator();
    }
}
