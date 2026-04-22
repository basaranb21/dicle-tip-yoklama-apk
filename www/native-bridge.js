/**
 * Native Bridge — APK icinden cagrilabilecek guvenlik fonksiyonlari
 *
 * WebView'da yuklenen ders.otomasyon.dicle.edu.tr sayfasi bu bridge uzerinden
 * native cihaz ID, GPS mock tespiti, biyometrik kontrolu yapabilir.
 *
 * Django tarafi: window.DicleNative.getDeviceId() gibi cagrilarla kullanir.
 */
(function() {
  if (typeof window.DicleNative !== 'undefined') return;

  window.DicleNative = {
    isNative: true,
    platform: 'android',
    appVersion: '1.0.0',

    // ── Cihaz ID (sabit, kullanici silemez) ────────────────────
    getDeviceId: function() {
      return new Promise(function(resolve) {
        if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Device) {
          window.Capacitor.Plugins.Device.getId().then(function(r) {
            resolve(r.identifier || r.uuid);
          }).catch(function() { resolve(null); });
        } else {
          resolve(null);
        }
      });
    },

    // ── GPS (yuksek hassasiyet, mock tespit) ────────────────────
    getLocation: function() {
      return new Promise(function(resolve, reject) {
        if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Geolocation) {
          window.Capacitor.Plugins.Geolocation.getCurrentPosition({
            enableHighAccuracy: true,
            timeout: 15000,
            maximumAge: 5000
          }).then(function(pos) {
            resolve({
              latitude: pos.coords.latitude,
              longitude: pos.coords.longitude,
              accuracy: pos.coords.accuracy,
              // Not: Capacitor default'da mock tespiti yapmaz, native plugin yaziliyor
              isMock: false
            });
          }).catch(reject);
        } else {
          reject(new Error('Capacitor Geolocation yok'));
        }
      });
    },

    // ── Biyometrik dogrulama ────────────────────────────────────
    authenticate: function(reason) {
      return new Promise(function(resolve) {
        if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.BiometricAuth) {
          window.Capacitor.Plugins.BiometricAuth.verify({
            reason: reason || 'Yoklama icin kimlik dogrulamasi',
            title: 'Dicle Tip Yoklama'
          }).then(function() { resolve(true); })
            .catch(function() { resolve(false); });
        } else {
          resolve(true); // Desteklenmiyorsa gec
        }
      });
    },

    // ── Push notification token ─────────────────────────────────
    getPushToken: function() {
      return new Promise(function(resolve) {
        try {
          var t = localStorage.getItem('fcm_token');
          resolve(t);
        } catch(e) { resolve(null); }
      });
    },

    // ── Cihaz bilgisi (tani icin) ───────────────────────────────
    getDeviceInfo: function() {
      return new Promise(function(resolve) {
        if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Device) {
          window.Capacitor.Plugins.Device.getInfo().then(function(info) {
            resolve({
              model: info.model,
              manufacturer: info.manufacturer,
              osVersion: info.osVersion,
              platform: info.platform,
              isVirtual: info.isVirtual, // Emulator tespiti
              memUsed: info.memUsed
            });
          }).catch(function() { resolve({}); });
        } else {
          resolve({});
        }
      });
    },

    // ── Network durumu ──────────────────────────────────────────
    getNetworkStatus: function() {
      return new Promise(function(resolve) {
        if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Network) {
          window.Capacitor.Plugins.Network.getStatus().then(resolve);
        } else {
          resolve({ connected: navigator.onLine });
        }
      });
    },

    // ── Titresim (haptic feedback) ──────────────────────────────
    vibrate: function(type) {
      if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Haptics) {
        var ImpactStyle = { Light: 'LIGHT', Medium: 'MEDIUM', Heavy: 'HEAVY' };
        window.Capacitor.Plugins.Haptics.impact({ style: ImpactStyle[type] || 'MEDIUM' });
      } else if (navigator.vibrate) {
        navigator.vibrate(type === 'Heavy' ? 200 : 100);
      }
    }
  };

  // Django sayfasina haber ver — native ortam var
  try {
    localStorage.setItem('is_native_app', '1');
    localStorage.setItem('native_platform', 'android');
  } catch(e) {}

  console.log('[DicleNative] Bridge yuklendi - v' + window.DicleNative.appVersion);
})();
