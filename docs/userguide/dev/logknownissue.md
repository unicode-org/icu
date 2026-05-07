---
layout: default
title: Skipping Known Test Failures
parent: Contributors
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Skipping Known Test Failures (logKnownIssue)

If you need a test to be disabled temporarily, call `logKnownIssue`. The method
is defined as below:

```java
/**
* Log the known issue.
* This method returns true unless -prop:logKnownIssue=no is specified
* in the argument list.
*
* @param ticket A ticket number string. For an ICU ticket, use numeric
characters only,
* such as "10245". For a CLDR ticket, use prefix "cldrbug:" followed by ticket
number,
* such as "cldrbug:5013".
* @param comment Additional comment, or null
* @return true unless -prop:logKnownIssue=no is specified in the test command
line argument.
*/
public boolean logKnownIssue(String ticket, String comment)
```

Below is an example:

```java
if (logKnownIssue("1234", "New data is not integrated yet.")) {
    return;
}

// test code below
```

By default, logKnownIssue returns true and emit a log line including the link to
the ticket and the comment.

When `-prop:logKnownIssue=no` is specified as a command line argument,
`logKnownIssue()` returns false, so you can temporary enable a test code skipped
by logKnownIssue.

Before ICU4J 52, we used to use isICUVersionBefore() method like below. The test
method is still available in the trunk, but developers are suggested to use
logKnownIssue() instead.

```java
if (isICUVersionBefore(50,0,2)) {
    return;
}
```

Before ICU4J 49M2, we used to use the style below -

```java
if(skipIfBeforeICU(4, 5, 2)) {
    return;
}
```
