package middleware.component_model.remoting;

import middleware.component_model.annotations.Param;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class JsonMarshaller implements Marshaller {

    @Override
    public Object[] unmarshal(String requestBody, Method method) throws Exception {
        if (requestBody == null || requestBody.isEmpty()) {
            return new Object[0];
        }

        JSONObject json = new JSONObject(requestBody);
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        // Faz o mapeamento dos parâmetros
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

            if (!param.isAnnotationPresent(Param.class)) {
                throw new IllegalArgumentException("Parâmetro do método sem a anotação @Param: " + param.getName());
            }

            String paramName = param.getAnnotation(Param.class).name();
            if (!json.has(paramName)) {
                throw new IllegalArgumentException("O corpo da requisição não contém o parâmetro obrigatório: " + paramName);
            }

            // mapeando de tipos primitivos e String
            args[i] = json.get(paramName);
        }
        return args;
    }

    @Override
    public String marshal(Object result) throws Exception {
        if (result == null) {
            return "{}";
        }
        JSONObject json = new JSONObject();
        json.put("result: ", result);
        return json.toString();
    }
}
