package love.forte.simbot.solon.demo.javaapp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

final class DemoJavaState {
    static final AtomicInteger INVOKED_COUNT = new AtomicInteger(0);
    static final AtomicReference<DemoJavaService> INJECTED_SERVICE_REF = new AtomicReference<>(null);

    private DemoJavaState() {
    }

    static void reset() {
        INVOKED_COUNT.set(0);
        INJECTED_SERVICE_REF.set(null);
    }
}
