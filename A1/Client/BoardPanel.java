import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/*visually renders the bulletin board, 
scales the logical board coordinates into screen pixels and draws
each not with its colour, pin status, and message text given by the Client
 */
public class BoardPanel extends JPanel {

    //Board dimensions as defined by the server
    private int boardW = 200, boardH = 100;

    //Note dimensions
    private int noteW = 20, noteH = 10;

    //Current notes to display on the board
    private List<ClientNote> notes = new ArrayList<>();

    public BoardPanel() {
        //Fixed preferred size
        setPreferredSize(new Dimension(700, 350));
    }

    //updates the board and note dimensions received from server handshake
    public void setDimensions(int boardW, int boardH, int noteW, int noteH) {
        this.boardW = boardW;
        this.boardH = boardH;
        this.noteW = noteW;
        this.noteH = noteH;
        repaint();
    }

    //replaces the current list of notes and triggers a redraw
    public void setNotes(List<ClientNote> notes) {
        this.notes = new ArrayList<>(notes);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // draw background
        g.setColor(new Color(245, 245, 245));
        g.fillRect(0, 0, getWidth(), getHeight());

        // draw board border
        g.setColor(Color.DARK_GRAY);
        g.drawRect(5, 5, getWidth() - 10, getHeight() - 10);

        // scale factors from board units to screen pixels
        double sx = (getWidth() - 10.0) / boardW;
        double sy = (getHeight() - 10.0) / boardH;

        // draw each note
        for (ClientNote n : notes) {

            //convert coordinates to pixels
            int px = 5 + (int) Math.round(n.x * sx);
            int py = 5 + (int) Math.round(n.y * sy);
            int pw = (int) Math.round(noteW * sx);
            int ph = (int) Math.round(noteH * sy);

            // fill note background based on its colour
            g.setColor(mapColour(n.colour));
            g.fillRect(px, py, pw, ph);

            // outline the note
            g.setColor(Color.BLACK);
            g.drawRect(px, py, pw, ph);

            // draw pin marker if the not is pinned
            if (n.pinned) {
                g.setColor(Color.RED);
                g.fillOval(px + pw - 10, py + 2, 8, 8);
            }

            // write note message and wrap text if too long
            g.setColor(Color.BLACK);
            drawWrappedText(g, n.message, px + 4, py + 14, pw - 8, ph - 6);

        }
    }

    // wrap text to fit note
    private void drawWrappedText(Graphics g, String text, int x, int y, int maxWidth, int maxHeight) {
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();
    
        int curY = y;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
    
        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
    
            if (fm.stringWidth(testLine) > maxWidth) {
                // Draw current line
                if (curY + lineHeight > y + maxHeight) return;
                g.drawString(line.toString(), x, curY);
                curY += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
    
        // Draw last line
        if (line.length() > 0 && curY + lineHeight <= y + maxHeight) {
            g.drawString(line.toString(), x, curY);
        }
    }
    
    // maps colour names sent by the server
    private Color mapColour(String c) {
        String s = c.toLowerCase();
    switch (s) {
        case "red": return new Color(255, 220, 220);
        case "green": return new Color(220, 255, 220);
        case "yellow": return new Color(255, 255, 200);
        case "white": return Color.WHITE;
        case "blue": return new Color(200, 220, 255);
        case "pink": return new Color(255, 200, 230);
        case "orange": return new Color(255, 220, 180);
        case "purple": return new Color(230, 200, 255);
        case "cyan": return new Color(200, 255, 255);
        default: return new Color(230, 230, 230);
    }
    }
}
