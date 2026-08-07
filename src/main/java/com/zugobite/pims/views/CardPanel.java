package com.zugobite.pims.views;

import com.zugobite.pims.utils.UIUtils;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * A bordered, square-corner white container - the Swing equivalent of shadcn/ui's
 * {@code Card} primitive. Every screen in the app wraps its main content (a data table, a
 * form) in one of these instead of placing it directly on the window background.
 *
 * <p>Fill, child clipping, and the border stroke share the same rectangular bounds. The
 * stroke is drawn <em>after</em> children so opaque table content cannot cover the edge.</p>
 */
public class CardPanel extends JPanel {

    public CardPanel() {
        this(null);
    }

    public CardPanel(LayoutManager layout) {
        if (layout != null) {
            setLayout(layout);
        }
        setOpaque(false);
        setBackground(UIUtils.BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(getBackground());
        g2.fill(contentBounds());
        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.clip(contentBounds());
        super.paintChildren(g2);
        g2.dispose();
    }

    @Override
    public void paint(Graphics g) {
        paintComponent(g);
        paintBorder(g);
        paintChildren(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(UIUtils.BORDER_COLOR);
        Rectangle bounds = contentBounds();
        g2.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
        g2.dispose();
    }

    private Rectangle contentBounds() {
        return new Rectangle(0, 0, getWidth(), getHeight());
    }
}
