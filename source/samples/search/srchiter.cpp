/*
**********************************************************************
*   Copyright (C) 1999-2000 IBM and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*  03/22/2000   helena      Creation.
**********************************************************************
*/

#include "unicode/brkiter.h"
#include "unicode/schriter.h"
#include "srchiter.h"

int32_t const SearchIterator::DONE = -1;
int32_t const SearchIterator::BEFORE = -2;    

SearchIterator::SearchIterator(void) :
    index(0),
    length(0),
    target(0),
    backward(FALSE), /* going forward */
    breaker(NULL),
    overlap(TRUE)
{
    UErrorCode status = U_ZERO_ERROR;
    this->breaker = BreakIterator::createCharacterInstance(Locale::getDefault(), status);
    if (U_FAILURE(status)) return;
}

SearchIterator::SearchIterator(CharacterIterator* target, 
                               BreakIterator* breaker) :
    index(0),
    length(0),
    target(0),
    backward(FALSE), /* going forward */
    breaker(NULL),
    overlap(TRUE)
{
    this->target = target;
    
    this->breaker = breaker;
    this->breaker->adoptText(this->target);
    
    index = this->target->startIndex();
    length = 0;
}

SearchIterator::SearchIterator(const  SearchIterator&   other) :
    length(other.length),
    target(0),
    backward(other.backward), /* going forward */
    breaker(NULL),
    overlap(other.overlap)  
{
    index = other.target->startIndex();
    this->target = other.target->clone();
    
    this->breaker = ((BreakIterator&)other.breaker).clone();
    this->breaker->adoptText(this->target);
}

SearchIterator::~SearchIterator()
{
    // deletion of breaker will delete target
    if (breaker != NULL) {
        delete breaker;
        breaker = 0;
    }
}

bool_t SearchIterator::operator == (const SearchIterator& that) const
{
    if (this == &that) return TRUE;
    if (*that.breaker != *breaker) return FALSE;
    else if (*that.target != *target) return FALSE;
    else if (that.backward != backward) return FALSE;
    else if (that.index != index) return FALSE;
    else if (that.length != length) return FALSE;
    else if (that.overlap != overlap) return FALSE;
    else return TRUE;
}

int32_t SearchIterator::first(void) 
{
    setIndex(SearchIterator::BEFORE);
    return next();
}

int32_t SearchIterator::following(int32_t pos) 
{
    setIndex(pos);
    return next();
}
    
int32_t SearchIterator::last(void) 
{
    setIndex(SearchIterator::DONE);
    return previous();
}

int32_t SearchIterator::preceding(int32_t pos) 
{
    setIndex(pos);
    return previous();
}
    
int32_t SearchIterator::next(void) 
{
    if (index == SearchIterator::BEFORE){
        // Starting at the beginning of the text
        index = target->startIndex();
    } else if (index == SearchIterator::DONE) {
        return SearchIterator::DONE;
    } else if (length > 0) {
        // Finding the next match after a previous one
        index += overlap ? 1 : length;
    }
    index -= 1;
    backward = FALSE;
        
    do {
        UErrorCode status = U_ZERO_ERROR;
        length = 0;
        index = handleNext(index + 1, status);
        if (U_FAILURE(status))
        {
            return SearchIterator::DONE;
        }
    } while (index != SearchIterator::DONE && !isBreakUnit(index, index+length));
    
    return index;
}

int32_t SearchIterator::previous(void) 
{
    if (index == SearchIterator::DONE) {
        index = target->endIndex();
    } else if (index == SearchIterator::BEFORE) {
        return SearchIterator::DONE;
    } else if (length > 0) {
        // Finding the previous match before a following one
        index = overlap ? index + length - 1 : index;
    }
    index += 1;
    backward = TRUE;
    
    do {
        UErrorCode status = U_ZERO_ERROR;
        length = 0;
        index = handlePrev(index - 1, status);
        if (U_FAILURE(status))
        {
            return SearchIterator::DONE;
        }
    } while (index != SearchIterator::DONE && !isBreakUnit(index, index+length));

    if (index == SearchIterator::DONE) {
        index = SearchIterator::BEFORE;
    }
    return getIndex();
}


int32_t SearchIterator::getIndex() const
{
    return index == SearchIterator::BEFORE ? SearchIterator::DONE : index;
}

void SearchIterator::setOverlapping(bool_t allowOverlap) 
{
     overlap = allowOverlap;
}
    
bool_t SearchIterator::isOverlapping(void) const
{
    return overlap;
}
    
int32_t SearchIterator::getMatchLength(void) const
{
    return length;
}

void SearchIterator::reset(void)
{
    length = 0;
    if (backward == FALSE) {
        index = 0;
        target->setToStart();
        breaker->first();
    } else {
        index = SearchIterator::DONE;
        target->setToEnd();
        breaker->last();
    }
    overlap = TRUE;
}

void SearchIterator::setBreakIterator(const BreakIterator* iterator) 
{
    CharacterIterator *buffer = target->clone();
    delete breaker;
    breaker = iterator->clone();
    breaker->adoptText(buffer);
}

const BreakIterator& SearchIterator::getBreakIterator(void) const
{
    return *breaker;
}
 
void SearchIterator::setTarget(const UnicodeString& newText)
{
    if (target != NULL && target->getDynamicClassID()
            == StringCharacterIterator::getStaticClassID()) {
        ((StringCharacterIterator*)target)->setText(newText);
    }
    else {
        delete target;
		target = new StringCharacterIterator(newText);
        target->first();
        breaker->adoptText(target);
    }
}
  
void SearchIterator::adoptTarget(CharacterIterator* iterator) {
    target = iterator;
    breaker->adoptText(target);
    setIndex(SearchIterator::BEFORE);
}

const CharacterIterator& SearchIterator::getTarget(void) const
{
    SearchIterator* nonConstThis = (SearchIterator*)this;
    
    // The iterator is initialized pointing to no text at all, so if this
    // function is called while we're in that state, we have to fudge an
    // an iterator to return.
    if (nonConstThis->target == NULL)
        nonConstThis->target = new StringCharacterIterator("");
    return *nonConstThis->target;
}

void SearchIterator::getMatchedText(UnicodeString& result) 
{
    result.remove();
    if (length > 0) {
        int i = 0;
        for (UChar c = target->setIndex(index); i < length; c = target->next(), i++)
        {
            result += c;
        }
    }
}


void SearchIterator::setMatchLength(int32_t length) 
{
    this->length = length;
}

void SearchIterator::setIndex(int32_t pos) {
    index = pos;
    length = 0;
}

bool_t SearchIterator::isBreakUnit(int32_t start, 
                                   int32_t end)
{
    if (breaker == NULL) {
        return TRUE;
    } 
    bool_t startBound = breaker->isBoundary(start);
    bool_t endBound = (end == target->endIndex()) || breaker->isBoundary(end);
    
    return startBound && endBound;
}


