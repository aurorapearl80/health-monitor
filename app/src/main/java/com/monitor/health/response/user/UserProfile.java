package com.monitor.health.response.user;

public class UserProfile {
    private String _id;
    private String id;
    private String fullname;
    private String username;
    private int age;
    private String dob;
    private String email;
    private String phone;
    private int pin;
    private String city;
    private String state;
    private String country;
    private String gender;
    private String address;
    private String zip;
    private String organization;
    private String subOrganization;
    private String memberId;
    private String height;
    private String weight;
    private String createdAt;
    private String updatedAt;

    // Getters and Setters
    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getPin() {
        return pin;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getSubOrganization() {
        return subOrganization;
    }

    public void setSubOrganization(String subOrganization) {
        this.subOrganization = subOrganization;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "_id='" + _id + '\'' +
                ", id='" + id + '\'' +
                ", fullname='" + fullname + '\'' +
                ", username='" + username + '\'' +
                ", age=" + age +
                ", dob='" + dob + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", pin=" + pin +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", country='" + country + '\'' +
                ", gender='" + gender + '\'' +
                ", address='" + address + '\'' +
                ", zip='" + zip + '\'' +
                ", organization='" + organization + '\'' +
                ", subOrganization='" + subOrganization + '\'' +
                ", memberId='" + memberId + '\'' +
                ", height='" + height + '\'' +
                ", weight='" + weight + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}
