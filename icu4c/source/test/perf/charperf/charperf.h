/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
**********************************************************************
*/
#ifndef _CHARPERF_H
#define _CHARPERF_H

#include "uperf.h"
#include "unicode/uchar.h"
#include <stdlib.h>
#include <stdio.h>
#include <wchar.h>

typedef void (*CharPerfFn)(UChar32 ch);
typedef void (*StdLibCharPerfFn)(wchar_t ch);

class CharPerfFunction : public UPerfFunction
{
public:
	virtual void call()
	{
		for (UChar32 i = MIN_; i < MAX_; i ++) {
			(*m_fn_)(i);
		}
	}

	virtual long getOperationsPerIteration()
	{
		return MAX_ - MIN_;
	}

	virtual long getEventsPerIteration()
	{
		return -1;
	}

	UErrorCode getStatus()
	{
		return U_ZERO_ERROR;
	}

	CharPerfFunction(CharPerfFn func, UChar32 min, UChar32 max)
	{
		m_fn_ = func;
		MIN_ = min;
		MAX_ = max;
	}   

private:
	CharPerfFn m_fn_;
	UChar32 MIN_;
	UChar32 MAX_;
}; 

class StdLibCharPerfFunction : public UPerfFunction
{
public:
	virtual void call()
	{
		// note wchar_t is unsigned, it will revert to 0 once it reaches 
		// 65535
		for (wchar_t i = MIN_; i < MAX_; i ++) {
			(*m_fn_)(i);
		}
	}

	virtual long getOperationsPerIteration()
	{
		return MAX_ - MIN_;
	}

	virtual long getEventsPerIteration()
	{
		return -1;
	}

	UErrorCode getStatus()
	{
		return U_ZERO_ERROR;
	}

	StdLibCharPerfFunction(StdLibCharPerfFn func, wchar_t min, wchar_t max)
	{
		m_fn_ = func;			
		MIN_ = min;
		MAX_ = max;
	}   

	~StdLibCharPerfFunction()
	{			
	}

private:
	StdLibCharPerfFn m_fn_;
	wchar_t MIN_;
	wchar_t MAX_;
};

class CharPerformanceTest : public UPerfTest
{
public:
	CharPerformanceTest(int32_t argc, const char *argv[], UErrorCode &status);
	~CharPerformanceTest();
	virtual UPerfFunction* runIndexedTest(int32_t index, UBool exec,
		                                  const char *&name, 
										  char *par = NULL);     
	UPerfFunction* TestIsAlpha();
	UPerfFunction* TestIsUpper();
	UPerfFunction* TestIsLower();
	UPerfFunction* TestIsDigit();
	UPerfFunction* TestIsSpace();
	UPerfFunction* TestIsAlphaNumeric();
	UPerfFunction* TestIsPrint();
	UPerfFunction* TestIsControl();
	UPerfFunction* TestToLower();
	UPerfFunction* TestToUpper();
	UPerfFunction* TestIsWhiteSpace();
	UPerfFunction* TestStdLibIsAlpha();
	UPerfFunction* TestStdLibIsUpper();
	UPerfFunction* TestStdLibIsLower();
	UPerfFunction* TestStdLibIsDigit();
	UPerfFunction* TestStdLibIsSpace();
	UPerfFunction* TestStdLibIsAlphaNumeric();
	UPerfFunction* TestStdLibIsPrint();
	UPerfFunction* TestStdLibIsControl();
	UPerfFunction* TestStdLibToLower();
	UPerfFunction* TestStdLibToUpper();
	UPerfFunction* TestStdLibIsWhiteSpace();

private:
	UChar32 MIN_;
	UChar32 MAX_;
};

void isAlpha(UChar32 ch) 
{
    u_isalpha(ch);
}

void isUpper(UChar32 ch)
{
	u_isupper(ch);
}

void isLower(UChar32 ch)
{
	u_islower(ch);
}

void isDigit(UChar32 ch)
{
	u_isdigit(ch);
}

void isSpace(UChar32 ch)
{
	u_isspace(ch);
}

void isAlphaNumeric(UChar32 ch)
{
	u_isalnum(ch);
}

/**
 * This test may be different since c lib has a type PUNCT and it is printable.
 * iswgraph is not used for testing since it is a subset of iswprint with the
 * exception of returning true for white spaces. no match found in icu4c.
 */
void isPrint(UChar32 ch)
{
	u_isprint(ch);
}

void isControl(UChar32 ch)
{
	u_iscntrl(ch);
}

void toLower(UChar32 ch)
{
	u_tolower(ch);
}

void toUpper(UChar32 ch)
{
	u_toupper(ch);
}

void isWhiteSpace(UChar32 ch)
{
	u_isWhitespace(ch);
}

void StdLibIsAlpha(wchar_t ch)
{
	iswalpha(ch);
}

void StdLibIsUpper(wchar_t ch)
{
	iswupper(ch);
}

void StdLibIsLower(wchar_t ch)
{
	iswlower(ch);
}

void StdLibIsDigit(wchar_t ch)
{
	iswdigit(ch);
}

void StdLibIsSpace(wchar_t ch)
{
	iswspace(ch);
}

void StdLibIsAlphaNumeric(wchar_t ch)
{
	iswalnum(ch);
}

/**
 * This test may be different since c lib has a type PUNCT and it is printable.
 * iswgraph is not used for testing since it is a subset of iswprint with the
 * exception of returning true for white spaces. no match found in icu4c.
 */
void StdLibIsPrint(wchar_t ch)
{
	iswprint(ch);
}

void StdLibIsControl(wchar_t ch)
{
	iswcntrl(ch);
}

void StdLibToLower(wchar_t ch)
{
	towlower(ch);
}

void StdLibToUpper(wchar_t ch)
{
	towupper(ch);
}

void StdLibIsWhiteSpace(wchar_t ch)
{
	iswspace(ch);
}

#endif // CHARPERF_H