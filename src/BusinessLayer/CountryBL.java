package BusinessLayer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public class CountryBL {
    private String name;
    private String code;
    private String flagUrl;
    private BufferedImage flagImage;

    public CountryBL(String name, String code, String flagUrl) {
        this.name = name;
        this.code = code;
        this.flagUrl = flagUrl;
    }
    public CountryBL() {}
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
    public String getCode() {
        return code;
    }

    /**
     * Returns the URL of the country's flag image.
     *
     * @return The flag image URL.
     */
    public String getFlagUrl() {
        return flagUrl;
    }

    /**
     * Returns the BufferedImage of the country's flag.
     * If the flag image has not been loaded yet, it will be fetched from the URL.
     *
     * @return The flag image as a BufferedImage, or {@code null} if loading fails.
     */
    public BufferedImage getFlagImage() {
        if (flagImage == null) {
            try {
                URL url = new URL(flagUrl);
                flagImage = ImageIO.read(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return flagImage;
    }

    /**
     * Compares this country to another object based on the country code.
     *
     * @param o The object to compare with.
     * @return {@code true} if the other object is a CountryBL with the same code, otherwise {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CountryBL))
            return false;
        CountryBL other = (CountryBL) o;
        return code != null && code.equals(other.code);
    }

    /**
     * Returns the hash code of this country, based on its code.
     *
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
}