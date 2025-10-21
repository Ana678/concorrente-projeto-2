package middleware.lifecycle.pooling;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import middleware.util.Log;

/**
 * Um pool de objetos simplificado para reutilização de instâncias.
 * @param <T> O tipo de objeto que o pool irá gerir.
 */
public class ObjectPool<T> {

    // Fila não-bloqueante e thread-safe
    private final ConcurrentLinkedQueue<T> pool;
    private final Supplier<T> objectFactory;
    private final int maxSize;

    /**
     * Cria um novo ObjectPool simplificado.
     *
     * @param preInstances       Número de instâncias para pré-carregar
     * @param maxSize       Número máximo de instâncias para *manter* no pool.
     * @param objectFactory O Supplier que sabe como criar novas instâncias de T.
     */
    public ObjectPool(int preInstances, int maxSize, Supplier<T> objectFactory) {
        if (preInstances < 0 || maxSize <= 0 || preInstances > maxSize) {
            throw new IllegalArgumentException("Parâmetros de tamanho do pool inválidos.");
        }
        this.pool = new ConcurrentLinkedQueue<>();
        this.objectFactory = objectFactory;
        this.maxSize = maxSize;

        // Pré-carrega o pool com o número mínimo
        for (int i = 0; i < preInstances; i++) {
            pool.offer(createObject()); // Usa o método auxiliar
        }
        Log.info("ObjectPool", "ObjectPool inicializado com %d instâncias (maxSize=%d).", pool.size(), maxSize);
    }

    /**
     * Pega emprestado um objeto do pool.
     * Se o pool estiver vazio, cria um novo objeto. Não bloqueia.
     *
     * @return Uma instância de T.
     * @throws RuntimeException Se a fábrica falhar ao criar um novo objeto.
     */
    public T borrowObject() {
        T object = pool.poll(); // Tenta pegar um objeto existente

        if (object == null) {
            // Pool vazio, cria um novo objeto
            Log.info("ObjectPool", "Pool vazio, criando novo objeto...");
            object = createObject();
        } else {
            Log.info("ObjectPool", "Objeto pego do pool. Tamanho restante: %d", pool.size());
        }
        return object;
    }

    /**
     * Devolve um objeto ao pool para reutilização.
     * Se o pool estiver cheio, o objeto é descartado.
     *
     * @param object O objeto a ser devolvido.
     */
    public void returnObject(T object) {
        if (object == null) {
            return;
        }

        if (pool.size() < maxSize) {
            boolean added = pool.offer(object);
                if (added) {
                Log.info("ObjectPool", "Objeto devolvido ao pool. Tamanho atual: %d", pool.size());
            }
        } else {
            Log.info("ObjectPool", "Pool cheio (maxSize=%d). Descartando objeto (Instance: %d)", maxSize, object.hashCode());
        }
    }


    public void close() {
    Log.info("ObjectPool", "Fechando ObjectPool. Limpando %d objetos.", pool.size());
    pool.clear();
    }

    private T createObject() {
        try {
            return objectFactory.get();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criar objeto pela fábrica do pool.", e);
        }
    }
}
