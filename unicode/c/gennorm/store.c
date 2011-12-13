/*
*******************************************************************************
*
*   Copyright (C) 1999-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  store.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001may25
*   created by: Markus W. Scherer
*
*   Store Unicode normalization data in a memory-mappable file.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/udata.h"
#include "unicode/uset.h"
#include "cmemory.h"
#include "filestrm.h"
#include "utrie.h"
#include "toolutil.h"
#include "writesrc.h"
#include "unormimp.h"
#include "gennorm.h"

#define DO_DEBUG_OUT 0

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

#if !UCONFIG_NO_NORMALIZATION

/* builder data ------------------------------------------------------------- */

static UNewTrie *normTrie;

static UToolMemory *normMem, *utf32Mem;

static Norm *norms;

static USet *compositionExclusions;

/* allocate and initialize a Norm unit */
static Norm *
allocNorm() {
    /* allocate Norm */
    Norm *p=(Norm *)utm_alloc(normMem);
    return p;
}

extern void
init() {
    uint16_t *p16;

    normTrie = (UNewTrie *)uprv_malloc(sizeof(UNewTrie));
    uprv_memset(normTrie, 0, sizeof(UNewTrie));

    /* initialize the two tries */
    if(NULL==utrie_open(normTrie, NULL, 30000, 0, 0, FALSE)) {
        fprintf(stderr, "error: failed to initialize tries\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }

    /* allocate Norm structures and reset the first one */
    normMem=utm_open("gennorm normalization structs", 20000, 20000, sizeof(Norm));
    norms=allocNorm();

    /* allocate UTF-32 string memory */
    utf32Mem=utm_open("gennorm UTF-32 strings", 30000, 30000, 4);

    compositionExclusions=uset_openEmpty();
}

/*
 * get or create a Norm unit;
 * get or create the intermediate trie entries for it as well
 */
static Norm *
createNorm(uint32_t code) {
    Norm *p;
    uint32_t i;

    i=utrie_get32(normTrie, (UChar32)code, NULL);
    if(i!=0) {
        p=norms+i;
    } else {
        /* allocate Norm */
        p=allocNorm();
        if(!utrie_set32(normTrie, (UChar32)code, (uint32_t)(p-norms))) {
            fprintf(stderr, "error: too many normalization entries\n");
            exit(U_BUFFER_OVERFLOW_ERROR);
        }
    }
    return p;
}

/* processing incoming normalization data ----------------------------------- */

/*
 * process the data for one code point listed in UnicodeData;
 * UnicodeData itself never maps a code point to both NFD and NFKD
 */
extern void
storeNorm(uint32_t code, Norm *norm) {
    Norm *p=createNorm(code);

    /* store the data */
    uprv_memcpy(p, norm, sizeof(Norm));

    /* store the decomposition string if there is one here */
    if(norm->lenNFD!=0) {
        uint32_t *s32=utm_allocN(utf32Mem, norm->lenNFD);
        uprv_memcpy(s32, norm->nfd, norm->lenNFD*4);
        p->nfd=s32;
    } else if(norm->lenNFKD!=0) {
        uint32_t *s32=utm_allocN(utf32Mem, norm->lenNFKD);
        uprv_memcpy(s32, norm->nfkd, norm->lenNFKD*4);
        p->nfkd=s32;
    }
}

extern void
setCompositionExclusion(uint32_t code) {
    uset_add(compositionExclusions, (UChar32)code);
}


static void
writeAllCC(FILE *f) {
    uint32_t i;
    UChar32 prevCode, code;
    uint8_t prevCC, cc;
    UBool isInBlockZero;

    fprintf(f, "# Canonical_Combining_Class (ccc) values\n");
    prevCode=0;
    prevCC=0;
    for(code=0; code<=0x110000;) {
        if(code==0x110000) {
            cc=0;
        } else {
            i=utrie_get32(normTrie, code, &isInBlockZero);
            if(i==0 || isInBlockZero) {
                cc=0;
            } else {
                cc=norms[i].udataCC;
            }
        }
        if(prevCC!=cc) {
            if(prevCC!=0) {
                uint32_t lastCode=code-1;
                if(prevCode==lastCode) {
                    fprintf(f, "%04lX:%d\n", (long)lastCode, prevCC);
                } else {
                    fprintf(f, "%04lX..%04lX:%d\n",
                            (long)prevCode, (long)lastCode, prevCC);
                }
            }
            prevCode=code;
            prevCC=cc;
        }
        if(isInBlockZero) {
            code+=UTRIE_DATA_BLOCK_LENGTH;
        } else {
            ++code;
        }
    }
}

static UBool
hasMapping(uint32_t code) {
    Norm *norm=norms+utrie_get32(normTrie, code, NULL);
    return norm->lenNFD!=0 || norm->lenNFKD!=0;
}

static UBool
hasOneWayMapping(uint32_t code, UBool withCompat) {
    for(;;) {
        Norm *norm=norms+utrie_get32(normTrie, code, NULL);
        uint8_t length;
        if((length=norm->lenNFD)!=0) {
            /*
             * The canonical decomposition is a one-way mapping if
             * - it does not map to exactly two code points
             * - the code has ccc!=0
             * - the code has the Composition_Exclusion property
             * - its starter has a one-way mapping (loop for this)
             * - its non-starter decomposes
             */
            if( length!=2 ||
                norm->udataCC!=0 ||
                uset_contains(compositionExclusions, (UChar32)code) ||
                hasMapping(norm->nfd[1])
            ) {
                return TRUE;
            }
            code=norm->nfd[0];  /* continue */
        } else if(withCompat && norm->lenNFKD!=0) {
            return TRUE;
        } else {
            return FALSE;
        }
    }
}

static void
writeAllMappings(FILE *f, UBool withCompat) {
    uint32_t i, code;
    UBool isInBlockZero;

    if(withCompat) {
        fprintf(f, "\n# Canonical and compatibility decomposition mappings\n");
    } else {
        fprintf(f, "\n# Canonical decomposition mappings\n");
    }
    for(code=0; code<=0x10ffff;) {
        i=utrie_get32(normTrie, code, &isInBlockZero);
        if(isInBlockZero) {
            code+=UTRIE_DATA_BLOCK_LENGTH;
        } else {
            if(i!=0) {
                uint32_t *s32;
                uint8_t length;
                char separator;
                if((length=norms[i].lenNFD)!=0) {
                    s32=norms[i].nfd;
                    separator= hasOneWayMapping(code, withCompat) ? '>' : '=';
                } else if(withCompat && (length=norms[i].lenNFKD)!=0) {
                    s32=norms[i].nfkd;
                    separator='>';
                }
                if(length!=0) {
                    uint8_t j;
                    fprintf(f, "%04lX%c", (long)code, separator);
                    for(j=0; j<length; ++j) {
                        if(j!=0) {
                            fputc(' ', f);
                        }
                        fprintf(f, "%04lX", (long)s32[j]);
                    }
                    fputc('\n', f);
                }
            }
            ++code;
        }
    }
}

static void
writeNorm2TextFile(const char *path, const char *filename, UBool withCompat) {
    FILE *f=usrc_createTextData(path, filename);
    if(f==NULL) {
        exit(U_FILE_ACCESS_ERROR);
    }
    writeAllCC(f);
    writeAllMappings(f, withCompat);
    fclose(f);
}

extern void
writeNorm2(const char *dataDir) {
    writeNorm2TextFile(dataDir, "nfc.txt", FALSE);
    writeNorm2TextFile(dataDir, "nfkc.txt", TRUE);
}

extern void
cleanUpData(void) {
    utm_close(normMem);
    utm_close(utf32Mem);
    utrie_close(normTrie);
    uprv_free(normTrie);
    uset_close(compositionExclusions);
}

#endif /* #if !UCONFIG_NO_NORMALIZATION */

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
