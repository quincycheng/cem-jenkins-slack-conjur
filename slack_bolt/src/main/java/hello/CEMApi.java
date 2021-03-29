package hello;

import com.quincycheng.cyberark.cem.Client;
import com.quincycheng.cyberark.cem.entities.Remediation;

import ConjurApi.ConjurApi;
import ConjurApi.AwsIamAuthn.ConjurAwsIamAuthn;
import ConjurApi.Config.ConjurConfig;
import ConjurApi.HttpClient.HttpResponse;

public class CEMApi {

    private String cemAccessKey;

    private void authenticate() {

        if (Cache.getInstance().getCemApikey() != null) {
            this.cemAccessKey = Cache.getInstance().getCemApikey();
        } else {
            ConjurConfig conjurConfig = new ConjurConfig(System.getenv("CONJUR_APPLIANCE_URL"),
                    System.getenv("CONJUR_ACCOUNT"));
            conjurConfig.username = System.getenv("CONJUR_AUTHN_LOGIN");

            conjurConfig.apiKey = ConjurAwsIamAuthn.getApiKey("conjur-authn-iam", System.getenv("AWS_ACCESS_KEY_ID"),
                    System.getenv("AWS_SECRET_ACCESS_KEY"), System.getenv("AWS_SESSION_TOKEN"));
            conjurConfig.authnType = "iam";
            conjurConfig.ignoreSsl = true;
            conjurConfig.serviceId = System.getenv("CONJUR_AUTHN_SERVICE_ID");

            ConjurApi conjurApi = new ConjurApi(conjurConfig);

            try {
                conjurApi.authenticate();
                HttpResponse res = conjurApi.getSecret(System.getenv("CEM_PASS"));
                this.cemAccessKey = res.body;

                Cache.getInstance().setCemApikey(this.cemAccessKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Remediation getRemediation(String leastPrivilegeOption, String platform, String accountId, String entityId) {

        Remediation result = new Remediation();

        // Authenicating with Conjur with Lambda IAM role to get access key of CEM
        this.authenticate();

        String cemOrg = System.getenv("CEM_ORG");

        try {
            Client cemClient = new Client(cemOrg, this.cemAccessKey);
            result = cemClient.getRemediation(leastPrivilegeOption, platform, accountId, entityId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
