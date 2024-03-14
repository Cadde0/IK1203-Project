package tcpclient;

import java.net.*;
import java.io.*;

public class TCPClient {
    boolean shutdown;
    Integer timeout;
    Integer limit;

    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
        this.shutdown = shutdown;
        this.timeout = timeout;
        this.limit = limit;
    }

    public byte[] askServer(String hostname, int port, byte [] toServerBytes) throws IOException {

        //Create socket
        Socket clientSocket = new Socket(hostname, port);
        
        //Creates dynamic byte array for server response
        ByteArrayOutputStream byteArr = new ByteArrayOutputStream(); 

        //Create input stream
        InputStream inStream = clientSocket.getInputStream();

        //Create output stream
        clientSocket.getOutputStream().write(toServerBytes);

        if (shutdown) {
            clientSocket.shutdownOutput();
        }

        clientSocket.setSoTimeout(timeout == null ? 0 : timeout);

        int limitResponse = limit == null ? 1 : limit;
        int bytesRead = 0;
        
        try {
            int temp = inStream.read();
            while(temp != -1 && bytesRead < limitResponse){
                byteArr.write(temp);
                temp = inStream.read();
                if (limit != null) {
                    bytesRead ++;
                }
            }
        } catch (SocketTimeoutException e) {
            //Timeout
        }
        //Close the connection
        clientSocket.close();

        //Returns the response from the server
        return byteArr.toByteArray();
    }
}
