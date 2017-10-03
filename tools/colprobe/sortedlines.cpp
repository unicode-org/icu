// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#include "sortedlines.h"

static int codePointCmp(const void *a, const void *b) {
  return u_strcmp((*(Line **)a)->name, (*(Line **)b)->name);
}

SortedLines::SortedLines(const UnicodeSet &set, const UnicodeSet &excludeBounds, const StrengthProbe &probe, 
                         UPrinter *logger, UPrinter *debug) :
toSort(NULL),
toSortCapacity(0),
lines(NULL),
size(0),
capacity(0),
repertoire(set),
excludeBounds(excludeBounds),
probe(probe),
first(NULL),
last(NULL),
logger(logger),
debug(debug),
contractionsTable(NULL),
duplicators(NULL),
maxExpansionPrefixSize(0),
wordSort(FALSE),
frenchSecondary(FALSE),
upperFirst(FALSE),
sortkeys(NULL),
sortkeyOffset(0)
{
  memset(UB, 0, sizeof(UB));
  int32_t i = 0;
  for(i = 0; i < UCOL_OFF; i++) {
    UB[i] = &empty;
  }
  init();
}

SortedLines::~SortedLines()
{
  delete[] lines;
  if(sortkeys) {
    delete[] sortkeys;
  }
  if(toSort) {
    delete[] toSort;
  }
  if(contractionsTable) {
    delete contractionsTable;
  }
  if(duplicators) {
    delete duplicators;
  }
}

void
SortedLines::getBounds(UErrorCode &status) {
  // first sort through the set
  debug->log(toString(), TRUE);
  int32_t i = 0, j = 0;
  UColAttributeValue strength = UCOL_OFF;
  for(i = 0; i < size; i++) {
    if(toSort[i]->strengthFromEmpty < strength) {
      if(i && strength < UCOL_OFF) {
        //u_strcpy(UB[strength], toSort[i-1]->name);
        j = 1;
        while(excludeBounds.contains(UnicodeString(toSort[i-j]->name, toSort[i-j]->len))) {
          j++;
        }
        UB[strength] = toSort[i-j];
      }
      strength = toSort[i]->strengthFromEmpty;
      if(strength == UCOL_PRIMARY) {
        probe.SE = toSort[i]->name[0];
      }
    }
  }
  //u_strcpy(UB[strength], toSort[size-1]->name);
  // a different solution for bounds: go from end and see if the guys on the top
  // cause duplication for things
  UChar dupch[] = { 0x0020, 0x0030, 0x0042, 0x0051, 0x0062, 0x0071, 0x0391, 0x0396, 0x03b1, 0x03b6 };
  j = 1;
  Line dup;
  Line bound;
  int32_t dups = 0;
  while(j < size) {
    dups = 0;
    for(i = 0; i < sizeof(dupch)/sizeof(dupch[0]); i++) {
      dup.setTo(dupch[i]);
      dup.append(dupch[i]);
      bound.setTo(dupch[i]);
      bound.append(toSort[size-j]->name, toSort[size-j]->len);
      if(probe.getStrength(dup, bound) >= UCOL_IDENTICAL) {
        dups++;
      }
    }
    if(dups == 0) {
      break;
    } else {
      if(!duplicators) {
        duplicators = new Hashtable();
      }
      duplicators->put(UnicodeString(toSort[size-j]->name, toSort[size-j]->len), &toSort[size-j], status);
      debug->log(toSort[size-j]->toString());
      debug->log(" is not good enough to be an upper bound\n");
      j++;
    }
  }
  if(j == size) {
    debug->log("Oi! I'm hallucinating. Will use the first upper bound");
    delete duplicators;
    duplicators = NULL;
    j = 1;
  }
/*
  j = 1;
  while(excludeBounds.contains(UnicodeString(toSort[size-j]->name, toSort[size-j]->len))) {
    j++;
  }
*/
  UB[strength] = toSort[size-j];
  for(i = 0; i < UCOL_OFF; i++) {
    if(UB[i]) {
      //debug->log(UB[i], TRUE);
      debug->log(UB[i]->toString(TRUE), TRUE);
    }
  }
}

// classifies repertoire according to the strength of their difference
// from the empty string
void
SortedLines::classifyRepertoire() {
  UColAttributeValue strongestStrengthFromEmpty = UCOL_OFF;
  int32_t lastChange = 0;
  int32_t i = 0, j = 0;
  while(i < size) // && probe.distanceFromEmptyString(*toSort[i]) > UCOL_PRIMARY) 
  {
    toSort[i]->strengthFromEmpty = probe.distanceFromEmptyString(*toSort[i]);
    if(toSort[i]->strengthFromEmpty < strongestStrengthFromEmpty) {
      strongestStrengthFromEmpty = toSort[i]->strengthFromEmpty;
      lastChange = i;
    } else if (toSort[i]->strengthFromEmpty > strongestStrengthFromEmpty) { 
      // there is a problem in detection. Most probably a quaternary.
      // why don't we try to interpolate
      UColAttributeValue nextStrength = UCOL_OFF;
      UColAttributeValue prevStrength = UCOL_OFF;
      UColAttributeValue st = UCOL_OFF;

      logger->log("Interpolating to get the distance from empty for Line ");
      logger->log(toSort[i]->toString(TRUE), TRUE);

      if(i) {
        st = probe.getStrength(*toSort[i-1], *toSort[i]);
        if(st == UCOL_OFF) {
          logger->log("Cannot deduce distance from empty using previous element. Something is very wrong! Line:");
          logger->log(toSort[i]->toString(TRUE), TRUE);
        } else if(st == UCOL_IDENTICAL || st >= toSort[i-1]->strengthFromEmpty) {
          prevStrength  = toSort[i-1]->strengthFromEmpty;
        } else if(st < toSort[i-1]->strengthFromEmpty) { 
          prevStrength = st;
        }
        toSort[i]->strengthFromEmpty = prevStrength;
      }
      if(i < size-2) {
        toSort[i+1]->strengthFromEmpty = probe.distanceFromEmptyString(*toSort[i+1]);
        st = probe.getStrength(*toSort[i+1], *toSort[i]);
        if(st == UCOL_OFF) {
          logger->log("Cannot deduce distance from empty using next element. Something is very wrong! Line:");
          logger->log(toSort[i]->toString(TRUE), TRUE);
        } else if(st == UCOL_IDENTICAL || st < toSort[i+1]->strengthFromEmpty) {
          nextStrength = toSort[i+1]->strengthFromEmpty;
        } else if(st >= toSort[i+1]->strengthFromEmpty) { 
          nextStrength = st;
        }        
        if(i) {
          if(prevStrength != nextStrength) {
            logger->log("Inconsistent results from interpolation! Results will most likely be wrong\n");
          }
        }
        toSort[i]->strengthFromEmpty = nextStrength;
      }
      /*
      UColAttributeValue problemStrength = UCOL_PRIMARY;
      for(j = lastChange; j < i ; j++) {
        if(toSort[j]->strength > problemStrength) {
          problemStrength = toSort[j]->strength;
        }
      }
      for(j = lastChange; j < i ; j++) {
        toSort[j]->strengthFromEmpty = problemStrength;
      }
      strongestStrengthFromEmpty = toSort[i]->strengthFromEmpty;
      lastChange = i;
      debug->log("Problem detected in distances from empty. Most probably word sort is on\n");
      */
      wordSort = TRUE;
    }
    i++;
  }
  debug->log("Distances from empty string\n");
  debug->log(toStringFromEmpty(), TRUE);
}

void
SortedLines::analyse(UErrorCode &status) {
  frenchSecondary = probe.isFrenchSecondary(status);
  if(U_FAILURE(status)) {
    logger->log("Test for French secondary failed. Bailing out!\n");
    return;
  }
  logger->log("French secondary value is %i\n", frenchSecondary, frenchSecondary);
  upperFirst = probe.isUpperFirst(status);
  if(U_FAILURE(status)) {
    logger->log("Test for upper first failed. Bailing out!\n");
    return;
  }
  logger->log("upper first value is %i\n", upperFirst, upperFirst);
  sort(TRUE, TRUE);
  classifyRepertoire();
  getBounds(status);
  //sort(TRUE, TRUE);
  addContractionsToRepertoire(status);
  //sort(TRUE, TRUE);
  debug->log("\n*** Order after detecting contractions\n\n");
  calculateSortKeys();
  debug->log(toPrettyString(FALSE, TRUE), TRUE);
  detectExpansions();
}

void SortedLines::init() 
{
  size = repertoire.size();
  capacity = 5*size;
  lines = new Line[capacity];
  init(repertoire, lines);
}

void SortedLines::init(UnicodeSet &rep, Line *lin)
{

  UnicodeSetIterator exemplarUSetIter(rep);
  int32_t size = 0;

  while(exemplarUSetIter.next()) {
    Line *currLine = lin+size;
    if(exemplarUSetIter.isString()) { // process a string
      currLine->setTo(exemplarUSetIter.getString());
    } else { // process code point
      currLine->setTo(exemplarUSetIter.getCodepoint());
    }
    currLine->name[currLine->len] = 0; // zero terminate, for our evil ways
    //currLine->index = size;
    size++;
  }
}

void 
SortedLines::setSortingArray(Line **sortingArray, Line *elements, int32_t sizeToSort) {
  int32_t i = 0;
  for(i = 0; i < sizeToSort; i++) {
    sortingArray[i] = &elements[i];
  }
}

int32_t
SortedLines::setSortingArray(Line **sortingArray, Hashtable *table) {
  int32_t size = table->count();
  int32_t hashIndex = -1;
  const UHashElement *hashElement = NULL;
  int32_t count = 0;
  while((hashElement = table->nextElement(hashIndex)) != NULL) {
    sortingArray[count++] = (Line *)hashElement->value.pointer;
  }
  return size;
}

void
SortedLines::sort(Line **sortingArray, int32_t sizeToSort, UBool setStrengths, UBool link) {
  int32_t i = 0;
  int32_t equalStart = 0;
  UColAttributeValue equalStrength = UCOL_OFF;

  qsort(sortingArray, sizeToSort, sizeof(Line *), probe.comparer);

  if(setStrengths) { // analyze strengths
    for(i = 1; i < sizeToSort; i++) {
      sortingArray[i]->strength = probe.getStrength(*sortingArray[i-1], *sortingArray[i]);
    }
    // for equal guys, do the code point ordering
    
    i = 1;
    while(i < sizeToSort) 
    {
      if(sortingArray[i]->strength == UCOL_IDENTICAL) {
        equalStart = i - 1;
        equalStrength = sortingArray[equalStart]->strength;
        sortingArray[equalStart]->strength = UCOL_IDENTICAL;
        while(i < sizeToSort && sortingArray[i]->strength == UCOL_IDENTICAL) {
          i++;
        }
        qsort(sortingArray+equalStart, i-equalStart, sizeof(Line *), codePointCmp);
        sortingArray[equalStart]->strength = equalStrength;
      } else {
        i++;
      }
    }
    
  }



  if(link) { // do the linking
    for(i = 0; i < sizeToSort - 1; i++) {
      Line *curr = *(sortingArray+i);
      curr->next = *(sortingArray+i+1);
      (*(sortingArray+i+1))->previous = curr;
    }
  }
}

void
SortedLines::sort(UBool setStrengths, UBool link) {
  if(toSortCapacity < size || !toSort) {
    if(toSort) {
      delete[] toSort;
    }
    toSort = new Line*[size*2];
    toSortCapacity = size*2;
  }

  setSortingArray(toSort, lines, size);
  sort(toSort, size, setStrengths, link);

  first = last = NULL;

  if(link) { // do the linking
    first = *toSort;
    last = *(toSort+size-1);
  }
}

void
SortedLines::updateBounds(UnicodeSet &set) {
  Line line;
  UnicodeString s1;
  UnicodeSetIterator it1(set); 
  while(it1.next()) {
    if(!debug->isOn()) {
      logger->log(".");
    }
    if(it1.isString()) { // process a string
      s1.setTo(it1.getString());
    } else { // process code point
      s1.setTo(it1.getCodepoint());
    }
    //line.setTo(s1);
    UColAttributeValue strength = probe.distanceFromEmptyString(s1);
    if(probe.compare(UnicodeString(UB[strength]->name), s1) < 0) {
      // TODO: leak here - fixit!
      UB[strength] = new Line(s1);
      //u_strcpy(UB[strength], s1.getTerminatedBuffer());
    }
  }



}

void SortedLines::addAll(Line* toAdd, int32_t toAddSize) 
{
  if(size+toAddSize > capacity) {
    int32_t doGrowingBreakpoint = 0;
    // we need to do growing here
  }
  int32_t i = 0;

  for(i = 0; i < toAddSize; i++) {
    lines[size+i] = toAdd[i];
  }
  size += toAddSize;
}

void SortedLines::setDistancesFromEmpty(Line* array, int32_t arraySize)
{
  int32_t i = 0;
  for(i = 0; i < arraySize; i++) {
    array[i].strengthFromEmpty = probe.distanceFromEmptyString(array[i]);
  }
}


// adds contractions in to repertoire
int32_t SortedLines::addContractionsToRepertoire(UErrorCode &status) 
{
  logger->log("\n*** Detecting contractions\n\n");
  contractionsTable = new Hashtable();
  int32_t noConts = 0;
  int32_t allocateSize = 50*size;
  // first check for simple contractions
  Line* delta = new Line[allocateSize];
  Line** deltaSorted = new Line*[allocateSize];
  Line* lesserToAddTo = new Line[allocateSize];
  Line* newDelta = new Line[allocateSize];
  Line** newDeltaSorted = new Line*[allocateSize];
  Line* deltaP = delta; 
  Line** deltaPP = deltaSorted;
  Line* newDeltaP = newDelta;
  int32_t deltaSize = 0, lesserToAddToSize = 0, newDeltaSize = 0;
  logger->log("++ Contraction detection generation 0\n");
  noConts = detectContractions(toSort, size, toSort, size, 
			       delta, deltaSize, lesserToAddTo, lesserToAddToSize, 3*size, status);
  setSortingArray(deltaSorted, delta, deltaSize);
  sort(deltaSorted, deltaSize, TRUE);

  setDistancesFromEmpty(delta, deltaSize);
  int32_t deltaPSize = deltaSize;
  //updateBounds(delta);

  int32_t generation = 0;
  // if we found any, we have to try multiple contractions
  // However, we want to prevent the contractions explosion
  // if the number of simple contractions is greater than the
  // starting size, chances are that we either have an algorithmic
  // contraction (like iteration marks on w2k) or something
  // is seriosly wrong.
  if(deltaPSize < size/2) {
    while (deltaPSize && generation < 1) {
      generation++;
      logger->log("\n++ Contraction detection generation %i\n", generation, generation);
      // find more, but avoid testing the combinations we already have
      noConts += detectContractions(toSort, size, deltaPP, deltaPSize,
				    newDeltaP, newDeltaSize, lesserToAddTo, lesserToAddToSize, 3*size, status);
      noConts += detectContractions(deltaPP, deltaPSize, toSort, size, 
				    newDeltaP, newDeltaSize, lesserToAddTo, lesserToAddToSize, 3*size, status);
      calculateSortKeys();

      addAll(deltaP, deltaPSize);
      setSortingArray(toSort, lines, size);
      sort(TRUE, TRUE);
      setSortingArray(newDeltaSorted, newDeltaP, newDeltaSize);
      sort(newDeltaSorted, newDeltaSize, TRUE);
	  
      // if no new ones, bail
      //if (newDeltaSize == 0) break;

      deltaPSize = newDeltaSize;
      newDeltaSize = 0;
      if(deltaP == delta) {
        deltaP = newDelta;
        deltaPP = newDeltaSorted;
        newDeltaP = delta;
      } else {
        deltaP = delta;
        deltaPP = deltaSorted;
        newDeltaP = newDelta;
      }
      setDistancesFromEmpty(deltaP, deltaPSize);
    }
  }
  status = U_ZERO_ERROR;
  // add stuff from the last batch
  addAll(deltaP, deltaPSize);

  // warning: we don't add the lesser ones in recursively, since they will
  // infinitely loop
  setDistancesFromEmpty(lesserToAddTo, lesserToAddToSize);
  addAll(lesserToAddTo, lesserToAddToSize);
  setSortingArray(toSort, lines, size);
  sort(TRUE, TRUE);

  delete[] deltaSorted;
  delete[] delta;
  delete[] lesserToAddTo;
  delete[] newDeltaSorted;
  delete[] newDelta;
  return noConts;
}


int32_t SortedLines::detectContractions(Line **firstRep, int32_t firstSize,
                                        Line **secondRep, int32_t secondSize,
                                        Line *toAddTo, int32_t &toAddToSize, 
                                        Line *lesserToAddTo, int32_t &lesserToAddToSize,
                                        int32_t capacity, UErrorCode &status) 
{
  int32_t noConts = 0;
  int i = 0, j = 0, k = 0;
  Line lower, upper, trial, toAdd, helper;
  UChar32 firstStart, firstEnd, secondStart;
  UChar NFCTrial[256];
  int32_t NFCTrialLen = 0;
  UBool thai;
  i = -1;
  while(i < firstSize-1 && U_SUCCESS(status)) {
    i++;
    if(!debug->isOn()) {
      logger->log("\rTesting %05i/%05i. Found %05i conts.", i, firstSize, noConts);
    }
    U16_GET(firstRep[i]->name, 0, 0, firstRep[i]->len, firstStart);
    if(uscript_getScript(firstStart, &status) == USCRIPT_HAN || firstRep[i]->strengthFromEmpty > UCOL_PRIMARY) //UCOL_TERTIARY) 
      {
        continue;
      }
    lower = *firstRep[i];
    for(j = 0; j < secondSize; j++) {
      if(noConts == capacity) {
        return noConts;
      }
      U16_GET(secondRep[j]->name, 0, 0, secondRep[j]->len, secondStart);
      if(firstStart == 0x41 && secondStart == 0x308) {
      int32_t putBreakPointHere = 0;
    }
      if(uscript_getScript(secondStart, &status) == USCRIPT_HAN) // || secondRep[j]->strengthFromEmpty > UCOL_TERTIARY) 
	{
          continue;
        }
      	if(duplicators && duplicators->get(UnicodeString(secondRep[j]->name, secondRep[j]->len)) != NULL) {
          debug->log("Skipping duplicator ");
          debug->log(secondRep[j]->toString(), TRUE);
		  continue;
		}

      if(firstRep[i]->name[0] == 0x61 && secondRep[j]->name[0] == 0x308) {
	    int32_t putBreakpointhere = 0;
      }
      upper.setToConcat(firstRep[i], UB[UCOL_PRIMARY]);
      //upper.setToConcat(firstRep[i], UB[secondRep[j]->strengthFromEmpty]);
      toAdd.setToConcat(firstRep[i], secondRep[j]);          
      U16_GET(firstRep[i]->name, 0, firstRep[i]->len-1, firstRep[i]->len, firstEnd);
      if((thai = u_hasBinaryProperty(firstEnd, UCHAR_LOGICAL_ORDER_EXCEPTION))) {
	// this means that the lower is single reordering character
	// if we do the lower test without taking this into account,
	// we'll comparing the secondRep directly to Thai. We add UB[UCOL_PRIMARY] to
	// end of lower and in the middle of trial, so we will have 
	// lower = Thai + UB, trial Thai + UB + x, resolving to
	// UB + Thai vs UB + Thai + x.
	// for upper bound, we do the similar, so we have
	// upper = Thai + UB + UB, trial = Thai + UB + x,
	// resolving to UB + Thai + UB vs UB + Thai + x
	if(secondRep[j]->firstCC) {
	  UChar32 UBChar;
	  U16_GET(UB[UCOL_SECONDARY]->name, 0, 0, UB[UCOL_SECONDARY]->len, UBChar);
	  if(secondRep[j]->firstCC > u_getCombiningClass(UBChar)) {
	    continue;
	  }
	} 
	upper = *firstRep[i];
	upper.append(*UB[UCOL_PRIMARY]);
	//upper.append(*UB[secondRep[j]->strengthFromEmpty]);
	upper.append(*UB[UCOL_PRIMARY]);
	lower.append(*UB[UCOL_PRIMARY]);
	trial = *firstRep[i];
	trial.append(*UB[UCOL_PRIMARY]);
	trial.append(*secondRep[j]);
      } else if((firstRep[i]->lastCC > secondRep[j]->firstCC && secondRep[j]->firstCC && !frenchSecondary) 
		|| (firstRep[i]->firstCC < secondRep[j]->lastCC && firstRep[i]->firstCC && frenchSecondary)) {
	// Skip because normalization will reorder
	// there will be a chance to check this again, since if we
	// try a+b, we will also try b+a
	    continue;
      } else if(frenchSecondary && (firstRep[i]->strengthFromEmpty > UCOL_PRIMARY && secondRep[j]->strengthFromEmpty > UCOL_PRIMARY)) {
	    continue;
      }else if(firstRep[i]->lastCC && secondRep[j]->firstCC && frenchSecondary) {
	    trial.setToConcat(secondRep[j], firstRep[i]);
      } else {
    	trial.setToConcat(firstRep[i], secondRep[j]);          
      }
      // Now let's check the trial. The problem is that when you combine characters, 
      // you can end up with concatenation that is unknown for the examined API.
      NFCTrialLen = unorm_normalize(trial.name, trial.len, UNORM_NFC, 0, NFCTrial, 256, &status);
      if((u_strcmp(trial.name, NFCTrial) == 0) || u_strFindLast(NFCTrial, NFCTrialLen, secondRep[j]->name, secondRep[j]->len)) {
	   if(secondRep[j]->strengthFromEmpty > UCOL_TERTIARY) {
	     continue;
	   }
	 }
      UChar32 c;
      U16_GET(NFCTrial, 0, 0, NFCTrialLen, c);
      helper.setTo(c);
      if(probe.distanceFromEmptyString(helper) > UCOL_TERTIARY) {
	    continue;
      }
      if(NFCTrialLen > 1) {
	    U16_GET(NFCTrial, 0, NFCTrialLen-1, NFCTrialLen, c);
	    helper.setTo(c);
        if(probe.distanceFromEmptyString(helper) > UCOL_TERTIARY) {
	      continue;
	    }
      }

      if (probe.compare(lower, trial) >= 0) { // if lower is bigger than trial
        // this might be ok, but I'm having doubts. Here is an additional check:
        if(firstRep[i]->len == 1 || secondRep[j]->strengthFromEmpty == UCOL_PRIMARY) {
          // I'm basically saying that I'll add this kind of contraction for cases where I combine
          // one letter with an accent OR when I'm combining more than one symbol with a letter.
          noteContraction("L", lesserToAddTo, lesserToAddToSize, firstRep[i], secondRep[j], noConts, status);
        }
      } 
      else if (probe.compare(trial, upper) > 0) { // trial is bigger than upper??
        noteContraction("U", toAddTo, toAddToSize, firstRep[i], secondRep[j], noConts, status);
      } 
#if 0
      else if(firstRep[i]->strengthFromEmpty == UCOL_PRIMARY)
      {
        Line expansionLine;
        if(getExpansionLine(trial, *firstRep[i], *secondRep[j], expansionLine) &&
        expansionLine.len && !(expansionLine == *secondRep[j])) {
          noteContraction("D", toAddTo, toAddToSize, firstRep[i], secondRep[j], noConts, status);
        }            
      }
#endif
      else if(firstRep[i]->strengthFromEmpty == UCOL_PRIMARY && probe.getStrength(lower, trial) < secondRep[j]->strengthFromEmpty) {
        noteContraction("D1", toAddTo, toAddToSize, firstRep[i], secondRep[j], noConts, status);
      } 
      else if (firstRep[i]->strengthFromEmpty == UCOL_PRIMARY && secondRep[j]->strengthFromEmpty == UCOL_PRIMARY) 
      {
        // I have added an additional check. The checks versus upper and lower bound should be sufficient
        // when the right side is a combining mark. There might be a reordering of combining marks, but
        // that should be already visible in their order.
	// compare the sequence 
	// Y- <? Y <? Y+
	// and 
	// XY- <? XY <? XY+
	Line xym, xyp, xy;
	UBool xymIsContraction = FALSE, toAddIsContraction = FALSE;
    if(j) {
	  if(((!secondRep[j-1]->firstCC || firstRep[i]->lastCC < secondRep[j-1]->firstCC) && !frenchSecondary) 
             ||((!firstRep[i]->firstCC || firstRep[i]->firstCC > secondRep[j-1]->lastCC) && frenchSecondary)) {
	    xym.setToConcat(firstRep[i], secondRep[j-1]);
	    toAdd.strength = probe.getStrength(xym, toAdd);
	    if(secondRep[j]->strength != toAdd.strength) {
	      // there is possibility that either xym or xy are contractions
	      // There are two situations:
	      // xym > xy or xym <n xy and ym <k y but n != k
	      // if they are reordered, we are going to see if each of them 
	      // is further reordered
	      if(toAdd.strength == UCOL_OFF) {
		// check whether toAdd shifted more down
		k = j - 2;
		while(k>=0 && secondRep[k]->strength > secondRep[j]->strength) {
		  k--;
		}
		while(!toAddIsContraction && k>=0) {
		  xyp.setToConcat(firstRep[i], secondRep[k]);
		  if(contractionsTable->get(UnicodeString(xyp.name, xyp.len)) != NULL) {
		    k--;
		    continue;
		  }
		  if(probe.compare(xyp, xym) >= 0) {
		    // xyp looks like a contraction
		    noteContraction("!1", toAddTo, toAddToSize, firstRep[i], secondRep[j], noConts, status);
		    toAddIsContraction = TRUE;
		  } else {
		    break;
		  }
		}
          // first let's see if xym has moved beyond
          if(contractionsTable->get(UnicodeString(xym.name, xym.len)) == NULL) {
            k = j+1;
            // ignore weaker strengths
            while(k < secondSize && secondRep[k]->strength > secondRep[j]->strength) {
              k++;
            }
            // check if we skipped the following guy
            if(k < secondSize) {
              xyp.setToConcat(firstRep[i], secondRep[k]);
              if(probe.compare(xyp, xym) <= 0) {
	            // xyp looks like a contraction
	            noteContraction("!2", toAddTo, toAddToSize, firstRep[i], secondRep[j-1], noConts, status);
	            xymIsContraction = TRUE;
              }
            }
          } else {
            xymIsContraction = TRUE;
          }
          // if they have reordered, but none has moved, then we add them both
          // and hope for the best
          if(!xymIsContraction && !toAddIsContraction) {
              // it is possible that there is an NFC version version of one of the 
              // strings. If we have XY > XZ, but NFC(XZ) = W and X < W, we might have
              // have a false contraction.
              trial.len = unorm_normalize(toAdd.name, toAdd.len, UNORM_NFC, 0, trial.name, 25, &status);
              //UColAttributeValue strength = probe.getStrength(*firstRep[i], trial);
              if(trial == toAdd) {
                noteContraction("!3", toAddTo, toAddToSize, firstRep[i], secondRep[j-1], noConts, status);
                noteContraction("!3", toAddTo, toAddToSize, firstRep[i], secondRep[j], noConts, status);
              } else {
                noteContraction("!4", toAddTo, toAddToSize, firstRep[i], secondRep[j], noConts, status);
              }
            }
	      } else { // only the strength has changed
            // check whether the previous is contraction and if not, add the current
            if(contractionsTable->get(UnicodeString(xym.name, xym.len)) == NULL) {
              noteContraction("!5", toAddTo, toAddToSize, firstRep[i], secondRep[j], noConts, status);
            }                  
	      }
	    }
	  }
	}
      }
      if(thai) { // restore lower
        lower = *firstRep[i];
      }
    }
  }
  return noConts;
}

void
SortedLines::noteContraction(const char* msg, Line *toAddTo, int32_t &toAddToSize, Line *left, Line *right, int32_t &noConts, UErrorCode &status) 
{
  Line toAdd;
  toAdd.setToConcat(left, right);
  toAdd.left = left;
  toAdd.right = right;
  // if we're adding an accent to an existing contraction, we want to check
#if 0
  Line test, trial1, trial2;
  if(right->strengthFromEmpty > UCOL_PRIMARY) {
    if(left->right && left->right->previous && left->right->next) {
      test.setToConcat(left->left, left->right->previous);
      trial1.setToConcat(&test, right);

      test.setToConcat(left->left, left->right->next);
      trial2.setToConcat(&test, right);
      if(probe.compare(trial1, toAdd) < 0 && probe.compare(toAdd, trial2) < 0) {
        // this means that the contraction has been broken by the newly added accent
        // so while 'ch' is contraction, 'ch'+dot_above sorts between 'cg'+dot_above and 'ci'+dot_above
        debug->log("Con -");
        debug->log(msg);
        debug->log(toAdd.toString(FALSE), TRUE);
        return;
      }
    } else {
      if(right->previous && right->next) {
        trial1.setToConcat(left, right->previous);
        trial2.setToConcat(left, right->next);
        if(probe.compare(trial1, toAdd) < 0 && probe.compare(toAdd, trial2) < 0) {
          // this means that the contraction has been broken by the newly added accent
          // so while 'ch' is contraction, 'ch'+dot_above sorts between 'cg'+dot_above and 'ci'+dot_above
          debug->log("Con -");
          debug->log(msg);
          debug->log(toAdd.toString(FALSE), TRUE);
          return;
        }
      }
      if(left->previous && left->next) {
        trial1.setToConcat(left->previous, right);
        trial2.setToConcat(left->next, right);
        if(probe.compare(trial1, toAdd) < 0 && probe.compare(toAdd, trial2) < 0) {
          // this means that the contraction has been broken by the newly added accent
          // so while 'ch' is contraction, 'ch'+dot_above sorts between 'cg'+dot_above and 'ci'+dot_above
          debug->log("Con -");
          debug->log(msg);
          debug->log(toAdd.toString(FALSE), TRUE);
          return;
        }
      }

    }
  }
  if(right->right && right->right->strengthFromEmpty > UCOL_PRIMARY && right->left->previous && right->left->next) { // maybe we already had a contraction with an accent
    test.setToConcat(right->left->previous, right->right);
    trial1.setToConcat(left, &test);
    test.setToConcat(right->left->next, right->right);
    trial2.setToConcat(left, &test);
    if(probe.compare(trial1, toAdd) < 0 && probe.compare(toAdd, trial2) < 0) {
      // this means that the contraction has been broken by the newly added accent
      // so while 'ch' is contraction, 'ch'+dot_above sorts between 'cg'+dot_above and 'ci'+dot_above
      debug->log("Con -");
      debug->log(msg);
      debug->log(toAdd.toString(FALSE), TRUE);
      return;
    }
  }
#endif
  if(contractionsTable->get(UnicodeString(toAdd.name, toAdd.len)) == NULL) {
    if(probe.distanceFromEmptyString(toAdd) <= UCOL_TERTIARY) {
      toAddTo[toAddToSize++] = toAdd;
      contractionsTable->put(UnicodeString(toAdd.name, toAdd.len), &toAdd, status);
      noConts++;
      debug->log(msg);
      debug->log(" Con + ");
      debug->log(toAdd.toString(FALSE), TRUE);

      if(!left->sortKey) {
        calculateSortKey(*left);
      }
      debug->log(left->dumpSortkey());
      debug->log(" + ");

      if(!right->sortKey) {
        calculateSortKey(*right);
      }
      debug->log(right->dumpSortkey());
      debug->log(" = ");

      calculateSortKey(toAdd);
      debug->log(toAdd.dumpSortkey(), TRUE);
      if(noConts > size/2) {
        status = U_BUFFER_OVERFLOW_ERROR;
      }
    }
  }
}


UBool
SortedLines::getExpansionLine(const Line &expansion, const Line &previous, const Line &exp, Line &expansionLine) 
{
  int expIndexSize = 0;
  UColAttributeValue expStrength = UCOL_OFF;
  int32_t comparisonResult = 0;
  int32_t i = 0, k = 0, prevK = 0;
  Line trial;
  UBool sequenceCompleted = FALSE;
  int32_t expIndexes[256];
  int32_t expIndexesSize = 0;

  if(!sequenceCompleted) {
    expIndexSize = 0;
    expansionLine.clear();

    // we will start from strength between the expansion
    // and the target (toSort[i] and toSort[j]. First we
    // will add as many primaries as possible. Then we will
    // try to add secondary pieces and then tertiary. 
    // found an expansion - what is the expanding sequence?
  
    expStrength = UCOL_PRIMARY;
    while(!sequenceCompleted) {
      k = 0;
      prevK = 0;
      while(k < size) {
        if(expansionLine.len > 15) {
          sequenceCompleted = TRUE;
          break;
        }
        while(k < size && toSort[k]->strength != UCOL_PRIMARY) 
        {
          k++;
        }
        // nothing found
        if(k == size) {
          break;
        }
        // we need to skip over reordering things. If they were worthy, they would
        // have been detected in the previous iteration.
        //if(expansionLine.lastCC && toSort[k]->firstCC && expansionLine.lastCC > toSort[k]->firstCC) {
          //k++;
          //continue;
        //}
        trial = previous;
        trial.append(expansionLine);
        trial.append(*toSort[k]);        
        if(toSort[k]->name[0] == 0x0067) {
          int32_t putBreakPointHere = 0;
        }
        comparisonResult = probe.compare(trial, expansion);
        if(comparisonResult == 0) {
          expansionLine = *toSort[k];
          return TRUE;
        } else if (comparisonResult > 0) {
          if(prevK) {
            if(exp == *toSort[prevK]) {
              expansionLine = exp;
              return TRUE;
            }
            i = prevK;
            while(i < k-1) {
              i++;
              if(toSort[i]->strength > exp.strength) {
                continue;
              }
              trial = previous;
              trial.append(expansionLine);
              trial.append(*toSort[i]);
              if(probe.compare(trial, expansion) > 0) {
                break;
              }
            }
            // we got into situation where we have ch > ch+dot-below
            // however, ch is a contraction and therefore we cannot use
            // it properly. If we have hit on a contraction, we'll just try
            // to continue. Probably need more logic here.
            if(contractionsTable->get(UnicodeString(trial.name, trial.len)) == NULL) {
              expansionLine.append(*toSort[i-1]);
              expIndexes[expIndexSize++] = i-1;
              break;
            } else {
              int32_t putBreakPointHere = 0;
            }
          } else {
            sequenceCompleted = TRUE;
            break;
          }
          //break;
        }
        prevK = k;
        k++;
      }
      if(!prevK || k == size) {
        break;
      }
    }
  }
  return expIndexSize > 0;
}

int32_t
SortedLines::gooseUp(int32_t resetIndex, int32_t expansionIndex, Line &expLine, int32_t *expIndexes, int32_t &expIndexSize, UColAttributeValue strength) 
{
  int32_t i = expansionIndex, k = resetIndex+1, n = 0, m = 0, start = 0;
  UBool haveChanges = FALSE;
  Line trial, prefix, suffix;
  // we will first try goosing up the reset index
  //while(toSort[k]->strength >= strength)
  for( ; toSort[k]->strength == strength; k++) 
  {
    //if(toSort[k]->strength > strength) {
      //continue;
    //}
    trial.setToConcat(toSort[k], &expLine);
    if(probe.compare(trial, *toSort[i]) > 0) {
      break;
    }
  }
  resetIndex = k-1;

  // goose up individual characters
  prefix = *toSort[resetIndex];
  for(n = 0; n < expIndexSize; n++) {
    suffix.clear();
    for(m = n+1; m < expIndexSize; m++) {
      suffix.append(*toSort[expIndexes[m]]);
    }
    k = expIndexes[n]+1;
    //while(toSort[k]->strength >= strength)
    for( ; toSort[k]->strength == strength; k++) 
    {
      //if(toSort[k]->strength > strength) {
        //continue;
      //}
      trial.setToConcat(&prefix, toSort[k]);
      trial.append(suffix);
      if(probe.compare(trial, *toSort[i]) > 0) {
        break;
      }
    }
    if(k > expIndexes[n]+1) {
      haveChanges = TRUE;
      expIndexes[n] = k-1;
    }
    prefix.append(*toSort[expIndexes[n]]);
  }

  // try inserting ingorables
  UColAttributeValue lastStr = UCOL_OFF;
  k = 0;
  while(toSort[k]->strengthFromEmpty > strength) {
    k++;
  }
  if(toSort[k]->strengthFromEmpty == strength) {
    start = k;
    prefix = *toSort[resetIndex];
    n = 0;
    while(n <= expIndexSize) {
      suffix.clear();
      for(m = n; m < expIndexSize; m++) {
        suffix.append(*toSort[expIndexes[m]]);
      }
      k = start;
      while(toSort[k]->strengthFromEmpty == strength) {
        trial.setToConcat(&prefix, toSort[k]);
        trial.append(suffix);
        lastStr = probe.getStrength(trial, *toSort[i]);
        if(lastStr == UCOL_OFF) { // shot over - we won't find anything here
          break;
        } else if(lastStr > strength) {
          for(m = expIndexSize; m > n; m--) {
            expIndexes[m] = expIndexes[m-1];
          }
          expIndexes[n] = k;
          expIndexSize++;
          haveChanges = TRUE;
          break;
        }
#if 0
        if(probe.compare(trial, *toSort[i]) > 0) {
          // if the first one skips, that means that
          // this position doesn't work
          if(k > start) {
            // insert an ignorable on position n
            for(m = expIndexSize; m > n; m--) {
              expIndexes[m] = expIndexes[m-1];
            }
            expIndexes[n] = k-1;
            expIndexSize++;
            haveChanges = TRUE;
            if(n == expIndexSize-1) { // added to the end of the string
              UColAttributeValue str = probe.getStrength(trial, *toSort[i]);
              int32_t putBreakHere = 0;
            }
          }
          break;
        } else {
          lastStr = probe.getStrength(trial, *toSort[i]);
        }
#endif
        k++;
      }
      prefix.append(*toSort[expIndexes[n]]);
      n++;
    }
  }

  if(haveChanges) {
    expLine.clear();
    for(m = 0; m < expIndexSize; m++) {
      expLine.append(*toSort[expIndexes[m]]);
    }
  }
  return resetIndex;
}

int32_t 
SortedLines::detectExpansions() 
{
  logger->log("\n*** Detecting expansions\n\n");
  int32_t exCount = 0;
  int32_t i = 0, j = 0, k = 0, prevK = 0;
  Line *previous, trial, expansionLine;
  UBool foundExp = FALSE, sequenceCompleted = FALSE;
  UColAttributeValue strength = UCOL_OFF;
  UColAttributeValue maxStrength = UCOL_IDENTICAL;
  UColAttributeValue expStrength = UCOL_OFF;
  int32_t expIndexes[256];
  int32_t expIndexSize = 0;
  memset(expIndexes, 0, sizeof(expIndexes));

  // for each element, we look back to find whether there is such a q for which
  // q <n x < qUBn. These are possible expansions. When going backwards we skip 
  // over already detected expansions.
  i = 0;
  // it turns out that looking at accents as possible expansions is
  // quite a stupid thing to do, especially on non ICU platforms.
  // Previously this line skipped over identicals only, but 
  // now we are going to skip all the way to non-ignorables.
  while(toSort[i]->strengthFromEmpty > UCOL_PRIMARY) {
    i++;
  }
  i++;
  for( ; i < size; i++) {
    if(toSort[i]->name[0]==0x0063 && toSort[i]->name[1] == 0x68) // && toSort[i]->name[1] == 0x308)0043 0043 0219
    { 
      int32_t putBreakpointhere = 0;
    }
    foundExp = FALSE;
    sequenceCompleted = FALSE;
    strength = toSort[i]->strength;
    if(strength == UCOL_IDENTICAL && toSort[i-1]->isExpansion == TRUE) {
      u_strcpy(toSort[i]->expansionString, toSort[i-1]->expansionString);
      toSort[i]->expLen = toSort[i-1]->expLen;
      toSort[i]->isExpansion = TRUE;
      toSort[i]->expIndex = toSort[i-1]->expIndex;
      toSort[i]->expStrength = UCOL_IDENTICAL;
      //toSort[i]->expStrength = toSort[i-1]->expStrength;
      foundExp = TRUE;
      sequenceCompleted = TRUE;
    }
    //logger->log("%i %i\n", i, j);
    while(!foundExp && strength <= maxStrength) {
      j = i-1;
      while(j && (toSort[j]->isExpansion == TRUE || toSort[j]->isRemoved == TRUE)) {
        //if(toSort[j]->strength < strength) {
          //strength = toSort[j]->strength;
        //}
        j--;
      }

      //while(j && toSort[j]->strength > strength)
      while(j && toSort[j]->strength > probe.getStrength(*toSort[j], *toSort[i]))
      {
        j--;
      }
      //if(toSort[j]->strength == strength) {
        previous = toSort[j];
        if(previous->strengthFromEmpty >= UCOL_IDENTICAL ||
          (previous->strengthFromEmpty == UCOL_SECONDARY
          && strength == UCOL_SECONDARY
          && previous->lastCC > UB[strength]->firstCC)) {
          break;
          //continue;
        }
        //trial.setToConcat(previous, UB[strength]);
        trial.setToConcat(previous, UB[probe.getStrength(*toSort[j], *toSort[i])]);
        if(probe.compare(trial, *toSort[i]) > 0) {
          foundExp = TRUE;
        }
      //}
      if(strength == UCOL_QUATERNARY) {
        strength = UCOL_IDENTICAL;
      } else {
        strength = (UColAttributeValue)(strength + 1);
      }
    }
    // calculate the expanding sequence
    if(foundExp && !sequenceCompleted) {
      expIndexSize = 0;
      expansionLine.clear();
      exCount++;
      // we will start from strength between the expansion
      // and the target (toSort[i] and toSort[j]. First we
      // will add as many primaries as possible. Then we will
      // try to add secondary pieces and then tertiary. 
      // found an expansion - what is the expanding sequence?
      
      expStrength = UCOL_PRIMARY;
      while(!sequenceCompleted) {
        k = 0;
        prevK = 0;
        while(k < size) {
          if(expansionLine.len > 15) {
            sequenceCompleted = TRUE;
            break;
          }
          while(k < size && toSort[k]->strength != UCOL_PRIMARY) {
            k++;
          }
          // nothing found
          if(k == size) {
            break;
          }
          // we need to skip over reordering things. If they were worthy, they would
          // have been detected in the previous iteration.
          //if(expansionLine.lastCC && toSort[k]->firstCC && expansionLine.lastCC > toSort[k]->firstCC) {
            //k++;
            //continue;
          //}
          trial = *previous;
          trial.append(expansionLine);
          trial.append(*toSort[k]);        
          if(toSort[k]->name[0] == 0x0067) {
            int32_t putBreakPointHere = 0;
          }
          if(probe.compare(trial, *toSort[i]) > 0) {
            if(prevK) {
              // we got into situation where we have ch > ch+dot-below
              // however, ch is a contraction and therefore we cannot use
              // it properly. If we have hit on a contraction, we'll just try
              // to continue. Probably need more logic here.
              if(contractionsTable->get(UnicodeString(trial.name, trial.len)) == NULL) {
                expansionLine.append(*toSort[prevK]);
                expIndexes[expIndexSize++] = prevK;
                break;
              } else {
                int32_t putBreakPointHere = 0;
              }
            } else {
              sequenceCompleted = TRUE;
              break;
            }
            //break;
          }
          prevK = k;
          k++;
        }
        if(!prevK || k == size) {
          break;
        }
      }
      // after this we have primaries lined up.
      // we are going to goose up with secondaries and 
      // tertiaries
      trial.setToConcat(toSort[j], &expansionLine);
      expStrength = probe.getStrength(trial, *toSort[i]);
      if(expStrength > UCOL_PRIMARY) {
        if(expStrength == UCOL_SECONDARY || expStrength == UCOL_OFF) {
          j = gooseUp(j, i, expansionLine, expIndexes, expIndexSize, UCOL_SECONDARY);
          trial.setToConcat(toSort[j], &expansionLine);
          expStrength = probe.getStrength(trial, *toSort[i]);
          if(expStrength == UCOL_TERTIARY) {
            j = gooseUp(j, i, expansionLine, expIndexes, expIndexSize, UCOL_TERTIARY);
          }
        } else if(expStrength == UCOL_TERTIARY) {
          j = gooseUp(j, i, expansionLine, expIndexes, expIndexSize, UCOL_TERTIARY);
        }
      }
      trial.setToConcat(toSort[j], &expansionLine);
      expStrength = probe.getStrength(trial, *toSort[i]);
      if(expansionLine.len) {
        if(expansionLine.name[0] == 0x73 && expansionLine.name[1] == 0x7a) {
          int32_t putBreakpointhere = 0;
        }
        UBool isExpansionLineAContraction = (contractionsTable->get(UnicodeString(expansionLine.name, expansionLine.len)) != NULL);
        // we have an expansion line and an expansion. There could be some expansions where 
        // the difference between expansion line and the end of expansion sequence is less or 
        // equal than the expansion strength. These should probably be removed.
        int32_t diffLen = toSort[i]->len - expansionLine.len;
        if(diffLen > 0) {
          trial.setTo(UnicodeString(toSort[i]->name + diffLen, toSort[i]->len - diffLen));
        } else {
          trial = *toSort[i];
        }
        UColAttributeValue s1 = probe.getStrength(trial, expansionLine);
        if(s1 == UCOL_OFF) {
          s1 = probe.getStrength(expansionLine, trial);
        }
        if((!isExpansionLineAContraction && s1 >= expStrength) || (diffLen <= 0 && s1 == UCOL_IDENTICAL)) {
          contractionsTable->remove(UnicodeString(toSort[i]->name, toSort[i]->len));
          toSort[i]->isRemoved = TRUE;
          if(toSort[i]->next && toSort[i]->previous) {
            toSort[i]->previous->next = toSort[i]->next;
          }
          if(toSort[i]->previous && toSort[i]->next) {
            toSort[i]->next->previous = toSort[i]->previous;
          }
	      debug->log("Exp -N: ");
	      debug->log(toSort[i]->toString(FALSE));
	      debug->log(" / ");
	      debug->log(expansionLine.toString(FALSE), TRUE);
        }
        else
        {         
          u_strncat(toSort[i]->expansionString, expansionLine.name, expansionLine.len);
          toSort[i]->isExpansion = TRUE;
          toSort[i]->expStrength = expStrength;
          toSort[i]->expLen = expansionLine.len;
          toSort[i]->expansionString[toSort[i]->expLen] = 0;
          toSort[i]->expIndex = j;
        }
      }
    }
    if(toSort[i]->isExpansion == TRUE) {
      if(debug->isOn()) {
        debug->log("Exp + : &");
        debug->log(toSort[j]->toString(FALSE));
        debug->log(toSort[i]->strengthToString(toSort[i]->expStrength, TRUE));
        debug->log(toSort[i]->toString(FALSE));
        debug->log(" ");
        if(!toSort[j]->sortKey) {
          calculateSortKey(*toSort[j]);
        }
        debug->log(toSort[j]->dumpSortkey());
        debug->log(" ... ");
        if(!toSort[i]->sortKey) {
          calculateSortKey(*toSort[i]);
        }
        debug->log(toSort[i]->dumpSortkey());
        calculateSortKey(expansionLine);
        debug->log("/");
        debug->log(expansionLine.dumpSortkey(), TRUE);
      }
      
    }
  }
  // after detecting expansions, we want to position them. 
  // it is better to position expansions after all have been detected,
  // since otherwise we will change the ordering.
  for(i = size-1; i >= 0; i--) {
    if(toSort[i]->isExpansion) {
      if(toSort[i]->name[0] == 0x2A3) {
        int32_t putBreakPointHere = 0;
      }
      if(i) {
        if(toSort[i]->previous) {
          toSort[i]->previous->next = toSort[i]->next;
        }
      }
      if(i < size-1) {
        if(toSort[i]->next) {
          toSort[i]->next->previous = toSort[i]->previous;
        }
      }
      j = toSort[i]->expIndex;
      toSort[i]->next = toSort[j]->next;
      toSort[i]->previous = toSort[j];
      toSort[j]->next = toSort[i];
      if(toSort[i]->next) {
        toSort[i]->next->previous = toSort[i];
      }
      toSort[i]->strength = toSort[i]->expStrength;
    }
  }
  return exCount;
}


Line *
SortedLines::getFirst() {
  current = first;
  return current;
}

Line *
SortedLines::getLast() {
  current = last;
  return current;
}

void 
SortedLines::add(Line *line, UBool linkIn) {
  if(size++ == capacity) {
    // grow
  }
  lines[size] = *line;
  Line *toAdd = &lines[size];
  if(linkIn && first) {
    Line *current = first;
    while(current != NULL && probe.comparer(&current, &toAdd) < 0) {
      current = current->next;
    }
    if(current == NULL) {
      toAdd->previous = last;
      toAdd->next = NULL;
      if(last != NULL) {
        last->next = toAdd;
      }
      last = toAdd;
      if(first == NULL) {
        first = toAdd;
      }
    } else { // current != NULL
      toAdd->next = current;
      toAdd->previous = current->previous;
      if(current->previous) {
        current->previous->next = toAdd;
      } else {
        first = toAdd;
      }
      current->previous = toAdd;
    }
  }
}


Line *
SortedLines::getNext()
{
  if(current != NULL) {
    current=current->next;
  }
  return current;
}

Line *
SortedLines::getPrevious()
{
  if(current != NULL) {
    current=current->previous;
  }
  return current;
}

Line *
SortedLines::operator[](int32_t index)
{
  int32_t i = 0;
  Line *c = first;
  for(i = 0; i < index; i++) {
    if(c != NULL) {
      c = c->next;
    }
  }
  return c;
}

UnicodeString
SortedLines::arrayToString(Line** sortedLines, int32_t linesSize, UBool pretty, UBool useLinks, UBool printSortKeys) {
  UnicodeString result;
  int32_t i = 0;

  Line *line = NULL;
  Line *previous = sortedLines[0];
  if(printSortKeys && !sortkeys) {
    printSortKeys = FALSE;
  }
  if(previous->isReset) {
    result.append(" & ");
    result.append(previous->name, previous->len);
    if(pretty) {
      result.append("        # ");
      result.append(previous->stringToName(previous->name, previous->len));
      result.append("\n");
    }
  } else if(!previous->isRemoved) {
    result.append(previous->toString(pretty));
    if(pretty) {
      result.append("\n");
    }
  }
  i = 1;
  while((i < linesSize && !useLinks) || (previous->next && useLinks)) {
    if(useLinks) {
      line = previous->next;
    } else {
      line = sortedLines[i];
    }
    if(line->isReset) {
      result.append(" &");
      result.append(line->name, line->len);
      if(pretty) {
        result.append("        # ");
        result.append(line->stringToName(line->name, line->len));
        result.append("\n");
      }
    } else if(!line->isRemoved) {
      if(i > 0) {
        result.append(line->strengthToString(line->strength, pretty));
      }
      result.append(line->toString(pretty));
      if(printSortKeys) {
        result.append(line->dumpSortkey());
      }
      if(pretty) {
        result.append("\n");
      }
    }
    previous = line;
    i++;
  }
  return result;
}

SortedLines::SortedLines(FILE *file, UPrinter *logger, UPrinter *debug, UErrorCode &status) :
toSort(NULL),
toSortCapacity(0),
lines(NULL),
size(0),
capacity(0),
first(NULL),
last(NULL),
logger(logger),
debug(debug),
contractionsTable(NULL),
duplicators(NULL),
maxExpansionPrefixSize(0),
wordSort(FALSE),
frenchSecondary(FALSE),
upperFirst(FALSE),
sortkeys(NULL),
sortkeyOffset(0)
{
  debug->log("*** loading a dump\n");
  memset(UB, 0, sizeof(UB));
  int32_t i = 0;
  for(i = 0; i < UCOL_OFF; i++) {
    UB[i] = &empty;
  }

  int32_t newFrench, newUpperFirst;
  fscanf(file, "%i,%i,%i\n", &size, &newFrench, &newUpperFirst);
  debug->log("Read size %i, frenchSecondary %i and upperFirst %i\n", size, newFrench, newUpperFirst);
  frenchSecondary = (UBool)newFrench;
  upperFirst = (UBool)newUpperFirst;
  capacity = size;
  lines = new Line[capacity];
  i = 0;

  char buff[256];

  while(fgets(buff, 256, file)) {
    if(i % 20 == 0) {
      logger->log("\rLine: %04i", i, buff);
    }
    lines[i].initFromString(buff, 256, status);
    if(i) {
      lines[i].previous = &lines[i-1];
      lines[i-1].next = &lines[i];
    }
    i++;
  }
  size = i;
  toSort = new Line*[size];
  setSortingArray(toSort, lines, size);
  first = &lines[0];
  last = &lines[size-1];
}

void
SortedLines::toFile(FILE *file, UBool useLinks, UErrorCode &status) 
{
  fprintf(file, "%i,%i,%i\n", size, frenchSecondary, upperFirst);
  int32_t i = 1;
  Line *previous = toSort[0];
  Line *line = NULL;
  char buff[256];
  previous->write(buff, 256, status);
  fprintf(file, "%s\n", buff);
  fflush(file);
  while(previous->next) {
    if(useLinks) {
      line = previous->next;
    } else {
      line = toSort[i];
    }
    line->write(buff, 256, status);
    fprintf(file, "%s\n", buff);
    i++;
    previous = line;
  }
}



UnicodeString 
SortedLines::toStringFromEmpty() {
  UBool useLinks = FALSE;
  UBool pretty = FALSE;
  UnicodeString result;
  int32_t i = 0;

  Line *line = NULL;
  Line *previous = toSort[0];
  if(previous->isReset) {
    result.append(" & ");
    if(pretty) {
      result.append("\n");
    }
    result.append(previous->name, previous->len);
  } else if(!previous->isRemoved) {
    result.append(previous->toString(pretty));
    if(pretty) {
      result.append("\n");
    }
  }
  i = 1;
  while(i < size || previous->next) {
    if(useLinks) {
      line = previous->next;
    } else {
      line = toSort[i];
    }
    if(line->isReset) {
      result.append(" &");
      result.append(line->name, line->len);
      if(pretty) {
        result.append("        # ");
        result.append(line->stringToName(line->name, line->len));
        result.append("\n");
      }
    } else if(!line->isRemoved) {
      if(i > 0) {
        result.append(line->strengthToString(line->strengthFromEmpty, pretty));
      }
      result.append(line->toString(pretty));
      if(pretty) {
        result.append("\n");
      }
    }
    previous = line;
    i++;
  }
  return result;
}

UnicodeString 
SortedLines::toString(UBool useLinks) 
{
  return arrayToString(toSort, size, FALSE, useLinks, FALSE);
}


UnicodeString 
SortedLines::toPrettyString(UBool useLinks, UBool printSortKeys) 
{
  return arrayToString(toSort, size, TRUE, useLinks, printSortKeys);
}

UnicodeString 
SortedLines::toOutput(const char *format, 
                       const char *locale, const char *platform, const char *reference, 
                       UBool useLinks, UBool initialize, UBool moreToCome) {
  if(strcmp(format, "HTML") == 0) {
    return toHTML(locale, platform, reference, useLinks, initialize, moreToCome);
  } else if(strcmp(format, "XML") == 0) {
    return toXML(locale, platform, reference, useLinks, initialize, moreToCome);
  } else {
    return toBundle(locale, platform, reference, useLinks, initialize, moreToCome);
  }
}


UnicodeString 
SortedLines::toHTML(const char *locale, 
                       const char *platform, const char *reference, 
                       UBool useLinks, UBool initialize, UBool moreToCome)
{
  UnicodeString result;
  int32_t i = 0;
  if(initialize) {
    result.append("<html>\n<head>\n<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">\n</head>\n");
    result.append("# Collation data resource bundle generated for locale: ");
    result.append(locale);
    result.append("<br>\n# For platform ");
    result.append(platform);
    result.append(" reference platform ");
    result.append(reference);
    result.append("<br><br>\n\n\n");

    result.append(locale);
    if(platform) {
      result.append("_");
      result.append(platform);
    }
    if(reference) {
      result.append("_vs_");
      result.append(reference);
    }
    result.append("&nbsp;{<br>\n");

    result.append("&nbsp;&nbsp;collations&nbsp;{<br>\n&nbsp;&nbsp;&nbsp;&nbsp;standard&nbsp;{<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Sequence&nbsp;{<br>\n");
  }

  if(frenchSecondary) {
    result.append("[backwards 2]<br>\n");
  }
  if(upperFirst) {
    result.append("[casefirst upper]<br>\n");
  }

  Line *line = toSort[0];

  i = 0;
  while((i < size && !useLinks) || (line->next && useLinks)) {
    if(line->isReset || !line->isRemoved) {
      result.append(line->toHTMLString());
    }
    i++;
    if(useLinks) {
      line = line->next;
    } else {
      line = toSort[i];
    }
  }
  if(!moreToCome) {
    result.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br>\n&nbsp;&nbsp;&nbsp;&nbsp;}<br>\n&nbsp;&nbsp;}<br>\n}<br>\n");

    result.append("</html>\n");
  }

  return result;
}

UnicodeString 
SortedLines::toXML(const char *locale, 
                       const char *platform, const char *reference, 
                       UBool useLinks, UBool initialize, UBool moreToCome)
{
  UnicodeString result;
  int32_t i = 0;
  if(initialize) {
    result.append("<html>\n<head>\n<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">\n</head>\n");
    result.append("# Collation data resource bundle generated for locale: ");
    result.append(locale);
    result.append("<br>\n# For platform ");
    result.append(platform);
    result.append(" reference platform ");
    result.append(reference);
    result.append("<br><br>\n\n\n");

    result.append(locale);
    if(platform) {
      result.append("_");
      result.append(platform);
    }
    if(reference) {
      result.append("_vs_");
      result.append(reference);
    }
    result.append("&nbsp;{<br>\n");

    result.append("&nbsp;&nbsp;collations&nbsp;{<br>\n&nbsp;&nbsp;&nbsp;&nbsp;standard&nbsp;{<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Sequence&nbsp;{<br>\n");
  }

  if(frenchSecondary) {
    result.append("[backwards 2]<br>\n");
  }
  if(upperFirst) {
    result.append("[casefirst upper]<br>\n");
  }

  Line *line = toSort[0];

  i = 0;
  while((i < size && !useLinks) || (line->next && useLinks)) {
    if(line->isReset || !line->isRemoved) {
      result.append(line->toHTMLString());
    }
    i++;
    if(useLinks) {
      line = line->next;
    } else {
      line = toSort[i];
    }
  }
  if(!moreToCome) {
    result.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br>\n&nbsp;&nbsp;&nbsp;&nbsp;}<br>\n&nbsp;&nbsp;}<br>\n}<br>\n");

    result.append("</html>\n");
  }

  return result;
}

UnicodeString 
SortedLines::toBundle(const char *locale, 
                       const char *platform, const char *reference, 
                       UBool useLinks, UBool initialize, UBool moreToCome)
{
  UnicodeString result;
  int32_t i = 0;

  if(initialize) {
    result.append("// Collation data resource bundle generated for locale: ");
    result.append(locale);
    result.append("\n// For platform ");
    result.append(platform);
    result.append(" reference platform ");
    result.append(reference);
    result.append("\n\n\n");

    result.append(locale);
    /*
    if(platform) {
      result.append("_");
      result.append(platform);
    }
    if(reference) {
      result.append("_vs_");
      result.append(reference);
    }
    */
    result.append(" {\n");

    result.append("  collations {\n   standard {\n    Sequence {\n");
  }

  if(frenchSecondary) {
    result.append("[backwards 2]\n");
  }
  if(upperFirst) {
    result.append("[casefirst upper]\n");
  }

  Line *line = toSort[0];

  i = 0;
  while((i < size && !useLinks) || (line->next && useLinks)) {
    if(line->isReset || !line->isRemoved) {
      result.append(line->toBundleString());
    }
    i++;
    if(useLinks) {
      line = line->next;
    } else {
      line = toSort[i];
    }
  }

  if(!moreToCome) {
    result.append("    }\n   }\n  }\n}\n");
  }

  return result;
}


int32_t 
SortedLines::getSize() const {
  return repertoire.size();
}

void
SortedLines::reduceDifference(SortedLines& reference) {
  UErrorCode status = U_ZERO_ERROR;
  if(upperFirst) {
    swapCase();
  }
  // both sorted lines structures need to have established links and strengths
  // We walk down both structures and note differences. These
  // differences will modify this by removng elements, setting resets
  // etc...
  // we will prefer insertions from tailoring to reference, then deletions
  // there are two tables that keep seen elements.
  Hashtable *seenThis = new Hashtable();
  Hashtable *seenReference = new Hashtable();


  UBool found = FALSE;
  UBool finished = FALSE;
  const int32_t lookForward = 20;
  int32_t tailoringMove = 0;
  //int32_t referenceSize = reference.getSize();
  Line *refLine = reference.getFirst();
  Line *refLatestEqual = refLine;
  refLine = refLine->next;
  Line *myLine = getFirst();
  Line *myLatestEqual = myLine;
  myLatestEqual->isRemoved = TRUE;
  myLine = myLine->next;
  while(myLine && refLine) {
    found = FALSE;
    while(myLine && refLine && myLine->equals(*refLine)) {
      myLatestEqual = myLine;
      myLatestEqual->isRemoved = TRUE;
      myLine = myLine->next;
      refLatestEqual = refLine;
      refLine = refLine->next;
      if(refLine == NULL && myLine == NULL) {
        finished = TRUE;
      }
    }
    if(myLine) {
      myLine->cumulativeStrength = myLine->strength;  
    }
    if(refLine) {
      refLine->cumulativeStrength = refLine->strength;
    }
 
    // here is the difference
    while(!found && !finished) {
      tailoringMove = 0;
      if(myLine && refLine) {
        if(myLine->cumulativeStrength > refLine->cumulativeStrength) { 
          // tailoring z <<< x, UCA z < y
          while(myLine->cumulativeStrength > refLine->cumulativeStrength) {
            myLine = myLine->next;
            if(myLine) {
              transferCumulativeStrength(myLine->previous, myLine);
            } else {
              break;
            }
          }
        } else if(myLine->cumulativeStrength < refLine->cumulativeStrength) {  
          // tailoring z < x, UCA z <<< y
          while(myLine->cumulativeStrength < refLine->cumulativeStrength) {
            seenReference->put(UnicodeString(refLine->name, refLine->len), refLine, status);
            refLine = refLine->next;
            if(refLine) {
              transferCumulativeStrength(refLine->previous, refLine);
            } else {
              break;
            }
          }
        }
        // this is the interesting point. Now we search for character match
        while(myLine && refLine && (!myLine->equals(*refLine) || myLine->strength == UCOL_IDENTICAL)
          && tailoringMove < lookForward) {
          if(seenThis->get(UnicodeString(refLine->name, refLine->len))) {
            // we are not interested in stuff from the reference that is already accounted
            // for in the tailoring.
            refLine = refLine->next;
            if(refLine) {
              transferCumulativeStrength(refLine->previous, refLine);
            }
          } else {
            myLine = myLine->next;
            if(myLine) {
              transferCumulativeStrength(myLine->previous, myLine);
              if(!seenReference->get(UnicodeString(myLine->name, myLine->len))) {
                tailoringMove++;
              }
            }
          }
        }
      }
      if(refLine == NULL) { // ran out of reference
        // this is the tail of tailoring - the last insertion
        myLine  = NULL;
        found = TRUE;
      } else if(tailoringMove == lookForward || myLine == NULL) { // run over treshold or out of tailoring
        tailoringMove = 0;
        // we didn't find insertion after all
        // we will try substitution next
        // reset the tailoring pointer
        myLine = myLatestEqual->next;
        // move the reference
        refLine = refLine->next;
        if(refLine) {
          transferCumulativeStrength(refLine->previous, refLine);
        }
      } else { // we found an insertion
        tailoringMove = 0;
        if(myLine->strength != refLine->strength) {
          while(myLine && refLine && *myLine == *refLine 
            && (myLine->strength != refLine->strength 
            || myLine->strength == UCOL_IDENTICAL)) {
            myLine = myLine->next;
            refLine = refLine->next;
          }
          if(*myLine != *refLine) {
            continue;
          }
        }
        if(myLine && refLine && myLine->previous->strength < myLine->strength) {
          myLine = myLine->next;
          refLine = refLine->next;
          if(*myLine != *refLine) {
            continue;
          }
        }
        found = TRUE;
      }
      if(found) {
        if(myLatestEqual->next != myLine || refLine == NULL) {
          Line *myStart = NULL;
          // this is a reset and a sequence
          // myLatestEqual points at the last point that was the same
          // This point will be a reset
          if(myLine && refLine) { // if there is anything more to do - it might be worth saving it
            myStart = myLatestEqual;
            while(myStart != myLine) {
              seenThis->put(UnicodeString(myStart->name, myStart->len), myStart, status);
              myStart = myStart->next;
            }
          }
          // Try to weed out stuff that is not affected, like:
          // Tailoring:
          // <<<S<<\u017F<\u0161<<<\u0160<t
          // UCA:
          // <<<S<<\u0161<<<\u0160<<\u017F<t
          // Result:
          // &S<<\u017F<\u0161<<<\u0160
          // we have a sequence that spans from myLatestEqual to myLine (that one could be NULL, 
          // so we have to go down from myLatestEqual. 
          // Basically, for every element, we want to see the strongest cumulative difference 
          // from the reset point. If the cumulative difference is the same in both the reference and
          // tailoring, that element could be removed.
          calculateCumulativeStrengths(myLatestEqual, myLine);
          calculateCumulativeStrengths(refLatestEqual, refLine);
          myStart = myLatestEqual;
          int32_t removed = 0;
          int32_t traversed = 0;
          while(myStart && myStart != myLine) {
            Line *refStart = refLatestEqual;
            while(refStart && refStart != refLine) {
              if(*myStart == *refStart) {
                if(myStart->cumulativeStrength == refStart->cumulativeStrength) {
                  myStart->isRemoved = TRUE;
                  removed++;
                }
              }
              refStart = refStart->next;
            }
            myStart = myStart->next;
            traversed++;
          }
          if(removed < traversed) {
            myLatestEqual->isReset = TRUE;
            myLatestEqual->isRemoved = FALSE;
          }

          myLatestEqual = myLine;
        }
      }
    }
  }

  if(upperFirst) {
    //swapCase();
  }

  delete seenThis;
  delete seenReference;

}

void
SortedLines::transferCumulativeStrength(Line *previous, Line *that) {
  if(that->strength > previous->cumulativeStrength) {
    that->cumulativeStrength = previous->cumulativeStrength;
  } else {
    that->cumulativeStrength = that->strength;
  }
}

void
SortedLines::calculateCumulativeStrengths(Line *start, Line *end) {
  // start is a reset - end may be NULL
  start = start->next;
  UColAttributeValue cumulativeStrength = UCOL_OFF;
  while(start && start != end) {
    if(start->strength < cumulativeStrength) {
      cumulativeStrength = start->strength;
    }
    start->cumulativeStrength = cumulativeStrength;
    start = start->next;
  }
}


void 
SortedLines::getRepertoire(UnicodeSet &fillIn) {
  fillIn.clear();
  fillIn.addAll(repertoire);
}


void
SortedLines::removeDecompositionsFromRepertoire() {
  UnicodeSetIterator repertoireIter(repertoire);
  UErrorCode status = U_ZERO_ERROR;
  UChar string[256];
  UChar composed[256];
  int32_t len = 0, compLen = 0;
  UnicodeString compString;
  UnicodeSet toRemove;

  while(repertoireIter.next()) {
    len = 0;
    if(repertoireIter.isString()) { // process a string
      len = repertoireIter.getString().length();
      u_memcpy(string, repertoireIter.getString().getBuffer(), len);
    } else { // process code point
      UBool isError = FALSE;
      U16_APPEND(string, len, 25, repertoireIter.getCodepoint(), isError);
    }
    string[len] = 0; // zero terminate, for our evil ways
    compLen = unorm_normalize(string, len, UNORM_NFC, 0, composed, 256, &status);
    if(compLen != len || u_strcmp(string, composed) != 0) {
      compString.setTo(composed, compLen);
      if(repertoire.contains(compString)) {
        toRemove.add(UnicodeString(string, len));
      }
    }
  }
  debug->log("\nRemoving\n");
  debug->log(toRemove.toPattern(compString, TRUE), TRUE);
  repertoire.removeAll(toRemove);
}


void
SortedLines::swapCase() 
{
  int32_t i = 0;
  for(i = 0; i < size; i++) {
    toSort[i]->swapCase();
  }
}

void
SortedLines::calculateSortKey(Line &line) 
{
  if(!sortkeys) {
    sortkeys = new uint8_t[size*1024];
    memset(sortkeys, 0, size*1024);
  }
  line.sortKey = sortkeys+sortkeyOffset;
  sortkeyOffset += probe.getSortKey(line, sortkeys+sortkeyOffset, size*256-sortkeyOffset);
}


void 
SortedLines::calculateSortKeys()
{
  if(sortkeys) {
    delete[] sortkeys;
  }
  sortkeyOffset = 0;
  sortkeys = new uint8_t[size*256];
  memset(sortkeys, 0, size*256);
  int32_t i = 0;
  for(i = 0; i < size; i++) {
    calculateSortKey(*toSort[i]);
  }
}
