---
layout: default
title: FormattedValue
nav_order: 4
grand_parent: Formatting
parent: Formatting Numbers
---
<!--
© 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# `FormattedValue`
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

`FormattedValue` is an abstraction for localized strings with attributes
returned by a number of ICU formatters.  APIs for `FormattedValue` are available
in Java, C++, and C.  For more details and a list of all implementing classes,
refer to the API docs:

- [C++ `FormattedValue`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classicu_1_1FormattedValue.html)
- [C `UFormattedValue`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/globals_u.html) -- search for "resultAsValue"
- [Java `FormattedValue`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/FormattedValue.html)

## Nested Span Fields

Certain ICU formatters, like `FormattedList` and `FormattedDateInterval`, use
*span fields* to return information about which spans of a string correspond
to different input parameters.  In C and C++, span fields are implemented
using a field category, with the field being set to the input index; in Java,
they are implemented by associating an `Integer` value with a `SpanField`
subclass.

For example, in C++, here is how you can determine which region in a formatted
date interval corresponds to the 2nd argument (index 1) in the input date
interval (the "to" date):

```cpp
// Let fmt be a DateIntervalFormat for locale en-US and skeleton dMMMMy
// Let input1 be July 20, 2018 and input2 be August 3, 2018:
FormattedDateInterval result = fmt->formatToValue(*input1, *input2, status);
assertEquals("Expected output from format",
    u"July 20 \u2013 August 3, 2018", result.toString(status));
ConstrainedFieldPosition cfpos;
cfpos.constrainField(UFIELD_CATEGORY_DATE_INTERVAL_SPAN, 0);
if (result.nextPosition(cfpos, status)) {
    assertEquals("Expect start index", 0, cfpos.getStart());
    assertEquals("Expect end index", 7, cfpos.getLimit());
} else {
    // No such span: can happen if input dates are equal.
}
assertFalse("No more than one occurrence of the field",
    result.nextPosition(cfpos, status));
```

In C, the code looks very similar, except you use the equivalent C types.

In Java, use the `constrainFieldAndValue` method:

```java
// Let fmt be a DateIntervalFormat for locale en-US and skeleton dMMMMy
// Let input1 be July 20, 2018 and input2 be August 3, 2018:
FormattedDateInterval result = fmt.formatToValue(input1, input2);
assertEquals("Expected output from format",
    "July 20 \u2013 August 3, 2018", result.toString());
ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();
cfpos.constrainFieldAndValue(DateIntervalFormat.SpanField.DATE_INTERVAL_SPAN, 0);
if (result.nextPosition(cfpos)) {
    assertEquals("Expect start index", 0, cfpos.getStart());
    assertEquals("Expect end index", 7, cfpos.getLimit());
} else {
    // No such span: can happen if input dates are equal.
}
assertFalse("No more than one occurrence of the field",
    result.nextPosition(cfpos));
```

A span may cover multiple primitive fields; in the above example, the span
contains both a month and a date. Using `FormattedValue`, those primitive
fields will also be present, and you can check their start and end indices to
see if they are contained within a desired span.
