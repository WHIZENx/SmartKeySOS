package com.cmu.project.sosapp.Model;

public class Sos {

    String from, to, fromname, latitude, longitude, userid, status, sosKey, check, sent;

    public Sos(String from, String to, String fromname, String check, String latitude, String longitude, String userid, String status, String sosKey, String sent) {
        this.from = from;
        this.to = to;
        this.fromname = fromname;
        this.check = check;
        this.latitude = latitude;
        this.longitude = longitude;
        this.userid = userid;
        this.status = status;
        this.sosKey = sosKey;
        this.sent = sent;
    }

    public Sos() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
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

    public String getSosKey() {
        return sosKey;
    }

    public void setSosKey(String sosKey) {
        this.sosKey = sosKey;
    }

    public String getCheck() {
        return check;
    }

    public void setCheck(String check) {
        this.check = check;
    }

    public String getFromname() {
        return fromname;
    }

    public void setFromname(String fromname) {
        this.fromname = fromname;
    }

    public String getSent() {
        return sent;
    }

    public void setSent(String sent) {
        this.sent = sent;
    }
}
