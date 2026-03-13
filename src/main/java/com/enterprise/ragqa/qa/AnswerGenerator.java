package com.enterprise.ragqa.qa;

import com.enterprise.ragqa.search.SearchResult;
import java.util.List;

public interface AnswerGenerator {

    String generateAnswer(String question, List<SearchResult> context);
}
