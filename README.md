# Namaz Vakti (Kotlin, internetsiz)

Vakitleri internet olmadan, enlem/boylamdan astronomik olarak hesaplayan Android uygulaması + ana ekran widget'ı.

## Çalıştırma
1. Android Studio'da **Open** ile bu klasörü açın, Gradle sync bitsin (JDK 17 gerekir, Studio'nun kendi JDK'sı yeterli).
2. Telefonu bağlayıp **Run** deyin (minSdk 26 / Android 8.0).
3. Widget için: ana ekranda boş yere uzun basın → Widget'lar → **Namaz Vakti**.

## Yapı
| Dosya | Görev |
|---|---|
| `prayer/PrayerCalculator.kt` | Vakit hesabı (İmsak 18°, Yatsı 17°, İkindi Şafii, dakika ayarı) |
| `prayer/Cities.kt` | 81 il koordinatı |
| `prayer/PrayerEngine.kt` | Önbellek, "şu an / sıradaki vakit" mantığı |
| `widget/WidgetUpdater.kt` | Widget çizimi (Chronometer ile canlı geri sayım) + sonraki vakit alarmı |
| `receiver/AlarmReceiver.kt` | Vakit girince bildirim + widget yenileme |
| `receiver/SystemEventReceiver.kt` | Açılış / saat değişimi sonrası alarmı yeniden kurma |
| `MainActivity.kt` | Şehir seçimi, canlı geri sayım, vakit listesi, ayarlar |

## Doğruluk ayarı
Varsayılan dakika ayarları `PrayerCalculator.DEFAULT_OFFSETS` içindedir (İmsak 0, Güneş -7, Öğle +5, İkindi +4, Akşam +7, Yatsı 0).
Diyanet takvimiyle bire bir tutmak için uygulamadaki **İnce ayar (dakika)** düğmesinden vakit başına ±dakika girebilirsiniz.
