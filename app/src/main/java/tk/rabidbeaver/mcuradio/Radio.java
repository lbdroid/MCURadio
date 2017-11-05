package tk.rabidbeaver.mcuradio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Radio extends Activity {

	LocalSocket mSocket;
	DataInputStream is;
	DataOutputStream os;

	private RadioDB rdb;
	private Button channelbtn;
	private ImageButton togglefav;
	//TODO private TextView radiotext;
	//TODO private TextView programservice;

	protected static int lastFreqAM = 1010;
	protected static int lastFreqFM = 9310;

	private Channel[] favs;
	private boolean lastChannelIsFav = false;

	int freq = 1010;

	private LinearLayout favorites;
	
	protected static boolean band_fm = false;

	private Button amfmbtn;
	
	private EditText channelinput;
	private Button dialogband;

	private void setFreq(final int freq){
		if (freq > 0){
			runOnUiThread(new Runnable() {
				public void run() {
					String value = (band_fm?Float.toString(((float)freq)/100):Integer.toString(freq)) + (band_fm?" MHz":" KHz");
					channelbtn.setText(value);
				}
			});
		}
	}

	private void tune(int frequency, boolean fm){
		Log.d("RADIOTUNE", "FREQ: "+frequency+", FM: "+fm);
		freq = frequency;
		band_fm = fm;

		byte band[] = {(byte)0xaa, 0x55, 0x02, 0x01, (byte)(band_fm?0x00:0x01), (byte)(band_fm?0x03:0x02)};
		byte freq[] = {(byte)0xaa, 0x55, 0x03, 0x02, 0x00, 0x00, 0x00};
		freq[4] = (byte)(frequency / 256 % 0x100);
		freq[5] = (byte)(frequency % 0x100);
		freq[6] = (byte)(freq[2] ^ freq[3] ^ freq[4] ^ freq[5]);
		try {
			os.write(band, 0, 6);
			os.write(freq, 0, 7);
		} catch (IOException e){
			e.printStackTrace();
		}

		if (band_fm) lastFreqFM = frequency;
		else lastFreqAM = frequency;

		rdb.setLastFreq(lastFreqAM, lastFreqFM, fm);

		if (rdb.isFavorite(new Channel(frequency, fm))){
			lastChannelIsFav = true;
			togglefav.setImageResource(R.drawable.ic_star_selected);
		} else {
			lastChannelIsFav = false;
			togglefav.setImageResource(R.drawable.ic_star);
		}

		amfmbtn.setText(band_fm?"FM":"AM");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_radio);
		rdb = new RadioDB(this);
		lastFreqAM = rdb.getLastAM();
		Log.d("RADIODB", "LASTFM:"+lastFreqFM);
		lastFreqFM = rdb.getLastFM();
		Log.d("RADIODB", "LASTFM:"+lastFreqFM);
		band_fm = rdb.isLastBandFM();
		if (band_fm) freq = lastFreqFM;
		else freq = lastFreqAM;

		//channel = (Button) this.findViewById(R.id.channelline);
		//TODO radiotext = findViewById(R.id.radiotextline);
		//TODO programservice = findViewById(R.id.programserviceline);
		
		channelbtn = findViewById(R.id.channelline);
		channelbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(Radio.this);
				builder.setTitle("Enter New Channel:");
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   //TODO: add some format checking....

						   tune(Integer.parseInt(channelinput.getText().toString()), dialogband.getText().toString().contentEquals("FM"));

			        	   dialog.dismiss();
			           }
			       });
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
			           }
			       });

				AlertDialog dialog = builder.create();
				
				LayoutInflater inflater=Radio.this.getLayoutInflater();
				View layout=inflater.inflate(R.layout.dialog_channel,(ViewGroup)getWindow().getDecorView().findViewById(R.layout.activity_radio));
				dialog.setView(layout);
				channelinput = layout.findViewById(R.id.channel);
				dialogband = layout.findViewById(R.id.dialog_amfm);
				//channelinput.append(lastChannel.split(" ")[0]);
				dialogband.setText(band_fm?"FM":"AM");
				dialogband.setOnClickListener(new Button.OnClickListener(){
					@Override
					public void onClick(View v) {
						String am = "AM";
						String fm = "FM";
						if (dialogband.getText().toString().equalsIgnoreCase("AM")) dialogband.setText(fm);
						else dialogband.setText(am);
					}
				});
				dialog.show();
			}
		});

		String amfm = Integer.toString(freq) + (band_fm?" FM":" AM");
		channelbtn.setText(amfm);
		
		amfmbtn = findViewById(R.id.amfmbtn);
		amfmbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				if (band_fm){
					tune(lastFreqAM, !band_fm);
				} else {
					tune(lastFreqFM, !band_fm);
				}
			}
		});
		
		togglefav = findViewById(R.id.favorite);
		togglefav.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v) {
				if (lastChannelIsFav){
					((ImageButton) v).setImageResource(R.drawable.ic_star);
					lastChannelIsFav = false;
					rdb.setFav(new Channel(freq, band_fm), false);
				} else {
					((ImageButton) v).setImageResource(R.drawable.ic_star_selected);
					lastChannelIsFav = true;
					rdb.setFav(new Channel(freq, band_fm), true);
				}
				refreshfav();
			}
		});
		
		Button seekupbtn = findViewById(R.id.seekup);
		seekupbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				byte seek[] = {(byte)0xaa, 0x55, 0x02, 0x04, 0x01, 0x07};
				try {
					os.write(seek, 0, 6);
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		});
		
		Button seekdownbtn = findViewById(R.id.seekdown);
		seekdownbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				byte seek[] = {(byte)0xaa, 0x55, 0x02, 0x04, 0x00, 0x06};
				try {
					os.write(seek, 0, 6);
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		});
		
		Button tuneupbtn = findViewById(R.id.tuneup);
		tuneupbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				if (band_fm) {
					lastFreqFM += 10;
					tune(lastFreqFM, band_fm);
				} else {
					lastFreqAM += 10;
					tune(lastFreqAM, band_fm);
				}
			}
		});
		
		Button tunedownbtn = findViewById(R.id.tunedown);
		tunedownbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				if (band_fm) {
					lastFreqFM -= 10;
					tune(lastFreqFM, band_fm);
				} else {
					lastFreqAM -= 10;
					tune(lastFreqAM, band_fm);
				}
			}
		});

		mSocket = new LocalSocket();

		try {
			mSocket.connect(new LocalSocketAddress("/dev/car/radio", LocalSocketAddress.Namespace.FILESYSTEM));
			is = new DataInputStream(mSocket.getInputStream());
			os = new DataOutputStream(mSocket.getOutputStream());
		} catch (Exception e){
			e.printStackTrace();
			finish();
		}

		favorites = findViewById(R.id.favorites);
		refreshfav();
		tune(band_fm?lastFreqFM:lastFreqAM, band_fm);

		new Thread() {
			@Override
			public void run() {
				int len, i, cs;
				byte s1, s2;
				try {
					while(true) {
						if (is.readByte() != (byte)0xaa) continue;
						if (is.readByte() != 0x55) continue;
						s1 = is.readByte();
						s2 = is.readByte();
						len = (int)s1*0x100 + s2;

						byte[] data = new byte[len+1];
						cs = 0;
						for (i=0; i<len; i++){
							data[i] = is.readByte();
							cs ^= data[i];
						}

						//if (cs != is.readByte()) continue;

						Log.d("Radio", "Instruction: "+Integer.toHexString(data[0]));
						switch(data[0]){
							case 0x01: // BAND
								break;
							case 0x02: // FREQ
								int f1 = data[1];
								if (f1 < 0) f1+=256;
								int f2 = data[2];
								if (f2 < 0) f2+=256;
								int f3 = data[3];
								if (f3 < 0) f3+=256;
								int freq = f1*0x10000 + f2*0x100 + f3;
								setFreq(freq);
								break;
							case 0x03: // AREA
								break;
							case 0x04: // RDS STAT
								break;
							case 0x05: // PTY ID
								break;
							case 0x06: // LOC
								break;
							case 0x09: // POWER
								break;
							case 0x0a: // RDS ON
								break;
							default:
								Log.d("Radio", "Unhandled: "+Integer.toHexString(data[0]));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	public void refreshfav(){
		favorites.removeAllViews();
		favs = rdb.getAllFavorites();
		for (int i=0; i<favs.length; i++){
			Button b = new Button(Radio.this);
			b.setId(i);
			b.setOnClickListener(new Button.OnClickListener(){
				@Override
				public void onClick(View v) {
					int favidx = v.getId();

					tune(favs[favidx].frequency, favs[favidx].fm);
				}
			});
			b.setOnLongClickListener(new Button.OnLongClickListener(){
				@Override
				public boolean onLongClick(final View v) {
					//final String channel = ((Button)v).getText().toString();
					AlertDialog.Builder builder = new AlertDialog.Builder(Radio.this);
					builder.setTitle("Move Favorite:");
					builder.setPositiveButton("Right -->", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   rdb.moveFav(favs[v.getId()], true);
				        	   refreshfav();
				        	   dialog.dismiss();
				           }
				       });
					builder.setNeutralButton("Cancel",  new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   dialog.dismiss();
				           }
				       });
					builder.setNegativeButton("<-- Left", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   rdb.moveFav(favs[v.getId()], false);
				        	   refreshfav();
				        	   dialog.dismiss();
				           }
				       });

					AlertDialog dialog = builder.create();
					dialog.show();
					return false;
				}
			});
			String favname;
			if (favs[i].fm)
				favname = Float.toString(((float) favs[i].frequency) / 100) + " FM";
			else
				favname=favs[i].frequency+" AM";
			b.setText(favname);
			favorites.addView(b);
		}
	}
}
