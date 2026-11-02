package com.project.back_end.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Login {

    @JsonAlias("email")
    private String identifier;
    private String password;

    public Login() {

    }

    public String getIdentifier() {
        return identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}