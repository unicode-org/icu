REM Batch file for building the ICU4J source files.
REM Please make sure that your addition does not generate any errors.
REM Check the class dependencies.

REM make new class directory
mkdir classes
REM math packages


REM text and util packages, they are tightly dependent on each other
javac -d classes -classpath classes src/com/ibm/icu/math/*.java  src/com/ibm/icu/util/*.java src/com/ibm/icu/text/*.java src/com/ibm/icu/impl/*.java  src/com/ibm/icu/lang/*.java src/com/ibm/icu/impl/data/*.java src/com/ibm/icu/dev/tool/translit/*.java

REM textlayout packages 
javac -d classes -classpath classes src/com/ibm/richtext/textlayout/*.java src/com/ibm/richtext/textlayout/attributes/*.java 

REM tool packages
javac -d classes -classpath classes src/com/ibm/icu/dev/tool/compression/*.java src/com/ibm/icu/dev/tool/normalizer/CPPWriter.java src/com/ibm/icu/dev/tool/normalizer/JavaWriter.java src/com/ibm/icu/dev/tool/normalizer/SourceWriter.java src/com/ibm/icu/dev/tool/normalizer/MutableChar.java src/com/ibm/icu/dev/tool/normalizer/NormalizerBuilder.java src/com/ibm/icu/dev/tool/rbbi/*.java src/com/ibm/icu/dev/tool/translit/*.java 


REM test packages 
javac -d classes -classpath classes src/com/ibm/icu/dev/test/TestFmwk.java src/com/ibm/icu/dev/test/TestLog.java src/com/ibm/icu/dev/test/bigdec/*.java src/com/ibm/icu/dev/test/bnf/*.java src/com/ibm/icu/dev/test/calendar/*.java src/com/ibm/icu/dev/test/compression/*.java 
javac -d classes -classpath classes src/com/ibm/icu/dev/test/normalizer/*.java src/com/ibm/icu/dev/test/rbbi/*.java src/com/ibm/icu/dev/test/rbnf/*.java src/com/ibm/icu/dev/test/search/*.java src/com/ibm/icu/dev/test/timezone/*.java src/com/ibm/icu/dev/test/translit/*.java
javac -d classes -classpath classes src/com/ibm/icu/dev/test/format/*.java src/com/ibm/icu/dev/test/text/*.java src/com/ibm/icu/dev/test/util/*.java src/com/ibm/icu/dev/test/ucharacter/*.java 
javac -d classes -classpath classes src/com/ibm/icu/dev/test/TestAll.java

REM Copying dat files to classes directory
copy src\com\ibm\icu\impl\data\*.dat classes\com\ibm\icu\impl\data

REM richtext packages 
javac -d classes -classpath classes src/com/ibm/richtext/awtui/*.java src/com/ibm/richtext/print/*.java src/com/ibm/richtext/styledtext/*.java src/com/ibm/richtext/swingui/*.java src/com/ibm/richtext/textformat/*.java src/com/ibm/richtext/textpanel/*.java src/com/ibm/richtext/uiimpl/*.java src/com/ibm/richtext/uiimpl/resources/*.java 
javac -d classes -classpath classes src/com/ibm/richtext/textapps/*.java src/com/ibm/richtext/textapps/resources/*.java src/com/ibm/richtext/demo/*.java src/com/ibm/richtext/swingdemo/*.java
javac -d classes -classpath classes src/com/ibm/richtext/demo/*.java src/com/ibm/icu/dev/demo/calendar/*.java src/com/ibm/icu/dev/demo/holiday/*.java src/com/ibm/icu/dev/demo/rbbi/*.java src/com/ibm/icu/dev/demo/rbnf/*.java src/com/ibm/icu/dev/demo/translit/*.java src/com/ibm/icu/dev/demo/impl/*.java 
javac -d classes -classpath classes src/com/ibm/richtext/test/*.java  src/com/ibm/richtext/test/unit/*.java

mkdir doc
javadoc -d doc -sourcepath src com.ibm.icu.util com.ibm.icu.impl.data com.ibm.icu.tool.translit com.ibm.icu.tool.compression com.ibm.icu.tool.normalizer com.ibm.icu.tool.rbbi com.ibm.icu.math com.ibm.richtext.textlayout com.ibm.richtext.textlayout.attributes com.ibm.richtext.awtui com.ibm.richtext.print com.ibm.richtext.styledtext com.ibm.richtext.swingui com.ibm.richtext.textapps com.ibm.richtext.textformat com.ibm.richtext.textpanel com.ibm.richtext.uiimpl com.ibm.richtext.uiimpl.resources com.ibm.richtext.textapps.resources
