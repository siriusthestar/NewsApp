package com.nevena.absudacity.newsapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    // container for loaded news items
    private List<NewsItem> loadedNewsItems = new ArrayList<>();

    // called by NewsActivity.onLoadFinished() to update list of loaded items
    public void appendNewsItems(List<NewsItem> items) {
        loadedNewsItems.addAll(items);
        notifyDataSetChanged(); // update visual representation
    }

    @Override
    public NewsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create news item visual representation from xml
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.news_list_item, parent, false);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // can we make Context final (line 30) and use it here? as a matter a fact should we make our class final, and all local variables?
                RecyclerView recyclerView = (RecyclerView)v.getParent();
                int pos = recyclerView.getChildLayoutPosition(v);
                // Create a new intent to view the earthquake URI
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(loadedNewsItems.get(pos).getUrl()));
                // Send the intent to launch a new activity
                 recyclerView.getContext().startActivity(websiteIntent);
            }
        });
        // return custom ViewHolder that will be used to update visual representation of items
        return new NewsViewHolder(itemView);
    }

    @Override
    // called for each item in list to update item's screen content
    public void onBindViewHolder(NewsViewHolder holder, int position) {
        NewsItem newsItem = loadedNewsItems.get(position);
        holder.setNewsItem(newsItem); // update screen content
    }

    @Override
    public int getItemCount() {
        return loadedNewsItems.size();
    }

    // custom ViewHolder implementation aware of our layout
    public class NewsViewHolder extends RecyclerView.ViewHolder {

        // saved for use in setNewsItem()
        private TextView section;
        private TextView title;

        public NewsViewHolder(View itemView) {
            super(itemView);
            // save actual TextView references instead of itemView ( for use in setNewsItem() )
            section = (TextView) itemView.findViewById(R.id.section);
            title = (TextView) itemView.findViewById(R.id.title);
        }

        // update TextView content
        public void setNewsItem(NewsItem newsItem) {
            title.setText(newsItem.getTitle());
            section.setText(newsItem.getSection());
        }

    }


}
