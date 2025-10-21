package middleware.lifecycle;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import middleware.component_model.identification.AbsoluteObjectReference;
import middleware.lifecycle.annotations.LifecyclePolicyType;
import middleware.lifecycle.annotations.Pooled;
import middleware.lifecycle.pooling.ObjectPool;
import middleware.util.Log;

public class LifecycleManager {

    private final Map<Class<?>, Object> staticInstanceCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, ObjectPool<?>> perRequestPools = new ConcurrentHashMap<>();

    private static final int DEFAULT_POOL_MIN_IDLE = 1;
    private static final int DEFAULT_POOL_MAX_SIZE = 10;

    public LifecycleManager() {
        Log.info("LifecycleManager", "LifecycleManager inicializado.");
    }

    public void cacheStaticInstance(Class<?> componentClass, Object instance) {
        if (componentClass != null && instance != null) {
            // o putIfAbsent evita sobrescrever uma instância já existente
            staticInstanceCache.putIfAbsent(componentClass, instance);
        }
    }

    /**
     *
     * @param absoluteReference A referência absoluta do objeto contendo a política e a classe/instância.
     * @return A instância do objeto a ser usada para a invocação.
     * @throws Exception Se ocorrer um erro ao criar uma instância (para PerRequest).
     */
    public Object getInstance(AbsoluteObjectReference absoluteReference) throws Exception {
        LifecyclePolicyType policy = absoluteReference.getPolicyType();
        Class<?> componentClass = absoluteReference.getRemoteObjectClass();

        if (policy == LifecyclePolicyType.STATIC_INSTANCE) {
            Log.info("LifecycleManager", "Retornando instância STATIC_INSTANCE para: " + componentClass.getSimpleName());
            return staticInstanceCache.computeIfAbsent(componentClass, classe -> {
                try {
                    Log.info("LifecycleManager", "Ausência no cache para STATIC_INSTANCE " + classe.getSimpleName() + ". Criando (lazy) ...");
                    return createInstance(classe);
                } catch (Exception e) {
                    throw new RuntimeException("Falha ao criar instância lazy para " + classe.getName(), e);
                }
            });

        } else if (policy == LifecyclePolicyType.PER_REQUEST) {
        if (componentClass.isAnnotationPresent(Pooled.class)) {
            Log.info("LifecycleManager", "Requisitando instância PER_REQUEST  anotado com @Pooled para :" + componentClass.getSimpleName());

                    ObjectPool<?> pool = perRequestPools.computeIfAbsent(componentClass, classe -> {
                        Log.info("LifecycleManager", "Criando novo ObjectPool para a classe: " + classe.getSimpleName());
                        Supplier<?> factory = () -> {
                            try {
                                return createInstance(classe);
                            } catch (Exception e) {
                                throw new RuntimeException("Falha ao criar instância no ObjectPool para " + classe.getName(), e);
                            }
                        };
                        return new ObjectPool<>(DEFAULT_POOL_MIN_IDLE, DEFAULT_POOL_MAX_SIZE, factory);
                    });

                    try {
                        Object instance = pool.borrowObject();

                        Log.info("LifecycleManager", "Instância obtida do pool para: " + componentClass.getSimpleName() + " (Instance: " + instance.hashCode() + ")");
                        return instance;
                    } catch (RuntimeException e) {
                        throw new Exception("Falha ao criar instância (via pool) para: " + componentClass.getSimpleName(), e);
                    }
                } else {
                    Log.info("LifecycleManager", "Requisitando instância PER_REQUEST para: " + componentClass.getSimpleName());
                    return createInstance(componentClass);
                }
        } else {
            throw new UnsupportedOperationException("Política de ciclo de vida não suportada: " + policy);
        }
    }

    private Object createInstance(Class<?> componentClass) throws Exception {
        Constructor<?> bestConstructor = null;
        Object[] argsForBestConstructor = null;

        // Itera por todos os construtores públicos da classe
        for (Constructor<?> constructor : componentClass.getConstructors()) {
            Parameter[] parameters = constructor.getParameters();
            List<Object> args = new ArrayList<>();
            boolean canSatisfy = true;

            // Tenta encontrar um Singleton registrado para cada parâmetro do construtor
            for (Parameter param : parameters) {

                Object staticDependency = findCachedStaticInstanceAssignableTo(param.getType());
                if (staticDependency != null) {
                    args.add(staticDependency);
                } else {
                    canSatisfy = false;
                    break;
                }
            }

            // Se conseguiu satisfazer todos os parâmetros do construtor
            if (canSatisfy) {
                if (bestConstructor == null || parameters.length > bestConstructor.getParameterCount()) {
                    bestConstructor = constructor;
                    argsForBestConstructor = args.toArray();
                }
            }
        }

        try {
            if (bestConstructor != null) {
                 Log.info("LifecycleManager", "Invocando construtor de " + componentClass.getSimpleName() + " com " + bestConstructor.getParameterCount() + " argumento(s) (STATIC_INSTANCE injetados).");
                return bestConstructor.newInstance(argsForBestConstructor);
            } else {
                 Log.info("LifecycleManager", "Nenhum construtor com STATIC_INSTANCEs compatível encontrado para " + componentClass.getSimpleName() + ". Tentando construtor padrão.");
                return componentClass.getDeclaredConstructor().newInstance();
            }
        } catch (NoSuchMethodException e) {
             throw new Exception("Componente " + componentClass.getName() + " não possui construtor compatível (nem padrão, nem com STATIC_INSTANCEs injetáveis).", e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new Exception("Falha ao instanciar componente " + componentClass.getName() + ".", e);
        }
    }

    private Object findCachedStaticInstanceAssignableTo(Class<?> requiredType) {
        for (Map.Entry<Class<?>, Object> entry : staticInstanceCache.entrySet()) {
            if (requiredType.isAssignableFrom(entry.getKey())) {
                 Log.info("LifecycleManager", "Encontrado STATIC_INSTANCE em cache (" + entry.getKey().getSimpleName() + ") para o tipo requisitado (" + requiredType.getSimpleName() + ")");
                return entry.getValue();
            }
        }
        Log.info("LifecycleManager", "Nenhum STATIC_INSTANCE em cache compatível encontrado para o tipo requisitado (" + requiredType.getSimpleName() + ")");
        return null;
    }

    /**
     * Devolve um objeto ao pool apropriado. Chamado pelo Invoker.
     * @param instance O objeto a ser devolvido.
     */
     public void returnInstanceToPool(Object instance, LifecyclePolicyType policy) {
        if (instance == null || policy != LifecyclePolicyType.PER_REQUEST) {
             return;
         }

         Class<?> componentClass = instance.getClass();

        if (componentClass.isAnnotationPresent(Pooled.class)) {
            ObjectPool<?> pool = perRequestPools.get(componentClass); // Obtém o ObjectPool<SpecificType> como ObjectPool<?>

            if (pool != null) {
                try {
                    Log.info("LifecycleManager", "Devolvendo instância ao pool: " + componentClass.getSimpleName() + " (Instance: " + instance.hashCode() + ")");
                    // O cast é necessário para chamar returnObject sem warnings de genéricos
                    @SuppressWarnings("unchecked")
                    ObjectPool<Object> typedPool = (ObjectPool<Object>) pool;
                    typedPool.returnObject(instance);
                } catch (Exception e) {
                    Log.error("LifecycleManager", "Erro ao devolver objeto ao pool para classe " + componentClass.getName() + ": " + e.getMessage(), e);
                }
            } else {
                Log.error("LifecycleManager", "WARN: Tentativa de devolver instância ao pool, mas nenhum pool encontrado para a classe: " + componentClass.getName());
            }
        } else {
             Log.info("LifecycleManager", "Instância PER_REQUEST descartada: " + componentClass.getSimpleName() + " (Instance: " + instance.hashCode() + ")");
         }
     }

     /**
      * Limpa todos os pools. Pode ser chamado ao parar o middleware.
      */
     public void shutdownPools() {
         Log.info("LifecycleManager", "Encerrando pools de objetos...");
         perRequestPools.values().forEach(ObjectPool::close);
         perRequestPools.clear();
         staticInstanceCache.clear(); // Limpa também o cache de singletons lazy
         Log.info("LifecycleManager", "Pools encerrados.");
     }
}
