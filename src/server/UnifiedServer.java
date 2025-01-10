package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;

public class UnifiedServer {
    private static final String STORAGE_DIR = "serwer/";
    private static final int UPLOAD_PORT = 6004;
    private static final int DOWNLOAD_PORT = 6005;
    private static final int FILE_RECEIVER_PORT = 6006;
    private static final int FILE_SEARCH_PORT = 6007;

    public static void main(String[] args){
        File storageDir = new File(STORAGE_DIR);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        new Thread(new UploadServer(UPLOAD_PORT)).start();
        new Thread(new FileReceiverServer(FILE_RECEIVER_PORT)).start();
        new Thread(new DownloadServer(DOWNLOAD_PORT)).start();
        new Thread(new FileSearchServer(FILE_SEARCH_PORT)).start();

        System.out.println("Serwer uruchomiony na portach " + UPLOAD_PORT + ", " + DOWNLOAD_PORT + ", " + FILE_RECEIVER_PORT);
    }
    public static class UploadServer implements Runnable {
        private final int port;
        private static final String STORAGE_DIR = "serwer/";

        public UploadServer(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try (ServerSocket server = new ServerSocket(port)) {
                while (true) {
                    try (Socket socket = server.accept();
                         PrintWriter sender = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                        System.out.println("Zaakceptowano połączenie do przesyłania plików");

                        String inputLine;
                        while ((inputLine = reader.readLine()) != null) {
                            if (inputLine.isEmpty()) {
                                continue;
                            }

                            String[] parts = inputLine.split("\\|");
                            if (parts.length < 6) {
                                System.err.println("Nieprawidłowy format zapytania");
                                continue;
                            }

                            String messageType = parts[0];
                            String fileName = parts[3].split(":")[1];
                            int offset = Integer.parseInt(parts[5].split(":")[1]);
                            String data = parts[6].split(":")[1];
                            if ("MessageType:UploadFileRequest".equalsIgnoreCase(messageType)) {
                                handleUpload(fileName, offset, data, sender);
                            } else {
                                System.err.println("Nieznany typ wiadomości: " + messageType);
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleUpload(String fileName, int offset, String data, PrintWriter sender) {
            try {
                File file = new File(STORAGE_DIR + fileName);
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }

                try (RandomAccessFile fileStream = new RandomAccessFile(file, "rw")) {
                    fileStream.seek(offset);
                    byte[] decodedData = Base64.getDecoder().decode(data);
                    fileStream.write(decodedData);

                    String response = "MessageType:UploadFileResponse|FileName:" + fileName + "|Status:200";
                    if (data.equals("true")) {
                        response = "MessageType:UploadFileResponse|FileName:" + fileName + "|Status:204";
                        System.out.println(response);
                        sender.println(response);
                    } else {
                        System.out.println(response);
                    }
                }

            } catch (IOException e) {
                sender.println("MessageType:UploadFileResponse|FileName:" + fileName + "|Status:400|Error:IOException");
                e.printStackTrace();
            }
        }

    }

    public static class DownloadServer implements Runnable {
        private final int port;
        private static final String STORAGE_DIR = "serwer/";

        public DownloadServer(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Serwer pobierania uruchomiony na porcie: " + port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                System.err.println("Błąd uruchamiania serwera: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleClient(Socket clientSocket) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Otrzymano zapytanie: " + inputLine);

                    String[] parts = inputLine.split("\\|");
                    String messageType = null;
                    String filename = null;
                    String messId = null;

                    for (String part : parts) {
                        if (part.startsWith("MessageType:")) {
                            messageType = part.split(":")[1];
                        } else if (part.startsWith("FileName:")) {
                            filename = part.split(":")[1];
                        } else if (part.startsWith("MessageId:")) {
                            messId = part.split(":")[1];
                        }
                    }

                    int messageId = Integer.parseInt(messId);

                    if ("DownloadFileRequest".equals(messageType) && filename != null) {
                        downloadFileRequest(filename, messageId, out);
                    } else {
                        out.println("400|Nieobsługiwany typ żądania lub brak nazwy pliku.");
                    }
                }
            } catch (IOException e) {
                System.err.println("Błąd podczas obsługi klienta: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void downloadFileRequest(String filename, int messageId, PrintWriter out) {
            File file = new File(STORAGE_DIR + filename);

            if (!file.exists() || file.isDirectory()) {
                out.println("404|Plik nie znaleziony: " + filename);
                return;
            }

            String responseBase = "MessageType:DownloadFileResponse|MessageId:" + messageId + "|FileName:" + file.getName();
            try (FileInputStream fis = new FileInputStream(file)) {
                long fileSize = file.length();
                System.out.println("Plik w trakcie wysyłania (" + fileSize + " bajtów)...");

                byte[] buffer = new byte[512];
                int bytesRead;
                long bytesSent = 0;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    String encodedData = Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, bytesRead));
                    String request = responseBase + "|FileSize:" + fileSize + "|Offset:" + bytesSent + "|Data:" + encodedData;
                    out.println(request);
                    out.flush();
                    bytesSent += bytesRead;
                }

                out.println(responseBase + "|FileSize:" + fileSize + "|Offset:" + bytesSent + "|EndOfFile:true");
                out.flush();
                System.out.println("Plik wysłany.");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class FileReceiverServer implements Runnable {
        private final int port;

        public FileReceiverServer(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new FileReceiverHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static class FileReceiverHandler implements Runnable {
            private final Socket socket;

            public FileReceiverHandler(Socket socket) {
                this.socket = socket;
            }

            @Override
            public void run() {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String line;
                    boolean receivingFile = false;
                    FileOutputStream fileStream = null;
                    long fileLength = 0;
                    long totalBytesReceived = 0;
                    String fileName = null;

                    while ((line = in.readLine()) != null) {
                        System.out.println("Otrzymano zapytanie: " + line);

                        String[] parts = line.split("\\|");
                        if (parts.length < 7) {
                            System.err.println("Nieprawidłowy format zapytania.");
                            continue;
                        }

                        String messageType = null, data = null;

                        for (String part : parts) {
                            if (part.startsWith("MessageType:")) {
                                messageType = part.split(":")[1];
                            } else if (part.startsWith("FileName:")) {
                                fileName = part.split(":")[1];
                            } else if (part.startsWith("file_length:")) {
                                fileLength = Long.parseLong(part.split(":")[1]);
                            } else if (part.startsWith("data:")) {
                                data = part.substring(part.indexOf(":") + 1);
                            }
                        }

                        if ("UploadFileRequest".equalsIgnoreCase(messageType)) {
                            if (fileName != null && data != null) {
                                receivingFile = true;
                                if (fileStream == null) {
                                    fileStream = new FileOutputStream(fileName);
                                }
                                fileStream.write(Base64.getDecoder().decode(data));
                                totalBytesReceived += data.length();
                            }
                        }

                        if (receivingFile && totalBytesReceived == fileLength) {
                            receivingFile = false;
                            if (fileStream != null) {
                                fileStream.close();
                                System.out.println("Plik odbierany pomyślnie.");

                                String response = "MessageType:UploadFileResponse|FileName:" + fileName + "|Status:Success";
                                System.out.println("Wysłano odpowiedź: " + response);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class FileSearchServer implements Runnable {
        private final int port;

        public FileSearchServer(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new FileSearchHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static class FileSearchHandler implements Runnable {
            private final Socket socket;

            public FileSearchHandler(Socket socket) {
                this.socket = socket;
            }

            @Override
            public void run() {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    String line;
                    while ((line = in.readLine()) != null) {
                        String[] parts = line.split("\\|");
                        String messageType = parts[0].split(":")[1];
                        String messageID = parts[1].split(":")[1];
                        if ("GetAvailableFilesRequest".equals(messageType)) {
                            searchFile(out,messageID);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            private void searchFile(PrintWriter out, String messageID) {
                File dir = new File(STORAGE_DIR);
                int id = Integer.parseInt(messageID);

                if (!dir.exists() || !dir.isDirectory()) {
                    out.println("Błąd: Katalog nie istnieje lub nie jest katalogiem.");
                    return;
                }

                File[] files = dir.listFiles();
                if (files != null && files.length > 0) {
                    String response = "MessageType:GetAvailableFilesResponde|MessageId:" + id;
                    for (File file : files) {
                        if (file.isFile()) {
                            response += "|"+file.getName();
                        }
                    }
                    out.println(response);
                } else {
                    out.println("Brak wyników.");
                }
            }

        }

    }
}
