/*
 **********************************************************************
 *   Copyright (C) 1998-2004, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */

#ifndef __LEINSERTIONLIST_H
#define __LEINSERTIONLIST_H

#include "LETypes.h"

U_NAMESPACE_BEGIN

struct InsertionRecord;

class LEInsertionCallback
{
public:
	virtual le_bool applyInsertion(le_int32 atPosition, le_int32 count, LEGlyphID newGlyphs[]) = 0;
};

class LEInsertionList : public UObject
{
public:
	LEInsertionList(le_bool rightToLeft);
	~LEInsertionList();

	LEGlyphID *insert(le_int32 position, le_int32 count);
	le_int32 getGrowAmount();

	le_bool applyInsertions(LEInsertionCallback *callback);

	void reset();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @stable ICU 2.8
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @stable ICU 2.8
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

private:

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;

	InsertionRecord *head;
	InsertionRecord *tail;

	le_int32 growAmount;
	le_bool  append;
};

U_NAMESPACE_END
#endif

