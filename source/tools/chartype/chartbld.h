/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1996                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/
//	Copyright (C) 1994-1995 Taligent, Inc. All rights reserved.
//
//  FILE NAME : chartbld.h
//
//	CREATED
//		Wednesday, December 11, 1996
//
//	CREATED BY
//		Helena Shih
//
//
//********************************************************************************************


#ifndef _CHARTBLD
#define _CHARTBLD


#include "utypes.h"



#include "ucmp8.h"


/**
 * CharTypeBuilder is an internal class that creates a CompactByteArray for use by
 * CharType.  The array is constructed from a data file.  The name is specified in
 * the hard coded constant INPUT_FILE_NAME.  CharTypeBuilder is run as an application
 * and the output sent to System.out is then copied into the CharType.java source file.
 */
class CharTypeBuilder
{
public :

	enum	ECharTypeMapping {
	UNASSIGNED				= 0,
	UPPERCASE_LETTER		= 1,
	LOWERCASE_LETTER		= 2,
	TITLECASE_LETTER		= 3,
	MODIFIER_LETTER			= 4,
	OTHER_LETTER			= 5,
	NON_SPACING_MARK		= 6,
	ENCLOSING_MARK			= 7,
	COMBINING_SPACING_MARK	= 8,
	DECIMAL_DIGIT_NUMBER	= 9,
	LETTER_NUMBER			= 10,
	OTHER_NUMBER			= 11,
	SPACE_SEPARATOR			= 12,
	LINE_SEPARATOR			= 13,
	PARAGRAPH_SEPARATOR		= 14,
	CONTROL					= 15,
	FORMAT					= 16,
	PRIVATE_USE				= 17,
	SURROGATE				= 18,
	DASH_PUNCTUATION		= 19,
	START_PUNCTUATION		= 20,
	END_PUNCTUATION			= 21,
    CONNECTOR_PUNCTUATION	= 22,
	OTHER_PUNCTUATION		= 23,
	MATH_SYMBOL				= 24,
	CURRENCY_SYMBOL			= 25,
	MODIFIER_SYMBOL			= 26,
	OTHER_SYMBOL			= 27,
	INITIAL_PUNCTUATION		= 28,
	FINAL_PUNCTUATION		= 29,
	};

	static CompactByteArray* getByteArray(FILE*);
	static void	writeByteArrays();

private :
	static int MakeProp(char* str);

	static const char  tagStrings[];
	static const short tagValues[];

	//LAST_CHAR_CODE_IN_FILE is taken from the data file itself. If the
	//  data file changes, this value may need to be changed also.
	//  After this value is read, the program exits.
	static const UChar LAST_CHAR_CODE_IN_FILE;

	static CompactByteArray *charTypeArray;
};
#endif
