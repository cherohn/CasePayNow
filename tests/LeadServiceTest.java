import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** No framework, database, network, or JVM assertion flag required. */
public final class LeadServiceTest {
    private static final LeadService.Auth AGENT = new LeadService.Auth("U-A", "T-A", "agent");
    private static final LeadService.Auth MANAGER = new LeadService.Auth("U-M", "T-A", "manager");
    private static int passed;

    public static void main(String[] args) {
        run("1. unauthenticated: 401 and no mutation", () ->
                rejected(null, body(101, "sent_to_funder", 1), fixtures(), 401, "unauthenticated"));
        run("2. assigned agent: 200 and only status/version change", () ->
                accepted(AGENT, 101, "sent_to_funder"));
        run("3. another agent's lead: 404 and no mutation", () ->
                rejected(AGENT, body(102, "sent_to_funder", 1), fixtures(), 404, "not_found"));
        run("4. cross-tenant manager: 404 and no mutation", () ->
                rejected(MANAGER, body(201, "sent_to_funder", 1), fixtures(), 404, "not_found"));
        run("5. same-tenant manager: 200", () -> accepted(MANAGER, 102, "cannot_fund"));
        run("6. unknown target status: 422 and no mutation", () ->
                rejected(AGENT, body(101, "approved", 1), fixtures(), 422, "invalid_body"));
        run("7. replay version 1 after success: 409 and no second write", () -> {
            Map<BigInteger, LeadService.Lead> leads = fixtures();
            acceptInStore(AGENT, 101, "sent_to_funder", leads);
            rejected(AGENT, body(101, "sent_to_funder", 1), leads, 409, "version_conflict");
        });
        run("8a. unknown role: 403 and no mutation", () ->
                rejected(new LeadService.Auth("U-A", "T-A", "admin"),
                        body(101, "sent_to_funder", 1), fixtures(), 403, "forbidden"));
        run("8b. forged manager in body cannot elevate agent", () -> {
            Map<String, Object> payload = body(102, "sent_to_funder", 1);
            payload.put("role", "manager");
            rejected(AGENT, payload, fixtures(), 422, "invalid_body");
        });
        run("8c. forged tenant in body cannot cross tenants", () -> {
            Map<String, Object> payload = body(201, "sent_to_funder", 1);
            payload.put("tenant_id", "T-B");
            payload.put("role", "manager");
            rejected(AGENT, payload, fixtures(), 422, "invalid_body");
        });

        for (String field : List.of("id", "version")) {
            Object[] invalid = {"1", true, false, 0, -1, 1.0, 1.5,
                    new BigDecimal("1.0"), BigInteger.ZERO, new BigInteger("-5"), null};
            for (Object value : invalid) {
                run("invalid " + field + ": " + describe(value), () -> {
                    Map<String, Object> payload = body(101, "sent_to_funder", 1);
                    payload.put(field, value);
                    rejected(AGENT, payload, fixtures(), 422, "invalid_body");
                });
            }
        }
        Object[] malformed = {null, "{}", List.of(), true, 101, Map.of()};
        for (Object value : malformed) {
            run("malformed body: " + describe(value), () ->
                    rejected(AGENT, value, fixtures(), 422, "invalid_body"));
        }
        for (String field : List.of("id", "status", "version")) {
            run("missing required field: " + field, () -> {
                Map<String, Object> payload = body(101, "sent_to_funder", 1);
                payload.remove(field);
                rejected(AGENT, payload, fixtures(), 422, "invalid_body");
            });
        }
        Object[] invalidStatuses = {null, true, 1, "", "SENT_TO_FUNDER", "sent_to_funder ", "contact_lawyer"};
        for (Object value : invalidStatuses) {
            run("invalid status: " + describe(value), () ->
                    rejected(AGENT, body(101, value, 1), fixtures(), 422, "invalid_body"));
        }
        for (String field : List.of("owner", "assigned_to", "notes")) {
            run("unexpected field is rejected: " + field, () -> {
                Map<String, Object> payload = body(101, "sent_to_funder", 1);
                payload.put(field, "U-A");
                rejected(AGENT, payload, fixtures(), 422, "invalid_body");
            });
        }

        run("agent can select the second terminal target", () -> accepted(AGENT, 101, "cannot_fund"));
        run("missing lead: same 404 payload as inaccessible lead", () ->
                rejected(AGENT, body(999, "sent_to_funder", 1), fixtures(), 404, "not_found"));
        run("cross-tenant agent with matching assignment is denied", () ->
                rejected(new LeadService.Auth("U-X", "T-A", "agent"),
                        body(201, "sent_to_funder", 1), fixtures(), 404, "not_found"));
        run("future version: 409 and no mutation", () ->
                rejected(AGENT, body(101, "sent_to_funder", 2), fixtures(), 409, "version_conflict"));

        for (String terminal : List.of("sent_to_funder", "cannot_fund")) {
            run("terminal state with current version: " + terminal, () -> {
                Map<BigInteger, LeadService.Lead> leads = fixtures();
                acceptInStore(AGENT, 101, terminal, leads);
                rejected(AGENT, body(101, "cannot_fund", 2), leads, 422, "invalid_transition");
            });
        }

        run("order: authentication before role/body checks", () ->
                rejected(null, null, fixtures(), 401, "unauthenticated"));
        run("order: unknown role before malformed body", () ->
                rejected(new LeadService.Auth("U-A", "T-A", "ADMIN"),
                        null, fixtures(), 403, "forbidden"));
        run("order: input validation before record lookup", () ->
                rejected(AGENT, body(999, "invalid", 1), fixtures(), 422, "invalid_body"));
        run("order: permission before version and state", () -> {
            Map<BigInteger, LeadService.Lead> leads = fixtures();
            LeadService.Lead old = leads.get(number(201));
            leads.put(old.id(), new LeadService.Lead(old.id(), old.tenantId(), old.assignedTo(),
                    "cannot_fund", number(2), old.notes()));
            rejected(MANAGER, body(201, "sent_to_funder", 1), leads, 404, "not_found");
        });
        run("large positive JSON integer ID is valid input, absent record gives 404", () ->
                rejected(AGENT, body(new BigInteger("9223372036854775808"), "cannot_fund", 1),
                        fixtures(), 404, "not_found"));
        run("integer representations and version increment beyond long range", () -> {
            Map<BigInteger, LeadService.Lead> leads = fixtures();
            BigInteger largeVersion = new BigInteger("9223372036854775807");
            LeadService.Lead old = leads.get(number(101));
            leads.put(old.id(), new LeadService.Lead(old.id(), old.tenantId(), old.assignedTo(),
                    old.status(), largeVersion, old.notes()));
            Map<BigInteger, LeadService.Lead> expected = new HashMap<>(leads);
            expected.put(number(101), new LeadService.Lead(number(101), "T-A", "U-A",
                    "cannot_fund", new BigInteger("9223372036854775808"), "Fake note A"));
            LeadService.Result result = LeadService.updateLead(AGENT,
                    body(101L, "cannot_fund", largeVersion), leads);
            equal(new LeadService.Result(200, Map.of("id", number(101), "status", "cannot_fund",
                    "version", new BigInteger("9223372036854775808"))), result, "response");
            equal(expected, leads, "entire store");
        });
        System.out.println("PASS: " + passed + " tests; no network or external dependencies.");
    }

    private static void accepted(LeadService.Auth auth, int id, String target) {
        acceptInStore(auth, id, target, fixtures());
    }

    private static void acceptInStore(LeadService.Auth auth, int id, String target,
                                      Map<BigInteger, LeadService.Lead> leads) {
        Map<BigInteger, LeadService.Lead> expected = new HashMap<>(leads);
        LeadService.Lead old = expected.get(number(id));
        expected.put(number(id), new LeadService.Lead(number(id), old.tenantId(),
                old.assignedTo(), target, number(2), old.notes()));
        Map<String, Object> payload = body(id, target, 1);
        Map<String, Object> originalPayload = new HashMap<>(payload);
        LeadService.Result actual = LeadService.updateLead(auth, payload, leads);
        equal(new LeadService.Result(200, Map.of("id", number(id), "status", target,
                "version", number(2))), actual, "response with only allowed fields");
        equal(expected, leads, "entire store, including unrelated fields and records");
        equal(originalPayload, payload, "input body is unchanged");
    }

    private static void rejected(LeadService.Auth auth, Object payload,
                                 Map<BigInteger, LeadService.Lead> leads, int status, String code) {
        Map<BigInteger, LeadService.Lead> before = new HashMap<>(leads);
        Object originalPayload = payload instanceof Map<?, ?> map ? new HashMap<>(map) : payload;
        LeadService.Result actual = LeadService.updateLead(auth, payload, leads);
        equal(new LeadService.Result(status, Map.of("error", code)), actual, "exact error response");
        equal(before, leads, "no mutation anywhere in the store");
        equal(originalPayload, payload, "input body is unchanged");
    }

    private static Map<BigInteger, LeadService.Lead> fixtures() {
        return new HashMap<>(Map.of(
                number(101), new LeadService.Lead(number(101), "T-A", "U-A",
                        "contact_lawyer", number(1), "Fake note A"),
                number(102), new LeadService.Lead(number(102), "T-A", "U-B",
                        "contact_lawyer", number(1), "Fake note B"),
                number(201), new LeadService.Lead(number(201), "T-B", "U-X",
                        "contact_lawyer", number(1), "Fake note X")));
    }

    private static Map<String, Object> body(Object id, Object status, Object version) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("status", status);
        result.put("version", version);
        return result;
    }

    private static BigInteger number(long value) {
        return BigInteger.valueOf(value);
    }

    private static String describe(Object value) {
        return value == null ? "null" : value + " (" + value.getClass().getSimpleName() + ")";
    }

    private static void equal(Object expected, Object actual, String context) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(context + ": expected " + expected + ", got " + actual);
        }
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("OK " + name);
        } catch (AssertionError | RuntimeException failure) {
            throw new AssertionError("FAILED: " + name, failure);
        }
    }
}
