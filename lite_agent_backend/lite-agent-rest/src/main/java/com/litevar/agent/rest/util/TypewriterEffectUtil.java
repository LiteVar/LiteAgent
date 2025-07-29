package com.litevar.agent.rest.util;

import java.util.function.Consumer;

/**
 * 模拟打字效果输出
 *
 * @author uncle
 * @since 2025/5/21 12:22
 */
public class TypewriterEffectUtil {
    public static void part(String text, long delay, Consumer<String> handler) {
        int length = text.length();
        for (int i = 0; i < length; ) {
            int codePoint = text.codePointAt(i);
            String charStr = new String(Character.toChars(codePoint));
            handler.accept(charStr);
            i += Character.charCount(codePoint);
            try {
                Thread.sleep((long) (delay * (Math.random() + 0.4)));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
