/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "ArabicShaping.h"
#include "LEGlyphStorage.h"

U_NAMESPACE_BEGIN

enum {
    _c_ = ArabicShaping::ST_NOSHAPE_DUAL,
    _d_ = ArabicShaping::ST_DUAL,
    _n_ = ArabicShaping::ST_NONE,
    _r_ = ArabicShaping::ST_RIGHT,
    _t_ = ArabicShaping::ST_TRANSPARENT,
    _x_ = ArabicShaping::ST_NOSHAPE_NONE
};

const ArabicShaping::ShapeType ArabicShaping::shapeTypes[] =
{
   _n_, _r_, _r_, _r_, _r_, _d_, _r_, _d_, _r_, _d_, _d_, _d_, _d_, _d_, _r_, _r_,   // 0x621 - 0x630
   _r_, _r_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _x_, _x_, _x_, _x_, _x_, _c_,   // 0x631 - 0x640
   _d_, _d_, _d_, _d_, _d_, _d_, _d_, _r_, _d_, _d_, _t_, _t_, _t_, _t_, _t_, _t_,   // 0x641 - 0x650
   _t_, _t_, _t_, _t_, _t_, _x_, _x_, _x_, _x_, _x_, _x_, _x_, _x_, _x_, _x_, _n_,   // 0x651 - 0x660
   _n_, _n_, _n_, _n_, _n_, _n_, _n_, _n_, _n_, _n_, _n_, _n_, _n_, _x_, _x_, _t_,   // 0x661 - 0x670
   _r_, _r_, _r_, _x_, _r_, _r_, _r_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_,   // 0x671 - 0x680
   _d_, _d_, _d_, _d_, _d_, _d_, _d_, _r_, _r_, _r_, _r_, _r_, _r_, _r_, _r_, _r_,   // 0x681 - 0x690
   _r_, _r_, _r_, _r_, _r_, _r_, _r_, _r_, _r_, _d_, _d_, _d_, _d_, _d_, _d_, _d_,   // 0x691 - 0x6a0
   _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_, _d_,   // 0x6a1 - 0x6b0
   _d_, _d_, _d_, _d_, _d_, _d_, _d_, _x_, _x_, _d_, _d_, _d_, _d_, _d_, _x_, _r_,   // 0x6b1 - 0x6c0
   _d_, _r_, _r_, _r_, _r_, _r_, _r_, _r_, _r_, _r_, _r_, _d_, _r_, _d_, _x_, _d_,   // 0x6c1 - 0x6d0
   _d_, _r_, _r_, _x_, _x_, _t_, _t_, _t_, _t_, _t_, _t_, _t_, _t_, _t_, _t_, _t_,   // 0x6d1 - 0x6e0
   _t_, _t_, _t_, _t_, _n_, _n_, _t_, _t_, _n_, _t_, _t_, _t_, _t_, _x_, _x_, _n_,   // 0x6e1 - 0x6f0
   _n_, _n_, _n_, _n_, _n_, _n_, _n_, _n_, _n_, _x_, _x_, _x_, _x_, _x_, _x_         // 0x6f1 - 0x6ff
};

/*
    shaping array holds types for arabic chars between 0621 and 0700
    other values are either unshaped, or transparent if a mark or format
    code, except for format codes 200c (zero-width non-joiner) and 200d 
    (dual-width joiner) which are both unshaped and non_joining or
    dual-joining, respectively.
*/
ArabicShaping::ShapeType ArabicShaping::getShapeType(LEUnicode c)
{
    if (c >= 0x0621 && c <= 0x206f) {
        if (c < 0x0700) {
            return shapeTypes[c - 0x0621];
        } else if (c == 0x200c) {   // ZWNJ
            return ST_NOSHAPE_NONE;
        } else if (c == 0x200d) {   // ZWJ
            return ST_NOSHAPE_DUAL;
        } else if (c >= 0x202a && c <= 0x202e) { // LRE - RLO
            return ST_TRANSPARENT;
        } else if (c >= 0x206a && c <= 0x206f) { // Inhibit Symmetric Swapping - Nominal Digit Shapes
            return ST_TRANSPARENT;
        }
    }

    return ST_NOSHAPE_NONE;
}

const LETag isolFeatureTag = LE_ISOL_FEATURE_TAG;
const LETag initFeatureTag = LE_INIT_FEATURE_TAG;
const LETag mediFeatureTag = LE_MEDI_FEATURE_TAG;
const LETag finaFeatureTag = LE_FINA_FEATURE_TAG;
const LETag ligaFeatureTag = LE_LIGA_FEATURE_TAG;
const LETag msetFeatureTag = LE_MSET_FEATURE_TAG;
const LETag markFeatureTag = LE_MARK_FEATURE_TAG;
const LETag ccmpFeatureTag = LE_CCMP_FEATURE_TAG;
const LETag rligFeatureTag = LE_RLIG_FEATURE_TAG;
const LETag caltFeatureTag = LE_CALT_FEATURE_TAG;
const LETag dligFeatureTag = LE_DLIG_FEATURE_TAG;
const LETag cswhFeatureTag = LE_CSWH_FEATURE_TAG;
const LETag cursFeatureTag = LE_CURS_FEATURE_TAG;
const LETag kernFeatureTag = LE_KERN_FEATURE_TAG;
const LETag mkmkFeatureTag = LE_MKMK_FEATURE_TAG;

const LETag emptyTag       = 0x00000000; // ''

const LETag featureOrder[] = 
{
    ccmpFeatureTag, isolFeatureTag, finaFeatureTag, mediFeatureTag, initFeatureTag, rligFeatureTag,
    caltFeatureTag, ligaFeatureTag, dligFeatureTag, cswhFeatureTag, msetFeatureTag, cursFeatureTag,
    kernFeatureTag, markFeatureTag, mkmkFeatureTag, emptyTag
};

const LETag ArabicShaping::tagArray[] =
{
    isolFeatureTag, ligaFeatureTag, msetFeatureTag, markFeatureTag, ccmpFeatureTag, rligFeatureTag,
        caltFeatureTag, dligFeatureTag, cswhFeatureTag, cursFeatureTag, kernFeatureTag, mkmkFeatureTag, emptyTag,

    finaFeatureTag, ligaFeatureTag, msetFeatureTag, markFeatureTag, ccmpFeatureTag, rligFeatureTag,
        caltFeatureTag, dligFeatureTag, cswhFeatureTag, cursFeatureTag, kernFeatureTag, mkmkFeatureTag, emptyTag,

    initFeatureTag, ligaFeatureTag, msetFeatureTag, markFeatureTag, ccmpFeatureTag, rligFeatureTag,
        caltFeatureTag, dligFeatureTag, cswhFeatureTag, cursFeatureTag, kernFeatureTag, mkmkFeatureTag, emptyTag,

    mediFeatureTag, ligaFeatureTag, msetFeatureTag, markFeatureTag, ccmpFeatureTag, rligFeatureTag,
        caltFeatureTag, dligFeatureTag, cswhFeatureTag, cursFeatureTag, kernFeatureTag, mkmkFeatureTag, emptyTag
};

#define TAGS_PER_GLYPH ((sizeof ArabicShaping::tagArray / sizeof ArabicShaping::tagArray[0]) / 4)

const LETag *ArabicShaping::getFeatureOrder()
{
    return featureOrder;
}

void ArabicShaping::adjustTags(le_int32 outIndex, le_int32 shapeOffset, LEGlyphStorage &glyphStorage)
{
    LEErrorCode success = LE_NO_ERROR;
    const LETag *glyphTags = (const LETag *) glyphStorage.getAuxData(outIndex, success);

    glyphStorage.setAuxData(outIndex, (void *) &glyphTags[TAGS_PER_GLYPH * shapeOffset], success);
}

void ArabicShaping::shape(const LEUnicode *chars, le_int32 offset, le_int32 charCount, le_int32 charMax,
                          le_bool rightToLeft, LEGlyphStorage &glyphStorage)
{
    // iterate in logical order, store tags in visible order
    // 
    // the effective right char is the most recently encountered 
    // non-transparent char
    //
    // four boolean states:
    //   the effective right char shapes
    //   the effective right char causes left shaping
    //   the current char shapes
    //   the current char causes right shaping
    // 
    // if both cause shaping, then
    //   shaper.shape(errout, 2) (isolate to initial, or final to medial)
    //   shaper.shape(out, 1) (isolate to final)

    ShapeType rightType = ST_NOSHAPE_NONE, leftType = ST_NOSHAPE_NONE;
    LEErrorCode success = LE_NO_ERROR;
    le_int32 i;

    for (i = offset - 1; i >= 0; i -= 1) {
        rightType = getShapeType(chars[i]);
        
        if (rightType != ST_TRANSPARENT) {
            break;
        }
    }

    for (i = offset + charCount; i < charMax; i += 1) {
        leftType = getShapeType(chars[i]);

        if (leftType != ST_TRANSPARENT) {
            break;
        }
    }

    // erout is effective right logical index
    le_int32 erout = -1;
    le_bool rightShapes = FALSE;
    le_bool rightCauses = (rightType & MASK_SHAPE_LEFT) != 0;
    le_int32 in, e, out = 0, dir = 1;

    if (rightToLeft) {
        out = charCount - 1;
        erout = charCount;
        dir = -1;
    }

    for (in = offset, e = offset + charCount; in < e; in += 1, out += dir) {
        LEUnicode c = chars[in];
        ShapeType t = getShapeType(c);

        glyphStorage.setAuxData(out, (void *) tagArray, success);

        if ((t & MASK_TRANSPARENT) != 0) {
            continue;
        }

        le_bool curShapes = (t & MASK_NOSHAPE) == 0;
        le_bool curCauses = (t & MASK_SHAPE_RIGHT) != 0;

        if (rightCauses && curCauses) {
            if (rightShapes) {
                adjustTags(erout, 2, glyphStorage);
            }

            if (curShapes) {
                adjustTags(out, 1, glyphStorage);
            }
        }

        rightShapes = curShapes;
        rightCauses = (t & MASK_SHAPE_LEFT) != 0;
        erout = out;
    }

    if (rightShapes && rightCauses && (leftType & MASK_SHAPE_RIGHT) != 0) {
        adjustTags(erout, 2, glyphStorage);
    }
}

U_NAMESPACE_END
