package A1.Server;

import java.util.*;

public class Note {

    private final int x, y, w, h;
    private final String colour, content;
    private final List<Pin> pins = new ArrayList<>();

    public Note(int x, int y, int w, int h, String colour, String content) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.colour = colour;
        this.content = content;
    }

    public boolean contains(int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public boolean completelyOverlaps(Note other) {
        return this.x == other.x && this.y == other.y &&
               this.w == other.w && this.h == other.h;
    }

    public void addPin(int px, int py) {
        pins.add(new Pin(px, py));
    }

    public boolean removePin(int px, int py) {
        return pins.removeIf(p -> p.x == px && p.y == py);
    }

    public boolean isPinned() {
        return !pins.isEmpty();
    }

    @Override
    public String toString() {
        return x + " " + y + " " + colour + " " +
               (isPinned() ? "PINNED" : "UNPINNED") + " " + content;
    }
}
