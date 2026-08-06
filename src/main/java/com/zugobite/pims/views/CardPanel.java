package com.zugobite.pims.views;

import com.zugobite.pims.utils.UIUtils;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A bordered, rounded-corner white container - the Swing equivalent of shadcn/ui's
 * {@code Card} primitive. Every screen in the app wraps its main content (a data table, a
 * form) in one of these instead of placing it directly on the window background, which is
 * what turns a flat list of controls into something that reads as a deliberately composed
 * "product" screen rather than a bare Swing form.
 *
 * <p>Painting is done by hand ({@link #paintComponent}) rather than via a
 * {@link javax.swing.border.Border}, because a rounded card needs its background fill
 * clipped to the same rounded rectangle as its border - a plain rectangular background
 * painted underneath a rounded border would show square corners poking out from behind it.</p>
 */
public class CardPanel extends JPanel {

    private static final int CORNER_RADIUS = UIUtils.CARD_RADIUS;

    public CardPanel() {
        this(null);
    }

    public CardPanel(LayoutManager layout) {
        if (layout != null) {
            setLayout(layout);
        }
        // Painting the background ourselves (see paintComponent) rather than through Swing's
        // normal opaque-background fill, so the rounded corners are respected.
        setOpaque(false);
        setBackground(UIUtils.BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
        g2.setColor(getBackground());
        g2.fill(shape);
        g2.setColor(UIUtils.BORDER_COLOR);
        g2.draw(shape);

        g2.dispose();
        super.paintComponent(g);
    }
}
