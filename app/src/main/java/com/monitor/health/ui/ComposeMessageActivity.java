package com.monitor.health.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.monitor.health.R;

public class ComposeMessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_message);

        EditText edtTo = findViewById(R.id.edt_to);
        EditText edtSubject = findViewById(R.id.edt_subject);
        EditText edtMessage = findViewById(R.id.edt_message);
        Button btnSend = findViewById(R.id.btn_send);

        String toName = getIntent().getStringExtra("to_name");
        if (toName != null) edtTo.setText(toName);

        btnSend.setOnClickListener(v -> {
            String to = edtTo.getText().toString().trim();
            String subject = edtSubject.getText().toString().trim();
            String msg = edtMessage.getText().toString().trim();

            if (to.isEmpty() || msg.isEmpty()) {
                Toast.makeText(this, "Please enter To and Message.", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: send to your API here
            Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}