import java.io.*;
import java.net.*;
import java.util.*;

public class BBoard {

    public static void main(String[] args) throws IOException {
        if (args.length < 6) {
            System.err.println("Usage: java BBoard <port> <board_w> <board_h> <note_w> <note_h> <colors...>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        int boardW = Integer.parseInt(args[1]);
        int boardH = Integer.parseInt(args[2]);
        int noteW = Integer.parseInt(args[3]);
        int noteH = Integer.parseInt(args[4]);

        Set<String> colours = new HashSet<>();
        for (int i = 5; i < args.length; i++) {
            colours.add(args[i]);
        }

        Board board = new Board(boardW, boardH, noteW, noteH, colours);
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("Bulletin Board Server running on port " + port);

        while (true) {
            Socket client = serverSocket.accept();
            new Thread(new ClientHandler(client, board)).start();
        }
    }
}
