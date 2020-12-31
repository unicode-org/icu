// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <utility>
#include <ctgmath>

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "brkeng.h"
#include "charstr.h"
#include "cmemory.h"
#include "lstmbe.h"
#include "uassert.h"
#include "ubrkimpl.h"
#include "uresimp.h"
#include "uvectr32.h"
#include "uvector.h"

#include "unicode/brkiter.h"
#include "unicode/resbund.h"
#include "unicode/ubrk.h"
#include "unicode/uniset.h"
#include "unicode/ustring.h"

U_NAMESPACE_BEGIN


ThaiLSTMBreakEngine::ThaiLSTMBreakEngine(const UnicodeString& name, UErrorCode &status)
    : LSTMBreakEngine(name, UnicodeString(u"[[:Thai:]&[:LineBreak=SA:]]"),
                      status)
{
}

BurmeseLSTMBreakEngine::BurmeseLSTMBreakEngine(const UnicodeString& name, UErrorCode &status)
    : LSTMBreakEngine(name, UnicodeString(u"[[:Mymr:]&[:LineBreak=SA:]]"),
                      status)
{
}


/**
 * Interface for reading 1D array.
 */
class ReadArray1D {
 public:
    virtual int32_t d1() const = 0;
    virtual float get(int32_t i) const = 0;

    inline void print() const {
        for (int32_t i = 0; i < d1(); i++) {
            printf("  %e", get(i));
            if (i % 4 == 3) {
                printf("\n");
            }
        }
        printf("\n");
    }
};

/**
 * Interface for reading 2D array.
 */
class ReadArray2D {
 public:
    virtual int32_t d1() const = 0;
    virtual int32_t d2() const = 0;
    virtual float get(int32_t i, int32_t j) const = 0;
};

/**
 * A class to index a float array as a 1D Array without owning the pointer or
 * copy the data.
 */
class ConstArray1D : public ReadArray1D {
 public:
    ConstArray1D() : data_(nullptr), d1_(0) {}

    ConstArray1D(const float* data, int32_t d1) : data_(data), d1_(d1) {}

    virtual ~ConstArray1D() {}

    // Init the object, the object does not own the data nor copy.
    // It is designed to directly use data from memory mapped resources.
    void init(const int32_t* data, int32_t d1) {
        data_ = reinterpret_cast<const float*>(data);
        d1_ = d1;
    }

    // ReadArray1D methods.
    virtual int32_t d1() const { return d1_; }
    virtual float get(int32_t i) const {
        U_ASSERT(i < d1_);
      return data_[i];
    }

 private:
    const float* data_;
    int32_t d1_;
};

/**
 * A class to index a float array as a 2D Array without owning the pointer or
 * copy the data.
 */
class ConstArray2D : public ReadArray2D {
 public:
    ConstArray2D() : data_(nullptr), d1_(0), d2_(0) {}

    ConstArray2D(const float* data, int32_t d1, int32_t d2)
        : data_(data), d1_(d1), d2_(d2) {}

    virtual ~ConstArray2D() { }

    // Init the object, the object does not own the data nor copy.
    // It is designed to directly use data from memory mapped resources.
    void init(const int32_t* data, int32_t d1, int32_t d2) {
        data_ = reinterpret_cast<const float*>(data);
        d1_ = d1;
        d2_ = d2;
    }

    // ReadArray2D methods.
    inline int32_t d1() const { return d1_; }
    inline int32_t d2() const { return d2_; }
    float get(int32_t i, int32_t j) const {
        U_ASSERT(i < d1_);
        U_ASSERT(j < d2_);
      return data_[i * d2_ + j];
    }

    // Expose the ith row as a ConstArray1D
    inline ConstArray1D row(int32_t i) const {
      U_ASSERT(i < d1_);
      return ConstArray1D(data_ + i * d2_, d2_);
    }

   private:
    const float* data_;
    int32_t d1_;
    int32_t d2_;
};

/**
 * A class to allocate data as a writable 1D array.
 * This is the main class implement matrix operation.
 */
class Array1D : public ReadArray1D {
 public:
    Array1D() : memory_(nullptr), data_(nullptr), d1_(0) {}
    Array1D(int32_t d1)
        : memory_(uprv_malloc(d1 * sizeof(float))),
          data_((float*)memory_), d1_(d1) {
      clear();
    }
    virtual ~Array1D() {
        uprv_free(memory_);
    }

    // A special constructor which does not own the memory but writeable
    // as a slice of an array.
    Array1D(float* data, int32_t d1)
        : memory_(nullptr), data_(data), d1_(d1) {}

    // ReadArray1D methods.
    virtual int32_t d1() const { return d1_; }
    virtual float get(int32_t i) const {
        U_ASSERT(i < d1_);
        return data_[i];
    }

    // Return the index which point to the max data in the array.
    inline int32_t maxIndex() const {
        int32_t index = 0;
        float max = data_[0];
        for (int32_t i = 1; i < d1_; i++) {
            if (data_[i] > max) {
                max = data_[i];
                index = i;
            }
        }
        return index;
    }

    // Slice part of the array to a new one.
    inline Array1D slice(int32_t from, int32_t size) const {
        U_ASSERT(from >= 0);
        U_ASSERT(from < d1_);
        U_ASSERT(from + size <= d1_);
        return Array1D(data_ + from, size);
    }

    // Dot product of a 1D array and a 2D array into this one.
    inline Array1D& dotProduct(const ReadArray1D& a, const ReadArray2D& b) {
        U_ASSERT(a.d1() == b.d1());
        U_ASSERT(b.d2() == d1());
        for (int32_t i = 0; i < d1(); i++) {
            data_[i] = 0;
            for (int32_t j = 0; j < a.d1(); j++) {
                data_[i] += a.get(j) * b.get(j, i);
            }
        }
        return *this;
    }

    // Hadamard Product the values of another array of the same size into this one.
    inline Array1D& hadamardProduct(const ReadArray1D& a) {
        U_ASSERT(a.d1() == d1());
        for (int32_t i = 0; i < d1(); i++) {
            data_[i] *= a.get(i);
        }
        return *this;
    }

    // Add the values of another array of the same size into this one.
    inline Array1D& add(const ReadArray1D& a) {
        U_ASSERT(a.d1() == d1());
        for (int32_t i = 0; i < d1(); i++) {
            data_[i] += a.get(i);
        }
        return *this;
    }

    // Assign the values of another array of the same size into this one.
    inline Array1D& assign(const ReadArray1D& a) {
        U_ASSERT(a.d1() == d1());
        for (int32_t i = 0; i < d1(); i++) {
            data_[i] = a.get(i);
        }
        return *this;
    }

    // Apply tanh to all the elements in the array.
    inline Array1D& tanh() {
        for (int32_t i = 0; i < d1_; i++) {
            data_[i] = std::tanh(data_[i]);
        }
        return *this;
    }

    // Apply sigmoid to all the elements in the array.
    inline Array1D& sigmoid() {
        for (int32_t i = 0; i < d1_; i++) {
            data_[i] = 1.0/(1.0 + exp(-data_[i]));
        }
        return *this;
    }

    inline Array1D& clear() {
        uprv_memset(data_, 0, d1_ * sizeof(float));
        return *this;
    }

 private:
    void* memory_;
    float* data_;
    int32_t d1_;
};

class Array2D : public ReadArray2D {
 public:
    Array2D() : memory_(nullptr), data_(nullptr), d1_(0), d2_(0) {}
    Array2D(int32_t d1, int32_t d2)
        : memory_(uprv_malloc(d1 * d2 * sizeof(float))),
          data_((float*)memory_), d1_(d1), d2_(d2) {
        clear();
    }
    virtual ~Array2D() {
        uprv_free(memory_);
    }

    // ReadArray2D methods.
    virtual int32_t d1() const { return d1_; }
    virtual int32_t d2() const { return d2_; }
    virtual float get(int32_t i, int32_t j) const {
        U_ASSERT(i < d1_);
        U_ASSERT(j < d2_);
        return data_[i * d2_ + j];
    }

    inline Array1D row(int32_t i) const {
        U_ASSERT(i < d1_);
        return Array1D(data_ + i * d2_, d2_);
    }

    inline Array2D& clear() {
        uprv_memset(data_, 0, d1_ * d2_ * sizeof(float));
        return *this;
    }

 private:
    void* memory_;
    float* data_;
    int32_t d1_;
    int32_t d2_;
};

typedef enum {
    UNKNOWN,
    CODE_POINTS,
    GRAPHEME_CLUSTER,
} EmbeddingType;

typedef enum {
    BEGIN,
    INSIDE,
    END,
    SINGLE
} LSTMClass;

class LSTMData : public UMemory {
 public:
    LSTMData(const UnicodeString& name, UErrorCode &status);
    virtual ~LSTMData();
    UHashtable* GetDictionary() const {
        return fDict;
    }

 private:
    UResourceBundle* fDataRes;
    UResourceBundle* fDictRes;
    UHashtable* fDict;

 public:
    EmbeddingType fType;
    const UChar* fName;
    ConstArray2D fEmbedding;
    ConstArray2D fForwardW;
    ConstArray2D fForwardU;
    ConstArray1D fForwardB;
    ConstArray2D fBackwardW;
    ConstArray2D fBackwardU;
    ConstArray1D fBackwardB;
    ConstArray2D fOutputW;
    ConstArray1D fOutputB;
};

LSTMData::LSTMData(const UnicodeString& name, UErrorCode &status)
    : fDataRes(nullptr), fDictRes(nullptr), fDict(nullptr),
    fType(UNKNOWN), fName(nullptr)
{
    if (U_FAILURE(status)) {
        return;
    }
    CharString namebuf;
    namebuf.appendInvariantChars(name, status).truncate(namebuf.lastIndexOf('.'));

    LocalUResourceBundlePointer rb(
        ures_openDirect(U_ICUDATA_BRKITR, namebuf.data(), &status));
    LocalUResourceBundlePointer embeddings_res(
        ures_getByKey(rb.getAlias(), "embeddings", nullptr, &status));
    int32_t embedding_size = ures_getInt(embeddings_res.getAlias(), &status);
    LocalUResourceBundlePointer hunits_res(
        ures_getByKey(rb.getAlias(), "hunits", nullptr, &status));
    int32_t hunits = ures_getInt(hunits_res.getAlias(), &status);
    const UChar* type = ures_getStringByKey(rb.getAlias(), "type", nullptr, &status);
    if (u_strCompare(type, -1, u"codepoints", -1, false) == 0) {
      fType = CODE_POINTS;
    } else if (u_strCompare(type, -1, u"graphclust", -1, false) == 0) {
      fType = GRAPHEME_CLUSTER;
    }
    fName = ures_getStringByKey(rb.getAlias(), "model", nullptr, &status);
    fDataRes = ures_getByKey(rb.getAlias(), "data", nullptr, &status);
    int32_t data_len = 0;
    const int32_t* data = ures_getIntVector(fDataRes, &data_len, &status);
    LocalUResourceBundlePointer fDictRes(
        ures_getByKey(rb.getAlias(), "dict", nullptr, &status));
    int32_t num_index = ures_getSize(fDictRes.getAlias());
    fDict = uhash_open(uhash_hashUChars, uhash_compareUChars, nullptr, &status);
    if (U_FAILURE(status)) {
        return;
    }

    ures_resetIterator(fDictRes.getAlias());
    int32_t idx = 0;
    // put dict into hash
    while(ures_hasNext(fDictRes.getAlias())) {
        const char *tempKey = nullptr;
        const UChar* str = ures_getNextString(fDictRes.getAlias(), nullptr, &tempKey, &status);
        uhash_puti(fDict, (void*)str, idx++, &status);
        if (U_FAILURE(status)) {
            return;
        }
    }
    int32_t mat1_size = (num_index + 1) * embedding_size;
    int32_t mat2_size = embedding_size * 4 * hunits;
    int32_t mat3_size = hunits * 4 * hunits;
    int32_t mat4_size = 4 * hunits;
    int32_t mat5_size = mat2_size;
    int32_t mat6_size = mat3_size;
    int32_t mat7_size = mat4_size;
    int32_t mat8_size = 2 * hunits * 4;
    int32_t mat9_size = 4;
    U_ASSERT(data_len == mat1_size + mat2_size + mat3_size + mat4_size + mat5_size +
        mat6_size + mat7_size + mat8_size + mat9_size);

    fEmbedding.init(data, (num_index + 1), embedding_size);
    data += mat1_size;
    fForwardW.init(data, embedding_size, 4 * hunits);
    data += mat2_size;
    fForwardU.init(data, hunits, 4 * hunits);
    data += mat3_size;
    fForwardB.init(data, 4 * hunits);
    data += mat4_size;
    fBackwardW.init(data, embedding_size, 4 * hunits);
    data += mat5_size;
    fBackwardU.init(data, hunits, 4 * hunits);
    data += mat6_size;
    fBackwardB.init(data, 4 * hunits);
    data += mat7_size;
    fOutputW.init(data, 2 * hunits, 4);
    data += mat8_size;
    fOutputB.init(data, 4);
}

LSTMData::~LSTMData() {
    uhash_close(fDict);
    ures_close(fDictRes);
    ures_close(fDataRes);
}

class Vectorizer : public UMemory {
 public:
    Vectorizer(UHashtable* dict) : dict(dict) {}
    virtual ~Vectorizer() {}
    virtual void vectorize(UText *text, int32_t startPos, int32_t endPos,
                           UVector32 &offsets, UVector32 &indices,
                           UErrorCode &status) const = 0;
 protected:
    int32_t stringToIndex(const UChar* str) const {
      return uhash_geti(dict, (const void*)str);
    }

 private:
    UHashtable* dict;
};


class CodePointsVectorizer : public Vectorizer {
 public:
    CodePointsVectorizer(UHashtable* dict) : Vectorizer(dict) {}
    virtual ~CodePointsVectorizer() {}
    virtual void vectorize(UText *text, int32_t startPos, int32_t endPos,
                           UVector32 &offsets, UVector32 &indices,
                           UErrorCode &status) const;
};

void CodePointsVectorizer::vectorize(
    UText *text, int32_t startPos, int32_t endPos,
    UVector32 &offsets, UVector32 &indices, UErrorCode &status) const
{
    if (offsets.ensureCapacity(endPos - startPos, status) &&
            indices.ensureCapacity(endPos - startPos, status)) {
        utext_setNativeIndex(text, startPos);
        int32_t current;
        UChar str[2] = {0, 0};
        while (U_SUCCESS(status) &&
               (current = (int32_t)utext_getNativeIndex(text)) < endPos) {
            str[0] = (UChar) utext_next32(text);
            offsets.addElement(current, status);
            indices.addElement(stringToIndex(str), status);
        }
    }
}

class GraphemeClusterVectorizer : public Vectorizer {
 public:
    GraphemeClusterVectorizer(UHashtable* dict) : Vectorizer(dict) {}
    virtual ~GraphemeClusterVectorizer() {}
    virtual void vectorize(UText *text, int32_t startPos, int32_t endPos,
                           UVector32 &offsets, UVector32 &indices,
                           UErrorCode &status) const;
};

#define MAX_GRAPHEME_CLSTER_LENTH 10
void GraphemeClusterVectorizer::vectorize(
    UText *text, int32_t startPos, int32_t endPos,
    UVector32 &offsets, UVector32 &indices, UErrorCode &status) const
{
    if (offsets.ensureCapacity(endPos - startPos, status) &&
            indices.ensureCapacity(endPos - startPos, status)) {
        LocalPointer<BreakIterator> graphemeIter(
            BreakIterator::createCharacterInstance(Locale(), status));
        graphemeIter->setText(text, status);

        if (startPos != 0) {
          graphemeIter->preceding(startPos);
        }
        int32_t last = startPos;
        int32_t current = startPos;
        UChar str[MAX_GRAPHEME_CLSTER_LENTH];
        while ((current = graphemeIter->next()) != BreakIterator::DONE) {
          if (current >= endPos) {
              break;
          }
          if (current > startPos) {
              utext_extract(text, last, current,
                            str, MAX_GRAPHEME_CLSTER_LENTH, &status);
              if (U_FAILURE(status)) {
                  break;
              }
              offsets.addElement(last, status);
              indices.addElement(stringToIndex(str), status);
          }
          last = current;
        }
        if (U_SUCCESS(status) && last < endPos) {
            utext_extract(text, last, endPos,
                          str, MAX_GRAPHEME_CLSTER_LENTH, &status);
            if (U_SUCCESS(status)) {
                offsets.addElement(last, status);
                indices.addElement(stringToIndex(str), status);
            }
        }
    }
}

void compute(
    const ReadArray2D& W, const ReadArray2D& U, const ReadArray1D& b,
    const ReadArray1D& x, Array1D& h, Array1D& c)
{
    // ifco = x * W + h * U + b
    Array1D ifco(b.d1());
    ifco.dotProduct(x, W)
        .add(Array1D(b.d1()).dotProduct(h, U))
        .add(b);

    int32_t hunits = b.d1() / 4;
    ifco.slice(0*hunits, hunits).sigmoid();  // i: sigmod
    ifco.slice(1*hunits, hunits).sigmoid(); // f: sigmoid
    ifco.slice(2*hunits, hunits).tanh(); // c_: tanh
    ifco.slice(3*hunits, hunits).sigmoid(); // o: sigmod

    c.hadamardProduct(ifco.slice(hunits, hunits))
        .add(Array1D(c.d1())
             .assign(ifco.slice(0, hunits))
             .hadamardProduct(ifco.slice(2*hunits, hunits)));

    h.assign(c)
        .tanh()
        .hadamardProduct(ifco.slice(3*hunits, hunits));
}

// Minimum word size
static const int32_t MIN_WORD = 2;

// Minimum number of characters for two words
static const int32_t MIN_WORD_SPAN = MIN_WORD * 2;

int32_t
LSTMBreakEngine::divideUpDictionaryRange( UText *text,
                                                int32_t startPos,
                                                int32_t endPos,
                                                UVector32 &foundBreaks ) const {
    utext_setNativeIndex(text, startPos);
    utext_moveIndex32(text, MIN_WORD_SPAN);
    if (utext_getNativeIndex(text) >= endPos) {
        return 0;       // Not enough characters for two words
    }
    utext_setNativeIndex(text, startPos);
    UErrorCode status = U_ZERO_ERROR;

    UVector32 offsets(status);
    UVector32 indices(status);
    fVectorizer->vectorize(text, startPos, endPos, offsets, indices, status);
    int32_t* offsetsBuf = offsets.getBuffer();
    int32_t* indicesBuf = indices.getBuffer();

    int32_t input_seq_len = indices.size();
    int32_t hunits = fData->fForwardU.d1();

    // printf("len %d\n", input_seq_len);
    // To save the needed memory usage, the following is different from the
    // Python or ICU4X implementation. We first perform the Backward LSTM
    // and then merge the iteration of the forward LSTM and the output layer
    // together because we only neetdto remember the h[t-1] for Forward LSTM.
    Array1D c(hunits);

    // TODO: limit size of hBackward. If input_seq_len is too big, we could
    // run out of memory.
    // Backward LSTM
    Array2D hBackward(input_seq_len, hunits);
    for (int32_t i = input_seq_len - 1; i >= 0; i--) {
        Array1D hRow = hBackward.row(i);
        if (i != input_seq_len - 1) {
            hRow.assign(hBackward.row(i+1));
        }
        compute(fData->fBackwardW, fData->fBackwardU, fData->fBackwardB,
                fData->fEmbedding.row(indicesBuf[i]),
                hRow, c);
        // printf("Backward %d %d\n", i, indicesBuf[i]);
        // printf("h\n");
        // hRow.print();
        // printf("c\n");
        // c.print();
    }

    Array1D logp(4);
    bool breakOnNext = true;

    // Allocate fbRow and slice the internal array in two.
    Array1D fbRow(2 * hunits);
    Array1D forwardRow = fbRow.slice(0, hunits);  // point to first half of data in fbRow.
    Array1D backwardRow = fbRow.slice(hunits, hunits);  // point to second half of data n fbRow.

    // The following iteration merge the forward LSTM and the output layer
    // together.
    c.clear();  // reuse c since it is the same size.
    for (int32_t i = 0; i < input_seq_len; i++) {
        // printf("Grapheme ID for  %d %d\n", i, indicesBuf[i]);
        // Forward LSTM
        // Calculate the result into forwardRow, which point to the data in the first half
        // of fbRow.
        compute(fData->fForwardW, fData->fForwardU, fData->fForwardB,
                fData->fEmbedding.row(indicesBuf[i]),
                forwardRow, c);

        // assign the data from hBackward.row(i) to second half of fbRowa.
        backwardRow.assign(hBackward.row(i));

        //printf("final %d\n", i);
        //fbRow.print();
        // Output layer
        // logp = fbRow * fOutputW + fOutputB
        logp.dotProduct(fbRow, fData->fOutputW).add(fData->fOutputB);

        //printf("logp %d\n", i);
        //logp.print();

        // current = argmax(logp)
        LSTMClass current = (LSTMClass)logp.maxIndex();
        // const char* bies = "bies";
        // printf("%d %c offset=%d\n", i, bies[(int)current], offsetsBuf[i]);
        // printf("%c", bies[(int)current]);

        // BIES logic.
        if (breakOnNext || current == BEGIN || current == SINGLE) {
            if (i != 0) {
                foundBreaks.addElement(offsetsBuf[i], status);
            }
        }
        breakOnNext = (current == END || current == SINGLE);
    }
    return foundBreaks.size();
}

Vectorizer* createVectorizer(const LSTMData* data, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return nullptr;
    }
    switch (data->fType) {
      case CODE_POINTS:
        return new CodePointsVectorizer(data->GetDictionary());
        break;
      case GRAPHEME_CLUSTER:
        return new GraphemeClusterVectorizer(data->GetDictionary());
        break;
      default:
        break;
    }
    UPRV_UNREACHABLE;
}

LSTMBreakEngine::LSTMBreakEngine(const UnicodeString& name, const UnicodeString& set, UErrorCode &status)
    : DictionaryBreakEngine(),
      fData(new LSTMData(name, status)),
      fVectorizer(createVectorizer(fData, status))
{
    UnicodeSet unicodeSet;
    unicodeSet.applyPattern(set, status);
    if (U_SUCCESS(status)) {
      setCharacters(unicodeSet);
    }
}

LSTMBreakEngine::~LSTMBreakEngine() {
    delete fData;
    delete fVectorizer;
}

const UChar* LSTMBreakEngine::name() const {
    return fData->fName;
}



U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

