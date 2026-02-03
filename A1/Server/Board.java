package A1.Server;

import java.util.*;

public class Board {

    private final int width, height, noteW, noteH;
    private final Set<String> colours;
    private final List<Note> notes = new ArrayList<>();

    public Board(int w, int h, int nw, int nh, Set<String> colours) {
        this.width = w;
        this.height = h;
        this.noteW = nw;
        this.noteH = nh;
        this.colours = colours;
    }

    public synchronized String handshake() {
        return width + " " + height + " " + noteW + " " + noteH + " " + String.join(" ", colours);
    }

    public synchronized String post(String cmd) throws ProtocolException {
        String[] parts = cmd.split(" ", 5);
        if (parts.length < 5) throw Errors.INVALID_FORMAT.exception();

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        String colour = parts[3];
        String content = parts[4];

        if (!colours.contains(colour)) throw Errors.COLOUR_NOT_SUPPORTED.exception();
        if (x < 0 || y < 0 || x + noteW > width || y + noteH > height)
            throw Errors.OUT_OF_BOUNDS.exception();

        Note newNote = new Note(x, y, noteW, noteH, colour, content);

        for (Note n : notes) {
            if (n.completelyOverlaps(newNote))
                throw Errors.COMPLETE_OVERLAP.exception();
        }

        notes.add(newNote);
        return "OK POSTED";
    }

    public synchronized String pin(String cmd) throws ProtocolException {
        String[] p = cmd.split(" ");
        int x = Integer.parseInt(p[1]);
        int y = Integer.parseInt(p[2]);

        boolean pinned = false;
        for (Note n : notes) {
            if (n.contains(x, y)) {
                n.addPin(x, y);
                pinned = true;
            }
        }
        if (!pinned) throw Errors.NO_NOTE_AT_COORDINATE.exception();
        return "OK PINNED";
    }

    public synchronized String unpin(String cmd) throws ProtocolException {
        String[] p = cmd.split(" ");
        int x = Integer.parseInt(p[1]);
        int y = Integer.parseInt(p[2]);

        boolean removed = false;
        for (Note n : notes) {
            removed |= n.removePin(x, y);
        }
        if (!removed) throw Errors.PIN_NOT_FOUND.exception();
        return "OK UNPINNED";
    }

    public synchronized String shake() {
        notes.removeIf(n -> !n.isPinned());
        return "OK SHAKEN";
    }

    public synchronized String clear() {
        notes.clear();
        return "OK CLEARED";
    }

    public synchronized String get(String cmd) {
        StringBuilder sb = new StringBuilder();
        for (Note n : notes) {
            sb.append(n).append("\n");
        }
        return sb.length() == 0 ? "OK No matching entries" : sb.toString().trim();
    }
}
