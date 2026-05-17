package com.monitor.health.model.healthscore;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "user_drwatch")
public class UserDrWatch {
    @PrimaryKey(autoGenerate = true)
    private long id;
    public String _id;
    public String prescribeTestRate;
    public boolean isActive;
    public String phone;
    public String memberId;
    public String email;
    public String bday;
    public String gender;
    public String organization;
    public String createdAt;
    public String username;
    public String subOrganization;
    public String fullname;
    public String client;
    public boolean withDevice;
    public String patient_conditions;
    public String practitioners;
    @Ignore
    public List<HealthRating> health_ratings;
    public int allReadingsFound;
    public double overallHealthScore;

    // Fields from ble-devices/user-profile endpoint
    public String firstName;
    public String lastName;
    public String state;
    public String country;
    public String zipCode;
    public String completeAddress;
    public String height;
    public String weight;
    public String profileImageUrl;
    public String status;
    public String generalPractitioner;
    public String primaryInsuranceName;
    public String homeNumber;
    public String angelSupport;

    public UserDrWatch() {}

    public UserDrWatch(String _id, String prescribeTestRate, boolean isActive, String phone, String memberId, String email, String bday, String gender, String organization, String createdAt, String username, String subOrganization, String fullname, String client, boolean withDevice, String patient_conditions, String practitioners, List<HealthRating> health_ratings, int allReadingsFound, double overallHealthScore) {
        this._id = _id;
        this.prescribeTestRate = prescribeTestRate;
        this.isActive = isActive;
        this.phone = phone;
        this.memberId = memberId;
        this.email = email;
        this.bday = bday;
        this.gender = gender;
        this.organization = organization;
        this.createdAt = createdAt;
        this.username = username;
        this.subOrganization = subOrganization;
        this.fullname = fullname;
        this.client = client;
        this.withDevice = withDevice;
        this.patient_conditions = patient_conditions;
        this.practitioners = practitioners;
        this.health_ratings = health_ratings;
        this.allReadingsFound = allReadingsFound;
        this.overallHealthScore = overallHealthScore;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getPrescribeTestRate() {
        return prescribeTestRate;
    }

    public void setPrescribeTestRate(String prescribeTestRate) {
        this.prescribeTestRate = prescribeTestRate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBday() {
        return bday;
    }

    public void setBday(String bday) {
        this.bday = bday;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSubOrganization() {
        return subOrganization;
    }

    public void setSubOrganization(String subOrganization) {
        this.subOrganization = subOrganization;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public boolean isWithDevice() {
        return withDevice;
    }

    public void setWithDevice(boolean withDevice) {
        this.withDevice = withDevice;
    }

    public String getPatient_conditions() {
        return patient_conditions;
    }

    public void setPatient_conditions(String patient_conditions) {
        this.patient_conditions = patient_conditions;
    }

    public String getPractitioners() {
        return practitioners;
    }

    public void setPractitioners(String practitioners) {
        this.practitioners = practitioners;
    }

    public List<HealthRating> getHealth_ratings() {
        return health_ratings;
    }

    public void setHealth_ratings(List<HealthRating> health_ratings) {
        this.health_ratings = health_ratings;
    }

    public int getAllReadingsFound() {
        return allReadingsFound;
    }

    public void setAllReadingsFound(int allReadingsFound) {
        this.allReadingsFound = allReadingsFound;
    }

    public double getOverallHealthScore() {
        return overallHealthScore;
    }

    public void setOverallHealthScore(double overallHealthScore) {
        this.overallHealthScore = overallHealthScore;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCompleteAddress() { return completeAddress; }
    public void setCompleteAddress(String completeAddress) { this.completeAddress = completeAddress; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGeneralPractitioner() { return generalPractitioner; }
    public void setGeneralPractitioner(String generalPractitioner) { this.generalPractitioner = generalPractitioner; }

    public String getPrimaryInsuranceName() { return primaryInsuranceName; }
    public void setPrimaryInsuranceName(String primaryInsuranceName) { this.primaryInsuranceName = primaryInsuranceName; }

    public String getHomeNumber() { return homeNumber; }
    public void setHomeNumber(String homeNumber) { this.homeNumber = homeNumber; }

    public String getAngelSupport() { return angelSupport; }
    public void setAngelSupport(String angelSupport) { this.angelSupport = angelSupport; }

    @Override
    public String toString() {
        return "UserDrWatch{" +
                "_id='" + _id + '\'' +
                ", prescribeTestRate='" + prescribeTestRate + '\'' +
                ", isActive=" + isActive +
                ", phone='" + phone + '\'' +
                ", memberId='" + memberId + '\'' +
                ", email='" + email + '\'' +
                ", bday='" + bday + '\'' +
                ", gender='" + gender + '\'' +
                ", organization='" + organization + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", username='" + username + '\'' +
                ", subOrganization='" + subOrganization + '\'' +
                ", fullname='" + fullname + '\'' +
                ", client='" + client + '\'' +
                ", withDevice=" + withDevice +
                ", patient_conditions='" + patient_conditions + '\'' +
                ", practitioners='" + practitioners + '\'' +
                ", health_ratings=" + health_ratings +
                ", allReadingsFound=" + allReadingsFound +
                ", overallHealthScore=" + overallHealthScore +
                '}';
    }
}
