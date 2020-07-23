<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Formatting Numbers

Since ICU 60, the recommended mechanism for formatting numbers is 
[NumberFormatter](http://icu-project.org/apiref/icu4c/numberformatter_8h.html)
([Java](http://icu-project.org/apiref/icu4j/com/ibm/icu/number/NumberFormatter.html)).  NumberFormatter supports the formatting of:

- Decimal Formatting
- Currencies
- Measurement Units
- Percentages
- Scientific Notation
- Compact Notation

For number ranges, including currency and measurement unit ranges, see [NumberRangeFormatter](http://icu-project.org/apiref/icu4c/numberrangeformatter_8h.html) ([Java](http://icu-project.org/apiref/icu4j/com/ibm/icu/number/NumberRangeFormatter.html)).

For rule-based number formatting, including spellout rules and support for traditional numbering systems not covered by base-10 decimal digits, see [rbnf.md](rbnf.md).

For the classic NumberFormat class, which also includes legacy parsing support for localized number strings, see [legacy-numberformat.md](legacy-numberformat.md).
