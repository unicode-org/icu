/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998 - 2003 - All Rights Reserved
 *
 */

#ifndef __SCRIPTCOMPOSITEFONTINSTANCE_H
#define __SCRIPTCOMPOSITEFONTINSTANCE_H

#include "layout/LETypes.h"
#include "layout/LEFontInstance.h"

#include "FontMap.h"

// U_NAMESPACE_BEGIN

class ScriptCompositeFontInstance : public LEFontInstance
{
public:

    ScriptCompositeFontInstance(FontMap *fontMap);

    virtual ~ScriptCompositeFontInstance();

    /**
     * This method is provided so that clients can tell if
     * a given LEFontInstance is an instance of a composite
     * font.
     * 
     * @return <code>true</code> if the instance represents a composite font, <code>false</code> otherwise.
     *
     * @draft ICU 2.6
     */
    virtual le_bool isComposite() const;

    /**
     * Get a sub-font for a run of text from a composite font. This method examines the
     * given text, finding a run of text which can all be rendered
     * using the same sub-font. Subclassers should try to keep all the text in a single
     * sub-font if they can.
     *
     * @param chars  - the array of unicode characters
     * @param offset - a pointer to the starting offset in the text. This will be
     *                 set to the limit offset of the run on exit.
     * @param count  - the number of characters in the array. Can be used as a hint for selecting a sub-font.
     * @param script - the script of the characters.
     *
     * @return an <code>LEFontInstance</code> for the sub font which can render the characters.
     *
     * @draft ICU 2.6
     */
    const LEFontInstance *getSubFont(const LEUnicode chars[], le_int32 *offset, le_int32 count, le_int32 script) const;

    /**
     * This method maps a single character to a glyph index, using the
     * font's charcter to glyph map.
     *
     * @param ch - the character
     *
     * @return the glyph index
     *
     * @draft ICU 2.6
     */
    virtual LEGlyphID mapCharToGlyph(LEUnicode32 ch) const;

    virtual const void *getFontTable(LETag tableTag) const;

    virtual le_int32 getUnitsPerEM() const;

    virtual le_int32 getAscent() const;

    virtual le_int32 getDescent() const;

    virtual le_int32 getLeading() const;

    virtual void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const;

    virtual le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const;

    float getXPixelsPerEm() const;

    float getYPixelsPerEm() const;

    float getScaleFactorX() const;

    float getScaleFactorY() const;

protected:
    FontMap *fFontMap;
};

inline le_bool ScriptCompositeFontInstance::isComposite() const
{
    return true;
}

inline const void *ScriptCompositeFontInstance::getFontTable(LETag tableTag) const
{
    return NULL;
}

// Can't get units per EM without knowing which sub-font, so
// return a value that will make units == points
inline le_int32 ScriptCompositeFontInstance::getUnitsPerEM() const
{
    return 1;
}

inline le_int32 ScriptCompositeFontInstance::getAscent() const
{
    return fFontMap->getAscent();
}

inline le_int32 ScriptCompositeFontInstance::getDescent() const
{
    return fFontMap->getDescent();
}

inline le_int32 ScriptCompositeFontInstance::getLeading() const
{
    return fFontMap->getLeading();
}

inline float ScriptCompositeFontInstance::getXPixelsPerEm() const
{
    return fFontMap->getPointSize();
}

inline float ScriptCompositeFontInstance::getYPixelsPerEm() const
{
    return fFontMap->getPointSize();
}

// Can't get a scale factor without knowing the sub-font, so
// return 1.0.
inline float ScriptCompositeFontInstance::getScaleFactorX() const
{
    return 1.0;
}

// Can't get a scale factor without knowing the sub-font, so
// return 1.0
inline float ScriptCompositeFontInstance::getScaleFactorY() const
{
    return 1.0;
}

// U_NAMESPACE_END
#endif
