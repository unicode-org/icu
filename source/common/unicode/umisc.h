/*
**********************************************************************
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  umisc.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999oct15
*   created by: Markus W. Scherer
*/

#ifndef UMISC_H
#define UMISC_H

#include "unicode/utypes.h"

/**
 * \file
 * \brief  C API:misc definitions 
 *
 *  This file contains miscellaneous definitions for the C APIs. 
 */
 
/** A struct representing a range of text containing a specific field 
 *  @stable ICU 2.0
 */
typedef struct UFieldPosition {
  /**
   * The field 
   * @stable ICU 2.0
   */
  int32_t field;
  /**
   * The start of the text range containing field 
   * @stable ICU 2.0
   */
  int32_t beginIndex;
  /** 
   * The limit of the text range containing field 
   * @stable ICU 2.0
   */
  int32_t endIndex;
} UFieldPosition;

#endif
