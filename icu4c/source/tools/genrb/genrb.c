/*
*******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File genrb.c
*
* Modification History:
*
*   Date        Name        Description
*   05/25/99    stephen     Creation.
*   5/10/01     Ram         removed ustdio dependency
*******************************************************************************
*/

#include "genrb.h"

/* Protos */
static void  processFile(const char *filename, const char* cp, const char *inputDir, const char *outputDir, const char *packageName, UErrorCode *status);
static char *make_res_filename(const char *filename, const char *outputDir, 
							   const char *packageName, UErrorCode *status);

/* File suffixes */
#define RES_SUFFIX ".res"
#define COL_SUFFIX ".col"

static char theCurrentFileName[4096];
const char *gCurrentFileName = theCurrentFileName;
#ifdef XP_MAC_CONSOLE
#include <console.h>
#endif

enum
{
    HELP1,
    HELP2,
    VERBOSE,
    QUIET,
    VERSION,
    SOURCEDIR,
    DESTDIR,
    ENCODING,
    ICUDATADIR,
    WRITE_JAVA,
    COPYRIGHT,
    PACKAGE_NAME,
    BUNDLE_NAME,
	TOUCHFILE
};

UOption options[]={
                      UOPTION_HELP_H,
                      UOPTION_HELP_QUESTION_MARK,
                      UOPTION_VERBOSE,
                      UOPTION_QUIET,
                      UOPTION_VERSION,
                      UOPTION_SOURCEDIR,
                      UOPTION_DESTDIR,
                      UOPTION_ENCODING,
                      UOPTION_ICUDATADIR,
                      UOPTION_WRITE_JAVA,
                      UOPTION_COPYRIGHT,
                      UOPTION_PACKAGE_NAME,
                      UOPTION_BUNDLE_NAME,
                      UOPTION_DEF( "touchfile", 't', UOPT_NO_ARG) /* 12 */
                  };

static     UBool       verbose = FALSE;
static     UBool       write_java = FALSE;
static	   UBool	   touchfile = FALSE;
static     const char* outputEnc ="";
static     const char* packageName=NULL;
static     const char* bundleName=NULL;

int
main(int argc,
     char* argv[])
{
    UErrorCode  status    = U_ZERO_ERROR;
    const char *arg       = NULL;
    const char *outputDir = NULL; /* NULL = no output directory, use current */
    const char *inputDir  = NULL;
    const char *encoding  = "";
    int         i;

    U_MAIN_INIT_ARGS(argc, argv);

    argc = u_parseArgs(argc, argv, (int32_t)(sizeof(options)/sizeof(options[0])), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr, "%s: error in command line argument \"%s\"\n", argv[0], argv[-argc]);
    } else if(argc<2) {
        argc = -1;
    }

    if(options[VERSION].doesOccur) {
        fprintf(stderr,
                "%s version %s (ICU version %s).\n"
                "%s\n",
                argv[0], GENRB_VERSION, U_ICU_VERSION, U_COPYRIGHT_STRING);
        return U_ZERO_ERROR;
    }

    if(argc<0 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
        /*
         * Broken into chucks because the C89 standard says the minimum
         * required supported string length is 509 bytes.
         */
        fprintf(stderr,
                "Usage: %s [OPTIONS] [FILES]\n"
                "\tReads the list of resource bundle source files and creates\n"
                "\tbinary version of reosurce bundles (.res files)\n",
                argv[0]);
        fprintf(stderr,
                "Options:\n"
                "\t-h or -? or --help           this usage text\n"
                "\t-q or --quiet                do not display warnings\n"
                "\t-v or --verbose              prints out extra information about processing the files\n"
                "\t-V or --version              prints out version number and exits\n"
                "\t-c or --copyright            include copyright notice\n");
        fprintf(stderr,
                "\t-e or --encoding             encoding of source files, leave empty for system default encoding\n"
                "\t                             NOTE: ICU must be completely built to use this option\n"
                "\t-d of --destdir              destination directory, followed by the path, defaults to %s\n"
                "\t-s or --sourcedir            source directory for files followed by path, defaults to %s\n"
                "\t-i or --icudatadir           directory for locating any needed intermediate data files,\n"
                "\t                             followed by path, defaults to %s\n",
                u_getDataDirectory(), u_getDataDirectory(), u_getDataDirectory());
        fprintf(stderr,
                "\t-j or --write-java           write a Java ListResourceBundle for ICU4J, followed by optional encoding\n"
                "\t                             defaults to ASCII and \\uXXXX format.\n"
                "\t-p or --package-name         For ICU4J: package name for writing the ListResourceBundle for ICU4J, defaults to\n"
                "\t                             com.ibm.icu.impl.data\n"
                "\t                             For ICU4C: Package name on output.  Using 'ICUDATA' defaults to the current ICU4C data name.\n"
                "\t-b or --bundle-name          bundle name for writing the ListResourceBundle for ICU4J, defaults to\n"
                "\t                             LocaleElements");

        return argc < 0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    if(options[VERBOSE].doesOccur) {
        verbose = TRUE;
    }

    if(options[QUIET].doesOccur) {
        setShowWarning(FALSE);
    }
    
    if(options[COPYRIGHT].doesOccur){
        setIncludeCopyright(TRUE);
    }

    if(options[SOURCEDIR].doesOccur) {
        inputDir = options[SOURCEDIR].value;
    }

    if(options[DESTDIR].doesOccur) {
        outputDir = options[DESTDIR].value;
    }
    if(options[PACKAGE_NAME].doesOccur) {
        packageName = options[PACKAGE_NAME].value;
        if(!strcmp(packageName, "ICUDATA"))
        {
            packageName = U_ICUDATA_NAME;
        }
        if(packageName[0] == 0)
        {
            packageName = NULL;
        }
    }

	if(options[TOUCHFILE].doesOccur) {
		if(packageName == NULL) {
			fprintf(stderr, "%s: Don't use touchfile (-t) option with no package.\n", 
					argv[0]);
			return -1;
		}
		touchfile = TRUE;
	}

    if(options[ENCODING].doesOccur) {
        encoding = options[ENCODING].value;
    }

    if(options[ICUDATADIR].doesOccur) {
        u_setDataDirectory(options[ICUDATADIR].value);
    }
    if(options[WRITE_JAVA].doesOccur) {
        write_java = TRUE;
        outputEnc = options[WRITE_JAVA].value;
    }

    if(options[BUNDLE_NAME].doesOccur) {
        bundleName = options[BUNDLE_NAME].value;
    }


    initParser();

    /* generate the binary files */
    for(i = 1; i < argc; ++i) {
        status = U_ZERO_ERROR;
        arg    = getLongPathname(argv[i]);

        if (inputDir) {
            uprv_strcpy(theCurrentFileName, inputDir);
            uprv_strcat(theCurrentFileName, U_FILE_SEP_STRING);
        } else {
            *theCurrentFileName = 0;
        }
        uprv_strcat(theCurrentFileName, arg);

        if (verbose) {
            printf("processing file \"%s\"\n", theCurrentFileName);
        }
        processFile(arg, encoding, inputDir, outputDir, packageName, &status);
    }

    return status;
}

/* Process a file */
static void
processFile(const char *filename, const char *cp, const char *inputDir, const char *outputDir, const char *packageName, UErrorCode *status) {
    FileStream     *in           = NULL;
    struct SRBRoot *data         = NULL;
    UCHARBUF       *ucbuf        = NULL;
    char           *rbname       = NULL;
    char           *openFileName = NULL;
    char           *inputDirBuf  = NULL;

    char           outputFileName[256];

    if (U_FAILURE(*status)) {
        return;
    }

    /* Setup */
    in = 0;

    /* Open the input file for reading */
    if (!uprv_strcmp(filename, "-")) {
        in = T_FileStream_stdin();
    } else if(inputDir == NULL) {
        const char *filenameBegin = uprv_strrchr(filename, U_FILE_SEP_CHAR);
        if (filenameBegin != NULL) {
            /*
             * When a filename ../../../data/root.txt is specified, 
             * we presume that the input directory is ../../../data
             * This is very important when the resource file includes
             * another file, like UCARules.txt or thaidict.brk.
             */
            int32_t filenameSize = filenameBegin - filename + 1;
            inputDirBuf = uprv_strncpy((char *)uprv_malloc(filenameSize), filename, filenameSize);

            /* test for NULL */
            if(inputDirBuf == NULL) {
                *status = U_MEMORY_ALLOCATION_ERROR;
                goto finish;
            }

	    inputDirBuf[filenameSize - 1] = 0;
            inputDir = inputDirBuf;
        }
        in = T_FileStream_open(filename, "rb");
    } else {
        int32_t dirlen  = (int32_t)uprv_strlen(inputDir);
        int32_t filelen = (int32_t)uprv_strlen(filename);
        if(inputDir[dirlen-1] != U_FILE_SEP_CHAR) {
            openFileName = (char *) uprv_malloc(dirlen + filelen + 2);

	    /* test for NULL */
	    if(openFileName == NULL) {
                *status = U_MEMORY_ALLOCATION_ERROR;
                goto finish;
	    }

            openFileName[0] = '\0';
            /*
             * append the input dir to openFileName if the first char in 
             * filename is not file seperation char and the last char input directory is  not '.'.
             * This is to support :
             * genrb -s. /home/icu/data
             * genrb -s. icu/data
             * The user cannot mix notations like
             * genrb -s. /icu/data --- the absolute path specified. -s redundant
             * user should use
             * genrb -s. icu/data  --- start from CWD and look in icu/data dir
             */
            if( (filename[0] != U_FILE_SEP_CHAR) && (inputDir[dirlen-1] !='.')){
                uprv_strcpy(openFileName, inputDir);
                openFileName[dirlen]     = U_FILE_SEP_CHAR;
            }
            openFileName[dirlen + 1] = '\0';
            uprv_strcat(openFileName, filename);
        } else {
            openFileName = (char *) uprv_malloc(dirlen + filelen + 1);

            /* test for NULL */
            if(openFileName == NULL) {
                *status = U_MEMORY_ALLOCATION_ERROR;
                goto finish;
            }

            uprv_strcpy(openFileName, inputDir);
            uprv_strcat(openFileName, filename);
        }
        in = T_FileStream_open(openFileName, "rb");
    }

    if(in == 0) {
        *status = U_FILE_ACCESS_ERROR;
        fprintf(stderr, "couldn't open file %s\n", openFileName == NULL ? filename : openFileName);
        goto finish;
    } else {
        /* auto detect popular encodings */
        if (*cp=='\0' && ucbuf_autodetect(in, &cp) && verbose) {
            printf("autodetected encoding %s\n", cp);
        }
    }

    ucbuf = ucbuf_open(in, cp, 0, status);

    if (ucbuf == NULL || U_FAILURE(*status)) {
        goto finish;
    }

    /* Parse the data into an SRBRoot */
    data = parse(ucbuf, inputDir, status);

    if (data == NULL || U_FAILURE(*status)) {
        goto finish;
    }

    /* Determine the target rb filename */
    rbname = make_res_filename(filename, outputDir, packageName, status);
	if(touchfile == TRUE) {
	    FileStream *q;
		char msg[1024];
		char *tfname = NULL;

		tfname = make_res_filename(filename, outputDir, NULL, status);
			  
		if(U_FAILURE(*status))
		{
			fprintf(stderr, "Error writing touchfile for \"%s\"\n", filename);
			*status = U_FILE_ACCESS_ERROR;
		} else {
			uprv_strcat(tfname, ".res");
			sprintf(msg, "This empty file tells nmake that %s in package %s has been updated.\n",
				filename, packageName);
	
			q = T_FileStream_open(tfname, "w");
			if(q == NULL)
			{		
				fprintf(stderr, "Error writing touchfile \"%s\"\n", tfname);
				*status = U_FILE_ACCESS_ERROR;
			}
			else
			{
				  T_FileStream_write(q, msg, uprv_strlen(msg));
				  T_FileStream_close(q);
			}
			uprv_free(tfname);
		}
	
	}
    if(U_FAILURE(*status)) {
        goto finish;
    }
    if(write_java== FALSE){
        /* Write the data to the file */
        bundle_write(data, outputDir, packageName, outputFileName, sizeof(outputFileName), status);
    }else{
        bundle_write_java(data,outputDir,outputEnc, outputFileName, sizeof(outputFileName),packageName,bundleName,status);
    }
    if (U_FAILURE(*status)) {
        fprintf(stderr, "couldn't write bundle %s. Error:%s\n", outputFileName,u_errorName(*status));
    }
    bundle_close(data, status);

finish:

    if (inputDirBuf != NULL) {
        uprv_free(inputDirBuf);
    }

    if (openFileName != NULL) {
        uprv_free(openFileName);
    }

    if(ucbuf) {
        ucbuf_close(ucbuf);
    }

    /* Clean up */
    if (in != T_FileStream_stdin()) {
        T_FileStream_close(in);
    }

    if (rbname) {
        uprv_free(rbname);
    }
}

/* Generate the target .res file name from the input file name */
static char*
make_res_filename(const char *filename,
                  const char *outputDir,
                  const char *packageName,
                  UErrorCode *status) {
    char *basename;
    char *dirname;
    char *resName;

    int32_t pkgLen = 0; /* length of package prefix */

    if (U_FAILURE(*status)) {
        return 0;
    }

    if(packageName != NULL)
    {
        pkgLen = 1 + uprv_strlen(packageName);
    }

    /* setup */
    basename = dirname = resName = 0;

    /* determine basename, and compiled file names */
    basename = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(filename) + 1));
    if(basename == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        goto finish;
    }

    get_basename(basename, filename);

    dirname = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(filename) + 1));
    if(dirname == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        goto finish;
    }

    get_dirname(dirname, filename);

    if (outputDir == NULL) {
        /* output in same dir as .txt */
        resName = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(dirname)
                                      + pkgLen
                                      + uprv_strlen(basename)
                                      + uprv_strlen(RES_SUFFIX) + 8));
        if(resName == 0) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            goto finish;
        }

        uprv_strcpy(resName, dirname);

        if(packageName != NULL)
        {
            uprv_strcat(resName, packageName);
            uprv_strcat(resName, "_");
        }

        uprv_strcat(resName, basename);

    } else {
        int32_t dirlen      = (int32_t)uprv_strlen(outputDir);
        int32_t basenamelen = (int32_t)uprv_strlen(basename);

        resName = (char*) uprv_malloc(sizeof(char) * (dirlen + pkgLen + basenamelen + 8));

        if (resName == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            goto finish;
        }

        uprv_strcpy(resName, outputDir);

        if(outputDir[dirlen] != U_FILE_SEP_CHAR) {
            resName[dirlen]     = U_FILE_SEP_CHAR;
            resName[dirlen + 1] = '\0';
        }

        if(packageName != NULL)
        {
            uprv_strcat(resName, packageName);
            uprv_strcat(resName, "_");
        }

        uprv_strcat(resName, basename);
    }

finish:
    uprv_free(basename);
    uprv_free(dirname);

    return resName;
}

/*
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 */
