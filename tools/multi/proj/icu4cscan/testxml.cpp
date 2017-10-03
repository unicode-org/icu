// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
// Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. 
#include <ostream>
#include "xmlout.h"
#include <stdio.h>
#include <stdlib.h>

#include "uoptions.h"
#include "unicode/putil.h"
#include "unicode/ucol.h"
#include "unicode/ucal.h"
#include "unicode/uchar.h"
#include "unicode/ures.h"
#include "unicode/udat.h"
#include "unicode/ustring.h"
#if (U_ICU_VERSION_MAJOR_NUM > 2) || ((U_ICU_VERSION_MAJOR_NUM>1)&&(U_ICU_VERSION_MINOR_NUM>5))
#include "unicode/uclean.h"
#endif


static char *progName;
static UOption options[]={
    UOPTION_HELP_H,             /* 0 */
    UOPTION_HELP_QUESTION_MARK, /* 1 */
    UOPTION_VERBOSE,            /* 2 */
    UOPTION_ICUDATADIR,         /* 3 */
    UOPTION_DESTDIR,            /* 4 */
    UOPTION_COPYRIGHT,          /* 5 */
};

const char *u_errorNameShort(UErrorCode code) {
    switch(code) {
        case U_ZERO_ERROR: return "ok";
        case U_MISSING_RESOURCE_ERROR: return "missing";
        default:   return u_errorName(code);
    }
}

void usageAndDie(int retCode) {
        printf("Usage: %s [-v] [-options] -o output-file dictionary-file\n", progName);
        printf("\tRead in word list and write out compact trie dictionary\n"
            "options:\n"
            "\t-h or -? or --help  this usage text\n"
            "\t-V or --version     show a version message\n"
            "\t-c or --copyright   include a copyright notice\n"
            "\t-v or --verbose     turn on verbose output\n"
            "\t-i or --icudatadir  directory for locating any needed intermediate data files,\n"
            "\t                    followed by path, defaults to %s\n"
            "\t-d or --destdir     destination directory, followed by the path\n",
            u_getDataDirectory());
        exit (retCode);
}

/*U_CAPI void U_EXPORT2*/
static void _versionFromUString(UVersionInfo versionArray, const UChar *versionString) {
    if(versionArray==NULL) {
        return;
    }

    if(versionString!=NULL) {
        char verchars[U_MAX_VERSION_LENGTH+1];
        u_UCharsToChars(versionString, verchars, U_MAX_VERSION_LENGTH);
        u_versionFromString(versionArray, verchars);
    }
}

/*U_CAPI void U_EXPORT2*/
static void _getCLDRVersionDirect(UVersionInfo versionArray, UErrorCode *status) {
    UResourceBundle *resindx;
    resindx = ures_openDirect(NULL, "supplementalData", status);
    if(!U_FAILURE(*status)) {
//        fprintf(stderr, "Err: could not open res_index, %s\n", u_errorName(status));
//        fflush(stderr);
//    } else {
        const UChar *cldrver;
        int32_t len;
        cldrver = ures_getStringByKey(resindx, "cldrVersion", &len, status);
        if(!U_FAILURE(*status)) {
//            fprintf(stderr, "ERR: could not load CLDRVersion key: %s\n", u_errorName(*status));
//            fflush(stderr);
//        } else {
//            UVersionInfo cldrVersion;
            _versionFromUString(versionArray, cldrver);
//            strcpy(tmp, "type=\"cldr\" version=\"");
//            u_versionToString(cldrVersion, tmp+strlen(tmp));
//            strcat(tmp, "\"");
//            XMLElement icuData(xf, "feature", tmp, TRUE);
        }
        ures_close(resindx);
    }
}

/*U_CAPI void U_EXPORT2*/
static void _getCLDRVersionOld(UVersionInfo versionArray, UErrorCode *status) {
    UResourceBundle *resindx;
    resindx = ures_openDirect(NULL, "res_index", status);
    if(!U_FAILURE(*status)) {
//        fprintf(stderr, "Err: could not open res_index, %s\n", u_errorName(status));
//        fflush(stderr);
//    } else {
        const UChar *cldrver;
        int32_t len;
        cldrver = ures_getStringByKey(resindx, "CLDRVersion", &len, status);
        if(!U_FAILURE(*status)) {
//            fprintf(stderr, "ERR: could not load CLDRVersion key: %s\n", u_errorName(*status));
//            fflush(stderr);
//        } else {
//            UVersionInfo cldrVersion;
            _versionFromUString(versionArray, cldrver);
//            strcpy(tmp, "type=\"cldr\" version=\"");
//            u_versionToString(cldrVersion, tmp+strlen(tmp));
//            strcat(tmp, "\"");
//            XMLElement icuData(xf, "feature", tmp, TRUE);
        }
        ures_close(resindx);
    }
}

int could_open(const char *locale, char *comments) {
    char tmp[200];
    UResourceBundle *rb = NULL;
    UErrorCode status = U_ZERO_ERROR;
    rb = ures_open(NULL, locale, &status);
    if(U_FAILURE(status)) {
        sprintf(tmp, " open:%s", u_errorName(status));
        strcat(comments, tmp);
        return 0;
    } else {
        ures_close(rb);
        sprintf(tmp, " open:%s", u_errorNameShort(status));
        strcat(comments, tmp);
        return 1;
    }
}
int col_could_open(const char *locale, char *comments) {
    char tmp[200];
    UCollator *rb = NULL;
    UErrorCode status = U_ZERO_ERROR;
    rb = ucol_open(locale, &status);
    if(U_FAILURE(status)) {
        sprintf(tmp, " open:%s", u_errorName(status));
        /*strcat(comments, tmp); */
        return 0;
    } else {
        ucol_close(rb);
        sprintf(tmp, " open:%s", u_errorNameShort(status));
        /* strcat(comments, tmp); */
        return 1;
    }
}

const char *UDateFormatSymbolType_name(UDateFormatSymbolType i) {
switch(i) {
    case UDAT_ERAS: return "UDAT_ERAS"; break;
    /** The month names, for example February */
    case UDAT_MONTHS: return "UDAT_MONTHS"; break;
    /** The short month names, for example Feb. */
    case UDAT_SHORT_MONTHS: return "UDAT_SHORT_MONTHS"; break;
    /** The weekday names, for example Monday */
    case UDAT_WEEKDAYS: return "UDAT_WEEKDAYS"; break;
    /** The short weekday names, for example Mon. */
    case UDAT_SHORT_WEEKDAYS: return "UDAT_SHORT_WEEKDAYS"; break;
    /** The AM/PM names, for example AM */
    case UDAT_AM_PMS: return "UDAT_AM_PMS"; break;
    /** The localized characters */
    case UDAT_LOCALIZED_CHARS: return "UDAT_LOCALIZED_CHARS"; break;
    /** The long era names, for example Anno Domini */
#if U_ICU_VERSION_MAJOR_NUM>3 || U_ICU_VERSION_MAJOR_NUM>3
    case UDAT_ERA_NAMES: return "UDAT_ERA_NAMES"; break;
#endif
#if U_ICU_VERSION_MAJOR_NUM>3 || U_ICU_VERSION_MAJOR_NUM>3
    /** The narrow month names, for example F */
    case UDAT_NARROW_MONTHS: return "UDAT_NARROW_MONTHS"; break;
    /** The narrow weekday names, for example N */
    case UDAT_NARROW_WEEKDAYS: return "UDAT_NARROW_WEEKDAYS"; break;
    /** Standalone context versions of months */
    case UDAT_STANDALONE_MONTHS: return "UDAT_STANDALONE_MONTHS"; break;
    case UDAT_STANDALONE_SHORT_MONTHS: return "UDAT_STANDALONE_SHORT_MONTHS"; break;
    case UDAT_STANDALONE_NARROW_MONTHS: return "UDAT_STANDALONE_NARROW_MONTHS"; break;
    /** Standalone context versions of weekdays */
    case UDAT_STANDALONE_WEEKDAYS: return "UDAT_STANDALONE_WEEKDAYS"; break;
    case UDAT_STANDALONE_SHORT_WEEKDAYS: return "UDAT_STANDALONE_SHORT_WEEKDAYS"; break;
    case UDAT_STANDALONE_NARROW_WEEKDAYS: return "UDAT_STANDALONE_NARROW_WEEKDAYS"; break;
#endif
#if U_ICU_VERSION_MAJOR_NUM>3 || U_ICU_VERSION_MAJOR_NUM>4
    /** The quarters, for example 1st Quarter */
    case UDAT_QUARTERS: return "UDAT_QUARTERS"; break;
    /** The short quarter names, for example Q1 */
    case UDAT_SHORT_QUARTERS: return "UDAT_SHORT_QUARTERS"; break;
    /** Standalone context versions of quarters */
    case UDAT_STANDALONE_QUARTERS: return "UDAT_STANDALONE_QUARTERS"; break;
    case UDAT_STANDALONE_SHORT_QUARTERS: return "UDAT_STANDALONE_SHORT_QUARTERS"; break;
#endif
}
    return "<Unknown>";
}


UDateFormatSymbolType scanArray[] = {
    UDAT_ERAS,
    /** The month names, for example February */
    UDAT_MONTHS,
    /** The short month names, for example Feb. */
    UDAT_SHORT_MONTHS,
    /** The weekday names, for example Monday */
    UDAT_WEEKDAYS,
    /** The short weekday names, for example Mon. */
    UDAT_SHORT_WEEKDAYS,
    /** The AM/PM names, for example AM */
//    UDAT_AM_PMS,
    /** The localized characters */
//    UDAT_LOCALIZED_CHARS,
    /** The long era names, for example Anno Domini */
//    UDAT_ERA_NAMES,
    /** The narrow month names, for example F */
//    UDAT_NARROW_MONTHS,
};

int *starts = NULL;

UChar ***rootdata = NULL;

void initroot(UErrorCode *status) {
  UDateFormat *fmt;
  fmt = udat_open(UDAT_DEFAULT, UDAT_DEFAULT, "root", NULL, -1,NULL,0, status);
  rootdata = (UChar***)malloc((sizeof(scanArray)/sizeof(scanArray[0]))*sizeof(rootdata[0]));
  starts = (int*)malloc((sizeof(scanArray)/sizeof(scanArray[0]))*sizeof(starts[0]));
  for(int i=0;U_SUCCESS(*status)&&i<sizeof(scanArray)/sizeof(scanArray[0]);i++) {
    int thisCount = udat_countSymbols(fmt, scanArray[i]);
    rootdata[i]=0;
    rootdata[i]=(UChar**)malloc(thisCount*sizeof(rootdata[i][0]));
    switch(scanArray[i]) {
        case UDAT_WEEKDAYS:
        case UDAT_SHORT_WEEKDAYS:
            starts[i]=1;
            break;
        default: 
            starts[i]=0;
    }
    for(int j=starts[i];U_SUCCESS(*status)&&j<thisCount;j++) {
        rootdata[i][j]=(UChar*)malloc(1024);
        int sz =  
        udat_getSymbols(fmt,
            scanArray[i],
            j,
            rootdata[i][j],
            1024,
            status);
    }
  }
}

/* Format the date */
static void
date(const UChar *tz,
     UDateFormatStyle style,
     char *format,
     const char *locale, char *comments,
     UErrorCode *status)
{
  UChar *s = 0;
  int32_t len = 0;
  UDateFormat *fmt;
  UChar uFormat[100];
  char tmp[200];
  
  int tc=0; // total count
  int tf=0; // total found
  int tl = 0;

  fmt = udat_open(style, style, locale, tz, -1,NULL,0, status);
  if ( format != NULL ) {
     u_charsToUChars(format,uFormat,strlen(format)),
     udat_applyPattern(fmt,FALSE,uFormat,strlen(format));
  }
  len = udat_format(fmt, ucal_getNow(), 0, len, 0, status);
  if(*status == U_BUFFER_OVERFLOW_ERROR) {
    *status = U_ZERO_ERROR;
    s = (UChar*) malloc(sizeof(UChar) * (len+1));
    if(s == 0) goto finish;
    udat_format(fmt, ucal_getNow(), s, len + 1, 0, status);
    if(U_FAILURE(*status)) goto finish;
  }

  /* print the date string */
  //uprint(s, stdout, status);

  /* print a trailing newline */
  //printf("\n");
  /* count bits */
  UChar outbuf[1024];
  for(int i=0;U_SUCCESS(*status)&&i<sizeof(scanArray)/sizeof(scanArray[0]);i++) {
    int thisCount = udat_countSymbols(fmt, scanArray[i]);
    tc += thisCount;
    for(int j=starts[i];U_SUCCESS(*status)&&j<thisCount;j++) {
	*status = U_ZERO_ERROR;
        int sz =  
        udat_getSymbols(fmt,
            scanArray[i],
            j,
            outbuf,
            1024,
            status);
            if(U_SUCCESS(*status)) { tf++; tl += u_strlen(outbuf); }
        //if(!u_strcmp(outbuf,rootdata[i][j])) {
        if(*status != U_ZERO_ERROR) {
#if  0
            fprintf(stderr, "<!-- %s: err: data %s:%d:%d is missing: %X... -->\n", locale, UDateFormatSymbolType_name(scanArray[i]), i, j, outbuf[0]);
#endif
            sprintf(tmp, " missing: %s#%d-%s ", UDateFormatSymbolType_name(scanArray[i]), j, u_errorNameShort(*status));
            *status = U_MISSING_RESOURCE_ERROR;
            strcat(comments, tmp);
        }
    }
  }
  
 finish:
  sprintf(tmp, " syms:%d/%d#%d:%s", tf, tc, tl, u_errorNameShort(*status));
  strcat(comments,tmp);

  udat_close(fmt);
  free(s);
}

static void writeOkComments(XMLFile &xf, int ok, const char *comments, const char *locale) { 
    char tmp[2000];
    tmp[0]=0;
    if(ok) {
        if(!comments||!*comments) {
            strcpy(tmp,locale);
            strcat(tmp, " ");
        } else {
            sprintf(tmp, "%s <!--  %s -->", locale, comments);
        }
    } else if(comments&&*comments) {
        sprintf(tmp, "<!-- !! %s: %s -->", locale, comments);
    }
    if(tmp&&*tmp) {
        xf.writeln(tmp);
    }
}


int could_fmt_dow(const char *locale, char *comments) {
    char tmp[200];
//    UResourceBundle *rb = NULL;
    UErrorCode status = U_ZERO_ERROR;
        
    date(NULL,
         UDAT_LONG,
     NULL,
     locale, comments,
     &status);

    if(U_FAILURE(status) || status != U_ZERO_ERROR) {
        
        sprintf(tmp, " fmt:%s", u_errorNameShort(status));
        strcat(comments, tmp);
        return 0;
    } else {
        sprintf(tmp, " fmt:%s", u_errorNameShort(status));
        strcat(comments, tmp);
        return 1;
    }
}

void probeCapability(XMLFile& xf, const char *locale) {
    char comments[1000];
    int ok=1;
    int rc =0;
    
    //fprintf(stderr, "PROBE: %s\n", locale);
    
    comments[0]=0;
    
    if(!could_open(locale, comments)) {
        ok = 0;
    }
    
#if (U_ICU_VERSION_MAJOR_NUM > 2) || ((U_ICU_VERSION_MAJOR_NUM>1)&&(U_ICU_VERSION_MINOR_NUM>2))
    if(!could_fmt_dow(locale, comments)) {
        ok = 0;
    }
#endif
    
    writeOkComments(xf,ok, comments,locale);
}

void probeColCapability(XMLFile& xf, const char *locale) {
    char comments[1000];
    int ok=1;
    int rc =0;
    UErrorCode status = U_ZERO_ERROR;
    
    //fprintf(stderr, "PROBE: %s\n", locale);
    
    comments[0]=0;
    
    if(!col_could_open(locale, comments)) {
        ok = 0;
    }
    
    /*
    if(!col_could_fmt_dow(locale, comments)) {
        ok = 0;
    }
    */
    writeOkComments(xf,ok, comments,locale);
}

int main (int argc, char **  argv) {
    U_MAIN_INIT_ARGS(argc, argv);
    progName = argv[0];
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);
//    const char *loc;
    
	{
		UErrorCode status = U_ZERO_ERROR;
#if (U_ICU_VERSION_MAJOR_NUM > 2) || ((U_ICU_VERSION_MAJOR_NUM>1)&&(U_ICU_VERSION_MINOR_NUM>5))
		u_init(&status);
#else
		ures_open(NULL, "en_US", &status);
#endif
		fprintf(stderr, " Init: %s\n", u_errorName(status));

	}

    {
        UErrorCode is = U_ZERO_ERROR;
#if (U_ICU_VERSION_MAJOR_NUM > 2) || ((U_ICU_VERSION_MAJOR_NUM>1)&&(U_ICU_VERSION_MINOR_NUM>2))
        initroot(&is);
        fprintf(stderr, "Init: %s\n", u_errorNameShort(is));
#endif
    }

    if(argc<0) {
        // Unrecognized option
        fprintf(stderr, "error in command line argument \"%s\"\n", argv[-argc]);
        usageAndDie(U_ILLEGAL_ARGUMENT_ERROR);
    }

    if(options[0].doesOccur || options[1].doesOccur) {
        //  -? or -h for help.
        usageAndDie(0);
    }

    

    {
        char tmp[200];
        XMLFile xf(stdout);
        {
            xf.writeln("<!DOCTYPE icuInfo SYSTEM \"http://icu-project.org/dtd/icumeta.dtd\">");
            XMLElement icuInfo(xf, "icuInfo");
            XMLElement icuProducts(xf, "icuProducts");
            XMLElement icuProduct(xf, "icuProduct", "type=\"icu4c\"");
            XMLElement releases(xf, "releases");
            sprintf(tmp, "version=\"%s\"", U_ICU_VERSION);
            XMLElement release(xf, "release", tmp);
            
            XMLElement capabilities(xf, "capabilities");
            {
                sprintf(tmp, "type=\"unicode\" version=\"%s\"",
                    U_UNICODE_VERSION);
                XMLElement icuData(xf, "feature", tmp, TRUE);
            }
            {
                UCollator *col;
                char ucavers[200];
                UVersionInfo vers;
                UErrorCode status = U_ZERO_ERROR;
                col = ucol_open("root", &status);
#if (U_ICU_VERSION_MAJOR_NUM>2) || ((U_ICU_VERSION_MAJOR_NUM>1)&&(U_ICU_VERSION_MINOR_NUM>7))
                ucol_getUCAVersion(col, vers);
                u_versionToString(vers, ucavers);
#else
		strcpy(ucavers, "???");
#endif
                sprintf(tmp, "type=\"uca\" version=\"%s\"",
                    ucavers);
                XMLElement icuData(xf, "feature", tmp, TRUE);
                ucol_close(col);
            }
#if (U_ICU_VERSION_MAJOR_NUM>3) || ((U_ICU_VERSION_MAJOR_NUM > 2) && (U_ICU_VERSION_MINOR_NUM >7))
            {
                const char *tzvers;
                UErrorCode status = U_ZERO_ERROR;
                tzvers = ucal_getTZDataVersion(&status);
                sprintf(tmp, "type=\"tz\" version=\"%s\"",
                    tzvers);
                XMLElement icuData(xf, "feature", tmp, TRUE);
            }
#endif            
            { 
                UErrorCode status = U_ZERO_ERROR;
                UVersionInfo cldrVersion;
                _getCLDRVersionDirect(cldrVersion, &status);
		if(U_FAILURE(status)) {
			UErrorCode subStatus = U_ZERO_ERROR;
                	_getCLDRVersionOld(cldrVersion, &subStatus);
			if(U_SUCCESS(subStatus)) {
				status = subStatus;
			}
		}
                if(U_FAILURE(status)) {
                    fprintf(stderr, "Err: could not get CLDR Version, %s\n", u_errorName(status));
                    fflush(stderr);
                } else {
                    strcpy(tmp, "type=\"cldr\" version=\"");
                    u_versionToString(cldrVersion, tmp+strlen(tmp));
                    strcat(tmp, "\"");
                    XMLElement icuData(xf, "feature", tmp, TRUE);
                }
            }
            if(1) {
                int n = uloc_countAvailable();
                sprintf(tmp, "type=\"formatting\" total=\"%d\" version=\"%s\"",
                    n,
                    "???");
                XMLElement icuData(xf, "feature", tmp);
                
               // probeCapability(xf, "root");
                for(int j=0;j<n;j++) {
                    probeCapability(xf, uloc_getAvailable(j));
                }
                
            }
            if(1) {
                int n = ucol_countAvailable();
                sprintf(tmp, "type=\"collation\" total=\"%d\" version=\"%s\"",
                    n,
                    "???");
                XMLElement icuData(xf, "feature", tmp);
                
               // probeCapability(xf, "root");
                for(int j=0;j<n;j++) {
                    probeColCapability(xf, ucol_getAvailable(j));
                }
                
            }
            
        }
    }
    

    return 0;
}
