package middleware;

import middleware.component_model.ComponentScanner;
import middleware.component_model.identification.Lookup;
import middleware.component_model.remoting.Invoker;
import middleware.component_model.remoting.ServerRequestHandler;

public class Middleware {

    private final ServerRequestHandler server;
    private final ComponentScanner scanner;

    public Middleware(int port) {

        System.out.println("Inicializando Middleware...");

        Lookup lookup = new Lookup();
        Invoker invoker = new Invoker(lookup);

        this.scanner = new ComponentScanner(lookup);
        this.server = new ServerRequestHandler(invoker, port);

        System.out.println("Componentes do Middleware inicializados.");
    }

    public void register(Object remoteObject) {
        try {
            scanner.register(remoteObject);
        } catch (IllegalArgumentException e) {
            System.err.println("Falha ao registrar objeto: " + e.getMessage());
        }
    }

    public void start() {
        server.start();
    }
    
    public void stop() {
        server.stop();
    }
}
