/*
*******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
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
#include "unicode/uchar.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "unormimp.h"
#include "gennorm.h"

#ifdef WIN32
#   pragma warning(disable: 4100)
#endif

#define DO_DEBUG_OUT 0

/* file data ---------------------------------------------------------------- */

/* UDataInfo cf. udata.h */
static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    { 0x4e, 0x6f, 0x72, 0x6d },   /* dataFormat="Norm" */
    {1, 0, 0, _NORM_TRIE_SHIFT},  /* formatVersion - [3] contains the trie shift! */
    {3, 1, 0, 0}                  /* dataVersion (Unicode version) */
};

extern void
setUnicodeVersion(const char *v) {
    UVersionInfo version;
    u_versionFromString(version, v);
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

static uint16_t indexes[_NORM_INDEX_TOP]={ 0 };

/* tool memory helper ------------------------------------------------------- */

/* ---*** PORATABILITY WARNING! ***---
On some 64-bit compilers the array field must be 64-bit aligned when you plan
to access the data. You can write the data, but you may not be able
to read the data back out. In general, custom memory management is discouraged.

An alternate solution to this is to either use the standard malloc, or to specify
that array is a union of several large basic C types.
*/
typedef struct UToolMemory {
    char name[64];
    uint32_t count, size, index;
    double array[1];
} UToolMemory;

static UToolMemory *
utm_open(const char *name, uint32_t count, uint32_t size) {
    UToolMemory *mem=(UToolMemory *)uprv_malloc(sizeof(UToolMemory)+count*size);
    if(mem==NULL) {
        fprintf(stderr, "error: %s - out of memory\n", name);
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    uprv_strcpy(mem->name, name);
    mem->count=count;
    mem->size=size;
    mem->index=0;
    return mem;
}

/* we don't use this - we don't clean up memory here... */
static void
utm_close(UToolMemory *mem) {
    if(mem!=NULL) {
        uprv_free(mem);
    }
}



static void *
utm_getStart(UToolMemory *mem) {
    return (char *)mem->array;
}

static void *
utm_alloc(UToolMemory *mem) {
    char *p=(char *)mem->array+mem->index*mem->size;
    if(++mem->index<=mem->count) {
        uprv_memset(p, 0, mem->size);
        return p;
    } else {
        fprintf(stderr, "error: %s - trying to use more than %ld preallocated units\n",
                mem->name, (long)mem->count);
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
}

static void *
utm_allocN(UToolMemory *mem, int32_t n) {
    char *p=(char *)mem->array+mem->index*mem->size;
    if((mem->index+=(uint32_t)n)<=mem->count) {
        uprv_memset(p, 0, n*mem->size);
        return p;
    } else {
        fprintf(stderr, "error: %s - trying to use more than %ld preallocated units\n",
                mem->name, (long)mem->count);
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
}

/* builder data ------------------------------------------------------------- */

typedef void EnumTrieFn(void *context, uint32_t code, Norm *norm);

static UToolMemory *stage2Mem, *normMem, *utf32Mem, *extraMem, *combiningTriplesMem;

static uint16_t stage1[_NORM_STAGE_1_MAX_COUNT], fcdStage1[_NORM_STAGE_1_MAX_COUNT];
static uint16_t *stage2;

static Norm *norms;

/*
 * set a flag for each code point that was seen in decompositions -
 * avoid to decompose ones that have not been used before
 */
static uint32_t haveSeenFlags[256];

static uint32_t combiningCPs[2000];
static uint16_t combiningIndexes[2000];
static uint16_t combineFwdTop=0, combineBothTop=0, combineBackTop=0;

typedef struct CombiningTriple {
    uint16_t leadIndex, trailIndex;
    uint32_t lead, trail, combined;
} CombiningTriple;

/* 15b in the combining index -> <=0x8000 pairs of uint16_t in the combining table */
static uint16_t combiningTable[2*0x8000];
static uint16_t combiningTableTop=0;

/* stage 2 table after turning Norm structs into 32-bit words */
static uint32_t *norm32Table=NULL, *fcdTable=NULL;

/* number of units used in stage 1 and norm32Table, and same for FCD */
static uint16_t stage1Top, fcdStage1Top,
                norm32TableTop, fcdTableTop;

extern void
init() {
    /* reset stage 1 of the trie */
    uprv_memset(stage1, 0, sizeof(stage1));

    /* allocate stage 2 of the trie and reset the first block */
    stage2Mem=utm_open("gennorm trie stage 2", 30000, sizeof(*stage2));
    stage2=utm_allocN(stage2Mem, _NORM_STAGE_2_BLOCK_COUNT);

    /* allocate Norm structures and reset the first one */
    normMem=utm_open("gennorm normalization structs", 20000, sizeof(Norm));
    norms=utm_alloc(normMem);

    /* allocate UTF-32 string memory */
    utf32Mem=utm_open("gennorm UTF-32 strings", 30000, 4);

    /* reset all "have seen" flags */
    uprv_memset(haveSeenFlags, 0, sizeof(haveSeenFlags));

    /* allocate extra data memory for UTF-16 decomposition strings and other values */
    extraMem=utm_open("gennorm extra 16-bit memory", _NORM_EXTRA_INDEX_TOP, 2);

    /* allocate temporary memory for combining triples */
    combiningTriplesMem=utm_open("gennorm combining triples", 0x4000, sizeof(CombiningTriple));

    /* set the minimum code points for no/maybe quick check values to the end of the BMP */
    indexes[_NORM_INDEX_MIN_NFC_NO_MAYBE]=0xffff;
    indexes[_NORM_INDEX_MIN_NFKC_NO_MAYBE]=0xffff;
    indexes[_NORM_INDEX_MIN_NFD_NO_MAYBE]=0xffff;
    indexes[_NORM_INDEX_MIN_NFKD_NO_MAYBE]=0xffff;
}

/* get or create a block in stage 2 of the trie */
static uint16_t
createStage2Block(uint32_t code) {
    uint32_t i;
    uint16_t j;

    i=code>>_NORM_TRIE_SHIFT;
    j=stage1[i];
    if(j==0) {
        /* allocate a stage 2 block */
        uint16_t *p;

        p=(uint16_t *)utm_allocN(stage2Mem, _NORM_STAGE_2_BLOCK_COUNT);
        stage1[i]=j=(uint16_t)(p-stage2);
    }
    return j;
}

/*
 * get or create a Norm unit;
 * get or create the intermediate trie entries for it as well
 */
static Norm *
createNorm(uint32_t code) {
    Norm *p;
    uint16_t stage2Block, k;

    stage2Block=createStage2Block(code);
    k=(uint16_t)(stage2Block+(code&_NORM_STAGE_2_MASK));
    if(stage2[k]==0) {
        /* allocate Norm */
        p=(Norm *)utm_alloc(normMem);
        stage2[k]=(uint16_t)(p-norms);
    } else {
        p=norms+stage2[k];
    }
    return p;
}

/* get an existing Norm unit */
static Norm *
getNorm(uint32_t code) {
    uint32_t i;
    uint16_t j;

    /* access stage 1 and get the stage 2 block start index */
    i=code>>_NORM_TRIE_SHIFT;
    j=stage1[i];
    if(j==0) {
        return NULL;
    }

    /* access stage 2 and get the Norm unit */
    i=(uint16_t)(j+(code&_NORM_STAGE_2_MASK));
    j=stage2[i];
    if(j==0) {
        return NULL;
    } else {
        return norms+j;
    }
}

/* get the canonical combining class of a character */
static uint8_t
getCCFromCP(uint32_t code) {
    Norm *norm=getNorm(code);
    if(norm==NULL) {
        return 0;
    } else {
        return norm->udataCC;
    }
}

/*
 * enumerate all code points with their Norm structs and call a function for each
 * return the number of code points with data
 */
static uint32_t
enumTrie(EnumTrieFn *fn, void *context) {
    uint32_t code, count, i;
    uint16_t j, k, l;

    code=0;
    count=0;
    for(i=0; i<_NORM_STAGE_1_MAX_COUNT; ++i) {
        j=stage1[i];
        if(j!=0) {
            for(k=0; k<_NORM_STAGE_2_BLOCK_COUNT; ++k) {
                l=stage2[j+k];
                if(l!=0) {
                    fn(context, code, norms+l);
                    ++count;
                }
                ++code;
            }
        } else {
            code+=_NORM_STAGE_2_BLOCK_COUNT;
        }
    }
    return count;
}

static void
setHaveSeenString(const uint32_t *s, int32_t length) {
    uint32_t c;

    while(length>0) {
        c=*s++;
        haveSeenFlags[(c>>5)&0xff]|=(1<<(c&0x1f));
        --length;
    }
}

#define HAVE_SEEN(c) (haveSeenFlags[((c)>>5)&0xff]&(1<<((c)&0x1f)))

/* handle combining data ---------------------------------------------------- */

static void
addCombiningCP(uint32_t code, uint8_t flags) {
    uint32_t newEntry;
    uint16_t i;

    newEntry=code|((uint32_t)flags<<24);

    /* search for this code point */
    for(i=0; i<combineBackTop; ++i) {
        if(code==(combiningCPs[i]&0xffffff)) {
            /* found it */
            if(newEntry==combiningCPs[i]) {
                return; /* no change */
            }

            /* combine the flags, remove the old entry from the old place, and insert the new one */
            newEntry|=combiningCPs[i];
            if(i!=--combineBackTop) {
                uprv_memmove(combiningCPs+i, combiningCPs+i+1, (combineBackTop-i)*4);
            }
            if(i<combineBothTop) {
                --combineBothTop;
            }
            if(i<combineFwdTop) {
                --combineFwdTop;
            }
            break;
        }
    }

    /* not found or modified, insert it */
    if(combineBackTop>=sizeof(combiningCPs)/4) {
        fprintf(stderr, "error: gennorm combining code points - trying to use more than %ld units\n",
                (long)(sizeof(combiningCPs)/4));
        exit(U_MEMORY_ALLOCATION_ERROR);
    }

    /* set i to the insertion point */
    flags=(uint8_t)(newEntry>>24);
    if(flags==1) {
        i=combineFwdTop++;
        ++combineBothTop;
    } else if(flags==3) {
        i=combineBothTop++;
    } else /* flags==2 */ {
        i=combineBackTop;
    }

    /* move the following code points up one and insert newEntry at i */
    if(i<combineBackTop) {
        uprv_memmove(combiningCPs+i+1, combiningCPs+i, (combineBackTop-i)*4);
    }
    combiningCPs[i]=newEntry;

    /* finally increment the total counter */
    ++combineBackTop;
}

static uint16_t
findCombiningCP(uint32_t code, UBool isLead) {
    uint16_t i, limit;

    if(isLead) {
        i=0;
        limit=combineBothTop;
    } else {
        i=combineFwdTop;
        limit=combineBackTop;
    }

    /* search for this code point */
    for(; i<limit; ++i) {
        if(code==(combiningCPs[i]&0xffffff)) {
            /* found it */
            return i;
        }
    }

    /* not found */
    return 0xffff;
}

static void
addCombiningTriple(uint32_t lead, uint32_t trail, uint32_t combined) {
    CombiningTriple *triple;

    /*
     * set combiningFlags for the two code points
     * do this after decomposition so that getNorm() above returns NULL
     * if we do not have actual sub-decomposition data for the initial NFD here
     */
    createNorm(lead)->combiningFlags|=1;    /* combines forward */
    createNorm(trail)->combiningFlags|=2;    /* combines backward */

    addCombiningCP(lead, 1);
    addCombiningCP(trail, 2);

    triple=(CombiningTriple *)utm_alloc(combiningTriplesMem);
    triple->lead=lead;
    triple->trail=trail;
    triple->combined=combined;
}

static int
compareTriples(const void *l, const void *r) {
    int diff;
    diff=(int)((CombiningTriple *)l)->leadIndex-
         (int)((CombiningTriple *)r)->leadIndex;
    if(diff==0) {
        diff=(int)((CombiningTriple *)l)->trailIndex-
             (int)((CombiningTriple *)r)->trailIndex;
    }
    return diff;
}

static void
processCombining() {
    CombiningTriple *triples;
    uint16_t *p;
    uint32_t combined;
    uint16_t i, j, count, tableTop, finalIndex, combinesFwd;

    triples=utm_getStart(combiningTriplesMem);

    /* add lead and trail indexes to the triples for sorting */
    count=(uint16_t)combiningTriplesMem->index;
    for(i=0; i<count; ++i) {
        /* findCombiningCP() must always find the code point */
        triples[i].leadIndex=findCombiningCP(triples[i].lead, TRUE);
        triples[i].trailIndex=findCombiningCP(triples[i].trail, FALSE);
    }

    /* sort them by leadIndex, trailIndex */
    qsort(triples, count, sizeof(CombiningTriple), compareTriples);

    /* calculate final combining indexes and store them in the Norm entries */
    tableTop=0;
    j=0; /* triples counter */

    /* first, combining indexes of fwd/both characters are indexes into the combiningTable */
    for(i=0; i<combineBothTop; ++i) {
        /* start a new table */

        /* assign combining index */
        createNorm(combiningCPs[i]&0xffffff)->combiningIndex=combiningIndexes[i]=tableTop;

        /* calculate the length of the combining data for this lead code point in the combiningTable */
        while(j<count && i==triples[j].leadIndex) {
            /* count 2 16-bit units per composition code unit */
            combined=triples[j++].combined;
            if(combined<=0x1fff) {
                tableTop+=2;
            } else {
                tableTop+=3;
            }
        }
    }

    /* second, combining indexes of back-only characters are simply incremented from here to be unique */
    finalIndex=tableTop;
    for(; i<combineBackTop; ++i) {
        createNorm(combiningCPs[i]&0xffffff)->combiningIndex=combiningIndexes[i]=finalIndex++;
    }

    /* it must be tableTop<=0x7fff because bit 15 is used in combiningTable as an end-for-this-lead marker */
    if(tableTop>=sizeof(combiningTable)/4) {
        fprintf(stderr, "error: gennorm combining table - trying to use %u units, more than the %ld units available\n",
                tableTop, (long)(sizeof(combiningTable)/4));
        exit(U_MEMORY_ALLOCATION_ERROR);
    }

    combiningTableTop=tableTop;

    /* store the combining data in the combiningTable, with the final indexes from above */
    p=combiningTable;
    j=0; /* triples counter */

    /*
     * this is essentially the same loop as above, but
     * it writes the table data instead of calculating and setting the final indexes;
     * it is necessary to have two passes so that all the final indexes are known before
     * they are written into the table
     */
    for(i=0; i<combineBothTop; ++i) {
        /* start a new table */

        combined=0; /* avoid compiler warning */

        /* store the combining data for this lead code point in the combiningTable */
        while(j<count && i==triples[j].leadIndex) {
            finalIndex=combiningIndexes[triples[j].trailIndex];
            combined=triples[j++].combined;

            /* is combined a starter? (i.e., cc==0 && combines forward) */
            combinesFwd=(uint16_t)((getNorm(combined)->combiningFlags&1)<<13);

            *p++=finalIndex;
            if(combined<=0x1fff) {
                *p++=(uint16_t)(combinesFwd|combined);
            } else if(combined<=0xffff) {
                *p++=(uint16_t)(0x8000|combinesFwd);
                *p++=(uint16_t)combined;
            } else {
                *p++=(uint16_t)(0xc000|combinesFwd|((combined-0x10000)>>10));
                *p++=(uint16_t)(0xdc00|(combined&0x3ff));
            }
        }

        /* set a marker on the last final trail index in this lead's table */
        if(combined<=0x1ffff) {
            *(p-2)|=0x8000;
        } else {
            *(p-3)|=0x8000;
        }
    }

    /* post condition: tableTop==(p-combiningTable) */
}

/* processing incoming normalization data ----------------------------------- */

/*
 * decompose the one decomposition further, may generate two decompositions
 * apply all previous characters' decompositions to this one
 */
static void
decompStoreNewNF(uint32_t code, Norm *norm) {
    uint32_t nfd[40], nfkd[40];
    uint32_t *s32;
    Norm *p;
    uint32_t c;
    int32_t i, length;
    uint8_t lenNFD=0, lenNFKD=0;
    UBool changedNFD=FALSE, changedNFKD=FALSE;

    if((length=norm->lenNFD)!=0) {
        /* always allocate the original string */
        changedNFD=TRUE;
        s32=norm->nfd;
    } else if((length=norm->lenNFKD)!=0) {
        /* always allocate the original string */
        changedNFKD=TRUE;
        s32=norm->nfkd;
    } else {
        /* no decomposition here, nothing to do */
        return;
    }

    /* decompose each code point */
    for(i=0; i<length; ++i) {
        c=s32[i];
        p=getNorm(c);
        if(p==NULL) {
            /* no data, no decomposition */
            nfd[lenNFD++]=c;
            nfkd[lenNFKD++]=c;
            continue;
        }

        /* canonically decompose c */
        if(changedNFD) {
            if(p->lenNFD!=0) {
                uprv_memcpy(nfd+lenNFD, p->nfd, p->lenNFD*4);
                lenNFD+=p->lenNFD;
            } else {
                nfd[lenNFD++]=c;
            }
        }

        /* compatibility-decompose c */
        if(p->lenNFKD!=0) {
            uprv_memcpy(nfkd+lenNFKD, p->nfkd, p->lenNFKD*4);
            lenNFKD+=p->lenNFKD;
            changedNFKD=TRUE;
        } else if(p->lenNFD!=0) {
            uprv_memcpy(nfkd+lenNFKD, p->nfd, p->lenNFD*4);
            lenNFKD+=p->lenNFD;
            changedNFKD=TRUE;
        } else {
            nfkd[lenNFKD++]=c;
        }
    }

    /* assume that norm->lenNFD==1 or ==2 */
    if(norm->lenNFD==2 && !(norm->combiningFlags&0x80)) {
        addCombiningTriple(s32[0], s32[1], code);
    }

    if(changedNFD) {
        if(lenNFD!=0) {
            s32=utm_allocN(utf32Mem, lenNFD);
            uprv_memcpy(s32, nfd, lenNFD*4);
        } else {
            s32=NULL;
        }
        norm->lenNFD=lenNFD;
        norm->nfd=s32;
        setHaveSeenString(nfd, lenNFD);
    }
    if(changedNFKD) {
        if(lenNFKD!=0) {
            s32=utm_allocN(utf32Mem, lenNFKD);
            uprv_memcpy(s32, nfkd, lenNFKD*4);
        } else {
            s32=NULL;
        }
        norm->lenNFKD=lenNFKD;
        norm->nfkd=s32;
        setHaveSeenString(nfkd, lenNFKD);
    }
}

typedef struct DecompSingle {
    uint32_t c;
    Norm *norm;
} DecompSingle;

/*
 * apply this one character's decompositions (there is at least one!) to
 * all previous characters' decompositions to decompose them further
 */
static void
decompWithSingleFn(void *context, uint32_t code, Norm *norm) {
    uint32_t nfd[40], nfkd[40];
    uint32_t *s32;
    DecompSingle *me=(DecompSingle *)context;
    uint32_t c, myC;
    int32_t i, length;
    uint8_t lenNFD=0, lenNFKD=0, myLenNFD, myLenNFKD;
    UBool changedNFD=FALSE, changedNFKD=FALSE;

    /* get the new character's data */
    myC=me->c;
    myLenNFD=me->norm->lenNFD;
    myLenNFKD=me->norm->lenNFKD;
    /* assume that myC has at least one decomposition */

    if((length=norm->lenNFD)!=0 && myLenNFD!=0) {
        /* apply NFD(myC) to norm->nfd */
        s32=norm->nfd;
        for(i=0; i<length; ++i) {
            c=s32[i];
            if(c==myC) {
                uprv_memcpy(nfd+lenNFD, me->norm->nfd, myLenNFD*4);
                lenNFD+=myLenNFD;
                changedNFD=TRUE;
            } else {
                nfd[lenNFD++]=c;
            }
        }
    }

    if((length=norm->lenNFKD)!=0) {
        /* apply NFD(myC) and NFKD(myC) to norm->nfkd */
        s32=norm->nfkd;
        for(i=0; i<length; ++i) {
            c=s32[i];
            if(c==myC) {
                if(myLenNFKD!=0) {
                    uprv_memcpy(nfkd+lenNFKD, me->norm->nfkd, myLenNFKD*4);
                    lenNFKD+=myLenNFKD;
                } else /* assume myLenNFD!=0 */ {
                    uprv_memcpy(nfkd+lenNFKD, me->norm->nfd, myLenNFD*4);
                    lenNFKD+=myLenNFD;
                }
                changedNFKD=TRUE;
            } else {
                nfkd[lenNFKD++]=c;
            }
        }
    } else if((length=norm->lenNFD)!=0 && myLenNFKD!=0) {
        /* apply NFKD(myC) to norm->nfd, forming a new norm->nfkd */
        s32=norm->nfd;
        for(i=0; i<length; ++i) {
            c=s32[i];
            if(c==myC) {
                uprv_memcpy(nfkd+lenNFKD, me->norm->nfkd, myLenNFKD*4);
                lenNFKD+=myLenNFKD;
                changedNFKD=TRUE;
            } else {
                nfkd[lenNFKD++]=c;
            }
        }
    }

    /* set the new decompositions, forget the old ones */
    if(changedNFD) {
        if(lenNFD!=0) {
            if(lenNFD>norm->lenNFD) {
                s32=utm_allocN(utf32Mem, lenNFD);
            } else {
                s32=norm->nfd;
            }
            uprv_memcpy(s32, nfd, lenNFD*4);
        } else {
            s32=NULL;
        }
        norm->lenNFD=lenNFD;
        norm->nfd=s32;
    }
    if(changedNFKD) {
        if(lenNFKD!=0) {
            if(lenNFKD>norm->lenNFKD) {
                s32=utm_allocN(utf32Mem, lenNFKD);
            } else {
                s32=norm->nfkd;
            }
            uprv_memcpy(s32, nfkd, lenNFKD*4);
        } else {
            s32=NULL;
        }
        norm->lenNFKD=lenNFKD;
        norm->nfkd=s32;
    }
}

/*
 * process the data for one code point listed in UnicodeData;
 * UnicodeData itself never maps a code point to both NFD and NFKD
 */
extern void
storeNorm(uint32_t code, Norm *norm) {
    DecompSingle decompSingle;
    Norm *p;

    /* copy existing derived normalization properties */
    p=createNorm(code);
    norm->qcFlags=p->qcFlags;
    norm->combiningFlags=p->combiningFlags;

    /* process the decomposition if if there is at one here */
    if((norm->lenNFD|norm->lenNFKD)!=0) {
        /* decompose this one decomposition further, may generate two decompositions */
        decompStoreNewNF(code, norm);

        /* has this code point been used in previous decompositions? */
        if(HAVE_SEEN(code)) {
            /* use this decomposition to decompose other decompositions further */
            decompSingle.c=code;
            decompSingle.norm=norm;
            enumTrie(decompWithSingleFn, &decompSingle);
        }
    }

    /* store the data */
    uprv_memcpy(p, norm, sizeof(Norm));
}

extern void
setQCFlags(uint32_t code, uint8_t qcFlags) {
    createNorm(code)->qcFlags|=qcFlags;

    /* adjust the minimum code point for quick check no/maybe */
    if(code<0xffff) {
        if((qcFlags&_NORM_QC_NFC) && (uint16_t)code<indexes[_NORM_INDEX_MIN_NFC_NO_MAYBE]) {
            indexes[_NORM_INDEX_MIN_NFC_NO_MAYBE]=(uint16_t)code;
        }
        if((qcFlags&_NORM_QC_NFKC) && (uint16_t)code<indexes[_NORM_INDEX_MIN_NFKC_NO_MAYBE]) {
            indexes[_NORM_INDEX_MIN_NFKC_NO_MAYBE]=(uint16_t)code;
        }
        if((qcFlags&_NORM_QC_NFD) && (uint16_t)code<indexes[_NORM_INDEX_MIN_NFD_NO_MAYBE]) {
            indexes[_NORM_INDEX_MIN_NFD_NO_MAYBE]=(uint16_t)code;
        }
        if((qcFlags&_NORM_QC_NFKD) && (uint16_t)code<indexes[_NORM_INDEX_MIN_NFKD_NO_MAYBE]) {
            indexes[_NORM_INDEX_MIN_NFKD_NO_MAYBE]=(uint16_t)code;
        }
    }
}

extern void
setCompositionExclusion(uint32_t code) {
    createNorm(code)->combiningFlags|=0x80;
}

static void
setHangulJamoSpecials() {
    Norm *norm;
    uint16_t *pStage2Block;
    uint32_t c;
    uint16_t i;

    /*
     * Hangul syllables are algorithmically decomposed into Jamos,
     * and Jamos are algorithmically composed into Hangul syllables.
     * The quick check flags are parsed, except for Hangul.
     */

    /* set Jamo 1 specials */
    for(c=0x1100; c<=0x1112; ++c) {
        norm=createNorm(c);
        norm->specialTag=_NORM_EXTRA_INDEX_TOP+_NORM_EXTRA_JAMO_1;
        norm->combiningFlags=1;
    }

    /* set Jamo 2 specials */
    for(c=0x1161; c<=0x1175; ++c) {
        norm=createNorm(c);
        norm->specialTag=_NORM_EXTRA_INDEX_TOP+_NORM_EXTRA_JAMO_2;
        norm->combiningFlags=2;
    }

    /* set Jamo 3 specials */
    for(c=0x11a8; c<=0x11c2; ++c) {
        norm=createNorm(c);
        norm->specialTag=_NORM_EXTRA_INDEX_TOP+_NORM_EXTRA_JAMO_3;
        norm->combiningFlags=2;
    }

    /* set Hangul specials, precompacted */
    norm=(Norm *)utm_alloc(normMem);
    norm->specialTag=_NORM_EXTRA_INDEX_TOP+_NORM_EXTRA_HANGUL;
    norm->qcFlags=_NORM_QC_NFD|_NORM_QC_NFKD;

    /* set one complete stage 2 block with this Hangul information */
    pStage2Block=(uint16_t *)utm_allocN(stage2Mem, _NORM_STAGE_2_BLOCK_COUNT);
    for(i=0; i<_NORM_STAGE_2_BLOCK_COUNT; ++i) {
        pStage2Block[i]=(uint16_t)(norm-norms);
    }

    /* set these data for U+ac00..U+d7a3 */
    c=0xac00;

    /* set a partial stage 2 block before pStage2Block can be repeated */
    if(c&_NORM_STAGE_2_MASK) {
        i=(uint16_t)(createStage2Block(c)+(c&_NORM_STAGE_2_MASK));
        do {
            stage2[i++]=(uint16_t)(norm-norms);
        } while(++c&_NORM_STAGE_2_MASK);
    }

    /* set full stage 1 blocks to the common stage 2 block */
    while(c<(0xd7a3&~_NORM_STAGE_2_MASK)) {
        stage1[c>>_NORM_TRIE_SHIFT]=(uint16_t)(pStage2Block-stage2);
        c+=_NORM_STAGE_2_BLOCK_COUNT;
    }

    /* set a partial stage 2 block after the repetition */
    i=createStage2Block(c);
    while(c<=0xd7a3) {
        stage2[i++]=(uint16_t)(norm-norms);
        ++c;
    }
}

/* build runtime structures ------------------------------------------------- */

/* canonically reorder a UTF-32 string; return { leadCC, trailCC } */
static uint16_t
reorderString(uint32_t *s, int32_t length) {
    uint8_t ccs[40];
    uint32_t c;
    int32_t i, j;
    uint8_t cc, prevCC;

    if(length<=0) {
        return 0;
    }

    for(i=0; i<length; ++i) {
        /* get the i-th code point and its combining class */
        c=s[i];
        cc=getCCFromCP(c);
        if(cc!=0 && i!=0) {
            /* it is a combining mark, see if it needs to be moved back */
            j=i;
            do {
                prevCC=ccs[j-1];
                if(prevCC<=cc) {
                    break;  /* found the right place */
                }
                /* move the previous code point here and go back */
                s[j]=s[j-1];
                ccs[j]=prevCC;
            } while(--j!=0);
            s[j]=c;
            ccs[j]=cc;
        } else {
            /* just store the combining class */
            ccs[i]=cc;
        }
    }

    return (uint16_t)(((uint16_t)ccs[0]<<8)|ccs[length-1]);
}

static UBool combineAndQC[64]={ 0 };

/*
 * canonically reorder the up to two decompositions
 * and store the leading and trailing combining classes accordingly
 */
static void
postParseFn(void *context, uint32_t code, Norm *norm) {
    int32_t length;

    /* canonically order the NFD */
    length=norm->lenNFD;
    if(length>0) {
        norm->canonBothCCs=reorderString(norm->nfd, length);
    }

    /* canonically reorder the NFKD */
    length=norm->lenNFKD;
    if(length>0) {
        norm->compatBothCCs=reorderString(norm->nfkd, length);
    }

    /* verify that code has a decomposition if and only if the quick check flags say "no" on NF(K)D */
    if((norm->lenNFD!=0) != ((norm->qcFlags&_NORM_QC_NFD)!=0)) {
        printf("U+%04lx has NFD[%d] but quick check 0x%02x\n", (long)code, norm->lenNFD, norm->qcFlags);
    }
    if(((norm->lenNFD|norm->lenNFKD)!=0) != ((norm->qcFlags&(_NORM_QC_NFD|_NORM_QC_NFKD))!=0)) {
        printf("U+%04lx has NFD[%d] NFKD[%d] but quick check 0x%02x\n", (long)code, norm->lenNFD, norm->lenNFKD, norm->qcFlags);
    }

    /* ### see which combinations of combiningFlags and qcFlags are used for NFC/NFKC */
    combineAndQC[(norm->qcFlags&0x33)|((norm->combiningFlags&3)<<2)]=1;

    if(norm->combiningFlags&1) {
        if(norm->udataCC!=0) {
            /* illegal - data-derivable composition exclusion */
            printf("U+%04lx combines forward but udataCC==%u\n", (long)code, norm->udataCC);
        }
    }
    if(norm->combiningFlags&2) {
        if((norm->qcFlags&0x11)==0) {
            printf("U+%04lx combines backward but qcNF?C==0\n", (long)code);
        }
#if 0
        /* occurs sometimes */
        if(norm->udataCC==0) {
            printf("U+%04lx combines backward but udataCC==0\n", (long)code);
        }
#endif
    }
    if((norm->combiningFlags&3)==3) {
        printf("U+%04lx combines both ways\n", (long)code);
    }
}

/* ### debug */
static uint32_t countCCSame=0, countCCTrail=0, countCCTwo=0;

static uint32_t
make32BitNorm(Norm *norm) {
    UChar extra[100];
    uint32_t word;
    int32_t i, length, beforeZero=0, count, start;

    /* reset the 32-bit word and set the quick check flags */
    word=norm->qcFlags;

    /* set the UnicodeData combining class */
    word|=(uint32_t)norm->udataCC<<_NORM_CC_SHIFT;

    /* set the combining flag and index */
    if(norm->combiningFlags&3) {
        word|=(uint32_t)(norm->combiningFlags&3)<<6;
    }

    /* set the combining index value into the extra data */
    if(norm->combiningIndex!=0) {
        extra[0]=norm->combiningIndex;
        beforeZero=1;
    }

    count=beforeZero;

    /* write the decompositions */
    if((norm->lenNFD|norm->lenNFKD)!=0) {
        extra[count++]=0; /* set the pieces when available, into extra[beforeZero] */

        length=norm->lenNFD;
        if(length>0) {
            if(norm->canonBothCCs!=0) {
                extra[beforeZero]|=0x80;
                extra[count++]=norm->canonBothCCs;
            }
            start=count;
            for(i=0; i<length; ++i) {
                UTF_APPEND_CHAR_UNSAFE(extra, count, norm->nfd[i]);
            }
            extra[beforeZero]|=(UChar)(count-start); /* set the decomp length as the number of UTF-16 code units */
        }

        length=norm->lenNFKD;
        if(length>0) {
            if(norm->compatBothCCs!=0) {
                extra[beforeZero]|=0x8000;
                extra[count++]=norm->compatBothCCs;
            }
            start=count;
            for(i=0; i<length; ++i) {
                UTF_APPEND_CHAR_UNSAFE(extra, count, norm->nfkd[i]);
            }
            extra[beforeZero]|=(UChar)((count-start)<<8); /* set the decomp length as the number of UTF-16 code units */
        }
    }

    /* allocate and copy the extra data */
    if(count!=0) {
        UChar *p;

        if(norm->specialTag!=0) {
            fprintf(stderr, "error: gennorm - illegal to have both extra data and a special tag (0x%x)\n", norm->specialTag);
            exit(U_ILLEGAL_ARGUMENT_ERROR);
        }

        p=(UChar *)utm_allocN(extraMem, count);
        uprv_memcpy(p, extra, count*2);

        /* set the extra index, offset by beforeZero */
        word|=(uint32_t)(beforeZero+(p-(UChar *)utm_getStart(extraMem)))<<_NORM_EXTRA_SHIFT;
    } else if(norm->specialTag!=0) {
        /* set a special tag instead of an extra index */
        word|=(uint32_t)norm->specialTag<<_NORM_EXTRA_SHIFT;
    }

    return word;
}

/* turn all Norm structs into corresponding 32-bit norm values */
static void
makeAll32() {
    uint16_t i, count;

    /*
     * allocate and fill the table of 32-bit normalization data
     * leave space for data for the up to 1024 lead surrogates
     */
    norm32TableTop=(uint16_t)stage2Mem->index;
    norm32Table=(uint32_t *)uprv_malloc((norm32TableTop+1024)*4);
    if(norm32Table==NULL) {
        fprintf(stderr, "error: gennorm - unable to allocate %ld 32-bit words for norm32Table\n",
                (long)(norm32TableTop+1024));
        exit(U_MEMORY_ALLOCATION_ERROR);
    }

    /* reset all entries */
    uprv_memset(norm32Table, 0, (norm32TableTop+1024)*4);

    count=0;

    /* skip the first, all-empty block */
    for(i=_NORM_STAGE_2_BLOCK_COUNT; i<norm32TableTop; ++i) {
        if(stage2[i]!=0) {
            if(0!=(norm32Table[i]=make32BitNorm(norms+stage2[i]))) {
                ++count;
            }
        }
    }

    printf("count of 16-bit extra data: %lu\n", (long)extraMem->index);
    printf("count of (uncompacted) non-zero 32-bit words: %lu\n", (long)count);
    printf("count CC frequencies: same %lu  trail %lu  two %lu\n",
            (long)countCCSame, (long)countCCTrail, (long)countCCTwo);
}

/*
 * extract all Norm.canonBothCCs into the FCD table
 * set 32-bit values to use the common fold and compact functions
 */
static void
makeFCD() {
    static uint16_t map[0x10000>>_NORM_TRIE_SHIFT];
    Norm *norm;
    uint32_t i, oredValues;
    uint16_t bothCCs, delta;

    /*
     * allocate and fill the table of 32-bit normalization data
     * leave space for data for the up to 1024 lead surrogates
     */
    fcdTableTop=(uint16_t)stage2Mem->index;
    fcdTable=(uint32_t *)uprv_malloc((fcdTableTop+1024)*4);
    if(fcdTable==NULL) {
        fprintf(stderr, "error: gennorm - unable to allocate %ld 32-bit words for fcdTable\n",
                (long)(fcdTableTop+1024));
        exit(U_MEMORY_ALLOCATION_ERROR);
    }

    /* reset all entries */
    uprv_memset(fcdTable, 0, (fcdTableTop+1024)*4);

    /* compact out the all-zero stage 2 blocks */
    map[0]=0;
    delta=0;

    /* oredValues detects all-zero stage 2 blocks that will be removed from fcdStage1 */
    oredValues=0;

    /* skip the first, all-empty block */
    for(i=_NORM_STAGE_2_BLOCK_COUNT; i<fcdTableTop; ++i) {
        if(stage2[i]!=0) {
            norm=norms+stage2[i];
            bothCCs=norm->canonBothCCs;
            if(bothCCs==0) {
                /* if there are no decomposition cc's then use the udataCC twice */
                bothCCs=norm->udataCC;
                bothCCs|=bothCCs<<8;
            }
            oredValues|=fcdTable[i-delta]=bothCCs;
        }

        if((i&_NORM_STAGE_2_MASK)==_NORM_STAGE_2_MASK) {
            /* at the end of a stage 2 block, check if there are any non-zero entries */
            if(oredValues==0) {
                /* all zero: skip this block */
                delta+=_NORM_STAGE_2_BLOCK_COUNT;
                map[i>>_NORM_TRIE_SHIFT]=(uint16_t)0;
            } else {
                /* keep this block */
                map[i>>_NORM_TRIE_SHIFT]=(uint16_t)((i&~_NORM_STAGE_2_MASK)-delta);
                oredValues=0;
            }
        }
    }

    /* now adjust stage 1 */
    for(i=0; i<_NORM_STAGE_1_MAX_COUNT; ++i) {
        fcdStage1[i]=map[fcdStage1[i]>>_NORM_TRIE_SHIFT];
    }

    printf("FCD: omitted %u stage 2 entries in all-zero blocks\n", delta);

    /* adjust the table top */
    fcdTableTop-=delta;
}

/*
 * Fold the supplementary code point data for one lead surrogate.
 */
static uint16_t
foldLeadSurrogate(uint16_t *parent, uint16_t parentCount,
                  uint32_t *stage, uint16_t *pStageCount,
                  uint32_t base,
                  UBool isNorm32) {
    uint32_t leadNorm32=0;
    uint32_t i, j, s2;
    uint32_t leadSurrogate=0xd7c0+(base>>10);

    printf("supplementary data for lead surrogate U+%04lx\n", (long)leadSurrogate);

    /* calculate the 32-bit data word for the lead surrogate */
    for(i=0; i<_NORM_SURROGATE_BLOCK_COUNT; ++i) {
        s2=parent[(base>>_NORM_TRIE_SHIFT)+i];
        if(s2!=0) {
            for(j=0; j<_NORM_STAGE_2_BLOCK_COUNT; ++j) {
                /* basically, or all 32-bit data into the one for the lead surrogate */
                leadNorm32|=stage[s2+j];
            }
        }
    }

    if(isNorm32) {
        /* turn multi-bit fields into the worst-case value */
        if(leadNorm32&_NORM_CC_MASK) {
            leadNorm32|=_NORM_CC_MASK;
        }

        /* clean up unnecessarily ored bit fields */
        leadNorm32&=~((uint32_t)0xffffffff<<_NORM_EXTRA_SHIFT);

        if(leadNorm32==0) {
            /* nothing to do (only composition exclusions?) */
            return 0;
        }

        /* add the extra surrogate index, offset by the BMP top, for the new stage 1 location */
        leadNorm32|=(
            (uint32_t)_NORM_EXTRA_INDEX_TOP+
            (uint32_t)((parentCount-_NORM_STAGE_1_BMP_COUNT)>>_NORM_SURROGATE_BLOCK_BITS)
        )<<_NORM_EXTRA_SHIFT;
    } else {
        if(leadNorm32==0) {
            /* FCD: nothing to do */
            return 0;
        }

        /*
         * For FCD, replace the entire combined value by the surrogate index
         * and make sure that it is not 0 (by not offsetting it by the BMP top,
         * since here we have enough bits for this);
         * lead surrogates are tested at runtime on the character code itself
         * instead on special values of the trie data -
         * this is because 16 bits in the FCD trie data do not allow for anything
         * but the two leading and trailing combining classes of the canonical decomposition.
         */
        leadNorm32=parentCount>>_NORM_SURROGATE_BLOCK_BITS;
    }

    /* enter the lead surrogate's data */
    s2=parent[leadSurrogate>>_NORM_TRIE_SHIFT];
    if(s2==0) {
        /* allocate a new stage 2 block in stage (the memory is there from makeAll32()/makeFCD()) */
        s2=parent[leadSurrogate>>_NORM_TRIE_SHIFT]=*pStageCount;
        *pStageCount+=_NORM_STAGE_2_BLOCK_COUNT;
    }
    stage[s2+(leadSurrogate&_NORM_STAGE_2_MASK)]=leadNorm32;

    /* move the actual stage 1 indexes from the supplementary position to the new one */
    uprv_memmove(parent+parentCount, parent+(base>>_NORM_TRIE_SHIFT), _NORM_SURROGATE_BLOCK_COUNT*2);

    /* increment stage 1 top */
    return _NORM_SURROGATE_BLOCK_COUNT;
}

/*
 * Fold the normalization data for supplementary code points into
 * a compact area on top of the BMP-part of the trie index,
 * with the lead surrogates indexing this compact area.
 *
 * Use after makeAll32().
 */
static uint16_t
foldSupplementary(uint16_t *parent, uint16_t parentCount,
                  uint32_t *stage, uint16_t *pStageCount,
                  UBool isNorm32) {
    uint32_t c;
    uint16_t i;

    /* search for any stage 1 entries for supplementary code points */
    for(c=0x10000; c<0x110000;) {
        i=parent[c>>_NORM_TRIE_SHIFT];
        if(i!=0) {
            /* there is data, treat the full block for a lead surrogate */
            c&=~0x3ff;
            parentCount+=foldLeadSurrogate(parent, parentCount, stage, pStageCount, c, isNorm32);
            c+=0x400;
        } else {
            c+=_NORM_STAGE_2_BLOCK_COUNT;
        }
    }

    printf("trie index count: BMP %u  all Unicode %lu  folded %u\n",
           _NORM_STAGE_1_BMP_COUNT, (long)_NORM_STAGE_1_MAX_COUNT, parentCount);
    return parentCount;
}

static uint16_t
compact(uint16_t *parent, uint16_t parentCount,
        uint32_t *stage, uint16_t stageCount) {
    /*
     * This function is the common implementation for compacting
     * the stage 2 tables of 32-bit values.
     * It is a copy of genprops/store.c's compactStage() adapted for the 32-bit stage 2 tables.
     */
    static uint16_t map[0x10000>>_NORM_TRIE_SHIFT];
    uint32_t x;
    uint16_t i, start, prevEnd, newStart;

    map[0]=0;
    newStart=_NORM_STAGE_2_BLOCK_COUNT;
    for(start=newStart; start<stageCount;) {
        prevEnd=(uint16_t)(newStart-1);
        x=stage[start];
        if(x==stage[prevEnd]) {
            /* overlap by at least one */
            for(i=1; i<_NORM_STAGE_2_BLOCK_COUNT && x==stage[start+i] && x==stage[prevEnd-i]; ++i) {}

            /* overlap by i */
            map[start>>_NORM_TRIE_SHIFT]=(uint16_t)(newStart-i);

            /* move the non-overlapping indexes to their new positions */
            start+=i;
            for(i=(uint16_t)(_NORM_STAGE_2_BLOCK_COUNT-i); i>0; --i) {
                stage[newStart++]=stage[start++];
            }
        } else if(newStart<start) {
            /* move the indexes to their new positions */
            map[start>>_NORM_TRIE_SHIFT]=newStart;
            for(i=_NORM_STAGE_2_BLOCK_COUNT; i>0; --i) {
                stage[newStart++]=stage[start++];
            }
        } else /* no overlap && newStart==start */ {
            map[start>>_NORM_TRIE_SHIFT]=start;
            newStart+=_NORM_STAGE_2_BLOCK_COUNT;
            start=newStart;
        }
    }

    /* now adjust the parent table */
    for(i=0; i<parentCount; ++i) {
        parent[i]=map[parent[i]>>_NORM_TRIE_SHIFT];
    }

    /* we saved some space */
    printf("compacting trie: count of 32-bit words %lu->%lu\n",
            (long)stageCount, (long)newStart);
    return newStart;
}

extern void
processData() {
#if 0
    uint16_t i;
#endif

    processCombining();

    /* canonically reorder decompositions and assign combining classes for decompositions */
    enumTrie(postParseFn, NULL);

#if 0
    for(i=1; i<64; ++i) {
        if(combineAndQC[i]) {
            printf("combiningFlags==0x%02x  qcFlags(NF?C)==0x%02x\n", (i&0xc)>>2, i&0x33);
        }
    }
#endif

    /* add hangul/jamo specials */
    setHangulJamoSpecials();

    /* copy stage 1 for the FCD trie */
    uprv_memcpy(fcdStage1, stage1, sizeof(stage1));

    /* --- finalize data for quick checks & normalization: stage1/norm32Table --- */

    /* turn the Norm structs (stage2, norms) into 32-bit data words (norm32Table) */
    makeAll32();

    /* fold supplementary code points into lead surrogates */
    stage1Top=foldSupplementary(stage1, _NORM_STAGE_1_BMP_COUNT, norm32Table, &norm32TableTop, TRUE);

    /* compact stage 2 */
    norm32TableTop=compact(stage1, stage1Top, norm32Table, norm32TableTop);

    /* --- finalize data for FCD checks: fcdStage1/fcdTable --- */

    /* FCD data: take Norm.canonBothCCs and store them in the FCD table */
    makeFCD();

    /* FCD: fold supplementary code points into lead surrogates */
    fcdStage1Top=foldSupplementary(fcdStage1, _NORM_STAGE_1_BMP_COUNT, fcdTable, &fcdTableTop, FALSE);

    /* FCD: compact stage 2 */
    fcdTableTop=compact(fcdStage1, fcdStage1Top, fcdTable, fcdTableTop);

    /* ### debug output */
#if 0
    printf("number of stage 2 entries: %ld\n", stage2Mem->index);
    printf("size of stage 1 (BMP) & 2 (uncompacted) + extra data: %ld bytes\n", _NORM_STAGE_1_BMP_COUNT*2+stage2Mem->index*4+extraMem->index*2);
#endif
    printf("combining CPs tops: fwd %u  both %u  back %u\n", combineFwdTop, combineBothTop, combineBackTop);
    printf("combining table count: %u\n", combiningTableTop);
}

extern void
generateData(const char *dataDir) {
    UNewDataMemory *pData;
    uint16_t *p16;
    UErrorCode errorCode=U_ZERO_ERROR;
    uint32_t size, dataLength;
    uint16_t i;

    size=
        _NORM_INDEX_TOP*2+
        stage1Top*2+
        norm32TableTop*4+
        extraMem->index*2+
        combiningTableTop*2+
        fcdStage1Top*2+
        fcdTableTop*2;

    printf("size of " DATA_NAME "." DATA_TYPE " contents: %lu bytes\n", (long)size);

    indexes[_NORM_INDEX_COUNT]=_NORM_INDEX_TOP;
    indexes[_NORM_INDEX_TRIE_SHIFT]=_NORM_TRIE_SHIFT;
    indexes[_NORM_INDEX_TRIE_INDEX_COUNT]=stage1Top;
    indexes[_NORM_INDEX_TRIE_DATA_COUNT]=norm32TableTop;
    indexes[_NORM_INDEX_UCHAR_COUNT]=(uint16_t)extraMem->index;

    indexes[_NORM_INDEX_COMBINE_DATA_COUNT]=combiningTableTop;
    indexes[_NORM_INDEX_COMBINE_FWD_COUNT]=combineFwdTop;
    indexes[_NORM_INDEX_COMBINE_BOTH_COUNT]=(uint16_t)(combineBothTop-combineFwdTop);
    indexes[_NORM_INDEX_COMBINE_BACK_COUNT]=(uint16_t)(combineBackTop-combineBothTop);

    indexes[_NORM_INDEX_FCD_TRIE_INDEX_COUNT]=fcdStage1Top;
    indexes[_NORM_INDEX_FCD_TRIE_DATA_COUNT]=fcdTableTop;

    /* adjust the stage 1 indexes to offset stage 2 from the beginning of stage 1 */

    /* stage1/norm32Table */
    for(i=0; i<stage1Top; ++i) {
        stage1[i]+=stage1Top/2; /* stage 2 is 32-bit indexed */
    }

    /* fcdStage1/fcdTable */
    for(i=0; i<fcdStage1Top; ++i) {
        fcdStage1[i]+=fcdStage1Top; /* FCD stage 2 is 16-bit indexed */
    }

    /* reduce the contents of fcdTable from 32-bit values to 16-bit values, in-place (destructive!) */
    p16=(uint16_t *)fcdTable;
    for(i=0; i<fcdTableTop; ++i) {
        p16[i]=(uint16_t)fcdTable[i];
    }

    /* write the data */
    pData=udata_create(dataDir, DATA_TYPE, DATA_NAME, &dataInfo,
                       haveCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gennorm: unable to create the output file, error %d\n", errorCode);
        exit(errorCode);
    }

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, stage1, stage1Top*2);
    udata_writeBlock(pData, norm32Table, norm32TableTop*4);
    udata_writeBlock(pData, utm_getStart(extraMem), extraMem->index*2);
    udata_writeBlock(pData, combiningTable, combiningTableTop*2);
    udata_writeBlock(pData, fcdStage1, fcdStage1Top*2);
    udata_writeBlock(pData, fcdTable, fcdTableTop*2);

    /* finish up */
    dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gennorm: error %d writing the output file\n", errorCode);
        exit(errorCode);
    }

    if(dataLength!=size) {
        fprintf(stderr, "gennorm: data length %lu != calculated size %lu\n",
            (long)dataLength, (long)size);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
}

extern void
cleanUpData(void) {
    uprv_free(norm32Table);
    uprv_free(fcdTable);

    utm_close(stage2Mem);
    utm_close(normMem);
    utm_close(utf32Mem);
    utm_close(extraMem);
    utm_close(combiningTriplesMem);
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
