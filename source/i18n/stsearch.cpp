/*
**********************************************************************
*   Copyright (C) 2001 IBM and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*  03/22/2000   helena      Creation.
**********************************************************************
*/

#include "unicode/stsearch.h"
#include "cmemory.h"
#include "usrchimp.h"

// public constructors and destructors -----------------------------------

StringSearch::StringSearch(const UnicodeString &pattern, 
                           const UnicodeString &text,
                           const Locale        &locale,       
                                 BreakIterator *breakiter,
                                 UErrorCode    &status) :
                           SearchIterator(text, breakiter), 
                           m_collator_(),
                           m_pattern_(pattern)
{
    m_strsrch_ = usearch_open(m_pattern_.fArray, m_pattern_.fLength, 
                              m_text_.fArray, m_text_.fLength, 
                              locale.getName(), NULL, &status);
    uprv_free(m_search_);
    m_search_ = NULL;

    if (U_SUCCESS(status)) {
              int32_t  length;
        const UChar   *rules = ucol_getRules(m_strsrch_->collator, &length);
        m_collation_rules_.setTo(rules, length);
        m_collator_.setUCollator((UCollator *)m_strsrch_->collator,
                                 &m_collation_rules_);
        // m_search_ has been created by the base SearchIterator class
        m_search_        = m_strsrch_->search;
    }
}

StringSearch::StringSearch(const UnicodeString     &pattern, 
                           const UnicodeString     &text,
                                 RuleBasedCollator *coll,       
                                 BreakIterator     *breakiter,
                                 UErrorCode        &status) :
                           SearchIterator(text, breakiter), 
                           m_collator_(),
                           m_pattern_(pattern)
{
    if (coll == NULL) {
        status     = U_ILLEGAL_ARGUMENT_ERROR;
        m_strsrch_ = NULL;
        return;
    }
    m_strsrch_ = usearch_openFromCollator(m_pattern_.fArray, 
                                          m_pattern_.fLength, m_text_.fArray, 
                                          m_text_.fLength, coll->ucollator, 
                                          NULL, &status);
    uprv_free(m_search_);
    m_search_ = NULL;

    if (U_SUCCESS(status)) {
              int32_t  length;
        const UChar   *rules = ucol_getRules(m_strsrch_->collator, &length);
        m_collation_rules_.setTo(rules, length);
        m_collator_.setUCollator((UCollator *)m_strsrch_->collator,
                                 &m_collation_rules_);
        // m_search_ has been created by the base SearchIterator class
        m_search_ = m_strsrch_->search;
    }
}

StringSearch::StringSearch(const UnicodeString     &pattern, 
                                 CharacterIterator &text,
                           const Locale            &locale, 
                                 BreakIterator     *breakiter,
                                 UErrorCode        &status) :
                           SearchIterator(text, breakiter), 
                           m_collator_(),
                           m_pattern_(pattern)
{
    m_strsrch_ = usearch_open(m_pattern_.fArray, m_pattern_.fLength, 
                              m_text_.fArray, m_text_.fLength, 
                              locale.getName(), NULL, &status);
    uprv_free(m_search_);
    m_search_ = NULL;

    if (U_SUCCESS(status)) {
              int32_t  length;
        const UChar   *rules = ucol_getRules(m_strsrch_->collator, &length);
        m_collation_rules_.setTo(rules, length);
        m_collator_.setUCollator((UCollator *)m_strsrch_->collator,
                                 &m_collation_rules_);
        // m_search_ has been created by the base SearchIterator class
        m_search_ = m_strsrch_->search;
    }
}

StringSearch::StringSearch(const UnicodeString     &pattern,
                                 CharacterIterator &text,
                                 RuleBasedCollator *coll, 
                                 BreakIterator     *breakiter,
                                 UErrorCode        &status) :
                           SearchIterator(text, breakiter), 
                           m_collator_(),
                           m_pattern_(pattern)
{
    if (coll == NULL) {
        status     = U_ILLEGAL_ARGUMENT_ERROR;
        m_strsrch_ = NULL;
        return;
    }
    m_strsrch_ = usearch_openFromCollator(m_pattern_.fArray, 
                                          m_pattern_.fLength, m_text_.fArray, 
                                          m_text_.fLength, coll->ucollator, 
                                          NULL, &status);
    uprv_free(m_search_);
    m_search_ = NULL;

    if (U_SUCCESS(status)) {
              int32_t  length;
        const UChar   *rules = ucol_getRules(m_strsrch_->collator, &length);
        m_collation_rules_.setTo(rules, length);
        m_collator_.setUCollator((UCollator *)m_strsrch_->collator,
                                 &m_collation_rules_);
        // m_search_ has been created by the base SearchIterator class
        m_search_ = m_strsrch_->search;
    }
}

StringSearch::StringSearch(const StringSearch &that) :
                       SearchIterator(that.m_text_, that.m_breakiterator_),
                       m_collator_(),
                       m_pattern_(that.m_pattern_)
{
    UErrorCode status = U_ZERO_ERROR;
    if (that.m_strsrch_ == NULL) {
        m_strsrch_ = NULL;
        status     = U_ILLEGAL_ARGUMENT_ERROR;
    }
    else {
        m_strsrch_ = usearch_openFromCollator(m_pattern_.fArray, 
                                              m_pattern_.fLength, 
                                              m_text_.fArray, m_text_.fLength, 
                                              that.m_strsrch_->collator, 
                                              NULL, &status);
    }
    uprv_free(m_search_);
    m_search_ = NULL;

    if (U_SUCCESS(status)) {
              int32_t  length;
        const UChar   *rules = ucol_getRules(m_strsrch_->collator, &length);
        m_collation_rules_.setTo(rules, length);
        m_collator_.setUCollator((UCollator *)m_strsrch_->collator,
                                 &m_collation_rules_);
        // m_search_ has been created by the base SearchIterator class
        m_search_        = m_strsrch_->search;
        m_breakiterator_ = that.m_breakiterator_;
    }
}

StringSearch::~StringSearch()
{
    usearch_close(m_strsrch_);
    m_search_ = NULL;
}

// operator overloading ---------------------------------------------
StringSearch & StringSearch::operator=(const StringSearch &that)
{
    if ((*this) != that) {
        UErrorCode status = U_ZERO_ERROR;
        m_text_          = that.m_text_;
        m_breakiterator_ = that.m_breakiterator_;
        m_pattern_       = that.m_pattern_;
        // all m_search_ in the parent class is linked up with m_strsrch_
        usearch_close(m_strsrch_);
        m_strsrch_ = usearch_openFromCollator(m_pattern_.fArray, 
                                              m_pattern_.fLength, 
                                              m_text_.fArray, 
                                              m_text_.fLength, 
                                              that.m_strsrch_->collator, 
                                              NULL, &status);
              int32_t  length;
        const UChar   *rules = ucol_getRules(m_strsrch_->collator, &length);
        m_collation_rules_.setTo(rules, length);
        m_collator_.setUCollator((UCollator *)m_strsrch_->collator,
                                 &m_collation_rules_);
        m_search_ = m_strsrch_->search;
    }
    return *this;
}

UBool StringSearch::operator==(const SearchIterator &that) const
{
    if (this == &that) {
        return TRUE;
    }
    if (SearchIterator::operator ==(that)) {
        StringSearch &thatsrch = (StringSearch &)that;
        return (this->m_pattern_ == thatsrch.m_pattern_ &&
                this->m_strsrch_->collator == thatsrch.m_strsrch_->collator);
    }
    return FALSE;
}

// public get and set methods ----------------------------------------

void StringSearch::setOffset(UTextOffset position, UErrorCode &status)
{
    usearch_setOffset(m_strsrch_, position, &status);
}

UTextOffset StringSearch::getOffset(void) const
{
    return usearch_getOffset(m_strsrch_);
}

void StringSearch::setText(const UnicodeString &text, UErrorCode &status)
{
    m_text_ = text;
    usearch_setText(m_strsrch_, text.fArray, text.fLength, &status);
}
    
void StringSearch::setText(CharacterIterator &text, UErrorCode &status)
{
    text.getText(m_text_);
    usearch_setText(m_strsrch_, m_text_.fArray, m_text_.fLength, &status);
}

RuleBasedCollator * StringSearch::getCollator() const
{
    return (RuleBasedCollator *)&m_collator_;
}
    
void StringSearch::setCollator(RuleBasedCollator *coll, UErrorCode &status)
{
    usearch_setCollator(m_strsrch_, coll->getUCollator(), &status);
    m_collation_rules_.setTo(coll->getRules());
    m_collator_.setUCollator((UCollator *)m_strsrch_->collator, 
                             &m_collation_rules_);
}
    
void StringSearch::setPattern(const UnicodeString &pattern, 
                                    UErrorCode    &status)
{
    m_pattern_ = pattern;
    usearch_setPattern(m_strsrch_, m_pattern_.fArray, m_pattern_.fLength,
                       &status);
}
    
const UnicodeString & StringSearch::getPattern() const
{
    return m_pattern_;
}

// public methods ----------------------------------------------------

void StringSearch::reset()
{
    usearch_reset(m_strsrch_);
}

SearchIterator * StringSearch::safeClone(void) const
{
    UErrorCode status = U_ZERO_ERROR;
    StringSearch *result = new StringSearch(m_pattern_, m_text_, 
                                            (RuleBasedCollator *)&m_collator_, 
                                            m_breakiterator_,
                                            status);
    result->setOffset(getOffset(), status);
    result->setMatchStart(m_strsrch_->search->matchedIndex);
    result->setMatchLength(m_strsrch_->search->matchedLength);
    if (U_FAILURE(status)) {
        return NULL;
    }
    return result;
}
    
// protected method -------------------------------------------------

UTextOffset StringSearch::handleNext(int32_t position, UErrorCode &status)
{
    // values passed here are already in the pre-shift position
    if (U_SUCCESS(status)) {
        if (m_strsrch_->pattern.CELength == 0) {
            m_search_->matchedIndex = 
                                    m_search_->matchedIndex == USEARCH_DONE ? 
                                    getOffset() : m_search_->matchedIndex + 1;
            m_search_->matchedLength = 0;
            ucol_setOffset(m_strsrch_->textIter, m_search_->matchedIndex, 
                           &status);
            if (m_search_->matchedIndex == m_search_->textLength) {
                m_search_->matchedIndex = USEARCH_DONE;
            }
        }
        else {
            // looking at usearch.cpp, this part is shifted out to 
            // StringSearch instead of SearchIterator because m_strsrch_ is
            // not accessible in SearchIterator
            if (!m_search_->isOverlap &&
                position + m_strsrch_->pattern.defaultShiftSize > 
                m_search_->textLength) {
                setMatchNotFound();
                return USEARCH_DONE;
            }
            while (TRUE) {
                if (m_search_->isCanonicalMatch) {
                    // can't use exact here since extra accents are allowed.
                    usearch_handleNextCanonical(m_strsrch_, &status);
                }
                else {
                    usearch_handleNextExact(m_strsrch_, &status);
                }
                if (U_FAILURE(status)) {
                    return USEARCH_DONE;
                }
                if (m_breakiterator_ == NULL || 
                    m_search_->matchedIndex == USEARCH_DONE ||
                    (m_breakiterator_->isBoundary(m_search_->matchedIndex) &&
                     m_breakiterator_->isBoundary(m_search_->matchedIndex + 
                                                  m_search_->matchedLength))) {
                    return m_search_->matchedIndex;
                }
            }
        }
    }
    return USEARCH_DONE;
}

UTextOffset StringSearch::handlePrev(int32_t position, UErrorCode &status)
{
    // values passed here are already in the pre-shift position
    if (U_SUCCESS(status)) {
        if (m_strsrch_->pattern.CELength == 0) {
            m_search_->matchedIndex = 
                  (m_search_->matchedIndex == USEARCH_DONE ? getOffset() : 
                   m_search_->matchedIndex);
            if (m_search_->matchedIndex == 0) {
                setMatchNotFound();
            }
            else {
                m_search_->matchedIndex --;
                ucol_setOffset(m_strsrch_->textIter, m_search_->matchedIndex, 
                               &status);
                m_search_->matchedLength = 0;
            }
        }
        else {
            // looking at usearch.cpp, this part is shifted out to 
            // StringSearch instead of SearchIterator because m_strsrch_ is
            // not accessible in SearchIterator
            if (!m_search_->isOverlap && 
                position - m_strsrch_->pattern.defaultShiftSize < 0) {
                setMatchNotFound();
                return USEARCH_DONE;
            }
            while (TRUE) {
                if (m_search_->isCanonicalMatch) {
                    // can't use exact here since extra accents are allowed.
                    usearch_handlePreviousCanonical(m_strsrch_, &status);
                }
                else {
                    usearch_handlePreviousExact(m_strsrch_, &status);
                }
                if (U_FAILURE(status)) {
                    return USEARCH_DONE;
                }
                if (m_breakiterator_ == NULL || 
                    m_search_->matchedIndex == USEARCH_DONE ||
                    (m_breakiterator_->isBoundary(m_search_->matchedIndex) &&
                     m_breakiterator_->isBoundary(m_search_->matchedIndex + 
                                                  m_search_->matchedLength))) {
                    return m_search_->matchedIndex;
                }
            }
        }
          
        return m_search_->matchedIndex;
    }
    return USEARCH_DONE;
}



