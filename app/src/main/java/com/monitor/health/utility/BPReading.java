package com.monitor.health.utility;
public class BPReading {
    public final int systolic;
    public final int diastolic;

    public BPReading(int systolic, int diastolic) {
        this.systolic = systolic;
        this.diastolic = diastolic;
    }

    @Override
    public String toString() {
        return systolic + "/" + diastolic + " mmHg";
    }
}
