package visualization;

import javax.swing.*;
import java.awt.*;

public class TopPanel extends JPanel {
    private View view;

    public TopPanel(View view) {
        this.view = view;

        setPreferredSize(new Dimension(600, 50));
        setBackground(Color.YELLOW);
    }
}
