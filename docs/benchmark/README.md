# 反射调用性能基准

本目录记录 `reflex` 反射调用路径的 JMH 基准结果，用于验证性能优化效果与回归监控。

## 目录内容

| 文件 | 说明 |
|---|---|
| `reflection-perf-affected.png` | 受优化影响的调用路径对比图（含提升倍数） |
| `reflection-perf-control.png` | 对照组，验证两次测量环境一致 |
| `reflection-perf-call-modes.png` | 调用方式互斥对照图（`invoke` vs `invokeWithArguments` vs `bindTo`） |
| `before.tsv` / `after.tsv` | 优化前后的原始 JMH 数据（ops/ms） |
| `call-modes.tsv` | 调用方式互斥对照的原始数据 |

## 环境

- JMH 1.37
- 运行参数：`-wi 5 -i 5 -f 1`（5 轮预热、5 轮测量、单 fork）
- 单位：ops/ms（每毫秒操作数，越高越好）

> 数据来自单机单次测量，绝对值仅供同机对比参考；跨机器比较无意义。

## 如何复现

基准模块位于仓库根目录的 `benchmark/`，不随正式产物发布。

```bash
# 全量运行（约 4 分钟）
./gradlew :benchmark:jmh

# 只看某个基准
./gradlew :benchmark:jmh -PjmhArgs="ReflexInvokeBench.reflexAsm"

# 更严谨的轮次（多 fork、更长测量，适合出结论）
./gradlew :benchmark:jmh -PjmhArgs="-wi 5 -i 5 -f 3"
```

## 覆盖的调用路径

| 基准 | 说明 |
|---|---|
| `direct` / `directNoArg` / `directStatic` / `directField` | 直接调用，JIT 基线 |
| `methodInvoke` / `methodInvokeNoArg` / `methodInvokeStatic` | 传统 `Method.invoke` |
| `fieldReflect` | 传统 `Field.get/set` |
| `reflexAsm` / `reflexAsmNoArg` / `reflexAsmStatic` | Reflex ASM 模式 |
| `reflexInstant` | Reflex REFLECTION_ONLY 模式 |
| `reflexFieldAsm` / `reflexFieldInstant` | Reflex 字段访问 |
| `mhExact` / `mhExactNoArg` | MethodHandle 精确签名（理论上限） |
| `mhGenericCached` | MethodHandle 泛化 + spreader（Reflex 内部实现形状） |
| `unsafeAccessField` | `UnsafeAccess.get/put` |
| `mhFieldInvoke` / `mhFieldInvokeWithArguments` | 同 handle 不同调用方式的互斥对照 |
| `mhFieldBindToEachCall` / `mhFieldBoundOnce` | `bindTo` 每次分配 vs 一次复用 |
| `mhFieldUnreflectEachCall` | 每次 unreflect（改造前行为） |

## 优化前后对比

### 受优化影响的路径

| 基准 | 优化前 (ops/ms) | 优化后 (ops/ms) | 提升 |
|---|---|---|---|
| `reflexAsm`（有参实例方法） | 5,156 | 48,152 | 9.3× |
| `reflexInstant`（有参实例方法） | 5,508 | 42,481 | 7.7× |
| `unsafeAccessField`（字段读写） | 581 | 23,905 | 41× |

优化内容：

1. **`JavaClassMethod`** — 实例方法分支去掉每次调用的 `bindTo`，
   改为直接传入接收者，消除 `BoundMethodHandle` 分配。
2. **`UnsafeAccess`** — 增加 `Field -> MethodHandle` 的 `WeakCache`，
   `unreflect` 只执行一次；去掉 `bindTo`，改用 `invoke` 直调。

### 对照组

`methodInvoke`、`fieldReflect`、`mhGenericCached`、`mhExact`、`direct` 等
未受改动影响的路径在两次测量中数据持平，说明测量环境一致、优化未引入回退。

## 调用方式互斥对照（为什么选 `invoke`）

为隔离「缓存 handle」与「调用方式」各自的成本，另设一组基准：
**同一个已缓存的字段 handle，只改变调用方式**，排除 unreflect、Reflex 包装等干扰。

参数：`-wi 5 -i 5 -f 3`（15 个采样点）。

| 基准 | ops/ms | 相对 `invoke` | 说明 |
|---|---|---|---|
| `mhFieldUnreflectEachCall` | 1,221 ±38 | 0.013× | 每次调用都 `unreflect`（改造前 `UnsafeAccess` 的真实行为） |
| `mhFieldInvokeWithArguments` | 2,612 ±118 | 0.028× | 缓存 handle，但用 `invokeWithArguments` |
| `mhFieldBindToEachCall` | 18,347 ±877 | 0.19× | 每次调用都 `bindTo` 再 invoke |
| `mhFieldInvoke` | 94,094 ±1,893 | 1× | 缓存 handle + `invoke` 直调（当前采用） |
| `mhFieldBoundOnce` | 246,414 ±5,111 | 2.6× | `bindTo` 一次后复用（仅适用于固定接收者） |

结论：

- `invokeWithArguments` 比 `invoke` **慢 36 倍** —— 每次调用都要做运行时参数类型推断与数组打包
- 每次 `bindTo` 比 `invoke` **慢 5 倍** —— 每次都会分配一个 `BoundMethodHandle`
- 每次 `unreflect` 是最慢的（比 `invoke` 慢 77 倍），这正是「增加缓存」要消除的开销

`mhFieldBoundOnce` 虽然更快，但要求接收者在编译期固定，
`UnsafeAccess.get(src, field)` 的 API 形态无法采用，故选择 `invoke`。

## 注意事项

- 反射调用的绝对性能受 JVM 版本、JIT 编译策略、CPU 影响较大；
  结论性对比应在同一台机器、同一 JVM 上连续完成。
- `-f 1` 时误差区间较宽（可达 ±40%），差异小于 2 倍的结论建议改用 `-f 3` 复测。
- `benchmark` 模块已禁用 publish 任务，不会进入 Maven 产物。
