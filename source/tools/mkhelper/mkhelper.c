/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   12/05/99    aliu        Creation.
**********************************************************************
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"

/*********************************************************************
mkhelper exists to enable ICU to use some of the same build files that
are used on UNIX.  It was originally written to support the building
of the numerous ICU converters on Windows, but it is general enough to
be useful in other contexts.  mkhelper is useful only on Windows; on
UNIX you would accomplish the same task with a ten line perl script.

mkhelper reads one or more input files and parses them looking for one
or more makefile variable name definitions.  For example, a typical
input file looks like this:

  # A list of UCM's to build

  UCM_SOURCE = ibm-1038.ucm ibm-1047.ucm ibm-1089.ucm ibm-1123.ucm \
  ibm-1140.ucm ibm-1141.ucm ibm-1142.ucm ibm-1143.ucm ibm-1144.ucm

For this file, with no string-modifying options specified, mkhelper
would emit the following to stdout:

  ibm-1038.ucm
  ibm-1047.ucm
  ...
  ibm-1144.ucm

The input file can contain comments, blank lines, and variable
definitions.  Lines are continued if they end with '\'.  The variable
definitions assign a variable name (UCM_SOURCE in the above example)
to a list of zero or more strings.

mkhelper reads one or more files of this format.  Its parsing is very
rudimentary, so input files cannot contain anything other than the
elements described above.  It looks for variable names specified on
the command line, and records their definitions.  It then does
optional string substitution on those definition strings.  Finally it
emits them to stdout, one per line, with optional prefix and/or suffix
strings.

If multiple files are read, definitions in later files (on the command
line) replace definitions with the same name in earlier files.

Because of problems with the Windows shell, it's difficult to use
environment variables of the form %VAR% on the command line.  To get
around this, mkhelper supports the alternative syntax $VAR$ in the
prefix, suffix, and string substitution options.  Use "$$" to indicate
the literal character '$'.  If a substring of the form $VAR$ is seen
in one of these command line parameters, mkhelper interprets it as an
environment variable and substitutes its definition.

Alan Liu 12/5/99 ****************************************************/

/* GLOBALS THAT ENCODE THE COMMAND-LINE ARGS */
const char* PREFIX = NULL; /* [-p] Prepend to each line */
const char* SUFFIX = NULL; /* [-s] Append to each line */
const char* OLD = NULL; /* [-old] Old pattern */
const char* NEW = NULL; /* [-new] New pattern */
#define     MAX_VARS 8
int         VARS_COUNT = 0;
const char* VARS[MAX_VARS]; /* [-v] Variables to look for */
#define     MAX_FILES 8
int         FILES_COUNT = 0;
const char* FILES[MAX_FILES]; /* Input files*/

/* GLOBALS THAT STORE THE VAR DEFS FROM THE FILES */
int         VARS_DEF_COUNT[MAX_VARS]; /* Number of names in this var's def */
int         VARS_DEF_FILE[MAX_VARS]; /* The index of the last file to define
                                        this var.  This is used to let files
                                        listed later on the command line
                                        override files listed earlier.  We only
                                        use the last file's definition. -1 if
                                        def not seen in any file. */
char**      VARS_DEF[MAX_VARS]; /* Actual definitions */
int         VARS_DEF_I[MAX_VARS]; /* Index var used by readVarDefs */
bool_t      VERBOSE = FALSE;

/* CONSTANTS */
const char    COMMENT        = '#';
const char    CR             = ((char)13);
const char    LF             = ((char)10);
const char    MINUS          = '-';
const char    SPACE          = ' ';
const char    TAB            = ((char)9);
const char    NUL            = ((char)0);
const char    CONTINUE       = '\\';

/* GLOBAL INPUT BUFFER */
#define     BUFLEN 2048
char        BUFFER[BUFLEN];
int32_t     LINE_NO;

/* processFile return code and callback */
enum FileStat { FS_ERR, FS_VARS_SEEN, FS_NO_VARS };
typedef void (*TokenHandler)(char* token, int32_t len, int fileNo, int varNo);

/* PROTOTYPES */
int main(int argc, char *argv[]);
void substitute(int varNo);
void parseCommandLine(int argc, char *argv[]);
void usage(const char* argv0);
void countVarDefs(char* token, int32_t len, int fileNo, int varNo);
void readVarDefs(char* token, int32_t len, int fileNo, int varNo);
enum FileStat processFile(FileStream*, TokenHandler, int fileNo);
void processLine(int fileNo, int varNo, char* line, bool_t isFirstLine, TokenHandler proc);
char* substituteEnvironmentVars(char*);
char* skipWhiteSpace(char* p);
int32_t readLine(FileStream*);
void assert(int); /* use of bool_t as arg causes warnings */
void memassert(void* a);

/**
 * Emit usage and exit.
 */
void usage(const char* argv0) {
    fprintf(stderr,
            "\n"
            "Usage: mkhelper [options] infile...\n"
            " -p prefix  Prepend the given string to each line\n"
            " -s suffix  Append the given string to each line\n"
            " -old str   String to replace with -new str in text.  Comparison\n"
            "            is case-sensitive. ONLY ONE REPLACEMENT MADE PER STRING.\n"
            " -new str   String to replace -old str in text\n"
            " -n var...  One or more variable names to parse\n"
            " -v         Be verbose\n"
            " infile...  One or more input files.  Files listed earlier are\n"
            "            overridden by files listed later.\n"
            "\n"
            "Read one or more simple UNIX-style makefile fragments, parse one or\n"
            "more variable definitions that define lists of strings, and emit those\n"
            "strings to stdout, one per line, after performing optional string\n"
            "manipulations.  Definitions in later files replace those in earlier\n"
            "files.  Use $VAR$ for environment vars.  See source code for details.\n"
            , argv0);
    exit(1);
}

int main(int argc, char *argv[]) {
    int i, j;
    bool_t err=FALSE;
    parseCommandLine(argc, argv);
    for (i=0; i<VARS_COUNT; ++i) {
        VARS_DEF_FILE[i] = -1;
    }
    for (i=0; i<FILES_COUNT && !err; ++i) {
        FileStream *in;
        in = T_FileStream_open(FILES[i], "r");
        if (in == 0) {
            /* This is just a warning, since some files may be
               intentionally absent, like ucmlocal.mk. */
            if (VERBOSE) fprintf(stderr, "Warning(mkhelper): cannot open %s\n", FILES[i]);
            break;
        }
        if (VERBOSE) fprintf(stderr, "mkhelper: Reading %s\n", FILES[i]);
        switch (processFile(in, countVarDefs, i)) {
        case FS_ERR:
            err = TRUE;
            break;
        case FS_VARS_SEEN:
            /* Got some vars on pass 1; make pass 2 */
            T_FileStream_rewind(in);
            if (FS_VARS_SEEN != processFile(in, readVarDefs, i)) {
                fprintf(stderr, "Error(mkhelper): %s pass 2 failed -- should never happen!\n",
                        FILES[i]);
            }
            break;
        case FS_NO_VARS:
            break;
        }
        T_FileStream_close(in);
    }
    if (err) {
        fprintf(stderr, "Error(mkheler): file processing failed\n");
    } else {
        for (i=0; i<VARS_COUNT; ++i) {
            if (VARS_DEF_FILE[i] < 0) {
                if (VERBOSE) fprintf(stderr, "Warning(mkhelper): %s: not seen\n", VARS[i]);
            } else {
                /* Now process the variable defs and output them */
                assert(VARS_DEF_I[i] == VARS_DEF_COUNT[i]);
                if (VERBOSE) fprintf(stderr, "mkhelper: %s = %d names in %s\n", VARS[i],
                                     VARS_DEF_COUNT[i],
                                     FILES[VARS_DEF_FILE[i]]);
                if (OLD) {
                    substitute(i);
                }
                for (j=0; j<VARS_DEF_COUNT[i]; ++j) {
                    fprintf(stdout, "%s%s%s\n",
                            PREFIX!=NULL?PREFIX:"",
                            VARS_DEF[i][j],
                            SUFFIX!=NULL?SUFFIX:"");
                }
            }
        }
    }
}

/**
 * Do old->new string pattern substitution on the given variable def.
 * We just do one replacement, but it would be easy to extend this to
 * to more.  (Just feeling lazy right now.)
 */
void substitute(int varNo) {
    int i;
    int32_t oldLen = uprv_strlen(OLD);
    int32_t newLen = uprv_strlen(NEW);
    assert(oldLen > 0 && newLen > 0);
    for (i=0; i<VARS_DEF_COUNT[varNo]; ++i) {
        char* oldDef = VARS_DEF[varNo][i];
        char* match = uprv_strstr(oldDef, OLD);
        if (match != NULL) {
            char* newDef = uprv_malloc(uprv_strlen(oldDef) + 1 + newLen - oldLen);
            memassert(newDef);
            uprv_strncpy(newDef, oldDef, match - oldDef);
            newDef[match - oldDef] = NUL;
            uprv_strcat(newDef, NEW);
            uprv_strcat(newDef, match + oldLen);
            VARS_DEF[varNo][i] = newDef;

            /* Check for multiple matches and issue warning if found. */
            match = uprv_strstr(newDef, OLD);
            if (match) {
                fprintf(stderr,
                        "Warning(mkhelper): ignoring multiple matches of \"%s\" in \"%s\"\n",
                        OLD, oldDef);
            }

            uprv_free(oldDef);
        }
    }
}

/**
 * Recognize "$VAR$" as an environment variable escape (for $ itself,
 * recognize $$).  The lets us work around difficulties with the
 * Windows shell.  Return newly allocated string with substitution
 * made.
 */
char* substituteEnvironmentVars(char* str) {
    char *p, *pp;
    char *result;
    int32_t len;
    int pass;
    /* Make 2 passes.  First go through and compute the final length.
       Next, go through and construct new string. */
    for (pass=0; pass<2; ++pass) {
        p = str;
        /* Do pass-specific initialization */
        if (pass==0) {
            len = uprv_strlen(str);
        } else {
            /* len was computed in pass 1 */
            pp = result = uprv_malloc(len+1);
            memassert(result);
        }
        while (*p) {
            char* q = NULL;
            if (*p == '$') {
                q = ++p; /* p points after $ now */
                while (*q && *q != '$') { ++q; }
                if (!*q) {
                    fprintf(stderr, "Warning(mkhelper): unterminated $ in \"%s\"\n", str);
                    q = NULL; /* Indicate failure to find variable */
                } else {
                    /* q points to closing $ now */
                    int32_t varNameLen = q - p;
                    if (varNameLen == 0) {
                        /* This is a "$$", which we change to "$"*/
                        if (pass==0) {
                            /* subtract one from length */
                            --len;
                        } else {
                            *pp++ = '$';
                        }
                    } else {
                        char* env;
                        *q = NUL; /* just temporarily */
                        env = getenv(p);
                        if (env == NULL) {
                            fprintf(stderr, "Error(mkhelper): undefined environment variable \"%s\"\n",
                                    p);
                            exit(1);
                        }
                        *q = '$'; /* restore */
                        if (pass==0) {
                            len = len - 2 - varNameLen + uprv_strlen(env);
                        } else {
                            uprv_strcpy(pp, env);
                            pp += uprv_strlen(pp);
                        }
                    }
                    p = q+1; /* Move p to after closing '$' */
                }
            }
            if (q == NULL) {
                /* No variable at this point */
                if (pass==0) {
                    ++p;
                } else {
                    *pp++ = *p++;
                }
            }
        }
    }

    /* Write zero, check len */
    *pp = NUL;
    assert((pp - result) == len);
    return result;
}

/**
 * Token handler callback that counts the number of defs for each var.
 * Also records which file is the _last_ to define a variable, so later
 * files override earlier ones.
 */
void countVarDefs(char* line, int32_t len, int fileNo, int varNo) {
    if (!line) {
        VARS_DEF_COUNT[varNo] = 0;
        VARS_DEF_FILE[varNo] = fileNo;
    } else {
        ++VARS_DEF_COUNT[varNo];
    }
}

/**
 * This is a callback for processFile that read the var definitions in.
 * It assumes that a previous pass has been made with countVarDefs().
 */
void readVarDefs(char* line, int32_t len, int fileNo, int varNo) {
    char* copy;

    /* Ignore defintions not in the last defining file, as recorded in
       VARS_DEF_FILE. */
    if (fileNo != VARS_DEF_FILE[varNo]) {
        return;
    }
    if (!line) {
        /* Allocate the array of pointers */
        VARS_DEF[varNo] = uprv_malloc(sizeof(char*) * VARS_DEF_COUNT[varNo]);
        memassert(VARS_DEF[varNo]);
        /* Initialize index */
        VARS_DEF_I[varNo] = 0;
    } else {
        /* Sanity check */
        assert(VARS_DEF_I[varNo] < VARS_DEF_COUNT[varNo]);
        /* Allocate buffer */
        copy = VARS_DEF[varNo][VARS_DEF_I[varNo]] = uprv_malloc(len + 1);
        memassert(copy);
        /* Copy */
        uprv_strncpy(copy, line, len);
        copy[len] = NUL;
        ++VARS_DEF_I[varNo];
    }
}

/**
 * Handle a line of a variable def.  Call the token handler once with NULL (for
 * each varNo) then once with each token.
 */
void processLine(int fileNo, int varNo, char* line, bool_t isFirstLine, TokenHandler proc) {
    if (isFirstLine) {
        /* Call once with NULL for initialization */
        (*proc)(NULL, 0, fileNo, varNo);
    }        
    for (;;) {
        int32_t len = 0;
        char* p;
        line = skipWhiteSpace(line);
        if (!*line || (*line == CONTINUE && line[1] == NUL)) {
            break;
        }
        /* Find the end */
        p = line;
        while (*p && *p != SPACE && *p != TAB &&
               !(*p == CONTINUE && p[1] == NUL)) { ++p; }
        len = p - line;
        assert(len > 0);
        (*proc)(line, len, fileNo, varNo);
        line = p;
    }
}

/**
 * Make a pass through a file, looking for variable def lines that
 * match our desired var, and handing them off to processLine.
 */
enum FileStat processFile(FileStream* in, TokenHandler proc, int fileNo) {
    int32_t len;
    char* p;
    int j;
    bool_t varsSeen = FALSE;
    LINE_NO = 0;
    while ((len = readLine(in)) >= 0) {
        if (len) {
            bool_t varFound = FALSE;
            /* Skip white space */
            p = skipWhiteSpace(BUFFER);
            /* Is the next word a var? */
            for (j=0; j<VARS_COUNT && !varFound; ++j) {
                if (0 == uprv_strncmp(VARS[j], p, uprv_strlen(VARS[j]))) {
                    /* Yes, found a var, maybe...parse more to see */
                    char* savep = p;
                    bool_t isFirstLine = TRUE;

                    p += uprv_strlen(VARS[j]); /* Go past var name */

                    /* Now look for /\s*=/ */
                    if (*p != SPACE && *p != TAB && *p != '=') {
                        p = savep;
                        continue; /* Didn't see it after all */
                    }
                    p = skipWhiteSpace(p);
                    if (*p != '=') {
                        p = savep;
                        continue; /* Didn't see it after all */
                    }
                    ++p;

                    /* Now it's definite */
                    varsSeen = varFound = TRUE;
                    p = skipWhiteSpace(p);
                    /* Now read file names until we get to the end of this
                       line, including line continuation characters. */
                    for (;;) {
                        processLine(fileNo, j, p, isFirstLine, proc);
                        isFirstLine = FALSE;
                        if (*(p + uprv_strlen(p) - 1) == CONTINUE) {
                            if ((len = readLine(in)) < 0) {
                                fprintf(stderr, "Error(mkhelper): unexpected eof after continuation char\n");
                                return FS_ERR; /*fail*/
                            }
                            p = skipWhiteSpace(BUFFER);
                        } else {
                            /* Last line processed -- look for more variables */
                            break;
                        }
                    }
                }
            }
            if (!varFound) {
                fprintf(stderr, "Warning(mkhelper): ignoring %s line %ld: %s\n",
                        FILES[fileNo], LINE_NO, BUFFER);
            }
        }
    }
    return varsSeen ? FS_VARS_SEEN : FS_NO_VARS;
}

char* skipWhiteSpace(char* p) {
    while (*p == SPACE || *p == TAB) { ++p; }
    return p;
}

void parseCommandLine(int argc, char *argv[]) {
    int i;
    for (i=1; i<argc; ++i) {
        const char* arg = argv[i];
        if (arg[0] == '-') {
            /* Handle options */
            switch (arg[1]) {
            case 'p':
            case 's':
            case 'o':
            case 'n':
                if ((i+1) >= argc) {
                    fprintf(stderr, "%s must be followed by something\n", arg);
                    usage(argv[0]);
                }
                break;
            }

            switch (arg[1]) {
            case 'p':
                if (PREFIX) {
                    fprintf(stderr, "Multiple -p options not allowed!\n", arg);
                    usage(argv[0]);
                }
                PREFIX = substituteEnvironmentVars(argv[++i]);
                break;
            case 's':
                if (SUFFIX) {
                    fprintf(stderr, "Multiple -s options not allowed!\n", arg);
                    usage(argv[0]);
                }
                SUFFIX = substituteEnvironmentVars(argv[++i]);
                break;
            case 'v':
                VERBOSE = TRUE;
                break;
            case 'o':
                if (OLD) {
                    fprintf(stderr, "Multiple -old options not allowed!\n",
                            arg);
                    usage(argv[0]);
                }
                OLD = substituteEnvironmentVars(argv[++i]);
                break;
            case 'n':
                if (arg[2] == 'e') {
                    if (NEW) {
                        fprintf(stderr, "Multiple -new options not allowed!\n",
                                arg);
                        usage(argv[0]);
                    }
                    NEW = substituteEnvironmentVars(argv[++i]);
                } else {
                    if (VARS_COUNT == MAX_VARS) {
                        fprintf(stderr, "Too many -n options -- fix tool and recompile!\n",
                                arg);
                        usage(argv[0]);
                    }
                    VARS[VARS_COUNT++] = argv[++i];
                }
                break;
            default:
                fprintf(stderr, "Bad option %s\n", arg);
                usage(argv[0]);
                break;
            }
        } else {
            if (FILES_COUNT == MAX_FILES) {
                fprintf(stderr, "Too many input files -- fix tool and recompile!\n",
                        arg);
                usage(argv[0]);
            }
            FILES[FILES_COUNT++] = arg;
        }
    }

    /* Make sure at least one input file and one variable exist */
    if (FILES_COUNT < 1 || VARS_COUNT < 1) {
        fprintf(stderr, "Please specify at least one variable and one input file\n");
        usage(argv[0]);
    }

    /* Need both old & new or neither */
    if ((OLD != NULL) != (NEW != NULL)) {
        fprintf(stderr, "Specify both -old and -new, or neither\n");
        usage(argv[0]);
    }

    if (VERBOSE) {
        if (OLD) {
            fprintf(stderr, "mkhelper: Substituting \"%s\" -> \"%s\"\n", OLD, NEW);
        }
        if (PREFIX) {
            fprintf(stderr, "mkhelper: Prefix \"%s\"\n", PREFIX);
        }
        if (SUFFIX) {
            fprintf(stderr, "mkhelper: Suffix \"%s\"\n", SUFFIX);
        }
    }
}

/**
 * Read one line into BUFFER, trim any comment, remove trailing white space
 * and line separators, bump the LINE_NO, return length.  Return negative
 * value when EOF reached.
 */
int32_t readLine(FileStream* in) {
    char* p;
    ++LINE_NO;
    if (T_FileStream_readLine(in, BUFFER, BUFLEN) == NULL) {
        return -1; /* EOF */
    }
    /* Trim off trailing comment */
    p = uprv_strchr(BUFFER, COMMENT);
    if (p != 0) {
        /* Back up past any space or tab characters before
         * the comment character. */
        while (p > BUFFER && (p[-1] == SPACE || p[-1] == TAB)) {
            p--;
        }
        *p = NUL;
    }
    /* Delete any trailing ^J and/or ^M characters */
    p = BUFFER + uprv_strlen(BUFFER);
    while (p > BUFFER && (p[-1] == CR || p[-1] == LF)) {
        p--;
    }
    *p = NUL;
    return uprv_strlen(BUFFER);
}

void assert(int a) {
    if (!a) {
        fprintf(stderr, "Error(mkhelper): assertion failure\n");
        exit(1);
    }

}

void memassert(void* a) {
    if (!a) {
        fprintf(stderr, "Error(mkhelper): out of memory\n");
        exit(1);
    }
}
