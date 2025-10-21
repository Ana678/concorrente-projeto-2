package middleware.component_model.remoting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.JsonProcessingException;
import middleware.util.Log;


public class ServerRequestHandler implements Runnable {

    private final Invoker invoker;
    private final int port;
    private volatile boolean running = true;
    private final ExecutorService pool;
    private ServerSocket serverSocket;

    public ServerRequestHandler(Invoker invoker, int port) {
        this.invoker = invoker;
        this.port = port;
        this.pool = Executors.newCachedThreadPool();
    }

    public void start() {
        new Thread(this).start();
    }

    public void stop() {
        this.running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.error("ServerRequestHandler", "Erro ao fechar o server socket: " + e.getMessage(), e);
        } finally {
            pool.shutdown();
            Log.info("ServerRequestHandler", "Middleware server parado.");
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            Log.info("ServerRequestHandler", "Middleware server iniciado na porta %d", port);
            while (running) {
                Socket clientSocket = serverSocket.accept();
                pool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                Log.error("ServerRequestHandler", "Erro Crítico no ServerRequestHandler: " + e.getMessage(), e);
            }
        } finally {
            if (!pool.isShutdown()) {
                pool.shutdownNow();
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            // "POST /messagestore/createGroup HTTP/1.1"
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;

                String httpMethod = parts[0];
                String path = parts[1];
                Log.info("ServerRequestHandler", "Received %s request for path: %s", httpMethod, path);

            // headers
            String headerLine;
            int contentLength = 0;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                }
            }

            // body
            String requestBody = "";
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                reader.read(bodyChars, 0, contentLength);
                requestBody = new String(bodyChars);
            }

            String responseBody;
            int statusCode = 200;
            String statusMessage = "OK";

            try {
                responseBody = invoker.invoke(httpMethod, path, requestBody);
                } catch (RouteNotFoundException e) {
                statusCode = 404;
                statusMessage = "Não Encontrado";
                responseBody = "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
                Log.error("ServerRequestHandler", "Erro do Cliente [404]: " + e.getMessage(), e);

            } catch (JsonProcessingException e) {
                statusCode = 400;
                statusMessage = "Requisição Inválida";
                responseBody = "{\"error\": \"Formato JSON ou campos inválidos.\", \"details\": \"" + e.getMessage().replace("\"", "'") + "\"}";
                Log.error("ServerRequestHandler", "Erro do Cliente [400]: Requisição JSON inválida. " + e.getMessage(), e);

            } catch (InvocationTargetException e) {

                statusCode = 500;
                statusMessage = "Erro Interno do Servidor";
                responseBody = "{\"error\": \"Erro ao executar a lógica de negócio.\", \"details\": \"" + e.getTargetException().getMessage().replace("\"", "'") + "\"}";
                Log.error("ServerRequestHandler", "Erro do Servidor [500]: Exceção no método remoto. Detalhes:", e.getTargetException());

            } catch (Exception e) {

                statusCode = 500;
                statusMessage = "Erro Interno do Servidor";
                responseBody = "{\"error\": \"Ocorreu um erro inesperado.\", \"details\": \"" + e.getMessage().replace("\"", "'") + "\"}";
                Log.error("ServerRequestHandler", "Erro no Middleware [500]: " + e.getMessage(), e);
            }

            // resposta
            writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
            writer.println("Content-Type: application/json; charset=UTF-8");
            writer.println("Content-Length: " + responseBody.getBytes(StandardCharsets.UTF_8).length);
            writer.println("Connection: close");
            writer.println(); // Linha em branco
            writer.println(responseBody);
            writer.flush();

        } catch (IOException e) {
            Log.error("ServerRequestHandler", "Erro ao lidar com a requisição do cliente: " + e.getMessage(), e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Log.error("ServerRequestHandler", "Erro ao fechar o socket do cliente: " + e.getMessage(), e);
            }
        }
    }
}
