# 🌤 Hava Durumu Çubuğu — Weather Status Bar

Bildirim çubuğuna (status bar) anlık hava durumu ekleyen Android uygulaması.

## Özellikler
- ✅ Bildirim çubuğunda sıcaklık + ikon gösterimi
- ✅ 3 farklı API desteği (Open-Meteo ücretsiz, OpenWeatherMap, WeatherAPI)
- ✅ °C / °F seçimi
- ✅ 4 farklı ikon paketi (Emoji, Minimal, ASCII, Unicode)
- ✅ Yenileme sıklığı ayarı (15 dk - 3 saat)
- ✅ Telefon yeniden başladığında otomatik çalışır
- ✅ Koyu tema arayüz

## Derleme (Android Studio)

1. **Android Studio** indir: https://developer.android.com/studio
2. Bu klasörü aç: File > Open > WeatherStatusBar
3. Gradle sync tamamlanmasını bekle
4. **Run** butonuna bas veya:
   ```
   ./gradlew assembleDebug
   ```
   APK çıkış yolu: `app/build/outputs/apk/debug/app-debug.apk`

## Kullanım

1. Uygulamayı aç
2. Şehir adı gir (Türkçe karakter olmadan, örn: Istanbul)
3. API seç (Open-Meteo ücretsiz, anahtar gereksiz)
4. İkon paketi ve sıcaklık birimi seç
5. **Kaydet** → **Başlat**
6. Bildirim çubuğunda hava durumu görünür!

## API Anahtarları (isteğe bağlı)
- **OpenWeatherMap**: https://openweathermap.org/api (ücretsiz plan var)
- **WeatherAPI**: https://www.weatherapi.com (ücretsiz plan var)
