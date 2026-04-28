package com.monitor.health.request;
import java.util.List;

public class UserTelemedikRequest {
    private String deviceUuid;
    private String name;
    private String surname;
    private String userEmail;
    private String phone;
    private String model;
    private String maker;
    private int os;
    private String country;
    private String identificationType;
    private String identificationNumber;
    private String gender;
    private int birthDate;
    private List<String> emergencyContactPersons;
    private String securityQuestion;
    private String securityAnswer;
    private List<String> medicalInfo;
    private List<String> userAllergies;
    private String affiliateNumber;
    private String osVersion;
    private String mutualInsuranceCompany;
    private String addressCity;
    private String addressStreet;
    private String addressPostalCode;
    private String addressLadder;
    private int addressFloor;
    private String addressDoor;

    // Getters and Setters
    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMaker() {
        return maker;
    }

    public void setMaker(String maker) {
        this.maker = maker;
    }

    public int getOs() {
        return os;
    }

    public void setOs(int os) {
        this.os = os;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getIdentificationType() {
        return identificationType;
    }

    public void setIdentificationType(String identificationType) {
        this.identificationType = identificationType;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(int birthDate) {
        this.birthDate = birthDate;
    }

    public List<String> getEmergencyContactPersons() {
        return emergencyContactPersons;
    }

    public void setEmergencyContactPersons(List<String> emergencyContactPersons) {
        this.emergencyContactPersons = emergencyContactPersons;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    public List<String> getMedicalInfo() {
        return medicalInfo;
    }

    public void setMedicalInfo(List<String> medicalInfo) {
        this.medicalInfo = medicalInfo;
    }

    public List<String> getUserAllergies() {
        return userAllergies;
    }

    public void setUserAllergies(List<String> userAllergies) {
        this.userAllergies = userAllergies;
    }

    public String getAffiliateNumber() {
        return affiliateNumber;
    }

    public void setAffiliateNumber(String affiliateNumber) {
        this.affiliateNumber = affiliateNumber;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getMutualInsuranceCompany() {
        return mutualInsuranceCompany;
    }

    public void setMutualInsuranceCompany(String mutualInsuranceCompany) {
        this.mutualInsuranceCompany = mutualInsuranceCompany;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressStreet() {
        return addressStreet;
    }

    public void setAddressStreet(String addressStreet) {
        this.addressStreet = addressStreet;
    }

    public String getAddressPostalCode() {
        return addressPostalCode;
    }

    public void setAddressPostalCode(String addressPostalCode) {
        this.addressPostalCode = addressPostalCode;
    }

    public String getAddressLadder() {
        return addressLadder;
    }

    public void setAddressLadder(String addressLadder) {
        this.addressLadder = addressLadder;
    }

    public int getAddressFloor() {
        return addressFloor;
    }

    public void setAddressFloor(int addressFloor) {
        this.addressFloor = addressFloor;
    }

    public String getAddressDoor() {
        return addressDoor;
    }

    public void setAddressDoor(String addressDoor) {
        this.addressDoor = addressDoor;
    }
}
