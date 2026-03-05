package com.example.flagdemo.DataAccessLayer;

import com.example.flagdemo.BusinessLayer.CountryBL;

import java.net.URL;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;

/**
 * Controller responsible for accessing country data from the database
 * and converting it into Business Layer objects (CountryBL).
 *
 * This class acts as a bridge between the Data Access Layer (DAL)
 * and the Business Layer (BL).
 */
public class CountryController {

    // Repository used to fetch country data from DAL
    private CountryRepository cr;

    /**
     * Constructor that injects the CountryRepository dependency.
     *
     * @param cr repository used for database access operations
     */
    public CountryController(CountryRepository cr) {
        this.cr = cr;
    }

    /**
     * Creates and returns a connection to the SQLite database.
     * The database file is located in the resources directory.
     *
     * @return Connection object to the SQLite database
     * @throws Exception if the database file cannot be found or opened
     */
    private Connection getConnection() throws Exception {
        URL dbUrl = getClass().getResource("/DB/Flaggle.db");

        // Check if the database file exists in the resources folder
        if (dbUrl == null) {
            throw new Exception("Database file not found in resources");
        }

        // Convert the resource URL to a file system path
        String dbPath = Paths.get(dbUrl.toURI()).toString();

        // Create and return a connection to the SQLite database
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    /**
     * Retrieves a country from the database using its ID.
     *
     * @param id the unique identifier of the country
     * @return CountryBL object representing the country, or null if not found
     * @throws SQLException if a database error occurs
     */
    public CountryBL getCountryById(int id) throws SQLException {
        String query = "SELECT ID, CountryName, Code, FlagPath FROM Countries WHERE ID = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Set the ID parameter in the query
            pstmt.setInt(1, id);

            ResultSet rs = pstmt.executeQuery();

            // If a matching country is found, create and return a CountryBL object
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

        // Return null if no country was found
        return null;
    }

    /**
     * Retrieves a country from the database using its name.
     * The repository is first used to convert the country name into its country code.
     *
     * @param countryName the name of the country
     * @return CountryBL object representing the country, or null if not found
     * @throws SQLException if a database error occurs
     */
    public CountryBL getCountryByName(String countryName) throws SQLException {

        // Get the country code associated with the given country name
        String countryCode = cr.getCodeByName(countryName);

        String query = "SELECT ID, CountryName, Code, FlagPath FROM Countries WHERE Code = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Set the country code parameter
            pstmt.setString(1, countryCode);

            ResultSet rs = pstmt.executeQuery();

            // If the country exists, return a corresponding CountryBL object
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

        // Return null if the country was not found
        return null;
    }

    /**
     * Retrieves all countries from the repository and converts them
     * into Business Layer objects.
     *
     * @return list of CountryBL objects
     */
    public List<CountryBL> getAllCountries() {

        // Retrieve all countries as DAL objects
        List<CountryDAL> listDAL = cr.getAllCountries();

        // Convert DAL objects into BL objects using stream mapping
        return listDAL.stream().map(CountryBL::new).toList();
    }

    /**
     * Returns the total number of countries stored in the database.
     *
     * @return number of countries
     */
    public int getNumberOfAllCountries() {

        // Retrieve all countries from repository
        List<CountryDAL> listDAL = cr.getAllCountries();

        // Return the size of the list
        return listDAL.size();
    }

    /**
     * Checks whether a country with the given name exists in the database.
     *
     * @param countryName the name of the country to check
     * @return true if the country exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    private boolean CheckCountryNameExists(String countryName) throws SQLException {

        String query = "SELECT 1 FROM Countries WHERE CountryName = ? LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Set the country name parameter
            pstmt.setString(1, countryName);

            ResultSet rs = pstmt.executeQuery();

            // If at least one row exists, the country name exists
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}