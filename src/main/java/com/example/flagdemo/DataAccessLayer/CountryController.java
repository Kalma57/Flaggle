package com.example.flagdemo.DataAccessLayer;

import com.example.flagdemo.BusinessLayer.CountryBL;

import java.net.URL;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;

public class CountryController {

    private CountryRepository cr;

    public CountryController(CountryRepository cr) {
        this.cr = cr;
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

    public CountryBL getCountryById(int id) throws SQLException {
        String query = "SELECT ID, CountryName, Code, FlagPath FROM Countries WHERE ID = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new CountryBL(
                        rs.getString("CountryName"),
                        rs.getInt("ID"),
                        rs.getString("FlagPath")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // if not found
    }

    public CountryBL getCountryByName(String countryName) throws SQLException {
        String countryCode = cr.getCodeByName(countryName);

        String query = "SELECT ID, CountryName, Code, FlagPath FROM Countries WHERE Code = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, countryCode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new CountryBL(
                        rs.getString("CountryName"),
                        rs.getInt("ID"),
                        rs.getString("FlagPath")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // if not found
    }

    public List<CountryBL> getAllCountries() {
        List<CountryDAL> listDAL = cr.getAllCountries();
        return listDAL.stream().map(CountryBL::new).toList();
    }

    public int getNumberOfAllCountries() {
        List<CountryDAL> listDAL = cr.getAllCountries();
        return listDAL.size();
    }

    private boolean CheckCountryNameExists(String countryName) throws SQLException {
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
