package com.enterprise.ragqa.qa;

import com.enterprise.ragqa.search.SearchResult;
import java.util.List;
import java.util.StringJoiner;

public class ExtractiveAnswerGenerator implements AnswerGenerator {

    @Override
    public String generateAnswer(String question, List<SearchResult> context) {
        if (context.isEmpty()) {
            return "I could not find relevant information in the indexed documents.";
        }

        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("Answer based on indexed documents:");
        for (SearchResult searchResult : context.stream().limit(3).toList()) {
            joiner.add(searchResult.chunk().getChunkText());
        }
        return joiner.toString();
    }
}
