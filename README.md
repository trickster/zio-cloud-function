# Cloud Functions with Scala in ZIO 

## Usage

Cloud function deployment

```
cloudFunctionDeploy
```

## Perf results

JVM language comparison (there is no performance difference between native Java code and Scala with ZIO)

| Language      | Average Time  |
| ------------- |:-------------:|
| Java          | 0.0256 secs   |
| Scala         | 0.0221 secs   |
| Scala (lazy val) | 0.0280 secs |
| Scala (ZIO)   | 0.0167 secs   |
| Scala (ZIO, Spanner) | 0.0215 secs - 0.0386 secs |
