/*
* Copyright (C) {1997-2003}, International Business Machines Corporation and others. All Rights Reserved.
*******************************************************************************
*
* File PARSEPOS.H
*
* Modification History:
*
*   Date        Name        Description
*   07/09/97    helena      Converted from java.
*   07/17/98    stephen     Added errorIndex support.
*   05/11/99    stephen     Cleaned up.
*******************************************************************************
*/

#ifndef PARSEPOS_H
#define PARSEPOS_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"

U_NAMESPACE_BEGIN

/**
 * <code>ParsePosition</code> is a simple class used by <code>Format</code>
 * and its subclasses to keep track of the current position during parsing.
 * The <code>parseObject</code> method in the various <code>Format</code>
 * classes requires a <code>ParsePosition</code> object as an argument.
 *
 * <p>
 * By design, as you parse through a string with different formats,
 * you can use the same <code>ParsePosition</code>, since the index parameter
 * records the current position.
 *
 * The ParsePosition class is not suitable for subclassing.
 *
 * @version     1.3 10/30/97
 * @author      Mark Davis, Helena Shih
 * @see         java.text.Format
 */

class U_COMMON_API ParsePosition : public UObject {
public:
    /**
     * Default constructor, the index starts with 0 as default.
     * @stable ICU 2.0
     */
    ParsePosition()
        : UObject()
      { this->index = 0; this->errorIndex = -1; }

    /**
     * Create a new ParsePosition with the given initial index.
     * @param newIndex the new text offset.
     * @stable ICU 2.0
     */
    ParsePosition(int32_t newIndex)
        : UObject()
      {    this->index = newIndex; this->errorIndex = -1; }

    /**
     * Copy constructor
     * @param copy the object to be copied from.
     * @stable ICU 2.0
     */
    ParsePosition(const ParsePosition& copy)
        : UObject(copy)
      {    this->index = copy.index; this->errorIndex = copy.errorIndex; }

    /**
     * Destructor
     * @stable ICU 2.0
     */
    ~ParsePosition() {}

    /**
     * Assignment operator
     * @stable ICU 2.0
     */
    ParsePosition&      operator=(const ParsePosition& copy);

    /**
     * Equality operator.
     * @return TRUE if the two parse positions are equal, FALSE otherwise.
     * @stable ICU 2.0
     */
    UBool              operator==(const ParsePosition& that) const;

    /**
     * Equality operator.
     * @return TRUE if the two parse positions are not equal, FALSE otherwise.
     * @stable ICU 2.0
     */
    UBool              operator!=(const ParsePosition& that) const;

    /**
     * Retrieve the current parse position.  On input to a parse method, this
     * is the index of the character at which parsing will begin; on output, it
     * is the index of the character following the last character parsed.
     * @return the current index.
     * @stable ICU 2.0
     */
    int32_t getIndex(void) const;

    /**
     * Set the current parse position.
     * @param index the new index.
     * @stable ICU 2.0
     */
    void setIndex(int32_t index);

    /**
     * Set the index at which a parse error occurred.  Formatters
     * should set this before returning an error code from their
     * parseObject method.  The default value is -1 if this is not
     * set.
     * @stable ICU 2.0
     */
    void setErrorIndex(int32_t ei);

    /**
     * Retrieve the index at which an error occurred, or -1 if the
     * error index has not been set.
     * @stable ICU 2.0
     */
    int32_t getErrorIndex(void) const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID();

private:
    /**
     * Input: the place you start parsing.
     * <br>Output: position where the parse stopped.
     * This is designed to be used serially,
     * with each call setting index up for the next one.
     */
    int32_t index;

    /**
     * The index at which a parse error occurred.
     */
    int32_t errorIndex;

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

inline UClassID
ParsePosition::getStaticClassID()
{ return (UClassID)&fgClassID; }

inline UClassID
ParsePosition::getDynamicClassID() const
{ return ParsePosition::getStaticClassID(); }

inline ParsePosition&
ParsePosition::operator=(const ParsePosition& copy)
{
  index = copy.index;
  errorIndex = copy.errorIndex;
  return *this;
}

inline UBool
ParsePosition::operator==(const ParsePosition& copy) const
{
  if(index != copy.index || errorIndex != copy.errorIndex)
  return FALSE;
  else
  return TRUE;
}

inline UBool
ParsePosition::operator!=(const ParsePosition& copy) const
{
  return !operator==(copy);
}

inline int32_t
ParsePosition::getIndex() const
{
  return index;
}

inline void
ParsePosition::setIndex(int32_t offset)
{
  this->index = offset;
}

inline int32_t
ParsePosition::getErrorIndex() const
{
  return errorIndex;
}

inline void
ParsePosition::setErrorIndex(int32_t ei)
{
  this->errorIndex = ei;
}
U_NAMESPACE_END

#endif
