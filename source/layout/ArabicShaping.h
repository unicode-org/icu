/*
 * @(#)ArabicShaping.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __ARABICSHAPING_H
#define __ARABICSHAPING_H

#include "LETypes.h"
#include "OpenTypeTables.h"

U_NAMESPACE_BEGIN

class Shaper
{
public:
    virtual void init(LEUnicode ch, le_int32 outIndex, le_bool isloate) = 0;
    virtual void shape(le_int32 outIndex, le_int32 shapeOffset) = 0;
};

class ArabicShaping
{
public:
    // shaping bit masks
    enum ShapingBitMasks
    {
        MASK_SHAPE_RIGHT    = 1, // if this bit set, shapes to right
        MASK_SHAPE_LEFT     = 2, // if this bit set, shapes to left
        MASK_TRANSPARENT    = 4, // if this bit set, is transparent (ignore other bits)
        MASK_NOSHAPE        = 8  // if this bit set, don't shape this char, i.e. tatweel
    };

    // shaping values
    enum ShapeTypeValues
    {
        ST_NONE         = 0,
        ST_RIGHT        = MASK_SHAPE_RIGHT,
        ST_LEFT         = MASK_SHAPE_LEFT,
        ST_DUAL         = MASK_SHAPE_RIGHT | MASK_SHAPE_LEFT,
        ST_TRANSPARENT  = MASK_TRANSPARENT,
        ST_NOSHAPE_DUAL = MASK_NOSHAPE | ST_DUAL,
        ST_NOSHAPE_NONE = MASK_NOSHAPE
    };

    typedef le_int32 ShapeType;

    static void shape(const LEUnicode *chars, le_int32 offset, le_int32 charCount, le_int32 charMax,
                      le_bool rightToLeft, Shaper &shaper);

    static le_uint8 glyphSubstitutionTable[];
  //static le_uint8 ligatureSubstitutionSubtable[];
    static le_uint8 glyphDefinitionTable[];

private:
    static ShapeType getShapeType(LEUnicode c);

    static const ShapeType shapeTypes[];
};

class GlyphShaper : public Shaper
{
public:
    virtual void init(LEUnicode ch, le_int32 outIndex, le_bool isolate);
    virtual void shape(le_int32 outIndex, le_int32 shapeOffset);

    GlyphShaper(const LETag **outputTags);
    ~GlyphShaper();

private:
    const LETag **charTags;

    static const LETag isolFeatureTag; // 'isol'
    static const LETag initFeatureTag; // 'init'
    static const LETag mediFeatureTag; // 'medi'
    static const LETag finaFeatureTag; // 'fina'
    static const LETag ligaFeatureTag; // 'liga'
    static const LETag msetFeatureTag; // 'mset'
    static const LETag markFeatureTag; // 'mark'

    static const LETag emptyTag;

    static const LETag tagArray[];

};

class CharShaper : public Shaper
{
public:
    virtual void init(LEUnicode ch, le_int32 outIndex, le_bool isolate);
    virtual void shape(le_int32 outIndex, le_int32 shapeOffset);

    CharShaper(LEUnicode *outputChars);
    ~CharShaper();

private:
    LEUnicode *chars;
    
    static LEUnicode isolateShapes[];

    static LEUnicode getToIsolateShape(LEUnicode ch);
};

U_NAMESPACE_END
#endif
