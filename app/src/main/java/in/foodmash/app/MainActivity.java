package in.foodmash.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import in.foodmash.app.commons.Actions;
import in.foodmash.app.commons.Animations;
import in.foodmash.app.commons.Filters;
import in.foodmash.app.commons.Info;
import in.foodmash.app.commons.JsonProvider;
import in.foodmash.app.commons.LinearLayoutManager;
import in.foodmash.app.commons.Swift;
import in.foodmash.app.commons.VolleyFailureFragment;
import in.foodmash.app.commons.VolleyProgressFragment;
import in.foodmash.app.custom.Cart;
import in.foodmash.app.custom.Combo;
import in.foodmash.app.custom.ComboDish;
import in.foodmash.app.custom.ComboOption;
import in.foodmash.app.custom.Dish;
import in.foodmash.app.custom.Restaurant;
import in.foodmash.app.utils.DateUtils;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.fill_layout) LinearLayout fillLayout;
    @Bind(R.id.main_layout) ScrollView mainLayout;
    @Bind(R.id.empty_combo_layout) LinearLayout emptyComboLayout;
    @Bind(R.id.filter) FloatingActionButton filterFab;
    @Bind(R.id.drawer_layout) DrawerLayout drawerLayout;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.fragment_container) FrameLayout fragmentContainer;
    @Bind(R.id.filters) RecyclerView recyclerView;
    @Bind(R.id.apply_filters) TextView applyFilters;
    @Bind(R.id.remove_filters) TextView removeFilters;

    private Snackbar snackbar;
    private Intent intent;
    private TextView cartCount;
    private Cart cart = Cart.getInstance();
    private ImageLoader imageLoader;
    private DisplayMetrics displayMetrics;
    private ObjectMapper objectMapper;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    private Set<Combo.Category> categorySelected = new HashSet<>();
    private Set<Combo.Size> sizeSelected = new HashSet<>();
    private Set<Dish.Label> preferenceSelected = new HashSet<>();
    private boolean sortPriceLowToHigh = true;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Info.isLoggedIn(MainActivity.this)) getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        else getMenuInflater().inflate(R.menu.menu_main_anonymous_login, menu);
        RelativeLayout cartCountLayout = (RelativeLayout) menu.findItem(R.id.menu_cart).getActionView();
        cartCount = (TextView) cartCountLayout.findViewById(R.id.cart_count);
        Actions.updateCartCount(cartCount);
        cartCountLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(MainActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) return true;
        switch (item.getItemId()) {
            case R.id.menu_profile: intent = new Intent(this, ProfileActivity.class); startActivity(intent); return true;
            case R.id.menu_addresses: intent = new Intent(this, AddressActivity.class); startActivity(intent); return true;
            case R.id.menu_order_history: intent = new Intent(this, OrderHistoryActivity.class); startActivity(intent); return true;
            case R.id.menu_contact_us: intent = new Intent(this, ContactUsActivity.class); startActivity(intent); return true;
            case R.id.menu_log_out: Actions.logout(MainActivity.this); return true;
            case R.id.menu_login: intent = new Intent(this, LoginActivity.class); startActivity(intent); return true;
            case R.id.menu_cart: intent = new Intent(this, CartActivity.class); startActivity(intent); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        try { getSupportActionBar().setDisplayShowTitleEnabled(false); }
        catch (Exception e) { Actions.handleIgnorableException(this,e); }

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new VolleyProgressFragment()).commit();
        getSupportFragmentManager().executePendingTransactions();

        imageLoader = Swift.getInstance(MainActivity.this).getImageLoader();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onSingleTapUp(MotionEvent e) { return true; } });
        final Filters filters = new Filters();

        filters.addHeader("Deliver to");
        filters.addFilter(Info.getAreaName(this), R.drawable.svg_marker_filled);

        filters.addHeader("Type");
        filters.addFilter("Regular", R.drawable.svg_hashtag);
        filters.addFilter("Budget", R.drawable.svg_coffee);
        filters.addFilter("Corporate", R.drawable.svg_sitemap);
        filters.addFilter("Health", R.drawable.svg_heartbeat);

        filters.addHeader("Size");
        filters.addFilter("Micro", R.drawable.svg_user1);
        filters.addFilter("Medium", R.drawable.svg_user2);
        filters.addFilter("Mega", R.drawable.svg_user3);

        filters.addHeader("Preference");
        filters.addFilter("Veg", R.drawable.svg_leaf);
        filters.addFilter("Egg", R.drawable.svg_egg);
        filters.addFilter("Non-Veg", R.drawable.svg_meat);

        filters.addHeader("Price");
        filters.addFilter("Low to High", R.drawable.svg_sort_amount_asc);
        filters.addFilter("High to Low", R.drawable.svg_sort_amount_desc);

        filters.setSelected(1);
        filters.setSelected(16);

        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(filters);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {

            private void makeActive(View view, Integer position, Combo.Category category) {
                if (view.isActivated()) { categorySelected.remove(category); filters.removeSelected(position); }
                else { categorySelected.add(category); filters.setSelected(position); }
                filters.notifyItemChanged(position);
            }
            private void makeActive(View view, Integer position, Combo.Size size) {
                if (view.isActivated()) { sizeSelected.remove(size); filters.removeSelected(position); }
                else { sizeSelected.add(size); filters.setSelected(position); }
                filters.notifyItemChanged(position);
            }
            private void makeActive(View view, Integer position, Dish.Label preference) {
                if (view.isActivated()) { preferenceSelected.remove(preference); filters.removeSelected(position); }
                else { preferenceSelected.add(preference); filters.setSelected(position); }
                filters.notifyItemChanged(position);
            }

            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if(child!=null && gestureDetector.onTouchEvent(e)) {
                    switch(recyclerView.getChildAdapterPosition(child)) {
                        case 1: startActivity(new Intent(MainActivity.this, SplashActivity.class));

                        case 3: makeActive(child, recyclerView.getChildAdapterPosition(child), Combo.Category.REGULAR); break;
                        case 4: makeActive(child, recyclerView.getChildAdapterPosition(child), Combo.Category.BUDGET); break;
                        case 5: makeActive(child, recyclerView.getChildAdapterPosition(child), Combo.Category.CORPORATE); break;
                        case 6: makeActive(child, recyclerView.getChildAdapterPosition(child), Combo.Category.HEALTH); break;

                        case 8: makeActive(child, recyclerView.getChildAdapterPosition(child), Combo.Size.MICRO); break;
                        case 9: makeActive(child, recyclerView.getChildAdapterPosition(child), Combo.Size.MEDIUM); break;
                        case 10: makeActive(child, recyclerView.getChildAdapterPosition(child), Combo.Size.MEGA); break;

                        case 12: makeActive(child, recyclerView.getChildAdapterPosition(child), Dish.Label.VEG); break;
                        case 13: makeActive(child, recyclerView.getChildAdapterPosition(child), Dish.Label.EGG); break;
                        case 14: makeActive(child, recyclerView.getChildAdapterPosition(child), Dish.Label.NON_VEG); break;

                        case 16: sortPriceLowToHigh = true; if (!child.isActivated()) { child.setActivated(true); recyclerView.findViewHolderForAdapterPosition(15).itemView.setActivated(false); } break;
                        case 17: sortPriceLowToHigh = false; if (!child.isActivated()) { child.setActivated(true); recyclerView.findViewHolderForAdapterPosition(14).itemView.setActivated(false); } break;
                    }
                }
                return false;
            }

            @Override public void onTouchEvent(RecyclerView rv, MotionEvent e) { }
            @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });

        actionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open_navbar,
                R.string.close_navbar) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                Log.i("Filters",categorySelected.toString());
                Log.i("Filters",sizeSelected.toString());
                Log.i("Filters",preferenceSelected.toString());
                try {updateFillLayout(Arrays.asList(objectMapper.readValue(Info.getComboJsonArrayString(MainActivity.this), Combo[].class))); }
                catch (Exception e) { Actions.handleIgnorableException(MainActivity.this,e); }
            }
        };

        applyFilters.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { drawerLayout.closeDrawer(Gravity.LEFT); } });
        removeFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filters.clearAllSelected();
                filters.setSelected(1);
                filters.setSelected(16);
                filters.notifyDataSetChanged();
                categorySelected.clear();
                sizeSelected.clear();
                preferenceSelected.clear();
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        filterFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(drawerLayout.isDrawerOpen(Gravity.LEFT)) drawerLayout.closeDrawer(Gravity.LEFT);
                else drawerLayout.openDrawer(Gravity.LEFT);
            }
        });


        Date comboUpdatedAt = null;
        boolean areCombosOutdated = true;
        if(Info.getComboUpdatedAtDate(this)!=null) {
            try {
                comboUpdatedAt = DateUtils.railsDateStringToJavaDate(Info.getComboUpdatedAtDate(this));
                areCombosOutdated = (DateUtils.howOldInHours(comboUpdatedAt) > 6);
            } catch (Exception e) { Actions.handleIgnorableException(this,e); }
        }
        if(Info.getComboJsonArrayString(this)==null || areCombosOutdated) {
            ((VolleyProgressFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container))
                    .setLoadingText("Loading Combos...", "We are loading as fast as we can");
            Animations.fadeIn(fragmentContainer, 300);
        } else {
            try {updateFillLayout(Arrays.asList(objectMapper.readValue(Info.getComboJsonArrayString(this), Combo[].class))); }
            catch (Exception e) { Actions.handleIgnorableException(this,e); }
            snackbar = Snackbar.make(fillLayout, "Updating combos...", Snackbar.LENGTH_INDEFINITE);
            snackbar.show();
        }
        makeComboRequest();
    }

    public void makeComboRequest() {
        JsonObjectRequest getCombosRequest = new JsonObjectRequest(Request.Method.POST, getString(R.string.api_root_path) + "/combos", getComboRequestJson(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        if (snackbar!=null && snackbar.isShown()) snackbar.dismiss();
                        Animations.fadeOut(fragmentContainer,100);
                        Log.i("Combos", response.getJSONObject("data").getJSONArray("combos").length() + " combos found");
                        String comboJsonArrayString = response.getJSONObject("data").getJSONArray("combos").toString();
                        updateFillLayout(Arrays.asList(objectMapper.readValue(comboJsonArrayString, Combo[].class)));
                        Actions.cacheCombos(MainActivity.this, comboJsonArrayString, new Date());
                    } else Snackbar.make(mainLayout,"Unable to load combos: "+response.getString("error"),Snackbar.LENGTH_LONG).show();
                } catch (Exception e) { e.printStackTrace(); Actions.handleIgnorableException(MainActivity.this,e); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (snackbar!=null && snackbar.isShown()) snackbar.setText("Update Failed!");
                else {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new VolleyFailureFragment()).commit();
                    getSupportFragmentManager().executePendingTransactions();
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, VolleyFailureFragment.newInstance(error, "makeComboRequest")).commit();
                }
            }
        });
        Swift.getInstance(this).addToRequestQueue(getCombosRequest, 20000, 2, 1.0f);
    }


    private JSONObject getComboRequestJson() {
        JSONObject comboRequestJson;
        if(Info.isLoggedIn(this)) comboRequestJson = JsonProvider.getStandardRequestJson(this);
        else comboRequestJson = JsonProvider.getAnonymousRequestJson(this);
        int packagingCentreId = Info.getPackagingCentreId(this);
        try {
            JSONObject dataJson = new JSONObject();
            dataJson.put("packaging_centre_id",packagingCentreId);
            comboRequestJson.put("data", dataJson);
        }
        catch (Exception e) { Actions.handleIgnorableException(this,e); }
        return comboRequestJson;
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(MainActivity.this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle("Exit App ?")
                .setMessage("We're sad to see you go. Do you really want to exit the app?")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                }).setNegativeButton("No, lemme eat more", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).show();
    }

    private void updateFillLayout(List<Combo> combos) {
        List<Combo> filteredCombos = applyFilters(combos);
        if(filteredCombos.size()==0) { emptyComboLayout.setVisibility(View.VISIBLE); fillLayout.setVisibility(View.GONE); return; }
        else { emptyComboLayout.setVisibility(View.GONE); fillLayout.setVisibility(View.VISIBLE); }
        TreeMap<Integer, LinearLayout> comboTreeMap = new TreeMap<>();
        for (final Combo combo : filteredCombos) {
            View.OnClickListener showDescription = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intent = new Intent(MainActivity.this, ComboDescriptionActivity.class);
                    intent.putExtra("combo_id", combo.getId());
                    startActivity(intent);
                }
            };
            final LinearLayout comboLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.repeatable_main_combo, fillLayout, false);
            ((TextView) comboLayout.findViewById(R.id.id)).setText(String.valueOf(combo.getId()));
            NetworkImageView comboPicture = (NetworkImageView) comboLayout.findViewById(R.id.image);
            comboPicture.setImageUrl(combo.getPicture(), imageLoader);
            comboPicture.getLayoutParams().height = displayMetrics.widthPixels/2 - (int)(10 * getResources().getDisplayMetrics().density);
            ((TextView) comboLayout.findViewById(R.id.name)).setText(combo.getName());
            LinearLayout contentsLayout = (LinearLayout) comboLayout.findViewById(R.id.contents_layout);
            contentsLayout.setOnClickListener(showDescription);
            TreeMap<Integer, Pair<String,Dish.Label>> contents = combo.getContents();
            for (int n : contents.navigableKeySet()) {
                LinearLayout contentTextView = (LinearLayout) getLayoutInflater().inflate(R.layout.repeatable_main_combo_content, contentsLayout, false);
                String dishNameString = contents.get(n).first;
                Dish.Label dishLabel = contents.get(n).second;
                ImageView label = (ImageView) contentTextView.findViewById(R.id.label);
                switch (dishLabel) {
                    case EGG: label.setColorFilter(ContextCompat.getColor(this, R.color.egg)); break;
                    case VEG: label.setColorFilter(ContextCompat.getColor(this, R.color.veg)); break;
                    case NON_VEG: label.setColorFilter(ContextCompat.getColor(this, R.color.non_veg)); break;
                }
                ((TextView) contentTextView.findViewById(R.id.content)).setText(dishNameString);
                contentsLayout.addView(contentTextView);
            }
            ((TextView) comboLayout.findViewById(R.id.price)).setText(String.valueOf((int) combo.getPrice()));
            ImageView foodLabel = (ImageView) comboLayout.findViewById(R.id.label);
            switch (combo.getLabel()) {
                case EGG: foodLabel.setColorFilter(ContextCompat.getColor(this, R.color.egg)); break;
                case VEG: foodLabel.setColorFilter(ContextCompat.getColor(this, R.color.veg)); break;
                case NON_VEG: foodLabel.setColorFilter(ContextCompat.getColor(this, R.color.non_veg)); break;
            }
            comboLayout.findViewById(R.id.view).setOnClickListener(showDescription);
            comboLayout.findViewById(R.id.clickable_layout).setOnClickListener(showDescription);
            comboLayout.findViewById(R.id.image).setOnClickListener(showDescription);
            final TextView addToCartLayout = (TextView) comboLayout.findViewById(R.id.add_to_cart_layout);
            final LinearLayout addedToCartLayout = (LinearLayout) comboLayout.findViewById(R.id.added_to_cart_layout);
            final LinearLayout countLayout = (LinearLayout) comboLayout.findViewById(R.id.count_layout);
            final TextView count = (TextView) countLayout.findViewById(R.id.count);
            int quantity = cart.getCount(combo.getId());
            count.setText(String.valueOf(quantity));
            if (quantity > 0) {
                addedToCartLayout.setVisibility(View.VISIBLE);
                addToCartLayout.setVisibility(View.GONE);
                countLayout.setVisibility(View.VISIBLE);
            }
            TextView plus = (TextView) countLayout.findViewById(R.id.plus);
            TextView minus = (TextView) countLayout.findViewById(R.id.minus);
            plus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cart.addToCart(new Combo(combo));
                    count.setText(String.valueOf(cart.getCount(combo.getId())));
                    Actions.updateCartCount(cartCount);
                }
            });
            minus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (count.getText().toString().equals("0")) return;
                    cart.decrementFromCart(combo.getId());
                    count.setText(String.valueOf(cart.getCount(combo.getId())));
                    if (cart.getCount(combo.getId()) == 0) {
                        Animations.fadeOut(addedToCartLayout, 200);
                        Animations.fadeOut(countLayout, 200);
                        Animations.fadeIn(addToCartLayout, 200);
                    }
                    Actions.updateCartCount(cartCount);
                }
            });
            addToCartLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cart.addToCart(new Combo(combo));
                    Animations.fadeInOnlyIfInvisible(addedToCartLayout, 500);
                    Animations.fadeOut(addToCartLayout, 200);
                    Animations.fadeIn(countLayout, 200);
                    count.setText(String.valueOf(cart.getCount(combo.getId())));
                    Actions.updateCartCount(cartCount);
                }
            });

            LinearLayout restaurantsLayout = (LinearLayout) comboLayout.findViewById(R.id.restaurant_layout);
            HashSet<Restaurant> restaurantsList = new HashSet<>();
            for (ComboOption comboOption : combo.getComboOptions())
                if (comboOption.isFromSameRestaurant())
                    restaurantsList.add(comboOption.getComboOptionDishes().get(0).getDish().getRestaurant());
                else for (ComboDish comboDish : comboOption.getComboOptionDishes())
                    restaurantsList.add(comboDish.getDish().getRestaurant());
            for (ComboDish comboDish : combo.getComboDishes())
                restaurantsList.add(comboDish.getDish().getRestaurant());
            for (Restaurant restaurant : restaurantsList) {
                LinearLayout restaurantLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.repeatable_restaurant_logo, restaurantsLayout, false);
                ((TextView) restaurantLayout.findViewById(R.id.name)).setText(restaurant.getName());
                ((NetworkImageView) restaurantLayout.findViewById(R.id.logo)).setImageUrl(restaurant.getLogo(), imageLoader);
                restaurantsLayout.addView(restaurantLayout);
            }

            comboTreeMap.put((int) combo.getPrice(), comboLayout);
        }

        fillLayout.removeAllViews();
        if(sortPriceLowToHigh)
            for (int n : comboTreeMap.navigableKeySet())
                fillLayout.addView(comboTreeMap.get(n));
        else for (int n : comboTreeMap.descendingKeySet())
            fillLayout.addView(comboTreeMap.get(n));
    }

    private List<Combo> applyFilters(List<Combo> combos) {
        List<Combo> filteredComboList = new ArrayList<>();
        for(Combo combo: combos) {
            boolean survived = true;
            if(!categorySelected.isEmpty() && !categorySelected.contains(combo.getCategory())) survived = false;
            if(!sizeSelected.isEmpty() && !sizeSelected.contains(combo.getSize())) survived = false;
            if(!preferenceSelected.isEmpty() && !preferenceSelected.contains(combo.getLabel())) survived = false;
            if(survived) filteredComboList.add(combo);
        }
        return filteredComboList;
    }
}
