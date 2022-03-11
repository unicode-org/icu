window.BENCHMARK_DATA = {
  "lastUpdate": 1647026476482,
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
        "date": 1646687338007,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 17.6833,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.6755,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.5906,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.9015,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.9012,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.8444,
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
        "date": 1646781671565,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.7069,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 19.9027,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 19.7387,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 20.0074,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.8884,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 20.046,
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
        "date": 1646782448388,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 17.6668,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.6626,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.5845,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.8965,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.8936,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.8178,
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
        "date": 1646783142347,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 17.7636,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.792,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.6504,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.0021,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.9441,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.7875,
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
        "date": 1646784595236,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 16.871,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.8687,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.8705,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.3018,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.3175,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.289,
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
        "date": 1646797360295,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.5374,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 21.7031,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 21.6088,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 21.1,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 21.2645,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 20.797,
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
        "date": 1646797931781,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 16.5518,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.4072,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.2079,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 16.0798,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 16.2433,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 16.1105,
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
        "date": 1646853807764,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 21.1643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 21.1788,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 21.0944,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 21.4791,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 21.45,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 21.4033,
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
        "date": 1646862813058,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.958,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 19.9197,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 19.9494,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 19.6302,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 19.6242,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.8027,
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
        "date": 1646868357508,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 17.6741,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.676,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.5903,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.8587,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.8614,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.7981,
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
        "date": 1646874612134,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 17.917,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.9037,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.8434,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.8745,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.8671,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.8105,
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
        "date": 1646883003830,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.3642,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 18.1757,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 18.7576,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.9228,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.6327,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 20.9517,
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
        "date": 1646934474607,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 16.9105,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 16.8397,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 16.8518,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.1754,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.1771,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 17.1555,
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
        "date": 1646940592445,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.1221,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 18.1217,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 18.0706,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.4373,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.4305,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.3541,
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
        "date": 1646956858529,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 17.7544,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 17.7062,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 17.7494,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 17.8306,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 17.743,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.9412,
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
        "date": 1646964492444,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 18.6322,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 18.6238,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 18.8058,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 19.0752,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.903,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 19.0901,
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
        "date": 1646976591069,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 19.3255,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 19.1441,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 19.1586,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 18.8776,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 18.3996,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 18.9103,
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
        "date": 1647026475087,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 20.9737,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 21.0067,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 21.075,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 21.0496,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 21.0533,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 21,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}