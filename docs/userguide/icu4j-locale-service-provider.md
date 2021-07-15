---
layout: default
title: ICU4J Locale Service Provider
nav_order: 7
parent: ICU
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4J Locale Service Provider
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Java SE 6 introduced a new feature which allows Java user code to extend locale
support in Java runtime environment. JREs shipped by Oracle or IBM come with
decent locale coverage, but some users may want more locale support. Java SE 6
includes abstract classes extending
[`java.util.spi.LocaleServiceProvider`](http://download.oracle.com/javase/6/docs/api/java/util/spi/LocaleServiceProvider.html).
Java SE 6 users can create a subclass of these abstract class to supply their
own locale support for text break, collation, date/number formatting or
providing translations for currency, locale and time zone names.

ICU4J has been providing more comprehensive locale coverage than standard JREs.
However, Java programmers have to use ICU4J's own internationalization service
APIs (`com.ibm.icu.\*`) to utilize the rich locale support. Sometimes, the
migration is not an option for various reasons. For example, your code may
depend on existing Java libraries utilizing JDK internationalization service
APIs, but you have no access to the source code. In this case, it is not
possible to modify the libraries to use ICU4J APIs.

ICU4J Locale Service Provider is a component consists of classes implementing
the Java SE 6 locale sensitive service provider interfaces. Available service
providers are:

*   [`BreakIteratorProvider`](http://download.oracle.com/javase/6/docs/api/java/text/spi/BreakIteratorProvider.html)
*   [`CollatorProvider`](http://download.oracle.com/javase/6/docs/api/java/text/spi/CollatorProvider.html)
*   [`DateFormatProvider`](http://download.oracle.com/javase/6/docs/api/java/text/spi/DateFormatProvider.html)
*   [`DateFormatSymbolsProvider`](http://download.oracle.com/javase/6/docs/api/java/text/spi/DateFormatSymbolsProvider.html)
*   [`DecimalFormatSymbolsProvider`](http://download.oracle.com/javase/6/docs/api/java/text/spi/DecimalFormatSymbolsProvider.html)
*   [`NumberFormatProvider`](http://download.oracle.com/javase/6/docs/api/java/text/spi/NumberFormatProvider.html)
*   [`CurrencyNameProvider`](http://download.oracle.com/javase/6/docs/api/java/util/spi/CurrencyNameProvider.html)
*   [`LocaleNameProvider`](http://download.oracle.com/javase/6/docs/api/java/util/spi/LocaleNameProvider.html)
*   [`TimeZoneNameProvider`](http://download.oracle.com/javase/6/docs/api/java/util/spi/TimeZoneNameProvider.html)

ICU4J Locale Service Provider is designed to work as installed extensions in a
JRE. Once the component is configured properly, Java application running on the
JRE automatically picks the ICU4J's internationalization service implementation
when a requested locale is not available in the JRE.

## Using ICU4J Locale Service Provider

Java SE 6 locale sensitive service providers are using the [Java Extension
Mechanism](http://download.oracle.com/javase/6/docs/technotes/guides/extensions/index.html).
An implementation of a locale sensitive service provider is installed as an
optional package to extend the functionality of the Java core platform. To
install an optional package, its JAR files must be placed in the Java extension
directory. The standard location is *<java-home>/lib/ext*. You can alternatively
use the system property *java.ext.dirs* to specify one or more locations where
optional packages are installed. For example, if the JRE root directory is
*JAVA_HOME* and you put ICU4J Locale Service Provider files in *ICU_SPI_DIR*, the
ICU4J Locale Service Provider is enabled by the following command:

*   `java -Djava.ext.dirs=%JAVA_HOME%\\lib\\ext;%ICU_SPI_DIR% <your_java_app>` \[Microsoft Windows\]
*    `java -Djava.ext.dirs=$JAVA_HOME/lib/ext:$ICU_SPI_DIR <your_java_app>` \[Linux,Solaris and other unix like platforms\]

The ICU4J's implementations of Java SE 6 locale sensitive service provider
interfaces and configuration files are packaged in a single JAR file
(*icu4j-localespi-<version>.jar*). But the actual implementation of the service
classes and data are in the ICU4J core JAR file (*icu4j-<version>.jar*). So you
need to put the localespi JAR file along with the core JAR file in the Java
extension directory.

Once the ICU4J Locale Service Provider is installed properly, factory methods in
JDK internationalization classes look for the implementation provided by ICU4J
when a requested locale is not supported by the JDK service class. For example,
locale *af_ZA* (Afrikaans - South Africa) is not supported by JDK `DateFormat` in
Oracle Java SE 6. The following code snippet returns an instance of `DateFormat`
from ICU4J Locale Service Provider and prints out the current date localized for
af_ZA.

    DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, new Locale("af", "ZA"));
    System.out.println(df.format(new Date()));

Sample output:

*   `2008 Junie 19` \[With ICU4J Locale Service Provider enabled\]
*   `June 19, 2008` \[Without ICU4J Locale Service Provider\]

## Optional Configuration

### Enabling or disabling individual service

By default, all Java 6 SE locale sensitive service providers are enabled in the
ICU4J Locale Service Provider JAR file. If you want to disable specific
providers supported by ICU4J, you can remove the corresponding provider
configuration files from *META-INF/services* in the localespi JAR file. For
example, if you do not want to use ICU's time zone name service at all, you can
remove the file: *META-INF/services/java.util.spi.TimeZoneNameProvider* from the
JAR file.

**Note:** Disabling `DateFormatSymbolsProvider/DecimalFormatSymbolsProvider` won't
affect the localized symbols actually used by `DateFormatProvider/NumberFormatProvider`
by the current implementation. These services are implemented independently.

### Configuring the behavior of ICU4J Locale Service Provider

*com/ibm/icu/impl/javaspi/ICULocaleServiceProviderConfig.properties* in the
localespi JAR file is used for configuring the behavior of the ICU4J Locale
Service Provider implementation. There are some configuration properties
available. See the table below for each configuration in detail.

|**Property**|**Value**|**Default**|**Description**|
|:---|:---:|:---:|:---|
|`com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.enableIcuVariants`|`"true"` or `"false"`|`"true"`|Whether if Locales with ICU's variant suffix will be included in `getAvailableLocales`. The current Java SE 6 locale sensitive service does not allow user provided provider implementations to override locales supported by JRE itself. When this property is `"true"` (default), ICU4J Locale Service Provider includes Locales with the suffix (`com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.icuVariantSuffix`) in the variant field. For example, the ICU4J provider includes locales fr_FR and fr_FR_ICU4J in the available locale list. So JDK API user can still access the internationalization service object created by the ICU4J provider by the special locale fr_FR_ICU4J|
|`com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.icuVariantSuffix`|*Any String*|`"ICU4J"` (49 or later) `"ICU"` (before 49)|Suffix string used in Locale's variant field to specify the ICU implementation.|
|`com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.enableIso3Languages`|`"true"` or `"false"`|`"true"`|Whether if 3-letter language locales are included in `getAvailabeLocales`. Use of 3-letter language codes in `java.util.Locale` is not supported by the API reference document. However, the implementation does not check the length of language code, so there is no practical problem with it.|
|`com.ibm.icu.impl.javaspi.ICULocaleServiceProvider.useDecimalFormat`|`"true"` or `"false"`|`"false"`|Whether if `java.text.DecimalFormat` subclass is used for `NumberFormat#getXXXInstance`. `DecimalFormat#format(Object,StringBuffer,FieldPosition)` is declared as final, so ICU cannot override the implementation. As a result, some number types such as `BigInteger`/`BigDecimal` are not handled by the ICU implementation. If a client expects `NumberFormat#getXXXInstance` returns a `DecimalFormat` (for example, need to manipulate decimal format patterns), he/she can set true to this setting. However, in this case, `BigInteger`/`BigDecimal` support is not done by ICU's implementation.|
