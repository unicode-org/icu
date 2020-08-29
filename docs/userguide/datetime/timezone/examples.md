---
layout: default
title:  Date and Time Zone Examples
nav_order: 4
parent: Date/Time
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Date and Time Zone Examples
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## C++ TimeZone example code

This example code illustrates some time zone operations.

```c++
 UErrorCode success = U_ZERO_ERROR;
 UnicodeString dateReturned, curTZNameEn, curTZNameFr;
 UDate curDate;
 int32_t stdOffset,dstOffset;

 // Create a Time Zone with America/Los_Angeles
 TimeZone *tzWest = TimeZone::createTimeZone("America/Los_Angeles");


// Print out the Time Zone Name, GMT offset etc.
 curTZNameEn = tzWest->getDisplayName(Locale::getEnglish(),curTZNameEn);
 u_printf("%s\n","Current Time Zone Name in English:");
 u_printf("%S\n", curTZNameEn.getTerminatedBuffer());

 curTZNameFr = tzWest->getDisplayName(Locale::getCanadaFrench(),curTZNameFr);
 u_printf("%s\n","Current Time Zone Name in French:");
 u_printf("%S\n", curTZNameFr.getTerminatedBuffer());


 // Create a Calendar to get current date
 Calendar* calendar = Calendar::createInstance(success);
 curDate = calendar->getNow();


 // Print out the Current Date/Time in the given time zone
 DateFormat *dt = DateFormat::createDateInstance();
 dateReturned = dt->format(curDate,dateReturned,success);
 u_printf("%s\n", "Current Time:");
 u_printf("%S\n", dateReturned.getTerminatedBuffer());


 // Use getOffset to get the stdOffset and dstOffset for the given time
 tzWest->getOffset(curDate,true,stdOffset,dstOffset,success);
 u_printf("%s\n%d\n","Current Time Zone STD offset:",stdOffset/(1000*60*60));
 u_printf("%s\n%d\n","Current Time Zone DST offset:",dstOffset/(1000*60*60));
 u_printf("%s\n", "Current date/time is in daylight savings time?");
 u_printf("%s\n", (calendar->inDaylightTime(success))?"Yes":"No");


 // Use createTimeZoneIDEnumeration to get the specific Time Zone IDs
 // in United States with -5 hour standard offset from GMT
 stdOffset = (-5)*U_MILLIS_PER_HOUR; // U_MILLIS_PER_HOUR = 60*60*1000;
 StringEnumeration *ids = TimeZone::createTimeZoneIDEnumeration(UCAL_ZONE_TYPE_CANONICAL_LOCATION,"US",&stdOffset,success);
 for (int i=0; i<ids->count(success);i++) {
   u_printf("%s\n",ids->next(NULL,success));
 }


 // Use Calendar to get the hour of the day for different time zones
 int32_t hour1,hour2;
 TimeZone *tzEast = TimeZone::createTimeZone("America/New_York");
 Calendar * cal1 = Calendar::createInstance(tzWest,success);
 Calendar * cal2 = Calendar::createInstance(tzEast,success);
 hour1 = cal1->get(UCAL_HOUR_OF_DAY,success);
 hour2 = cal2->get(UCAL_HOUR_OF_DAY,success);
 u_printf("%s\n%d\n","Current hour of the day in North American West: ", hour1);
 u_printf("%s\n%d\n","Current hour of the day in North American East: ", hour2);

 delete cal1;
 delete cal2;
 delete ids;
 delete calendar;
 delete dt;
 ```
