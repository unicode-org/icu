package com.ibm.text.resources;

import java.util.ListResourceBundle;

public class TransliterationRule$Latin$Cyrillic extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "Description",
                "xxxxxxxxxxxx" },

            { "Rule",
              // Russian Letters

              "cyA=\u0410;" +
              "cyBe=\u0411;" +
              "cyVe=\u0412;" +
              "cyGe=\u0413;" +
              "cyDe=\u0414;" +
              "cyYe=\u0415;" +
              "cyYo=\u0416;" +
              "cyZhe=\u0417;" +
              "cyZe=\u0418;" +
              "cyYi=\u0419;" +
              "cyY=\u0419;" +
              "cyKe=\u041a;" +
              "cyLe=\u041b;" +
              "cyMe=\u041c;" +
              "cyNe=\u041d;" +
              "cyO=\u041e;" +
              "cyPe=\u041f;" +

              "cyRe=\u0420;" +
              "cySe=\u0421;" +
              "cyTe=\u0422;" +
              "cyU=\u0423;" +
              "cyFe=\u0424;" +
              "cyKhe=\u0425;" +
              "cyTse=\u0426;" +
              "cyChe=\u0427;" +
              "cyShe=\u0428;" +
              "cyShche=\u0429;" +
              "cyHard=\u042a;" +
              "cyI=\u042b;" +
              "cySoft=\u042c;" +
              "cyE=\u042d;" +
              "cyYu=\u042e;" +
              "cyYa=\u042f;" +

              "cya=\u0430;" +
              "cybe=\u0431;" +
              "cyve=\u0432;" +
              "cyge=\u0433;" +
              "cyde=\u0434;" +
              "cyye=\u0435;" +
              "cyzhe=\u0436;" +
              "cyze=\u0437;" +
              "cyyi=\u0438;" +
              "cyy=\u0439;" +
              "cyke=\u043a;" +
              "cyle=\u043b;" +
              "cyme=\u043c;" +
              "cyne=\u043d;" +
              "cyo=\u043e;" +
              "cype=\u043f;" +

              "cyre=\u0440;" +
              "cyse=\u0441;" +
              "cyte=\u0442;" +
              "cyu=\u0443;" +
              "cyfe=\u0444;" +
              "cykhe=\u0445;" +
              "cytse=\u0446;" +
              "cyche=\u0447;" +
              "cyshe=\u0448;" +
              "cyshche=\u0449;" +
              "cyhard=\u044a;" +
              "cyi=\u044b;" +
              "cysoft=\u044c;" +
              "cye=\u044d;" +
              "cyyu=\u044e;" +
              "cyya=\u044f;" +

              "cyyo=\u0451;" +

              "a=[aA];" +
              "c=[cC];" +
              "e=[eE];" +
              "h=[hH];" +
              "i=[iI];" +
              "o=[oO];" +
              "s=[sS];" +
              "t=[tT];" +
              "u=[uU];" +
              "iey=[ieyIEY];" +
              "lower=[:Lu:];" +

              // convert English to Russian
              "Russian>\u041f\u0420\u0410\u0412\u0414\u0410\u00D1\u0020\u0411\u044d\u043b\u0430\u0440\u0443\u0441\u043a\u0430\u044f\u002c\u0020\u043a\u044b\u0440\u0433\u044b\u0437\u002c\u0020\u041c\u043e\u043b\u0434\u043e\u0432\u044d\u043d\u044f\u0441\u043a\u044d\u002e;" +

              //special equivs for ay, oy, ...
              "Y{a}{i}>{cyYa}{cyY};" +
              "Y{e}{i}>{cyYe}{cyY};" +
              "Y{i}{i}>{cyYi}{cyY};" +
              "Y{o}{i}>{cyYo}{cyY};" +
              "Y{u}{i}>{cyYu}{cyY};" +
              "A{i}>{cyA}{cyY};" +
              "E{i}>{cyE}{cyY};" +
              //skip II, since it is the soft sign
              "O{i}>{cyO}{cyY};" +
              "U{i}>{cyU}{cyY};" +

              "A>{cyA};" +
              "B>{cyBe};" +
              "C{h}>{cyChe};" +
              "C[{iey}>{cySe};" +
              "C>{cyKe};" +
              "D>{cyDe};" +
              "E>{cyE};" +
              "F>{cyFe};" +
              "G>{cyGe};" +
              "H>{cyHard};" +
              "I{i}>{cySoft};" +
              "I>{cyI};" +
              "J>{cyDe}{cyZhe};" +
              "K{h}>{cyKhe};" +
              "K>{cyKe};" +
              "L>{cyLe};" +
              "M>{cyMe};" +
              "N>{cyNe};" +
              "O>{cyO};" +
              "P>{cyPe};" +
              "Q{u}>{cyKe}{cyVe};" +
              "R>{cyRe};" +
              "S{h}{t}{c}{h}>{cyShche};" +
              "S{h}{c}{h}>{cyShche};" +
              "S{h}>{cyShe};" +
              "S>{cySe};" +
              "T{c}{h}>{cyChe};" +
              "T{h}>{cyZe};" +
              "T{s}>{cyTse};" +
              "T>{cyTe};" +
              "U>{cyU};" +
              "V>{cyVe};" +
              "W{h}>{cyVe};" +
              "W>{cyVe};" +
              "X>{cyKe}{cySe};" +
              "Y{e}>{cyYe};" +
              "Y{o}>{cyYo};" +
              "Y{u}>{cyYu};" +
              "Y{a}>{cyYa};" +
              "Y{i}>{cyYi};" +
              "Y>{cyY};" +
              "Z{h}>{cyZhe};" +
              "Z>{cyZe};" +
              "X>{cyKe}{cySe};" +

              //lower case: doesn''t solve join bug
              "y{a}{i}>{cyya}{cyy};" +
              "y{e}{i}>{cyye}{cyy};" +
              "y{i}{i}>{cyyi}{cyy};" +
              "y{o}{i}>{cyyo}{cyy};" +
              "y{u}{i}>{cyyu}{cyy};" +
              "a{i}>{cya}{cyy};" +
              "e{i}>{cye}{cyy};" +
              //skip ii, since it is the soft sign
              "o{i}>{cyo}{cyy};" +
              "u{i}>{cyu}{cyy};" +

              "a>{cya};" +
              "b>{cybe};" +
              "c{h}>{cyche};" +
              "c[{iey}>{cyse};" +
              "c>{cyke};" +
              "d>{cyde};" +
              "e>{cye};" +
              "f>{cyfe};" +
              "g>{cyge};" +
              "h>{cyhard};" +
              "i{i}>{cysoft};" +
              "i>{cyi};" +
              "j>{cyde}{cyzhe};" +
              "k{h}>{cykhe};" +
              "k>{cyke};" +
              "l>{cyle};" +
              "m>{cyme};" +
              "n>{cyne};" +
              "o>{cyo};" +
              "p>{cype};" +
              "q{u}>{cyke}{cyve};" +
              "r>{cyre};" +
              "s{h}{t}{c}{h}>{cyshche};" +
              "s{h}{c}{h}>{cyshche};" +
              "s{h}>{cyshe};" +
              "s>{cyse};" +
              "t{c}{h}>{cyche};" +
              "t{h}>{cyze};" +
              "t{s}>{cytse};" +
              "t>{cyte};" +
              "u>{cyu};" +
              "v>{cyve};" +
              "w{h}>{cyve};" +
              "w>{cyve};" +
              "x>{cyke}{cyse};" +
              "y{e}>{cyye};" +
              "y{o}>{cyyo};" +
              "y{u}>{cyyu};" +
              "y{a}>{cyya};" +
              "y{i}>{cyyi};" +
              "y>{cyy};" +
              "z{h}>{cyzhe};" +
              "z>{cyze};" +
              "x>{cyke}{cyse};" +

              //generally the last rule
              "''>;" +

              //now Russian to English

              "Y''<{cyY}[{cyA};" +
              "Y''<{cyY}[{cyE};" +
              "Y''<{cyY}[{cyI};" +
              "Y''<{cyY}[{cyO};" +
              "Y''<{cyY}[{cyU};" +
              "Y''<{cyY}[{cya};" +
              "Y''<{cyY}[{cye};" +
              "Y''<{cyY}[{cyi};" +
              "Y''<{cyY}[{cyo};" +
              "Y''<{cyY}[{cyu};" +
              "A<{cyA};" +
              "B<{cyBe};" +
              "J<{cyDe}{cyZhe};" +
              "J<{cyDe}{cyzhe};" +
              "D<{cyDe};" +
              "V<{cyVe};" +
              "G<{cyGe};" +
              "Zh<{cyZhe}[{lower};" +
              "ZH<{cyZhe};" +
              "Z''<{cyZe}[{cyHard};" +
              "Z''<{cyZe}[{cyhard};" +
              "Z<{cyZe};" +
              "Ye<{cyYe}[{lower};" +
              "YE<{cyYe};" +
              "Yo<{cyYo}[{lower};" +
              "YO<{cyYo};" +
              "Yu<{cyYu}[{lower};" +
              "YU<{cyYu};" +
              "Ya<{cyYa}[{lower};" +
              "YA<{cyYa};" +
              "Yi<{cyYi}[{lower};" +
              "YI<{cyYi};" +
              "Y<{cyY};" +
              "Kh<{cyKhe}[{lower};" +
              "KH<{cyKhe};" +
              "K''<{cyKe}[{cyHard};" +
              "K''<{cyKe}[{cyhard};" +
              "X<{cyKe}{cySe};" +
              "X<{cyKe}{cyse};" +
              "K<{cyKe};" +
              "L<{cyLe};" +
              "M<{cyMe};" +
              "N<{cyNe};" +
              "O<{cyO};" +
              "P<{cyPe};" +

              "R<{cyRe};" +
              "Shch<{cyShche}[{lower};" +
              "SHCH<{cyShche};" +
              "Sh''<{cyShe}[{cyche};" +
              "SH''<{cyShe}[{cyChe};" +
              "Sh<{cyShe}[{lower};" +
              "SH<{cyShe};" +
              "S''<{cySe}[{cyHard};" +
              "S''<{cySe}[{cyhard};" +
              "S<{cySe};" +
              "Ts<{cyTse}[{lower};" +
              "TS<{cyTse};" +
              "T''<{cyTe}[{cySe};" +
              "T''<{cyTe}[{cyse};" +
              "T''<{cyTe}[{cyHard};" +
              "T''<{cyTe}[{cyhard};" +
              "T<{cyTe};" +
              "U<{cyU};" +
              "F<{cyFe};" +
              "Ch<{cyChe}[{lower};" +
              "CH<{cyChe};" +
              "H<{cyHard};" +
              "I''<{cyI}[{cyI};" +
              "I''<{cyI}[{cyi};" +
              "I<{cyI};" +
              "Ii<{cySoft}[{lower};" +
              "II<{cySoft};" +
              "E<{cyE};" +

              //lowercase
              "y''<{cyy}[{cya};" +
              "y''<{cyy}[{cye};" +
              "y''<{cyy}[{cyi};" +
              "y''<{cyy}[{cyo};" +
              "y''<{cyy}[{cyu};" +
              "y''<{cyy}[{cyA};" +
              "y''<{cyy}[{cyE};" +
              "y''<{cyy}[{cyI};" +
              "y''<{cyy}[{cyO};" +
              "y''<{cyy}[{cyU};" +
              "a<{cya};" +
              "b<{cybe};" +
              "j<{cyde}{cyzhe};" +
              "j<{cyde}{cyZhe};" +
              "d<{cyde};" +
              "v<{cyve};" +
              "g<{cyge};" +
              "zh<{cyzhe};" +
              "z''<{cyze}[{cyhard};" +
              "z''<{cyze}[{cyHard};" +
              "z<{cyze};" +
              "ye<{cyye};" +
              "yo<{cyyo};" +
              "yu<{cyyu};" +
              "ya<{cyya};" +
              "yi<{cyyi};" +
              "y<{cyy};" +
              "kh<{cykhe};" +
              "k''<{cyke}[{cyhard};" +
              "k''<{cyke}[{cyHard};" +
              "x<{cyke}{cyse};" +
              "x<{cyke}{cySe};" +
              "k<{cyke};" +
              "l<{cyle};" +
              "m<{cyme};" +
              "n<{cyne};" +
              "o<{cyo};" +
              "p<{cype};" +

              "r<{cyre};" +
              "shch<{cyshche};" +
              "sh''<{cyshe}[{cyche};" +
              "sh''<{cyshe}[{cyChe};" +
              "sh<{cyshe};" +
              "s''<{cyse}[{cyhard};" +
              "s''<{cyse}[{cyHard};" +
              "s<{cyse};" +
              "ts<{cytse};" +
              "t''<{cyte}[{cyse};" +
              "t''<{cyte}[{cySe};" +
              "t''<{cyte}[{cyhard};" +
              "t''<{cyte}[{cyHard};" +
              "t<{cyte};" +
              "u<{cyu};" +
              "f<{cyfe};" +
              "ch<{cyche};" +
              "h<{cyhard};" +
              "i''<{cyi}[{cyI};" +
              "i''<{cyi}[{cyi};" +
              "i<{cyi};" +
              "ii<{cysoft};" +
              "e<{cye};" +

              //generally the last rule
              "''>;"
              //the end
            }
        };
    }
}
