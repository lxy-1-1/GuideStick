package com.example.guidesticks_android;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.example.guidesticks_android.api.ApiErrorParser;
import com.example.guidesticks_android.api.RetrofitClient;
import com.example.guidesticks_android.model.RegisterResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {
    private EditText etUsername;
    private EditText etPassword;
    private EditText etEmail;
    private EditText etConfirmPassword;
    private TextInputLayout layoutUsername;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputLayout layoutConfirmPassword;
    private MaterialButton btnRegister;
    private LinearProgressIndicator progress;
    private Call<RegisterResponse> activeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        markAccessibilityHeadings(R.id.headingRegister);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        layoutUsername = findViewById(R.id.layoutUsername);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progressRegister);

        btnRegister.setOnClickListener(view -> {
            performAccessibleFeedback(view);
            submit();
        });
    }

    private void submit() {
        clearErrors();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmation = etConfirmPassword.getText().toString();

        boolean valid = true;
        if (username.isEmpty()) {
            layoutUsername.setError(getString(R.string.field_required));
            valid = false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError(getString(R.string.invalid_email));
            valid = false;
        }
        if (password.isEmpty()) {
            layoutPassword.setError(getString(R.string.field_required));
            valid = false;
        }
        if (!password.equals(confirmation)) {
            layoutConfirmPassword.setError(getString(R.string.password_mismatch));
            valid = false;
        }
        if (!valid) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("email", email);
        params.put("password", password);
        params.put("confirm_password", confirmation);
        setLoading(true);

        Call<RegisterResponse> call = RetrofitClient.getInstance(this)
                .getApiService().register(params);
        activeCall = call;
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> request,
                                   Response<RegisterResponse> response) {
                if (activeCall != request || isFinishing()) {
                    return;
                }
                activeCall = null;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getUser() != null) {
                    Toast.makeText(RegisterActivity.this, R.string.register_success,
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                String error = ApiErrorParser.parse(response.errorBody(),
                        getString(R.string.register_failed));
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<RegisterResponse> request, Throwable error) {
                if (activeCall != request || request.isCanceled() || isFinishing()) {
                    return;
                }
                activeCall = null;
                setLoading(false);
                Toast.makeText(RegisterActivity.this,
                        getString(R.string.network_error_with_address, appSettings.getServerUrl()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearErrors() {
        layoutUsername.setError(null);
        layoutEmail.setError(null);
        layoutPassword.setError(null);
        layoutConfirmPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        etUsername.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        etConfirmPassword.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
        super.onDestroy();
    }
}
