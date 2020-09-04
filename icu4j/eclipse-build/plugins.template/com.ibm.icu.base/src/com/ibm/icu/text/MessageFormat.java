// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**********************************************************************
* Copyright (c) 2004-2012, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 6, 2004
* Since: ICU 3.0
**********************************************************************
*/
package com.ibm.icu.text;

import java.io.InvalidObjectException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;

/**
 * {@icuenhanced java.text.MessageFormat}.{@icu _usage_}
 *
 * <p>MessageFormat produces concatenated messages in a language-neutral
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
 *
 * <p>Within a <i>String</i>, <code>"''"</code> represents a single
 * quote. A <i>QuotedString</i> can contain arbitrary characters
 * except single quotes; the surrounding single quotes are removed.
 * An <i>UnquotedString</i> can contain arbitrary characters
 * except single quotes and left curly brackets. Thus, a string that
 * should result in the formatted message "'{0}'" can be written as
 * <code>"'''{'0}''"</code> or <code>"'''{0}'''"</code>.
 *
 * <p>Within a <i>SubformatPattern</i>, different rules apply.
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
 *
 * <p><dl><dt><b>Warning:</b><dd>The rules for using quotes within message
 * format patterns unfortunately have shown to be somewhat confusing.
 * In particular, it isn't always obvious to localizers whether single
 * quotes need to be doubled or not. Make sure to inform localizers about
 * the rules, and tell them (for example, by using comments in resource
 * bundle source files) which strings will be processed by MessageFormat.
 * Note that localizers may need to use single quotes in translated
 * strings where the original version doesn't have them.
 *
 * <br>Note also that the simplest way to avoid the problem is to
 * use the real apostrophe (single quote) character \u2019 (') for
 * human-readable text, and to use the ASCII apostrophe (\u0027 ' )
 * only in program syntax, like quoting in MessageFormat.
 * See the annotations for U+0027 Apostrophe in The Unicode Standard.</p>
 * </dl>
 *
 * <p>The <i>ArgumentIndex</i> value is a non-negative integer written
 * using the digits '0' through '9', and represents an index into the
 * <code>arguments</code> array passed to the <code>format</code> methods
 * or the result array returned by the <code>parse</code> methods.
 *
 * <p>The <i>FormatType</i> and <i>FormatStyle</i> values are used to create
 * a <code>Format</code> instance for the format element. The following
 * table shows how the values map to Format instances. Combinations not
 * shown in the table are illegal. A <i>SubformatPattern</i> must
 * be a valid pattern string for the Format subclass used.
 *
 * <p><table border=1>
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
 * <p>Here are some examples of usage:
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
 * <p>Example 2:
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
 * <p>For more sophisticated patterns, you can use a <code>ChoiceFormat</code> to get
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
 *
 * <p><strong>Note:</strong> As we see above, the string produced
 * by a <code>ChoiceFormat</code> in <code>MessageFormat</code> is treated specially;
 * occurances of '{' are used to indicated subformats, and cause recursion.
 * If you create both a <code>MessageFormat</code> and <code>ChoiceFormat</code>
 * programmatically (instead of using the string patterns), then be careful not to
 * produce a format that recurses on itself, which will cause an infinite loop.
 *
 * <p>When a single argument is parsed more than once in the string, the last match
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
 *
 * <p>Likewise, parsing with a MessageFormat object using patterns containing
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
 * <p>Message formats are not synchronized.
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
    static final long serialVersionUID = 1L;

    /**
     * @internal
     */
    public final java.text.MessageFormat messageFormat;
        
    /**
     * @internal
     * @param delegate the DateFormat to which to delegate
     */
    public MessageFormat(java.text.MessageFormat delegate) {
        wrapNestedFormatters(delegate);
        this.messageFormat = delegate;
    }

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
        this(new java.text.MessageFormat(pattern, ULocale.getDefault(Category.FORMAT).toLocale()));
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
        this(new java.text.MessageFormat(pattern, locale));
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
        this(new java.text.MessageFormat(pattern, locale.toLocale()));
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
        messageFormat.setLocale(locale);
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
        messageFormat.setLocale(locale.toLocale());
    }

    /**
     * Returns the locale that's used when creating or comparing subformats.
     *
     * @return the locale used when creating or comparing subformats
     * @stable ICU 3.0
     */
    public Locale getLocale() {
        return messageFormat.getLocale();
    }

    /**
     * {@icu} Returns the locale that's used when creating or comparing subformats.
     *
     * @return the locale used when creating or comparing subformats
     * @stable ICU 3.2
     */
    public ULocale getULocale() {
        return ULocale.forLocale(messageFormat.getLocale());
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
    public void applyPattern(String pttrn) {
        messageFormat.applyPattern(pttrn);
        wrapNestedFormatters(messageFormat);
    }

//    /**
//     * {@icu} Sets the ApostropheMode and the pattern used by this message format.
//     * Parses the pattern and caches Format objects for simple argument types.
//     * Patterns and their interpretation are specified in the
//     * <a href="#patterns">class description</a>.
//     * <p>
//     * This method is best used only once on a given object to avoid confusion about the mode,
//     * and after constructing the object with an empty pattern string to minimize overhead.
//     *
//     * @param pattern the pattern for this message format
//     * @param aposMode the new ApostropheMode
//     * @throws IllegalArgumentException if the pattern is invalid
//     * @see MessagePattern.ApostropheMode
//     * @stable ICU 4.8
//     */
//    public void applyPattern(String pattern, MessagePattern.ApostropheMode aposMode) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu}
//     * @return this instance's ApostropheMode.
//     * @stable ICU 4.8
//     */
//    public MessagePattern.ApostropheMode getApostropheMode() {
//    	throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Returns a pattern representing the current state of the message format.
     * The string is constructed from internal information and therefore
     * does not necessarily equal the previously applied pattern.
     *
     * @return a pattern representing the current state of the message format
     * @stable ICU 3.0
     */
    public String toPattern() {
        String pattern = savedPattern == null ? messageFormat.toPattern() : savedPattern;
        return pattern;
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
        messageFormat.setFormatsByArgumentIndex(newFormats);
        savedPattern = null;
    }

//    /**
//     * {@icu} Sets the formats to use for the values passed into
//     * <code>format</code> methods or returned from <code>parse</code>
//     * methods. The keys in <code>newFormats</code> are the argument
//     * names in the previously set pattern string, and the values
//     * are the formats.
//     * <p>
//     * Only argument names from the pattern string are considered.
//     * Extra keys in <code>newFormats</code> that do not correspond
//     * to an argument name are ignored.  Similarly, if there is no
//     * format in newFormats for an argument name, the formatter
//     * for that argument remains unchanged.
//     * <p>
//     * This may be called on formats that do not use named arguments.
//     * In this case the map will be queried for key Strings that
//     * represent argument indices, e.g. "0", "1", "2" etc.
//     *
//     * @param newFormats a map from String to Format providing new
//     *        formats for named arguments.
//     * @stable ICU 3.8
//     */
//    public void setFormatsByArgumentName(Map<String, Format> newFormats) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

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
        messageFormat.setFormats(newFormats);
        savedPattern = null;
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
        messageFormat.setFormatByArgumentIndex(argumentIndex, newFormat);
        savedPattern = null;
    }

//    /**
//     * {@icu} Sets the format to use for the format elements within the
//     * previously set pattern string that use the given argument
//     * name.
//     * <p>
//     * If the argument name is used for more than one format element
//     * in the pattern string, then the new format is used for all such
//     * format elements. If the argument name is not used for any format
//     * element in the pattern string, then the new format is ignored.
//     * <p>
//     * This API may be used on formats that do not use named arguments.
//     * In this case <code>argumentName</code> should be a String that names
//     * an argument index, e.g. "0", "1", "2"... etc.  If it does not name
//     * a valid index, the format will be ignored.  No error is thrown.
//     *
//     * @param argumentName the name of the argument to change
//     * @param newFormat the new format to use
//     * @stable ICU 3.8
//     */
//    public void setFormatByArgumentName(String argumentName, Format newFormat) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

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
        messageFormat.setFormat(formatElementIndex, newFormat);
        savedPattern = null;
    }

    /**
     * Returns the formats used for the values passed into
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
        return messageFormat.getFormatsByArgumentIndex();
    }

    /**
     * Returns the formats used for the format elements in the
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
        return messageFormat.getFormats();
    }

//    /**
//     * {@icu} Returns the format argument names. For more details, see
//     * {@link #setFormatByArgumentName(String, Format)}.
//     * @return List of names
//     * @internal
//     * @deprecated This API is ICU internal only.
//     */
//    public Set<String> getFormatArgumentNames() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the first top-level format associated with the given argument name.
//     * For more details, see {@link #setFormatByArgumentName(String, Format)}.
//     * @param argumentName The name of the desired argument.
//     * @return the Format associated with the name, or null if there isn't one.
//     * @stable ICU 4.8
//     */
//    public Format getFormatByArgumentName(String argumentName) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the top-level argument names. For more details, see
//     * {@link #setFormatByArgumentName(String, Format)}.
//     * @return a Set of argument names
//     * @stable ICU 4.8
//     */
//    public Set<String> getArgumentNames() {
//    	throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }
    
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
                                     FieldPosition pos) {
        FieldPosition jdkPos = toJDKFieldPosition(pos);
        StringBuffer buf = messageFormat.format(arguments, result, jdkPos);
        if (jdkPos != null) {
            pos.setBeginIndex(jdkPos.getBeginIndex());
            pos.setEndIndex(jdkPos.getEndIndex());
        }
        return buf;
    }

//    /**
//     * Formats a map of objects and appends the <code>MessageFormat</code>'s
//     * pattern, with format elements replaced by the formatted objects, to the
//     * provided <code>StringBuffer</code>.
//     * <p>
//     * The text substituted for the individual format elements is derived from
//     * the current subformat of the format element and the
//     * <code>arguments</code> value corresopnding to the format element's
//     * argument name.
//     * <p>
//     * This API may be called on formats that do not use named arguments.
//     * In this case the the keys in <code>arguments</code> must be numeric
//     * strings (e.g. "0", "1", "2"...).
//     * <p>
//     * An argument is <i>unavailable</i> if <code>arguments</code> is
//     * <code>null</code> or does not have a value corresponding to an argument
//     * name in the pattern.  When an argument is unavailable no substitution
//     * is performed.
//     *
//     * @param arguments a map of objects to be formatted and substituted.
//     * @param result where text is appended.
//     * @param pos On input: an alignment field, if desired.
//     *            On output: the offsets of the alignment field.
//     * @throws IllegalArgumentException if an argument in the
//     *         <code>arguments</code> array is not of the type
//     *         expected by the format element(s) that use it.
//     * @return the passed-in StringBuffer
//     * @stable ICU 3.8
//     */
//    public final StringBuffer format(Map<String, Object> arguments, StringBuffer result,
//                                     FieldPosition pos) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

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
    public static String format(String pattern, Object... arguments) {
        return java.text.MessageFormat.format(pattern, arguments);
    }

//    /**
//     * Creates a MessageFormat with the given pattern and uses it to
//     * format the given arguments.  The pattern must identifyarguments
//     * by name instead of by number.
//     * <p>
//     * @throws IllegalArgumentException if the pattern is invalid,
//     *         or if an argument in the <code>arguments</code> map
//     *         is not of the type expected by the format element(s)
//     *         that use it.
//     * @see #format(Map, StringBuffer, FieldPosition)
//     * @see #format(String, Object[])
//     * @stable ICU 3.8
//     */
//    public static String format(String pattern, Map<String, Object> arguments) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * {@icu} Returns true if this MessageFormat uses named arguments,
     * and false otherwise.  See class description.
     *
     * @return true if named arguments are used.
     * @stable ICU 3.8
     */
    public boolean usesNamedArguments() {
        // always false with com.ibm.icu.base
        return false;
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
    public final StringBuffer format(Object arguments, StringBuffer result,
                                     FieldPosition pos) {
        FieldPosition jdkPos = toJDKFieldPosition(pos);
        StringBuffer buf = messageFormat.format(arguments, result, jdkPos);
        if (jdkPos != null) {
            pos.setBeginIndex(jdkPos.getBeginIndex());
            pos.setEndIndex(jdkPos.getEndIndex());
        }
        return buf;
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
    public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
        AttributedCharacterIterator it = messageFormat.formatToCharacterIterator(arguments);

        // Extract formatted String first
        StringBuilder sb = new StringBuilder();
        for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
            sb.append(c);
        }

        // Create AttributedString
        AttributedString attrstr = new AttributedString(sb.toString());

        // Map JDK Field to ICU Field
        int idx = 0;
        it.first();
        while (idx < it.getEndIndex()) {
            int end = it.getRunLimit();
            Map<Attribute, Object> attributes = it.getAttributes();
            if (attributes != null) {
                for (Entry<Attribute, Object> entry : attributes.entrySet()) {
                    Attribute attr = entry.getKey();
                    Object val = entry.getValue();
                    if (attr.equals(java.text.MessageFormat.Field.ARGUMENT)) {
                        val = attr = Field.ARGUMENT;
                    }
                    attrstr.addAttribute(attr, val, idx, end);
                }
            }
            idx = end;
            while (it.getIndex() < idx) {
                it.next();
            }
        }

        return attrstr.getIterator();
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
        return messageFormat.parse(source, pos);
    }

//    /**
//     * {@icu} Parses the string, returning the results in a Map.
//     * This is similar to the version that returns an array
//     * of Object.  This supports both named and numbered
//     * arguments-- if numbered, the keys in the map are the
//     * corresponding Strings (e.g. "0", "1", "2"...).
//     *
//     * @param source the text to parse
//     * @param pos the position at which to start parsing.  on return,
//     *        contains the result of the parse.
//     * @return a Map containing key/value pairs for each parsed argument.
//     * @stable ICU 3.8
//     */
//    public Map<String, Object> parseToMap(String source, ParsePosition pos) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

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
        return messageFormat.parse(source);
    }

//    /**
//     * {@icu} Parses text from the beginning of the given string to produce a map from
//     * argument to values. The method may not use the entire text of the given string.
//     *
//     * <p>See the {@link #parse(String, ParsePosition)} method for more information on
//     * message parsing.
//     *
//     * @param source A <code>String</code> whose beginning should be parsed.
//     * @return A <code>Map</code> parsed from the string.
//     * @throws ParseException if the beginning of the specified string cannot
//     *         be parsed.
//     * @see #parseToMap(String, ParsePosition)
//     * @stable ICU 3.8
//     */
//    public Map<String, Object> parseToMap(String source) throws ParseException {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

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
        return messageFormat.parse(source, pos);
    }

    /**
     * Overrides clone.
     *
     * @return a clone of this instance.
     * @stable ICU 3.0
     */
    public Object clone() {
        MessageFormat fmt = new MessageFormat((java.text.MessageFormat)messageFormat.clone());
        fmt.savedPattern = savedPattern;
        return fmt;
    }

    /**
     * Overrides equals.
     * @stable ICU 3.0
     */
    public boolean equals(Object obj) {
        try {
            return messageFormat.equals(((MessageFormat)obj).messageFormat);
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Overrides hashCode.
     * @stable ICU 3.0
     */
    public int hashCode() {
        return messageFormat.hashCode();
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

    private static final char SINGLE_QUOTE = '\'';
    private static final char CURLY_BRACE_LEFT = '{';
    private static final char CURLY_BRACE_RIGHT = '}';

    private static final int STATE_INITIAL = 0;
    private static final int STATE_SINGLE_QUOTE = 1;
    private static final int STATE_IN_QUOTE = 2;
    private static final int STATE_MSG_ELEMENT = 3;

    /**
     * {@icu} Converts an 'apostrophe-friendly' pattern into a standard
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
        StringBuilder buf = new StringBuilder(pattern.length() * 2);
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

    private static FieldPosition toJDKFieldPosition(FieldPosition icuPos) {
        if (icuPos == null) {
            return null;
        }

        int fieldID = icuPos.getField();
        Format.Field fieldAttribute = icuPos.getFieldAttribute();

        FieldPosition jdkPos = null;
        if (fieldAttribute != null) {
            // map field
            if (fieldAttribute.equals(Field.ARGUMENT)) {
                fieldAttribute = java.text.MessageFormat.Field.ARGUMENT;
            }
            jdkPos = new FieldPosition(fieldAttribute, fieldID);
        } else {
            jdkPos = new FieldPosition(fieldID);
        }

        jdkPos.setBeginIndex(icuPos.getBeginIndex());
        jdkPos.setEndIndex(icuPos.getEndIndex());

        return jdkPos;

    }

    private void wrapNestedFormatters(java.text.MessageFormat mfmt) {
        // Update nested formatters created by Java MessageFormat
        // with ICU versions, so FieldPosition / AttributedText will
        // use ICU formatter's definition, such as com.ibm.icu.text.NumberFormat.INTEGER_FIELD

        // Replacing nested formatter may change the pattern string
        // originally used. For example, "{0,integer} files" is replaced
        // with "{0} files". We preserve the original pattern.
        savedPattern = mfmt.toPattern();

        Format[] subfmts = mfmt.getFormats();
        for (int i = 0; i < subfmts.length; i++) {
            if (subfmts[i] instanceof java.text.DateFormat) {
                subfmts[i] = new DateFormat((java.text.DateFormat)subfmts[i]);
            } else if (subfmts[i] instanceof java.text.NumberFormat) {
                subfmts[i] = new NumberFormat((java.text.NumberFormat)subfmts[i]);
            }
        }
        mfmt.setFormats(subfmts);
    }

    private String savedPattern;
}
