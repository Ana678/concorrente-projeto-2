package application.services;

import application.entity.Grupo;
import application.entity.Mensagem;
import middleware.component_model.annotations.Param;
import middleware.component_model.annotations.RemoteMethod;
import middleware.component_model.annotations.RemoteObject;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@RemoteObject(name = "messagestore")
public final class MessageStore {
    private final Map<String, LinkedList<Mensagem>> groupLogs = new ConcurrentHashMap<>();
    private final Map<String, Grupo> activeGroups = new ConcurrentHashMap<>();
    private final int logCapacity;

    public MessageStore(int logCapacity) {
        this.logCapacity = logCapacity;

        System.out.println("MessageStore populado com dados de exemplo.");
        createGroup("grupo-123", "Grupo de Teste");
        adicionarMembro("grupo-123", "user-Alice", "Alice");
        adicionarMembro("grupo-123", "user-Bob", "Bob");
        addMessage("grupo-123", "user-Alice", "Olá, pessoal!");
    }

    @RemoteMethod(name = "addMessage")
    public String addMessage(@Param(name = "groupId") String groupId, @Param(name = "userId") String userId, @Param(name = "content") String content) {
        Mensagem message = new Mensagem(UUID.randomUUID().toString(), userId, groupId, content);
        if (!groupExists(groupId)) {
            return "GROUP_NOT_FOUND";
        }
        if (!getMembros(groupId).contains(message.getUserId())) {
            return "USER_NOT_AUTHORIZED";
        }

        groupLogs.computeIfAbsent(groupId, k -> new LinkedList<>()).addLast(message);
        checkAndClean(groupId);
        return "SUCCESS;" + message.getId();
    }

    private void checkAndClean(String groupId) {
        LinkedList<Mensagem> log = groupLogs.get(groupId);
        if (log != null && log.size() > logCapacity) {
            int toRemove = log.size() - logCapacity;
            for (int i = 0; i < toRemove; i++) {
                log.removeFirst();
            }
        }
    }

    @RemoteMethod(name = "getMessages")
    public LinkedList<Mensagem> getMessagesForGroup(@Param(name = "groupId") String groupId) {
        return groupLogs.getOrDefault(groupId, new LinkedList<>());
    }

    @RemoteMethod(name = "createGroup")
    public boolean createGroup(@Param(name = "groupId") String groupId, @Param(name = "groupName") String groupName) {
        if (activeGroups.containsKey(groupId)) {
            return false;
        }
        activeGroups.put(groupId, new Grupo(groupId, groupName));
        System.out.println("[MessageStore] Grupo criado: " + groupId);
        return true;
    }

    @RemoteMethod(name = "groupExists")
    public boolean groupExists(@Param(name = "groupId") String groupId) {
        return activeGroups.containsKey(groupId);
    }

    @RemoteMethod(name = "addMember")
    public String adicionarMembro(@Param(name = "groupId") String groupId, @Param(name = "userId") String userId, @Param(name = "userName") String userName) {
        Grupo grupo = activeGroups.get(groupId);
        if (grupo == null) return "GROUP_NOT_FOUND";
        if (userId == null || userId.trim().isEmpty() || userName == null || userName.trim().isEmpty()) return "INVALID_MEMBER_DATA";
        return grupo.adicionarMembro(userId, userName) ? "SUCCESS" : "MEMBER_ALREADY_EXISTS";
    }

    @RemoteMethod(name = "removeMember")
    public String removerMembro(@Param(name = "groupId") String groupId, @Param(name = "userId") String userId) {
        Grupo grupo = activeGroups.get(groupId);
        if (grupo == null) return "GROUP_NOT_FOUND";
        return grupo.removerMembro(userId) ? "SUCCESS" : "MEMBER_NOT_FOUND";
    }

    @RemoteMethod(name = "getMembers")
    public Set<String> getMembros(@Param(name = "groupId") String groupId) {
        Grupo grupo = activeGroups.get(groupId);
        return (grupo != null) ? grupo.getMembros() : Set.of();
    }

    @RemoteMethod(name = "deleteGroup")
    public boolean deleteGroup(@Param(name = "groupId") String groupId) {
        if (activeGroups.remove(groupId) != null) {
            groupLogs.remove(groupId);
            System.out.println("[MessageStore] Grupo removido: " + groupId);
            return true;
        }
        return false;
    }
}
