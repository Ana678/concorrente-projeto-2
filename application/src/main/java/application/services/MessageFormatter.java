package application.services;

import middleware.component_model.annotations.*;
import application.dto.GroupIdDTO;
import application.entity.Mensagem;

import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import middleware.lifecycle.annotations.LifecyclePolicy;
import middleware.lifecycle.annotations.LifecyclePolicyType;
import middleware.lifecycle.annotations.Pooled;

@LifecyclePolicy(LifecyclePolicyType.PER_REQUEST)
@RequestMapping(path = "/formatter")
public class MessageFormatter {

    private final MessageStore messageStore;

    public MessageFormatter(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    @GetMapping(path = "/getFormattedMessages")
    public String format(@RequestBody GroupIdDTO data) {
        LinkedList<Mensagem> rawMessages = messageStore.getMessagesForGroup(data);

        if (rawMessages == null || rawMessages.isEmpty()) {
            return "Nenhuma mensagem para o grupo " + data.getGroupId();
        }

        StringBuilder formatted = new StringBuilder();
        formatted.append("--- MENSAGENS DO GRUPO: ").append(data.getGroupId()).append(" ---\n");

        for (Mensagem msg : rawMessages) {
            String user = msg.getUserId().replace("user-", "");
            String timestamp = msg.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
            formatted.append(String.format("[%s] %s: %s\n", timestamp, user, msg.getContent()));
        }

        return formatted.toString();
    }
}
