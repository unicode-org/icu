/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#ifndef __GLYPHPOSITIONADJUSTMENTS_H
#define __GLYPHPOSITIONADJUSTMENTS_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "OpenTypeTables.h"

U_NAMESPACE_BEGIN

class GlyphPositionAdjustment : public UMemory {
public:

    GlyphPositionAdjustment();
    GlyphPositionAdjustment(float xPlace, float yPlace, float xAdv, float yAdv, le_int32 baseOff = -1);
    ~GlyphPositionAdjustment();

    float    getXPlacement();
    float    getYPlacement();
    float    getXAdvance();
    float    getYAdvance();

    le_int32 getBaseOffset();

    void     setXPlacement(float newXPlacement);
    void     setYPlacement(float newYPlacement);
    void     setXAdvance(float newXAdvance);
    void     setYAdvance(float newYAdvance);

    void     setBaseOffset(le_int32 newBaseOffset);

    void    adjustXPlacement(float xAdjustment);
    void    adjustYPlacement(float yAdjustment);
    void    adjustXAdvance(float xAdjustment);
    void    adjustYAdvance(float yAdjustment);

private:
    float xPlacement;
    float yPlacement;
    float xAdvance;
    float yAdvance;

    le_int32 baseOffset;

    // allow copying of this class because all of its fields are simple types
};

inline GlyphPositionAdjustment::GlyphPositionAdjustment()
  : xPlacement(0), yPlacement(0), xAdvance(0), yAdvance(0), baseOffset(-1)
{
    // nothing else to do!
}

inline GlyphPositionAdjustment::GlyphPositionAdjustment(float xPlace, float yPlace, float xAdv, float yAdv, le_int32 baseOff)
  : xPlacement(xPlace), yPlacement(yPlace), xAdvance(xAdv), yAdvance(yAdv), baseOffset(baseOff)
{
    // nothing else to do!
}

inline GlyphPositionAdjustment::~GlyphPositionAdjustment()
{
    // nothing to do!
}

inline float GlyphPositionAdjustment::getXPlacement()
{
    return xPlacement;
}

inline float GlyphPositionAdjustment::getYPlacement()
{
    return yPlacement;
}

inline float GlyphPositionAdjustment::getXAdvance()
{
    return xAdvance;
}

inline float GlyphPositionAdjustment::getYAdvance()
{
    return yAdvance;
}

inline le_int32 GlyphPositionAdjustment::getBaseOffset()
{
    return baseOffset;
}

inline void GlyphPositionAdjustment::setXPlacement(float newXPlacement)
{
    xPlacement = newXPlacement;
}

inline void GlyphPositionAdjustment::setYPlacement(float newYPlacement)
{
    yPlacement = newYPlacement;
}

inline void GlyphPositionAdjustment::setXAdvance(float newXAdvance)
{
    xAdvance = newXAdvance;
}

inline void GlyphPositionAdjustment::setYAdvance(float newYAdvance)
{
    yAdvance = newYAdvance;
}

inline void GlyphPositionAdjustment::setBaseOffset(le_int32 newBaseOffset)
{
    baseOffset = newBaseOffset;
}

inline void GlyphPositionAdjustment::adjustXPlacement(float xAdjustment)
{
    xPlacement += xAdjustment;
}

inline void GlyphPositionAdjustment::adjustYPlacement(float yAdjustment)
{
    yPlacement += yAdjustment;
}

inline void GlyphPositionAdjustment::adjustXAdvance(float xAdjustment)
{
    xAdvance += xAdjustment;
}

inline void GlyphPositionAdjustment::adjustYAdvance(float yAdjustment)
{
    yAdvance += yAdjustment;
}

U_NAMESPACE_END
#endif
