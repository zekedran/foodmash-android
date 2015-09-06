package in.foodmash.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by sarav on Aug 08 2015.
 */
public class ChangePasswordActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {

    EditText oldPassword;
    EditText newPassword;
    EditText confirmPassword;

    boolean forgot = false;
    String otpToken;

    ImageView oldPasswordValidate;
    ImageView newPasswordValidate;
    ImageView confirmPasswordValidate;

    LinearLayout back;
    LinearLayout change;

    Intent intent;
    TouchableImageButton clearFields;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_profile) { intent = new Intent(this,ProfileActivity.class); startActivity(intent); return true; }
        if (id == R.id.menu_addresses) { intent = new Intent(this,AddressActivity.class); startActivity(intent); return true; }
        if (id == R.id.menu_order_history) { intent = new Intent(this,OrderHistoryActivity.class); startActivity(intent); return true; }
        if (id == R.id.menu_contact_us) { intent = new Intent(this,ContactUsActivity.class); startActivity(intent); return true; }
        if (id == R.id.menu_log_out) { intent = new Intent(this,LoginActivity.class); startActivity(intent); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        if(getIntent().getBooleanExtra("forgot",false)) { forgot = true; otpToken = getIntent().getStringExtra("otp_token"); }

        oldPasswordValidate = (ImageView) findViewById(R.id.old_password_validate);
        newPasswordValidate = (ImageView) findViewById(R.id.new_password_validate);
        confirmPasswordValidate = (ImageView) findViewById(R.id.confirm_password_validate);

        oldPassword = (EditText) findViewById(R.id.old_password); if(forgot) oldPassword.setVisibility(View.GONE); else oldPassword.addTextChangedListener(this);
        newPassword = (EditText) findViewById(R.id.new_password); newPassword.addTextChangedListener(this);
        confirmPassword = (EditText) findViewById(R.id.confirm_password); confirmPassword.addTextChangedListener(this);

        back = (LinearLayout) findViewById(R.id.back); back.setOnClickListener(this);
        change = (LinearLayout) findViewById(R.id.change); change.setOnClickListener(this);
        clearFields = (TouchableImageButton) findViewById(R.id.clear_fields); clearFields.setOnClickListener(this);

        if(getIntent().getBooleanExtra("forgot", false)) {
            oldPassword.setVisibility(View.GONE);
            back.setVisibility(View.GONE);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clear_fields: oldPassword.setText(null); newPassword.setText(null); confirmPassword.setText(null); break;
            case R.id.back: intent = new Intent(this, MainActivity.class); startActivity(intent); break;
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
            if(forgot) requestJson.put("android_id", Settings.Secure.getString(this.getContentResolver(),Settings.Secure.ANDROID_ID));
            else requestJson = JsonProvider.getStandartRequestJson(ChangePasswordActivity.this);
            JSONObject dataJson = new JSONObject();
            dataJson.put("user",userJson);
            requestJson.put("data", dataJson);
        } catch (Exception e) { e.printStackTrace(); }
        return requestJson;
    }

    private void makeRequest() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getString(R.string.api_root_path) + ((forgot)?"/registrations/resetPasswordFromToken":"/registrations/changePassword"), getRequestJson(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(response.getBoolean("success")) {
                        if(forgot) {
                            JSONObject dataJson = response.getJSONObject("data");
                            String userToken = dataJson.getString("user_token");
                            String sessionToken = dataJson.getString("session_token");
                            String androidId = Settings.Secure.getString(ChangePasswordActivity.this.getContentResolver(),Settings.Secure.ANDROID_ID);
                            SharedPreferences sharedPreferences = getSharedPreferences("session", 0);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("logged_in",true);
                            editor.putString("user_token", userToken);
                            editor.putString("session_token", sessionToken);
                            editor.putString("android_token", Cryptography.encrypt(androidId, sessionToken));
                            editor.commit();
                            intent = new Intent(ChangePasswordActivity.this, MainActivity.class);
                            startActivity(intent);
                        } else {
                            intent = new Intent(ChangePasswordActivity.this, ProfileActivity.class);
                            startActivity(intent);
                        }
                    } else if(!response.getBoolean("success")) {
                        if(forgot) Alerts.commonErrorAlert(ChangePasswordActivity.this,"OTP Error","There's a problem processing the OTP that you've sent","Okay");
                        else Alerts.commonErrorAlert(ChangePasswordActivity.this,"Invalid Old Password","We are unable to change your password as Old Password entered by you is Invalid","Okay");
                        System.out.println(response.getString("error"));
                    }
                } catch (JSONException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error instanceof NoConnectionError || error instanceof TimeoutError) Alerts.internetConnectionErrorAlert(ChangePasswordActivity.this);
                else Alerts.unknownErrorAlert(ChangePasswordActivity.this);
                System.out.println("Response Error: " + error);
            }
        });
        Swift.getInstance(ChangePasswordActivity.this).addToRequestQueue(jsonObjectRequest);
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
                Animations.fadeInOnlyIfInvisible(oldPasswordValidate,500);
            else Animations.fadeOut(oldPasswordValidate,500);
            if(newPassword.getText().length()>0 && !newPassword.getText().toString().equals(oldPassword.getText().toString()))
                Animations.fadeInOnlyIfInvisible(newPasswordValidate,500);
            else if(newPassword.getText().length()>=8 && newPassword.getText().toString().equals(oldPassword.getText().toString()));
                    Animations.fadeOut(newPasswordValidate,500);
        } else if(s==newPassword.getEditableText()) {
            if(newPassword.getText().toString().length()<8
                    || (!forgot && !newPassword.getText().toString().equals(oldPassword.getText().toString())))
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
