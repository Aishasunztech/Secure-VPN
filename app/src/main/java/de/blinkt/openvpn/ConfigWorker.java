package de.blinkt.openvpn;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.security.KeyChain;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.views.FileSelectLayout;

import static de.blinkt.openvpn.activities.MainActivity.TAG;


/**
 * @author Muhammad Nadeem
 * @Date 10/1/2019.
 */
public class ConfigWorker extends Worker {

    private Map<Utils.FileType, FileSelectLayout> fileSelectMap = new HashMap<>();
    private String mAliasName;

    public ConfigWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        List<CountryProfile> list = new ArrayList<>();
        list.add(new CountryProfile("America", R.drawable.flag_usa, "usa1.ovpn"));

        for (CountryProfile countryProfile : list) {
            Uri fileuri = Uri.fromFile(new File("//android_asset/" + countryProfile.getmAssetName()));
            List<String> mPathsegments = fileuri.getPathSegments();
            String possibleName = countryProfile.getmAssetName().substring(0, (countryProfile.getmAssetName().lastIndexOf(".")) - 1);

            possibleName = possibleName.replace(".ovpn", "");
            possibleName = possibleName.replace(".conf", "");
            startImportTask(null, possibleName, countryProfile.getmAssetName(), mPathsegments);
        }


        return null;
    }

    private void startImportTask(final Uri data, final String possibleName, String asset, List<String> mPathsegments) {

        Log.i(Utils.TAG, "startImportTask:  data is " + data + " ..... possible name is ...." + possibleName);

        try {
//                    InputStream is = getContentResolver().openInputStream(data);
//
//                    doImport(is);
//                    if (mResult==null)
//                        return -3;
            if (data == null) {


                Log.i(Utils.TAG, "doInBackground: assert file to read is ...  " + Utils.ASSETS_FILE);
                AssetManager am = getApplicationContext().getAssets();
                try {
                    InputStream is = am.open(asset);
                    ConfigParser cp = new ConfigParser();
                    try {
                        InputStreamReader isr = new InputStreamReader(is);

                        cp.parseConfig(isr);
                        VpnProfile mResult = cp.convertProfile();
                        String mEm = embedFiles(cp, mResult, mPathsegments);
                        userActionSaveProfile(mResult, possibleName, mEm);

                    } catch (IOException | ConfigParser.ConfigParseError e) {
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {

//                InputStream is = getContentResolver().openInputStream(data);
//
//                doImport(is);
//                if (mResult == null)
//                    return -3;

            }


        } catch (SecurityException se) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //checkMarschmallowFileImportError(data);
            }
        }

    }

    private String embedFiles(ConfigParser cp, VpnProfile mResult, List<String> mPathsegments) {
        // This where I would like to have a c++ style
        // void embedFile(std::string & option)

        if (mResult.mPKCS12Filename != null) {
            File pkcs12file = findFileRaw(mResult.mPKCS12Filename, mPathsegments);

            if (pkcs12file != null) {
                mAliasName = pkcs12file.getName().replace(".p12", "");
            } else {
                mAliasName = "Imported PKCS12";
            }
        }


        mResult.mCaFilename = embedFile(mResult.mCaFilename, Utils.FileType.CA_CERTIFICATE, false, mPathsegments);
        mResult.mClientCertFilename = embedFile(mResult.mClientCertFilename, Utils.FileType.CLIENT_CERTIFICATE, false, mPathsegments);
        mResult.mClientKeyFilename = embedFile(mResult.mClientKeyFilename, Utils.FileType.KEYFILE, false, mPathsegments);
        mResult.mTLSAuthFilename = embedFile(mResult.mTLSAuthFilename, Utils.FileType.TLS_AUTH_FILE, false, mPathsegments);
        mResult.mPKCS12Filename = embedFile(mResult.mPKCS12Filename, Utils.FileType.PKCS12, false, mPathsegments);
        mResult.mCrlFilename = embedFile(mResult.mCrlFilename, Utils.FileType.CRL_FILE, true, mPathsegments);

        String mEmbeddedPwFile = null;
        if (cp != null) {
            mEmbeddedPwFile = cp.getAuthUserPassFile();
            mEmbeddedPwFile = embedFile(cp.getAuthUserPassFile(), Utils.FileType.USERPW_FILE, false, mPathsegments);
        }
        return mEmbeddedPwFile;

    }

    private File findFileRaw(String filename, List<String> mPathsegments) {
        if (filename == null || filename.equals(""))
            return null;

        // Try diffent path relative to /mnt/sdcard
        File sdcard = Environment.getExternalStorageDirectory();
        File root = new File("/");

        HashSet<File> dirlist = new HashSet<>();

        for (int i = mPathsegments.size() - 1; i >= 0; i--) {
            String path = "";
            for (int j = 0; j <= i; j++) {
                path += "/" + mPathsegments.get(j);
            }
            // Do a little hackish dance for the Android File Importer
            // /document/primary:ovpn/openvpn-imt.conf


            if (path.indexOf(':') != -1 && path.lastIndexOf('/') > path.indexOf(':')) {
                String possibleDir = path.substring(path.indexOf(':') + 1, path.length());
                // Unquote chars in the  path
                try {
                    possibleDir = URLDecoder.decode(possibleDir, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }

                possibleDir = possibleDir.substring(0, possibleDir.lastIndexOf('/'));


                dirlist.add(new File(sdcard, possibleDir));

            }
            dirlist.add(new File(path));


        }
        dirlist.add(sdcard);
        dirlist.add(root);


        String[] fileparts = filename.split("/");
        for (File rootdir : dirlist) {
            String suffix = "";
            for (int i = fileparts.length - 1; i >= 0; i--) {
                if (i == fileparts.length - 1)
                    suffix = fileparts[i];
                else
                    suffix = fileparts[i] + "/" + suffix;

                File possibleFile = new File(rootdir, suffix);
                if (possibleFile.canRead())
                    return possibleFile;

            }
        }
        return null;
    }

    private String embedFile(String filename, Utils.FileType type, boolean onlyFindFileAndNullonNotFound, List<String> mPathsegments) {
        if (filename == null)
            return null;

        // Already embedded, nothing to do
        if (VpnProfile.isEmbedded(filename))
            return filename;

        File possibleFile = findFile(filename, type, mPathsegments);
        if (possibleFile == null)
            if (onlyFindFileAndNullonNotFound)
                return null;
            else
                return filename;
        else if (onlyFindFileAndNullonNotFound)
            return possibleFile.getAbsolutePath();
        else
            return readFileContent(possibleFile, type == Utils.FileType.PKCS12);

    }

    private File findFile(String filename, Utils.FileType fileType, List<String> mPathsegments) {
        File foundfile = findFileRaw(filename, mPathsegments);

        if (foundfile == null && filename != null && !filename.equals("")) {
//            log(R.string.import_could_not_open, filename);
        }
        fileSelectMap.put(fileType, null);

        return foundfile;
    }

    String readFileContent(File possibleFile, boolean base64encode) {
        byte[] filedata;
        try {
            filedata = readBytesFromFile(possibleFile);
        } catch (IOException e) {
            //log(e.getLocalizedMessage());
            return null;
        }

        String data;
        if (base64encode) {
            data = Base64.encodeToString(filedata, Base64.DEFAULT);
        } else {
            data = new String(filedata);

        }

        return VpnProfile.DISPLAYNAME_TAG + possibleFile.getName() + VpnProfile.INLINE_TAG + data;

    }

    private byte[] readBytesFromFile(File file) throws IOException {
        InputStream input = new FileInputStream(file);

        long len = file.length();
        if (len > VpnProfile.MAX_EMBED_FILE_SIZE)
            throw new IOException("File size of file to import too large.");

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) len];

        // Read in the bytes
        int offset = 0;
        int bytesRead;
        while (offset < bytes.length
                && (bytesRead = input.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += bytesRead;
        }

        input.close();
        return bytes;
    }

    private Intent installPKCS12(VpnProfile mResult) {

//        if (!((CheckBox) findViewById(R.id.importpkcs12)).isChecked()) {
//            setAuthTypeToEmbeddedPKCS12();
//            return null;
//
//        }
        String pkcs12datastr = mResult.mPKCS12Filename;
        if (VpnProfile.isEmbedded(pkcs12datastr)) {
            Intent inkeyIntent = KeyChain.createInstallIntent();

            pkcs12datastr = VpnProfile.getEmbeddedContent(pkcs12datastr);


            byte[] pkcs12data = Base64.decode(pkcs12datastr, Base64.DEFAULT);


            inkeyIntent.putExtra(KeyChain.EXTRA_PKCS12, pkcs12data);

            if (mAliasName.equals(""))
                mAliasName = null;

            if (mAliasName != null) {
                inkeyIntent.putExtra(KeyChain.EXTRA_NAME, mAliasName);
            }
            return inkeyIntent;

        }
        return null;
    }

    private boolean userActionSaveProfile(VpnProfile mResult, String posibleName, String mEmbeddedPwFile) {
        if (mResult == null) {
            Log.d(TAG, "userActionSaveProfile: ");
            return true;
        }

        mResult.mName = posibleName;
        ProfileManager vpl = ProfileManager.getInstance(getApplicationContext());
        if (vpl.getProfileByName(mResult.mName) != null) {
//            mProfilename.setError(getString(R.string.duplicate_profile_name));
            return true;
        }

        Intent in = installPKCS12(mResult);

        if (in != null) {
            //startActivityForResult(in, RESULT_INSTALLPKCS12);
        } else
            saveProfile(mResult, mEmbeddedPwFile);

        return true;
    }

    private void saveProfile(VpnProfile mResult, String mEmbeddedPwFile) {
        Intent result = new Intent();
        ProfileManager vpl = ProfileManager.getInstance(getApplicationContext());

        if (!TextUtils.isEmpty(mEmbeddedPwFile))
            ConfigParser.useEmbbedUserAuth(mResult, mEmbeddedPwFile);

        vpl.addProfile(mResult);
        vpl.saveProfile(getApplicationContext(), mResult);
        vpl.saveProfileList(getApplicationContext());
        result.putExtra(VpnProfile.EXTRA_PROFILEUUID, mResult.getUUID().toString());

    }
}

