package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
    
    public TCPClient() {
        //Create an instance
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

        //writes input to the dynamic byte array
        byte [] buffer = new byte[1024];
        int temp = inStream.read(buffer);
        while(temp != -1){
            byteArr.write(buffer, 0, temp);
            temp = inStream.read(buffer);
        }

        //Close the connection
        clientSocket.close();

        //Returns the response from the server
        return byteArr.toByteArray();
    }
}
