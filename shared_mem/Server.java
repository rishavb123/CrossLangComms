import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;

public class Server extends Thread {

    private int port;

    private ServerSocket server;
    private ArrayList<ServerThread> threads;

    public Server(int port) {
        this.port = port;
        threads = new ArrayList<>();
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        System.out.println("Server starting on port " + port);
        try {
            while (true) {
                try {
                    Socket s = server.accept();
                    ServerThread serverThread = new ServerThread(s, threads.size());
                    threads.add(serverThread);
                    serverThread.start();
                } catch (EOFException eof) {
                    eof.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.close();
        }
    }
    
    public void close() {
        for (ServerThread s : threads) {
            s.close();
        }
        System.out.println("Shutting down server");
    }

    public int getPort() {
        return port;
    }

    public class ServerThread extends Thread {

        private Socket connection;

        private OutputStream outputStream;
        private Scanner inputScanner;

        private String readString;

        private boolean running;

        private int index;
        private String hostname;

        public ServerThread(Socket socket, int index) {
            connection = socket;
            this.index = index;
            this.hostname = connection.getInetAddress().getHostName();
            this.setupStreams();
            log(hostname + " joined");
        }

        private void setupStreams() {
            try {
                outputStream = connection.getOutputStream();
                outputStream.flush();
                inputScanner = new Scanner(connection.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            running = true;
            while (!running) {
                synchronized (inputScanner) {
                    readString = inputScanner.nextLine();
                    log("Received \"" + readString + "\" from connection " + index);
                    send("Received \"" + readString + "\" from connection " + index);
                }
            }
            log(hostname + " left");
        }

        public String read() {
            return readString;
        }

        public void send(String s) {
            synchronized (outputStream) {
                try {
                    if (s != null)
                        outputStream.write(s.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void close() {
            running = false;
            try {
                inputScanner.close();
                outputStream.close();
                connection.close();
                threads.remove(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        private void log(String s) {
            System.out.println("\tConnection " + index + ": " + s);
        }

    }

    public static void main(String[] args) {
        Server s = new Server(8000);
        s.start();
    }

}