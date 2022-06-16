window.BENCHMARK_DATA = {
  "lastUpdate": 1655413244345,
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
          "id": "80ee559205dd165c2d647610376d6f9a06822ae4",
          "message": "ICU-21843 Add ICU4C performance tests to continuous integration\n\nSee #1987",
          "timestamp": "2022-03-07T12:53:44-08:00",
          "tree_id": "7da686bd1662079612215dc8b0f27437626720c3",
          "url": "https://github.com/unicode-org/icu/commit/80ee559205dd165c2d647610376d6f9a06822ae4"
        },
        "date": 1646687232830,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 25.3374,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 18.0496,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 18.4753,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 19.2577,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 20.0695,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 20.4257,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "avetlov@riverlogic.com",
            "name": "Alexey Vetlov",
            "username": "avetlov"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "1393face12f7205a133a63f13bb592f561b4abfb",
          "message": "ICU-21768 Fixed (u_snprintf improperly counts the required buffer size). Modified TestSnprintf to test the null buffer case.",
          "timestamp": "2022-03-08T23:09:54Z",
          "tree_id": "586d92fe70bb8985ec51cb46c5c3b4f26dadd05c",
          "url": "https://github.com/unicode-org/icu/commit/1393face12f7205a133a63f13bb592f561b4abfb"
        },
        "date": 1646781656320,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.7382,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.9708,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.9792,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.148,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.3433,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.2423,
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
          "id": "31871cc14f5b55a53511884d9787494905a80a00",
          "message": "ICU-21801 Fix array comparison warning in uspoof_impl.cpp\n\nSee #2015",
          "timestamp": "2022-03-08T15:21:53-08:00",
          "tree_id": "bd6da903ad6a15330b5714ac76b55b1000680a13",
          "url": "https://github.com/unicode-org/icu/commit/31871cc14f5b55a53511884d9787494905a80a00"
        },
        "date": 1646782382866,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.8534,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.1356,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.1121,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.4316,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.7162,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.6611,
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
          "id": "f57ef9ebf72d889e9b93be90fbc020fae0e374ae",
          "message": "ICU-21527 Assert pattern equality instead of object equality",
          "timestamp": "2022-03-08T15:31:52-08:00",
          "tree_id": "ecf2895c54243a8853b544b889a8c7677e6381b5",
          "url": "https://github.com/unicode-org/icu/commit/f57ef9ebf72d889e9b93be90fbc020fae0e374ae"
        },
        "date": 1646783036120,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 24.2068,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 18.0013,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 18.0578,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.4705,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.6605,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 20.1092,
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
          "id": "f79f03dad5ffcd0e7ac9e1bcbd05fc38bff8e3a2",
          "message": "ICU-21322 Add parse and format methods for DecimalQuantity with exponent\n\nSee #2012",
          "timestamp": "2022-03-08T15:56:50-08:00",
          "tree_id": "e99f29d061b511c3a728814891002ca64d0008cd",
          "url": "https://github.com/unicode-org/icu/commit/f79f03dad5ffcd0e7ac9e1bcbd05fc38bff8e3a2"
        },
        "date": 1646784418606,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.4123,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.2842,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.266,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.2988,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.2707,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.2448,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "daniel.bevenius@gmail.com",
            "name": "Daniel Bevenius",
            "username": "danbev"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "55a51fa9bde41598fb70c5931d313e908f21ca65",
          "message": "ICU-21784 suppress -Wunused-function warning in unistr.cpp\n\nThis commit adds an ignore of the unused function\nuprv_UnicodeStringDummy.",
          "timestamp": "2022-03-09T03:31:37Z",
          "tree_id": "6f7b710d4a93a3e7597bccfba1321df26f0084ee",
          "url": "https://github.com/unicode-org/icu/commit/55a51fa9bde41598fb70c5931d313e908f21ca65"
        },
        "date": 1646797359768,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.4082,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.1424,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.1153,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.407,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.5577,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.5294,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "alexandermv@gmail.com",
            "name": "Alexander Morozov",
            "username": "alxrmorozov"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "234cbe2c17034c68d6dd6df09fda79a052f0a238",
          "message": "ICU-21805 Remove useless check",
          "timestamp": "2022-03-09T03:37:37Z",
          "tree_id": "9e1ab2725eb42216e690fc018fce2b085fddb70f",
          "url": "https://github.com/unicode-org/icu/commit/234cbe2c17034c68d6dd6df09fda79a052f0a238"
        },
        "date": 1646797920182,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.8371,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 18.1541,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.9784,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.4928,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.3233,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 20.36,
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
          "id": "77e0c9e371994b739e4049ab4dc476ab8b8c5719",
          "message": "ICU-21900 Adjusts performance alert threshold to 150%, i.e. the alert only is\ntriggered if the current measured execution time is 50% higher than the previous\ntime.\nThe current setting of 100% means that even a 1% increase from previous time\ntriggers the alert already.",
          "timestamp": "2022-03-09T11:10:43-08:00",
          "tree_id": "430a4aa2ed3e70bf913386f73e673d6391f87c4b",
          "url": "https://github.com/unicode-org/icu/commit/77e0c9e371994b739e4049ab4dc476ab8b8c5719"
        },
        "date": 1646853704774,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 24.0248,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 18.0663,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 18.0309,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.5851,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.6714,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.8209,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies",
            "username": "younies"
          },
          "committer": {
            "email": "younies@chromium.org",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "distinct": true,
          "id": "bb822ba38a8b8e0ebd33d8311579043bd7978794",
          "message": "ICU-21936 Make the internal units converters public\n\nSee #2021",
          "timestamp": "2022-03-09T22:42:04+01:00",
          "tree_id": "1e1a52e65f91a8ea8454578ce854063b1be82290",
          "url": "https://github.com/unicode-org/icu/commit/bb822ba38a8b8e0ebd33d8311579043bd7978794"
        },
        "date": 1646862784294,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.3023,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.287,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.2573,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 13.5143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 14.3521,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 14.3353,
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
          "id": "571d12abfbe73a974fbe9d260b85975cbfd5f43c",
          "message": "ICU-21409 add word for bell to laodict",
          "timestamp": "2022-03-09T15:14:42-08:00",
          "tree_id": "360a095baf4597f47ff422696cb50d2e95e1e59d",
          "url": "https://github.com/unicode-org/icu/commit/571d12abfbe73a974fbe9d260b85975cbfd5f43c"
        },
        "date": 1646868358903,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.4125,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.2775,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.264,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.2969,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.2571,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.2183,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mspector@fastmail.com",
            "name": "Michael Spector",
            "username": "spektom"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "e730bce02f15fecc290fe042cdf6f74cc9a78a4c",
          "message": "ICU-21815 Remove unused line",
          "timestamp": "2022-03-10T01:00:26Z",
          "tree_id": "d9fdd09810b4069b89b80efbb1e108c11f85f1bb",
          "url": "https://github.com/unicode-org/icu/commit/e730bce02f15fecc290fe042cdf6f74cc9a78a4c"
        },
        "date": 1646874625126,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.8354,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 13.457,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 13.464,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.4701,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.6879,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.7164,
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
          "id": "8a5f045992eab8898eff5f6628650e75b8f13bc3",
          "message": "ICU-21900 Fix issue filtering in commit checker",
          "timestamp": "2022-03-09T19:20:02-08:00",
          "tree_id": "40c23771b9eb09cead8bd04f2a099e81767191cb",
          "url": "https://github.com/unicode-org/icu/commit/8a5f045992eab8898eff5f6628650e75b8f13bc3"
        },
        "date": 1646882986963,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.7899,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.5477,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.7663,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.6679,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.7321,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.8271,
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
          "id": "996378821c4ee89e68dc74183359e00f7130de0d",
          "message": "ICU-21839 Add ICU4J test that ISO8601 inherits patterns/symbols grom Gregorian",
          "timestamp": "2022-03-10T09:37:21-08:00",
          "tree_id": "0106e37264406db1343e6cef2ec0b5afbf40768c",
          "url": "https://github.com/unicode-org/icu/commit/996378821c4ee89e68dc74183359e00f7130de0d"
        },
        "date": 1646934471811,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 17.3002,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 12.7062,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 12.7783,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 13.7443,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 14.5566,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 14.6209,
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
          "id": "f374427f6018056a6643c6519bbfadf869755ae0",
          "message": "ICU-21900 integrate CLDR release-41-beta1 to ICU main for 71rc",
          "timestamp": "2022-03-10T11:17:09-08:00",
          "tree_id": "52377cd9d7b956ff5192e4fa0fe3d53b630e9a63",
          "url": "https://github.com/unicode-org/icu/commit/f374427f6018056a6643c6519bbfadf869755ae0"
        },
        "date": 1646940490623,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.6982,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.7074,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.7076,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.8346,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.7662,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.8113,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "7fe330119e6b5503232cfd780869f5ff685ecdea",
          "message": "ICU-21900 ICU4C 71 change report\n\nSee #2024. Includes update to generator that removes the minor version indication from the reports.",
          "timestamp": "2022-03-10T15:50:45-08:00",
          "tree_id": "006f83812ce403ec53e3769323919eb155941a7e",
          "url": "https://github.com/unicode-org/icu/commit/7fe330119e6b5503232cfd780869f5ff685ecdea"
        },
        "date": 1646956851361,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.9857,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.942,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.9648,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.6558,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.7613,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.0255,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "19c13048d24f0dc010d48249331913bf436fedb0",
          "message": "ICU-21900 Update ICU4J Change report (BRS#20)\n\nUpdate to show only major release numbers.",
          "timestamp": "2022-03-10T17:58:16-08:00",
          "tree_id": "eceff86f8dff9a267d5dfed594393cb554113fef",
          "url": "https://github.com/unicode-org/icu/commit/19c13048d24f0dc010d48249331913bf436fedb0"
        },
        "date": 1646964484545,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.6203,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.6187,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.6389,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.4176,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.4695,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.4856,
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
          "id": "b5f10c4a1c19a0fe50bc4faa73062df4ed7d41fc",
          "message": "ICU-21332 Add tests from ICU4C TestDelimiters() into related ICU4J test",
          "timestamp": "2022-03-10T21:20:11-08:00",
          "tree_id": "3155b8800258c99573d9556c3e7be8ede960b1ba",
          "url": "https://github.com/unicode-org/icu/commit/b5f10c4a1c19a0fe50bc4faa73062df4ed7d41fc"
        },
        "date": 1646976613734,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 20.6257,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 15.3958,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.3169,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.6048,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.4923,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.5607,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "zhin@google.com",
            "name": "Ng Zhi An",
            "username": "ngzhian"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "e0bb2ccdeb2d14000c2af63fc530b019f9361200",
          "message": "ICU-21844 Fix variable shadowing",
          "timestamp": "2022-03-11T19:10:53Z",
          "tree_id": "e2752ea8ab56cc092a5a06e9079d324bc5af7500",
          "url": "https://github.com/unicode-org/icu/commit/e0bb2ccdeb2d14000c2af63fc530b019f9361200"
        },
        "date": 1647026459153,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.8131,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 13.4401,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 13.4728,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.4852,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.6805,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.6662,
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
          "id": "df6a8c877c76a120846323e7662016d534d0690f",
          "message": "ICU-13619 Now that UDATPG_NARROW is @stable, can reference it in header",
          "timestamp": "2022-03-11T13:06:50-08:00",
          "tree_id": "3b6dbaef199f1c19fb5e03bbf08bd8e40c6d29a6",
          "url": "https://github.com/unicode-org/icu/commit/df6a8c877c76a120846323e7662016d534d0690f"
        },
        "date": 1647033456221,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.5994,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.6308,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.664,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.3736,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.446,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.4635,
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
          "id": "29111fd19582bb995f6a3a8052d21e4fd756b3b4",
          "message": "ICU-21824 make the Unicode license match exactly\n\nThe ICU copy of the Unicode license was missing some lines that are\nconsidered part of the license text.\n\nSee https://github.com/unicode-org/template-repo/blob/main/LICENSE\nand https://www.unicode.org/license.txt",
          "timestamp": "2022-03-14T19:50:44Z",
          "tree_id": "fc1f892d2ef84e0169155887767528a32ca956a5",
          "url": "https://github.com/unicode-org/icu/commit/29111fd19582bb995f6a3a8052d21e4fd756b3b4"
        },
        "date": 1647288110479,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.6643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.0641,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.4582,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.0015,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.5361,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.8188,
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
          "id": "bfca3ccaac707f5168f5888b2446a4518426bb04",
          "message": "ICU-21875 improve C API coverage",
          "timestamp": "2022-03-15T05:07:59Z",
          "tree_id": "8225a14dcf2fd2abc50cc44b8cbc4b0d4b5643be",
          "url": "https://github.com/unicode-org/icu/commit/bfca3ccaac707f5168f5888b2446a4518426bb04"
        },
        "date": 1647321539962,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 20.5914,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.9321,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.9183,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.9384,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.8632,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.1893,
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
          "id": "811351f67cca8291f0404d9ee30f0acf191b926b",
          "message": "ICU-21900 BRS71 Updated serialization test data for 71.1",
          "timestamp": "2022-03-15T09:47:57-04:00",
          "tree_id": "6fec003664bd37e4fd2da820024913a5620c6207",
          "url": "https://github.com/unicode-org/icu/commit/811351f67cca8291f0404d9ee30f0acf191b926b"
        },
        "date": 1647352695099,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.3156,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.7252,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.7443,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.5688,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.6892,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.73,
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
          "id": "5b4559df24b8b610998b9c3596534dbcf6744bc4",
          "message": "ICU-21900 BRS71 Cleanup import statements in ICU4J",
          "timestamp": "2022-03-15T09:47:25-04:00",
          "tree_id": "710673993a1d12aa96e3404892847295c45f2e07",
          "url": "https://github.com/unicode-org/icu/commit/5b4559df24b8b610998b9c3596534dbcf6744bc4"
        },
        "date": 1647353038979,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.589,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.6286,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.3626,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.434,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.4926,
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
          "id": "dbf7c20be650d9425ba8f25784c3f70b4c07f838",
          "message": "ICU-21942 Fix Kosovo 3-letter code to be XKK for uloc_getISO3Country etc.",
          "timestamp": "2022-03-15T10:59:13-07:00",
          "tree_id": "33cac0a5222e84bdd8a8acd8d8f0092e4f2a2765",
          "url": "https://github.com/unicode-org/icu/commit/dbf7c20be650d9425ba8f25784c3f70b4c07f838"
        },
        "date": 1647367817850,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.6032,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.6155,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.6327,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.5067,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.5095,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.5185,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shaobero@adobe.com",
            "name": "shaobero",
            "username": "shaobero"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "b1269c91211deab8f94c3cf85accb51e5a71ca5c",
          "message": "ICU-21823 Adding changes to fix charset detection incase of no match",
          "timestamp": "2022-03-15T21:47:40Z",
          "tree_id": "4dccceca277645512aea849db22a064750395d60",
          "url": "https://github.com/unicode-org/icu/commit/b1269c91211deab8f94c3cf85accb51e5a71ca5c"
        },
        "date": 1647381565168,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.5876,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.6156,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.6141,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.4133,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.4542,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.4463,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies",
            "username": "younies"
          },
          "committer": {
            "email": "younies@chromium.org",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "distinct": true,
          "id": "f30956fc9c9fd9e83189ac1c8b372dea0832eb90",
          "message": "ICU-21840 Fix formatting Aliases\n\nSee #2016",
          "timestamp": "2022-03-15T23:27:43+01:00",
          "tree_id": "effd861fbdf9feda8b88ac7df6e6337ffc160e42",
          "url": "https://github.com/unicode-org/icu/commit/f30956fc9c9fd9e83189ac1c8b372dea0832eb90"
        },
        "date": 1647383916847,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.9217,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 13.5633,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 13.4506,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.4605,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.6462,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.6831,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "daju@microsoft.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "committer": {
            "email": "41210545+daniel-ju@users.noreply.github.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "distinct": true,
          "id": "741bbddcf9279f41385862be8913ed13b9e342c0",
          "message": "ICU-21900 BRS71RC Update version number and regenerate configure",
          "timestamp": "2022-03-15T18:26:10-05:00",
          "tree_id": "1b2b5e930f41b85f81e6baf2dc1b46dda2462bd9",
          "url": "https://github.com/unicode-org/icu/commit/741bbddcf9279f41385862be8913ed13b9e342c0"
        },
        "date": 1647387402937,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.5125,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.479,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.5018,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.4646,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.7362,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.7292,
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
          "id": "33c9b61b26250e9fe39e098f9f131549d9aa516f",
          "message": "ICU-21871 Make behavior consistent on list format of empty strings",
          "timestamp": "2022-03-15T18:11:57-07:00",
          "tree_id": "fecddd3c2aad1ff68252adb1d70169a10776b5b8",
          "url": "https://github.com/unicode-org/icu/commit/33c9b61b26250e9fe39e098f9f131549d9aa516f"
        },
        "date": 1647393719239,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.6309,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.5947,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.6229,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.4427,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.5168,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.5279,
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
          "id": "633070497465bea1d19d66ad2162a1ec5f4bdda3",
          "message": "ICU-21944 Sync recent uloc_getLanguage/Countries updates to ICU4J; add \"mo\" mapping for C",
          "timestamp": "2022-03-16T09:01:59-07:00",
          "tree_id": "3ddb1ecd0ec14e15c35b4d94a5ec8352fd487ac8",
          "url": "https://github.com/unicode-org/icu/commit/633070497465bea1d19d66ad2162a1ec5f4bdda3"
        },
        "date": 1647447570132,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.812,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.333,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.1071,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.8755,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.3502,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.6875,
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
          "id": "8575c0dacef68253329608650b25412e6db7ca78",
          "message": "ICU-21900 check non-stable API macros, move class boilerplate out of conditionals",
          "timestamp": "2022-03-16T09:03:05-07:00",
          "tree_id": "a400718b943fe30bfd9da7c0ac9a99ed55899aa7",
          "url": "https://github.com/unicode-org/icu/commit/8575c0dacef68253329608650b25412e6db7ca78"
        },
        "date": 1647447974840,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 24.268,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.4948,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.5038,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.4795,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.5664,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.8179,
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
          "id": "89c5d03023d5e96945188bc365a3d15e53441c86",
          "message": "ICU-21900 BRS71 ICU4J API signature file",
          "timestamp": "2022-03-16T12:36:40-04:00",
          "tree_id": "45a430aee8792898a7dc2a9bc6970172d466a6b5",
          "url": "https://github.com/unicode-org/icu/commit/89c5d03023d5e96945188bc365a3d15e53441c86"
        },
        "date": 1647449280145,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.8441,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.7686,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.7196,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.9808,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.7693,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.8048,
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
          "id": "0f49e5086850b0a5171000964a07b570749dc1c3",
          "message": "ICU-21843 Increase performance alert threshold to 200% to avoid false alerts\nfrom occasional spikes.",
          "timestamp": "2022-03-17T09:31:53-07:00",
          "tree_id": "ba17dca7ac476bc685815bcd81bb86df6a6f1d95",
          "url": "https://github.com/unicode-org/icu/commit/0f49e5086850b0a5171000964a07b570749dc1c3"
        },
        "date": 1647535326993,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.4123,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 15.5947,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.5606,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.815,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.7086,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.8214,
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
          "id": "ee6433c297477453bf12cd1cdb83a58ef74aea4e",
          "message": "ICU-21949 tzdata2022a update in ICU",
          "timestamp": "2022-03-17T13:56:20-04:00",
          "tree_id": "e98ba87a01a2366cff5ef67b84a910d40deb71cf",
          "url": "https://github.com/unicode-org/icu/commit/ee6433c297477453bf12cd1cdb83a58ef74aea4e"
        },
        "date": 1647540379363,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.2239,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 15.4502,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.537,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.465,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.3679,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.9538,
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
          "id": "ee6433c297477453bf12cd1cdb83a58ef74aea4e",
          "message": "ICU-21949 tzdata2022a update in ICU",
          "timestamp": "2022-03-17T13:56:20-04:00",
          "tree_id": "e98ba87a01a2366cff5ef67b84a910d40deb71cf",
          "url": "https://github.com/unicode-org/icu/commit/ee6433c297477453bf12cd1cdb83a58ef74aea4e"
        },
        "date": 1647542854672,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.147,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.2187,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.08,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.3645,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.5936,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.5122,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "daju@microsoft.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "committer": {
            "email": "41210545+daniel-ju@users.noreply.github.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "distinct": true,
          "id": "09331b75bf4eba38e9e4eb3c06109c54ad940dd4",
          "message": "ICU-21901 BRS71GA Update version number",
          "timestamp": "2022-03-21T15:01:26-05:00",
          "tree_id": "5a68777dbd795812fe73f8d7d51314dfe6a1d6dd",
          "url": "https://github.com/unicode-org/icu/commit/09331b75bf4eba38e9e4eb3c06109c54ad940dd4"
        },
        "date": 1647893592540,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 20.3235,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 15.1541,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.9131,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.474,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.4078,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.8302,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "1d9cc717e2b93b8b27b84b2969c3bfc9a4d0a593",
          "message": "ICU-21956 Merge maint-71 to main\n\nMerging the maint/maint-71 branch into the main branch.",
          "timestamp": "2022-03-23T12:50:13-07:00",
          "tree_id": "5a68777dbd795812fe73f8d7d51314dfe6a1d6dd",
          "url": "https://github.com/unicode-org/icu/commit/1d9cc717e2b93b8b27b84b2969c3bfc9a4d0a593"
        },
        "date": 1648065638044,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.2862,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.3246,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.3223,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.6063,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.7388,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.704,
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
          "id": "9b3c0c328c78d5ea74bfdff9026f31e91b173c0b",
          "message": "ICU-21901 integrate CLDR release-41-beta2 to ICU main for 71ga",
          "timestamp": "2022-03-23T16:43:02-07:00",
          "tree_id": "aea6b6784ef5f219025dc77f8d8bf48d19777bc9",
          "url": "https://github.com/unicode-org/icu/commit/9b3c0c328c78d5ea74bfdff9026f31e91b173c0b"
        },
        "date": 1648079646680,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.1967,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 13.1158,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 13.1081,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.1978,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.3623,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.3622,
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
          "id": "2aa2a7221c2d28c331ffbe0209b74d3156bbe312",
          "message": "ICU-21953 remove outdated ucdterms.txt\n\nThis is an old version of the Unicode license from 2004,\nwhen the ICU license was different from the Unicode license.\n\nWe should have removed this file in 2016,\nwhen the Unicode license was revised,\nand when the ICU project became a project of the Unicode Consortium,\nadopting the Unicode license for all of ICU.\n\nSince 2016, the Unicode data files are covered by the same license as the\ntop license text in the ICU LICENSE file.",
          "timestamp": "2022-03-24T00:14:02Z",
          "tree_id": "1ceb57fa15c77488db9f7db1a5f553c0d902a004",
          "url": "https://github.com/unicode-org/icu/commit/2aa2a7221c2d28c331ffbe0209b74d3156bbe312"
        },
        "date": 1648081446364,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.5812,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 15.6952,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.7736,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.7634,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.7676,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.7061,
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
          "id": "ed1d9efc153bce41d61dd10b0ee4641b8513c431",
          "message": "ICU-21954 LICENSE: add more license texts from icu4c files",
          "timestamp": "2022-03-24T21:22:05Z",
          "tree_id": "95b674daae808a0ca19eb48d26139fbd81742c43",
          "url": "https://github.com/unicode-org/icu/commit/ed1d9efc153bce41d61dd10b0ee4641b8513c431"
        },
        "date": 1648157578138,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 24.1149,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.3222,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.3514,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 23.4269,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 24.8755,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 24.3861,
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
          "id": "2612b8b55af90106c11422a3558006bdc8016bc4",
          "message": "ICU-21965 fix utilities.jar module name\n\ncom.ibm.icu.utilities - otherwise the automatic module name is unusable",
          "timestamp": "2022-03-30T11:09:53-05:00",
          "tree_id": "c25c55651b6b9eedb525e2eafeff337ca86dedcb",
          "url": "https://github.com/unicode-org/icu/commit/2612b8b55af90106c11422a3558006bdc8016bc4"
        },
        "date": 1648657398949,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 16.2512,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 12.1718,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 12.1952,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 13.0438,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 13.8546,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 13.8642,
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
          "id": "b144aac447e4ddbc20cbec90ed625ef983a64fd9",
          "message": "ICU-21762 Export Script_Extensions with --all flag in icuexportdata\n\nSee #2054",
          "timestamp": "2022-03-30T11:44:02-07:00",
          "tree_id": "9f2a01db8caee468c31e2289b4569774b0bebc48",
          "url": "https://github.com/unicode-org/icu/commit/b144aac447e4ddbc20cbec90ed625ef983a64fd9"
        },
        "date": 1648666511090,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.3996,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.4325,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.4022,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.4275,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.367,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.374,
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
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "c205e7ee49a7086a28b9c275fcfdac9ca3dc815d",
          "message": "ICU-21971 Added a new numeric currecny code SLE/695 for Sierra Leone Leone.",
          "timestamp": "2022-03-30T13:49:51-07:00",
          "tree_id": "f4b713638cc22c33d7948a0c67f139597d39895e",
          "url": "https://github.com/unicode-org/icu/commit/c205e7ee49a7086a28b9c275fcfdac9ca3dc815d"
        },
        "date": 1648674106114,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.7785,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.6338,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.4304,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.0017,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.1847,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.0046,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "committer": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "distinct": true,
          "id": "861e55c246bf56867699a11d274c1875e0690fcc",
          "message": "ICU-21972 Use a shared props file for the ICU Major Version number in the Windows Visual Studio project files.",
          "timestamp": "2022-03-30T20:55:36-07:00",
          "tree_id": "69b9145357b51f281cc1709e50a968a3b0498b49",
          "url": "https://github.com/unicode-org/icu/commit/861e55c246bf56867699a11d274c1875e0690fcc"
        },
        "date": 1648699591743,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.1367,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 13.0739,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 13.1346,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.2867,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.3809,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.4897,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "jubrianc@microsoft.com",
            "name": "Julien Brianceau",
            "username": "jbrianceau"
          },
          "committer": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "distinct": true,
          "id": "a2c90781f14869f6fbd9267d03e155efa192b245",
          "message": "ICU-21967 Remove obsolete references in makedata.vcxproj and makedata_uwp.vcxproj\n\nThese references should have been removed in ICU-20489 and ICU-21420.\nTake the opportunity to update the doc as well.",
          "timestamp": "2022-03-31T11:23:29-07:00",
          "tree_id": "f609c16843cff6482f6dd06be11b759537e87d3b",
          "url": "https://github.com/unicode-org/icu/commit/a2c90781f14869f6fbd9267d03e155efa192b245"
        },
        "date": 1648751613222,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.9446,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.7285,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.6512,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.9895,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.8793,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.7261,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "daju@microsoft.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "committer": {
            "email": "41210545+daniel-ju@users.noreply.github.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "distinct": true,
          "id": "f4f74c1f2ee380c20434f224d7fea05fdb4e91d6",
          "message": "ICU-21957 BRS72RC Update version number to 72.0.1",
          "timestamp": "2022-03-31T16:39:13-07:00",
          "tree_id": "490d3bcef2c5aed9fa04a64e006f6eea14f5098b",
          "url": "https://github.com/unicode-org/icu/commit/f4f74c1f2ee380c20434f224d7fea05fdb4e91d6"
        },
        "date": 1648770555448,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 20.6089,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.9525,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.1751,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.0609,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.8912,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.8998,
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
          "id": "4833cc89b2fae2e8863b46bf1dc785964847e882",
          "message": "ICU-20715 CollationDataBuilder reset outdated prefix+contraction values\n\nSee #2052",
          "timestamp": "2022-04-04T16:10:13Z",
          "tree_id": "32748a15222ff1509cad57b04d3294c0f402e8df",
          "url": "https://github.com/unicode-org/icu/commit/4833cc89b2fae2e8863b46bf1dc785964847e882"
        },
        "date": 1649089314037,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.1355,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.2491,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.2766,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.2849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.5819,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.7326,
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
          "id": "4747484273dc65a18a0469aca4d36840d64c10c6",
          "message": "ICU-21966 Cleanup -Wunused-but-set-variable\n\nSee #2055",
          "timestamp": "2022-04-06T12:51:46-07:00",
          "tree_id": "61c6000acd9127af32ccdebbe58a5458d0f63962",
          "url": "https://github.com/unicode-org/icu/commit/4747484273dc65a18a0469aca4d36840d64c10c6"
        },
        "date": 1649275368383,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.5212,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.4357,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.4042,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.6812,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.572,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.5397,
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
          "id": "87cee860e035e93cee30cc0dab1be8a697f42646",
          "message": "ICU-21984 Fix DateIntervalFormat.normalizeHourMetacharacters() so that it doesn't require the hour and day-period\nfields to appear in any particular order or position in the skeleton string.",
          "timestamp": "2022-04-11T14:37:15-07:00",
          "tree_id": "9d15834932f32762c8a0a350e9263a8dd2a38900",
          "url": "https://github.com/unicode-org/icu/commit/87cee860e035e93cee30cc0dab1be8a697f42646"
        },
        "date": 1649713710650,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.624,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 15.7424,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.6989,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.7823,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.4961,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.3373,
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
          "id": "131146a5f43955eee68693e1e627df13da1ae384",
          "message": "ICU-21984 Fix DateIntervalFormat.normalizeHourMetacharacters() so that it doesn't require the hour and day-period\nfields to appear in any particular order or position in the skeleton string.",
          "timestamp": "2022-04-14T14:21:02-07:00",
          "tree_id": "fb805e6034ebad2b48accba6256dbdf1297371ac",
          "url": "https://github.com/unicode-org/icu/commit/131146a5f43955eee68693e1e627df13da1ae384"
        },
        "date": 1649971944499,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.9319,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.8574,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.0349,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.8258,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.6426,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.7923,
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
          "id": "47824c5568f0a1528c44e5264cb65e9e18705c18",
          "message": "ICU-21956 Merge maint-71 to main",
          "timestamp": "2022-04-14T17:45:38-07:00",
          "tree_id": "b53a205780b474ed928dcf2732513ac8de8743a6",
          "url": "https://github.com/unicode-org/icu/commit/47824c5568f0a1528c44e5264cb65e9e18705c18"
        },
        "date": 1649984144455,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.4881,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.3065,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.2952,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.3187,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.8169,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.5357,
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
          "id": "fca6b342c03d89998eba227d284e232dabbfab47",
          "message": "ICU-21994 Fix heap-buffer-overflow",
          "timestamp": "2022-04-20T12:03:35-07:00",
          "tree_id": "09972e260fbe59552e69bb75887d064410144398",
          "url": "https://github.com/unicode-org/icu/commit/fca6b342c03d89998eba227d284e232dabbfab47"
        },
        "date": 1650482042412,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.5069,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.4266,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.4106,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.6764,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.5756,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.5286,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "17109322+hugovdm@users.noreply.github.com",
            "name": "Hugo van der Merwe",
            "username": "hugovdm"
          },
          "committer": {
            "email": "17109322+hugovdm@users.noreply.github.com",
            "name": "Hugo van der Merwe",
            "username": "hugovdm"
          },
          "distinct": true,
          "id": "50e14fe15bcde3311f3e76bb196977a0724d7e8b",
          "message": "ICU-21959 Update stale Jira tickets in ICU4J TODOs\n\nSee #2062",
          "timestamp": "2022-04-27T23:31:46+02:00",
          "tree_id": "320adf839470614e0950cfe770d84936a3fe9df2",
          "url": "https://github.com/unicode-org/icu/commit/50e14fe15bcde3311f3e76bb196977a0724d7e8b"
        },
        "date": 1651095737133,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 20.9315,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 15.5023,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.4048,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.399,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.4964,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.5016,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "cb6b778e1b5ef6a1bb82e085aaf427fc086331c9",
          "message": "ICU-21959 Online demos how to update\n\nSee #2069",
          "timestamp": "2022-04-29T09:36:51-07:00",
          "tree_id": "0e8fc014a5ef382a0bcb4e65b3f1013839bed3db",
          "url": "https://github.com/unicode-org/icu/commit/cb6b778e1b5ef6a1bb82e085aaf427fc086331c9"
        },
        "date": 1651250861457,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.3431,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 12.9582,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 12.9562,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.3241,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.4544,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.4591,
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
          "id": "43d082665e72d853ef66626e0b17470722776497",
          "message": "ICU-22006 icupkg: %%ALIAS & %%Parent do not need truncation parent",
          "timestamp": "2022-04-29T17:50:11Z",
          "tree_id": "cc8b7de3a086a733ec693be2cd739669cd6d57c9",
          "url": "https://github.com/unicode-org/icu/commit/43d082665e72d853ef66626e0b17470722776497"
        },
        "date": 1651255214338,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.6091,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.4302,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.4863,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.5457,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.6279,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.6698,
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
          "id": "505535813fd539800de0930883cdb0e0ba678842",
          "message": "ICU-22007 remove outdated terms of use from Unicode conversion files",
          "timestamp": "2022-04-29T19:51:01Z",
          "tree_id": "0648d877b9f7113b462aa8be66a35844db09208a",
          "url": "https://github.com/unicode-org/icu/commit/505535813fd539800de0930883cdb0e0ba678842"
        },
        "date": 1651262503456,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.4141,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 12.9518,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 12.9347,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.4303,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.6103,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.5896,
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
          "id": "07a50207b7a0ae162a5b51856f43576f39751377",
          "message": "ICU-21679 avoid escape of U+FFFF in a character literal",
          "timestamp": "2022-04-29T20:43:53Z",
          "tree_id": "0eab5a9224a4cf8dcff516967a14f248572b25be",
          "url": "https://github.com/unicode-org/icu/commit/07a50207b7a0ae162a5b51856f43576f39751377"
        },
        "date": 1651265645560,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.459,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.3583,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.3352,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.8035,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.893,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.863,
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
          "id": "e96e9410bde06962c211fa6f21c3d91263a90f86",
          "message": "ICU-22005 Fix int32 overflow in FormattedStringBuilder\n\nSee #2070",
          "timestamp": "2022-04-29T18:25:01-07:00",
          "tree_id": "02cea832ea6354cb413ef3ac4167fba0fde781bd",
          "url": "https://github.com/unicode-org/icu/commit/e96e9410bde06962c211fa6f21c3d91263a90f86"
        },
        "date": 1651282490301,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.3566,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.3281,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.3355,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.769,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.5051,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.4907,
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
          "id": "eea7985e5a7108d00f1224ed36f0220fa9441cdc",
          "message": "ICU-22016 utrie2.h need not include mutex.h",
          "timestamp": "2022-05-06T20:50:58Z",
          "tree_id": "f2382974fa83f3747ccc3c7bf9e390d0f9d141dc",
          "url": "https://github.com/unicode-org/icu/commit/eea7985e5a7108d00f1224ed36f0220fa9441cdc"
        },
        "date": 1651870916252,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.6279,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.8353,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.7628,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.2462,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.2608,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.2971,
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
          "id": "fda4a82bba405579a280b71922056c801c2b3ca2",
          "message": "ICU-21960 fix clang 13 C++20 warnings",
          "timestamp": "2022-05-07T15:21:08Z",
          "tree_id": "d4b6c9e51f35a2735c3c07de3993e596cb42e051",
          "url": "https://github.com/unicode-org/icu/commit/fda4a82bba405579a280b71922056c801c2b3ca2"
        },
        "date": 1651937551391,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.3712,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.2226,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.2573,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.2125,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.4001,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.3317,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "chetanladdha@microsoft.com",
            "name": "Chetan Laddha",
            "username": "chetanladdha"
          },
          "committer": {
            "email": "chetan.laddha@gmail.com",
            "name": "Chetan Laddha",
            "username": "chetanladdha"
          },
          "distinct": true,
          "id": "5961aacd3c66f3bb8f51d9b4833c03613c57dc01",
          "message": "ICU-21945 Add support for Visual Studio 2022\n\nSee #2059",
          "timestamp": "2022-05-10T22:57:18+05:30",
          "tree_id": "0a38273102e79b4fb0c96aae8fd320b79fae8c57",
          "url": "https://github.com/unicode-org/icu/commit/5961aacd3c66f3bb8f51d9b4833c03613c57dc01"
        },
        "date": 1652204308377,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.8248,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.4849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.5348,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.5514,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.6858,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.7515,
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
          "id": "bdcec144b9f627c8f96261a6afeebb52e4d81ab0",
          "message": "ICU-22012 Add four Japanese word into the dictionary\n\nSee #2072",
          "timestamp": "2022-05-11T08:19:53-07:00",
          "tree_id": "85e2beca4e203db5c621d2700bc85b0a8c9b1b52",
          "url": "https://github.com/unicode-org/icu/commit/bdcec144b9f627c8f96261a6afeebb52e4d81ab0"
        },
        "date": 1652283083279,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.341,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 13.0452,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 13.0536,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.4293,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.5727,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.4587,
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
          "id": "f8a0810a5a9a8811d13e1e4d9ef266ef5a102729",
          "message": "ICU-22030 Modifies GHA CI performance testing so that existing files in the\nperformance results publishing repository are no longer deleted when the test\nresults are forwarded. This concretely affects the README file in the icu-perf\nrepository, which got deleted with the first data transfer.\n\nRestoring the README file in icu-perf will be a complementary PR.",
          "timestamp": "2022-05-13T17:00:05-07:00",
          "tree_id": "9ba8576b904411dc587d6603ba71189588114912",
          "url": "https://github.com/unicode-org/icu/commit/f8a0810a5a9a8811d13e1e4d9ef266ef5a102729"
        },
        "date": 1652487110077,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.0374,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.1327,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.9136,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.0548,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.9952,
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
          "id": "85705f04e03f9cb41d4406bcd38c3e419eb7889d",
          "message": "ICU-21960 C++20 Warnings from ATOMIC_VAR_INIT\n\nRemove the ICU macros ATOMIC_INT32_T_INITIALIZER and U_INITONCE_INITIALIZER,\nwhich made use of C++ ATOMIC_VAR_INIT, which has been removed from C++20.\n\nWith modern C++ features being available, these macros no longer served\nany real need.",
          "timestamp": "2022-05-17T15:45:06-07:00",
          "tree_id": "4f972e8ea8588a819020277d8c518b5102e52695",
          "url": "https://github.com/unicode-org/icu/commit/85705f04e03f9cb41d4406bcd38c3e419eb7889d"
        },
        "date": 1652828112627,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.5494,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.4435,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.4264,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.7005,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.5495,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.518,
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
          "id": "3d89af0f72d78d0825fbf17fd7808a4d80b6b4ad",
          "message": "ICU-22023 Fix Calendar::get() return out of bound value and SimpleDateTime::format assert while TimeZone is \"UTC\" and value is -1e-9\n\nSee #2086",
          "timestamp": "2022-05-19T13:45:59-07:00",
          "tree_id": "b9cf8e61607c058b52fb71efef44dedd843e1ae7",
          "url": "https://github.com/unicode-org/icu/commit/3d89af0f72d78d0825fbf17fd7808a4d80b6b4ad"
        },
        "date": 1652993764827,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.5324,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.3352,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.3078,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.4286,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.3455,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.3531,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "committer": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "distinct": true,
          "id": "f6300c97cd4b8f1224776e43226d3a8bbb695c2c",
          "message": "ICU-22040 Update clang-13 build bots to clang-14. Add libc++ build bot.",
          "timestamp": "2022-05-19T14:05:14-07:00",
          "tree_id": "4a40a3af51ba2ebe33f2fbb66613585625f0a00c",
          "url": "https://github.com/unicode-org/icu/commit/f6300c97cd4b8f1224776e43226d3a8bbb695c2c"
        },
        "date": 1652994915804,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.187,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 12.9645,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 12.9355,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.179,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.3101,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.3013,
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
          "id": "8fcb22f88e7100cd714861b672a3137c7d431a01",
          "message": "ICU-22037 Adds performance tests for ICU forward and backward\nstring search.\n\nICU-22037 Removes a left-over escaped doublequote; removes a\ntest print-out.",
          "timestamp": "2022-05-20T11:56:11-07:00",
          "tree_id": "52eaabe6b81058bf46f32476bad6441fe6d1175c",
          "url": "https://github.com/unicode-org/icu/commit/8fcb22f88e7100cd714861b672a3137c7d431a01"
        },
        "date": 1653073626074,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 23.4016,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.2071,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.1314,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.5091,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.5973,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.5592,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies@chromium.org",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "committer": {
            "email": "younies@chromium.org",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "distinct": true,
          "id": "fcc981a5b7a11628187ee6a1f97203449b48bd36",
          "message": "ICU-21935 Add DisplayOptions\n\nSee #2061",
          "timestamp": "2022-05-21T03:20:37+02:00",
          "tree_id": "11b85ea79fcb3796ba2871ae8602fce888bb0309",
          "url": "https://github.com/unicode-org/icu/commit/fcc981a5b7a11628187ee6a1f97203449b48bd36"
        },
        "date": 1653096696365,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.5265,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.8416,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.8215,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.9412,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.4269,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.0427,
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
          "id": "74a723f22fe2c53445087215d0d1acee69efdda6",
          "message": "ICU-21959 Fix the URL to the icu-le-hb repository.\n\nhttps://sourceforge.net/p/icu/mailman/message/37657918/",
          "timestamp": "2022-05-24T20:55:00+02:00",
          "tree_id": "9acb2514b5ff0f3877d1f8808f75c88511903b33",
          "url": "https://github.com/unicode-org/icu/commit/74a723f22fe2c53445087215d0d1acee69efdda6"
        },
        "date": 1653419338855,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.2434,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 12.9101,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 12.8993,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.1708,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.355,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.3457,
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
          "id": "f5f4813d161d685a807aa5ea25efa685b0f6eaeb",
          "message": "ICU-22043 Change minimum ZONE_OFFSET value from -12h to -16h\n\nSee #2100",
          "timestamp": "2022-05-24T16:11:39-07:00",
          "tree_id": "affebcf816f9f90df71ea765d812209d8e6490d9",
          "url": "https://github.com/unicode-org/icu/commit/f5f4813d161d685a807aa5ea25efa685b0f6eaeb"
        },
        "date": 1653434732574,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 17.9789,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.3391,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.3335,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.4303,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.3297,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.3086,
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
          "id": "e17219582ed0396d993927066e95af0c9199f8db",
          "message": "ICU-21980 add missing comments, finish change log",
          "timestamp": "2022-05-25T18:23:11Z",
          "tree_id": "f3a73d2dbd02b2945b6b3253c0c5e0532259f4fd",
          "url": "https://github.com/unicode-org/icu/commit/e17219582ed0396d993927066e95af0c9199f8db"
        },
        "date": 1653503660463,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.5563,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.331,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.3234,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.4414,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.379,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.3478,
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
          "id": "8d5a97ae0f49f6974372736ca67db15c37522f6f",
          "message": "ICU-21935 DisplayOptions cleanup",
          "timestamp": "2022-05-26T18:30:07Z",
          "tree_id": "398c8d5544c475dd1e10c83bfec4a84e5b6f66d9",
          "url": "https://github.com/unicode-org/icu/commit/8d5a97ae0f49f6974372736ca67db15c37522f6f"
        },
        "date": 1653590436428,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.4848,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.302,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.3016,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.3701,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.3206,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.3123,
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
          "id": "fc64f8db3f21464b748d4e22383268e9cf5cdb58",
          "message": "ICU-22041 Fix \"Africa/Casablanca\" show strange LONG displayName\n\nSee #2096",
          "timestamp": "2022-05-26T14:59:10-07:00",
          "tree_id": "458cb5f8db5aad448a044731d505ac5a4701a132",
          "url": "https://github.com/unicode-org/icu/commit/fc64f8db3f21464b748d4e22383268e9cf5cdb58"
        },
        "date": 1653603078405,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.8893,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.4904,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.4663,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.1729,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.9799,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.9863,
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
          "id": "64b35481263ac4df37a28a9c549553ecc9710db2",
          "message": "ICU-21957 integrate CLDR release-42-m1 (early milestone) to ICU main for 72 (rebased on main) +\nFormattedStringBuilderTest::testInsertOverflow infolns,logKnownIssue skip for CI exhaustive crash",
          "timestamp": "2022-05-27T13:50:43-07:00",
          "tree_id": "df4c6331dadbb97587cbcf5982199f57d20160b3",
          "url": "https://github.com/unicode-org/icu/commit/64b35481263ac4df37a28a9c549553ecc9710db2"
        },
        "date": 1653685963909,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.731,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.6927,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.4977,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.1499,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.3894,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.2026,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies@chromium.org",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "committer": {
            "email": "younies@chromium.org",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "distinct": true,
          "id": "d6fc9b828c95f0d8d10a8648b3471459d05fb8c4",
          "message": "ICU-21935 Add DisplayOptions to NumberFormatterSettings\n\nSee #2099",
          "timestamp": "2022-05-31T13:02:01-07:00",
          "tree_id": "e043f84264e963b665a5b126a084fab5dd042bbd",
          "url": "https://github.com/unicode-org/icu/commit/d6fc9b828c95f0d8d10a8648b3471459d05fb8c4"
        },
        "date": 1654027950333,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.5438,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.3237,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.3084,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.4391,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.3466,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.3179,
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
          "id": "c5d0fff5a07390f8cf65db8e177489e5b03ff422",
          "message": "ICU-21980 parse multiple `@missing` lines",
          "timestamp": "2022-06-02T21:29:24Z",
          "tree_id": "14e81937066103dfc9353c958a0bb41c72dbf84d",
          "url": "https://github.com/unicode-org/icu/commit/c5d0fff5a07390f8cf65db8e177489e5b03ff422"
        },
        "date": 1654205962128,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.5529,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.3523,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.3081,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.4365,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.3483,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.3126,
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
          "id": "33af80e980ff918ab4e34d85dd9e3d864ad17e2e",
          "message": "ICU-21957 improve logKnownIssue skip for FormattedStringBuilderTest::testInsertOverflow crash",
          "timestamp": "2022-06-03T09:02:31-07:00",
          "tree_id": "62e75a5c1e84e5ed56035da1986beab420f8c1f4",
          "url": "https://github.com/unicode-org/icu/commit/33af80e980ff918ab4e34d85dd9e3d864ad17e2e"
        },
        "date": 1654272844905,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.1184,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 12.7894,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 12.7789,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 14.0439,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 15.2087,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 15.2008,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "agrieve@chromium.org",
            "name": "agrieve",
            "username": "agrieve"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "18dd0e4c223716b73e2b0735cc2b653cef5ee1ab",
          "message": "ICU-21960 Add missing \"const\" to kAttributeKey\n\nThis variable was flagged by a chromium check that looks for variables named like constants that end up in the `.data` ELF section (rather than in `.rodata`).",
          "timestamp": "2022-06-08T16:56:24Z",
          "tree_id": "27918448252d8943e20b1969b94b193224488ead",
          "url": "https://github.com/unicode-org/icu/commit/18dd0e4c223716b73e2b0735cc2b653cef5ee1ab"
        },
        "date": 1654708015692,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 20.2693,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.9511,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.691,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.836,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.0455,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.0276,
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
          "id": "0dcb1cb065d60685aa487848050c3b385c9e6c0b",
          "message": "ICU-22035 Remove instances of pre c++11 default constructor prevention pattern",
          "timestamp": "2022-06-08T16:28:02-07:00",
          "tree_id": "eff466f1fdf88e3b23d1de71c12ac226948e7455",
          "url": "https://github.com/unicode-org/icu/commit/0dcb1cb065d60685aa487848050c3b385c9e6c0b"
        },
        "date": 1654731928274,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.2882,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 15.9352,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.8203,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.9874,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.2793,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.8917,
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
          "id": "1eb6f38b96116b456eaf2c06726a7d764c4df62d",
          "message": "ICU-22053 Clean up usages of Macro Header Guards",
          "timestamp": "2022-06-09T10:18:28-07:00",
          "tree_id": "ec128dbd6d1be5eb97d215131cd69b53c0d201c3",
          "url": "https://github.com/unicode-org/icu/commit/1eb6f38b96116b456eaf2c06726a7d764c4df62d"
        },
        "date": 1654795774619,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.6474,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.9245,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.4828,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.6708,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.9055,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.8886,
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
          "id": "df8fbc22e8dbb7adb7a0677692184e508ebd7e2e",
          "message": "ICU-22036 Modify ICU4J performance test framework to work when executed by\nGitHub Action. This includes introducing a new flag (-a) to indicate\ninvocation from command line/GHA shell, moving elements of the workflow\nfrom Perl into the Java framework (calculation of iteration numbers), and\ngenerating NDJSON output for GitHub Benchmark processing.\nBackward compatibility with the Perl script invocation has been preserved.\n\nICU-22036 Adds a comment clarifying the reason why NDJSON output is generated.",
          "timestamp": "2022-06-09T14:57:21-07:00",
          "tree_id": "85631259fdd484ac01da942bdb50b24f3d40e06b",
          "url": "https://github.com/unicode-org/icu/commit/df8fbc22e8dbb7adb7a0677692184e508ebd7e2e"
        },
        "date": 1654812652405,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 17.9745,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 14.2395,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 14.2516,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 15.1791,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.2343,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.2596,
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
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "c5872e7f679e2acdcc00fda97da92e2d82dbc5e3",
          "message": "ICU-22017 Fix numbering system resolution in NumberRangeFormatter\n\nSee #2085",
          "timestamp": "2022-06-15T13:08:46-07:00",
          "tree_id": "ad7ac4ace1153b30fadd7deadaac7d7e44615d15",
          "url": "https://github.com/unicode-org/icu/commit/c5872e7f679e2acdcc00fda97da92e2d82dbc5e3"
        },
        "date": 1655324374285,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 22.2293,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 15.9541,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.6576,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.5813,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.2152,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.2781,
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
          "id": "5a77fd9d110fba9f6fd4b16a5b969d64facd766e",
          "message": "ICU-21997 Fixes currency code retrieval for locale: instead of selecting the\nfirst currency in the list now select the first legal tender currency in the\nlist. Or the first currency if the list has no legal tender currencies (which\nis the previous behaviour).\n\nICU-21997 Removed an overlooked earlier unit test attempt.\n\nICU-21997 Shields C++ unit test from compilation when configuration flag\nUCONFIG_NO_FORMATTING is set.",
          "timestamp": "2022-06-16T13:49:15-07:00",
          "tree_id": "a749cb19ab2ddd2b88b536b237fe01a7cf76dec7",
          "url": "https://github.com/unicode-org/icu/commit/5a77fd9d110fba9f6fd4b16a5b969d64facd766e"
        },
        "date": 1655413235361,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.1151,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 15.8677,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 15.9968,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.9626,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.8024,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.9344,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}