
[![Build Status](https://travis-ci.org/prashant-ramcharan/courgette-jvm.svg?branch=master)](https://travis-ci.org/prashant-ramcharan/courgette-jvm)
[ ![Download](https://api.bintray.com/packages/prashantr/Courgette-JVM/courgette-jvm/images/download.svg) ](https://bintray.com/prashantr/Courgette-JVM/courgette-jvm/_latestVersion)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# Courgette-JVM #

Courgette-JVM is an extension of Cucumber-JVM with added capabilities to **run cucumber tests in parallel on a feature level or on a scenario level**. It also provides an option to **automatically re-run failed scenarios**.

## Key Features
- **All features** can be executed in parallel on independent threads.
- **All scenarios** can be executed in parallel on independent threads.
- **Automatic re-run** of failed scenarios.
- **Requires only 1 annotated class** to run all feature files in parallel.
- **Single report generation** for all executed features including embedded files (Json and Html reports)
- **Single re-run file** listing all failed scenarios that occured during parallel execution.
- Supports **Cucumber-JVM 2+**
- Supports **JUnit** and **TestNG**
- Can be used with **Gradle** and **Maven**.
- Searchable and paginated **Courgette-JVM Html Report** which includes all step definitions, embedded screenshots, thrown exceptions, pie chart and Courgette run information.
[![Courgette-_JVM_2.0_Report.png](https://s5.postimg.cc/bk2gdnb7r/Courgette-_JVM_2.0_Report.png)](https://postimg.cc/image/g5yklzwqr/)

## Requirements
- Java 8
- Java 9 and 10 (_supported from version 2.3.1_)

## Installation

#### Repository: [jcenter](https://bintray.com/bintray/jcenter?filterByPkgName=courgette-jvm)

<a href='https://bintray.com/prashantr/Courgette-JVM/courgette-jvm?source=watch' alt='Get automatic notifications about new "courgette-jvm" versions'><img src='https://www.bintray.com/docs/images/bintray_badge_color.png'></a>

#### Maven
````xml
<repositories>
    <repository>
      <id>jcenter</id>
      <url>https://jcenter.bintray.com/</url>
    </repository>
</repositories>

<dependency>
  <groupId>io.github.prashant-ramcharan</groupId>
  <artifactId>courgette-jvm</artifactId>
  <version>2.3.2</version>
</dependency>
````

#### Gradle
````gradle
repositories {
    jcenter()
}

compile 'io.github.prashant-ramcharan:courgette-jvm:2.3.2'
````

#### Included Dependencies
* cucumber-core 2.4.0
* cucumber-java 2.4.0
* cucumber-java8 2.4.0
* cucumber-junit 2.4.0
* testng 6.14.3
* jackson-databind 2.8.8


## Usage

Example project: [Courgette-JVM Example](https://github.com/prashant-ramcharan/courgette-jvm-example)

Courgette-JVM supports JUnit and TestNG to run cucumber features and scenarios in parallel. A JUnit runner class must be annotated with **@RunWith(Courgette.class)** and a TestNG runner class must extend **TestNGCourgette**.

* **threads** : The number of concurrent threads to run cucumber features. 

    * _Example: If you have 10 cucumber features and you use 6 threads, 6 features would first run in parallel then the following 4 features would run in parallel_.

* **runLevel** : Options are CourgetteRunLevel.FEATURE or CourgetteRunLevel.SCENARIO

    * _If set to feature level, all features would run in parallel. If set to scenario level, all scenarios would be run in parallel._
    
* **rerunFailedScenarios** : If set to true, any failed scenario will be immediately re-run in the same thread. If the re-run succeeds, the initial failure will be ignored and not cause the build to fail.

* **showTestOutput** : If set to true, the output for each feature will be redirected to the current I/O source or destination.

* **reportTargetDir** : Target directory where courgette-report is generated. Set to target by default.
    
* **cucumberOptions** : The standard cucumber options for specifying feature paths, glue, tags etc..

### Additional

* At the end of the test run, a **single report** ( _if included in the cucumberOptions_ ) listing all executed features and scenarios will be created in the specified report path. All embedded images will be placed in the images folder in the specified report path.

* A **courgette-rerun.txt** file listing all failed scenarios will be created in the specified rerun plugin path or the target folder ( _default_ )

* A Courgette-JVM Html report will be created in the reportTargetDir (_defaulted to the target directory_).

##### JUnit Runner

````java
@RunWith(Courgette.class)
@CourgetteOptions(
        threads = 10,
        runLevel = CourgetteRunLevel.SCENARIO,
        rerunFailedScenarios = true,
        showTestOutput = true,
        reportTargetDir = "build",
        cucumberOptions = @CucumberOptions(
                features = "src/test/resources/features",
                glue = "steps",
                tags = {"@regression", "not @wip"},
                plugin = {
                        "pretty",
                        "json:build/cucumber-report/cucumber.json",
                        "html:build/cucumber-report/cucumber.html"},
                strict = true
        ))
public class RegressionTestSuite {
}
````


##### TestNG Runner

````java
@Test
@CourgetteOptions(
        threads = 10,
        runLevel = CourgetteRunLevel.SCENARIO,
        rerunFailedScenarios = true,
        showTestOutput = true,
        reportTargetDir = "build",
        cucumberOptions = @CucumberOptions(
                features = "src/test/resources/features",
                glue = "steps",
                tags = {"@regression", "not @wip"},
                plugin = {
                        "pretty",
                        "json:build/cucumber-report/cucumber.json",
                        "html:build/cucumber-report/cucumber.html"},
                strict = true
        ))
public class RegressionTestSuite extends TestNGCourgette {
}
````


## Gradle Build Task

````gradle
tasks.withType(Test) {
    systemProperties = System.getProperties()
}

// JUnit
task regressionSuite(type: Test) {
    include '**/RegressionTestSuite.class'

    outputs.upToDateWhen { false }
}

// TestNG
task regressionSuite(type: Test) {
    useTestNG()

    include '**/RegressionTestSuite.class'

    outputs.upToDateWhen { false }
}
````

## Gradle Run Options

To override the hard-coded courgette options (_threads, runLevel, rerunFailedScenarios, showTestOutput, reportTargetDir_) set in the runner class, you can provide system properties to the gradle task.

````gradle

gradle regressionSuite -Dcourgette.threads=2 -Dcourgette.runLevel=FEATURE -Dcourgette.rerunFailedScenarios=false -Dcourgette.showTestOutput=true -Dcourgette.reportTargetDir=build

````

To override the hard-coded cucumber options (_tags, glue, plugin, name, junit_) set in the runner class, you can provide comma separated system properties to the gradle task.

````gradle

gradle regressionSuite -Dcucumber.tags="@regression, ~@bug" -Dcucumber.glue="steps, hooks"

````

To specify non standard VM options (_-X options_)

````gradle

gradle regressionSuite -Dcourgette.vmoptions="-Xms256m -Xmx512m"

````

## Limitations and Known Issues

* JUnit test notifier is not updated when running features in the IDE during parallel test execution.
   
   * _Each feature is run using the Cucumber CLI and because of this JUnit is not notified off the test result. The workaround to this is the Courgette-JVM html report which lists all test passes, failures and re-runs. Alternatively, you can review the Cucumber reports or the results from the build tool_.
   
  
* When there's a failure in the feature and your runner is set to 'runLevel = CourgetteRunLevel.FEATURE' and 'rerunFailedScenarios = true', the re-run cucumber feature report will override the original cucumber feature report.
   
   * _Use CourgetteRunLevel.SCENARIO which resolves this issue and retains all results in the cucumber report._


## Submitting Issues
For any issues or requests, please submit [here](https://github.com/prashant-ramcharan/courgette-jvm/issues/new)
