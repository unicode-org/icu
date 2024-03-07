---
layout: default
title: Demos
nav_order: 350
description: Demos
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Demos

## ICU4C Demos

[List of ICU Demonstrations](https://icu4c-demos.unicode.org/icu-bin/icudemos)

## ICU4J Demos

### Server Side Demos

#### Web Demos

These demos are running on the ICU server, and are implemented as Java Servlets
and JSP pages.

*   [Browse the Demos](http://demo.icu-project.org/icu4jweb/)
*   [View Demo Source](https://github.com/unicode-org/icu-demos/tree/master/icu4jweb/)

### Client Side demos

#### To build the client side samples:

1.  Download the ICU4J source code ( see [Source Code Setup](../devsetup/source) )
2.  Run `ant jar` to build ICU4J jar
3.  Run `ant jarDemos` to build the demos
4.  Run `cp icu4j.jar demos/out/lib`
5.  Finally, run `java -jar demos/out/lib/icu4j-demos.jar` to launch the demos

**CalendarApp** This demo compares two calendars against each other. Choose the
two calendar types, and the display language, from the pop-up menus. Navigate by
days using the < and > buttons, or by years using the << and >> buttons.

**Translit** This demonstration shows ICU Transliteration. The transliteration
mode chosen in the menu will be used as you type.

**HolidayCalendarDemo** This demo displays holidays from a certain locale,
localized into the display language of your choice. Navigate by days using the <
and > buttons, or by years using the << and >> buttons.

**RbnfDemo** This demo shows Rule Based Number Formatting. Please expand the
window to show the entire demo. A number may be entered in the top left corner,
or the navigation buttons may be used. The pop-up menus in the top right corner
will pick the rule and the variant used.

**DetectingViewer** By opening a document using the Open file or Open URL menu
items, this demo will statistically detect the probable file encoding of a file.
Use the DetectedEncodings menu to see which encodings were detected.

*Note:* Due to security constraints, you must use the Downloadable Demo Jar in
order to use these demos with files on your local disk. The Java Web Start
application will not have permission to read local files.

---

### ICU Introduction Applets

#### About the Applets

This is a paper introducing ICU calendars, which has live applets throughout the
text to demonstrate various features.

The paper is now archived, see <https://github.com/unicode-org/icu-demos/pull/5>
