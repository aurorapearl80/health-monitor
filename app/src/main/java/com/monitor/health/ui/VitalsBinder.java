package com.monitor.health.ui;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Centralized binders for health metrics with safe defaults.
 * Each method observes the provided LiveData, handles null/empty cases,
 * and calls your bindMetric callback.
 */
public final class VitalsBinder {

    // Helper: safely get a TextView by ID from a container (returns null if missing)
    @Nullable
    private static TextView optText(@Nullable View container, @IdRes int id) {
        if (container == null || id == View.NO_ID || id == 0) return null;
        View v = container.findViewById(id);
        return (v instanceof TextView) ? (TextView) v : null;
    }

    private VitalsBinder() {}

    // Reuse this to call your existing bindMetric(row, icon, title, time, value, onClick)
    public interface MetricRowBinder {
        void bind(View row, @DrawableRes int iconRes, String title,
                  String timeAgo, String value, View.OnClickListener onClick);
    }

    // ---------- ECG ----------
    public static <G> void bindECG(
            LifecycleOwner owner,
            LiveData<List<G>> gLive,
            LiveData<String> updatedAtLive,
            LiveData<String> descLive,
            View row, @DrawableRes int iconRes, String title,
            MetricRowBinder binder, View.OnClickListener onClick
    ) {
        gLive.observe(owner, g -> {
            if (g == null || g.isEmpty() || g.get(0) == null) {
                binder.bind(row, iconRes, title, "--", "-- ", onClick);
                return;
            }
            updatedAtLive.observe(owner, timeAgo -> {
                if (timeAgo == null) {
                    binder.bind(row, iconRes, title, "--", "-- ", onClick);
                    return;
                }
                descLive.observe(owner, bpm -> {
                    String displayValue = "-- ";
                    try {
                        Object first = g.get(0);
                        List<?> conv = (List<?>) first.getClass().getMethod("getConvertedValues").invoke(first);
                        if (conv != null && !conv.isEmpty() && conv.get(0) != null) {
                            Object cv0 = conv.get(0);
                            double v = ((Number) cv0.getClass().getMethod("getValue").invoke(cv0)).doubleValue();
                            displayValue = Math.round(v) + " ";
                        }
                    } catch (Exception ignore) { }
                    binder.bind(row, iconRes, title, timeAgo, displayValue, onClick);
                });
            });
        });
    }

    // ---------- Temperature ----------
    public static <G> void bindTemperature(
            LifecycleOwner owner,
            LiveData<List<G>> gLive,
            LiveData<String> updatedAtLive,
            LiveData<String> descLive,
            View row, @DrawableRes int iconRes, String title,
            MetricRowBinder binder, View.OnClickListener onClick
    ) {
        gLive.observe(owner, g -> {
            if (g == null || g.isEmpty() || g.get(0) == null) {
                binder.bind(row, iconRes, title, "--", "-- ", onClick);
                return;
            }
            updatedAtLive.observe(owner, timeAgo -> {
                if (timeAgo == null) {
                    binder.bind(row, iconRes, title, "--", "-- ", onClick);
                    return;
                }
                descLive.observe(owner, bpm -> {
                    String displayValue = "-- ";
                    try {
                        Object first = g.get(0);
                        List<?> conv = (List<?>) first.getClass().getMethod("getConvertedValues").invoke(first);
                        if (conv != null && !conv.isEmpty() && conv.get(0) != null) {
                            Object cv0 = conv.get(0);
                            double v = ((Number) cv0.getClass().getMethod("getValue").invoke(cv0)).doubleValue();
                            displayValue = Math.round(v) + " ";
                        }
                    } catch (Exception ignore) { }
                    binder.bind(row, iconRes, title, timeAgo, displayValue, onClick);
                });
            });
        });
    }

    // ---------- Blood Oxygen ----------
    public static <G> void bindBloodOxygen(
            LifecycleOwner owner,
            LiveData<List<G>> gLive,
            LiveData<String> updatedAtLive,
            LiveData<String> descLive,
            View row, @DrawableRes int iconRes, String title,
            MetricRowBinder binder, View.OnClickListener onClick
    ) {
        gLive.observe(owner, g -> {
            if (g == null || g.size() <= 1 || g.get(1) == null) {
                binder.bind(row, iconRes, title, "--", "-- %", onClick);
                return;
            }
            updatedAtLive.observe(owner, timeAgo -> {
                if (timeAgo == null) {
                    binder.bind(row, iconRes, title, "--", "-- %", onClick);
                    return;
                }
                descLive.observe(owner, bpm -> {
                    String displayValue = "-- %";
                    try {
                        Object second = g.get(1);
                        List<?> conv = (List<?>) second.getClass().getMethod("getConvertedValues").invoke(second);
                        if (conv != null && !conv.isEmpty() && conv.get(0) != null) {
                            Object cv0 = conv.get(0);
                            double v = ((Number) cv0.getClass().getMethod("getValue").invoke(cv0)).doubleValue();
                            displayValue = Math.round(v) + " %";
                        }
                    } catch (Exception ignore) { }
                    binder.bind(row, iconRes, title, timeAgo, displayValue, onClick);
                });
            });
        });
    }

    // ---------- Weight ----------
    public static <G> void bindWeight(
            LifecycleOwner owner,
            LiveData<List<G>> gLive,
            LiveData<String> updatedAtLive,
            LiveData<String> descLive,
            View row, @DrawableRes int iconRes, String title,
            MetricRowBinder binder, View.OnClickListener onClick
    ) {
        gLive.observe(owner, g -> {
            if (g == null || g.isEmpty() || g.get(0) == null) {
                binder.bind(row, iconRes, title, "--", "-- lbs", onClick);
                return;
            }
            updatedAtLive.observe(owner, timeAgo -> {
                if (timeAgo == null) {
                    binder.bind(row, iconRes, title, "--", "-- lbs", onClick);
                    return;
                }
                descLive.observe(owner, bpm -> {
                    String displayValue = "-- lbs";
                    try {
                        Object first = g.get(0);
                        List<?> conv = (List<?>) first.getClass().getMethod("getConvertedValues").invoke(first);
                        if (conv != null && conv.size() > 1 && conv.get(1) != null) {
                            Object cv1 = conv.get(1);
                            double v = ((Number) cv1.getClass().getMethod("getValue").invoke(cv1)).doubleValue();
                            displayValue = Math.round(v) + " lbs";
                        }
                    } catch (Exception ignore) { }
                    binder.bind(row, iconRes, title, timeAgo, displayValue, onClick);
                });
            });
        });
    }

    // ---------- Blood Pressure ----------
    public static <G> void bindBloodPressure(
            LifecycleOwner owner,
            LiveData<List<G>> gLive,
            LiveData<String> updatedAtLive,
            LiveData<String> descLive,
            View row, @DrawableRes int iconRes, String title,
            MetricRowBinder binder, View.OnClickListener onClick
    ) {
        gLive.observe(owner, g -> {
            if (g == null || g.size() <= 2 || g.get(0) == null || g.get(2) == null) {
                binder.bind(row, iconRes, title, "--", "--/-- mmHg", onClick);
                return;
            }
            updatedAtLive.observe(owner, timeAgo -> {
                if (timeAgo == null) {
                    binder.bind(row, iconRes, title, "--", "--/-- mmHg", onClick);
                    return;
                }
                descLive.observe(owner, bpm -> {
                    String unit = "mmHg";
                    String displayValue = "--/-- " + unit;
                    try {
                        Object sys = g.get(0);
                        Object dia = g.get(2);
                        Object u = sys.getClass().getMethod("getUnit").invoke(sys);
                        if (u instanceof String && !((String) u).isEmpty()) unit = (String) u;

                        double sv = ((Number) sys.getClass().getMethod("getValue").invoke(sys)).doubleValue();
                        double dv = ((Number) dia.getClass().getMethod("getValue").invoke(dia)).doubleValue();
                        displayValue = Math.round(sv) + "/" + Math.round(dv) + " " + unit;
                    } catch (Exception ignore) { }
                    binder.bind(row, iconRes, title, timeAgo, displayValue, onClick);
                });
            });
        });
    }

    // ---------- Glucose ----------
    // SIMPLE: no TextViews / no IDs. Just binds the row.
    public static <G> void bindGlucose(
            LifecycleOwner owner,
            LiveData<G> gLive,
            LiveData<String> updatedAtLive,
            LiveData<String> descLive,
            View row, @DrawableRes int iconRes, String title,
            MetricRowBinder binder, View.OnClickListener onClick
    ) {
        bindGlucose(
                owner, gLive, updatedAtLive, descLive,
                row, iconRes, title, binder, onClick,
                /*container*/ null,
                /*valueId*/ View.NO_ID,
                /*unitId*/  View.NO_ID,
                /*outcomeId*/ View.NO_ID,
                /*timeAgoId*/ View.NO_ID,
                /*bpmId*/ View.NO_ID
        );
    }

    // OPTIONAL IDs: provide a container view + IDs you want updated (others can be NO_ID/0)
    public static <G> void bindGlucose(
            LifecycleOwner owner,
            LiveData<G> gLive,
            LiveData<String> updatedAtLive,
            LiveData<String> descLive,
            View row, @DrawableRes int iconRes, String title,
            MetricRowBinder binder, View.OnClickListener onClick,
            @Nullable View container,
            @IdRes int valueId,
            @IdRes int unitId,
            @IdRes int outcomeId,
            @IdRes int timeAgoId,
            @IdRes int bpmId
    ) {
        gLive.observe(owner, g -> {
            if (g == null) {
                TextView tvVal  = optText(container, valueId);   if (tvVal  != null) tvVal.setText("--");
                TextView tvUnit = optText(container, unitId);    if (tvUnit != null) tvUnit.setText("");
                TextView tvOut  = optText(container, outcomeId); if (tvOut  != null) tvOut.setText("");
                TextView tvTime = optText(container, timeAgoId); if (tvTime != null) tvTime.setText("--");
                TextView tvBpm  = optText(container, bpmId);     if (tvBpm  != null) tvBpm.setText("--");
                binder.bind(row, iconRes, title, "--", "--", onClick);
                return;
            }

            // Build unit/value/outcome safely via reflection
            String unit = "";
            String valueStr = "--";
            String outcome = "";
            try {
                Object u = g.getClass().getMethod("getUnit").invoke(g);
                if (u instanceof String) unit = (String) u;

                double raw = ((Number) g.getClass().getMethod("getValue").invoke(g)).doubleValue();
                if (!Double.isNaN(raw)) {
                    valueStr = (raw == Math.floor(raw)) ? String.valueOf((int) raw) : String.valueOf(raw);
                }

                Object rating = g.getClass().getMethod("getRatingInfo").invoke(g);
                if (rating != null) {
                    Object ro = rating.getClass().getMethod("getReadingOutcome").invoke(rating);
                    if (ro instanceof String) outcome = (String) ro;
                }
            } catch (Exception ignore) { }

            // Update optional views if provided
            final String unitFinal = unit;
            final String valueStrFinal = valueStr;
            final String outcomeFinal = outcome;

            TextView tvVal  = optText(container, valueId);   if (tvVal  != null) tvVal.setText(valueStrFinal);
            TextView tvUnit = optText(container, unitId);    if (tvUnit != null) tvUnit.setText(unitFinal);
            TextView tvOut  = optText(container, outcomeId); if (tvOut  != null) tvOut.setText(outcomeFinal);

            updatedAtLive.observe(owner, timeAgo -> {
                final String displayTime = (timeAgo != null) ? timeAgo : "--";
                TextView tvTime = optText(container, timeAgoId); if (tvTime != null) tvTime.setText(displayTime);

                descLive.observe(owner, bpm -> {
                    TextView tvBpm = optText(container, bpmId); if (tvBpm != null) tvBpm.setText(bpm != null ? bpm : "--");

                    final String valueWithUnit = "--".equals(valueStrFinal)
                            ? "--"
                            : (unitFinal.isEmpty() ? valueStrFinal : valueStrFinal + " " + unitFinal);

                    binder.bind(row, iconRes, title, displayTime, valueWithUnit, onClick);
                });
            });
        });
    }
}
