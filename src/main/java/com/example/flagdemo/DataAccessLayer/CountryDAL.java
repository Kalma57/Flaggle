package com.example.flagdemo.DataAccessLayer;

public class CountryDAL {
    private int ID;
    private String countryName;
    private String code;
    private String flagPath;

    public CountryDAL(int ID, String countryName, String code, String flagPath){
        this.ID = ID;
        this.countryName = countryName;
        this. code = code;
        this.flagPath = flagPath;
    }

    public int getID() { return ID; }
    public String getCountryName() { return countryName; }
    public String getCode() { return code; }
    public String getFlagPath() { return flagPath; }

}
