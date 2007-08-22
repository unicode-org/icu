/*
 *******************************************************************************
 * Copyright (C) 2004-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

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
 * @stable ICU 3.0
 */
public class MessageFormat extends Format {
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
        this(new java.text.MessageFormat(pattern));
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
        // locale is ignored
        this(new java.text.MessageFormat(pattern));
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
        // locale is ignored
        this(pattern);
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
     * Gets the locale that's used when creating or comparing subformats.
     *
     * @return the locale used when creating or comparing subformats
     * @stable ICU 3.0
     */
    public Locale getLocale() {
        return messageFormat.getLocale();
    }

    /**
     * Gets the locale that's used when creating or comparing subformats.
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
     * 
     * @param pattern the pattern for this message format
     * @exception IllegalArgumentException if the pattern is invalid
     * @stable ICU 3.0
     */
   public void applyPattern(String pattern) {
       messageFormat.applyPattern(pattern);
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
       return messageFormat.toPattern();
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
    * @stable ICU 3.0
    * @throws UnsupportedOperationException if the underlying JVM does not
    * support this method.
    */
   public void setFormatsByArgumentIndex(Format[] newFormats) {
	   if (sfsbai == null) {
		   synchronized (missing) {
			   try {
				   Class[] params = { Format[].class };
				   sfsbai = java.text.MessageFormat.class.getMethod("setFormatsByArgumentIndex", params);
			   }
			   catch (NoSuchMethodException e) {
				   sfsbai = missing;
			   }
		   }
	   }
	   if (sfsbai != missing) {
		   try {
			   Format[] unwrapped = new Format[newFormats.length];
			   for (int i = 0; i < newFormats.length; ++i) {
				   unwrapped[i] = unwrap(newFormats[i]);
			   }
			   Object[] args = { unwrapped };
			   ((Method)sfsbai).invoke(messageFormat, args);
			   return;
		   }
		   catch (IllegalAccessException e) {
			   // can't happen
		   }
		   catch (InvocationTargetException e) {
			   // can't happen
		   }
	   }
	   throw new UnsupportedOperationException();
    }
   private static Object sfsbai;
   
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
    * @stable ICU 3.0
    * @throws UnsupportedOperationException if the underlying JVM does not
    * support this method.
    */
   public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
	   if (sfbai == null) {
		   synchronized (missing) {
			   try {
				   Class[] params = { Integer.TYPE, Format.class };
				   sfbai = java.text.MessageFormat.class.getMethod("setFormatByArgumentIndex", params);
			   }
			   catch (NoSuchMethodException e) {
				   sfbai = missing;
			   }
		   }
	   }
	   if (sfbai != missing) {
		   try {
			   Object[] args = { new Integer(argumentIndex), newFormat };
			   ((Method)sfbai).invoke(messageFormat, args);
			   return;
		   }
		   catch (IllegalAccessException e) {
			   // can't happen
		   }
		   catch (InvocationTargetException e) {
			   // can't happen
		   }
	   }
	   throw new UnsupportedOperationException();
   }
   private static Object sfbai;

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
       messageFormat.setFormat(formatElementIndex, unwrap(newFormat));
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
    * @stable ICU 3.0
    * @throws UnsupportedOperationException if the underlying JVM does not 
    * support this method.
    */
   public Format[] getFormatsByArgumentIndex() {
	   if (gfbai == null) {
		   synchronized (missing) {
			   try {
				   gfbai = java.text.MessageFormat.class.getMethod("getFormatsByArgumentIndex", null);
			   }
			   catch (NoSuchMethodException e) {
				   gfbai = missing;
			   }
		   }
	   }
	   if (gfbai != missing) {
		   try {
			   Format[] result = (Format[])((Method)gfbai).invoke(messageFormat, null);
			   for (int i = 0; i < result.length; ++i) {
				   result[i] = wrap(result[i]);
			   }
			   return result;
		   }
		   catch (IllegalAccessException e) {
			   // can't happen
		   }
		   catch (InvocationTargetException e) {
			   // can't happen
		   }
	   }
	   throw new UnsupportedOperationException();
   }
   private static Object gfbai;
   private static final Object missing = new Object();

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
    * @stable ICU 3.0
    */
   public Format[] getFormats() {
       Format[] result = messageFormat.getFormats();
       for (int i = 0; i < result.length; ++i) {
           result[i] = wrap(result[i]);
       }
       return result;
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
    * @stable ICU 3.0
    */
   public final StringBuffer format(Object[] arguments, StringBuffer result,
       FieldPosition pos)
   {
       return messageFormat.format(arguments, result, pos);
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
    * @stable ICU 3.0
    */
   public static String format(String pattern, Object[] arguments) {
       return java.text.MessageFormat.format(pattern, arguments);
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
    * @stable ICU 3.0
    */
   public final StringBuffer format(Object arguments, StringBuffer result,
       FieldPosition pos)
   {
       return messageFormat.format(arguments, result, pos);
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
    * where in the string did the parsing failed.  The returned error
    * index is the starting offset of the sub-patterns that the string
    * is comparing with.  For example, if the parsing string "AAA {0} BBB"
    * is comparing against the pattern "AAD {0} BBB", the error index is
    * 0. When an error occurs, the call to this method will return null.
    * If the source is null, return an empty array.
    * @stable ICU 3.0
    */
   public Object[] parse(String source, ParsePosition pos) {
       return messageFormat.parse(source, pos);
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
    * @stable ICU 3.0
    */
   public Object[] parse(String source) throws ParseException {
       return messageFormat.parse(source);
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
    * @stable ICU 3.0
    */
   public Object parseObject(String source, ParsePosition pos) {
       return messageFormat.parse(source, pos);
   }

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
    * @draft ICU 3.4
    * @provisional
    */
   public static String autoQuoteApostrophe(String pattern) {
       StringBuffer buf = new StringBuffer(pattern.length()*2);
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
           default: // Never happens.
               break;
           }
           buf.append(c);
       }
       // End of scan
       if (state == STATE_SINGLE_QUOTE || state == STATE_IN_QUOTE) {
           buf.append(SINGLE_QUOTE);
       }
       return new String(buf);
   }
   
   /**
    * Creates and returns a copy of this object.
    *
    * @return a clone of this instance.
    * @stable ICU 3.0
    */
   public Object clone() {
       return new MessageFormat((java.text.MessageFormat)messageFormat.clone());
   }

   /**
    * Equality comparison between two message format objects
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
    * Generates a hash code for the message format object.
    * @stable ICU 3.0
    */
   public int hashCode() {
       return messageFormat.hashCode();
   }

   /**
    * Return a string suitable for debugging.
    * @return a string suitable for debugging
    * @draft ICU 3.4.2
    */
   public String toString() {
       return messageFormat.toPattern();
   }

   private static Format unwrap(Format f) {
       if (f instanceof DateFormat) {
           return ((DateFormat)f).dateFormat;
       } else if (f instanceof NumberFormat) {
           return ((NumberFormat)f).numberFormat;
       } else if (f instanceof MessageFormat) {
           return ((MessageFormat)f).messageFormat;
       } else {
           return f;
       }
   }

   private static Format wrap(Format f) {
       if (f instanceof java.text.DateFormat) {
           return new DateFormat((java.text.DateFormat)f);
       } else if (f instanceof java.text.DecimalFormat) {
           return new DecimalFormat((java.text.DecimalFormat)f);
       } else if (f instanceof java.text.MessageFormat) {
           return new MessageFormat((java.text.MessageFormat)f);
       } else {
           return f;
       }
   }

   private static final char SINGLE_QUOTE = '\'';
   private static final char CURLY_BRACE_LEFT = '{';
   private static final char CURLY_BRACE_RIGHT = '}';

   private static final int STATE_INITIAL = 0;
   private static final int STATE_SINGLE_QUOTE = 1;
   private static final int STATE_IN_QUOTE = 2;
   private static final int STATE_MSG_ELEMENT = 3;
}
