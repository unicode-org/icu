---
layout: default
title: Message Formatting Examples
nav_order: 1
parent: Formatting Messages
grand_parent: Formatting
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Message Formatting Examples
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## `MessageFormat` Class

ICU's `MessageFormat` class can be used to format messages in a locale-independent
manner to localize the user interface (UI) strings.

### C++

```cpp

/* The strings below can be isolated into a resource bundle
* and retrieved dynamically
*/
#define LANGUAGE_NAMES "{0}<{1}languages {2}>\n"
#define LANG_ATTRIB   "{0}<language id=\"{1}\" >{2}</language>\n"
#define MONTH_NAMES "{0}<monthNames>\n"
#define END_MONTH_NAMES "{0}</monthNames>\n"
#define MONTH   "{0}<month id=\"{1}\">{2}</month>\n"
#define MONTH_ABBR "{0}<monthAbbr>\n"
#define END_MONTH_ABBR "{0}</monthAbbr>\n"

UnicodeString CXMLGenerator::formatString(UnicodeString& str,UnicodeString&
argument){
Formattable args[] ={ argument};
UnicodeString result;
MessageFormat format(str,mError);
FieldPosition fpos=0;
format.format(args,1, result,fpos,mError);
if(U_FAILURE(mError)) {
  return UnicodeString("Illegal argument");
}

return result;
}

void CXMLGenerator::writeLanguage(UnicodeString& xmlString){

UnicodeString *itemTags, *items;
char* key="Languages";
int32_t numItems;

if(U_FAILURE(mError)) {
  return;
}

mRBundle.getTaggedArray(key,itemTags, items, numItems, mError);
if(mError!=U_USING_DEFAULT_ERROR && U_SUCCESS(mError) &&
mError!=U_ERROR_INFO_START){

  Formattable args[]={indentOffset,"",""};
  xmlString= formatString(UnicodeString(LANGUAGE_NAMES),args,3);
  indentOffset.append("\t");
  for(int32_t i=0;i<numItems;i++){

    args[0] = indentOffset;
    args[1] =itemTags[i] ;
    args[2] = items[i] ;
    xmlString.append(formatString(UnicodeString(LANG_ATTRIB),args,3));
  }

  chopIndent();
  args[0]=indentOffset;
  args[1] =(UnicodeString(XML_END_SLASH));
  args[2] = "";
  xmlString.append(formatString(UnicodeString(LANGUAGE_NAMES),args,3));

  return;
}
mError=U_ZERO_ERROR;
xmlString.remove();
}


void CXMLGenerator::writeMonthNames(UnicodeString& xmlString){

int32_t lNum;
const UnicodeString* longMonths=
mRBundle.getStringArray("MonthNames",lNum,mError);
if(mError!=U_USING_DEFAULT_ERROR && mError!=U_ERROR_INFO_START && mError !=
U_MISSING_RESOURCE_ERROR){
  xmlString.append(formatString(UnicodeString(MONTH_NAMES),indentOffset));
  indentOffset.append("\t");
  for(int i=0;i<lNum;i++){
   char c;
   itoa(i+1,&c,10);
   Formattable args[]={indentOffset,UnicodeString(&c),longMonths[i]};
   xmlString.append(formatString(UnicodeString(MONTH),args,3));
  }
  chopIndent();
  xmlString.append(formatString(UnicodeString(END_MONTH_NAMES),indentOffset));
  mError=U_ZERO_ERROR;
  return;
}
xmlString.remove();
mError= U_ZERO_ERROR;
}
```

### C

```c

void msgSample1(){

    UChar *result, *tzID, *str;
    UChar pattern[100];
    int32_t resultLengthOut, resultlength;
    UCalendar *cal;
    UDate d1;
    UErrorCode status = U_ZERO_ERROR;
    str=(UChar*)malloc(sizeof(UChar) * (strlen("disturbance in force") +1));
    u_uastrcpy(str, "disturbance in force");
    tzID=(UChar*)malloc(sizeof(UChar) * 4);
     u_uastrcpy(tzID, "PST");
     cal=ucal_open(tzID, u_strlen(tzID), "en_US", UCAL_TRADITIONAL, &status);
     ucal_setDateTime(cal, 1999, UCAL_MARCH, 18, 0, 0, 0, &status);
     d1=ucal_getMillis(cal, &status);
     u_uastrcpy(pattern, "On {0, date, long}, there was a {1} on planet
{2,number,integer}");
     resultlength=0;
     resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern),
NULL,
resultlength, &status, d1, str, 7);
     if(status==U_BUFFER_OVERFLOW_ERROR){
         status=U_ZERO_ERROR;
         resultlength=resultLengthOut+1;
         result=(UChar*)realloc(result, sizeof(UChar) * resultlength);
         u_formatMessage( "en_US", pattern, u_strlen(pattern), result,
resultlength, &status, d1, str, 7);
     }
     printf("%s\n",austrdup(result) ); //austrdup( a function used to convert
UChar* to char*)
     free(tzID);
     free(str);
     free(result);
}

char *austrdup(const UChar* unichars)

{
    int   length;
    char *newString;

    length    = u_strlen ( unichars );
    newString = (char*)malloc ( sizeof( char ) * 4 * ( length + 1 ) );
    if ( newString == NULL )
        return NULL;

    u_austrcpy ( newString, unichars );

    return newString;
}

This is a more practical sample which retrieves data from a resource bundle
and
feeds the data
to u_formatMessage to produce a formatted string

void msgSample3(){

char* key="Languages";
int32_t numItems;
    /* This constant string can also be in the resouce bundle and retrieved at
the time
     * of formatting
     * eg:
     * UResouceBundle* myResB = ures_open("myResources",currentLocale,&err);
     * UChar* Lang_Attrib = ures_getString(myResb,"LANG_ATTRIB",&err);
     */
    UChar* LANG_ATTRIB   =(UChar*) "{0}<language id=\"{1}\"
>{2}</language>\n";
    UChar *result;
    UResourceBundle* pResB,*pDeltaResB=NULL;
    UErrorCode err=U_ZERO_ERROR;
    UChar* indentOffset = (UChar*)"\t\t\t";
    pResB = ures_open("","en",&err);
if(U_FAILURE(err)) {
  return;
}

    ures_getByKey(pResB, key, pDeltaResB, &err);

    if(U_SUCCESS(err)) {
        const UChar *value = 0;
        const char *key = 0;
        int32_t len = 0;
        int16_t indexR = -1;
        int32_t resultLength=0,resultLengthOut=0;
        numItems = ures_getSize(pDeltaResB);
        for(;numItems-->0;){
            key= ures_getKey(pDeltaResB);
            value = ures_get(pDeltaResB,key,&err);
            resultLength=0;
            resultLengthOut=u_formatMessage( "en_US", LANG_ATTRIB,
u_strlen(LANG_ATTRIB),
                                                NULL, resultLength, &err,
indentOffset, value, key);
            if(err==U_BUFFER_OVERFLOW_ERROR){
                 err=U_ZERO_ERROR;
                 resultLength=resultLengthOut+1;
                 result=(UChar*)realloc(result, sizeof(UChar) * resultLength);
                 u_formatMessage("en_US",LANG_ATTRIB,u_strlen(LANG_ATTRIB),
                                result,resultLength,&err,indentOffset,
                                value,key);

                 printf("%s\n", austrdup(result) );
            }

        }

  return;

}
err=U_ZERO_ERROR;
}
```

### Java

```java
import com.ibm.icu.text.*;
import java.util.Date;
import java.text.FieldPosition;

public class TestMessageFormat{
    public void runTest() {
        String format = "At {1,time,::jmm} on {1,date,::dMMMM}, there was {2} on planet {3,number,integer}.";
        MessageFormat mf = new MessageFormat(format);
        Object objectsToFormat[] = { new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), "a Disturbance in the Force", new Integer(5)};
        FieldPosition fp = new FieldPosition(1);
        StringBuffer sb = new StringBuffer();
        try{
            sb = mf.format(objectsToFormat, sb, fp);
            System.out.println(sb.toString());
        }catch(IllegalArgumentException e){
            System.out.println("Exception during formating of type :" +e);
        }
    }

    public static void main(String args[]){
        try{
            new TestMessageFormat().runTest();
        }catch(Exception e){
            System.out.println("Exception of type: "+e);
        }
    }
}
```

## `ChoiceFormat` Class

**Important:** The following documentation is outdated. *`ChoiceFormat` is
probably not what you need. Please use `MessageFormat` with plural arguments for
proper plural selection, and select arguments for simple selection among a fixed
set of choices!*

ICU's `ChoiceFormat` class provides more flexibility than the `printf()` and `scanf()`
style functions for formatting UI strings. This interface can be useful if you
would like a message to change according to the number of items you are
displaying. 

Note: Some Asian languages do not have plural words or phrases.

### C++

```cpp
void msgSample1(){

    UChar *result, *tzID, *str;
    UChar pattern[100];
    int32_t resultLengthOut, resultlength;
    UCalendar *cal;
    UDate d1;
    UErrorCode status = U_ZERO_ERROR;
    str=(UChar*)malloc(sizeof(UChar) * (strlen("disturbance in force") +1));
    u_uastrcpy(str, "disturbance in force");
    tzID=(UChar*)malloc(sizeof(UChar) * 4);
     u_uastrcpy(tzID, "PST");
     cal=ucal_open(tzID, u_strlen(tzID), "en_US", UCAL_TRADITIONAL, &status);
     ucal_setDateTime(cal, 1999, UCAL_MARCH, 18, 0, 0, 0, &status);
     d1=ucal_getMillis(cal, &status);
     u_uastrcpy(pattern, "On {0, date, long}, there was a {1} on planet

{2,number,integer}");
     resultlength=0;
     resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern),
NULL,
resultlength, &status, d1, str, 7);
     if(status==U_BUFFER_OVERFLOW_ERROR){
         status=U_ZERO_ERROR;
         resultlength=resultLengthOut+1;
         result=(UChar*)realloc(result, sizeof(UChar) * resultlength);
         u_formatMessage( "en_US", pattern, u_strlen(pattern), result,
resultlength, &status, d1, str, 7);
     }
     printf("%s\n",austrdup(result) ); //austrdup( a function used to convert
UChar* to char*)
     free(tzID);
     free(str);
double filelimits[] = {0,1,2};
UErrorCode err;
UnicodeString filepart[] = {"are no files","is one file","are {2} files"};
ChoiceFormat fileform(filelimits, filepart,err);
Format testFormats[] = {fileform, null, NumberFormat.getInstance()};
MessageFormat pattform("There {0} on {1}",err);
pattform.setFormats(testFormats);
Formattable testArgs[] = {null, "ADisk", null};
for (int i = 0; i < 4; ++i) {
     testArgs[0] = i;
     testArgs[2] = testArgs[0];
     FieldPosition fpos=0;
     format.format(args,1, result,fpos,mError);
     UnicodeString result = pattform.format(testArgs);
}
```

### C

```c
void msgSample2(){
     UChar* str;
     UErrorCode status = U_ZERO_ERROR;
     UChar *result;
     UChar pattern[100];
     int32_t resultlength,resultLengthOut, i;
     double testArgs[3]= { 100.0, 1.0, 0.0};
     str=(UChar*)malloc(sizeof(UChar) * 10);
     u_uastrcpy(str, "MyDisk");
     u_uastrcpy(pattern, "The disk {1} contains {0,choice,0#no files|1#one
file|1<{0,number,integer} files}");
     for(i=0; i<3; i++){
         resultlength=0;
         resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern),
NULL, resultlength, &status, testArgs[i], str);
         if(status==U_BUFFER_OVERFLOW_ERROR){
             status=U_ZERO_ERROR;
             resultlength=resultLengthOut+1;
             result=(UChar*)malloc(sizeof(UChar) * resultlength);
             u_formatMessage( "en_US", pattern, u_strlen(pattern), result,
resultlength, &status, testArgs[i], str);
         }
     }
     printf("%s\n", austrdup(result) ); //austrdup( a function used to
convert
UChar* to char*)
     free(result);

}
```

### Java

```java
import java.text.ChoiceFormat;
import com.ibm.icu.text.*;
import java.text.Format;

public class TestChoiceFormat{
    public void run(){
        double[] filelimits = {0,1,2};
        String[] filepart = {"are no files","is one file","are {2} files"};
        ChoiceFormat fileform = new ChoiceFormat(filelimits,filepart);
        Format[] testFormats = {fileform,null,NumberFormat.getInstance()};
        MessageFormat pattform = new MessageFormat("There {0} on {1}");
        Object[] testArgs = {null,"ADisk",null};
        for(int i=0;i<4;++i) {
            testArgs[0] = new Integer(i);
            testArgs[2] = testArgs[0];
            System.out.println(pattform.format(testArgs));
        }
    }

    public static void main(String args[]){
        new TestChoiceFormat().run();
    }
}
```
