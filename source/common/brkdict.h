/*
**********************************************************************
*   Copyright (C) 1999-2000 IBM and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   12/1/99     rtg         Ported from Java
*   01/13/2000  helena      Added UErrorCode to ctors.
**********************************************************************
*/

#ifndef BRKDICT_H
#define BRKDICT_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "ucmp8.h"
#include "umemstrm.h"

U_NAMESPACE_BEGIN

/**
 * This is the class that represents the list of known words used by
 * DictionaryBasedBreakIterator.  The conceptual data structure used
 * here is a trie: there is a node hanging off the root node for every
 * letter that can start a word.  Each of these nodes has a node hanging
 * off of it for every letter that can be the second letter of a word
 * if this node is the first letter, and so on.  The trie is represented
 * as a two-dimensional array that can be treated as a table of state
 * transitions.  Indexes are used to compress this array, taking
 * advantage of the fact that this array will always be very sparse.
 */
class BreakDictionary : public UMemory {
    //=================================================================================
    // data members
    //=================================================================================
private:

    /**
     * Maps from characters to column numbers.  The main use of this is to
     * avoid making room in the array for empty columns.
     */
    CompactByteArray* columnMap;

    /**
     * The number of actual columns in the table
     */
    int32_t numCols;

    /**
     * Columns are organized into groups of 32.  This says how many
     * column groups.  (We could calculate this, but we store the
     * value to avoid having to repeatedly calculate it.)
     */
    int32_t numColGroups;

    /**
     * The actual compressed state table.  Each conceptual row represents
     * a state, and the cells in it contain the row numbers of the states
     * to transition to for each possible letter.  0 is used to indicate
     * an illegal combination of letters (i.e., the error state).  The
     * table is compressed by eliminating all the unpopulated (i.e., zero)
     * cells.  Multiple conceptual rows can then be doubled up in a single
     * physical row by sliding them up and possibly shifting them to one
     * side or the other so the populated cells don't collide.  Indexes
     * are used to identify unpopulated cells and to locate populated cells.
     */
    int16_t* table;

    /**
     * This index maps logical row numbers to physical row numbers
     */
    int16_t* rowIndex;

    /**
     * A bitmap is used to tell which cells in the comceptual table are
     * populated.  This array contains all the unique bit combinations
     * in that bitmap.  If the table is more than 32 columns wide,
     * successive entries in this array are used for a single row.
     */
    int32_t* rowIndexFlags;

    /**
     * This index maps from a logical row number into the bitmap table above.
     * (This keeps us from storing duplicate bitmap combinations.)  Since there
     * are a lot of rows with only one populated cell, instead of wasting space
     * in the bitmap table, we just store a negative number in this index for
     * rows with one populated cell.  The absolute value of that number is
     * the column number of the populated cell.
     */
    int16_t* rowIndexFlagsIndex;

    /**
     * For each logical row, this index contains a constant that is added to
     * the logical column number to get the physical column number
     */
    int8_t* rowIndexShifts;

    //=================================================================================
    // deserialization
    //=================================================================================

public:
    /**
     * Constructor.  Creates the BreakDictionary by using readDictionaryFile() to
     * load the dictionary tables from the disk.
     * @param dictionaryFilename The name of the dictionary file
     * @param status for errors if it occurs
     */
    BreakDictionary(const char* dictionaryFilename, UErrorCode& status);

    /**
     * Destructor.
     */
    ~BreakDictionary();

    /**
     * Reads the dictionary file on the disk and constructs the appropriate in-memory
     * representation.
     * @param in The given memory stream
     */
    void readDictionaryFile(UMemoryStream* in);

    //=================================================================================
    // access to the words
    //=================================================================================

    /**
     * Uses the column map to map the character to a column number, then
     * passes the row and column number to the other version of at()
     * @param row The current state
     * @param ch The character whose column we're interested in
     * @return The new state to transition to
     */
    int16_t at(int32_t row, UChar ch) const;

    /**
     * Returns the value in the cell with the specified (logical) row and
     * column numbers.  In DictionaryBasedBreakIterator, the row number is
     * a state number, the column number is an input, and the return value
     * is the row number of the new state to transition to.  (0 is the
     * "error" state, and -1 is the "end of word" state in a dictionary)
     * @param row The row number of the current state
     * @param col The column number of the input character (0 means "not a
     * dictionary character")
     * @return The row number of the new state to transition to
     */
    int16_t at(int32_t row, int32_t col) const;

private:
    /**
     * Given (logical) row and column numbers, returns true if the
     * cell in that position is populated
     * @param row The LOGICAL row number of the cell
     * @param col The PHYSICAL row number of the cell
     * @return true if the cell in that position is populated
     */
    UBool cellIsPopulated(int32_t row, int32_t col) const;

    /**
     * Implementation of at() when we know the specified cell is populated.
     * @param row The PHYSICAL row number of the cell
     * @param col The PHYSICAL column number of the cell
     * @return The value stored in the cell
     */
    int16_t internalAt(int32_t row, int32_t col) const;

    // the following methods are never meant to be called and so are not defined
    // (if you don't declare them, you get default implementations)
    BreakDictionary(const BreakDictionary& that);
    BreakDictionary& operator=(const BreakDictionary& that);
};

U_NAMESPACE_END

#endif
