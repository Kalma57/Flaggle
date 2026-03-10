package com.example.flagdemo.BusinessLayer;

import com.example.flagdemo.DataAccessLayer.CountryDAL;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class CountryBL {
    private String name;
    private int ID;
    private String flagPath;
    private double latitude;
    private double longitude;
    private String neighbors;
    private String iso3;

    public CountryBL(String name, int ID, String flagPath, double lat, double longi, String neighbors, String iso3) {
        this.name = name;
        this.ID = ID;
        this.flagPath = flagPath;
        this.latitude = lat;
        this.longitude = longi;
        this.neighbors = neighbors;
        this.iso3 = iso3;
    }

    public CountryBL() {}

    // FIXED: Now copies all data fields from CountryDAL, not just a few!
    public CountryBL(CountryDAL cd) {
        this.name = cd.getCountryName();
        this.ID = cd.getID();
        this.flagPath = cd.getFlagPath();
        this.latitude = cd.getLatitude();
        this.longitude = cd.getLongitude();
        this.neighbors = cd.getNeighborsList();
        this.iso3 = cd.getIso3();
    }

    /**
     * Returns the name of the country.
     *
     * @return The country's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the country's ID.
     *
     * @return The country's ID.
     */
    public int getCode() {
        return ID;
    }

    /**
     * Returns the URL of the country's flag image.
     *
     * @return The flag image URL.
     */
    public String getFlagPath() {
        return flagPath;
    }

    /**
     * Returns the BufferedImage of the country's flag.
     * If the flag image has not been loaded yet, it will be fetched from the URL.
     *
     * @return The flag image as a BufferedImage, or {@code null} if loading fails.
     */
    @JsonIgnore
    public BufferedImage getFlagImage() {
        // FIXED: Extracts just the file name and generates an accurate absolute path to the new directory.
        String fileName = Paths.get(flagPath).getFileName().toString();
        String correctPath = Paths.get("src/main/resources/static/DB/FlagsImages", fileName).toAbsolutePath().toString();

        File file = new File(correctPath);

        if (!file.exists()) {
            System.err.println("Flag file not found: " + file.getAbsolutePath());
            return null;
        }

        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getNeighborsList() { return neighbors; }
    public String getIso3() { return iso3; }

    /**
     * Compares this country to another object based on the country ID.
     *
     * @param o The object to compare with.
     * @return {@code true} if the other object is a CountryBL with the same ID, otherwise {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof CountryBL){
            if (this.ID == ((CountryBL) o).ID) {
                return true;
            }
        }
        return false;
    }
}