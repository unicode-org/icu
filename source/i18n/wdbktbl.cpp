/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*
* File WDBKTBL.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.  Made statics const.
*****************************************************************************************
*/

// *****************************************************************************
// This file was generated from the java source file WordBreakTable.java
// *****************************************************************************

#include "wdbktbl.h"

// *****************************************************************************
// class WordBreakTable
//
// The word break table implements a state machine that leads to the next
// transition state from the current one and is used by BreakIterator for 
// character, word or sentence.  To better illustrate the use of transition
// tables, the  following example shows a very simplified version of the 
// word break table that deals with only kNB  (not a blank char) and kB
// (a blank char) character categories. The state machine of the word break 
// table would look like,
//
//    Diagram 1 : the state machine for kNB and kB
//
//                         kNB
//                         ----
//            kNB   +----+/    \
//           ------>|SI+1|      |
//          /       +----+<----/ 
//    +----+         kB|          kNB     +-------+
// 0->|stop|           V   -------------> |SI_stop|
//    +----+\------>+----+/               +-------+
//                  |SI+2|<----\
//             kB   +----+      |
//                        \----/
//                          kB
//
//  Table 1 : flattened state table for Diagram 1
//  ---------------------------------------------
//  States       kB               kNB
//    0         stop             stop
//    1         SI+2             SI+1
//    2         SI+2             SI_stop
//
// In the table, SI+n shows where the characters will be "marked" and led
// to a different state if necessary.  For example, consider the string 
// "This is a test.".
// Iterating through the string shows the following,
// (stop)->'T'(SI+1)->'h'(SI+1)->'i'(SI+1)->'s'(SI+1)->' '(SI+2)->i(SI_stop)
// When a (SI_stop) is reached, we know that we have found a word break right 
// after ' '.  
//
// The actual char, word and sentence break data is a lot more complicated 
// than the above.  The character type showed here is only limited to kNB
// and kB for ease of demonstration.  All the break tables are essentially
// a flattened state table of their orginal state machine diagrams.
//
// *****************************************************************************

// -------------------------------------

WordBreakTable::WordBreakTable(int32_t cols, const WordBreakTable::Node data[], int32_t data_length)
  : fData(data), fData_length(data_length), fCols(cols)
{
}

// -------------------------------------

const WordBreakTable::Node WordBreakTable::kMark_mask = (WordBreakTable::Node)0x80;

const WordBreakTable::Node WordBreakTable::kIndex_mask = (WordBreakTable::Node)0x7F;

const WordBreakTable::Node WordBreakTable::kInitial_state = 1;

const WordBreakTable::Node WordBreakTable::kEnd_state = 0;

//eof
