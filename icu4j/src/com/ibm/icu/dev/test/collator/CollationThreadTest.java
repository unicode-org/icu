/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.collator;

import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CollationThreadTest extends TestFmwk {
  public static void main(String[] args) throws Exception{
    new CollationThreadTest().run(args);
  }

private final String[] threadTestData = {
  "Banc Seókdyaaouq Pfuymjec",
  "BSH óy",
  "ABB - óg",
  "Gókpo Adhdoetpwtx Twxma, qm. Ilnudx",
  "Góbjh Zcgopqmjidw Dyhlu, ky. Npyamr",
  "Gódxb Slfduvgdwfi Qhreu, ao. Adyfqx",
  "Góten Emrmbmttgne Rtpir, rx. Mgmpjy",
  "Gókjo Hciqkymfcds Jpudo, ti. Ueceedbm (tkvyj vplrnpoq)",
  "Przjrpnbhrflnoo Dbiccp Lnmikfhsuoós Tgfhlpqoso / ZAD ENR",
  "Bang Nbygmoyc Ndónipcryjtzm",
  "Citjkëd Qgmgvr Er. w u.x.",
  "Dyrscywp Kvoifmyxo Ivvór Lbyxtrwnzp",
  "Géawk Ssqenl Pk. c r.g.",
  "Nesdoé Ilwbay Z.U.",
  "Poczsb Lrdtqg",
  "Pocafu Tgbmpn - wwg zo Mpespnzdllqk",
  "Polyvmg Z.C.",
  "POLUHONANQ FO",
  "Polrpycn",
  "Poleeaw-Rqzghgnnj R.W.",
  "Polyto Sgrgcvncz",
  "Polixj Tyfcóvcga Gbkjxfóf Tuogcybbbkyd C.U.",
  "Poltmzzlrkwt",
  "Polefgb Oiqefrkq",
  "Polrfdk Kónvyrfot Xuzbzzn f Ujmfwkdbnzh E.U. Wxkfiwss",
  "Polxtcf Hfowus Zzobblfm N.I.",
  "POLJNXO ZVYU L.A.",
  "PP Lowyr Rmknyoew",
  "Pralpe",
  "Preyojy Qnrxr",
  "PRK -5",
  "PRONENC U.P.",
  "Prowwyq & Relnda Hxkvauksnn Znyord Tz. w t.o.",
  "Propydv Afobbmhpg",
  "Proimpoupvp",
  "Probfo Hfttyr",
  "Propgi Lutgumnj X.W. BL",
  "Prozkch K.E.",
  "Progiyvzr Erejqk T.W.",
  "Prooxwq-Ydglovgk J.J.",
  "PTU Ntcw Lwkxjk S.M. UYF",
  "PWN",
  "PWP",
  "PZU I.D. Tlpzmhax",
  "PZU ioii A.T. Yqkknryu - bipdq badtg 500/9",
  "Qumnl-Udffq",
  "Radmvv",
  "Railoggeqd Aewy Fwlmsp K.S. Ybrqjgyr",
  "Remhmxkx Ewuhxbg",
  "Renafwp Sapnqr io v z.n.",
  "Repqbpuuo",
  "Resflig",
  "Rocqz Mvwftutxozs VQ",
  "Rohkui",
  "RRC",
  "Samgtzg Fkbulcjaaqv Ollllq Ad. l l.v.",
  "Schelrlw Fu. t z.x.",
  "Schemxgoc Axvufoeuh",
  "Siezsxz Eb. n r.h",
  "Sikj Wyvuog",
  "Sobcwssf Oy. q o.s. Kwaxj",
  "Sobpxpoc Fb. w q.h. Elftx",
  "Soblqeqs Kpvppc RH - tbknhjubw siyaenc Njsjbpx Buyshpgyv",
  "Sofeaypq FJ",
  "Stacyok Qurqjw Hw. f c.h.",
  "STOWN HH",
  "Stopjhmq Prxhkakjmalkvdt Weqxejbyig Wgfplnvk D.C.",
  "STRHAEI Clydqr Ha. d z.j.",
  "Sun Clvaqupknlk",
  "TarfAml",
  "Tchukm Rhwcpcvj Cc. v y.a.",
  "Teco Nyxm Rsvzkx pm. J a.t.",
  "Tecdccaty",
  "Telruaet Nmyzaz Twwwuf",
  "Tellrwihv Xvtjle N.U.",
  "Telesjedc Boewsx A.F",
  "tellqfwiqkv dinjlrnyit yktdhlqquihzxr (ohvso)",
  "Tetft Kna Ab. j l.z.",
  "Thesch",
  "Totqucvhcpm Gejxkgrz Is. e k.i.",
  "Towajgixetj Ngaayjitwm fj csxm Mxebfj Sbocok X.H.",
  "Toyfon Meesp Neeban Jdsjmrn sz v z.w.",
  "TRAJQ NZHTA Li. n x.e. - Vghfmngh",
  "Triuiu",
  "Tripsq",
  "TU ENZISOP ZFYIPF V.U.",
  "TUiX Kscdw G.G.",
  "TVN G.A.",
  "Tycd",
  "Unibjqxv rdnbsn - ZJQNJ XCG / Wslqfrk",
  "Unilcs - hopef ps 20 nixi",
  "UPC Gwwmru Ds. g o.r.",
  "Vaidgoav",
  "Vatyqzcgqh Kjnnsy GQ WT",
  "Volhz",
  "Vos Jviggogjt Iyqhlm Ih. w j.y. (fbshoihdnb)",
  "WARMFC E.D.",
  "Wincqk Pqadskf",
  "WKRD",
  "Wolk Pyug",
  "WPRV",
  "WSiI",
  "Wurag XZ",
  "Zacrijl B.B.",
  "Zakja Tziaboysenum Squlslpp - Diifw V.D.",
  "Zakgat Meqivadj Nrpxlekmodx s Bbymjozge W.Y.",
  "Zjetxpbkpgj Mmhhgohasjtpkjd Uwucubbpdj K.N.",
  "ZREH"
};

  public void testThreads() {
    final Collator theCollator = Collator.getInstance(new Locale("pl","",""));
    final String[] theData = threadTestData;
    final Random r = new Random();

    class Control {
      private boolean go;
      private String fail;

      synchronized void start() {
        go = true;
        notifyAll();
      }

      synchronized void stop() {
        go = false;
        notifyAll();
      }

      boolean go() {
        return go;
      }

      void fail(String msg) {
        fail = msg;
        stop();
      }
    }

    final Control control = new Control();

    class Test implements Runnable {
      private String[] data;
      private Collator collator;
      private String name;

      Test(String name) {
        this.name = name;

        try {
          data = (String[])theData.clone();
          collator = (Collator)theCollator.clone();
        } catch (CloneNotSupportedException e) {
          // should not happen, if it does we'll get an exception right away
          errln("could not clone");
          data = null;
          collator = null;
        }
      }

      public void run() {
        try {
          synchronized (control) {
            while (!control.go()) {
              control.wait();
            }
          }

          while (control.go()) {
            scramble();
            sort();
          }
        }
        catch (InterruptedException e) {
          // die
        }
        catch (IndexOutOfBoundsException e) {
          control.fail(name + " " + e.getMessage());
        }
      }

      private void scramble() {
        for (int i = 0; i < data.length; ++i) {
          int ix = r.nextInt(data.length);
          String s = data[i];
          data[i] = data[ix];
          data[ix] = s;
        }
      }

      private void sort() {
        Arrays.sort(data, collator);
      }
    }

    Thread[] threads = new Thread[10];
    for (int i = 0; i < threads.length; ++i) {
      threads[i] = new Thread(new Test("test " + i));
      threads[i].start();
    }
     
    try {
      control.start();

      long stopTime = System.currentTimeMillis() + 5000;
      do {
        Thread.sleep(100);
      } while (control.go() && System.currentTimeMillis() < stopTime);

      control.stop();

      for (int i = 0; i < threads.length; ++i) {
        threads[i].join();
      }
    } catch (InterruptedException e) {
      // die
    }

    if (control.fail != null) {
      errln(control.fail);
    }
  }
}  
  
