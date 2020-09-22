/*
*******************************************************************************
*
*   © 2020 and later: Unicode, Inc. and others.
*   License & terms of use: http://www.unicode.org/copyright.html
*
*******************************************************************************
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unicode/brkiter.h>
#include <unicode/uniset.h>

#include <brkeng.h>
#include <localsvc.h>
#include <uoptions.h>
#include <uvectr32.h>

using namespace icu;

// Command line option variables
//     These global variables are set according to the options specified
//     on the command line by the user.
char * opt_del       = "|";
int    opt_engine    = 0;
char * opt_locale    = "en_US";
char * opt_text      = 0;
char * opt_type      = "word";
bool   opt_help      = false;

void printUnicodeString(const UnicodeString &s) {
    char charBuf[1000];
    s.extract(0, s.length(), charBuf, sizeof(charBuf)-1, 0);
    charBuf[sizeof(charBuf)-1] = 0;
    printf("%s", charBuf);
}


void printTextRange( BreakIterator& iterator,
                    int32_t start, int32_t end )
{
    std::unique_ptr<CharacterIterator> strIter(iterator.getText().clone());
    UnicodeString  s;
    strIter->getText(s);

    printf(" %ld %ld\t", (long)start, (long)end);
    printUnicodeString(UnicodeString(s, 0, start));
    printf("%s", opt_del);
    printUnicodeString(UnicodeString(s, start, end-start));
    printf("%s", opt_del);
    printUnicodeString(UnicodeString(s, end));
    puts("");
}


/* Print each element in order: */
void printEachForward( BreakIterator& boundary)
{
    int32_t start = boundary.first();
    for (int32_t end = boundary.next();
         end != BreakIterator::DONE;
         start = end, end = boundary.next())
    {
        printTextRange( boundary, start, end );
    }
}

/* Print each element in reverse order: */
void printEachBackward( BreakIterator& boundary)
{
    int32_t end = boundary.last();
    for (int32_t start = boundary.previous();
         start != BreakIterator::DONE;
         end = start, start = boundary.previous())
    {
        printTextRange( boundary, start, end );
    }
}

const char gUsageString[] =
 "usage:  %s options...\n"
    "-help                            Display this message.\n"
    "-delimiter text                  Delimiter to show the break position, default to '|'.\n"
    "-engine num                      Engine number, defaul to 0 - the ICU curren implementation.\n"
    "-locale name                     ICU locale to use.  Default is en_US.\n"
    "-type [word|line|sentence|char]  Break type, default to 'word'.\n"
    "-text text                       Text in UTF8.\n"
    ;

//
//   Definitions for the command line options
//
struct OptSpec {
    const char *name;
    enum {FLAG, NUM, STRING} type;
    void *pVar;
};

OptSpec opts[] = {
    {"-delimiter",   OptSpec::STRING, &opt_del},
    {"-engine",      OptSpec::NUM,    &opt_engine},
    {"-locale",      OptSpec::STRING, &opt_locale},
    {"-text",        OptSpec::STRING, &opt_text},
    {"-type",        OptSpec::STRING, &opt_type},
    {"-help",        OptSpec::FLAG,   &opt_help},
    {"-?",           OptSpec::FLAG,   &opt_help},
    {0, OptSpec::FLAG, 0}
};


bool ProcessOptions(int argc, const char **argv, OptSpec opts[])
{
    const char  *pArgName;
    OptSpec    *pOpt;

    for (int argNum=1; argNum<argc; argNum++) {
        pArgName = argv[argNum];
        for (pOpt = opts;  pOpt->name != 0; pOpt++) {
            if (strcmp(pOpt->name, pArgName) == 0) {
                switch (pOpt->type) {
                    case OptSpec::FLAG:
                        *(bool *)(pOpt->pVar) = true;
                        break;
                    case OptSpec::STRING:
                        argNum ++;
                        if (argNum >= argc) {
                            fprintf(stderr, "value expected for \"%s\" option.\n", pOpt->name);
                            return false;
                        }
                        *(const char **)(pOpt->pVar)  = argv[argNum];
                        break;
                    case OptSpec::NUM:
                        argNum ++;
                        if (argNum >= argc) {
                            fprintf(stderr, "value expected for \"%s\" option.\n", pOpt->name);
                            return false;
                        }
                        char *endp;
                        int i = strtol(argv[argNum], &endp, 0);
                        if (endp == argv[argNum]) {
                            fprintf(stderr, "integer value expected for \"%s\" option.\n", pOpt->name);
                            return false;
                        }
                        *(int *)(pOpt->pVar) = i;
                    }
                break;
            }
        }
        if (pOpt->name == 0)
        {
            fprintf(stderr, "Unrecognized option \"%s\"\n", pArgName);
            return false;
        }
    }
    return true;
}

// Just for demo purpose, this class always brak before char
// U+0E00-U+0E0F
class MyThaiLanguageBreakEngine1 : public LanguageBreakEngine {
 public:
  MyThaiLanguageBreakEngine1(UErrorCode status) {
     fSet.applyPattern(UNICODE_STRING_SIMPLE("[[:Thai:]&[:LineBreak=SA:]]"), status);
     fSet.compact();
  }
  virtual ~MyThaiLanguageBreakEngine1() {
  }
  virtual UBool handles(UChar32 c) const {
      return fSet.contains(c);
  }
  virtual int32_t findBreaks( UText *text,
                              int32_t,
                              int32_t endPos,
                              UVector32 &foundBreaks ) const {
      int32_t current;
      UErrorCode status = U_ZERO_ERROR;
      UChar32 c = utext_current32(text);
      while((current = (int32_t)utext_getNativeIndex(text)) < endPos && fSet.contains(c)) {
          utext_next32(text);
          c = utext_current32(text);
          if (c >= 0x0E00 && c <= 0x0E0F) {
              foundBreaks.push((int32_t)utext_getNativeIndex(text), status);
          }
      }
      utext_setNativeIndex(text, current);
      return  foundBreaks.size();
  }
 private:
  UnicodeSet    fSet;
};

// Just for demo purpose, this class always brak before char
// U+0E10-U+0E2F
class MyThaiLanguageBreakEngine2 : public LanguageBreakEngine {
 public:
  MyThaiLanguageBreakEngine2(UErrorCode status) {
     fSet.applyPattern(UNICODE_STRING_SIMPLE("[[:Thai:]&[:LineBreak=SA:]]"), status);
     fSet.compact();
  }
  virtual ~MyThaiLanguageBreakEngine2() {
  }
  virtual UBool handles(UChar32 c) const {
      return fSet.contains(c);
  }
  virtual int32_t findBreaks( UText *text,
                              int32_t,
                              int32_t endPos,
                              UVector32 &foundBreaks ) const {
      int32_t current;
      UErrorCode status = U_ZERO_ERROR;
      UChar32 c = utext_current32(text);
      while((current = (int32_t)utext_getNativeIndex(text)) < endPos && fSet.contains(c)) {
          utext_next32(text);
          c = utext_current32(text);
          if (c >= 0x0E10 && c <= 0x0E2F) {
              foundBreaks.push((int32_t)utext_getNativeIndex(text), status);
          }
      }
      utext_setNativeIndex(text, current);
      return  foundBreaks.size();
  }
 private:
  UnicodeSet    fSet;
};

class MyLanguageBreakFactory : public LanguageBreakFactory {
 public:
  MyLanguageBreakFactory(UErrorCode status)
      : engine1(status), engine2(status) {
  }
  virtual ~MyLanguageBreakFactory() {
  }
  virtual const LanguageBreakEngine *getEngineFor(UChar32 c) {
      UErrorCode status = U_ZERO_ERROR;
      UScriptCode code = uscript_getScript(c, &status);
      if (code == USCRIPT_THAI) {
          switch(opt_engine) {
              case 1:
                  return &engine1;
              case 2:
                  return &engine2;
          }
      }
      return nullptr;
  }
 public:
  MyThaiLanguageBreakEngine1 engine1;
  MyThaiLanguageBreakEngine2 engine2;
};

void* my_svc_hook(const char *what, UErrorCode *status) {
    static MyLanguageBreakFactory myfactory(*status);
    if (strcmp(what, "languageBreakFactory") == 0) {
        return (void*) &myfactory;
    }
    return nullptr;
}

/* Creating and using text boundaries */
int main( int argc, const char** argv )
{
    if (!ProcessOptions(argc, argv, opts) || opt_help) {
        fprintf(stderr, gUsageString, argv[0]);
        exit (1);
    }

    printf("Delimiter: %s\n", opt_del);
    printf("Engine: %d\n", opt_engine);
    if (opt_engine > 0) {
#if defined(U_LOCAL_SERVICE_HOOK) && U_LOCAL_SERVICE_HOOK
        uprv_setup_svc_hook(my_svc_hook);
#else
        printf("Program should be configure with U_LOCAL_SERVICE_HOOK\n");
        printf("Configure your ICU build as command as the following way:\n");
        printf("CXXFLAGS=\"-DU_LOCAL_SERVICE_HOOK\" ./runConfigureICU  ....\n");
        return (-1);
#endif
    }

    printf("ICU Break Iterator Sample Program\n");
    printf("Locale: %s\n", opt_locale);
    Locale locale(opt_locale);

    UnicodeString stringToExamine(u"นายสนธิญากล่าวอีกว่า");
    if (opt_text == 0) {
        printf("No text specified, use hard coded text\n");
    } else {
        stringToExamine = UnicodeString::fromUTF8(opt_text);
    }
    printf("Text: ");
    printUnicodeString(stringToExamine);
    printf("\n");

    UErrorCode status = U_ZERO_ERROR;
    std::unique_ptr<BreakIterator> boundary;
    if (strcmp("word", opt_type) == 0) {
        boundary.reset(BreakIterator::createWordInstance(locale, status));
    } else if (strcmp("line", opt_type) == 0) {
        boundary.reset(BreakIterator::createLineInstance(locale, status));
    } else if (strcmp("sentence", opt_type) == 0) {
        boundary.reset(BreakIterator::createSentenceInstance(locale, status));
    } else if (strcmp("char", opt_type) == 0) {
        boundary.reset(BreakIterator::createCharacterInstance(locale, status));
    } else {
        fprintf(stderr, "Unrecognized type \"%s\"\n", opt_type);
        fprintf(stderr, gUsageString, argv[0]);
        exit(1);
    }
    if (U_FAILURE(status)) {
        fprintf(stderr, "failed to create word break iterator.  status = %s",
            u_errorName(status));
        exit(1);
    }

    printf("Type: %s\n", opt_type);

    boundary->setText(stringToExamine);
    printf("----- forward: -----------\n");
    printEachForward(*boundary);
    printf("----- backward: ----------\n");
    printEachBackward(*boundary);

    return 0;
}
