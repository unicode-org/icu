/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1996-1999               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/
//===============================================================================
//
// File tables.h
//
// 
//
// Created by: Helena Shih
//
// Modification History:
//
//  Date        Name        Description
//  2/5/97      aliu        Added streamIn and streamOut methods to EntryPair,
//                          VectorOfInt, VectorOfPToExpandTable, VectorOfPToContractElement,
//                          VectorOfPToContractTable.  These are used by TableCollation
//                          streamIn and streamOut methods.
//  3/5/97      aliu        Made VectorOfPointersToPatternEntry::at() inline.
//  5/07/97     helena      Added isBogus().
//  6/18/97     helena      Added VectorOfPointer for queue-up extension list in
//                          MergeCollation.
//  8/18/97     helena      Added internal API documentation.  Note. All the VectorOfXXX
//                          interface is about the same.  The internal API docs will be
//                          added to only the first version and additional description
//                          will be added where necessary.
//  8/04/98        erm            Added fwd member to EntryPair.
//===============================================================================

#ifndef TABLES_H
#define TABLES_H


#include "filestrm.h"
#include "unistr.h"


/**
 * EntryPair is used for contracting characters.  Each entry pair contains the contracting 
 * character string and its collation order.
 */
class EntryPair
{
public:
    /**
     * Constructor
     */
    EntryPair();
    EntryPair(const UnicodeString &name, int32_t aValue, bool_t aFwd = TRUE);

    UnicodeString entryName;        // Contracting characters
    int32_t value;                  // Collation order
    bool_t fwd;                        // true if this pair is for the forward direction

    /**
     * The streamIn and streamOut methods read and write objects of this
     * class as binary, platform-dependent data in the iostream.  The stream
     * must be in ios::binary mode for this to work.  These methods are not
     * intended for general public use; they are used by the framework to improve
     * performance by storing certain objects in binary files.
     */
    void streamOut(FileStream* os) const;
    void streamIn(FileStream* is);
};


/**
 * VectorOfInt is a dynamic array of 32-bit integers.  
 * Ideally we would use templates for this, but they're not supported
 * on all of the platforms we need to support.
 */
class VectorOfInt {

    public:
        /**
         * The chunk size by which the array is grown.
         * This probably shouldn't be in the API
         */
        enum EGrowthRate { GROWTH_RATE = 4 };
        /**
         * Creates a vector that contains elements of integers.
         * @param initialSize the initial size of the vector object.
         */
                                VectorOfInt(int32_t initialSize = 0);
        /**
         * Copy constructor.
         */
                                VectorOfInt(const VectorOfInt&  that);
        /**
         * Destructor.
         */
                                ~VectorOfInt();

        /**
         * Assignment operator.
         */
        const VectorOfInt&      operator=(const VectorOfInt&    that);
        

        /**
         * Equality operators.
         */
        bool_t                  operator==(const VectorOfInt& that);
        bool_t                  operator!=(const VectorOfInt& that);

        /**
         * Gets a read-only reference to the element at the specified index.
         * This does not do range-checking; an invalid index may cause a crash.
         * @return the accessed element.
         */
        int32_t                 operator[](int32_t  index) const;
        int32_t                 at(int32_t  index) const;

        /**
         * Gets a non-const reference to the element at the specified index.
         * This does range-checking; access to elements beyond the end of the
         * array will cause the array to grow.
         */
        int32_t&                operator[](int32_t  index);
        int32_t&                at(int32_t  index);

        /**
         * Sets the element at the specified index to a different value.
         * @param index the specified index.
         * @param value the new value.
         */
        void                    atPut(  int32_t     index,
                                        const int32_t&  value);
        /**
         * Inserts a value at the specified index, sliding the rest of
         * the elements in the array over to make room.
         * @param index the specified index.
         * @param value the value.
         */
        void                    atInsert(   int32_t     index,
                                        const int32_t&  value);

        /**
         * Returns the number of elements in the vector.
         * @return the size of vector.
         */
        int32_t                 size(void) const;

        /**
         * Checks if this vector object is valid.
         * @return TRUE if the vector object is valid, FALSE otherwise.
         */
        bool_t                  isBogus(void) const;

        /**
         * The streamIn and streamOut methods read and write objects of this
         * class as binary, platform-dependent data in the iostream.  The stream
         * must be in ios::binary mode for this to work.  These methods are not
         * intended for general public use; they are used by the framework to improve
         * performance by storing certain objects in binary files.
         */
        void                    streamOut(FileStream* os) const;
        void                    streamIn(FileStream* is);

    private:
        /**
         * Resizes the vector if necessary when compared to a new size.
         * @param newSize the new size.
         */
        void                        resize(int32_t  newSize);
    
        int32_t                     fSize;
        int32_t                     fCapacity;
        int32_t*                    fElements;
        bool_t                      fBogus;
};

/**
 * VectorOfPointer is a dynamic array of void* pointers.  
 *  <P>
 *  This is a vector class that is designed to be used with pointer types and which implements
 *  owning semantics.  That is, once a value is placed an element of the vector, the vector is
 *  considered to own it and is responsible for disposing it.  This will happen both when the
 *  element is changed using atPut() or through an PointerTo****, and when the vector itself is
 *  disposed.  
 *  <P>
 *  WARNING:  The caller must be careful to avoid holding onto any dangling references
 *  after the vector is disposed, and the caller must also be careful not to put the same
 *  value into more than one element in the vector (unless the value is NULL).
 *  <P>
 *  As with VectorOf***>, the vector grows as necessary to accommodate all elements, the
 *  size is one plus the index of the highest element that's been set, and any elements below
 *  the highest element that aren't explicitly initialized are initialized to NULL.
 */
class VectorOfPointer {
    public:
        /**
         * The chunk size by which the array is grown.
         * This probably shouldn't be in the API
         */
        enum EGrowthRate { GROWTH_RATE = 4 };
        /**
         * Creates a vector that contains elements of pointers to objects.
         * @param initialSize the initial size of the vector object.
         */
                                    VectorOfPointer(int32_t initialSize = 0);
        /**
         * Copy constructor.
         */
                                    VectorOfPointer(const VectorOfPointer&  that);
        /**
         * Destructor.
         */
                                    ~VectorOfPointer();
        /**
         * Assignment operator.
         */
        const VectorOfPointer&      operator=(const VectorOfPointer&    that);

        /**
         * Equality operators.
         */
        bool_t                      operator==(const VectorOfPointer& that);
        bool_t                      operator!=(const VectorOfPointer& that);

        /**
         * Gets a read-only reference to the element at the specified index.
         * This does not do range-checking; an invalid index may cause a crash.
         * @return the accessed element.
         */
        void*                       operator[](int32_t  index) const;
        void*                       at(int32_t  index) const;

        /**
         * Gets a non-const reference to the element at the specified index.
         * This does range-checking; access to elements beyond the end of the
         * array will cause the array to grow.
         */
        void*&                      operator[](int32_t  index);
        void*&                      at(int32_t  index);

        /**
         * Sets the element at the specified index to a different value.
         * @param index the specified index.
         * @param value the new value.
         */
        void                        atPut(  int32_t         index,
                                            const void*&    value);

        /**
         * Inserts a value at the specified index, sliding the rest of
         * the elements in the array over to make room.
         * @param index the specified index.
         * @param value the value.
         */
        void                        atInsert(   int32_t     index,
                                                const void*&    value);
        /**
         * Returns the number of elements in the vector.
         * @return the size of vector.
         */
        int32_t                     size(void) const;

        /**
         * Checks if this vector object is valid.
         * @return TRUE if the vector object is valid, FALSE otherwise.
         */
        bool_t                      isBogus(void) const;

    private:
        /**
         * Resizes the vector if necessary when compared to a new size.
         * @param newSize the new size.
         */
        void                        resize(int32_t  newSize);
    
        int32_t                     fSize;
        int32_t                     fCapacity;
        void**                      fElements;
        bool_t                      fBogus;
};

//=================================================================================================
//  The following diagram shows the data structure of the RuleBasedCollator object.
//  Suppose we have the rule, where 'o-umlaut' is the unicode char 0x00F6.
//  "a, A < b, B < c, C, ch, cH, Ch, CH < d, D ... < o, O; 'o-umlaut'/E, 'O-umlaut'/E ...".
//  What the rule says is, sorts 'ch'ligatures and 'c' only with tertiary difference and
//  sorts 'o-umlaut' as if it's always expanded with 'e'.
//
//                                     (VectorOfPToContractTable)         (VectorOfPToExpandTable)
// mapping table                           contracting list                  expanding list
// (contains all unicode char
//  entries)
//                   (VectorOfPToContractElement) *(PToContractElement)      (PToExpandTable)
//                                      ___        _____________         _________________________
//   ________                   |=====>|_*_|----->|'c'  |v('c') |   |==>|v('o')|v('umlaut')|v('e')|
//  |_\u0001_|--> v('\u0001')   |      |_:_|      |-------------|   |   |-------------------------|
//  |_\u0002_|--> v('\u0002')   |      |_:_|      |'ch' |v('ch')|   |   |             :           |
//  |____:___|                  |      |_:_|      |-------------|   |   |-------------------------|
//  |____:___|                  |                 |'cH' |v('cH')|   |   |             :           |
//  |__'a'___|--> v('a')        |                 |-------------|   |   |-------------------------|
//  |__'b'___|--> v('b')        |                 |'Ch' |v('Ch')|   |   |             :           |
//  |____:___|                  |                 |-------------|   |   |-------------------------|
//  |____:___|                  |                 |'CH' |v('CH')|   |   |             :           |
//  |__'ch'__|-------------------                  -------------    |   |-------------------------|
//  |____:___|                                                      |   |             :           |
//  |o-umlaut|------------------------------------------------------    |_________________________|
//  |____:___|
//
//
// Noted by Helena Shih on 6/23/97 with pending design changes (slimming collation).
//=================================================================================================

/** 
 * PToExpandTable is a smart-pointer to a VectorOfInt that is used to store
 * the collation orders that are the result of an expansion.
 * <P>
 * You can use this object as if it were a pointer to a VectorOfInt, e.g.
 * <pre>
 * PToExpandTable foo = ....;
 * foo->atInsert(....);
 * </pre>
 */
class PToExpandTable {
    public:
        
        /**
         * Destructor.
         */
                                            ~PToExpandTable();
        
        /**
         * Assignment operators
         * The expand table that this object already points to (if any) is deleted.
         */
        const PToExpandTable&               operator=(VectorOfInt*  newValue);
        const PToExpandTable&               operator=(const PToExpandTable& pointerToNewValue);

        /**
         * Pointer operator override
         */
                                            operator VectorOfInt*() const;

    private:
        /**
         * Constructor
         */
                                            PToExpandTable(VectorOfInt*&    value);
        /**
         * Copy constructor.
         */
                                            PToExpandTable(const PToExpandTable&    that);

        VectorOfInt*&                       fValue;

        friend class VectorOfPToExpandTable;
};

/**
 *  VectorOfPointer is a dynamic array of PToExpandTable objects
 *  which in turn point to the array of collation orders for each expanding character.
 *  <P>
 *  This is a vector class that is designed to be used with pointer types and which implements
 *  owning semantics.  That is, once a value is placed an element of the vector, the vector is
 *  considered to own it and is responsible for disposing it.  This will happen both when the
 *  element is changed using atPut() or through an PointerTo****, and when the vector itself is
 *  disposed.  
 *  <P>
 *  WARNING:  The caller must be careful to avoid holding onto any dangling references
 *  after the vector is disposed, and the caller must also be careful not to put the same
 *  value into more than one element in the vector (unless the value is NULL).
 *  <P>
 *  As with VectorOf***>, the vector grows as necessary to accommodate all elements, the
 *  size is one plus the index of the highest element that's been set, and any elements below
 *  the highest element that aren't explicitly initialized are initialized to NULL.
 */
class VectorOfPToExpandTable {
    public:

        /**
         * The chunk size by which the array is grown.
         * This probably shouldn't be in the API
         */
        enum EGrowthRate { GROWTH_RATE = 4 };
        /**
         * Creates a vector that contains elements of PToExpandTable.
         * @param initialSize the initial size of the vector object.
         */
                            VectorOfPToExpandTable(int32_t  initialSize = 0);
        /**
         * Copy constructor.
         */
                            VectorOfPToExpandTable(const VectorOfPToExpandTable&    that);

        /**
         * Destructor.
         */
                            ~VectorOfPToExpandTable();

        /**
         * Assignment operator.
         */
        const VectorOfPToExpandTable&   
                            operator=(const VectorOfPToExpandTable& that);

        /**
         * Return a modifiable smart-pointer to the expansion table
         * at the given index.  Assigning to this smart pointer will work, e.g.
         *  VectorOfPToExpandTable foo = ....;
         *  foo[5] = new VectorOfInt ...;
         * This does range-checking; access to elements beyond the end of the
         * array will cause the array to grow.
         */
        PToExpandTable      at(int32_t  index);
        PToExpandTable      operator[](int32_t  index);

        /**
         * Return a pointer to the table at the given index.
         * The pointer itself cannot be modified, but the elements it points to may:
         * <pre>
         *  const VectorOfPToExpandTable foo = ....;
         *  foo[5] = ....;      // NOT ALLOWED
         *  foo[5][0] = 12345;  // ok
         * </pre>
         * This does not do range-checking; an invalid index may cause a crash.
         * @return the accessed element.
         */
        VectorOfInt*        at(int32_t  index) const;
        VectorOfInt*        operator[](int32_t  index) const;

        /**
         * Sets the element at the specified index to a different value.
         * If there was aready an object stored at this index, it is deleted.
         * @param index the specified index.
         * @param value the new value.
         */
        void                atPut(  int32_t         index,
                                    VectorOfInt*    value);

        /**
         * "Orphan" the pointer at the specified index.  The array will no
         * longer contain a reference to the object, and the caller is
         * now responsible for deleting its storage.
         */
        VectorOfInt*        orphanAt(int32_t    index);

        /**
         * Returns the number of elements in the vector.
         * @return the size of vector.
         */
        int32_t             size(void) const;

        /**
         * Checks if this vector object is valid.
         * @return TRUE if the vector object is valid, FALSE otherwise.
         */
        bool_t              isBogus(void) const;

    /**
     * The streamIn and streamOut methods read and write objects of this
     * class as binary, platform-dependent data in the iostream.  The stream
     * must be in ios::binary mode for this to work.  These methods are not
     * intended for general public use; they are used by the framework to improve
     * performance by storing certain objects in binary files.
     */
        void                streamOut(FileStream* os) const;
        void                streamIn(FileStream* is);

    private:
        /**
         * Resizes the vector if necessary when compared to a new size.
         * @param newSize the new size.
         */
        void                resize(int32_t      newSize);

        int32_t             fSize;
        int32_t             fCapacity;
        VectorOfInt**       fElements;
        bool_t              fBogus;
};

/** 
 * PToContractElement is a smart-pointer to an array that is used to store
 * the contracting-character strings that are associated with a given Unicode character.
 * <P>
 * You can use this object as if it were a pointer to an EntryPair array, e.g.
 * <pre>
 * PToContractElement foo = ....;
 * foo->entryName = ....;
 * </pre>
 */
class PToContractElement {
    public:
        /**
         * Destructor.
         */
                                            ~PToContractElement();
        
        /**
         * Assignment operators
         * The EntryPair that this object already points to (if any) is deleted.
         */
        const PToContractElement&               operator=(EntryPair*    newValue);
        const PToContractElement&               operator=(const PToContractElement& pointerToNewValue);

        /**
         * Pointer operator override
         */
                                            operator EntryPair*() const;

    private:
        /**
         * Constructor
         */
                                            PToContractElement(EntryPair*&  value);
        /**
         * Copy constructor.
         */
                                            PToContractElement(const PToContractElement&    that);

        EntryPair*&                     fValue;

        friend class VectorOfPToContractElement;
};

/**
 *  The table that contains the list of contracting character entries.
 *  <P>
 *  This is a vector class that is designed to be used with pointer types and which implements
 *  owning semantics.  That is, once a value is placed an element of the vector, the vector is
 *  considered to own it and is responsible for disposing it.  This will happen both when the
 *  element is changed using atPut() or through an PointerTo****, and when the vector itself is
 *  disposed.  
 *  <P>
 *  WARNING:  The caller must be careful to avoid holding onto any dangling references
 *  after the vector is disposed, and the caller must also be careful not to put the same
 *  value into more than one element in the vector (unless the value is NULL).
 *  <P>
 *  As with VectorOf***>, the vector grows as necessary to accommodate all elements, the
 *  size is one plus the index of the highest element that's been set, and any elements below
 *  the highest element that aren't explicitly initialized are initialized to NULL.
 */
class VectorOfPToContractElement {
    public:
        /**
         * The chunk size by which the array is grown.
         * This probably shouldn't be in the API
         */
        enum EGrowthRate { GROWTH_RATE = 4 };
        /**
         * Creates a vector that contains elements of PToContractElement.
         * @param initialSize the initial size of the vector object.
         */
                            VectorOfPToContractElement(int32_t  initialSize = 0);
        /**
         * Copy constructor.
         */
                            VectorOfPToContractElement(const VectorOfPToContractElement&    that);

        /**
         * Destructor.
         */
                            ~VectorOfPToContractElement();

        /**
         * Assignment operator.
         */
        const VectorOfPToContractElement&   
                            operator=(const VectorOfPToContractElement& that);

        /**
         * Return a modifiable smart-pointer to the EntryPair
         * at the given index.  Assigning to this smart pointer will work, e.g.
         * <pre>
         *  PToContractElement foo = ....;
         *  foo[5] = ...;
         * </pre>
         * This does range-checking; access to elements beyond the end of the
         * array will cause the array to grow.
         */
        PToContractElement  operator[](int32_t  index);
        PToContractElement  at(int32_t  index);

        /**
         * Return a pointer to the EntryPair at the given index.
         * The pointer itself cannot be modified, but the elements it points to may:
         * <pre>
         *  const VectorOfPToExpandTable foo = ....;
         *  foo[5] = ....;              // NOT ALLOWED
         *  foo[5]->entryName = ....;   // ok
         * </pre>
         * This does not do range-checking; an invalid index may cause a crash.
         * @return the accessed element.
         */
        EntryPair*          operator[](int32_t  index) const;
        EntryPair*          at(int32_t  index) const;

        /**
         * Sets the element at the specified index to a different value.
         * If there was aready an object stored at this index, it is deleted.
         * @param index the specified index.
         * @param value the new value.
         */
        void                atPut(  int32_t         index,
                                    EntryPair*      value);
        /**
         * Inserts a value at the specified index, sliding the rest of
         * the elements in the array over to make room.
         * @param index the specified index.
         * @param value the value.
         */
        void                atInsert(   int32_t     index,
                                        EntryPair*  value);
        /**
         * "Orphan" the pointer at the specified index.  The array will no
         * longer contain a reference to the object, and the caller is
         * now responsible for deleting its storage.
         */
        EntryPair*          orphanAt(int32_t    index);

        /**
         * Returns the number of elements in the vector.
         * @return the size of vector.
         */
        int32_t             size(void) const;

        /**
         * Checks if this vector object is valid.
         * @return TRUE if the vector object is valid, FALSE otherwise.
         */
        bool_t              isBogus(void) const;

        /**
         * The streamIn and streamOut methods read and write objects of this
         * class as binary, platform-dependent data in the iostream.  The stream
         * must be in ios::binary mode for this to work.  These methods are not
         * intended for general public use; they are used by the framework to improve
         * performance by storing certain objects in binary files.
         */
        void                streamOut(FileStream* os) const;
        void                streamIn(FileStream* is);

    private:
        /**
         * Resizes the vector if necessary when compared to a new size.
         * @param newSize the new size.
         */
        void                resize(int32_t      newSize);

        int32_t             fSize;
        int32_t             fCapacity;
        EntryPair**         fElements;
        bool_t              fBogus;
};

/**
 * Pointer to each contracing element list.
 */
class PToContractTable {
    public:
        /**
         * Destructor.
         */
                                            ~PToContractTable();
        
        /**
         * Assignment operators.
         * <P>
         * The contracting element list (if any) that this object already points to
         * is deleted.
         */
        const PToContractTable&             operator=(VectorOfPToContractElement*   newValue);
        const PToContractTable&             operator=(const PToContractTable&   pointerToNewValue);

        /**
         * Pointer operator override
         */
                                            operator VectorOfPToContractElement*() const;

    private:
        /**
         * Constructor
         */
                                            PToContractTable(VectorOfPToContractElement*&   value);
        /**
         * Copy constructor.
         */
                                            PToContractTable(const PToContractTable&    that);

        VectorOfPToContractElement*&                        fValue;

        friend class VectorOfPToContractTable;
};

/**
 * The vector that contains all contracting list tables.
 *  <P>
 *  This is a vector class that is designed to be used with pointer types and which implements
 *  owning semantics.  That is, once a value is placed an element of the vector, the vector is
 *  considered to own it and is responsible for disposing it.  This will happen both when the
 *  element is changed using atPut() or through an PointerTo****, and when the vector itself is
 *  disposed.  
 *  <P>
 *  WARNING:  The caller must be careful to avoid holding onto any dangling references
 *  after the vector is disposed, and the caller must also be careful not to put the same
 *  value into more than one element in the vector (unless the value is NULL).
 *  <P>
 *  As with VectorOf***>, the vector grows as necessary to accommodate all elements, the
 *  size is one plus the index of the highest element that's been set, and any elements below
 *  the highest element that aren't explicitly initialized are initialized to NULL.
 */
class VectorOfPToContractTable {
    public:
        /**
         * The chunk size by which the array is grown.
         * This probably shouldn't be in the API
         */
        enum EGrowthRate { GROWTH_RATE = 4 };
        /**
         * Creates a vector that contains elements of PToContractTable.
         * @param initialSize the initial size of the vector object.
         */
                            VectorOfPToContractTable(int32_t    initialSize = 0);
        /**
         * Copy constructor.
         */
                            VectorOfPToContractTable(const VectorOfPToContractTable&    that);

                            ~VectorOfPToContractTable();

        /**
         * Assignment operator.
         */
        const VectorOfPToContractTable& 
                            operator=(const VectorOfPToContractTable&   that);

        /**
         * Return a modifiable smart-pointer to the contraction table
         * at the given index.  Assigning to this smart pointer will work, e.g.
         * <pre>
         *  VectorOfPToContractTable foo = ....;
         *  foo[5] = ...;
         * </pre>
         * This does range-checking; access to elements beyond the end of the
         * array will cause the array to grow.
         */
        PToContractTable        operator[](int32_t  index);
        PToContractTable        at(int32_t  index);

        /**
         * Return a pointer to the contraction table at the given index.
         * The pointer itself cannot be modified, but the elements it points to may:
         * <pre>
         *  const VectorOfPToExpandTable foo = ....;
         *  foo[5] = ....;              // NOT ALLOWED
         *  foo[5][0] = ....;           // ok
         * </pre>
         * This does not do range-checking; an invalid index may cause a crash.
         * @return the accessed element.
         */
        VectorOfPToContractElement*     operator[](int32_t  index) const;
        VectorOfPToContractElement*     at(int32_t  index) const;

        /**
         * Sets the element at the specified index to a different value.
         * If there was aready an object stored at this index, it is deleted.
         * @param index the specified index.
         * @param value the new value.
         */
        void                atPut(  int32_t         index,
                                    VectorOfPToContractElement* value);
        /**
         * "Orphan" the pointer at the specified index.  The array will no
         * longer contain a reference to the object, and the caller is
         * now responsible for deleting its storage.
         */
        VectorOfPToContractElement*     orphanAt(int32_t    index);

        /**
         * Returns the number of elements in the vector.
         * @return the size of vector.
         */
        int32_t             size(void) const;

        /**
         * Checks if this vector object is valid.
         * @return TRUE if the vector object is valid, FALSE otherwise.
         */
        bool_t              isBogus(void) const;
        /**
         * The streamIn and streamOut methods read and write objects of this
         * class as binary, platform-dependent data in the iostream.  The stream
         * must be in ios::binary mode for this to work.  These methods are not
         * intended for general public use; they are used by the framework to improve
         * performance by storing certain objects in binary files.
         */
        void                streamOut(FileStream* os) const;
        void                streamIn(FileStream* is);

    private:
        /**
         * Resizes the vector if necessary when compared to a new size.
         * @param newSize the new size.
         */
        void                resize(int32_t      newSize);

        int32_t             fSize;
        int32_t             fCapacity;
        VectorOfPToContractElement**        fElements;
        bool_t              fBogus;
};

class PatternEntry;

/**
 *  Proxy class for accessing elements of a VectorOfPointersToPatternEntry
 *  <P>
 *  This class is a simple proxy class that implements the owning semantics for the
 *  operator[] and at() functions on VectorOfPointersToPatternEntry.  It enables
 *  expressions like "v[3] = someNewValue".  One never creates a PointerToPatternEntry
 *  directly, and one never declares variables of this type.  It just exists to
 *  implement the API of VectorOfPointersToPatternEntry.
 */

class PointerToPatternEntry {
    public:
        /**
         * Destructor.
         */
                                            ~PointerToPatternEntry();
        
        /**
         * Assignment operators
         * The PatternEntry that this object already points to (if any) is deleted.
         */
        const PointerToPatternEntry&        operator=(PatternEntry* newValue);
        const PointerToPatternEntry&        operator=(const PointerToPatternEntry&  pointerToNewValue);

        /**
         * Pointer operator override
         */
                                            operator PatternEntry*() const;

    private:
        /**
         * Constructor
         */
                                            PointerToPatternEntry(PatternEntry*&    value);
        /**
         * Copy constructor.
         */
                                            PointerToPatternEntry(const PointerToPatternEntry&  that);

        PatternEntry*&                      fValue;

        friend class VectorOfPointersToPatternEntry;
};

/**
 *  Simple owning-vector class
 *  This is a vector class that is designed to be used with pointer types and which implements
 *  owning semantics.  That is, once a value is placed an element of the vector, the vector is
 *  considered to own it and is responsible for disposing it.  This will happen both when the
 *  element is changed using atPut() or through an PointerTo****, and when the vector itself is
 *  disposed.  
 *  <P>
 *  WARNING:  The caller must be careful to avoid holding onto any dangling references
 *  after the vector is disposed, and the caller must also be careful not to put the same
 *  value into more than one element in the vector (unless the value is NULL).
 *  <P>
 *  As with VectorOf***>, the vector grows as necessary to accommodate all elements, the
 *  size is one plus the index of the highest element that's been set, and any elements below
 *  the highest element that aren't explicitly initialized are initialized to NULL.
 */

class VectorOfPointersToPatternEntry {
    public:
        /**
         * The chunk size by which the array is grown.
         * This probably shouldn't be in the API
         */
        enum EGrowthRate { GROWTH_RATE = 4 };
        /**
         * Creates a vector that contains elements of PointerToPatternEntry.
         * @param initialSize the initial size of the vector object.
         */
                                            VectorOfPointersToPatternEntry(int32_t  initialSize = 0);
        /**
         * Copy constructor.
         */
                                            VectorOfPointersToPatternEntry(const VectorOfPointersToPatternEntry& that);

        /**
         * Destructor.
         */
                                            ~VectorOfPointersToPatternEntry();

        /**
         * Assignment operator.
         */
        const VectorOfPointersToPatternEntry& operator=(const VectorOfPointersToPatternEntry& that);

        /**
         * Return a modifiable smart-pointer to the contraction table
         * at the given index.  Assigning to this smart pointer will work, e.g.
         * <pre>
         *  VectorOfPointersToPatternEntry foo = ....;
         *  foo[5] = ...;
         * </pre>
         * This does range-checking; access to elements beyond the end of the
         * array will cause the array to grow.
         */
        PointerToPatternEntry               operator[](int32_t  index);
        inline PointerToPatternEntry        at(int32_t  index) { return (*this)[index]; }

        /**
         * Return a pointer to the EntryPair at the given index.
         * The pointer itself cannot be modified, but the elements it points to may:
         * <pre>
         *  const VectorOfPointersToPatternEntryfoo = ....;
         *  foo[5] = ....;              // NOT ALLOWED
         *  foo[5]->getStrength();      // ok
         * </pre>
         * This does not do range-checking; an invalid index may cause a crash.
         * @return the accessed element.
         */
        PatternEntry*                       operator[](int32_t  index) const;
        inline PatternEntry*                at(int32_t  index) const { return (*this)[index]; }

        /**
         * Sets the element at the specified index to a different value.
         * If there was aready an object stored at this index, it is deleted.
         * @param index the specified index.
         * @param value the new value.
         */
        void                                atPut(  int32_t     index,
                                                    PatternEntry*   value);
        /**
         * Inserts a value at the specified index, sliding the rest of
         * the elements in the array over to make room.
         * @param index the specified index.
         * @param value the value.
         */
        void                                atInsert(   int32_t     index,
                                                        PatternEntry*   value);
        /**
         * "Orphan" the pointer at the specified index.  The array will no
         * longer contain a reference to the object, and the caller is
         * now responsible for deleting its storage.
         */
        PatternEntry*                       orphanAt(int32_t    index);

        /**
         * Remove all elements from the vector.
         */
        void                                clear(void);

        /**
         * Returns the number of elements in the vector.
         * @return the size of vector.
         */
        int32_t                             size(void) const;

        /**
         * If the specified value exists in the vector, return its index.
         * If not, return -1.
         */
        int32_t                             indexOf(const PatternEntry* value) const;

        /**
         * Return the index of the last occurance of value in the vector,
         * or -1 if the vector doesn't contain value.
         */
        int32_t                             lastIndexOf(const PatternEntry* value) const;

        /**
         * Checks if this vector object is valid.
         * @return TRUE if the vector object is valid, FALSE otherwise.
         */
        bool_t                              isBogus(void) const;
    private:
        /**
         * Resizes the vector if necessary when compared to a new size.
         * @param newSize the new size.
         */
        void                                resize(int32_t      newSize);

        int32_t                             fSize;
        int32_t                             fCapacity;
        PatternEntry**                      fElements;
        bool_t                              fBogus;
};

inline
EntryPair::EntryPair()
  : entryName(), value(0xffffffff), fwd(TRUE)
{
}

inline
EntryPair::EntryPair(const UnicodeString &name, int32_t aValue, bool_t aFwd)
  : entryName(name), value(aValue), fwd(aFwd)
{
}

//=======================================================================================
// METHODS ON VectorOfInt
//=======================================================================================

inline int32_t
VectorOfInt::operator[](int32_t index) const
{
    return (index < fCapacity) ? fElements[index] : 0;
}


inline int32_t
VectorOfInt::at(int32_t index) const
{
    return (*this)[index];
}

inline int32_t&
VectorOfInt::at(int32_t index)
{
    return (*this)[index];
}

inline int32_t
VectorOfInt::size() const
{
    return fSize;
}

//=======================================================================================
// METHODS ON VectorOfPointer
//=======================================================================================

inline void*
VectorOfPointer::operator[](int32_t index) const
{
    return (index < fCapacity) ? fElements[index] : 0;
}


inline void*
VectorOfPointer::at(int32_t index) const
{
    return (*this)[index];
}

inline void*&
VectorOfPointer::at(int32_t index)
{
    return (*this)[index];
}

inline int32_t
VectorOfPointer::size() const
{
    return fSize;
}
//=======================================================================================
// METHODS ON PToExpandTable
//=======================================================================================

inline
PToExpandTable::operator VectorOfInt*() const
{
    return fValue;
}

inline
PToExpandTable::PToExpandTable(VectorOfInt*&    value)
: fValue(value)
{
}

inline
PToExpandTable::PToExpandTable(const PToExpandTable&    that)
: fValue(that.fValue)
{
}

inline
PToExpandTable::~PToExpandTable()
{
}

inline const PToExpandTable&
PToExpandTable::operator=(VectorOfInt*  newValue)
{
    delete fValue;
    fValue = newValue;
    return *this;
}

inline const PToExpandTable&
PToExpandTable::operator=(const PToExpandTable& pointerToNewValue)
{
    delete fValue;
    fValue = (VectorOfInt*)(pointerToNewValue);
    return *this;
}

//=======================================================================================
// METHODS ON VectorOfPToExpandTable
//=======================================================================================
inline VectorOfInt*
VectorOfPToExpandTable::operator[](int32_t  index) const
{
    return (index < fCapacity) ? fElements[index] : 0;
}

inline VectorOfInt*
VectorOfPToExpandTable::at(int32_t  index) const
{
    return (*this)[index];
}

inline PToExpandTable
VectorOfPToExpandTable::at(int32_t  index)
{
    return (*this)[index];
}

inline int32_t
VectorOfPToExpandTable::size() const
{
    return fSize;
}

//=======================================================================================
// METHODS ON PToContractElement
//=======================================================================================

inline
PToContractElement::operator EntryPair*() const
{
    return fValue;
}

inline
PToContractElement::PToContractElement(EntryPair*&  value)
: fValue(value)
{
}

inline
PToContractElement::PToContractElement(const PToContractElement&    that)
: fValue(that.fValue)
{
}

inline
PToContractElement::~PToContractElement()
{
}

inline const PToContractElement&
PToContractElement::operator=(EntryPair*    newValue)
{
    delete fValue;
    fValue = newValue;
    return *this;
}

inline const PToContractElement&
PToContractElement::operator=(const PToContractElement& pointerToNewValue)
{
    delete fValue;
    fValue = (EntryPair*)(pointerToNewValue);
    return *this;
}

//=======================================================================================
// METHODS ON VectorOfPToContractElement
//=======================================================================================

inline EntryPair*
VectorOfPToContractElement::operator[](int32_t  index) const
{
    return (index < fCapacity) ? fElements[index] : 0;
}

inline EntryPair*
VectorOfPToContractElement::at(int32_t  index) const
{
    return (*this)[index];
}

inline PToContractElement
VectorOfPToContractElement::at(int32_t  index)
{
    return (*this)[index];
}

inline int32_t
VectorOfPToContractElement::size() const
{
    return fSize;
}

//=======================================================================================
// METHODS ON PToContractTable
//=======================================================================================

inline
PToContractTable::operator VectorOfPToContractElement*() const
{
    return fValue;
}

inline
PToContractTable::PToContractTable(VectorOfPToContractElement*& value)
: fValue(value)
{
}

inline
PToContractTable::PToContractTable(const PToContractTable&  that)
: fValue(that.fValue)
{
}

inline
PToContractTable::~PToContractTable()
{
}

inline const PToContractTable&
PToContractTable::operator=(VectorOfPToContractElement* newValue)
{
    delete fValue;
    fValue = newValue;
    return *this;
}

inline const PToContractTable&
PToContractTable::operator=(const PToContractTable& pointerToNewValue)
{
    delete fValue;
    fValue = (VectorOfPToContractElement*)(pointerToNewValue);
    return *this;
}

//=======================================================================================
// METHODS ON VectorOfPToContractTable
//=======================================================================================

inline VectorOfPToContractElement*
VectorOfPToContractTable::operator[](int32_t    index) const
{
    return (index < fCapacity) ? fElements[index] : 0;
}

inline VectorOfPToContractElement*
VectorOfPToContractTable::at(int32_t    index) const
{
    return (*this)[index];
}

inline PToContractTable
VectorOfPToContractTable::at(int32_t    index)
{
    return (*this)[index];
}

inline int32_t
VectorOfPToContractTable::size() const
{
    return fSize;
}

#endif
