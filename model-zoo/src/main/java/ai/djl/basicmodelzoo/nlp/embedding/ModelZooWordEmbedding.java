/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.basicmodelzoo.nlp.embedding;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.WordEmbedding;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.nn.core.Embedding;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

/** A {@link WordEmbedding} using a {@link ZooModel}. */
public class ModelZooWordEmbedding implements WordEmbedding, AutoCloseable {

    private ZooModel<NDList, NDList> model;
    private Predictor<NDList, NDList> predictor;
    private Embedding<String> embedding;
    private String unknownToken;

    /**
     * Constructs a {@link ModelZooWordEmbedding}.
     *
     * @param model the model for the embedding. The model's block must consist of only an {@link
     *     Embedding}&lt;{@link String}&gt;.
     */
    @SuppressWarnings("unchecked")
    public ModelZooWordEmbedding(ZooModel<NDList, NDList> model) {
        this.model = model;
        this.unknownToken = model.getProperty("unknownToken");
        predictor = model.newPredictor();
        try {
            embedding = (Embedding<String>) model.getBlock();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The model was not an embedding", e);
        }
    }

    @Override
    public boolean vocabularyContains(String word) {
        return embedding.hasItem(word);
    }

    @Override
    public NDArray preprocessWordToEmbed(NDManager manager, String word) {
        if (embedding.hasItem(word)) {
            return embedding.embed(manager, word);
        } else {
            return embedding.embed(manager, unknownToken);
        }
    }

    @Override
    public NDArray embedWord(NDArray word) throws ModelException {
        try {
            return predictor.predict(new NDList(word)).singletonOrThrow();
        } catch (TranslateException e) {
            throw new ModelException("Could not embed word", e);
        }
    }

    @Override
    public String unembedWord(NDArray word) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void close() {
        predictor.close();
        model.close();
    }
}