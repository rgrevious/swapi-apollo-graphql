package com.assignment.philo.starwars;

import android.content.Context;
import android.util.Log;

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
import com.apollographql.apollo.swapi.SWPeopleFilmsQuery;
import com.apollographql.apollo.swapi.SWPeopleQuery;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import okhttp3.OkHttpClient;



/**
 * A graphql client wrapper for Android.  The client connects to a SWAPI graphql endpoint created
 * and deployed at herokuapp for this assignment.
 * This static class wraps around the Andoid {@link ApolloClient}
 * Only one instance should be used for the duration of the app according to the ApolloClient docs.
 * "It is recommended to only create a single ApolloClient and use that for execution of all the queries."
 *  The connection implements a normalized cache policy of {@link HttpCachePolicy}.NETWORK_FIRST
 *
 */
public class SWApolloClient
{

    private static ApolloClient mApolloClient = null;
    private static final String BASE_URL = "https://swapi-graphql-demo.herokuapp.com/";
    private static final String TAG = "SWAPI";

    /**
    * An interface for simplifying query results from graphql to adapters.
     * If the query returns a list then onResponseList will be called.
     * If the resulting list is based on another swapi item id, then both
     * onResponseList and onResponseItem will be called with the item matching
     * the id that was used to query the list.
     */
    public interface OnQueryResults{
        public <T> void onResponseList(List<T> results);
        public <T> void onResponseItem(T item);
        //TODO: add failure event
    }

    public static boolean isReady(){
        return (mApolloClient!=null);
    }

    /**
     * create the single instance of {@link ApolloClient}
     */

    public static void init(Context context, String cacheFile){
        ApolloSqlHelper apolloSqlHelper = ApolloSqlHelper.create(context, cacheFile);
        //Create NormalizedCacheFactory
        NormalizedCacheFactory cacheFactory = new SqlNormalizedCacheFactory(apolloSqlHelper);
        //Create the cache key resolver, this example works well when all types have globally unique ids.
        CacheKeyResolver resolver =  new CacheKeyResolver() {
            @NotNull
            @Override
            public CacheKey fromFieldRecordSet(@NotNull ResponseField field, @NotNull Map<String, Object> recordSet) {
                return formatCacheKey((String) recordSet.get("id"));
            }

            @NotNull @Override
            public CacheKey fromFieldArguments(@NotNull ResponseField field, @NotNull Operation.Variables variables) {
                return formatCacheKey((String) field.resolveArgument("id", variables));
            }

            private CacheKey formatCacheKey(String id) {
                if (id == null || id.isEmpty()) {
                    return CacheKey.NO_KEY;
                } else {
                    return CacheKey.from(id);
                }
            }
        };

        //try to connect first before using cache results
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build();

        mApolloClient = ApolloClient.builder()
                .normalizedCache(cacheFactory, resolver)
                .serverUrl(BASE_URL)
                .okHttpClient(okHttpClient)
                .build();
    }

    /**
    * Query and cache all Star Wars characters. The result callback will return a List
     * of {@link SWPeopleQuery.person}
     */
    public static void queryAllCharacters(final OnQueryResults queryEvent){
        if(mApolloClient==null)
            return;
        if(queryEvent==null)
            return;

        mApolloClient.query(SWPeopleQuery.builder()
                .build())
                .httpCachePolicy(HttpCachePolicy.NETWORK_FIRST)
                .enqueue(new ApolloCall.Callback<SWPeopleQuery.Data>() {

                    @Override
                    public void onResponse(@Nonnull Response<SWPeopleQuery.Data> response) {
                        SWPeopleQuery.Data responseData = response.data();
                        if (responseData == null)
                            return;
                        queryEvent.onResponseList(responseData.allPeople().people());

                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                });
    }

    /**
     * Query and cache Star Wars movies by featured Character. The result callback will return a List
     * of {@link SWPeopleFilmsQuery.Film} and the character person object
     * whose id was used to make the query. SWPeopleFilmsQuery.Person}
     */

    public static void queryFilmsByCharacter(String characterId, final OnQueryResults queryEvent){
        if(mApolloClient==null)
            return;
        if(queryEvent == null)
            return;
        mApolloClient.query(SWPeopleFilmsQuery.builder()
                .id(characterId)
                .build())
                .httpCachePolicy(HttpCachePolicy.NETWORK_FIRST)
                .enqueue(new ApolloCall.Callback<SWPeopleFilmsQuery.Data>() {

                    @Override
                    public void onResponse(@Nonnull Response<SWPeopleFilmsQuery.Data> response) {
                        SWPeopleFilmsQuery.Data responseData = response.data();
                        if (responseData == null)
                            return;
                        queryEvent.onResponseItem(responseData.person());
                        queryEvent.onResponseList(responseData.person().filmConnection().films());

                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                });
    }
}
