package swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class HttpsClient extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextField urlField;
    private JTextArea responseArea;

    public HttpsClient() {
        super("HTTPS Client");

        // create UI components
        urlField = new JTextField("https://localhost:8443");
        responseArea = new JTextArea();
        responseArea.setEditable(false);
        JButton sendButton = new JButton("Send");

        // add UI components to panels
        JPanel urlPanel = new JPanel(new BorderLayout());
        urlPanel.add(urlField, BorderLayout.CENTER);
        urlPanel.add(sendButton, BorderLayout.EAST);
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.add(new JScrollPane(responseArea), BorderLayout.CENTER);

        // add panels to frame
        add(urlPanel, BorderLayout.NORTH);
        add(responsePanel, BorderLayout.CENTER);

        // set action listener for send button
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String url = urlField.getText();
                sendRequest(url);
            }
        });

        // configure frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void sendRequest(String url) {
        try {
            // create SSL socket
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost", 8443);

            // send HTTP GET request
            Writer writer = new OutputStreamWriter(sslSocket.getOutputStream());
            writer.write("GET " + url + " HTTP/1.1\r\n");
            writer.write("Host: localhost\r\n");
            writer.write("\r\n");
            writer.flush();

            // read response from server
            BufferedReader reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }

            // display response in text area
            responseArea.setText(response.toString());

            // close SSL socket
            sslSocket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new HttpsClient();
    }
}
