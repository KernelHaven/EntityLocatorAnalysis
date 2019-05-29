# EntityLocatorAnalysis

![Build Status](https://jenkins-2.sse.uni-hildesheim.de/buildStatus/icon?job=KH_EntityLocatorAnalysis)

An analysis plugin for [KernelHaven](https://github.com/KernelHaven/KernelHaven).

Analysis components for locating different entities.

## Capabilities

Currently supported locators:
* Detection of variability variables in an external mailing list (e.g. the Linux Kernel Mailing List).

## Usage

Place [`EntityLocatorAnalysis.jar`](https://jenkins-2.sse.uni-hildesheim.de/job/KH_EntityLocatorAnalysis/lastSuccessfulBuild/artifact/build/jar/EntityLocatorAnalysis.jar) in the plugins folder of KernelHaven.

## Dependencies

This plugin has no additional dependencies other than KernelHaven.

Some analysis components require a Git installation, with the `git` executable in the path.

## License

This plugin is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).
