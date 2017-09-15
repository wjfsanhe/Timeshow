package framework.com.timeshow;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements FeedbackText {

    TextView tv;
    EditText out;
    Button send;
    SimulateClient client=new SimulateClient();
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
        send = (Button) findViewById(R.id.button);
        out = (EditText) findViewById(R.id.editText4);
        Log.d("MainW","onCreate be called");

        //start service.
        Intent it=new Intent(this,TimeInfoService.class);
        startService(it);

        final Thread thread = new Thread(client);
        client.setup(this,this);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                thread.start();
            }

        }, 2 * 1000);

        send.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                String msg = out.getText().toString();
                Log.d("TimeInfoService","send " +msg);
                client.sendCmd(out.getText().toString());
            }
        });
    }

    @Override
    public void TextFeedBack(String txt){
        tv.setText(txt);
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
