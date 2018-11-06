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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.swapi.SWPeopleQuery;
import com.apollographql.apollo.swapi.fragment.FilmDetails;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
//import com.apollographql.apollo.cache.normalized.NormalizedCache;

/**
 * A fragment representing a single Character detail screen.
 * This fragment is either contained in a {@link CharacterListActivity}
 * in two-pane mode (on tablets) or a {@link CharacterDetailActivity}
 * on handsets.
 */
public class CharacterDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";


    private SWPeopleQuery.person mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CharacterDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the character content specified by the fragment
            // arguments.
            mItem = CharacterListActivity.SimpleItemRecyclerViewAdapter.mCharacterMap.get(getArguments().getString(ARG_ITEM_ID));

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(mItem.fragments().personDetails().name());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.character_detail, container, false);

        // Show the character content as text in a TextView.
        if (mItem != null) {
            TextView details = ((TextView) rootView.findViewById(R.id.character_detail));
            details.setText(
                    "gender: " +  mItem.fragments().personDetails().gender()
                    + "\n\neye color: " +  mItem.fragments().personDetails().eyeColor()
                    + "\n\nskin color: " +  mItem.fragments().personDetails().skinColor()
                    + "\n\nhair color: " +  mItem.fragments().personDetails().hairColor()
                    + "\n\nheight: " +  mItem.fragments().personDetails().height()

            );

            TextView films = ((TextView) rootView.findViewById(R.id.films));
            if(films!=null) {
                films.setText("Find all " + mItem.fragments().personDetails().name() + " films");
                films.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = getContext();
                        Intent intent = new Intent(context, FilmDetailFragment.FilmListActivity.class);
                        intent.putExtra(ARG_ITEM_ID, mItem.fragments().personDetails().id());
                        context.startActivity(intent);
                    }
                });
            }
        }

        return rootView;
    }

    /**
     * An activity representing a list of Star Wars Characters. This activity
     * has different presentations for handset and tablet-size devices. On
     * handsets, the activity presents a list of items, which when touched,
     * lead to a {@link CharacterDetailActivity} representing
     * Star Wars character details. On tablets, the activity presents the list of items and
     * item details side-by-side using two vertical panes.
     */
    public static class CharacterListActivity extends AppCompatActivity {

        /**
         * Whether or not the activity is in two-pane mode, i.e. running on a tablet
         * device.
         */
        private boolean mTwoPane;
        public static CharacterListActivity.SimpleItemRecyclerViewAdapter mPeople = null;
        EditText mSearchBox = null;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_character_list);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            toolbar.setTitle(getTitle());

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                   //TODO: add home button functionality
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

            //get the list of star wars characters if the list is empty
            if(mPeople != null && mPeople.getItemCount() <=0 ) {
                SWApolloClient.queryAllCharacters(new SWApolloClient.OnQueryResults() {

                    @Override
                    public <T> void onResponseList(List<T> results) {
                        mPeople.updateData((List<SWPeopleQuery.person>) results);
                        CharacterDetailFragment.CharacterListActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPeople.notifyDataSetChanged();
                            }
                        });
                    }

                    //no single item will be returned for this query
                    @Override
                    public <T> void onResponseItem(T item) {
                    }
                });
            }
        }

        public SWPeopleQuery.person getCharacter(String id){
            return mPeople.getCharacter(id);
        }

        private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
            if(mPeople == null)
                mPeople = new SimpleItemRecyclerViewAdapter(this, new ArrayList<SWPeopleQuery.person>(), mTwoPane);
            recyclerView.setAdapter(mPeople);
            if(mSearchBox == null) {
                mSearchBox = (EditText) findViewById(R.id.search_box);
                mSearchBox.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        mPeople.getFilter().filter(s.toString());
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
            mSearchBox.setText((CharSequence) mPeople.getConstraint());
        }
        public static class SimpleItemRecyclerViewAdapter
                extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>
                implements Filterable
        {

            private final CharacterListActivity mParentActivity;
            private List<SWPeopleQuery.person> mValues;
            private List<SWPeopleQuery.person> mFilteredResult = new ArrayList<SWPeopleQuery.person>();
            public static Map<String,SWPeopleQuery.person> mCharacterMap = new HashMap<String, SWPeopleQuery.person>();
            private CustomFilter mFilter;
            private final boolean mTwoPane;
            private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SWPeopleQuery.person person = (SWPeopleQuery.person) view.getTag();
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(ARG_ITEM_ID, person.fragments().personDetails().id());
                        CharacterDetailFragment fragment = new CharacterDetailFragment();
                        fragment.setArguments(arguments);
                        mParentActivity.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.character_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = view.getContext();
                        Intent intent = new Intent(context, CharacterDetailActivity.class);
                        intent.putExtra(ARG_ITEM_ID, person.fragments().personDetails().id());

                        context.startActivity(intent);
                    }
                }
            };

            SimpleItemRecyclerViewAdapter(CharacterListActivity parent,
                                          List<SWPeopleQuery.person> items,
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

            void updateData(List<SWPeopleQuery.person> items){
                mValues = items;
                for (SWPeopleQuery.person character : mValues) {
                    mCharacterMap.put(character.fragments().personDetails().id(), character);
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
                holder.mIdView.setText(mFilteredResult.get(position).fragments().personDetails().id());
                holder.mContentView.setText(mFilteredResult.get(position).fragments().personDetails().name());

                holder.itemView.setTag(mFilteredResult.get(position));
                holder.itemView.setOnClickListener(mOnClickListener);
            }

            @Override
            public int getItemCount() {
                return mFilteredResult.size();
            }

            public SWPeopleQuery.person getCharacter(String id){
                return mCharacterMap.get(id);
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
                        for (final SWPeopleQuery.person character : mValues) {//n characters
                            //search first and last name or any number of formal names(m)
                            String[] fullName = character.fragments().personDetails().name().split("\\s+");
                            for (final String name : fullName) {//m names each character has
                                if (name.toLowerCase().startsWith(filterPattern)) {
                                    mFilteredResult.add(character);
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
                    System.out.println("Count Number 2 " + ((List<SWPeopleQuery.person>) results.values).size());
                    this.mAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
