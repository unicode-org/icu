/*
**********************************************************************
* Copyright (c) 2002-2010,International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
**********************************************************************
*/

#ifndef _DATEFMTPERF_H
#define _DATEFMTPERF_H

#include "unicode/uperf.h"
#include "unicode/unistr.h"
#include "unicode/datefmt.h"
#include "unicode/calendar.h"
#include "unicode/uclean.h"
#include "unicode/brkiter.h"
#include "util.h"
#include "datedata.h"
#include "breakdata.h"
#include <stdlib.h>
#include <fstream>
#include <string>

#include <iostream>
using namespace std;

//  Stubs for Windows API functions when building on UNIXes.
//
#if defined(U_WINDOWS)
// do nothing
#else
#define _UNICODE
typedef int DWORD;
inline int FoldStringW(DWORD dwMapFlags, const UChar* lpSrcStr,int cchSrc, UChar* lpDestStr,int cchDest);
#endif

class BreakItFunction : public UPerfFunction
{
private:
	int num;
	bool wordIteration;

public:

	BreakItFunction(){num = -1;}
	BreakItFunction(int a, bool b){num = a; wordIteration = b;}

	virtual void call(UErrorCode *status)
	{		
		BreakIterator* boundary;

		if(wordIteration)
		{	
			for(int i = 0; i < num; i++)
			{
				boundary = BreakIterator::createWordInstance(Locale::getUS(), *status);
				boundary->setText(str);

				int32_t start = boundary->first();
				for (int32_t end = boundary->next();
					 end != BreakIterator::DONE;
					 start = end, end = boundary->next())
				{
					printTextRange( *boundary, start, end );
				}
			}
		}
		else // character iteration
		{
			for(int i = 0; i < num; i++)
			{
				boundary = BreakIterator::createCharacterInstance(Locale::getUS(), *status);
				boundary->setText(str);

				int32_t start = boundary->first();
				for (int32_t end = boundary->next();
					 end != BreakIterator::DONE;
					 start = end, end = boundary->next())
				{
					printTextRange( *boundary, start, end );
				}
			}
		}


	}

	virtual long getOperationsPerIteration()
	{
		if(wordIteration) return 125*num;
		else return 355*num;
	}

	void printUnicodeString(const UnicodeString &s) {
		char charBuf[1000];
		s.extract(0, s.length(), charBuf, sizeof(charBuf)-1, 0);   
		charBuf[sizeof(charBuf)-1] = 0;          
		printf("%s", charBuf);
	}


	void printTextRange( BreakIterator& iterator, 
						int32_t start, int32_t end )
	{
		CharacterIterator *strIter = iterator.getText().clone();
		UnicodeString  s;
		strIter->getText(s);
		//printUnicodeString(UnicodeString(s, start, end-start));
		//puts("");
		delete strIter;
	}

	// Print the given string to stdout (for debugging purposes)
	void uprintf(const UnicodeString &str) {
		char *buf = 0;
		int32_t len = str.length();
		int32_t bufLen = len + 16;
		int32_t actualLen;
		buf = new char[bufLen + 1];
		actualLen = str.extract(0, len, buf/*, bufLen*/); // Default codepage conversion
		buf[actualLen] = 0;
		printf("%s", buf);
		delete[] buf;
	}

};

class DateFmtFunction : public UPerfFunction
{

private:
	int num;
public:
	
	DateFmtFunction()
	{
		num = -1;
	}

	DateFmtFunction(int a)
	{
		num = a;
	}

	virtual void call(UErrorCode* status)
	{
		UErrorCode status2 = U_ZERO_ERROR;		
		Calendar *cal;
		TimeZone *zone;
		UnicodeString str;
		UDate date;

		cal = Calendar::createInstance(status2);
		check(status2, "Calendar::createInstance");
		zone = TimeZone::createTimeZone("GMT"); // Create a GMT zone
		cal->adoptTimeZone(zone);
		
		Locale loc("en");
		DateFormat *fmt;
		fmt = DateFormat::createDateTimeInstance(
								DateFormat::kFull, DateFormat::kFull, loc);

		
		// (dates are imported from datedata.h)
		for(int j = 0; j < num; j++)
			for(int i = 0; i < NUM_DATES; i++)
			{
				cal->clear();
				cal->set(years[i], months[i], days[i]);
				date = cal->getTime(status2);
				check(status2, "Calendar::getTime");

				fmt->setCalendar(*cal);

				// Format the date
				str.remove();
				fmt->format(date, str, status2);

				
				// Display the formatted date string
				//uprintf(str);
				//printf("\n");
				
			}
		
		delete fmt;
		delete cal;
		//u_cleanup();

	}

	virtual long getOperationsPerIteration()
	{
		return NUM_DATES * num;
	}

	// Print the given string to stdout (for debugging purposes)
	void uprintf(const UnicodeString &str) {
		char *buf = 0;
		int32_t len = str.length();
		int32_t bufLen = len + 16;
		int32_t actualLen;
		buf = new char[bufLen + 1];
		actualLen = str.extract(0, len, buf/*, bufLen*/); // Default codepage conversion
		buf[actualLen] = 0;
		printf("%s", buf);
		delete[] buf;
	}

	// Verify that a UErrorCode is successful; exit(1) if not
	void check(UErrorCode& status, const char* msg) {
		if (U_FAILURE(status)) {
			printf("ERROR: %s (%s)\n", u_errorName(status), msg);
			exit(1);
		}
	}

};

class DateFormatPerfTest : public UPerfTest
{
private:

public:

	DateFormatPerfTest(int32_t argc, const char* argv[], UErrorCode& status);
	~DateFormatPerfTest();
	virtual UPerfFunction* runIndexedTest(int32_t index, UBool exec,const char* &name, char* par);

	UPerfFunction* DateFmt250();
	UPerfFunction* DateFmt10000();
	UPerfFunction* DateFmt100000();
	UPerfFunction* BreakItWord250();
	UPerfFunction* BreakItWord10000();
	UPerfFunction* BreakItChar250();
	UPerfFunction* BreakItChar10000();
};

#endif // DateFmtPerf