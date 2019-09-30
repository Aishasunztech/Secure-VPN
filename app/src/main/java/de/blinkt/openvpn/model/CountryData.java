package de.blinkt.openvpn.model;

public class CountryData {

    private String[] countries_names;
    private int[] countries_flgs;
    private String[] countries_profiles;

    public CountryData() {
    }

    public CountryData(String[] countries_names, int[] countries_flgs, String[] countries_profiles) {
        this.countries_names = countries_names;
        this.countries_flgs = countries_flgs;
        this.countries_profiles = countries_profiles;
    }

    public String[] getCountries_names() {
        return countries_names;
    }

    public void setCountries_names(String[] countries_names) {
        this.countries_names = countries_names;
    }

    public int[] getCountries_flgs() {
        return countries_flgs;
    }

    public void setCountries_flgs(int[] countries_flgs) {
        this.countries_flgs = countries_flgs;
    }

    public String[] getCountries_profiles() {
        return countries_profiles;
    }

    public void setCountries_profiles(String[] countries_profiles) {
        this.countries_profiles = countries_profiles;
    }
}
