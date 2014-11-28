package combined2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.example.android.wifidirect.R;

public class CombinedActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        /*
		Button sendBt = (Button) findViewById(R.id.senderBt);
		Button recvBt = (Button) findViewById(R.id.receiverBt);

		sendBt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(CombinedActivity.this,
						SenderInitial.class);
				intent.putExtra("start?", "no");
				startActivity(intent);
			}
		});
		recvBt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(CombinedActivity.this,
						ReceiverActivity.class);
				intent.putExtra("start?", "no");
				startActivity(intent);
			}
		});
		*/
	}
}