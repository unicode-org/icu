package com.ibm.text;

import java.util.*;
import java.text.MessageFormat;

/**
 * <code>Transliterator</code> is an abstract class that
 * transliterates text from one format to another.  The most common
 * kind of transliterator is a script, or alphabet, transliterator.
 * For example, a Russian to Latin transliterator changes Russian text
 * written in Cyrillic characters to phonetically equivalent Latin
 * characters.  It does not <em>translate</em> Russian to English!
 * Transliteration, unlike translation, operates on characters, without
 * reference to the meanings of words and sentences.
 *
 * <p>Although script conversion is its most common use, a
 * transliterator can actually perform a more general class of tasks.
 * In fact, <code>Transliterator</code> defines a very general API
 * which specifies only that a segment of the input text is replaced
 * by new text.  The particulars of this conversion are determined
 * entirely by subclasses of <code>Transliterator</code>.
 *
 * <p><b>Transliterators are stateless</b>
 *
 * <p><code>Transliterator</code> objects are <em>stateless</em>; they
 * retain no information between calls to
 * <code>transliterate()</code>.  As a result, threads may share
 * transliterators without synchronizing them.  This might seem to
 * limit the complexity of the transliteration operation.  In
 * practice, subclasses perform complex transliterations by delaying
 * the replacement of text until it is known that no other
 * replacements are possible.  In other words, although the
 * <code>Transliterator</code> objects are stateless, the source text
 * itself embodies all the needed information, and delayed operation
 * allows arbitrary complexity.
 *
 * <p><b>Batch transliteration</b>
 *
 * <p>The simplest way to perform transliteration is all at once, on a
 * string of existing text.  This is referred to as <em>batch</em>
 * transliteration.  For example, given a string <code>input</code>
 * and a transliterator <code>t</code>, the call
 *
 * <blockquote><code>String result = t.transliterate(input);
 * </code></blockquote>
 *
 * will transliterate it and return the result.  Other methods allow
 * the client to specify a substring to be transliterated and to use
 * {@link Replaceable} objects instead of strings, in order to
 * preserve out-of-band information (such as text styles).
 *
 * <p><b>Keyboard transliteration</b>
 *
 * <p>Somewhat more involved is <em>keyboard</em>, or incremental
 * transliteration.  This is the transliteration of text that is
 * arriving from some source (typically the user's keyboard) one
 * character at a time, or in some other piecemeal fashion.
 *
 * <p>In keyboard transliteration, a <code>Replaceable</code> buffer
 * stores the text.  As text is inserted, as much as possible is
 * transliterated on the fly.  This means a GUI that displays the
 * contents of the buffer may show text being modified as each new
 * character arrives.
 *
 * <p>Consider the simple <code>RuleBasedTransliterator</code>:
 *
 * <blockquote><code>
 * th&gt;{theta}<br>
 * t&gt;{tau}
 * </code></blockquote>
 *
 * When the user types 't', nothing will happen, since the
 * transliterator is waiting to see if the next character is 'h'.  To
 * remedy this, we introduce the notion of a cursor, marked by a '|'
 * in the output string:
 *
 * <blockquote><code>
 * t&gt;|{tau}<br>
 * {tau}h&gt;{theta}
 * </code></blockquote>
 *
 * Now when the user types 't', tau appears, and if the next character
 * is 'h', the tau changes to a theta.  This is accomplished by
 * maintaining a cursor position (independent of the insertion point,
 * and invisible in the GUI) across calls to
 * <code>keyboardTransliterate()</code>.  Typically, the cursor will
 * be coincident with the insertion point, but in a case like the one
 * above, it will precede the insertion point.
 *
 * <p>Keyboard transliteration methods maintain a set of three indices
 * that are updated with each call to
 * <code>keyboardTransliterate()</code>, including the cursor, start,
 * and limit.  Since these indices are changed by the method, they are
 * passed in an <code>int[]</code> array. The <code>START</code> index
 * marks the beginning of the substring that the transliterator will
 * look at.  It is advanced as text becomes committed (but it is not
 * the committed index; that's the <code>CURSOR</code>).  The
 * <code>CURSOR</code> index, described above, marks the point at
 * which the transliterator last stopped, either because it reached
 * the end, or because it required more characters to disambiguate
 * between possible inputs.  The <code>CURSOR</code> can also be
 * explicitly set by rules in a <code>RuleBasedTransliterator</code>.
 * Any characters before the <code>CURSOR</code> index are frozen;
 * future keyboard transliteration calls within this input sequence
 * will not change them.  New text is inserted at the
 * <code>LIMIT</code> index, which marks the end of the substring that
 * the transliterator looks at.
 *
 * <p>Because keyboard transliteration assumes that more characters
 * are to arrive, it is conservative in its operation.  It only
 * transliterates when it can do so unambiguously.  Otherwise it waits
 * for more characters to arrive.  When the client code knows that no
 * more characters are forthcoming, perhaps because the user has
 * performed some input termination operation, then it should call
 * <code>finishKeyboardTransliteration()</code> to complete any
 * pending transliterations.
 *
 * <p><b>Inverses</b>
 *
 * <p>Pairs of transliterators may be inverses of one another.  For
 * example, if transliterator <b>A</b> transliterates characters by
 * incrementing their Unicode value (so "abc" -> "def"), and
 * transliterator <b>B</b> decrements character values, then <b>A</b>
 * is an inverse of <b>B</b> and vice versa.  If we compose <b>A</b>
 * with <b>B</b> in a compound transliterator, the result is the
 * indentity transliterator, that is, a transliterator that does not
 * change its input text.
 *
 * The <code>Transliterator</code> method <code>getInverse()</code>
 * returns a transliterator's inverse, if one exists, or
 * <code>null</code> otherwise.  However, the result of
 * <code>getInverse()</code> usually will <em>not</em> be a true
 * mathematical inverse.  This is because true inverse transliterators
 * are difficult to formulate.  For example, consider two
 * transliterators: <b>AB</b>, which transliterates the character 'A'
 * to 'B', and <b>BA</b>, which transliterates 'B' to 'A'.  It might
 * seem that these are exact inverses, since
 *
 * <blockquote>"A" x <b>AB</b> -> "B"<br>
 * "B" x <b>BA</b> -> "A"</blockquote>
 *
 * where 'x' represents transliteration.  However,
 *
 * <blockquote>"ABCD" x <b>AB</b> -> "BBCD"<br>
 * "BBCD" x <b>BA</b> -> "AACD"</blockquote>
 *
 * so <b>AB</b> composed with <b>BA</b> is not the
 * identity. Nonetheless, <b>BA</b> may be usefully considered to be
 * <b>AB</b>'s inverse, and it is on this basis that
 * <b>AB</b><code>.getInverse()</code> could legitimately return
 * <b>BA</b>.
 *
 * <p><b>IDs and display names</b>
 *
 * <p>A transliterator is designated by a short identifier string or
 * <em>ID</em>.  IDs follow the format <em>source-destination</em>,
 * where <em>source</em> describes the entity being replaced, and
 * <em>destination</em> describes the entity replacing
 * <em>source</em>.  The entities may be the names of scripts,
 * particular sequences of characters, or whatever else it is that the
 * transliterator converts to or from.  For example, a transliterator
 * from Russian to Latin might be named "Russian-Latin".  A
 * transliterator from keyboard escape sequences to Latin-1 characters
 * might be named "KeyboardEscape-Latin1".  By convention, system
 * entity names are in English, with the initial letters of words
 * capitalized; user entity names may follow any format so long as
 * they do not contain dashes.
 *
 * <p>In addition to programmatic IDs, transliterator objects have
 * display names for presentation in user interfaces, returned by
 * {@link #getDisplayName}.
 *
 * <p><b>Factory methods and registration</b>
 *
 * <p>In general, client code should use the factory method
 * <code>getInstance()</code> to obtain an instance of a
 * transliterator given its ID.  Valid IDs may be enumerated using
 * <code>getAvailableIDs()</code>.  Since transliterators are
 * stateless, multiple calls to <code>getInstance()</code> with the
 * same ID will return the same object.
 *
 * <p>In addition to the system transliterators registered at startup,
 * user transliterators may be registered by calling
 * <code>registerInstance()</code> at run time.  To register a
 * transliterator subclass without instantiating it (until it is
 * needed), users may call <code>registerClass()</code>.
 *
 * <p><b>Subclassing</b>
 *
 * <p>Subclasses must implement the abstract
 * <code>transliterate()</code> method.  They should also override the
 * <code>transliterate()</code> method taking a <code>String</code>
 * and <code>StringBuffer</code> if the performance of these methods
 * can be improved over the performance obtained by the default
 * implementations in this class.  Subclasses must also implement
 * <code>handleKeyboardTransliterate()</code>.
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: Transliterator.java,v $ $Revision: 1.5 $ $Date: 2000/01/06 01:36:36 $
 */
public abstract class Transliterator {
    /**
     * In the <code>keyboardTransliterate()</code>
     * <code>index[]</code> array, the beginning index, inclusive
     * @see #keyboardTransliterate
     */
    public static final int START  = 0;

    /**
     * In the <code>keyboardTransliterate()</code>
     * <code>index[]</code> array, the ending index, exclusive
     * @see #keyboardTransliterate
     */
    public static final int LIMIT  = 1;

    /**
     * In the <code>keyboardTransliterate()</code>
     * <code>index[]</code> array, the next character to be considered
     * for transliteration
     * @see #keyboardTransliterate
     */
    public static final int CURSOR = 2;

    /**
     * Programmatic name, e.g., "Latin-Arabic".
     */
    private String ID;

    /** 
     * This transliterator's filter.  Any character for which
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    private UnicodeFilter filter;

    /**
     * Dictionary of known transliterators.  Keys are <code>String</code>
     * names, values are one of the following:
     *
     * <ul><li><code>Transliterator</code> objects
     *
     * <li><code>Class</code> objects.  Such objects must represent
     * subclasses of <code>Transliterator</code>, and must satisfy the
     * constraints described in <code>registerClass()</code>
     *
     * <li><code>RULE_BASED_PLACEHOLDER</code>, in which case the ID
     * will have its first '-' removed and be appended to
     * RB_RULE_BASED_PREFIX to form a resource bundle name from which
     * the RB_RULE key is looked up to obtain the rule.
     *
     * <li><code>REVERSE_RULE_BASED_PLACEHOLDER</code>.  Like
     * <code>RULE_BASED_PLACEHOLDER</code>, except the entity names in
     * the ID are reversed, and the argument
     * RuleBasedTransliterator.REVERSE is pased to the
     * RuleBasedTransliterator constructor.
     * </ul>
     */
    private static Hashtable cache;

    /**
     * Internal object used to stand for instances of
     * <code>RuleBasedTransliterator</code> that have not been
     * constructed yet in the <code>cache</code>.  When a
     * <code>getInstance()</code> call retrieves this object, it is
     * replaced by the actual <code>RuleBasedTransliterator</code>.
     * This allows <code>Transliterator</code> to delay instantiation
     * of such transliterators until they are needed.
     */
    private static final Object RULE_BASED_PLACEHOLDER = new Object();

    /**
     * Internal object used to stand for instances of
     * <code>RuleBasedTransliterator</code> that have not been
     * constructed yet in the <code>cache</code>.  These instances are
     * constructed with an argument
     * <code>RuleBasedTransliterator.REVERSE</code>.
     */
    private static final Object REVERSE_RULE_BASED_PLACEHOLDER = new Object();

    /**
     * Prefix for resource bundle key for the display name for a
     * transliterator.  The ID is appended to this to form the key.
     * The resource bundle value should be a String.
     */
    private static final String RB_DISPLAY_NAME_PREFIX = "%Translit%%";

    /**
     * Prefix for resource bundle key for the display name for a
     * transliterator SCRIPT.  The ID is appended to this to form the key.
     * The resource bundle value should be a String.
     */
    private static final String RB_SCRIPT_DISPLAY_NAME_PREFIX = "%Translit%";

    /**
     * Resource bundle key for display name pattern.
     * The resource bundle value should be a String forming a
     * MessageFormat pattern, e.g.:
     * "{0,choice,0#|1#{1} Transliterator|2#{1} to {2} Transliterator}".
     */
    private static final String RB_DISPLAY_NAME_PATTERN = "TransliteratorNamePattern";

    /**
     * Resource bundle key for the list of RuleBasedTransliterator IDs.
     * The resource bundle value should be a String[] with each element
     * being a valid ID.  The ID will be appended to RB_RULE_BASED_PREFIX
     * to obtain the class name in which the RB_RULE key will be sought.
     */
    private static final String RB_RULE_BASED_IDS = "RuleBasedTransliteratorIDs";

    /**
     * Resource bundle containing display name keys and the
     * RB_RULE_BASED_IDS array.
     *
     * <p>If we ever integrate this with the Sun JDK, the resource bundle
     * root will change to java.text.resources.LocaleElements
     */
    private static final String RB_LOCALE_ELEMENTS =
        "com.ibm.text.resources.LocaleElements";

    /**
     * Prefix for resource bundle containing RuleBasedTransliterator
     * RB_RULE string.  The ID is munged to remove the first '-' then appended
     * to this String to obtain the class name.
     */
    private static final String RB_RULE_BASED_PREFIX =
        "com.ibm.text.resources.TransliterationRule$";

    /**
     * Resource bundle key for the RuleBasedTransliterator rule.
     */
    private static final String RB_RULE = "Rule";

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Default constructor.
     * @param ID the string identifier for this transliterator
     * @param filter the filter.  Any character for which
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    protected Transliterator(String ID, UnicodeFilter filter) {
        if (ID == null) {
            throw new NullPointerException();
        }
        this.ID = ID;
        this.filter = filter;
    }

    /**
     * Transliterates the segment of a string that begins at the
     * character at offset <code>start</code> and extends to the
     * character at offset <code>limit - 1</code>, with optional
     * filtering.  A default implementaion is provided here;
     * subclasses should provide a more efficient implementation if
     * possible.
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param result buffer to receive the transliterated text; previous
     * contents are discarded
     */
    public void transliterate(String text, int start, int limit,
                              StringBuffer result) {
        /* This is a default implementation that should be replaced by
         * a more efficient subclass implementation if possible.
         */
        result.setLength(0);
        result.append(text.substring(start, limit));
        transliterate(new ReplaceableString(result),
                      0, result.length());
    }

    /**
     * Transliterates a segment of a string, with optional filtering.
     * Subclasses must override this abstract method.
     *
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param filter the filter.  Any character for which
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @return The new limit index.  The text previously occupying <code>[start,
     * limit)</code> has been transliterated, possibly to a string of a different
     * length, at <code>[start, </code><em>new-limit</em><code>)</code>, where
     * <em>new-limit</em> is the return value.
     */
    public abstract int transliterate(Replaceable text, int start, int limit);

    /**
     * Transliterates an entire string. Convenience method.
     * @param text the string to be transliterated
     * @param result buffer to receive the transliterated text; previous
     * contents are discarded
     */
    public final void transliterate(String text, StringBuffer result) {
        transliterate(text, 0, text.length(), result);
    }

    /**
     * Transliterate an entire string and returns the result. Convenience method.
     *
     * @param text the string to be transliterated
     * @return The transliterated text
     */
    public final String transliterate(String text) {
        StringBuffer result = new StringBuffer();
        transliterate(text, 0, text.length(), result);
        return result.toString();
    }

    /**
     * Transliterates an entire string in place. Convenience method.
     * @param text the string to be transliterated
     */
    public final void transliterate(Replaceable text) {
        transliterate(text, 0, text.length());
    }

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly after new text has been inserted,
     * typically as a result of a keyboard event.  The new text in
     * <code>insertion</code> will be inserted into <code>text</code>
     * at <code>index[LIMIT]</code>, advancing
     * <code>index[LIMIT]</code> by <code>insertion.length()</code>.
     * Then the transliterator will try to transliterate characters of
     * <code>text</code> between <code>index[CURSOR]</code> and
     * <code>index[LIMIT]</code>.  Characters before
     * <code>index[CURSOR]</code> will not be changed.
     *
     * <p>Upon return, values in <code>index[]</code> will be updated.
     * <code>index[START]</code> will be advanced to the first
     * character that future calls to this method will read.
     * <code>index[CURSOR]</code> and <code>index[LIMIT]</code> will
     * be adjusted to delimit the range of text that future calls to
     * this method may change.
     *
     * <p>Typical usage of this method begins with an initial call
     * with <code>index[START]</code> and <code>index[LIMIT]</code>
     * set to indicate the portion of <code>text</code> to be
     * transliterated, and <code>index[CURSOR] == index[START]</code>.
     * Thereafter, <code>index[]</code> can be used without
     * modification in future calls, provided that all changes to
     * <code>text</code> are made via this method.
     *
     * <p>This method assumes that future calls may be made that will
     * insert new text into the buffer.  As a result, it only performs
     * unambiguous transliterations.  After the last call to this
     * method, there may be untransliterated text that is waiting for
     * more input to resolve an ambiguity.  In order to perform these
     * pending transliterations, clients should call {@link
     * #finishKeyboardTransliteration} after the last call to this
     * method has been made.
     * 
     * @param text the buffer holding transliterated and untransliterated text
     * @param index an array of three integers.
     *
     * <ul><li><code>index[START]</code>: the beginning index,
     * inclusive; <code>0 <= index[START] <= index[LIMIT]</code>.
     *
     * <li><code>index[LIMIT]</code>: the ending index, exclusive;
     * <code>index[START] <= index[LIMIT] <= text.length()</code>.
     * <code>insertion</code> is inserted at
     * <code>index[LIMIT]</code>.
     *
     * <li><code>index[CURSOR]</code>: the next character to be
     * considered for transliteration; <code>index[START] <=
     * index[CURSOR] <= index[LIMIT]</code>.  Characters before
     * <code>index[CURSOR]</code> will not be changed by future calls
     * to this method.</ul>
     *
     * @param insertion text to be inserted and possibly
     * transliterated into the translation buffer at
     * <code>index[LIMIT]</code>.  If <code>null</code> then no text
     * is inserted.
     * @see #START
     * @see #LIMIT
     * @see #CURSOR
     * @see #handleKeyboardTransliterate
     * @exception IllegalArgumentException if <code>index[]</code>
     * is invalid
     */
    public final void keyboardTransliterate(Replaceable text, int[] index,
                                            String insertion) {
        if (index.length < 3 ||
            index[START] < 0 ||
            index[LIMIT] > text.length() ||
            index[CURSOR] < index[START] ||
            index[CURSOR] > index[LIMIT]) {
            throw new IllegalArgumentException("Invalid index array");
        }

        int originalStart = index[START];
        if (insertion != null) {
            text.replace(index[LIMIT], index[LIMIT], insertion);
            index[LIMIT] += insertion.length();
        }

        handleKeyboardTransliterate(text, index);

        index[START] = Math.max(index[CURSOR] - getMaximumContextLength(),
                                originalStart);
    }

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly after a new character has been
     * inserted, typically as a result of a keyboard event.  This is a
     * convenience method; see {@link
     * #keyboardTransliterate(Replaceable, int[], String)} for details.
     * @param text the buffer holding transliterated and
     * untransliterated text
     * @param index an array of three integers.  See {@link
     * #keyboardTransliterate(Replaceable, int[], String)}.
     * @param insertion text to be inserted and possibly
     * transliterated into the translation buffer at
     * <code>index[LIMIT]</code>.
     * @see #keyboardTransliterate(Replaceable, int[], String)
     */
    public final void keyboardTransliterate(Replaceable text, int[] index,
                                            char insertion) {
        keyboardTransliterate(text, index, String.valueOf(insertion));
    }

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly.  This is a convenience method; see
     * {@link #keyboardTransliterate(Replaceable, int[], String)} for
     * details.
     * @param text the buffer holding transliterated and
     * untransliterated text
     * @param index an array of three integers.  See {@link
     * #keyboardTransliterate(Replaceable, int[], String)}.
     * @see #keyboardTransliterate(Replaceable, int[], String)
     */
    public final void keyboardTransliterate(Replaceable text, int[] index) {
        keyboardTransliterate(text, index, null);
    }

    /**
     * Finishes any pending transliterations that were waiting for
     * more characters.  Clients should call this method as the last
     * call after a sequence of one or more calls to
     * <code>keyboardTransliterate()</code>.
     * @param text the buffer holding transliterated and
     * untransliterated text.
     * @param index the array of indices previously passed to {@link
     * #keyboardTransliterate}
     */
    public final void finishKeyboardTransliteration(Replaceable text,
                                                    int[] index) {
        transliterate(text, index[START], index[LIMIT]);
    }

    /**
     * Abstract method that concrete subclasses define to implement
     * keyboard transliteration.  This method should transliterate all
     * characters between <code>index[CURSOR]</code> and
     * <code>index[LIMIT]</code> that can be unambiguously
     * transliterated, regardless of future insertions of text at
     * <code>index[LIMIT]</code>.  <code>index[CURSOR]</code> should
     * be advanced past committed characters (those that will not
     * change in future calls to this method).
     * <code>index[LIMIT]</code> should be updated to reflect text
     * replacements that shorten or lengthen the text between
     * <code>index[CURSOR]</code> and <code>index[LIMIT]</code>.  Upon
     * return, neither <code>index[CURSOR]</code> nor
     * <code>index[LIMIT]</code> should be less than the initial value
     * of <code>index[CURSOR]</code>.  <code>index[START]</code>
     * should <em>not</em> be changed.
     *
     * @param text the buffer holding transliterated and
     * untransliterated text
     * @param index an array of three integers.  See {@link
     * #keyboardTransliterate(Replaceable, int[], String)}.
     * @see #keyboardTransliterate
     */
    protected abstract void handleKeyboardTransliterate(Replaceable text,
                                                        int[] index);

    /**
     * Returns the length of the longest context required by this transliterator.
     * This is <em>preceding</em> context.  The default implementation supplied
     * by <code>Transliterator</code> returns zero; subclasses
     * that use preceding context should override this method to return the
     * correct value.  For example, if a transliterator translates "ddd" (where
     * d is any digit) to "555" when preceded by "(ddd)", then the preceding
     * context length is 5, the length of "(ddd)".
     *
     * @return The maximum number of preceding context characters this
     * transliterator needs to examine
     */
    protected int getMaximumContextLength() {
        return 0;
    }

    /**
     * Returns a programmatic identifier for this transliterator.
     * If this identifier is passed to <code>getInstance()</code>, it
     * will return this object, if it has been registered.
     * @see #registerInstance
     * @see #registerClass
     * @see #getAvailableIDs
     */
    public final String getID() {
        return ID;
    }

    /**
     * Returns a name for this transliterator that is appropriate for
     * display to the user in the default locale.  See {@link
     * #getDisplayName(Locale)} for details.
     */
    public final static String getDisplayName(String ID) {
        return getDisplayName(ID, Locale.getDefault());
    }

    /**
     * Returns a name for this transliterator that is appropriate for
     * display to the user in the given locale.  This name is taken
     * from the locale resource data in the standard manner of the
     * <code>java.text</code> package.
     *
     * <p>If no localized names exist in the system resource bundles,
     * a name is synthesized using a localized
     * <code>MessageFormat</code> pattern from the resource data.  The
     * arguments to this pattern are an integer followed by one or two
     * strings.  The integer is the number of strings, either 1 or 2.
     * The strings are formed by splitting the ID for this
     * transliterator at the first '-'.  If there is no '-', then the
     * entire ID forms the only string.
     * @param inLocale the Locale in which the display name should be
     * localized.
     * @see java.text.MessageFormat
     */
    public static String getDisplayName(String ID, Locale inLocale) {
        ResourceBundle bundle = ResourceBundle.getBundle(
            RB_LOCALE_ELEMENTS, inLocale);

        // Use display name for the entire transliterator, if it
        // exists.
        try {
            return bundle.getString(RB_DISPLAY_NAME_PREFIX + ID);
        } catch (MissingResourceException e) {}

        try {
            // Construct the formatter first; if getString() fails
            // we'll exit the try block
            MessageFormat format = new MessageFormat(
                    bundle.getString(RB_DISPLAY_NAME_PATTERN));
            // Construct the argument array
            int i = ID.indexOf('-');
            Object[] args = (i < 0)
                ? new Object[] { new Integer(1), ID }
                : new Object[] { new Integer(2), ID.substring(0, i),
                                 ID.substring(i+1) };

            // Use display names for the scripts, if they exist
            for (int j=1; j<=((i<0)?1:2); ++j) {
                try {
                    args[j] = bundle.getString(RB_SCRIPT_DISPLAY_NAME_PREFIX +
                                               (String) args[j]);
                } catch (MissingResourceException e) {}
            }

            // Format it using the pattern in the resource
            return format.format(args);
        } catch (MissingResourceException e2) {}

        // We should not reach this point unless there is something
        // wrong with the build or the RB_DISPLAY_NAME_PATTERN has
        // been deleted from the root RB_LOCALE_ELEMENTS resource.
        throw new RuntimeException();
    }

    /**
     * Returns the filter used by this transliterator, or <tt>null</tt>
     * if this transliterator uses no filter.
     */
    public UnicodeFilter getFilter() {
        return filter;
    }

    /**
     * Changes the filter used by this transliterator.  If the filter
     * is set to <tt>null</tt> then no filtering will occur.
     *
     * <p>Callers must take care if a transliterator is in use by
     * multiple threads.  The filter should not be changed by one
     * thread while another thread may be transliterating.
     */
    public void setFilter(UnicodeFilter filter) {
        this.filter = filter;
    }

    /**
     * Returns this transliterator's inverse.  See the class
     * documentation for details.  This implementation simply inverts
     * the two entities in the ID and attempts to retrieve the
     * resulting transliterator.  That is, if <code>getID()</code>
     * returns "A-B", then this method will return the result of
     * <code>getInstance("B-A")</code>, or <code>null</code> if that
     * call fails.
     *
     * <p>This method does not take filtering into account.  The
     * returned transliterator will have no filter.
     *
     * <p>Subclasses with knowledge of their inverse may wish to
     * override this method.
     *
     * @return a transliterator that is an inverse, not necessarily
     * exact, of this transliterator, or <code>null</code> if no such
     * transliterator is registered.
     * @see #registerInstance
     */
    public Transliterator getInverse() {
        int i = ID.indexOf('-');
        if (i >= 0) {
            String inverseID = ID.substring(i+1) + '-' + ID.substring(0, i);
            return internalGetInstance(inverseID);
        }
        return null;
    }

    /**
     * Returns a <code>Transliterator</code> object given its ID.
     * The ID must be either a system transliterator ID or a ID registered
     * using <code>registerInstance()</code>.
     *
     * @param ID a valid ID, as enumerated by <code>getAvailableIDs()</code>
     * @return A <code>Transliterator</code> object with the given ID
     * @exception IllegalArgumentException if the given ID is invalid.
     * @see #registerInstance
     * @see #getAvailableIDs
     * @see #getID
     */
    public static Transliterator getInstance(String ID) {
        Transliterator t = internalGetInstance(ID);
        if (t != null) {
            return t;
        }
        throw new IllegalArgumentException("Unsupported transliterator: "
                                           + ID);
    }

    /**
     * Returns a transliterator object given its ID.  Unlike getInstance(),
     * this method returns null if it cannot make use of the given ID.
     */
    private static Transliterator internalGetInstance(String ID) {
        Object obj = cache.get(ID);
        RuleBasedTransliterator.Data data = null;

        if (obj instanceof RuleBasedTransliterator.Data) {
            data = (RuleBasedTransliterator.Data) obj;
            // Fall through to construct transliterator from cached Data object.
        } else if (obj instanceof Class) {
            try {
                return (Transliterator) ((Class) obj).newInstance();
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e2) {}
        } else {
            synchronized (cache) {
                boolean isReverse = (obj == REVERSE_RULE_BASED_PLACEHOLDER);
                String resourceName = RB_RULE_BASED_PREFIX;
                int i = ID.indexOf('-');
                if (i < 0) {
                    resourceName += ID;
                } else {
                    String IDLeft  = ID.substring(0, i);
                    String IDRight = ID.substring(i+1);
                    resourceName += isReverse ? (IDRight + '$' + IDLeft)
                                              : (IDLeft + '$' + IDRight);
                }
                try {
                    ResourceBundle resource = ResourceBundle.getBundle(resourceName);

                    // We allow the resource bundle to contain either an array
                    // of rules, or a single rule string.
                    String[] ruleArray;
                    try {
                        ruleArray = resource.getStringArray(RB_RULE);
                    } catch (Exception e) {
                        // This is a ClassCastException under JDK 1.1.8
                        ruleArray = new String[] { resource.getString(RB_RULE) };
                    }

                    data = RuleBasedTransliterator.parse(ruleArray,
                                                         isReverse
                                                         ? RuleBasedTransliterator.REVERSE
                                                         : RuleBasedTransliterator.FORWARD);

                    cache.put(ID, data);
                    // Fall through to construct transliterator from Data object.
                } catch (MissingResourceException e) {}
            }
        }

        if (data != null) {
            return new RuleBasedTransliterator(ID, data, null);
        }

        return null;
    }

    /**
     * Registers a subclass of <code>Transliterator</code> with the
     * system.  This subclass must have a public constructor taking no
     * arguments.  When that constructor is called, the resulting
     * object must return the <code>ID</code> passed to this method if
     * its <code>getID()</code> method is called.
     *
     * @param ID the result of <code>getID()</code> for this
     * transliterator
     * @param transClass a subclass of <code>Transliterator</code>
     * @see #registerInstance
     * @see #unregister
     */
    public static void registerClass(String ID, Class transClass) {
        cache.put(ID, transClass);        
    }

    /**
     * Unregisters a transliterator or class.  This may be either
     * a system transliterator or a user transliterator or class.
     * 
     * @param ID the ID of the transliterator or class
     * @return the <code>Object</code> that was registered with
     * <code>ID</code>, or <code>null</code> if none was
     * @see #registerInstance
     * @see #registerClass
     */
    public static Object unregister(String ID) {
        return cache.remove(ID);
    }

    /**
     * Returns an enumeration over the programmatic names of registered
     * <code>Transliterator</code> objects.  This includes both system
     * transliterators and user transliterators registered using
     * <code>registerInstance()</code>.  The enumerated names may be
     * passed to <code>getInstance()</code>.
     *
     * @return An <code>Enumeration</code> over <code>String</code> objects
     * @see #getInstance
     * @see #registerInstance
     */
    public static final Enumeration getAvailableIDs() {
        return cache.keys();
    }

    static {
        ResourceBundle bundle = ResourceBundle.getBundle(RB_LOCALE_ELEMENTS);
        
        try {
            String[] ruleBasedIDs = bundle.getStringArray(RB_RULE_BASED_IDS);
            
            cache = new Hashtable();
            
            for (int i=0; i<ruleBasedIDs.length; ++i) {
                String ID = ruleBasedIDs[i];
                boolean isReverse = (ID.charAt(0) == '*');
                if (isReverse) {
                    ID = ID.substring(1);
                }
                cache.put(ID, isReverse ? REVERSE_RULE_BASED_PLACEHOLDER
                                        : RULE_BASED_PLACEHOLDER);
            }
        } catch (MissingResourceException e) {}

        registerClass(HexToUnicodeTransliterator._ID,
                      HexToUnicodeTransliterator.class);
        registerClass(UnicodeToHexTransliterator._ID,
                      UnicodeToHexTransliterator.class);
    }
}
