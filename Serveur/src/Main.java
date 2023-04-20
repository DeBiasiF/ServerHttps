import serv.MyHttpsServer;

public class Main {
    public static void main(String[] args) throws Exception {
        new MyHttpsServer(8443).start();
    }
}