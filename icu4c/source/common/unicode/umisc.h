/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
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

/* This file contains miscellaneous definitions for the C APIs. */

/** A struct representing a range of text containing a specific field */
struct UFieldPosition {
  /** The field */
  int32_t field;
  /** The start of the text range containing field */
  int32_t beginIndex;
  /** The limit of the text range containing field */
  int32_t endIndex;
};
typedef struct UFieldPosition UFieldPosition;

#endif
