package love.forte.simbot.solon.onebot11.demo.javaapp;

import love.forte.simbot.application.Application;
import org.noear.solon.Solon;

import java.nio.file.Paths;

public final class OneBot11NapCatJavaDemoApp {
    private OneBot11NapCatJavaDemoApp() {
    }

    public static void main(String[] args) {
        // The demo pins its config entry to app.properties so startup behavior
        // stays stable across `exec:java` and direct `java -cp` launches.
        var solonApp = Solon.start(
            OneBot11NapCatJavaDemoApp.class,
            new String[]{"cfg=app.properties"}
        );
        var context = solonApp.context();
        var simbotApplication = (Application) context.getBean("simbotApplication");
        if (simbotApplication == null) {
            throw new IllegalStateException("simbotApplication bean not found. Is simbot-solon-starter loaded?");
        }

        System.out.println("OneBot11 + NapCat Java demo started.");
        System.out.println("Current workdir: " + Paths.get("").toAbsolutePath());
        System.out.println("Bot configuration file: ./simbot-bots/napcat.bot.json");
        System.out.println("If NapCat is already connected, send `ping` to the bot and it should reply `pong`.");

        simbotApplication.joinBlocking();
    }
}
