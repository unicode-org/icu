
/*
 * @(#)LESwaps.h	1.3 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __LESWAPS_H
#define __LESWAPS_H

#include "LETypes.h"

/**
 * These are convenience macros which invoke the swap functions
 * from a concise call.
 */
#define SWAPW(value) (LESwaps::isBigEndian() ? (value) : LESwaps::swapWord(value))
#define SWAPL(value) (LESwaps::isBigEndian() ? (value) : LESwaps::swapLong(value))

/**
 * This class is used to access data which stored in big endian order
 * regardless of the conventions of the platform. It has been designed
 * to automatically detect the endian-ness of the platform, so that a
 * compilation flag is not needed.
 *
 * All methods are static and inline in an attempt to induce the compiler
 * to do most of the calculations at compile time.
 */
class LESwaps
{
public:

	/**
	 * This method detects the endian-ness of the platform by
	 * casting a pointer to a word to a pointer to a byte. On
	 * big endian platforms the FF will be in the byte with the
	 * lowest address. On little endian platforms, the FF will
	 * be in the byte with the highest address.
	 *
	 * @return true if the platform is big endian
	 */
    static le_bool isBigEndian()
    {
        static le_uint16 word = 0xFF00;
        static le_uint8 *byte = (le_uint8 *) &word;

        return *byte;
    };

	/**
	 * This method the byte swap required on little endian platforms
	 * to correctly access a word.
	 *
	 * @param value - the word to be byte swapped
	 *
	 * @return the byte swapped word
	 */
    static le_uint16 swapWord(le_uint16 value)
    {
        return (((le_uint8) (value >> 8)) | (value << 8));
    };

	/**
	 * This method does the byte swapping required on little endian platforms
	 * to correctly access a long.
	 *
	 * @param value - the long to be byte swapped
	 *
	 * @return the byte swapped long
	 */
    static le_uint32 swapLong(le_uint32 value)
    {
        return swapWord((le_uint16) (value >> 16)) | (swapWord((le_uint16) value) << 16);
    };
};

#endif
