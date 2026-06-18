package com.meetbowl.infrastructure.search.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class UserSearchIndexInitializer implements ApplicationRunner {

    private final ElasticsearchUserSearchAdapter elasticsearchUserSearchAdapter;

    public UserSearchIndexInitializer(
            ElasticsearchUserSearchAdapter elasticsearchUserSearchAdapter) {
        this.elasticsearchUserSearchAdapter = elasticsearchUserSearchAdapter;
    }

    @Override
    public void run(ApplicationArguments args) {
        elasticsearchUserSearchAdapter.prepareIndexIfPossible();
    }
}
