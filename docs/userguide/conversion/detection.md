---
layout: default
title: Charset Detection
nav_order: 3
parent: Conversion
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Character Set Detection
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Character set detection is the process of determining the character set, or
encoding, of character data in an unknown format. This is, at best, an imprecise
operation using statistics and heuristics. Because of this, detection works best
if you supply at least a few hundred bytes of character data that's mostly in a
single language. In some cases, the language can be determined along with the
encoding.

Several different techniques are used for character set detection. For
multi-byte encodings, the sequence of bytes is checked for legal patterns. The
detected characters are also check against a list of frequently used characters
in that encoding. For single byte encodings, the data is checked against a list
of the most commonly occurring three letter groups for each language that can be
written using that encoding. The detection process can be configured to
optionally ignore html or xml style markup, which can interfere with the
detection process by changing the statistics.

The input data can either be a Java input stream, or an array of bytes. The
output of the detection process is a list of possible character sets, with the
most likely one first. For simplicity, you can also ask for a Java Reader that
will read the data in the detected encoding.

There is another character set detection C++ library, the [Compact Encoding
Detector](https://github.com/google/compact_enc_det), that may have a lower
error rate, particularly when working with short samples of text.

## CharsetMatch

The `CharsetMatch` class holds the result of comparing the input data to a
particular encoding. You can use an instance of this class to get the name of
the character set, the language, and how good the match is. You can also use
this class to decode the input data.

To find out how good the match is, you use the `getConfidence()` method to get a
*confidence value*. This is an integer from 0 to 100. The higher the value, the
more confidence there is in the match For example:

```java
CharsetMatch match = ...;
int confidence;
confidence = match.getConfidence();
if (confidence < 50 ) {
// handle a poor match...
} else {
// handle a good match...
}
```

In C, you can use the `ucsdet_getConfidence(const UCharsetMatch *ucsm, UErrorCode *status)`
method to get a confidence value.

```c
const UCharsetMatch *ucm;
UErrorCode status = U_ZERO_ERROR;
int32_t confidence = ucsdet_getConfidence(ucm, &status);
if (confidence <50) {
    // handle a poor match...
} else {
    // handle a good match...
}
```

To get the name of the character set, which can be used as an encoding name in
Java, you use the `getName()` method:

```java
CharsetMatch match = ...;
byte characterData[] = ...;
String charsetName;
String unicodeData;
charsetName = match.getName();
unicodeData = new String(characterData, charsetName);
```

To get the name of the character set in C:

```c
const UCharsetMatch *ucm;
UErrorCode status = U_ZERO_ERROR;
const char *name = ucsdet_getName(ucm, &status);
```

To get the three letter ISO code for the detected language, you use the
`getLanguage()` method. If the language could not be determined, `getLanguage()`
will return `null`. Note that language detection does not work with all charsets,
and includes only a very small set of possible languages. It should not used if
robust, reliable language detection is required.

```java
CharsetMatch match = ...;
String languageCode;
languageCode = match.getLanguage();
if (languageCode != null) {
    // handle the language code...
}
```

The `ucsdet_getLanguage(const UCharsetMatch *ucsm, UErrorCode *status)` method
can be used in C to get the language code. If the language could not be
determined, the method will return an empty string.

```c
const UCharsetMatch *ucm;
UErrorCode status = U_ZERO_ERROR;
const char *language = ucsdet_getLanguage(ucm, &status);
```

If you want to get a Java String containing the converted data you can use the
`getString()` method:

```java
CharsetMatch match = ...;
String unicodeData;
unicodeData = match.getString();
```

If you want to limit the number of characters in the string, pass the maximum
number of characters you want to the `getString()` method:

```java
CharsetMatch match = ...;
String unicodeData;
unicodeData = match.getString(1024);
```

To get a `java.io.Reader` to read the converted data, use the `getReader()` method:

```java
CharsetMatch match = ...;
Reader reader;
StringBuffer sb = new StringBuffer();
char[] buffer = new char[1024];
int bytesRead = 0;
reader = match.getReader();
while ((bytesRead = reader.read(buffer, 0, 1024)) >= 0) {
    sb.append(buffer, 0, bytesRead);
}
reader.close();
```

## CharsetDetector

The `CharsetDetector` class does the actual detection. It matches the input data
against all character sets, and computes a list of `CharsetMatch` objects to hold
the results. The input data can be supplied as an array of bytes, or as a
`java.io.InputStream`.

To use a `CharsetDetector` object, first you construct it, and then you set the
input data, using the `setText()` method. Because setting the input data is
separate from the construction, it is easy to reuse a `CharsetDetector` object:

```java
CharsetDetector detector;
byte[] byteData = ...;
InputStream streamData = ...;
detector = new CharsetDetector();
detector.setText(byteData);
// use detector with byte data...
detector.setText(streamData);
// use detector with stream data...
```

If you want to know which character set matches your input data with the highest
confidence, you can use the `detect()` method, which will return a `CharsetMatch`
object for the match with the highest confidence:

```java
CharsetDetector detector;
CharsetMatch match;
byte[] byteData = ...;
detector = new CharsetDetector();
detector.setText(byteData);
match = detector.detect();
```

If you want to know which character set matches your input data in C, you can
use the `ucsdet_detect(UCharsetDetector *csd , UErrorCode *status)` method.

```c
UCharsetDetector *csd;
const UCharsetMatch *ucm;
static char buffer[BUFFER_SIZE] = {....};
int32_t inputLength = ... // length of the input text
UErrorCode status = U_ZERO_ERROR;
ucsdet_setText(csd, buffer, inputLength, &status);
ucm = ucsdet_detect(csd, &status);
```

If you want to know all of the character sets that could match your input data
with a non-zero confidence, you can use the `detectAll()` method, which will
return an array of `CharsetMatch` objects sorted by confidence, from highest to
lowest.:

```java
CharsetDetector detector;
CharsetMatch matches[];
byte[] byteData = ...;
detector = new CharsetDetector();
detector.setText(byteData);
matches = detector.detectAll();
for (int m = 0; m < matches.length; m += 1) {
    // process this match...
}
```

> :point_right: **Note**: The `ucsdet_detectALL(UCharsetDetector *csd , int32_t *matchesFound, UErrorCode *status)` 
> method can be used in C in order to detect all of the character sets where `matchesFound` is a pointer
> to a variable that will be set to the number of charsets identified that are consistent with the input data.

The `CharsetDetector` class also implements a crude *input filter* that can strip
out html and xml style tags. If you want to enable the input filter, which is
disabled when you construct a `CharsetDetector`, you use the `enableInputFilter()`
method, which takes a boolean. Pass in true if you want to enable the input
filter, and false if you want to disable it:

```java
CharsetDetector detector;
CharsetMatch match;
byte[] byteDataWithTags = ...;
detector = new CharsetDetector();
detector.setText(byteDataWithTags);
detector.enableInputFilter(true);
match = detector.detect();
```

To enable an input filter in C, you can use
`ucsdet_enableInputFilter(UCharsetDetector *csd, UBool filter)` function.

```c
UCharsetDetector *csd;
const UCharsetMatch *ucm;
static char buffer[BUFFER_SIZE] = {....};
int32_t inputLength = ... // length of the input text
UErrorCode status = U_ZERO_ERROR;
ucsdet_setText(csd, buffer, inputLength, &status);
ucsdet_enableInputFilter(csd, true);
ucm = ucsdet_detect(csd, &status);
```

If you have more detailed knowledge about the structure of the input data, it is
better to filter the data yourself before you pass it to CharsetDetector. For
example, you might know that the data is from an html page that contains CSS
styles, which will not be stripped by the input filter.

You can use the `inputFilterEnabled()` method to see if the input filter is
enabled:

```java
CharsetDetector detector;
detector = new CharsetDetector();
// do a bunch of stuff with detector
// which may or may not enable the input filter...
if (detector.inputFilterEnabled()) {
    // handle enabled input filter
} else {
    // handle disabled input filter
}
```

> :point_right: **Note**: The ICU4C API provide `uscdet_isInputFilterEnabled(const UCharsetDetector* csd)` function
> to check whether the input filter is enabled.

The `CharsetDetector` class also has two convenience methods that let you detect
and convert the input data in one step: the `getReader()` and `getString()` methods:

```java
CharsetDetector detector;
byte[] byteData = ...;
InputStream streamData = ...;
String unicodeData;
Reader unicodeReader;
detector = new CharsetDetector();
unicodeData = detector.getString(byteData, null);
unicodeReader = detector.getReader(streamData, null);
```

> :point_right: **Note**: The second argument to the `getReader()` and `getString()` methods
> is a String called `declaredEncoding`, which is not currently used. There is also a
> `setDeclaredEncoding()` method, which is also not currently used.

The following code is equivalent to using the convenience methods:

```java
CharsetDetector detector;
CharsetMatch match;
byte[] byteData = ...;
InputStream streamData = ...;
String unicodeData;
Reader unicodeReader;
detector = new CharsetDetector();
detector.setText(byteData);
match = detector.detect();
unicodeData = match.getString();
detector.setText(streamData);
match = detector.detect();
unicodeReader = match.getReader();CharsetDetector
```

## Detected Encodings

The following table shows all the encodings that can be detected. You can get
this list (without the languages) by calling the `getAllDetectableCharsets()`
method:

| **Character Set** | **Languages** |
| ----------------- | ------------- |
| UTF-8             | &nbsp;        |
| UTF-16BE          | &nbsp;        |
| UTF-16LE          | &nbsp;        |
| UTF-32BE          | &nbsp;        |
| UTF-32LE          | &nbsp;        |
| Shift_JIS         | Japanese      |
| ISO-2022-JP       | Japanese      |
| ISO-2022-CN       | Simplified Chinese |
| ISO-2022-KR       | Korean        |
| GB18030           | Chinese       |
| Big5              | Traditional Chinese |
| EUC-JP            | Japanese      |
| EUC-KR            | Korean        |
| ISO-8859-1        | Danish, Dutch, English, French, German, Italian, Norwegian, Portuguese, Swedish |
| ISO-8859-2        | Czech, Hungarian, Polish, Romanian |
| ISO-8859-5        | Russian       |
| ISO-8859-6        | Arabic        |
| ISO-8859-7        | Greek         |
| ISO-8859-8        | Hebrew        |
| ISO-8859-9        | Turkish       |
| windows-1250      | Czech, Hungarian, Polish, Romanian |
| windows-1251      | Russian       |
| windows-1252      | Danish, Dutch, English, French, German, Italian, Norwegian, Portuguese, Swedish |
| windows-1253      | Greek         |
| windows-1254      | Turkish       |
| windows-1255      | Hebrew        |
| windows-1256      | Arabic        |
| KOI8-R            | Russian       |
| IBM420            | Arabic        |
| IBM424            | Hebrew        |
