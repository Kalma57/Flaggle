package com.example.flagdemo.BusinessLayer;

import com.example.flagdemo.DataAccessLayer.CountryDAL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class CountryBL {
    private String name;
    private int ID;
    private String flagPath;

    public CountryBL(String name, int ID, String flagPath) {
        this.name = name;
        this.ID = ID;
        this.flagPath = flagPath;
    }

    public CountryBL() {}

    public CountryBL(CountryDAL cd) {
        this.name = cd.getCountryName();
        this.ID = cd.getID();
        this.flagPath = cd.getFlagPath();
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
     * Returns the country's code.
     *
     * @return The country's code.
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
    public BufferedImage getFlagImage() {
        File file = new File("DB/" + flagPath); // עכשיו "DB/FlagsImages/fr.png"

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
    /**
     * Compares this country to another object based on the country code.
     *
     * @param o The object to compare with.
     * @return {@code true} if the other object is a CountryBL with the same code, otherwise {@code false}.
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

    /**
     * Returns the hash code of this country, based on its code.
     *
     * @return The hash code value.
     */

    /*@Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }*/
}