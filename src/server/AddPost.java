package server;

import database.Database;

import java.io.*;
import java.net.*;
import java.sql.*;

public class AddPost {

    private static final int PORT = 6002;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("AddPostServer uruchomiony na porcie " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Błąd uruchamiania serwera: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Otrzymano zapytanie: " + inputLine);

                String response = handleRequest(inputLine);
                System.out.println("Wysyłana odpowiedź: " + response);
                out.println(response);

                if ("exit".equalsIgnoreCase(inputLine)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd obsługi klienta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String handleRequest(String request) {
        String[] parts = request.split("\\|");

        if (parts.length < 4 || !parts[0].contains("MessageType:AddPostRequest")) {
            return formatResponse("400", "Nieprawidłowe żądanie", null, getField(parts, "MessageId"));
        }

        String messageId = getField(parts, "MessageId");
        String userId = getField(parts, "UserId");
        String postContent = getField(parts, "Content");

        if (messageId == null || userId == null || postContent == null) {
            return formatResponse("400", "Brak wymaganych danych (MessageId, UserId lub Content)", null, messageId);
        }

        try {
            Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            return formatResponse("400", "Nieprawidłowe ID użytkownika", null, messageId);
        }

        if (postContent.trim().isEmpty()) {
            return formatResponse("400", "Treść postu nie może być pusta", null, messageId);
        }

        try (Connection conn = Database.getConnection()) {
            String insertPostQuery = "INSERT INTO posts (user_id, post) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertPostQuery)) {
                stmt.setInt(1, Integer.parseInt(userId));
                stmt.setString(2, postContent);
                stmt.executeUpdate();
                return formatResponse("200", "Post dodany pomyślnie", null, messageId);
            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas dodawania postu: " + e.getMessage());
            e.printStackTrace();
            return formatResponse("400", "Błąd podczas dodawania postu", null, messageId);
        }
    }

    private static String formatResponse(String status, String message, String userId, String messageId) {
        StringBuilder response = new StringBuilder("MessageType:AddPostResponse|");
        response.append("MessageId:").append(messageId).append("|");
        response.append("Status:").append(status).append("|");
        response.append("Message:").append(message);
        if (userId != null) {
            response.append("|User ID:").append(userId);
        }
        return response.toString();
    }

    private static String getField(String[] parts, String key) {
        for (String part : parts) {
            if (part.startsWith(key + ":")) {
                return part.split(":", 2)[1];
            }
        }
        return null;
    }
}
