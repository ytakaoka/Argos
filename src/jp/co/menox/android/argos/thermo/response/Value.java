package jp.co.menox.android.argos.thermo.response;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 測定結果
 * @author Youta
 *
 */
public class Value implements Parcelable {
   
    public Value () {
        // noop
    }
    
    private Value(Parcel source) {
        this.setTransaction(source.readInt());
        this.setName(source.readString());
        this.setId(source.readString());
        this.setTime(source.readLong());
        this.setValue(source.readDouble());
        this.setUnit(source.readString());
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
        dest.writeLong(this.getTime());
        dest.writeDouble(this.getValue());
        dest.writeString(this.getUnit());
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getTimeAsString() {
        return TIME_FORMAT.format(new Date(time));
    }

    /**
     * @param time yyyyMMdd/mmHHss.SS フォーマットの日付文字列
     * @throws ParseException 上記のパターンでない場合
     */
    public void setTimeAsString(String time) throws ParseException {
        this.time = TIME_FORMAT.parse(time).getTime();
    }
    
    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    private int transaction;
    private String id;
    private String name;
    private long time;
    private double value;
    private String unit;
    
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("yyyyMMdd/HHmmss.SS", Locale.JAPAN);

    public static final Parcelable.Creator<Value> CREATOR = new Creator<Value>() {
        @Override
        public Value[] newArray(int size) {
            return new Value[size];
        }

        @Override
        public Value createFromParcel(Parcel source) {
            return new Value(source);
        }
    };

}
