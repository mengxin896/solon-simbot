package love.forte.simbot.solon.demo.javaapp;

import love.forte.simbot.application.Application;
import love.forte.simbot.event.EventProcessors;
import org.noear.solon.Solon;

public final class DemoJavaApp {
    private DemoJavaApp() {
    }

    public static void main(String[] args) {
        DemoJavaState.reset();

        var solonApp = Solon.start(DemoJavaApp.class, args, app -> app.enableHttp(false));

        try {
            var context = solonApp.context();
            var simbotApp = (Application) context.getBean("simbotApplication");
            if (simbotApp == null) {
                throw new IllegalStateException("simbotApplication bean not found. Is simbot-solon-starter on classpath?");
            }

            EventProcessors.pushAndCollectToListBlocking(simbotApp.getEventDispatcher(), new DemoJavaEvent());

            var invoked = DemoJavaState.INVOKED_COUNT.get();
            if (invoked != 1) {
                throw new IllegalStateException("Expected Java listener to be invoked once, but actual=" + invoked);
            }

            if (DemoJavaState.INJECTED_SERVICE_REF.get() == null) {
                throw new IllegalStateException("Java listener did not receive DemoJavaService from Solon context");
            }

            System.out.println("Demo done. invoked=" + invoked);
        } finally {
            Solon.stopBlock();
        }
    }
}
