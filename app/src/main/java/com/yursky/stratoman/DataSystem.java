package com.yursky.stratoman;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaScannerConnection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;

@SuppressLint("UseSparseArrays")
class DataSystem {

    private HashMap<Integer, DeviceCategory> deviceCategories = new HashMap<>();

    private String dataDir;
    private String writeFileName;
    private String openedFileName;

    DataSystem(String dataDir) {
        this.dataDir = dataDir;
    }

    DataPoint parseData(String data) throws Exception {
        if (data.startsWith("[") && data.endsWith("]")) {
            return new SuitNavPoint(data);

        } else if (data.startsWith("{") && data.endsWith("}")) {
            return new SuitInfoPoint(data);

        } else if (data.startsWith("N:")) {
            return new SuitNtypePoint(data);
        }

        throw new Exception();
    }


    String getStatus() {

        int deviceCategoriesNumber = deviceCategories.size();
        int devicesNumber = 0;
        int pointsNumber = 0;

        for (DeviceCategory deviceCategory : deviceCategories.values()) {
            devicesNumber += deviceCategory.devices.size();

            for (Device device : deviceCategory.devices.values()) {
                pointsNumber += device.dataPoints.size();
            }
        }

        return " \nDevice_categories: " + deviceCategoriesNumber + "\nDevices: " + devicesNumber + "\nPoints: " + pointsNumber;

    }

    DeviceCategory getDeviceCategory(int deviceCategory) {
        return deviceCategories.get(deviceCategory);
    }

    void addDeviceCategory(int deviceCategory) {
        if (getDeviceCategory(deviceCategory) == null)
            deviceCategories.put(deviceCategory, new DeviceCategory(deviceCategory));
    }

    Device getDevice(int deviceCategory, int deviceId) {
        return getDeviceCategory(deviceCategory).getDevice(deviceId);
    }

    void addDevice(int deviceCategory, int deviceId) {
        addDeviceCategory(deviceCategory);
        getDeviceCategory(deviceCategory).addDevice(deviceId);
    }

    DataPoint getDataPoint(int deviceCategory, int deviceId, Calendar date) {
        return getDevice(deviceCategory, deviceId).getDataPoint(date);
    }

    void addDataPoint(DataPoint dataPoint) {
        addDevice(dataPoint.deviceCategory, dataPoint.deviceId);
        getDevice(dataPoint.deviceCategory, dataPoint.deviceId).addDataPoint(dataPoint);
    }

    void readData(String openFileShort) {
        clear();
        openedFileName = openFileShort;

        try {
            FileReader fileReader = new FileReader(dataDir + openFileShort);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            //Read by line
            while ((line = bufferedReader.readLine()) != null) {
                DataPoint dataPoint = null;
                try {
                    dataPoint = parseData(line);
                    addDataPoint(dataPoint);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            bufferedReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void writeData(Context context, String data) {
        File dir = new File(dataDir);
        if (dir.mkdirs())
            MediaScannerConnection.scanFile(context, new String[]{dataDir}, null, null);

        if (writeFileName == null) createWriteFile(context);

        try {
            //Write data to file
            File file = new File(dataDir + writeFileName);
            file.createNewFile();

            FileWriter fileWriter = new FileWriter(file, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(data + System.lineSeparator());
            bufferedWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //Save lastPointDate to preference
        Util.savePreferences("lastDateInfo",
                writeFileName.replace(".txt", "") + ">" +
                        new SimpleDateFormat("dd.MM.yy_HH-mm", Locale.US).format(Calendar.getInstance().getTime()),
                context);

        MediaScannerConnection.scanFile(context, new String[]{dataDir + writeFileName}, null, null);
    }

    /**
     * @return true - if new file will be created, false - if previous file exists and data will be written to it
     */
    private boolean createWriteFile(Context context) {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();

        String lastDateInfo = Util.loadPreferences("lastDateInfo", context);

        if (!lastDateInfo.equals("")) {
            //Check last point date, create new writeFileName if needed
            String[] lastDateValues = lastDateInfo.split(">");
            String lastDataFile = lastDateValues[0];
            String lastPoint = lastDateValues[1];

            if (writeFileName == null) {
                try {
                    Date lastPointDate = new SimpleDateFormat("dd.MM.yy_HH-mm", Locale.US).parse(lastPoint);

                    calendar.add(Calendar.MINUTE, -5);

                    if (calendar.getTime().before(lastPointDate)) {
                        writeFileName = lastDataFile + ".txt";
                        File file = new File(dataDir + writeFileName);

                        if (file.exists()) return false;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //If no last date, or old last file, or last file deleted - create new file
        writeFileName = new SimpleDateFormat("dd.MM.yy_HH-mm", Locale.US).format(now) + ".txt";
        return true;
    }

    void clear() {
        deviceCategories.clear();
    }

    class DeviceCategory {

        private HashMap<Integer, Device> devices = new HashMap<>();
        int deviceCategory;

        DeviceCategory(int deviceCategory) {
            this.deviceCategory = deviceCategory;
        }

        static final int SUIT_NAV = 100;
        static final int SUIT_INFO = 101;
        static final int SUIT_NTYPE = 102;

        Device getDevice(int deviceId) {
            return devices.get(deviceId);
        }

        void addDevice(int deviceId) {
            if (getDevice(deviceId) == null) devices.put(deviceId, new Device(deviceId));
        }
    }


    class Device {

        private TreeMap<Calendar, DataPoint> dataPoints = new TreeMap<>();
        int deviceId;

        Device(int deviceId) {
            this.deviceId = deviceId;
        }

        DataPoint getDataPoint(Calendar date) {
            return dataPoints.get(date);
        }

        void addDataPoint(DataPoint dataPoint) {
            dataPoints.put(dataPoint.date, dataPoint);
        }

        DataPoint getLastDataPoint() {
            return dataPoints.lastEntry().getValue();
        }
    }


    class DataPoint {

        int deviceCategory;
        int deviceId;
        Calendar date;

        String getStringValue(String data, String startCharEx, String endCharEx) {
            data = data.substring(data.indexOf(startCharEx), data.indexOf(endCharEx));
            return data.replace(startCharEx, "");
        }
    }


    class SuitNavPoint extends DataPoint {

        SuitNavPoint(String data) throws Exception {
            deviceCategory = DeviceCategory.SUIT_NAV;
            date = Calendar.getInstance();
        }
    }


    class SuitInfoPoint extends DataPoint {

        SuitInfoPoint(String data) throws Exception {
            deviceCategory = DeviceCategory.SUIT_INFO;
            date = Calendar.getInstance();
        }
    }


    class SuitNtypePoint extends DataPoint {
        int secFromStart;
        int mpStage;
        int mpAlt;
        float mpVSpeed;
        float mpAvgVSpeed;
        float baroPress;
        int baroAlt;
        float baroTemp;
        double gpsLat = 0;
        double gpsLon = 0;
        double homeLat = 0;
        double homeLon = 0;
        float gpsHdop = 0;
        int gpsAlt = 0;
        int gpsDist = 0;
        int gpsHSpeed = 0;
        int gpsCourse = 0;
        float criusTemp = 0;
        float airTankTemp = 0;
        float airFlowTemp = 0;
        float chestTemp = 0;
        float underarmTemp = 0;
        float shoulderTemp = 0;
        float externalTemp = 0;
        float voltage5 = 0;
        float voltage12 = 0;
        float airTankPress = 0;
        float suitPress = 0;
        float suitPressO2 = 0;
        float suitPressCO2 = 0;
        String relayStatus;

        SuitNtypePoint(String data) throws Exception {
//            deviceId = 0;
            deviceCategory = DeviceCategory.SUIT_NTYPE;
            date = Calendar.getInstance();

            String info = getStringValue(data, "T:", ";MP.Stage").replace("s", "");
            secFromStart = Integer.valueOf(info.split("m")[0]) * 60 + Integer.valueOf(info.split("m")[1]);

            mpStage = Integer.valueOf(getStringValue(data, "MP.Stage:", ";MP.Alt"));
            mpAlt = Integer.valueOf(getStringValue(data, "MP.Alt:", ";MP.VSpeed"));
            mpVSpeed = Float.valueOf(getStringValue(data, "MP.VSpeed:", ";MP.AvgVSpeed"));
            mpAvgVSpeed = Float.valueOf(getStringValue(data, "MP.AvgVSpeed:", ";Baro.Press"));
            baroPress = Float.valueOf(getStringValue(data, "Baro.Press:", ";Baro.Alt")) / 1000f;    //bar
            baroAlt = Integer.valueOf(getStringValue(data, "Baro.Alt:", ";Baro.Temp"));
            baroTemp = Float.valueOf(getStringValue(data, "Baro.Temp:", ";GPS.Coord"));

            gpsHdop = Float.valueOf(getStringValue(data, "GPS.HDOP:", ";GPS.Alt"));

            if (gpsHdop != 99.99f) {
                String gpsInfo = getStringValue(data, "GPS.Coord:", ";GPS.Home");
                String latitude = gpsInfo.split(",")[0];
                String longitude = gpsInfo.split(",")[1];
                gpsLat = getCoordFromString(latitude);
                gpsLon = getCoordFromString(longitude);

                gpsInfo = getStringValue(data, "GPS.Home:", ";GPS.HDOP");
                latitude = gpsInfo.split(",")[0];
                longitude = gpsInfo.split(",")[1];
                homeLat = getCoordFromString(latitude);
                homeLon = getCoordFromString(longitude);

                gpsAlt = Integer.valueOf(getStringValue(data, "GPS.Alt:", ";GPS.Dst"));
                gpsDist = Integer.valueOf(getStringValue(data, "GPS.Dst:", ";GPS.HSpeed"));
                gpsHSpeed = Integer.valueOf(getStringValue(data, "GPS.HSpeed:", ";GPS.Course"));
                gpsCourse = Integer.valueOf(getStringValue(data, "GPS.Course:", ";GPS.Time"));

                gpsInfo = getStringValue(data, "GPS.Time:", ";DS.Temp");
                gpsInfo = gpsInfo.replace("h", ":").replace("m", ":").replace("s", "").replace("GPS.Date:", "");
                Date gpsDate = new SimpleDateFormat("HH:mm:ss;dd.MM.yyyy", Locale.US).parse(gpsInfo);
                int timeZone = TimeZone.getDefault().getOffset(gpsDate.getTime());
                date.setTimeInMillis(gpsDate.getTime() + timeZone);
            }

            String tempInfo = getStringValue(data, "DS.Temp:", ";Volt");
            if (!tempInfo.equals("none")) {
                String[] temps = tempInfo.split(",");

                for (String temp : temps) {
                    float tempValue = Float.valueOf(temp.substring(temp.indexOf("=") + 1));

                    if (temp.contains("[e6]")) criusTemp = tempValue;
                    else if (temp.contains("[d6]")) externalTemp = tempValue;
                    else if (temp.contains("[02]")) airTankTemp = tempValue;
                    else if (temp.contains("[44]")) airFlowTemp = tempValue;
                    else if (temp.contains("[7b]")) chestTemp = tempValue;
                    else if (temp.contains("[7e]")) underarmTemp = tempValue;
                    else if (temp.contains("[fb]")) shoulderTemp = tempValue;
                }
            }

            String sensorInfo = getStringValue(data, "Volt:", ";Relays");
            String[] values = sensorInfo.split(",");

            voltage5 = Float.valueOf(values[0]);
            voltage12 = Float.valueOf(values[1]);
            airTankPress = (Float.valueOf(values[7]) - 3.02f) / 0.0714f; //bar

            relayStatus = data.substring(data.indexOf("Relays:") + 7).replace(";", "");
        }

        double getCoordFromString(String data) {
            return (double) (Integer.valueOf(getStringValue(data, data.substring(0, 1), "d")) +
                    Integer.valueOf(getStringValue(data, "d", "m")) / 60f +
                    Integer.valueOf(getStringValue(data, "m", "s")) / 3600f);
        }
    }
}
