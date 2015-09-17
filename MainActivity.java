package d1.com.dropboxtestd1;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;


public class MainActivity extends ActionBarActivity {

    private static final String APP_KEY = "49h94xleesirqj2";
    private static final String APP_SECRET = "ykc30r9q010gldn";
    private DropboxAPI<AndroidAuthSession> mDBApi;
    public static final String MY_BD_DIR = "Apps/theone";

    TextView tv;
    ProgressDialog dialog;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //normally a good idea to do before set content view
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        mDBApi = new DropboxAPI<>(session);

        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.output);
        Button button = (Button) findViewById(R.id.sync);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDBApi.getSession().startOAuth2Authentication(MainActivity.this);

            }
        });
    }

    protected void onResume() {
        super.onResume();

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
                //You'll need this token again after your app closes, so it's important to save it
                // for future access (though it's not shown here). If you don't, the user will have
                // to re-authenticate every time they use your app. A common way to implement storing
                // keys is through Android's SharedPreferences API.
                tv.setText("Access Token :" + accessToken + "\n");
            } catch (IllegalStateException e) {
                Log.e("dropbox ex", "Error authenticating", e);
            }

            AlertDialog.Builder bilder = new AlertDialog.Builder(MainActivity.this);
            bilder.setTitle("Write to file");
            final EditText editText = new EditText(MainActivity.this);
            bilder.setView(editText);
            bilder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    new CheckDropBoxDir().execute(editText.getText().toString());
                }
            });

            bilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            bilder.show();
        }
    }

    class CheckDropBoxDir extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Uploading...");
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            String contentToWrite = strings[0];

            //first check if the directory exist and also the file exists or not
            try {
                DropboxAPI.Entry entry = mDBApi.metadata("/" + MY_BD_DIR, 1, null, false, null);

                    if (entry.isDir) {
                        Log.e(TAG, "Directory exists");
                        return true;
                    }
            } catch (DropboxException e) {
                Log.e(TAG, "Directory not exists");
                DropboxAPI.Entry createDir = null;
                try {
                    createDir = mDBApi.createFolder("/" + MY_BD_DIR);
                    Log.e(TAG, "folder create" + createDir.rev);
                    return true;
                } catch (DropboxException e1) {
                    Log.e(TAG, "error in create dir", e1.fillInStackTrace());
                    return false;
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            dialog.dismiss();
            if (aBoolean){

            }
        }
    }
}
