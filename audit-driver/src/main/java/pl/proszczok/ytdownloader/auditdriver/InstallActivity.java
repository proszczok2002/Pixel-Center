package pl.proszczok.ytdownloader.auditdriver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/** Opens the Android installer; the audit helper never silently installs updates. */
public class InstallActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(Uri.parse("content://pl.proszczok.ytdownloader.auditdriver.candidate/candidate.apk"),
                "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(install);
        finish();
    }
}
