/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "OpenTypeUtilities.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

//
// Finds the high bit by binary searching
// through the bits in n.
//
le_int8 OpenTypeUtilities::highBit(le_int32 value)
{
    if (value <= 0) {
        return -32;
    }

    le_uint8 bit = 0;

    if (value >= 1 << 16) {
        value >>= 16;
        bit += 16;
    }

    if (value >= 1 << 8) {
        value >>= 8;
        bit += 8;
    }

    if (value >= 1 << 4) {
        value >>= 4;
        bit += 4;
    }

    if (value >= 1 << 2) {
        value >>= 2;
        bit += 2;
    }

    if (value >= 1 << 1) {
        value >>= 1;
        bit += 1;
    }

    return bit;
}


Offset OpenTypeUtilities::getTagOffset(LETag tag, const LEReferenceToArrayOf<TagAndOffsetRecord> &records, LEErrorCode &success)
{
  const TagAndOffsetRecord *r0 = (const TagAndOffsetRecord*)records.getAlias();
  if(LE_FAILURE(success)) return 0;

  le_uint32 recordCount = records.getCount();
  le_uint8 bit = highBit(recordCount);
  le_int32 power = 1 << bit;
  le_int32 extra = recordCount - power;
  le_int32 probe = power;
  le_int32 index = 0;
  
  { 
    const ATag &aTag = (r0+extra)->tag;
    if (SWAPT(aTag) <= tag) {
      index = extra;
    }
  }
  
  while (probe > (1 << 0)) {
    probe >>= 1;
    
    {
      const ATag &aTag = (r0+index+probe)->tag;
      if (SWAPT(aTag) <= tag) {
        index += probe;
      }
    }
  }
  
  {
    const ATag &aTag = (r0+index)->tag;
    if (SWAPT(aTag) == tag) {
      return SWAPW((r0+index)->offset);
    }
  }

  return 0;
}

le_int32 OpenTypeUtilities::getGlyphRangeIndex(TTGlyphID glyphID, const LEReferenceToArrayOf<GlyphRangeRecord> &records, LEErrorCode &success)
{
  if(LE_FAILURE(success)) return -1;

    le_uint32 recordCount = records.getCount();
    le_uint8 bit = highBit(recordCount);
    le_int32 power = 1 << bit;
    le_int32 extra = recordCount - power;
    le_int32 probe = power;
    le_int32 range = 0;

    if (recordCount == 0) {
      return -1;
    }

    if (SWAPW(records(extra,success).firstGlyph) <= glyphID) {
        range = extra;
    }

    while (probe > (1 << 0) && LE_SUCCESS(success)) {
        probe >>= 1;

        if (SWAPW(records(range + probe,success).firstGlyph) <= glyphID) {
            range += probe;
        }
    }

    if (SWAPW(records(range,success).firstGlyph) <= glyphID && SWAPW(records(range,success).lastGlyph) >= glyphID) {
        return range;
    }

    return -1;
}

le_int32 OpenTypeUtilities::search(le_uint32 value, const le_uint32 array[], le_int32 count)
{
    le_int32 power = 1 << highBit(count);
    le_int32 extra = count - power;
    le_int32 probe = power;
    le_int32 index = 0;

    if (value >= array[extra]) {
        index = extra;
    }

    while (probe > (1 << 0)) {
        probe >>= 1;

        if (value >= array[index + probe]) {
            index += probe;
        }
    }

    return index;
}

le_int32 OpenTypeUtilities::search(le_uint16 value, const le_uint16 array[], le_int32 count)
{
    le_int32 power = 1 << highBit(count);
    le_int32 extra = count - power;
    le_int32 probe = power;
    le_int32 index = 0;

    if (value >= array[extra]) {
        index = extra;
    }

    while (probe > (1 << 0)) {
        probe >>= 1;

        if (value >= array[index + probe]) {
            index += probe;
        }
    }

    return index;
}

//
// Straight insertion sort from Knuth vol. III, pg. 81
//
void OpenTypeUtilities::sort(le_uint16 *array, le_int32 count)
{
    for (le_int32 j = 1; j < count; j += 1) {
        le_int32 i;
        le_uint16 v = array[j];

        for (i = j - 1; i >= 0; i -= 1) {
            if (v >= array[i]) {
                break;
            }

            array[i + 1] = array[i];
        }

        array[i + 1] = v;
    }
}

U_NAMESPACE_END

#if LE_ASSERT_BAD_FONT
#include <stdio.h>

static const char *letagToStr(LETag tag, char *str) {
  str[0]= 0xFF & (tag>>24);
  str[1]= 0xFF & (tag>>16);
  str[2]= 0xFF & (tag>>8);
  str[3]= 0xFF & (tag>>0);
  str[4]= 0;
  return str;
}

U_CAPI void U_EXPORT2 _debug_LETableReference(const char *f, int l, const char *msg, const LETableReference *what, const void *ptr, size_t len) {
  char tagbuf[5];
  
  fprintf(stderr, "%s:%d: LETableReference@0x%p: ", f, l, what);
  fprintf(stderr, msg, ptr, len);
  fprintf(stderr, "\n");

  for(int depth=0;depth<10&&(what!=NULL);depth++) {
    for(int i=0;i<depth;i++) {
      fprintf(stderr, " "); // indent
    }
    if(!what->isValid()) {
      fprintf(stderr, "(invalid)");
    }
    fprintf(stderr, "@%p: tag (%s) font (0x%p), [0x%p+0x%lx]\n", what, letagToStr(what->getTag(), tagbuf), what->getFont(),
            what->getAlias(), what->getLength());

    what = what->getParent();
  }
}
#endif
