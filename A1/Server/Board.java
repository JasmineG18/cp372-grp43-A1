import java.util.*;

public class Board {

    // Board dimensions and note size
    private final int width, height, noteW, noteH;

    // Allowed note colours
    private final Set<String> colours;

    // All notes currently on the board
    private final List<Note> notes = new ArrayList<>();

    // Constructor initializes board settings
    public Board(int w, int h, int nw, int nh, Set<String> colours) {
        this.width = w;
        this.height = h;
        this.noteW = nw;
        this.noteH = nh;
        this.colours = colours;
    }

    // Returns board configuration when a client first connects
    public synchronized String handshake() {
        return width + " " + height + " " + noteW + " " + noteH + " " + String.join(" ", colours);
    }

    // Handles POST command to add a new note
    public synchronized String post(String cmd) throws ProtocolException {
        String[] parts = cmd.split(" ", 5);
        if (parts.length < 5) throw Errors.INVALID_FORMAT.exception();

        int x = Integer.parseInt(parts[1]);     // Note top-left x
        int y = Integer.parseInt(parts[2]);     // Note top-left y
        String colour = parts[3];               // Note colour
        String content = parts[4];              // Note message text

        // Validate colour and board boundaries
        if (!colours.contains(colour)) throw Errors.COLOUR_NOT_SUPPORTED.exception();
        if (x < 0 || y < 0 || x + noteW > width || y + noteH > height)
            throw Errors.OUT_OF_BOUNDS.exception();

        Note newNote = new Note(x, y, noteW, noteH, colour, content);

        // Reject if it completely overlaps an existing note
        for (Note n : notes) {
            if (n.completelyOverlaps(newNote))
                throw Errors.COMPLETE_OVERLAP.exception();
        }

        notes.add(newNote); // Add note to board
        return "OK POSTED";
    }

    // Handles PIN command to pin a note at a coordinate
    public synchronized String pin(String cmd) throws ProtocolException {
        String[] p = cmd.split(" ");
        if (p.length != 3) throw Errors.INVALID_FORMAT.exception();

        int x = Integer.parseInt(p[1]);
        int y = Integer.parseInt(p[2]);

        boolean pinned = false;

        // Pin any note that contains this coordinate
        for (Note n : notes) {
            if (n.contains(x, y)) {
                n.addPin(x, y);
                pinned = true;
            }
        }

        if (!pinned) throw Errors.NO_NOTE_AT_COORDINATE.exception();
        return "OK PINNED";
    }

    // Handles UNPIN command to remove a pin
    public synchronized String unpin(String cmd) throws ProtocolException {
        String[] p = cmd.split(" ");
        if (p.length != 3) throw Errors.INVALID_FORMAT.exception();

        int x = Integer.parseInt(p[1]);
        int y = Integer.parseInt(p[2]);

        boolean removed = false;

        // Try removing pin from all notes
        for (Note n : notes) {
            removed |= n.removePin(x, y);
        }

        if (!removed) throw Errors.PIN_NOT_FOUND.exception();
        return "OK UNPINNED";
    }

    // Removes all notes that have no pins
    public synchronized String shake() {
        notes.removeIf(n -> !n.isPinned());
        return "OK SHAKEN";
    }

    // Clears the entire board
    public synchronized String clear() {
        notes.clear();
        return "OK CLEARED";
    }

    // Returns all notes on the board
    public synchronized String get(String cmd) {
        StringBuilder sb = new StringBuilder();

        for (Note n : notes) {
            sb.append(n).append("\n");
        }

        return sb.length() == 0 ? "OK No matching entries" : sb.toString().trim();
    }
}
