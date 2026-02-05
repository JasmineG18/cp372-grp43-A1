// Source code is decompiled from a .class file using FernFlower decompiler (from IntelliJ IDEA).

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
 * Main server class for the Bulletin Board system.
 * This class starts the server, initializes the board,
 * and listens for incoming client connections.
 */
public class BBoard {

   // Default constructor (not used, but included by Java)
   public BBoard() {
   }

   /**
    * Program entry point.
    * Expected command line arguments:
    * 0 - Port number
    * 1 - Board width
    * 2 - Board height
    * 3 - Note width
    * 4 - Note height
    * 5+ - Supported note colors
    */
   public static void main(String[] var0) throws IOException {

      // Ensure the correct number of arguments were provided
      if (var0.length < 6) {
         System.err.println("Usage: java BBoard <port> <board_w> <board_h> <note_w> <note_h> <colors...>");
         System.exit(1); // Exit if arguments are missing
      }

      // Parse command line arguments into integers
      int var1 = Integer.parseInt(var0[0]); // Server port number
      int var2 = Integer.parseInt(var0[1]); // Board width
      int var3 = Integer.parseInt(var0[2]); // Board height
      int var4 = Integer.parseInt(var0[3]); // Note width
      int var5 = Integer.parseInt(var0[4]); // Note height

      // Store all allowed note colors in a HashSet for quick lookup
      HashSet<String> var6 = new HashSet<>();

      // Add each color (starting from argument index 5) into the set
      for (int var7 = 5; var7 < var0.length; ++var7) {
         var6.add(var0[var7]);
      }

      // Create the shared Board object that all clients will interact with
      Board var10 = new Board(var2, var3, var4, var5, var6);

      // Create a server socket that listens for client connections on the given port
      ServerSocket var8 = new ServerSocket(var1);
      System.out.println("Bulletin Board Server running on port " + var1);

      // Main server loop — runs forever
      while (true) {

         // Wait (block) until a client connects
         Socket var9 = var8.accept();

         // Create a new thread to handle this client connection
         // ClientHandler is responsible for processing client commands
         (new Thread(new ClientHandler(var9, var10))).start();
      }
   }
}
