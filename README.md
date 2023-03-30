# ZIO with Spanner

Some notes from translated from [cats-http4s](https://github.com/killaitis/http4s-cloud-functions)

## Usage


Run basic DB test

```
test:runMain HelloWorldSpec -tags DB
```

Cloud function deployment

```
cloudFunctionDeploy
```

## Perf results

JVM language comparison

| Language      | Average Time  |
| ------------- |:-------------:|
| Java          | 0.0256 secs   |
| Scala         | 0.0221 secs   |
| Scala (lazy val) | 0.0280 secs |
| Scala (ZIO)   | 0.0167 secs   |
| Scala (ZIO, Spanner) | 0.0215 secs - 0.0386 secs |

