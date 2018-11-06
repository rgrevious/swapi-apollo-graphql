package com.assignment.philo.starwars;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.swapi.SWFilmsQuery;
import com.apollographql.apollo.swapi.SWPeopleFilmsQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;

/**
 * A fragment representing a single Film detail screen.
 * This fragment is either contained in a {@link FilmListActivity}
 * in two-pane mode (on tablets) or a {@link FilmDetailActivity}
 * on handsets.
 */
public class FilmDetailFragment extends Fragment {
    /**
     * The fragment argument representing the film ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";


    private SWPeopleFilmsQuery.Film mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FilmDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = FilmListActivity.SimpleItemRecyclerViewAdapter.mFilmMap.get(getArguments().getString(ARG_ITEM_ID));

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(mItem.title());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.character_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            TextView details = ((TextView) rootView.findViewById(R.id.character_detail));
            details.setText(
                    "release date: " +  mItem.releaseDate()
                    + "\n\n director: " + mItem.director()

            );
        }
        //there are no more films to show and we are already in the featured character films activity
        LinearLayout filmsLinkLayout = ((LinearLayout) rootView.findViewById(R.id.film_link_layout));
        if(filmsLinkLayout!=null) {
            filmsLinkLayout.setVisibility(View.GONE);
        }

        return rootView;
    }

    /**
     * An activity representing a list of Films filtered by a single Character. This activity
     * has different presentations for handset and tablet-size devices. On
     * handsets, the activity presents a list of items, which when touched,
     * lead to a {@link FilmDetailActivity} representing
     * film details. On tablets, the activity presents the list of films and
     * film details side-by-side using two vertical panes.
     *
     * The films are queried from a swapi graphql end point hosted by my harokuapp service
     */
    public static class FilmListActivity extends AppCompatActivity {

        private static final String BASE_URL = "https://swapi-graphql-demo.herokuapp.com/";
        String TAG = "SWAPI";
        public static FilmListActivity.SimpleItemRecyclerViewAdapter mFilmsAdapter = null;
        /**
         * Whether or not the activity is in two-pane mode, i.e. running on a tablet
         * device.
         */
        private boolean mTwoPane;

        EditText mSearchBox = null;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_character_list);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            toolbar.setTitle(getTitle());
            //Films screen will be considered a child of the Characters home screen
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                   // TODO: add home button action
                }
            });

            if (findViewById(R.id.character_detail_container) != null) {
                // The detail container view will be present only in the
                // large-screen layouts (res/values-w900dp).
                // If this view is present, then the
                // activity should be in two-pane mode.
                mTwoPane = true;
            }

            final View recyclerView = findViewById(R.id.character_list);
            assert recyclerView != null;
            setupRecyclerView((RecyclerView) recyclerView);

            //intialize Apollo once if it hasn't been already
            if(!SWApolloClient.isReady())
                SWApolloClient.init(this,"db_sw_results");

            //get the list of star wars films by character if the list is empty
            if(mFilmsAdapter != null && mFilmsAdapter.getItemCount() <=0 ) {
                SWApolloClient.queryFilmsByCharacter(getIntent().getStringExtra(ARG_ITEM_ID),new SWApolloClient.OnQueryResults() {

                    @Override
                    public <T> void onResponseList(List<T> results) {
                        mFilmsAdapter.updateData((List<SWPeopleFilmsQuery.Film>) results);
                        FilmDetailFragment.FilmListActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mFilmsAdapter.notifyDataSetChanged();
                            }
                        });
                    }

                    //update the title bar to reflect the films this character is in
                    @Override
                    public <T> void onResponseItem(final T item) {
                        FilmDetailFragment.FilmListActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView toolbarTitle = (TextView) findViewById(R.id.toolbar_title);
                                toolbarTitle.setText(((SWPeopleFilmsQuery.Person)item).name() + " Films");
                                toolbarTitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                            }
                        });
                    }
                });
            }
        }

        @Override
        public boolean onSupportNavigateUp(){
            finish();
            return true;
        }

        private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
            if(mFilmsAdapter == null)
                mFilmsAdapter = new SimpleItemRecyclerViewAdapter(this, new ArrayList<SWPeopleFilmsQuery.Film>(), mTwoPane);
            recyclerView.setAdapter(mFilmsAdapter);
            if(mSearchBox == null) {
                mSearchBox = (EditText) findViewById(R.id.search_box);
                mSearchBox.setHint("Type in a Star Wars Movie");
                mSearchBox.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        mFilmsAdapter.getFilter().filter(s.toString());
                    }

                    @Override
                    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                                  int arg3) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void afterTextChanged(Editable arg0) {
                        // TODO Auto-generated method stub

                    }
                });
            }
            mSearchBox.setText((CharSequence) mFilmsAdapter.getConstraint());
        }

        /**
         * A recycler list representing films by a featured Character
         */
        public static class SimpleItemRecyclerViewAdapter
                extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>
                implements Filterable
        {

            private final FilmListActivity mParentActivity;
            private List<SWPeopleFilmsQuery.Film> mValues;
            private List<SWPeopleFilmsQuery.Film> mFilteredResult = new ArrayList<SWPeopleFilmsQuery.Film>();
            public static Map<String,SWPeopleFilmsQuery.Film> mFilmMap = new HashMap<String, SWPeopleFilmsQuery.Film>();
            private CustomFilter mFilter;
            private final boolean mTwoPane;
            private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SWPeopleFilmsQuery.Film film = (SWPeopleFilmsQuery.Film) view.getTag();
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(ARG_ITEM_ID, film.id());
                        FilmDetailFragment fragment = new FilmDetailFragment();
                        fragment.setArguments(arguments);
                        mParentActivity.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.character_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = view.getContext();
                        Intent intent = new Intent(context, FilmDetailActivity.class);
                        intent.putExtra(ARG_ITEM_ID, film.id());

                        context.startActivity(intent);
                    }
                }
            };

            SimpleItemRecyclerViewAdapter(FilmListActivity parent,
                                          List<SWPeopleFilmsQuery.Film> items,
                                          boolean twoPane) {
                mValues = items;
                mParentActivity = parent;
                mTwoPane = twoPane;
                mFilter = new CustomFilter(SimpleItemRecyclerViewAdapter.this);
            }

            @Override
            public Filter getFilter() {
                return mFilter;
            }

            public String getConstraint(){
                return mFilter.getConstraint();
            }

            void updateData(List<SWPeopleFilmsQuery.Film> items){
                mValues = items;
                for (SWPeopleFilmsQuery.Film film : mValues) {
                    mFilmMap.put(film.id(), film);
                }
                //apply the last filter used on the new data
                mFilter.refresh();
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.character_list_content, parent, false);
                return new ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(final ViewHolder holder, int position) {
                holder.mIdView.setText(mFilteredResult.get(position).id());
                holder.mContentView.setText(mFilteredResult.get(position).title());

                holder.itemView.setTag(mFilteredResult.get(position));
                holder.itemView.setOnClickListener(mOnClickListener);
            }

            @Override
            public int getItemCount() {
                return mFilteredResult.size();
            }

            public SWPeopleFilmsQuery.Film getCharacter(String id){
                return mFilmMap.get(id);
            }

            class ViewHolder extends RecyclerView.ViewHolder {
                final TextView mIdView;
                final TextView mContentView;

                ViewHolder(View view) {
                    super(view);
                    mIdView = (TextView) view.findViewById(R.id.id_text);
                    mContentView = (TextView) view.findViewById(R.id.content);
                }
            }
            public class CustomFilter extends Filter {
                private SimpleItemRecyclerViewAdapter mAdapter;
                private String mConstraint = "";

                private CustomFilter(SimpleItemRecyclerViewAdapter mAdapter) {
                    super();
                    this.mAdapter = mAdapter;
                }
                public String getConstraint(){
                    return mConstraint;
                }
                public void refresh(){
                    filter(mConstraint);
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    mFilteredResult.clear();
                    final FilterResults results = new FilterResults();

                    if (constraint.length() == 0) {
                        mFilteredResult.addAll(mValues);
                    } else {
                        final String filterPattern = constraint.toString().toLowerCase().trim();
                        //O(n*m). average case (n*2) where m = first and last name
                        for (final SWPeopleFilmsQuery.Film film : mValues) {//n characters
                            //search first and last name or any number of formal names(m)
                            String[] fullName = film.title().split("\\s+");
                            for (final String name : fullName) {//m names each character has
                                if (name.toLowerCase().startsWith(filterPattern)) {
                                    mFilteredResult.add(film);
                                }
                            }
                        }
                        mConstraint = filterPattern;
                    }

                    System.out.println("Count Number " + mFilteredResult.size());
                    results.values = mFilteredResult;
                    results.count = mFilteredResult.size();

                    return results;

                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    System.out.println("Count Number 2 " + ((List<SWPeopleFilmsQuery.Film>) results.values).size());
                    this.mAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
