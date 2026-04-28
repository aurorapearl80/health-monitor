package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.monitor.health.model.BleDeviceModel;

import java.util.List;

@Dao
public interface BleDeviceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(BleDeviceModel bleDeviceModel);

    /**
     * Get device by serial - CASE INSENSITIVE
     * Handles uppercase/lowercase serial number variations
     */
    @Query("SELECT * FROM ble_device WHERE LOWER(serial) = LOWER(:serial) LIMIT 1")
    BleDeviceModel getBySerial(String serial);

    /**
     * Get device by deviceId - CASE INSENSITIVE
     * Handles uppercase/lowercase deviceId variations
     */
    @Query("SELECT * FROM ble_device WHERE LOWER(deviceId) = LOWER(:deviceId) LIMIT 1")
    BleDeviceModel getByDevice(String deviceId);


    /**
     * Update device by serial - CASE INSENSITIVE
     */
    @Query("UPDATE ble_device SET deviceId = :deviceId, serverId = :serverId,  isConnected = :isConnected WHERE LOWER(serial) = LOWER(:serial)")
    int updateBySerial(String serial, String deviceId, String serverId, boolean isConnected);
    
    

    /**
     * Update connection status - CASE INSENSITIVE
     */
    @Query("UPDATE ble_device SET isConnected = :isConnected WHERE LOWER(serial) = LOWER(:serial)")
    int updateConnectionStatus(String serial, boolean isConnected);

    /**
     * Update connection status by device name - CASE INSENSITIVE
     */
    @Query("UPDATE ble_device SET isConnected = :isConnected WHERE LOWER(deviceName) = LOWER(:deviceName)")
    int updateConnectionStatusByDeviceName(String deviceName, boolean isConnected);


    /**
     * Update device by deviceId - CASE INSENSITIVE (without changing serial)
     */
    @Query("UPDATE ble_device SET serverId = :serverId, isConnected = :isConnected WHERE LOWER(deviceId) = LOWER(:deviceId)")
    int updateByDeviceId(String deviceId, String serverId, boolean isConnected);

    /**
     * Get all devices
     */
    @Query("SELECT * FROM ble_device")
    List<BleDeviceModel> getAllBleDevices();

    /**
     * Get connected devices
     */
    @Query("SELECT * FROM ble_device WHERE isConnected = 1")
    List<BleDeviceModel> getConnectedDevices();
    
    

    /**
     * Get disconnected devices
     */
    @Query("SELECT * FROM ble_device WHERE isConnected = 0")
    List<BleDeviceModel> getDisconnectedDevices();

    /**
     * Delete by id
     */
    @Query("DELETE FROM ble_device WHERE id = :id")
    void deleteById(long id);

    /**
     * Delete by serial - CASE INSENSITIVE
     */
    @Query("DELETE FROM ble_device WHERE LOWER(serial) = LOWER(:serial)")
    void deleteBySerial(String serial);

    /**
     * Delete all
     */
    @Query("DELETE FROM ble_device")
    void deleteAll();

    /**
     * Get count
     */
    @Query("SELECT COUNT(*) FROM ble_device")
    int getCount();


    /**
     * Get server ID by serial - CASE INSENSITIVE
     */
    @Query("SELECT serverId FROM ble_device WHERE LOWER(serial) = LOWER(:serial) LIMIT 1")
    String getServerIdBySerial(String serial);

    /**
     * Reset all connection statuses to not connected (isConnected = 0)
     */
    @Query("UPDATE ble_device SET isConnected = 0")
    int resetAllConnections();
}