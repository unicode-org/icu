/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Transliterator.java,v $
 * $Date: 2001/11/15 23:38:01 $
 * $Revision: 1.53 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

import java.util.*;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.io.UnsupportedEncodingException;
import com.ibm.text.resources.ResourceReader;
import com.ibm.util.CaseInsensitiveString;
import com.ibm.util.Utility;

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
 * @version $RCSfile: Transliterator.java,v $ $Revision: 1.53 $ $Date: 2001/11/15 23:38:01 $
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
     * System transliterator registry.
     */
    private static TransliteratorRegistry registry;

    private static Hashtable displayNameCache;

    private static Hashtable specialInverses;

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

    protected static final char ID_DELIM = ';';

    protected static final char ID_SEP = '-';

    protected static final char VARIANT_SEP = '/';

    private static final String ANY = "Any";

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
        filteredTransliterate(text, pos, false);
        return pos.limit;
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
            index.start < index.contextStart ||
            index.limit < index.start ||
            index.contextLimit < index.limit ||
            text.length() < index.contextLimit) {
            throw new IllegalArgumentException("Invalid index {" +
                                               index.contextStart + ", " +
                                               index.start + ", " +
                                               index.limit + ", " +
                                               index.contextLimit + "}, len=" +
                                               text.length());
        }

//        int originalStart = index.contextStart;
        if (insertion != null) {
            text.replace(index.limit, index.limit, insertion);
            index.limit += insertion.length();
            index.contextLimit += insertion.length();
        }

        if (index.limit > 0 &&
            UTF16.isLeadSurrogate(text.charAt(index.limit - 1))) {
            // Oops, there is a dangling lead surrogate in the buffer.
            // This will break most transliterators, since they will
            // assume it is part of a pair.  Don't transliterate until
            // more text comes in.
            return;
        }

        filteredTransliterate(text, index, true);

// This doesn't work once we add quantifier support.  Need to rewrite
// this code to support quantifiers and 'use maximum backup <n>;'.
//
//        index.contextStart = Math.max(index.start - getMaximumContextLength(),
//                                      originalStart);
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
                                    int insertion) {
        transliterate(text, index, UTF16.valueOf(insertion));
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
        filteredTransliterate(text, index, false);
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
     * <p>Subclasses may safely assume that all characters in
     * [index.start, index.limit) are unfiltered.  In other words, the
     * filter has already been applied by the time this method is
     * called.  See filteredTransliterate().
     *
     * <p>This method is <b>not</b> for public consumption.  Calling
     * this method directly will transliterate [index.start,
     * index.limit) without applying the filter.  End user code that
     * wants to call this method should be calling transliterate().
     * Subclass code that wants to call this method should probably be
     * calling filteredTransliterate().
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
     * This method breaks up the input text into runs of unfiltered
     * characters.  It passes each such run to
     * <subclass>.handleTransliterate().  Subclasses that can handle the
     * filter logic more efficiently themselves may override this method.
     *
     * All transliteration calls in this class go through this method.
     */
    protected void filteredTransliterate(Replaceable text,
                                         Position index,
                                         boolean incremental) {
        if (filter == null) {
            // Short circuit path for transliterators with no filter
            handleTransliterate(text, index, incremental);
            return;
        }

        // globalLimit is the limit value for the entire operation.  We
        // set index.limit to the end of each unfiltered run before
        // calling handleTransliterate(), so we need to maintain the real
        // value of index.limit here.  After each transliteration, we
        // update globalLimit for insertions or deletions that have
        // happened.
        int globalLimit = index.limit;

        // Break the input text up.  Say the input text has the form:
        //   xxxabcxxdefxx
        // where 'x' represents a filtered character.  Then we break this
        // up into:
        //   xxxabc xxdef xx
        // Each pass through the loop consumes a run of filtered
        // characters (which are ignored) and a subsequent run of
        // unfiltered characters (which are transliterated).  If, at any
        // point, we fail to consume our entire segment, we stop.
        for (;;) {
            // Narrow the range to be transliterated to the first segment
            // of unfiltered characters at or after index.start.

            int c;

            // Advance compoundStart past filtered chars
            while (index.start < globalLimit &&
                   !filter.contains(c=UTF16.charAt(text, index.start))) {
                index.start += UTF16.getCharCount(c);
            }

            // Find the end of this run of unfiltered chars
            index.limit = index.start;
            while (index.limit < globalLimit &&
                   filter.contains(c=UTF16.charAt(text, index.limit))) {
                index.limit += UTF16.getCharCount(c);
            }

            // Check to see if the unfiltered run is empty.  This only
            // happens at the end of the string when all the remaining
            // characters are filtered.
            if (index.limit == index.start) {
                // assert(index.start == globalLimit);
                break;
            }

            int limit = index.limit;

            // Is this segment incremental?  If there is additional
            // filtered text (if limit < globalLimit) then we pass in
            // an incremental value of FALSE to force the subclass to
            // complete the transliteration for this segment.
            boolean isIncrementalSegment =
                (limit < globalLimit ? false : incremental);

            // Implement rollback.  To understand the need for rollback,
            // consider the following transliterator:
            //
            //  "t" is "a > A;"
            //  "u" is "A > b;"
            //  "v" is a compound of "t; NFD; u" with a filter [:Ll:]
            //
            // Now apply "c" to the input text "a".  The result is "b".  But if
            // the transliteration is done incrementally, then the NFD holds
            // things up after "t" has already transformed "a" to "A".  When
            // finishTransliterate() is called, "A" is _not_ processed because
            // it gets excluded by the [:Ll:] filter, and the end result is "A"
            // -- incorrect.  The problem is that the filter is applied to a
            // partially-transliterated result, when we only want it to apply to
            // input text.  Although this example hinges on a compound
            // transliterator containing NFD and a specific filter, it can
            // actually happen with any transliterator which may do a partial
            // transformation in incremental mode into characters outside its
            // filter.
            //
            // There are two solutions.  The first is to add two new index
            // values to the position structure, a filteredStart and a
            // filteredLimit.  Then filteredTransliterate() can set and read
            // these, and avoid filtering partially transliterated results.  A
            // variant of this solution is to retain an internal state object
            // with the filtered range that is indexed by the text pointer and
            // the position object pointer, in analogy to strtok().  The third
            // solution involves no change to the API and no internal state
            // cache.  It is to roll back any partially transliterated results
            // if (a) there is a filter, and (b) the transliteration is
            // incremental.  This is the solution implemented here.
            int rollbackStart = 0;
            int rollbackCopy = 0;
            if (isIncrementalSegment) {
                // Make a rollback copy at the end of the string
                rollbackStart = index.start;
                rollbackCopy = text.length();
                text.copy(rollbackStart, limit, rollbackCopy);
            }

            // Delegate to subclass for actual transliteration.
            handleTransliterate(text, index, isIncrementalSegment);

            int delta = index.limit - limit; // change in length

            // Adjust overall limit for insertions/deletions.  Don't need
            // to worry about contextLimit because handleTransliterate()
            // maintains that.
            globalLimit += delta;

            // If we failed to complete transliterate this segment,
            // then we are done.  If rollback is required, then do so.
            if (index.start != index.limit) {
                if (isIncrementalSegment) {
                    // Replace [rollbackStart, limit) -- this is the
                    // original filtered segment -- with
                    // [rollbackCopy, text.length()), the rollback
                    // copy, then delete the rollback copy.
                    rollbackCopy += delta;
                    int rollbackLen = text.length() - rollbackCopy;
                    
                    // Delete the partially transliterated segment
                    rollbackCopy -= index.limit - rollbackStart;
                    text.replace(rollbackStart, index.limit, "");
                    
                    // Copy the rollback copy back
                    text.copy(rollbackCopy, text.length(), rollbackStart);
                    
                    // Delete the rollback copy
                    rollbackCopy += rollbackLen;
                    text.replace(rollbackCopy, text.length(), "");
                    
                    // Restore indices
                    index.start = rollbackStart;
                    index.limit = limit;
                    index.contextLimit -= delta;
                    globalLimit -= delta;
                }
                break;
            } else if (isIncrementalSegment) {
                // We finished this segment; delete the rollback copy
                rollbackCopy += delta;
                text.replace(rollbackCopy, text.length(), "");
            }

            // If we did completely transliterate this
            // segment, then repeat with the next unfiltered segment.
        }

        // Start is valid where it is.  Limit needs to be put back where
        // it was, modulo adjustments for deletions/insertions.
        index.limit = globalLimit;
    }

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
     * Set the programmatic identifier for this transliterator.  Only
     * for use by subclasses.
     */
    protected final void setID(String id) {
        ID = id;
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
    public final UnicodeFilter getFilter() {
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
    public static final Transliterator getInstance(String ID, int direction) {
        return getInstance(ID, direction, -1, null);
    }

    public static final Transliterator getInstance(String ID) {
        return getInstance(ID, FORWARD, -1, null);
    }

    /**
     * Create a transliterator given a compound ID (possibly degenerate,
     * with no ID_DELIM).  If idSplitPoint >= 0 and adoptedSplitTrans !=
     * 0, then insert adoptedSplitTrans in the compound ID at offset
     * idSplitPoint.  Otherwise idSplitPoint should be -1 and
     * adoptedSplitTrans should be 0.  The resultant transliterator will
     * be an atomic (non-compound) transliterator if this is indicated by
     * ID.  Otherwise it will be a compound translitertor.
     */
    private static Transliterator getInstance(String ID,
                                              int dir,
                                              int idSplitPoint,
                                              Transliterator adoptedSplitTrans) {
        Vector list = new Vector();
        int[] ignored = new int[1];
        UnicodeSet[] compoundFilter = new UnicodeSet[1];
        StringBuffer regenID = new StringBuffer();
        parseCompoundID(ID, regenID, dir, idSplitPoint, adoptedSplitTrans,
                        list, ignored, compoundFilter);

        Transliterator t = null;
        switch (list.size()) {
        case 0:
            t = new NullTransliterator();
            break;
        case 1:
            t = (Transliterator) list.elementAt(0);
            break;
        default:
            t = new CompoundTransliterator(dir, list);
            break;
        }
        t.setID(regenID.toString());
        if (compoundFilter[0] != null) {
            t.setFilter(compoundFilter[0]);
        }
        return t;
    }

    /**
     * Returns a <code>Transliterator</code> object constructed from
     * the given rule string.  This will be a RuleBasedTransliterator,
     * if the rule string contains only rules, or a
     * CompoundTransliterator, if it contains ID blocks, or a
     * NullTransliterator, if it contains ID blocks which parse as
     * empty for the given direction.
     */
    public static final Transliterator createFromRules(String ID, String rules, int dir) {
        Transliterator t = null;

        TransliteratorParser parser = new TransliteratorParser();
        parser.parse(rules, dir);

        // NOTE: The logic here matches that in TransliteratorRegistry.
        if (parser.idBlock.length() == 0) {
            if (parser.data == null) {
                // No idBlock, no data -- this is just an
                // alias for Null
                t = new NullTransliterator();
            } else {
                // No idBlock, data != 0 -- this is an
                // ordinary RBT_DATA.
                t = new RuleBasedTransliterator(ID, parser.data, null);
            }
        } else {
            if (parser.data == null) {
                // idBlock, no data -- this is an alias.  The ID has
                // been munged from reverse into forward mode, if
                // necessary, so instantiate the ID in the forward
                // direction.
                t = getInstance(parser.idBlock);
                if (t != null) {
                    t.setID(ID);
                }
            } else {
                // idBlock and data -- this is a compound
                // RBT
                t = new RuleBasedTransliterator("_", parser.data, null);
                t = new CompoundTransliterator(ID, parser.idBlock, parser.idSplitPoint,
                                               t);
                if (parser.compoundFilter != null) {
                    t.setFilter(parser.compoundFilter);
                }
            }
        }

        return t;
    }

    public String toRules(boolean escapeUnprintable) {
        return baseToRules(escapeUnprintable);
    }

    protected final String baseToRules(boolean escapeUnprintable) {
        // The base class implementation of toRules munges the ID into
        // the correct format.  That is: foo => ::foo
        // KEEP in sync with rbt_pars
        return "::" + getID() + ID_DELIM;
    }

    /**
     * Parse a compound ID (possibly a degenerate one, containing no
     * ID_DELIM).  If idSplitPoint >= 0 and adoptedSplitTrans != 0, then
     * insert adoptedSplitTrans in the compound ID at offset idSplitPoint.
     * Otherwise idSplitPoint should be -1 and adoptedSplitTrans should be
     * 0.  Return in the result vector the instantiated transliterator
     * objects (one of these will be adoptedSplitTrans, if the latter was
     * specified).  These will be in order of id, so if dir is REVERSE,
     * then the caller will have to reverse the order.
     *
     * @param regenID regenerated ID, reversed if appropriate, which
     * should be applied to the final created transliterator
     * @param splitTransIndex output parameter to receive the index in
     * 'result' at which the adoptedSplitTrans is stored, or -1 if
     * adoptedSplitTrans == 0
     * @param compoundFilter output parameter to receive the parsed
     * compound filter, if any.  It receives either the FORWARD or the
     * REVERSE compound filter, depending on dir.
     */
    static void parseCompoundID(String id,
                                StringBuffer regenID,
                                int dir,
                                int idSplitPoint,
                                Transliterator splitTrans,
                                Vector result,
                                int[] splitTransIndex,
                                UnicodeSet[] compoundFilter) {
        regenID.setLength(0);
        splitTransIndex[0] = -1;
        int pos = 0;
        int i;

        // A compound filter is a filter on an entire compound
        // transliterator.  It is indicated by the syntax [abc]; A-B;
        // B-C or in the reverse direction A-B; B-C; ([abc]).  We
        // record the filter and its index (in terms of the result
        // vector).
        compoundFilter[0] = null;
        int compoundFilterIndex = -1;

        while (pos < id.length()) {
            // We compare (pos >= split), not (pos == split), so we can
            // skip over whitespace (see below).
            if (pos >= idSplitPoint && splitTrans != null) {
                splitTransIndex[0] = result.size();
                result.addElement(splitTrans);
                splitTrans = null;
            }
            int[] p = new int[] { pos };
            boolean[] sawDelimiter = new boolean[1];
            UnicodeSet[] cpdFilter = new UnicodeSet[1];
            Transliterator t =
                parseID(id, regenID, p, sawDelimiter, cpdFilter, dir, true);

            if (p[0] == pos || (p[0] < id.length() && !sawDelimiter[0])) {
                throw new IllegalArgumentException("Invalid ID " + id);
            }
            if (cpdFilter[0] != null) {
                if (compoundFilter[0] != null) {
                    // Multiple compound filters
                    throw new IllegalArgumentException("Multiple compound filters in " + id);
                }
                compoundFilter[0] = cpdFilter[0];
                compoundFilterIndex = result.size();
            }
            pos = p[0];
            // The return value may be NULL when, for instance, creating a
            // REVERSE transliterator of ID "Latin-Greek()".
            if (t != null) {
                result.addElement(t);
            }
        }

        // Handle case of idSplitPoint == id.length()
        if (pos >= idSplitPoint && splitTrans != null) {
            splitTransIndex[0] = result.size();
            result.addElement(splitTrans);
            splitTrans = null;
        }

        // Check validity of compound filter position
        if (compoundFilter[0] != null) {
            if ((dir == FORWARD && compoundFilterIndex != 0) ||
                (dir == REVERSE && compoundFilterIndex != result.size())) {
                throw new IllegalArgumentException("Compound filters misplaced in " + id);
            }
        }
    }

    /**
     * Parse a single ID, possibly including an inline filter, and return
     * the resultant transliterator object.  NOTE: If 'create' is false,
     * then the amount of syntax checking is limited.  However, the 'pos'
     * parameter will be updated correctly, assuming the input string is
     * valid.
     *
     * A trailing /;? \s* / is skipped.  The parameter sawDelimiter
     * indicates whether the ';' was seen or not.  Upon return, if pos is
     * advanced, it will either point to a non-whitespace character past
     * the trailing ';', if any, or be equal to length().
     *
     * @param ID the ID string
     * @param regenID regenerated ID, reversed if appropriate, which
     * should be applied to the final created transliterator.  This method
     * will append to this parameter for FORWARD direction and insert
     * addition text at offset 0 for REVERSE direction.  If create is
     * false then this parameter is not used.
     * @param pos INPUT-OUTPUT parameter.  On input, the position of the
     * first character to parse.  On output, the position after the last
     * character parsed.  This will be a semicolon or ID.length().  In the
     * case of an error this value will be unchanged.
     * @param compoundFilter OUTPUT parameter to receive a compound
     * filter, if one is parsed.  When a non-null compound filter is
     * returned then a null Transliterator pointer is returned.
     * @param create if true, create and return the result.  If false,
     * only scan the ID, and return NULL.
     * @return a newly created transliterator, or NULL.  NULL is returned
     * in all cases if create is false.  If create is true, then NULL is
     * returned on error, or if the ID is effectively empty.
     * E.g. "Latin-Greek()" with dir == REVERSE.  Do NOT check for NULL to
     * determine if there was an error.  Instead, check to see if pos
     * moved.
     */
    static Transliterator parseID(String ID,
                                  StringBuffer regenID,
                                  int[] pos,
                                  boolean[] sawDelimiter,
                                  UnicodeSet[] compoundFilter,
                                  int dir,
                                  boolean create) {
        int limit, preDelimLimit,
            revStart, revLimit=0,
            idStart, idLimit,
            setStart, setLimit;

        UnicodeSet[] fwdFilter = new UnicodeSet[1];
        UnicodeSet[] revFilter = new UnicodeSet[1];
        UnicodeSet filter = null;
        int[] indices = new int[4];

        parseIDBounds(ID, pos[0], false, indices, fwdFilter);
        limit = indices[0];
        setStart = indices[1];
        setLimit = indices[2];
        revStart = indices[3];
        filter = fwdFilter[0];

        idStart = pos[0];
        idLimit = limit;

        if (revStart >= 0 && revStart < limit) {
            int revSetStart, revSetLimit;
            parseIDBounds(ID, revStart+1, true, indices, revFilter);
            revLimit = indices[0];
            revSetStart = indices[1];
            revSetLimit = indices[2];
            // we ignore indices[3]

            // revStart points to '('
            if (dir == REVERSE) {
                idStart = revStart+1;
                idLimit = revLimit;
                setStart = revSetStart;
                setLimit = revSetLimit;
                filter = revFilter[0];
            } else {
                idLimit = revStart;
            }
            // assert(revLimit < ID.length() && ID.charAt(revLimit) == ')');
            limit = revLimit+1;
        } else {
            // Ignore () exprs outside of this atomic ID, that is, in
            // "Greek-Latin; Title()", ignore the "()" after Title when
            // parsing Greek-Latin.
            revStart = -1;
        }

        // Advance limit past /\s*;?\s*/
        preDelimLimit = limit;
        limit = skipSpaces(ID, limit);
        sawDelimiter[0] = (limit < ID.length() && ID.charAt(limit) == ID_DELIM);
        if (sawDelimiter[0]) {
            limit = skipSpaces(ID, ++limit);
        }

        // 'id' is the ID with the filter pattern removed and with
        // whitespace deleted.  In a Foo(Bar) ID, id is Foo for FORWARD
        // and Bar for REVERSE.
        String str;
        str = ID.substring(setLimit, idLimit);
        StringBuffer id = new StringBuffer(ID.substring(idStart, setStart));
        id.append(str);

        // Delete whitespace
        int i;
        for (i=0; i<id.length(); ++i) {
            if (UCharacter.isWhitespace(id.charAt(i))) {
                id.deleteCharAt(i);
                --i;
            }
        }

        Transliterator t = null;

        // If id is empty, then we have either an empty specifier,
        // which is illegal, or a compound filter, which is legal
        // as long as its in the right place -- we let the caller
        // decide that.
        boolean isCompoundFilter = (id.length() == 0 && filter != null);
        if (isCompoundFilter) {
            compoundFilter[0] = (dir == REVERSE) ? revFilter[0] : fwdFilter[0];
        }

        else {

            // Normalize the ID.  Take IDs of the form T, T/V, S-T, S-T/V, or S/V-T
            // and produce S-T/V.  If the ID needs to be reversed, do so.  This
            // produces T-S/V, with a default S of "Any".  If the ID has a special
            // non-canonical inverse, look it up (e.g., NFC -> NFD, Null -> Null).
            if (id.length() > 0) { // We handle empty IDs below
                String source = ANY;
                String target = null;
                String variant = ""; // Variant INCLUDING "/"

                String idSTR = id.toString();
                int sep = idSTR.indexOf(ID_SEP);
                int var = idSTR.indexOf(VARIANT_SEP);
                if (var < 0) {
                    var = id.length();
                }
                
                if (sep < 0) {
                    // Form: T/V or T (or /V)
                    target = id.substring(0, var);
                    variant = id.substring(var);
                } else if (sep < var) {
                    // Form: S-T/V or S-T
                    source = id.substring(0, sep++);
                    target = id.substring(sep, var);
                    variant = id.substring(var);
                } else {
                    // Form: S/V-T
                    source = id.substring(0, var);
                    variant = id.substring(var, sep++);
                    target = id.substring(sep);
                }
                id.setLength(0);
                // For forward IDs *or IDs that were part of a Foo(Bar) ID*,
                // normalize them to canonical form.
                if (dir == FORWARD || revStart >= 0) {
                    id.append(source).append(ID_SEP).append(target);
                } else {
                    // Handle special, non-canonical inverse mappings,
                    // e.g. inverse(Any-NFC) = Any-NFD and vice versa.
                    if (source.equals(ANY)) {
                        String inverseTarget = (String) specialInverses.get(
                            new CaseInsensitiveString(target));
                        if (inverseTarget != null) {
                            // If the original ID contained "Any-" then make the
                            // special inverse "Any-Foo"; otherwise make it "Foo".
                            // So "Any-NFC" => "Any-NFD" but "NFC" => "NFD".
                            if (sep < 0) {
                                id.append(inverseTarget);
                            } else {
                                source = inverseTarget;
                                target = ANY;
                            }
                        }
                    }
                    if (id.length() == 0) {
                        id.append(target).append(ID_SEP).append(source);
                    }
                }
                // If the variant is empty ("/") then don't append it
                if (variant.length() > 1) {
                    id.append(variant);
                }
            }
            
            // If we have a reverse part of the ID, e.g., Foo(Bar), then we
            // need to check for an empty part, which represents a Null
            // transliterator.  We return 0 (not a NullTransliterator).  If we
            // are not of the form Foo(Bar) then an empty string is illegal.
            if (revStart >= 0 && id.length() == 0) {
                // Ignore any filters; filters on Null are meaningless (and we
                // can't attach them to 0 anyway)
                filter = null;
            }

            else if (create) {
                StringBuffer s = new StringBuffer();

                t = registry.get(id.toString(), s);

                if (s.length() != 0) {
                    // assert(t==0);
                    // Instantiate an alias
                    t = getInstance(s.toString(), FORWARD);
                }

                if (t == null) {
                    // Creation failed; the ID is invalid or is an alias
                    filter = null;
                    return null;
                }

                // Set the filter, if any.  The transliterator may
                // already have a filter on it so we need to AND any
                // id-based filter together with it.  E.g.,
                // getInstance("[abc] Latin-Foo"), where Latin-Foo is
                // an RBT of "::[:Latin:]; a>A;".
                // getInstance("Latin-Foo") is going to return an RBT
                // with an a [:Latin:] filter, and we need to AND this
                // with [abc].
                t.setFilter(UnicodeFilterLogic.and(filter, t.getFilter()));
            }
        }

        // Set the ID.  This is normally just a substring of the input
        // ID, but for reverse transliterators we need to munge A-B to
        // B-A or Foo(Bar) to Bar(Foo).
        if (dir == FORWARD) {
            id.setLength(0);
            id.append(ID.substring(pos[0], preDelimLimit));
        } else if (isCompoundFilter) {
            // Change [:Foo:] to ([:Foo:]) and vice versa
            id.setLength(0);
            if (revStart < 0) {
                id.append('(').append(ID.substring(setStart, setLimit)).
                    append(')');
            } else {
                id.append(ID.substring(revStart+1, revLimit));
            }
        } else if (revStart < 0) {
            id.insert(0, ID.substring(setStart, setLimit));
        } else {
            // Change Foo(Bar) to Bar(Foo)
            str = ID.substring(pos[0], revStart);
            str = str.trim();
            id.setLength(0);
            id.append(ID.substring(revStart+1, revLimit));
            Utility.trim(id);
            id.append('(').append(str).append(')');
        }
        Utility.trim(id);

        if (t != null) {
            t.setID(id.toString());
        }

        // Regenerate ID of a compound entity
        if (dir == FORWARD) {
            if (regenID.length() != 0) {
                regenID.append(ID_DELIM);
            }
            regenID.append(id);
        } else {
            if (regenID.length() != 0) {
                regenID.insert(0, ID_DELIM);
            }
            regenID.insert(0, id);
        }

        // Indicate success by bumping pos past the final /;?\s*/.
        pos[0] = limit;

        return t;
    }

    /**
     * Internal method used by parseID.  Given a piece of a single ID,
     * find the boundaries of various parts.  For IDs of the form
     * Foo(Bar), this method parses the Foo, then the Bar.  In each piece
     * it locates any inline UnicodeSet pattern [setStart, setLimit)
     * and finds the limit (this will point to either ';' or ')' or
     * ID.length()).
     *
     * @param ID the ID to be parsed
     * @param pos the index of ID at which to start
     * @param withinParens if true, parse the Bar of Foo(Bar), stop at a
     * close paren, and do not look for an open paren.  If true then a
     * close paren MUST be seen or false is returned; if false then the
     * ';' delimiter is optional.
     * @param indices[0] = limit set to the position of ';' or ')' (depending on
     * withinParens), or ID.length() if no delimiter was found
     * @param indices[1] = setStart set to the start of an inline filter pattern,
     * or pos if none
     * @param indices[2] = setLimit set to the limit of an inline filter pattern,
     * or pos if none
     * @param indices[3] = revStart if not withinParens then set to the position of the
     * first '(', which may be > limit; otherwise set to -1
     * @param filter set to a newly created UnicodeSet object for the
     * inline filter pattern, if any; OWNED BY THE CALLER
     *
     * @return true if the pattern is valid, false is there is an invalid
     * UnicodeSet pattern or if withinParens is true and no close paren is
     * seen.
     */
    private static void parseIDBounds(String ID,
                                      int pos,
                                      boolean withinParens,
                                      int[] indices,
                                      UnicodeSet[] filter) {
        int limit;
        int setStart;
        int setLimit;
        int revStart;

        char endDelimiter = withinParens ? ')' : ID_DELIM;
        limit = ID.indexOf(endDelimiter, pos);
        if (limit < 0) {
            if (withinParens) {
                //return false;
                throw new IllegalArgumentException("Missing closing parenthesis in " + ID);
            }
            limit = ID.length();
        }
        setStart = ID.indexOf('[', pos);
        revStart = withinParens ? -1 : ID.indexOf('(', pos);

        if (setStart >= 0 && setStart < limit &&
            (revStart < 0 || setStart < revStart)) {
            ParsePosition ppos = new ParsePosition(setStart);
            // TODO Improve performance by scanning the UnicodeSet pattern
            // without actually constructing it, if create is false.  That
            // is, create a method like this one for UnicodeSet.
            filter[0] = new UnicodeSet();
            filter[0].applyPattern(ID, ppos, null, true);
            setLimit = ppos.getIndex();
            if (limit < setLimit) {
                limit = ID.indexOf(endDelimiter, setLimit);
                if (limit < 0) {
                    if (withinParens) {
                        //return false;
                        throw new IllegalArgumentException("Missing closing parenthesis in " + ID);
                    }
                    limit = ID.length();
                }
            }
            if (revStart >= 0 && revStart < setLimit) {
                revStart = ID.indexOf(')', setLimit);
            }
        } else {
            setStart = setLimit = pos;
        }
        indices[0] = limit;
        indices[1] = setStart;
        indices[2] = setLimit;
        indices[3] = revStart;
    }

    /**
     * If pos is the index of a space in str, then advance it over that
     * space and any immediately subsequent ones.
     */
    private static int skipSpaces(String str,
                                  int pos) {
        while (pos < str.length() &&
               UCharacter.isWhitespace(str.charAt(pos))) {
            ++pos;
        }
        return pos;
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
        registry.put(ID, transClass, true);
        if (displayName != null) {
            displayNameCache.put(new CaseInsensitiveString(ID), displayName);
        }
    }

    /**
     * Register a factory object with the given ID.  The factory
     * method should return a new instance of the given transliterator.
     * @param ID the ID of this transliterator
     * @param factory the factory object
     */
    public static void registerFactory(String ID, Factory factory) {
        registry.put(ID, factory, true);
    }

    /**
     * Register two targets as being inverses of one another.  For
     * example, calling registerSpecialInverses("NFC", "NFD") causes
     * Transliterator to form the following inverse relationships:
     *
     * <pre>NFC => NFD
     * Any-NFC => Any-NFD
     * NFD => NFC
     * Any-NFD => Any-NFC</pre>
     *
     * (Without the special inverse registration, the inverse of NFC
     * would be NFC-Any.)  Note that NFD is shorthand for Any-NFD, but
     * that the presence or absence of "Any-" is preserved.
     *
     * <p>The relationship is symmetrical; registering (a, b) is
     * equivalent to registering (b, a).
     *
     * <p>The relevant IDs must still be registered separately as
     * factories or classes.
     *
     * <p>Only the targets are specified.  Special inverses always
     * have the form Any-Target1 <=> Any-Target2.  The target should
     * have canonical casing (the casing desired to be produced when
     * an inverse is formed) and should contain no whitespace or other
     * extraneous characters.
     */
    public static void registerSpecialInverses(String target1, String target2) {
        specialInverses.put(new CaseInsensitiveString(target1), target2);
        if (!target1.equalsIgnoreCase(target2)) {
            specialInverses.put(new CaseInsensitiveString(target2), target1);
        }
    }

    /**
     * Unregisters a transliterator or class.  This may be either
     * a system transliterator or a user transliterator or class.
     *
     * @param ID the ID of the transliterator or class
     * @see #registerClass
     */
    public static void unregister(String ID) {
        displayNameCache.remove(new CaseInsensitiveString(ID));
        registry.remove(ID);
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
        return registry.getAvailableIDs();
    }

    public static final Enumeration getAvailableSources() {
        return registry.getAvailableSources();
    }

    public static final Enumeration getAvailableTargets(String source) {
        return registry.getAvailableTargets(source);
    }

    public static final Enumeration getAvailableVariants(String source,
                                                         String target) {
        return registry.getAvailableVariants(source, target);
    }

    /**
     * Method for subclasses to use to obtain a character in the given
     * string, with filtering.  If the character at the given offset
     * is excluded by this transliterator's filter, then U+FFFE is returned.
     *
     * <p><b>Note:</b> Most subclasses that implement
     * handleTransliterator() will <em>not</em> want to use this
     * method, since characters they see are already filtered.  Only
     * subclasses with special requirements, such as those overriding
     * filteredTransliterate(), should need this method.
     *
     * @deprecated the new architecture provides filtering at the top
     * level.  This method will be removed Dec 31 2001.
     */
    protected char filteredCharAt(Replaceable text, int i) {
        char c;
        UnicodeFilter filter = getFilter();
        return (filter == null) ? text.charAt(i) :
            (filter.contains(c = text.charAt(i)) ? c : '\uFFFE');
    }

    static {
        registry = new TransliteratorRegistry();

        // The display name cache starts out empty
        displayNameCache = new Hashtable();

        // Read the index file and populate the registry.
        // Each line of the index file is either blank, a '#' comment,
        // or a colon-delimited line.  In the latter case the first
        // field is the ID being defined.  The second field is one of
        // three strings: "file", "internal", or "alias".  Remaining
        // fields vary according the value fo the second field.  See
        // the index file itself for further documentation.
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
                //                pos       colon      c2
                colon = line.indexOf(':', pos);
                int c2 = line.indexOf(':', colon+1);
                int dir = line.substring(c2+1).equals("FORWARD") ?
                    FORWARD :  REVERSE;
                registry.put(ID,
                             line.substring(pos, colon), // resource
                             line.substring(colon+1, c2), // encoding
                             dir,
                             !type.equals("internal"));
            } else if (type.equals("alias")) {
                // Rest of line is the <getInstanceArg>
                registry.put(ID, line.substring(pos), true);
            } else {
                // Unknown type
                throw new RuntimeException("Can't parse line: " + line);
            }
        }

        specialInverses = new Hashtable();
        registerSpecialInverses(NullTransliterator.SHORT_ID, NullTransliterator.SHORT_ID);

        // Register non-rule-based transliterators
        registerClass(HexToUnicodeTransliterator._ID,
                      HexToUnicodeTransliterator.class, null);
        registerClass(UnicodeToHexTransliterator._ID,
                      UnicodeToHexTransliterator.class, null);
        registerClass(NullTransliterator._ID,
                      NullTransliterator.class, null);
        registerClass(RemoveTransliterator._ID,
                      RemoveTransliterator.class, null);
        LowercaseTransliterator.register();
        UppercaseTransliterator.register();
        TitlecaseTransliterator.register();
        UnicodeNameTransliterator.register();
        NameUnicodeTransliterator.register();
        NormalizationTransliterator.register();
    }

    /**
     * The factory interface for transliterators.  Transliterator
     * subclasses can register factory objects for IDs using the
     * registerFactory() method of Transliterator.  When invoked, the
     * factory object will be passed the ID being instantiated.  This
     * makes it possible to register one factory method to more than
     * one ID, or for a factory method to parameterize its result
     * based on the variant.
     */
    public static interface Factory {
        Transliterator getInstance(String ID);
    }
}
