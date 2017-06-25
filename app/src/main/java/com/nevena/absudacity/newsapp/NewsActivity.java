package com.nevena.absudacity.newsapp;

import android.app.LoaderManager;
import android.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class NewsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<NewsItem>> {

    // Should be calculated from available screen height and item height
    // also used in NewsLoader as page size (number of items to load in one request)
    public static final int NUMBER_OF_ITEMS_ON_SCREEN = 14;

    // save for reuse
    private NewsAdapter newsAdapter;
    private NewsLoader newsLoader;
    private View loadingIndicator;
    private TextView notificationPanel;

    // indicates that data load is in progress:
    // set in loadItems() reset in onLoadFinished() callback
    // used to disable sending more than one data load request at the time
    private boolean loadingData=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationPanel = (TextView) findViewById(R.id.message);
        loadingIndicator = findViewById(R.id.loading_indicator);

        // LoaderManager notifies NewsActivity about loader events via LoaderCallbacks interface
        getLoaderManager().initLoader(1, null, this);

        // RecyclerView and LinearLayoutManager references never change
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        newsAdapter = new NewsAdapter();
        recyclerView.setAdapter(newsAdapter);

        // add event handler for scroll event that will handle loading of additional news pages
        recyclerView.addOnScrollListener(
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy > 0) { // handle only scroll down
                        int lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

                        // there is an extra page of loaded items in view (below visible page),
                        // initiate load of new page if there is not at last one page of items after
                        // the last visible item
                        if (newsAdapter.getItemCount()<lastVisibleItem+NUMBER_OF_ITEMS_ON_SCREEN ) {
                            loadItems();
                        }
                    }
                }
            }
        );

        newsLoader = new NewsLoader(this);

        if (!newsLoader.isOnline()) { // check if not online
            notificationPanel.setText(R.string.no_internet_connection);
        } else {
            loadItems(); // load first two pages of news
        }

    }

    @Override
    public Loader<List<NewsItem>> onCreateLoader(int id, Bundle args) {
        return newsLoader;
    }

    @Override
    // This gets called when data has been fetched
    public void onLoadFinished(Loader<List<NewsItem>> loader, List<NewsItem> data) {

        loadingIndicator.setVisibility(View.GONE);

        if (data.isEmpty()) { // data is empty if there are no search results or if error occurred
            //check for errors
            if (newsLoader.getErrorMessageResourceId() != NewsLoader.NO_ERROR) {
                notificationPanel.setText(newsLoader.getErrorMessageResourceId()); // display error message
            } else { // no errors
                if (newsAdapter.getItemCount()==0) { // were there any items loaded before?
                    notificationPanel.setText(R.string.no_data); // display "no results found" message
                }
            }
        } else { // we've got some search results
            newsAdapter.appendNewsItems(data); // update adapter/view
        }
        loadingData=false; // reset to enable next load request
    }

    @Override
    public void onLoaderReset(Loader<List<NewsItem>> loader) {    }

    // this is wrapped in a private method to avoid code redundancy
    private void loadItems() {
        // perform data fetch only if there are potentially more results
        // and the previous fetch has been serviced
        if (!newsLoader.isResultsExhausted() && !loadingData ) {
            loadingData=true; // set to disable load requests until onLoadFinished() is done for this request
            loadingIndicator.setVisibility(View.VISIBLE);
            newsLoader.loadMore();
        }
    }
}
