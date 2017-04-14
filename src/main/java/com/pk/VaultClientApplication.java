package com.pk;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@SpringBootApplication
@RestController
public class VaultClientApplication {

    private static final String VAULT_TOKEN = Preconditions.checkNotNull(System.getenv("VAULT_TOKEN"));
    private static final String VAULT_HOST = Preconditions.checkNotNull(System.getenv("VAULT_HOST"));
    private static final OkHttpClient client = new OkHttpClient();

    @RequestMapping("/")
    public String listSecrets() throws Exception {
        JsonObject secretList = askVault("/secret/?list=true");
        Map<String, JsonObject> secrets = Maps.newHashMap();
        for (JsonElement key : secretList.get("keys").getAsJsonArray()) {
            secrets.put(key.getAsString(), askVault("/secret/" + key.getAsString()));
        }
        return secrets.toString();
    }

    @RequestMapping("/aws")
    public String getAwsCredentials() throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject jsonObject = gson.fromJson(askVaultRaw("/aws/creds/test"), JsonObject.class);
        return "<pre>" + gson.toJson(jsonObject) + "</pre>";
    }

    private JsonObject askVault(String reqPath) throws IOException {
        return getData(askVaultRaw(reqPath));
    }

    private String askVaultRaw(String reqPath) throws IOException {
        Request req = new Request.Builder()
                .url(VAULT_HOST + "/v1" + reqPath)
                .get()
                .addHeader("x-vault-token", VAULT_TOKEN)
                .build();
        Response response = client.newCall(req).execute();
        try (ResponseBody body = response.body()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(body.string());
            }
            return body.string();
        }
    }

    private JsonObject getData(String vaultResponse) {
        JsonObject jsonObject = new Gson().fromJson(vaultResponse, JsonObject.class);
        return jsonObject.get("data").getAsJsonObject();
    }

    public static void main(String[] args) {
		SpringApplication.run(VaultClientApplication.class, args);
	}
}
