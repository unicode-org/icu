/*
**********************************************************************
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/rep.h"
#include "unicode/uniset.h"
#include "rbt_pars.h"
#include "rbt_data.h"
#include "rbt_rule.h"
#include "rbt.h"

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(RuleBasedTransliterator)

void RuleBasedTransliterator::_construct(const UnicodeString& rules,
                                         UTransDirection direction,
                                         UParseError& parseError,
                                         UErrorCode& status) {
    data = 0;
    isDataOwned = TRUE;
    if (U_FAILURE(status)) {
        return;
    }

    TransliteratorParser parser;
    parser.parse(rules, direction, parseError, status);
    if (U_FAILURE(status)) {
        return;
    }

    if (parser.idBlock.length() != 0 ||
        parser.compoundFilter != NULL) {
        status = U_INVALID_RBT_SYNTAX; // ::ID blocks disallowed in RBT
        return;
    }

    data = parser.orphanData();
    setMaximumContextLength(data->ruleSet.getMaximumContextLength());
}

/**
 * Constructs a new transliterator from the given rules.
 * @param id            the id for the transliterator.
 * @param rules         rules, separated by ';'
 * @param direction     either FORWARD or REVERSE.
 * @param adoptedFilter the filter for this transliterator.
 * @param parseError    Struct to recieve information on position 
 *                      of error if an error is encountered
 * @param status        Output param set to success/failure code.
 * @exception IllegalArgumentException if rules are malformed
 * or direction is invalid.
 */
RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& id,
                            const UnicodeString& rules,
                            UTransDirection direction,
                            UnicodeFilter* adoptedFilter,
                            UParseError& parseError,
                            UErrorCode& status) :
    Transliterator(id, adoptedFilter) {
    _construct(rules, direction,parseError,status);
}

/**
 * Constructs a new transliterator from the given rules.
 * @param id            the id for the transliterator.
 * @param rules         rules, separated by ';'
 * @param direction     either FORWARD or REVERSE.
 * @param adoptedFilter the filter for this transliterator.
 * @param status        Output param set to success/failure code.
 * @exception IllegalArgumentException if rules are malformed
 * or direction is invalid.
 */
RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& id,
                            const UnicodeString& rules,
                            UTransDirection direction,
                            UnicodeFilter* adoptedFilter,
                            UErrorCode& status) :
    Transliterator(id, adoptedFilter) {
    UParseError parseError;
    _construct(rules, direction,parseError, status);
}

/**
 * Covenience constructor with no filter.
 */
RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& id,
                            const UnicodeString& rules,
                            UTransDirection direction,
                            UErrorCode& status) :
    Transliterator(id, 0) {
    UParseError parseError;
    _construct(rules, direction,parseError, status);
}

/**
 * Covenience constructor with no filter and FORWARD direction.
 */
RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& id,
                            const UnicodeString& rules,
                            UErrorCode& status) :
    Transliterator(id, 0) {
    UParseError parseError;
    _construct(rules, UTRANS_FORWARD, parseError, status);
}

/**
 * Covenience constructor with FORWARD direction.
 */
RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& id,
                            const UnicodeString& rules,
                            UnicodeFilter* adoptedFilter,
                            UErrorCode& status) :
    Transliterator(id, adoptedFilter) {
    UParseError parseError;
    _construct(rules, UTRANS_FORWARD,parseError, status);
}

RuleBasedTransliterator::RuleBasedTransliterator(const UnicodeString& id,
                                 const TransliterationRuleData* theData,
                                 UnicodeFilter* adoptedFilter) :
    Transliterator(id, adoptedFilter),
    data((TransliterationRuleData*)theData), // cast away const
    isDataOwned(FALSE) {
    setMaximumContextLength(data->ruleSet.getMaximumContextLength());
}

/**
 * Internal constructor.
 */
RuleBasedTransliterator::RuleBasedTransliterator(const UnicodeString& id,
                                                 TransliterationRuleData* theData,
                                                 UBool isDataAdopted) :
    Transliterator(id, 0),
    data(theData),
    isDataOwned(isDataAdopted) {
    setMaximumContextLength(data->ruleSet.getMaximumContextLength());
}

/**
 * Copy constructor.
 */
RuleBasedTransliterator::RuleBasedTransliterator(
        const RuleBasedTransliterator& other) :
    Transliterator(other), data(other.data),
    isDataOwned(other.isDataOwned) {

    // The data object may or may not be owned.  If it is not owned we
    // share it; it is invariant.  If it is owned, it's still
    // invariant, but we need to copy it to prevent double-deletion.
    // If this becomes a performance issue (if people do a lot of RBT
    // copying -- unlikely) we can reference count the data object.

    // Only do a deep copy if this is owned data, that is, data that
    // will be later deleted.  System transliterators contain
    // non-owned data.
    if (isDataOwned) {
        data = new TransliterationRuleData(*other.data);
    }
}

/**
 * Destructor.
 */
RuleBasedTransliterator::~RuleBasedTransliterator() {
    // Delete the data object only if we own it.
    if (isDataOwned) {
        delete data;
    }
}

Transliterator* // Covariant return NOT ALLOWED (for portability)
RuleBasedTransliterator::clone(void) const {
    return new RuleBasedTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void
RuleBasedTransliterator::handleTransliterate(Replaceable& text, UTransPosition& index,
                                             UBool isIncremental) const {
    /* We keep contextStart and contextLimit fixed the entire time,
     * relative to the text -- contextLimit may move numerically if
     * text is inserted or removed.  The start offset moves toward
     * limit, with replacements happening under it.
     *
     * Example: rules 1. ab>x|y
     *                2. yc>z
     *
     * |eabcd   begin - no match, advance start
     * e|abcd   match rule 1 - change text & adjust start
     * ex|ycd   match rule 2 - change text & adjust start
     * exz|d    no match, advance start
     * exzd|    done
     */

    /* A rule like
     *   a>b|a
     * creates an infinite loop. To prevent that, we put an arbitrary
     * limit on the number of iterations that we take, one that is
     * high enough that any reasonable rules are ok, but low enough to
     * prevent a server from hanging.  The limit is 16 times the
     * number of characters n, unless n is so large that 16n exceeds a
     * uint32_t.
     */
    uint32_t loopCount = 0;
    uint32_t loopLimit = index.limit - index.start;
    if (loopLimit >= 0x10000000) {
        loopLimit = 0xFFFFFFFF;
    } else {
        loopLimit <<= 4;
    }

    while (index.start < index.limit &&
           loopCount <= loopLimit &&
           data->ruleSet.transliterate(text, index, isIncremental)) {
        ++loopCount;
    }
}

UnicodeString& RuleBasedTransliterator::toRules(UnicodeString& rulesSource,
                                                UBool escapeUnprintable) const {
    return data->ruleSet.toRules(rulesSource, escapeUnprintable);
}

/**
 * Implement Transliterator framework
 */
void RuleBasedTransliterator::handleGetSourceSet(UnicodeSet& result) const {
    data->ruleSet.getSourceTargetSet(result, FALSE);
}

/**
 * Override Transliterator framework
 */
UnicodeSet& RuleBasedTransliterator::getTargetSet(UnicodeSet& result) const {
    return data->ruleSet.getSourceTargetSet(result, TRUE);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */
