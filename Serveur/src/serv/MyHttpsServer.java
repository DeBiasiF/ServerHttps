package serv;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import javax.net.ssl.*;

public class MyHttpsServer extends Thread {
    private static final String KEYSTORE_FILE = "./certificates/keystore.jks";
    private static final char[] KEYSTORE_PASSWORD = "azerty".toCharArray();
    private static final char[] KEY_PASSWORD = "azerty".toCharArray();
    private static final String ALIAS = "localhost";

    private int port;

    public MyHttpsServer(int port) {
        this.port = port;
    }

    public void run(){
            try {
                // Load or create keystore
                KeyStore keyStore = KeyStore.getInstance("JKS");
                if (new File(KEYSTORE_FILE).exists()) {
                    // Keystore already exists, load it
                    keyStore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD);
                    System.out.println("Keystore loaded.");
                } else {
                    // Keystore does not exist, create it
                    JksCertManager.createCert(KEYSTORE_FILE, new String(KEYSTORE_PASSWORD), ALIAS, new String(KEY_PASSWORD));
                    keyStore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD);
                    System.out.println("Keystore created.");
                }

                SSLContext sslContext = SSLContext.getInstance("TLS");
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");

                keyManagerFactory.init(keyStore, KEY_PASSWORD);
                trustManagerFactory.init(keyStore);

                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
                SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

                SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);
                sslServerSocket.setEnabledCipherSuites(sslServerSocket.getSupportedCipherSuites());

                System.out.println("HTTPS Server started on port " + port);

                while (true) {
                    try (Socket clientSocket = sslServerSocket.accept()) {
                        System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                        StringBuilder response = new StringBuilder();
                        String contentType = "";
                        String filePath = "C:\\Users\\CDA-08\\IdeaProjects\\HTML\\ECF_FRONT\\";

                        String request = readRequest(clientSocket);
                        String[] requestLines = request.split("\r\n");

                        // Find the requested file path
                        String requestedFile = "";
                        for (String line : requestLines) {
                            if (line.startsWith("GET")) {
                                String[] parts = line.split(" ");
                                requestedFile = parts[1];
                                break;
                            }
                        }
                        if (requestedFile.equals("/") || requestedFile.equals("")) {
                            // If no file is requested, list the directory
                            contentType = "text/html";
                            response.append("HTTP/1.1 200 OK\r\n");
                            response.append("Content-Type: " + contentType + "\r\n");
                            response.append("\r\n");
                            response.append("<html><body>");

                            Path folder = Paths.get(filePath);
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
                                for (Path file : stream) {
                                    if (Files.isRegularFile(file)) {
                                        response.append("<a href=\"" + file.getFileName() + "\">" + file.getFileName() + "</a><br>");
                                    }
                                }
                            }

                            response.append("</body></html>");
                        } else {
                            // Serve the requested file
                            Path file = Paths.get(filePath + requestedFile);
                            if (Files.exists(file) && Files.isReadable(file) && !Files.isDirectory(file)) {
                                String extension = "";
                                int i = requestedFile.lastIndexOf('.');
                                if (i > 0) {
                                    extension = requestedFile.substring(i + 1);
                                }
                                switch (extension.toLowerCase()) {
                                    case "html":
                                    case "htm":
                                        contentType = "text/html";
                                        break;
                                    case "txt":
                                        contentType = "text/plain";
                                        break;
                                    case "css":
                                        contentType = "text/css";
                                        break;
                                    case "js":
                                        contentType = "application/javascript";
                                        break;
                                    case "json":
                                        contentType = "application/json";
                                        break;
                                    case "jpg":
                                    case "jpeg":
                                        contentType = "image/jpeg";
                                        break;
                                    case "png":
                                        contentType = "image/png";
                                        break;
                                    case "gif":
                                        contentType = "image/gif";
                                        break;
                                    case "ico":
                                        contentType = "image/x-icon";
                                        break;
                                    default:
                                        contentType = "application/octet-stream";
                                        break;
                                }
                                byte[] fileContent = Files.readAllBytes(file);

                                response.append("HTTP/1.1 200 OK\r\n");
                                response.append("Content-Type: " + contentType + "\r\n");
                                response.append("\r\n");
                                response.append(new String(fileContent));
                            } else {
                                // If file is not found, return a 404 error
                                contentType = "text/html";
                                response.append("HTTP/1.1 404 Not Found\r\n");
                                response.append("Content-Type: " + contentType + "\r\n");
                                response.append("\r\n");
                                response.append("<html><body>");
                                response.append("<h1>404 Not Found</h1>");
                                response.append("<p>The requested file " + requestedFile + " was not found on this server.</p>");
                                response.append("</body></html>");
                            }

                        }
                        // Send the response to the client
                        OutputStream out = clientSocket.getOutputStream();
                        out.write(response.toString().getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
    }

    private static String readRequest(Socket clientSocket) throws IOException {
        InputStream in = clientSocket.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
            if (n < 1024) {
                break;
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
