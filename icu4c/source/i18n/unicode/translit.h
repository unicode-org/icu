/*
* Copyright (C) 1999-2001, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef TRANSLIT_H
#define TRANSLIT_H

#include "unicode/unistr.h"
#include "unicode/parseerr.h"
#include "unicode/utrans.h" // UTransPosition, UTransDirection

class Replaceable;
class UnicodeFilter;
class UnicodeSet;
class TransliterationRuleData;
class Hashtable;
class U_I18N_API UVector;
class CompoundTransliterator;

U_CFUNC UBool transliterator_cleanup();

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
 * <code>transliterate()</code>.  (However, this does <em>not</em>
 * mean that threads may share transliterators without synchronizing
 * them.  Transliterators are not immutable, so they must be
 * synchronized when shared between threads.)  This1 might seem to
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
 * <code>getAvailableIDs()</code>.  Since transliterators are mutable,
 * multiple calls to <code>getInstance()</code> with the same ID will
 * return distinct objects.
 *
 * <p>In addition to the system transliterators registered at startup,
 * user transliterators may be registered by calling
 * <code>registerInstance()</code> at run time.  A registered instance
 * acts a template; future calls to <tt>getInstance()</tt> with the ID
 * of the registered object return clones of that object.  Thus any
 * object passed to <tt>registerInstance()</tt> must implement
 * <tt>clone()</tt> propertly.  To register a transliterator subclass
 * without instantiating it (until it is needed), users may call
 * <code>registerClass()</code>.  In this case, the objects are
 * instantiated by invoking the zero-argument public constructor of
 * the class.
 *
 * <p><b>Subclassing</b>
 *
 * Subclasses must implement the abstract method
 * <code>handleTransliterate()</code>.  <p>Subclasses should override
 * the <code>transliterate()</code> method taking a
 * <code>Replaceable</code> and the <code>transliterate()</code>
 * method taking a <code>String</code> and <code>StringBuffer</code>
 * if the performance of these methods can be improved over the
 * performance obtained by the default implementations in this class.
 *
 * @author Alan Liu
 * @draft
 */
class U_I18N_API Transliterator {

private:

    /**
     * Programmatic name, e.g., "Latin-Arabic".
     */
    UnicodeString ID;

    /**
     * This transliterator's filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    UnicodeFilter* filter;

    int32_t maximumContextLength;

 public:

    /**
     * A function that creates and returns a Transliterator.
     */
    typedef Transliterator* (*Factory)(void);

protected:

    /**
     * Default constructor.
     * @param ID the string identifier for this transliterator
     * @param adoptedFilter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    Transliterator(const UnicodeString& ID, UnicodeFilter* adoptedFilter);

    /**
     * Copy constructor.
     */
    Transliterator(const Transliterator&);

    /**
     * Assignment operator.
     */
    Transliterator& operator=(const Transliterator&);
    
    /**
     * Internal factory method.
     */
    static Transliterator* createInstance(const UnicodeString& ID,
                                          UTransDirection dir,
                                          int32_t idSplitPoint,
                                          Transliterator *adoptedSplitTrans,
                                          UParseError& parseError,
                                          UErrorCode& status);
    
    /**
     * Internal parsing method.
     */
    static void parseCompoundID(const UnicodeString& ID,
                                UnicodeString& regenID,
                                UTransDirection dir,
                                int32_t idSplitPoint,
                                Transliterator *adoptedSplitTrans,
                                UVector& result,
                                int32_t& splitTransIndex,
                                UParseError& parseError,
                                UErrorCode& status);
    /**
     * Internal parsing method for subclasses.
     */
    static Transliterator* parseID(const UnicodeString& ID,
                                   UnicodeString& regenID,
                                   int32_t& pos,
                                   UBool& sawDelimiter,
                                   UTransDirection dir,
                                   UParseError& parseError,
                                   UBool create,
                                   UErrorCode& status);

    /**
     * Internal parsing method for parseID.
     */
    static UBool parseIDBounds(const UnicodeString& ID,
                               int32_t pos,
                               UBool withinParens,
                               int32_t& limit,
                               int32_t& setStart,
                               int32_t& setLimit,
                               int32_t& revStart,
                               UnicodeSet*& filter);

    static void skipSpaces(const UnicodeString& str,
                           int32_t& pos);

    friend class TransliteratorParser; // for parseID()

public:

    /**
     * Destructor.
     * @draft
     */
    virtual ~Transliterator();

    /**
     * Implements Cloneable.
     * All subclasses are encouraged to implement this method if it is
     * possible and reasonable to do so.  Subclasses that are to be
     * registered with the system using <tt>registerInstance()<tt>
     * are required to implement this method.  If a subclass does not
     * implement clone() properly and is registered with the system
     * using registerInstance(), then the default clone() implementation
     * will return null, and calls to createInstance() will fail.
     *
     * @see #registerInstance
     * @draft
     */
    virtual Transliterator* clone() const { return 0; }

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
     * @draft
     */
    virtual int32_t transliterate(Replaceable& text,
                                  int32_t start, int32_t limit) const;

    /**
     * Transliterates an entire string in place. Convenience method.
     * @param text the string to be transliterated
     * @draft
     */
    virtual void transliterate(Replaceable& text) const;

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly after new text has been inserted,
     * typically as a result of a keyboard event.  The new text in
     * <code>insertion</code> will be inserted into <code>text</code>
     * at <code>index.limit</code>, advancing
     * <code>index.limit</code> by <code>insertion.length()</code>.
     * Then the transliterator will try to transliterate characters of
     * <code>text</code> between <code>index.cursor</code> and
     * <code>index.limit</code>.  Characters before
     * <code>index.cursor</code> will not be changed.
     *
     * <p>Upon return, values in <code>index</code> will be updated.
     * <code>index.start</code> will be advanced to the first
     * character that future calls to this method will read.
     * <code>index.cursor</code> and <code>index.limit</code> will
     * be adjusted to delimit the range of text that future calls to
     * this method may change.
     *
     * <p>Typical usage of this method begins with an initial call
     * with <code>index.start</code> and <code>index.limit</code>
     * set to indicate the portion of <code>text</code> to be
     * transliterated, and <code>index.cursor == index.start</code>.
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
     * @param index an array of three integers.
     *
     * <ul><li><code>index.start</code>: the beginning index,
     * inclusive; <code>0 <= index.start <= index.limit</code>.
     *
     * <li><code>index.limit</code>: the ending index, exclusive;
     * <code>index.start <= index.limit <= text.length()</code>.
     * <code>insertion</code> is inserted at
     * <code>index.limit</code>.
     *
     * <li><code>index.cursor</code>: the next character to be
     * considered for transliteration; <code>index.start <=
     * index.cursor <= index.limit</code>.  Characters before
     * <code>index.cursor</code> will not be changed by future calls
     * to this method.</ul>
     *
     * @param insertion text to be inserted and possibly
     * transliterated into the translation buffer at
     * <code>index.limit</code>.  If <code>null</code> then no text
     * is inserted.
     * @see #handleTransliterate
     * @exception IllegalArgumentException if <code>index</code>
     * is invalid
     * @draft
     */
    virtual void transliterate(Replaceable& text, UTransPosition& index,
                               const UnicodeString& insertion,
                               UErrorCode& status) const;

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly after a new character has been
     * inserted, typically as a result of a keyboard event.  This is a
     * convenience method; see {@link
     * #transliterate(Replaceable, int[], String)} for details.
     * @param text the buffer holding transliterated and
     * untransliterated text
     * @param index an array of three integers.  See {@link
     * #transliterate(Replaceable, int[], String)}.
     * @param insertion text to be inserted and possibly
     * transliterated into the translation buffer at
     * <code>index.limit</code>.
     * @see #transliterate(Replaceable, int[], String)
     * @draft
     */
    virtual void transliterate(Replaceable& text, UTransPosition& index,
                               UChar32 insertion,
                               UErrorCode& status) const;

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly.  This is a convenience method; see
     * {@link #transliterate(Replaceable, int[], String)} for
     * details.
     * @param text the buffer holding transliterated and
     * untransliterated text
     * @param index an array of three integers.  See {@link
     * #transliterate(Replaceable, int[], String)}.
     * @see #transliterate(Replaceable, int[], String)
     * @draft
     */
    virtual void transliterate(Replaceable& text, UTransPosition& index,
                               UErrorCode& status) const;

    /**
     * Finishes any pending transliterations that were waiting for
     * more characters.  Clients should call this method as the last
     * call after a sequence of one or more calls to
     * <code>transliterate()</code>.
     * @param text the buffer holding transliterated and
     * untransliterated text.
     * @param index the array of indices previously passed to {@link
     * #transliterate}
     * @draft
     */
    virtual void finishTransliteration(Replaceable& text,
                                       UTransPosition& index) const;

private:

    /**
     * This internal method does incremental transliteration.  If the
     * 'insertion' is non-null then we append it to 'text' before
     * proceeding.  This method calls through to the pure virtual
     * framework method handleTransliterate() to do the actual
     * work.
     */
    void _transliterate(Replaceable& text,
                        UTransPosition& index,
                        const UnicodeString* insertion,
                        UErrorCode &status) const;

protected:

    /**
     * Abstract method that concrete subclasses define to implement
     * keyboard transliteration.  This method should transliterate all
     * characters between <code>index.cursor</code> and
     * <code>index.limit</code> that can be unambiguously
     * transliterated, regardless of future insertions of text at
     * <code>index.limit</code>.  <code>index.cursor</code> should
     * be advanced past committed characters (those that will not
     * change in future calls to this method).
     * <code>index.limit</code> should be updated to reflect text
     * replacements that shorten or lengthen the text between
     * <code>index.cursor</code> and <code>index.limit</code>.  Upon
     * return, neither <code>index.cursor</code> nor
     * <code>index.limit</code> should be less than the initial value
     * of <code>index.cursor</code>.  <code>index.start</code>
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
     * @param index an array of three integers.  See {@link
     * #transliterate(Replaceable, int[], String)}.
     * @see #transliterate
     */
    virtual void handleTransliterate(Replaceable& text,
                                     UTransPosition& index,
                                     UBool incremental) const = 0;

    /**
     * Core transliteration method called by all other methods in
     * Tranliterator.  This method splits up the input text into
     * segments of unfiltered text and passes those to
     * handleTransliterate().  For most subclasses this is convenient
     * and efficient.  Subclasses that can more efficiently handle the
     * filter logic on their own (rare) can override
     * filteredTransliterate().  Such subclasses must still implement
     * handleTransliterate() but they can do so with an empty body,
     * since filteredTransliterate() is the only method that calls
     * handleTransliterate().
     */
    virtual void filteredTransliterate(Replaceable& text,
                                       UTransPosition& index,
                                       UBool incremental) const;

    friend class CompoundTransliterator; // for filteredTransliterate

public:

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
     * @draft
     */
    int32_t getMaximumContextLength(void) const;

protected:

    /**
     * Method for subclasses to use to set the maximum context length.
     * @see #getMaximumContextLength
     */
    void setMaximumContextLength(int32_t maxContextLength);

public:

    /**
     * Returns a programmatic identifier for this transliterator.
     * If this identifier is passed to <code>getInstance()</code>, it
     * will return this object, if it has been registered.
     * @see #registerInstance
     * @see #registerClass
     * @see #getAvailableIDs
     * @draft
     */
    virtual const UnicodeString& getID(void) const;

    /**
     * Returns a name for this transliterator that is appropriate for
     * display to the user in the default locale.  See {@link
     * #getDisplayName(Locale)} for details.
     * @draft
     */
    static UnicodeString& getDisplayName(const UnicodeString& ID,
                                         UnicodeString& result);

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
     * @draft
     */
    static UnicodeString& getDisplayName(const UnicodeString& ID,
                                         const Locale& inLocale,
                                         UnicodeString& result);

    /**
     * Returns the filter used by this transliterator, or <tt>NULL</tt>
     * if this transliterator uses no filter.
     * @draft
     */
    const UnicodeFilter* getFilter(void) const;

    /**
     * Returns the filter used by this transliterator, or <tt>NULL</tt> if this
     * transliterator uses no filter.  The caller must eventually delete the
     * result.  After this call, this transliterator's filter is set to
     * <tt>NULL</tt>.  Calls adoptFilter().
     */
    UnicodeFilter* orphanFilter(void);

    /**
     * Changes the filter used by this transliterator.  If the filter
     * is set to <tt>null</tt> then no filtering will occur.
     *
     * <p>Callers must take care if a transliterator is in use by
     * multiple threads.  The filter should not be changed by one
     * thread while another thread may be transliterating.
     * @draft
     */
    virtual void adoptFilter(UnicodeFilter* adoptedFilter);

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
     * @draft
     */
    Transliterator* createInverse(UErrorCode& status) const;

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
     * @draft
     */
    static Transliterator* createInstance(const UnicodeString& ID,
                                          UTransDirection dir,
                                          UParseError& parseError,
                                          UErrorCode& status);

    static Transliterator* createInstance(const UnicodeString& ID,
                                          UTransDirection dir,
                                          UErrorCode& status);
    /**
     * Returns a <code>Transliterator</code> object constructed from
     * the given rule string.  This will be a RuleBasedTransliterator,
     * if the rule string contains only rules, or a
     * CompoundTransliterator, if it contains ID blocks, or a
     * NullTransliterator, if it contains ID blocks which parse as
     * empty for the given direction.
     */
    static Transliterator* createFromRules(const UnicodeString& ID,
                                           const UnicodeString& rules,
                                           UTransDirection dir,
                                           UParseError& parseError,
                                           UErrorCode& status);

    /**
     * Create a rule string that can be passed to createFromRules()
     * to recreate this transliterator.
     * @param result the string to receive the rules.  Previous
     * contents will be deleted.
     * @param escapeUnprintable if TRUE then convert unprintable
     * character to their hex escape representations, \uxxxx or
     * \Uxxxxxxxx.  Unprintable characters are those other than
     * U+000A, U+0020..U+007E.
     */
    virtual UnicodeString& toRules(UnicodeString& result,
                                   UBool escapeUnprintable) const;

public:

    /**
     * Registers a factory function that creates transliterators of
     * a given ID.
     * @param id the ID being registered
     * @param factory a function pointer that will be copied and
     * called later when the given ID is passed to createInstance()
     */
    static void registerFactory(const UnicodeString& id,
                                Factory factory);

    /**
     * Registers a instance <tt>obj</tt> of a subclass of
     * <code>Transliterator</code> with the system.  When
     * <tt>createInstance()</tt> is called with an ID string that is
     * equal to <tt>obj->getID()</tt>, then <tt>obj->clone()</tt> is
     * returned.
     *
     * After this call the Transliterator class owns the adoptedObj
     * and will delete it.
     *
     * @param obj an instance of subclass of
     * <code>Transliterator</code> that defines <tt>clone()</tt>
     * @see #getInstance
     * @see #registerClass
     * @see #unregister
     * @draft
     */
    static void registerInstance(Transliterator* adoptedObj);

private:

    friend class NormalizationTransliterator;

    static void _registerFactory(const UnicodeString& id,
                                 Factory factory);

public:

    /**
     * Unregisters a transliterator or class.  This may be either
     * a system transliterator or a user transliterator or class.
     *
     * @param ID the ID of the transliterator or class
     * @return the <code>Object</code> that was registered with
     * <code>ID</code>, or <code>null</code> if none was
     * @see #registerInstance
     * @see #registerClass
     * @draft
     */
    static void unregister(const UnicodeString& ID);

public:

    /**
     * Return the number of IDs currently registered with the system.
     * To retrieve the actual IDs, call getAvailableID(i) with
     * i from 0 to countAvailableIDs() - 1.
     * @draft
     */
    static int32_t countAvailableIDs(void);

    /**
     * Return the index-th available ID.  index must be between 0
     * and countAvailableIDs() - 1, inclusive.  If index is out of
     * range, the result of getAvailableID(0) is returned.
     * @draft
     */
    static const UnicodeString& getAvailableID(int32_t index);

    /**
     * Return the number of registered source specifiers.
     */
    static int32_t countAvailableSources(void);
    
    /**
     * Return a registered source specifier.
     * @param index which specifier to return, from 0 to n-1, where
     * n = countAvailableSources()
     * @param result fill-in paramter to receive the source specifier.
     * If index is out of range, result will be empty.
     * @return reference to result
     */
    static UnicodeString& getAvailableSource(int32_t index,
                                             UnicodeString& result);
    
    /**
     * Return the number of registered target specifiers for a given
     * source specifier.
     */
    static int32_t countAvailableTargets(const UnicodeString& source);
    
    /**
     * Return a registered target specifier for a given source.
     * @param index which specifier to return, from 0 to n-1, where
     * n = countAvailableTargets(source)
     * @param source the source specifier
     * @param result fill-in paramter to receive the target specifier.
     * If source is invalid or if index is out of range, result will
     * be empty.
     * @return reference to result
     */
    static UnicodeString& getAvailableTarget(int32_t index,
                                             const UnicodeString& source,
                                             UnicodeString& result);
    
    /**
     * Return the number of registered variant specifiers for a given
     * source-target pair.
     */
    static int32_t countAvailableVariants(const UnicodeString& source,
                                          const UnicodeString& target);
    
    /**
     * Return a registered variant specifier for a given source-target
     * pair.
     * @param index which specifier to return, from 0 to n-1, where
     * n = countAvailableVariants(source, target)
     * @param source the source specifier
     * @param target the target specifier
     * @param result fill-in paramter to receive the variant
     * specifier.  If source is invalid or if target is invalid or if
     * index is out of range, result will be empty.
     * @return reference to result
     */
    static UnicodeString& getAvailableVariant(int32_t index,
                                              const UnicodeString& source,
                                              const UnicodeString& target,
                                              UnicodeString& result);

    /**
     * Return the class ID for this class.  This is useful only for
     * comparing to a return value from getDynamicClassID().  For example:
     * <pre>
     * .      Base* polymorphic_pointer = createPolymorphicObject();
     * .      if (polymorphic_pointer->getDynamicClassID() ==
     * .          Derived::getStaticClassID()) ...
     * </pre>
     * @return          The class ID for all objects of this class.
     * @stable
     */
    static UClassID getStaticClassID(void) { return (UClassID)&fgClassID; }

    /**
     * Returns a unique class ID <b>polymorphically</b>.  This method
     * is to implement a simple version of RTTI, since not all C++
     * compilers support genuine RTTI.  Polymorphic operator==() and
     * clone() methods call this method.
     * 
     * <p>Concrete subclasses of Transliterator that wish clients to
     * be able to identify them should implement getDynamicClassID()
     * and also a static method and data member:
     * 
     * <pre>
     * static UClassID getStaticClassID() { return (UClassID)&fgClassID; }
     * static char fgClassID;
     * </pre>
     *
     * Subclasses that do not implement this method will have a
     * dynamic class ID of Transliterator::getStatisClassID().
     *
     * @return The class ID for this object. All objects of a given
     * class have the same class ID.  Objects of other classes have
     * different class IDs.
     */
    virtual UClassID getDynamicClassID(void) const { return getStaticClassID(); };

private:

    /**
     * Class identifier for subclasses of Transliterator that do not
     * define their class (anonymous subclasses).
     */
    static char fgClassID;

protected:

    /**
     * Method for subclasses to use to obtain a character in the given
     * string, with filtering.  If the character at the given offset
     * is excluded by this transliterator's filter, then U+FFFE is returned.
     * @deprecated the new architecture provides filtering at the top
     * level.  This method will be removed Dec 31 2001.
     */
    UChar filteredCharAt(const Replaceable& text, int32_t i) const;

    /**
     * Set the ID of this transliterators.  Subclasses shouldn't do
     * this, unless the underlying script behavior has changed.
     */
    void setID(const UnicodeString& id);

private:
    static void initializeRegistry(void);
};

inline int32_t Transliterator::getMaximumContextLength(void) const {
    return maximumContextLength;
}

inline void Transliterator::setID(const UnicodeString& id) {
    ID = id;
}

#endif
