package com.monitor.health.response.ble;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BleUserData {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("username")
    private String username;

    @SerializedName("phone")
    private String phone;

    @SerializedName("date_of_birth")
    private String dateOfBirth;

    @SerializedName("gender")
    private String gender;

    @SerializedName("state")
    private String state;

    @SerializedName("organization")
    private String organization;

    @SerializedName("sub_organization")
    private String subOrganization;

    @SerializedName("status")
    private String status;

    @SerializedName("enrolled_by")
    private String enrolledBy;

    @SerializedName("practitioners")
    private List<String> practitioners;

    @SerializedName("conditions")
    private List<String> conditions;

    @SerializedName("height")
    private String height;

    @SerializedName("weight")
    private String weight;

    @SerializedName("member_id")
    private String memberId;

    @SerializedName("general_practitioner")
    private String generalPractitioner;

    @SerializedName("primary_insurance_name")
    private String primaryInsuranceName;

    @SerializedName("primary_insurance_policy_id")
    private String primaryInsurancePolicyId;

    @SerializedName("primary_insurance_phone")
    private String primaryInsurancePhone;

    @SerializedName("country")
    private String country;

    @SerializedName("zip_code")
    private String zipCode;

    @SerializedName("complete_address")
    private String completeAddress;

    @SerializedName("home_number")
    private String homeNumber;

    @SerializedName("profile_image_url")
    private String profileImageUrl;

    @SerializedName("angel_support")
    private String angelSupport;

    @SerializedName("glucose_test_rate")
    private String glucoseTestRate;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("measurement_units")
    private MeasurementUnits measurementUnits;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getSubOrganization() { return subOrganization; }
    public void setSubOrganization(String subOrganization) { this.subOrganization = subOrganization; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEnrolledBy() { return enrolledBy; }
    public void setEnrolledBy(String enrolledBy) { this.enrolledBy = enrolledBy; }

    public List<String> getPractitioners() { return practitioners; }
    public void setPractitioners(List<String> practitioners) { this.practitioners = practitioners; }

    public List<String> getConditions() { return conditions; }
    public void setConditions(List<String> conditions) { this.conditions = conditions; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getGeneralPractitioner() { return generalPractitioner; }
    public void setGeneralPractitioner(String generalPractitioner) { this.generalPractitioner = generalPractitioner; }

    public String getPrimaryInsuranceName() { return primaryInsuranceName; }
    public void setPrimaryInsuranceName(String primaryInsuranceName) { this.primaryInsuranceName = primaryInsuranceName; }

    public String getPrimaryInsurancePolicyId() { return primaryInsurancePolicyId; }
    public void setPrimaryInsurancePolicyId(String primaryInsurancePolicyId) { this.primaryInsurancePolicyId = primaryInsurancePolicyId; }

    public String getPrimaryInsurancePhone() { return primaryInsurancePhone; }
    public void setPrimaryInsurancePhone(String primaryInsurancePhone) { this.primaryInsurancePhone = primaryInsurancePhone; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCompleteAddress() { return completeAddress; }
    public void setCompleteAddress(String completeAddress) { this.completeAddress = completeAddress; }

    public String getHomeNumber() { return homeNumber; }
    public void setHomeNumber(String homeNumber) { this.homeNumber = homeNumber; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getAngelSupport() { return angelSupport; }
    public void setAngelSupport(String angelSupport) { this.angelSupport = angelSupport; }

    public String getGlucoseTestRate() { return glucoseTestRate; }
    public void setGlucoseTestRate(String glucoseTestRate) { this.glucoseTestRate = glucoseTestRate; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public MeasurementUnits getMeasurementUnits() { return measurementUnits; }
    public void setMeasurementUnits(MeasurementUnits measurementUnits) { this.measurementUnits = measurementUnits; }
}
