package io.github.luigichopper.palimpsest.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SeedGate {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/SeedGate");
    private static final String SEED_RESOURCE = "/palimpsest_seed.json";

    private final String expectedSalt;
    private final String expectedHash;

    public SeedGate() {
        this(loadResourceData());
    }

    public SeedGate(String salt, String hash) {
        this.expectedSalt = salt;
        this.expectedHash = hash;
    }

    private SeedGate(String[] data) {
        this(data != null ? data[0] : "", data != null ? data[1] : "");
    }

    private static String[] loadResourceData() {
        try (InputStream in = SeedGate.class.getResourceAsStream(SEED_RESOURCE)) {
            if (in == null) {
                LOGGER.warn("Seed resource {} not found; mod will remain dormant", SEED_RESOURCE);
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String salt = json.has("salt") ? json.get("salt").getAsString() : "";
                String hash = json.has("hash") ? json.get("hash").getAsString() : "";
                return new String[]{salt, hash};
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse palimpsest_seed.json", e);
            return null;
        }
    }

    public static String computeHash(String salt, long seed) {
        return computeHash(salt, Long.toString(seed));
    }

    public static String computeHash(String salt, String seedAsDecimalString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] input = (salt + seedAsDecimalString).getBytes(StandardCharsets.UTF_8);
            byte[] hashBytes = digest.digest(input);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }

    public boolean isEligible(long seed) {
        PalimpsestConfig config = PalimpsestConfig.get();
        if (config.devMode && config.forceActivate) {
            LOGGER.info("SeedGate bypassed: devMode && forceActivate is true.");
            return true;
        }

        if (expectedSalt == null || expectedHash == null || expectedSalt.isEmpty() || expectedHash.isEmpty()) {
            LOGGER.warn("SeedGate has no valid salt/hash loaded.");
            return false;
        }

        String calculatedHash = computeHash(expectedSalt, seed);
        boolean matches = expectedHash.equalsIgnoreCase(calculatedHash);
        if (matches) {
            LOGGER.info("SeedGate match: world seed is eligible for archive recovery.");
        }
        return matches;
    }

    public String getExpectedSalt() {
        return expectedSalt;
    }

    public String getExpectedHash() {
        return expectedHash;
    }
}
