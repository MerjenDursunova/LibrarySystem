import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AIClient {











    private static final String API_URL = System.getenv().getOrDefault("AI_API_URL",
            System.getenv().getOrDefault("OPENROUTER_API_URL", "https://openrouter.ai/api/v1/chat/completions"));
    private static final String API_KEY = System.getenv().getOrDefault("AI_API_KEY", System.getenv().getOrDefault(
            "YOUR_API_KEY", "YOUR_API_KEY"));
    private static final String API_MODEL = System.getenv().getOrDefault("AI_MODEL",
            System.getenv().getOrDefault("OPENROUTER_MODEL", "google/gemma-3-27b-it:free"));


    private static final String SYSTEM_PROMPT = "You are the Library AI Assistant. Follow these rules when replying: " +
            "(1) Be very brief and answer in plain sentences. " +
            "(2) Do NOT use Markdown, headings, bullets, or formatting characters (no **, no ##, no backticks, no lists). "
            +
            "(3) Dxo NOT emit literal escape sequences like \\n or \\t; use plain text without newlines where possible. "
            +
            "(4) Call out any important restrictions briefly (for example: cannot provide full copyrighted text, cannot reveal private data). "
            +
            "(5) Identify as the Library Assistant in one short phrase at the start if relevant.";







    public static String sendQuery(String query) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String endpoint = API_URL;

            String[] tryModels = new String[] { API_MODEL, "your_ai_model" };
            Exception lastException = null;
            String lastErrorBody = null;

            for (String model : tryModels) {
                try {

                    String payload = String.format(
                            "{\"model\":\"%s\",\"messages\":[{\"role\":\"system\",\"content\":\"%s\"},{\"role\":\"user\",\"content\":\"%s\"}]}",
                            escapeJson(model), escapeJson(SYSTEM_PROMPT), escapeJson(query));

                    HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .timeout(Duration.ofSeconds(30))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload));

                    if (!API_KEY.isEmpty()) {
                        reqBuilder.header("Authorization", "Bearer " + API_KEY);
                    }

                    HttpRequest request = reqBuilder.build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        lastErrorBody = response.body();

                        try {
                            java.nio.file.Path logsDir = java.nio.file.Paths.get("logs");
                            if (!java.nio.file.Files.exists(logsDir))
                                java.nio.file.Files.createDirectories(logsDir);
                            String ts = String.valueOf(java.time.Instant.now().toEpochMilli());
                            java.nio.file.Files.writeString(logsDir.resolve("ai_error_" + ts + ".json"), lastErrorBody);
                        } catch (Exception writeEx) {

                        }


                        String body = response.body();
                        try {
                            int m = body.indexOf("\"message\"");
                            if (m >= 0) {
                                int c = body.indexOf(':', m);
                                int q1 = body.indexOf('"', c + 1);
                                int q2 = body.indexOf('"', q1 + 1);
                                if (q1 >= 0 && q2 > q1) {
                                    String msg = body.substring(q1 + 1, q2);
                                    if (response.statusCode() == 404 && msg.toLowerCase()
                                            .contains("no endpoints found matching your data policy")) {

                                        continue;
                                    }
                                    return "[AI Error] HTTP " + response.statusCode() + ": " + msg;
                                }
                            }
                        } catch (Exception ex) {

                        }
                        return "[AI Error] HTTP " + response.statusCode() + ": " + body;
                    }

                    String body = response.body();


                    String content = extractChoiceContent(body);
                    if (content != null && !content.isEmpty())
                        return sanitizeResponse(content);


                    int idx = body.indexOf("\"content\"");
                    if (idx >= 0) {
                        int colon = body.indexOf(':', idx);
                        if (colon > 0) {
                            int start = body.indexOf('"', colon + 1);
                            int end = body.indexOf('"', start + 1);
                            if (start > 0 && end > start) {
                                return sanitizeResponse(body.substring(start + 1, end).replace("\\\"", "\""));
                            }
                        }
                    }

                    return sanitizeResponse(body);
                } catch (Exception e) {
                    lastException = e;

                    continue;
                }
            }


            if (lastErrorBody != null) {
                String lowered = lastErrorBody.toLowerCase();
                if (lowered.contains("no endpoints found matching your data policy")) {
                    StringBuilder g = new StringBuilder();
                    g.append(
                            "[AI Error] HTTP 404: OpenRouter indicates this model/endpoint isn't available to your API key due to privacy/publishing settings.\n");
                    g.append(
                            "Visit https://openrouter.ai/settings/privacy and ensure the model is published/allowed for your account, or try a different model.\n");
                    g.append("You can also set the environment variables:\n  OPENROUTER_MODEL (e.g. " + API_MODEL
                            + ")\n  YOUR_API_KEY\n\n");
                    g.append("As a quick test, try a curl request (replace the API key with your key):\n");
                    g.append("  curl -v -X POST \"" + API_URL
                            + "\" -H \"Authorization: Bearer <YOUR_KEY>\" -H \"Content-Type: application/json\" -d '<JSON_PAYLOAD>'\n\n");
                    return g.toString();
                }
                return "[AI Error] All models returned errors. See logs/ for the last response.\n" + lastErrorBody;
            }
            if (lastException != null)
                return "[AI Error] " + lastException.getMessage();
            return "[AI Error] Unknown failure";
        } catch (Exception e) {
            return "[AI Error] " + e.getMessage();
        }
    }

    private static String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String extractChoiceContent(String resp) {
        if (resp == null)
            return null;
        try {

            int choices = resp.indexOf("\"choices\"");
            if (choices < 0)
                return null;
            int msg = resp.indexOf("\"message\"", choices);
            if (msg < 0)
                return null;
            int content = resp.indexOf("\"content\"", msg);
            if (content < 0)
                return null;
            int colon = resp.indexOf(':', content);
            int firstQuote = resp.indexOf('"', colon + 1);
            if (firstQuote < 0)
                return null;
            int secondQuote = resp.indexOf('"', firstQuote + 1);
            if (secondQuote < 0)
                return null;
            String val = resp.substring(firstQuote + 1, secondQuote);
            return val.replace("\\\"", "\"");
        } catch (Exception e) {
            return null;
        }
    }

    private static String sanitizeResponse(String s) {
        if (s == null)
            return null;
        try {

            String out = s.replace("\\n", " ").replace("\\r", " ").replace('\n', ' ').replace('\r', ' ');

            out = out.replaceAll("\"?\\*+\"?", "");
            out = out.replaceAll("#+ ", "");
            out = out.replaceAll("`+", "");

            out = out.replaceAll("\\s+", " ");

            out = out.trim();

            if (out.length() > 2000)
                out = out.substring(0, 2000) + "...";
            return out;
        } catch (Exception e) {
            return s;
        }
    }
}
