/*
 * Copyright 2014 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "kaa_status.h"
#include "kaa_common.h"
#include "kaa_mem.h"
#include "kaa_external.h"
#include <string.h>

struct kaa_status_t {

    KAA_BOOL        is_registered;
    KAA_BOOL        is_attached;
    KAA_INT32T      event_seq_n;
    kaa_digest      endpoint_public_key_hash;
    kaa_digest      profile_hash;

    char *          endpoint_access_token;
};
#define KAA_STATUS_STATIC_SIZE      (sizeof(KAA_BOOL) + sizeof(KAA_BOOL) + sizeof(KAA_INT32T) + SHA_1_DIGEST_LENGTH*sizeof(char) + SHA_1_DIGEST_LENGTH*sizeof(char))

#define READ_BUFFER(FROM, TO, SIZE) \
        memcpy(TO, FROM, SIZE); \
        FROM += SIZE;

#define WRITE_BUFFER(FROM, TO, SIZE) \
        memcpy(TO, FROM, SIZE); \
        TO += SIZE;

kaa_error_t kaa_create_status(kaa_status_t ** kaa_status_p)
{
    kaa_status_t * kaa_status = KAA_CALLOC(1, sizeof(kaa_status_t));
    if (kaa_status == NULL) {
        return KAA_ERR_NOMEM;
    }

    kaa_status->is_registered = 0;
    kaa_status->is_attached = 0;
    kaa_status->event_seq_n = 0;
    memset(kaa_status->endpoint_public_key_hash, 0, SHA_1_DIGEST_LENGTH);
    memset(kaa_status->profile_hash, 0, SHA_1_DIGEST_LENGTH);
    kaa_status->endpoint_access_token = NULL;

    char *  read_buf = NULL, * read_buf_head = NULL;
    size_t  read_size = 0;
    int     needs_deallocation = 0;
    kaa_read_status_ext(&read_buf, &read_size, &needs_deallocation);
    read_buf_head = read_buf;
    if (read_size >= KAA_STATUS_STATIC_SIZE + sizeof(size_t)) {
        READ_BUFFER(read_buf, &kaa_status->is_registered, sizeof(kaa_status->is_registered))
        READ_BUFFER(read_buf, &kaa_status->is_attached, sizeof(kaa_status->is_attached))
        READ_BUFFER(read_buf, &kaa_status->event_seq_n, sizeof(kaa_status->event_seq_n))
        READ_BUFFER(read_buf, kaa_status->endpoint_public_key_hash, SHA_1_DIGEST_LENGTH)
        READ_BUFFER(read_buf, kaa_status->profile_hash, SHA_1_DIGEST_LENGTH)

        size_t enpoint_access_token_length = 0;
        READ_BUFFER(read_buf, &enpoint_access_token_length, sizeof(enpoint_access_token_length))

        if (enpoint_access_token_length > 0) {
            kaa_status->endpoint_access_token = KAA_CALLOC(enpoint_access_token_length + 1, sizeof(char));
            READ_BUFFER(read_buf, kaa_status->endpoint_access_token, enpoint_access_token_length)
        }
    }

    if (needs_deallocation) {
        KAA_FREE(read_buf_head);
    }

    *kaa_status_p = kaa_status;
    return KAA_ERR_NONE;
}

void kaa_destroy_status(kaa_status_t *status)
{
    KAA_FREE(status->endpoint_access_token);
    KAA_FREE(status);
}

KAA_BOOL    kaa_is_endpoint_registered(kaa_status_t *status)
{
    return status->is_registered;
}

kaa_error_t kaa_set_endpoint_registered(kaa_status_t *status, KAA_BOOL is_registered)
{
    if (status == NULL) {
        return KAA_ERR_BADPARAM;
    }

    status->is_registered = is_registered;

    return KAA_ERR_NONE;
}

KAA_BOOL kaa_is_endpoint_attached_to_user(kaa_status_t *status)
{
    return status->is_attached;
}

kaa_error_t kaa_set_endpoint_attached_to_user(kaa_status_t *status, KAA_BOOL is_attached)
{
    if (status == NULL) {
        return KAA_ERR_BADPARAM;
    }

    status->is_attached = is_attached;

    return KAA_ERR_NONE;
}

char * kaa_status_get_endpoint_access_token(kaa_status_t *status)
{
    if (status != NULL) {
        return status->endpoint_access_token;
    }
    return NULL;
}

kaa_error_t kaa_status_set_endpoint_access_token(kaa_status_t * status, const char *token)
{
    if (status == NULL) {
        return KAA_ERR_BADPARAM;
    }

    if (status->endpoint_access_token) {
        KAA_FREE(status->endpoint_access_token);
    }

    size_t len = strlen(token);
    status->endpoint_access_token = KAA_CALLOC(len + 1, sizeof(char));
    if (!status->endpoint_access_token) {
        return KAA_ERR_NOMEM;
    }

    memcpy(status->endpoint_access_token, token, len);
    return KAA_ERR_NONE;
}

kaa_digest * kaa_status_get_endpoint_public_key_hash(kaa_status_t *status)
{
    if (status != NULL) {
        return &status->endpoint_public_key_hash;
    }
    return NULL;
}

kaa_error_t kaa_status_set_endpoint_public_key_hash(kaa_status_t *status, const kaa_digest hash)
{
    if (status == NULL) {
        return KAA_ERR_BADPARAM;
    }

    memcpy(status->endpoint_public_key_hash, hash, SHA_1_DIGEST_LENGTH);
    return KAA_ERR_NONE;
}

kaa_digest* kaa_status_get_profile_hash(kaa_status_t *status)
{
    if (status != NULL) {
        return &status->profile_hash;
    }
    return NULL;
}

kaa_error_t kaa_status_set_profile_hash(kaa_status_t *status, const kaa_digest hash)
{
    if (status == NULL) {
        return KAA_ERR_BADPARAM;
    }

    memcpy(status->profile_hash, hash, SHA_1_DIGEST_LENGTH);
    return KAA_ERR_NONE;
}

KAA_INT32T  kaa_status_get_event_sequence_number(kaa_status_t* status)
{
    if (status != NULL) {
        return status->event_seq_n;
    }
    return 0;
}

kaa_error_t kaa_status_set_event_sequence_number(kaa_status_t* status, KAA_INT32T seq_n)
{
    if (status == NULL || seq_n < status->event_seq_n) {
        return KAA_ERR_BADPARAM;
    }

    status->event_seq_n = seq_n;
    return KAA_ERR_NONE;
}

kaa_error_t kaa_status_save(kaa_status_t *status)
{
    size_t endpoint_access_token_length = status->endpoint_access_token ? strlen(status->endpoint_access_token) : 0;
    size_t buffer_size = KAA_STATUS_STATIC_SIZE + sizeof(endpoint_access_token_length) + (endpoint_access_token_length );

    char *buffer_head = KAA_CALLOC(buffer_size, sizeof(char)), *buffer = buffer_head;
    if (buffer_head == NULL) {
        return KAA_ERR_NOMEM;
    }

    WRITE_BUFFER(&status->is_registered, buffer, sizeof(status->is_registered));
    WRITE_BUFFER(&status->is_attached, buffer, sizeof(status->is_attached));
    WRITE_BUFFER(&status->event_seq_n, buffer, sizeof(status->event_seq_n));
    WRITE_BUFFER(status->endpoint_public_key_hash, buffer, SHA_1_DIGEST_LENGTH);
    WRITE_BUFFER(status->profile_hash, buffer, SHA_1_DIGEST_LENGTH);
    WRITE_BUFFER(&endpoint_access_token_length, buffer, sizeof(endpoint_access_token_length));
    if (endpoint_access_token_length) {
        WRITE_BUFFER(status->endpoint_access_token, buffer, endpoint_access_token_length);
    }

    kaa_store_status_ext(buffer_head, buffer_size);

    KAA_FREE(buffer_head);

    return KAA_ERR_NONE;
}