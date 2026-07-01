package com.meetbowl.infrastructure.search.user;

import org.springframework.stereotype.Component;

@Component
public class UserSearchIndexInitializer {

    private final ElasticsearchUserSearchAdapter elasticsearchUserSearchAdapter;

    public UserSearchIndexInitializer(
            ElasticsearchUserSearchAdapter elasticsearchUserSearchAdapter) {
        this.elasticsearchUserSearchAdapter = elasticsearchUserSearchAdapter;
    }

    public void prepare() {
        elasticsearchUserSearchAdapter.prepareIndexIfPossible();
    }
}
