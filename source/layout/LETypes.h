
/*
 * @(#)LETypes.h	1.2 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __LETYPES_H
#define __LETYPES_H

typedef long le_int32;
typedef unsigned long le_uint32;
typedef short le_int16;
typedef unsigned short le_uint16;
typedef signed char le_int8;
typedef unsigned char le_uint8;

typedef char le_bool;

#ifndef true
#define true 1
#endif

#ifndef false
#define false 0
#endif

#ifndef NULL
#define NULL 0
#endif

typedef le_uint32 LETag;

typedef le_uint16 LEGlyphID;

typedef le_uint16 LEUnicode16;
typedef le_uint32 LEUnicode32;
typedef le_uint16 LEUnicode;	// FIXME: we should depricate this type in favor of LEUnicode16...

struct LEPoint
{
    float fX;
    float fY;
};

#endif


