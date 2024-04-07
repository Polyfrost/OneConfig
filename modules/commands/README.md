# OneConfig Commands
`dependsOn("utils")`

This module contains the command system used in OneConfig. 
It is based around a simple tree based structure where each command is a node in the tree.

Commands support variable arity, arbitrary data types for parameters (see `ArgumentParser`), overloading, autocompletion, and more.

As with the rest of OneConfig, one of the main features of this is the decoupling from internal structure and the creation 
of the command - so there are multiple 'factories' that can create commands, by default being:
- `CommandBuilder` - a java style builder pattern, similar to brigaider
- `CommandDSL` - a kotlin style DSL
- `AnnotationCommandFactory` - a factory using annotated methods to create commands

The command system is designed to be as flexible as possible, and can be used in a variety of ways.
