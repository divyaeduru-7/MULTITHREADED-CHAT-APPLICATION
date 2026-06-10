import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 6666;
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        System.out.println("Chat Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected: " + socket.getInetAddress());
                
                ClientHandler handler = new ClientHandler(socket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers) {
                if (handler != sender) {
                    handler.sendMessage(message);
                }
            }
        }
    }

    public static void removeClient(ClientHandler handler) {
        clientHandlers.remove(handler);
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String userName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Enter your name: ");
            userName = in.readLine();
            if (userName == null) return;
            
            ChatServer.broadcast(userName + " has joined the chat!", this);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("/quit")) break;
                ChatServer.broadcast(userName + ": " + message, this);
            }
        } catch (IOException e) {
            System.err.println("User error: " + userName + " disconnected unexpectedly.");
        } finally {
            ChatServer.removeClient(this);
            ChatServer.broadcast(userName + " has left the chat.", this);
            try { 
                socket.close(); 
            } catch (IOException e) { 
                System.err.println("Error closing socket: " + e.getMessage()); 
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
