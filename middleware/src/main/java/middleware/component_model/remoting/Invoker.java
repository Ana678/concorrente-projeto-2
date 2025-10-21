package middleware.component_model.remoting;

import java.lang.reflect.Method;

import middleware.component_model.identification.AbsoluteObjectReference;
import middleware.component_model.identification.Lookup;
import middleware.lifecycle.LifecycleManager;
import middleware.lifecycle.annotations.LifecyclePolicyType;
import middleware.util.Log;


// A função do Invoker é localizar um objeto remoto a partir de um caminho.
// Ele usa o Lookup para encontrar o alvo e o Marshaller para preparar os dados e a resposta.
public class Invoker {

    private final Lookup lookup;
    private final Marshaller marshaller;
    private final LifecycleManager lifecycleManager;

    public Invoker(Lookup lookup, LifecycleManager lifecycleManager) {
        this.lookup = lookup;
        this.lifecycleManager = lifecycleManager;
        // Instancia o Marshaller que usa Jackson
        this.marshaller = new JsonMarshaller();
    }

    /**
     * Lida com uma requisição de invocação.
     *
     * @param httpMethod  O método HTTP (ex: "POST")
     * @param fullPath    O caminho da URL (ex: "/messagestore/createGroup")
     * @param requestBody O JSON cru do corpo da requisição
     * @return uma string JSON representando o resultado da invocação
     * @throws Exception se o método não for encontrado, ou se a invocação falhar
     */
    public String invoke(String httpMethod, String fullPath, String requestBody) throws Exception {

        // chave de busca. Ex.: "POST:/messagestore/createGroup"
        String lookupKey = httpMethod + ":" + fullPath;

        AbsoluteObjectReference absoluteObject  = lookup.find(lookupKey);

        if (absoluteObject == null) {
            throw new RouteNotFoundException("No remote method found for: " + lookupKey);
        }

        Object targetObject = null;
        LifecyclePolicyType policy = absoluteObject.getPolicyType();

        try {
            // Obter instância via LifecycleManager
            targetObject = lifecycleManager.getInstance(absoluteObject);
            Method targetMethod = absoluteObject.getMethod();

             // deserializa o corpo da requisição para os parâmetros do método
            Object[] args = marshaller.unmarshal(requestBody, targetMethod);

            Log.info("Invoker", "Invoking method: %s on object: %s (Policy: %s, Instance: %d)", targetMethod.getName(), targetObject.getClass().getSimpleName(), policy, targetObject.hashCode());

            // Invocar o método
            Object result = targetMethod.invoke(targetObject, args);

            // Serializar resultado
            return marshaller.marshal(result);

        } finally {
            if (targetObject != null) {
                lifecycleManager.returnInstanceToPool(targetObject, policy);
            }
        }
    }
}
