window.BENCHMARK_DATA = {
  "lastUpdate": 1649972277973,
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
        "date": 1646687816891,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 18.9293,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 20.3467,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 22.983,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 31.0679,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 37.6227,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 39.4043,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 22.521,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 15.6418,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 172.1561,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 23.8727,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 49.9261,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 42.1532,
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
        "date": 1646781993246,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.2804,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.7565,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 24.658,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.3567,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.2904,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 42.2717,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 24.0849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.7229,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 179.6346,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.4615,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 53.1914,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 44.7796,
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
        "date": 1646782924698,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 24.4427,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 26.1652,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 29.6503,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 40.0894,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 48.5157,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 50.8752,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 28.9704,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 20.0384,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 201.5077,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 30.6163,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 63.9813,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 53.9196,
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
        "date": 1646783575783,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.3554,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.7986,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 24.6843,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.3017,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.3004,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 42.3303,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 24.0682,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.667,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 173.4969,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.4501,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 53.2806,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 44.9788,
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
        "date": 1646785098335,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.3441,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.7895,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 24.6902,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.3377,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.3286,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 42.3413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 24.1668,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.7205,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 168.1652,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.5171,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 53.2645,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 44.8579,
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
        "date": 1646797674898,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 17.9312,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 19.1998,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 21.8108,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 29.3859,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 35.5615,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 37.4479,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 21.2595,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 14.7212,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 159.0644,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 22.4667,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 46.8771,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 39.6156,
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
        "date": 1646798399841,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.3815,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.9098,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.4515,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.7581,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 41.8219,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 40.1562,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.4468,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.2694,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 155.3343,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 27.8799,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 52.7655,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 48.0255,
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
        "date": 1646854248556,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.4428,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.9021,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.3671,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.779,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 41.8363,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 40.16,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.4226,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.3134,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 154.3414,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 27.8078,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 52.7507,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 47.9964,
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
        "date": 1646863137013,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.4188,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.8644,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 24.7059,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.3911,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.4355,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 42.6256,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 24.1246,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.5926,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 177.9765,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.6579,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 53.3872,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 44.9282,
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
        "date": 1646868703678,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.4232,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.88,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.362,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.7385,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 41.8613,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 40.1784,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.428,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.2592,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 151.5862,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 27.7931,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 52.7234,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 48.0111,
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
        "date": 1646875141816,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 19.5972,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.3506,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 24.331,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 32.9618,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 38.8956,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 40.2114,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 23.5902,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.3336,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 175.3649,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 24.3329,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 53.5479,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 41.8441,
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
        "date": 1646883343085,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.5667,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.274,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.4605,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.8456,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.8741,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 41.8759,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 24.3349,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.934,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 180.5901,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.4811,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 56.5416,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 43.133,
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
        "date": 1646934848526,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.2582,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.944,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.5243,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.7771,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.7151,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 41.6975,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 23.1063,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.1929,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 165.2428,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.5812,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 59.3903,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 48.2959,
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
        "date": 1646941048204,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 22.8637,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 24.7323,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 28.5589,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 39.4475,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 46.088,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 46.6974,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 26.2164,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 18.3425,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 189.5353,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 28.2282,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 67.8776,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 51.7864,
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
        "date": 1646957223429,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.5809,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.3767,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.5857,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.8982,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.8343,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 42.2717,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 24.3327,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.9296,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 183.3588,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.539,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 56.4566,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 43.5198,
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
        "date": 1646964835170,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 24.7482,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 26.8338,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 30.7025,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 40.5833,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 47.9304,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 49.8629,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 29.1155,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 20.162,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 199.3036,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 30.4948,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 66.8194,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 51.6923,
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
        "date": 1646976947346,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 23.455,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 24.3757,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 28.3844,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 38.1922,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 44.4412,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 47.3697,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 27.678,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 18.4411,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 186.9809,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 28.9632,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 64.1202,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 49.9845,
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
        "date": 1647026814984,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.6466,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.794,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.8242,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.8308,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 42.0077,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 40.4317,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.9687,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.1489,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 151.9812,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.7543,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 54.6121,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.6333,
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
        "date": 1647033804606,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 22.5987,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 24.4462,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 28.1005,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 38.224,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 45.5209,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 46.7379,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 26.6813,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 18.7699,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 184.6528,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 28.2422,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 62.3166,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 48.2052,
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
        "date": 1647288461802,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.5613,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.2712,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.4812,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.8703,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.7859,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 41.8505,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 24.3652,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.9572,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 181.1844,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.5323,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 56.5133,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 43.1747,
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
        "date": 1647321899703,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 22.1052,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 24.0556,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 27.7419,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 38.5417,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 45.359,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 45.376,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.1846,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.7939,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 175.8971,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 27.4397,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 65.1148,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 50.3848,
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
        "date": 1647353267128,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 24.6746,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 26.8929,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 30.5689,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 40.7654,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 49.024,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 50.2751,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 29.1916,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 20.3206,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 205.5147,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 30.5995,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 67.7376,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 51.8149,
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
        "date": 1647353599934,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.5975,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.4703,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.5321,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.0283,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.8812,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 41.9442,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 24.338,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.0258,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 179.9107,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.5712,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 56.3872,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 43.1666,
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
        "date": 1647368609024,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 23.1056,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 25.0293,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 28.4897,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 38.0042,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 46.0637,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 46.8599,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 27.1529,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 19.045,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 186.2818,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 28.4785,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 63.1731,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 50.5043,
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
        "date": 1647382235076,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 24.3443,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 26.5216,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 30.2249,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 40.2888,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 48.3497,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 49.6162,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 28.7615,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 20.0838,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 197.8325,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 30.1749,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 66.9269,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 51.5239,
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
        "date": 1647384362086,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.5569,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.4098,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.501,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 33.9234,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.8675,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 41.9258,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 24.2489,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.9946,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 179.7679,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.5182,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 56.3439,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 43.0533,
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
        "date": 1647388071316,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.6981,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.5954,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.3498,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.8056,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 43.4385,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 42.0908,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 23.9589,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.3317,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 175.2441,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 27.7335,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 62.9018,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 47.2991,
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
        "date": 1647394300419,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.8119,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.9351,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.74,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.7212,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 42.1482,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 40.5738,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.8664,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.2884,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 152.3364,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.7959,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 54.7165,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.622,
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
        "date": 1647448083959,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 22.3899,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 24.7634,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 28.4514,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 36.6817,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 45.5183,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 45.3704,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 27.1955,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 18.0141,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 169.804,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 27.9941,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 60.0447,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.4977,
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
        "date": 1647448410804,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.7169,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 21.5974,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 27.185,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.1246,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.6138,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 39.5781,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 27.5487,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.5284,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 146.8135,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 27.365,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 53.5216,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 47.4544,
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
        "date": 1647449702809,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.136,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.3465,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.8711,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.5365,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 41.9374,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 43.2371,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.9252,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.3629,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 180.6758,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.2913,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 54.3609,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.2922,
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
        "date": 1647535683052,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 24.4257,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 25.6652,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 29.0097,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 38.8403,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 46.4826,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 48.3499,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 29.1851,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 19.4525,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 183.8893,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 29.4539,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 60.5888,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 52.4279,
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
        "date": 1647540709067,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.1379,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.1254,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.8625,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.5695,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 41.8267,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 43.1321,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 26.0502,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.4557,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 164.9044,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.2996,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 54.5026,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.1789,
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
        "date": 1647543169460,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 23.0687,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 25.7605,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 28.849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 38.6192,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 47.6854,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 47.0384,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 28.319,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 18.9804,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 188.1661,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 30.5655,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 64.7648,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 50.5964,
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
        "date": 1647893928315,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.2098,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.114,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.8562,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.5521,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 41.9327,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 43.2081,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.9788,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.4347,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 181.402,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.2948,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 54.4131,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.2931,
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
        "date": 1648065987017,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.2385,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.4241,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.8783,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.6781,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 41.9679,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 43.1583,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 26.023,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.3092,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 178.1536,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.4853,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 54.3842,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.3775,
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
        "date": 1648080001478,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.1312,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.0801,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.8372,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.5963,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 41.8341,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 43.1554,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.9832,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.3739,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 180.0655,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.495,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 54.3217,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.5213,
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
        "date": 1648081779395,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 24.7233,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 26.2175,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 30.0396,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 41.032,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 50.0112,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 51.5046,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 31.0054,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 20.8174,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 198.7902,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 31.6623,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 64.6679,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 55.2928,
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
        "date": 1648158045184,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.1358,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.3211,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.8629,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.685,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 42.0275,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 43.2388,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 26.0152,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.3362,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 182.126,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.2979,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 54.432,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.1269,
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
        "date": 1648657706009,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.2462,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.1495,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.8413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.0059,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 40.9639,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 40.7277,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.1343,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.4878,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 179.1023,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 28.1066,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 56.6411,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.2839,
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
        "date": 1648667131121,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 22.2577,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 24.6235,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 28.5084,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 36.9086,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 44.7978,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 44.6381,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 27.6804,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 18.5727,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 192.0404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 30.4071,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 61.7754,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 49.3631,
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
        "date": 1648674509532,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 24.9296,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 26.0805,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 30.4172,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 40.9619,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 49.8383,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 50.4487,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 30.3746,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 20.6167,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 194.4332,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 31.0832,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 63.669,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 54.7003,
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
        "date": 1648699964597,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 23.226,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 25.4011,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 28.7353,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 38.2702,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 47.0962,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 46.2394,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 28.1713,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 18.7859,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 181.1556,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 30.2131,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 64.8063,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 49.9072,
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
        "date": 1648751988793,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 23.5223,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 26.0497,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 29.7704,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 38.6655,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 48.029,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 48.4011,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 28.9331,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 19.4889,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 192.3745,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 31.1991,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 66.8566,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 56.7222,
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
        "date": 1648771041571,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.2964,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.1946,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.8948,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.5941,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 42.0349,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 43.3041,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 26.2052,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.4588,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 181.7292,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.3294,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 55.0717,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.3235,
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
        "date": 1649089636607,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 22.979,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 24.3045,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 29.6273,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 37.1745,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 46.3105,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 44.579,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 28.1334,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 18.6594,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 204.5981,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 32.0899,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 65.0795,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 52.0753,
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
        "date": 1649275739098,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.0643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.2819,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.8838,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.6002,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 42.1537,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 44.1782,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 29.1006,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.4216,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 175.1889,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.4101,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 55.017,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.3944,
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
        "date": 1649714046900,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 20.8962,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.7956,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 26.175,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.6072,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 41.5937,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 42.053,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.2949,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.0652,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 170.942,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 27.2901,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 59.2102,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 45.0125,
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
        "date": 1649972276431,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.0978,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 22.2536,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.892,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 34.5557,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 41.7382,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 43.2592,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 26.015,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 17.4665,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 178.0089,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 26.3033,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 54.4594,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 46.2485,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}