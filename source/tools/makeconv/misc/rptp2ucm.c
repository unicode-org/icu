/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  rptp2ucm.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001feb16
*   created by: Markus W. Scherer
*
*   This tool reads two CDRA conversion table files (RPMAP & TPMAP or RXMAP and TXMAP) and
*   generates a canonicalized ICU .ucm file from them.
*   If the RPMAP/RXMAP file does not contain a comment line with the substitution character,
*   then this tool also attempts to read the header of the corresponding UPMAP/UXMAP file
*   to extract subchar and subchar1.
*
*   R*MAP: Unicode->codepage
*   T*MAP: codepage->Unicode
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl rptp2ucm.c
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct UCMSubchar {
    const char *name;
    unsigned long subchar, subchar1;
} UCMSubchar;

static const UCMSubchar
knownSubchars[]={
    "274_P100", 0x3f, 0,
    "850_P100", 0x7f, 0,
    "913_P100", 0x1a, 0,
    "1047_P100", 0x3f, 0
};

typedef struct CCSIDStateTable {
    unsigned int ccsid;
    const char *table;
} CCSIDStateTable;

static const CCSIDStateTable
knownStateTables[]={
    367,   "<icu:state>                   0-7f\n",

    941,   "<icu:state>                   0-80:2, 81-fe:1, ff:2\n"
           "<icu:state>                   40-7e, 80-fc\n"
           "<icu:state>\n",

    942,   "<icu:state>                   0-80, 81-9f:1, a0-df, e0-fc:1, fd-ff\n"
           "<icu:state>                   40-7e, 80-fc\n",

    943,   "<icu:state>                   0-7f, 81-9f:1, a0-df, e0-fc:1\n"
           "<icu:state>                   40-7e, 80-fc\n",

    944,   "<icu:state>                   0-80, 81-bf:1, c0-ff\n"
           "<icu:state>                   40-7e, 80-fe\n",

    949,   "<icu:state>                   0-84, 8f-fe:1\n"
           "<icu:state>                   40-7e, 80-fe\n",

    950,   "<icu:state>                   0-7f, 81-fe:1\n"
           "<icu:state>                   40-7e, 81-fe\n",

    964,   "<icu:state>                   0-8d, 8e:2, 90-9f, a1-fe:1, aa-c1:5, c3:5, fe:5\n"
           "<icu:state>                   a1-fe\n"
           "<icu:state>                   a1-b0:3, a1:4, a2:8, a3-ab:4, ac:7, ad:6, ae-b0:4\n"
           "<icu:state>                   a1-fe:1\n"
           "<icu:state>                   a1-fe:5\n"
           "<icu:state>                   a1-fe.u\n"
           "<icu:state>                   a1-a4:1, a5-fe:5\n"
           "<icu:state>                   a1-e2:1, e3-fe:5\n"
           "<icu:state>                   a1-f2:1, f3-fe:5\n",

    970,   "<icu:state>                   0-9f, a1-fe:1\n"
           "<icu:state>                   a1-fe\n",

    1363,  "<icu:state>                   0-7f, 81-fe:1\n"
           "<icu:state>                   40-7e, 80-fe\n",

    1370,  "<icu:state>                   0-80, 81-fe:1\n"
           "<icu:state>                   40-7e, 81-fe\n",

    1383,  "<icu:state>                   0-9f, a1-fe:1\n"
           "<icu:state>                   a1-fe\n",

    1386,  "<icu:state>                   0-7f, 81-fe:1\n"
           "<icu:state>                   40-7e, 80-fe\n",

    5050,  "<icu:state>                   0-8d, 8e:2, 8f:3, 90-9f, a1-fe:1\n"
           "<icu:state>                   a1-fe\n"
           "<icu:state>                   a1-e4\n"
           "<icu:state>                   a1-fe:1, a1:4, a3-af:4, b6:4, d6:4, da-db:4, ed-f2:4\n"
           "<icu:state>                   a1-fe.u\n",

    21427, "<icu:state>                   0-80:2, 81-fe:1, ff:2\n"
           "<icu:state>                   40-7e, 80-fe\n"
           "<icu:state>\n",

    33722, "<icu:state>                   0-8d, 8e:2, 8f:3, 90-9f, a1-fe:1\n"
           "<icu:state>                   a1-fe\n"
           "<icu:state>                   a1-e4\n"
           "<icu:state>                   a1-fe:1, a1:4, a3-af:4, b6:4, d6:4, da-db:4, ed-f2:4\n"
           "<icu:state>                   a1-fe.u\n"
};

typedef struct Mapping {
    /*
     * u bits:
     * 31..24  fallback indicator
     *         0  roundtrip
     *         1  Unicode->codepage
     *         3  codepage->Unicode
     * 23.. 0  Unicode code point
     *
     * b: codepage bytes with leading zeroes
     */
    unsigned long u, b;
} Mapping;

#define MAX_MAPPINGS_COUNT 200000

static Mapping
fromUMappings[MAX_MAPPINGS_COUNT], toUMappings[MAX_MAPPINGS_COUNT];

static long fromUMappingsTop, toUMappingsTop;

static unsigned long subchar, subchar1;
static unsigned int ccsid;

enum {
    ASCII,
    EBCDIC,
    UNKNOWN
};

static char
minCharLength,
maxCharLength,
charsetFamily,
usesPUA,
variantLF,
variantASCII,
variantControls,
variantSUB,
is7Bit;

static void
init() {
    fromUMappingsTop=toUMappingsTop=0;

    subchar=subchar1=0;
    ccsid=0;

    minCharLength=4;
    maxCharLength=0;
    charsetFamily=UNKNOWN;
    usesPUA=0;
    variantLF=0;
    variantASCII=0;
    variantControls=0;
    variantSUB=0;
    is7Bit=0;
}

/* lexically compare Mappings for sorting */
static int
compareMappings(const void *left, const void *right) {
    const Mapping *l=(const Mapping *)left, *r=(const Mapping *)right;
    long result;

    /* the code points use fewer than 32 bits, just cast them to signed values and subtract */
    result=(long)(l->u&0xffffff)-(long)(r->u&0xffffff);
    if(result!=0) {
        /* shift right 16 with sign-extend to take care of int possibly being 16 bits wide */
        return (int)(result>>16)|1;
    }

    /* the b fields may use all 32 bits as unsigned long, so result=(long)(l->b-r->b) would not work (try l->b=0x80000000 and r->b=1) */
    if(l->b<r->b) {
        return -1;
    } else if(l->b>r->b) {
        return 1;
    }

    return (int)(l->u>>24)-(int)(r->u>>24);
}

static const char *
skipWhitespace(const char *s) {
    while(*s==' ' || *s=='\t') {
        ++s;
    }
    return s;
}

static long
parseMappings(FILE *f, Mapping *mappings) {
    char line[200];
    Mapping *oldMappings;
    char *s, *end;
    long mappingsTop=0;

    oldMappings=mappings;
    while(fgets(line, sizeof(line), f)!=NULL) {
        s=(char *)skipWhitespace(line);

        /* skip empty lines */
        if(*s==0 || *s=='\n' || *s=='\r') {
            continue;
        }

        /* explicit end of table */
        if(memcmp(s, "END CHARMAP", 11)==0) {
            break;
        }

        /* comment lines, parse substitution characters, otherwise skip them */
        if(*s=='#' || *s=='*') {
            /* get subchar1 */
            s=strstr(line, "for U+00xx");
            if(s!=NULL) {
                s=strstr(line, "x'");
                if(s!=NULL) {
                    s+=2;
                    subchar1=strtoul(s, &end, 16);
                    if(end!=s+2 || *end!='\'') {
                        fprintf(stderr, "error parsing subchar1 from \"%s\"\n", line);
                        exit(2);
                    }
                    continue;
                } else {
                    fprintf(stderr, "error finding subchar1 on \"%s\"\n", line);
                    exit(2);
                }
            }

            /* get subchar */
            s=strstr(line, "for U+xxxx");
            if(s!=NULL) {
                s=strstr(line, "x'");
                if(s!=NULL) {
                    s+=2;
                    subchar=strtoul(s, &end, 16);
                    if(end<s+2 || *end!='\'') {
                        fprintf(stderr, "error parsing subchar from \"%s\"\n", line);
                        exit(2);
                    }
                    continue;
                } else {
                    fprintf(stderr, "error finding subchar on \"%s\"\n", line);
                    exit(2);
                }
            }

            continue;
        }

        mappings->b=strtoul(s, &end, 16);
        if(s==end || (*end!=' ' && *end!='\t')) {
            if((s+1)==end && *end=='-' && (mappings->b<=3)) {
                /* this is a special EUC format where the code set number prepends the bytes */
                unsigned long prefix;

                switch(mappings->b) {
                case 0:
                    prefix=0;
                    break;
                case 1:
                    prefix=0;
                    break;
                case 2:
                    prefix=0x8e;
                    break;
                case 3:
                    prefix=0x8f;
                    break;
                default:
                    /* never occurs because of above check */
                    break;
                }

                s+=2;
                mappings->b=strtoul(s, &end, 16);
                if(s==end || ((end-s)&1) || (*end!=' ' && *end!='\t')) {
                    fprintf(stderr, "error parsing EUC codepage bytes on \"%s\"\n", line);
                    exit(2);
                }
                mappings->b|=prefix<<(4*(end-s));
            } else {
                fprintf(stderr, "error parsing codepage bytes on \"%s\"\n", line);
                exit(2);
            }
        }

        s=(char *)skipWhitespace(end);
        mappings->u=strtoul(s, &end, 16);
        if(s==end || (*end!=' ' && *end!='\t' && *end!='\n' && *end!='\r' && *end!=0)) {
            if(strncmp(s, "????", 4)==0 || strstr(s, "UNASSIGNED")!=NULL) {
                /* this is a non-entry, do not add it to the mapping table */
                continue;
            }
            fprintf(stderr, "error parsing Unicode code point on \"%s\"\n", line);
            exit(2);
        }

        ++mappings;
        if(++mappingsTop>=MAX_MAPPINGS_COUNT) {
            fprintf(stderr, "error: too many mappings at \"%s\"\n", line);
            exit(2);
        }
    }

    /* sort the mappings */
    qsort(oldMappings, mappingsTop, sizeof(Mapping), compareMappings);

    return mappingsTop;
}

/* merge the mappings into fromUMappings and add fallback indicator values to Mapping.u bits 31..24 */
static void
mergeMappings() {
    long fromUIndex, toUIndex, newFromUMappingsTop=fromUMappingsTop;
    int cmp;

    fromUIndex=toUIndex=0;
    while(fromUIndex<fromUMappingsTop && toUIndex<toUMappingsTop) {
        cmp=compareMappings(fromUMappings+fromUIndex, toUMappings+toUIndex);
        if(cmp==0) {
            /* equal: roundtrip, nothing to do */
            ++fromUIndex;
            ++toUIndex;
        } else if(cmp<0) {
            /*
             * the fromU mapping does not have a toU counterpart:
             * fallback Unicode->codepage
             */
            if(fromUMappings[fromUIndex].b!=subchar && fromUMappings[fromUIndex].b!=subchar1) {
                fromUMappings[fromUIndex++].u|=0x1000000;
            } else {
                fromUMappings[fromUIndex++].u|=0x2000000;
            }
        } else {
            /*
             * the toU mapping does not have a fromU counterpart:
             * (reverse) fallback codepage->Unicode, copy it to the fromU table
             */
            fromUMappings[newFromUMappingsTop].u=toUMappings[toUIndex].u|=0x3000000;
            fromUMappings[newFromUMappingsTop++].b=toUMappings[toUIndex++].b;
        }
    }

    /* either one or both tables are exhausted */
    while(fromUIndex<fromUMappingsTop) {
        /* leftover fromU mappings are fallbacks */
        if(fromUMappings[fromUIndex].b!=subchar && fromUMappings[fromUIndex].b!=subchar1) {
            fromUMappings[fromUIndex++].u|=0x1000000;
        } else {
            fromUMappings[fromUIndex++].u|=0x2000000;
        }
    }

    while(toUIndex<toUMappingsTop) {
        /* leftover toU mappings are reverse fallbacks */
        fromUMappings[newFromUMappingsTop].u=toUMappings[toUIndex].u|=0x3000000;
        fromUMappings[newFromUMappingsTop++].b=toUMappings[toUIndex++].b;
    }

    fromUMappingsTop=newFromUMappingsTop;

    /* re-sort the mappings */
    qsort(fromUMappings, fromUMappingsTop, sizeof(Mapping), compareMappings);
}

static void
analyzeTable() {
    unsigned long u, b, f, minTwoByte=0xffff, maxTwoByte=0, oredBytes=0;
    long i, countASCII=0;
    char length;

    for(i=0; i<fromUMappingsTop; ++i) {
        f=fromUMappings[i].u>>24;
        u=fromUMappings[i].u&0xffffff;
        b=fromUMappings[i].b;

        oredBytes|=b;

        /* character length? */
        if(b<=0xff) {
            length=1;
        } else if(b<=0xffff) {
            length=2;
            if(b<minTwoByte) {
                minTwoByte=b;
            }
            if(b>maxTwoByte) {
                maxTwoByte=b;
            }
        } else if(b<=0xffffff) {
            length=3;
        } else {
            length=4;
        }
        if(length<minCharLength) {
            minCharLength=length;
        }
        if(length>maxCharLength) {
            maxCharLength=length;
        }

        /* PUA used? */
        if((unsigned long)(u-0xe000)<0x1900 || (unsigned long)(u-0xf0000)<0x20000) {
            usesPUA=1;
        }

        /* only consider roundtrip mappings for the rest */
        if(f!=0) {
            continue;
        }

        /* ASCII or EBCDIC? */
        if(u==0x41) {
            if(b==0x41) {
                charsetFamily=ASCII;
            } else if(b==0xc1) {
                charsetFamily=EBCDIC;
            }
        } else if(u==0xa) {
            if(b==0xa) {
                charsetFamily=ASCII;
            } else if(b==0x25) {
                charsetFamily=EBCDIC;
                variantLF=0;
            } else if(b==0x15) {
                charsetFamily=EBCDIC;
                variantLF=1;
            }
        }

        /* US-ASCII? */
        if((unsigned long)(u-0x21)<94) {
            if(u==b) {
                ++countASCII;
            } else {
                variantASCII=1;
            }
        } else if(u<0x20 || u==0x7f) {
            /* non-ISO C0 controls? */
            if(u!=b) {
                /* IBM PC rotation of SUB and other controls: 0x1a->0x7f->0x1c->0x1a */
                if(u==0x1a && b==0x7f || u==0x1c && b==0x1a || u==0x7f && b==0x1c) {
                    charsetFamily=ASCII;
                    variantSUB=1;
                } else {
                    variantControls=1;
                }
            }
        }
    }

    is7Bit= oredBytes<=0x7f;

    if(charsetFamily==UNKNOWN) {
        if(minCharLength==2 && maxCharLength==2) {
            /* guess the charset family for DBCS according to typical byte distributions */
            if( ((0x2020<=minTwoByte || minTwoByte<=0x217e) && maxTwoByte<=0x7e7e) ||
                ((0xa0a0<=minTwoByte || minTwoByte<=0xa1fe) && maxTwoByte<=0xfefe) ||
                ((0x8140<=minTwoByte || minTwoByte<=0x81fe) && maxTwoByte<=0xfefe)
            ) {
                charsetFamily=ASCII;
            } else if((minTwoByte==0x4040 || (0x4141<=minTwoByte && minTwoByte<=0x41fe)) && maxTwoByte<=0xfefe) {
                charsetFamily=EBCDIC;
            }
        }
        if(charsetFamily==UNKNOWN) {
            fprintf(stderr, "error: unable to determine the charset family\n");
            exit(3);
        }
    }

    /* reset variant indicators if they do not apply */
    if(charsetFamily!=ASCII || minCharLength!=1) {
        variantASCII=variantSUB=variantControls=0;
    } else if(countASCII!=94) {
        /* if there are not 94 mappings for ASCII graphic characters, then set variantASCII */
        variantASCII=1;
    }

    if(charsetFamily!=EBCDIC || minCharLength!=1) {
        variantLF=0;
    }
}

static int
getSubchar(const char *name) {
    int i;

    for(i=0; i<sizeof(knownSubchars)/sizeof(knownSubchars[0]); ++i) {
        if(strcmp(name, knownSubchars[i].name)==0) {
            subchar=knownSubchars[i].subchar;
            subchar1=knownSubchars[i].subchar1;
            return 1;
        }
    }

    return 0;
}

static void
getSubcharFromUPMAP(FILE *f) {
    char line[200];
    char *s, *end;
    unsigned long *p;
    unsigned long value, bytes;

    while(fgets(line, sizeof(line), f)!=NULL && memcmp(line, "CHARMAP", 7)!=0) {
        s=(char *)skipWhitespace(line);

        /* skip empty lines */
        if(*s==0 || *s=='\n' || *s=='\r') {
            continue;
        }

        /* look for variations of subchar entries */
        if(memcmp(s, "<subchar>", 9)==0) {
            s=(char *)skipWhitespace(s+9);
            p=&subchar;
        } else if(memcmp(s, "<subchar1>", 10)==0) {
            s=(char *)skipWhitespace(s+10);
            p=&subchar1;
        } else if(memcmp(s, "#<subchar1>", 11)==0) {
            s=(char *)skipWhitespace(s+11);
            p=&subchar1;
        } else {
            continue;
        }

        /* get the value and store it in *p */
        bytes=0;
        while(s[0]=='\\' && s[1]=='x') {
            value=strtoul(s+2, &end, 16);
            s+=4;
            if(end!=s) {
                fprintf(stderr, "error parsing UPMAP subchar from \"%s\"\n", line);
                exit(2);
            }
            bytes=(bytes<<8)|value;
        }
        *p=bytes;
    }
}

static const char *
getStateTable() {
    int i;

    for(i=0; i<sizeof(knownStateTables)/sizeof(knownStateTables[0]); ++i) {
        if(ccsid==knownStateTables[i].ccsid) {
            return knownStateTables[i].table;
        }
    }

    return NULL;
}

static void
writeBytes(char *s, unsigned long b) {
    if(b<=0xff) {
        sprintf(s, "\\x%02lX", b);
    } else if(b<=0xffff) {
        sprintf(s, "\\x%02lX\\x%02lX", b>>8, b&0xff);
    } else if(b<=0xffffff) {
        sprintf(s, "\\x%02lX\\x%02lX\\x%02lX", b>>16, (b>>8)&0xff, b&0xff);
    } else {
        sprintf(s, "\\x%02lX\\x%02lX\\x%02lX\\x%02lX", b>>24, (b>>16)&0xff, (b>>8)&0xff, b&0xff);
    }
}

static void
writeUCM(FILE *f, const char *ucmname, const char *rpname, const char *tpname) {
    char buffer[100];
    const char *s;
    long i;

    /* write the header */
    fprintf(f,
        "# *******************************************************************************\n"
        "# *\n"
        "# *   Copyright (C) 1995-2001, International Business Machines\n"
        "# *   Corporation and others.  All Rights Reserved.\n"
        "# *\n"
        "# *******************************************************************************\n"
        "#\n"
        "# File created by rptp2ucm (compiled on %s)\n"
        "# from source files %s and %s\n"
        "#\n", __DATE__, rpname, tpname);

    /* ucmname does not have a path or .ucm */
    fprintf(f, "<code_set_name>               \"%s\"\n", ucmname);

    fputs("<char_name_mask>              \"AXXXX\"\n", f);
    fprintf(f, "<mb_cur_max>                  %u\n", maxCharLength);
    fprintf(f, "<mb_cur_min>                  %u\n", minCharLength);

    if(maxCharLength==1) {
        fputs("<uconv_class>                 \"SBCS\"\n", f);
    } else if(maxCharLength==2) {
        if(minCharLength==1) {
            if(charsetFamily==EBCDIC) {
                fputs("<uconv_class>                 \"EBCDIC_STATEFUL\"\n", f);
            } else {
                fputs("<uconv_class>                 \"MBCS\"\n", f);
            }
        } else if(minCharLength==2) {
            fputs("<uconv_class>                 \"DBCS\"\n", f);
        } else {
            fputs("<uconv_class>                 \"MBCS\"\n", f);
        }
    } else {
        fputs("<uconv_class>                 \"MBCS\"\n", f);
    }

    if(subchar!=0) {
        writeBytes(buffer, subchar);
        fprintf(f, "<subchar>                     %s\n", buffer);
    }

    if(subchar1!=0) {
        fprintf(f, "<subchar1>                    \\x%02X\n", subchar1);
    }

    /* write charset family */
    if(charsetFamily==ASCII) {
        fputs("<icu:charsetFamily>           \"ASCII\"\n", f);
    } else {
        fputs("<icu:charsetFamily>           \"EBCDIC\"\n", f);
    }

    /* write alias describing the codepage */
    sprintf(buffer, "<icu:alias>                   \"ibm-%u", ccsid);
    if(!usesPUA && !variantLF && !variantASCII && !variantControls && !variantSUB) {
        strcat(buffer, "_STD\"\n\n");
    } else {
        /* add variant indicators in alphabetic order */
        if(variantASCII) {
            strcat(buffer, "_VASCII");
        }
        if(variantControls) {
            strcat(buffer, "_VGCTRL");
        }
        if(variantLF) {
            strcat(buffer, "_VLF");
        }
        if(variantSUB) {
            strcat(buffer, "_VSUB");
        }
        if(usesPUA) {
            strcat(buffer, "_VPUA");
        }
        strcat(buffer, "\"\n\n");
    }
    fputs(buffer, f);

    /* write the state table - <icu:state> */
    s=getStateTable();
    if(s!=NULL) {
        fputs(s, f);
        fputs("\n", f);
    } else if(is7Bit) {
        fputs("<icu:state>                   0-7f\n\n", f);
    }

    /* write the mappings */
    fputs("CHARMAP\n", f);
    for(i=0; i<fromUMappingsTop; ++i) {
        writeBytes(buffer, fromUMappings[i].b);
        fprintf(f, "<U%04lX> %s |%lu\n", fromUMappings[i].u&0xffffff, buffer, fromUMappings[i].u>>24);
    }
    fputs("END CHARMAP\n", f);
}

static void
processTable(const char *arg) {
    char filename[1024], tpname[32];
    const char *basename, *s;
    FILE *rpmap, *tpmap, *ucm;
    unsigned long value, unicode;
    int length;

    init();

    /* separate path and basename */
    basename=strrchr(arg, '/');
    if(basename==NULL) {
        basename=strrchr(arg, '\\');
        if(basename==NULL) {
            basename=arg;
        } else {
            ++basename;
        }
    } else {
        ++basename;
        s=strrchr(arg, '\\');
        if(s!=NULL && ++s>basename) {
            basename=s;
        }
    }

    /* is this a standard RPMAP filename? */
    value=strtoul(basename, (char **)&s, 16);
    if( strlen(basename)!=17 ||
        (memcmp(basename+9, "RPMAP", 5)!=0 && memcmp(basename+9, "rpmap", 5)!=0 &&
         memcmp(basename+9, "RXMAP", 5)!=0 && memcmp(basename+9, "rxmap", 5)!=0) ||
        (s-basename)!=8 ||
        *s!='.'
    ) {
        fprintf(stderr, "error: \"%s\" is not a standard RPMAP filename\n", basename);
        exit(1);
    }

    /* is this really a Unicode conversion table? - get the CCSID */
    unicode=value&0xffff;
    if(unicode==13488 || unicode==17584) {
        ccsid=(unsigned int)(value>>16);
    } else {
        unicode=value>>16;
        if(unicode==13488 || unicode==17584) {
            ccsid=(unsigned int)(value&0xffff);
        } else {
            fprintf(stderr, "error: \"%s\" is not a Unicode conversion table\n", basename);
            exit(1);
        }
    }

    /* try to open the RPMAP file */
    rpmap=fopen(arg, "r");
    if(rpmap==NULL) {
        fprintf(stderr, "error: unable to open \"%s\"\n", arg);
        exit(1);
    }

    /* try to open the TPMAP file */
    strcpy(filename, arg);
    length=strlen(filename);

    /* guess the TPMAP filename; note that above we have checked the format of the basename */
    /* replace the R in RPMAP by T, keep upper- or lowercase */
    if(filename[length-8]=='R') {
        filename[length-8]='T';
    } else {
        filename[length-8]='t';
    }

    /* reverse the CCSIDs */
    memcpy(filename+length-17, basename+4, 4);
    memcpy(filename+length-13, basename, 4);

    /* first, keep the same suffix */
    tpmap=fopen(filename, "r");
    if(tpmap==NULL) {
        /* next, try reducing the second to last digit by 1 */
        --filename[length-2];
        tpmap=fopen(filename, "r");
        if(tpmap==NULL) {
            /* there is no TPMAP */
            fprintf(stderr, "error: unable to find the TPMAP file for \"%s\"\n", arg);
            exit(1);
        }
    }
    strcpy(tpname, filename+length-17);

    /* parse both files */
    fromUMappingsTop=parseMappings(rpmap, fromUMappings);
    toUMappingsTop=parseMappings(tpmap, toUMappings);
    fclose(tpmap);
    fclose(rpmap);

    /* if there is no subchar, then try to get it from the corresponding UPMAP */
    if(subchar==0) {
        FILE *f;

        /* restore the RPMAP filename and just replace the R by U */
        strcpy(filename+length-17, basename);
        if(filename[length-8]=='R') {
            filename[length-8]='U';
        } else {
            filename[length-8]='u';
        }

        f=fopen(filename, "r");
        if(f==NULL) {
            /* try reversing the CCSIDs */
            memcpy(filename+length-17, basename+4, 4);
            memcpy(filename+length-13, basename, 4);
            f=fopen(filename, "r");
        }
        if(f!=NULL) {
            getSubcharFromUPMAP(f);
            fclose(f);
        }
    }

    /* generate the .ucm filename - necessary before getSubchar() */
    length=sprintf(filename, "ibm-%u_", ccsid);

    /* uppercase and append the suffix */
    filename[length++]=toupper(basename[10]);  /* P or X */
    filename[length++]=toupper(basename[14]);  /* last 3 suffix characters */
    filename[length++]=toupper(basename[15]);
    filename[length++]=toupper(basename[16]);
    filename[length]=0;

    /* find the subchar if still necessary - necessary before merging for correct |2 */
    if(subchar==0 && !getSubchar(filename+4)) {
        fprintf(stderr, "warning: missing subchar in \"%s\" (CCSID=0x%04X)\n", filename, ccsid);
    }

    /* merge the mappings */
    mergeMappings();

    /* analyze the conversion table */
    analyzeTable();

    /* open the .ucm file */
    strcat(filename, ".ucm");
    ucm=fopen(filename, "w");
    if(ucm==NULL) {
        fprintf(stderr, "error: unable to open output file \"%s\"\n", filename);
        exit(4);
    }

    /* remove the .ucm from the filename for the following processing */
    filename[strlen(filename)-4]=0;

    /* write the .ucm file */
    writeUCM(ucm, filename, basename, tpname);
    fclose(ucm);
}

extern int
main(int argc, const char *argv[]) {
    if(argc<2) {
        fprintf(stderr,
                "usage: %s { rpmap/rxmap-filename }+\n",
                argv[0]);
        exit(1);
    }

    while(--argc>0) {
        processTable(*++argv);
    }

    return 0;
}
