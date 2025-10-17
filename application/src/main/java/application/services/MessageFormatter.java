package application.services;

import application.entity.Mensagem;
import middleware.component_model.annotations.Param;
import middleware.component_model.annotations.RemoteMethod;
import middleware.component_model.annotations.RemoteObject;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

@RemoteObject(name = "formatter")
public class MessageFormatter {

    private final MessageStore messageStore;

    public MessageFormatter(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    @RemoteMethod(name = "getFormattedMessages")
    public String format(@Param(name = "groupId") String groupId) {
        
        LinkedList<Mensagem> rawMessages = messageStore.getMessagesForGroup(groupId);

        if (rawMessages == null || rawMessages.isEmpty()) {
            return "Nenhuma mensagem para o grupo " + groupId;
        }

        StringBuilder formatted = new StringBuilder();
        formatted.append("--- MENSAGENS DO GRUPO: ").append(groupId).append(" ---\n");

        for (Mensagem msg : rawMessages) {
            String user = msg.getUserId().replace("user-", "");
            String timestamp = msg.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
            formatted.append(String.format("[%s] %s: %s\n", timestamp, user, msg.getContent()));
        }

        return formatted.toString();
    }
}
