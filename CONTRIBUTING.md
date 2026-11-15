# Contributing

Please be sure to read our [official OneConfig contributing documentation](https://contributing.polyfrost.org/oneconfig), as the following inline docs will be removed soon.

We recommend the use of an IDE, specifically [IntelliJ IDEA](https://www.jetbrains.com/idea/). This guide will be based on IntelliJ and might be slightly different for other IDE's.

### Prerequisites
- A Java 17 or up JDK.
- A Java 8 JDK.

### Setting up your IDE

#### IntelliJ IDEA:
Your Project JDK should be set to Java 8, to do this in IntelliJ go to `File > Project Structure` and set `SDK` to your Java 8 JDK.

Your Gradle JDK should be set to Java 17 or up, to do this in Intellij go to `File > Settings > Build, Execution, Deployment > Build Tools > Gradle` and set `Gradle JVM` to a Java 17 JDK.

You might have to reload your Gradle project, to do this click Gradle in the top right and then click the reload button.

Now in the Gradle tab under the `loom` you should run the `setupGradle` task and then under the `ide` tab you should run the `genIntellijRuns` task.
This will allow you to run Minecraft with your ide and use the debugger. 
To login in to your Minecraft account while in your development enviroment, you can use the `microsoftLogin` Gradle task. This will securely save your Minecraft access token at a user level.
A stale token that cannot be refreshed will fall back to offline launch, but you can log out explicitly using the `microsoftLogout` task.

# Pull Requests
To contribute to OneConfig, please open a pull request. Here are the basic guidelines.

Guidelines for pull requests:
- **Clear description**: Make sure you document **what** was changed and **why** the change was needed.
- **One pull request per feature**: Please open an individual pull request for each new feature or for each bug fix, this makes it easier for us to keep track of everything.
- **Update documentation**: Please update the documentation of features your pull request effects, and if you don't have access to this docmentation please state it in your pull request description.
- **Backwards compatibility**: Please keep in mind that OneConfig **has** to be backwards compatible, if you change anything about the public API that can break mods that use OneConfig, your pull request will be instantly denied.

## Commit messages

Commits follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>(<scope>): <description>
```

- `type` is one of `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `style`, `build`, `ci`, `chore` or `revert`.
- `scope` is optional. Use the module or area the change touches, or `deps` for dependency bumps.
- Write the description in the imperative, starting lowercase, with no full stop at the end.
- Mark a breaking change with `!` before the colon.
- Keep the subject under about 72 characters. Only add a body to reference an issue, such as `Fixes #12`.
- Put one change in each commit. Unrelated fixes go in separate commits.

Examples:

```
fix: keep the menu open after resizing the window
chore(deps): bump Gradle to 9.2
feat!: rename the config file
```
