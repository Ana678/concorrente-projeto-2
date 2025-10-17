package application.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Grupo {
    private String id;
    private String nome;
    private Map<String, String> membros; // userId -> userName

    public Grupo(String id, String nome) {
        this.id = id;
        this.nome = nome;
    this.membros = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Set<String> getMembros() {
        return new HashSet<>(membros.keySet());
    }

    public Map<String, String> getMembrosMap() {
        return membros;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMembros(Map<String, String> membros) {
        this.membros = membros;
    }

    public boolean adicionarMembro(String userId, String userName) {
        if (userId == null || userId.trim().isEmpty()) return false;
        if (userName == null || userName.trim().isEmpty()) return false;
        if (this.membros.containsKey(userId)) return false;
        this.membros.put(userId, userName);
        return true;
    }

    public boolean removerMembro(String userId) {
        return this.membros.remove(userId) != null;
    }

    @Override
    public String toString() {
        return "Grupo{" +
               "id='" + id + '\'' +
               ", nome='" + nome + '\'' +
               ", membros=" + membros +
               '}';
    }
}
