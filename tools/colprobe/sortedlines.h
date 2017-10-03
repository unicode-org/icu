// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#ifndef COLPROBE_SORTEDLINES_H
#define COLPROBE_SORTEDLINES_H

// colprobe includes
#include "colprobe.h"
#include "line.h"
#include "uprinter.h"
#include "strengthprobe.h"


// ICU includes
#include "unicode/uniset.h"
#include "unicode/usetiter.h"
#include "unicode/uscript.h"
#include "hash.h"

class SortedLines {
  Line empty;
  Line *UB[UCOL_OFF];
  UnicodeSet ignorables[UCOL_OFF];

  Line **toSort;
  int32_t toSortCapacity;
  Line *lines;
  int32_t size;
  int32_t capacity;

  UnicodeSet repertoire;
  UnicodeSet excludeBounds;

  StrengthProbe probe;

  Line *first;
  Line *last;
  Line *current;
  SortedLines() {};

  UPrinter *logger;
  UPrinter *debug;

  Hashtable *contractionsTable;
  Hashtable *duplicators; // elements that duplicate preceding characters
  int32_t maxExpansionPrefixSize;

  // Properties of the sort
  UBool wordSort;
  UBool frenchSecondary;
  UBool upperFirst;

  uint8_t *sortkeys;
  int32_t sortkeyOffset;
public:
  SortedLines(const UnicodeSet &set, const UnicodeSet &excludeBounds, const StrengthProbe &probe, UPrinter *logger, UPrinter *debug);
  SortedLines(FILE *file, UPrinter *logger, UPrinter *debug, UErrorCode &status);
  ~SortedLines();
  void analyse(UErrorCode &status);

  void sort(UBool setStrengths = TRUE, UBool link = FALSE);
  void sort(Line **sortingArray, int32_t sizeToSort, UBool setStrengths = TRUE, UBool link = FALSE);

  Line *getFirst();
  Line *getLast();
  void add(Line *line, UBool linkIn = FALSE);
  void insert(Line *line, int32_t index);
  Line *getNext();
  Line *getPrevious();
  Line *operator[](int32_t index);
  int32_t addContractionsToRepertoire(UErrorCode &status);

  int32_t getSize() const;

  int32_t detectExpansions();
  
  UnicodeString toString(UBool useLinks = FALSE);
  UnicodeString toStringFromEmpty();
  UnicodeString toPrettyString(UBool useLinks, UBool printSortKeys = FALSE);
  UnicodeString toOutput(const char *format, 
                         const char *locale, const char *platform, const char *reference, 
                         UBool useLinks, UBool initialize, UBool moreToCome);
  UnicodeString toBundle(const char *locale, const char *platform, const char *reference, 
                         UBool useLinks, UBool initialize, UBool moreToCome);
  UnicodeString toHTML(const char *locale, const char *platform, const char *reference, 
                       UBool useLinks, UBool initialize, UBool moreToCome);
  UnicodeString toXML(const char *locale, const char *platform, const char *reference, 
                       UBool useLinks, UBool initialize, UBool moreToCome);
  UnicodeString arrayToString(Line** sortedLines, int32_t linesSize, UBool pretty, UBool useLinks, UBool printSortKeys);
  void setSortingArray(Line **sortingArray, Line *elements, int32_t sizeToSort);
  int32_t setSortingArray(Line **sortingArray, Hashtable *table);

  void reduceDifference(SortedLines& reference);
  void getRepertoire(UnicodeSet &fillIn);
  void removeDecompositionsFromRepertoire();
  void getBounds(UErrorCode &status);
  void classifyRepertoire();
  void toFile(FILE *file, UBool useLinks, UErrorCode &status);
  void swapCase();
  void calculateSortKeys();
  void calculateSortKey(Line &line);
private:
  void init();
  void init(UnicodeSet &rep, Line *lin);
  int32_t detectContractions(Line **firstRep, int32_t firstSize,
                                        Line **secondRep, int32_t secondSize,
                                        Line *toAddTo, int32_t &toAddToSize, 
                                        Line *lesserToAddTo, int32_t &lesserToAddToSize,
                                        int32_t capacity, UErrorCode &status);

  void calculateCumulativeStrengths(Line *start, Line *end);
  void transferCumulativeStrength(Line *previous, Line *that);
  void updateBounds(UnicodeSet &set);
  void addAll(Line* toAdd, int32_t toAddSize);
  void setDistancesFromEmpty(Line* array, int32_t arraySize);
  void noteContraction(const char* msg, Line *toAddTo, int32_t &toAddToSize, Line *left, Line *right, int32_t &noConts, UErrorCode &status);
  int32_t gooseUp(int32_t resetIndex, int32_t expansionIndex, Line &expLine, int32_t *expIndexes, int32_t &expIndexSize, UColAttributeValue strength);
  UBool getExpansionLine(const Line &expansion, const Line &previous, const Line &exp, Line &expansionLine);


};

#endif  // #ifndef COLPROBE_SORTEDLINES_H
