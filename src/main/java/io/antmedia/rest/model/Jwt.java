package io.antmedia.rest.model;

public class Jwt {
    private String jwt;

    // Default constructor
    public Jwt() {
    }

    // Constructor with jwt parameter
    public Jwt(String jwt) {
        this.jwt = jwt;
    }

    // Getter and setter for jwt
    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
