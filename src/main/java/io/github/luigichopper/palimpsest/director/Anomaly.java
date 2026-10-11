package io.github.luigichopper.palimpsest.director;

public interface Anomaly {
    String id();

    int tier(); // 1 = subtle, 2 = unsettling, 3 = intense

    float cost(); // Tension cost deducted when anomaly fires

    long cooldownSeconds();

    boolean canFire(AnomalyContext ctx);

    boolean fire(AnomalyContext ctx);
}
