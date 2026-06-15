package com.meetbowl.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3ObjectStorageAdapterTest {

    @Test
    void upload_puts_object_with_content_metadata() {
        S3Client client = Mockito.mock(S3Client.class);
        S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(client, "meetbowl-files");
        byte[] content = "document".getBytes();

        adapter.upload("personal-drive/user/file", "application/pdf", content);

        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("meetbowl-files", request.bucket());
        assertEquals("personal-drive/user/file", request.key());
        assertEquals("application/pdf", request.contentType());
        assertEquals(content.length, request.contentLength());
    }
}
