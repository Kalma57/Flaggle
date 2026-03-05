package com.example.flagdemo.DataAccessLayer;

public class CountryDAL {
    private int ID;
    private String countryName;
    private String code;
    private String flagPath;
    private double latitude;
    private double longitude;
    private String neighborsList;

    public CountryDAL(int ID, String countryName, String code, String flagPath, double lat, double longitude, String neigbors){
        this.ID = ID;
        this.countryName = countryName;
        this. code = code;
        this.flagPath = flagPath;
        this.latitude = lat;
        this.longitude = longitude;
        this.neighborsList = neigbors;

    }

    public int getID() { return ID; }
    public String getCountryName() { return countryName; }
    public String getCode() { return code; }
    public String getFlagPath() { return flagPath; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getNeighborsList() { return neighborsList; }


}
