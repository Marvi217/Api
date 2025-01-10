package server;

import database.Database;

import java.io.*;
import java.net.*;
import java.sql.*;

public class Login {

    private static final int PORT = 6001;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("LoginServer uruchomiony na porcie " + PORT);

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

        if (parts.length < 4 || !parts[0].contains("MessageType:LoginRequest")) {
            return formatResponse("400", "Nieprawidłowe żądanie", null, getField(parts, "MessageId"));
        }

        String username = getField(parts, "Login");
        String password = getField(parts, "Password");
        String messageId = getField(parts, "MessageId");

        if (username == null || password == null) {
            return formatResponse("400", "Nieprawidłowe dane logowania", null, messageId);
        }

        try (Connection conn = Database.getConnection()) {
            String query = "SELECT id, password FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String storedPassword = rs.getString("password");

                    if (storedPassword.equals(password)) {
                        String userId = rs.getString("id");
                        return formatResponse("200", "Logowanie zakończone sukcesem", userId, messageId);
                    } else {
                        return formatResponse("400", "Błędne hasło", null, messageId);
                    }
                } else {
                    return formatResponse("400", "Użytkownik nie znaleziony", null, messageId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas logowania: " + e.getMessage());
            e.printStackTrace();
            return formatResponse("400", "Błąd podczas logowania użytkownika", null, messageId);
        }
    }

    private static String formatResponse(String status, String message, String userId, String messageId) {
        StringBuilder response = new StringBuilder("MessageType:LoginResponse|");
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
