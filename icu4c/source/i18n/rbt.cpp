/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "unicode/rbt.h"
#include "rbt_pars.h"
#include "rbt_data.h"
#include "rbt_rule.h"
#include "unicode/rep.h"

char RuleBasedTransliterator::fgClassID = 0; // Value is irrelevant

void RuleBasedTransliterator::_construct(const UnicodeString& rules,
                                         UTransDirection direction,
                                         UErrorCode& status,
                                         UParseError* parseError) {
    data = 0;
    isDataOwned = TRUE;
    if (U_SUCCESS(status)) {
        data = TransliteratorParser::parse(rules, direction, parseError);
        if (data == 0) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        } else {
            setMaximumContextLength(data->ruleSet.getMaximumContextLength());
        }
    }
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
 * Copy constructor.  Since the data object is immutable, we can share
 * it with other objects -- no need to clone it.
 */
RuleBasedTransliterator::RuleBasedTransliterator(
        const RuleBasedTransliterator& other) :
    Transliterator(other), data(other.data),
    isDataOwned(other.isDataOwned) {

    // Only do a deep copy if this is non-owned data, that is,
    // data that will be later deleted.  System transliterators
    // contain owned data.
    if (isDataOwned) {
        data = new TransliterationRuleData(*other.data);
    }
}

/**
 * Destructor.  We do NOT own the data object, so we do not delete it.
 */
RuleBasedTransliterator::~RuleBasedTransliterator() {
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
