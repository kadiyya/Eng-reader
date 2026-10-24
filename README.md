# EngReader

Sesli ve hızlı okuma ile İngilizce akıcılığını geliştiren Android oyunu.

Metin bir teleprompter gibi yukarı doğru kayar; sen de sarı banttaki satırı yüksek sesle okursun.
Hızı sen seçmezsin: her seviyenin sabit bir hızı vardır ve metin ilerledikçe hızlanır.

## Harita ve dünyalar

Seviyeler bir haritada, 6 dünyaya yayılmış 30 durak olarak dizilir. Her dünyanın sonunda bir **boss** seviyesi vardır.
Tüm metinler telif hakkı olmayan (public domain) klasiklerden, Project Gutenberg kopyalarından birebir alınmıştır:

| Dünya | Eser | Hız |
| --- | --- | --- |
| 🌲 Fabl Ormanı | Aesop's Fables | 125–150 k/dk |
| 🐇 Harikalar Diyarı | Lewis Carroll — Alice's Adventures in Wonderland | 145–170 k/dk |
| 🕯️ Noel Şarkısı | Charles Dickens — A Christmas Carol | 150–180 k/dk |
| 🔍 Baker Sokağı | Arthur Conan Doyle — The Adventures of Sherlock Holmes | 160–190 k/dk |
| 🎩 Longbourn Malikânesi | Jane Austen — Pride and Prejudice | 165–195 k/dk |
| 🦇 Poe'nun Karanlık Şatosu | Edgar Allan Poe — The Raven, The Cask of Amontillado, The Masque of the Red Death, The Fall of the House of Usher, The Tell-Tale Heart | 170–210 k/dk |

## Oyun kuralları

- **Hız**: Seviyenin hızının %90'ında başlar, sonunda %110'una çıkar. Metni elle kaydıramazsın.
- **Canlı takip**: Ses tanıma okuduğun kelimeleri yeşile boyar; okumadan geçtiğin kelimeler kırmızıya döner.
- **3 can**: Banttan büyük ölçüde okunmadan geçen her satır bir can götürür. Canın biterse seviye biter.
- **Kombo**: Üst üste iyi okunan satırlar puan çarpanını x5'e kadar yükseltir.
- **Geçme şartı**: En az %60 doğruluk (boss seviyelerinde %70). Sonraki seviye ve dünya ancak böyle açılır.
- **Yıldızlar**: ★ geçtin · ★★ %78+ · ★★★ %90+ ve hiç can kaybetmeden.
- **Seri** 🔥: Her gün en az bir seviye geçersen günlük serin büyür.
- **Serbest mod** (✎): Kendi metnini 150 k/dk hızda oku.

Mikrofon izni verilmezse uygulama pratik modunda çalışır (puan ve can yok, seviye 1 yıldızla geçilir).

## APK'yı indirme

Her push'ta GitHub Actions APK'yı derler ve **Releases** sayfasına `EngReader.apk` olarak ekler.
Telefonda bu dosyayı indirip aç; "bilinmeyen kaynaklardan yüklemeye" izin vermen gerekebilir.

## Metinleri yeniden üretme

`app/src/main/assets/levels.json`, `tools/spec.py` içindeki seviye listesinden `tools/gen.py` ile üretilir.
Gutenberg metin dosyalarını bir klasöre indirip:

```
python3 tools/gen.py <metin-klasörü> app/src/main/assets/levels.json
```

## Yerelde derleme

Android SDK kurulu bir makinede: `./gradlew assembleDebug`
