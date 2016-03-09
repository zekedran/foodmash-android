package in.foodmash.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import in.foodmash.app.commons.Alerts;
import in.foodmash.app.commons.Animations;
import in.foodmash.app.commons.Cryptography;
import in.foodmash.app.commons.JsonProvider;
import in.foodmash.app.commons.Swift;

/**
 * Created by Zeke on Aug 08 2015.
 */
public class ChangePasswordActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {

    @Bind(R.id.change) FloatingActionButton change;
    @Bind(R.id.main_layout) ScrollView mainLayout;
    @Bind(R.id.saving_layout) LinearLayout savingLayout;
    @Bind(R.id.toolbar) Toolbar toolbar;

    private EditText oldPassword;
    private EditText newPassword;
    private EditText confirmPassword;
    private JsonObjectRequest changePasswordRequest;

    private boolean forgot = false;
    private String otpToken;

    private ImageView oldPasswordValidate;
    private ImageView newPasswordValidate;
    private ImageView confirmPasswordValidate;

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        try {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) { e.printStackTrace(); }

        if(getIntent().getBooleanExtra("forgot",false)) { forgot = true; otpToken = getIntent().getStringExtra("otp_token"); }

        oldPasswordValidate = (ImageView) findViewById(R.id.old_password_validate);
        newPasswordValidate = (ImageView) findViewById(R.id.new_password_validate);
        confirmPasswordValidate = (ImageView) findViewById(R.id.confirm_password_validate);

        oldPassword = (EditText) findViewById(R.id.old_password); if(forgot) oldPassword.setVisibility(View.GONE); else oldPassword.addTextChangedListener(this);
        newPassword = (EditText) findViewById(R.id.new_password); newPassword.addTextChangedListener(this);
        confirmPassword = (EditText) findViewById(R.id.confirm_password); confirmPassword.addTextChangedListener(this);

        change.setOnClickListener(this);
        if(getIntent().getBooleanExtra("forgot", false)) oldPassword.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.change: if (isEverythingValid()) makeRequest(); else Alerts.validityAlert(ChangePasswordActivity.this); break;
        }
    }

    private JSONObject getRequestJson() {
        JSONObject requestJson = new JSONObject();
        try {
            HashMap<String, String> hashMap = new HashMap<>();
            if(forgot) hashMap.put("otp_token", otpToken);
            else hashMap.put("old_password", oldPassword.getText().toString());
            hashMap.put("password", newPassword.getText().toString());
            hashMap.put("password_confirmation", confirmPassword.getText().toString());
            JSONObject userJson = new JSONObject(hashMap);
            if(forgot) requestJson = JsonProvider.getAnonymousRequestJson(ChangePasswordActivity.this);
            else requestJson = JsonProvider.getStandardRequestJson(ChangePasswordActivity.this);
            JSONObject dataJson = new JSONObject();
            dataJson.put("user",userJson);
            requestJson.put("data", dataJson);
        } catch (Exception e) { e.printStackTrace(); }
        return requestJson;
    }

    private void makeRequest() {
        changePasswordRequest = new JsonObjectRequest(Request.Method.POST, getString(R.string.api_root_path) + ((forgot)?"/registrations/resetPasswordFromToken":"/registrations/changePassword"), getRequestJson(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(response.getBoolean("success")) {
                        if(forgot) {
                            JSONObject dataJson = response.getJSONObject("data");
                            String userToken = dataJson.getString("user_token");
                            String sessionToken = dataJson.getString("session_token");
                            SharedPreferences sharedPreferences = getSharedPreferences("session", 0);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("logged_in", true);
                            editor.putString("user_token", userToken);
                            editor.putString("session_token", sessionToken);
                            editor.putString("android_token", Cryptography.getEncryptedAndroidId(ChangePasswordActivity.this, userToken));
                            editor.apply();
                            intent = new Intent(ChangePasswordActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        } else {
                            finish();
                        }
                    } else {
                        Animations.fadeOut(savingLayout,500);
                        Animations.fadeIn(mainLayout,500);
                        if(forgot) Alerts.commonErrorAlert(ChangePasswordActivity.this,"OTP Error","There's a problem processing the OTP that you've sent","Okay");
                        else Alerts.commonErrorAlert(ChangePasswordActivity.this,"Invalid Old Password","We are unable to change your password as Old Password entered by you is Invalid","Okay");
                        Log.e("Success False",response.getString("error"));
                    }
                } catch (JSONException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Animations.fadeOut(savingLayout,500);
                Animations.fadeIn(mainLayout,500);
                DialogInterface.OnClickListener onClickTryAgain = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Animations.fadeIn(savingLayout,500);
                        Animations.fadeOut(mainLayout, 500);
                        Swift.getInstance(ChangePasswordActivity.this).addToRequestQueue(changePasswordRequest);
                    }
                };
                if(error instanceof TimeoutError) Alerts.timeoutErrorAlert(ChangePasswordActivity.this, onClickTryAgain);
                if(error instanceof NoConnectionError) Alerts.internetConnectionErrorAlert(ChangePasswordActivity.this, onClickTryAgain);
                else Alerts.unknownErrorAlert(ChangePasswordActivity.this);
                Log.e("Json Request Failed", error.toString());
            }
        });
        Animations.fadeIn(savingLayout,500);
        Animations.fadeOut(mainLayout, 500);
        Swift.getInstance(ChangePasswordActivity.this).addToRequestQueue(changePasswordRequest);
    }

    private boolean isEverythingValid() {
        return (forgot) || oldPassword.getText().length() >= 8 &&
                (forgot) || !newPassword.getText().toString().equals(oldPassword.getText().toString()) &&
                confirmPassword.getText().toString().equals(newPassword.getText().toString()) &&
                newPassword.getText().length() >= 8;
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {  }
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {  }
    @Override public void afterTextChanged(Editable s) {
        if(s==oldPassword.getEditableText()) {
            if(oldPassword.getText().toString().length()<8)
                Animations.fadeInOnlyIfInvisible(oldPasswordValidate, 500);
            else Animations.fadeOut(oldPasswordValidate,500);
            if(newPassword.getText().length()>0 && !newPassword.getText().toString().equals(oldPassword.getText().toString()))
                Animations.fadeInOnlyIfInvisible(newPasswordValidate,500);
            else if(newPassword.getText().length()>=8 && newPassword.getText().toString().equals(oldPassword.getText().toString()))
                    Animations.fadeOut(newPasswordValidate,500);
        } else if(s==newPassword.getEditableText()) {
            if(newPassword.getText().toString().length()<8
                    || (!forgot && newPassword.getText().toString().equals(oldPassword.getText().toString())))
                Animations.fadeInOnlyIfInvisible(newPasswordValidate,500);
            else Animations.fadeOut(newPasswordValidate,500);
            if(confirmPassword.getText().length()>0 && !confirmPassword.getText().toString().equals(newPassword.getText().toString()))
                Animations.fadeInOnlyIfInvisible(confirmPasswordValidate,500);
            else if(confirmPassword.getText().toString().equals(newPassword.getText().toString()))
                Animations.fadeOut(confirmPasswordValidate,500);
        } else if(s==confirmPassword.getEditableText()) {
            if(!confirmPassword.getText().toString().equals(newPassword.getText().toString())) Animations.fadeInOnlyIfInvisible(confirmPasswordValidate,500);
            else Animations.fadeOut(confirmPasswordValidate,500);
        }
    }
}
