/*  
*****************************************************************************************
*
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*****************************************************************************************
*/


#ifndef CTEST_H
#define CTEST_H


/*Deals with imports and exports of the dynamic library*/
#ifdef _WIN32
    #define T_CTEST_EXPORT __declspec(dllexport)
    #define T_CTEST_IMPORT __declspec(dllimport)
#else
    #define T_CTEST_EXPORT
    #define T_CTEST_IMPORT
#endif

#ifdef __cplusplus
    #define C_CTEST_API extern "C"
#else
    #define C_CTEST_API
#endif 

#ifdef T_CTEST_IMPLEMENTATION
    #define T_CTEST_API C_CTEST_API  T_CTEST_EXPORT
    #define T_CTEST_EXPORT_API T_CTEST_EXPORT
#else
    #define T_CTEST_API C_CTEST_API  T_CTEST_IMPORT
    #define T_CTEST_EXPORT_API T_CTEST_IMPORT
#endif



/* True and false for sanity. (removes ICU dependancy) */

#ifndef FALSE
#define FALSE 0
#endif
#ifndef TRUE
#define TRUE 1
#endif




/* prototypes *********************************/

typedef void (*TestFunctionPtr)();
typedef struct TestNode TestNode;

/**
 * Count of errors from all tests. 
 * May be reset.
 * @internal Internal APIs for testing purpose only
 */
T_CTEST_EXPORT_API extern int ERROR_COUNT;

/**
 * Set this to zero to disable log_verbose() messages.
 * Otherwise nonzero to see log_verbose() messages.
 * @internal Internal APIs for testing purpose only
 *
 */
T_CTEST_EXPORT_API extern int VERBOSITY;  

/**
 * Set this to zero to disable log_verbose() messages.
 * Otherwise nonzero to see log_verbose() messages.
 * @internal Internal APIs for testing purpose only
 *
 */
T_CTEST_EXPORT_API extern int ERR_MSG; 

/**
 * Show the names of all nodes.
 * 
 * @param root Subtree of tests.
 * @internal Internal APIs for testing purpose only
 */
T_CTEST_API void showTests ( const TestNode *root);

/**
 * Run a subtree of tests.
 * 
 * @param root Subtree of tests.
 * @internal Internal APIs for testing purpose only
 */
T_CTEST_API void runTests ( const TestNode* root);

/**
 * Add a test to the subtree.
 * Example usage:
 * <PRE>
 *     TestNode* root=NULL;
 *     addTest(&root, &mytest, "/a/b/mytest" );
 * </PRE>
 * @param root Pointer to the root pointer.
 * @param test Pointer to 'void function(void)' for actual test.
 * @param path Path from root under which test will be placed. Ex. '/a/b/mytest'
 * @internal Internal APIs for testing purpose only
 */
T_CTEST_API void addTest ( TestNode** root,
           TestFunctionPtr test,
           const char *path);

/**
 * Retreive a specific subtest. (subtree).
 *
 * @param root Pointer to the root.
 * @param path Path relative to the root, Ex. '/a/b'
 * @return The subtest, or NULL on failure.
 * @internal Internal APIs for testing purpose only
 */
T_CTEST_API const TestNode* getTest (const TestNode* root,
                                     const char *path);


/**
 * Log an error message. (printf style)
 * @param pattern printf-style format string
 * @internal Internal APIs for testing purpose only
 */
T_CTEST_API void log_err(const char* pattern, ...);

/**
 * Log an informational message. (printf style)
 * @param pattern printf-style format string
 * @internal Internal APIs for testing purpose only
 */
T_CTEST_API void log_info(const char* pattern, ...);

/**
 * Log a verbose informational message. (printf style)
 * This message will only appear if the global VERBOSITY is nonzero
 * @param pattern printf-style format string
 * @internal Internal APIs for testing purpose only
 */
T_CTEST_API void log_verbose(const char* pattern, ...);

/**
 * Processes the command line arguments.
 * This is a sample implementation
 * <PRE>Usage: %s [ -l ] [ -v ] [ -? ] [ /path/to/test ]
 *        -l List only, do not run\
 *        -v turn OFF verbosity
 *        -? print this message</PRE>
 * @param root Testnode root with tests already attached to it
 * @param argv argument list from main (stdio.h)
 * @param argc argument list count from main (stdio.h)
 * @return positive for error count, 0 for success, negative for illegal argument
 * @internal Internal APIs for testing purpose only
 */

T_CTEST_API int processArgs(const TestNode* root,
                             int argc,
                             const char** argv);




#endif
