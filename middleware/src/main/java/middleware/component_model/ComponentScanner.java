package middleware.component_model;

import java.lang.reflect.Method;

import middleware.component_model.annotations.DeleteMapping;
import middleware.component_model.annotations.GetMapping;
import middleware.component_model.annotations.PostMapping;
import middleware.component_model.annotations.PutMapping;
import middleware.component_model.annotations.RequestMapping;
import middleware.component_model.identification.AbsoluteObjectReference;
import middleware.component_model.identification.Lookup;
import middleware.lifecycle.LifecycleManager;
import middleware.lifecycle.annotations.LifecyclePolicy;
import middleware.lifecycle.annotations.LifecyclePolicyType;
import middleware.util.Log;


// Faz o scan de objetos à procura de anotações do modelo de componentes e regista-os no Lookup.
public class ComponentScanner {

    private final Lookup lookup;
    private final LifecycleManager lifecycleManager;

    public ComponentScanner(Lookup lookup, LifecycleManager lifecycleManager) {
        this.lookup = lookup;
        this.lifecycleManager = lifecycleManager;
    }

    public void register(Object instance) throws IllegalArgumentException {
        Class<?> classe = instance.getClass();

        if (!classe.isAnnotationPresent(RequestMapping.class)) {
            throw new IllegalArgumentException("O objeto fornecido não é um componente válido (falta @RequestMapping).");
        }

        // Lifecycle Policy
        LifecyclePolicyType policy = LifecyclePolicyType.PER_REQUEST;
        if (classe.isAnnotationPresent(LifecyclePolicy.class)) {
            policy = classe.getAnnotation(LifecyclePolicy.class).value();
            Log.info("ComponentScanner", "Política de Ciclo de Vida detectada para %s: %s", classe.getSimpleName(), policy);
        } else {
            Log.info("ComponentScanner", "Nenhuma Política de Ciclo de Vida definida para %s. Usando padrão: %s", classe.getSimpleName(), policy);
        }

        // Se for STATIC_INSTANCE, cacheia a instância no LifecycleManager
        if (policy == LifecyclePolicyType.STATIC_INSTANCE) {
            lifecycleManager.cacheStaticInstance(classe, instance);
            Log.info("ComponentScanner", "Instância STATIC_INSTANCE %s colocada no cache do LifecycleManager.", classe.getSimpleName());
        }

        RequestMapping classMapping = classe.getAnnotation(RequestMapping.class);
        String basePath = classMapping.path(); // Ex: "/messagestore"

        Log.info("ComponentScanner", "Registando Controller: %s", basePath);

        for (Method method : classe.getDeclaredMethods()) {
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

                // Sempre criamos a AbsoluteObjectReference, independentemente da política
                AbsoluteObjectReference absoluteReference = new AbsoluteObjectReference(basePath, classe, method, policy);

                 // regista no lookup
                lookup.bind(lookupKey, absoluteReference);
            }
        }
    }
}
