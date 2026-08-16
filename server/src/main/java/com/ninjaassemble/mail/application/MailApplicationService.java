package com.ninjaassemble.mail.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.inventory.application.InventoryService;
import com.ninjaassemble.inventory.application.ItemCatalogService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class MailApplicationService {
    public static final String WELCOME_MAIL_VERSION = "welcome-mail-design-v1";
    private static final String WELCOME_SUBJECT = "mail.welcome.subject";
    private static final String WELCOME_BODY = "mail.welcome.body";
    private static final String WELCOME_ATTACHMENTS = """
            [{"kind":"CURRENCY","id":"GOLD","quantity":5000},
             {"kind":"CURRENCY","id":"DIAMOND","quantity":100},
             {"kind":"ITEM","id":"summon-ticket","quantity":1}]
            """;
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile(
            "\\{\\s*\"kind\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"id\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"quantity\"\\s*:\\s*(\\d+)\\s*}");

    private final JdbcTemplate jdbc;
    private final WalletService wallet;
    private final InventoryService inventory;
    private final ItemCatalogService items;
    private final Clock clock;

    public MailApplicationService(JdbcTemplate jdbc, WalletService wallet, InventoryService inventory,
                                  ItemCatalogService items, Clock clock) {
        this.jdbc = jdbc;
        this.wallet = wallet;
        this.inventory = inventory;
        this.items = items;
        this.clock = clock;
    }

    @Transactional
    public Mailbox view(UUID playerId) {
        ensureWelcome(playerId);
        Instant now = clock.instant();
        List<MailView> mails = jdbc.query("""
                select id, subject_key, body_key, attachments::text, read, claimed, created_at, expires_at
                from player_mail
                where player_id = ? and (expires_at is null or expires_at > ?)
                order by created_at desc, id desc
                """, (rs, row) -> new MailView(
                rs.getObject("id", UUID.class), rs.getString("subject_key"), displaySubject(rs.getString("subject_key")),
                rs.getString("body_key"), displayBody(rs.getString("body_key")), attachments(rs.getString("attachments")),
                rs.getBoolean("read"), rs.getBoolean("claimed"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant()),
                playerId, now);
        return new Mailbox(WELCOME_MAIL_VERSION, List.copyOf(mails));
    }

    @Transactional
    public void markRead(UUID playerId, UUID mailId) {
        int updated = jdbc.update("update player_mail set read = true where id = ? and player_id = ?", mailId, playerId);
        if (updated != 1) throw new IllegalArgumentException("mail not found");
    }

    @Transactional
    public ClaimResult claim(UUID playerId, UUID mailId) {
        jdbc.queryForObject("select id from players where id = ? for update", UUID.class, playerId);
        MailRow mail = jdbc.query("""
                select attachments::text, claimed, expires_at from player_mail
                where id = ? and player_id = ? for update
                """, rs -> rs.next() ? new MailRow(rs.getString(1), rs.getBoolean(2), rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toInstant()) : null,
                mailId, playerId);
        if (mail == null) throw new IllegalArgumentException("mail not found");
        if (mail.expiresAt() != null && !mail.expiresAt().isAfter(clock.instant())) throw new IllegalStateException("mail expired");
        List<Attachment> attachments = attachments(mail.attachmentsJson());
        if (mail.claimed()) return new ClaimResult(mailId, true, List.of());

        List<Grant> grants = new ArrayList<>();
        int index = 0;
        for (Attachment attachment : attachments) {
            String key = "mail:" + mailId + ":" + index++ + ":" + attachment.id();
            if ("CURRENCY".equals(attachment.kind())) {
                Currency currency = Currency.valueOf(attachment.id());
                long after = wallet.mutate(playerId, currency, attachment.quantity(), "MAIL_ATTACHMENT", mailId.toString(), key);
                grants.add(new Grant(attachment.kind(), attachment.id(), attachment.quantity(), after));
            } else if ("ITEM".equals(attachment.kind())) {
                items.require(attachment.id());
                long after = inventory.mutate(playerId, attachment.id(), attachment.quantity(), "MAIL_ATTACHMENT", key).quantity();
                grants.add(new Grant(attachment.kind(), attachment.id(), attachment.quantity(), after));
            } else {
                throw new IllegalStateException("unsupported mail attachment kind: " + attachment.kind());
            }
        }
        jdbc.update("update player_mail set read = true, claimed = true where id = ? and player_id = ?", mailId, playerId);
        return new ClaimResult(mailId, false, List.copyOf(grants));
    }

    private void ensureWelcome(UUID playerId) {
        jdbc.queryForObject("select id from players where id = ? for update", UUID.class, playerId);
        Boolean exists = jdbc.queryForObject("select exists(select 1 from player_mail where player_id = ? and subject_key = ?)", Boolean.class, playerId, WELCOME_SUBJECT);
        if (Boolean.TRUE.equals(exists)) return;
        jdbc.update("""
                insert into player_mail(id, player_id, subject_key, body_key, attachments, read, claimed, created_at)
                values (?, ?, ?, ?, cast(? as jsonb), false, false, ?)
                """, UUID.randomUUID(), playerId, WELCOME_SUBJECT, WELCOME_BODY, WELCOME_ATTACHMENTS, clock.instant());
    }

    private static List<Attachment> attachments(String json) {
        String source = json == null ? "[]" : json.trim();
        if (source.equals("[]")) return List.of();
        Matcher matcher = ATTACHMENT_PATTERN.matcher(source);
        List<Attachment> result = new ArrayList<>();
        while (matcher.find()) {
            String kind = matcher.group(1);
            String id = matcher.group(2);
            long quantity = Long.parseLong(matcher.group(3));
            if (kind.isBlank() || id.isBlank() || quantity <= 0) throw new IllegalStateException("invalid mail attachment");
            result.add(new Attachment(kind, id, quantity));
        }
        if (result.isEmpty()) throw new IllegalStateException("invalid mail attachments json");
        String normalized = source.replaceAll("\\s+", "");
        String reconstructed = result.stream()
                .map(value -> "{\"kind\":\"" + value.kind() + "\",\"id\":\"" + value.id() + "\",\"quantity\":" + value.quantity() + "}")
                .collect(Collectors.joining(",", "[", "]"));
        if (!normalized.equals(reconstructed)) throw new IllegalStateException("unsupported mail attachments json");
        return List.copyOf(result);
    }

    private static String displaySubject(String key) { return WELCOME_SUBJECT.equals(key) ? "Welcome to Ninja Assemble" : key; }
    private static String displayBody(String key) { return WELCOME_BODY.equals(key) ? "Your starter supplies are ready. Claim them once from this inbox." : key; }

    private record MailRow(String attachmentsJson, boolean claimed, Instant expiresAt) {}
    public record Attachment(String kind, String id, long quantity) {}
    public record Mailbox(String mailProfileVersion, List<MailView> mails) {}
    public record MailView(UUID mailId, String subjectKey, String subject, String bodyKey, String body,
                           List<Attachment> attachments, boolean read, boolean claimed, Instant createdAt, Instant expiresAt) {}
    public record Grant(String kind, String id, long quantity, long balanceAfter) {}
    public record ClaimResult(UUID mailId, boolean replayed, List<Grant> grants) {}
}
