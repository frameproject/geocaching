package mag.linux.android_sensors;

/**
 * @author fredericamps@gmail.com - 2017
 *
 * GeoPoint representation
 *
 */

public class GeoPoint {

    private double latitude;
    private double longitude;
    private String name;
    private String info;

    public GeoPoint( String name, double latitude, double longitude, String info) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.info = info;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongiture(double longiture) {
        this.longitude = longiture;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }


    public String toString()
    {
        return name;
    }

}
