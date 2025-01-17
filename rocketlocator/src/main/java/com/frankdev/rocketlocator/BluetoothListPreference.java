/*
* Copyright (C) 2013 Fran�ois Girard
* Copyright (C) 2020 Balazs Mihaly | mihu86
*
* This file is part of Rocket Finder.
*
* Rocket Finder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Rocket Finder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with Rocket Finder. If not, see <http://www.gnu.org/licenses/>.*/

package com.frankdev.rocketlocator;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class BluetoothListPreference extends ListPreference {

    private static final String LOG_TAG = "RocketLocator";

    private Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothAdapter.LeScanCallback scanCallback;

    private ListAdapter listAdapter;
    private final LayoutInflater inflater;
    private int selectedIndex;

    public BluetoothListPreference(Context context) {
        super(context);
        inflater = LayoutInflater.from(context);
    }

    public BluetoothListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflater = LayoutInflater.from(context);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        handler = new Handler();
        handler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                                && getSharedPreferences().getBoolean(SettingsActivity.PREF_USE_BLE, false)) {
                            scanCallback =
                                    new BluetoothAdapter.LeScanCallback() {
                                        @Override
                                        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                                            SharedHolder.getInstance()
                                                    .getLogs()
                                                    .v(LOG_TAG, "Found device: " + device.getAddress());
                                            addDevice(
                                                    new BluetoothListPreference.Device(
                                                            device.getAddress(), device.getName()));
                                        }
                                    };
                            bluetoothAdapter.startLeScan(scanCallback);
                        } else {
                            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                                SharedHolder.getInstance()
                                        .getLogs()
                                        .v(LOG_TAG, "device: " + device.getName() + " -- " + device.getAddress());
                                addDevice(new Device(device.getAddress(), device.getName()));
                            }
                        }
                    }
                });

        listAdapter = new ListAdapter();
        builder.setAdapter(
                listAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedIndex = which;

                        /*
                        * Clicking on an item simulates the positive button
                        * click, and dismisses the dialog.
                        */
                        BluetoothListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
                });

        /*
        * The typical interaction for list-based dialogs is to have
        * click-on-an-item dismiss the dialog instead of the user having to
        * press 'Ok'.
        */
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && bluetoothAdapter != null
                && scanCallback != null) {
            bluetoothAdapter.stopLeScan(scanCallback);
        }

        if (positiveResult && listAdapter != null && selectedIndex >= 0) {
            String value = listAdapter.getItem(selectedIndex).getAddress();
            SharedHolder.getInstance().getLogs().d(LOG_TAG, "Selected BT device: " + value);
            if (callChangeListener(value)) {
                setValue(value);
            }
        }
    }

    private void addDevice(Device device) {
        listAdapter.addDevice(device);
        listAdapter.notifyDataSetChanged();
    }

    class ListAdapter extends BaseAdapter {

        private List<Device> devices = new ArrayList<>();

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Device getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        void addDevice(Device device) {
            if (!devices.contains(device) && device.getName() != null && !device.getName().isEmpty()) {
                devices.add(device);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO use ViewHolder
            View view =
                    convertView != null
                            ? convertView
                            : inflater.inflate(R.layout.listitem_device, parent, false);
            final Device device = devices.get(position);
            TextView name = view.findViewById(R.id.device_name);

            if (device.getName() != null && !device.getName().isEmpty()) {
                name.setText(device.getName());
            } else {
                name.setText("N/A");
            }

            TextView address = view.findViewById(R.id.device_address);
            address.setText(device.getAddress());

            return view;
        }
    }

    static class Device {
        private String address;
        private String name;

        Device(String address, String name) {
            this.address = address;
            this.name = name;
        }

        String getAddress() {
            return address;
        }

        String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Device that = (Device) o;

            return address != null ? address.equals(that.address) : that.address == null;
        }

        @Override
        public int hashCode() {
            return address != null ? address.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "DeviceInfo{" + "address='" + address + '\'' + ", name='" + name + '\'' + '}';
        }
    }
}
