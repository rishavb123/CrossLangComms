import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.crypto.Data;

public class MemoryServer extends Thread {

    private int port;

    private ServerSocket server;
    private ArrayList<ServerThread> threads;

    private ConcurrentHashMap<String, DataObject> memory;

    public MemoryServer(int port) {
        this.port = port;
        threads = new ArrayList<>();
        memory = new ConcurrentHashMap<>();
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

        private volatile boolean running;

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
                        if (readString != null) {
                            this.send(processReadString(readString));
                            log("Received \"" + readString + "\"");
                        }
                    }
                } catch (SocketException e) {
                    log("Client terminated connection");
                    this.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            log("Left");
            this.close();
        }

        private String processReadString(String s) {
            String[] arr = s.split(" ");
            String command = arr[0];
            String[] args = new String[arr.length - 1];
            System.arraycopy(arr, 1, args, 0, args.length);

            switch(command.toUpperCase()) {
                case "DOC":
                    if (args.length == 0) {
                        String[] commands = { "GET", "PUT", "SET", "UPDATE", "DELETE", "DOC", "KEYS", "DISP", "DISPLAY",
                                "EXIT" };
                        String output = "F Please use a command from the following list for the DOC command [";
                        for (String cmd : commands) {
                            output += String.format("%s, ", cmd);
                        }
                        return output.substring(0, output.length() - 2) + "]";
                    }
                    String docCommand = args[0];
                    switch (docCommand.toUpperCase()) {
                        case "GET":
                            return "P To get a value: GET {key} -> P {type} {value}";
                        case "PUT":
                            return "P To put a value into a key or to change the type of a key: PUT {key} {type} {value} -> P ";
                        case "SET":
                            return "P To essentially keep the type of a key if possible. Otherwise, the program will infer the type and set the key to that type: SET {key} {value} -> P ";
                        case "UPDATE":
                            return "P To update a value into a key (do not change type): " + command.toUpperCase()
                                    + " {key} {value} -> P ";
                        case "DELETE":
                            return "P To delete a key from the shared memory: DELETE {key} -> P ";
                        case "DOC":
                            return "P To document any command: DOC {command} -> P {docs}";
                        case "KEYS":
                            return "P To get a list of all the keys: KEYS -> P {keys}";
                        case "DISP":
                        case "DISPLAY":
                            return String.format(
                                    "P To display all the values stored in the share memory: %s -> P {output}",
                                    docCommand.toUpperCase());
                        case "CLEAR":
                            return "P To clear all the data inside the map: CLEAR -> P ";
                        case "EXIT":
                            return "P To exit and close the connection: EXIT -> D ";
                        default:
                            return "P Error messages: * -> F {message which can include spaces}";
                    }
                case "KEYS":
                    return "P " + memory.keySet().toString();
                case "DISP":
                case "DISPLAY":
                    return "P " + memory.toString();
                case "CLEAR":
                    memory.clear();
                    return "P ";
                case "EXIT":
                    running = false;
                    return "T ";
            }

            if (args.length == 0)
                return "F command must include the key argument";
            String key = args[0];
            switch (command.toUpperCase()) {
                case "GET":
                    if (memory.containsKey(key))
                        return "P " + memory.get(key).toString();
                    return "F key " + key + " does not exist";
                case "PUT":
                    if (args.length < 3)
                        return "F command must include the key, type, and val arguments";
                    String type = args[1];
                    String val = args[2];
                    if (DataObject.validate(type, val)) {
                        memory.put(key, new DataObject(key, type, val));
                        return "P ";
                    }
                    return "F value " + val + " not of type " + type;
                case "SET":
                    if (args.length < 2)
                        return "F command must include the key and val arguments";
                    String value = args[1];
                    if (memory.containsKey(key)) {
                        DataObject dataObject = memory.get(key);
                        if (DataObject.validate(dataObject.getType(), value)) {
                            dataObject.setValue(value);
                            return "P ";
                        }
                    }
                    String inferredType = DataObject.inferType(value);
                    if (DataObject.validate(inferredType, value)) {
                        memory.put(key, new DataObject(key, inferredType, value));
                        return "P ";
                    }
                    return "F value " + value + " not of type " + inferredType;
                case "UPDATE":
                    if (args.length < 2)
                        return "F command must include the key and val arguments";
                    String updateValue = args[1];
                    if (memory.containsKey(key)) {
                        DataObject dataObject = memory.get(key);
                        if (!DataObject.validate(dataObject.getType(), updateValue))
                            return "F value " + updateValue + " not of type " + dataObject.getType();
                        dataObject.setValue(updateValue);
                        return "P ";
                    }
                    return "F key " + key + " does not exist";
                case "DELETE":
                    if (memory.containsKey(key)) {
                        memory.remove(key);
                        return "P ";
                    }
                    return "F key " + key + " does not exist";             
                default:
                    return "F Unknown command " + command;
            }
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
        int port = 8000;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);
        MemoryServer s = new MemoryServer(port);
        s.start();
    }

}