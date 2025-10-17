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
        this.marshaller = new JsonMarshaller();
    }

    public String invoke(String fullPath, String requestBody) throws Exception {

        AbsoluteObjectReference absoluteObject = lookup.find(fullPath);

        if (absoluteObject == null) {
            throw new Exception("Nenhum objeto remoto encontrado para o caminho: " + fullPath);
        }

        Object targetObject = absoluteObject.getRemoteObject();
        Method targetMethod = absoluteObject.getMethod();

        // Obter os argumentos do método
        Object[] args = marshaller.unmarshal(requestBody, targetMethod);

        System.out.println("Invocando método: " + targetMethod.getName() + " no objeto: " + targetObject.getClass().getSimpleName());
        Object result = targetMethod.invoke(targetObject, args);

        return marshaller.marshal(result);
    }
}
