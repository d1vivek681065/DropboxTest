package d1.com.dropboxtestd1;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MainActivity extends ActionBarActivity {

    private static final String APP_KEY = "49h94xleesirqj2";
    private static final String APP_SECRET = "ykc30r9q010gldn";
    private DropboxAPI<AndroidAuthSession> mDBApi;
    public static final String MY_BD_DIR = "Apps/theone";

    TextView tv;
    ProgressDialog dialog;
    private static final String TAG = "MainActivity";
    private static File sdcard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sdcard = Environment.getExternalStorageDirectory();
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
//                    new CheckDropBoxDir().execute(editText.getText().toString());
                    new CheckDropBoxFile().execute(editText.getText().toString());
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
            DropboxAPI.Entry entry = null;
            try {
                entry = mDBApi.metadata("/" + MY_BD_DIR, 1, null, false, null);

                if (entry.isDir) {
                    Log.e(TAG, "Directory exists : " + entry.fileName());
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
            if (aBoolean) {

            }
        }
    }

    class CheckDropBoxFile extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                DropboxAPI.Entry fileCheck = mDBApi.metadata("/" + "theone.txt", 1, null, false, null);//exception
                Log.e(TAG, "fileCheck :" + fileCheck.fileName());
                if (fileCheck.fileName().equals("theone.txt")) {
                    Log.e(TAG, "File Exists: " + fileCheck.fileName());
                    //download the file get content modify it and upload again
                    downladAndUpload(strings[0]);
                } else {
                    Log.e(TAG, "file not exists nor exception");
                }
            } catch (DropboxException e) {
                Log.e(TAG, "file not exists");
                //file not exists so create one
                createAndUploadFile(strings[0]);
            }
            return null;
        }
    }

    private void downladAndUpload(String contentToAppend) {
        //download the file get content and append the input here
        File file = new File(sdcard.getAbsolutePath() + "/" + "theone.txt");
        String fileContent = "";
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));

            DropboxAPI.DropboxFileInfo info = mDBApi.getFile("/" + "theone.txt", null, out, null);
            Log.e(TAG, "file download " + info.getMetadata().rev);

            //get content append it with supplied content and upload
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                char current;
                while (fis.available() > 0) {
                    current = (char) fis.read();
                    Log.e(TAG, "read from fis :" + current);
                    fileContent = fileContent + String.valueOf(current);
                }
                Log.e(TAG, "fileContent ::" + fileContent);

                if (fis != null) {
                    fis.close();
                }
            }
            fileContent = fileContent + contentToAppend;
            createAndUploadFile(fileContent);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "fiel not found while download", e.fillInStackTrace());
        } catch (DropboxException e) {
            Log.e(TAG, "db ex while downlad", e.fillInStackTrace());
        } catch (IOException e) {
            Log.e(TAG, "io ex while fis.available() || fis.read()");
        }
    }

    // read text from file
    // write text to file
    public boolean writeToFile(String toWriteContent, String writeName) {
        boolean myuploadStatus = false;
        sdcard = Environment.getExternalStorageDirectory();
        // add-write text into file
        File file_local = new File(sdcard.getAbsolutePath() + "/" + writeName);
        try {
            // This not contain PATH FileOutputStream fileout = openFileOutput(writePath, MODE_PRIVATE);

            // File dir = new File (sdCard.getAbsolutePath());
            // File file_local = new File(dir, writeName);
            FileOutputStream fileout = new FileOutputStream(file_local);
            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
            outputWriter.write(toWriteContent);
            outputWriter.close();
            Log.e(TAG, "File saved successfully");
            myuploadStatus = true;
            if (fileout != null) {
                fileout.close();
            }

        } catch (Exception e) {
            myuploadStatus = false;
            e.printStackTrace();
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        Log.e(TAG, "writeToFile" + myuploadStatus);
        return myuploadStatus;
    }

    private void createAndUploadFile(String fileContent) {
        boolean isWrite = writeToFile(fileContent, "theone.txt");

        File getFile = new File(sdcard.getAbsolutePath() + "/" + "theone.txt");
        try {
            FileInputStream inputStream = new FileInputStream(getFile);
            Log.e(TAG, "upto here is ok");
            DropboxAPI.Entry response = mDBApi.putFileOverwrite("/" + "theone.txt", inputStream, getFile.length(), null); //"/" + "Apps/theone" +
            Log.e(TAG, "EXAmple log upload file review : " + response.rev);
        } catch (DropboxException e1) {
            Log.e(TAG, "ex while upload", e1.fillInStackTrace());
        } catch (FileNotFoundException e1) {
            Log.e(TAG, "file not found while input stream", e1.fillInStackTrace());
        }

    }
}
