/*
 *************************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1996-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 *************************************************************************
 */

/*
* Modification history
* 
* Date      Name      Description
* 02/02/01  synwee    Added converters from EMode to UNormalizationMode, 
*                     getUNormalizationMode and getNormalizerEMode,
*                     useful in tbcoll and unorm.
*                     Added quickcheck method and incorporated it into 
*                     normalize()
* 06/20/01+ Markus Scherer
*                     total rewrite, implement all normalization in unorm.cpp
*                     and turn Normalizer into a wrapper;
*                     fix the very broken iteration API
*/

#include "unicode/normlzr.h"
#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/chariter.h"
#include "unicode/schriter.h"
#include "unicode/uchriter.h"
#include "unormimp.h"

//-------------------------------------------------------------------------
// Constructors and other boilerplate
//-------------------------------------------------------------------------

Normalizer::Normalizer(const UnicodeString& str, 
                       EMode mode) :
    fMode(mode), fOptions(0),
    text(new StringCharacterIterator(str)), nextIndex(-1),
    buffer(), bufferPos(0)
{}

Normalizer::Normalizer(const UnicodeString& str, 
                       EMode mode, 
                       int32_t options) :
    fMode(mode), fOptions(options),
    text(new StringCharacterIterator(str)), nextIndex(-1),
    buffer(), bufferPos(0)
{}

Normalizer::Normalizer(const UChar *str, int32_t length, EMode mode) :
    fMode(mode), fOptions(0),
    text(new UCharCharacterIterator(str, length)), nextIndex(-1),
    buffer(), bufferPos(0)
{}

Normalizer::Normalizer(const CharacterIterator& iter, 
                       EMode mode) :
    fMode(mode), fOptions(0),
    text(iter.clone()), nextIndex(-1),
    buffer(), bufferPos(0)
{}

Normalizer::Normalizer(const CharacterIterator& iter, 
                       EMode mode, 
                       int32_t options) :
    fMode(mode), fOptions(options),
    text(iter.clone()), nextIndex(-1),
    buffer(), bufferPos(0)
{}

Normalizer::Normalizer(const Normalizer &copy) :
    fMode(copy.fMode), fOptions(copy.fOptions),
    text(copy.text->clone()), nextIndex(copy.nextIndex),
    buffer(copy.buffer), bufferPos(copy.bufferPos)
{}

Normalizer::~Normalizer()
{
    delete text;
}

Normalizer* 
Normalizer::clone() const
{
    if(this!=0) {
        return new Normalizer(*this);
    } else {
        return 0;
    }
}

/**
 * Generates a hash code for this iterator.
 */
int32_t Normalizer::hashCode() const
{
    return text->hashCode() + fMode + fOptions + buffer.hashCode() + bufferPos + nextIndex;
}
    
UBool Normalizer::operator==(const Normalizer& that) const
{
    return
        this==&that ||
        fMode==that.fMode &&
        fOptions==that.fOptions &&
        *text==*(that.text) &&
        buffer==that.buffer &&
        bufferPos==that.bufferPos &&
        nextIndex==that.nextIndex;
}

//-------------------------------------------------------------------------
// Static utility methods
//-------------------------------------------------------------------------

void 
Normalizer::normalize(const UnicodeString& source, 
                      EMode mode, 
                      int32_t options,
                      UnicodeString& result, 
                      UErrorCode &status) {
    if(source.isBogus() || U_FAILURE(status)) {
        result.setToBogus();
    } else {
        /* make sure that we do not operate on the same buffer in source and result */
        result.cloneArrayIfNeeded(-1, source.length()+20, FALSE);
        result.fLength=unorm_internalNormalize(result.fArray, result.fCapacity,
                                               source.fArray, source.fLength,
                                               getUNormalizationMode(mode, status), (options&IGNORE_HANGUL)!=0,
                                               UnicodeString::growBuffer, &result,
                                               &status);
        if(U_FAILURE(status)) {
            result.setToBogus();
        }
    }
}

UNormalizationCheckResult
Normalizer::quickCheck(const UnicodeString& source,
                       Normalizer::EMode mode, 
                       UErrorCode &status) {
    if(U_FAILURE(status)) {
        return UNORM_MAYBE;
    }

    return unorm_quickCheck(source.fArray, source.length(), 
                            getUNormalizationMode(mode, status), &status);
}

void
Normalizer::compose(const UnicodeString& source, 
                    UBool compat,
                    int32_t options,
                    UnicodeString& result, 
                    UErrorCode &status) {
    if(source.isBogus()) {
        result.setToBogus();
    } else {
        /* make sure that we do not operate on the same buffer in source and result */
        result.cloneArrayIfNeeded(-1, source.length()+20, FALSE);
        result.fLength=unorm_compose(result.fArray, result.fCapacity,
                                     source.fArray, source.fLength,
                                     compat, (options&IGNORE_HANGUL)!=0,
                                     UnicodeString::growBuffer, &result,
                                     &status);
        if(U_FAILURE(status)) {
            result.setToBogus();
        }
    }
}

void
Normalizer::decompose(const UnicodeString& source, 
                      UBool compat,
                      int32_t options,
                      UnicodeString& result, 
                      UErrorCode &status) {
    if(source.isBogus()) {
        result.setToBogus();
    } else {
        /* make sure that we do not operate on the same buffer in source and result */
        result.cloneArrayIfNeeded(-1, source.length()+20, FALSE);
        result.fLength=unorm_decompose(result.fArray, result.fCapacity,
                                       source.fArray, source.fLength,
                                       compat, (options&IGNORE_HANGUL)!=0,
                                       UnicodeString::growBuffer, &result,
                                       &status);
        if(U_FAILURE(status)) {
            result.setToBogus();
        }
    }
}

//-------------------------------------------------------------------------
// Iteration API
//-------------------------------------------------------------------------

/**
 * Return the current character in the normalized text.
 */
UChar32 Normalizer::current() {
    if(bufferPos<buffer.length()) {
        return buffer.char32At(bufferPos);
    } else {
        /*
         * Normalize from the current index,
         * return the first character from there, and
         * reset the character iterator to the original index.
         * Set nextIndex to where the iterator stopped so
         * that next() can later continue from there.
         */
        UTextOffset currentIndex=text->getIndex();
        UChar32 c;

        if(nextNormalize()) {
            c=buffer.char32At(bufferPos);
            nextIndex=text->getIndex();
        } else {
            c=DONE;
        }
        text->setIndex(currentIndex);
        return c;
    }
}

/**
 * Return the next character in the normalized text and advance
 * the iteration position by one.  If the end
 * of the text has already been reached, {@link #DONE} is returned.
 */
UChar32 Normalizer::next() {
    UChar32 c;

    if(bufferPos<buffer.length()) {
        c=buffer.char32At(bufferPos);
        bufferPos+=UTF_CHAR_LENGTH(c);
        return c;
    } else {
        /*
         * If the buffer (which is now exhausted) was normalized
         * during current() or setIndex() then the character iterator
         * must be set to behind what was normalized then
         * in order to continue with the following text.
         * That "position behind what was normalized" is nextIndex.
         */
        if(nextIndex>=0) {
            text->setIndex(nextIndex);
        }
        if(nextNormalize()) {
            c=buffer.char32At(bufferPos);
            bufferPos+=UTF_CHAR_LENGTH(c);
            return c;
        } else {
            return DONE;
        }
    }
}

/**
 * Return the previous character in the normalized text and decrement
 * the iteration position by one.  If the beginning
 * of the text has already been reached, {@link #DONE} is returned.
 */
UChar32 Normalizer::previous() {
    UChar32 c;

    if(bufferPos>0 || previousNormalize()) {
        c=buffer.char32At(bufferPos-1);
        bufferPos-=UTF_CHAR_LENGTH(c);
        return c;
    } else {
        return DONE;
    }
}

void Normalizer::reset() {
    text->setToStart();
    clearBuffer();
}

void
Normalizer::setIndexOnly(UTextOffset index) {
    text->setIndex(index);
    clearBuffer();
}

/**
 * Set the iteration position in the input text that is being normalized
 * and return the first normalized character at that position.
 * <p>
 * <b>Note:</b> This method sets the position in the <em>input</em> text,
 * while {@link #next} and {@link #previous} iterate through characters
 * in the normalized <em>output</em>.  This means that there is not
 * necessarily a one-to-one correspondence between characters returned
 * by <tt>next</tt> and <tt>previous</tt> and the indices passed to and
 * returned from <tt>setIndex</tt> and {@link #getIndex}.
 * <p>
 * @param index the desired index in the input text.
 *
 * @return      the first normalized character that is the result of iterating
 *              forward starting at the given index.
 *
 * @throws IllegalArgumentException if the given index is less than
 *          {@link #getBeginIndex} or greater than {@link #getEndIndex}.
 */
UChar32 Normalizer::setIndex(UTextOffset index) {
    setIndexOnly(index);
    return current();
}

/**
 * Return the first character in the normalized text.  This resets
 * the <tt>Normalizer's</tt> position to the beginning of the text.
 */
UChar32 Normalizer::first() {
    text->setToStart();
    clearBuffer();
    return next();
}

/**
 * Return the last character in the normalized text.  This resets
 * the <tt>Normalizer's</tt> position to be just before the
 * the input text corresponding to that normalized character.
 */
UChar32 Normalizer::last() {
    text->setToEnd();
    clearBuffer();
    return previous();
}

/**
 * Retrieve the current iteration position in the input text that is
 * being normalized.  This method is useful in applications such as
 * searching, where you need to be able to determine the position in
 * the input text that corresponds to a given normalized output character.
 * <p>
 * <b>Note:</b> This method sets the position in the <em>input</em>, while
 * {@link #next} and {@link #previous} iterate through characters in the
 * <em>output</em>.  This means that there is not necessarily a one-to-one
 * correspondence between characters returned by <tt>next</tt> and
 * <tt>previous</tt> and the indices passed to and returned from
 * <tt>setIndex</tt> and {@link #getIndex}.
 *
 */
UTextOffset Normalizer::getIndex() const {
    return text->getIndex();
}

/**
 * Retrieve the index of the start of the input text.  This is the begin index
 * of the <tt>CharacterIterator</tt> or the start (i.e. 0) of the <tt>String</tt>
 * over which this <tt>Normalizer</tt> is iterating
 */
UTextOffset Normalizer::startIndex() const {
    return text->startIndex();
}

/**
 * Retrieve the index of the end of the input text.  This is the end index
 * of the <tt>CharacterIterator</tt> or the length of the <tt>String</tt>
 * over which this <tt>Normalizer</tt> is iterating
 */
UTextOffset Normalizer::endIndex() const {
    return text->endIndex();
}

//-------------------------------------------------------------------------
// Property access methods
//-------------------------------------------------------------------------

void
Normalizer::setMode(EMode newMode) 
{
    fMode = newMode;
}

Normalizer::EMode 
Normalizer::getMode() const
{
    return fMode;
}

void
Normalizer::setOption(int32_t option, 
                      UBool value) 
{
    if (value) {
        fOptions |= option;
    } else {
        fOptions &= (~option);
    }
}

UBool
Normalizer::getOption(int32_t option) const
{
    return (fOptions & option) != 0;
}

/**
 * Set the input text over which this <tt>Normalizer</tt> will iterate.
 * The iteration position is set to the beginning of the input text.
 */
void
Normalizer::setText(const UnicodeString& newText, 
                    UErrorCode &status)
{
    if (U_FAILURE(status)) {
        return;
    }
    CharacterIterator *newIter = new StringCharacterIterator(newText);
    if (newIter == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    delete text;
    text = newIter;
    reset();
}

/**
 * Set the input text over which this <tt>Normalizer</tt> will iterate.
 * The iteration position is set to the beginning of the string.
 */
void
Normalizer::setText(const CharacterIterator& newText, 
                    UErrorCode &status) 
{
    if (U_FAILURE(status)) {
        return;
    }
    CharacterIterator *newIter = newText.clone();
    if (newIter == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    delete text;
    text = newIter;
    reset();
}

void
Normalizer::setText(const UChar* newText,
                    int32_t length,
                    UErrorCode &status)
{
    if (U_FAILURE(status)) {
        return;
    }
    CharacterIterator *newIter = new UCharCharacterIterator(newText, length);
    if (newIter == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    delete text;
    text = newIter;
    reset();
}

/**
 * Copies the text under iteration into the UnicodeString referred to by "result".
 * @param result Receives a copy of the text under iteration.
 */
void
Normalizer::getText(UnicodeString&  result) 
{
    text->getText(result);
}

//-------------------------------------------------------------------------
// Private utility methods
//-------------------------------------------------------------------------

void Normalizer::clearBuffer() {
    nextIndex=-1;
    buffer.remove();
    bufferPos=0;
}

UBool
Normalizer::nextNormalize() {
    UErrorCode errorCode=U_ZERO_ERROR;
    int32_t length;

    clearBuffer();
    switch(fMode) {
    case NO_OP:
        buffer.setTo(text->next32PostInc());
        length=buffer.length();
        break;
    case COMPOSE:        
    case COMPOSE_COMPAT:
        length=unorm_nextCompose(buffer.fArray, buffer.fCapacity, *text,
                                 fMode==COMPOSE_COMPAT, (fOptions&IGNORE_HANGUL)!=0,
                                 UnicodeString::growBuffer, &buffer,
                                 &errorCode);
        break;
    case DECOMP:    
    case DECOMP_COMPAT:
        length=unorm_nextDecompose(buffer.fArray, buffer.fCapacity, *text,
                                   fMode==COMPOSE_COMPAT, (fOptions&IGNORE_HANGUL)!=0,
                                   UnicodeString::growBuffer, &buffer,
                                   &errorCode);
        break;
    case FCD:
        length=unorm_nextFCD(buffer.fArray, buffer.fCapacity, *text,
                             UnicodeString::growBuffer, &buffer,
                             &errorCode);
        break;
    }

    return U_SUCCESS(errorCode) && length>0;
}

UBool
Normalizer::previousNormalize() {
    UErrorCode errorCode=U_ZERO_ERROR;
    int32_t length;

    clearBuffer();
    switch(fMode) {
    case NO_OP:
        buffer.setTo(text->previous32());
        length=buffer.length();
        break;
    case COMPOSE:        
    case COMPOSE_COMPAT:
        length=unorm_prevCompose(buffer.fArray, buffer.fCapacity, *text,
                                 fMode==COMPOSE_COMPAT, (fOptions&IGNORE_HANGUL)!=0,
                                 UnicodeString::growBuffer, &buffer,
                                 &errorCode);
        break;
    case DECOMP:    
    case DECOMP_COMPAT:
        length=unorm_prevDecompose(buffer.fArray, buffer.fCapacity, *text,
                                   fMode==COMPOSE_COMPAT, (fOptions&IGNORE_HANGUL)!=0,
                                   UnicodeString::growBuffer, &buffer,
                                   &errorCode);
        break;
    case FCD:
        length=unorm_prevFCD(buffer.fArray, buffer.fCapacity, *text,
                             UnicodeString::growBuffer, &buffer,
                             &errorCode);
        break;
    }

    bufferPos=length;
    return U_SUCCESS(errorCode) && length>0;
}
