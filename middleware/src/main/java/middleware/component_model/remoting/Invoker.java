package middleware.component_model.remoting;

import java.lang.reflect.Method;

import middleware.component_model.identification.AbsoluteObjectReference;
import middleware.component_model.identification.Lookup;


// A função do Invoker é localizar um objeto remoto a partir de um caminho.
// Ele usa o Lookup para encontrar o alvo e o Marshaller para preparar os dados e a resposta.
public class Invoker {

    private final Lookup lookup;
    private final Marshaller marshaller;

    public Invoker(Lookup lookup) {
        this.lookup = lookup;
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

        Object targetObject = absoluteObject .getRemoteObject();
        Method targetMethod = absoluteObject .getMethod();

        // deserializa o corpo da requisição para os parâmetros do método
        Object[] args = marshaller.unmarshal(requestBody, targetMethod);

        System.out.println("Invoking method: " + targetMethod.getName() + " on object: " + targetObject.getClass().getSimpleName());
        Object result = targetMethod.invoke(targetObject, args);

        // serializa o resultado
        return marshaller.marshal(result);
    }
}
