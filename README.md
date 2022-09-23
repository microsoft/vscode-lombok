# <img src="https://raw.githubusercontent.com/microsoft/vscode-lombok/master/images/icon.png" alt="Lombok logo" width="48" height="48">   vscode-lombok
![VS Marketplace](https://vsmarketplacebadge.apphb.com/version-short/vscjava.vscode-lombok.svg)
![Installs](https://vsmarketplacebadge.apphb.com/installs-short/vscjava.vscode-lombok.svg)

⚠️Starting from 1.8.0, the [Language Support for Java(TM) by RedHat](https://marketplace.visualstudio.com/items?itemName=redhat.java) extension has built-in support for Lombok and automatically uses the lombok.jar from your project classpath. The embedded lombok.jar in the vscode-lombok extension will be deprecated in favor of RedHat Java extension.

## Overview

https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-lombok

A lightweight extension based on [Language Support for Java by Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.java). Provide refactoring code actions to [Lombok](https://projectlombok.org/)/Delombok annotations in your code.
- Lombok - Refactor code with Lombok annotations.
- Delombok - Remove annotations with actual methods.

![Screenshot](https://raw.githubusercontent.com/microsoft/vscode-lombok/main/images/vscode-lombok.gif)

## Requirements
- VS Code (version 1.65.0 or later)
- Lombok added as a dependency in your Java Project (Make sure you're using the latest version to avoid issues!) [Add with Maven](https://projectlombok.org/setup/maven) or  [Add with Gradle](https://projectlombok.org/setup/gradle)

## Install

Open VS Code and press `Ctrl + Shift + X` to open extension manager. Type `lombok` and click install. Reload VS Code when asked.

## Features / Supports
Here are the supported annotations for lombok/delombok code actions.
- [@Getter and @Setter](http://projectlombok.org/features/GetterSetter.html)
- [@ToString](http://projectlombok.org/features/ToString.html)
- [@EqualsAndHashCode](http://projectlombok.org/features/EqualsAndHashCode.html)
- [@AllArgsConstructor and @NoArgsConstructor](http://projectlombok.org/features/Constructor.html)
- [@Data](https://projectlombok.org/features/Data.html)

## Data/Telemetry

VS Code collects usage data and sends it to Microsoft to help improve our products and services. Read our [privacy statement](http://go.microsoft.com/fwlink/?LinkId=521839) to learn more. If you don’t wish to send usage data to Microsoft, you can set the `telemetry.enableTelemetry` setting to `false`. Learn more in our [FAQ](https://code.visualstudio.com/docs/supporting/faq#_how-to-disable-telemetry-reporting).

## Credits
This project was originally started by [@GabrielBB](https://github.com/GabrielBB) and is now currently maintained by Microsoft. Huge thanks to [@GabrielBB](https://github.com/GabrielBB) who started it all and made this Lombok extension available.

[I'm having issues](https://github.com/Microsoft/vscode-lombok/issues)
