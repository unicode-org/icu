/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ushape.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000jun29
*   created by: Markus W. Scherer
*/

#ifndef __USHAPE_H__
#define __USHAPE_H__

#include "unicode/utypes.h"

/**
 * Shape Arabic text on a character basis.
 * ### TBD
 */
U_CAPI int32_t U_EXPORT2
u_shapeArabic(const UChar *source, int32_t sourceLength,
              UChar *dest, int32_t destSize,
              uint32_t options,
              UErrorCode *pErrorCode);

#define U_SHAPE_LENGTH_GROW_SHRINK              0
#define U_SHAPE_LENGTH_FIXED_SPACES_NEAR        1
#define U_SHAPE_LENGTH_FIXED_SPACES_AT_END      2
#define U_SHAPE_LENGTH_RESERVED                 3
#define U_SHAPE_LENGTH_MASK                     3

#define U_SHAPE_TEXT_DIRECTION_LOGICAL          0
#define U_SHAPE_TEXT_DIRECTION_VISUAL_LTR       4
#define U_SHAPE_TEXT_DIRECTION_MASK             4

#define U_SHAPE_LETTERS_NOOP                    0
#define U_SHAPE_LETTERS_SHAPE                   8
#define U_SHAPE_LETTERS_UNSHAPE                 0x10
#define U_SHAPE_LETTERS_RESERVED                0x18
#define U_SHAPE_LETTERS_MASK                    0x18

#define U_SHAPE_DIGITS_NOOP                     0
#define U_SHAPE_DIGITS_EN2AN                    0x20
#define U_SHAPE_DIGITS_AN2EN                    0x40
#define U_SHAPE_DIGITS_ALEN2AN_INIT_LR          0x60
#define U_SHAPE_DIGITS_ALEN2AN_INIT_AL          0x80
#define U_SHAPE_DIGITS_RESERVED1                0xa0
#define U_SHAPE_DIGITS_RESERVED2                0xc0
#define U_SHAPE_DIGITS_RESERVED3                0xe0
#define U_SHAPE_DIGITS_MASK                     0xe0

#define U_SHAPE_DIGIT_TYPE_AN                   0
#define U_SHAPE_DIGIT_TYPE_AN_EXTENDED          0x100
#define U_SHAPE_DIGIT_TYPE_MASK                 0x100

#endif
