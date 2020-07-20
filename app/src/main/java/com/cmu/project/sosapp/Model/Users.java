package com.cmu.project.sosapp.Model;

public class Users {

    String name, id, userid, status;

    public Users(String name, String id, String userid, String status) {
        this.name = name;
        this.id = id;
        this.userid = userid;
        this.status = status;
    }

    public Users() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
