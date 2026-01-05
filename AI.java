import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Logger {
    static String path = "/data/data/com.termux/files/home/context.txt";;

    public static void log(String prompt, String response) {
        try (FileWriter writer = new FileWriter(path, true)) {
            writer.write("User: " + prompt + "\n");
            writer.write("AI: " + response + "\n\n");
        } catch (IOException e) {
            System.out.println("Could not write to log file: " + e.getMessage());
        }
    }
}

public class AI {
    public static void main(String[] args) throws Exception {
        boolean running = true;
        Scanner scanner = new Scanner(System.in);

        while (running) {
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null) {
                System.out.println("API key not found! Make sure you set GEMINI_API_KEY");
                return;
            }

            System.out.print("\nEnter prompt: ");
            String prompt = scanner.nextLine();

            if (prompt.equalsIgnoreCase("exit")) {
                System.out.println("You closed the AI");
                break;
            }

            prompt = prompt.replace("\n", " ").replace("\"", "\\\"");

            // Load history from file
            String history = "";
            try {
                history = Files.readString(Paths.get(Logger.path));
            } catch (IOException e) {
                // ignore if file doesn't exist yet
            }

            // Build full conversation prompt
            String fullPrompt = history + "\nUser: " + prompt;

            // Build JSON for the API
            String json = "{"
                    + "\"contents\": ["
                    + "  {"
                    + "    \"parts\": ["
                    + "      {\"text\": \"" + fullPrompt + "\"}"
                    + "    ]"
                    + "  }"
                    + "]"
                    + "}";

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429 || response.body().contains("quota") || response.body().contains("limit")) {
                System.out.println("\nJavAI RESPONSE: You have reached the usage limit. Try again after reset.");
            } else {
                String aiText = extractText(response.body());
                System.out.println("\nJavAI RESPONSE: " + aiText);
                Logger.log(prompt, aiText);
            }
        }
    }

    private static String extractText(String json) {
        int start = json.indexOf("\"text\": \"") + 9;
        if (start < 9) return "No text found in response.";
        int end = json.indexOf("\"", start);
        if (end < 0) return "No text found in response.";
        return json.substring(start, end);
    }
}