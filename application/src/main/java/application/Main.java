package application;

import middleware.Middleware;
import middleware.util.Log;

import application.services.MessageFormatter;
import application.services.MessageStore;

public class Main {

    public static void main(String[] args) {
        int port = 8080;

        Log.info("Main", "Iniciando a plataforma de Middleware...");
        Middleware middleware = new Middleware(port);

        Log.info("Main", "Criando instâncias dos serviços...");
        MessageStore store = new MessageStore(10);
        MessageFormatter formatter = new MessageFormatter(store);

        Log.info("Main", "Registando MessageStore em /messagestore ...");
        middleware.register(store);

        Log.info("Main", "Registando MessageFormatter em /formatter ...");
        middleware.register(formatter);

        middleware.start();

        Log.info("Main", "\nServidor iniciado na porta %d", port);
    }
}
