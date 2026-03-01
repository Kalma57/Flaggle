package DBSetup;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

public class FlagsInserter {

    public static void main(String[] args) {
        String dbPath = "DB/Flaggle.db";
        String flagsFolder = "DB/FlagsImages";

        File folder = new File(flagsFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("תיקיית התמונות לא נמצאה או אינה תיקייה: " + flagsFolder);
            return;
        }

        List<String> failedFiles = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            Statement stmt = conn.createStatement();
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Countries (
                    ID INTEGER PRIMARY KEY,
                    Code TEXT,
                    CountryName TEXT,
                    FlagPath TEXT
                );
            """);
            stmt.execute("DELETE FROM Countries;");

            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files == null || files.length == 0) {
                System.out.println("לא נמצאו קבצי PNG בתיקייה!");
                return;
            }

            String insertSQL = "INSERT INTO Countries (ID, Code, CountryName, FlagPath) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);

            int rowId = 1;
            int insertedCount = 0;

            int targetWidth = 320;
            int targetHeight = 160;

            for (File file : files) {
                String filename = file.getName();
                String code = filename.replace(".png", "").toUpperCase();
                String countryName;

                switch (code.toLowerCase()) {
                    case "eng": code = "ENG"; countryName = "England"; break;
                    case "nir": code = "NIR"; countryName = "Northern Ireland"; break;
                    case "sct": code = "SCT"; countryName = "Scotland"; break;
                    case "wls": code = "WLS"; countryName = "Wales"; break;
                    default:
                        if (!code.matches("^[A-Z]{2,3}$")) continue;
                        Locale locale = new Locale("", code);
                        countryName = locale.getDisplayCountry();
                        if (countryName.isEmpty()) countryName = code;
                }

                try {
                    BufferedImage original = ImageIO.read(file);
                    BufferedImage resized = stretchToSize(original, targetWidth, targetHeight);

                    // אפשר לשמור את הקובץ מחדש (לא חובה אם רוצים רק DB)
                    ImageIO.write(resized, "PNG", file);

                    String path = "FlagsImages/" + filename;
                    pstmt.setInt(1, rowId++);
                    pstmt.setString(2, code.toLowerCase());
                    pstmt.setString(3, countryName);
                    pstmt.setString(4, path);
                    pstmt.executeUpdate();
                    insertedCount++;

                } catch (Exception e) {
                    failedFiles.add(filename);
                    e.printStackTrace();
                }
            }

            System.out.println("הכנסת דגלים ל-DB סיימה. סך הכל: " + insertedCount);
            if (!failedFiles.isEmpty()) {
                System.out.println("קבצים שלא הוכנסו:");
                failedFiles.forEach(f -> System.out.println(" - " + f));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage stretchToSize(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }
}
