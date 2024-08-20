window.BENCHMARK_DATA = {
  "lastUpdate": 1724113814858,
  "repoUrl": "https://github.com/unicode-org/icu",
  "entries": {
    "Benchmark": [
      {
        "commit": {
          "author": {
            "email": "nrunge@google.com",
            "name": "gnrunge",
            "username": "gnrunge"
          },
          "committer": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "distinct": true,
          "id": "58060eafdc69ac7a060e8b80d942246adc471f0f",
          "message": "ICU-22036 Adds ICU4J date formatting performance test, which is last of the\nICU4J performance tests.",
          "timestamp": "2022-06-29T12:02:29-07:00",
          "tree_id": "c282c6da7f45a320f8d00b465f596b5b55770b50",
          "url": "https://github.com/unicode-org/icu/commit/58060eafdc69ac7a060e8b80d942246adc471f0f"
        },
        "date": 1656529557447,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.38647600237437,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "robertbastian@users.noreply.github.com",
            "name": "Robert Bastian",
            "username": "robertbastian"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "783b4f7b6a7f0996563c802bcec296e2a81d882f",
          "message": "ICU-22061 rename",
          "timestamp": "2022-06-29T13:16:26-07:00",
          "tree_id": "acffa8df9f2a19ac3aa86209887641a5cf2fcb49",
          "url": "https://github.com/unicode-org/icu/commit/783b4f7b6a7f0996563c802bcec296e2a81d882f"
        },
        "date": 1656534000980,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 236.8694272774609,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nrunge@google.com",
            "name": "gnrunge",
            "username": "gnrunge"
          },
          "committer": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "distinct": true,
          "id": "58060eafdc69ac7a060e8b80d942246adc471f0f",
          "message": "ICU-22036 Adds ICU4J date formatting performance test, which is last of the\nICU4J performance tests.",
          "timestamp": "2022-06-29T12:02:29-07:00",
          "tree_id": "c282c6da7f45a320f8d00b465f596b5b55770b50",
          "url": "https://github.com/unicode-org/icu/commit/58060eafdc69ac7a060e8b80d942246adc471f0f"
        },
        "date": 1656541141140,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.65980636048877,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "cyndy_ishida@apple.com",
            "name": "Cyndy Ishida",
            "username": "cyndyishida"
          },
          "committer": {
            "email": "cyndy_ishida@apple.com",
            "name": "Cyndy Ishida",
            "username": "cyndyishida"
          },
          "distinct": true,
          "id": "e2ae4f5324e863a92bf79892ef1a2e0d6b183af7",
          "message": "ICU-22054 Remove declarations for unimplemented APIs\n\nThis patch also includes marking `=delete` on specific `normal` member functions, as opposed to compiler generated functions,\nbased on the description of such functions' surrounding comments.",
          "timestamp": "2022-07-01T08:57:10-07:00",
          "tree_id": "fda6935be0dbf84c2fec7832622127bc05ca0b48",
          "url": "https://github.com/unicode-org/icu/commit/e2ae4f5324e863a92bf79892ef1a2e0d6b183af7"
        },
        "date": 1656691235364,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 221.1116978266143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "daschuer@mixxx.org",
            "name": "Daniel Schürmann",
            "username": "daschuer"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "rp9-next",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "9f358ccb2472866a849aa4f393ef4528448c3e9d",
          "message": "ICU-22010 Add early check for AX_CHECK_COMPILE_FLAG\n\nThis helps to avoid missleading error message:\n\n```\n./source/configure: line 7981: syntax error near unexpected token 'newline'\n./source/configure: line 7981: 'AX_CHECK_COMPILE_FLAG('\n```",
          "timestamp": "2022-07-12T13:30:01+05:30",
          "tree_id": "cb81138f619f83e5a8667ed95e034c4d81ae9b8b",
          "url": "https://github.com/unicode-org/icu/commit/9f358ccb2472866a849aa4f393ef4528448c3e9d"
        },
        "date": 1657613063114,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.41724421268182,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "721d41153ec4f80cd8556ac57dc023d169ef9dd2",
          "message": "ICU-22071 Fixed DateTimePatternGenerator to respect the locale's \"rg\" subtag (when it has one) when determining the hour cycle.",
          "timestamp": "2022-07-12T10:55:03-07:00",
          "tree_id": "c8298bfaaccafa158a689ec1bfd5c56a0cc8f44d",
          "url": "https://github.com/unicode-org/icu/commit/721d41153ec4f80cd8556ac57dc023d169ef9dd2"
        },
        "date": 1657648750141,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 237.258234446372,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "4f6f087f2eaa4011237ee0c4e4cf2b1e47ea70fc",
          "message": "ICU-22085 Fix old typo on calculating path size in loadTestData() and add a few small changes to support running ICU unit tests in Xcode.",
          "timestamp": "2022-07-14T08:53:16-07:00",
          "tree_id": "35a3d64dc578f2aac68d1255b7f91072d95f1e29",
          "url": "https://github.com/unicode-org/icu/commit/4f6f087f2eaa4011237ee0c4e4cf2b1e47ea70fc"
        },
        "date": 1657814200637,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.3921035394423,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "william.marlow@ibm.com",
            "name": "William Marlow",
            "username": "lux01"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "9a7b0e08d0bb8ed11775c76f935eabc155fdc795",
          "message": "ICU-22086 Add ibm-clang_r/ibm-clang++_r to runConfigureICU",
          "timestamp": "2022-07-14T17:38:59Z",
          "tree_id": "0fedd85fc633e091452763491c5688581ca32177",
          "url": "https://github.com/unicode-org/icu/commit/9a7b0e08d0bb8ed11775c76f935eabc155fdc795"
        },
        "date": 1657820549633,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 227.14913881595672,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "6394a48d067134a99faca4c7329accee0c580301",
          "message": "ICU-21957 integrate CLDR release-42-m2 (mid milestone) to ICU main for 72",
          "timestamp": "2022-07-14T10:56:39-07:00",
          "tree_id": "313c19455be6773aa335e6e10e0b9518d142b996",
          "url": "https://github.com/unicode-org/icu/commit/6394a48d067134a99faca4c7329accee0c580301"
        },
        "date": 1657821605680,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 260.0725303789964,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "a8594a401f3241b19f9b8b61bf786fde917b4c72",
          "message": "ICU-22056 Add a new unum_hasAttribute() method.",
          "timestamp": "2022-07-15T16:03:56-07:00",
          "tree_id": "1db5acef8d7251a4829b61c61c686f5822486667",
          "url": "https://github.com/unicode-org/icu/commit/a8594a401f3241b19f9b8b61bf786fde917b4c72"
        },
        "date": 1657926851027,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 227.68761928303834,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "hsivonen@hsivonen.fi",
            "name": "Henri Sivonen",
            "username": "hsivonen"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "9c72bf975890a1edacce046a2e55e88023a86389",
          "message": "ICU-22087 Export a non-recursive canonical decomposition supplement for ICU4X",
          "timestamp": "2022-07-18T10:05:10-07:00",
          "tree_id": "cf9c0091bce7abf7054b71884fd644acc42cae16",
          "url": "https://github.com/unicode-org/icu/commit/9c72bf975890a1edacce046a2e55e88023a86389"
        },
        "date": 1658164250801,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.60705221149715,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "9d230f923c51bb72af9420e8cf53a019894a8e52",
          "message": "ICU-21939 Fix bogus \"conflicting fields\" error in DateIntervalFormat.",
          "timestamp": "2022-07-18T15:16:40-07:00",
          "tree_id": "c0319f3181cebd91a769eaebd21495a02b16a9cb",
          "url": "https://github.com/unicode-org/icu/commit/9d230f923c51bb72af9420e8cf53a019894a8e52"
        },
        "date": 1658182843386,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.850059036283,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nrunge@google.com",
            "name": "gnrunge",
            "username": "gnrunge"
          },
          "committer": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "distinct": true,
          "id": "86166e0a2dd1119e086d04b8973dc486b4af8dcc",
          "message": "ICU-22075 Adds a random waiting period (0 - 179 secs) to each test that runs as\npart of a high concurrency performance test setup. This will disperse commits\nof performance test results into the data branch over a wider time frame and\ndrastically reduces the chance of data uploads failing after ten unsuccesful\ncommit attempts.\n\nTest runs showed a huge drop in failed commits/retry, from a high of 113\nwithout wait down to only 4 with the extra wait.\n\nICU-22075 Add comment explaining the rationale of the random\nsleep period prior to test execution.",
          "timestamp": "2022-07-22T08:14:58-07:00",
          "tree_id": "e00a8df1e80c75ce21faf8a38134eaaa9b3b31d3",
          "url": "https://github.com/unicode-org/icu/commit/86166e0a2dd1119e086d04b8973dc486b4af8dcc"
        },
        "date": 1658503330606,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.5057980013098,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "hsivonen@hsivonen.fi",
            "name": "Henri Sivonen",
            "username": "hsivonen"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "c258f3d6f81a2514b76d72c5deae8cbc295aecd6",
          "message": "ICU-22095 Export ICU4X normalization data with tries only without Unicode sets",
          "timestamp": "2022-07-25T15:54:29-07:00",
          "tree_id": "1e4ac369923311e8ac0922960bbb1cc63650003f",
          "url": "https://github.com/unicode-org/icu/commit/c258f3d6f81a2514b76d72c5deae8cbc295aecd6"
        },
        "date": 1658790034903,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 257.90187911998254,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "58a51495dd1c94d29cda93ffc29a904d77e50d31",
          "message": "ICU-22088 Various fixes to make dealing with NUMBERING_SYSTEM formatters easier.",
          "timestamp": "2022-07-28T16:18:01-07:00",
          "tree_id": "69c47d1745ae982673f69de39b18f9c44cdc75b7",
          "url": "https://github.com/unicode-org/icu/commit/58a51495dd1c94d29cda93ffc29a904d77e50d31"
        },
        "date": 1659050807225,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.71534958102208,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "dcd19ae9bc0914f8be439a2de1a5fbc17e7b5447",
          "message": "ICU-21957 integrate CLDR release-42-alpha0 (first with Survey Tool data) to ICU main for 72 (#2142)",
          "timestamp": "2022-07-29T15:32:45-07:00",
          "tree_id": "6e9b3f20e3d80a16de21313da72a5cb5b6ac2361",
          "url": "https://github.com/unicode-org/icu/commit/dcd19ae9bc0914f8be439a2de1a5fbc17e7b5447"
        },
        "date": 1659135077011,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.73067578848975,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "rp9-next",
            "username": "rp9-next"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "rp9-next",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "4cf4116dadd2f34c20d9df42b1272cac7dcb30c4",
          "message": "ICU-22074 Increase the valgrind CI timeout",
          "timestamp": "2022-08-01T12:51:55+05:30",
          "tree_id": "5219374afb583705529f150783e07b0ac009b0c1",
          "url": "https://github.com/unicode-org/icu/commit/4cf4116dadd2f34c20d9df42b1272cac7dcb30c4"
        },
        "date": 1659339060719,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 222.24786439649117,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "0266970e977b9e2488dfbf788cc280be3a0338ca",
          "message": "ICU-21957 integrate CLDR release-42-alpha1 to ICU main for 72",
          "timestamp": "2022-08-05T09:39:58-07:00",
          "tree_id": "ae5c1572c4ed62ac177b5a843ecc6451b53749b2",
          "url": "https://github.com/unicode-org/icu/commit/0266970e977b9e2488dfbf788cc280be3a0338ca"
        },
        "date": 1659717912727,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 244.811581504815,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "d99abb6d65fd48db3bbf6aad523e7aaa793781ec",
          "message": "ICU-21957 integrate CLDR release-42-alpha1b to ICU main for 72",
          "timestamp": "2022-08-09T16:05:20-07:00",
          "tree_id": "114745d9b1092b441127d501c28853c12f8182a3",
          "url": "https://github.com/unicode-org/icu/commit/d99abb6d65fd48db3bbf6aad523e7aaa793781ec"
        },
        "date": 1660086637139,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.58085090195044,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "0eecb25011de051c57f68c75d767ad3586de7859",
          "message": "ICU-22101 Error prone reports \"StringSplitter\" error in PluralRules.java\n\nString.split(String) and Pattern.split(CharSequence) have surprising behaviour.\n\"a::b:\".split(\":\") produces [\"a\", \"b\"], when one would expect [\"a\", \"\", \"b\", \"\"]\n\nThe recommended fix is to use the Guava Splitter, or setting an explicit limit:\nString.split(String,int limit) and Pattern.split(CharSequence,int limit)",
          "timestamp": "2022-08-11T08:27:19-07:00",
          "tree_id": "724c7700b37e1fcbaf574d25414c4370ec250537",
          "url": "https://github.com/unicode-org/icu/commit/0eecb25011de051c57f68c75d767ad3586de7859"
        },
        "date": 1660231995008,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.1881026814447,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango",
            "username": "echeran"
          },
          "distinct": true,
          "id": "3ef03a408714cf0be1f6be62e3fad57757403da3",
          "message": "ICU-21947 Replace FixedDecimal with DecimalQuantity in PluralRule sample parsing\n\nSee #2007",
          "timestamp": "2022-08-11T15:10:37-07:00",
          "tree_id": "2216414cc87171c10282454fcf0a35d917a80fbd",
          "url": "https://github.com/unicode-org/icu/commit/3ef03a408714cf0be1f6be62e3fad57757403da3"
        },
        "date": 1660256482781,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 256.56034230302646,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "8492a8291670aa0af0f3fa6c088ddaf4ff373833",
          "message": "ICU-22105 Fixed the unit-conversion logic to work correctly with negative temperature values.",
          "timestamp": "2022-08-16T10:18:24-07:00",
          "tree_id": "ade7eb2c651708551e62d475d3987927dec81353",
          "url": "https://github.com/unicode-org/icu/commit/8492a8291670aa0af0f3fa6c088ddaf4ff373833"
        },
        "date": 1660670756986,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.36489393089283,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "hsivonen@hsivonen.fi",
            "name": "Henri Sivonen",
            "username": "hsivonen"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango",
            "username": "echeran"
          },
          "distinct": true,
          "id": "59615c93f694227eaacf242d22a549c316557cdf",
          "message": "ICU-22115 Merge passthrough and canonical combining class data into the NFD trie for ICU4X",
          "timestamp": "2022-08-16T15:53:56-07:00",
          "tree_id": "4f51bc73e1477d7f636740f5506076ee6756f1af",
          "url": "https://github.com/unicode-org/icu/commit/59615c93f694227eaacf242d22a549c316557cdf"
        },
        "date": 1660690830551,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 217.94076302229348,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "26733782601ec384b8070019a8121795f01000cd",
          "message": "ICU-22118 tzdata2022b updates in icu code\n\nSee #2157",
          "timestamp": "2022-08-18T19:12:31-04:00",
          "tree_id": "5db269c3b030282c375195f2c01fbe94b8459a95",
          "url": "https://github.com/unicode-org/icu/commit/26733782601ec384b8070019a8121795f01000cd"
        },
        "date": 1660864674543,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 245.4402183380872,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "andy.heninger@gmail.com",
            "name": "Andy Heninger",
            "username": "aheninger"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "b95c6b1f3eb12eb84c2dabe438fe36be55a0772c",
          "message": "ICU-21946 RBBI Break Cache Optimizations\n\nAdjust RuleBasedBreakIterator::BreakCache::populateNear() to retain the cache\nthe cache contents in additional cases where are still useful, resulting in\nimproved performance.\n\nThis change is related to PR #2039, which addressed the same problem. This one\nretains the cache contents in more situations.",
          "timestamp": "2022-08-20T16:16:30-07:00",
          "tree_id": "4d8c5b400952f14136ae2a89ba22db2a736a2745",
          "url": "https://github.com/unicode-org/icu/commit/b95c6b1f3eb12eb84c2dabe438fe36be55a0772c"
        },
        "date": 1661037786282,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.98053891469314,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard.purdie@linuxfoundation.org",
            "name": "Richard Purdie",
            "username": "rpurdie"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "4ac7cd13938d44137bbf3949bcb7b63ff8bfaf23",
          "message": "ICU-22109 icu-config hardcodes build paths unnecessarily\n\nThe makefile hardcodes paths to the build directory into icu-config. It doesn’t\nneed to do this and it unnecessarily breaks build reproducibility. This patch\nmakes a simple change to avoid this.\n\nSigned-off-by: Richard Purdie <richard.purdie@linuxfoundation.org>",
          "timestamp": "2022-08-22T15:02:39-05:00",
          "tree_id": "2f9a426bc80fc925c0774e0e8263f93d45635d65",
          "url": "https://github.com/unicode-org/icu/commit/4ac7cd13938d44137bbf3949bcb7b63ff8bfaf23"
        },
        "date": 1661198998772,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 229.6554863696348,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "ca9bdb97801cb1d0383c36b66b43bf0587b7697a",
          "message": "ICU-21957 integrate CLDR release-42-alpha2 to ICU main for 72",
          "timestamp": "2022-08-22T13:07:59-07:00",
          "tree_id": "b48551e7f45f096b68a791d5158448659d5c0d62",
          "url": "https://github.com/unicode-org/icu/commit/ca9bdb97801cb1d0383c36b66b43bf0587b7697a"
        },
        "date": 1661199178744,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.7782827426447,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "allenwtsu@google.com",
            "name": "allenwtsu",
            "username": "allensu05"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "8c669a7c2e6da8a1fbff1c5054bfffccb115c769",
          "message": "ICU-22012 Add more Japanese words into the dictionary",
          "timestamp": "2022-08-23T10:18:45-07:00",
          "tree_id": "9f7136671717ceea712b975ae541f65937d4ab53",
          "url": "https://github.com/unicode-org/icu/commit/8c669a7c2e6da8a1fbff1c5054bfffccb115c769"
        },
        "date": 1661275463017,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.92532502691896,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "49d192fefe09fcc38547203487cf4e63d2dad61f",
          "message": "ICU-22112 word break updates for @,colon; colon tailorings for fi,sv\n\nSee #2159",
          "timestamp": "2022-08-23T12:45:55-07:00",
          "tree_id": "860bf4fdbcdb8953fab05089e4b3d0b92fbd27bc",
          "url": "https://github.com/unicode-org/icu/commit/49d192fefe09fcc38547203487cf4e63d2dad61f"
        },
        "date": 1661284317865,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.6009951555068,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "37e295627156bc334e1f1e88807025fac984da0e",
          "message": "ICU-21957 ICU4J API status and change report",
          "timestamp": "2022-08-26T11:33:50-07:00",
          "tree_id": "22673db46dd386a73e7498417bba169e1e62c780",
          "url": "https://github.com/unicode-org/icu/commit/37e295627156bc334e1f1e88807025fac984da0e"
        },
        "date": 1661539197084,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 263.5264783000443,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "3e6219ba4d309acb7f48deca96100127ade8d084",
          "message": "ICU-21957 BRS72 organize import statements",
          "timestamp": "2022-08-30T19:34:01-04:00",
          "tree_id": "b0d2f65c1c26b0904a5b30ddac24f9151758e67f",
          "url": "https://github.com/unicode-org/icu/commit/3e6219ba4d309acb7f48deca96100127ade8d084"
        },
        "date": 1661902806411,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 270.6043096265926,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "3d935f2d493bc613d9884a7878d04fb5515f5005",
          "message": "ICU-21957 BRS72 Updating currency numeric code data.",
          "timestamp": "2022-08-30T19:34:49-04:00",
          "tree_id": "f1ca2d572a984c9eac3146ef873f3cd4e95cdb74",
          "url": "https://github.com/unicode-org/icu/commit/3d935f2d493bc613d9884a7878d04fb5515f5005"
        },
        "date": 1661903828119,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 219.83730219889253,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "5334e2819d769c5992dae2c07a999b90e26db308",
          "message": "ICU-21958 ICU 70 API Promotions",
          "timestamp": "2022-08-31T15:38:22-07:00",
          "tree_id": "3e789d667b6ae477513c5444f8705583ac00b853",
          "url": "https://github.com/unicode-org/icu/commit/5334e2819d769c5992dae2c07a999b90e26db308"
        },
        "date": 1661985780681,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 254.03943605303238,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "8050af54847348504a879564c28008203510201d",
          "message": "ICU-21980 Unicode 15 update 2022aug30",
          "timestamp": "2022-08-31T16:15:42-07:00",
          "tree_id": "95cbedfa8185c379bfc37eb45bca48716bae6869",
          "url": "https://github.com/unicode-org/icu/commit/8050af54847348504a879564c28008203510201d"
        },
        "date": 1661988133430,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 217.70286452528453,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "9acba58c493123234b9c3ed4326d35d8f73cbcc1",
          "message": "ICU-22116 Updating minimum Java runtime support to Java 8",
          "timestamp": "2022-09-01T13:02:27-04:00",
          "tree_id": "3effaf2286da632e64849215f3216eb05a4448f1",
          "url": "https://github.com/unicode-org/icu/commit/9acba58c493123234b9c3ed4326d35d8f73cbcc1"
        },
        "date": 1662052197622,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.519443686503,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "4ab713b1c6fb604d3854e7781bee2051878d6814",
          "message": "ICU-22081 Added missing copyright notice to PersonName.java.",
          "timestamp": "2022-09-01T13:36:05-07:00",
          "tree_id": "30653b09944911ad017a064338ff8254052c36ae",
          "url": "https://github.com/unicode-org/icu/commit/4ab713b1c6fb604d3854e7781bee2051878d6814"
        },
        "date": 1662064962317,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 234.60114604354393,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nrunge@google.com",
            "name": "gnrunge",
            "username": "gnrunge"
          },
          "committer": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "distinct": true,
          "id": "baa104b50b51218b35e4bd629aa45f3ea88a4a96",
          "message": "ICU-21957 Clean-up of TODO and logKnownIssue entries (BRS task):\n\nRemoved logKnownIssue(ICU-21322) in plurults.cpp, ICU-21322 is done and the\nentire if-statement was commented out.\n\nReplaced CLDR-13700 with CLDR-13701 in several TODOs. 13700 is a duplicate of\n13701.\n\nLikewise for CLDR-14502 --> CLDR-14582.\n\nPR#1999 from ICU 71 release missed some of the cases.",
          "timestamp": "2022-09-02T09:54:57-07:00",
          "tree_id": "91b7432c24997b436155399ab6c285c33f6ed5ee",
          "url": "https://github.com/unicode-org/icu/commit/baa104b50b51218b35e4bd629aa45f3ea88a4a96"
        },
        "date": 1662137919576,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 238.05698610287692,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango",
            "username": "echeran"
          },
          "distinct": true,
          "id": "6e3a9230563b97cb925723a6d9e91888396f2035",
          "message": "ICU-22116 Update CI job for ICU4J to use Java 8 instead of Java 7\n\nSee #2173",
          "timestamp": "2022-09-06T09:09:22-07:00",
          "tree_id": "c82f414f17d7a8f69e9a123155d5900893e5f77e",
          "url": "https://github.com/unicode-org/icu/commit/6e3a9230563b97cb925723a6d9e91888396f2035"
        },
        "date": 1662480846504,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.37542652685144,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nrunge@google.com",
            "name": "gnrunge",
            "username": "gnrunge"
          },
          "committer": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "distinct": true,
          "id": "00003dcbf2065d79fa45f8d53ef779861f71adb5",
          "message": "ICU-21957 Update TODO ticket reference: CLDR-13044 (done) ---> ICU-21420 (open).",
          "timestamp": "2022-09-06T09:14:07-07:00",
          "tree_id": "4d26f8c373ecc777e50d8ad362170299340629a3",
          "url": "https://github.com/unicode-org/icu/commit/00003dcbf2065d79fa45f8d53ef779861f71adb5"
        },
        "date": 1662481684260,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.33224800573674,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "030fa1a4791ee7c2f58505ebb61253c3032916ec",
          "message": "ICU-21148 Consistently use standard lowercase true/false everywhere.\n\nThis is the normal standard way in C, C++ as well as Java and there's no\nlonger any reason for ICU to be different. The various internal macros\nproviding custom boolean constants can all be deleted and code as well\nas documentation can be updated to use lowercase true/false everywhere.",
          "timestamp": "2022-09-07T20:56:33+02:00",
          "tree_id": "849f746cda1267d213f8ae31b557ea7d847f3acc",
          "url": "https://github.com/unicode-org/icu/commit/030fa1a4791ee7c2f58505ebb61253c3032916ec"
        },
        "date": 1662577384138,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 219.12788996068647,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "cyndy_ishida@apple.com",
            "name": "Cyndy Ishida",
            "username": "cyndyishida"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "03b94e9cb3045072c73d50938ee0e14597d24ce0",
          "message": "ICU-22068 Cleanup inconsistent annotations between declarations and definitions\n\nThis cleans up inconsistent annotations between declared APIs in headers\nvs defined implementations in cpp's. This better ensures the API's\nreferenceable in headers represent what is exposed and defined in the\nultimate binary library's symbol table.",
          "timestamp": "2022-09-08T08:34:56-07:00",
          "tree_id": "a65dc75f11c10725ce59a87a1d3376169142f030",
          "url": "https://github.com/unicode-org/icu/commit/03b94e9cb3045072c73d50938ee0e14597d24ce0"
        },
        "date": 1662651450064,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 220.24251927499122,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "rp9-next",
            "username": "rp9-next"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "bebcd6b8bb3239b2288fcf279ad1a1b5b3b68d1d",
          "message": "ICU-22114 Update pipeline workflows to use macOS-latest",
          "timestamp": "2022-09-08T09:02:31-07:00",
          "tree_id": "6d3af8477fed20b557199ee7fe82c616f56eb3b8",
          "url": "https://github.com/unicode-org/icu/commit/bebcd6b8bb3239b2288fcf279ad1a1b5b3b68d1d"
        },
        "date": 1662653377674,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.9698486176115,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "cyndy_ishida@apple.com",
            "name": "Cyndy Ishida",
            "username": "cyndyishida"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "48124d170635f4abcd7440c5beeb6ea9878de32b",
          "message": "ICU-22072 Update Darwin Specific Macros\n\n* Update `U_PF_IPHONE` to be 0 when building for macOS/macCatalyst.\n* add macro definition for `attribute((visibility(\"hidden\")))` for cases\n  where internal structs exist within exposed classes.",
          "timestamp": "2022-09-08T09:08:57-07:00",
          "tree_id": "0dafe5b7ad2082eb0fdf56246a27bbef07669cd9",
          "url": "https://github.com/unicode-org/icu/commit/48124d170635f4abcd7440c5beeb6ea9878de32b"
        },
        "date": 1662654052539,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 239.2433426742894,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "1de1e36d6f6050d05b2db027f4c64ae6823eae2a",
          "message": "ICU-21957 integrate CLDR release-42-alpha3 to ICU main for 72",
          "timestamp": "2022-09-08T18:19:10-07:00",
          "tree_id": "43c37bedd13165396856bfd9fd7a067a73e93291",
          "url": "https://github.com/unicode-org/icu/commit/1de1e36d6f6050d05b2db027f4c64ae6823eae2a"
        },
        "date": 1662686675725,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 219.81103126470242,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "d3a753a0d4a3e6e9d4b5392ec7a0777d229c71f5",
          "message": "ICU-21957 BRS 72rc, update urename.h",
          "timestamp": "2022-09-08T18:19:30-07:00",
          "tree_id": "0c076a705771be959218146920f7658d04800557",
          "url": "https://github.com/unicode-org/icu/commit/d3a753a0d4a3e6e9d4b5392ec7a0777d229c71f5"
        },
        "date": 1662687179875,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 234.52766705393228,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "5de6ee0c61b38feb4e35b842bc81d9d8030d2afa",
          "message": "ICU-21959 fix DateIntervalFormat general usage example",
          "timestamp": "2022-09-08T18:59:19-07:00",
          "tree_id": "0397c38bdb1077ee68baebac985ebe4ad514188a",
          "url": "https://github.com/unicode-org/icu/commit/5de6ee0c61b38feb4e35b842bc81d9d8030d2afa"
        },
        "date": 1662689493630,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.11458013772793,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "44496574+floratmin@users.noreply.github.com",
            "name": "floratmin",
            "username": "floratmin"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "1dfe456fe8a98de1f8ea966c45ef157f16160da6",
          "message": "ICU-21983 Fix fraction precision skeleton\n\nSee #2058",
          "timestamp": "2022-09-08T20:17:48-07:00",
          "tree_id": "3d308762dd0f304b04e153e238616d745d769ee9",
          "url": "https://github.com/unicode-org/icu/commit/1dfe456fe8a98de1f8ea966c45ef157f16160da6"
        },
        "date": 1662693765392,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 258.26807467159017,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "baee21aa7ac4ad952dd93c3430326f35cc54941b",
          "message": "ICU-22125 Add note about future deprecation to MeasureUnit createMetricTon/getMetricTon/METRIC_TON",
          "timestamp": "2022-09-08T20:59:07-07:00",
          "tree_id": "0de5e44d6f2ee5329452d8a04fed8b26ad09528d",
          "url": "https://github.com/unicode-org/icu/commit/baee21aa7ac4ad952dd93c3430326f35cc54941b"
        },
        "date": 1662696763986,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.79387545113914,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "b9fdd2a7cfcf77454762ce9479bdfc40fe14168e",
          "message": "ICU-22025 Rename the enum type for Hebrew calendar to Month",
          "timestamp": "2022-09-09T01:29:06-07:00",
          "tree_id": "7fa42099194411991193f0d32c484edf4a92baa6",
          "url": "https://github.com/unicode-org/icu/commit/b9fdd2a7cfcf77454762ce9479bdfc40fe14168e"
        },
        "date": 1662712360976,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.7239008258051,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "e4df3043677bf7fc10ec03c8cc37142fc46a7fda",
          "message": "ICU-21957 Update double-conversion to 256ac809561b756645e73ab7127c2aaaeabaa427\n\nSee #2179",
          "timestamp": "2022-09-09T15:47:12-07:00",
          "tree_id": "ca2a638aaa9226a53a37118d05ee384d7996789f",
          "url": "https://github.com/unicode-org/icu/commit/e4df3043677bf7fc10ec03c8cc37142fc46a7fda"
        },
        "date": 1662764458846,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.3134440732201,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "krlmlr@users.noreply.github.com",
            "name": "Kirill Müller",
            "username": "krlmlr"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "a48ae42864e8fcb702a5dfb6f6a076e4dde7e397",
          "message": "ICU-22117 Replace uprv_strncpy() by uprv_memcpy()\n\nThis fixes a warning on gcc 9.4.0, which is triggered because the third argument to strncpy() depends on the length of the second argument (but should actually indicate the buffer size). Replacing by memcpy() seems harmless because a null terminator is appended further below, and the buffer is sized to be \"large enough\" elsewhere.\n\nSee https://github.com/duckdb/duckdb/issues/4391 for details.\n\nFixing the warning is important for us, because the checks in the duckdb repository treat all warnings as errors.",
          "timestamp": "2022-09-09T17:07:53-07:00",
          "tree_id": "5204a95a33d0eb3b78a5a30a3fee3cee294a0da7",
          "url": "https://github.com/unicode-org/icu/commit/a48ae42864e8fcb702a5dfb6f6a076e4dde7e397"
        },
        "date": 1662769402689,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 262.1528524197856,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "99dee47fb783b1839af5cc742797ba5cdf8924ac",
          "message": "ICU-21959 add the uemoji feature to ICU Data Build Tool chapter",
          "timestamp": "2022-09-12T08:57:06-07:00",
          "tree_id": "5d7f297d26373540044bd9d1355a8b92bf240108",
          "url": "https://github.com/unicode-org/icu/commit/99dee47fb783b1839af5cc742797ba5cdf8924ac"
        },
        "date": 1662998519345,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.2574755244747,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "cfd99f3f3f309fc224a43377395085bea58b58cb",
          "message": "ICU-22143 Increase COMPACT_MAX_DIGITS from 15 to 20, needed for new ja data",
          "timestamp": "2022-09-12T17:17:19-07:00",
          "tree_id": "587d6e1872cc987a2bd6e5d4d8f00882be138e94",
          "url": "https://github.com/unicode-org/icu/commit/cfd99f3f3f309fc224a43377395085bea58b58cb"
        },
        "date": 1663028631906,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 243.39145047725992,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "06259cc6c33d220108d68818a9cdaec8942f9552",
          "message": "ICU-21957 integrate CLDR release-42-beta1 to ICU main for 72",
          "timestamp": "2022-09-13T11:18:37-07:00",
          "tree_id": "cb8a27062497043f6cc5be9122738e0d43981cad",
          "url": "https://github.com/unicode-org/icu/commit/06259cc6c33d220108d68818a9cdaec8942f9552"
        },
        "date": 1663093590799,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.40415929723864,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "db5903479332caf213e3d6813dfa75ed7e91720c",
          "message": "ICU-22124 Adding a tech preview implementation of MessageFormat v2\n\nSee #2170",
          "timestamp": "2022-09-15T10:32:13-07:00",
          "tree_id": "5bf067f7e5996c2571bf2c901465dd8757b4af77",
          "url": "https://github.com/unicode-org/icu/commit/db5903479332caf213e3d6813dfa75ed7e91720c"
        },
        "date": 1663263431665,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 266.1084571766006,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "b7340487995b6db9e70567690d2d14870739e97f",
          "message": "ICU-21957 fix NumberFormatterSettings::unitDisplayCase status, remove FormattedNumber:getGender",
          "timestamp": "2022-09-16T08:34:28-07:00",
          "tree_id": "9b37f45d9b4d6104f4c533de4df87c275083a6b9",
          "url": "https://github.com/unicode-org/icu/commit/b7340487995b6db9e70567690d2d14870739e97f"
        },
        "date": 1663342747646,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.88942186186827,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "9dc1c020a19a5b950af3e6304b97e7396b92aaaf",
          "message": "ICU-20512 Add extra matchers to handle empty currency symbols",
          "timestamp": "2022-09-16T09:00:50-07:00",
          "tree_id": "24a6fbc2a421138fe4900be8283523091a3cfbc7",
          "url": "https://github.com/unicode-org/icu/commit/9dc1c020a19a5b950af3e6304b97e7396b92aaaf"
        },
        "date": 1663344199757,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 238.9240052817248,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "498abf69611ca32ed41c5811f82ab7dfcb244757",
          "message": "ICU-21125 Improvements to resource fallback:\n\n- Added code to use the parentLocales data in supplementalData.xml to determine the \"parent locale ID\" to use when\n  the requested resource bundle is not present (ICU-21126).\n- Added code to change the parent-chain search path to handle the script better (ICU-21125; algorithm was described\n  in CLDR-15265):\n  - The base search patch is now ll_Ssss_RR -> ll_RR -> ll_Ssss -> ll -> root\n  - If the requested script is not the default script for the requested language and region, we automatically\n    avoid fallbacks that will implicitly change the script.\n- Added new code to the CLDR-to-ICU data generation tool to generate source code, and used it to generate the lookup\n  tables for the new resource-fallback logic (we can't use the existing resource files for this, since that would\n  involve opening a resource bundle while trying to open another resource bundle).  The data-generation stuff is\n  intended to be generic enough to allow for us to generate more static data tables in the future.\n- Commented out a few collator tests, and changed one resource bundle test, because they're incompatible with the\n  new fallback logic (specifically, the default-script logic).",
          "timestamp": "2022-09-16T14:26:50-07:00",
          "tree_id": "4e378c360728a7aa22646c603405ea06d07168d3",
          "url": "https://github.com/unicode-org/icu/commit/498abf69611ca32ed41c5811f82ab7dfcb244757"
        },
        "date": 1663364018089,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 232.90528560167328,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "17435c4533c173b228d60e47413f35c1c6a9d748",
          "message": "ICU-22151 Update timezone-region supplementation mapping data for Jan Mayen and some others",
          "timestamp": "2022-09-16T16:49:42-07:00",
          "tree_id": "b53cde68b9a46dfbf1b6ce2fe8afcb340e7a4275",
          "url": "https://github.com/unicode-org/icu/commit/17435c4533c173b228d60e47413f35c1c6a9d748"
        },
        "date": 1663372970975,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 254.12626264627676,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "e646ea23e9988fff220ae8c135adf577dfe4c65c",
          "message": "ICU-20512 ICU4J: just add test of parse with empty curr symbol, code already works",
          "timestamp": "2022-09-16T19:01:06-07:00",
          "tree_id": "99ac611eaa8870e031f49960b83b84848d839c25",
          "url": "https://github.com/unicode-org/icu/commit/e646ea23e9988fff220ae8c135adf577dfe4c65c"
        },
        "date": 1663380522516,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.29734317412675,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "b403a10e514d7317999c78a53e300b15c85616a5",
          "message": "ICU-21957 integrate CLDR release-42-beta1b to ICU main for 72",
          "timestamp": "2022-09-19T14:43:59-07:00",
          "tree_id": "04722e603f2a579cf386a39865f6ee0178114e18",
          "url": "https://github.com/unicode-org/icu/commit/b403a10e514d7317999c78a53e300b15c85616a5"
        },
        "date": 1663624303707,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.84714970276897,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "f5367befba65cc6a94cea5ff557bf72f1af9254c",
          "message": "ICU-22124 Update the MessageFormat v2 links to the main branch",
          "timestamp": "2022-09-20T16:47:38-07:00",
          "tree_id": "6a3c5be851b439f5e83d53d6b21a2431eedb8e83",
          "url": "https://github.com/unicode-org/icu/commit/f5367befba65cc6a94cea5ff557bf72f1af9254c"
        },
        "date": 1663718566869,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.46485547364657,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "dbfe83010856a58698edf68476ef693236f49a5a",
          "message": "ICU-22122 Support Locale Tags (ms, mu and rg)\n\nSee #2182",
          "timestamp": "2022-09-20T16:51:24-07:00",
          "tree_id": "4817fd24c8d0124c0b6375b844fafa8a93c32998",
          "url": "https://github.com/unicode-org/icu/commit/dbfe83010856a58698edf68476ef693236f49a5a"
        },
        "date": 1663719130589,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 220.5194242340304,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "rp9.next@gmail.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "b08e51fa911c03c9b98eed36594b1c1970e2ccf9",
          "message": "ICU-21957 BRS72RC Update version number and regenerate configure",
          "timestamp": "2022-09-21T12:06:18+05:30",
          "tree_id": "1cdef136e269849dc95bf823bc26c3cf34ced39d",
          "url": "https://github.com/unicode-org/icu/commit/b08e51fa911c03c9b98eed36594b1c1970e2ccf9"
        },
        "date": 1663742603805,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.85562160656147,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "b23d6c1afe339a09da31943f5fe104cd3f2689ff",
          "message": "ICU-22124 Tag the default constructor of OrderedMap as internal/deprecated\n\nICU-22124 Tag the default constructor of OrderedMap as internal/deprecated\r\n\r\nFix for apireport, see #2193",
          "timestamp": "2022-09-21T11:50:09-07:00",
          "tree_id": "2d04f8c14ced0ad2bb157e2c6f7df13de67bcf0d",
          "url": "https://github.com/unicode-org/icu/commit/b23d6c1afe339a09da31943f5fe104cd3f2689ff"
        },
        "date": 1663786760904,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.16401902179368,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "4f707beadeb1bccdbe15fc45c7fa03b13141a959",
          "message": "ICU-21957 Update ICU4J & ICU4C Change Reports BRS#19 and BRS#20\n\nSee #2193",
          "timestamp": "2022-09-21T16:16:28-07:00",
          "tree_id": "525225c26a294115e70799ecf39e6a85b6810271",
          "url": "https://github.com/unicode-org/icu/commit/4f707beadeb1bccdbe15fc45c7fa03b13141a959"
        },
        "date": 1663802453867,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.6318609493684,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "329c95e3b9df2d22fb20b02044a035fb509ef5f4",
          "message": "ICU-21957 BRS72 Cleanup import statements",
          "timestamp": "2022-09-22T08:07:12-04:00",
          "tree_id": "fd306139753f0b8e767268d57390120887aa73d9",
          "url": "https://github.com/unicode-org/icu/commit/329c95e3b9df2d22fb20b02044a035fb509ef5f4"
        },
        "date": 1663848646853,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 217.7991481845787,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "0c9d6f6b856c698bf8631c3971ff1fd8581b402b",
          "message": "ICU-21957 Fix status tags for U_HIDDEN, unum_hasAttribute",
          "timestamp": "2022-09-22T10:50:22-07:00",
          "tree_id": "284aa4e537dcb92e7f3c21d350654e469bc666fb",
          "url": "https://github.com/unicode-org/icu/commit/0c9d6f6b856c698bf8631c3971ff1fd8581b402b"
        },
        "date": 1663869645662,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.3667049615437,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "b5acb0ffc2fee16bf09bb657b19278c60f9f03e0",
          "message": "ICU-21957 Fixed java compiler warnings",
          "timestamp": "2022-09-22T16:37:56-04:00",
          "tree_id": "29fa83c5f6eaf9f033d4bd23b94f0a6101619241",
          "url": "https://github.com/unicode-org/icu/commit/b5acb0ffc2fee16bf09bb657b19278c60f9f03e0"
        },
        "date": 1663879491435,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 235.64071835090547,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "306be3ddf6160ac4799a20dc67704891101dc9bd",
          "message": "ICU-21957 BRS72 J API signature file",
          "timestamp": "2022-09-22T16:39:47-04:00",
          "tree_id": "b9e110ee3d076db6c0277539125e2e7d750e8cc0",
          "url": "https://github.com/unicode-org/icu/commit/306be3ddf6160ac4799a20dc67704891101dc9bd"
        },
        "date": 1663880144340,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.81658384942313,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "ef706cddf1aa4a94b796411925bbf4be63a110a8",
          "message": "ICU-21957 BRS72 ICU4J serialization test data",
          "timestamp": "2022-09-22T16:49:16-04:00",
          "tree_id": "7a931aa92a3a77163857f33bf61fc2279fb77205",
          "url": "https://github.com/unicode-org/icu/commit/ef706cddf1aa4a94b796411925bbf4be63a110a8"
        },
        "date": 1663880791076,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 242.39864199224894,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "cd12cebb8768edc4d47c158c903be40c32af4cde",
          "message": "ICU-21879 Fix UserGuide link to info in Korean transliteration",
          "timestamp": "2022-09-22T13:54:40-07:00",
          "tree_id": "92eec4df02a7500358b90dad09e005b9fd90cac1",
          "url": "https://github.com/unicode-org/icu/commit/cd12cebb8768edc4d47c158c903be40c32af4cde"
        },
        "date": 1663882095555,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 252.30215854739015,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "7c1f8d675b2424e07942010cfd86feabaa83a6cc",
          "message": "ICU-21957 update API Changes in ICU4C 72",
          "timestamp": "2022-09-22T14:55:24-07:00",
          "tree_id": "c83ef7f23364370852bacc7dbe033e770de8dd2b",
          "url": "https://github.com/unicode-org/icu/commit/7c1f8d675b2424e07942010cfd86feabaa83a6cc"
        },
        "date": 1663884041318,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 286.6956744604955,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "8f5529f30d25b282dfd7b12f87b1b69ab704a0eb",
          "message": "ICU-20894 Add UserGuide info on date pattern chars B, b",
          "timestamp": "2022-09-22T16:14:44-07:00",
          "tree_id": "1d83d348bba378665bf1005bb1d2c53b30ca1bb6",
          "url": "https://github.com/unicode-org/icu/commit/8f5529f30d25b282dfd7b12f87b1b69ab704a0eb"
        },
        "date": 1663888837961,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 355.37587513330357,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "123e5c1cd6f033a1f52f71c879cf93228d2f7387",
          "message": "ICU-21957 Change the internal/deprecated javadoc tags",
          "timestamp": "2022-09-22T16:31:38-07:00",
          "tree_id": "2500781058c4d621fcfeec34ef1ac2ccc9f7fcae",
          "url": "https://github.com/unicode-org/icu/commit/123e5c1cd6f033a1f52f71c879cf93228d2f7387"
        },
        "date": 1663889950512,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.33364777587602,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "123e5c1cd6f033a1f52f71c879cf93228d2f7387",
          "message": "ICU-21957 Change the internal/deprecated javadoc tags",
          "timestamp": "2022-09-22T16:31:38-07:00",
          "tree_id": "2500781058c4d621fcfeec34ef1ac2ccc9f7fcae",
          "url": "https://github.com/unicode-org/icu/commit/123e5c1cd6f033a1f52f71c879cf93228d2f7387"
        },
        "date": 1663891024201,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.98971955340028,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "2f07ce2c6cdae63cea58efb98deeed8fe5f4e8c5",
          "message": "ICU-22158 Make TestAlgorithmicParentFallback() test more robust to different default locales.",
          "timestamp": "2022-09-23T13:37:42-07:00",
          "tree_id": "fcd14af32a7124526598b13771c483a8314acedd",
          "url": "https://github.com/unicode-org/icu/commit/2f07ce2c6cdae63cea58efb98deeed8fe5f4e8c5"
        },
        "date": 1663965900277,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 239.39988027002224,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "e5980f1dcb9c3700c2d5b9b4d7f3641279cf01c6",
          "message": "ICU-22081 Fix table in PersonNameFormatter Javadoc.",
          "timestamp": "2022-09-26T16:17:17-07:00",
          "tree_id": "7e8bb38007c260785083dae5351b83062631707e",
          "url": "https://github.com/unicode-org/icu/commit/e5980f1dcb9c3700c2d5b9b4d7f3641279cf01c6"
        },
        "date": 1664235048537,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 253.93761367917597,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "e72233f8b7526bbb840ae7742948a892686acfee",
          "message": "ICU-21959 fix file-level doxygen issues",
          "timestamp": "2022-09-27T15:58:43-07:00",
          "tree_id": "e935fae45795ccdd00637839bd409b521f5265ff",
          "url": "https://github.com/unicode-org/icu/commit/e72233f8b7526bbb840ae7742948a892686acfee"
        },
        "date": 1664320261964,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 272.18401544436546,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "49b08b414d5cb03901eaeca5e223ec835f59d3c5",
          "message": "ICU-21958 integrate CLDR release-42-beta2 to ICU main for 72",
          "timestamp": "2022-09-29T10:12:36-07:00",
          "tree_id": "9f70acc7afe174eafa0a00cbe205f70fc144472d",
          "url": "https://github.com/unicode-org/icu/commit/49b08b414d5cb03901eaeca5e223ec835f59d3c5"
        },
        "date": 1664471867135,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.98694217875249,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "702e529d31acf4a2ae225147876be7ac99c11a74",
          "message": "ICU-22165 Update ICU tzdata to 2022d",
          "timestamp": "2022-09-30T15:56:00-04:00",
          "tree_id": "0ac7309770029b5e6a07ef9634629c85e5de1c75",
          "url": "https://github.com/unicode-org/icu/commit/702e529d31acf4a2ae225147876be7ac99c11a74"
        },
        "date": 1664568134134,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.35125968759988,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "fe17bab2bb1499a30f10453228ab31173f0d2e83",
          "message": "ICU-21958 integrate CLDR release-42-beta3 to ICU main for 72",
          "timestamp": "2022-10-06T08:40:32-07:00",
          "tree_id": "d7afe17aae18d2fa4edc38a5a0d269d7ca13be5b",
          "url": "https://github.com/unicode-org/icu/commit/fe17bab2bb1499a30f10453228ab31173f0d2e83"
        },
        "date": 1665071142455,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 311.23235394079074,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "599ecdc4a181062c47ea500a72145c40e42982d8",
          "message": "ICU-21958 Improved process for Releasing ICU API Docs",
          "timestamp": "2022-10-06T09:07:55-07:00",
          "tree_id": "d63d033e13575c68686acb09a088dff586018ce8",
          "url": "https://github.com/unicode-org/icu/commit/599ecdc4a181062c47ea500a72145c40e42982d8"
        },
        "date": 1665072866557,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 248.4132013694622,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "be9a07907c7bc452bdcf9ec8b3511338718aa2cf",
          "message": "ICU-21958 Fix typo in displayoptions.h documentation",
          "timestamp": "2022-10-06T11:21:21-07:00",
          "tree_id": "2071800fd38dfad3febb25135d4322865b313800",
          "url": "https://github.com/unicode-org/icu/commit/be9a07907c7bc452bdcf9ec8b3511338718aa2cf"
        },
        "date": 1665080866542,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 261.87025320806805,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "felipe@felipegasper.com",
            "name": "Felipe Gasper",
            "username": "FGasper"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "ff4ecd9f5017b940a7a29e5a25a70289dc3652db",
          "message": "ICU-22170 Fix typo in resource bundle documentation.\n\nhttps://unicode-org.atlassian.net/browse/ICU-22170",
          "timestamp": "2022-10-06T13:00:31-07:00",
          "tree_id": "7cbd9ccfdce9b92a9b79154d1263690414f6ac98",
          "url": "https://github.com/unicode-org/icu/commit/ff4ecd9f5017b940a7a29e5a25a70289dc3652db"
        },
        "date": 1665086812844,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.45433863079592,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "c203003b173fb867db26ebb2e838ca142400d1bb",
          "message": "ICU-21958 Revise C++ API Change reports\n\nAfter updating icu4c displayoptions.h",
          "timestamp": "2022-10-07T09:57:56-07:00",
          "tree_id": "09efaf3a4e405b24546df4f4bf982b08910341a3",
          "url": "https://github.com/unicode-org/icu/commit/c203003b173fb867db26ebb2e838ca142400d1bb"
        },
        "date": 1665162241313,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 298.62991967463824,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "jharkawat@microsoft.com",
            "name": "JALAJ HARKAWAT",
            "username": "jalaj-microsoft"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "ea4fca604f975b88753abbcdbb8b80f707709894",
          "message": "ICU-21958 BRS72GA Update version number",
          "timestamp": "2022-10-11T22:19:20+05:30",
          "tree_id": "3c93225b8f8f2a7abbef6c56c17e697624e85860",
          "url": "https://github.com/unicode-org/icu/commit/ea4fca604f975b88753abbcdbb8b80f707709894"
        },
        "date": 1665507368709,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 245.53010225847356,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "c6c01f4b79bcd46377b71506d466ef0f54e21ec2",
          "message": "ICU-21958 integrate CLDR release-42-beta4 to ICU main for 72",
          "timestamp": "2022-10-11T19:44:30-07:00",
          "tree_id": "b299f9a7baebb8658bf821519d2ddc91ef1be6ef",
          "url": "https://github.com/unicode-org/icu/commit/c6c01f4b79bcd46377b71506d466ef0f54e21ec2"
        },
        "date": 1665542951571,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.0711957535724,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "a620057d76c2a107f04f924d13678b5aa4bfb0d0",
          "message": "ICU-21958 BRS72 Updated ISO currency data URLs",
          "timestamp": "2022-10-12T12:37:52-04:00",
          "tree_id": "c75f8e572065d8dac1beb717d1357487b82b1730",
          "url": "https://github.com/unicode-org/icu/commit/a620057d76c2a107f04f924d13678b5aa4bfb0d0"
        },
        "date": 1665593082017,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 260.2295174006201,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "00a4cdbd5be8efabc7141ad7eec3b457b415fcef",
          "message": "ICU-22178 Update ICU tzdata to 2022e",
          "timestamp": "2022-10-12T21:07:34-04:00",
          "tree_id": "fb525902cbff3e9a7be2469d45821cc35074267b",
          "url": "https://github.com/unicode-org/icu/commit/00a4cdbd5be8efabc7141ad7eec3b457b415fcef"
        },
        "date": 1665623683169,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 238.03601085863477,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "954d999126e5960cb32e80ce642870735949760f",
          "message": "ICU-21755 commit checker: skip No Time To Do This\n\n- also, verify that ALL resolutions are accounted for.",
          "timestamp": "2022-10-13T12:05:17-05:00",
          "tree_id": "05c4bb3a5c032f31313cfc3d3853f5521bf712da",
          "url": "https://github.com/unicode-org/icu/commit/954d999126e5960cb32e80ce642870735949760f"
        },
        "date": 1665681124668,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 264.1709510384078,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nrunge@google.com",
            "name": "gnrunge",
            "username": "gnrunge"
          },
          "committer": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "distinct": true,
          "id": "f949713ce8647a1b35031ed81e578b3a19cb05c6",
          "message": "ICU-22177 Replace angular brackets in include with double quotes.",
          "timestamp": "2022-10-13T13:51:05-07:00",
          "tree_id": "aeb72421b7346d9a5a1af2131d06fd709c95b12c",
          "url": "https://github.com/unicode-org/icu/commit/f949713ce8647a1b35031ed81e578b3a19cb05c6"
        },
        "date": 1665694639941,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.71034821539925,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "ff3514f257ea10afe7e710e9f946f68d256704b1",
          "message": "ICU-21958 integrate CLDR release-42-beta5 to ICU main for 72",
          "timestamp": "2022-10-13T16:23:11-07:00",
          "tree_id": "1434057e6698a7015f43aae6e113805e7d938078",
          "url": "https://github.com/unicode-org/icu/commit/ff3514f257ea10afe7e710e9f946f68d256704b1"
        },
        "date": 1665703505241,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.75113162047177,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "649c5f0176b2f737155130b2fa63a2f5c9b7d07a",
          "message": "ICU-22189 Merge maint/maint-72 to main (#2235)",
          "timestamp": "2022-10-20T13:53:44-07:00",
          "tree_id": "247df49532cbacf779bb05db3c4f257ea7cf9ef0",
          "url": "https://github.com/unicode-org/icu/commit/649c5f0176b2f737155130b2fa63a2f5c9b7d07a"
        },
        "date": 1666299364980,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.02581975587808,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "rp9-next",
            "username": "rp9-next"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "511b4111f2f1d97a70d08c6a885c333a8542a729",
          "message": "ICU-22190 Add public PGP Key\n\nSee #2236",
          "timestamp": "2022-10-20T23:29:13+02:00",
          "tree_id": "ba77322f3b9d45d1dc97c474231d3dbd2a211139",
          "url": "https://github.com/unicode-org/icu/commit/511b4111f2f1d97a70d08c6a885c333a8542a729"
        },
        "date": 1666301847781,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 220.00990530734782,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "d453c12bfacde783b7e985cdb4eb15c6db277f78",
          "message": "ICU-22190 Update KEYS with additional public key\n\nSee #2237",
          "timestamp": "2022-10-21T02:13:32+02:00",
          "tree_id": "0fc2e58be4144d2ed6f6e966dda6667392347a4b",
          "url": "https://github.com/unicode-org/icu/commit/d453c12bfacde783b7e985cdb4eb15c6db277f78"
        },
        "date": 1666311641008,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.30366607232588,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "fbb4a5a167192d2d918ee0020e8e084c299de97d",
          "message": "ICU-22190 Update KEYS with additional signature data.",
          "timestamp": "2022-10-24T12:39:23-07:00",
          "tree_id": "031c0f6d3b697be99f016d7a0f08de1467a3af78",
          "url": "https://github.com/unicode-org/icu/commit/fbb4a5a167192d2d918ee0020e8e084c299de97d"
        },
        "date": 1666640734430,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 289.7246240010768,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "andy.heninger@gmail.com",
            "name": "Andy Heninger",
            "username": "aheninger"
          },
          "committer": {
            "email": "andy.heninger@gmail.com",
            "name": "Andy Heninger",
            "username": "aheninger"
          },
          "distinct": true,
          "id": "866254ef12750bda67b86cc685d07e7906765aac",
          "message": "ICU-21180 BreakIterator, change all NULL to nulptr\n\nIn the C++ break iterator code, change all use of NULL to nullptr.\nThis is in preparation for follow-on PRs to improve out-of-memory error handling\nin Break Iterators, keeping use of nullptr consistent between old and new\nor updated code.",
          "timestamp": "2022-10-26T18:55:48-07:00",
          "tree_id": "f96afd8a247d599df392b27c6be44b2aa97565f5",
          "url": "https://github.com/unicode-org/icu/commit/866254ef12750bda67b86cc685d07e7906765aac"
        },
        "date": 1666836255071,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 267.4729485841778,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "vital.had@gmail.com",
            "name": "Sergey Fedorov",
            "username": "barracuda156"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "2b77e39fbbe08468aaa9102843b172a200019a9c",
          "message": "ICU-22191 writesrc.cpp: fix cinttypes header and place after C headers",
          "timestamp": "2022-10-28T08:41:22-07:00",
          "tree_id": "f82402c905121c4f1ffdb84918196b7ff9f31496",
          "url": "https://github.com/unicode-org/icu/commit/2b77e39fbbe08468aaa9102843b172a200019a9c"
        },
        "date": 1666971834551,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 265.86333684989944,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "e0f2d491aa35739e699a52803a1c968fd2e96397",
          "message": "ICU-22194 Fix typo in doc for BreakIterator rules update",
          "timestamp": "2022-10-28T14:37:49-07:00",
          "tree_id": "5d31f29279e1c1e35d3f099ab5fe34fdfed99b7a",
          "url": "https://github.com/unicode-org/icu/commit/e0f2d491aa35739e699a52803a1c968fd2e96397"
        },
        "date": 1666993502969,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.42536795110775,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "9f3feed545bf6acf6c9811c125e2c17b9e9d4a3e",
          "message": "ICU-22160 clean up Calendar code\n\nRemove unnecessary BuddhistCalendar::handleComputeMonthStart\nRemove unnecessary include",
          "timestamp": "2022-10-31T08:41:54-07:00",
          "tree_id": "857aad0e45023a84ad0ca41f441fc9b0ce7de298",
          "url": "https://github.com/unicode-org/icu/commit/9f3feed545bf6acf6c9811c125e2c17b9e9d4a3e"
        },
        "date": 1667231367634,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.77123154890924,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "294b26eb7aa2385a5232bb151320fa06e5266add",
          "message": "ICU-22159 Merge inDaylightTime to Calendar\n\nAll the subclass implementation of inDaylightTime are the same\nso just move to a base class implementation.",
          "timestamp": "2022-10-31T08:42:51-07:00",
          "tree_id": "121ade60a8dfe56a33501abdbb49999b88717c4b",
          "url": "https://github.com/unicode-org/icu/commit/294b26eb7aa2385a5232bb151320fa06e5266add"
        },
        "date": 1667231977291,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 263.25686348251224,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "436f5a7df59c4780dedc0149c6756c16f4c803ce",
          "message": "ICU-22194 runConfigureICU computer->compiler\n\n@josephshen found that the help text here has the wrong word, and I dropped the ball on his PR #2217 :-(",
          "timestamp": "2022-10-31T16:18:18-07:00",
          "tree_id": "451b22446725ab19d85013e389d39011e4932d3e",
          "url": "https://github.com/unicode-org/icu/commit/436f5a7df59c4780dedc0149c6756c16f4c803ce"
        },
        "date": 1667258718223,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.26168645320473,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "2d19377a8d5eb459acce1911919f8a76d37d1405",
          "message": "ICU-22196 TZ Database 2022f updates",
          "timestamp": "2022-11-01T20:21:18-04:00",
          "tree_id": "55628e3f94f78702d84fdce814fb936e691dc99e",
          "url": "https://github.com/unicode-org/icu/commit/2d19377a8d5eb459acce1911919f8a76d37d1405"
        },
        "date": 1667349278506,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 224.49720320220538,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "andy.heninger@gmail.com",
            "name": "Andy Heninger",
            "username": "aheninger"
          },
          "committer": {
            "email": "andy.heninger@gmail.com",
            "name": "Andy Heninger",
            "username": "aheninger"
          },
          "distinct": true,
          "id": "67a7e2caf063b5e591dbe738d3329607c8a2ba85",
          "message": "ICU-21180 RuleBasedBreakIterator, refactor init.\n\nIn class RuleBasedBreakIterator, refactor how object initialization is handled\nby the various constructors, taking advantage of C++11's ability to directly\ninitialize data members in the class declaration.\n\nThis will simplify ongoing maintenance of the code by eliminating the need\nto keep initialization lists synchronized with the class data members.\nThis is being done now in preparation for additional changes to fix problems\nwith the handling of memory allocation failures.",
          "timestamp": "2022-11-02T16:25:41-07:00",
          "tree_id": "21e27911b19a90c1ccfd226a7716b68991ea5f18",
          "url": "https://github.com/unicode-org/icu/commit/67a7e2caf063b5e591dbe738d3329607c8a2ba85"
        },
        "date": 1667431687796,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 217.22263119075626,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "jshin@chromium.org",
            "name": "Jungshik Shin",
            "username": "jungshik"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "05dc2ac924a0acd3f9a7ca65e8a6078f8f62d299",
          "message": "ICU-22119 Add lw=phrase for Korean using line_*_phrase_cj\n\nbrkitr/ko.txt is created to use line_*_.cj.txt for both\nlw=phrase and lw != phrase cases for Korean. This is the simplest\nway to fix ICU-22119 taking advantage of the fact that ICU\ndoes not have a Korean dictionary so we don't have to worry about\nadding the list of Korean particles to keep them attached to the\npreceeding word.\n\nThe downside is that it only works when the locale is ko or ja while\nit should work in any locale. Another is it makes ICU deviate from\nCSS3 by using the same CJ (conditonal Japanese) rules for Korean as\nwell. However, CSS3 spec is wrong on that point and should be changed.\nSee https://unicode-org.atlassian.net/browse/CLDR-4931 .",
          "timestamp": "2022-11-07T22:30:49Z",
          "tree_id": "194d3e6ae08e801ae8dc90e8fbf4188494fdfa6c",
          "url": "https://github.com/unicode-org/icu/commit/05dc2ac924a0acd3f9a7ca65e8a6078f8f62d299"
        },
        "date": 1667860829145,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.40604806413285,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "b00562e989af75bd68d326f1d5fc06ae1fae1036",
          "message": "ICU-22191 writesrc.cpp: enable PRI formatting constants on all compilers",
          "timestamp": "2022-11-09T04:38:00Z",
          "tree_id": "4d1d88b3e519c9ddacde6cd5e7c7875d3a193cab",
          "url": "https://github.com/unicode-org/icu/commit/b00562e989af75bd68d326f1d5fc06ae1fae1036"
        },
        "date": 1667968980903,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 249.24890571714047,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "b0ab1171ad3c577325c878e40c2c8cdce1354f60",
          "message": "ICU-10752 Spread (s|g)etRelativeYear to subclass\n\nRemove the switch statment implementaiton in\nCalendar::(g|s)etRelatedYear\nand move the code into each subclass as proper OOP style.",
          "timestamp": "2022-11-09T13:18:24-08:00",
          "tree_id": "461f511a96b20fb97b7aa89c970d083bc79a89d0",
          "url": "https://github.com/unicode-org/icu/commit/b0ab1171ad3c577325c878e40c2c8cdce1354f60"
        },
        "date": 1668029099701,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 233.1853505767485,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "9c1fb785b30ad8ce3177f1a6dab13ba224ef54f9",
          "message": "ICU-22164 Replace switch+getType with subclass\n\nSee #2215\n\nfix",
          "timestamp": "2022-11-11T14:49:16-08:00",
          "tree_id": "73d8753f29e0fd31bc6b0aaa0d0c66b5e01658a2",
          "url": "https://github.com/unicode-org/icu/commit/9c1fb785b30ad8ce3177f1a6dab13ba224ef54f9"
        },
        "date": 1668207095661,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 273.9707936662735,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "kobrineli@ispras.ru",
            "name": "Eli Kobrin",
            "username": "kobrineli"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "8b7ef3d9088c130cf0ef8973b30ceb39a40b605a",
          "message": "ICU-22198 Fix stack buffer overflow.",
          "timestamp": "2022-11-16T11:29:32-08:00",
          "tree_id": "c884bed743fc9d9f7b2c3133f5b3f804d75a7f82",
          "url": "https://github.com/unicode-org/icu/commit/8b7ef3d9088c130cf0ef8973b30ceb39a40b605a"
        },
        "date": 1668627481863,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 244.6672728977984,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "da5d3e0c5bea073bcf7d6c1e493211007c5112ea",
          "message": "ICU-22206 Fix unsafe mix of UBool\n\nFix \"unsafe mix of type 'UBool' and type 'bool' in operation:",
          "timestamp": "2022-11-18T10:36:55-08:00",
          "tree_id": "7bacd4bae7440ab1d91fe5c047ee2ac3344ad20b",
          "url": "https://github.com/unicode-org/icu/commit/da5d3e0c5bea073bcf7d6c1e493211007c5112ea"
        },
        "date": 1668797002909,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 240.09810534017413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "3654e945b68d5042cbf6254dd559a7ba794a76b3",
          "message": "ICU-22201 Add test to verify ICU-22043 on Java",
          "timestamp": "2022-11-21T17:42:16-08:00",
          "tree_id": "91f8c2a8bf92a8cfb345ec9401e2595a8f84c042",
          "url": "https://github.com/unicode-org/icu/commit/3654e945b68d5042cbf6254dd559a7ba794a76b3"
        },
        "date": 1669081592131,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.48548084947603,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "vichang@google.com",
            "name": "Victor Chang",
            "username": "gvictor"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "194236a1b4ba692a2ba4775cb7dbb2c6a0248f03",
          "message": "ICU-22214 Fix set pointer to false in sprpdata.c",
          "timestamp": "2022-11-28T14:48:18-08:00",
          "tree_id": "6501209b0409064a1b8342212a8b15a867576753",
          "url": "https://github.com/unicode-org/icu/commit/194236a1b4ba692a2ba4775cb7dbb2c6a0248f03"
        },
        "date": 1669675856863,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 220.38867907323075,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "a2854b615aa3876e15cda03eabbb6faf2e260fc2",
          "message": "ICU-22093 ICU4C: Add SimpleNumber and SimpleNumberFormatter\n\nSee #2241",
          "timestamp": "2022-11-28T20:28:50-08:00",
          "tree_id": "6671c62eaa101f144181dba98b1427b44c9f85bb",
          "url": "https://github.com/unicode-org/icu/commit/a2854b615aa3876e15cda03eabbb6faf2e260fc2"
        },
        "date": 1669696433465,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 218.025985090809,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "cecd19e9ba46db107b02a9de62b2ae34e199360b",
          "message": "ICU-22217 TZ Database 2022g updates",
          "timestamp": "2022-11-30T22:08:08-05:00",
          "tree_id": "e19f9bc448a7622e50cd56b6c57a23e6534d0ced",
          "url": "https://github.com/unicode-org/icu/commit/cecd19e9ba46db107b02a9de62b2ae34e199360b"
        },
        "date": 1669864203831,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.71159971318784,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "d02b30fc3f665677a775950b550179a8b3b736b2",
          "message": "ICU-20115 ICU4C: Move SimpleDateFormat over to SimpleNumberFormatter",
          "timestamp": "2022-12-01T09:40:55-08:00",
          "tree_id": "ce7707da3b9edf6f880f231053c5b9626e5701e9",
          "url": "https://github.com/unicode-org/icu/commit/d02b30fc3f665677a775950b550179a8b3b736b2"
        },
        "date": 1669916799766,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 285.94551458717183,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tushuhei@google.com",
            "name": "Shuhei Iitsuka",
            "username": "tushuhei"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "b6b7b045e9cef2c942efd267bb89c5a545017f0c",
          "message": "ICU-22100 Incorporate BudouX into ICU (C++)",
          "timestamp": "2022-12-02T10:11:06-08:00",
          "tree_id": "d416d38810869c31b830dee6a216b33a43dbc907",
          "url": "https://github.com/unicode-org/icu/commit/b6b7b045e9cef2c942efd267bb89c5a545017f0c"
        },
        "date": 1670004970957,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.72745780811766,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "amy@amyspark.me",
            "name": "L. E. Segovia",
            "username": "amyspark"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "e7aad77f31640666262e9ae1d098929920bd9a4b",
          "message": "ICU-22204 toolutil: Fix crash when trying to generate MinGW assembly",
          "timestamp": "2022-12-02T10:42:24-08:00",
          "tree_id": "ff8eff2e96f3b8edcb7eb3669fb9ca89f3991845",
          "url": "https://github.com/unicode-org/icu/commit/e7aad77f31640666262e9ae1d098929920bd9a4b"
        },
        "date": 1670007112448,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.3483386321191,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "amy@amyspark.me",
            "name": "L. E. Segovia",
            "username": "amyspark"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "ba8e4087ca1b086d0e83a685654ff69a0005f1a9",
          "message": "ICU-22203 Windows: enable C-code generation with overridden entry point",
          "timestamp": "2022-12-02T11:45:09-08:00",
          "tree_id": "386e221678647a5c982c13fcfc3a40ff1bcd84ef",
          "url": "https://github.com/unicode-org/icu/commit/ba8e4087ca1b086d0e83a685654ff69a0005f1a9"
        },
        "date": 1670010465455,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 256.54632948593854,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "7e083e34fbdfc12e4d12adaa593a8da21a8ef2c5",
          "message": "ICU-22220 BRS73RC Update version number to 73.0.1",
          "timestamp": "2022-12-04T21:14:41-08:00",
          "tree_id": "f188442a34cb1a848905317a6cda67d7971c9507",
          "url": "https://github.com/unicode-org/icu/commit/7e083e34fbdfc12e4d12adaa593a8da21a8ef2c5"
        },
        "date": 1670217671745,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.06071693492802,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "0cbf969cf75539b3423554595c8756e7923c2653",
          "message": "ICU-22193 fix some CI test failures",
          "timestamp": "2022-12-13T11:18:44-08:00",
          "tree_id": "6652128a523a2e696a3682d783591d3799ef231f",
          "url": "https://github.com/unicode-org/icu/commit/0cbf969cf75539b3423554595c8756e7923c2653"
        },
        "date": 1670959504044,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.69326280817862,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "57c9313eb7da760f27922fec7ce40f515232e7e7",
          "message": "ICU-22193 Use Ubuntu 20.04 for jobs failing in migration to 22.04",
          "timestamp": "2022-12-14T20:04:45Z",
          "tree_id": "e14c5268f7dcd929919029260ca7a00697f3723f",
          "url": "https://github.com/unicode-org/icu/commit/57c9313eb7da760f27922fec7ce40f515232e7e7"
        },
        "date": 1671048463351,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.27185794900544,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "45e98d4f67e6d88c29d70fb8f5322d549066f76d",
          "message": "ICU-12811 Build ICU4J using Maven\n\nSee #2251",
          "timestamp": "2022-12-15T00:48:31Z",
          "tree_id": "47f397196d5494ea204df32d02f60eaab06d4ef5",
          "url": "https://github.com/unicode-org/icu/commit/45e98d4f67e6d88c29d70fb8f5322d549066f76d"
        },
        "date": 1671065909703,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 238.6321418191084,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "2a6f06cb4c80474fe751401adbdf84c4c54414d0",
          "message": "ICU-22193 Make run-with-stubdata work with ubuntu-latest.\n\nIt remains unknown when and why this changed, but nowadays the required\ndata files are to be found in a subdirectory named \"build\".",
          "timestamp": "2022-12-16T10:06:32+09:00",
          "tree_id": "d3bb990e60097bd173df7b28d16e354f99beae33",
          "url": "https://github.com/unicode-org/icu/commit/2a6f06cb4c80474fe751401adbdf84c4c54414d0"
        },
        "date": 1671153122351,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.2055931716952,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "f6353aeedcfa4b5907f1037a9d924660ff8f4d87",
          "message": "ICU-12811 Make CI jobs for Maven run serially to avoid CI cache race condition",
          "timestamp": "2022-12-17T12:59:35-05:00",
          "tree_id": "0392e4c95b704e3f6885fc5bed37e19012ad8259",
          "url": "https://github.com/unicode-org/icu/commit/f6353aeedcfa4b5907f1037a9d924660ff8f4d87"
        },
        "date": 1671300292226,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 218.47236876604774,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "82115c060f14dc68601ccd66bcb2fa6e6918d926",
          "message": "ICU-22193 Make clang-release-build-and-test work with ubuntu-latest.\n\nContemporary implementations of the C++ standard library also use the\n@deprecated annotation in its header files and these then get included\nby the preprocessor when preprocessing the ICU header files, like this:\n\n  /// @deprecated Non-standard. Use `is_null_pointer` instead.\n\nIn order to work as expected, testtagsguards.sh must therefore be\nupdated to ignore @deprecated annotations unless they're for ICU.",
          "timestamp": "2022-12-19T10:56:01+09:00",
          "tree_id": "ad9a42e55c0d1d045b196e9d43b859fcf6e2d39d",
          "url": "https://github.com/unicode-org/icu/commit/82115c060f14dc68601ccd66bcb2fa6e6918d926"
        },
        "date": 1671415425702,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.6913613470112,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "49699333+dependabot[bot]@users.noreply.github.com",
            "name": "dependabot[bot]",
            "username": "dependabot[bot]"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "44480c4ba430eef36056a66867e5abad892c5e96",
          "message": "ICU-22193 Bump junit from 4.12 to 4.13.1 in /icu4j/maven-build\n\nBumps [junit](https://github.com/junit-team/junit4) from 4.12 to 4.13.1.\n- [Release notes](https://github.com/junit-team/junit4/releases)\n- [Changelog](https://github.com/junit-team/junit4/blob/main/doc/ReleaseNotes4.12.md)\n- [Commits](https://github.com/junit-team/junit4/compare/r4.12...r4.13.1)\n\n---\nupdated-dependencies:\n- dependency-name: junit:junit\n  dependency-type: direct:production\n...\n\nSigned-off-by: dependabot[bot] <support@github.com>",
          "timestamp": "2022-12-19T10:48:04-08:00",
          "tree_id": "1c007e81782e26919c8569d7c3a839bb535a3b0f",
          "url": "https://github.com/unicode-org/icu/commit/44480c4ba430eef36056a66867e5abad892c5e96"
        },
        "date": 1671475914502,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 256.24159301479386,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "allenwtsu@google.com",
            "name": "allenwtsu",
            "username": "allensu05"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "90caafbcd437e04147106d8e35a29b189977f0b7",
          "message": "ICU-22100 Incorporate BudouX into ICU (Java)\n\nSee #2214",
          "timestamp": "2022-12-20T14:27:04-08:00",
          "tree_id": "b0f1243db09796adc0ab6a1402cb26cea8d97484",
          "url": "https://github.com/unicode-org/icu/commit/90caafbcd437e04147106d8e35a29b189977f0b7"
        },
        "date": 1671575472595,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 229.02357154152213,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "dcac8ac4c1c450b291276e6c84b29f8b5844cf48",
          "message": "ICU-22233 Use separate Bazel cache keys per OS.",
          "timestamp": "2022-12-22T14:43:22+09:00",
          "tree_id": "1277c3ad5ffcd596586185975df17ac4dd2b2ede",
          "url": "https://github.com/unicode-org/icu/commit/dcac8ac4c1c450b291276e6c84b29f8b5844cf48"
        },
        "date": 1671688169381,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.96679440224764,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tkoeppe@google.com",
            "name": "Thomas Köppe",
            "username": "tkoeppe"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "0c6d7fc98d0bf6967d2eb952cc686771c2cac78b",
          "message": "ICU-22222 Add explicit instantiation declarations\n\nWe exclude these declarations from MSVC, where they don't work because\nthe don't interact well with the U_I18N_API import/export macros",
          "timestamp": "2023-01-07T00:03:38Z",
          "tree_id": "735d5cc385901dc54bbd88e8ab504e957dc72a50",
          "url": "https://github.com/unicode-org/icu/commit/0c6d7fc98d0bf6967d2eb952cc686771c2cac78b"
        },
        "date": 1673049950522,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.46795257774755,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "allenwtsu@google.com",
            "name": "allenwtsu",
            "username": "allensu05"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "80fb309c8a5f865767ef72f85ea1bf70c29e2b39",
          "message": "ICU-22100 Remove unicode blocks from Japanese ML phrase breaking\n\nSee #2278",
          "timestamp": "2023-01-09T17:38:51-08:00",
          "tree_id": "f43d7fa47d78eff1cfd536e76ebc55ca2905a0ce",
          "url": "https://github.com/unicode-org/icu/commit/80fb309c8a5f865767ef72f85ea1bf70c29e2b39"
        },
        "date": 1673315164651,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.1554192277203,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "8d411e9b6aba9d15577341663ca2a70dd806e5a7",
          "message": "ICU-22220 integrate CLDR release-43-m0 to ICU main for 73, update maven-build files",
          "timestamp": "2023-01-10T11:32:24-08:00",
          "tree_id": "4c8e7eed718809007e0c1ebcbff8394214003f45",
          "url": "https://github.com/unicode-org/icu/commit/8d411e9b6aba9d15577341663ca2a70dd806e5a7"
        },
        "date": 1673379304967,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.31861524505666,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "ad82a6693acdab6adde7499853bb9bb5befc6fc3",
          "message": "ICU-22220 integrate root exemplarCities chnages for CLDR release-43-m0 to ICU main",
          "timestamp": "2023-01-12T08:57:35-08:00",
          "tree_id": "e154680212a1754592fc958909b26f7ae8f91b4f",
          "url": "https://github.com/unicode-org/icu/commit/ad82a6693acdab6adde7499853bb9bb5befc6fc3"
        },
        "date": 1673542900504,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.93436682486748,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "89c1700424ee903718fad955c240017d6431d0eb",
          "message": "ICU-22093 Polish for SimpleNumberFormatter\n\nSee #2277",
          "timestamp": "2023-01-12T11:38:27-06:00",
          "tree_id": "283a2331ea8afbfeeeb7839b7168bd93a6c70077",
          "url": "https://github.com/unicode-org/icu/commit/89c1700424ee903718fad955c240017d6431d0eb"
        },
        "date": 1673545317315,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.7836003761383,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "2007e135f1e78711247b2f0ab440dec54307cfe9",
          "message": "ICU-12811 Add CI workflow to retain caches that are flaky/costly to init\n\nSee #2281",
          "timestamp": "2023-01-13T12:36:48-08:00",
          "tree_id": "2a21451602d4cf3110e614b1f01632ea5d99cabf",
          "url": "https://github.com/unicode-org/icu/commit/2007e135f1e78711247b2f0ab440dec54307cfe9"
        },
        "date": 1673642742249,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.97396723745973,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "e7530bd9ff2e7317710a1611f6d6de264143bf6a",
          "message": "ICU-12811 Fix CI cache retain workflow's cron schedule string",
          "timestamp": "2023-01-13T14:57:51-08:00",
          "tree_id": "8f682b0352a1e935f54d6bf6533a94d6ee722c88",
          "url": "https://github.com/unicode-org/icu/commit/e7530bd9ff2e7317710a1611f6d6de264143bf6a"
        },
        "date": 1673651718433,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.1734506839507,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "1b292fa92463e81881f0cf3d6f8da4d92d3a5ad3",
          "message": "ICU-22233 Fix CI cache name for Bazel build",
          "timestamp": "2023-01-17T10:50:47-08:00",
          "tree_id": "eb0733b35209e1bb1f35b9bb8d51ea47ec253cb3",
          "url": "https://github.com/unicode-org/icu/commit/1b292fa92463e81881f0cf3d6f8da4d92d3a5ad3"
        },
        "date": 1673981879020,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 219.6879514628264,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "d4ac09edbdc25feb06b3a011cbbb150b243d91f2",
          "message": "ICU-12811 Replace local-maven-repo with data jar contents\n\nSee #2275",
          "timestamp": "2023-01-17T11:09:29-08:00",
          "tree_id": "6c93956c5a721a7c04b0e8045b60c405e9cb0169",
          "url": "https://github.com/unicode-org/icu/commit/d4ac09edbdc25feb06b3a011cbbb150b243d91f2"
        },
        "date": 1673983036542,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 265.5081275987031,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "a7f4531bfaf4e7ca93fea518b5c1734cd6ffdc1a",
          "message": "ICU-12811 Fix localespi tests when run by Maven on Java 8\n\nSee #2283",
          "timestamp": "2023-01-17T13:17:29-08:00",
          "tree_id": "ab7208fb8048e6124c55b6e56967b3743e83b787",
          "url": "https://github.com/unicode-org/icu/commit/a7f4531bfaf4e7ca93fea518b5c1734cd6ffdc1a"
        },
        "date": 1673990559843,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 235.81420123683145,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "cd1b772cbfb54f7f995ddeeea59788d55d51a68f",
          "message": "ICU-22027 Add C++ Calendar API for Temporal\n\nAPI proposal\nhttps://docs.google.com/document/d/1UYriEzzExiLhi2RD3zjTsI5UQHv1dXaFqrct7yXNdCA/edit#heading=h.x9obor85vpx9\n\nDesign Doc\nhttps://docs.google.com/document/d/15ViyC9s0k3VEDwBmAkKxxz4IadZ6QrAIoETkdkF0cVA/\n\nICU-22027 Adjust API to remove the mention of M00L for now.",
          "timestamp": "2023-01-17T15:08:08-08:00",
          "tree_id": "2f12d4892565296d13ab321bd16ecabbf49e1693",
          "url": "https://github.com/unicode-org/icu/commit/cd1b772cbfb54f7f995ddeeea59788d55d51a68f"
        },
        "date": 1673997264542,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.5945736438461,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "da5b047dbedf4026dd95638296d25f539561c986",
          "message": "ICU-22242 Test to show incorrect calculation of 1890\n\nSee #2270",
          "timestamp": "2023-01-18T12:05:34-08:00",
          "tree_id": "25d17fe75737df672323a556d02261f1d97667e3",
          "url": "https://github.com/unicode-org/icu/commit/da5b047dbedf4026dd95638296d25f539561c986"
        },
        "date": 1674072790288,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 227.4027557761462,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "bb0e745e25c99cc57055caf45c81b95ef63b25d4",
          "message": "ICU-22058 make pointer argument in floorDivide optional\n\nCheck the third argument and not set if it is a nullptr",
          "timestamp": "2023-01-18T20:24:48-08:00",
          "tree_id": "e8313c3668e355649f80e591a4e4f7eae184d1f8",
          "url": "https://github.com/unicode-org/icu/commit/bb0e745e25c99cc57055caf45c81b95ef63b25d4"
        },
        "date": 1674102440509,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.3248819395532,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "de9cb9a133c3538030fd2d2275756e15e8320c4b",
          "message": "ICU-12725 Update u_isIDStart and u_isIDPart to TR31\n\nICU-12725 move to uprops.cpp\n\nICU-12725 change dependency\n\nICU-12725 Fix Java implementation",
          "timestamp": "2023-01-25T12:02:53-08:00",
          "tree_id": "979f490b1438a16993f1ef99b28304a5cf07b5fb",
          "url": "https://github.com/unicode-org/icu/commit/de9cb9a133c3538030fd2d2275756e15e8320c4b"
        },
        "date": 1674677360759,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.50916472572405,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "de0a28644b26e2a85ccb916480d338e6404ec3d6",
          "message": "ICU-22251 Move sprintf to snprintf.\n\nSee #2291",
          "timestamp": "2023-01-25T23:23:29-08:00",
          "tree_id": "f024d8521b725c1f346bb12f9c76b84e930456dc",
          "url": "https://github.com/unicode-org/icu/commit/de0a28644b26e2a85ccb916480d338e6404ec3d6"
        },
        "date": 1674717982667,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 299.9426457140658,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "76df897b77fd938abc29c9121dde794300a171e6",
          "message": "ICU-22226 Fix Calendar.getFirstDayOfWeek to honor -u-fw",
          "timestamp": "2023-01-31T00:26:30-08:00",
          "tree_id": "2b05b7fc6cbcaa271e487783d48b1a21413679ba",
          "url": "https://github.com/unicode-org/icu/commit/76df897b77fd938abc29c9121dde794300a171e6"
        },
        "date": 1675153918102,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.82919541718567,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "allenwtsu@google.com",
            "name": "allenwtsu",
            "username": "allensu05"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "0b3b83a80966f638fae1704a6a6042596af2a757",
          "message": "ICU-22100 Improve Japanese phrase breaking performance\n\nSee #2287",
          "timestamp": "2023-01-31T00:29:41-08:00",
          "tree_id": "aa9a9bdc146e2022b3285d78f1b6d0411cfff53c",
          "url": "https://github.com/unicode-org/icu/commit/0b3b83a80966f638fae1704a6a6042596af2a757"
        },
        "date": 1675154590600,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.71803798479823,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "a47717934aee8e9581aefa68a917f60a3348375c",
          "message": "ICU-22252 Suppress Calendar Consistency\n\nSee #2298",
          "timestamp": "2023-02-03T16:38:50+01:00",
          "tree_id": "392f6888a8313682ef77f357b1874f8bbc63990e",
          "url": "https://github.com/unicode-org/icu/commit/a47717934aee8e9581aefa68a917f60a3348375c"
        },
        "date": 1675439263032,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 269.16386353702,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "2e0d30cfcf43490d0d3a0026139256a7328a0183",
          "message": "ICU-21833 Replace NULL with nullptr in all C++ code.",
          "timestamp": "2023-02-03T20:20:38+01:00",
          "tree_id": "63ac5877ca5497b3d64ffd8ac4e7d89d80f68bc2",
          "url": "https://github.com/unicode-org/icu/commit/2e0d30cfcf43490d0d3a0026139256a7328a0183"
        },
        "date": 1675452379292,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.1975200248353,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "summersnow9403@gmail.com",
            "name": "HanatoK",
            "username": "HanatoK"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "fcd6e384bd283fc960f543ef9ff92ed775406671",
          "message": "ICU-22194 Change CharacterIterator.DONE to CharacterIterator::DONE in\ndoc\n\nIn C++, the enum inside a class should be accessed by \"::\" instead of\n\".\".",
          "timestamp": "2023-02-03T12:01:51-08:00",
          "tree_id": "b8c2b713545549e678ff146f32156a79933b05e5",
          "url": "https://github.com/unicode-org/icu/commit/fcd6e384bd283fc960f543ef9ff92ed775406671"
        },
        "date": 1675455017010,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 236.76685153094104,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "5560ee8870b41c94c0f89e363f31f57c1014de99",
          "message": "ICU-21833 Replace nullptr with 0 when assigning to UChar.\n\nThis bug was originally introduced by ICU-4844 which erroneously\nassigned NULL to UChar (which happens to work, even though it's\nconceptually wrong).",
          "timestamp": "2023-02-03T22:04:36+01:00",
          "tree_id": "65b67180cf3f4e4c30b5327ca64bc0d7c81524df",
          "url": "https://github.com/unicode-org/icu/commit/5560ee8870b41c94c0f89e363f31f57c1014de99"
        },
        "date": 1675458381894,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 242.31324427515213,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "allenwtsu@google.com",
            "name": "allenwtsu",
            "username": "allensu05"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "3f05361b4192d6d337c3dacc63a91f53c966da3e",
          "message": "ICU-22100 Modify ML model to improve Japanese phrase breaking performance",
          "timestamp": "2023-02-03T13:07:53-08:00",
          "tree_id": "cd9867e63b7e7efbccbbc7b9093e57b6664d2cfc",
          "url": "https://github.com/unicode-org/icu/commit/3f05361b4192d6d337c3dacc63a91f53c966da3e"
        },
        "date": 1675459356127,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 266.5903176466098,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "08f14db4c45f6cbcf5b6b5d45131c98b7b64b57c",
          "message": "ICU-22249 Fixed endless loop in ICUResourceBundle when you ask for a locale with a nonstandard parent and that locale\nis also the system default locale.",
          "timestamp": "2023-02-03T16:49:24-08:00",
          "tree_id": "8557245915c10c3a3c16f545d62f41e69b952160",
          "url": "https://github.com/unicode-org/icu/commit/08f14db4c45f6cbcf5b6b5d45131c98b7b64b57c"
        },
        "date": 1675472442621,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.04967858935024,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "2de88f9d9c07f7e693449f94858d96053222acea",
          "message": "ICU-21833 Replace UChar with char16_t in all C++ code.",
          "timestamp": "2023-02-06T19:27:44+01:00",
          "tree_id": "412b7cd21e471cc5d02327b90b8fda073771a4a7",
          "url": "https://github.com/unicode-org/icu/commit/2de88f9d9c07f7e693449f94858d96053222acea"
        },
        "date": 1675708413688,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.79210067691238,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "9fd2742dfa5d78f3a5d14c603c85ed3dcf88a197",
          "message": "ICU-21833 Replace UChar with char16_t in C++ code.",
          "timestamp": "2023-02-06T21:53:20+01:00",
          "tree_id": "691c225ac134d26e5a69d43c0a300f27b295c040",
          "url": "https://github.com/unicode-org/icu/commit/9fd2742dfa5d78f3a5d14c603c85ed3dcf88a197"
        },
        "date": 1675717205312,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.0574202665093,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "0264f411b8e0d7ede4bba157b42193fde5ebc150",
          "message": "ICU-22257 Add \"Test ICU4J with only little-endian ICU4C data\"\n\nAutomate \"Test ICU4J with only little-endian ICU4C data\" as stated in\nhttps://unicode-org.github.io/icu/processes/release/tasks/integration.html#test-icu4j-with-only-little-endian-icu4c-data\n\nUpdate .ci-builds/.azure-exhaustive-tests.yml\n\nCo-authored-by: Markus Scherer <markus.icu@gmail.com>",
          "timestamp": "2023-02-06T14:32:33-08:00",
          "tree_id": "ddbd22ca64745e12b860d426e7ace5b4f2d88d54",
          "url": "https://github.com/unicode-org/icu/commit/0264f411b8e0d7ede4bba157b42193fde5ebc150"
        },
        "date": 1675723066208,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 219.18160189311462,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "87fc840bf7750cee0884a133e2efb976c9714f30",
          "message": "ICU-22220 CLDR release-43-alpha0 (with SurveyTool data) to ICU main",
          "timestamp": "2023-02-06T14:46:14-08:00",
          "tree_id": "6440201cdfe62f73b439452e53765529e410775f",
          "url": "https://github.com/unicode-org/icu/commit/87fc840bf7750cee0884a133e2efb976c9714f30"
        },
        "date": 1675724614354,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.87680834184886,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "638acd0c389aa386acd62f61bc32808549fa4f20",
          "message": "ICU-21374 Add a CFI build bot for ICU4C\n\nAdd the github action bot to build with cfi\nAlso fix all the known issues which require the change from C style cast to\nstatic_cast inside the i18n and common directory while we are sure about\nthe object. and use\nC++ style dynamic_cast for base-to-derive cast in other code inside i18n\nand common and in test code or tool.\nChange to use const_cast for casting between const / non-const",
          "timestamp": "2023-02-06T15:47:14-08:00",
          "tree_id": "0b3dadee331155ccd574dabffa6b1587e9b378a2",
          "url": "https://github.com/unicode-org/icu/commit/638acd0c389aa386acd62f61bc32808549fa4f20"
        },
        "date": 1675727756974,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.6377257724639,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "39dfee39b88a09fcafb33992a5495c4117490644",
          "message": "ICU-22257 BRS doc: J with little-endian data automatic",
          "timestamp": "2023-02-06T17:52:40-08:00",
          "tree_id": "f7b750f20376f0be1ea10ec21548b44276a631e2",
          "url": "https://github.com/unicode-org/icu/commit/39dfee39b88a09fcafb33992a5495c4117490644"
        },
        "date": 1675735606891,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.07329724652814,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "3f093b602a2de85c95249a057ab0bc5528bbda82",
          "message": "ICU-22266 update OS version to 22.04 from 18.04\n\nUpdate configuration in main/.ci-builds from the deprecated 18.04\nto the newest 22.04\n\nSince vangrind has issue under 22.04 use 20.04 for vangrind for now.\n\nAlso use apt-get instead of apt since apt does not have a stable\ncommandline interface.",
          "timestamp": "2023-02-08T13:43:14-08:00",
          "tree_id": "cdf7cfee32d593132cafd9a0c17b0fe01060b715",
          "url": "https://github.com/unicode-org/icu/commit/3f093b602a2de85c95249a057ab0bc5528bbda82"
        },
        "date": 1675893255706,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 228.26682055312492,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "f1e02f149fcaffcc7cea1be21363f0cb2d59f4a9",
          "message": "ICU-22194 Update DateTime skeleton docs with link to symbols table",
          "timestamp": "2023-02-09T11:34:53-05:00",
          "tree_id": "7abdedb361d7651a4ceed1bef1f04fce741f7a9c",
          "url": "https://github.com/unicode-org/icu/commit/f1e02f149fcaffcc7cea1be21363f0cb2d59f4a9"
        },
        "date": 1675960898173,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 261.95308624783587,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "9f108554af3eea7739dd07598c8e681926dc9755",
          "message": "ICU-22270 icuexportdata: Add property and property value names/aliases",
          "timestamp": "2023-02-09T15:44:48-08:00",
          "tree_id": "b51aea8d52b7d05eb29fabf01da950694fda57d0",
          "url": "https://github.com/unicode-org/icu/commit/9f108554af3eea7739dd07598c8e681926dc9755"
        },
        "date": 1675987096223,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.22313633728615,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "288c4c7555915ce7b1fb675d94ddd495058fc039",
          "message": "ICU-22220 ICU 73 API promotions (promoting ICU 71 and earlier)",
          "timestamp": "2023-02-09T16:44:56-08:00",
          "tree_id": "e313f0c03ca432bb9e7db57ab537d0cc87d4dc7f",
          "url": "https://github.com/unicode-org/icu/commit/288c4c7555915ce7b1fb675d94ddd495058fc039"
        },
        "date": 1675990339555,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.69056878418965,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "ec2d4b346edab4b803a85464a4ff170bf28cb417",
          "message": "ICU-22251 Move sprintf to snprintf.\n\nSee #2291",
          "timestamp": "2023-02-09T17:38:32-08:00",
          "tree_id": "12cdd6a87c85ce8ed9bb454ea638ac0b1f3137b2",
          "url": "https://github.com/unicode-org/icu/commit/ec2d4b346edab4b803a85464a4ff170bf28cb417"
        },
        "date": 1675993260043,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.7261562043855,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "b20c97009c29a6c230b834a02dea93bc7c2c1e7b",
          "message": "ICU-22194 Add User Guide doc for MessageFormat 2.0 tech preview impl\n\nSee #2313",
          "timestamp": "2023-02-10T21:17:59-05:00",
          "tree_id": "beea462ce3cb4c5c6e8d5e465956091c0c65bedc",
          "url": "https://github.com/unicode-org/icu/commit/b20c97009c29a6c230b834a02dea93bc7c2c1e7b"
        },
        "date": 1676082200589,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.8083260757244,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "32bc47eefe0c74ea4b7479595ae804673428491f",
          "message": "ICU-22257 Revert PR 2302\n\nSince the same work has been done in PR 1884 before.",
          "timestamp": "2023-02-13T12:37:11-08:00",
          "tree_id": "6b0fcd8acb42bf22e8ceaf2bf810cc1048965a11",
          "url": "https://github.com/unicode-org/icu/commit/32bc47eefe0c74ea4b7479595ae804673428491f"
        },
        "date": 1676321139507,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.48152502253876,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "47b321f1fa6a5bafcc69a8bb01215773a577fedc",
          "message": "ICU-22277 correct collation error return of uninitialized length value while previous code return error\n\nSee #2320",
          "timestamp": "2023-02-13T14:24:33-08:00",
          "tree_id": "97737c89c28e20f49abfba94a136e7b313ce2dc6",
          "url": "https://github.com/unicode-org/icu/commit/47b321f1fa6a5bafcc69a8bb01215773a577fedc"
        },
        "date": 1676327624435,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.34614894238274,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "461bcef128dabde84e796700c1634128dbd74d1b",
          "message": "ICU-22194 Fix javadoc error (self-closing element not allowed)",
          "timestamp": "2023-02-13T16:30:28-08:00",
          "tree_id": "21aa5ca2b55432a11f067d6f5e6b2c6dcd7de029",
          "url": "https://github.com/unicode-org/icu/commit/461bcef128dabde84e796700c1634128dbd74d1b"
        },
        "date": 1676335147541,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.930410677189,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "c91c2fbdc5c20fbbb62e4aeca24e887a074b2c6b",
          "message": "ICU-22257 BRS doc: J with little-endian data automatic since ICU 70",
          "timestamp": "2023-02-13T17:12:04-08:00",
          "tree_id": "218d2e4d87b573bb862b9bed78e6b77905949e88",
          "url": "https://github.com/unicode-org/icu/commit/c91c2fbdc5c20fbbb62e4aeca24e887a074b2c6b"
        },
        "date": 1676337622853,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.95287415681395,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "331172f0a34c5447541b115122251ea3fb1426d7",
          "message": "ICU-22262 Automate environment test\n\nSee #2309",
          "timestamp": "2023-02-15T15:24:19-08:00",
          "tree_id": "b7b5e63e6193e6eaaa6858f36acf341829c51aed",
          "url": "https://github.com/unicode-org/icu/commit/331172f0a34c5447541b115122251ea3fb1426d7"
        },
        "date": 1676504221155,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 266.82792469724643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "cb87c0893bab7b68eab6d129a28e0673149c1665",
          "message": "ICU-22269 Parallelize uconfig* tests\n\n1. Shorten job name uconfig_variation-check-unit-tests to uconfig-unit-tests\n2. Shorten job name uconfig_variation-check-all-header-tests to uconfig-header-tests\n3. use 11 jobs to run each of them in parallel to reduce the ~1hrs run to about 6-8 mins\n\nCo-authored-by: Markus Scherer <markus.icu@gmail.com>",
          "timestamp": "2023-02-15T16:52:14-08:00",
          "tree_id": "033ec82ca5a92d88b78c4373bec6578d1d192813",
          "url": "https://github.com/unicode-org/icu/commit/cb87c0893bab7b68eab6d129a28e0673149c1665"
        },
        "date": 1676509405451,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.9315344875986,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "a4c6b2a3595853dac07bb39c81a812bf1eb7d4f5",
          "message": "ICU-22262 Document environment test automation",
          "timestamp": "2023-02-15T16:53:02-08:00",
          "tree_id": "c8e121ab5c02e5fd51a14fa5744e35c3689cb22b",
          "url": "https://github.com/unicode-org/icu/commit/a4c6b2a3595853dac07bb39c81a812bf1eb7d4f5"
        },
        "date": 1676510293189,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 228.6374990007182,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "bd065d4704bb212dfe3414c9d1ef105fb2a4df75",
          "message": "ICU-22220 Automate BRS tasks\n\nSee #2318",
          "timestamp": "2023-02-15T21:18:58-05:00",
          "tree_id": "811adba53a48b4f444702294bf9c98c462633c37",
          "url": "https://github.com/unicode-org/icu/commit/bd065d4704bb212dfe3414c9d1ef105fb2a4df75"
        },
        "date": 1676514219946,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 261.9045027732166,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "d469e628f391d1d464e980eb257c595fdb5a2a3a",
          "message": "ICU-22220 CLDR release-43-alpha1 to ICU main",
          "timestamp": "2023-02-21T11:39:48-08:00",
          "tree_id": "139994bd1a780ac4496fcb075f619aacce918cee",
          "url": "https://github.com/unicode-org/icu/commit/d469e628f391d1d464e980eb257c595fdb5a2a3a"
        },
        "date": 1677010163700,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 268.5342091736113,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "015222105a614dbf09e187a06cbc5d97839fe287",
          "message": "ICU-12811 Refactor test-framework to not depend on core\n\nFixing the maven-build projects",
          "timestamp": "2023-02-21T14:16:19-08:00",
          "tree_id": "8d1dbbaf15df0b4821f1dcdc349c7417738c54fd",
          "url": "https://github.com/unicode-org/icu/commit/015222105a614dbf09e187a06cbc5d97839fe287"
        },
        "date": 1677018061923,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.7798302230032,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "a3cbe809091807b406cf526635cb866270a20da2",
          "message": "ICU-21833 Replace U_OVERRIDE with override everywhere.",
          "timestamp": "2023-02-22T18:28:07+01:00",
          "tree_id": "e908dabb0e71b134ca3d429594088298ca114087",
          "url": "https://github.com/unicode-org/icu/commit/a3cbe809091807b406cf526635cb866270a20da2"
        },
        "date": 1677087127553,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.36790739487807,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "f924741bf2d7a2810a47ec44dd24e66f4979f03e",
          "message": "ICU-22220 BRS 73rc update urename.h pass 1",
          "timestamp": "2023-02-22T10:30:25-08:00",
          "tree_id": "063ed2669cbe165196f736e33b56fe82c0a90fc0",
          "url": "https://github.com/unicode-org/icu/commit/f924741bf2d7a2810a47ec44dd24e66f4979f03e"
        },
        "date": 1677091181637,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.70102100658409,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "c3b8ffa11e4509d9c7993c04933346efc944a9a5",
          "message": "ICU-22291 doc automate \"Test samples (C) on Linux\"\n\nJust realize the task is already automated in \r\n.github/workflows/icu_ci.yml\r\nas \"icu4c-test-samples:\" but we should document that.",
          "timestamp": "2023-02-22T11:41:18-08:00",
          "tree_id": "639e18fc313bed872e640e7958d15d598d3484d2",
          "url": "https://github.com/unicode-org/icu/commit/c3b8ffa11e4509d9c7993c04933346efc944a9a5"
        },
        "date": 1677095140407,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.5337038533232,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "ea2cb8549efa2d60d17ca59517606626cc1626e8",
          "message": "ICU-21833 Replace U_FINAL with final everywhere.",
          "timestamp": "2023-02-22T22:39:41+01:00",
          "tree_id": "fd419258901e8fcb56d02745a36fa887f1adeb89",
          "url": "https://github.com/unicode-org/icu/commit/ea2cb8549efa2d60d17ca59517606626cc1626e8"
        },
        "date": 1677102717236,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 252.93907850652312,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "d86b1cebe192004759b6c875b0f831b97ccdae34",
          "message": "ICU-22220 update root collation from CLDR 43",
          "timestamp": "2023-02-22T17:13:13-08:00",
          "tree_id": "a8524f42361f61a8d752effc0f6ac4b89edecd2c",
          "url": "https://github.com/unicode-org/icu/commit/d86b1cebe192004759b6c875b0f831b97ccdae34"
        },
        "date": 1677115549728,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.16609637520662,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "0f4e084208f8486aef6200be91e59a24848ea552",
          "message": "ICU-22270 Add support for General_Category_Mask in icuexport",
          "timestamp": "2023-02-24T11:42:13-08:00",
          "tree_id": "ad18d73322c27efe8e6227aa829db90a205b6f7f",
          "url": "https://github.com/unicode-org/icu/commit/0f4e084208f8486aef6200be91e59a24848ea552"
        },
        "date": 1677268332892,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.95763954001916,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "d3c94cc062ef83e05b6b1dc1dd699c543efff39a",
          "message": "ICU-22270 Use hex for mask properties",
          "timestamp": "2023-02-24T14:06:31-08:00",
          "tree_id": "976d0d3e6f2e9953fd64a711eb20e7a10931b9f4",
          "url": "https://github.com/unicode-org/icu/commit/d3c94cc062ef83e05b6b1dc1dd699c543efff39a"
        },
        "date": 1677276859850,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 220.41176911951922,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "ea2711d9b0ad236d41f78d120ecb39c3adc4bed5",
          "message": "ICU-22270 Use hex for mask properties",
          "timestamp": "2023-02-24T14:10:00-08:00",
          "tree_id": "022bfe333fbcb7031efd16cf25902ca3be191309",
          "url": "https://github.com/unicode-org/icu/commit/ea2711d9b0ad236d41f78d120ecb39c3adc4bed5"
        },
        "date": 1677277470763,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 240.88007336663316,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "rob_dereycke@hotmail.com",
            "name": "Rob De Reycke",
            "username": "Robbos"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "e8bc04d5dfd4a463608ccbb47efca7de222d66a5",
          "message": "ICU-21386 uprv_tzname() should find the correct Olson ID when /etc/localtime is a \"double\" link\n\nSee #2323",
          "timestamp": "2023-02-25T16:48:43-05:00",
          "tree_id": "cf677fdd3ad52417f47b3117537a40b9cfca574c",
          "url": "https://github.com/unicode-org/icu/commit/e8bc04d5dfd4a463608ccbb47efca7de222d66a5"
        },
        "date": 1677362362560,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 254.39050014521624,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "18f6a3a6e242dce81486382420c9e3400c231660",
          "message": "ICU-22220 CLDR release-43-alpha2 to ICU main",
          "timestamp": "2023-02-27T11:09:02-08:00",
          "tree_id": "99e8d79097963a625bfbeccd7cdccc4895e03f20",
          "url": "https://github.com/unicode-org/icu/commit/18f6a3a6e242dce81486382420c9e3400c231660"
        },
        "date": 1677525648583,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.32610019932474,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "80414a247b652f0e1215adef2799da689f162533",
          "message": "ICU-22224 Enable UBSAN and fix breakage\n\nSee #2324",
          "timestamp": "2023-02-27T17:31:49-08:00",
          "tree_id": "5a78919db64968a78c2cb0c33cf8c48583906e72",
          "url": "https://github.com/unicode-org/icu/commit/80414a247b652f0e1215adef2799da689f162533"
        },
        "date": 1677548445031,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.56644568273757,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "d8e80fea88fe5b48d305444e19c07ddaee4952eb",
          "message": "ICU-21833 remove pre-C++11 code; U_SIZEOF_UCHAR=2",
          "timestamp": "2023-03-01T15:23:34-08:00",
          "tree_id": "e5f398604863be223e30b7161b88a49d44a183b0",
          "url": "https://github.com/unicode-org/icu/commit/d8e80fea88fe5b48d305444e19c07ddaee4952eb"
        },
        "date": 1677713519051,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 236.52119940585982,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "28643799377ecf654564f6f31854b02788cebe33",
          "message": "ICU-21833 replace U_NOEXCEPT with C++11 standard noexcept",
          "timestamp": "2023-03-01T15:24:34-08:00",
          "tree_id": "270c042f0a59e17ed55a92489897e2ee8f42f78e",
          "url": "https://github.com/unicode-org/icu/commit/28643799377ecf654564f6f31854b02788cebe33"
        },
        "date": 1677714017231,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 262.02302915634516,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "79ab90b5f9373935b8ae1308cf1148fdc1a04d7b",
          "message": "ICU-6065 UnicodeSet::closeOver(simple case folding)\n\nSee #2322",
          "timestamp": "2023-03-02T08:12:57-08:00",
          "tree_id": "0461660238861886ae746869c846c22750a92157",
          "url": "https://github.com/unicode-org/icu/commit/79ab90b5f9373935b8ae1308cf1148fdc1a04d7b"
        },
        "date": 1677773909803,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 217.22201975394472,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "b6dcc95d3c7c370ed993f7e2b7972adc48364293",
          "message": "ICU-21833 remove redundant void parameter lists\n\nSee #2351",
          "timestamp": "2023-03-02T09:31:57-08:00",
          "tree_id": "7ee8d0e0daf0d493fb83f78e9ae7c7212991c124",
          "url": "https://github.com/unicode-org/icu/commit/b6dcc95d3c7c370ed993f7e2b7972adc48364293"
        },
        "date": 1677778779853,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 219.4997570840484,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "797a01ee2baec8f97f747016092142cad58f241d",
          "message": "ICU-22287 Move PersonName/PersonNameFormatter API from Tech Preview to @draft",
          "timestamp": "2023-03-02T11:22:09-08:00",
          "tree_id": "8612a76645597566da79bcf8934b1fc50f7ca308",
          "url": "https://github.com/unicode-org/icu/commit/797a01ee2baec8f97f747016092142cad58f241d"
        },
        "date": 1677785530806,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.49699773606463,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "461ec392a5b0779434d8e8e4751216af547ee08e",
          "message": "ICU-22220 ICU4J API change report",
          "timestamp": "2023-03-03T19:47:31Z",
          "tree_id": "9df1568e5bee95e936fef335c1dacabc5740f014",
          "url": "https://github.com/unicode-org/icu/commit/461ec392a5b0779434d8e8e4751216af547ee08e"
        },
        "date": 1677873403056,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.5338626647783,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "5c07ee700b0e5a122e70d948a05edc6a0f1360fe",
          "message": "ICU-22220 ICU4C APIChangeReport for ICU73\n\nSee #2347",
          "timestamp": "2023-03-04T02:17:48Z",
          "tree_id": "ddb8d5686dfff87ca047e1b546b8df5a3c6fb1f9",
          "url": "https://github.com/unicode-org/icu/commit/5c07ee700b0e5a122e70d948a05edc6a0f1360fe"
        },
        "date": 1677896924897,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 258.9084888189833,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "bca85d4641978596cbefb0269ebd7f9834345d67",
          "message": "ICU-22220 Update ICU4J API status",
          "timestamp": "2023-03-04T02:18:24Z",
          "tree_id": "9cce2afd94c02304e7f988156cd28466dbfec93e",
          "url": "https://github.com/unicode-org/icu/commit/bca85d4641978596cbefb0269ebd7f9834345d67"
        },
        "date": 1677897663027,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.28662205414685,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "3748ef8f8adca873fbd2081f0d0435020807ce46",
          "message": "ICU-22220 adjust #ifndef U_HIDE_DRAFT_API for virtual methods, fix conditionalized enums",
          "timestamp": "2023-03-06T11:01:50-08:00",
          "tree_id": "7334f7b2571c2ffbf95ae22ab0372614169df9f2",
          "url": "https://github.com/unicode-org/icu/commit/3748ef8f8adca873fbd2081f0d0435020807ce46"
        },
        "date": 1678129978647,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.27386649969404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "2b714406eb5f163620a76b0f298b74b00c6be458",
          "message": "ICU-22220 Add a step to instructions and fix an obsolete path.\n\nSee #2348",
          "timestamp": "2023-03-06T19:29:02Z",
          "tree_id": "ee6bd428c9c3bde8260557987e0fd0382a21a7af",
          "url": "https://github.com/unicode-org/icu/commit/2b714406eb5f163620a76b0f298b74b00c6be458"
        },
        "date": 1678131122184,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.2939855821269,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "be6428690dc9b2e0e4a622691eb4c1101647cd2d",
          "message": "ICU-22270 expose uproperty values in icuexportdata",
          "timestamp": "2023-03-06T20:13:55-05:00",
          "tree_id": "d41ef0ddbefd8d1546bc80da0c12dce51dfdbe08",
          "url": "https://github.com/unicode-org/icu/commit/be6428690dc9b2e0e4a622691eb4c1101647cd2d"
        },
        "date": 1678152182942,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.75173742923292,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "6046af063ddd7ed9cbab601a3c6304ad9070545d",
          "message": "ICU-22270 expose uproperty values in icuexportdata",
          "timestamp": "2023-03-06T20:14:27-05:00",
          "tree_id": "2c4fe8d1d04e3e6a00de50dadaee671e3593097c",
          "url": "https://github.com/unicode-org/icu/commit/6046af063ddd7ed9cbab601a3c6304ad9070545d"
        },
        "date": 1678152932176,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 256.19472230612143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "9e16711b54d055cad10f7dc59f19d124ca6618be",
          "message": "ICU-22220 BRS73 Updating the Unicode data files and software license",
          "timestamp": "2023-03-06T22:31:39-05:00",
          "tree_id": "b445950bcb8afaecd9204e706cc9e6440566de97",
          "url": "https://github.com/unicode-org/icu/commit/9e16711b54d055cad10f7dc59f19d124ca6618be"
        },
        "date": 1678160553240,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 248.3467861967589,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "e5854c84a11036d2dcc8591156d7b7adf9e77c1e",
          "message": "ICU-22265 Update PersonNameFormatter and its associated classes so that the behavior matches that of the\nPersonNameFormatter in CLDR.  Added a new test that tests the ICU PersonNameFormatter against a comprehensive\nset of test results from the CLDR PersonNameFormatter.",
          "timestamp": "2023-03-08T13:56:17-08:00",
          "tree_id": "a9eeeade485dd6d785922569acf3675497ebcf57",
          "url": "https://github.com/unicode-org/icu/commit/e5854c84a11036d2dcc8591156d7b7adf9e77c1e"
        },
        "date": 1678313163766,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.8867922561546,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "57e4b0cd17f2fd29dcb8e98628c8d4f7395c9c77",
          "message": "ICU-22299 deprecate redundant UnicodeSet.CASE",
          "timestamp": "2023-03-09T08:40:08-08:00",
          "tree_id": "87751c12e28dd20e1772e736e4f28db8ba2bd105",
          "url": "https://github.com/unicode-org/icu/commit/57e4b0cd17f2fd29dcb8e98628c8d4f7395c9c77"
        },
        "date": 1678380709993,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.67540654867182,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "a36619060c382d759838417a6488f537ff964878",
          "message": "ICU-22279 Fix invalid new Locale in testing",
          "timestamp": "2023-03-09T22:19:44-08:00",
          "tree_id": "548869efed4bd060cc3696eb4fe729bdc050e335",
          "url": "https://github.com/unicode-org/icu/commit/a36619060c382d759838417a6488f537ff964878"
        },
        "date": 1678429494704,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 255.26900074935412,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shenlebantongying@gmail.com",
            "name": "shenleban tongying",
            "username": "shenlebantongying"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "ed011f2e3158c1205ec4517ead92a880fdc80f45",
          "message": "ICU-22247 remove historical mention of old versions",
          "timestamp": "2023-03-13T14:05:21-07:00",
          "tree_id": "32c116556e9c368e31eb75cb804ebc9c74642a1c",
          "url": "https://github.com/unicode-org/icu/commit/ed011f2e3158c1205ec4517ead92a880fdc80f45"
        },
        "date": 1678742121342,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.4924067392868,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "64b4cde6636b726c91db2a46103247b1129e2ea5",
          "message": "ICU-22256 Add helper code to dump Bidi_Mirroring_Glyph data to file\n\nCo-authored-by: Markus Scherer <markus.icu@gmail.com>",
          "timestamp": "2023-03-13T18:59:19-04:00",
          "tree_id": "a6f1843e9e2ea2231abebdad1ccaf98577cb86e7",
          "url": "https://github.com/unicode-org/icu/commit/64b4cde6636b726c91db2a46103247b1129e2ea5"
        },
        "date": 1678748904847,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.557694722849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "2a9d0ccdb2e65c05eb828c62a751efbd39761b39",
          "message": "ICU-22283 Add additional ERoundingMode variants\n\nSee #2329",
          "timestamp": "2023-03-14T00:51:42-07:00",
          "tree_id": "69a18752b20204c428f7db6a2e67879ba13e8b14",
          "url": "https://github.com/unicode-org/icu/commit/2a9d0ccdb2e65c05eb828c62a751efbd39761b39"
        },
        "date": 1678780613775,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 272.2751133751631,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "2c20fa45fb379a7ac5cdfb498f10294494f23d94",
          "message": "ICU-22220 CLDR release-43-beta0 to ICU main",
          "timestamp": "2023-03-14T09:53:14-07:00",
          "tree_id": "81e123266d4d55b021612b512339c11739220a69",
          "url": "https://github.com/unicode-org/icu/commit/2c20fa45fb379a7ac5cdfb498f10294494f23d94"
        },
        "date": 1678812984893,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 260.0138371924397,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "b399c67d5af748662275e1fa1d26d123caad3ed0",
          "message": "ICU-12811 Fix cldrUtils (add ElapsedTime and UnicodeMap*)",
          "timestamp": "2023-03-14T12:39:13-07:00",
          "tree_id": "433d13691d197b9a0d9440d74d9dad8a2c2641f4",
          "url": "https://github.com/unicode-org/icu/commit/b399c67d5af748662275e1fa1d26d123caad3ed0"
        },
        "date": 1678823384229,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.00796701655668,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "97510de5d428edc7f866fac9bb11d73d91ec714d",
          "message": "ICU-22280 Replace markdown in javadoc with proper tags",
          "timestamp": "2023-03-14T12:48:58-07:00",
          "tree_id": "137506b34a093e7f6d85cfe618c9dd76e0d6f49f",
          "url": "https://github.com/unicode-org/icu/commit/97510de5d428edc7f866fac9bb11d73d91ec714d"
        },
        "date": 1678824559307,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 253.29541120458586,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "2d9fa3fa9971c095b92f375dd0fe0a398d1dc6a8",
          "message": "ICU-22285 omit the gb2312 & big5han collation tailorings by default",
          "timestamp": "2023-03-14T15:20:03-07:00",
          "tree_id": "557e489f98737e764ca5acf98fca003cb63d88a0",
          "url": "https://github.com/unicode-org/icu/commit/2d9fa3fa9971c095b92f375dd0fe0a398d1dc6a8"
        },
        "date": 1678832749324,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 261.9826947657207,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ayzhao@google.com",
            "name": "Alan Zhao",
            "username": "alanzhao1"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "7ed7d42f58bc6ba24324e6be14db3e6a60e46355",
          "message": "ICU-22298 Include <utility> in measunit.h\n\nmeasunit.h uses std::pair, std::make_pair, and std::move, all of which\nare declared in the <utility> header. This still compiles because many\nimplementations of the C++ standard library have <utility> as a\ntransitive dependency of other C++ standard library headers; however,\nthese transitive includes are not guaranteed to exist and will not exist\nin some contexts (e.g. building against LLVM's libc++ with -fmodules).",
          "timestamp": "2023-03-14T17:08:53-07:00",
          "tree_id": "9b9b458f20bb13b1646353dffe3fb46ed298f6aa",
          "url": "https://github.com/unicode-org/icu/commit/7ed7d42f58bc6ba24324e6be14db3e6a60e46355"
        },
        "date": 1678839780830,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.73950234978173,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "bugagashenkj@gmail.com",
            "name": "Ivan Tymoshenko",
            "username": "ivan-tymoshenko"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "2c46fb7f610fef5e4d0a32881d8dc46d08265123",
          "message": "ICU-22286 Speed up substring equality comparison",
          "timestamp": "2023-03-15T11:10:48-07:00",
          "tree_id": "71649bae687245b52542f0a90999bb2fd9aaf461",
          "url": "https://github.com/unicode-org/icu/commit/2c46fb7f610fef5e4d0a32881d8dc46d08265123"
        },
        "date": 1678904085046,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.5704656658659,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "amy@amyspark.me",
            "name": "L. E. Segovia",
            "username": "amyspark"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "167f8855a2dfbe854a86f25f1dca80d295d3dbb7",
          "message": "ICU-22210 toolutil: Add NASM generator",
          "timestamp": "2023-03-15T16:42:09-07:00",
          "tree_id": "17def8c6c864c187c6b7cd1155adea94ed05a70e",
          "url": "https://github.com/unicode-org/icu/commit/167f8855a2dfbe854a86f25f1dca80d295d3dbb7"
        },
        "date": 1678924017246,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.44223443057035,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "amy@amyspark.me",
            "name": "L. E. Segovia",
            "username": "amyspark"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "62375dca3aad27e394312af3e8b785336b8a8f94",
          "message": "ICU-22205 toolutil: Fix MASM generation for x86-64 and ARM64",
          "timestamp": "2023-03-15T16:48:54-07:00",
          "tree_id": "a20423699df056bc33174102d5aa6362f6c9eeaf",
          "url": "https://github.com/unicode-org/icu/commit/62375dca3aad27e394312af3e8b785336b8a8f94"
        },
        "date": 1678925430467,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 231.03873403315177,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "06506023c5dc61886cf97c0f7aa2a8658a479d7b",
          "message": "ICU-22194 Add CI job to generate Github Pages using Github Actions",
          "timestamp": "2023-03-15T20:00:06-04:00",
          "tree_id": "04340890576db0a0a6b45aad1e4ed4063be43e3e",
          "url": "https://github.com/unicode-org/icu/commit/06506023c5dc61886cf97c0f7aa2a8658a479d7b"
        },
        "date": 1678926552656,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 260.6378680196816,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "3db74e8ae7375bb48361ea625bb0f82c465d35d3",
          "message": "ICU-22220 CLDR release-43-beta2 to ICU main",
          "timestamp": "2023-03-15T20:52:34-07:00",
          "tree_id": "79487b3791c8b00b19164aaded6453b464d6bfdd",
          "url": "https://github.com/unicode-org/icu/commit/3db74e8ae7375bb48361ea625bb0f82c465d35d3"
        },
        "date": 1678939401088,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.9949088781058,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "3b030512ff836522d0a294b6206dcf8b50eab725",
          "message": "ICU-22220 Update ICU4J samples instructions\n\nSee #2360",
          "timestamp": "2023-03-16T08:39:14-07:00",
          "tree_id": "a6d70cb26d691722b57927afbd13c81dcabc3c6f",
          "url": "https://github.com/unicode-org/icu/commit/3b030512ff836522d0a294b6206dcf8b50eab725"
        },
        "date": 1678982114927,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.57110978186435,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "0684f8644ca37153e8dbb25940d82bc14078e712",
          "message": "ICU-22285 how to include gb2312 & big5han collation tailorings",
          "timestamp": "2023-03-16T14:14:38-07:00",
          "tree_id": "4ae2a89f2eeb1a36b8a41a34f22fba0f6a79668f",
          "url": "https://github.com/unicode-org/icu/commit/0684f8644ca37153e8dbb25940d82bc14078e712"
        },
        "date": 1679001805484,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.4499303392782,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "d73b4417e196825f87e7440a1f74e318a8ca19c2",
          "message": "ICU-22186 Add unit tests for Croatia currency change from HRK to EUR",
          "timestamp": "2023-03-16T14:49:42-07:00",
          "tree_id": "ada4dccda4089860f4438a90bd5b89441cfd17d9",
          "url": "https://github.com/unicode-org/icu/commit/d73b4417e196825f87e7440a1f74e318a8ca19c2"
        },
        "date": 1679004150154,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.24485516348594,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "cdeae44c6bf33fe147c97161695f70778ab4c380",
          "message": "ICU-10297 Add tests showing that display name bug in ticket no longer exists",
          "timestamp": "2023-03-16T15:31:12-07:00",
          "tree_id": "2014f172234f61a4906ae661652ac44fa65fedae",
          "url": "https://github.com/unicode-org/icu/commit/cdeae44c6bf33fe147c97161695f70778ab4c380"
        },
        "date": 1679006635446,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 261.63697008721755,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "3d31e7263322588a9d004042305577c8171d9629",
          "message": "ICU-22307 Fix crash inside TimeZone\n\nReturn while the status fail without checking and cause crash",
          "timestamp": "2023-03-16T17:05:02-07:00",
          "tree_id": "95d6f04c31f8ef34a527bfade5adc575f082df94",
          "url": "https://github.com/unicode-org/icu/commit/3d31e7263322588a9d004042305577c8171d9629"
        },
        "date": 1679012139148,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 242.5543639510645,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "7a670998b0cbb217c30fae74b69c0b001cb6071e",
          "message": "ICU-22220 BRS73RC Update version number and regenerate configure v73.1\n\nSee #2375",
          "timestamp": "2023-03-17T09:48:06+05:30",
          "tree_id": "c6bde867bb9d17171e8602b7f9b351af157e2ef9",
          "url": "https://github.com/unicode-org/icu/commit/7a670998b0cbb217c30fae74b69c0b001cb6071e"
        },
        "date": 1679027141879,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.37870917389245,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "34eaffc66899da1ff5c4773c898bc4e27a8f8007",
          "message": "ICU-22308 stubdata empty pointerTOC alignas(16)",
          "timestamp": "2023-03-17T08:11:26-07:00",
          "tree_id": "92bf62de979f4b370217ee22e0e777ea7d18f29a",
          "url": "https://github.com/unicode-org/icu/commit/34eaffc66899da1ff5c4773c898bc4e27a8f8007"
        },
        "date": 1679066588182,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 239.2434497150437,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "dd1d4de29b3083a548d1b7a9b2f6f8cb86cca87a",
          "message": "ICU-21653 utext_current32 should handle access callback that changes chunk size",
          "timestamp": "2023-03-17T08:45:00-07:00",
          "tree_id": "ad91c9531eb9ab0eef75282b1ab1b4cd86e72c24",
          "url": "https://github.com/unicode-org/icu/commit/dd1d4de29b3083a548d1b7a9b2f6f8cb86cca87a"
        },
        "date": 1679068349509,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 253.53317810006723,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "74641a344e231c01570ed99025ef4319a2186be6",
          "message": "ICU-12725 adjust API docs for new impl != JDK",
          "timestamp": "2023-03-17T08:48:42-07:00",
          "tree_id": "be6d51e4869ff2f00298db834c887868e9b47b07",
          "url": "https://github.com/unicode-org/icu/commit/74641a344e231c01570ed99025ef4319a2186be6"
        },
        "date": 1679069317496,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 273.20084261696417,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "70ed5b4702fc4e1526478260a3674855df5ea134",
          "message": "ICU-21940 DateFormatSymbols::assignArray should handle null srcArray",
          "timestamp": "2023-03-17T08:52:19-07:00",
          "tree_id": "d98fea6e0f7f9944a79314bd513a7a042af1fc18",
          "url": "https://github.com/unicode-org/icu/commit/70ed5b4702fc4e1526478260a3674855df5ea134"
        },
        "date": 1679070299141,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.21864613214328,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pandeyrah@microsoft.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "3076874c3231047f55c561e1dd797137e764efea",
          "message": "ICU-22301 Add azure CI tests to be run post merge\n\nSee #2363",
          "timestamp": "2023-03-17T08:57:01-07:00",
          "tree_id": "e08301f68d681be212cb45ee1fdc186741b2cdcc",
          "url": "https://github.com/unicode-org/icu/commit/3076874c3231047f55c561e1dd797137e764efea"
        },
        "date": 1679070757137,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 268.1021313934701,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "kirill@korins.ky",
            "name": "Kirill A. Korinsky",
            "username": "catap"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "f2459c16dccd26e0cc07580e43e06e852dfc3104",
          "message": "ICU-22211 macOS PPC should use `.p2align 4` instead `.balign 16`\n\nAn attempt to build ICU on old macOS with PowerPC leads to an issue:\n```\n./out/tmp/icudt72b_dat.S:7:Unknown pseudo-op: .balign\n./out/tmp/icudt72b_dat.S:7:Rest of line ignored. 1st junk character valued 49 (1).\n```\n\nWhy? Because `as` is too old.\n\nAnyway, switch back to `.p2align` fix a build and allows to pass all tests.\n\nSee: https://trac.macports.org/ticket/66258\n\nSigned-off-by: Kirill A. Korinsky <kirill@korins.ky>",
          "timestamp": "2023-03-17T09:10:21-07:00",
          "tree_id": "f494b28370b81547138598de04ce1cf627d5f5a1",
          "url": "https://github.com/unicode-org/icu/commit/f2459c16dccd26e0cc07580e43e06e852dfc3104"
        },
        "date": 1679072054136,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 265.78033017280313,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "21581f4ec59b52338065866960dc2b155a428700",
          "message": "ICU-22220 ICU 73 API Changes 4J & 4C",
          "timestamp": "2023-03-17T11:56:41-07:00",
          "tree_id": "a12cd998a82a75cd53d97a64ec7421c595ff05d8",
          "url": "https://github.com/unicode-org/icu/commit/21581f4ec59b52338065866960dc2b155a428700"
        },
        "date": 1679079936973,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.40594951059563,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "a518312ce2b6bf9e96451b73d6e890cf07409872",
          "message": "ICU-22220 Add usimplenumberformatter and SimpleNumberFormatter to docmain.h",
          "timestamp": "2023-03-17T21:03:29-07:00",
          "tree_id": "e53af4b72ac1a4c23d81729cd4ae6a44d5bf5fa8",
          "url": "https://github.com/unicode-org/icu/commit/a518312ce2b6bf9e96451b73d6e890cf07409872"
        },
        "date": 1679112459238,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.33220508416048,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "e612a4f2ab3773cc120f32ebe71c62362b9c78b7",
          "message": "ICU-22220 BRS 73rc update urename.h pass 2",
          "timestamp": "2023-03-20T13:32:31-07:00",
          "tree_id": "5482e3c5d37cfcc087d81d1617fca2117d5ad0d2",
          "url": "https://github.com/unicode-org/icu/commit/e612a4f2ab3773cc120f32ebe71c62362b9c78b7"
        },
        "date": 1679344717249,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.48737014502504,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "fa5a6c15191dea895837ccfb2d658d315e23e6e4",
          "message": "ICU-22288 Remove unnecessary assertion in ICU4J NumberRangeFormatter\n\nSee #2377",
          "timestamp": "2023-03-21T12:34:34-05:00",
          "tree_id": "f19c73a6aeacf6481bd6415f005559bcc89e4e08",
          "url": "https://github.com/unicode-org/icu/commit/fa5a6c15191dea895837ccfb2d658d315e23e6e4"
        },
        "date": 1679420238446,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.3497889201657,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "49e8b992962c048a31bc542d915a9daa6f4b3dc5",
          "message": "ICU-22220 BRS73 Clean up Java import statements",
          "timestamp": "2023-03-21T15:35:52-04:00",
          "tree_id": "af2d0660599b43060cbd604b64fa84e6e65323bf",
          "url": "https://github.com/unicode-org/icu/commit/49e8b992962c048a31bc542d915a9daa6f4b3dc5"
        },
        "date": 1679427915031,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 276.04151672627086,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "rp9.next@gmail.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "68f5ef835bd69221bf1c23ba65228d0cae7e5647",
          "message": "ICU-22220 BRS73RC Regenerate configure files and fix broken links in comments",
          "timestamp": "2023-03-22T12:38:41+05:30",
          "tree_id": "4606a4c58c5a129161edf4bf043d632e809c413e",
          "url": "https://github.com/unicode-org/icu/commit/68f5ef835bd69221bf1c23ba65228d0cae7e5647"
        },
        "date": 1679469499361,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.37418880896314,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "1d507acacd2957a605cc28b5a48b52fab6ec4c4a",
          "message": "ICU-22256 Add helper code to dump Bidi_Mirroring_Glyph data to file\n\nSee #2391",
          "timestamp": "2023-03-22T16:33:17-04:00",
          "tree_id": "c7d00ce72b3005fc91485405c876d3b2711d247f",
          "url": "https://github.com/unicode-org/icu/commit/1d507acacd2957a605cc28b5a48b52fab6ec4c4a"
        },
        "date": 1679517333420,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 223.4571996427391,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "6ddf63ed926c7d934ffa01a1c841cec300207aae",
          "message": "ICU-22220 BRS73 Fix API doc tagging problems",
          "timestamp": "2023-03-22T19:41:42-04:00",
          "tree_id": "3f60d7c82f1405f739d1598380417a96a6f3b5ef",
          "url": "https://github.com/unicode-org/icu/commit/6ddf63ed926c7d934ffa01a1c841cec300207aae"
        },
        "date": 1679529249420,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 223.42524649138628,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "bfa5f4e6ae177860d867af047d759a88076d7c38",
          "message": "ICU-22270 Fix hex number formatting in icuexportdata",
          "timestamp": "2023-03-22T20:02:57-04:00",
          "tree_id": "1871a864c2c92520f92724f78601a4cf4c2de6ad",
          "url": "https://github.com/unicode-org/icu/commit/bfa5f4e6ae177860d867af047d759a88076d7c38"
        },
        "date": 1679530971668,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 231.6958858193242,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "ac596798e808b971edad48c78d9539459dd8e9d2",
          "message": "ICU-22270 Fix hex number formatting in icuexportdata",
          "timestamp": "2023-03-22T20:03:03-04:00",
          "tree_id": "a48d167828547b3d2018080f8b4cf985b123ff8b",
          "url": "https://github.com/unicode-org/icu/commit/ac596798e808b971edad48c78d9539459dd8e9d2"
        },
        "date": 1679531392641,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.69118461390997,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "4a30076686bfc7e8f08dc9e9081ecc0dacb4752d",
          "message": "ICU-22220 BRS 73rc ICU4J API signature data file",
          "timestamp": "2023-03-23T08:45:26-07:00",
          "tree_id": "a8609615ebcf9d175f035d3394598a72696338bf",
          "url": "https://github.com/unicode-org/icu/commit/4a30076686bfc7e8f08dc9e9081ecc0dacb4752d"
        },
        "date": 1679587020209,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 241.83619476588632,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "cb65f7573978edecd8a598ab4137327ed7000844",
          "message": "ICU-22220 BRS 73rc ICU4J Serialization test data for ICU 73.1",
          "timestamp": "2023-03-23T08:45:38-07:00",
          "tree_id": "a44935926111d32d475f7b1f25e0f34eef09aa7c",
          "url": "https://github.com/unicode-org/icu/commit/cb65f7573978edecd8a598ab4137327ed7000844"
        },
        "date": 1679587663797,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 304.39274785876927,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "4b5a6edc9514543436d0c5b39cbac37fc802957c",
          "message": "ICU-21755 commit-checker: update Tip\n\n- improve recommendation on no-commit-found case for ICU and CLDR\n- for CLDR-15423",
          "timestamp": "2023-03-23T11:16:25-05:00",
          "tree_id": "7933e0290d78ef64241338ffa547f60ffb83c55c",
          "url": "https://github.com/unicode-org/icu/commit/4b5a6edc9514543436d0c5b39cbac37fc802957c"
        },
        "date": 1679589460720,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 254.3186967132358,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "b31579920e11fe42eca67ccebe58d9dfd33edd80",
          "message": "ICU-22327 Update tzdata to 2023a",
          "timestamp": "2023-03-23T13:56:40-04:00",
          "tree_id": "c505f625913b6a0f0e88d34bd308788dfe5c2ac8",
          "url": "https://github.com/unicode-org/icu/commit/b31579920e11fe42eca67ccebe58d9dfd33edd80"
        },
        "date": 1679594659366,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 229.38002644380254,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "b31579920e11fe42eca67ccebe58d9dfd33edd80",
          "message": "ICU-22327 Update tzdata to 2023a",
          "timestamp": "2023-03-23T13:56:40-04:00",
          "tree_id": "c505f625913b6a0f0e88d34bd308788dfe5c2ac8",
          "url": "https://github.com/unicode-org/icu/commit/b31579920e11fe42eca67ccebe58d9dfd33edd80"
        },
        "date": 1679597674361,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 249.22963677410124,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "uioptt24@gmail.com",
            "name": "Ho Cheung",
            "username": "gz83"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "84e47620692be90950d090f2f4722494b020ad96",
          "message": "ICU-22295 Remove deprecated RBBI LBCMNoChain option\n\nICU-22295 Remove deprecated RBBI LBCMNoChain option\n\nICU-22295 Remove deprecated RBBI LBCMNoChain option",
          "timestamp": "2023-03-24T12:59:44-07:00",
          "tree_id": "0f7695aef1a4eea2d432cb5fb46151e81b51d3e4",
          "url": "https://github.com/unicode-org/icu/commit/84e47620692be90950d090f2f4722494b020ad96"
        },
        "date": 1679688393363,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 240.21389830819825,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "a6e9f518590f97dd77ab51e794c7ba9955c679a0",
          "message": "ICU-22334 Update ICU tzdata to 2023b for ICU 73.1 release",
          "timestamp": "2023-03-24T16:23:50-04:00",
          "tree_id": "471e078e91f90ea52ff0487049742a6d014a6468",
          "url": "https://github.com/unicode-org/icu/commit/a6e9f518590f97dd77ab51e794c7ba9955c679a0"
        },
        "date": 1679689969497,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 243.6196442745973,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "3867acf1099428fdc394bcf97755863a9d715c13",
          "message": "ICU-22330 build and test ICU4J with Java 17",
          "timestamp": "2023-03-24T14:00:02-07:00",
          "tree_id": "1702b1a40e5a0c6400655ab04a0d7a52301861f4",
          "url": "https://github.com/unicode-org/icu/commit/3867acf1099428fdc394bcf97755863a9d715c13"
        },
        "date": 1679692189169,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.53945137025485,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "22bbc7e12e2f6b49410c3665dee2dc202e578213",
          "message": "ICU-22308 add alignas(16) to the data entry point definition\nnot just to its struct declaration.",
          "timestamp": "2023-03-28T14:20:21-07:00",
          "tree_id": "9c0257311f94ca8f15a1d602ea60f289d1311d88",
          "url": "https://github.com/unicode-org/icu/commit/22bbc7e12e2f6b49410c3665dee2dc202e578213"
        },
        "date": 1680038999058,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.74980352721522,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "dfdf9141e577d61e340442a3ffbfdb80cd935587",
          "message": "ICU-22220 Add step for updating API Doc table\n\nSee #2400",
          "timestamp": "2023-03-29T11:25:39-07:00",
          "tree_id": "ae2677a51d30b6a763421ed7508d7970a25ee93b",
          "url": "https://github.com/unicode-org/icu/commit/dfdf9141e577d61e340442a3ffbfdb80cd935587"
        },
        "date": 1680115066293,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 219.04029027804629,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "9f774e2b8c8da3910ba43109e51c940cc8ab1696",
          "message": "ICU-22339 Update ICU tzdata to 2023c for ICU 73.1 release",
          "timestamp": "2023-03-29T17:31:51-04:00",
          "tree_id": "339722d5cd8153d0c6d43b643c34ce8898e83419",
          "url": "https://github.com/unicode-org/icu/commit/9f774e2b8c8da3910ba43109e51c940cc8ab1696"
        },
        "date": 1680126160114,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.30700867638265,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "c7029502947779b27562ff50a0b9af41fb30efa1",
          "message": "ICU-22340 Fix it so that UNUM_NUMBERING_SYSTEM again always returns a RuleBasedNumberFormat.",
          "timestamp": "2023-03-29T16:31:55-07:00",
          "tree_id": "13d2084dc5d365ca263203d30b7ada8c1ed77c42",
          "url": "https://github.com/unicode-org/icu/commit/c7029502947779b27562ff50a0b9af41fb30efa1"
        },
        "date": 1680133392109,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.71844415830162,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "95d905a9e9236dc5b9f44b2e1ba30aafa7c0db62",
          "message": "ICU-22190 Add information about the KEYS file to the Publish document.",
          "timestamp": "2023-03-30T14:06:15+02:00",
          "tree_id": "b4b1f0e81dc5de77468d7d1542fdfb80e8711dec",
          "url": "https://github.com/unicode-org/icu/commit/95d905a9e9236dc5b9f44b2e1ba30aafa7c0db62"
        },
        "date": 1680178653629,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 249.74223835925002,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "c125cf67f033b74ba56b0787f1cad75fd64d7454",
          "message": "ICU-22337 restore strict parsing length tolerance for non-abutting numeric date fields",
          "timestamp": "2023-03-30T13:52:40-07:00",
          "tree_id": "a8a6b79b603cd36a62522883685776cedb0bf255",
          "url": "https://github.com/unicode-org/icu/commit/c125cf67f033b74ba56b0787f1cad75fd64d7454"
        },
        "date": 1680210188010,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 230.804017099122,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "2c584abf7e4143731f90fa317481e2fb0890ffa5",
          "message": "ICU-22221 rebase CLDR 43-beta4 import onto latest maint/maint-73 (new zoneinfo64)",
          "timestamp": "2023-03-30T15:43:22-07:00",
          "tree_id": "f9130260a324ea309babf5eb9478f0eac80ec9d4",
          "url": "https://github.com/unicode-org/icu/commit/2c584abf7e4143731f90fa317481e2fb0890ffa5"
        },
        "date": 1680216772555,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 257.57693160173784,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "981c182a7f3583fe262552d5bdea2c9f83f0d5b2",
          "message": "ICU-22297 Speed up valgrind tests\n\nRemove the valgrind build in azure pipeline which is slow (about 50-75\nmins to run) and replace with a set in github action that run 17\ntesting jobs in parallel to speed it up to about 25 mins the longest.",
          "timestamp": "2023-04-05T16:42:32-07:00",
          "tree_id": "935fe91a68cfb34b2f718107e9251a426c260231",
          "url": "https://github.com/unicode-org/icu/commit/981c182a7f3583fe262552d5bdea2c9f83f0d5b2"
        },
        "date": 1680739091405,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.9719396542127,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "f4687fc25abb660b5c497536a7042c33d4c010c5",
          "message": "ICU-22221 update root collation again from CLDR 43",
          "timestamp": "2023-04-06T08:20:03-07:00",
          "tree_id": "62ce7fb5b0e606c85bb47af599b3021d287d5ba1",
          "url": "https://github.com/unicode-org/icu/commit/f4687fc25abb660b5c497536a7042c33d4c010c5"
        },
        "date": 1680795105363,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.19395161983346,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "7d4e57dff198e838c9a7dd844544a4c342859340",
          "message": "ICU-22340 Fix it so that UNUM_NUMBERING_SYSTEM again always returns a RuleBasedNumberFormat.\n\n(cherry picked from commit c7029502947779b27562ff50a0b9af41fb30efa1)",
          "timestamp": "2023-04-07T10:13:51-07:00",
          "tree_id": "e19dba025100a123fa354a7d32e6d7a50af1013f",
          "url": "https://github.com/unicode-org/icu/commit/7d4e57dff198e838c9a7dd844544a4c342859340"
        },
        "date": 1680888430887,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.68466801745092,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "80f94a636efc4dabb8e83a80ae7ec7ab6c8456e7",
          "message": "ICU-22221 CLDR release-43-beta5 to ICU maint/maint-73",
          "timestamp": "2023-04-07T11:15:47-07:00",
          "tree_id": "9ef42ecb6a67d93ef44eccf7a9f6c79d74acccf5",
          "url": "https://github.com/unicode-org/icu/commit/80f94a636efc4dabb8e83a80ae7ec7ab6c8456e7"
        },
        "date": 1680891962025,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.53422353919646,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "58c674a72eca18a0883e8e0e9258107ed48e303d",
          "message": "ICU-22309 update CONTRIBUTING.md\n\n- fix broken links\n- move all content into CONTRIBUTING.md from https://icu.unicode.org/processes/contribute\n- no process change\n\nCo-authored-by: Markus Scherer <markus.icu@gmail.com>",
          "timestamp": "2023-04-10T12:23:25-05:00",
          "tree_id": "e9a67f982b256a842c163cedd38dbb2cffc78cac",
          "url": "https://github.com/unicode-org/icu/commit/58c674a72eca18a0883e8e0e9258107ed48e303d"
        },
        "date": 1681147681553,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 246.81952925336142,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "0d2a03dbba2f867c1cce2291a4832e1791cd8b5e",
          "message": "ICU-22338 getProperty(Value)Name accepts nameChoice above 1",
          "timestamp": "2023-04-10T17:50:12-07:00",
          "tree_id": "e991e34fa892919dc36ef45f47385ac60504a482",
          "url": "https://github.com/unicode-org/icu/commit/0d2a03dbba2f867c1cce2291a4832e1791cd8b5e"
        },
        "date": 1681174788196,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 262.82186555219477,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "rp9.next@gmail.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "69e1d707f872ef861930ddc7d186b3eea9ac2918",
          "message": "ICU-22221 BRS73GA: Update version number to 73.1",
          "timestamp": "2023-04-11T15:46:18+05:30",
          "tree_id": "247bc918ed0fafb7a3d8649fde3a062295a915d4",
          "url": "https://github.com/unicode-org/icu/commit/69e1d707f872ef861930ddc7d186b3eea9ac2918"
        },
        "date": 1681208812292,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 264.5763228661154,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "5861e1fd52f1d7673eee38bc3c965aa18b336062",
          "message": "ICU-22221 update cldr-icu instructions, and ICU tag for integration",
          "timestamp": "2023-04-11T11:38:40-07:00",
          "tree_id": "132de1bb16b7d3f8fb2631203f0d909ae439be64",
          "url": "https://github.com/unicode-org/icu/commit/5861e1fd52f1d7673eee38bc3c965aa18b336062"
        },
        "date": 1681239054514,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.86104812786073,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "312bae866003461bcd4022855ac7ec60546b81f7",
          "message": "ICU-21964 use a single LICENSE file\n\n- make the icu4c and icu4j LICENSE files symlinks\n- fix paths",
          "timestamp": "2023-04-12T14:36:09-05:00",
          "tree_id": "082e0a6a51ed4e15aa2c631bfe0c0510d565ec59",
          "url": "https://github.com/unicode-org/icu/commit/312bae866003461bcd4022855ac7ec60546b81f7"
        },
        "date": 1681328893347,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 249.417607483081,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "cbb25bb186595641bc2ce9dc3f35f23d33fbcf87",
          "message": "ICU-22349 Change to use just -j value in make to \"not limit the number of jobs that can run simultaneously\"\n\nSee #2422",
          "timestamp": "2023-04-12T15:47:06-07:00",
          "tree_id": "68632a76a8468e0e26e808b1cea7ee70b4910407",
          "url": "https://github.com/unicode-org/icu/commit/cbb25bb186595641bc2ce9dc3f35f23d33fbcf87"
        },
        "date": 1681340680096,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 272.9267417600576,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "rp9-next",
            "username": "rp9-next"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "12c5fe04c06961676abc35b4e9ca5d0bdddd45f7",
          "message": "ICU-22351 Update PGP key information for release signing",
          "timestamp": "2023-04-13T08:17:47+05:30",
          "tree_id": "03d5a685a37890828ed2b9d9e53798abaf3334bd",
          "url": "https://github.com/unicode-org/icu/commit/12c5fe04c06961676abc35b4e9ca5d0bdddd45f7"
        },
        "date": 1681354598360,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 224.27196227568083,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "4b515504fdee138bbcb5fcabb6ed7ed18f544135",
          "message": "ICU-22349 Build cygwin with -j  to run multiple cores\n\nSee #2421",
          "timestamp": "2023-04-13T11:48:01+05:30",
          "tree_id": "9249dab7ae36363e3cfaa7b79d6a1dbd91de7986",
          "url": "https://github.com/unicode-org/icu/commit/4b515504fdee138bbcb5fcabb6ed7ed18f544135"
        },
        "date": 1681367330314,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 251.75316232864614,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "1e60df0f8206f136fd614a97b1ec901de94117c4",
          "message": "ICU-22341 commit checker: umbrella, explanations\n\n- support Umbrella ticket type (no commits)\n- explain why commit policy was applied",
          "timestamp": "2023-04-13T12:20:12-05:00",
          "tree_id": "1367d59a75c844a1250fd629b0865c7e4dce8df9",
          "url": "https://github.com/unicode-org/icu/commit/1e60df0f8206f136fd614a97b1ec901de94117c4"
        },
        "date": 1681407469044,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.04253921567823,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "2a2995951bb2226b998ed8e30d7709e80012cdd5",
          "message": "ICU-20926 Add tests for identity behavior in DateIntervalFormat\n\nSee #2245",
          "timestamp": "2023-04-17T14:57:50-07:00",
          "tree_id": "9549904e7dd0c5637aeb562b041323217deae87b",
          "url": "https://github.com/unicode-org/icu/commit/2a2995951bb2226b998ed8e30d7709e80012cdd5"
        },
        "date": 1681769471622,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.55227161614135,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "85fd7389e0fdd89b5a0aafe07429dd2452f1d310",
          "message": "ICU-22349 Speed up pre-merge CI\n\nSee #2420",
          "timestamp": "2023-04-19T08:06:59+05:30",
          "tree_id": "d4d1708e8d9e0574d02ea37967030c10232cea0e",
          "url": "https://github.com/unicode-org/icu/commit/85fd7389e0fdd89b5a0aafe07429dd2452f1d310"
        },
        "date": 1681872170202,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 218.84420141490614,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "295f0f2a43e05be92d417090240702cc087c595d",
          "message": "ICU-22333 Exclude EhaustivePersonNameFormatterTest also from releaseJarCheck target.",
          "timestamp": "2023-04-20T11:40:17-04:00",
          "tree_id": "d39cf2de3989e1bd8dc2fbe05e518f9bfcec5876",
          "url": "https://github.com/unicode-org/icu/commit/295f0f2a43e05be92d417090240702cc087c595d"
        },
        "date": 1682006411851,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 289.87467751442125,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "09f4459a77a8dbc12f15e02f6820a994636773b5",
          "message": "ICU-22221 Add the -R flag to less to display ANSI colors.\n\nWithout this flag, any ANSI color escape sequences in the Maven error\nlog will be displayed in their escaped form, which isn't very readable.",
          "timestamp": "2023-04-20T17:57:22+02:00",
          "tree_id": "cd5967983aa370d57e06368ae3ae19d5d4537067",
          "url": "https://github.com/unicode-org/icu/commit/09f4459a77a8dbc12f15e02f6820a994636773b5"
        },
        "date": 1682008312746,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 227.51020345032822,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "5be2ea84e59e63e4d68ebcd4082996d6db3af9f0",
          "message": "ICU-22349 Bump the cygwin timeout to 50 minutes",
          "timestamp": "2023-04-20T19:32:34-07:00",
          "tree_id": "a4299e363ed4dfa61dd3e105076e6b7566daaae7",
          "url": "https://github.com/unicode-org/icu/commit/5be2ea84e59e63e4d68ebcd4082996d6db3af9f0"
        },
        "date": 1682045199731,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.1338104051577,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "ec3aef8feca208fa9c9c624dd591cfc982f826d2",
          "message": "ICU-22309 update CONTRIBUTING.md for new process\n\n- see https://github.com/unicode-org/.github/blob/main/.github/CONTRIBUTING.md",
          "timestamp": "2023-04-24T13:55:48-05:00",
          "tree_id": "cc589453ba99873a3421d14a22767492711e1249",
          "url": "https://github.com/unicode-org/icu/commit/ec3aef8feca208fa9c9c624dd591cfc982f826d2"
        },
        "date": 1682363460424,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 260.2733184373282,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "4fd9d6ce9a951e66e727b296138f22cd05479de1",
          "message": "ICU-22356 Use ConstChar16Ptr to safely cast from UChar* to char16_t*.\n\nThis is necessary for this header file to be usable by clients that\ndefine UCHAR_TYPE as a type not compatible with char16_t, eg. uint16_t.",
          "timestamp": "2023-04-24T23:56:49+02:00",
          "tree_id": "2f0ee2671c534967bbe737d437a82479794a917a",
          "url": "https://github.com/unicode-org/icu/commit/4fd9d6ce9a951e66e727b296138f22cd05479de1"
        },
        "date": 1682374165189,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 262.8429186375326,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "85fd7389e0fdd89b5a0aafe07429dd2452f1d310",
          "message": "ICU-22349 Speed up pre-merge CI\n\nSee #2420",
          "timestamp": "2023-04-19T08:06:59+05:30",
          "tree_id": "d4d1708e8d9e0574d02ea37967030c10232cea0e",
          "url": "https://github.com/unicode-org/icu/commit/85fd7389e0fdd89b5a0aafe07429dd2452f1d310"
        },
        "date": 1682421426585,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 262.14343867606414,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "62f493827fdc7a0b0c8b8b0e879f14ae4b9866dc",
          "message": "ICU-22369 Merge maint/maint-73 into main (#2437)",
          "timestamp": "2023-04-27T16:19:55-07:00",
          "tree_id": "a75fc27e6b4fcadb54bfa537c2072f62771a20e0",
          "url": "https://github.com/unicode-org/icu/commit/62f493827fdc7a0b0c8b8b0e879f14ae4b9866dc"
        },
        "date": 1682637859189,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 251.27640108827381,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "robertbastian@google.com",
            "name": "Robert Bastian",
            "username": "robertbastian"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "6c05042cbcf447eefd49da70c4d94359c336e60d",
          "message": "ICU-22373 Export segmentation dictionaries",
          "timestamp": "2023-05-02T07:29:27-07:00",
          "tree_id": "b704f2e6895b6f4a1c0b2e8ed23553914dc7f83a",
          "url": "https://github.com/unicode-org/icu/commit/6c05042cbcf447eefd49da70c4d94359c336e60d"
        },
        "date": 1683038606402,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 242.1684163366737,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "robertbastian@google.com",
            "name": "Robert Bastian",
            "username": "robertbastian"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "6342f9140a4c8179667a1757461bd8fe1d6bf4b9",
          "message": "ICU-22373 Export segmentation dictionaries\n\n(cherry picked from commit 6c05042cbcf447eefd49da70c4d94359c336e60d)",
          "timestamp": "2023-05-02T10:45:59-07:00",
          "tree_id": "e0be65e7e334c1d888674977da978a7f50e2222a",
          "url": "https://github.com/unicode-org/icu/commit/6342f9140a4c8179667a1757461bd8fe1d6bf4b9"
        },
        "date": 1683050211148,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.63669275290425,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "0e4b10b1121a26fe4d1653d740f5a15bad9c48f0",
          "message": "ICU-22378 Fix temperature format ignoring -u-mu-fahrenhe",
          "timestamp": "2023-05-03T09:24:53-07:00",
          "tree_id": "062c1b22153ce55dbe44c2e7876c84761b658492",
          "url": "https://github.com/unicode-org/icu/commit/0e4b10b1121a26fe4d1653d740f5a15bad9c48f0"
        },
        "date": 1683131822527,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.80301358088832,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "00bb6789b3895e4ed13701ab1cfaf6954a2d54fe",
          "message": "ICU-22323 Remove broken link from the Chinese Calendar documentation",
          "timestamp": "2023-05-03T10:51:22-07:00",
          "tree_id": "d5a3943d80732201af43afd611f74e2f6474fdec",
          "url": "https://github.com/unicode-org/icu/commit/00bb6789b3895e4ed13701ab1cfaf6954a2d54fe"
        },
        "date": 1683136945478,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 283.0103012595085,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "vichang@google.com",
            "name": "Victor Chang",
            "username": "gvictor"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "530ca9817641c6a90c9c8c5028996de1d559e4a8",
          "message": "ICU-22371 Fix ULocale.getISOLanguages() API docs",
          "timestamp": "2023-05-03T11:13:32-07:00",
          "tree_id": "4c0c892bf64dfe4a34ef1a0a7b2b2404e1bc60d7",
          "url": "https://github.com/unicode-org/icu/commit/530ca9817641c6a90c9c8c5028996de1d559e4a8"
        },
        "date": 1683138991152,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.2089518428354,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "5618203821839cad2286a67682fb1940c8ce586f",
          "message": "ICU-22360 revert portions of #2159 which included @ in ALetter for wordbreak, update tests",
          "timestamp": "2023-05-06T21:36:46-07:00",
          "tree_id": "d8b965506e7bbeb71df2856099dabe5527aafa02",
          "url": "https://github.com/unicode-org/icu/commit/5618203821839cad2286a67682fb1940c8ce586f"
        },
        "date": 1683435008779,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 221.690396128084,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "e1e1c5feaf67652d9f124b23749a430d3278fe7b",
          "message": "ICU-22349 Use -l with make -j to limit jobs based on load average.\n\nIt has been proposed that make running too many parallel jobs recently\nhas led to resource exhaustion in our CI, so that some kind of limit\nwould be helpful to set.\n\nThe load average 2.5 limit choosen here is simply the limit used as an\nexample in the make documentation, as we don't really have any way of\npicking an initial value that's certain to be better.\n\nhttps://www.gnu.org/software/make/manual/html_node/Parallel.html\n\nSee #2421\nSee #2422",
          "timestamp": "2023-05-11T00:50:59+02:00",
          "tree_id": "70da6711d369c3bf0ac406c287b6bf192d4757a5",
          "url": "https://github.com/unicode-org/icu/commit/e1e1c5feaf67652d9f124b23749a430d3278fe7b"
        },
        "date": 1683759912212,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.71052916985434,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "d205e03352d0b2339d6b99d35e143e0843029cfa",
          "message": "ICU-22325 CLDR release-43-1-beta1 to ICU main",
          "timestamp": "2023-05-10T18:08:25-07:00",
          "tree_id": "21e686796049898b1f55d5588ea1ed312e280032",
          "url": "https://github.com/unicode-org/icu/commit/d205e03352d0b2339d6b99d35e143e0843029cfa"
        },
        "date": 1683768223899,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.07370074939507,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "committer": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "distinct": true,
          "id": "ba1c7006b7b5acd734538b9997102d7f46203576",
          "message": "ICU-22384 Limit execution of performance tests\n\nRun the tests only on the main branch of the unicode-org/icu repository. This avoids diluting the performance charts with performance results from the maintenance branches. Also, the performance tests won't execute on forked directories anymore, on which they fail after execution anyway, thus using GitHub resources without purpose.",
          "timestamp": "2023-05-11T23:02:31-07:00",
          "tree_id": "c052171b524a44e1631c9a523bdbcb2de64532a5",
          "url": "https://github.com/unicode-org/icu/commit/ba1c7006b7b5acd734538b9997102d7f46203576"
        },
        "date": 1683871673143,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.80535625346644,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "6e72d908665fa9bc69e0853b1851d3eb8acbf843",
          "message": "ICU-22360 revert portions of #2159 which included @ in ALetter for wordbreak, update tests\n\n(cherry picked from commit 5618203821839cad2286a67682fb1940c8ce586f)",
          "timestamp": "2023-05-12T13:04:33-07:00",
          "tree_id": "4fbe12156aa08d96c28abd1200afd0595a9ddec3",
          "url": "https://github.com/unicode-org/icu/commit/6e72d908665fa9bc69e0853b1851d3eb8acbf843"
        },
        "date": 1683922516783,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 217.2281917573056,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "7f5d679a982cf8fc9308a01159a1ccb071c7b508",
          "message": "ICU-22357 Update gb18030 mappings for the -2022 version\n\nSee #2430",
          "timestamp": "2023-05-18T08:51:47-07:00",
          "tree_id": "7a1da39fadce0c7c60f622464bae85df659e4269",
          "url": "https://github.com/unicode-org/icu/commit/7f5d679a982cf8fc9308a01159a1ccb071c7b508"
        },
        "date": 1684426006707,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 275.16617218108155,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "18182903cdabff007d90be4fd6b3743e753f5e22",
          "message": "ICU-22356 Use ConstChar16Ptr to safely cast from UChar* to char16_t*.\n\nThis is necessary for this header file to be usable by clients that\ndefine UCHAR_TYPE as a type not compatible with char16_t, eg. uint16_t.",
          "timestamp": "2023-05-18T10:47:55-07:00",
          "tree_id": "38ff36de982b5159afbc7c6833f55027bd2015b3",
          "url": "https://github.com/unicode-org/icu/commit/18182903cdabff007d90be4fd6b3743e753f5e22"
        },
        "date": 1684432728443,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.2336187637841,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "8d2c78d160dcb831a11efd2d4e5ec1ef081a19bf",
          "message": "ICU-22357 Rename gb18030.ucm to add -2022, follow-on to PR #2430",
          "timestamp": "2023-05-25T09:25:35-07:00",
          "tree_id": "028575b3795ebd11bd5d40e9ef3d4b519b18a984",
          "url": "https://github.com/unicode-org/icu/commit/8d2c78d160dcb831a11efd2d4e5ec1ef081a19bf"
        },
        "date": 1685032671693,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.85494357819016,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "43cd3ce6470be5817749b9e0c4da9f0e14fb1b58",
          "message": "ICU-22372 Fix headers in icuexportdata",
          "timestamp": "2023-05-25T10:09:07-07:00",
          "tree_id": "a934f5d3f0dc70611fd53ad4750bf193060c07a8",
          "url": "https://github.com/unicode-org/icu/commit/43cd3ce6470be5817749b9e0c4da9f0e14fb1b58"
        },
        "date": 1685036246821,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 270.42467485054937,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "5ef4fa29899dfc8ea71dc563002b34edcffe2e4f",
          "message": "ICU-22379 Update ICU PersonNameFormatter to match the spec change requested by CLDR-16623",
          "timestamp": "2023-05-25T16:43:33-07:00",
          "tree_id": "cfc8d8ed92d966af5aa5510b40ae1bfd0a105e92",
          "url": "https://github.com/unicode-org/icu/commit/5ef4fa29899dfc8ea71dc563002b34edcffe2e4f"
        },
        "date": 1685058869464,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.43870444196907,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "b5f8aaf0103a98c76232639546a1e401aa354727",
          "message": "ICU-22379 Update ICU PersonNameFormatter to match the spec change requested by CLDR-16623\n\n(cherry picked from commit 11f85eb4c219aa9138abd408077f00664e8496e5)",
          "timestamp": "2023-05-25T17:44:10-07:00",
          "tree_id": "d329bf5d1f069a63d6d4cd21c2b5e095d1c95228",
          "url": "https://github.com/unicode-org/icu/commit/b5f8aaf0103a98c76232639546a1e401aa354727"
        },
        "date": 1685062280277,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.68736142539362,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nvinson234@gmail.com",
            "name": "Nicholas Vinson",
            "username": "nvinson"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "0fb1b5588e604cb013b605e38895d8fb3866bffb",
          "message": "ICU-22377 Fix va_end errors found by gcc -fanalyzer\n\nFixes missing call to ‘va_end’ errors.\n\nSigned-off-by: Nicholas Vinson <nvinson234@gmail.com>",
          "timestamp": "2023-05-26T15:50:21+02:00",
          "tree_id": "0df58514c8318f3d781631e1536d2e8d005292f0",
          "url": "https://github.com/unicode-org/icu/commit/0fb1b5588e604cb013b605e38895d8fb3866bffb"
        },
        "date": 1685110033828,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 271.16021576109256,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "130bdd47480ee1bf4ac7edc5188c997e1058e481",
          "message": "ICU-22357 Update gb18030 mappings for the -2022 version\n\n(cherry picked from commit 7f5d679a982cf8fc9308a01159a1ccb071c7b508)",
          "timestamp": "2023-05-26T12:34:06-07:00",
          "tree_id": "c058851e55b012b7fd4e0d029bc7a35bd6ff7ba7",
          "url": "https://github.com/unicode-org/icu/commit/130bdd47480ee1bf4ac7edc5188c997e1058e481"
        },
        "date": 1685129927591,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 229.0270372700641,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "8bbb8f506ea0f4c2fe5ea5fd1cdadbd53d11ced6",
          "message": "ICU-22403 Fix icuexportdata out-of-bounds during decomposition",
          "timestamp": "2023-05-26T13:27:55-07:00",
          "tree_id": "a6e5ee837a7db2fa3da2efd7831214d119e2784c",
          "url": "https://github.com/unicode-org/icu/commit/8bbb8f506ea0f4c2fe5ea5fd1cdadbd53d11ced6"
        },
        "date": 1685133775013,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 224.84012013107176,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "de26ea8c6a37c4f6e1382560fb8f791f7cecaa2b",
          "message": "ICU-22322 Revert extra parallelism for data-filter test in .azure-pipelines.yml\n\nSee #2471",
          "timestamp": "2023-05-26T13:39:11-07:00",
          "tree_id": "661eaf1a4ee4fa0f6d79244e29c7b4b7cbbc431d",
          "url": "https://github.com/unicode-org/icu/commit/de26ea8c6a37c4f6e1382560fb8f791f7cecaa2b"
        },
        "date": 1685135031514,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 249.2780777480568,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "5435007e6a25bd8d365e8a3e560f40f14961d2b9",
          "message": "ICU-21697 Convert ICU Site pages to markdown for Github Pages\n\nSee #1785",
          "timestamp": "2023-05-30T16:18:32-07:00",
          "tree_id": "363563a66f490b7b613093e83ffdcd4393faa1eb",
          "url": "https://github.com/unicode-org/icu/commit/5435007e6a25bd8d365e8a3e560f40f14961d2b9"
        },
        "date": 1685489522652,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 262.3595627865865,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "jharkawat@microsoft.com",
            "name": "JALAJ HARKAWAT",
            "username": "jalaj-microsoft"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "90d7ea8196110f8f32e60e7e79b472bd9c82e35e",
          "message": "ICU-22389 BRS73: Update version number to 73.2",
          "timestamp": "2023-06-01T14:52:29+05:30",
          "tree_id": "28764dbc64980c86c5b412c770edb81c931a9562",
          "url": "https://github.com/unicode-org/icu/commit/90d7ea8196110f8f32e60e7e79b472bd9c82e35e"
        },
        "date": 1685611790203,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 227.3345262887093,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "vichang@google.com",
            "name": "Victor Chang",
            "username": "gvictor"
          },
          "committer": {
            "email": "mark@unicode.org",
            "name": "Mark Davis",
            "username": "macchiati"
          },
          "distinct": true,
          "id": "a18c8f83e3adbfd70b6b229c2afdb3f05e3586a6",
          "message": "ICU-22390 Speed-up ICU4J Transliterator.<clinit>\n\nIt reduces the method runtime by approx. 60%.",
          "timestamp": "2023-06-01T05:32:56-07:00",
          "tree_id": "6926aedc2b59e9504ba3010360d64ec117d952e6",
          "url": "https://github.com/unicode-org/icu/commit/a18c8f83e3adbfd70b6b229c2afdb3f05e3586a6"
        },
        "date": 1685623195888,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.32238034903125,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "1119ae4885559e7665f7464d64b7f64e5f1b2344",
          "message": "ICU-22333 Exclude EhaustivePersonNameFormatterTest also from releaseJarCheck target.",
          "timestamp": "2023-06-01T11:39:40-04:00",
          "tree_id": "b61e76e13ae9e26f74b88ff7434d7aef602a78a0",
          "url": "https://github.com/unicode-org/icu/commit/1119ae4885559e7665f7464d64b7f64e5f1b2344"
        },
        "date": 1685634854547,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.60590732821296,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "7d0d24e7da64009429544c54a059fe158539df57",
          "message": "ICU-22372 Fix headers in icuexportdata",
          "timestamp": "2023-06-01T08:47:31-07:00",
          "tree_id": "56ffd99c2d1b61176a51d0301eb741409dce352d",
          "url": "https://github.com/unicode-org/icu/commit/7d0d24e7da64009429544c54a059fe158539df57"
        },
        "date": 1685635653690,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.67127406773636,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "fe042bdc0def305eeb77f587bc09c438fb699a19",
          "message": "ICU-22403 Fix icuexportdata out-of-bounds during decomposition",
          "timestamp": "2023-06-01T08:51:28-07:00",
          "tree_id": "8f7d2c4f719d4de66be8cfa4cd3da29f9f581207",
          "url": "https://github.com/unicode-org/icu/commit/fe042bdc0def305eeb77f587bc09c438fb699a19"
        },
        "date": 1685636887793,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 275.41060594173695,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "f4234577ad723ec27c251a5b88733728aad212e8",
          "message": "ICU-22404 initial changes.txt for Unicode 15.1\n\nSee #2490\r\n- copy 15.0 change log to the top, unchanged\r\n- adjust changes.txt for 15.1, incl. diffs from CLDR 43 root collation update",
          "timestamp": "2023-06-04T20:16:39-07:00",
          "tree_id": "f5884e4e3844adf95356942ec886bd2e02dd3df7",
          "url": "https://github.com/unicode-org/icu/commit/f4234577ad723ec27c251a5b88733728aad212e8"
        },
        "date": 1685935301251,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.1535833435323,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "e2fde33b4bea1ef865eadf48981b092273442fd7",
          "message": "ICU-22396 Fix tool until ICU4J add Temporal Calendar API\n\nSee #2488",
          "timestamp": "2023-06-05T15:26:53-07:00",
          "tree_id": "af9b1e861010ebe168965a04492d760e194ddcac",
          "url": "https://github.com/unicode-org/icu/commit/e2fde33b4bea1ef865eadf48981b092273442fd7"
        },
        "date": 1686004997966,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 233.79584023097098,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "680f521746a3bd6a86f25f25ee50a62d88b489cf",
          "message": "ICU-22389 Cherry-pick CLDR 43.1 data from main to maint-73 (#2495)",
          "timestamp": "2023-06-07T19:19:55-07:00",
          "tree_id": "0b0e97b8941c68d18e1f5b98fda41ce081935ec1",
          "url": "https://github.com/unicode-org/icu/commit/680f521746a3bd6a86f25f25ee50a62d88b489cf"
        },
        "date": 1686191531087,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 218.30435452061374,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pnacht@google.com",
            "name": "Pedro Kaj Kjellerup Nacht",
            "username": "pnacht"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "7a101d68eab556c1b3bc0adf65998362ee941589",
          "message": "ICU-22350 Fix broken performance CI\n\nSigned-off-by: Pedro Kaj Kjellerup Nacht <pnacht@google.com>",
          "timestamp": "2023-06-13T16:41:39-07:00",
          "tree_id": "52a670393d4cc28f78185c88be7644a582ea1007",
          "url": "https://github.com/unicode-org/icu/commit/7a101d68eab556c1b3bc0adf65998362ee941589"
        },
        "date": 1686700627300,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.83072414600272,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "f00ff4f5e31d43f4813c219e980b0a8653574356",
          "message": "ICU-22406 Add LIBRARY_DATA_DIR",
          "timestamp": "2023-06-13T22:01:59-07:00",
          "tree_id": "68e406d5283a2f5d7e253101873925d03b3b2886",
          "url": "https://github.com/unicode-org/icu/commit/f00ff4f5e31d43f4813c219e980b0a8653574356"
        },
        "date": 1686719798190,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.8943835491188,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "53e04868558469dc394ac8a58c7ac58e231972f2",
          "message": "ICU-22389 Add missing git lfs install to instructions.",
          "timestamp": "2023-06-14T15:08:35+02:00",
          "tree_id": "226fd3ae5027aba47419cd2e9fe693111f63e492",
          "url": "https://github.com/unicode-org/icu/commit/53e04868558469dc394ac8a58c7ac58e231972f2"
        },
        "date": 1686748465273,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.91872569425223,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "e567b50df853b7307d25f45f1ecf5df912b78ec9",
          "message": "ICU-22389 Set STRIP_FROM_PATH to enable Doxygen out-of-source builds.\n\nWithout this flag, the full path to the source directory will be\nincluded in the output generated by Doxygen.",
          "timestamp": "2023-06-14T15:08:59+02:00",
          "tree_id": "e2a959e4d182c234303af31d1351cf037eebe1ba",
          "url": "https://github.com/unicode-org/icu/commit/e567b50df853b7307d25f45f1ecf5df912b78ec9"
        },
        "date": 1686749947821,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 239.5891803338317,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "9411a940ea878969d80da7ef4d74cae5b6d953bc",
          "message": "ICU-22414 Fix bogus locale in collation res fallback",
          "timestamp": "2023-06-14T11:30:00-07:00",
          "tree_id": "1b645cbefa8a6ec47a76e4ba7f610900fe62fd03",
          "url": "https://github.com/unicode-org/icu/commit/9411a940ea878969d80da7ef4d74cae5b6d953bc"
        },
        "date": 1686767906145,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 263.152120742657,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pnacht@google.com",
            "name": "Pedro Kaj Kjellerup Nacht",
            "username": "pnacht"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "758099ab3b87a2404dcc297ff88260d7a09eccf7",
          "message": "ICU-22410 Add security policy\n\nSigned-off-by: Pedro Kaj Kjellerup Nacht <pnacht@google.com>\nCo-authored-by: Markus Scherer <markus.icu@gmail.com>",
          "timestamp": "2023-06-14T13:54:42-07:00",
          "tree_id": "f3a27970e172a59c0b79eec7686e648895d5012b",
          "url": "https://github.com/unicode-org/icu/commit/758099ab3b87a2404dcc297ff88260d7a09eccf7"
        },
        "date": 1686776618128,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 217.4900985509021,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "c94469b9a0fe2e717a8cb95edc4b2336e3082161",
          "message": "ICU-22323 Correct alt='ascii' paths, point Readme.txt to .md",
          "timestamp": "2023-06-15T11:25:32-07:00",
          "tree_id": "b06dab4b3f15e138e58ebde191d673d672e6a1b3",
          "url": "https://github.com/unicode-org/icu/commit/c94469b9a0fe2e717a8cb95edc4b2336e3082161"
        },
        "date": 1686854551133,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.91513875782493,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pnacht@google.com",
            "name": "Pedro Kaj Kjellerup Nacht",
            "username": "pnacht"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "7a101d68eab556c1b3bc0adf65998362ee941589",
          "message": "ICU-22350 Fix broken performance CI\n\nSigned-off-by: Pedro Kaj Kjellerup Nacht <pnacht@google.com>",
          "timestamp": "2023-06-13T16:41:39-07:00",
          "tree_id": "52a670393d4cc28f78185c88be7644a582ea1007",
          "url": "https://github.com/unicode-org/icu/commit/7a101d68eab556c1b3bc0adf65998362ee941589"
        },
        "date": 1687260389593,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 306.26087413993304,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "hsivonen@hsivonen.fi",
            "name": "Henri Sivonen",
            "username": "hsivonen"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "3fec4e718eed62d2b6a2a13fe5b9b82435cc9707",
          "message": "ICU-22260 Allow relative datetime formatting without context adjustment with UCONFIG_NO_BREAK_ITERATION",
          "timestamp": "2023-06-20T18:48:01-07:00",
          "tree_id": "5701096dc270afbde3ad099d14ad16be05848f5b",
          "url": "https://github.com/unicode-org/icu/commit/3fec4e718eed62d2b6a2a13fe5b9b82435cc9707"
        },
        "date": 1687312996599,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.9425377448469,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "e83b0715a15714d3de6b25b1787245abb7500a5f",
          "message": "ICU-22368 Reduce ~200K langInfo.res size by encode LSR into 32bits int.\n\nSee #2458",
          "timestamp": "2023-06-22T01:18:41-07:00",
          "tree_id": "f3264d02b6b8b5f5b457f0579e47f812f792451b",
          "url": "https://github.com/unicode-org/icu/commit/e83b0715a15714d3de6b25b1787245abb7500a5f"
        },
        "date": 1687423319147,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.47946253856045,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "26bd70e301bceed47a05b0689f8a48248ec10baf",
          "message": "ICU-22421 Remove two unused internal methods in gregorian cal\n\nRemove pinDayOfMonth() and yearLength(int32_t year)\nfrom GregorianCalendar.\n\nThese two methods are\n1. Unused by any code inside ICU, not in source/{common,i18n,test}.\n2. Marked as @internal in the header.\n3. Wrap inside #ifndef U_HIDE_INTERNAL_API block in the header\n4. In \"protected:\" section.\n5. No ICU4J counterpart.\n\nThe yearLength(int32_t year) dup the functionality as\nhandleGetYearLength of the same class and that one is the correct one to\nbe keep and used..\nThere is another yearLength() w/o the year as parameter should NOT be\nremoved and still needed internally.",
          "timestamp": "2023-06-22T15:44:05-07:00",
          "tree_id": "7d56078c3fff737c253268d856ca5c00ba7d261f",
          "url": "https://github.com/unicode-org/icu/commit/26bd70e301bceed47a05b0689f8a48248ec10baf"
        },
        "date": 1687474885009,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 230.71690564260896,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "a9f75708929a2bd14f941f36aa36a7189d76a389",
          "message": "ICU-22412 Fix C API ucal_(g|s)etGregorianChange for iso8601 calendar\n\nSee #2510",
          "timestamp": "2023-06-22T15:44:37-07:00",
          "tree_id": "4fca3922a70bb6c49fcf1c00dc88cbbd7bc48b3c",
          "url": "https://github.com/unicode-org/icu/commit/a9f75708929a2bd14f941f36aa36a7189d76a389"
        },
        "date": 1687475715548,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 219.3200832369283,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "vichang@google.com",
            "name": "Victor Chang",
            "username": "gvictor"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "83ee7e662f8b027e653f5e8b7afd73738782c088",
          "message": "ICU-22390 Transliterator.<clinit> can be optimized - part 2\n\nWIDTH_FIX instance could be lazily initialized, because it's used\nonly if transliterate() is called on the AnyTransliterator instance,\nbut apparently not used by other Transliterator instance.",
          "timestamp": "2023-06-27T11:25:43-07:00",
          "tree_id": "027a15f4548f8c7c1ac95506a236d995de9870e5",
          "url": "https://github.com/unicode-org/icu/commit/83ee7e662f8b027e653f5e8b7afd73738782c088"
        },
        "date": 1687890709468,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 285.4088580854924,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "4a255c4301f99183d2dae99f4ddd1ce6d60f8b35",
          "message": "ICU-21239 Improve docs for MeasureUnit default constructor",
          "timestamp": "2023-06-27T11:27:25-07:00",
          "tree_id": "5376b9db51ec6af5e68ff75d58687d8f948ddddf",
          "url": "https://github.com/unicode-org/icu/commit/4a255c4301f99183d2dae99f4ddd1ce6d60f8b35"
        },
        "date": 1687892440349,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 227.37050138448095,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "f1b13a850a92557d6f6430ee0f867c3bcfbecbd5",
          "message": "ICU-22354 Revert benchmark-action changes\n\nRevert the change of benchmark-action in\nhttps://github.com/unicode-org/icu/pull/2428 which cause\npost merge test brekage.\n\nSee https://github.com/unicode-org/icu/actions/runs/5393383252/jobs/9793048045\nfor the problem",
          "timestamp": "2023-06-27T14:53:04-07:00",
          "tree_id": "7ec46ee1a0240ad833328079d8aaf73b6c005a72",
          "url": "https://github.com/unicode-org/icu/commit/f1b13a850a92557d6f6430ee0f867c3bcfbecbd5"
        },
        "date": 1687903731450,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 267.0601615156722,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "jakewharton@gmail.com",
            "name": "Jake Wharton",
            "username": "JakeWharton"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "adbcfb1e077db0ac47676b861ce70b10176ec7a6",
          "message": "ICU-22426 Do not create nested StringBuilder when appending",
          "timestamp": "2023-06-29T15:01:24-07:00",
          "tree_id": "d708b459c948ef01ce96f9f1246c44610b1f5f48",
          "url": "https://github.com/unicode-org/icu/commit/adbcfb1e077db0ac47676b861ce70b10176ec7a6"
        },
        "date": 1688077405677,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 224.41232075752103,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "1b15a4e9db345b67ad284b92801f3fd99fe56e69",
          "message": "ICU-22424 Fix Calendar::clear(UCAL_MONTH)\n\nMake the calling of clear(UCAL_MONTH or UCAL_ORDINAL_MONTH) clear both fields.",
          "timestamp": "2023-06-30T00:48:34-07:00",
          "tree_id": "7a890e6c298e85f65b9044e9b7c464700d7f3c3e",
          "url": "https://github.com/unicode-org/icu/commit/1b15a4e9db345b67ad284b92801f3fd99fe56e69"
        },
        "date": 1688112026826,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 267.2591600279498,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "jw@squareup.com",
            "name": "Jake Wharton",
            "username": "JakeWharton"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "cd6ff4a64d2b6bca5c6ccc50d2d15a19fcdf2fa1",
          "message": "ICU-22425 Eliminate double map lookup for common case of present argument\n\nIn the uncommon case where the map lookup returns null, only then perform a second map lookup to determine whether it was an absent value or explicit null.",
          "timestamp": "2023-07-06T17:17:49+03:00",
          "tree_id": "bf532455650c6ba8f13f505281198e6274b68bec",
          "url": "https://github.com/unicode-org/icu/commit/cd6ff4a64d2b6bca5c6ccc50d2d15a19fcdf2fa1"
        },
        "date": 1688654010043,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 275.4051972989089,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "70d308731ae01a4187fe49f7ae1c8e1ad1570be6",
          "message": "ICU-22325 BRS 74 front-load update version to 74.0.1",
          "timestamp": "2023-07-10T16:45:41-07:00",
          "tree_id": "d8cc5bf0d7548806dcc1445da79c881e06d6c46a",
          "url": "https://github.com/unicode-org/icu/commit/70d308731ae01a4187fe49f7ae1c8e1ad1570be6"
        },
        "date": 1689033505604,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 264.5830120556081,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "034a8086049336e105179d98c6f7492ee4e6b47e",
          "message": "ICU-20898 Improve number skeleton rounding increment docs\n\nSee #2475",
          "timestamp": "2023-07-13T18:41:05+02:00",
          "tree_id": "797bd0b27ddcb31a228faa17e9f638741ebe7ff2",
          "url": "https://github.com/unicode-org/icu/commit/034a8086049336e105179d98c6f7492ee4e6b47e"
        },
        "date": 1689267496559,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 232.01904078591377,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "5826bf7ed7f45afd18793c491313b86e2b9385ba",
          "message": "ICU-22434 Not calling setFirstDayOfWeek(MONDAY) if the locale has fw\n\nThe Calendar constructor already take care of the fw override.\nWe should not set the first day of week for iso8601 to Monday if\nwe have a fw keyword/type in the locale.\n\nICU-22434 Fix incorrect calendar keyword extraction",
          "timestamp": "2023-07-13T09:49:32-07:00",
          "tree_id": "a375506ffaf4e7b361bc909f4de15008e7abd541",
          "url": "https://github.com/unicode-org/icu/commit/5826bf7ed7f45afd18793c491313b86e2b9385ba"
        },
        "date": 1689268581436,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.7601549787567,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "2e45e6ec0e84a1c01812015a254ea31b286316fb",
          "message": "ICU-22404 Unicode 15.1 beta data files & API constants\n\nSee #2492\n\nCo-authored-by: Andy Heninger <andy.heninger@gmail.com>\nCo-authored-by: Robin Leroy <egg.robin.leroy@gmail.com>",
          "timestamp": "2023-07-13T19:26:14-07:00",
          "tree_id": "56d55c3d7693da95923625d70f80d539ee552592",
          "url": "https://github.com/unicode-org/icu/commit/2e45e6ec0e84a1c01812015a254ea31b286316fb"
        },
        "date": 1689302082329,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 230.00565926280873,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "technicalcute@gmail.com",
            "name": "Jiawen Geng",
            "username": "gengjiawen"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "c7e967c456ceff6436607ca2a3da034320ca34c3",
          "message": "ICU-22401 fix build on MSVC with cpp20",
          "timestamp": "2023-07-14T14:31:40+02:00",
          "tree_id": "cef2de2169d0ee3ac2f561216cd86344355b9ab2",
          "url": "https://github.com/unicode-org/icu/commit/c7e967c456ceff6436607ca2a3da034320ca34c3"
        },
        "date": 1689338854026,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 276.51764565284,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pnacht@google.com",
            "name": "Pedro Kaj Kjellerup Nacht",
            "username": "pnacht"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "281a2a77ec8acc723f3603ad3d892e34b9ac2a29",
          "message": "ICU-22350 Give maven.yml packages:write\n\nSigned-off-by: Pedro Kaj Kjellerup Nacht <pnacht@google.com>",
          "timestamp": "2023-07-20T08:12:16-07:00",
          "tree_id": "ba2b603c4e289458016d16a5b62686f7be65d0d4",
          "url": "https://github.com/unicode-org/icu/commit/281a2a77ec8acc723f3603ad3d892e34b9ac2a29"
        },
        "date": 1689866907689,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.88143576207113,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "85e75ecc13d9bbb324bc87cb5194e6982486f2d1",
          "message": "ICU-22304 Miscellanous PersonNameFormatter fixes; made ExhaustivePersonNameFormatterTest into a real unit test.",
          "timestamp": "2023-07-20T10:52:37-07:00",
          "tree_id": "bc5617b796004d916f8e1d4cfdab37c479a0c190",
          "url": "https://github.com/unicode-org/icu/commit/85e75ecc13d9bbb324bc87cb5194e6982486f2d1"
        },
        "date": 1689876730024,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 285.5694039352151,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "2238f1c2c1279a83af4aae7f6eaec44d667f0c98",
          "message": "ICU-20297 Improve discoverability of currency formatting",
          "timestamp": "2023-07-23T09:23:13-07:00",
          "tree_id": "44a268be510bfe054933a46b370170c3e7f3ea23",
          "url": "https://github.com/unicode-org/icu/commit/2238f1c2c1279a83af4aae7f6eaec44d667f0c98"
        },
        "date": 1690130247187,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 267.26591600084384,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "03e98c04f3f1b0a66cc7dcdb5c1ce09d0c4986e4",
          "message": "ICU-22442 Sync the spelling of NEHASSA to ICU4J NEHASSE\n\nICU4J has that as part of public API but ++ is private\nso we should sync to the ICU4J one.",
          "timestamp": "2023-07-27T09:56:31-07:00",
          "tree_id": "0b5784c751989767fd537d0de515458e21fa9520",
          "url": "https://github.com/unicode-org/icu/commit/03e98c04f3f1b0a66cc7dcdb5c1ce09d0c4986e4"
        },
        "date": 1690478161486,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 300.7290593252275,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "73b61ceece7811e3e50bf8f8803bb6dca6828893",
          "message": "ICU-22444 Remove \"unknown\" from Calendar.getKeywordValuesForLocale\n\nRemove CalType enum value UNKNOWN and use null for unknown CalType\nThis value is an internal enum and the only place use it is inside Calendar.java\nUse null for that instead (same as C++)",
          "timestamp": "2023-07-27T17:07:01-07:00",
          "tree_id": "30a39dea4aa3d7c298e12e68c578e9708d7b0f01",
          "url": "https://github.com/unicode-org/icu/commit/73b61ceece7811e3e50bf8f8803bb6dca6828893"
        },
        "date": 1690504138649,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 265.54093713395207,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "19bac42f984aa3df9467135f45b7727e7fa31e81",
          "message": "ICU-22446 Fix incorrect Hebrew ORDINAL MONTH bug",
          "timestamp": "2023-07-27T22:18:00-07:00",
          "tree_id": "626d36faefa9611e01e8132eb590b82d61122926",
          "url": "https://github.com/unicode-org/icu/commit/19bac42f984aa3df9467135f45b7727e7fa31e81"
        },
        "date": 1690522446779,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 260.4945923994357,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dragan@unicode.org",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "1f07d2b29f192695a5d6f9b8a084862425b3f3d3",
          "message": "ICU-22325 Integrate CLDR 44.1 to ICU, add personName testdata, fix RBBITestMonkey",
          "timestamp": "2023-07-28T16:53:50-07:00",
          "tree_id": "54b30a91307a011563948e88573131fba52420a5",
          "url": "https://github.com/unicode-org/icu/commit/1f07d2b29f192695a5d6f9b8a084862425b3f3d3"
        },
        "date": 1690589213060,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 224.320235651588,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "d511cad90d3144ab7324f4368261249ff27a9f59",
          "message": "ICU-22407 Implement Java Temporal Calendar API\n\nSee #2526",
          "timestamp": "2023-07-28T18:17:38-07:00",
          "tree_id": "3ca05f99e25564f615c60db5305c9105e1533ec5",
          "url": "https://github.com/unicode-org/icu/commit/d511cad90d3144ab7324f4368261249ff27a9f59"
        },
        "date": 1690594496424,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 339.1597422875982,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "9511fff62aa962246aaa142a743d86234c760911",
          "message": "ICU-22407 Suppress Calendar Consistency\n\nSimilar to changes to C++ in\nhttps://github.com/unicode-org/icu/pull/2298",
          "timestamp": "2023-07-31T20:57:01-07:00",
          "tree_id": "89ff2d5af07893e60580f5651b4d5712738bc6e6",
          "url": "https://github.com/unicode-org/icu/commit/9511fff62aa962246aaa142a743d86234c760911"
        },
        "date": 1690863130149,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 265.5278479781174,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "6ba5a1a1b76a7d7dfb79e2a2db40c0cd6c401411",
          "message": "ICU-22365 C API for ULocaleBuilder\n\nSee #2520\n\nICU-22365 Fix comments",
          "timestamp": "2023-08-03T14:11:12-07:00",
          "tree_id": "c3a5b82dc4184091283bdea640ab3c2e816b53dd",
          "url": "https://github.com/unicode-org/icu/commit/6ba5a1a1b76a7d7dfb79e2a2db40c0cd6c401411"
        },
        "date": 1691097646844,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.87165325923053,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "89b952dffde5e1e49697ab2a477374b1a1387a5a",
          "message": "ICU-22362 Fix the name order derivation code in PersonNameFormatter to match the CLDR spec.",
          "timestamp": "2023-08-04T12:34:20-07:00",
          "tree_id": "63894886cce73116e6506f5bf6e1d0338de31303",
          "url": "https://github.com/unicode-org/icu/commit/89b952dffde5e1e49697ab2a477374b1a1387a5a"
        },
        "date": 1691178528211,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 232.0068497146876,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "720e5741ccaa112c4faafffdedeb7459b66c5673",
          "message": "ICU-22362 Fix build error in exhaustive tests resulting from last PersonName change.",
          "timestamp": "2023-08-04T18:44:00-07:00",
          "tree_id": "8c935ae46d180a82badbb8d09daee03d69b9082b",
          "url": "https://github.com/unicode-org/icu/commit/720e5741ccaa112c4faafffdedeb7459b66c5673"
        },
        "date": 1691200546927,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 258.1488196278224,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "committer": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "distinct": true,
          "id": "47e47ec439aee36ab4abbaa018a4d8a53fe3154d",
          "message": "ICU-22433 uppercase decomposed greek to decomposed greek and precomposed greek to precomposed greek.",
          "timestamp": "2023-08-08T22:20:20+02:00",
          "tree_id": "2e8e9441fccb5142764b957374c7dde47bf80e83",
          "url": "https://github.com/unicode-org/icu/commit/47e47ec439aee36ab4abbaa018a4d8a53fe3154d"
        },
        "date": 1691527526694,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 270.9873538191341,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "56850c9a42eab1a28c65e0c177accd9ea9272a5d",
          "message": "ICU-22402 Add support in ICU and in the CLDR-to-ICU tool for the new nativeSpaceReplacement and parameterDefault\nresources for PersonNameFormatter in CLDR. Regenerated the ICU4J data resources as well as the ICU4C resources\nto include the new resources.",
          "timestamp": "2023-08-08T14:42:02-07:00",
          "tree_id": "cd236353f48d24d9672525c7419db8b180222a96",
          "url": "https://github.com/unicode-org/icu/commit/56850c9a42eab1a28c65e0c177accd9ea9272a5d"
        },
        "date": 1691532319648,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 223.63496720743677,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "ca1435c3ea7314d9636e59a9b87d95df6dd7f8a9",
          "message": "ICU-22453 Fix non null terminated buffer issue.\n\nSee #2543",
          "timestamp": "2023-08-09T15:36:04-07:00",
          "tree_id": "3299c8d1611921acdf14a65c1045fb6e26cf082a",
          "url": "https://github.com/unicode-org/icu/commit/ca1435c3ea7314d9636e59a9b87d95df6dd7f8a9"
        },
        "date": 1691621289897,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 332.49240769498635,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "committer": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "distinct": true,
          "id": "86193b1b982528ab1a4407e80cd3bdac2e23fc26",
          "message": "ICU-22404 Improve documentation of segmentation rules",
          "timestamp": "2023-08-10T03:01:20+02:00",
          "tree_id": "5f10e968738ce16dc4e4ca46654093d68e80a29d",
          "url": "https://github.com/unicode-org/icu/commit/86193b1b982528ab1a4407e80cd3bdac2e23fc26"
        },
        "date": 1691630348908,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 256.20977508925284,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "committer": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "distinct": true,
          "id": "a6fc915e05cd00252fada5c38fc29f26968c6002",
          "message": "ICU-22404 Strip default ignorable code points in the skeleton for confusable detection",
          "timestamp": "2023-08-10T17:55:14+02:00",
          "tree_id": "0214079f29ca8235045d39b705c20d4acfb468b4",
          "url": "https://github.com/unicode-org/icu/commit/a6fc915e05cd00252fada5c38fc29f26968c6002"
        },
        "date": 1691683427715,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 227.40942692078266,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "cc2ddc0d113aa4db6efa6b071eca172e4a72f6cd",
          "message": "ICU-22325 Convert cldr-icu-readme to md, update it, point to it from older docs",
          "timestamp": "2023-08-10T14:03:06-07:00",
          "tree_id": "d7dbb7808f644aed0e2b891de9cadc3844eba560",
          "url": "https://github.com/unicode-org/icu/commit/cc2ddc0d113aa4db6efa6b071eca172e4a72f6cd"
        },
        "date": 1691702443057,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 228.20185842037554,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "ca3fd47f4cd6102e91b7d88607dcea0be7648d87",
          "message": "ICU-22402 Add options to PersonNameFormatter and update nativeSpaceReplacement logic",
          "timestamp": "2023-08-10T15:15:49-07:00",
          "tree_id": "1e7171c17d71ea9a4f84afb4f42cb84a00629d29",
          "url": "https://github.com/unicode-org/icu/commit/ca3fd47f4cd6102e91b7d88607dcea0be7648d87"
        },
        "date": 1691706629961,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 274.7529161493519,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "chachoi@blackberry.com",
            "name": "James Choi",
            "username": "chachoi"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "b70b2d0433d5004f72b5d7590a82879673527ae0",
          "message": "ICU-22363 Add support for QNX 7.1\n\nSigned-off-by: James Choi <chachoi@blackberry.com>",
          "timestamp": "2023-08-10T17:46:01-07:00",
          "tree_id": "a41aa016f03b75259a403b5549008671b4835d54",
          "url": "https://github.com/unicode-org/icu/commit/b70b2d0433d5004f72b5d7590a82879673527ae0"
        },
        "date": 1691715396534,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.13509732951377,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "d91859de21c22ad41c9eda5e11dfa214d58e46fc",
          "message": "ICU-22363 Add copyright notices to new QNX build files",
          "timestamp": "2023-08-11T12:44:28-07:00",
          "tree_id": "63315a338d0fb3df69d723fa71db06b4ca61f594",
          "url": "https://github.com/unicode-org/icu/commit/d91859de21c22ad41c9eda5e11dfa214d58e46fc"
        },
        "date": 1691783986326,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 218.0088353858627,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "6338b704ff9ed73757e11f6917cf835aee7ec0e4",
          "message": "ICU-22452 API for resolving preferred IANA zone ID from a zone ID.",
          "timestamp": "2023-08-11T17:00:05-04:00",
          "tree_id": "8823388b80cb41d8f0b455c058ddd80e059140d3",
          "url": "https://github.com/unicode-org/icu/commit/6338b704ff9ed73757e11f6917cf835aee7ec0e4"
        },
        "date": 1691788259302,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 257.67394784928376,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "81a6edb2870abee08ecbbc7d28407081277e134f",
          "message": "ICU-22404 Unicode 15.1 data 20230811 plus UTS46 fix",
          "timestamp": "2023-08-16T14:25:22-07:00",
          "tree_id": "bb64cbbdf1ae6ba4650b7ebd52a2522930389ba5",
          "url": "https://github.com/unicode-org/icu/commit/81a6edb2870abee08ecbbc7d28407081277e134f"
        },
        "date": 1692222302952,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 225.72530961530532,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "27181e36a6bed372482e5a11c25009fa4dd971f1",
          "message": "ICU-22435 Add C API for Locale \n\nSee #2531",
          "timestamp": "2023-08-17T12:15:47-07:00",
          "tree_id": "df38124fc9221054d4d0427422e9a1842fc06d98",
          "url": "https://github.com/unicode-org/icu/commit/27181e36a6bed372482e5a11c25009fa4dd971f1"
        },
        "date": 1692300759985,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 274.60277275857976,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "ffc449de6243b17857b5d67854bdec723bf06f21",
          "message": "ICU-20777 Merge the likelySubtags implemention\n\nChange testdata/likelySubtags.txt to consider FAIL line\n\nICU-20777 Fix Java Tests\n\nICU-20777 Fix all issues\n\nICU-20777 Incase timeout\n\nICU-20777\n\nICU-20777 Skip Data Driven test",
          "timestamp": "2023-08-18T09:35:54-07:00",
          "tree_id": "c3e30331616dbbed862e21a941a266410240f79e",
          "url": "https://github.com/unicode-org/icu/commit/ffc449de6243b17857b5d67854bdec723bf06f21"
        },
        "date": 1692377627791,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 254.67083826399357,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "a7a2fdbcf257fa22c7a24cfae838675dafd5e071",
          "message": "ICU-22324 Script moving folders to maven structure",
          "timestamp": "2023-08-18T20:58:16-07:00",
          "tree_id": "96a79ac750efc3947bb08a8d52bb81896b783086",
          "url": "https://github.com/unicode-org/icu/commit/a7a2fdbcf257fa22c7a24cfae838675dafd5e071"
        },
        "date": 1692418234580,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 233.88224797441595,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "1fc560c07f34165a56b109b1a3c4be27d345a4ad",
          "message": "ICU-22465 Change .s to .data() for replacing TinyString with CharString.\n\nThis was forgotten by ICU-7496 which replaced the local TinyString data\ntype with the shared CharString data type, but as it's in code heavily\nnested in #ifdef's it hasn't been noticed until now.",
          "timestamp": "2023-08-21T15:57:20+02:00",
          "tree_id": "966093ae5aeb7a7004e6267c7a180025cf1b5425",
          "url": "https://github.com/unicode-org/icu/commit/1fc560c07f34165a56b109b1a3c4be27d345a4ad"
        },
        "date": 1692627110709,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 266.70175381535785,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "248b1c2a13fc7fc346e9e9419c4794df8a590ad9",
          "message": "ICU-22309 update to LICENSE v3, Readme.md, CONTRIBUTING.md\n\n- LICENSE is now the v3 license with the correct year and title\n- README.md now has the required features and drops Terms of Use\n- CONTRIBUTING.md now matches the updated language",
          "timestamp": "2023-08-21T11:48:04-05:00",
          "tree_id": "ebf670f363b6ef1b0628ce3d13b5d3270995cc65",
          "url": "https://github.com/unicode-org/icu/commit/248b1c2a13fc7fc346e9e9419c4794df8a590ad9"
        },
        "date": 1692637839857,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 234.50627173995642,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "52177cc8c78901b2085bc1d8beb0df10c24afadb",
          "message": "ICU-22364 Modify ulocimp_getRegionForSupplementalData() to ignore the subdivision code, rather than requiring it to\nbe \"zzzz\".",
          "timestamp": "2023-08-21T14:06:00-04:00",
          "tree_id": "e29ce619c8e481125566a8a151323b4684b511f8",
          "url": "https://github.com/unicode-org/icu/commit/52177cc8c78901b2085bc1d8beb0df10c24afadb"
        },
        "date": 1692642017964,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 248.47173293081846,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "667ee72b7c2921d28b935af4e620832054452a09",
          "message": "ICU-22457 count() of getUnicodeKeywords is incorrect\n\nThe count() is incorrect if the Locale contains extension which is not -u-\nfor example -x-, -t-.\n\nCurrently, this PR only contains tests to show the problem.\n\nICU-22457 Fix the enum_count",
          "timestamp": "2023-08-21T14:44:10-07:00",
          "tree_id": "f6dec3a2cb7bd8ab9f252016e48591c596b1a679",
          "url": "https://github.com/unicode-org/icu/commit/667ee72b7c2921d28b935af4e620832054452a09"
        },
        "date": 1692654877837,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 217.25629611668606,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "5d6d197a03c100428cd523346ace880ef9e56fe5",
          "message": "ICU-22466 Fix incorrect memory read while the locale is bogus\n\nICU-22466 Fix illegal read\n\nICU-22466 Fix memory issue",
          "timestamp": "2023-08-22T09:22:12-07:00",
          "tree_id": "6d844428e019275808c91ed0d9ae6633ec07eb3a",
          "url": "https://github.com/unicode-org/icu/commit/5d6d197a03c100428cd523346ace880ef9e56fe5"
        },
        "date": 1692721823260,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 219.73085603320592,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@apple.com",
            "name": "Peter Edberg",
            "username": "pedberg"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "2270c174a5839b97a3bab9f9e67db7594778a905",
          "message": "ICU-22325 CLDR release-44-alpha1 to main:\n  - binaries, binary-as-source, CLDR data sources;\n  - CLDR test data & dtd, ICU lib/tool/test source updates.",
          "timestamp": "2023-08-22T14:40:51-07:00",
          "tree_id": "73386c8714dd865d71e0a70c780302b83a4289d4",
          "url": "https://github.com/unicode-org/icu/commit/2270c174a5839b97a3bab9f9e67db7594778a905"
        },
        "date": 1692741464280,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 248.7098133571023,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "fb0f36203affeddff8e1456bce634d996466659e",
          "message": "ICU-22324 Small fixes for script moving folders to maven structure\n\n- Symlinks to LICENSE, so that it is included in the .jar files\n- Fixed version in the jar manifests (s/73/74/)\n- Added Main-Class, Export-Package, some *-Title and *-Description\n\nAt this point the .jar files (including manifests) produced by ant / maven\nare byte to byte identical, except for some small differences that are\nexpected and can be explained (will cover them in the email to the team)",
          "timestamp": "2023-08-22T16:19:43-07:00",
          "tree_id": "7b67f9a79cb3c9fea9b04b72dd0cf90e42ffcf95",
          "url": "https://github.com/unicode-org/icu/commit/fb0f36203affeddff8e1456bce634d996466659e"
        },
        "date": 1692747165790,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 267.2315034423345,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "committer": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "distinct": true,
          "id": "f79fe9347a2db853b1114ab3846ced39c86cc09d",
          "message": "ICU-22332 bidiSkeleton and LTR- and RTL-confusabilities",
          "timestamp": "2023-08-23T15:56:02+02:00",
          "tree_id": "f522c1c6196216eafeed48e5d7c855a327567abf",
          "url": "https://github.com/unicode-org/icu/commit/f79fe9347a2db853b1114ab3846ced39c86cc09d"
        },
        "date": 1692799751956,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 270.18034884857786,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "706044b0a20ce0464a2c13950f1d875203b89a61",
          "message": "ICU-20777 Remove the generation of likelySubtags.res",
          "timestamp": "2023-08-23T15:47:50-07:00",
          "tree_id": "4bdf449b1844eb111fd90762d972312bf6170ab7",
          "url": "https://github.com/unicode-org/icu/commit/706044b0a20ce0464a2c13950f1d875203b89a61"
        },
        "date": 1692831696842,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 263.1873168069966,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "ea0dbd4c4166831278a2004511a1f252d77b983d",
          "message": "ICU-22462 Rename to PersonNameConsistencyTest\n\nRename ExhaustivePersonNameFormatterTest to PersonNameConsistencyTest\nAlways run. Paramaterize the test so each test file are run in their\nown test case and report the failure/success separately",
          "timestamp": "2023-08-24T11:45:27-07:00",
          "tree_id": "6ebfdeda6c30a504e083255f5909305041026c46",
          "url": "https://github.com/unicode-org/icu/commit/ea0dbd4c4166831278a2004511a1f252d77b983d"
        },
        "date": 1692903923199,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 266.8143649552556,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "8817c25c1eac3a0a1b66ac7437e24977e2b93887",
          "message": "ICU-22449 Fixed SimpleDateFormat (in C++ and Java) to correctly honor the rg and hc subtags in the locale when choosing the hour cycle.",
          "timestamp": "2023-08-28T13:30:45-04:00",
          "tree_id": "d259744fa5ac21eacdd19f424a44dc2ec95eb1d6",
          "url": "https://github.com/unicode-org/icu/commit/8817c25c1eac3a0a1b66ac7437e24977e2b93887"
        },
        "date": 1693244839638,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.87846406433565,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "29a6ffc350c7d3a16420e4da375ddb2d618b7637",
          "message": "ICU-22365 call u_terminateChars in ULocale(Builder)?",
          "timestamp": "2023-08-28T15:35:36-07:00",
          "tree_id": "3dc109a223c722afabebf0787f9890671dfc8f9a",
          "url": "https://github.com/unicode-org/icu/commit/29a6ffc350c7d3a16420e4da375ddb2d618b7637"
        },
        "date": 1693262899289,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.73527657266516,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "0cc2d9ed4c98ae34fc4f572f874a75e5ad298623",
          "message": "ICU-22471 Bring ICU4J PersonNameFormatter up to date\n\n- Bring in the latest version of the CLDR PersonNameFormatter test data.\n- Fix PersonNameConsistencyTest to recognize the new “givenFirst” and “surnameFirst” keywords used in the test data files.\n- Fix tokenization code in InitialModifier to use a BreakIterator.  Add unit test.\n- Add support for the “retain” modifier.  Add unit test.\n- Remove the “log known failure” logic for all of the PersonNameConsistencyTest data files that pass now.",
          "timestamp": "2023-08-29T12:54:56-04:00",
          "tree_id": "e70f6cb762bddfbb0016cf2e1e7f93bec7610c66",
          "url": "https://github.com/unicode-org/icu/commit/0cc2d9ed4c98ae34fc4f572f874a75e5ad298623"
        },
        "date": 1693329039524,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.07149842281248,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "2207e2c3dfc4142fecba922c7818a36924ece782",
          "message": "ICU-22324 Exclude tools/build from jdk > 8",
          "timestamp": "2023-08-29T14:35:47-07:00",
          "tree_id": "849fc901661cf510cb8f057045c3b78ada4a5fc5",
          "url": "https://github.com/unicode-org/icu/commit/2207e2c3dfc4142fecba922c7818a36924ece782"
        },
        "date": 1693345535680,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 227.67993605299426,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "02d5e7190305deae8adf71da4e10710f1bc391e7",
          "message": "ICU-22342 Implement ExternalBreakEngineAPI\n\nICU-22342 Fix comments",
          "timestamp": "2023-08-30T11:43:16-07:00",
          "tree_id": "c98cb9466a96fe03f1edd189ac38c39e53783378",
          "url": "https://github.com/unicode-org/icu/commit/02d5e7190305deae8adf71da4e10710f1bc391e7"
        },
        "date": 1693422707399,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 280.14066805335165,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "35bae683a5d35b330a2f9ac5ab244db8c1d281fd",
          "message": "ICU-22475 Fix double free in Locale under OOM\n\nSee #2567",
          "timestamp": "2023-08-30T12:35:15-07:00",
          "tree_id": "2cbec7424603be004316857a509d82ae609ed9ef",
          "url": "https://github.com/unicode-org/icu/commit/35bae683a5d35b330a2f9ac5ab244db8c1d281fd"
        },
        "date": 1693425975649,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 220.97610608226503,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pnacht@google.com",
            "name": "Pedro Kaj Kjellerup Nacht",
            "username": "pnacht"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "95f25839f4b4bd25ceb779f885b0158b8a8ab685",
          "message": "ICU-22423 Add scorecard.yml\n\nSigned-off-by: Pedro Kaj Kjellerup Nacht <pnacht@google.com>",
          "timestamp": "2023-08-30T15:11:50-07:00",
          "tree_id": "679112f772059518c90ed00309b6fbb9695c592b",
          "url": "https://github.com/unicode-org/icu/commit/95f25839f4b4bd25ceb779f885b0158b8a8ab685"
        },
        "date": 1693434327596,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 266.65264406567354,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "d501225db2bfed4fee3049827d6e9076dae2d1ad",
          "message": "ICU-22477 Fix the performance tests after the maven folder changes",
          "timestamp": "2023-08-31T19:38:20-07:00",
          "tree_id": "5f82cab6498d273366e12d6fbe2623b6776e97d0",
          "url": "https://github.com/unicode-org/icu/commit/d501225db2bfed4fee3049827d6e9076dae2d1ad"
        },
        "date": 1693536241873,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 251.80251853422394,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "869713b65fd11bf8bf3c758a4dd75bdaa8d717ca",
          "message": "ICU-22479 Improve collator_compare_fuzzer\n\nSee #2574",
          "timestamp": "2023-09-01T17:57:30-07:00",
          "tree_id": "ad70b7ad728a1fa30f0b23b60b83173862278c5c",
          "url": "https://github.com/unicode-org/icu/commit/869713b65fd11bf8bf3c758a4dd75bdaa8d717ca"
        },
        "date": 1693617015563,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.06042395172162,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "5fb2a6ad06f5fbf621eb04b04687b9cf0f4986c0",
          "message": "ICU-22324 Mavenization, updating the cldr-to-icu scripts and instructions",
          "timestamp": "2023-09-05T10:24:23-07:00",
          "tree_id": "25d6e51fdc281738220c92ef5e23ab5a3491faeb",
          "url": "https://github.com/unicode-org/icu/commit/5fb2a6ad06f5fbf621eb04b04687b9cf0f4986c0"
        },
        "date": 1693935899530,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 295.8410623648257,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "aa70ba6746f5a692c14a8a40ec4588669e8efdc7",
          "message": "ICU-22479 Add a new fuzzer to test more Locale methods\n\nSee #2576",
          "timestamp": "2023-09-05T12:08:39-07:00",
          "tree_id": "839c852baaaf1d9690320375c193b6dd1da95518",
          "url": "https://github.com/unicode-org/icu/commit/aa70ba6746f5a692c14a8a40ec4588669e8efdc7"
        },
        "date": 1693943189365,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 267.555363151811,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "21f74b369898a3d63e01ca4b8590ed630094e657",
          "message": "ICU-22455 Implemented algorithm in CLDR-16981 to preserve regional unit overrides when they don't conflict\nwith the ms subtag.",
          "timestamp": "2023-09-05T16:52:18-04:00",
          "tree_id": "333f5bca04f57f0df757e7b27d0d3fccc53ea99a",
          "url": "https://github.com/unicode-org/icu/commit/21f74b369898a3d63e01ca4b8590ed630094e657"
        },
        "date": 1693947710456,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 239.99837955058308,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "fab5faa3aadf7617df038b372ba21106a4ab1f27",
          "message": "ICU-22479 Limit the collator_compare_fuzzer\n\nTest only first 4K bytes of data, which means compare two\nUnicodeString each with 1024 Unicodes at most.\n\nAvoid finding timeout issue due to large amount of data.",
          "timestamp": "2023-09-05T14:10:33-07:00",
          "tree_id": "3ada1f707a6526ada85c140f9dab17883849a449",
          "url": "https://github.com/unicode-org/icu/commit/fab5faa3aadf7617df038b372ba21106a4ab1f27"
        },
        "date": 1693949385839,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 226.83336062071788,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "c294c3272c9cb18e35e5827cf37f9f5c6e1c8b58",
          "message": "ICU-22365 Add adoptULocale and buildULocale to ULocaleBuilder\n\nAdd two methods related to ULocale to ULocaleBuilder API\n\nvoid ulocbld_adoptULocale(ULocaleBuilder* builder, ULocale* locale);\nULocale* ulocbld_buildULocale(ULocaleBuilder* builder, UErrorCode* err);\n\nICU TC approved this in 2023-08-31",
          "timestamp": "2023-09-05T16:37:39-07:00",
          "tree_id": "f768296b9afca9c608ebcb559cea263d79e01451",
          "url": "https://github.com/unicode-org/icu/commit/c294c3272c9cb18e35e5827cf37f9f5c6e1c8b58"
        },
        "date": 1693957920182,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 225.22842904356156,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "b6a4eb8a654148627a9d92d29489d509acec4675",
          "message": "ICU-22325 Promoted all @draft ICU 72 APIs to @stable ICU 72.",
          "timestamp": "2023-09-06T14:03:05-07:00",
          "tree_id": "e710f1232c1b9ebdb534790ed4fb5d4321d0daaa",
          "url": "https://github.com/unicode-org/icu/commit/b6a4eb8a654148627a9d92d29489d509acec4675"
        },
        "date": 1694035060575,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 277.2232503521448,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "8c7a78f663ac9cf263055ff080c564c81a9f87e3",
          "message": "ICU-22471 Made PersonName.FieldModifier.RETAIN @internal, at least until we find we need it in the API.",
          "timestamp": "2023-09-06T14:32:14-07:00",
          "tree_id": "c4434445e44dc67e4076bd594133831b6f814dfd",
          "url": "https://github.com/unicode-org/icu/commit/8c7a78f663ac9cf263055ff080c564c81a9f87e3"
        },
        "date": 1694037089225,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.74662005183157,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ssb22@cam.ac.uk",
            "name": "Silas S. Brown",
            "username": "ssb22"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "7ae7b156983474a52d99bb5c579106a99d02efb1",
          "message": "ICU-22323 update comment: it is now only two word lists, not three",
          "timestamp": "2023-09-07T21:34:25-04:00",
          "tree_id": "c7b2d23767d3ac3110c4458085fe8ebae6277611",
          "url": "https://github.com/unicode-org/icu/commit/7ae7b156983474a52d99bb5c579106a99d02efb1"
        },
        "date": 1694137304584,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 223.35920011946885,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "612cfbbfe424f441a6dc940632cdf55de0081d2d",
          "message": "ICU-22325 BRS 74rc update rename.h #1",
          "timestamp": "2023-09-07T22:21:55-07:00",
          "tree_id": "784b25c2e4fe7a9be928bff34bd30678f736cb20",
          "url": "https://github.com/unicode-org/icu/commit/612cfbbfe424f441a6dc940632cdf55de0081d2d"
        },
        "date": 1694151427777,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 224.6775006719955,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "916452befcc002c8fa7111ee889a7002d9a9148d",
          "message": "ICU-22169 Fixed ures_getFunctionalEquivalent() to check the resource bundles' %%Parent resources when ascending\nthe parent tree.",
          "timestamp": "2023-09-11T12:58:41-07:00",
          "tree_id": "3a11a05ee832be1548b752afa833aecc65bef67a",
          "url": "https://github.com/unicode-org/icu/commit/916452befcc002c8fa7111ee889a7002d9a9148d"
        },
        "date": 1694462953643,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 268.60490584205996,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "687feb1eaa9cf1dec0dbf780f37fad570c435d1a",
          "message": "ICU-22478 Fix log spew in PersonNameConsistencyTest.",
          "timestamp": "2023-09-11T13:34:31-07:00",
          "tree_id": "b1c4d92d609077b333f6b03f4df7efa4038d6f4e",
          "url": "https://github.com/unicode-org/icu/commit/687feb1eaa9cf1dec0dbf780f37fad570c435d1a"
        },
        "date": 1694465417182,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 298.9900624928265,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "35645abdcb5ad7ffa9bb4fba9debff05d300c29d",
          "message": "ICU-22494 Avoid adding empty or duplicate variants during locale canoncalization.\n\nIt change the failure case (see the bug) from 35s to 0.126s on a very\nfast developement machine.",
          "timestamp": "2023-09-11T15:25:37-07:00",
          "tree_id": "31113b191914a2519e1ce4ebe4d13044158adeef",
          "url": "https://github.com/unicode-org/icu/commit/35645abdcb5ad7ffa9bb4fba9debff05d300c29d"
        },
        "date": 1694471828416,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 250.47771038940425,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "68a61daa959c03f4c624d73802d98730c35ad3da",
          "message": "ICU-22497 Fix buffer-overflow READ for toLanguateTag",
          "timestamp": "2023-09-12T15:21:26-07:00",
          "tree_id": "a8f6062fcab29dc7434a37170ebc8650f83ae659",
          "url": "https://github.com/unicode-org/icu/commit/68a61daa959c03f4c624d73802d98730c35ad3da"
        },
        "date": 1694558607473,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 224.31804336666372,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "6d178fd0026ea5c996d0621780412a16b8ee1780",
          "message": "ICU-22325 Update ICU4C API Change Reports - frontload\nChange version on uspoof.h from \"74.0\" to \"74\"",
          "timestamp": "2023-09-12T16:15:11-07:00",
          "tree_id": "66dd53b09b29df73ae462426faf623b8e61885e0",
          "url": "https://github.com/unicode-org/icu/commit/6d178fd0026ea5c996d0621780412a16b8ee1780"
        },
        "date": 1694561545194,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 249.25670101768006,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dragan@unicode.org",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "committer": {
            "email": "131725218+DraganBesevic@users.noreply.github.com",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "distinct": true,
          "id": "bb7352990e01fe6e484fe98a3edd1bdd3f3499b4",
          "message": "ICU-22325 CLDR 44 alpha2 integration to ICU part three, source files changes",
          "timestamp": "2023-09-13T11:06:53-07:00",
          "tree_id": "c693fd6e9eab9ca8c36ccd1362c5dcb8bdca4f52",
          "url": "https://github.com/unicode-org/icu/commit/bb7352990e01fe6e484fe98a3edd1bdd3f3499b4"
        },
        "date": 1694628737859,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.60000767107124,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "9fb9bd4950091c518b76c6cbfe0120e7953f4956",
          "message": "ICU-22342 Rename fillBreak to fillBreaks",
          "timestamp": "2023-09-14T10:04:57-07:00",
          "tree_id": "3fa630db6f9c23629fb2f2d93349b7f73affe757",
          "url": "https://github.com/unicode-org/icu/commit/9fb9bd4950091c518b76c6cbfe0120e7953f4956"
        },
        "date": 1694712993571,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 305.7984710873834,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "9e9bc3695fe4c1ad32d6d1d48ab878b2d5d4fde5",
          "message": "ICU-22489 Clarify the default setting of Collator\n\nSee #2595",
          "timestamp": "2023-09-14T10:05:36-07:00",
          "tree_id": "57a0436eb589fe45f13a02b60eac8dbf960a587e",
          "url": "https://github.com/unicode-org/icu/commit/9e9bc3695fe4c1ad32d6d1d48ab878b2d5d4fde5"
        },
        "date": 1694714073350,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 220.15875702516556,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "832997c57e7ad4a128447d8c90ee338d90a0faf3",
          "message": "ICU-22325 Disable tests as workaround for failures following CI changes\n\nSee #2601",
          "timestamp": "2023-09-15T09:46:23-07:00",
          "tree_id": "a48e6c696f2019b5ed215dd5f721c75b7c779ff5",
          "url": "https://github.com/unicode-org/icu/commit/832997c57e7ad4a128447d8c90ee338d90a0faf3"
        },
        "date": 1694797149201,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 222.17457317400886,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "386e9a10db28af620658ca0c635e1d98915f6306",
          "message": "ICU-22504 Fix buffer overflow write error",
          "timestamp": "2023-09-15T11:01:37-07:00",
          "tree_id": "dd8900ffcfa28b0b45dcf3243ee9c3512c13efdd",
          "url": "https://github.com/unicode-org/icu/commit/386e9a10db28af620658ca0c635e1d98915f6306"
        },
        "date": 1694801715768,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 232.89258945130183,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "84ae742ea01565054a4cccd7fbaed51753ecd80d",
          "message": "ICU-22479 Add fuzzer for Calendar API",
          "timestamp": "2023-09-15T11:02:55-07:00",
          "tree_id": "be03255559302f5652a521bd2017d39621a510a9",
          "url": "https://github.com/unicode-org/icu/commit/84ae742ea01565054a4cccd7fbaed51753ecd80d"
        },
        "date": 1694802551852,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 227.9854866922622,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "28572ab67eeedc6e49e15fdf7bb96e7392193e55",
          "message": "ICU-22325 CLDR release-44-alpha3 to main part 3 (ICU sources: lib, tools, tests)",
          "timestamp": "2023-09-15T14:02:20-07:00",
          "tree_id": "6e3dba62b142d0816cffd49e48861e541ef10281",
          "url": "https://github.com/unicode-org/icu/commit/28572ab67eeedc6e49e15fdf7bb96e7392193e55"
        },
        "date": 1694812561106,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 251.72231422996717,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "71a483d174958a6a9102e8bef8382eae60c2f58b",
          "message": "ICU-22495 Added support for \"genitive\" and \"vocative\" field modifiers; cleaned up PersonNameConsistencyTest. Fixed\nbug in implementation of \"retain\" keyword.",
          "timestamp": "2023-09-15T15:04:01-07:00",
          "tree_id": "1409b992a87ed5b75d90eb061e19e2bf159405c0",
          "url": "https://github.com/unicode-org/icu/commit/71a483d174958a6a9102e8bef8382eae60c2f58b"
        },
        "date": 1694815791031,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 307.17739055832794,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "14ca2b0e6da14ef45aadf5067deb562fc6c70a52",
          "message": "ICU-22313 Various fixes for duration formatting:\n- Changed the C++ and Java interfaces to that the URBNF_DURATION ruleset is marked deprecated.\n- Fixed a bug in RuleBasedNumberFormat in both Java and C++ that caused the existing duration-formatting rules to produce bogus results when used on a non-integral value.\n\n(Earlier versions of this PR added code to use a MeasureFormat under the covers when a caller used\nunum_open(UNUM_DURATION).  I took that out because of backward compatibility concerns, so we're still using RBNF\nin the C API.  I'm hoping to add a \"real\" duration formatter in ICU 75.)",
          "timestamp": "2023-09-15T16:27:58-07:00",
          "tree_id": "49d55842d905b80745e303e46f0edca62c774223",
          "url": "https://github.com/unicode-org/icu/commit/14ca2b0e6da14ef45aadf5067deb562fc6c70a52"
        },
        "date": 1694821186054,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 223.0522555331401,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "d082de574ff13873123c55212ad20cf258ffb283",
          "message": "ICU-22479 Add Fuzzer for DateFormat",
          "timestamp": "2023-09-15T17:47:17-07:00",
          "tree_id": "7c0cf52cd0941fb7b95d52c57bcbf88b98b19c5f",
          "url": "https://github.com/unicode-org/icu/commit/d082de574ff13873123c55212ad20cf258ffb283"
        },
        "date": 1694827464963,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 319.4372074120406,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "d8659b476d4703b18bb9ea040798c1e62ff3329e",
          "message": "ICU-22404 new properties IDS_Unary_Operator, ID_Compat_Math_*, NFKC_SCF",
          "timestamp": "2023-09-16T14:41:51-07:00",
          "tree_id": "56f8034d279d1b6ce890ccbb147a36bc1ecb9bc8",
          "url": "https://github.com/unicode-org/icu/commit/d8659b476d4703b18bb9ea040798c1e62ff3329e"
        },
        "date": 1694901171020,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.2427036180315,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "f3b869cbb0b9ced42d7ca4e24626a868a14ddcfc",
          "message": "ICU-22512 Fix broken TestHebrewCalendarInTemporalLeapYear\n\nFix broken test mistakenly landed in\nhttps://github.com/unicode-org/icu/pull/2274\n\nSome important steps were missed in the last landing.",
          "timestamp": "2023-09-19T09:47:03-07:00",
          "tree_id": "afc0a65feefd9db03da5d6f6d355803f8e103d54",
          "url": "https://github.com/unicode-org/icu/commit/f3b869cbb0b9ced42d7ca4e24626a868a14ddcfc"
        },
        "date": 1695143098043,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 301.7634458723958,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "6df1babae26f363d3140af7c50a9956e8f80a12c",
          "message": "ICU-22479 Add Fuzzer for ListFormat",
          "timestamp": "2023-09-19T15:52:42-07:00",
          "tree_id": "6a18cb16063677dbf64588f91737bd90c4bde94a",
          "url": "https://github.com/unicode-org/icu/commit/6df1babae26f363d3140af7c50a9956e8f80a12c"
        },
        "date": 1695164706344,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 225.8618330107929,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "990779c4bf935d2741ef6514f4d070231c388d05",
          "message": "ICU-22509 Limit the dateStyle and timeStyle in fuzzer to only valid enum",
          "timestamp": "2023-09-19T15:54:04-07:00",
          "tree_id": "e95a6e47e94ba513f1f90645a8b2da0058d6d92a",
          "url": "https://github.com/unicode-org/icu/commit/990779c4bf935d2741ef6514f4d070231c388d05"
        },
        "date": 1695165600636,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 258.66941897559076,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "c4a596e18e008139e3be91043247ab8d01bd6b58",
          "message": "ICU-22508 Reject NaN as input to Calendar::setTime\n\nFix DateFormat::format issue",
          "timestamp": "2023-09-19T15:55:02-07:00",
          "tree_id": "68c6ded527d29408fb82ffc68617dc45f1bf3170",
          "url": "https://github.com/unicode-org/icu/commit/c4a596e18e008139e3be91043247ab8d01bd6b58"
        },
        "date": 1695166719462,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 313.1760267400784,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "2f7090256abe90d58c8d7c7f9533ea52f25bbe1a",
          "message": "ICU-22502 Fix TestPersonNames failure on Windows",
          "timestamp": "2023-09-19T22:54:46-04:00",
          "tree_id": "15a6d3465f64a16f847b39d688ee9da85aa9cbfd",
          "url": "https://github.com/unicode-org/icu/commit/2f7090256abe90d58c8d7c7f9533ea52f25bbe1a"
        },
        "date": 1695179108463,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 289.6253101558905,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "9be9ed3231f6cf79190128af6eee6dfc2e6b573c",
          "message": "ICU-22324 Mavenization, resolve some of the release tasks",
          "timestamp": "2023-09-20T10:27:12-07:00",
          "tree_id": "e38d8cf616c6007928b0021a3c414811f441cee8",
          "url": "https://github.com/unicode-org/icu/commit/9be9ed3231f6cf79190128af6eee6dfc2e6b573c"
        },
        "date": 1695231543073,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.52639424895744,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "f4227f5d544e9fe4b9883639f1915399f9c7c9df",
          "message": "ICU-22324 Include all unit tests, move tests needing multiple components",
          "timestamp": "2023-09-20T10:51:19-07:00",
          "tree_id": "3ce41cc9aa236f66cebe0e042da34ef11dfd4745",
          "url": "https://github.com/unicode-org/icu/commit/f4227f5d544e9fe4b9883639f1915399f9c7c9df"
        },
        "date": 1695233669026,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 258.99367699992007,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "87d606c39ac1d89c380bce6cce568fa14011a7d1",
          "message": "ICU-22509 Fix the RelativeDateFormat to reject unsupported dateStyle",
          "timestamp": "2023-09-20T12:02:57-07:00",
          "tree_id": "0952a4a3bbad09ab9868af1f8ef8bb40ed4242c6",
          "url": "https://github.com/unicode-org/icu/commit/87d606c39ac1d89c380bce6cce568fa14011a7d1"
        },
        "date": 1695239064905,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.85543779678065,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "2ca7c49662cfa0455c211f310998302a2e0d64d9",
          "message": "ICU-22464 Fix Java DateFormatSymbols to copy abbreviated day period names to the other sizes when appropriate.",
          "timestamp": "2023-09-20T13:46:00-07:00",
          "tree_id": "4a794fa8d1c86e664b4256c938f9df1d394bb9a2",
          "url": "https://github.com/unicode-org/icu/commit/2ca7c49662cfa0455c211f310998302a2e0d64d9"
        },
        "date": 1695243490656,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 221.5405217555849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "6a42197331c23ed7e09d040f6deaa7c352c02cca",
          "message": "ICU-22510 Fix Calendar::set to check the field is valid.",
          "timestamp": "2023-09-20T13:50:56-07:00",
          "tree_id": "d9e1f7b63caf181062360853a456a6662a56a0c6",
          "url": "https://github.com/unicode-org/icu/commit/6a42197331c23ed7e09d040f6deaa7c352c02cca"
        },
        "date": 1695245417449,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 233.33441599241877,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "1b980e5999f53ac562775f22a6f5072170072c43",
          "message": "ICU-21877 Fixed it so that getAllChildrenWithFallback() correctly calls its sink with every possible resource the\none the user requested might be inheriting elements from.",
          "timestamp": "2023-09-20T14:09:21-07:00",
          "tree_id": "c3a2af18a4835b7385aa1983941221737bf89239",
          "url": "https://github.com/unicode-org/icu/commit/1b980e5999f53ac562775f22a6f5072170072c43"
        },
        "date": 1695246783180,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 217.67352209605514,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "4fcf8d22b9e547a8ca115742c50e880440f599ba",
          "message": "ICU-22507 Fix stack overflow in ChineseCalendar::isLeapMonthBetween\n\nRewrite the recursive call to while loop to avoid stack overflow\nwhen the two values have big gap.\nInclude tests to verify the problem in unit test.",
          "timestamp": "2023-09-20T23:56:15-07:00",
          "tree_id": "dc89729b42b8289f8b2b694145f0424471f1b300",
          "url": "https://github.com/unicode-org/icu/commit/4fcf8d22b9e547a8ca115742c50e880440f599ba"
        },
        "date": 1695279782493,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 225.3081719523805,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "f14b24a845f7ff36519bdee5f64181f7ee389091",
          "message": "ICU-22324 Allow single Maven cache creation and multiple read-only usage",
          "timestamp": "2023-09-21T08:39:05-07:00",
          "tree_id": "d7e6f90de38a03010299d5dc678ac5f161bfb68a",
          "url": "https://github.com/unicode-org/icu/commit/f14b24a845f7ff36519bdee5f64181f7ee389091"
        },
        "date": 1695311323539,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 232.83559954532817,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "263db44a3ae848a7364d3fefc6af0b6b0827ba9f",
          "message": "ICU-22325 BRS#19 ICU4J 74 Status and Change Report\nChange FormattedNumber.getNounClass to @stable ICU 72",
          "timestamp": "2023-09-21T13:21:20-07:00",
          "tree_id": "4aab45e6a43d3dd241c5435cfb9272f534ced3d6",
          "url": "https://github.com/unicode-org/icu/commit/263db44a3ae848a7364d3fefc6af0b6b0827ba9f"
        },
        "date": 1695328320641,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.87325583472835,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "77bc51e03e6de0a92ef063da85903ddadce97043",
          "message": "ICU-22479 Add fuzzer for RelativeTimeFormatFuzzer\n\nAnd also fix a bug by checking the unit",
          "timestamp": "2023-09-21T14:50:35-07:00",
          "tree_id": "fe32fa4aa78adcf2f9f1fd708a09f787b6098b61",
          "url": "https://github.com/unicode-org/icu/commit/77bc51e03e6de0a92ef063da85903ddadce97043"
        },
        "date": 1695334014945,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.19968734079058,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "c3544278cbecdc561d926cb0bd6b3a3cfa28a61d",
          "message": "ICU-22325 BRS#13: urename.h",
          "timestamp": "2023-09-21T16:45:25-07:00",
          "tree_id": "cccde14430d2efeeb80421467a496cb712145417",
          "url": "https://github.com/unicode-org/icu/commit/c3544278cbecdc561d926cb0bd6b3a3cfa28a61d"
        },
        "date": 1695345446696,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 248.42105263157896,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "501fe1b74a6027709cb72e6b58fba189db541fd6",
          "message": "ICU-22324 Mavenization, preparing for maven central deployment",
          "timestamp": "2023-09-21T18:21:06-07:00",
          "tree_id": "b1e35c68e2dc4f7cab75f20b41f2c38a0f4edd8b",
          "url": "https://github.com/unicode-org/icu/commit/501fe1b74a6027709cb72e6b58fba189db541fd6"
        },
        "date": 1695346918137,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.36097228240519,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "cdcea0c3787dcb47fbc6741221145712ffeb07ac",
          "message": "ICU-22479 Enahnce the fuzzer for ListFormatter\n\nAdd the testing for invalid type and width.\nAlso test formatStringsToValue",
          "timestamp": "2023-09-21T21:21:21-07:00",
          "tree_id": "f82d15eeec7a049a784c41dbc07ecda670564efb",
          "url": "https://github.com/unicode-org/icu/commit/cdcea0c3787dcb47fbc6741221145712ffeb07ac"
        },
        "date": 1695357552705,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 319.2716104199519,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "6e0468d3bfa2f6b795eee09e286bf3cf2193495e",
          "message": "ICU-22498 Write directly to sink (instead of into legacy buffers).",
          "timestamp": "2023-09-22T15:01:42+02:00",
          "tree_id": "e874aa4d7f505738a032ce26bc22e7ec74caa328",
          "url": "https://github.com/unicode-org/icu/commit/6e0468d3bfa2f6b795eee09e286bf3cf2193495e"
        },
        "date": 1695388467920,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 272.28782313643325,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "8c4af504a0b36a21d098bc2dc3880c39c549c3db",
          "message": "ICU-22324 Mavenization, minimize the places that need a version",
          "timestamp": "2023-09-22T13:08:25-07:00",
          "tree_id": "65ca19624039aae16f103c2f2fa8ecc1e7a12617",
          "url": "https://github.com/unicode-org/icu/commit/8c4af504a0b36a21d098bc2dc3880c39c549c3db"
        },
        "date": 1695413973692,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 245.6896958862563,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "committer": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "distinct": true,
          "id": "c4689841c001a0b98e5b59adaab34656bd2a998f",
          "message": "ICU-22463 Fix the conversion from gasoline-equivalent units to kilograms-per-meter-squared-per-second\n\nSee #2616",
          "timestamp": "2023-09-23T06:48:25+02:00",
          "tree_id": "336d1d0d2a8eb26799aff8af2dd936b63e5ae43d",
          "url": "https://github.com/unicode-org/icu/commit/c4689841c001a0b98e5b59adaab34656bd2a998f"
        },
        "date": 1695445253167,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 244.33580759601688,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "6d1999fbb9fd8dbca901b692cda6acb147982673",
          "message": "ICU-21289 Switch to using CharString for calling uloc_getKeywordValue().",
          "timestamp": "2023-09-25T19:02:41+02:00",
          "tree_id": "0f4fa991e436494e54f2ee7cfa1a8f347f15d8c1",
          "url": "https://github.com/unicode-org/icu/commit/6d1999fbb9fd8dbca901b692cda6acb147982673"
        },
        "date": 1695662141087,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 232.20487535289766,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "c1475f4bbdf230bd2ed277ad97d667869291e58b",
          "message": "ICU-22516 Return error while the style is invalid\n\nTo avoid later unknown address in icu_74::UnicodeString::copyFrom",
          "timestamp": "2023-09-25T15:43:49-07:00",
          "tree_id": "0e1409de3b8bbf02ada2ac7210546d7a42cf04f8",
          "url": "https://github.com/unicode-org/icu/commit/c1475f4bbdf230bd2ed277ad97d667869291e58b"
        },
        "date": 1695682494320,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.5689193615017,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "2df1ab724040dc9cb25a4df7f0244cfe79ffef62",
          "message": "ICU-21289 Switch to using CharString for calling uloc_canonicalize().",
          "timestamp": "2023-09-26T00:52:13+02:00",
          "tree_id": "829501061a49bd823f23b5b1b63e368989891653",
          "url": "https://github.com/unicode-org/icu/commit/2df1ab724040dc9cb25a4df7f0244cfe79ffef62"
        },
        "date": 1695684366702,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 250.20309801271154,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "9bce52f003496527b2e37e31ae08521cd35212ac",
          "message": "ICU-21289 Switch to using CharString for calling uloc_toLanguageTag().",
          "timestamp": "2023-09-26T13:26:11+02:00",
          "tree_id": "6946ee770cbe8066d20610ca6cbfa72cf94cab1d",
          "url": "https://github.com/unicode-org/icu/commit/9bce52f003496527b2e37e31ae08521cd35212ac"
        },
        "date": 1695728279573,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 225.23486495679745,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "99026f01cc79bdd25f2ed7e5dabd44ce6026e427",
          "message": "ICU-22325 BRS74RC: Update version number to 74.1\n\nSee #2613",
          "timestamp": "2023-09-26T08:21:21-07:00",
          "tree_id": "92ade653ecaf31bab4eda9eac23f2699a7ae2fc9",
          "url": "https://github.com/unicode-org/icu/commit/99026f01cc79bdd25f2ed7e5dabd44ce6026e427"
        },
        "date": 1695742568037,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 221.80295376329406,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "96dcaf7da81f0676519946f601117b9825ba0d06",
          "message": "ICU-21289 Switch to using CharString for calling uloc_forLanguageTag().",
          "timestamp": "2023-09-26T17:52:51+02:00",
          "tree_id": "ddccfb24540afd8a7df5ae389869d3c89d75546a",
          "url": "https://github.com/unicode-org/icu/commit/96dcaf7da81f0676519946f601117b9825ba0d06"
        },
        "date": 1695744273961,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 251.15502796024202,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "037449fff8db873afdd2e3c6ed5d24db604ffe64",
          "message": "ICU-21289 Switch to using CharString for calling uloc_getParent().",
          "timestamp": "2023-09-26T23:41:24+02:00",
          "tree_id": "dae404fab21a5ef1a9671ff79ca87baaea872d16",
          "url": "https://github.com/unicode-org/icu/commit/037449fff8db873afdd2e3c6ed5d24db604ffe64"
        },
        "date": 1695765094811,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 230.15266648813113,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "87fe057838515e660be4cbdf5b735d5ecdd4c4e1",
          "message": "ICU-22324 Mavenization, add -SNAPSHOT back to the maven version",
          "timestamp": "2023-09-27T06:46:31-07:00",
          "tree_id": "50a88d129978e323dcef0ba35a1ef7eba23022b2",
          "url": "https://github.com/unicode-org/icu/commit/87fe057838515e660be4cbdf5b735d5ecdd4c4e1"
        },
        "date": 1695823204764,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 235.22098228119464,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "c670bbd5b099da2e24acb04a0a57704e158b5f96",
          "message": "ICU-22420 GB18030 change 3 mappings for GBK/web compat",
          "timestamp": "2023-09-27T08:37:24-07:00",
          "tree_id": "dba05c65b2bd66fb02b4011fc2ecb084c4bb7e52",
          "url": "https://github.com/unicode-org/icu/commit/c670bbd5b099da2e24acb04a0a57704e158b5f96"
        },
        "date": 1695829857820,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 229.62988431778766,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "7a0373411ea44e682565f91c6dfd1aa00135b6c0",
          "message": "ICU-22324 Mavenization, building the CLDR utilities",
          "timestamp": "2023-09-27T11:01:06-07:00",
          "tree_id": "00c765052b5dc1249d0645baa37e7b3b9c8ea8df",
          "url": "https://github.com/unicode-org/icu/commit/7a0373411ea44e682565f91c6dfd1aa00135b6c0"
        },
        "date": 1695838323715,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 340.59058017562876,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "aa377d7366231b244a0b6288be5378bef8280dbe",
          "message": "ICU-22324 Mavenization, script to generate serial test data",
          "timestamp": "2023-09-27T16:11:39-07:00",
          "tree_id": "76652cec81f6d2fce31258130f4f67a10400ccce",
          "url": "https://github.com/unicode-org/icu/commit/aa377d7366231b244a0b6288be5378bef8280dbe"
        },
        "date": 1695858080467,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 265.9279675004921,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "committer": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "distinct": true,
          "id": "9ed8532d2dd25afd15841a80aac010a6c3d0638e",
          "message": "ICU-22332 document advanced usage of bidi confusability\n\nCo-authored-by: Markus Scherer <markus.icu@gmail.com>",
          "timestamp": "2023-09-28T02:22:05+02:00",
          "tree_id": "4c87988086e4b6959a97f1346437736843dd2da9",
          "url": "https://github.com/unicode-org/icu/commit/9ed8532d2dd25afd15841a80aac010a6c3d0638e"
        },
        "date": 1695861289119,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 251.02918586789554,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "bfbf578f1c83fbda60d68ef2739f381ad6c61866",
          "message": "ICU-22325 docmain ICU 74 new C services",
          "timestamp": "2023-09-27T17:55:37-07:00",
          "tree_id": "b17718641046e4a76a94c8410ac51a2ec632ac4d",
          "url": "https://github.com/unicode-org/icu/commit/bfbf578f1c83fbda60d68ef2739f381ad6c61866"
        },
        "date": 1695863290518,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 285.6139824061383,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "acfe1c299b010026b98f315eb3960f19086590c1",
          "message": "ICU-22325 Update double-conversion to v3.3.0",
          "timestamp": "2023-09-28T14:31:16+02:00",
          "tree_id": "333ec8533860e613066e4a98f8bbd5116376dd53",
          "url": "https://github.com/unicode-org/icu/commit/acfe1c299b010026b98f315eb3960f19086590c1"
        },
        "date": 1695905074480,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 245.88512477230955,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "07137b64e454dfda7ab0c46eaaa55d6bfc29e399",
          "message": "ICU-22521 Return U_INTERNAL_PROGRAM_ERROR instead of % 0\n\nWhen the gap is 0, return status as U_INTERNAL_PROGRAM_ERROR\nand avoid the operation of \"% gap\"",
          "timestamp": "2023-09-28T14:23:07-07:00",
          "tree_id": "b5dfc3acc475ba088d23eb5f55b3be8500c8ae92",
          "url": "https://github.com/unicode-org/icu/commit/07137b64e454dfda7ab0c46eaaa55d6bfc29e399"
        },
        "date": 1695936896983,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 218.3513602709083,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "76b9e089be35dbcbd374111ad9eadccd0d8907bb",
          "message": "ICU-22324 Mavenization, publish the root pom to Maven",
          "timestamp": "2023-09-28T17:32:30-07:00",
          "tree_id": "589208cb6a7823818d2f3a1816b1242246266144",
          "url": "https://github.com/unicode-org/icu/commit/76b9e089be35dbcbd374111ad9eadccd0d8907bb"
        },
        "date": 1695948373931,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 240.36052507777842,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dragan@unicode.org",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "committer": {
            "email": "131725218+DraganBesevic@users.noreply.github.com",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "distinct": true,
          "id": "38643e14e50a1f0679ea7de8e863b8b7f64d0354",
          "message": "ICU-22325 CLDR 44 beta1 integration to ICU part two, source files generated or copied from CLDR",
          "timestamp": "2023-09-29T20:29:49-07:00",
          "tree_id": "956f9614a307d72908fe603565e2060626d806e4",
          "url": "https://github.com/unicode-org/icu/commit/38643e14e50a1f0679ea7de8e863b8b7f64d0354"
        },
        "date": 1696045377459,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 253.0550283069033,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "atnbueno@gmail.com",
            "name": "Antonio Bueno",
            "username": "atnbueno"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "f24e0a0ff36ae214415898917f17290bd4211c38",
          "message": "ICU-22323 Describe date field symbol `ee` as `02`, not `2`\n\nSee #2636",
          "timestamp": "2023-10-02T12:29:59-07:00",
          "tree_id": "d4e8e4129146536afb4b188917f54aecc86e97f9",
          "url": "https://github.com/unicode-org/icu/commit/f24e0a0ff36ae214415898917f17290bd4211c38"
        },
        "date": 1696275834777,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 260.00865776361377,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "3446660d27d71490999872271e60d06854747d60",
          "message": "ICU-22325 BRS74 Clean up import statements",
          "timestamp": "2023-10-02T17:22:26-04:00",
          "tree_id": "4d5745100d0a7c74083a9cfe1a3e408ce3f57f3c",
          "url": "https://github.com/unicode-org/icu/commit/3446660d27d71490999872271e60d06854747d60"
        },
        "date": 1696283723529,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.92671269562993,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "e73d9736c9cd9fb3a7ab45b3a51a98dec3d6b5bc",
          "message": "ICU-22325 Update docs.md to change `ant` commands to new scripts",
          "timestamp": "2023-10-02T16:04:02-07:00",
          "tree_id": "eed108fd5609a081e068ce26859ed797dba28b6b",
          "url": "https://github.com/unicode-org/icu/commit/e73d9736c9cd9fb3a7ab45b3a51a98dec3d6b5bc"
        },
        "date": 1696288830705,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.85652701353456,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "f6d09d514dbe09366265dbc5381a7e2eddb577c6",
          "message": "ICU-22527 add Measure::operator!=()",
          "timestamp": "2023-10-02T17:03:28-07:00",
          "tree_id": "866a0b99be396c014ff865082ffa6b129da2c3bb",
          "url": "https://github.com/unicode-org/icu/commit/f6d09d514dbe09366265dbc5381a7e2eddb577c6"
        },
        "date": 1696293083760,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 233.42311213956737,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "05b0e7abaf1e46c7d66c46ee997a0bf9eff7e36a",
          "message": "ICU-22517 Limit the closure expansion loop and return error\n\nTo avoid very slow return from the constructor, we return\nerror while the Collation rule expand too big.\nAdd a soft limit to limit to the number of loop needed for 8 Hanguls\n  Necessary number of loop: H(0)=0; H(i)=3H(i-1)+2.\n  Where i is the length of Hangul in the rule.\n  H(1) = 2, H(2) = 8, H(3)=26, H(4)=80, H(5) = 242 ...",
          "timestamp": "2023-10-02T19:06:38-07:00",
          "tree_id": "da708510595a1a6f0cc9512e6b55e6fc23011f8b",
          "url": "https://github.com/unicode-org/icu/commit/05b0e7abaf1e46c7d66c46ee997a0bf9eff7e36a"
        },
        "date": 1696299481396,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 252.45799397856607,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "dff59b61f9e0fc240add56e5ededc446ea7cb8b9",
          "message": "ICU-22325 ICU4C update APIChangeReport 73 -> 74",
          "timestamp": "2023-10-03T07:21:20-07:00",
          "tree_id": "428750079b7680e284db954289a7c99acc73ff84",
          "url": "https://github.com/unicode-org/icu/commit/dff59b61f9e0fc240add56e5ededc446ea7cb8b9"
        },
        "date": 1696343984450,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 253.05613092647394,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "e1af930c6ad54100767c21a883f88f529336c4ad",
          "message": "ICU-22325 BRS 74rc move cldr testdata to consistent place, adjust test & tools to match",
          "timestamp": "2023-10-03T10:24:27-07:00",
          "tree_id": "8831e6e445ed11d32938335e76c577dafbd6999b",
          "url": "https://github.com/unicode-org/icu/commit/e1af930c6ad54100767c21a883f88f529336c4ad"
        },
        "date": 1696354587831,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 252.89544198213895,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "e6f7ef7ef41882ae87d4608b3c5e27fefb5d2a8f",
          "message": "ICU-22324 Mavenization, remove maven proof of concept and ant script",
          "timestamp": "2023-10-03T10:58:25-07:00",
          "tree_id": "0da6bd5e956eaed17379ad557f0078abe3ed776f",
          "url": "https://github.com/unicode-org/icu/commit/e6f7ef7ef41882ae87d4608b3c5e27fefb5d2a8f"
        },
        "date": 1696356467212,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 250.9594942811506,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "3cc4e1aac692f3c23c4e06c62c2a9c7102e4dafb",
          "message": "ICU-22325 BRS74 Updating ICU4J serialization test data",
          "timestamp": "2023-10-03T15:20:03-04:00",
          "tree_id": "f47598b1ff9adb8fbf0696284f910fef9d3b1b3a",
          "url": "https://github.com/unicode-org/icu/commit/3cc4e1aac692f3c23c4e06c62c2a9c7102e4dafb"
        },
        "date": 1696361553691,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 307.62492123387887,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "e09adbf05a136cc2e59159300d74e0ec81401e41",
          "message": "ICU-22324 Mavenization: removed license links (error on Windows)\n\nSee #2652",
          "timestamp": "2023-10-03T12:47:48-07:00",
          "tree_id": "5a0d192b434f566dfd30a84be0f7faead76e146a",
          "url": "https://github.com/unicode-org/icu/commit/e09adbf05a136cc2e59159300d74e0ec81401e41"
        },
        "date": 1696364057051,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 230.91913607304562,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "fc386c3a9a7b7bc68a746e51319388eafcaf2c2b",
          "message": "ICU-22149 Be more graceful with missing lang bundle data\n\nSee #2635",
          "timestamp": "2023-10-03T14:20:26-07:00",
          "tree_id": "f852d38a7ca70692df4baee0de32bbaba84a59a6",
          "url": "https://github.com/unicode-org/icu/commit/fc386c3a9a7b7bc68a746e51319388eafcaf2c2b"
        },
        "date": 1696369326533,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.40911087792244,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "c58706989552debfb3a555664b01553664518855",
          "message": "ICU-22527 Add -Wambiguous-reversed-operator test code.",
          "timestamp": "2023-10-04T00:05:31+02:00",
          "tree_id": "4aaaa92f5ccab10a66d03ef7988a035c43ad3f9c",
          "url": "https://github.com/unicode-org/icu/commit/c58706989552debfb3a555664b01553664518855"
        },
        "date": 1696371491097,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.88252699634592,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "1651e63b338a947b5979a219c5ecaa1e38e5fea5",
          "message": "ICU-22325 BRS74 ICU4J API signature file",
          "timestamp": "2023-10-04T12:11:07-04:00",
          "tree_id": "bf91f0563fb1c4520317c6f4024a639f9c6117f9",
          "url": "https://github.com/unicode-org/icu/commit/1651e63b338a947b5979a219c5ecaa1e38e5fea5"
        },
        "date": 1696436535844,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 302.94129724305134,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "d82ad9975d46f901b01fd277becc0e4f458b8f7c",
          "message": "ICU-22526 Allow GMT-23:59 time zone\n\nECMA402 now allow offset timezone from -23:59 to 23:59.\nWe need to lower the minimum ZONE_OFFSET value to -23:59",
          "timestamp": "2023-10-04T14:43:36-07:00",
          "tree_id": "8874ee6f66b2e4e728081582c76d65ca567db66c",
          "url": "https://github.com/unicode-org/icu/commit/d82ad9975d46f901b01fd277becc0e4f458b8f7c"
        },
        "date": 1696456418233,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 219.44114802915854,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dragan@unicode.org",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "committer": {
            "email": "131725218+DraganBesevic@users.noreply.github.com",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "distinct": true,
          "id": "597e3110a5a129f6ad644c8dff0f04bacd565bc5",
          "message": "ICU-22325 CLDR 44 beta2 integration to ICU part three, source files changes",
          "timestamp": "2023-10-04T15:18:56-07:00",
          "tree_id": "84367251f03ce690b8856ecbf753a3b291bd5689",
          "url": "https://github.com/unicode-org/icu/commit/597e3110a5a129f6ad644c8dff0f04bacd565bc5"
        },
        "date": 1696459137066,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 234.62488200244263,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "1b5542da42a984f40f828871ad9674cadf04509d",
          "message": "ICU-22324 Mavenization: fix the with_full_javadoc profile\n\nSee #2658",
          "timestamp": "2023-10-04T16:29:18-07:00",
          "tree_id": "5d7f5d9df12241531ff31ed4f7b820d1bbeeca92",
          "url": "https://github.com/unicode-org/icu/commit/1b5542da42a984f40f828871ad9674cadf04509d"
        },
        "date": 1696462741518,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 259.78700342884457,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "623cb1acccfca1a6cbe02322a8fbd53ba3a3dd4e",
          "message": "ICU-22324 Update BRS task docs",
          "timestamp": "2023-10-04T21:05:20-07:00",
          "tree_id": "d9bcb9b86391eb2600da12544dcb8d0e70b0b212",
          "url": "https://github.com/unicode-org/icu/commit/623cb1acccfca1a6cbe02322a8fbd53ba3a3dd4e"
        },
        "date": 1696479377050,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 241.2381568939314,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "cc777ef48c27ae9f3c180be8354b9034118ccd6a",
          "message": "ICU-22324 Remove Ant build's `shared` directory",
          "timestamp": "2023-10-05T08:43:18-07:00",
          "tree_id": "8e67d2fa4cd27bc5317a7059bf04ea273cda4737",
          "url": "https://github.com/unicode-org/icu/commit/cc777ef48c27ae9f3c180be8354b9034118ccd6a"
        },
        "date": 1696521481015,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 224.42702305640097,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "deab8eef82c2005d7690b8dd9e95b24ca325d10b",
          "message": "ICU-22324 Update Maven migration scripts\n\nSee #2661",
          "timestamp": "2023-10-05T13:43:31-07:00",
          "tree_id": "4b8a78b881153ab8a31bfad79a3f13891cb5187a",
          "url": "https://github.com/unicode-org/icu/commit/deab8eef82c2005d7690b8dd9e95b24ca325d10b"
        },
        "date": 1696539934568,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 217.1518209409319,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "2e9d1288dd2f627227ff7a34c64905d3c92eb643",
          "message": "ICU-22324 Add docs for Maven usage\n\nSee #2655",
          "timestamp": "2023-10-05T13:43:38-07:00",
          "tree_id": "2f025ce0b82847c0f504ec2b26037c576055de66",
          "url": "https://github.com/unicode-org/icu/commit/2e9d1288dd2f627227ff7a34c64905d3c92eb643"
        },
        "date": 1696540353335,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.55797346107536,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "049c8ad36518d75bfefa63f3647cfee8b35c5ac0",
          "message": "ICU-22534 Script preparing the GitHub icu4j release files\n\nSee #2665",
          "timestamp": "2023-10-06T16:49:26-07:00",
          "tree_id": "d482652aa9019a6fdf01855022c2f428212567c6",
          "url": "https://github.com/unicode-org/icu/commit/049c8ad36518d75bfefa63f3647cfee8b35c5ac0"
        },
        "date": 1696636868301,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 224.64312122510285,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dakusan@castledragmire.com",
            "name": "Jeffrey Riaboy",
            "username": "dakusan"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "cdbf0ef6dfb478d04bb0f661c4f86e1ae7ea7075",
          "message": "ICU-22529 Update services.md\n\nAll external links were incorrectly pointing to the current directory with .md as the file extension.\n\nChanged all links to be \"../\" with the .md extension removed",
          "timestamp": "2023-10-09T13:01:06-07:00",
          "tree_id": "adda7df8cb5659be82be4aa9a7a54b276c34d46a",
          "url": "https://github.com/unicode-org/icu/commit/cdbf0ef6dfb478d04bb0f661c4f86e1ae7ea7075"
        },
        "date": 1696882367412,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 258.4892618102062,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "sarvesharora@microsoft.com",
            "name": "Sarvesh Arora",
            "username": "arorasarvesh"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "bd9e5ed620edda29b9e00ca471c3cd4e2eef4e26",
          "message": "ICU-21991 added VS2022 checks and changed windows SDK version",
          "timestamp": "2023-10-12T14:36:27+05:30",
          "tree_id": "e94b19085fb57812d95d001f77445b66499d337b",
          "url": "https://github.com/unicode-org/icu/commit/bd9e5ed620edda29b9e00ca471c3cd4e2eef4e26"
        },
        "date": 1697102206604,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.77375739640692,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "cb7b1b6506e6bbd4f241d7f9b9e0d23b892e398f",
          "message": "ICU-22548 Reduce the loop limit to fail faster\n\nReduce the error return from about 100s to 30s to avoid\nfuzzer 60s timeout error.\nAlso add a include file to fix uint8_t build breakage.",
          "timestamp": "2023-10-13T16:29:29-07:00",
          "tree_id": "1cf986850337444eb611569ab37daef3c7d515ee",
          "url": "https://github.com/unicode-org/icu/commit/cb7b1b6506e6bbd4f241d7f9b9e0d23b892e398f"
        },
        "date": 1697240473502,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 221.61196345250218,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mohd.akram@outlook.com",
            "name": "Mohamed Akram",
            "username": "mohd-akram"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "3d1dee683743c4578ced479c10b1fbe25aeacc9a",
          "message": "ICU-22528 Improve date formatting performance",
          "timestamp": "2023-10-13T18:43:04-07:00",
          "tree_id": "446db9fd9d2084fb39744715ad742d12747aa403",
          "url": "https://github.com/unicode-org/icu/commit/3d1dee683743c4578ced479c10b1fbe25aeacc9a"
        },
        "date": 1697248408876,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 250.22049659119548,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "cdab88ff4ef53342482a81f276af91567ddc4ff5",
          "message": "ICU-22513 Return error if days is too large in IslamicUmalquraCalendar\n\nIf the year is too large it may overflow the int32_t variable and cause\nslow or infinity loop, return error if the year is too large that the\nconversion to day may overflow int32_t. Limit the value to max value of\nint32_t divide by 400.",
          "timestamp": "2023-10-26T17:09:41-07:00",
          "tree_id": "4cbb2afadc7ff44c575612d67ca5a3a0f270e0dd",
          "url": "https://github.com/unicode-org/icu/commit/cdab88ff4ef53342482a81f276af91567ddc4ff5"
        },
        "date": 1698366823336,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 249.5278629218773,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "e04f4427dc838279f458fdc94fca26171af7a12c",
          "message": "ICU-22559 Hardcode the macroregions in XLikelySubtags and add a debug assertion\n\nSee #2688",
          "timestamp": "2023-10-27T14:18:51-07:00",
          "tree_id": "27d0ffdcb1715aa465bf3371821ac7ef19034957",
          "url": "https://github.com/unicode-org/icu/commit/e04f4427dc838279f458fdc94fca26171af7a12c"
        },
        "date": 1698442219976,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 224.46254753047683,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "92eeb45811055e0f055c21ac28e536a41156e57f",
          "message": "ICU-22547 fix addLikelySubtags for 4 chars script code\n\nAlso fix ICU-22546 to correct the comments in the API doc\nand add additional unit tests",
          "timestamp": "2023-10-27T17:29:05-07:00",
          "tree_id": "6e48d66768f9fad7e84f559dc01d0c709a977e30",
          "url": "https://github.com/unicode-org/icu/commit/92eeb45811055e0f055c21ac28e536a41156e57f"
        },
        "date": 1698453674676,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 221.8307628980041,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "b070c932ad3541e33ffb52f202b3bafb6f6c5118",
          "message": "ICU-22560 Merge ICU 74 maintenance branch to main (#2689)",
          "timestamp": "2023-10-30T13:50:52-07:00",
          "tree_id": "685233430c7a0ac58ad12feb2254e1367cc00247",
          "url": "https://github.com/unicode-org/icu/commit/b070c932ad3541e33ffb52f202b3bafb6f6c5118"
        },
        "date": 1698699712042,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 267.99377104238266,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "a7c7d8f214926ef23d2b54500ab77d95e5ea0068",
          "message": "ICU-22561 Added maven-gpg-plugin in pom.xml to sign artifacts for maven central release.",
          "timestamp": "2023-10-30T19:51:44-07:00",
          "tree_id": "d036564712e564ff038d3e3ae718297daa440b74",
          "url": "https://github.com/unicode-org/icu/commit/a7c7d8f214926ef23d2b54500ab77d95e5ea0068"
        },
        "date": 1698721369675,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 310.05343341569744,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "1bb711ad20427182566d769471fe0cda951fe297",
          "message": "ICU-22540 Add new CLDR units ronto, ronna, quecto, and quetta",
          "timestamp": "2023-10-31T14:58:52-07:00",
          "tree_id": "d5cd4dce7a9d55b7f5df27fdb641e44663eeb9d3",
          "url": "https://github.com/unicode-org/icu/commit/1bb711ad20427182566d769471fe0cda951fe297"
        },
        "date": 1698791234492,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 252.883979948199,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "fa6a4661ba002c1c1ee68cbf5c7ac9af75132d07",
          "message": "ICU-22545 Fix addLikelySubtags for pseudo Locales",
          "timestamp": "2023-10-31T15:00:34-07:00",
          "tree_id": "1cd1cc90cd4cad01544f57fa822debf95fb014de",
          "url": "https://github.com/unicode-org/icu/commit/fa6a4661ba002c1c1ee68cbf5c7ac9af75132d07"
        },
        "date": 1698792096391,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 275.7777943324256,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "091fcf6f82d3e2a3b956637bc44bd6c5650e8b85",
          "message": "ICU-22533 Don't mention 'Release Candidate' in javadoc",
          "timestamp": "2023-11-09T16:09:55-08:00",
          "tree_id": "c6ce532f15b0af2ac634dccf8f8edadd4bf811d4",
          "url": "https://github.com/unicode-org/icu/commit/091fcf6f82d3e2a3b956637bc44bd6c5650e8b85"
        },
        "date": 1699575515832,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.19965048565263,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "511e5efe562667dfe3f57c8d4cd8eb3af5b2137b",
          "message": "ICU-22533 Update BRS instructions for tagging release",
          "timestamp": "2023-11-10T15:33:31-08:00",
          "tree_id": "3fc88a64ac768951fe26db0482c36fc94d1f049a",
          "url": "https://github.com/unicode-org/icu/commit/511e5efe562667dfe3f57c8d4cd8eb3af5b2137b"
        },
        "date": 1699659795812,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 250.39453955241623,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "102ed8b6bd5a80b556349f62275c4edb60fd100b",
          "message": "ICU-22563 Limit the size for calendar fuzzer\n\nLimit to 1000 bytes of valid test data so the fuzzer will\nnot timeout because of running many operations.\n\nICU-22563 fix comment",
          "timestamp": "2023-11-16T14:55:00-08:00",
          "tree_id": "1b8dacfb6e0a1158df30ccdd82822783f277bf56",
          "url": "https://github.com/unicode-org/icu/commit/102ed8b6bd5a80b556349f62275c4edb60fd100b"
        },
        "date": 1700176045728,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.98500180388857,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "8d3d214ad7f76b7d0650f19a871a0e7d58a5986f",
          "message": "ICU-22531 Remove X from XLikelySubtags*",
          "timestamp": "2023-11-17T14:49:39-08:00",
          "tree_id": "9a9d59df81f551204a6ceda1d13d61ee96b6f874",
          "url": "https://github.com/unicode-org/icu/commit/8d3d214ad7f76b7d0650f19a871a0e7d58a5986f"
        },
        "date": 1700262044074,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.50066560026582,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "757d2cd90a434b35596d04fc89421dd0add33952",
          "message": "ICU-22555 Fix infinity loop in RuleBasedCollator constructor\n\nFix C++ and Java code.\nAdd unit tests for both C++ and Java.",
          "timestamp": "2023-11-29T11:31:27-08:00",
          "tree_id": "5f0f43f87175d11f696b1ffae7072536ce107d03",
          "url": "https://github.com/unicode-org/icu/commit/757d2cd90a434b35596d04fc89421dd0add33952"
        },
        "date": 1701286744417,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 183.6726882392209,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "5d3e84afc0a67abeb34af3384768104af7872523",
          "message": "ICU-22549 Add RuleBasedBreakIterator fuzzer",
          "timestamp": "2023-11-29T11:55:09-08:00",
          "tree_id": "7c2e7c8ce244098a3356d663ac357ec6877e6c48",
          "url": "https://github.com/unicode-org/icu/commit/5d3e84afc0a67abeb34af3384768104af7872523"
        },
        "date": 1701288816325,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.92332163880442,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "276d3dc8658f6fcb6c71cb0a74eba4244c270e2d",
          "message": "ICU-22493 Implement First Day Override in Calendar",
          "timestamp": "2023-11-29T11:55:51-08:00",
          "tree_id": "0e64e50c2b7eca8f027976e5192cf86fff2f1be3",
          "url": "https://github.com/unicode-org/icu/commit/276d3dc8658f6fcb6c71cb0a74eba4244c270e2d"
        },
        "date": 1701289103306,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.38555178377868,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "0bfa5f4c44ef3344982a23c1e1aa1bd6af6f8427",
          "message": "ICU-22549 Add DateTimePatternGenerator fuzzer\n\nSee #2708",
          "timestamp": "2023-11-29T14:45:21-08:00",
          "tree_id": "32717b9001f415957dd8723f0681946ad8568594",
          "url": "https://github.com/unicode-org/icu/commit/0bfa5f4c44ef3344982a23c1e1aa1bd6af6f8427"
        },
        "date": 1701298539699,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.61912000548006,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "2adf957de2d747b6e14cff8e0a5791c2fa484f40",
          "message": "ICU-22314 Refactor GH CI jobs into workflows triggered by modified paths",
          "timestamp": "2023-11-29T17:59:41-05:00",
          "tree_id": "1ef2c5ffddff54534094eb0d65dc08683dc50ffb",
          "url": "https://github.com/unicode-org/icu/commit/2adf957de2d747b6e14cff8e0a5791c2fa484f40"
        },
        "date": 1701299207058,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 194.8903010158616,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "ba4e8f2ef7feed2fe43c5e84a4540c793a2843c8",
          "message": "ICU-22549 Add Fuzer for Unicode property API",
          "timestamp": "2023-11-29T16:30:46-08:00",
          "tree_id": "24d9fc61a2c28f5094841212c5918969151e2d7a",
          "url": "https://github.com/unicode-org/icu/commit/ba4e8f2ef7feed2fe43c5e84a4540c793a2843c8"
        },
        "date": 1701304861173,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.89712559669596,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "78b1a3fc46f182c9277b532ac6f65f4bc35f230c",
          "message": "ICU-22522 Update the Azure CI from Clang 14 to Clang 16.",
          "timestamp": "2023-11-30T15:34:36+01:00",
          "tree_id": "0208ff4a672164a56f3ab8c7f3ecd6f9322e1d4b",
          "url": "https://github.com/unicode-org/icu/commit/78b1a3fc46f182c9277b532ac6f65f4bc35f230c"
        },
        "date": 1701355525565,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 184.53911361391212,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "368d67316c8413b7c8539e050acdbf8e64daf9e3",
          "message": "ICU-22549 Add UnicodeSet fuzzer",
          "timestamp": "2023-11-30T08:32:09-08:00",
          "tree_id": "18cbd3c57a2579aab308f879f9dfd0dc99e33a1e",
          "url": "https://github.com/unicode-org/icu/commit/368d67316c8413b7c8539e050acdbf8e64daf9e3"
        },
        "date": 1701362590866,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 194.78330607402611,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "e8e19454dac630d874fa980cfa8de8495c7bd268",
          "message": "ICU-22549 Add fuzzer for NumberFormatter",
          "timestamp": "2023-12-01T15:04:22-08:00",
          "tree_id": "d75c8643351c5109f01dc32a19ce96afb6642360",
          "url": "https://github.com/unicode-org/icu/commit/e8e19454dac630d874fa980cfa8de8495c7bd268"
        },
        "date": 1701472703560,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.604887663676,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "83327fb92ca5831df8ab78d5f706fae4ba25eb11",
          "message": "ICU-22549 Add Fuzzer for TimeZone",
          "timestamp": "2023-12-01T15:04:51-08:00",
          "tree_id": "c8601a6f3a7edba5a0c4a9c752186d3d0c6e4374",
          "url": "https://github.com/unicode-org/icu/commit/83327fb92ca5831df8ab78d5f706fae4ba25eb11"
        },
        "date": 1701473904984,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.24756248525964,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "d93c7b60fca892802bbffa6df9e42ff5e738e125",
          "message": "ICU-22568 return TimeZomeFormat::createInstance for bogus locale",
          "timestamp": "2023-12-01T15:10:41-08:00",
          "tree_id": "b506d22547fc9a112f53dd889a233313dc617264",
          "url": "https://github.com/unicode-org/icu/commit/d93c7b60fca892802bbffa6df9e42ff5e738e125"
        },
        "date": 1701474737861,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.8176828968519,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "394ebaaee0eda33a4baa440aebb12868556997a1",
          "message": "ICU-22522 Update configure files from configure.ac using autoreconf.",
          "timestamp": "2023-12-04T16:17:20+01:00",
          "tree_id": "6bd082273dc007d520d25ab199e297a3344d0af1",
          "url": "https://github.com/unicode-org/icu/commit/394ebaaee0eda33a4baa440aebb12868556997a1"
        },
        "date": 1701703638649,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.36351500443936,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "bcae6f2a437f3e58eb5afb8568f88b286a389e37",
          "message": "ICU-22575 Change AvailableFormatsSink to allow locales to inherit availableFormats items from the root locale.",
          "timestamp": "2023-12-04T12:47:50-08:00",
          "tree_id": "a6ca9466535d8b759e17aeeef210da7671712c80",
          "url": "https://github.com/unicode-org/icu/commit/bcae6f2a437f3e58eb5afb8568f88b286a389e37"
        },
        "date": 1701723396573,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.19852661501812,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "b246489074aa20dff7e5bbb99050ba437eec988d",
          "message": "ICU-22588 Limit test data to avoid meaningless timeout",
          "timestamp": "2023-12-05T16:24:02-08:00",
          "tree_id": "ebd4d101fe3a9d58153f388f4821fdea519f4789",
          "url": "https://github.com/unicode-org/icu/commit/b246489074aa20dff7e5bbb99050ba437eec988d"
        },
        "date": 1701823272728,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 194.5377057043658,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "2a3cb99f6e7aaf75fc88c536e95b34c60af1fd87",
          "message": "ICU-22589 Avoid timeout in TimeZone test",
          "timestamp": "2023-12-05T16:24:33-08:00",
          "tree_id": "e0296c97e01eb455b2f0d2215875ccbe04ad7fb9",
          "url": "https://github.com/unicode-org/icu/commit/2a3cb99f6e7aaf75fc88c536e95b34c60af1fd87"
        },
        "date": 1701824026425,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.69243068503366,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "da83309900e90509d6d559e4ad5f9a2a0a1cef6b",
          "message": "ICU-22595 GitHub release file generation script to include javadoc for each artifact in addition to full javadoc",
          "timestamp": "2023-12-07T10:37:15-08:00",
          "tree_id": "7739ff175cf22e7965eb192ad49dd11697ac658d",
          "url": "https://github.com/unicode-org/icu/commit/da83309900e90509d6d559e4ad5f9a2a0a1cef6b"
        },
        "date": 1701975030091,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.74480513003027,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "andy.heninger@gmail.com",
            "name": "Andy Heninger",
            "username": "aheninger"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "e6892996b1541564965a8eb0151a0bcabbd88ffc",
          "message": "ICU-22584 Fix RBBI rule builder stack overflow.\n\nThe problem was found by fuzz testing.\n\nA rule consisting of a long literal string produces a large, unbalanced parse tree,\none node per string element. Deleting the tree was recursive, once per node, resulting\nin deep recursion.\n\nThis PR changes node deletion to use an iterative (non-recursive) approach.\n\nThis change only affects rule building. There is no change to the RBBI run time\nusing pre-built rules.",
          "timestamp": "2023-12-08T12:49:26-08:00",
          "tree_id": "1468d96e3a369e3aa0d59b567cbf8088e05cccc7",
          "url": "https://github.com/unicode-org/icu/commit/e6892996b1541564965a8eb0151a0bcabbd88ffc"
        },
        "date": 1702069206375,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.13301060632423,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "rp9.next@gmail.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "665d9dbbe993b89c074c3463500237f8c99a4ac3",
          "message": "ICU-22534 BRS 75 front-load update version to 75.0.1\n\nSee #2726",
          "timestamp": "2023-12-08T14:32:40-08:00",
          "tree_id": "82548d317d2baa371e96c050243c26478a6906ba",
          "url": "https://github.com/unicode-org/icu/commit/665d9dbbe993b89c074c3463500237f8c99a4ac3"
        },
        "date": 1702075744411,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.2037747353092,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "73f972f7ff1a7799e70195cfbceff2ac32229169",
          "message": "ICU-22581 Fix RBBI leakage\n\nDuplicate variable references in the rule should not cause leakage",
          "timestamp": "2023-12-08T15:47:51-08:00",
          "tree_id": "5eee268cb2b2845dea6d6194d74a77e8f1321363",
          "url": "https://github.com/unicode-org/icu/commit/73f972f7ff1a7799e70195cfbceff2ac32229169"
        },
        "date": 1702080301021,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.66178457318833,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "4da7ffaa3648386704e1e4c6cbd1447bbac1678f",
          "message": "ICU-22580 Address infinity loop in RBBI\n\nICU-22580 Fix tests",
          "timestamp": "2023-12-11T11:34:47-08:00",
          "tree_id": "65ec517d38129a313f31ec5d9991d34816c8ae0b",
          "url": "https://github.com/unicode-org/icu/commit/4da7ffaa3648386704e1e4c6cbd1447bbac1678f"
        },
        "date": 1702323960388,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.074192961623,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "7d3cd7cba5a08304eec8f193ef070a89dd3bd31e",
          "message": "ICU-22584 Fix def of nullptr\n\nICU-22584 fix",
          "timestamp": "2023-12-11T14:35:10-08:00",
          "tree_id": "7b73a5291b341a91932409910d0fe194ad848e5c",
          "url": "https://github.com/unicode-org/icu/commit/7d3cd7cba5a08304eec8f193ef070a89dd3bd31e"
        },
        "date": 1702334932514,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.5955803264026,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "8b14c0579190eea71c030899581b1004ab665c77",
          "message": "ICU-22585 Fix infinity loop while unicode set contains single surrogate",
          "timestamp": "2023-12-11T15:33:12-08:00",
          "tree_id": "96c2fec2a75ad720208cd2a7612003124173534d",
          "url": "https://github.com/unicode-org/icu/commit/8b14c0579190eea71c030899581b1004ab665c77"
        },
        "date": 1702338129618,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.36433432323125,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "4a7d61d2617b2c165d2a58841d99762757f70f75",
          "message": "ICU-22579 Fix Null deref while Unicode Set only has string",
          "timestamp": "2023-12-12T14:39:12-08:00",
          "tree_id": "cebbbfc602168758597444336d5838e860d95fbe",
          "url": "https://github.com/unicode-org/icu/commit/4a7d61d2617b2c165d2a58841d99762757f70f75"
        },
        "date": 1702421250736,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.87848327746923,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "1bc059e7d6159a6c6daf6adfcb2a94768ad554c9",
          "message": "ICU-21107 Update configure files from configure.ac using autoreconf.",
          "timestamp": "2023-12-14T09:36:22-08:00",
          "tree_id": "341da52e22b44900a76bb29a7dbcd986e0ee0689",
          "url": "https://github.com/unicode-org/icu/commit/1bc059e7d6159a6c6daf6adfcb2a94768ad554c9"
        },
        "date": 1702575870923,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.93046064455794,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "e112f896a215e79ea249a715f128c73c1da04e7a",
          "message": "ICU-22576 Use standard alignof() with modern C.",
          "timestamp": "2023-12-14T19:11:29+01:00",
          "tree_id": "0db256688fe42baf082b42d5fae5e6dd880e10f9",
          "url": "https://github.com/unicode-org/icu/commit/e112f896a215e79ea249a715f128c73c1da04e7a"
        },
        "date": 1702577991579,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.58507107521368,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "e2d1d3ed43773549b31e201501d28a8f98bba9b5",
          "message": "ICU-22592 Rename source file that should not be directly compiled.",
          "timestamp": "2023-12-14T20:11:50+01:00",
          "tree_id": "4844eb11f6154cbdb58fdb1a3cbe128caf6d292a",
          "url": "https://github.com/unicode-org/icu/commit/e2d1d3ed43773549b31e201501d28a8f98bba9b5"
        },
        "date": 1702581727651,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.5006821709264,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "6f2e37ecb5db4c511473b38f5309d7fcd048ac51",
          "message": "ICU-22590 Delete useless source file.",
          "timestamp": "2023-12-14T20:41:44+01:00",
          "tree_id": "a08c0b43d9f46a32a2e41895b7e7dc10e5ac73c6",
          "url": "https://github.com/unicode-org/icu/commit/6f2e37ecb5db4c511473b38f5309d7fcd048ac51"
        },
        "date": 1702584044922,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.20236348646458,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "391e3b85fed0319e6c63588e9d9e69685542a8f2",
          "message": "ICU-22591 Delete obsolete source file.",
          "timestamp": "2023-12-14T20:42:00+01:00",
          "tree_id": "42ba0f42d26d6092ecc9810fa38560cb113e0412",
          "url": "https://github.com/unicode-org/icu/commit/391e3b85fed0319e6c63588e9d9e69685542a8f2"
        },
        "date": 1702584797927,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.07888726799715,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "99f6be43458a2339186cc527d979ec9f0bbad313",
          "message": "ICU-22593 Add missing layout/ prefix for Layout Engine header files.",
          "timestamp": "2023-12-14T23:51:10+01:00",
          "tree_id": "bb6b87c5270d1c315ccbfbdcf18336d22a07bc32",
          "url": "https://github.com/unicode-org/icu/commit/99f6be43458a2339186cc527d979ec9f0bbad313"
        },
        "date": 1702594893784,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.91345583074173,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "19af9e7ce3869cbe959e8e85b89abc8a28592309",
          "message": "ICU-22602 Fix stack overflow inside flattenVariables\n\nLimit the recursive call of flattenVariables to maximum depth 3500\nsince Java on my machine throw stack overflow exception around 3900.",
          "timestamp": "2023-12-14T15:14:21-08:00",
          "tree_id": "cb8d379e142f7d5b63e79f9ea26c5e267a863ce2",
          "url": "https://github.com/unicode-org/icu/commit/19af9e7ce3869cbe959e8e85b89abc8a28592309"
        },
        "date": 1702596282132,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.5070182094491,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "e76094c55a0fe4bc6f5307102698e0fc632bf89d",
          "message": "ICU-22605 Exclude the data files from the -sources.jar",
          "timestamp": "2023-12-15T09:08:22-08:00",
          "tree_id": "8ee8d12124d3163af2a54194543e145f7ebfd7bf",
          "url": "https://github.com/unicode-org/icu/commit/e76094c55a0fe4bc6f5307102698e0fc632bf89d"
        },
        "date": 1702660383771,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.1647872908701,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "77759422ddbe8603e1c9c909c0c006eb0775535d",
          "message": "ICU-22549 Add Normalizer2 Fuzzer\n\nICU-22549 Remove unnecessary include files",
          "timestamp": "2023-12-15T11:30:33-08:00",
          "tree_id": "f1bae80903c7e8c74dcc4cb52e9e05f131f7f13d",
          "url": "https://github.com/unicode-org/icu/commit/77759422ddbe8603e1c9c909c0c006eb0775535d"
        },
        "date": 1702669944572,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.49281807068652,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "5cf5ec1adbd2332b3cc289b5b1f5ca8324275fc3",
          "message": "ICU-22549 Add TimeZoneNames fuzzer",
          "timestamp": "2023-12-15T11:30:45-08:00",
          "tree_id": "9adc7082a05fb360ee47cf198c123c829d86f83a",
          "url": "https://github.com/unicode-org/icu/commit/5cf5ec1adbd2332b3cc289b5b1f5ca8324275fc3"
        },
        "date": 1702670730827,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.7328671381135,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "7bef50e71d6ae7a58c5547092feb110ec71810ab",
          "message": "ICU-22609 Fix nulldef w/ bogus locale in DateFormat::create*",
          "timestamp": "2023-12-15T16:14:56-08:00",
          "tree_id": "68dc6b3791d7d9eaf1b9eb8a272d7a618e257b40",
          "url": "https://github.com/unicode-org/icu/commit/7bef50e71d6ae7a58c5547092feb110ec71810ab"
        },
        "date": 1702686782434,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.00922520800412,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "4ba5d9191bc20e33773ee1ff0b7a96b2b2de8359",
          "message": "ICU-22549 Add DateFormatSymbols fuzzer",
          "timestamp": "2023-12-15T16:17:38-08:00",
          "tree_id": "0b8ff175a81f70716149a310b18c5be4ce6bd3f1",
          "url": "https://github.com/unicode-org/icu/commit/4ba5d9191bc20e33773ee1ff0b7a96b2b2de8359"
        },
        "date": 1702687785850,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.10550922133643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "11d1148e5680fe62456659af85b37f690612ba73",
          "message": "ICU-22549 Improve fuzzer to test more locale\n\nWe found bogus locale cause crash in DateFormat so here\nwe enhance the fuzzer to also test locale name which are not\nreturn by the available locale list.",
          "timestamp": "2023-12-18T13:31:06-08:00",
          "tree_id": "6408ab517d2e1a3cc6b80abb260b057428f7512e",
          "url": "https://github.com/unicode-org/icu/commit/11d1148e5680fe62456659af85b37f690612ba73"
        },
        "date": 1702935580634,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.1798577313669,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "838227ce9570921e0aa75a399fdff47862d34f61",
          "message": "ICU-22614 Fix buffer overflow in TimeZoneNames\n\nSee #2752",
          "timestamp": "2023-12-18T16:26:06-08:00",
          "tree_id": "962109c774b87bf4dc255ee952fb4b976335eacd",
          "url": "https://github.com/unicode-org/icu/commit/838227ce9570921e0aa75a399fdff47862d34f61"
        },
        "date": 1702947155758,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 187.51458117147394,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "85b7f5fda245e3832803ec4ec2540a2d2ea959d0",
          "message": "ICU-22604 Use 'void' for empty C parameter lists (-Wstrict-prototypes).",
          "timestamp": "2023-12-19T09:27:01+09:00",
          "tree_id": "ad1d34f0badf61b279bc70e0ef1d4753b866bf40",
          "url": "https://github.com/unicode-org/icu/commit/85b7f5fda245e3832803ec4ec2540a2d2ea959d0"
        },
        "date": 1702947902116,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 222.1670082489345,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "1384d9f3959470974470267eab722614b71e02d8",
          "message": "ICU-22532 Remove redundant 'void' from empty C++ parameter lists.\n\nhttps://releases.llvm.org/16.0.0/tools/clang/tools/extra/docs/clang-tidy/checks/modernize/redundant-void-arg.html",
          "timestamp": "2023-12-19T09:27:18+09:00",
          "tree_id": "50eb51f43a937b8afd14c9f05d419cac3865d52b",
          "url": "https://github.com/unicode-org/icu/commit/1384d9f3959470974470267eab722614b71e02d8"
        },
        "date": 1702948629428,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.45236092763363,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "fc6e282d98668dea248c847f9cf13a0a4b55443a",
          "message": "ICU-22549 Limit Normalizer2 Fuzzer for 5K of input\n\nTo avoid timeout",
          "timestamp": "2023-12-19T13:58:03-08:00",
          "tree_id": "b6c96857821b0eb92b74e403a33d2102e7c6d385",
          "url": "https://github.com/unicode-org/icu/commit/fc6e282d98668dea248c847f9cf13a0a4b55443a"
        },
        "date": 1703023651960,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 186.75887030425122,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "ef78f2a86359c3aff05eec5ab6de5dbc28c69190",
          "message": "ICU-21107 Update configure files from configure.ac using autoreconf.",
          "timestamp": "2023-12-21T09:31:55+09:00",
          "tree_id": "5fcc9d9c6426758cb4f935b6dc4d8caf5e73c193",
          "url": "https://github.com/unicode-org/icu/commit/ef78f2a86359c3aff05eec5ab6de5dbc28c69190"
        },
        "date": 1703119326322,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 180.65023012683756,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "23dd2b8b5499506a9ff541c9c2f8bf7701cf93b8",
          "message": "ICU-22549 Add PluralRule Fuzzer",
          "timestamp": "2023-12-21T14:21:33-08:00",
          "tree_id": "5ccead0f9023e30c98546f9ce98c71be6bff5aee",
          "url": "https://github.com/unicode-org/icu/commit/23dd2b8b5499506a9ff541c9c2f8bf7701cf93b8"
        },
        "date": 1703197858274,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.37540101269167,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "daa0171501f47962bb41e9fc1bb7e1a1e7148ab0",
          "message": "ICU-22608 Add necessary using statements for namespace icu.",
          "timestamp": "2023-12-22T08:12:47+09:00",
          "tree_id": "bd144ab4491415949b4791324098d0bd37ef335a",
          "url": "https://github.com/unicode-org/icu/commit/daa0171501f47962bb41e9fc1bb7e1a1e7148ab0"
        },
        "date": 1703200943230,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 186.75252287195738,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "887005b68a689cc1d9f2b13e8e23dd4bfeaaf5f5",
          "message": "ICU-22520 Remove unnecessary copying of statically allocated strings.",
          "timestamp": "2023-12-22T08:43:55+09:00",
          "tree_id": "8882ca817ed66a753f3f867b66959bd6cea767cd",
          "url": "https://github.com/unicode-org/icu/commit/887005b68a689cc1d9f2b13e8e23dd4bfeaaf5f5"
        },
        "date": 1703203074081,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.70807007908581,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "9d3e12b8202e030f5993a195bf8d1332485206c6",
          "message": "ICU-22601 v74.2 fix dangling LICENSE file\n\n- 'git archive' works on a subtree so did not include LICENSE\n- we copy the LICENSE file from the build dir\n- broken by ICU-22309",
          "timestamp": "2023-12-22T09:28:09-08:00",
          "tree_id": "931a9c4fb66c66d728da81bc3c2ded4dbfc13534",
          "url": "https://github.com/unicode-org/icu/commit/9d3e12b8202e030f5993a195bf8d1332485206c6"
        },
        "date": 1703266606461,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.05447728582922,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "9a91599fd44333a65ae3b1519b577b848f4c46d6",
          "message": "ICU-22520 Replace uprv_malloc() / uprv_free() with icu::LocalPointer.",
          "timestamp": "2023-12-23T10:42:55+09:00",
          "tree_id": "ec2f281b502d22e57b51c02ffe87b3eeeaf07ed6",
          "url": "https://github.com/unicode-org/icu/commit/9a91599fd44333a65ae3b1519b577b848f4c46d6"
        },
        "date": 1703296246335,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.9155155706559,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "03a870a23cc07fab7e937919b5095b7ec2b98553",
          "message": "ICU-22520 Replace uprv_malloc() / uprv_free() with icu::CharString.",
          "timestamp": "2023-12-28T10:27:35+09:00",
          "tree_id": "8c8283296e42f5bcf3a0bcb6e7dd3432d6bfb8c6",
          "url": "https://github.com/unicode-org/icu/commit/03a870a23cc07fab7e937919b5095b7ec2b98553"
        },
        "date": 1703727466593,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.32981027810666,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "7eb56fee1ec2ce125c92f3a38003f743cb738992",
          "message": "ICU-22520 Remove now superfluous intermediate buffers.",
          "timestamp": "2023-12-28T11:13:38+09:00",
          "tree_id": "dec5277fd6c69f542fda90c5bdad4f5231862e7e",
          "url": "https://github.com/unicode-org/icu/commit/7eb56fee1ec2ce125c92f3a38003f743cb738992"
        },
        "date": 1703730204281,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.75080179285234,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "manishsmail@gmail.com",
            "name": "Manish Goregaokar",
            "username": "Manishearth"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "74abcfe288b4b25b8255e65c9cafbe8f89319bf4",
          "message": "ICU-22481 Add toml support to gendict\n\n(and use it in CI)",
          "timestamp": "2023-12-27T22:59:57-08:00",
          "tree_id": "bef361841b7a432f18296e39bafdc5b6b3ce2f8c",
          "url": "https://github.com/unicode-org/icu/commit/74abcfe288b4b25b8255e65c9cafbe8f89319bf4"
        },
        "date": 1703747281582,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.45501975289386,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "02740597ce6a90d14ee2bb9b1a66e16ed5543328",
          "message": "ICU-22520 Replace char arrays with icu::CharString.",
          "timestamp": "2023-12-29T11:30:57+09:00",
          "tree_id": "2b97a4f8909dae952a07d05e7f9ee3737e84a0c9",
          "url": "https://github.com/unicode-org/icu/commit/02740597ce6a90d14ee2bb9b1a66e16ed5543328"
        },
        "date": 1703817627356,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.66401051243238,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "1a60a038e14f0c56f50052c03fe76c4933cda339",
          "message": "ICU-21952 Add withoutLocale functions to LocalizedNumber[Range]Formatter\n\nSee #2483",
          "timestamp": "2023-12-28T22:04:02-08:00",
          "tree_id": "3acc1dc6c4403976a6da569cc0bbfd32999a1b4a",
          "url": "https://github.com/unicode-org/icu/commit/1a60a038e14f0c56f50052c03fe76c4933cda339"
        },
        "date": 1703831219559,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.40951019530348,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "52ff51e82c74adc52629b411d8cf6718c45e2210",
          "message": "ICU-22549 Revert \"Improve fuzzer to test more locale\"\n\nSee #2770",
          "timestamp": "2024-01-03T14:19:32-08:00",
          "tree_id": "76faf473235822d1cadbae0db235856c8933cd20",
          "url": "https://github.com/unicode-org/icu/commit/52ff51e82c74adc52629b411d8cf6718c45e2210"
        },
        "date": 1704321391986,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.9645771495231,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "539e8f41a35fec30fba4cfb3a65ea4db67978f85",
          "message": "ICU-22532 Compiler warning: conversion from 'double' to 'int32_t'.\n\nThe definition of kOneDay is 1.0 * U_MILLIS_PER_DAY so there's no\nreason whatsoever to not just use U_MILLIS_PER_DAY directly here.",
          "timestamp": "2024-01-04T09:40:40+09:00",
          "tree_id": "25d2437641ab75a247edd9732303544139770b89",
          "url": "https://github.com/unicode-org/icu/commit/539e8f41a35fec30fba4cfb3a65ea4db67978f85"
        },
        "date": 1704329396506,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.4485684341405,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "cfba9a8caf6cb6ab39eb4ca5702c78f97989bcde",
          "message": "ICU-22549 Reland PR2770 w/ fix\n\nReland PR2770 w/ the fix that null termination the input to Locale\nconstuctor.",
          "timestamp": "2024-01-05T01:18:10-08:00",
          "tree_id": "3f6b34bc29d24edb76e878374b92031d49dd1d6e",
          "url": "https://github.com/unicode-org/icu/commit/cfba9a8caf6cb6ab39eb4ca5702c78f97989bcde"
        },
        "date": 1704447175624,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.67824113349707,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "3f054adaf3ca34d55338e56773088bd589600583",
          "message": "ICU-22549 Fix fuzzer to call Locale with null-terminiate string",
          "timestamp": "2024-01-05T01:18:48-08:00",
          "tree_id": "9b7b8cde8751e8ca3e3d1feaf306bf9f1a706c09",
          "url": "https://github.com/unicode-org/icu/commit/3f054adaf3ca34d55338e56773088bd589600583"
        },
        "date": 1704448078363,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.23778564997173,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "87ca0234476c62f7d42ab43bb4f5e17ca329f5d0",
          "message": "ICU-22520 Replace char arrays with icu::CharString.",
          "timestamp": "2024-01-10T12:19:44+09:00",
          "tree_id": "051272f95789f629f03907ef8763fbbb7aff961a",
          "url": "https://github.com/unicode-org/icu/commit/87ca0234476c62f7d42ab43bb4f5e17ca329f5d0"
        },
        "date": 1704857329853,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.78885617171815,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "906093f31b44316ba7da24972f95a0510355e61d",
          "message": "ICU-22520 Make icu::CharString comparable with itself.",
          "timestamp": "2024-01-10T17:30:32+09:00",
          "tree_id": "23c5a2293028696211e169684baf57e36ec7dc42",
          "url": "https://github.com/unicode-org/icu/commit/906093f31b44316ba7da24972f95a0510355e61d"
        },
        "date": 1704875988888,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.66938581596963,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "23d1fc5989c87d5a0572eed54f7c1000f1e3e71b",
          "message": "ICU-22549 Fix incorrect pointer\n\nRemove the adjustment of data pointer to avoid buffer-overflow\nFix bug https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=65632",
          "timestamp": "2024-01-10T12:36:39-08:00",
          "tree_id": "0b452a9ffd9f1b9168ca3f579299dcff87f6a506",
          "url": "https://github.com/unicode-org/icu/commit/23d1fc5989c87d5a0572eed54f7c1000f1e3e71b"
        },
        "date": 1704919585032,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.4688218038288,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "a837e0d3991e05db61dfbd35ee985976109092d5",
          "message": "ICU-22532 Set a value for _POSIX_C_SOURCE to get symlink() declared.\n\nISO C99 and later do not support implicit function declarations.",
          "timestamp": "2024-01-11T17:35:28+09:00",
          "tree_id": "8a3d055b1a7b5613d68bfa5913908d0eda3c9963",
          "url": "https://github.com/unicode-org/icu/commit/a837e0d3991e05db61dfbd35ee985976109092d5"
        },
        "date": 1704962796985,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.29933772906492,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "dc7014fda6d6f08b3ddf468bddd62548c8c6263f",
          "message": "ICU-22620 tz2023d updates",
          "timestamp": "2024-01-11T11:41:23-05:00",
          "tree_id": "324b428a5eb44f9b51c10f09c8bda35c1923126b",
          "url": "https://github.com/unicode-org/icu/commit/dc7014fda6d6f08b3ddf468bddd62548c8c6263f"
        },
        "date": 1704991834441,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.45179376008537,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "509405c9f2ee99a56a3fc77ba8de4799cec28d7c",
          "message": "ICU-22626 Fix leakage when 2 '=' in PluralRules\n\nSee #2782",
          "timestamp": "2024-01-12T09:36:29-08:00",
          "tree_id": "ddfdfc694910b45a4719331b731180fd916366a0",
          "url": "https://github.com/unicode-org/icu/commit/509405c9f2ee99a56a3fc77ba8de4799cec28d7c"
        },
        "date": 1705081553474,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.18763414241434,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "ab72ab1d4a3c3f9beeb7d92b0c7817ca93dfdb04",
          "message": "ICU-22520 Replace char arrays with icu::CharString & icu::MemoryPool.",
          "timestamp": "2024-01-13T17:05:58+09:00",
          "tree_id": "371968f83db3ff8f1becc9ff5b5797046b90f0b7",
          "url": "https://github.com/unicode-org/icu/commit/ab72ab1d4a3c3f9beeb7d92b0c7817ca93dfdb04"
        },
        "date": 1705133878280,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.35262368021873,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "078b88a11abe97766824a0f0e712fa876bbb08b4",
          "message": "ICU-21107 Specify language standard versions C11 & C++17 also for MSVC.\n\nThere were until now no versions specified at all, relying on the\ndefault (or commandline overrides) to be sufficiently recent.",
          "timestamp": "2024-01-16T22:22:05+09:00",
          "tree_id": "20e7b34047139c0cf8057270309bb6431861e19a",
          "url": "https://github.com/unicode-org/icu/commit/078b88a11abe97766824a0f0e712fa876bbb08b4"
        },
        "date": 1705411864199,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.5878299566517,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "2d65456a3bb3f874df4c14ca1584598517bfe611",
          "message": "ICU-21107 Specify language standard versions C11 & C++17 also for Bazel.\n\nThis is copied (with C11 added) from:\n\nhttps://github.com/tensorflow/tensorflow/blob/v2.15.0/.bazelrc\n\nThere were until now no versions specified at all, relying on the\ndefault (or commandline overrides) to be sufficiently recent.",
          "timestamp": "2024-01-17T16:26:57+09:00",
          "tree_id": "a0f1e5e082d14396e30841240765d74b49075823",
          "url": "https://github.com/unicode-org/icu/commit/2d65456a3bb3f874df4c14ca1584598517bfe611"
        },
        "date": 1705476855264,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.40093321418166,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "4a8cd80973a29a48d4876160406a3fb6004d13bd",
          "message": "ICU-21107 Clarify the C11 & C++17 requirement in the userguide.",
          "timestamp": "2024-01-17T17:48:10+09:00",
          "tree_id": "1eea726aa6877c37f4697232e587e613eee6790b",
          "url": "https://github.com/unicode-org/icu/commit/4a8cd80973a29a48d4876160406a3fb6004d13bd"
        },
        "date": 1705481598448,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.22301808334043,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "47b9a517be000559499dcfcb2ed62eace740e194",
          "message": "ICU-21107 Specify language standard versions C11 & C++17 also for MSVC\n\nICU-21107 Specify language standard versions C11 & C++17 also for MSVC",
          "timestamp": "2024-01-17T09:26:22-08:00",
          "tree_id": "662e3a8cfae8854367b76031cf58a4ae6db8185b",
          "url": "https://github.com/unicode-org/icu/commit/47b9a517be000559499dcfcb2ed62eace740e194"
        },
        "date": 1705512958875,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.39963354549252,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "981057143ceb68bcf6dc6c81a3c279645f98d6a9",
          "message": "ICU-21107 Remove CI jobs using MSVC too old to support C11.\n\nVersions before VS 2019 don't have C11 standard library features:\n\nhttps://learn.microsoft.com/en-us/cpp/overview/visual-cpp-language-conformance?view=msvc-170#c-standard-library-features-1",
          "timestamp": "2024-01-18T13:03:45+09:00",
          "tree_id": "d7a42b277b0c3c249d6c4692067638899dd6c15e",
          "url": "https://github.com/unicode-org/icu/commit/981057143ceb68bcf6dc6c81a3c279645f98d6a9"
        },
        "date": 1705550781287,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 194.86217421201044,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "d0672fa8ab11a9795ad7d42f20e63dd2307cd8e2",
          "message": "ICU-21107 Simplify type_traits expressions for C++17.",
          "timestamp": "2024-01-18T20:28:39+09:00",
          "tree_id": "0893fe1623c3207c625e372bb155877aa123d8e3",
          "url": "https://github.com/unicode-org/icu/commit/d0672fa8ab11a9795ad7d42f20e63dd2307cd8e2"
        },
        "date": 1705577838925,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.20647053238568,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "c5160765d40cf18751f4b3407e1cc7e8800c1cbe",
          "message": "ICU-22639 Clone the calendar so we don't mess with the real one.",
          "timestamp": "2024-01-19T08:07:22+09:00",
          "tree_id": "392cd07c74d4cd17a45694c2bd1cf12de9aeeff5",
          "url": "https://github.com/unicode-org/icu/commit/c5160765d40cf18751f4b3407e1cc7e8800c1cbe"
        },
        "date": 1705621112482,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.5489786746224,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "ca53838b8363589816b1464140128c2963a3881d",
          "message": "ICU-22576 Remove now obsolete pre-C11 backward compatibility code.",
          "timestamp": "2024-01-19T08:05:24+09:00",
          "tree_id": "2154f68dd394fa6c651518a6ef0b269ed454d4c3",
          "url": "https://github.com/unicode-org/icu/commit/ca53838b8363589816b1464140128c2963a3881d"
        },
        "date": 1705637647542,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.87326390159126,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "7b690aa8c797ca3fead7821200f1b1b10093bcfc",
          "message": "ICU-22639 Revert now obsolete bug workarounds.\n\nThis reverts changes from commit 47b9a517be000559499dcfcb2ed62eace740e194.\nThis reverts changes from commit 214ae60d9425e5cc78580d0dddf3bbbe4c80e991.",
          "timestamp": "2024-01-19T13:11:49+09:00",
          "tree_id": "9c5b90e11bb0ac463376ac7cccc1376f7f71a481",
          "url": "https://github.com/unicode-org/icu/commit/7b690aa8c797ca3fead7821200f1b1b10093bcfc"
        },
        "date": 1705638072529,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.34875625700602,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "3eb8923b9730efeae10a92f8054f63495fe8a95f",
          "message": "ICU-22638 Use parseNumber to fix buffer-overflow\n\nSee #2795",
          "timestamp": "2024-01-19T01:43:35-08:00",
          "tree_id": "eae3867ff6963eb9947bc1f0897e722ef88616e8",
          "url": "https://github.com/unicode-org/icu/commit/3eb8923b9730efeae10a92f8054f63495fe8a95f"
        },
        "date": 1705658698449,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.13043809307973,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "c8336085bb7489650c449f05c13be7b6a660fd89",
          "message": "ICU-22637 Rewrite custom timezone parser\n\nSee #2792",
          "timestamp": "2024-01-19T01:44:52-08:00",
          "tree_id": "f26273c3c36ce63d5c80fabed5cea318955aa21f",
          "url": "https://github.com/unicode-org/icu/commit/c8336085bb7489650c449f05c13be7b6a660fd89"
        },
        "date": 1705659496029,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.89984317727075,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "7cec4a9308b0d693b08f0bc35a2a08ce6d5f4646",
          "message": "ICU-21107 Replace use of adapter and binder classes removed in C++17.",
          "timestamp": "2024-01-19T19:49:44+09:00",
          "tree_id": "8d10c33edb1e82c9049488e3e35ed8814a73ce0d",
          "url": "https://github.com/unicode-org/icu/commit/7cec4a9308b0d693b08f0bc35a2a08ce6d5f4646"
        },
        "date": 1705661947563,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.61161108436966,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "9832f48e229010e2a5d413eb8d48cacc3cafbdcd",
          "message": "ICU-22636 Return U_BRK_RULE_SYNTAX when status number is too large\n\nSee #2793",
          "timestamp": "2024-01-19T17:16:54-08:00",
          "tree_id": "1f0a7cf45c1ed76faa6c21a5a7eb870fdcf5c630",
          "url": "https://github.com/unicode-org/icu/commit/9832f48e229010e2a5d413eb8d48cacc3cafbdcd"
        },
        "date": 1705713946399,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.1049390202029,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "9167d6beaf4be16f2f88ec9997563d21d4251757",
          "message": "ICU-21107 Update configure files from configure.ac using autoreconf.",
          "timestamp": "2024-01-22T12:30:24+09:00",
          "tree_id": "5199ff2a053426e21ec1340797831ad7a4213258",
          "url": "https://github.com/unicode-org/icu/commit/9167d6beaf4be16f2f88ec9997563d21d4251757"
        },
        "date": 1705895082776,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.7827466943644,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "49a7a1e76d720a265daf420d4d271dd0d30cbc16",
          "message": "ICU-22520 Update urename.h with ulocimp_getParent().\n\nThis was forgotten in commit 037449fff8db873afdd2e3c6ed5d24db604ffe64.",
          "timestamp": "2024-01-22T12:30:37+09:00",
          "tree_id": "21a2cde30084f1beb8471fc422d62ba35a9e0917",
          "url": "https://github.com/unicode-org/icu/commit/49a7a1e76d720a265daf420d4d271dd0d30cbc16"
        },
        "date": 1705895900988,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.18205965157765,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "7c48842e239e509a419a04e7d25fdbf1196eb90a",
          "message": "ICU-22637 Accept non-ASCII digit to fix mistake",
          "timestamp": "2024-01-22T14:00:12-08:00",
          "tree_id": "103f0cbd51a907fd2defd2a7044ed7a84d7b3909",
          "url": "https://github.com/unicode-org/icu/commit/7c48842e239e509a419a04e7d25fdbf1196eb90a"
        },
        "date": 1705961521785,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.26646375995492,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "5eded36279b89b00420d37acdbdfcf57e2cdd76d",
          "message": "ICU-22520 Bugfix: Use macro parameter name instead of variable name.",
          "timestamp": "2024-01-23T13:00:26+09:00",
          "tree_id": "59c90c02a4a698226f65d7b5c2ee959524b51eae",
          "url": "https://github.com/unicode-org/icu/commit/5eded36279b89b00420d37acdbdfcf57e2cdd76d"
        },
        "date": 1705983473528,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.5384618827483,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "e81b8727aaafa306858565c0e4116703c4893459",
          "message": "ICU-22532 Move the definition of _POSIX_C_SOURCE to the Makefile.\n\nThis is already set in Makefile.in and therefore results in a macro\nredefined warning if also defined in the source file. It seems that\nsetting this in the Makefile was how it was originally intended do be\ndone (but then it was just never updated there).",
          "timestamp": "2024-01-23T13:00:54+09:00",
          "tree_id": "9a6422a3f0dee226460d2bc21a04a4489dc5d0f3",
          "url": "https://github.com/unicode-org/icu/commit/e81b8727aaafa306858565c0e4116703c4893459"
        },
        "date": 1705984202615,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.79489302330094,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "f7f9dbb88d34011c1fc9aee53e9ce6fda20bfb1e",
          "message": "ICU-22641 Change const refs in the class to const pointers\n\nThe function may return nullptr, we cannot just deref the return value.\nChange them to const pointer instead.",
          "timestamp": "2024-01-23T13:39:49-08:00",
          "tree_id": "272d629ce9dbff52e51470b04d03f4392b487b0c",
          "url": "https://github.com/unicode-org/icu/commit/f7f9dbb88d34011c1fc9aee53e9ce6fda20bfb1e"
        },
        "date": 1706046575739,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.42691430214896,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "400d97e7d206ffae77c294faeb9e2aa3850c6c23",
          "message": "ICU-22533 Update Maven CLI instructions for multi-module ICU4J\n\nSee #2791",
          "timestamp": "2024-01-23T13:59:56-08:00",
          "tree_id": "e634878e4f29d0755bdf5063ebaffc4befeef719",
          "url": "https://github.com/unicode-org/icu/commit/400d97e7d206ffae77c294faeb9e2aa3850c6c23"
        },
        "date": 1706047596832,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.85779415446189,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "8f80c62aa2a3307d0d6248fbb2116851edcdcce4",
          "message": "ICU-22638 Fix cast overflow issue",
          "timestamp": "2024-01-25T12:11:56-08:00",
          "tree_id": "492c598550f87b2b9b0f1f5eb9b3a758be6cb673",
          "url": "https://github.com/unicode-org/icu/commit/8f80c62aa2a3307d0d6248fbb2116851edcdcce4"
        },
        "date": 1706214010278,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.3133938083341,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "e1415d1282b058e701d41d75e42b0bffed5f8477",
          "message": "ICU-22635 Avoid integer-overflow for invalid large UChar32",
          "timestamp": "2024-01-29T11:57:12-08:00",
          "tree_id": "037f96ae21f0c3b9d50004e2ca03d6b8ca93c720",
          "url": "https://github.com/unicode-org/icu/commit/e1415d1282b058e701d41d75e42b0bffed5f8477"
        },
        "date": 1706558843410,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.48967341161736,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "ae9cc8cbd1f0f70d429ceb1c5c369d5481829661",
          "message": "ICU-22520 Replace char arrays with icu::CharString.",
          "timestamp": "2024-01-30T12:04:53+01:00",
          "tree_id": "0ece6fb5f0d2ea395bfe0105bb8acbb92608419d",
          "url": "https://github.com/unicode-org/icu/commit/ae9cc8cbd1f0f70d429ceb1c5c369d5481829661"
        },
        "date": 1706613285492,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.4827626749114,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "9515e82741875b09fbd223573fb5e6d23fad6023",
          "message": "ICU-22633 Fix Integer-overflow in icu_75::Calendar::add\n\nSee #2805",
          "timestamp": "2024-02-01T13:49:41-08:00",
          "tree_id": "b72fd7a042249c6199b503abc8f52b98afa24697",
          "url": "https://github.com/unicode-org/icu/commit/9515e82741875b09fbd223573fb5e6d23fad6023"
        },
        "date": 1706824814825,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.38245321911756,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "6562a7df85f6aa9c3f028e6f9523de3f9ec3fa7f",
          "message": "ICU-22627 Delete obsolete test case letest/api/ScriptTest.",
          "timestamp": "2024-02-02T15:55:27+01:00",
          "tree_id": "c6ec0ba46d243b006649efd89d94c963c871254f",
          "url": "https://github.com/unicode-org/icu/commit/6562a7df85f6aa9c3f028e6f9523de3f9ec3fa7f"
        },
        "date": 1706886283339,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.11182463190912,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "b8271577b66f0a29409d40b686505e917538c1e8",
          "message": "ICU-22649 Fix possible leakage by using LocalUResourceBundlePointer",
          "timestamp": "2024-02-02T10:24:21-08:00",
          "tree_id": "c1bd16adf0d830bd97276ba436eab5aab194fc98",
          "url": "https://github.com/unicode-org/icu/commit/b8271577b66f0a29409d40b686505e917538c1e8"
        },
        "date": 1706898831879,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.51393432498594,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "6fa113eaa8e73088399995ac79fdc332a7499c1c",
          "message": "ICU-22651 Refactor U_DEFINE_LOCAL_OPEN_POINTER into a template.",
          "timestamp": "2024-02-05T14:15:15+01:00",
          "tree_id": "d629c9f9201fe85e1c9a2514b5ce1db420b7841d",
          "url": "https://github.com/unicode-org/icu/commit/6fa113eaa8e73088399995ac79fdc332a7499c1c"
        },
        "date": 1707139549680,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.73591600624647,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "835b009314ed49778a4d664f770c4a9e2a15fa75",
          "message": "ICU-22520 Make ulocimp_get*() internal to ulocimp_getSubtags().\n\nThese functions now no longer have any other callers so they can be made\ninternal to the compilation unit of ulocimp_getSubtags(), thus bringing\nthem back to how they originally were intended to be used (and making\nthe comment above them true once again).\n\nThis also makes it possible to remove the temporary icu::CharString\nobjects that previously were returned to callers and instead write\ndirectly to icu::ByteSink, making the code both simpler and less\nwasteful (also that how this was once intended).",
          "timestamp": "2024-02-06T13:12:55+01:00",
          "tree_id": "5aaae898059a68c8783a483ae59fb89eb5ef486f",
          "url": "https://github.com/unicode-org/icu/commit/835b009314ed49778a4d664f770c4a9e2a15fa75"
        },
        "date": 1707222133463,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.73751423819576,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "d28e12b1f22491b451642dd84299e9f321c422ab",
          "message": "ICU-22520 Replace char arrays with icu::CharString.",
          "timestamp": "2024-02-06T19:53:53+01:00",
          "tree_id": "4c91f99c22c8ec1dc81ce14a6cb4bb3a4bfe2e29",
          "url": "https://github.com/unicode-org/icu/commit/d28e12b1f22491b451642dd84299e9f321c422ab"
        },
        "date": 1707246080433,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.32299545721722,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "43ab3d1de8a06f93bd07fa4b9d5168e1df440783",
          "message": "ICU-22583 BRS 75rc CLDR 45-alpha0 to ICU main part 4 (fix to get new unitPrefixes data)",
          "timestamp": "2024-02-06T18:07:44-08:00",
          "tree_id": "503d6ee43c8f57e8d44b1fb5fb72bb4b1c0c4430",
          "url": "https://github.com/unicode-org/icu/commit/43ab3d1de8a06f93bd07fa4b9d5168e1df440783"
        },
        "date": 1707272223371,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.78499606157087,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "56509e88bfd9e1e48d2541373b1beeb12844ab58",
          "message": "ICU-22651 Move LocalOpenPointer into an internal nested namespace.",
          "timestamp": "2024-02-07T14:27:17+01:00",
          "tree_id": "04d9809965c9d4d76bf3639bdba6b050e9263219",
          "url": "https://github.com/unicode-org/icu/commit/56509e88bfd9e1e48d2541373b1beeb12844ab58"
        },
        "date": 1707313386895,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 194.09315018635291,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "a6efa924ad04f7e3c7ad325237bf026b39abe4e8",
          "message": "ICU-22520 Let ulocimp_getRegionForSupplementalData() return CharString.",
          "timestamp": "2024-02-07T14:27:40+01:00",
          "tree_id": "1506023d9bf4a704e5f9fea58c84f3a8dca7c264",
          "url": "https://github.com/unicode-org/icu/commit/a6efa924ad04f7e3c7ad325237bf026b39abe4e8"
        },
        "date": 1707314184386,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.96762746573398,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "0b66fada30eeb572a75f7266db56326600c437a2",
          "message": "ICU-22633 Fix integer overflow inside Calendar code\n\nSee #2806",
          "timestamp": "2024-02-07T10:58:41-08:00",
          "tree_id": "65a74dc2d406474c298c0a40ec731d351f5ee698",
          "url": "https://github.com/unicode-org/icu/commit/0b66fada30eeb572a75f7266db56326600c437a2"
        },
        "date": 1707335815191,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.48795358836847,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "a210fc8351141c2e0547db1020f19dfd11c62462",
          "message": "ICU-22651 Add a docstring for LocalOpenPointer.",
          "timestamp": "2024-02-07T21:47:13+01:00",
          "tree_id": "7aa0e42d2f2f7c37a21414f9661c3e075ca9619d",
          "url": "https://github.com/unicode-org/icu/commit/a210fc8351141c2e0547db1020f19dfd11c62462"
        },
        "date": 1707339504366,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 186.52014752550005,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "699555a5bd43b439aba370288fa90f5fcb0705f8",
          "message": "ICU-22520 Use a ByteSink append buffer instead of a local CharString.\n\nThese functions that eventually write their output to a ByteSink need a\nsmall temporary buffer for processing the subtag they're about to write\nand currently use a local CharString object to provide this buffer,\nwhich then gets written to the ByteSink and discarded.\n\nThis intermediate step is unnecessary as a ByteSink can provide an\nappend buffer which can be used instead, eliminating the need to\nallocate a local temporary buffer and to copy the data around.\n\nThis approach also makes it natural to split the processing into two\nsteps, first calculating the length of the subtag, then processing it,\nwhich makes it possible to return early when no output is requested.",
          "timestamp": "2024-02-08T00:38:09+01:00",
          "tree_id": "687e9039d6c4bc244a46afe4c68d2213490bed86",
          "url": "https://github.com/unicode-org/icu/commit/699555a5bd43b439aba370288fa90f5fcb0705f8"
        },
        "date": 1707349679498,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.27231977068857,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "committer": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "distinct": true,
          "id": "ba1208e49b10c808651cd40bc05e0138c5291194",
          "message": "ICU-22518 Add a flag to export the output of the reference implementation from the old segmentation monkey tests",
          "timestamp": "2024-02-08T04:54:33+01:00",
          "tree_id": "b81baf84994204a358f7267689b22f0f54f25627",
          "url": "https://github.com/unicode-org/icu/commit/ba1208e49b10c808651cd40bc05e0138c5291194"
        },
        "date": 1707365054100,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.68794621095697,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "63ae786bf79e10568b8d20ad770c38e17c053e20",
          "message": "ICU-22520 Refactor function macros into inline functions.\n\nThis is to facilitate further refactoring of the locale code.",
          "timestamp": "2024-02-08T14:24:48+01:00",
          "tree_id": "075a9bcb2f9fdc9f53963a349456eb9ec23eead8",
          "url": "https://github.com/unicode-org/icu/commit/63ae786bf79e10568b8d20ad770c38e17c053e20"
        },
        "date": 1707399305374,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 223.50049902395557,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "cd251ee62e08dd634162a269c54374be99b51c86",
          "message": "ICU-22659 tzdata2024a updates in ICU repo",
          "timestamp": "2024-02-08T15:00:39-05:00",
          "tree_id": "efd70214f351f283616c21f52d058f86192e9e92",
          "url": "https://github.com/unicode-org/icu/commit/cd251ee62e08dd634162a269c54374be99b51c86"
        },
        "date": 1707422875645,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.3234155755469,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "abcb80fd536b4505d8f74209aca71656a7aa54e7",
          "message": "ICU-22615 Test TimeZoneNames API will not assert with non ASCII.\n\nAdd tests and return error when the ID is non ASCII",
          "timestamp": "2024-02-08T23:37:14-08:00",
          "tree_id": "90ed3c527ea7ff05be9931355803f818ebf1ea90",
          "url": "https://github.com/unicode-org/icu/commit/abcb80fd536b4505d8f74209aca71656a7aa54e7"
        },
        "date": 1707464800967,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.02169235891867,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "2c16b037cf6a9c7b01addbd4ca2873f164485f39",
          "message": "ICU-22557 Add kxv_IN to build-icu-data.xml, update generate stubs",
          "timestamp": "2024-02-09T09:40:52-08:00",
          "tree_id": "b673ae1bea308a76a3e0d999945aa2dd9b3199b1",
          "url": "https://github.com/unicode-org/icu/commit/2c16b037cf6a9c7b01addbd4ca2873f164485f39"
        },
        "date": 1707501089473,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.07490090218351,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "61fdbe0d0699e8b5678e1bb7a36020203eac729f",
          "message": "ICU-22520 Refactor code to remove the use of goto for error handling.\n\nThis is to facilitate further refactoring of the locale code, goto\ndoesn't play all too well with C++ memory handling.",
          "timestamp": "2024-02-09T18:47:22+01:00",
          "tree_id": "a75c950f6f286e7d652771b556f786ddac923676",
          "url": "https://github.com/unicode-org/icu/commit/61fdbe0d0699e8b5678e1bb7a36020203eac729f"
        },
        "date": 1707502819419,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 194.65437561115363,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "69c8e12642fb628e7b6643720c34e1eb83899859",
          "message": "ICU-22520 Remove local custom code for parsing variant subtags.\n\nNow when the parseTagString() helper function just is a wrapper over\nulocimp_getSubtags() it can be replaced by calling that function\ndirectly instead and letting it handle variant subtags as well.",
          "timestamp": "2024-02-09T20:26:09+01:00",
          "tree_id": "cd8533c16f9a8b5c3fa3750e01d986705cd967be",
          "url": "https://github.com/unicode-org/icu/commit/69c8e12642fb628e7b6643720c34e1eb83899859"
        },
        "date": 1707507344899,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.87244103867513,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "939f08f2743dc7ec752e3a74758ccde0bb7a91d4",
          "message": "ICU-22520 Use C++ function signatures for internal C++ functions.\n\nSome of this code was originally written as C code and some of this code\nwas originally written as C++ code but made to resemble the then already\nexisting code that had once been C code. Changing it all to normal C++\nnow will make it easier and safer to work with going forward.\n\n· Use unnamed namespace instead of static.\n· Use reference instead of non-nullable pointer.\n· Use bool instead of UBool.\n· Use constexpr for static data.\n· Use U_EXPORT instead of U_CAPI or U_CFUNC.\n· Use the default calling convention instead of U_EXPORT2.",
          "timestamp": "2024-02-12T21:44:06+01:00",
          "tree_id": "a5ce266887bc2a225e1a34fc014831da5bc0dc59",
          "url": "https://github.com/unicode-org/icu/commit/939f08f2743dc7ec752e3a74758ccde0bb7a91d4"
        },
        "date": 1707771234125,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.8798570277553,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "b24b251bca2f4decd0d25ece718e9adeaa63c468",
          "message": "ICU-22633 Fix more int overflow issues in calendar",
          "timestamp": "2024-02-13T17:24:18-08:00",
          "tree_id": "6b64c9452543823e502731cc5dfedc1f40956812",
          "url": "https://github.com/unicode-org/icu/commit/b24b251bca2f4decd0d25ece718e9adeaa63c468"
        },
        "date": 1707917083890,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.60038736605645,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "164a56b73613159111d32dbb8497c6f5e96fa5cf",
          "message": "ICU-22533 move custom normalization page from Sites to GitHub",
          "timestamp": "2024-02-14T09:35:14-08:00",
          "tree_id": "9b052ccbe6d8b023daf30b675cda347af43813b8",
          "url": "https://github.com/unicode-org/icu/commit/164a56b73613159111d32dbb8497c6f5e96fa5cf"
        },
        "date": 1707932271323,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.05930449240356,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "90b2eed71aa31ff5d4859502f92922f419d5b510",
          "message": "ICU-22533 compact norm16 tables\n\nSee #2827",
          "timestamp": "2024-02-14T14:49:43-08:00",
          "tree_id": "0b8939e79071e9ea20e3823a83e7bbf11a6fd317",
          "url": "https://github.com/unicode-org/icu/commit/90b2eed71aa31ff5d4859502f92922f419d5b510"
        },
        "date": 1707951348121,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.39496798861563,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "df46d089d545bbb25cce9e01f94c154e3124917d",
          "message": "ICU-22314 Refactor Azure CI into workflows conditional on modified paths\n\nSee #2701",
          "timestamp": "2024-02-16T15:23:47+05:30",
          "tree_id": "5897e253e9b21b8c0482b4b2ab8812069f68b761",
          "url": "https://github.com/unicode-org/icu/commit/df46d089d545bbb25cce9e01f94c154e3124917d"
        },
        "date": 1708077518359,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.45285774371567,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "8acebe4a0c6986814c03497ee303becfb3c153c5",
          "message": "ICU-22314 Add PR triggers for Azure pipelines\n\nSee #2835",
          "timestamp": "2024-02-21T13:38:48+05:30",
          "tree_id": "577bf466d30579d5773c3ed56eb306dcb2ab6406",
          "url": "https://github.com/unicode-org/icu/commit/8acebe4a0c6986814c03497ee303becfb3c153c5"
        },
        "date": 1708503096078,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 183.66323089218355,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "diegogy@google.com",
            "name": "Diego Gutierrez Yepiz",
            "username": "Diego1149"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "f3e50a7624384e325b9fb2c133a887c505308301",
          "message": "ICU-22582 Avoid synchronizing in RuleBasedBreakIterator and ULocale unless strictly necessary\n\nSee #2775",
          "timestamp": "2024-02-21T09:38:41-08:00",
          "tree_id": "98e355fecc333b6d0e2b66ac110db362a12d214d",
          "url": "https://github.com/unicode-org/icu/commit/f3e50a7624384e325b9fb2c133a887c505308301"
        },
        "date": 1708537286783,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.9880850896115,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "4633254f9ea434bb48a30fbcc98812eb561ff4eb",
          "message": "ICU-22314 Move Cygwin test to post-merge pipeline",
          "timestamp": "2024-02-22T11:23:17-08:00",
          "tree_id": "032c3b808d111452bbfae8a504696914a4339296",
          "url": "https://github.com/unicode-org/icu/commit/4633254f9ea434bb48a30fbcc98812eb561ff4eb"
        },
        "date": 1708630062477,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.8882586217263,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "fd5d6c97b1d0cff4a07db3c7e7ab04b20099e124",
          "message": "ICU-22532 Improve commit checker instructions\n\nSee #2622",
          "timestamp": "2024-02-23T16:38:14-06:00",
          "tree_id": "60a62e4a47c60e625c4950bc7593f7307213164f",
          "url": "https://github.com/unicode-org/icu/commit/fd5d6c97b1d0cff4a07db3c7e7ab04b20099e124"
        },
        "date": 1708728474465,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.18430129783152,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "3c82e6857c0bc0bbe93c0cdeaf03be3844dcad96",
          "message": "ICU-22676 Undefine move32 since it is interpreted system call with MSVC ARM64",
          "timestamp": "2024-02-26T08:55:31-08:00",
          "tree_id": "f5c231cecb726a1439e4834962579f491c072de0",
          "url": "https://github.com/unicode-org/icu/commit/3c82e6857c0bc0bbe93c0cdeaf03be3844dcad96"
        },
        "date": 1708967114757,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.37395927980734,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "6d15faab4806df96e6cba4a6bb0e9a2e67b4c630",
          "message": "ICU-22677 update CONTRIBUTING.md\n\n- https://github.com/unicode-org/.github/issues/12",
          "timestamp": "2024-02-26T11:44:35-06:00",
          "tree_id": "de4512e32457c48a61848ad3c63ceb31c2f5cc13",
          "url": "https://github.com/unicode-org/icu/commit/6d15faab4806df96e6cba4a6bb0e9a2e67b4c630"
        },
        "date": 1708969698341,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.67281768494414,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "ea1c6da07fa345dd485408caee51703bc95c0454",
          "message": "ICU-22677 update LICENSE and README.md and pom.xml\n\n- https://github.com/unicode-org/.github/issues/15\n- use Unicode-3.0 in pom instead of raw license link\n- https://github.com/unicode-org/.github/issues/15",
          "timestamp": "2024-02-26T14:34:59-06:00",
          "tree_id": "f428ed2e6edb51b10fcbfb60669bba6fa40e4b79",
          "url": "https://github.com/unicode-org/icu/commit/ea1c6da07fa345dd485408caee51703bc95c0454"
        },
        "date": 1708979998277,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.78675361830113,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "alta.liepa@gmail.com",
            "name": "Rūdolfs Mazurs",
            "username": "Mazurs"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "394341edba31196937036560c8f5a61736cd5636",
          "message": "ICU-22646 Update collation test for Latvian locale\n\nThis test is also relevant to issues ICU-12765 ICU-13508 ICU-20532",
          "timestamp": "2024-02-26T14:09:40-08:00",
          "tree_id": "00d620e77dd2a3017b0eb0b5f62e154f1bc2a707",
          "url": "https://github.com/unicode-org/icu/commit/394341edba31196937036560c8f5a61736cd5636"
        },
        "date": 1708985975036,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.21737527177723,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "201af462fc1f939f569c70e748f63bb36dd4b2a4",
          "message": "ICU-22675 Migrate from deprecated boxed primitive constructors to their replacements",
          "timestamp": "2024-02-26T16:39:05-08:00",
          "tree_id": "7d9554fa01f6c1eb18e6ec91fd12152f134d2e60",
          "url": "https://github.com/unicode-org/icu/commit/201af462fc1f939f569c70e748f63bb36dd4b2a4"
        },
        "date": 1708994648218,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.00450333335826,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "314f03eeaf7473bf86a29ece677050709138ca22",
          "message": "ICU-22532 Don't dereference nullptr (-Wtautological-undefined-compare).",
          "timestamp": "2024-02-27T14:11:38+01:00",
          "tree_id": "df28a8598aeca97427d249d0b72fad1880561c00",
          "url": "https://github.com/unicode-org/icu/commit/314f03eeaf7473bf86a29ece677050709138ca22"
        },
        "date": 1709040225803,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.2178644359629,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "ec800e7407493f65f3f5aee9bc7a657bb25850f1",
          "message": "ICU-22633 Return error if era is out of range",
          "timestamp": "2024-02-27T10:56:28-08:00",
          "tree_id": "b0ca1cbed057e4ae50b219bf5a7e09476612e60d",
          "url": "https://github.com/unicode-org/icu/commit/ec800e7407493f65f3f5aee9bc7a657bb25850f1"
        },
        "date": 1709060957101,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.00897752083654,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "3df505b5116218a7a4be976ba6bd8f802d84a7ff",
          "message": "ICU-22532 Increase Github Actions CI parallelism from 2 to 4",
          "timestamp": "2024-02-27T10:58:09-08:00",
          "tree_id": "4d130cf878d425c7f4f0e10ee2c31848590a3829",
          "url": "https://github.com/unicode-org/icu/commit/3df505b5116218a7a4be976ba6bd8f802d84a7ff"
        },
        "date": 1709061456632,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.41309226689944,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "c2b328267e2c92a7482775c3450bcfd9c3ba3d08",
          "message": "ICU-22532 Increase timeout for exhaustive ICU4J tests",
          "timestamp": "2024-02-27T10:58:18-08:00",
          "tree_id": "42ba8185b0e2d649253444fc20aee39ed6eedc3e",
          "url": "https://github.com/unicode-org/icu/commit/c2b328267e2c92a7482775c3450bcfd9c3ba3d08"
        },
        "date": 1709062246422,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.9074147669078,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "d1fa15bc1f91335427c9718bfa3609bdae4a9d38",
          "message": "ICU-22571 add Aran script code variant",
          "timestamp": "2024-02-27T14:23:59-08:00",
          "tree_id": "babe607f8fe1499e5a7e94262bccbda62fa5d69b",
          "url": "https://github.com/unicode-org/icu/commit/d1fa15bc1f91335427c9718bfa3609bdae4a9d38"
        },
        "date": 1709073232327,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.34213982009777,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "d271d3f269fc06059ddbcb7681567eed7acf35ea",
          "message": "ICU-21952 fix draft version of withoutLocale to ICU 75",
          "timestamp": "2024-02-27T17:03:46-08:00",
          "tree_id": "bf3eaeadd4f31dcc79b2e3fcb1ab637c4627cad1",
          "url": "https://github.com/unicode-org/icu/commit/d271d3f269fc06059ddbcb7681567eed7acf35ea"
        },
        "date": 1709082755141,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.95450394574726,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "fd5d6c97b1d0cff4a07db3c7e7ab04b20099e124",
          "message": "ICU-22532 Improve commit checker instructions\n\nSee #2622",
          "timestamp": "2024-02-23T16:38:14-06:00",
          "tree_id": "60a62e4a47c60e625c4950bc7593f7307213164f",
          "url": "https://github.com/unicode-org/icu/commit/fd5d6c97b1d0cff4a07db3c7e7ab04b20099e124"
        },
        "date": 1709132306707,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.6265099589608,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dragan@unicode.org",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "committer": {
            "email": "131725218+DraganBesevic@users.noreply.github.com",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "distinct": true,
          "id": "a1925abf4f070541d7fb795acdc5be0e7cae6211",
          "message": "ICU-22534 CLDR 45 alpha2 integration to ICU",
          "timestamp": "2024-02-28T08:28:08-08:00",
          "tree_id": "0abd28798029ee7d35b38061fcda8f5b1de32c78",
          "url": "https://github.com/unicode-org/icu/commit/a1925abf4f070541d7fb795acdc5be0e7cae6211"
        },
        "date": 1709138961738,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.9860293044141,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "0d7cedc0dd7cbe595ba415bbad226262a5358a3d",
          "message": "ICU-22532 Trigger CI workflows when workflow definitions change",
          "timestamp": "2024-02-28T12:50:43-05:00",
          "tree_id": "fb2b0a51f868dc33626949c35cb4fd4a7f59aaaa",
          "url": "https://github.com/unicode-org/icu/commit/0d7cedc0dd7cbe595ba415bbad226262a5358a3d"
        },
        "date": 1709143144324,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.20395823942235,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "ceca6c412016f3b7581db2a4385a4a34d38878d9",
          "message": "ICU-22274 display tz db version on the test machine.",
          "timestamp": "2024-02-28T11:07:32-08:00",
          "tree_id": "2742a04788fee4f5ebfd1baf1bec021addaf7dea",
          "url": "https://github.com/unicode-org/icu/commit/ceca6c412016f3b7581db2a4385a4a34d38878d9"
        },
        "date": 1709147785988,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.66197600918144,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "09ccfb99566673e8dd731d723e6987ee704a123c",
          "message": "ICU-22534 ICU4J 75 frontload: API status update",
          "timestamp": "2024-02-28T11:38:41-08:00",
          "tree_id": "d99284d713d0ebde9e8c2cb4228d719bf46c07d0",
          "url": "https://github.com/unicode-org/icu/commit/09ccfb99566673e8dd731d723e6987ee704a123c"
        },
        "date": 1709149625873,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.78113098087175,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "0563859d8cf7c0d3dfea3b576adca25667d77c17",
          "message": "ICU-22679 Optimize calendar code for edge cases\n\nSee #2853",
          "timestamp": "2024-02-28T17:08:24-08:00",
          "tree_id": "9dda80fe6701d9733fcad429d95e8d4934836be9",
          "url": "https://github.com/unicode-org/icu/commit/0563859d8cf7c0d3dfea3b576adca25667d77c17"
        },
        "date": 1709169516034,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.87553169604502,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "7d636aecf7b71724e19a1c4cb6b148eb60fe32e1",
          "message": "ICU-22655 Implement \"special\" conversion for speed-beaufort, part 1 icu4j",
          "timestamp": "2024-02-28T12:57:33-08:00",
          "tree_id": "0749e3e9d3e5cab4cf126ca2fad0c21fe6492ab4",
          "url": "https://github.com/unicode-org/icu/commit/7d636aecf7b71724e19a1c4cb6b148eb60fe32e1"
        },
        "date": 1709215362805,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 188.05494984408648,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "96fb7ae73a78aec7e1a5f3a51e830fa44e0d12df",
          "message": "ICU-22534 ICU4J 75 frontload API change report",
          "timestamp": "2024-02-29T08:49:43-08:00",
          "tree_id": "1e9219cff79569154a8a687958c3f398d584af06",
          "url": "https://github.com/unicode-org/icu/commit/96fb7ae73a78aec7e1a5f3a51e830fa44e0d12df"
        },
        "date": 1709225571422,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.24992216029753,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "137b4c9e47bfb8db4d65f92082c366735f3afcf2",
          "message": "ICU-22556 Update configure files from configure.ac using autoreconf.",
          "timestamp": "2024-02-29T19:43:43+01:00",
          "tree_id": "42b58894ebe7e8ff60bb7834b62f8d38cd537f64",
          "url": "https://github.com/unicode-org/icu/commit/137b4c9e47bfb8db4d65f92082c366735f3afcf2"
        },
        "date": 1709232639177,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.14121944444793,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "devnexen@gmail.com",
            "name": "David Carlier",
            "username": "devnexen"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "35353f2d7fcc3041ea4ebf03974146026cff2e43",
          "message": "ICU-22671 format_date should use c++ nullptr instead of 0 for udat_open/DateFormat::create\n\n- [x] Required: Issue filed: https://unicode-org.atlassian.net/browse/ICU-22671\n- [x] Required: The PR title must be prefixed with a JIRA Issue number. <!-- For example: \"ICU-1234 Fix xyz\" -->\n- [x] Required: The PR description must include the link to the Jira Issue, for example by completing the URL in the first checklist item\n- [x] Required: Each commit message must be prefixed with a JIRA Issue number. <!-- For example: \"ICU-1234 Fix xyz\" -->\n- [ ] Issue accepted (done by Technical Committee after discussion)\n- [ ] Tests included, if applicable\n- [ ] API docs and/or User Guide docs changed or added, if applicable",
          "timestamp": "2024-02-29T20:02:20+01:00",
          "tree_id": "87eb69e1268f5ed4c01bde0e688a6763d9d6eb61",
          "url": "https://github.com/unicode-org/icu/commit/35353f2d7fcc3041ea4ebf03974146026cff2e43"
        },
        "date": 1709233979729,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.492445390021,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "929cd9bb4f8c24e0f41e82f9c18bc1abd8b9019b",
          "message": "ICU-22520 Standardize return on error for all locale functions.\n\n· No function should do anything if an error has already occurred.\n· On error, a value of 0, nullptr, {}, etc., should be returned.\n· Values shouldn't have overloaded meanings (eg. index or found).\n· Values that are never used should not be returned at all.",
          "timestamp": "2024-02-29T20:42:03+01:00",
          "tree_id": "daece93bdabb4edb08a320d09120ccbf862a1aaf",
          "url": "https://github.com/unicode-org/icu/commit/929cd9bb4f8c24e0f41e82f9c18bc1abd8b9019b"
        },
        "date": 1709236238696,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.41096362543541,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "aa3e49fa9a706e332528e807fe327b99f271f61b",
          "message": "ICU-22274 Add zdump info while env test fail",
          "timestamp": "2024-02-29T14:36:14-08:00",
          "tree_id": "329040e8e7af4b84568bb10d0ac6a2d676478b17",
          "url": "https://github.com/unicode-org/icu/commit/aa3e49fa9a706e332528e807fe327b99f271f61b"
        },
        "date": 1709246772634,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.7886809503283,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "71b9b88200db7524f9693a3c8c9d3a5cb9d3d84a",
          "message": "ICU-22319 Fix number range semanticallyEquivalent\n\nSee #2385",
          "timestamp": "2024-03-04T08:23:00-08:00",
          "tree_id": "9fe77b95af0f062eecf225a96aca74cfd9cc5375",
          "url": "https://github.com/unicode-org/icu/commit/71b9b88200db7524f9693a3c8c9d3a5cb9d3d84a"
        },
        "date": 1709570048972,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 184.16808808761945,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "37526240e1be1ec8036f4a80772185ca83b1e3b2",
          "message": "ICU-22274 Mark known issue for 3 timezones for EnvTest\n\ntz2024a change \"Asia/Qostanay\" \"Asia/Almaty\" but test machines has\nnot yet update their zoneinfo to 2024a so we mark them as known issues\n\nextern long timezone; in <time.h> (set man tzset on Linux shell)\nreturns wrong value when TZ=America/Scoresbysund",
          "timestamp": "2024-03-04T11:06:39-08:00",
          "tree_id": "4d200ef8898218e9b9c1d2ede05e99852eea5e10",
          "url": "https://github.com/unicode-org/icu/commit/37526240e1be1ec8036f4a80772185ca83b1e3b2"
        },
        "date": 1709579628065,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.90447643136784,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "73744ea41f03b20a9d6dc6ed09a56781e89dacb4",
          "message": "ICU-22633 Fix overflow cause by large AM PM value\n\nFix https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=66771",
          "timestamp": "2024-03-04T13:48:24-08:00",
          "tree_id": "3ded34e62e794fcf69617141e91ea325fc6602da",
          "url": "https://github.com/unicode-org/icu/commit/73744ea41f03b20a9d6dc6ed09a56781e89dacb4"
        },
        "date": 1709589462747,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.65374389845755,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "232362bf17948316c6a50035b8d79a31bb304ffa",
          "message": "ICU-22520 Use operator* instead of calling std::optional::value().\n\nThere's a subtle difference between these two ways of accessing the\nvalue of an optional and that is that the value() method can throw an\nexception if there isn't any value, but operator* won't do that (it's\njust undefined behavior if there isn't any value).\n\nICU4C code never tries to access any optional value without first\nchecking that it exists, but the ability of the value() method to throw\nan exception in case there wasn't any such check first is the reason why\nstd::exception symbols previously could show up in debug builds.\n\nThis reverts the changes that were made to dependencies.txt by\ncommit dc70b5a056b618c014c71e8bfd45f3dd9145e9fe.",
          "timestamp": "2024-03-04T23:40:15+01:00",
          "tree_id": "44e638be633151a9d900c294538752dcc64edc5d",
          "url": "https://github.com/unicode-org/icu/commit/232362bf17948316c6a50035b8d79a31bb304ffa"
        },
        "date": 1709592541891,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.4141730044014,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "c610d7f986d9f2ccd0084adef4ed4a6cd09ccbd1",
          "message": "ICU-22534 Promote (almost) all @draft ICU 73 APIs to @stable ICU 73",
          "timestamp": "2024-03-04T18:05:29-08:00",
          "tree_id": "18952de2fc86ef071519ee54af784ec2638d7717",
          "url": "https://github.com/unicode-org/icu/commit/c610d7f986d9f2ccd0084adef4ed4a6cd09ccbd1"
        },
        "date": 1709604811175,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 185.02799482439485,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "02a1bfc59f1bf1e42b5d1b6c47748dfa630db691",
          "message": "ICU-22520 Refactor CheckedArrayByteSink & u_terminateChars into helper.\n\nThe repeated sequence of allocating a CheckedArrayByteSink, calling some\nfunction that writes into this, then checking for overflow and returning\nthrough u_terminateChars() can all be moved into a single shared helper\nfunction.",
          "timestamp": "2024-03-05T20:09:54+01:00",
          "tree_id": "85abd6c4dac2643292ea7b98160cb22c4ab4e08a",
          "url": "https://github.com/unicode-org/icu/commit/02a1bfc59f1bf1e42b5d1b6c47748dfa630db691"
        },
        "date": 1709666218726,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.66891213782083,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "7bc202ae87394fd12879439bde0d5dacc4409f46",
          "message": "ICU-22534 BRS#27 scrub closed issues (frontload)",
          "timestamp": "2024-03-05T11:15:28-08:00",
          "tree_id": "23afc2cd20403935b2ed192e8a1d98d77ab71d01",
          "url": "https://github.com/unicode-org/icu/commit/7bc202ae87394fd12879439bde0d5dacc4409f46"
        },
        "date": 1709668122155,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.6281757771111,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "53568e8dfc4cf2b7a93e196b9d098d98767d7d14",
          "message": "ICU-22520 Refactor CharString & CharStringByteSink into helper.\n\nThe repeated sequence of allocating a CharString and CharStringByteSink,\nbefore calling some function that writes into this, can be moved into a\nsingle shared helper function which then is used to give all ulocimp.h\nfunctions that write to ByteSink an overload that instead returns a\nCharString, to make call sites look like perfectly normal C++ code.",
          "timestamp": "2024-03-05T23:44:50+01:00",
          "tree_id": "05f2d9648d01aee7166cb6265e5e15e4d42a7e38",
          "url": "https://github.com/unicode-org/icu/commit/53568e8dfc4cf2b7a93e196b9d098d98767d7d14"
        },
        "date": 1709679267411,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 185.41195457195047,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "c0c46988c5f64ddea9c48df18c50b3aec2738414",
          "message": "ICU-22612 Update configure files from configure.ac using autoreconf.",
          "timestamp": "2024-03-06T10:56:53+01:00",
          "tree_id": "d2dd5735c9db9f01bc99a2229b5ba695336746b3",
          "url": "https://github.com/unicode-org/icu/commit/c0c46988c5f64ddea9c48df18c50b3aec2738414"
        },
        "date": 1709719549069,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.42655885408422,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "e0a711c0a4c7684651cbd02ae247590aa551bc54",
          "message": "ICU-22633 Fix Hebrew overflow issue",
          "timestamp": "2024-03-06T14:52:19-08:00",
          "tree_id": "b7fdb0b7e1a286ccee7480ec69d92328383e36b7",
          "url": "https://github.com/unicode-org/icu/commit/e0a711c0a4c7684651cbd02ae247590aa551bc54"
        },
        "date": 1709766112264,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.4739421964693,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "89cf56333f3361470e4250e594c16c96b9634549",
          "message": "ICU-22534 BRS#27 fix CI-Exhaustive-Main breakage for locale qaa",
          "timestamp": "2024-03-07T13:47:49+01:00",
          "tree_id": "94a75a796461a1fdb8406e8ebea6cbf7cadf6836",
          "url": "https://github.com/unicode-org/icu/commit/89cf56333f3361470e4250e594c16c96b9634549"
        },
        "date": 1709816175530,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.468381376622,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "a0cdb9cda5d0bcd86c06ae7a73fa45682cf90dea",
          "message": "ICU-22597 Add MSVC build bot to build ICU4C without exceptions\n\nSee #2829",
          "timestamp": "2024-03-07T16:14:16+01:00",
          "tree_id": "64d107e417ae3502d6d6d12430367dd052d7d031",
          "url": "https://github.com/unicode-org/icu/commit/a0cdb9cda5d0bcd86c06ae7a73fa45682cf90dea"
        },
        "date": 1709824760867,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 187.84285876103547,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "3a88e99a2735d1689e9fe05ad8811b09d5559094",
          "message": "ICU-22520 Add a StringByteSink<CharString> template specialization.\n\nThis makes it possible to call public functions that take a string class\nas a template parameter and return an object of that class (implemented\nthrough the StringByteSink helper class) also with the CharString class,\neven though this class doesn't actually provide the public API required\nby StringByteSink.\n\nThis makes it possible to use such more modern APIs also internally.",
          "timestamp": "2024-03-07T17:35:48+01:00",
          "tree_id": "94bde9b96a2121e8d67db79865f6813e78bbe032",
          "url": "https://github.com/unicode-org/icu/commit/3a88e99a2735d1689e9fe05ad8811b09d5559094"
        },
        "date": 1709829887755,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.8772468161793,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "ebaf3e9f7584103f39bb74219b048d1d98ec40ef",
          "message": "ICU-22534 BRS#19 Update ICU4C API Change Report (frontloading)",
          "timestamp": "2024-03-07T14:37:06-08:00",
          "tree_id": "6e78186f138c4a25e6ba161beaf8e3effe6c4e44",
          "url": "https://github.com/unicode-org/icu/commit/ebaf3e9f7584103f39bb74219b048d1d98ec40ef"
        },
        "date": 1709851535749,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.31003438562848,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "784056dfdbb38e88d5d768c1cc4b5e32ba3b72b3",
          "message": "ICU-22633 Fix overflow in Chinese calendar\n\nFix issue found by https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=67256",
          "timestamp": "2024-03-07T16:03:00-08:00",
          "tree_id": "c96e10303abd6485e847030e7f713e193d12cd6f",
          "url": "https://github.com/unicode-org/icu/commit/784056dfdbb38e88d5d768c1cc4b5e32ba3b72b3"
        },
        "date": 1709856765331,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.17809468703643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "4c664b2180fc0d8b597f59a6747a7665a118d833",
          "message": "ICU-22534 Changed ExternalBreakEngine so that it's surrounded by U_HIDE_INTERNAL_API instead of U_HIDE_DRAFT_API.",
          "timestamp": "2024-03-08T17:49:33-08:00",
          "tree_id": "85ddcf2bb416aee6596da800f59e7ffe6c429e46",
          "url": "https://github.com/unicode-org/icu/commit/4c664b2180fc0d8b597f59a6747a7665a118d833"
        },
        "date": 1709949429251,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.47968507023734,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "ceee4f0b46415f325a9afbe890dea1434446bb4e",
          "message": "ICU-22655 Implement \"special\" conversion for speed-beaufort, part 2 icu4c",
          "timestamp": "2024-03-09T19:52:42-08:00",
          "tree_id": "5914cfd2fb3673b0027b118b8dd93a3685f161a0",
          "url": "https://github.com/unicode-org/icu/commit/ceee4f0b46415f325a9afbe890dea1434446bb4e"
        },
        "date": 1710043336300,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 222.98104550749295,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tushuhei@gmail.com",
            "name": "Shuhei Iitsuka",
            "username": "tushuhei"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "37ecee3a0c80bb10108e7e6d4a91989756384a67",
          "message": "ICU-22666 Update ML model to improve Japanese phrase breaking quality",
          "timestamp": "2024-03-11T12:00:03-07:00",
          "tree_id": "d7f8acdb1897a739dafa511a410d51885b84e5a7",
          "url": "https://github.com/unicode-org/icu/commit/37ecee3a0c80bb10108e7e6d4a91989756384a67"
        },
        "date": 1710184159875,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.24391286375007,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "4c664b2180fc0d8b597f59a6747a7665a118d833",
          "message": "ICU-22534 Changed ExternalBreakEngine so that it's surrounded by U_HIDE_INTERNAL_API instead of U_HIDE_DRAFT_API.",
          "timestamp": "2024-03-08T17:49:33-08:00",
          "tree_id": "85ddcf2bb416aee6596da800f59e7ffe6c429e46",
          "url": "https://github.com/unicode-org/icu/commit/4c664b2180fc0d8b597f59a6747a7665a118d833"
        },
        "date": 1710254877159,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.68238947857336,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "18c7d48b3e527ccead8ba93abd6059dfe1329258",
          "message": "ICU-22533 Add docs on Continuous Integration\n\nSee #2876",
          "timestamp": "2024-03-12T09:14:24-07:00",
          "tree_id": "1cf11c531df5cddb4a73e09f28e5d5ce3e6bd90e",
          "url": "https://github.com/unicode-org/icu/commit/18c7d48b3e527ccead8ba93abd6059dfe1329258"
        },
        "date": 1710260552947,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.51650180559105,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "d43d216febe18d37ad840cbac3f953b989b65965",
          "message": "ICU-22633 Test and fix int32_t overflow for Calendar set\n\nAdd test to set with INT32_MAX and INT32_MIN then call getTime()\nand fix all the undefined errors.",
          "timestamp": "2024-03-12T16:46:53-07:00",
          "tree_id": "8df8456fe2ad6a4a7d889b4e460b61d4f2803cc9",
          "url": "https://github.com/unicode-org/icu/commit/d43d216febe18d37ad840cbac3f953b989b65965"
        },
        "date": 1710295878702,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.16249428908995,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "1cedbbd90d7dc1aa11576991dd7c169138d6b236",
          "message": "ICU-22534 Fixed a couple issues from the API-promotions PR that Frank found in code review.",
          "timestamp": "2024-03-12T16:47:57-07:00",
          "tree_id": "322b316783d7b7c12269b7c2379f45871e785549",
          "url": "https://github.com/unicode-org/icu/commit/1cedbbd90d7dc1aa11576991dd7c169138d6b236"
        },
        "date": 1710296280528,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.71068743255609,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "00072801a51fce1014f86d47eeebdf80ada14ff3",
          "message": "ICU-22687 Fix genren.pl not to skip C++ functions on Mac",
          "timestamp": "2024-03-12T16:56:26-07:00",
          "tree_id": "7435bf030ab6bd59cb74d70f0d1d0024a6750e55",
          "url": "https://github.com/unicode-org/icu/commit/00072801a51fce1014f86d47eeebdf80ada14ff3"
        },
        "date": 1710297123501,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.59658140663893,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "2a1853c9a9e92df4e2d2b6182c66b4448d06bb3f",
          "message": "ICU-22621 Clang-Tidy: modernize-use-emplace\n\nhttps://releases.llvm.org/17.0.1/tools/clang/tools/extra/docs/clang-tidy/checks/modernize/use-emplace.html",
          "timestamp": "2024-03-13T16:31:47+01:00",
          "tree_id": "b05e0fb8daa1905b925ad7c7f7c087aade47f7ae",
          "url": "https://github.com/unicode-org/icu/commit/2a1853c9a9e92df4e2d2b6182c66b4448d06bb3f"
        },
        "date": 1710344550496,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.30020974597284,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "9a912bb51be6b0a6eec37d005e38648e06b8f43d",
          "message": "ICU-22633 Add more Calendar overflow tests\n\nTest set+set, set+add, set+roll, add+add, roll+roll\nFix more int32_t overflow problems.\n\nOptimize both Java and C++ Hebrew Calendar month/year advancement by first consider\nevery 235 months is 19 years before iteration.",
          "timestamp": "2024-03-13T15:48:25-07:00",
          "tree_id": "45a0444043426c272f9739746c736db8cde3db10",
          "url": "https://github.com/unicode-org/icu/commit/9a912bb51be6b0a6eec37d005e38648e06b8f43d"
        },
        "date": 1710370734913,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.90022015966835,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dragan@unicode.org",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "committer": {
            "email": "131725218+DraganBesevic@users.noreply.github.com",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "distinct": true,
          "id": "b396885aae1f515df35688ea72943583c0268836",
          "message": "ICU-22534 Integrate CLDR 45 release alpha 3, part 4, update supplementalData to rollback root changes",
          "timestamp": "2024-03-14T08:30:09-07:00",
          "tree_id": "f69bb6ee4a6dc2f409eb5f8f54bc026d0ddd6e71",
          "url": "https://github.com/unicode-org/icu/commit/b396885aae1f515df35688ea72943583c0268836"
        },
        "date": 1710430874236,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.19145474865587,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "2b6ddc50feafc3caca086a5ef03a85c4af034d52",
          "message": "ICU-22202 Fixed DateIntervalFormat to solve a couple \"conflicting fields\" errors.",
          "timestamp": "2024-03-14T14:31:48-07:00",
          "tree_id": "cd7f99e89e13c1fba145f6986a4e88a0c337a566",
          "url": "https://github.com/unicode-org/icu/commit/2b6ddc50feafc3caca086a5ef03a85c4af034d52"
        },
        "date": 1710452496605,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 187.05054555094006,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "c771fc444f2338479b29e0eeea70e94fec13f154",
          "message": "ICU-22523 Cut down the large tables in the APIs docs for SimpleDateFormat and included a link to the full table\nin the LDML spec.",
          "timestamp": "2024-03-14T14:32:29-07:00",
          "tree_id": "80ad61486be23ce247654c1c6bfd12026cae9169",
          "url": "https://github.com/unicode-org/icu/commit/c771fc444f2338479b29e0eeea70e94fec13f154"
        },
        "date": 1710453718873,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.38866298324012,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "104214aeae7c944070ff886eed787ac24287d334",
          "message": "ICU-22532 Use previous Ubuntu version for ICU4C in GH Actions CI for now",
          "timestamp": "2024-03-14T16:20:49-07:00",
          "tree_id": "2fe1713b545f938b9613e074d8a2943b295ce169",
          "url": "https://github.com/unicode-org/icu/commit/104214aeae7c944070ff886eed787ac24287d334"
        },
        "date": 1710459114707,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.8252564210989,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "de9910659dbfe7c5113c2dc1c93e989b8c1978b5",
          "message": "ICU-22661 Limit the size of variants in Locale\n\nSee #2821",
          "timestamp": "2024-03-14T16:23:51-07:00",
          "tree_id": "fafc322e63a947dd377e983fc63b31becd8396f6",
          "url": "https://github.com/unicode-org/icu/commit/de9910659dbfe7c5113c2dc1c93e989b8c1978b5"
        },
        "date": 1710460119482,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.72868503862804,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "5401c12018bc3a46be70a3c6c6492a20b373d7e2",
          "message": "ICU-22621 Clang-Tidy: modernize-use-nullptr\n\nhttps://releases.llvm.org/17.0.1/tools/clang/tools/extra/docs/clang-tidy/checks/modernize/use-nullptr.html",
          "timestamp": "2024-03-15T14:31:54+01:00",
          "tree_id": "b9d654baf70786bce9d43ae0ea8fddb6d58db1e1",
          "url": "https://github.com/unicode-org/icu/commit/5401c12018bc3a46be70a3c6c6492a20b373d7e2"
        },
        "date": 1710510115266,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.20951123074624,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "f7099878188df5e054ea44b9d52cf89d2da176f7",
          "message": "ICU-22679 Refactor Islamic Calendar and Optmize starting condition of loop.\n\nRefactor different rules into implementation of private interface Algorithm.\nSince IslamicCalendar is public in Java (not in C++), we cannot put them into different subclass of Calendar and have to let them use the same class and object since the caller UNFORTUNALLY can call setCalculationType and setCivil to change the object to different rule. With this refactoring, we group the formula for the same rule into the same Algorithm\nimplementaiton and now we only do the if/switch check in the constructor or during the call of setCalculationType and setCivil only. The calculation operation is then just delegate the work to the assigned concrete Algorithm.\n\nImprove the efficency while the month is very large. Apply the same optimization in PR 2853 which estimate starting point of year iteration based on an inverse calculation.\n\nICU-22679 change parameter name\n\nICU-22679 Use Consumer",
          "timestamp": "2024-03-15T15:01:51-07:00",
          "tree_id": "42fa3f6af46f32cb581e5eb0847bc217a07ea48c",
          "url": "https://github.com/unicode-org/icu/commit/f7099878188df5e054ea44b9d52cf89d2da176f7"
        },
        "date": 1710540396758,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.20319917277203,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "a1c5294ae73230fea9d70cb7eeecc53acacd873d",
          "message": "ICU-22679 Remove duplicate code inside Islamic calendar",
          "timestamp": "2024-03-15T16:25:52-07:00",
          "tree_id": "cca91640d34b671efca208c73ea3089ee0f5ff76",
          "url": "https://github.com/unicode-org/icu/commit/a1c5294ae73230fea9d70cb7eeecc53acacd873d"
        },
        "date": 1710545949760,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.61084740682605,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "committer": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "distinct": true,
          "id": "80b33416e80009f7a20b549be9ec8c1552b45b08",
          "message": "ICU-22686 Fix Unit preferences calculation in Java\n\nSee #2899",
          "timestamp": "2024-03-18T13:23:37+01:00",
          "tree_id": "b44ec3f768b4303b0b5cf0e2d6e8c17f0de914cb",
          "url": "https://github.com/unicode-org/icu/commit/80b33416e80009f7a20b549be9ec8c1552b45b08"
        },
        "date": 1710765467117,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 187.9964458220497,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "committer": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "distinct": true,
          "id": "4405c543adc0c50d9b2a7f6c5c1fd05f5bfdc8fb",
          "message": "ICU-22686 Fix Unit preferences calculation in Cpp",
          "timestamp": "2024-03-18T13:23:58+01:00",
          "tree_id": "1485061abb1ccc2103e964ce3a7444f13e7edff8",
          "url": "https://github.com/unicode-org/icu/commit/4405c543adc0c50d9b2a7f6c5c1fd05f5bfdc8fb"
        },
        "date": 1710765690040,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.3447298707757,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "ce052f52c2d1d56a8b290dc67773c65204d48f1e",
          "message": "ICU-22621 Clang-Tidy: readability-delete-null-pointer\n\nhttps://releases.llvm.org/17.0.1/tools/clang/tools/extra/docs/clang-tidy/checks/readability/delete-null-pointer.html",
          "timestamp": "2024-03-18T17:14:23+01:00",
          "tree_id": "1885f970a8b7961735c48998405254c4395d60e5",
          "url": "https://github.com/unicode-org/icu/commit/ce052f52c2d1d56a8b290dc67773c65204d48f1e"
        },
        "date": 1710779078019,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 186.17332005083236,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "281d8ef140d7eb2ac79065c829b5bad805a4d1df",
          "message": "ICU-22152 Uncommented a bunch of commented-out test cases in ULocaleCollationTest.TestNameList() and made them pass again.",
          "timestamp": "2024-03-18T17:01:14-07:00",
          "tree_id": "86cf42effcb33f76adfeb309a4624d4a02718cee",
          "url": "https://github.com/unicode-org/icu/commit/281d8ef140d7eb2ac79065c829b5bad805a4d1df"
        },
        "date": 1710806737565,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.91248398700083,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "16b87419ed361351c58e3a33b618b02e8464fd10",
          "message": "ICU-22612 remove obsolete comment",
          "timestamp": "2024-03-18T17:13:53-07:00",
          "tree_id": "798dfae26451788e8069a50fba966d56e662a5ec",
          "url": "https://github.com/unicode-org/icu/commit/16b87419ed361351c58e3a33b618b02e8464fd10"
        },
        "date": 1710808311215,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.10202178220032,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "606623cf309829c195620d6878566108d881cff7",
          "message": "ICU-22679 Refactor ChineseCalendar\n\nSee #2898",
          "timestamp": "2024-03-18T18:51:27-07:00",
          "tree_id": "4803b37c4a13442afb463f6bbff3480e5b555f54",
          "url": "https://github.com/unicode-org/icu/commit/606623cf309829c195620d6878566108d881cff7"
        },
        "date": 1710813694940,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.22594409709535,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dragan@unicode.org",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "committer": {
            "email": "131725218+DraganBesevic@users.noreply.github.com",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "distinct": true,
          "id": "4f2cefb7ca6968308f70b83c82650475082e313c",
          "message": "ICU-22534 Integrate CLDR 45 release beta1",
          "timestamp": "2024-03-18T20:49:00-07:00",
          "tree_id": "1685743e143e178c92be4f5b843e72a2d5a73ff1",
          "url": "https://github.com/unicode-org/icu/commit/4f2cefb7ca6968308f70b83c82650475082e313c"
        },
        "date": 1710820761474,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.80284359707744,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "7c22ff7541ae523d378977d7a455010edfd46730",
          "message": "ICU-22621 Clang-Tidy: readability-string-compare\n\nhttps://releases.llvm.org/17.0.1/tools/clang/tools/extra/docs/clang-tidy/checks/readability/string-compare.html",
          "timestamp": "2024-03-19T15:55:56+01:00",
          "tree_id": "d090d62d21b71673a2b252c5bdaccfec6294bf97",
          "url": "https://github.com/unicode-org/icu/commit/7c22ff7541ae523d378977d7a455010edfd46730"
        },
        "date": 1710860684178,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.12981254681722,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "6b67715a94be204218338bc484e66ee2169ee95b",
          "message": "ICU-22633 Fix hang on large negative day in hebrew calendar\n\nCheck error status and return error early in the loop",
          "timestamp": "2024-03-19T16:32:06-07:00",
          "tree_id": "caec82bb8621a8138f7041e11dc2d0e54e730684",
          "url": "https://github.com/unicode-org/icu/commit/6b67715a94be204218338bc484e66ee2169ee95b"
        },
        "date": 1710891695788,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.3026481541626,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "7a3dfe877d9b39c025920fe457639496ac431ca1",
          "message": "ICU-22679 Remove getType and string comparsion\n\nChange the logic of handling year in era 0 counting backwards\nto depend on a boolean virtual function instead of adding\nstring comparsion code in the base class to have specific knowledge of\nbehavior of subclass.",
          "timestamp": "2024-03-19T17:44:37-07:00",
          "tree_id": "f7269d69efbbb943368c48388f8d2c436614bf9f",
          "url": "https://github.com/unicode-org/icu/commit/7a3dfe877d9b39c025920fe457639496ac431ca1"
        },
        "date": 1710896570164,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 194.99069000390065,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "1be861209e007174dd7fa64eb7d670dc1f44bc46",
          "message": "ICU-22689 Add PPUCD-based data driven test for binary props\n\nSee #2889",
          "timestamp": "2024-03-20T09:11:57-07:00",
          "tree_id": "34f9eeb9b71d523944b0822e27e00144e8d31fea",
          "url": "https://github.com/unicode-org/icu/commit/1be861209e007174dd7fa64eb7d670dc1f44bc46"
        },
        "date": 1710951681424,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 187.3407391259434,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "cce162bf4d0b0f09545b5d3b1141b31454a42ee8",
          "message": "ICU-11396 new properties Identifier_Status & Identifier_Type\n\nSee #2879",
          "timestamp": "2024-03-20T13:20:14-07:00",
          "tree_id": "98aafde7b3524431e46932f8f520ec18cdd7feb6",
          "url": "https://github.com/unicode-org/icu/commit/cce162bf4d0b0f09545b5d3b1141b31454a42ee8"
        },
        "date": 1710966525780,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.4537141279339,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "0b77215040a7381e2d2d0fa3045d2da279a59f73",
          "message": "ICU-22698 Clean up CalendarAstronomer",
          "timestamp": "2024-03-20T13:36:17-07:00",
          "tree_id": "168e577f06fe1ceb96805112cdeb89c094e7d74a",
          "url": "https://github.com/unicode-org/icu/commit/0b77215040a7381e2d2d0fa3045d2da279a59f73"
        },
        "date": 1710968253610,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.62522867381773,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "5e95ed829495f594ed53f8fdfa86a7aca8364478",
          "message": "ICU-22689 reminder for 16.0 update",
          "timestamp": "2024-03-20T15:49:44-07:00",
          "tree_id": "616e081f5edcef7a915e24bcf3bfca63d3fa9236",
          "url": "https://github.com/unicode-org/icu/commit/5e95ed829495f594ed53f8fdfa86a7aca8364478"
        },
        "date": 1710975465372,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.7504462432134,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pnacht@google.com",
            "name": "Pedro Kaj Kjellerup Nacht",
            "username": "pnacht"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "80a01a475b3704b57adbf117b746c2981ef5e7f5",
          "message": "ICU-22482 Hash-pin GHA, add dependabot to keep them updated\n\nSigned-off-by: Pedro Kaj Kjellerup Nacht <pnacht@google.com>\n\nUse latest version, uses a version >= 2.0.6 to overcome invalid key bug",
          "timestamp": "2024-03-20T22:14:52-07:00",
          "tree_id": "70c01e6dc4d821e5509464f232feca0ee80c8b8c",
          "url": "https://github.com/unicode-org/icu/commit/80a01a475b3704b57adbf117b746c2981ef5e7f5"
        },
        "date": 1710998612324,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.7953336181977,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "91721504efda86122222cd559baacee9cf6be6d5",
          "message": "ICU-22621 Clang-Tidy: modernize-return-braced-init-list\n\nhttps://releases.llvm.org/17.0.1/tools/clang/tools/extra/docs/clang-tidy/checks/modernize/return-braced-init-list.html",
          "timestamp": "2024-03-21T13:50:45+01:00",
          "tree_id": "6faf4b00dccbc3c1d78269aebea3221639b09e76",
          "url": "https://github.com/unicode-org/icu/commit/91721504efda86122222cd559baacee9cf6be6d5"
        },
        "date": 1711025992569,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.83541154047788,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "e246d3b712c317d6c5e2063cc11db89da7de5094",
          "message": "ICU-22643 Make UCHAR_TYPE work as intended with C++11, C11 & macOS.\n\nSince ICU4C requires C++11 and C11 the char16_t data type can be used\neverywhere, as long as the uchar.h header file gets included when\nbuilding as C (where it isn't a keyword as in C++), but this doesn't\nwork on macOS which for unknown reasons lacks the uchar.h header file\nand therefore needs a workaround to define char16_t.",
          "timestamp": "2024-03-21T15:44:20+01:00",
          "tree_id": "cbfe75eb470e11719072475b9983ed62c7db7d02",
          "url": "https://github.com/unicode-org/icu/commit/e246d3b712c317d6c5e2063cc11db89da7de5094"
        },
        "date": 1711032787852,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.3025728685761,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "028fa70c2950a8014f424c89c37a8137c10ee77b",
          "message": "ICU-22701 Bugfix: Make test independent of the default locale.\n\nThe original intention behind this test case was to use the root locale,\nbut ures_getFunctionalEquivalent() is implemented by calling ures_open()\nwhich sets URES_OPEN_LOCALE_DEFAULT_ROOT which will cause the default\nlocale to be loaded before the root locale.\n\nTo avoid that, pick a locale other than the root locale for the test.",
          "timestamp": "2024-03-21T09:16:05-07:00",
          "tree_id": "2c4b8fea7ac2a6c44545d691950b1673abb42cfa",
          "url": "https://github.com/unicode-org/icu/commit/028fa70c2950a8014f424c89c37a8137c10ee77b"
        },
        "date": 1711039394558,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.67600598409248,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "49699333+dependabot[bot]@users.noreply.github.com",
            "name": "dependabot[bot]",
            "username": "dependabot[bot]"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "94305fc59bbd56ff46b54290a052767ffa964ac2",
          "message": "ICU-22532 Bump the github-actions group with 9 updates\n\nSee #2916",
          "timestamp": "2024-03-21T09:45:50-07:00",
          "tree_id": "e55c87e9eefe67c071b22dab0a2e154a6467ee78",
          "url": "https://github.com/unicode-org/icu/commit/94305fc59bbd56ff46b54290a052767ffa964ac2"
        },
        "date": 1711041371845,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.21548377742178,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "fbc1f33e7e8afeb92ed7498fca1a1686d8b6b9c4",
          "message": "ICU-22679 Clean up Calendar code.\n\n1. Remove redudant implementation of default system ceuntry by using\n   macro\n2. Fold long if / else block if one block return.",
          "timestamp": "2024-03-21T09:50:10-07:00",
          "tree_id": "ea6f9b0200fda44e1c66b628dcef83c19d5d7adf",
          "url": "https://github.com/unicode-org/icu/commit/fbc1f33e7e8afeb92ed7498fca1a1686d8b6b9c4"
        },
        "date": 1711043545952,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.86082775144558,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "d259da81183bd2439e19dcb17cccf57cc31cf46c",
          "message": "ICU-22700 Fix large POSIX charset name cause hang\n\nFix fuzzer found issue of hang that caused by long POSIX charset name.\nLimit the POSIX charset name to at most 64 chars.",
          "timestamp": "2024-03-21T11:33:52-07:00",
          "tree_id": "19f1ada54943481c18d9ee9bdfd25ad35b76da62",
          "url": "https://github.com/unicode-org/icu/commit/d259da81183bd2439e19dcb17cccf57cc31cf46c"
        },
        "date": 1711046572814,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.18113820942594,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "37bdffb24041e78c23f36d315bf3f527fe7a6e4e",
          "message": "ICU-22679 Fix broken header test\n\nSee #2918",
          "timestamp": "2024-03-21T15:12:10-07:00",
          "tree_id": "c9be8a174ddb754bce6cd8d117edeeca9bf7146f",
          "url": "https://github.com/unicode-org/icu/commit/37bdffb24041e78c23f36d315bf3f527fe7a6e4e"
        },
        "date": 1711060053923,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.79077110565603,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "b3032aba0c6cdb7e36043125e6b7f73f7648bf21",
          "message": "ICU-22643 Verify that UCHAR_TYPE works for all public header files.",
          "timestamp": "2024-03-21T23:18:53+01:00",
          "tree_id": "343b9aa4526772977e25a47b9b68e46df269d768",
          "url": "https://github.com/unicode-org/icu/commit/b3032aba0c6cdb7e36043125e6b7f73f7648bf21"
        },
        "date": 1711062019690,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 186.18509872975153,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "142850ae2cc0900a04316b0ac51df31f4904a66d",
          "message": "ICU-22691 Make sample code possible to compile with current ICU4C.",
          "timestamp": "2024-03-21T23:19:54+01:00",
          "tree_id": "a548aa932c6b235bc19c4be1b4d13e3c9faf7f6a",
          "url": "https://github.com/unicode-org/icu/commit/142850ae2cc0900a04316b0ac51df31f4904a66d"
        },
        "date": 1711063241574,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.26895723295712,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "6eb43b164806efdbe0d55fc0db9c60a8cbcb971f",
          "message": "ICU-22533 Update badges for CI pipelines, user guide docs about CI",
          "timestamp": "2024-03-22T12:18:47+05:30",
          "tree_id": "fa604507d8924d6ee687f519894c7c720a8065e0",
          "url": "https://github.com/unicode-org/icu/commit/6eb43b164806efdbe0d55fc0db9c60a8cbcb971f"
        },
        "date": 1711090387769,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 183.4534044407209,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "13bf3c8313ff3bee2bfb72185b8615c636782aac",
          "message": "ICU-22611 Fixed the RBNF MultiplierSubstitution to only perform floor() on the value being formatted when the\nsubstitution is using a DecimalFormat and its owning rule also has a modulus substitution.  Took out a redundant\ncall to floor().  Added a hack to allow the caller to change the rounding behavior with setRoundingMode().\nAdded appropriate unit tests. Added additional documentation of the behavior to the API docs.",
          "timestamp": "2024-03-22T11:18:27-07:00",
          "tree_id": "e099e514320954cf5ab0e1e49fa6af9f9126ef97",
          "url": "https://github.com/unicode-org/icu/commit/13bf3c8313ff3bee2bfb72185b8615c636782aac"
        },
        "date": 1711132079075,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.95214034185855,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "141e820f7134b67818a76a8050671e8eea42eeb2",
          "message": "ICU-22690 Update ICU4J MessageFormatter to the latest spec, LDML 45",
          "timestamp": "2024-03-22T14:39:02-07:00",
          "tree_id": "1c50c25a8556c312e5c8b863bce4237444a709e3",
          "url": "https://github.com/unicode-org/icu/commit/141e820f7134b67818a76a8050671e8eea42eeb2"
        },
        "date": 1711143695572,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 187.7460020963326,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "rp9.next@gmail.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "8ba19195f9483df3b1b2330e52d0dd7dd4f2c067",
          "message": "ICU-22534 Update version number to 75.1",
          "timestamp": "2024-03-26T07:02:45+05:30",
          "tree_id": "654aef78bf4afa25f20b26538e17836bae3668ec",
          "url": "https://github.com/unicode-org/icu/commit/8ba19195f9483df3b1b2330e52d0dd7dd4f2c067"
        },
        "date": 1711417408957,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.2147651874962,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "4f75c627675b426938f569003ee9dc0ea43490bb",
          "message": "ICU-22534 BRS75 clean up import statements",
          "timestamp": "2024-03-26T08:50:56-04:00",
          "tree_id": "72cc74be103239be5cdf4d4bb6e4353475cfed7e",
          "url": "https://github.com/unicode-org/icu/commit/4f75c627675b426938f569003ee9dc0ea43490bb"
        },
        "date": 1711457773896,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.27380911412132,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "6ad2ffb9dbef10ca1044632dc478dfd10877d085",
          "message": "ICU-22534 BRS75 J API Signature file and API change report",
          "timestamp": "2024-03-26T08:51:16-04:00",
          "tree_id": "b2f3e6d5a0e09bce14b3b9d328baac1674dba644",
          "url": "https://github.com/unicode-org/icu/commit/6ad2ffb9dbef10ca1044632dc478dfd10877d085"
        },
        "date": 1711458249400,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.0754226605533,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "57fc3094f9658a283ed0b6d2648c0bb8a75f3f91",
          "message": "ICU-22534 BRS75 J Serialization test data",
          "timestamp": "2024-03-26T08:51:38-04:00",
          "tree_id": "3e51a606ab7c2f5c4b95e4b7e199b3ae9c2e8679",
          "url": "https://github.com/unicode-org/icu/commit/57fc3094f9658a283ed0b6d2648c0bb8a75f3f91"
        },
        "date": 1711458640914,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 184.1191268922705,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "8a1df5a7f4ce462e4f9558258b85377c8044c652",
          "message": "ICU-22692 Change SimpleNumber truncateAt to setMaximumIntegerDigits\n\nAlso promotes the remaining draft SimpleNumber functions to stable.\r\n\r\nSee #2892",
          "timestamp": "2024-03-26T16:13:46-07:00",
          "tree_id": "ecaac026d5c7da78a55d736dedd6a332dc0bc534",
          "url": "https://github.com/unicode-org/icu/commit/8a1df5a7f4ce462e4f9558258b85377c8044c652"
        },
        "date": 1711495264017,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.34232213379792,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "869cc60f3cc59c5d480dceebff0a9623b8b9ed96",
          "message": "ICU-22711 Set EnvTest frequency to nightly in March/April/Sept/Oct",
          "timestamp": "2024-03-26T16:20:08-07:00",
          "tree_id": "a65a9d503fcbf399c3dc4891bb2e5076af8a7b55",
          "url": "https://github.com/unicode-org/icu/commit/869cc60f3cc59c5d480dceebff0a9623b8b9ed96"
        },
        "date": 1711497080907,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.82504090687635,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "d4bc9bef26e57da2a609ab8c1ed19b6e457cc80a",
          "message": "ICU-22611 Fixed failures in EnvTest.",
          "timestamp": "2024-03-26T16:59:48-07:00",
          "tree_id": "e67644c5c11a22f64b73a3bea830b054eba7533c",
          "url": "https://github.com/unicode-org/icu/commit/d4bc9bef26e57da2a609ab8c1ed19b6e457cc80a"
        },
        "date": 1711498257965,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.28596205446567,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "rp9-next",
            "username": "rp9-next"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "4b6c85737a722fbc4dbb9e3ea8236e793e10d67b",
          "message": "ICU-22693 Remove workaround in CI for ICU4C due to failures running ./configure",
          "timestamp": "2024-03-27T12:43:27+05:30",
          "tree_id": "7c6e39dd6f956a4d29ddd579fe83bceb79c07c58",
          "url": "https://github.com/unicode-org/icu/commit/4b6c85737a722fbc4dbb9e3ea8236e793e10d67b"
        },
        "date": 1711524148002,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.50590898065303,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "d4bc9bef26e57da2a609ab8c1ed19b6e457cc80a",
          "message": "ICU-22611 Fixed failures in EnvTest.",
          "timestamp": "2024-03-26T16:59:48-07:00",
          "tree_id": "e67644c5c11a22f64b73a3bea830b054eba7533c",
          "url": "https://github.com/unicode-org/icu/commit/d4bc9bef26e57da2a609ab8c1ed19b6e457cc80a"
        },
        "date": 1711549066311,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 184.07722956981115,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "86b07da451786aaa831230e60ae0131c2ad4be6d",
          "message": "ICU-22618 Make unisetperf/draft possible to compile.",
          "timestamp": "2024-03-27T15:54:42+01:00",
          "tree_id": "593befa6f98c477b64e5f17c76c607f3eb1ca638",
          "url": "https://github.com/unicode-org/icu/commit/86b07da451786aaa831230e60ae0131c2ad4be6d"
        },
        "date": 1711551844036,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.38170721399655,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "4b8f8f3c48f6e04621dcdbae55b17940fcbd7d1c",
          "message": "ICU-22628 Replace log_err() with log_knownIssue() for known failure.",
          "timestamp": "2024-03-27T15:58:19+01:00",
          "tree_id": "e5899efbdabd1c07b5dcd3b9427bc5909ecc6a3e",
          "url": "https://github.com/unicode-org/icu/commit/4b8f8f3c48f6e04621dcdbae55b17940fcbd7d1c"
        },
        "date": 1711553334015,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.80873722764028,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tjc@igalia.com",
            "name": "Tim Chevalier",
            "username": "catamorphism"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "f7d641d5adb0460d1f58bad5947a29725870cc83",
          "message": "ICU-22261 Add tech preview implementation for MessageFormat 2.0 to icu4c",
          "timestamp": "2024-03-27T17:04:07-04:00",
          "tree_id": "fb3f9ad62e2200b6bd4c2a2632b5119736bcfced",
          "url": "https://github.com/unicode-org/icu/commit/f7d641d5adb0460d1f58bad5947a29725870cc83"
        },
        "date": 1711574799872,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.50548827935415,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "cce642a84fd8b812cec97626c4d8675f2c3b268b",
          "message": "ICU-22534 Regenerate urename.h for the ICU 75 release candidate.",
          "timestamp": "2024-03-28T08:39:08-07:00",
          "tree_id": "e8509887253ae023904b4ef48fca62152a281ab2",
          "url": "https://github.com/unicode-org/icu/commit/cce642a84fd8b812cec97626c4d8675f2c3b268b"
        },
        "date": 1711641001078,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.02229098758198,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tjc@igalia.com",
            "name": "Tim Chevalier",
            "username": "catamorphism"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "aff1bbaa14c21fabe0182ac2e7cad6ffceb2e59d",
          "message": "ICU-22261 Add UCONFIG_NO_MF2 flag that can be used to disable MessageFormat 2 functionality",
          "timestamp": "2024-03-28T08:48:35-07:00",
          "tree_id": "ef844ca49e4caf7fdabe49f04da6b6c6a1742664",
          "url": "https://github.com/unicode-org/icu/commit/aff1bbaa14c21fabe0182ac2e7cad6ffceb2e59d"
        },
        "date": 1711642557331,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.79513131415928,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "vichang@google.com",
            "name": "Victor Chang",
            "username": "gvictor"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "9e6173fcef468e878e43d07b928d22e782c4c8eb",
          "message": "ICU-22536 Fix ICUServiceThreadTest flakiness\n\nSometimes getVisibleIDs() method returns a null reference which might happend\nbecause of inaccurate concurrent access. This change attempts to fix this\nICUServiceThreadTest flakiness.",
          "timestamp": "2024-03-28T10:55:49-07:00",
          "tree_id": "c587c10deab80b1ee85158fcc8dacdb1cbe2e0e5",
          "url": "https://github.com/unicode-org/icu/commit/9e6173fcef468e878e43d07b928d22e782c4c8eb"
        },
        "date": 1711648767603,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.69871571634678,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "d83d26cc5db4ae190c3d015b83b8f377cbb0de9c",
          "message": "ICU-9972 Fix Chinese/Dangi Calendar getActualMaximum(UCAL_DAY_OF_YEAR)\n\nAlso fix ICU-12620 which is mark duplicate of ICU-9972 just now.\nand fix ICU-22258.\n\nSeparate the new year and winter solstice cache since the calculated\nvalue for these two calendar are mostly but not always the same due\nto slightly different observation timeZone.\n\nRemove the epochYear and zoneAstroCalc from the member data\nand instead return them from a getStting() method with the two caches\nsince all four of them are constant per subclass of ChineseCalendar\nand do not need to be different per object.\n\nThe known issues in the TestLimit is caused by both Calendar get/put the\nvalue from the same cache while the calculated result depends on the\ntimeZone zoneAstroCalc.",
          "timestamp": "2024-03-28T11:02:21-07:00",
          "tree_id": "c838cfdfac714928118b8f24bd4dbfc7b8f0fc0f",
          "url": "https://github.com/unicode-org/icu/commit/d83d26cc5db4ae190c3d015b83b8f377cbb0de9c"
        },
        "date": 1711650050437,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 188.81885008569526,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tjc@igalia.com",
            "name": "Tim Chevalier",
            "username": "catamorphism"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "070a1f420bc68042ead85ab07a73212382f0fa05",
          "message": "ICU-22261 Add missing API tags for MessageFormat 2 methods/constants",
          "timestamp": "2024-03-28T15:46:32-07:00",
          "tree_id": "671aaafa383bef5c5e4e758ff62066aea7e9478c",
          "url": "https://github.com/unicode-org/icu/commit/070a1f420bc68042ead85ab07a73212382f0fa05"
        },
        "date": 1711666510469,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.72029163643725,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "0127e4f7607873c1f58533c56907c905e4e34ec7",
          "message": "ICU-22534 BRS75 Remove fixed logKnownIssue for CLDR-17024\n\nCLDR-17024 is fixed in CLDR45 and the test now passed.\nRemove the logKnownIssue so it will test and report future breakage.",
          "timestamp": "2024-03-28T16:11:27-07:00",
          "tree_id": "cab5f8b8a6430ab5be34cbd5f4f819240b7db6de",
          "url": "https://github.com/unicode-org/icu/commit/0127e4f7607873c1f58533c56907c905e4e34ec7"
        },
        "date": 1711668547645,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.4738028171375,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "b2539f44df8f89f51401b903168a48a1aa9d972d",
          "message": "ICU-22534 BRS75 Remove fixed logKnownIssue\n\nCLDR17099 is fixed in CLDR44 and the test now passed. Remove\nthe logKnownIssue so it will test and report future breakage.",
          "timestamp": "2024-03-28T16:11:34-07:00",
          "tree_id": "2dbaf800762a669cc08aa70c695539952b1396cd",
          "url": "https://github.com/unicode-org/icu/commit/b2539f44df8f89f51401b903168a48a1aa9d972d"
        },
        "date": 1711669445015,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.5185662710693,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "1ac14c4ea71ad462988acc530adfb38ac1f9a190",
          "message": "ICU-22534 ICU4C API change report update",
          "timestamp": "2024-03-28T16:27:46-07:00",
          "tree_id": "601720f89128b64848c12f4c2a9ac31cb297ef13",
          "url": "https://github.com/unicode-org/icu/commit/1ac14c4ea71ad462988acc530adfb38ac1f9a190"
        },
        "date": 1711671142597,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.562863001855,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "97335dfa7e323f41b46918bbf5cad95c5e521268",
          "message": "ICU-22534 add mf2 to docmain.h",
          "timestamp": "2024-03-28T17:18:48-07:00",
          "tree_id": "1147424df9dad523432fe7c0792999be319fc9ce",
          "url": "https://github.com/unicode-org/icu/commit/97335dfa7e323f41b46918bbf5cad95c5e521268"
        },
        "date": 1711672371197,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.0552501563244,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "richard_gillam@apple.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "cce642a84fd8b812cec97626c4d8675f2c3b268b",
          "message": "ICU-22534 Regenerate urename.h for the ICU 75 release candidate.",
          "timestamp": "2024-03-28T08:39:08-07:00",
          "tree_id": "e8509887253ae023904b4ef48fca62152a281ab2",
          "url": "https://github.com/unicode-org/icu/commit/cce642a84fd8b812cec97626c4d8675f2c3b268b"
        },
        "date": 1712070834905,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.92355821254847,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "49699333+dependabot[bot]@users.noreply.github.com",
            "name": "dependabot[bot]",
            "username": "dependabot[bot]"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "a86455825ab231bdad57acaf5ae633915ccc7527",
          "message": "ICU-22721 Bump the github-actions group with 2 updates\n\nSee #2942",
          "timestamp": "2024-04-04T21:26:56-04:00",
          "tree_id": "8d0f92ba734fd2be2ec407ffca174fdc90981747",
          "url": "https://github.com/unicode-org/icu/commit/a86455825ab231bdad57acaf5ae633915ccc7527"
        },
        "date": 1712281144897,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.97623232105565,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "943b0ca31b38f5c7ba8d58c5f3d88d34c4ebff8d",
          "message": "ICU-22722 Fix Readme CI badge link",
          "timestamp": "2024-04-05T08:31:47-07:00",
          "tree_id": "e96105ee5908f4e8984f7b83352335e647ff848b",
          "url": "https://github.com/unicode-org/icu/commit/943b0ca31b38f5c7ba8d58c5f3d88d34c4ebff8d"
        },
        "date": 1712331380561,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.65233889286003,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "49699333+dependabot[bot]@users.noreply.github.com",
            "name": "dependabot[bot]",
            "username": "dependabot[bot]"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "a86455825ab231bdad57acaf5ae633915ccc7527",
          "message": "ICU-22721 Bump the github-actions group with 2 updates\n\nSee #2942",
          "timestamp": "2024-04-04T21:26:56-04:00",
          "tree_id": "8d0f92ba734fd2be2ec407ffca174fdc90981747",
          "url": "https://github.com/unicode-org/icu/commit/a86455825ab231bdad57acaf5ae633915ccc7527"
        },
        "date": 1712332401326,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.78784247622144,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "29b1141f79eac6f5178f75f1d6d62c8e7a5cede4",
          "message": "ICU-22730 Fix Japanese extended year int32 overflow",
          "timestamp": "2024-04-10T22:17:01-07:00",
          "tree_id": "223755abc0452969bc8ac4e7dcac4903526b0f68",
          "url": "https://github.com/unicode-org/icu/commit/29b1141f79eac6f5178f75f1d6d62c8e7a5cede4"
        },
        "date": 1712813179270,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.45627893929785,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "dbb3fe957a52921aabf433d77ae87c33de328801",
          "message": "ICU-22722 Fix missing starter character for MF2 keyword in User Guide",
          "timestamp": "2024-04-11T12:49:15-07:00",
          "tree_id": "0ce2cae677f2dc9c88be254782d96b155b9b74ae",
          "url": "https://github.com/unicode-org/icu/commit/dbb3fe957a52921aabf433d77ae87c33de328801"
        },
        "date": 1712865142911,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 206.22701924509488,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "617b094df3eb853a35f1227472178836ce625cff",
          "message": "ICU-22723 Update PR template help text for next version's ticket numbers\n\nSee #2962",
          "timestamp": "2024-04-15T22:22:46-04:00",
          "tree_id": "c26fac35b7f67c3332f2b95d6860c883a1e374c3",
          "url": "https://github.com/unicode-org/icu/commit/617b094df3eb853a35f1227472178836ce625cff"
        },
        "date": 1713234395111,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.31370901858557,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ccornelius@google.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "3f960044b87d0e343386c3311cfe025bd163483e",
          "message": "ICU-22722 Revised ICU4J API doc process to modern tools\n\nSee #2969",
          "timestamp": "2024-04-17T13:20:26-07:00",
          "tree_id": "4b529ea15644141fbce821a9e57e7075aff7d43c",
          "url": "https://github.com/unicode-org/icu/commit/3f960044b87d0e343386c3311cfe025bd163483e"
        },
        "date": 1713385443015,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.43079845675751,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "5e35ffc87edba2c90e75bec0093ba218824d60c2",
          "message": "ICU-22745 Merge ICU 75 maintenance branch to main (#2972)",
          "timestamp": "2024-04-18T15:04:33-07:00",
          "tree_id": "d04aef54824e13c302519413e20eef52a9aa124c",
          "url": "https://github.com/unicode-org/icu/commit/5e35ffc87edba2c90e75bec0093ba218824d60c2"
        },
        "date": 1713478500351,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.42487867548863,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "0c02f8c0075b07424e75311e776eef662bec9413",
          "message": "ICU-22743 Change internal functions to propagate errors up.",
          "timestamp": "2024-04-18T15:32:59-07:00",
          "tree_id": "93bb1aac0df9258480a5eb68fc0cbb3558fae924",
          "url": "https://github.com/unicode-org/icu/commit/0c02f8c0075b07424e75311e776eef662bec9413"
        },
        "date": 1713480552053,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.69590019932787,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "0e4c2d8bc68bbd46f2b74c0404e0cc26a98251f7",
          "message": "ICU-22724 ICU BRS 76: front-load update version to 76.0.1",
          "timestamp": "2024-04-18T16:57:47-07:00",
          "tree_id": "f47281914dafe91618107b8894a161510d8225e4",
          "url": "https://github.com/unicode-org/icu/commit/0e4c2d8bc68bbd46f2b74c0404e0cc26a98251f7"
        },
        "date": 1713485282250,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.72930665886014,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "4c9770f73d8bafee7fe63dc6f6fbf2947ba78839",
          "message": "ICU-22721 Remove now obsolete disabled warnings for LocalPointerBase.\n\nThe missing operators that used to be warned about were all deleted by\ncommit 75eab42060d6a73b62db2bc639216eae1f8ec47e.",
          "timestamp": "2024-04-22T21:58:17+02:00",
          "tree_id": "c9fb1e12be0cf7706cf075eb2d5d1f1a5b733d74",
          "url": "https://github.com/unicode-org/icu/commit/4c9770f73d8bafee7fe63dc6f6fbf2947ba78839"
        },
        "date": 1713817046697,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.9882364871717,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "0312308566b5a232102d1bce41070b0498cd8579",
          "message": "ICU-22721 Prevent inconsistent ICU4J Maven deploys via CI",
          "timestamp": "2024-04-22T15:28:56-07:00",
          "tree_id": "05e4284185557af4393ea84306911ca7e8365b49",
          "url": "https://github.com/unicode-org/icu/commit/0312308566b5a232102d1bce41070b0498cd8579"
        },
        "date": 1713825818714,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 212.48967909671225,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "a91cbd65789d69b06904d68b9dd680a80cc6d35c",
          "message": "ICU-22750 Fix Floating-point-exception in icu::Calendar::roll\n\nSee #2979",
          "timestamp": "2024-04-23T09:46:29-07:00",
          "tree_id": "84d8fb8d2bd35874ce2736c261d828e8dec61b02",
          "url": "https://github.com/unicode-org/icu/commit/a91cbd65789d69b06904d68b9dd680a80cc6d35c"
        },
        "date": 1713891753461,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 194.62396298545,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "3aa8b8c5eefa10d4916428955fb39dacc39f06c1",
          "message": "ICU-22742 Fix handling of XA,XB,XC for addLikelySubtags\n\nAdd more tests.\n\nICU-22742 Add PS... variants\n\nICU-22742 Add java tests\n\nICU-22742 extend tests\n\nICU-22742 wrap java",
          "timestamp": "2024-04-24T15:24:35-07:00",
          "tree_id": "054171e40051372b5bdf0fe29e8207ba2e9371dc",
          "url": "https://github.com/unicode-org/icu/commit/3aa8b8c5eefa10d4916428955fb39dacc39f06c1"
        },
        "date": 1713998022655,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.37948305010426,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "a7e23a531c9f35ad804e3ae161ac0179745a6888",
          "message": "ICU-22721 Clang-Tidy: modernize-use-override\n\nhttps://releases.llvm.org/17.0.1/tools/clang/tools/extra/docs/clang-tidy/checks/modernize/use-override.html",
          "timestamp": "2024-04-29T19:28:40+02:00",
          "tree_id": "a95359f5b53a2d900256780cc28c11f62ee935bb",
          "url": "https://github.com/unicode-org/icu/commit/a7e23a531c9f35ad804e3ae161ac0179745a6888"
        },
        "date": 1714412202812,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.3066085289331,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "8b84ae1ddae19412d5fecc88661593136a6f6144",
          "message": "ICU-22721 Comment out variables only used in commented out code.",
          "timestamp": "2024-04-29T11:43:21-07:00",
          "tree_id": "bce9c9c0982d94ccb68818a95c029fe0b8be8604",
          "url": "https://github.com/unicode-org/icu/commit/8b84ae1ddae19412d5fecc88661593136a6f6144"
        },
        "date": 1714417024084,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.8045467822337,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "755b0981ec0af447c51fa362cbf9d143fa56092c",
          "message": "ICU-22721 Reorder initializer to match declaration (-Wreorder-ctor).",
          "timestamp": "2024-04-29T11:44:35-07:00",
          "tree_id": "4a741e418af2eac3d234f6fd0d53acadec4dec35",
          "url": "https://github.com/unicode-org/icu/commit/755b0981ec0af447c51fa362cbf9d143fa56092c"
        },
        "date": 1714417984583,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.65521363475838,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "a57c68364eed30d6c4565ecd5c43e38929ed42ca",
          "message": "ICU-22079 Rename",
          "timestamp": "2024-04-29T14:51:13-07:00",
          "tree_id": "bcb4d406f0b95ce240a84c105cb1aff949214386",
          "url": "https://github.com/unicode-org/icu/commit/a57c68364eed30d6c4565ecd5c43e38929ed42ca"
        },
        "date": 1714427913709,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.30388964114465,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "6e6b57672958de52cb71dbd17d9caab0e19f5574",
          "message": "ICU-22707 Azure ICU4J timeout 20->25min",
          "timestamp": "2024-04-29T17:00:55-07:00",
          "tree_id": "24e9c858e42ce146cf87a15ed660a421641c5e4f",
          "url": "https://github.com/unicode-org/icu/commit/6e6b57672958de52cb71dbd17d9caab0e19f5574"
        },
        "date": 1714435860481,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.1954800724365,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "6041c249f4612ee556d56ae4cd056450258e460e",
          "message": "ICU-21757 Remove ICU dependencies on CollectionUtilities",
          "timestamp": "2024-04-30T19:10:06-07:00",
          "tree_id": "6c86af21b977184d7338072417d88f025f19c3ea",
          "url": "https://github.com/unicode-org/icu/commit/6041c249f4612ee556d56ae4cd056450258e460e"
        },
        "date": 1714529699109,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.22568367701982,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "6041c249f4612ee556d56ae4cd056450258e460e",
          "message": "ICU-21757 Remove ICU dependencies on CollectionUtilities",
          "timestamp": "2024-04-30T19:10:06-07:00",
          "tree_id": "6c86af21b977184d7338072417d88f025f19c3ea",
          "url": "https://github.com/unicode-org/icu/commit/6041c249f4612ee556d56ae4cd056450258e460e"
        },
        "date": 1714662973929,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 194.79401362657387,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "1ff249b73f8a8c3c5c355c976622fc8e39a863cb",
          "message": "ICU-21589 Remove format-overflow warning\n\n1. double size one stack buffer\n2. add pragma to ignore some theorical case because the array are in\n   same length but some fields are shorter than the other.\n\nICU-21589 wrap with defined(__GNUC__)\n\nICU-21589 avoid clang",
          "timestamp": "2024-05-02T18:23:08+02:00",
          "tree_id": "ca2c175a1238111eab7b38d68ba6be5196a2a13c",
          "url": "https://github.com/unicode-org/icu/commit/1ff249b73f8a8c3c5c355c976622fc8e39a863cb"
        },
        "date": 1714667560856,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.84892642570892,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "464531eb8e08abe0bd183401525a05e32423cb30",
          "message": "ICU-22079 Fix bidi fuzzer\n\nCheck data length to avoid memcpy out of bound.",
          "timestamp": "2024-05-02T11:59:22-07:00",
          "tree_id": "9da25f1634a7e5a2ba7a58a36e47e07de7e0de8a",
          "url": "https://github.com/unicode-org/icu/commit/464531eb8e08abe0bd183401525a05e32423cb30"
        },
        "date": 1714677082532,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.94531683731233,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "23bf38f10f146d5b637922321119926b5e60bf03",
          "message": "ICU-22764 Fix gendict memory safety in toml uchars mode",
          "timestamp": "2024-05-03T11:02:29-07:00",
          "tree_id": "5241a78e2af87e50e31760e0eab9caf07385b4d7",
          "url": "https://github.com/unicode-org/icu/commit/23bf38f10f146d5b637922321119926b5e60bf03"
        },
        "date": 1714760218218,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 189.79771388757564,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "49699333+dependabot[bot]@users.noreply.github.com",
            "name": "dependabot[bot]",
            "username": "dependabot[bot]"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "1c7d7f9a9598930c76980171d6d2e902fc89823a",
          "message": "ICU-22721 Bump the github-actions group with 4 updates\n\nSee #2992",
          "timestamp": "2024-05-03T15:36:34-07:00",
          "tree_id": "f3749028da8bd2b2207929ab16bd4c1c0b55f960",
          "url": "https://github.com/unicode-org/icu/commit/1c7d7f9a9598930c76980171d6d2e902fc89823a"
        },
        "date": 1714776272952,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.2224845782304,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "69cb085e12764b84269c31be5a054e7c510be443",
          "message": "ICU-21757 Replace UOption with commons-cli in perf-tests",
          "timestamp": "2024-05-03T16:28:26-07:00",
          "tree_id": "06d97b32718b202704c03dbaa7d1d79ba7b636ce",
          "url": "https://github.com/unicode-org/icu/commit/69cb085e12764b84269c31be5a054e7c510be443"
        },
        "date": 1714779341709,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 194.41917182749526,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "hsivonen@hsivonen.fi",
            "name": "Henri Sivonen",
            "username": "hsivonen"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "7eaefe076e719d58d06fc42a723e84a5b6e158a3",
          "message": "ICU-22758 Make icuexportdata compatible with Unicode 16 normalization",
          "timestamp": "2024-05-06T08:27:06-07:00",
          "tree_id": "84ea3bc068509d161e47dcab414d9d04ec2e121c",
          "url": "https://github.com/unicode-org/icu/commit/7eaefe076e719d58d06fc42a723e84a5b6e158a3"
        },
        "date": 1715009808566,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.02652671134348,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "c92c188caccabe6a77a8bb949f46809f00d8aea2",
          "message": "ICU-21757 Replace UOption with commons-cli in XLIFF2ICUConverter",
          "timestamp": "2024-05-06T11:22:13-07:00",
          "tree_id": "b738b17bed644341286453ef4e6aa19a1be54dfc",
          "url": "https://github.com/unicode-org/icu/commit/c92c188caccabe6a77a8bb949f46809f00d8aea2"
        },
        "date": 1715019899445,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.80639429591395,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "9369b7a20914322ba9cc2eb115bf2ac7dcefefbe",
          "message": "ICU-21757 Stop sharing `utilities-for-cldr`\n\nMoves UnicodeMap and related classes to core.\nAlso removes `CollectionUtilities`, `UOption`, and `ElapsedTimer`.\nThey will end up in UnicodeTools, CLDR, and CLDR respectively.",
          "timestamp": "2024-05-06T15:49:03-07:00",
          "tree_id": "91582562ad92a37a5f9f17bdbfadda69d1d2af7c",
          "url": "https://github.com/unicode-org/icu/commit/9369b7a20914322ba9cc2eb115bf2ac7dcefefbe"
        },
        "date": 1715036314529,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.25058007093534,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tjc@igalia.com",
            "name": "Tim Chevalier",
            "username": "catamorphism"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "85a9b2ee8458ffbe9fe7ef7a48f4512db2c49e8d",
          "message": "ICU-22746 Refactor MF2 tests to be data-driven and add JSON lib\n\nThis change moves all test strings out of test/intltest/messageformat2test.cpp\nand into JSON files, which are parsed/run by code in\na new file, test/intltest/messageformat2test_read_json.cpp .\nIt also removes the file test/intltest/messageformat2test_fromjson.cpp ,\nwhich contained tests that are now stored in JSON files.\n\nTo enable this, a new vendored library is added:\nnlohmann/json .\nThis library is introduced as a dependency for the MF2 tests.\nThe required part of the library is a single header file,\nwhich is added under icu4c/source/tools/toolutil/.\nAlso adds a wrapper file for the vendored JSON header file\nthat defines macros that disable exceptions.\n\nCo-authored-by: Steven R. Loomis <srl295@gmail.com>",
          "timestamp": "2024-05-07T14:09:24-07:00",
          "tree_id": "756a8fad2832177a0445c0a0752b43f302164c0b",
          "url": "https://github.com/unicode-org/icu/commit/85a9b2ee8458ffbe9fe7ef7a48f4512db2c49e8d"
        },
        "date": 1715116800812,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.57298523790624,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "cb0d0d4e5649c89069d2939ddf175c11c62c57c6",
          "message": "ICU-21757 Small GitHub workflow cleanup",
          "timestamp": "2024-05-08T12:51:27-07:00",
          "tree_id": "0c0c3d24415b4a4ec47e16281ffbe0b0c2631d47",
          "url": "https://github.com/unicode-org/icu/commit/cb0d0d4e5649c89069d2939ddf175c11c62c57c6"
        },
        "date": 1715198385066,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 213.53582823727902,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "f133a0bd9bc6567a5ae943a5e4aaeda1cad04628",
          "message": "ICU-22723 Cleanup: remove the icu4j/maven-migration/ folder",
          "timestamp": "2024-05-08T14:17:44-07:00",
          "tree_id": "195fdecdc0b63354168cdffd1a13f7f91b32d9dd",
          "url": "https://github.com/unicode-org/icu/commit/f133a0bd9bc6567a5ae943a5e4aaeda1cad04628"
        },
        "date": 1715203219468,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 184.12694196154962,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "63afc7642582622f4c27925835e9e95828efea76",
          "message": "ICU-22769 Rename of the ICU4J data folder to not contain a version (script)",
          "timestamp": "2024-05-08T15:33:21-07:00",
          "tree_id": "cf1737606cf01c3fd54ef28de6dda4e3bc4c2011",
          "url": "https://github.com/unicode-org/icu/commit/63afc7642582622f4c27925835e9e95828efea76"
        },
        "date": 1715207836939,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.9961928698987,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "564c92d666354a8ed0f99cba0f239eba790bd148",
          "message": "ICU-22721 Delete obsolete __STRICT_ANSI__ workaround for MinGW.\n\nThis was originally added in the year 2004 for ICU-3854 but is no longer\nneeded with contemporary versions of MinGW where it instead as of GCC 14\ncauses this problem:\n\n__STRICT_ANSI__ seems to have been undefined; this is not supported.",
          "timestamp": "2024-05-10T16:11:21-07:00",
          "tree_id": "fa5d9f5763bd3693b31481b6ea279d34741c798d",
          "url": "https://github.com/unicode-org/icu/commit/564c92d666354a8ed0f99cba0f239eba790bd148"
        },
        "date": 1715383157173,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.3548805194842,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "hsivonen@hsivonen.fi",
            "name": "Henri Sivonen",
            "username": "hsivonen"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "f5056cb46aaf377f52e96cf02554f7c6e830955a",
          "message": "ICU-22757 Remove allow list of known contractions with precomposed form from ICU4X mode of genuca\n\nThis assumes that future cases will work OK, since the addition that was seen in Unicode 16 alpha\nwas OK.",
          "timestamp": "2024-05-13T11:25:01-07:00",
          "tree_id": "33242c83e28f940ad31c70ce52a4883715223d14",
          "url": "https://github.com/unicode-org/icu/commit/f5056cb46aaf377f52e96cf02554f7c6e830955a"
        },
        "date": 1715625252200,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.60105756679633,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "75ef0d97e158492731604d600fbeeff8a0158ddd",
          "message": "ICU-22723 Add line number info to icuexportdata handleError\n\nSee #3004",
          "timestamp": "2024-05-13T11:58:09-07:00",
          "tree_id": "742c6ebe1b1c789d3b386e8bc07fcdc03474add7",
          "url": "https://github.com/unicode-org/icu/commit/75ef0d97e158492731604d600fbeeff8a0158ddd"
        },
        "date": 1715627109508,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.23708214867233,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tjc@igalia.com",
            "name": "Tim Chevalier",
            "username": "catamorphism"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "6d5555a739179b5d177e73db7c111c5ef1cac22d",
          "message": "ICU-22746 Import ICU4J tests\n\nIncludes code fixes for `numberingSystem`, `percent`,\nand `precision` options in `:number`\n\nAlso includes a code fix for number selection:\n  Refactor code to conform more closely to the steps in the spec,\n  and call the number formatter before the selector so that a FormattedNumber\n  with the right options is selected on\n\nSome modifications were needed to add missing params\nand to mark some tests as ignored (see ICU-22754).\nAlso added decimal arguments in JSON test reader\n\nFinally, some redundant tests are removed:\nall tests in messageformat2test_features and\nmessageformat2test_icu, and\nmessageformat2test_builtin are now covered by JSON tests",
          "timestamp": "2024-05-13T14:51:14-07:00",
          "tree_id": "2ec4275f6f6122b813de2a87ea9cb416dce79385",
          "url": "https://github.com/unicode-org/icu/commit/6d5555a739179b5d177e73db7c111c5ef1cac22d"
        },
        "date": 1715637613349,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.36899550933433,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "hsivonen@hsivonen.fi",
            "name": "Henri Sivonen",
            "username": "hsivonen"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "f5056cb46aaf377f52e96cf02554f7c6e830955a",
          "message": "ICU-22757 Remove allow list of known contractions with precomposed form from ICU4X mode of genuca\n\nThis assumes that future cases will work OK, since the addition that was seen in Unicode 16 alpha\nwas OK.",
          "timestamp": "2024-05-13T11:25:01-07:00",
          "tree_id": "33242c83e28f940ad31c70ce52a4883715223d14",
          "url": "https://github.com/unicode-org/icu/commit/f5056cb46aaf377f52e96cf02554f7c6e830955a"
        },
        "date": 1715688002287,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.32290309316636,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "ed52e0a25b084b71bad0272b73b7f9634b615fb5",
          "message": "ICU-22721 Rename scoped variable to not shadow variable in outer scope.",
          "timestamp": "2024-05-14T19:03:14+02:00",
          "tree_id": "f2af96bc732f5bc48891788aa78dada8a326bdb8",
          "url": "https://github.com/unicode-org/icu/commit/ed52e0a25b084b71bad0272b73b7f9634b615fb5"
        },
        "date": 1715706817009,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.90003243934297,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tjc@igalia.com",
            "name": "Tim Chevalier",
            "username": "catamorphism"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "084af5c2b6588cef38a169d65c9bac7f6820fe5f",
          "message": "ICU-22740 Add -Wctad-maybe-unsupported to CI jobs that use -Wall -Wextra.\n\nThis modifies the following jobs:\n\nC: Linux Clang-16 WarningsAsErrors (Ubuntu 20.04)\nC: macOS(Latest) Clang WarningsAsErrors",
          "timestamp": "2024-05-14T12:39:44-07:00",
          "tree_id": "89ddcacd113a197614b24042eccf5312354fd8d8",
          "url": "https://github.com/unicode-org/icu/commit/084af5c2b6588cef38a169d65c9bac7f6820fe5f"
        },
        "date": 1715716609488,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.63447103871576,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tjc@igalia.com",
            "name": "Tim Chevalier",
            "username": "catamorphism"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "e6ac2a292fac59032eb3ba11847a43bd5837c30c",
          "message": "ICU-22753 Fix read-after-move in MF2\n\nIn StaticErrors::addError() and DynamicErrors::addError(),\ndon't read from `e` after moving out of it.",
          "timestamp": "2024-05-14T12:40:06-07:00",
          "tree_id": "dbccda0408e9cc3b85aeba6b58373101a1b0acc5",
          "url": "https://github.com/unicode-org/icu/commit/e6ac2a292fac59032eb3ba11847a43bd5837c30c"
        },
        "date": 1715717434445,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.2695850307541,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "hsivonen@hsivonen.fi",
            "name": "Henri Sivonen",
            "username": "hsivonen"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "398890853d4937e8ea886d44776edb2cc49f1081",
          "message": "ICU-22718 Export disallowed/ignored UTS 46 data for ICU4X",
          "timestamp": "2024-05-15T11:20:09-07:00",
          "tree_id": "6e6ed82255a9a0ec91985a25805cbe0be7bf7d8a",
          "url": "https://github.com/unicode-org/icu/commit/398890853d4937e8ea886d44776edb2cc49f1081"
        },
        "date": 1715797724791,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.4745037329758,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "f017c3722b4c04368e5a00c3850b3a81c8d5e957",
          "message": "ICU-22722 Better name for workflow",
          "timestamp": "2024-05-15T14:18:34-07:00",
          "tree_id": "5329316e5ac19804ba783ff1e1fb84ef8316619c",
          "url": "https://github.com/unicode-org/icu/commit/f017c3722b4c04368e5a00c3850b3a81c8d5e957"
        },
        "date": 1715808480127,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.29838750061558,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "6ac6fdda74948d0a2ddd51ee81a672baa5c2bf35",
          "message": "ICU-22730 propagate error avoid overflow",
          "timestamp": "2024-05-16T00:27:14-07:00",
          "tree_id": "5b28dbdab426800e6f3a12ebf22200698f9afe1b",
          "url": "https://github.com/unicode-org/icu/commit/6ac6fdda74948d0a2ddd51ee81a672baa5c2bf35"
        },
        "date": 1715844897825,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.29288567775325,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "6ac6fdda74948d0a2ddd51ee81a672baa5c2bf35",
          "message": "ICU-22730 propagate error avoid overflow",
          "timestamp": "2024-05-16T00:27:14-07:00",
          "tree_id": "5b28dbdab426800e6f3a12ebf22200698f9afe1b",
          "url": "https://github.com/unicode-org/icu/commit/6ac6fdda74948d0a2ddd51ee81a672baa5c2bf35"
        },
        "date": 1715864303409,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.84684060826106,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "6ac6fdda74948d0a2ddd51ee81a672baa5c2bf35",
          "message": "ICU-22730 propagate error avoid overflow",
          "timestamp": "2024-05-16T00:27:14-07:00",
          "tree_id": "5b28dbdab426800e6f3a12ebf22200698f9afe1b",
          "url": "https://github.com/unicode-org/icu/commit/6ac6fdda74948d0a2ddd51ee81a672baa5c2bf35"
        },
        "date": 1715866329797,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.4055630883703,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "aszasz@users.noreply.github.com",
            "name": "aszasz",
            "username": "aszasz"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "0ef4da943c1cfc694e84fcb85cee5c78bae89d71",
          "message": "ICU-22722 Fix broken link to faq in docs/userguide/icu/index.md\n\nSee #3006",
          "timestamp": "2024-05-17T14:43:28-07:00",
          "tree_id": "4f02fd9d47f9f78577b0db9e62a5522f7fc034cc",
          "url": "https://github.com/unicode-org/icu/commit/0ef4da943c1cfc694e84fcb85cee5c78bae89d71"
        },
        "date": 1715982533031,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.60016149836963,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "788b89321454f3eeb20782032c8444aabba42417",
          "message": "ICU-22769 Fix the jar generation to not use versioned folder",
          "timestamp": "2024-05-22T10:01:41-07:00",
          "tree_id": "f7a44e93df0dec5f9bcdc94ffdd6cdf7c6905b97",
          "url": "https://github.com/unicode-org/icu/commit/788b89321454f3eeb20782032c8444aabba42417"
        },
        "date": 1716397879806,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 209.39706445922386,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dragan@unicode.org",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "committer": {
            "email": "131725218+DraganBesevic@users.noreply.github.com",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "distinct": true,
          "id": "699fb1dbc4cfbae6f78ff0b28570f44a20a7b149",
          "message": "ICU-22723 Integrate CLDR 46 release m0, part 3, source files",
          "timestamp": "2024-05-24T11:32:57-07:00",
          "tree_id": "3f4559d70429d4d14080bbac3e7dd83f1579d06d",
          "url": "https://github.com/unicode-org/icu/commit/699fb1dbc4cfbae6f78ff0b28570f44a20a7b149"
        },
        "date": 1716576684298,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.20879281073871,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "faac745421f0ef85baf6001216b80aaaddb0e821",
          "message": "ICU-22768 Fix bidi buffer overflow\n\nSee #3016",
          "timestamp": "2024-05-29T15:16:32-07:00",
          "tree_id": "6d9b2c312a504a632100e794851998bc3c6142a8",
          "url": "https://github.com/unicode-org/icu/commit/faac745421f0ef85baf6001216b80aaaddb0e821"
        },
        "date": 1717021553490,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.15137209610936,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "faac745421f0ef85baf6001216b80aaaddb0e821",
          "message": "ICU-22768 Fix bidi buffer overflow\n\nSee #3016",
          "timestamp": "2024-05-29T15:16:32-07:00",
          "tree_id": "6d9b2c312a504a632100e794851998bc3c6142a8",
          "url": "https://github.com/unicode-org/icu/commit/faac745421f0ef85baf6001216b80aaaddb0e821"
        },
        "date": 1717059245259,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.97385913479965,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "faac745421f0ef85baf6001216b80aaaddb0e821",
          "message": "ICU-22768 Fix bidi buffer overflow\n\nSee #3016",
          "timestamp": "2024-05-29T15:16:32-07:00",
          "tree_id": "6d9b2c312a504a632100e794851998bc3c6142a8",
          "url": "https://github.com/unicode-org/icu/commit/faac745421f0ef85baf6001216b80aaaddb0e821"
        },
        "date": 1717063054796,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.2236190353243,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "faac745421f0ef85baf6001216b80aaaddb0e821",
          "message": "ICU-22768 Fix bidi buffer overflow\n\nSee #3016",
          "timestamp": "2024-05-29T15:16:32-07:00",
          "tree_id": "6d9b2c312a504a632100e794851998bc3c6142a8",
          "url": "https://github.com/unicode-org/icu/commit/faac745421f0ef85baf6001216b80aaaddb0e821"
        },
        "date": 1717067768317,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.57011420141316,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "edfe255de360eed46e653c7d35983a757568dec8",
          "message": "ICU-22001 Put translation unit local definitions in unnamed namespaces.\n\nAny symbol that isn't intended to have external linkage should either be\ndeclared static (legacy code) or put in an unnamed namespace.",
          "timestamp": "2024-05-30T14:06:12+02:00",
          "tree_id": "9dd54a00adab863b738e37e513567501489645a8",
          "url": "https://github.com/unicode-org/icu/commit/edfe255de360eed46e653c7d35983a757568dec8"
        },
        "date": 1717071277903,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.30510497602924,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "edfe255de360eed46e653c7d35983a757568dec8",
          "message": "ICU-22001 Put translation unit local definitions in unnamed namespaces.\n\nAny symbol that isn't intended to have external linkage should either be\ndeclared static (legacy code) or put in an unnamed namespace.",
          "timestamp": "2024-05-30T14:06:12+02:00",
          "tree_id": "9dd54a00adab863b738e37e513567501489645a8",
          "url": "https://github.com/unicode-org/icu/commit/edfe255de360eed46e653c7d35983a757568dec8"
        },
        "date": 1717077630583,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.1720769389167,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "3235c38f24711a86d7fc5adf5e536bf6a4f1d10a",
          "message": "ICU-22777 Fix incorrect pointer comparision",
          "timestamp": "2024-05-30T23:46:56-07:00",
          "tree_id": "d98a8c3629e843dd88cfe0580c3f3fe80da78fc0",
          "url": "https://github.com/unicode-org/icu/commit/3235c38f24711a86d7fc5adf5e536bf6a4f1d10a"
        },
        "date": 1717138552820,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.41834826199215,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "b9b324ccc5da8e16bf338d15d4acc6429aa8bad2",
          "message": "ICU-22722 Update docs for cldr-to-icu converter",
          "timestamp": "2024-05-31T14:21:24-07:00",
          "tree_id": "c185d5bf44258b0582c01f0e5b763554e542c619",
          "url": "https://github.com/unicode-org/icu/commit/b9b324ccc5da8e16bf338d15d4acc6429aa8bad2"
        },
        "date": 1717190741418,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.27778984109906,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "81492ae9a238884d47f55bf115e660d0afa4f0e0",
          "message": "ICU-22722 Make example config for cldr-to-icu work over all locales",
          "timestamp": "2024-06-02T21:41:48-07:00",
          "tree_id": "a1df215cf27196dbb77a747aeed22794bc4db92f",
          "url": "https://github.com/unicode-org/icu/commit/81492ae9a238884d47f55bf115e660d0afa4f0e0"
        },
        "date": 1717389872882,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.70301361045514,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "0d8a3ccd111b35a295a571db1f5c79cd20525b6d",
          "message": "ICU-22785 move Block bits from propsvec0 to new trie",
          "timestamp": "2024-06-04T10:49:53-07:00",
          "tree_id": "aeb420d5812d340fc08f8211d6d3a06b1504da12",
          "url": "https://github.com/unicode-org/icu/commit/0d8a3ccd111b35a295a571db1f5c79cd20525b6d"
        },
        "date": 1717523949579,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.6544528821918,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "47e9389b8e356aa3d7646ee523a73c36d3aa58ae",
          "message": "ICU-22785 move cptrie bit setter to toolutil; add getCPTrieSize()",
          "timestamp": "2024-06-04T18:51:53-07:00",
          "tree_id": "884de7681cfa19cf7de83524becf182fa413e23b",
          "url": "https://github.com/unicode-org/icu/commit/47e9389b8e356aa3d7646ee523a73c36d3aa58ae"
        },
        "date": 1717552834066,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.45635173670303,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "6543634649d0a5686273bf3f75e51fe6a50292db",
          "message": "ICU-22707 fix hst=V: hst=NA for Kirat Rai",
          "timestamp": "2024-06-05T08:10:21-07:00",
          "tree_id": "da3f28d634fa44a28489b9d955d5441f3cdd30ef",
          "url": "https://github.com/unicode-org/icu/commit/6543634649d0a5686273bf3f75e51fe6a50292db"
        },
        "date": 1717600788030,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.40303486090764,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "87fce2423373e1e89049b0f43b4881b456414216",
          "message": "ICU-22642 Avoid spending too much time inside CanonicalIterator\n\nSee #3017",
          "timestamp": "2024-06-06T12:48:59+02:00",
          "tree_id": "f76dffdf81413d8965eae656da8526dd8a6b3375",
          "url": "https://github.com/unicode-org/icu/commit/87fce2423373e1e89049b0f43b4881b456414216"
        },
        "date": 1717671424972,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.20243983527826,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "49699333+dependabot[bot]@users.noreply.github.com",
            "name": "dependabot[bot]",
            "username": "dependabot[bot]"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "4f7b73dfdb79003b386e34e209c79b830fb9def1",
          "message": "ICU-22721 Bump the github-actions group with 2 updates\n\nSee #3021",
          "timestamp": "2024-06-06T13:16:39-07:00",
          "tree_id": "62e29825d2ef11cd7e50ff07af307239d9cfe320",
          "url": "https://github.com/unicode-org/icu/commit/4f7b73dfdb79003b386e34e209c79b830fb9def1"
        },
        "date": 1717705568887,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.22348489848798,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "aefbea616201f69cd800744791baa3ebc4e258f1",
          "message": "ICU-22722 Make cldr-to-icu verbiage on alt=\"ascii\" sound more authoritative",
          "timestamp": "2024-06-07T11:45:29-07:00",
          "tree_id": "5362ef240de0d2859db4e0069dd42d32d455ff22",
          "url": "https://github.com/unicode-org/icu/commit/aefbea616201f69cd800744791baa3ebc4e258f1"
        },
        "date": 1717786086023,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.66867433149994,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "6a24e8d1dfb29b6123bffa91284b00e52a802267",
          "message": "ICU-22310 Update configure files from configure.ac using autoreconf.",
          "timestamp": "2024-06-13T15:09:00+02:00",
          "tree_id": "bbeb1d497395e95fc272c375077b0a88a3cdfc6f",
          "url": "https://github.com/unicode-org/icu/commit/6a24e8d1dfb29b6123bffa91284b00e52a802267"
        },
        "date": 1718285224903,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.56704874646158,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "64f529e113a369afbde9a16d31c73c3e2153753e",
          "message": "ICU-22310 Update configure files from configure.ac using autoreconf.",
          "timestamp": "2024-06-13T15:09:29+02:00",
          "tree_id": "ff9280e5142ea87cbe0533d0df12a9e272ebb880",
          "url": "https://github.com/unicode-org/icu/commit/64f529e113a369afbde9a16d31c73c3e2153753e"
        },
        "date": 1718286345248,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.40812654866363,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "09dce103f2bd6e621a153da3f399d2c52a9f668e",
          "message": "ICU-22797 Move the loop limit before ignore continue\n\nFix an infinity loop issue inside collation builder",
          "timestamp": "2024-06-13T09:58:15-07:00",
          "tree_id": "1b2b3707e6605125b59e986f2aa9325062367ec9",
          "url": "https://github.com/unicode-org/icu/commit/09dce103f2bd6e621a153da3f399d2c52a9f668e"
        },
        "date": 1718298559590,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.4258857096513,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "b82291fd77b0d425d06b6d39c769c0e58cddf9c4",
          "message": "ICU-21810 Fix error checking of ucurr.cpp part 1\n\nSee #3027",
          "timestamp": "2024-06-13T14:31:27-07:00",
          "tree_id": "1425ca53696d03b0fd185b5200481b98be700e72",
          "url": "https://github.com/unicode-org/icu/commit/b82291fd77b0d425d06b6d39c769c0e58cddf9c4"
        },
        "date": 1718314926302,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 190.0413714772187,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "697cb14f08df83f94e2ec9d1046035cdace56783",
          "message": "ICU-22798 Avoid stack overflow by return error.\n\nSee #3035",
          "timestamp": "2024-06-14T13:09:30+05:30",
          "tree_id": "91a2b5fd6deaeb33ec000a1f2178686e59e6a209",
          "url": "https://github.com/unicode-org/icu/commit/697cb14f08df83f94e2ec9d1046035cdace56783"
        },
        "date": 1718351363561,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.06140173751135,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dayeung@chromium.org",
            "name": "David Yeung"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "6e9e120520b73b78961dd957cb3ce762ed560199",
          "message": "ICU-22796 Bugfix: Memory access after uprv_free().",
          "timestamp": "2024-06-17T13:09:27-07:00",
          "tree_id": "7fe2279e96a42dd6488d07caa448a1c393217870",
          "url": "https://github.com/unicode-org/icu/commit/6e9e120520b73b78961dd957cb3ce762ed560199"
        },
        "date": 1718655825646,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 211.00340796949632,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "0ea487a58b6d0aedc061d61402340b68f1c861e8",
          "message": "ICU-21810 Fix memory allocation error check in ucurr.cpp\n\nSee #3039",
          "timestamp": "2024-06-17T15:33:20-07:00",
          "tree_id": "7c923608ba64f9c128ef427ca31a1465c5f5975f",
          "url": "https://github.com/unicode-org/icu/commit/0ea487a58b6d0aedc061d61402340b68f1c861e8"
        },
        "date": 1718664583012,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.24785403948107,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "e5b8660a463821f2c4880cabdfd96f1c0395f24f",
          "message": "ICU-22716 Add uregex_match_fuzzer\n\nBased on https://chromium-review.googlesource.com/c/chromium/deps/icu/+/5465572",
          "timestamp": "2024-06-17T20:06:41-07:00",
          "tree_id": "77e78ba7b9415034087fff7c866e6dae1f1ea96a",
          "url": "https://github.com/unicode-org/icu/commit/e5b8660a463821f2c4880cabdfd96f1c0395f24f"
        },
        "date": 1718680503957,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.17950166151283,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "8f6ba2a7a520ad6bad4fe9b7366e988f590d0ec0",
          "message": "ICU-22721 Correct format specifier for type int32_t (-Wformat).",
          "timestamp": "2024-06-18T09:57:56-07:00",
          "tree_id": "8eeb267458e926187461844dad0dfe311366fd79",
          "url": "https://github.com/unicode-org/icu/commit/8f6ba2a7a520ad6bad4fe9b7366e988f590d0ec0"
        },
        "date": 1718730492083,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.55202712791805,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "m_kato@ga2.so-net.ne.jp",
            "name": "Makoto Kato",
            "username": "makotokato"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "f48944e06297bb5b3ac4ebfa122d3ebceccab51d",
          "message": "ICU-21809 Possible memory leak of tempTable.resFlags",
          "timestamp": "2024-06-18T13:49:23-07:00",
          "tree_id": "5b1bee2c3e12d10643852ed4b936ce88ddcb5797",
          "url": "https://github.com/unicode-org/icu/commit/f48944e06297bb5b3ac4ebfa122d3ebceccab51d"
        },
        "date": 1718744235389,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 199.1508042304264,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "garywade@desisoftsystems.com",
            "name": "Gary L. Wade",
            "username": "garywade"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "b29eb331e1b101117c00e57868eaae147d6677f2",
          "message": "ICU-22720 Update ICU4J to be comparable for relative date formatting as ICU4C for days of the week and quarters\nAdded enums for the days of week to RelativeUnit of RelativeDateTimeFormatter and changed QUARTERS to be included in this change rather than deprecated.  Removed short-circuiting of unit tests for the comparable formatting in ICU4C.  Added changes in formatNumericImpl and RelDateTimeDataSink to use the enums.  Added unit tests to RelativeDateTimeFormatter.java to test the enums proposed and removed the short-circuiting of days-of-week tests.",
          "timestamp": "2024-06-18T13:56:07-07:00",
          "tree_id": "25c1466d9e050ebe54671266ef5d93840deb6cfd",
          "url": "https://github.com/unicode-org/icu/commit/b29eb331e1b101117c00e57868eaae147d6677f2"
        },
        "date": 1718747259802,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.97587856928644,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "m_kato@ga2.so-net.ne.jp",
            "name": "Makoto Kato",
            "username": "makotokato"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "f48944e06297bb5b3ac4ebfa122d3ebceccab51d",
          "message": "ICU-21809 Possible memory leak of tempTable.resFlags",
          "timestamp": "2024-06-18T13:49:23-07:00",
          "tree_id": "5b1bee2c3e12d10643852ed4b936ce88ddcb5797",
          "url": "https://github.com/unicode-org/icu/commit/f48944e06297bb5b3ac4ebfa122d3ebceccab51d"
        },
        "date": 1718916733740,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.58719664828845,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "42d0bab7c3ce6716472a8642c79788ed0ffd38b9",
          "message": "ICU-22800 Avoid inconsistent state inside Locale\n\nThere are some memory leak in Locale which is hard to figure out why.\nUse different variable to track memory allocation to avoid inconsistent\nstate while malloc fail",
          "timestamp": "2024-06-21T11:49:11-07:00",
          "tree_id": "68bc936973d9bf27d7d1a8f3e5d09b8b44998adf",
          "url": "https://github.com/unicode-org/icu/commit/42d0bab7c3ce6716472a8642c79788ed0ffd38b9"
        },
        "date": 1718996708942,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 207.60833698032053,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "86add69c928141d69d8138da43012720b283dc53",
          "message": "ICU-22716 Set timeout limit for uregex_match_fuzzer\n\nTo avoid unnecessary timeout.",
          "timestamp": "2024-06-21T11:49:55-07:00",
          "tree_id": "e5dd4f7e9ff5cc461204029e457964c2d6aeb85d",
          "url": "https://github.com/unicode-org/icu/commit/86add69c928141d69d8138da43012720b283dc53"
        },
        "date": 1718997443048,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.6946575530337,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "cf7ff1b0a508c216c57b61badfa44681cf0978fc",
          "message": "ICU-22716 Set smaller timeout limit for uregex_match_fuzzer\n\n3000 is still too large and cause https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=69869\nset to 300 per andy's earlier suggestion",
          "timestamp": "2024-06-24T14:06:08-07:00",
          "tree_id": "f25ae28a45e208226a2b81cc7eb8484813077a79",
          "url": "https://github.com/unicode-org/icu/commit/cf7ff1b0a508c216c57b61badfa44681cf0978fc"
        },
        "date": 1719263677750,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 216.79720560943957,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "2e00f3866013eba7cd6532aa508db58e5cabc533",
          "message": "ICU-22721 Prevent redundant concurrent CI runs on the same PR branch",
          "timestamp": "2024-06-30T14:13:33-07:00",
          "tree_id": "d9e9c13395c6a79d5d18de9e803024dcd8565eaa",
          "url": "https://github.com/unicode-org/icu/commit/2e00f3866013eba7cd6532aa508db58e5cabc533"
        },
        "date": 1719783169869,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.1632816758398,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "f668bc5218c9eda9ba6df453ccba3642f0f2c21c",
          "message": "ICU-22721 Add workflow that enforces required checks",
          "timestamp": "2024-06-30T21:21:16-07:00",
          "tree_id": "dd0af64228b508bd5792b96e9ddbbd279d4ba753",
          "url": "https://github.com/unicode-org/icu/commit/f668bc5218c9eda9ba6df453ccba3642f0f2c21c"
        },
        "date": 1719808176909,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.38263695213405,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "0178a07a26fa7dd827a49048104b4089aa7e2b84",
          "message": "ICU-22793 Clang-Tidy: google-readability-casting\n\nhttps://releases.llvm.org/17.0.1/tools/clang/tools/extra/docs/clang-tidy/checks/google/readability-casting.html",
          "timestamp": "2024-07-04T22:32:12+02:00",
          "tree_id": "1e145b3ae6d00ed1429738eb486b01d34d03f7ae",
          "url": "https://github.com/unicode-org/icu/commit/0178a07a26fa7dd827a49048104b4089aa7e2b84"
        },
        "date": 1720125657732,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.1895170134519,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "ee93218eabd72406d81f8a1c92a6b9249ceb4de9",
          "message": "ICU-22721 Simplify wait-for-checks match logic",
          "timestamp": "2024-07-11T05:19:59-07:00",
          "tree_id": "ad1de2a4a9928f25b10b2a37739a6e197c463430",
          "url": "https://github.com/unicode-org/icu/commit/ee93218eabd72406d81f8a1c92a6b9249ceb4de9"
        },
        "date": 1720701011668,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.16771022829266,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "clopez@igalia.com",
            "name": "Carlos Alberto Lopez Perez",
            "username": "clopez"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "8ca6bc754599a01689751e8f1c68f482ff5997e8",
          "message": "ICU-22813 Rise the size of the buffers used for the command strings at pkgdata\n\nThe tool pkgdata uses snprintf() to build the strings of the commands that\nwill execute later during the install process. But the maximum size of this\nbuffers is not enough when there is a long path.\n\nThis has caused issues on some CI systems that use very long paths, causing\nthe install process to produce a wrong result.\n\nThe maximum path on Linux is 4096 (defined as PATH_MAX at <linux/limits.h>)\nSo the size of SMALL_BUFFER_MAX_SIZE should be 4096 to avoid errors related\nto truncated paths.",
          "timestamp": "2024-07-19T18:02:53Z",
          "tree_id": "b4bdc4589ecebdb32d0f5d81693eea5b8e4eccad",
          "url": "https://github.com/unicode-org/icu/commit/8ca6bc754599a01689751e8f1c68f482ff5997e8"
        },
        "date": 1721412928703,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.31705974168406,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "40b2ec3c3727bca975824fdb9d1f084207d535ff",
          "message": "ICU-22814 Add CIFuzz to ICU\n\nSee #3059",
          "timestamp": "2024-07-19T15:53:12-07:00",
          "tree_id": "76b92bb15a0c311473fccc11f330217b4baef378",
          "url": "https://github.com/unicode-org/icu/commit/40b2ec3c3727bca975824fdb9d1f084207d535ff"
        },
        "date": 1721430201627,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.88692871203605,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "23d9628f88a2d0127c564ad98297061c36d3ce77",
          "message": "ICU-22801 Try to add LEAKSANITIZER\n\nSee #3041",
          "timestamp": "2024-07-23T09:34:04-07:00",
          "tree_id": "4dc5d46af928caf6b5c27c6f62acf6e37b528643",
          "url": "https://github.com/unicode-org/icu/commit/23d9628f88a2d0127c564ad98297061c36d3ce77"
        },
        "date": 1721753064709,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.06118457213168,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "2cbfd134ef3ad3050a4dbc3e6648373bacdc40e4",
          "message": "ICU-22741 Update CLDR-ICU instructions to mention some hardcoded lists that may need updating\nCo-authored-by: Steven R. Loomis <srl295@gmail.com>",
          "timestamp": "2024-07-26T12:17:00-07:00",
          "tree_id": "fb593549972d9701feb5b0c493826d7d6172182e",
          "url": "https://github.com/unicode-org/icu/commit/2cbfd134ef3ad3050a4dbc3e6648373bacdc40e4"
        },
        "date": 1722021601951,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.15266752167514,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "06c077bd35b3b5950c17ac610ef94803fbf756ab",
          "message": "ICU-22503 add property Indic_Conjunct_Break",
          "timestamp": "2024-07-26T14:47:39-07:00",
          "tree_id": "f4f07fd93c1c6dbac08a9d9a87672eab32e95e03",
          "url": "https://github.com/unicode-org/icu/commit/06c077bd35b3b5950c17ac610ef94803fbf756ab"
        },
        "date": 1722031085195,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 214.37815620533377,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "4acb4724cfc45491261525033aa35788944ebd9e",
          "message": "ICU-22707 Copy new monkey rules to ICU4J",
          "timestamp": "2024-07-18T23:56:34Z",
          "tree_id": "d943a7fc08ec44e585e50cb83791f802a0e3603c",
          "url": "https://github.com/unicode-org/icu/commit/4acb4724cfc45491261525033aa35788944ebd9e"
        },
        "date": 1722281332733,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.02727986181162,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "5d7cbdbc025160a51d96118450c7d1ea1a34e4d8",
          "message": "ICU-22696 Delete unused code.\n\nThese optional output parameters weren't used when these function were\noriginally added so they were most likely included just in case someone\nwould want to use them in the future, but that was 10 years ago now and\nthey still haven't been used yet, so it's unlikely that they'll be used\nin the foreseeable future and call sites as well as the implementation\ncan instead be simplified by removing them.",
          "timestamp": "2024-07-29T22:03:10+02:00",
          "tree_id": "b70d9bbc36861f096d2fa39898e1b263fb8f64ad",
          "url": "https://github.com/unicode-org/icu/commit/5d7cbdbc025160a51d96118450c7d1ea1a34e4d8"
        },
        "date": 1722283945168,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.67362493164825,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "10fe2a6110dfcdf160e95161cb9142bbb0883e8f",
          "message": "ICU-22696 Add uhash support for std::string_view.",
          "timestamp": "2024-07-30T06:45:43+02:00",
          "tree_id": "236e4ad0103e54da5d2539ab06ef970b17078ba6",
          "url": "https://github.com/unicode-org/icu/commit/10fe2a6110dfcdf160e95161cb9142bbb0883e8f"
        },
        "date": 1722315776003,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.71176005454728,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "8891c070bddedb626569cb9b432bf2e11d9ab1bd",
          "message": "ICU-22696 Add implicit conversion from StringPiece to std::string_view.\n\nThis will allow ICU4C to seamlessly use std::string_view internally\nwhile continuing to use StringPiece in the public API.",
          "timestamp": "2024-07-30T06:45:33+02:00",
          "tree_id": "7a057687ea2e526e10d924ffcb3c65ddadc2a86d",
          "url": "https://github.com/unicode-org/icu/commit/8891c070bddedb626569cb9b432bf2e11d9ab1bd"
        },
        "date": 1722349998167,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 205.28252889773663,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "4acb4724cfc45491261525033aa35788944ebd9e",
          "message": "ICU-22707 Copy new monkey rules to ICU4J",
          "timestamp": "2024-07-18T23:56:34Z",
          "tree_id": "d943a7fc08ec44e585e50cb83791f802a0e3603c",
          "url": "https://github.com/unicode-org/icu/commit/4acb4724cfc45491261525033aa35788944ebd9e"
        },
        "date": 1722353326697,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 201.97618670574968,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "3663cc1e0f0226652450fe6af32b7161534707ad",
          "message": "ICU-22707 Add support for property Modifier_Combining_Mark",
          "timestamp": "2024-07-30T15:44:32-07:00",
          "tree_id": "8f6491d0e7858aff68bfe1b77b17dd89565c174d",
          "url": "https://github.com/unicode-org/icu/commit/3663cc1e0f0226652450fe6af32b7161534707ad"
        },
        "date": 1722380018454,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.84628538138455,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "dd65ee3f0b3995ba1b7efcfa0b3bcfe944c0b1fa",
          "message": "ICU-22696 Update ulocimp_getKeywordValue() to use std::string_view.",
          "timestamp": "2024-07-31T15:39:15+02:00",
          "tree_id": "693596c1f99419afc983dbae4c68df2a50046beb",
          "url": "https://github.com/unicode-org/icu/commit/dd65ee3f0b3995ba1b7efcfa0b3bcfe944c0b1fa"
        },
        "date": 1722433755018,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.89145176853566,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "bca5fa50045f571110d89f06784251b6233a2e27",
          "message": "ICU-22826 Fix memory leak",
          "timestamp": "2024-08-01T13:46:58-07:00",
          "tree_id": "12d52be47a93eb5ca5d8c9a6eda0d287d2d59a1d",
          "url": "https://github.com/unicode-org/icu/commit/bca5fa50045f571110d89f06784251b6233a2e27"
        },
        "date": 1722545800067,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.59448591205674,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "2f5a46ffd5013a9d2566c6649d753ebc0cae3540",
          "message": "ICU-22819 Fix memory leak during error",
          "timestamp": "2024-08-05T11:41:06-07:00",
          "tree_id": "1319cdb27414b2ef5f08cb041c1090d8c6c0f106",
          "url": "https://github.com/unicode-org/icu/commit/2f5a46ffd5013a9d2566c6649d753ebc0cae3540"
        },
        "date": 1722884467925,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.18611020691046,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "6c8c6aa430018d8a06273421c735d6b479dfb119",
          "message": "ICU-22827 Fix memLeak by using LocalUResourceBundlePointer",
          "timestamp": "2024-08-05T16:34:16-07:00",
          "tree_id": "9432123298e31a417f008bd11e1bd381c273da5f",
          "url": "https://github.com/unicode-org/icu/commit/6c8c6aa430018d8a06273421c735d6b479dfb119"
        },
        "date": 1722901887291,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 202.85866961559316,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "09ae31427ed3ca61a200567d04ed62363ebbabe9",
          "message": "ICU-22830 Fix memLeak in numrange_fluent.cpp\n\nMaybe related to the leak in ICU-22800",
          "timestamp": "2024-08-05T19:30:54-07:00",
          "tree_id": "8e523d9201dc9e18d05358960e538bfce5341f1e",
          "url": "https://github.com/unicode-org/icu/commit/09ae31427ed3ca61a200567d04ed62363ebbabe9"
        },
        "date": 1722911698355,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.33410893744605,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "575bb781cfdda09b8238337b9e73eacf0f61362f",
          "message": "ICU-22831 Fix memLeak in number_longnames.cpp by using LocalPointer\n\nMaybe fix issues in ICU-22800",
          "timestamp": "2024-08-05T19:28:34-07:00",
          "tree_id": "9452b258654fc8f98c18dc80f70e172c9bf9e16b",
          "url": "https://github.com/unicode-org/icu/commit/575bb781cfdda09b8238337b9e73eacf0f61362f"
        },
        "date": 1722950161084,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.6737401137621,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "49867907f99e5c1908b4066a4e78f16f0af2aac6",
          "message": "ICU-22824 Fix mem Leak during error in uidna.cpp",
          "timestamp": "2024-08-05T11:31:14-07:00",
          "tree_id": "48b55f1d3c4747d3986137b90b2f6f447a7a2610",
          "url": "https://github.com/unicode-org/icu/commit/49867907f99e5c1908b4066a4e78f16f0af2aac6"
        },
        "date": 1722956598582,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.69863509429638,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "6de4472db045c59b1b847a5e1a69f6cd29f65926",
          "message": "ICU-22820 Fix memLeak during error in genrb\n\nFix also ICU-22821 ICU-22822",
          "timestamp": "2024-08-06T18:00:27+02:00",
          "tree_id": "c63010d480171f9c6d4e8a89566fd04bc4687b9f",
          "url": "https://github.com/unicode-org/icu/commit/6de4472db045c59b1b847a5e1a69f6cd29f65926"
        },
        "date": 1722960632121,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.29795550895074,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "a22dc93e3aab556f0b672735622a33fd3853c844",
          "message": "ICU-22825 Fix memLeak during error in tznames_impl.cpp\n\nRewrite the TextTrieMap::put() which should delete the value\nduring error instead of deleting key.\nRewrite to simplified the error handling.",
          "timestamp": "2024-08-06T09:58:15-07:00",
          "tree_id": "368b615ef2089c268c02700ce272b9a35d6bf9a6",
          "url": "https://github.com/unicode-org/icu/commit/a22dc93e3aab556f0b672735622a33fd3853c844"
        },
        "date": 1722964901174,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.96474093005517,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "1eb0ed2fad659a23b837ea7ccbed661cf1b31aff",
          "message": "ICU-22818 Fix memory leak during error in messageformat2_data_model.cpp\n\nSee #3077",
          "timestamp": "2024-08-06T15:39:20-07:00",
          "tree_id": "7a28c74300437f051b52057e32a65fa7eaba73ff",
          "url": "https://github.com/unicode-org/icu/commit/1eb0ed2fad659a23b837ea7ccbed661cf1b31aff"
        },
        "date": 1722984535033,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.91488850427348,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "8a6d59ec80b5229ac2bfa1c3c4202106e2b821f1",
          "message": "ICU-22696 Update ulocimp_to*{Key,Type}() to use std::string_view.",
          "timestamp": "2024-08-07T14:14:23+02:00",
          "tree_id": "a286a67e3bebd9c09c4da85fd2f7a14c7003b3b4",
          "url": "https://github.com/unicode-org/icu/commit/8a6d59ec80b5229ac2bfa1c3c4202106e2b821f1"
        },
        "date": 1723033763321,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 241.0606793911567,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "366bb463b10401776499fc7c6f5eeb0a43252bdd",
          "message": "ICU-22722 Update the ICU Vice-Chair for Maven publishing",
          "timestamp": "2024-08-07T07:36:23-07:00",
          "tree_id": "0600fa1e2651c87527176ed495ef7cce090dc148",
          "url": "https://github.com/unicode-org/icu/commit/366bb463b10401776499fc7c6f5eeb0a43252bdd"
        },
        "date": 1723041660517,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 198.21253324732876,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tjc@igalia.com",
            "name": "Tim Chevalier",
            "username": "catamorphism"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "57ed0a2a53cc1ed5ed61bec6d0cbbc139e1b4542",
          "message": "ICU-22794 MF2: Move .json files for tests into top-level testdata/ directory\n\nModify ICU4C and ICU4J test readers to handle all tests\n\nAdd `ignoreJava` and `ignoreCpp` properties to tests where needed\n\nIncludes parser bug fixes:\n\nICU4J: require a complex-body after declarations\n\nICU4J: Correctly parse the complex body after an unsupported statement\n\nICU4J: Handle date params in tests and remove default params for tests\n\nICU4J: Handle decimal params in tests\n\nICU4J: Require whitespace before variable/literal in reserved annotation\n\nICU4J: Require whitespace between options\n\nICU4J: Require a variable-expression in an .input declaration\n\nICU4J: don't require space between last key and pattern in variant\n\nICU4J: don't require space between selectors\n\nICU4J: allow whitespace after '=' in option\n\nICU4J: parse escape sequences in quoted literals according to grammar\n\nICU4J: allow whitespace within markup after attributes list",
          "timestamp": "2024-08-08T09:14:44-07:00",
          "tree_id": "d637c2ed55a8260a46ac605f14e8a1f49d8cf48a",
          "url": "https://github.com/unicode-org/icu/commit/57ed0a2a53cc1ed5ed61bec6d0cbbc139e1b4542"
        },
        "date": 1723134157137,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 208.01367978791478,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "stefan.stojanovic@janeasystems.com",
            "name": "StefanStojanovic",
            "username": "StefanStojanovic"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "Rahul Pandey",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "66ba09973a4231711b6de0de042f4e532b1873e5",
          "message": "ICU-22787 Fix ClangCL compilation on Windows",
          "timestamp": "2024-08-09T10:54:21+05:30",
          "tree_id": "eaa81518f2faa325769cc8e03e74d3f3ddffccfd",
          "url": "https://github.com/unicode-org/icu/commit/66ba09973a4231711b6de0de042f4e532b1873e5"
        },
        "date": 1723181598083,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 191.8445406160158,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "b5b3e16afac61f9aa9b775aaf497f8cc88ce9481",
          "message": "ICU-22845 Better iterations for the ICU4J UnicodeSet",
          "timestamp": "2024-08-09T08:56:25-07:00",
          "tree_id": "c1e3517413d5486216c70caa2effaeb466f344f4",
          "url": "https://github.com/unicode-org/icu/commit/b5b3e16afac61f9aa9b775aaf497f8cc88ce9481"
        },
        "date": 1723219159706,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.16807446843902,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "0bd2b4d10c81527fdbdddc04c6b0c61325f72cc2",
          "message": "ICU-22849 Fix memLeak in RBBIRuleBuilder by using LocalMemory",
          "timestamp": "2024-08-09T16:19:06-07:00",
          "tree_id": "d9b9b126ec1780010f5227a794cada015cbdf9ba",
          "url": "https://github.com/unicode-org/icu/commit/0bd2b4d10c81527fdbdddc04c6b0c61325f72cc2"
        },
        "date": 1723246441466,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.79896626898164,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "7ffbe77e12d109b8624037994959adba8bb6f6c8",
          "message": "ICU-22696 Update ulocimp_setKeywordValue() to use std::string_view.",
          "timestamp": "2024-08-13T14:03:18+02:00",
          "tree_id": "656ba71a340a9c4c92c3293eaac34d35d298cca2",
          "url": "https://github.com/unicode-org/icu/commit/7ffbe77e12d109b8624037994959adba8bb6f6c8"
        },
        "date": 1723551158114,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.17026912443606,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "72206495de9b76713107810f847d54e4c3ecd209",
          "message": "ICU-22843 UnicodeString <-> std::u16string_view / wstring_view via templates",
          "timestamp": "2024-08-13T09:10:01-07:00",
          "tree_id": "05868c64b4ad0c5af6473d45b21c339d58527d4b",
          "url": "https://github.com/unicode-org/icu/commit/72206495de9b76713107810f847d54e4c3ecd209"
        },
        "date": 1723566004306,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 215.6200192955732,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "bae2aa65d8e99efbd4643ddb2da3cc71701e5070",
          "message": "ICU-22696 Avoid unnecessary copies of already NUL terminated strings.",
          "timestamp": "2024-08-13T21:15:26+02:00",
          "tree_id": "cfe821f8d4434a740017e686db19d55127dc71de",
          "url": "https://github.com/unicode-org/icu/commit/bae2aa65d8e99efbd4643ddb2da3cc71701e5070"
        },
        "date": 1723577033431,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 197.63956432988184,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "37b2bc6999c1de45d739a57d13cfbd92a593cc1d",
          "message": "ICU-22721 Use correct initializer list syntax.\n\nThis will make the code ever so slightly simpler but more importantly\nmake it possible to compile also when using -D_GLIBCXX_DEBUG.",
          "timestamp": "2024-08-13T21:33:53-07:00",
          "tree_id": "2e568f868231da30ad1e6b4a17c9d41537f63ffb",
          "url": "https://github.com/unicode-org/icu/commit/37b2bc6999c1de45d739a57d13cfbd92a593cc1d"
        },
        "date": 1723610520785,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 196.6890359867063,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "dragan@unicode.org",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "committer": {
            "email": "131725218+DraganBesevic@users.noreply.github.com",
            "name": "DraganBesevic",
            "username": "DraganBesevic"
          },
          "distinct": true,
          "id": "045350e7c1e21861d9a664a586a33ad70a7ad387",
          "message": "ICU-22723 Integrate CLDR 46 release alpha0, part 4, fixes for exausting tests",
          "timestamp": "2024-08-14T17:09:35-07:00",
          "tree_id": "0086847282e134f70965b7394e64b27fbf3e0446",
          "url": "https://github.com/unicode-org/icu/commit/045350e7c1e21861d9a664a586a33ad70a7ad387"
        },
        "date": 1723681120369,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 193.03739229508994,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "49699333+dependabot[bot]@users.noreply.github.com",
            "name": "dependabot[bot]",
            "username": "dependabot[bot]"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "distinct": true,
          "id": "f4a04631cd079ff141176f3c4d523198f151eb9e",
          "message": "ICU-22722 Bump the github-actions group across 1 directory with 4 updates\n\nSee #3075",
          "timestamp": "2024-08-15T17:52:40-04:00",
          "tree_id": "e6ad589b7b9b1fc7cd5a87436b7f29dceb2f7405",
          "url": "https://github.com/unicode-org/icu/commit/f4a04631cd079ff141176f3c4d523198f151eb9e"
        },
        "date": 1723759249121,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.55820907434483,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "esther.wang@garmin.com",
            "name": "Esther Wang",
            "username": "EstherWx"
          },
          "committer": {
            "email": "egg.robin.leroy@gmail.com",
            "name": "Robin Leroy",
            "username": "eggrobin"
          },
          "distinct": true,
          "id": "1c312f7caa8cb7847059e6d79715aec3f2e333f8",
          "message": "ICU-22861 std::max deduced conflicting types in unifiedcache.cpp",
          "timestamp": "2024-08-16T10:33:38+02:00",
          "tree_id": "ed8ddafa1874c2445272bdff976bcaca4a57480b",
          "url": "https://github.com/unicode-org/icu/commit/1c312f7caa8cb7847059e6d79715aec3f2e333f8"
        },
        "date": 1723797837142,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 192.88557884365264,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "ed117b99575fd732de19c6029594ccb601e72941",
          "message": "ICU-22864 Move main() out of the ICU namespace.",
          "timestamp": "2024-08-16T20:14:50+02:00",
          "tree_id": "ba89a6e4e9eb4f4973d0bbe4eebc25750ccba1f6",
          "url": "https://github.com/unicode-org/icu/commit/ed117b99575fd732de19c6029594ccb601e72941"
        },
        "date": 1723832639530,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.9769245027013,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mnita@google.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "distinct": true,
          "id": "40189ffe575c66e2acef294765acbf23e653fe76",
          "message": "ICU-21205 Fix Eclipse failing to import the icu4j maven project",
          "timestamp": "2024-08-16T13:38:56-07:00",
          "tree_id": "13f2177fa9abbfc67e096cb152bbcaf377537ec4",
          "url": "https://github.com/unicode-org/icu/commit/40189ffe575c66e2acef294765acbf23e653fe76"
        },
        "date": 1723840949855,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 195.61647565562143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "654010905@qq.com",
            "name": "taiyang-li",
            "username": "taiyang-li"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "0253c828d96b6b97347b432b4f43d63522fe7016",
          "message": "ICU-22832 Fix msan complain about use-of-uninitialized-value",
          "timestamp": "2024-08-19T08:43:38-07:00",
          "tree_id": "747af9f152dcfd9d594e54327e045311e92d3e3e",
          "url": "https://github.com/unicode-org/icu/commit/0253c828d96b6b97347b432b4f43d63522fe7016"
        },
        "date": 1724082944732,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 200.35367450027928,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "305098bdf8cba7a60daf24c9529765dd03abea0d",
          "message": "ICU-22722 Update README.md\n\nSee #3111 and ICU-22722",
          "timestamp": "2024-08-19T13:41:03-05:00",
          "tree_id": "721196f4fe08f372cdd2db7d10baa3a0f5900296",
          "url": "https://github.com/unicode-org/icu/commit/305098bdf8cba7a60daf24c9529765dd03abea0d"
        },
        "date": 1724093121125,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 203.01929674940905,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ashutosh@yandex.ru",
            "name": "Anton Voloshin",
            "username": "ashutosh108"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "e4cba341b655aa0705966c8c310f43d1b2d57603",
          "message": "ICU-22850 ucol_openRules: fix ucol_open reference in the description\n\nIt should obviously be ucol_open here. This typo was introduced in the\nhttps://github.com/unicode-org/icu/commit/0335b3b9c3aaa5d6826b33cb3f45e56b917618e9",
          "timestamp": "2024-08-19T13:03:21-07:00",
          "tree_id": "7e7a00cdb491cc15bcf59014f3edb723926c4d9e",
          "url": "https://github.com/unicode-org/icu/commit/e4cba341b655aa0705966c8c310f43d1b2d57603"
        },
        "date": 1724098275824,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 210.2824848735944,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "51e21af692e95737ad8f75fdf2dbf105fe5811b0",
          "message": "ICU-22707 Unicode 16 aug16",
          "timestamp": "2024-08-19T17:15:00-07:00",
          "tree_id": "708ecdaff4df900a758d028755ef55e1d8db2c4b",
          "url": "https://github.com/unicode-org/icu/commit/51e21af692e95737ad8f75fdf2dbf105fe5811b0"
        },
        "date": 1724113666587,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICUFormat",
            "value": 204.3329265914382,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}