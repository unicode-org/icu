/*
 **********************************************************************
 *   Copyright (C) 2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */

#ifndef __RUNARRAYS_H

#define __RUNARRAYS_H

#include "layout/LETypes.h"
#include "layout/LEFontInstance.h"

#define INITIAL_CAPACITY 16
#define CAPACITY_GROW_LIMIT 128

/**
 * The <code>RunArray</code> class is a base class for building classes
 * which represent data that is associated with runs of text. This class
 * maintains an array of limit indicies into the text, subclasses
 * provide one or more arrays of data.
 *
 * @draft ICU 2.6
 */
class RunArray
{
public:
    /**
     * Construct a <code>RunArray</code> object from a pre-existing
     * array of limit indices.
     *
     * @param limits is an array of limit indicies.
     *
     * @param count is the number of entries in the limit array.
     *
     * @draft ICU 2.6
     */
    RunArray(const le_int32 *limits, le_int32 count);

    /**
     * Construct an empty <code>RunArray</code> object. Clients can add limit
     * indicies array using the <code>add</code> method.
     *
     * @param initialCapacity is the initial size of the limit indicies array. If
     *        this value is zero, no array will be allocated.
     *
     * @see add
     *
     * @draft ICU 2.6
     */
    RunArray(le_int32 initalCapacity);

    /**
     * The destructor; virtual so that subclass destructors are invoked as well.
     *
     * @draft ICU 2.6
     */
    virtual ~RunArray();

    /**
     * Get the number of entries in the limit indicies array.
     *
     * @return the number of entries in the limit indices array.
     *
     * @draft ICU 2.6
     */
    le_int32 getCount() const;

    /**
     * Get the last limit index. This is the number of characters in
     * the text.
     *
     * @return the last limit index.
     *
     * @draft ICU 2.6
     */
    le_int32 getLimit() const;

    /**
     * Get the limit index for a particular run of text.
     *
     * @param run is the run. This is an index into the limit index array.
     *
     * @return the limit index for the run, or -1 if <code>run</code> is out of bounds.
     *
     * @draft ICU 2.6
     */
    le_int32 getLimit(le_int32 run) const;

    /**
     * Add a limit index to the limit indicies array and return the run index
     * where it was stored. If the array does not exist, it will be created by
     * calling the <code>init</code> method. If it is full, it will be grown by
     * calling the <code>grow</code> method.
     *
     * If the <code>RunArray</code> object was created with a client-supplied
     * limit indicies array, this method will return a run index of -1.
     *
     * Subclasses should not override this method. Rather they should provide
     * a new <code>add</code> method which takes a limit index along with whatever
     * other data they implement. The new <code>add</code> method should
     * first call this method to grow the data arrays, and use the return value
     * to store the data in their own arrays.
     *
     * @param limit is the limit index to add to the array.
     *
     * @return the run index where the limit index was stored, or -1 if the limit index cannt be stored.
     *
     * @see init
     * @see grow
     *
     * @draft ICU 2.6
     */
    le_int32 add(le_int32 limit);

protected:
    /**
     * Create a data array with the given inital size. This method will be
     * called by the <code>add</code> method if there is no limit indicies
     * array. Subclasses which override this method must also call it from
     * the overridding method to create the limit indicies array.
     *
     * @param capacity is the initial size of the data array.
     *
     * @see add
     *
     * @draft ICU 2.6
     */
    virtual void init(le_int32 capacity);

    /**
     * Grow a data array to the given inital size. This method will be
     * called by the <code>add</code> method if the limit indicies
     * array is full. Subclasses which override this method must also call it from
     * the overridding method to grow the limit indicies array.
     *
     * @param capacity is the initial size of the data array.
     *
     * @see add
     *
     * @draft ICU 2.6
     */
    virtual void grow(le_int32 capacity);

    /**
     * Set by the constructors to indicate whether
     * or not the client supplied the data arrays.
     * If they were supplied by the client, the 
     * <code>add</code> method won't change the arrays
     * and the destructor won't delete them.
     */
    le_bool fClientArrays;

private:
    le_int32 ensureCapacity();

    const le_int32 *fLimits;
          le_int32  fCount;
          le_int32  fCapacity;
};

inline RunArray::RunArray(const le_int32 *limits, le_int32 count)
    : fClientArrays(true), fLimits(limits), fCount(count), fCapacity(count)
{
    // nothing else to do...
}

inline RunArray::RunArray(le_int32 initialCapacity)
    : fClientArrays(false), fLimits(NULL), fCount(0), fCapacity(initialCapacity)
{
    if (initialCapacity > 0) {
        fLimits = LE_NEW_ARRAY(le_int32, fCapacity);
    }
}

inline RunArray::~RunArray()
{
    if (! fClientArrays) {
        LE_DELETE_ARRAY(fLimits);
        fLimits = NULL;
    }
}

inline le_int32 RunArray::getCount() const
{
    return fCount;
}

inline le_int32 RunArray::getLimit() const
{
    return getLimit(fCount - 1);
}

inline le_int32 RunArray::getLimit(le_int32 run) const
{
    if (run < 0 || run >= fCount) {
        return -1;
    }

    return fLimits[run];
}

/**
 * The <code>FontRuns</code> class associates pointers to <code>LEFontInstance</code>
 * objects with runs of text.
 *
 * @draft ICU 2.6
 */
class FontRuns : public RunArray
{
public:
    /**
     * Construct a <code>FontRuns</code> object from pre-existing arrays of fonts
     * and limit indicies.
     *
     * @param fonts is the address of an array of pointers to <code>LEFontInstance</code> objects.
     *
     * @param limits is the address of an array of limit indicies.
     *
     * @param count is the number of entries in the two arrays.
     *
     * @draft ICU 2.6
     */
    FontRuns(const LEFontInstance **fonts, const le_int32 *limits, le_int32 count);

    /**
     * Construct an empty <code>FontRuns</code> object. Clients can add font and limit
     * indicies arrays using the <code>add</code> method.
     *
     * @param initialCapacity is the initial size of the font and limit indicies arrays. If
     *        this value is zero, no arrays will be allocated.
     *
     * @see add
     *
     * @draft ICU 2.6
     */
    FontRuns(le_int32 initialCapacity);

    /**
     * The destructor; virtual so that subclass destructors are invoked as well.
     *
     * @draft ICU 2.6
     */
    virtual ~FontRuns();

    /**
     * Get the <code>LEFontInstance</code> object assoicated with the given run
     * of text. Use <code>RunArray::getLimit(run)</code> to get the corresponding
     * limit index.
     *
     * @param run is the index into the font and limit indicies arrays.
     *
     * @return the <code>LEFontInstance</code> associated with the given text run.
     *
     * @see RunArray::getLimit
     *
     * @draft ICU 2.6
     */
    const LEFontInstance *getFont(le_int32 run) const;


    /**
     * Add an <code>LEFontInstance</code> and limit index pair to the data arrays and return
     * the run index where the data was stored. This  method calls
     * <code>RunArray::add(limit)</code> which will create or grow the arrays as needed.
     *
     * If the <code>FontRuns</code> object was created with a client-supplied
     * font and limit indicies arrays, this method will return a run index of -1.
     *
     * Subclasses should not override this method. Rather they should provide a new <code>add</code>
     * method which takes a font and a limit index along with whatever other data they implement.
     * The new <code>add</code> method should first call this method to grow the font and limit indicies
     * arrays, and use the returned run index to store data their own arrays.
     *
     * @param font is the address of the <code>LEFontInstance</code> to add
     *
     * @param limit is the limit index to add
     *
     * @return the run index where the font and limit index were stored, or -1 if the data cannot be stored.
     *
     * @draft ICU 2.6
     */
    le_int32 add(const LEFontInstance *font, le_int32 limit);

protected:
    virtual void init(le_int32 capacity);
    virtual void grow(le_int32 capacity);

private:
    const LEFontInstance **fFonts;
};


inline FontRuns::FontRuns(const LEFontInstance **fonts, const le_int32 *limits, le_int32 count)
    : RunArray(limits, count), fFonts(fonts)
{
    // nothing else to do...
}

inline FontRuns::FontRuns(le_int32 initialCapacity)
    : RunArray(initialCapacity), fFonts(NULL)
{
    if (initialCapacity > 0) {
        fFonts = LE_NEW_ARRAY(const LEFontInstance *, initialCapacity);
    }
}

inline FontRuns::~FontRuns()
{
    if (! fClientArrays) {
        LE_DELETE_ARRAY(fFonts);
        fFonts = NULL;
    }
}

/**
 * The <code>ValueRuns</code> class associates integer values with runs of text.
 *
 * @draft ICU 2.6
 */
class ValueRuns : public RunArray
{
public:
    /**
     * Construct a <code>ValueRuns</code> object from pre-existing arrays of values
     * and limit indicies.
     *
     * @param values is the address of an array of integer.
     *
     * @param limits is the address of an array of limit indicies.
     *
     * @param count is the number of entries in the two arrays.
     *
     * @draft ICU 2.6
     */
    ValueRuns(const le_int32 *values, const le_int32 *limits, le_int32 count);

    /**
     * Construct an empty <code>ValueRuns</code> object. Clients can add value and limit
     * indicies arrays using the <code>add</code> method.
     *
     * @param initialCapacity is the initial size of the value and limit indicies arrays. If
     *        this value is zero, no arrays will be allocated.
     *
     * @see add
     *
     * @draft ICU 2.6
     */
    ValueRuns(le_int32 initialCapacity);

    /**
     * The destructor; virtual so that subclass destructors are invoked as well.
     *
     * @draft ICU 2.6
     */
    virtual ~ValueRuns();

    /**
     * Get the integer value assoicated with the given run
     * of text. Use <code>RunArray::getLimit(run)</code> to get the corresponding
     * limit index.
     *
     * @param run is the index into the font and limit indicies arrays.
     *
     * @return the integer value associated with the given text run.
     *
     * @see RunArray::getLimit
     *
     * @draft ICU 2.6
     */
    le_int32 getValue(le_int32 run) const;


    /**
     * Add an integer value and limit index pair to the data arrays and return
     * the run index where the data was stored. This  method calls
     * <code>RunArray::add(limit)</code> which will create or grow the arrays as needed.
     *
     * If the <code>ValueRuns</code> object was created with a client-supplied
     * font and limit indicies arrays, this method will return a run index of -1.
     *
     * Subclasses should not override this method. Rather they should provide a new <code>add</code>
     * method which takes an integer value and a limit index along with whatever other data they implement.
     * The new <code>add</code> method should first call this method to grow the font and limit indicies
     * arrays, and use the returned run index to store data their own arrays.
     *
     * @param value is the integer value to add
     *
     * @param limit is the limit index to add
     *
     * @return the run index where the value and limit index were stored, or -1 if the data cannot be stored.
     *
     * @draft ICU 2.6
     */
    le_int32 add(le_int32 value, le_int32 limit);

protected:
    virtual void init(le_int32 capacity);
    virtual void grow(le_int32 capacity);

private:
    const le_int32 *fValues;
};


inline ValueRuns::ValueRuns(const le_int32 *values, const le_int32 *limits, le_int32 count)
    : RunArray(limits, count), fValues(values)
{
    // nothing else to do...
}

inline ValueRuns::ValueRuns(le_int32 initialCapacity)
    : RunArray(initialCapacity), fValues(NULL)
{
    if (initialCapacity > 0) {
        fValues = LE_NEW_ARRAY(le_int32, initialCapacity);
    }
}

inline ValueRuns::~ValueRuns()
{
    if (! fClientArrays) {
        LE_DELETE_ARRAY(fValues);
        fValues = NULL;
    }
}

#endif
