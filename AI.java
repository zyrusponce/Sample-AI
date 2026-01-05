import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class AI {

    public static void main(String[] args) throws Exception {
        boolean running = true;
        while (running) {
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null) {
                System.out.println("API key not found! Make sure you set GEMINI_API_KEY");
                return;
            }

            // read prompts
            Scanner scanner = new Scanner(System.in);
            System.out.print("\nEnter prompt: ");
            String prompt = scanner.nextLine();

            if (prompt.toLowerCase().equals("exit")) {
                System.out.println("You closed the AI");
                break;
            }
            prompt = prompt.replace("\n", " ").replace("\"", "\\\"");

            // Build JSON for the API
            String json = "{"
                    + "\"contents\": ["
                    + "  {"
                    + "    \"parts\": ["
                    + "      {\"text\": \"" + prompt + "\"}"
                    + "    ]"
                    + "  }"
                    + "]"
                    + "}";

            // passing the prompt and retrieving it
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            // check for quota/limit error
            if (response.statusCode() == 429 || response.body().contains("quota") || response.body().contains("limit")) {
                System.out.println("\nJavAI RESPONSE: You have reached the usage limit. Try again after reset.");
            } else {
                String aiText = extractText(response.body());
                System.out.print("\nJavAI RESPONSE: ");
                System.out.println(aiText);
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