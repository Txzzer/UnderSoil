package com.example.undersoil.network;

import com.example.undersoil.data.db.GardenEntity;
import com.example.undersoil.data.model.IotAlert;
import com.example.undersoil.data.model.ScanLog;
import com.example.undersoil.data.model.Sector;
import com.example.undersoil.data.model.WateringSchedule;

import java.util.Collections;
import java.util.List;

// Response of GET api/sync/all: everything the server holds for the logged-in user.
public class UserDataResponse {

    private List<Sector> sectors;
    private List<GardenEntity> garden;
    private List<ScanLog> scanLogs;
    private List<IotAlert> alerts;
    private List<WateringSchedule> schedules;

    public List<Sector> getSectors() {
        return orEmpty(sectors);
    }

    public List<GardenEntity> getGarden() {
        return orEmpty(garden);
    }

    public List<ScanLog> getScanLogs() {
        return orEmpty(scanLogs);
    }

    public List<IotAlert> getAlerts() {
        return orEmpty(alerts);
    }

    public List<WateringSchedule> getSchedules() {
        return orEmpty(schedules);
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
