/******************************************************************************
*
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************/
//
// uconv demonstration example of ICU and codepage conversion
// Purpose is to be a similar tool as the UNIX iconv program.
//
// Usage: uconv [flag] [file]
// -f [codeset]  Convert file from this codeset
// -t [codeset]  Convert file to this code set
// -l            Display all available converters
// -x [transliterator]  Run everything through a transliterator
// -L            Display all available transliterators
// If no file is given, uconv tries to read from stdin
// 
// To compile: c++ -o uconv -I${ICUHOME}/include -Wall -g uconv.cpp -L${ICUHOME}/lib -licuuc -licui18n
//
// Original contributor was Jonas Utterström <jonas.utterstrom@vittran.norrnod.se> in 1999
// Converted to the C conversion API by Yves Arrouye <yves@realnames.com>. 
//
// Permission is granted to use, copy, modify, and distribute this software
//

#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>

#include "cmemory.h"

// This is the UConverter headerfile
#include "unicode/ucnv.h"

// This is the UnicodeString headerfile
#include "unicode/unistr.h"

// Our message printer..
#include "unicode/uwmsg.h"

#ifdef WIN32
#include <string.h>
#include <io.h>
#include <fcntl.h>
#endif

#include "unicode/translit.h"

static const size_t buffsize = 4096;

static UResourceBundle *gBundle = 0;

static void initMsg(const char *pname) {
    static int ps = 0;

    if (!ps) {
        char dataPath[500];
        UErrorCode err = U_ZERO_ERROR;

        ps = 1;

        /* Get messages. */
        
        strcpy(dataPath, u_getDataDirectory());
        strcat(dataPath, "uconvmsg");
        
        gBundle = u_wmsg_setPath(dataPath, &err);
        if(U_FAILURE(err))
            {
                fprintf(stderr, "%s: warning: couldn't open resource bundle %s: %s\n", 
                        pname,
                        dataPath,
                        u_errorName(err));
            }
    }
}

// Callbacks
static struct callback_ent {
    const char *name;
    UConverterFromUCallback fromu;
    const void *fromuctxt;
    UConverterToUCallback tou;
    const void *touctxt;
} transcode_callbacks[] = {
    { "substitute", UCNV_FROM_U_CALLBACK_SUBSTITUTE, 0, UCNV_TO_U_CALLBACK_SUBSTITUTE, 0 },
    { "skip", UCNV_FROM_U_CALLBACK_SKIP, 0, UCNV_TO_U_CALLBACK_SKIP, 0 },
    { "stop", UCNV_FROM_U_CALLBACK_STOP, 0, UCNV_TO_U_CALLBACK_STOP, 0 },
    { "escape", UCNV_FROM_U_CALLBACK_ESCAPE, 0, UCNV_TO_U_CALLBACK_ESCAPE, 0 },
    { "escape-icu", UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_ICU, UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_ICU },
    { "escape-java", UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_JAVA, UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_JAVA },
    { "escape-c", UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_C, UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_C },
    { "escape-xml", UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_DEC, UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_DEC },
    { "escape-xml-dec", UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_DEC, UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_DEC },
    { "escape-xml-hex", UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_DEC, UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_DEC }
};

static const struct callback_ent *findCallback(const char *name) {
    int i, count = sizeof(transcode_callbacks) / sizeof(*transcode_callbacks);

    /* We'll do a linear search, there aren't many of them and bsearch()
       may not be that portable. */

    for (i = 0; i < count; ++i) {
        if (!strcmp(name, transcode_callbacks[i].name)) {
            return &transcode_callbacks[i];
        }
    }

    return 0;
}

// Print all available codepage converters
static int printConverters(const char *pname, const char *lookfor, int canon)
{
    UErrorCode err = U_ZERO_ERROR;

    int32_t num;
    uint16_t num_stds;
    const char **stds;

    if (lookfor) {
        if (!canon) {
            printf("%s\n", lookfor);
            return 0;
        } else {
            /* We've done that already except for the default name. Oh well. */

            const char *truename = ucnv_getAlias(lookfor, 0, &err);
            if (U_SUCCESS(err)) {
                lookfor = truename;
            } else {
                err = U_ZERO_ERROR;
            }
        }
    }

    num = ucnv_countAvailable();
    num_stds = ucnv_countStandards();
    stds = (const char **) uprv_malloc(num_stds * sizeof(*stds));
    
    if (!stds) {
        u_wmsg("cantGetTag", u_wmsg_errorName(U_MEMORY_ALLOCATION_ERROR));
        return -1;
    } else {
        uint16_t s;
            
        for (s = 0; s < num_stds; ++s) {
            stds[s] = ucnv_getStandard(s, &err);
            if (U_FAILURE(err)) {
                u_wmsg("cantGetTag", u_wmsg_errorName(err));
                return -1;
            }
        }
    }
        
#if 0
    size_t numprint = 0;
    static const size_t maxline = 70;
#endif
        
    if (num <= 0)
        {
            initMsg(pname);   
            u_wmsg("cantGetNames");
            return -1;
        }
        
    for (int32_t i = 0; i<num; i++)
        {
            // ucnv_getAvailableName gets the codepage name at a specific
            // index
                
            const char *name = ucnv_getAvailableName(i);
            uint16_t num_aliases;

            if (lookfor && ucnv_compareNames(lookfor, name)) {
                continue;
            }
                
#if 0
            numprint += printf("%-20s", name);
            if (numprint>maxline)
                {
                    putchar('\n');
                    numprint = 0;
                }
#else
            err = U_ZERO_ERROR;
            num_aliases = ucnv_countAliases(name, &err);
            if (U_FAILURE(err)) {
                printf("%s", name);
                    
                UnicodeString str(name);
                putchar('\t');
                u_wmsg("cantGetAliases", str.getBuffer(), u_wmsg_errorName(err));
                return -1;
            } else {
                uint16_t a, s, t;
                    
                for (a = 0; a < num_aliases; ++a) {
                    const char *alias = ucnv_getAlias(name, a, &err);
                        
                    if (U_FAILURE(err)) {
                        UnicodeString str(name);
                        putchar('\t');
                        u_wmsg("cantGetAliases", str.getBuffer(), u_wmsg_errorName(err));
                        return -1;
                    }
                        
                    printf("%s", alias);
                        
                    /* Look (slowly) for a tag. */
                        
                    if (canon) {
                        for (s = t = 0; s < num_stds; ++s) {
                            const char *standard = ucnv_getStandardName(name, stds[s], &err);
                            if (U_SUCCESS(err) && standard) {
                                if (!strcmp(standard, alias)) {
                                    if (!t) {
                                        printf(" {");
                                        t = 1;
                                    }
                                    printf(" %s", stds[s]);
                                }
                            }
                        }
                        if (t) {
                            printf(" }");
                        }
                    }
                        
                    /* Move on. */
                        
                    if (a < num_aliases - 1) {
                        putchar(a || !canon ? ' ' : '\t');
                    }
                }
            }
            if (canon) {
                putchar('\n');
            } else if (i < num - 1) {
                putchar(' ');
            } 
                
#endif
        }

    return 0;
}

// Print all available transliterators
static int printTransliterators(const char *pname, int canon) {
    int32_t numtrans = utrans_countAvailableIDs(), i;
    int buflen = 512;
    char *buf = (char *) uprv_malloc(buflen);
    char staticbuf[512];

    char sepchar = canon ? '\n' : ' ';

    if (!buf) {
        buf = staticbuf;
        buflen = sizeof(staticbuf);
    }

    for (i = 0; i < numtrans; ++i) {
        int32_t len = utrans_getAvailableID(i, buf, buflen);
        if (len >= buflen -1) {
            if (buf != staticbuf) {
                buflen <<= 1;
                if (buflen < len) {
                    buflen = len + 64;
                }
                buf = (char *) uprv_realloc(buf, buflen);
                if (!buf) {
                    buf = staticbuf;
                    buflen = sizeof(staticbuf);
                }
            }
            utrans_getAvailableID(i, buf, buflen);
            if (len >= buflen) {
                strcpy(buf + buflen - 4, "...");
            }
        }

        printf("%s", buf);
        if (i < numtrans - 1) {
            putchar(sepchar);
        }
    }

    if (sepchar != '\n') {
        putchar('\n');
    }

    if (buf != staticbuf) {
        uprv_free(buf);
    }

    return 0;
}


// Convert a file from one encoding to another
static UBool convertFile(const char* fromcpage, 
                         UConverterToUCallback toucallback,
                         const void *touctxt,
                         const char* tocpage,
                         UConverterFromUCallback fromucallback,
                         const void *fromuctxt,
                         const char *translit,
                         FILE* infile, 
                         FILE* outfile)
{
  UBool ret = TRUE;
    UConverter* convfrom = 0;
    UConverter* convto = 0;
    UErrorCode err = U_ZERO_ERROR;
    UBool  flush;
    const char* cbuffiter;
    char* buffiter;
    const size_t readsize = buffsize-1;
    char* buff = 0;

    UConverterFromUCallback oldfromucallback;
    UConverterToUCallback oldtoucallback;
    const void *oldcontext;

    const UChar* cuniiter;
    UChar* uniiter;
    UChar* unibuff = 0;

    size_t rd, totbuffsize;

    Transliterator *t = NULL;

    if(translit != NULL && *translit)
      {
        UnicodeString str(translit);
        t = Transliterator::createInstance(str, UTRANS_FORWARD, err);
        if (U_FAILURE(err)) {
            u_wmsg("cantOpenTranslit", str.getBuffer(), u_wmsg_errorName(err));
            if (t) {
                delete t;
                t = 0;
            }
            goto error_exit;
        }
      }

    // Create codepage converter. If the codepage or its aliases weren't
    // available, it returns NULL and a failure code
    convfrom = ucnv_open(fromcpage, &err);
    if (U_FAILURE(err))
    {
      UnicodeString str(fromcpage,"");
      u_wmsg("cantOpenFromCodeset",str.getBuffer(),
             u_wmsg_errorName(err));
      goto error_exit;
    }
    ucnv_setToUCallBack(convfrom, toucallback, touctxt, &oldtoucallback, &oldcontext, &err);
    if (U_FAILURE(err))
    {
      u_wmsg("cantSetCallback", u_wmsg_errorName(err));
      goto error_exit;
    }

    convto = ucnv_open(tocpage, &err);
    if (U_FAILURE(err))
    {
      UnicodeString str(tocpage,"");
      u_wmsg("cantOpenToCodeset",str.getBuffer(),
             u_wmsg_errorName(err));
      goto error_exit;
    }
    ucnv_setFromUCallBack(convto, fromucallback, fromuctxt, &oldfromucallback, &oldcontext, &err);
    if (U_FAILURE(err))
    {
      u_wmsg("cantSetCallback", u_wmsg_errorName(err));
      goto error_exit;
    }

    // To ensure that the buffer always is of enough size, we
    // must take the worst case scenario, that is the character in the codepage
    // that uses the most bytes and multiply it against the buffsize
    totbuffsize = buffsize * ucnv_getMaxCharSize(convto);
    buff = new char[totbuffsize];
    unibuff = new UChar[buffsize];
        
    do  
    {
        rd = fread(buff, 1, readsize, infile);
        if (ferror(infile) != 0)
        {
            UnicodeString str(strerror(errno), "");
            u_wmsg("cantRead",str.getBuffer());
            goto error_exit;
        }
            
        // Convert the read buffer into the new coding
        // After the call 'uniiter' will be placed on the last character that was converted
        // in the 'unibuff'. 
        // Also the 'cbuffiter' is positioned on the last converted character.
        // At the last conversion in the file, flush should be set to true so that
        // we get all characters converted
        //
        // The converter must be flushed at the end of conversion so that characters
        // on hold also will be written
        uniiter = unibuff;
        cbuffiter = buff;
        flush = rd!=readsize;
        ucnv_toUnicode(convfrom, &uniiter, uniiter + buffsize, &cbuffiter, cbuffiter + rd, 0, flush, &err);
            
        if (U_FAILURE(err))
        {
            u_wmsg("problemCvtToU", u_wmsg_errorName(err));
            goto error_exit;
        }
            
        // At the last conversion, the converted characters should be equal to number
        // of chars read.
        if (flush && cbuffiter!=(buff+rd))
        {
            u_wmsg("premEndInput");
            goto error_exit;
        }
            
        // Convert the Unicode buffer into the destination codepage
        // Again 'buffiter' will be placed on the last converted character
        // And 'cuniiter' will be placed on the last converted unicode character
        // At the last conversion flush should be set to true to ensure that 
        // all characters left get converted

        UnicodeString u(unibuff, uniiter-unibuff);
        buffiter = buff;
        cuniiter = unibuff;

        if(t) 
          {
            t->transliterate(u);
            u.extract(0, u.length(), unibuff, 0);
            uniiter = unibuff + u.length();
            
          }

        ucnv_fromUnicode(convto, &buffiter, buffiter + totbuffsize, &cuniiter, cuniiter + (size_t) (uniiter - unibuff), 0, flush, &err);
            
        if (U_FAILURE(err))
        {
           u_wmsg("problemCvtFromU", u_wmsg_errorName(err));
           goto error_exit;
        }
                        
        // At the last conversion, the converted characters should be equal to number
        // of consumed characters.
        if (flush && cuniiter!=(unibuff+(size_t)(uniiter-unibuff)))
        {
          u_wmsg("premEnd");
          goto error_exit;
        }
            
        // Finally, write the converted buffer to the output file
        rd =  (size_t)(buffiter-buff);
        if (fwrite(buff, 1, rd, outfile) != rd)
        {
          UnicodeString str(strerror(errno),"");
          u_wmsg("cantWrite", str.getBuffer());
            goto error_exit;
        }
        
    } while (!flush); // Stop when we have flushed the converters (this means that it's the end of output)

    goto normal_exit;
  error_exit:
    ret = TRUE;
  normal_exit:
    if (convfrom) ucnv_close(convfrom);
    if (convto) ucnv_close(convto);

    if ( t ) delete t;

    // Close the created converters
    if (buff) delete [] buff;
    if (unibuff) delete [] unibuff;
    return ret;
}

static void usage(const char *pname, int ecode)
{
  const UChar *msg;
  int32_t      msgLen;
  UErrorCode  err = U_ZERO_ERROR;
   
  initMsg(pname);
  msg = ures_getStringByKey(gBundle, ecode ? "lcUsageWord" : "ucUsageWord", &msgLen, &err);
  UnicodeString upname(pname);
  UnicodeString mname(msg, msgLen);

  u_wmsg("usage", mname.getBuffer(), upname.getBuffer());
  if (!ecode) {
    putchar('\n');
    u_wmsg("help");

    /* Now dump callbacks and finish. */

    int i, count = sizeof(transcode_callbacks) / sizeof(*transcode_callbacks);
    for (i = 0; i < count; ++i) {
        printf(" %s", transcode_callbacks[i].name);
    }
    putchar('\n');
  }

  exit(ecode);
}

int main(int argc, char** argv)
{
    FILE* file = 0;
    FILE* infile;
    int   ret = 0;

    const char* fromcpage = 0;
    const char* tocpage = 0;
    const char *translit = 0;
    const char* infilestr = 0;

    UConverterFromUCallback fromucallback = UCNV_FROM_U_CALLBACK_SUBSTITUTE;
    const void *fromuctxt = 0;
    UConverterToUCallback toucallback = UCNV_TO_U_CALLBACK_SUBSTITUTE;
    const void *touctxt = 0;

    char** iter = argv+1;
    char** end = argv+argc;    

    const char *pname = *argv;

    int printConvs = 0, printCanon = 0;
    const char *printName = 0;
    int printTranslits = 0;

    // First, get the arguments from command-line
    // to know the codepages to convert between
    for (; iter!=end; iter++)
    {
        // Check for from charset
        if (strcmp("-f", *iter) == 0 || !strcmp("--from-code", *iter))
        {
            iter++;
            if (iter!=end)
                fromcpage = *iter;
        }
        else if (strcmp("-t", *iter) == 0 || !strcmp("--to-code", *iter))
        {
            iter++;
            if (iter!=end)
                tocpage = *iter;
        }
        else if (strcmp("-x", *iter) == 0)
        {
            iter++;
            if (iter!=end)
                translit = *iter;
            else
                usage(pname, 1);
        }
        else if (strcmp("-l", *iter) == 0 || !strcmp("--list", *iter))
        {
            if (printTranslits) {
                usage(pname, 1);
            }
            printConvs = 1;
        }
        else if (strcmp("--default-code", *iter) == 0)
        {
            if (printTranslits) {
                usage(pname, 1);
            }
            printName = ucnv_getDefaultName();
        }
        else if (strcmp("--list-code", *iter) == 0) {
            if (printTranslits) {
                usage(pname, 1);
            }

            iter++;
            if (iter!=end) {
                UErrorCode e = U_ZERO_ERROR;
                printName = ucnv_getAlias(*iter, 0, &e);
                if (U_FAILURE(e) || !printName) {
                    UnicodeString str(*iter);
                    initMsg(pname);
                    u_wmsg("noSuchCodeset", str.getBuffer());
                    return 2;
                }
            }
            else usage(pname, 1);
        }
        else if (strcmp("--canon", *iter) == 0) {
            printCanon = 1;
        }
        else if (strcmp("-L", *iter) == 0 || !strcmp("--list-transliterators", *iter))
        {
            if (printConvs) {
                usage(pname, 1);
            }
            printTranslits = 1;
        }
        else if (strcmp("-h", *iter) == 0 || !strcmp("-?", *iter)|| !strcmp("--help", *iter))
        {
            usage(pname, 0);
        }
        else if (!strcmp("-c", *iter)) {
            fromucallback = UCNV_FROM_U_CALLBACK_SKIP;
        }
        else if (!strcmp("--to-callback", *iter)) {
            iter++;
            if (iter!=end) {
                const struct callback_ent *cbe = findCallback(*iter);
                if (cbe) {
                    fromucallback = cbe->fromu;
                    fromuctxt = cbe->fromuctxt;
                } else {
                    UnicodeString str(*iter);
                    initMsg(pname);
                    u_wmsg("unknownCallback", str.getBuffer());
                    return 4;
                }
            } else {
                usage(pname, 1);
            }
        }
        else if (!strcmp("--from-callback", *iter)) {
            iter++;
            if (iter!=end) {
                const struct callback_ent *cbe = findCallback(*iter);
                if (cbe) {
                    toucallback = cbe->tou;
                    touctxt = cbe->touctxt;
                } else {
                    UnicodeString str(*iter);
                    initMsg(pname);
                    u_wmsg("unknownCallback", str.getBuffer());
                    return 4;
                }
            } else {
                usage(pname, 1);
            }
        }
        else if (!strcmp("-i", *iter)) {
            toucallback = UCNV_TO_U_CALLBACK_SKIP;
        }
        else if (**iter == '-' && (*iter)[1]) {
            usage(pname, 1);
        } else if (!infilestr) {
            infilestr = *iter;
        } else {
            usage(pname, 1);
        }
    }

    if (printConvs || printName) {
        return printConverters(pname, printName, printCanon) ? 2 : 0;
    } else if (printTranslits) {
        return printTransliterators(pname, printCanon) ? 3 : 0;
    }

    if (fromcpage==0 && tocpage==0)
    {
        usage(pname, 1);
    }

    if (fromcpage==0)
    {
      initMsg(pname);
      u_wmsg("noFromCodeset");
      //"No conversion from codeset given (use -f)\n");
        goto error_exit;
    }
    if (tocpage==0)
    {
      initMsg(pname);
      u_wmsg("noToCodeset");
      // "No conversion to codeset given (use -t)\n");
      goto error_exit;
    }

    // Open the correct input file or connect to stdin for reading input
    if (infilestr!=0 && strcmp(infilestr, "-"))
    {
        file = fopen(infilestr, "rb");
        if (file==0)
        {
          UnicodeString str1(infilestr,"");
          UnicodeString str2(strerror(errno),"");
          initMsg(pname);
          u_wmsg("cantOpenInputF", 
                 str1.getBuffer(),
                 str2.getBuffer());
          return 1;
        }
        infile = file;
    }
    else {
        infile = stdin;
#ifdef WIN32
        if( setmode( fileno ( stdin ), O_BINARY ) == -1 ) {
                perror ( "Cannot set stdin to binary mode" );
                exit(-1);
        }
#endif
    }
#ifdef WIN32
  if( setmode( fileno ( stdout ), O_BINARY ) == -1 ) {
          perror ( "Cannot set stdout to binary mode" );
          exit(-1);
  }
#endif

  initMsg(pname);
    if (!convertFile(fromcpage, toucallback, touctxt, tocpage, fromucallback, fromuctxt, translit, infile, stdout))
        goto error_exit;

    goto normal_exit;
  error_exit:
    ret = 1;
  normal_exit:

    if (file!=0)
        fclose(file);
    return ret;
}


/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
