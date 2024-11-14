# Custom HTTP Server

This project implements a basic HTTP server in Java that demonstrates key concepts of handling HTTP requests, managing active connections, and serving both static and dynamic content. It is designed as a "Build Your Own X" project, allowing you to understand how a web server operates at a low level by managing socket connections, HTTP methods (GET, POST), file handling, and HTTP headers.

## Table of Contents
- [Project Overview](#project-overview)
- [Features](#features)
- [How It Works](#how-it-works)
- [Directory Structure](#directory-structure)
- [Usage](#usage)
- [End Points](#end-points)

## Project Overview

This **Custom HTTP Server** is a simple implementation of an HTTP server that listens on port `4221`. It handles both **GET** and **POST** requests, including:
- Serving static files from a directory.
- Allowing file creation through **POST** requests.
- Providing dynamic responses like echoing strings and showing client information.

The server is built using basic **sockets** in Java and includes mechanisms for tracking active connections.

## Features

- **Handle GET and POST requests**:
    - Serve static files via GET requests.
    - Accept file content creation via POST requests.

- **Echo Functionality**:
    - The server can echo back a string sent in the URL path.

- **User-Agent Header**:
    - Return the `User-Agent` header along with client IP and active connections.

- **Dynamic File Handling**:
    - Handle file uploads through POST requests to create files in the specified directory.

- **Active Connection Management**:
    - Track and return the number of active connections to the server.

## How It Works

The **Main** class starts a **ServerSocket** listening on port `4221`. When a connection is received, it spawns a new **RequestHandler** thread to handle the incoming HTTP request.

### Request Handling Process:
1. **Request Parsing**:
    - Reads the incoming HTTP request, including the method (`GET` or `POST`), request target (URL), and headers.

2. **Handling Specific Routes**:
    - For **GET** requests:
        - Returns the content of a file if it exists.
        - Supports dynamic paths such as `/echo/{message}` to return the path parameter as text.
        - Returns the `User-Agent` header with additional information (client IP and active connections).

    - For **POST** requests:
        - Allows file creation in the specified directory.
        - Responds with an HTTP status code (e.g., `201 Created` for successful file creation).

3. **Connection Management**:
    - Each request increments the active connection count, and once the request is processed, the connection count is decremented.

## Directory Structure

```bash
/custom-http-server
├── src
│   ├── Main.java         # Main server class that starts the server
│   └── RequestHandler.java  # Handles incoming HTTP requests
├── README.md            # This documentation
└── build/                # Compiled classes (generated after build)
```

## Usage
1. **Clone the repository**:
```bash
git clone https://github.com/sagar-cpp/custom-http-server.git 
```
2. **Build the project (use your preferred Java IDE or command line)**:
```bash
javac src/*.java
```
3. **Run the server**:
    - To run the server with the default settings (no directory specified):
   ```bash
   Java Main 
   ```
   
    - To specify a custom directory for serving files:
   ```bash
   java Main --directory /path/to/directory
   ```

4. **Make Requests**:
    - Use a browser or `curl` to make HTTP requests to `http://localhost:4221`:
        - `GET /`: Basic response.
        - `GET /echo/{message}`: Echoes the `message` parameter.
        - `GET /user-agent`: Returns the `User-Agent` header along with client IP and active connections.
        - `POST /files/{filename}`: Creates a file with the content in the request body.

    *Example request:*

    ```bash
    curl -X POST --data "Hello, World!" http://localhost:4221/files/hello.txt
    ```

## End Point
**GET Endpoints**
- `/`: Returns a simple `200 OK` response.
- `/echo/{message}`: Echoes the string in the URL as the response.
    - Example: `GET /echo/HelloWorld` -> "**HelloWorld**"
- `/user-agent`: Returns the `User-Agent` header and additional information about the client.
    - Example Response:
    ```bash
    User-Agent: Mozilla/5.0
    Client IP: 127.0.0.1
    Active Connections: 5
    ```
- `/files/{filename}`: Serves a file from the server directory.
    - Example: `GET /files/example.txt` -> Contents of `example.txt`.

**POST Endpoints**
- `/files/{filename}`: Accepts a file upload and saves the file in the specified directory.
    - Example: `POST /files/hello.txt` with body content will create a file `hello.txt` on the server.