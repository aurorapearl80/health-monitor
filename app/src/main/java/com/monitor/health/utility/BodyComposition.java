package com.monitor.health.utility;

public class BodyComposition {

    private double totalBodyWater = 0;
    private double fatFreeMass = 0;
    private double fatMass = 0;
    private double leanMass = 0;
    private double bodyFatPercent = 0;

    // ===============================
    // Getters and Setters
    // ===============================
    public double getTotalBodyWater() {
        return totalBodyWater;
    }

    public void setTotalBodyWater(double totalBodyWater) {
        this.totalBodyWater = totalBodyWater;
    }

    public double getFatFreeMass() {
        return fatFreeMass;
    }

    public void setFatFreeMass(double fatFreeMass) {
        this.fatFreeMass = fatFreeMass;
    }

    public double getFatMass() {
        return fatMass;
    }

    public void setFatMass(double fatMass) {
        this.fatMass = fatMass;
    }

    public double getLeanMass() {
        return leanMass;
    }

    public void setLeanMass(double leanMass) {
        this.leanMass = leanMass;
    }

    public double getBodyFatPercent() {
        return bodyFatPercent;
    }

    public void setBodyFatPercent(double bodyFatPercent) {
        this.bodyFatPercent = bodyFatPercent;
    }

    public BodyComposition(double totalBodyWater, double fatFreeMass, double fatMass, double leanMass, double bodyFatPercent) {
        this.totalBodyWater = totalBodyWater;
        this.fatFreeMass = fatFreeMass;
        this.fatMass = fatMass;
        this.leanMass = leanMass;
        this.bodyFatPercent = bodyFatPercent;
    }

    public BodyComposition() {}

    // ===============================
    // Computation Functions
    // ===============================

    public double computeFatFreeMass_1a(double weight_kg, double r_ohms, double height_cm, String gender, int age) {
        int g = gender.equals("M") ? 1 : 0;
        return 13.055
                + 0.204 * weight_kg
                + 0.394 * (height_cm * height_cm) / r_ohms
                - 0.136 * age
                + 8.125 * g;
    }

    public void computeBodyWaterMuscleFat_1a(double weight_kg, double r_ohms, double height_cm, String gender, int age) {

        // FFM
        double ffm = computeFatFreeMass_1a(weight_kg, r_ohms, height_cm, gender, age);
        setFatFreeMass(ffm);

        // TBW
        setTotalBodyWater(getFatFreeMass() * 0.732);

        // Fat mass
        setFatMass(weight_kg - getFatFreeMass());

        // Minimum/essential fat
        double minimumFat = gender.equals("M") ? weight_kg * 0.06 : weight_kg * 0.13;
        double essentialFat = gender.equals("M") ? weight_kg * 0.04 : weight_kg * 0.10;

        // Enforce minimum fat
        if (getFatMass() < minimumFat) {
            setFatMass(minimumFat);

            // Recalculate FFM + TBW
            setFatFreeMass(weight_kg - getFatMass());
            setTotalBodyWater(getFatFreeMass() * 0.732);
        }

        // Lean mass
        setLeanMass(getFatFreeMass() + essentialFat);

        // Body fat %
        setBodyFatPercent((getFatMass() / weight_kg) * 100);
    }

    // ===============================
    // Display
    // ===============================

    public void displayBodyComposition() {

        double weight_kg = 59.9;
        double footToFootResistance = 600;
        double height_ft = 5.3;
        String gender = "male";
        int age = 45;

        double height_cm = height_ft * 12 * 2.54;

        computeBodyWaterMuscleFat_1a(weight_kg, footToFootResistance, height_cm, gender, age);

        System.out.println("=== Accurate Body Composition Results ===");
        System.out.printf("Gender: %s%n", gender);
        System.out.printf("Height: %.2f cm%n", height_cm);
        System.out.printf("Weight: %.2f kg%n", weight_kg);
        System.out.printf("Resistance: %.0f ohms%n", footToFootResistance);
        System.out.println("");
        System.out.println("----------------------------------------");
        System.out.printf("Fat-Free Mass (FFM): %.2f kg%n", getFatFreeMass());
        System.out.printf("Lean Body Mass (LBM = FFM + essential fat): %.2f kg%n", getLeanMass());
        System.out.printf("Fat Mass (total FM): %.2f kg%n", getFatMass());
        System.out.printf("Body Fat Percentage: %.2f%%%n", getBodyFatPercent());
        System.out.printf("Total Body Water (TBW = 0.73 * FFM): %.2f L%n", getTotalBodyWater());
    }
}
