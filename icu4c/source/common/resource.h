// Copyright (C) 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
* Copyright (C) 2015-2016, International Business Machines
* Corporation and others.  All Rights Reserved.
*******************************************************************************
* resource.h
*
* created on: 2015nov04
* created by: Markus W. Scherer
*/

#ifndef __URESOURCE_H__
#define __URESOURCE_H__

/**
 * \file
 * \brief ICU resource bundle key and value types.
 */

// Note: Ported from ICU4J class UResource and its nested classes,
// but the C++ classes are separate, not nested.

// We use the Resource prefix for C++ classes, as usual.
// The UResource prefix would be used for C types.

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/ures.h"

struct ResourceData;

U_NAMESPACE_BEGIN

class ResourceTableSink;
class ResourceValue;

// Note: In C++, we use const char * pointers for keys,
// rather than an abstraction like Java UResource.Key.

/**
 * Interface for iterating over a resource bundle array resource.
 */
class U_COMMON_API ResourceArray {
public:
    /** Constructs an empty array object. */
    ResourceArray() : items16(NULL), items32(NULL), length(0) {}

    /** Only for implementation use. @internal */
    ResourceArray(const uint16_t *i16, const uint32_t *i32, int32_t len) :
            items16(i16), items32(i32), length(len) {}

    /**
     * @return The number of items in the array resource.
     */
    int32_t getSize() const { return length; }
    /**
     * @param i Array item index.
     * @param value Output-only, receives the value of the i'th item.
     * @return TRUE if i is non-negative and less than getSize().
     */
    UBool getValue(int32_t i, ResourceValue &value) const;

    /** Only for implementation use. @internal */
    uint32_t internalGetResource(const ResourceData *pResData, int32_t i) const;

private:
    const uint16_t *items16;
    const uint32_t *items32;
    int32_t length;
};

/**
 * Interface for iterating over a resource bundle table resource.
 */
class U_COMMON_API ResourceTable {
public:
    /** Constructs an empty table object. */
    ResourceTable() : keys16(NULL), keys32(NULL), items16(NULL), items32(NULL), length(0) {}

    /** Only for implementation use. @internal */
    ResourceTable(const uint16_t *k16, const int32_t *k32,
                  const uint16_t *i16, const uint32_t *i32, int32_t len) :
            keys16(k16), keys32(k32), items16(i16), items32(i32), length(len) {}

    /**
     * @return The number of items in the array resource.
     */
    int32_t getSize() const { return length; }
    /**
     * @param i Array item index.
     * @param key Output-only, receives the key of the i'th item.
     * @param value Output-only, receives the value of the i'th item.
     * @return TRUE if i is non-negative and less than getSize().
     */
    UBool getKeyAndValue(int32_t i, const char *&key, ResourceValue &value) const;

private:
    const uint16_t *keys16;
    const int32_t *keys32;
    const uint16_t *items16;
    const uint32_t *items32;
    int32_t length;
};

/**
 * Represents a resource bundle item's value.
 * Avoids object creations as much as possible.
 * Mutable, not thread-safe.
 */
class U_COMMON_API ResourceValue : public UObject {
public:
    virtual ~ResourceValue();

    /**
     * @return ICU resource type, for example, URES_STRING
     */
    virtual UResType getType() const = 0;

    /**
     * Sets U_RESOURCE_TYPE_MISMATCH if this is not a string resource.
     *
     * @see ures_getString()
     */
    virtual const UChar *getString(int32_t &length, UErrorCode &errorCode) const = 0;

    inline UnicodeString getUnicodeString(UErrorCode &errorCode) const {
        int32_t len = 0;
        const UChar *r = getString(len, errorCode);
        return UnicodeString(TRUE, r, len);
    }

    /**
     * Sets U_RESOURCE_TYPE_MISMATCH if this is not an alias resource.
     */
    virtual const UChar *getAliasString(int32_t &length, UErrorCode &errorCode) const = 0;

    inline UnicodeString getAliasUnicodeString(UErrorCode &errorCode) const {
        int32_t len = 0;
        const UChar *r = getAliasString(len, errorCode);
        return UnicodeString(TRUE, r, len);
    }

    /**
     * Sets U_RESOURCE_TYPE_MISMATCH if this is not an integer resource.
     *
     * @see ures_getInt()
     */
    virtual int32_t getInt(UErrorCode &errorCode) const = 0;

    /**
     * Sets U_RESOURCE_TYPE_MISMATCH if this is not an integer resource.
     *
     * @see ures_getUInt()
     */
    virtual uint32_t getUInt(UErrorCode &errorCode) const = 0;

    /**
     * Sets U_RESOURCE_TYPE_MISMATCH if this is not an intvector resource.
     *
     * @see ures_getIntVector()
     */
    virtual const int32_t *getIntVector(int32_t &length, UErrorCode &errorCode) const = 0;

    /**
     * Sets U_RESOURCE_TYPE_MISMATCH if this is not a binary-blob resource.
     *
     * @see ures_getBinary()
     */
    virtual const uint8_t *getBinary(int32_t &length, UErrorCode &errorCode) const = 0;

    /**
     * Sets U_RESOURCE_TYPE_MISMATCH if this is not an array resource
     */
    virtual ResourceArray getArray(UErrorCode &errorCode) const = 0;

    /**
     * Sets U_RESOURCE_TYPE_MISMATCH if this is not a table resource
     */
    virtual ResourceTable getTable(UErrorCode &errorCode) const = 0;

    /**
     * Is this a no-fallback/no-inheritance marker string?
     * Such a marker is used for
     * CLDR no-fallback data values of (three empty-set symbols)=={2205, 2205, 2205}
     * when enumerating tables with fallback from the specific resource bundle to root.
     *
     * @return TRUE if this is a no-inheritance marker string
     */
    virtual UBool isNoInheritanceMarker() const = 0;

    /**
     * Sets the dest strings from the string values in this array resource.
     *
     * @return the number of strings in this array resource.
     *     If greater than capacity, then an overflow error is set.
     *
     * Sets U_RESOURCE_TYPE_MISMATCH if this is not an array resource
     *     or if any of the array items is not a string
     */
    virtual int32_t getStringArray(UnicodeString *dest, int32_t capacity,
                                   UErrorCode &errorCode) const = 0;

    /**
     * Same as
     * <pre>
     * if (getType() == URES_STRING) {
     *     return new String[] { getString(); }
     * } else {
     *     return getStringArray();
     * }
     * </pre>
     *
     * Sets U_RESOURCE_TYPE_MISMATCH if this is
     *     neither a string resource nor an array resource containing strings
     * @see getString()
     * @see getStringArray()
     */
    virtual int32_t getStringArrayOrStringAsArray(UnicodeString *dest, int32_t capacity,
                                                  UErrorCode &errorCode) const = 0;

    /**
     * Same as
     * <pre>
     * if (getType() == URES_STRING) {
     *     return getString();
     * } else {
     *     return getStringArray()[0];
     * }
     * </pre>
     *
     * Sets U_RESOURCE_TYPE_MISMATCH if this is
     *     neither a string resource nor an array resource containing strings
     * @see getString()
     * @see getStringArray()
     */
    virtual UnicodeString getStringOrFirstOfArray(UErrorCode &errorCode) const = 0;

protected:
    ResourceValue() {}

private:
    ResourceValue(const ResourceValue &);  // no copy constructor
    ResourceValue &operator=(const ResourceValue &);  // no assignment operator
};

/**
 * Sink for ICU resource bundle contents.
 */
class U_COMMON_API ResourceSink : public UObject {
public:
    ResourceSink() {}
    virtual ~ResourceSink();

    /**
     * Called once for each bundle (child-parent-...-root).
     * The value is normally an array or table resource,
     * and implementations of this method normally iterate over the
     * tree of resource items stored there.
     *
     * @param key The key string of the enumeration-start resource.
     *     Empty if the enumeration starts at the top level of the bundle.
     * @param value Call getArray() or getTable() as appropriate.
     *     Then reuse for output values from Array and Table getters.
     * @param noFallback true if the bundle has no parent;
     *     that is, its top-level table has the nofallback attribute,
     *     or it is the root bundle of a locale tree.
     */
    virtual void put(const char *key, ResourceValue &value, UBool noFallback,
                     UErrorCode &errorCode) = 0;

private:
    ResourceSink(const ResourceSink &);  // no copy constructor
    ResourceSink &operator=(const ResourceSink &);  // no assignment operator
};

/**
 * Sink for ICU resource array contents.
 * The base class does nothing.
 *
 * Nested arrays and tables are stored as nested sinks,
 * never put() as ResourceValue items.
 */
class U_COMMON_API ResourceArraySink : public UObject {
public:
    ResourceArraySink() {}
    virtual ~ResourceArraySink();

    /**
     * "Enters" the array.
     * Called just before enumerating the array's resource items.
     * The size can be used to allocate storage for the items.
     * It may differ between child and parent bundles.
     *
     * @param size number of array items
     */
    virtual void enter(int32_t size, UErrorCode &errorCode);

    /**
     * Adds a value from a resource array.
     *
     * @param index of the resource array item
     * @param value resource value
     */
    virtual void put(int32_t index, const ResourceValue &value, UErrorCode &errorCode);

    /**
     * Returns a nested resource array at the array index as another sink.
     * Creates the sink if none exists for the key.
     * Returns NULL if nested arrays are not supported.
     * The default implementation always returns NULL.
     *
     * This sink (not the caller) owns the nested sink.
     *
     * @param index of the resource array item
     * @return nested-array sink, or NULL
     */
    virtual ResourceArraySink *getOrCreateArraySink(int32_t index, UErrorCode &errorCode);

    /**
     * Returns a nested resource table at the array index as another sink.
     * Creates the sink if none exists for the key.
     * Returns NULL if nested tables are not supported.
     * The default implementation always returns NULL.
     *
     * This sink (not the caller) owns the nested sink.
     *
     * @param index of the resource array item
     * @return nested-table sink, or NULL
     */
    virtual ResourceTableSink *getOrCreateTableSink(int32_t index, UErrorCode &errorCode);

    /**
     * "Leaves" the array.
     * Indicates that all of the resources and sub-resources of the current array
     * have been enumerated.
     */
    virtual void leave(UErrorCode &errorCode);

private:
    ResourceArraySink(const ResourceArraySink &);  // no copy constructor
    ResourceArraySink &operator=(const ResourceArraySink &);  // no assignment operator
};

/**
 * Sink for ICU resource table contents.
 * The base class does nothing.
 *
 * Nested arrays and tables are stored as nested sinks,
 * never put() as ResourceValue items.
 */
class U_COMMON_API ResourceTableSink : public UObject {
public:
    ResourceTableSink() {}
    virtual ~ResourceTableSink();

    /**
     * "Enters" the table.
     * Called just before enumerating the table's resource items.
     * The size can be used to allocate storage for the items.
     * It usually differs between child and parent bundles.
     *
     * @param size number of table items
     */
    virtual void enter(int32_t size, UErrorCode &errorCode);

    /**
     * Adds a key-value pair from a resource table.
     *
     * @param key resource key string
     * @param value resource value
     */
    virtual void put(const char *key, const ResourceValue &value, UErrorCode &errorCode);

    /**
     * Adds a no-fallback/no-inheritance marker for this key.
     * Used for CLDR no-fallback data values of (three empty-set symbols)=={2205, 2205, 2205}
     * when enumerating tables with fallback from the specific resource bundle to root.
     *
     * The default implementation does nothing.
     *
     * @param key to be removed
     */
    virtual void putNoFallback(const char *key, UErrorCode &errorCode);

    /**
     * Returns a nested resource array for the key as another sink.
     * Creates the sink if none exists for the key.
     * Returns NULL if nested arrays are not supported.
     * The default implementation always returns NULL.
     *
     * This sink (not the caller) owns the nested sink.
     *
     * @param key resource key string
     * @return nested-array sink, or NULL
     */
    virtual ResourceArraySink *getOrCreateArraySink(const char *key, UErrorCode &errorCode);

    /**
     * Returns a nested resource table for the key as another sink.
     * Creates the sink if none exists for the key.
     * Returns NULL if nested tables are not supported.
     * The default implementation always returns NULL.
     *
     * This sink (not the caller) owns the nested sink.
     *
     * @param key resource key string
     * @return nested-table sink, or NULL
     */
    virtual ResourceTableSink *getOrCreateTableSink(const char *key, UErrorCode &errorCode);

    /**
     * "Leaves" the table.
     * Indicates that all of the resources and sub-resources of the current table
     * have been enumerated.
     */
    virtual void leave(UErrorCode &errorCode);

private:
    ResourceTableSink(const ResourceTableSink &);  // no copy constructor
    ResourceTableSink &operator=(const ResourceTableSink &);  // no assignment operator
};

U_NAMESPACE_END

#endif
