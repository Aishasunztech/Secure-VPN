/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import de.blinkt.openvpn.model.CountryData;

import static android.content.Context.MODE_PRIVATE;

public class Utils {

    public static final String TAG  = "UTILS_TAG";



    public static final String FILE_TAG = "FILE_TAG";
    public static String ASSETS_FILE = "usa1_newyork1.ovpn";
    public static final String FILE_TYPE_ASSETS = "FILE_TYPE_ASSETS";
    public static final String FILE_TYPE_STORAGE = "FILE_TYPE_STORAGE";




    public static final String SUBSCRIBE_THREE_MONTHS_TAG = "SUBSCRIBE_THREE_MONTHS";
    public static final String SUBSCRIBE_SIX_MONTHS_TAG = "SUBSCRIBE_SIX_MONTHS";
    public static final String SUBSCRIBE_twelve_Months_TAG = "SUBSCRIBE_twelve_Months";


    public static String CURRENT_MONTHS = "CURRENT_MONTHS";


//
//    public static CountryData country_three_months_data =
//
//            new CountryData(
//                    // names
//                    new String[]{"Netherlands","GER - Berlin"},
//                    // flags
//                    new int[]{R.drawable.netharland_flg,R.drawable.germany_flg},
//                    // profiles
//                    new String[]{"netherland.ovpn", "grm1_berlin1.ovpn"}
//            );


    public static CountryData country_three_months_data =

            new CountryData(
                    // names
                    new String[]{"America"},
                    // flags
                    new int[]{R.drawable.flag_usa},
                    // profiles
                    new String[]{"usa1.ovpn"}
            );


    public static CountryData country_six_months_data =

            new CountryData(
                    // names
                    new String[]{},
                    // flags
                    new int[]{},
                    // profiles
                    new String[]{}
            );

    public static CountryData country_twelve_months_data =

            new CountryData(
                    // names
                    new String[]{},
                    // flags
                    new int[]{},
                    // profiles
                    new String[]{}
            );



    public static VpnProfile currentVpnProfile = null;


   private static final String prefrencekey = "prefrencekey";
   private static final String key_data_apps = "key_data_apps";


    private static final String PROFILE_COUNTRY_INDEX = "PROFILE_COUNTRY_INDEX";
    public static final String TOUR_STATUS = "IsTourDone";




    private static final String SUBSCRIBE_KEY = "SUBSCRIBE_KEY";
    private static final String SUBSCRIBE_DURATION = "SUBSCRIBE_DURATION";
    private static final String PROFILE_LOC = "PROFILE_LOC";



    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Intent getFilePickerIntent(Context c, FileType fileType) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        TreeSet<String> supportedMimeTypes = new TreeSet<String>();
        Vector<String> extensions = new Vector<String>();

        switch (fileType) {
            case PKCS12:
                i.setType("application/x-pkcs12");
                supportedMimeTypes.add("application/x-pkcs12");
                extensions.add("p12");
                extensions.add("pfx");
                break;
            case CLIENT_CERTIFICATE:
            case CA_CERTIFICATE:
                i.setType("application/x-pem-file");
                supportedMimeTypes.add("application/x-x509-ca-cert");
                supportedMimeTypes.add("application/x-x509-user-cert");
                supportedMimeTypes.add("application/x-pem-file");
                supportedMimeTypes.add("application/pkix-cert");
                supportedMimeTypes.add("text/plain");

                extensions.add("pem");
                extensions.add("crt");
                extensions.add("cer");
                break;
            case KEYFILE:
                i.setType("application/x-pem-file");
                supportedMimeTypes.add("application/x-pem-file");
                supportedMimeTypes.add("application/pkcs8");

                // Google drive ....
                supportedMimeTypes.add("application/x-iwork-keynote-sffkey");
                extensions.add("key");
                break;

            case TLS_AUTH_FILE:
                i.setType("text/plain");

                // Backup ....
                supportedMimeTypes.add("application/pkcs8");
                // Google Drive is kind of crazy .....
                supportedMimeTypes.add("application/x-iwork-keynote-sffkey");

                extensions.add("txt");
                extensions.add("key");
                break;

            case OVPN_CONFIG:
                i.setType("application/x-openvpn-profile");
                supportedMimeTypes.add("application/x-openvpn-profile");
                supportedMimeTypes.add("application/openvpn-profile");
                supportedMimeTypes.add("application/ovpn");
                supportedMimeTypes.add("text/plain");
                extensions.add("ovpn");
                extensions.add("conf");
                break;

            case CRL_FILE:
                supportedMimeTypes.add("application/x-pkcs7-crl");
                supportedMimeTypes.add("application/pkix-crl");
                extensions.add("crl");
                break;

            case USERPW_FILE:
                i.setType("text/plain");
                supportedMimeTypes.add("text/plain");
                break;
        }

        MimeTypeMap mtm = MimeTypeMap.getSingleton();

        for (String ext : extensions) {
            String mimeType = mtm.getMimeTypeFromExtension(ext);
            if (mimeType != null)
                supportedMimeTypes.add(mimeType);
        }

        // Always add this as fallback
        supportedMimeTypes.add("application/octet-stream");

        i.putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes.toArray(new String[supportedMimeTypes.size()]));

        // People don't know that this is actually a system setting. Override it ...
        // DocumentsContract.EXTRA_SHOW_ADVANCED is hidden
        i.putExtra("android.content.extra.SHOW_ADVANCED", true);

        /* Samsung has decided to do something strange, on stock Android GET_CONTENT opens the document UI */
        /* fist try with documentsui */
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N)
            i.setPackage("com.android.documentsui");




        //noinspection ConstantConditions
        if (!isIntentAvailable(c,i)) {
                i.setAction(Intent.ACTION_OPEN_DOCUMENT);
            i.setPackage(null);

            // Check for really broken devices ... :(
            if (!isIntentAvailable(c,i)) {
                return null;
            }
        }


        /*
        final PackageManager packageManager = c.getPackageManager();
        ResolveInfo list = packageManager.resolveActivity(i, 0);

        Toast.makeText(c, "Starting package: "+ list.activityInfo.packageName
                + "with ACTION " + i.getAction(), Toast.LENGTH_LONG).show();

        */
        return i;
    }


    public static boolean isIntentAvailable(Context context, Intent i) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(i,
                        PackageManager.MATCH_DEFAULT_ONLY);

        // Ignore the Android TV framework app in the list
        int size = list.size();
        for (ResolveInfo ri: list)
        {
            // Ignore stub apps
            if ("com.google.android.tv.frameworkpackagestubs".equals(ri.activityInfo.packageName))
            {
                size--;
            }
        }

        return size > 0;
    }


    public enum FileType {
        PKCS12(0),
        CLIENT_CERTIFICATE(1),
        CA_CERTIFICATE(2),
        OVPN_CONFIG(3),
        KEYFILE(4),
        TLS_AUTH_FILE(5),
        USERPW_FILE(6),
        CRL_FILE(7);

        private int value;

        FileType(int i) {
            value = i;
        }

        public static FileType getFileTypeByValue(int value) {
            switch (value) {
                case 0:
                    return PKCS12;
                case 1:
                    return CLIENT_CERTIFICATE;
                case 2:
                    return CA_CERTIFICATE;
                case 3:
                    return OVPN_CONFIG;
                case 4:
                    return KEYFILE;
                case 5:
                    return TLS_AUTH_FILE;
                case 6:
                    return USERPW_FILE;
                case 7:
                    return CRL_FILE;
                default:
                    return null;
            }
        }

        public int getValue() {
            return value;
        }
    }

    static private byte[] readBytesFromStream(InputStream input) throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        ;

        long totalread = 0;
        while ((nRead = input.read(data, 0, data.length)) != -1 && totalread <VpnProfile.MAX_EMBED_FILE_SIZE ) {
            buffer.write(data, 0, nRead);
            totalread+=nRead;
        }

        buffer.flush();
        input.close();
        return buffer.toByteArray();
    }

    public static String getFilePickerResult(FileType ft, Intent result, Context c) throws IOException, SecurityException {

        Uri uri = result.getData();
        if (uri == null)
            return null;

        byte[] fileData = readBytesFromStream(c.getContentResolver().openInputStream(uri));
        String newData = null;

        Cursor cursor = c.getContentResolver().query(uri, null, null, null, null);

        String prefix = "";
        try {
            if (cursor!=null && cursor.moveToFirst()) {
                int cidx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (cidx != -1) {
                    String displayName = cursor.getString(cidx);

                    if (!displayName.contains(VpnProfile.INLINE_TAG) && !displayName.contains(VpnProfile.DISPLAYNAME_TAG))
                        prefix = VpnProfile.DISPLAYNAME_TAG + displayName;
                }
            }
        } finally {
            if(cursor!=null)
                cursor.close();
        }

        switch (ft) {
            case PKCS12:
                newData = Base64.encodeToString(fileData, Base64.DEFAULT);
                break;
            default:
                newData = new String(fileData, "UTF-8");
                break;
        }

        return prefix + VpnProfile.INLINE_TAG + newData;
    }

    public static String determineTermuxArchName() {
        // Note that we cannot use System.getProperty("os.arch") since that may give e.g. "aarch64"
        // while a 64-bit runtime may not be installed (like on the Samsung Galaxy S5 Neo).
        // Instead we search through the supported abi:s on the device, see:
        // http://developer.android.com/ndk/guides/abis.html
        // Note that we search for abi:s in preferred order (the ordering of the
        // Build.SUPPORTED_ABIS list) to avoid e.g. installing arm on an x86 system where arm
        // emulation is available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (String androidArch : Build.SUPPORTED_ABIS) {
                switch (androidArch) {
                    case "arm64-v8a":
                        return "arm64-v8a";
                    //  return "aarch64";
                    case "armeabi-v7a":
                        return "armeabi-v7a";
                    //  return "arm";
                    case "x86_64":
                        return "x86_64";
                    //  return "x86_64";
                    case "x86":
                        return "x86";
                    //  return "i686";
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            throw new RuntimeException("Unable to determine arch from Build.SUPPORTED_ABIS =  " +
//                    Arrays.toString(Build.SUPPORTED_ABIS));
            return "error";
        }else{
            return "error";
        }
    }


    public static void saveList(Context activity, ArrayList<String> list) {
        SharedPreferences prefs = activity.getSharedPreferences(prefrencekey, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key_data_apps, json);
        editor.apply();     // This line is IMPORTANT !!!
    }

    public static ArrayList<String> getList(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(prefrencekey, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(key_data_apps, null);
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return gson.fromJson(json, type);
    }



    public static int  getSelectedCountry(Context context){

        SharedPreferences sharedPreferences = context.getSharedPreferences("Sub_Pref", Context.MODE_PRIVATE);
        return  sharedPreferences.getInt(PROFILE_COUNTRY_INDEX,45268);
    }

    public static void setSelectedCountry(Context context, int country_index){


        SharedPreferences sharedPreferences = context.getSharedPreferences("Sub_Pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PROFILE_COUNTRY_INDEX,country_index);
        editor.apply();

    }

    public static void setSubscribeDuration(Context context, boolean is_subscribe , String subscribe_duration){
        SharedPreferences sharedPreferences = context.getSharedPreferences("Sub_Pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SUBSCRIBE_KEY,is_subscribe);
        editor.putString(SUBSCRIBE_DURATION,subscribe_duration);
        editor.apply();
        if(subscribe_duration.equals(SUBSCRIBE_THREE_MONTHS_TAG)){
            Toast.makeText(context, "Your Three Months Subscription Start", Toast.LENGTH_SHORT).show();
        }else if(subscribe_duration.equals(SUBSCRIBE_SIX_MONTHS_TAG)){
            Toast.makeText(context, "Your Six Months Subscription Start", Toast.LENGTH_SHORT).show();
        }else if(subscribe_duration.equals(SUBSCRIBE_twelve_Months_TAG)){
            Toast.makeText(context, "Your Twelve Months Subscription Start", Toast.LENGTH_SHORT).show();
        }

    }

    public static String getSubscribeDuration(Context context){

        // return null if not subscribe else return duration of subscription

        SharedPreferences sharedPreferences = context.getSharedPreferences("Sub_Pref", Context.MODE_PRIVATE);
        return sharedPreferences.getString(SUBSCRIBE_DURATION,null);

    }


    public static boolean isNetworkConnected(Context context){

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return connectivityManager.getActiveNetworkInfo()!=null && connectivityManager.getActiveNetworkInfo().isConnected();
    }



    public static String getCurrentStatus(Context context){

        SharedPreferences sharedPreferences = context.getSharedPreferences("Sub_Pref", Context.MODE_PRIVATE);

        return sharedPreferences.getString("yyyyyyyo",null);
    }


   public static void setCurrentStatus(Context context, String s){

       SharedPreferences sharedPreferences = context.getSharedPreferences("Sub_Pref", Context.MODE_PRIVATE);
       SharedPreferences.Editor editor = sharedPreferences.edit();
       editor.putString("yyyyyyyo",s);
       editor.apply();
    }



}
