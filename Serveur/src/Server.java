import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Serveur démarré sur le port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connecté : " + clientSocket.getInetAddress().getHostAddress());

            File folder = new File("path/to/your/folder");
            File[] files = folder.listFiles();

            StringBuilder response = new StringBuilder();
            response.append("HTTP/1.1 200 OK\r\n");
            response.append("Content-Type: text/html\r\n");
            response.append("\r\n");
            response.append("<html><body>");

            for (File file : files) {
                if (file.isFile()) {
                    response.append("<a href=\"" + file.getName() + "\">" + file.getName() + "</a><br>");
                }
            }

            response.append("</body></html>");

            clientSocket.getOutputStream().write(response.toString().getBytes());
            clientSocket.close();
        }
    }
}
