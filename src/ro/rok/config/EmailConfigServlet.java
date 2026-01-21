package ro.rok.config;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class EmailConfigServlet extends HttpServlet {
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
    private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(1);
    private static final Map<Integer, EmailConfig> STORE = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String acceptHeader = Optional.ofNullable(req.getHeader("Accept")).orElse("");
        String pathInfo = Optional.ofNullable(req.getPathInfo()).orElse("");

        if (pathInfo.isEmpty() || "/".equals(pathInfo) || acceptHeader.contains("text/html")) {
            respondHtml(req, resp);
            return;
        }

        if ("/api".equals(pathInfo) || "/api/".equals(pathInfo)) {
            respondJson(resp, HttpServletResponse.SC_OK, toJsonList(listAll()));
            return;
        }

        Integer id = parseId(pathInfo);
        if (id == null) {
            respondError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid id");
            return;
        }

        EmailConfig config = STORE.get(id);
        if (config == null) {
            respondError(resp, HttpServletResponse.SC_NOT_FOUND, "Config not found");
            return;
        }
        respondJson(resp, HttpServletResponse.SC_OK, toJson(config));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        EmailConfig payload = parsePayload(req);
        if (payload == null) {
            respondError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid payload");
            return;
        }

        int id = ID_SEQUENCE.getAndIncrement();
        EmailConfig created = payload.withId(id);
        STORE.put(id, created);
        respondJson(resp, HttpServletResponse.SC_CREATED, toJson(created));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = parseId(req.getPathInfo());
        if (id == null) {
            respondError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid id");
            return;
        }
        EmailConfig existing = STORE.get(id);
        if (existing == null) {
            respondError(resp, HttpServletResponse.SC_NOT_FOUND, "Config not found");
            return;
        }

        EmailConfig payload = parsePayload(req);
        if (payload == null) {
            respondError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid payload");
            return;
        }

        EmailConfig updated = payload.withId(id);
        STORE.put(id, updated);
        respondJson(resp, HttpServletResponse.SC_OK, toJson(updated));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = parseId(req.getPathInfo());
        if (id == null) {
            respondError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid id");
            return;
        }

        EmailConfig existing = STORE.get(id);
        if (existing == null) {
            respondError(resp, HttpServletResponse.SC_NOT_FOUND, "Config not found");
            return;
        }

        EmailConfig updated = existing.withActive(false);
        STORE.put(id, updated);
        respondJson(resp, HttpServletResponse.SC_OK, toJson(updated));
    }

    private void respondHtml(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(CONTENT_TYPE_HTML);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        req.getRequestDispatcher("/email-config.jsp").forward(req, resp);
    }

    private List<EmailConfig> listAll() {
        List<EmailConfig> configs = new ArrayList<>(STORE.values());
        configs.sort(Comparator.comparingInt(EmailConfig::id));
        return configs;
    }

    private Integer parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) {
            return null;
        }
        String sanitized = pathInfo.replace("/", "").trim();
        if (sanitized.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(sanitized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private EmailConfig parsePayload(HttpServletRequest req) throws IOException {
        String body = req.getReader().lines().reduce("", (acc, line) -> acc + line);
        if (body.isBlank()) {
            return null;
        }
        Map<String, String> map = JsonHelper.parseJsonObject(body);
        if (map.isEmpty()) {
            return null;
        }
        return new EmailConfig(
                null,
                Boolean.parseBoolean(map.getOrDefault("addReply", "false")),
                map.getOrDefault("alias", ""),
                parseNullableInt(map.get("assignToGroup")),
                parseNullableInt(map.get("clientId")),
                parseNullableInt(map.get("defaultPostedBy")),
                parseNullableInt(map.get("locationId")),
                map.getOrDefault("password", ""),
                parseNullableInt(map.get("port")),
                map.getOrDefault("protocol", ""),
                map.getOrDefault("server", ""),
                parseNullableInt(map.get("ticketType")),
                map.getOrDefault("username", ""),
                Boolean.parseBoolean(map.getOrDefault("active", "true"))
        );
    }

    private Integer parseNullableInt(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void respondJson(HttpServletResponse resp, int status, String body) throws IOException {
        resp.setStatus(status);
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        PrintWriter writer = resp.getWriter();
        writer.write(body);
    }

    private void respondError(HttpServletResponse resp, int status, String message) throws IOException {
        respondJson(resp, status, "{\"message\":\"" + escapeJson(message) + "\"}");
    }

    private String toJsonList(List<EmailConfig> configs) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < configs.size(); i++) {
            builder.append(toJson(configs.get(i)));
            if (i < configs.size() - 1) {
                builder.append(',');
            }
        }
        builder.append(']');
        return builder.toString();
    }

    private String toJson(EmailConfig config) {
        return "{" +
                "\"id\":" + config.id() +
                ",\"addReply\":" + config.addReply() +
                ",\"alias\":\"" + escapeJson(config.alias()) + "\"" +
                ",\"assignToGroup\":" + jsonNullable(config.assignToGroup()) +
                ",\"clientId\":" + jsonNullable(config.clientId()) +
                ",\"defaultPostedBy\":" + jsonNullable(config.defaultPostedBy()) +
                ",\"locationId\":" + jsonNullable(config.locationId()) +
                ",\"password\":\"" + escapeJson(config.password()) + "\"" +
                ",\"port\":" + jsonNullable(config.port()) +
                ",\"protocol\":\"" + escapeJson(config.protocol()) + "\"" +
                ",\"server\":\"" + escapeJson(config.server()) + "\"" +
                ",\"ticketType\":" + jsonNullable(config.ticketType()) +
                ",\"username\":\"" + escapeJson(config.username()) + "\"" +
                ",\"active\":" + config.active() +
                "}";
    }

    private String jsonNullable(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private record EmailConfig(
            Integer id,
            boolean addReply,
            String alias,
            Integer assignToGroup,
            Integer clientId,
            Integer defaultPostedBy,
            Integer locationId,
            String password,
            Integer port,
            String protocol,
            String server,
            Integer ticketType,
            String username,
            boolean active
    ) {
        EmailConfig withId(int id) {
            return new EmailConfig(id, addReply, alias, assignToGroup, clientId, defaultPostedBy,
                    locationId, password, port, protocol, server, ticketType, username, active);
        }

        EmailConfig withActive(boolean active) {
            return new EmailConfig(id, addReply, alias, assignToGroup, clientId, defaultPostedBy,
                    locationId, password, port, protocol, server, ticketType, username, active);
        }
    }

    private static final class JsonHelper {
        private JsonHelper() {
        }

        static Map<String, String> parseJsonObject(String raw) {
            String trimmed = raw.trim();
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return Collections.emptyMap();
            }
            String content = trimmed.substring(1, trimmed.length() - 1).trim();
            if (content.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, String> map = new ConcurrentHashMap<>();
            List<String> pairs = splitPairs(content);
            for (String pair : pairs) {
                String[] parts = splitPair(pair);
                if (parts.length != 2) {
                    continue;
                }
                String key = unquote(parts[0].trim());
                String value = unquote(parts[1].trim());
                map.put(key, value);
            }
            return map;
        }

        private static List<String> splitPairs(String content) {
            List<String> pairs = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                    inQuotes = !inQuotes;
                }
                if (c == ',' && !inQuotes) {
                    pairs.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            if (current.length() > 0) {
                pairs.add(current.toString());
            }
            return pairs;
        }

        private static String[] splitPair(String pair) {
            boolean inQuotes = false;
            for (int i = 0; i < pair.length(); i++) {
                char c = pair.charAt(i);
                if (c == '"' && (i == 0 || pair.charAt(i - 1) != '\\')) {
                    inQuotes = !inQuotes;
                }
                if (c == ':' && !inQuotes) {
                    return new String[]{pair.substring(0, i), pair.substring(i + 1)};
                }
            }
            return new String[0];
        }

        private static String unquote(String value) {
            String trimmed = value.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                String inner = trimmed.substring(1, trimmed.length() - 1);
                return inner.replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t")
                        .replace("\\\\", "\\");
            }
            return trimmed;
        }
    }
}
