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
Reflex 为基于 Kotlin 语言开发的反射工具，其与 Java 原生反射API及 `kotlin-reflect` 间最大区别在于其可**无视软兼容**反射目标类中的字段或方法。 

如下方代码所示，这种环境在 Bukkit 插件开发中较为常见：

```java
public class AnyPlugin extends JavaPlugin {
    
    private PlayerPointsAPI api; // PlayerPoints
    private AnyField target;
}
```

习惯于将其他插件内的接口缓存于主类的 Bukkit 开发者不在少数，因即便此时 `PlayerPoints` 并未安装，类的运行和插件的启动也不会受到影响。但此做法造成的结果是开发者无法通过反射获取该类中的任何字段：

```java
Field field = Main.class.getDeclaredField("target"); // NoClassDefFoundError: PlayerPointsAPI
```

同类问题也体现在 Bukkit 监听器的注册之上。若插件运行于 1.12 版本以下的环境时，下述 `PlayerJoinEvent` 监听器将无法正常注册。

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

因此， Reflex 从 [TabooLib](https://github.com/taboolib/taboolib) 中分离并发展为独立类库。通过此类库，下述操作将成为可能：

+ 在 [AnalyserTestAsm.kt](https://github.com/TabooLib/Reflex/blob/master/analyser/src/test/kotlin/org/tabooproject/reflex/AnalyserTestAsm.kt) 中获取 `analyser` 的用法。
+ 在 [ReflexTest.kt](https://github.com/TabooLib/Reflex/blob/master/reflex/src/test/kotlin/org/tabooproject/reflex/ReflexTest.kt) 中获取 `relfex` 的用法。

## fast-instance-getter
基于 Java 且不依赖反射的高性能 Kotlin 单例/伴生类实例获取工具，此工具同样解决了上述问题。

```kotlin
val getter = FastInstGetter(ObjectTarget::class.java.name) // 初始化损耗较高，复用时需手动缓存该实例
getter.instance // 获取单例实例
getter.companion // 获取伴生类实例
```
