// Enum representing all possible server-side error types
public enum Errors {

    INVALID_FORMAT,        // Command structure is incorrect
    OUT_OF_BOUNDS,         // Coordinates exceed board limits
    COLOUR_NOT_SUPPORTED,  // Colour is not in the allowed set
    COMPLETE_OVERLAP,      // Note fully overlaps an existing note
    PIN_NOT_FOUND,         // No pin exists at the given coordinate
    NO_NOTE_AT_COORDINATE; // No note exists at the given coordinate

    // Creates a ProtocolException with a standard error message
    public ProtocolException exception() {
        return new ProtocolException("ERROR " + name());
    }

    // Creates a formatted error message with extra details
    public String message(String msg) {
        return "ERROR " + name() + " " + msg;
    }
}
