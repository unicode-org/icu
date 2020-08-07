---
layout: default
title: Locales and Resources
nav_order: 5
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Locale
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

This chapter explains **locales**, a fundamental concept in ICU. ICU services
are parameterized by locale, to allow client code to be written in a
locale-independent way, but to deliver culturally correct results.

## The Locale Concept

A locale identifies a specific user community - a group of users who have
similar culture and language expectations for human-computer interaction (and
the kinds of data they process).

A community is usually understood as the intersection of all users speaking the
same language and living in the same country. Furthermore, a community can use
more specific conventions. For example, an English/United States/Military locale
is separate from the regular English/United States locale since the US military
writes times and dates differently than most of the civilian community.

A program should be localized according to the rules specific for the target
locale. Many ICU services rely on the proper locale identification in their
function.

The locale object in ICU is an identifier that specifies a particular locale and
has fields for language, country, and an optional code to specify further
variants or subdivisions. These fields also can be represented as a string with
the fields separated by an underscore.

In the C++ API, the locale is represented by the `Locale` class, which provides
methods for finding language, country and variant components. In the C API the locale
is defined simply by a character string. In the Java API, the locale is represented by
`ULocale` which is analogous to the `Locale` class but provide additional support
for ICU protocol. All the locale-sensitive ICU services use the locale information
to determine language and other locale specific parameters of their function.
The list of locale-sensitive services can be found in the Introduction to ICU
section. Other parts of the library use the locale as an indicator to
customize their behavior.

For example, when the locale-sensitive date format service needs to format a
date, it uses the convention appropriate to the current locale. If the locale is
English, it uses the word "Monday" and if it is French, it uses the word
"Lundi".

The locale object also defines the concept of a default locale. The default
locale is the locale, used by many programs, that regulates the rest of the
computer's behavior by default and is usually controlled by the user in a
control panel window. The locale mechanism does not require a program to know
which locale the user is using and thus makes most programming simpler.

Since locale objects can be passed as parameters or stored in variables, the
program does not have to know specifically which locales they identify. Many
applications enable a user to select a locale. The resulting locale object is
passed as a parameter, which then produces the customized behavior for that
locale.

A locale provides a means of identifying a specific region for the purposes of
internationalization and localization.

> :point_right: **Note**: An ICU locale is frequently confused with a Portable
> Operating System Interface (POSIX) locale ID. An ICU locale ID is not a POSIX
> locale ID. ICU locales do not specify the encoding and specify variant locales
> differently.

A locale consists of one or more pieces of ordered information:

### Language code

The languages are specified using a two- or three-letter lowercase code for a
particular language. For example, Spanish is "es", English is "en" and French is
"fr". The two-letter language code uses the 
[ISO-639](https://www.loc.gov/standards/iso639-2/) standard.

### Script code

The optional four-letter script code follows the language code. If specified, it
should be a valid script code as listed on the 
[Unicode ISO 15924 Registry](https://www.unicode.org/iso15924/iso15924-codes.html).

### Country code

There are often different language conventions within the same language. For
example, Spanish is spoken in many countries in Central and South America but
the currencies are different in each country. To allow for these differences
among specific geographical, political, or cultural regions, locales are
specified by two-letter, uppercase codes. For example, "ES" represents Spain and
"MX" represents Mexico. The two letter country code uses the
[ISO-3166](https://www.iso.org/iso-3166-country-codes.html) standard.

Java supports two letter country codes that uses ISO-3166 and UN M.49 code.

### Variant code

Differences may also appear in language conventions used within the same
country. For example, the Euro currency is used in several European countries
while the individual country's currency is still in circulation. Variations
inside a language and country pair are handled by adding a third code, the
variant code. The variant code is arbitrary and completely application-specific.
ICU adds "_EURO" to its locale designations for locales that support the Euro
currency. Variants can have any number of underscored key words. For example,
"EURO_WIN" is a variant for the Euro currency on a Windows computer.

Another use of the variant code is to designate the Collation (sorting order) of
a locale. For instance, the "es__TRADITIONAL" locale uses the traditional
sorting order which is different from the default modern sorting of Spanish.

Collation order and currency can be more flexibly specified using keywords
instead of variants; see below.

### Keywords

The final element of a locale is an optional list of keywords together with
their values. Keywords must be unique. Their order is not significant. Unknown
keywords are ignored. The handling of keywords depends on the specific services
that utilize them. Currently, the following keywords are recognized:

Keyword | Possible Values | Description
--------|-----------------|------------
calendar | A calendar specifier such as "gregorian", "islamic", "chinese", "islamic-civil", "hebrew", "japanese", or "buddhist". See the Key/Type Definitions table in the [Locale Data Markup Language](http://www.unicode.org/reports/tr35/) for a list of recognized values. | If present, the calendar keyword specifies the calendar type that the `Calendar` factory methods create. See the calendar locale and keyword handling section (§) of the [Calendar Classes](../datetime/calendar/index.md) chapter for details.
collation | A collation specifier such as "phonebook", "pinyin", "traditional", "stroke", "direct", or "posix". See the Key/Type Definitions table in the [Locale Data Markup Language](http://www.unicode.org/reports/tr35/) for a list of recognized values. | If present, the collation keyword modifies how the collation service searches through the locale data when instantiating a collator. See the collation locale and keyword handling section (§) of the [Collation Services Architecture](../collation/architecture.md) chapter for details.
currency | Any standard three-letter currency code, such as "USD" or "JPY". See the LocaleExplorer [currency list](http://demo.icu-project.org/icu-bin/locexp?_=en&SHOWCurrencies=1#Currencies) for a list of currently recognized currency codes. | If present, the currency keyword is used by `NumberFormat` to determine the currency to use to format a currency value, and by `ucurr_forLocale()` to specify a currency.
numbers | A numbering system specifier such as "latn", "arab", "deva", "hansfin" or "thai". See the Key/Type Definitions table in the [Locale Data Markup Language](http://www.unicode.org/reports/tr35/) for a list of recognized values. | If present, the numbers keyword is used by `NumberFormat` to determine the numbering system to be used for formatting and parsing numbers. The numbering system defines the set of digits used for decimal formatting, such as "latn" for western (ASCII) digits, or "thai" for Thai digits. The numbering system may also define complex algorithms for number formatting, such as "hansfin" for simplified Chinese numerals using financial ideographs.

If any of these keywords is absent, the service requesting it will typically use
the rest of the locale specifier in order to determine the appropriate behavior
for the locale. The keywords allow a locale specifier to override or refine this
default behavior.

### Examples

Locale ID | Language | Script | Country | Variant | Keywords | Definition
----------|----------|--------|---------|---------|----------|-----------
en_US | en | | US | | | English, United States of America. <br>Browse in [LocaleExplorer](http://demo.icu-project.org/icu-bin/locexp?_=en_US)
en_IE_PREEURO | en | | IE | | | English, Ireland. <br>Browse in [LocaleExplorer](http://demo.icu-project.org/icu-bin/locexp?_=en_IE_PREEURO)
en_IE@currency=IEP | en | | IE | | currency=IEP | English, Ireland with Irish Pound. <br>Browse in [LocaleExplorer](http://demo.icu-project.org/icu-bin/locexp?_=en_IE@currency=IEP)
eo | eo | | | | | Esperanto. <br>Browse in [LocaleExplorer](http://demo.icu-project.org/icu-bin/locexp?_=eo)
fr@collation=phonebook;calendar=islamic-civil | fr | | | | collation=phonebook <br>calendar=islamic-civil | French (Calendar=Islamic-Civil Calendar, Collation=Phonebook Order). <br>Browse in [LocaleExplorer](http://demo.icu-project.org/icu-bin/locexp?_=fr@collation=phonebook;calendar=islamic-civil)
sr_Latn_RS_REVISED@currency=USD | sr | Latn | RS | REVISED | currency=USD | Serbian (Latin, Yugoslavia, Revised Orthography, Currency=US Dollar) <br>Browse in [LocaleExplorer](http://demo.icu-project.org/icu-bin/locexp?d_=en&_=sr_Latn_RS_REVISED@currency=USD)


### Default Locales

Default locales are available to all the objects in a program. If you set a new
default locale for one section of code, it can affect the entire program.
Application programs should not set the default locale as a way to request an
international object. The default locale is set to be the system locale on that
platform.

For example, when you set the default locale, the change affects the default
behavior of the `Collator` and `NumberFormat` instances. When the default locale is
not wanted, you can set the desired locale using a factory method supplied with
the classes such as `Collator::createInstance()`.

Using the ICU C functions, `NULL` can be passed for a locale parameter to specify
the default locale.

## Locales and Services

ICU is implemented as a set of services. One example of a service is the
formatting of a numeric value into a string. Another is the sorting of a list of
strings. When client code wants to use a service, the first thing it does is
request a service object for a given locale. The resulting object is then
expected to perform the its operations in a way that is culturally correct for
the requested locale.

### Requested Locale

The **requested** locale is the one specified by the client code when the
service object is requested.

### Valid Locale

A **populated** locale is one for which ICU has data, or one in which client
code has registered a service. If the requested locale is not populated, then
ICU will fallback until it reaches a populated locale. The first populated
locale it reaches is the **valid** locale. The
valid locale is reachable from the requested locale via zero or more fallback
steps.

### Fallback

Locale **fallback** proceeds as follows:

1.  The variant is removed, if there is one.

2.  The country is removed, if there is one.

3.  The script is removed, if there is one.

4.  The ICU default locale is examined. The same set of steps is performed for
    the default locale.

At any point, if the desired data is found, then the fallback procedure stops.
Keywords are not altered during fallback until the default locale is reached, at
which point all keywords are replaced by those assigned to the default locale.

### Actual Locale

Services request specific resources within the valid locale. If the valid locale
directly contains the requested resource, then it is the **actual** locale. If
not, then ICU will fallback until it reaches a locale that does directly contain
the requested resource. The first such locale is the actual locale. The actual
locale is reachable from the valid locale via zero or more fallback steps.

### getLocale()

Client code may wish to know what the valid and actual locales are for a given
service object. To support this, ICU services provide the method `getLocale()`.
The `getLocale()` method takes an argument specifying whether the actual or 
valid locale is to be returned.

Some service object will have an empty or null return from `getLocale()`. This
indicates that the given service object was not created from locale data, or
that it has since been modified so that it no longer reflects locale data,
typically through alteration of the pattern (but not localized symbol changes --
such changes do not reset the actual and valid locale settings).

Currently, the services that support the `getLocale()` API are the following
classes and their subclasses:

### Functional Equivalence

Various services provide the API `getFunctionalEquivalent` to allow callers
determine the **functionally equivalent locale** for a requested locale. For
example, when instantiating a collator for the locale `en_US_CALIFORNIA`, the
functionally equivalent locale may be `en`.

The purpose of this is to allow applications to do intelligent caching. If an
application opens a service object for locale A with a functional equivalent Q
and caches it, then later when it requires a service object for locale B, it can
first check if locale B has the **same functional equivalent** as locale A; if
so, it can reuse the cached A object for the B locale, and be guaranteed the
same results as if it has instantiated a service object for B. In other words,

```
Service.getFunctionalEquivalent(A) == Service.getFunctionalEquivalent(B)
```

implies that the object returned by `Service.getInstance(A)` will behave
equivalently to the object returned by `Service.getInstance(B)`.

Here is a pseudo-code example:

The functional equivalent locale returned by a service has no meaning beyond
what is stated above. For example, if the functional equivalent of Greek is
Hebrew for collation, that makes no statement about the linguistic relation of
the languages -- it only means that the two collators are functionally
equivalent.

While two locales with the same functional equivalent are guaranteed to be
equivalent, the converse is **not** true: If two locales are in fact equivalent,
they may **not** return the same result from `getFunctionalEquivalent`. That is,
if the object returned by `Service.getInstance(A)` behaves equivalently to the
object returned by `Service.getInstance(B)`, `Service.getFunctionalEquivalent(A)`
**may or may not** be equal to `Service.getFunctionalEquivalent(B)`. Take again
the example of Greek and Hebrew, with respect to collation. These locales may
happen to be functional equivalents (since they each just turn on full
normalization), but it may or may not be the case that they return the same
functionally equivalent locale. This depends on how the data is structured
internally.

The functional equivalent for a locale may change over time. Suppose that Greek
were enhanced to change sorting of additional ancient Greek characters. In that
case, it would diverge; the functional equivalent of Greek would no longer be
Hebrew.

## Canonicalization

ICU works with **ICU format locale IDs**. These are strings that obey the
following character set and syntax restrictions:

1.  The only permitted characters are ASCII letters, hyphen ('-'), underscore
    ('_'), at-sign ('@'), equals sign ('='), and semicolon (';').

2.  IDs consist of either a base name, keyword list, or both. If a keyword list
    is present it must be preceded by an at-sign.

3.  The base name must precede the keyword list, if both are present.

4.  The base name defines the language, script, country, and variant, and can
    contain only ASCII letters, hyphen, or underscore.

5.  The keyword list consists of keyword/value pairs. Each keyword or value
    consists of one or more ASCII letters, hyphen, or underscore. Keywords and
    values are separated by a single equals sign. Multiple keyword/value pairs,
    if present, are separated by a single semicolon. A keyword may not appear
    without a value. The same keyword may not appear twice.

ICU performs two kinds of canonicalizing operations on 'ICU format' locale IDs.
Level 1 canonicalization is performed routinely and automatically by ICU APIs.
The recommended procedure for client code using locale IDs from outside sources
(e.g., POSIX, user input, etc.) is to pass such "foreign IDs" through level 2
canonicalization before use.

**Level 1 canonicalization**. This operation performs minor, isolated changes,
such as changing "en-us" to "en_US". Level 1 canonicalization is **not**
designed to handle "foreign" locale IDs (POSIX, .NET) but rather IDs that are in
ICU format, but which do not have normalized case and delimiters. Level 1
canonicalization is accomplished by the ICU functions `uloc_getName`,
`Locale::createFromName`, and `Locale::Locale`. The latter two APIs exist in both
C++ and Java.

1.  Level 1 canonicalization is defined only on ICU format locale IDs as defined
    above. Behavior with any other kind of input is unspecified.

2.  Case is normalized. Elements interpreted as **language** strings will be
    converted to lowercase. **Country** and **variant** elements will be
    converted to uppercase. **Script** elements will be title-cased. **Keywords**
    will be converted to lowercase. **Keyword values** will remain unchanged.

3.  Hyphens are converted to underscores.

4.  All 3-letter country codes are converted to 2-letter equivalents.

5.  Any 3-letter language codes are converted to 2-letter equivalents if
    possible. 3-letter language codes with no 2-letter equivalent are kept as
    3-letter codes.

6.  Keywords are sorted.

**Level 2 canonicalization**. This operation may make major changes to the ID,
possibly replacing entire elements of the ID. An example is changing
"fr-fr@EURO" to "fr_FR@currency=EUR". Level 2 canonicalization is designed to
translate POSIX and .NET IDs, as well as nonstandard ICU locale IDs. Level 2 is
a **superset** of level 1; every operation performed by level 1 is also
performed by level 2. Level 2 canonicalization is performed by `uloc_canonicalize`
and `Locale::createCanonical`. The latter API exists in both C++ and Java.

1.  Level 2 canonicalization operates on ICU format locale IDs with the
    following additions:

    1.  The period ('.') is also a valid input character.

    2.  An at-sign may be followed by text that is not a keyword/value pair. If
        present, such text is added to the variant.

2.  POSIX variants are normalized, e.g., "en_US@VARIANT" => "en_US_VARIANT".

3.  POSIX charset specifiers are **deleted**, e.g. "en_US.utf8" => "en_US".

4.  The variant "EURO" is converted to the keyword specifier "currency=EUR".
    This conversion applies to both "fr_FR_EURO" and "fr_FR@EURO" style IDs.

5.  The variant "PREEURO" is converted to the keyword specifier "currency=K",
    where K is the 3-letter currency code for the country's national currency in
    effect at the time of the euro transitiion. This conversion applies to both
    "fr_FR_PREURO" and "fr_FR@PREURO" style IDs. This mapping is only performed
    for the following locales: ca_ES (ESP), de_AT (ATS), de_DE (DEM), de_LU
    (EUR), el_GR (GRD), en_BE (BEF), en_IE (IEP), es_ES (ESP), eu_ES (ESP),
    fi_FI (FIM), fr_BE (BEF), fr_FR (FRF), fr_LU (LUF), ga_IE (IEP), gl_ES
    (ESP), it_IT (ITL), nl_BE (BEF), nl_NL (NLG), pt_PT (PTE).

6.  The following IANA registered ISO 3066 names are remapped: art_LOJBAN =>
    jbo, cel_GAULISH => cel__GAULISH, de_1901 => de__1901, de_1906 => de__1906,
    en_BOONT => en__BOONT, en_SCOUSE => en__SCOUSE, sl_ROZAJ => sl__ROZAJ,
    zh_GAN => zh__GAN, zh_GUOYU => zh, zh_HAKKA => zh__HAKKA, zh_MIN => zh__MIN,
    zh_MIN_NAN => zh__MINNAN, zh_WUU => zh__WUU, zh_XIANG => zh__XIANG, zh_YUE
    => zh__YUE.

7.  The following .NET identifiers are remapped: "" (empty string) =>
    en_US_POSIX, az_AZ_CYRL => az_Cyrl_AZ, az_AZ_LATN => az_Latn_AZ, sr_SP_CYRL
    => sr_Cyrl_SP, sr_SP_LATN => sr_Latn_SP, uz_UZ_CYRL => uz_Cyrl_UZ,
    uz_UZ_LATN => uz_Latn_UZ, zh_CHS => zh_Hans, zh_CHT => zh_Hant. The empty
    string is not remapped if a keyword list is present.

8.  Variants specifying collation are remapped to collation keyword specifiers,
    as follows: de__PHONEBOOK => de@collation=phonebook, es__TRADITIONAL =>
    es@collation=traditional, hi__DIRECT => hi@collation=direct, zh_TW_STROKE =>
    zh_TW@collation=stroke, zh__PINYIN => zh@collation=pinyin.

9.  Variants specifying a calendar are remapped to calendar keyword specifiers,
    as follows: ja_JP_TRADITIONAL => ja_JP@calendar=japanese, th_TH_TRADITIONAL
    => th_TH@calendar=buddhist.

10. Special case: C => en_US_POSIX.

Certain other operations are not performed by either level 1 or level 2
canonicalization. These are listed here for completeness.

1.  Language identifiers that have been superseded will not be remapped. In
    particular, the following transformations are not performed:

    1.  no => nb

    2.  iw => he

    3.  id => in

    4.  nb_no_NY => nn_NO

2.  The behavior of level 2 canonicalization when presented with a remapped ID
    combined together with keywords is not defined. For example,
    fr_FR_EURO@currency=FRF has an undefined level 2 canonicalization.

All APIs (with a few exceptions) in ICU4C that take a `const char* locale` 
parameter can be assumed to automatically peform level 1 canonicalization before
using the locale ID to do resource lookup, keyword interpretation, etc.
Specifically, the static API `getLanguage`, `getScript`, `getCountry`, and `getVariant`
behave exactly like their non-static counterparts in the class `Locale`. That is,
for any locale ID `loc`, `new Locale(loc).getFoo() == Locale::getFoo(loc)`, where
Foo is one of Language, Script, Country, or Variant.

The `Locale` constructor (in C++ and Java) taking multiple strings behaves exactly
as if those strings were concatenated, with the '_' separator inserted between
two adjacent non-empty strings, and the result passed to `uloc_getName`.

> :point_right: **Note**: Throughout this discussion `Locale` refers to both the
> C++ `Locale` class and the ICU4J `com.ibm.icu.util.ULocale` class. Although C++
> notation is used, all statements made regarding `Locale` apply equally to
> `com.ibm.icu.util.ULocale`.

## Usage: Creating Locales

If you are localizing an application to a locale that is not already supported,
you need to create your own `Locale` object. New `Locale` objects are created using
one of the three constructors in this class:

```c++
Locale( const char * language);
Locale( const char * language, const char * country);
Locale( const char * language, const char * country, const char * variant);
```

Because a locale object is just an identifier for a region, no validity check is
performed. If you want to verify that the particular resources are available for
the locale you construct, you must query those resources. For example, you can
query the `NumberFormat` object for the locales it supports using its
`getAvailableLocales()` method.

New `ULocale` objects in Java are created using one the following three
constructor in this class:

```java
ULocale( String localeID)
ULocale( String a, String b)
ULocale( String a, String b, String c)
```

The locale ID passed in the constructor consists of optional languages, scripts,
country and variant fields in that oder, separated by underscore, followed by an
optional keywords. For example, "en_US", "sy_Cyrl_YU", "zh__pinyin",
"es_ES@currency=EUR,collation=traditional". The fields a, b, c in the other two
constructors are the components of the locale ID. For example, the following two
locale object are same:

```java
ULocale ul = new Ulocale("sy_Cyrl_YU");
ULocale ul = new ULocale("sy", "Cyrl", "YU");
```

In C++, the `Locale` class provides a number of convenient constants that you can
use to create locales. For example, the following refers to a `NumberFormat` object
for the United States:

```c++
Locale::getUS()
```

In C, a string with the language country and variant concatenated together with
an underscore '_' describe a locale. For example, "en_US" is a locale that is
based on the English language in the United States. The following can be used as
equivalents to the locale constants:

```c
ULOC_US
```

In Java, the `ULocale` provides a number of convenient constants that can be used
to create locales.

```java
ULocale.US;
```

## Usage: Retrieving Locales

Locale-sensitive classes have a `getAvailableLocales()` method that returns all of
the locales supported by that class. This method also shows the other methods
that get locale information from the resource bundle. For example, the following
shows that the `NumberFormat` class provides three convenience methods for
creating a default `NumberFormat` object:

```c++
NumberFormat::createInstance();
NumberFormat::createCurrencyInstance();
NumberFormat::createPercentInstance();
```

Locale-sensitive classes in Java also have a `getAvailableULocales()` method that
returns all of the locales supported by that class.

### Displayable Names

Once you've created a `Locale` in C++ and a `ULocale` in java, you can perform a
query of the locale for information about itself. The following shows the
information you can receive from a locale:

Method | Description 
-------|------------
`getCountry()` | Retrieves the ISO Country Code
`getLanguage()` | Retrieves the ISO Language
`getDisplayCountry()` | Shows the name of the country suitable for displaying information to the user
`getDisplayLanguage()` | Shows the name of the language suitable for displaying to the user

> :point_right: **Note**: The `getDisplayXXX` methods are themselves locale-sensitive 
> and have two versions in C++: one that uses the default locale and one that takes a
> locale as an argument and displays the name or country in a language appropriate to
> that locale.

> :point_right: **Note**: In Java, the `getDisplayXXX` methods have three versions:
> one that uses the default locale, the other takes a locale as an argument and the
> third one which takes locale ID as an argument.

Each class that performs locale-sensitive operations allows you to get all the
available objects of that type. You can sift through these objects by language,
country, or variant, and use the display names to present a menu to the user.
For example, you can create a menu of all the collation objects suitable for a
given language.

### HTTP Accept-Language

ICU provides functions to negotiate the best locale to use for an operation,
given a user's list of acceptable locales, and the application's list of
available locales. For example, a browser sends the web server the HTTP
"`Accept-Language`" header indicating which locales, with a ranking, are
acceptable to the user. The server must determine which locale to use when
returning content to the user.

Here is an example of selecting an acceptable locale within a CGI application:

C:

```c
char resultLocale[200];
UAcceptResult outResult;
available = ures_openAvailableLocales("myBundle", &status);
int32_t len = uloc_acceptLanguageFromHTTP(resultLocale, 200, &outResult, 
                getenv("HTTP_ACCEPT_LANGUAGE"), available, &status);
if(U_SUCCESS(status)) {
    printf("Using locale %s\n", outResult);
}
```

Here is an example of selecting an acceptable locale within a Java application:

Java:

```java
ULocale[] availableLocales = ULocale.getAvailableLocales();
boolean[] fallback = { false };
ULocale result = ULocale.acceptLanguage(availableLocales, fallback);

System.out.println("Using locale " + result);
```

> :point_right: **Note**: As of this writing, this functionality is available in
> both C and Java. Please read the following two linked documents for important 
> considerations and recommendations when using this header in a web application.

> *For further information about the Accept-Language HTTP header:* <br>
> https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.4 <br>
> *Notes and cautions about the use of this header:* <br>
> https://www.w3.org/International/questions/qa-accept-lang-locales

## Programming in C vs. C++ vs. Java

See Programming for Locale in [C, C++ and Java](examples.md) for more information.
