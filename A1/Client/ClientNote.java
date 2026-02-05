/* Simple data holder representing a single note on the bulletin board

Stores the logical position, colour, pin state, and message text for 
rendering on the Client
*/
public class ClientNote {

    // x and y coordinates of the note on the board
    public final int x, y;

    // colour of the note
    public final String colour;

    // pin indicator
    public final boolean pinned;

    // text message contained in the note
    public final String message;

    // constructs a set ClientNote instance
    public ClientNote(int x, int y, String colour, boolean pinned, String message) {
        this.x = x;
        this.y = y;
        this.colour = colour;
        this.pinned = pinned;
        this.message = message;
    }
}
