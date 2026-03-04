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

public class CountryRepository {

    private Map<String, String> codeToName = new HashMap<>();
    private Map<String, String> nameToCode = new HashMap<>();
    private List<CountryDAL> allCountries = new ArrayList<>();

    public CountryRepository() {
        loadCountries();
    }

    // שיטה לקבלת חיבור ל־DB מתוך resources
    private Connection getConnection() throws Exception {
        URL dbUrl = getClass().getResource("/DB/Flaggle.db");
        if (dbUrl == null) {
            throw new Exception("Database file not found in resources");
        }
        String dbPath = Paths.get(dbUrl.toURI()).toString();
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    private void loadCountries() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT ID, Code, CountryName, FlagPath FROM Countries");
            while (rs.next()) {
                int ID = rs.getInt("ID");
                String name = rs.getString("CountryName");
                String code = rs.getString("Code");
                String flagPath = rs.getString("FlagPath");

                CountryDAL country = new CountryDAL(ID, name, code, flagPath);
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
