/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
package com.ibm.icu.dev.test.perf;

import com.ibm.icu.text.*;

public class NormalizerPerformanceTest extends PerfTest {
    
    String[] NFDFileLines;
    String[] NFCFileLines;
    String[] fileLines;
    
    public static void main(String[] args) throws Exception {
        new NormalizerPerformanceTest().run(args);
    }
    
    protected void setup(String[] args) {
        if(bulk_mode == line_mode){
            printUsage();
        }
        if (fileName.equalsIgnoreCase("")){
            printUsage();
        }
        fileLines = readLines(fileName, encoding, bulk_mode);
        NFDFileLines = normalizeInput(fileLines, Normalizer.NFD);
        NFCFileLines = normalizeInput(fileLines, Normalizer.NFC);
    }
    
    // Test NFC Performance
    PerfTest.Function TestICU_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    Normalizer.normalize(NFDFileLines[i], Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestICU_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    Normalizer.normalize(NFCFileLines[i], Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestICU_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.normalize(fileLines[i], Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    // Test NFD Performance
    PerfTest.Function TestICU_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    Normalizer.normalize(NFDFileLines[i], Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestICU_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    Normalizer.normalize(NFCFileLines[i], Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestICU_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.normalize(fileLines[i], Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
/** I really wish there was conditional compilation in Java      
        // Test NFC Performance
    PerfTest.Function TestJDK_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    String nfc = sun.text.Normalizer.normalize(NFDFileLines[i], sun.text.Normalizer.COMPOSE,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
  
    PerfTest.Function TestJDK_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    String nfc = sun.text.Normalizer.normalize(NFCFileLines[i], sun.text.Normalizer.COMPOSE,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestJDK_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    String nfc = sun.text.Normalizer.normalize(fileLines[i], sun.text.Normalizer.COMPOSE,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    // Test NFD Performance
    PerfTest.Function TestJDK_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    String nfc = sun.text.Normalizer.normalize(NFDFileLines[i], sun.text.Normalizer.DECOMP,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestJDK_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    String nfc = sun.text.Normalizer.normalize(NFCFileLines[i], sun.text.Normalizer.DECOMP,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestJDK_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    String nfc = sun.text.Normalizer.normalize(fileLines[i], sun.text.Normalizer.DECOMP,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    // Test FCD Performance
    PerfTest.Function TestICU_FCD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    String nfc = Normalizer.normalize(NFDFileLines[i], Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
**/    
    PerfTest.Function TestICU_FCD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    Normalizer.normalize(NFCFileLines[i], Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestICU_FCD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.normalize(fileLines[i], Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    // Test Quick Check Performance
    PerfTest.Function TestQC_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    Normalizer.quickCheck(NFDFileLines[i], Normalizer.NFC,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    Normalizer.quickCheck(NFCFileLines[i], Normalizer.NFC,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.quickCheck(fileLines[i], Normalizer.NFC,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    Normalizer.quickCheck(NFDFileLines[i], Normalizer.NFD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                     Normalizer.quickCheck(NFCFileLines[i], Normalizer.NFD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                     Normalizer.quickCheck(fileLines[i], Normalizer.NFD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_FCD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                     Normalizer.quickCheck(NFDFileLines[i], Normalizer.FCD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_FCD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                     Normalizer.quickCheck(NFCFileLines[i], Normalizer.FCD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_FCD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                     Normalizer.quickCheck(fileLines[i], Normalizer.FCD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    // Test isNormalized Performance
    PerfTest.Function TestIsNormalized_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                     Normalizer.isNormalized(NFDFileLines[i], Normalizer.NFC, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    Normalizer.isNormalized(NFCFileLines[i], Normalizer.NFC, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.isNormalized(fileLines[i], Normalizer.NFC, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                     Normalizer.isNormalized(NFDFileLines[i], Normalizer.NFD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                     Normalizer.isNormalized(NFCFileLines[i], Normalizer.NFD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.isNormalized(fileLines[i], Normalizer.NFD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_FCD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                     Normalizer.isNormalized(NFDFileLines[i], Normalizer.FCD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_FCD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                     Normalizer.isNormalized(NFCFileLines[i], Normalizer.FCD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_FCD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                     Normalizer.isNormalized(fileLines[i], Normalizer.FCD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
      
    private void printUsage() {
        System.out.println("Usage: " + this.getClass().getName() + " [OPTIONS] fileName\n"
                            + "\t-f or --fileName  \tfile to be used as test data\n"
                            + "\t-s or --sourceDir \tsource directory for files followed by path\n"
                            + "\t-e or --encoding  \tencoding of source files\n"
                            + "\t-b or --bulkMode  \tnormalize whole file at once\n"
                            + "\t-l or --lineMode  \tnormalize file one line at a time\n"
            );
        System.exit(1);
    }
    String[] normalizeInput(String[] src, Normalizer.Mode mode) {
        String[] dest = new String[src.length];
        for (int i = 0; i < src.length; i++) {
            dest[i] = Normalizer.normalize(src[i], mode);
        }
        
        return dest;
    }
}
