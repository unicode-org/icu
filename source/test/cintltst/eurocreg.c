#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "ctest.h"
#include "unicode/ucnv.h"
#include <stdio.h>

void TestEuroRegression(void);
void addTestEuroRegression(TestNode** root)
{
    addTest(root, &TestEuroRegression, "tsconv/eurocreg/TestEuroRegression");
}

char convertersToCheck[][15] = { 
  "cp1250",
  "cp1251",
  "cp1252",
  "cp1254",
  "cp1255",
  "cp1256",
  "cp1257",
  "cp1258",
  "cp37",
  "cp273",
  "cp277",
  "cp278",
  "cp280",
  "cp284",
  "cp285",
  "cp297",
  "cp500",
  "cp871",
  "cp850",
  "cp500",
  "cp850",
  "cp870",
  "cp1026",
  "cp857",
  "cp1025",
  "cp1123",
  "cp1122",
  "cp1112",
  "cp424",
  "cp803",
  "cp862",
  "cp9030",
  "cp1130",
  "cp1258",
  "cp290",
  "cp930",
  "cp950",
  "cp937",
  "cp1253",
  /*  "cp819",
      "cp13488",*/
  "cp875",
  "cp869",
  "cp813",
  "cp852",
  "cp855",
  "cp866",
  "cp1131",
  "cp1125",
  "cp922",
  "cp921",
  "cp420",
  "cp864",
  "cp1008",
  "cp1046",
  /*  "cp9066",
      "cp1129",*/
  "cp1027",
  "cp300",
  "cp939",
  /*  "cp4930",
      "cp1364",*/
  "cp1362",
  "cp1363",
  "cp1114",
  "cp947",
  "cp28709",
  "cp835",
  ""};

bool_t isEuroAware(const UConverter*);

void TestEuroRegression()
{
  int32_t i=0;
  
  do 
    {
      UErrorCode err = U_ZERO_ERROR;
      UConverter* myConv =  ucnv_open(convertersToCheck[i], &err);
      if (U_FAILURE(err)&&convertersToCheck[i][0]) log_err("%s  \tMISSING [%s]\n", convertersToCheck[i], u_errorName(err));
      else 
	{
	  if (isEuroAware(myConv)) log_verbose("%s  \tsupports euro\n", convertersToCheck[i]);
	  else log_err("%s  \tDOES NOT support euro\n", convertersToCheck[i]);
	  ucnv_close(myConv);
	}
    } while (convertersToCheck[++i][0]);
}

bool_t isEuroAware(const UConverter* myConv)
{
  static const UChar euroString[2] = { 0x20AC, 0x0000 };
  char target[2];
  UChar euroBack[2];
  int32_t targetSize, euroBackSize;
  UErrorCode err = U_ZERO_ERROR;
  const char* myName =   ucnv_getName(myConv, &err);

  targetSize = ucnv_fromUChars(myConv,
	  target,
			       2,
			       euroString,
			       &err);
  if (U_FAILURE(err))
    {
      log_err("Failure Occured in ucnv_fromUChars euro roundtrip test\n");
      return FALSE;
    }
  euroBackSize = ucnv_toUChars(myConv,
			       euroBack,
			       2,
			       target,
			       targetSize,
			       &err);
  if (U_FAILURE(err))
    {
      log_err("Failure Occured in ucnv_toUChars euro roundtrip test\n");
      return FALSE;
    }
  if (u_strcmp(euroString, euroBack)) 
    {
      /*      log_err("%s FAILED Euro rountrip\n", myName);*/
      return FALSE;
    }
  else 
    {
      /*      log_verbose("%s PASSED Euro rountrip\n", myName);*/
      return TRUE;
    }
		
}
