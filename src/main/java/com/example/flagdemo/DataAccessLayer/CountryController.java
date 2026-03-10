package com.example.flagdemo.DataAccessLayer;

import com.example.flagdemo.BusinessLayer.CountryBL;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;

/**
 * Controller responsible for accessing country data from the database
 * and converting it into Business Layer objects (CountryBL).
 */
public class CountryController {

    // Repository used to fetch country data from DAL
    private CountryRepository cr;

    /**
     * Constructor that injects the CountryRepository dependency.
     */
    public CountryController(CountryRepository cr) {
        this.cr = cr;
    }

    /**
     * Creates and returns a connection to the SQLite database.
     * FIXED: Using the direct absolute path to avoid "Resource not found" exceptions.
     */
    private Connection getConnection() throws Exception {
        // Using direct absolute path to the DB file
        String dbPath = Paths.get("src/main/resources/static/DB/Flaggle.db").toAbsolutePath().toString();
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    /**
     * Helper method to fix the outdated image path coming from the database.
     * It extracts just the file name (e.g., "ss.png") and appends it to the new correct directory.
     */
    private String fixImagePath(String dbPath) {
        if (dbPath == null || dbPath.isEmpty()) {
            return dbPath;
        }
        // Extract just the file name from the old database path
        String fileName = Paths.get(dbPath).getFileName().toString();

        // Build and return the absolute path to the new directory
        return Paths.get("src/main/resources/static/DB/FlagsImages", fileName).toAbsolutePath().toString();
    }

    /**
     * Retrieves a country from the database using its ID.
     */
    public CountryBL getCountryById(int id) {
        String query = "SELECT ID, CountryName, Code, FlagPath, Latitude, Longitude, neighborList, ISO3 FROM Countries WHERE ID = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Fix the image path before passing it to CountryBL
                    String correctImagePath = fixImagePath(rs.getString("FlagPath"));

                    return new CountryBL(
                            rs.getString("CountryName"),
                            rs.getInt("ID"),
                            correctImagePath,
                            rs.getDouble("Latitude"),
                            rs.getDouble("Longitude"),
                            rs.getString("neighborList"),
                            rs.getString("ISO3")
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a country from the database using its name.
     */
    public CountryBL getCountryByName(String countryName) {
        String countryCode = cr.getCodeByName(countryName);

        String query = "SELECT ID, CountryName, Code, FlagPath, Latitude, Longitude, neighborList, ISO3 FROM Countries WHERE Code = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, countryCode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Fix the image path before passing it to CountryBL
                String correctImagePath = fixImagePath(rs.getString("FlagPath"));

                return new CountryBL(
                        rs.getString("CountryName"),
                        rs.getInt("ID"),
                        correctImagePath,
                        rs.getDouble("Latitude"),
                        rs.getDouble("Longitude"),
                        rs.getString("neighborList"),
                        rs.getString("ISO3")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<CountryBL> getAllCountries() {
        List<CountryDAL> listDAL = cr.getAllCountries();
        return listDAL.stream().map(CountryBL::new).toList();
    }

    public int getNumberOfAllCountries() {
        List<CountryDAL> listDAL = cr.getAllCountries();
        return listDAL.size();
    }

    private boolean CheckCountryNameExists(String countryName) {
        String query = "SELECT 1 FROM Countries WHERE CountryName = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, countryName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}