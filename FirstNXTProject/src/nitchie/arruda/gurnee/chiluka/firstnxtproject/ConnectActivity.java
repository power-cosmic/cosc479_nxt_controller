package nitchie.arruda.gurnee.chiluka.firstnxtproject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ConnectActivity extends Activity implements OnClickListener {

	private final String TAG = "NXT Project 1";
	private final double MAX_MILLI_VOLTS = 9000.0;

	// UI Components
	Button connectButton;
	Button disconnectButton;
	ImageView btImage;
	TextView statusLabel;
	ProgressBar batteryStatus;

	// Bluetooth Variables
	private BluetoothDevice bd;
	private BluetoothSocket socket;
	private InputStream is;
	private OutputStream os;
	private final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

	private final int PICK_BLUETOOTH_ID = 1;

	boolean flag = false;

	int mpower1 = 20;
	int mpower2 = 30;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect_view);

		connectButton = (Button) this.findViewById(R.id.connectButton);
		connectButton.setOnClickListener(this);

		disconnectButton = (Button) this.findViewById(R.id.disconnectButton);
		disconnectButton.setOnClickListener(this);
		disconnectButton.setVisibility(View.GONE);

		btImage = (ImageView) findViewById(R.id.imageView1);
		btImage.setImageAlpha(50);

		statusLabel = (TextView) findViewById(R.id.statusLabel);
		batteryStatus = (ProgressBar) findViewById(R.id.progressBar1);
		batteryStatus.setIndeterminate(false);
		batteryStatus.setMax(100);
		batteryStatus.setProgress(100);
	}

	private int getBatteryLevel() {
		byte[] response = new byte[7];
		try {

			byte[] buffer = new byte[4];

			// request battery level
			buffer[0] = 2; // length lsb
			buffer[1] = 0; // length msb
			buffer[2] = 0x00; // actual
			buffer[3] = 0x0B; // message

			os.write(buffer);
			os.flush();

			// receive battery level
			response[0] = (byte) is.read(); // length lsb
			response[1] = (byte) is.read(); // length msb
			response[2] = (byte) is.read(); // will be 2
			response[3] = (byte) is.read(); // will be 11 -> 0x0B
			response[4] = (byte) is.read(); // Status byte. 0 = successful.
			response[5] = (byte) is.read(); // battery level lsb
			response[6] = (byte) is.read(); // bettery level msb

		} catch (Exception e) {
			Log.e(TAG, "Error getting battery level(" + e.getMessage() + ")");
			return -1;
		}

		// converting unsigned word to an int
		int responseVoltage = (0xFF & response[5])
				| ((0xFF & response[6]) << 8);

		return responseVoltage;
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case (R.id.connectButton):
			Intent i = new Intent(this, PopupActivity.class);
			this.startActivityForResult(i, PICK_BLUETOOTH_ID);
			break;
		case (R.id.disconnectButton):
			disconnectNXT(v);
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_BLUETOOTH_ID) {
			if (resultCode == RESULT_OK) {
				this.connectToDevice();
			}
		}
	}

	public void connectToDevice() {
		try {
			DeviceData myObject = (DeviceData) DeviceData.getInstance();
			bd = myObject.getBt();

			socket = bd.createRfcommSocketToServiceRecord(UUID
					.fromString(this.SPP_UUID));
			socket.connect();
		} catch (IOException e) {
			Log.e(TAG,
					"Error interacting with remote device -> " + e.getMessage());
			return;
		}

		try {
			is = socket.getInputStream();
			os = socket.getOutputStream();

			DeviceData myObject = (DeviceData) DeviceData.getInstance();
			myObject.setIs(is);
			myObject.setOs(os);

		} catch (IOException e) {
			is = null;
			os = null;
			disconnectNXT(null);
			return;
		}

		connectButton.setVisibility(View.GONE);
		disconnectButton.setVisibility(View.VISIBLE);
		setBatteryMeter(getBatteryLevel());
		btImage.setImageAlpha(255);
		statusLabel.setText(R.string.nxtConnected);

		Log.i(TAG, "Connected with " + bd.getName());
	}

	private void setBatteryMeter(int voltage) {
		double batteryLevel = voltage / this.MAX_MILLI_VOLTS;
		int batteryProgress = (int) (batteryLevel * 100);
		batteryStatus.setProgress(batteryProgress);
	}

	public void disconnectNXT(View v) {
		try {
			Log.i(TAG, "Attempting to break BT connection of " + bd.getName());
			socket.close();
			is.close();
			os.close();
			Log.i(TAG, "BT connection of " + bd.getName() + " is disconnected");
		} catch (Exception e) {
			Log.e(TAG, "Error in disconnect -> " + e.getMessage());
		}

		connectButton.setVisibility(View.VISIBLE);
		disconnectButton.setVisibility(View.GONE);
		btImage.setImageAlpha(100);
		statusLabel.setText(R.string.nxtDisconnected);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
