package love.forte.simbot.solon;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

final class JavaListenerTestState {
    static final AtomicInteger INVOKED_COUNT = new AtomicInteger(0);
    static final AtomicReference<JavaTestService> INJECTED_SERVICE_REF = new AtomicReference<>(null);

    private JavaListenerTestState() {
    }
}
