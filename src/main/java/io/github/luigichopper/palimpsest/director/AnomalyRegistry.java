package io.github.luigichopper.palimpsest.director;

import io.github.luigichopper.palimpsest.director.anomalies.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnomalyRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/AnomalyRegistry");
    private static final AnomalyRegistry INSTANCE = new AnomalyRegistry();

    private final Map<String, Anomaly> anomalies = new ConcurrentHashMap<>();

    public AnomalyRegistry() {
        registerDefaults();
    }

    public static AnomalyRegistry get() {
        return INSTANCE;
    }

    public void register(Anomaly anomaly) {
        anomalies.put(anomaly.id(), anomaly);
        LOGGER.debug("Registered anomaly: {} (tier: {}, cost: {})", anomaly.id(), anomaly.tier(), anomaly.cost());
    }

    public Optional<Anomaly> get(String id) {
        return Optional.ofNullable(anomalies.get(id));
    }

    public Collection<Anomaly> getAll() {
        return Collections.unmodifiableCollection(anomalies.values());
    }

    public List<Anomaly> getCandidates(int maxTier, float tension, AnomalyContext ctx) {
        if (maxTier <= 0) {
            return Collections.emptyList();
        }

        List<Anomaly> candidates = new ArrayList<>();
        for (Anomaly a : anomalies.values()) {
            if (a.tier() <= maxTier && a.cost() <= tension) {
                if (a.canFire(ctx)) {
                    candidates.add(a);
                }
            }
        }
        return candidates;
    }

    public void registerDefaults() {
        register(new DoorAjarAnomaly());
        register(new TorchOutAnomaly());
        register(new FootstepsBehindAnomaly());
        register(new SoundOfYouAnomaly());
        register(new ItemFrameTurnAnomaly());
        register(new SignChangeAnomaly());
        register(new WatcherAnomaly());
        register(new WrongHourAnomaly());
    }
}
