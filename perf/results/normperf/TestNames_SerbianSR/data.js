window.BENCHMARK_DATA = {
  "lastUpdate": 1647353087544,
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
        "date": 1646687296532,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 7.9749,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 7.9662,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.0966,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 7.8922,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 7.8937,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 8.1403,
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
        "date": 1646781682207,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 9.5911,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 9.4199,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 9.4819,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 9.7347,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 9.712,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 9.7108,
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
        "date": 1646782396609,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 8.4759,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 8.4542,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.4433,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 8.7023,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 8.6889,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 8.7625,
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
        "date": 1646783048425,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 8.4868,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 8.4651,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.4516,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 8.705,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 7.6891,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 7.6783,
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
        "date": 1646784576564,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 8.4557,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 8.4384,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.4389,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 8.6706,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 8.6607,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 8.7413,
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
        "date": 1646797356321,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 8.9058,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 8.905,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.8047,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 9.1853,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 9.0101,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 9.0646,
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
        "date": 1646797922347,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 10.6342,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 10.6486,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 10.7087,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 10.183,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 10.2472,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 10.3725,
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
        "date": 1646853793610,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 10.1424,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 10.1047,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 10.1298,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 9.6533,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 9.7164,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 9.8241,
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
        "date": 1646862814028,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 9.7911,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 10.1039,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 9.9582,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 9.3899,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 9.3793,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 9.6545,
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
        "date": 1646868364727,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 8.5923,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 8.5821,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.4831,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 8.7834,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 8.7682,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 8.7919,
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
        "date": 1646874627110,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 9.8849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 9.7371,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 9.9101,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 9.8154,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 9.8527,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 9.9656,
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
        "date": 1646883006941,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 9.6626,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 9.6929,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 9.7197,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 9.5422,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 9.5566,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 9.6832,
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
        "date": 1646934480369,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 9.561,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 8.6915,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.6176,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 8.6557,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 8.5504,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 8.532,
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
        "date": 1646940552771,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 8.7636,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 8.7448,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.7948,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 8.9898,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 8.9745,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 9.0058,
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
        "date": 1646956841159,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 8.7484,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 8.7143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.7482,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 8.8855,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 8.8884,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 8.9393,
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
        "date": 1646964492533,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 7.6979,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 7.6855,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 7.7179,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 7.8453,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 7.823,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 7.8892,
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
        "date": 1646976594600,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 9.0674,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 9.0503,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.9895,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 9.554,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 9.1746,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 9.2309,
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
        "date": 1647026463960,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 8.8113,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 8.7676,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.7638,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 9.0254,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 9.0072,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 9.0303,
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
        "date": 1647033458550,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 10.5228,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 10.4914,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 10.4955,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 10.6746,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 10.6868,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 10.7423,
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
        "date": 1647288114620,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 7.9273,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 7.9214,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.0228,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 7.7804,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 7.7892,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 8.0175,
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
        "date": 1647321522129,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 8.7502,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 8.7413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.7561,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 8.9157,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 8.8919,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 8.9356,
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
        "date": 1647352820863,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 10.1218,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 10.0456,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 10.0772,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 10.2894,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 10.3244,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 10.2588,
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
        "date": 1647353086066,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestICU_NFC_NFD_Text",
            "value": 7.9322,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_NFC_Text",
            "value": 7.9449,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFC_Orig_Text",
            "value": 8.3346,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFD_Text",
            "value": 7.7929,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_NFC_Text",
            "value": 7.7709,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestICU_NFD_Orig_Text",
            "value": 7.9289,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}