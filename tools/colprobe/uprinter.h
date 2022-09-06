// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File uprinter.h
*
* Modification History:
*
*   Date        Name        Description
*   03/18/2003  weiv        Creation.
*******************************************************************************
*/

#ifndef COLPROBE_UPRINTER_H
#define COLPROBE_UPRINTER_H

#include "line.h"

#include "unicode/ustdio.h"
#include "unicode/unistr.h"
#include "unicode/ustring.h"


class UPrinter {
  UFILE *out;
  UChar buffer[256];
  UBool _on;
  char _locale[256];
public:
  UPrinter(FILE *file, const char *locale, const char *encoding, UBool transliterateNonPrintable=true);
  UPrinter(const char *name, const char *locale, const char *encoding, UTransliterator *trans, UBool transliterateNonPrintable);
  ~UPrinter();
  void log(const UnicodeString &string, UBool nl = false);
  void log(const UChar *string, UBool nl = false);
  //void log(const char *string, UBool nl = false);
  void log(const Line *line, UBool nl = false);
  void log(const char *fmt, ...);
  void off(void);
  void on(void);
  UBool isOn(void) {
    return _on;
  };
};



#endif // #ifndef COLPROBE_UPRINTER_H
