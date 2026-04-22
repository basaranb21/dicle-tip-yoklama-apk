package tr.edu.dicle.tip.yoklama;

import android.os.Bundle;
import android.view.WindowManager;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Ekran kaydini ve screenshot'i engelle — sinav sonuclari / QR kodu korunsun
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        );

        // Security plugin'leri WebView baslamadan kaydet
        registerPlugin(DeviceSecurityPlugin.class);

        super.onCreate(savedInstanceState);
    }
}
