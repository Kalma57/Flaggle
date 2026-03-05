package com.example.flagdemo.DataAccessLayer;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
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
 *
 * This class loads all countries from the database once at initialization
 * and stores them in memory for fast access.
 *
 * It also maintains lookup maps for quick conversion between
 * country codes and country names.
 */
public class CountryRepository {

    // Map used to convert country code -> country name
    private Map<String, String> codeToName = new HashMap<>();

    // Map used to convert country name -> country code
    private Map<String, String> nameToCode = new HashMap<>();

    // List containing all countries loaded from the database
    private List<CountryDAL> allCountries = new ArrayList<>();

    /**
     * Constructor that initializes the repository
     * and loads all countries from the database.
     */
    public CountryRepository() {
        loadCountries();
    }

    /**
     * Creates and returns a connection to the SQLite database.
     * The database file is located inside the resources folder.
     *
     * @return Connection object to the SQLite database
     * @throws Exception if the database file cannot be found or opened
     */
    private Connection getConnection() throws Exception {
        // נתיב יחסי לפרויקט שלך: מחפש את הקובץ DB/Flaggle.db בתיקיית הפרויקט
        String dbPath = Paths.get("DB/Flaggle.db").toAbsolutePath().toString();

        System.out.println("USING DATABASE AT:");
        System.out.println(dbPath);

        // בדיקה שהקובץ באמת קיים
        if (!Paths.get(dbPath).toFile().exists()) {
            throw new Exception("Database file not found at: " + dbPath);
        }

        // יצירת חיבור ל-SQLite
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    /**
     * Loads all countries from the database and stores them in memory.
     * This method also fills the lookup maps for quick access by
     * country code or country name.
     */
    private void loadCountries() {

        try (Connection conn = getConnection()) {

            // Create SQL statement to retrieve all countries
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT ID, Code, CountryName, FlagPath, Latitude, Longitude, neighborList FROM Countries");

            // Iterate through the result set and create CountryDAL objects
            while (rs.next()) {

                int ID = rs.getInt("ID");
                String name = rs.getString("CountryName");
                String code = rs.getString("Code");
                String flagPath = rs.getString("FlagPath");
                double Latitude = rs.getDouble("Latitude");
                double longitude = rs.getDouble("Longitude");
                String neighborList = rs.getString("neighborList");


                // Create DAL object representing the country
                CountryDAL country = new CountryDAL(ID, name, code, flagPath, Latitude, longitude, neighborList);

                // Store country in the in-memory list
                allCountries.add(country);

                // Populate lookup maps
                codeToName.put(code, name);
                nameToCode.put(name.toLowerCase(), code);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a country name using its country code.
     *
     * @param code the country code
     * @return the corresponding country name
     */
    public String getNameByCode(String code) {
        return codeToName.get(code);
    }

    /**
     * Retrieves a country code using its country name.
     * The lookup is case-insensitive.
     *
     * @param name the country name
     * @return the corresponding country code
     */
    public String getCodeByName(String name) {
        return nameToCode.get(name.toLowerCase());
    }

    /**
     * Returns the list of all countries loaded from the database.
     *
     * @return list of CountryDAL objects
     */
    public List<CountryDAL> getAllCountries() {
        return allCountries;
    }

    /**
     * Returns the total number of countries currently stored in memory.
     *
     * @return number of countries
     */
    public int getNumberOfCountries() {
        return allCountries.size();
    }
}