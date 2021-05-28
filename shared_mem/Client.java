import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;

import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.Socket;
import java.net.SocketException;

public class Client extends Thread{
    
    private BufferedReader inputReader;
    private OutputStream outputStream;

    private Socket connection;

    private String readString;

    private boolean running;

    private String host;
    private int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        
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
        while (running) {
            try {
                synchronized(inputReader) {
                    readString = inputReader.readLine();
                    System.out.println("Got the following back: " + readString);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        System.out.println("Sent " + s);
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

    public static void main(String[] args) {
        Client c = new Client("localhost", 8000);
        c.start();
        c.send("Hello, World!");
    }

}
