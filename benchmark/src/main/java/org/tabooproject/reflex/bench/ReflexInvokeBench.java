package org.tabooproject.reflex.bench;

import org.openjdk.jmh.annotations.*;
import org.tabooproject.reflex.AnalyseMode;
import org.tabooproject.reflex.ClassField;
import org.tabooproject.reflex.ClassMethod;
import org.tabooproject.reflex.ReflexClass;
import org.tabooproject.reflex.UnsafeAccess;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 反射调用路径基准测试。
 *
 * 运行方式：
 * <pre>
 *   ./gradlew :benchmark:jmh                                   # 全量
 *   ./gradlew :benchmark:jmh -PjmhArgs="ReflexInvokeBench.*"   # 按正则过滤
 *   ./gradlew :benchmark:jmh -PjmhArgs="-wi 5 -i 5 -f 3"       # 自定义轮次
 * </pre>
 *
 * 各路径说明：
 * - direct          直接调用，JIT 基线
 * - methodInvoke    传统 Method.invoke（setAccessible）
 * - reflexAsm       Reflex ASM 模式（含 vararg 装箱）
 * - reflexInstant   Reflex REFLECTION_ONLY 模式（Instant 路径）
 * - mhExact         MethodHandle 精确签名 invokeExact（理论上限）
 * - mhGenericCached MethodHandle 泛化 + 缓存接收者（模拟 JavaClassMethod 形状）
 * - unsafeAccess    UnsafeAccess.get/put（弱引用缓存 MethodHandle 后）
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class ReflexInvokeBench {

    /** 被反射调用的目标类 */
    public static class Target {
        public int counter = 0;

        public int add(int a, int b) {
            return a + b;
        }

        public String noArg() {
            return "ok";
        }

        public static String staticFunc(int x) {
            return "s" + x;
        }
    }

    final Target target = new Target();
    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    Method methodAdd;
    Method methodNoArg;
    Method methodStatic;
    Field counterField;

    ReflexClass reflexClassAsm;
    ReflexClass reflexClassInstant;
    ClassMethod reflexAddAsm;
    ClassMethod reflexAddInstant;
    ClassMethod reflexNoArgAsm;
    ClassMethod reflexStaticAsm;
    ClassField reflexCounterAsm;
    ClassField reflexCounterInstant;

    MethodHandle exactAdd;
    MethodHandle genericAddSpreader;
    MethodHandle exactNoArg;

    /** 字段 setter/getter 的精确签名 handle，用于对照 invoke 与 invokeWithArguments */
    MethodHandle cachedSetter;
    MethodHandle cachedGetter;
    /** bindTo 后的 handle，用于对照 bindTo 每次分配的开销 */
    MethodHandle boundGetter;

    int sink;

    @Setup(Level.Trial)
    public void setup() throws Throwable {
        // 传统反射
        methodAdd = Target.class.getMethod("add", int.class, int.class);
        methodNoArg = Target.class.getMethod("noArg");
        methodStatic = Target.class.getMethod("staticFunc", int.class);
        methodAdd.setAccessible(true);
        methodNoArg.setAccessible(true);
        methodStatic.setAccessible(true);
        counterField = Target.class.getDeclaredField("counter");
        counterField.setAccessible(true);

        // Reflex ASM 模式
        reflexClassAsm = ReflexClass.Companion.of(Target.class, AnalyseMode.ASM_ONLY);
        reflexAddAsm = reflexClassAsm.getMethod("add", true, false, 1, 2);
        reflexNoArgAsm = reflexClassAsm.getMethod("noArg", true, false);
        reflexStaticAsm = reflexClassAsm.getMethod("staticFunc", true, false, 1);
        reflexCounterAsm = reflexClassAsm.getField("counter", false, false);

        // Reflex REFLECTION_ONLY 模式
        reflexClassInstant = ReflexClass.Companion.of(Target.class, AnalyseMode.REFLECTION_ONLY);
        reflexAddInstant = reflexClassInstant.getMethod("add", true, false, 1, 2);
        reflexCounterInstant = reflexClassInstant.getField("counter", false, false);

        // MethodHandle：精确签名（理论上限）
        MethodType addType = MethodType.methodType(int.class, int.class, int.class);
        exactAdd = LOOKUP.findVirtual(Target.class, "add", addType);
        exactNoArg = LOOKUP.findVirtual(Target.class, "noArg", MethodType.methodType(String.class)).bindTo(target);

        // MethodHandle：泛化 + spreader（复刻 JavaClassMethod.optimizedInvoker 的构造方式）
        MethodHandle addHandle = LOOKUP.findVirtual(Target.class, "add", addType);
        MethodHandle spreader = addHandle.asSpreader(Object[].class, 2);
        genericAddSpreader = spreader.asType(spreader.type().generic());

        // 字段 handle：对照 invoke / invokeWithArguments / bindTo 的开销差异
        cachedSetter = LOOKUP.findSetter(Target.class, "counter", int.class);
        cachedGetter = LOOKUP.findGetter(Target.class, "counter", int.class);
        boundGetter = cachedGetter.bindTo(target);
    }

    // ==================== 有参实例方法 add(int,int) ====================

    @Benchmark
    public int direct() {
        return target.add(1, 2);
    }

    @Benchmark
    public int methodInvoke() throws Exception {
        return (Integer) methodAdd.invoke(target, 1, 2);
    }

    @Benchmark
    public int reflexAsm() {
        return (Integer) reflexAddAsm.invoke(target, 1, 2);
    }

    @Benchmark
    public int reflexInstant() {
        return (Integer) reflexAddInstant.invoke(target, 1, 2);
    }

    @Benchmark
    public int mhExact() throws Throwable {
        return (int) exactAdd.invokeExact(target, 1, 2);
    }

    @Benchmark
    public int mhGenericCached() throws Throwable {
        return (Integer) genericAddSpreader.invoke(target, new Object[]{1, 2});
    }

    // ==================== 无参实例方法 noArg() ====================

    @Benchmark
    public String directNoArg() {
        return target.noArg();
    }

    @Benchmark
    public String methodInvokeNoArg() throws Exception {
        return (String) methodNoArg.invoke(target);
    }

    @Benchmark
    public String reflexAsmNoArg() {
        return (String) reflexNoArgAsm.invoke(target);
    }

    @Benchmark
    public String mhExactNoArg() throws Throwable {
        return (String) exactNoArg.invokeExact();
    }

    // ==================== 静态方法 staticFunc(int) ====================

    @Benchmark
    public String directStatic() {
        return Target.staticFunc(42);
    }

    @Benchmark
    public String methodInvokeStatic() throws Exception {
        return (String) methodStatic.invoke(null, 42);
    }

    @Benchmark
    public String reflexAsmStatic() {
        return (String) reflexStaticAsm.invokeStatic(42);
    }

    // ==================== 字段读写 counter（private int）====================

    @Benchmark
    public int directField() {
        target.counter = 42;
        return target.counter;
    }

    @Benchmark
    public int fieldReflect() throws Exception {
        counterField.set(target, 42);
        return (Integer) counterField.get(target);
    }

    @Benchmark
    public int reflexFieldAsm() {
        reflexCounterAsm.set(target, 42);
        return (Integer) reflexCounterAsm.get(target);
    }

    @Benchmark
    public int reflexFieldInstant() {
        reflexCounterInstant.set(target, 42);
        return (Integer) reflexCounterInstant.get(target);
    }

    @Benchmark
    public int unsafeAccessField() {
        // 缓存 MethodHandle 后的 UnsafeAccess 路径（内部使用 invoke 直调）
        UnsafeAccess.INSTANCE.put(target, counterField, 42);
        Integer result = UnsafeAccess.INSTANCE.get(target, counterField);
        return result == null ? 0 : result;
    }

    // ==================== 互斥对照：同一缓存 handle，只改变调用方式 ====================
    // 用于隔离 invoke / invokeWithArguments / bindTo 各自的成本，
    // 排除 unreflect 缓存、Reflex 包装等因素的干扰。

    /** 精确签名 handle + invoke 直调（当前 UnsafeAccess 采用的写法） */
    @Benchmark
    public int mhFieldInvoke() throws Throwable {
        cachedSetter.invoke(target, 42);
        return (int) cachedGetter.invoke(target);
    }

    /** 精确签名 handle + invokeWithArguments（改造前的写法，每次做运行时参数打包） */
    @Benchmark
    public int mhFieldInvokeWithArguments() throws Throwable {
        cachedSetter.invokeWithArguments(target, 42);
        return (Integer) cachedGetter.invokeWithArguments(target);
    }

    /** 每次调用都 bindTo 再 invoke（改造前的另一处开销来源） */
    @Benchmark
    public int mhFieldBindToEachCall() throws Throwable {
        MethodHandle bound = cachedGetter.bindTo(target);
        return (int) bound.invoke();
    }

    /** bindTo 一次后复用（bindTo 的正确用法对照） */
    @Benchmark
    public int mhFieldBoundOnce() throws Throwable {
        return (int) boundGetter.invoke();
    }

    /** 每次调用都 unreflect（改造前的 UnsafeAccess 真实行为） */
    @Benchmark
    public int mhFieldUnreflectEachCall() throws Throwable {
        MethodHandle setter = LOOKUP.unreflectSetter(counterField);
        MethodHandle getter = LOOKUP.unreflectGetter(counterField);
        setter.invoke(target, 42);
        return (int) getter.invoke(target);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        // 消费 sink 防止死代码消除（JMH 返回值已消费，此处仅占位）
        sink = target.counter;
    }
}
