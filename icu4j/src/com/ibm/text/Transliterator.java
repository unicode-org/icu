/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/Transliterator.java,v $ 
 * $Date: 2001/03/30 23:24:30 $ 
 * $Revision: 1.28 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

import java.util.*;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.io.UnsupportedEncodingException;
import com.ibm.text.resources.ResourceReader;

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
 * <code>transliterate()</code>.  Typically, the cursor will
 * be coincident with the insertion point, but in a case like the one
 * above, it will precede the insertion point.
 *
 * <p>Keyboard transliteration methods maintain a set of three indices
 * that are updated with each call to
 * <code>transliterate()</code>, including the cursor, start,
 * and limit.  These indices are changed by the method, and they are
 * passed in and out via a Position object. The <code>start</code> index
 * marks the beginning of the substring that the transliterator will
 * look at.  It is advanced as text becomes committed (but it is not
 * the committed index; that's the <code>cursor</code>).  The
 * <code>cursor</code> index, described above, marks the point at
 * which the transliterator last stopped, either because it reached
 * the end, or because it required more characters to disambiguate
 * between possible inputs.  The <code>cursor</code> can also be
 * explicitly set by rules in a <code>RuleBasedTransliterator</code>.
 * Any characters before the <code>cursor</code> index are frozen;
 * future keyboard transliteration calls within this input sequence
 * will not change them.  New text is inserted at the
 * <code>limit</code> index, which marks the end of the substring that
 * the transliterator looks at.
 *
 * <p>Because keyboard transliteration assumes that more characters
 * are to arrive, it is conservative in its operation.  It only
 * transliterates when it can do so unambiguously.  Otherwise it waits
 * for more characters to arrive.  When the client code knows that no
 * more characters are forthcoming, perhaps because the user has
 * performed some input termination operation, then it should call
 * <code>finishTransliteration()</code> to complete any
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
 * <p><b>Composed transliterators</b>
 *
 * <p>In addition to built-in system transliterators like
 * "Latin-Greek", there are also built-in <em>composed</em>
 * transliterators.  These are implemented by composing two or more
 * component transliterators.  For example, if we have scripts "A",
 * "B", "C", and "D", and we want to transliterate between all pairs
 * of them, then we need to write 12 transliterators: "A-B", "A-C",
 * "A-D", "B-A",..., "D-A", "D-B", "D-C".  If it is possible to
 * convert all scripts to an intermediate script "M", then instead of
 * writing 12 rule sets, we only need to write 8: "A~M", "B~M", "C~M",
 * "D~M", "M~A", "M~B", "M~C", "M~D".  (This might not seem like a big
 * win, but it's really 2<em>n</em> vs. <em>n</em><sup>2</sup> -
 * <em>n</em>, so as <em>n</em> gets larger the gain becomes
 * significant.  With 9 scripts, it's 18 vs. 72 rule sets, a big
 * difference.)  Note the use of "~" rather than "-" for the script
 * separator here; this indicates that the given transliterator is
 * intended to be composed with others, rather than be used as is.
 *
 * <p>Composed transliterators can be instantiated as usual.  For
 * example, the system transliterator "Devanagari-Gujarati" is a
 * composed transliterator built internally as
 * "Devanagari~InterIndic;InterIndic~Gujarati".  When this
 * transliterator is instantiated, it appears externally to be a
 * standard transliterator (e.g., getID() returns
 * "Devanagari-Gujarati").
 *
 * <p><b>Subclassing</b>
 *
 * <p>Subclasses must implement the abstract method
 * <code>handleTransliterate()</code>.  <p>Subclasses should override
 * the <code>transliterate()</code> method taking a
 * <code>Replaceable</code> and the <code>transliterate()</code>
 * method taking a <code>String</code> and <code>StringBuffer</code>
 * if the performance of these methods can be improved over the
 * performance obtained by the default implementations in this class.
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: Transliterator.java,v $ $Revision: 1.28 $ $Date: 2001/03/30 23:24:30 $
 */
public abstract class Transliterator {
    /**
     * Direction constant indicating the forward direction in a transliterator,
     * e.g., the forward rules of a RuleBasedTransliterator.  An "A-B"
     * transliterator transliterates A to B when operating in the forward
     * direction, and B to A when operating in the reverse direction.
     * @see RuleBasedTransliterator
     * @see CompoundTransliterator
     */
    public static final int FORWARD = 0;

    /**
     * Direction constant indicating the reverse direction in a transliterator,
     * e.g., the reverse rules of a RuleBasedTransliterator.  An "A-B"
     * transliterator transliterates A to B when operating in the forward
     * direction, and B to A when operating in the reverse direction.
     * @see RuleBasedTransliterator
     * @see CompoundTransliterator
     */
    public static final int REVERSE = 1;    

    /**
     * Position structure for incremental transliteration.  This data
     * structure defines two substrings of the text being
     * transliterated.  The first region, [contextStart,
     * contextLimit), defines what characters the transliterator will
     * read as context.  The second region, [start, limit), defines
     * what characters will actually be transliterated.  The second
     * region should be a subset of the first.
     *
     * <p>After a transliteration operation, some of the indices in this
     * structure will be modified.  See the field descriptions for
     * details.
     *
     * <p>contextStart <= start <= limit <= contextLimit
     */
    public static class Position {

        /**
         * Beginning index, inclusive, of the context to be considered for
         * a transliteration operation.  The transliterator will ignore
         * anything before this index.  INPUT/OUTPUT parameter: This parameter
         * is updated by a transliteration operation to reflect the maximum
         * amount of antecontext needed by a transliterator.
         */
        public int contextStart;

        /**
         * Ending index, exclusive, of the context to be considered for a
         * transliteration operation.  The transliterator will ignore
         * anything at or after this index.  INPUT/OUTPUT parameter: This
         * parameter is updated to reflect changes in the length of the
         * text, but points to the same logical position in the text.
         */
        public int contextLimit;

        /**
         * Beginning index, inclusive, of the text to be transliteratd.
         * INPUT/OUTPUT parameter: This parameter is advanced past
         * characters that have already been transliterated by a
         * transliteration operation.
         */
        public int start;

        /**
         * Ending index, exclusive, of the text to be transliteratd.
         * INPUT/OUTPUT parameter: This parameter is updated to reflect
         * changes in the length of the text, but points to the same
         * logical position in the text.
         */
        public int limit;

        public Position() {
            this(0, 0, 0, 0);
        }

        public Position(int contextStart, int contextLimit, int start) {
            this(contextStart, contextLimit, start, contextLimit);
        }

        public Position(int contextStart, int contextLimit,
                        int start, int limit) {
            this.contextStart = contextStart;
            this.contextLimit = contextLimit;
            this.start = start;
            this.limit = limit;
        }
    }

    /**
     * Programmatic name, e.g., "Latin-Arabic".
     */
    private String ID;

    /** 
     * This transliterator's filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    private UnicodeFilter filter;

    private int maximumContextLength = 0;

    /**
     * Cache of system transliterators.  Keys are <code>String</code>
     * names, values are one of the following:
     *
     * <ul><li><code>String</code> objects.  These represent
     * RuleBasedTransliterators that have not been loaded yet, or
     * aliases.  The first character determines the type: 'f'
     * indicates a FORWARD RBT, with the rest of the string giving the
     * resource name and encoding, separated by a colon.  'r' is
     * similar, but indicates a REVERSE RBT.  'a' indicates an alias,
     * with the rest of the string giving the ID to create.
     *
     * <li><code>Class</code> objects.  Such objects must represent
     * subclasses of <code>Transliterator</code>, and must satisfy the
     * constraints described in <code>registerClass()</code>.
     *
     * <li><code>RuleBasedTransliterator.Data</code> objects.  These
     * are built in-memory transliterator data cores that are wrapped
     * thinly to create RuleBasedTransliterator objects.  When an RBT
     * is created, its Data core is cached and shared among future
     * instances of the same ID.
     * </ul>
     */
    private static Hashtable cache;

    /**
     * Identical to 'cache' but contains internal transliterators.
     * These are not enumerated by getAvailableIDs().
     */
    private static Hashtable internalCache;

    private static Hashtable displayNameCache;

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
     * Resource bundle containing display name keys and the
     * RB_RULE_BASED_IDS array.
     *
     * <p>If we ever integrate this with the Sun JDK, the resource bundle
     * root will change to java.text.resources.LocaleElements
     */
    private static final String RB_LOCALE_ELEMENTS =
        "com.ibm.text.resources.LocaleElements";

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Default constructor.
     * @param ID the string identifier for this transliterator
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
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
     * Transliterates a segment of a string, with optional filtering.
     *
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @return The new limit index.  The text previously occupying <code>[start,
     * limit)</code> has been transliterated, possibly to a string of a different
     * length, at <code>[start, </code><em>new-limit</em><code>)</code>, where
     * <em>new-limit</em> is the return value.
     */
    public final int transliterate(Replaceable text, int start, int limit) {
        Position pos = new Position(start, limit, start);
        handleTransliterate(text, pos, false);
        return pos.contextLimit;
    }

    /**
     * Transliterates an entire string in place. Convenience method.
     * @param text the string to be transliterated
     */
    public final void transliterate(Replaceable text) {
        transliterate(text, 0, text.length());
    }

    /**
     * Transliterate an entire string and returns the result. Convenience method.
     *
     * @param text the string to be transliterated
     * @return The transliterated text
     */
    public final String transliterate(String text) {
        ReplaceableString result = new ReplaceableString(text);
        transliterate(result);
        return result.toString();
    }

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly after new text has been inserted,
     * typically as a result of a keyboard event.  The new text in
     * <code>insertion</code> will be inserted into <code>text</code>
     * at <code>index.contextLimit</code>, advancing
     * <code>index.contextLimit</code> by <code>insertion.length()</code>.
     * Then the transliterator will try to transliterate characters of
     * <code>text</code> between <code>index.start</code> and
     * <code>index.contextLimit</code>.  Characters before
     * <code>index.start</code> will not be changed.
     *
     * <p>Upon return, values in <code>index</code> will be updated.
     * <code>index.contextStart</code> will be advanced to the first
     * character that future calls to this method will read.
     * <code>index.start</code> and <code>index.contextLimit</code> will
     * be adjusted to delimit the range of text that future calls to
     * this method may change.
     *
     * <p>Typical usage of this method begins with an initial call
     * with <code>index.contextStart</code> and <code>index.contextLimit</code>
     * set to indicate the portion of <code>text</code> to be
     * transliterated, and <code>index.start == index.contextStart</code>.
     * Thereafter, <code>index</code> can be used without
     * modification in future calls, provided that all changes to
     * <code>text</code> are made via this method.
     *
     * <p>This method assumes that future calls may be made that will
     * insert new text into the buffer.  As a result, it only performs
     * unambiguous transliterations.  After the last call to this
     * method, there may be untransliterated text that is waiting for
     * more input to resolve an ambiguity.  In order to perform these
     * pending transliterations, clients should call {@link
     * #finishTransliteration} after the last call to this
     * method has been made.
     * 
     * @param text the buffer holding transliterated and untransliterated text
     * @param index the start and limit of the text, the position
     * of the cursor, and the start and limit of transliteration.
     * @param insertion text to be inserted and possibly
     * transliterated into the translation buffer at
     * <code>index.contextLimit</code>.  If <code>null</code> then no text
     * is inserted.
     * @see #handleTransliterate
     * @exception IllegalArgumentException if <code>index</code>
     * is invalid
     */
    public final void transliterate(Replaceable text, Position index,
                                    String insertion) {
        if (index.contextStart < 0 ||
            index.contextLimit > text.length() ||
            index.start < index.contextStart ||
            index.start > index.contextLimit) {
            throw new IllegalArgumentException("Invalid index");
        }

        int originalStart = index.contextStart;
        if (insertion != null) {
            text.replace(index.limit, index.limit, insertion);
            index.limit += insertion.length();
            index.contextLimit += insertion.length();
        }

        handleTransliterate(text, index, true);

        index.contextStart = Math.max(index.start - getMaximumContextLength(),
                                      originalStart);
    }

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly after a new character has been
     * inserted, typically as a result of a keyboard event.  This is a
     * convenience method; see {@link #transliterate(Replaceable,
     * Transliterator.Position, String)} for details.
     * @param text the buffer holding transliterated and
     * untransliterated text
     * @param index the start and limit of the text, the position
     * of the cursor, and the start and limit of transliteration.
     * @param insertion text to be inserted and possibly
     * transliterated into the translation buffer at
     * <code>index.contextLimit</code>.
     * @see #transliterate(Replaceable, Transliterator.Position, String)
     */
    public final void transliterate(Replaceable text, Position index,
                                    char insertion) {
        transliterate(text, index, String.valueOf(insertion));
    }

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly.  This is a convenience method; see
     * {@link #transliterate(Replaceable, Transliterator.Position,
     * String)} for details.
     * @param text the buffer holding transliterated and
     * untransliterated text
     * @param index the start and limit of the text, the position
     * of the cursor, and the start and limit of transliteration.
     * @see #transliterate(Replaceable, Transliterator.Position, String)
     */
    public final void transliterate(Replaceable text, Position index) {
        transliterate(text, index, null);
    }

    /**
     * Finishes any pending transliterations that were waiting for
     * more characters.  Clients should call this method as the last
     * call after a sequence of one or more calls to
     * <code>transliterate()</code>.
     * @param text the buffer holding transliterated and
     * untransliterated text.
     * @param index the array of indices previously passed to {@link
     * #transliterate}
     */
    public final void finishTransliteration(Replaceable text,
                                            Position index) {
        handleTransliterate(text, index, false);
    }

    /**
     * Abstract method that concrete subclasses define to implement
     * keyboard transliteration.  This method should transliterate all
     * characters between <code>index.start</code> and
     * <code>index.contextLimit</code> that can be unambiguously
     * transliterated, regardless of future insertions of text at
     * <code>index.contextLimit</code>.  <code>index.start</code> should
     * be advanced past committed characters (those that will not
     * change in future calls to this method).
     * <code>index.contextLimit</code> should be updated to reflect text
     * replacements that shorten or lengthen the text between
     * <code>index.start</code> and <code>index.contextLimit</code>.  Upon
     * return, neither <code>index.start</code> nor
     * <code>index.contextLimit</code> should be less than the initial value
     * of <code>index.start</code>.  <code>index.contextStart</code>
     * should <em>not</em> be changed.
     *
     * @param text the buffer holding transliterated and
     * untransliterated text
     * @param pos the start and limit of the text, the position
     * of the cursor, and the start and limit of transliteration.
     * @param incremental if true, assume more text may be coming after
     * pos.contextLimit.  Otherwise, assume the text is complete.
     * @see #transliterate
     */
    protected abstract void handleTransliterate(Replaceable text,
                                                Position pos, boolean incremental);

    /**
     * Returns the length of the longest context required by this transliterator.
     * This is <em>preceding</em> context.  The default value is zero, but
     * subclasses can change this by calling <code>setMaximumContextLength()</code>.
     * For example, if a transliterator translates "ddd" (where
     * d is any digit) to "555" when preceded by "(ddd)", then the preceding
     * context length is 5, the length of "(ddd)".
     *
     * @return The maximum number of preceding context characters this
     * transliterator needs to examine
     */
    protected final int getMaximumContextLength() {
        return maximumContextLength;
    }

    /**
     * Method for subclasses to use to set the maximum context length.
     * @see #getMaximumContextLength
     */
    protected void setMaximumContextLength(int a) {
        if (a < 0) {
            throw new IllegalArgumentException("Invalid context length " + a);
        }
        maximumContextLength = a;
    }

    /**
     * Returns a programmatic identifier for this transliterator.
     * If this identifier is passed to <code>getInstance()</code>, it
     * will return this object, if it has been registered.
     * @see #registerClass
     * @see #getAvailableIDs
     */
    public final String getID() {
        return ID;
    }

    /**
     * Returns a name for this transliterator that is appropriate for
     * display to the user in the default locale.  See {@link
     * #getDisplayName(String,Locale)} for details.
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

        // Use the registered display name, if any
        String n = (String) displayNameCache.get(ID);
        if (n != null) {
            return n;
        }

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
     * Returns a <code>Transliterator</code> object given its ID.
     * The ID must be either a system transliterator ID or a ID registered
     * using <code>registerClass()</code>.
     *
     * @param ID a valid ID, as enumerated by <code>getAvailableIDs()</code>
     * @return A <code>Transliterator</code> object with the given ID
     * @exception IllegalArgumentException if the given ID is invalid.
     * @see #registerClass
     * @see #getAvailableIDs
     * @see #getID
     */
    public static Transliterator getInstance(String ID, int direction) {
        if (ID.indexOf(';') >= 0) {
            return new CompoundTransliterator(ID, direction, null);
        }

        for (;;) {
            UnicodeFilter filter = null;
            int i = ID.indexOf('[');
            if (i >= 0) {
                ParsePosition pos = new ParsePosition(i);
                filter = new UnicodeSet(ID, pos, null);
                if (pos.getIndex() != ID.length()) {
                    break; // unparsed junk after ']'
                }
                ID = ID.substring(0, i);
            }

            if (direction == REVERSE) {
                i = ID.indexOf('-');
                if (i < 0) {
                    if (!ID.equals(NullTransliterator._ID)) {
                        throw new IllegalArgumentException("No inverse for: "
                                                           + ID);
                    }
                } else {
                    ID = ID.substring(i+1) + '-' + ID.substring(0, i);
                }
            }

            Transliterator t = internalGetInstance(ID);
            if (t != null) {
                if (filter != null) {
                    t.setFilter(filter);
                }
                return t;
            }
        
            break;
        }
        throw new IllegalArgumentException("Unsupported transliterator: "
                                           + ID);
    }

    public static final Transliterator getInstance(String ID) {
        return getInstance(ID, FORWARD);
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
     * @see #registerClass
     */
    public final Transliterator getInverse() {
        return getInstance(ID, REVERSE);
    }
    
    /**
     * Returns a transliterator object given its ID.  Unlike getInstance(),
     * this method returns null if it cannot make use of the given ID.
     */
    private static Transliterator internalGetInstance(String ID) {
        RuleBasedTransliterator.Data data = null;
        Hashtable sourceCache = cache;
        Object obj = cache.get(ID);
        if (obj == null) {
            obj = internalCache.get(ID);
            sourceCache = internalCache;
        }
        
        if (obj != null) {
            if (obj instanceof RuleBasedTransliterator.Data) {
                data = (RuleBasedTransliterator.Data) obj;
                // Fall through to construct transliterator from cached Data object.
            } else if (obj instanceof Class) {
                try {
                    return (Transliterator) ((Class) obj).newInstance();
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e2) {}
            } else if (obj instanceof String) {
                String spec = (String) obj;
                if (spec.charAt(0) == 'a') {
                    // alias
                    Transliterator t = getInstance(spec.substring(1));
                    t.ID = ID;
                    return t;
                } else {
                    synchronized (cache) {
                        // file, either forward or reverse
                        int dir = (spec.charAt(0) == 'f') ? FORWARD:REVERSE;
                        int colon = spec.indexOf(':', 1);
                        String resourceName = spec.substring(1, colon);
                        String encoding = spec.substring(colon+1);
                        ResourceReader r = null;
                        try {
                            r = new ResourceReader(resourceName, encoding);
                        } catch (UnsupportedEncodingException e) {
                            // This should never happen; UTF8 is always supported
                        } catch (IllegalArgumentException e2) {
                            // Can't load UTF8 file
                        }
                        
                        if (r != null) {
                            data = RuleBasedTransliterator.parse(r, dir);
                            sourceCache.put(ID, data);
                            // Fall through to construct transliterator from Data object.
                        }
                    }
                }
            } else {
                throw new RuntimeException("Bogus cache object");
            }

            if (data != null) {
                return new RuleBasedTransliterator(ID, data, null);
            }
        }

        return null;
    }

// Currently unused, but may be of use in the future.
//    /**
//     * Find a path through the composed transliterator graph.  This
//     * will not necessarily be the only path, or the shortest path.
//     * This is a simple recursive algorithm.
//     * 
//     * <p><code>composedGraph</code> is the links table.
//     * composedGraph.get(x) should return a String[] array, each of
//     * which is a node that x is connected to.
//     * @param start the starting node
//     * @param end the ending node
//     * @param path the result vector; should be empty on entry.  Upon
//     * success, it will contain successive nodes on the path from
//     * start to end, including start and end.  If false is returned,
//     * then path is unchanged.
//     * @return true if a path from start to end is found
//     */
//    private static boolean findComposedPath(String start, String end,
//                                            Vector path) {
//        path.addElement(start);
//        // composedGraph lists all links emanating from a node
//        String[] links = (String[]) composedGraph.get(start);
//        if (links != null) {
//            for (int i=0; i<links.length; ++i) {
//                if (links[i].equals(end)) {
//                    path.addElement(end);
//                    return true;
//                }
//            }
//            for (int i=0; i<links.length; ++i) {
//                // Avoid cycles: ignore links already on our path
//                if (path.indexOf(links[i]) >= 0) {
//                    continue;
//                }
//                if (findComposedPath(links[i], end, path)) {
//                    return true;
//                }
//            }
//        }
//        path.removeElementAt(path.size() - 1);    
//        return false;
//    }

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
     * @see #unregister
     */
    public static void registerClass(String ID, Class transClass, String displayName) {
        cache.put(ID, transClass);
        if (displayName != null) {
            displayNameCache.put(ID, displayName);
        }
    }

    /**
     * Unregisters a transliterator or class.  This may be either
     * a system transliterator or a user transliterator or class.
     * 
     * @param ID the ID of the transliterator or class
     * @return the <code>Object</code> that was registered with
     * <code>ID</code>, or <code>null</code> if none was
     * @see #registerClass
     */
    public static Object unregister(String ID) {
        displayNameCache.remove(ID);
        return cache.remove(ID);
    }

    /**
     * Returns an enumeration over the programmatic names of registered
     * <code>Transliterator</code> objects.  This includes both system
     * transliterators and user transliterators registered using
     * <code>registerClass()</code>.  The enumerated names may be
     * passed to <code>getInstance()</code>.
     *
     * @return An <code>Enumeration</code> over <code>String</code> objects
     * @see #getInstance
     * @see #registerClass
     */
    public static final Enumeration getAvailableIDs() {
        return cache.keys();
    }

    /**
     * Method for subclasses to use to obtain a character in the given
     * string, with filtering.  If the character at the given offset
     * is excluded by this transliterator's filter, then U+FFFE is returned.
     */
    protected char filteredCharAt(Replaceable text, int i) {
        char c;
        UnicodeFilter filter = getFilter();
        return (filter == null) ? text.charAt(i) :
            (filter.contains(c = text.charAt(i)) ? c : '\uFFFE');
    }

    static {
        // The display name cache starts out empty
        displayNameCache = new Hashtable();

        // Read the index file and construct the cache/internalCache.
        // Each line of the index file is either blank, a '#' comment,
        // or a colon-delimited line.  In the latter case the first
        // field is the ID being defined.  The second field is one of
        // three strings: "file", "internal", or "alias".  Remaining
        // fields vary according the value fo the second field.  See
        // the index file itself for further documentation.
        cache = new Hashtable();
        internalCache = new Hashtable();
        ResourceReader r = new ResourceReader("Transliterator_index.txt");
        for (;;) {
            String line = null;
            try {
                line = r.readLine();
            } catch (java.io.IOException e) {}
            if (line == null) {
                break;
            }
            // Skip over whitespace
            int pos = 0;
            while (pos < line.length() &&
                   Character.isWhitespace(line.charAt(pos))) {
                ++pos;
            }
            // Ignore blank lines and comments
            if (pos == line.length() || line.charAt(pos) == '#') {
                continue;
            }
            // Parse colon-delimited line
            int colon = line.indexOf(':', pos);
            String ID = line.substring(pos, colon);
            pos = colon+1;
            colon = line.indexOf(':', pos);
            String type = line.substring(pos, colon);
            pos = colon+1;

            if (type.equals("file") || type.equals("internal")) {
                // Rest of line is <resource>:<encoding>:<direction>
                colon = line.indexOf(':', pos);
                colon = line.indexOf(':', colon+1); // skip over 1 colon
                String fileNameAndEncoding = line.substring(pos, colon);
                pos = colon+1;
                boolean isForward = line.substring(pos).equals("FORWARD");
                Hashtable h = type.equals("internal") ? internalCache:cache;
                h.put(ID, (isForward ? "f" : "r") + fileNameAndEncoding);
            } else if (type.equals("alias")) {
                // Rest of line is the <getInstanceArg>
                cache.put(ID, "a" + line.substring(pos));
            } else {
                // Unknown type
                throw new RuntimeException("Can't parse line: " + line);
            }
        }

        // Register non-rule-based transliterators
        registerClass(HangulJamoTransliterator._ID,
                      HangulJamoTransliterator.class, null);
        registerClass(JamoHangulTransliterator._ID,
                      JamoHangulTransliterator.class, null);
                      
        registerClass(HexToUnicodeTransliterator._ID,
                      HexToUnicodeTransliterator.class, null);
        registerClass(UnicodeToHexTransliterator._ID,
                      UnicodeToHexTransliterator.class, null);
        registerClass(NullTransliterator._ID,
                      NullTransliterator.class, null);
    }
}
