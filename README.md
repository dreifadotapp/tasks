# The "Tasks" framework

[![Circle CI](https://circleci.com/gh/dreifadotapp/tasks.svg?style=shield)](https://circleci.com/gh/dreifadotapp/tasks)
[![Licence Status](https://img.shields.io/github/license/dreifadotapp/tasks)](https://github.com/dreifadotapp/tasks/blob/master/licence.txt)

## What it does

The "Tasks" framework simply provides a common way of encapsulating calls into the plethora of tools and APIs that we
typically need to build out the backends to deploy and manage complex applications. The original use case is
encapsulating the many dev ops tools needed to deploy and manage the DLT and its related components.

This framework is most certainly **NOT** intended as replacement for existing devops tooling. In these cases each task
should ideally be a simple wrapper over the underlying toolset. The key insight is that all tasks expose a similar API
and can be thought of as Lego style building block.

This library also implements a standard framework (the LoggingContext) to manage [logging](docs/logging.md)
. Its likely this will be broken out into its own library in the future.

There are also higher level services that build on these tasks, for example the
task [Http Remoting](https://github.com/dreifadotapp/tasks-http#readme)
toolkit lets clients call tasks on remote servers using http(s) standards for data transfer and security.

The design and modularity is heavily influenced by [http4K](https://www.http4k.org/guide/concepts/rationale/). In
particular:

* Minimal Dependencies (see below)
* Very modular
* Avoid "magic": No annotations, very minimal use of reflections
* Fast startup/ shutdown

## Dependencies

As with everything in [Dreifa dot App](https://dreifa.app), this library has minimal dependencies:

* Kotlin 1.5
* Java 11
* The object [Registry](https://github.com/dreifadotapp/registry#readme)
* The [Commons](https://github.com/dreifadotadotapp/commons#readme) module
* The [Really Simple Serialisation(rss)](https://github.com/dreifadotapp/really-simple-serialisation#readme) module
    - [Jackson](https://github.com/FasterXML/jackson) for JSON serialisation
* The [Simple Event Store(ses)](https://github.com/dreifadotapp/simple-event-store#readme) module
* The [Simple KV Store(sks)](https://github.com/dreifadotapp/simple-kv-store#readme) module
* The [Open Telemetry](https://github.com/dreifadotapp/open-telemetry#readme) module

## Adding as a dependency
Maven jars are deployed using JitPack. See releases for version details.

```groovy
//add jitpack repo
maven { url "https://jitpack.io" }

// add dependency
implementation "com.github.dreifadotapp:tasks:<release>"
```

JitPack build status is at https://jitpack.io/com/github/dreifadotapp/tasks/$releaseTag/build.log

## Testing with Open Telemetry

Open Telemetry support is being added and certain test cases will produce telemetry. The telemetry will be captured
by Zipkin if running locally. The easiest way to run Zipkin is via Docker.

```bash
docker run --rm -it --name zipkin \
  -p 9411:9411 -d \
  openzipkin/zipkin:latest
```

Then open the [Zipkin UI](http://localhost:9411/zipkin/).

Each test run is tagged with a unique booking ref style test id. To filter on a specific
id edit open [this link](http://localhost:9411/zipkin/?annotationQuery=dreifa.correlation.testId%3Datestid) and
edit `atestid`

## Next Steps

* more on building and using Tasks is [here](./docs/tasks.md)
* details of the Logging Context are [here](./docs/logging.md)
* design notes on the "task worker" concept are [here](./docs/task-worker.md)
