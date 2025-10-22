package application.services;

import middleware.component_model.annotations.*;
import application.dto.*;
import application.entity.Grupo;
import application.entity.Mensagem;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import middleware.lifecycle.annotations.LifecyclePolicy;
import middleware.lifecycle.annotations.LifecyclePolicyType;
import middleware.util.Log;

@LifecyclePolicy(LifecyclePolicyType.STATIC_INSTANCE)
@RequestMapping(path = "/messagestore")
public class MessageStore {
    private final Map<String, LinkedList<Mensagem>> groupLogs = new ConcurrentHashMap<>();
    private final Map<String, Grupo> activeGroups = new ConcurrentHashMap<>();
    private final int logCapacity;

    public MessageStore(int logCapacity) {
        this.logCapacity = logCapacity;
        Log.info("MessageStore", "MessageStore populado com dados de exemplo.");

        Grupo g1 = new Grupo("grupo-123", "Grupo de Teste");
        g1.adicionarMembro("user-Alice", "Alice");
        g1.adicionarMembro("user-Bob", "Bob");
        activeGroups.put("grupo-123", g1);
        addMessageInternal("grupo-123", new Mensagem(UUID.randomUUID().toString(), "user-Alice", "grupo-123", "Olá, pessoal!"));
    }

    // popular dados de exemplo
    private void addMessageInternal(String groupId, Mensagem message) {
        groupLogs.computeIfAbsent(groupId, k -> new LinkedList<>()).addLast(message);
        checkAndClean(groupId);
    }

    @PostMapping(path = "/addMessage")
    public String addMessage(@RequestBody AddMessageDTO data) {
        Mensagem message = new Mensagem(UUID.randomUUID().toString(), data.getUserId(), data.getGroupId(), data.getContent());
        Grupo grupo = activeGroups.get(data.getGroupId());
        if (grupo == null) {
            return "GROUP_NOT_FOUND";
        }
        if (!grupo.getMembros().contains(message.getUserId())) {
            return "USER_NOT_AUTHORIZED";
        }
        addMessageInternal(data.getGroupId(), message);
        return "SUCCESS;" + message.getId();
    }

    private void checkAndClean(String groupId) {
        LinkedList<Mensagem> log = groupLogs.get(groupId);
        if (log != null && log.size() > logCapacity) {
            log.removeFirst();
        }
    }

    @GetMapping(path = "/getMessages")
    public LinkedList<Mensagem> getMessagesForGroup(@RequestBody GroupIdDTO data) {
        return groupLogs.getOrDefault(data.getGroupId(), new LinkedList<>());
    }

    @PostMapping(path = "/createGroup")
    public boolean createGroup(@RequestBody CreateGroupDTO data) {
        if (activeGroups.containsKey(data.getGroupId())) {
            return false;
        }
        activeGroups.put(data.getGroupId(), new Grupo(data.getGroupId(), data.getGroupName()));
        Log.info("MessageStore", "Grupo criado: %s", data.getGroupId());
        return true;
    }

    @GetMapping(path = "/groupExists")
    public boolean groupExists(@RequestBody GroupIdDTO data) {
        return activeGroups.containsKey(data.getGroupId());
    }

    @PostMapping(path = "/addMember")
    public String adicionarMembro(@RequestBody AddMemberDTO data) {
        Grupo grupo = activeGroups.get(data.getGroupId());
        if (grupo == null) return "GROUP_NOT_FOUND";
        if (data.getUserId() == null || data.getUserId().trim().isEmpty() || data.getUserName() == null || data.getUserName().trim().isEmpty()) return "INVALID_MEMBER_DATA";
        return grupo.adicionarMembro(data.getUserId(), data.getUserName()) ? "SUCCESS" : "MEMBER_ALREADY_EXISTS";
    }

    @GetMapping(path = "/getMembers")
    public Set<String> getMembros(@RequestBody GroupIdDTO data) {
        Grupo grupo = activeGroups.get(data.getGroupId());
        return (grupo != null) ? grupo.getMembros() : Set.of();
    }

    public boolean isMember(String groupId, String userId) {
        Grupo grupo = activeGroups.get(groupId);
        return (grupo != null) && grupo.getMembros().contains(userId);
    }

}
