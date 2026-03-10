package com.example.flagdemo.DataAccessLayer;

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
 */
public class CountryRepository {

    private Map<String, String> codeToName = new HashMap<>();
    private Map<String, String> nameToCode = new HashMap<>();
    private List<CountryDAL> allCountries = new ArrayList<>();

    public CountryRepository() {
        loadCountries();
    }

    private Connection getConnection() throws Exception {
        String dbPath = Paths.get("src/main/resources/static/DB/Flaggle.db").toAbsolutePath().toString();

        if (!Paths.get(dbPath).toFile().exists()) {
            throw new Exception("Database file not found at: " + dbPath);
        }

        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
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
                double Latitude = rs.getDouble("Latitude");
                double longitude = rs.getDouble("Longitude");
                String neighborList = rs.getString("neighborList");
                String ISO3 = rs.getString("ISO3");

                CountryDAL country = new CountryDAL(ID, name, code, flagPath, Latitude, longitude, neighborList, ISO3);
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