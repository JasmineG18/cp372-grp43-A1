import java.util.*;

public class Note {

    // Top-left position and size of the note
    private final int x, y, w, h;

    // Note appearance and message
    private final String colour, content;

    // List of pins attached to this note
    private final List<Pin> pins = new ArrayList<>();

    // Constructor sets position, size, colour, and text
    public Note(int x, int y, int w, int h, String colour, String content) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.colour = colour;
        this.content = content;
    }

    // Checks if a coordinate lies within this note
    public boolean contains(int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    // Returns true if another note has the exact same position and size
    public boolean completelyOverlaps(Note other) {
        return this.x == other.x && this.y == other.y &&
               this.w == other.w && this.h == other.h;
    }

    // Adds a pin to this note
    public void addPin(int px, int py) {
        pins.add(new Pin(px, py));
    }

    // Removes a pin at the given coordinate
    public boolean removePin(int px, int py) {
        return pins.removeIf(p -> p.x == px && p.y == py);
    }

    // Returns true if the note has at least one pin
    public boolean isPinned() {
        return !pins.isEmpty();
    }

    // Formats note data for GET response
    @Override
    public String toString() {
        return x + " " + y + " " + colour + " " +
               (isPinned() ? "PINNED" : "UNPINNED") + " " + content;
    }
}
