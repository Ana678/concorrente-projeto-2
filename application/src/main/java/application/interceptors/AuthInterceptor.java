package application.interceptors;

import application.dto.AddMessageDTO;
import application.services.MessageStore;
import middleware.exceptions.AuthException;
import middleware.extension.InvocationContext;
import middleware.extension.InvocationInterceptor;
import middleware.util.Log;

/**
 * Interceptador que verifica se um usuário tem permissão para
 * enviar uma mensagem para um grupo (se ele é membro).
 */
public class AuthInterceptor implements InvocationInterceptor {

    @Override
    public void beforeInvocation(InvocationContext context) throws Exception {

        String methodName = context.getAbsoluteObjectReference().getMethod().getName();
        if (!methodName.equals("addMessage")) return;

        Object target = context.getTargetObject();
        Object[] params = context.getMethodParameters();

        if (!(target instanceof MessageStore) || !(params[0] instanceof AddMessageDTO)) {
            Log.warn("AuthInterceptor", "Não foi possível aplicar. Tipos de objeto ou parâmetro inesperados.");
            return;
        }

        MessageStore store = (MessageStore) target;
        AddMessageDTO dto = (AddMessageDTO) params[0];

        String userId = dto.getUserId();
        String groupId = dto.getGroupId();

        if (userId == null || groupId == null) {
            throw new AuthException("Usuário ou Grupo não podem ser nulos.");
        }

        if (store.isMember(groupId, userId)) {
            Log.info("AuthInterceptor", "AUTORIZADO: Usuário '%s' pode postar no grupo '%s'.", userId, groupId);
        } else {
            Log.error("AuthInterceptor", String.format("NEGADO: Usuário '%s' não é membro do grupo '%s'.", userId, groupId));
            throw new AuthException("Usuário '" + userId + "' não tem permissão para postar no grupo '" + groupId + "'.");
        }
    }

    @Override
    public void afterInvocation(InvocationContext context) {
    }
}
