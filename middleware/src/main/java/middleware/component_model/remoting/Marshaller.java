package middleware.component_model.remoting;

import java.lang.reflect.Method;

public interface Marshaller {

    Object[] unmarshal(String requestBody, Method method) throws Exception;
    
    String marshal(Object result) throws Exception;
}
