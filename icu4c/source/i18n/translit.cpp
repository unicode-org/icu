/*
**********************************************************************
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "cmemory.h"
#include "cstring.h"
#include "hash.h"
#include "mutex.h"
#include "rbt_data.h"
#include "rbt_pars.h"
#include "transreg.h"
#include "unicode/cpdtrans.h"
#include "unicode/hangjamo.h"
#include "unicode/hextouni.h"
#include "unicode/jamohang.h"
#include "unicode/locid.h"
#include "unicode/msgfmt.h"
#include "unicode/name2uni.h"
#include "unicode/nortrans.h"
#include "unicode/nultrans.h"
#include "unicode/putil.h"
#include "unicode/rep.h"
#include "unicode/remtrans.h"
#include "unicode/resbund.h"
#include "unicode/titletrn.h"
#include "unicode/tolowtrn.h"
#include "unicode/toupptrn.h"
#include "unicode/translit.h"
#include "unicode/uni2name.h"
#include "unicode/unicode.h"
#include "unicode/unifilt.h"
#include "unicode/uniset.h"
#include "unicode/unitohex.h"
#include "unicode/uscript.h"

// keep in sync with CompoundTransliterator
static const UChar ID_SEP      = 0x002D; /*-*/
static const UChar ID_DELIM    = 0x003B; /*;*/
static const UChar VARIANT_SEP = 0x002F; // '/'
static const UChar OPEN_PAREN  = 40;
static const UChar CLOSE_PAREN = 41;

/**
 * The mutex controlling access to registry object.
 */
static UMTX registryMutex = 0;

/**
 * System transliterator registry; non-null when initialized.
 */
static TransliteratorRegistry* registry = 0;

/**
 * Prefix for resource bundle key for the display name for a
 * transliterator.  The ID is appended to this to form the key.
 * The resource bundle value should be a String.
 */
static const char* RB_DISPLAY_NAME_PREFIX = "%Translit%%";

/**
 * Prefix for resource bundle key for the display name for a
 * transliterator SCRIPT.  The ID is appended to this to form the key.
 * The resource bundle value should be a String.
 */
static const char* RB_SCRIPT_DISPLAY_NAME_PREFIX = "%Translit%";

/**
 * Resource bundle key for display name pattern.
 * The resource bundle value should be a String forming a
 * MessageFormat pattern, e.g.:
 * "{0,choice,0#|1#{1} Transliterator|2#{1} to {2} Transliterator}".
 */
static const char* RB_DISPLAY_NAME_PATTERN = "TransliteratorNamePattern";

/**
 * Resource bundle key for the list of RuleBasedTransliterator IDs.
 * The resource bundle value should be a String[] with each element
 * being a valid ID.  The ID will be appended to RB_RULE_BASED_PREFIX
 * to obtain the class name in which the RB_RULE key will be sought.
 */
static const char* RB_RULE_BASED_IDS = "RuleBasedTransliteratorIDs";

/**
 * Class identifier for subclasses of Transliterator that do not
 * define their class (anonymous subclasses).
 */
char Transliterator::fgClassID = 0; // Value is irrelevant

/**
 * Default constructor.
 * @param theID the string identifier for this transliterator
 * @param theFilter the filter.  Any character for which
 * <tt>filter.contains()</tt> returns <tt>FALSE</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 */
Transliterator::Transliterator(const UnicodeString& theID,
                               UnicodeFilter* adoptedFilter) :
    ID(theID), filter(adoptedFilter),
    maximumContextLength(0) {}

/**
 * Destructor.
 */
Transliterator::~Transliterator() {
    delete filter;
}

/**
 * Copy constructor.
 */
Transliterator::Transliterator(const Transliterator& other) :
    ID(other.ID), filter(0),
    maximumContextLength(other.maximumContextLength) {
    if (other.filter != 0) {
        // We own the filter, so we must have our own copy
        filter = (UnicodeFilter*) other.filter->clone();
    }
}

/**
 * Assignment operator.
 */
Transliterator& Transliterator::operator=(const Transliterator& other) {
    ID = other.ID;
    maximumContextLength = other.maximumContextLength;
    // MUST go through adoptFilter in case latter is overridden
    adoptFilter((other.filter == 0) ? 0 : (UnicodeFilter*) other.filter->clone());
    return *this;
}

/**
 * Transliterates a segment of a string.  <code>Transliterator</code> API.
 * @param text the string to be transliterated
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @return the new limit index
 */
int32_t Transliterator::transliterate(Replaceable& text,
                                      int32_t start, int32_t limit) const {

    UTransPosition offsets;
    offsets.contextStart= start;
    offsets.contextLimit = limit;
    offsets.start = start;
    offsets.limit = limit;
    filteredTransliterate(text, offsets, FALSE);
    return offsets.limit;
}

/**
 * Transliterates an entire string in place. Convenience method.
 * @param text the string to be transliterated
 */
void Transliterator::transliterate(Replaceable& text) const {
    transliterate(text, 0, text.length());
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
 * #finishKeyboardTransliteration} after the last call to this
 * method has been made.
 * 
 * @param text the buffer holding transliterated and untransliterated text
 * @param index an array of three integers.
 *
 * <ul><li><code>index.contextStart</code>: the beginning index,
 * inclusive; <code>0 <= index.contextStart <= index.contextLimit</code>.
 *
 * <li><code>index.contextLimit</code>: the ending index, exclusive;
 * <code>index.contextStart <= index.contextLimit <= text.length()</code>.
 * <code>insertion</code> is inserted at
 * <code>index.contextLimit</code>.
 *
 * <li><code>index.start</code>: the next character to be
 * considered for transliteration; <code>index.contextStart <=
 * index.start <= index.contextLimit</code>.  Characters before
 * <code>index.start</code> will not be changed by future calls
 * to this method.</ul>
 *
 * @param insertion text to be inserted and possibly
 * transliterated into the translation buffer at
 * <code>index.contextLimit</code>.  If <code>null</code> then no text
 * is inserted.
 * @see #START
 * @see #LIMIT
 * @see #CURSOR
 * @see #handleTransliterate
 * @exception IllegalArgumentException if <code>index</code>
 * is invalid
 */
void Transliterator::transliterate(Replaceable& text,
                                   UTransPosition& index,
                                   const UnicodeString& insertion,
                                   UErrorCode &status) const {
    _transliterate(text, index, &insertion, status);
}

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
 * <code>index.contextLimit</code>.
 * @see #transliterate(Replaceable, int[], String)
 */
void Transliterator::transliterate(Replaceable& text,
                                   UTransPosition& index,
                                   UChar32 insertion,
                                   UErrorCode& status) const {
    UnicodeString str(insertion);
    if (UTF_IS_LEAD(insertion)) {
        // Oops, the caller passed us a single lead surrogate.  In
        // general, we don't support this, but we'll do the caller a
        // favor in the special case of LEAD followed by TRAIL
        // insertion.  Anything else won't work.
        text.handleReplaceBetween(index.limit, index.limit, str);
        ++index.limit;
        ++index.contextLimit;
    } else {
        _transliterate(text, index, &str, status);
    }
}

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
 */
void Transliterator::transliterate(Replaceable& text,
                                   UTransPosition& index,
                                   UErrorCode& status) const {
    _transliterate(text, index, 0, status);
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
void Transliterator::finishTransliteration(Replaceable& text,
                                           UTransPosition& index) const {
    filteredTransliterate(text, index, FALSE);
}

/**
 * This internal method does keyboard transliteration.  If the
 * 'insertion' is non-null then we append it to 'text' before
 * proceeding.  This method calls through to the pure virtual
 * framework method handleTransliterate() to do the actual
 * work.
 */
void Transliterator::_transliterate(Replaceable& text,
                                    UTransPosition& index,
                                    const UnicodeString* insertion,
                                    UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return;
    }

    if (index.contextStart < 0 ||
        index.contextLimit > text.length() ||
        index.start < index.contextStart ||
        index.start > index.contextLimit) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

//    int32_t originalStart = index.contextStart;
    if (insertion != 0) {
        text.handleReplaceBetween(index.limit, index.limit, *insertion);
        index.limit += insertion->length();
        index.contextLimit += insertion->length();
    }

    filteredTransliterate(text, index, TRUE);

#if 0
    // I CAN'T DO what I'm attempting below now that the Kleene star
    // operator is supported.  For example, in the rule

    //   ([:Lu:]+) { x } > $1;

    // what is the maximum context length?  getMaximumContextLength()
    // will return 1, but this is just the length of the ante context
    // part of the pattern string -- 1 character, which is a standin
    // for a Quantifier, which contains a StringMatcher, which
    // contains a UnicodeSet.

    // There is a complicated way to make this work again, and that's
    // to add a "maximum left context" protocol into the
    // UnicodeMatcher hierarchy.  At present I'm not convinced this is
    // worth it.

    // ---

    // The purpose of the code below is to keep the context small
    // while doing incremental transliteration.  When part of the left
    // context (between contextStart and start) is no longer needed,
    // we try to advance contextStart past that portion.  We use the
    // maximum context length to do so.
    int32_t newCS = index.start;
    int32_t n = getMaximumContextLength();
    while (newCS > originalStart && n-- > 0) {
        --newCS;
        newCS -= UTF_CHAR_LENGTH(text.char32At(newCS)) - 1;
    }
    index.contextStart = uprv_max(newCS, originalStart);
#endif
}

/**
 * This method breaks up the input text into runs of unfiltered
 * characters.  It passes each such run to
 * <subclass>.handleTransliterate().  Subclasses that can handle the
 * filter logic more efficiently themselves may override this method.
 *
 * All transliteration calls in this class go through this method.
 */
void Transliterator::filteredTransliterate(Replaceable& text,
                                           UTransPosition& index,
                                           UBool incremental) const {
    if (filter == 0) {
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
    int32_t globalLimit = index.limit;

    // Break the input text up.  Say the input text has the form:
    //   xxxabcxxdefxx
    // where 'x' represents a filtered character.  Then we break this
    // up into:
    //   xxxabc xxdef xx
    // Each pass through the loop consumes a run of filtered
    // characters (which are ignored) and a subsequent run of
    // unfiltered characters (which are transliterated).  If, at any
    // point, we fail to consume our entire segment, we stop.
    do {
        // Narrow the range to be transliterated to the first segment
        // of unfiltered characters at or after index.start.

        UChar32 c;

        // Advance compoundStart past filtered chars
        while (index.start < globalLimit &&
               !filter->contains(c=text.char32At(index.start))) {
            index.start += UTF_CHAR_LENGTH(c);
        }

        // Find the end of this run of unfiltered chars
        index.limit = index.start;
        while (index.limit < globalLimit &&
               filter->contains(c=text.char32At(index.limit))) {
            index.limit += UTF_CHAR_LENGTH(c);
        }

        // Check to see if the unfiltered run is empty.  This only
        // happens at the end of the string when all the remaining
        // characters are filtered.
        if (index.limit == index.start) {
            // assert(index.start == globalLimit);
            break;
        }

        int32_t limit = index.limit;

        // Delegate to subclass for actual transliteration.  If there
        // is additional filtered text (if limit < globalLimit) then
        // we pass in an incremental value of FALSE to force the subclass
        // to complete the transliteration for this segment.
        handleTransliterate(text, index,
                            limit < globalLimit ? FALSE : incremental);
        
        // Adjust overall limit for insertions/deletions.  Don't need
        // to worry about contextLimit because handleTransliterate()
        // maintains that.
        globalLimit += index.limit - limit;

        // If we failed to complete transliterate this segment, then
        // we are done.  If we did completely transliterate this
        // segment, then repeat with the next unfiltered segment.
    } while (index.start == index.limit);

    // Start is valid where it is.  Limit needs to be put back where
    // it was, modulo adjustments for deletions/insertions.
    index.limit = globalLimit;    
}

/**
 * Method for subclasses to use to set the maximum context length.
 * @see #getMaximumContextLength
 */
void Transliterator::setMaximumContextLength(int32_t maxContextLength) {
    maximumContextLength = maxContextLength;
}

/**
 * Returns a programmatic identifier for this transliterator.
 * If this identifier is passed to <code>getInstance()</code>, it
 * will return this object, if it has been registered.
 * @see #registerInstance
 * @see #getAvailableIDs
 */
const UnicodeString& Transliterator::getID(void) const {
    return ID;
}

/**
 * Returns a name for this transliterator that is appropriate for
 * display to the user in the default locale.  See {@link
 * #getDisplayName(Locale)} for details.
 */
UnicodeString& Transliterator::getDisplayName(const UnicodeString& ID,
                                              UnicodeString& result) {
    return getDisplayName(ID, Locale::getDefault(), result);
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
 * transliterator at the first ID_SEP.  If there is no ID_SEP, then the
 * entire ID forms the only string.
 * @param inLocale the Locale in which the display name should be
 * localized.
 * @see java.text.MessageFormat
 */
UnicodeString& Transliterator::getDisplayName(const UnicodeString& ID,
                                              const Locale& inLocale,
                                              UnicodeString& result) {
    UErrorCode status = U_ZERO_ERROR;

    ResourceBundle bundle(u_getDataDirectory(), inLocale, status);

    // Suspend checking status until later...

    // build the char* key
    char key[100];
    uprv_strcpy(key, RB_DISPLAY_NAME_PREFIX);
    int32_t length=(int32_t)uprv_strlen(RB_DISPLAY_NAME_PREFIX);
    key[length + ID.extract(0, (int32_t)(sizeof(key)-length-1), key+length, "")]=0;

    // Try to retrieve a UnicodeString from the bundle.
    UnicodeString resString = bundle.getStringEx(key, status);

    if (U_SUCCESS(status) && resString.length() != 0) {
        return result = resString; // [sic] assign & return
    }

    // We have failed to get a name from the locale data.  This is
    // typical, since most transliterators will not have localized
    // name data.  The next step is to retrieve the MessageFormat
    // pattern from the locale data and to use it to synthesize the
    // name from the ID.

    status = U_ZERO_ERROR;
    resString = bundle.getStringEx(RB_DISPLAY_NAME_PATTERN, status);

    if (U_SUCCESS(status) && resString.length() != 0) {
        MessageFormat msg(resString, inLocale, status);
        // Suspend checking status until later...

        // We pass either 2 or 3 Formattable objects to msg.
        Formattable args[3];
        int32_t i = ID.indexOf(ID_SEP);
        int32_t nargs;
        if (i < 0) {
            args[0].setLong(1); // # of args to follow
            args[1].setString(ID);
            nargs = 2;
        } else {
            UnicodeString left, right;
            ID.extractBetween(0, i, left);
            ID.extractBetween(i+1, ID.length(), right);
            args[0].setLong(2); // # of args to follow
            args[1].setString(left);
            args[2].setString(right);
            nargs = 3;
        }

        // Use display names for the scripts, if they exist
        UnicodeString s;
        length=(int32_t)uprv_strlen(RB_SCRIPT_DISPLAY_NAME_PREFIX);
        for (int j=1; j<=((i<0)?1:2); ++j) {
            status = U_ZERO_ERROR;
            uprv_strcpy(key, RB_SCRIPT_DISPLAY_NAME_PREFIX);
            args[j].getString(s);
            key[length + s.extract(0, sizeof(key)-length-1, key+length, "")]=0;

            resString = bundle.getStringEx(key, status);

            if (U_SUCCESS(status)) {
                args[j] = resString;
            }
        }
        
        status = U_ZERO_ERROR;
        FieldPosition pos; // ignored by msg
        msg.format(args, nargs, result, pos, status);
        if (U_SUCCESS(status)) {
            return result;
        }
    }

    // We should not reach this point unless there is something
    // wrong with the build or the RB_DISPLAY_NAME_PATTERN has
    // been deleted from the root RB_LOCALE_ELEMENTS resource.
    result = ID;
    return result;
}

/**
 * Returns the filter used by this transliterator, or <tt>null</tt>
 * if this transliterator uses no filter.  Caller musn't delete
 * the result!
 */
const UnicodeFilter* Transliterator::getFilter(void) const {
    return filter;
}

/**
 * Returns the filter used by this transliterator, or
 * <tt>NULL</tt> if this transliterator uses no filter.  The
 * caller must eventually delete the result.  After this call,
 * this transliterator's filter is set to <tt>NULL</tt>.
 */
UnicodeFilter* Transliterator::orphanFilter(void) {
    UnicodeFilter *result = filter;
    // MUST go through adoptFilter in case latter is overridden
    adoptFilter(0);
    return result;
}

/**
 * Changes the filter used by this transliterator.  If the filter
 * is set to <tt>null</tt> then no filtering will occur.
 *
 * <p>Callers must take care if a transliterator is in use by
 * multiple threads.  The filter should not be changed by one
 * thread while another thread may be transliterating.
 */
void Transliterator::adoptFilter(UnicodeFilter* filterToAdopt) {
    delete filter;
    filter = filterToAdopt;
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
Transliterator* Transliterator::createInverse(void) const {
    return Transliterator::createInstance(ID, UTRANS_REVERSE);
}

/**
 * Returns a <code>Transliterator</code> object given its ID.
 * The ID must be either a system transliterator ID or a ID registered
 * using <code>registerInstance()</code>.
 *
 * @param ID a valid ID, as enumerated by <code>getAvailableIDs()</code>
 * @return A <code>Transliterator</code> object with the given ID
 * @see #registerInstance
 * @see #getAvailableIDs
 * @see #getID
 */
Transliterator* Transliterator::createInstance(const UnicodeString& ID,
                                               UTransDirection dir,
                                               UParseError* parseError) {
    UErrorCode status = U_ZERO_ERROR;
    return createInstance(ID, dir, -1, NULL, parseError, status);
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
Transliterator* Transliterator::createInstance(const UnicodeString& ID,
                                               UTransDirection dir,
                                               int32_t idSplitPoint,
                                               Transliterator *adoptedSplitTrans,
                                               UParseError* parseError,
                                               UErrorCode& status) {
    if (U_FAILURE(status)) {
        return 0;
    }

    UVector list(status);
    int32_t ignored;
    UnicodeString regenID;
    parseCompoundID(ID, regenID, dir, idSplitPoint, adoptedSplitTrans,
                    list, ignored, parseError, status);

    if (U_FAILURE(status)) {
        return 0;
    }

    Transliterator *t;
    switch (list.size()) {
    case 0:
        t = new NullTransliterator();
        break;
    case 1:
        t = (Transliterator*) list.elementAt(0);
        break;
    default:
        t = new CompoundTransliterator(dir, list, status);
        break;
    }
    t->setID(regenID);
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
Transliterator* Transliterator::createFromRules(const UnicodeString& ID,
                                                const UnicodeString& rules,
                                                UTransDirection dir,
                                                UParseError* parseError) {
    UnicodeString idBlock;
    int32_t idSplitPoint = -1;
    TransliterationRuleData *data = 0;
    UErrorCode status = U_ZERO_ERROR;

    TransliteratorParser::parse(rules, dir, data,
                                idBlock, idSplitPoint,
                                parseError, status);

    if (U_FAILURE(status)) {
        delete data;
        return 0;
    }

    // NOTE: The logic here matches that in TransliteratorRegistry.
    if (idBlock.length() == 0) {
        if (data == 0) {
            // No idBlock, no data -- this is just an
            // alias for Null
            return new NullTransliterator();
        } else {
            // No idBlock, data != 0 -- this is an
            // ordinary RBT_DATA.
            return new RuleBasedTransliterator(ID, data, TRUE); // TRUE == adopt data object
        }
    } else {
        if (data == 0) {
            // idBlock, no data -- this is an alias
            Transliterator *t = createInstance(idBlock, dir, parseError);
            if (t != 0) {
                t->setID(ID);
            }
            return t;
        } else {
            // idBlock and data -- this is a compound
            // RBT
            UnicodeString id("_", "");
            Transliterator *t = new RuleBasedTransliterator(id, data, TRUE); // TRUE == adopt data object
            t = new CompoundTransliterator(ID, idBlock, idSplitPoint,
                                           t, status);
            if (U_FAILURE(status)) {
                delete t;
                t = 0;
            }
            return t;
        }
    }
}

UnicodeString& Transliterator::toRules(UnicodeString& rulesSource,
                                       UBool escapeUnprintable) const {
    // The base class implementation of toRules munges the ID into
    // the correct format.  That is: foo => ::foo
    rulesSource = getID();
    // KEEP in sync with rbt_pars
    rulesSource.insert(0, UnicodeString("::", ""));
    rulesSource.append(ID_DELIM);
    return rulesSource;
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
 */
void Transliterator::parseCompoundID(const UnicodeString& id,
                                     UnicodeString& regenID,
                                     UTransDirection dir,
                                     int32_t idSplitPoint,
                                     Transliterator *adoptedSplitTrans,
                                     UVector& result,
                                     int32_t& splitTransIndex,
                                     UParseError* parseError,
                                     UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }
    
    regenID.truncate(0);
    splitTransIndex = -1;
    int32_t pos = 0;
    int32_t i;
    while (pos < id.length()) {
        // We compare (pos >= split), not (pos == split), so we can
        // skip over whitespace (see below).
        if (pos >= idSplitPoint && adoptedSplitTrans != 0) {
            splitTransIndex = result.size();
            result.addElement(adoptedSplitTrans, status);
            adoptedSplitTrans = 0;
        }
        int32_t p = pos;
        UBool sawDelimiter; // We ignore this
        Transliterator *t =
            parseID(id, regenID, p, sawDelimiter, dir, parseError, TRUE);
        if (p == pos || (p < id.length() && !sawDelimiter)) {
            delete t;
            status = U_ILLEGAL_ARGUMENT_ERROR;
            break;
        }
        pos = p;
        // The return value may be NULL when, for instance, creating a
        // REVERSE transliterator of ID "Latin-Greek()".
        if (t != 0) {
            result.addElement(t, status);
        }
    }

    // Handle case of idSplitPoint == id.length()
    if (pos >= idSplitPoint && adoptedSplitTrans != 0) {
        splitTransIndex = result.size();
        result.addElement(adoptedSplitTrans, status);
        adoptedSplitTrans = 0;
    }

    if (U_FAILURE(status)) {
        for (i=0; i<result.size(); ++i) {
            delete (Transliterator*)result.elementAt(i);
        }
        result.removeAllElements();
        delete adoptedSplitTrans;
    }
}

/**
 * Parse a single ID, possibly including an inline filter, and return
 * the resultant transliterator object.  NOTE: If 'create' is FALSE,
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
 * FALSE then this parameter is not used.
 * @param pos INPUT-OUTPUT parameter.  On input, the position of the
 * first character to parse.  On output, the position after the last
 * character parsed.  This will be a semicolon or ID.length().  In the
 * case of an error this value will be unchanged.
 * @param create if TRUE, create and return the result.  If FALSE,
 * only scan the ID, and return NULL.
 * @return a newly created transliterator, or NULL.  NULL is returned
 * in all cases if create is FALSE.  If create is TRUE, then NULL is
 * returned on error, or if the ID is effectively empty.
 * E.g. "Latin-Greek()" with dir == REVERSE.  Do NOT check for NULL to
 * determine if there was an error.  Instead, check to see if pos
 * moved.
 */
Transliterator* Transliterator::parseID(const UnicodeString& ID,
                                        UnicodeString& regenID,
                                        int32_t& pos,
                                        UBool& sawDelimiter,
                                        UTransDirection dir,
                                        UParseError* parseError,
                                        UBool create) {
    int32_t limit, preDelimLimit,
        revStart, revLimit,
        idStart, idLimit,
        setStart, setLimit;

    UnicodeSet* filter = 0;

    if (!parseIDBounds(ID, pos, FALSE, limit,
                       setStart, setLimit, revStart, filter)) {
        delete filter;
        return 0;
    }
    idStart = pos;
    idLimit = limit;

    if (revStart >= 0 && revStart < limit) {
        int32_t revSetStart, revSetLimit, dummy;
        UnicodeSet* revFilter = 0;
        if (!parseIDBounds(ID, revStart+1, TRUE, revLimit,
                           revSetStart, revSetLimit, dummy, revFilter)) {
            delete filter;
            delete revFilter;
            return 0;
        }
        // revStart points to '('
        if (dir == UTRANS_REVERSE) {
            idStart = revStart+1;
            idLimit = revLimit;
            setStart = revSetStart;
            setLimit = revSetLimit;
            delete filter;
            filter = revFilter;
        } else {
            idLimit = revStart;
            delete revFilter;
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
    skipSpaces(ID, limit);
    sawDelimiter = (limit < ID.length() && ID.charAt(limit) == ID_DELIM);
    if (sawDelimiter) {
        ++limit;
    }
    skipSpaces(ID, limit);

    if (!create) {
        // TODO Improve performance by scanning the UnicodeSet pattern
        // without actually constructing it, if create is FALSE.  That
        // is, create a method like this one for UnicodeSet.
        delete filter;
        pos = limit;
        return 0;
    }

    // 'id' is the ID with the filter pattern removed and with
    // whitespace deleted.  In a Foo(Bar) ID, id is Foo for FORWARD
    // and Bar for REVERSE.
    UnicodeString id, str;
    ID.extractBetween(idStart, setStart, id);
    ID.extractBetween(setLimit, idLimit, str);
    id.append(str);

    // Delete whitespace
    int32_t i;
    for (i=0; i<id.length(); ++i) {
        if (u_isspace(id.charAt(i))) {
            id.remove(i, 1);
            --i;
        }
    }

    // Fix the id, if necessary, by reversing it (A-B => B-A).  This
    // is only done if the id is NOT of the form Foo(Bar).  Record the
    // position of the separator.
    // 
    // For both A-B and Foo(Bar) ids, detect the special case of Null,
    // whose inverse is itself.  Given an ID with no separator "Foo",
    // an abbreviation for "Any-Foo", consider the inverse to be
    // "Foo-Any".
    int32_t sep = id.indexOf(ID_SEP);
    if (sep < 0 && id.caseCompare(NullTransliterator::SHORT_ID,
                                  U_FOLD_CASE_DEFAULT) == 0) {
        // Handle "Null"
        sep = id.length();
    } else if (dir == UTRANS_REVERSE &&
               id.caseCompare(NullTransliterator::ID,
                              U_FOLD_CASE_DEFAULT) == 0) {
        // Reverse of "Any-Null" => "Null"
        id.removeBetween(0, sep+1);
        sep = id.length();
    } else if (dir == UTRANS_REVERSE && revStart < 0) {
        if (sep >= 0) {
            id.extractBetween(0, sep, str);
            id.removeBetween(0, sep+1);
        } else {
            str = UnicodeString("Any", "");
        }
        sep = id.length();
        id.append(ID_SEP).append(str);
    } else if (sep < 0 && id.length() > 0) {
        // Don't do anything for empty IDs -- we handle these specially below
        str = UnicodeString("Any-", "");
        sep = str.length() - 1;
        id.insert(0, str);
    }

    Transliterator *t = 0;

    // If we have a reverse part of the ID, e.g., Foo(Bar), then we
    // need to check for an empty part, which represents a Null
    // transliterator.  We return 0 (not a NullTransliterator).  If we
    // are not of the form Foo(Bar) then an empty string is illegal.
    if (revStart >= 0 && id.length() == 0) {
        // Ignore any filters; filters on Null are meaningless (and we
        // can't attach them to 0 anyway)
        delete filter;
    }

    else {
        // Create the actual transliterator from the registry
        if (parseError != 0) {
            parseError->line = parseError->offset = 0;
            parseError->preContext[0] = parseError->postContext[0] = 0;
        }
        if (registry == 0) {
            initializeRegistry();
        }
        {
            Mutex lock(&registryMutex);
            str.truncate(0);
            t = registry->get(id, str, parseError);
            // Need to enclose this in a block to prevent deadlock when
            // instantiating aliases (below).
        }
        
        if (str.length() != 0) {
            // assert(t==0);
            // Instantiate an alias
            t = createInstance(str, UTRANS_FORWARD, parseError);
        }

        if (t == 0) {
            // Creation failed; the ID is invalid or is an alias
            delete filter;
            return 0;
        }

        // Set the filter, if any
        t->adoptFilter(filter);
    }
    
    // Set the ID.  This is normally just a substring of the input
    // ID, but for reverse transliterators we need to munge A-B to
    // B-A or Foo(Bar) to Bar(Foo).
    if (dir == UTRANS_FORWARD) {
        ID.extractBetween(pos, preDelimLimit, id);
    } else if (revStart < 0) {
        id.insert(sep, ID, setStart, setLimit-setStart);
    } else {
        // Change Foo(Bar) to Bar(Foo)
        ID.extractBetween(pos, revStart, str);
        str.trim();
        ID.extractBetween(revStart+1, revLimit, id);
        id.trim().append(OPEN_PAREN).append(str).append(CLOSE_PAREN);
    }
    id.trim();

    if (t != 0) {
        t->setID(id);
    }

    // Regenerate ID of a compound entity
    if (dir == UTRANS_FORWARD) {
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
    pos = limit;

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
 * @param withinParens if TRUE, parse the Bar of Foo(Bar), stop at a
 * close paren, and do not look for an open paren.  If TRUE then a
 * close paren MUST be seen or FALSE is returned; if FALSE then the
 * ';' delimiter is optional.
 * @param limit set to the position of ';' or ')' (depending on
 * withinParens), or ID.length() if no delimiter was found
 * @param setStart set to the start of an inline filter pattern,
 * or pos if none
 * @param setLimit set to the limit of an inline filter pattern,
 * or pos if none
 * @param revStart if not withinParens then set to the position of the
 * first '(', which may be > limit; otherwise set to -1
 * @param filter set to a newly created UnicodeSet object for the
 * inline filter pattern, if any; OWNED BY THE CALLER
 *
 * @return TRUE if the pattern is valid, FALSE is there is an invalid
 * UnicodeSet pattern or if withinParens is TRUE and no close paren is
 * seen.
 */
UBool Transliterator::parseIDBounds(const UnicodeString& ID,
                                    int32_t pos,
                                    UBool withinParens,
                                    int32_t& limit,
                                    int32_t& setStart,
                                    int32_t& setLimit,
                                    int32_t& revStart,
                                    UnicodeSet*& filter) {
    UChar endDelimiter = withinParens ? CLOSE_PAREN : ID_DELIM;
    limit = ID.indexOf(endDelimiter, pos);
    if (limit < 0) {
        if (withinParens) {
            return FALSE;
        }
        limit = ID.length();
    }
    setStart = ID.indexOf((UChar)0x005B /*[*/, pos);
    revStart = withinParens ? -1 : ID.indexOf(OPEN_PAREN, pos);

    if (setStart >= 0 && setStart < limit &&
        (revStart < 0 || setStart < revStart)) {
        UErrorCode status = U_ZERO_ERROR;
        ParsePosition ppos(setStart);
        // TODO Improve performance by scanning the UnicodeSet pattern
        // without actually constructing it, if create is FALSE.  That
        // is, create a method like this one for UnicodeSet.
        filter = new UnicodeSet();
        filter->applyPattern(ID, ppos, 0, status);
        if (U_FAILURE(status)) {
            delete filter;
            return FALSE;
        }
        setLimit = ppos.getIndex();
        if (limit < setLimit) {
            limit = ID.indexOf(endDelimiter, setLimit);
            if (limit < 0) {
                if (withinParens) {
                    return FALSE;
                }
                limit = ID.length();
            }
        }
        if (revStart >= 0 && revStart < setLimit) {
            revStart = ID.indexOf(CLOSE_PAREN, setLimit);
        }
    } else {
        setStart = setLimit = pos;
    }
    return TRUE;
}

/**
 * If pos is the index of a space in str, then advance it over that
 * space and any immediately subsequent ones.
 */
void Transliterator::skipSpaces(const UnicodeString& str,
                                int32_t& pos) {
    while (pos < str.length() &&
           u_isspace(str.charAt(pos))) {
        ++pos;
    }
}

// For public consumption
void Transliterator::registerFactory(const UnicodeString& id,
                                     Transliterator::Factory factory) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    _registerFactory(id, factory);
}

// To be called only by Transliterator subclasses that are called
// to register themselves by initializeRegistry().
void Transliterator::_registerFactory(const UnicodeString& id,
                                      Transliterator::Factory factory) {
    registry->put(id, factory, TRUE);
}

/**
 * Registers a instance <tt>obj</tt> of a subclass of
 * <code>Transliterator</code> with the system.  This object must
 * implement the <tt>clone()</tt> method.  When
 * <tt>getInstance()</tt> is called with an ID string that is
 * equal to <tt>obj.getID()</tt>, then <tt>obj.clone()</tt> is
 * returned.
 *
 * @param obj an instance of subclass of
 * <code>Transliterator</code> that defines <tt>clone()</tt>
 * @see #getInstance
 * @see #unregister
 */
void Transliterator::registerInstance(Transliterator* adoptedPrototype) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    registry->put(adoptedPrototype, TRUE);
}

/**
 * Unregisters a transliterator or class.  This may be either
 * a system transliterator or a user transliterator or class.
 * 
 * @param ID the ID of the transliterator or class
 * @see #registerInstance

 */
void Transliterator::unregister(const UnicodeString& ID) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    registry->remove(ID);
}

/**
 * Return the number of IDs currently registered with the system.
 * To retrieve the actual IDs, call getAvailableID(i) with
 * i from 0 to countAvailableIDs() - 1.
 */
int32_t Transliterator::countAvailableIDs(void) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    return registry->countAvailableIDs();
}

/**
 * Return the index-th available ID.  index must be between 0
 * and countAvailableIDs() - 1, inclusive.  If index is out of
 * range, the result of getAvailableID(0) is returned.
 */
const UnicodeString& Transliterator::getAvailableID(int32_t index) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    return registry->getAvailableID(index);
}

int32_t Transliterator::countAvailableSources(void) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    return registry->countAvailableSources();
}

UnicodeString& Transliterator::getAvailableSource(int32_t index,
                                                  UnicodeString& result) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    return registry->getAvailableSource(index, result);
}

int32_t Transliterator::countAvailableTargets(const UnicodeString& source) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    return registry->countAvailableTargets(source);
}

UnicodeString& Transliterator::getAvailableTarget(int32_t index,
                                                  const UnicodeString& source,
                                                  UnicodeString& result) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    return registry->getAvailableTarget(index, source, result);
}

int32_t Transliterator::countAvailableVariants(const UnicodeString& source,
                                               const UnicodeString& target) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    return registry->countAvailableVariants(source, target);
}

UnicodeString& Transliterator::getAvailableVariant(int32_t index,
                                                   const UnicodeString& source,
                                                   const UnicodeString& target,
                                                   UnicodeString& result) {
    if (registry == 0) {
        initializeRegistry();
    }
    Mutex lock(&registryMutex);
    return registry->getAvailableVariant(index, source, target, result);
}

/**
 * Method for subclasses to use to obtain a character in the given
 * string, with filtering.
 * @deprecated the new architecture provides filtering at the top
 * level.  This method will be removed Dec 31 2001.
 */
UChar Transliterator::filteredCharAt(const Replaceable& text, int32_t i) const {
    UChar c;
    const UnicodeFilter* localFilter = getFilter();
    return (localFilter == 0) ? text.charAt(i) :
        (localFilter->contains(c = text.charAt(i)) ? c : (UChar)0xFFFE);
}

void Transliterator::initializeRegistry(void) {
    // Lock first, check registry pointer second
    Mutex lock(&registryMutex);
    if (registry != 0) {
        // We were blocked by another thread in initializeRegistry()
        return;
    }

    UErrorCode status = U_ZERO_ERROR;

    registry = new TransliteratorRegistry(status);
    if (registry == 0 || U_FAILURE(status)) {
        return; // out of memory, no recovery
    }

    /* The following code parses the index table located in
     * icu/data/translit_index.txt.  The index is an n x 4 table
     * that follows this format:
     *
     *   <id>:file:<resource>:<direction>
     *   <id>:internal:<resource>:<direction>
     *   <id>:alias:<getInstanceArg>:
     *  
     * <id> is the ID of the system transliterator being defined.  These
     * are public IDs enumerated by Transliterator.getAvailableIDs(),
     * unless the second field is "internal".
     * 
     * <resource> is a ResourceReader resource name.  Currently these refer
     * to file names under com/ibm/text/resources.  This string is passed
     * directly to ResourceReader, together with <encoding>.
     * 
     * <direction> is either "FORWARD" or "REVERSE".
     * 
     * <getInstanceArg> is a string to be passed directly to
     * Transliterator.getInstance().  The returned Transliterator object
     * then has its ID changed to <id> and is returned.
     *
     * The extra blank field on "alias" lines is to make the array square.
     */

    Locale indexLoc("translit_index");

    ResourceBundle bundle((char *)0,
                          indexLoc, status);
    ResourceBundle transIDs(bundle.get(RB_RULE_BASED_IDS, status));

    int32_t row, maxRows;
    if (U_SUCCESS(status)) {
        maxRows = transIDs.getSize();
        for (row = 0; row < maxRows; row++) {
            ResourceBundle colBund(transIDs.get(row, status));

            if (U_SUCCESS(status) && colBund.getSize() == 4) {
                UnicodeString id(colBund.getStringEx((int32_t)0, status));
                UChar type = colBund.getStringEx(1, status).charAt(0);
                UnicodeString resString(colBund.getStringEx(2, status));

                if (U_SUCCESS(status)) {
                    switch (type) {
                    case 0x66: // 'f'
                    case 0x69: // 'i'
                        // 'file' or 'internal';
                        // row[2]=resource, row[3]=direction
                        {
                            UBool visible = (type == 0x0066 /*f*/);
                            UTransDirection dir = 
                                (colBund.getStringEx(3, status).charAt(0) ==
                                 0x0046 /*F*/) ?
                                UTRANS_FORWARD : UTRANS_REVERSE;
                            registry->put(id, resString, dir, visible);
                        }
                        break;
                    case 0x61: // 'a'
                        // 'alias'; row[2]=createInstance argument
                        registry->put(id, resString, TRUE);
                        break;
                    }
                }
            }
        }
    }
    // Manually add prototypes that the system knows about to the
    // cache.  This is how new non-rule-based transliterators are
    // added to the system.

    registry->put(new HexToUnicodeTransliterator(), TRUE);
    registry->put(new UnicodeToHexTransliterator(), TRUE);
    registry->put(new JamoHangulTransliterator(), TRUE);
    registry->put(new HangulJamoTransliterator(), TRUE);
    registry->put(new NullTransliterator(), TRUE);
    registry->put(new RemoveTransliterator(), TRUE);
    registry->put(new LowercaseTransliterator(), TRUE);
    registry->put(new UppercaseTransliterator(), TRUE);
    registry->put(new TitlecaseTransliterator(), TRUE);
    registry->put(new UnicodeNameTransliterator(), TRUE);
    registry->put(new NameUnicodeTransliterator(), TRUE);
    NormalizationTransliterator::registerIDs();
}

//eof
