package middleware.extension;

import java.util.HashMap;
import java.util.Map;

import middleware.component_model.identification.AbsoluteObjectReference;

public class InvocationContext {

    private String httpMethod;
    private String fullPath;
    private String requestBody;

    private AbsoluteObjectReference absoluteObjectReference;
    private Object targetObject;
    private Object[] methodParameters;

    private Object result;
    private Exception exception;

    private final Map<String, Object> attributes = new HashMap<>();
    private Map<String, String> requestHeaders = new HashMap<>();

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getFullPath() { return fullPath; }
    public void setFullPath(String fullPath) { this.fullPath = fullPath; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public AbsoluteObjectReference getAbsoluteObjectReference() { return absoluteObjectReference; }
    public void setAbsoluteObjectReference(AbsoluteObjectReference absoluteObjectReference) { this.absoluteObjectReference = absoluteObjectReference; }

    public Object getTargetObject() { return targetObject; }
    public void setTargetObject(Object targetObject) { this.targetObject = targetObject; }

    public Object[] getMethodParameters() { return methodParameters; }
    public void setMethodParameters(Object[] methodParameters) { this.methodParameters = methodParameters; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    public Exception getException() { return exception; }
    public void setException(Exception exception) { this.exception = exception; }

    // para dados personalizados pelos interceptadores
    public Map<String, Object> getAttributes() { return attributes; }

    public boolean hasException() {
        return this.exception != null;
    }

}
