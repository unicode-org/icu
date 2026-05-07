// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#include "targetsetgenerator.h"

TargetSetGenerator::TargetSetGenerator(UnicodeSet &startingSet, CompareFn comparer) :
 comparer(comparer),
 set(startingSet)
{
   addAll(startingSet);
}
