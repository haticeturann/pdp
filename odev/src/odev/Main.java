package odev;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.HttpURLConnection;
import java.net.HttpURLConnection;


public class Main {
    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("GitHub Repository URL giriniz:");
        String repoUrl = null;
        try {
            repoUrl = reader.readLine().trim();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // GitHub repository'sinden dosya listesini al
        List<String> fileUrls = getFileUrlsFromGitHub(repoUrl);
        if (fileUrls == null) {
            System.out.println("Hata: GitHub repository'sine erişilemiyor veya dosya listesi alınamıyor.");
            return;
        }

        // Dosyaları oku ve sınıfları analiz et
        for (String fileUrl : fileUrls) {
            String fileContent = getFileContentFromUrl(fileUrl);
            if (fileContent != null) {
                analyzeClass(fileContent);
            }
        }
    }

    private static List<String> getFileUrlsFromGitHub(String repoUrl) {
        List<String> fileUrls = new ArrayList<>();
        try {
            URL url = new URL(repoUrl.replace("/blob/main", ""));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Dosya URL'lerini çıkarmak için regex desenini belirleyelim
                Pattern pattern = Pattern.compile("<a\\s*href=\"([^\"]+\\.java)\"[^>]*>");
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    String fileUrl = matcher.group(1);
                    fileUrls.add(fileUrl);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return fileUrls;
    }


    private static String getFileContentFromUrl(String fileUrl) {
        StringBuilder content = new StringBuilder();
        try {
            URL url = new URL("https://raw.githubusercontent.com" + fileUrl.replace("/blob/", "/"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return content.toString();
    }

    private static void analyzeClass(String content) {
        // Sınıf adını bul
        Pattern classPattern = Pattern.compile("class\\s+(\\w+)\\s*\\{");
        Matcher classMatcher = classPattern.matcher(content);
        if (classMatcher.find()) {
            String className = classMatcher.group(1);

            // Yorum ve kod satırlarını ayır
            Pattern pattern = Pattern.compile("(//.*)|(/\\*.*?\\*/)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);
            int javadocLines = 0;
            int otherCommentLines = 0;
            int codeLines = 0;
            int totalLines = 0;
            int functionCount = 0;
            while (matcher.find()) {
                String comment = matcher.group();
                if (comment.startsWith("//")) {
                    otherCommentLines++;
                } else {
                    otherCommentLines += comment.split("\r\n|\r|\n").length;
                }
                content = content.replace(comment, ""); // Yorum satırlarını temizle
            }

            // Javadoc satırlarını say
            pattern = Pattern.compile("/\\*\\*(.*?)\\*/", Pattern.DOTALL);
            matcher = pattern.matcher(content);
            while (matcher.find()) {
                String javadoc = matcher.group();
                javadocLines += javadoc.split("\r\n|\r|\n").length;
                content = content.replace(javadoc, ""); // Javadoc satırlarını temizle
            }

            // Boşlukları ve satır sayısını hesapla
            String[] lines = content.split("\r\n|\r|\n");
            for (String line : lines) {
                line = line.trim();
                totalLines++;
                if (!line.isEmpty()) {
                    codeLines++;
                }
            }

            // Fonksiyon sayısını hesapla
            pattern = Pattern.compile("(?:(?:public|private|protected|static|\\s) +)[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *\\{");
            matcher = pattern.matcher(content);
            while (matcher.find()) {
                functionCount++;
            }

         // Yorum yapma yüzdesi hesaplaması
            double YG = ((double) (javadocLines + otherCommentLines) * 0.8) / functionCount;
            double YH = ((double) codeLines / functionCount) * 0.3;
            double commentDeviation = ((100 * YG) / YH) - 100;
            
            // Sonuçları yazdır
            System.out.println("Class Name: " + className);
            System.out.println("Javadoc Satır Sayısı: " + javadocLines);
            System.out.println("Diğer Yorum Satır Sayısı: " + otherCommentLines);
            System.out.println("Kod Satır Sayısı: " + codeLines);
            System.out.println("LOC (Toplam Satır Sayısı): " + totalLines);
            System.out.println("Fonksiyon Sayısı: " + functionCount);
            System.out.println("Yorum Sapma Yüzdesi: " + commentDeviation + "%");
            System.out.println();
        }
    }
}
