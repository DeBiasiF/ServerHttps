package serv;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;

public class MyHttpsServerThread implements Runnable {
    private static final String KEYSTORE_FILE = "./certificates/keystore.jks";
    private static final char[] KEYSTORE_PASSWORD = "azerty".toCharArray();
    private static final char[] KEY_PASSWORD = "azerty".toCharArray();
    private static final String ALIAS = "localhost";

    private int port;

    public MyHttpsServerThread(int port) {
        this.port = port;
    }

    @Override
    public void run() {
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

            while (!Thread.currentThread().isInterrupted()) {
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
                        // If a file is requested, try to send it
                        filePath += requestedFile;
                        File file = new File(filePath);
                        if (file.exists()) {
                            // File exists, send it
                            contentType = Files.probeContentType(file.toPath());
                            response.append("HTTP/1.1 200 OK\r\n");
                            response.append("Content-Type: " + contentType + "\r\n");
                            response.append("\r\n");

                            byte[] fileContent = Files.readAllBytes(file.toPath());
                            response.append(new String(fileContent));
                        } else {
                            // File does not exist, return 404 error
                            contentType = "text/html";
                            response.append("HTTP/1.1 404 Not Found\r\n");
                            response.append("Content-Type: " + contentType + "\r\n");
                            response.append("\r\n");
                            response.append("<html><body><h1>404 Not Found</h1></body></html>");
                        }
                    }

                    OutputStream outputStream = clientSocket.getOutputStream();
                    outputStream.write(response.toString().getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException |
                 UnrecoverableKeyException | KeyManagementException e) {

        } catch (Exception e) {
            e.printStackTrace();        }
    }

    private String readRequest(Socket socket) throws IOException {
        StringBuilder request = new StringBuilder();
        InputStream inputStream = socket.getInputStream();

        while (inputStream.available() == 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        byte[] buffer = new byte[4096];
        while (inputStream.available() > 0 && inputStream.read(buffer) != -1) {
            request.append(new String(buffer));
        }

        return request.toString();
    }

    public static void main(String[] args) {
        MyHttpsServerThread server = new MyHttpsServerThread(8000);
        Thread thread = new Thread(server);
        thread.start();
    }
}