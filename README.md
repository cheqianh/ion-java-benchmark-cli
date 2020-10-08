## ion-java Benchmarking CLI

This tool allows users to...
* Determine which ion-java configurations perform best
* Compare [ion-java](https://github.com/amzn/ion-java/) to Java implementations of other
serialization formats (not yet implemented)

...for the individual users' data and access patterns.

Additionally, allows ion-java developers to...
* Determine the impact of a proposed change
* Decide where investments should be made in improving performance

...by generating results from a variety of real-world data and access patterns.

The tool uses the [JMH](https://openjdk.java.net/projects/code-tools/jmh/) microbenchmarking
library and produces results in formats generated by JMH.

The following stats will be included in benchmark results:

* Speed (or throughput)
* Heap usage
* Garbage collection statistics (number of GCs, total time taken, average size of various GC
generations)
* Size of the data (the input data for read benchmarks and the output data for write benchmarks)

## Installation

Building the tool generates a self-contained executable `jar`.

To build the tool with the latest version of the ion-java library, simply run

```
mvn clean install
```
from the directory containing `pom.xml`. When the build completes, a `jar` named
`ion-java-benchmark-cli-<version>-jar-with-dependencies.jar` will be present in the `target/`
subdirectory.

This `jar` may be copied to any location and may be executed using `java -jar <path-to-jar>`.
Users may find it convenient to create an alias to `java -jar <path-to-jar>` named, e.g.,
`ion-java-benchmark` to simplify invocation of the tool.

Note: the Maven build will look for the latest ion-java version available in the local Maven
repository. Users may use the tool with a custom or in-development ion-java version by installing
the desired version into the local Maven repository. To determine which ion-java version the tool
is using, use the `--version` command. After switching ion-java versions, the executable `jar`
must be rebuilt.

## Examples

(Note: these examples assume the invocation of the tool has been aliased to `ion-java-benchmark`.)

Benchmark a full-traversal read of `example.10n` from file using the IonReader API, with 10 warmups,
10 iterations, and 1 fork, printing the results to stdout in JMH’s standard text format.

```
ion-java-benchmark read example.10n
```

Benchmark a fully-buffered write of binary Ion data equivalent to `example.10n` to file using the
IonWriter API, with 10 warmups, 10 iterations, and 1 fork, printing the results to stdout in JMH’s
standard text format.

```
ion-java-benchmark write example.10n
```

Benchmark a write of binary Ion data equivalent to the first 1,000 top-level values in `example.10n`
to in-memory bytes using the IonWriter API, flushing after every 100 top-level values. Produce
results for both 0-byte length preallocation and 2-byte length preallocation to facilitate
comparison of both settings.

```
ion-java-benchmark write --io-type buffer \
                         --limit 1000 \
                         --ion-flush-period 100 \
                         --ion-length-preallocation 0 \
                         --ion-length-preallocation 2 \
                         example.10n
```

Profile a sparse read of `example.10n` from file, materializing only the values that match the paths
specified in paths.ion, using [ion-java-path-extraction](https://github.com/amzn/ion-java-path-extraction/).
This process will repetitively execute until manually terminated, allowing the user to attach a tool
for gathering performance profiles.

```
ion-java-benchmark read --profile --paths paths.ion example.10n
```

Benchmark a fully-buffered write of binary Ion data equivalent to example.10n both with and without
using shared symbol tables. The file tables.ion contains a sequence of Ion symbol tables.

```
ion-java-benchmark write --ion-imports-for-benchmark tables.ion \
                         --ion-imports-for-benchmark none \
                         example.10n
```

Benchmark a full-traversal read of data equivalent to exampleWithImports.10n, which declares the shared
symbol table imports provided by inputTables.ion, re-encoded (if necessary) using the shared symbol
tables provided by benchmarkTables.ion, inputTables.ion, and no shared symbol tables. Produce
results from using both the DOM and IonReader APIs.\n" +

```
ion-java-benchmark read --ion-imports-for-input inputTables.ion \
                        --ion-imports-for-benchmark benchmarkTables.ion \
                        --ion-imports-for-benchmark auto \
                        --ion-imports-for-benchmark none \
                        --ion-api dom \
                        --ion-api streaming \
                        exampleWithImports.10n
```

## Tips

As the JMH output warns: "Do not assume the numbers tell you what you want them to tell." Benchmarking
on the JVM is hard. There is non-deterministic behavior that can lead to high variance between
iterations. Be suspicious of benchmark results with a reported Error that is a high percentage of the
Score. Aim for an Error percentage of less than 10%.

To reduce Error, try increasing the number of warmup iterations, timed iterations, and forks. To
ensure the JVM is properly warmed up, benchmarks should include enough warmup iterations to allow for
the scores to stabilize. This often takes at least 20 seconds. Benchmarks should be run on idle systems.
Background processes competing for resources can lead to higher variance, especially for benchmarks
with a short execution time per invocation.

The default benchmark mode is SingleShotTime, meaning that the reported score is the result of a
single invocation of the benchmark method. This works well for medium and large input data that takes
on the order of seconds per invocation, but leads to higher variance for input data that takes only
milliseconds or microseconds per invocation. Such data should be used with one of the other modes,
each of which generates an iteration score by averaging the score of multiple invocations of the
benchmark method. For very small data, it may also be necessary to change the reported time unit to
provide enough granularity to observe differences between trials.

Both the `read` and `write` benchmark commands involve a setup phase that occurs before the benchmark
begins. However, due to a quirk in the JMH implementation, this phase occurs after JMH prints
`Warmup Iteration 1:` to the output. This can make it seem like the first warmup iteration takes an
excessive amount of time or is deadlocked, but give it a chance to complete. Once the scores stabilize,
if the iterations take longer than you're willing to wait, consider using the `--limit` option to
limit the amount of data processed by the benchmark. For write benchmarks, using `--limit` may be
necessary depending on the size of the input data and the memory constraints of the system, as
the setup phase involves generating write instruction lambdas and storing them in memory.

## For developers

### Adding an option

Adding an option involves the following steps:
1. In `Main`, add the option to the `USAGE` and `OPTIONS` strings, mimicking the existing format.
2. If the option applies to both the `read` and `write` commands, add parameterization logic to the
`OptionsMatrixBase` constructor using the existing helper methods. If the option applies only to
the `read` or the `write` command, add this logic to the `ReadOptionsMatrix` or `WriteOptionsMatrix`
constructor, respectively. These classes are responsible for generating the complete set of
combinations for the chosen set of option values, which corresponds to the complete set of benchmark
trials to be run.
3. If the option applies to both the `read` and `write` commands, add parsing logic to the
`OptionsCombinationBase` constructor using the existing helper methods. If the option applies only
to the `read` or the `write` command, add this logic to the `ReadOptionsCombination` or
`WriteOptionsCombination` constructor, respectively. These classes are responsible for representing
a single combination of option values, which corresponds to a single benchmark trial.
4. Determine where to place the logic that uses the new option. If the option applies to all formats
or affects how a resource is constructed or configured, it may make sense to add a factory method
to `OptionsCombinationBase`, `ReadOptionsCombination`, or `WriteOptionsCombination` and invoke this
method in any `MeasurableTask` implementations to which the option applies. As an example, see
`OptionsCombinationBase.newInputStream`. If the option applies to all formats but only the `write`
or `read` command, then using the option within `MeasurableReadTask` or `MeasurableWriteTask` may
make sense. As an example, see `MeasurableWriteTask.getTask`. If the option applies to multiple
formats, but not all formats, it may make sense to add a utility function or use inheritance. As
an example, see `IonUtilities`, which is used by `IonMeasurableReadTask` and `IonMeasurableWriteTask`.
If the option is limited to use with a particular format and command, then its logic may belong
in the concrete `MeasurableTask` implementation for that format/command combination. As an example,
the `--ion-reader` option only applies to the `read` command when used with either Ion text or binary,
so the logic that uses the option is contained within `IonMeasurableReadTask`.
5. In `OptionsTest`, add tests that exercise all values for this option (if enumerated) or a variety
of values, for all commands to which it applies.

### Adding a format

Adding support for an additional serialization format involves the following steps.
1. Identify the Java library (or libraries) that provides the reader/writer implementation for that
format in Java. Add an open-ended dependency on that library to `pom.xml`.
2. Add a value to the `Format` enum to represent the new format.
3. In `Main`'s `OPTIONS` string, edit the entry for the `--format` command to allow for the new format.
4. Using `IonMeasurableReadTask` and `IonMeasurableWriteTask` as examples, create concrete
implementations of `MeasurableReadTask` and `MeasurableWriteTask` for the new format.
5. Implement the inherited abstract methods in the new `Format` enum value. This involves adding
logic to convert between formats and to instantiate the `MeasurableTask` classes created in the
previous step. The `convert` implementations for the existing `Format` values will need to be updated
as well to support conversions from the new format. Add logic to `Format.classify` to determine whether
a file contains data in the new format.
6. Follow the steps from the `Adding an option` section above to add any format- or library-specific
options to the CLI.
7. In `OptionsTest`, add tests that thoroughly exercise the new format. Add data in the new format
into the test directory for use in tests.
8. Build the tool using `mvn clean install`. Using various samples of data in the new format, run
the tool by hand to make sure everything looks correct and the benchmark results look reasonable.
9. Add at least one example of using the new format to the `EXAMPLES` string in `Main`. Copy this
example into the `Examples` section of this README.

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.

