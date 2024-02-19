// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
//
/**
 * A LSTMBreakEngine
 */
package com.ibm.icu.impl.breakiter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.UResourceBundle;

/**
 * @internal
 */
public class LSTMBreakEngine extends DictionaryBreakEngine {
    public enum EmbeddingType {
      UNKNOWN,
      CODE_POINTS,
      GRAPHEME_CLUSTER
    }

    public enum LSTMClass {
      BEGIN,
      INSIDE,
      END,
      SINGLE,
    }

    private static float[][] make2DArray(int[] data, int start, int d1, int d2) {
        byte[] bytes = new byte[4];
        float [][] result = new float[d1][d2];
        for (int i = 0; i < d1 ; i++) {
            for (int j = 0; j < d2 ; j++) {
                int d = data[start++];
                bytes[0] = (byte) (d >> 24);
                bytes[1] = (byte) (d >> 16);
                bytes[2] = (byte) (d >> 8);
                bytes[3] = (byte) (d /*>> 0*/);
                result[i][j] = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
            }
        }
        return result;
    }

    private static float[] make1DArray(int[] data, int start, int d1) {
        byte[] bytes = new byte[4];
        float [] result = new float[d1];
        for (int i = 0; i < d1 ; i++) {
            int d = data[start++];
            bytes[0] = (byte) (d >> 24);
            bytes[1] = (byte) (d >> 16);
            bytes[2] = (byte) (d >> 8);
            bytes[3] = (byte) (d /*>> 0*/);
            result[i] = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
        }
        return result;
    }

    /** @internal */
    public static class LSTMData {
        private LSTMData() {
        }

        public LSTMData(UResourceBundle rb) {
            int embeddings = rb.get("embeddings").getInt();
            int hunits = rb.get("hunits").getInt();
            this.fType = EmbeddingType.UNKNOWN;
            this.fName = rb.get("model").getString();
            String typeString = rb.get("type").getString();
            if (typeString.equals("codepoints")) {
                this.fType = EmbeddingType.CODE_POINTS;
            } else if (typeString.equals("graphclust")) {
                this.fType = EmbeddingType.GRAPHEME_CLUSTER;
            }
            String[] dict = rb.get("dict").getStringArray();
            int[] data = rb.get("data").getIntVector();
            int dataLen = data.length;
            int numIndex = dict.length;
            fDict = new HashMap<String, Integer>(numIndex + 1);
            int idx = 0;
            for (String embedding : dict){
                fDict.put(embedding, idx++);
            }
            int mat1Size = (numIndex + 1) * embeddings;
            int mat2Size = embeddings * 4 * hunits;
            int mat3Size = hunits * 4 * hunits;
            int mat4Size = 4 * hunits;
            int mat5Size = mat2Size;
            int mat6Size = mat3Size;
            int mat7Size = mat4Size;
            int mat8Size = 2 * hunits * 4;
            int mat9Size = 4;
            assert dataLen == mat1Size + mat2Size + mat3Size + mat4Size + mat5Size + mat6Size + mat7Size + mat8Size + mat9Size;
            int start = 0;
            this.fEmbedding = make2DArray(data, start, (numIndex+1), embeddings);
            start += mat1Size;
            this.fForwardW = make2DArray(data, start, embeddings, 4 * hunits);
            start += mat2Size;
            this.fForwardU = make2DArray(data, start, hunits, 4 * hunits);
            start += mat3Size;
            this.fForwardB = make1DArray(data, start, 4 * hunits);
            start += mat4Size;
            this.fBackwardW = make2DArray(data, start, embeddings, 4 * hunits);
            start += mat5Size;
            this.fBackwardU = make2DArray(data, start, hunits, 4 * hunits);
            start += mat6Size;
            this.fBackwardB = make1DArray(data, start, 4 * hunits);
            start += mat7Size;
            this.fOutputW = make2DArray(data, start, 2 * hunits, 4);
            start += mat8Size;
            this.fOutputB = make1DArray(data, start, 4);
        }

        public EmbeddingType fType;
        public String fName;
        public Map<String, Integer> fDict;
        public float fEmbedding[][];
        public float fForwardW[][];
        public float fForwardU[][];
        public float fForwardB[];
        public float fBackwardW[][];
        public float fBackwardU[][];
        public float fBackwardB[];
        public float fOutputW[][];
        public float fOutputB[];
    }

    // Minimum word size
    private static final byte MIN_WORD = 2;

    // Minimum number of characters for two words
    private static final byte MIN_WORD_SPAN = MIN_WORD * 2;

    abstract class Vectorizer {
        public Vectorizer(Map<String, Integer> dict) {
            this.fDict = dict;
        }
        abstract public void vectorize(CharacterIterator fIter, int rangeStart, int rangeEnd,
                              List<Integer> offsets, List<Integer> indicies);
        protected int getIndex(String token) {
            Integer res = fDict.get(token);
            return (res == null) ? fDict.size() : res;
        }
        private Map<String, Integer> fDict;
    }

    class CodePointsVectorizer extends Vectorizer {
        public CodePointsVectorizer(Map<String, Integer> dict) {
            super(dict);
        }

        public void vectorize(CharacterIterator fIter, int rangeStart, int rangeEnd,
                              List<Integer> offsets, List<Integer> indicies) {
            fIter.setIndex(rangeStart);
            for (char c = fIter.current();
                 c != CharacterIterator.DONE && fIter.getIndex() < rangeEnd;
                 c = fIter.next()) {
                offsets.add(fIter.getIndex());
                indicies.add(getIndex(String.valueOf(c)));
            }
        }
    }

    class GraphemeClusterVectorizer extends Vectorizer {
        public GraphemeClusterVectorizer(Map<String, Integer> dict) {
            super(dict);
        }

        private String substring(CharacterIterator text, int startPos, int endPos) {
            int saved = text.getIndex();
            text.setIndex(startPos);
            StringBuilder sb = new StringBuilder();
            for (char c = text.current();
                 c != CharacterIterator.DONE && text.getIndex() < endPos;
                 c = text.next()) {
                sb.append(c);
            }
            text.setIndex(saved);
            return sb.toString();
        }

        public void vectorize(CharacterIterator text, int startPos, int endPos,
                              List<Integer> offsets, List<Integer> indicies) {
            BreakIterator iter = BreakIterator.getCharacterInstance();
            iter.setText(text);
            int last = iter.next(startPos);
            for (int curr = iter.next(); curr != BreakIterator.DONE && curr <= endPos; curr = iter.next()) {
                offsets.add(last);
                String segment = substring(text, last, curr);
                int index = getIndex(segment);
                indicies.add(index);
                last = curr;
            }
        }
    }

    private final LSTMData fData;
    private int fScript;
    private final Vectorizer fVectorizer;

    private Vectorizer makeVectorizer(LSTMData data) {
        switch(data.fType) {
            case CODE_POINTS:
                return new CodePointsVectorizer(data.fDict);
            case GRAPHEME_CLUSTER:
                return new GraphemeClusterVectorizer(data.fDict);
            default:
                return null;
        }
    }

    public LSTMBreakEngine(int script, UnicodeSet set, LSTMData data) {
        setCharacters(set);
        this.fScript = script;
        this.fData = data;
        this.fVectorizer = makeVectorizer(this.fData);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean handles(int c) {
        return fScript == UCharacter.getIntPropertyValue(c, UProperty.SCRIPT);
    }

    static private void addDotProductTo(final float [] a, final float[][] b, float[] result) {
        assert a.length == b.length;
        assert b[0].length == result.length;
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < a.length; j++) {
                result[i] += a[j] * b[j][i];
            }
        }
    }

    static private void addTo(final float [] a, float[] result) {
        assert a.length == result.length;
        for (int i = 0; i < result.length; i++) {
            result[i] += a[i];
        }
    }

    static private void hadamardProductTo(final float [] a, float[] result) {
        assert a.length == result.length;
        for (int i = 0; i < result.length; i++) {
            result[i] *= a[i];
        }
    }

    static private void addHadamardProductTo(final float [] a, final float [] b, float[] result) {
        assert a.length == result.length;
        assert b.length == result.length;
        for (int i = 0; i < result.length; i++) {
            result[i] += a[i] * b[i];
        }
    }

    static private void sigmoid(float [] result, int start, int length) {
        assert start < result.length;
        assert start + length <= result.length;
        for (int i = start; i < start + length; i++) {
            result[i] = (float)(1.0/(1.0 + Math.exp(-result[i])));
        }
    }

    static private void tanh(float [] result, int start, int length) {
        assert start < result.length;
        assert start + length <= result.length;
        for (int i = start; i < start + length; i++) {
            result[i] = (float)Math.tanh(result[i]);
        }
    }

    static private int maxIndex(float [] data) {
        int index = 0;
        float max = data[0];
        for (int i = 1; i < data.length; i++) {
            if (data[i] > max) {
                max = data[i];
                index = i;
            }
        }
        return index;
    }

    /*
    static private void print(float [] data) {
        for (int i=0; i < data.length; i++) {
            System.out.format("  %e", data[i]);
            if (i % 4 == 3) {
              System.out.println();
            }
        }
        System.out.println();
    }
    */

    private float[] compute(final float[][] W, final float[][] U, final float[] B,
                            final float[] x, float[] h, float[] c) {
        // ifco = x * W + h * U + b
        float[] ifco = Arrays.copyOf(B, B.length);
        addDotProductTo(x, W, ifco);
        float[] hU = new float[B.length];
        addDotProductTo(h, U, ifco);

        int hunits = B.length / 4;
        sigmoid(ifco, 0*hunits, hunits);  // i
        sigmoid(ifco, 1*hunits, hunits);  // f
        tanh(ifco, 2*hunits, hunits);  // c_
        sigmoid(ifco, 3*hunits, hunits);  // o

        hadamardProductTo(Arrays.copyOfRange(ifco, hunits, 2*hunits), c);
        addHadamardProductTo(Arrays.copyOf(ifco, hunits),
            Arrays.copyOfRange(ifco, 2*hunits, 3*hunits), c);

        h = Arrays.copyOf(c, c.length);
        tanh(h, 0, h.length);
        hadamardProductTo(Arrays.copyOfRange(ifco, 3*hunits, 4*hunits), h);
        // System.out.println("c");
        // print(c);
        // System.out.println("h");
        // print(h);
        return h;
    }

    @Override
    public int divideUpDictionaryRange(CharacterIterator fIter, int rangeStart, int rangeEnd,
            DequeI foundBreaks, boolean isPhraseBreaking) {
        int beginSize = foundBreaks.size();

        if ((rangeEnd - rangeStart) < MIN_WORD_SPAN) {
            return 0;  // Not enough characters for word
        }
        List<Integer> offsets = new ArrayList<Integer>(rangeEnd - rangeStart);
        List<Integer> indicies = new ArrayList<Integer>(rangeEnd - rangeStart);

        fVectorizer.vectorize(fIter, rangeStart, rangeEnd, offsets, indicies);

        // To save the needed memory usage, the following is different from the
        // Python or ICU4X implementation. We first perform the Backward LSTM
        // and then merge the iteration of the forward LSTM and the output layer
        // together because we only need to remember the h[t-1] for Forward LSTM.
        int inputSeqLength = indicies.size();
        int hunits = this.fData.fForwardU.length;
        float c[] = new float[hunits];

        // TODO: limit size of hBackward. If input_seq_len is too big, we could
        // run out of memory.
        // Backward LSTM
        float hBackward[][] = new float[inputSeqLength][hunits];
        for (int i = inputSeqLength - 1; i >= 0;  i--) {
            if (i != inputSeqLength - 1) {
                hBackward[i] = Arrays.copyOf(hBackward[i+1], hunits);
            }
            // System.out.println("Backward LSTM " + i);
            hBackward[i] = compute(this.fData.fBackwardW, this.fData.fBackwardU, this.fData.fBackwardB,
                    this.fData.fEmbedding[indicies.get(i)],
                    hBackward[i], c);
        }

        c = new float[hunits];
        float forwardH[] = new float[hunits];
        float both[] = new float[2*hunits];

        // The following iteration merge the forward LSTM and the output layer
        // together.
        for (int i = 0 ; i < inputSeqLength; i++) {
            // Forward LSTM
            forwardH = compute(this.fData.fForwardW, this.fData.fForwardU, this.fData.fForwardB,
                    this.fData.fEmbedding[indicies.get(i)],
                    forwardH, c);

            System.arraycopy(forwardH, 0, both, 0, hunits);
            System.arraycopy(hBackward[i], 0, both, hunits, hunits);

            //System.out.println("Merged " + i);
            //print(both);

            // Output layer
            // logp = fbRow * fOutputW + fOutputB
            float logp[] = Arrays.copyOf(this.fData.fOutputB, this.fData.fOutputB.length);
            addDotProductTo(both, this.fData.fOutputW, logp);

            int current = maxIndex(logp);

            // BIES logic.
            if (current == LSTMClass.BEGIN.ordinal() ||
                current == LSTMClass.SINGLE.ordinal()) {
                if (i != 0) {
                    foundBreaks.push(offsets.get(i));
                }
            }
        }

        return foundBreaks.size() - beginSize;
    }

    public static LSTMData createData(UResourceBundle bundle) {
        return new LSTMData(bundle);
    }

    private static String defaultLSTM(int script) {
        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUData.ICU_BRKITR_BASE_NAME);
        return rb.getStringWithFallback("lstm/" + UScript.getShortName(script));
    }

    public static LSTMData createData(int script) {
        if (script != UScript.KHMER && script != UScript.LAO && script != UScript.MYANMAR && script != UScript.THAI) {
            return null;
        }
        String name = defaultLSTM(script);
        name = name.substring(0, name.indexOf("."));

        UResourceBundle rb = UResourceBundle.getBundleInstance(
             ICUData.ICU_BRKITR_BASE_NAME, name,
             ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        return createData(rb);
    }

    public static LSTMBreakEngine create(int script, LSTMData data) {
        String setExpr = "[[:" + UScript.getShortName(script) + ":]&[:LineBreak=SA:]]";
        UnicodeSet set = new UnicodeSet();
        set.applyPattern(setExpr);
        set.compact();
        return new LSTMBreakEngine(script, set, data);
    }
}
