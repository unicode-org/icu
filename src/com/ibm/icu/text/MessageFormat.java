/*
**********************************************************************
* Copyright (c) 2004, International Business Machines
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
import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;

/**
 * <code>MessageFormat</code> provides a means to produce concatenated
 * messages in language-neutral way. Use this to construct messages
 * displayed for end users.
 *
 * <p>
 * <code>MessageFormat</code> takes a set of objects, formats them, then
 * inserts the formatted strings into the pattern at the appropriate places.
 *
 * <p>
 * <strong>Note:</strong>
 * <code>MessageFormat</code> differs from the other <code>Format</code>
 * classes in that you create a <code>MessageFormat</code> object with one
 * of its constructors (not with a <code>getInstance</code> style factory
 * method). The factory methods aren't necessary because <code>MessageFormat</code>
 * itself doesn't implement locale specific behavior. Any locale specific
 * behavior is defined by the pattern that you provide as well as the
 * subformats used for inserted arguments.
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
 *         { <i>ArgumentIndex</i> }
 *         { <i>ArgumentIndex</i> , <i>FormatType</i> }
 *         { <i>ArgumentIndex</i> , <i>FormatType</i> , <i>FormatStyle</i> }
 *
 * <i>FormatType: one of </i>
 *         number date time choice
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
 * @author       Mark Davis
 * @draft ICU 3.0
 * @deprecated This is a draft API and might change in a future release of ICU.
 */
public class MessageFormat extends UFormat {

    // TODO set up serialization
    // private static final long serialVersionUID = 6479157306784022952L;

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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Locale getLocale() {
        return ulocale.toLocale();
    }

    /**
     * Gets the locale that's used when creating or comparing subformats.
     *
     * @return the locale used when creating or comparing subformats
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * 
     * @param pattern the pattern for this message format
     * @exception IllegalArgumentException if the pattern is invalid
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public void applyPattern(String pattern) {
            StringBuffer[] segments = new StringBuffer[4];
            for (int i = 0; i < segments.length; ++i) {
                segments[i] = new StringBuffer();
            }
            int part = 0;
            int formatNumber = 0;
            boolean inQuote = false;
            int braceStack = 0;
            maxOffset = -1;
            for (int i = 0; i < pattern.length(); ++i) {
                char ch = pattern.charAt(i);
                if (part == 0) {
                    if (ch == '\'') {
                        if (i + 1 < pattern.length()
                            && pattern.charAt(i+1) == '\'') {
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
                } else  if (inQuote) {              // just copy quotes in parts
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String toPattern() {
        // later, make this more extensible
        int lastOffset = 0;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i <= maxOffset; ++i) {
            copyAndFixQuotes(pattern, lastOffset, offsets[i],result);
            lastOffset = offsets[i];
            result.append('{');
            result.append(argumentNumbers[i]);
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
                result.append(",choice," + ((ChoiceFormat)formats[i]).toPattern());
            } else {
                //result.append(", unknown");
            }
            result.append('}');
        }
        copyAndFixQuotes(pattern, lastOffset, pattern.length(), result);
        return result.toString();
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
     * @param newFormats the new formats to use
     * @exception NullPointerException if <code>newFormats</code> is null
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public void setFormatsByArgumentIndex(Format[] newFormats) {
        for (int i = 0; i <= maxOffset; i++) {
            int j = argumentNumbers[i];
            if (j < newFormats.length) {
                formats[i] = newFormats[j];
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @param argumentIndex the argument index for which to use the new format
     * @param newFormat the new format to use
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        for (int j = 0; j <= maxOffset; j++) {
            if (argumentNumbers[j] == argumentIndex) {
                formats[j] = newFormat;
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @return the formats used for the arguments within the pattern
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Format[] getFormatsByArgumentIndex() {
        int maximumArgumentNumber = -1;
        for (int i = 0; i <= maxOffset; i++) {
            if (argumentNumbers[i] > maximumArgumentNumber) {
                maximumArgumentNumber = argumentNumbers[i];
            }
        }
        Format[] resultArray = new Format[maximumArgumentNumber + 1];
        for (int i = 0; i <= maxOffset; i++) {
            resultArray[argumentNumbers[i]] = formats[i];
        }
        return resultArray;
    }

    /**
     * Gets the formats used for the format elements in the
     * previously set pattern string.
     * The order of formats in the returned array corresponds to
     * the order of format elements in the pattern string.
     * <p>
     * Since the order of format elements in a pattern string often
     * changes during localization, it's generally better to use the
     * {@link #getFormatsByArgumentIndex getFormatsByArgumentIndex}
     * method, which assumes an order of formats corresponding to the
     * order of elements in the <code>arguments</code> array passed to
     * the <code>format</code> methods or the result array returned by
     * the <code>parse</code> methods.
     *
     * @return the formats used for the format elements in the pattern
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Format[] getFormats() {
        Format[] resultArray = new Format[maxOffset + 1];
        System.arraycopy(formats, 0, resultArray, 0, maxOffset + 1);
        return resultArray;
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
     * <code>null</code> or has fewer than argumentIndex+1 elements.
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
     *       <td><code>DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale()).format(argument)</code>
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
     * @param arguments an array of objects to be formatted and substituted.
     * @param result where text is appended.
     * @param pos On input: an alignment field, if desired.
     *            On output: the offsets of the alignment field.
     * @exception IllegalArgumentException if an argument in the
     *            <code>arguments</code> array is not of the type
     *            expected by the format element(s) that use it.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final StringBuffer format(Object[] arguments, StringBuffer result,
                                     FieldPosition pos)
    {
        return subformat(arguments, result, pos);
    }

    /**
     * Creates a MessageFormat with the given pattern and uses it
     * to format the given arguments. This is equivalent to
     * <blockquote>
     *     <code>(new {@link #MessageFormat(String) MessageFormat}(pattern)).{@link #format(java.lang.Object[], java.lang.StringBuffer, java.text.FieldPosition) format}(arguments, new StringBuffer(), null).toString()</code>
     * </blockquote>
     *
     * @exception IllegalArgumentException if the pattern is invalid,
     *            or if an argument in the <code>arguments</code> array
     *            is not of the type expected by the format element(s)
     *            that use it.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String format(String pattern, Object[] arguments) {
        MessageFormat temp = new MessageFormat(pattern);
        return temp.format(arguments);
    }

    // Overrides
    /**
     * Formats an array of objects and appends the <code>MessageFormat</code>'s
     * pattern, with format elements replaced by the formatted objects, to the
     * provided <code>StringBuffer</code>.
     * This is equivalent to
     * <blockquote>
     *     <code>{@link #format(java.lang.Object[], java.lang.StringBuffer, java.text.FieldPosition) format}((Object[]) arguments, result, pos)</code>
     * </blockquote>
     *
     * @param arguments an array of objects to be formatted and substituted.
     * @param result where text is appended.
     * @param pos On input: an alignment field, if desired.
     *            On output: the offsets of the alignment field.
     * @exception IllegalArgumentException if an argument in the
     *            <code>arguments</code> array is not of the type
     *            expected by the format element(s) that use it.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final StringBuffer format(Object arguments, StringBuffer result,
                                     FieldPosition pos)
    {
        return subformat((Object[]) arguments, result, pos);
    }

// TODO Do not remove, this is API in JDK that we need to implement
//    /**
//     * Formats an array of objects and inserts them into the
//     * <code>MessageFormat</code>'s pattern, producing an
//     * <code>AttributedCharacterIterator</code>.
//     * You can use the returned <code>AttributedCharacterIterator</code>
//     * to build the resulting String, as well as to determine information
//     * about the resulting String.
//     * <p>
//     * The text of the returned <code>AttributedCharacterIterator</code> is
//     * the same that would be returned by
//     * <blockquote>
//     *     <code>{@link #format(java.lang.Object[], java.lang.StringBuffer, java.text.FieldPosition) format}(arguments, new StringBuffer(), null).toString()</code>
//     * </blockquote>
//     * <p>
//     * In addition, the <code>AttributedCharacterIterator</code> contains at
//     * least attributes indicating where text was generated from an
//     * argument in the <code>arguments</code> array. The keys of these attributes are of
//     * type <code>MessageFormat.Field</code>, their values are
//     * <code>Integer</code> objects indicating the index in the <code>arguments</code>
//     * array of the argument from which the text was generated.
//     * <p>
//     * The attributes/value from the underlying <code>Format</code>
//     * instances that <code>MessageFormat</code> uses will also be
//     * placed in the resulting <code>AttributedCharacterIterator</code>.
//     * This allows you to not only find where an argument is placed in the
//     * resulting String, but also which fields it contains in turn.
//     *
//     * @param arguments an array of objects to be formatted and substituted.
//     * @return AttributedCharacterIterator describing the formatted value.
//     * @exception NullPointerException if <code>arguments</code> is null.
//     * @exception IllegalArgumentException if an argument in the
//     *            <code>arguments</code> array is not of the type
//     *            expected by the format element(s) that use it.
//     */
//    public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
//        StringBuffer result = new StringBuffer();
//        ArrayList iterators = new ArrayList();
//
//        if (arguments == null) {
//            throw new NullPointerException(
//                   "formatToCharacterIterator must be passed non-null object");
//        }
//        subformat((Object[]) arguments, result, null, iterators);
//        if (iterators.size() == 0) {
//            return createAttributedCharacterIterator("");
//        }
//        return createAttributedCharacterIterator(
//                     (AttributedCharacterIterator[])iterators.toArray(
//                     new AttributedCharacterIterator[iterators.size()]));
//    }

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
     * where in the string did the parsing failed.  The returned error
     * index is the starting offset of the sub-patterns that the string
     * is comparing with.  For example, if the parsing string "AAA {0} BBB"
     * is comparing against the pattern "AAD {0} BBB", the error index is
     * 0. When an error occurs, the call to this method will return null.
     * If the source is null, return an empty array.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Object[] parse(String source, ParsePosition pos) {
        if (source == null) {
            Object[] empty = {};
            return empty;
        }

        int maximumArgumentNumber = -1;
        for (int i = 0; i <= maxOffset; i++) {
            if (argumentNumbers[i] > maximumArgumentNumber) {
                maximumArgumentNumber = argumentNumbers[i];
            }
        }
        Object[] resultArray = new Object[maximumArgumentNumber + 1];

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
                    String strValue= source.substring(sourceOffset,next);
                    if (!strValue.equals("{"+argumentNumbers[i]+"}"))
                        resultArray[argumentNumbers[i]]
                            = source.substring(sourceOffset,next);
                    sourceOffset = next;
                }
            } else {
                tempStatus.setIndex(sourceOffset);
                resultArray[argumentNumbers[i]]
                    = formats[i].parseObject(source,tempStatus);
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
        return resultArray;
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
     * @exception ParseException if the beginning of the specified string
     *            cannot be parsed.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Object[] parse(String source) throws ParseException {
        ParsePosition pos  = new ParsePosition(0);
        Object[] result = parse(source, pos);
        if (pos.getIndex() == 0)  // unchanged, returned object is null
            throw new ParseException("MessageFormat parse error!", pos.getErrorIndex());

        return result;
    }

    /**
     * Parses text from a string to produce an object array.
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
     * @return An <code>Object</code> array parsed from the string. In case of
     *         error, returns null.
     * @exception NullPointerException if <code>pos</code> is null.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a clone of this instance.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Object clone() {
        MessageFormat other = (MessageFormat) super.clone();

        // clone arrays. Can't do with utility because of bug in Cloneable
        other.formats = (Format[]) formats.clone(); // shallow clone
        for (int i = 0; i < formats.length; ++i) {
            if (formats[i] != null)
                other.formats[i] = (Format)formats[i].clone();
        }
        // for primitives or immutables, shallow clone is enough
        other.offsets = (int[]) offsets.clone();
        other.argumentNumbers = (int[]) argumentNumbers.clone();

        return other;
    }

    /**
     * Equality comparison between two message format objects
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public boolean equals(Object obj) {
        if (this == obj)                      // quick check
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MessageFormat other = (MessageFormat) obj;
        return (maxOffset == other.maxOffset
                && pattern.equals(other.pattern)
                && Utility.objectEquals(ulocale, other.ulocale)   // does null check
                && Utility.arrayEquals(offsets,other.offsets)
                && Utility.arrayEquals(argumentNumbers,other.argumentNumbers)
                && Utility.arrayEquals(formats,other.formats));
    }

    /**
     * Generates a hash code for the message format object.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public int hashCode() {
        return pattern.hashCode(); // enough for reasonable distribution
    }

// TODO Do not remove, this is API in JDK that we need to implement
//    /**
//     * Defines constants that are used as attribute keys in the
//     * <code>AttributedCharacterIterator</code> returned
//     * from <code>MessageFormat.formatToCharacterIterator</code>.
//     * @draft ICU 3.0
//     * @deprecated This is a draft API and might change in a future release of ICU.
//     */
//    public static class Field extends Format.Field {
//        /**
//         * Creates a Field with the specified name.
//         *
//         * @param name Name of the attribute
//         */
//        protected Field(String name) {
//            super(name);
//        }
//
//        /**
//         * Resolves instances being deserialized to the predefined constants.
//         *
//         * @throws InvalidObjectException if the constant could not be
//         *         resolved.
//         * @return resolved MessageFormat.Field constant
//         */
//        protected Object readResolve() throws InvalidObjectException {
//            if (this.getClass() != MessageFormat.Field.class) {
//                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
//            }
//
//            return ARGUMENT;
//        }
//
//        //
//        // The constants
//        //
//
//        /**
//         * Constant identifying a portion of a message that was generated
//         * from an argument passed into <code>formatToCharacterIterator</code>.
//         * The value associated with the key will be an <code>Integer</code>
//         * indicating the index in the <code>arguments</code> array of the
//         * argument from which the text was generated.
//         */
//        public final static Field ARGUMENT =
//                           new Field("message argument field");
//    }

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
     * The positions where the results of formatting each argument are to be inserted
     * into the pattern.
     * @serial
     */
    private int[] offsets = new int[INITIAL_FORMATS];

    /**
     * The argument numbers corresponding to each formatter.  (The formatters are stored
     * in the order they occur in the pattern, not in the order in which the arguments
     * are specified.)
     * @serial
     */
    private int[] argumentNumbers = new int[INITIAL_FORMATS];

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
                                   FieldPosition fp
                                   /*, List characterIterators*/) {
        // note: this implementation assumes a fast substring & index.
        // if this is not true, would be better to append chars one by one.
        int lastOffset = 0;
        int last = result.length();
        for (int i = 0; i <= maxOffset; ++i) {
            result.append(pattern.substring(lastOffset, offsets[i]));
            lastOffset = offsets[i];
            int argumentNumber = argumentNumbers[i];
            if (arguments == null || argumentNumber >= arguments.length) {
                result.append("{" + argumentNumber + "}");
                continue;
            }
            // int argRecursion = ((recursionProtection >> (argumentNumber*2)) & 0x3);
            if (false) { // if (argRecursion == 3){
                // prevent loop!!!
                result.append('\uFFFD');
            } else {
                Object obj = arguments[argumentNumber];
                String arg = null;
                Format subFormatter = null;
                if (obj == null) {
                    arg = "null";
                } else if (formats[i] != null) {
                    subFormatter = formats[i];
                    if (subFormatter instanceof ChoiceFormat) {
                        arg = formats[i].format(obj);
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

// TODO Do not remove, this is API in JDK that we need to implement
//                if (characterIterators != null) {
//                    // If characterIterators is non-null, it indicates we need
//                    // to get the CharacterIterator from the child formatter.
//                    if (last != result.length()) {
//                        characterIterators.add(
//                            createAttributedCharacterIterator(result.substring
//                                                              (last)));
//                        last = result.length();
//                    }
//                    if (subFormatter != null) {
//                        AttributedCharacterIterator subIterator =
//                                   subFormatter.formatToCharacterIterator(obj);
//
//                        append(result, subIterator);
//                        if (last != result.length()) {
//                            characterIterators.add(
//                                         createAttributedCharacterIterator(
//                                         subIterator, Field.ARGUMENT,
//                                         new Integer(argumentNumber)));
//                            last = result.length();
//                        }
//                        arg = null;
//                    }
//                    if (arg != null && arg.length() > 0) {
//                        result.append(arg);
//                        characterIterators.add(
//                                 createAttributedCharacterIterator(
//                                 arg, Field.ARGUMENT,
//                                 new Integer(argumentNumber)));
//                        last = result.length();
//                    }
//                }
//                else
                {
                    if (subFormatter != null) {
                        arg = subFormatter.format(obj);
                    }
//                    last = result.length(); // Useless? [alan]
                    result.append(arg);
// TODO Do not remove, this is JDK API we need to implement.
//                    if (i == 0 && fp != null && Field.ARGUMENT.equals(
//                                  fp.getFieldAttribute())) {
//                        fp.setBeginIndex(last);
//                        fp.setEndIndex(result.length());
//                    }
                    last = result.length();
                }
            }
        }
        result.append(pattern.substring(lastOffset, pattern.length()));
// TODO Do not remove, this is JDK API we need to implement.
//        if (characterIterators != null && last != result.length()) {
//            characterIterators.add(createAttributedCharacterIterator(
//                                   result.substring(last)));
//        }
        return result;
    }

// TODO Do not remove, this is JDK API we need to implement.
//    /**
//     * Convenience method to append all the characters in
//     * <code>iterator</code> to the StringBuffer <code>result</code>.
//     */
//    private void append(StringBuffer result, CharacterIterator iterator) {
//        if (iterator.first() != CharacterIterator.DONE) {
//            char aChar;
//
//            result.append(iterator.first());
//            while ((aChar = iterator.next()) != CharacterIterator.DONE) {
//                result.append(aChar);
//            }
//        }
//    }

    private static final String[] typeList =
        {"", "number", "date", "time", "choice", "spellout", "ordinal", "duration"};
    private static final int 
        TYPE_EMPTY = 0,
        TYPE_NUMBER = 1,
        TYPE_DATE = 2,
        TYPE_TIME = 3,
        TYPE_CHOICE = 4,
        TYPE_SPELLOUT = 5,
        TYPE_ORDINAL = 6,
        TYPE_DURATION = 7;

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
        int argumentNumber;
        try {
            argumentNumber = Integer.parseInt(segments[1].toString()); // always unlocalized!
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("can't parse argument number " + segments[1]);
        }
        if (argumentNumber < 0) {
            throw new IllegalArgumentException("negative argument number " + argumentNumber);
        }

        // resize format information arrays if necessary
        if (offsetNumber >= formats.length) {
            int newLength = formats.length * 2;
            Format[] newFormats = new Format[newLength];
            int[] newOffsets = new int[newLength];
            int[] newArgumentNumbers = new int[newLength];
            System.arraycopy(formats, 0, newFormats, 0, maxOffset + 1);
            System.arraycopy(offsets, 0, newOffsets, 0, maxOffset + 1);
            System.arraycopy(argumentNumbers, 0, newArgumentNumbers, 0, maxOffset + 1);
            formats = newFormats;
            offsets = newOffsets;
            argumentNumbers = newArgumentNumbers;
        }
        int oldMaxOffset = maxOffset;
        maxOffset = offsetNumber;
        offsets[offsetNumber] = segments[0].length();
        argumentNumbers[offsetNumber] = argumentNumber;

        // now get the format
        Format newFormat = null;
        switch (findKeyword(segments[2].toString(), typeList)) {
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
                newFormat = new DecimalFormat(segments[3].toString(), new DecimalFormatSymbols(ulocale));
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
                throw new IllegalArgumentException(
                                         "Choice Pattern incorrect");
            }
            break;
        case TYPE_SPELLOUT: 
            {
                RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ulocale, RuleBasedNumberFormat.SPELLOUT);
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
                RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ulocale, RuleBasedNumberFormat.ORDINAL);
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
                RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ulocale, RuleBasedNumberFormat.DURATION);
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

    private static final void copyAndFixQuotes(String source, int start, int end, StringBuffer target) {
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
        boolean isValid = maxOffset >= -1
                && formats.length > maxOffset
                && offsets.length > maxOffset
                && argumentNumbers.length > maxOffset;
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
            throw new InvalidObjectException("Could not reconstruct MessageFormat from corrupt stream.");
        }
        if (ulocale == null) {
            ulocale = ULocale.forLocale(locale);
        }
    }
}
