package com.example.flagdemo.DataAccessLayer;

import com.example.flagdemo.BusinessLayer.CountryBL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;

/**
 * Controller responsible for accessing country data from the database
 * and converting it into Business Layer objects (CountryBL).
 */
public class CountryController implements java.io.Serializable {

    // Repository used to fetch country data from DAL
    private CountryRepository cr;

    /**
     * Constructor that injects the CountryRepository dependency.
     */
    public CountryController(CountryRepository cr) {
        this.cr = cr;
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

    /**
     * Fixes the image path to work from the classpath (inside the JAR).
     * Extracts just the file name and returns a classpath-relative path.
     */
    private String fixImagePath(String dbPath) {
        if (dbPath == null || dbPath.isEmpty()) {
            return dbPath;
        }
        String fileName = Paths.get(dbPath).getFileName().toString();
        return "/static/DB/FlagsImages/" + fileName;
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