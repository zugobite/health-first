package com.zugobite.pims.views;

import com.zugobite.pims.utils.UIUtils;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A bordered, rounded-corner white container - the Swing equivalent of shadcn/ui's
 * {@code Card} primitive. Every screen in the app wraps its main content (a data table, a
 * form) in one of these instead of placing it directly on the window background.
 *
 * <p>Fill, child clipping, and the border stroke are all painted against the same round
 * rect. The stroke is drawn <em>after</em> children so opaque table content cannot square
 * off the corners.</p>
 */
public class CardPanel extends JPanel {

    private static final int ARC = UIUtils.CARD_RADIUS;

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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fill(roundShape());
        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.clip(roundShape());
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
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(UIUtils.BORDER_COLOR);
        g2.draw(roundShape());
        g2.dispose();
    }

    private RoundRectangle2D roundShape() {
        return new RoundRectangle2D.Float(
                0.5f, 0.5f, Math.max(0, getWidth() - 1.5f), Math.max(0, getHeight() - 1.5f), ARC, ARC);
    }
}
