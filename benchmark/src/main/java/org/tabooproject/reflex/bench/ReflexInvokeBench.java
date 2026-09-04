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
        private int counter = 0;

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
        // 缓存 MethodHandle 后的 UnsafeAccess 路径
        UnsafeAccess.INSTANCE.put(target, counterField, 42);
        Integer result = UnsafeAccess.INSTANCE.get(target, counterField);
        return result == null ? 0 : result;
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        // 消费 sink 防止死代码消除（JMH 返回值已消费，此处仅占位）
        sink = target.counter;
    }
}
