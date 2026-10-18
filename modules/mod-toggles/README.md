# OneConfig Mod Toggles

A single interface that lets a mod put its own on/off switch on its card in OneConfig, without depending on
OneConfig at runtime.

```kotlin
dependencies {
    compileOnly("org.polyfrost.oneconfig:mod-toggles:1.0.0")
}
```

```java
public class MyModToggle implements ModToggle {
    @Override
    public boolean isEnabled() {
        return MyConfig.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        MyConfig.enabled = enabled;
        MyConfig.save();
    }
}
```

```json
"entrypoints": {
  "oneconfig:toggle": ["com.example.MyModToggle"]
}
```

The class is only loaded when OneConfig asks for the entrypoint, so nothing breaks when OneConfig is not installed.
Keep it out of classes the mod loads on its own.
