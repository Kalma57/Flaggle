package com.example.flagdemo.BusinessLayer;

import com.example.flagdemo.DataAccessLayer.CountryDAL;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

public class CountryBL implements java.io.Serializable {
    private String name;
    private int ID;
    private String flagPath;
    private double latitude;
    private double longitude;
    private String neighbors;
    private String iso3;
    private String capital;

    public CountryBL(String name, int ID, String flagPath, double lat, double longi, String neighbors, String iso3, String capital) {
        this.name = name;
        this.ID = ID;
        this.flagPath = flagPath;
        this.latitude = lat;
        this.longitude = longi;
        this.neighbors = neighbors;
        this.iso3 = iso3;
        this.capital = capital;
    }

    public CountryBL() {}

    // Constructor to initialize from Data Access Layer object
    public CountryBL(CountryDAL cd) {
        this.name = cd.getCountryName();
        this.ID = cd.getID();
        this.flagPath = cd.getFlagPath();
        this.latitude = cd.getLatitude();
        this.longitude = cd.getLongitude();
        this.neighbors = cd.getNeighborsList();
        this.iso3 = cd.getIso3();
        this.capital = cd.getCapital();
    }

    public String getName() {
        return name;
    }

    public int getCode() {
        return ID;
    }

    public String getFlagPath() {
        return flagPath;
    }

    /**
     * Loads the flag image from the classpath.
     * Uses getResourceAsStream to ensure compatibility inside packaged JARs.
     * * @return The flag image as a BufferedImage, or null if loading fails.
     */
    @JsonIgnore
    public BufferedImage getFlagImage() {
        // Extract the filename from the path
        String fileName = Paths.get(flagPath).getFileName().toString();

        // Define the path relative to the resources folder (Classpath)
        String resourcePath = "/static/DB/FlagsImages/" + fileName;

        // Load the image as a stream to support execution from inside a JAR
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Flag file not found in classpath: " + resourcePath);
                return null;
            }
            return ImageIO.read(is);
        } catch (IOException e) {
            System.err.println("Error reading flag image: " + resourcePath);
            e.printStackTrace();
            return null;
        }
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getNeighborsList() { return neighbors; }
    public String getIso3() { return iso3; }

    /** Null/blank for entities with no real capital (Antarctica, the EU, dependent territories, etc.). */
    public String getCapital() { return capital; }

    /** True if this entity has a real capital city on record - check before using it in the capitals game. */
    public boolean hasCapital() { return capital != null && !capital.isBlank(); }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CountryBL){
            return this.ID == ((CountryBL) o).ID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ID;
    }
}