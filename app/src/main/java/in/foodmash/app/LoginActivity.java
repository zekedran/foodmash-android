package in.foodmash.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.apache.commons.validator.routines.EmailValidator;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import in.foodmash.app.commons.Alerts;
import in.foodmash.app.commons.Animations;
import in.foodmash.app.commons.Cryptography;
import in.foodmash.app.commons.JsonProvider;
import in.foodmash.app.commons.Swift;
import in.foodmash.app.custom.TouchableImageButton;
import in.foodmash.app.utils.NumberUtils;

/**
 * Created by Zeke on Jul 19 2015.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher{

    LinearLayout register;
    LinearLayout forgotPassword;
    LinearLayout skip;
    LinearLayout login;

    TouchableImageButton clearAllFields;
    boolean isEmail = true;
    EditText email;
    EditText password;
    EditText phonePrefix;
    ImageView emailValidate;
    ImageView passwordValidate;
    Switch keepLoggedIn;

    JsonObjectRequest loginRequest;
    Intent intent;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_signed_out, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_contact_us: intent = new Intent(this,ContactUsActivity.class); startActivity(intent); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        register = (LinearLayout) findViewById(R.id.register); register.setOnClickListener(this);
        forgotPassword = (LinearLayout) findViewById(R.id.forgot_password); forgotPassword.setOnClickListener(this);
        skip = (LinearLayout) findViewById(R.id.skip); skip.setOnClickListener(this);
        login = (LinearLayout) findViewById(R.id.login); login.setOnClickListener(this);

        clearAllFields = (TouchableImageButton) findViewById(R.id.clear_fields); clearAllFields.setOnClickListener(this);
        emailValidate = (ImageView) findViewById(R.id.email_validate);
        passwordValidate = (ImageView) findViewById(R.id.password_validate);
        phonePrefix = (EditText) findViewById(R.id.phone_prefix);
        email = (EditText) findViewById(R.id.email_or_phone); email.addTextChangedListener(this);
        if(getPhone()!=null) { email.setText(getPhone()); }
        else if(getEmail()!=null) { email.setText(getEmail()); }
        password = (EditText) findViewById(R.id.password); password.addTextChangedListener(this);
        keepLoggedIn = (Switch) findViewById(R.id.keep_logged_in); keepLoggedIn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked) { keepMeLoggedIn(false); Alerts.commonErrorAlert(LoginActivity.this, "Logout on exit", "You will be logged out once you close the app", "Okay"); }
                else keepMeLoggedIn(true);
            }
        });

        keepMeLoggedIn(true);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clear_fields: email.setText(null); password.setText(null); keepLoggedIn.setChecked(true); break;
            case R.id.register: intent = new Intent(this, SignupActivity.class); startActivity(intent); break;
            case R.id.forgot_password: intent = new Intent(this, ForgotPasswordActivity.class); startActivity(intent); break;
            case R.id.login: if(isEverythingValid()) makeJsonRequest(); else Alerts.validityAlert(LoginActivity.this); break;
        }
    }

    private JSONObject getRequestJson() {
        JSONObject jsonObject = JsonProvider.getAnonymousRequestJson(LoginActivity.this);
        HashMap<String,String> hashMap=  new HashMap<>();
        if(isEmail) hashMap.put("email", email.getText().toString().trim());
        else hashMap.put("mobile_no", email.getText().toString().trim());
        hashMap.put("password", password.getText().toString());
        JSONObject userJson = new JSONObject(hashMap);
        try {
            JSONObject dataJson = new JSONObject();
            dataJson.put("user",userJson);
            jsonObject.put("data",dataJson);
        } catch (JSONException e) { e.printStackTrace(); }
        return jsonObject;
    }

    private void makeJsonRequest() {
        loginRequest = new JsonObjectRequest(Request.Method.POST, getString(R.string.api_root_path)+"/sessions",getRequestJson(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                System.out.println("JSON Response: "+response);
                intent = new Intent(LoginActivity.this, MainActivity.class);
                try {
                    if (response.getBoolean("success")) {
                        JSONObject dataJson = response.getJSONObject("data");
                        JSONObject userJson = dataJson.getJSONObject("user");
                        cacheEmailAndPhone(userJson.getString("email"),userJson.getString("mobile_no"));
                        String userToken = dataJson.getString("user_token");
                        String sessionToken = dataJson.getString("session_token");
                        SharedPreferences sharedPreferences = getSharedPreferences("session", 0);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("logged_in",true);
                        editor.putString("user_token", userToken);
                        editor.putString("session_token", sessionToken);
                        editor.putString("android_token", Cryptography.getEncryptedAndroidId(LoginActivity.this, sessionToken));
                        editor.apply();
                        startActivity(intent);
                        finish();
                    } else if(!(response.getBoolean("success"))) {
                        Alerts.commonErrorAlert(LoginActivity.this,
                                "Invalid username or password",
                                "We are unable to log you in with the entered credentials. Please try again!",
                                "Okay");
                        System.out.println("Error Details: " + response.getString("error"));
                    }
                } catch (JSONException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error instanceof TimeoutError) Alerts.timeoutErrorAlert(LoginActivity.this, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Swift.getInstance(LoginActivity.this).addToRequestQueue(loginRequest);
                    }
                });
                if(error instanceof NoConnectionError) Alerts.internetConnectionErrorAlert(LoginActivity.this);
                else Alerts.unknownErrorAlert(LoginActivity.this);
                System.out.println("Response Error: " + error);
            }
        });
        Swift.getInstance(LoginActivity.this).addToRequestQueue(loginRequest);
    }

    private boolean isEverythingValid() {
        return (isEmail)?EmailValidator.getInstance().isValid(email.getText().toString().trim()):email.getText().toString().trim().length()==10
                && password.getText().length()>=8;
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
    @Override public void afterTextChanged(Editable s) {
        if(s==email.getEditableText()) {
            if (NumberUtils.isInteger(s.toString())) {
                Animations.fadeInOnlyIfInvisible(phonePrefix, 500);
                if(s.length()==10) Animations.fadeOut(emailValidate,500);
                else Animations.fadeInOnlyIfInvisible(emailValidate,500);
                isEmail = false;
            }
            else {
                Animations.fadeOut(phonePrefix, 500);
                if (EmailValidator.getInstance().isValid(s.toString().trim()))
                    Animations.fadeOut(emailValidate, 500);
                else Animations.fadeInOnlyIfInvisible(emailValidate, 500);
                isEmail = true;
            }
        }
        else if(s==password.getEditableText()) { if(s.length()>=8) Animations.fadeOut(passwordValidate, 500); else Animations.fadeInOnlyIfInvisible(passwordValidate,500); }
    }

    private void keepMeLoggedIn(boolean bool) {
        SharedPreferences sharedPreferences = getSharedPreferences("preferences",0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("keep_me_logged_in",bool);
        editor.apply();
    }

    private String getEmail() {
        SharedPreferences sharedPreferences = getSharedPreferences("cache",0);
        return sharedPreferences.getString("email",null);
    }

    private String getPhone() {
        SharedPreferences sharedPreferences = getSharedPreferences("cache",0);
        return sharedPreferences.getString("phone",null);
    }

    private void cacheEmailAndPhone(String email, String phone) {
        SharedPreferences sharedPreferences = getSharedPreferences("cache", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email",email);
        editor.putString("phone",phone);
        editor.apply();
    }

}
