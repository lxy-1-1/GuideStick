package com.example.guidesticks_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.example.guidesticks_android.api.ApiErrorParser;
import com.example.guidesticks_android.api.RetrofitClient;
import com.example.guidesticks_android.model.LoginResponse;
import com.example.guidesticks_android.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {
    private EditText etUsername;
    private EditText etPassword;
    private TextInputLayout layoutUsername;
    private TextInputLayout layoutPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnRegister;
    private LinearProgressIndicator progress;
    private Call<LoginResponse> activeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getString("token", "").isEmpty()) {
            openHome();
            return;
        }
        setContentView(R.layout.activity_login);
        markAccessibilityHeadings(R.id.headingLogin);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        layoutUsername = findViewById(R.id.layoutUsername);
        layoutPassword = findViewById(R.id.layoutPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(view -> {
            performAccessibleFeedback(view);
            submit();
        });
        btnRegister.setOnClickListener(view -> {
            performAccessibleFeedback(view);
            startActivity(new Intent(this, RegisterActivity.class));
        });
        etPassword.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
    }

    private void submit() {
        layoutUsername.setError(null);
        layoutPassword.setError(null);
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (username.isEmpty()) {
            layoutUsername.setError(getString(R.string.field_required));
            return;
        }
        if (password.isEmpty()) {
            layoutPassword.setError(getString(R.string.field_required));
            return;
        }

        setLoading(true);
        Call<LoginResponse> call = RetrofitClient.getInstance(this)
                .getApiService().login(new User(username, password));
        activeCall = call;
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> request, Response<LoginResponse> response) {
                if (activeCall != request || isFinishing()) {
                    return;
                }
                activeCall = null;
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getUser() == null) {
                    String error = ApiErrorParser.parse(response.errorBody(),
                            getString(R.string.login_failed));
                    layoutPassword.setError(error);
                    return;
                }
                LoginResponse login = response.body();
                User user = login.getUser();
                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                        .putString("token", login.getToken())
                        .putString("username", user.getUsername())
                        .putString("email", user.getEmail())
                        .apply();
                openHome();
            }

            @Override
            public void onFailure(Call<LoginResponse> request, Throwable error) {
                if (activeCall != request || request.isCanceled() || isFinishing()) {
                    return;
                }
                activeCall = null;
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        getString(R.string.network_error_with_address, appSettings.getServerUrl()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
        etUsername.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    private void openHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
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
