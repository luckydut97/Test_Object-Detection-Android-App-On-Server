package com.luckydut.ondeviceaitest;

import java.util.List;

public class DetectionResult {
    private List<Object> objects;

    public List<Object> getObjects() {
        return objects;
    }

    public void setObjects(List<Object> objects) {
        this.objects = objects;
    }

    public static class Object {
        private float centerX;
        private float centerY;
        private float width;
        private float height;
        private String label;

        public float getCenterX() {
            return centerX;
        }

        public void setCenterX(float centerX) {
            this.centerX = centerX;
        }

        public float getCenterY() {
            return centerY;
        }

        public void setCenterY(float centerY) {
            this.centerY = centerY;
        }

        public float getWidth() {
            return width;
        }

        public void setWidth(float width) {
            this.width = width;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return "Object{" +
                    "centerX=" + centerX +
                    ", centerY=" + centerY +
                    ", width=" + width +
                    ", height=" + height +
                    ", label='" + label + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "DetectionResult{" +
                "objects=" + objects +
                '}';
    }
}