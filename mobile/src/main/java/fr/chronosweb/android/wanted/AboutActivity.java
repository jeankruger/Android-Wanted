package fr.chronosweb.android.wanted;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import fr.chronosweb.android.wanted.R;

/**
 * Created by Jean Kruger on 13/07/14.
 * http://www.chronos-web.fr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/


public class AboutActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(this.getPackageName(), 0);

            TextView textViewName = (TextView)findViewById(R.id.textViewName);
            TextView textViewVersion = (TextView)findViewById(R.id.textViewVersion);

            String pname = pInfo.applicationInfo.loadLabel(getPackageManager()).toString();
            String pversion = pInfo.versionName;

            textViewName.setText(pname);
            textViewVersion.setText(pversion);
        } catch (PackageManager.NameNotFoundException e1) {
            e1.printStackTrace();
        }

        WebView webView = (WebView)findViewById(R.id.webViewAbout);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        String language = getResources().getConfiguration().locale.getLanguage();

        String aboutFileName = "file:///android_asset/about/about_en.html";

        AssetManager am = getAssets();
        try {
            InputStream is = am.open("about/about_" + language + ".html", AssetManager.ACCESS_STREAMING);
            is.close();
            aboutFileName = "file:///android_asset/about/about_" + language + ".html";
        } catch (IOException e) {}

        webView.loadUrl(aboutFileName);
    }
}
