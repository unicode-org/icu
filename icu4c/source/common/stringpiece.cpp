// Copyright (C) 2009, International Business Machines
// Corporation and others. All Rights Reserved.
//
// Copyright 2004 and onwards Google Inc.
//
// Author: wilsonh@google.com (Wilson Hsieh)
//

#include "unicode/utypes.h"
#include "unicode/stringpiece.h"
#include "cstring.h"

U_NAMESPACE_BEGIN

StringPiece::StringPiece(const char* str)
    : ptr_(str), length_((str == NULL) ? 0 : static_cast<int32_t>(uprv_strlen(str))) { }

StringPiece::StringPiece(const StringPiece& x, int32_t pos) {
  if (pos < 0) {
    pos = 0;
  } else if (pos > x.length_) {
    pos = x.length_;
  }
  ptr_ = x.ptr_ + pos;
  length_ = x.length_ - pos;
}

StringPiece::StringPiece(const StringPiece& x, int32_t pos, int32_t len) {
  if (pos < 0) {
    pos = 0;
  } else if (pos > x.length_) {
    pos = x.length_;
  }
  if (len < 0) {
    len = 0;
  } else if (len > x.length_ - pos) {
    len = x.length_ - pos;
  }
  ptr_ = x.ptr_ + pos;
  length_ = len;
}

const int32_t StringPiece::npos;

U_NAMESPACE_END
