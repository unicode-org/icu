/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnv_lmb.cpp
*   encoding:   US-ASCII
*   tab size:   4 (not used)
*   indentation:4
*
*   created on: 2000feb09
*   created by: Brendan Murray
*/

#include "unicode/utypes.h"
#include "cmemory.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"

/* LMBCS -------------------------------------------------------------------- */

/* Group bytes, and things that look like group bytes, should always be 8-bits */
typedef uint8_t ulmbcs_grp_t;


/* Define some constants instead of using literals */


/* LMBCS groups */
#define ULMBCS_GRP_EXCEPT     0x00    /* placeholder index for 'oddballs' XY, where Y<0x80 */
#define ULMBCS_GRP_L1         0x01   /* Latin-1   */
#define ULMBCS_GRP_GR         0x02   /* Greek     */
#define ULMBCS_GRP_HE         0x03   /* Hebrew    */
#define ULMBCS_GRP_AR         0x04   /* Arabic    */
#define ULMBCS_GRP_RU         0x05   /* Cyrillic  */
#define ULMBCS_GRP_L2         0x06   /* Latin-2   */
#define ULMBCS_GRP_TR         0x08   /* Turkish   */
#define ULMBCS_GRP_TH         0x0B   /* Thai      */
#define ULMBCS_GRP_CTRL       0x0F   /* C0/C1 controls */
#define ULMBCS_GRP_JA         0x10   /* Japanese  */
#define ULMBCS_GRP_KO         0x11   /* Korean    */
#define ULMBCS_GRP_CN         0x12   /* Chinese PRC */
#define ULMBCS_GRP_TW         0x13   /* Chinese Taiwan */
#define ULMBCS_GRP_UNICODE    0x14   /* Unicode compatibility group */
#define ULMBCS_GRP_LAST       0x14   /* last LMBCS group that means anything */

/* some special values that can appear in place of optimization groups */
#define ULMBCS_HT              0x09   /* Fixed control char - Horizontal Tab */
#define ULMBCS_LF              0x0A   /* Fixed control char - Line Feed */
#define ULMBCS_CR              0x0D   /* Fixed control char - Carriage Return */
#define ULMBCS_123SYSTEMRANGE  0x19   /* Fixed control char for 1-2-3 file data: start system range name */
#define ULMBCS_DEFAULTOPTGROUP 0x1    /* default optimization group for LMBCS */    
#define ULMBCS_DOUBLEOPTGROUP  0x10   /* start of double-byte optimization groups */

/* parts of LMBCS values, or ranges for LMBCS data */
#define ULMBCS_UNICOMPATZERO   0xF6   /* PUA range for Unicode chars containing LSB = 0 */
#define ULMBCS_CTRLOFFSET      0x20   /* Offset of control range in group 0x0F */
#define ULMBCS_C1START         0x80   /* Start of 'C1' upper ascii range in ANSI code pages */
#define ULMBCS_C0END           0x1F   /* last of the  'C0' lower ascii contraol range in ANSI code pages */
#define ULMBCS_INVALIDCHAR     0xFFFF /* Invalid character value = convert failed */


/* special return values for FindLMBCSUniRange */
#define ULMBCS_AMBIGUOUS_SBCS   0x80   /* could fit in more than one 
                                          LMBCS sbcs native encoding (example: most accented latin) */
#define ULMBCS_AMBIGUOUS_MBCS   0x81   /* could fit in more than one 
                                          LMBCS mbcs native encoding (example: Unihan) */

/* macro to check compatibility of groups */
#define ULMBCS_AMBIGUOUS_MATCH(agroup, xgroup) \
                  ((((agroup) == ULMBCS_AMBIGUOUS_SBCS) && \
                  (xgroup) < ULMBCS_DOUBLEOPTGROUP) || \
                  (((agroup) == ULMBCS_AMBIGUOUS_MBCS) && \
                  (xgroup) >= ULMBCS_DOUBLEOPTGROUP))

/* Max size for 1 LMBCS char */
#define ULMBCS_CHARSIZE_MAX      3


/* JSGTODO: what is ICU standard debug assertion method? 
   Invent an all-crash stop here, for now */
#if 1 
#define MyAssert(b) {if (!(b)) {*(char *)0 = 1;}}
#else
#define MyAssert(b) 
#endif


/* Map Optimization group byte to converter name. Note the following:
      0x00 is dummy, and contains the name of the exceptions converter.
      0x02 is currently unavailable: NLTC have been asked to provide.
      0x0F and 0x14 are algorithmically calculated
      0x09, 0x0A, 0x0D are data bytes (HT, LF, CR)
      0x07, 0x0C and 0x0E are unused
*/
static const char * OptGroupByteToCPName[ULMBCS_CTRLOFFSET] = {
   /* 0x0000 */ "lmb-excp", /* No zero opt group: for non-standard entries */
   /* 0x0001 */ "ibm-850",
   /* 0x0002 */ "ibm-851",
   /* 0x0003 */ "ibm-1255",
   /* 0x0004 */ "ibm-1256",
   /* 0x0005 */ "ibm-1251",
   /* 0x0006 */ "ibm-852",
   /* 0x0007 */ NULL,      /* Unused */
   /* 0x0008 */ "ibm-1254",
   /* 0x0009 */ NULL,      /* Control char HT */
   /* 0x000A */ NULL,      /* Control char LF */
   /* 0x000B */ "ibm-874",
   /* 0x000C */ NULL,      /* Unused */
   /* 0x000D */ NULL,      /* Control char CR */
   /* 0x000E */ NULL,      /* Unused */
   /* 0x000F */ NULL,      /* Control chars: 0x0F20 + C0/C1 character: algorithmic */
   /* 0x0010 */ "ibm-943",
   /* 0x0011 */ "ibm-1361",
   /* 0x0012 */ "ibm-950",
   /* 0x0013 */ "ibm-1386"

   /* The rest are null, including the 0x0014 Unicode compatibility region
   and 0x0019, the 1-2-3 system range control char */      

};





/* map UNICODE ranges to converter indexes (or special values) */

ulmbcs_grp_t FindLMBCSUniRange(UChar uniChar, UErrorCode*    err);

struct _UniLMBCSGrpMap  
{
   UChar uniStartRange;
   UChar uniEndRange;
   ulmbcs_grp_t  GrpType;
} UniLMBCSGrpMap[]
=
{
   { 0x0001, 0x001F,  ULMBCS_GRP_CTRL },
   { 0x0080, 0x009F,  ULMBCS_GRP_CTRL },
   { 0x00A0, 0x0113,  ULMBCS_AMBIGUOUS_SBCS },
   { 0x0115, 0x0120,  ULMBCS_AMBIGUOUS_SBCS }, 
   { 0x0120, 0x012B,  ULMBCS_GRP_EXCEPT },
   { 0x012C, 0x01CD,  ULMBCS_AMBIGUOUS_SBCS },
   { 0x01CE, 0x01CE,  ULMBCS_AMBIGUOUS_MBCS },
   { 0x01CF, 0x1FFF,  ULMBCS_AMBIGUOUS_SBCS },
   { 0x2000, 0xFFFD,  ULMBCS_AMBIGUOUS_MBCS },
   { 0xFFFF, 0xFFFF }
};
   
ulmbcs_grp_t FindLMBCSUniRange(UChar uniChar, UErrorCode*    err)
{
   struct _UniLMBCSGrpMap * pTable = UniLMBCSGrpMap;

   while (uniChar > pTable->uniEndRange) 
   {
      pTable++;
   }

   if (uniChar >= pTable->uniStartRange) 
   {
      return pTable->GrpType;
   }
   
   if (pTable->uniStartRange == 0xFFFF) 
   {
      *err = ULMBCS_INVALIDCHAR;
   }
   return ULMBCS_GRP_UNICODE;
}
   
#if 0 
/* JSGTODO (by Brendan?) some incomplete source data from Brendan to be integrated */

         0xFE30,  ULMBCS_GRP_JA,       ULMBCS_FLAGS_CONTINUE,
         0xFA2E,  ULMBCS_GRP_UNICODE,  ULMBCS_FLAGS_UNICODE,
         0xF8FF,  ULMBCS_GRP_JA,       ULMBCS_FLAGS_CONTINUE,
         0xD7FF,  ULMBCS_GRP_UNICODE,  ULMBCS_FLAGS_UNICODE,
         0xABFF,  ULMBCS_GRP_KO,       ULMBCS_FLAGS_UNICODE,
         0x9FFF,  ULMBCS_GRP_UNICODE,  ULMBCS_FLAGS_UNICODE,
         0x31FF,  ULMBCS_GRP_JA,       ULMBCS_FLAGS_CONTINUE,
         0x318F,  ULMBCS_GRP_CN,       ULMBCS_FLAGS_CONTINUE,
         0x3130,  ULMBCS_GRP_KO,       ULMBCS_FLAGS_UNICODE,
         0x3100,  ULMBCS_GRP_CN,       ULMBCS_FLAGS_CONTINUE,
         0x313F,  ULMBCS_GRP_JA,       ULMBCS_FLAGS_UNICODE,
         0x2FFF,  ULMBCS_GRP_JA,       ULMBCS_FLAGS_CONTINUE,
         0x2714,  ULMBCS_GRP_UNICODE,  ULMBCS_FLAGS_UNICODE,
         0x2000,  ULMBCS_GRP_L1,       ULMBCS_FLAGS_CONTINUE,
         0x0E5C,  ULMBCS_GRP_UNICODE,  ULMBCS_FLAGS_UNICODE,
         0x0E00,  ULMBCS_GRP_TH,       ULMBCS_FLAGS_UNICODE,
         0x06FF,  ULMBCS_GRP_UNICODE,  ULMBCS_FLAGS_UNICODE,
         0x0600,  ULMBCS_GRP_AR,       ULMBCS_FLAGS_UNICODE,
         0x0500,  ULMBCS_GRP_HE,       ULMBCS_FLAGS_UNICODE,
         0x0400,  ULMBCS_GRP_RU,       ULMBCS_FLAGS_UNICODE,
         0x0300,  ULMBCS_GRP_GR,       ULMBCS_FLAGS_UNICODE,
         0x001F,  ULMBCS_GRP_L1,       ULMBCS_FLAGS_CONTINUE,
         0x0000,  ULMBCS_GRP_CTRL,     ULMBCS_FLAGS_UNICODE
#endif


int LMBCSConversionWorker (
   UConverterDataLMBCS * extraInfo, ulmbcs_grp_t group, 
   uint8_t * pStartLMBCS, UChar * pUniChar, 
   ulmbcs_grp_t * lastConverterIndex, UBool * groups_tried,
   UErrorCode* err);

int LMBCSConversionWorker (
   UConverterDataLMBCS * extraInfo, ulmbcs_grp_t group, 
   uint8_t * pStartLMBCS, UChar * pUniChar, 
   ulmbcs_grp_t * lastConverterIndex, UBool * groups_tried,
   UErrorCode * err)
{
   uint8_t * pLMBCS = pStartLMBCS;
   UConverter * xcnv = extraInfo->OptGrpConverter[group];
   uint8_t mbChar [ULMBCS_CHARSIZE_MAX];
   uint8_t * pmbChar = mbChar;
   UBool isDoubleByteGroup = (group >= ULMBCS_DOUBLEOPTGROUP) ? TRUE : FALSE;
   UErrorCode localErr = 0;
   int bytesConverted =0;

   MyAssert(xcnv);
   MyAssert(group<ULMBCS_GRP_UNICODE);

   ucnv_fromUnicode(xcnv, (char **)&pmbChar,(char *)mbChar+sizeof(mbChar),(const UChar **)&pUniChar,pUniChar+1,NULL,TRUE,&localErr);
   bytesConverted = pmbChar - mbChar;
   pmbChar = mbChar;

   /* most common failure mode is the sub-converter using the substitution char (0x7f for our converters)
   */

   if (*mbChar == xcnv->subChar[0] || U_FAILURE(localErr) || !bytesConverted )
   {
      /* JSGTODO: are there some local failure modes that ought to be bubbled up in some other way? */
      groups_tried[group] = TRUE;
      return 0;
   }
   
   *lastConverterIndex = group;

   /* All initial byte values in lower ascii range should have been caught by now,
      except with the exception group.

      Uncomment this assert to find them.
   */

   /* MyAssert((*pmbChar <= ULMBCS_C0END) || (*pmbChar >= ULMBCS_C1START) || (group == ULMBCS_GRP_EXCEPT)); */
   
   /* use converted data: first write 0, 1 or two group bytes */
   if (group != ULMBCS_GRP_EXCEPT && extraInfo->OptGroup != group)
   {
      *pLMBCS++ = group;
      if (bytesConverted == 1 && isDoubleByteGroup)
      {
         *pLMBCS++ = group;
      }
   }
   /* then move over the converted data */
   do 
   {
      *pLMBCS++ = *pmbChar++;
   } 
   while(--bytesConverted);   
      
   return (pLMBCS - pStartLMBCS);
}


/* Convert Unicode string to LMBCS */
void _LMBCSFromUnicode(UConverter*     _this,
                       char**          target,
                       const char*     targetLimit,
                       const UChar**   source,
                       const UChar*    sourceLimit,
                       int32_t *       offsets,
                       UBool          flush,
                       UErrorCode*     err)
{
   ulmbcs_grp_t lastConverterIndex = 0;
   UChar uniChar;
   uint8_t LMBCS[ULMBCS_CHARSIZE_MAX];
   uint8_t * pLMBCS;
   int bytes_written;
   UBool groups_tried[ULMBCS_GRP_LAST];
   UConverterDataLMBCS * extraInfo = (UConverterDataLMBCS *) _this->extraInfo;

   /* Arguments Check */
   if  (!err || U_FAILURE(*err)) 
   {
      return;
   }

   if  (sourceLimit < *source)
   {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
   }

   
   do 
   {
      uniChar = *(*source)++;
      bytes_written = 0;
      pLMBCS = LMBCS;

      /* single byte matches */

      if (uniChar == 0 || uniChar == ULMBCS_HT || uniChar == ULMBCS_CR || 
          uniChar == ULMBCS_LF || uniChar == ULMBCS_123SYSTEMRANGE || 
          ((uniChar >= ULMBCS_CTRLOFFSET) && (uniChar < ULMBCS_C1START)))
      {
         *pLMBCS++ = (uint8_t) uniChar;
         bytes_written = 1;
      }


      if (!bytes_written) 
      {
         /* Check by UNICODE range */
         ulmbcs_grp_t group = FindLMBCSUniRange(uniChar,err);
         
         if (group == ULMBCS_GRP_UNICODE)
         {
            /* encode into LMBCS Unicode range */
            uint8_t LowCh = (uint8_t) (uniChar & 0x00FF);
            uint8_t HighCh  = (uint8_t)(uniChar >> 8);

            *pLMBCS++ = ULMBCS_GRP_UNICODE;

            if (LowCh == 0)
            {
               *pLMBCS++ = ULMBCS_UNICOMPATZERO;
               *pLMBCS++ = HighCh;
            }
            else
            {
               *pLMBCS++ = HighCh;
               *pLMBCS++ = LowCh;
            }
            
            bytes_written = pLMBCS - LMBCS;
         }
         else if (group == ULMBCS_GRP_CTRL)
         {
            /* Handle control characters here */
            if (uniChar <= ULMBCS_C0END)
            {
               *pLMBCS++ = ULMBCS_GRP_CTRL;
               *pLMBCS++ = ULMBCS_CTRLOFFSET + (uint8_t) uniChar;
            }
            else if (uniChar >= ULMBCS_C1START && uniChar <= ULMBCS_C1START + ULMBCS_CTRLOFFSET)
            {
               *pLMBCS++ = ULMBCS_GRP_CTRL;
               *pLMBCS++ = (uint8_t) (uniChar & 0x00FF);
            }
            bytes_written = pLMBCS - LMBCS;
         }
         else if (group < ULMBCS_GRP_UNICODE)
         {
            /* a specific converter has been identified - use it */
            bytes_written = LMBCSConversionWorker (
                              extraInfo, group, pLMBCS, &uniChar, 
                              &lastConverterIndex, groups_tried, err);

            MyAssert(bytes_written); /* table should never return unusable group */

         }
         else  /* the ambiguous group cases */
         {
            memset(groups_tried, 0, sizeof(groups_tried));

            /* check for non-default optimization group */
            if (extraInfo->OptGroup != 1 
                  && ULMBCS_AMBIGUOUS_MATCH(group, extraInfo->OptGroup)) 
            {
               bytes_written = LMBCSConversionWorker (extraInfo, 
                  extraInfo->OptGroup, pLMBCS, &uniChar, 
                  &lastConverterIndex, groups_tried, err);
            }
            /* check for locale optimization group */
            if (!bytes_written 
               && (extraInfo->localeConverterIndex) 
               && (ULMBCS_AMBIGUOUS_MATCH(group, extraInfo->localeConverterIndex)))
               {
                  bytes_written = LMBCSConversionWorker (extraInfo, 
                     extraInfo->localeConverterIndex, pLMBCS, &uniChar, 
                     &lastConverterIndex, groups_tried, err);
               }
            /* check for last optimization group used for this string */
            if (!bytes_written 
                && (lastConverterIndex) 
                && (ULMBCS_AMBIGUOUS_MATCH(group, lastConverterIndex)))
               {
                  bytes_written = LMBCSConversionWorker (extraInfo, 
                     lastConverterIndex, pLMBCS, &uniChar, 
                     &lastConverterIndex, groups_tried, err);
              
               }
            if (!bytes_written)
            {
               /* just check every matching converter */
               ulmbcs_grp_t grp_start;
               ulmbcs_grp_t grp_end;  
               ulmbcs_grp_t grp_ix;
               grp_start = (group == ULMBCS_AMBIGUOUS_MBCS) 
                           ? ULMBCS_DOUBLEOPTGROUP 
                           :  ULMBCS_GRP_L1;
               grp_end = (group == ULMBCS_AMBIGUOUS_MBCS) 
                           ? ULMBCS_GRP_LAST-1 
                           :  ULMBCS_GRP_TH;

               for (grp_ix = grp_start;
                      grp_ix <= grp_end && !bytes_written; 
                       grp_ix++)
               {
                  if (extraInfo->OptGrpConverter [grp_ix] && !groups_tried [grp_ix])
                  {
                     bytes_written = LMBCSConversionWorker (extraInfo, 
                          grp_ix, pLMBCS, &uniChar, 
                          &lastConverterIndex, groups_tried, err);
                  }
               }

                  /* a final conversion fallback for sbcs to the exceptions group */
                  if (!bytes_written && group == ULMBCS_AMBIGUOUS_SBCS)
               {
                  bytes_written = LMBCSConversionWorker (extraInfo, 
                     ULMBCS_GRP_EXCEPT, pLMBCS, &uniChar, 
                     &lastConverterIndex, groups_tried, err);
               }
               /* all of our strategies failed. Fallback to Unicode. Consider adding these to table */

               if (!bytes_written)
               {
                              /* encode into LMBCS Unicode range */
                  uint8_t LowCh = (uint8_t) uniChar;
                  uint8_t HighCh  = (uint8_t)(uniChar >> 8);

                  *pLMBCS++ = ULMBCS_GRP_UNICODE;

                  if (LowCh == 0)
                  {
                     *pLMBCS++ = ULMBCS_UNICOMPATZERO;
                     *pLMBCS++ = HighCh;
                  }
                  else
                  {
                     *pLMBCS++ = HighCh;
                     *pLMBCS++ = LowCh;
                  }
               
                  bytes_written = pLMBCS - LMBCS;
               }
            }
         }
      }
  
      if (*target + bytes_written > targetLimit)
      {
         /* JSGTODO deal with buffer running out here */
      }

      /* now that we are sure it all fits, move it in */
      for(pLMBCS = LMBCS; bytes_written--; *(*target)++ = *pLMBCS++)
         { };
                   
   }
   while (*source< sourceLimit && 
      *target < targetLimit && 
      !U_FAILURE(*err));
      
      /* JSGTODO Check the various exit conditions */
}



/* Return the Unicode representation for the current LMBCS character */
UChar32 _LMBCSGetNextUChar(UConverter*   _this,
                         const char**  source,
                         const char*   sourceLimit,
                         UErrorCode*   err)
{
   uint8_t  CurByte; /* A byte from the input stream */
   UChar32 uniChar;    /* an output UNICODE char */
   UChar mbChar;  /* an intermediate multi-byte value (mbcs or LMBCS) */
   CompactShortArray *MyCArray = NULL;
   UConverterDataLMBCS * extraInfo = (UConverterDataLMBCS *) _this->extraInfo;
   ulmbcs_grp_t group = 0; 
   UConverter* cnv = 0; 

   /* Opt Group (or first data byte) */
      CurByte = *((uint8_t *) (*source)++);
      uniChar = 0;

      /*
       * at entry of each if clause:
       * 1. 'CurByte' points at the first byte of a LMBCS character
       * 2. '*source'points to the next byte of the source stream after 'CurByte' 
       *
       * the job of each if clause is:
       * 1. set '*source' to point at the beginning of next char (nop if LMBCS char is only 1 byte)
       * 2. set 'uniChar' up with the right Unicode value, or set 'err' appropriately
       */
      
      /* First lets check the simple fixed values. */
      /* JSGTODO (from markus): a switch would be much faster here */
      if (CurByte == 0 || CurByte == ULMBCS_HT || CurByte == ULMBCS_CR || 
          CurByte == ULMBCS_LF || CurByte == ULMBCS_123SYSTEMRANGE || 
          ((CurByte >= ULMBCS_CTRLOFFSET) && (CurByte < ULMBCS_C1START)))
      {
         uniChar = CurByte;
      }
      else 
      if (CurByte == ULMBCS_GRP_CTRL)  /* Control character group - no opt group update */
      {
        /* JSGTODO (from markus): please make sure your error code returns are consistent with
           those of the other converters; the utf implementations return truncated only when
           the input is too short; if there is nothing at all, then they set index out of bounds.
           see unicode in here.
           (and, please, come to a common indentation - brendan 2, you 3??)
           (plus, no // comments in c code - it breaks many c compilers!)
         */
         if (*source >= sourceLimit)
         {
            *err = U_TRUNCATED_CHAR_FOUND;
         }
         else
         {
             uint8_t C0C1byte = *(*source)++;
             uniChar = (C0C1byte < ULMBCS_C1START) ? C0C1byte - ULMBCS_CTRLOFFSET : C0C1byte;          
         }
      }
      else 
      if (CurByte == ULMBCS_GRP_UNICODE) /* Unicode compatibility group: BE as is */
      {
        uint8_t HighCh, LowCh;
        
        if (*source + 2 > sourceLimit)
        {
          if (*source >= sourceLimit)
          {
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
          }
          else
          {
            *err = U_TRUNCATED_CHAR_FOUND;
          }
        }
        else
        {
          HighCh = *(*source)++; /* Big-endian Unicode in LMBCS compatibility group*/
          LowCh = *(*source)++;

          if (HighCh == ULMBCS_UNICOMPATZERO ) 
          {
             HighCh = LowCh;
             LowCh = 0; /* zero-byte in LSB special character */
          }

          uniChar = (HighCh << 8) | LowCh;

          /* UTF-16 means that there may be a surrogate pair */
          if(UTF_IS_FIRST_SURROGATE(uniChar))
          {
            /* assume that single surrogates only occur in Unicode LMBCS sequences */
            if (*source >= sourceLimit)
            {
              *err = U_TRUNCATED_CHAR_FOUND;
            }
            else
            /* is there really Unicode, and a second surrogate?
               if not, then we ignore it without error
             */
            if(**source == ULMBCS_GRP_UNICODE)
            {
              if (*source + 3 > sourceLimit)
              {
                *err = U_TRUNCATED_CHAR_FOUND;
              }
              else
              {
                uint16_t second;
                HighCh = *(*source + 1); /* Big-endian Unicode in LMBCS compatibility group*/
                LowCh = *(*source + 2);

                if (HighCh == ULMBCS_UNICOMPATZERO ) 
                {
                   HighCh = LowCh;
                   LowCh = 0; /* zero-byte in LSB special character */
                }

                second = (HighCh << 8) | LowCh;
                if(UTF_IS_SECOND_SURROGATE(second))
                {
                  uniChar = UTF16_GET_PAIR_VALUE(uniChar, second);
                  *source += 3;
                }
              }
            }
          }
        }
                       
      }
      
      else if (CurByte <= ULMBCS_CTRLOFFSET)  
      {
         group = CurByte;                   /* group byte is in the source */
         cnv = extraInfo->OptGrpConverter[group];
         
         if (!cnv)
         {
            /* this is not a valid group byte - no converter*/
            *err = U_INVALID_CHAR_FOUND;
         }
         

         else if (group >= ULMBCS_DOUBLEOPTGROUP)    /* double byte conversion */
         {
            uint8_t HighCh, LowCh;

            
            HighCh = *(*source)++; 
            LowCh = *(*source)++;

            /* check for LMBCS doubled-group-byte case */
            mbChar = (HighCh == group) ? LowCh : (HighCh<<8) | LowCh;

            MyCArray = &cnv->sharedData->table->mbcs.toUnicode;
            uniChar = (UChar) ucmp16_getu (MyCArray, mbChar);
            
         }
         else                                   /* single byte conversion */
         {
            CurByte = *(*source)++;
            if (CurByte >= ULMBCS_C1START)
            {
               uniChar = cnv->sharedData->table->sbcs.toUnicode[CurByte];
            }
            else
            {
               /* The non-optimizable oddballs where there is an explicit byte 
                * AND the second byte is not in the upper ascii range
               */
               cnv = extraInfo->OptGrpConverter [ULMBCS_GRP_EXCEPT];  
               
               /* Lookup value must include opt group */
               mbChar =  (UChar)(group << 8) | (UChar) CurByte;

               MyCArray = &cnv->sharedData->table->mbcs.toUnicode;
               uniChar = (UChar) ucmp16_getu(MyCArray, mbChar);

            }
         }
      }
      else if (CurByte >= ULMBCS_C1START) /* group byte is implicit */
      {
         group = extraInfo->OptGroup;
         cnv = extraInfo->OptGrpConverter[group];

         if (group >= ULMBCS_DOUBLEOPTGROUP)    /* double byte conversion */
         {
            uint8_t HighCh, LowCh;
         
            /* JSGTODO need to deal with case of single byte G1
               chars in mbcs groups */

            HighCh = CurByte;
            LowCh = *(*source)++;

            mbChar = (HighCh<<8) | LowCh;
            MyCArray = &cnv->sharedData->table->mbcs.toUnicode;
            uniChar = (UChar) ucmp16_getu (MyCArray, mbChar);
            (*source) += sizeof(UChar);
         }
         else                                   /* single byte conversion */
         {
             uniChar = cnv->sharedData->table->sbcs.toUnicode[CurByte];
         }
      }
      else
      {
#if DEBUG
         /* JSGTODO: assert here: we should never get here. */
#endif
         
      }
      /* JSGTODO: need to correctly deal with partial chars */
      /* JSGTODO (from markus :-) - deal with surrogate pairs;
         see UTF-8/16BE/16LE implementations,
         http://oss.software.ibm.com/icu/archives/icu/icu.0002/msg00043.html

         behavior: uniChar is now declared UChar32;
         if(UTF_IS_FIRST_SURROGATE(uniChar)) then check for more input length
         if too short, then error
         else get another 16-bit unit
              if(UTF_IS_SECOND_SURROGATE(second unit)) then
                  uniChar=UTF16_GET_PAIR_VALUE(uniChar, second unit);

         You may need to do this only when the following LMBCS byte indicates
         embedded Unicode (ULMBCS_GRP_UNICODE), and get the following surrogate directly
         from the following two bytes like the UTF-16BE implementation.

         actually, just for the embedded Unicode, i did this. if no other groups
         in LMBCS can carry single surrogates, then we may be done with my changes.
       */
      return uniChar;
}



void _LMBCSToUnicodeWithOffsets(UConverter*    _this,
                     UChar**        target,
                     const UChar*   targetLimit,
                     const char**   source,
                     const char*    sourceLimit,
                     int32_t*       offsets,
                     UBool         flush,
                     UErrorCode*    err)
{
   UChar32 uniChar;    /* an output UNICODE char */
   CompactShortArray *MyCArray = NULL;
   UConverterDataLMBCS * extraInfo = (UConverterDataLMBCS *) _this->extraInfo;
   ulmbcs_grp_t group = 0; 
   UConverter* cnv = 0; 
   const char * pStartLMBCS = *source; 
   
   if  (!err || U_FAILURE(*err))
   {
      return;
   }
   if ((_this == NULL) || (targetLimit < *target) || (sourceLimit < *source))
   {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
   }

#if 0 /* JSGTODOD - restore incomplete char handling      */

   /* Have we arrived here from a prior conversion ending with a partial char?
      The only possible configurations are:
         1. mode contains the group byte of SBCS LMBCS char;
         2. mode contains the group byte of MBCS LMBCS char
            For both continue with next char in input buffer
         3. mode contains group byte + 1st data byte of MBCS LMBCS char
            Partially process & get the second data byte
         4. mode contains both group bytes of double group-byte MBCS LMBCS char
            Nuke contents after setting up converter & continue with buffer data
   */
   if (_this->toUnicodeStatus)
   {
      mbChar = (UChar) _this->mode;      /* Restore the previously calculated char    */

      _this->toUnicodeStatus   = 0;       /* Reset other fields*/
      _this->invalidCharLength = 0;

      /* Check if this is a partial MBCS char (fall through if SBCS) */
      if (mbChar > 0xFF)
      {
         /* Select the correct converter */
         group = (mbChar >> 8) & 0x00FF;
         cnv = extraInfo->OptGrpConverter[group];
                          
         /* Pick up the converter table */
         MyCArray = cnv->sharedData->table->mbcs.toUnicode;
      
         /* Use only data byte: NULL if the character has pair of group-bytes */
         if (mbChar & 0x00FF < ULMBCS_MAXGRPBYTE)
            CurByte = 0;
         else
            CurByte = ((mbChar & 0x00FF) << 8);
         
         /* Add the current char from the buffer */            
         CurByte |=  *((uint8_t *) (*source)++);

         goto continueWithPartialMBCSChar;
         
      }
      else
      {
         goto continueWithPartialChar;
      }
   }
#endif

   

   /* Process from source to limit */
   while (!*err && sourceLimit > *source && targetLimit > *target)
   {
      if(offsets)
      {
         *offsets = (*source) - pStartLMBCS;
      }

      uniChar = _LMBCSGetNextUChar(_this, source, sourceLimit, err);

      
      /* last step is always to move the new value into the buffer */
      if (U_SUCCESS(*err) && uniChar != missingUCharMarker)
      {
         /* JSGTODO  deal with missingUCharMarker case for error/info reporting. */
         if(!UTF_NEED_MULTIPLE_UCHAR(uniChar)) {
            *(*target)++ = (UChar)uniChar;
         } else {
            /* JSGTODO (from markus)
               write several UChar's for this UChar32;
               you may need to use macros like UTF_APPEND_CHAR() or similar (from utf.h)
               what does this mean for the target range check and for the offsets?
             */
         }
         if(offsets)
         {
            offsets++;
         }

       }
   }
#if 0
   /* JSGTODO restore partial char handling */
   /* Check to see if we've fallen through because of a partial char */
   if (*err == U_TRUNCATED_CHAR_FOUND)
   {
      _this->mode = mbChar; /* Save current partial char */
   }
#endif
}



/* Convert LMBCS string to Unicode */
void _LMBCSToUnicode(UConverter*    _this,
                     UChar**        target,
                     const UChar*   targetLimit,
                     const char**   source,
                     const char*    sourceLimit,
                     int32_t*       offsets,
                     UBool         flush,
                     UErrorCode*    err)
{
    _LMBCSToUnicodeWithOffsets(_this, target, targetLimit, source, sourceLimit, offsets, flush,err);
}



static void _LMBCSOpenWorker(UConverter*  _this, 
                       const char*  name, 
                       const char*  locale, 
                       UErrorCode*  err,
                       ulmbcs_grp_t OptGroup
                       )
{
   UConverterDataLMBCS * extraInfo = uprv_malloc (sizeof (UConverterDataLMBCS));

   if(extraInfo != NULL)
    {

       ulmbcs_grp_t i;
       ulmbcs_grp_t imax;

       imax = sizeof(extraInfo->OptGrpConverter)/sizeof(extraInfo->OptGrpConverter[0]);

       for (i=0; i < imax; i++)         
       {
            extraInfo->OptGrpConverter[i] =
               (OptGroupByteToCPName[i] != NULL) ? 
               ucnv_open(OptGroupByteToCPName[i], err) : NULL;
       }

       extraInfo->OptGroup = OptGroup;
        /* JSGTODO: add LocaleConverterIndex logic here */
       extraInfo->localeConverterIndex = 0;
   } 
   else
   {
       *err = U_MEMORY_ALLOCATION_ERROR;
   }
   
   _this->extraInfo = extraInfo;
}




static void _LMBCSClose(UConverter *   _this) 
{
    if (_this->extraInfo != NULL)
    {
        ulmbcs_grp_t Ix;

        for (Ix=0; Ix < ULMBCS_GRP_UNICODE; Ix++)
        {
           UConverterDataLMBCS * extraInfo = (UConverterDataLMBCS *) _this->extraInfo;
           if (extraInfo->OptGrpConverter[Ix] != NULL)
              ucnv_close (extraInfo->OptGrpConverter[Ix]);
        }
        uprv_free (_this->extraInfo);
    }
}



#define DEFINE_LMBCS_OPEN(n) \
static void _LMBCSOpen##n(UConverter*  _this,const char* name,const char* locale,UErrorCode*  err) \
{ _LMBCSOpenWorker(_this, name,locale, err, n);} \


DEFINE_LMBCS_OPEN(1)
DEFINE_LMBCS_OPEN(2)
DEFINE_LMBCS_OPEN(3)
DEFINE_LMBCS_OPEN(4)
DEFINE_LMBCS_OPEN(5)
DEFINE_LMBCS_OPEN(6)
DEFINE_LMBCS_OPEN(8)
DEFINE_LMBCS_OPEN(11)
DEFINE_LMBCS_OPEN(16)
DEFINE_LMBCS_OPEN(17)
DEFINE_LMBCS_OPEN(18)
DEFINE_LMBCS_OPEN(19)

#define DECLARE_LMBCS_DATA(n) \
 static const UConverterImpl _LMBCSImpl##n={\
    UCNV_LMBCS_##n,\
    NULL,NULL,\
    _LMBCSOpen##n,\
    _LMBCSClose,\
    NULL,\
    _LMBCSToUnicode,\
    _LMBCSToUnicodeWithOffsets,\
    _LMBCSFromUnicode,\
    NULL,\
    _LMBCSGetNextUChar,\
    NULL\
};\
const UConverterStaticData _LMBCSStaticData##n={\
  sizeof(UConverterStaticData),\
"LMBCS_" #n,\
    0, UCNV_IBM, UCNV_LMBCS_1, 1, 1,\
    1, { 0x3f, 0, 0, 0 } \
};\
const UConverterSharedData _LMBCSData##n={\
    sizeof(UConverterSharedData), ~0,\
    NULL, NULL, &_LMBCSStaticData##n, FALSE, &_LMBCSImpl##n, \
    0 \
};

DECLARE_LMBCS_DATA(1)
DECLARE_LMBCS_DATA(2)
DECLARE_LMBCS_DATA(3)
DECLARE_LMBCS_DATA(4)
DECLARE_LMBCS_DATA(5)
DECLARE_LMBCS_DATA(6)
DECLARE_LMBCS_DATA(8)
DECLARE_LMBCS_DATA(11)
DECLARE_LMBCS_DATA(16)
DECLARE_LMBCS_DATA(17)
DECLARE_LMBCS_DATA(18)
DECLARE_LMBCS_DATA(19)




