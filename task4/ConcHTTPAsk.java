
import java.net.*;

import tcpclient.TCPClient;

import java.io.*;

public class ConcHTTPAsk {

    private static int port;
    public static void main(String[] args) throws IOException {
        // Check if the file is properly executed
        try {
            port = Integer.parseInt(args[0]);
        } catch (IndexOutOfBoundsException e) {
            System.exit(1);
        }

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server socket created on port " + port);

        while (true) {
            System.out.println("Waiting for a client to connect");
            Socket clientSocket = serverSocket.accept();
            System.out.println("A client has connected");

            MyRunnable runnable = new MyRunnable(clientSocket);
            Thread thread = new Thread(runnable);
            thread.start();
        }
    }

    public static class MyRunnable implements Runnable {

        private static final String HTTP_OK = "HTTP/1.1 200 OK\r\n\r\n";
        private static final String HTTP_BAD_REQUEST = "HTTP/1.1 400 Bad Request\r\n\r\n";
        private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found\r\n\r\n";
        private static final String HTTP_NOT_SUPPORTED = "HTTP/1.1 501 Not Implemented\r\n\r\n";
    
        private final Socket connectionSocket;
    
        public MyRunnable(Socket connectionSocket) {
            this.connectionSocket = connectionSocket;
        }
    
        @Override
        public void run() {
            try (InputStream clientInput = connectionSocket.getInputStream();
                 OutputStream serverOutput = connectionSocket.getOutputStream()) {
    
                ByteArrayOutputStream request = readRequest(clientInput);
                String requestHeader = getRequestHeader(request);
    
                System.out.println("------Request Header------");
                System.out.println(requestHeader);
                System.out.println("----End Request Header----\n");
    
                String[] requestLineParts = requestHeader.split("\r")[0].split(" ");
                String reqMethod = requestLineParts[0];
                String reqPath = requestLineParts[1];
                String reqVersion = requestLineParts[2];
    
                if (!reqMethod.equals("GET")) {
                    sendResponse(HTTP_NOT_SUPPORTED, serverOutput);
                    return;
                }
    
                String[] params = reqPath.split("[?&=]");
                handleRequestParams(params, reqVersion, serverOutput);
    
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        private ByteArrayOutputStream readRequest(InputStream clientInput) throws IOException {
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int readBytes;
            while ((readBytes = clientInput.read(buffer)) != -1) {
                request.write(buffer, 0, readBytes);
                if (readBytes < buffer.length) break;
            }
            return request;
        }
    
        private String getRequestHeader(ByteArrayOutputStream request) {
            String requestString = request.toString();
            return requestString.split("\r\n\r\n")[0];
        }
    
        private void handleRequestParams(String[] params, String reqVersion, OutputStream serverOutput) throws IOException {
            String fullResponse;
            if (params.length > 0 && params[0].equals("/ask")) {
                fullResponse = processAskRequest(params, reqVersion);
            } else {
                fullResponse = HTTP_NOT_FOUND;
            }
    
            sendResponse(fullResponse, serverOutput);
        }
    
        private String processAskRequest(String[] params, String reqVersion) {
            String fullResponse;
            String hostname = null;
            int port = -1;
            byte[] data = new byte[0];
            boolean shutdown = false;
            Integer timeout = null;
            Integer limit = null;
    
            if (params.length > 1) {
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
    
                fullResponse = createAskResponse(hostname, port, data, reqVersion, shutdown, timeout, limit);
            } else {
                fullResponse = HTTP_BAD_REQUEST;
            }
            return fullResponse;
        }
    
        private String createAskResponse(String hostname, int port, byte[] data, String reqVersion, boolean shutdown, Integer timeout, Integer limit) {
            String fullResponse;
            TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);
    
            if (hostname != null && port != -1 && reqVersion.equals("HTTP/1.1")) {
                try {
                    byte[] serverResponse = tcpClient.askServer(hostname, port, data);
                    System.out.println("Arguments to TCPClient.askServer: " + hostname + " " + port + " " + new String(data) + "\n");
    
                    System.out.println("----Response from server----");
                    System.out.println(new String(serverResponse));
                    System.out.println("---------------------------\n");
                    fullResponse = HTTP_OK + new String(serverResponse);
                } catch (IOException e) {
                    fullResponse = HTTP_OK + "There was an error trying to connect to the server given the information you provided, try again.";
                }
            } else {
                fullResponse = HTTP_BAD_REQUEST;
            }
            return fullResponse;
        }
    
        private void sendResponse(String response, OutputStream serverOutput) throws IOException {
            System.out.println("-----Response to client-----");
            System.out.println(response);
            System.out.println("--------------------------\n");
    
            serverOutput.write(response.getBytes());
            connectionSocket.close();
            System.out.println("Connection is closed");
        }
    }
    
}