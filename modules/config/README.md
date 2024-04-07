# OneConfig Config
`dependsOn("utils")`

This module contains the main attraction - the configuration system used in OneConfig.
It is based on a simple tree based structure where each configuration option is a node in the tree.

Configs support arbitrary data types for values, callbacks, can contain metadata, and conditions for displaying them.

> To see the default implementation of the config system, see the `config-impl` module - which utilizes files for storage, and uses `PolyUI` to show the trees to the end user.

As with the rest of OneConfig, one of the main features of this is the decoupling from internal structure and the creation
of the config - so the config system is split into three components:
- `api.config.collect` - the collection of config options from arbitrary objects
- `api.config.backend` - the storage and loading of configs from any source, for example files, databases, etc.
- `api.config.serialize` - the serialization of objects into simple data types using an automated `ObjectSerializer` and developer-defined `adapters`

The process of creating trees revolves around creating trees and merging them into one: where the collector will produce a tree, 
and data from the backend that was stored previously will create a tree, and these trees will be merged
into one another, producing the final complete tree that can be displayed to the user - with any changes
made being reflected in the backend and on the object that the config was created from at the same time.

This also has a powerful side effect - we can take otherwise incomplete trees (say, configs from a mod that does not use oneconfig directly) and we can 
add data to them, for example, from a GitHub repo, which can contain metadata such as descriptions, titles and display strategies for the options - allowing us to 
display otherwise useless data to the user.
