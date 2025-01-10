package server;

import database.Database;
import java.io.*;
import java.net.*;
import java.sql.*;

public class RegistrationServer {

    private static final int PORT = 6000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("RegistrationServer uruchomiony na porcie " + PORT);

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
        if (!request.startsWith("MessageType:RegistrationRequest|")) {
            return "MessageType:RegistrationResponse|MessageId:" + getMessageId(request) + "|Status:400|Message:Nieprawidłowe żądanie";
        }

        String username = getField(request, "Login");
        String password = getField(request, "Password");

        if (username == null || password == null) {
            return "MessageType:RegistrationResponse|MessageId:" + getMessageId(request) + "|Status:400|Message:Brak wymaganych danych (Login lub Password)";
        }

        try (Connection conn = Database.getConnection()) {
            String checkUserQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) {
                    return "MessageType:RegistrationResponse|MessageId:" + getMessageId(request) + "|Status:400|Message:Użytkownik już istnieje";
                }
            }
            String insertUserQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertUserQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.executeUpdate();
                return "MessageType:RegistrationResponse|MessageId:" + getMessageId(request) + "|Status:200|Message:Rejestracja zakończona sukcesem";
            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas rejestracji użytkownika: " + e.getMessage());
            e.printStackTrace();
            return "MessageType:RegistrationResponse|MessageId:" + getMessageId(request) + "|Status:400|Message:Błąd podczas rejestracji użytkownika";
        }
    }
    private static String getMessageId(String request) {
        String[] parts = request.split("\\|");
        for (String part : parts) {
            if (part.startsWith("MessageId:")) {
                return part.split(":", 2)[1];
            }
        }
        return "0";
    }


    private static String getField(String request, String key) {
        String[] parts = request.split("\\|");
        for (String part : parts) {
            if (part.startsWith(key + ":")) {
                return part.split(":", 2)[1];
            }
        }
        return null;
    }
}
