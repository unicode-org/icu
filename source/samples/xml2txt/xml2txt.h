/******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ******************************************************************************/
#ifndef xml2txt_H_
#define xml2txt_H_

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#ifdef WIN32
#include <direct.h>
#else
#include <unistd.h>
#endif

#include <process.h> 
#include <errno.h> 


#include "uoptions.h"
#include "toolutil.h"
#include "ChildName.h"
#include "DOMPrintFormatTarget.h"

//#define UOPTION_TXT              UOPTION_DEF("txt", 't', UOPT_NO_ARG)
//#define UOPTION_RES              UOPTION_DEF("res", 'r', UOPT_NO_ARG)

void     usage();
void	 InitParser();
void	 recycle();
int		 ProcessTxtFile();
void ErrorReport(DOM_Node& towrite, int ErrorType);
void Check(DOM_Node &document);
ostream& operator<<(ostream& target, const DOMString& toWrite);
ostream& operator<<(ostream& target, DOM_Node& toWrite);
XMLFormatter& operator<< (XMLFormatter& strm, const DOMString& s);
char* CreateTxtName(const char* arg, const char* Dir);
char* CreateFile(const char* arg, const char* Dir);
DOMString CheckIntvector(DOMString attributeVal, DOM_Node document);
void CheckInt(DOMString attributeVal, DOM_Node document);
void CheckBin(DOMString attributeVal, DOM_Node document);
unsigned int GetCNodeNum(DOM_Node document);
ChildName* CheckNameDuplicate(DOM_Node document, ChildName* cn);
DOMString getAttributeKey(DOM_Node CNode);
void DelChildName(ChildName* cn);
void CheckEscape(DOM_NamedNodeMap attributes, DOMString attributeVal, int item_num);

#endif
