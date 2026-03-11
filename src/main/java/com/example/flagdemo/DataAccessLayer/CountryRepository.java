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

/**
 * Repository responsible for loading and storing country data from the database.
 */
public class CountryRepository {

    private Map<String, String> codeToName = new HashMap<>();
    private Map<String, String> nameToCode = new HashMap<>();
    private List<CountryDAL> allCountries = new ArrayList<>();

    public CountryRepository() {
        loadCountries();
    }

    /**
     * Creates a connection to the SQLite database.
     * Loads the DB from the classpath (inside the JAR) and copies it to a temp file.
     */
    private Connection getConnection() throws Exception {
        InputStream is = getClass().getResourceAsStream("/static/DB/Flaggle.db");
        if (is == null) {
            throw new Exception("Database not found in classpath at /static/DB/Flaggle.db");
        }
        File tempDb = File.createTempFile("Flaggle", ".db");
        tempDb.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempDb)) {
            is.transferTo(fos);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + tempDb.getAbsolutePath());
    }

    private void loadCountries() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT ID, Code, CountryName, FlagPath, Latitude, Longitude, neighborList, ISO3 FROM Countries");

            while (rs.next()) {
                int ID = rs.getInt("ID");
                String name = rs.getString("CountryName");
                String code = rs.getString("Code");
                String flagPath = rs.getString("FlagPath");
                double latitude = rs.getDouble("Latitude");
                double longitude = rs.getDouble("Longitude");
                String neighborList = rs.getString("neighborList");
                String iso3 = rs.getString("ISO3");

                CountryDAL country = new CountryDAL(ID, name, code, flagPath, latitude, longitude, neighborList, iso3);
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