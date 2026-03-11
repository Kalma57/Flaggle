package DBSetup;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class FlagsInserter {

    private static final Map<String, String> iso2ToIso3 = new HashMap<>();
    private static final Map<String, String> specialIso3 = new HashMap<>();
    private static final Map<String, String> specialNames = new HashMap<>();
    private static final Map<String, CountryInfo> countryExtraInfo = new HashMap<>();

    private static class CountryInfo {
        Double latitude;  // Can be NULL
        Double longitude; // Can be NULL
        CountryInfo(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    static {
        // Special ISO cases
        iso2ToIso3.put("UK", "GBR");

        specialIso3.put("XK", "XKX"); specialNames.put("XK", "Kosovo");
        specialIso3.put("US", "USA"); specialNames.put("US", "United States");
        specialNames.put("TR", "Turkey");

        // Fix 1: Manually adding Scotland and Wales so they don't appear as "Unknown"
        specialNames.put("SCT", "Scotland");
        specialNames.put("WLS", "Wales");

        // Load ISO2 to ISO3 from Java Locales
        for (Locale locale : Locale.getAvailableLocales()) {
            if (!locale.getCountry().isEmpty()) {
                try {
                    String iso2 = locale.getCountry().toUpperCase();
                    String iso3 = locale.getISO3Country().toUpperCase();
                    iso2ToIso3.putIfAbsent(iso2, iso3);
                } catch (Exception ignored) {}
            }
        }
    }

    public static void main(String[] args) {
        // Absolute relative paths - based on the project root folder
        String baseDir = "src/main/resources/static/DB";
        String dbPath = baseDir + "/Flaggle.db";
        String flagsFolder = baseDir + "/FlagsImages";
        String longLatFilePath = baseDir + "/countriesBorders/CountriesLongLat.txt";
        String neighborsFilePath = baseDir + "/countriesBorders/CountriesNeighbors.txt";

        loadCountryLongLat(longLatFilePath);

        // Fix 2: Injecting hardcoded coordinates for Portugal and South Sudan
        // We do this after reading the file to overwrite missing data if any
        countryExtraInfo.put("PRT", new CountryInfo(39.3999, -8.2245));
        countryExtraInfo.put("SSD", new CountryInfo(6.8770, 31.3070));

        File folder = new File(flagsFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("❌ Error: Flags folder not found at relative path: " + folder.getAbsolutePath());
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {

            Statement stmt = conn.createStatement();
            stmt.execute("DROP TABLE IF EXISTS Countries;");

            String createTableSQL = """
                    CREATE TABLE Countries (
                        ID INTEGER PRIMARY KEY,
                        Code TEXT,
                        ISO3 TEXT,
                        CountryName TEXT,
                        FlagPath TEXT,
                        Latitude REAL,
                        Longitude REAL
                    );
                    """;
            stmt.execute(createTableSQL);

            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".svg"));
            if (files == null || files.length == 0) {
                System.out.println("No SVG files found in folder!");
                return;
            }

            String insertSQL = """
                    INSERT INTO Countries
                    (ID, Code, ISO3, CountryName, FlagPath, Latitude, Longitude)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            int rowId = 1;
            int missingGeoInfo = 0;

            for (File file : files) {
                String filename = file.getName();
                String codeFromFile = filename.split("\\.")[0].toUpperCase();

                String iso3 = normalizeToIso3(codeFromFile);
                String countryName = specialNames.getOrDefault(codeFromFile, "Unknown");
                String iso2 = codeFromFile.length() == 2 ? codeFromFile : null;

                if (countryName.equals("Unknown") && iso2 != null) {
                    countryName = new Locale("", iso2).getDisplayCountry();
                }

                CountryInfo info = countryExtraInfo.get(iso3);
                if (info == null) {
                    missingGeoInfo++;
                    System.out.println("Missing geo info for: " + iso3 + " -> using NULL for lat/lon");
                }

                String pngFilename = filename.replace(".svg", ".png");
                File pngFile = new File(flagsFolder, pngFilename);
                try (FileOutputStream fos = new FileOutputStream(pngFile)) {
                    PNGTranscoder transcoder = new PNGTranscoder();
                    TranscoderInput input = new TranscoderInput(file.toURI().toString());
                    TranscoderOutput output = new TranscoderOutput(fos);
                    transcoder.transcode(input, output);
                } catch (Exception ignored) {}

                // Path saved to DB
                String path = "DB/FlagsImages/" + pngFilename;

                pstmt.setInt(1, rowId++);
                pstmt.setString(2, iso2 != null ? iso2 : iso3);
                pstmt.setString(3, iso3);
                pstmt.setString(4, countryName);
                pstmt.setString(5, path);

                if (info != null && info.latitude != null && info.longitude != null) {
                    pstmt.setDouble(6, info.latitude);
                    pstmt.setDouble(7, info.longitude);
                } else {
                    pstmt.setNull(6, Types.REAL);
                    pstmt.setNull(7, Types.REAL);
                }

                pstmt.addBatch();
            }

            pstmt.executeBatch();
            System.out.println("Insertion complete! Total: " + (rowId - 1));
            System.out.println("Countries missing geo info: " + missingGeoInfo);

            try {
                stmt.execute("ALTER TABLE Countries ADD COLUMN neighborList TEXT;");
            } catch (SQLException ignored) {}

            Map<String, String> neighborsMap = new HashMap<>();
            File neighborsFile = new File(neighborsFilePath);
            if (!neighborsFile.exists()) {
                System.err.println("❌ Error: Neighbors file not found at: " + neighborsFile.getAbsolutePath());
            } else {
                try (BufferedReader br = new BufferedReader(new FileReader(neighborsFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        String[] parts = line.split(":");
                        if (parts.length != 2) continue;
                        String countryIso3 = parts[0].trim();
                        String neighborList = parts[1].trim();
                        neighborsMap.put(countryIso3, neighborList);
                    }
                } catch (IOException e) {
                    System.out.println("Failed to read neighbors file: " + e.getMessage());
                }
            }

            for (Map.Entry<String, String> entry : neighborsMap.entrySet()) {
                String iso3 = entry.getKey();
                String neighborStr = entry.getValue();

                String updateSQL = "UPDATE Countries SET neighborList = ? WHERE ISO3 = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    updateStmt.setString(1, neighborStr.isEmpty() ? null : neighborStr);
                    updateStmt.setString(2, iso3);
                    updateStmt.executeUpdate();
                } catch (SQLException ex) {
                    System.out.println("Failed to update neighbors for " + iso3 + ": " + ex.getMessage());
                }
            }

            System.out.println("Neighbors update complete!");

            String targetPath = "target/classes/static/DB/Flaggle.db";
            File originalDb = new File(dbPath);
            File targetDb = new File(targetPath);

            if (targetDb.getParentFile() != null) {
                targetDb.getParentFile().mkdirs();
            }

            try (InputStream is = new FileInputStream(originalDb);
                 OutputStream os = new FileOutputStream(targetDb)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
                System.out.println("Database successfully copied to target: " + targetPath);

            } catch (IOException e) {
                System.out.println("Failed to copy database to target: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String normalizeToIso3(String code) {
        if (code == null) return "UNK";
        code = code.trim().toUpperCase();
        if (specialIso3.containsKey(code)) return specialIso3.get(code);
        if (code.matches("^[A-Z]{3}$")) return code;
        if (code.matches("^[A-Z]{2}$") && iso2ToIso3.containsKey(code)) return iso2ToIso3.get(code);
        return code;
    }

    private static void loadCountryLongLat(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("❌ ERROR: LongLat file not found at relative path! Java is looking here: " + file.getAbsolutePath());
            return;
        }

        int loaded = 0;
        int skipped = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\t"); // Tab-separated
                if (parts.length < 4) {
                    skipped++;
                    continue;
                }

                try {
                    String iso2 = parts[0].trim().toUpperCase();
                    Double lat = null;
                    Double lon = null;

                    String latStr = parts[1].trim();
                    String lonStr = parts[2].trim();
                    if (!latStr.isEmpty()) lat = Double.parseDouble(latStr);
                    if (!lonStr.isEmpty()) lon = Double.parseDouble(lonStr);

                    String iso3 = normalizeToIso3(iso2);

                    countryExtraInfo.put(iso3, new CountryInfo(lat, lon));
                    loaded++;
                } catch (Exception ex) {
                    System.out.println("Bad line skipped: " + line);
                }
            }
            System.out.println("✅ Loaded long/lat info for " + loaded + " countries.");
            if (skipped > 0) {
                System.out.println("⚠️ Warning: Skipped " + skipped + " lines (Are you sure they are separated by Tabs and not Spaces?)");
            }
        } catch (Exception e) {
            System.out.println("Failed to read long/lat file: " + e.getMessage());
        }
    }
}