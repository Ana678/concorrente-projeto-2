package middleware.component_model.remoting;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import middleware.component_model.identification.AbsoluteObjectReference;
import middleware.component_model.identification.Lookup;
import middleware.exceptions.RouteNotFoundException;
import middleware.extension.InvocationContext;
import middleware.extension.InvocationInterceptor;
import middleware.lifecycle.LifecycleManager;
import middleware.lifecycle.annotations.LifecyclePolicyType;
import middleware.util.Log;


// A função do Invoker é localizar um objeto remoto a partir de um caminho.
// Ele usa o Lookup para encontrar o alvo e o Marshaller para preparar os dados e a resposta.
public class Invoker {

    private final Lookup lookup;
    private final Marshaller marshaller;
    private final LifecycleManager lifecycleManager;
    private final List<InvocationInterceptor> interceptors = new ArrayList<>();

    public Invoker(Lookup lookup, LifecycleManager lifecycleManager) {
        this.lookup = lookup;
        this.lifecycleManager = lifecycleManager;
        // Instancia o Marshaller que usa Jackson
        this.marshaller = new JsonMarshaller();
    }

    public void addInterceptor(InvocationInterceptor interceptor) {
        this.interceptors.add(interceptor);
        Log.info("Invoker", "Interceptor registrado: %s", interceptor.getClass().getSimpleName());
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

        InvocationContext context = new InvocationContext();
        context.setHttpMethod(httpMethod);
        context.setFullPath(fullPath);
        context.setRequestBody(requestBody);

        // chave de busca. Ex.: "POST:/messagestore/createGroup"
        String lookupKey = httpMethod + ":" + fullPath;

        AbsoluteObjectReference absoluteObject  = lookup.find(lookupKey);

        if (absoluteObject == null) {
            throw new RouteNotFoundException("Nenhum método remoto encontrado para: " + lookupKey);
        }

        context.setAbsoluteObjectReference(absoluteObject);

        Object targetObject = null;
        LifecyclePolicyType policy = absoluteObject.getPolicyType();

        try {

            targetObject = lifecycleManager.getInstance(absoluteObject);
            Method targetMethod = absoluteObject.getMethod();
            context.setTargetObject(targetObject);

             // deserializa o corpo da requisição para os parâmetros do método
            Object[] args = marshaller.unmarshal(requestBody, targetMethod);
            context.setMethodParameters(args);

            Log.info("Invoker", "Executando 'beforeInvocation' interceptors");
            for (InvocationInterceptor interceptor : interceptors) {
                interceptor.beforeInvocation(context);
            }

            Object[] processedArgs = context.getMethodParameters();

            // Invocar o método
            Log.info("Invoker", "Invocando método: %s no objeto: %s (Política: %s, Instância: %d)", targetMethod.getName(), targetObject.getClass().getSimpleName(), policy, targetObject.hashCode());

            Object result = targetMethod.invoke(targetObject, processedArgs);
            context.setResult(result);

            Log.info("Invoker", "Executando 'afterInvocation' interceptors . . . SUCESSO");
            for (InvocationInterceptor interceptor : interceptors) {
                interceptor.afterInvocation(context);
            }

            // Serializar resultado
            return marshaller.marshal(result);

        } catch (Exception e) {
            context.setException(e);

            Log.info("Invoker", "Executando 'afterInvocation' interceptors . . . ERRO");
            for (InvocationInterceptor interceptor : interceptors) {
                interceptor.afterInvocation(context);
            }
            throw e;

        } finally {
            if (targetObject != null) {
                lifecycleManager.returnInstanceToPool(targetObject, policy);
            }
        }
    }
}
