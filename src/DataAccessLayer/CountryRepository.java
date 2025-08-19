package DataAccessLayer;

import DataAccessLayer.CountryDAL;

import java.sql.*;
import java.util.*;

public class CountryRepository {
    private static final String DB_PATH = "DB/Flaggle.db";

    private Map<String, String> codeToName = new HashMap<>();
    private Map<String, String> nameToCode = new HashMap<>();
    private List<CountryDAL> allCountries = new ArrayList<>();

    public CountryRepository() {
        loadCountries();
    }

    private void loadCountries() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT Code, CountryName, FlagPath FROM Countries");

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

            System.out.println("Loaded " + allCountries.size() + " countries into memory.");

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
