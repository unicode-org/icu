
/*
 * @(#)LEFontInstance.h	1.3 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __LEFONTINSTANCE_H
#define __LEFONTINSTANCE_H

#include "LETypes.h"

U_NAMESPACE_BEGIN

/**
 * Instances of this class are used by LEFontInstance::mapCharsToGlyphs and
 * LEFontInstance::mapCharToGlyph to adjust character codes before the character
 * to glyph mapping process. Examples of this are filtering out control charcters
 * and character mirroring - replacing a character which has both a left and a right
 * hand form with the opposite form.
 */
class LECharMapper
{
public:
	/**
	 * This method does the adjustments.
	 *
	 * @param ch - the input charcter
	 *
	 * @return the adjusted character
	 */
    virtual LEUnicode32 mapChar(LEUnicode32 ch) const = 0;
};

/**
 * This is a pure virtual base class that servers as the interface between a LayoutEngine
 * and the platform font environment. It allows a LayoutEngine to access font tables, do
 * character to glyph mapping, and obtain metrics information without knowing any platform
 * specific details. There are also a few utility methods for converting between points,
 * pixels and funits. (font design units)
 *
 * Each instance of an LEFontInstance represents a renerable instance of a font. (i.e. a
 * single font at a particular point size, with a particular transform)
 */
class LEFontInstance
{
public:

	/**
	 * This virtual destructor is here so that the subclass
	 * destructors can be invoked through the base class.
	 */
    virtual ~LEFontInstance() { };

	//
    // Font file access
	//

	/**
	 * This method reads a table from the font.
	 *
	 * @param tableTag - the four byte table tag
	 *
	 * @return the address of the table in memory
	 */
    virtual const void *getFontTable(LETag tableTag) const = 0;

	/**
	 * This method is used to determine if the font can
	 * render the given character. This can usually be done
	 * by looking the character up in the font's character
	 * to glyph mapping.
	 *
	 * @param ch - the character to be tested
	 *
	 * @return true if the font can render ch.
	 */
    virtual le_bool canDisplay(LEUnicode32 ch) const = 0;

	/**
	 * This method returns the number of design units in
	 * the font's EM square.
	 *
	 * @return the number of design units pre EM.
	 */
    virtual le_int32 getUnitsPerEM() const = 0;

	/**
	 * This method maps an array of charcter codes to an array of glyph
	 * indices, using the font's character to glyph map.
	 *
	 * @param chars - the character array
	 * @param offset - the index of the first charcter
	 * @param count - the number of charcters
	 * @param reverse - if true, store the glyph indices in reverse order.
	 * @param mapper - the character mapper.
	 * @param glyphs - the output glyph array
	 *
	 * @see LECharMapper
	 */
    virtual void mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, const LECharMapper *mapper, LEGlyphID glyphs[]) const = 0;

	/**
	 * This method maps a single character to a glyph index, using the
	 * font's charcter to glyph map.
	 *
	 * @param ch - the character
	 * @param mapper - the character mapper
	 *
	 * @return the glyph index
	 *
	 * @see LECharMapper
	 */
    virtual LEGlyphID mapCharToGlyph(LEUnicode32 ch, const LECharMapper *mapper) const = 0;

	/**
	 * This method gets a name from the font. (e.g. the family name) The encoding
	 * of the name is specified by the platform, the script, and the language.
	 *
	 * @param platformID - the platform id
	 * @param scriptID - the script id
	 * @param langaugeID - the language id
	 * @param name - the destination character array (can be null)
	 *
	 * @return the number of characters in the name
	 */
    virtual le_int32 getName(le_uint16 platformID, le_uint16 scriptID, le_uint16 languageID, le_uint16 nameID, LEUnicode *name) const = 0;

	//
    // Metrics
	//

	/**
	 * This method gets the X and Y advance of a particular glyph, in pixels.
	 *
	 * @param glyph - the glyph index
	 * @param advance - the X and Y pixel values will be stored here
	 */
    virtual void getGlyphAdvance(LEGlyphID glyph, LEPoint &advance) const = 0;

	/**
	 * This method gets the hinted X and Y pixel coordinates of a particular
	 * point in the outline of the given glyph.
	 *
	 * @param glyph - the glyph index
	 * @param pointNumber - the number of the point
	 * @param point - the point's X and Y pixel values will be stored here
	 *
	 * @return true if the point coordinates could be stored.
	 */
    virtual le_bool getGlyphPoint(LEGlyphID glyph, le_int32 pointNumber, LEPoint &point) const = 0;

    /**
	 * This method returns the width of the font's EM square
	 * in pixels.
	 *
	 * @return the pixel width of the EM square
	 */
    virtual float getXPixelsPerEm() const = 0;

    /**
	 * This method returns the height of the font's EM square
	 * in pixels.
	 *
	 * @return the pixel height of the EM square
	 */
    virtual float getYPixelsPerEm() const = 0;

    /**
	 * This method converts font design units in the
	 * X direction to points.
	 *
	 * @param xUnits - design units in the X direction
	 *
	 * @return points in the X direction
	 */
    virtual float xUnitsToPoints(float xUnits) const = 0;

    /**
	 * This method converts font design units in the
	 * Y direction to points.
	 *
	 * @param yUnits - design units in the Y direction
	 *
	 * @return points in the Y direction
	 */
    virtual float yUnitsToPoints(float yUunits) const = 0;

    /**
	 * This method converts font design units to points.
	 *
	 * @param units - X and Y design units
	 * @param points - set to X and Y points
	 */
    virtual void unitsToPoints(LEPoint &units, LEPoint &points) const = 0;

    /**
	 * This method converts pixels in the
	 * X direction to font design units.
	 *
	 * @param xPixels - pixels in the X direction
	 *
	 * @return font design units in the X direction
	 */
    virtual float xPixelsToUnits(float xPixels) const = 0;

    /**
	 * This method converts pixels in the
	 * Y direction to font design units.
	 *
	 * @param yPixels - pixels in the Y direction
	 *
	 * @return font design units in the Y direction
	 */
    virtual float yPixelsToUnits(float yPixels) const = 0;

    /**
	 * This method converts pixels to font design units.
	 *
	 * @param pixels - X and Y pixel
	 * @param units - set to X and Y font design units
	 */
    virtual void pixelsToUnits(LEPoint &pixels, LEPoint &units) const = 0;

	/**
	 * This method transforms an X, Y point in font design units to a
	 * pixel coordinate, applying the font's transform.
	 *
	 * @param xFunits - the X coordinate in font design units
	 * @param yFunits - the Y coordinate in font design units
	 * @param pixels - the tranformed co-ordinate in pixels
	 */
    virtual void transformFunits(float xFunits, float yFunits, LEPoint &pixels) const = 0;

	/**
	 * This is a convenience method used to convert
	 * values in a 16.16 fixed point format to floating point.
	 *
	 * @param fixed - the fixed point value
	 *
	 * @return the floating point value
	 */
    static float fixedToFloat(le_int32 fixed)
    {
        return (float) (fixed / 65536.0);
    };

	/**
	 * This is a convenience method used to convert
	 * floating point values to 16.16 fixed point format.
	 *
	 * @param theFloat - the floating point value
	 *
	 * @return the fixed point value
	 */
    static le_int32 floatToFixed(float theFloat)
    {
        return (le_int32) (theFloat * 65536.0);
    };
};

U_NAMESPACE_END
#endif


