package Tests;

import java.util.*;
import java.sql.*;

import ServiceLayer.GameService;
import BusinessLayer.CountryBL;
import BusinessLayer.GuessResultBL;
import BusinessLayer.GameEngineBL;
import DataAccessLayer.CountryController;
import DataAccessLayer.CountryRepository;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServiceLayerTests {
    private GameService gameService;
    private CountryController cc;
    private GameEngineBL engine;
    private CountryBL france;
    private CountryBL germany;

    @Before
    public void setup() throws SQLException {
        System.out.println("DAL tests: ");
        CountryRepository cr = new CountryRepository();
        cc = new CountryController(cr);

        System.out.println("BusinessLayer tests: ");
        engine = new GameEngineBL();
        engine.StartNewGame();
        france = new CountryBL("France", 79, engine.getCountryController().getCountryByName("france").getFlagPath());
        engine.setTargetCountry(france);
        germany = new CountryBL("Germany", 58, engine.getCountryController().getCountryByName("Germany").getFlagPath());

        System.out.println("ServiceLayer tests: ");
        gameService = new GameService();
        gameService.StartNewGame();
        gameService.getEngine().setTargetCountry(france);

    }

    /*---------------------DAL tests-------------------------------------*/

    @Test
    public void testGetCountryById_Valid() throws SQLException {
        CountryBL country = cc.getCountryById(1);
        System.out.println("testGetCountryById_Valid - Expected: Andorra, Got: " + (country != null ? country.getName() : "null"));
        assertNotNull("Country should not be null", country);
        assertEquals("Andorra", country.getName());
    }

    @Test
    public void testGetCountryById_Invalid() throws SQLException {
        CountryBL country = cc.getCountryById(9999);
        System.out.println("testGetCountryById_Invalid - Expected: null, Got: " + (country != null ? country.getName() : "null"));
        assertNull("Country should be null for invalid ID", country);
    }

    @Test
    public void testGetCountryByName_Valid() throws SQLException {
        CountryBL country = cc.getCountryByName("Andorra");
        System.out.println("testGetCountryByName_Valid - Expected ID: 1, Got: " + (country != null ? country.getCode() : "null"));
        assertNotNull("Country should not be null", country);
        assertEquals(1, country.getCode());
    }

    @Test
    public void testGetCountryByName_Invalid() throws SQLException {
        CountryBL country = cc.getCountryByName("NonExistentCountry");
        System.out.println("testGetCountryByName_Invalid - Expected: null, Got: " + (country != null ? country.getName() : "null"));
        assertNull("Country should be null for invalid name", country);
    }

    @Test
    public void testGetAllCountriesDAL() {
        List<CountryBL> countries = cc.getAllCountries();
        System.out.println("testGetAllCountries - Number of countries: " + (countries != null ? countries.size() : "null"));
        assertNotNull("List should not be null", countries);
        assertTrue("List should contain at least 1 country", countries.size() > 0);
    }

    @Test
    public void testGetNumberOfAllCountries() {
        int num = cc.getNumberOfAllCountries();
        System.out.println("testGetNumberOfAllCountries - Number of countries: " + num);
        assertTrue("Number of countries should be greater than 0", num > 0);
    }

    /*--------------------------BussinessLayer tests-------------------------------*/

    @Test
    public void testCorrectGuess() throws SQLException {
        GuessResultBL result = engine.Guess("France");
        System.out.println("testCorrectGuess - Guess result: " + result);

        assertTrue("The guess should have been correct", result.isCorrect());
        assertEquals("The returned country does not match the expected target",
                france.getName(), result.getTargetCountry().getName());
    }

    @Test
    public void testWrongGuessFlagDifferenceNotNull() throws SQLException {
        GuessResultBL result = engine.Guess("Spain");
        System.out.println("testWrongGuessFlagDifferenceNotNull - Guess result: " + result);

        assertNotNull("The GuessResultBL object should not be null", result);
        assertFalse("The guess should have been incorrect", result.isCorrect());
        assertNotNull("The flagDifference field should not be null", result.getFlagDifferences());
    }

    @Test
    public void testGetAllCountriesBusiness() {
        System.out.println("testGetAllCountriesBusiness - All countries: " + engine.getCountryController().getAllCountries());

        assertEquals("There should be exactly two countries registered in the engine",
                258, engine.getCountryController().getNumberOfAllCountries());
        assertTrue("The list of countries should contain France",
                engine.getCountryController().getAllCountries().contains(france));
        assertTrue("The list of countries should contain Germany",
                engine.getCountryController().getAllCountries().contains(germany));
    }

        /*--------------------------ServiceLayer tests-------------------------------*/
    @Test
    public void testCorrectGuessService() throws SQLException {
        GuessResultBL result = gameService.Guess("France");
        System.out.println("testCorrectGuess - Guess result: " + result);

        assertTrue("The guess should have been correct", result.isCorrect());
        assertEquals("The returned country does not match the expected target",
                france.getName(), result.getTargetCountry().getName());
    }

    @Test
    public void testWrongGuessFlagDifferenceNotNullService() throws SQLException {
        GuessResultBL result = gameService.Guess("Spain");
        System.out.println("testWrongGuessFlagDifferenceNotNull - Guess result: " + result);

        assertNotNull("The GuessResultBL object should not be null", result);
        assertFalse("The guess should have been incorrect", result.isCorrect());
        assertNotNull("The flagDifference field should not be null", result.getFlagDifferences());
    }
}

