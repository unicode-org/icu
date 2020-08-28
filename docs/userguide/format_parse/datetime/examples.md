---
layout: default
title: Date and Time Formatting Examples
nav_order: 1
grand_parent: Formatting
parent: Formatting Dates and Times
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Date and Time Formatting Examples
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Format

The ICU DateFormat interface enables you to format a date in milliseconds into a
string representation of the date. Also, the interface enables you to parse the
string back to the internal date representation in milliseconds.

### C++

```cpp
DateFormat* df = DateFormat::createDateInstance();
UnicodeString myString;
UDate myDateArr[] = { 0.0, 100000000.0, 2000000000.0 }; 
for (int32_t i = 0; i < 3; ++i) {
  myString.remove();
  cout << df->format( myDateArr[i], myString ) << endl;
}
```

### C

```c
/* 1st example: format the dates in millis 100000000 and 2000000000 */
UErrorCode status=U_ZERO_ERROR;
int32_t i, myStrlen=0;
UChar* myString;
UDate myDateArr[] = { 0.0, 100000000.0, 2000000000.0 }; // test values
UDateFormat* df = udat_open(UCAL_DEFAULT, UCAL_DEFAULT, NULL, "GMT", &status);
for (i = 0; i < 3; ++i) {
  myStrlen = udat_format(df, myDateArr[i], NULL, myStrlen, NULL, &status);
  if(status==U_BUFFER_OVERFLOW_ERROR){
    status=U_ZERO_ERROR;
    myString=(UChar*)malloc(sizeof(UChar) * (myStrlen+1) );
    udat_format(df, myDateArr[i], myString, myStrlen+1, NULL, &status);
    printf("%s\n", austrdup(myString) ); 
    /* austrdup( a function used to convert UChar* to char*) */
    free(myString);
  }
}
```

## Parse

To parse a date for a different locale, specify it in the locale call. This call
creates a formatting object.

### C++

```cpp
DateFormat* df = DateFormat::createDateInstance
  ( DateFormat::SHORT, Locale::getFrance());
```

### C

```c
/* 2nd example: parse a date with short French date/time formatter */
UDateFormat* df = udat_open(UDAT_SHORT, UDAT_SHORT, "fr_FR", "GMT", &status);
UErrorCode status = U_ZERO_ERROR;
int32_t parsepos=0;     
UDate myDate = udat_parse(df, myString, u_strlen(myString), &parsepos,
&status);
```

### Java

```java
import java.text.FieldPosition;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.text.DateFormat;

public class TestDateTimeFormat {
    public void run() {

        // Formatting Dates

        DateFormat dfUS = DateFormat.getDateInstance(DateFormat.FULL, Locale.US);
        DateFormat dfFrance = DateFormat.getDateInstance(DateFormat.FULL, Locale.FRANCE);
        StringBuffer sb = new StringBuffer();
        Calendar c = Calendar.getInstance();
        Date d = c.getTime();
        sb = dfUS.format(d, sb, new FieldPosition(0));
        System.out.println(sb.toString());

        StringBuffer sbf = new StringBuffer();
        sbf = dfFrance.format(d, sbf, new FieldPosition(0));
        System.out.println(sbf.toString());

        StringBuffer sbg = new StringBuffer();
        DateFormat dfg = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);
        FieldPosition pos = new FieldPosition(DateFormat.MINUTE_FIELD);
        sbg = dfg.format(d, sbg, pos);
        System.out.println(sbg.toString());
        System.out.println(sbg.toString().substring(pos.getBeginIndex(), pos.getEndIndex()));

        // Parsing Dates

        String dateString_US = "Thursday, February 7, 2008";
        String dateString_FRANCE = "jeudi 7 février 2008";
        try {
            Date parsedDate_US = dfUS.parse(dateString_US);
            Date parsedDate_FRANCE = dfFrance.parse(dateString_FRANCE);
            System.out.println(parsedDate_US.toString());
            System.out.println(parsedDate_FRANCE.toString());
        } catch (ParseException pe) {
            System.out.println("Exception while parsing :" + pe);
        }
    }

    public static void main(String args[]) {
        new TestDateTimeFormat().run();
    }
}
```

## Getting Specific Date Fields

To get specific fields of a date, you can use the FieldPosition function for C++
or UFieldPosition function for C.

### C++

```cpp
UErrorCode status = U_ZERO_ERROR;
FieldPosition pos(DateFormat::YEAR_FIELD)
UDate myDate = Calendar::getNow();
UnicodeString str;
DateFormat* df = DateFormat::createDateInstance
  ( DateFormat::LONG, Locale::getFrance());

df->format(myDate, str, pos, status);
cout << pos.getBeginIndex() << "," << pos. getEndIndex() << endl;
```

### C

```c
UErrorCode status = U_ZERO_ERROR;
UFieldPosition pos;
UChar *myString;
int32_t myStrlen = 0;
char buffer[1024];


pos.field = 1; /* Same as the DateFormat::EField enum */
UDateFormat* dfmt = udat_open(UCAL_DEFAULT, UCAL_DEFAULT, NULL, "PST",
&status);
myStrlen = udat_format(dfmt, myDate, NULL, myStrlen, &pos, &status);
if (status==U_BUFFER_OVERFLOW_ERROR){
    status=U_ZERO_ERROR;
    myString=(UChar*)malloc(sizeof(UChar) * (myStrlen+1) );
    udat_format(dfmt, myDate, myString, myStrlen+1, &pos, &status);
}
printf("date format: %s\n", u_austrcpy(buffer, myString));
buffer[pos.endIndex] = 0;   // NULL terminate the string.
printf("UFieldPosition position equals %s\n", &buffer[pos.beginIndex]);
```

## DateTimePatternGenerator

This class lets you get a different variety of patterns, such as month+day. The
following illustrates this in Java, C++ and C.

### Java

```java
// set up the generator
DateTimePatternGenerator generator
    = DateTimePatternGenerator.getInstance(locale);

// get a pattern for an abbreviated month and day
final String pattern = generator.getBestPattern("MMMd");
SimpleDateFormat formatter = new SimpleDateFormat(pattern, locale);

// use it to format (or parse)
String formatted = formatter.format(new Date());
// for French, the result is "13 sept."
```

### C++

```cpp
// set up the generator
status = U_ZERO_ERROR;
DateTimePatternGenerator *generator = DateTimePatternGenerator::createInstance( locale, status);
if (U_FAILURE(status)) {
    return;
}
    
// get a pattern for an abbreviated month and day
UnicodeString pattern = generator->getBestPattern(UnicodeString("MMMd"), status); 
SimpleDateFormat *formatter = new SimpleDateFormat(pattern, locale, status); 

// use it to format (or parse)
UnicodeString formatted;
formatted = formatter->format(Calendar::getNow(), formatted, status); 
// for French, the result is "13 sept."
```

### C

```c
const UChar skeleton[]= {'M', 'M', 'M', 'd', 0};

status=U_ZERO_ERROR;    
generator=udatpg_open(locale, &status);
if(U_FAILURE(status)) {
    return;

}

/* get a pattern for an abbreviated month and day */
length = udatpg_getBestPattern(generator, skeleton, 4,
                                pattern, patternCapacity, &status);
formatter = udat_open(UDAT_IGNORE, UDAT_DEFAULT, locale, NULL, -1, 
                        pattern, length, &status);

/* use it to format (or parse) */
formattedCapacity = (int32_t)(sizeof(formatted)/sizeof((formatted)[0]));
resultLen=udat_format(formatter, ucal_getNow(), formatted, formattedCapacity,
                        NULL, &status);
/* for French, the result is "13 sept." */
```

## Changing the TimeZone Formatting Style

It also contains some helper functions for parsing patterns. Here's an example
of replacing the kind of timezone used in a pattern.

### Java

```cpp
/**
 * Replace the zone string with a different type, eg v's for z's, etc.
 * <p>Called with a pattern, such as one gotten from 
 * <pre>
 * String pattern = ((SimpleDateFormat)
 * DateFormat.getTimeInstance(style, locale)).toPattern();
 * </pre>
 * @param pattern original pattern to change, such as "HH:mm zzzz"
 * @param newZone Must be: z, zzzz, Z, ZZZZ, v, vvvv, V, or VVVV
 * @return
 */
public String replaceZoneString(String pattern, String newZone) {
    DateTimePatternGenerator.FormatParser formatParser =
        new DateTimePatternGenerator.FormatParser();
    final List itemList = formatParser.set(pattern).getItems();
    boolean found = false;
    for (int i = 0; i < itemList.size(); ++i) {
        Object item = itemList.get(i);
        if (item instanceof VariableField) {
            // the first character of the variable field determines the type,
            // according to CLDR.
            String variableField = item.toString();
            switch (variableField.charAt(0)) {
            case 'z': case 'Z': case 'v': case 'V':
                if (!variableField.equals(newZone)) {
                    found = true;
                    itemList.set(i, new VariableField(newZone));
                }
                break;
            }
        }
    }
    return found ? formatParser.toString() : pattern;
}
```
