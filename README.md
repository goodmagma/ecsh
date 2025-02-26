# Ecsh

AWS ECS Shell helper.

This tool allow to do basic staff like list of ECS cluster of a profile and ssh to one of them.

## Getting started

To make it working, first of all you have to install AWS CLI for your operating system.

## Install AWS CLI

- [Install or update the latest version of the AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)

## Install Session Manager Plugin

- [Install the Session Manager plugin for the AWS CLI](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html)

## Configure AWS CLI

```
aws --version
aws configure
```

### Configure a second aws profile

```
aws configure --profile prod
```

## Compile

```
javac Ecsh.java
```

## Configure ECSH and profiles

```bash
ecsh --configure

AWS ECS Shell - v1.1.0

Loading configuration file C:\Users\Denis\.ecsh
Profile name[default]:
Cluster name: dev-flexymob
Saving Configuration file C:\Users\Denis\.ecsh
```

Ecsh allow you to define more profiles.
Default profile is the first loaded when you call `ecsh` without arguments.

If you want to define a different profile, you must choose a profile name during configuration (eg `prod`) and then run:

```bash
ecsh prod
```
