package middleware.component_model.remoting;

import com.fasterxml.jackson.databind.ObjectMapper;
import middleware.component_model.annotations.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class JsonMarshaller implements Marshaller {

    // classe principal do Jackson para conversão
    private final ObjectMapper objectMapper;

    public JsonMarshaller() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Object[] unmarshal(String requestBody, Method method) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        if (parameters.length == 0) {
            return args; // método sem parâmetros
        }

        // busca pelo parâmetro anotado com @RequestBody
        Parameter bodyParam = null;
        for (Parameter param : parameters) {
            if (param.isAnnotationPresent(RequestBody.class)) {
                bodyParam = param;
                break;
            }
        }

        // se encontrou um @RequestBody, faz o mapeamento
        if (bodyParam != null) {
            if (requestBody == null || requestBody.isEmpty()) {
                throw new Exception("Request body is missing for parameter annotated with @RequestBody.");
            }

            Class<?> paramType = bodyParam.getType(); // Ex: Usuario.class

            // JACKSON: Converte a string JSON num objeto do tipo 'paramType'
            Object mappedObject = objectMapper.readValue(requestBody, paramType);

            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(RequestBody.class)) {
                    args[i] = mappedObject;
                } else {
                    args[i] = null;
                }
            }
        }

        return args;
    }

    @Override
    public String marshal(Object result) throws Exception {
        // converte o objeto de resultado em uma string JSON
        return objectMapper.writeValueAsString(result);
    }
}
