---
layout: default
title: Why Use ICU4J?
nav_order: 100
parent: ICU4J
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Why Use ICU4J?

## Summary

*   Fully implements current standards
    *   Unicode collation, normalization, break iteration
    *   Updated more frequently than Java
    *   Full CLDR Locale data
*   Improved performance

## Details

*   Normalization
    *   Addresses lack of Unicode normalization support in Java 5
    *   Addresses outdated Unicode normalization support in Java 6
*   Up-To-Date Unicode version
    *   Java 5 & 6 are Unicode 4.0, while ICU 4.0 is Unicode 5.1
    *   Characters added after Unicode 4.0 do not have character properties in
        Java
*   IDNA and StringPrep
    *   Addresses lack of Internationalized Domain Name support in Java 5
    *   Addresses generic stringprep (RFC3454) support. stringprep is required
        for supporting various internet protocols (NFS, LDAP...)
*   Collation
    *   Provides Unicode standard compliant collation support
    *   ICU Collator fully implements UTR#10, while the Java implementation is
        outdated and not compatible.
*   Provides ICU UnicodeSet for easy character range validation
    *   much more flexible and convenient for validating identifiers/text tokens
        with a given syntax
    *   full boolean operations (union, intersection, difference)
    *   all Unicode properties supported
*   Locales
    *   BCP47 (language tag) support in locale class (supporting "script",
        3-letter language codes, 3-digit region codes)
    *   Locale data coverage - much better, many more locales, up-to-date
*   Broader charset converter coverage
    *   In ICU4J 4.2, also output charset selection
    *   Custom fallback in charset converter
*   Other features missing in the JDK
    *   Dates:
        *   Many more date formats: month+day, year+month,...
        *   Date interval formats: "Dec 15-17, 2009"
        *   APIs for returning time zone transitions
    *   Other formatting
        *   Plural formatting, including units: "1 hour" / "2 hours"
        *   Rule based number format ("three thousand two hundred")
        *   Extensive Non-Gregorian calendar support
    *   Transliterator (for flexible text/script transformations)
    *   Collation-sensitive string search
    *   Same data as ICU4C, allowing same behavior across programming languages
    *   All Unicode character properties - over 80, Java provides access to only
        about 10
    *   Thai wordbreak

## Performance & Size

*   Instantiation times are comparable
    *   Common instantiate and reuse model
    *   ICU4J and Java both use caches to limit impact
*   Collation performance *many times* faster
    *   sorting: 2 to 20 times faster
    *   sort key generation: 1.5 to 4 times faster
    *   sort key length: 2/3 to 1/4 the length of Java sort keys
*   Property access much faster (isLetter, isWhitespace,...)
*   Can easily produce scaled-down version (removing data)

## API

*   Subclasses of JDK classes where possible
*   Drop-in (change of import) if not

## Summary

* **ICU4J is not for you if**

    *   you have tight size constraints
    *   you require the Java runtime behavior

* **ICU4J is for you if**

    *   you need full compliance with current standards
    *   you need current or additional locale and property data
    *   you need customizability
    *   you need features missing from Java (normalization, collation,...)
    *   you need better performance
