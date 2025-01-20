import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClientGUI {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;

    public ChatClientGUI() {
        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            if (message.startsWith("@")) {
                int spaceIndex = message.indexOf(" ");
                if (spaceIndex != -1) {
                    String targetUser = message.substring(1, spaceIndex).trim();
                    String privateMessage = message.substring(spaceIndex + 1).trim();
                    message = "@" + targetUser + ": " + privateMessage;
                }
            }
            chatArea.append("You: " + message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            out.println(message);
            messageField.setText("");
        }
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Thread listenThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        chatArea.append(message + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            listenThread.start();

            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                chatArea.append(serverMessage + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
                if (serverMessage.contains("Enter a unique username:")) {
                    String username = JOptionPane.showInputDialog(frame, "Enter a unique username:");
                    out.println(username);
                    break;
                }
            }
            // socket.close();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Unable to connect to the server.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}
