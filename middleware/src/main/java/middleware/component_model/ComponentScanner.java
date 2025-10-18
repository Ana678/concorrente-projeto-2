package middleware.component_model;

import middleware.component_model.annotations.*;
import middleware.component_model.identification.*;

import java.lang.reflect.Method;


// Faz o scan de objetos à procura de anotações do modelo de componentes e regista-os no Lookup.
public class ComponentScanner {

    private final Lookup lookup;

    public ComponentScanner(Lookup lookup) {
        this.lookup = lookup;
    }

    public void register(Object instance) throws IllegalArgumentException {
        Class<?> clazz = instance.getClass();

        if (!clazz.isAnnotationPresent(RequestMapping.class)) {
            throw new IllegalArgumentException("O objeto fornecido não é um componente válido (falta @RequestMapping).");
        }

        RequestMapping classMapping = clazz.getAnnotation(RequestMapping.class);
        String basePath = classMapping.path(); // Ex: "/messagestore"
        ObjectId objectId = new ObjectId(basePath);

        System.out.println("Registando Controller: " + basePath);

        for (Method method : clazz.getDeclaredMethods()) {
            String httpMethod = null;
            String methodPath = null;

            if (method.isAnnotationPresent(GetMapping.class)) {
                httpMethod = "GET";
                methodPath = method.getAnnotation(GetMapping.class).path();
            } else if (method.isAnnotationPresent(PostMapping.class)) {
                httpMethod = "POST";
                methodPath = method.getAnnotation(PostMapping.class).path();
            } else if (method.isAnnotationPresent(PutMapping.class)) {
                httpMethod = "PUT";
                methodPath = method.getAnnotation(PutMapping.class).path();
            } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                httpMethod = "DELETE";
                methodPath = method.getAnnotation(DeleteMapping.class).path();
            }

            if (httpMethod != null) {
                // rota completa Ex.: /messagestore/createGroup
                String fullPath = basePath + methodPath;

                // monta a CHAVE ÚNICA do lookup. Ex.: "POST:/messagestore/createGroup"
                String lookupKey = httpMethod + ":" + fullPath;

                AbsoluteObjectReference absoluteReference = new AbsoluteObjectReference(objectId, instance, method);

                 // regista no lookup
                lookup.bind(lookupKey, absoluteReference);
            }
        }
    }
}
