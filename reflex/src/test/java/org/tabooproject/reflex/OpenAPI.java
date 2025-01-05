package org.tabooproject.reflex;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * TabooLib
 * taboolib.common.org.tabooproject.reflex.OpenAPI
 *
 * @author sky
 * @since 2021/7/2 10:37 下午
 */
public class OpenAPI {

    Object[] data;

    public OpenAPI(Object[] data) {
        this.data = data;
    }

    @NotNull
    public static OpenResult call(String name, Object[] data) {
        System.out.println("OpenAPI call " + name + " " + Arrays.toString(data));
        return null;
    }
}
