window.BENCHMARK_DATA = {
  "lastUpdate": 1646797714918,
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
        "date": 1646687702435,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 18.0983,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 19.8199,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 22.4196,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 31.3212,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 38.8804,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 40.1538,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 21.3551,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 14.1727,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 147.1651,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 23.8278,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 51.1942,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 42.6701,
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
        "date": 1646781994728,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.3967,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 23.2595,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 26.3875,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 36.6133,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 44.8454,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 46.4988,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.0631,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.5797,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 162.1788,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 27.9698,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 60.6919,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 50.9244,
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
        "date": 1646782871834,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 18.8373,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 20.8056,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 25.0487,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 32.4746,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 39.7359,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 40.7348,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 24.0404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 15.2465,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 140.7144,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 25.8815,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 54.8096,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 45.5926,
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
        "date": 1646783512295,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 18.2747,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 20.0158,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 22.4605,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 31.2919,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 39.255,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 40.9006,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 21.5914,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 14.3657,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 146.5311,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 24.0637,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 51.6409,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 42.9587,
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
        "date": 1646785072511,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 18.0323,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 19.0429,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 22.6117,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 30.2948,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 39.4773,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 37.8604,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 21.879,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 14.0519,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 132.6253,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 24.6381,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 49.7778,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 44.9023,
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
        "date": 1646797712101,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 21.3081,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 23.8743,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 27.4725,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 36.8909,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 44.5582,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 45.0704,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 25.7636,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 16.593,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 154.3331,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 27.3284,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 59.4337,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 52.4416,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}