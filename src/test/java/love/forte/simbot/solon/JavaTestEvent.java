package love.forte.simbot.solon;

import love.forte.simbot.common.id.ID;
import love.forte.simbot.common.id.UUID;
import love.forte.simbot.common.time.Timestamp;
import love.forte.simbot.event.Event;

public final class JavaTestEvent implements Event {
    private final ID id = UUID.random();
    private final Timestamp time = Timestamp.now();

    @Override
    public ID getId() {
        return id;
    }

    @Override
    public Timestamp getTime() {
        return time;
    }
}
