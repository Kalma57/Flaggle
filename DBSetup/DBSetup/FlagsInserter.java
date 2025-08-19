package DBSetup;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Locale;

public class FlagsInserter {

    public static void main(String[] args) {
        String dbPath = "DB/Flaggle.db";
        String flagsFolder = "DB/FlagsImages";

        File folder = new File(flagsFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("תיקיית התמונות לא נמצאה או אינה תיקייה: " + flagsFolder);
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            if (conn == null) {
                System.out.println("לא הצלחנו להתחבר ל-DB!");
                return;
            }

            Statement stmt = conn.createStatement();

            // יצירת הטבלה אם לא קיימת, עכשיו ללא AUTOINCREMENT
            String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS Countries (
                        ID INTEGER PRIMARY KEY,
                        Code TEXT,
                        CountryName TEXT,
                        FlagPath TEXT
                    );
                    """;
            stmt.execute(createTableSQL);

            // מחיקת רשומות קיימות
            stmt.execute("DELETE FROM Countries;");

            File[] files = folder.listFiles((dir, name) -> name.endsWith(".svg"));
            if (files == null || files.length == 0) {
                System.out.println("לא נמצאו קבצי SVG בתיקייה!");
                return;
            }

            String insertSQL = "INSERT INTO Countries (ID, Code, CountryName, FlagPath) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);

            int rowId = 1; // מספר שורה מתחיל מ-1
            int insertedCount = 0;

            for (File file : files) {
                String filename = file.getName();           // לדוגמה: il.svg
                String code = filename.split("\\.")[0].toUpperCase();
                String countryName;

                // בדיקה ידנית למדינות UK החלקיות
                switch (code.toLowerCase()) {
                    case "eng":
                        code = "ENG";
                        countryName = "England";
                        break;
                    case "nir":
                        code = "NIR";
                        countryName = "Northern Ireland";
                        break;
                    case "sct":
                        code = "SCT";
                        countryName = "Scotland";
                        break;
                    case "wls":
                        code = "WLS";
                        countryName = "Wales";
                        break;
                    default:
                        // רק קודים דו-אותיים חוקיים
                        if (!code.matches("^[A-Z]{2}$")) {
                            System.out.println("דלגתי על קובץ עם קוד לא חוקי: " + filename);
                            continue;
                        }
                        Locale locale = new Locale("", code);
                        countryName = locale.getDisplayCountry();
                        if (countryName.isEmpty()) {
                            System.out.println("לא נמצא שם למדינה עבור קוד: " + code);
                            continue; // דלג אם אין שם
                        }
                }

                String path = "FlagsImages/" + filename;

                pstmt.setInt(1, rowId++);  // נותן ID לפי מספר השורה
                pstmt.setString(2, code.toLowerCase());
                pstmt.setString(3, countryName);
                pstmt.setString(4, path);
                pstmt.executeUpdate();
                insertedCount++;
            }

            System.out.println("כל הקבצים החוקיים הוכנסו ל-DB בהצלחה! סך הכל: " + insertedCount);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
