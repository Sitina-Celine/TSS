import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class TSSWorker {
    private static Properties config;

    public static void main(String[] args) {
        System.out.println("🚀 Starting TSS Worker...");

        try {
            loadConfig();
        } catch (IOException e) {
            System.out.println("❌ Failed to load config: " + e.getMessage());
            return;
        }

        int interval = Integer.parseInt(config.getProperty("poll.interval.seconds", "60")) * 1000;
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new WorkerTask(), 0, interval);

        try {
            while (true) Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {}
    }

    private static void loadConfig() throws IOException {
        config = new Properties();
        FileInputStream in = new FileInputStream("config/app.properties");
        config.load(in);
        in.close();
    }

    static class WorkerTask extends TimerTask {
        @Override
        public void run() {
            System.out.println("⏰ Checking for messages...");

            try {
                String base = config.getProperty("api.base.url");
                String fetchUrl = base + config.getProperty("api.fetch.endpoint");
                String token = config.getProperty("auth.token");

                String response = sendGetRequest(fetchUrl, token);
                System.out.println("📥 Received: " + response);

                boolean sent = sendMessage("email", "someone@example.com", "Hi Cesy!");

                if (sent) {
                    String updateUrl = base + config.getProperty("api.update.endpoint") + "?id=123&sent=true";
                    sendPostRequest(updateUrl, token);
                    System.out.println("✅ Message status updated.");
                }

            } catch (Exception e) {
                System.out.println("⚠️ Error during run: " + e.getMessage());
            }
        }

        private boolean sendMessage(String type, String to, String message) throws IOException {
            if (type.equalsIgnoreCase("email")) {
                String emailUrl = config.getProperty("email.gateway.url");
                String payload = "{\"to\": \"" + to + "\", \"message\": \"" + message + "\"}";
                String apiKey = config.getProperty("email.gateway.apiKey");

                sendPostRequest(emailUrl, apiKey, payload);
                System.out.println("📧 Email sent to " + to);
                return true;
            }
            return false;
        }

        private String sendGetRequest(String urlStr, String token) throws IOException {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestMethod("GET");

            return readResponse(conn);
        }

        private void sendPostRequest(String urlStr, String token) throws IOException {
            sendPostRequest(urlStr, token, null);
        }

        private void sendPostRequest(String urlStr, String token, String payload) throws IOException {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            if (payload != null) {
                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes());
                os.flush();
                os.close();
            }

            readResponse(conn);
        }

        private String readResponse(HttpURLConnection conn) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();

            while ((line = in.readLine()) != null) {
                output.append(line);
            }

            in.close();
            conn.disconnect();
            return output.toString();
        }
    }
}
