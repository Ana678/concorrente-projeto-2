package application.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Mensagem {
    private final String id;
    private final String userId;
    private final String groupId;
    private final String content;
    private final LocalDateTime timestamp;

    public Mensagem(String id, String userId, String groupId, String content) {
        this.id = id;
        this.userId = userId;
        this.groupId = groupId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getGroupId() { return groupId; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public Instant getTimestampAsInstant() {
        return this.timestamp.toInstant(ZoneOffset.UTC);
    }

    @Override
    public String toString() {
        return String.join("|",
                id,
                userId,
                groupId,
                content,
                timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
