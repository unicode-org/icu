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

void RuleBasedTransliterator::_construct(const UnicodeString& rules,
                                         Direction direction,
                                         UErrorCode& status) {
    data = 0;
    isDataOwned = TRUE;
    if (U_SUCCESS(status)) {
        data = TransliterationRuleParser::parse(rules, direction);
        if (data == 0) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        } else {
            setMaximumContextLength(data->ruleSet.getMaximumContextLength());
        }
    }
}

RuleBasedTransliterator::RuleBasedTransliterator(const UnicodeString& ID,
                                 const TransliterationRuleData* theData,
                                 UnicodeFilter* adoptedFilter) :
    Transliterator(ID, adoptedFilter),
    data((TransliterationRuleData*)theData), // cast away const
    isDataOwned(FALSE) {
    setMaximumContextLength(data->ruleSet.getMaximumContextLength());
}

/**
 * Copy constructor.  Since the data object is immutable, we can share
 * it with other objects -- no need to clone it.
 */
RuleBasedTransliterator::RuleBasedTransliterator(
        const RuleBasedTransliterator& other) :
    Transliterator(other), data(other.data) {
    // TODO: Finish this -- implement with correct data ownership handling
    if (other.isDataOwned) {
        // TODO: At this point we need to make our own copy of the data.
    } else {
        isDataOwned = FALSE;
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
RuleBasedTransliterator::handleTransliterate(Replaceable& text, Position& index,
                                             bool_t isIncremental) const {
    /* We keep start and limit fixed the entire time,
     * relative to the text -- limit may move numerically if text is
     * inserted or removed.  The cursor moves from start to limit, with
     * replacements happening under it.
     *
     * Example: rules 1. ab>x|y
     *                2. yc>z
     *
     * |eabcd   start - no match, advance cursor
     * e|abcd   match rule 1 - change text & adjust cursor
     * ex|ycd   match rule 2 - change text & adjust cursor
     * exz|d    no match, advance cursor
     * exzd|    done
     */

    int32_t start = index.start;
    int32_t limit = index.limit;
    int32_t cursor = index.cursor;

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
    uint32_t loopLimit = limit - cursor;
    if (loopLimit >= 0x10000000) {
        loopLimit = 0xFFFFFFFF;
    } else {
        loopLimit <<= 4;
    }

    bool_t isPartial = FALSE;

    while (cursor < limit && loopCount <= loopLimit) {
        TransliterationRule* r = isIncremental ?
            data->ruleSet.findIncrementalMatch(text, start, limit, cursor,
                                               *data, isPartial,
                                               getFilter()) :
            data->ruleSet.findMatch(text, start, limit,
                                    cursor, *data,
                                    getFilter());

        /* If we match a rule then apply it by replacing the key
         * with the rule output and repositioning the cursor
         * appropriately.  If we get a partial match, then we
         * can't do anything without more text; return with the
         * cursor at the current position.  If we get null, then
         * there is no match at this position, and we can advance
         * the cursor.
         */
        if (r == 0) {
            if (isPartial) { // always FALSE unless isIncremental
                break;
            } else {
                ++cursor;
            }
        } else {
            text.handleReplaceBetween(cursor, cursor + r->getKeyLength(),
                                      r->getOutput());
            limit += r->getOutput().length() - r->getKeyLength();
            cursor += r->getCursorPos();
            ++loopCount;
        }
    }

    index.limit = limit;
    index.cursor = cursor;
}
