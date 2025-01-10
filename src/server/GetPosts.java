package server;

import database.Database;

import java.io.*;
import java.net.*;
import java.sql.*;

public class GetPosts {

    private static final int PORT = 6003;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("GetPostsServer uruchomiony na porcie " + PORT);

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
        String messageId = getField(parts);

        if (parts.length < 2 || !parts[0].contains("MessageType:GetPostsRequest")) {
            return formatResponse("Nieprawidłowe żądanie", messageId);
        }

        try (Connection conn = Database.getConnection()) {
            String query = "SELECT u.username, p.post FROM posts p JOIN users u ON p.user_id = u.id ORDER BY p.upload_date DESC LIMIT 10";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();

                StringBuilder posts = new StringBuilder("MessageType:GetPostsResponse|MessageId:" + messageId + "|Status:200");

                int postCount = 0;
                while (rs.next()) {
                    postCount++;
                    posts.append("|User:").append(rs.getString("username")).append("-Post:").append(rs.getString("post"));
                }

                if (postCount > 0) {
                    return posts.toString();
                } else {
                    return posts.append("|Message:Brak postów").toString();
                }
            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas pobierania postów: " + e.getMessage());
            e.printStackTrace();
            return formatResponse("Błąd podczas pobierania postów", messageId);
        }

    }

    private static String formatResponse(String message, String messageId) {
        String response = "MessageType:GetPostsResponse|" + "MessageId:" + messageId + "|" +
                "Status:" + "400" + "|" +
                "Message:" + message;
        return response;
    }

    private static String getField(String[] parts) {
        for (String part : parts) {
            if (part.startsWith("MessageId" + ":")) {
                return part.split(":", 2)[1];
            }
        }
        return null;
    }
}
