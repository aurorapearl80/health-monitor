package com.monitor.health;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.monitor.health.utility.PreferenceHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private Button btnLogin;
    private ProgressBar progressLogin;
    private TextView tvError;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        btnLogin = findViewById(R.id.btn_login);
        progressLogin = findViewById(R.id.progress_login);
        tvError = findViewById(R.id.tv_error);

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            etPassword.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivTogglePassword.setAlpha(1.0f);
        } else {
            etPassword.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivTogglePassword.setAlpha(0.6f);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        tvError.setVisibility(View.GONE);
        etEmail.setError(null);
        etPassword.setError(null);

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        ApiClient.getUserService(Constant.BASE_URL, "", "")
                .userLogin(request)
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getToken() != null) {
                            PreferenceHelper.getInstance(getApplicationContext())
                                    .putString(Constant.AUTH_TOKEN, response.body().getToken());
                            launchMain();
                        } else {
                            tvError.setText("Invalid email or password");
                            tvError.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        setLoading(false);
                        tvError.setText("Connection failed. Try again.");
                        tvError.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setAlpha(loading ? 0.6f : 1.0f);
    }

    private void launchMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}