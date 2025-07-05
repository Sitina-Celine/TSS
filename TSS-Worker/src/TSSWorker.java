import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * TSSWorker is a background service that:
 * - Periodically fetches messages from a REST API
 * - Sends EMAIL or SMS messages based on type
 * - Updates the sent status of messages via POST request
 */
public class TSSWorker {
    private static Properties config; // Configuration from app.properties
    private static ScheduledExecutorService scheduler; // Runs background job periodically

    public static void main(String[] args) {
        System.out.println("Starting TSS Worker...");

        try {
            // Load configuration from config/app.properties
            loadConfig();
        } catch (IOException e) {
            System.out.println("Failed to load config: " + e.getMessage());
            return;
        }

        // Get polling interval from config (defaults to 60 seconds if not set)
        int interval = Integer.parseInt(config.getProperty("poll.interval.seconds", "60"));

        // Schedule the worker task to run periodically
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new WorkerTask(), 0, interval, TimeUnit.SECONDS);

        // Ensure graceful shutdown when the application is terminated
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down TSS Worker...");
            scheduler.shutdown();
        }));

        // Keep the main thread alive indefinitely
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Worker was interrupted.");
        }
    }

    // Loads the configuration from the app.properties file
    private static void loadConfig() throws IOException {
        config = new Properties();
        FileInputStream in = new FileInputStream(Paths.get("config", "app.properties").toFile());
        config.load(in);
        in.close();
    }

    /**
     * WorkerTask is the background task that:
     * - Fetches messages from API
     * - Sends EMAIL or SMS messages
     * - Updates the status in the API if successful
     */
    static class WorkerTask implements Runnable {
        @Override
        public void run() {
            System.out.println("Checking for messages...");

            try {
                String base = config.getProperty("api.base.url");
                String fetchUrl = base + config.getProperty("api.fetch.endpoint");
                String token = config.getProperty("auth.token");

                // Step 1: Fetch messages
                String response = sendGetRequest(fetchUrl, token);
                System.out.println("Received: " + response);

                JSONArray messages = new JSONArray(response);

                // Step 2: Process each message
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject msg = messages.getJSONObject(i);

                    int id = msg.getInt("id");
                    String type = msg.getString("type");
                    String to = msg.getString("to");
                    String message = msg.getString("message");
                    boolean sentAlready = msg.getBoolean("sent");

                    if (!sentAlready) {
                        // Try sending the message with up to 3 retries
                        boolean sent = sendMessageWithRetries(type, to, message, 3);

                        // If successfully sent, mark it as sent
                        if (sent) {
                            String updateUrl = base + config.getProperty("api.update.endpoint");

                            String updatePayload = new JSONObject()
                                    .put("id", id)
                                    .put("sent", true)
                                    .toString();

                            sendPostRequest(updateUrl, token, updatePayload);
                            System.out.println("Message ID " + id + " marked as sent.");
                        } else {
                            System.out.println("Message ID " + id + " failed to send after retries.");
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("Error in WorkerTask: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Retries sending the message up to maxRetries times
        private boolean sendMessageWithRetries(String type, String to, String message, int maxRetries) throws IOException {
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    System.out.println("Attempt " + attempt + " to send " + type + " to " + to);
                    if (sendMessage(type, to, message)) return true;
                } catch (IOException e) {
                    System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(2000); // Wait before retrying
                        } catch (InterruptedException ignored) {}
                    }
                }
            }
            return false;
        }

        // Sends EMAIL or SMS using the appropriate API
        private boolean sendMessage(String type, String to, String message) throws IOException {
            // Prepare the request payload as JSON
            String payload = new JSONObject()
                    .put("to", to)
                    .put("message", message)
                    .toString();

            // Normalize and clean up the message type
            String cleanType = type.trim().toUpperCase();

            System.out.println("Normalized message type: " + cleanType);

            // Handle EMAIL
            if (cleanType.equals("EMAIL")) {
                String emailUrl = config.getProperty("email.gateway.url");
                String emailApiKey = config.getProperty("email.gateway.apiKey");

                sendPostRequest(emailUrl, emailApiKey, payload);
                System.out.println("Email sent to " + to);
                return true;
            }

            // Handle SMS
            if (cleanType.equals("SMS")) {
                String smsUrl = config.getProperty("sms.gateway.url");
                String smsApiKey = config.getProperty("sms.gateway.apiKey");

                sendPostRequest(smsUrl, smsApiKey, payload);
                System.out.println("SMS sent to " + to);
                return true;
            }

            // If type is neither EMAIL nor SMS, log unsupported type
            System.out.println("Unsupported message type: " + cleanType);
            return false;
        }

        // Sends a GET request with Authorization token
        private static String sendGetRequest(String urlStr, String token) throws IOException {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            return readResponse(conn);
        }

        // Sends a POST request with Authorization token and JSON payload
        private static void sendPostRequest(String urlStr, String token, String payload) throws IOException {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            readResponse(conn); // Read and discard response (or log if needed)
        }

        // Reads the HTTP response from a connection
        private static String readResponse(HttpURLConnection conn) throws IOException {
            int status = conn.getResponseCode();
            InputStream is = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();

            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                output.append(line);
            }

            in.close();
            conn.disconnect();
            return output.toString();
        }
    }
}
