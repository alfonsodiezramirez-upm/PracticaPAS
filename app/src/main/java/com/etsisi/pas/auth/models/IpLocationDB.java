package com.etsisi.pas.auth.models;

public class IpLocationDB {
    private String user;
    private String fecha;
    private Iplocation iplocation;

    public IpLocationDB(String user, String fecha, Iplocation iplocation) {
        this.user = user;
        this.fecha = fecha;
        this.iplocation = iplocation;
    }

    public Iplocation getIplocation() {
        return iplocation;
    }

    public void setIplocation(Iplocation iplocation) {
        this.iplocation = iplocation;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
