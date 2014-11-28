package combined2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.example.android.wifidirect.R;
public class SenderWelcome extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.senderwelcome);
	    // TODO Auto-generated method stub
	    Button nextBt = (Button) findViewById(R.id.nextBt);
	    final EditText smscount = (EditText) findViewById(R.id.SMSCount);
	    final EditText mmscount = (EditText) findViewById(R.id.MMSCount);
	    final EditText tgcount = (EditText) findViewById(R.id.TGCount);
	    nextBt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(SenderWelcome.this,
						SenderActivity.class);
				intent.putExtra("smscount", smscount.getText().toString());
				intent.putExtra("mmscount", mmscount.getText().toString());
				intent.putExtra("3Gcount", tgcount.getText().toString());
				
				intent.putExtra("start?", "no");
				startActivity(intent);
			}
		});
	}

}
