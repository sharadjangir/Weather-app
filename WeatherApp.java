import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * WeatherApp.java
 * ─────────────────────────────────────────────────────────────────
 * Main entry-point and GUI for the Weather Forecast Application.
 *
 * Dark / Light mode works via a static Theme class.
 * Every custom-painted component reads Theme at paint time, so
 * flipping Theme.dark + repaint() re-themes the entire UI instantly.
 *
 * Run: java -cp ".;json-20240303.jar" WeatherApp   (Windows)
 *      java -cp ".:json-20240303.jar" WeatherApp   (macOS/Linux)
 * ─────────────────────────────────────────────────────────────────
 */
public class WeatherApp extends JFrame {

    // =================================================================
    //  Theme – single source of truth for every colour in the app
    // =================================================================

    /**
     * All UI colours live here.  Components call Theme.xxx() inside
     * paintComponent / getForeground so they always use the current
     * theme without needing to be rebuilt.
     */
    public static class Theme {

        /** true = dark mode (default), false = light mode */
        public static boolean dark = true;

        // ── Backgrounds ───────────────────────────────────────────
        public static Color bg()         { return dark ? new Color(0x2f2f2f) : new Color(0xDEDEDE); }
        public static Color card()       { return dark ? new Color(0x3b3b3b) : new Color(0xF0F0F0); }
        public static Color tile()       { return dark ? new Color(0x404040) : new Color(0xE4E4E4); }

        // ── Text ──────────────────────────────────────────────────
        public static Color textMain()   { return dark ? new Color(0xFFFFFF) : new Color(0x111111); }
        public static Color textDim()    { return dark ? new Color(0xAAAAAA) : new Color(0x555555); }
        public static Color textGrey()   { return dark ? new Color(0x888888) : new Color(0x999999); }

        // ── Shadows & Gloss ───────────────────────────────────────
        public static Color shadowDark() { return dark ? new Color(0x22000000,true) : new Color(0x28000000,true); }
        public static Color shadowLight(){ return dark ? new Color(0x40606060,true) : new Color(0x60FFFFFF,true); }
        public static Color gloss()      { return dark ? new Color(0x12FFFFFF,true) : new Color(0x30FFFFFF,true); }

        // ── Input field ───────────────────────────────────────────
        public static Color searchBg()   { return dark ? new Color(0x464646) : new Color(0xFFFFFF); }
        public static Color searchBorder(){ return dark ? new Color(0x555555) : new Color(0xBBBBBB); }

        // ── Toggle button background ──────────────────────────────
        public static Color toggleBg()   { return dark ? new Color(0x555555) : new Color(0xCCCCCC); }

        // ── Fixed accent colours ──────────────────────────────────
        public static final Color ACCENT_GN = new Color(0x4CAF50);  // green button
        public static final Color ACCENT_YL = new Color(0xFFCC00);  // temperature text
        public static final Color WIND_BLUE = new Color(0x64B5F6);  // wind arrow
    }

    // =================================================================
    //  Fields
    // =================================================================

    private final WeatherService weatherService = new WeatherService();
    private WeatherData currentData;

    // Top bar
    private JToggleButton darkModeToggle;
    private JTextField    searchField;

    // City / Clock card
    private JLabel cityNameLabel;
    private JLabel clockLabel;
    private JLabel dateLabel;

    // Current weather card
    private JLabel tempLabel;
    private JLabel feelsLikeLabel;
    private JLabel conditionLabel;
    private ForecastPanel.WeatherIconLabel weatherIconLabel;
    private JLabel sunriseLabel, sunsetLabel;
    private JLabel humidityLabel, windLabel, pressureLabel, uvLabel;

    // Forecast containers (swapped on each search)
    private JPanel dailyContainer;
    private JPanel hourlyContainer;

    // The root panel – referenced so we can repaint it
    private JPanel rootPanel;

    // Live clock
    private Timer clockTimer;

    // =================================================================
    //  Constructor
    // =================================================================

    public WeatherApp() {
        setTitle("Weather Forecast App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);

        rootPanel = buildRoot();
        add(rootPanel);
        startClock();
        setVisible(true);
    }

    // =================================================================
    //  Root panel
    // =================================================================

    private JPanel buildRoot() {
        // Overrides paintComponent so the background tracks Theme.bg()
        JPanel root = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.bg());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(true);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.add(buildTopBar(),   BorderLayout.NORTH);
        root.add(buildMainGrid(), BorderLayout.CENTER);
        return root;
    }

    // =================================================================
    //  TOP BAR
    // =================================================================

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.bg());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setOpaque(true);
        bar.setBorder(new EmptyBorder(0, 0, 8, 0));

        // ── 1. Dark mode toggle (left) ────────────────────────────
        darkModeToggle = new JToggleButton("🌙  Dark Mode") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Pill background colour reads Theme at runtime
                g2.setColor(Theme.toggleBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                // Slight gloss strip
                g2.setColor(Theme.gloss());
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        darkModeToggle.setSelected(true);   // starts in dark mode
        styleToggle();

        darkModeToggle.addActionListener(e -> applyTheme(darkModeToggle.isSelected()));

        JPanel toggleWrapper = transparentBgPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toggleWrapper.add(darkModeToggle);
        bar.add(toggleWrapper, BorderLayout.WEST);

        // ── 2. Search field (centre) ──────────────────────────────
        searchField = new JTextField("Search for your preferred city...");
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        applySearchFieldTheme();   // sets colours from Theme

        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (searchField.getText().startsWith("Search")) {
                    searchField.setText("");
                    searchField.setForeground(Theme.textMain());
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search for your preferred city...");
                    searchField.setForeground(Theme.textGrey());
                }
            }
        });
        searchField.addActionListener(e -> performSearch(searchField.getText()));
        bar.add(searchField, BorderLayout.CENTER);

        // ── 3. Current Location button (right) ───────────────────
        JButton locationBtn = new RoundedButton("📍  Current Location", Theme.ACCENT_GN, Color.WHITE);
        locationBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        locationBtn.addActionListener(e -> fetchCurrentLocation());

        JPanel btnWrapper = transparentBgPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrapper.add(locationBtn);
        bar.add(btnWrapper, BorderLayout.EAST);

        return bar;
    }

    // =================================================================
    //  MAIN GRID  (2 rows × 2 cols)
    // =================================================================

    private JPanel buildMainGrid() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.bg()); g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        grid.setOpaque(true);
        grid.add(buildCityCard());
        grid.add(buildWeatherCard());
        grid.add(buildDailyCard());
        grid.add(buildHourlyCard());
        return grid;
    }

    // =================================================================
    //  CARD FACTORY – neumorphic rounded card
    // =================================================================

    private JPanel createCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), arc = 22;

                // Dark shadow (bottom-right offset)
                g2.setColor(Theme.shadowDark());
                g2.fillRoundRect(5, 5, w - 2, h - 2, arc, arc);

                // Light shadow (top-left offset – creates raised feel)
                g2.setColor(Theme.shadowLight());
                g2.fillRoundRect(-3, -3, w - 2, h - 2, arc, arc);

                // Card body
                g2.setColor(Theme.card());
                g2.fillRoundRect(1, 1, w - 4, h - 4, arc, arc);

                // Glossy highlight at top
                GradientPaint gloss = new GradientPaint(0, 2, Theme.gloss(), 0, h / 4f, new Color(0,0,0,0));
                g2.setPaint(gloss);
                g2.fillRoundRect(1, 1, w - 4, h / 4, arc, arc);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        return card;
    }

    // =================================================================
    //  CITY / CLOCK CARD  (top-left)
    // =================================================================

    private JPanel buildCityCard() {
        JPanel card = createCard();
        card.setLayout(new GridBagLayout());

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        cityNameLabel = themedLabel("--", Font.BOLD, 30, false);
        cityNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Uses Monospaced font for a clean digital clock look
        clockLabel = new JLabel("--:--") {
            @Override public Color getForeground() { return Theme.textMain(); }
        };
        clockLabel.setFont(new Font("Monospaced", Font.BOLD, 58));
        clockLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        dateLabel = themedLabel("--", Font.PLAIN, 16, true);
        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(cityNameLabel);
        inner.add(Box.createVerticalStrut(10));
        inner.add(clockLabel);
        inner.add(Box.createVerticalStrut(6));
        inner.add(dateLabel);

        card.add(inner);
        return card;
    }

    // =================================================================
    //  CURRENT WEATHER CARD  (top-right)
    // =================================================================

    private JPanel buildWeatherCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(12, 0));

        // ── Left column: temperature, feels-like, sunrise/sunset ──
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        // Temperature uses fixed yellow accent (looks good in both modes)
        tempLabel = new JLabel("--°C") {
            @Override public Color getForeground() { return Theme.ACCENT_YL; }
        };
        tempLabel.setFont(new Font("SansSerif", Font.BOLD, 54));
        tempLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        feelsLikeLabel = themedLabel("Feels like: --°C", Font.PLAIN, 15, true);
        feelsLikeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sunriseLabel = themedLabel("☀ Sunrise   --:--", Font.PLAIN, 14, true);
        sunriseLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sunsetLabel  = themedLabel("🌙 Sunset    --:--", Font.PLAIN, 14, true);
        sunsetLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(tempLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(feelsLikeLabel);
        left.add(Box.createVerticalStrut(16));
        left.add(sunriseLabel);
        left.add(Box.createVerticalStrut(6));
        left.add(sunsetLabel);
        card.add(left, BorderLayout.WEST);

        // ── Centre: big icon + condition text ─────────────────────
        JPanel centre = new JPanel();
        centre.setOpaque(false);
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));

        weatherIconLabel = new ForecastPanel.WeatherIconLabel("01d", 90);
        weatherIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        conditionLabel = themedLabel("--", Font.BOLD, 20, false);
        conditionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centre.add(Box.createVerticalGlue());
        centre.add(weatherIconLabel);
        centre.add(Box.createVerticalStrut(6));
        centre.add(conditionLabel);
        centre.add(Box.createVerticalGlue());
        card.add(centre, BorderLayout.CENTER);

        // ── Right: 2×2 metric tiles ───────────────────────────────
        JPanel right = new JPanel(new GridLayout(2, 2, 10, 10));
        right.setOpaque(false);

        humidityLabel = dynamicMainLabel("--");
        windLabel     = dynamicMainLabel("--");
        pressureLabel = dynamicMainLabel("--");
        uvLabel       = dynamicMainLabel("--");

        right.add(metricTile("〰", humidityLabel, "Humidity"));
        right.add(metricTile("💨", windLabel,     "Wind Speed"));
        right.add(metricTile("🔵", pressureLabel,  "Pressure"));
        right.add(metricTile("☀",  uvLabel,        "UV"));
        card.add(right, BorderLayout.EAST);

        return card;
    }

    // ── Metric tile helper ────────────────────────────────────────
    private JPanel metricTile(String icon, JLabel valueLabel, String caption) {
        // GlossyTile reads Theme automatically at paint time
        ForecastPanel.GlossyTile tile = new ForecastPanel.GlossyTile();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 18));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel capLbl = new JLabel(caption) {
            @Override public Color getForeground() { return Theme.textDim(); }
        };
        capLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        capLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(iconLbl);
        tile.add(Box.createVerticalStrut(4));
        tile.add(valueLabel);
        tile.add(capLbl);
        return tile;
    }

    // =================================================================
    //  DAILY FORECAST CARD  (bottom-left)
    // =================================================================

    private JPanel buildDailyCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout());
        dailyContainer = new JPanel(new BorderLayout());
        dailyContainer.setOpaque(false);
        dailyContainer.add(placeholderLabel("Search for a city to see the 5-day forecast."), BorderLayout.CENTER);
        card.add(dailyContainer, BorderLayout.CENTER);
        return card;
    }

    // =================================================================
    //  HOURLY FORECAST CARD  (bottom-right)
    // =================================================================

    private JPanel buildHourlyCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout());
        hourlyContainer = new JPanel(new BorderLayout());
        hourlyContainer.setOpaque(false);
        hourlyContainer.add(placeholderLabel("Search for a city to see hourly data."), BorderLayout.CENTER);
        card.add(hourlyContainer, BorderLayout.CENTER);
        return card;
    }

    // =================================================================
    //  THEME SWITCH  ← THE KEY FIX
    // =================================================================

    /**
     * Flips Theme.dark, updates any components that have hardcoded
     * colours set via setXxx() (not overrideGetForeground), then
     * triggers a full repaint cascade.
     *
     * Components that override getForeground() or paintComponent()
     * to call Theme.xxx() will automatically reflect the new theme
     * when Swing calls repaint on them.
     */
    private void applyTheme(boolean dark) {
        Theme.dark = dark;

        // ── Toggle button ─────────────────────────────────────────
        styleToggle();

        // ── Search field (uses setBackground / setForeground) ─────
        applySearchFieldTheme();

        // ── Repaint the whole component tree ─────────────────────
        // This causes every custom paintComponent to fire again and
        // pick up the new Theme colours.
        SwingUtilities.invokeLater(() -> {
            repaintTree(rootPanel);
        });
    }

    /** Recursively calls repaint() on every component in the tree. */
    private void repaintTree(Component c) {
        c.repaint();
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                repaintTree(child);
            }
        }
    }

    /** Applies label / border / background colours to the toggle button. */
    private void styleToggle() {
        darkModeToggle.setText(Theme.dark ? "🌙  Dark Mode" : "☀️  Light Mode");
        darkModeToggle.setForeground(Theme.textMain());
        darkModeToggle.setFont(new Font("SansSerif", Font.BOLD, 12));
        darkModeToggle.setContentAreaFilled(false);
        darkModeToggle.setFocusPainted(false);
        darkModeToggle.setBorderPainted(false);
        darkModeToggle.setBorder(new EmptyBorder(8, 18, 8, 18));
        darkModeToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Applies search field colours from Theme. */
    private void applySearchFieldTheme() {
        boolean isPlaceholder = searchField.getText().startsWith("Search");
        searchField.setBackground(Theme.searchBg());
        searchField.setForeground(isPlaceholder ? Theme.textGrey() : Theme.textMain());
        searchField.setCaretColor(Theme.textMain());
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.searchBorder(), 1, true),
            new EmptyBorder(10, 20, 10, 20)
        ));
    }

    // =================================================================
    //  SEARCH / API LOGIC
    // =================================================================

    /** Validates input then fetches weather on a background thread. */
    private void performSearch(String rawInput) {
        InputValidator.ValidationResult result = InputValidator.validateCity(rawInput);
        if (!result.isValid()) {
            JOptionPane.showMessageDialog(this, result.getMessage(),
                "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String city = result.getValue();

        // Optimistic loading state
        cityNameLabel.setText(city);
        tempLabel.setText("…");
        conditionLabel.setText("Fetching weather…");
        feelsLikeLabel.setText("Feels like: …");

        // Fetch on background thread (never block the EDT)
        SwingWorker<WeatherData, Void> worker = new SwingWorker<>() {
            @Override
            protected WeatherData doInBackground() throws Exception {
                return weatherService.fetchWeather(city);
            }
            @Override
            protected void done() {
                try {
                    currentData = get();
                    updateUI(currentData);
                } catch (java.util.concurrent.ExecutionException ex) {
                    String msg = ex.getCause() != null
                        ? ex.getCause().getMessage()
                        : "An unexpected error occurred.";
                    JOptionPane.showMessageDialog(WeatherApp.this,
                        msg, "Weather Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(WeatherApp.this,
                        "Failed to fetch weather data.\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void fetchCurrentLocation() {
        JOptionPane.showMessageDialog(this,
            "Location detection requires device GPS access.\n" +
            "Please type your city name in the search box instead.",
            "Current Location", JOptionPane.INFORMATION_MESSAGE);
    }

    // =================================================================
    //  UI UPDATE – populate all components from fresh WeatherData
    // =================================================================

    private void updateUI(WeatherData data) {
        if (data == null) return;

        cityNameLabel.setText(data.getCityName());
        tempLabel.setText(Math.round(data.getTemperature()) + "°C");
        feelsLikeLabel.setText("Feels like: " + Math.round(data.getFeelsLike()) + "°C");
        conditionLabel.setText(data.getCondition());

        // Replace big weather icon in-place
        Container iconParent = weatherIconLabel.getParent();
        if (iconParent != null) {
            int idx = ((JPanel) iconParent).getComponentZOrder(weatherIconLabel);
            iconParent.remove(weatherIconLabel);
            weatherIconLabel = new ForecastPanel.WeatherIconLabel(data.getConditionIcon(), 90);
            weatherIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            ((JPanel) iconParent).add(weatherIconLabel, idx);
            iconParent.revalidate();
            iconParent.repaint();
        }

        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a");
        sunriseLabel.setText("☀ Sunrise   " + timeFmt.format(new Date(data.getSunriseEpoch() * 1000L)));
        sunsetLabel .setText("🌙 Sunset    " + timeFmt.format(new Date(data.getSunsetEpoch()  * 1000L)));

        humidityLabel.setText(data.getHumidity() + "%");
        windLabel    .setText(data.getWindSpeed() + " km/h");
        pressureLabel.setText(data.getPressure()  + " hPa");
        uvLabel      .setText(String.valueOf(data.getUvIndex()));

        // Rebuild forecast panels (they read Theme at paint time automatically)
        dailyContainer.removeAll();
        dailyContainer.add(
            new ForecastPanel.DailyForecastPanel(data.getDailyForecast()),
            BorderLayout.CENTER
        );
        dailyContainer.revalidate();
        dailyContainer.repaint();

        hourlyContainer.removeAll();
        hourlyContainer.add(
            new ForecastPanel.HourlyForecastPanel(data.getHourlyForecast()),
            BorderLayout.CENTER
        );
        hourlyContainer.revalidate();
        hourlyContainer.repaint();
    }

    // =================================================================
    //  LIVE CLOCK
    // =================================================================

    private void startClock() {
        clockTimer = new Timer(true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                SwingUtilities.invokeLater(() -> {
                    clockLabel.setText(new SimpleDateFormat("HH:mm").format(new Date()));
                    dateLabel .setText(new SimpleDateFormat("EEEE, d MMM").format(new Date()));
                });
            }
        }, 0, 1000);
    }

    // =================================================================
    //  HELPER FACTORIES
    // =================================================================

    /**
     * JLabel whose foreground tracks Theme at runtime.
     * @param dim  true = muted (textDim), false = primary (textMain)
     */
    private JLabel themedLabel(String text, int style, int size, boolean dim) {
        JLabel l = new JLabel(text) {
            @Override public Color getForeground() {
                return dim ? Theme.textDim() : Theme.textMain();
            }
        };
        l.setFont(new Font("SansSerif", style, size));
        return l;
    }

    /** JLabel whose foreground is always Theme.textMain(). */
    private JLabel dynamicMainLabel(String text) {
        JLabel l = new JLabel(text) {
            @Override public Color getForeground() { return Theme.textMain(); }
        };
        return l;
    }

    /** Centred placeholder label for empty forecast areas. */
    private JLabel placeholderLabel(String text) {
        JLabel l = new JLabel(text) {
            @Override public Color getForeground() { return Theme.textDim(); }
        };
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    /** JPanel with opaque background that paints Theme.bg(). */
    private JPanel transparentBgPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.bg());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setOpaque(true);
        return p;
    }

    // =================================================================
    //  RoundedButton  – pill-shaped custom button
    // =================================================================

    private static class RoundedButton extends JButton {
        private final Color bg;

        RoundedButton(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(fg);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 22, 10, 22));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color paint = getModel().isPressed()  ? bg.darker()
                        : getModel().isRollover() ? bg.brighter()
                        : bg;
            g2.setColor(paint);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            // Gloss highlight
            g2.setColor(new Color(0x20FFFFFF, true));
            g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =================================================================
    //  MAIN
    // =================================================================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("OptionPane.background",        new Color(0x3b3b3b));
            UIManager.put("Panel.background",             new Color(0x3b3b3b));
            UIManager.put("OptionPane.messageForeground", new Color(0xFFFFFF));
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(WeatherApp::new);
    }
}
