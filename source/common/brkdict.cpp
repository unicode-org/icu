/*
**********************************************************************
*   Copyright (C) 1999-2000 IBM and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   12/1/99     rtg         Ported from Java
*   01/13/2000  helena      Added UErrorCode to ctors.
**********************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/resbund.h"
#include "brkdict.h"
#include "cmemory.h"

U_NAMESPACE_BEGIN

//=================================================================================
// deserialization
//=================================================================================

BreakDictionary::BreakDictionary(const char* /*dictionaryFilename*/, UErrorCode& status)
 :  columnMap(NULL),
    table(NULL),
    rowIndex(NULL),
    rowIndexFlags(NULL),
    rowIndexFlagsIndex(NULL),
    rowIndexShifts(NULL)
{
    if (U_FAILURE(status)) return;
    
    ResourceBundle th((char *)0, Locale("th"), status);

    if (U_FAILURE(status)) return;

    ResourceBundle th_dict = th.get("BreakDictionaryData", status);
    if (U_FAILURE(status)) return;

    int32_t len;
    const uint8_t * data = th_dict.getBinary(len, status);
    if (U_FAILURE(status)) return;

    UMemoryStream* dictionaryStream = uprv_mstrm_openBuffer(data, len);

    if (dictionaryStream == 0) {
        status = U_FILE_ACCESS_ERROR;
        return;
    }
    readDictionaryFile(dictionaryStream);
    uprv_mstrm_close(dictionaryStream);
}

BreakDictionary::~BreakDictionary()
{
    ucmp8_close(columnMap);
    uprv_free(table);
    uprv_free(rowIndex);
    uprv_free(rowIndexFlags);
    uprv_free(rowIndexFlagsIndex);
    uprv_free(rowIndexShifts);
}

// macros to support readDictionaryFile.  The data files originated from a Java
// program, and Java always writes data out in big-endian format.  These macros will
// byte-swap the data for appropriate use on Windows.

#if U_IS_BIG_ENDIAN
#define SWAP32(x)
#define SWAP16(x)
#else
#define SWAP32(x) x = (uint32_t)((x >> 24 & 0xff) | (x >> 8 & 0xff00) | (x << 8 & 0xff0000) | (x << 24 & 0xff000000))
#define SWAP16(x) x = (uint16_t)((x << 8 & 0xff00) | (x >> 8 & 0xff))
#endif

void
BreakDictionary::readDictionaryFile(UMemoryStream* in)
{
    int32_t l;
    int32_t version;

    int i;

    // read in the version number (right now we just ignore it)
    uprv_mstrm_read(in, &version, 4);

    // read in the column map (this is serialized in its internal form:
    // an index array followed by a data array)
    uprv_mstrm_read(in, &l, 4);
    SWAP32(l);
    uint16_t* temp = (uint16_t*) uprv_malloc(sizeof(uint16_t)*l);
    uprv_mstrm_read(in, temp, l * sizeof (int16_t) );
    for (i = 0; i < l; i++) {
        SWAP16(temp[i]);
    }
    uprv_mstrm_read(in, &l, 4);
    SWAP32(l);
    int8_t* temp2 = (int8_t*) uprv_malloc(sizeof(int8_t)*l);
    uprv_mstrm_read(in, temp2, l);
    columnMap = ucmp8_openAdopt(temp, temp2, l);

    // read in numCols and numColGroups
    uprv_mstrm_read(in, &numCols, 4);
    SWAP32(numCols);
    uprv_mstrm_read(in, &numColGroups, 4);
    SWAP32(numColGroups);

    // read in the row-number index
    uprv_mstrm_read(in, &l, 4);
    SWAP32(l);
    rowIndex = (int16_t *)uprv_malloc(l*2);
    uprv_mstrm_read(in, rowIndex, l * sizeof (int16_t) );
    for (i = 0; i < l; i++) {
        SWAP16(rowIndex[i]);
    }

    // load in the populated-cells bitmap: index first, then bitmap list
    uprv_mstrm_read(in, &l, 4);
    SWAP32(l);
    rowIndexFlagsIndex = (int16_t *)uprv_malloc(l*2);
    uprv_mstrm_read(in, rowIndexFlagsIndex, l * sizeof(int16_t) );
    for (i = 0; i < l; i++) {
        SWAP16(rowIndexFlagsIndex[i]);
    }
    uprv_mstrm_read(in, &l, 4);
    SWAP32(l);
    rowIndexFlags = (int32_t *)uprv_malloc(l*4);
    uprv_mstrm_read(in, rowIndexFlags, l * sizeof(int32_t));
    for (i = 0; i < l; i++) {
        SWAP32(rowIndexFlags[i]);
    }

    // load in the row-shift index
    uprv_mstrm_read(in, &l, 4);
    SWAP32(l);
    rowIndexShifts = (int8_t *)uprv_malloc(l);
    uprv_mstrm_read(in, rowIndexShifts, l);

    // finally, load in the actual state table
    uprv_mstrm_read(in, &l, 4);
    SWAP32(l);
    table = (int16_t *)uprv_malloc(l*2);
    uprv_mstrm_read(in, table, l * sizeof(int16_t) );
    for (i = 0; i < l; i++) {
        SWAP16(table[i]);
    }

    // the reverse column map occurs next in the file.  In the C/C++ code, for the
    // time being, we're not going to worry about that.
}

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
int16_t
BreakDictionary::at(int32_t row, UChar ch) const
{
    int16_t col = ucmp8_get(columnMap, ch);
    return at(row, (int32_t)col);
}

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
int16_t
BreakDictionary::at(int32_t row, int32_t col) const
{
    if (cellIsPopulated(row, col)) {
        // we map from logical to physical row number by looking up the
        // mapping in rowIndex; we map from logical column number to
        // physical column number by looking up a shift value for this
        // logical row and offsetting the logical column number by
        // the shift amount.  Then we can use internalAt() to actually
        // get the value out of the table.
        return internalAt(rowIndex[row], col + rowIndexShifts[row]);
    }
    else {
        return 0;
    }
}

//=================================================================================
// implementation
//=================================================================================
/**
 * Given (logical) row and column numbers, returns true if the
 * cell in that position is populated
 */
UBool
BreakDictionary::cellIsPopulated(int32_t row, int32_t col) const
{
    // look up the entry in the bitmap index for the specified row.
    // If it's a negative number, it's the column number of the only
    // populated cell in the row
    if (rowIndexFlagsIndex[row] < 0) {
        return col == -rowIndexFlagsIndex[row];
    }

    // if it's a positive number, it's the offset of an entry in the bitmap
    // list.  If the table is more than 32 columns wide, the bitmap is stored
    // successive entries in the bitmap list, so we have to divide the column
    // number by 32 and offset the number we got out of the index by the result.
    // Once we have the appropriate piece of the bitmap, test the appropriate
    // bit and return the result.
    else {
        int32_t flags = rowIndexFlags[rowIndexFlagsIndex[row] + (col >> 5)];
        return (flags & (1 << (col & 0x1f))) != 0;
    }
}

/**
 * Implementation of at() when we know the specified cell is populated.
 * @param row The PHYSICAL row number of the cell
 * @param col The PHYSICAL column number of the cell
 * @return The value stored in the cell
 */
int16_t
BreakDictionary::internalAt(int32_t row, int32_t col) const
{
    // the table is a one-dimensional array, so this just does the math necessary
    // to treat it as a two-dimensional array (we don't just use a two-dimensional
    // array because two-dimensional arrays are inefficient in Java)
    return table[row * numCols + col];
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
