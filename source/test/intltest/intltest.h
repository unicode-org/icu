/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2009, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


/**
 * IntlTest is a base class for tests.  */

#ifndef _INTLTEST
#define _INTLTEST

// The following includes utypes.h, uobject.h and unistr.h
#include "unicode/fmtable.h"
#include "unicode/testlog.h"

U_NAMESPACE_USE

#ifdef OS390
// avoid collision with math.h/log()
// this must be after including utypes.h so that OS390 is actually defined
#pragma map(IntlTest::log( const UnicodeString &message ),"logos390")
#endif

//-----------------------------------------------------------------------------
//convenience classes to ease porting code that uses the Java
//string-concatenation operator (moved from findword test by rtg)
UnicodeString UCharToUnicodeString(UChar c);
UnicodeString Int64ToUnicodeString(int64_t num);
//UnicodeString operator+(const UnicodeString& left, int64_t num); // Some compilers don't allow this because of the long type.
UnicodeString operator+(const UnicodeString& left, long num);
UnicodeString operator+(const UnicodeString& left, unsigned long num);
UnicodeString operator+(const UnicodeString& left, double num);
UnicodeString operator+(const UnicodeString& left, char num); 
UnicodeString operator+(const UnicodeString& left, short num);  
UnicodeString operator+(const UnicodeString& left, int num);      
UnicodeString operator+(const UnicodeString& left, unsigned char num);  
UnicodeString operator+(const UnicodeString& left, unsigned short num);  
UnicodeString operator+(const UnicodeString& left, unsigned int num);      
UnicodeString operator+(const UnicodeString& left, float num);
#if !UCONFIG_NO_FORMATTING
UnicodeString toString(const Formattable& f); // liu
UnicodeString toString(int32_t n);
#endif
//-----------------------------------------------------------------------------

// Use the TESTCASE macro in subclasses of IntlTest.  Define the
// runIndexedTest method in this fashion:
//
//| void MyTest::runIndexedTest(int32_t index, UBool exec,
//|                             const char* &name, char* /*par*/) {
//|     switch (index) {
//|         TESTCASE(0,TestSomething);
//|         TESTCASE(1,TestSomethingElse);
//|         TESTCASE(2,TestAnotherThing);
//|         default: name = ""; break;
//|     }
//| }
#define TESTCASE(id,test)             \
    case id:                          \
        name = #test;                 \
        if (exec) {                   \
            logln(#test "---");       \
            logln();                  \
            test();                   \
        }                             \
        break

class IntlTest : public TestLog {
public:

    IntlTest();
    // TestLog has a virtual destructor.

    virtual UBool runTest( char* name = NULL, char* par = NULL ); // not to be overidden

    virtual UBool setVerbose( UBool verbose = TRUE );
    virtual UBool setNoErrMsg( UBool no_err_msg = TRUE );
    virtual UBool setQuick( UBool quick = TRUE );
    virtual UBool setLeaks( UBool leaks = TRUE );
    virtual UBool setWarnOnMissingData( UBool warn_on_missing_data = TRUE );
    virtual int32_t setThreadCount( int32_t count = 1);

    virtual int32_t getErrors( void );
    virtual int32_t getDataErrors (void );

    virtual void setCaller( IntlTest* callingTest ); // for internal use only
    virtual void setPath( char* path ); // for internal use only

    virtual void log( const UnicodeString &message );

    virtual void logln( const UnicodeString &message );

    virtual void logln( void );

    virtual void info( const UnicodeString &message );

    virtual void infoln( const UnicodeString &message );

    virtual void infoln( void );

    virtual void err(void);
    
    virtual void err( const UnicodeString &message );

    virtual void errln( const UnicodeString &message );

    virtual void dataerr( const UnicodeString &message );

    virtual void dataerrln( const UnicodeString &message );
    
    void errcheckln(UErrorCode status, const UnicodeString &message );

    // convenience functions: sprintf() + errln() etc.
    void log(const char *fmt, ...);
    void logln(const char *fmt, ...);
    void info(const char *fmt, ...);
    void infoln(const char *fmt, ...);
    void err(const char *fmt, ...);
    void errln(const char *fmt, ...);
    void dataerr(const char *fmt, ...);
    void dataerrln(const char *fmt, ...);
    void errcheckln(UErrorCode status, const char *fmt, ...);

    // Print ALL named errors encountered so far
    void printErrors(); 
        
    virtual void usage( void ) ;

    /**
     * Returns a uniform random value x, with 0.0 <= x < 1.0.  Use
     * with care: Does not return all possible values; returns one of
     * 714,025 values, uniformly spaced.  However, the period is
     * effectively infinite.  See: Numerical Recipes, section 7.1.
     *
     * @param seedp pointer to seed. Set *seedp to any negative value
     * to restart the sequence.
     */
    static float random(int32_t* seedp);

    /**
     * Convenience method using a global seed.
     */
    static float random();

    /**
     * Ascertain the version of ICU. Useful for 
     * time bomb testing
     */
    UBool isICUVersionAtLeast(const UVersionInfo x);

    enum { kMaxProps = 16 };

    virtual void setProperty(const char* propline);
    virtual const char* getProperty(const char* prop);

protected:
    /* JUnit-like assertions. Each returns TRUE if it succeeds. */
    UBool assertTrue(const char* message, UBool condition, UBool quiet=FALSE, UBool possibleDataError=FALSE);
    UBool assertFalse(const char* message, UBool condition, UBool quiet=FALSE);
    UBool assertSuccess(const char* message, UErrorCode ec, UBool possibleDataError=FALSE);
    UBool assertEquals(const char* message, const UnicodeString& expected,
                       const UnicodeString& actual, UBool possibleDataError=FALSE);
    UBool assertEquals(const char* message, const char* expected,
                       const char* actual);
#if !UCONFIG_NO_FORMATTING
    UBool assertEquals(const char* message, const Formattable& expected,
                       const Formattable& actual);
    UBool assertEquals(const UnicodeString& message, const Formattable& expected,
                       const Formattable& actual);
#endif
    UBool assertTrue(const UnicodeString& message, UBool condition, UBool quiet=FALSE);
    UBool assertFalse(const UnicodeString& message, UBool condition, UBool quiet=FALSE);
    UBool assertSuccess(const UnicodeString& message, UErrorCode ec);
    UBool assertEquals(const UnicodeString& message, const UnicodeString& expected,
                       const UnicodeString& actual);
    UBool assertEquals(const UnicodeString& message, const char* expected,
                       const char* actual);

    virtual void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL ); // overide !

    virtual UBool runTestLoop( char* testname, char* par );

    virtual int32_t IncErrorCount( void );

    virtual int32_t IncDataErrorCount( void );

    virtual UBool callTest( IntlTest& testToBeCalled, char* par );


    UBool       verbose;
    UBool       no_err_msg;
    UBool       quick;
    UBool       leaks;
    UBool       warn_on_missing_data;
    int32_t     threadCount;

private:
    UBool       LL_linestart;
    int32_t     LL_indentlevel;

    int32_t     errorCount;
    int32_t     dataErrorCount;
    IntlTest*   caller;
    char*       testPath;           // specifies subtests

    //FILE *testoutfp;
    void *testoutfp;

    const char* proplines[kMaxProps];
    int32_t     numProps;

protected:

    virtual void LL_message( UnicodeString message, UBool newline );

    // used for collation result reporting, defined here for convenience

    static UnicodeString &prettify(const UnicodeString &source, UnicodeString &target);
    static UnicodeString prettify(const UnicodeString &source, UBool parseBackslash=FALSE);
    static UnicodeString &appendHex(uint32_t number, int32_t digits, UnicodeString &target);

public:
    static void setICU_DATA();       // Set up ICU_DATA if necessary.

    static const char* pathToDataDirectory();

public:
    UBool run_phase2( char* name, char* par ); // internally, supports reporting memory leaks
    static const char* loadTestData(UErrorCode& err);
    virtual const char* getTestDataPath(UErrorCode& err);
    static const char* getSourceTestData(UErrorCode& err);

// static members
public:
    static IntlTest* gTest;
    static const char* fgDataDir;

};

void it_log( UnicodeString message );
void it_logln( UnicodeString message );
void it_logln( void );
void it_info( UnicodeString message );
void it_infoln( UnicodeString message );
void it_infoln( void );
void it_err(void);
void it_err( UnicodeString message );
void it_errln( UnicodeString message );
void it_dataerr( UnicodeString message );
void it_dataerrln( UnicodeString message );

/**
 * This is a variant of cintltst/ccolltst.c:CharsToUChars().
 * It converts a character string into a UnicodeString, with
 * unescaping \u sequences.
 */
extern UnicodeString CharsToUnicodeString(const char* chars);

/* alias for CharsToUnicodeString */
extern UnicodeString ctou(const char* chars);

// TODO(markus): Propose as public API.
/**
 * Does not throw exceptions.
 * Do not use this base class directly, since it does not delete its pointer.
 * A subclass must implement methods that delete the pointer:
 * Destructor and adoptInstead().
 *
 * There is no operator T *() provided because the programmer must decide
 * whether to use getAlias() (without transfer of ownership) or orpan()
 * (with transfer of ownership and NULLing of the pointer).
 */
template<typename T>
class /* U_COMMON_API */ LocalPointerBase {
public:
    // Takes ownership.
    explicit LocalPointerBase(T *p=NULL) : ptr(p) {}
    // Deletes the object it owns.
    ~LocalPointerBase() { /* delete ptr; */ }
    // NULL checks.
    UBool isNull() const { return ptr==NULL; }
    UBool isValid() const { return ptr!=NULL; }
    // Comparisons with simple pointers, so that existing code
    // with ==NULL and !=NULL need not be changed.
    bool operator==(const T *other) const { return ptr==other; }
    bool operator!=(const T *other) const { return ptr!=other; }
    // Access without ownership change.
    T *getAlias() const { return ptr; }
    T &operator*() const { return *ptr; }
    T *operator->() const { return ptr; }
    // Gives up ownership; the internal pointer becomes NULL.
    T *orphan() {
        T *p=ptr;
        ptr=NULL;
        return p;
    }
    // Deletes the object it owns and adopt (take ownership of) the one passed in.
    void adoptInstead(T *p) {
        // delete ptr;
        ptr=p;
    }
protected:
    T *ptr;
private:
    // No comparison operators with other LocalPointerBase's.
    bool operator==(const LocalPointerBase &other);
    bool operator!=(const LocalPointerBase &other);
    // No ownership transfer: No copy constructor, no assignment operator.
    LocalPointerBase(const LocalPointerBase &other);
    void operator=(const LocalPointerBase &other);
    // No heap allocation. Use only on the stack.
    static void * U_EXPORT2 operator new(size_t size);
    static void * U_EXPORT2 operator new[](size_t size);
#if U_HAVE_PLACEMENT_NEW
    static void * U_EXPORT2 operator new(size_t, void *ptr);
#endif
};

// TODO: Option 1: Destructor and adoptInstead() and possible future methods
// call a protected handleDelete() which is virtual and is the single method
// which a subclass must override.
// Con: Requires the LocalPointerBase class to be virtual, doubles its size,
// and the linker picks an arbitrary .o file for the vtable.
//    virtual void handleDelete() {
//        delete ptr;
//    }

// Option 2: Require all subclasses to override all deleting methods
// (destructor and adoptInstead()) if they need to use something other than
// the delete operator.
// Con: More methods need to be overwritten, and if we add further deleting
// methods, then that set increases further.
// Also, using a subclass instance via the base class API would not work,
// but that would be outside the intended usage.

template<typename T>
class /* U_COMMON_API */ LocalPointer : public LocalPointerBase<T> {
public:
    explicit LocalPointer(T *p=NULL) : LocalPointerBase<T>(p) {}
    ~LocalPointer() {
        delete LocalPointerBase<T>::ptr;
    }
    void adoptInstead(T *p) {
        delete LocalPointerBase<T>::ptr;
        LocalPointerBase<T>::ptr=p;
    }
};

template<typename T>
class /* U_COMMON_API */ LocalArray : public LocalPointerBase<T> {
public:
    explicit LocalArray(T *p=NULL) : LocalPointerBase<T>(p) {}
    ~LocalArray() {
        delete[] LocalPointerBase<T>::ptr;
    }
    void adoptInstead(T *p) {
        delete[] LocalPointerBase<T>::ptr;
        LocalPointerBase<T>::ptr=p;
    }
    T &operator[](ptrdiff_t i) const { return LocalPointerBase<T>::ptr[i]; }
};

// Requirement: The closeFunction must tolerate a NULL pointer.
// Or, we could add a NULL check here.
#define U_DEFINE_LOCAL_OPEN_POINTER(LocalPointerClassName, Type, closeFunction) \
    class /* U_COMMON_API */ LocalPointerClassName : public LocalPointerBase<Type> { \
    public: \
        explicit LocalPointerClassName(Type *p=NULL) : LocalPointerBase<Type>(p) {} \
        ~LocalPointerClassName() { closeFunction(ptr); } \
        void adoptInstead(Type *p) { \
            closeFunction(ptr); \
            ptr=p; \
        } \
    }

#endif // _INTLTEST
