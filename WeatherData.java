import java.util.List;

/**
 * WeatherData.java
 * ─────────────────────────────────────────────────────────────────
 * Data model class that holds all weather information fetched from
 * the OpenWeatherMap API.  Pure POJO – no logic, just getters/setters.
 * ─────────────────────────────────────────────────────────────────
 */
public class WeatherData {

    // ── Current weather ──────────────────────────────────────────
    private String cityName;
    private double temperature;      // °C
    private double feelsLike;        // °C
    private String condition;        // e.g. "Clear", "Rain"
    private String conditionIcon;    // OWM icon code e.g. "01d"
    private int    humidity;         // %
    private double windSpeed;        // km/h
    private int    pressure;         // hPa
    private int    uvIndex;
    private long   sunriseEpoch;     // Unix timestamp
    private long   sunsetEpoch;      // Unix timestamp

    // ── Hourly forecast (next few hours) ─────────────────────────
    private List<HourlyItem> hourlyForecast;

    // ── Daily forecast (next 5 days) ─────────────────────────────
    private List<DailyItem> dailyForecast;

    // ─────────────────────────────────────────────────────────────
    //  Inner class – one hourly slot
    // ─────────────────────────────────────────────────────────────
    public static class HourlyItem {
        private String time;          // "12:00"
        private double temperature;   // °C
        private String conditionIcon; // OWM icon code
        private double windSpeed;     // km/h

        public HourlyItem(String time, double temperature,
                          String conditionIcon, double windSpeed) {
            this.time          = time;
            this.temperature   = temperature;
            this.conditionIcon = conditionIcon;
            this.windSpeed     = windSpeed;
        }

        public String getTime()          { return time; }
        public double getTemperature()   { return temperature; }
        public String getConditionIcon() { return conditionIcon; }
        public double getWindSpeed()     { return windSpeed; }
    }

    // ─────────────────────────────────────────────────────────────
    //  Inner class – one daily slot
    // ─────────────────────────────────────────────────────────────
    public static class DailyItem {
        private String dayLabel;       // "Friday, 1 Sep"
        private double temperature;    // °C
        private String conditionIcon;  // OWM icon code

        public DailyItem(String dayLabel, double temperature, String conditionIcon) {
            this.dayLabel      = dayLabel;
            this.temperature   = temperature;
            this.conditionIcon = conditionIcon;
        }

        public String getDayLabel()      { return dayLabel; }
        public double getTemperature()   { return temperature; }
        public String getConditionIcon() { return conditionIcon; }
    }

    // ─────────────────────────────────────────────────────────────
    //  Getters & Setters – Current weather
    // ─────────────────────────────────────────────────────────────
    public String getCityName()          { return cityName; }
    public void   setCityName(String v)  { cityName = v; }

    public double getTemperature()       { return temperature; }
    public void   setTemperature(double v) { temperature = v; }

    public double getFeelsLike()         { return feelsLike; }
    public void   setFeelsLike(double v) { feelsLike = v; }

    public String getCondition()         { return condition; }
    public void   setCondition(String v) { condition = v; }

    public String getConditionIcon()     { return conditionIcon; }
    public void   setConditionIcon(String v) { conditionIcon = v; }

    public int    getHumidity()          { return humidity; }
    public void   setHumidity(int v)     { humidity = v; }

    public double getWindSpeed()         { return windSpeed; }
    public void   setWindSpeed(double v) { windSpeed = v; }

    public int    getPressure()          { return pressure; }
    public void   setPressure(int v)     { pressure = v; }

    public int    getUvIndex()           { return uvIndex; }
    public void   setUvIndex(int v)      { uvIndex = v; }

    public long   getSunriseEpoch()      { return sunriseEpoch; }
    public void   setSunriseEpoch(long v){ sunriseEpoch = v; }

    public long   getSunsetEpoch()       { return sunsetEpoch; }
    public void   setSunsetEpoch(long v) { sunsetEpoch = v; }

    // ─────────────────────────────────────────────────────────────
    //  Getters & Setters – Forecasts
    // ─────────────────────────────────────────────────────────────
    public List<HourlyItem> getHourlyForecast()              { return hourlyForecast; }
    public void             setHourlyForecast(List<HourlyItem> v) { hourlyForecast = v; }

    public List<DailyItem>  getDailyForecast()               { return dailyForecast; }
    public void             setDailyForecast(List<DailyItem> v)  { dailyForecast = v; }
}
