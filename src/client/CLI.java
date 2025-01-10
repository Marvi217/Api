package client;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class CLI {
    private static int messageIdCounter = 0;

    public static void main(String[] args) {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        String userId = null;

        try (Socket socket = new Socket("localhost", 7000);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            while (true) {
                if (userId == null) {
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    System.out.println("Wybierz opcję:");
                    System.out.println("1: Rejestracja");
                    System.out.println("2: Logowanie");
                    System.out.println("0: Wyjście");
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    String command = console.readLine();

                    switch (command) {
                        case "1":
                            handleRegistration(console, out, in);
                            break;

                        case "2":
                            userId = handleLogin(console, out, in);
                            break;

                        case "0":
                            System.out.println("Zakończono połączenie.");
                            return;

                        default:
                            System.out.println("Nieznane polecenie.");
                    }
                } else {
                    System.out.println("Witaj, wybierz opcję:");
                    System.out.println("1: Wyloguj");
                    System.out.println("2: Dodaj post");
                    System.out.println("3: Wyświetl ostatnie 10 postów");
                    System.out.println("4: Wyślij plik");
                    System.out.println("5: Pobierz plik");
                    System.out.println("0: Wyjście");

                    String command = console.readLine();
                    switch (command) {
                        case "1":
                            System.out.println("Wylogowano pomyślnie.");
                            userId = null;
                            break;

                        case "2":
                            handleAddPost(console, userId, out, in);
                            break;

                        case "3":
                            handleGetPosts(out, in);
                            break;

                        case "4":
                            handleUploadFile(console, userId, out, in);
                            break;

                        case "5":
                            handleDownloadFile(console, userId, out, in);
                            break;

                        case "0":
                            System.out.println("Zakończono połączenie.");
                            return;

                        default:
                            System.out.println("Nieznane polecenie.");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleRegistration(BufferedReader console, PrintWriter out, BufferedReader in) throws IOException {
        int messageId = generateMessageId();
        System.out.println("Podaj nazwę użytkownika:");
        String username = console.readLine();
        System.out.println("Podaj hasło:");
        String password = console.readLine();

        String request = "MessageType:RegistrationRequest|" + "MessageId:" + messageId + "|" + "Login:" + username + "|" + "Password:" + password;
        String response = sendRequest(out, in, request);

        String[] parts = response.split("\\|");
        String status = getField(parts, "Status");

        if ("200".equals(status)) {
            System.out.println("Rejestracja zakończona sukcesem.");
        } else {
            String message = getField(parts, "Message");
            System.out.println("Błąd rejestracji: " + message);
        }
    }

    private static String handleLogin(BufferedReader console, PrintWriter out, BufferedReader in) throws IOException {
        int messageId = generateMessageId();
        System.out.println("Podaj nazwę użytkownika:");
        String username = console.readLine();
        System.out.println("Podaj hasło:");
        String password = console.readLine();

        String request = "MessageType:LoginRequest|MessageId:" + messageId + "|Login:" + username + "|Password:" + password;
        String response = sendRequest(out, in, request);

        if (response.startsWith("MessageType:LoginResponse|")) {
            String[] parts = response.split("\\|");
            String responseMessageId = getField(parts, "MessageId");
            String status = getField(parts, "Status");
            String message = getField(parts, "Message");
            String userId = getField(parts, "User ID");

            if (!String.valueOf(messageId).equals(responseMessageId)) {
                System.out.println("Ostrzeżenie: Otrzymano odpowiedź z niezgodnym MessageId");
            }

            if ("200".equals(status)) {
                System.out.println("Zalogowano pomyślnie, ID użytkownika: " + userId);
                return userId;
            } else {
                System.out.println("Błąd logowania: " + message);
                return null;
            }
        } else {
            System.out.println("Niepoprawny format odpowiedzi: " + response);
            return null;
        }
    }

    private static void handleAddPost(BufferedReader console, String userId, PrintWriter out, BufferedReader in) throws IOException {
        int messageId = generateMessageId();
        System.out.println("Napisz swój post:");
        String postContent = console.readLine();

        String request = "MessageType:AddPostRequest|" + "MessageId:" + messageId + "|" + "UserId:" + userId + "|" + "Content:" + postContent;
        String response = sendRequest(out, in, request);

        if (response != null) {
            String[] responseParts = response.split("\\|");

            String status = getField(responseParts, "Status");
            String message = getField(responseParts, "Message");

            if ("200".equals(status)) {
                System.out.println("Post dodany pomyślnie.");
            } else {
                System.out.println("Błąd dodawania postu: " + (message != null ? message : "Brak informacji o błędzie"));
            }
        } else {
            System.out.println("Brak odpowiedzi od serwera.");
        }
    }

    private static void handleGetPosts(PrintWriter out, BufferedReader in) throws IOException {
        int messageId = generateMessageId();
        String request = "MessageType:GetPostsRequest|MessageId:" + messageId;
        String response = sendRequest(out, in, request);

        if (response != null) {
            String[] responseParts = response.split("\\|");

            String status = getField(responseParts, "Status");
            String message = getField(responseParts, "Message");

            if ("200".equals(status)) {
                System.out.println("Ostatnie 10 postów:");

                for (int i = 3; i < responseParts.length; i++) {
                    String part = responseParts[i];

                    if (part.startsWith("User:") && part.contains("-Post:")) {
                        String[] postParts = part.split("-Post:");

                        String user = postParts[0].substring("User:".length());
                        String post = postParts[1];

                        System.out.println((i - 2) + ". " + user + ": " + post);
                    }
                }
            } else {
                System.out.println("Błąd pobierania postów: " + (message != null ? message : "Brak informacji o błędzie"));
            }
        } else {
            System.out.println("Brak odpowiedzi od serwera.");
        }
    }

    private static void handleUploadFile(BufferedReader console, String userId, PrintWriter out, BufferedReader in) throws IOException {
        System.out.println("Podaj ścieżkę do pliku do wysłania:");
        String filePath = console.readLine();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Plik nie istnieje.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            long fileSize = file.length();
            System.out.println("Plik w trakcie wysyłania (" + fileSize + " bajtów)...");

            byte[] buffer = new byte[512];
            int bytesRead;
            long bytesSent = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                int messageId = generateMessageId();
                String encodedData = Base64.getEncoder().encodeToString(buffer);
                String request = "MessageType:UploadFileRequest|MessageId:" + messageId + "|UserId:" + userId + "|FileName:" + file.getName()+"|FileSize:" + fileSize + "|Offset:" + bytesSent + "|Data:" + encodedData;
                out.println(request);
                out.flush();
                bytesSent += bytesRead;
            }

            out.println("MessageType:UploadFileRequest|MessageId:" + generateMessageId() + "|UserId:" + userId + "|FileName:" + file.getName()+"|FileSize:" + fileSize + "|Offset:" + bytesSent + "|EndOfFile:true");
            out.flush();
            System.out.println("Plik wysłany.");

            String response;
            if ((response = in.readLine()) != null && response.contains("204")) {
                System.out.println("Plik wysłany pomyślnie.");
            } else {
                System.out.println("Błąd wysyłania pliku: " + response);
            }
        }
    }

    private static void handleDownloadFile(BufferedReader console, String userId, PrintWriter out, BufferedReader in) throws IOException {
        try {
            System.out.println("Dostępne pliki:");
            String availableFiles = getAvailableFiles(out, in);
            availableFiles.split("\\|");
            for(String file : availableFiles.split("\\|")) {
                if(file.contains(".")){
                    System.out.println(file);
                }
            }

            System.out.println("Podaj nazwę pliku do pobrania:");
            String fileName = console.readLine();

            int messageId = generateMessageId();
            String request = "MessageType:DownloadFileRequest|MessageId:" + messageId + "|UserId:" + userId + "|FileName:" + fileName;

            out.println(request);
            out.flush();

            File file = new File("uploads/" + fileName);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                String response;
                while ((response = in.readLine()) != null) {
                    String[] responseParts = response.split("\\|");

                    String encodedData = null;
                    boolean endOfFile = false;

                    for (String part : responseParts) {
                        if (part.startsWith("Data:")) {
                            encodedData = part.substring(5);
                        } else if (part.startsWith("EndOfFile:")) {
                            endOfFile = part.substring(10).equalsIgnoreCase("true");
                        }
                    }

                    if (encodedData != null) {
                        try {
                            byte[] decodedData = Base64.getDecoder().decode(encodedData);
                            bos.write(decodedData);
                        } catch (IllegalArgumentException e) {
                            System.err.println("Błąd dekodowania Base64: " + e.getMessage());
                            return;
                        }
                    }

                    if (endOfFile) {
                        System.out.println("Plik został pobrany i zapisany: " + file.getAbsolutePath());
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Błąd podczas zapisywania pliku: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Błąd podczas pobierania pliku: " + e.getMessage());
        }
    }

    private static String getAvailableFiles(PrintWriter out, BufferedReader in) throws IOException {
        int messageId = generateMessageId();
        String request = "MessageType:GetAvailableFilesRequest|MessageId:" + messageId;
        return sendRequest(out, in, request);
    }

    private static String sendRequest(PrintWriter out, BufferedReader in, String request) throws IOException {
        out.println(request);
        out.flush();
        return in.readLine();
    }

    private static String getField(String[] parts, String key) {
        for (String part : parts) {
            if (part.startsWith(key + ":")) {
                return part.split(":", 2)[1];
            }
        }
        return null;
    }

    private static int generateMessageId() {
        return ++messageIdCounter;
    }
}