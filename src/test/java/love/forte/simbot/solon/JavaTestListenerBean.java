package love.forte.simbot.solon;

import love.forte.simbot.quantcat.common.annotations.Listener;
import org.noear.solon.annotation.Component;

@Component
public class JavaTestListenerBean {
    @Listener
    public void onTest(JavaTestEvent event, JavaTestService service) {
        JavaListenerTestState.INVOKED_COUNT.incrementAndGet();
        JavaListenerTestState.INJECTED_SERVICE_REF.set(service);
    }
}
