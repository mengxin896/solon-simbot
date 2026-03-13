package love.forte.simbot.solon.demo.javaapp;

import org.noear.solon.annotation.Component;

@Component
public class DemoJavaService {
    public String ping() {
        return "pong-java";
    }
}
