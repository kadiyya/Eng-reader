# EngReader

Sesli ve hızlı okuma ile İngilizce akıcılığını geliştiren Android uygulaması.

Metin bir teleprompter gibi yukarı doğru kayar; sen de sarı banttaki satırı yüksek sesle okursun.
Her seviyede metin biraz daha zorlaşır, kayma hızı (kelime/dakika) artar.

## Özellikler

- **15 seviye**: A2'den C1'e, 100 → 210 kelime/dk.
- **Prompter**: 3-2-1 geri sayım, duraklat/devam, hızı anlık olarak ±10 k/dk ayarlama, parmakla geri sarma.
- **Sesli kontrol**: Android'in ses tanıması ile okuduğun kelimeler dinlenir; mikrofon seviye çubuğu ve
  "sesini duyamıyorum" uyarısı vardır. Bitince metnin yüzde kaçının anlaşıldığı gösterilir.
- **Puanlama**: Hedef hızın en az %90'ı ve kelimelerin en az %50'si anlaşılırsa seviye geçilir,
  bir sonraki seviye açılır. Hız ve doğruluğa göre 1–3 yıldız.
- **Serbest mod**: Kendi İngilizce metnini yapıştırıp aynı şekilde oku.

## APK'yı indirme

Her push'ta GitHub Actions APK'yı derler ve **Releases** sayfasına `EngReader.apk` olarak ekler.
Telefonda bu dosyayı indirip aç; "bilinmeyen kaynaklardan yüklemeye" izin vermen gerekebilir.

## Yerelde derleme

Android SDK kurulu bir makinede:

```
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`
