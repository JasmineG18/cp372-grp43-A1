import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class BBoardClient extends JFrame {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JTextArea output = new JTextArea(15, 40);
    private BoardPanel boardPanel = new BoardPanel();

    private int boardW, boardH, noteW, noteH;

    public BBoardClient() {
        setTitle("Bulletin Board Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        output.setEditable(false);

        JButton connectBtn = new JButton("Connect");
        JButton postBtn = new JButton("POST");
        JButton getBtn = new JButton("GET");
        JButton pinBtn = new JButton("PIN");
        JButton unpinBtn = new JButton("UNPIN");
        JButton shakeBtn = new JButton("SHAKE");
        JButton clearBtn = new JButton("CLEAR");

        JPanel buttons = new JPanel();
        buttons.add(connectBtn);
        buttons.add(postBtn);
        buttons.add(getBtn);
        buttons.add(pinBtn);
        buttons.add(unpinBtn);
        buttons.add(shakeBtn);
        buttons.add(clearBtn);

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                boardPanel,
                new JScrollPane(output)
        );
        split.setResizeWeight(0.7);
        add(split, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

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

    private void connect() {
        try {
            // Basic connect dialog (so you’re not hardcoded)
            JTextField hostF = new JTextField("localhost");
            JTextField portF = new JTextField("4554");
            Object[] fields = {"Host:", hostF, "Port:", portF};

            int ok = JOptionPane.showConfirmDialog(this, fields, "Connect", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;

            String host = hostF.getText().trim();
            int port = Integer.parseInt(portF.getText().trim());

            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // Handshake is the FIRST line the server sends after connect
            String handshake = in.readLine();
            output.append("Connected: " + handshake + "\n");

            parseHandshake(handshake);
            boardPanel.setDimensions(boardW, boardH, noteW, noteH);

            refreshBoard(); // draw existing notes (if any)
        } catch (Exception e) {
            output.append("Connection failed: " + e.getMessage() + "\n");
            closeSocket();
        }
    }

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

        String cmd = "POST " + xF.getText().trim() + " " + yF.getText().trim() + " "
                + colourF.getText().trim() + " " + msgF.getText();

        sendAndDisplay(cmd);
    }

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

    private void sendAndDisplay(String cmd) {
        if (!isConnected()) {
            output.append("Not connected.\n");
            return;
        }

        try {
            output.append(">> " + cmd + "\n");
            out.println(cmd);

            // Read a full response block (handles multi-line GET)
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

    private void refreshBoard() {
        if (!isConnected()) return;

        try {
            out.println("GET");

            List<String> lines = readResponseBlock();

            // If GET returns OK ... then no notes
            if (lines.size() == 1 && (lines.get(0).startsWith("OK") || lines.get(0).startsWith("ERROR"))) {
                boardPanel.setNotes(new ArrayList<>());
                return;
            }

            List<ClientNote> parsed = new ArrayList<>();
            for (String line : lines) {
                ClientNote n = parseNoteLine(line);
                if (n != null) parsed.add(n);
            }

            boardPanel.setNotes(parsed);

        } catch (Exception ignored) {
            // If refresh fails, just don’t repaint
        }
    }

    private List<String> readResponseBlock() throws IOException {
        // We read the first line normally, then keep reading quickly for any extra lines.
        // This works with your server returning multi-line GET results (each line in the same response string).
        List<String> lines = new ArrayList<>();

        String first = in.readLine();
        if (first == null) throw new IOException("Socket closed");

        lines.add(first);

        // If it’s a simple one-line OK/ERROR, we’re done
        if (first.startsWith("OK") || first.startsWith("ERROR")) return lines;

        // Otherwise, it’s likely a multi-line data response. Read remaining lines with short timeout.
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

    private void parseHandshake(String handshake) {
        // handshake format: "boardW boardH noteW noteH colour1 colour2 ..."
        String[] p = handshake.trim().split("\\s+");
        boardW = Integer.parseInt(p[0]);
        boardH = Integer.parseInt(p[1]);
        noteW  = Integer.parseInt(p[2]);
        noteH  = Integer.parseInt(p[3]);
    }

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

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private void closeSocket() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        in = null;
        out = null;
        socket = null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BBoardClient::new);
    }
}
