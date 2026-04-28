package com.monitor.health.model;

public class MedicinePayload {

    public String referenceMedicineId;
    public String medicineSchedule;
    public String name;
    public String type2;
    public String type;
    public int amount;
    public String unit;
    public String timeTaken;
    public String exactTimeTaken;
    public String dateTaken;

    public MedicinePayload(String referenceMedicineId, String medicineSchedule, String name,
                           String type2,String type, int amount,
                           String unit, String timeTaken, String exactTimeTaken, String dateTaken) {
        this.referenceMedicineId = referenceMedicineId;
        this.medicineSchedule = medicineSchedule;
        this.name = name;
        this.type2 = type2;
        this.type = type;
        this.amount = amount;
        this.unit = unit;
        this.timeTaken = timeTaken;
        this.exactTimeTaken = exactTimeTaken;
        this.dateTaken = dateTaken;
    }

    public String getReferenceMedicineId() {
        return referenceMedicineId;
    }

    public void setReferenceMedicineId(String referenceMedicineId) {
        this.referenceMedicineId = referenceMedicineId;
    }

    public String getMedicineSchedule() {
        return medicineSchedule;
    }

    public void setMedicineSchedule(String medicineSchedule) {
        this.medicineSchedule = medicineSchedule;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType2() {
        return type2;
    }

    public void setType2(String type2) {
        this.type2 = type2;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(String timeTaken) {
        this.timeTaken = timeTaken;
    }

    public String getExactTimeTaken() {
        return exactTimeTaken;
    }

    public void setExactTimeTaken(String exactTimeTaken) {
        this.exactTimeTaken = exactTimeTaken;
    }

    public String getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(String dateTaken) {
        this.dateTaken = dateTaken;
    }
}


/*
 Log.d("TAG", "Received Data: size db "+medicationDBValueList.size());
            Log.d("TAG", "Received Data: size db "+medicationDBValueList.get(0).getName());
            Log.d("TAG", "Received Data: size db "+medicationDBValueList.get(0).getParentDescription());
            Log.d("TAG", "Received Data: size db "+medicationDBValueList.get(0).getTime());
            Log.d("TAG", "Received Data: size db "+medicationDBValueList.get(0).getTimeDisplay());
            Log.d("TAG", "Received Data: size db "+medicationDBValueList.get(0).getReferenceMedicineId());
            Log.d("TAG", "Received Data: size db "+medicationDBValueList.get(0).getMedicineSchedule());
 */