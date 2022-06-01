# Contributing
We recommend the use of an IDE, specifically [IntelliJ IDEA](https://www.jetbrains.com/idea/). This guide will be based on IntelliJ and might be slightly different for other IDE's.

### Prerequisites
- A Java 17 or up JDK. We reccomend [Adoptium 17](https://adoptium.net/temurin/releases?version=17)
- A Java 8 JDK. We reccomend [Adoptium 8](https://adoptium.net/temurin/releases?version=8)

### Setting up your IDE

#### IntelliJ IDEA:
Your Project JDK should be set to Java 8, to do this in IntelliJ go to `File > Project Structure` and set `SDK` to your Java 8 JDK.

Your Gradle JDK should be set to Java 17 or up, to do this in Intellij go to `File > Settings > Build, Execution, Deployment > Build Tools > Gradle` and set `Gradle JVM` to a Java 17 JDK.

You might have to reload your Gradle project, to do this click Gradle in the top right and then click the reload button.

Now in the Gradle tab under the `loom` you should run the `setupGradle` task and then under the `ide` tab you should run the `genIntellijRuns` task.
This will allow you to run Minecraft with your ide and use the debugger. 
To login in to your Minecraft account while in your development enviroment, we recommend the use of [DevAuth](https://github.com/DJtheRedstoner/DevAuth)

# Pull Requests
To contribute to OneConfig, please open a pull request. We guide you through making a pull request using a template, but these are the basic guidelines.

Guidelines for pull requests:
- **Clear description**: Make sure you document **what** was changed and **why** the change was needed.
- **One pull request per feature**: Please open an individual pull request for each new feature or for each bug fix, this makes it easier for us to keep track of everything.
- **Update documentation**: Please update the documentation of features your pull request effects, and if you don't have access to this docmentation please state it in your pull request description.
- **Backwards compatibility**: Please keep in mind that OneConfig **has** to be backwards compatible, if you change anything about the public API that can break mods that use OneConfig, your pull request will be instantly denied.
