---
layout: default
title: MessageFormat 2.0
nav_order: 2
parent: Formatting Messages
grand_parent: Formatting
---
<!--
© 2023 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# MessageFormat 2.0
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

*Note: This page describes `MessageFormatter`, which is a Technical Preview API implementing
MessageFormat 2.0.
It will be a successor to the current [ICU MessageFormat](index.md).
MessageFormat 2.0 is being developed
[in a working group](https://github.com/unicode-org/message-format-wg),
which has created a [draft specification](https://github.com/unicode-org/message-format-wg/tree/main/spec).
Also see the 
[API docs for `MessageFormatter`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/index.html?com/ibm/icu/message2/MessageFormatter.html).*

## Overview of `MessageFormatter`

In ICU4J, the `MessageFormatter` class is the next iteration of [MessageFormat](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/MessageFormat.html).
This new version will build on the lessons learned from using MessageFormat for 25 years in various environments, when used directly or as a base for other public APIs.

The effort to design a succesor to `MessageFormat` will result in a specification referred to as MessageFormat 2.0.
The reasoning for this effort is shared in the [“Why MessageFormat needs a successor”](https://github.com/unicode-org/message-format-wg/blob/main/docs/why_mf_next.md) document.

MessageFormat 2.0 will be more modular and easier to port and backport.
It will also provide extension points via interfaces to allow users to supply new formatters and selectors without having to modify the specification.
ICU will eventually include support for new formatters, such as intervals, relative time, lists, measurement units, personal names, and more, as well as the ability for users to supply their own custom implementations.
These will potentially support use cases like grammatical gender, inflection, markup regimes (such as those require for text-to-speech), and other complex message management needs.

The MessageFormat Working Group, which develops the new data model, semantics, and syntax, is hosted on [GitHub](https://github.com/unicode-org/message-format-wg).
The current specification for the syntax and data model can be found [here](https://github.com/unicode-org/message-format-wg/blob/main/spec/syntax.md).

This technical preview implements enough functions for `MessageFormater` to be useful in many situations,
but the final set of functions and the parameters accepted by those functions is not yet finalized.

## Examples

### Basic usage

```java
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.message2.MessageFormatter;

@Test
public void testMf2() {
    final Locale enGb = Locale.forLanguageTag("en-GB");
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("name", "John");
    arguments.put("exp", new Date(1679971371000L));  // March 27, 2023, 7:42:51 PM

    MessageFormatter mf2 = MessageFormatter.builder()
        .setPattern("{Hello {$name}, your card expires on {$exp :datetime skeleton=yMMMdE}!}")
        .setLocale(enGb)
        .build();

    assertEquals(
        "Hello John, your card expires on Mon, 27 Mar 2023!",
        mf2.formatToString(arguments));
}
```

### Placeholder examples

| Code to set runtime value for placeholder          | Examples of placeholder in message pattern                                   |
|----------------------------------------------------|------------------------------------------------------------------------------|
| `arguments.put("name", "John")`                    | `{$name}`                                                                    |
| `arguments.put("exp", new Date(…))`                | `{$exp :datetime skeleton=yMMMdE}` <br/> `{$exp :datetime datestyle=full}` |
| `arguments.put("val", 3.141592653)`                | `{$val}` <br/> `{$val :number skeleton=(.####)}`                             |
| No argument for fixed values known at build time   | `{(123456789.531) :number}`                                                  |


### Plural selection message

```java
@Test
public void testMf2Selection() {
   final String message = "match {$count :plural}"
           + " when 1 {You have one notification.}"
           + " when one {You have {$count} notification.}"
           + " when * {You have {$count} notifications.}";
   final Locale enGb = Locale.forLanguageTag("en-GB");
   Map<String, Object> arguments = new HashMap<>();


   MessageFormatter mf2 = MessageFormatter.builder()
       .setPattern(message)
       .setLocale(enGb)
       .build();


   arguments.put("count", 1);
   assertEquals(
       "You have one notification.",
       mf2.formatToString(arguments));


   arguments.put("count", 42);
   assertEquals(
       "You have 42 notifications.",
       mf2.formatToString(arguments));
}
```

### Built-in formatter functions

The tech preview implementation comes with formatters for numbers (`number`), 
date / time (`datetime`), 
plural selectors (`plural` and `selectordinal`),
and general selector (`select`), 
very similar to what MessageFormat offers.

The [ICU test code](https://github.com/unicode-org/icu/tree/main/icu4j/main/tests/core/src/com/ibm/icu/dev/test/message2)
covers most features, and has examples of how to make custom placeholder formatters;
you can look for classes that implement `com.ibm.icu.message2.FormatterFactory`
(they are named `Custom*Test.java`).

## Functions currently implemented

These are the functions implemented right now:


<table border="1">
<tr>
  <td rowspan="4"><code>datetime</code></td>
  <td>Similar to MessageFormat's <code>date</code> and <code>time</code>.</td>
</tr>
  <tr><td><code>datestyle</code> and <code>timestyle</code><br>
  Similar to <code>argStyle : short | medium | long | full</code>.<br>
  Same values are accepted, but we can use both in one placeholder,
  for example <code>{$due :datetime datestyle=full timestyle=long}</code>.
  </td></tr>
  <tr><td><code>pattern</code><br>
  Similar to <code>argStyle = argStyleText</code>.<br>
  This is bad i18n practice, and will probably be dropped.<br>
  This is included just to support migration to MessageFormat 2.
  </td></tr>
  <tr><td><code>skeleton</code><br>
  Same as <code>argStyle = argSkeletonText</code>.<br>
  These are the date/time skeletons as supported by <a href=""><code>com.ibm.icu.text.SimpleDateFormat</code></a>.
  </td></tr>
<tr>
  <td rowspan="4"><code>number</code></td>
  <td>Similar to MessageFormat's <code>number</code>.</td>
</tr>
  <tr><td><code>skeleton</code><br>
  These are the number skeletons as supported by <a href=""><code>com.ibm.icu.number.NumberFormatter</code></a>.</td></tr>
  <tr><td><code>minimumFractionDigits</code><br>
  Only implemented to be able to pass the unit tests from the ECMA tech preview implementation,
  which prefers options bags to skeletons.<br>
  TBD if the final function will support skeletons, option backs, or both.</td></tr>
  <tr><td><code>offset</code><br>
  Used to support plural with an offset.</td></tr>
<tr><td ><code>identity</code></td><td>Returns the direct string value of the argument (calling <code>toString()</code>).</td></tr>
<tr>
  <td rowspan="3"><code>plural</code></td>
  <td>Similar to MessageFormat's <code>plural</code>.</td>
</tr>
  <tr><td><code>skeleton</code><br>
  These are the number skeletons as supported by <a href=""><code>com.ibm.icu.number.NumberFormatter</code></a>.<br>
  Can also be indirect, from a local variable of type <code>number</code> (recommended).</td></tr>
  <tr><td><code>offset</code><br>
  Used to support plural with an offset.<br>
  Can also be indirect, from a local variable of type <code>number</code> (recommended).</td></tr>
<tr>
  <td><code>selectordinal</code></td>
  <td>Similar to MessageFormat's <code>selectordinal</code>.<br>
For now it accepts the same parameters as <code>plural</code>, although there is no use case for them.<br>
TBD if this will be merged into <code>plural</code> (with some <code>kind</code> option) or not.</td></tr>
<tr><td><code>select</code></td><td>Literal match, same as MessageFormat's <code>select</code>.</td></tr>
</table>

## Quickstart guide

If you don't have ICU set up, here are instructions for doing that using Maven or Gradle:

### Requirements

- JDK (version 8 or newer)
- Maven or Gradle
- Your preferred IDE or text editor

### Maven

#### Create a new project

```
$ mvn archetype:generate -DgroupId=org.unicode -DartifactId=mf2 -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4 -DinteractiveMode=false

$ cd mf2
```

#### Add a dependency to ICU4J 72.1 (or newer)

In the `pom.xml` find the `<dependencies>` element and add this:

```
<dependency>
    <groupId>com.ibm.icu</groupId>
    <artifactId>icu4j</artifactId>
    <version>72.1</version>
</dependency>
```

#### Add a bit of code

Open the test file (`src/test/java/org/unicode/AppTest.java`)
and copy / paste the include directives and the `testMf2()` method shown in the previous section.

#### Test it

```
$ mvn test
```

### Gradle

#### Create a new project

```
$ mkdir mf2

$ cd mf2

$ gradle init --dsl groovy --test-framework junit --type java-application --package org.unicode --project-name mf2
```

#### Add a dependency to ICU4J 72.1 (or newer)

In the `app/build.gradle` file, find the `dependencies {...}` section add this:

```
implementation 'com.ibm.icu:icu4j:72.1'
```
#### Add a bit of code

Open the test file (`src/test/java/org/unicode/AppTest.java`)
and copy / paste the include directives and the `testMf2()` method shown in the previous section.

#### Test it

```
$ gradle test
```

### Experiment from here

At this point you have a basic application using MessageFormat 2.

You can experiment with more messages using as inspiration:

- the [syntax document](https://github.com/unicode-org/message-format-wg/blob/main/spec/syntax.md)
- the official [ICU4J javadoc](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j//index.html?com/ibm/icu/message2/)
- the [ICU4J unit tests](https://github.com/unicode-org/icu/tree/main/icu4j/main/tests/core/src/com/ibm/icu/dev/test/message2)

You should be able to use your preferred IDE (Eclipse, IntelliJ, Visual Studio Code, more), use a different build system, etc.

