---
layout: default
title: ICU4J FAQ
nav_order: 7
parent: Misc
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4J FAQ
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

# Overview

This page contains frequently asked questions about the content provided with
the International Components for Unicode for Java as well as basics on
internationalization. It is organized into the following sections:

### Common Questions

#### What version of Java is required for ICU4J?

ICU4J 4.4 or later versions utilize Java 5 language features and only run on JRE
5 or later. The ICU4J Locale SPI module depends on JDK 6 Locale Service Provider
framework, therefore, it requires JRE 6 or later.

#### Comparison between ICU and JDK: What's the difference?

This is one of our most popular question. Please refer to [our comparison
chart](http://icu-project.org/charts/comparison/).

#### How can I get the version information of ICU4J library on my system?

You can get the ICU4J version information by public API class
[com.ibm.icu.util.VersionInfo](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/VersionInfo.html).
The static field
[VersionInfo.ICU_VERSION](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/VersionInfo.html#ICU_VERSION)
contains the current ICU4J library version information.
Since ICU4J 4.6, ICU4J jar file includes Main-Class that prints out the ICU
version information like below:

```
$ java -jar icu4j.jar
International Component for Unicode for Java 4.8

Implementation Version: 4.8
Unicode Data Version: 6.0
CLDR Data Version: 2.0
Time Zone Data Version: 2011g
```

#### I'm using ICU4J X, but planning to upgrade ICU4J version to X+1 soon. What should I do for the migration?

See the user guide section
[Version Numbers in ICU](../design#version-numbers-in-icu)
for the details about the meaning of the version number parts and how the ICU
version number changes.

In general, two different reference releases are not binary compatible (i.e.
drop-in jar file replacement would not work). To use a new reference version of
ICU4J, you should rebuild your application with the new ICU4J library. ICU
project has the
[API compatibility policy](../design#icu-api-compatibility)
long as you're using ICU APIs marked as @stable in the API reference
documentation, your application should successfully compile with the new
reference version of ICU4J library without any source code modifications. (Note:
ICU project team may retract APIs previously marked as @stable by well-defined
process. But this is a very rare case.) However, you might still need to review
the usage of ICU4J APIs especially when your application set a certain
assumption on the behavior of APIs driven by Unicode or locale data. For
example, a date format pattern used for locale X might not be exactly the same
with the pattern in a new version.

#### How can I see all API changes between two different ICU versions?

For every ICU4J release, we publish
[APIChangeReport.html](https://htmlpreview.github.io/?https://github.com/unicode-org/icu/blob/master/icu4j/APIChangeReport.html)
which captures all API changes since previous reference release.
However, someone may want to see the changes between the
current release and much older ICU4J version. For example, you're currently
using ICU4J 60 and considering to upgrade to ICU4J 64. In this case, you can
generate a change report page by following steps.

1.  Download [ICU4J 64 source package
    archive](http://site.icu-project.org/download/64#TOC-ICU4J-Download)
    from the ICU 64 download page and extract files to your local system.
2.  Set up ICU4J build environment as explained in
    [readme.html](https://htmlpreview.github.io/?https://github.com/unicode-org/icu/blob/master/icu4j/readme.html)
    included in the root directory of the ICU4J source package archive.
3.  Edit
    [build.properties](https://github.com/unicode-org/icu/blob/master/icu4j/build.properties)
    in the root directory and change the property value api.report.prev.version
    from 63 to 60.
4.  Invoke ant target "apireport".
5.  The output is generated at out/icu4j_compare_60_64.html.

### International Calendars

#### Why do I need these classes?

If your application displays or manipulates dates and times, and if you want
your application to run in countries outside of North America and western
Europe, you need to support the traditional calendar systems that are still in
use in some parts of the world. These classes provide that support while
conforming to the standard Java Calendar API, allowing you to code your
application once and have it work with any international calendar.

#### Which Japanese calendar do you support?

Currently, our JapaneseCalendar is almost identical to the Gregorian calendar,
except that it follows the traditional conventions for year and era names. In
modern times, each emperor's reign is treated as an era, and years are numbered
from the start of that era. Historically each emperor's reign would be divided
up into several eras, or *gengou*. Currently, our era data extends back to
*Haika*, which began in 645 AD. In all other respects (month and date, all of
the time fields, etc.) the JapaneseCalendar class will give results that are
identical to GregorianCalendar.

Lunar calendars similar to the Chinese calendar have also been used in Japan
during various periods in history, but according to our sources they are not in
common use today. If you see a real need for a Japanese lunar calendar, and
especially if you know of any good references on how it differs from the Chinese
calendar, please let us know by posting a note on the [mailing
list](http://icu-project.org/contacts.html).

#### Do you *really* support the true lunar Islamic calendar?

The Islamic calendar is strictly lunar, and a month begins at the moment when
the crescent of the new moon is visible above the horizon at sunset. It is
impossible to calculate this calendar in advance with 100% accuracy, since moon
sightings are dependent on the location of the observer, the weather, the
observer's eyesight, and so on. However, there are fairly commonly-accepted
criteria (the angle between the sun and the moon, the moon's angle above the
horizon, the position of the moon's bright limb, etc.) that let you predict the
start of any given month with a very high degree of accuracy, except of course
for the weather factor. We currently use a fairly crude approximation that is
still relatively accurate, corresponding with the official Saudi calendar for
all but one month in the last 40-odd years. This will be improved in future
versions of the class.

What all this boils down to is that the IslamicCalendar class does a fairly good
job of predicting the Islamic calendar, and it is good enough for most
computational purposes. However, for religious purposes you should, of course,
consult the appropriate mosque or other authority.

### TimeZone

#### Does ICU4J have its own time zone rule data?

Yes. ICU4J library contains time zone rule data generated from the [tz
database](https://www.iana.org/time-zones).

#### Why does ICU4J carry the time zone rule data while my JRE also has the data?

There are several reasons. Bundling our own time zone data allow us to provide
quick updates to users. ICU project team usually release the latest time zone
rule data patch as soon as the new tz database release is published (usually
within 1 to 3 days). Having our own rule data also allows the ICU4J library to
provide some advanced TimeZone features (see [com.ibm.icu.util.BasicTimeZone API
documentation](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/BasicTimeZone.html)).

#### How can I get the latest time zone rule data patch?

You can use [ICU4J Time Zone Update
Utility](http://site.icu-project.org/download/icutzu) to update the time zone
rule data to the latest.

#### I do not want to maintain yet another time zone rule data. Are there any way to configure ICU4J to use the JRE's time zone data?

If you do not use the advanced TimeZone features, then you can configure ICU4J
to use JRE's time zone support by editing ICUConfig.properties (included in
ICU4J library jar file) or simply setting a system property. See
[com.ibm.icu.util.TimeZone API
documentation](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/TimeZone.html)
for the details.

### StringSearch

#### Do I have to know anything about Collators to use StringSearch?

Since StringSearch uses a RuleBasedCollator to handle the language-sensitive
aspects of searching, understanding how collation works certainly helps. But the
only parts of the Collator API that you really need to know about are the
collation strength values, `PRIMARY`, `SECONDARY`, and `TERTIARY`, that
determine whether case and accents are ignored during a search.

#### What algorithm are you using to perform the search?

StringSearch uses a version of the Boyer-Moore search algorithm that has been
modified for use with Unicode. Rather than using raw Unicode character values in
its comparisons and shift tables, the algorithm uses collation elements that
have been "hashed" down to a smaller range to make the tables a reasonable size.

### RuleBasedBreakIterator

#### Why did you bother to rewrite BreakIterator? Wasn't the old version working?

It was working, but we were too constrained by the design. The break-data tables
were hard-coded, and there was only one set of them. This meant you couldn't
customize BreakIterator's behavior, nor could we accommodate languages with
mutually-exclusive breaking rules (Japanese and Chinese, for example, have
different word-breaking rules.) The hard-coded tables were also very
complicated, difficult to maintain, and easy to mess up, leading to mysterious
bugs. And in the original version, there was no way to subclass BreakIterator
and get any implementation at all -- if you wanted different behavior, you had to
rewrite the whole thing from scratch. We undertook this project to fix all these
problems and give us a better platform for future development. In addition, we
managed to get some significant performance improvements out of the new version.

#### What do you mean, performance improvements? It seems WAY slower to me!

The one thing that's significantly slower is construction. This is because it
actually builds the tables at runtime by parsing a textual description. In the
old version, the tables were hard-coded, so no initialization was necessary. If
this is causing you trouble, it's likely that you're creating and destroying
BreakIterators too frequently. For example, if you're writing code to word-wrap
a document in a text editor, and you create and destroy a new BreakIterator for
every line you process, performance will be unbelievably slow. If you move the
creation out of the inner loop and create a new BreakIterator only once per
word-wrapping operation, or once per document, you'll find that your performance
improves dramatically. If you still have problems after doing this, let us
know -- there may be bugs we need to fix.

#### This still has all the same bugs that the old BreakIterator did! Why would I want to use this one instead?

Because now you can fix it. The resource data in this package was designed to
mimic as closely as possible the behavior of the original BreakIterator class
(as of JDK 1.2). We did this deliberately to minimize our variables when making
sure the new iterator still passed all the old tests. We haven't updated it
since to avoid the bookkeeping hassles of keeping track of which version
includes which fixes. We're hoping to get this added to a future version of the
JDK, at which time we'll fix all the outstanding bugs relating to breaking in
the wrong places. In the meantime, you can customize the resource data to modify
things to work the way you want them to.

#### Why is there no demo?

We haven't had time to write a good demo for this new functionality yet. We'll
add one later.

#### What's this DictionaryBasedBreakIterator thing?

This is a new feature that isn't in the JDK. DictionaryBasedBreakIterator is
intended for use with languages that don't put spaces between words (such as
Thai), or for languages that do put spaces between words, but often combine lots
of words into long compound words (such as German). Instead of looking through
the text for sequences of characters that signal the end of a word, it compares
the text against a list of known words, using this to determine where the
boundaries should go. The algorithm we use for this is fast, accurate, and
error-tolerant.

#### Why do you have a Thai dictionary, but no resource data that actually lets me use it?

We're not quite done doing the necessary research. We don't currently have good
test cases we can use to verify it's working correctly with Thai, nor are we
completely confident in our dictionary. If you can help us with this, we'd like
to hear from you!

#### What's this BreakIteratorRules_en_US_TEST thing?

This is a resource file that, in conjunction with the "english.dict" dictionary,
we used to test the dictionary-based break iterator. It allows you to locate
word boundaries in English text that has had the spaces taken out. (The
SimpleBITest program demonstrates this.) The dictionary isn't
industrial-strength, however: we included enough words to make for a reasonable
test, but it's by no means complete or anywhere near it.

#### How can I create my own dictionary file?

Right now, you can't. We didn't include the tool we used to create dictionary
files because it's very rough and extremely slow. There's also a strong
likelihood that the format of the dictionary files will change in the future. If
you really want to create your own dictionary file, contact us, and we'll see
what we can do.
