---
layout: default
title: Locale Examples
nav_order: 1
parent: Locales and Resources
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Locale Examples

## Locale Currency Conventions

Application programs should not reset the default locale as a way of requesting
an international object, because resetting default locale affects the other
programs running in the same process. Use one of the factory methods instead,
e.g. `Collator::createInstance(Locale)`.

In general, a locale object or locale string is used for specifying the locale.
Here is an example to specify the Belgium French with Euro currency locale:

**C++**

```c++
Locale loc("fr", "BE");
Locale loc2("fr_BE");
```

**C**

```c
const char *loc = "fr_BE";
```

**Java**

```java
ULocale loc = new ULocale("fr_BE");
```

> :point_right: **Note**: **Java** does **not** support the form `Locale("xx_yy_ZZ")`,
> instead use the form `Locale("xx","yy","ZZ")`.

## Locale Constants

A `Locale` is the mechanism for identifying the kind of object (`NumberFormat`) that
you would like to get. The locale is just a mechanism for identifying objects,
not a container for the objects themselves. For example, the following creates
various number formatters for the "Germany" locale:

**C++**

```c++
UErrorCode status = U_ZERO_ERROR;
NumberFormat *nf;
nf = NumberFormat::createInstance(Locale::getGermany(), status);
delete nf;
nf = NumberFormat::createCurrencyInstance(Locale::getGermany(), status);
delete nf;
nf = NumberFormat::createPercentInstance(Locale::getGermany(), status);
delete nf;
```

**C**

```c
UErrorCode success = U_ZERO_ERROR;
UChar *pattern;
UNumberFormat *nf;
UParseError *pe;
nf = unum_open( UNUM_DEFAULT, pattern, 0, "fr_FR", pe, &success );
unum_close(nf);
nf = unum_open( UNUM_CURRENCY, pattern, 0, "fr_FR", pe, &success );
unum_close(nf);
nf = unum_open( UNUM_PERCENT, pattern, 0, "fr_FR", pe, &success );
unum_close(nf);
```

**Java**

```java
NumberFormat nf = NumberFormat.getInstance(ULocale.GERMANY);
NumberFormat currencyInstance = NumberFormat.getCurrencyInstance(ULocale.GERMANY);
NumberFormat percentInstance = NumberFormat.getPercentInstance(ULocale.GERMANY);
```

## Querying Locale

Each class that performs locale-sensitive operations allows you to get all the
available objects of that type. You can sift through these objects by language,
country, or variant, and use the display names to present a menu to the user.
For example, you can create a menu of all the collation objects suitable for a
given language. For example, the following shows the display name of all
available locales in English (US):

**C++**

```c++
int32_t count;
const Locale* list = NULL;
UnicodeString result;
list = Locale::getAvailableLocales(count);
for (int i = 0; i < count; i++) {
    list[i].getDisplayName(Locale::getUS(), result);
    /* print result */
}
```

**C**

```c
int32_t count;
UChar result[100];
int i = 0;
UErrorCode status = U_ZERO_ERROR;
count = uloc_countAvailable();
for (i = 0; i < count; i++) {
    uloc_getDisplayName(uloc_getAvailable(i), "en_US", result, 100, &status);
    /* print result */
}
```

**Java**

```java
import com.ibm.icu.util.*;
public class TestLocale {
    public void run() {
        ULocale l[] = ULocale.getAvailableLocales();
        int n = l.length;
        for(int i=0; i<n; ++i) {
            ULocale locale = l[i];
            System.out.println();
            System.out.println("The base name of this locale is: " + locale.getBaseName());
            System.out.println("Locale's country name: " + locale.getDisplayCountry());
            System.out.println("Locale's script name: " + locale.getDisplayScript());
            System.out.println("Locale's language: " + locale.getDisplayLanguage());
            System.out.println("Locale's variant: " + locale.getDisplayVariant());
        }
    }
    public static void main(String args[]) {
        new TestLocale().run();
    }
}
```
