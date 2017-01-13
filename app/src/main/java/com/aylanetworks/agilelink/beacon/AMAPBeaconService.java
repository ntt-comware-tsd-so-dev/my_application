

package com.aylanetworks.agilelink.beacon;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.ServerError;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */
/**
 * AMAPBeaconService class is to set scan Regions (both Enter and Exit). This has helper methods
 * to fire actions associated with the Automations. Currently we are scanning Eddystone Beacons
 * and iBeacons.
 */
public class AMAPBeaconService extends Service implements BootstrapNotifier {
    private static final String TAG = "AMAPBeaconService";
    /**
     * The Eddystone-UID frame broadcasts an opaque, unique 16-byte Beacon ID composed of a
     * 10-byte namespace and a 6-byte instance. The Beacon ID may be useful in mapping a device to
     * a record in external storage. The namespace portion of the ID may be used to group a
     * particular set of beacons, while the instance portion of the ID identifies individual devices
     * in the group. The division of the ID into namespace and instance components may also be used
     * to optimize BLE scanning strategies, e.g. by filtering only on the namespace.

     Frame Specification
     The UID frame is encoded in the advertisement as a Service Data block associated with the
     Eddystone service UUID. The layout is:

     Byte offset	Field	Description
     0	Frame Type	Value = 0x00
     1	Ranging Data	Calibrated Tx power at 0 m
     2	NID[0]	10-byte Namespace
     3	NID[1]
     4	NID[2]
     5	NID[3]
     6	NID[4]
     7	NID[5]
     8	NID[6]
     9	NID[7]
     10	NID[8]
     11	NID[9]
     12	BID[0]	6-byte Instance
     13	BID[1]
     14	BID[2]
     15	BID[3]
     16	BID[4]
     17	BID[5]
     18	RFU	Reserved for future use, must be0x00
     19	RFU	Reserved for future use, must be0x00
     All multi-byte values are big-endian.
     */
    public static final String EDDYSTONE_BEACON_LAYOUT = "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19";

    /**
     * iBeacon packet structure

     After 9 bytes of constant preamble, the Proximity UUID, Major and Minor values are transmitted.

     UUID is 16 bytes long, Major and Minor are 2 bytes long. Together they form an ID for your
     iBeacon. Mobile devices recognize which Beacon they approach on the basis of these values.

     The final byte is the packet is used to calculate distance from the iBeacon.
     It represents RSSI value (Received Signal Strength Indication) measured at 1 meter
     from the iBeacon. The value of this byte changes automatically if the user changes the
     transmission power for the iBeacon.

     Kontakt.io iBeacon - Advertising packet structure
     Byte offset	Default value	Description	Properties
     0	0x02	Data length – 2 bytes	constant preamble
     1	0x01	Data type – flags	constant preamble
     2	0x06	LE and BR/EDR flag	constant preamble
     3	0x1a	Data length – 26 bytes	constant preamble
     4	0xff	Data type - manufacturer specific data	constant preamble
     5	0x4c	Manufacturer data	constant preamble
     6	0x00	Manufacturer data	constant preamble
     7	0x02	Manufacturer data	constant preamble
     8	0x15	Manufacturer data	constant preamble
     9	0xf7	Proximity UUID 1st byte	set user UUID
     10	0x82	Proximity UUID 2nd byte	set user UUID
     11	0x6d	Proximity UUID 3rd byte	set user UUID
     12	0xa6	Proximity UUID 4th byte	set user UUID
     13	0x4f	Proximity UUID 5th byte	set user UUID
     14	0xa2	Proximity UUID 6th byte	set user UUID
     15	0x4e	Proximity UUID 7th byte	set user UUID
     16	0x98	Proximity UUID 8th byte	set user UUID
     17	0x80	Proximity UUID 9th byte	set user UUID
     18	0x24	Proximity UUID 10th byte	set user UUID
     19	0xbc	Proximity UUID 11th byte	set user UUID
     20	0x5b	Proximity UUID 12th byte	set user UUID
     21	0x71	Proximity UUID 13th byte	set user UUID
     22	0xe0	Proximity UUID 14th byte	set user UUID
     23	0x89	Proximity UUID 15th byte	set user UUID
     24	0x3e	Proximity UUID 16th byte	set user UUID
     25	xx*	Major 1st byte	set major value
     26	xx*	Major 2nd byte	set major value
     27	xx*	Minor 1st byte	set minor value
     28	xx*	Minor 2nd byte	set minor value
     29	0xb3	Signal power (calibrated RSSI@1m)	signal power value
     */
    public static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    private  static BeaconManager _beaconManager;
    private static AMAPBeaconService _amapBeaconService;
    private static final HashSet<String> _mapBeaconID = new HashSet<>();

    private static final Automation.ALAutomationTriggerType triggerTypeBeaconEnter = Automation
            .ALAutomationTriggerType.TriggerTypeBeaconEnter;
    private static final Automation.ALAutomationTriggerType triggerTypeBeaconExit = Automation
            .ALAutomationTriggerType.TriggerTypeBeaconExit;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _beaconManager = BeaconManager.getInstanceForApplication(this);
        _beaconManager.getBeaconParsers().clear();

        List<BeaconParser> parserList = _beaconManager.getBeaconParsers();
        parserList.add(new BeaconParser().setBeaconLayout(AMAPBeaconService.EDDYSTONE_BEACON_LAYOUT));
        parserList.add(new BeaconParser().setBeaconLayout(AMAPBeaconService.IBEACON_LAYOUT));

        //Scan lasts for SCAN_PERIOD time
        long SCAN_DURATION = 5000;
        _beaconManager.setBackgroundScanPeriod(SCAN_DURATION);

        //Wait every SCAN_PERIOD_IN_BETWEEN time
        long SCAN_BETWEEN_DURATION = 1000 * 30;
        _beaconManager.setBackgroundBetweenScanPeriod(SCAN_BETWEEN_DURATION);
        _beaconManager.setForegroundBetweenScanPeriod(SCAN_BETWEEN_DURATION);
        _amapBeaconService = this;
        fetchAndMonitorBeacons();
        _mapBeaconID.clear();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.d("didEnterRegion", "Got a didEnterRegion call");
        String msgString = "entered region";
        AMAPCore instance = AMAPCore.sharedInstance();
        //Get the Unique id for this region, This is same as the beacon id
        final String id = region.getUniqueId();
        if (instance == null || instance.getSessionManager() == null) {
            addNotification(msgString, id, true);
        } else {
            fireActions(id, true);
        }
    }

    @Override
    public void didExitRegion(Region region) {
        Log.d("didExitRegion", "Got a didExitRegion call");
        String msgString = "exited region";
        AMAPCore instance = AMAPCore.sharedInstance();
        //Get the Unique id for this region, This is same as the beacon id
        final String id = region.getUniqueId();
        if (instance == null || instance.getSessionManager() == null) {
            addNotification(msgString, id, false);
        } else {
            fireActions(id, false);
        }
    }

    @Override
    public void onDestroy() {
        _beaconManager.removeAllMonitorNotifiers();
    }

    /**
     * Checks if this Beacon ID matches Trigger UUID and fires the Actions that were set
     * @param id Unique Region identifier. This is same as the beacon id
     * @param hasEntered has Beacon enetered
     */
    public static void fireActions(final String id, final boolean hasEntered) {
        AutomationManager.fetchAutomation(new Response.Listener<Automation[]>
                () {
            @Override
            public void onResponse(Automation[] response) {
                for (Automation automation : response) {
                    Automation.ALAutomationTriggerType automationTriggerType = automation
                            .getAutomationTriggerType();
                    Automation.ALAutomationTriggerType triggerType = triggerTypeBeaconExit;
                    if (hasEntered) {
                        triggerType = triggerTypeBeaconEnter;
                    }
                    if (automation.isEnabled(MainActivity.getInstance()) && id.equals(automation.getTriggerUUID()) &&
                            triggerType.equals(automationTriggerType)) {
                        HashSet<String> actionSet = new HashSet<>(Arrays.asList(automation
                                .getActions()));
                        setBeaconActions(actionSet);
                        break;
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                //Check if there are no existing automations. This is not an actual error and we
                //don't want to show this error. Just log it in case of no Existing automations
                if (error instanceof ServerError) {
                    ServerError serverError = ((ServerError) error);
                    int code = serverError.getServerResponseCode();
                    if (code == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus()) {
                        Log.d(TAG, "No Existing Automation");
                    } else {
                        Log.e(TAG, "Error in fetch automations " + error.getMessage());
                    }
                } else {
                    Log.e(TAG, "Error in fetch automations " + error.getMessage());
                }
            }
        });
    }

    private void addNotification(String msg, String regionId, boolean entered) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.address_book_icon)
                        .setContentTitle("Notifications")
                        .setContentText(msg);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        Bundle bundle = new Bundle();
        bundle.putBoolean(MainActivity.ARG_TRIGGER_TYPE, entered);
        bundle.putString(MainActivity.REGION_ID, regionId);
        notificationIntent.putExtras(bundle);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
    /**
     * Fetches all the Beacons that are set for Automations for this User. It then sets Beacon
     * Monitoring for these Beacons
     */
    public static void fetchAndMonitorBeacons() {
        AMAPCore instance = AMAPCore.sharedInstance();
        if (instance == null || instance.getSessionManager() == null) {
            return;
        }

        AutomationManager.fetchAutomation(new Response.Listener<Automation[]>
                () {
            @Override
            public void onResponse(Automation[] response) {
                for (Automation automation : response) {
                    Automation.ALAutomationTriggerType automationTriggerType = automation
                            .getAutomationTriggerType();

                    if (automation.isEnabled(MainActivity.getInstance()) &&
                            (triggerTypeBeaconEnter.equals(automationTriggerType) ||
                                    triggerTypeBeaconExit.equals(automationTriggerType))) {
                        String beaconId = automation.getTriggerUUID();
                        if (!_mapBeaconID.contains(beaconId)) {
                            _mapBeaconID.add(beaconId);
                            List<Identifier> listIdentifier = getIdentifiersFromString(beaconId);
                            if(listIdentifier != null) {
                                Region singleBeaconRegion = new Region(beaconId, listIdentifier);
                                if(_amapBeaconService != null) {
                                    new RegionBootstrap(_amapBeaconService, singleBeaconRegion);
                                }
                            }
                        }
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                //Check if there are no existing automations. This is not an actual error and we
                //don't want to show this error. Just log it in case of no Existing automations
                if (error instanceof ServerError) {
                    ServerError serverError = ((ServerError) error);
                    int code = serverError.getServerResponseCode();
                    if (code == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus()) {
                        Log.d(TAG, "No Existing Automation");
                    } else {
                        Log.e(TAG, "Error in fetch automations " + error.getMessage());
                    }
                } else {
                    Log.e(TAG, "Error in fetch automations " + error.getMessage());
                }
            }
        });
    }

    private static void setBeaconActions(final HashSet<String> actionSet) {
        AylaDeviceActions.fetchActions(new Response.Listener<Action[]>() {
            @Override
            public void onResponse(Action[] arrayAction) {
                for (final Action action : arrayAction) {
                    if (action != null && actionSet.contains((action.getId()))) {
                        AylaDevice device = AMAPCore.sharedInstance().getDeviceManager()
                                .deviceWithDSN(action.getDSN());
                        if (device == null) {
                            continue;
                        }
                        final AylaProperty entryProperty = device.getProperty(action.getPropertyName());
                        if (entryProperty == null) {
                            continue;
                        }
                        Object value = action.getValue();
                        entryProperty.createDatapoint(value, null, new Response
                                        .Listener<AylaDatapoint<Integer>>() {
                                    @Override
                                    public void onResponse(final AylaDatapoint<Integer> response) {
                                        String str = "Property Name:" + entryProperty.getName();
                                        str += " value " + action.getValue();
                                        Log.d("setGeofenceActions", "OnEnteredExitedGeofences success: " + str);
                                    }
                                },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(AylaError error) {
                                        Toast.makeText(MainActivity.getInstance(), error.getMessage(), Toast
                                                .LENGTH_LONG).show();
                                    }
                                });
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Toast.makeText(MainActivity.getInstance(), error.getMessage(), Toast
                        .LENGTH_LONG).show();
            }
        });
    }

    private static List<Identifier> getIdentifiersFromString(String idString) {
        if (idString == null) {
            return null;
        }
        List<Identifier> identifierList = new ArrayList<>();
        String ID1 = "id1: ";
        String ID2 = "id2: ";
        String ID3 = "id3: ";
        int index1 = idString.indexOf(ID2);
        if (index1 > 0) {
            String id = idString.substring(ID1.length(), index1);
            id = id.trim();
            Identifier identifier1 = Identifier.parse(id);
            identifierList.add(identifier1);
        } else {
            return identifierList;
        }
        int index2 = idString.indexOf(ID3);
        if (index2 > 0) {
            String id = idString.substring(index1 + ID2.length(), index2);
            id = id.trim();
            Identifier identifier1 = Identifier.parse(id);
            identifierList.add(identifier1);

        } else {
            String id = idString.substring(index1 + ID2.length());
            id = id.trim();
            Identifier identifier1 = Identifier.parse(id);
            identifierList.add(identifier1);
            return identifierList;
        }

        String id = idString.substring(index2 + ID3.length());
        id = id.trim();
        Identifier identifier1 = Identifier.parse(id);
        identifierList.add(identifier1);
        return identifierList;
    }

    /**
     * Stops Monitoring for a Beacon. When a Beacon is deleted this method is called.
     */
    public static void stopMonitoringRegion(String beaconId) {
        if (_mapBeaconID.contains(beaconId)) {
            List<Identifier> listIdentifier = getIdentifiersFromString(beaconId);
            Region singleBeaconRegion = new Region(beaconId, listIdentifier);
            try {
                _beaconManager.stopRangingBeaconsInRegion(singleBeaconRegion);
            } catch (RemoteException ex) {
                Log.e(TAG, "stopMonitoringRegion " + ex.getMessage());
            }
        }
    }
}
