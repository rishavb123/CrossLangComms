import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class TestClient extends Thread{
    
    private BufferedReader inputReader;
    private OutputStream outputStream;

    private Socket connection;

    private String readString;

    private volatile boolean running;

    private String host;
    private int port;

    private Scanner scanner;

    public TestClient(String host, int port) {
        this.host = host;
        this.port = port;
        
        scanner = new Scanner(System.in);

        try {
            connection = new Socket(host, port);
            setupStreams();
            System.out.println("Connected to " + host + ":" + port);
        } catch (IOException e) {
            System.out.println("Could not connect to Server!");
        }
    }

    private void setupStreams() {
        try {
            inputReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            outputStream = connection.getOutputStream();
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        running = true;
        System.out.print("> ");
        String command;
        while (running && !(command = scanner.nextLine()).equals("exit")) {
            try {
                this.send(command);
                synchronized (inputReader) {
                    readString = inputReader.readLine();
                    System.out.println(readString);
                }
                System.out.print("> ");
            } catch (SocketException e) {
                System.out.println("Server closed connection");
                this.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        try {
            running = false;
            inputReader.close();
            outputStream.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String read() {
        return readString;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }

    public static void main(String[] args) {
        int port = 8000;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);
        TestClient c = new TestClient("localhost", port);
        c.start();
    }

}
