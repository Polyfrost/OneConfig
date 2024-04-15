# OneConfig Events

`dependsOn("utils")`

This module contains the event system used in OneConfig.
It utilizes simple `EventHandler<E>`s which are similar to `Consumer<T>` and take one parameter, `E event`. They can be added and removed on the fly, and can be cancelled.

By default, the system has 20+ events, and you can add your own - event dispatch and registration is handled
by the `EventManager` singleton object. As with all the other APIs provided by OneConfig, the internal representation
is abstracted away from the creation of them, and so, by default there are two ways to create event handlers:

- using the factory methods `EventHandler.of`, which have kotlin extensions for a prettier syntax
- using an annotation based system which will automatically register any applicable `@Subscribe`d event handling methods in a given object, and dispatch using MethodHandles.

The event system is designed to be as flexible as possible, and can be used in a variety of ways.