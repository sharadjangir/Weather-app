# Weather App ☀️🌧️

A **Java Swing Desktop Weather Forecast Application** that fetches real-time weather data using the **OpenWeatherMap API** and displays it in a modern UI dashboard.

## 🚀 Features

* 🌤 Real-time weather information
* 🔍 Search weather by city
* 🌡 Displays temperature, humidity, wind speed, pressure
* 📅 5-day weather forecast
* ⏰ Hourly forecast panel
* 🌙 Dark / Light mode UI
* ✅ Input validation for city names

## 🛠 Technologies Used

* **Java**
* **Java Swing (GUI)**
* **OpenWeatherMap API**
* **JSON Library**

## 📂 Project Structure

```
WeatherApp.java        → Main application UI
WeatherService.java    → API communication
WeatherData.java       → Weather data model
ForecastPanel.java     → Forecast UI panels
InputValidator.java    → Input validation
json-20240303.jar      → JSON parsing library
```

## ⚙️ Requirements

* Java 17 or higher
* Internet connection (for API requests)

## ▶️ How to Run

### 1️⃣ Compile the project

```bash
javac -cp ".;json-20240303.jar" *.java
```

### 2️⃣ Run the application

```bash
java -cp ".;json-20240303.jar" WeatherApp
```

## 📌 Note

The **Current Location feature** requires an IP-based location API because desktop Java applications cannot access device GPS directly.

## 👨‍💻 Author

**Sharad Jangir**

GitHub: https://github.com/sharadjangir

