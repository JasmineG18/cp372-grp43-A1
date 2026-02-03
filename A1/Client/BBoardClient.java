package A1.Client;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class BBoardClient extends JFrame {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private JTextArea output = new JTextArea(15, 40);

    public BBoardClient() {
        setTitle("Bulletin Board Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JButton connect = new JButton("Connect");
        JButton post = new JButton("POST");
        JButton get = new JButton("GET");
        JButton pin = new JButton("PIN");
        JButton unpin = new JButton("UNPIN");
        JButton shake = new JButton("SHAKE");
        JButton clear = new JButton("CLEAR");

        JPanel buttons = new JPanel();
        buttons.add(connect);
        buttons.add(post);
        buttons.add(get);
        buttons.add(pin);
        buttons.add(unpin);
        buttons.add(shake);
        buttons.add(clear);

        add(new JScrollPane(output), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        connect.addActionListener(e -> connect());
        post.addActionListener(e -> send("POST 2 3 white Hello"));
        get.addActionListener(e -> send("GET"));
        pin.addActionListener(e -> send("PIN 2 3"));
        unpin.addActionListener(e -> send("UNPIN 2 3"));
        shake.addActionListener(e -> send("SHAKE"));
        clear.addActionListener(e -> send("CLEAR"));

        pack();
        setVisible(true);
    }

    private void connect() {
        try {
            socket = new Socket("localhost", 4554);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            output.append("Connected: " + in.readLine() + "\n");
        } catch (IOException e) {
            output.append("Connection failed\n");
        }
    }

    private void send(String cmd) {
        out.println(cmd);
        try {
            output.append(in.readLine() + "\n");
        } catch (IOException e) {
            output.append("Disconnected\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BBoardClient::new);
    }
}


