/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
**********************************************************************
*/
#ifndef _STRINGPERF_H
#define _STRINGPERF_H

#include <string>

#include "uperf.h"
#include "unicode/utypes.h"
#include "unicode/unistr.h"

typedef std::basic_string<UChar> stlstring;	

// Define all constants for test case operations
#define MAXNUMLINES	40000
#define MAXSRCLEN 50
#define LOOPS 10
#define catenate_STRLEN 2

const UChar TESTCHAR1 =  'a';
const UChar* catenate_STR	= L"!!";
const UnicodeString uEMPTY;
const stlstring sEMPTY;
const UnicodeString uScan_STRING=L"Dot. 123. Some more data.";
const stlstring sScan_STRING=L"Dot. 123. Some more data.";

// global variables for all operations
UnicodeString unistr;
stlstring stlstr;
// for concatenation operation
static UnicodeString* catICU;
static stlstring* catStd;
BOOL bCatenatePrealloc;
// Simulate construction with a single-char string for basic_string
UChar simulate[2]={TESTCHAR1, 0};


enum FnType { Fn_ICU, Fn_STD };
typedef FnType FnType;
typedef void (*ICUStringPerfFn)(const UChar* src,int32_t srcLen, UnicodeString s0);
typedef void (*StdStringPerfFn)(const UChar* src,int32_t srcLen, stlstring s0);


class StringPerfFunction : public UPerfFunction
{
public:
	virtual void call(UErrorCode* status)
	{
        if(line_mode_==TRUE){
            if(uselen_){
                for(int32_t i = 0; i< numLines_; i++){
					if (fnType_==Fn_ICU) {
						(*fn1_)(lines_[i].name,lines_[i].len,uS0_[i]);
					} else {
						(*fn2_)(lines_[i].name,lines_[i].len,sS0_[i]);
					}
                }
            }else{
                for(int32_t i = 0; i< numLines_; i++){
                    if (fnType_==Fn_ICU) {
						(*fn1_)(lines_[i].name,-1,uS0_[i]);
					} else {
						(*fn2_)(lines_[i].name,-1,sS0_[i]);
					}
                }
            }
        }else{
            if(uselen_){
				if (fnType_==Fn_ICU) {
					(*fn1_)(src_,srcLen_,*ubulk_);
				} else {
					(*fn2_)(src_,srcLen_,*sbulk_);
				}
            }else{
				if (fnType_==Fn_ICU) {
					(*fn1_)(src_,-1,*ubulk_);
				} else {
					(*fn2_)(src_,-1,*sbulk_);
				}
            }
        }
	}

	virtual long getOperationsPerIteration()
	{
        if(line_mode_==TRUE){
            int32_t totalChars=0;
            for(int32_t i =0; i< numLines_; i++){
                totalChars+= lines_[i].len;
            }
            return totalChars;
        }else{
            return srcLen_;
        }
	}

	StringPerfFunction(ICUStringPerfFn func, ULine* srcLines, int32_t srcNumLines, UBool uselen)
	{
		fn1_ = func;
		lines_=srcLines;
		numLines_=srcNumLines;
		uselen_=uselen;
		line_mode_=TRUE;
        src_ = NULL;
        srcLen_ = 0;
		fnType_ = Fn_ICU;
		uS0_=new UnicodeString[numLines_];
		for(int32_t i=0; i<numLines_; i++) {
			uS0_[i]=UnicodeString(lines_[i].name, lines_[i].len);
		} 
		sS0_=NULL;
		ubulk_=NULL;
		sbulk_=NULL;
	} 

	StringPerfFunction(StdStringPerfFn func, ULine* srcLines, int32_t srcNumLines, UBool uselen)
	{
		fn2_ = func;
		lines_=srcLines;
		numLines_=srcNumLines;
		uselen_=uselen;
		line_mode_=TRUE;
        src_ = NULL;
        srcLen_ = 0;
		fnType_ = Fn_STD;
		sS0_=new stlstring[numLines_];
		for(int32_t i=0; i<numLines_; i++) {		
			if(uselen_) {
				sS0_[i]=stlstring(lines_[i].name, lines_[i].len);
			} else {
				sS0_[i]=stlstring(lines_[i].name);
			}
		} 
		uS0_=NULL;
		ubulk_=NULL;
		sbulk_=NULL;
	} 

	StringPerfFunction(ICUStringPerfFn func, UChar* source, int32_t sourceLen, UBool uselen)
	{
		fn1_ = func;
		lines_=NULL;
		numLines_=0;
		uselen_=uselen;
		line_mode_=FALSE;
		src_ = new UChar[sourceLen];
		memcpy(src_, source, sourceLen * U_SIZEOF_UCHAR);
        srcLen_ = sourceLen;

		fnType_ = Fn_ICU;
		uS0_=NULL;
		sS0_=NULL;	
		ubulk_=new UnicodeString(src_,srcLen_);
		sbulk_=NULL;
	}

	StringPerfFunction(StdStringPerfFn func, UChar* source, int32_t sourceLen, UBool uselen)
	{
		fn2_ = func;
		lines_=NULL;
		numLines_=0;
		uselen_=uselen;
		line_mode_=FALSE;
		src_ = new UChar[sourceLen];
		memcpy(src_, source, sourceLen * U_SIZEOF_UCHAR);
        srcLen_ = sourceLen;

		fnType_ = Fn_STD;
		uS0_=NULL;
		sS0_=NULL;
		ubulk_=NULL;
		if(uselen_) {
			sbulk_=new stlstring(src_,srcLen_);
		} else {
			sbulk_=new stlstring(src_);
		}			 
	}

	~StringPerfFunction()
	{
		//free(src_);
		delete[] src_;
		delete ubulk_;
		delete sbulk_;
		delete[] uS0_;
		delete[] sS0_;
	}

private:
	ICUStringPerfFn fn1_;
	StdStringPerfFn fn2_;
	long COUNT_;
	ULine* lines_;
	int32_t numLines_;
	UBool uselen_;
	UChar* src_;
    int32_t srcLen_;
    UBool line_mode_;
	//added for preparing testing data
	UnicodeString* uS0_;
	stlstring* sS0_;
	UnicodeString* ubulk_;
	stlstring* sbulk_;
	FnType fnType_;
}; 


class StringPerformanceTest : public UPerfTest
{
public:
	StringPerformanceTest(int32_t argc, const char *argv[], UErrorCode &status);
	~StringPerformanceTest();
	virtual UPerfFunction* runIndexedTest(int32_t index, UBool exec,
		                                  const char *&name, 
										  char *par = NULL);     
	UPerfFunction* TestCtor();
	UPerfFunction* TestCtor1();
	UPerfFunction* TestCtor2();
	UPerfFunction* TestCtor3();
	UPerfFunction* TestAssign();
	UPerfFunction* TestAssign1();
	UPerfFunction* TestAssign2();
	UPerfFunction* TestGetch();
	UPerfFunction* TestCatenate();
	UPerfFunction* TestScan();
	UPerfFunction* TestScan1();
	UPerfFunction* TestScan2();

	UPerfFunction* TestStdLibCtor();
	UPerfFunction* TestStdLibCtor1();
	UPerfFunction* TestStdLibCtor2();
	UPerfFunction* TestStdLibCtor3();
	UPerfFunction* TestStdLibAssign();
	UPerfFunction* TestStdLibAssign1();
	UPerfFunction* TestStdLibAssign2();
	UPerfFunction* TestStdLibGetch();
	UPerfFunction* TestStdLibCatenate();
	UPerfFunction* TestStdLibScan();
	UPerfFunction* TestStdLibScan1();
	UPerfFunction* TestStdLibScan2();

private:
	long COUNT_;
	ULine* filelines_;
	UChar* StrBuffer;
	int32_t StrBufferLen;

};


inline void ctor(const UChar* src,int32_t srcLen, UnicodeString s0) 
{
	UnicodeString a;		
}

inline void ctor1(const UChar* src,int32_t srcLen, UnicodeString s0) 
{
	UnicodeString b(TESTCHAR1);
}

inline void ctor2(const UChar* src,int32_t srcLen, UnicodeString s0) 
{
	UnicodeString c(uEMPTY);
}

inline void ctor3(const UChar* src,int32_t srcLen, UnicodeString s0) 
{
	UnicodeString d(src,srcLen);
}

inline UnicodeString icu_assign_helper(const UChar* src,int32_t srcLen)
{
	if (srcLen==-1) { return src;} 
	else { return UnicodeString(src, srcLen);}
}

inline void assign(const UChar* src,int32_t srcLen, UnicodeString s0) 
{
	unistr = icu_assign_helper(src,srcLen);
}

inline void assign1(const UChar* src,int32_t srcLen, UnicodeString s0) 
{
	unistr.setTo(src, srcLen);
}

inline void assign2(const UChar* src,int32_t srcLen, UnicodeString s0) 
{
	unistr = s0;
}

inline void getch(const UChar* src,int32_t srcLen, UnicodeString s0)
{
	s0.charAt(0);
}


inline void catenate(const UChar* src,int32_t srcLen, UnicodeString s0)
{
   	*catICU += s0;
	*catICU += catenate_STR;
}

volatile int scan_idx;

inline void scan(const UChar* src,int32_t srcLen, UnicodeString s0)
{
	UChar c='.';
	scan_idx = uScan_STRING.indexOf(c);
}

inline void scan1(const UChar* src,int32_t srcLen, UnicodeString s0)
{
	scan_idx = uScan_STRING.indexOf(L"123",3);
}

inline void scan2(const UChar* src,int32_t srcLen, UnicodeString s0)
{
	UChar c1='s';
	UChar c2='m';
	scan_idx = uScan_STRING.indexOf(c1);
	scan_idx = uScan_STRING.indexOf(c2);
}


inline void StdLibCtor(const UChar* src,int32_t srcLen, stlstring s0)
{
	stlstring a;
}

inline void StdLibCtor1(const UChar* src,int32_t srcLen, stlstring s0)
{

	stlstring b(simulate);
}

inline void StdLibCtor2(const UChar* src,int32_t srcLen, stlstring s0)
{
	stlstring c(sEMPTY);
}

inline void StdLibCtor3(const UChar* src,int32_t srcLen, stlstring s0)
{
	if (srcLen==-1) {	
		stlstring d(src); 
	}else {
		stlstring d(src, srcLen); 
	}
}

inline stlstring stl_assign_helper(const UChar* src,int32_t srcLen)
{
	if (srcLen==-1) { return src;}
	else { return stlstring(src, srcLen);}
}

inline void StdLibAssign(const UChar* src,int32_t srcLen, stlstring s0) 
{
	stlstr = stl_assign_helper(src,srcLen);
}

inline void StdLibAssign1(const UChar* src,int32_t srcLen, stlstring s0) 
{
	if (srcLen==-1) { stlstr=src;}
	else { stlstr.assign(src, srcLen);}
}

inline void StdLibAssign2(const UChar* src,int32_t srcLen, stlstring s0) 
{
	stlstr=s0;
}

inline void StdLibGetch(const UChar* src,int32_t srcLen, stlstring s0)
{
	s0.at(0);
}

inline void StdLibCatenate(const UChar* src,int32_t srcLen, stlstring s0)
{
	
    *catStd += s0;
    *catStd += catenate_STR;
	
}

inline void StdLibScan(const UChar* src,int32_t srcLen, stlstring s0)
{
	scan_idx = (int) sScan_STRING.find('.');	
}

inline void StdLibScan1(const UChar* src,int32_t srcLen, stlstring s0)
{
	scan_idx = (int) sScan_STRING.find(L"123");
}

inline void StdLibScan2(const UChar* src,int32_t srcLen, stlstring s0)
{	
	scan_idx = (int) sScan_STRING.find_first_of(L"sm");
}

#endif // STRINGPERF_H