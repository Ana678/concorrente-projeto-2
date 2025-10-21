package middleware;

import middleware.component_model.ComponentScanner;
import middleware.component_model.identification.Lookup;
import middleware.component_model.remoting.Invoker;
import middleware.component_model.remoting.ServerRequestHandler;
import middleware.lifecycle.LifecycleManager;
import middleware.util.Log;

public class Middleware {

    private final ServerRequestHandler server;
    private final ComponentScanner scanner;
    private final LifecycleManager lifecycleManager;

    public Middleware(int port) {

    Log.info("Middleware", "Inicializando Middleware...");

        this.lifecycleManager = new LifecycleManager();

        Lookup lookup = new Lookup();
        Invoker invoker = new Invoker(lookup, lifecycleManager);

        this.scanner = new ComponentScanner(lookup, lifecycleManager);
        this.server = new ServerRequestHandler(invoker, port);

    Log.info("Middleware", "Componentes do Middleware inicializados.");
    }

    public void register(Object componentInstance) {
        if (componentInstance == null) return;
        Class<?> componentClass = componentInstance.getClass();

        try {
            Log.info("Middleware", "Middleware registrando (via Scanner): " + componentClass.getSimpleName());
            scanner.register(componentInstance);

        } catch (IllegalArgumentException e) {
            Log.error("Middleware", "Falha ao registrar objeto: " + e.getMessage(), e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop();
        if (lifecycleManager != null) {
            lifecycleManager.shutdownPools();
        }
    }
}
