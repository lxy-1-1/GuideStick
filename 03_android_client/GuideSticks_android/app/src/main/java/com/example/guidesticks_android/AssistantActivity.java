package com.example.guidesticks_android;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.guidesticks_android.api.ApiErrorParser;
import com.example.guidesticks_android.api.ApiService;
import com.example.guidesticks_android.api.RetrofitClient;
import com.example.guidesticks_android.model.AssistantRequest;
import com.example.guidesticks_android.model.AssistantResponse;
import com.example.guidesticks_android.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssistantActivity extends BaseActivity {
    private final ConversationStore conversationStore = new ConversationStore();
    private ApiService apiService;
    private ChatMessageAdapter adapter;
    private EditText input;
    private ImageButton btnSend;
    private ImageButton btnMic;
    private ListView listView;
    private TextView tvAssistantStatus;
    private Toolbar toolbar;
    private boolean requestInFlight;
    private boolean destroyed;
    private Call<AssistantResponse> activeCall;
    private String lastFailedQuestion;
    private List<ChatMessage> lastRequestHistory;

    private final ActivityResultLauncher<String> microphonePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (destroyed) {
                    return;
                }
                if (granted) {
                    launchSpeechRecognizer();
                } else {
                    Toast.makeText(this, R.string.speech_permission_required,
                            Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (destroyed || result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                ArrayList<String> values = result.getData()
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (values != null && !values.isEmpty()) {
                    input.setText(values.get(0));
                    sendQuestion(values.get(0));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        toolbar.setOnMenuItemClickListener(this::handleToolbarAction);

        input = findViewById(R.id.etQuestion);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        listView = findViewById(R.id.listMessages);
        tvAssistantStatus = findViewById(R.id.tvAssistantStatus);
        conversationStore.add("assistant", getString(R.string.assistant_greeting));
        adapter = new ChatMessageAdapter(this, conversationStore.allMessages());
        listView.setAdapter(adapter);
        apiService = RetrofitClient.getInstance(this).getApiService();

        btnSend.setOnClickListener(view -> {
            performAccessibleFeedback(view);
            sendQuestion(input.getText().toString());
        });
        btnMic.setOnClickListener(view -> {
            performAccessibleFeedback(view);
            startVoiceInput();
        });
        findViewById(R.id.chipScene).setOnClickListener(view -> sendQuickPrompt(R.string.assistant_prompt_scene));
        findViewById(R.id.chipCrossing).setOnClickListener(view -> sendQuickPrompt(R.string.assistant_prompt_crossing));
        findViewById(R.id.chipCalm).setOnClickListener(view -> sendQuickPrompt(R.string.assistant_prompt_calm));
        input.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendQuestion(input.getText().toString());
                return true;
            }
            return false;
        });

    }

    private boolean handleToolbarAction(MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_speaking) {
            performAccessibleFeedback(toolbar);
            boolean enabled = !appSettings.isAutoSpeakEnabled();
            appSettings.setAutoSpeakEnabled(enabled);
            if (!enabled) {
                stopSpeaking();
            }
            updateSpeechMenuItem();
            String status = getString(SpeechMenuState.statusRes(enabled));
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
            if (enabled) {
                speakImportant(toolbar, status);
            } else {
                announceStatus(toolbar, status);
            }
            return true;
        }
        if (item.getItemId() == R.id.action_clear_chat) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.assistant_clear_confirm)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.assistant_clear_description, (dialog, which) -> {
                        conversationStore.clear();
                        conversationStore.add("assistant", getString(R.string.assistant_greeting));
                        lastFailedQuestion = null;
                        lastRequestHistory = null;
                        input.setText("");
                        adapter.notifyMessagesChanged();
                        scrollToLatest();
                    })
                    .show();
            return true;
        }
        return false;
    }

    private void updateSpeechMenuItem() {
        if (toolbar == null) {
            return;
        }
        boolean enabled = appSettings.isAutoSpeakEnabled();
        MenuItem item = toolbar.getMenu().findItem(R.id.action_toggle_speaking);
        if (item != null) {
            item.setIcon(SpeechMenuState.iconRes(enabled));
            item.setTitle(SpeechMenuState.titleRes(enabled));
        }
    }

    private void sendQuickPrompt(int stringRes) {
        String question = getString(stringRes);
        input.setText(question);
        sendQuestion(question);
    }

    private void startVoiceInput() {
        if (destroyed) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            launchSpeechRecognizer();
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void launchSpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.assistant_listening));
        try {
            speechLauncher.launch(intent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, R.string.speech_service_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendQuestion(String rawQuestion) {
        String question = rawQuestion == null ? "" : rawQuestion.trim();
        if (destroyed || question.isEmpty() || requestInFlight) {
            return;
        }
        if (question.length() > 200) {
            Toast.makeText(this, R.string.assistant_question_too_long, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean retry = question.equals(lastFailedQuestion) && lastRequestHistory != null;
        List<ChatMessage> history = retry
                ? new ArrayList<>(lastRequestHistory)
                : conversationStore.historyForApi();
        if (!retry) {
            conversationStore.add("user", question);
            lastRequestHistory = new ArrayList<>(history);
        }
        lastFailedQuestion = null;
        input.setText("");
        adapter.notifyMessagesChanged();
        scrollToLatest();
        setLoading(true);

        Call<AssistantResponse> call = apiService.chat(new AssistantRequest(question, history));
        activeCall = call;
        call.enqueue(new Callback<AssistantResponse>() {
            @Override
            public void onResponse(Call<AssistantResponse> request,
                                   Response<AssistantResponse> response) {
                if (!isActiveCall(request)) {
                    return;
                }
                activeCall = null;
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getAnswer() == null) {
                    String error = ApiErrorParser.parse(response.errorBody(),
                            getString(R.string.assistant_network_error));
                    keepForRetry(question, error);
                    return;
                }
                String answer = response.body().getAnswer();
                lastRequestHistory = null;
                conversationStore.add("assistant", answer);
                adapter.notifyMessagesChanged();
                scrollToLatest();
                speakAnswer(answer);
            }

            @Override
            public void onFailure(Call<AssistantResponse> request, Throwable throwable) {
                if (!isActiveCall(request)) {
                    return;
                }
                activeCall = null;
                setLoading(false);
                keepForRetry(question, getString(R.string.assistant_network_error));
            }
        });
    }

    private void keepForRetry(String question, String error) {
        lastFailedQuestion = question;
        input.setText(question);
        input.setSelection(input.length());
        tvAssistantStatus.setText(getString(R.string.assistant_retry_hint));
        Toast.makeText(this, error + "\n" + appSettings.getServerUrl(), Toast.LENGTH_LONG).show();
    }

    private void speakAnswer(String answer) {
        if (!destroyed) {
            speakImportant(listView, answer);
        }
    }

    private void setLoading(boolean loading) {
        requestInFlight = loading;
        btnSend.setEnabled(!loading);
        btnMic.setEnabled(!loading);
        if (loading) {
            tvAssistantStatus.setText(R.string.assistant_thinking);
        } else {
            tvAssistantStatus.setText("");
        }
    }

    private void scrollToLatest() {
        if (adapter.getCount() > 0) {
            listView.setSelection(adapter.getCount() - 1);
        }
    }

    private boolean isActiveCall(Call<AssistantResponse> call) {
        return !destroyed && activeCall == call;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSpeechMenuItem();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
        super.onDestroy();
    }
}
