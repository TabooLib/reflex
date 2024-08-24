package org.tabooproject.reflex.res;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeResource {

    String value();

    String hash();

    DependencyScope[] scopes();
}