/*
*****************************************************************************************
*                                                                                       
* Copyright © {1997-1999}, International Business Machines Corporation and others. All Rights Reserved.
*****************************************************************************************
*
* File WDBKTBL.H
*
* WordBreakTable implements a state transition table.
*
* @package  Text and International
* @category Text Scanning
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.  Made statics const.
*****************************************************************************************
*/

#ifndef WDBKTBL_H
#define WDBKTBL_H

#include "unicode/utypes.h"
#include "txtbdat.h"

/**
 * This class implements a state transition table.
 * After each transition, using the get method, the
 * new state is returned along with information about
 * the state change (ex. was it a "marked" transition").
 * For efficiency, none of the arguments to any of these
 * methods are validated.
 */
class WordBreakTable {
public:
    // For convenience
    typedef TextBoundaryData::Node Node;
    typedef TextBoundaryData::Type Type;

    /**
     * Construct a table from the provided data.  See CharacterBreakData or
     * the other TextBoundaryData subclasses for examples.  Each row represents
     * a state, each column within a row represents a transition.  The values
     * in the table represent the new state and mark information.
     * @param cols number of columns in the table (transitions)
     * @param data an encoded byte array containing state and transition data
     * @param data_length the length of the byte array data
     */
    WordBreakTable(int32_t cols, const Node data[], int32_t data_length);

    /**
     * Get the resulting state moving from oldState accepting input.
     * @param oldState current state
     * @param input input
     * @return resulting state and transition data
     */
    Node get(Node oldState, Type input) const;

    /**
     * Checks to see if the transition into the specified state was "marked."
     * @param state the state as returned by get, initialState, or endState
     * @return true if transition into state was marked.
     */
    UBool isMarkState(Node state) const;

    /**
     * Check to see if the state is the end state
     * @param state the state to check
     * @return true if state is an end state
     */
    UBool isEndState(Node state) const;

    /**
     * Get the initial state
     * @return the initial state
     */
    Node initialState(void) const;

private:
    static const Node  kMark_mask;
    static const Node  kIndex_mask;

    static const Node   kInitial_state;
    static const Node   kEnd_state;

    const TextBoundaryData::Node* fData;
    int32_t fData_length;
    int32_t fCols;
};

// -------------------------------------

inline WordBreakTable::Node
WordBreakTable::get(WordBreakTable::Node oldState, WordBreakTable::Type input) const
{
    return fData[(oldState & kIndex_mask) * fCols + input];
}

inline UBool
WordBreakTable::isMarkState(WordBreakTable::Node state) const
{
    return (state & kMark_mask) != 0;
}

inline UBool
WordBreakTable::isEndState(WordBreakTable::Node state) const
{
    return (state & kIndex_mask) == kEnd_state;
}

inline WordBreakTable::Node
WordBreakTable::initialState() const
{
    return kInitial_state;
}

#endif // _WDBKTBL
//eof
