package love.forte.simbot.solon.demo.javaapp;

import love.forte.simbot.quantcat.common.annotations.Listener;
import org.noear.solon.annotation.Component;

@Component
public class DemoJavaListener {
    @Listener
    public void onDemo(DemoJavaEvent event, DemoJavaService service) {
        DemoJavaState.INVOKED_COUNT.incrementAndGet();
        DemoJavaState.INJECTED_SERVICE_REF.set(service);
        System.out.println("onDemo invoked. eventId=" + event.getId() + ", service=" + service.ping());
    }
}
