package lab.dcs.net.datagram;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * UDP chat terminal. Two identical instances talk to each other.
 *
 * Each terminal owns ONE DatagramSocket bound to its local port and runs two threads:
 *   - SenderThread   : waits for messages typed in the GUI (via a queue) and sends them
 *   - ReceiverThread : blocks on socket.receive() and shows incoming messages
 *
 * Usage:
 *   java lab.dcs.net.datagram.ChatTerminal <localPort> <remoteHost> <remotePort> [name]
 *
 * Example (two terminals on the same machine):
 *   java lab.dcs.net.datagram.ChatTerminal 5000 localhost 5001 Alice
 *   java lab.dcs.net.datagram.ChatTerminal 5001 localhost 5000 Bob
 *
 * Running with no arguments opens both terminals in one JVM for a quick test.
 */
public class ChatTerminal extends JFrame {

    private static final int BUF_SIZE = 1024;

    private final DatagramSocket socket;
    private final InetAddress remoteAddress;
    private final int remotePort;
    private final String name;

    // GUI
    private final JTextArea chatArea = new JTextArea(18, 40);
    private final JTextField inputField = new JTextField(30);
    private final JButton sendButton = new JButton("Send");

    private final BlockingQueue<String> outgoing = new LinkedBlockingQueue<>();

    private final Thread senderThread;
    private final Thread receiverThread;
    private volatile boolean running = true;

    public ChatTerminal(int localPort, String remoteHost, int remotePort, String name) throws IOException {
        super(name + "  (UDP :" + localPort + " -> " + remoteHost + ":" + remotePort + ")");
        this.socket = new DatagramSocket(localPort);
        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        this.name = name;

        buildGui();

        senderThread = new Thread(this::sendLoop, name + "-Sender");
        receiverThread = new Thread(this::receiveLoop, name + "-Receiver");
        senderThread.start();
        receiverThread.start();

        appendMessage("System", "Listening on port " + localPort + ", sending to "
                + remoteHost + ":" + remotePort);
    }


    private void buildGui() {
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> queueMessage());
        inputField.addActionListener(e -> queueMessage());

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                shutdown();
            }
        });

        pack();
        setLocationByPlatform(true);
    }

    private void queueMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        outgoing.offer(text);
        inputField.setText("");
        inputField.requestFocusInWindow();
    }

    private void appendMessage(String who, String text) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + time + "] " + who + ": " + text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }



    private void sendLoop() {
        while (running) {
            try {
                String text = outgoing.take();          // blocks until user sends something
                String payload = name + ": " + text;
                byte[] buf = payload.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, remoteAddress, remotePort);
                socket.send(packet);
                appendMessage("Me", text);
            } catch (InterruptedException e) {
                break;                                   // window closed
            } catch (IOException e) {
                if (running) appendMessage("System", "Send failed: " + e.getMessage());
            }
        }
    }

    private void receiveLoop() {String text = outgoing.take();          // blocks until user sends something
        String payload = name +
        byte[] buf = new byte[BUF_SIZE];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                int sep = received.indexOf(": ");
                if (sep > 0) {
                    appendMessage(received.substring(0, sep), received.substring(sep + 2));
                } else {
                    appendMessage(packet.getAddress().getHostAddress() + ":" + packet.getPort(), received);
                }
            } catch (SocketException e) {
                break;
            } catch (IOException e) {
                if (running) appendMessage("System", "Receive failed: " + e.getMessage());
            }
        }
    }

    private void shutdown() {
        running = false;
        senderThread.interrupt();   // wakes the sender from outgoing.take()
        socket.close();             // wakes the receiver from socket.receive()
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (args.length >= 3) {
                    int localPort = Integer.parseInt(args[0]);
                    String remoteHost = args[1];
                    int remotePort = Integer.parseInt(args[2]);
                    String name = args.length >= 4 ? args[3] : "User" + localPort;
                    new ChatTerminal(localPort, remoteHost, remotePort, name).setVisible(true);
                } else {
                    // Demo: two identical terminals talking to each other on this machine
                    new ChatTerminal(5000, "localhost", 5001, "Terminal A").setVisible(true);
                    new ChatTerminal(5001, "localhost", 5000, "Terminal B").setVisible(true);
                }
            } catch (IOException | NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Could not start chat: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.out.println("Usage: java lab.dcs.net.datagram.ChatTerminal "
                        + "<localPort> <remoteHost> <remotePort> [name]");
            }
        });
    }
}