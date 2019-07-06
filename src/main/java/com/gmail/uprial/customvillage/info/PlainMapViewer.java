package com.gmail.uprial.customvillage.info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PlainMapViewer {
    private class PlainVector {
        private int x;
        private int y;

        private PlainVector(final PlainVector vector) {
            x = vector.x;
            y = vector.y;
        }

        private PlainVector(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        private void minimize(final PlainVector vector) {
            x = Math.min(x, vector.x);
            y = Math.min(y, vector.y);
        }

        private void maximize(final PlainVector vector) {
            x = Math.max(x, vector.x);
            y = Math.max(y, vector.y);
        }

        @Override
        public int hashCode() {
            return x * 1000 + y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (this.getClass() != o.getClass()) return false;
            final PlainVector vector = (PlainVector)o;
            return x == vector.x
                    && y == vector.y;
        }

        @Override
        public String toString() {
            return String.format("(%d, %d)", x, y);
        }
    }

    private class PlainVectorMap extends HashMap<PlainVector,Integer> {
    }

    private final int scale;

    private final PlainVectorMap map = new PlainVectorMap();

    PlainMapViewer(final int scale) {
        this.scale = scale;
    }

    void add(final int x, final int y) {
        final PlainVector vector = new PlainVector(x / scale, y / scale);
        final int value  = map.getOrDefault(vector, 0);
        map.put(vector, value + 1);
    }

    List<String> getTextLines() {
        if(map.isEmpty()) {
            return new ArrayList<>();
        }

        PlainVector minVector = null;
        PlainVector maxVector = null;

        // Calculate boundaries.
        for(Map.Entry<PlainVector,Integer> entry : map.entrySet()) {
            final PlainVector vector = entry.getKey();

            if (minVector == null) {
                minVector = new PlainVector(vector);
                maxVector = new PlainVector(vector);
            } else {
                minVector.minimize(vector);
                maxVector.maximize(vector);
            }
        }

        // Calculate max numbers lengths.
        final int xDigits = (int)Math.log10(Math.max(Math.abs(minVector.x * scale), Math.abs(maxVector.x * scale))) + 1;
        final int yDigits = (int)Math.log10(Math.max(Math.abs(minVector.y * scale), Math.abs(maxVector.y * scale))) + 1;

        // Initialize an array.
        final List<String> lines = new ArrayList<>(maxVector.y - minVector.y + 1 + yDigits + 2);
        // Initialize a linear pen.
        final char[] line = new char[maxVector.x - minVector.x + 1];

        // Save initial X values without scale.
        int[] xAxis = new int[maxVector.x - minVector.x + 1];
        for (int x = minVector.x; x <= maxVector.x; x++) {
            xAxis[x - minVector.x] = x * scale;
        }
        // Populate initial lines.
        for (int i = 0; i < yDigits + 2; i++) {
            lines.add("");
        }
        // Draw minuses for negative numbers.
        for (int x = minVector.x; x <= maxVector.x; x++) {
            if(xAxis[x - minVector.x] < 0) {
                line[x - minVector.x] = '-';
                xAxis[x - minVector.x] = -xAxis[x - minVector.x];
            } else {
                line[x - minVector.x] = ' ';
            }
            lines.set(0, String.format("%" + (xDigits + 2) + "s", " ") + String.valueOf(line));
        }
        // Draw X values vertically, from top to bottom.
        for (int i = 0; i < yDigits; i++) {
            for (int x = minVector.x; x <= maxVector.x; x++) {
                line[x - minVector.x] = (char)((xAxis[x - minVector.x] % 10) + '0');
                xAxis[x - minVector.x] = xAxis[x - minVector.x] / 10;
            }
            lines.set(yDigits - i, String.format("%" + (xDigits + 2) + "s", " ") + String.valueOf(line));
        }
        // Insert a separating line.
        lines.set(yDigits + 1, String.format("%" + (xDigits + 2 + maxVector.x - minVector.x + 1) + "s", " "));
        // Draw the map.
        for (int y = minVector.y; y <= maxVector.y; y++) {
            for (int x = minVector.x; x <= maxVector.x; x++) {
                int value = map.getOrDefault(new PlainVector(x, y), 0);
                line[x - minVector.x] = int2char(value);
            }
            lines.add(String.format("%" + (xDigits + 1) + "d ", y * scale) + String.valueOf(line));
        }

        return lines;
    }

    // ==== PRIVATE METHODS ====

    private char int2char(final int value) {
        if (value < 1) {
            return ' ';
        } else if (value < 10) {
            return (char) (value + '0');
        } else if (value < ('z' - 'a' + 11)) {
            return (char) ((value - 10) + 'a');
        } else {
            return '+';
        }
    }

    // ==== COMMON METHODS ====

    @Override
    public String toString() {
        final Map<String,String> map = new HashMap<>();
        map.put("scale", String.valueOf(scale));
        map.put("map", map.toString());

        return map.toString();

    }
}
