import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

class RequestHandler extends Thread {
    final private InputStream inputStream;
    final private OutputStream outputStream;
    final private String fileDir;
    final private Socket clientSocket;  // Add the client socket to get the IP address

    // Constructor to initialize the socket and other parameters
    RequestHandler(InputStream inputStream, OutputStream outputStream,
                   String fileDir, Socket clientSocket) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.fileDir = fileDir == null ? "" : fileDir + File.separator;
        this.clientSocket = clientSocket;
    }

    public void run() {
        try {
            // Read the request from the client
            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(inputStream));
            String requestLine = bufferedReader.readLine();
            Map<String, String> requestHeaders = new HashMap<>();
            String header;
            while ((header = bufferedReader.readLine()) != null &&
                    !header.isEmpty()) {
                String[] keyVal = header.split(":", 2);
                if (keyVal.length == 2) {
                    requestHeaders.put(keyVal[0], keyVal[1].trim());
                }
            }

            // Read the body of the request (if any)
            StringBuilder bodyBuffer = new StringBuilder();
            while (bufferedReader.ready()) {
                bodyBuffer.append((char) bufferedReader.read());
            }
            String body = bodyBuffer.toString();

            // Split the request line to extract method, target, and version
            String[] requestLinePieces = requestLine.split(" ", 3);
            String httpMethod = requestLinePieces[0];
            String requestTarget = requestLinePieces[1];
            // String httpVersion = requestLinePieces[2];

            // Get the client's IP address
            String clientIP = clientSocket.getInetAddress().getHostAddress();

            // Get the number of active connections
            int activeConnections = Main.getActiveConnections();

            // Process different HTTP methods and routes
            if ("POST".equals(httpMethod)) {
                if (requestTarget.startsWith("/files/")) {
                    // Example: POST /files/hello.txt
                    // Creates a file with the content from the body
                    File file = new File(fileDir + requestTarget.substring(7));
                    if (file.createNewFile()) {
                        FileWriter fileWriter = new FileWriter(file);
                        fileWriter.write(body);
                        fileWriter.close();
                    }
                    outputStream.write("HTTP/1.1 201 Created\r\n\r\n".getBytes());
                } else {
                    // For invalid targets, return 404 Not Found
                    outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                }
                outputStream.flush();
                outputStream.close();
                return;
            }

            // Handle GET requests for various routes
            if (requestTarget.equals("/")) {
                // Example: GET /
                // A simple endpoint that returns 200 OK
                outputStream.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
            } else if (requestTarget.startsWith("/echo/")) {
                // Example: GET /echo/HelloWorld
                // Echoes the string in the URL
                String echoString = requestTarget.substring(6);
                String outputString = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "Content-Length: " + echoString.length() +
                        "\r\n"
                        + "\r\n" + echoString;
                outputStream.write(outputString.getBytes());
            } else if (requestTarget.equals("/user-agent")) {
                // Example: GET /user-agent (with User-Agent header)
                // Returns the User-Agent header from the request
                String userAgent = requestHeaders.get("User-Agent");

                // Enhanced response with IP address and active connections count
                String outputString =
                        "HTTP/1.1 200 OK\r\n"
                                + "Content-Type: text/plain\r\n"
                                + "Content-Length: " + (userAgent.length() + 50) +  // Adjust content length to account for new data
                                "\r\n"
                                + "\r\n"
                                + "User-Agent: " + userAgent + "\r\n"
                                + "Client IP: " + clientIP + "\r\n"
                                + "Active Connections: " + activeConnections;

                outputStream.write(outputString.getBytes());
            } else if (requestTarget.startsWith("/files/")) {
                // Example: GET /files/hello.txt
                // Returns the content of the requested file
                String fileName = requestTarget.substring(7);
                FileReader fileReader;
                try {
                    fileReader = new FileReader(fileDir + fileName);
                } catch (FileNotFoundException e) {
                    // If the file doesn't exist, return 404 Not Found
                    outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                    outputStream.flush();
                    outputStream.close();
                    return;
                }

                BufferedReader bufferedFileReader = new BufferedReader(fileReader);
                StringBuilder stringBuffer = new StringBuilder();
                String line;
                while ((line = bufferedFileReader.readLine()) != null) {
                    stringBuffer.append(line);
                }
                bufferedFileReader.close();
                fileReader.close();
                String outputString = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: application/octet-stream\r\n"
                        + "Content-Length: " + stringBuffer.length() +
                        "\r\n"
                        + "\r\n" + stringBuffer;
                outputStream.write(outputString.getBytes());
            } else {
                // For any unrecognized requests, return 404 Not Found
                outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            }

            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            // Decrement active connections when done
            Main.decrementActiveConnections();
        }
    }
}

public class Main {
    private static int activeConnections = 0;  // Static variable to track active connections

    public static void main(String[] args) {
        ServerSocket serverSocket;
        Socket clientSocket;
        try {
            String directoryString = null;
            if (args.length > 1 && "--directory".equals(args[0])) {
                directoryString = args[1];
            }
            // Start the server on port 4221
            serverSocket = new ServerSocket(4221);
            serverSocket.setReuseAddress(true);
            while (true) {
                clientSocket = serverSocket.accept();
                synchronized (Main.class) {
                    activeConnections++;  // Increment active connections on new connection
                }
                System.out.println("Accepted new connection. Active connections: " + activeConnections);

                // Create a new request handler for each incoming connection
                RequestHandler handler =
                        new RequestHandler(clientSocket.getInputStream(),
                                clientSocket.getOutputStream(), directoryString, clientSocket);
                handler.start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    // Static method to decrement active connections when a request is done
    public static synchronized void decrementActiveConnections() {
        activeConnections--;
    }

    public static synchronized int getActiveConnections() {
        return activeConnections;
    }
}