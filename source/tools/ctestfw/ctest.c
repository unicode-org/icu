/*  
*****************************************************************************************
*
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*****************************************************************************************
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <stdarg.h>

/* NOTES:
   3/20/1999 srl - strncpy called w/o setting nulls at the end 
 */

#define MAXTESTNAME 128
#define MAXTESTS  512
#define MAX_TEST_LOG 4096

struct TestNode
{
  char name[MAXTESTNAME];
  void (*test)();
  struct TestNode* sibling;
  struct TestNode* child;
};

typedef enum { RUNTESTS, SHOWTESTS } TestMode;
#define TEST_SEPARATOR '/'

#include "unicode/ctest.h"

char ERROR_LOG[MAX_TEST_LOG][MAXTESTNAME];

static TestNode* addTestNode( TestNode *root, const char *name );

static TestNode* createTestNode();

static int strncmp_nullcheck( const char* s1,
                  const char* s2,
                  int n );

static void getNextLevel( const char* name,
              int* nameLen,
              const char** nextName );

static void iterateTestsWithLevel( const TestNode *root, int len, 
                   const TestNode** list,
                   TestMode mode);
/* If we need to make the framework multi-thread safe
   we need to pass around the following 3 vars
*/

static void help ( const char *argv0 );

static int ERRONEOUS_FUNCTION_COUNT = 0;
 int ERROR_COUNT = 0;
static int INDENT_LEVEL = 0;
 int VERBOSITY = 0; /* be No-verbose by default */
 int ERR_MSG =1; /*error messages will be displayed by default*/
/*-------------------------------------------*/

/* strncmp that also makes sure there's a \0 at s2[0] */
int strncmp_nullcheck( const char* s1,
               const char* s2,
               int n )
{
  if ( ( (int)strlen(s2)>=n) && s2[n] != 0 )    return 3; /* null check fails */
  else  return strncmp ( s1, s2, n );
}

void getNextLevel( const char* name,
           int* nameLen,
           const char** nextName )
{
  /* Get the next component of the name */
  *nextName = strchr(name, TEST_SEPARATOR);
  
  if( *nextName != 0 )
    {
      char n[255];
      *nameLen = (*nextName) - name;
      (*nextName)++; /* skip '/' */
      strncpy(n, name, *nameLen);
      n[*nameLen] = 0;
      /*      printf("->%s-< [%d] -> [%s]\n", name, *nameLen, *nextName);*/
    }
  else    *nameLen = strlen(name);  
}

TestNode *createTestNode( )
{
  TestNode *newNode;
  
  newNode = (TestNode*)malloc ( sizeof ( TestNode ) );
  
  newNode->name[0]  = '\0';
  newNode->test = NULL;
  newNode->sibling = NULL;
  newNode->child = NULL;    
  
  return  newNode;
}

void addTest ( TestNode** root,
           TestFunctionPtr test,
           const char* name )
{
  TestNode *newNode;
  
  /*if this is the first Test created*/
  if (*root == NULL) *root = createTestNode();
  
  newNode = addTestNode( *root, name );
  assert(newNode != 0 );
  /*  printf("addTest: nreName = %s\n", newNode->name );*/
  
  newNode->test = test;
}

/* non recursive insert function */
TestNode *addTestNode ( TestNode *root, const char *name )
{
  const char* nextName;
  TestNode *nextNode, *curNode;
  int nameLen; /* length of current 'name' */
  
  /* remove leading slash */
  if ( *name == TEST_SEPARATOR )  name++;    
  
  curNode = root;
  
  for(;;)
    {
      /* Start with the next child */
      nextNode = curNode->child;
      
      getNextLevel ( name, &nameLen, &nextName );
      
      /*      printf("* %s\n", name );*/
      
      /* if nextNode is already null, then curNode has no children
     -- add them */
      if( nextNode == NULL )
    {
      /* Add all children of the node */
      do
        {
          curNode->child = createTestNode ( );
          
          /* Get the next component of the name */
          getNextLevel ( name, &nameLen, &nextName );
          
          /* update curName to have the next name segment */
          strncpy ( curNode->child->name , name, nameLen );
          curNode->child->name[nameLen] = 0;
          /* printf("*** added %s\n", curNode->child->name );*/
          curNode = curNode->child;
          name = nextName;
        }
      while( name != NULL );
      
      return curNode;
    }
      
      /* Search across for the name */
      while (strncmp_nullcheck ( name, nextNode->name, nameLen) != 0 )
    {
      curNode = nextNode;
      nextNode = nextNode -> sibling;
      
      if ( nextNode == NULL )
        {
          /* Did not find 'name' on this level. */
          nextNode = createTestNode ( );
          strncpy( nextNode->name, name, nameLen );
          nextNode->name[nameLen] = 0;
          curNode->sibling = nextNode;
          break;
        }
    }
      
      /* nextNode matches 'name' */
      
      if (nextName == NULL) /* end of the line */
    {
      return nextNode;
    }
      
      /* Loop again with the next item */
      name = nextName;
      curNode = nextNode;
    }
}

void iterateTestsWithLevel ( const TestNode* root,
                 int len, 
                 const TestNode** list,
                 TestMode mode)
{
  int i;
  int saveIndent;

  char pathToFunction[MAXTESTNAME] = "";
  char separatorString[2] = { TEST_SEPARATOR, '\0'};

  if ( root == NULL )    return;

  list[len++] = root;       

  for ( i=0;i<(len-1);i++ )
    {
      strcat(pathToFunction, list[i]->name);
      strcat(pathToFunction, separatorString);
    }

  strcat(pathToFunction, list[i]->name);

  INDENT_LEVEL = len;
  if ( (mode == RUNTESTS) && (root->test != NULL)) 
    {
      int myERROR_COUNT = ERROR_COUNT;
      root->test();
      if (myERROR_COUNT != ERROR_COUNT)
    {

      log_info("---[%d ERRORS] ", ERROR_COUNT - myERROR_COUNT);
      strcpy(ERROR_LOG[ERRONEOUS_FUNCTION_COUNT++], pathToFunction);
    }
      else log_info("---[OK] ");
    }


  /* we want these messages to be at 0 indent. so just push the indent level breifly. */
  saveIndent = INDENT_LEVEL;
  INDENT_LEVEL = 0;
  log_info("%s%s%c\n", (list[i]->test||mode==SHOWTESTS)?"---":"",pathToFunction, list[i]->test?'\0':TEST_SEPARATOR );
  INDENT_LEVEL = saveIndent;

  iterateTestsWithLevel ( root->child, len, list, mode );

  len--;

  if ( len != 0 ) /* DO NOT iterate over siblings of the root. */
      iterateTestsWithLevel ( root->sibling, len, list, mode );
}



void showTests ( const TestNode *root )
{
  /* make up one for them */
  const TestNode *aList[MAXTESTS];

  if (root == NULL) log_err("TEST CAN'T BE FOUND!");
  
  iterateTestsWithLevel ( root, 0, aList, SHOWTESTS );
  
}

void runTests ( const TestNode *root )
{
  int i;
  const TestNode *aList[MAXTESTS];
  /* make up one for them */
  

  if (root == NULL) log_err("TEST CAN'T BE FOUND!\n");

  ERRONEOUS_FUNCTION_COUNT = ERROR_COUNT = 0;  
  iterateTestsWithLevel ( root, 0, aList, RUNTESTS );
  
  /*print out result summary*/
  
  if (ERROR_COUNT)
    {
      log_info("\nSUMMARY:\n******* [Total error count:\t%d]\n Errors in\n", ERROR_COUNT);
      for (i=0;i < ERRONEOUS_FUNCTION_COUNT; i++) log_info("[%s]\n",ERROR_LOG[i]);
    }
  else 
    {
      log_info("\n[All tests passed successfully...]\n");
    }

}

const TestNode* getTest(const TestNode* root, const char* name)
{
  const char* nextName;
  TestNode *nextNode;
  const TestNode* curNode;
  int nameLen; /* length of current 'name' */
  
  if (root == NULL) log_err("TEST CAN'T BE FOUND!\n");
  /* remove leading slash */
  if ( *name == TEST_SEPARATOR )  name++;    
  
  curNode = root;
  
  for(;;)
    {
      /* Start with the next child */
      nextNode = curNode->child;
      
      getNextLevel ( name, &nameLen, &nextName );
      
      /*      printf("* %s\n", name );*/
      
      /* if nextNode is already null, then curNode has no children
     -- add them */
      if( nextNode == NULL )
    {
      return NULL;
    }
      
      /* Search across for the name */
      while (strncmp_nullcheck ( name, nextNode->name, nameLen) != 0 )
    {
      curNode = nextNode;
      nextNode = nextNode -> sibling;
      
      if ( nextNode == NULL )
        {
          /* Did not find 'name' on this level. */
          return NULL;
        }
    }
      
      /* nextNode matches 'name' */
      
      if (nextName == NULL) /* end of the line */
    {
      return nextNode;
    }
      
      /* Loop again with the next item */
      name = nextName;
      curNode = nextNode;
    }
}

void log_err(const char* pattern, ...)
{
  va_list ap;
  if( ERR_MSG == FALSE){
      ERROR_COUNT++;
      return;
  }
  va_start(ap, pattern);
  fprintf(stdout, "%-*s", INDENT_LEVEL," " );
  vfprintf(stderr, pattern, ap);
  va_end(ap);
  
  ERROR_COUNT++;
}

void log_info(const char* pattern, ...)
{
  va_list ap;
  
  va_start(ap, pattern);
  fprintf(stdout, "%-*s", INDENT_LEVEL," " );
  vfprintf(stdout, pattern, ap);
  va_end(ap);
}

void log_verbose(const char* pattern, ...)
{
  va_list ap;
  
  if ( VERBOSITY == FALSE )
    return;

  va_start(ap, pattern);
  fprintf(stdout, "%-*s", INDENT_LEVEL," " );
  vfprintf(stdout, pattern, ap);
  va_end(ap);
}




int processArgs(const TestNode* root,
             int argc,
             const char** argv)
{
/**
 * This main will parse the l, v, h, n, and path arguments
 */
  const TestNode*    toRun;
  int                i;
  int                doList = FALSE;
  int                runAll = FALSE;
  int                           subtreeOptionSeen = FALSE;

  int                           errorCount = 0;
  
  toRun = root;
  VERBOSITY = FALSE;
  ERR_MSG = TRUE;

  for( i=1; i<argc; i++)
  {
    if ( argv[i][0] == '/' )
    {
        printf("Selecting subtree '%s'\n", argv[i]);

        if ( argv[i][1] == 0 )
            toRun = root;
        else
            toRun = getTest(root, argv[i]);

        if ( toRun == NULL )
        {
            printf("* Could not find any matching subtree\n");
            return -1;
        }

        if( doList == TRUE)
          showTests(toRun);
        else if( runAll == TRUE)
          runTests(toRun);
        else
          runTests(toRun);

        errorCount += ERROR_COUNT;

        subtreeOptionSeen = TRUE;
    }
    else if (strcmp( argv[i], "-v" )==0 )
    {
        VERBOSITY = TRUE;
    }
    else if (strcmp( argv[i], "-verbose")==0 )
    {
        VERBOSITY = TRUE;
    }
    else if (strcmp( argv[i], "-l" )==0 )
    {
        doList = TRUE;
    }
    else if (strcmp( argv[i], "-all") ==0)
    {
        runAll = TRUE;
    }
    else if(strcmp( argv[i], "-a") == 0)
    {
        runAll = TRUE;
    }
    else if(strcmp( argv[i], "-n") == 0)
    {
        ERR_MSG = FALSE;
    }
    else if (strcmp( argv[i], "-no_err_msg") == 0)
    {
        ERR_MSG = FALSE;
    }
    else if (strcmp( argv[1], "-h" )==0 )
    {
        help( argv[0] );
        return 0;
    }
    else
    {
        printf("* unknown option: %s\n", argv[i]);
        help( argv[0] );
        return -1;
    }
  }

  if( subtreeOptionSeen == FALSE) /* no other subtree given, run the default */
    {
      if( doList == TRUE)
    showTests(toRun);
      else if( runAll == TRUE)
    runTests(toRun);
      else
    runTests(toRun);
      
      errorCount += ERROR_COUNT;
    }
  else
    {
      if( ( doList == FALSE ) && ( errorCount > 0 ) )
    printf(" Total errors: %d\n", errorCount );
    }

  return errorCount; /* total error count */
}

/**
 * Display program invocation arguments
 */

void help ( const char *argv0 )
{
    printf("Usage: %s [ -l ] [ -v ] [ -verbose] [-a] [ -all] [-n] \n [ -no_err_msg] [ -h ] [ /path/to/test ]\n",
            argv0);
    printf("    -l To get a list of test names\n");
    printf("    -all To run all the test\n");
    printf("    -a To run all the test(same a -all)\n");
    printf("    -verbose To turn ON verbosity\n");
    printf("    -v To turn ON verbosity(same as -verbose)\n");
    printf("    -h To print this message\n");
    printf("    -n To turn OFF printing error messages\n");
    printf("    -no_err_msg (same as -n) \n");
    printf("    -[/subtest] To run a subtest \n");
    printf("    eg: to run just the utility tests type: cintltest /tsutil) \n");
}

