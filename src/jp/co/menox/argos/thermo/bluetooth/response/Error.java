package jp.co.menox.argos.thermo.bluetooth.response;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * これは Exception でも Throwable でもありません！
 * 
 * @author Youta
 *
 */
public class Error implements Parcelable {
   
    public Error () {
        // noop
    }
    
    private Error(Parcel source) {
        this.setTransaction(source.readInt());
        this.setError(source.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.getTransaction());
        dest.writeString(this.getError());
    }
    
    public int getTransaction() {
        return transaction;
    }

    public void setTransaction(int transaction) {
        this.transaction = transaction;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    private int transaction;
    private String error;

    public static final Parcelable.Creator<Error> CREATOR = new Creator<Error>() {
        @Override
        public Error[] newArray(int size) {
            return new Error[size];
        }

        @Override
        public Error createFromParcel(Parcel source) {
            return new Error(source);
        }
    };

}
