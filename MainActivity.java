package com.example.mqttapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "AiotMqtt";
    final private String PUB_TOPIC = "projeto/sensor/alimentar";
    final private String SUB_TOPIC_DIST = "projeto/sensor/distancia";
    final private String SUB_TOPIC_GEAR = "projeto/sensor/motor";

    final String host = "tcp://broker.hivemq.com:1883";
    private String clientId = MqttClient.generateClientId();;
    private String userName;
    private String passWord;

    MqttAndroidClient mqttAndroidClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView tvStatus = (TextView)findViewById(R.id.tvStatus);
        //setContentView(tvStatus);

        /* MqttConnectOptions */
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        //mqttConnectOptions.setUserName(userName);
        //mqttConnectOptions.setPassword(passWord.toCharArray());

        /* MqqtAndroidClient */
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(),host,clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG, "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG,"topic: " + topic + ", msg: " + new String(message.getPayload()));
                String msg = new String(message.getPayload());

                if(msg.toString().equals("acabando")){
                    String aviso = "A ração do reservatório está acabando!!";
                    tvStatus.setText(aviso);
                    Log.i(TAG,aviso);
                }

                if(msg.toString().equals("on")){
                    String aviso = "Alimentação do pet liberada!!";
                    tvStatus.setText(aviso);
                    Log.i(TAG,aviso);
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(TAG, "msg delivered");
            }
        });

        /* Mqtt */
        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "connect succed");
                    tvStatus.setText("Conectado!");

                    try {
                        subscribeTopic(SUB_TOPIC_DIST);
                        subscribeTopic(SUB_TOPIC_GEAR);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "connect failed");
                    tvStatus.setText("Falha na conexão!");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }


        Button pubButton = findViewById(R.id.publish);
        pubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage("alimentar");
                tvStatus.setText("Comando para alimentar o pet enviado:");
            }
        });
    }

    /** @param topic mqtt */
    public void subscribeTopic(String topic) throws MqttException {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "subscribed succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "subscribed failed");
                }
            });

        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    /** @param payload */
    public void publishMessage(String payload){
        try{
            if(mqttAndroidClient.isConnected()==false){
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);
            mqttAndroidClient.publish(PUB_TOPIC, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "publish succeed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "publish failed");
                }
            });
        } catch (MqttException e){
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }
}
