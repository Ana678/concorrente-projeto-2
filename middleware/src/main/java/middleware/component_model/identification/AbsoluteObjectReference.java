package middleware.component_model.identification;

import java.lang.reflect.Method;

public class AbsoluteObjectReference {

    private final ObjectId objectId;
    private final Object remoteObject;
    private final Method method;

    public AbsoluteObjectReference(ObjectId objectId, Object remoteObject, Method method) {
        this.objectId = objectId;
        this.remoteObject = remoteObject;
        this.method = method;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public Object getRemoteObject() {
        return remoteObject;
    }

    public Method getMethod() {
        return method;
    }
}
