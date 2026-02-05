import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

//Cleint for the Bulletin Board system which handles user interface, networking, and visual display

public class BBoardClient extends JFrame {

    // Network communication objects
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    //UI components
    private JTextArea output = new JTextArea(15, 40);
    private BoardPanel boardPanel = new BoardPanel();

    //Board dimensions received from the server handshake
    private int boardW, boardH, noteW, noteH;

    //GUI and event handlers
    public BBoardClient() {
        setTitle("Bulletin Board Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //Output text area is read-only
        output.setEditable(false);

        //Control buttons
        JButton connectBtn = new JButton("Connect");
        JButton postBtn = new JButton("POST");
        JButton getBtn = new JButton("GET");
        JButton pinBtn = new JButton("PIN");
        JButton unpinBtn = new JButton("UNPIN");
        JButton shakeBtn = new JButton("SHAKE");
        JButton clearBtn = new JButton("CLEAR");

        //Button panel
        JPanel buttons = new JPanel();
        buttons.add(connectBtn);
        buttons.add(postBtn);
        buttons.add(getBtn);
        buttons.add(pinBtn);
        buttons.add(unpinBtn);
        buttons.add(shakeBtn);
        buttons.add(clearBtn);

        // split view for board on top, output log on bottom
        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                boardPanel,
                new JScrollPane(output)
        );
        split.setResizeWeight(0.7);
        add(split, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        //Button actions
        connectBtn.addActionListener(e -> connect());
        postBtn.addActionListener(e -> doPost());
        getBtn.addActionListener(e -> sendAndDisplay("GET"));
        pinBtn.addActionListener(e -> doPin(true));
        unpinBtn.addActionListener(e -> doPin(false));
        shakeBtn.addActionListener(e -> sendAndDisplay("SHAKE"));
        clearBtn.addActionListener(e -> sendAndDisplay("CLEAR"));

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Connects Client to server and processes the handshake
    private void connect() {
        try {
            // prompt user for host and port
            JTextField hostF = new JTextField("localhost");
            JTextField portF = new JTextField("4554");
            Object[] fields = {"Host:", hostF, "Port:", portF};

            int ok = JOptionPane.showConfirmDialog(this, fields, "Connect", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;

            String host = hostF.getText().trim();
            int port = Integer.parseInt(portF.getText().trim());

            //Open socket and streams
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // Read hankshake line from server
            String handshake = in.readLine();
            output.append("Connected: " + handshake + "\n");

            //Parse board dimensions and configue display
            parseHandshake(handshake);
            boardPanel.setDimensions(boardW, boardH, noteW, noteH);

            //Load any existing notes
            refreshBoard(); 
        } catch (Exception e) {
            output.append("Connection failed: " + e.getMessage() + "\n");
            closeSocket();
        }
    }

    // Handles POST command input and send it to the server
    private void doPost() {
        if (!isConnected()) {
            output.append("Not connected.\n");
            return;
        }

        JTextField xF = new JTextField();
        JTextField yF = new JTextField();
        JTextField colourF = new JTextField();
        JTextField msgF = new JTextField();

        Object[] fields = {"x:", xF, "y:", yF, "colour:", colourF, "message:", msgF};
        int ok = JOptionPane.showConfirmDialog(this, fields, "POST", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        //Build POST command in format POST x y colour message
        String cmd = "POST " + xF.getText().trim() + " " + yF.getText().trim() + " "
                + colourF.getText().trim() + " " + msgF.getText();

        sendAndDisplay(cmd);
    }

    //Handles PIN and UNPIN commands
    private void doPin(boolean pin) {
        if (!isConnected()) {
            output.append("Not connected.\n");
            return;
        }

        JTextField xF = new JTextField();
        JTextField yF = new JTextField();
        Object[] fields = {"x:", xF, "y:", yF};

        int ok = JOptionPane.showConfirmDialog(this, fields, pin ? "PIN" : "UNPIN", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String cmd = (pin ? "PIN " : "UNPIN ") + xF.getText().trim() + " " + yF.getText().trim();
        sendAndDisplay(cmd);
    }

    //Send a command to server and display the response
    private void sendAndDisplay(String cmd) {
        if (!isConnected()) {
            output.append("Not connected.\n");
            return;
        }

        try {
            output.append(">> " + cmd + "\n");
            out.println(cmd);

            // Read the server response (supports multi-line GET)
            List<String> lines = readResponseBlock();

            for (String s : lines) output.append(s + "\n");

            // After any board-changing command, refresh the visual board
            if (cmd.startsWith("POST") || cmd.startsWith("PIN") || cmd.startsWith("UNPIN")
                    || cmd.equals("SHAKE") || cmd.equals("CLEAR")) {
                refreshBoard();
            }

        } catch (Exception e) {
            output.append("Disconnected.\n");
            closeSocket();
        }
    }

    //Refreshes the board display by issuing a GET command
    private void refreshBoard() {
        if (!isConnected()) return;

        try {
            out.println("GET");

            List<String> lines = readResponseBlock();

            // If GET returns OK, then no notes
            if (lines.size() == 1 && (lines.get(0).startsWith("OK") || lines.get(0).startsWith("ERROR"))) {
                boardPanel.setNotes(new ArrayList<>());
                return;
            }

            //Parse note lines
            List<ClientNote> parsed = new ArrayList<>();
            for (String line : lines) {
                ClientNote n = parseNoteLine(line);
                if (n != null) parsed.add(n);
            }

            boardPanel.setNotes(parsed);

        } catch (Exception ignored) {
            // Fail silently on refresh
        }
    }

    //Reads a full server response block, including multi-line GET responses
    private List<String> readResponseBlock() throws IOException {
       
        List<String> lines = new ArrayList<>();

        String first = in.readLine();
        if (first == null) throw new IOException("Socket closed");

        lines.add(first);

        // Single line responses
        if (first.startsWith("OK") || first.startsWith("ERROR")) return lines;

        // attempt to read additional lines with short timeout
        int oldTimeout = socket.getSoTimeout();
        socket.setSoTimeout(80);

        while (true) {
            try {
                String nxt = in.readLine();
                if (nxt == null) break;
                if (nxt.trim().isEmpty()) break;
                // If server ever sent OK/ERROR after data (unlikely), stop
                if (nxt.startsWith("OK") || nxt.startsWith("ERROR")) break;
                lines.add(nxt);
            } catch (SocketTimeoutException timeout) {
                break; // no more lines immediately available
            }
        }

        socket.setSoTimeout(oldTimeout);
        return lines;
    }

    //Parses handshake data sent by the server
    private void parseHandshake(String handshake) {
        // handshake format: "boardW boardH noteW noteH colour1 colour2 ..."
        String[] p = handshake.trim().split("\\s+");
        boardW = Integer.parseInt(p[0]);
        boardH = Integer.parseInt(p[1]);
        noteW  = Integer.parseInt(p[2]);
        noteH  = Integer.parseInt(p[3]);
    }

    //Converts a server note line into a ClientNote object
    private ClientNote parseNoteLine(String line) {
        // expected: x y colour PINNED|UNPINNED message...
        try {
            String[] parts = line.split(" ", 5);
            if (parts.length < 5) return null;

            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            String colour = parts[2];
            boolean pinned = parts[3].equals("PINNED");
            String msg = parts[4];

            return new ClientNote(x, y, colour, pinned, msg);
        } catch (Exception e) {
            return null;
        }
    }

    //Checks whether the client is currently connected
    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    //Closes all network resources safely
    private void closeSocket() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        in = null;
        out = null;
        socket = null;
    }

    //Main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BBoardClient::new);
    }
}
