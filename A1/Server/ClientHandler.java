import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    // Socket connected to a single client
    private final Socket socket;

    // Shared board object
    private final Board board;

    // Constructor stores client socket and shared board
    public ClientHandler(Socket socket, Board board) {
        this.socket = socket;
        this.board = board;
    }

    @Override
    public void run() {
        try (
            // Input stream from client
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Output stream to client (auto-flush enabled)
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Send initial board configuration to client
            out.println(board.handshake());

            String line;
            // Continuously read client commands
            while ((line = in.readLine()) != null) {
                String response = handleCommand(line.trim()); // Process command
                out.println(response); // Send response back to client

                // End session if client disconnects
                if (line.startsWith("DISCONNECT")) break;
            }
        } catch (IOException e) {
            // Client disconnected unexpectedly or network error
        }
    }

    // Determines which board operation to execute
    private String handleCommand(String cmd) {
        try {
            if (cmd.startsWith("POST")) return board.post(cmd);
            if (cmd.startsWith("GET")) return board.get(cmd);
            if (cmd.startsWith("PIN")) return board.pin(cmd);
            if (cmd.startsWith("UNPIN")) return board.unpin(cmd);
            if (cmd.equals("SHAKE")) return board.shake();
            if (cmd.equals("CLEAR")) return board.clear();
            if (cmd.equals("DISCONNECT")) return "OK DISCONNECTED";

            // Unknown command format
            return Errors.INVALID_FORMAT.message("Unknown command");
        } catch (ProtocolException e) {
            // Return protocol-specific error message
            return e.getMessage();
        }
    }
}
