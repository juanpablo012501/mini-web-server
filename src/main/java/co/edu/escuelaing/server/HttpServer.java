package co.edu.escuelaing.server;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class HttpServer {

    private static final String STATIC_ROOT = "/webroot";

    public static void main(String[] args) throws IOException {
        String portEnv = System.getenv("PORT");
        int port = (portEnv == null || portEnv.isBlank()) ? 8080 : Integer.parseInt(portEnv);

        System.out.println("Server starting on port " + port);
        System.out.println("Open: http://localhost:" + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket);
                } catch (Exception e) {
                    System.err.println("Error handling request: " + e.getMessage());
                }
            }
        }
    }

    private static void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();

        // Leer línea de petición: "GET /path?query HTTP/1.1"
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            sendError(out, 400, "Bad Request");
            return;
        }

        System.out.println(">>> " + requestLine);

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            sendError(out, 400, "Bad Request");
            return;
        }

        String method = parts[0];
        String fullPath = parts[1];

        // Solo aceptamos GET
        if (!method.equals("GET")) {
            sendError(out, 405, "Method Not Allowed");
            return;
        }

        // Separar path y query string
        String path;
        String queryString = "";
        int queryIndex = fullPath.indexOf('?');
        if (queryIndex >= 0) {
            path = fullPath.substring(0, queryIndex);
            queryString = fullPath.substring(queryIndex + 1);
        } else {
            path = fullPath;
        }

        // Ruta raíz
        if (path.equals("/")) {
            path = "/index.html";
        }

        // ── Rutas dinámicas hardcodeadas ──
        if (path.equals("/api/greeting")) {
            handleGreeting(out, queryString);
        } else if (path.equals("/api/square")) {
            handleSquare(out, queryString);
        } else if (path.equals("/api/time")) {
            handleTime(out);
        } else if (path.equals("/api/health")) {
            handleHealth(out);
        } else {
            serveStaticFile(out, path);
        }
    }

    // ── Servicios dinámicos ──

    private static void handleGreeting(OutputStream out, String queryString) throws IOException {
        Map<String, String> params = parseQueryString(queryString);
        String name = params.get("name");

        if (name == null || name.isBlank()) {
            sendError(out, 400, "Missing parameter: name");
            return;
        }

        name = name.replace("\"", "\\\"");
        String json = "{\"greeting\": \"Hello, " + name + "!\"}";
        sendJson(out, 200, json);
    }

    private static void handleSquare(OutputStream out, String queryString) throws IOException {
        Map<String, String> params = parseQueryString(queryString);
        String valueStr = params.get("value");

        if (valueStr == null || valueStr.isBlank()) {
            sendError(out, 400, "Missing parameter: value");
            return;
        }

        try {
            double value = Double.parseDouble(valueStr);
            double square = value * value;
            String json = "{\"input\": " + value + ", \"square\": " + square + "}";
            sendJson(out, 200, json);
        } catch (NumberFormatException e) {
            sendError(out, 400, "Invalid number: " + valueStr);
        }
    }

    private static void handleTime(OutputStream out) throws IOException {
        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String json = "{\"serverTime\": \"" + now + "\"}";
        sendJson(out, 200, json);
    }

    private static void handleHealth(OutputStream out) throws IOException {
        sendJson(out, 200, "{\"status\": \"ok\"}");
    }

    // ── Archivos estáticos ──

    private static void serveStaticFile(OutputStream out, String path) throws IOException {
        // Seguridad: rechazar path traversal
        if (path.contains("..")) {
            sendError(out, 403, "Forbidden");
            return;
        }

        String resourcePath = STATIC_ROOT + path;
        InputStream fileStream = HttpServer.class.getResourceAsStream(resourcePath);

        if (fileStream == null) {
            sendError(out, 404, "Not Found: " + path);
            return;
        }

        byte[] fileBytes = fileStream.readAllBytes();
        String contentType = getContentType(path);

        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + fileBytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        out.write(headers.getBytes());
        out.write(fileBytes);
        out.flush();
    }

    // ── Utilidades ──

    private static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isBlank()) return params;

        for (String pair : queryString.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    String key = URLDecoder.decode(kv[0], "UTF-8");
                    String value = URLDecoder.decode(kv[1], "UTF-8");
                    params.put(key, value);
                } catch (Exception e) {
                    // par inválido, se ignora
                }
            }
        }
        return params;
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".ico"))  return "image/x-icon";
        if (path.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    private static void sendJson(OutputStream out, int status, String json) throws IOException {
        byte[] body = json.getBytes("UTF-8");
        String headers = "HTTP/1.1 " + status + " OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(headers.getBytes());
        out.write(body);
        out.flush();
    }

    private static void sendError(OutputStream out, int code, String message) throws IOException {
        byte[] body = (code + " " + message).getBytes("UTF-8");
        String headers = "HTTP/1.1 " + code + " " + message + "\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(headers.getBytes());
        out.write(body);
        out.flush();
    }
}