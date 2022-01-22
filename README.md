# Reflex
![](https://img.shields.io/github/v/release/taboolib/reflex.svg)

```kotlin
repositories {
    maven { url = uri("https://repo.tabooproject.org/repository/releases/") }
    mavenCentral()
}

dependencies {
    // 本体
    implementation("org.tabooproject.reflex:analyser:{version}")
    implementation("org.tabooproject.reflex:fast-instance-getter:{version}")
    implementation("org.tabooproject.reflex:reflex:{version}") // 需要 analyser 模块
    // 本体依赖
    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-util:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")
    implementation(kotlin("stdlib"))
}
```

## analyser & reflex
基于 Kotlin 语言开发的反射工具，与 Java 原生或 `kotlin-reflect` 之间最大的区别在于能够**无视软兼容**反射目标类中的字段或方法。 

这种环境常见于 Bukkit 插件开发，举一个简单的例子：

```java
public class AnyPlugin extends JavaPlugin {
    
    private PlayerPointsAPI api; // PlayerPoints
    private AnyField target;
}
```

不少 Bukkit 开发者习惯将其他插件的接口缓存到主类，就算 `PlayerPoints` 没有安装也不会影响到类的运行和插件的启动，这样做到结果是无法通过反射获取这个类中的任何字段：

```java
Field field = Main.class.getDeclaredField("target"); // NoClassDefFoundError: PlayerPointsAPI
```

这种问题也体现在注册 Bukkit 监听器上，如果插件在低于 1.12 的版本运行时，下面的 `PlayerJoinEvent` 监听器将无法正常注册。

```java
public class AnyListener extends Listener {
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // 全版本
    }
    
    @EventHandler
    public void onSwap(PlayerSwapItemEvent event) {
        // 1.12 新增
    }
}
```

因此 Reflex 从 [TabooLib](https://github.com/taboolib/taboolib) 中独立，你可以：

+ 在 [AnalyserTestAsm.kt](https://github.com/TabooLib/Reflex/blob/master/analyser/src/test/kotlin/org/tabooproject/reflex/AnalyserTestAsm.kt) 中获取 `analyser` 的用法。
+ 在 [ReflexTest.kt](https://github.com/TabooLib/Reflex/blob/master/reflex/src/test/kotlin/org/tabooproject/reflex/ReflexTest.kt) 中获取 `relfex` 的用法。

## fast-instance-getter
基于 Java 且不依靠反射的高性能 Kotlin 单例/伴生类实例获取工具，同样解决了上面的问题。

```kotlin
val getter = FastInstGetter(ObjectTarget::class.java.name) // 初始化损耗较高，复用时需要手动缓存该实例
getter.instance // 获取单例实例
getter.companion // 获取伴生类实例
```