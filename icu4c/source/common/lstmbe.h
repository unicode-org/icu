// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef LSTMBE_H
#define LSTMBE_H

#include "unicode/utypes.h"
#include "unicode/uniset.h"
#include "unicode/utext.h"

#include "dictbe.h"
#include "brkeng.h"
#include "uvectr32.h"

U_NAMESPACE_BEGIN

class LSTMData;
class Vectorizer;

class LSTMBreakEngine : public DictionaryBreakEngine {
 private:
  const LSTMData* fData;
  const Vectorizer* fVectorizer;
 public:
  /**
   * <p>Default constructor.</p>
   */
  LSTMBreakEngine(const UnicodeString& model, const UnicodeString& set, UErrorCode &status);

  /**
   * <p>Virtual destructor.</p>
   */
  virtual ~LSTMBreakEngine();

  virtual const UChar* name() const;
 protected:
 /**
  * <p>Divide up a range of known dictionary characters handled by this break engine.</p>
  *
  * @param text A UText representing the text
  * @param rangeStart The start of the range of dictionary characters
  * @param rangeEnd The end of the range of dictionary characters
  * @param foundBreaks Output of C array of int32_t break positions, or 0
  * @return The number of breaks found
  */
  virtual int32_t divideUpDictionaryRange( UText *text,
                                           int32_t rangeStart,
                                           int32_t rangeEnd,
                                           UVector32 &foundBreaks ) const;
};

class ThaiLSTMBreakEngine : public LSTMBreakEngine {
 public:
  ThaiLSTMBreakEngine(const UnicodeString& name, UErrorCode &status);
  virtual ~ThaiLSTMBreakEngine() {};
};

class BurmeseLSTMBreakEngine : public LSTMBreakEngine {
 public:
  BurmeseLSTMBreakEngine(const UnicodeString& name,UErrorCode &status);
  virtual ~BurmeseLSTMBreakEngine() {};
};


U_NAMESPACE_END

#endif  /* LSTMBE_H */
