---
layout: default
title: Collation Examples
nav_order: 7
parent: Collation
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Collation Examples
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Simple Collation Sample Customization

The following program demonstrates how to compare and create sort keys with
default locale.

In **C:**

```c
#include <stdio.h>
#include <memory.h>
#include <string.h>
#include "unicode/ustring.h"
#include "unicode/utypes.h"
#include "unicode/uloc.h"
#include "unicode/ucol.h"

#define MAXBUFFERSIZE 100
#define BIGBUFFERSIZE 5000

UBool collateWithLocaleInC(const char* locale, UErrorCode *status)
{
    UChar         dispName    [MAXBUFFERSIZE]; 
    int32_t       bufferLen   = 0;
    UChar         source            [MAXBUFFERSIZE];
    UChar         target            [MAXBUFFERSIZE];
    UCollationResult result   = UCOL_EQUAL;
    uint8_t             sourceKeyArray    [MAXBUFFERSIZE];
    uint8_t             targetKeyArray    [MAXBUFFERSIZE]; 
    int32_t       sourceKeyOut      = 0, 
                targetKeyOut = 0;
    UCollator     *myCollator = 0;
    if (U_FAILURE(*status))
    {
        return false;
    }
    u_uastrcpy(source, "This is a test.");
    u_uastrcpy(target, "THIS IS A TEST.");
    myCollator = ucol_open(locale, status);
    if (U_FAILURE(*status)){
        bufferLen = uloc_getDisplayName(locale, 0, dispName, MAXBUFFERSIZE, status);
        /*Report the error with display name... */
        fprintf(stderr,
        "Failed to create the collator for : \"%s\"\n", dispName);
        return false;
    }
    result = ucol_strcoll(myCollator, source, u_strlen(source), target, u_strlen(target));
    /* result is 1, secondary differences only for ignorable space characters*/
    if (result != UCOL_LESS)
    {
        fprintf(stderr,
        "Comparing two strings with only secondary differences in C failed.\n");
        return false;
    }
    /* To compare them with just primary differences */
    ucol_setStrength(myCollator, UCOL_PRIMARY);
    result = ucol_strcoll(myCollator, source, u_strlen(source), target, u_strlen(target));
    /* result is 0 */
    if (result != 0)
    {
        fprintf(stderr,
        "Comparing two strings with no differences in C failed.\n");
        return false;
    }

    /* Now, do the same comparison with keys */
    sourceKeyOut = ucol_getSortKey(myCollator, source, -1, sourceKeyArray, MAXBUFFERSIZE);
    targetKeyOut = ucol_getSortKey(myCollator, target, -1, targetKeyArray, MAXBUFFERSIZE);
    result = 0;
    result = strcmp(sourceKeyArray, targetKeyArray);
    if (result != 0)
    {
        fprintf(stderr,
        "Comparing two strings with sort keys in C failed.\n");
        return false;
    }
    ucol_close(myCollator);
    return true;
}
```

In **C++:**

```c++
#include <stdio.h>
#include "unicode/unistr.h"
#include "unicode/utypes.h"
#include "unicode/locid.h"
#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "unicode/sortkey.h"
UBool collateWithLocaleInCPP(const Locale& locale, UErrorCode& status)
{
    UnicodeString dispName; 
    UnicodeString source("This is a test.");
    UnicodeString target("THIS IS A TEST.");
    Collator::EComparisonResult result    = Collator::EQUAL;
    CollationKey sourceKey;
    CollationKey targetKey; 
    Collator      *myCollator = 0;
    if (U_FAILURE(status))
    {
        return false;
    }
    myCollator = Collator::createInstance(locale, status);
    if (U_FAILURE(status)){
        locale.getDisplayName(dispName);
        /*Report the error with display name... */
        fprintf(stderr,
        "%s: Failed to create the collator for : \"%s\"\n", dispName);
        return false;
    }
    result = myCollator->compare(source, target);
    /* result is 1, secondary differences only for ignorable space characters*/
    if (result != UCOL_LESS)
    {
        fprintf(stderr,
        "Comparing two strings with only secondary differences in C failed.\n");
        return false;
    }
    /* To compare them with just primary differences */
    myCollator->setStrength(Collator::PRIMARY);
    result = myCollator->compare(source, target);
    /* result is 0 */
    if (result != 0)
    {
        fprintf(stderr,
        "Comparing two strings with no differences in C failed.\n");
        return false;
    }
    /* Now, do the same comparison with keys */
    myCollator->getCollationKey(source, sourceKey, status);
    myCollator->getCollationKey(target, targetKey, status);
    result = Collator::EQUAL;

    result = sourceKey.compareTo(targetKey);
    if (result != 0)
    {
        fprintf(stderr,
        "%s: Comparing two strings with sort keys in C failed.\n");
        return false;
    }
    delete myCollator;
    return true;
}
```

### Main Function

```c++
extern "C" UBool collateWithLocaleInC(const char* locale, UErrorCode *status);
int main()
{
   UErrorCode status = U_ZERO_ERROR;
   fprintf(stdout, "\n");
   if (collateWithLocaleInCPP(Locale("en", "US"), status) != true)
   {
        fprintf(stderr,
        "Collate with locale in C++ failed.\n");
   } else 
   {
       fprintf(stdout, "Collate with Locale C++ example worked!!\n");
   }
   status = U_ZERO_ERROR;
   fprintf(stdout, "\n");
   if (collateWithLocaleInC("en_US", &status) != true)
   {
        fprintf(stderr,
        "%s: Collate with locale in C failed.\n");
   } else 
   {
       fprintf(stdout, "Collate with Locale C example worked!!\n");
   }
   return 0;
}
```

In **Java:**

```java
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.CollationKey;
import java.util.Locale;

public class CollateExample
{

    public static void main(String arg[]) 
    {
        CollateExample example = new CollateExample();
        try {
            if (!example.collateWithLocale(Locale.US)) {
                System.err.println("Collate with locale example failed.");
            } 
            else {
                System.out.println("Collate with Locale example worked!!");
            }
        } catch (Exception e) {
            System.err.println("Collating with locale failed");
            e.printStackTrace();
        }
    }

    public boolean collateWithLocale(Locale locale) throws Exception
    {
        String source = "This is a test.";
        String target = "THIS IS A TEST.";
        Collator myCollator = Collator.getInstance(locale);

        int result = myCollator.compare(source, target);
        // result is 1, secondary differences only for ignorable space characters
        if (result >= 0) {
            System.err.println(
                "Comparing two strings with only secondary differences failed.");
            return false;
        }
        // To compare them with just primary differences
        myCollator.setStrength(Collator.PRIMARY);
        result = myCollator.compare(source, target);
        // result is 0
        if (result != 0) {
            System.err.println(
                           "Comparing two strings with no differences failed.");
            return false;
        }
        // Now, do the same comparison with keys
        CollationKey sourceKey = myCollator.getCollationKey(source);
        CollationKey targetKey = myCollator.getCollationKey(target);
        result = sourceKey.compareTo(targetKey);
        if (result != 0) {
            System.err.println("Comparing two strings with sort keys failed.");
            return false;
        }
        return true;
    }   
}
```

## Language-sensitive searching

String searching is a well-researched area, and there are algorithms that can
optimize the searching process. Perhaps the best is the Boyer-Moore method. For a
full description of this concept, please see Laura
Werner's text searching article for more details
(<http://icu-project.org/docs/papers/efficient_text_searching_in_java.html>).

However, implementing collation-based search with the Boyer-Moore method
while getting correct results is very tricky, and ICU no longer uses this method
(as of ICU4C 4.0 and ICU4J 53).

Please see the [String Search Service](./string-search) chapter.

## Using large buffers to manage sort keys

A good solution for the problem of not knowing the sort key size in advance is
to allocate a large buffer and store all the sort keys there, while keeping a
list of indexes or pointers to that buffer.

Following is sample code that will take a pointer to an array of UChar pointer,
an array of key indexes. It will allocate and fill a buffer with sort keys and
return the maximum size for a sort key. Once you have done this to your string,
you just need to allocate a field of maximum size and copy your sortkeys from
the buffer to fields.

```c++
uint32_t fillBufferWithKeys(UCollator *coll, UChar **source, uint32_t *keys,
                            uint32_t sourceSize, uint8_t **buffer,
                            uint32_t *maxSize, UErrorCode *status) 
{
  if(status == NULL || U_FAILURE(*status)) {
    return 0;
  }

  uint32_t bufferSize = 16384;
  uint32_t increment = 16384;
  uint32_t currentOffset = 0;
  uint32_t keySize = 0;
  uint32_t i = 0;
  *maxSize = 0;

  *buffer = (uint8_t *)malloc(bufferSize * sizeof(uint8_t));
  if(buffer == NULL) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  for(i = 0; i < sourceSize; i++) {
    keys[i] = currentOffset;
    keySize = ucol_getSortKey(coll, source[i], -1, *buffer+currentOffset, bufferSize-currentOffset);
    if(keySize > bufferSize-currentOffset) {
      *buffer = (uint8_t *)realloc(*buffer, bufferSize+increment);
      if(buffer == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return 0;
      }
      bufferSize += increment;
      keySize = ucol_getSortKey(coll, source[i], -1, *buffer+currentOffset, bufferSize-currentOffset);
    }
    /* here you can hook code that does something interesting with the keySize - 
     * remembers the maximum or similar...
     */
    if(keySize > *maxSize) {
      *maxSize = keySize;
    }
    currentOffset += keySize;
  }

  return currentOffset;
}
```
