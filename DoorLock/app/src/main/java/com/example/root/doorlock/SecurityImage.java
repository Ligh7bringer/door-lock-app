package com.example.root.doorlock;

import android.view.View;

import java.util.ArrayList;

public class SecurityImage extends ArrayList<View> {
    private String title;
    private String image;

    public SecurityImage(String ttl, String img) {
        title = ttl;
        image = img;
    }

    public String getTitle() {
        return title;
    }

    public String getImage() {
        return image;
    }
}
