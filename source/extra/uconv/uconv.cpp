/*****************************************************************************
*
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************/

/*
 * uconv(1): an iconv(1)-like converter using ICU.
 *
 * Original code by Jonas Utterström <jonas.utterstrom@vittran.norrnod.se>
 * contributed in 1999.
 *
 * Conversion to the C conversion API and many improvements by
 * Yves Arrouye <yves@realnames.com>, current maintainer.
 *
 */

#include <unicode/utypes.h>
#include <unicode/ucnv.h>
#include <unicode/unistr.h>
#include <unicode/translit.h>

#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>

#include "cmemory.h"
#include "cstring.h"
#include "ustrfmt.h"

#include "unicode/uwmsg.h"

#ifdef WIN32
#include <string.h>
#include <io.h>
#include <fcntl.h>
#endif

#ifdef UCONVMSG_LINK
/* below from the README */
#include "unicode/utypes.h"
#include "unicode/udata.h"
U_CFUNC char uconvmsg_dat[];
#endif

#define DEFAULT_BUFSZ   4096
#define UCONVMSG "uconvmsg"

static UResourceBundle *gBundle = 0;    /* Bundle containing messages. */

/*
 * Initialize the message bundle so that message strings can be fetched
 * by u_wmsg().
 *
 */

static void initMsg(const char *pname) {
    static int ps = 0;

    if (!ps) {
        char dataPath[2048];        /* XXX Sloppy: should be PATH_MAX. */
        UErrorCode err = U_ZERO_ERROR;

        ps = 1;

        /* Set up our static data - if any */
#ifdef UCONVMSG_LINK
        udata_setAppData(UCONVMSG, (const void*) uconvmsg_dat, &err);
        if (U_FAILURE(err)) {
          fprintf(stderr, "%s: warning, problem installing our static resource bundle data uconvmsg: %s - trying anyways.\n",
                  pname, u_errorName(err));
          err = U_ZERO_ERROR; /* It may still fail */
        }
#endif

        /* Get messages. */
        gBundle = u_wmsg_setPath(UCONVMSG, &err);
        if (U_FAILURE(err)) {
            fprintf(stderr,
                    "%s: warning: couldn't open bundle %s: %s\n",
                    pname, UCONVMSG, u_errorName(err));
#ifdef UCONVMSG_LINK
            fprintf(stderr,
                    "%s: setAppData was called, internal data %s failed to load\n",
                        pname, UCONVMSG);
#endif
 
            err = U_ZERO_ERROR;
            /* that was try #1, try again with a path */
            uprv_strcpy(dataPath, u_getDataDirectory());
            uprv_strcat(dataPath, U_FILE_SEP_STRING);
            uprv_strcat(dataPath, UCONVMSG);

            gBundle = u_wmsg_setPath(dataPath, &err);
            if (U_FAILURE(err)) {
                fprintf(stderr,
                    "%s: warning: still couldn't open bundle %s: %s\n",
                    pname, dataPath, u_errorName(err));
                fprintf(stderr, "%s: warning: messages will not be displayed\n", pname);
            }
        }
    }
}

/* Mapping of callback names to the callbacks passed to the converter
   API. */

static struct callback_ent {
    const char *name;
    UConverterFromUCallback fromu;
    const void *fromuctxt;
    UConverterToUCallback tou;
    const void *touctxt;
} transcode_callbacks[] = {
    { "substitute",
      UCNV_FROM_U_CALLBACK_SUBSTITUTE, 0,
      UCNV_TO_U_CALLBACK_SUBSTITUTE, 0 },
    { "skip",
      UCNV_FROM_U_CALLBACK_SKIP, 0,
      UCNV_TO_U_CALLBACK_SKIP, 0 },
    { "stop",
      UCNV_FROM_U_CALLBACK_STOP, 0,
      UCNV_TO_U_CALLBACK_STOP, 0 },
    { "escape",
      UCNV_FROM_U_CALLBACK_ESCAPE, 0,
      UCNV_TO_U_CALLBACK_ESCAPE, 0},
    { "escape-icu",
      UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_ICU,
      UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_ICU },
    { "escape-java",
      UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_JAVA,
      UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_JAVA },
    { "escape-c",
      UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_C,
      UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_C },
    { "escape-xml",
      UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_HEX,
      UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_HEX },
    { "escape-xml-hex",
      UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_HEX,
      UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_HEX },
    { "escape-xml-dec",
      UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_DEC,
      UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_XML_DEC },
    { "escape-unicode", UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_UNICODE,
      UCNV_TO_U_CALLBACK_ESCAPE, UCNV_ESCAPE_UNICODE }
};

/* Return a pointer to a callback record given its name. */

static const struct callback_ent *findCallback(const char *name) {
    int i, count =
        sizeof(transcode_callbacks) / sizeof(*transcode_callbacks);

    /* We'll do a linear search, there aren't many of them and bsearch()
       may not be that portable. */

    for (i = 0; i < count; ++i) {
        if (!uprv_stricmp(name, transcode_callbacks[i].name)) {
            return &transcode_callbacks[i];
        }
    }

    return 0;
}

/* Print converter information. If lookfor is set, only that converter will
   be printed, otherwise all converters will be printed. If canon is non
   zero, tags and aliases for each converter are printed too, in the format
   expected for convrters.txt(5). */

static int printConverters(const char *pname, const char *lookfor,
    int canon)
{
    UErrorCode err = U_ZERO_ERROR;
    int32_t num;
    uint16_t num_stds;
    const char **stds;

    /* If there is a specified name, just handle that now. */

    if (lookfor) {
        if (!canon) {
            printf("%s\n", lookfor);
            return 0;
        } else {
        /*  Because we are printing a canonical name, we need the
            true converter name. We've done that already except for
            the default name (because we want to print the exact
            name one would get when calling ucnv_getDefaultName()
            in non-canon mode). But since we do not know at this
            point if we have the default name or something else, we
            need to normalize again to the canonical converter
            name. */

            const char *truename = ucnv_getAlias(lookfor, 0, &err);
            if (U_SUCCESS(err)) {
                lookfor = truename;
            } else {
                err = U_ZERO_ERROR;
            }
        }
    }

    /* Print converter names. We come here for one of two reasons: we
       are printing all the names (lookfor was null), or we have a
       single converter to print but in canon mode, hence we need to
       get to it in order to print everything. */

    num = ucnv_countAvailable();
    if (num <= 0) {
        initMsg(pname);
        u_wmsg(stderr, "cantGetNames");
        return -1;
    }
    if (lookfor) {
        num = 1;                /* We know where we want to be. */
    }

    num_stds = ucnv_countStandards();
    stds = (const char **) uprv_malloc(num_stds * sizeof(*stds));
    if (!stds) {
        u_wmsg(stderr, "cantGetTag", u_wmsg_errorName(U_MEMORY_ALLOCATION_ERROR));
        return -1;
    } else {
        uint16_t s;

        for (s = 0; s < num_stds; ++s) {
            stds[s] = ucnv_getStandard(s, &err);
            if (U_FAILURE(err)) {
                u_wmsg(stderr, "cantGetTag", u_wmsg_errorName(err));
                return -1;
            }
        }
    }

    for (int32_t i = 0; i < num; i++) {
        const char *name;
        uint16_t num_aliases;

        /* Set the name either to what we are looking for, or
        to the current converter name. */

        if (lookfor) {
            name = lookfor;
        } else {
            name = ucnv_getAvailableName(i);
        }

        /* Get all the aliases associated to the name. */

        err = U_ZERO_ERROR;
        num_aliases = ucnv_countAliases(name, &err);
        if (U_FAILURE(err)) {
            printf("%s", name);

            UnicodeString str(name, (int32_t)(uprv_strlen(name) + 1));
            putchar('\t');
            u_wmsg(stderr, "cantGetAliases", str.getBuffer(),
                u_wmsg_errorName(err));
            return -1;
        } else {
            uint16_t a, s, t;

            /* Write all the aliases and their tags. */

            for (a = 0; a < num_aliases; ++a) {
                const char *alias = ucnv_getAlias(name, a, &err);

                if (U_FAILURE(err)) {
                    UnicodeString str(name, (int32_t)(uprv_strlen(name) + 1));
                    putchar('\t');
                    u_wmsg(stderr, "cantGetAliases", str.getBuffer(),
                        u_wmsg_errorName(err));
                    return -1;
                }

                printf("%s", alias);

                /* Look (slowly, linear searching) for a tag. */

                if (canon) {
                    for (s = t = 0; s < num_stds; ++s) {
                        const char *standard =
                            ucnv_getStandardName(name, stds[s], &err);
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

        /* Terminate this entry. */

        if (canon) {
            putchar('\n');
        } else if (i < num - 1) {
            putchar(' ');
        }
    }

    /* Free temporary data. */

    uprv_free(stds);

    /* Success. */

    return 0;
}

/* Print all available transliterators. If canon is non zero, print
   one transliterator per line. */

static int printTransliterators(int canon)
{
#if UCONFIG_NO_TRANSLITERATION
    printf("no transliterators available because of UCONFIG_NO_TRANSLITERATION, see uconfig.h\n");
    return 1;
#else
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
        if (len >= buflen - 1) {
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
                uprv_strcpy(buf + buflen - 4, "..."); /* Truncate the name. */
            }
        }

        printf("%s", buf);
        if (i < numtrans - 1) {
            putchar(sepchar);
        }
    }

    /* Add a terminating newline if needed. */

    if (sepchar != '\n') {
        putchar('\n');
    }

    /* Free temporary data. */

    if (buf != staticbuf) {
        uprv_free(buf);
    }

    /* Success. */

    return 0;
#endif
}

/* Return the offset of a byte in its source, given the from and to offsets
   vectors and the byte offset itself. */

static inline int32_t dataOffset(int32_t whereto,
    const int32_t *fromoffsets, int32_t fromsz,
    const int32_t *tooffsets, int32_t tosz) {
    if (whereto >= 0 && whereto < tosz) {
        whereto = tooffsets[whereto];
        if (whereto >= 0 && whereto < fromsz) {
            return fromoffsets[whereto];
        }
    }

    return 0;
}

// Convert a file from one encoding to another
static UBool convertFile(const char *pname,
                         const char *fromcpage,
                         UConverterToUCallback toucallback,
                         const void *touctxt,
                         const char *tocpage,
                         UConverterFromUCallback fromucallback,
                         const void *fromuctxt,
                         int fallback,
                         size_t bufsz,
                         const char *translit,
                         const char *infilestr,
                         FILE * outfile, int verbose)
{
    FILE *infile;
    UBool ret = TRUE;
    UConverter *convfrom = 0;
    UConverter *convto = 0;
    UErrorCode err = U_ZERO_ERROR;
    UBool flush;
    const char *cbufp;
    char *bufp;
    char *buf = 0;

    uint32_t infoffset = 0, outfoffset = 0;   /* Where we are in the file, for error reporting. */

    const UChar *unibufbp;
    UChar *unibufp;
    UChar *unibuf = 0;
    int32_t *fromoffsets = 0, *tooffsets = 0;

    size_t rd, wr, tobufsz;

#if !UCONFIG_NO_TRANSLITERATION
    Transliterator *t = 0;      // Transliterator acting on Unicode data.
#endif
    UnicodeString u;            // String to do the transliteration.

    // Open the correct input file or connect to stdin for reading input

    if (infilestr != 0 && strcmp(infilestr, "-")) {
        infile = fopen(infilestr, "rb");
        if (infile == 0) {
            UnicodeString str1(infilestr, "");
            str1.append((UChar32) 0);
            UnicodeString str2(strerror(errno), "");
            str2.append((UChar32) 0);
            initMsg(pname);
            u_wmsg(stderr, "cantOpenInputF", str1.getBuffer(), str2.getBuffer());
            return FALSE;
        }
    } else {
        infilestr = "-";
        infile = stdin;
#ifdef WIN32
        if (setmode(fileno(stdin), O_BINARY) == -1) {
            initMsg(pname);
            u_wmsg(stderr, "cantSetInBinMode");
            return FALSE;
        }
#endif
    }

    if (verbose) {
        fprintf(stderr, "%s:\n", infilestr);
    }

#if !UCONFIG_NO_TRANSLITERATION
    // Create transliterator as needed.

    if (translit != NULL && *translit) {
        UParseError parse;
        UnicodeString str(translit), pestr;

        /* Create from rules or by ID as needed. */

        parse.line = -1;

        if (uprv_strchr(translit, ':') || uprv_strchr(translit, '>') || uprv_strchr(translit, '<') || uprv_strchr(translit, '>')) {
            t = Transliterator::createFromRules("Uconv", str, UTRANS_FORWARD, parse, err);
        } else {
            t = Transliterator::createInstance(translit, UTRANS_FORWARD, err);
        }

        if (U_FAILURE(err)) {
            str.append((UChar32) 0);
            initMsg(pname);

            if (parse.line >= 0) {
                UChar linebuf[20], offsetbuf[20];
                uprv_itou(linebuf, 20, parse.line, 10, 0);
                uprv_itou(offsetbuf, 20, parse.offset, 10, 0);
                u_wmsg(stderr, "cantCreateTranslitParseErr", str.getBuffer(),
                    u_wmsg_errorName(err), linebuf, offsetbuf);
            } else {
                u_wmsg(stderr, "cantCreateTranslit", str.getBuffer(),
                    u_wmsg_errorName(err));
            }

            if (t) {
                delete t;
                t = 0;
            }
            goto error_exit;
        }
    }
#endif

    // Create codepage converter. If the codepage or its aliases weren't
    // available, it returns NULL and a failure code. We also set the
    // callbacks, and return errors in the same way.

    convfrom = ucnv_open(fromcpage, &err);
    if (U_FAILURE(err)) {
        UnicodeString str(fromcpage, (int32_t)(uprv_strlen(fromcpage) + 1));
        initMsg(pname);
        u_wmsg(stderr, "cantOpenFromCodeset", str.getBuffer(),
            u_wmsg_errorName(err));
        goto error_exit;
    }
    ucnv_setToUCallBack(convfrom, toucallback, touctxt, 0, 0, &err);
    if (U_FAILURE(err)) {
        initMsg(pname);
        u_wmsg(stderr, "cantSetCallback", u_wmsg_errorName(err));
        goto error_exit;
    }

    convto = ucnv_open(tocpage, &err);
    if (U_FAILURE(err)) {
        UnicodeString str(tocpage, (int32_t)(uprv_strlen(tocpage) + 1));
        initMsg(pname);
        u_wmsg(stderr, "cantOpenToCodeset", str.getBuffer(),
            u_wmsg_errorName(err));
        goto error_exit;
    }
    ucnv_setFromUCallBack(convto, fromucallback, fromuctxt, 0, 0, &err);
    if (U_FAILURE(err)) {
        initMsg(pname);
        u_wmsg(stderr, "cantSetCallback", u_wmsg_errorName(err));
        goto error_exit;
    }
    ucnv_setFallback(convto, fallback);

    // To ensure that the buffer always is of enough size, we
    // must take the worst case scenario, that is the character in
    // the codepage that uses the most bytes and multiply it against
    // the buffer size.

    // use bufsz+1 to allow for additional BOM/signature character (U+FEFF)
    tobufsz = (bufsz+1) * ucnv_getMaxCharSize(convto);

    buf = new char[tobufsz];
    unibuf = new UChar[bufsz];

    fromoffsets = new int32_t[bufsz];
    tooffsets = new int32_t[tobufsz];

    // OK, we can convert now.

    do {
        char willexit = 0;

        rd = fread(buf, 1, bufsz, infile);
        if (ferror(infile) != 0) {
            UnicodeString str(strerror(errno));
            str.append((UChar32) 0);
            initMsg(pname);
            u_wmsg(stderr, "cantRead", str.getBuffer());
            goto error_exit;
        }

        // Convert the read buffer into the new coding
        // After the call 'unibufp' will be placed on the last
        // character that was converted in the 'unibuf'.
        // Also the 'cbufp' is positioned on the last converted
        // character.
        // At the last conversion in the file, flush should be set to
        // true so that we get all characters converted
        //
        // The converter must be flushed at the end of conversion so
        // that characters on hold also will be written.

        unibufp = unibuf;
        cbufp = buf;
        flush = rd != bufsz;
        ucnv_toUnicode(convfrom, &unibufp, unibufp + bufsz, &cbufp,
            cbufp + rd, fromoffsets, flush, &err);

        infoffset += (uint32_t)(cbufp - buf);

        if (U_FAILURE(err)) {
            char pos[32];
            sprintf(pos, "%u", infoffset - 1);
            UnicodeString str(pos, (int32_t)(uprv_strlen(pos) + 1));
            initMsg(pname);
            u_wmsg(stderr, "problemCvtToU", str.getBuffer(), u_wmsg_errorName(err));
            willexit = 1;
            err = U_ZERO_ERROR; /* reset the error for the rest of the conversion. */
        }

        // At the last conversion, the converted characters should be
        // equal to number of chars read.

        if (flush && !willexit && cbufp != (buf + rd)) {
            char pos[32];
            sprintf(pos, "%u", infoffset);
            UnicodeString str(pos, (int32_t)(uprv_strlen(pos) + 1));
            initMsg(pname);
            u_wmsg(stderr, "premEndInput", str.getBuffer());
            willexit = 1;
        }

        // Prepare to transliterate and convert. Transliterate if needed.

#if !UCONFIG_NO_TRANSLITERATION
        if (t) {
            u.setTo(unibuf, (int32_t)(unibufp - unibuf)); // Copy into string.
            t->transliterate(u);
        } else
#endif
        {
            u.setTo(unibuf, (int32_t)(unibufp - unibuf), (int32_t)(bufsz)); // Share the buffer.
        }

        int32_t ulen = u.length();

        // Convert the Unicode buffer into the destination codepage
        // Again 'bufp' will be placed on the last converted character
        // And 'unibufbp' will be placed on the last converted unicode character
        // At the last conversion flush should be set to true to ensure that
        // all characters left get converted

        const UChar *unibufu = unibufbp = u.getBuffer();

        do {
            int32_t len = ulen > (int32_t)bufsz ? (int32_t)bufsz : ulen;

            bufp = buf;
            unibufp = (UChar *) (unibufbp + len);

            ucnv_fromUnicode(convto, &bufp, bufp + tobufsz,
                             &unibufbp,
                             unibufp,
                             tooffsets, flush, &err);

            if (U_FAILURE(err)) {
                const char *errtag;
                char pos[32];

                uint32_t erroffset =
                    dataOffset((int32_t)(bufp - buf - 1), fromoffsets, (int32_t)(bufsz), tooffsets, (int32_t)(tobufsz));
                int32_t ferroffset = (int32_t)(infoffset - (unibufp - unibufu) + erroffset);

                if ((int32_t) ferroffset < 0) {
                    ferroffset = (int32_t)(outfoffset + (bufp - buf));
                    errtag = "problemCvtFromUOut";
                } else {
                    errtag = "problemCvtFromU";
                }
                sprintf(pos, "%u", ferroffset);
                UnicodeString str(pos, (int32_t)(uprv_strlen(pos) + 1));
                initMsg(pname);
                u_wmsg(stderr, errtag, str.getBuffer(),
                       u_wmsg_errorName(err));
                willexit = 1;
            }

            // At the last conversion, the converted characters should be equal to number
            // of consumed characters.
            if (flush && !willexit && unibufbp != (unibufu + (size_t) (unibufp - unibufu))) {
                char pos[32];
                sprintf(pos, "%u", infoffset);
                UnicodeString str(pos, (int32_t)(uprv_strlen(pos) + 1));
                initMsg(pname);
                u_wmsg(stderr, "premEnd", str.getBuffer());
                willexit = 1;
            }

            // Finally, write the converted buffer to the output file


            rd = (size_t) (bufp - buf);
            outfoffset += (int32_t)(wr = fwrite(buf, 1, rd, outfile));
            if (wr != rd) {
                UnicodeString str(strerror(errno), "");
                initMsg(pname);
                u_wmsg(stderr, "cantWrite", str.getBuffer());
                willexit = 1;
            }

            if (willexit) {
                goto error_exit;
            }
        } while ((ulen -= (int32_t)(bufsz)) > 0);
    } while (!flush);           // Stop when we have flushed the
                                // converters (this means that it's
                                // the end of output)

    goto normal_exit;

error_exit:
    ret = FALSE;

normal_exit:
    // Cleanup.

    if (convfrom) ucnv_close(convfrom);
    if (convto) ucnv_close(convto);

#if !UCONFIG_NO_TRANSLITERATION
    if (t) delete t;
#endif

    if (buf) delete[] buf;
    if (unibuf) delete[] unibuf;

    if (fromoffsets) delete[] fromoffsets;
    if (tooffsets) delete[] tooffsets;

    if (infile != stdin) {
        fclose(infile);
    }

    return ret;
}

static void usage(const char *pname, int ecode) {
    const UChar *msg;
    int32_t msgLen;
    UErrorCode err = U_ZERO_ERROR;
    FILE *fp = ecode ? stderr : stdout;
    int res;

    initMsg(pname);
    msg =
        ures_getStringByKey(gBundle, ecode ? "lcUsageWord" : "ucUsageWord",
                            &msgLen, &err);
    UnicodeString upname(pname, (int32_t)(uprv_strlen(pname) + 1));
    UnicodeString mname(msg, msgLen + 1);

    res = u_wmsg(fp, "usage", mname.getBuffer(), upname.getBuffer());
    if (!ecode) {
        if (!res) {
            fputc('\n', fp);
        }
        if (!u_wmsg(fp, "help")) {
            /* Now dump callbacks and finish. */

            int i, count =
                sizeof(transcode_callbacks) / sizeof(*transcode_callbacks);
            for (i = 0; i < count; ++i) {
                fprintf(fp, " %s", transcode_callbacks[i].name);
            }
            fputc('\n', fp);
        }
    }

    exit(ecode);
}

int main(int argc, char **argv)
{
    FILE *outfile;
    int ret = 0;
    int seenf = 0;

    size_t bufsz = DEFAULT_BUFSZ;

    const char *fromcpage = 0;
    const char *tocpage = 0;
    const char *translit = 0;
    const char *outfilestr = 0;
    int fallback = 0;

    UConverterFromUCallback fromucallback = UCNV_FROM_U_CALLBACK_STOP;
    const void *fromuctxt = 0;
    UConverterToUCallback toucallback = UCNV_TO_U_CALLBACK_STOP;
    const void *touctxt = 0;

    char **iter;
    char **end = argv + argc;

    const char *pname;

    int printConvs = 0, printCanon = 0;
    const char *printName = 0;
    int printTranslits = 0;

    int verbose = 0;

    // Get and prettify pname.
    pname = uprv_strrchr(*argv, U_FILE_SEP_CHAR);
#ifdef WIN32
    if (!pname) {
        pname = uprv_strrchr(*argv, '/');
    }
#endif
    if (!pname) {
        pname = *argv;
    } else {
        ++pname;
    }

    // First, get the arguments from command-line
    // to know the codepages to convert between

    // XXX When you add to this loop, you need to add to the similar loop
    // below.

    for (iter = argv + 1; iter != end; iter++) {
        // Check for from charset
        if (strcmp("-f", *iter) == 0 || !strcmp("--from-code", *iter)) {
            iter++;
            if (iter != end)
                fromcpage = *iter;
            else
                usage(pname, 1);
        } else if (strcmp("-t", *iter) == 0 || !strcmp("--to-code", *iter)) {
            iter++;
            if (iter != end)
                tocpage = *iter;
            else
                usage(pname, 1);
        } else if (strcmp("-x", *iter) == 0) {
            iter++;
            if (iter != end)
                translit = *iter;
            else
                usage(pname, 1);
        } else if (!strcmp("--fallback", *iter)) {
            fallback = 1;
        } else if (!strcmp("--no-fallback", *iter)) {
            fallback = 0;
        } else if (strcmp("-b", *iter) == 0 || !strcmp("--block-size", *iter)) {
            iter++;
            if (iter != end) {
                bufsz = atoi(*iter);
                if ((int) bufsz <= 0) {
                    initMsg(pname);
                    UnicodeString str(*iter);
                    initMsg(pname);
                    u_wmsg(stderr, "badBlockSize", str.getBuffer());
                    return 3;
                }
            } else {
                usage(pname, 1);
            }
        } else if (strcmp("-l", *iter) == 0 || !strcmp("--list", *iter)) {
            if (printTranslits) {
                usage(pname, 1);
            }
            printConvs = 1;
        } else if (strcmp("--default-code", *iter) == 0) {
            if (printTranslits) {
                usage(pname, 1);
            }
            printName = ucnv_getDefaultName();
        } else if (strcmp("--list-code", *iter) == 0) {
            if (printTranslits) {
                usage(pname, 1);
            }

            iter++;
            if (iter != end) {
                UErrorCode e = U_ZERO_ERROR;
                printName = ucnv_getAlias(*iter, 0, &e);
                if (U_FAILURE(e) || !printName) {
                    UnicodeString str(*iter);
                    initMsg(pname);
                    u_wmsg(stderr, "noSuchCodeset", str.getBuffer());
                    return 2;
                }
            } else
                usage(pname, 1);
        } else if (strcmp("--canon", *iter) == 0) {
            printCanon = 1;
        } else if (strcmp("-L", *iter) == 0
            || !strcmp("--list-transliterators", *iter)) {
            if (printConvs) {
                usage(pname, 1);
            }
            printTranslits = 1;
        } else if (strcmp("-h", *iter) == 0 || !strcmp("-?", *iter)
            || !strcmp("--help", *iter)) {
            usage(pname, 0);
        } else if (!strcmp("-c", *iter)) {
            fromucallback = UCNV_FROM_U_CALLBACK_SKIP;
        } else if (!strcmp("--to-callback", *iter)) {
            iter++;
            if (iter != end) {
                const struct callback_ent *cbe = findCallback(*iter);
                if (cbe) {
                    fromucallback = cbe->fromu;
                    fromuctxt = cbe->fromuctxt;
                } else {
                    UnicodeString str(*iter);
                    initMsg(pname);
                    u_wmsg(stderr, "unknownCallback", str.getBuffer());
                    return 4;
                }
            } else {
                usage(pname, 1);
            }
        } else if (!strcmp("--from-callback", *iter)) {
            iter++;
            if (iter != end) {
                const struct callback_ent *cbe = findCallback(*iter);
                if (cbe) {
                    toucallback = cbe->tou;
                    touctxt = cbe->touctxt;
                } else {
                    UnicodeString str(*iter);
                    initMsg(pname);
                    u_wmsg(stderr, "unknownCallback", str.getBuffer());
                    return 4;
                }
            } else {
                usage(pname, 1);
            }
        } else if (!strcmp("-i", *iter)) {
            toucallback = UCNV_TO_U_CALLBACK_SKIP;
        } else if (!strcmp("--callback", *iter)) {
            iter++;
            if (iter != end) {
                const struct callback_ent *cbe = findCallback(*iter);
                if (cbe) {
                    fromucallback = cbe->fromu;
                    fromuctxt = cbe->fromuctxt;
                    toucallback = cbe->tou;
                    touctxt = cbe->touctxt;
                } else {
                    UnicodeString str(*iter);
                    initMsg(pname);
                    u_wmsg(stderr, "unknownCallback", str.getBuffer());
                    return 4;
                }
            } else {
                usage(pname, 1);
            }
        } else if (!strcmp("-s", *iter) || !strcmp("--silent", *iter)) {
            verbose = 0;
        } else if (!strcmp("-v", *iter) || !strcmp("--verbose", *iter)) {
            verbose = 1;
        } else if (!strcmp("-V", *iter) || !strcmp("--version", *iter)) {
            printf("%s v2.0\n", pname);
            return 0;
        } else if (!strcmp("-o", *iter) || !strcmp("--output", *iter)) {
            ++iter;
            if (iter != end && !outfilestr) {
                outfilestr = *iter;
            } else {
                usage(pname, 1);
            }
        } else if (**iter == '-' && (*iter)[1]) {
            usage(pname, 1);
        }
    }

    if (printConvs || printName) {
        return printConverters(pname, printName, printCanon) ? 2 : 0;
    } else if (printTranslits) {
        return printTransliterators(printCanon) ? 3 : 0;
    }

    if (!fromcpage || !uprv_strcmp(fromcpage, "-")) {
        fromcpage = ucnv_getDefaultName();
    }
    if (!tocpage || !uprv_strcmp(tocpage, "-")) {
        tocpage = ucnv_getDefaultName();
    }

    // Open the correct output file or connect to stdout for reading input
    if (outfilestr != 0 && strcmp(outfilestr, "-")) {
        outfile = fopen(outfilestr, "wb");
        if (outfile == 0) {
            UnicodeString str1(outfilestr, "");
            UnicodeString str2(strerror(errno), "");
            initMsg(pname);
            u_wmsg(stderr, "cantCreateOutputF",
                str1.getBuffer(), str2.getBuffer());
            return 1;
        }
    } else {
        outfilestr = "-";
        outfile = stdout;
#ifdef WIN32
        if (setmode(fileno(outfile), O_BINARY) == -1) {
            u_wmsg(stderr, "cantSetOutBinMode");
            exit(-1);
        }
#endif
    }

    /* Loop again on the arguments to find all the input files, and
    convert them. XXX Cheap and sloppy. */

    for (iter = argv + 1; iter != end; iter++) {
        if (strcmp("-f", *iter) == 0 || !strcmp("--from-code", *iter)) {
            iter++;
        } else if (strcmp("-t", *iter) == 0 || !strcmp("--to-code", *iter)) {
            iter++;
        } else if (strcmp("-x", *iter) == 0) {
            iter++;
        } else if (!strcmp("--fallback", *iter)) {
            ;
        } else if (!strcmp("--no-fallback", *iter)) {
            ;
        } else if (strcmp("-b", *iter) == 0 || !strcmp("--block-size", *iter)) {
            iter++;
        } else if (strcmp("-l", *iter) == 0 || !strcmp("--list", *iter)) {
            ;
        } else if (strcmp("--default-code", *iter) == 0) {
            ;
        } else if (strcmp("--list-code", *iter) == 0) {
            ;
        } else if (strcmp("--canon", *iter) == 0) {
            ;
        } else if (strcmp("-L", *iter) == 0
            || !strcmp("--list-transliterators", *iter)) {
            ;
        } else if (strcmp("-h", *iter) == 0 || !strcmp("-?", *iter)
            || !strcmp("--help", *iter)) {
            ;
        } else if (!strcmp("-c", *iter)) {
            ;
        } else if (!strcmp("--to-callback", *iter)) {
            iter++;
        } else if (!strcmp("--from-callback", *iter)) {
            iter++;
        } else if (!strcmp("-i", *iter)) {
            ;
        } else if (!strcmp("--callback", *iter)) {
            iter++;
        } else if (!strcmp("-s", *iter) || !strcmp("--silent", *iter)) {
            ;
        } else if (!strcmp("-v", *iter) || !strcmp("--verbose", *iter)) {
            ;
        } else if (!strcmp("-V", *iter) || !strcmp("--version", *iter)) {
            ;
        } else if (!strcmp("-o", *iter) || !strcmp("--output", *iter)) {
            ++iter;
        } else {
            seenf = 1;
            if (!convertFile
                (pname, fromcpage, toucallback, touctxt, tocpage,
                fromucallback, fromuctxt, fallback, bufsz, translit, *iter,
                outfile, verbose)) {
                goto error_exit;
            }
        }
    }

    if (!seenf) {
        if (!convertFile
            (pname, fromcpage, toucallback, touctxt, tocpage,
            fromucallback, fromuctxt, fallback, bufsz, translit, 0, outfile,
            verbose)) {
            goto error_exit;
        }
    }

    goto normal_exit;
error_exit:
    ret = 1;
normal_exit:

    if (outfile != stdout)
        fclose(outfile);

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
