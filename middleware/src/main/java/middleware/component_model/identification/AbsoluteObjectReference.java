package middleware.component_model.identification;

import java.lang.reflect.Method;

import middleware.lifecycle.annotations.LifecyclePolicyType;

public class AbsoluteObjectReference {

    private final String basePath;
    private final Method method;
    private final Class<?> remoteObjectClass; // PER_REQUEST
    private final LifecyclePolicyType policyType;

    public AbsoluteObjectReference(String basePath, Class<?> remoteObjectClass, Method method, LifecyclePolicyType policyType) {
        this.basePath = basePath;
        this.remoteObjectClass = remoteObjectClass;
        this.method = method;
        this.policyType = policyType;
    }

    public String getBasePath() {
        return basePath;
    }

    public Class<?> getRemoteObjectClass() {
        return remoteObjectClass;
    }

    public Method getMethod() {
        return method;
    }

    public LifecyclePolicyType getPolicyType() {
        return policyType;
    }
}
