import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * WeatherService.java
 * ─────────────────────────────────────────────────────────────────
 * Handles all communication with the OpenWeatherMap API.
 *
 * APIs used:
 *   • Current weather  – /data/2.5/weather
 *   • 3-hour forecast  – /data/2.5/forecast   (gives 5-day / hourly)
 *   • UV index         – /data/2.5/uvi
 *
 * Replace API_KEY with your own key from openweathermap.org
 * ─────────────────────────────────────────────────────────────────
 */
public class WeatherService {

    // ── ⚠  Replace with your OpenWeatherMap API key ──────────────
    private static final String API_KEY  = "35fd51b106255ddfa2dbf36269d1f635";

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String UNITS    = "metric";   // Celsius
    private static final int    TIMEOUT  = 10_000;     // 10 seconds

    // ─────────────────────────────────────────────────────────────
    //  Public entry point – fetches everything for a city
    // ─────────────────────────────────────────────────────────────

    /**
     * Fetches complete weather data for the given city name.
     *
     * @param cityName validated city name (from InputValidator)
     * @return populated {@link WeatherData} object
     * @throws WeatherException on any API or network error
     */
    public WeatherData fetchWeather(String cityName) throws WeatherException {

        String encodedCity;
        try {
            encodedCity = URLEncoder.encode(cityName, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new WeatherException("Failed to encode city name: " + e.getMessage());
        }

        // Step 1 – current weather (gives us lat/lon for UV call)
        JSONObject currentJson  = getJson(BASE_URL + "weather?q=" + encodedCity
                                          + "&units=" + UNITS + "&appid=" + API_KEY);

        // Step 2 – 5-day / 3-hour forecast
        JSONObject forecastJson = getJson(BASE_URL + "forecast?q=" + encodedCity
                                          + "&units=" + UNITS + "&appid=" + API_KEY);

        // Step 3 – UV index (requires lat/lon from current weather)
        double lat = currentJson.getJSONObject("coord").getDouble("lat");
        double lon = currentJson.getJSONObject("coord").getDouble("lon");
        JSONObject uvJson = getJson(BASE_URL + "uvi?lat=" + lat + "&lon=" + lon
                                    + "&appid=" + API_KEY);

        return parseWeatherData(currentJson, forecastJson, uvJson);
    }

    // ─────────────────────────────────────────────────────────────
    //  JSON parser – maps raw API JSON → WeatherData model
    // ─────────────────────────────────────────────────────────────

    private WeatherData parseWeatherData(JSONObject current,
                                         JSONObject forecast,
                                         JSONObject uvJson) {
        WeatherData data = new WeatherData();

        // ── City name ─────────────────────────────────────────
        data.setCityName(current.getString("name"));

        // ── Main metrics ──────────────────────────────────────
        JSONObject main = current.getJSONObject("main");
        data.setTemperature(main.getDouble("temp"));
        data.setFeelsLike(main.getDouble("feels_like"));
        data.setHumidity(main.getInt("humidity"));
        data.setPressure(main.getInt("pressure"));

        // ── Weather condition ─────────────────────────────────
        JSONObject weather = current.getJSONArray("weather").getJSONObject(0);
        data.setCondition(weather.getString("main"));
        data.setConditionIcon(weather.getString("icon"));

        // ── Wind ─────────────────────────────────────────────
        // OWM returns m/s; convert to km/h
        double windMs = current.getJSONObject("wind").getDouble("speed");
        data.setWindSpeed(Math.round(windMs * 3.6 * 10.0) / 10.0);

        // ── Sunrise / Sunset ─────────────────────────────────
        JSONObject sys = current.getJSONObject("sys");
        data.setSunriseEpoch(sys.getLong("sunrise"));
        data.setSunsetEpoch(sys.getLong("sunset"));

        // ── UV index ─────────────────────────────────────────
        data.setUvIndex((int) Math.round(uvJson.getDouble("value")));

        // ── Hourly forecast (next 5 slots = 15 hours) ─────────
        JSONArray  slots       = forecast.getJSONArray("list");
        List<WeatherData.HourlyItem> hourlyList = new ArrayList<>();
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");

        // Take up to 5 slots (each slot = 3 hours)
        int hourlyCount = Math.min(5, slots.length());
        for (int i = 0; i < hourlyCount; i++) {
            JSONObject slot = slots.getJSONObject(i);

            long   dtEpoch   = slot.getLong("dt") * 1000L;
            String timeLabel = timeFmt.format(new Date(dtEpoch));
            double temp      = slot.getJSONObject("main").getDouble("temp");
            String icon      = slot.getJSONArray("weather")
                                   .getJSONObject(0).getString("icon");
            double wSpeed    = Math.round(
                slot.getJSONObject("wind").getDouble("speed") * 3.6 * 10.0) / 10.0;

            hourlyList.add(new WeatherData.HourlyItem(timeLabel, temp, icon, wSpeed));
        }
        data.setHourlyForecast(hourlyList);

        // ── Daily forecast (next 5 days – noon slot per day) ──
        List<WeatherData.DailyItem> dailyList = new ArrayList<>();
        SimpleDateFormat dayFmt  = new SimpleDateFormat("EEEE, d MMM");
        String           today   = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        int              daysDone = 0;

        for (int i = 0; i < slots.length() && daysDone < 5; i++) {
            JSONObject slot    = slots.getJSONObject(i);
            String     dtTxt   = slot.getString("dt_txt");          // "2024-09-01 12:00:00"
            String     slotDay = dtTxt.substring(0, 10);

            // Pick the 12:00 slot for each day (skip today's date)
            if (!slotDay.equals(today) && dtTxt.contains("12:00:00")) {
                long   dtEpoch   = slot.getLong("dt") * 1000L;
                String dayLabel  = dayFmt.format(new Date(dtEpoch));
                double temp      = slot.getJSONObject("main").getDouble("temp");
                String icon      = slot.getJSONArray("weather")
                                       .getJSONObject(0).getString("icon");

                dailyList.add(new WeatherData.DailyItem(dayLabel, temp, icon));
                daysDone++;
            }
        }
        data.setDailyForecast(dailyList);

        return data;
    }

    // ─────────────────────────────────────────────────────────────
    //  HTTP helper – GETs a URL and returns parsed JSON
    // ─────────────────────────────────────────────────────────────

    private JSONObject getJson(String urlString) throws WeatherException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();

            // ── Handle API-level errors ───────────────────────
            if (status == 401) throw new WeatherException("Invalid API key. Please check your OpenWeatherMap API key.");
            if (status == 404) throw new WeatherException("City not found. Please check the city name and try again.");
            if (status == 429) throw new WeatherException("Too many requests. Please wait a moment before trying again.");
            if (status != 200) throw new WeatherException("API error (HTTP " + status + "). Please try again later.");

            // ── Read response body ────────────────────────────
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            return new JSONObject(sb.toString());

        } catch (WeatherException e) {
            throw e; // re-throw our own exceptions
        } catch (java.net.UnknownHostException e) {
            throw new WeatherException("No internet connection.\nPlease check your network and try again.");
        } catch (java.net.SocketTimeoutException e) {
            throw new WeatherException("Connection timed out.\nThe server is taking too long to respond.");
        } catch (Exception e) {
            throw new WeatherException("Network error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Custom exception class
    // ─────────────────────────────────────────────────────────────

    /**
     * Thrown by WeatherService for any API / network problem.
     * Always contains a human-readable message suitable for dialogs.
     */
    public static class WeatherException extends Exception {
        public WeatherException(String message) {
            super(message);
        }
    }
}
