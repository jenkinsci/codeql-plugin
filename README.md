# CodeQL Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/codeql-plugin/job/main/badge/icon)](https://ci.jenkins.io/job/Plugins/job/codeql-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/codeql-plugin.svg)](https://github.com/jenkinsci/codeql-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/codeql.svg)](https://plugins.jenkins.io/codeql)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/codeql-plugin.svg?label=changelog)](https://github.com/jenkinsci/codeql-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/codeql.svg?color=blue)](https://plugins.jenkins.io/codeql)

## Introduction

The CodeQL Plugin automatically installs and sets up the [CodeQL CLI](https://codeql.github.com/docs/codeql-cli/) on a Jenkins agent during a build.

## Functionality

During a build this plugin will:
* Install a specific version of the CodeQL CLI
* Add the following environment variables:
  * `PATH`: The CodeQL CLI home will be added to the `PATH` variable so that the tool will be available during build
  * `CODEQL_CLI_HOME`: A new environment variable will be added containing the home of the CODEQL_CLI

## Getting started

* In the Jenkins global tool configuration settings (Manage Jenkins → Global Tool Configuration), find the "CodeQL" section, click "CodeQL Installations…" and "Add CodeQL".
* Enter a name, e.g. "CodeQL 2.5.5": This will be the name entered in the Pipeline
* Select "Install automatically" and select the desired CodeQL version from the drop-down list

For pipelines, you can use the `tool` step or the `withCodeQL` step as seen below:

```
node {
    stage('Build') {
         withCodeQL(codeql: 'CodeQL 2.5.5') {
            sh 'codeql --version'
        }
    }
}

```

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

