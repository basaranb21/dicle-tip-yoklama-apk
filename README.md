# Dicle Tip Fakultesi - Yoklama APK

Bu proje, Dicle Üniversitesi Tıp Fakültesi öğrencileri için Android uygulamasıdır.

## Özellikler

- **QR Yoklama** — Kamera ile QR okutarak devam kaydı
- **Tek Cihaz Kuralı** — Android ID ile hesap-cihaz kilidi
- **Mock GPS Tespiti** — Sahte konum uygulamalarını engeller
- **Root Cihaz Tespiti** — Rootlu telefonlardan yoklama alınmaz
- **Push Bildirim** — Yoklama açılınca, ders hatırlatma, kritik devamsızlık
- **Biyometrik Giriş** — Parmak izi / yüz tanıma
- **Sınav Sonuçları** — Ders bazlı, detay, optik form
- **Anket Doldurma** — Aktif anketleri listeler
- **Ekran Kaydı Engelleme** — QR ve sınav sonuçları korunur

## APK Build (GitHub Actions ile Otomatik)

### 1. GitHub'da yeni repo oluşturun
- `https://github.com/new` — public veya private
- İsim: `dicle-tip-yoklama-apk` (öneri)

### 2. Lokal repoyu pushlayın
```bash
cd C:/Users/bulentb/Desktop/otomasyonsistemleri/DicleTipYoklamaAPK
git init
git add .
git commit -m "Initial APK project"
git branch -M main
git remote add origin https://github.com/KULLANICI/dicle-tip-yoklama-apk.git
git push -u origin main
```

### 3. GitHub Actions otomatik çalışır
Push yaptığınız anda `Actions` sekmesinde build başlar. ~5 dakika sürer.

### 4. APK'yı indirin
- `Actions` sekmesinde son build'e tıklayın
- `Artifacts` altında `dicle-tip-yoklama-debug` varsa → indir
- ZIP'i açın → `app-debug.apk` çıkar

### 5. APK'yı sunucuya yükleyin
```bash
scp app-debug.apk otomasyon@10.1.1.212:/srv/otomasyon/apps/ders/media/apk/dicle-tip-yoklama.apk
```

### 6. Test edin
- Telefondan: `https://ders.otomasyon.dicle.edu.tr/accounts/indir/`
- APK İndir butonu → yükle → kullan

## Geliştirme (Lokal)

Android Studio kurulduysa:
```bash
npm install
npx cap sync android
npx cap open android
```

## Proje Yapısı

```
DicleTipYoklamaAPK/
├── android/                    # Native Android projesi
│   └── app/src/main/java/tr/edu/dicle/tip/yoklama/
│       ├── MainActivity.java
│       └── DeviceSecurityPlugin.java   # Root/mock/emulator tespit
├── www/                        # Web kaynak (splash)
│   ├── index.html
│   └── native-bridge.js
├── capacitor.config.json       # Capacitor ayarlari
├── package.json                # npm bagimliliklar
└── .github/workflows/
    └── build-apk.yml           # GitHub Actions otomatik build
```

## Güvenlik Özellikleri

| Özellik | Teknoloji |
|---|---|
| Tek cihaz kilidi | Android ID (sistem) |
| Mock GPS tespit | `Location.isMock()` |
| Root tespiti | 3 yöntem (build tags, su binary, xbin) |
| Emulator tespiti | Build fingerprint |
| Ekran kaydı engeli | `FLAG_SECURE` |
| HTTPS zorunlu | `network_security_config` |
| Sadece Dicle domain | Navigation whitelist |

## Üretim (Release) Build

Release APK için imzalama gerekir:

```bash
# Keystore oluştur (bir kere)
keytool -genkey -v -keystore dicle.jks -alias dicle -keyalg RSA -keysize 2048 -validity 10000

# GitHub Secrets'a ekle:
# - KEYSTORE_BASE64 (dicle.jks'in base64'ü)
# - KEYSTORE_PASSWORD
# - KEY_ALIAS = dicle
# - KEY_PASSWORD
```

Sonra `build-apk.yml`'deki release job'u aktif edilir.
