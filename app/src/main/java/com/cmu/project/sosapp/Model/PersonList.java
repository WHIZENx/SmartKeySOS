package com.cmu.project.sosapp.Model;

public class PersonList {

    private String id, name, imageURL, personKey, userid;

    public PersonList(String id, String name, String imageURL, String personKey, String userid) {
        this.id = id;
        this.name = name;
        this.imageURL = imageURL;
        this.personKey = personKey;
        this.userid = userid;
    }

    public PersonList() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getPersonKey() {
        return personKey;
    }

    public void setPersonKey(String personKey) {
        this.personKey = personKey;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }
}
