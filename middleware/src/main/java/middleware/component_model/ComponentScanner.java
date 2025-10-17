package middleware.component_model;

import java.lang.reflect.Method;

import middleware.component_model.annotations.RemoteMethod;
import middleware.component_model.annotations.RemoteObject;
import middleware.component_model.identification.AbsoluteObjectReference;
import middleware.component_model.identification.Lookup;
import middleware.component_model.identification.ObjectId;

public class ComponentScanner {

    private final Lookup lookup;

    public ComponentScanner(Lookup lookup) {
        this.lookup = lookup;
    }

    public void register(Object instance) throws IllegalArgumentException {
        Class<?> classe = instance.getClass();

        if (!classe.isAnnotationPresent(RemoteObject.class)) {
            throw new IllegalArgumentException("O objeto fornecido não é um @RemoteObject válido.");
        }

        RemoteObject remoteObjectAnnotation = classe.getAnnotation(RemoteObject.class);
        String basePath = remoteObjectAnnotation.name();
        ObjectId objectId = new ObjectId(basePath);

        System.out.println("Registrando Remote Object: " + basePath);

        for (Method method : classe.getDeclaredMethods()) {

            if (method.isAnnotationPresent(RemoteMethod.class)) {
                RemoteMethod remoteMethodAnnotation = method.getAnnotation(RemoteMethod.class);
                String methodPath = remoteMethodAnnotation.name();

                // Monta a rota
                String fullPath = "/" + basePath + "/" + methodPath;

                AbsoluteObjectReference absoluteReference = new AbsoluteObjectReference(objectId, instance, method);

                lookup.bind(fullPath, absoluteReference);
            }
        }
    }
}
