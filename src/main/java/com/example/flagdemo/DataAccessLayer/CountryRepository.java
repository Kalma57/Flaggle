package com.example.flagdemo.DataAccessLayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.sqlite.SQLiteConfig;

/**
 * Repository responsible for loading and storing country data from the database.
 *
 * Spring-managed singleton: loaded once at application startup and shared by every
 * request, instead of being reconstructed (and re-copying the SQLite temp file) per game.
 */
@Repository
public class CountryRepository implements java.io.Serializable {

    private static final String DB_RESOURCE = "/data/MundoFlag.db";

    private Map<String, String> codeToName = new HashMap<>();
    private Map<String, String> nameToCode = new HashMap<>();
    private List<CountryDAL> allCountries = new ArrayList<>();

    public CountryRepository() {
        loadCountries();
    }

    /**
     * Creates a connection to the SQLite database.
     * Loads the DB from the classpath (inside the JAR) and copies it to a temp file.
     *
     * The DB lives under /data, not /static, so the web server never serves it to
     * browsers (it holds every country's answer data), and the connection is opened
     * read-only so nothing at runtime can change it.
     */
    private Connection getConnection() throws Exception {
        InputStream is = getClass().getResourceAsStream(DB_RESOURCE);
        if (is == null) {
            throw new Exception("Database not found in classpath at " + DB_RESOURCE);
        }
        File tempDb = File.createTempFile("Flags", ".db");
        tempDb.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempDb)) {
            is.transferTo(fos);
        }
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        return DriverManager.getConnection("jdbc:sqlite:" + tempDb.getAbsolutePath(), config.toProperties());
    }

    private void loadCountries() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT ID, Code, CountryName, FlagPath, Latitude, Longitude, neighborList, ISO3, Capital FROM Countries");

            while (rs.next()) {
                int ID = rs.getInt("ID");
                String name = rs.getString("CountryName");
                String code = rs.getString("Code");
                String flagPath = rs.getString("FlagPath");
                double latitude = rs.getDouble("Latitude");
                double longitude = rs.getDouble("Longitude");
                String neighborList = rs.getString("neighborList");
                String iso3 = rs.getString("ISO3");
                String capital = rs.getString("Capital");

                CountryDAL country = new CountryDAL(ID, name, code, flagPath, latitude, longitude, neighborList, iso3, capital);
                allCountries.add(country);
                codeToName.put(code, name);
                nameToCode.put(name.toLowerCase(), code);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getNameByCode(String code) {
        return codeToName.get(code);
    }

    public String getCodeByName(String name) {
        return nameToCode.get(name.toLowerCase());
    }

    public List<CountryDAL> getAllCountries() {
        return allCountries;
    }

    public int getNumberOfCountries() {
        return allCountries.size();
    }
}