package DataAccessLayer;

import DataAccessLayer.CountryDAL;
import DataAccessLayer.CountryRepository;
import BusinessLayer.CountryBL;

import java.sql.*;
import java.util.*;

public class CountryController {
    private static final String DB_PATH = "DB/Flaggle.db";

    private CountryRepository cr;


    public CountryController(CountryRepository cr){
        this.cr = cr;
    }

    public CountryBL getCountryById(int id) throws SQLException {
        String query = "SELECT ID, CountryName, Code, FlagPath FROM Countries WHERE ID = ?";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
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

        }

        return null; // if not found
    }

    public CountryBL getCountryByName(String countryName) throws SQLException {
        String countryCode = cr.getCodeByName(countryName);

        String query = "SELECT ID, CountryName, Code, FlagPath FROM Countries WHERE Code = ?";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
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

        }

        return null; // if not found
    }

    public List<CountryBL> getAllCountries(){
        List<CountryDAL> listDAL= cr.getAllCountries();
        List<CountryBL> listBL = listDAL.stream().map(CountryBL::new).toList();
        return listBL;
    }

    public int getNumberOfAllCountries(){
        List<CountryDAL> listDAL= cr.getAllCountries();
        return listDAL.size();
    }

    private boolean CheckCountryNameExists(String countryName) throws SQLException {
        String query = "SELECT 1 FROM Countries WHERE CountryName = ? LIMIT 1";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, countryName);
            ResultSet rs = pstmt.executeQuery();

            return rs.next();
        }
    }
}
