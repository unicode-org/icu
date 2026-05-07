// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#ifndef TARGETSETGENERATOR_H
#define TARGETSETGENERATOR_H

#include "colprobe.h"
#include "unicode/uniset.h"

class TargetSetGenerator : public UnicodeSet {
public:
  TargetSetGenerator(UnicodeSet &startingSet, CompareFn comparer);
private:
  CompareFn comparer;
  UnicodeSet set;
};

#endif