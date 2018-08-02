package io.mapwize.app;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import io.mapwize.mapwizeformapbox.model.Place;
import io.mapwize.mapwizeformapbox.model.PlaceList;
import io.mapwize.mapwizeformapbox.model.Translation;
import io.mapwize.mapwizeformapbox.model.Venue;

public class SearchResultsListAdapter extends RecyclerView.Adapter<SearchResultsListAdapter.SearchItemViewHolder> {


    private List mSearchSuggestions = new ArrayList<>();
    private Context mContext;
    private Listener mListener;
    private String language = "en";

    SearchResultsListAdapter(Context context) {
        this.mContext = context;
    }

    void swapData(List searchSuggestions) {
        mSearchSuggestions = searchSuggestions;
        notifyDataSetChanged();
    }

    void setListener(Listener listener) {
        mListener = listener;
    }

    void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public SearchItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_results_list_item, parent, false);

        return new SearchItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SearchItemViewHolder holder, int position) {

        holder.itemView.setClickable(true);
        Object suggestionItem = mSearchSuggestions.get(position);
        NumberFormat nf = new DecimalFormat("##.###");
        if (suggestionItem instanceof Place) {
            Place place = (Place) suggestionItem;
            Translation translation = place.getTranslation(language);
            holder.titleView.setText(translation.getTitle());
            if (translation.getSubtitle() != null && translation.getSubtitle().length() > 0) {
                holder.subtitleView.setText(translation.getSubtitle());
                holder.subtitleView.setVisibility(View.VISIBLE);
            }
            else {
                holder.subtitleView.setVisibility(View.GONE);
            }
            String floorPlaceHolder = mContext.getResources().getString(R.string.FLOOR_PLACEHOLDER);
            holder.floorView.setText(String.format(floorPlaceHolder, nf.format(place.getFloor())));
            holder.floorView.setVisibility(View.VISIBLE);
            if (place.getIcon()!= null && place.getIcon().length() > 0) {
                Picasso.get().load(place.getIcon()).into(holder.leftIcon);
            }
        }

        if (suggestionItem instanceof PlaceList) {
            PlaceList placeList = (PlaceList) suggestionItem;
            Translation translation = placeList.getTranslation(language);
            holder.titleView.setText(translation.getTitle());
            if (translation.getSubtitle() != null && translation.getSubtitle().length() > 0) {
                holder.subtitleView.setText(translation.getSubtitle());
                holder.subtitleView.setVisibility(View.VISIBLE);
            }
            else {
                holder.subtitleView.setVisibility(View.GONE);
            }
            holder.floorView.setVisibility(View.GONE);
            if (placeList.getIcon() != null && placeList.getIcon().length() > 0) {
                Picasso.get().load(placeList.getIcon()).into(holder.leftIcon);
            }
        }

        if (suggestionItem instanceof Venue) {
            Venue venue = (Venue) suggestionItem;
            Translation translation = venue.getTranslation(language);
            holder.titleView.setText(translation.getTitle());
            holder.subtitleView.setVisibility(View.GONE);
            holder.floorView.setVisibility(View.GONE);
            Picasso.get().load(venue.getIcon()).into(holder.leftIcon);
        }

        if (suggestionItem instanceof String) {
            String string = (String) suggestionItem;
            if (string.equals("current_location")) {
                holder.titleView.setText(mContext.getResources().getString(R.string.CURRENT_LOCATION));
                holder.subtitleView.setVisibility(View.GONE);
                holder.floorView.setVisibility(View.GONE);
                holder.leftIcon.setVisibility(View.VISIBLE);
                holder.leftIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_location_on_black_24dp));
            }
            if (string.equals("no_results")) {
                holder.itemView.setClickable(false);
                holder.titleView.setText(mContext.getResources().getString(R.string.NO_RESULTS));
                holder.subtitleView.setVisibility(View.GONE);
                holder.floorView.setVisibility(View.GONE);
                holder.leftIcon.setVisibility(View.INVISIBLE);
            }
        }

    }

    @Override
    public int getItemCount() {
        return mSearchSuggestions != null ? mSearchSuggestions.size() : 0;
    }

    class SearchItemViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout containerLayout;
        ImageView leftIcon;
        TextView titleView;
        TextView subtitleView;
        TextView floorView;

        SearchItemViewHolder(View itemView) {
            super(itemView);
            containerLayout = itemView.findViewById(R.id.suggestions_item_container);
            leftIcon = itemView.findViewById(R.id.suggestions_item_icon);
            titleView = itemView.findViewById(R.id.suggestions_item_title);
            subtitleView = itemView.findViewById(R.id.suggestions_item_subtitle);
            floorView = itemView.findViewById(R.id.suggestions_item_floor);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int adapterPosition = getAdapterPosition();
                    if (mListener != null && adapterPosition != RecyclerView.NO_POSITION) {
                        mListener.onItemSelected(mSearchSuggestions.get(adapterPosition));
                    }
                }
            });
        }

    }

    public interface Listener {

        void onItemSelected(Object item);

    }
}
