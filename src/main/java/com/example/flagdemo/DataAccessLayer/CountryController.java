package com.example.flagdemo.DataAccessLayer;

import com.example.flagdemo.BusinessLayer.CountryBL;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Controller responsible for accessing country data from the database
 * and converting it into Business Layer objects (CountryBL).
 *
 * Spring-managed singleton, shared by every request/game instead of being
 * recreated (with its own temp SQLite file) each time.
 */
@Service
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
     * Every country, built once from the repository's already-loaded rows and
     * then reused. Looking a country up used to copy the whole SQLite file to a
     * fresh temp file and open a new connection on every single call (several
     * per guess), which dominated the cost of a guess on a slow host.
     */
    private transient volatile List<CountryBL> allCountriesCache;
    private transient volatile Map<String, CountryBL> byNameCache;
    private transient volatile Map<Integer, CountryBL> byIdCache;

    private void ensureCaches() {
        if (allCountriesCache != null) return;
        synchronized (this) {
            if (allCountriesCache != null) return;
            Map<String, CountryBL> byName = new HashMap<>();
            Map<Integer, CountryBL> byId = new HashMap<>();
            for (CountryDAL d : cr.getAllCountries()) {
                CountryBL c = new CountryBL(
                        d.getCountryName(), d.getID(), fixImagePath(d.getFlagPath()),
                        d.getLatitude(), d.getLongitude(), d.getNeighborsList(), d.getIso3(), d.getCapital());
                byName.put(d.getCountryName().toLowerCase(), c);
                byId.putIfAbsent(d.getID(), c);
            }
            byNameCache = byName;
            byIdCache = byId;
            allCountriesCache = cr.getAllCountries().stream().map(CountryBL::new).toList();
        }
    }

    /**
     * Retrieves a country using its ID.
     */
    public CountryBL getCountryById(int id) {
        ensureCaches();
        return byIdCache.get(id);
    }

    /**
     * Retrieves a country using its name (case-insensitive).
     */
    public CountryBL getCountryByName(String countryName) {
        if (countryName == null) return null;
        ensureCaches();
        return byNameCache.get(countryName.toLowerCase());
    }

    public List<CountryBL> getAllCountries() {
        ensureCaches();
        return allCountriesCache;
    }

    public int getNumberOfAllCountries() {
        return cr.getAllCountries().size();
    }
}
