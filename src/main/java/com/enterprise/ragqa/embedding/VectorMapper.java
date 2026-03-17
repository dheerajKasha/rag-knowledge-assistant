package com.enterprise.ragqa.embedding;

import java.util.List;

public final class VectorMapper {

    private VectorMapper() {
    }

    public static float[] toFloatArray(List<Double> values) {
        float[] vector = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            vector[index] = values.get(index).floatValue();
        }
        return vector;
    }
}
