/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1999               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*
* File SIMTXBD.H
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.  Changed text() to getText() and
*                           made return type const &.
*   08/11/98    helena      Sync-up JDK1.2.
*****************************************************************************************
*/

#ifndef SIMTXBD_H
#define SIMTXBD_H


#include "utypes.h"
#include "unistr.h"
#include "chariter.h"
#include "brkiter.h"
#include "txtbdat.h"


/**
 * SIMPLETEXTBOUNDARY is a concrete implementation of BreakIterator.
 * SimpleTextBoundary uses a state machine to compute breaks.
 * <P>
 * Different state machines are available that compute breaks for
 * sentences, words, lines, and characters.  They are accessable
 * through static functions of BreakIterator.
 */
class SimpleTextBoundary: public BreakIterator {
public:
    /**
     * Destructor.
     */
    virtual ~SimpleTextBoundary();

    /**
     * Return true if another object is semantically equal to this
     * one. The other object should be an instance of a subclass of
     * TextBoundary. Objects of different subclasses are considered
     * unequal.
     * <P>
     * Return true if this BreakIterator is at the same position in the
     * same text, and is the same class and type (word, line, etc.) of
     * BreakIterator, as the argument.  Text is considered the same if
     * it contains the same characters, it need not be the same
     * object, and styles are not considered.
     */
    virtual bool_t operator==(const BreakIterator&) const;

    /**
     * Return a polymorphic copy of this object.  This is an abstract
     * method which subclasses implement.
     */
    BreakIterator* clone(void) const;

    /**
     * Return a polymorphic class ID for this object. Different subclasses
     * will return distinct unequal values.
     */
    virtual ClassID getDynamicClassID(void) const { return getStaticClassID(); }

    /**
     * Return a static class ID for this class.
     */
    static ClassID getStaticClassID(void) { return (ClassID)&fgClassID; }

    /**
     * Return the text iterator over which this operates.
     * @return the caller of this method owns the returned object.
     */
    virtual CharacterIterator* createText(void) const;

    /**
     * Change the text over which this operates. The text boundary is
     * reset to the start.
     */
    virtual void  setText(const UnicodeString* it);

    /**
     * Change the text over which this operates. The text boundary is
     * reset to the start.
     */
    virtual void  adoptText(CharacterIterator* it);

    /**
     * Return the index of the first character in the text being scanned.
     */
    virtual UTextOffset first(void);

    /**
     * Return the index of the last character in the text being scanned.
     */
    virtual UTextOffset last(void);

    /**
     * Return the character index of the previous text boundary, or kDone if all
     * boundaries have been returned.
     */
    virtual UTextOffset previous(void);

    /**
     * Return the character index of the next text boundary, or kDone if all
     * boundaries have been returned.
     */
    virtual UTextOffset next(void);

    /**
     * Return the character index of the text boundary that was most recently
     * returned by next(), previous(), first(), or last().
     */
    virtual UTextOffset current(void) const;

    /**
     * Return the first boundary following the specified offset.
     * The value returned is always greater than the offset, or is kDone.
     * @param offset the offset to begin scanning.
     * @return The first boundary after the specified offset.
     */
    virtual UTextOffset following(UTextOffset offset);

    /**
     * Return the first boundary preceding the specified offset.
     * The value returned is always smaller than the offset, or is kDone.
     * @param offset the offset to begin scanning.
     * @return The first boundary before the specified offset.
     */
    virtual UTextOffset preceding(UTextOffset offset);
 
    /**
     * Return true if the specfied position is a boundary position.
     * @param offset the offset to check.
     * @return True if "offset" is a boundary position.
     */
    virtual bool_t isBoundary(UTextOffset offset);

    /**
     * Return the nth boundary from the current boundary.
     * @param n the signed number of boundaries to traverse.
     * Negative values move to previous boundaries
     * and positive values move to later boundaries. A value of 0 does nothing.
     * @return The character index of the nth boundary from the current position.
     */
    virtual UTextOffset next(int32_t n);

private:
    /**
     * Construct an SimpleTextBoundary from the provided state table.
     * This protected constructor is called from the friend class
     * BreakIterator.
     */
    SimpleTextBoundary(const TextBoundaryData* data);

    /**
     * Copy constructor.
     */
    SimpleTextBoundary(const SimpleTextBoundary&); // only used by clone

    /**
     * The assignment operator is required to satisfy the compiler, but never called.
     */
    SimpleTextBoundary operator=(const SimpleTextBoundary&) { return *this; } // do not call

    /**
     * Internal utility used to locate the previous position from which it is safe
     * to do a forward scan (state is known).
     */
    UTextOffset previousSafePosition(UTextOffset offset);

    /**
     * Internal utility to get the next position.
     */
    UTextOffset nextPosition(UTextOffset offset);

    static char fgClassID;
    static const UChar kEND_OF_STRING;

    const TextBoundaryData*     fData;
    const WordBreakTable*       fForward;   // fData->forward()
    const WordBreakTable*       fBackward;  // fData->backward()
    const UnicodeClassMapping*  fMap;       // fData->map()
    CharacterIterator*          fText;
    UTextOffset                  fPos;

    /* Making BreakIterator a friend is somewhat messy. */
    friend class BreakIterator;
};

#endif // _SIMTXBD
//eof
