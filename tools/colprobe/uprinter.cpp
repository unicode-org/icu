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
* File uprinter.cpp
*
* Modification History:
*
*   Date        Name        Description
*   03/18/2003  weiv        Creation.
*******************************************************************************
*/

#include "uprinter.h"

UPrinter::UPrinter(FILE *file, const char *locale, const char *encoding, UBool transliterateNonPrintable) {
  _on = true;
  out = u_finit(file, locale, encoding);
  strcpy(_locale, locale);
  if(transliterateNonPrintable) {
    UErrorCode status = U_ZERO_ERROR;
    UTransliterator *anyHex = utrans_open("[^\\u000d\\u000a\\u0009\\u0020-\\u007f] Any-Hex/Java", UTRANS_FORWARD, nullptr, 0, nullptr, &status);
    u_fsettransliterator(out, U_WRITE, anyHex, &status);
  }
};

UPrinter::UPrinter(const char *name, const char *locale, const char *encoding, UTransliterator *trans, UBool transliterateNonPrintable) {
  _on = true;
  out = u_fopen(name, "wb", locale, encoding);
  u_fputc(0xFEFF, out); // emit a BOM
  strcpy(_locale, locale);
  if(transliterateNonPrintable) {
    UErrorCode status = U_ZERO_ERROR;
    if(trans == nullptr) {
      UTransliterator *anyHex = utrans_open("[^\\u000d\\u000a\\u0009\\u0020-\\u007f] Any-Hex/Java", UTRANS_FORWARD, nullptr, 0, nullptr, &status);
      u_fsettransliterator(out, U_WRITE, anyHex, &status);
    } else {
      u_fsettransliterator(out, U_WRITE, trans, &status);
    }
  }
};

UPrinter::~UPrinter() {
  u_fclose(out);
}

void 
UPrinter::log(const UnicodeString &string, UBool nl) {
  if(_on) {
    log(((UnicodeString)string).getTerminatedBuffer(), nl);
  }
}

void 
UPrinter::log(const char16_t *string, UBool nl) {
  if(_on) {
    u_fprintf(out, "%S", string);
    if(nl) {
      u_fprintf(out, "\n");
    }
    u_fflush(out);
  }
}
/*
void 
UPrinter::log(const char *string, UBool nl) {
  if(_on) {
    u_fprintf(out, "%s", string);
    if(nl) {
      u_fprintf(out, "\n");
    }
  }
}
*/
void 
UPrinter::log(const Line *line, UBool nl) {
  if(_on) {
    log(line->name);
    if(line->expLen) {
      log("/");
      log(line->expansionString);
    }
    if(nl) {
      u_fprintf(out, "\n");
      u_fflush(out);
    }
  }
}

void UPrinter::log(const char *fmt, ...)
{
    char16_t buffer[4000];
    va_list ap;

    va_start(ap, fmt);
    /* sprintf it just to make sure that the information is valid */
    u_vsprintf(buffer, _locale, fmt, ap);
    va_end(ap);
    if( _on ) {
        log(buffer);
    }
}

void
UPrinter::on() {
  _on = true;
}

void
UPrinter::off() {
  _on = false;
}
