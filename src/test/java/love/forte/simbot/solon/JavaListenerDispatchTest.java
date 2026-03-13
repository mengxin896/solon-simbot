package love.forte.simbot.solon;

import love.forte.simbot.application.Application;
import love.forte.simbot.event.EventProcessors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.Solon;

class JavaListenerDispatchTest {

    @Test
    void javaListenerShouldBeRegisteredAndInvokedWithInjectedBean() {
        JavaListenerTestState.INVOKED_COUNT.set(0);
        JavaListenerTestState.INJECTED_SERVICE_REF.set(null);

        var solonApp = Solon.start(JavaListenerDispatchTest.class, new String[0], app -> app.enableHttp(false));

        try {
            var context = solonApp.context();
            var application = (Application) context.getBean("simbotApplication");
            Assertions.assertNotNull(application, "Solon context should contain bean: simbotApplication");

            EventProcessors.pushAndCollectToListBlocking(application.getEventDispatcher(), new JavaTestEvent());

            Assertions.assertEquals(1, JavaListenerTestState.INVOKED_COUNT.get(), "Java listener should be invoked exactly once");
            var injectedService = JavaListenerTestState.INJECTED_SERVICE_REF.get();
            Assertions.assertNotNull(injectedService, "Java listener should receive injected JavaTestService");
            Assertions.assertSame(context.getBean(JavaTestService.class), injectedService);
        } finally {
            Solon.stopBlock();
        }
    }
}
