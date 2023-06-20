package io.antmedia.rest.model;

import dev.morphia.annotations.Entity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="jwt", description="The basic jwt class for jwt blacklist")
@Entity(value = "jwt")
public class Jwt {
    @ApiModelProperty(value = "the jwt")
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
