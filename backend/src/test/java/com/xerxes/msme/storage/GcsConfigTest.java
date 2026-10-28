package com.xerxes.msme.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.cloud.storage.Storage;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class GcsConfigTest {

    // A throwaway, locally-generated RSA test key (openssl genpkey), not tied to any real GCP
    // project or account - only used here to prove GcsConfig can parse a well-formed service
    // account key. Structurally valid so parsing succeeds; never used to authenticate anywhere.
    private static final String FAKE_SERVICE_ACCOUNT_JSON =
            """
            {
              "type": "service_account",
              "project_id": "test-project",
              "private_key_id": "abc123",
              "private_key": "-----BEGIN PRIVATE KEY-----\\nMIIEwAIBADANBgkqhkiG9w0BAQEFAASCBKowggSmAgEAAoIBAQDvE+xJlB7FVzcX\\nTIvPb+7+ywBwggG6zcXT6CMdjXG3+1NuCq/WNW5exsPxsrlvTwKFJNZWFOTIrkB5\\nugVAaHxGOnRWTS+sUJCObJ8aPI9WS5U2Jws5C/MB9zjO8abvVckYjnfif4CL+iKD\\nCfDqZlC+tSu17WjRJ2DWI44GgvPJPgAmWfpqQQ94QboZLWhPU+PRyfbyAOl6/CrG\\nRO7zmu+1NXubeI3Qeun3Vku45DSDUI90im9h6cQ3VIuNy4Lirx7Gn9kmE12UYZTu\\nLN0+v0/QiaOorMMDbC+9gu5NRDjSUwLBTtJHv+YOv/e+iXNMUcCCSpAJPntcdyzX\\nSDwg8SVlAgMBAAECggEBAIo0LSqXmnYfpbQtRsEChtj5huaC8PcIzuxhzDLgOjXN\\n7nu1Z9Jfjs7w/gJxe6qxtNY85j5a06qCuLjlZPXHsNd0IR5mfkGRTQ9YEV08Vuve\\nw4HLbjxrAO5I+KkAnqzKHXa5nOntJOVGEr9HduI5hUHJP1xqwmXt6pe/c/Glr8HL\\nnHiuapURNEs0RCuUEnquqjqhGrjEmbVmjtZXj6L5uhGDrwLG1YNCYQOYzXpKfG2u\\nG+MvnqmtiyBC4m/hfWpHsS6hagcKw4LFQnm9pJXXo4v5svwzBeQwPCWgJg+oEDfy\\nPQooVv+Wukzrh135qR+bbowO6hZAxJ0CqF3mwHItXV0CgYEA/MdW8sqa1MQK1D8s\\nE4oXnLHtZuDe9edOJBfXrIrE5L0PiyBOVk5CXKiATBtH/75KY1InuHNbAmiG+yiw\\n81nUMuRgdGTicKhEzTReFUmv29QwTbEbEHM+53Cj0CYT6OiyIarMgBFyMwxPBCU+\\nie5mMOkgL88CHeofSJ5HKYNZnWsCgYEA8h/izynGzcRTqT8ZcpCkedKK2elWNYqj\\nANt0ZupgsExhKB/SJafAa0PPLFI/EMW8m9LXk4IU2Z8w56/tgEV79v9R+nrJzjM2\\n1aqFniBYl2PJtbf0t998gmg8kQWHoXdfgKbldNETCjpwpvzsBK2T/7f+9ZUwnytp\\nAw1rOjh2rG8CgYEA+726hgnosMSc1EwlpT/9y/wXJkIFSniVhbSJAjkhZXw0IqaF\\n3cYKqSTWwPTae4QTqV6EWFnp5TgcVb1snK51ekmfFcvzB6dM/eCNYj4EebWUdYtQ\\nHM3pc6zmN8H0ShyUapsjFthJFzXG66X75noaI9PITlSnEwu7JCMdU+aQNQkCgYEA\\nhqJIYl2emGilBKV7pwLbEoqKfyWKtHcEsMNW9Ks7XIz8MW2QUzP2gS7RkpmhS9hI\\nSnVxPirFIUWt0gc3j24TwoKyuQ9yC+DaivEpH4Zo/LWWQh+qSlxRRcpkc1eZ1aUl\\nS8qCKPThG3zni1N0gh1rAHT1puVwhfGDkRjzGQqwoq0CgYEA9PXZkUgnBQmD3gz3\\nch6DjPjrh1iri/wVLq8jv03v9gefeR0VzHxN943wkm/H/a25fEtKnBPAlLgGOLoH\\nUUvS97voCj5hRAChmhHBxFkApqpotlIgI/DTv3deahB6YTDwR0alBT18JDadrVq4\\nheYSeNyhTZxF5H8HW45ExsZYe7c=\\n-----END PRIVATE KEY-----\\n",
              "client_email": "test@test-project.iam.gserviceaccount.com",
              "client_id": "123456789",
              "token_uri": "https://oauth2.googleapis.com/token"
            }
            """;

    private final GcsConfig gcsConfig = new GcsConfig();

    @Test
    void storage_noCredentialsJson_fallsBackToApplicationDefaultCredentials() throws Exception {
        // StorageOptions.getDefaultInstance() resolves credentials lazily (only on an actual API
        // call), so it succeeds here regardless of environment - this just proves the JSON-key
        // branch was skipped entirely (it would instead throw an IOException on a malformed key).
        GcsProperties properties = new GcsProperties("bucket", null);

        assertThat(gcsConfig.storage(properties)).isNotNull();
    }

    @Test
    void storage_blankCredentialsJson_fallsBackToApplicationDefaultCredentials() throws Exception {
        GcsProperties properties = new GcsProperties("bucket", "   ");

        assertThat(gcsConfig.storage(properties)).isNotNull();
    }

    @Test
    void storage_malformedCredentialsJson_throwsIOException() {
        GcsProperties properties = new GcsProperties("bucket", "not valid json");

        assertThatThrownBy(() -> gcsConfig.storage(properties)).isInstanceOf(IOException.class);
    }

    @Test
    void storage_validServiceAccountJson_buildsAStorageClientWithoutCallingOutToNetwork() throws Exception {
        GcsProperties properties = new GcsProperties("bucket", FAKE_SERVICE_ACCOUNT_JSON);

        Storage storage = gcsConfig.storage(properties);

        assertThat(storage).isNotNull();
    }
}
