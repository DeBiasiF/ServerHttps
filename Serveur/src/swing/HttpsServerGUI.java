package swing;

import com.sun.net.httpserver.HttpsServer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class HttpsServerGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private int port = 443;
    private SSLServerSocket sslServerSocket = null;
    private JButton startButton = new JButton("Start");
    private JButton stopButton = new JButton("Stop");
    private JTextField portField = new JTextField(String.valueOf(port), 5);

    public HttpsServerGUI() {
        super("HTTPS Server");

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);
        topPanel.add(startButton);
        topPanel.add(stopButton);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    port = Integer.parseInt(portField.getText());
                    startServer();
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    portField.setEnabled(false);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(HttpsServerGUI.this, "Invalid port number", "Error",
                            JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HttpsServerGUI.this, "Error starting server: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    stopServer();
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    portField.setEnabled(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HttpsServerGUI.this, "Error stopping server: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        stopButton.setEnabled(false);
        mainPanel.add(new JLabel("Server is not running."), BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }


    private void startServer() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");

        String keystoreFile = "./certificates/keystore.jks";
        char[] keystorePassword = "azerty".toCharArray();
        char[] keyPassword = "azerty".toCharArray();

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keystoreFile), keystorePassword);
        keyManagerFactory.init(keyStore, keyPassword);

        trustManagerFactory.init(keyStore);

        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
        sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!sslServerSocket.isClosed()) {
                    try {
                        Socket socket = sslServerSocket.accept();
                        OutputStream outputStream = socket.getOutputStream();

                        // Handle the request
                        handleRequest(outputStream);

                        // Close the socket
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void stopServer() throws IOException {
        if (sslServerSocket != null && !sslServerSocket.isClosed()) {
            sslServerSocket.close();
        }
    }

    private void handleRequest(OutputStream outputStream) throws IOException {
        Path webRoot = Paths.get("C:\\Users\\CDA-08\\IdeaProjects\\HTML\\ECF_FRONT\\boardgames.json");

        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isRegularFile(entry) && !entry.getFileName().toString().startsWith(".");
            }
        };

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(webRoot, filter)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<html>");
            stringBuilder.append("<head>");
            stringBuilder.append("<title>HTTPS Server</title>");
            stringBuilder.append("</head>");
            stringBuilder.append("<body>");
            stringBuilder.append("<h1>HTTPS Server</h1>");
            stringBuilder.append("<ul>");
            for (Path path : directoryStream) {
                String filename = path.getFileName().toString();
                String href = "/" + filename;
                stringBuilder.append("<li><a href=\"").append(href).append("\">").append(filename).append("</a></li>");

                // Check if file is JSON and has the name "data.json"
                if (filename.endsWith(".json")) {
                    String fileContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    String jsonResponse = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: " + fileContent.length() + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n" +
                            fileContent;
                    outputStream.write(jsonResponse.getBytes());
                    return;
                }
            }
            stringBuilder.append("</ul>");
            stringBuilder.append("</body>");
            stringBuilder.append("</html>");

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + stringBuilder.length() + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    stringBuilder.toString();

            outputStream.write(response.getBytes());
        }
    }


    public static void main(String[] args) {
        new HttpsServerGUI();
    }
}