/*
 **********************************************************************
 *   Copyright (C) 1998-2004, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */

#ifndef __LEGLYPHSTORAGE_H
#define __LEGLYPHSTORAGE_H

#include "LETypes.h"
#include "LEInsertionList.h"

U_NAMESPACE_BEGIN

class LEGlyphStorage : public UObject, protected LEInsertionCallback
{
private:
	le_int32   fGlyphCount;

	LEGlyphID *fGlyphs;
	le_int32  *fCharIndices;
	float     *fPositions;
	void     **fAuxData;

	LEInsertionList *fInsertionList;
	le_int32 fSrcIndex;
	le_int32 fDestIndex;

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;

protected:
	virtual le_bool applyInsertion(le_int32 atPosition, le_int32 count, LEGlyphID newGlyphs[]);

public:

	// allocates glyphs, charIndices...
	// call allocatePositions() or allocateAuxData() to allocat those.
	LEGlyphStorage();
	~LEGlyphStorage();

    /**
     * This method returns the number of glyphs in the glyph array. Note
     * that the number of glyphs will be greater than or equal to the number
     * of characters used to create the LayoutEngine.
     *
     * @return the number of glyphs in the glyph array
     *
     * @draft ICU 3.0
     */
    le_int32 getGlyphCount() const
    {
        return fGlyphCount;
    };

    /**
     * This method copies the glyph array into a caller supplied array.
     * The caller must ensure that the array is large enough to hold all
     * the glyphs.
     *
     * @param glyphs - the destiniation glyph array
     * @param success - set to an error code if the operation fails
     *
     * @draft ICU 3.0
     */
    void getGlyphs(LEGlyphID glyphs[], LEErrorCode &success) const;

    /**
     * This method copies the glyph array into a caller supplied array,
     * ORing in extra bits. (This functionality is needed by the JDK,
     * which uses 32 bits pre glyph idex, with the high 16 bits encoding
     * the composite font slot number)
     *
     * @param glyphs - the destination (32 bit) glyph array
     * @param extraBits - this value will be ORed with each glyph index
     * @param success - set to an error code if the operation fails
     *
     * @draft ICU 3.0
     */
    void getGlyphs(le_uint32 glyphs[], le_uint32 extraBits, LEErrorCode &success) const;

    /**
     * This method copies the character index array into a caller supplied array.
     * The caller must ensure that the array is large enough to hold a
     * character index for each glyph.
     *
     * @param charIndices - the destiniation character index array
     * @param success - set to an error code if the operation fails
     *
     * @draft ICU 3.0
     */
    void getCharIndices(le_int32 charIndices[], LEErrorCode &success) const;

    /**
     * This method copies the character index array into a caller supplied array.
     * The caller must ensure that the array is large enough to hold a
     * character index for each glyph.
     *
     * @param charIndices - the destiniation character index array
     * @param indexBase - an offset which will be added to each index
     * @param success - set to an error code if the operation fails
     *
     * @draft ICU 3.0
     */
    void getCharIndices(le_int32 charIndices[], le_int32 indexBase, LEErrorCode &success) const;

    /**
     * This method copies the position array into a caller supplied array.
     * The caller must ensure that the array is large enough to hold an
     * X and Y position for each glyph, plus an extra X and Y for the
     * advance of the last glyph.
     *
     * @param glyphs - the destiniation position array
     * @param success - set to an error code if the operation fails
     *
     * @draft ICU 3.0
     */
    void getGlyphPositions(float positions[], LEErrorCode &success) const;

    /**
     * This method returns the X and Y position of the glyph at
     * the given index.
     *
     * Input parameters:
     * @param glyphIndex - the index of the glyph
     *
     * Output parameters:
     * @param x - the glyph's X position
     * @param y - the glyph's Y position
     * @param success - set to an error code if the operation fails
     *
     * @draft ICU 3.0
     */
    void getGlyphPosition(le_int32 glyphIndex, float &x, float &y, LEErrorCode &success) const;

	void allocateGlyphArray(le_int32 initialGlyphCount, le_bool rightToLeft, LEErrorCode &success);

	// allocate the given data array, return the size? (it's just the glyph count,
	// so maybe we don't need to return it?)
	le_int32 allocatePositions(LEErrorCode &success);
	le_int32 allocateAuxData(LEErrorCode &success);

	void getAuxData(void *auxData[], LEErrorCode &success) const;

	LEGlyphID getGlyphID(le_int32 glyphIndex, LEErrorCode &success) const;
	le_int32  getCharIndex(le_int32 glyphIndex, LEErrorCode &success) const;

	void *getAuxData(le_int32 glyphIndex, LEErrorCode &success) const; // or "getAuxDatum"?
	void setAuxData(le_int32 glyphIndex, void *auxData, LEErrorCode &success); // or "setAuxDatum"?

	LEGlyphID &operator[](le_int32 glyphIndex) const;

	// return value is address of storage for new glyphs...
	LEGlyphID *insertGlyphs(le_int32 atIndex, le_int32 insertCount);

	// return value is new glyph count.
	le_int32 applyInsertions();

	void setGlyphID(le_int32 glyphIndex, LEGlyphID glyphID, LEErrorCode &success);
	void setCharIndex(le_int32 glyphIndex, le_int32 charIndex, LEErrorCode &success);
	void setPosition(le_int32 glyphIndex, float x, float y, LEErrorCode &success);
	void adjustPosition(le_int32 glyphIndex, float xAdjust, float yAdjust, LEErrorCode &success);

	void adoptGlyphArray(LEGlyphStorage &from);
	void adoptCharIndicesArray(LEGlyphStorage &from);
	void adoptPositionArray(LEGlyphStorage &from);
	void adoptAuxDataArray(LEGlyphStorage &from);
	void adoptGlyphCount(LEGlyphStorage &from);
	void adoptGlyphCount(le_int32 newGlyphCount);

    /**
     * This method frees the glyph, character index and position arrays
     * so that the LayoutEngine can be reused to layout a different
     * characer array. (This method is also called by the destructor)
     *
     * @draft ICU 3.0
     */
    void reset();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @darft ICU 3.0
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 3.0
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }
};

inline LEGlyphID &LEGlyphStorage::operator[](le_int32 glyphIndex) const
{
	return fGlyphs[glyphIndex];
}


U_NAMESPACE_END
#endif

