/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   08/10/2001  aliu        Creation.
**********************************************************************
*/
#include "transreg.h"
#include "rbt_data.h"
#include "rbt_pars.h"
#include "unicode/cpdtrans.h"
#include "unicode/nultrans.h"
#include "unicode/parseerr.h"
#include "unicode/rbt.h"
#include "unicode/resbund.h"
#include "unicode/translit.h"
#include "unicode/uscript.h"

// UChar constants
static const UChar LOCALE_SEP  = 95; // '_'
static const UChar ID_SEP      = 0x002D; /*-*/
static const UChar VARIANT_SEP = 0x002F; // '/'

// String constants
static const UChar NO_VARIANT[] = { 0 }; // empty string
static const UChar ANY[] = { 65, 110, 121, 0 }; // Any

/**
 * Resource bundle key for the RuleBasedTransliterator rule.
 */
static const char* RB_RULE = "Rule";

//----------------------------------------------------------------------
// class CharString
//----------------------------------------------------------------------

class CharString {
 public:
    CharString(const UnicodeString& str);
    ~CharString();
    operator char*() { return ptr; }
 private:
    char buf[128];
    char* ptr;
};

CharString::CharString(const UnicodeString& str) {
    if (str.length() >= (int32_t)sizeof(buf)) {
        ptr = new char[str.length() + 8];
    } else {
        ptr = buf;
    }
    str.extract(0, 0x7FFFFFFF, ptr, "");
}

CharString::~CharString() {
    if (ptr != buf) {
        delete[] ptr;
    }
}

//----------------------------------------------------------------------
// class Spec
//----------------------------------------------------------------------

/**
 * A Spec is a string specifying either a source or a target.  In more
 * general terms, it may also specify a variant, but we only use the
 * Spec class for sources and targets.
 *
 * A Spec may be a locale or a script.  If it is a locale, it has a
 * fallback chain that goes xx_YY_ZZZ -> xx_YY -> xx -> ssss, where
 * ssss is the script mapping of xx_YY_ZZZ.  The Spec API methods
 * hasFallback(), next(), and reset() iterate over this fallback
 * sequence.
 *
 * The Spec class canonicalizes itself, so the locale is put into
 * canonical form, or the script is transformed from an abbreviation
 * to a full name.
 */
class Spec {
 public:
    Spec(const UnicodeString& spec);
    ~Spec();

    const UnicodeString& get() const;
    UBool hasFallback() const;
    const UnicodeString& next();
    void reset();

    UBool isLocale() const;
    ResourceBundle& getBundle() const;

    operator const UnicodeString&() const { return get(); }
    const UnicodeString& getTop() const { return top; }

 private:
    void setupNext();

    UnicodeString top;
    UnicodeString spec;
    UnicodeString nextSpec;
    UnicodeString scriptName;
    UBool isSpecLocale; // TRUE if spec is a locale
    UBool isNextLocale; // TRUE if nextSpec is a locale
    ResourceBundle* res;
};

Spec::Spec(const UnicodeString& theSpec) : top(theSpec) {
    UErrorCode status = U_ZERO_ERROR;
    CharString topch(top);
    Locale toploc(topch);
    res = new ResourceBundle(u_getDataDirectory(), toploc, status);
    if (U_FAILURE(status) ||
        status == U_USING_DEFAULT_ERROR) {
        delete res;
        res = 0;
    }

    // Canonicalize script name -or- do locale->script mapping
    status = U_ZERO_ERROR;
    CharString spc(top);
    UScriptCode s = uscript_getCode(spc, &status);
    if (s != USCRIPT_INVALID_CODE
        && s != USCRIPT_KATAKANA // TEMPORARY! REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME REMOVE ME
) {
        scriptName = UnicodeString(uscript_getName(s), "");
    }

    // Canonicalize top
    char buf[256];
    if (res != 0) {
        // Canonicalize locale name
        status = U_ZERO_ERROR;
        buf[uloc_getName(spc, buf, sizeof(buf)-1, &status)] = 0;
        if (U_SUCCESS(status)) {
            top = UnicodeString(buf, "");
        }
    } else if (scriptName.length() != 0) {
        // We are a script; use canonical name
        top = scriptName;
    }

    // assert(spec != top);
    reset();
}

Spec::~Spec() {
    delete res;
}

UBool Spec::hasFallback() const {
    return nextSpec.length() != 0;
}

void Spec::reset() {
    if (spec != top) {
        spec = top;
        isSpecLocale = (res != 0);
        setupNext();
    }
}

void Spec::setupNext() {
    isNextLocale = FALSE;
    if (isSpecLocale) {
        nextSpec = spec;
        int32_t i = nextSpec.lastIndexOf(LOCALE_SEP);
        // If i == 0 then we have _FOO, so we fall through
        // to the scriptName.
        if (i > 0) {
            nextSpec.truncate(i);
            isNextLocale = TRUE;
        } else {
            nextSpec = scriptName; // scriptName may be empty
        }
    } else {
        // spec is a script, so we are at the end
        nextSpec.truncate(0);
    }
}

// Protocol:
// for(const UnicodeString& s(spec.get());
//     spec.hasFallback(); s(spec.next())) { ...

const UnicodeString& Spec::next() {
    spec = nextSpec;
    isSpecLocale = isNextLocale;
    setupNext();
    return spec;
}

const UnicodeString& Spec::get() const {
    return spec;
}

UBool Spec::isLocale() const {
    return isSpecLocale;
}

ResourceBundle& Spec::getBundle() const {
    return *res;
}

//----------------------------------------------------------------------
// class Entry
//----------------------------------------------------------------------

/**
 * The Entry object stores objects of different types and
 * singleton objects as placeholders for rule-based transliterators to
 * be built as needed.  Instances of this struct can be placeholders,
 * can represent prototype transliterators to be cloned, or can
 * represent TransliteratorData objects.  We don't support storing
 * classes in the registry because we don't have the rtti infrastructure
 * for it.  We could easily add this if there is a need for it in the
 * future.
 */
class Entry {
public:
    enum Type {
        RULES_FORWARD,
        RULES_REVERSE,
        LOCALE_RULES,
        PROTOTYPE,
        RBT_DATA,
        COMPOUND_RBT,
        ALIAS,
        FACTORY,
        NONE // Only used for uninitialized entries
    } entryType;
    // NOTE: stringArg cannot go inside the union because
    // it has a copy constructor
    UnicodeString stringArg; // For RULES_*, ALIAS, COMPOUND_RBT
    int32_t intArg; // For COMPOUND_RBT
    union {
        Transliterator* prototype; // For PROTOTYPE
        TransliterationRuleData* data; // For RBT_DATA, COMPOUND_RBT
        Transliterator::Factory factory; // For FACTORY
    } u;
    Entry();
    ~Entry();
    void adoptPrototype(Transliterator* adopted);
    void setFactory(Transliterator::Factory factory);
};

Entry::Entry() {
    u.prototype = 0;
    entryType = NONE;
}

Entry::~Entry() {
    if (entryType == PROTOTYPE) {
        delete u.prototype;
    } else if (entryType == RBT_DATA) {
        // The data object is shared between instances of RBT.  The
        // entry object owns it.  It should only be deleted when the
        // transliterator component is being cleaned up.  Doing so
        // invalidates any RBTs that the user has instantiated.
        delete u.data;
    }
}

void Entry::adoptPrototype(Transliterator* adopted) {
    if (entryType == PROTOTYPE) {
        delete u.prototype;
    }
    entryType = PROTOTYPE;
    u.prototype = adopted;
}

void Entry::setFactory(Transliterator::Factory factory) {
    if (entryType == PROTOTYPE) {
        delete u.prototype;
    }
    entryType = FACTORY;
    u.factory = factory;
}

// UObjectDeleter for Hashtable::setValueDeleter
static void deleteEntry(void* obj) {
    delete (Entry*) obj;
}

//----------------------------------------------------------------------
// class TransliteratorRegistry: Basic public API
//----------------------------------------------------------------------

TransliteratorRegistry::TransliteratorRegistry(UErrorCode& status) :
    registry(TRUE),
    specDAG(TRUE),
    availableIDs(status)
{
    registry.setValueDeleter(deleteEntry);
    availableIDs.setDeleter(uhash_deleteUnicodeString);
    availableIDs.setComparer(uhash_compareCaselessUnicodeString);
    specDAG.setValueDeleter(uhash_deleteHashtable);
}

TransliteratorRegistry::~TransliteratorRegistry() {
    // Through the magic of C++, everything cleans itself up
}

Transliterator* TransliteratorRegistry::get(const UnicodeString& ID,
                                            UnicodeString& aliasReturn,
                                            UParseError& parseError,
                                            UErrorCode& status) {
    Entry *entry = find(ID);
    return (entry == 0) ? 0
        : instantiateEntry(ID, entry, aliasReturn, parseError,status);
}

void TransliteratorRegistry::put(Transliterator* adoptedProto,
                                 UBool visible) {
    Entry *entry = new Entry();
    entry->adoptPrototype(adoptedProto);
    registerEntry(adoptedProto->getID(), entry, visible);
}

void TransliteratorRegistry::put(const UnicodeString& ID,
                                 Transliterator::Factory factory,
                                 UBool visible) {
    Entry *entry = new Entry();
    entry->setFactory(factory);
    registerEntry(ID, entry, visible);
}

void TransliteratorRegistry::put(const UnicodeString& ID,
                                 const UnicodeString& resourceName,
                                 UTransDirection dir,
                                 UBool visible) {
    Entry *entry = new Entry();
    entry->entryType = (dir == UTRANS_FORWARD) ? Entry::RULES_FORWARD
        : Entry::RULES_REVERSE;
    entry->stringArg = resourceName;
    registerEntry(ID, entry, visible);
}

void TransliteratorRegistry::put(const UnicodeString& ID,
                                 const UnicodeString& alias,
                                 UBool visible) {
    Entry *entry = new Entry();
    entry->entryType = Entry::ALIAS;
    entry->stringArg = alias;
    registerEntry(ID, entry, visible);
}

void TransliteratorRegistry::remove(const UnicodeString& ID) {
    UnicodeString source, target, variant;
    IDtoSTV(ID, source, target, variant);
    // Only need to do this if ID.indexOf('-') < 0
    UnicodeString id;
    STVtoID(source, target, variant, id);
    registry.remove(id);
    removeSTV(source, target, variant);
    availableIDs.removeElement((void*) &id);
}

//----------------------------------------------------------------------
// class TransliteratorRegistry: Public ID and spec management
//----------------------------------------------------------------------

/**
 * Return the number of IDs currently registered with the system.
 * To retrieve the actual IDs, call getAvailableID(i) with
 * i from 0 to countAvailableIDs() - 1.
 */
int32_t TransliteratorRegistry::countAvailableIDs(void) {
    return availableIDs.size();
}

/**
 * Return the index-th available ID.  index must be between 0
 * and countAvailableIDs() - 1, inclusive.  If index is out of
 * range, the result of getAvailableID(0) is returned.
 */
const UnicodeString& TransliteratorRegistry::getAvailableID(int32_t index) {
    if (index < 0 || index >= availableIDs.size()) {
        index = 0;
    }
    return *(const UnicodeString*) availableIDs[index];
}

int32_t TransliteratorRegistry::countAvailableSources(void) {
    return specDAG.count();
}

UnicodeString& TransliteratorRegistry::getAvailableSource(int32_t index,
                                                          UnicodeString& result) {
    int32_t pos = -1;
    const UHashElement *e = 0;
    while (index-- >= 0) {
        e = specDAG.nextElement(pos);
        if (e == 0) {
            break;
        }
    }
    if (e == 0) {
        result.truncate(0);
    } else {
        result = *(UnicodeString*) e->key.pointer;
    }
    return result;
}

int32_t TransliteratorRegistry::countAvailableTargets(const UnicodeString& source) {
    Hashtable *targets = (Hashtable*) specDAG.get(source);
    return (targets == 0) ? 0 : targets->count();
}

UnicodeString& TransliteratorRegistry::getAvailableTarget(int32_t index,
                                                          const UnicodeString& source,
                                                          UnicodeString& result) {
    Hashtable *targets = (Hashtable*) specDAG.get(source);
    if (targets == 0) {
        result.truncate(0); // invalid source
        return result;
    }
    int32_t pos = -1;
    const UHashElement *e = 0;
    while (index-- >= 0) {
        e = targets->nextElement(pos);
        if (e == 0) {
            break;
        }
    }
    if (e == 0) {
        result.truncate(0); // invalid index
    } else {
        result = *(UnicodeString*) e->key.pointer;
    }
    return result;
}

int32_t TransliteratorRegistry::countAvailableVariants(const UnicodeString& source,
                                                       const UnicodeString& target) {
    Hashtable *targets = (Hashtable*) specDAG.get(source);
    if (targets == 0) {
        return 0;
    }
    UVector *variants = (UVector*) targets->get(target);
    // variants may be 0 if the source/target are invalid
    return (variants == 0) ? 0 : variants->size();
}

UnicodeString& TransliteratorRegistry::getAvailableVariant(int32_t index,
                                                           const UnicodeString& source,
                                                           const UnicodeString& target,
                                                           UnicodeString& result) {
    Hashtable *targets = (Hashtable*) specDAG.get(source);
    if (targets == 0) {
        result.truncate(0); // invalid source
        return result;
    }
    UVector *variants = (UVector*) targets->get(target);
    if (variants == 0) {
        result.truncate(0); // invalid target
        return result;
    }
    UnicodeString *v = (UnicodeString*) variants->elementAt(index);
    if (v == 0) {
        result.truncate(0); // invalid index
    } else {
        result = *v;
    }
    return result;
}

//----------------------------------------------------------------------
// class TransliteratorRegistry: internal
//----------------------------------------------------------------------

/**
 * Given an ID, parse it into source, target, and variant strings.
 * The variant may be empty.  If the source is empty it will be set to
 * "Any".
 */
void TransliteratorRegistry::IDtoSTV(const UnicodeString& id,
                                     UnicodeString& source,
                                     UnicodeString& target,
                                     UnicodeString& variant) {
    int32_t dash = id.indexOf(ID_SEP);
    int32_t stroke = id.indexOf(VARIANT_SEP);
    int32_t start = 0;
    int32_t limit = id.length();
    if (dash < 0) {
        source = ANY;
    } else {
        id.extractBetween(0, dash, source);
        start = dash + 1;
    }
    if (stroke >= 0) {
        id.extractBetween(stroke + 1, id.length(), variant);
        limit = stroke;
    }
    id.extractBetween(start, limit, target);
}

/**
 * Given source, target, and variant strings, concatenate them into a
 * full ID.  If the source is empty, then "Any" will be used for the
 * source, so the ID will always be of the form s-t/v or s-t.
 */
void TransliteratorRegistry::STVtoID(const UnicodeString& source,
                                     const UnicodeString& target,
                                     const UnicodeString& variant,
                                     UnicodeString& id) {
    id = source;
    if (id.length() == 0) {
        id = ANY;
    }
    id.append(ID_SEP).append(target);
    if (variant.length() != 0) {
        id.append(VARIANT_SEP).append(variant);
    }
}

/**
 * Convenience method.  Calls 6-arg registerEntry().
 */
void TransliteratorRegistry::registerEntry(const UnicodeString& source,
                                           const UnicodeString& target,
                                           const UnicodeString& variant,
                                           Entry* adopted,
                                           UBool visible) {
    UnicodeString ID;
    UnicodeString s(source);
    if (s.length() == 0) {
        s = "Any";
    }
    STVtoID(source, target, variant, ID);
    registerEntry(ID, s, target, variant, adopted, visible);
}

/**
 * Convenience method.  Calls 6-arg registerEntry().
 */
void TransliteratorRegistry::registerEntry(const UnicodeString& ID,
                                           Entry* adopted,
                                           UBool visible) {
    UnicodeString source, target, variant;
    IDtoSTV(ID, source, target, variant);
    // Only need to do this if ID.indexOf('-') < 0
    UnicodeString id;
    STVtoID(source, target, variant, id);
    registerEntry(id, source, target, variant, adopted, visible);
}

/**
 * Register an entry object (adopted) with the given ID, source,
 * target, and variant strings.
 */
void TransliteratorRegistry::registerEntry(const UnicodeString& ID,
                                           const UnicodeString& source,
                                           const UnicodeString& target,
                                           const UnicodeString& variant,
                                           Entry* adopted,
                                           UBool visible) {
    UErrorCode status = U_ZERO_ERROR;
    registry.put(ID, adopted, status);
    if (visible) {
        registerSTV(source, target, variant);
        if (!availableIDs.contains((void*) &ID)) {
            availableIDs.addElement(new UnicodeString(ID), status);
        }
    } else {
        removeSTV(source, target, variant);
        availableIDs.removeElement((void*) &ID);
    }
}

/**
 * Register a source-target/variant in the specDAG.  Variant may be
 * empty, but source and target must not be.  If variant is empty then
 * the special variant NO_VARIANT is stored in slot zero of the
 * UVector of variants.
 */
void TransliteratorRegistry::registerSTV(const UnicodeString& source,
                                         const UnicodeString& target,
                                         const UnicodeString& variant) {
    // assert(source.length() > 0);
    // assert(target.length() > 0);
    UErrorCode status = U_ZERO_ERROR;
    Hashtable *targets = (Hashtable*) specDAG.get(source);
    if (targets == 0) {
        targets = new Hashtable(TRUE);
        if (targets == 0) {
            return;
        }
        targets->setValueDeleter(uhash_deleteUVector);
        specDAG.put(source, targets, status);
    }
    UVector *variants = (UVector*) targets->get(target);
    if (variants == 0) {
        variants = new UVector(uhash_deleteUnicodeString,
                               uhash_compareCaselessUnicodeString, status);
        if (variants == 0) {
            return;
        }
        targets->put(target, variants, status);
    }
    // assert(NO_VARIANT == "");
    // We add the variant string.  If it is the special "no variant"
    // string, that is, the empty string, we add it at position zero.
    if (!variants->contains((void*) &variant)) {
        if (variant.length() > 0) {
            variants->addElement(new UnicodeString(variant), status);
        } else {
            variants->insertElementAt(new UnicodeString(NO_VARIANT), 0, status);
        }
    }
}

/**
 * Remove a source-target/variant from the specDAG.
 */
void TransliteratorRegistry::removeSTV(const UnicodeString& source,
                                       const UnicodeString& target,
                                       const UnicodeString& variant) {
    // assert(source.length() > 0);
    // assert(target.length() > 0);
//    UErrorCode status = U_ZERO_ERROR;
    Hashtable *targets = (Hashtable*) specDAG.get(source);
    if (targets == 0) {
        return; // should never happen for valid s-t/v
    }
    UVector *variants = (UVector*) targets->get(target);
    if (variants == 0) {
        return; // should never happen for valid s-t/v
    }
    variants->removeElement((void*) &variant);
    if (variants->size() == 0) {
        targets->remove(target); // should delete variants
        if (targets->count() == 0) {
            specDAG.remove(source); // should delete targets
        }
    }
}

/**
 * Attempt to find a source-target/variant in the dynamic registry
 * store.  Return 0 on failure.
 *
 * Caller does NOT own returned object.
 */
Entry* TransliteratorRegistry::findInDynamicStore(const Spec& src,
                                                  const Spec& trg,
                                                  const UnicodeString& variant) {
    UnicodeString ID;
    STVtoID(src, trg, variant, ID);
    return (Entry*) registry.get(ID);
}

/**
 * Attempt to find a source-target/variant in the static locale
 * resource store.  Do not perform fallback.  Return 0 on failure.
 *
 * On success, create a new entry object, register it in the dynamic
 * store, and return a pointer to it, but do not make it public --
 * just because someone requested something, we do not expand the
 * available ID list (or spec DAG).
 *
 * Caller does NOT own returned object.
 */
Entry* TransliteratorRegistry::findInStaticStore(const Spec& src,
                                                 const Spec& trg,
                                                 const UnicodeString& variant) {
    Entry* entry = 0;
    if (src.isLocale()) {
        entry = findInBundle(src, trg, variant,
                             "TransliterateTo");
    } else if (trg.isLocale()) {
        entry = findInBundle(trg, src, variant,
                             "TransliterateFrom");
    }

    // If we found an entry, store it in the Hashtable for next
    // time.
    if (entry != 0) {
        registerEntry(src.getTop(), trg.getTop(), variant, entry, FALSE);
    }

    return entry;
}

/**
 * Attempt to find an entry in a single resource bundle.  This is
 * a one-sided lookup.  findInStaticStore() performs up to two such
 * lookups, one for the source, and one for the target.
 *
 * Do not perform fallback.  Return 0 on failure.
 *
 * On success, create a new Entry object, populate it, and return it.
 * The caller owns the returned object.
 */
Entry* TransliteratorRegistry::findInBundle(const Spec& specToOpen,
                                            const Spec& specToFind,
                                            const UnicodeString& variant,
                                            const char* tagPrefix) {

    UnicodeString utag(tagPrefix);
    utag.append(LOCALE_SEP).append(specToFind.get());
    CharString tag(utag);

    UErrorCode status = U_ZERO_ERROR;
    ResourceBundle subres(specToOpen.getBundle().get(tag, status));
    if (U_FAILURE(status) ||
        status == U_USING_DEFAULT_ERROR) {
        return 0;
    }

    if (specToOpen.get() != subres.getLocale().getName()) {
        return 0;
    }

    UnicodeString resStr;
    if (variant.length() != 0) {
        CharString var(variant);
        status = U_ZERO_ERROR;
        UnicodeString resStr = subres.getStringEx(var, status);
        if (U_FAILURE(status)) {
            return 0;
        }
    }

    else {
        // Variant is empty, which means match the first variant listed.
        status = U_ZERO_ERROR;
        ResourceBundle subsub(subres.getNext(status));
        if (U_FAILURE(status)) {
            return 0;
        }
        resStr = subsub.getNextString(status);
        if (U_FAILURE(status)) {
            return 0;
        }
    }

    // We have succeeded in loading a string from the locale
    // resources.  Create a new registry entry to hold it and return it.
    Entry *entry = new Entry();
    if (entry != 0) {
        entry->entryType = Entry::LOCALE_RULES;
        entry->stringArg = resStr;
    }

    return entry;
}

/**
 * Convenience method.  Calls 3-arg find().
 */
Entry* TransliteratorRegistry::find(const UnicodeString& ID) {
    UnicodeString source, target, variant;
    IDtoSTV(ID, source, target, variant);
    return find(source, target, variant);
}

/**
 * Top-level find method.  Attempt to find a source-target/variant in
 * either the dynamic or the static (locale resource) store.  Perform
 * fallback.
 * 
 * Lookup sequence for ss_SS_SSS-tt_TT_TTT/v:
 *
 *   ss_SS_SSS-tt_TT_TTT/v -- in hashtable
 *   ss_SS_SSS-tt_TT_TTT/v -- in ss_SS_SSS (no fallback)
 * 
 *     repeat with t = tt_TT_TTT, tt_TT, tt, and tscript
 *
 *     ss_SS_SSS-t/*
 *     ss_SS-t/*
 *     ss-t/*
 *     sscript-t/*
 *
 * Here * matches the first variant listed.
 *
 * Caller does NOT own returned object.  Return 0 on failure.
 */
Entry* TransliteratorRegistry::find(UnicodeString& source,
                                    UnicodeString& target,
                                    UnicodeString& variant) {
    
    Spec src(source);
    Spec trg(target);
    Entry* entry;

    if (variant.length() != 0) {
        
        // Seek exact match in hashtable
        entry = findInDynamicStore(src, trg, variant);
        if (entry != 0) {
            return entry;
        }
        
        // Seek exact match in locale resources
        entry = findInStaticStore(src, trg, variant);
        if (entry != 0) {
            return entry;
        }
    }

    for (;;) {
        src.reset();
        for (;;) {
            // Seek match in hashtable
            entry = findInDynamicStore(src, trg, NO_VARIANT);
            if (entry != 0) {
                return entry;
            }
            
            // Seek match in locale resources
            entry = findInStaticStore(src, trg, NO_VARIANT);
            if (entry != 0) {
                return entry;
            }
            if (!src.hasFallback()) {
                break;
            }
            src.next();
        }
        if (!trg.hasFallback()) {
            break;
        }
        trg.next();
    }

    return 0;
}

/**
 * Given an Entry object, instantiate it.  Caller owns result.  Return
 * 0 on failure.
 *
 * Return a non-empty aliasReturn value if the ID points to an alias.
 * We cannot instantiate it ourselves because the alias may contain
 * filters or compounds, which we do not understand.  Caller should
 * make aliasReturn empty before calling.
 *
 * The entry object is assumed to reside in the dynamic store.  It may be
 * modified.
 */
Transliterator* TransliteratorRegistry::instantiateEntry(const UnicodeString& ID,
                                                         Entry *entry,
                                                         UnicodeString &aliasReturn,
                                                         UParseError& parseError,
                                                         UErrorCode& status) {

    for (;;) {
        if (entry->entryType == Entry::RBT_DATA) {
            return new RuleBasedTransliterator(ID, entry->u.data);
        } else if (entry->entryType == Entry::PROTOTYPE) {
            return entry->u.prototype->clone();
        } else if (entry->entryType == Entry::ALIAS) {
            aliasReturn = entry->stringArg;
            return 0;
        } else if (entry->entryType == Entry::FACTORY) {
            return entry->u.factory();
        } else if (entry->entryType == Entry::COMPOUND_RBT) {
            UnicodeString id("_", "");
            Transliterator *t = new RuleBasedTransliterator(id, entry->u.data);
            t = new CompoundTransliterator(ID, entry->stringArg,
                                           entry->intArg, t, parseError, status);
            if (U_FAILURE(status)) {
                delete t;
                t = 0;
                remove(ID);
            }
            return t;
        }

        // At this point entry type must be either RULES_FORWARD or
        // RULES_REVERSE.  We process the rule data into a
        // TransliteratorRuleData object, and possibly also into an
        // ::id header and/or footer.  Then we modify the registry with
        // the parsed data and retry.
        UBool isReverse = (entry->entryType == Entry::RULES_REVERSE);

        // We use the file name, taken from another resource bundle
        // 2-d array at static init time, as a locale language.  We're
        // just using the locale mechanism to map through to a file
        // name; this in no way represents an actual locale.
        CharString ch(entry->stringArg);
        Locale fakeLocale(ch);
        ResourceBundle bundle((char*)0, fakeLocale, status);
        UnicodeString rules = bundle.getStringEx(RB_RULE, status);

        // If the status indicates a failure, then we don't have any
        // rules -- there is probably an installation error.  The list
        // in the root locale should correspond to all the installed
        // transliterators; if it lists something that's not
        // installed, we'll get an error from ResourceBundle.

        TransliteratorParser::parse(rules, isReverse ?
                                    UTRANS_REVERSE : UTRANS_FORWARD,
                                    entry->u.data,
                                    entry->stringArg,
                                    entry->intArg,
                                    parseError,
                                    status);

        if (U_FAILURE(status)) {
            // We have a failure of some kind.  Remove the ID from the
            // registry so we don't keep trying.  NOTE: This will throw off
            // anyone who is, at the moment, trying to iterate over the
            // available IDs.  That's acceptable since we should never
            // really get here except under installation, configuration,
            // or unrecoverable run time memory failures.
            remove(ID);
            break;
        }

        // Reset entry->entryType to something that we process at the
        // top of the loop, then loop back to the top.  As long as we
        // do this, we only loop through twice at most.
        // NOTE: The logic here matches that in
        // Transliterator::createFromRules().
        if (entry->stringArg.length() == 0) {
            if (entry->u.data == 0) {
                // No idBlock, no data -- this is just an
                // alias for Null
                entry->entryType = Entry::ALIAS;
                entry->stringArg = NullTransliterator::ID;
            } else {
                // No idBlock, data != 0 -- this is an
                // ordinary RBT_DATA
                entry->entryType = Entry::RBT_DATA;
            }
        } else {
            if (entry->u.data == 0) {
                // idBlock, no data -- this is an alias
                entry->entryType = Entry::ALIAS;
            } else {
                // idBlock and data -- this is a compound
                // RBT
                entry->entryType = Entry::COMPOUND_RBT;
            }
        }
    }

    return 0; // failed
}

//eof
