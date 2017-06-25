package com.nevena.absudacity.newsapp;


// holds information about one news item
public class NewsItem {


    private String title;
    private String section;
    private String url;

    public NewsItem(String title, String section, String url) {
        this.title = title;
        this.section = section;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getSection() {
        return section;
    }

    public String getUrl() {
        return url;
    }
}
