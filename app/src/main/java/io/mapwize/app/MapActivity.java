package io.mapwize.app;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.indoorlocation.core.IndoorLocation;

import io.indoorlocation.core.IndoorLocationProviderListener;
import io.mapwize.mapwizeformapbox.DirectionOptions;
import io.mapwize.mapwizeformapbox.FollowUserMode;
import io.mapwize.mapwizeformapbox.MapOptions;
import io.mapwize.mapwizeformapbox.MapwizePlugin;
import io.mapwize.mapwizeformapbox.Marker;
import io.mapwize.mapwizeformapbox.UISettings;
import io.mapwize.mapwizeformapbox.api.Api;
import io.mapwize.mapwizeformapbox.api.ApiCallback;
import io.mapwize.mapwizeformapbox.api.ApiFilter;
import io.mapwize.mapwizeformapbox.api.SearchParams;
import io.mapwize.mapwizeformapbox.model.Direction;
import io.mapwize.mapwizeformapbox.model.DirectionPoint;
import io.mapwize.mapwizeformapbox.model.LatLngFloor;
import io.mapwize.mapwizeformapbox.model.MapwizeObject;
import io.mapwize.mapwizeformapbox.model.ParsedUrlObject;
import io.mapwize.mapwizeformapbox.model.Place;
import io.mapwize.mapwizeformapbox.model.PlaceList;
import io.mapwize.mapwizeformapbox.model.Route;
import io.mapwize.mapwizeformapbox.model.Style;
import io.mapwize.mapwizeformapbox.model.Translation;
import io.mapwize.mapwizeformapbox.model.Universe;
import io.mapwize.mapwizeformapbox.model.Venue;
import io.mapwize.mapwizeformapbox.navigation.NavigationInfo;
import io.mapwize.mapwizeformapbox.navigation.OnNavigationUpdateListener;

import static com.mikepenz.materialize.util.UIUtils.convertDpToPixel;

public class MapActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SearchResultsListAdapter.Listener {

    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 0;

    private final int BOTTOM_PADDING = 54;

    private MapboxMap mapboxMap;
    private MapView mapView;
    private MapwizePlugin mapwizePlugin;
    private DrawerLayout drawer;
    private RelativeLayout mapRelativeLayout;
    private LinearLayout bottomInfoLayout;
    private TextView mainText;
    private TextView secondText;
    private TextView thirdText;
    private ImageView infoIcon;
    private TextView creditsTextView;
    private ImageButton directionButton;
    private ImageButton languagesButton;
    private ImageButton universesButton;
    private ImageView leftActionButton;
    private LinearLayout searchBarLayout;
    private EditText searchBarEditText;
    private RelativeLayout searchSuggestionsListLayout;
    private RecyclerView searchSuggestionsList;
    private SearchResultsListAdapter searchResultsListAdapter;
    private View backedView;
    private ImageView qrcodeButton;
    private NavigationView navigationView;
    private LinearLayout directionBottomInfoLayout;
    private ImageView directionBottomInfoAccessibleImage;
    private TextView directionDistance;
    private TextView directionTime;
    private LinearLayout directionHeaderMainLayout;
    private EditText directionHeaderFromTextView;
    private EditText directionHeaderToTextView;
    private ImageButton directionHeaderSwapButton;
    private ImageButton directionHeaderAccessibilityButton;
    private ImageButton directionHeaderBackButton;
    private ProgressBar progressBar;
    private BottomSheetBehavior mBottomSheetBehavior;
    private View bottomSheet;
    private WebView placeDetailsWebView;
    private ImageView infoBottomMoreImageView;
    private SearchMode currentSearchMode = SearchMode.NONE;
    private DirectionPoint fromDirectionPoint;
    private DirectionPoint toDirectionPoint;
    private boolean isAccessible = false;
    private boolean inDirectionMode = false;
    private Map<String, FullDirectionObject> directionByVenue = new HashMap<>();
    private List<Place> promotedPlaces = new ArrayList<>();
    private LocationProvidersManager mapwizeLocationProvider;
    private Venue mCurrentVenue = null;
    private MapwizeObject shouldBeSelected;
    private boolean startedFromUrl = false;

    private IndoorLocation lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.mapwize");
        setContentView(R.layout.activity_map);
        findViews();
        mapView.onCreate(savedInstanceState);

        final MapOptions opts = new MapOptions.Builder()
                .build();

        mapwizePlugin = new MapwizePlugin(mapView, opts);
        initMapwizePluginListeners();
        mapwizePlugin.setOnDidLoadListener(new MapwizePlugin.OnDidLoadListener() {
            @Override
            public void didLoad(MapwizePlugin mapwizePlugin) {
                initInterfaceComponents();
                mapwizePlugin.setPreferredLanguage(Locale.getDefault().getLanguage());
                mapwizePlugin.setTopPadding((int)getApplicationContext().getResources().getDimension(R.dimen.mapwize_distance_to_bottom_of_searchbar));
                Intent intent = MapActivity.this.getIntent();
                String url = null;
                if (intent.getStringExtra("mapwizeUrl") != null) {
                    url = intent.getStringExtra("mapwizeUrl");
                }
                else if (intent.getDataString() != null) {
                    url = intent.getDataString();
                }
                if (url != null) {
                    startedFromUrl = true;
                    Api.getParsedUrlObject(url, new ApiCallback<ParsedUrlObject>() {
                        @Override
                        public void onSuccess(final ParsedUrlObject object) {
                            Handler uiHandler = new Handler(Looper.getMainLooper());
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    handleParsedUrl(object);
                                }
                            };
                            uiHandler.post(runnable);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            if (t != null) {
                                t.printStackTrace();
                            }
                        }
                    });
                }
                mapwizePlugin.setFollowUserMode(FollowUserMode.FOLLOW_USER);
                requestLocationPermission();
                
            }
        });
        initMapWithOptions();
        setupSearchEditTexts();
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            setupSearchResult(charSequence.toString(), currentSearchMode);
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private void setupSearchEditTexts() {
        searchBarEditText.addTextChangedListener(textWatcher);
        searchBarEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    searchBarEditText.setText("");
                    currentSearchMode = SearchMode.DEFAULT;
                    openSearchTable(currentSearchMode);
                }
                else {
                    currentSearchMode = SearchMode.NONE;
                }
                setupLeftActionButton(currentSearchMode);
            }
        });
        directionHeaderFromTextView.addTextChangedListener(textWatcher);
        directionHeaderFromTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    directionHeaderFromTextView.setText("");
                    currentSearchMode = SearchMode.FROM_DIRECTION;
                    openSearchTable(currentSearchMode);
                }
                else {
                    currentSearchMode = SearchMode.NONE;
                }
            }
        });
        directionHeaderToTextView.addTextChangedListener(textWatcher);
        directionHeaderToTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    directionHeaderToTextView.setText("");
                    currentSearchMode = SearchMode.TO_DIRECTION;
                    openSearchTable(currentSearchMode);
                }
                else {
                    currentSearchMode = SearchMode.NONE;
                }
            }
        });
    }

    private void setupSearchResult(String query, SearchMode mode) {
        if (mode == SearchMode.DEFAULT) {
            if (mapwizePlugin.getVenue() == null) {
                searchVenues(query);
            }
            else {
                searchInVenue(query);
            }
        }
        if (mode == SearchMode.FROM_DIRECTION) {
            searchFrom(query);
        }

        if (mode == SearchMode.TO_DIRECTION) {
            searchInVenue(query);
        }
    }

    private void searchInVenue(String query) {
        if (query.length() > 0) {
            SearchParams params = new SearchParams.Builder()
                    .setObjectClass(new String[]{"place", "placeList"})
                        .setQuery(query)
                        .setVenueId(mapwizePlugin.getVenue().getId())
                        .setUniverseId(mapwizePlugin.getUniverseForVenue(mapwizePlugin.getVenue()).getId())
                        .build();
            Api.search(params, new ApiCallback<List<MapwizeObject>>() {
                @Override
                public void onSuccess(final List<MapwizeObject> object) {
                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (object.size() == 0) {
                                List<Object> list = new ArrayList<>();
                                list.add("no_results");
                                searchResultsListAdapter.swapData(list);
                            }
                            else {
                                searchResultsListAdapter.swapData(object);
                            }
                        }
                    };
                    uiHandler.post(runnable);
                }

                @Override
                public void onFailure(Throwable t) {
                    if (t != null) {
                        t.printStackTrace();
                    }
                }
            });
        }
        else {
            Api.getMainSearchesForVenue(mapwizePlugin.getVenue().getId(), new ApiCallback<List<MapwizeObject>>() {
                @Override
                public void onSuccess(final List<MapwizeObject> object) {
                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            searchResultsListAdapter.swapData(object);
                        }
                    };
                    uiHandler.post(runnable);
                }

                @Override
                public void onFailure(Throwable t) {
                    if (t != null) {
                        t.printStackTrace();
                    }
                }
            });
        }
    }

    private void searchFrom(String query) {
        if (query.length() > 0) {
            SearchParams params = new SearchParams.Builder()
                    .setObjectClass(new String[]{"place"})
                    .setQuery(query)
                    .setVenueId(mapwizePlugin.getVenue().getId())
                    .setUniverseId(mapwizePlugin.getUniverseForVenue(mapwizePlugin.getVenue()).getId())
                    .build();
            Api.search(params, new ApiCallback<List<MapwizeObject>>() {
                @Override
                public void onSuccess(final List<MapwizeObject> response) {
                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            List<Object> objects = new ArrayList<>();
                            if (mapwizePlugin.getUserPosition() != null && mapwizePlugin.getUserPosition().getFloor() != null) {
                                objects.add("current_location");
                            }
                            objects.addAll(response);
                            searchResultsListAdapter.swapData(objects);
                        }
                    };
                    uiHandler.post(runnable);
                }

                @Override
                public void onFailure(Throwable t) {
                    if (t != null) {
                        t.printStackTrace();
                    }
                }
            });
        }
        else {
            Api.getMainFromsForVenue(mapwizePlugin.getVenue().getId(), new ApiCallback<List<Place>>() {
                @Override
                public void onSuccess(final List<Place> object) {
                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            List<Object> objects = new ArrayList<>();
                            if (mapwizePlugin.getUserPosition() != null && mapwizePlugin.getUserPosition().getFloor() != null) {
                                objects.add("current_location");
                            }
                            objects.addAll(object);
                            searchResultsListAdapter.swapData(objects);
                        }
                    };
                    uiHandler.post(runnable);
                }

                @Override
                public void onFailure(Throwable t) {
                    if (t != null) {
                    t.printStackTrace();
                }
                }
            });
        }
    }

    private void searchVenues(String query) {

        SearchParams params = new SearchParams.Builder()
                .setObjectClass(new String[]{"venue"})
                .setQuery(query)
                .build();

        Api.search(params, new ApiCallback<List<MapwizeObject>>() {
            @Override
            public void onSuccess(final List<MapwizeObject> object) {
                Handler uiHandler = new Handler(Looper.getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (object.size() == 0) {
                            List<Object> list = new ArrayList<>();
                            list.add("no_results");
                            searchResultsListAdapter.swapData(list);
                        }
                        else {
                            searchResultsListAdapter.swapData(object);
                        }
                    }
                };
                uiHandler.post(runnable);
            }

            @Override
            public void onFailure(Throwable t) {
                if (t != null) {
                  t.printStackTrace();
                }
            }
        });
    }

    public void hideSoftKeyboard(AppCompatActivity activity) {
        if (activity == null) return;
        if (activity.getCurrentFocus() == null) return;

        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void findViews() {
        directionHeaderMainLayout = findViewById(R.id.direction_header_main_layout);
        directionHeaderFromTextView = findViewById(R.id.direction_header_from_text);
        directionHeaderToTextView = findViewById(R.id.direction_header_to_text);
        directionHeaderSwapButton = findViewById(R.id.direction_header_swap_button);
        directionHeaderAccessibilityButton = findViewById(R.id.direction_header_accessibility_button);
        directionHeaderBackButton = findViewById(R.id.direction_header_back_button);
        directionBottomInfoLayout = findViewById(R.id.direction_info_bottom_layout);
        directionBottomInfoAccessibleImage = findViewById(R.id.direction_info_bottom_accessible_image);
        directionDistance = findViewById(R.id.direction_info_bottom_distance_text);
        directionTime = findViewById(R.id.direction_info_bottom_time_text);
        drawer = findViewById(R.id.drawer_layout);
        leftActionButton = findViewById(R.id.left_action);
        bottomInfoLayout = findViewById(R.id.info_bottom_layout);
        mainText = findViewById(R.id.info_bottom_main_text);
        secondText = findViewById(R.id.info_bottom_second_text);
        thirdText = findViewById(R.id.info_bottom_third_text);
        infoIcon = findViewById(R.id.info_bottom_icon);
        mapView = findViewById(R.id.mapboxMap);
        searchBarLayout = findViewById(R.id.search_bar_layout);
        searchBarEditText = findViewById(R.id.search_bar_edittext);
        backedView = findViewById(R.id.backedView);
        mapRelativeLayout = findViewById(R.id.relative_layout);
        creditsTextView = findViewById(R.id.credits_text_view);
        qrcodeButton = findViewById(R.id.qrcode_action);
        progressBar = findViewById(R.id.progress_bar);
        navigationView = findViewById(R.id.nav_view);
        bottomSheet = findViewById(R.id.bottom_sheet1);

        placeDetailsWebView = findViewById(R.id.place_details_webview);
        infoBottomMoreImageView = findViewById(R.id.info_bottom_more_icon);
    }

    private void initMapWithOptions() {

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mMap) {
                mapboxMap = mMap;
            }
        });
    }

    private void openSearchTable(SearchMode mode) {

        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return;
        }
        searchSuggestionsListLayout = (RelativeLayout) inflater.inflate(R.layout.search_suggestions_list_layout, searchSuggestionsListLayout);

        RelativeLayout.LayoutParams params = null;

        if (searchSuggestionsListLayout != null) {
            if (searchSuggestionsListLayout.getParent() == searchBarLayout) {
                searchBarLayout.removeView(searchSuggestionsListLayout);
            }
            if (searchSuggestionsListLayout.getParent() == mapRelativeLayout) {
                mapRelativeLayout.removeView(searchSuggestionsListLayout);
            }
        }

        if (mode == SearchMode.DEFAULT) {
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.rightMargin = (int)convertDpToPixel(4,this);
            params.leftMargin = (int)convertDpToPixel(4,this);
            searchBarLayout.addView(searchSuggestionsListLayout, params);
        }

        if (mode == SearchMode.FROM_DIRECTION || mode == SearchMode.TO_DIRECTION) {
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.rightMargin = (int)convertDpToPixel(12,this);
            params.leftMargin = (int)convertDpToPixel(12,this);
            params.addRule(RelativeLayout.BELOW, R.id.direction_header_card);
            mapRelativeLayout.addView(searchSuggestionsListLayout, params);
        }

        if (params != null) {
            searchResultsListAdapter = new SearchResultsListAdapter(this);
            searchResultsListAdapter.setListener(this);
            searchResultsListAdapter.setLanguage(mapwizePlugin.getLanguage());
            searchSuggestionsList = findViewById(R.id.search_suggestions_list);
            searchSuggestionsList.setLayoutManager(new LinearLayoutManager(this));
            searchSuggestionsList.setAdapter(searchResultsListAdapter);
            setupSearchResult("", mode);
            backedView.setVisibility(View.VISIBLE);
        }



    }

    private void setupLeftActionButton(SearchMode mode) {
        if (mode == SearchMode.NONE) {
            leftActionButton.setImageResource(R.drawable.ic_dehaze_black_24dp);
            leftActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    drawer.openDrawer(GravityCompat.START);
                }
            });
        }
        else {
            leftActionButton.setImageResource(R.drawable.ic_arrow_back_black_24dp);
            leftActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    closeSearch(null);
                }
            });
        }
    }

    @Override
    public void onItemSelected(Object item) {
        closeSearch(item);
    }

    private void closeSearch(Object object) {

        if (object != null) {
            if (currentSearchMode == SearchMode.DEFAULT) {
                if (object instanceof Venue) {
                    Venue venue = (Venue) object;
                    mapwizePlugin.centerOnVenue(venue);

                }
                if (object instanceof Place) {
                    Place place = (Place) object;
                    mapwizePlugin.centerOnPlace(place);
                    selectContent(place);
                }
                if (object instanceof PlaceList) {
                    PlaceList placeList = (PlaceList) object;
                    selectContent(placeList);
                }
                mapwizePlugin.setFollowUserMode(FollowUserMode.NONE);
            }
            if (currentSearchMode == SearchMode.FROM_DIRECTION) {
                if (object instanceof String) {
                    IndoorLocation userLocation = mapwizePlugin.getUserPosition();
                    fromDirectionPoint = new MapwizeIndoorLocation(userLocation);
                    directionHeaderFromTextView.setText(R.string.CURRENT_LOCATION);
                }
                if (object instanceof Place) {
                    Place place = (Place) object;
                    directionHeaderFromTextView.setText(place.getTranslation(mapwizePlugin.getLanguage()).getTitle());
                    fromDirectionPoint = place;
                }
                tryToStartDirection();
            }
            if (currentSearchMode == SearchMode.TO_DIRECTION) {
                if (object instanceof Place) {
                    Place place = (Place) object;
                    directionHeaderToTextView.setText(place.getTranslation(mapwizePlugin.getLanguage()).getTitle());
                    toDirectionPoint = place;
                }
                if (object instanceof PlaceList) {
                    PlaceList placeList = (PlaceList) object;
                    directionHeaderToTextView.setText(placeList.getTranslation(mapwizePlugin.getLanguage()).getTitle());
                    toDirectionPoint = placeList;
                }
                tryToStartDirection();
            }
        }

        mapRelativeLayout.removeView(searchSuggestionsListLayout);
        searchBarLayout.removeView(searchSuggestionsListLayout);
        searchSuggestionsListLayout = null;
        searchSuggestionsList.setAdapter(null);
        searchSuggestionsList = null;
        currentSearchMode = SearchMode.NONE;
        searchBarEditText.setText("");
        searchBarEditText.clearFocus();
        directionHeaderFromTextView.clearFocus();
        directionHeaderToTextView.clearFocus();
        if (object == null) {
            directionHeaderFromTextView.setText("");
            directionHeaderToTextView.setText("");
        }
        backedView.setVisibility(View.GONE);
        hideSoftKeyboard(MapActivity.this);


    }

    private void initInterfaceComponents() {
        navigationView.setNavigationItemSelectedListener(this);
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return;
        }
        RelativeLayout bottomButtonsLayout = (RelativeLayout) inflater.inflate(R.layout.bottom_buttons_layout, null);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, mapwizePlugin.followUserModeButton.getId());
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        int rightMargin = (int) getResources().getDimension(io.mapwize.mapwizeformapbox.R.dimen.mapwize_followusermode_button_size);
        rightMargin += (int) getResources().getDimension(io.mapwize.mapwizeformapbox.R.dimen.mapwize_default_component_spacing);

        params.setMargins(0, 0, rightMargin, 0);
        bottomButtonsLayout.setPadding(0,0,0, 0);
        mapwizePlugin.uiLayout.addView(bottomButtonsLayout, params);


        languagesButton = findViewById(R.id.languages_button);
        languagesButton.setElevation(8);
        languagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupLanguagesUI();
            }
        });

        universesButton = findViewById(R.id.universes_button);
        universesButton.setElevation(8);
        universesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupUniversesUI();
            }
        });

        directionButton = findViewById(R.id.direction_button);
        directionButton.setElevation(8);
        directionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupSearchDirectionUI();
                tryToStartDirection();
            }
        });

        directionHeaderSwapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapDirection();
            }
        });
        directionHeaderAccessibilityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isAccessible = !isAccessible;
                if (isAccessible) {
                    directionHeaderAccessibilityButton.setBackground(getDrawable(R.drawable.rounded_button));
                    directionHeaderAccessibilityButton.setColorFilter(Color.parseColor("#C51586"));
                } else {
                    directionHeaderAccessibilityButton.setBackgroundColor(Color.parseColor("#C51586"));
                    directionHeaderAccessibilityButton.setColorFilter(Color.parseColor("#ffffff"));
                }
                tryToStartDirection();
            }
        });
        directionHeaderBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentSearchMode != SearchMode.NONE) {
                    closeSearch(null);
                }
                else {
                    mapwizePlugin.stopNavigation();
                    mapwizePlugin.setDirection(null);
                    directionByVenue = new HashMap<>();
                    setupInVenueUI(mapwizePlugin.getVenue());
                }
            }
        });
        leftActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawer.openDrawer(GravityCompat.START);
            }
        });
        bottomInfoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        backedView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeSearch(null);
            }
        });

        creditsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCredits();
            }
        });

        qrcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapActivity.this, ScannerActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        infoBottomMoreImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        mBottomSheetBehavior = new BottomSheetBehavior();//BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setPeekHeight((int)convertDpToPixel(BOTTOM_PADDING, this));
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    unselectContent();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (mapwizePlugin.getDirection() == null) {
                    int pad = mapView.getHeight() - bottomSheet.getTop();
                    mapwizePlugin.setBottomPadding(pad);
                }
            }
        });

        CoordinatorLayout.LayoutParams coordParams = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
        coordParams.setBehavior(mBottomSheetBehavior);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheet.requestLayout();
    }

    private void initMapwizePluginListeners() {

        mapwizePlugin.addOnFloorChangeListener(new MapwizePlugin.OnFloorChangeListener() {
            @Override
            public void onFloorChange(Double floor) {
            }
        });

        mapwizePlugin.setOnMapClickListener(new MapwizePlugin.OnMapClickListener() {
            @Override
            public void onMapClick(LatLngFloor latLngFloor) {
                if (!inDirectionMode && selectedContent != null) {
                    unselectContent();
                }
            }
        });

        mapwizePlugin.setOnPlaceClickListener(new MapwizePlugin.OnPlaceClickListener() {
            @Override
            public boolean onPlaceClick(Place place) {
                if (!inDirectionMode) {
                    selectContent(place);
                }

                return true;
            }
        });

        mapwizePlugin.setOnVenueClickListener(new MapwizePlugin.OnVenueClickListener() {
            @Override
            public boolean onVenueClick(Venue venue) {
                mapwizePlugin.centerOnVenue(venue);
                return false;

            }
        });

        mapwizePlugin.setOnVenueEnterListener(new MapwizePlugin.OnVenueEnterListener() {
            @Override
            public void onVenueEnter(Venue venue) {
                if (directionByVenue.get(venue.getId()) != null) {
                    FullDirectionObject o = directionByVenue.get(venue.getId());
                    setupSearchDirectionUI();
                    startDirection(o.from, o.to, o.direction, false);
                }
                else {
                    setupInVenueUI(venue);
                    mapwizePlugin.stopNavigation();
                    mapwizePlugin.setDirection(null);
                    directionByVenue = new HashMap<>();
                }

            }

            @Override
            public void willEnterInVenue(Venue venue) {
                setupEnteringInVenue(venue);
            }
        });

        mapwizePlugin.setOnVenueExitListener(new MapwizePlugin.OnVenueExitListener() {
            @Override
            public void onVenueExit(Venue venue) {
                setupInVenueUI(null);
            }
        });

        mapwizePlugin.setOnFollowUserModeChangeListener(new MapwizePlugin.OnFollowUserModeChange() {
            @Override
            public void followUserModeChange(int followUserMode) {

                if (followUserMode == FollowUserMode.FOLLOW_USER_AND_HEADING) {
                    mapboxMap.setCameraPosition(new CameraPosition.Builder().tilt(45).build());

                }

                if (followUserMode == FollowUserMode.FOLLOW_USER) {
                    mapboxMap.setCameraPosition(new CameraPosition.Builder().tilt(0).build());
                }
            }
        });

    }

    private void setupLanguagesUI() {
        if (mapwizePlugin.getVenue() != null) {
            List<String> languages = mapwizePlugin.getVenue().getSupportedLanguages();
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
            builderSingle.setIcon(R.drawable.ic_language_black_24dp);
            builderSingle.setTitle(R.string.LANGUAGE_SELECTION);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice);
            int i = 0;
            int selected = 0;
            for (String language : languages) {
                arrayAdapter.add(language.toUpperCase());
                if (language.equals(mapwizePlugin.getLanguage())) {
                    selected = i;
                }
                i++;
            }

            builderSingle.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builderSingle.setSingleChoiceItems(arrayAdapter, selected, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String strName = arrayAdapter.getItem(which);
                    if (strName != null) {
                        mapwizePlugin.setLanguageForVenue(strName.toLowerCase(), mapwizePlugin.getVenue());
                    }
                    dialog.dismiss();
                }
            });
            builderSingle.show();
        }
    }

    private void setupUniversesUI() {
        if (mapwizePlugin.getVenue() != null) {
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(MapActivity.this);
            builderSingle.setIcon(R.drawable.ic_apps_black_24dp);
            builderSingle.setTitle(R.string.UNIVERSE_SELECTION);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MapActivity.this, android.R.layout.select_dialog_singlechoice);
            int i = 0;
            int selected = 0;
            for (Universe universe : mapwizePlugin.getVenue().getUniverses()) {
                arrayAdapter.add(universe.getName());
                if (universe.equals(mapwizePlugin.getUniverseForVenue(mapwizePlugin.getVenue()))) {
                    selected = i;
                }
                i++;
            }

            builderSingle.setSingleChoiceItems(arrayAdapter, selected, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    String strName = arrayAdapter.getItem(which);
                    for (Universe u : mapwizePlugin.getVenue().getUniverses()) {
                        if (u.getName().equals(strName)) {
                            mapwizePlugin.setUniverseForVenue(u, mapwizePlugin.getVenue());
                        }
                    }
                    dialogInterface.dismiss();
                }
            });

            builderSingle.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builderSingle.show();
        }
    }

    private void setupEnteringInVenue(Venue venue) {
        String formatString = getResources().getString(R.string.ENTERING_IN_VENUE_PLACEHOLDER);
        searchBarEditText.setHint(String.format(formatString, venue.getTranslation(mapwizePlugin.getLanguage()).getTitle()));
        progressBar.setVisibility(View.VISIBLE);
        leftActionButton.setVisibility(View.GONE);
    }

    private void setupInVenueUI(Venue venue) {
        if (venue == null) {
            unselectContent();
        }
        else if (shouldBeSelected != null) {
            if (shouldBeSelected.getClass().equals(Place.class)) {
                Place p = (Place) shouldBeSelected;
                if (p.getVenueId().equals(venue.getId())) {
                    selectContent(shouldBeSelected);
                    shouldBeSelected = null;
                }
            }
        }
        progressBar.setVisibility(View.GONE);
        leftActionButton.setVisibility(View.VISIBLE);
        mCurrentVenue = venue;
        mapwizePlugin.setTopPadding((int)getApplicationContext().getResources().getDimension(R.dimen.mapwize_distance_to_bottom_of_searchbar));
        directionHeaderMainLayout.setVisibility(View.GONE);
        ValueAnimator heightAnimator = ValueAnimator.ofInt(mapwizePlugin.getBottomPadding(), 0);
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)directionBottomInfoLayout.getLayoutParams();
                params.height = (int)animation.getAnimatedValue();
                directionBottomInfoLayout.setLayoutParams(params);
                mapwizePlugin.setBottomPadding((int)animation.getAnimatedValue());
            }
        });
        heightAnimator.setDuration(250);
        heightAnimator.start();

        searchBarLayout.setVisibility(View.VISIBLE);
        inDirectionMode = false;
        for (Place p : promotedPlaces) {
            mapwizePlugin.removePromotedPlace(p);
        }
        promotedPlaces.clear();

        if (venue != null) {
            if (venue.getSupportedLanguages().size() > 1) {
                languagesButton.setVisibility(View.VISIBLE);
            }
            if (venue.getUniverses().size() > 1) {
                universesButton.setVisibility(View.VISIBLE);
            }
            directionButton.setVisibility(View.VISIBLE);
            String formatString = getResources().getString(R.string.SEARCH_IN_VENUE_PLACEHOLDER);
            searchBarEditText.setHint(String.format(formatString, venue.getTranslation(mapwizePlugin.getLanguage()).getTitle()));

        } else {
            directionButton.setVisibility(View.GONE);
            languagesButton.setVisibility(View.GONE);
            universesButton.setVisibility(View.GONE);
            searchBarEditText.setHint(getResources().getString(R.string.SEARCH_VENUE));
        }
    }

    private void swapDirection() {
        DirectionPoint tmp = fromDirectionPoint;
        fromDirectionPoint = toDirectionPoint;
        toDirectionPoint = tmp;

        CharSequence stmp = directionHeaderFromTextView.getText();
        directionHeaderFromTextView.setText(directionHeaderToTextView.getText());
        directionHeaderToTextView.setText(stmp);

        if (fromDirectionPoint == null) {
            directionHeaderFromTextView.setText(R.string.CHOOSE_FROM);
        }
        if (toDirectionPoint == null) {
            directionHeaderToTextView.setText(R.string.CHOOSE_TO);
        }

        tryToStartDirection();
    }

    private void setupSearchDirectionUI() {
        mapwizePlugin.setTopPadding((int)getApplicationContext().getResources().getDimension(R.dimen.mapwize_distance_to_bottom_of_directionsearchbar));
        inDirectionMode = true;
        searchBarLayout.setVisibility(View.GONE);
        directionHeaderMainLayout.setVisibility(View.VISIBLE);
        directionButton.setVisibility(View.GONE);
        languagesButton.setVisibility(View.GONE);
        universesButton.setVisibility(View.GONE);
        if (isAccessible) {
            directionHeaderAccessibilityButton.setColorFilter(Color.parseColor("#882E88"));
        } else {
            directionHeaderAccessibilityButton.setColorFilter(Color.parseColor("#ffffff"));
        }

        UiSettings settings = mapboxMap.getUiSettings();
        settings.setCompassMargins(settings.getCompassMarginLeft(), 300, settings.getCompassMarginRight(), settings.getCompassMarginBottom());

        if (mapwizePlugin.getUserPosition() != null && mapwizePlugin.getUserPosition().getFloor() != null) {
            fromDirectionPoint = new MapwizeIndoorLocation(mapwizePlugin.getUserPosition());
            directionHeaderFromTextView.setText(R.string.CURRENT_LOCATION);
        } else {
            fromDirectionPoint = null;
            directionHeaderFromTextView.setText("");
        }

        if (selectedContent != null) {
            if (selectedContent instanceof Place) {
                toDirectionPoint = (Place) selectedContent;
            }
            if (selectedContent instanceof PlaceList) {
                toDirectionPoint = (PlaceList) selectedContent;
            }
            directionHeaderToTextView.setText(selectedContent.getTranslation(mapwizePlugin.getLanguage()).getTitle());
        } else {
            toDirectionPoint = null;
            directionHeaderToTextView.setText("");
        }
    }

    private void startDirection(DirectionPoint fromPoint, DirectionPoint toPoint, Direction direction, boolean fitBounds) {

        DirectionOptions.Builder optsBuilder = new DirectionOptions.Builder();
        if (!fitBounds) {
            optsBuilder.centerOnStart(false);
            optsBuilder.setToStartingFloor(false);
        }

        mapwizePlugin.setFollowUserMode(FollowUserMode.NONE);
        if (mapwizePlugin.getDirection() != direction) {
            if (fromDirectionPoint instanceof MapwizeIndoorLocation) {
                mapwizePlugin.startNavigation(direction, optsBuilder.build(), new OnNavigationUpdateListener() {
                    @Override
                    public void onUpdate(NavigationInfo navigationInfo) {
                        long time = Math.round(navigationInfo.getDuration() / 60);
                        String timPlaceHolder = getString(R.string.DIRECTION_TIME_PLACEHOLDER);
                        directionTime.setText(String.format(timPlaceHolder,time));
                        directionDistance.setText(UnitLocale.distanceAsString(navigationInfo.getDistance()));
                        if (navigationInfo.getLocationDelta() > 10) {
                            fromDirectionPoint = new MapwizeIndoorLocation(mapwizeLocationProvider.getLastLocation());
                            tryToStartDirection();
                        }
                    }
                });
            }
            else {
                mapwizePlugin.setDirection(direction);
            }
        }
        unselectContent();
        if (fromPoint instanceof Place) {
            Place place = (Place)fromPoint;
            mapwizePlugin.addPromotedPlace(place);
            promotedPlaces.add(place);
        }
        if (toPoint instanceof Place) {
            Place place = (Place)toPoint;
            mapwizePlugin.addPromotedPlace(place);
            promotedPlaces.add(place);
        }

        if (fromPoint instanceof Place) {
            MapwizeObject o = (MapwizeObject) fromPoint;
            directionHeaderFromTextView.setText(o.getTranslation(mapwizePlugin.getLanguage()).getTitle());
        }
        else if (fromPoint instanceof MapwizeIndoorLocation) {
            directionHeaderFromTextView.setText(getResources().getString(R.string.CURRENT_LOCATION));
        }
        else {
            directionHeaderFromTextView.setText(getResources().getString(R.string.COORDINATES));
        }
        if (toPoint instanceof Place || toPoint instanceof PlaceList) {
            MapwizeObject o = (MapwizeObject) toPoint;
            directionHeaderToTextView.setText(o.getTranslation(mapwizePlugin.getLanguage()).getTitle());
        }
        else {
            directionHeaderToTextView.setText(getResources().getString(R.string.COORDINATES));
        }
        ValueAnimator heightAnimator = ValueAnimator.ofInt(mapwizePlugin.getBottomPadding(), (int)convertDpToPixel(BOTTOM_PADDING, this));
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)directionBottomInfoLayout.getLayoutParams();
                params.height = (int)animation.getAnimatedValue();
                directionBottomInfoLayout.setLayoutParams(params);
                mapwizePlugin.setBottomPadding((int)animation.getAnimatedValue());
            }
        });
        heightAnimator.setDuration(250);
        heightAnimator.start();

        directionBottomInfoLayout.setVisibility(View.VISIBLE);
        directionBottomInfoAccessibleImage.setImageResource(!isAccessible ? R.drawable.ic_directions_walk_black_24dp : R.drawable.ic_accessible_black_24dp);
        long time = Math.round(direction.getTraveltime() / 60);
        String timPlaceHolder = getString(R.string.DIRECTION_TIME_PLACEHOLDER);
        directionTime.setText(String.format(timPlaceHolder,time));
        directionDistance.setText(UnitLocale.distanceAsString(direction.getDistance()));
        directionButton.setVisibility(View.GONE);

    }

    private void tryToStartDirection() {
        if (fromDirectionPoint != null && toDirectionPoint != null) {
            Api.getDirection(fromDirectionPoint, toDirectionPoint, isAccessible, new ApiCallback<Direction>() {
                @Override
                public void onSuccess(final Direction object) {
                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (mCurrentVenue != null) {
                                directionByVenue.put(mCurrentVenue.getId(), new FullDirectionObject(object, fromDirectionPoint, toDirectionPoint));
                            }
                            startDirection(fromDirectionPoint, toDirectionPoint, object, true);
                        }
                    };
                    uiHandler.post(runnable);

                }

                @Override
                public void onFailure(Throwable t) {
                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MapActivity.this, getResources().getString(R.string.DIRECTION_NOT_FOUND),
                                    Toast.LENGTH_LONG).show();
                        }
                    };
                    uiHandler.post(runnable);
                }
            });
        }
    }

    private List<Marker> contentSelectionMarkers = new ArrayList<>();
    private MapwizeObject selectedContent;
    private void selectContent(MapwizeObject content) {
        if (selectedContent != null) {
            if (selectedContent instanceof Place) {
                mapwizePlugin.removePromotedPlace(((Place) selectedContent));
            }
            selectedContent = null;
            for (Marker m : contentSelectionMarkers) {
                mapwizePlugin.removeMarker(m);
            }
            contentSelectionMarkers.clear();
            mapwizePlugin.removePromotedPlaces(promotedPlaces);
            promotedPlaces.clear();
        }
        Translation tr = content.getTranslation(mapwizePlugin.getLanguage());
        if (tr.getTitle().equals("")) {
            mainText.setVisibility(View.GONE);
        } else {
            mainText.setVisibility(View.VISIBLE);
            mainText.setText(tr.getTitle());
        }
        if (tr.getSubtitle().equals("")) {
            secondText.setVisibility(View.GONE);
        } else {
            secondText.setVisibility(View.VISIBLE);
            secondText.setText(tr.getSubtitle());
        }
        if (content instanceof Place) {
            Place place = (Place) content;
            contentSelectionMarkers.add(mapwizePlugin.addMarker(place));
            mapwizePlugin.addPromotedPlace(place);
            if (place.getFloor() == null) {
                thirdText.setVisibility(View.GONE);
            } else {
                thirdText.setVisibility(View.GONE);
                NumberFormat nf = new DecimalFormat("###.###");
                String floorPlaceHolder = getResources().getString(R.string.FLOOR_PLACEHOLDER);
                thirdText.setText(String.format(floorPlaceHolder, nf.format(place.getFloor())));
            }

        } else {
            thirdText.setVisibility(View.GONE);
            if (content instanceof PlaceList) {
                final PlaceList placeList = (PlaceList) content;
                contentSelectionMarkers.addAll(mapwizePlugin.addMarkers(placeList));
                for (String placeId : placeList.getPlaceIds()) {
                    Api.getPlace(placeId, new ApiCallback<Place>() {
                        @Override
                        public void onSuccess(Place object) {
                            mapwizePlugin.addPromotedPlace(object);
                            promotedPlaces.add(object);

                        }

                        @Override
                        public void onFailure(Throwable t) {

                        }
                    });
                }

            }
        }
        if (content.getIcon() != null && content.getIcon().length()>0) {
            Picasso.get().load(content.getIcon()).into(infoIcon);
        }
        bottomInfoLayout.setVisibility(View.VISIBLE);
        selectedContent = content;

        if (tr.getDetails() != null && tr.getDetails().length() > 0) {
            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = mapView.getHeight() - ((int)convertDpToPixel(23, this));
            bottomSheet.setLayoutParams(layoutParams);
            placeDetailsWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }
            });
            placeDetailsWebView.loadData(tr.getDetails(),"text/html; charset=utf-8", "UTF-8");
            infoBottomMoreImageView.setVisibility(View.VISIBLE);
        }
        else {
            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = Math.round(convertDpToPixel(BOTTOM_PADDING, this));
            bottomSheet.setLayoutParams(layoutParams);
            infoBottomMoreImageView.setVisibility(View.GONE);
        }
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void unselectContent() {
        if (selectedContent != null) {
            if (selectedContent instanceof Place) {
                mapwizePlugin.removePromotedPlace(((Place) selectedContent));
            }
            selectedContent = null;
            for (Marker m : contentSelectionMarkers) {
                mapwizePlugin.removeMarker(m);
            }
            contentSelectionMarkers.clear();
            mapwizePlugin.removePromotedPlaces(promotedPlaces);
            promotedPlaces.clear();
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void showCredits() {
        drawer.closeDrawer(GravityCompat.START);
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);

                return true;
            }
        });
        webView.loadUrl("file:///android_asset/credits.html");
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Credits");
        alert.setView(webView);
        alert.setNegativeButton(getResources().getString(R.string.CLOSE), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void handleAccessKey(String result) {
        mapwizePlugin.grantAccess(result, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean object) {
                Handler uiHandler = new Handler(Looper.getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        mCurrentVenue = mapwizePlugin.getVenue();
                        if (mCurrentVenue != null && mCurrentVenue.getUniverses().size() > 1) {
                            universesButton.setVisibility(View.VISIBLE);
                        }

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this);
                        alertDialog.setTitle(R.string.ACCESS_KEY);
                        alertDialog.setMessage(R.string.ACCESS_GRANTED);
                        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alertDialog.show();

                        if (mapwizeLocationProvider != null) {
                            ApiFilter apiFilter = new ApiFilter.Builder().build();
                            Api.getVenues(apiFilter, new ApiCallback<List<Venue>>() {
                                @Override
                                public void onSuccess(List<Venue> object) {
                                    mapwizeLocationProvider.setVenues(object);
                                    mapwizeLocationProvider.checkVenueForIndoorLocationActivation();
                                }

                                @Override
                                public void onFailure(Throwable t) {

                                }
                            });
                        }
                    }
                };
                uiHandler.post(runnable);
            }

            @Override
            public void onFailure(Throwable t) {
                Handler uiHandler = new Handler(Looper.getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this);
                        alertDialog.setTitle(R.string.ACCESS_KEY);
                        alertDialog.setMessage(R.string.ACCESS_INVALID);
                        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alertDialog.show();
                    }
                };
                uiHandler.post(runnable);
            }
        });

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.access_key) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this);
            alertDialog.setTitle(R.string.ACCESS_KEY);
            alertDialog.setMessage(R.string.ACCESS_KEY_EXPLANATION);
            final EditText input = new EditText(MapActivity.this);
            input.setMaxLines(1);
            input.setSingleLine(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            alertDialog.setView(input);
            alertDialog.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, int which) {
                            handleAccessKey(input.getText().toString());
                        }
                    });

            alertDialog.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });


            final AlertDialog d = alertDialog.create();
            Window window = d.getWindow();
            WindowManager.LayoutParams wlp;
            if (window != null) {
                wlp = window.getAttributes();
            }
            else {
                return false;
            }
            wlp.gravity = Gravity.TOP;
            wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(wlp);
            input.setOnEditorActionListener(new TextView.OnEditorActionListener()
            {
                public boolean onEditorAction(TextView v, int keyCode, KeyEvent event)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                        case KeyEvent.KEYCODE_ENDCALL:
                            handleAccessKey(input.getText().toString());
                            d.cancel();
                            return true;
                        default:
                            break;
                    }
                    return false;
                }
            });
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            d.show();
        }

        if (id == R.id.contact_us) {
            Intent emailIntent=new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{  "support@mapwize.io"});
            emailIntent.setType("message/rfc822");
            try {
                startActivity(Intent.createChooser(emailIntent,
                        "Send email using..."));
            } catch (android.content.ActivityNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        if (id == R.id.about_mapwize) {
            String url = "https://www.mapwize.io";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        if (mapwizePlugin != null) {
            mapwizePlugin.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (mapwizePlugin != null) {
            mapwizePlugin.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_COARSE_LOCATION);
        } else {
            setupLocationProvider();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupLocationProvider();
                }
            }
        }
    }

    private IndoorLocation lastIndoorLocation = null;
    private void setupLocationProvider() {

        ApiFilter apiFilter = new ApiFilter.Builder().build();
        Api.getVenues(apiFilter, new ApiCallback<List<Venue>>() {
            @Override
            public void onSuccess(final List<Venue> object) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mapwizeLocationProvider = new LocationProvidersManager(MapActivity.this, Api.getMapwizeOkHttpClient());
                        mapwizeLocationProvider.setVenues(object);
                        mapwizePlugin.setLocationProvider(mapwizeLocationProvider);
                        if (!startedFromUrl) {
                            mapwizeLocationProvider.addListener(new IndoorLocationProviderListener() {
                                @Override
                                public void onProviderStarted() {

                                }

                                @Override
                                public void onProviderStopped() {

                                }

                                @Override
                                public void onProviderError(Error error) {

                                }

                                @Override
                                public void onIndoorLocationChange(IndoorLocation indoorLocation) {

                                }
                            });
                        }
                    }
                });

            }

            @Override
            public void onFailure(Throwable t) {
                Log.i("Error", "Error while loading venues in location provider");
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra("url");
                Api.getParsedUrlObject(result, new ApiCallback<ParsedUrlObject>() {
                    @Override
                    public void onSuccess(final ParsedUrlObject object) {
                        Handler uiHandler = new Handler(Looper.getMainLooper());
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                handleParsedUrl(object);
                            }
                        };
                        uiHandler.post(runnable);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        if (t != null) {
                            t.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    private void handleParsedUrl(final ParsedUrlObject parsedUrlObject) {
        if (parsedUrlObject.getAccessKey() != null) {
            Api.getAccess(parsedUrlObject.getAccessKey(), new ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean access) {
                    parsedUrlObject.setAccessKey(null);
                    mapwizePlugin.refresh(new MapwizePlugin.OnAsyncTaskReady() {
                        @Override
                        public void ready() {
                            Handler uiHandler = new Handler(Looper.getMainLooper());
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    handleParsedUrl(parsedUrlObject);
                                }
                            };
                            uiHandler.post(runnable);

                            ApiFilter apiFilter = new ApiFilter.Builder().build();
                            Api.getVenues(apiFilter, new ApiCallback<List<Venue>>() {
                                @Override
                                public void onSuccess(final List<Venue> object) {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mapwizeLocationProvider.setVenues(object);
                                            mapwizeLocationProvider.checkVenueForIndoorLocationActivation();
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Throwable t) {

                                }
                            });
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    if (t != null) {
                        t.printStackTrace();
                    }
                }
            });
            return;
        }

        if (parsedUrlObject.getIndoorLocation() != null) {
            if (mapwizeLocationProvider != null) {
                mapwizeLocationProvider.defineLocation(parsedUrlObject.getIndoorLocation());
            }
            else {
                lastLocation = parsedUrlObject.getIndoorLocation();
            }
        }

        if (parsedUrlObject.getLanguage() != null) {
            mapwizePlugin.setPreferredLanguage(parsedUrlObject.getLanguage());
        }

        if (parsedUrlObject.getUniverse() != null) {
            mapwizePlugin.setUniverseForVenue(parsedUrlObject.getUniverse(), null);
        }

        Double zoom = parsedUrlObject.getZoom();
        LatLngBounds bounds = parsedUrlObject.getBounds();
        if (bounds.getSouthWest().equals(bounds.getNorthEast())) {
            CameraUpdate nextPosition = CameraUpdateFactory.newLatLngZoom(bounds.getSouthWest(), zoom==null?20:zoom);
            mapboxMap.easeCamera(nextPosition);
        }
        else {
            CameraUpdate nextPosition = CameraUpdateFactory.newLatLngBounds(bounds, 10);
            CameraPosition nextCameraPosition = nextPosition.getCameraPosition(this.mapboxMap);
            if (nextCameraPosition != null && nextCameraPosition.zoom < 16) {
                nextPosition = CameraUpdateFactory.newLatLngZoom(nextCameraPosition.target, 16);
            }
            if (nextCameraPosition != null && zoom != null) {
                nextPosition = CameraUpdateFactory.newLatLngZoom(nextCameraPosition.target, zoom);
            }
            mapboxMap.easeCamera(nextPosition);
        }
        if (parsedUrlObject.getFloor() != null) {
            mapwizePlugin.setFloor(parsedUrlObject.getFloor());
        }

        if (parsedUrlObject.getPlace() != null) {
            shouldBeSelected = parsedUrlObject.getPlace();
        }

        if (parsedUrlObject.getAccessible() != null) {
            isAccessible = parsedUrlObject.getAccessible();
        }

        if (parsedUrlObject.getDirection() != null) {
            FullDirectionObject o = new FullDirectionObject(parsedUrlObject.getDirection(), parsedUrlObject.getFrom(), parsedUrlObject.getTo());
            directionByVenue.put(parsedUrlObject.getVenue().getId(), o);
        }

    }

    enum SearchMode {
        NONE,
        DEFAULT,
        FROM_DIRECTION,
        TO_DIRECTION
    }




}
