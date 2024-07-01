package com.luckydut.ondeviceaitest;

public class ImageData {
    private String image;
    private int id;

    public ImageData(String image, int id) {
        this.image = image;
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}