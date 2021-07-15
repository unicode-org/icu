---
layout: default
title: Calendar Examples
nav_order: 2
parent: Date/Time
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Calendar Examples
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Calendar for Default Time Zone

These C++, C , and Java examples get a Calendar based on the default time zone
and add days to a date.

**C++**

```c++
UErrorCode status = U_ZERO_ERROR;
GregorianCalendar* gc = new GregorianCalendar(status);
if (U_FAILURE(status)) {
    puts("Couldn't create GregorianCalendar");
    return;
}
// set up the date
gc->set(2000, Calendar::FEBRUARY, 26);
gc->set(Calendar::HOUR_OF_DAY, 23);
gc->set(Calendar::MINUTE, 0);
gc->set(Calendar::SECOND, 0);
gc->set(Calendar::MILLISECOND, 0);
// Iterate through the days and print it out.
for (int32_t i = 0; i < 30; i++) {
    // print out the date.  
    // You should use the DateFormat to properly format it
    printf("year: %d, month: %d (%d in the implementation), day: %d\n",
    gc->get(Calendar::YEAR, status),
    gc->get(Calendar::MONTH, status) + 1,
    gc->get(Calendar::MONTH, status),
    gc->get(Calendar::DATE, status));
    if (U_FAILURE(status)) {
        puts("Calendar::get failed");
        return;
    }
    // Add a day to the date
    gc->add(Calendar::DATE, 1, status);
    if (U_FAILURE(status)) {
        puts("Calendar::add failed");
        return;
    }
}
delete gc;
```

**C**

```c
UErrorCode status = U_ZERO_ERROR;
int32_t i;
UCalendar* cal = ucal_open(NULL, -1, NULL, UCAL_GREGORIAN, &status);
if (U_FAILURE(status)) {
    puts("Couldn't create GregorianCalendar");
    return;
}
// set up the date
ucal_set(cal, UCAL_YEAR, 2000);
ucal_set(cal, UCAL_MONTH, UCAL_FEBRUARY); /* FEBRUARY */
ucal_set(cal, UCAL_DATE, 26);
ucal_set(cal, UCAL_HOUR_OF_DAY, 23);
ucal_set(cal, UCAL_MINUTE, 0);
ucal_set(cal, UCAL_SECOND, 0);
ucal_set(cal, UCAL_MILLISECOND, 0);
// Iterate through the days and print it out.
for (i = 0; i < 30; i++) {
    // print out the date.
    // You should use the udat_* API to properly format it
    printf("year: %d, month: %d (%d in the implementation), day: %d\n",
           ucal_get(cal, UCAL_YEAR, &status),
           ucal_get(cal, UCAL_MONTH, &status) + 1,
           ucal_get(cal, UCAL_MONTH, &status),
           ucal_get(cal, UCAL_DATE, &status));
    if (U_FAILURE(status)) {
        puts("Calendar::get failed");
        return;
    }
    // Add a day to the date
    ucal_add(cal, UCAL_DATE, 1, &status);
    if (U_FAILURE(status)) {
        puts("Calendar::add failed");
        return;
    }
}
ucal_close(cal);
```

**Java**

```java
Calendar cal = new GregorianCalendar();
if (cal == null) {
    System.out.println("Couldn't create GregorianCalendar");
    return;
}
// set up the date
cal.set(Calendar.YEAR, 2000);
cal.set(Calendar.MONTH, Calendar.FEBRUARY); /* FEBRUARY */
cal.set(Calendar.DATE, 26);
cal.set(Calendar.HOUR_OF_DAY, 23);
cal.set(Calendar.MINUTE, 0);
cal.set(Calendar.SECOND, 0);
cal.set(Calendar.MILLISECOND, 0);
// Iterate through the days and print it out.
for (int i = 0; i < 30; i++) {
    // print out the date.
    System.out.println(" year: " + cal.get(Calendar.YEAR) + 
                       " month: " + (cal.get(Calendar.MONTH) + 1) +
                       " day : " + cal.get(Calendar.DATE)
    );
    cal.add(Calendar.DATE, 1);
}
```

## Converting dates between calendars

These C++, C , and Java examples demonstrates converting dates from one calendar
(Gregorian) to another calendar (Japanese).

**C++**

```c++
UErrorCode status = U_ZERO_ERROR;
UDate time;
Calendar *cal1, *cal2;
// Create a new Gregorian Calendar.
cal1 = Calendar::createInstance("en_US@calendar=gregorian", status);
if (U_FAILURE(status)) {
    printf("Error creating Gregorian calendar.\n");
    return;
}
// Set the Gregorian Calendar to a specific date for testing.
cal1->set(1980, UCAL_SEPTEMBER, 3);
// Display the date.
printf("Gregorian Calendar:\t%d/%d/%d\n",
        cal1->get(UCAL_MONTH, status) + 1,
        cal1->get(UCAL_DATE, status),
        cal1->get(UCAL_YEAR, status));
if (U_FAILURE(status)) {
    printf("Error getting Gregorian date.");
    return;
}
// Create a Japanese Calendar.
cal2 = Calendar::createInstance("ja_JP@calendar=japanese", status);
if (U_FAILURE(status)) {
    printf("Error creating Japnese calendar.\n");
    return;
}
// Set the date.
time = cal1->getTime(status);
if (U_FAILURE(status)) {
    printf("Error getting time.\n");
    return;
}
cal2->setTime(time, status);
if (U_FAILURE(status)) {
    printf("Error setting the date for Japanese calendar.\n");
    return;
}
// Set the timezone
cal2->setTimeZone(cal1->getTimeZone());
// Display the date.
printf("Japanese Calendar:\t%d/%d/%d\n",
        cal2->get(UCAL_MONTH, status) + 1,
        cal2->get(UCAL_DATE, status),
        cal2->get(UCAL_YEAR, status));
if (U_FAILURE(status)) {
    printf("Error getting Japanese date.");
    return;
}
delete cal1;
delete cal2;
```

**C**

```c
UErrorCode status = U_ZERO_ERROR;
UDate time;
UCalendar *cal1, *cal2;
// Create a new Gregorian Calendar.
cal1 = ucal_open(NULL, -1, "en_US@calendar=gregorian", UCAL_TRADITIONAL,
                 &status);
if (U_FAILURE(status)) {
    printf("Couldn't create Gregorian Calendar.");
    return;
}
// Set the Gregorian Calendar to a specific date for testing.
ucal_setDate(cal1, 1980, UCAL_SEPTEMBER, 3, &status);
if (U_FAILURE(status)) {
    printf("Error setting date.");
    return;
}
// Display the date.
printf("Gregorian Calendar:\t%d/%d/%d\n",
        ucal_get(cal1, UCAL_MONTH, &status) + 1,
        ucal_get(cal1, UCAL_DATE, &status),
        ucal_get(cal1, UCAL_YEAR, &status));
if (U_FAILURE(status)) {
    printf("Error getting Gregorian date.");
    return 1;
}
// Create a Japanese Calendar.
cal2 = ucal_open(NULL, -1, "ja_J@calendar=japanese", UCAL_TRADITIONAL, &status);
if (U_FAILURE(status)) {
    printf("Couldn't create Japanese Calendar.");
    return 1;
}
// Set the date.
time = ucal_getMillis(cal1, &status);
if (U_FAILURE(status)) {
    printf("Error getting time.\n");
    return;
}
ucal_setMillis(cal2, time, &status);
if (U_FAILURE(status)) {
    printf("Error setting time.\n");
    return;
}
// Display the date.
printf("Japanese Calendar:\t%d/%d/%d\n",
        ucal_get(cal2, UCAL_MONTH, &status) + 1,
        ucal_get(cal2, UCAL_DATE, &status),
        ucal_get(cal2, UCAL_YEAR, &status));
if (U_FAILURE(status)) {
    printf("Error getting Japanese date.");
    return;
}
ucal_close(cal1);
ucal_close(cal2);
```

**Java**

```java
Calendar cal1, cal2;
// Create a new Gregorian Calendar.
cal1 = new GregorianCalendar();
// Set the Gregorian Calendar to a specific date for testing.
cal1.set(1980, Calendar.SEPTEMBER, 3);
// Display the date.
System.out.println("Gregorian Calendar:\t" + (cal1.get(Calendar.MONTH) + 1) +
                    "/" +
                    cal1.get(Calendar.DATE) + "/" +
                    cal1.get(Calendar.YEAR));
// Create a Japanese Calendar.
cal2 = new JapaneseCalendar();
// Set the date and timezone
cal2.setTime(cal1.getTime());
cal2.setTimeZone(cal1.getTimeZone());
// Display the date.
System.out.println("Japanese Calendar:\t" + (cal2.get(Calendar.MONTH) + 1) +
                    "/" +
                    cal2.get(Calendar.DATE) + "/" +
                    cal2.get(Calendar.YEAR));
```
