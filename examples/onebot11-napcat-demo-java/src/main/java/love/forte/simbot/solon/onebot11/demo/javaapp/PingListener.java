package love.forte.simbot.solon.onebot11.demo.javaapp;

import love.forte.simbot.component.onebot.v11.core.event.message.OneBotGroupMessageEvent;
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotPrivateMessageEvent;
import love.forte.simbot.event.MessageEvent;
import love.forte.simbot.quantcat.common.annotations.Listener;
import org.noear.solon.annotation.Component;

@Component
public class PingListener {
    @Listener
    public void onMessage(MessageEvent event) {
        var plainText = event.getMessageContent().getPlainText();
        var text = plainText == null ? "" : plainText.trim();
        if (!"ping".equals(text)) {
            return;
        }

        if (event instanceof OneBotPrivateMessageEvent) {
            System.out.println("Private message: author=" + event.getAuthorId() + ", text=" + text);
        } else if (event instanceof OneBotGroupMessageEvent groupEvent) {
            System.out.println("Group message: group=" + groupEvent.getGroupId() + ", user=" + groupEvent.getUserId() + ", text=" + text);
        } else {
            System.out.println("Message event: type=" + event.getClass().getName() + ", author=" + event.getAuthorId() + ", text=" + text);
        }

        event.replyBlocking("pong");
    }
}
