package com.zugobite.pims.views;

import com.zugobite.pims.utils.AppIcons;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * A single headline metric on a dashboard home screen: a small uppercase caption, a large
 * value, and a one-line supporting note — e.g. "TODAY'S SALES / R 4,120.50 / 18 transactions".
 *
 * <p>This is the component that most changes how the app reads. A launcher screen of nothing
 * but navigation tiles looks like a student project; the same screen led by live figures
 * pulled from the business reads as operational software, because it answers a question the
 * user actually has ("how are we doing right now?") before they've clicked anything.</p>
 *
 * <p>Metrics that represent a problem needing attention (stock at reorder level, stock
 * expiring within the month) can be marked as {@link Tone#WARNING} or {@link Tone#DANGER},
 * which tints the value and draws a colored rule down the card's leading edge — so a manager
 * spots trouble from across the room without reading a single number.</p>
 */
public class StatCard extends CardPanel {

    /** Visual emphasis for a metric's value and leading accent rule. */
    public enum Tone {
        /** Ordinary informational figure — neutral dark text, brand-teal rule. */
        NEUTRAL(UIUtils.FOREGROUND, UIUtils.PRIMARY),
        /** Needs attention soon (e.g. items expiring this month) — amber. */
        WARNING(new Color(0xB4, 0x53, 0x09), new Color(0xF5, 0x9E, 0x0B)),
        /** Needs attention now (e.g. items at/below reorder level) — red. */
        DANGER(UIUtils.DESTRUCTIVE, UIUtils.DESTRUCTIVE);

        private final Color valueColor;
        private final Color accentColor;

        Tone(Color valueColor, Color accentColor) {
            this.valueColor = valueColor;
            this.accentColor = accentColor;
        }
    }

    private static final int ICON_SIZE = 16;

    /**
     * @param caption small uppercase label naming the metric, e.g. {@code "LOW STOCK"}
     * @param value   the figure itself, pre-formatted for display (money via
     *                {@link UIUtils#formatCurrency}, counts as plain integers)
     * @param note    one-line supporting detail shown under the value, or {@code null} for none
     * @param icon    a Lucide icon name from {@link AppIcons} shown beside the caption
     * @param tone    emphasis for the value text and the card's leading accent rule
     */
    public StatCard(String caption, String value, String note, String icon, Tone tone) {
        super(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                // A 3px colored rule down the leading edge is what carries the tone at a
                // glance; the rest of the card stays neutral so a row of these still reads
                // as one calm group rather than a wall of competing color.
                BorderFactory.createMatteBorder(0, 3, 0, 0, tone.accentColor),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);

        stack.add(buildCaptionRow(caption, icon, tone));
        stack.add(Box.createVerticalStrut(8));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 24f));
        valueLabel.setForeground(tone.valueColor);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(valueLabel);

        if (note != null && !note.isBlank()) {
            JLabel noteLabel = new JLabel(note);
            noteLabel.setFont(noteLabel.getFont().deriveFont(Font.PLAIN, 12f));
            noteLabel.setForeground(UIUtils.MUTED_FOREGROUND);
            noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            stack.add(Box.createVerticalStrut(4));
            stack.add(noteLabel);
        }

        add(stack, BorderLayout.CENTER);
    }

    /** The icon + small uppercase caption line above the value. */
    private JPanel buildCaptionRow(String caption, String icon, Tone tone) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (icon != null) {
            JLabel iconLabel = new JLabel(AppIcons.lucide(icon, ICON_SIZE, tone.accentColor));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
            row.add(iconLabel);
        }

        JLabel captionLabel = new JLabel(caption.toUpperCase());
        captionLabel.setFont(captionLabel.getFont().deriveFont(Font.BOLD, 11f));
        captionLabel.setForeground(UIUtils.MUTED_FOREGROUND);
        row.add(captionLabel);
        row.add(Box.createHorizontalGlue());

        // Fixed row height keeps every card in a row optically aligned even when one has an
        // icon and another does not.
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ICON_SIZE + 4));
        return row;
    }
}
