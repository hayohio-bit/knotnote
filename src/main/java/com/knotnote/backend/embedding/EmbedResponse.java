package com.knotnote.backend.embedding;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class EmbedResponse {
    private List<Double> embedding;
    private int dim;
}
