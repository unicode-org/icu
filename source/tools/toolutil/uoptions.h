/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uoptions.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000apr17
*   created by: Markus W. Scherer
*
*   This file provides a command line argument parser.
*/

#ifndef __UOPTIONS_H__
#define __UOPTIONS_H__

#include "unicode/utypes.h"

/* forward declarations for the function declaration */
struct UOption;
typedef struct UOption UOption;

/* function to be called for a command line option */
typedef int UOptionFn(void *context, UOption *option);

/* values of UOption.hasArg */
enum { UOPT_NO_ARG, UOPT_REQUIRES_ARG, UOPT_OPTIONAL_ARG };

/* structure describing a command line option */
struct UOption {
    const char *longName;   /* "foo" for --foo */
    const char *value;      /* output placeholder, will point to the argument string, if any */
    UOptionFn *optionFn;    /* function to be called when this option occurs */
    void *context;          /* parameter for the function */
    char shortName;         /* 'f' for -f */
    char hasArg;            /* enum value: option takes no/requires/may have argument */
    char doesOccur;         /* boolean for "this one occured" */
};

/* macro for an entry in a declaration of UOption[] */
#define UOPTION_DEF(longName, shortName, hasArg) \
    { longName, NULL, NULL, NULL, shortName, hasArg, 0 }

/* ICU Tools option definitions */
#define UOPTION_HELP_H              UOPTION_DEF("help", 'h', UOPT_NO_ARG)
#define UOPTION_HELP_QUESTION_MARK  UOPTION_DEF("help", '?', UOPT_NO_ARG)
#define UOPTION_VERBOSE             UOPTION_DEF("verbose", 'v', UOPT_NO_ARG)
#define UOPTION_QUIET               UOPTION_DEF("quiet", 'q', UOPT_NO_ARG)
#define UOPTION_VERSION             UOPTION_DEF("version", 'V', UOPT_NO_ARG)
#define UOPTION_COPYRIGHT           UOPTION_DEF("copyright", 'c', UOPT_NO_ARG)

#define UOPTION_DESTDIR             UOPTION_DEF("destdir", 'd', UOPT_REQUIRES_ARG)
#define UOPTION_SOURCEDIR           UOPTION_DEF("sourcedir", 's', UOPT_REQUIRES_ARG)
#define UOPTION_ENCODING            UOPTION_DEF("encoding", 'e', UOPT_REQUIRES_ARG)

/**
 * C Command line argument parser.
 *
 * This function takes the argv[argc] command line and a description of
 * the program's options in form of an array of UOption structures.
 * Each UOption defines a long and a short name (a string and a character)
 * for options like "--foo" and "-f".
 *
 * Each option is marked with whether it does not take an argument,
 * requires one, or optionally takes one. The argument may follow in
 * the same argv[] entry for short options, or it may always follow
 * in the next argv[] entry.
 *
 * An argument is in the next argv[] entry for both long and short name
 * options, except it is taken from directly behind the short name in
 * its own argv[] entry if there are characters following the option letter.
 * An argument in its own argv[] entry must not begin with a '-'
 * unless it is only the '-' itself. There is no restriction of the
 * argument format if it is part of the short name options's argv[] entry.
 *
 * The argument is stored in the value field of the corresponding
 * UOption entry, and the doesOccur field is set to 1 if the option
 * is found at all.
 *
 * Short name options without arguments can be collapsed into a single
 * argv[] entry. After an option letter takes an argument, following
 * letters will be taken as its argument.
 *
 * If the same option is found several times, then the last
 * argument value will be stored in the value field.
 *
 * For each option, a function can be called. This could be used
 * for options that occur multiple times and all arguments are to
 * be collected.
 *
 * All options are removed from the argv[] array itself. If the parser
 * is successful, then it returns the number of remaining non-option
 * strings (including argv[0]).
 * argv[0], the program name, is never read or modified.
 *
 * An option "--" ends option processing; everything after this
 * remains in the argv[] array.
 *
 * An option string "-" alone is treated as a non-option.
 *
 * If an option is not recognized or an argument missing, then
 * the parser returns with the negative index of the argv[] entry
 * where the error was detected.
 */
int
u_parseArgs(int argc, const char *argv[],
            int optionCount, UOption options[]);

#endif
