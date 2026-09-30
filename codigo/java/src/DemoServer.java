import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/** Loopback-only demo adapter. This is deliberately not a production login system. */
public final class DemoServer {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private static final Map<String, LeadService.Auth> PROFILES = Map.of(
            "agent-a", new LeadService.Auth("U-A", "T-A", "agent"),
            "manager-a", new LeadService.Auth("U-M", "T-A", "manager"),
            "manager-b", new LeadService.Auth("U-X", "T-B", "manager"),
            "unknown", new LeadService.Auth("U-A", "T-A", "viewer"));
    private static final Map<String, Session> SESSIONS = new HashMap<>();
    private static Path publicRoot;

    private static final class Session {
        String profile = "agent-a";
        final String csrf = UUID.randomUUID().toString();
        final Map<BigInteger, LeadService.Lead> leads = new HashMap<>();
        Session() { reset(); }
        LeadService.Auth auth() { return PROFILES.get(profile); }
        void reset() {
            leads.clear();
            add(101, "T-A", "U-A");
            add(102, "T-A", "U-B");
            add(201, "T-B", "U-X");
        }
        private void add(int id, String tenant, String owner) {
            BigInteger key = BigInteger.valueOf(id);
            leads.put(key, new LeadService.Lead(key, tenant, owner,
                    "contact_lawyer", BigInteger.ONE, "Registro fictício da avaliação."));
        }
    }

    public static void main(String[] args) throws IOException {
        publicRoot = Path.of(args.length > 0 ? args[0] : "../frontend/dist").toAbsolutePath().normalize();
        if (!Files.isRegularFile(publicRoot.resolve("index.html"))) {
            throw new IllegalStateException("Frontend não compilado. Execute sh codigo/run-demo.sh.");
        }
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.createContext("/", DemoServer::handle);
        server.start();
        System.out.println("Demo local: http://127.0.0.1:8080 — React + Java, dados fictícios.");
        System.out.println("Ctrl+C para encerrar. Não disponibilize este servidor na internet.");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Content-Security-Policy",
                    "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; "
                    + "img-src 'self' data:; object-src 'none'; frame-ancestors 'none'; base-uri 'none'");
            if (!"127.0.0.1:8080".equals(exchange.getRequestHeaders().getFirst("Host"))) {
                send(exchange, 403, Map.of("error", "invalid_host"));
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (!path.startsWith("/api/")) {
                serveFile(exchange, path);
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod()) && !"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, Map.of("error", "method_not_allowed"));
                return;
            }
            Session session = session(exchange);
            if ("POST".equals(exchange.getRequestMethod())) {
                String origin = exchange.getRequestHeaders().getFirst("Origin");
                if ((origin != null && !origin.equals("http://127.0.0.1:8080"))
                        || !session.csrf.equals(exchange.getRequestHeaders().getFirst("X-CSRF-Token"))) {
                    send(exchange, 403, Map.of("error", "csrf_rejected"));
                    return;
                }
            }
            if ("GET".equals(exchange.getRequestMethod()) && path.equals("/api/session")) {
                send(exchange, 200, Map.of("profile", session.profile, "csrf", session.csrf));
            } else if ("GET".equals(exchange.getRequestMethod()) && path.equals("/api/leads")) {
                LeadService.Auth auth = session.auth();
                if (auth == null) { send(exchange, 401, Map.of("error", "unauthenticated")); return; }
                if (!knownRole(auth)) { send(exchange, 403, Map.of("error", "forbidden")); return; }
                send(exchange, 200, session.leads.values().stream().filter(lead -> canAccess(auth, lead))
                        .sorted((a, b) -> a.id().compareTo(b.id())).toList());
            } else if ("POST".equals(exchange.getRequestMethod()) && path.equals("/api/leads/update")) {
                Object body = readJson(exchange);
                if ("before-save".equals(exchange.getRequestHeaders().getFirst("X-Demo-Failure"))) {
                    send(exchange, 503, Map.of("error", "demo_unavailable"));
                    return;
                }
                LeadService.Result result = LeadService.updateLead(session.auth(), body, session.leads);
                send(exchange, result.status(), result.data_or_error());
            } else if ("POST".equals(exchange.getRequestMethod()) && path.equals("/api/demo/session")) {
                Object body = readJson(exchange);
                if (!(body instanceof Map<?, ?> values) || !(values.get("profile") instanceof String profile)
                        || !(PROFILES.containsKey(profile) || profile.equals("anonymous"))) {
                    send(exchange, 422, Map.of("error", "invalid_profile"));
                    return;
                }
                // Only a predefined fake profile can be selected; update bodies never set auth.
                session.profile = profile;
                send(exchange, 200, Map.of("profile", profile, "csrf", session.csrf));
            } else if ("POST".equals(exchange.getRequestMethod()) && path.equals("/api/demo/reset")) {
                session.reset();
                send(exchange, 200, Map.of("reset", true));
            } else if ("POST".equals(exchange.getRequestMethod()) && path.equals("/api/demo/conflict")) {
                Object body = readJson(exchange);
                Object id = body instanceof Map<?, ?> values ? values.get("id") : null;
                LeadService.Lead lead = session.leads.get(id);
                if (lead == null || !canAccess(session.auth(), lead)) {
                    send(exchange, 404, Map.of("error", "not_found"));
                    return;
                }
                session.leads.put(lead.id(), new LeadService.Lead(lead.id(), lead.tenantId(),
                        lead.assignedTo(), lead.status(), lead.version().add(BigInteger.ONE), lead.notes()));
                send(exchange, 200, Map.of("changed", true));
            } else {
                send(exchange, 404, Map.of("error", "not_found"));
            }
        } catch (RuntimeException badRequest) {
            send(exchange, 422, Map.of("error", "invalid_body"));
        } finally {
            exchange.close();
        }
    }

    private static Session session(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) {
            for (String part : cookie.split(";")) {
                if (part.trim().startsWith("demo_session=")) {
                    Session existing = SESSIONS.get(part.trim().substring("demo_session=".length()));
                    if (existing != null) { return existing; }
                }
            }
        }
        String key = UUID.randomUUID().toString();
        Session fresh = new Session();
        SESSIONS.put(key, fresh);
        exchange.getResponseHeaders().add("Set-Cookie", "demo_session=" + key + "; Path=/; HttpOnly; SameSite=Strict");
        return fresh;
    }

    private static boolean knownRole(LeadService.Auth auth) {
        return "agent".equals(auth.role()) || "manager".equals(auth.role());
    }

    private static boolean canAccess(LeadService.Auth auth, LeadService.Lead lead) {
        return auth != null && knownRole(auth) && auth.tenantId().equals(lead.tenantId())
                && ("manager".equals(auth.role()) || auth.userId().equals(lead.assignedTo()));
    }

    private static Object readJson(HttpExchange exchange) throws IOException {
        if (!"application/json".equals(exchange.getRequestHeaders().getFirst("Content-Type"))) {
            return null;
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(8193);
        if (bytes.length > 8192) { return null; }
        try {
            JsonElement tree = JSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonElement.class);
            return convert(tree);
        } catch (RuntimeException malformedJson) {
            return null;
        }
    }

    private static Object convert(JsonElement element) {
        if (element == null || element.isJsonNull()) { return null; }
        if (element.isJsonObject()) {
            Map<String, Object> result = new LinkedHashMap<>();
            element.getAsJsonObject().entrySet().forEach(entry -> result.put(entry.getKey(), convert(entry.getValue())));
            return result;
        }
        if (element.isJsonArray()) { return element.getAsJsonArray().asList().stream().map(DemoServer::convert).toList(); }
        JsonPrimitive value = element.getAsJsonPrimitive();
        if (value.isBoolean()) { return value.getAsBoolean(); }
        if (value.isString()) { return value.getAsString(); }
        String number = value.getAsString();
        return number.matches("-?(0|[1-9][0-9]*)") ? new BigInteger(number) : new BigDecimal(number);
    }

    private static void send(HttpExchange exchange, int status, Object data) throws IOException {
        byte[] bytes = JSON.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static void serveFile(HttpExchange exchange, String requestPath) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("error", "method_not_allowed"));
            return;
        }
        Path file = publicRoot.resolve(requestPath.equals("/") ? "index.html" : requestPath.substring(1)).normalize();
        if (!file.startsWith(publicRoot) || !Files.isRegularFile(file)) {
            send(exchange, 404, Map.of("error", "not_found"));
            return;
        }
        String type = file.toString().endsWith(".js") ? "text/javascript" :
                file.toString().endsWith(".css") ? "text/css" : "text/html";
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        byte[] bytes = Files.readAllBytes(file);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
