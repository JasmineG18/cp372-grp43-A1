package A1.Client;

public class ClientNote {
    public final int x, y;
    public final String colour;
    public final boolean pinned;
    public final String message;

    public ClientNote(int x, int y, String colour, boolean pinned, String message) {
        this.x = x;
        this.y = y;
        this.colour = colour;
        this.pinned = pinned;
        this.message = message;
    }
}
