import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

/**
 * ForecastPanel.java
 * ─────────────────────────────────────────────────────────────────
 * Reusable Swing panel components for forecast sections.
 *
 * All colours are read from WeatherApp.Theme at paint time, so
 * dark/light mode switching works automatically without rebuilding
 * the panels.
 * ─────────────────────────────────────────────────────────────────
 */
public class ForecastPanel {

    // OWM icon URL template
    private static final String ICON_URL = "https://openweathermap.org/img/wn/%s@2x.png";

    // =================================================================
    //  DailyForecastPanel  – "5 Days Forecast"
    // =================================================================

    public static class DailyForecastPanel extends JPanel {

        public DailyForecastPanel(List<WeatherData.DailyItem> items) {
            setOpaque(false);
            setLayout(new BorderLayout());

            JLabel title = new JLabel("5 Days Forecast:") {
                @Override public Color getForeground() { return WeatherApp.Theme.textMain(); }
            };
            title.setFont(new Font("SansSerif", Font.BOLD, 18));
            title.setBorder(new EmptyBorder(0, 0, 12, 0));
            add(title, BorderLayout.NORTH);

            JPanel rows = new JPanel();
            rows.setOpaque(false);
            rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));

            if (items != null && !items.isEmpty()) {
                for (WeatherData.DailyItem item : items) {
                    rows.add(createDailyRow(item));
                    rows.add(Box.createVerticalStrut(8));
                }
            } else {
                JLabel noData = new JLabel("No forecast data available.") {
                    @Override public Color getForeground() { return WeatherApp.Theme.textDim(); }
                };
                rows.add(noData);
            }
            add(rows, BorderLayout.CENTER);
        }

        private JPanel createDailyRow(WeatherData.DailyItem item) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            row.add(new WeatherIconLabel(item.getConditionIcon(), 36), BorderLayout.WEST);

            JLabel tempLabel = new JLabel(Math.round(item.getTemperature()) + "°C") {
                @Override public Color getForeground() { return WeatherApp.Theme.textMain(); }
            };
            tempLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            row.add(tempLabel, BorderLayout.CENTER);

            JLabel dayLabel = new JLabel(item.getDayLabel()) {
                @Override public Color getForeground() { return WeatherApp.Theme.textDim(); }
            };
            dayLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            row.add(dayLabel, BorderLayout.EAST);

            return row;
        }
    }

    // =================================================================
    //  HourlyForecastPanel  – "Hourly Forecast"
    // =================================================================

    public static class HourlyForecastPanel extends JPanel {

        public HourlyForecastPanel(List<WeatherData.HourlyItem> items) {
            setOpaque(false);
            setLayout(new BorderLayout());

            JLabel title = new JLabel("Hourly Forecast:") {
                @Override public Color getForeground() { return WeatherApp.Theme.textMain(); }
            };
            title.setFont(new Font("SansSerif", Font.BOLD, 18));
            title.setBorder(new EmptyBorder(0, 0, 12, 0));
            add(title, BorderLayout.NORTH);

            JPanel tilesRow = new JPanel(new GridLayout(1, 0, 10, 0));
            tilesRow.setOpaque(false);

            if (items != null && !items.isEmpty()) {
                for (WeatherData.HourlyItem item : items) {
                    tilesRow.add(createHourlyTile(item));
                }
            } else {
                JLabel noData = new JLabel("No hourly data available.") {
                    @Override public Color getForeground() { return WeatherApp.Theme.textDim(); }
                };
                tilesRow.add(noData);
            }
            add(tilesRow, BorderLayout.CENTER);
        }

        private GlossyTile createHourlyTile(WeatherData.HourlyItem item) {
            GlossyTile tile = new GlossyTile();
            tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
            tile.setBorder(new EmptyBorder(12, 8, 12, 8));

            JLabel timeLabel = new JLabel(item.getTime()) {
                @Override public Color getForeground() { return WeatherApp.Theme.textMain(); }
            };
            timeLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
            timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            WeatherIconLabel iconLabel = new WeatherIconLabel(item.getConditionIcon(), 44);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel tempLabel = new JLabel(Math.round(item.getTemperature()) + "°C") {
                @Override public Color getForeground() { return WeatherApp.Theme.textMain(); }
            };
            tempLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel windLabel = new JLabel("▲  " + item.getWindSpeed() + " km/h");
            windLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            windLabel.setForeground(WeatherApp.Theme.WIND_BLUE);
            windLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            tile.add(timeLabel);
            tile.add(Box.createVerticalStrut(6));
            tile.add(iconLabel);
            tile.add(Box.createVerticalStrut(6));
            tile.add(tempLabel);
            tile.add(Box.createVerticalStrut(6));
            tile.add(windLabel);
            return tile;
        }
    }

    // =================================================================
    //  GlossyTile  – neumorphic dark/light tile (reads Theme)
    // =================================================================

    public static class GlossyTile extends JPanel {
        private static final int ARC = 18;

        public GlossyTile() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // ── Bottom-right shadow ───────────────────────────────
            g2.setColor(WeatherApp.Theme.shadowDark());
            g2.fillRoundRect(4, 4, w - 2, h - 2, ARC, ARC);

            // ── Top-left highlight ────────────────────────────────
            g2.setColor(WeatherApp.Theme.shadowLight());
            g2.fillRoundRect(-2, -2, w - 2, h - 2, ARC, ARC);

            // ── Tile fill ─────────────────────────────────────────
            g2.setColor(WeatherApp.Theme.tile());
            g2.fillRoundRect(1, 1, w - 4, h - 4, ARC, ARC);

            // ── Gloss strip ───────────────────────────────────────
            GradientPaint gloss = new GradientPaint(
                0, 2, WeatherApp.Theme.gloss(),
                0, h / 3f, new Color(0, 0, 0, 0)
            );
            g2.setPaint(gloss);
            g2.fillRoundRect(1, 1, w - 4, h / 3, ARC, ARC);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =================================================================
    //  WeatherIconLabel  – async OWM icon loader with cache + fallback
    // =================================================================

    public static class WeatherIconLabel extends JLabel {

        private static final java.util.Map<String, ImageIcon> CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

        public WeatherIconLabel(String iconCode, int size) {
            setText("⏳");
            setFont(new Font("SansSerif", Font.PLAIN, size / 2));
            setHorizontalAlignment(SwingConstants.CENTER);

            if (iconCode == null || iconCode.isEmpty()) {
                setText(emojiFor(""));
                return;
            }

            String cacheKey = iconCode + "_" + size;

            if (CACHE.containsKey(cacheKey)) {
                setIcon(CACHE.get(cacheKey));
                setText("");
                return;
            }

            SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
                @Override protected ImageIcon doInBackground() throws Exception {
                    BufferedImage img = ImageIO.read(new URL(String.format(ICON_URL, iconCode)));
                    if (img == null) return null;
                    return new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH));
                }
                @Override protected void done() {
                    try {
                        ImageIcon icon = get();
                        if (icon != null) {
                            CACHE.put(cacheKey, icon);
                            setIcon(icon);
                            setText("");
                        } else {
                            setText(emojiFor(iconCode));
                        }
                    } catch (Exception e) {
                        setText(emojiFor(iconCode));
                    }
                }
            };
            worker.execute();
        }

        // Emoji fallback based on OWM icon prefix
        private static String emojiFor(String code) {
            if (code == null || code.isEmpty()) return "🌡";
            if (code.startsWith("01")) return "☀️";
            if (code.startsWith("02") || code.startsWith("03") || code.startsWith("04")) return "⛅";
            if (code.startsWith("09") || code.startsWith("10")) return "🌧";
            if (code.startsWith("11")) return "⛈";
            if (code.startsWith("13")) return "❄️";
            if (code.startsWith("50")) return "🌫";
            return "🌡";
        }
    }
}
