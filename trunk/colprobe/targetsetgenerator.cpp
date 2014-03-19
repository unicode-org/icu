#include "targetsetgenerator.h"

TargetSetGenerator::TargetSetGenerator(UnicodeSet &startingSet, CompareFn comparer) :
 comparer(comparer),
 set(startingSet)
{
   addAll(startingSet);
}
