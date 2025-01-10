package server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class APIGateway {
    private static final int GATEWAY_PORT = 7000;
    private static final String HOST = "localhost";
    private static final Map<String, Integer> SERVICE_PORTS = new HashMap<>();
    static {
        SERVICE_PORTS.put("RegistrationRequest", 6000);
        SERVICE_PORTS.put("LoginRequest", 6001);
        SERVICE_PORTS.put("AddPostRequest", 6002);
        SERVICE_PORTS.put("GetPostsRequest", 6003);
        SERVICE_PORTS.put("UploadFileRequest", 6004);
        SERVICE_PORTS.put("DownloadFileRequest", 6005);
        SERVICE_PORTS.put("GetAvailableFilesRequest", 6007);
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(GATEWAY_PORT)) {
            System.out.println("APIGatewayServer uruchomiony na porcie " + GATEWAY_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket){
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Otrzymano zapytanie: " + inputLine);

                if (!inputLine.contains("MessageType:")) {
                    out.println("400|Nieprawidłowy format żądania: brak MessageType");
                    continue;
                }
                String[] parts = inputLine.split("\\|");
                String messageType = null;
                for (String part : parts) {
                    if (part.startsWith("MessageType:")) {
                        messageType = part.split(":")[1];
                        break;
                    }
                }
                if (messageType == null) {
                    out.println("400|Nieprawidłowy format żądania: brak typu wiadomości");
                    continue;
                }

                Integer servicePort = SERVICE_PORTS.get(messageType);
                if (servicePort == null) {
                    out.println("400|Nieobsługiwany typ żądania: " + messageType);
                    continue;
                }

                if (messageType.equals("UploadFileRequest")) {
                    handleFileUpload(servicePort, in, out, inputLine);
                } else if (messageType.equals("DownloadFileRequest")) {
                    handleFileDownload(servicePort, out, inputLine);
                } else {
                    String response = forwardRequest(servicePort, inputLine);
                    System.out.println("Wysyłana odpowiedź: " + response);
                    out.println(response);
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd podczas obsługi klienta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleFileUpload(int servicePort, BufferedReader in, PrintWriter out, String firstInputLine) {
        try (Socket serviceSocket = new Socket("localhost", servicePort);
             PrintWriter serviceOut = new PrintWriter(serviceSocket.getOutputStream(), true);
             BufferedReader serviceIn = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()))) {

            serviceOut.println(firstInputLine);
            serviceOut.flush();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Otrzymano dane do przesyłania: " + inputLine);

                if (inputLine.contains("EndOfFile:true")) {
                    serviceOut.println(inputLine);
                    serviceOut.flush();
                    break;
                }
                serviceOut.println(inputLine);
                serviceOut.flush();
            }

            String response;
            while ((response= serviceIn.readLine()) != null) {
                System.out.println("Odpowiedź serwera: " + response);
                out.println(response);
                out.flush();
                break;
            }

        } catch (IOException e) {
            System.err.println("Błąd podczas przekazywania pliku: " + e.getMessage());
            out.println("Status:400|Błąd podczas przekazywania pliku");
            e.printStackTrace();
        }
    }

    private static void handleFileDownload(int servicePort, PrintWriter out, String inputLine) {
    try (Socket serviceSocket = new Socket(HOST, servicePort);
         PrintWriter serviceOut = new PrintWriter(serviceSocket.getOutputStream(), true);
         BufferedReader serviceIn = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()))) {

        serviceOut.println(inputLine);

        String packet;
        while ((packet = serviceIn.readLine()) != null) {
            out.println(packet);
            out.flush();

            if (packet.contains("EndOfFile:true")) {
                break;
            }
        }
    } catch (IOException e) {
        System.err.println("Błąd podczas przekazywania pliku: " + e.getMessage());
        out.println("400|Błąd podczas przekazywania pliku");
        e.printStackTrace();
    }
}

    private static String forwardRequest(int port, String request) {
        try (Socket socket = new Socket(HOST, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(request);
            out.flush();
            String response = in.readLine();
            System.out.println(response);
            if (response != null) {
                return response;
            } else {
                return "Błąd przetwarzania żądania";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Błąd przetwarzania żądania";
        }
    }
}
