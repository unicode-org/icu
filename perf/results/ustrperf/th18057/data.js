window.BENCHMARK_DATA = {
  "lastUpdate": 1646687850888,
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
        "date": 1646687848515,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestCtor",
            "value": 23.7806,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor1",
            "value": 26.1398,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor2",
            "value": 29.3143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCtor3",
            "value": 39.1429,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign",
            "value": 47.6122,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign1",
            "value": 46.0102,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestAssign2",
            "value": 29.9263,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestGetch",
            "value": 19.6141,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestCatenate",
            "value": 140.9586,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan",
            "value": 30.5255,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan1",
            "value": 62.6497,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestScan2",
            "value": 49.6374,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}