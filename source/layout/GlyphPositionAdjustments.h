/*
 * @(#)GlyphPositionAdjustments.h	1.8 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __GLYPHPOSITIONADJUSTMENTS_H
#define __GLYPHPOSITIONADJUSTMENTS_H

#include "LETypes.h"
#include "OpenTypeTables.h"


class GlyphPositionAdjustment
{
public:

    GlyphPositionAdjustment();
    GlyphPositionAdjustment(float xPlace, float yPlace, float xAdv, float yAdv);
    ~GlyphPositionAdjustment();

    float   getXPlacement();
    float   getYPlacement();
    float   getXAdvance();
    float   getYAdvance();

    void    setXPlacement(float newXPlacement);
    void    setYPlacement(float newYPlacement);
    void    setXAdvance(float newXAdvance);
    void    setYAdvance(float newYAdvance);

    void    adjustXPlacement(float xAdjustment);
    void    adjustYPlacement(float yAdjustment);
    void    adjustXAdvance(float xAdjustment);
    void    adjustYAdvance(float yAdjustment);

private:
    float xPlacement;
    float yPlacement;
    float xAdvance;
    float yAdvance;
};

inline GlyphPositionAdjustment::GlyphPositionAdjustment()
  : xPlacement(0), yPlacement(0), xAdvance(0), yAdvance(0)
{
    // nothing else to do!
}

inline GlyphPositionAdjustment::GlyphPositionAdjustment(float xPlace, float yPlace, float xAdv, float yAdv)
  : xPlacement(xPlace), yPlacement(yPlace), xAdvance(xAdv), yAdvance(yAdv)
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

#endif
