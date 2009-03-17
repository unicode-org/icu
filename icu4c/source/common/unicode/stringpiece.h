// Copyright (C) 2009, International Business Machines
// Corporation and others. All Rights Reserved.
//
// Copyright 2001 and onwards Google Inc.
// Author: Sanjay Ghemawat
//


#ifndef __STRINGPIECE_H__
#define __STRINGPIECE_H__

/**
 * \file 
 * \brief C++ API: StringPiece: Read-only byte string wrapper class.
 */

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/std_string.h"

// Arghh!  I wish C++ literals were "string".

U_NAMESPACE_BEGIN

///
/// A string-like object that points to a sized piece of memory.
///
/// Functions or methods may use const StringPiece& parameters to accept either
/// a "const char*" or a "string" value that will be implicitly converted to
/// a StringPiece.
///
/// Systematic usage of StringPiece is encouraged as it will reduce unnecessary
/// conversions from "const char*" to "string" and back again.
///
///
///
/// @draft ICU 4.2
///
class U_COMMON_API StringPiece : public UMemory {
 private:
  const char*   ptr_;
  int32_t       length_;

 public:
  /// We provide non-explicit singleton constructors so users can pass
  /// in a "const char*" or a "string" wherever a "StringPiece" is
  /// expected.
  /// @draft ICU 4.2
  StringPiece() : ptr_(NULL), length_(0) { }
  /**
   * @draft ICU 4.2
   */
  StringPiece(const char* str);
#if U_HAVE_STD_STRING
  /**
   * @draft ICU 4.2
   */
  StringPiece(const U_STD_NSQ string& str)
    : ptr_(str.data()), length_(static_cast<int32_t>(str.size())) { }
#endif
  /**
   * @draft ICU 4.2
   */
  StringPiece(const char* offset, int32_t len) : ptr_(offset), length_(len) { }
  /// Substring of another StringPiece.
  /// pos must be non-negative and <= x.length().
  /// @draft ICU 4.2
  StringPiece(const StringPiece& x, int32_t pos);
  /// Substring of another StringPiece.
  /// pos must be non-negative and <= x.length().
  /// len must be non-negative and will be pinned to at most x.length() - pos.
  /// @draft ICU 4.2
  StringPiece(const StringPiece& x, int32_t pos, int32_t len);

  /// data() may return a pointer to a buffer with embedded NULs, and the
  /// returned buffer may or may not be null terminated.  Therefore it is
  /// typically a mistake to pass data() to a routine that expects a NUL
  /// terminated string.
  /// @draft ICU 4.2
  const char* data() const { return ptr_; }
  /** @draft ICU 4.2 */
  int32_t size() const { return length_; }
  /** @draft ICU 4.2 */
  int32_t length() const { return length_; }
  /** @draft ICU 4.2 */
  UBool empty() const { return length_ == 0; }

  /** @draft ICU 4.2 */
  void clear() { ptr_ = NULL; length_ = 0; }

  /** @draft ICU 4.2 */
  void remove_prefix(int32_t n) {
    if (n >= 0) {
      if (n > length_) {
        n = length_;
      }
      ptr_ += n;
      length_ -= n;
    }
  }

  /** @draft ICU 4.2 */
  void remove_suffix(int32_t n) {
    if (n >= 0) {
      if (n <= length_) {
        length_ -= n;
      } else {
        length_ = 0;
      }
    }
  }

  /** @draft ICU 4.2 */
  static const int32_t npos = 0x7fffffff;

  /** @draft ICU 4.2 */
  StringPiece substr(int32_t pos, int32_t n = npos) const {
    return StringPiece(*this, pos, n);
  }
};

U_NAMESPACE_END

#endif  // __STRINGPIECE_H__
