/*
 * @(#)ArabicShaping.cpp    1.10 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "ArabicShaping.h"

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

const LETag GlyphShaper::tagArray[] =
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

#define TAGS_PER_GLYPH (sizeof GlyphShaper::tagArray / sizeof(LETag) / 4)

const LETag *ArabicShaping::getFeatureOrder()
{
    return featureOrder;
}

void ArabicShaping::shape(const LEUnicode *chars, le_int32 offset, le_int32 charCount, le_int32 charMax,
                          le_bool rightToLeft, Shaper &shaper)
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
    le_bool rightShapes = false;
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

        shaper.init(c, out, (t & (MASK_TRANSPARENT | MASK_NOSHAPE)) == 0);

        if ((t & MASK_TRANSPARENT) != 0) {
            continue;
        }

        le_bool curShapes = (t & MASK_NOSHAPE) == 0;
        le_bool curCauses = (t & MASK_SHAPE_RIGHT) != 0;

        if (rightCauses && curCauses) {
            if (rightShapes) {
                shaper.shape(erout, 2);
            }

            if (curShapes) {
                shaper.shape(out, 1);
            }
        }

        rightShapes = curShapes;
        rightCauses = (t & MASK_SHAPE_LEFT) != 0;
        erout = out;
    }

    if (rightShapes && rightCauses && (leftType & MASK_SHAPE_RIGHT) != 0) {
        shaper.shape(erout, 2);
    }
}

GlyphShaper::GlyphShaper(const LETag **outputTags)
{
    charTags = outputTags;
}

GlyphShaper::~GlyphShaper()
{
    // nothing to do
}

void GlyphShaper::init(LEUnicode /*ch*/, le_int32 outIndex, le_bool /*isloate*/)
{
    charTags[outIndex] = tagArray;
}

void GlyphShaper::shape(le_int32 outIndex, le_int32 shapeOffset)
{
    charTags[outIndex] = &charTags[outIndex][TAGS_PER_GLYPH * shapeOffset];
}

CharShaper::CharShaper(LEUnicode *outputChars)
{
    chars = outputChars;
}

CharShaper::~CharShaper()
{
    // nothing to do
}

void CharShaper::init(LEUnicode ch, le_int32 outIndex, le_bool isloate)
{
    if (isloate) {
        chars[outIndex] = getToIsolateShape(ch);
    } else {
        chars[outIndex] = ch;
    }
}

void CharShaper::shape(le_int32 outIndex, le_int32 shapeOffset)
{
    chars[outIndex] += (LEUnicode) shapeOffset;
}

const LEUnicode CharShaper::isolateShapes[] = 
{
    0xfe80, 0xfe81, 0xfe83, 0xfe85, 0xfe87, 0xfe89, 0xfe8d, 0xfe8f, 0xfe93, 0xfe95,
    0xfe99, 0xfe9d, 0xfea1, 0xfea5, 0xfea9, 0xfeab, 0xfead, 0xfeaf, 0xfeb1, 0xfeb5,
    0xfeb9, 0xfebd, 0xfec1, 0xfec5, 0xfec9, 0xfecd, 0x063b, 0x063c, 0x063d, 0x063e,
    0x063f, 0x0640, 0xfed1, 0xfed5, 0xfed9, 0xfedd, 0xfee1, 0xfee5, 0xfee9, 0xfeed,
    0xfeef, 0xfef1, 0x064b, 0x064c, 0x064d, 0x064e, 0x064f, 0x0650, 0x0651, 0x0652,
    0x0653, 0x0654, 0x0655, 0x0656, 0x0657, 0x0658, 0x0659, 0x065a, 0x065b, 0x065c,
    0x065d, 0x065e, 0x065f, 0x0660, 0x0661, 0x0662, 0x0663, 0x0664, 0x0665, 0x0666,
    0x0667, 0x0668, 0x0669, 0x066a, 0x066b, 0x066c, 0x066d, 0x066e, 0x066f, 0x0670,
    0xfb50, 0x0672, 0x0673, 0x0674, 0x0675, 0x0676, 0xfbdd, 0x0678, 0xfb66, 0xfb5e,
    0xfb52, 0x067c, 0x067d, 0xfb56, 0xfb62, 0xfb5a, 0x0681, 0x0682, 0xfb76, 0xfb72,
    0x0685, 0xfb7a, 0xfb7e, 0xfb88, 0x0689, 0x068a, 0x068b, 0xfb84, 0xfb82, 0xfb86,
    0x068f, 0x0690, 0xfb8c, 0x0692, 0x0693, 0x0694, 0x0695, 0x0696, 0x0697, 0xfb8a,
    0x0699, 0x069a, 0x069b, 0x069c, 0x069d, 0x069e, 0x069f, 0x06a0, 0x06a1, 0x06a2,
    0x06a3, 0xfb6a, 0x06a5, 0xfb6e, 0x06a7, 0x06a8, 0xfb8e, 0x06aa, 0x06ab, 0x06ac,
    0xfbd3, 0x06ae, 0xfb92, 0x06b0, 0xfb9a, 0x06b2, 0xfb96, 0x06b4, 0x06b5, 0x06b6,
    0x06b7, 0x06b8, 0x06b9, 0xfb9e, 0xfba0, 0x06bc, 0x06bd, 0xfbaa, 0x06bf, 0xfba4,
    0xfba6, 0x06c2, 0x06c3, 0x06c4, 0xfbe0, 0xfbd9, 0xfbd7, 0xfbdb, 0xfbe2, 0x06ca,
    0xfbde, 0xfbfc, 0x06cd, 0x06ce, 0x06cf, 0xfbe4, 0x06d1, 0xfbae, 0xfbb0
};

LEUnicode CharShaper::getToIsolateShape(LEUnicode ch)
{
    if (ch < 0x0621 || ch > 0x06d3) {
        return ch;
    }

    return isolateShapes[ch - 0x0621];
}

U_NAMESPACE_END
