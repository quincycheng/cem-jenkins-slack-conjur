package hello;

import ConjurApi.ConjurApi;
import ConjurApi.AwsIamAuthn.ConjurAwsIamAuthn;
import ConjurApi.Config.ConjurConfig;
import ConjurApi.HttpClient.HttpResponse;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Base64;
import java.io.IOException;
import java.io.OutputStream;

public class JenkinsApi {

    String jenkinsPass; 
    String jenkinsUser;
    
    private void getSecretFromConjur() {
        jenkinsPass = Cache.getInstance().getJenkinsPassword();
        jenkinsUser = Cache.getInstance().getJenkinsUsername();

        if ((jenkinsPass == null) || (jenkinsUser == null)) {
            try {
                ConjurConfig conjurConfig = new ConjurConfig(System.getenv("CONJUR_APPLIANCE_URL"),
                        System.getenv("CONJUR_ACCOUNT"));
                conjurConfig.username = System.getenv("CONJUR_AUTHN_LOGIN");

                conjurConfig.apiKey = ConjurAwsIamAuthn.getApiKey("conjur-authn-iam",
                        System.getenv("AWS_ACCESS_KEY_ID"), System.getenv("AWS_SECRET_ACCESS_KEY"),
                        System.getenv("AWS_SESSION_TOKEN"));
                conjurConfig.authnType = "iam";
                conjurConfig.ignoreSsl = true;
                conjurConfig.serviceId = System.getenv("CONJUR_AUTHN_SERVICE_ID");

                ConjurApi conjurApi = new ConjurApi(conjurConfig);
                conjurApi.authenticate();

                HttpResponse res = conjurApi.getSecret(System.getenv("JENKINS_USER"));
                jenkinsUser = res.body;

                res = conjurApi.getSecret(System.getenv("JENKINS_PASS"));
                jenkinsPass = res.body;

                Cache.getInstance().setJenkinsPassword(jenkinsPass);
                Cache.getInstance().setJenkinsUsername(jenkinsUser);

            } catch (Exception e) {
                e.getMessage();
            }
        }
    }

    public void applyFix(String leastPrivilegeOption, String entitiesString, String userId, String channelName) {

        System.out.println(">>>>> Applying Fix");
        this.getSecretFromConjur();

        System.out.println(">>>>> After Get Secret");

        try {

            URL theUrl = new URL(System.getenv("JENKINS_URL"));
            HttpURLConnection conn = (HttpURLConnection) theUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);

            String token = Base64.getEncoder().encodeToString((this.jenkinsUser + ":" + this.jenkinsPass).getBytes());
            conn.setRequestProperty("Authorization", "Basic " + token);

            String requestBody = "least_privilege_option=" + leastPrivilegeOption + "&" + "entities=" + entitiesString
                    + "&" + "user_id=" + userId + "&" + "channel_name=" + channelName;
            OutputStream os = conn.getOutputStream();
            os.write(requestBody.getBytes());
            os.flush();
            os.close();

            conn.getInputStream();

            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
