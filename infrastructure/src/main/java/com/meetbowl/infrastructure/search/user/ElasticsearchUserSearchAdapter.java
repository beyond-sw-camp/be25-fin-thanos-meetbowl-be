package com.meetbowl.infrastructure.search.user;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meetbowl.domain.user.UserSearchIndexPort;
import com.meetbowl.infrastructure.config.ElasticsearchUserSearchProperties;
import com.meetbowl.infrastructure.persistence.user.SpringDataUserRepository;
import com.meetbowl.infrastructure.persistence.user.UserSearchSourceRow;

@Component
public class ElasticsearchUserSearchAdapter implements UserSearchIndexPort {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchUserSearchAdapter.class);
    private static final List<String> ADMIN_SEARCH_FIELDS =
            List.of(
                    "name^4",
                    "email^3",
                    "loginId^3",
                    "affiliateName^2",
                    "departmentName^2",
                    "teamName^2",
                    "positionName^2",
                    "roleLabel^2");
    private static final List<String> ADMIN_WILDCARD_FIELDS =
            List.of(
                    "name.raw",
                    "email.raw",
                    "loginId.raw",
                    "affiliateName.raw",
                    "departmentName.raw",
                    "teamName.raw",
                    "positionName.raw",
                    "role",
                    "roleLabel.raw");
    private static final List<String> DIRECTORY_SEARCH_FIELDS =
            List.of(
                    "name^4",
                    "email^3",
                    "loginId^3",
                    "affiliateName^2",
                    "departmentName^2",
                    "teamName^2",
                    "positionName^2");
    private static final List<String> DIRECTORY_WILDCARD_FIELDS =
            List.of(
                    "name.raw",
                    "email.raw",
                    "loginId.raw",
                    "affiliateName.raw",
                    "departmentName.raw",
                    "teamName.raw",
                    "positionName.raw");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SpringDataUserRepository springDataUserRepository;
    private final ElasticsearchUserSearchProperties properties;
    private final AtomicBoolean indexPrepared = new AtomicBoolean(false);

    public ElasticsearchUserSearchAdapter(
            @Qualifier("userSearchElasticsearchRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            SpringDataUserRepository springDataUserRepository,
            ElasticsearchUserSearchProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.springDataUserRepository = springDataUserRepository;
        this.properties = properties;
    }

    public SearchIdsPage searchAdmin(String keyword, int page, int size) {
        return executeSearch(
                keyword,
                page,
                size,
                ADMIN_SEARCH_FIELDS,
                ADMIN_WILDCARD_FIELDS,
                true,
                null,
                null,
                null,
                null,
                null);
    }

    public SearchIdsPage searchDirectory(
            String keyword,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            String status,
            int page,
            int size) {
        return executeSearch(
                keyword,
                page,
                size,
                DIRECTORY_SEARCH_FIELDS,
                DIRECTORY_WILDCARD_FIELDS,
                false,
                affiliateId,
                departmentId,
                teamId,
                positionId,
                status);
    }

    @Override
    public void indexUser(UUID userId) {
        try {
            springDataUserRepository.findSearchSourcesByIdIn(List.of(userId)).stream()
                    .findFirst()
                    .ifPresent(this::indexDocument);
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to sync Elasticsearch user search document. userId={}, reason={}",
                    userId,
                    exception.getMessage());
        }
    }

    @Override
    public ReindexResult reindexAll() {
        ensureIndexReady();
        long processedCount = 0;
        long failedCount = 0;
        int page = 0;
        int batchSize = Math.max(properties.reindexBatchSize(), 100);

        while (true) {
            Page<UserSearchSourceRow> batch =
                    springDataUserRepository.findAllSearchSources(PageRequest.of(page, batchSize));
            if (batch.isEmpty()) {
                break;
            }
            for (UserSearchSourceRow row : batch.getContent()) {
                if (safeIndexDocument(row)) {
                    processedCount++;
                } else {
                    failedCount++;
                }
            }
            if (!batch.hasNext()) {
                break;
            }
            page++;
        }
        return new ReindexResult(processedCount, failedCount);
    }

    @Override
    public void reindexByAffiliateId(UUID affiliateId) {
        safeReindexRows(
                springDataUserRepository.findSearchSourcesByAffiliateId(affiliateId),
                "affiliateId",
                affiliateId);
    }

    @Override
    public void reindexByDepartmentId(UUID departmentId) {
        safeReindexRows(
                springDataUserRepository.findSearchSourcesByDepartmentId(departmentId),
                "departmentId",
                departmentId);
    }

    @Override
    public void reindexByTeamId(UUID teamId) {
        safeReindexRows(
                springDataUserRepository.findSearchSourcesByTeamId(teamId), "teamId", teamId);
    }

    @Override
    public void reindexByPositionId(UUID positionId) {
        safeReindexRows(
                springDataUserRepository.findSearchSourcesByPositionId(positionId),
                "positionId",
                positionId);
    }

    public void prepareIndexIfPossible() {
        try {
            ensureIndexReady();
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to prepare Elasticsearch user search index: {}",
                    exception.getMessage());
        }
    }

    private void safeReindexRows(
            List<UserSearchSourceRow> rows, String filterName, UUID filterValue) {
        try {
            reindexRows(rows);
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to reindex Elasticsearch user search documents. {}={}, reason={}",
                    filterName,
                    filterValue,
                    exception.getMessage());
        }
    }

    private void reindexRows(List<UserSearchSourceRow> rows) {
        ensureIndexReady();
        for (UserSearchSourceRow row : rows) {
            safeIndexDocument(row);
        }
    }

    private boolean safeIndexDocument(UserSearchSourceRow row) {
        try {
            indexDocument(row);
            return true;
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to index user search document. userId={}, reason={}",
                    row.userId(),
                    exception.getMessage());
            return false;
        }
    }

    private void indexDocument(UserSearchSourceRow row) {
        ensureIndexReady();
        restClient
                .put()
                .uri("/{index}/_doc/{id}", properties.userIndexName(), row.userId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(toDocument(row))
                .retrieve()
                .toBodilessEntity();
    }

    private SearchIdsPage executeSearch(
            String keyword,
            int page,
            int size,
            List<String> multiMatchFields,
            List<String> wildcardFields,
            boolean includeRoleSearch,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            String status) {
        ensureIndexReady();

        // 기존 API의 page/size 계약을 그대로 유지하기 위해 ES 요청도 같은 범위 계산으로 만든다.
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("from", (page - 1) * size);
        requestBody.put("size", size);
        requestBody.put("track_total_hits", true);
        requestBody.set(
                "query",
                buildQuery(
                        keyword,
                        multiMatchFields,
                        wildcardFields,
                        includeRoleSearch,
                        affiliateId,
                        departmentId,
                        teamId,
                        positionId,
                        status));
        requestBody.set("sort", buildSort(keyword, includeRoleSearch));

        JsonNode response =
                restClient
                        .post()
                        .uri("/{index}/_search", properties.userIndexName())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(JsonNode.class);

        return parseSearchIdsPage(response);
    }

    private JsonNode buildQuery(
            String keyword,
            List<String> multiMatchFields,
            List<String> wildcardFields,
            boolean includeRoleSearch,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            String status) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        ObjectNode bool = objectMapper.createObjectNode();
        ArrayNode filters = bool.putArray("filter");
        ArrayNode should = bool.putArray("should");

        // 이번 범위에서 검색 가능한 권한은 ADMIN / USER뿐이므로 다른 role 문서는 처음부터 제외한다.
        filters.add(
                objectMapper
                        .createObjectNode()
                        .set(
                                "terms",
                                objectMapper
                                        .createObjectNode()
                                        .set(
                                                "role",
                                                objectMapper
                                                        .createArrayNode()
                                                        .add("admin")
                                                        .add("user"))));

        addTermFilter(filters, "affiliateId", affiliateId);
        addTermFilter(filters, "departmentId", departmentId);
        addTermFilter(filters, "teamId", teamId);
        addTermFilter(filters, "positionId", positionId);
        addTermFilter(filters, "status", status);

        if (trimmedKeyword != null && !trimmedKeyword.isBlank()) {
            // 자동완성 성격의 prefix 검색은 multi_match로, 중간 문자열 검색은 wildcard로 보완한다.
            should.add(buildMultiMatchQuery(trimmedKeyword, multiMatchFields));
            for (String wildcardField : wildcardFields) {
                should.add(buildWildcardQuery(wildcardField, trimmedKeyword));
            }
            if (includeRoleSearch) {
                should.add(buildWildcardQuery("role", trimmedKeyword));
            }
            bool.put("minimum_should_match", 1);
        }

        return objectMapper.createObjectNode().set("bool", bool);
    }

    private ObjectNode buildMultiMatchQuery(String keyword, List<String> fields) {
        // edge_ngram analyzer로 생성된 prefix 토큰을 활용해 추천 검색어처럼 앞부분 일치를 우선 잡는다.
        ObjectNode multiMatch = objectMapper.createObjectNode();
        multiMatch.put("query", keyword);
        multiMatch.put("type", "bool_prefix");
        ArrayNode fieldArray = multiMatch.putArray("fields");
        fields.forEach(fieldArray::add);
        return objectMapper.createObjectNode().set("multi_match", multiMatch);
    }

    private ObjectNode buildWildcardQuery(String field, String keyword) {
        // 이메일, 로그인 ID처럼 중간 문자열 검색이 필요한 필드는 wildcard로 추가 보완한다.
        ObjectNode wildcardConfig = objectMapper.createObjectNode();
        wildcardConfig.put("value", "*" + keyword + "*");
        wildcardConfig.put("case_insensitive", true);
        return objectMapper
                .createObjectNode()
                .set("wildcard", objectMapper.createObjectNode().set(field, wildcardConfig));
    }

    private ArrayNode buildSort(String keyword, boolean adminSearch) {
        ArrayNode sorts = objectMapper.createArrayNode();
        if (keyword != null && !keyword.isBlank()) {
            sorts.add(objectMapper.createObjectNode().set("_score", orderNode("desc")));
        }
        if (adminSearch) {
            sorts.add(objectMapper.createObjectNode().set("createdAt", orderNode("desc")));
        } else {
            sorts.add(objectMapper.createObjectNode().set("name.raw", orderNode("asc")));
            sorts.add(objectMapper.createObjectNode().set("createdAt", orderNode("asc")));
        }
        return sorts;
    }

    private ObjectNode orderNode(String direction) {
        return objectMapper.createObjectNode().put("order", direction);
    }

    private void addTermFilter(ArrayNode filters, String field, Object value) {
        if (value == null) {
            return;
        }
        filters.add(
                objectMapper
                        .createObjectNode()
                        .set(
                                "term",
                                objectMapper
                                        .createObjectNode()
                                        .set(field, objectMapper.valueToTree(value.toString()))));
    }

    private SearchIdsPage parseSearchIdsPage(JsonNode response) {
        JsonNode hitsNode = response.path("hits");
        long total =
                hitsNode.path("total").path("value").isMissingNode()
                        ? hitsNode.path("hits").size()
                        : hitsNode.path("total").path("value").asLong();

        // 응답 DTO 조립은 기존 DB 매핑 흐름을 재사용하기 위해 ES에서는 userId 목록만 추린다.
        List<UUID> userIds = new ArrayList<>();
        for (JsonNode hit : hitsNode.path("hits")) {
            String userId = hit.path("_source").path("userId").asText(null);
            if (userId != null) {
                userIds.add(UUID.fromString(userId));
            }
        }
        return new SearchIdsPage(userIds, total);
    }

    private ObjectNode toDocument(UserSearchSourceRow row) {
        // 검색 전용 인덱스이므로 비밀번호/토큰 같은 민감 정보는 넣지 않고 표시용 필드만 저장한다.
        ObjectNode document = objectMapper.createObjectNode();
        document.put("userId", row.userId().toString());
        document.put("loginId", row.loginId());
        document.put("name", row.name());
        document.put("email", row.email());
        document.put("role", row.role().name());
        document.put("roleLabel", toRoleLabel(row.role().name()));
        document.put("status", row.status().name());
        putNullable(document, "affiliateId", row.affiliateId());
        putNullable(document, "affiliateName", row.affiliateName());
        putNullable(document, "departmentId", row.departmentId());
        putNullable(document, "departmentName", row.departmentName());
        putNullable(document, "teamId", row.teamId());
        putNullable(document, "teamName", row.teamName());
        putNullable(document, "positionId", row.positionId());
        putNullable(document, "positionName", row.positionName());
        document.put("createdAt", row.createdAt().toString());
        return document;
    }

    // 권한 영문 코드와 한글 별칭을 같이 색인해 ADMIN/관리자 모두 같은 검색 경험을 제공한다.
    private String toRoleLabel(String role) {
        return switch (role) {
            case "ADMIN" -> "관리자";
            case "USER" -> "일반 사용자";
            default -> role;
        };
    }

    private void putNullable(ObjectNode document, String field, Object value) {
        if (value == null) {
            document.putNull(field);
            return;
        }
        document.put(field, value.toString());
    }

    private void ensureIndexReady() {
        if (indexPrepared.get()) {
            return;
        }
        synchronized (indexPrepared) {
            if (indexPrepared.get()) {
                return;
            }
            if (!indexExists()) {
                if (!properties.autoCreateIndex()) {
                    throw new IllegalStateException(
                            "Elasticsearch user search index is missing: "
                                    + properties.userIndexName());
                }
                // 로컬/개발 환경에서는 수동 생성 단계를 줄이기 위해 인덱스를 자동으로 준비한다.
                createIndex();
            }
            indexPrepared.set(true);
        }
    }

    private boolean indexExists() {
        return Boolean.TRUE.equals(
                restClient
                        .head()
                        .uri("/{index}", properties.userIndexName())
                        .exchange(
                                (request, response) -> response.getStatusCode().is2xxSuccessful()));
    }

    private void createIndex() {
        try {
            // analyzer와 매핑은 리소스 파일에 고정해 Java 코드 변경 없이도 추적 가능하게 유지한다.
            restClient
                    .put()
                    .uri("/{index}", properties.userIndexName())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(loadIndexTemplate())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to create Elasticsearch index.", exception);
        }
    }

    private JsonNode loadIndexTemplate() {
        try (InputStream inputStream =
                new ClassPathResource("elasticsearch/user-search-index.json").getInputStream()) {
            return objectMapper.readTree(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to load Elasticsearch index template.", exception);
        }
    }

    public record SearchIdsPage(List<UUID> userIds, long totalElements) {
        public SearchIdsPage {
            userIds = List.copyOf(Objects.requireNonNull(userIds));
        }
    }
}
