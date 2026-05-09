import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AIClient {
    // Reads endpoint, API key and model from environment variables.
    // Also supports OpenRouter-style variables as a fallback.
    // Inspector-style defaults (do NOT hardcode secrets in source control):
    // [Header("OpenRouter Settings")]
    // [Tooltip("OpenRouter API Key. Do NOT hardcode secrets in source control — set
    // this in the Inspector at runtime or use the YOUR_API_KEY environment
    // variable.")]

    // [Tooltip("Model to request from OpenRouter")]
    // public String model = "openai/gpt-oss-20b:free";
    // [TextArea]
    private static final String API_URL = System.getenv().getOrDefault("AI_API_URL",
            System.getenv().getOrDefault("OPENROUTER_API_URL", "https://openrouter.ai/api/v1/chat/completions"));
    private static final String API_KEY = System.getenv().getOrDefault("AI_API_KEY", System.getenv().getOrDefault(
            "YOUR_API_KEY", "YOUR_API_KEY"));
    private static final String API_MODEL = System.getenv().getOrDefault("AI_MODEL",
            System.getenv().getOrDefault("OPENROUTER_MODEL", "google/gemma-3-27b-it:free"));
    // System prompt to instruct the assistant to avoid markdown/newline escapes and
    // be brief.
    private static final String SYSTEM_PROMPT = "You are the Library AI Assistant. Follow these rules when replying: " +
            "(1) Be very brief and answer in plain sentences. " +
            "(2) Do NOT use Markdown, headings, bullets, or formatting characters (no **, no ##, no backticks, no lists). "
            +
            "(3) Dxo NOT emit literal escape sequences like \\n or \\t; use plain text without newlines where possible. "
            +
            "(4) Call out any important restrictions briefly (for example: cannot provide full copyrighted text, cannot reveal private data). "
            +
            "(5) Identify as the Library Assistant in one short phrase at the start if relevant.";

    /**
     * Send a query using an OpenRouter-like chat completions API. Returns the
     * assistant message
     * content (first choice) or an error string starting with "[AI Error]" on
     * failure.
     */
    public static String sendQuery(String query) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            // Use endpoint variable for clarity (user-suggested)
            String endpoint = API_URL;
            // Try a small fallback list of models: configured model first, then gpt-4o-mini
            String[] tryModels = new String[] { API_MODEL, "your_ai_model" };
            Exception lastException = null;
            String lastErrorBody = null;

            for (String model : tryModels) {
                try {
                    // Build messages array including a system prompt and the user message
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
                        // write raw error body for debugging
                        try {
                            java.nio.file.Path logsDir = java.nio.file.Paths.get("logs");
                            if (!java.nio.file.Files.exists(logsDir))
                                java.nio.file.Files.createDirectories(logsDir);
                            String ts = String.valueOf(java.time.Instant.now().toEpochMilli());
                            java.nio.file.Files.writeString(logsDir.resolve("ai_error_" + ts + ".json"), lastErrorBody);
                        } catch (Exception writeEx) {
                            // ignore logging failure
                        }

                        // If policy 404, and message suggests data policy, try next model in list
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
                                        // try next model in list
                                        continue;
                                    }
                                    return "[AI Error] HTTP " + response.statusCode() + ": " + msg;
                                }
                            }
                        } catch (Exception ex) {
                            // fallthrough
                        }
                        return "[AI Error] HTTP " + response.statusCode() + ": " + body;
                    }

                    String body = response.body();

                    // Try to extract choices[0].message.content
                    String content = extractChoiceContent(body);
                    if (content != null && !content.isEmpty())
                        return sanitizeResponse(content);

                    // Fallback content extraction
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
                    // try next model
                    continue;
                }
            }

            // If we reached here, all attempts failed
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
            // look for "choices" then first "message" then "content"
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
            // Replace escaped newline sequences and real newlines with a space
            String out = s.replace("\\n", " ").replace("\\r", " ").replace('\n', ' ').replace('\r', ' ');
            // Remove common markdown markers: #, *, `
            out = out.replaceAll("\"?\\*+\"?", "");
            out = out.replaceAll("#+ ", "");
            out = out.replaceAll("`+", "");
            // Collapse multiple whitespace into single space
            out = out.replaceAll("\\s+", " ");
            // Trim
            out = out.trim();
            // Limit length to a reasonable number to keep answers brief (optional)
            if (out.length() > 2000)
                out = out.substring(0, 2000) + "...";
            return out;
        } catch (Exception e) {
            return s;
        }
    }
}
