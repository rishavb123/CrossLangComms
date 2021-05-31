import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.Socket;
import java.net.SocketException;

public class Accessor {
    
    public static final String SPACE_REPLACEMENT = "~`*@!#";

    private String host;
    private int port;

    private Socket connection;

    private BufferedReader inputReader;
    private OutputStream outputStream;

    public Accessor() {
        this("localhost", 8000);
    }

    public Accessor(String host, int port) {
        this.host = host;
        this.port = port;

        initConnection();
    }

    public boolean initConnection() {
        try {
            connection = new Socket(host, port);
            setupStreams();
            return true;
        } catch (IOException e) {
            System.out.println("Could not connect to Server!");
            return false;
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

    private void send(String s) {
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

    private String receive() {
        synchronized (inputReader) {
            try {
                return inputReader.readLine();
            } catch (SocketException e) {
                System.out.println("Server closed connection");
                this.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public Object get(String key) throws KeyException {
        send(String.format("GET %s", key));
        String resp = receive();
        if (resp.charAt(0) == 'P') {
            String[] arr = resp.split(" ");
            String type = arr[1];
            String val = arr[2];
            return resolveType(type, val);
        }
        throw new KeyException(resp.substring(2));
    }

    public void put(String key, Object obj) throws TypeException {
        String[] arr = resolveObject(obj);
        send(String.format("PUT %s %s %s", key, arr[0], arr[1]));
        String resp = receive();
        if (resp.charAt(0) != 'P') {
            throw new TypeException(resp.substring(2));
        }
    }

    public void set(String key, Object obj) throws KeyException, TypeException {
        update(key, obj);
    }

    public void update(String key, Object obj) throws KeyException, TypeException {
        String val = resolveObject(obj)[1];
        send(String.format("UPDATE %s %s", key, val));
        String resp = receive();
        if (resp.charAt(0) != 'P') {
            String[] arr = resp.split(" ");
            if (arr[1].equals("key")) {
                throw new KeyException(resp.substring(2));
            } else if (arr[1].equals("value")) {
                throw new TypeException(resp.substring(2));
            }
        }
    }

    public void delete(String key) throws KeyException {
        send(String.format("DELETE %s", key));
        String resp = receive();
        if (resp.charAt(0) != 'P')
            throw new KeyException(resp.substring(2));
    }
    
    public String doc(String command) {
        send(String.format("DOC %s", command));
        String resp = receive();
        return resp.substring(2);
    }

    public void close() {
        try {
            inputReader.close();
            outputStream.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static String[] resolveObject(Object object) {
        String[] arr = object.getClass().getName().split("\\.");
        String type = arr[arr.length - 1].toLowerCase();
        String val = object.toString();
        if (type.equals("string")) {
            val = val.replaceAll(" ", SPACE_REPLACEMENT);
        }
        return new String[] { type, val };
    }

    public static Object resolveType(String type, String val) {
        switch (type.toLowerCase()) {
            case "int":
            case "integer":
                return Integer.parseInt(val);
            case "long":
                return Long.parseLong(val);
            case "short":
                return Short.parseShort(val);
            case "float":
                return Float.parseFloat(val);
            case "double":
                return Double.parseDouble(val);
            case "boolean":
            case "bool":
                return Boolean.parseBoolean(val);
            case "string":
            case "str":
                return val.replaceAll(SPACE_REPLACEMENT, " ");
            case "char":
            case "chr":
                return val.charAt(0);
            default:
                return val;
        }
    }

    private static class ServerException extends RuntimeException {
        private ServerException(String msg) {
            super(msg);
        }
    }

    private static class KeyException extends ServerException {
        private KeyException(String msg) {
            super(msg);
        }
    }

    private static class TypeException extends ServerException {
        private TypeException(String msg) {
            super(msg);
        }
    }

    public static void main(String[] args) {
        Accessor accessor;
        if (args.length > 0) 
            accessor = new Accessor("localhost", Integer.parseInt(args[0]));
        else
            accessor = new Accessor();

        Integer x = (Integer) accessor.get("x");
        x++;
        accessor.update("x", x);

        accessor.put("hi", 1.2);
        accessor.put("test", true);
        accessor.put("stringTest", "hisdflksdjf");
        accessor.put("stringTest2", "SPACE HERE");

        System.out.println(accessor.get("stringTest2"));

        accessor.delete("stringTest2");

        System.out.println(accessor.doc("doc"));
        
        accessor.close();

    }

}
