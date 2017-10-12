package tk.rabidbeaver.mcuradio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Radio extends Activity {
	
	//private RadioWrapper rw;
	private RadioDB rdb;
	private Button channelbtn;
	private ImageButton togglefav;
	private TextView radiotext;
	private TextView programservice;

	private int lastFreqAM = 1010;
	private int lastFreqFM = 9310;

	private Channel[] favs;
	private boolean lastChannelIsFav = false;

	int freq = 1010;

	private LinearLayout favorites;
	
	private boolean band_fm = false;
	//private int lastss = 0;
	
	private EditText channelinput;
	private Button dialogband;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int band = intent.getIntExtra("BAND", -1);
			int channel = intent.getIntExtra("CHANNEL", -1);
			int area = intent.getIntExtra("AREA", -1);
			int freq = intent.getIntExtra("FREQ", -1);
			int ptyid = intent.getIntExtra("PTYID", -1);
			String rdstext = intent.getStringExtra("RDSTEXT");
			String pstext = intent.getStringExtra("PSTEXT");
			int loc = intent.getIntExtra("LOC", -1);
			int stereo = intent.getIntExtra("STEREO", -1);
			if (freq > 0) channelbtn.setText(Integer.toString(freq));
		}
	};

	private void tune(int frequency, boolean fm){
		Log.d("RADIOTUNE", "FREQ: "+frequency+", FM: "+fm);
		freq = frequency;
		band_fm = fm;
		Intent i = new Intent();
		i.setAction("tk.rabidbeaver.radiocontroller.BAND");
		i.putExtra("BAND", band_fm?"FM":"AM");
		sendBroadcast(i);

		i = new Intent();
		i.setAction("tk.rabidbeaver.radiocontroller.TUNE");
		i.putExtra("FREQ", frequency);
		sendBroadcast(i);

		if (band_fm) lastFreqFM = frequency;
		else lastFreqAM = frequency;

		rdb.setLastFreq(lastFreqAM, lastFreqFM, fm);

		if (rdb.isFavorite(new Channel(frequency, fm, -1))){
			lastChannelIsFav = true;
			togglefav.setImageResource(R.drawable.ic_star_selected);
		} else {
			lastChannelIsFav = false;
			togglefav.setImageResource(R.drawable.ic_star);
		}
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

		IntentFilter filter = new IntentFilter();
		filter.addAction("tk.rabidbeaver.radiocontroller.BROADCAST");
		registerReceiver(receiver, filter);

		//channel = (Button) this.findViewById(R.id.channelline);
		radiotext = findViewById(R.id.radiotextline);
		programservice = findViewById(R.id.programserviceline);
		
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
				channelinput = (EditText) layout.findViewById(R.id.channel);
				dialogband = (Button) layout.findViewById(R.id.dialog_amfm);
				//channelinput.append(lastChannel.split(" ")[0]);
				dialogband.setText(band_fm?"FM":"AM");
				dialogband.setOnClickListener(new Button.OnClickListener(){
					@Override
					public void onClick(View v) {
						if (dialogband.getText().toString().equalsIgnoreCase("AM"))
							dialogband.setText("FM");
						else dialogband.setText("AM");
					}
				});
				dialog.show();
			}
		});

		channelbtn.setText(Integer.toString(freq) + (band_fm?" FM":" AM"));
		
		Button amfmbtn = findViewById(R.id.amfmbtn);
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
					rdb.setFav(new Channel(freq, band_fm, -1), false);
				} else {
					((ImageButton) v).setImageResource(R.drawable.ic_star_selected);
					lastChannelIsFav = true;
					rdb.setFav(new Channel(freq, band_fm, -1), true);
				}
				refreshfav();
			}
		});
		
		Button seekupbtn = findViewById(R.id.seekup);
		seekupbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent();
				i.setAction("tk.rabidbeaver.radiocontroller.SEEK_UP");
				sendBroadcast(i);
			}
		});
		
		Button seekdownbtn = findViewById(R.id.seekdown);
		seekdownbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent();
				i.setAction("tk.rabidbeaver.radiocontroller.SEEK_DOWN");
				sendBroadcast(i);
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
		
		favorites = findViewById(R.id.favorites);
		
		refreshfav();
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
			b.setText(favs[i].frequency+" "+(favs[i].fm?"FM":"AM"));
			favorites.addView(b);
		}
	}

	// Essentially, this function is there to receive data from the radio and update the UI accordingly.
	public void updateStatus(final String name, final String value){
			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					if (name.equalsIgnoreCase("rdsprogramservice"))
						programservice.setText(value.replaceAll(" +", " ").trim());
					if (name.equalsIgnoreCase("rdsradiotext"))
						radiotext.setText(value.replaceAll(" +", " ").trim());
					if (name.equalsIgnoreCase("tune") || name.equalsIgnoreCase("seek")){
						if (name.equalsIgnoreCase("tune")){
							//if (signalmenu != null) signalmenu.setIcon(R.drawable.bars_0);
							//lastss = 0;

							/*lastChannel = value;
							if (lastChannel.contains("FM")){
								band_fm=true;
								lastChannelFM=lastChannel;
								rdb.setLastFM(lastChannelFM);
							} else {
								band_fm=false;
								lastChannelAM=lastChannel;
								rdb.setLastAM(lastChannelAM);
							}*/
						}
						channelbtn.setText(value);
						programservice.setText("");
						radiotext.setText("");
						
						/*lastChannelIsFav = rdb.isFavorite(value);
						if (lastChannelIsFav){
							togglefav.setImageResource(R.drawable.ic_star_selected);
						} else {
							togglefav.setImageResource(R.drawable.ic_star);
						}*/
					}
				}
			});
	}
}
