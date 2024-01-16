# Performance Testing Vaadin Apps with Playwright

A prototype project to test out the possibility to performance-test Vaadin apps
using [Playwright](https://playwright.dev/). Written in Java, requires Java 17 or higher.

This project contains a very simple Vaadin app and a JUnit test case which runs the
performance test. When the test is run, 10 invisible browsers are started, and they will starts bombarding
the Vaadin app with requests. Response time is remembered and printed at the end of the test.

> WARNING! Each browser requires at least 256 MB of RAM. Launching 100 browsers will
> consume 25 GB of memory; if your machine is not powerful enough it will make the entire OS
> unresponsive and you WILL need to hard-reset your machine.

Obviously running 100 browsers at the same time on one machine doesn't scale well -
you won't be able to run, say, 1000 browsers unless you have a tremendously powerful machine.
Yet writing the performance tests this way is very simple, and even with 100 browsers you will
be able to measure your app's performance.

If using this approach for actual performance testing, make sure the Vaadin app is running on
another machine. Otherwise, the Vaadin app will compete for CPU and memory with the browsers,
and you may experience degraded the performance of the Vaadin app.

## Running

Before running the test you need to start the app itself. This is very easy, simply
open the `Main.java` and run its `main()` method. Please see the [Vaadin Boot](https://github.com/mvysny/vaadin-boot#preparing-environment) documentation
for further info on how you run Vaadin-Boot-based apps. Alternatively,
run `./gradlew run` from your command line.

After the app is up-and-running, please run the performance test itself:

* Via your IDE, by running the `PerformanceIT` java class as a test suite, OR
* From command-line via Gradle: `./gradlew integrationTest --info`

## Increasing number of browsers

The default number of concurrent browsers is 10. Each browser consumes around 256 MB of RAM;
the test therefore needs 2,5 GB of RAM to run. Make sure you have at least that amount of RAM
available in your machine otherwise your OS will crash.

Each browser also requires roughly 0,25 CPU core to run (depending on the tests). Make sure you have enough CPU cores,
otherwise the browsers will start choking each other and you won't measure the performance
of your server app but rather the ability of your OS to fail to run the browsers concurrently.

A good rule of thumb to run the test on 100 browsers is to have a machine with 16 cores and 32 GB of RAM.
Also prefer Linux or MacOS over Windows since Windows threading is known to suck. You can try running the tests,
but your OS UI will become unresponsive.
