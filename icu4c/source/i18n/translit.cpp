/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "unicode/translit.h"
#include "cmemory.h"
#include "cstring.h"
#include "unicode/hextouni.h"
#include "unicode/locid.h"
#include "unicode/msgfmt.h"
#include "mutex.h"
#include "rbt_data.h"
#include "rbt_pars.h"
#include "unicode/rep.h"
#include "unicode/resbund.h"
#include "uhash.h"
#include "unicode/unifilt.h"
#include "unicode/unitohex.h"
#include "unicode/nultrans.h"
#include "unicode/putil.h"
#include "unicode/cpdtrans.h"
#include "unicode/jamohang.h"
#include "unicode/hangjamo.h"

const UChar Transliterator::ID_SEP   = 0x002D; /*-*/
const UChar Transliterator::ID_DELIM = 0x003B; /*;*/

/**
 * Dictionary of known transliterators.  Keys are <code>String</code>
 * names, values are one of the following:
 *
 * <ul><li><code>Transliterator</code> objects
 *
 * <li><code>RULE_BASED_PLACEHOLDER</code>, in which case the ID
 * will have its first ID_SEP removed and be appended to
 * RB_RULE_BASED_PREFIX to form a resource bundle name from which
 * the RB_RULE key is looked up to obtain the rule.
 *
 * <li><code>REVERSE_RULE_BASED_PLACEHOLDER</code>.  Like
 * <code>RULE_BASED_PLACEHOLDER</code>, except the entity names in
 * the ID are reversed, and the argument
 * RuleBasedTransliterator::REVERSE is pased to the
 * RuleBasedTransliterator constructor.
 * </ul>
 */
UHashtable* Transliterator::cache = 0;

/**
 * The mutex controlling access to the cache.
 */
UMTX Transliterator::cacheMutex = NULL;

/**
 * When set to TRUE, the cache has been initialized.  Any code must
 * check this boolean before accessing the cache, and if the boolean
 * is FALSE, it must call initializeCache().  We do this form of lazy
 * evaluation for two reasons: (1) so we don't initialize if we don't
 * have to (i.e., if no one is using Transliterator, but has included
 * the code as part of a shared library, and (2) to avoid static
 * intialization problems.
 */
bool_t Transliterator::cacheInitialized = FALSE;

/**
 * Prefix for resource bundle key for the display name for a
 * transliterator.  The ID is appended to this to form the key.
 * The resource bundle value should be a String.
 */
const char* Transliterator::RB_DISPLAY_NAME_PREFIX = "%Translit%%";

/**
 * Prefix for resource bundle key for the display name for a
 * transliterator SCRIPT.  The ID is appended to this to form the key.
 * The resource bundle value should be a String.
 */
const char* Transliterator::RB_SCRIPT_DISPLAY_NAME_PREFIX = "%Translit%";

/**
 * Resource bundle key for display name pattern.
 * The resource bundle value should be a String forming a
 * MessageFormat pattern, e.g.:
 * "{0,choice,0#|1#{1} Transliterator|2#{1} to {2} Transliterator}".
 */
const char* Transliterator::RB_DISPLAY_NAME_PATTERN =
    "TransliteratorNamePattern";

/**
 * Resource bundle key for the list of RuleBasedTransliterator IDs.
 * The resource bundle value should be a String[] with each element
 * being a valid ID.  The ID will be appended to RB_RULE_BASED_PREFIX
 * to obtain the class name in which the RB_RULE key will be sought.
 */
const char* Transliterator::RB_RULE_BASED_IDS =
    "RuleBasedTransliteratorIDs";

/**
 * Resource bundle key for the RuleBasedTransliterator rule.
 */
const char* Transliterator::RB_RULE = "Rule";

/**
 * Default constructor.
 * @param theID the string identifier for this transliterator
 * @param theFilter the filter.  Any character for which
 * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
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
        filter = other.filter->clone();
    }
}

/**
 * Assignment operator.
 */
Transliterator& Transliterator::operator=(const Transliterator& other) {
    ID = other.ID;
    maximumContextLength = other.maximumContextLength;
    filter = (other.filter == 0) ?
        0 : other.filter->clone();
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

    Position offsets(start, limit);
    handleTransliterate(text, offsets, FALSE);
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
 * #finishKeyboardTransliteration} after the last call to this
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
 * @see #START
 * @see #LIMIT
 * @see #CURSOR
 * @see #handleTransliterate
 * @exception IllegalArgumentException if <code>index</code>
 * is invalid
 */
void Transliterator::transliterate(Replaceable& text,
                                   Position& index,
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
 * <code>index.limit</code>.
 * @see #transliterate(Replaceable, int[], String)
 */
void Transliterator::transliterate(Replaceable& text,
                                   Position& index,
                                   UChar insertion,
                                   UErrorCode& status) const {
    UnicodeString str(insertion);
    _transliterate(text, index, &str, status);
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
                                   Position& index,
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
                                           Position& index) const {
    transliterate(text, index.start, index.limit);
}

/**
 * This internal method does keyboard transliteration.  If the
 * 'insertion' is non-null then we append it to 'text' before
 * proceeding.  This method calls through to the pure virtual
 * framework method handleTransliterate() to do the actual
 * work.
 */
void Transliterator::_transliterate(Replaceable& text,
                                    Position& index,
                                    const UnicodeString* insertion,
                                    UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return;
    }

    if (index.start < 0 ||
        index.limit > text.length() ||
        index.cursor < index.start ||
        index.cursor > index.limit) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    int32_t originalStart = index.start;
    if (insertion != 0) {
        text.handleReplaceBetween(index.limit, index.limit, *insertion);
        index.limit += insertion->length();
    }

    handleTransliterate(text, index, TRUE);

    index.start = uprv_max(index.cursor - getMaximumContextLength(),
                           originalStart);
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
    ResourceBundle bundle(Locale::getDataDirectory(), inLocale, status);
    // Suspend checking status until later...

    // build the char* key
    char key[100];
    uprv_strcpy(key, RB_DISPLAY_NAME_PREFIX);
    int32_t length=uprv_strlen(RB_DISPLAY_NAME_PREFIX);
    key[length + ID.extract(0, sizeof(key)-length-1, key+length, "")]=0;

    // Try to retrieve a UnicodeString* from the bundle.  The result,
    // if any, should NOT be deleted.
    const UnicodeString* resString = bundle.getString(key, status);

    if (U_SUCCESS(status) && resString != 0) {
        return result = *resString; // [sic] assign & return
    }

    // We have failed to get a name from the locale data.  This is
    // typical, since most transliterators will not have localized
    // name data.  The next step is to retrieve the MessageFormat
    // pattern from the locale data and to use it to synthesize the
    // name from the ID.

    status = U_ZERO_ERROR;
    resString = bundle.getString(RB_DISPLAY_NAME_PATTERN, status);

    if (U_SUCCESS(status) && resString != 0) {
        MessageFormat msg(*resString, inLocale, status);
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
        for (int j=1; j<=((i<0)?1:2); ++j) {
            status = U_ZERO_ERROR;
            uprv_strcpy(key, RB_SCRIPT_DISPLAY_NAME_PREFIX);
            length=uprv_strlen(RB_SCRIPT_DISPLAY_NAME_PREFIX);
            args[j].getString(s);
            key[length + s.extract(0, sizeof(key)-length-1, key+length, "")]=0;

            resString = bundle.getString(key, status);

            if (U_SUCCESS(status)) {
                args[j] = *resString;
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
    return Transliterator::createInstance(ID, REVERSE);
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
Transliterator* Transliterator::createInstance(const UnicodeString& ID,
                                               Transliterator::Direction dir) {
    if (ID.indexOf(ID_DELIM) >= 0) {
        return new CompoundTransliterator(ID, dir, 0);
    }
    Transliterator* t = 0;
    if (dir == REVERSE) {
        int32_t i = ID.indexOf(ID_SEP);
        if (i >= 0) {
            UnicodeString inverseID, right;
            ID.extractBetween(i+1, ID.length(), inverseID);
            ID.extractBetween(0, i, right);
            inverseID.append(ID_SEP).append(right);
            t = _createInstance(inverseID);
        }
    } else {
        t = _createInstance(ID);
    }
    return t;
}

/**
 * This is the path to the subdirectory within the locale data
 * directory that contains the rule-based transliterator resource
 * bundle files.  This is constructed dynamically the first time
 * Transliterator::getDataDirectory() is called.
 */
char* Transliterator::DATA_DIR = 0;

/**
 * This is the name of a subdirectory within the locale data directory
 * that contains the rule-based transliterator resource bundle files.
 */
const char* Transliterator::RESOURCE_SUB_DIR = "translit";

/**
 * Returns the directory in which the transliterator resource bundle
 * files are located.  This is a subdirectory, named RESOURCE_SUB_DIR,
 * under Locale::getDataDirectory().  It ends in a path separator.
 */
const char* Transliterator::getDataDirectory(void) {
    if (DATA_DIR == 0) {
        Mutex lock; // Okay to use the global mutex here
        if (DATA_DIR == 0) {
            /* Construct the transliterator data directory path.  This
             * is a subdirectory of the locale data directory.
             */
            const char* data = Locale::getDataDirectory();
            int32_t len = uprv_strlen(data);
            DATA_DIR = (char*) uprv_malloc(
                                 len + uprv_strlen(RESOURCE_SUB_DIR) + 2);
            if (DATA_DIR == 0) {
                // This is a fatal unrecoverable error -- we just set DATA_DIR
                // to the ICU data directory.  We won't be able to retrieve any
                // rule-based transliterator data but we won't keep trying.
                DATA_DIR = (char*) data;
            } else {
                uprv_strcpy(DATA_DIR, data);
                uprv_strcat(DATA_DIR, RESOURCE_SUB_DIR);
                uprv_strcat(DATA_DIR, U_FILE_SEP_STRING);
            }
        }
    }
    return DATA_DIR;
}

inline int32_t Transliterator::hash(const UnicodeString& str) {
    return str.hashCode() & 0x7FFFFFFF;
}

/**
 * Returns a transliterator object given its ID.  Unlike getInstance(),
 * this method returns null if it cannot make use of the given ID.
 */
Transliterator* Transliterator::_createInstance(const UnicodeString& ID) {
    UErrorCode status = U_ZERO_ERROR;

    if (!cacheInitialized) {
        initializeCache();
    }

    Mutex lock(&cacheMutex);

    CacheEntry* entry = (CacheEntry*) uhash_get(cache, hash(ID));
    TransliterationRuleData* data = 0;

    if (entry == 0) {
        return 0;
    }

    if (entry->entryType == CacheEntry::RBT_DATA) {
        data = entry->u.data;
        // Fall through to construct transliterator from cached Data object.
    } else if (entry->entryType == CacheEntry::PROTOTYPE) {
        return entry->u.prototype->clone();
    } else {
        // At this point entry type must be either RULE_BASED_PLACEHOLDER
        // or REVERSE_RULE_BASED_PLACEHOLDER.
        bool_t isReverse =
            (entry->entryType ==
             CacheEntry::REVERSE_RULE_BASED_PLACEHOLDER);
        
        // We use the file name, taken from another resource bundle
        // 2-d array at static init time, as a locale language.  We're
        // just using the locale mechanism to map through to a file
        // name; this in no way represents an actual locale.
        Locale fakeLocale(entry->rbFile);

        ResourceBundle bundle(Transliterator::getDataDirectory(),
                              fakeLocale, status);
        
        // Call RBT to parse the rules from the resource bundle

        // We don't own the rules - 'rules' is an alias pointer to
        // a string in the RB cache.
        const UnicodeString* rules = bundle.getString(RB_RULE, status);

        // If rules == 0 at this point, or if the status indicates a
        // failure, then we don't have any rules -- there is probably
        // an installation error.  The list in the root locale should
        // correspond to all the installed transliterators; if it
        // lists something that's not installed, we'll get a null
        // pointer here.
        if (rules != 0 && U_SUCCESS(status)) {

            data = TransliterationRuleParser::parse(*rules, isReverse
                                    ? RuleBasedTransliterator::REVERSE
                                    : RuleBasedTransliterator::FORWARD);
            
            // Double check to see if someone has modified the entry
            // since we last looked at it.
            if (entry->entryType != CacheEntry::RBT_DATA) {
                entry->entryType = CacheEntry::RBT_DATA;
                entry->u.data = data;
            } else {
                // Oops!  Another thread has updated this cache entry
                // already to point to a data object.  Discard the
                // one we just created and use the one in the cache
                // instead.
                delete data;
                data = entry->u.data;
            }
        }
    }

    if (data != 0) {
        return new RuleBasedTransliterator(ID, data);
    } else {
        // We have a failure of some kind.  Remove the ID from the
        // cache so we don't keep trying.  NOTE: This will throw off
        // anyone who is, at the moment, trying to iterate over the
        // available IDs.  That's acceptable since we should never
        // really get here except under installation, configuration,
        // or unrecoverable run time memory failures.
        _unregister(ID);
    }

    return 0;
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
void Transliterator::registerInstance(Transliterator* adoptedPrototype,
                                      UErrorCode &status) {    
    if (!cacheInitialized) {
        initializeCache();
    }

    Mutex lock(&cacheMutex);
    _registerInstance(adoptedPrototype, status);
}

/**
 * This internal method registers a prototype instance in the cache.
 * The CALLER MUST MUTEX using cacheMutex before calling this method.
 */
void Transliterator::_registerInstance(Transliterator* adoptedPrototype,
                                       UErrorCode &status) {
    if (U_FAILURE(status)) {
        return;
    }

    int32_t hashCode = hash(adoptedPrototype->getID());

    // This needs explaining: The string reference that getID returns
    // is to the ID data member of Transliterator.  As long as the
    // Transliterator object exists, this reference is valid, and in
    // fact we can take its address and store it in IDS.  No problem
    // there.  The only thing we have to be sure of is that before we
    // remove the prototype (via unregister()), we remove the ID
    // entry.
    cacheIDs.addElement((void*) &adoptedPrototype->getID());

    CacheEntry* entry = (CacheEntry*) uhash_get(cache, hashCode);
    if (entry == 0) {
        entry = new CacheEntry();
    }

    entry->adoptPrototype(adoptedPrototype);

    uhash_putKey(cache, hashCode, entry, &status);
}

/**
 * Unregisters a transliterator or class.  This may be either
 * a system transliterator or a user transliterator or class.
 * 
 * @param ID the ID of the transliterator or class
 * @see #registerInstance
 */
void Transliterator::unregister(const UnicodeString& ID) {
    if (!cacheInitialized) {
        initializeCache();
    }
    Mutex lock(&cacheMutex);
    _unregister(ID);
}

/**
 * Unregisters a transliterator or class.  Internal method.
 * Prerequisites: The cache must be initialized, and the
 * caller must own the cacheMutex.
 */
void Transliterator::_unregister(const UnicodeString& ID) {
    cacheIDs.removeElement((void*) &ID);
	int32_t hc = hash(ID);
    CacheEntry* entry = (CacheEntry*) uhash_get(cache, hc);
	if (entry != 0) {
		UErrorCode status = U_ZERO_ERROR;
		uhash_remove(cache, hc, &status);
		delete entry;
	}
}

/**
 * Vector of registered IDs.
 */
UVector Transliterator::cacheIDs;

/**
 * Return the number of IDs currently registered with the system.
 * To retrieve the actual IDs, call getAvailableID(i) with
 * i from 0 to countAvailableIDs() - 1.
 */
int32_t Transliterator::countAvailableIDs(void) {
    if (!cacheInitialized) {
        initializeCache();
    }
    Mutex lock(&cacheMutex);
    return cacheIDs.size();
}

/**
 * Return the index-th available ID.  index must be between 0
 * and countAvailableIDs() - 1, inclusive.  If index is out of
 * range, the result of getAvailableID(0) is returned.
 */
const UnicodeString& Transliterator::getAvailableID(int32_t index) {
    if (index < 0 || index >= cacheIDs.size()) {
        index = 0;
    }
    if (!cacheInitialized) {
        initializeCache();
    }
    Mutex lock(&cacheMutex);
    return *(const UnicodeString*) cacheIDs[index];
}

/**
 * Method for subclasses to use to obtain a character in the given
 * string, with filtering.
 */
UChar Transliterator::filteredCharAt(const Replaceable& text, int32_t i) const {
    UChar c;
    const UnicodeFilter* filter = getFilter();
    return (filter == 0) ? text.charAt(i) :
        (filter->contains(c = text.charAt(i)) ? c : (UChar)0xFFFF);
}

/**
 * Comparison function for UVector.  Compares two UnicodeString
 * objects given void* pointers to them.
 */
bool_t Transliterator::compareIDs(void* a, void* b) {
    const UnicodeString* aa = (const UnicodeString*) a;
    const UnicodeString* bb = (const UnicodeString*) b;
    return *aa == *bb;
}

void Transliterator::initializeCache(void) {
    // Lock first, check init boolean second
    Mutex lock(&cacheMutex);
    if (cacheInitialized) {
        return;
    }
        
    UErrorCode status = U_ZERO_ERROR;

    // Before looking for the resource, construct our cache.
    // That way if the resource is absent, we will at least
    // have a valid cache object.
    cache = uhash_open((UHashFunction)uhash_hashUString, &status);
    cacheIDs.setComparer(compareIDs);

    /* The following code parses the index table located in
     * icu/data/translit/index.txt.  The index is an n x 3 table
     * that looks like this:
     *
     * RuleBasedTransliteratorIDs {
     *     { "Latin-Arabic", "Arabic-Latin", "larabic" }
     *     { "KeyboardEscape-Latin1", "", "keyescl1" }
     *     ...
     * }
     */

    Locale indexLoc(UNICODE_STRING("index", 5));
    ResourceBundle bundle(Transliterator::getDataDirectory(),
                          indexLoc, status);

    int32_t rows, cols;
    const UnicodeString** ruleBasedIDs =
        bundle.get2dArray(RB_RULE_BASED_IDS, rows, cols, status);
        
    if (U_SUCCESS(status) && (cols == 3)) {
        for (int32_t i=0; i<rows; ++i) {
            const UnicodeString* row = ruleBasedIDs[i];
            for (int32_t col=0; col<2; ++col) {
                
                if (row[col].length() > 0) {
                    CacheEntry* entry = new CacheEntry();
                    entry->entryType = (col == 0) ?
                        CacheEntry::RULE_BASED_PLACEHOLDER :
                        CacheEntry::REVERSE_RULE_BASED_PLACEHOLDER;
                    entry->rbFile = row[2];
                    uhash_putKey(cache, hash(row[col]), entry, &status);

                    /* It's okay to take the address of the string
                     * from the resource bundle under the assumption
                     * that the RB is caching these, and that they
                     * stay around forever.  If this changes, what we
                     * need to do is change the id vector so that it
                     * owns its strings and create a copy here.
                     */
                    cacheIDs.addElement((void*) &row[col]);
                }
            }
        }
    }

    // Manually add prototypes that the system knows about to the
    // cache.  This is how new non-rule-based transliterators are
    // added to the system.

    status = U_ZERO_ERROR; // Reset status for following calls
    _registerInstance(new HexToUnicodeTransliterator(), status);
    _registerInstance(new UnicodeToHexTransliterator(), status);
    _registerInstance(new JamoHangulTransliterator(), status);
    _registerInstance(new HangulJamoTransliterator(), status);
    _registerInstance(new NullTransliterator(), status);

    cacheInitialized = TRUE;
}

Transliterator::CacheEntry::CacheEntry() {
    u.prototype = 0;
    entryType = NONE;
}

Transliterator::CacheEntry::~CacheEntry() {
    if (entryType == PROTOTYPE) {
        delete u.prototype;
    }
}

void Transliterator::CacheEntry::adoptPrototype(Transliterator* adopted) {
    if (entryType == PROTOTYPE) {
        delete u.prototype;
    }
    entryType = PROTOTYPE;
    u.prototype = adopted;
}
