package application;

import middleware.Middleware;

import application.services.MessageFormatter;
import application.services.MessageStore;

public class Main {

    public static void main(String[] args) {
        int port = 8080;

        System.out.println("Iniciando a plataforma de Middleware...");
        Middleware middleware = new Middleware(port);

        System.out.println("Criando instâncias dos serviços...");
        MessageStore store = new MessageStore(10); 
        MessageFormatter formatter = new MessageFormatter(store);

        System.out.println("Registando MessageStore em /messagestore ...");
        middleware.register(store);

        System.out.println("Registando MessageFormatter em /formatter ...");
        middleware.register(formatter);

        middleware.start();

        System.out.println("\nAPLICAÇÃO PRONTA! O servidor está a escutar na porta " + port);
        System.out.println("Pronto para receber testes do JMeter.");
        System.out.println("Pressione Ctrl+C para encerrar.");
    }
}
