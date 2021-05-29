import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;


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
        private BufferedReader inputReader;

        private String readString;

        private boolean running;

        private int index;
        private String hostname;

        public ServerThread(Socket socket, int index) {
            connection = socket;
            this.index = index;
            this.hostname = connection.getInetAddress().getHostName();
            this.setupStreams();
            log("Joined");
        }

        private void setupStreams() {
            try {
                outputStream = connection.getOutputStream();
                outputStream.flush();
                inputReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            running = true;
            while (running) {
                try {
                    synchronized (inputReader) {
                        readString = inputReader.readLine();
                        log("Received \"" + readString);
                        send("Received \"" + readString + "\" from connection " + index);
                    }
                } catch (SocketException e) {
                    log("Client terminated connection");
                    this.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            log("Left");
        }

        public String read() {
            return readString;
        }

        public void send(String s) {
            synchronized (outputStream) {
                try {
                    if (s != null)
                        outputStream.write((s + "\n").getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void close() {
            running = false;
            try {
                inputReader.close();
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
            System.out.println("\tConnection " + index + " (" + hostname + "): " + s);
        }

    }

    public static void main(String[] args) {
        Server s = new Server(8000);
        s.start();
    }

}