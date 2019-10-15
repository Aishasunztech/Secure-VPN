package de.blinkt.openvpn;

/**
 * @author Muhammad Nadeem
 * @Date 10/2/2019.
 */
public class CountryProfile {
    private String mName;
    private int mFlagId;
    private String mAssetName;

    public CountryProfile(String mName, int mFlagId, String mAssetName) {
        this.mName = mName;
        this.mFlagId = mFlagId;
        this.mAssetName = mAssetName;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public int getmFlagId() {
        return mFlagId;
    }

    public void setmFlagId(int mFlagId) {
        this.mFlagId = mFlagId;
    }

    public String getmAssetName() {
        return mAssetName;
    }

    public void setmAssetName(String mAssetName) {
        this.mAssetName = mAssetName;
    }
}
