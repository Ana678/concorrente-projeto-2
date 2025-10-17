package middleware.component_model.remoting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            System.err.println("Erro ao fechar o socket do servidor: " + e.getMessage());
        } finally {
            pool.shutdown();
            System.out.println("Servidor Middleware parado.");
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Servidor Middleware iniciado na porta " + port);
            while (running) {
                Socket clientSocket = serverSocket.accept();
                pool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Erro crítico no ServerRequestHandler: " + e.getMessage());
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

            // GET /algum/path HTTP/1.1
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;

            String httpMethod = parts[0];
            String path = parts[1];
            System.out.println("Received " + httpMethod + " request for path: " + path);

            // cabeçalhos
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
                responseBody = invoker.invoke(path, requestBody);
            } catch (Exception e) {
                e.printStackTrace();
                responseBody = "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";

                if (e.getMessage() != null && e.getMessage().contains("No remote object found")) {
                    statusCode = 404;
                    statusMessage = "Not Found";
                } else {
                    statusCode = 500;
                    statusMessage = "Internal Server Error";
                }
            }

            // resposta
            writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
            writer.println("Content-Type: application/json; charset=UTF-8");
            writer.println("Content-Length: " + responseBody.getBytes(StandardCharsets.UTF_8).length);
            writer.println("Connection: close");
            writer.println(); // linha em branco
            writer.println(responseBody);
            writer.flush();

        } catch (IOException e) {
            System.err.println("Erro ao processar requisição do cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Silently ignore
            }
        }
    }
}
