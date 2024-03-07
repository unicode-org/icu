// Copyright 2008 the V8 project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials provided
//       with the distribution.
//     * Neither the name of Google Inc. nor the names of its
//       contributors may be used to endorse or promote products derived
//       from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#ifndef CCTEST_H_
#define CCTEST_H_

#include <stdio.h>
#include <string.h>
#include <inttypes.h>

#include "double-conversion/utils.h"

#ifndef TEST
#define TEST(Name)                                                       \
  static void Test##Name();                                              \
  CcTest register_test_##Name(Test##Name, __FILE__, #Name, NULL, true);  \
  static void Test##Name()
#endif

#ifndef DEPENDENT_TEST
#define DEPENDENT_TEST(Name, Dep)                                        \
  static void Test##Name();                                              \
  CcTest register_test_##Name(Test##Name, __FILE__, #Name, #Dep, true);  \
  static void Test##Name()
#endif

#ifndef DISABLED_TEST
#define DISABLED_TEST(Name)                                              \
  static void Test##Name();                                              \
  CcTest register_test_##Name(Test##Name, __FILE__, #Name, NULL, false); \
  static void Test##Name()
#endif

#define CHECK(condition) CheckHelper(__FILE__, __LINE__, #condition, condition)
#define CHECK_GE(a, b) CHECK((a) >= (b))

static inline void CheckHelper(const char* file,
                               int line,
                               const char* source,
                               bool condition) {
  if (!condition) {
    printf("%s:%d:\n CHECK(%s) failed\n", file, line, source);
    abort();
  }
}

#define CHECK_EQ(a, b) CheckEqualsHelper(__FILE__, __LINE__, #a, a, #b, b)

template<typename T> inline void PrintfValue(T x);
template<> inline void PrintfValue(int x) { printf("%d", x); }
template<> inline void PrintfValue(unsigned int x) { printf("%u", x); }
template<> inline void PrintfValue(short x) { printf("%hd", x); }
template<> inline void PrintfValue(unsigned short x) { printf("%hu", x); }
template<> inline void PrintfValue(int64_t x) { printf("%" PRId64, x); }
template<> inline void PrintfValue(uint64_t x) { printf("%" PRIu64, x); }
template<> inline void PrintfValue(float x) { printf("%.30e", static_cast<double>(x)); }
template<> inline void PrintfValue(double x) { printf("%.30e", x); }
template<> inline void PrintfValue(bool x) { printf("%s", x ? "true" : "false"); }

template<typename T1, typename T2>
inline void CheckEqualsHelper(const char* file, int line,
                       const char* expected_source,
                       T1 expected,
                       const char* value_source,
                       T2 value) {
  // If expected and value are NaNs then expected != value.
  if (expected != value && (expected == expected || value == value)) {
    printf("%s:%d:\n CHECK_EQ(%s, %s) failed\n",
           file, line, expected_source, value_source);
    printf("#  Expected: ");
    PrintfValue(expected);
    printf("\n");
    printf("#  Found:    ");
    PrintfValue(value);
    printf("\n");
    abort();
  }
}

template<>
inline void CheckEqualsHelper(const char* file, int line,
                       const char* expected_source,
                       const char* expected,
                       const char* value_source,
                       const char* value) {
  if ((expected == NULL && value != NULL) ||
      (expected != NULL && value == NULL)) {
    abort();
  }

  if ((expected != NULL && value != NULL && strcmp(expected, value) != 0)) {
    printf("%s:%d:\n CHECK_EQ(%s, %s) failed\n"
           "#  Expected: %s\n"
           "#  Found:    %s\n",
           file, line, expected_source, value_source, expected, value);
    abort();
  }
}

template<>
inline void CheckEqualsHelper(const char* file, int line,
                       const char* expected_source,
                       const char* expected,
                       const char* value_source,
                       char* value) {
  CheckEqualsHelper(file, line, expected_source, expected, value_source, static_cast<const char*>(value));
}

class CcTest {
 public:
  typedef void (TestFunction)();
  CcTest(TestFunction* callback, const char* file, const char* name,
         const char* dependency, bool enabled);
  void Run() { callback_(); }
  static int test_count();
  static CcTest* last() { return last_; }
  CcTest* prev() { return prev_; }
  const char* file() const { return file_; }
  const char* name() const { return name_; }
  const char* dependency() const { return dependency_; }
  bool enabled() const { return enabled_; }
 private:
  TestFunction* callback_;
  const char* file_;
  const char* name_;
  const char* dependency_;
  bool enabled_;
  static CcTest* last_;
  CcTest* prev_;
};

#endif  // ifndef CCTEST_H_
