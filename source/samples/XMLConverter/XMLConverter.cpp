/*
**********************************************************************
* Copyright (C) 1998-2000, International Business Machines Corporation 
* and others.  All Rights Reserved.
**********************************************************************
*
*/
// XMLConverter.cpp
// To convert one encoded XML file to another

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

/* Define _XPG4_2 for Solaris and friends. */
#ifndef _XPG4_2
#define _XPG4_2
#endif

/* Define __USE_XOPEN_EXTENDED for Linux and glibc. */
#ifndef __USE_XOPEN_EXTENDED
#define __USE_XOPEN_EXTENDED
#endif

#include <string.h>

#ifdef _WIN32
#  include <windows.h>
#endif

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"
#include "unicode/uloc.h"
#include "unicode/uchar.h"

#define MAXFILENAMELEN  1024
#define RAWBUFSIZE       4096
#define ENCODINGCOUNT   5
#define FIRSTLINEBUF    256
typedef unsigned char  BYTE;


char firstLine[128];
char encodingNameInFile[256];
bool verbose = false;

extern void convertFile(char*, char*, char*, UConverter*);
extern void usage();
extern void printChars(unsigned char*, int);
extern int getInputEncodingType(const BYTE* rawBuffer, 
                                unsigned long byteCount);
extern long convertFirstLine(FILE* inF, 
                             char* inEncName, 
                             FILE* outF, 
                             char* outEncName, 
                             char* ptrBuf, 
                             unsigned long toRead, 
                             UChar* uBuf);
extern void catString(char* thisString, bool quote);
extern int32_t  XMLUConvert( UConverter* inConverter,
                      UConverter* outConverter,
                      const char* inBuffer, 
                      int32_t* inBufSize, 
                      char* outBuffer, 
                      int32_t outBufCapacity, 
                      UBool flush,
                      UErrorCode* err);
extern void XMLU_fromCodepageToCodepage(    UConverter*    outConverter,
                        UConverter*    inConverter,
                        char**         target,
                        const char*        targetLimit,
                        const char**        source,
                        const char*        sourceLimit,
                        int32_t*        offsets,
                        UBool            flush,
                        UErrorCode*        err);

static const BYTE    gEBCDICPre[]    = { 0x4C, 0x6F, 0xA7, 0x94 };
#if 0
//not supported encodings
static const BYTE    gUCS4BPre[]     = { 0x00, 0x00, 0x00, 0x3C };
static const BYTE    gUCS4LPre[]     = { 0x3C, 0x00, 0x00, 0x00 };
#endif 
static const BYTE    gUTF16BPre[]    = { 0x00, 0x3C, 0x00, 0x3F };
static const BYTE    gUTF16LPre[]    = { 0x3C, 0x00, 0x3F, 0x00 };
static const char    gXMLDecl_ASCII[]= { 0x3C, 0x3F, 0x78, 0x6D, 0x6C };

enum Encodings
    {
        EBCDIC          = 0,
        UCS_4B          = 1,
        UCS_4L          = 2,
        US_ASCII        = 3,
        UTF_8           = 4,
        UTF_16B         = 5,
        UTF_16L         = 6,

        Encodings_Count = ENCODINGCOUNT,
        Encodings_Min   = EBCDIC,
        Encodings_Max   = UTF_16L,

        OtherEncoding   = 999
    };


void usage(char *  exeName)
{
    fprintf(stdout, "\n USAGE: \n \t%s [-h] [-v] -e trgEncName inputFile outputFile \n\n", exeName);
    fprintf(stdout, " %s    = Exe name \n ", exeName);
	fprintf(stdout, "-h     \t= to get help (this information!) \n ");
    fprintf(stdout, "-v     \t= set verbose on; \n \t\t  to get more information about the conversion process \n ");
    fprintf(stdout, "-e     \t= This is a mandatory option and follows with the targetEncName");
    fprintf(stdout, "       \t\t  E.g., output encoding can be like : \n \t\t  ascii, utf8, utf-16be, utf-16le, ebcdic-cp-us \n");
    fprintf(stdout, "trgEncName  \t= The output encoding type needed. \n \t\t  It always should follow the -e switch\n");
    fprintf(stdout, "inputFile     \t= The input XML file name \n");
    fprintf(stdout, "outputFile    \t= The output XML file name \n");
    fprintf(stdout, " \n For example: \n ");
    fprintf(stdout, " \t %s -e utf8 pr-utf-16.xml pr-utf-8.xml \n\n\n ", exeName);
}



int main(int argc, char** argv)
{
    UErrorCode err = U_ZERO_ERROR;
    char* inFileName; 
    char* outFileName;
    char * encName = NULL;

	UConverter*  conv = NULL;

    for (int i=0; i< argc; i++)
    {
        if (!strcmp( argv[i], "-h") || (argc < 5) )
        {
            usage(argv[0]);
            exit(1);
        }
        if (!strcmp( argv[i], "-v"))
            verbose = true;
        if (!strcmp( argv[i], "-e"))
        {
            if ( argc == i+4)
            {
                encName = new char[strlen(argv[i+1]) +1];
                strcpy(encName, argv[i+1]);
                inFileName = new char[strlen(argv[i+2]) +1];
                strcpy(inFileName, argv[i+2]);
                outFileName = new char[strlen(argv[i+3]) +1];
                strcpy(outFileName, argv[i+3]);
                break;
            }
            else
            {
                usage(argv[0]);
                exit(1);
            }
        }
    }

	conv = ucnv_open(encName, &err);
	if (U_FAILURE(err))
	{
        if (verbose)
        {
            fprintf(stderr, "Could not create converter to: %s\n", encName);
#if defined(_DEBUG) && defined(XP_CPLUSPLUS)
	    	fprintf (stderr,"FAILURE! (%s) (%d)\n", u_errorName(err), err);
#endif
        }
        ucnv_close(conv);
        exit(1);
    }
	
    fprintf(stdout, "Converting %s to %s...\n", inFileName, outFileName);
	convertFile(encName, inFileName, outFileName, conv);
	fprintf(stdout, "Finished transcoding file: %s\n", inFileName);
    
    ucnv_close(conv);
    if (encName)
        delete encName;
    return 0;
}

void convertFile(char* encName, char* iFN, char* oFN, UConverter* outConvrtr)
{
    //Read the input file
    //
    FILE* inFile = fopen( iFN, "rb");
    if (inFile == NULL) {
        if (verbose)
            fprintf(stderr, "Could not open input file - %s for reading \n", iFN);
        exit(1);
    }

    FILE*   outFile = fopen(oFN, "wb");
	if (outFile == NULL)
	{
        if (verbose)
		    fprintf(stderr, "Could not open output file - %s for writing \n", oFN);
		fclose(inFile);
		return;
	}

    char            rawBuf[RAWBUFSIZE];
    char*           pRawBuf     = NULL;
    unsigned long   bytesRead   = 0;
	UErrorCode       err         = U_ZERO_ERROR;
	
    //get the file size
    //
    unsigned int    curPos      = ftell(inFile);

    if(verbose)
      fprintf(stderr, "curPos = %d\n", curPos);

    if (curPos == 0xFFFFFFFF)
    {
        fprintf(stderr, "fileSize - Could not save current pos \n");
        exit(1);
    }

    // Seek to the end and save that value for return
    //
    if ( fseek(inFile, 0 , SEEK_END) )
    {
        fprintf(stderr, "fileSize - Could not seek to end \n");
        exit(1);
    }

    const unsigned int endPos = ftell(inFile);
    if (endPos == 0xFFFFFFFF)
    {
        fprintf(stderr, "fileSize - Could not get the end pos \n");
        exit(1);
    }

    // And put the pointer back
    //
    if (fseek(inFile, curPos, SEEK_SET))
    {
        fprintf(stderr, "fileSize - Could not seek back to original pos \n");
        exit(1);
    }

    if (curPos >= endPos) 
    {
        fprintf(stderr,"Reached end of input file while reading \n");
        exit(1);
    } 
    
    unsigned int    bytesLeft   = endPos - curPos; 
    if (verbose)
        fprintf(stdout,"Input file size is %d \n", bytesLeft);
    
    unsigned int toRead = (RAWBUFSIZE > bytesLeft) ? bytesLeft : RAWBUFSIZE;
    
    //Read the infile    
    //
    bytesRead = fread( (void*)rawBuf, 1, toRead, inFile);
    if (ferror(inFile)) 
    {
        fprintf(stderr," couldnot read file for input encoding \n");
        exit(1);
    }
    
    if (bytesRead ==  0) 
    {
        fprintf(stderr," couldnot fill raw buffer \n");
        exit(1);
    }
    pRawBuf = rawBuf;

    // get the input encoding type
    int inputEnc = getInputEncodingType((const BYTE*)rawBuf, bytesRead); 
    if (inputEnc == OtherEncoding)
    {
        fprintf(stderr, " Unknown encoded input file. \n Only input encodings supported in the first line are \n");
        fprintf(stderr, " ascii, ebcdic-cp-us, utf8, utf-16be, utf-16le \n");
        exit(1);
    }
    
    //transcoding the first line from inEncodName to ascii and then replacing
    //the encoding=inEncodingName to encoding=outEncodingName
    //
    
    UChar          ucBuf[RAWBUFSIZE];
    char * inEncodName;
    char* tmpPtr = (char*) rawBuf;
    
    //get the input encoding name
    //
    switch (inputEnc)
    {
    case 0 : 
        inEncodName = new char[strlen("ebcdic-cp-us") +1];
        strcpy(inEncodName, "ebcdic-cp-us");
        break;
    case 3 :
        inEncodName = new char[strlen("ascii") +1];
        strcpy(inEncodName, "ascii");
        break;
    case 4 :
        inEncodName = new char[strlen("utf8") +1];
        strcpy(inEncodName, "utf8");
        break;
    case 5 :
        inEncodName = new char[strlen("utf-16be") +1];
        strcpy(inEncodName, "utf-16be");
        break;
    case 6 :
        inEncodName = new char[strlen("utf-16le") +1];
        strcpy(inEncodName, "utf-16le");
        break;
    default :
        break;
    };

    if(verbose)
      {
	fprintf(stderr, "inConverter = %s\n", inEncodName);
      }
    
    UConverter* inConvrtr = ucnv_open(inEncodName, &err);
    //now read and transcode the input to output file
    //Process the firstline separately
    //
    long afterFirstLine = convertFirstLine(inFile, inEncodName, outFile, encName, 
					   pRawBuf, toRead, (UChar*)ucBuf);
    
    //move the pointer after the first line
    //
    if (fseek(inFile, (unsigned long) afterFirstLine, SEEK_SET))
    {
        fprintf(stderr, "fileSize - Could not set the cursor to %d after the first line \n", afterFirstLine);
        exit(1);
    }
    else
      if(verbose)
	fprintf(stderr,"Seeked to %d OK \n", afterFirstLine);
    bytesLeft = endPos - afterFirstLine;
    toRead = (RAWBUFSIZE > bytesLeft) ? bytesLeft : RAWBUFSIZE;
    
    //  read the rest of the input file
    //
    if (verbose)
        fprintf(stdout,"The first line consists of %d bytes \n", afterFirstLine);
    if (encodingNameInFile !=NULL) 
    {
        if (inEncodName)
            delete inEncodName;
        inEncodName = new char[strlen(encodingNameInFile)+1];
        strcpy(inEncodName, encodingNameInFile);
        ucnv_close(inConvrtr);
        inConvrtr = ucnv_open(inEncodName, &err);
    }
    if (verbose)
        fprintf(stdout, "Input Encoding type = %s,  Output Encoding type = %s \n", inEncodName, encName);
    
    char *outBuf = new char[RAWBUFSIZE];
    int  outBufSize = RAWBUFSIZE;
    bool tFlush = false;
    err = U_ZERO_ERROR;
   
    if (verbose)
        fprintf(stdout, "processing the rest of the file \n");
    while( (bytesRead = fread((void *) rawBuf, 1, toRead, inFile)) > 0 || !tFlush)
    {
         int32_t  bytesNeeded = XMLUConvert( inConvrtr,
                      outConvrtr,
                      pRawBuf, 
                      (int32_t*)&bytesRead, 
                      outBuf, 
                      outBufSize, 
                      tFlush,
                      &err);
         if (bytesNeeded > 0)
         {
             long bout =
                 fwrite((void *) outBuf, 1, bytesNeeded, outFile);
             if (bout != bytesNeeded)
             {
                 fprintf(stderr, "Wrote only %d bytes.\n", bout);
                 fclose(inFile);
                 fclose(outFile);
             }
         }

        if ((err != U_BUFFER_OVERFLOW_ERROR) && U_FAILURE(err) )
        {
#if defined(_DEBUG)
            fprintf (stderr, "Error transcoding rest of the file: (%s) %d\n", u_errorName(err), err);
#endif
            fclose(inFile);
            fclose(outFile);
            exit(1);
        }
        if ((bytesRead > 0) && (err !=U_ZERO_ERROR))
        {
	  if(verbose)
	    fprintf(stderr, "err=%d * read %d bytes\n", err,bytesRead);

            if (fseek(inFile, (curPos+bytesRead), SEEK_SET))
            {
                fprintf(stderr, "fileSize - Could not set the input cursor to %d (curpos=%d, bytesRead=%d)\n", curPos+bytesRead,curPos,bytesRead);
                exit(1);
            }
            curPos = ftell(inFile);
            bytesLeft = endPos - curPos;
        }
        else 
        {
            curPos = ftell(inFile);
            bytesLeft = endPos - curPos;
        }
        toRead = (RAWBUFSIZE > bytesLeft) ? bytesLeft : RAWBUFSIZE;
        if (toRead < RAWBUFSIZE) tFlush = true;
        if (err == U_BUFFER_OVERFLOW_ERROR)
            err = U_ZERO_ERROR;
    }
    ucnv_close(inConvrtr);
    delete inEncodName;
    fclose(inFile);
    fclose(outFile);
};



int getInputEncodingType(const BYTE* rawBuffer, unsigned long byteCount)
{
    //match the first four bytes of the input buffer with the encoding types available
    //checking for ASCII
    //
    if (byteCount > 5)  
    {
        if (!memcmp(rawBuffer, gXMLDecl_ASCII, 5))
        return US_ASCII;
    }
     
    //  If the count of raw bytes is less than 2, it cannot be anything
    //  we understand, so return UTF-8 as a fallback.
    //
    if (byteCount < 2)
        return  UTF_8;

    //  We know its at least two bytes, so lets check for a UTF-16 BOM. 
    //
    if ((rawBuffer[0] == 0xFE) && (rawBuffer[1] == 0xFF))
        return UTF_16B;
    else if ((rawBuffer[0] == 0xFF) && (rawBuffer[1] == 0xFE))
        return UTF_16L;
    
    //  Oh well, not one of those. So now lets see if we have at least 4
    //  bytes. If not, then we are out of ideas and can return UTF-8 as the
    //  fallback.
    //
    if (byteCount < 4)
        return OtherEncoding;
    
    //  We have at least 4 bytes. So lets check the 4 byte sequences that
    //  indicate other UTF-16 encodings.
    //
    if ((rawBuffer[0] == 0x00) || (rawBuffer[0] == 0x3C))
    {
#if 0
        //not supported encodings
        if (!memcmp(rawBuffer, gUCS4BPre, 4))
            return UCS_4B;
        else if (!memcmp(rawBuffer, gUCS4LPre, 4))
            return UCS_4L;
        else 
#endif
            if (!memcmp(rawBuffer, gUTF16BPre, 4))
            return UTF_16B;
        else if (!memcmp(rawBuffer, gUTF16LPre, 4))
            return UTF_16L;
    }
    
    //  See if we have enough bytes to possibly match the EBCDIC prefix.
    //  If so, try it.
    //
    if (!memcmp(rawBuffer, gEBCDICPre, 4))
         return EBCDIC;

    //  Does not seem to be anything we know, so go with UTF-8 to get at
    //  least through the first line and see what it really is.
    //
    return OtherEncoding;
}


long convertFirstLine( FILE* inF, char* inEncName, 
                       FILE* outF, char* outEncName, 
                       char* ptrBuf, unsigned long toRead, 
                       UChar* uBuf)
{
    //Here we read the inputFile with the specified buffer size.
    //Then convert this to ascii. then read the first line and convert to 
    //output and input encoding types and return for rest of the conversion
    //

    if (fseek(inF, 0, SEEK_SET))
    {
        fprintf(stderr, "file - Could not seek the begin pos \n");
        exit(1);
    }

    unsigned long bytesRead = fread( (void*)ptrBuf, 1, toRead, inF);

    char            tempBuf[RAWBUFSIZE];
    int             bufLength       = 0;
    long            bytesNeeded     = 0;
    UErrorCode      err             = U_ZERO_ERROR;

    bytesNeeded = ucnv_convert("ascii",
			inEncName,
			(char*) tempBuf,
			0,
			(const char*) ptrBuf,
			bytesRead,
			&err);
    
    if (err == U_BUFFER_OVERFLOW_ERROR)
    {
	    err = U_ZERO_ERROR;
    }
	else if (U_FAILURE(err))
	{
#if defined(_DEBUG)
		printf ("Error transcoding first line of input file: (%s) %d\n", u_errorName(err), err);
#endif
        fclose(inF);
    	fclose(outF);
        exit(1);
	}

    ucnv_convert("ascii",
			inEncName,
			(char*) tempBuf,
			bytesNeeded,
			(const char*) ptrBuf,
			bytesRead,
			&err);										 

	if (U_FAILURE(err))
	{
#if defined(_DEBUG)
		printf ("Error transcoding2 first line of input file: (%s) %d\n", u_errorName(err), err);
#endif
        fclose(inF);
    	fclose(outF);
        exit(1);
    } 
    else
	{
        //read the tempBuf to get the first line
        //
        char firstLineBuf[FIRSTLINEBUF];
        int tempBufLength = 0;
                
        for( bufLength = 0,  tempBufLength=0; bufLength < FIRSTLINEBUF; bufLength++, tempBufLength++)
        {
            if ((tempBufLength == 0) && ((inEncName == "utf-16be") || (inEncName == "utf-16le") || (inEncName == "utf16")) )
                tempBufLength++;
            firstLineBuf[bufLength] = (char)tempBuf[tempBufLength];
            if (tempBuf[tempBufLength] == 0x3E) {
                firstLineBuf[bufLength+1] = '\0';
                break;
            }
            
        }
        char* pFLB = new char[sizeof(firstLineBuf) +1];
        strcpy(pFLB, firstLineBuf);

        //if the file doesnot contain the version string line then its and illegal file
        //
        if (firstLineBuf[0] != 0x3C ) 
        {
              fprintf(stderr,"Illegal xml file: It doesnot contain the xml declaration statement on the first line \n");
              fclose(inF);
    	      fclose(outF);
              exit(1);
        }
     
        bool encString      = true;
        bool stdString      = true;
        bool encInsertMid   = false;
        bool encInsertLast  = false;
        bool dQuote         = true;
        char* doubleQuote   = "\"";
        char* singleQuote   = "\'";
        
        if (!strstr( (const char*)pFLB, doubleQuote))
        {
            if (!strstr( (const char*)pFLB, singleQuote))
            {
              fprintf(stderr,"Illegal xml file: It doesnot contain the approprite xml declaration \n");
              fclose(inF);
    	      fclose(outF);
              exit(1);
            }
            dQuote = false;
        }
        
        char* newString     = strstr( (const char*) pFLB, "encoding");
        char* stringWithEnc = 0;

        if (!newString)
            encString = false;
        else 
	  {
	    stringWithEnc = new char[strlen(newString)+1];
            strcpy(stringWithEnc, newString);
	  }
            
        newString = strstr( (const char*) pFLB, "standalone");
        char* stringWithStd = 0;
        if (!newString) 
            stdString = false;
        else
        {
	    stringWithStd = new char[strlen(newString)+1];
            strcpy(stringWithStd, newString);
       }
        
        if (!encString && !stdString)
             encInsertLast = true;
        if (!encString && stdString)
             encInsertMid = true;

        //Encodingname for the rest of the input file could be different. 
        //If its not specified in the  first line then assume it to be UTF8
        if (encInsertLast || encInsertMid)
        {
            //if the encoding type was found utf16 family or ebcdic and 
            // the encoding string is not present in the file then its an error
            if (!strcmp(inEncName, "utf-16be") 
                || !strcmp(inEncName, "utf-16le")
                || !strcmp(inEncName, "ebcdic-cp-us"))
            {
                fprintf(stderr, "Illegal xml file: it doesnot contain the encoding string in the first line of the input file\n");
                fclose(inF);
    	        fclose(outF);
                exit(1);
            }
            strcpy(encodingNameInFile, inEncName);
        }

        char* tempString    = " encoding=";
        char* dupFLB        = strdup(pFLB);
	int stringTwoLength = 0;

	/* build up the length */
	stringTwoLength = bufLength;

	if(tempString)
	  stringTwoLength += strlen(tempString);

	if(outEncName)
	  stringTwoLength += strlen(outEncName);

	if(stringWithStd)
	  stringTwoLength += strlen(stringWithStd);

	stringTwoLength   += 5;
	
        char* stringTwo     = new char[stringTwoLength];

        if (encInsertLast) {
            char* stringOne = new char[bufLength];
            strncpy(stringOne, pFLB, bufLength-1);
            strcpy(stringOne+bufLength-1, "");
            stringTwo = strcpy(stringTwo, stringOne);
            strcat(stringTwo, tempString);
            catString(stringTwo, dQuote);
            strcat(stringTwo, outEncName);
            catString(stringTwo, dQuote);
            strcat(stringTwo , " ?>");
            delete stringOne;
        }
        //insert the string before 'standalone' statement
        else if (encInsertMid) {
            char* stringThree = new char[bufLength + strlen(tempString) + strlen(outEncName) + 5];
            if (dQuote)         
                stringThree = strtok(dupFLB, doubleQuote);
            else
                stringThree = strtok(dupFLB, singleQuote);

            strcpy(stringTwo, stringThree);
            catString(stringTwo, dQuote);

            char* tmpString;
            if (dQuote)
                tmpString = strtok(0, doubleQuote);
            else
                tmpString = strtok(0, singleQuote);
            if (tmpString != NULL)
                strcat(stringTwo, tmpString);
            
            catString(stringTwo, dQuote);
            strcat(stringTwo, tempString);
            catString(stringTwo, dQuote);

            strcat(stringTwo, outEncName);
            if (dQuote)
                strcat(stringTwo, "\" ");
            else
                strcat(stringTwo, "\' ");
            strcat(stringTwo, stringWithStd);
            delete stringThree;
        }
        //if the encoding string is there then modify the output encoding name in it.
        else if (encString)
        {
            char* stringFive  = new char[strlen(dupFLB)+1];
                    
            if (dQuote)
                stringFive = strtok (dupFLB, doubleQuote);
            else
                stringFive = strtok (dupFLB, singleQuote);

            strcpy(stringTwo, stringFive);
            catString(stringTwo, dQuote);
            while (stringFive != NULL)
            {
                if (dQuote)
                    stringFive = strtok(0,doubleQuote);
                else
                    stringFive = strtok(0,singleQuote);

                if (stringFive == NULL)
                    break;
                strcat(stringTwo, stringFive);
                
                char* n1String = strstr(stringFive, ">");
                if (!n1String)
                    catString(stringTwo, dQuote);
                
                char* nString = strstr(stringFive, "encoding");
                if (nString) 
                {
                    strcat(stringTwo, outEncName);
                    if (dQuote)
                        stringFive = strtok(0, doubleQuote);
                    else
                        stringFive = strtok(0, singleQuote);
                    strcpy(encodingNameInFile, stringFive); //this is the encoded string name
                    catString(stringTwo, dQuote);
                }
            }
            if (stringFive != NULL)
            {
                delete stringFive;
                stringFive = 0;
            }
        }
      
        // introduce the first order bytes for utf16 be and le files
        //
        if (!strcmp(outEncName, "utf-16be") || !strcmp(outEncName, "utf16"))
        {
            uBuf[0] = 0xFE;
            fwrite( (void*) uBuf, 1, 1, outF);
            uBuf[0] = 0xFF;
            fwrite( (void*) uBuf, 1, 1, outF);
        } else if (!strcmp(outEncName , "utf-16le"))
        {
           uBuf[0] = 0xFF;
           fwrite( (void*) uBuf, 1, 1, outF);
           uBuf[0] = 0xFE;
           fwrite( (void*) uBuf, 1, 1, outF);
        }

        err = U_ZERO_ERROR;
        long oneChar = 0;
        while ( *stringTwo != '\0' )
        {
            //transcode character-by-character
            oneChar = ucnv_convert(outEncName,
			    "ascii",
			    (char*) uBuf,
			    0,
                (const char*) stringTwo,
			    1,
			    &err);
            if (err == U_BUFFER_OVERFLOW_ERROR)
            {
	            err = U_ZERO_ERROR;
            }
	        else if (U_FAILURE(err))
	        {
#if defined(_DEBUG)
		        fprintf (stderr, "Error transcoding char-by-char: (%s) %d\n", u_errorName(err), err);
#endif
                fclose(inF);
    	        fclose(outF);
                exit(1);
	        }

            ucnv_convert(outEncName,
			    "ascii",
			    (char*) uBuf,
			    oneChar,
                (const char*) stringTwo,
			    1,
			    &err);
	        if (U_FAILURE(err))
	        {
#if defined(_DEBUG)
		        fprintf (stderr, "Error transcoding2 char-by-char: (%s) %d\n", u_errorName(err), err);
#endif
                fclose(inF);
    	        fclose(outF);
                exit(1);
            } 
            fwrite( (void*) uBuf, 1, oneChar, outF);
            stringTwo++;
        }
    }
    

    //Now get the pointer offset after the first line in the input file 
    //and return this position
    //
    char* newInEncName  = new char[strlen(inEncName) +1];
    strcpy(newInEncName, inEncName);
    if (encodingNameInFile !=NULL) 
    {
        if (inEncName)
            delete newInEncName;
        newInEncName = new char[strlen(encodingNameInFile)+1];
        strcpy(newInEncName, encodingNameInFile);
    }

     char   oldBuf[RAWBUFSIZE];   
     int    bufHere    = bufLength +1;
     if (!strcmp(newInEncName, "utf-16be") || !strcmp(newInEncName, "utf16") || !strcmp(newInEncName, "utf-16le")) 
     {
         bufHere +=1;
        memcpy((void*)oldBuf, (void*) tempBuf, bufHere);
     }
     else
              memcpy((void*)oldBuf, (void*) tempBuf, bufHere);
     
     char   newBuf[RAWBUFSIZE];
     long   endBytes    = 0;    
     //transcode this ascii type to the input encoding type  
     //and get the pointer to the end of first line in the input buffer 
     //
     err = U_ZERO_ERROR;
     endBytes = ucnv_convert(newInEncName,
     		"ascii",
			(char*) newBuf,
			0,
			(const char*) oldBuf,
			bufHere,
			&err);
    
    if (err == U_BUFFER_OVERFLOW_ERROR)
    {
	    err = U_ZERO_ERROR;
    }
	else if (U_FAILURE(err))
	{
#if defined(_DEBUG)
		fprintf (stderr, "Error transcoding from ascii to input encoding: (%s) %d\n", u_errorName(err), err);
#endif
        fclose(inF);
    	fclose(outF);
        exit(1);
	}
    ucnv_convert(newInEncName,
        	"ascii",
			(char*) newBuf,
			endBytes,
			(const char*) oldBuf,
			bufHere,
			&err);										 
	if (U_FAILURE(err))
	{
#if defined(_DEBUG)
		fprintf (stderr, "Error transcoding2 from ascii to input encoding: (%s) %d\n", u_errorName(err), err);
#endif
        delete newInEncName;
        fclose(inF);
    	fclose(outF);
        exit(1);
    }
    
    return endBytes;
}


int32_t  XMLUConvert( UConverter* inConverter,
                      UConverter* outConverter,
                      const char* inBuffer, 
                      int32_t* inBufSize, 
                      char* outBuffer, 
                      int32_t outBufCapacity, 
                      UBool flush,
                      UErrorCode* err)
{
    const char* inBufferAlias = inBuffer;
    char* outBufferAlias = outBuffer;
    const char* inBufferEnd = inBuffer + *inBufSize;
    const char* outBufferEnd = outBuffer + outBufCapacity;
    //const char* consumed;
    
    if (U_FAILURE(*err)) return 0;
    
    XMLU_fromCodepageToCodepage(outConverter,
        inConverter,
        &outBufferAlias, 
        outBufferEnd, 
        &inBufferAlias, 
        inBufferEnd, 
        NULL, 
        flush,
        err);
    
    if (*err == U_INDEX_OUTOFBOUNDS_ERROR) *err = U_BUFFER_OVERFLOW_ERROR;
    
   // *inBufSize = inBufferAlias;
    return outBufferAlias - outBuffer;
}

void XMLU_fromCodepageToCodepage(    UConverter*    outConverter,
                        UConverter*    inConverter,
                        char**         target,
                        const char*    targetLimit,
                        const char**   source,
                        const char*    sourceLimit,
                        int32_t*       offsets,
                        UBool         flush,
                        UErrorCode*    err)
{
    
#if 0
    UChar out_chunk[RAWBUFSIZE];
    const UChar* out_chunk_limit = out_chunk + RAWBUFSIZE;
    UChar* out_chunk_alias;
    UChar const* out_chunk_alias2;
    UChar const* consumed_UChars;
    
    
    if (U_FAILURE(*err)) return;
    
    *consumed = *source;
    /*loops until the input buffer is completely consumed
    *or if an error has be encountered
    *first we convert from inConverter codepage to Unicode
    *then from Unicode to outConverter codepage
    */
    
    while ((sourceLimit != *source) && U_SUCCESS(*err))
    {
        out_chunk_alias = out_chunk;
        *source = *consumed;
        ucnv_reset(inConverter);
        ucnv_toUnicode(inConverter,
            &out_chunk_alias,
            out_chunk_limit,
            source,
            sourceLimit,
            consumed,
            flush,
            err);
        
            /*U_INDEX_OUTOFBOUNDS_ERROR means that the output "CHUNK" is full
            *we will require at least another loop (it's a recoverable error)
        */
        
        if (U_SUCCESS(*err) || (*err == U_INDEX_OUTOFBOUNDS_ERROR))
        {
            *err = U_ZERO_ERROR;
            out_chunk_alias2 = out_chunk;
            
            while ((out_chunk_alias2 != out_chunk_alias) && U_SUCCESS(*err))
            {
                ucnv_fromUnicode(outConverter,
                    target,
                    targetLimit,
                    &out_chunk_alias2,
                    out_chunk_alias,
                    &consumed_UChars,
                    FALSE,
                    err); 
                
            }
        }
        else break;
    }
    return;

#endif


  UChar out_chunk[RAWBUFSIZE];
  const UChar *out_chunk_limit = out_chunk + RAWBUFSIZE;
  UChar *out_chunk_alias;
  UChar const *out_chunk_alias2;


  if (U_FAILURE (*err))    return;


  /*loops until the input buffer is completely consumed
   *or if an error has be encountered
   *first we convert from inConverter codepage to Unicode
   *then from Unicode to outConverter codepage
   */
  while ((*source != sourceLimit) && U_SUCCESS (*err))
    {
      out_chunk_alias = out_chunk;
      ucnv_toUnicode (inConverter,
		      &out_chunk_alias,
		      out_chunk_limit,
		      source,
		      sourceLimit,
		      NULL,
		      flush,
		      err);

      /*U_INDEX_OUTOFBOUNDS_ERROR means that the output "CHUNK" is full
       *we will require at least another loop (it's a recoverable error)
       */

      if (U_SUCCESS (*err) || (*err == U_INDEX_OUTOFBOUNDS_ERROR))
	{
	  *err = U_ZERO_ERROR;
	  out_chunk_alias2 = out_chunk;

	  while ((out_chunk_alias2 != out_chunk_alias) && U_SUCCESS (*err))
	    {
	      ucnv_fromUnicode (outConverter,
				target,
				targetLimit,
				&out_chunk_alias2,
				out_chunk_alias,
				NULL,
				TRUE,
				err);

	    }
	}
      else
	break;
    }

  return;
}

void catString(char* thisString, bool quote)
{
    if (quote)
        strcat(thisString, "\"");
    else
        strcat(thisString, "\'");
}
