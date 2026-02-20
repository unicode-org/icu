---
layout: default
title: C++ Code Point Iterators
nav_order: 24
parent: ICU4C
---
<!--
© 2025 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Plug-ins
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Sometimes you need to process a string one character at a time.
This is trivial in a UTF-32 string, but those are not common.
Most Unicode strings are UTF-8 or UTF-16 strings and may use multiple code units per Unicode code point.

(Note that a Unicode code point is not necessarily what you think of as a character.
See the Wikipedia article on
[combining characters](https://en.wikipedia.org/wiki/Combining_character)
for some examples.)

Starting with ICU 78, ICU4C has [C++ header-only APIs](../icu4c/header-only.md)
for conveniently iterating over the code points of a Unicode string in any standard encoding form (UTF-8/16/32).
They work seamlessly with modern C++ iterators and ranges.
These APIs are fully inline-implemented and can be used without linking with the ICU libraries.

As with the existing C macros, there are versions which validate the code unit sequences on the fly,
as well as fast but “unsafe” versions which assume & require well-formed strings.

Header file documentation:
[unicode/utfiterator.h](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/utfiterator_8h.html)
including some
[sample code snippets](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/utfiterator_8h.html#details).

## Old: C macros

ICU continues to provide C macros for iterating through
[UTF-8](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/utf8_8h.html)
and
[UTF-16](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/utf16_8h.html)
strings. For example:

```c++
int32_t rangeLoop16(std::u16string_view s) {
    // We are just adding up the code points for minimal-code demonstration purposes.
    const char16_t *p = s.data();
    size_t length = s.length();
    int32_t sum = 0;
    for (size_t i = 0; i < length;) {  // loop body increments i
        UChar32 c;
        U16_NEXT(p, i, length, c);
        sum += c;  // < 0 if ill-formed
    }
    return sum;
}
```

## C++ code point iterators and ranges

The `unicode/utfiterator.h` APIs let you wrap the string in a “range” object that provides iterators over the string’s code points. You could rewrite the C macro example above like this:

```c++
int32_t rangeLoop16(std::u16string_view s) {
    // We are just adding up the code points for minimal-code demonstration purposes.
    int32_t sum = 0;
    for (auto units : utfStringCodePoints<UChar32, UTF_BEHAVIOR_NEGATIVE>(s)) {
        sum += units.codePoint();  // < 0 if ill-formed
    }
    return sum;
}
```

This has a number of benefits compared with the C macros:
- These C++ APIs provide iterator and range adaptors that are
  compatible with the C++ standard library, and thus look and feel natural.
  They are composable with standard library utilities, especially in C++20 and later.
- Instead of raw pointer+length manipulation,
  they work with a large variety of code unit iterators.
  - This makes it possible to use constrained inputs without having to use an intermediate buffer of code units.
  - It also allows for safe code execution if the input supplies code unit iterators with bounds checking.
- The same types and functions work for any of UTF-8/16/32.\
  (There are different macros for UTF-8 vs. UTF-16, and none for UTF-32.)
- The APIs offer a number of options for a good fit for many use cases.

Here is an example for composing a `utfStringCodePoints()` range adaptor
with C++20 language and standard library features:
```c++
auto codePoint = [](const auto &codeUnits) { return codeUnits.codePoint(); };
const std::u16string text = u"𒂍𒁾𒁀𒀀𒂠 𒉌𒁺𒉈𒂗\n"
                                u"𒂍𒁾𒁀𒀀 𒀀𒈾𒀀𒀭 𒉌𒀝\n"
                                u"𒁾𒈬 𒉌𒋃 𒃻𒅗𒁺𒈬 𒉌𒅥\n";
auto lines2sqq = text | std::ranges::views::lazy_split(u'\n') | std::views::drop(1);
auto codeUnits = *lines2sqq.begin();
assertTrue(std::ranges::equal(
        utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits) |
                std::ranges::views::transform(codePoint),
        std::u32string_view(U"𒂍𒁾𒁀𒀀 𒀀𒈾𒀀𒀭 𒉌𒀝")));
```

<!--
Simplified example from icu4c/source/test/intltest/utfiteratortest.cpp

Eggsplanation: Split lines on U+000A without decoding, then decode the second line.
In case anyone needs to read the Sumerian aloud, the three lines on the slide read
edubbaʾaše iŋennen / edubbaʾa anam iak / dubŋu išid niŋzugubŋu igu;
translation:
I went to school. / what did you do at school? / I recited my tablet and ate my lunch.
See https://cdli.earth/artifacts/464238/reader/213101
-->

### Output: CodeUnits

The iterators do not merely return code point integers.
As you iterate over a string, you are getting a `CodeUnits` object representing a
Unicode code point and its code unit sequence.
This supports use cases that are not centered on the code point integer.

Here is a simplified version of the class:
```c++
class CodeUnits {
public:
    CodeUnits(const CodeUnits &other);
    CodeUnits &operator=(const CodeUnits &other);

    CP32 codePoint() const;

    UnitIter begin() const;
    UnitIter end() const;
    uint8_t length() const;

    std::basic_string_view<Unit> stringView() const;

    bool wellFormed() const;
};
```

The `CP32` code unit type is a required template parameter. It must be a 32-bit integer value, but it can be signed or unsigned.
You choose the code point integer type to fit your use case:
It is typically an ICU `UChar32` (=`int32_t` / signed) or a `char32_t` or a `uint32_t` (both unsigned).

Pick any of these if you do not read the code point value.

Here is an example for just counting how many code points are in a string:
```c++
int32_t countCodePoints16(std::u16string_view s) {
    auto range = utfStringCodePoints<UChar32, UTF_BEHAVIOR_SURROGATE>(s);
    return std::distance(range.begin(), range.end());
}
```

Fetching the first code point’s code unit sequence if it is well-formed:
```c++
std::string_view firstSequence8(std::string_view s) {
    if (s.empty()) { return {}; }
    auto range = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(s);
    auto units = *(range.begin());
    if (units.wellFormed()) {
        return units.stringView();
    } else {
        return {};
    }
}
```

### Input: UTF-8/16/32

The iterators and range adaptors work with any of the Unicode standard in-memory string encodings.
The appropriate types and implementations are usually auto-detected.

Details:

A `UTFIterator` is instantiated with the input code unit iterator type which may yield bytes for UTF-8, 16-bit values for UTF-16, or 32-bit values for UTF-32. Using the `utfIterator()` function deduces the code unit iterator type from its arguments.

You may not need to work with a `UTFIterator` directly.
A `UTFStringCodePoints` range adaptor can be constructed from a `std::string`, `std::string_view`, their variants (e.g., `std::u16string` or `std::u32string_view`), an `icu::UnicodeString`, or a wide variety of other code unit “ranges”.

Again, if you use the `utfStringCodePoints()` function, the `Range` template parameter is deduced from the argument.

### Input: Validation

You choose whether you want to validate the input string on the fly, by
using `utfStringCodePoints` or `unsafeUTFStringCodePoints`,
and similarly named siblings of the other types and functions.

The “unsafe” version compiles into smaller and faster code,
especially for UTF-8 which is fairly complex,
but it requires well-formed input.

The C++ standard string and string_view types, as well as ICU’s UnicodeString,
do not require or enforce well-formed Unicode strings.
However, you may enforce well-formed strings in large parts of your code base
by checking on input and checking or debug-checking between some processing steps.

“Unsafe”, that is, non-validating iterators return `UnsafeCodeUnits`,
which lack the `wellFormed()` function,
but otherwise have the same API as `CodeUnits`.

All of the validating classes take another required template parameter for
what code point value should be returned for an ill-formed code unit sequence:
[enum UTFIllFormedBehavior](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/utfiterator_8h.html#ae96b61b479fe4d7b8e525787353d1d46)
- `UTF_BEHAVIOR_NEGATIVE`:
  - Returns a negative value (-1=`U_SENTINEL`) instead of a code point.\
    (As usual, the intended check for a code point value from a well-formed sequence is
    `cp >= 0`, not `cp != U_SENTINEL`.)
  - If the `CP32` template parameter for the relevant classes is an unsigned type,
    then the negative value becomes 0xffffffff=`UINT32_MAX`.
- `UTF_BEHAVIOR_FFFD`: Returns U+FFFD Replacement Character.
- `UTF_BEHAVIOR_SURROGATE`:
  - UTF-8: Not allowed.
  - UTF-16: Returns the unpaired surrogate.
  - UTF-32: Returns the surrogate code point, or U+FFFD if out of range.

Again, pick any of these if you do not read the code point value.

### Input: Code unit iterators

C++ standard iterators are modeled after pointers,
with operators like `*` and `->` for value access,
`++` and `--` for iteration, and `==` for comparing with iteration limits.
In fact, pointers to code units work as inputs to `UTFIterator`. However, they are not required.

When supplying a pointer or a `contiguous_iterator` for the code units, then
`CodeUnits` supports the `stringView()` function.

When supplying at least a `bidirectional_iterator` for the code units, then the `UTFIterator` is also a `bidirectional_iterator`,
`std::make_reverse_iterator(iter)` will return an efficient backward iterator,
and using `utfStringCodePoints()` on a range of such iterators
supports `rbegin()` and `rend()`.

When supplying only a `forward_iterator`, then
the `UTFIterator` is also a `forward_iterator`, without backward iteration.

The minimal input is an `input_iterator`, which does not even allow reading the same value more than once.
The resulting `UTFIterator` is then also a single-pass `input_iterator`, and
it returns `CodeUnits` which only support `codePoint()`, `length()`, and (if validating) `wellFormed()`.

Each validating iterator needs to be instantiated with both
the current-position code unit iterator as well as a “limit” (exclusive-end) or “sentinel” iterator.
(Otherwise it would not know when to stop reading the variable number of code units.)
The API supports “sentinel” types that differ from the code unit iterator,
as long as the two can be compared.

An example of an `input_iterator` is the standard-input stream.
The API docs include this code example for that:

```c++20
template<typename InputStream>  // some istream or streambuf
std::u32string cpFromInput(InputStream &in) {
    // This is a single-pass input_iterator.
    std::istreambuf_iterator bufIter(in);
    std::istreambuf_iterator<typename InputStream::char_type> bufLimit;
    auto iter = utfIterator<char32_t, UTF_BEHAVIOR_FFFD>(bufIter);
    auto limit = utfIterator<char32_t, UTF_BEHAVIOR_FFFD>(bufLimit);
    std::u32string s32;
    for (; iter != limit; ++iter) {
        s32.push_back(iter->codePoint());
    }
    return s32;
}

std::u32string cpFromStdin() { return cpFromInput(std::cin); }
std::u32string cpFromWideStdin() { return cpFromInput(std::wcin); }
```

### Compiled code size

All of the code is inline-implemented in the header file.
Where available for a compiler (e.g., g++ and clang), the code is force-inlined.
As a result, the compiler will omit code whose output is not used.
For example, if you do not use the code point integer,
then the compiler will omit the code to assemble it from the code unit bits.

The code has also been written to make it easy for the compiler to detect and eliminate redundant code, especially in typical use cases including range-based for loops.

Some of the implementation code is necessarily fairly complex,
especially for validating iteration over UTF-8.
Compiler-friendly implementation techniques, force-inlining, and modern compiler optimizations yield code as small and fast as possible.