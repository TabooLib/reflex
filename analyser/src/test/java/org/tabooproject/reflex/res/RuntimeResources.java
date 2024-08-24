package org.tabooproject.reflex.res;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeResources {

    RuntimeResource[] value1();

    int[] value2();

    String[] value3();

    boolean[] value4();

    boolean value5();
}
