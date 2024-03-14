import java.net.*;
import tcpclient.TCPClient;
import java.io.*;

public class HTTPAsk {

    private static final String HTTP_OK = "HTTP/1.1 200 OK\r\n\r\n";
    private static final String HTTP_BAD_REQUEST = "HTTP/1.1 400 Bad Request\r\n\r\n";
    private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found\r\n\r\n";
    private static final String HTTP_NOT_SUPPORTED = "HTTP/1.1 501 Not Implemented\r\n\r\n";

    private static int port;

    public static void main(String[] args) throws IOException {
        try {
            port = Integer.parseInt(args[0]);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Usage: HTTPAsk port");
            System.exit(1);
        }

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server listening on port:" + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("A client has connected!");

            handleRequest(clientSocket);

            System.out.println("Connection to client is closed!");
        }
    }

    private static void handleRequest(Socket connectionSocket) throws IOException {
        InputStream input = connectionSocket.getInputStream();
        ByteArrayOutputStream reqBytes = new ByteArrayOutputStream();
        OutputStream output = connectionSocket.getOutputStream();
        byte[] serverResponse;

        int readBytes;
        byte[] buffer = new byte[1024];

        while ((readBytes = input.read(buffer)) != -1) {
            reqBytes.write(buffer, 0, readBytes);
            if (readBytes < buffer.length) break;
        }

        String reqHeader = reqBytes.toString().split("\r\n\r\n")[0];

        System.out.println("Request Header:");
        System.out.println(reqHeader);

        String[] reqLineParts = reqHeader.split("\r")[0].split(" ");
        String reqMethod = reqLineParts[0];
        String reqPath = reqLineParts[1];
        String reqVersion = reqLineParts[2];

        String[] params = reqPath.split("[?&=]");

        if (!reqMethod.equals("GET")) {
            output.write(HTTP_NOT_SUPPORTED.getBytes());
            connectionSocket.close();
            return;
        }

        String clientResponse;

        if (params[0].equals("/ask")) {
            String hostname = null;
            int port = -1;
            byte[] data = new byte[0];
            boolean shutdown = false;
            Integer timeout = null;
            Integer limit = null;

            for (int i = 1; i < params.length; i++) {
                switch (params[i++]) {
                    case "hostname" -> hostname = params[i];
                    case "port" -> port = Integer.parseInt(params[i]);
                    case "string" -> data = (params[i] + "\r\n").getBytes();
                    case "shutdown" -> shutdown = params[i].equalsIgnoreCase("true");
                    case "limit" -> limit = Integer.parseInt(params[i]);
                    case "timeout" -> timeout = Integer.parseInt(params[i]);
                }
            }

            TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);

            if (hostname != null && port != -1 && reqVersion.equals("HTTP/1.1")) {
                try {
                    serverResponse = tcpClient.askServer(hostname, port, data);

                    System.out.println("----Response from server----");
                    System.out.println(new String(serverResponse));
                    System.out.println("-------End response---------\n");
                    clientResponse = HTTP_OK + new String(serverResponse);
                } catch (IOException e) {
                    clientResponse = HTTP_OK + "There was an error trying to connect to the server";
                }
            } else {
                clientResponse = HTTP_BAD_REQUEST;
            }
        } else {
            clientResponse = HTTP_NOT_FOUND;
        }

        output.write(clientResponse.getBytes());
        connectionSocket.close();
    }
}