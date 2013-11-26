package jp.co.menox.android.argos.thermo.response;

import android.os.Parcel;
import android.os.Parcelable;

public class Info implements Parcelable {
   
    public Info () {
        // noop
    }
    
    private Info(Parcel source) {
        this.setTransaction(source.readInt());
        this.setName(source.readString());
        this.setId(source.readString());
        this.setUnit(source.readString());
        this.setAccuracy(source.readString());
        this.setLocation(source.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.getTransaction());
        dest.writeString(this.getName());
        dest.writeString(this.getId());
        dest.writeString(this.getUnit());
        dest.writeString(this.getAccuracy());
        dest.writeString(this.getLocation());
    }
    
    public int getTransaction() {
        return transaction;
    }

    public void setTransaction(int transaction) {
        this.transaction = transaction;
    }

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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    private int transaction;
    private String id;
    private String name;
    private String unit;
    private String accuracy;
    private String location;

    public static final Parcelable.Creator<Info> CREATOR = new Creator<Info>() {
        @Override
        public Info[] newArray(int size) {
            return new Info[size];
        }

        @Override
        public Info createFromParcel(Parcel source) {
            return new Info(source);
        }
    };

}
