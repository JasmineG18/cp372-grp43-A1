public enum Errors {
    INVALID_FORMAT,
    OUT_OF_BOUNDS,
    COLOUR_NOT_SUPPORTED,
    COMPLETE_OVERLAP,
    PIN_NOT_FOUND,
    NO_NOTE_AT_COORDINATE;

    public ProtocolException exception() {
        return new ProtocolException("ERROR " + name());
    }

    public String message(String msg) {
        return "ERROR " + name() + " " + msg;
    }
}
