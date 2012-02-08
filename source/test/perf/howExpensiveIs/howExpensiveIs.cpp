/*
 **********************************************************************
 * Copyright (c) 2011,International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 */
#include <stdio.h>
#include "sieve.h"
#include "unicode/utimer.h"
#include "udbgutil.h"

void runTests(void);

FILE *out = NULL;
UErrorCode setupStatus = U_ZERO_ERROR;

int main(int argc, const char* argv[]){
#if U_DEBUG
  fprintf(stderr,"%s: warning: U_DEBUG is on.\n", argv[0]);
#endif
#if U_DEBUG
  {
    double m;
    double s = uprv_getSieveTime(&m);
    fprintf(stderr, "** Standard sieve time: %.9fs +/- %.9fs (%d iterations)\n", s,m, (int)U_LOTS_OF_TIMES);
  }
#endif

  if(argc==2) {
    out=fopen(argv[1],"w");
    if(out==NULL) {
      fprintf(stderr,"Err: can't open %s for writing.\n", argv[1]);
      return 1;
    }
    fprintf(out, "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
    fprintf(out, "<tests icu=\"%s\">\n", U_ICU_VERSION);
    fprintf(out, "<!-- %s -->\n", U_COPYRIGHT_STRING);
  } else if(argc>2) {
    fprintf(stderr, "Err: usage: %s [ output-file.xml ]\n", argv[0]);
    return 1;
  }

  runTests();
  

  if(out!=NULL) {
    udbg_writeIcuInfo(out);
    fprintf(out, "</tests>\n");
    fclose(out);
  }

  if(U_FAILURE(setupStatus)) {
    fprintf(stderr, "Error in tests: %s\n", u_errorName(setupStatus));
    return 1;
  }
  
  return 0;
}

class HowExpensiveTest {
public:
  virtual ~HowExpensiveTest(){}
protected:
  HowExpensiveTest(const char *name, const char *file, int32_t line) : fName(name), fFile(file), fLine(line) {}
protected:
  /**
   * @return number of iterations 
   */
  virtual int32_t run() = 0;
  virtual void warmup() {  run(); } 
public:
  virtual int32_t runTest(double *subTime) {
    UTimer a,b;
    utimer_getTime(&a);
    int32_t iter = run();
    utimer_getTime(&b);
    *subTime = utimer_getDeltaSeconds(&a,&b);
    return iter;
  }

  virtual int32_t runTests(double *subTime, double *marginOfError) {
    warmup(); /* warmup */
    #define ITERATIONS 5
    double times[ITERATIONS];
    int subIterations = 0;
    for(int i=0;i<ITERATIONS;i++) {
      subIterations = runTest(&times[i]);
#if U_DEBUG
      fprintf(stderr, "trial: %d/%d = %.9fs\n", i, ITERATIONS,times[i]);
      fflush(stderr);
#endif
    }
    *subTime = uprv_getMeanTime(times,ITERATIONS,marginOfError);
    return subIterations;
  }
public:
  const char *fName;
  const char *fFile;
  int32_t fLine;
  int32_t fIterations;
};

void runTestOn(HowExpensiveTest &t) {
  fprintf(stderr, "%s:%d: Running: %s\n", t.fFile, t.fLine, t.fName);
  double sieveTime = uprv_getSieveTime(NULL);
  double st;
  double me;
  
  fflush(stdout);
  fflush(stderr);
  int32_t iter = t.runTests(&st,&me);
  fflush(stdout);
  fflush(stderr);
  
  double stn = st/sieveTime;

  printf("%s\t%.9f\t%.9f +/- %.9f,  @ %d iter\n", t.fName,stn,st,me,iter);

  if(out!=NULL) {
    fprintf(out, "   <test name=\"%s\" standardizedTime=\"%f\" realDuration=\"%f\" marginOfError=\"%f\" iterations=\"%d\" />\n",
            t.fName,stn,st,me,iter);
    fflush(out);
  }
}

/* ------------------- test code here --------------------- */

class SieveTest : public HowExpensiveTest {
public:
  virtual ~SieveTest(){}
  SieveTest():HowExpensiveTest("SieveTest",__FILE__,__LINE__){}
  virtual int32_t run(){return 0;} // dummy
  int32_t runTest(double *subTime) {
    *subTime = uprv_getSieveTime(NULL);
    return U_LOTS_OF_TIMES;
  }
  virtual int32_t runTests(double *subTime, double *marginOfError) {
    *subTime = uprv_getSieveTime(marginOfError);
    return U_LOTS_OF_TIMES;
  }
};


/* ------- NumParseTest ------------- */
#include "unicode/unum.h"
/* open and close tests */
#define OCName(svc,ub,testn,suffix,n) testn ## svc ## ub ## suffix ## n
#define OCStr(svc,ub,suffix,n) "Test_" # svc # ub # suffix # n
#define OCRun(svc,ub,suffix) svc ## ub ## suffix
// TODO: run away screaming
#define OpenCloseTest(n, svc,suffix,c,a,d) class OCName(svc,_,Test_,suffix,n) : public HowExpensiveTest { public: OCName(svc,_,Test_,suffix,n)():HowExpensiveTest(OCStr(svc,_,suffix,n),__FILE__,__LINE__) c int32_t run() { int32_t i; for(i=0;i<U_LOTS_OF_TIMES;i++){ OCRun(svc,_,close) (  OCRun(svc,_,suffix) a );  } return i; }   void warmup() { OCRun(svc,_,close) ( OCRun(svc,_,suffix) a); } virtual ~ OCName(svc,_,Test_,suffix,n) () d };
#define QuickTest(n,c,r,d)  class n : public HowExpensiveTest { public: n():HowExpensiveTest(#n,__FILE__,__LINE__) c int32_t run() r virtual ~n () d };

// TODO: move, scope.
static UChar pattern[] = { 0x23 }; // '#'

UNumberFormat *NumParseTest_fmt;

// TODO: de-uglify.
QuickTest(NumParseTest,{    static UChar pattern[] = { 0x23 };    NumParseTest_fmt = unum_open(UNUM_PATTERN_DECIMAL,         pattern,                    1,                    "en_US",                    0,                    &setupStatus);  },{    int32_t i;    static UChar str[] = { 0x31 };double val;    for(i=0;i<U_LOTS_OF_TIMES;i++) {      val=unum_parse(NumParseTest_fmt,str,1,NULL,&setupStatus);    }    return i;  },{unum_close(NumParseTest_fmt);})


QuickTest(NullTest,{},{int j=U_LOTS_OF_TIMES;while(--j);return U_LOTS_OF_TIMES;},{})
OpenCloseTest(pattern,unum,open,{},(UNUM_PATTERN_DECIMAL,pattern,1,"en_US",0,&setupStatus),{})
OpenCloseTest(default,unum,open,{},(UNUM_DEFAULT,NULL,-1,"en_US",0,&setupStatus),{})
#include "unicode/ucnv.h"
OpenCloseTest(gb18030,ucnv,open,{},("gb18030",&setupStatus),{})
#include "unicode/ures.h"
OpenCloseTest(root,ures,open,{},(NULL,"root",&setupStatus),{})

void runTests() {
  {
    SieveTest t;
    runTestOn(t);
  }
  {
    NullTest t;
    runTestOn(t);
  }
  {
    NumParseTest t;
    runTestOn(t);
  }
  {
    Test_unum_opendefault t;
    runTestOn(t);
  }
  {
    Test_ucnv_opengb18030 t;
    runTestOn(t);
  }
  {
    Test_unum_openpattern t;
    runTestOn(t);
  }
  {
    Test_ures_openroot t;
    runTestOn(t);
  }
}
