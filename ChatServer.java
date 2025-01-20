import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Set<ClientHandler> clients = new HashSet<>();
    private static final Map<String, ClientHandler> usernameMap = new ConcurrentHashMap<>();
    private static final String LOG_FILE = "chat_log.txt";
    
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);
            
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void logMessage(String username, String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(new Date() + " [" + username + "]: " + message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static synchronized void broadcastMessage(String message, ClientHandler sender) {
        String formattedMessage = sender.getUsername() + ": " + message;
        
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(formattedMessage);
            }
        }
        logMessage(sender.getUsername(), message);
    }
    
    public static synchronized void sendPrivateMessage(String username, String message, ClientHandler sender) {
        ClientHandler recipient = usernameMap.get(username);
        if (recipient != null) {
            recipient.sendMessage("Private message from " + sender.getUsername() + ": " + message);
            sender.sendMessage("Private message to " + username + ": " + message);
            logMessage(sender.getUsername(), "Private to " + username + ": " + message);
        } else {
            sender.sendMessage("User " + username + " not found.");
        }
    }
    
    public static synchronized void addClient(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }
    
    public static synchronized void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        usernameMap.remove(clientHandler.getUsername());
        broadcastMessage(clientHandler.getUsername() + " has left the chat.", clientHandler);
    }
    
    public static synchronized void listActiveUsers(ClientHandler clientHandler) {
        StringBuilder usersList = new StringBuilder("Active users: ");
        for (String username : usernameMap.keySet()) {
            usersList.append(username).append(", ");
        }
        if (usersList.length() > 0) {
            usersList.setLength(usersList.length() - 2);
        }
        clientHandler.sendMessage(usersList.toString());
    }
    
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // Request username
                out.println("Enter a unique username:");
                username = in.readLine().trim();
                
                synchronized (usernameMap) {
                    while (usernameMap.containsKey(username)) {
                        out.println("Username already taken. Enter a different username:");
                        username = in.readLine().trim();
                    }
                    usernameMap.put(username, this);
                    addClient(this);
                    broadcastMessage(username + " has joined the chat.", this);
                    out.println("Welcome to the chat, " + username + "!");
                }
                
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("@")) {
                        int index = message.indexOf(":");
                        if (index != -1) {
                            String targetUser = message.substring(1, index).trim();
                            String privateMessage = message.substring(index + 1).trim();
                            sendPrivateMessage(targetUser, privateMessage, this);
                        }
                    } else if (message.equals("/users")) {
                        listActiveUsers(this);
                    } else {
                        broadcastMessage(message, this);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                removeClient(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        public String getUsername() {
            return username;
        }
        
        public void sendMessage(String message) {
            out.println(message);
        }
    }
}