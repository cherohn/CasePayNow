import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

/** Local, single-threaded implementation. Authentication is supplied by the server. */
public final class LeadService {
    private static final Set<String> BODY_FIELDS = Set.of("id", "status", "version");

    private LeadService() {}

    public record Auth(String userId, String tenantId, String role) {}

    /** Immutable records allow tests to compare the complete store before and after. */
    public record Lead(BigInteger id, String tenantId, String assignedTo,
                       String status, BigInteger version, String notes) {}

    public record Result(int status, Map<String, Object> data_or_error) {}

    /**
     * body is untrusted, already deserialized JSON: objects are Maps, arrays are Lists.
     * Integer tokens must retain integer types; strings/booleans/floats are not coerced.
     * leads is a trusted, mutable store, accessed by one thread for this exercise.
     */
    public static Result updateLead(Auth auth, Object body, Map<BigInteger, Lead> leads) {
        // The validation order is part of the assessment's contract.
        if (auth == null) {
            return error(401, "unauthenticated");
        }
        if (!"agent".equals(auth.role()) && !"manager".equals(auth.role())) {
            return error(403, "forbidden");
        }
        if (!(body instanceof Map<?, ?> fields) || !fields.keySet().equals(BODY_FIELDS)) {
            return error(422, "invalid_body");
        }

        BigInteger id = positiveInteger(fields.get("id"));
        BigInteger version = positiveInteger(fields.get("version"));
        Object target = fields.get("status");
        if (id == null || version == null
                || !(target instanceof String targetStatus)
                || !("sent_to_funder".equals(targetStatus) || "cannot_fund".equals(targetStatus))) {
            return error(422, "invalid_body");
        }

        Lead lead = leads.get(id);
        if (lead == null || !auth.tenantId().equals(lead.tenantId())
                || ("agent".equals(auth.role()) && !auth.userId().equals(lead.assignedTo()))) {
            return error(404, "not_found");
        }
        if (!version.equals(lead.version())) {
            return error(409, "version_conflict");
        }
        if (!"contact_lawyer".equals(lead.status())) {
            return error(422, "invalid_transition");
        }

        Lead updated = new Lead(lead.id(), lead.tenantId(), lead.assignedTo(),
                targetStatus, lead.version().add(BigInteger.ONE), lead.notes());
        leads.put(id, updated);
        return new Result(200, Map.of("id", updated.id(), "status", updated.status(),
                "version", updated.version()));
    }

    private static Result error(int status, String code) {
        return new Result(status, Map.of("error", code));
    }

    private static BigInteger positiveInteger(Object value) {
        BigInteger integer;
        if (value instanceof BigInteger big) {
            integer = big;
        } else if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            integer = BigInteger.valueOf(((Number) value).longValue());
        } else {
            return null;
        }
        return integer.signum() > 0 ? integer : null;
    }
}
