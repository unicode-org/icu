package com.ibm.icu.impl.units;

import com.ibm.icu.impl.UResource;

//
//public class ReadUnitsData {
//
//    public ReadUnitsData() {
//
//
//    }
//
//
//
//    /**
//     * Constants for path names for the data bundles.
//     */
//    private static final String LATIN_NUMBERING_SYSTEM = "latn";
//    private static final String NUMBER_ELEMENTS = "NumberElements";
//    private static final String SYMBOLS = "symbols";
//
//    /**
//     * Sink for enumerating all of the decimal format symbols (more specifically, anything
//     * under the "NumberElements.symbols" tree).
//     *
//     * More specific bundles (en_GB) are enumerated before their parents (en_001, en, root):
//     * Only store a value if it is still missing, that is, it has not been overridden.
//     */
//    private static final class UnitsDataSink extends UResource.Sink {
//
//        private String[] numberElements; // Array where to store the characters (set in constructor)
//
//        public UnitsDataSink(String[] numberElements) {
//            this.numberElements = numberElements;
//        }
//
////        @Override
////        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
////            UResource.Table symbolsTable = value.getTable();
////            for (int j = 0; symbolsTable.getKeyAndValue(j, key, value); ++j) {
////                for (int i = 0; i < SYMBOL_KEYS.length; i++) {
////                    if (key.contentEquals(SYMBOL_KEYS[i])) {
////                        if (numberElements[i] == null) {
////                            numberElements[i] = value.toString();
////                        }
////                        break;
////                    }
////                }
////            }
////        }
//    }
//
//
//}
//
