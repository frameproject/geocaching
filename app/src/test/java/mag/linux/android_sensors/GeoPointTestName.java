package mag.linux.android_sensors;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by fcamps on 4/24/17.
 */
public class GeoPointTestName {
    @Test
    public void getName() throws Exception {
        assertTrue(new GeoPoint("name", 0, 0, "info").getLatitude() == 0);
    }

}