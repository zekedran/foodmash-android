package in.foodmash.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import in.foodmash.app.commons.Actions;
import in.foodmash.app.commons.Alerts;
import in.foodmash.app.commons.Animations;
import in.foodmash.app.commons.Info;
import in.foodmash.app.commons.Swift;
import in.foodmash.app.custom.Cart;
import in.foodmash.app.custom.Combo;
import in.foodmash.app.custom.ComboDish;
import in.foodmash.app.custom.ComboOption;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by Zeke on Sep 30 2015.
 */
public class ComboDescriptionActivity extends AppCompatActivity implements View.OnClickListener {

    @Bind(R.id.parentLayout) View parentLayout;
    @Bind(R.id.fill_layout) LinearLayout fillLayout;
    @Bind(R.id.price) TextView currentPrice;
    @Bind(R.id.buy) FloatingActionButton buy;
    @Bind(R.id.toolbar) Toolbar toolbar;

    private TextView cartCount;
    private Cart cart = Cart.getInstance();
    private Intent intent;
    private Combo combo;
    private ImageLoader imageLoader;
    private DisplayMetrics displayMetrics;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Info.isLoggedIn(ComboDescriptionActivity.this)) getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        else getMenuInflater().inflate(R.menu.menu_main_anonymous_login, menu);
        RelativeLayout cartCountLayout = (RelativeLayout) menu.findItem(R.id.menu_cart).getActionView();
        cartCount = (TextView) cartCountLayout.findViewById(R.id.cart_count);
        Actions.updateCartCount(cartCount);
        cartCountLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(ComboDescriptionActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_profile: intent = new Intent(this, ProfileActivity.class); startActivity(intent); return true;
            case R.id.menu_addresses: intent = new Intent(this, AddressActivity.class); startActivity(intent); return true;
            case R.id.menu_order_history: intent = new Intent(this, OrderHistoryActivity.class); startActivity(intent); return true;
            case R.id.menu_contact_us: intent = new Intent(this, ContactUsActivity.class); startActivity(intent); return true;
            case R.id.menu_log_out: Actions.logout(ComboDescriptionActivity.this); return true;
            case R.id.menu_cart: intent = new Intent(this, CartActivity.class); startActivity(intent); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combo_description);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        try {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) { e.printStackTrace(); }

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

        int comboId = getIntent().getIntExtra("combo_id", -1);
        if(comboId==-1) { Alerts.unknownErrorAlert(ComboDescriptionActivity.this); return; }
        List<Combo> combos = null;
        try { combos = Arrays.asList(objectMapper.readValue(Info.getComboJsonArrayString(this), Combo[].class)); }
        catch (Exception e) { e.printStackTrace(); }
        for (Combo c : combos)
            if (c.getId()==comboId)
                combo = c;
        if(combo==null) { Alerts.unknownErrorAlert(ComboDescriptionActivity.this); return; }
        System.out.println(combo.toString());

        buy.setOnClickListener(this);
        imageLoader = Swift.getInstance(ComboDescriptionActivity.this).getImageLoader();
        updateFillLayout();

    }

    private void resetLayout() {
        for(ComboOption comboOption: combo.getComboOptions()) {
            for (ComboDish comboDish : comboOption.getComboOptionDishes())
                comboDish.resetQuantity();
            comboOption.resetSelectedComboOptionDishes();
        }
        for(ComboDish comboDish: combo.getComboDishes())
            comboDish.resetQuantity();
        updateFillLayout();
    }

    private void updateFillLayout() {
        fillLayout.removeAllViews();
        final TreeMap<Integer,LinearLayout> layoutOrderTreeMap = new TreeMap<>();

        for (final ComboOption comboOption: combo.getComboOptions()) {
            final LinearLayout currentComboFoodLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.repeatable_combo_description_combo_option, fillLayout, false);
            final LinearLayout optionsLayout = (LinearLayout) currentComboFoodLayout.findViewById(R.id.combo_dishes_layout);

            for (final ComboDish comboDish: comboOption.getComboOptionDishes()) {
                final LinearLayout comboOptionsLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.repeatable_combo_description_combo_option_dish, currentComboFoodLayout, false);
                final ImageView selected = (ImageView) comboOptionsLayout.findViewById(R.id.selected);
                if (comboOption.getSelectedComboOptionDishes().contains(comboDish)) { Animations.fadeIn(selected, 500); }
                else { Animations.fadeOut(selected, 500); }
                ImageView foodLabel = (ImageView) comboOptionsLayout.findViewById(R.id.label);
                switch(comboDish.getDish().getLabel()) {
                    case EGG: foodLabel.setColorFilter(ContextCompat.getColor(this, R.color.egg)); break;
                    case VEG: foodLabel.setColorFilter(ContextCompat.getColor(this, R.color.veg)); break;
                    case NON_VEG: foodLabel.setColorFilter(ContextCompat.getColor(this, R.color.non_veg)); break;
                }
                ((TextView) comboOptionsLayout.findViewById(R.id.id)).setText(String.valueOf(comboDish.getId()));
                ((TextView) comboOptionsLayout.findViewById(R.id.name)).setText(comboDish.getDish().getName());
                NetworkImageView comboDishPicture = (NetworkImageView) comboOptionsLayout.findViewById(R.id.image);
                comboDishPicture.setImageUrl(comboDish.getDish().getPicture(), imageLoader);
                comboDishPicture.getLayoutParams().height = displayMetrics.widthPixels/2 - (int)(10 * getResources().getDisplayMetrics().density);
                ((TextView) comboOptionsLayout.findViewById(R.id.description)).setText(comboDish.getDish().getDescription());
                ((TextView) comboOptionsLayout.findViewById(R.id.restaurant_name)).setText(comboDish.getDish().getRestaurant().getName());
                ((NetworkImageView) comboOptionsLayout.findViewById(R.id.restaurant_logo)).setImageUrl(comboDish.getDish().getRestaurant().getLogo(), imageLoader);
                final TextView addExtraLayout = (TextView) comboOptionsLayout.findViewById(R.id.add_extra);
                final LinearLayout countLayout = (LinearLayout) comboOptionsLayout.findViewById(R.id.count_layout);
                final TextView count = (TextView) countLayout.findViewById(R.id.count);
                int quantity = comboDish.getQuantity();
                count.setText(String.valueOf(quantity));
                if (comboOption.getSelectedComboOptionDishes().contains(comboDish)) {
                    Animations.fadeOut(addExtraLayout, 500);
                    Animations.fadeIn(countLayout, 500);
                    Animations.fadeIn(selected, 500);
                } else {
                    Animations.fadeIn(addExtraLayout, 500);
                    Animations.fadeOut(countLayout, 500);
                    Animations.fadeOut(selected, 500);
                }
                TextView plus = (TextView) countLayout.findViewById(R.id.plus);
                TextView minus = (TextView) countLayout.findViewById(R.id.minus);
                plus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!comboOption.incrementQuantity(comboDish))
                            Alerts.maxCountAlert(ComboDescriptionActivity.this, comboOption);
                        count.setText(String.valueOf(comboDish.getQuantity()));
                        updatePrice();
                    }
                });
                minus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(count.getText().toString().equals("0")) return;
                        if(!comboOption.decrementQuantity(comboDish))
                            Alerts.minCountAlert(ComboDescriptionActivity.this, comboOption);
                        count.setText(String.valueOf(comboDish.getQuantity()));
                        if(comboDish.getQuantity()==1 && !comboOption.getSelectedComboOptionDishes().contains(comboDish)) {
                            Animations.fadeOut(selected, 500);
                            Animations.fadeOut(countLayout, 500);
                            Animations.fadeIn(addExtraLayout, 500);
                        }
                        updatePrice();
                    }
                });
                addExtraLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        comboOption.addToSelected(comboDish);
                        Animations.fadeOut(addExtraLayout, 500);
                        Animations.fadeIn(countLayout, 500);
                        count.setText(String.valueOf(comboDish.getQuantity()));
                        Animations.fadeIn(selected, 500);
                        updatePrice();
                    }
                });

                ((View) comboOptionsLayout.findViewById(R.id.count_layout).getParent()).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { } });
                comboOptionsLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        comboOption.addToSelectedAfterClear(comboDish);
                        for (int l = 0; l < comboOption.getComboOptionDishes().size(); l++) {
                            LinearLayout comboOptionsLayout = (LinearLayout) optionsLayout.getChildAt(l);
                            ImageView selected = (ImageView) comboOptionsLayout.findViewById(R.id.selected);
                            TextView addExtraLayout = (TextView) comboOptionsLayout.findViewById(R.id.add_extra);
                            LinearLayout countLayout = (LinearLayout) comboOptionsLayout.findViewById(R.id.count_layout);
                            TextView id = (TextView) comboOptionsLayout.findViewById(R.id.id);
                            ComboDish comboDish = comboOption.fetch(Integer.parseInt(id.getText().toString()));
                            TextView count = (TextView) countLayout.findViewById(R.id.count);
                            if (comboOption.getSelectedComboOptionDishes().contains(comboDish)) {
                                Animations.fadeOut(addExtraLayout, 500);
                                Animations.fadeIn(countLayout, 500);
                                Animations.fadeIn(selected, 500);
                            } else {
                                comboDish.resetQuantity();
                                Animations.fadeIn(addExtraLayout, 500);
                                Animations.fadeOut(countLayout, 500);
                                Animations.fadeOut(selected, 500);
                            }
                            count.setText(String.valueOf(comboDish.getQuantity()));
                        }
                        Animations.fadeOut(addExtraLayout, 500);
                        Animations.fadeIn(countLayout, 500);
                        Animations.fadeIn(selected, 500);
                        updatePrice();
                    }
                });
                optionsLayout.addView(comboOptionsLayout);
            }

            if (comboOption.isFromSameRestaurant()) {
                for (int m = 0; m < comboOption.getComboOptionDishes().size(); m++)
                    optionsLayout.getChildAt(m).findViewById(R.id.restaurant_layout).setVisibility(View.GONE);
                ((TextView) currentComboFoodLayout.findViewById(R.id.restaurant_name)).setText(comboOption.getComboOptionDishes().get(0).getDish().getRestaurant().getName());
                ((NetworkImageView) currentComboFoodLayout.findViewById(R.id.restaurant_logo)).setImageUrl(comboOption.getComboOptionDishes().get(0).getDish().getRestaurant().getLogo(),imageLoader);
            } else currentComboFoodLayout.findViewById(R.id.restaurant_layout).setVisibility(View.GONE);
            layoutOrderTreeMap.put(comboOption.getPriority(), currentComboFoodLayout);

        }
        for (final ComboDish comboDish: combo.getComboDishes()) {
            final LinearLayout comboDishLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.repeatable_combo_description_combo_dish, fillLayout, false);
            NetworkImageView comboDishPicture = (NetworkImageView) comboDishLayout.findViewById(R.id.image);
            comboDishPicture.setImageUrl(comboDish.getDish().getPicture(), imageLoader);
            comboDishPicture.getLayoutParams().height = displayMetrics.widthPixels/2 - (int)(10 * getResources().getDisplayMetrics().density);
            ((TextView) comboDishLayout.findViewById(R.id.name)).setText(comboDish.getDish().getName());
            ((TextView) comboDishLayout.findViewById(R.id.description)).setText(comboDish.getDish().getDescription());
            ((TextView) comboDishLayout.findViewById(R.id.restaurant_name)).setText(comboDish.getDish().getRestaurant().getName());
            ((NetworkImageView) comboDishLayout.findViewById(R.id.restaurant_logo)).setImageUrl(comboDish.getDish().getRestaurant().getLogo(), imageLoader);
            ImageView foodLabel = (ImageView) comboDishLayout.findViewById(R.id.label);
            switch(comboDish.getDish().getLabel()) {
                case EGG: foodLabel.setColorFilter(ContextCompat.getColor(this, R.color.egg)); break;
                case VEG: foodLabel.setColorFilter(ContextCompat.getColor(this, R.color.veg)); break;
                case NON_VEG: foodLabel.setColorFilter(ContextCompat.getColor(this, R.color.non_veg)); break;
            }
            LinearLayout countLayout = (LinearLayout) comboDishLayout.findViewById(R.id.count_layout);
            final TextView count = (TextView) countLayout.findViewById(R.id.count);
            count.setText(String.valueOf(comboDish.getQuantity()));
            countLayout.findViewById(R.id.plus).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!comboDish.incrementQuantity()) Alerts.maxCountAlert(ComboDescriptionActivity.this, comboDish);
                    count.setText(String.valueOf(comboDish.getQuantity()));
                    updatePrice();
                }
            });
            countLayout.findViewById(R.id.minus).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!comboDish.decrementQuantity()) Alerts.minCountAlert(ComboDescriptionActivity.this, comboDish);
                    count.setText(String.valueOf(comboDish.getQuantity()));
                    updatePrice();
                }
            });
            layoutOrderTreeMap.put(comboDish.getPriority(), comboDishLayout);
        }
        for (LinearLayout comboFoodLayout : layoutOrderTreeMap.values())
            fillLayout.addView(comboFoodLayout);
        updatePrice();
    }

    private void updatePrice() {
        float price = 0;
        for(ComboDish comboDish: combo.getComboDishes())
            price += comboDish.getDish().getPrice() * comboDish.getQuantity();
        for(ComboOption comboOption: combo.getComboOptions())
            for(ComboDish comboDish: comboOption.getSelectedComboOptionDishes())
                price += comboDish.getDish().getPrice()*comboDish.getQuantity();
        currentPrice.setText(String.valueOf((int)price));
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buy:
                cart.addToCart(new Combo(combo));
                Snackbar.make(parentLayout, "Added to Cart", Snackbar.LENGTH_SHORT)
                    .setAction("Undo", new View.OnClickListener() { @Override public void onClick(View v) { cart.decrementFromCart(combo); Actions.updateCartCount(cartCount); } })
                    .show();
                Actions.updateCartCount(cartCount);
                break;
        }
    }

}
