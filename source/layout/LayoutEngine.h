
/*
 * %W% %W%
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#ifndef __LAYOUTENGINE_H
#define __LAYOUTENGINE_H

#ifndef __LETYPES_H
#include "LETypes.h"
#endif

#include <string.h>

U_NAMESPACE_BEGIN

class LEFontInstance;
class LEGlyphFilter;

/**
 * This is a virtual base class used to do complex text layout. The text must all
 * be in a single font, script, and language. An instance of a LayoutEngine can be
 * created by calling the layoutEngineFactory method. Fonts are identified by
 * instances of the LEFontInstance class. Script and language codes are identified
 * by integer codes, which are defined in ScriptAndLanuageTags.h.
 *
 * Note that this class is not public API. It is declared public so that it can be
 * exported from the library that it is a part of.
 *
 * The input to the layout process is an array of characters in logical order,
 * and a starting X, Y position for the text. The output is an array of glyph indices,
 * an array of character indices for the glyphs, and an array of glyph positions.
 * These arrays are protected members of LayoutEngine which can be retreived by a
 * public method. The reset method can be called to free these arrays so that the
 * LayoutEngine can be reused.
 *
 * The layout process is done in three steps. There is a protected virtual method
 * for each step. These methods have a default implementation which only does
 * character to glyph mapping and default positioning using the glyph's advance
 * widths. Subclasses can override these methods for more advanced layout.
 * There is a public method which invokes the steps in the correct order.
 * 
 * The steps are:
 *
 * 1) Glyph processing - character to glyph mapping and any other glyph processing
 *    such as ligature substitution and contextual forms.
 *
 * 2) Glyph positioning - position the glyphs based on their advance widths.
 *
 * 3) Glyph position adjustments - adjustment of glyph positions for kerning,
 *    accent placement, etc.
 *
 * NOTE: in all methods below, output parameters are references to pointers so
 * the method can allocate and free the storage as needed. All storage allocated
 * in this way is owned by the object which created it, and will be freed when it
 * is no longer needed, or when the object's destructor is invoked.
 *
 * @see LEFontInstance
 * @see ScriptAndLanguageTags.h
 *
 * @draft ICU 2.2
 */
class U_LAYOUT_API LayoutEngine : public UObject {
protected:
    /**
     * The number of glyphs in the output
     *
     * @internal
     */
    le_int32 fGlyphCount;

    /**
     * The output glyph array
     *
     * @internal
     */
    LEGlyphID *fGlyphs;

    /**
     * The character index array. One entry for each output glyph, giving the index
     * in the input character array of the character which corresponds to this glyph.
     *
     * @internal
     */
    le_int32 *fCharIndices;

    /**
     * The glyph position array. There are two entries for each glyph giving the
     * X and Y positions of the glyph. Thus, for glyph i, the X position is at index
     * 2i, and the Y position is at index 2i + 1. There are also two entries for the
     * X and Y position of the advance of the last glyph.
     *
     * @internal
     */
    float *fPositions;

    /**
     * The font instance for the text font.
     *
     * @see LEFontInstance
     *
     * @internal
     */
    const LEFontInstance *fFontInstance;

    /**
     * The script code for the text
     *
     * @see ScriptAndLanguageTags.h for script codes.
     *
     * @internal
     */
    le_int32 fScriptCode;

    /**
     * The langauge code for the text
     *
     * @see ScriptAndLanguageTags.h for language codes.
     *
     * @internal
     */
    le_int32 fLanguageCode;

    /**
     * This constructs an instance for a given font, script and language. Subclass constructors
     * must call this constructor.
     *
     * @param fontInstance - the font for the text
     * @param scriptCode - the script for the text
     * @param langaugeCode - the language for the text
     *
     * @see LEFontInstance
     * @see ScriptAndLanguageTags.h
     *
     * @internal
     */
    LayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode);

    /**
     * This overrides the default no argument constructor to make it
     * difficult for clients to call it. Clients are expected to call
     * layoutEngineFactory.
     *
     * @internal
     */
    LayoutEngine();

    /**
     * This method does the glyph processing. It converts an array of characters
     * into an array of glyph indices and character indices. The characters to be
     * processed are passed in a surrounding context. The context is specified as
     * a starting address and a maximum character count. An offset and a count are
     * used to specify the characters to be processed.
     *
     * The default implementation of this method only does character to glyph mapping.
     * Subclasses needing more elaborate glyph processing must override this method.
     *
     * Input parameters:
     * @param chars - the character context
     * @param offset - the offset of the first character to process
     * @param count - the number of characters to process
     * @param max - the number of characters in the context.
     * @param rightToLeft - true if the text is in a right to left directional run
     *
     * Output parameters:
     * @param glyphs - the glyph index array
     * @param charIndices - the character index array
     * @param success - set to an error code if the operation fails
     *
     * @return the number of glyphs in the glyph index array
     *
     * @internal
     */
    virtual le_int32 computeGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft, LEGlyphID *&glyphs, le_int32 *&charIndices, LEErrorCode &success);

    /**
     * This method does basic glyph positioning. The default implementation positions
     * the glyphs based on their advance widths. This is sufficient for most uses. It
     * is not expected that many subclasses will override this method.
     *
     * Input parameters:
     * @param glyphs - the input glyph array
     * @param glyphCount - the number of glyphs in the glyph array
     * @param x - the starting X position
     * @param y - the starting Y position
     *
     * Output parameters:
     * @param positions - the output X and Y positions (two entries per glyph)
     *
     * @internal
     */
    virtual void positionGlyphs(const LEGlyphID glyphs[], le_int32 glyphCount, float x, float y, float *&positions, LEErrorCode &success);

    /**
     * This method does positioning adjustments like accent positioning and
     * kerning. The default implementation does nothing. Subclasses needing
     * position adjustments must override this method.
     *
     * Note that this method has both characters and glyphs as input so that
     * it can use the character codes to determine glyph types if that information
     * isn't directly available. (e.g. Some Arabic OpenType fonts don't have a GDEF
     * table)
     *
     * @param chars - the input character context
     * @param offset - the offset of the first character to process
     * @param count - the number of characters to process
     * @param reverse - true if the glyphs in the glyph array have been reordered
     * @param glyphs - the input glyph array
     * @param glyphCount - the number of glyphs
     * @param positions - the position array, will be updated as needed
     * @param success - output parameter set to an error code if the operation fails
     *
     * Note: the positions are passed as a plain array because this method should
     * not need to reallocate them.
     *
     * @internal
     */
    virtual void adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool /*reverse*/, LEGlyphID glyphs[], le_int32 glyphCount, float positions[], LEErrorCode &success)
    {
        if (LE_FAILURE(success)) {
            return;
        }

        if (chars == NULL || glyphs == NULL || positions == NULL || offset < 0 || count < 0 || glyphCount < 0) {
            success = LE_ILLEGAL_ARGUMENT_ERROR;
            return;
        }

        // default is no adjustments
        return;
    };

    /**
     * This method gets a table from the font associated with
     * the text. The default implementation gets the table from
     * the font instance. Subclasses which need to get the tables
     * some other way must override this method.
     * 
     * @param tableTag - the four byte table tag.
     *
     * @return the address of the table.
     *
     * @internal
     */
    virtual const void *getFontTable(LETag tableTag) const;

    /**
     * This method does character to glyph mapping. The default implementation
     * uses the font instance to do the mapping. It will allocate the glyph and
     * character index arrays if they're not already allocated. If it allocates the
     * character index array, it will fill it it.
     *
     * This method supports right to left
     * text with the ability to store the glyphs in reverse order, and by supporting
     * character mirroring, which will replace a character which has a left and right
     * form, such as parens, with the opposite form before mapping it to a glyph index.
     *
     * Input parameters:
     * @param chars - the input character context
     * @param offset - the offset of the first character to be mapped
     * @param count - the number of characters to be mapped
     * @param reverse - if true, the output will be in reverse order
     * @param mirror - if true, do character mirroring
     *
     * Output parameters:
     * @param glyphs - the glyph array
     * @param charIndices - the character index array
     * @param success - set to an error code if the operation fails
     *
     * @see LEFontInstance
     *
     * @internal
     */
    virtual void mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, le_bool mirror, LEGlyphID *&glyphs, le_int32 *&charIndices, LEErrorCode &success);

    /**
     * This is a convenience method that forces the advance width of mark
     * glyphs to be zero, which is required for proper selection and highlighting.
     * 
     * @param glyphs - the glyph array
     * @param glyphCount - the number of glyphs
     * @param reverse - true if the glyph array has been reordered
     * @param markFilter - used to identify mark glyphs
     * @param positions - the glyph position array - updated as required
     * @param success - output parameter set to an error code if the operation fails
     *
     * @see LEGlyphFilter
     *
     * @internal
     */
    static void adjustMarkGlyphs(const LEGlyphID glyphs[], le_int32 glyphCount, le_bool reverse, LEGlyphFilter *markFilter, float positions[], LEErrorCode &success);

public:
    /**
     * The destructor. It will free any storage allocated for the
     * glyph, character index and position arrays by calling the reset
     * method. It is declared virtual so that it will be invoked by the
     * subclass destructors.
     *
     * @draft ICU 2.2
     */
    virtual ~LayoutEngine();

    /**
     * This method will invoke the layout steps in their correct order by calling
     * the computeGlyphs, positionGlyphs and adjustGlyphPosition methods.. It will
     * compute the glyph, character index and position arrays.
     *
     * @param chars - the input character context
     * @param offset - the offset of the first character to process
     * @param count - the number of characters to process
     * @param max - the number of characters in the input context
     * @param rightToLeft - true if the characers are in a right to left directional run
     * @param x - the initial X position
     * @param y - the initial Y position
     * @param success - output parameter set to an error code if the operation fails
     *
     * @return the number of glyphs in the glyph array
     *
     * Note; the glyph, character index and position array can be accessed
     * using the getter method below.
     *
     * @draft ICU 2.2
     */
    virtual le_int32 layoutChars(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft, float x, float y, LEErrorCode &success);

    /**
     * This method returns the number of glyphs in the glyph array. Note
     * that the number of glyphs will be greater than or equal to the number
     * of characters used to create the LayoutEngine.
     *
     * @return the number of glyphs in the glyph array
     *
     * @draft ICU 2.2
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
     * @draft ICU 2.2
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
     * @draft ICU 2.2
     */
    virtual void getGlyphs(le_uint32 glyphs[], le_uint32 extraBits, LEErrorCode &success) const;

    /**
     * This method copies the character index array into a caller supplied array.
     * The caller must ensure that the array is large enough to hold a
     * character index for each glyph.
     *
     * @param charIndices - the destiniation character index array
     * @param success - set to an error code if the operation fails
     *
     * @draft ICU 2.2
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
     * @draft ICU 2.2
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
     * @draft ICU 2.2
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
     * @draft ICU 2.2
     */
    void getGlyphPosition(le_int32 glyphIndex, float &x, float &y, LEErrorCode &success) const;

    /**
     * This method frees the glyph, character index and position arrays
     * so that the LayoutEngine can be reused to layout a different
     * characer array. (This method is also called by the destructor)
     *
     * @draft ICU 2.2
     */
    virtual void reset();

    /**
     * This method returns a LayoutEngine capable of laying out text
     * in the given font, script and langauge. Note that the LayoutEngine
     * returned may be a subclass of LayoutEngine.
     *
     * @param fontInstance - the font of the text
     * @param scriptCode - the script of the text
     * @param langaugeCode - the language of the text
     * @param success - output parameter set to an error code if the operation fails
     *
     * @return a LayoutEngine which can layout text in the given font.
     *
     * @see LEFontInstance
     *
     * @draft ICU 2.2
     */
    static LayoutEngine *layoutEngineFactory(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode, LEErrorCode &success);

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

private:

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

U_NAMESPACE_END
#endif

