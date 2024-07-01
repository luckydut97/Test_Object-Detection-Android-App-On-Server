package com.luckydut.ondeviceaitest;

import java.util.List;
import java.util.Map;

public class DetectionResult {
    private String id;
    private List<Box> boxes;
    private Map<String, Integer> class_cnt;
    private int detection;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Box> getBoxes() {
        return boxes;
    }

    public void setBoxes(List<Box> boxes) {
        this.boxes = boxes;
    }

    public Map<String, Integer> getClassCnt() {
        return class_cnt;
    }

    public void setClassCnt(Map<String, Integer> class_cnt) {
        this.class_cnt = class_cnt;
    }

    public int getDetection() {
        return detection;
    }

    public void setDetection(int detection) {
        this.detection = detection;
    }

    public static class Box {
        private float x1;
        private float y1;
        private float x2;
        private float y2;
        private String label;

        public float getX1() {
            return x1;
        }

        public void setX1(float x1) {
            this.x1 = x1;
        }

        public float getY1() {
            return y1;
        }

        public void setY1(float y1) {
            this.y1 = y1;
        }

        public float getX2() {
            return x2;
        }

        public void setX2(float x2) {
            this.x2 = x2;
        }

        public float getY2() {
            return y2;
        }

        public void setY2(float y2) {
            this.y2 = y2;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return "Box{" +
                    "x1=" + x1 +
                    ", y1=" + y1 +
                    ", x2=" + x2 +
                    ", y2=" + y2 +
                    ", label='" + label + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "DetectionResult{" +
                "id='" + id + '\'' +
                ", boxes=" + boxes +
                ", class_cnt=" + class_cnt +
                ", detection=" + detection +
                '}';
    }
}