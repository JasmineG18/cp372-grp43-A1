package A1.Server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Board board;

    public ClientHandler(Socket socket, Board board) {
        this.socket = socket;
        this.board = board;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Handshake
            out.println(board.handshake());

            String line;
            while ((line = in.readLine()) != null) {
                String response = handleCommand(line.trim());
                out.println(response);
                if (line.startsWith("DISCONNECT")) break;
            }
        } catch (IOException e) {
            // Client disconnected unexpectedly
        }
    }

    private String handleCommand(String cmd) {
        try {
            if (cmd.startsWith("POST")) return board.post(cmd);
            if (cmd.startsWith("GET")) return board.get(cmd);
            if (cmd.startsWith("PIN")) return board.pin(cmd);
            if (cmd.startsWith("UNPIN")) return board.unpin(cmd);
            if (cmd.equals("SHAKE")) return board.shake();
            if (cmd.equals("CLEAR")) return board.clear();
            if (cmd.equals("DISCONNECT")) return "OK DISCONNECTED";

            return Errors.INVALID_FORMAT.message("Unknown command");
        } catch (ProtocolException e) {
            return e.getMessage();
        }
    }
}
